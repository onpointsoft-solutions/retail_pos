<?php
declare(strict_types=1);

session_start();

// Check if setup is complete
require_once __DIR__ . '/config/database.php';

try {
    $setupComplete = Database::fetchOne("SELECT value FROM app_settings WHERE `key` = 'setup_complete'");
    if (!$setupComplete || $setupComplete['value'] !== 'true') {
        header('Location: setup.php');
        exit;
    }
} catch (Exception $e) {
    header('Location: setup.php');
    exit;
}

// Check if user is logged in
if (!isset($_SESSION['user_id'])) {
    header('Location: login.php');
    exit;
}

// Load user data
$userData = Database::fetchOne('SELECT * FROM users WHERE id = ?', [$_SESSION['user_id']]);
$settings = Database::fetchAll('SELECT * FROM app_settings');
$settingsArray = [];
foreach ($settings as $s) {
    $settingsArray[$s['key']] = $s['value'];
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($settingsArray['store_name'] ?? 'Retail POS'); ?></title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>
    <div id="app">
        <nav class="sidebar">
            <div class="brand">
                <h2><?php echo htmlspecialchars($settingsArray['store_name'] ?? 'Retail POS'); ?></h2>
            </div>
            <ul class="nav-links">
                <li><a href="#" class="active" data-page="sales">Sales</a></li>
                <li><a href="#" data-page="products">Products</a></li>
                <li><a href="#" data-page="customers">Customers</a></li>
                <?php if ($userData['role'] === 'ADMIN' || $userData['role'] === 'MANAGER'): ?>
                    <li><a href="#" data-page="suppliers">Suppliers</a></li>
                    <li><a href="#" data-page="inventory">Inventory</a></li>
                    <li><a href="#" data-page="reports">Reports</a></li>
                    <li><a href="#" data-page="settings">Settings</a></li>
                <?php endif; ?>
            </ul>
            <div class="user-info">
                <span><?php echo htmlspecialchars($userData['full_name'] ?? $userData['username']); ?></span>
                <a href="logout.php" class="logout">Logout</a>
            </div>
        </nav>
        
        <main class="content">
            <div id="sales-page" class="page active">
                <div class="page-header">
                    <h1>Sales</h1>
                </div>
                <div class="sales-layout">
                    <div class="products-section">
                        <div class="search-bar">
                            <input type="text" id="product-search" placeholder="Search products or scan barcode...">
                        </div>
                        <div class="products-grid" id="products-grid"></div>
                    </div>
                    <div class="cart-section">
                        <div class="cart-header">
                            <h2>Cart</h2>
                        </div>
                        <div class="cart-items" id="cart-items"></div>
                        <div class="cart-totals">
                            <div class="total-row">
                                <span>Subtotal:</span>
                                <span id="subtotal">KES 0.00</span>
                            </div>
                            <div class="total-row">
                                <span>Tax (<?php echo $settingsArray['tax_rate'] ?? 16; ?>%):</span>
                                <span id="tax">KES 0.00</span>
                            </div>
                            <div class="total-row grand-total">
                                <span>Total:</span>
                                <span id="total">KES 0.00</span>
                            </div>
                        </div>
                        <div class="cart-actions">
                            <button id="pay-btn" class="btn btn-success">Pay</button>
                            <button id="clear-cart-btn" class="btn btn-danger">Clear</button>
                        </div>
                    </div>
                </div>
            </div>
            
            <div id="products-page" class="page">
                <div class="page-header">
                    <h1>Products</h1>
                    <button class="btn btn-primary" id="add-product-btn">Add Product</button>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>SKU</th>
                                <th>Price</th>
                                <th>Stock</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="products-table-body"></tbody>
                    </table>
                </div>
            </div>
            
            <div id="customers-page" class="page">
                <div class="page-header">
                    <h1>Customers</h1>
                    <button class="btn btn-primary" id="add-customer-btn">Add Customer</button>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Phone</th>
                                <th>Email</th>
                                <th>Loyalty Points</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="customers-table-body"></tbody>
                    </table>
                </div>
            </div>
            
            <div id="suppliers-page" class="page">
                <div class="page-header">
                    <h1>Suppliers</h1>
                    <button class="btn btn-primary" id="add-supplier-btn">Add Supplier</button>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Phone</th>
                                <th>Email</th>
                                <th>Balance</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="suppliers-table-body"></tbody>
                    </table>
                </div>
            </div>
            
            <div id="inventory-page" class="page">
                <div class="page-header">
                    <h1>Inventory</h1>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Product</th>
                                <th>Current Stock</th>
                                <th>Minimum Stock</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="inventory-table-body"></tbody>
                    </table>
                </div>
            </div>
            
            <div id="reports-page" class="page">
                <div class="page-header">
                    <h1>Reports</h1>
                </div>
                <div class="reports-grid">
                    <div class="report-card">
                        <h3>Today's Sales</h3>
                        <p class="report-value" id="today-sales">KES 0.00</p>
                    </div>
                    <div class="report-card">
                        <h3>This Week</h3>
                        <p class="report-value" id="week-sales">KES 0.00</p>
                    </div>
                    <div class="report-card">
                        <h3>This Month</h3>
                        <p class="report-value" id="month-sales">KES 0.00</p>
                    </div>
                </div>
            </div>
            
            <div id="settings-page" class="page">
                <div class="page-header">
                    <h1>Settings</h1>
                </div>
                <div class="settings-form">
                    <div class="form-group">
                        <label>Store Name</label>
                        <input type="text" id="setting-store-name" value="<?php echo htmlspecialchars($settingsArray['store_name'] ?? ''); ?>">
                    </div>
                    <div class="form-group">
                        <label>Store Address</label>
                        <input type="text" id="setting-store-address" value="<?php echo htmlspecialchars($settingsArray['store_address'] ?? ''); ?>">
                    </div>
                    <div class="form-group">
                        <label>Store Phone</label>
                        <input type="text" id="setting-store-phone" value="<?php echo htmlspecialchars($settingsArray['store_phone'] ?? ''); ?>">
                    </div>
                    <div class="form-group">
                        <label>Tax Rate (%)</label>
                        <input type="number" id="setting-tax-rate" value="<?php echo $settingsArray['tax_rate'] ?? 16; ?>">
                    </div>
                    <button class="btn btn-primary" id="save-settings-btn">Save Settings</button>
                </div>
            </div>
        </main>
    </div>
    
    <script>
        const API_URL = '<?php echo API_URL; ?>';
        const TOKEN = '<?php echo $_SESSION['token'] ?? ''; ?>';
        const TAX_RATE = <?php echo (float)($settingsArray['tax_rate'] ?? 16); ?>;
    </script>
    <script src="assets/js/app.js"></script>
</body>
</html>
