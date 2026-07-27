<?php
declare(strict_types=1);

require_once __DIR__ . '/../services/LicensePaymentService.php';

$payload = file_get_contents('php://input') ?: '';
$signature = (string)($_SERVER['HTTP_X_PAYSTACK_SIGNATURE'] ?? '');

if (!verifyPaystackWebhook($payload, $signature)) {
    http_response_code(401);
    exit;
}

$event = json_decode($payload, true);
if (!is_array($event)) {
    http_response_code(400);
    exit;
}

try {
    if (($event['event'] ?? '') === 'charge.success') {
        completeLicensePayment($event['data'] ?? []);
    }
    http_response_code(200);
    header('Content-Type: application/json');
    echo '{"received":true}';
} catch (Throwable $exception) {
    error_log('BizFlow Paystack webhook failed: ' . $exception->getMessage());
    http_response_code(500);
    echo '{"received":false}';
}
