<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class PurchaseOrderController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    public function index(array $payload, array $params): never
    {
        $since      = $_GET['since']       ?? null;
        $supplierId = $_GET['supplier_id'] ?? null;
        $status     = $_GET['status']      ?? null;
        $page       = max(1, (int)($_GET['page']     ?? 1));
        $perPage    = min(500, max(1, (int)($_GET['per_page'] ?? 100)));

        $where = ['po.deleted_at IS NULL'];
        $binds = [];

        if ($since) {
            $where[] = 'po.updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($supplierId) {
            $where[] = 'po.supplier_id = ?';
            $binds[] = $supplierId;
        }
        if ($status) {
            $where[] = 'po.status = ?';
            $binds[] = strtoupper($status);
        }

        $whereSQL  = implode(' AND ', $where);
        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM purchase_orders po WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total  = (int)$countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT po.*, s.name AS supplier_name
             FROM purchase_orders po
             LEFT JOIN suppliers s ON s.id = po.supplier_id
             WHERE {$whereSQL}
             ORDER BY po.created_at DESC
             LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));
        $orders = $stmt->fetchAll();

        foreach ($orders as &$order) {
            $order['items'] = $this->getItems($order['id']);
        }
        unset($order);

        Response::paginated($orders, $total, $page, $perPage);
    }

    public function show(array $payload, array $params): never
    {
        $id   = $params['id'] ?? '';
        $stmt = $this->db->prepare(
            'SELECT po.*, s.name AS supplier_name
             FROM purchase_orders po
             LEFT JOIN suppliers s ON s.id = po.supplier_id
             WHERE po.id = ? AND po.deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$id]);
        $order = $stmt->fetch();
        if (!$order) {
            Response::error('Purchase order not found', 404);
        }
        $order['items'] = $this->getItems($id);
        Response::json($order);
    }

    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['supplier_id', 'items']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        if (!is_array($body['items']) || empty($body['items'])) {
            Response::error('Purchase order must have at least one item', 422);
        }

        $this->db->beginTransaction();
        try {
            $orderId = $body['id'] ?? null;
            if (!$orderId) {
                $uuidStmt = $this->db->query('SELECT UUID() AS uuid');
                $orderId  = $uuidStmt->fetchColumn();
            }

            $poStmt = $this->db->prepare(
                'INSERT INTO purchase_orders
                 (id, po_number, supplier_id, status, total_amount, notes, expected_date,
                  received_date, created_by, sync_status, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, \'SYNCED\', NOW(), NOW())'
            );
            $poStmt->execute([
                $orderId,
                $body['po_number']     ?? $this->generatePoNumber(),
                $body['supplier_id'],
                strtoupper($body['status'] ?? 'PENDING'),
                $body['total_amount']  ?? 0,
                $body['notes']         ?? null,
                $body['expected_date'] ?? null,
                $body['received_date'] ?? null,
                $payload['user_id'],
            ]);

            foreach ($body['items'] as $item) {
                $iStmt = $this->db->prepare(
                    'INSERT INTO purchase_order_items
                     (id, purchase_order_id, product_id, quantity, unit_cost, subtotal, received_quantity, created_at)
                     VALUES (UUID(), ?, ?, ?, ?, ?, ?, NOW())'
                );
                $iStmt->execute([
                    $orderId,
                    $item['product_id'],
                    $item['quantity'],
                    $item['unit_cost'],
                    $item['subtotal'] ?? ($item['quantity'] * $item['unit_cost']),
                    $item['received_quantity'] ?? 0,
                ]);
            }

            $this->db->commit();
        } catch (Exception $e) {
            $this->db->rollBack();
            Response::error('Failed to create purchase order: ' . $e->getMessage(), 500);
        }

        $stmt = $this->db->prepare(
            'SELECT po.*, s.name AS supplier_name
             FROM purchase_orders po
             LEFT JOIN suppliers s ON s.id = po.supplier_id
             WHERE po.id = ? LIMIT 1'
        );
        $stmt->execute([$orderId]);
        $order = $stmt->fetch();
        $order['items'] = $this->getItems($orderId);
        Response::json($order, 201);
    }

    public function update(array $payload, array $params, array $body): never
    {
        $id = $params['id'] ?? '';
        $stmt = $this->db->prepare('SELECT id FROM purchase_orders WHERE id = ? AND deleted_at IS NULL LIMIT 1');
        $stmt->execute([$id]);
        if (!$stmt->fetch()) {
            Response::error('Purchase order not found', 404);
        }

        $fields  = [];
        $values  = [];
        $allowed = ['status', 'total_amount', 'notes', 'expected_date', 'received_date'];

        foreach ($allowed as $f) {
            if (array_key_exists($f, $body)) {
                $fields[] = "{$f} = ?";
                $values[] = $f === 'status' ? strtoupper($body[$f]) : $body[$f];
            }
        }
        if (empty($fields)) {
            Response::error('No fields to update', 422);
        }

        $fields[] = "sync_status = 'SYNCED'";
        $fields[] = 'updated_at = NOW()';
        $values[]  = $id;

        $upd = $this->db->prepare(
            'UPDATE purchase_orders SET ' . implode(', ', $fields) . ' WHERE id = ? AND deleted_at IS NULL'
        );
        $upd->execute($values);

        $stmt = $this->db->prepare(
            'SELECT po.*, s.name AS supplier_name
             FROM purchase_orders po
             LEFT JOIN suppliers s ON s.id = po.supplier_id
             WHERE po.id = ? LIMIT 1'
        );
        $stmt->execute([$id]);
        $order = $stmt->fetch();
        $order['items'] = $this->getItems($id);
        Response::json($order);
    }

    public function destroy(array $payload, array $params): never
    {
        $id   = $params['id'] ?? '';
        $stmt = $this->db->prepare(
            "UPDATE purchase_orders SET sync_status = 'DELETED', deleted_at = NOW(), updated_at = NOW()
             WHERE id = ? AND deleted_at IS NULL"
        );
        $stmt->execute([$id]);
        if ($stmt->rowCount() === 0) {
            Response::error('Purchase order not found', 404);
        }
        Response::json(['message' => 'Purchase order deleted']);
    }

    private function getItems(string $orderId): array
    {
        $stmt = $this->db->prepare(
            'SELECT poi.*, p.name AS product_name, p.sku
             FROM purchase_order_items poi
             LEFT JOIN products p ON p.id = poi.product_id
             WHERE poi.purchase_order_id = ?'
        );
        $stmt->execute([$orderId]);
        return $stmt->fetchAll();
    }

    private function generatePoNumber(): string
    {
        return 'PO-' . date('Ymd') . '-' . strtoupper(substr(uniqid('', true), -6));
    }
}
