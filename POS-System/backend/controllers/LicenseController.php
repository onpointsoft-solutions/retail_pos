<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';
require_once __DIR__ . '/../helpers/JwtHelper.php';
require_once __DIR__ . '/../helpers/TenantManager.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class LicenseController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
        $this->ensureSchema();
        TenantManager::ensureSchema($this->db);
    }

    public function plans(array $payload = [], array $params = []): never
    {
        $stmt = $this->db->query(
            'SELECT code, name, description, monthly_price, annual_price, max_devices, features
             FROM license_plans WHERE active = 1 ORDER BY display_order ASC'
        );
        $plans = $stmt->fetchAll();
        foreach ($plans as &$plan) {
            $plan['monthly_price'] = (float)$plan['monthly_price'];
            $plan['annual_price'] = (float)$plan['annual_price'];
            $plan['max_devices'] = (int)$plan['max_devices'];
            $plan['features'] = json_decode((string)$plan['features'], true) ?: [];
        }
        unset($plan);
        Response::json(['currency' => 'KES', 'trial_days' => 30, 'plans' => $plans]);
    }

    public function trial(array $body): never
    {
        try {
            Validator::required($body, ['machine_id']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        $machineId = $this->normalizeMachineId((string)$body['machine_id']);
        $requested = $this->safeDate((string)($body['trial_started_at'] ?? ''))
            ?? new DateTimeImmutable('now', new DateTimeZone('UTC'));
        $now = new DateTimeImmutable('now', new DateTimeZone('UTC'));
        if ($requested > $now) $requested = $now;
        $started = $requested->format('Y-m-d H:i:s');

        $insert = $this->db->prepare(
            'INSERT IGNORE INTO license_trials (machine_id, started_at, expires_at, last_seen_at)
             VALUES (?, ?, DATE_ADD(?, INTERVAL 30 DAY), UTC_TIMESTAMP())'
        );
        $insert->execute([$machineId, $started, $started]);
        $this->db->prepare(
            'UPDATE license_trials SET last_seen_at = UTC_TIMESTAMP() WHERE machine_id = ?'
        )->execute([$machineId]);

        $stmt = $this->db->prepare('SELECT started_at, expires_at FROM license_trials WHERE machine_id = ?');
        $stmt->execute([$machineId]);
        $trial = $stmt->fetch();
        $active = $trial && strtotime($trial['expires_at'] . ' UTC') > time();
        Response::json([
            'status' => $active ? 'TRIAL' : 'EXPIRED',
            'active' => $active,
            'trial_started_at' => $this->isoUtc($trial['started_at'] ?? $started),
            'expires_at' => $this->isoUtc($trial['expires_at'] ?? $started),
            'trial_days' => 30,
        ]);
    }

    public function activate(array $body): never
    {
        try {
            Validator::required($body, ['license_key', 'machine_id']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }
        $license = $this->findUsableLicense((string)$body['license_key']);
        $license['business_id'] = TenantManager::ensureLicenseBusiness($this->db, $license);
        TenantManager::ensureSchema($this->db);
        $machineId = $this->normalizeMachineId((string)$body['machine_id']);

        $this->db->beginTransaction();
        try {
            $existing = $this->db->prepare(
                'SELECT id FROM license_activations WHERE license_id = ? AND machine_id = ? LIMIT 1'
            );
            $existing->execute([$license['id'], $machineId]);
            $activationId = $existing->fetchColumn();
            if (!$activationId) {
                $count = $this->db->prepare(
                    'SELECT COUNT(*) FROM license_activations WHERE license_id = ? AND revoked_at IS NULL'
                );
                $count->execute([$license['id']]);
                if ((int)$count->fetchColumn() >= (int)$license['max_devices']) {
                    $this->db->rollBack();
                    Response::error('This license has reached its workstation limit.', 409);
                }
                $activationId = $this->uuid();
                $insert = $this->db->prepare(
                    'INSERT INTO license_activations
                     (id, license_id, machine_id, device_name, store_name, app_version, activated_at, last_seen_at)
                     VALUES (?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())'
                );
                $insert->execute([
                    $activationId, $license['id'], $machineId,
                    trim((string)($body['device_name'] ?? 'Windows workstation')),
                    trim((string)($body['store_name'] ?? '')),
                    trim((string)($body['app_version'] ?? '2.0.0')),
                ]);
            } else {
                $update = $this->db->prepare(
                    'UPDATE license_activations
                     SET device_name = ?, store_name = ?, app_version = ?,
                         last_seen_at = UTC_TIMESTAMP(), revoked_at = NULL WHERE id = ?'
                );
                $update->execute([
                    trim((string)($body['device_name'] ?? 'Windows workstation')),
                    trim((string)($body['store_name'] ?? '')),
                    trim((string)($body['app_version'] ?? '2.0.0')),
                    $activationId,
                ]);
            }
            $this->db->commit();
        } catch (Throwable $e) {
            if ($this->db->inTransaction()) $this->db->rollBack();
            throw $e;
        }
        $this->respondWithLicense($license, $machineId);
    }

    public function validateLicense(array $body): never
    {
        try {
            Validator::required($body, ['license_key', 'machine_id']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }
        $license = $this->findUsableLicense((string)$body['license_key']);
        $license['business_id'] = TenantManager::ensureLicenseBusiness($this->db, $license);
        TenantManager::ensureSchema($this->db);
        $machineId = $this->normalizeMachineId((string)$body['machine_id']);
        $stmt = $this->db->prepare(
            'SELECT id FROM license_activations
             WHERE license_id = ? AND machine_id = ? AND revoked_at IS NULL LIMIT 1'
        );
        $stmt->execute([$license['id'], $machineId]);
        if (!$stmt->fetchColumn()) {
            Response::error('This workstation is not activated for the supplied license.', 403);
        }
        $this->db->prepare(
            'UPDATE license_activations SET last_seen_at = UTC_TIMESTAMP(), app_version = ?
             WHERE license_id = ? AND machine_id = ?'
        )->execute([trim((string)($body['app_version'] ?? '2.0.0')), $license['id'], $machineId]);
        $this->respondWithLicense($license, $machineId);
    }

    public function issue(array $payload, array $body): never
    {
        $this->requireLicenseAdmin($payload);
        try {
            Validator::required($body, ['plan_code', 'customer_name']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }
        $planCode = strtoupper(trim((string)$body['plan_code']));
        $planStmt = $this->db->prepare(
            'SELECT code, max_devices FROM license_plans WHERE code = ? AND active = 1 LIMIT 1'
        );
        $planStmt->execute([$planCode]);
        $plan = $planStmt->fetch();
        if (!$plan) Response::error('Unknown or inactive license plan.', 422);

        $months = max(1, min(60, (int)($body['months'] ?? 12)));
        $maxDevices = max(1, min(100, (int)($body['max_devices'] ?? $plan['max_devices'])));
        $plainKey = $this->generateLicenseKey();
        $expiresAt = (new DateTimeImmutable('now', new DateTimeZone('UTC')))
            ->modify('+' . $months . ' months')
            ->format('Y-m-d H:i:s');
        $businessId = TenantManager::createBusiness(
            $this->db,
            trim((string)$body['customer_name'])
        );
        TenantManager::ensureSchema($this->db);
        $insert = $this->db->prepare(
            'INSERT INTO licenses
             (id, business_id, license_key_hash, key_prefix, plan_code, customer_name, customer_email,
              customer_phone, status, max_devices, issued_at, expires_at, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, ?)'
        );
        $insert->execute([
            $this->uuid(), $businessId, hash('sha256', $plainKey), substr($plainKey, 0, 14), $planCode,
            trim((string)$body['customer_name']), trim((string)($body['customer_email'] ?? '')),
            trim((string)($body['customer_phone'] ?? '')), 'ACTIVE', $maxDevices, $expiresAt,
            $payload['user_id'] ?? null,
        ]);
        Response::json([
            'message' => 'License issued. Copy the key now; it is not stored in plain text.',
            'license_key' => $plainKey,
            'plan_code' => $planCode,
            'business_id' => $businessId,
            'months' => $months,
            'max_devices' => $maxDevices,
        ], 201);
    }

    public function renew(array $payload, array $body): never
    {
        $this->requireLicenseAdmin($payload);
        try {
            Validator::required($body, ['license_key', 'months']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }
        $months = max(1, min(60, (int)$body['months']));
        $hash = hash('sha256', strtoupper(trim((string)$body['license_key'])));
        $lookup = $this->db->prepare('SELECT expires_at FROM licenses WHERE license_key_hash = ?');
        $lookup->execute([$hash]);
        $currentExpiry = $lookup->fetchColumn();
        if (!$currentExpiry) Response::error('License key not found.', 404);
        $now = new DateTimeImmutable('now', new DateTimeZone('UTC'));
        $current = new DateTimeImmutable((string)$currentExpiry, new DateTimeZone('UTC'));
        $base = $current > $now ? $current : $now;
        $expiresAt = $base->modify('+' . $months . ' months')->format('Y-m-d H:i:s');
        $statement = $this->db->prepare(
            'UPDATE licenses SET status = ?, expires_at = ? WHERE license_key_hash = ?'
        );
        $statement->execute(['ACTIVE', $expiresAt, $hash]);
        Response::json([
            'message' => 'License renewed successfully.',
            'months_added' => $months,
            'expires_at' => $this->isoUtc($expiresAt),
        ]);
    }

    public function revoke(array $payload, array $body): never
    {
        $this->requireLicenseAdmin($payload);
        try {
            Validator::required($body, ['license_key']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }
        $hash = hash('sha256', strtoupper(trim((string)$body['license_key'])));
        $lookup = $this->db->prepare('SELECT id FROM licenses WHERE license_key_hash = ?');
        $lookup->execute([$hash]);
        $licenseId = $lookup->fetchColumn();
        if (!$licenseId) Response::error('License key not found.', 404);
        $this->db->beginTransaction();
        try {
            $this->db->prepare('UPDATE licenses SET status = ? WHERE id = ?')
                ->execute(['REVOKED', $licenseId]);
            $this->db->prepare(
                'UPDATE license_activations SET revoked_at = UTC_TIMESTAMP() WHERE license_id = ?'
            )->execute([$licenseId]);
            $this->db->commit();
        } catch (Throwable $e) {
            if ($this->db->inTransaction()) $this->db->rollBack();
            throw $e;
        }
        Response::json(['message' => 'License revoked.']);
    }

    private function findUsableLicense(string $plainKey): array
    {
        $stmt = $this->db->prepare(
            'SELECT l.*, p.name AS plan_name FROM licenses l
             JOIN license_plans p ON p.code = l.plan_code
             WHERE l.license_key_hash = ? LIMIT 1'
        );
        $stmt->execute([hash('sha256', strtoupper(trim($plainKey)))]);
        $license = $stmt->fetch();
        if (!$license) Response::error('Invalid license key.', 404);
        if (strtoupper((string)$license['status']) !== 'ACTIVE') {
            Response::error('This license is suspended or revoked.', 403);
        }
        if (strtotime($license['expires_at'] . ' UTC') <= time()) {
            Response::error('This license has expired. Please renew to continue.', 402);
        }
        return $license;
    }

    private function requireLicenseAdmin(array $payload): void
    {
        if (strtoupper((string)($payload['role'] ?? '')) !== 'ADMIN'
            || ($payload['user_id'] ?? 'system') === 'system') {
            Response::error('A signed-in backend administrator is required to manage licenses.', 403);
        }
    }

    private function respondWithLicense(array $license, string $machineId): never
    {
        $count = $this->db->prepare(
            'SELECT COUNT(*) FROM license_activations WHERE license_id = ? AND revoked_at IS NULL'
        );
        $count->execute([$license['id']]);
        $businessId = TenantManager::ensureLicenseBusiness($this->db, $license);
        $syncToken = JwtHelper::encode([
            'user_id' => 'license:' . $license['id'],
            'username' => 'licensed-workstation',
            'role' => 'SYNC',
            'business_id' => $businessId,
            'license_id' => $license['id'],
            'machine_id' => $machineId,
        ]);
        Response::json([
            'status' => 'ACTIVE',
            'active' => true,
            'plan_code' => $license['plan_code'],
            'plan_name' => $license['plan_name'],
            'expires_at' => $this->isoUtc($license['expires_at']),
            'max_devices' => (int)$license['max_devices'],
            'activated_devices' => (int)$count->fetchColumn(),
            'machine_id' => $machineId,
            'business_id' => $businessId,
            'sync_token' => $syncToken,
            'sync_token_expires_in' => JWT_EXPIRY,
            'next_check_hours' => 24,
            'offline_grace_days' => 7,
        ]);
    }

    private function ensureSchema(): void
    {
        $this->db->exec('CREATE TABLE IF NOT EXISTS license_plans (
            code VARCHAR(30) NOT NULL PRIMARY KEY, name VARCHAR(80) NOT NULL,
            description VARCHAR(255) NULL, monthly_price DECIMAL(12,2) NOT NULL,
            annual_price DECIMAL(12,2) NOT NULL, max_devices INT NOT NULL DEFAULT 1,
            features JSON NULL, active TINYINT(1) NOT NULL DEFAULT 1,
            display_order INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
        $this->db->exec('CREATE TABLE IF NOT EXISTS licenses (
            id VARCHAR(36) NOT NULL PRIMARY KEY, license_key_hash CHAR(64) NOT NULL,
            key_prefix VARCHAR(20) NOT NULL, plan_code VARCHAR(30) NOT NULL,
            customer_name VARCHAR(150) NOT NULL, customer_email VARCHAR(190) NULL,
            customer_phone VARCHAR(40) NULL, status VARCHAR(20) NOT NULL DEFAULT \'ACTIVE\',
            max_devices INT NOT NULL DEFAULT 1, issued_at DATETIME NOT NULL, expires_at DATETIME NOT NULL,
            created_by VARCHAR(36) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uq_license_hash (license_key_hash), INDEX idx_license_plan (plan_code),
            INDEX idx_license_status (status), INDEX idx_license_expiry (expires_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
        $this->db->exec('CREATE TABLE IF NOT EXISTS license_activations (
            id VARCHAR(36) NOT NULL PRIMARY KEY, license_id VARCHAR(36) NOT NULL,
            machine_id CHAR(64) NOT NULL, device_name VARCHAR(150) NULL,
            store_name VARCHAR(150) NULL, app_version VARCHAR(30) NULL,
            activated_at DATETIME NOT NULL, last_seen_at DATETIME NOT NULL, revoked_at DATETIME NULL,
            UNIQUE KEY uq_license_machine (license_id, machine_id), INDEX idx_activation_machine (machine_id),
            CONSTRAINT fk_activation_license FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
        $this->db->exec('CREATE TABLE IF NOT EXISTS license_trials (
            machine_id CHAR(64) NOT NULL PRIMARY KEY, started_at DATETIME NOT NULL,
            expires_at DATETIME NOT NULL, last_seen_at DATETIME NOT NULL,
            INDEX idx_trial_expiry (expires_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

        $seed = $this->db->prepare(
            'INSERT INTO license_plans
             (code, name, description, monthly_price, annual_price, max_devices, features, display_order)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description),
             monthly_price = VALUES(monthly_price), annual_price = VALUES(annual_price),
             max_devices = VALUES(max_devices), features = VALUES(features), display_order = VALUES(display_order)'
        );
        $plans = [
            ['STARTER', 'Starter', 'For a single growing shop', 2500, 25000, 1,
                ['Complete POS and inventory', 'Professional receipts and reports', 'Product image sync', 'Email support'], 1],
            ['BUSINESS', 'Business', 'For established shops and small chains', 5500, 55000, 5,
                ['Everything in Starter', 'Up to 5 synchronized computers', 'M-Pesa Bridge transactions', 'Priority WhatsApp support'], 2],
            ['ENTERPRISE', 'Enterprise', 'For multi-branch retail operations', 12000, 120000, 20,
                ['Everything in Business', 'Up to 20 synchronized computers', 'Multi-branch deployment support', 'Priority onboarding and backups'], 3],
        ];
        foreach ($plans as $plan) {
            [$code, $name, $description, $monthly, $annual, $devices, $features, $order] = $plan;
            $seed->execute([$code, $name, $description, $monthly, $annual, $devices, json_encode($features), $order]);
        }
    }

    private function normalizeMachineId(string $machineId): string
    {
        $machineId = strtolower(trim($machineId));
        if (!preg_match('/^[a-f0-9]{64}$/', $machineId)) {
            Response::error('Invalid workstation fingerprint.', 422);
        }
        return $machineId;
    }

    private function generateLicenseKey(): string
    {
        $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
        $parts = [];
        for ($group = 0; $group < 4; $group++) {
            $part = '';
            for ($index = 0; $index < 5; $index++) {
                $part .= $alphabet[random_int(0, strlen($alphabet) - 1)];
            }
            $parts[] = $part;
        }
        return 'BIZF-' . implode('-', $parts);
    }

    private function safeDate(string $value): ?DateTimeImmutable
    {
        if ($value === '') return null;
        try {
            return new DateTimeImmutable($value, new DateTimeZone('UTC'));
        } catch (Throwable) {
            return null;
        }
    }

    private function isoUtc(string $value): string
    {
        return (new DateTimeImmutable($value, new DateTimeZone('UTC')))
            ->setTimezone(new DateTimeZone('UTC'))->format(DateTimeInterface::ATOM);
    }

    private function uuid(): string
    {
        $data = random_bytes(16);
        $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
        $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
    }
}
