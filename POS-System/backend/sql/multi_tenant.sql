-- BizFlow POS multi-business migration.
-- Run this once on an existing backend before activating production licenses.
-- The backend also performs these changes automatically through TenantManager.

CREATE TABLE IF NOT EXISTS businesses (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_business_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TenantManager safely adds business_id to licenses and every synchronized table,
-- migrates legacy rows to the first activated license, and changes these global
-- unique keys into business-scoped keys:
-- users(username), categories(name), products(sku), sales(receipt_number),
-- mpesa_transactions(code), and app_settings(business_id, key).
--
-- The automatic migration is preferred because it checks information_schema
-- before each ALTER and remains safe to run after partial deployments.
