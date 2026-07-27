<?php
$pageTitle = 'BizFlow POS Licensing & Pricing';
$whatsAppNumber = '254742071810';
$plans = [
    [
        'name' => 'Starter',
        'code' => 'STARTER',
        'description' => 'Everything a single growing shop needs to sell professionally.',
        'monthly' => 2500,
        'annual' => 25000,
        'devices' => '1 computer',
        'popular' => false,
        'features' => [
            'Complete POS and inventory',
            'Professional receipts and reports',
            'Product image synchronization',
            '30-day free trial',
            'Email support',
        ],
    ],
    [
        'name' => 'Business',
        'code' => 'BUSINESS',
        'description' => 'Built for established shops and teams working across several computers.',
        'monthly' => 5500,
        'annual' => 55000,
        'devices' => 'Up to 5 computers',
        'popular' => true,
        'features' => [
            'Everything in Starter',
            'Seamless multi-computer sync',
            'M-Pesa Bridge transactions',
            'Automated backups and imports',
            'Priority WhatsApp support',
        ],
    ],
    [
        'name' => 'Enterprise',
        'code' => 'ENTERPRISE',
        'description' => 'Advanced deployment for multi-branch and high-volume retail operations.',
        'monthly' => 12000,
        'annual' => 120000,
        'devices' => 'Up to 20 computers',
        'popular' => false,
        'features' => [
            'Everything in Business',
            'Multi-branch deployment support',
            'Large-scale synchronization',
            'Priority onboarding and migration',
            'Dedicated support channel',
        ],
    ],
];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($pageTitle) ?></title>
    <meta name="description" content="Choose a BizFlow POS license package in Kenya shillings. Start with a free 30-day trial.">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: { 50:'#EFF6FF', 100:'#DBEAFE', 500:'#3B82F6', 600:'#2563EB', 700:'#1D4ED8' },
                        ink: '#0F172A',
                        muted: '#64748B'
                    },
                    boxShadow: {
                        soft: '0 20px 50px -24px rgba(15, 23, 42, .25)'
                    }
                }
            }
        }
    </script>
