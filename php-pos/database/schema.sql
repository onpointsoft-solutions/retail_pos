-- ============================================================
-- Retail POS Web System - MySQL Schema
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                    VARCHAR(36)   NOT NULL,
    username              VARCHAR(64)   NOT NULL,
    password_hash         VARCHAR(255)  NOT NULL,
    role                  VARCHAR(20)   NOT NULL DEFAULT 'CASHIER',
    full_name             VARCHAR(150)  NULL,
    active                TINYINT(1)    NOT NULL DEFAULT 1,
    failed_login_attempts INT           NOT NULL DEFAULT 0,
    lockout_until         DATETIME      NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at            DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username  (username),
    INDEX idx_users_role          (role),
    INDEX idx_users_updated_at    (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Categories ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_name     (name),
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
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_suppliers_name        (name),
    INDEX idx_suppliers_updated_at  (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Products ──────────────────────────────────────────────────────────────────
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
    INDEX idx_products_updated_at   (updated_at),
    INDEX idx_products_status       (status),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers  (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Product Images ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_images (
    id            VARCHAR(36)  NOT NULL,
    product_id    VARCHAR(36)  NOT NULL,
    image_path    VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_product_images_product_id (product_id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Customers ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customers (
    id              VARCHAR(36)   NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    phone           VARCHAR(20)  NULL,
    email           VARCHAR(150) NULL,
    loyalty_points  INT           NOT NULL DEFAULT 0,
    credit_balance  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_customers_name        (name),
    INDEX idx_customers_phone       (phone),
    INDEX idx_customers_email       (email),
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
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_sales_receipt      (receipt_number),
    INDEX idx_sales_cashier_id       (cashier_id),
    INDEX idx_sales_customer_id      (customer_id),
    INDEX idx_sales_payment_method   (payment_method),
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
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_inv_product_id  (product_id),
    INDEX idx_inv_type        (type),
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
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at             DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_po_supplier_id  (supplier_id),
    INDEX idx_po_status       (status),
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

-- ── Suspended Cart Items ──────────────────────────────────────────────────────
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
    INDEX idx_sci_cart_id      (cart_id),
    INDEX idx_sci_product_id   (product_id),
    CONSTRAINT fk_sci_cart      FOREIGN KEY (cart_id)      REFERENCES suspended_carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_sci_product   FOREIGN KEY (product_id)   REFERENCES products (id) ON DELETE SET NULL
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

-- ── App Settings ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS app_settings (
    `key`      VARCHAR(100) NOT NULL,
    `value`    TEXT         NULL,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Sessions (for web authentication) ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sessions (
    id         VARCHAR(128) NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    ip_address VARCHAR(45)  NULL,
    user_agent TEXT         NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_sessions_user_id (user_id),
    INDEX idx_sessions_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
