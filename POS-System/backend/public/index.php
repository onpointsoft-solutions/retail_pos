<?php
declare(strict_types=1);

require_once __DIR__ . '/logo.php';
require_once __DIR__ . '/../helpers/VisitTracker.php';
VisitTracker::record('home');

$shopUrl     = rtrim(getenv('MAIN_SITE_URL') ?: 'https://mobilemealscenter.co.ke', '/') . '/';
$whatsAppNum = '254742071810';
$waText      = urlencode('Hello, I need help with BizFlow POS.');
$version     = '2.0.0';

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
        'file'     => null,
        'icon'     => 'linux',
        'size'     => null,
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

// ── Feature cards (complete feature set) ─────────────────────────────────────
$features = [
    [
        'tag'   => 'SALES',
        'color' => 'blue',
        'icon'  => 'M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z',
        'title' => 'Fast POS Interface',
        'desc'  => 'Barcode scan, product grid with live stock indicators, quick-search, hold & resume carts, and one-click M-Pesa or cash checkout.',
    ],
    [
        'tag'   => 'INVENTORY',
        'color' => 'emerald',
        'icon'  => 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4',
        'title' => 'Real-time Inventory',
        'desc'  => 'Stock updates the instant a sale or purchase completes. Low-stock alerts, batch tracking, expiry dates, and adjustment history across all workstations.',
    ],
    [
        'tag'   => 'ANALYTICS',
        'color' => 'violet',
        'icon'  => 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z',
        'title' => 'Advanced Analytics',
        'desc'  => 'Income vs Expenses bar chart, P&L waterfall statement, revenue category donut, daily sparkline, and market basket heatmap showing which products sell together.',
    ],
    [
        'tag'   => 'EXPENSES',
        'color' => 'rose',
        'icon'  => 'M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z',
        'title' => 'Expense Management',
        'desc'  => 'Log rent, utilities, salaries, supplies and more with category, date, reference and notes. Expenses flow directly into your P&L and net profit calculation.',
    ],
    [
        'tag'   => 'M-PESA',
        'color' => 'green',
        'icon'  => 'M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z',
        'title' => 'M-Pesa Bridge',
        'desc'  => 'TransRouter reads M-Pesa confirmation SMS on Android and sends payments via UDP to the POS. Cashiers confirm with one tap — no manual reference entry.',
    ],
    [
        'tag'   => 'SYNC',
        'color' => 'sky',
        'icon'  => 'M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4',
        'title' => 'Multi-Computer Sync',
        'desc'  => 'Sales, products, customers and purchase orders sync automatically. Works offline — queues changes and pushes when connectivity returns.',
    ],
    [
        'tag'   => 'SERVICES',
        'color' => 'amber',
        'icon'  => 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z',
        'title' => 'Services & Job Cards',
        'desc'  => 'Create job cards for repairs and service work, assign technicians, track status from Open to Completed, and generate quotations linked to each job.',
    ],
    [
        'tag'   => 'REPORTS',
        'color' => 'indigo',
        'icon'  => 'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
        'title' => 'Professional Reports',
        'desc'  => 'Daily & period sales, profit, tax, best-sellers, cashier performance, sales by category, inventory valuation — export to PDF or Excel.',
    ],
    [
        'tag'   => 'PURCHASES',
        'color' => 'teal',
        'icon'  => 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2',
        'title' => 'Purchase Orders',
        'desc'  => 'Create POs against suppliers, track ordered vs received quantities, receive partial deliveries, and automatically update stock when goods arrive.',
    ],
    [
        'tag'   => 'CUSTOMERS',
        'color' => 'orange',
        'icon'  => 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z',
        'title' => 'Customers & Loyalty',
        'desc'  => 'Customer profiles with loyalty points, credit balance, and purchase history. Attach a customer to any sale for automatic points accumulation.',
    ],
    [
        'tag'   => 'DARK MODE',
        'color' => 'slate',
        'icon'  => 'M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z',
        'title' => 'Dark Mode & Live Refresh',
        'desc'  => 'Full dark/light theme with animated panel transitions and a per-panel auto-refresh system — data updates every 30–120 seconds without interrupting your work.',
    ],
    [
        'tag'   => 'BACKUP',
        'color' => 'cyan',
        'icon'  => 'M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z',
        'title' => 'Backups & Security',
        'desc'  => 'Scheduled automatic backups with one-click restore. Role-based access (Admin, Manager, Cashier) with per-user permission overrides and audit logging.',
    ],
];

