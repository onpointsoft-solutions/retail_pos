<?php
declare(strict_types=1);

class Database {
    private static ?PDO $instance = null;
    private static string $host = 'localhost';
    private static string $dbname = 'retail_pos';
    private static string $username = 'root';
    private static string $password = '';
    private static string $charset = 'utf8mb4';

    public static function configure(string $host, string $dbname, string $username, string $password): void {
        self::$host = $host;
        self::$dbname = $dbname;
        self::$username = $username;
        self::$password = $password;
    }

    public static function getInstance(): PDO {
        if (self::$instance === null) {
            $dsn = "mysql:host=" . self::$host . ";dbname=" . self::$dbname . ";charset=" . self::$charset;
            $options = [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ];
            self::$instance = new PDO($dsn, self::$username, self::$password, $options);
        }
        return self::$instance;
    }

    public static function query(string $sql, array $params = []): PDOStatement {
        $stmt = self::getInstance()->prepare($sql);
        $stmt->execute($params);
        return $stmt;
    }

    public static function fetchAll(string $sql, array $params = []): array {
        return self::query($sql, $params)->fetchAll();
    }

    public static function fetchOne(string $sql, array $params = []): ?array {
        $result = self::query($sql, $params)->fetch();
        return $result ?: null;
    }

    public static function execute(string $sql, array $params = []): bool {
        return self::query($sql, $params)->rowCount() > 0;
    }

    public static function lastInsertId(): string {
        return self::getInstance()->lastInsertId();
    }

    public static function beginTransaction(): bool {
        return self::getInstance()->beginTransaction();
    }

    public static function commit(): bool {
        return self::getInstance()->commit();
    }

    public static function rollback(): bool {
        return self::getInstance()->rollBack();
    }
}
