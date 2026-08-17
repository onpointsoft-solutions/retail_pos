-- ============================================================
-- Retail POS System — Job Cards & Quotations Schema
-- MySQL / MariaDB  (utf8mb4)
--
-- Tables covered:
--   1. job_cards              — service jobs opened for customers
--   2. job_card_service_items — labour lines on a job card
--   3. quotations             — parts quotation raised against a job card
--   4. quotation_items        — individual part/product lines on a quotation
--
-- Dependencies (must exist before running this file):
--   customers, products, users, sales
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. JOB CARDS
--    One row per service job. Tracks the customer, the asset being serviced,
--    the technician assigned, labour charge, and the overall lifecycle status.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS job_cards (
    id                   VARCHAR(36)    NOT NULL
        COMMENT 'UUID primary key',

    -- Numbering
    job_number           VARCHAR(50)    NOT NULL
        COMMENT 'Human-readable number, e.g. JOB-20260816-0001',

    -- Customer
    customer_id          VARCHAR(36)    NULL
        COMMENT 'FK → customers.id (nullable: walk-in jobs)',
    customer_name        VARCHAR(150)   NOT NULL
        COMMENT 'Denormalised for display / printing',
    customer_phone       VARCHAR(30)    NULL,

    -- Asset being serviced
    asset_description    TEXT           NOT NULL
        COMMENT 'e.g. Samsung TV UA55, Toyota 110 KCA 123A',
    asset_serial         VARCHAR(100)   NULL
        COMMENT 'Serial number, plate number, IMEI, etc.',

    -- Service details
    problem_description  TEXT           NOT NULL
        COMMENT 'What the customer reported',
    diagnosis            TEXT           NULL
        COMMENT 'Technician diagnosis after inspection',
    resolution           TEXT           NULL
        COMMENT 'Work done / parts replaced',

    -- Technician
    technician_id        VARCHAR(36)    NULL
        COMMENT 'FK → users.id',
    technician_name      VARCHAR(150)   NULL
        COMMENT 'Denormalised name for display',

    -- Financials
    labour_charge        DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Flat labour fee outside service-item lines',

    -- Lifecycle
    status               VARCHAR(30)    NOT NULL DEFAULT 'OPEN'
        COMMENT 'OPEN | IN_PROGRESS | AWAITING_PARTS | COMPLETED | CANCELLED | INVOICED',

    -- Linked quotation
    active_quotation_id  VARCHAR(36)    NULL
        COMMENT 'FK → quotations.id — the current active quotation for this job',

    -- Dates
    due_date             DATETIME       NULL,
    sync_status          VARCHAR(20)    NOT NULL DEFAULT 'SYNCED'
        COMMENT 'SYNCED | PENDING | MODIFIED',
    created_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                 ON UPDATE CURRENT_TIMESTAMP,
    deleted_at           DATETIME       NULL
        COMMENT 'Soft-delete timestamp',

    -- ── Constraints ────────────────────────────────────────────────────────
    PRIMARY KEY (id),
    UNIQUE  KEY uq_job_cards_number      (job_number),
    INDEX       idx_job_cards_customer   (customer_id),
    INDEX       idx_job_cards_technician (technician_id),
    INDEX       idx_job_cards_status     (status),
    INDEX       idx_job_cards_sync       (sync_status),
    INDEX       idx_job_cards_updated_at (updated_at),
    INDEX       idx_job_cards_due_date   (due_date),

    CONSTRAINT fk_job_cards_customer
        FOREIGN KEY (customer_id)
        REFERENCES  customers (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_job_cards_technician
        FOREIGN KEY (technician_id)
        REFERENCES  users (id)
        ON DELETE SET NULL

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Service job cards';


-- ─────────────────────────────────────────────────────────────────────────────
-- 2. JOB CARD SERVICE ITEMS  (labour lines)
--    Each row is one line of work performed on the job (not parts).
--    Parts are tracked in quotation_items instead.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS job_card_service_items (
    id          VARCHAR(36)    NOT NULL
        COMMENT 'UUID primary key',
    job_card_id VARCHAR(36)    NOT NULL
        COMMENT 'FK → job_cards.id',

    description TEXT           NOT NULL
        COMMENT 'e.g. Panel replacement, Diagnostic fee, Software install',
    charge      DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Unit price for this service line',
    quantity    INT            NOT NULL DEFAULT 1,

    -- ── Constraints ────────────────────────────────────────────────────────
    PRIMARY KEY (id),
    INDEX idx_jcsi_job_card (job_card_id),

    CONSTRAINT fk_jcsi_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES  job_cards (id)
        ON DELETE CASCADE

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Labour / service lines on a job card';


-- ─────────────────────────────────────────────────────────────────────────────
-- 3. QUOTATIONS
--    A quotation is raised against a job card and lists the parts (products
--    from inventory) required to complete the service.  Once approved it can
--    be converted to an invoice / sale which decrements stock.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quotations (
    id                VARCHAR(36)    NOT NULL
        COMMENT 'UUID primary key',

    -- Numbering
    quotation_number  VARCHAR(50)    NOT NULL
        COMMENT 'Human-readable number, e.g. QT-20260816-0001',

    -- Parent job card
    job_card_id       VARCHAR(36)    NOT NULL
        COMMENT 'FK → job_cards.id',
    job_card_number   VARCHAR(50)    NULL
        COMMENT 'Denormalised for display / printing',

    -- Invoice link (set when status → INVOICED)
    invoice_sale_id   VARCHAR(36)    NULL
        COMMENT 'FK → sales.id — populated after the quotation is invoiced',

    -- Customer (copied from job card at creation)
    customer_id       VARCHAR(36)    NULL
        COMMENT 'FK → customers.id',
    customer_name     VARCHAR(150)   NOT NULL,
    customer_phone    VARCHAR(30)    NULL,

    -- Financials
    subtotal          DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Sum of all quotation_items.line_total',
    discount_amount   DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    labour_total      DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Copied from job card labour at invoice time',
    grand_total       DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT 'subtotal - discount + tax + labour_total',

    notes             TEXT           NULL,

    -- Lifecycle
    status            VARCHAR(30)    NOT NULL DEFAULT 'DRAFT'
        COMMENT 'DRAFT | SENT | APPROVED | REJECTED | INVOICED',

    -- Authored by
    created_by_id     VARCHAR(36)    NULL
        COMMENT 'FK → users.id',
    created_by_name   VARCHAR(150)   NULL
        COMMENT 'Denormalised name',

    valid_until       DATETIME       NULL
        COMMENT 'Quote expiry date',
    sync_status       VARCHAR(20)    NOT NULL DEFAULT 'SYNCED'
        COMMENT 'SYNCED | PENDING | MODIFIED',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                              ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME       NULL
        COMMENT 'Soft-delete timestamp',

    -- ── Constraints ────────────────────────────────────────────────────────
    PRIMARY KEY (id),
    UNIQUE  KEY uq_quotations_number         (quotation_number),
    INDEX       idx_quotations_job_card      (job_card_id),
    INDEX       idx_quotations_customer      (customer_id),
    INDEX       idx_quotations_status        (status),
    INDEX       idx_quotations_invoice_sale  (invoice_sale_id),
    INDEX       idx_quotations_sync          (sync_status),
    INDEX       idx_quotations_updated_at    (updated_at),
    INDEX       idx_quotations_valid_until   (valid_until),

    CONSTRAINT fk_quotations_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES  job_cards (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_quotations_customer
        FOREIGN KEY (customer_id)
        REFERENCES  customers (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_quotations_created_by
        FOREIGN KEY (created_by_id)
        REFERENCES  users (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_quotations_invoice_sale
        FOREIGN KEY (invoice_sale_id)
        REFERENCES  sales (id)
        ON DELETE SET NULL

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Parts quotations raised against service job cards';


-- ─────────────────────────────────────────────────────────────────────────────
-- 4. QUOTATION ITEMS  (parts / product lines)
--    Each row is one product (part) included in a quotation.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quotation_items (
    id            VARCHAR(36)    NOT NULL
        COMMENT 'UUID primary key',
    quotation_id  VARCHAR(36)    NOT NULL
        COMMENT 'FK → quotations.id',

    -- Product reference (nullable: free-text / non-stock parts allowed)
    product_id    VARCHAR(36)    NULL
        COMMENT 'FK → products.id (NULL for free-text parts)',
    product_name  VARCHAR(255)   NOT NULL,
    product_sku   VARCHAR(50)    NULL,

    -- Quantities & pricing
    quantity      INT            NOT NULL DEFAULT 1,
    unit_price    DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Selling price charged to customer',
    buying_price  DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Cost price at time of quoting (for margin reporting)',
    discount      DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_rate      DECIMAL(6,  2) NOT NULL DEFAULT 0.00,
    line_total    DECIMAL(12, 2) NOT NULL DEFAULT 0.00
        COMMENT '(unit_price × quantity) − discount',

    -- ── Constraints ────────────────────────────────────────────────────────
    PRIMARY KEY (id),
    INDEX idx_qi_quotation (quotation_id),
    INDEX idx_qi_product   (product_id),

    CONSTRAINT fk_qi_quotation
        FOREIGN KEY (quotation_id)
        REFERENCES  quotations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_qi_product
        FOREIGN KEY (product_id)
        REFERENCES  products (id)
        ON DELETE SET NULL

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Product / parts lines on a quotation';


-- ─────────────────────────────────────────────────────────────────────────────
-- Back-reference: job_cards.active_quotation_id → quotations.id
-- Added as a deferred FK after both tables exist.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE job_cards
    ADD CONSTRAINT fk_job_cards_active_quotation
        FOREIGN KEY (active_quotation_id)
        REFERENCES  quotations (id)
        ON DELETE SET NULL;

SET FOREIGN_KEY_CHECKS = 1;
