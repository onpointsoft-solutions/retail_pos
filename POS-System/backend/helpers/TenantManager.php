<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';

final class TenantManager
{
    private const TENANT_TABLES = [
        'users', 'categories', 'suppliers', 'products', 'product_images',
        'customers', 'sales', 'sale_items', 'inventory_movements',
        'purchase_orders', 'purchase_order_items', 'mpesa_transactions',
        'app_settings',
    ];

    public static function ensureSchema(PDO $db): void
    {
        $db->exec(
            'CREATE TABLE IF NOT EXISTS businesses (
                id VARCHAR(36) NOT NULL PRIMARY KEY,
                name VARCHAR(150) NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT "ACTIVE",
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_business_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
        );

        if (self::tableExists($db, 'licenses')) {
            self::addColumn($db, 'licenses', 'business_id', 'VARCHAR(36) NULL AFTER id');
            self::addIndex($db, 'licenses', 'idx_license_business', ['business_id']);
        }

        foreach (self::TENANT_TABLES as $table) {
            if (!self::tableExists($db, $table)) {
                continue;
            }
            self::addColumn($db, $table, 'business_id', 'VARCHAR(36) NULL');
            self::addIndex($db, $table, 'idx_' . $table . '_business', ['business_id']);
            if (self::columnExists($db, $table, 'updated_at')) {
                self::addIndex(
                    $db,
                    $table,
                    'idx_' . $table . '_business_updated',
                    ['business_id', 'updated_at']
                );
            }
            if (self::columnExists($db, $table, 'sync_status')) {
                self::addIndex(
                    $db,
                    $table,
                    'idx_' . $table . '_business_sync',
                    ['business_id', 'sync_status']
                );
            }
        }

        if (self::tableExists($db, 'users')) {
            self::replaceUniqueIndex($db, 'users', 'uq_users_username', ['business_id', 'username']);
        }
        if (self::tableExists($db, 'categories')) {
            self::replaceUniqueIndex($db, 'categories', 'uq_categories_name', ['business_id', 'name']);
        }
        if (self::tableExists($db, 'products')) {
            self::replaceUniqueIndex($db, 'products', 'uq_products_sku', ['business_id', 'sku']);
        }
        if (self::tableExists($db, 'sales')) {
            self::replaceUniqueIndex($db, 'sales', 'uq_sales_receipt', ['business_id', 'receipt_number']);
        }
        if (self::tableExists($db, 'mpesa_transactions')) {
            self::replaceUniqueIndex($db, 'mpesa_transactions', 'uq_mpesa_code', ['business_id', 'code']);
        }
        if (self::tableExists($db, 'app_settings')) {
            self::replacePrimaryKey($db, 'app_settings', ['business_id', 'key']);
        }
    }

    public static function ensureLicenseBusiness(PDO $db, array $license): string
    {
        $businessId = trim((string)($license['business_id'] ?? ''));
        if ($businessId !== '') {
            self::claimLegacyData($db, $businessId);
            return $businessId;
        }

        $businessId = self::uuid();
        $name = trim((string)($license['customer_name'] ?? 'BizFlow Business'));
        $db->beginTransaction();
        try {
            $db->prepare('INSERT INTO businesses (id, name) VALUES (?, ?)')
                ->execute([$businessId, $name]);
            $updated = $db->prepare(
                'UPDATE licenses SET business_id = ? WHERE id = ? AND business_id IS NULL'
            );
            $updated->execute([$businessId, $license['id']]);
            if ($updated->rowCount() === 0) {
                $lookup = $db->prepare('SELECT business_id FROM licenses WHERE id = ?');
                $lookup->execute([$license['id']]);
                $businessId = (string)$lookup->fetchColumn();
            }
            $db->commit();
        } catch (Throwable $exception) {
            if ($db->inTransaction()) {
                $db->rollBack();
            }
            throw $exception;
        }

        self::claimLegacyData($db, $businessId);
        return $businessId;
    }