// ── Tab showcase ──────────────────────────────────────────────────────────────
$tabs = [
    ['label'=>'Sales',     'desc'=>'Scan products, manage cart, apply discounts, accept M-Pesa or cash.',        'icon'=>'M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z'],
    ['label'=>'Dashboard', 'desc'=>'Live metrics: revenue, profit, pending sync, low-stock count, 7-day trend.', 'icon'=>'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6'],
    ['label'=>'Products',  'desc'=>'Full catalogue with images, categories, suppliers, barcode/QR, and price tiers.','icon'=>'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4'],
    ['label'=>'Analytics', 'desc'=>'Income vs expenses chart, P&L statement, market basket heatmap, daily trend.', 'icon'=>'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'],
    ['label'=>'Services',  'desc'=>'Job cards for repairs, technician assignment, status pipeline, linked quotations.','icon'=>'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z'],
    ['label'=>'Reports',   'desc'=>'10 report types, date-range filters, PDF and Excel export, print support.',    'icon'=>'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z'],
    ['label'=>'Settings',  'desc'=>'Store info, printers, tax rates, sync credentials, backup schedule, users.',   'icon'=>'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z'],
];

// colour map for feature tags
$tagColors = [
    'blue'   => 'bg-blue-50 text-blue-700 border-blue-200',
    'emerald'=> 'bg-emerald-50 text-emerald-700 border-emerald-200',
    'violet' => 'bg-violet-50 text-violet-700 border-violet-200',
    'rose'   => 'bg-rose-50 text-rose-700 border-rose-200',
    'green'  => 'bg-green-50 text-green-700 border-green-200',
    'sky'    => 'bg-sky-50 text-sky-700 border-sky-200',
    'amber'  => 'bg-amber-50 text-amber-700 border-amber-200',
    'indigo' => 'bg-indigo-50 text-indigo-700 border-indigo-200',
    'teal'   => 'bg-teal-50 text-teal-700 border-teal-200',
    'orange' => 'bg-orange-50 text-orange-700 border-orange-200',
    'slate'  => 'bg-slate-100 text-slate-700 border-slate-200',
    'cyan'   => 'bg-cyan-50 text-cyan-700 border-cyan-200',
];
$iconBg = [
    'blue'=>'bg-blue-600','emerald'=>'bg-emerald-600','violet'=>'bg-violet-600',
    'rose'=>'bg-rose-600','green'=>'bg-green-600','sky'=>'bg-sky-600',
    'amber'=>'bg-amber-500','indigo'=>'bg-indigo-600','teal'=>'bg-teal-600',
    'orange'=>'bg-orange-500','slate'=>'bg-slate-700','cyan'=>'bg-cyan-600',
];

