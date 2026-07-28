<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/config.php';
require_once __DIR__ . '/../config/database.php';

function licensePaymentDb(): PDO
{
    static $connection = null;
    if ($connection instanceof PDO) {
        return $connection;
    }
    $connection = Database::getConnection();
    ensureLicensePaymentSchema($connection);
    return $connection;
}

function ensureLicensePaymentSchema(PDO $db): void
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
    $db->exec(
        'CREATE TABLE IF NOT EXISTS license_plans (
            code VARCHAR(30) NOT NULL PRIMARY KEY,
            name VARCHAR(80) NOT NULL,
            description VARCHAR(255) NULL,
            monthly_price DECIMAL(12,2) NOT NULL,
            annual_price DECIMAL(12,2) NOT NULL,
            max_devices INT NOT NULL DEFAULT 1,
            features JSON NULL,
            active TINYINT(1) NOT NULL DEFAULT 1,
            display_order INT NOT NULL DEFAULT 0,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    );
    $db->exec(
        'CREATE TABLE IF NOT EXISTS licenses (
            id VARCHAR(36) NOT NULL PRIMARY KEY,
            business_id VARCHAR(36) NULL,
            license_key_hash CHAR(64) NOT NULL,
            key_prefix VARCHAR(20) NOT NULL,
            plan_code VARCHAR(30) NOT NULL,
            customer_name VARCHAR(150) NOT NULL,
            customer_email VARCHAR(190) NULL,
            customer_phone VARCHAR(40) NULL,
            status VARCHAR(20) NOT NULL DEFAULT "ACTIVE",
            max_devices INT NOT NULL DEFAULT 1,
            issued_at DATETIME NOT NULL,
            expires_at DATETIME NOT NULL,
            created_by VARCHAR(36) NULL,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uq_license_hash (license_key_hash),
            INDEX idx_license_business (business_id),
            INDEX idx_license_plan (plan_code),
            INDEX idx_license_status (status),
            INDEX idx_license_expiry (expires_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    );
    if (!licensePaymentColumnExists($db, 'licenses', 'business_id')) {
        $db->exec('ALTER TABLE licenses ADD COLUMN business_id VARCHAR(36) NULL AFTER id');
        $db->exec('ALTER TABLE licenses ADD INDEX idx_license_business (business_id)');
    }
    $db->exec(
        'CREATE TABLE IF NOT EXISTS license_orders (
            id VARCHAR(36) NOT NULL PRIMARY KEY,
            reference VARCHAR(80) NOT NULL,
            plan_code VARCHAR(30) NOT NULL,
            billing_period VARCHAR(10) NOT NULL,
            customer_name VARCHAR(150) NOT NULL,
            customer_email VARCHAR(190) NOT NULL,
            customer_phone VARCHAR(40) NULL,
            amount_subunit BIGINT NOT NULL,
            currency CHAR(3) NOT NULL DEFAULT "KES",
            status VARCHAR(20) NOT NULL DEFAULT "PENDING",
            paystack_transaction_id VARCHAR(30) NULL,
            paystack_channel VARCHAR(40) NULL,
            license_id VARCHAR(36) NULL,
            business_id VARCHAR(36) NULL,
            activation_file VARCHAR(255) NULL,
            paid_at DATETIME NULL,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uq_license_order_reference (reference),
            INDEX idx_license_order_email (customer_email),
            INDEX idx_license_order_status (status),
            INDEX idx_license_order_created (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    );

    $seed = $db->prepare(
        'INSERT INTO license_plans
         (code, name, description, monthly_price, annual_price, max_devices, features, display_order)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description),
         monthly_price = VALUES(monthly_price), annual_price = VALUES(annual_price),
         max_devices = VALUES(max_devices), features = VALUES(features),
         display_order = VALUES(display_order)'
    );
    $plans = [
        ['STARTER', 'Starter', 'For a single growing shop', 2500, 1, 1,
            ['Complete POS and inventory', 'Professional receipts and reports', 'Product image sync'], 1],
        ['BUSINESS', 'Business', 'For established shops and small chains', 5500, 55000, 5,
            ['Everything in Starter', 'Up to 5 synchronized computers', 'M-Pesa Bridge transactions'], 2],
        ['ENTERPRISE', 'Enterprise', 'For multi-branch retail operations', 12000, 120000, 20,
            ['Everything in Business', 'Up to 20 synchronized computers', 'Priority onboarding'], 3],
    ];
    foreach ($plans as $plan) {
        [$code, $name, $description, $monthly, $annual, $devices, $features, $order] = $plan;
        $seed->execute([
            $code, $name, $description, $monthly, $annual, $devices,
            json_encode($features, JSON_UNESCAPED_SLASHES), $order,
        ]);
    }
}

