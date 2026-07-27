<?php
declare(strict_types=1);

require_once __DIR__ . '/../services/LicensePaymentService.php';

try {
    $reference = trim((string)($_GET['reference'] ?? ''));
    $token = trim((string)($_GET['token'] ?? ''));
    $order = findDownloadableOrder($reference, $token);
    $path = activationFilePath($order);
    if (!is_file($path) || !is_readable($path)) {
        throw new RuntimeException('The activation file could not be found. Contact support.');
    }
    header('Content-Type: text/plain; charset=utf-8');
    header('Content-Disposition: attachment; filename="' . basename($path) . '"');
    header('Content-Length: ' . filesize($path));
    header('Cache-Control: private, no-store, no-cache, must-revalidate');
    header('Pragma: no-cache');
    header('X-Content-Type-Options: nosniff');
    readfile($path);
    exit;
} catch (Throwable $exception) {
    http_response_code(404);
    header('Content-Type: text/plain; charset=utf-8');
    echo $exception->getMessage();
}
