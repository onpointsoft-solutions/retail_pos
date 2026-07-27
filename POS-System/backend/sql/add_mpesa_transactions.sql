CREATE TABLE IF NOT EXISTS mpesa_transactions (
    id            VARCHAR(36)   NOT NULL,
    code          VARCHAR(20)   NOT NULL,
    customer_name VARCHAR(150)  NULL,
    amount        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    received_at   BIGINT        NOT NULL,
    sync_status   VARCHAR(20)   NOT NULL DEFAULT 'SYNCED',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_mpesa_code (code),
    INDEX idx_mpesa_received_at (received_at),
    INDEX idx_mpesa_sync_status (sync_status),
    INDEX idx_mpesa_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
