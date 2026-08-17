<?php
declare(strict_types=1);

/**
 * VisitTracker
 *
 * Records every public page visit to the `page_visits` table.
 * Falls back silently if the DB is unavailable so the page still loads.
 *
 * Usage (top of any public page, after DB is accessible):
 *   require_once __DIR__ . '/../helpers/VisitTracker.php';
 *   VisitTracker::record('home');        // or 'pricing', 'download', etc.
 */
class VisitTracker
{
    /**
     * Record one visit.
     *
     * @param string $page  Short slug for the page, e.g. 'home', 'pricing'.
     */
    public static function record(string $page): void
    {
        try {
            require_once __DIR__ . '/../config/database.php';
            $pdo = Database::getConnection();

            self::ensureTable($pdo);

            $ip        = self::clientIp();
            $userAgent = isset($_SERVER['HTTP_USER_AGENT'])
                ? substr($_SERVER['HTTP_USER_AGENT'], 0, 512) : null;
            $referer   = isset($_SERVER['HTTP_REFERER'])
                ? substr($_SERVER['HTTP_REFERER'], 0, 512) : null;

            $stmt = $pdo->prepare(
                'INSERT INTO page_visits
                    (page, ip_address, user_agent, referer, visited_at)
                 VALUES
                    (:page, :ip, :ua, :ref, NOW())'
            );
            $stmt->execute([
                ':page' => $page,
                ':ip'   => $ip,
                ':ua'   => $userAgent,
                ':ref'  => $referer,
            ]);

        } catch (Throwable) {
            // Never break the page because of tracking errors.
        }
    }

    /**
     * Return visit counts grouped by page (most visited first).
     *
     * @return array<array{page:string, visits:int, unique_ips:int, last_visit:string}>
     */
    public static function summary(): array
    {
        try {
            require_once __DIR__ . '/../config/database.php';
            $pdo = Database::getConnection();

            self::ensureTable($pdo);

            $stmt = $pdo->query(
                'SELECT
                    page,
                    COUNT(*)                     AS visits,
                    COUNT(DISTINCT ip_address)   AS unique_ips,
                    MAX(visited_at)              AS last_visit
                 FROM page_visits
                 GROUP BY page
                 ORDER BY visits DESC'
            );
            return $stmt->fetchAll();
        } catch (Throwable) {
            return [];
        }
    }

    /**
     * Return the most recent N visits (newest first).
     *
     * @param  int $limit
     * @return array
     */
    public static function recent(int $limit = 50): array
    {
        try {
            require_once __DIR__ . '/../config/database.php';
            $pdo = Database::getConnection();

            self::ensureTable($pdo);

            $stmt = $pdo->prepare(
                'SELECT page, ip_address, user_agent, referer, visited_at
                 FROM   page_visits
                 ORDER  BY visited_at DESC
                 LIMIT  :lim'
            );
            $stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
            $stmt->execute();
            return $stmt->fetchAll();
        } catch (Throwable) {
            return [];
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Create the table on first use (idempotent).
     */
    private static function ensureTable(PDO $pdo): void
    {
        $pdo->exec(
            'CREATE TABLE IF NOT EXISTS page_visits (
                id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                page        VARCHAR(100)     NOT NULL,
                ip_address  VARCHAR(45)      NULL          COMMENT "IPv4 or IPv6",
                user_agent  VARCHAR(512)     NULL,
                referer     VARCHAR(512)     NULL,
                visited_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                INDEX idx_pv_page       (page),
                INDEX idx_pv_visited_at (visited_at),
                INDEX idx_pv_ip         (ip_address)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
        );
    }

    /**
     * Best-effort real client IP — respects common proxy headers.
     */
    private static function clientIp(): ?string
    {
        foreach (
            ['HTTP_CF_CONNECTING_IP',  // Cloudflare
             'HTTP_X_REAL_IP',          // Nginx proxy
             'HTTP_X_FORWARDED_FOR',    // Generic proxy
             'REMOTE_ADDR']             // Direct connection
            as $key
        ) {
            if (!empty($_SERVER[$key])) {
                // X-Forwarded-For can be a comma-separated list; take the first
                $ip = trim(explode(',', $_SERVER[$key])[0]);
                if (filter_var($ip, FILTER_VALIDATE_IP)) {
                    return $ip;
                }
            }
        }
        return null;
    }
}