function licensePlans(): array
{
    $statement = licensePaymentDb()->query(
        'SELECT code, name, description, monthly_price, annual_price, max_devices, features
         FROM license_plans WHERE active = 1 ORDER BY display_order'
    );
    $plans = [];
    foreach ($statement->fetchAll() as $plan) {
        $plan['monthly_price'] = (float)$plan['monthly_price'];
        $plan['annual_price'] = (float)$plan['annual_price'];
        $plan['max_devices'] = (int)$plan['max_devices'];
        $plan['features'] = json_decode((string)$plan['features'], true) ?: [];
        $plans[$plan['code']] = $plan;
    }
    return $plans;
}

function initializeLicensePayment(array $input): string
{
    $secret = paystackSecretKey();
    $plans = licensePlans();
    $planCode = strtoupper(trim((string)($input['plan_code'] ?? '')));
    $period = strtolower(trim((string)($input['billing_period'] ?? 'annual')));
    if (!isset($plans[$planCode])) {
        throw new InvalidArgumentException('Select a valid BizFlow POS package.');
    }
    if (!in_array($period, ['monthly', 'annual'], true)) {
        throw new InvalidArgumentException('Select monthly or annual billing.');
    }

    $name = trim((string)($input['customer_name'] ?? ''));
    $email = strtolower(trim((string)($input['customer_email'] ?? '')));
    $phone = trim((string)($input['customer_phone'] ?? ''));
    if ($name === '' || strlen($name) > 150) {
        throw new InvalidArgumentException('Enter the business or customer name.');
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($email) > 190) {
        throw new InvalidArgumentException('Enter a valid email address.');
    }
    if ($phone === '' || strlen($phone) > 40) {
        throw new InvalidArgumentException('Enter a valid phone number.');
    }

    $plan = $plans[$planCode];
    $price = $period === 'annual' ? $plan['annual_price'] : $plan['monthly_price'];
    $amountSubunit = (int)round($price * 100);
    $reference = 'BIZFLOW-' . gmdate('YmdHis') . '-' . strtoupper(bin2hex(random_bytes(6)));
    $orderId = licenseUuid();

    $db = licensePaymentDb();
    $insert = $db->prepare(
        'INSERT INTO license_orders
         (id, reference, plan_code, billing_period, customer_name, customer_email,
          customer_phone, amount_subunit, currency, status)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, "KES", "PENDING")'
    );
    $insert->execute([
        $orderId, $reference, $planCode, $period, $name, $email, $phone, $amountSubunit,
    ]);

    try {
        $response = paystackRequest('POST', '/transaction/initialize', [
            'email' => $email,
            'amount' => (string)$amountSubunit,
            'currency' => 'KES',
            'reference' => $reference,
            'callback_url' => licensePublicUrl('license-callback.php'),
            'metadata' => [
                'order_id' => $orderId,
                'plan_code' => $planCode,
                'billing_period' => $period,
                'customer_name' => $name,
                'customer_phone' => $phone,
                'cancel_action' => licensePublicUrl('licensing.php?payment=cancelled'),
            ],
        ], $secret);
    } catch (Throwable $exception) {
        $db->prepare('UPDATE license_orders SET status = "FAILED" WHERE id = ?')
            ->execute([$orderId]);
        throw $exception;
    }

    $authorizationUrl = trim((string)($response['data']['authorization_url'] ?? ''));
    if ($authorizationUrl === '' || !str_starts_with($authorizationUrl, 'https://')) {
        throw new RuntimeException('Paystack did not return a secure checkout URL.');
    }
    return $authorizationUrl;
}

function verifyAndCompleteLicensePayment(string $reference): array
{
    if (!preg_match('/^[A-Za-z0-9.=-]{8,80}$/', $reference)) {
        throw new InvalidArgumentException('Invalid payment reference.');
    }
    $response = paystackRequest(
        'GET',
        '/transaction/verify/' . rawurlencode($reference),
        null,
        paystackSecretKey()
    );
    return completeLicensePayment($response['data'] ?? []);
}

