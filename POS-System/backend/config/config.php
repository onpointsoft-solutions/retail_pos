<?php
declare(strict_types=1);

define('JWT_SECRET', getenv('JWT_SECRET') ?: 'change-this-in-production-use-env-var');
define('JWT_EXPIRY', 3600 * 24); // 24 hours
define('APP_NAME', 'BizFlow POS API');
define('APP_VERSION', '2.0.0');
define('BACKUP_DIR', __DIR__ . '/../backups');
define('MAX_LOGIN_ATTEMPTS', 5);
define('LOCKOUT_MINUTES', 15);

/**
 * REQUIRE_AUTH — set to false to allow sync without authentication.
 * Useful for local/LAN deployments where security is handled by the network.
 *
 * Set to true in production when the API is publicly accessible.
 *
 * You can also control this via environment variable:
 *   SetEnv REQUIRE_AUTH false   (Apache)
 *   fastcgi_param REQUIRE_AUTH false;  (Nginx)
 */
define('REQUIRE_AUTH', filter_var(
    getenv('REQUIRE_AUTH') !== false ? getenv('REQUIRE_AUTH') : 'true',
    FILTER_VALIDATE_BOOLEAN
));
