<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class ProductController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    public function index(array $payload, array $params): never
    {
        $since     = $_GET['since']       ?? null;
        $page      = max(1, (int)($_GET['page']     ?? 1));
        $perPage   = min(500, max(1, (int)($_GET['per_page'] ?? 100)));
        $status    = $_GET['status']      ?? null;
        $catId     = $_GET['category_id'] ?? null;
        $search    = $_GET['search']      ?? null;

        $where  = ['p.deleted_at IS NULL'];
        $binds  = [];

        if ($since) {
            $where[] = 'p.updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($status) {
            $where[] = 'p.sync_status = ?';
            $binds[] = strtoupper($status);
        }
        if ($catId) {
            $where[] = 'p.category_id = ?';
            $binds[] = $catId;
        }
        if ($search) {
            $where[] = '(p.name LIKE ? OR p.sku LIKE ? OR p.barcode LIKE ?)';
            $like    = '%' . $search . '%';
            $binds   = array_merge($binds, [$like, $like, $like]);
        }

        $whereSQL = implode(' AND ', $where);

        // Count
        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM products p WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total = (int)$countStmt->fetchColumn();

        // Data
        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT p.*, c.name AS category_name 
             FROM products p
             LEFT JOIN categories c ON c.id = p.category_id
             WHERE {$whereSQL}
             ORDER BY p.updated_at DESC
             LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));
        $rows = $stmt->fetchAll();

        Response::paginated($rows, $total, $page, $perPage);
    }

    public function show(array $payload, array $params): never
    {
        $id   = $params['id'] ?? '';
        $stmt = $this->db->prepare(
            'SELECT p.*, c.name AS category_name 
             FROM products p
             LEFT JOIN categories c ON c.id = p.category_id
             WHERE p.id = ? AND p.deleted_at IS NULL
             LIMIT 1'
        );
        $stmt->execute([$id]);
        $product = $stmt->fetch();

        if (!$product) {
            Response::error('Product not found', 404);
        }
        Response::json($product);
    }

    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['name', 'sku', 'selling_price']);
            Validator::maxLength($body['name'], 200, 'name');
            Validator::maxLength($body['sku'], 100, 'sku');
            Validator::nonNegative($body['selling_price'], 'selling_price');
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        // Duplicate checks
        if (!empty($body['sku'])) {
            $chk = $this->db->prepare('SELECT id FROM products WHERE sku = ? AND deleted_at IS NULL LIMIT 1');
            $chk->execute([$body['sku']]);
            if ($chk->fetch()) {
                Response::error('SKU already exists', 409);
            }
        }
        if (!empty($body['barcode'])) {
            $chk = $this->db->prepare('SELECT id FROM products WHERE barcode = ? AND deleted_at IS NULL LIMIT 1');
            $chk->execute([$body['barcode']]);
            if ($chk->fetch()) {
                Response::error('Barcode already exists', 409);
            }
        }

        $stmt = $this->db->prepare(
            'INSERT INTO products 
             (id, name, sku, barcode, qr_code, category_id, supplier_id, description,
              cost_price, selling_price, stock_quantity, reorder_level, unit, image_url,
              is_active, sync_status, created_at, updated_at)
             VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, \'SYNCED\', NOW(), NOW())'
        );
        $stmt->execute([
            $body['name'],
            $body['sku'],
            $body['barcode']       ?? null,
            $body['qr_code']       ?? null,
            $body['category_id']   ?? null,
            $body['supplier_id']   ?? null,
            $body['description']   ?? null,
            $body['cost_price']    ?? 0.00,
            $body['selling_price'],
            $body['stock_quantity'] ?? 0,
            $body['reorder_level']  ?? 0,
            $body['unit']           ?? 'pcs',
            $body['image_url']      ?? null,
            isset($body['is_active']) ? (int)(bool)$body['is_active'] : 1,
        ]);

        $newId = $this->db->lastInsertId();
        $created = $this->getById($newId ?: $this->getIdBySku($body['sku']));
        Response::json($created, 201);
    }

    public function update(array $payload, array $params, array $body): never
    {
        $id = $params['id'] ?? '';
        $existing = $this->getById($id);
        if (!$existing) {
            Response::error('Product not found', 404);
        }

        $fields = [];
        $values = [];

        $allowed = [
            'name', 'sku', 'barcode', 'qr_code', 'category_id', 'supplier_id',
            'description', 'cost_price', 'selling_price', 'stock_quantity',
            'reorder_level', 'unit', 'image_url', 'is_active',
        ];
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

        $sql  = 'UPDATE products SET ' . implode(', ', $fields) . ' WHERE id = ? AND deleted_at IS NULL';
        $stmt = $this->db->prepare($sql);
        $stmt->execute($values);

        Response::json($this->getById($id));
    }

    public function destroy(array $payload, array $params): never
    {
        $id = $params['id'] ?? '';
        $stmt = $this->db->prepare(
            "UPDATE products SET sync_status = 'DELETED', deleted_at = NOW(), updated_at = NOW()
             WHERE id = ? AND deleted_at IS NULL"
        );
        $stmt->execute([$id]);

        if ($stmt->rowCount() === 0) {
            Response::error('Product not found', 404);
        }
        Response::json(['message' => 'Product deleted']);
    }

    private function getById(string $id): array|false
    {
        $stmt = $this->db->prepare(
            'SELECT p.*, c.name AS category_name 
             FROM products p
             LEFT JOIN categories c ON c.id = p.category_id
             WHERE p.id = ? LIMIT 1'
        );
        $stmt->execute([$id]);
        return $stmt->fetch();
    }

    private function getIdBySku(string $sku): string
    {
        $stmt = $this->db->prepare('SELECT id FROM products WHERE sku = ? LIMIT 1');
        $stmt->execute([$sku]);
        return (string)($stmt->fetchColumn() ?: '');
    }
}
