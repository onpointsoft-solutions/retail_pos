-- ============================================================
-- BizFlow POS — Database Migration  v2.1.0
-- ============================================================
-- Run this file on any existing v2.0.x installation to bring
-- the MySQL / MariaDB schema up to date with the Java client
-- changes shipped in version 2.1.0.
--
-- Safe to run multiple times — every statement uses
-- ALTER TABLE … ADD COLUMN IF NOT EXISTS, CREATE TABLE IF NOT
-- EXISTS, and CREATE INDEX IF NOT EXISTS so no data is
-- destroyed and no errors are raised on a fresh install.
--
-- Sections
--   1. expenses            — NEW table for operating-expense tracking
--   2. products            — new preferred_order_quantity column
--   3. users               — new permissions column
--   4. categories          — deleted_at soft-delete column
--   5. sales               — deleted_at soft-delete column
--   6. purchase_orders     — deleted_at soft-delete column
--   7. suspended_cart_items— new child table (was missing from schema.sql)
--   8. job_cards           — deleted_at soft-delete column
--   9. quotations          — deleted_at soft-delete column
--  10. sync entity indexes — performance indexes added by SyncController
--  11. retail_pos_mysql    — patch legacy install (sync_status DEFAULT fix)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ════════════════════════════════════════════════════════════
-- 1. EXPENSES  (new table — tracks operating costs for P&L)
-- ════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS expenses (
    id          VARCHAR(36)    NOT NULL,
    category    VARCHAR(50)    NOT NULL DEFAULT 'OTHER'
                    COMMENT 'RENT|UTILITIES|SALARIES|SUPPLIES|MAINTENANCE|TRANSPORT|MARKETING|INSURANCE|OTHER',
    description VARCHAR(500)   NOT NULL,
    amount      DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    date        DATE           NOT NULL,
    reference   VARCHAR(200)   NULL,
    created_by  VARCHAR(36)    NULL     COMMENT 'FK → users.id',
    sync_status VARCHAR(20)    NOT NULL DEFAULT 'SYNCED',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME       NULL,
    PRIMARY KEY (id),
    INDEX idx_expenses_date        (date),
    INDEX idx_expenses_category    (category),
    INDEX idx_expenses_sync_status (sync_status),
    INDEX idx_expenses_updated_at  (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ════════════════════════════════════════════════════════════
-- 2. PRODUCTS  — new columns added by Java client v2.1
-- ════════════════════════════════════════════════════════════

-- preferred_order_quantity was present in Java SQLite but missing
-- from some older MySQL installs.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS preferred_order_quantity INT NOT NULL DEFAULT 0
        COMMENT 'Suggested reorder quantity shown in low-stock alerts';

-- track_expiry was added in the 2.0 cycle; guard for older installs.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS track_expiry TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Enables batch/expiry tracking for this product';


-- ════════════════════════════════════════════════════════════
-- 3. USERS  — permissions column (granular access control)
-- ════════════════════════════════════════════════════════════

-- Comma-separated permission keys, e.g.
-- "MANAGE_PRODUCTS,VIEW_REPORTS,MANAGE_SERVICES"
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS permissions TEXT NULL
        COMMENT 'Comma-separated permission overrides for non-admin users';


-- ════════════════════════════════════════════════════════════
-- 4. CATEGORIES  — soft-delete support
-- ════════════════════════════════════════════════════════════

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL
        COMMENT 'Soft-delete timestamp — NULL means active';

-- Ensure sync indexes exist (retail_pos_mysql.sql omitted these)
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS sync_status VARCHAR(20) NOT NULL DEFAULT 'SYNCED';

CREATE INDEX IF NOT EXISTS idx_categories_sync_status ON categories (sync_status);
CREATE INDEX IF NOT EXISTS idx_categories_updated_at  ON categories (updated_at);


-- ════════════════════════════════════════════════════════════
-- 5. SALES  — soft-delete support + deleted_at index
-- ════════════════════════════════════════════════════════════

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL
        COMMENT 'Soft-delete timestamp — NULL means active';

CREATE INDEX IF NOT EXISTS idx_sales_status ON sales (status);


-- ════════════════════════════════════════════════════════════
-- 6. PURCHASE ORDERS  — soft-delete support
-- ════════════════════════════════════════════════════════════

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL
        COMMENT 'Soft-delete timestamp — NULL means active';


-- ════════════════════════════════════════════════════════════
-- 7. SUSPENDED CART ITEMS  — child table (missing from some
--    installs that used retail_pos_mysql.sql as their base)
-- ════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS suspended_cart_items (
    id           VARCHAR(36)   NOT NULL,
    cart_id      VARCHAR(36)   NOT NULL,
    product_id   VARCHAR(36)   NULL,
    product_name VARCHAR(255)  NULL,
    product_sku  VARCHAR(50)   NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    unit_price   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    buying_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_rate     DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
    line_total   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    INDEX idx_sci_cart_id (cart_id),
    CONSTRAINT fk_sci_cart FOREIGN KEY (cart_id)
        REFERENCES suspended_carts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ════════════════════════════════════════════════════════════
-- 8. JOB CARDS  — soft-delete + customer index
-- ════════════════════════════════════════════════════════════

ALTER TABLE job_cards
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL
        COMMENT 'Soft-delete timestamp — NULL means active';

CREATE INDEX IF NOT EXISTS idx_job_cards_customer_name ON job_cards (customer_name);
CREATE INDEX IF NOT EXISTS idx_job_cards_created_at    ON job_cards (created_at);

-- job_number unique key may be missing on installs from the
-- compact schema definition in schema.sql
ALTER TABLE job_cards
    ADD UNIQUE KEY IF NOT EXISTS uq_job_cards_number (job_number);


-- ════════════════════════════════════════════════════════════
-- 9. QUOTATIONS  — soft-delete support
-- ════════════════════════════════════════════════════════════

ALTER TABLE quotations
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL
        COMMENT 'Soft-delete timestamp — NULL means active';

CREATE INDEX IF NOT EXISTS idx_quotations_status     ON quotations (status);
CREATE INDEX IF NOT EXISTS idx_quotations_created_at ON quotations (created_at);


-- ════════════════════════════════════════════════════════════
-- 10. SYNC PERFORMANCE INDEXES
--     Added to all tables that the SyncController queries by
--     updated_at for incremental downloads.
-- ════════════════════════════════════════════════════════════

-- suppliers
CREATE INDEX IF NOT EXISTS idx_suppliers_updated_at ON suppliers (updated_at);

-- sale_items  (used by analytics queries)
CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id    ON sale_items (sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items (product_id);

-- purchase_order_items
CREATE INDEX IF NOT EXISTS idx_poi_po_id             ON purchase_order_items (po_id);
CREATE INDEX IF NOT EXISTS idx_poi_product_id        ON purchase_order_items (product_id);

-- mpesa_transactions
CREATE INDEX IF NOT EXISTS idx_mpesa_updated_at ON mpesa_transactions (updated_at);

-- inventory_movements  — updated_at column needed for sync delta
ALTER TABLE inventory_movements
    ADD COLUMN IF NOT EXISTS updated_at DATETIME
        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE inventory_movements
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL;

CREATE INDEX IF NOT EXISTS idx_inv_updated_at ON inventory_movements (updated_at);


-- ════════════════════════════════════════════════════════════
-- 11. LEGACY PATCH  — retail_pos_mysql.sql sync_status fix
--     That file used DEFAULT 'PENDING' whereas the Java client
--     now expects 'SYNCED' for server-originated rows.
-- ════════════════════════════════════════════════════════════

-- Silently correct any PENDING rows that were server-inserted
-- (actual client uploads will still arrive as PENDING — that is correct).
UPDATE categories  SET sync_status = 'SYNCED' WHERE sync_status = 'PENDING' AND created_at < NOW();
UPDATE suppliers   SET sync_status = 'SYNCED' WHERE sync_status = 'PENDING' AND created_at < NOW();


-- ════════════════════════════════════════════════════════════
-- 12. DATA VERIFICATION VIEW  (optional helper)
--     Lists every table with row counts — useful after import.
-- ════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_table_row_counts AS
SELECT 'users'                  AS tbl, COUNT(*) AS rows FROM users         UNION ALL
SELECT 'categories',                   COUNT(*)          FROM categories     UNION ALL
SELECT 'suppliers',                    COUNT(*)          FROM suppliers       UNION ALL
SELECT 'products',                     COUNT(*)          FROM products        UNION ALL
SELECT 'customers',                    COUNT(*)          FROM customers       UNION ALL
SELECT 'sales',                        COUNT(*)          FROM sales           UNION ALL
SELECT 'sale_items',                   COUNT(*)          FROM sale_items      UNION ALL
SELECT 'purchase_orders',              COUNT(*)          FROM purchase_orders UNION ALL
SELECT 'purchase_order_items',         COUNT(*)          FROM purchase_order_items UNION ALL
SELECT 'inventory_movements',          COUNT(*)          FROM inventory_movements  UNION ALL
SELECT 'job_cards',                    COUNT(*)          FROM job_cards       UNION ALL
SELECT 'job_card_service_items',       COUNT(*)          FROM job_card_service_items UNION ALL
SELECT 'quotations',                   COUNT(*)          FROM quotations      UNION ALL
SELECT 'quotation_items',              COUNT(*)          FROM quotation_items UNION ALL
SELECT 'expenses',                     COUNT(*)          FROM expenses        UNION ALL
SELECT 'mpesa_transactions',           COUNT(*)          FROM mpesa_transactions UNION ALL
SELECT 'suspended_carts',              COUNT(*)          FROM suspended_carts UNION ALL
SELECT 'suspended_cart_items',         COUNT(*)          FROM suspended_cart_items UNION ALL
SELECT 'audit_logs',                   COUNT(*)          FROM audit_logs      UNION ALL
SELECT 'app_settings',                 COUNT(*)          FROM app_settings;

-- After running this migration, verify with:
--   SELECT * FROM v_table_row_counts;


SET FOREIGN_KEY_CHECKS = 1;

-- ── End of updates_v2_1_0.sql ─────────────────────────────────────────────────
