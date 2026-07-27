<?php
declare(strict_types=1);

// Application settings
define('APP_NAME', 'Retail POS');
define('APP_VERSION', '1.0.0');
define('APP_URL', 'http://localhost/php-pos');
define('API_URL', APP_URL . '/api');

// JWT Settings
define('JWT_SECRET', 'your-secret-key-change-this-in-production');
define('JWT_ALGORITHM', 'HS256');
define('JWT_EXPIRY', 86400); // 24 hours in seconds

// File Upload Settings
define('UPLOAD_DIR', __DIR__ . '/../uploads');
define('MAX_FILE_SIZE', 5 * 1024 * 1024); // 5MB
define('ALLOWED_IMAGE_TYPES', ['image/jpeg', 'image/png', 'image/gif', 'image/webp']);

// Pagination
define('DEFAULT_PAGE_SIZE', 50);

// Timezone
date_default_timezone_set('Africa/Nairobi');

// Error reporting (disable in production)
if (getenv('APP_ENV') === 'production') {
    error_reporting(0);
    ini_set('display_errors', '0');
} else {
    error_reporting(E_ALL);
    ini_set('display_errors', '1');
}
