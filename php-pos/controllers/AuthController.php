<?php
declare(strict_types=1);

class AuthController {
    public function login(array $params, array $body): array {
        $username = $body['username'] ?? '';
        $password = $body['password'] ?? '';
        
        if (empty($username) || empty($password)) {
            http_response_code(400);
            return ['error' => 'Username and password are required'];
        }
        
        $userData = Database::fetchOne(
            'SELECT * FROM users WHERE username = ? AND deleted_at IS NULL',
            [$username]
        );
        
        if (!$userData) {
            http_response_code(401);
            return ['error' => 'Invalid credentials'];
        }
        
        $user = new User($userData);
        
        if (!$user->isActive()) {
            http_response_code(403);
            return ['error' => 'Account is disabled'];
        }
        
        if ($user->isLocked()) {
            http_response_code(423);
            return ['error' => 'Account is locked. Please try again later.'];
        }
        
        if (!Auth::verifyPassword($password, $user->getPasswordHash())) {
            $user->setFailedLoginAttempts($user->getFailedLoginAttempts() + 1);
            
            if ($user->getFailedLoginAttempts() >= 5) {
                $lockoutTime = new DateTime('+15 minutes');
                $user->setLockoutUntil($lockoutTime);
            }
            
            Database::execute(
                'UPDATE users SET failed_login_attempts = ?, lockout_until = ? WHERE id = ?',
                [$user->getFailedLoginAttempts(), $user->getLockoutUntil()?->format('Y-m-d H:i:s'), $user->getId()]
            );
            
            http_response_code(401);
            return ['error' => 'Invalid credentials'];
        }
        
        // Reset failed attempts on successful login
        Database::execute(
            'UPDATE users SET failed_login_attempts = 0, lockout_until = NULL WHERE id = ?',
            [$user->getId()]
        );
        
        $token = Auth::generateToken(['user_id' => $user->getId(), 'role' => $user->getRole()]);
        
        return [
            'token' => $token,
            'user' => $user->toPublicArray(),
        ];
    }

    public function logout(array $params, array $body): array {
        // In a real implementation, you might want to blacklist the token
        return ['message' => 'Logged out successfully'];
    }

    public function me(array $params, array $body): array {
        $user = Auth::user();
        if (!$user) {
            http_response_code(401);
            return ['error' => 'Unauthorized'];
        }
        return $user->toPublicArray();
    }
}
