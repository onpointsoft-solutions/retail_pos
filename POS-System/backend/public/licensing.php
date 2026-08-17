<?php
declare(strict_types=1);

require_once __DIR__ . '/logo.php';
require_once __DIR__ . '/../helpers/VisitTracker.php';
VisitTracker::record('pricing');

$pageTitle    = 'BizFlow POS — Licensing & Pricing';
$whatsAppNum  = '254702502952';
$shopUrl      = rtrim(getenv('MAIN_SITE_URL') ?: 'https://mobilemealscenter.co.ke', '/') . '/';

$plans = [
    [
        'name'        => 'Starter',
        'code'        => 'STARTER',
        'description' => 'Everything a single growing shop needs to sell professionally.',
        'monthly'     => 2500,
        'annual'      => 25000,   // 2 months free vs monthly × 12 = 30 000
        'devices'     => '1 computer',
        'popular'     => false,
        'badge'       => null,
        'features'    => [
            'Complete POS and inventory management',
            'Professional receipts and reports',
            'Product image synchronization',
            '30-day free trial — no card required',
            'Email support',
        ],
    ],
    [
        'name'        => 'Business',
        'code'        => 'BUSINESS',
        'description' => 'Built for established shops and teams working across several computers.',
        'monthly'     => 5500,
        'annual'      => 55000,
        'devices'     => 'Up to 5 computers',
        'popular'     => true,
        'badge'       => 'MOST POPULAR',
        'features'    => [
            'Everything in Starter',
            'Seamless multi-computer sync',
            'M-Pesa Bridge transactions',
            'Automated backups and imports',
            'Priority WhatsApp support',
        ],
    ],
    [
        'name'        => 'Enterprise',
        'code'        => 'ENTERPRISE',
        'description' => 'Advanced deployment for multi-branch and high-volume retail operations.',
        'monthly'     => 12000,
        'annual'      => 120000,
        'devices'     => 'Up to 20 computers',
        'popular'     => false,
        'badge'       => 'BEST VALUE',
        'features'    => [
            'Everything in Business',
            'Multi-branch deployment support',
            'Large-scale synchronization',
            'Priority onboarding and migration',
            'Dedicated support channel',
        ],
    ],
];

