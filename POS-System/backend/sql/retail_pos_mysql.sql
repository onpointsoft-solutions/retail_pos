-- MySQL-compatible version of retail_pos.sql
-- Converted from SQLite dump for MySQL import

SET FOREIGN_KEY_CHECKS = 0;
SET AUTOCOMMIT = 0;

-- App Settings
CREATE TABLE IF NOT EXISTS `app_settings` (
    `key` VARCHAR(255) NOT NULL,
    `value` TEXT,
    PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit Logs
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` VARCHAR(36) NOT NULL,
    `user_id` VARCHAR(36),
    `event_type` VARCHAR(100),
    `entity_id` VARCHAR(36),
    `details` TEXT,
    `created_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Categories
CREATE TABLE IF NOT EXISTS `categories` (
    `id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Customers
CREATE TABLE IF NOT EXISTS `customers` (
    `id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `phone` VARCHAR(30) UNIQUE,
    `email` VARCHAR(150) UNIQUE,
    `loyalty_points` INT DEFAULT 0,
    `credit_balance` DECIMAL(12,2) DEFAULT 0.00,
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inventory Movements
CREATE TABLE IF NOT EXISTS `inventory_movements` (
    `id` VARCHAR(36) NOT NULL,
    `product_id` VARCHAR(36) NOT NULL,
    `product_name` VARCHAR(255),
    `type` VARCHAR(50) NOT NULL,
    `quantity` INT,
    `reason` TEXT,
    `batch_number` VARCHAR(100),
    `expiry_date` DATE,
    `user_id` VARCHAR(36),
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_id`) REFERENCES `products`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product Images
CREATE TABLE IF NOT EXISTS `product_images` (
    `id` VARCHAR(36) NOT NULL,
    `product_id` VARCHAR(36) NOT NULL,
    `image_path` TEXT NOT NULL,
    `display_order` INT DEFAULT 0,
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    `updated_at` DATETIME,
    `deleted_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_id`) REFERENCES `products`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Products
CREATE TABLE IF NOT EXISTS `products` (
    `id` VARCHAR(36) NOT NULL,
    `barcode` VARCHAR(50),
    `qr_code` VARCHAR(100),
    `sku` VARCHAR(50) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `category_id` VARCHAR(36),
    `buying_price` DECIMAL(12,2) DEFAULT 0.00,
    `selling_price` DECIMAL(12,2) NOT NULL,
    `wholesale_price` DECIMAL(12,2) DEFAULT 0.00,
    `current_stock` INT DEFAULT 0,
    `minimum_stock` INT DEFAULT 0,
    `tax_rate` DECIMAL(6,2) DEFAULT 0.00,
    `discount` DECIMAL(6,2) DEFAULT 0.00,
    `supplier_id` VARCHAR(36),
    `description` TEXT,
    `image_path` TEXT,
    `unit` VARCHAR(20) DEFAULT 'pcs',
    `status` VARCHAR(20) DEFAULT 'active',
    `track_expiry` TINYINT(1) DEFAULT 0,
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `version` BIGINT DEFAULT 1,
    `created_at` DATETIME,
    `updated_at` DATETIME,
    `deleted_at` DATETIME,
    `preferred_order_quantity` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Purchase Order Items
CREATE TABLE IF NOT EXISTS `purchase_order_items` (
    `id` VARCHAR(36) NOT NULL,
    `po_id` VARCHAR(36) NOT NULL,
    `product_id` VARCHAR(36),
    `product_name` VARCHAR(255),
    `ordered_qty` INT,
    `received_qty` INT DEFAULT 0,
    `buying_price` DECIMAL(12,2),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`po_id`) REFERENCES `purchase_orders`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Purchase Orders
CREATE TABLE IF NOT EXISTS `purchase_orders` (
    `id` VARCHAR(36) NOT NULL,
    `supplier_id` VARCHAR(36),
    `supplier_name` VARCHAR(150),
    `status` VARCHAR(20) DEFAULT 'ORDERED',
    `expected_delivery_date` DATE,
    `notes` TEXT,
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`supplier_id`) REFERENCES `suppliers`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sale Items
CREATE TABLE IF NOT EXISTS `sale_items` (
    `id` VARCHAR(36) NOT NULL,
    `sale_id` VARCHAR(36) NOT NULL,
    `product_id` VARCHAR(36),
    `product_name` VARCHAR(255),
    `product_sku` VARCHAR(50),
    `quantity` INT,
    `unit_price` DECIMAL(12,2),
    `buying_price` DECIMAL(12,2),
    `discount` DECIMAL(6,2) DEFAULT 0.00,
    `tax_rate` DECIMAL(6,2) DEFAULT 0.00,
    `line_total` DECIMAL(12,2),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`sale_id`) REFERENCES `sales`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sales
CREATE TABLE IF NOT EXISTS `sales` (
    `id` VARCHAR(36) NOT NULL,
    `receipt_number` VARCHAR(50) NOT NULL UNIQUE,
    `cashier_id` VARCHAR(36),
    `cashier_name` VARCHAR(150),
    `customer_id` VARCHAR(36),
    `subtotal` DECIMAL(12,2),
    `discount_amount` DECIMAL(12,2) DEFAULT 0.00,
    `tax_amount` DECIMAL(12,2) DEFAULT 0.00,
    `grand_total` DECIMAL(12,2),
    `payment_method` VARCHAR(50),
    `cash_tendered` DECIMAL(12,2) DEFAULT 0.00,
    `change_amount` DECIMAL(12,2) DEFAULT 0.00,
    `payment_reference` VARCHAR(100),
    `status` VARCHAR(20) DEFAULT 'COMPLETED',
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Suppliers
CREATE TABLE IF NOT EXISTS `suppliers` (
    `id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `phone` VARCHAR(30),
    `email` VARCHAR(150),
    `address` TEXT,
    `balance` DECIMAL(12,2) DEFAULT 0.00,
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Suspended Cart Items
CREATE TABLE IF NOT EXISTS `suspended_cart_items` (
    `id` VARCHAR(36) NOT NULL,
    `cart_id` VARCHAR(36) NOT NULL,
    `product_id` VARCHAR(36),
    `product_name` VARCHAR(255),
    `product_sku` VARCHAR(50),
    `quantity` INT,
    `unit_price` DECIMAL(12,2),
    `buying_price` DECIMAL(12,2),
    `discount` DECIMAL(6,2) DEFAULT 0.00,
    `tax_rate` DECIMAL(6,2) DEFAULT 0.00,
    `line_total` DECIMAL(12,2),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`cart_id`) REFERENCES `suspended_carts`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Suspended Carts
CREATE TABLE IF NOT EXISTS `suspended_carts` (
    `id` VARCHAR(36) NOT NULL,
    `cashier_id` VARCHAR(36),
    `customer_id` VARCHAR(36),
    `discount_amount` DECIMAL(12,2) DEFAULT 0.00,
    `suspended_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Users
CREATE TABLE IF NOT EXISTS `users` (
    `id` VARCHAR(36) NOT NULL,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `full_name` VARCHAR(150),
    `active` TINYINT(1) DEFAULT 1,
    `failed_login_attempts` INT DEFAULT 0,
    `lockout_until` DATETIME,
    `sync_status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` DATETIME,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

-- Insert data converted from SQLite
INSERT INTO `app_settings` (`key`, `value`) VALUES 
('store_name','victorious general shop'),
('store_address','kabarak,Nakuru'),
('store_phone','0742071810'),
('store_footer','Thank you for shopping with us!'),
('logo_path','C:\\Users\\Victorious\\AppData\\Local\\RetailPOS\\images\\logo-b27b2708-2f14-42e9-863e-0ef97a870580.png'),
('printer_name','(Default printer)'),
('paper_width','80'),
('tax_rate','0.0'),
('loyalty_earning_rate','1.0'),
('sync_api_url','https://pos.victoriousgeneralshop.com/api/'),
('sync_api_token','eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiNjI3NzBjZDgtODYxZi0xMWYxLWJhZjctOWM3YmVmNzY1YTllIiwidXNlcm5hbWUiOiJlcmlja21vc2VzcyIsInJvbGUiOiJBRE1JTiIsInN0b3JlX2lkIjpudWxsLCJpYXQiOjE3ODQ4MDI4MzcsImV4cCI6MTc4NDg4OTIzN30.Qv8PVt0lVHMmTuSt8dZJO_sHBjQubv0Nns-b1373O7o'),
('sync_api_username','erickmosess'),
('sync_api_password','erick2030'),
('auto_sync','true'),
('dark_mode','false'),
('primary_color','#D97706'),
('backup_path','backups'),
('backup_time','23:00'),
('auto_print_receipt','true'),
('setup_complete','true'),
('last_successful_sync','2026-07-24 15:09:34');
