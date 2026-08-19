<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class ExpenseController
{
    private PDO $db;

    private const VALID_CATEGORIES = [
        'RENT', 'UTILITIES', 'SALARIES', 'SUPPLIES', 'MAINTENANCE',
        'TRANSPORT', 'MARKETING', 'INSURANCE', 'OTHER',
    ];

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    // GET /expenses
    public function index(array $payload, array $params): never
    {
        $since    = $_GET['since']     ?? null;
        $category = $_GET['category']  ?? null;
        $dateFrom = $_GET['date_from'] ?? null;
        $dateTo   = $_GET['date_to']   ?? null;
        $page     = max(1, (int)($_GET['page']     ?? 1));
        $perPage  = min(500, max(1, (int)($_GET['per_page'] ?? 100)));

        $where = ['deleted_at IS NULL'];
        $binds = [];

        if ($since) {
            $where[] = 'updated_at > ?';
            $binds[] = date('Y-m-d H:i:s', strtotime($since));
        }
        if ($category) {
            $where[] = 'category = ?';
            $binds[] = strtoupper($category);
        }
        if ($dateFrom) {
            $where[] = 'date >= ?';
            $binds[] = date('Y-m-d', strtotime($dateFrom));
        }
        if ($dateTo) {
            $where[] = 'date <= ?';
            $binds[] = date('Y-m-d', strtotime($dateTo));
        }

        $whereSQL  = implode(' AND ', $where);
        $countStmt = $this->db->prepare("SELECT COUNT(*) FROM expenses WHERE {$whereSQL}");
        $countStmt->execute($binds);
        $total = (int) $countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT * FROM expenses WHERE {$whereSQL} ORDER BY date DESC, created_at DESC LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));

        Response::paginated($stmt->fetchAll(), $total, $page, $perPage);
    }

    // GET /expenses/:id
    public function show(array $payload, array $params): never
    {
        $stmt = $this->db->prepare(
            'SELECT * FROM expenses WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$params['id'] ?? '']);
        $row = $stmt->fetch();
        if (!$row) Response::error('Expense not found', 404);
        Response::json($row);
    }

    // POST /expenses
    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['id', 'description', 'amount', 'date']);
            Validator::numeric($body['amount'], 'amount');
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        if ((float) $body['amount'] < 0) {
            Response::error('Amount must be 0 or greater', 422);
        }
        if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $body['date'] ?? '')) {
            Response::error('date must be YYYY-MM-DD', 422);
        }

        $category = strtoupper($body['category'] ?? 'OTHER');
        if (!in_array($category, self::VALID_CATEGORIES, true)) {
            $category = 'OTHER';
        }

        $now = date('Y-m-d H:i:s');
        $this->db->prepare(
            'INSERT INTO expenses
             (id, category, description, amount, date, reference, created_by, sync_status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        )->execute([
            $body['id'],
            $category,
            trim($body['description']),
            (float) $body['amount'],
            $body['date'],
            $body['reference'] ?? null,
            $body['created_by'] ?? ($payload['sub'] ?? null),
            'SYNCED',
            $body['created_at'] ?? $now,
            $now,
        ]);

        $stmt = $this->db->prepare('SELECT * FROM expenses WHERE id = ? LIMIT 1');
        $stmt->execute([$body['id']]);
        Response::json($stmt->fetch(), 201);
    }

    // PUT/PATCH /expenses/:id
    public function update(array $payload, array $params, array $body): never
    {
        $id = $params['id'] ?? '';
        $chk = $this->db->prepare(
            'SELECT id FROM expenses WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $chk->execute([$id]);
        if (!$chk->fetch()) Response::error('Expense not found', 404);

        $fields = [];
        $binds  = [];

        if (isset($body['category'])) {
            $cat = strtoupper($body['category']);
            $fields[] = 'category = ?';
            $binds[]  = in_array($cat, self::VALID_CATEGORIES, true) ? $cat : 'OTHER';
        }
        if (isset($body['description'])) {
            $fields[] = 'description = ?'; $binds[] = trim($body['description']);
        }
        if (isset($body['amount'])) {
            if ((float)$body['amount'] < 0) Response::error('Amount must be 0 or greater', 422);
            $fields[] = 'amount = ?'; $binds[] = (float) $body['amount'];
        }
        if (isset($body['date'])) {
            if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $body['date'])) {
                Response::error('date must be YYYY-MM-DD', 422);
            }
            $fields[] = 'date = ?'; $binds[] = $body['date'];
        }
        if (array_key_exists('reference', $body)) {
            $fields[] = 'reference = ?'; $binds[] = $body['reference'];
        }
        $fields[] = 'sync_status = ?'; $binds[] = 'SYNCED';
        $fields[] = 'updated_at = ?';  $binds[] = date('Y-m-d H:i:s');
        $binds[]  = $id;

        $this->db->prepare('UPDATE expenses SET ' . implode(', ', $fields) . ' WHERE id = ?')
                 ->execute($binds);

        $stmt = $this->db->prepare('SELECT * FROM expenses WHERE id = ? LIMIT 1');
        $stmt->execute([$id]);
        Response::json($stmt->fetch());
    }

    // DELETE /expenses/:id
    public function destroy(array $payload, array $params): never
    {
        $id = $params['id'] ?? '';
        $chk = $this->db->prepare(
            'SELECT id FROM expenses WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $chk->execute([$id]);
        if (!$chk->fetch()) Response::error('Expense not found', 404);

        $this->db->prepare(
            'UPDATE expenses SET deleted_at = ?, sync_status = ? WHERE id = ?'
        )->execute([date('Y-m-d H:i:s'), 'SYNCED', $id]);

        Response::json(['deleted' => true]);
    }

    // GET /expenses/summary  →  totals grouped by category + monthly aggregates
    public function summary(array $payload, array $params): never
    {
        $dateFrom = $_GET['date_from'] ?? date('Y-m-01');
        $dateTo   = $_GET['date_to']   ?? date('Y-m-d');

        // By category
        $catStmt = $this->db->prepare(
            'SELECT category,
                    COUNT(*) AS count,
                    COALESCE(SUM(amount), 0) AS total
             FROM expenses
             WHERE deleted_at IS NULL AND date >= ? AND date <= ?
             GROUP BY category
             ORDER BY total DESC'
        );
        $catStmt->execute([$dateFrom, $dateTo]);
        $byCategory = $catStmt->fetchAll();

        // Monthly trend
        $monthStmt = $this->db->prepare(
            "SELECT DATE_FORMAT(date, '%Y-%m') AS month,
                    COALESCE(SUM(amount), 0) AS total
             FROM expenses
             WHERE deleted_at IS NULL AND date >= ? AND date <= ?
             GROUP BY month
             ORDER BY month ASC"
        );
        $monthStmt->execute([$dateFrom, $dateTo]);
        $monthly = $monthStmt->fetchAll();

        // Grand total
        $totalStmt = $this->db->prepare(
            'SELECT COALESCE(SUM(amount), 0) AS grand_total FROM expenses
             WHERE deleted_at IS NULL AND date >= ? AND date <= ?'
        );
        $totalStmt->execute([$dateFrom, $dateTo]);
        $grandTotal = (float) $totalStmt->fetchColumn();

        Response::json([
            'period_from'  => $dateFrom,
            'period_to'    => $dateTo,
            'grand_total'  => $grandTotal,
            'by_category'  => $byCategory,
            'monthly'      => $monthly,
        ]);
    }
}
