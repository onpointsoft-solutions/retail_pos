<?php
declare(strict_types=1);

require_once __DIR__ . '/../services/LicensePaymentService.php';
require_once __DIR__ . '/logo.php';

$plans    = licensePlans();
$planCode = strtoupper(trim((string)($_POST['plan_code'] ?? $_GET['plan'] ?? 'BUSINESS')));
$period   = strtolower(trim((string)($_POST['billing_period'] ?? $_GET['period'] ?? 'annual')));

if (!isset($plans[$planCode])) {
    $planCode = 'BUSINESS';
}
if (!in_array($period, ['monthly', 'annual'], true)) {
    $period = 'annual';
}

$plan  = $plans[$planCode];
$error = '';
$values = [
    'customer_name'  => trim((string)($_POST['customer_name']  ?? '')),
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

$price     = $period === 'annual' ? $plan['annual_price'] : $plan['monthly_price'];
$csrfToken = licenseCsrfToken();

$periodLabel   = $period === 'annual' ? 'Annual (12 months)' : 'Monthly';
$deviceCount   = (int)$plan['max_devices'];
$deviceLabel   = $deviceCount === 1 ? '1 computer' : "{$deviceCount} computers";
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout — <?= htmlspecialchars($plan['name']) ?> · BizFlow POS</title>
    <meta name="robots" content="noindex,nofollow">
    <link rel="icon" type="image/png" href="logo.png">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:opsz,wght@14..32,400;14..32,500;14..32,600;14..32,700;14..32,800;14..32,900&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: { 50:'#eff6ff', 100:'#dbeafe', 500:'#3b82f6', 600:'#2563eb', 700:'#1d4ed8', 800:'#1e40af' },
                        ink:   '#0f172a',
                        muted: '#64748b',
                    },
                    fontFamily: { sans: ['Inter','ui-sans-serif','system-ui','sans-serif'] },
                    boxShadow: {
                        card: '0 4px 24px -4px rgba(15,23,42,.10), 0 1px 4px -1px rgba(15,23,42,.06)',
                        glow: '0 8px 32px -4px rgba(37,99,235,.30)',
                    },
                }
            }
        }
    </script>
    <style>
        input:focus { outline: none; }
        .input-field {
            width: 100%;
            border-radius: .75rem;
            border: 1.5px solid #e2e8f0;
            padding: .875rem 1rem;
            font-size: .875rem;
            color: #0f172a;
            transition: border-color .15s, box-shadow .15s;
            background: #fff;
        }
        .input-field:focus {
            border-color: #2563eb;
            box-shadow: 0 0 0 4px rgba(37,99,235,.12);
        }
        .input-field::placeholder { color:#94a3b8; }
    </style>
</head>
<body class="min-h-screen bg-slate-50 text-ink antialiased" style="font-family:'Inter',sans-serif">

<!-- ══════════  HEADER  ══════════ -->
<header class="sticky top-0 z-40 border-b border-slate-200/80 bg-white/95 backdrop-blur-md shadow-sm">
    <div class="max-w-6xl mx-auto px-5 h-[68px] flex items-center justify-between gap-4">
        <a href="licensing.php" class="flex items-center gap-3 shrink-0">
            <?= bizflowLogoImg(42) ?>
            <span class="hidden sm:block">
                <span class="block font-black text-[16px] tracking-tight text-ink leading-none">BizFlow POS</span>
                <span class="block text-[11px] font-medium text-muted mt-0.5">Secure checkout</span>
            </span>
        </a>
        <div class="flex items-center gap-2 text-sm font-semibold text-slate-500">
            <span class="flex items-center justify-center w-7 h-7 rounded-full bg-emerald-100 text-emerald-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-7a2 2 0 00-2-2h-1V7a5 5 0 00-10 0v3H6a2 2 0 00-2 2v7a2 2 0 002 2z"/>
                </svg>
            </span>
            <span class="hidden sm:block">Secured by Paystack</span>
        </div>
    </div>
</header>

<!-- ══════════  MAIN  ══════════ -->
<main class="max-w-6xl mx-auto px-5 py-10 lg:py-14 grid lg:grid-cols-[1fr_400px] gap-8 items-start">

    <!-- ── Left: Form ── -->
    <section class="rounded-3xl border border-slate-200 bg-white p-7 sm:p-10 shadow-card">

        <!-- Breadcrumb -->
        <nav class="flex items-center gap-2 text-xs font-semibold text-muted mb-7">
            <a href="licensing.php" class="hover:text-brand-600 transition">Packages</a>
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 5l7 7-7 7"/>
            </svg>
            <span class="text-ink">Checkout</span>
        </nav>

        <p class="text-xs font-black tracking-[.18em] text-brand-600">COMPLETE YOUR PURCHASE</p>
        <h1 class="mt-2 text-3xl font-black tracking-tight">
            <?= htmlspecialchars($plan['name']) ?> License
        </h1>
        <p class="mt-2 text-muted text-sm leading-6">
            Your activation file is generated and sent to you immediately after Paystack confirms payment.
        </p>

        <!-- Error alert -->
        <?php if ($error !== ''): ?>
        <div class="mt-6 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 px-5 py-4">
            <span class="mt-0.5 shrink-0 w-5 h-5 rounded-full bg-red-100 text-red-600 grid place-items-center text-xs font-black">!</span>
            <p class="text-sm font-semibold text-red-700"><?= htmlspecialchars($error) ?></p>
        </div>
        <?php endif; ?>

        <!-- Form -->
        <form method="post" novalidate class="mt-8 space-y-5">
            <input type="hidden" name="csrf_token"     value="<?= htmlspecialchars($csrfToken) ?>">
            <input type="hidden" name="plan_code"      value="<?= htmlspecialchars($planCode) ?>">
            <input type="hidden" name="billing_period" value="<?= htmlspecialchars($period) ?>">

            <!-- Name -->
            <div>
                <label class="block text-sm font-bold text-slate-700 mb-2" for="customer_name">
                    Business or customer name <span class="text-red-500">*</span>
                </label>
                <input id="customer_name" name="customer_name" type="text"
                       required maxlength="150" autocomplete="organization"
                       placeholder="e.g. Kamau General Store"
                       value="<?= htmlspecialchars($values['customer_name']) ?>"
                       class="input-field">
            </div>

            <!-- Email + Phone -->
            <div class="grid sm:grid-cols-2 gap-5">
                <div>
                    <label class="block text-sm font-bold text-slate-700 mb-2" for="customer_email">
                        Email address <span class="text-red-500">*</span>
                    </label>
                    <input id="customer_email" name="customer_email" type="email"
                           required maxlength="190" autocomplete="email"
                           placeholder="you@example.com"
                           value="<?= htmlspecialchars($values['customer_email']) ?>"
                           class="input-field">
                    <p class="mt-1.5 text-xs text-muted">Your activation file is emailed here.</p>
                </div>
                <div>
                    <label class="block text-sm font-bold text-slate-700 mb-2" for="customer_phone">
                        Phone number <span class="text-red-500">*</span>
                    </label>
                    <input id="customer_phone" name="customer_phone" type="tel"
                           required maxlength="40" autocomplete="tel"
                           placeholder="+254 7XX XXX XXX"
                           value="<?= htmlspecialchars($values['customer_phone']) ?>"
                           class="input-field">
                </div>
            </div>

            <!-- Confirmation checkbox -->
            <label class="flex items-start gap-3 rounded-2xl bg-slate-50 border border-slate-200 p-4 cursor-pointer hover:bg-blue-50 hover:border-brand-200 transition">
                <input type="checkbox" required
                       class="mt-0.5 h-4 w-4 shrink-0 rounded border-slate-300 accent-brand-600">
                <span class="text-sm text-slate-600 leading-6">
                    I confirm these details are correct and understand the activation key
                    must be <strong class="text-ink">kept private</strong> to protect my license.
                </span>
            </label>

            <!-- Submit -->
            <button type="submit"
                    class="w-full flex items-center justify-center gap-2.5 rounded-2xl bg-brand-600 px-6 py-4 text-base font-extrabold text-white hover:bg-brand-700 transition shadow-glow active:scale-[.98]">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-7a2 2 0 00-2-2h-1V7a5 5 0 00-10 0v3H6a2 2 0 00-2 2v7a2 2 0 002 2z"/>
                </svg>
                Pay KES <?= number_format($price) ?> securely
            </button>

            <p class="text-center text-xs leading-5 text-muted">
                Payment is processed by <strong>Paystack</strong>. BizFlow POS never
                receives or stores your card or mobile-money credentials.
            </p>
        </form>
    </section>

    <!-- ── Right: Order summary ── -->
    <aside class="lg:sticky lg:top-24">
        <div class="rounded-3xl overflow-hidden shadow-card border border-slate-200">

            <!-- Dark header -->
            <div class="bg-ink px-7 pt-8 pb-6 text-white">
                <p class="text-xs font-black tracking-[.18em] text-brand-400 mb-4">ORDER SUMMARY</p>

                <div class="flex items-center gap-3 mb-4">
                    <?= bizflowLogoImg(40, 'opacity-90') ?>
                    <div>
                        <p class="font-black text-lg leading-tight"><?= htmlspecialchars($plan['name']) ?></p>
                        <p class="text-xs text-blue-200 font-semibold">BizFlow POS License</p>
                    </div>
                </div>

                <p class="text-sm text-slate-300 leading-6"><?= htmlspecialchars($plan['description']) ?></p>

                <div class="mt-6 pt-5 border-t border-white/10">
                    <div class="flex items-end justify-between gap-2">
                        <div>
                            <p class="text-xs text-slate-400 font-semibold mb-1"><?= htmlspecialchars($periodLabel) ?></p>
                            <p class="text-sm text-slate-300"><?= htmlspecialchars($deviceLabel) ?></p>
                        </div>
                        <div class="text-right">
                            <p class="text-xs text-slate-400 font-semibold mb-0.5">Total</p>
                            <p class="text-3xl font-black">KES <?= number_format($price) ?></p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Features list -->
            <div class="bg-white px-7 py-6">
                <p class="text-xs font-extrabold tracking-widest text-muted mb-4">INCLUDED</p>
                <ul class="space-y-3">
                    <?php foreach ($plan['features'] as $feature): ?>
                    <li class="flex items-start gap-2.5 text-sm text-slate-700">
                        <span class="mt-0.5 shrink-0 w-4.5 h-4.5 w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 grid place-items-center">
                            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="m5 12 4 4L19 6"/>
                            </svg>
                        </span>
                        <?= htmlspecialchars($feature) ?>
                    </li>
                    <?php endforeach; ?>
                    <li class="flex items-start gap-2.5 text-sm text-slate-700">
                        <span class="mt-0.5 shrink-0 w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 grid place-items-center">
                            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="m5 12 4 4L19 6"/>
                            </svg>
                        </span>
                        Instant TXT activation file
                    </li>
                    <li class="flex items-start gap-2.5 text-sm text-slate-700">
                        <span class="mt-0.5 shrink-0 w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 grid place-items-center">
                            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="m5 12 4 4L19 6"/>
                            </svg>
                        </span>
                        Secure business-isolated sync
                    </li>
                </ul>

                <!-- Change plan link -->
                <a href="licensing.php"
                   class="mt-6 flex items-center justify-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700 transition">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                    </svg>
                    Change package
                </a>
            </div>
        </div>
    </aside>
</main>

<!-- ══════════  FOOTER  ══════════ -->
<footer class="border-t border-slate-200 bg-white mt-8">
    <div class="max-w-6xl mx-auto px-5 py-6 flex flex-col sm:flex-row gap-3 justify-between items-center text-xs text-muted">
        <p>© 2026 BizFlow POS. Professional retail software for Kenya.</p>
        <p>Prices in Kenya Shillings (KES).</p>
    </div>
</footer>

</body>
</html>
