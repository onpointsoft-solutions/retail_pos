<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class CategoryController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    // GET /categories
    public function index(array $payload, array $params): never
    {
        $since   = $_GET['since']  ?? null;
        $search  = $_GET['search'] ?? null;
        $page    = max(1, (int)($_GET['page']     ?? 1));
        $perPage = min(500, max(1, (int)($_GET['per_page'] ?? 200)));

        $where = ['deleted_at IS NULL'];
        $binds = [];

        if ($since) {
            $where[] = 'updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($search) {
            $where[] = 'name LIKE ?';
            $binds[] = '%' . $search . '%';
        }

        $whereSQL  = implode(' AND ', $where);
        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM categories WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total = (int) $countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT * FROM categories WHERE {$whereSQL} ORDER BY name ASC LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));

        Response::paginated($stmt->fetchAll(), $total, $page, $perPage);
    }

    // GET /categories/:id
    public function show(array $payload, array $params): never
    {
        $stmt = $this->db->prepare(
            'SELECT * FROM categories WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$params['id'] ?? '']);
        $row = $stmt->fetch();
        if (!$row) Response::error('Category not found', 404);
        Response::json($row);
    }

    // POST /categories
    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['id', 'name']);
            Validator::maxLength($body['name'], 100, 'name');
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        // Unique name check
        $chk = $this->db->prepare(
            'SELECT id FROM categories WHERE name = ? AND deleted_at IS NULL LIMIT 1'
        );
        $chk->execute([$body['name']]);
        if ($chk->fetch()) {
            Response::error("Category name '{$body['name']}' already exists", 409);
        }

        $now = date('Y-m-d H:i:s');
        $stmt = $this->db->prepare(
            'INSERT INTO categories (id, name, description, sync_status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $body['id'],
            trim($body['name']),
            $body['description'] ?? null,
            'SYNCED',
            $body['created_at'] ?? $now,
            $now,
        ]);

        $stmt2 = $this->db->prepare('SELECT * FROM categories WHERE id = ? LIMIT 1');
        $stmt2->execute([$body['id']]);
        Response::json($stmt2->fetch(), 201);
    }

    // PUT/PATCH /categories/:id
    public function update(array $payload, array $params, array $body): never
    {
        $id   = $params['id'] ?? '';
        $stmt = $this->db->prepare(
            'SELECT id FROM categories WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$id]);
        if (!$stmt->fetch()) Response::error('Category not found', 404);

        $fields = [];
        $binds  = [];

        if (isset($body['name'])) {
            // Unique check (excluding self)
            $chk = $this->db->prepare(
                'SELECT id FROM categories WHERE name = ? AND id != ? AND deleted_at IS NULL LIMIT 1'
            );
            $chk->execute([$body['name'], $id]);
            if ($chk->fetch()) Response::error("Category name '{$body['name']}' already exists", 409);
            $fields[] = 'name = ?';       $binds[] = trim($body['name']);
        }
        if (array_key_exists('description', $body)) {
            $fields[] = 'description = ?'; $binds[] = $body['description'];
        }
        $fields[] = 'sync_status = ?'; $binds[] = 'SYNCED';
        $fields[] = 'updated_at = ?';  $binds[] = date('Y-m-d H:i:s');
        $binds[]  = $id;

        $this->db->prepare('UPDATE categories SET ' . implode(', ', $fields) . ' WHERE id = ?')
                 ->execute($binds);

        $stmt2 = $this->db->prepare('SELECT * FROM categories WHERE id = ? LIMIT 1');
        $stmt2->execute([$id]);
        Response::json($stmt2->fetch());
    }

    // DELETE /categories/:id
    public function destroy(array $payload, array $params): never
    {
        $id = $params['id'] ?? '';
        // Check if any products use this category
        $used = $this->db->prepare(
            'SELECT COUNT(*) FROM products WHERE category_id = ? AND deleted_at IS NULL'
        );
        $used->execute([$id]);
        if ((int) $used->fetchColumn() > 0) {
            Response::error('Cannot delete category: products are assigned to it', 409);
        }

        $this->db->prepare(
            'UPDATE categories SET deleted_at = ?, sync_status = ? WHERE id = ?'
        )->execute([date('Y-m-d H:i:s'), 'SYNCED', $id]);

        Response::json(['deleted' => true]);
    }
}