    public static function createBusiness(PDO $db, string $name): string
    {
        $businessId = self::uuid();
        $db->prepare('INSERT INTO businesses (id, name) VALUES (?, ?)')
            ->execute([$businessId, trim($name) ?: 'BizFlow Business']);
        return $businessId;
    }

    private static function claimLegacyData(PDO $db, string $businessId): void
    {
        foreach (self::TENANT_TABLES as $table) {
            if (!self::tableExists($db, $table)) {
                continue;
            }
            $statement = $db->prepare(
                "UPDATE `{$table}` SET business_id = ? WHERE business_id IS NULL"
            );
            $statement->execute([$businessId]);
        }
    }

    private static function addColumn(PDO $db, string $table, string $column, string $definition): void
    {
        if (!self::columnExists($db, $table, $column)) {
            $db->exec("ALTER TABLE `{$table}` ADD COLUMN `{$column}` {$definition}");
        }
    }

    private static function addIndex(PDO $db, string $table, string $index, array $columns): void
    {
        if (!self::indexExists($db, $table, $index)) {
            $quoted = implode('`,`', $columns);
            $db->exec("ALTER TABLE `{$table}` ADD INDEX `{$index}` (`{$quoted}`)");
        }
    }

    private static function replaceUniqueIndex(
        PDO $db,
        string $table,
        string $index,
        array $columns
    ): void {
        $current = self::indexColumns($db, $table, $index);
        if ($current === $columns) {
            return;
        }
        if ($current) {
            $db->exec("ALTER TABLE `{$table}` DROP INDEX `{$index}`");
        }
        $quoted = implode('`,`', $columns);
        $db->exec("ALTER TABLE `{$table}` ADD UNIQUE INDEX `{$index}` (`{$quoted}`)");
    }

    private static function replacePrimaryKey(PDO $db, string $table, array $columns): void
    {
        $current = self::indexColumns($db, $table, 'PRIMARY');
        if ($current === $columns) {
            return;
        }
        if (in_array('business_id', $columns, true)) {
            $legacyBusiness = self::firstBusinessId($db);
            if ($legacyBusiness === null) {
                return;
            }
            $db->prepare("UPDATE `{$table}` SET business_id = ? WHERE business_id IS NULL")
                ->execute([$legacyBusiness]);
        }
        if ($current) {
            $db->exec("ALTER TABLE `{$table}` DROP PRIMARY KEY");
        }
        $quoted = implode('`,`', $columns);
        $db->exec("ALTER TABLE `{$table}` ADD PRIMARY KEY (`{$quoted}`)");
    }

    private static function firstBusinessId(PDO $db): ?string
    {
        $value = $db->query('SELECT id FROM businesses ORDER BY created_at ASC LIMIT 1')
            ->fetchColumn();
        return $value === false ? null : (string)$value;
    }

    private static function columnExists(PDO $db, string $table, string $column): bool
    {
        $statement = $db->prepare(
            'SELECT 1 FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?'
        );
        $statement->execute([$table, $column]);
        return (bool)$statement->fetchColumn();
    }

    private static function tableExists(PDO $db, string $table): bool
    {
        $statement = $db->prepare(
            'SELECT 1 FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?'
        );
        $statement->execute([$table]);
        return (bool)$statement->fetchColumn();
    }

    private static function indexExists(PDO $db, string $table, string $index): bool
    {
        return self::indexColumns($db, $table, $index) !== [];
    }

    private static function indexColumns(PDO $db, string $table, string $index): array
    {
        $statement = $db->prepare(
            'SELECT COLUMN_NAME FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
             ORDER BY SEQ_IN_INDEX'
        );
        $statement->execute([$table, $index]);
        return array_map(
            static fn(array $row): string => (string)$row['COLUMN_NAME'],
            $statement->fetchAll()
        );
    }

    private static function uuid(): string
    {
        $data = random_bytes(16);
        $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
        $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
    }
}
