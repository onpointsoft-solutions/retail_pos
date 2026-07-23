<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class SaleController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    public function index(array $payload, array $params): never
    {
        $since      = $_GET['since']       ?? null;
        $cashierId  = $_GET['cashier_id']  ?? null;
        $dateFrom   = $_GET['date_from']   ?? null;
        $dateTo     = $_GET['date_to']     ?? null;
        $page       = max(1, (int)($_GET['page']     ?? 1));
        $perPage    = min(500, max(1, (int)($_GET['per_page'] ?? 100)));

        $where = ['s.deleted_at IS NULL'];
        $binds = [];

        if ($since) {
            $where[] = 's.updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($cashierId) {
            $where[] = 's.cashier_id = ?';
            $binds[] = $cashierId;
        }
        if ($dateFrom) {
            $where[] = 'DATE(s.created_at) >= ?';
            $binds[] = date('Y-m-d', strtotime($dateFrom));
        }
        if ($dateTo) {
            $where[] = 'DATE(s.created_at) <= ?';
            $binds[] = date('Y-m-d', strtotime($dateTo));
        }

        $whereSQL = implode(' AND ', $where);

        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM sales s WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total = (int)$countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT s.*, u.full_name AS cashier_name, c.name AS customer_name
             FROM sales s
             LEFT JOIN users u ON u.id = s.cashier_id
             LEFT JOIN customers c ON c.id = s.customer_id
             WHERE {$whereSQL}
             ORDER BY s.created_at DESC
             LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));
        $sales = $stmt->fetchAll();

        // Attach items for each sale
        foreach ($sales as &$sale) {
            $sale['items'] = $this->getSaleItems($sale['id']);
        }
        unset($sale);

        Response::paginated($sales, $total, $page, $perPage);
    }

    public function show(array $payload, array $params): never
    {
        $id   = $params['id'] ?? '';
        $stmt = $this->db->prepare(
            'SELECT s.*, u.full_name AS cashier_name, c.name AS customer_name
             FROM sales s
             LEFT JOIN users u ON u.id = s.cashier_id
             LEFT JOIN customers c ON c.id = s.customer_id
             WHERE s.id = ? AND s.deleted_at IS NULL
             LIMIT 1'
        );
        $stmt->execute([$id]);
        $sale = $stmt->fetch();

        if (!$sale) {
            Response::error('Sale not found', 404);
        }

        $sale['items'] = $this->getSaleItems($id);
        Response::json($sale);
    }

    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['items', 'total_amount', 'payment_method']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        if (!is_array($body['items']) || empty($body['items'])) {
            Response::error('Sale must have at least one item', 422);
        }

        $this->db->beginTransaction();
        try {
            $saleId = $body['id'] ?? null; // Allow client-side UUID for sync
            if (!$saleId) {
                $uuidStmt = $this->db->query('SELECT UUID() AS uuid');
                $saleId   = $uuidStmt->fetchColumn();
            }

            $stmt = $this->db->prepare(
                'INSERT INTO sales 
                 (id, receipt_number, cashier_id, customer_id, subtotal, discount_amount,
                  tax_amount, total_amount, amount_paid, change_amount, payment_method,
                  payment_reference, notes, sync_status, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, \'SYNCED\', NOW(), NOW())'
            );
            $stmt->execute([
                $saleId,
                $body['receipt_number']    ?? $this->generateReceiptNumber(),
                $payload['user_id'],
                $body['customer_id']       ?? null,
                $body['subtotal']          ?? $body['total_amount'],
                $body['discount_amount']   ?? 0,
                $body['tax_amount']        ?? 0,
                $body['total_amount'],
                $body['amount_paid']       ?? $body['total_amount'],
                $body['change_amount']     ?? 0,
                strtoupper($body['payment_method']),
                $body['payment_reference'] ?? null,
                $body['notes']             ?? null,
            ]);

            foreach ($body['items'] as $item) {
                $itemStmt = $this->db->prepare(
                    'INSERT INTO sale_items
                     (id, sale_id, product_id, product_name, sku, quantity, unit_price,
                      discount, subtotal, created_at)
                     VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, ?, NOW())'
                );
                $itemStmt->execute([
                    $saleId,
                    $item['product_id'],
                    $item['product_name']  ?? '',
                    $item['sku']           ?? '',
                    $item['quantity'],
                    $item['unit_price'],
                    $item['discount']      ?? 0,
                    $item['subtotal']      ?? ($item['quantity'] * $item['unit_price']),
                ]);

                // Update stock
                $stockStmt = $this->db->prepare(
                    'UPDATE products SET stock_quantity = stock_quantity - ?, updated_at = NOW()
                     WHERE id = ?'
                );
                $stockStmt->execute([$item['quantity'], $item['product_id']]);
            }

            $this->db->commit();

            $stmt = $this->db->prepare('SELECT * FROM sales WHERE id = ? LIMIT 1');
            $stmt->execute([$saleId]);
            $sale           = $stmt->fetch();
            $sale['items']  = $this->getSaleItems($saleId);

            Response::json($sale, 201);
        } catch (Exception $e) {
            $this->db->rollBack();
            Response::error('Failed to create sale: ' . $e->getMessage(), 500);
        }
    }

    private function getSaleItems(string $saleId): array
    {
        $stmt = $this->db->prepare(
            'SELECT si.*, p.image_url 
             FROM sale_items si
             LEFT JOIN products p ON p.id = si.product_id
             WHERE si.sale_id = ?'
        );
        $stmt->execute([$saleId]);
        return $stmt->fetchAll();
    }

    private function generateReceiptNumber(): string
    {
        return 'RCP-' . strtoupper(substr(uniqid('', true), -8));
    }
}
