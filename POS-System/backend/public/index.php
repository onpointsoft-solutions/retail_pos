<?php
declare(strict_types=1);

require_once __DIR__ . '/logo.php';
require_once __DIR__ . '/../helpers/VisitTracker.php';
VisitTracker::record('home');

$shopUrl     = rtrim(getenv('MAIN_SITE_URL') ?: 'https://mobilemealscenter.co.ke', '/') . '/';
$whatsAppNum = '254742071810';
$waText      = urlencode('Hello, I need help with BizFlow POS.');
$version     = '2.0.0';

// ── Download assets ──────────────────────────────────────────────────────────
// Paths are relative to the public/ directory; adjust if assets are served
// from a CDN or a different path in production.
$downloads = [
    [
        'id'       => 'windows',
        'label'    => 'Windows Installer',
        'subtitle' => 'Windows 10 / 11 · 64-bit',
        'file'     => 'downloads/BizFlowPOS-Setup-' . $version . '.exe',
        'icon'     => 'windows',
        'size'     => '54 MB',
        'primary'  => true,
    ],
    [
        'id'       => 'linux',
        'label'    => 'Linux Package (.deb)',
        'subtitle' => 'Ubuntu 22.04+ / Debian 12+',
        'file'     => 'downloads/bizflowpos_' . $version . '_amd64.deb',
        'icon'     => 'linux',
        'size'     => null,           // built on request — link shown but marked "contact us"
        'primary'  => false,
    ],
    [
        'id'       => 'android',
        'label'    => 'TransRouter APK',
        'subtitle' => 'Android 7.0+ · M-Pesa Bridge',
        'file'     => 'downloads/TransRouter-1.0.apk',
        'icon'     => 'android',
        'size'     => null,
        'primary'  => false,
    ],
];

