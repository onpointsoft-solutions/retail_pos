<?php
/**
 * Retail POS — Password hash generator
 * Run: php tools/generate_password.php
 * Or open in browser: http://localhost/backend/tools/generate_password.php?p=admin123
 *
 * SECURITY: Remove or restrict this file in production.
 */

// Allow CLI or HTTP
$password = '';
if (PHP_SAPI === 'cli') {
    $password = $argv[1] ?? '';
    if (empty($password)) {
        echo "Usage: php generate_password.php <password>\n";
        exit(1);
    }
} else {
    $password = $_GET['p'] ?? '';
    if (empty($password)) {
        echo '<form><input name="p" placeholder="Enter password" size="30"><button>Hash</button></form>';
        exit(0);
    }
}

$hash = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
$verified = password_verify($password, $hash);

if (PHP_SAPI === 'cli') {
    echo "Password : {$password}\n";
    echo "Hash     : {$hash}\n";
    echo "Verified : " . ($verified ? 'YES' : 'NO') . "\n";
    echo "\nSQL UPDATE:\n";
    echo "UPDATE users SET password_hash = '{$hash}' WHERE username = 'admin';\n";
} else {
    header('Content-Type: text/plain');
    echo "Password : {$password}\n";
    echo "Hash     : {$hash}\n";
    echo "Verified : " . ($verified ? 'YES' : 'NO') . "\n\n";
    echo "SQL:\nUPDATE users SET password_hash = '{$hash}' WHERE username = 'admin';\n";
}
