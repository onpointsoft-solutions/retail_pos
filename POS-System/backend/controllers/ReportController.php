<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';

/**
 * ReportController — on-demand server-side reports.
 *
 * All endpoints are GET and require auth.
 * Date parameters:
 *   date_from  YYYY-MM-DD  (default: first day of current month)
 *   date_to    YYYY-MM-DD  (default: today)
 */
class ReportController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private function dateRange(): array
    {
        $from = $_GET['date_from'] ?? date('Y-m-01');
        $to   = $_GET['date_to']   ?? date('Y-m-d');
        return [
            date('Y-m-d', strtotime($from)) . ' 00:00:00',
            date('Y-m-d', strtotime($to))   . ' 23:59:59',
        ];
    }

    // ── GET /reports/sales ─────────────────────────────────────────────────
    public function sales(array $payload, array $params): never
    {
        [$from, $to] = $this->dateRange();

        $stmt = $this->db->prepare(
            "SELECT
                COUNT(*)                          AS transaction_count,
                COALESCE(SUM(grand_total),0)      AS total_revenue,
                COALESCE(SUM(discount_amount),0)  AS total_discounts,
                COALESCE(SUM(tax_amount),0)       AS total_tax
             FROM sales
             WHERE status = 'COMPLETED' AND created_at >= ? AND created_at <= ?"
        );
        $stmt->execute([$from, $to]);
        $summary = $stmt->fetch();

        // Profit (revenue - COGS)
        $profitStmt = $this->db->prepare(
            "SELECT COALESCE(SUM((si.unit_price - si.buying_price) * si.quantity), 0) AS profit
             FROM sale_items si
             JOIN sales s ON si.sale_id = s.id
             WHERE s.status = 'COMPLETED' AND s.created_at >= ? AND s.created_at <= ?"
        );
        $profitStmt->execute([$from, $to]);
        $summary['total_profit'] = (float) $profitStmt->fetchColumn();

        // By payment method
        $pmStmt = $this->db->prepare(
            "SELECT payment_method, COALESCE(SUM(grand_total),0) AS total
             FROM sales
             WHERE status = 'COMPLETED' AND created_at >= ? AND created_at <= ?
             GROUP BY payment_method ORDER BY total DESC"
        );
        $pmStmt->execute([$from, $to]);
        $summary['by_payment_method'] = $pmStmt->fetchAll();

        $summary['period_from'] = $_GET['date_from'] ?? date('Y-m-01');
        $summary['period_to']   = $_GET['date_to']   ?? date('Y-m-d');

        Response::json($summary);
    }

    // ── GET /reports/profit-loss ───────────────────────────────────────────
    public function profitLoss(array $payload, array $params): never
    {
        [$from, $to] = $this->dateRange();

        // Revenue
        $revStmt = $this->db->prepare(
            "SELECT COALESCE(SUM(grand_total),0) AS revenue,
                    COALESCE(SUM(discount_amount),0) AS discounts,
                    COALESCE(SUM(tax_amount),0) AS tax_collected,
                    COUNT(*) AS tx_count
             FROM sales WHERE status='COMPLETED' AND created_at>=? AND created_at<=?"
        );
        $revStmt->execute([$from, $to]);
        $rev = $revStmt->fetch();

        // COGS
        $cogsStmt = $this->db->prepare(
            "SELECT COALESCE(SUM(si.buying_price * si.quantity),0) AS cogs
             FROM sale_items si JOIN sales s ON si.sale_id=s.id
             WHERE s.status='COMPLETED' AND s.created_at>=? AND s.created_at<=?"
        );
        $cogsStmt->execute([$from, $to]);
        $cogs = (float) $cogsStmt->fetchColumn();

        // Operating expenses by category
        $expStmt = $this->db->prepare(
            "SELECT category, COALESCE(SUM(amount),0) AS total
             FROM expenses
             WHERE deleted_at IS NULL AND date >= ? AND date <= ?
             GROUP BY category ORDER BY total DESC"
        );
        $dfrom = substr($from, 0, 10);
        $dto   = substr($to, 0, 10);
        $expStmt->execute([$dfrom, $dto]);
        $expRows       = $expStmt->fetchAll();
        $totalExpenses = array_sum(array_column($expRows, 'total'));
        $expByCategory = [];
        foreach ($expRows as $r) $expByCategory[$r['category']] = (float)$r['total'];

        $grossProfit = (float)$rev['revenue'] - $cogs;
        $netProfit   = $grossProfit - $totalExpenses;
        $netRev      = (float)$rev['revenue'];

        Response::json([
            'period_from'             => $dfrom,
            'period_to'               => $dto,
            'transaction_count'       => (int)$rev['tx_count'],
            'gross_revenue'           => (float)$rev['revenue'],
            'total_discounts'         => (float)$rev['discounts'],
            'net_revenue'             => $netRev,
            'tax_collected'           => (float)$rev['tax_collected'],
            'cost_of_goods_sold'      => $cogs,
            'gross_profit'            => $grossProfit,
            'gross_margin_pct'        => $netRev > 0 ? round($grossProfit / $netRev * 100, 1) : 0,
            'expenses_by_category'    => $expByCategory,
            'total_operating_expenses'=> $totalExpenses,
            'net_profit'              => $netProfit,
            'net_margin_pct'          => $netRev > 0 ? round($netProfit / $netRev * 100, 1) : 0,
            'is_profitable'           => $netProfit >= 0,
        ]);
    }

    // ── GET /reports/best-sellers ──────────────────────────────────────────
    public function bestSellers(array $payload, array $params): never
    {
        [$from, $to] = $this->dateRange();
        $limit = min(100, max(1, (int)($_GET['limit'] ?? 20)));

        $stmt = $this->db->prepare(
            "SELECT si.product_id, si.product_name,
                    SUM(si.quantity) AS total_qty,
                    SUM(si.line_total) AS total_revenue,
                    SUM((si.unit_price - si.buying_price) * si.quantity) AS total_profit
             FROM sale_items si
             JOIN sales s ON si.sale_id = s.id
             WHERE s.status = 'COMPLETED' AND s.created_at >= ? AND s.created_at <= ?
             GROUP BY si.product_id, si.product_name
             ORDER BY total_qty DESC
             LIMIT ?"
        );
        $stmt->execute([$from, $to, $limit]);
        Response::json($stmt->fetchAll());
    }

    // ── GET /reports/cashier-performance ──────────────────────────────────
    public function cashierPerformance(array $payload, array $params): never
    {
        [$from, $to] = $this->dateRange();

        $stmt = $this->db->prepare(
            "SELECT cashier_id, cashier_name,
                    COUNT(*) AS transactions,
                    COALESCE(SUM(grand_total),0) AS revenue,
                    COALESCE(SUM(discount_amount),0) AS discounts
             FROM sales
             WHERE status = 'COMPLETED' AND created_at >= ? AND created_at <= ?
             GROUP BY cashier_id, cashier_name
             ORDER BY revenue DESC"
        );
        $stmt->execute([$from, $to]);
        Response::json($stmt->fetchAll());
    }

    // ── GET /reports/sales-by-category ────────────────────────────────────
    public function salesByCategory(array $payload, array $params): never
    {
        [$from, $to] = $this->dateRange();

        $stmt = $this->db->prepare(
            "SELECT COALESCE(c.name, 'Uncategorised') AS category,
                    COALESCE(SUM(si.quantity),0) AS total_qty,
                    COALESCE(SUM(si.line_total),0) AS total_revenue
             FROM sale_items si
             JOIN sales s ON si.sale_id = s.id
             LEFT JOIN products p ON si.product_id = p.id
             LEFT JOIN categories c ON p.category_id = c.id
             WHERE s.status = 'COMPLETED' AND s.created_at >= ? AND s.created_at <= ?
             GROUP BY c.name
             ORDER BY total_revenue DESC"
        );
        $stmt->execute([$from, $to]);
        Response::json($stmt->fetchAll());
    }

    // ── GET /reports/tax ───────────────────────────────────────────────────
    public function tax(array $payload, array $params): never
    {
        [$from, $to] = $this->dateRange();

        $stmt = $this->db->prepare(
            "SELECT COALESCE(SUM(grand_total),0)     AS total_revenue,
                    COALESCE(SUM(tax_amount),0)       AS total_tax,
                    COALESCE(SUM(subtotal),0)         AS pre_tax_revenue
             FROM sales WHERE status='COMPLETED' AND created_at>=? AND created_at<=?"
        );
        $stmt->execute([$from, $to]);
        $row = $stmt->fetch();
        $row['period_from'] = substr($from, 0, 10);
        $row['period_to']   = substr($to, 0, 10);
        Response::json($row);
    }

    // ── GET /reports/low-stock ─────────────────────────────────────────────
    public function lowStock(array $payload, array $params): never
    {
        $stmt = $this->db->prepare(
            "SELECT id, name, sku, barcode, current_stock, minimum_stock,
                    (minimum_stock - current_stock) AS shortfall
             FROM products
             WHERE status = 'active'
               AND deleted_at IS NULL
               AND current_stock <= minimum_stock
             ORDER BY shortfall DESC"
        );
        $stmt->execute();
        Response::json($stmt->fetchAll());
    }

    // ── GET /reports/inventory-valuation ──────────────────────────────────
    public function inventoryValuation(array $payload, array $params): never
    {
        $stmt = $this->db->prepare(
            "SELECT p.name, p.sku, p.buying_price, p.selling_price, p.current_stock,
                    (p.buying_price  * p.current_stock) AS cost_value,
                    (p.selling_price * p.current_stock) AS sell_value,
                    c.name AS category
             FROM products p
             LEFT JOIN categories c ON c.id = p.category_id
             WHERE p.status = 'active' AND p.deleted_at IS NULL
             ORDER BY cost_value DESC"
        );
        $stmt->execute();
        $items = $stmt->fetchAll();

        $totalCost = array_sum(array_column($items, 'cost_value'));
        $totalSell = array_sum(array_column($items, 'sell_value'));

        Response::json([
            'items'              => $items,
            'total_cost_value'   => $totalCost,
            'total_sell_value'   => $totalSell,
            'potential_profit'   => $totalSell - $totalCost,
        ]);
    }

    // ── GET /reports/daily-trend ───────────────────────────────────────────
    public function dailyTrend(array $payload, array $params): never
    {
        $days  = min(365, max(1, (int)($_GET['days'] ?? 30)));
        $stmt  = $this->db->prepare(
            "SELECT DATE(created_at) AS date,
                    COALESCE(SUM(grand_total),0) AS revenue,
                    COUNT(*) AS transactions
             FROM sales
             WHERE status = 'COMPLETED'
               AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
             GROUP BY DATE(created_at)
             ORDER BY date ASC"
        );
        $stmt->execute([$days]);
        Response::json($stmt->fetchAll());
    }
}