function completeLicensePayment(array $transaction): array
{
    $reference = trim((string)($transaction['reference'] ?? ''));
    if ($reference === '') {
        throw new RuntimeException('Payment reference is missing.');
    }

    $db = licensePaymentDb();
    $lookup = $db->prepare('SELECT * FROM license_orders WHERE reference = ? LIMIT 1');
    $lookup->execute([$reference]);
    $order = $lookup->fetch();
    if (!$order) {
        throw new RuntimeException('This payment does not match a BizFlow POS order.');
    }
    if (($transaction['status'] ?? '') !== 'success') {
        throw new RuntimeException('Paystack has not confirmed this payment as successful.');
    }
    if (strtoupper((string)($transaction['currency'] ?? '')) !== 'KES') {
        throw new RuntimeException('Payment currency does not match the license order.');
    }
    if ((int)($transaction['amount'] ?? 0) !== (int)$order['amount_subunit']) {
        throw new RuntimeException('Payment amount does not match the selected package.');
    }
    $paidEmail = strtolower(trim((string)($transaction['customer']['email'] ?? '')));
    if ($paidEmail !== '' && $paidEmail !== strtolower((string)$order['customer_email'])) {
        throw new RuntimeException('Payment customer does not match the license order.');
    }

    $db->beginTransaction();
    $activationPath = null;
    try {
        $locked = $db->prepare('SELECT * FROM license_orders WHERE reference = ? FOR UPDATE');
        $locked->execute([$reference]);
        $order = $locked->fetch();
        if (!$order) {
            throw new RuntimeException('License order was not found.');
        }
        if ($order['status'] === 'PAID' && $order['license_id']) {
            $db->commit();
            return $order;
        }

        $planStatement = $db->prepare(
            'SELECT code, name, max_devices FROM license_plans
             WHERE code = ? AND active = 1 LIMIT 1'
        );
        $planStatement->execute([$order['plan_code']]);
        $plan = $planStatement->fetch();
        if (!$plan) {
            throw new RuntimeException('The selected license package is no longer available.');
        }

        $businessId = licenseUuid();
        $licenseId = licenseUuid();
        $licenseKey = generateLicenseKey();
        $months = $order['billing_period'] === 'annual' ? 12 : 1;
        $issuedAt = new DateTimeImmutable('now', new DateTimeZone('UTC'));
        $expiresAt = $issuedAt->modify("+{$months} months");

        $db->prepare('INSERT INTO businesses (id, name) VALUES (?, ?)')
            ->execute([$businessId, $order['customer_name']]);
        $db->prepare(
            'INSERT INTO licenses
             (id, business_id, license_key_hash, key_prefix, plan_code, customer_name,
              customer_email, customer_phone, status, max_devices, issued_at, expires_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, "ACTIVE", ?, ?, ?)'
        )->execute([
            $licenseId,
            $businessId,
            hash('sha256', $licenseKey),
            substr($licenseKey, 0, 14),
            $plan['code'],
            $order['customer_name'],
            $order['customer_email'],
            $order['customer_phone'],
            (int)$plan['max_devices'],
            $issuedAt->format('Y-m-d H:i:s'),
            $expiresAt->format('Y-m-d H:i:s'),
        ]);

        $activationPath = writeActivationFile(
            $order,
            $plan,
            $licenseKey,
            $businessId,
            $issuedAt,
            $expiresAt,
            $transaction
        );
        $update = $db->prepare(
            'UPDATE license_orders SET status = "PAID", paystack_transaction_id = ?,
             paystack_channel = ?, license_id = ?, business_id = ?, activation_file = ?,
             paid_at = ?, updated_at = UTC_TIMESTAMP() WHERE id = ?'
        );
        $update->execute([
            (string)($transaction['id'] ?? ''),
            (string)($transaction['channel'] ?? ''),
            $licenseId,
            $businessId,
            basename($activationPath),
            normalizePaystackDate((string)($transaction['paid_at'] ?? '')),
            $order['id'],
        ]);
        $db->commit();
    } catch (Throwable $exception) {
        if ($db->inTransaction()) {
            $db->rollBack();
        }
        if ($activationPath && is_file($activationPath)) {
            @unlink($activationPath);
        }
        throw $exception;
    }

    $lookup->execute([$reference]);
    return $lookup->fetch() ?: $order;
}

function activationDownloadUrl(array $order): string
{
    $reference = (string)$order['reference'];
    $signature = hash_hmac('sha256', $reference, licenseDownloadSecret());
    return licensePublicUrl(
        'license-download.php?reference=' . rawurlencode($reference)
        . '&token=' . rawurlencode($signature)
    );
}

