<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';
require_once __DIR__ . '/../helpers/TenantManager.php';

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
        'mpesa_transactions' => [
            'table'  => 'mpesa_transactions',
            'fields' => ['code', 'customer_name', 'amount', 'received_at',
                         'sync_status', 'created_at', 'updated_at'],
        ],
        'users' => [
            'table'  => 'users',
            'fields' => ['username', 'password_hash', 'role', 'full_name', 'active',
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
        TenantManager::ensureSchema($this->db);
    }

    /**
     * POST /api/sync/upload
     * Body: { entity_type: string, records: [...] }
     */
    public function upload(array $payload, array $body): never
    {
        $businessId = $this->requireBusinessId($payload);
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
        $acceptedIds   = [];
        $conflicts    = [];
        $errors       = [];

        // Process product images first
        foreach ($records as &$record) {
            if (empty($record['id'])) {
                $errors[] = ['record' => $record, 'reason' => 'Missing id field'];
                continue;
            }
            if ($entityType === 'products' && !empty($record['image_blobs'])) {
                $record['image_path'] = $this->storeProductImages(
                    $businessId,
                    $record['id'],
                    $record['image_blobs']
                );
                unset($record['image_blobs']);
            }
        }
        unset($record);

        // Filter out records with errors
        $validRecords = array_filter($records, fn($r) => !empty($r['id']));

        // Use bulk upsert for better performance
        $bulkResult = $this->bulkUpsertRecords(
            $table,
            $fields,
            $validRecords,
            $entityType,
            $businessId
        );
        $successCount = $bulkResult['inserted'] + $bulkResult['updated'];
        $acceptedIds = $bulkResult['accepted_ids'];
        $conflicts = $bulkResult['conflicts'];

        Response::json([
            'success'   => $successCount,
            'accepted_ids' => $acceptedIds,
            'conflicts' => $conflicts,
            'errors'    => $errors,
            'server_time' => gmdate('Y-m-d H:i:s'),
        ]);
    }

    private function storeProductImages(string $businessId, string $productId, array $images): string
    {
        $safeBusinessId = preg_replace('/[^A-Za-z0-9_-]/', '', $businessId);
        $directory = dirname(__DIR__) . '/uploads/products/' . $safeBusinessId;
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
        $businessId = $this->requireBusinessId($payload);
        $entityType = $params['entity'] ?? '';
        $since      = $_GET['since']   ?? null;

        if (!isset($this->entityMap[$entityType])) {
            Response::error('Unknown entity type: ' . $entityType, 422);
        }

        $table = $this->entityMap[$entityType]['table'];

        // Build query — settings table uses `key` as PK, others use `id`
        if ($entityType === 'settings') {
            $sql   = 'SELECT * FROM app_settings WHERE business_id = ?';
            $binds = [$businessId];
            if ($since) {
                $sql   .= ' AND updated_at > ?';
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

        $sinceFormatted = $this->normalizeDateTime($since);
        $limit = max(1, min(5000, (int)($_GET['limit'] ?? 1000)));
        $offset = max(0, (int)($_GET['offset'] ?? 0));

        // Password hashes are synchronized only through this authenticated endpoint.
        if ($entityType === 'users') {
            $sql = 'SELECT id, username, password_hash, role, full_name, active,
                           sync_status, created_at, updated_at
                    FROM users WHERE business_id = ? AND updated_at > ?
                    ORDER BY updated_at ASC, id ASC LIMIT ? OFFSET ?';
        } else {
            $sql  = "SELECT * FROM {$table} WHERE business_id = ? AND updated_at > ?
                     ORDER BY updated_at ASC, id ASC LIMIT ? OFFSET ?";
        }

        $stmt = $this->db->prepare($sql);
        $stmt->bindValue(1, $businessId);
        $stmt->bindValue(2, $sinceFormatted);
        $stmt->bindValue(3, $limit, PDO::PARAM_INT);
        $stmt->bindValue(4, $offset, PDO::PARAM_INT);
        $stmt->execute();
        $records = $stmt->fetchAll();

        // For sales, attach items
        if ($entityType === 'sales') {
            foreach ($records as &$sale) {
                $iStmt = $this->db->prepare(
                    'SELECT * FROM sale_items WHERE business_id = ? AND sale_id = ?'
                );
                $iStmt->execute([$businessId, $sale['id']]);
                $sale['items'] = $iStmt->fetchAll();
            }
            unset($sale);
        }

        // For purchase orders, attach items — FK column is `po_id`
        if ($entityType === 'purchase_orders') {
            foreach ($records as &$order) {
                $iStmt = $this->db->prepare(
                    'SELECT * FROM purchase_order_items WHERE business_id = ? AND po_id = ?'
                );
                $iStmt->execute([$businessId, $order['id']]);
                $order['items'] = $iStmt->fetchAll();
            }
            unset($order);
        }

        Response::json([
            'entity_type' => $entityType,
            'records'     => $records,
            'server_time' => gmdate('Y-m-d H:i:s'),
            'count'       => count($records),
            'limit'       => $limit,
            'offset'      => $offset,
        ]);
    }

    /**
     * GET /api/sync/status
     */
    public function status(array $payload): never
    {
        $businessId = $this->requireBusinessId($payload);
        $entities = [
            'products'             => 'products',
            'sales'                => 'sales',
            'customers'            => 'customers',
            'suppliers'            => 'suppliers',
            'purchase_orders'      => 'purchase_orders',
            'inventory_movements'  => 'inventory_movements',
            'users'                => 'users',
            'mpesa_transactions'   => 'mpesa_transactions',
        ];

        $counts = [];
        // Tables that DO have deleted_at
        $tablesWithDelete = ['products','sales','customers','suppliers',
                             'purchase_orders','users'];
        // Tables without deleted_at
        $tablesNoDelete   = ['inventory_movements','categories','mpesa_transactions'];

        foreach ($entities as $name => $table) {
            try {
                $hasDeletedAt = in_array($table, $tablesWithDelete, true);
                $where = $hasDeletedAt
                    ? 'WHERE business_id = ? AND deleted_at IS NULL'
                    : 'WHERE business_id = ?';
                $stmt = $this->db->prepare(
                    "SELECT sync_status, COUNT(*) as cnt FROM {$table} {$where} GROUP BY sync_status"
                );
                $stmt->execute([$businessId]);
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
            'server_time' => gmdate('Y-m-d H:i:s'),
            'entities'    => $counts,
        ]);
    }

    /**
     * Perform an upsert for a single record.
     * Returns 'inserted', 'updated', or 'conflict'.
     */
    private function upsertRecord(
        string $table,
        array $allowedFields,
        array $record,
        string $entityType,
        string $businessId
    ): string
    {
        $id = $record['id'];
        $items = [];
        if (($entityType === 'sales' || $entityType === 'purchase_orders') && isset($record['items']) && is_array($record['items'])) {
            $items = $record['items'];
            unset($record['items']);
        }
        $record = $this->normalizeRecordDates($record);

        // Fetch server record
        if ($table === 'app_settings') {
            $chk = $this->db->prepare(
                "SELECT * FROM {$table} WHERE business_id = ? AND `key` = ? LIMIT 1"
            );
        } else {
            $chk = $this->db->prepare(
                "SELECT * FROM {$table} WHERE business_id = ? AND id = ? LIMIT 1"
            );
        }
        $chk->execute([$businessId, $id]);
        $serverRecord = $chk->fetch();

        // Handle DELETED sync status
        if (isset($record['sync_status']) && strtoupper($record['sync_status']) === 'DELETED') {
            if ($serverRecord) {
                if ($table !== 'app_settings') {
                    $del = $this->db->prepare(
                        "UPDATE {$table} SET sync_status = 'DELETED', deleted_at = NOW(),
                         updated_at = NOW() WHERE business_id = ? AND id = ?"
                    );
                    $del->execute([$businessId, $id]);
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

        $this->db->beginTransaction();
        try {
            $insertFields  = ['id', 'business_id'];
            $insertMarkers = ['?', '?'];
            $insertValues  = [$id, $businessId];
            $updateParts   = [];

            foreach ($allowedFields as $field) {
                if ($field === 'id') continue;
                if (!array_key_exists($field, $record)) continue;

                if ($entityType === 'users' && $field === 'password_hash') continue;

                $insertFields[]  = "`{$field}`";
                $insertMarkers[] = '?';
                $insertValues[]  = $record[$field];
                $updateParts[]   = "`{$field}` = VALUES(`{$field}`)";
            }

            if (!in_array('`sync_status`', $insertFields, true) && $table !== 'app_settings') {
                $insertFields[]  = '`sync_status`';
                $insertMarkers[] = '?';
                $insertValues[]  = 'SYNCED';
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

            if ($entityType === 'sales') {
                $this->replaceSaleItems($id, $items, $businessId);
            }
            if ($entityType === 'purchase_orders') {
                $this->replacePurchaseOrderItems($id, $items, $businessId);
            }

            $this->db->commit();
        } catch (Throwable $e) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $e;
        }

        return $serverRecord ? 'updated' : 'inserted';
    }

    private function replaceSaleItems(string $saleId, array $items, string $businessId): void
    {
        $delete = $this->db->prepare(
            'DELETE FROM sale_items WHERE business_id = ? AND sale_id = ?'
        );
        $delete->execute([$businessId, $saleId]);
        if (!$items) return;

        // Use bulk INSERT for better performance
        $sql = 'INSERT INTO sale_items (id, business_id, sale_id, product_id, product_name, product_sku, quantity, unit_price, buying_price, discount, tax_rate, line_total)
                VALUES ';
        $placeholders = [];
        $values = [];

        foreach ($items as $item) {
            if (!is_array($item)) continue;
            $placeholders[] = '(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)';
            $values[] = $item['id'] ?? $this->uuid();
            $values[] = $businessId;
            $values[] = $saleId;
            $values[] = $item['product_id'] ?? null;
            $values[] = $item['product_name'] ?? '';
            $values[] = $item['product_sku'] ?? null;
            $values[] = (int)($item['quantity'] ?? 0);
            $values[] = (float)($item['unit_price'] ?? 0);
            $values[] = (float)($item['buying_price'] ?? 0);
            $values[] = (float)($item['discount'] ?? 0);
            $values[] = (float)($item['tax_rate'] ?? 0);
            $values[] = (float)($item['line_total'] ?? 0);
        }

        $sql .= implode(', ', $placeholders);
        $stmt = $this->db->prepare($sql);
        $stmt->execute($values);
    }

    private function replacePurchaseOrderItems(string $orderId, array $items, string $businessId): void
    {
        $delete = $this->db->prepare(
            'DELETE FROM purchase_order_items WHERE business_id = ? AND po_id = ?'
        );
        $delete->execute([$businessId, $orderId]);
        if (!$items) return;

        // Use bulk INSERT for better performance
        $sql = 'INSERT INTO purchase_order_items (id, business_id, po_id, product_id, product_name, ordered_qty, received_qty, buying_price)
                VALUES ';
        $placeholders = [];
        $values = [];

        foreach ($items as $item) {
            if (!is_array($item)) continue;
            $placeholders[] = '(?, ?, ?, ?, ?, ?, ?, ?)';
            $values[] = $item['id'] ?? $this->uuid();
            $values[] = $businessId;
            $values[] = $orderId;
            $values[] = $item['product_id'] ?? null;
            $values[] = $item['product_name'] ?? '';
            $values[] = (int)($item['ordered_qty'] ?? 0);
            $values[] = (int)($item['received_qty'] ?? 0);
            $values[] = (float)($item['buying_price'] ?? 0);
        }

        $sql .= implode(', ', $placeholders);
        $stmt = $this->db->prepare($sql);
        $stmt->execute($values);
    }

    private function normalizeRecordDates(array $record): array
    {
        foreach ($record as $key => $value) {
            if ($value === null) continue;
            if (str_ends_with((string)$key, '_at') || $key === 'lockout_until') {
                $record[$key] = $this->normalizeDateTime((string)$value);
            }
        }
        return $record;
    }

    private function normalizeDateTime(string $value): string
    {
        $timestamp = strtotime($value);
        if ($timestamp === false) {
            return gmdate('Y-m-d H:i:s');
        }
        return gmdate('Y-m-d H:i:s', $timestamp);
    }

    private function uuid(): string
    {
        $data = random_bytes(16);
        $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
        $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
    }

    /**
     * Bulk upsert records for better performance
     */
    private function bulkUpsertRecords(
        string $table,
        array $allowedFields,
        array $records,
        string $entityType,
        string $businessId
    ): array
    {
        if (empty($records)) {
            return ['inserted' => 0, 'updated' => 0, 'accepted_ids' => [], 'conflicts' => []];
        }

        $this->db->beginTransaction();
        $inserted = 0;
        $updated = 0;
        $acceptedIds = [];
        $conflicts = [];

        try {
            // Batch fetch existing records
            $ids = array_column($records, 'id');
            $placeholders = implode(',', array_fill(0, count($ids), '?'));
            $chk = $this->db->prepare(
                "SELECT * FROM {$table} WHERE business_id = ? AND id IN ({$placeholders})"
            );
            $chk->execute(array_merge([$businessId], $ids));
            $existingRecords = [];
            while ($row = $chk->fetch()) {
                $existingRecords[$row['id']] = $row;
            }

            // Prepare bulk INSERT statement
            $insertFields = ['id', 'business_id'];
            $insertMarkers = ['?', '?'];
            $updateParts = [];

            foreach ($allowedFields as $field) {
                if ($field === 'id') continue;
                $insertFields[] = "`{$field}`";
                $insertMarkers[] = '?';
                $updateParts[] = "`{$field}` = VALUES(`{$field}`)";
            }

            if (!in_array('`sync_status`', $insertFields, true) && $table !== 'app_settings') {
                $insertFields[] = '`sync_status`';
                $insertMarkers[] = '?';
                $updateParts[] = "`sync_status` = 'SYNCED'";
            }

            $sql = sprintf(
                'INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s',
                $table,
                implode(', ', $insertFields),
                implode(', ', $insertMarkers),
                implode(', ', $updateParts)
            );

            $stmt = $this->db->prepare($sql);

            // Process records
            foreach ($records as $record) {
                $id = $record['id'];
                $ownerCheck = $this->db->prepare(
                    "SELECT business_id FROM {$table} WHERE id = ? LIMIT 1"
                );
                $ownerCheck->execute([$id]);
                $existingOwner = $ownerCheck->fetchColumn();
                if ($existingOwner !== false && (string)$existingOwner !== $businessId) {
                    $conflicts[] = ['id' => $id, 'reason' => 'Record belongs to another business'];
                    continue;
                }
                $items = [];
                if (($entityType === 'sales' || $entityType === 'purchase_orders')
                    && isset($record['items']) && is_array($record['items'])) {
                    $items = $record['items'];
                    unset($record['items']);
                }
                $record = $this->normalizeRecordDates($record);
                if (isset($record['sync_status'])
                    && strtoupper((string)$record['sync_status']) !== 'DELETED') {
                    $record['sync_status'] = 'SYNCED';
                }

                // Conflict check
                if (isset($existingRecords[$id]) && isset($record['updated_at']) && isset($existingRecords[$id]['updated_at'])) {
                    $clientTime = strtotime($record['updated_at']);
                    $serverTime = strtotime($existingRecords[$id]['updated_at']);
                    if ($serverTime > $clientTime) {
                        $conflicts[] = ['id' => $id, 'reason' => 'Server record is newer'];
                        continue;
                    }
                }

                // Build values array - always include a value for each field to match placeholder count
                $values = [$id, $businessId];
                foreach ($allowedFields as $field) {
                    if ($field === 'id') continue;
                    // Use null for missing fields instead of skipping
                    $values[] = array_key_exists($field, $record) ? $record[$field] : null;
                }
                // Add sync_status value if it was added as a placeholder
                if (!in_array('`sync_status`', $insertFields, true) && $table !== 'app_settings') {
                    $values[] = 'SYNCED';
                }

                $stmt->execute($values);

                if ($entityType === 'sales') {
                    $this->replaceSaleItems($id, $items, $businessId);
                } elseif ($entityType === 'purchase_orders') {
                    $this->replacePurchaseOrderItems($id, $items, $businessId);
                }

                if (isset($existingRecords[$id])) {
                    $updated++;
                } else {
                    $inserted++;
                }
                $acceptedIds[] = $id;
            }

            $this->db->commit();
        } catch (Throwable $e) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $e;
        }

        return [
            'inserted' => $inserted,
            'updated' => $updated,
            'accepted_ids' => $acceptedIds,
            'conflicts' => $conflicts,
        ];
    }

    private function requireBusinessId(array $payload): string
    {
        $businessId = trim((string)($payload['business_id'] ?? ''));
        if (!preg_match(
            '/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i',
            $businessId
        )) {
            Response::error(
                'A valid licensed business token is required for synchronization.',
                403
            );
        }
        return strtolower($businessId);
    }
}
