-- ============================================================
-- Retail POS System — Seed Data
-- Default admin password: admin123  (bcrypt cost 12)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ── Default Admin User ───────────────────────────────────────────────────────
-- Password: admin123  →  bcrypt cost 12 (verified correct hash)
INSERT IGNORE INTO users (id, username, password_hash, role, full_name, active, sync_status, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    '$2y$12$LCiVmxBn2y4mGPIzFIhPFen/w7Fk/M7PQAN0/4zHRt7yI8vCvPMGm',
    'ADMIN',
    'Administrator',
    1,
    'SYNCED',
    NOW(),
    NOW()
);

-- ── Default Cashier User ─────────────────────────────────────────────────────
-- Password: cashier123  →  bcrypt cost 12
INSERT IGNORE INTO users (id, username, password_hash, role, full_name, active, sync_status, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'cashier',
    '$2y$12$KIB3WnRnJPMY2IyH9bMKSOzCBCGSGXp7ZLhd3Ln4LvjRn3qTrDr4C',
    'CASHIER',
    'Default Cashier',
    1,
    'SYNCED',
    NOW(),
    NOW()
);

-- ── Default Categories ───────────────────────────────────────────────────────
INSERT IGNORE INTO categories (id, name, description, color, sort_order, is_active, sync_status) VALUES
('c0000000-0000-0000-0000-000000000001', 'General',     'General merchandise',       '#607D8B', 0,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000002', 'Food',        'Food items',                '#FF5722', 1,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000003', 'Beverages',   'Drinks and beverages',      '#2196F3', 2,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000004', 'Snacks',      'Snack foods',               '#FFC107', 3,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000005', 'Electronics', 'Electronic products',       '#9C27B0', 4,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000006', 'Clothing',    'Apparel and accessories',   '#4CAF50', 5,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000007', 'Household',   'Household supplies',        '#795548', 6,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000008', 'Health',      'Health and personal care',  '#E91E63', 7,  1, 'SYNCED'),
('c0000000-0000-0000-0000-000000000009', 'Stationery',  'Office and school supplies','#00BCD4', 8,  1, 'SYNCED'),
('c0000000-0000-0000-0000-00000000000a', 'Other',       'Miscellaneous items',       '#9E9E9E', 99, 1, 'SYNCED');

-- ── Default App Settings ─────────────────────────────────────────────────────
INSERT IGNORE INTO app_settings (`key`, `value`) VALUES
('store_name',           'My Retail Store'),
('store_address',        ''),
('store_phone',          ''),
('store_email',          ''),
('currency_symbol',      '$'),
('currency_code',        'USD'),
('tax_rate',             '0'),
('receipt_header',       'Thank you for shopping with us!'),
('receipt_footer',       'Please come again.'),
('low_stock_threshold',  '10'),
('sync_interval_minutes','15'),
('app_version',          '2.0.0'),
('timezone',             'UTC');

SET FOREIGN_KEY_CHECKS = 1;