function findDownloadableOrder(string $reference, string $token): array
{
    $expected = hash_hmac('sha256', $reference, licenseDownloadSecret());
    if (!hash_equals($expected, $token)) {
        throw new RuntimeException('Invalid activation download link.');
    }
    $statement = licensePaymentDb()->prepare(
        'SELECT * FROM license_orders
         WHERE reference = ? AND status = "PAID" AND activation_file IS NOT NULL LIMIT 1'
    );
    $statement->execute([$reference]);
    $order = $statement->fetch();
    if (!$order) {
        throw new RuntimeException('Activation details are not available.');
    }
    return $order;
}

function activationFilePath(array $order): string
{
    $file = basename((string)$order['activation_file']);
    return activationStorageDirectory() . DIRECTORY_SEPARATOR . $file;
}

function verifyPaystackWebhook(string $payload, string $signature): bool
{
    if ($signature === '') {
        return false;
    }
    return hash_equals(
        hash_hmac('sha512', $payload, paystackSecretKey()),
        strtolower(trim($signature))
    );
}

function paystackRequest(
    string $method,
    string $path,
    ?array $body,
    string $secret
): array {
    if (!function_exists('curl_init')) {
        throw new RuntimeException('The PHP cURL extension is required for Paystack.');
    }
    $curl = curl_init('https://api.paystack.co' . $path);
    $headers = [
        'Authorization: Bearer ' . $secret,
        'Accept: application/json',
        'Content-Type: application/json',
        'Cache-Control: no-cache',
    ];
    curl_setopt_array($curl, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CUSTOMREQUEST => strtoupper($method),
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
    ]);
    if ($body !== null) {
        curl_setopt(
            $curl,
            CURLOPT_POSTFIELDS,
            json_encode($body, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES)
        );
    }
    $raw = curl_exec($curl);
    $status = (int)curl_getinfo($curl, CURLINFO_HTTP_CODE);
    $error = curl_error($curl);
    curl_close($curl);
    if ($raw === false || $error !== '') {
        throw new RuntimeException('Could not connect to Paystack: ' . $error);
    }
    $response = json_decode($raw, true);
    if (!is_array($response)) {
        throw new RuntimeException('Paystack returned an invalid response.');
    }
    if ($status < 200 || $status >= 300 || ($response['status'] ?? false) !== true) {
        throw new RuntimeException(
            'Paystack error: ' . (string)($response['message'] ?? "HTTP {$status}")
        );
    }
    return $response;
}

function writeActivationFile(
    array $order,
    array $plan,
    string $licenseKey,
    string $businessId,
    DateTimeImmutable $issuedAt,
    DateTimeImmutable $expiresAt,
    array $transaction
): string {
    $directory = activationStorageDirectory();
    if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) {
        throw new RuntimeException('Could not create activation storage.');
    }
    $filename = sprintf(
        'BizFlow_POS_Activation_%s_%s.txt',
        preg_replace('/[^A-Za-z0-9_-]/', '', (string)$order['reference']),
        $issuedAt->format('Ymd_His')
    );
    $contents = implode(PHP_EOL, [
        'BIZFLOW POS - LICENSE ACTIVATION DETAILS',
        '========================================',
        '',
        'Business: ' . $order['customer_name'],
        'Email: ' . $order['customer_email'],
        'Phone: ' . $order['customer_phone'],
        'Package: ' . $plan['name'] . ' (' . $plan['code'] . ')',
        'Billing: ' . ucfirst((string)$order['billing_period']),
        'Licensed computers: ' . $plan['max_devices'],
        'Amount paid: KES ' . number_format(((int)$order['amount_subunit']) / 100, 2),
        'Paystack reference: ' . $order['reference'],
        'Payment channel: ' . (string)($transaction['channel'] ?? 'Paystack'),
        'Issued (UTC): ' . $issuedAt->format('Y-m-d H:i:s'),
        'Expires (UTC): ' . $expiresAt->format('Y-m-d H:i:s'),
        'Business ID: ' . $businessId,
        '',
        'LICENSE KEY',
        $licenseKey,
        '',
        'ACTIVATION',
        '1. Open BizFlow POS.',
        '2. Open License Management.',
        '3. Backend URL: ' . bizflowBackendUrl(),
        '4. Enter the license key exactly as shown above.',
        '5. Use the same key on permitted computers for this business.',
        '',
        'Keep this file private. It grants access to your BizFlow POS license.',
        'Support: ' . (getenv('BIZFLOW_SUPPORT_EMAIL') ?: 'support@mobilemealscenter.co.ke'),
        '',
    ]);
    $path = $directory . DIRECTORY_SEPARATOR . $filename;
    if (file_put_contents($path, $contents, LOCK_EX) === false) {
        throw new RuntimeException('Could not save activation details.');
    }
    @chmod($path, 0640);
    return $path;
}

