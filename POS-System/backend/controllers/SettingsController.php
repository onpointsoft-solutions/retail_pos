<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class SettingsController
{
    private PDO $db;

    public function __construct()
    {
        $this->db = Database::getConnection();
    }

    /**
     * GET /api/settings
     * Returns all settings as a key-value object.
     */
    public function show(array $payload, array $params): never
    {
        $stmt = $this->db->query('SELECT `key`, `value`, `updated_at` FROM app_settings ORDER BY `key` ASC');
        $rows = $stmt->fetchAll();

        $settings = [];
        foreach ($rows as $row) {
            $settings[$row['key']] = [
                'value'      => $row['value'],
                'updated_at' => $row['updated_at'],
            ];
        }

        Response::json($settings);
    }

    /**
     * PUT /api/settings
     * Body: { key: value, ... } — merges/upserts each key.
     * Admin only.
     */
    public function update(array $payload, array $body): never
    {
        AuthMiddleware::requireRole($payload, 'ADMIN');

        if (empty($body) || !is_array($body)) {
            Response::error('Request body must be a JSON object of key-value pairs', 422);
        }

        $stmt = $this->db->prepare(
            'INSERT INTO app_settings (`key`, `value`, `updated_at`)
             VALUES (?, ?, NOW())
             ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `updated_at` = NOW()'
        );

        $updated = 0;
        foreach ($body as $key => $value) {
            if (!is_string($key) || strlen($key) > 100) {
                continue;
            }
            $stmt->execute([
                $key,
                is_array($value) || is_object($value) ? json_encode($value) : (string)$value,
            ]);
            $updated++;
        }

        Response::json(['message' => 'Settings updated', 'updated' => $updated]);
    }
}
