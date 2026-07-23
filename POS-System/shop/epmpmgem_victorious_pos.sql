-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Jul 22, 2026 at 11:42 PM
-- Server version: 8.4.10
-- PHP Version: 8.4.22

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `epmpmgem_victorious_pos`
--

-- --------------------------------------------------------

--
-- Table structure for table `app_settings`
--

CREATE TABLE `app_settings` (
  `key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` text COLLATE utf8mb4_unicode_ci,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `audit_logs`
--

CREATE TABLE `audit_logs` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `details` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `categories`
--

INSERT INTO `categories` (`id`, `name`, `description`, `sync_status`, `created_at`, `updated_at`, `deleted_at`) VALUES
('1ed51f2c-be00-4b35-a48c-e61c315e82a3', 'Household', NULL, 'PENDING', '2026-07-21 23:14:11', '2026-07-21 23:14:11', NULL),
('3e7701be-013e-49dd-ab72-708ab21cd095', 'General', NULL, 'PENDING', '2026-07-21 23:14:11', '2026-07-21 23:14:11', NULL),
('98439c44-dc22-432b-ac08-9ea35f80ee84', 'Food & Beverages', NULL, 'PENDING', '2026-07-21 23:14:11', '2026-07-21 23:14:11', NULL),
('bb9a2d3c-a3d9-4d54-9360-79bb88d725b6', 'Clothing', NULL, 'PENDING', '2026-07-21 23:14:11', '2026-07-21 23:14:11', NULL),
('f8514b41-41eb-4e0f-8086-c496d735e54f', 'Electronics', NULL, 'PENDING', '2026-07-21 23:14:11', '2026-07-21 23:14:11', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `customers`
--

CREATE TABLE `customers` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loyalty_points` int NOT NULL DEFAULT '0',
  `credit_balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `inventory_movements`
--

CREATE TABLE `inventory_movements` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` int NOT NULL DEFAULT '0',
  `reason` text COLLATE utf8mb4_unicode_ci,
  `batch_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `products`
--

CREATE TABLE `products` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `barcode` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qr_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sku` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buying_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `selling_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `wholesale_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `current_stock` int NOT NULL DEFAULT '0',
  `minimum_stock` int NOT NULL DEFAULT '0',
  `preferred_order_quantity` int NOT NULL DEFAULT '0',
  `tax_rate` decimal(6,2) NOT NULL DEFAULT '0.00',
  `discount` decimal(6,2) NOT NULL DEFAULT '0.00',
  `supplier_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `image_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pcs',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `track_expiry` tinyint(1) NOT NULL DEFAULT '0',
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `version` bigint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `products`
--

INSERT INTO `products` (`id`, `barcode`, `qr_code`, `sku`, `name`, `category_id`, `buying_price`, `selling_price`, `wholesale_price`, `current_stock`, `minimum_stock`, `preferred_order_quantity`, `tax_rate`, `discount`, `supplier_id`, `description`, `image_path`, `unit`, `status`, `track_expiry`, `sync_status`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES
('124830f7-888a-4521-a438-7ba03ca1bf51', NULL, NULL, 'kabras-123', 'sugar(1kg)', '98439c44-dc22-432b-ac08-9ea35f80ee84', 12.00, 14.00, 12.00, 34, 10, 12, 16.00, 0.00, '87e77414-71f2-4453-9382-4435634bf5f7', NULL, 'uploads/products/124830f7-888a-4521-a438-7ba03ca1bf51-0-4a569195379d.png', 'pcs', 'active', 0, 'PENDING', 1, '2026-07-23 01:52:01', '2026-07-23 01:52:01', NULL),
('92aaa3ab-782d-47ab-a43d-6ad479332c5b', NULL, NULL, 'kips2026', 'kips', '98439c44-dc22-432b-ac08-9ea35f80ee84', 12.00, 13.00, 12.00, 10, 4, 20, 16.00, 0.00, NULL, NULL, 'uploads/products/92aaa3ab-782d-47ab-a43d-6ad479332c5b-0-2260d0a8fabf.png;uploads/products/92aaa3ab-782d-47ab-a43d-6ad479332c5b-1-b7f3f1e2adee.png;uploads/products/92aaa3ab-782d-47ab-a43d-6ad479332c5b-2-e8a22d261c1a.png', 'pcs', 'active', 0, 'MODIFIED', 3, '2026-07-23 02:11:10', '2026-07-23 02:16:01', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `product_images`
--

CREATE TABLE `product_images` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `purchase_orders`
--

CREATE TABLE `purchase_orders` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `supplier_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `supplier_name` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORDERED',
  `expected_delivery_date` date DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `purchase_orders`
--

INSERT INTO `purchase_orders` (`id`, `supplier_id`, `supplier_name`, `status`, `expected_delivery_date`, `notes`, `sync_status`, `created_at`, `updated_at`, `deleted_at`) VALUES
('cf141dd9-0871-442c-acec-dfa0b3a2b23b', '87e77414-71f2-4453-9382-4435634bf5f7', 'bonface onduso', 'RECEIVED', '2026-07-29', NULL, 'MODIFIED', '2026-07-22 16:26:39', '2026-07-22 16:46:54', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `purchase_order_items`
--

CREATE TABLE `purchase_order_items` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `po_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ordered_qty` int NOT NULL DEFAULT '0',
  `received_qty` int NOT NULL DEFAULT '0',
  `buying_price` decimal(12,2) NOT NULL DEFAULT '0.00'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `sales`
--

CREATE TABLE `sales` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cashier_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cashier_name` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `tax_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `grand_total` decimal(12,2) NOT NULL DEFAULT '0.00',
  `payment_method` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CASH',
  `cash_tendered` decimal(12,2) NOT NULL DEFAULT '0.00',
  `change_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `payment_reference` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMPLETED',
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `sales`
--

INSERT INTO `sales` (`id`, `receipt_number`, `cashier_id`, `cashier_name`, `customer_id`, `subtotal`, `discount_amount`, `tax_amount`, `grand_total`, `payment_method`, `cash_tendered`, `change_amount`, `payment_reference`, `status`, `sync_status`, `created_at`, `updated_at`, `deleted_at`) VALUES
('5f3d1703-5e72-4118-9840-66ef6a3a4810', 'RCP-20260722-0007', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'erickmosess', NULL, 122.00, 0.00, 1.22, 123.22, 'MPESA', 123.22, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-22 23:56:38', '2026-07-22 23:56:38', NULL),
('ad935207-6174-4590-bb1e-b28a128bc8f6', 'RCP-20260722-0006', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'erickmosess', NULL, 96.00, 0.00, 15.36, 111.36, 'CASH', 111.36, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-22 21:24:50', '2026-07-22 21:24:50', NULL),
('b5696dda-8281-4e5e-a278-b89c34fab6b9', 'RCP-20260722-0002', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'Administrator', NULL, 4000.00, 0.00, 640.00, 4640.00, 'CASH', 4640.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-22 16:19:41', '2026-07-22 16:19:41', NULL),
('c1d95db4-2e96-4007-b219-90952067bdc6', 'RCP-20260722-0004', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'erickmosess', NULL, 390.00, 0.00, 62.40, 452.40, 'CASH', 452.40, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-22 21:22:37', '2026-07-22 21:22:37', NULL),
('c35e9b62-87f2-4331-9e39-25f11b288a64', 'RCP-20260722-0001', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'Administrator', NULL, 4000.00, 400.00, 576.00, 4176.00, 'CASH', 4176.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-22 16:18:36', '2026-07-22 16:18:36', NULL),
('d8749735-634d-4bb5-a1b0-446cb1692c22', 'RCP-20260722-0005', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'erickmosess', NULL, 2806.00, 0.00, 448.96, 3254.96, 'CASH', 3254.96, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-22 21:24:16', '2026-07-22 21:24:16', NULL),
('e413a932-e32f-4657-adfb-fdf815d205d8', 'RCP-20260723-0001', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'erickmosess', NULL, 4072.00, 0.00, 40.72, 4112.72, 'CASH', 4112.72, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-23 00:24:10', '2026-07-23 00:24:10', NULL),
('eafa144e-db34-4e40-8cac-a19e941a586b', 'RCP-20260722-0003', 'fe7c4dfb-ee55-417e-9f3a-444d86d94343', 'Administrator', NULL, 32000.00, 0.00, 5120.00, 37120.00, 'MPESA', 37120.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-22 16:21:55', '2026-07-22 16:21:55', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `sale_items`
--

CREATE TABLE `sale_items` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sale_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_sku` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `unit_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `buying_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `tax_rate` decimal(6,2) NOT NULL DEFAULT '0.00',
  `line_total` decimal(12,2) NOT NULL DEFAULT '0.00'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `suppliers`
--

CREATE TABLE `suppliers` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` text COLLATE utf8mb4_unicode_ci,
  `balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `suppliers`
--

INSERT INTO `suppliers` (`id`, `name`, `phone`, `email`, `address`, `balance`, `sync_status`, `created_at`, `updated_at`, `deleted_at`) VALUES
('87e77414-71f2-4453-9382-4435634bf5f7', 'bonface onduso', 'bonfaceobnduso9@gmail.com', 'Nakuru,kenya', NULL, 0.00, 'MODIFIED', '2026-07-22 16:11:58', '2026-07-22 23:43:35', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `suspended_carts`
--

CREATE TABLE `suspended_carts` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cashier_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `suspended_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CASHIER',
  `full_name` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `failed_login_attempts` int NOT NULL DEFAULT '0',
  `lockout_until` datetime DEFAULT NULL,
  `sync_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYNCED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  `store_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password_hash`, `role`, `full_name`, `active`, `failed_login_attempts`, `lockout_until`, `sync_status`, `created_at`, `updated_at`, `deleted_at`, `store_id`) VALUES
('62770cd8-861f-11f1-baf7-9c7bef765a9e', 'erickmosess', '$2y$10$WLYcRBB.eSzgzHw.TL4PG.QY/l0ei/nQts19XBuiauEo.93kzWltG', 'ADMIN', 'erickmosess', 1, 0, NULL, 'PENDING', '2026-07-21 23:14:10', '2026-07-23 01:49:28', NULL, NULL),
('a0000000-0000-0000-0000-000000000001', 'admin', '$2y$10$mTWMBmM5qj6ug./GDsXkUeRiQXKKCgfJfmHrO6I7PpvNLFgaCa/Qm', 'ADMIN', 'Administrator', 1, 0, NULL, 'SYNCED', '2026-07-23 01:08:39', '2026-07-23 01:49:40', NULL, NULL),
('a0000000-0000-0000-0000-000000000002', 'cashier', '$2y$12$KIB3WnRnJPMY2IyH9bMKSOzCBCGSGXp7ZLhd3Ln4LvjRn3qTrDr4C', 'CASHIER', 'Default Cashier', 1, 0, NULL, 'SYNCED', '2026-07-23 01:08:39', '2026-07-23 01:08:39', NULL, NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `app_settings`
--
ALTER TABLE `app_settings`
  ADD PRIMARY KEY (`key`);

--
-- Indexes for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_audit_user_id` (`user_id`),
  ADD KEY `idx_audit_event_type` (`event_type`),
  ADD KEY `idx_audit_created_at` (`created_at`);

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_categories_name` (`name`),
  ADD KEY `idx_categories_sync_status` (`sync_status`),
  ADD KEY `idx_categories_updated_at` (`updated_at`);

--
-- Indexes for table `customers`
--
ALTER TABLE `customers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_customers_name` (`name`),
  ADD KEY `idx_customers_phone` (`phone`),
  ADD KEY `idx_customers_email` (`email`),
  ADD KEY `idx_customers_sync_status` (`sync_status`),
  ADD KEY `idx_customers_updated_at` (`updated_at`);

--
-- Indexes for table `inventory_movements`
--
ALTER TABLE `inventory_movements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_inv_product_id` (`product_id`),
  ADD KEY `idx_inv_type` (`type`),
  ADD KEY `idx_inv_sync_status` (`sync_status`),
  ADD KEY `idx_inv_created_at` (`created_at`);

--
-- Indexes for table `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_products_sku` (`sku`),
  ADD KEY `idx_products_barcode` (`barcode`),
  ADD KEY `idx_products_qr_code` (`qr_code`),
  ADD KEY `idx_products_name` (`name`),
  ADD KEY `idx_products_category_id` (`category_id`),
  ADD KEY `idx_products_supplier_id` (`supplier_id`),
  ADD KEY `idx_products_sync_status` (`sync_status`),
  ADD KEY `idx_products_updated_at` (`updated_at`),
  ADD KEY `idx_products_status` (`status`);

--
-- Indexes for table `product_images`
--
ALTER TABLE `product_images`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_product_images_product_id` (`product_id`),
  ADD KEY `idx_product_images_sync_status` (`sync_status`),
  ADD KEY `idx_product_images_updated_at` (`updated_at`);

--
-- Indexes for table `purchase_orders`
--
ALTER TABLE `purchase_orders`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_po_supplier_id` (`supplier_id`),
  ADD KEY `idx_po_status` (`status`),
  ADD KEY `idx_po_sync_status` (`sync_status`),
  ADD KEY `idx_po_updated_at` (`updated_at`);

--
-- Indexes for table `purchase_order_items`
--
ALTER TABLE `purchase_order_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_poi_po_id` (`po_id`),
  ADD KEY `idx_poi_product_id` (`product_id`);

--
-- Indexes for table `sales`
--
ALTER TABLE `sales`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_sales_receipt` (`receipt_number`),
  ADD KEY `idx_sales_cashier_id` (`cashier_id`),
  ADD KEY `idx_sales_customer_id` (`customer_id`),
  ADD KEY `idx_sales_payment_method` (`payment_method`),
  ADD KEY `idx_sales_sync_status` (`sync_status`),
  ADD KEY `idx_sales_updated_at` (`updated_at`),
  ADD KEY `idx_sales_created_at` (`created_at`);

--
-- Indexes for table `sale_items`
--
ALTER TABLE `sale_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_sale_items_sale_id` (`sale_id`),
  ADD KEY `idx_sale_items_product_id` (`product_id`);

--
-- Indexes for table `suppliers`
--
ALTER TABLE `suppliers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_suppliers_name` (`name`),
  ADD KEY `idx_suppliers_sync_status` (`sync_status`),
  ADD KEY `idx_suppliers_updated_at` (`updated_at`);

--
-- Indexes for table `suspended_carts`
--
ALTER TABLE `suspended_carts`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_users_username` (`username`),
  ADD KEY `idx_users_role` (`role`),
  ADD KEY `idx_users_sync_status` (`sync_status`),
  ADD KEY `idx_users_updated_at` (`updated_at`);

--
-- Constraints for dumped tables
--

--
-- Constraints for table `inventory_movements`
--
ALTER TABLE `inventory_movements`
  ADD CONSTRAINT `fk_inv_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `products`
--
ALTER TABLE `products`
  ADD CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_products_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `product_images`
--
ALTER TABLE `product_images`
  ADD CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `purchase_orders`
--
ALTER TABLE `purchase_orders`
  ADD CONSTRAINT `fk_po_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `purchase_order_items`
--
ALTER TABLE `purchase_order_items`
  ADD CONSTRAINT `fk_poi_po` FOREIGN KEY (`po_id`) REFERENCES `purchase_orders` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_poi_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `sales`
--
ALTER TABLE `sales`
  ADD CONSTRAINT `fk_sales_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `sale_items`
--
ALTER TABLE `sale_items`
  ADD CONSTRAINT `fk_sale_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_sale_items_sale` FOREIGN KEY (`sale_id`) REFERENCES `sales` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
