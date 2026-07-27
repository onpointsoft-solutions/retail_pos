<?php
declare(strict_types=1);

if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit('This tool is available from the cPanel terminal only.');
}
if ($argc < 3) {
    fwrite(STDERR, "Usage: php renew_license.php LICENSE_KEY MONTHS\n");
    exit(1);
}

require_once __DIR__ . '/../controllers/LicenseController.php';
(new LicenseController())->renew(
    ['user_id' => 'cpanel-cli', 'username' => 'cpanel-cli', 'role' => 'ADMIN'],
    ['license_key' => $argv[1], 'months' => (int)$argv[2]]
);
