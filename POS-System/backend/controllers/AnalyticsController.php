<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';

/**
 * AnalyticsController — advanced analytics matching the Java AnalyticsPanel.
 *
 * Endpoints:
 *   GET /analytics/income-vs-expenses   monthly revenue / COGS / expenses / net profit
 *   GET /analytics/market-basket        product co-occurrence with support, confidence, lift
 *   GET /analytics/revenue-breakdown    top products + category donut data
 */
class AnalyticsController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private function dates(): array
    {
        $from = $_GET['date_from'] ?? date('Y-m-01', strtotime('-5 months'));
        $to   = $_GET['date_to']   ?? date('Y-m-d');
        return [
            date('Y-m-d', strtotime($from)),
            date('Y-m-d', strtotime($to)),
        ];
    }

    // ── GET /analytics/income-vs-expenses ─────────────────────────────────────
    public function incomeVsExpenses(array $payload, array $params): never
    {
        [$from, $to] = $this->dates();

        // Monthly revenue + COGS
        $revStmt = $this->db->prepare(
            "SELECT DATE_FORMAT(s.created_at, '%Y-%m') AS month,
                    COALESCE(SUM(s.grand_total), 0)                         AS income,
                    COALESCE(SUM(si.buying_price * si.quantity), 0)         AS cogs
             FROM sales s
             LEFT JOIN sale_items si ON si.sale_id = s.id
             WHERE s.status = 'COMPLETED'
               AND DATE(s.created_at) >= ? AND DATE(s.created_at) <= ?
             GROUP BY month
             ORDER BY month ASC"
        );
        $revStmt->execute([$from, $to]);
        $revByMonth = [];
        foreach ($revStmt->fetchAll() as $r) {
            $revByMonth[$r['month']] = [
                'income'   => (float)$r['income'],
                'cogs'     => (float)$r['cogs'],
                'expenses' => 0.0,
            ];
        }

        // Monthly expenses
        $expStmt = $this->db->prepare(
            "SELECT DATE_FORMAT(date, '%Y-%m') AS month,
                    COALESCE(SUM(amount), 0) AS total
             FROM expenses
             WHERE deleted_at IS NULL AND date >= ? AND date <= ?
             GROUP BY month"
        );
        $expStmt->execute([$from, $to]);
        foreach ($expStmt->fetchAll() as $r) {
            if (!isset($revByMonth[$r['month']])) {
                $revByMonth[$r['month']] = ['income'=>0.0,'cogs'=>0.0,'expenses'=>0.0];
            }
            $revByMonth[$r['month']]['expenses'] = (float)$r['total'];
        }

        // Fill missing months
        $cursor = new DateTime($from . '-01');
        $last   = new DateTime($to);
        while ($cursor <= $last) {
            $key = $cursor->format('Y-m');
            if (!isset($revByMonth[$key])) {
                $revByMonth[$key] = ['income'=>0.0,'cogs'=>0.0,'expenses'=>0.0];
            }
            $cursor->modify('+1 month');
        }
        ksort($revByMonth);

        $result = [];
        foreach ($revByMonth as $month => $d) {
            $gp        = $d['income'] - $d['cogs'];
            $net       = $gp - $d['expenses'];
            $result[]  = [
                'month'        => $month,
                'income'       => $d['income'],
                'cogs'         => $d['cogs'],
                'gross_profit' => $gp,
                'expenses'     => $d['expenses'],
                'net_profit'   => $net,
                'gross_margin' => $d['income'] > 0 ? round($gp / $d['income'] * 100, 1) : 0,
                'net_margin'   => $d['income'] > 0 ? round($net / $d['income'] * 100, 1) : 0,
            ];
        }

        Response::json($result);
    }

    // ── GET /analytics/market-basket ─────────────────────────────────────────
    public function marketBasket(array $payload, array $params): never
    {
        [$from, $to] = $this->dates();
        $limit = min(100, max(5, (int)($_GET['limit'] ?? 40)));
        $minCount = max(1, (int)($_GET['min_count'] ?? 2));

        $start = $from . ' 00:00:00';
        $end   = $to   . ' 23:59:59';

        // Total transaction count in period
        $txStmt = $this->db->prepare(
            "SELECT COUNT(*) FROM sales
             WHERE status='COMPLETED' AND created_at>=? AND created_at<=?"
        );
        $txStmt->execute([$start, $end]);
        $totalTx = max(1, (int)$txStmt->fetchColumn());

        // Co-occurrence pairs (self-join)
        $pairsStmt = $this->db->prepare(
            "SELECT a.product_name AS product_a, b.product_name AS product_b,
                    COUNT(DISTINCT a.sale_id)                    AS co_count,
                    COUNT(DISTINCT a.sale_id) * 1.0 / ?         AS support
             FROM sale_items a
             JOIN sale_items b ON a.sale_id = b.sale_id AND a.product_id < b.product_id
             JOIN sales s ON a.sale_id = s.id
             WHERE s.status='COMPLETED' AND s.created_at>=? AND s.created_at<=?
             GROUP BY a.product_id, b.product_id
             HAVING co_count >= ?
             ORDER BY co_count DESC
             LIMIT ?"
        );
        $pairsStmt->execute([$totalTx, $start, $end, $minCount, $limit]);
        $pairs = $pairsStmt->fetchAll();

        if (empty($pairs)) {
            Response::json([]);
        }

        // Individual product frequencies for confidence + lift
        $freqStmt = $this->db->prepare(
            "SELECT si.product_name, COUNT(DISTINCT si.sale_id) AS freq
             FROM sale_items si
             JOIN sales s ON si.sale_id = s.id
             WHERE s.status='COMPLETED' AND s.created_at>=? AND s.created_at<=?
             GROUP BY si.product_id"
        );
        $freqStmt->execute([$start, $end]);
        $freq = [];
        foreach ($freqStmt->fetchAll() as $r) {
            $freq[$r['product_name']] = (int)$r['freq'];
        }

        $result = [];
        foreach ($pairs as $p) {
            $co    = (int)  $p['co_count'];
            $sup   = (float)$p['support'];
            $fA    = $freq[$p['product_a']] ?? 1;
            $fB    = $freq[$p['product_b']] ?? 1;
            $pA    = $fA / $totalTx;
            $pB    = $fB / $totalTx;
            $lift  = ($pA * $pB) > 0 ? round($sup / ($pA * $pB), 2) : 0;

            $result[] = [
                'product_a'      => $p['product_a'],
                'product_b'      => $p['product_b'],
                'co_occurrences' => $co,
                'support'        => round($sup * 100, 1),
                'confidence_a_b' => $fA > 0 ? round($co / $fA * 100, 1) : 0,
                'confidence_b_a' => $fB > 0 ? round($co / $fB * 100, 1) : 0,
                'lift'           => $lift,
                'freq_a'         => $fA,
                'freq_b'         => $fB,
            ];
        }

        Response::json($result);
    }

    // ── GET /analytics/revenue-breakdown ─────────────────────────────────────
    public function revenueBreakdown(array $payload, array $params): never
    {
        [$from, $to] = $this->dates();
        $topN = min(50, max(5, (int)($_GET['top'] ?? 10)));
        $start = $from . ' 00:00:00';
        $end   = $to   . ' 23:59:59';

        // Top products by revenue
        $prodStmt = $this->db->prepare(
            "SELECT si.product_name AS name,
                    SUM(si.line_total) AS revenue,
                    SUM(si.quantity)   AS qty,
                    SUM((si.unit_price - si.buying_price) * si.quantity) AS profit
             FROM sale_items si
             JOIN sales s ON si.sale_id = s.id
             WHERE s.status='COMPLETED' AND s.created_at>=? AND s.created_at<=?
             GROUP BY si.product_id, si.product_name
             ORDER BY revenue DESC
             LIMIT ?"
        );
        $prodStmt->execute([$start, $end, $topN]);
        $topProducts = $prodStmt->fetchAll();

        // Category breakdown
        $catStmt = $this->db->prepare(
            "SELECT COALESCE(c.name,'Uncategorised') AS category,
                    COALESCE(SUM(si.line_total),0) AS revenue
             FROM sale_items si
             JOIN sales s ON si.sale_id = s.id
             LEFT JOIN products p ON si.product_id = p.id
             LEFT JOIN categories c ON p.category_id = c.id
             WHERE s.status='COMPLETED' AND s.created_at>=? AND s.created_at<=?
             GROUP BY c.name
             ORDER BY revenue DESC"
        );
        $catStmt->execute([$start, $end]);

        Response::json([
            'period_from'  => $from,
            'period_to'    => $to,
            'top_products' => $topProducts,
            'categories'   => $catStmt->fetchAll(),
        ]);
    }
}
