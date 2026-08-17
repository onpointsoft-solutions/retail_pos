<?php
declare(strict_types=1);

require_once __DIR__ . '/../services/LicensePaymentService.php';
require_once __DIR__ . '/logo.php';

$reference = trim((string)($_GET['reference'] ?? $_GET['trxref'] ?? ''));
$order     = null;
$error     = '';

try {
    $order = verifyAndCompleteLicensePayment($reference);
} catch (Throwable $exception) {
    $error = $exception->getMessage();
}

$success    = $order !== null;
$pageTitle  = $success ? 'License Ready · BizFlow POS' : 'Payment Verification · BizFlow POS';
$whatsAppNum = '254742071810';
$waSupport   = urlencode('Hello, I need help with my BizFlow POS payment. Reference: ' . $reference);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($pageTitle) ?></title>
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
                        card:    '0 4px 24px -4px rgba(15,23,42,.10), 0 1px 4px -1px rgba(15,23,42,.06)',
                        success: '0 16px 48px -8px rgba(5,150,105,.25)',
                        glow:    '0 8px 32px -4px rgba(37,99,235,.30)',
                    },
                }
            }
        }
    </script>
    <style>
        @keyframes pop {
            0%  { transform: scale(.7); opacity: 0; }
            70% { transform: scale(1.08); }
            100%{ transform: scale(1);  opacity: 1; }
        }
        .animate-pop { animation: pop .45s cubic-bezier(.34,1.56,.64,1) both; }

        @keyframes fade-up {
            from { opacity: 0; transform: translateY(16px); }
            to   { opacity: 1; transform: translateY(0); }
        }
        .animate-fade-up { animation: fade-up .4s ease both; }
        .delay-1 { animation-delay: .12s; }
        .delay-2 { animation-delay: .22s; }
        .delay-3 { animation-delay: .32s; }
        .delay-4 { animation-delay: .42s; }
    </style>
</head>
<body class="min-h-screen bg-slate-50 text-ink antialiased" style="font-family:'Inter',sans-serif">

<!-- ══════════  HEADER  ══════════ -->
<header class="border-b border-slate-200/80 bg-white/95 backdrop-blur-md shadow-sm">
    <div class="max-w-5xl mx-auto px-5 h-[68px] flex items-center justify-between gap-4">
        <a href="licensing.php" class="flex items-center gap-3 shrink-0">
            <?= bizflowLogoImg(42) ?>
            <span class="hidden sm:block">
                <span class="block font-black text-[16px] tracking-tight text-ink leading-none">BizFlow POS</span>
                <span class="block text-[11px] font-medium text-muted mt-0.5">
                    <?= $success ? 'Activation ready' : 'Payment verification' ?>
                </span>
            </span>
        </a>
        <?php if ($success): ?>
        <span class="flex items-center gap-2 text-sm font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-full px-4 py-1.5">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m5 12 4 4L19 6"/>
            </svg>
            Payment confirmed
        </span>
        <?php endif; ?>
    </div>
</header>

