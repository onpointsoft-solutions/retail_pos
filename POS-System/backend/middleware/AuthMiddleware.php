<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/config.php';
require_once __DIR__ . '/../helpers/JwtHelper.php';
require_once __DIR__ . '/../helpers/Response.php';

class AuthMiddleware
{
    /**
     * Guest payload returned when REQUIRE_AUTH = false.
     * Gives sync operations a valid payload shape without needing a real token.
     */
    private static array $GUEST_PAYLOAD = [
        'user_id'  => 'system',
        'username' => 'system',
        'role'     => 'ADMIN',
        'store_id' => null,
    ];

    /**
     * Verify JWT and return payload.
     *
     * When REQUIRE_AUTH is false (see config/config.php), any request is allowed
     * through with a synthetic ADMIN payload — no token required.
     *
     * When REQUIRE_AUTH is true, a valid Bearer token must be present.
     */
    public static function handle(): array
    {
        // ── Auth bypass mode ──────────────────────────────────────────────────
        if (!REQUIRE_AUTH) {
            return self::$GUEST_PAYLOAD;
        }

        // ── Token extraction ──────────────────────────────────────────────────
        $authHeader = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? '';

        if (empty($authHeader) && function_exists('apache_request_headers')) {
            $headers    = apache_request_headers();
            $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
        }

        if (empty($authHeader) || !str_starts_with($authHeader, 'Bearer ')) {
            Response::error('Unauthorized: missing or invalid Authorization header', 401);
        }

        $token = substr($authHeader, 7);

        try {
            return JwtHelper::decode($token);
        } catch (RuntimeException $e) {
            Response::error('Unauthorized: ' . $e->getMessage(), 401);
        }
    }

    /**
     * Require a specific role.
     * Skipped when REQUIRE_AUTH is false.
     */
    public static function requireRole(array $payload, string $role): void
    {
        if (!REQUIRE_AUTH) return;
        if (strtoupper($payload['role'] ?? '') !== strtoupper($role)) {
            Response::error("Forbidden: requires role {$role}", 403);
        }
    }

    /**
     * Require one of several roles.
     * Skipped when REQUIRE_AUTH is false.
     */
    public static function requireAnyRole(array $payload, array $roles): void
    {
        if (!REQUIRE_AUTH) return;
        $userRole = strtoupper($payload['role'] ?? '');
        if (!in_array($userRole, array_map('strtoupper', $roles), true)) {
            Response::error('Forbidden: insufficient permissions', 403);
        }
    }
}
