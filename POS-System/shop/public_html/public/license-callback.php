<?php
declare(strict_types=1);

require_once __DIR__ . '/../includes/license_payments.php';

$reference = trim((string)($_GET['reference'] ?? $_GET['trxref'] ?? ''));
$order = null;
$error = '';
try {
    $order = verifyAndCompleteLicensePayment($reference);
} catch (Throwable $exception) {
    $error = $exception->getMessage();
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><?= $order ? 'License Ready' : 'Payment Verification' ?> · BizFlow POS</title>
    <meta name="robots" content="noindex">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-slate-950 px-5 py-14 text-slate-900" style="font-family:Inter,sans-serif">
    <main class="mx-auto max-w-2xl rounded-3xl bg-white p-8 text-center shadow-2xl sm:p-12">
        <?php if ($order): ?>
            <span class="mx-auto grid h-20 w-20 place-items-center rounded-full bg-emerald-100 text-4xl text-emerald-700">✓</span>
            <p class="mt-7 text-xs font-extrabold tracking-[.18em] text-emerald-700">PAYMENT CONFIRMED</p>
            <h1 class="mt-3 text-4xl font-extrabold tracking-tight">Your license is ready</h1>
            <p class="mx-auto mt-4 max-w-lg leading-7 text-slate-500">Your BizFlow POS activation details have been generated. Download the file and keep it private.</p>
            <div class="mt-8 rounded-2xl bg-slate-50 p-5 text-left text-sm">
                <div class="flex justify-between gap-5 border-b border-slate-200 pb-3"><span class="text-slate-500">Package</span><strong><?= htmlspecialchars($order['plan_code']) ?></strong></div>
                <div class="flex justify-between gap-5 border-b border-slate-200 py-3"><span class="text-slate-500">Billing</span><strong><?= htmlspecialchars(ucfirst($order['billing_period'])) ?></strong></div>
                <div class="flex justify-between gap-5 pt-3"><span class="text-slate-500">Reference</span><strong class="break-all text-right"><?= htmlspecialchars($order['reference']) ?></strong></div>
            </div>
            <a href="<?= htmlspecialchars(activationDownloadUrl($order)) ?>"
               class="mt-8 inline-flex w-full items-center justify-center rounded-xl bg-blue-600 px-6 py-4 font-extrabold text-white shadow-lg shadow-blue-200 hover:bg-blue-700">
                Download activation details (.txt)
            </a>
            <p class="mt-4 text-xs leading-5 text-slate-500">The file contains your private license key and the correct backend URL.</p>
        <?php else: ?>
            <span class="mx-auto grid h-20 w-20 place-items-center rounded-full bg-amber-100 text-4xl text-amber-700">!</span>
            <p class="mt-7 text-xs font-extrabold tracking-[.18em] text-amber-700">VERIFICATION INCOMPLETE</p>
            <h1 class="mt-3 text-3xl font-extrabold">We could not release the license yet</h1>
            <p class="mt-4 leading-7 text-slate-500"><?= htmlspecialchars($error ?: 'Payment has not been confirmed.') ?></p>
            <a href="licensing.php" class="mt-8 inline-flex rounded-xl bg-slate-900 px-6 py-3.5 font-bold text-white">Return to packages</a>
        <?php endif; ?>
    </main>
</body>
</html>