<!-- ══════════  MAIN  ══════════ -->
<main class="max-w-2xl mx-auto px-5 py-14">

    <?php if ($success): ?>
    <!-- ─── SUCCESS STATE ─── -->
    <div class="text-center mb-10">
        <!-- Animated tick -->
        <div class="animate-pop mx-auto w-24 h-24 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 text-white grid place-items-center shadow-success mb-6">
            <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m5 12 4 4L19 6"/>
            </svg>
        </div>
        <p class="animate-fade-up text-xs font-black tracking-widest text-emerald-700 mb-3">PAYMENT CONFIRMED</p>
        <h1 class="animate-fade-up delay-1 text-4xl font-black tracking-tight">Your license is ready.</h1>
        <p class="animate-fade-up delay-2 mt-3 text-muted leading-7 max-w-md mx-auto">
            Your BizFlow POS activation details have been generated.
            Download the file below and keep it somewhere safe.
        </p>
    </div>

    <!-- Order detail card -->
    <div class="animate-fade-up delay-2 rounded-3xl border border-slate-200 bg-white shadow-card overflow-hidden mb-6">

        <!-- Card header with logo -->
        <div class="bg-ink px-7 py-6 flex items-center gap-4">
            <?= bizflowLogoImg(44, 'opacity-90') ?>
            <div>
                <p class="font-black text-white text-lg leading-tight">
                    <?= htmlspecialchars($order['plan_code']) ?> License
                </p>
                <p class="text-blue-200 text-xs font-semibold mt-0.5">
                    <?= htmlspecialchars(ucfirst($order['billing_period'])) ?> billing
                </p>
            </div>
        </div>

        <!-- Details rows -->
        <dl class="divide-y divide-slate-100 px-7">
            <div class="flex justify-between gap-4 py-4 text-sm">
                <dt class="text-muted font-semibold">Package</dt>
                <dd class="font-extrabold text-ink"><?= htmlspecialchars($order['plan_code']) ?></dd>
            </div>
            <div class="flex justify-between gap-4 py-4 text-sm">
                <dt class="text-muted font-semibold">Billing period</dt>
                <dd class="font-extrabold text-ink"><?= htmlspecialchars(ucfirst($order['billing_period'])) ?></dd>
            </div>
            <div class="flex justify-between gap-4 py-4 text-sm">
                <dt class="text-muted font-semibold">Customer</dt>
                <dd class="font-extrabold text-ink text-right"><?= htmlspecialchars($order['customer_name']) ?></dd>
            </div>
            <div class="flex justify-between gap-4 py-4 text-sm">
                <dt class="text-muted font-semibold">Email</dt>
                <dd class="font-semibold text-ink text-right break-all"><?= htmlspecialchars($order['customer_email']) ?></dd>
            </div>
            <div class="flex justify-between gap-4 py-4 text-sm">
                <dt class="text-muted font-semibold">Paystack reference</dt>
                <dd class="font-mono font-semibold text-ink text-right break-all text-xs leading-5">
                    <?= htmlspecialchars($order['reference']) ?>
                </dd>
            </div>
        </dl>
    </div>

    <!-- Download CTA -->
    <div class="animate-fade-up delay-3 space-y-3">
        <a href="<?= htmlspecialchars(activationDownloadUrl($order)) ?>"
           class="flex items-center justify-center gap-2.5 w-full rounded-2xl bg-brand-600 px-6 py-4 text-base font-extrabold text-white hover:bg-brand-700 transition shadow-glow active:scale-[.98]">
            <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/>
            </svg>
            Download activation file (.txt)
        </a>

        <div class="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 flex items-start gap-3">
            <span class="shrink-0 mt-0.5 text-amber-500">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 9v2m0 4h.01M10.29 3.86 1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                </svg>
            </span>
            <p class="text-sm text-amber-800 leading-6">
                <strong>Keep this file private.</strong> It contains your license key and backend URL.
                Anyone with the file can activate BizFlow POS for your business.
            </p>
        </div>
    </div>

    <!-- Activation steps -->
    <div class="animate-fade-up delay-4 mt-8 rounded-3xl border border-slate-200 bg-white shadow-card p-7">
        <p class="text-xs font-black tracking-widest text-muted mb-5">NEXT STEPS</p>
        <ol class="space-y-4">
            <?php
            $steps = [
                ['num' => '1', 'text' => 'Open BizFlow POS on your computer.'],
                ['num' => '2', 'text' => 'Go to <strong>License Management</strong> in the settings menu.'],
                ['num' => '3', 'text' => 'Enter your backend URL (included in the activation file).'],
                ['num' => '4', 'text' => 'Paste your license key exactly as shown in the file.'],
                ['num' => '5', 'text' => 'BizFlow POS will verify your license and unlock all features.'],
            ];
            foreach ($steps as $step):
            ?>
            <li class="flex items-start gap-4 text-sm text-slate-700 leading-6">
                <span class="shrink-0 w-7 h-7 rounded-full bg-brand-600 text-white text-xs font-black grid place-items-center shadow-sm">
                    <?= $step['num'] ?>
                </span>
                <span><?= $step['text'] ?></span>
            </li>
            <?php endforeach; ?>
        </ol>
    </div>

    <!-- Support footer -->
    <div class="animate-fade-up delay-4 mt-6 text-center">
        <p class="text-sm text-muted">
            Need help?
            <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waSupport ?>"
               target="_blank" rel="noopener"
               class="font-semibold text-brand-600 hover:text-brand-700 transition">
                Chat with us on WhatsApp
            </a>
        </p>
    </div>

    <?php else: ?>
    <!-- ─── FAILURE / PENDING STATE ─── -->
    <div class="text-center mb-8">
        <div class="animate-pop mx-auto w-24 h-24 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 text-white grid place-items-center shadow-lg mb-6">
            <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5"
                      d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
            </svg>
        </div>
        <p class="animate-fade-up text-xs font-black tracking-widest text-amber-700 mb-3">VERIFICATION INCOMPLETE</p>
        <h1 class="animate-fade-up delay-1 text-3xl sm:text-4xl font-black tracking-tight">
            We couldn't release the license yet.
        </h1>
        <p class="animate-fade-up delay-2 mt-4 text-muted leading-7 max-w-md mx-auto">
            <?= htmlspecialchars($error ?: 'Payment has not been confirmed by Paystack.') ?>
        </p>
    </div>

    <!-- What to do card -->
    <div class="animate-fade-up delay-2 rounded-3xl border border-slate-200 bg-white shadow-card overflow-hidden mb-6">
        <div class="bg-amber-50 border-b border-amber-200 px-7 py-5">
            <p class="font-extrabold text-amber-900 text-sm">What could have happened?</p>
        </div>
        <ul class="divide-y divide-slate-100 px-7">
            <?php
            $reasons = [
                'Your payment is still being processed — this can take up to 5 minutes.',
                'The payment was cancelled before completing.',
                'A network error occurred during verification.',
                'The payment reference is missing or was altered.',
            ];
            foreach ($reasons as $reason):
            ?>
            <li class="flex items-start gap-3 py-4 text-sm text-slate-600">
                <span class="shrink-0 mt-0.5 w-1.5 h-1.5 rounded-full bg-amber-400 mt-2"></span>
                <?= htmlspecialchars($reason) ?>
            </li>
            <?php endforeach; ?>
        </ul>
    </div>

    <!-- Actions -->
    <div class="animate-fade-up delay-3 space-y-3">
        <?php if ($reference !== ''): ?>
        <a href="license-callback.php?reference=<?= urlencode($reference) ?>"
           class="flex items-center justify-center gap-2.5 w-full rounded-2xl bg-brand-600 px-6 py-4 text-base font-extrabold text-white hover:bg-brand-700 transition shadow-glow active:scale-[.98]">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            Try verifying again
        </a>
        <?php endif; ?>
        <a href="licensing.php"
           class="flex items-center justify-center gap-2 w-full rounded-2xl border border-slate-300 bg-white px-6 py-4 text-base font-bold text-ink hover:bg-slate-50 transition active:scale-[.98]">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
            Return to packages
        </a>
        <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waSupport ?>"
           target="_blank" rel="noopener"
           class="flex items-center justify-center gap-2 w-full rounded-2xl border border-slate-200 bg-white px-6 py-3.5 text-sm font-bold text-slate-600 hover:text-brand-600 hover:border-brand-200 transition">
            <svg class="w-4 h-4 text-emerald-600" fill="currentColor" viewBox="0 0 24 24">
                <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
            </svg>
            Contact support on WhatsApp
        </a>
    </div>

    <?php endif; ?>
</main>

<!-- ══════════  FOOTER  ══════════ -->
<footer class="border-t border-slate-200 bg-white mt-10">
    <div class="max-w-5xl mx-auto px-5 py-7 flex flex-col sm:flex-row gap-4 justify-between items-center">
        <div class="flex items-center gap-3">
            <?= bizflowLogoImg(32) ?>
            <p class="text-sm font-extrabold text-ink">BizFlow POS</p>
        </div>
        <p class="text-xs text-muted">© 2026 BizFlow POS. All rights reserved.</p>
    </div>
</footer>

</body>
</html>