?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>BizFlow POS — Professional Retail &amp; Analytics Software for Kenya</title>
<meta name="description" content="BizFlow POS: fast sales, real-time inventory, M-Pesa bridge, P&L analytics, market basket analysis, job cards and multi-computer sync — built for Kenyan retail.">
<link rel="icon" type="image/png" href="logo.png">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:opsz,wght@14..32,300;14..32,400;14..32,500;14..32,600;14..32,700;14..32,800;14..32,900&display=swap" rel="stylesheet">
<script src="https://cdn.tailwindcss.com"></script>
<script>
tailwind.config = {
    theme: {
        extend: {
            colors: {
                brand:{ 50:'#eff6ff',100:'#dbeafe',200:'#bfdbfe',500:'#3b82f6',600:'#2563eb',700:'#1d4ed8',800:'#1e40af' },
                ink:'#0f172a', muted:'#64748b'
            },
            fontFamily:{ sans:['Inter','ui-sans-serif','system-ui','sans-serif'] },
            boxShadow:{
                card:'0 4px 24px -4px rgba(15,23,42,.10),0 1px 4px -1px rgba(15,23,42,.06)',
                glow:'0 8px 32px -4px rgba(37,99,235,.28)',
            }
        }
    }
}
</script>
<style>
.gradient-hero{background:linear-gradient(140deg,#eff6ff 0%,#f8fafc 55%,#fdf4ff 100%)}
.gradient-dark{background:linear-gradient(160deg,#0f172a 0%,#1e293b 100%)}
.tab-pill.active{background:#2563eb;color:#fff;box-shadow:0 4px 14px -2px rgba(37,99,235,.40)}
.tab-pill{transition:all .18s ease}
.tab-content{display:none}.tab-content.active{display:flex}
</style>
</head>
<body class="bg-white text-ink antialiased" style="font-family:'Inter',sans-serif">

<!-- ══ HEADER ══ -->
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
      <a href="#features" class="hidden sm:inline-flex items-center px-4 py-2 text-sm font-semibold text-slate-600 hover:text-brand-600 transition rounded-lg hover:bg-brand-50">Features</a>
      <a href="#download" class="hidden sm:inline-flex items-center px-4 py-2 text-sm font-semibold text-slate-600 hover:text-brand-600 transition rounded-lg hover:bg-brand-50">Download</a>
      <a href="licensing.php" class="hidden sm:inline-flex items-center px-4 py-2 text-sm font-semibold text-slate-600 hover:text-brand-600 transition rounded-lg hover:bg-brand-50">Pricing</a>
      <a href="licensing.php" class="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-brand-700 transition shadow-md shadow-blue-200 active:scale-95">
        Get license
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 7l5 5m0 0-5 5m5-5H6"/></svg>
      </a>
    </nav>
  </div>
</header>

<!-- ══ HERO ══ -->
<section class="relative overflow-hidden gradient-hero">
  <div class="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2 w-[900px] h-[550px] rounded-full bg-blue-200/25 blur-3xl"></div>
  <div class="pointer-events-none absolute bottom-0 right-0 w-96 h-96 rounded-full bg-violet-100/40 blur-3xl"></div>
  <div class="relative max-w-6xl mx-auto px-5 lg:px-10 pt-20 pb-20 grid lg:grid-cols-2 gap-12 items-center">
    <div>
      <div class="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-white/80 backdrop-blur px-4 py-2 text-xs font-bold tracking-widest text-brand-700 shadow-sm mb-7">
        <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        VERSION <?= htmlspecialchars($version) ?> · FREE 30-DAY TRIAL
      </div>
      <h1 class="text-4xl sm:text-5xl font-black tracking-tight leading-[1.08] text-ink">
        POS &amp; analytics<br>built for<br><span class="text-brand-600">Kenyan retail.</span>
      </h1>
      <p class="mt-5 text-lg text-muted leading-relaxed max-w-lg">
        Fast sales, real-time inventory, M-Pesa payment matching, full P&amp;L analytics,
        market basket insights and multi-computer sync — works offline, activates with one file.
      </p>
      <div class="mt-9 flex flex-wrap gap-3">
        <a href="#download" class="inline-flex items-center gap-2.5 rounded-2xl bg-brand-600 px-7 py-4 font-extrabold text-white hover:bg-brand-700 transition shadow-glow active:scale-[.98]">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/></svg>
          Download free
        </a>
        <a href="#features" class="inline-flex items-center gap-2 rounded-2xl border border-slate-300 bg-white px-7 py-4 font-bold text-slate-700 hover:bg-slate-50 hover:border-brand-300 transition active:scale-[.98]">
          See all features
        </a>
      </div>
      <div class="mt-8 flex flex-wrap items-center gap-5 text-xs font-semibold text-muted">
        <?php foreach(['No credit card required','Works offline','KES pricing','12 powerful modules'] as $t): ?>
        <span class="flex items-center gap-1.5">
          <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m5 12 4 4L19 6"/></svg>
          <?= htmlspecialchars($t) ?>
        </span>
        <?php endforeach; ?>
      </div>
    </div>
    <div class="flex justify-center lg:justify-end">
      <div class="relative">
        <div class="absolute inset-0 rounded-3xl bg-brand-100/60 blur-2xl scale-105"></div>
        <div class="relative rounded-3xl bg-white/80 backdrop-blur border border-slate-200/80 shadow-[0_32px_72px_-12px_rgba(37,99,235,.18)] p-10 flex items-center justify-center">
          <?= bizflowLogoImg(180) ?>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- ══ MODULE TABS ══ -->
<section class="py-20 bg-white border-b border-slate-100">
  <div class="max-w-6xl mx-auto px-5 lg:px-10">
    <div class="text-center mb-10">
      <p class="text-xs font-black tracking-widest text-brand-600 mb-3">INSIDE THE APP</p>
      <h2 class="text-3xl sm:text-4xl font-black tracking-tight">Every module, one system.</h2>
      <p class="mt-3 text-muted max-w-xl mx-auto">BizFlow POS ships <?= count($tabs) ?> fully integrated tabs. No add-ons, no hidden fees.</p>
    </div>
    <!-- Tab pills -->
    <div class="flex flex-wrap justify-center gap-2 mb-8" id="tabPills">
      <?php foreach($tabs as $i => $t): ?>
      <button onclick="switchTab(<?= $i ?>)"
              class="tab-pill <?= $i===0?'active':'' ?> flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-semibold border border-slate-200 text-slate-600 cursor-pointer"
              data-tab="<?= $i ?>">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="<?= $t['icon'] ?>"/></svg>
        <?= htmlspecialchars($t['label']) ?>
      </button>
      <?php endforeach; ?>
    </div>
    <!-- Tab content panels -->
    <div class="rounded-3xl border border-slate-200 bg-slate-50 overflow-hidden shadow-card">
      <?php foreach($tabs as $i => $t): ?>
      <div class="tab-content <?= $i===0?'active':'' ?> items-center gap-10 p-8 lg:p-12" id="tabContent<?= $i ?>">
        <div class="shrink-0 w-16 h-16 rounded-2xl bg-brand-600 text-white grid place-items-center shadow-lg shadow-blue-200">
          <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="<?= $t['icon'] ?>"/></svg>
        </div>
        <div>
          <h3 class="text-2xl font-black text-ink mb-2"><?= htmlspecialchars($t['label']) ?></h3>
          <p class="text-muted text-base leading-7 max-w-xl"><?= htmlspecialchars($t['desc']) ?></p>
        </div>
      </div>
      <?php endforeach; ?>
    </div>
  </div>
</section>
<script>
function switchTab(idx) {
    document.querySelectorAll('.tab-pill').forEach((p,i) => p.classList.toggle('active', i===idx));
    document.querySelectorAll('.tab-content').forEach((c,i) => c.classList.toggle('active', i===idx));
}
</script>

<!-- ══ ANALYTICS DEEP-DIVE ══ -->
<section class="py-20 bg-slate-50 border-y border-slate-200">
  <div class="max-w-6xl mx-auto px-5 lg:px-10">
    <div class="grid lg:grid-cols-2 gap-16 items-center">
      <div>
        <p class="text-xs font-black tracking-widest text-violet-600 mb-3">ANALYTICS</p>
        <h2 class="text-3xl sm:text-4xl font-black tracking-tight text-ink">Know your numbers.</h2>
        <p class="mt-4 text-muted leading-7">
          The Analytics tab combines every financial signal into one screen.
          Select any date range and click <strong>Analyse</strong>.
        </p>
        <ul class="mt-6 space-y-3">
          <?php foreach([
            ['Income vs Expenses bar chart', 'Monthly grouped bars — revenue, COGS and operating expenses — with a net profit overlay line and hover tooltips.'],
            ['Profit & Loss waterfall', 'Structured P&L: revenue → COGS → gross profit → operating expenses by category → net profit, with margin percentages.'],
            ['Market basket heatmap', '10×10 co-occurrence matrix showing which products sell together. Table includes support %, confidence and lift score.'],
            ['Revenue category donut', 'Visual breakdown of revenue share per product category with percentage legend.'],
            ['Daily revenue sparkline', 'Dual-area chart of daily revenue and profit over the selected period.'],
            ['Expense management', 'Add, edit and delete expenses (9 categories) with reference, notes and date. Flows directly into P&L.'],
          ] as [$title,$sub]): ?>
          <li class="flex gap-3">
            <span class="shrink-0 mt-1 w-5 h-5 rounded-full bg-violet-100 text-violet-600 grid place-items-center">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="m5 12 4 4L19 6"/></svg>
            </span>
            <div>
              <span class="font-bold text-ink text-sm"><?= htmlspecialchars($title) ?></span>
              <p class="text-xs text-muted mt-0.5"><?= htmlspecialchars($sub) ?></p>
            </div>
          </li>
          <?php endforeach; ?>
        </ul>
      </div>
      <!-- Illustration card -->
      <div class="rounded-3xl border border-violet-200 bg-white p-8 shadow-card space-y-4">
        <div class="flex items-center justify-between mb-2">
          <span class="font-black text-sm text-ink">Analytics — Aug 2026</span>
          <span class="text-xs font-semibold text-violet-600 bg-violet-50 border border-violet-200 px-3 py-1 rounded-full">Live P&amp;L</span>
        </div>
        <?php
        $plRows = [
            ['Gross Revenue',          'KES 284,500', 'text-emerald-600 font-bold'],
            ['Cost of Goods Sold',     '– KES 134,200','text-rose-600'],
            ['Gross Profit (53%)',      'KES 150,300', 'text-emerald-600 font-bold'],
            ['Operating Expenses',     '– KES 42,800', 'text-rose-600'],
            ['Net Profit (37.8%)',      'KES 107,500', 'text-brand-600 font-extrabold text-base'],
        ];
        foreach($plRows as [$label,$val,$cls]): ?>
        <div class="flex justify-between items-center py-2 border-b border-slate-100 last:border-0">
          <span class="text-sm text-muted"><?= htmlspecialchars($label) ?></span>
          <span class="text-sm <?= $cls ?>"><?= htmlspecialchars($val) ?></span>
        </div>
        <?php endforeach; ?>
        <div class="mt-4 grid grid-cols-3 gap-3 text-center">
          <?php foreach([['Income','KES 284K','emerald'],['COGS','KES 134K','amber'],['Net','KES 107K','brand']] as [$l,$v,$c]): ?>
          <div class="rounded-2xl bg-<?= $c ?>-50 border border-<?= $c ?>-200 p-3">
            <p class="text-xs text-<?= $c ?>-600 font-bold"><?= $l ?></p>
            <p class="text-sm font-extrabold text-<?= $c ?>-700 mt-1"><?= $v ?></p>
          </div>
          <?php endforeach; ?>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- ══ FEATURE GRID ══ -->
<section id="features" class="py-20 bg-white">
  <div class="max-w-7xl mx-auto px-5 lg:px-10">
    <div class="text-center mb-12">
      <p class="text-xs font-black tracking-widest text-brand-600 mb-3">FULL FEATURE SET</p>
      <h2 class="text-3xl sm:text-4xl font-black tracking-tight">Everything your shop needs.</h2>
      <p class="mt-3 text-muted max-w-xl mx-auto">12 modules, all included in every plan. Built for Kenyan retail — works offline, speaks M-Pesa natively.</p>
    </div>
    <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
      <?php foreach($features as $f): $bg = $iconBg[$f['color']] ?? 'bg-brand-600'; $tag = $tagColors[$f['color']] ?? ''; ?>
      <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-card hover:shadow-lg transition-shadow group">
        <div class="flex items-start justify-between mb-4">
          <div class="w-10 h-10 rounded-xl <?= $bg ?> text-white grid place-items-center shadow-sm">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="<?= $f['icon'] ?>"/>
            </svg>
          </div>
          <span class="text-[10px] font-black tracking-widest border rounded-full px-2.5 py-1 <?= $tag ?>">
            <?= htmlspecialchars($f['tag']) ?>
          </span>
        </div>
        <h3 class="font-extrabold text-sm mb-1.5"><?= htmlspecialchars($f['title']) ?></h3>
        <p class="text-sm text-muted leading-6"><?= htmlspecialchars($f['desc']) ?></p>
      </div>
      <?php endforeach; ?>
    </div>
  </div>
</section>

<!-- ══ MARKET BASKET CALLOUT ══ -->
<section class="py-20 bg-slate-50 border-y border-slate-200">
  <div class="max-w-6xl mx-auto px-5 lg:px-10">
    <div class="grid lg:grid-cols-2 gap-16 items-center">
      <!-- Heatmap mock -->
      <div class="rounded-3xl border border-slate-200 bg-white p-8 shadow-card overflow-x-auto">
        <p class="font-black text-sm text-ink mb-5">Market Basket — Products Bought Together</p>
        <?php
        $products = ['Bread','Milk','Sugar','Tea','Eggs'];
        $matrix = [
          [0,18,12,9,7],
          [18,0,14,11,6],
          [12,14,0,8,4],
          [9,11,8,0,3],
          [7,6,4,3,0],
        ];
        ?>
        <div class="overflow-x-auto">
          <table class="text-xs border-collapse w-full">
            <thead>
              <tr>
                <th class="w-16"></th>
                <?php foreach($products as $p): ?>
                <th class="text-center pb-2 text-muted font-semibold text-[10px]"><?= htmlspecialchars($p) ?></th>
                <?php endforeach; ?>
              </tr>
            </thead>
            <tbody>
              <?php foreach($products as $ri => $row): ?>
              <tr>
                <td class="pr-3 text-right text-muted font-semibold text-[10px] py-1"><?= htmlspecialchars($row) ?></td>
                <?php foreach($products as $ci => $col): ?>
                <?php
                  $v = $matrix[$ri][$ci];
                  $alpha = $ri===$ci ? '0' : round($v/18*0.9+0.05,2);
                  $bg = $ri===$ci ? 'bg-slate-100' : ($v>=14?'bg-brand-600':($v>=8?'bg-brand-400':'bg-brand-200'));
                ?>
                <td class="p-0.5">
                  <div class="<?= $bg ?> rounded text-center text-white font-bold leading-6 <?= $ri===$ci?'opacity-0':'' ?>"
                       style="min-width:28px;min-height:24px;font-size:10px;opacity:<?= $ri===$ci?0:($alpha) ?>">
                    <?= $v > 0 && $ri!==$ci ? $v : '' ?>
                  </div>
                </td>
                <?php endforeach; ?>
              </tr>
              <?php endforeach; ?>
            </tbody>
          </table>
        </div>
        <p class="mt-4 text-xs text-muted">Darker = more co-purchases. Use this to position related products together on your shelf.</p>
      </div>
      <div>
        <p class="text-xs font-black tracking-widest text-sky-600 mb-3">MARKET BASKET</p>
        <h2 class="text-3xl font-black tracking-tight text-ink">Know what sells together.</h2>
        <p class="mt-4 text-muted leading-7">
          BizFlow analyses every transaction to find products your customers naturally buy together.
          Use that data to place high-lift pairs side by side on the shelf and increase basket size.
        </p>
        <ul class="mt-6 space-y-3 text-sm">
          <?php foreach([
              'Co-occurrence count — how often two products appear in the same sale',
              'Support % — fraction of all transactions that include both products',
              'Confidence — given product A, how likely is product B in the same cart',
              'Lift score — values above 1.5 signal a genuine buying relationship',
          ] as $item): ?>
          <li class="flex gap-2 text-muted">
            <span class="mt-1 shrink-0 w-4 h-4 rounded-full bg-sky-100 text-sky-600 grid place-items-center">
              <svg class="w-2.5 h-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="m5 12 4 4L19 6"/></svg>
            </span>
            <?= htmlspecialchars($item) ?>
          </li>
          <?php endforeach; ?>
        </ul>
      </div>
    </div>
  </div>
</section>

<!-- ══ DOWNLOAD ══ -->
<section id="download" class="bg-slate-50 border-y border-slate-200 py-20">
  <div class="max-w-5xl mx-auto px-5 lg:px-10">
    <div class="text-center mb-12">
      <p class="text-xs font-black tracking-widest text-brand-600 mb-3">FREE DOWNLOAD</p>
      <h2 class="text-3xl sm:text-4xl font-black tracking-tight">Get BizFlow POS</h2>
      <p class="mt-3 text-muted max-w-lg mx-auto">Download, install and start selling in minutes. A license unlocks everything after your 30-day trial.</p>
    </div>
    <div class="grid sm:grid-cols-3 gap-5 mb-10">
      <?php foreach($downloads as $dl):
        $isWindows = $dl['id']==='windows'; $isAndroid = $dl['id']==='android'; ?>
      <div class="group relative flex flex-col rounded-3xl border shadow-card overflow-hidden transition-shadow hover:shadow-lg
                  <?= $dl['primary'] ? 'border-brand-300 bg-white ring-2 ring-brand-100' : 'border-slate-200 bg-white' ?>">
        <?php if($dl['primary']): ?><div class="absolute top-0 inset-x-0 h-1 bg-gradient-to-r from-brand-500 to-brand-700 rounded-t-3xl"></div><?php endif; ?>
        <div class="p-7 flex-1">
          <div class="w-12 h-12 rounded-2xl mb-5 grid place-items-center <?= $dl['primary']?'bg-brand-600 text-white shadow-md shadow-blue-200':'bg-slate-100 text-slate-600' ?>">
            <?php if($isWindows): ?>
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor"><path d="M0 3.449 9.75 2.1v9.451H0m10.949-9.602L24 0v11.549H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/></svg>
            <?php elseif($dl['id']==='linux'): ?>
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor"><path d="M12.504 0c-.155 0-.315.008-.48.021-4.226.333-3.105 4.807-3.17 6.298-.076 1.092-.3 1.953-1.05 3.02-.885 1.051-2.127 2.75-2.716 4.521-.278.832-.41 1.684-.287 2.489a.424.424 0 0 0-.11.135c-.26.268-.45.6-.663.839-.199.199-.485.267-.797.4-.313.136-.658.269-.864.68-.09.189-.136.394-.132.602 0 .199.027.4.055.536.058.399.116.728.04.97-.249.68-.28 1.145-.106 1.484.174.334.535.47.94.601.81.2 1.91.135 2.774.6.926.466 1.866.67 2.616.47.526-.116.97-.464 1.208-.946.587-.003 1.23-.269 2.026-.268.795 0 1.439.269 2.028.268.238.482.68.83 1.208.946.752.2 1.686-.004 2.613-.47.862-.465 1.963-.4 2.773-.6.407-.13.768-.267.94-.602.176-.338.145-.803-.106-1.483-.074-.242-.016-.572.04-.97.028-.136.055-.337.055-.54.003-.208-.044-.414-.132-.602-.21-.41-.564-.543-.876-.68-.313-.132-.588-.2-.79-.4-.213-.239-.404-.571-.663-.839a.424.424 0 0 0-.11-.135c.122-.805-.01-1.657-.286-2.49-.587-1.77-1.829-3.47-2.714-4.52-.751-1.067-.974-1.928-1.05-3.02-.067-1.492 1.057-5.966-3.17-6.298-.166-.012-.325-.021-.48-.021z"/></svg>
            <?php else: ?>
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor"><path d="M17.523 15.3414c-.5511 0-.9993-.4486-.9993-.9997s.4482-.9993.9993-.9993c.5511 0 .9993.4482.9993.9993.0001.5511-.4482.9997-.9993.9997m-11.046 0c-.5511 0-.9993-.4486-.9993-.9997s.4482-.9993.9993-.9993c.5511 0 .9993.4482.9993.9993 0 .5511-.4482.9997-.9993.9997m11.4045-6.02l1.9973-3.4592a.416.416 0 0 0-.1521-.5676.416.416 0 0 0-.5676.1521l-2.0223 3.503C15.5902 8.2439 13.8533 7.8508 12 7.8508s-3.5902.3931-5.1367 1.0989L4.841 5.4467a.4161.4161 0 0 0-.5677-.1521.4157.4157 0 0 0-.1521.5676l1.9973 3.4592C2.6889 11.1867.3432 14.6589 0 18.761h24c-.3435-4.1021-2.6892-7.5743-6.1185-9.4396"/></svg>
            <?php endif; ?>
          </div>
          <p class="font-extrabold text-base text-ink mb-1"><?= htmlspecialchars($dl['label']) ?></p>
          <p class="text-xs text-muted font-medium"><?= htmlspecialchars($dl['subtitle']) ?></p>
          <?php if($dl['size']): ?><p class="mt-3 text-xs text-slate-400 font-semibold"><?= htmlspecialchars($dl['size']) ?></p><?php endif; ?>
        </div>
        <div class="px-7 pb-7">
          <?php if($dl['primary'] && $dl['file']): ?>
          <a href="<?= htmlspecialchars($dl['file']) ?>" download
             class="flex items-center justify-center gap-2 w-full rounded-xl bg-brand-600 px-5 py-3.5 text-sm font-extrabold text-white hover:bg-brand-700 transition shadow-md shadow-blue-100 active:scale-[.98]">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/></svg>
            Download for Windows
          </a>
          <?php else: ?>
          <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= urlencode('Hello, I would like the BizFlow POS ' . $dl['label']) ?>"
             target="_blank" rel="noopener"
             class="flex items-center justify-center gap-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-5 py-3.5 text-sm font-bold text-slate-600 hover:bg-slate-100 transition active:scale-[.98]">
            <svg class="w-4 h-4 text-emerald-600" fill="currentColor" viewBox="0 0 24 24"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
            Request via WhatsApp
          </a>
          <?php endif; ?>
        </div>
      </div>
      <?php endforeach; ?>
    </div>
    <!-- Requirements note -->
    <div class="rounded-2xl border border-blue-200 bg-brand-50 px-6 py-5 flex items-start gap-4">
      <span class="shrink-0 mt-0.5 w-9 h-9 rounded-xl bg-brand-600 text-white grid place-items-center shadow-sm">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
      </span>
      <div>
        <p class="font-extrabold text-brand-900 text-sm">System requirements</p>
        <p class="text-sm text-brand-800 mt-1 leading-6">
          <strong>Windows:</strong> 10 or 11 (64-bit), Java 11+ bundled in installer, 512 MB RAM minimum. &nbsp;·&nbsp;
          <strong>Linux:</strong> Ubuntu 22.04 / Debian 12+, Java 11+ required separately. &nbsp;·&nbsp;
          <strong>TransRouter:</strong> Android 7.0+, SMS permissions required.
        </p>
      </div>
    </div>
  </div>
</section>

<!-- ══ HOW IT WORKS ══ -->
<section class="py-20 bg-white">
  <div class="max-w-4xl mx-auto px-5 lg:px-10">
    <div class="text-center mb-12">
      <p class="text-xs font-black tracking-widest text-brand-600 mb-3">GET STARTED</p>
      <h2 class="text-3xl font-black tracking-tight">Up and running in 4 steps.</h2>
    </div>
    <ol class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
      <?php foreach([
        ['1','Download','Grab the free Windows installer or request Linux / APK via WhatsApp.'],
        ['2','Install & open','Run the setup wizard — Java runtime is bundled on Windows.'],
        ['3','Try free for 30 days','Every feature unlocked — no credit card, no time pressure.'],
        ['4','Activate','Buy a license key, paste it in Settings → License, and you\'re live.'],
      ] as [$n,$t,$d]): ?>
      <li class="rounded-2xl bg-white border border-slate-200 shadow-card p-6 text-center">
        <div class="w-10 h-10 rounded-full bg-brand-600 text-white font-black text-lg grid place-items-center mx-auto mb-4 shadow-md shadow-blue-200"><?= $n ?></div>
        <p class="font-extrabold text-sm mb-2"><?= htmlspecialchars($t) ?></p>
        <p class="text-xs text-muted leading-5"><?= htmlspecialchars($d) ?></p>
      </li>
      <?php endforeach; ?>
    </ol>
  </div>
</section>

<!-- ══ PRICING CALLOUT ══ -->
<section class="py-20 bg-slate-50 border-t border-slate-200">
  <div class="max-w-5xl mx-auto px-5 lg:px-10">
    <div class="relative overflow-hidden rounded-3xl gradient-dark px-8 py-12 lg:px-14 lg:py-14 grid lg:grid-cols-[1.3fr_.7fr] gap-10 items-center">
      <div class="absolute -right-10 -top-10 w-72 h-72 rounded-full bg-brand-600/20 blur-3xl pointer-events-none"></div>
      <div class="relative">
        <p class="text-xs font-black tracking-widest text-brand-400 mb-3">LICENSING</p>
        <h2 class="text-3xl font-black text-white tracking-tight">Starting at KES 2,500/month.</h2>
        <p class="mt-4 text-slate-300 leading-7 max-w-lg">
          Pay annually and save two months. All plans include every feature and a 30-day free trial.
          Activate with a single TXT file — no internet required at the point of sale.
        </p>
        <div class="mt-6 flex flex-wrap gap-3 text-xs font-semibold text-slate-400">
          <?php foreach(['Starter — 1 computer','Business — 5 computers','Enterprise — 20 computers'] as $p): ?>
          <span class="flex items-center gap-1.5">
            <span class="w-1.5 h-1.5 rounded-full bg-brand-500"></span><?= htmlspecialchars($p) ?>
          </span>
          <?php endforeach; ?>
        </div>
      </div>
      <div class="relative flex flex-col sm:flex-row lg:flex-col gap-3 lg:items-end">
        <a href="licensing.php" class="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-7 py-4 font-extrabold text-ink hover:bg-blue-50 transition active:scale-[.98] shadow-lg">
          View pricing
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 7l5 5m0 0-5 5m5-5H6"/></svg>
        </a>
        <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waText ?>" target="_blank" rel="noopener"
           class="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/20 px-7 py-4 font-bold text-white hover:bg-white/10 transition">
          <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
          Ask us anything
        </a>
      </div>
    </div>
  </div>
</section>

<!-- ══ FOOTER ══ -->
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
      <a href="#features"    class="hover:text-brand-600 transition font-medium">Features</a>
      <a href="#download"    class="hover:text-brand-600 transition font-medium">Download</a>
      <a href="licensing.php" class="hover:text-brand-600 transition font-medium">Pricing</a>
      <a href="https://wa.me/<?= $whatsAppNum ?>?text=<?= $waText ?>" target="_blank" rel="noopener"
         class="hover:text-brand-600 transition font-medium">Support</a>
    </nav>
    <p class="text-xs text-muted">© <?= date('Y') ?> BizFlow POS. All rights reserved.</p>
  </div>
</footer>

</body>
</html>
