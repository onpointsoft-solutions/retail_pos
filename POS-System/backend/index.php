<?php
declare(strict_types=1);

// ── Force JSON output even for PHP errors ─────────────────────────────────────
ini_set('display_errors', '0');
ini_set('log_errors', '1');
header('Content-Type: application/json; charset=utf-8');

// Enable gzip compression for responses
if (isset($_SERVER['HTTP_ACCEPT_ENCODING']) && strpos($_SERVER['HTTP_ACCEPT_ENCODING'], 'gzip') !== false) {
    ob_start('ob_gzhandler');
}

// Catch fatal errors and return them as JSON
register_shutdown_function(function () {
    $err = error_get_last();
    if ($err && in_array($err['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR])) {
        http_response_code(500);
        echo json_encode(['error' => 'Server error: ' . $err['message']]);
    }
});

set_exception_handler(function (Throwable $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
    exit;
});

// ── Bootstrap ─────────────────────────────────────────────────────────────────
require_once __DIR__ . '/config/config.php';
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/helpers/Response.php';
require_once __DIR__ . '/helpers/JwtHelper.php';
require_once __DIR__ . '/helpers/Validator.php';
require_once __DIR__ . '/middleware/CorsMiddleware.php';
require_once __DIR__ . '/middleware/AuthMiddleware.php';

// ── CORS ─────────────────────────────────────────────────────────────────────
CorsMiddleware::handle();

// ── Request basics ────────────────────────────────────────────────────────────
$method = strtoupper($_SERVER['REQUEST_METHOD']);
$uri    = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

// Strip /api prefix (and any sub-directory prefix, e.g. /pos/api)
$uri = preg_replace('#^(?:/[^/]+)?/api#', '', $uri);
$uri = rtrim($uri ?: '/', '/') ?: '/';

// Body (JSON only; multipart handled by $_FILES)
$body = [];
$contentType = $_SERVER['CONTENT_TYPE'] ?? '';
if (!str_contains($contentType, 'multipart/form-data')) {
    $rawBody = file_get_contents('php://input');
    if (!empty($rawBody)) {
        $body = json_decode($rawBody, true) ?? [];
    }
}

// ── Route table ──────────────────────────────────────────────────────────────
// Each entry: [METHOD, regex_pattern, controller_class, method, public?]
// Named groups in regex become $params keys.
$routes = [
    // ── Health ──────────────────────────────────────────────────────────────
    ['GET',    '#^/health$#',                          null,                      '_health',             true],

    // ── Auth ────────────────────────────────────────────────────────────────
    ['POST',   '#^/auth/login$#',                      'AuthController',          'login',               true],
    ['POST',   '#^/auth/refresh$#',                    'AuthController',          'refresh',             true],
    ['POST',   '#^/auth/logout$#',                     'AuthController',          'logout',              false],

    // ── Products ─────────────────────────────────────────────────────────────
    ['GET',    '#^/products$#',                        'ProductController',       'index',               false],
    ['POST',   '#^/products$#',                        'ProductController',       'store',               false],
    ['GET',    '#^/products/(?P<id>[^/]+)$#',          'ProductController',       'show',                false],
    ['PUT',    '#^/products/(?P<id>[^/]+)$#',          'ProductController',       'update',              false],
    ['PATCH',  '#^/products/(?P<id>[^/]+)$#',          'ProductController',       'update',              false],
    ['DELETE', '#^/products/(?P<id>[^/]+)$#',          'ProductController',       'destroy',             false],

    // ── Sales ────────────────────────────────────────────────────────────────
    ['GET',    '#^/sales$#',                           'SaleController',          'index',               false],
    ['POST',   '#^/sales$#',                           'SaleController',          'store',               false],
    ['GET',    '#^/sales/(?P<id>[^/]+)$#',             'SaleController',          'show',                false],

    // ── Customers ────────────────────────────────────────────────────────────
    ['GET',    '#^/customers$#',                       'CustomerController',      'index',               false],
    ['POST',   '#^/customers$#',                       'CustomerController',      'store',               false],
    ['GET',    '#^/customers/(?P<id>[^/]+)$#',         'CustomerController',      'show',                false],
    ['PUT',    '#^/customers/(?P<id>[^/]+)$#',         'CustomerController',      'update',              false],
    ['PATCH',  '#^/customers/(?P<id>[^/]+)$#',         'CustomerController',      'update',              false],
    ['DELETE', '#^/customers/(?P<id>[^/]+)$#',         'CustomerController',      'destroy',             false],

    // ── Suppliers ────────────────────────────────────────────────────────────
    ['GET',    '#^/suppliers$#',                       'SupplierController',      'index',               false],
    ['POST',   '#^/suppliers$#',                       'SupplierController',      'store',               false],
    ['GET',    '#^/suppliers/(?P<id>[^/]+)$#',         'SupplierController',      'show',                false],
    ['PUT',    '#^/suppliers/(?P<id>[^/]+)$#',         'SupplierController',      'update',              false],
    ['PATCH',  '#^/suppliers/(?P<id>[^/]+)$#',         'SupplierController',      'update',              false],
    ['DELETE', '#^/suppliers/(?P<id>[^/]+)$#',         'SupplierController',      'destroy',             false],

    // ── Purchase Orders ──────────────────────────────────────────────────────
    ['GET',    '#^/purchase-orders$#',                 'PurchaseOrderController', 'index',               false],
    ['POST',   '#^/purchase-orders$#',                 'PurchaseOrderController', 'store',               false],
    ['GET',    '#^/purchase-orders/(?P<id>[^/]+)$#',   'PurchaseOrderController', 'show',                false],
    ['PUT',    '#^/purchase-orders/(?P<id>[^/]+)$#',   'PurchaseOrderController', 'update',              false],
    ['PATCH',  '#^/purchase-orders/(?P<id>[^/]+)$#',   'PurchaseOrderController', 'update',              false],
    ['DELETE', '#^/purchase-orders/(?P<id>[^/]+)$#',   'PurchaseOrderController', 'destroy',             false],

    // ── Inventory ────────────────────────────────────────────────────────────
    ['GET',    '#^/inventory/movements$#',             'InventoryController',     'movements',           false],
    ['POST',   '#^/inventory/movements$#',             'InventoryController',     'store',               false],

    // ── Users ────────────────────────────────────────────────────────────────
    ['GET',    '#^/users$#',                           'UserController',          'index',               false],
    ['POST',   '#^/users$#',                           'UserController',          'store',               false],
    ['GET',    '#^/users/(?P<id>[^/]+)$#',             'UserController',          'show',                false],
    ['PUT',    '#^/users/(?P<id>[^/]+)$#',             'UserController',          'update',              false],
    ['PATCH',  '#^/users/(?P<id>[^/]+)$#',             'UserController',          'update',              false],
    ['DELETE', '#^/users/(?P<id>[^/]+)$#',             'UserController',          'destroy',             false],

    // Licensing
    ['GET',    '#^/license/plans$#',                   'LicenseController',       'plans',               true],
    ['POST',   '#^/license/trial$#',                   'LicenseController',       'trial',               true],
    ['POST',   '#^/license/activate$#',                'LicenseController',       'activate',            true],
    ['POST',   '#^/license/validate$#',                'LicenseController',       'validateLicense',     true],
    ['POST',   '#^/license/issue$#',                   'LicenseController',       'issue',               false],
    ['POST',   '#^/license/renew$#',                   'LicenseController',       'renew',               false],
    ['POST',   '#^/license/revoke$#',                  'LicenseController',       'revoke',              false],

    // ── Sync ─────────────────────────────────────────────────────────────────
    ['POST',   '#^/sync/upload$#',                     'SyncController',          'upload',              false],
    ['GET',    '#^/sync/download/(?P<entity>[^/]+)$#', 'SyncController',          'download',            false],
    ['GET',    '#^/sync/status$#',                     'SyncController',          'status',              false],

    // ── Settings ─────────────────────────────────────────────────────────────
    ['GET',    '#^/settings$#',                        'SettingsController',      'show',                false],
    ['PUT',    '#^/settings$#',                        'SettingsController',      'update',              false],
    ['PATCH',  '#^/settings$#',                        'SettingsController',      'update',              false],

    // ── Backup ───────────────────────────────────────────────────────────────
    ['POST',   '#^/backup/upload$#',                   'BackupController',        'upload',              false],
    ['GET',    '#^/backup/list$#',                     'BackupController',        'listBackups',         false],

    // ── Categories ───────────────────────────────────────────────────────────
    ['GET',    '#^/categories$#',                              'CategoryController',  'index',             false],
    ['POST',   '#^/categories$#',                              'CategoryController',  'store',             false],
    ['GET',    '#^/categories/(?P<id>[^/]+)$#',                'CategoryController',  'show',              false],
    ['PUT',    '#^/categories/(?P<id>[^/]+)$#',                'CategoryController',  'update',            false],
    ['PATCH',  '#^/categories/(?P<id>[^/]+)$#',                'CategoryController',  'update',            false],
    ['DELETE', '#^/categories/(?P<id>[^/]+)$#',                'CategoryController',  'destroy',           false],

    // ── Expenses ─────────────────────────────────────────────────────────────
    ['GET',    '#^/expenses$#',                                'ExpenseController',   'index',             false],
    ['POST',   '#^/expenses$#',                                'ExpenseController',   'store',             false],
    ['GET',    '#^/expenses/summary$#',                        'ExpenseController',   'summary',           false],
    ['GET',    '#^/expenses/(?P<id>[^/]+)$#',                  'ExpenseController',   'show',              false],
    ['PUT',    '#^/expenses/(?P<id>[^/]+)$#',                  'ExpenseController',   'update',            false],
    ['PATCH',  '#^/expenses/(?P<id>[^/]+)$#',                  'ExpenseController',   'update',            false],
    ['DELETE', '#^/expenses/(?P<id>[^/]+)$#',                  'ExpenseController',   'destroy',           false],

    // ── Job Cards (Services) ──────────────────────────────────────────────────
    ['GET',    '#^/job-cards$#',                               'JobCardController',   'index',             false],
    ['POST',   '#^/job-cards$#',                               'JobCardController',   'store',             false],
    ['GET',    '#^/job-cards/(?P<id>[^/]+)$#',                 'JobCardController',   'show',              false],
    ['PUT',    '#^/job-cards/(?P<id>[^/]+)$#',                 'JobCardController',   'update',            false],
    ['PATCH',  '#^/job-cards/(?P<id>[^/]+)$#',                 'JobCardController',   'update',            false],
    ['DELETE', '#^/job-cards/(?P<id>[^/]+)$#',                 'JobCardController',   'destroy',           false],

    // ── Reports ───────────────────────────────────────────────────────────────
    ['GET',    '#^/reports/sales$#',                           'ReportController',    'sales',             false],
    ['GET',    '#^/reports/profit-loss$#',                     'ReportController',    'profitLoss',        false],
    ['GET',    '#^/reports/best-sellers$#',                    'ReportController',    'bestSellers',       false],
    ['GET',    '#^/reports/cashier-performance$#',             'ReportController',    'cashierPerformance',false],
    ['GET',    '#^/reports/sales-by-category$#',               'ReportController',    'salesByCategory',   false],
    ['GET',    '#^/reports/tax$#',                             'ReportController',    'tax',               false],
    ['GET',    '#^/reports/low-stock$#',                       'ReportController',    'lowStock',          false],
    ['GET',    '#^/reports/inventory-valuation$#',             'ReportController',    'inventoryValuation',false],
    ['GET',    '#^/reports/daily-trend$#',                     'ReportController',    'dailyTrend',        false],

    // ── Analytics ─────────────────────────────────────────────────────────────
    ['GET',    '#^/analytics/income-vs-expenses$#',            'AnalyticsController', 'incomeVsExpenses',  false],
    ['GET',    '#^/analytics/market-basket$#',                 'AnalyticsController', 'marketBasket',      false],
    ['GET',    '#^/analytics/revenue-breakdown$#',             'AnalyticsController', 'revenueBreakdown',  false],
];

// ── Autoloader ───────────────────────────────────────────────────────────────
$controllerDirs = [
    __DIR__ . '/controllers/',
];

function loadController(string $class): void
{
    global $controllerDirs;
    foreach ($controllerDirs as $dir) {
        $file = $dir . $class . '.php';
        if (file_exists($file)) {
            require_once $file;
            return;
        }
    }
}

// ── Dispatch ─────────────────────────────────────────────────────────────────
$matched = false;

foreach ($routes as [$routeMethod, $pattern, $controllerClass, $action, $isPublic]) {
    if ($routeMethod !== $method) continue;
    if (!preg_match($pattern, $uri, $matches)) continue;

    $matched = true;

    // Extract named capture groups as params
    $params = array_filter($matches, 'is_string', ARRAY_FILTER_USE_KEY);

    // ── Health route ────────────────────────────────────────────────────────
    if ($action === '_health') {
        Response::json([
            'status'    => 'ok',
            'app'       => APP_NAME,
            'version'   => APP_VERSION,
            'timestamp' => date('c'),
        ]);
    }

    // ── Auth for protected routes ────────────────────────────────────────────
    $payload = [];
    if (!$isPublic) {
        $payload = AuthMiddleware::handle();
        $tenantBound = trim((string)($payload['business_id'] ?? '')) !== '';
        $licenseAdminRoute = $controllerClass === 'LicenseController'
            && strtoupper((string)($payload['role'] ?? '')) === 'ADMIN';
        if ($tenantBound && $controllerClass !== 'SyncController' && !$licenseAdminRoute) {
            Response::error(
                'Licensed business tokens may only access tenant-safe synchronization routes.',
                403
            );
        }
    }

    // ── Load and dispatch controller ─────────────────────────────────────────
    loadController($controllerClass);

    if (!class_exists($controllerClass)) {
        Response::error("Controller {$controllerClass} not found", 500);
    }

    $controller = new $controllerClass();

    if (!method_exists($controller, $action)) {
        Response::error("Action {$action} not found on {$controllerClass}", 500);
    }

    // Dispatch with appropriate signature based on HTTP method
    try {
        switch ($method) {
            case 'GET':
            case 'DELETE':
                // GET/DELETE: controller($payload, $params)
                // Special cases for no-param endpoints
                if (in_array($action, ['status', 'logout', 'listBackups'], true)) {
                    $controller->$action($payload);
                } else {
                    $controller->$action($payload, $params);
                }
                break;

            case 'POST':
                // POST: controller($payload, $body) or controller($payload)
                if (in_array($action, ['upload', 'listBackups'], true) && $controllerClass === 'BackupController') {
                    $controller->$action($payload);
                } elseif ($action === 'logout') {
                    $controller->$action($payload);
                } elseif ($action === 'status') {
                    $controller->$action($payload);
                } elseif ($isPublic) {
                    // Public POST: login, refresh pass body only
                    $controller->$action($body);
                } else {
                    $controller->$action($payload, $body);
                }
                break;

            case 'PUT':
            case 'PATCH':
                // PUT/PATCH: controller($payload, $params, $body)
                // Settings update has no params
                if (in_array($action, ['update'], true) && empty($params)) {
                    $controller->$action($payload, $body);
                } else {
                    $controller->$action($payload, $params, $body);
                }
                break;

            default:
                Response::error('Method not allowed', 405);
        }
    } catch (InvalidArgumentException $e) {
        Response::error($e->getMessage(), 422);
    } catch (RuntimeException $e) {
        Response::error($e->getMessage(), 500);
    } catch (PDOException $e) {
        // Sanitize DB error messages in production
        error_log('DB Error: ' . $e->getMessage());
        Response::error('A database error occurred', 500);
    }

    break; // Route matched — stop
}

if (!$matched) {
    Response::json(['error' => 'Not found', 'path' => $uri], 404);
}
