<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');

require_once __DIR__ . '/../config/config.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Auth.php';

// Autoload models and controllers
spl_autoload_register(function ($class) {
    $paths = [
        __DIR__ . '/../models/' . $class . '.php',
        __DIR__ . '/../controllers/' . $class . '.php',
    ];
    foreach ($paths as $path) {
        if (file_exists($path)) {
            require_once $path;
            return;
        }
    }
});

// CORS headers
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Parse request
$method = $_SERVER['REQUEST_METHOD'];
$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$uri = str_replace('/php-pos/api', '', $uri);
$uri = rtrim($uri, '/') ?: '/';

// Parse body
$body = [];
if ($method !== 'GET' && $method !== 'DELETE') {
    $rawBody = file_get_contents('php://input');
    if (!empty($rawBody)) {
        $body = json_decode($rawBody, true) ?? [];
    }
}

// Routes
$routes = [
    // Auth
    'POST /auth/login' => ['AuthController', 'login'],
    'POST /auth/logout' => ['AuthController', 'logout'],
    'GET /auth/me' => ['AuthController', 'me'],
    
    // Products
    'GET /products' => ['ProductController', 'index'],
    'POST /products' => ['ProductController', 'store'],
    'GET /products/{id}' => ['ProductController', 'show'],
    'PUT /products/{id}' => ['ProductController', 'update'],
    'DELETE /products/{id}' => ['ProductController', 'destroy'],
    
    // Sales
    'GET /sales' => ['SaleController', 'index'],
    'POST /sales' => ['SaleController', 'store'],
    'GET /sales/{id}' => ['SaleController', 'show'],
    
    // Customers
    'GET /customers' => ['CustomerController', 'index'],
    'POST /customers' => ['CustomerController', 'store'],
    'GET /customers/{id}' => ['CustomerController', 'show'],
    'PUT /customers/{id}' => ['CustomerController', 'update'],
    'DELETE /customers/{id}' => ['CustomerController', 'destroy'],
    
    // Categories
    'GET /categories' => ['CategoryController', 'index'],
    'POST /categories' => ['CategoryController', 'store'],
    'PUT /categories/{id}' => ['CategoryController', 'update'],
    'DELETE /categories/{id}' => ['CategoryController', 'destroy'],
    
    // Suppliers
    'GET /suppliers' => ['SupplierController', 'index'],
    'POST /suppliers' => ['SupplierController', 'store'],
    'PUT /suppliers/{id}' => ['SupplierController', 'update'],
    'DELETE /suppliers/{id}' => ['SupplierController', 'destroy'],
    
    // Settings
    'GET /settings' => ['SettingsController', 'index'],
    'PUT /settings' => ['SettingsController', 'update'],
];

// Match route
$routeKey = "$method $uri";
$params = [];

if (!isset($routes[$routeKey])) {
    // Try with parameter matching
    foreach ($routes as $pattern => $handler) {
        $patternParts = explode(' ', $pattern);
        $patternMethod = $patternParts[0];
        $patternPath = $patternParts[1];
        
        if ($patternMethod !== $method) continue;
        
        $regex = preg_replace('#\{([a-z_]+)\}#', '([^/]+)', $patternPath);
        $regex = '#^' . $regex . '$#';
        
        if (preg_match($regex, $uri, $matches)) {
            array_shift($matches);
            $paramNames = [];
            preg_match_all('#\{([a-z_]+)\}#', $patternPath, $paramNames);
            $params = array_combine($paramNames[1], $matches);
            $routeKey = $pattern;
            break;
        }
    }
}

if (!isset($routes[$routeKey])) {
    http_response_code(404);
    echo json_encode(['error' => 'Route not found']);
    exit;
}

// Authenticate (except login)
$handler = $routes[$routeKey];
if ($routeKey !== 'POST /auth/login') {
    $headers = getallheaders();
    $token = $headers['Authorization'] ?? '';
    if (str_starts_with($token, 'Bearer ')) {
        $token = substr($token, 7);
    }
    
    $payload = Auth::verifyToken($token);
    if (!$payload) {
        http_response_code(401);
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
    
    // Load user and set as current
    $userData = Database::fetchOne('SELECT * FROM users WHERE id = ?', [$payload['user_id']]);
    if ($userData) {
        Auth::setCurrentUser(new User($userData));
    }
}

// Call controller
[$controllerClass, $method] = $handler;
$controller = new $controllerClass();

try {
    $response = $controller->$method($params, $body);
    echo json_encode($response);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
