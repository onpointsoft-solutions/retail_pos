<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';

class SyncController
{
    private PDO $db;

    // Map entity type -> [table, updatable_fields]
    // Field names match the SQLite column names sent by the Java client.
    private array $entityMap = [
        'products' => [
            'table'  => 'products',
            'fields' => ['barcode', 'qr_code', 'sku', 'name', 'category_id', 'supplier_id',
                         'description', 'buying_price', 'selling_price', 'wholesale_price',
                         'current_stock', 'minimum_stock', 'preferred_order_quantity', 'tax_rate', 'discount',
                         'image_path', 'unit', 'status', 'track_expiry',
                         'sync_status', 'version', 'created_at', 'updated_at', 'deleted_at'],
        ],
        'product_images' => [
            'table'  => 'product_images',
            'fields' => ['product_id', 'image_path', 'display_order', 'sync_status', 'created_at', 'updated_at', 'deleted_at'],
        ],
        'categories' => [
            'table'  => 'categories',
            'fields' => ['name', 'description',
                         'sync_status', 'created_at', 'updated_at'],
        ],
        'sales' => [
            'table'  => 'sales',
            'fields' => ['receipt_number', 'cashier_id', 'cashier_name', 'customer_id',
                         'subtotal', 'discount_amount', 'tax_amount', 'grand_total',
                         'payment_method', 'cash_tendered', 'change_amount',
                         'payment_reference', 'status',
                         'sync_status', 'created_at', 'updated_at'],
        ],
        'customers' => [
            'table'  => 'customers',
            'fields' => ['name', 'phone', 'email', 'loyalty_points', 'credit_balance',
                         'sync_status', 'created_at', 'updated_at'],
        ],
        'suppliers' => [
            'table'  => 'suppliers',
            'fields' => ['name', 'phone', 'email', 'address',
                         'sync_status', 'created_at', 'updated_at'],
        ],
        'purchase_orders' => [
            'table'  => 'purchase_orders',
            'fields' => ['supplier_id', 'supplier_name', 'status', 'notes',
                         'expected_delivery_date',
                         'sync_status', 'created_at', 'updated_at'],
        ],
        'inventory_movements' => [
            'table'  => 'inventory_movements',
            'fields' => ['product_id', 'product_name', 'type', 'quantity',
                         'reason', 'batch_number', 'expiry_date', 'user_id',
                         'sync_status', 'created_at'],
        ],
        'users' => [
            'table'  => 'users',
            'fields' => ['username', 'role', 'full_name', 'active',
                         'sync_status', 'created_at', 'updated_at'],
        ],
        'settings' => [
            'table'  => 'app_settings',
            'fields' => ['value', 'updated_at'],
        ],
    ];

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    /**
     * POST /api/sync/upload
     * Body: { entity_type: string, records: [...] }
     */
    public function upload(array $payload, array $body): never
    {
        try {
            Validator::required($body, ['entity_type', 'records']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        $entityType = $body['entity_type'];
        $records    = $body['records'];

        if (!isset($this->entityMap[$entityType])) {
            Response::error('Unknown entity type: ' . $entityType, 422);
        }
        if (!is_array($records)) {
            Response::error('records must be an array', 422);
        }

        $config    = $this->entityMap[$entityType];
        $table     = $config['table'];
        $fields    = $config['fields'];

        $successCount = 0;
        $conflicts    = [];
        $errors       = [];

        foreach ($records as $record) {
            if (empty($record['id'])) {
                $errors[] = ['record' => $record, 'reason' => 'Missing id field'];
                continue;
            }

            try {
                if ($entityType === 'products' && !empty($record['image_blobs'])) {
                    $record['image_path'] = $this->storeProductImages($record['id'], $record['image_blobs']);
                    unset($record['image_blobs']);
                }
                $result = $this->upsertRecord($table, $fields, $record, $entityType);
                if ($result === 'conflict') {
                    $conflicts[] = [
                        'id'     => $record['id'],
                        'entity' => $entityType,
                        'reason' => 'Server record is newer; client version discarded',
                    ];
                } else {
                    $successCount++;
                }
            } catch (Exception $e) {
                $errors[] = ['id' => $record['id'], 'reason' => $e->getMessage()];
            }
        }

        Response::json([
            'success'   => $successCount,
            'conflicts' => $conflicts,
            'errors'    => $errors,
        ]);
    }

    private function storeProductImages(string $productId, array $images): string
    {
        $directory = dirname(__DIR__) . '/uploads/products';
        if (!is_dir($directory) && !mkdir($directory, 0755, true) && !is_dir($directory)) {
            throw new RuntimeException('Unable to create product image storage');
        }
        $stored = [];
        foreach (array_slice($images, 0, 8) as $index => $image) {
            $bytes = base64_decode((string)($image['content'] ?? ''), true);
            if ($bytes === false || strlen($bytes) > 5000000) continue;
            $extension = strtolower(pathinfo((string)($image['name'] ?? ''), PATHINFO_EXTENSION));
            if (!in_array($extension, ['png', 'jpg', 'jpeg', 'gif', 'bmp'], true)) $extension = 'jpg';
            $filename = preg_replace('/[^A-Za-z0-9_-]/', '', $productId) . '-' . $index . '-' . substr(hash('sha256', $bytes), 0, 12) . '.' . $extension;
            if (file_put_contents($directory . '/' . $filename, $bytes) === false) throw new RuntimeException('Unable to save product image');
            $stored[] = 'uploads/products/' . $filename;
        }
        return implode(';', $stored);
    }

    /**
     * GET /api/sync/download/{entity_type}?since=ISO
     */
    public function download(array $payload, array $params): never
    {
        $entityType = $params['entity'] ?? '';
        $since      = $_GET['since']   ?? null;

        if (!isset($this->entityMap[$entityType])) {
            Response::error('Unknown entity type: ' . $entityType, 422);
        }

        $table = $this->entityMap[$entityType]['table'];

        // Build query — settings table uses `key` as PK, others use `id`
        if ($entityType === 'settings') {
            $sql   = 'SELECT * FROM app_settings';
            $binds = [];
            if ($since) {
                $sql   .= ' WHERE updated_at > ?';
                $binds[] = date('Y-m-d H:i:s', strtotime($since));
            }
            $stmt = $this->db->prepare($sql);
            $stmt->execute($binds);
            Response::json([
                'entity_type'   => $entityType,
                'records'       => $stmt->fetchAll(),
                'server_time'   => date('c'),
                'count'         => $stmt->rowCount(),
            ]);
        }

        if (!$since) {
            Response::error('since parameter is required', 422);
        }

        $sinceFormatted = date('Y-m-d H:i:s', strtotime($since));

        // For users, strip sensitive fields and omit deleted_at (not in local schema)
        if ($entityType === 'users') {
            $sql = 'SELECT id, username, role, full_name, active, sync_status, created_at, updated_at
                    FROM users WHERE updated_at > ? ORDER BY updated_at ASC';
        } else {
            $sql  = "SELECT * FROM {$table} WHERE updated_at > ? ORDER BY updated_at ASC";
        }

        $stmt = $this->db->prepare($sql);
        $stmt->execute([$sinceFormatted]);
        $records = $stmt->fetchAll();

        // For sales, attach items
        if ($entityType === 'sales') {
            foreach ($records as &$sale) {
                $iStmt = $this->db->prepare('SELECT * FROM sale_items WHERE sale_id = ?');
                $iStmt->execute([$sale['id']]);
                $sale['items'] = $iStmt->fetchAll();
            }
            unset($sale);
        }

        // For purchase orders, attach items — FK column is `po_id`
        if ($entityType === 'purchase_orders') {
            foreach ($records as &$order) {
                $iStmt = $this->db->prepare('SELECT * FROM purchase_order_items WHERE po_id = ?');
                $iStmt->execute([$order['id']]);
                $order['items'] = $iStmt->fetchAll();
            }
            unset($order);
        }

        Response::json([
            'entity_type' => $entityType,
            'records'     => $records,
            'server_time' => date('c'),
            'count'       => count($records),
        ]);
    }

    /**
     * GET /api/sync/status
     */
    public function status(array $payload): never
    {
        $entities = [
            'products'             => 'products',
            'sales'                => 'sales',
            'customers'            => 'customers',
            'suppliers'            => 'suppliers',
            'purchase_orders'      => 'purchase_orders',
            'inventory_movements'  => 'inventory_movements',
            'users'                => 'users',
        ];

        $counts = [];
        // Tables that DO have deleted_at
        $tablesWithDelete = ['products','sales','customers','suppliers',
                             'purchase_orders','users'];
        // Tables without deleted_at
        $tablesNoDelete   = ['inventory_movements','categories'];

        foreach ($entities as $name => $table) {
            try {
                $hasDeletedAt = in_array($table, $tablesWithDelete, true);
                $where = $hasDeletedAt ? 'WHERE deleted_at IS NULL' : '';
                $stmt = $this->db->prepare(
                    "SELECT sync_status, COUNT(*) as cnt FROM {$table} {$where} GROUP BY sync_status"
                );
                $stmt->execute();
                $rows = $stmt->fetchAll();
                $counts[$name] = [];
                foreach ($rows as $row) {
                    $counts[$name][strtolower($row['sync_status'])] = (int)$row['cnt'];
                }
            } catch (PDOException $e) {
                $counts[$name] = ['error' => $e->getMessage()];
            }
        }

        Response::json([
            'server_time' => date('c'),
            'entities'    => $counts,
        ]);
    }

    /**
     * Perform an upsert for a single record.
     * Returns 'inserted', 'updated', or 'conflict'.
     */
    private function upsertRecord(string $table, array $allowedFields, array $record, string $entityType): string
    {
        $id = $record['id'];

        // Fetch server record
        if ($table === 'app_settings') {
            $chk = $this->db->prepare("SELECT * FROM {$table} WHERE `key` = ? LIMIT 1");
        } else {
            $chk = $this->db->prepare("SELECT * FROM {$table} WHERE id = ? LIMIT 1");
        }
        $chk->execute([$id]);
        $serverRecord = $chk->fetch();

        // Handle DELETED sync status
        if (isset($record['sync_status']) && strtoupper($record['sync_status']) === 'DELETED') {
            if ($serverRecord) {
                if ($table !== 'app_settings') {
                    $del = $this->db->prepare(
                        "UPDATE {$table} SET sync_status = 'DELETED', deleted_at = NOW(), updated_at = NOW() WHERE id = ?"
                    );
                    $del->execute([$id]);
                }
            }
            return 'updated';
        }

        // Conflict check: if server record exists and is newer, keep server version
        if ($serverRecord && isset($record['updated_at']) && isset($serverRecord['updated_at'])) {
            $clientTime = strtotime($record['updated_at']);
            $serverTime = strtotime($serverRecord['updated_at']);
            if ($serverTime > $clientTime) {
                return 'conflict';
            }
        }

        // Build INSERT ... ON DUPLICATE KEY UPDATE
        $insertFields  = ['id'];
        $insertMarkers = ['?'];
        $insertValues  = [$id];
        $updateParts   = [];

        foreach ($allowedFields as $field) {
            if ($field === 'id') continue;
            if (!array_key_exists($field, $record)) continue;

            // For users: never sync password_hash this way
            if ($entityType === 'users' && $field === 'password_hash') continue;

            $insertFields[]  = "`{$field}`";
            $insertMarkers[] = '?';
            $insertValues[]  = $record[$field];
            $updateParts[]   = "`{$field}` = VALUES(`{$field}`)";
        }

        // Always set sync_status to SYNCED on server
        if (!in_array('`sync_status`', $insertFields, true) && $table !== 'app_settings') {
            $insertFields[]  = '`sync_status`';
            $insertMarkers[] = "'SYNCED'";
            $updateParts[]   = "`sync_status` = 'SYNCED'";
        }

        $sql = sprintf(
            'INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s',
            $table,
            implode(', ', $insertFields),
            implode(', ', $insertMarkers),
            implode(', ', $updateParts)
        );

        $stmt = $this->db->prepare($sql);
        $stmt->execute($insertValues);

        return $serverRecord ? 'updated' : 'inserted';
    }
}
