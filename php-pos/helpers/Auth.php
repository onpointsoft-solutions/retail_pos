<?php
declare(strict_types=1);

class Auth {
    private static ?User $currentUser = null;

    public static function hashPassword(string $password): string {
        return password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
    }

    public static function verifyPassword(string $password, string $hash): bool {
        return password_verify($password, $hash);
    }

    public static function generateToken(array $payload): string {
        $header = json_encode(['typ' => 'JWT', 'alg' => JWT_ALGORITHM]);
        $payload['iat'] = time();
        $payload['exp'] = time() + JWT_EXPIRY;
        
        $base64UrlHeader = self::base64UrlEncode($header);
        $base64UrlPayload = self::base64UrlEncode(json_encode($payload));
        
        $signature = hash_hmac('sha256', $base64UrlHeader . "." . $base64UrlPayload, JWT_SECRET, true);
        $base64UrlSignature = self::base64UrlEncode($signature);
        
        return $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;
    }

    public static function verifyToken(string $token): ?array {
        $tokenParts = explode('.', $token);
        if (count($tokenParts) !== 3) {
            return null;
        }

        list($header, $payload, $signature) = $tokenParts;

        $expectedSignature = hash_hmac('sha256', $header . "." . $payload, JWT_SECRET, true);
        if (!hash_equals(self::base64UrlDecode($signature), $expectedSignature)) {
            return null;
        }

        $decodedPayload = json_decode(self::base64UrlDecode($payload), true);
        
        if (isset($decodedPayload['exp']) && $decodedPayload['exp'] < time()) {
            return null;
        }

        return $decodedPayload;
    }

    private static function base64UrlEncode(string $data): string {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $data): string {
        return base64_decode(strtr($data, '-_', '+/'));
    }

    public static function setCurrentUser(User $user): void {
        self::$currentUser = $user;
    }

    public static function getCurrentUser(): ?User {
        return self::$currentUser;
    }

    public static function check(): bool {
        return self::$currentUser !== null;
    }

    public static function id(): ?string {
        return self::$currentUser?->getId();
    }

    public static function user(): ?User {
        return self::$currentUser;
    }
}