function activationStorageDirectory(): string
{
    $configured = trim((string)(getenv('LICENSE_FILE_DIR') ?: ''));
    return $configured !== ''
        ? rtrim($configured, '/\\')
        : dirname(__DIR__) . DIRECTORY_SEPARATOR . 'storage'
            . DIRECTORY_SEPARATOR . 'license-activations';
}

function bizflowBackendUrl(): string
{
    $url = trim((string)(
        getenv('PUBLIC_API_URL') ?: 'https://pos.mobilemealscenter.co.ke/api/'
    ));
    return rtrim($url, '/') . '/';
}

function licensePaymentColumnExists(PDO $db, string $table, string $column): bool
{
    $statement = $db->prepare(
        'SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?'
    );
    $statement->execute([$table, $column]);
    return (bool)$statement->fetchColumn();
}

function paystackSecretKey(): string
{
    $secrets = licensePrivateSecrets();
    $secret = trim((string)(
        getenv('PAYSTACK_SECRET_KEY')
        ?: ($secrets['paystack_secret_key'] ?? '')
    ));
    if ($secret === '' || !str_starts_with($secret, 'sk_')) {
        throw new RuntimeException(
            'Paystack is not configured. Set PAYSTACK_SECRET_KEY or create '
            . 'backend/config/secrets.php from secrets.example.php.'
        );
    }
    return $secret;
}

function licenseDownloadSecret(): string
{
    $secrets = licensePrivateSecrets();
    $secret = trim((string)(
        getenv('LICENSE_DOWNLOAD_SECRET')
        ?: ($secrets['license_download_secret'] ?? '')
    ));
    return $secret !== '' ? $secret : paystackSecretKey();
}

function licensePrivateSecrets(): array
{
    static $secrets = null;
    if (is_array($secrets)) {
        return $secrets;
    }

    $path = dirname(__DIR__) . DIRECTORY_SEPARATOR . 'config'
        . DIRECTORY_SEPARATOR . 'secrets.php';
    if (!is_file($path)) {
        $secrets = [];
        return $secrets;
    }

    $loaded = require $path;
    $secrets = is_array($loaded) ? $loaded : [];
    return $secrets;
}

function licensePublicUrl(string $path): string
{
    $base = trim((string)(
        getenv('LICENSE_SITE_URL') ?: 'https://pos.mobilemealscenter.co.ke/public'
    ));
    return rtrim($base, '/') . '/' . ltrim($path, '/');
}

function normalizePaystackDate(string $value): string
{
    $timestamp = strtotime($value);
    return $timestamp === false
        ? gmdate('Y-m-d H:i:s')
        : gmdate('Y-m-d H:i:s', $timestamp);
}

function generateLicenseKey(): string
{
    $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    $groups = [];
    for ($group = 0; $group < 4; $group++) {
        $part = '';
        for ($index = 0; $index < 5; $index++) {
            $part .= $alphabet[random_int(0, strlen($alphabet) - 1)];
        }
        $groups[] = $part;
    }
    return 'BIZF-' . implode('-', $groups);
}

function licenseUuid(): string
{
    $data = random_bytes(16);
    $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
    $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
    return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
}

function licenseCsrfToken(): string
{
    startLicenseSession();
    if (empty($_SESSION['license_csrf'])) {
        $_SESSION['license_csrf'] = bin2hex(random_bytes(24));
    }
    return (string)$_SESSION['license_csrf'];
}

function validateLicenseCsrf(string $token): void
{
    startLicenseSession();
    if (empty($_SESSION['license_csrf'])
        || !hash_equals((string)$_SESSION['license_csrf'], $token)) {
        throw new RuntimeException('Your checkout session expired. Please try again.');
    }
}

function startLicenseSession(): void
{
    if (session_status() === PHP_SESSION_ACTIVE) {
        return;
    }
    session_name('bizflow_license_checkout');
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/',
        'secure' => true,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}
