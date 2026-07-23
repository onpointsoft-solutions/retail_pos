<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class SupplierController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    public function index(array $payload, array $params): never
    {
        $since   = $_GET['since']  ?? null;
        $search  = $_GET['search'] ?? null;
        $page    = max(1, (int)($_GET['page']     ?? 1));
        $perPage = min(500, max(1, (int)($_GET['per_page'] ?? 100)));

        $where = ['deleted_at IS NULL'];
        $binds = [];

        if ($since) {
            $where[] = 'updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($search) {
            $where[] = '(name LIKE ? OR contact_name LIKE ? OR email LIKE ?)';
            $like    = '%' . $search . '%';
            $binds   = array_merge($binds, [$like, $like, $like]);
        }

        $whereSQL  = implode(' AND ', $where);
        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM suppliers WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total  = (int)$countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT * FROM suppliers WHERE {$whereSQL} ORDER BY name ASC LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));

        Response::paginated($stmt->fetchAll(), $total, $page, $perPage);
    }

    public function show(array $payload, array $params): never
    {
        $id   = $params['id'] ?? '';
        $stmt = $this->db->prepare('SELECT * FROM suppliers WHERE id = ? AND deleted_at IS NULL LIMIT 1');
        $stmt->execute([$id]);
        $supplier = $stmt->fetch();
        if (!$supplier) {
            Response::error('Supplier not found', 404);
        }
        Response::json($supplier);
    }

    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['name']);
            Validator::maxLength($body['name'], 150, 'name');
            if (!empty($body['email'])) {
                Validator::email($body['email'], 'email');
            }
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        $stmt = $this->db->prepare(
            'INSERT INTO suppliers (id, name, contact_name, email, phone, address, notes, sync_status, created_at, updated_at)
             VALUES (UUID(), ?, ?, ?, ?, ?, ?, \'SYNCED\', NOW(), NOW())'
        );
        $stmt->execute([
            $body['name'],
            $body['contact_name'] ?? null,
            $body['email']        ?? null,
            $body['phone']        ?? null,
            $body['address']      ?? null,
            $body['notes']        ?? null,
        ]);

        $newId = $this->db->lastInsertId();
        $row   = $this->getById($newId ?: '');
        Response::json($row, 201);
    }

    public function update(array $payload, array $params, array $body): never
    {
        $id = $params['id'] ?? '';
        if (!$this->getById($id)) {
            Response::error('Supplier not found', 404);
        }

        $fields  = [];
        $values  = [];
        $allowed = ['name', 'contact_name', 'email', 'phone', 'address', 'notes'];

        foreach ($allowed as $f) {
            if (array_key_exists($f, $body)) {
                $fields[] = "{$f} = ?";
                $values[] = $body[$f];
            }
        }
        if (empty($fields)) {
            Response::error('No fields to update', 422);
        }

        $fields[] = "sync_status = 'SYNCED'";
        $fields[] = 'updated_at = NOW()';
        $values[]  = $id;

        $stmt = $this->db->prepare(
            'UPDATE suppliers SET ' . implode(', ', $fields) . ' WHERE id = ? AND deleted_at IS NULL'
        );
        $stmt->execute($values);
        Response::json($this->getById($id));
    }

    public function destroy(array $payload, array $params): never
    {
        $id   = $params['id'] ?? '';
        $stmt = $this->db->prepare(
            "UPDATE suppliers SET sync_status = 'DELETED', deleted_at = NOW(), updated_at = NOW()
             WHERE id = ? AND deleted_at IS NULL"
        );
        $stmt->execute([$id]);
        if ($stmt->rowCount() === 0) {
            Response::error('Supplier not found', 404);
        }
        Response::json(['message' => 'Supplier deleted']);
    }

    private function getById(string $id): array|false
    {
        $stmt = $this->db->prepare('SELECT * FROM suppliers WHERE id = ? LIMIT 1');
        $stmt->execute([$id]);
        return $stmt->fetch();
    }
}
