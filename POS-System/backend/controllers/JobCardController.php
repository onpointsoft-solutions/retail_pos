<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class JobCardController
{
    private PDO $db;

    private const VALID_STATUSES = [
        'OPEN', 'IN_PROGRESS', 'AWAITING_PARTS', 'COMPLETED', 'CANCELLED', 'INVOICED',
    ];

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    // GET /job-cards
    public function index(array $payload, array $params): never
    {
        $since     = $_GET['since']        ?? null;
        $status    = $_GET['status']       ?? null;
        $techId    = $_GET['technician_id']?? null;
        $custId    = $_GET['customer_id']  ?? null;
        $search    = $_GET['search']       ?? null;
        $page      = max(1, (int)($_GET['page']     ?? 1));
        $perPage   = min(500, max(1, (int)($_GET['per_page'] ?? 100)));

        $where = ['jc.deleted_at IS NULL'];
        $binds = [];

        if ($since)   { $where[] = 'jc.updated_at > ?'; $binds[] = date('Y-m-d H:i:s', strtotime($since)); }
        if ($status)  { $where[] = 'jc.status = ?';     $binds[] = strtoupper($status); }
        if ($techId)  { $where[] = 'jc.technician_id = ?'; $binds[] = $techId; }
        if ($custId)  { $where[] = 'jc.customer_id = ?';   $binds[] = $custId; }
        if ($search) {
            $where[] = '(jc.job_number LIKE ? OR jc.customer_name LIKE ? OR jc.asset_description LIKE ?)';
            $like = '%' . $search . '%';
            $binds = array_merge($binds, [$like, $like, $like]);
        }

        $whereSQL  = implode(' AND ', $where);
        $countStmt = $this->db->prepare(
            "SELECT COUNT(*) FROM job_cards jc WHERE {$whereSQL}"
        );
        $countStmt->execute($binds);
        $total  = (int) $countStmt->fetchColumn();

        $offset = ($page - 1) * $perPage;
        $stmt   = $this->db->prepare(
            "SELECT jc.*
             FROM job_cards jc
             WHERE {$whereSQL}
             ORDER BY jc.created_at DESC
             LIMIT ? OFFSET ?"
        );
        $stmt->execute(array_merge($binds, [$perPage, $offset]));
        $rows = $stmt->fetchAll();

        // Attach service items to each job card
        foreach ($rows as &$row) {
            $si = $this->db->prepare(
                'SELECT * FROM job_card_service_items WHERE job_card_id = ? ORDER BY rowid'
            );
            $si->execute([$row['id']]);
            $row['service_items'] = $si->fetchAll();
        }

        Response::paginated($rows, $total, $page, $perPage);
    }

    // GET /job-cards/:id
    public function show(array $payload, array $params): never
    {
        $stmt = $this->db->prepare(
            'SELECT * FROM job_cards WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$params['id'] ?? '']);
        $job = $stmt->fetch();
        if (!$job) Response::error('Job card not found', 404);

        $si = $this->db->prepare(
            'SELECT * FROM job_card_service_items WHERE job_card_id = ? ORDER BY rowid'
        );
        $si->execute([$job['id']]);
        $job['service_items'] = $si->fetchAll();

        Response::json($job);
    }

    // POST /job-cards
    public function store(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['id', 'job_number', 'customer_name', 'asset_description', 'problem_description']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        // Unique job number check
        $chk = $this->db->prepare(
            'SELECT id FROM job_cards WHERE job_number = ? AND deleted_at IS NULL LIMIT 1'
        );
        $chk->execute([$body['job_number']]);
        if ($chk->fetch()) Response::error("Job number '{$body['job_number']}' already exists", 409);

        $now    = date('Y-m-d H:i:s');
        $status = strtoupper($body['status'] ?? 'OPEN');
        if (!in_array($status, self::VALID_STATUSES, true)) $status = 'OPEN';

        $this->db->prepare(
            'INSERT INTO job_cards
             (id, job_number, customer_id, customer_name, customer_phone,
              asset_description, asset_serial, problem_description,
              diagnosis, resolution, technician_id, technician_name,
              labour_charge, status, active_quotation_id, due_date,
              sync_status, created_at, updated_at)
             VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)'
        )->execute([
            $body['id'],
            $body['job_number'],
            $body['customer_id']          ?? null,
            $body['customer_name'],
            $body['customer_phone']       ?? null,
            $body['asset_description'],
            $body['asset_serial']         ?? null,
            $body['problem_description'],
            $body['diagnosis']            ?? null,
            $body['resolution']           ?? null,
            $body['technician_id']        ?? null,
            $body['technician_name']      ?? null,
            (float)($body['labour_charge'] ?? 0),
            $status,
            $body['active_quotation_id']  ?? null,
            $body['due_date']             ?? null,
            'SYNCED',
            $body['created_at'] ?? $now,
            $now,
        ]);

        // Insert service items
        if (!empty($body['service_items']) && is_array($body['service_items'])) {
            $this->replaceServiceItems($body['id'], $body['service_items']);
        }

        $stmt = $this->db->prepare('SELECT * FROM job_cards WHERE id = ? LIMIT 1');
        $stmt->execute([$body['id']]);
        $job = $stmt->fetch();
        $si  = $this->db->prepare('SELECT * FROM job_card_service_items WHERE job_card_id = ?');
        $si->execute([$body['id']]);
        $job['service_items'] = $si->fetchAll();

        Response::json($job, 201);
    }

    // PUT/PATCH /job-cards/:id
    public function update(array $payload, array $params, array $body): never
    {
        $id  = $params['id'] ?? '';
        $chk = $this->db->prepare(
            'SELECT id FROM job_cards WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $chk->execute([$id]);
        if (!$chk->fetch()) Response::error('Job card not found', 404);

        $mutable = [
            'customer_name', 'customer_phone', 'asset_description', 'asset_serial',
            'problem_description', 'diagnosis', 'resolution',
            'technician_id', 'technician_name', 'labour_charge',
            'active_quotation_id', 'due_date',
        ];

        $fields = [];
        $binds  = [];

        foreach ($mutable as $col) {
            if (array_key_exists($col, $body)) {
                $fields[] = "{$col} = ?";
                $binds[]  = $body[$col];
            }
        }
        if (isset($body['status'])) {
            $s = strtoupper($body['status']);
            $fields[] = 'status = ?';
            $binds[]  = in_array($s, self::VALID_STATUSES, true) ? $s : 'OPEN';
        }
        $fields[] = 'sync_status = ?'; $binds[] = 'SYNCED';
        $fields[] = 'updated_at = ?';  $binds[] = date('Y-m-d H:i:s');
        $binds[]  = $id;

        $this->db->prepare('UPDATE job_cards SET ' . implode(', ', $fields) . ' WHERE id = ?')
                 ->execute($binds);

        if (isset($body['service_items']) && is_array($body['service_items'])) {
            $this->replaceServiceItems($id, $body['service_items']);
        }

        $stmt = $this->db->prepare('SELECT * FROM job_cards WHERE id = ? LIMIT 1');
        $stmt->execute([$id]);
        $job = $stmt->fetch();
        $si  = $this->db->prepare('SELECT * FROM job_card_service_items WHERE job_card_id = ?');
        $si->execute([$id]);
        $job['service_items'] = $si->fetchAll();

        Response::json($job);
    }

    // DELETE /job-cards/:id  (soft)
    public function destroy(array $payload, array $params): never
    {
        $id = $params['id'] ?? '';
        $chk = $this->db->prepare(
            'SELECT id FROM job_cards WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $chk->execute([$id]);
        if (!$chk->fetch()) Response::error('Job card not found', 404);

        $this->db->prepare(
            'UPDATE job_cards SET deleted_at = ?, sync_status = ? WHERE id = ?'
        )->execute([date('Y-m-d H:i:s'), 'SYNCED', $id]);

        Response::json(['deleted' => true]);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private function replaceServiceItems(string $jobCardId, array $items): void
    {
        $this->db->prepare(
            'DELETE FROM job_card_service_items WHERE job_card_id = ?'
        )->execute([$jobCardId]);

        $ins = $this->db->prepare(
            'INSERT INTO job_card_service_items (id, job_card_id, description, charge, quantity)
             VALUES (?, ?, ?, ?, ?)'
        );
        foreach ($items as $item) {
            if (empty($item['description'])) continue;
            $ins->execute([
                $item['id']          ?? bin2hex(random_bytes(16)),
                $jobCardId,
                $item['description'],
                (float)($item['charge']   ?? 0),
                (int)  ($item['quantity'] ?? 1),
            ]);
        }
    }
}
