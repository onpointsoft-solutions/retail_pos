<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/config.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/JwtHelper.php';
require_once __DIR__ . '/../helpers/Validator.php';

class AuthController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    public function login(array $body): never
    {
        try {
            Validator::required($body, ['username', 'password']);
        } catch (InvalidArgumentException $e) {
            Response::error($e->getMessage(), 422);
        }

        $username = trim($body['username']);
        $password = $body['password'];

        $stmt = $this->db->prepare(
            'SELECT id, username, password_hash, role, full_name, active, 
                    failed_login_attempts, lockout_until, store_id
             FROM users 
             WHERE username = ? AND deleted_at IS NULL
             LIMIT 1'
        );
        $stmt->execute([$username]);
        $user = $stmt->fetch();

        if (!$user) {
            Response::error('Invalid credentials', 401);
        }

        // Check account active
        if (!$user['active']) {
            Response::error('Account is disabled', 403);
        }

        // Check lockout
        if ($user['lockout_until'] !== null && strtotime($user['lockout_until']) > time()) {
            $remaining = ceil((strtotime($user['lockout_until']) - time()) / 60);
            Response::error("Account locked. Try again in {$remaining} minute(s).", 429);
        }

        // Verify password
        if (!password_verify($password, $user['password_hash'])) {
            // Increment failed attempts
            $attempts = (int)$user['failed_login_attempts'] + 1;
            if ($attempts >= MAX_LOGIN_ATTEMPTS) {
                $lockUntil = date('Y-m-d H:i:s', time() + (LOCKOUT_MINUTES * 60));
                $upd = $this->db->prepare(
                    'UPDATE users SET failed_login_attempts = ?, lockout_until = ? WHERE id = ?'
                );
                $upd->execute([$attempts, $lockUntil, $user['id']]);
                Response::error("Too many failed attempts. Account locked for " . LOCKOUT_MINUTES . " minutes.", 429);
            } else {
                $upd = $this->db->prepare(
                    'UPDATE users SET failed_login_attempts = ? WHERE id = ?'
                );
                $upd->execute([$attempts, $user['id']]);
            }
            Response::error('Invalid credentials', 401);
        }

        // Reset failed attempts
        $upd = $this->db->prepare(
            'UPDATE users SET failed_login_attempts = 0, lockout_until = NULL WHERE id = ?'
        );
        $upd->execute([$user['id']]);

        // Build JWT
        $payload = [
            'user_id'  => $user['id'],
            'username' => $user['username'],
            'role'     => $user['role'],
            'store_id' => $user['store_id'] ?? null,
        ];
        $token = JwtHelper::encode($payload);

        // Audit log
        $this->auditLog($user['id'], 'LOGIN', 'users', $user['id'], null, null);

        Response::json([
            'access_token' => $token,
            'token_type'   => 'Bearer',
            'expires_in'   => JWT_EXPIRY,
            'user'         => [
                'id'        => $user['id'],
                'username'  => $user['username'],
                'full_name' => $user['full_name'],
                'role'      => $user['role'],
            ],
        ]);
    }

    public function refresh(array $body): never
    {
        // Accept token from body or Authorization header
        $token = $body['token'] ?? null;
        if (!$token) {
            $authHeader = $_SERVER['HTTP_AUTHORIZATION']
                ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
                ?? '';
            if (str_starts_with($authHeader, 'Bearer ')) {
                $token = substr($authHeader, 7);
            }
        }

        if (!$token) {
            Response::error('Token required', 422);
        }

        try {
            $payload = JwtHelper::decode($token);
        } catch (RuntimeException $e) {
            Response::error('Invalid or expired token: ' . $e->getMessage(), 401);
        }

        // Verify user still exists and is active
        $stmt = $this->db->prepare(
            'SELECT id, username, role, full_name, active, store_id 
             FROM users WHERE id = ? AND active = 1 AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$payload['user_id']]);
        $user = $stmt->fetch();

        if (!$user) {
            Response::error('User not found or inactive', 401);
        }

        $newPayload = [
            'user_id'  => $user['id'],
            'username' => $user['username'],
            'role'     => $user['role'],
            'store_id' => $user['store_id'] ?? null,
        ];
        $newToken = JwtHelper::encode($newPayload);

        Response::json([
            'access_token' => $newToken,
            'token_type'   => 'Bearer',
            'expires_in'   => JWT_EXPIRY,
        ]);
    }

    public function logout(array $payload): never
    {
        $this->auditLog($payload['user_id'], 'LOGOUT', 'users', $payload['user_id'], null, null);
        Response::json(['message' => 'Logged out successfully']);
    }

    private function auditLog(
        string $userId,
        string $action,
        string $entityType,
        string $entityId,
        ?array $oldValues,
        ?array $newValues
    ): void {
        try {
            $stmt = $this->db->prepare(
                'INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, old_values, new_values, ip_address, created_at)
                 VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, NOW())'
            );
            $stmt->execute([
                $userId,
                $action,
                $entityType,
                $entityId,
                $oldValues ? json_encode($oldValues) : null,
                $newValues ? json_encode($newValues) : null,
                $_SERVER['REMOTE_ADDR'] ?? null,
            ]);
        } catch (PDOException) {
            // Audit log failures should not break the main flow
        }
    }
}
