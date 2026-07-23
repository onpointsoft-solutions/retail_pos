<?php
declare(strict_types=1);

class Validator
{
    /**
     * @throws InvalidArgumentException if any required field is missing or empty
     */
    public static function required(array $data, array $fields): void
    {
        $missing = [];
        foreach ($fields as $field) {
            if (!isset($data[$field]) || $data[$field] === '' || $data[$field] === null) {
                $missing[] = $field;
            }
        }
        if (!empty($missing)) {
            throw new InvalidArgumentException('Missing required fields: ' . implode(', ', $missing));
        }
    }

    /**
     * @throws InvalidArgumentException if value exceeds max length
     */
    public static function maxLength(string $value, int $max, string $field): void
    {
        if (mb_strlen($value) > $max) {
            throw new InvalidArgumentException("Field '{$field}' exceeds maximum length of {$max} characters");
        }
    }

    /**
     * @throws InvalidArgumentException if value is not numeric
     */
    public static function numeric(mixed $value, string $field): void
    {
        if (!is_numeric($value)) {
            throw new InvalidArgumentException("Field '{$field}' must be numeric");
        }
    }

    /**
     * @throws InvalidArgumentException if value is not a positive integer
     */
    public static function positiveInt(mixed $value, string $field): void
    {
        if (!filter_var($value, FILTER_VALIDATE_INT) || (int)$value < 1) {
            throw new InvalidArgumentException("Field '{$field}' must be a positive integer");
        }
    }

    /**
     * @throws InvalidArgumentException if value is negative
     */
    public static function nonNegative(mixed $value, string $field): void
    {
        if (!is_numeric($value) || (float)$value < 0) {
            throw new InvalidArgumentException("Field '{$field}' must be non-negative");
        }
    }

    /**
     * @throws InvalidArgumentException if value is not a valid ISO 8601 datetime
     */
    public static function isoDatetime(string $value, string $field): void
    {
        $dt = DateTime::createFromFormat(DateTime::ATOM, $value)
            ?: DateTime::createFromFormat('Y-m-d\TH:i:s\Z', $value)
            ?: DateTime::createFromFormat('Y-m-d H:i:s', $value);
        if ($dt === false) {
            throw new InvalidArgumentException("Field '{$field}' must be a valid ISO 8601 datetime");
        }
    }

    /**
     * @throws InvalidArgumentException if email is invalid
     */
    public static function email(string $value, string $field): void
    {
        if (!filter_var($value, FILTER_VALIDATE_EMAIL)) {
            throw new InvalidArgumentException("Field '{$field}' must be a valid email address");
        }
    }
}