</head>
<body class="bg-slate-50 text-ink antialiased" style="font-family:Inter,sans-serif" x-data="{annual:true}">
    <header class="border-b border-slate-200 bg-white/90 backdrop-blur sticky top-0 z-40">
        <div class="max-w-7xl mx-auto px-5 lg:px-8 h-20 flex items-center justify-between">
            <a href="index.php" class="flex items-center gap-3">
                <span class="w-11 h-11 rounded-2xl bg-brand-600 text-white grid place-items-center shadow-lg shadow-blue-200">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7h16M5 7l1 13h12l1-13M9 11v5m6-5v5M8 7l1-3h6l1 3"/>
                    </svg>
                </span>
                <span>
                    <span class="block font-extrabold tracking-tight text-lg">BizFlow POS</span>
                    <span class="block text-xs text-muted">Retail made simple</span>
                </span>
            </a>
            <div class="flex items-center gap-3">
                <a href="index.php" class="hidden sm:inline-flex px-4 py-2 text-sm font-semibold text-slate-600 hover:text-brand-600">Back to shop</a>
                <a href="https://wa.me/<?= $whatsAppNumber ?>?text=<?= urlencode('Hello, I would like to activate BizFlow POS.') ?>"
                   target="_blank"
                   class="inline-flex items-center rounded-xl bg-brand-600 px-5 py-3 text-sm font-bold text-white hover:bg-brand-700 transition shadow-lg shadow-blue-200">
                    Talk to sales
                </a>
            </div>
        </div>
    </header>

    <main>
        <section class="relative overflow-hidden">
            <div class="absolute inset-0 bg-gradient-to-b from-brand-50 to-slate-50"></div>
            <div class="absolute -top-24 left-1/2 -translate-x-1/2 w-[700px] h-[400px] rounded-full bg-blue-200/40 blur-3xl"></div>
            <div class="relative max-w-5xl mx-auto px-5 pt-20 pb-14 text-center">
                <span class="inline-flex rounded-full border border-blue-200 bg-white px-4 py-2 text-xs font-bold tracking-wide text-brand-700">
                    30-DAY FREE TRIAL · NO PAYMENT REQUIRED
                </span>
                <h1 class="mt-7 text-4xl sm:text-6xl font-extrabold tracking-tight">
                    Professional retail software<br class="hidden sm:block">
                    priced for <span class="text-brand-600">Kenyan businesses.</span>
                </h1>
                <p class="mt-6 text-lg text-muted max-w-3xl mx-auto leading-8">
                    Sell faster, manage stock, receive M-Pesa references and keep every computer synchronized.
                    Start free for one month, then activate the package that fits your shop.
                </p>

                <div class="mt-9 inline-flex rounded-2xl border border-slate-200 bg-white p-1.5 shadow-sm">
                    <button @click="annual=false"
                            :class="annual ? 'text-slate-500' : 'bg-ink text-white shadow'"
                            class="rounded-xl px-5 py-2.5 text-sm font-bold transition">Monthly</button>
                    <button @click="annual=true"
                            :class="annual ? 'bg-ink text-white shadow' : 'text-slate-500'"
                            class="rounded-xl px-5 py-2.5 text-sm font-bold transition">
                        Annual <span class="text-emerald-400">Save 2 months</span>
                    </button>
                </div>
            </div>
        </section>

        <section class="max-w-7xl mx-auto px-5 lg:px-8 pb-24">
            <div class="grid lg:grid-cols-3 gap-6 items-stretch">
                <?php foreach ($plans as $plan): ?>
                    <article class="relative flex flex-col rounded-3xl bg-white p-7 lg:p-8 shadow-soft border <?= $plan['popular'] ? 'border-brand-500 ring-4 ring-blue-100' : 'border-slate-200' ?>">
                        <?php if ($plan['popular']): ?>
                            <span class="absolute -top-3.5 left-1/2 -translate-x-1/2 rounded-full bg-brand-600 px-4 py-1.5 text-xs font-extrabold text-white tracking-wide">
                                MOST POPULAR
                            </span>
                        <?php endif; ?>
                        <div>
                            <p class="text-sm font-extrabold tracking-wide text-brand-600"><?= htmlspecialchars(strtoupper($plan['name'])) ?></p>
                            <p class="mt-3 text-sm text-muted leading-6 min-h-[48px]"><?= htmlspecialchars($plan['description']) ?></p>
                            <div class="mt-7 flex items-end gap-2">
                                <span class="text-sm font-semibold text-slate-500 mb-2">KES</span>
                                <span class="text-4xl font-extrabold tracking-tight" x-text="annual ? '<?= number_format($plan['annual']) ?>' : '<?= number_format($plan['monthly']) ?>'"></span>
                                <span class="text-sm text-muted mb-2" x-text="annual ? '/year' : '/month'"></span>
                            </div>
                            <p class="mt-2 text-sm font-semibold text-slate-600"><?= htmlspecialchars($plan['devices']) ?></p>
                        </div>
                        <ul class="mt-8 space-y-4 flex-1">
                            <?php foreach ($plan['features'] as $feature): ?>
                                <li class="flex gap-3 text-sm text-slate-700">
                                    <span class="mt-0.5 w-5 h-5 shrink-0 rounded-full bg-emerald-100 text-emerald-700 grid place-items-center">
                                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="m5 12 4 4L19 6"/>
                                        </svg>
                                    </span>
                                    <?= htmlspecialchars($feature) ?>
                                </li>
                            <?php endforeach; ?>
                        </ul>
                        <?php
                            $message = "Hello, I want the {$plan['name']} BizFlow POS package ("
                                . ($plan['popular'] ? 'recommended' : $plan['devices']) . ").";
                        ?>
                        <a href="https://wa.me/<?= $whatsAppNumber ?>?text=<?= urlencode($message) ?>"
                           target="_blank"
                           class="mt-9 inline-flex justify-center rounded-xl px-5 py-3.5 text-sm font-extrabold transition <?= $plan['popular'] ? 'bg-brand-600 text-white hover:bg-brand-700 shadow-lg shadow-blue-200' : 'bg-slate-100 text-ink hover:bg-slate-200' ?>">
                            Choose <?= htmlspecialchars($plan['name']) ?>
                        </a>
                    </article>
                <?php endforeach; ?>
            </div>

            <div class="mt-16 rounded-3xl bg-ink px-7 py-10 lg:px-12 lg:py-12 text-white grid lg:grid-cols-[1.3fr_.7fr] gap-8 items-center overflow-hidden relative">
                <div class="absolute right-0 top-0 w-72 h-72 rounded-full bg-brand-500/20 blur-3xl"></div>
                <div class="relative">
                    <p class="text-sm font-bold text-blue-300 tracking-wide">READY TO GET STARTED?</p>
                    <h2 class="mt-3 text-3xl font-extrabold">Use every feature free for 30 days.</h2>
                    <p class="mt-4 text-slate-300 leading-7">Your data remains on your computer and synchronizes securely with your configured backend. When the trial ends, enter your license key and continue where you stopped.</p>
                </div>
                <div class="relative lg:text-right">
                    <a href="https://wa.me/<?= $whatsAppNumber ?>?text=<?= urlencode('Hello, I want to start and activate BizFlow POS.') ?>"
                       target="_blank"
                       class="inline-flex rounded-xl bg-white px-6 py-4 font-extrabold text-ink hover:bg-blue-50 transition">
                        Start with BizFlow POS
                    </a>
                </div>
            </div>

            <div class="mt-20 max-w-3xl mx-auto">
                <h2 class="text-center text-3xl font-extrabold">Frequently asked questions</h2>
                <div class="mt-8 divide-y divide-slate-200 border-y border-slate-200">
                    <details class="group py-5">
                        <summary class="cursor-pointer list-none flex justify-between font-bold">What happens after the free month?<span class="text-brand-600">+</span></summary>
                        <p class="mt-3 text-sm leading-6 text-muted">BizFlow POS asks for activation before opening. Your database is not deleted; activation restores access immediately.</p>
                    </details>
                    <details class="group py-5">
                        <summary class="cursor-pointer list-none flex justify-between font-bold">Does the POS work without internet?<span class="text-brand-600">+</span></summary>
                        <p class="mt-3 text-sm leading-6 text-muted">Yes. A previously validated paid license has a seven-day offline grace period. Connect periodically for license validation and cloud synchronization.</p>
                    </details>
                    <details class="group py-5">
                        <summary class="cursor-pointer list-none flex justify-between font-bold">Can I add more computers later?<span class="text-brand-600">+</span></summary>
                        <p class="mt-3 text-sm leading-6 text-muted">Yes. Upgrade to Business or Enterprise and activate the same license on additional workstations up to the package limit.</p>
                    </details>
                </div>
            </div>
        </section>
    </main>

    <footer class="border-t border-slate-200 bg-white">
        <div class="max-w-7xl mx-auto px-5 lg:px-8 py-8 flex flex-col sm:flex-row gap-4 justify-between text-sm text-muted">
            <p>© 2026 BizFlow POS. Professional retail software.</p>
            <p>Prices shown in Kenya shillings (KES).</p>
        </div>
    </footer>
</body>
</html>
