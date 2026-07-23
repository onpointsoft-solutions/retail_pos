<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class UserController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    public function index(array $payload, array $params): never
    {
        AuthMiddleware::requireRole($payload, 'ADMIN');

        $since   = $_GET['since']  ?? null;
        $role    = $_GET['role']   ?? null;
        $page    = max(1, (int)($_GET['page']     ?? 1));
        $perPage = min(200, max(1, (int)($_GET['per_page'] ?? 50)));

        $where = ['deleted_at IS NULL'];
        $binds = [];

        if ($since) {
            $where[] = 'updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($role) {
            $where[] = 'role = ?';
            $binds[] = strtoupper($role);
        }

        $whereSQL  = implode(' AND ', $where);
        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM users WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total  = (int)$countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT id, username, role, full_name, active, sync_status, created_at, updated_at
             FROM users WHERE {$whereSQL} ORDER BY username ASC LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));

        Response::paginated($stmt->fetchAll(), $total, $page, $perPage);
    }

    public function show(array $payload, array $params): never
    {
        AuthMiddleware::requireAnyRole($payload, ['ADMIN', 'CASHIER']);
        $id = $params['id'] ?? $payload['user_id'];

        // Non-admins can only view themselves
        if (strtoupper($payload['role']) !== 'ADMIN' && $id !== $payload['user_id']) {
            Response::error('Forbidden', 403);
        }

        $stmt = $this->db->prepare(
            'SELECT id, username, role, full_name, active, sync_status, created_at, updated_at
             FROM users WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$id]);
        $user = $stmt->fetch();
        if (!$user) {
            Response::error('User not found', 404);
        }
        Response::json($user);
    }

    public function store(array $payload, array $body): never
    {
        AuthMiddleware::requireRole($payload, 'ADMIN');

        try {
            Validator::required($body, ['username', 'password', 'role']);
            Validator::maxLength($body['username'], 64, 'username');
            if (strlen($body['password']) < 8) {
                throw new InvalidArgumentException('Password must be at least 8 characters');
            }
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        $allowedRoles = ['ADMIN', 'CASHIER'];
        if (!in_array(strtoupper($body['role']), $allowedRoles, true)) {
            Response::error('Role must be one of: ' . implode(', ', $allowedRoles), 422);
        }

        $chk = $this->db->prepare('SELECT id FROM users WHERE username = ? AND deleted_at IS NULL LIMIT 1');
        $chk->execute([$body['username']]);
        if ($chk->fetch()) {
            Response::error('Username already exists', 409);
        }

        $hash = password_hash($body['password'], PASSWORD_BCRYPT, ['cost' => 12]);

        $stmt = $this->db->prepare(
            'INSERT INTO users (id, username, password_hash, role, full_name, active, sync_status, created_at, updated_at)
             VALUES (UUID(), ?, ?, ?, ?, ?, \'SYNCED\', NOW(), NOW())'
        );
        $stmt->execute([
            $body['username'],
            $hash,
            strtoupper($body['role']),
            $body['full_name'] ?? null,
            isset($body['active']) ? (int)(bool)$body['active'] : 1,
        ]);

        $newId = $this->db->lastInsertId();
        $user  = $this->getSafeUser($newId ?: $this->getIdByUsername($body['username']));
        Response::json($user, 201);
    }

    public function update(array $payload, array $params, array $body): never
    {
        AuthMiddleware::requireRole($payload, 'ADMIN');
        $id = $params['id'] ?? '';

        $existing = $this->getSafeUser($id);
        if (!$existing) {
            Response::error('User not found', 404);
        }

        $fields = [];
        $values = [];

        if (!empty($body['password'])) {
            if (strlen($body['password']) < 8) {
                Response::error('Password must be at least 8 characters', 422);
            }
            $fields[] = 'password_hash = ?';
            $values[] = password_hash($body['password'], PASSWORD_BCRYPT, ['cost' => 12]);
        }

        $allowed = ['username', 'role', 'full_name', 'active'];
        foreach ($allowed as $f) {
            if (array_key_exists($f, $body)) {
                $fields[] = "{$f} = ?";
                $values[] = $f === 'role' ? strtoupper($body[$f]) : $body[$f];
            }
        }
        if (empty($fields)) {
            Response::error('No fields to update', 422);
        }

        $fields[] = "sync_status = 'SYNCED'";
        $fields[] = 'updated_at = NOW()';
        $values[]  = $id;

        $stmt = $this->db->prepare(
            'UPDATE users SET ' . implode(', ', $fields) . ' WHERE id = ? AND deleted_at IS NULL'
        );
        $stmt->execute($values);

        Response::json($this->getSafeUser($id));
    }

    public function destroy(array $payload, array $params): never
    {
        AuthMiddleware::requireRole($payload, 'ADMIN');
        $id = $params['id'] ?? '';

        // Prevent self-deletion
        if ($id === $payload['user_id']) {
            Response::error('Cannot delete your own account', 400);
        }

        $stmt = $this->db->prepare(
            "UPDATE users SET sync_status = 'DELETED', deleted_at = NOW(), updated_at = NOW(), active = 0
             WHERE id = ? AND deleted_at IS NULL"
        );
        $stmt->execute([$id]);

        if ($stmt->rowCount() === 0) {
            Response::error('User not found', 404);
        }
        Response::json(['message' => 'User deleted']);
    }

    private function getSafeUser(string $id): array|false
    {
        $stmt = $this->db->prepare(
            'SELECT id, username, role, full_name, active, sync_status, created_at, updated_at
             FROM users WHERE id = ? LIMIT 1'
        );
        $stmt->execute([$id]);
        return $stmt->fetch();
    }

    private function getIdByUsername(string $username): string
    {
        $stmt = $this->db->prepare('SELECT id FROM users WHERE username = ? LIMIT 1');
        $stmt->execute([$username]);
        return (string)($stmt->fetchColumn() ?: '');
    }
}