$waHello  = urlencode('Hello, I would like to activate BizFlow POS.');
$waStart  = urlencode('Hello, I want to start and activate BizFlow POS.');
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($pageTitle) ?></title>
    <meta name="description" content="Choose a BizFlow POS license in Kenya shillings. Starts with a free 30-day trial. Monthly or annual billing.">
    <link rel="icon" type="image/png" href="logo.png">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,400;0,14..32,500;0,14..32,600;0,14..32,700;0,14..32,800;0,14..32,900;1,14..32,400&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand:  { 50:'#eff6ff', 100:'#dbeafe', 400:'#60a5fa', 500:'#3b82f6', 600:'#2563eb', 700:'#1d4ed8', 800:'#1e40af' },
                        ink:    '#0f172a',
                        muted:  '#64748b',
                        coral:  '#e8432d',
                    },
                    fontFamily: { sans: ['Inter','ui-sans-serif','system-ui','sans-serif'] },
                    boxShadow: {
                        card:  '0 4px 24px -4px rgba(15,23,42,.10), 0 1px 4px -1px rgba(15,23,42,.06)',
                        glow:  '0 0 40px 0 rgba(37,99,235,.20)',
                        hero:  '0 32px 80px -16px rgba(37,99,235,.18)',
                    },
                }
            }
        }
    </script>
    <style>
        [x-cloak]{display:none}
        .gradient-hero { background: linear-gradient(135deg,#eff6ff 0%,#f8fafc 55%,#fff7ed 100%); }
        .card-popular  { background: linear-gradient(160deg,#1e40af 0%,#1d4ed8 40%,#2563eb 100%); }
        details > summary { list-style:none; }
        details > summary::-webkit-details-marker { display:none; }
        details[open] .faq-icon { transform:rotate(45deg); }
        .faq-icon { transition:transform .2s ease; }
        .shimmer {
            background: linear-gradient(90deg,#f1f5f9 25%,#e2e8f0 50%,#f1f5f9 75%);
            background-size: 200% 100%;
            animation: shimmer 1.6s infinite;
        }
        @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
    </style>
</head>
<body class="bg-slate-50 text-ink antialiased" style="font-family:'Inter',sans-serif" x-data="{annual:true}">

<!-- ═══════════════════════  STICKY HEADER  ═══════════════════════ -->
<header class="sticky top-0 z-50 border-b border-slate-200/80 bg-white/95 backdrop-blur-md shadow-sm">
    <div class="max-w-7xl mx-auto px-5 lg:px-10 h-[72px] flex items-center justify-between gap-4">

        <!-- Logo -->
        <a href="<?= htmlspecialchars($shopUrl) ?>" class="flex items-center gap-3 shrink-0">
            <?= bizflowLogoImg(46) ?>
            <span class="hidden sm:block">
                <span class="block font-black tracking-tight text-[17px] text-ink leading-none">BizFlow POS</span>
                <span class="block text-[11px] font-medium text-muted mt-0.5 tracking-wide">Retail made simple</span>
            </span>
        </a>

        <!-- Nav actions -->
        <nav class="flex items-center gap-2">
            <a href="<?= htmlspecialchars($shopUrl) ?>"
               class="hidden sm:inline-flex items-center px-4 py-2 text-sm font-semibold text-slate-600 hover:text-brand-600 transition rounded-lg hover:bg-brand-50">
                ← Back to shop
            </a>
            <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waHello ?>"
               target="_blank" rel="noopener"
               class="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-brand-700 transition shadow-md shadow-blue-200 active:scale-95">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
                Talk to sales
            </a>
        </nav>
    </div>
</header>

<!-- ═══════════════════════  HERO  ═══════════════════════ -->
<section class="relative overflow-hidden gradient-hero">
    <!-- Decorative blobs -->
    <div class="pointer-events-none absolute -top-32 left-1/2 -translate-x-1/2 w-[900px] h-[500px] rounded-full bg-blue-200/30 blur-3xl"></div>
    <div class="pointer-events-none absolute bottom-0 right-0 w-72 h-72 rounded-full bg-orange-100/60 blur-3xl"></div>

    <div class="relative max-w-4xl mx-auto px-5 pt-20 pb-16 text-center">

        <!-- Trust badge -->
        <div class="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-white/80 backdrop-blur px-5 py-2 text-xs font-bold tracking-widest text-brand-700 shadow-sm mb-8">
            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            30-DAY FREE TRIAL · NO CARD REQUIRED
        </div>

        <h1 class="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-[1.08] text-ink">
            Professional retail software<br>
            <span class="text-brand-600">priced for Kenyan businesses.</span>
        </h1>

        <p class="mt-6 text-lg sm:text-xl text-muted max-w-2xl mx-auto leading-relaxed">
            Sell faster, manage stock, receive M-Pesa references and keep every
            computer synchronized — starting at just <strong class="text-ink">KES&nbsp;2,500/month.</strong>
        </p>

        <!-- Billing toggle -->
        <div class="mt-10 inline-flex rounded-2xl border border-slate-200 bg-white p-1.5 shadow-card gap-1">
            <button @click="annual=false"
                    :class="!annual ? 'bg-ink text-white shadow-md' : 'text-muted hover:text-ink'"
                    class="rounded-xl px-6 py-3 text-sm font-bold transition-all duration-200 select-none">
                Monthly
            </button>
            <button @click="annual=true"
                    :class="annual ? 'bg-ink text-white shadow-md' : 'text-muted hover:text-ink'"
                    class="rounded-xl px-6 py-3 text-sm font-bold transition-all duration-200 select-none flex items-center gap-2">
                Annual
                <span :class="annual ? 'bg-emerald-400 text-emerald-950' : 'bg-emerald-100 text-emerald-700'"
                      class="rounded-full px-2.5 py-0.5 text-[11px] font-black transition-colors">
                    Save 2 months
                </span>
            </button>
        </div>
    </div>
</section>

<!-- ═══════════════════════  PRICING CARDS  ═══════════════════════ -->
<section class="max-w-7xl mx-auto px-5 lg:px-10 -mt-2 pb-8">
    <div class="grid lg:grid-cols-3 gap-6 items-start">

        <?php foreach ($plans as $plan):
            $isPopular = $plan['popular'];
        ?>
        <article class="relative flex flex-col rounded-3xl p-8 <?= $isPopular
            ? 'card-popular text-white shadow-hero ring-4 ring-blue-300/30 lg:-mt-4 lg:mb-4'
            : 'bg-white text-ink shadow-card border border-slate-200' ?>">

            <?php if ($plan['badge']): ?>
                <div class="absolute -top-4 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full px-5 py-1.5 text-[11px] font-black tracking-widest shadow-md <?= $isPopular ? 'bg-amber-400 text-amber-950' : 'bg-brand-600 text-white' ?>">
                    <?= htmlspecialchars($plan['badge']) ?>
                </div>
            <?php endif; ?>

            <!-- Plan header -->
            <div>
                <p class="text-xs font-black tracking-[.18em] <?= $isPopular ? 'text-blue-200' : 'text-brand-600' ?>">
                    <?= htmlspecialchars(strtoupper($plan['name'])) ?>
                </p>
                <p class="mt-3 text-sm leading-6 <?= $isPopular ? 'text-blue-100' : 'text-muted' ?> min-h-[48px]">
                    <?= htmlspecialchars($plan['description']) ?>
                </p>

                <!-- Price -->
                <div class="mt-7 flex items-end gap-1.5">
                    <span class="text-sm font-bold <?= $isPopular ? 'text-blue-200' : 'text-slate-400' ?> mb-2">KES</span>
                    <span class="text-5xl font-black tracking-tight leading-none"
                          x-text="annual ? '<?= number_format($plan['annual']) ?>' : '<?= number_format($plan['monthly']) ?>'">
                    </span>
                    <span class="text-sm <?= $isPopular ? 'text-blue-200' : 'text-muted' ?> mb-2"
                          x-text="annual ? '/year' : '/month'">
                    </span>
                </div>

                <!-- Per-month equivalent when annual -->
                <p class="mt-1.5 text-xs <?= $isPopular ? 'text-blue-200' : 'text-muted' ?>"
                   x-show="annual">
                    ≈ KES <?= number_format(intdiv($plan['annual'], 12)) ?>/month · billed annually
                </p>

                <p class="mt-2 text-sm font-semibold <?= $isPopular ? 'text-blue-100' : 'text-slate-600' ?>">
                    <?= htmlspecialchars($plan['devices']) ?>
                </p>
            </div>

            <!-- Divider -->
            <div class="my-6 border-t <?= $isPopular ? 'border-white/15' : 'border-slate-100' ?>"></div>

            <!-- Features -->
            <ul class="space-y-3.5 flex-1">
                <?php foreach ($plan['features'] as $feature): ?>
                    <li class="flex items-start gap-3 text-sm <?= $isPopular ? 'text-blue-50' : 'text-slate-700' ?>">
                        <span class="mt-0.5 shrink-0 w-5 h-5 rounded-full grid place-items-center <?= $isPopular ? 'bg-white/20 text-white' : 'bg-emerald-100 text-emerald-700' ?>">
                            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="m5 12 4 4L19 6"/>
                            </svg>
                        </span>
                        <?= htmlspecialchars($feature) ?>
                    </li>
                <?php endforeach; ?>
            </ul>

            <!-- CTA button -->
            <a :href="'license-checkout.php?plan=<?= urlencode($plan['code']) ?>&period=' + (annual ? 'annual' : 'monthly')"
               class="mt-8 flex items-center justify-center gap-2 rounded-2xl px-5 py-4 text-sm font-extrabold transition-all active:scale-95 <?= $isPopular
                   ? 'bg-white text-brand-700 hover:bg-blue-50 shadow-lg'
                   : 'bg-brand-600 text-white hover:bg-brand-700 shadow-md shadow-blue-100' ?>">
                Get <?= htmlspecialchars($plan['name']) ?>
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 7l5 5m0 0-5 5m5-5H6"/>
                </svg>
            </a>
        </article>
        <?php endforeach; ?>
    </div>
</section>

<!-- ═══════════════════════  TRUST STRIP  ═══════════════════════ -->
<section class="max-w-7xl mx-auto px-5 lg:px-10 py-6">
    <div class="rounded-2xl border border-emerald-200 bg-emerald-50 px-6 py-5 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-4">
            <div class="w-12 h-12 shrink-0 rounded-xl bg-emerald-600 text-white grid place-items-center shadow">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-7a2 2 0 00-2-2h-1V7a5 5 0 00-10 0v3H6a2 2 0 00-2 2v7a2 2 0 002 2z"/>
                </svg>
            </div>
            <div>
                <p class="font-extrabold text-emerald-900">Secure automated activation</p>
                <p class="text-sm text-emerald-800 mt-0.5">
                    Pay through Paystack and download your private activation file immediately after verification.
                </p>
            </div>
        </div>
        <span class="shrink-0 rounded-xl bg-white px-5 py-2.5 text-sm font-extrabold text-emerald-700 shadow-sm border border-emerald-200">
            KES · Paystack
        </span>
    </div>
</section>

<!-- ═══════════════════════  FEATURES GRID  ═══════════════════════ -->
<section class="max-w-7xl mx-auto px-5 lg:px-10 py-16">
    <div class="text-center mb-12">
        <p class="text-xs font-black tracking-widest text-brand-600 mb-3">WHY BIZFLOW POS</p>
        <h2 class="text-3xl sm:text-4xl font-black tracking-tight">Everything your shop needs.</h2>
        <p class="mt-3 text-muted max-w-xl mx-auto">Built specifically for Kenyan retail — works offline, syncs in real time, and speaks M-Pesa natively.</p>
    </div>
    <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <?php
        $features = [
            ['icon' => 'M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 11h.01M12 11h.01M15 11h.01M4 19h16a2 2 0 002-2V7a2 2 0 00-2-2H4a2 2 0 00-2 2v10a2 2 0 002 2z',
             'title' => 'Fast POS Interface', 'desc' => 'Ring up sales in seconds with barcode scanning and quick-search product lookup.'],
            ['icon' => 'M4 7h16M4 11h16M4 15h16M4 19h16M8 3v2m8-2v2',
             'title' => 'Real-time Inventory', 'desc' => 'Stock levels update instantly across every workstation the moment a sale completes.'],
            ['icon' => 'M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z',
             'title' => 'M-Pesa Bridge', 'desc' => 'TransRouter reads M-Pesa SMS confirmations and attaches them to the right transaction automatically.'],
            ['icon' => 'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
             'title' => 'Professional Reports', 'desc' => 'Daily sales summaries, stock movement and profit snapshots exported as PDF or CSV.'],
            ['icon' => 'M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4',
             'title' => 'Multi-computer Sync', 'desc' => 'All computers on your network share one live database — no manual reconciliation required.'],
            ['icon' => 'M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z',
             'title' => 'Automatic Backups', 'desc' => 'Scheduled cloud backups keep your data safe. Restore to any point in time with one click.'],
        ];
        foreach ($features as $f): ?>
        <div class="rounded-2xl bg-white border border-slate-200 p-6 shadow-card hover:shadow-lg transition-shadow">
            <div class="w-11 h-11 rounded-xl bg-brand-50 text-brand-600 grid place-items-center mb-4">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="<?= $f['icon'] ?>"/>
                </svg>
            </div>
            <h3 class="font-extrabold text-base mb-1.5"><?= htmlspecialchars($f['title']) ?></h3>
            <p class="text-sm text-muted leading-6"><?= htmlspecialchars($f['desc']) ?></p>
        </div>
        <?php endforeach; ?>
    </div>
</section>

<!-- ═══════════════════════  CTA BANNER  ═══════════════════════ -->
<section class="max-w-7xl mx-auto px-5 lg:px-10 pb-16">
    <div class="relative overflow-hidden rounded-3xl bg-ink px-8 py-12 lg:px-14 lg:py-14 grid lg:grid-cols-[1.4fr_.6fr] gap-10 items-center">
        <!-- Background decoration -->
        <div class="absolute -right-12 -top-12 w-80 h-80 rounded-full bg-brand-600/25 blur-3xl pointer-events-none"></div>
        <div class="absolute right-40 bottom-0 w-48 h-48 rounded-full bg-blue-400/10 blur-2xl pointer-events-none"></div>

        <div class="relative">
            <p class="text-xs font-black tracking-widest text-brand-400 mb-3">READY TO GET STARTED?</p>
            <h2 class="text-3xl sm:text-4xl font-black text-white tracking-tight leading-tight">
                Use every feature free<br>for 30 days.
            </h2>
            <p class="mt-4 text-slate-300 leading-7 max-w-lg">
                Your data stays on your computer and synchronizes securely with your configured
                backend. When the trial ends, enter your license key and continue where you left off.
            </p>
            <div class="mt-8 flex flex-wrap gap-3">
                <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waStart ?>"
                   target="_blank" rel="noopener"
                   class="inline-flex items-center gap-2 rounded-xl bg-white px-7 py-3.5 font-extrabold text-ink hover:bg-blue-50 transition active:scale-95 shadow-lg">
                    <svg class="w-4 h-4 text-emerald-600" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                    </svg>
                    Start with BizFlow POS
                </a>
                <a href="#pricing"
                   class="inline-flex items-center gap-2 rounded-xl border border-white/20 px-7 py-3.5 font-bold text-white hover:bg-white/10 transition">
                    View packages
                </a>
            </div>
        </div>

        <div class="relative flex justify-center lg:justify-end">
            <div class="w-48 h-48 lg:w-56 lg:h-56 opacity-90 drop-shadow-2xl">
                <?= bizflowLogoImg(220, 'opacity-95') ?>
            </div>
        </div>
    </div>
</section>

<!-- ═══════════════════════  FAQ  ═══════════════════════ -->
<section class="max-w-3xl mx-auto px-5 pb-24">
    <div class="text-center mb-10">
        <p class="text-xs font-black tracking-widest text-brand-600 mb-3">FAQ</p>
        <h2 class="text-3xl font-black tracking-tight">Common questions</h2>
    </div>

    <?php
    $faqs = [
        ['q' => 'What happens after the free 30 days?',
         'a' => 'BizFlow POS will ask for activation before opening. Your database is not deleted — entering a valid license key restores full access immediately.'],
        ['q' => 'Does the POS work without internet?',
         'a' => 'Yes. A previously validated paid license has a seven-day offline grace period. Connect periodically for license validation and cloud synchronization.'],
        ['q' => 'Can I add more computers later?',
         'a' => 'Absolutely. Upgrade to Business or Enterprise at any time and activate the same license on additional workstations up to your plan limit.'],
        ['q' => 'How does M-Pesa integration work?',
         'a' => 'The optional TransRouter Android app reads M-Pesa confirmation SMS messages and forwards them to BizFlow POS over your local network — no manual entry needed.'],
        ['q' => 'Is my payment information secure?',
         'a' => 'All payments are processed by Paystack. BizFlow POS never receives, stores, or has access to your card or mobile-money credentials.'],
        ['q' => 'What if I lose my activation file?',
         'a' => 'Contact support with your Paystack reference number and we will re-issue your activation details within one business day.'],
    ];
    ?>

    <div class="divide-y divide-slate-200 border border-slate-200 rounded-3xl bg-white shadow-card overflow-hidden">
        <?php foreach ($faqs as $faq): ?>
        <details class="group px-7 py-1">
            <summary class="flex cursor-pointer items-center justify-between gap-4 py-5 font-bold text-ink select-none">
                <?= htmlspecialchars($faq['q']) ?>
                <span class="faq-icon shrink-0 w-7 h-7 rounded-full bg-slate-100 text-slate-500 grid place-items-center text-xl font-light leading-none">+</span>
            </summary>
            <p class="pb-6 text-sm leading-7 text-muted -mt-1">
                <?= htmlspecialchars($faq['a']) ?>
            </p>
        </details>
        <?php endforeach; ?>
    </div>
</section>

<!-- ═══════════════════════  FOOTER  ═══════════════════════ -->
<footer class="border-t border-slate-200 bg-white">
    <div class="max-w-7xl mx-auto px-5 lg:px-10 py-10 flex flex-col sm:flex-row gap-6 justify-between items-start sm:items-center">
        <div class="flex items-center gap-3">
            <?= bizflowLogoImg(36) ?>
            <div>
                <p class="font-extrabold text-sm text-ink">BizFlow POS</p>
                <p class="text-xs text-muted">Professional retail software for Kenya</p>
            </div>
        </div>
        <div class="flex flex-col sm:flex-row gap-2 sm:gap-8 text-sm text-muted">
            <p>© 2026 BizFlow POS. All rights reserved.</p>
            <p>Prices in Kenya Shillings (KES). VAT may apply.</p>
        </div>
    </div>
</footer>

</body>
</html>