$features = [
    ['icon' => 'M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 11h.01M12 11h.01M15 11h.01M4 19h16a2 2 0 002-2V7a2 2 0 00-2-2H4a2 2 0 00-2 2v10a2 2 0 002 2z',
     'title' => 'Fast POS Interface',         'desc' => 'Barcode scan, quick-search and one-click checkout.'],
    ['icon' => 'M4 7h16M4 11h16M4 15h16M4 19h16',
     'title' => 'Real-time Inventory',        'desc' => 'Stock updates the instant a sale completes — across every workstation.'],
    ['icon' => 'M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z',
     'title' => 'M-Pesa Bridge',              'desc' => 'TransRouter reads M-Pesa SMS and links payments automatically.'],
    ['icon' => 'M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4',
     'title' => 'Multi-computer Sync',        'desc' => 'One shared live database — no manual reconciliation.'],
    ['icon' => 'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
     'title' => 'Professional Reports',       'desc' => 'Daily summaries, stock movement and profit — export to PDF or CSV.'],
    ['icon' => 'M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z',
     'title' => 'Automatic Backups',          'desc' => 'Scheduled cloud backups with one-click restore.'],
];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BizFlow POS — Professional Retail Software for Kenya</title>
    <meta name="description" content="Download BizFlow POS — a fast, offline-capable point-of-sale system built for Kenyan retail businesses. Windows installer and M-Pesa bridge included.">
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
                        card:  '0 4px 24px -4px rgba(15,23,42,.10), 0 1px 4px -1px rgba(15,23,42,.06)',
                        glow:  '0 8px 32px -4px rgba(37,99,235,.28)',
                        hero:  '0 32px 72px -12px rgba(37,99,235,.18)',
                    },
                }
            }
        }
    </script>
    <style>
        .gradient-hero { background: linear-gradient(140deg,#eff6ff 0%,#f8fafc 60%,#fff7ed 100%); }
        .gradient-dark { background: linear-gradient(160deg,#0f172a 0%,#1e293b 100%); }
        .download-card:hover .dl-arrow { transform: translateY(2px); }
        .dl-arrow { transition: transform .2s ease; }
    </style>
</head>
<body class="bg-white text-ink antialiased" style="font-family:'Inter',sans-serif">

<!-- ══════════════════  STICKY HEADER  ══════════════════ -->
<header class="sticky top-0 z-50 border-b border-slate-200/80 bg-white/95 backdrop-blur-md shadow-sm">
    <div class="max-w-7xl mx-auto px-5 lg:px-10 h-[68px] flex items-center justify-between gap-4">

        <a href="/" class="flex items-center gap-3 shrink-0">
            <?= bizflowLogoImg(44) ?>
            <span class="hidden sm:block">
                <span class="block font-black tracking-tight text-[17px] text-ink leading-none">BizFlow POS</span>
                <span class="block text-[11px] font-medium text-muted mt-0.5 tracking-wide">Retail made simple</span>
            </span>
        </a>

        <nav class="flex items-center gap-2">
            <a href="#download"
               class="hidden sm:inline-flex items-center px-4 py-2 text-sm font-semibold text-slate-600 hover:text-brand-600 transition rounded-lg hover:bg-brand-50">
                Download
            </a>
            <a href="licensing.php"
               class="hidden sm:inline-flex items-center px-4 py-2 text-sm font-semibold text-slate-600 hover:text-brand-600 transition rounded-lg hover:bg-brand-50">
                Pricing
            </a>
            <a href="licensing.php"
               class="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-brand-700 transition shadow-md shadow-blue-200 active:scale-95">
                Get license
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 7l5 5m0 0-5 5m5-5H6"/>
                </svg>
            </a>
        </nav>
    </div>
</header>

<!-- ══════════════════  HERO  ══════════════════ -->
<section class="relative overflow-hidden gradient-hero">
    <div class="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2 w-[900px] h-[550px] rounded-full bg-blue-200/25 blur-3xl"></div>
    <div class="pointer-events-none absolute bottom-0 right-0 w-96 h-96 rounded-full bg-orange-100/50 blur-3xl"></div>

    <div class="relative max-w-6xl mx-auto px-5 lg:px-10 pt-20 pb-20 grid lg:grid-cols-2 gap-12 items-center">

        <!-- Copy -->
        <div>
            <div class="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-white/80 backdrop-blur px-4 py-2 text-xs font-bold tracking-widest text-brand-700 shadow-sm mb-7">
                <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                VERSION <?= htmlspecialchars($version) ?> · FREE 30-DAY TRIAL
            </div>

            <h1 class="text-4xl sm:text-5xl font-black tracking-tight leading-[1.08] text-ink">
                The POS built for<br>
                <span class="text-brand-600">Kenyan retail.</span>
            </h1>

            <p class="mt-5 text-lg text-muted leading-relaxed max-w-lg">
                Fast sales, real-time inventory, M-Pesa payment matching and
                multi-computer sync — works offline, activates with one file.
            </p>

            <!-- Hero CTAs -->
            <div class="mt-9 flex flex-wrap gap-3">
                <a href="#download"
                   class="inline-flex items-center gap-2.5 rounded-2xl bg-brand-600 px-7 py-4 font-extrabold text-white hover:bg-brand-700 transition shadow-glow active:scale-[.98]">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/>
                    </svg>
                    Download free
                </a>
                <a href="licensing.php"
                   class="inline-flex items-center gap-2 rounded-2xl border border-slate-300 bg-white px-7 py-4 font-bold text-slate-700 hover:bg-slate-50 hover:border-brand-300 transition active:scale-[.98]">
                    View pricing
                </a>
            </div>

            <!-- Micro-trust row -->
            <div class="mt-8 flex flex-wrap items-center gap-5 text-xs font-semibold text-muted">
                <span class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m5 12 4 4L19 6"/>
                    </svg>
                    No credit card required
                </span>
                <span class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m5 12 4 4L19 6"/>
                    </svg>
                    Works offline
                </span>
                <span class="flex items-center gap-1.5">
                    <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m5 12 4 4L19 6"/>
                    </svg>
                    KES pricing
                </span>
            </div>
        </div>

        <!-- Hero logo -->
        <div class="flex justify-center lg:justify-end">
            <div class="relative">
                <div class="absolute inset-0 rounded-3xl bg-brand-100/60 blur-2xl scale-105"></div>
                <div class="relative rounded-3xl bg-white/80 backdrop-blur border border-slate-200/80 shadow-hero p-10 flex items-center justify-center">
                    <?= bizflowLogoImg(180) ?>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- ══════════════════  DOWNLOAD SECTION  ══════════════════ -->
<section id="download" class="bg-slate-50 border-y border-slate-200 py-20">
    <div class="max-w-5xl mx-auto px-5 lg:px-10">

        <div class="text-center mb-12">
            <p class="text-xs font-black tracking-widest text-brand-600 mb-3">FREE DOWNLOAD</p>
            <h2 class="text-3xl sm:text-4xl font-black tracking-tight">Get BizFlow POS</h2>
            <p class="mt-3 text-muted max-w-lg mx-auto">
                Download, install and start selling in minutes.
                A license activates the full product after your free trial.
            </p>
        </div>

        <!-- Download cards -->
        <div class="grid sm:grid-cols-3 gap-5 mb-10">
            <?php foreach ($downloads as $dl): ?>
            <?php
                $isWindows = $dl['id'] === 'windows';
                $isLinux   = $dl['id'] === 'linux';
                $isAndroid = $dl['id'] === 'android';
                $hasFile   = $dl['size'] !== null; // Windows only for now
            ?>
            <div class="download-card group relative flex flex-col rounded-3xl border shadow-card overflow-hidden transition-shadow hover:shadow-lg
                        <?= $dl['primary'] ? 'border-brand-300 bg-white ring-2 ring-brand-100' : 'border-slate-200 bg-white' ?>">

                <?php if ($dl['primary']): ?>
                <div class="absolute top-0 inset-x-0 h-1 bg-gradient-to-r from-brand-500 to-brand-700 rounded-t-3xl"></div>
                <?php endif; ?>

                <div class="p-7 flex-1">
                    <!-- Platform icon -->
                    <div class="w-12 h-12 rounded-2xl mb-5 grid place-items-center
                                <?= $dl['primary'] ? 'bg-brand-600 text-white shadow-md shadow-blue-200' : 'bg-slate-100 text-slate-600' ?>">
                        <?php if ($isWindows): ?>
                        <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M0 3.449 9.75 2.1v9.451H0m10.949-9.602L24 0v11.549H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/>
                        </svg>
                        <?php elseif ($isLinux): ?>
                        <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M12.504 0c-.155 0-.315.008-.48.021-4.226.333-3.105 4.807-3.17 6.298-.076 1.092-.3 1.953-1.05 3.02-.885 1.051-2.127 2.75-2.716 4.521-.278.832-.41 1.684-.287 2.489a.424.424 0 0 0-.11.135c-.26.268-.45.6-.663.839-.199.199-.485.267-.797.4-.313.136-.658.269-.864.68-.09.189-.136.394-.132.602 0 .199.027.4.055.536.058.399.116.728.04.97-.249.68-.28 1.145-.106 1.484.174.334.535.47.94.601.81.2 1.91.135 2.774.6.926.466 1.866.67 2.616.47.526-.116.97-.464 1.208-.946.587-.003 1.23-.269 2.026-.268.795 0 1.439.269 2.028.268.238.482.68.83 1.208.946.752.2 1.686-.004 2.613-.47.862-.465 1.963-.4 2.773-.6.407-.13.768-.267.94-.602.176-.338.145-.803-.106-1.483-.074-.242-.016-.572.04-.97.028-.136.055-.337.055-.54.003-.208-.044-.414-.132-.602-.21-.41-.564-.543-.876-.68-.313-.132-.588-.2-.79-.4-.213-.239-.404-.571-.663-.839a.424.424 0 0 0-.11-.135c.122-.805-.01-1.657-.286-2.49-.587-1.77-1.829-3.47-2.714-4.52-.751-1.067-.974-1.928-1.05-3.02-.067-1.492 1.057-5.966-3.17-6.298-.166-.012-.325-.021-.48-.021zm0 .657c.13 0 .263.006.394.017 3.673.286 2.726 4.4 2.787 5.988.08 1.21.322 2.21 1.17 3.4.878 1.042 2.091 2.715 2.666 4.433.29.866.376 1.698.173 2.394.023.027.051.054.073.084.232.29.433.65.651.9.217.252.5.336.807.48.306.144.65.296.812.639.08.178.098.372.09.556-.008.18-.032.359-.056.479-.063.433-.135.794-.039 1.106.28.775.277 1.163.155 1.4-.123.236-.408.351-.782.465-.742.185-1.861.133-2.817.632-.903.458-1.7.638-2.345.482-.473-.107-.85-.415-1.059-.83-.137.026-.276.04-.415.04-.138 0-.277-.014-.416-.04-.208.415-.587.723-1.059.83-.647.156-1.44-.024-2.346-.482-.955-.499-2.073-.447-2.816-.632-.376-.114-.66-.23-.782-.464-.122-.237-.125-.626.155-1.401.095-.312.023-.673-.04-1.107-.023-.12-.047-.3-.054-.479-.01-.184.008-.378.089-.555.162-.342.508-.495.813-.639.308-.144.59-.228.808-.48.218-.25.42-.61.651-.901.023-.03.05-.056.073-.083-.203-.696-.117-1.528.173-2.394.574-1.718 1.788-3.392 2.666-4.433.848-1.19 1.09-2.19 1.17-3.4.06-1.587-.886-5.702 2.787-5.987a5.63 5.63 0 0 1 .394-.018z"/>
                        </svg>
                        <?php else: ?>
                        <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M17.523 15.3414c-.5511 0-.9993-.4486-.9993-.9997s.4482-.9993.9993-.9993c.5511 0 .9993.4482.9993.9993.0001.5511-.4482.9997-.9993.9997m-11.046 0c-.5511 0-.9993-.4486-.9993-.9997s.4482-.9993.9993-.9993c.5511 0 .9993.4482.9993.9993 0 .5511-.4482.9997-.9993.9997m11.4045-6.02l1.9973-3.4592a.416.416 0 0 0-.1521-.5676.416.416 0 0 0-.5676.1521l-2.0223 3.503C15.5902 8.2439 13.8533 7.8508 12 7.8508s-3.5902.3931-5.1367 1.0989L4.841 5.4467a.4161.4161 0 0 0-.5677-.1521.4157.4157 0 0 0-.1521.5676l1.9973 3.4592C2.6889 11.1867.3432 14.6589 0 18.761h24c-.3435-4.1021-2.6892-7.5743-6.1185-9.4396"/>
                        </svg>
                        <?php endif; ?>
                    </div>

                    <p class="font-extrabold text-base text-ink mb-1"><?= htmlspecialchars($dl['label']) ?></p>
                    <p class="text-xs text-muted font-medium"><?= htmlspecialchars($dl['subtitle']) ?></p>

                    <?php if ($dl['size']): ?>
                    <p class="mt-3 text-xs text-slate-400 font-semibold"><?= htmlspecialchars($dl['size']) ?></p>
                    <?php endif; ?>
                </div>

                <!-- Download button at bottom of card -->
                <div class="px-7 pb-7">
                    <?php if ($dl['primary']): ?>
                    <a href="<?= htmlspecialchars($dl['file']) ?>"
                       download
                       class="dl-arrow flex items-center justify-center gap-2 w-full rounded-xl bg-brand-600 px-5 py-3.5 text-sm font-extrabold text-white hover:bg-brand-700 transition shadow-md shadow-blue-100 active:scale-[.98]">
                        <svg class="w-4 h-4 dl-arrow" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                  d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/>
                        </svg>
                        Download for Windows
                    </a>
                    <?php elseif ($isLinux): ?>
                    <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= urlencode('Hello, I would like the BizFlow POS Linux .deb package.') ?>"
                       target="_blank" rel="noopener"
                       class="flex items-center justify-center gap-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-5 py-3.5 text-sm font-bold text-slate-600 hover:bg-slate-100 transition active:scale-[.98]">
                        <svg class="w-4 h-4 text-emerald-600" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                        </svg>
                        Request via WhatsApp
                    </a>
                    <?php else: /* Android */ ?>
                    <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= urlencode('Hello, I would like the TransRouter APK for M-Pesa Bridge.') ?>"
                       target="_blank" rel="noopener"
                       class="flex items-center justify-center gap-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-5 py-3.5 text-sm font-bold text-slate-600 hover:bg-slate-100 transition active:scale-[.98]">
                        <svg class="w-4 h-4 text-emerald-600" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                        </svg>
                        Request via WhatsApp
                    </a>
                    <?php endif; ?>
                </div>
            </div>
            <?php endforeach; ?>
        </div>

        <!-- Installation note -->
        <div class="rounded-2xl border border-blue-200 bg-brand-50 px-6 py-5 flex items-start gap-4">
            <span class="shrink-0 mt-0.5 w-9 h-9 rounded-xl bg-brand-600 text-white grid place-items-center shadow-sm">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
            </span>
            <div>
                <p class="font-extrabold text-brand-900 text-sm">System requirements</p>
                <p class="text-sm text-brand-800 mt-1 leading-6">
                    <strong>Windows:</strong> Windows 10 or 11 (64-bit), Java 11+ included in installer, 512 MB RAM minimum. &nbsp;·&nbsp;
                    <strong>Linux:</strong> Ubuntu 22.04 / Debian 12 or newer, Java 11+ required. &nbsp;·&nbsp;
                    <strong>TransRouter:</strong> Android 7.0+, SMS permissions required for M-Pesa Bridge.
                </p>
            </div>
        </div>
    </div>
</section>

<!-- ══════════════════  FEATURES  ══════════════════ -->
<section class="py-20 bg-white">
    <div class="max-w-7xl mx-auto px-5 lg:px-10">
        <div class="text-center mb-12">
            <p class="text-xs font-black tracking-widest text-brand-600 mb-3">WHAT'S INCLUDED</p>
            <h2 class="text-3xl sm:text-4xl font-black tracking-tight">Everything your shop needs.</h2>
            <p class="mt-3 text-muted max-w-xl mx-auto">Built for Kenyan retail — works offline, syncs in real time, speaks M-Pesa natively.</p>
        </div>
        <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
            <?php foreach ($features as $f): ?>
            <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-card hover:shadow-lg transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-brand-50 text-brand-600 grid place-items-center mb-4">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="<?= $f['icon'] ?>"/>
                    </svg>
                </div>
                <h3 class="font-extrabold text-sm mb-1.5"><?= htmlspecialchars($f['title']) ?></h3>
                <p class="text-sm text-muted leading-6"><?= htmlspecialchars($f['desc']) ?></p>
            </div>
            <?php endforeach; ?>
        </div>
    </div>
</section>

<!-- ══════════════════  HOW IT WORKS  ══════════════════ -->
<section class="py-20 bg-slate-50 border-y border-slate-200">
    <div class="max-w-4xl mx-auto px-5 lg:px-10">
        <div class="text-center mb-12">
            <p class="text-xs font-black tracking-widest text-brand-600 mb-3">GET STARTED</p>
            <h2 class="text-3xl font-black tracking-tight">Up and running in 4 steps.</h2>
        </div>
        <ol class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <?php
            $steps = [
                ['n'=>'1','title'=>'Download',        'desc'=>'Grab the free installer for your platform above.'],
                ['n'=>'2','title'=>'Install & open',  'desc'=>'Run the setup wizard — Java is bundled on Windows.'],
                ['n'=>'3','title'=>'Try free',         'desc'=>'Use every feature for 30 days, no card needed.'],
                ['n'=>'4','title'=>'Activate',         'desc'=>'Buy a license and paste your key to unlock fully.'],
            ];
            foreach ($steps as $s):
            ?>
            <li class="rounded-2xl bg-white border border-slate-200 shadow-card p-6 text-center">
                <div class="w-10 h-10 rounded-full bg-brand-600 text-white font-black text-lg grid place-items-center mx-auto mb-4 shadow-md shadow-blue-200">
                    <?= $s['n'] ?>
                </div>
                <p class="font-extrabold text-sm mb-2"><?= htmlspecialchars($s['title']) ?></p>
                <p class="text-xs text-muted leading-5"><?= htmlspecialchars($s['desc']) ?></p>
            </li>
            <?php endforeach; ?>
        </ol>
    </div>
</section>

<!-- ══════════════════  PRICING CALLOUT  ══════════════════ -->
<section class="py-20 bg-white">
    <div class="max-w-5xl mx-auto px-5 lg:px-10">
        <div class="relative overflow-hidden rounded-3xl gradient-dark px-8 py-12 lg:px-14 lg:py-14 grid lg:grid-cols-[1.3fr_.7fr] gap-10 items-center">
            <div class="absolute -right-10 -top-10 w-72 h-72 rounded-full bg-brand-600/20 blur-3xl pointer-events-none"></div>
            <div class="relative">
                <p class="text-xs font-black tracking-widest text-brand-400 mb-3">LICENSING</p>
                <h2 class="text-3xl font-black text-white tracking-tight">
                    Starting at KES 2,500/month.
                </h2>
                <p class="mt-4 text-slate-300 leading-7 max-w-lg">
                    Pay annually and save two months. All plans include a 30-day free trial.
                    Activate with a single TXT file — no internet required at point of sale.
                </p>
            </div>
            <div class="relative flex flex-col sm:flex-row lg:flex-col gap-3 lg:items-end">
                <a href="licensing.php"
                   class="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-7 py-4 font-extrabold text-ink hover:bg-blue-50 transition active:scale-[.98] shadow-lg">
                    View pricing
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 7l5 5m0 0-5 5m5-5H6"/>
                    </svg>
                </a>
                <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waText ?>"
                   target="_blank" rel="noopener"
                   class="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/20 px-7 py-4 font-bold text-white hover:bg-white/10 transition">
                    <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                    </svg>
                    Ask us anything
                </a>
            </div>
        </div>
    </div>
</section>

<!-- ══════════════════  FOOTER  ══════════════════ -->
<footer class="border-t border-slate-200 bg-white">
    <div class="max-w-7xl mx-auto px-5 lg:px-10 py-10 flex flex-col sm:flex-row gap-6 justify-between items-start sm:items-center">
        <div class="flex items-center gap-3">
            <?= bizflowLogoImg(36) ?>
            <div>
                <p class="font-extrabold text-sm text-ink">BizFlow POS</p>
                <p class="text-xs text-muted">Professional retail software for Kenya</p>
            </div>
        </div>
        <nav class="flex flex-wrap gap-5 text-sm text-muted">
            <a href="#download"    class="hover:text-brand-600 transition font-medium">Download</a>
            <a href="licensing.php" class="hover:text-brand-600 transition font-medium">Pricing</a>
            <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waText ?>" target="_blank" rel="noopener"
               class="hover:text-brand-600 transition font-medium">Support</a>
        </nav>
        <p class="text-xs text-muted">© 2026 BizFlow POS. All rights reserved.</p>
    </div>
</footer>

</body>
</html>
