<?php
declare(strict_types=1);

if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit('This tool is available from the cPanel terminal only.');
}

if ($argc < 4) {
    fwrite(STDERR, "Usage: php issue_license.php PLAN \"Customer Name\" MONTHS [MAX_DEVICES] [EMAIL] [PHONE]\n");
    fwrite(STDERR, "Example: php issue_license.php BUSINESS \"Acme Stores\" 12 5 owner@example.com 0712345678\n");
    exit(1);
}

require_once __DIR__ . '/../controllers/LicenseController.php';

$controller = new LicenseController();
$controller->issue(
    ['user_id' => 'cpanel-cli', 'username' => 'cpanel-cli', 'role' => 'ADMIN'],
    [
        'plan_code' => $argv[1],
        'customer_name' => $argv[2],
        'months' => (int)$argv[3],
        'max_devices' => isset($argv[4]) ? (int)$argv[4] : null,
        'customer_email' => $argv[5] ?? '',
        'customer_phone' => $argv[6] ?? '',
    ]
);
