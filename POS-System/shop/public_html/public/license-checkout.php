<?php
declare(strict_types=1);

require_once __DIR__ . '/../includes/license_payments.php';

$plans = licensePlans();
$planCode = strtoupper(trim((string)($_POST['plan_code'] ?? $_GET['plan'] ?? 'BUSINESS')));
$period = strtolower(trim((string)($_POST['billing_period'] ?? $_GET['period'] ?? 'annual')));
if (!isset($plans[$planCode])) {
    $planCode = 'BUSINESS';
}
if (!in_array($period, ['monthly', 'annual'], true)) {
    $period = 'annual';
}
$plan = $plans[$planCode];
$error = '';
$values = [
    'customer_name' => trim((string)($_POST['customer_name'] ?? '')),
    'customer_email' => trim((string)($_POST['customer_email'] ?? '')),
    'customer_phone' => trim((string)($_POST['customer_phone'] ?? '')),
];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        validateLicenseCsrf((string)($_POST['csrf_token'] ?? ''));
        $checkoutUrl = initializeLicensePayment($_POST);
        header('Location: ' . $checkoutUrl, true, 303);
        exit;
    } catch (Throwable $exception) {
        $error = $exception->getMessage();
    }
}

$price = $period === 'annual' ? $plan['annual_price'] : $plan['monthly_price'];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Secure BizFlow POS Checkout</title>
    <meta name="robots" content="noindex">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <script>tailwind.config={theme:{extend:{colors:{brand:'#2563EB',ink:'#0F172A'}}}}</script>
</head>
<body class="min-h-screen bg-slate-50 text-ink antialiased" style="font-family:Inter,sans-serif">
    <header class="border-b border-slate-200 bg-white">
        <div class="mx-auto flex h-20 max-w-6xl items-center justify-between px-5">
            <a href="licensing.php" class="flex items-center gap-3 font-extrabold">
                <span class="grid h-11 w-11 place-items-center rounded-2xl bg-brand text-white">BF</span>
                <span>BizFlow POS</span>
            </a>
            <span class="flex items-center gap-2 text-sm font-semibold text-slate-500">
                <svg class="h-4 w-4 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2h-1V7a5 5 0 0 0-10 0v3H6a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2Z"/>
                </svg>
                Secure Paystack checkout
            </span>
        </div>
    </header>

    <main class="mx-auto grid max-w-6xl gap-8 px-5 py-12 lg:grid-cols-[1fr_420px]">
        <section class="rounded-3xl border border-slate-200 bg-white p-7 shadow-sm sm:p-10">
            <p class="text-xs font-extrabold tracking-[.18em] text-brand">LICENSE DETAILS</p>
            <h1 class="mt-3 text-3xl font-extrabold tracking-tight">Complete your purchase</h1>
            <p class="mt-3 text-slate-500">Your activation file is generated immediately after Paystack confirms payment.</p>

            <?php if ($error !== ''): ?>
                <div class="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
                    <?= htmlspecialchars($error) ?>
                </div>
            <?php endif; ?>

            <form method="post" class="mt-8 space-y-5">
                <input type="hidden" name="csrf_token" value="<?= htmlspecialchars(licenseCsrfToken()) ?>">
                <input type="hidden" name="plan_code" value="<?= htmlspecialchars($planCode) ?>">
                <input type="hidden" name="billing_period" value="<?= htmlspecialchars($period) ?>">

                <label class="block">
                    <span class="text-sm font-bold text-slate-700">Business or customer name</span>
                    <input name="customer_name" required maxlength="150" autocomplete="organization"
                           value="<?= htmlspecialchars($values['customer_name']) ?>"
                           class="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3.5 outline-none transition focus:border-brand focus:ring-4 focus:ring-blue-100">
                </label>
                <div class="grid gap-5 sm:grid-cols-2">
                    <label class="block">
                        <span class="text-sm font-bold text-slate-700">Email address</span>
                        <input type="email" name="customer_email" required maxlength="190" autocomplete="email"
                               value="<?= htmlspecialchars($values['customer_email']) ?>"
                               class="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3.5 outline-none transition focus:border-brand focus:ring-4 focus:ring-blue-100">
                    </label>
                    <label class="block">
                        <span class="text-sm font-bold text-slate-700">Phone number</span>
                        <input type="tel" name="customer_phone" required maxlength="40" autocomplete="tel"
                               placeholder="+254 7XX XXX XXX" value="<?= htmlspecialchars($values['customer_phone']) ?>"
                               class="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3.5 outline-none transition focus:border-brand focus:ring-4 focus:ring-blue-100">
                    </label>
                </div>

                <label class="flex items-start gap-3 rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                    <input type="checkbox" required class="mt-1 h-4 w-4 rounded border-slate-300 text-brand">
                    <span>I confirm these details are correct and understand the activation key must be kept private.</span>
                </label>

                <button class="flex w-full items-center justify-center rounded-xl bg-brand px-6 py-4 font-extrabold text-white shadow-lg shadow-blue-200 transition hover:bg-blue-700">
                    Pay KES <?= number_format($price) ?> securely
                </button>
                <p class="text-center text-xs leading-5 text-slate-500">
                    Payment is processed by Paystack. BizFlow POS never receives or stores your card or mobile-money credentials.
                </p>
            </form>
        </section>

        <aside>
            <div class="sticky top-8 rounded-3xl bg-ink p-7 text-white shadow-2xl shadow-slate-300">
                <p class="text-xs font-extrabold tracking-[.18em] text-blue-300">ORDER SUMMARY</p>
                <h2 class="mt-4 text-2xl font-extrabold"><?= htmlspecialchars($plan['name']) ?></h2>
                <p class="mt-2 text-sm leading-6 text-slate-300"><?= htmlspecialchars($plan['description']) ?></p>
                <div class="my-7 border-y border-white/10 py-6">
                    <div class="flex items-end justify-between">
                        <span class="text-sm text-slate-300"><?= ucfirst($period) ?> license</span>
                        <span class="text-3xl font-extrabold">KES <?= number_format($price) ?></span>
                    </div>
                    <p class="mt-2 text-right text-xs text-slate-400">
                        <?= $period === 'annual' ? '12 months' : '1 month' ?> · <?= (int)$plan['max_devices'] ?> computer<?= (int)$plan['max_devices'] === 1 ? '' : 's' ?>
                    </p>
                </div>
                <ul class="space-y-3 text-sm text-slate-200">
                    <?php foreach ($plan['features'] as $feature): ?>
                        <li class="flex gap-3"><span class="text-emerald-400">✓</span><?= htmlspecialchars($feature) ?></li>
                    <?php endforeach; ?>
                    <li class="flex gap-3"><span class="text-emerald-400">✓</span>Instant TXT activation file</li>
                    <li class="flex gap-3"><span class="text-emerald-400">✓</span>Secure business-isolated synchronization</li>
                </ul>
            </div>
        </aside>
    </main>
</body>
</html>
