<?php
declare(strict_types=1);

class SettingsController {
    public function index(array $params, array $body): array {
        $settings = Database::fetchAll('SELECT * FROM app_settings');
        $result = [];
        foreach ($settings as $setting) {
            $result[$setting['key']] = $setting['value'];
        }
        return $result;
    }

    public function update(array $params, array $body): array {
        foreach ($body as $key => $value) {
            if (is_array($value)) {
                $value = json_encode($value);
            }
            
            $existing = Database::fetchOne(
                'SELECT * FROM app_settings WHERE `key` = ?',
                [$key]
            );
            
            if ($existing) {
                Database::execute(
                    'UPDATE app_settings SET `value` = ?, updated_at = ? WHERE `key` = ?',
                    [$value, date('Y-m-d H:i:s'), $key]
                );
            } else {
                Database::execute(
                    'INSERT INTO app_settings (`key`, `value`, updated_at) VALUES (?, ?, ?)',
                    [$key, $value, date('Y-m-d H:i:s')]
                );
            }
        }
        
        return ['message' => 'Settings updated successfully'];
    }
}
