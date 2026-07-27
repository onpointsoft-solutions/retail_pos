<?php
// Database configuration
define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_NAME', getenv('DB_NAME') ?: 'epmpmgem_victorious_pos');
define('DB_USER', getenv('DB_USER') ?: 'root');
define('DB_PASS', getenv('DB_PASS') ?: '');
define('DB_CHARSET', 'utf8mb4');

// Product images are stored by the POS API on its own subdomain. Override this
// in cPanel with PRODUCT_IMAGE_BASE_URL if the API hostname changes.
define(
    'PRODUCT_IMAGE_BASE_URL',
    rtrim(getenv('PRODUCT_IMAGE_BASE_URL') ?: 'https://pos.mobilemealscenter.co.ke', '/')
);

function shopProductImageUrl(?string $path): string {
    $fallback = '/assets/product-images/SampleProduct.png';
    $path = trim((string) $path);
    if ($path === '') {
        return $fallback;
    }
    if (preg_match('#^https?://#i', $path)) {
        return $path;
    }

    $normalized = ltrim(str_replace('\\', '/', $path), '/');
    if (str_starts_with($normalized, 'uploads/')) {
        return PRODUCT_IMAGE_BASE_URL . '/' . $normalized;
    }

    return '/' . $normalized;
}

function shopProductImageUrls(?string $paths): array {
    $images = array_values(array_filter(array_map(
        static fn(string $path): string => trim($path),
        explode(';', (string) $paths)
    )));

    if (!$images) {
        return [shopProductImageUrl(null)];
    }

    return array_map('shopProductImageUrl', $images);
}

// Create database connection
function getDbConnection() {
    try {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
        $options = [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ];
        
        $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        return $pdo;
    } catch (PDOException $e) {
        error_log('Shop database connection failed: ' . $e->getMessage());
        throw new RuntimeException('The shop database is temporarily unavailable.');
    }
}
