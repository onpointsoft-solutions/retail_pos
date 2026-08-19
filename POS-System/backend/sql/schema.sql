-- ============================================================
-- Retail POS System — MySQL Schema
-- Column names match the Java SQLite schema exactly for seamless sync.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                    VARCHAR(36)   NOT NULL,
    username              VARCHAR(64)   NOT NULL,
    password_hash         VARCHAR(255)  NOT NULL,
    role                  VARCHAR(20)   NOT NULL DEFAULT 'CASHIER',
    permissions           TEXT          NULL,
    full_name             VARCHAR(150)  NULL,
    active                TINYINT(1)    NOT NULL DEFAULT 1,
    failed_login_attempts INT           NOT NULL DEFAULT 0,
    lockout_until         DATETIME      NULL,
    sync_status           VARCHAR(20)   NOT NULL DEFAULT 'SYNCED',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at            DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username  (username),
    INDEX idx_users_role          (role),
    INDEX idx_users_sync_status   (sync_status),
    INDEX idx_users_updated_at    (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Categories ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    sync_status VARCHAR(20)  NOT NULL DEFAULT 'SYNCED',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_name     (name),
    INDEX idx_categories_sync_status  (sync_status),
    INDEX idx_categories_updated_at   (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Suppliers ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS suppliers (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(30)  NULL,
    email       VARCHAR(150) NULL,
    address     TEXT         NULL,
    balance     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    sync_status VARCHAR(20)  NOT NULL DEFAULT 'SYNCED',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME     NULL,
    PRIMARY KEY (id),
    INDEX idx_suppliers_name        (name),
    INDEX idx_suppliers_sync_status (sync_status),
    INDEX idx_suppliers_updated_at  (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Products ──────────────────────────────────────────────────────────────────
-- Column names exactly match Java SQLite schema for zero-friction sync.
CREATE TABLE IF NOT EXISTS products (
    id              VARCHAR(36)   NOT NULL,
    barcode         VARCHAR(50)   NULL,
    qr_code         VARCHAR(100)  NULL,
    sku             VARCHAR(50)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    category_id     VARCHAR(36)   NULL,
    buying_price    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    selling_price   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    wholesale_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_stock   INT           NOT NULL DEFAULT 0,
    minimum_stock   INT           NOT NULL DEFAULT 0,
    preferred_order_quantity INT  NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
    discount        DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
    supplier_id     VARCHAR(36)   NULL,
    description     TEXT          NULL,
    image_path      VARCHAR(500)  NULL,
    unit            VARCHAR(20)   NOT NULL DEFAULT 'pcs',
    status          VARCHAR(20)   NOT NULL DEFAULT 'active',
    track_expiry    TINYINT(1)    NOT NULL DEFAULT 0,
    sync_status     VARCHAR(20)   NOT NULL DEFAULT 'SYNCED',
    version         BIGINT        NOT NULL DEFAULT 1,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_products_sku      (sku),
    INDEX idx_products_barcode      (barcode),
    INDEX idx_products_qr_code      (qr_code),
    INDEX idx_products_name         (name),
    INDEX idx_products_category_id  (category_id),
    INDEX idx_products_supplier_id  (supplier_id),
    INDEX idx_products_sync_status  (sync_status),
    INDEX idx_products_updated_at   (updated_at),
    INDEX idx_products_status       (status),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers  (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Customers ─────────────────────────────────────────────────────────────────
-- Additional product photos synced by the desktop client. The first photo remains products.image_path.
CREATE TABLE IF NOT EXISTS product_images (
    id            VARCHAR(36)  NOT NULL,
    product_id    VARCHAR(36)  NOT NULL,
    image_path    VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    sync_status   VARCHAR(20)  NOT NULL DEFAULT 'SYNCED',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME     NULL,
    PRIMARY KEY (id),
    INDEX idx_product_images_product_id (product_id),
    INDEX idx_product_images_sync_status (sync_status),
    INDEX idx_product_images_updated_at (updated_at),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customers (
    id              VARCHAR(36)   NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    phone           VARCHAR(20)   NULL,
    email           VARCHAR(150)  NULL,
    loyalty_points  INT           NOT NULL DEFAULT 0,
    credit_balance  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    sync_status     VARCHAR(20)   NOT NULL DEFAULT 'SYNCED',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_customers_name        (name),
    INDEX idx_customers_phone       (phone),
    INDEX idx_customers_email       (email),
    INDEX idx_customers_sync_status (sync_status),
    INDEX idx_customers_updated_at  (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Sales ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sales (
    id                VARCHAR(36)   NOT NULL,
    receipt_number    VARCHAR(50)   NOT NULL,
    cashier_id        VARCHAR(36)   NULL,
    cashier_name      VARCHAR(150)  NULL,
    customer_id       VARCHAR(36)   NULL,
    subtotal          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    grand_total       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    payment_method    VARCHAR(30)   NOT NULL DEFAULT 'CASH',
    cash_tendered     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    change_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    payment_reference VARCHAR(100)  NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    sync_status       VARCHAR(20)   NOT NULL DEFAULT 'SYNCED',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_sales_receipt      (receipt_number),
    INDEX idx_sales_cashier_id       (cashier_id),
    INDEX idx_sales_customer_id      (customer_id),
    INDEX idx_sales_payment_method   (payment_method),
    INDEX idx_sales_sync_status      (sync_status),
    INDEX idx_sales_updated_at       (updated_at),
    INDEX idx_sales_created_at       (created_at),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Sale Items ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sale_items (
    id           VARCHAR(36)   NOT NULL,
    sale_id      VARCHAR(36)   NOT NULL,
    product_id   VARCHAR(36)   NULL,
    product_name VARCHAR(255)  NOT NULL,
    product_sku  VARCHAR(50)   NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    unit_price   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    buying_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_rate     DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
    line_total   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    INDEX idx_sale_items_sale_id    (sale_id),
    INDEX idx_sale_items_product_id (product_id),
    CONSTRAINT fk_sale_items_sale    FOREIGN KEY (sale_id)    REFERENCES sales    (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Inventory Movements ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory_movements (
    id           VARCHAR(36)  NOT NULL,
    product_id   VARCHAR(36)  NOT NULL,
    product_name VARCHAR(255) NULL,
    type         VARCHAR(30)  NOT NULL,
    quantity     INT          NOT NULL DEFAULT 0,
    reason       TEXT         NULL,
    batch_number VARCHAR(50)  NULL,
    expiry_date  DATE         NULL,
    user_id      VARCHAR(36)  NULL,
    sync_status  VARCHAR(20)  NOT NULL DEFAULT 'SYNCED',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   DATETIME     NULL,
    PRIMARY KEY (id),
    INDEX idx_inv_product_id  (product_id),
    INDEX idx_inv_type        (type),
    INDEX idx_inv_sync_status (sync_status),
    INDEX idx_inv_created_at  (created_at),
    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Purchase Orders ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase_orders (
    id                     VARCHAR(36)   NOT NULL,
    supplier_id            VARCHAR(36)   NULL,
    supplier_name          VARCHAR(150)  NULL,
    status                 VARCHAR(30)   NOT NULL DEFAULT 'ORDERED',
    expected_delivery_date DATE          NULL,
    notes                  TEXT          NULL,
    sync_status            VARCHAR(20)   NOT NULL DEFAULT 'SYNCED',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at             DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_po_supplier_id  (supplier_id),
    INDEX idx_po_status       (status),
    INDEX idx_po_sync_status  (sync_status),
    INDEX idx_po_updated_at   (updated_at),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Purchase Order Items ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id            VARCHAR(36)   NOT NULL,
    po_id         VARCHAR(36)   NOT NULL,
    product_id    VARCHAR(36)   NULL,
    product_name  VARCHAR(255)  NULL,
    ordered_qty   INT           NOT NULL DEFAULT 0,
    received_qty  INT           NOT NULL DEFAULT 0,
    buying_price  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    INDEX idx_poi_po_id      (po_id),
    INDEX idx_poi_product_id (product_id),
    CONSTRAINT fk_poi_po      FOREIGN KEY (po_id)       REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_poi_product FOREIGN KEY (product_id)  REFERENCES products        (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Suspended Carts ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS suspended_carts (
    id              VARCHAR(36)   NOT NULL,
    cashier_id      VARCHAR(36)   NULL,
    customer_id     VARCHAR(36)   NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    suspended_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Audit Logs ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NULL,
    event_type  VARCHAR(50)  NOT NULL,
    entity_id   VARCHAR(36)  NULL,
    details     TEXT         NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_user_id    (user_id),
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- M-Pesa confirmations received from TransRouter and shared by all workstations.
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

-- ── Service job cards and quotations ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS job_cards (
    id VARCHAR(36) NOT NULL, job_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(36) NULL, customer_name VARCHAR(150) NOT NULL, customer_phone VARCHAR(30) NULL,
    asset_description TEXT NOT NULL, asset_serial VARCHAR(100) NULL, problem_description TEXT NOT NULL,
    diagnosis TEXT NULL, resolution TEXT NULL, technician_id VARCHAR(36) NULL, technician_name VARCHAR(150) NULL,
    labour_charge DECIMAL(12,2) NOT NULL DEFAULT 0.00, status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    active_quotation_id VARCHAR(36) NULL, due_date DATETIME NULL, sync_status VARCHAR(20) NOT NULL DEFAULT 'SYNCED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id), UNIQUE KEY uq_job_cards_number (job_number),
    INDEX idx_job_cards_status (status), INDEX idx_job_cards_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_card_service_items (
    id VARCHAR(36) NOT NULL, job_card_id VARCHAR(36) NOT NULL, description TEXT NOT NULL,
    charge DECIMAL(12,2) NOT NULL DEFAULT 0.00, quantity INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id), INDEX idx_jcsi_job_card_id (job_card_id),
    CONSTRAINT fk_jcsi_job_card FOREIGN KEY (job_card_id) REFERENCES job_cards(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS quotations (
    id VARCHAR(36) NOT NULL, quotation_number VARCHAR(50) NOT NULL, job_card_id VARCHAR(36) NOT NULL,
    job_card_number VARCHAR(50) NULL, invoice_sale_id VARCHAR(36) NULL, customer_id VARCHAR(36) NULL,
    customer_name VARCHAR(150) NOT NULL, customer_phone VARCHAR(30) NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00, discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00, labour_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    grand_total DECIMAL(12,2) NOT NULL DEFAULT 0.00, notes TEXT NULL, status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by_id VARCHAR(36) NULL, created_by_name VARCHAR(150) NULL, valid_until DATETIME NULL,
    sync_status VARCHAR(20) NOT NULL DEFAULT 'SYNCED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id), UNIQUE KEY uq_quotations_number (quotation_number),
    INDEX idx_quotations_job_card_id (job_card_id), INDEX idx_quotations_updated_at (updated_at),
    CONSTRAINT fk_quotation_job_card FOREIGN KEY (job_card_id) REFERENCES job_cards(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS quotation_items (
    id VARCHAR(36) NOT NULL, quotation_id VARCHAR(36) NOT NULL, product_id VARCHAR(36) NULL,
    product_name VARCHAR(255) NOT NULL, product_sku VARCHAR(50) NULL, quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00, buying_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0.00, tax_rate DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id), INDEX idx_quotation_items_quotation_id (quotation_id),
    CONSTRAINT fk_quotation_item FOREIGN KEY (quotation_id) REFERENCES quotations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── App Settings ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS app_settings (
    `key`      VARCHAR(100) NOT NULL,
    `value`    TEXT         NULL,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Expenses ──────────────────────────────────────────────────────────────────
-- Mirrors the SQLite expenses table created by DatabaseManager.java
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

SET FOREIGN_KEY_CHECKS = 1;
