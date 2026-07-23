<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/config.php';

class JwtHelper
{
    private static function base64UrlEncode(string $data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $data): string
    {
        $remainder = strlen($data) % 4;
        if ($remainder) {
            $data .= str_repeat('=', 4 - $remainder);
        }
        return base64_decode(strtr($data, '-_', '+/'));
    }

    public static function encode(array $payload): string
    {
        $header = self::base64UrlEncode(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));

        $now = time();
        $payload['iat'] = $now;
        $payload['exp'] = $now + JWT_EXPIRY;

        $payloadEncoded = self::base64UrlEncode(json_encode($payload));
        $signature = self::base64UrlEncode(
            hash_hmac('sha256', "{$header}.{$payloadEncoded}", JWT_SECRET, true)
        );

        return "{$header}.{$payloadEncoded}.{$signature}";
    }

    public static function decode(string $token): array
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            throw new RuntimeException('Invalid token structure');
        }

        [$headerB64, $payloadB64, $signatureB64] = $parts;

        // Verify signature
        $expectedSig = self::base64UrlEncode(
            hash_hmac('sha256', "{$headerB64}.{$payloadB64}", JWT_SECRET, true)
        );

        if (!hash_equals($expectedSig, $signatureB64)) {
            throw new RuntimeException('Invalid token signature');
        }

        $payload = json_decode(self::base64UrlDecode($payloadB64), true);
        if (!is_array($payload)) {
            throw new RuntimeException('Invalid token payload');
        }

        // Check expiry
        if (!isset($payload['exp']) || $payload['exp'] < time()) {
            throw new RuntimeException('Token has expired');
        }

        return $payload;
    }
}
