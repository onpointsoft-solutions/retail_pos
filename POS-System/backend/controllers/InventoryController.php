<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class InventoryController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    public function movements(array $payload, array $params): never
    {
        $since     = $_GET['since']      ?? null;
        $productId = $_GET['product_id'] ?? null;
        $type      = $_GET['type']       ?? null;
        $dateFrom  = $_GET['date_from']  ?? null;
        $dateTo    = $_GET['date_to']    ?? null;
        $page      = max(1, (int)($_GET['page']     ?? 1));
        $perPage   = min(500, max(1, (int)($_GET['per_page'] ?? 100)));

        $where = ['im.deleted_at IS NULL'];
        $binds = [];

        if ($since) {
            $where[] = 'im.updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($productId) {
            $where[] = 'im.product_id = ?';
            $binds[] = $productId;
        }
        if ($type) {
            $where[] = 'im.type = ?';
            $binds[] = strtoupper($type);
        }
        if ($dateFrom) {
            $where[] = 'DATE(im.created_at) >= ?';
            $binds[] = date('Y-m-d', strtotime($dateFrom));
        }
        if ($dateTo) {
            $where[] = 'DATE(im.created_at) <= ?';
            $binds[] = date('Y-m-d', strtotime($dateTo));
        }

        $whereSQL  = implode(' AND ', $where);
        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM inventory_movements im WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total  = (int)$countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT im.*, p.name AS product_name, p.sku, u.full_name AS user_name
             FROM inventory_movements im
             LEFT JOIN products p ON p.id = im.product_id
             LEFT JOIN users u ON u.id = im.user_id
             WHERE {$whereSQL}
             ORDER BY im.created_at DESC
             LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));

        Response::paginated($stmt->fetchAll(), $total, $page, $perPage);
    }

    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['product_id', 'type', 'quantity']);
            Validator::numeric($body['quantity'], 'quantity');
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        $allowedTypes = ['SALE', 'PURCHASE', 'ADJUSTMENT', 'RETURN', 'TRANSFER', 'DAMAGE', 'OPENING'];
        $type         = strtoupper($body['type']);
        if (!in_array($type, $allowedTypes, true)) {
            Response::error('Invalid movement type. Allowed: ' . implode(', ', $allowedTypes), 422);
        }

        // Get current stock
        $chk = $this->db->prepare('SELECT stock_quantity FROM products WHERE id = ? AND deleted_at IS NULL LIMIT 1');
        $chk->execute([$body['product_id']]);
        $product = $chk->fetch();
        if (!$product) {
            Response::error('Product not found', 404);
        }

        $qty      = (float)$body['quantity'];
        $stockBefore = (float)$product['stock_quantity'];

        // Determine stock delta: positive types increase stock, negative decrease
        $negativeTypes = ['SALE', 'TRANSFER', 'DAMAGE'];
        $delta = in_array($type, $negativeTypes, true) ? -abs($qty) : abs($qty);
        $stockAfter = $stockBefore + $delta;

        $this->db->beginTransaction();
        try {
            $mvStmt = $this->db->prepare(
                'INSERT INTO inventory_movements
                 (id, product_id, type, quantity, stock_before, stock_after, reference_id,
                  reference_type, notes, user_id, sync_status, created_at, updated_at)
                 VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, ?, ?, \'SYNCED\', NOW(), NOW())'
            );
            $mvStmt->execute([
                $body['product_id'],
                $type,
                $qty,
                $stockBefore,
                $stockAfter,
                $body['reference_id']   ?? null,
                $body['reference_type'] ?? null,
                $body['notes']          ?? null,
                $payload['user_id'],
            ]);

            // Update product stock
            $updStmt = $this->db->prepare(
                'UPDATE products SET stock_quantity = ?, updated_at = NOW() WHERE id = ?'
            );
            $updStmt->execute([$stockAfter, $body['product_id']]);

            $this->db->commit();
        } catch (Exception $e) {
            $this->db->rollBack();
            Response::error('Failed to record movement: ' . $e->getMessage(), 500);
        }

        $stmt = $this->db->prepare(
            'SELECT im.*, p.name AS product_name, p.sku
             FROM inventory_movements im
             LEFT JOIN products p ON p.id = im.product_id
             WHERE im.product_id = ? ORDER BY im.created_at DESC LIMIT 1'
        );
        $stmt->execute([$body['product_id']]);
        Response::json($stmt->fetch(), 201);
    }
}
