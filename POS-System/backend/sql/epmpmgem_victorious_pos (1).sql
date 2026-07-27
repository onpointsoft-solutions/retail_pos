-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Jul 24, 2026 at 05:33 PM
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

INSERT INTO `categories` (`id`, `name`, `description`, `sync_status`, `created_at`, `updated_at`) VALUES
('7d817fac-4835-443e-a1a7-e907024cfef2', 'General', NULL, 'SYNCED', '2026-07-22 17:01:00', '2026-07-22 17:01:00'),
('1c69c01f-28f9-40ad-8905-c669107afe6b', 'Food & Beverages', NULL, 'SYNCED', '2026-07-22 17:01:00', '2026-07-22 17:01:00'),
('617a4610-d564-45ff-b6aa-30fbb73f9b1c', 'Electronics', NULL, 'SYNCED', '2026-07-22 17:01:00', '2026-07-22 17:01:00'),
('ba79ace6-3873-4925-89cd-a73ab9ccd215', 'Clothing', NULL, 'SYNCED', '2026-07-22 17:01:00', '2026-07-22 17:01:00'),
('b60c1464-b5c4-420b-964b-49fbea48c96b', 'Household', NULL, 'SYNCED', '2026-07-22 17:01:00', '2026-07-22 17:01:00'),
('1ed51f2c-be00-4b35-a48c-e61c315e82a3', 'Household', NULL, 'SYNCED', '2026-07-22 17:01:00', '2026-07-22 17:01:00'),
('bb9a2d3c-a3d9-4d54-9360-79bb88d725b6', 'Clothing', NULL, 'SYNCED', '2026-07-22 17:01:00', '2026-07-22 17:01:00'),
('2cbdaf4d-2a5c-40db-89b6-baea41b69dda', 'Clothing', NULL, 'SYNCED', '2026-07-23 17:54:06', '2026-07-23 17:54:06'),
('6e1814a1-9176-408f-b0cc-a9a92bc3c357', 'General', NULL, 'SYNCED', '2026-07-23 17:54:06', '2026-07-23 17:54:06'),
('929395f4-a7f5-4bbf-843c-96673a8a7bec', 'Household', NULL, 'SYNCED', '2026-07-23 17:54:06', '2026-07-23 17:54:06'),
('b57c01d7-5495-4fdb-983b-77668cd1c006', 'Food & Beverages', NULL, 'SYNCED', '2026-07-23 17:54:06', '2026-07-23 17:54:06'),
('c537aea9-f4ec-40c1-814e-da16ef76cc63', 'Electronics', NULL, 'SYNCED', '2026-07-23 17:54:06', '2026-07-23 17:54:06');


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
('703ce993-c0c6-4988-b761-0be633222e18', '6161113940134', '6161113940134', '6161113940134', 'Ajab Fortified All Purpose Home Baking Wheat Flour', '1c69c01f-28f9-40ad-8905-c669107afe6b', 90.00, 100.00, 100.00, 27, 10, 50, 0.00, 0.00, NULL, 'Fortified All Purpose Home Baking Flour', '/pos.victoriousgeneralshop.com/uploads/products/42b2db21-8847-4df4-80f0-c65066e54b8d.png;/pos.victoriousgeneralshop.com/uploads/products/21728c4a-b6b2-405a-93c4-c2ce951c98d7.png', 'packets', 'active', 1, 'SYNCED', 2, '2026-07-22 23:33:41', '2026-07-23 01:34:55', NULL),
('bfbf8bcc-41ab-46dd-baa8-3afa5ac0bb1c', '7171109930154', '7171109930079', '7171109930154', 'ASIS Premium Tangawizi-15G', '1c69c01f-28f9-40ad-8905-c669107afe6b', 5.00, 10.00, 10.00, 3, 20, 20, 0.00, 0.00, NULL, '15 g Asis High Quality Kenya Tea', '/pos.victoriousgeneralshop.com/uploads/products/2b681a62-1c53-45c4-9047-b051765785ab.png', 'sachets', 'active', 0, 'SYNCED', 3, '2026-07-22 23:42:10', '2026-07-23 15:46:16', NULL),
('c0ec287b-220d-467a-a2a6-ce52746ed6bf', '61614208', '61614208', '61614208', 'Vaseline BlueSeal Cocoa Butter 95g', '7d817fac-4835-443e-a1a7-e907024cfef2', 130.00, 140.00, 140.00, 12, 10, 20, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/fe4246a8-735e-4c3f-928b-7582daac9595.png', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-22 23:46:49', '2026-07-22 23:46:49', NULL),
('aac1f838-5ad0-402d-9c3b-105e3df34d22', '6161107774059', '6161107774059', '6161107774059', 'KIMBO 500G', '1c69c01f-28f9-40ad-8905-c669107afe6b', 165.00, 180.00, 180.00, 6, 15, 50, 0.00, 0.00, NULL, 'Pure vegetable fat', '/pos.victoriousgeneralshop.com/uploads/products/2c409c8e-3ee0-46d0-9ec7-7e3fb4438e02.png', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-22 23:52:12', '2026-07-22 23:52:12', NULL),
('d4186bf3-2178-4044-ade4-728a6dd6fc7b', '61611101660211', '61611101660211', '61611101660211', 'Tilly Cooking Fat', '1c69c01f-28f9-40ad-8905-c669107afe6b', 15.00, 20.00, 20.00, 27, 15, 30, 0.00, 0.00, NULL, 'Fortified Edible Vegetable Cooking Fat', '/pos.victoriousgeneralshop.com/uploads/products/6fce86c7-01c2-4368-8fe8-70958fc4523d.png', 'sachets', 'active', 0, 'MODIFIED', 1, '2026-07-22 23:58:50', '2026-07-23 19:31:45', NULL),
('126560a6-2315-44f3-bb4c-12528dc8b9b9', '8718951308213', '8718951308213', '8718951308213', 'Colgate', '7d817fac-4835-443e-a1a7-e907024cfef2', 30.00, 40.00, 40.00, 24, 10, 20, 0.00, 0.00, NULL, 'Colgate Maximum Cavity Protection', '/pos.victoriousgeneralshop.com/uploads/products/1906b4ed-f2bf-407a-8b37-2d69014ea048.jpg', 'sachets', 'active', 0, 'SYNCED', 1, '2026-07-23 00:08:06', '2026-07-23 00:08:06', NULL),
('6f36e781-fb79-4824-8513-a19c4d652a07', '6008677002277', '6008677002277', '6008677002277', 'Pure Glucose', '1c69c01f-28f9-40ad-8905-c669107afe6b', 20.00, 25.00, 25.00, 14, 10, 20, 0.00, 0.00, NULL, 'Pure glucose Instant energy', '/pos.victoriousgeneralshop.com/uploads/products/356fd0a9-6587-4842-83b8-12dde8ba26d4.jpg', 'boxes', 'active', 0, 'SYNCED', 1, '2026-07-23 00:16:51', '2026-07-23 00:16:51', NULL),
('b3843ebe-3392-4e56-b4e5-5f7620292172', '6164001283344', '6164001283344', '6164001283344', 'Barjot Office Glue 90gms', '7d817fac-4835-443e-a1a7-e907024cfef2', 30.00, 40.00, 40.00, 10, 5, 20, 0.00, 0.00, NULL, 'Barjot Office glue', '/pos.victoriousgeneralshop.com/uploads/products/0e18b136-c71e-41be-baa9-8d3f6cee6b6b.jpg', 'bottles', 'active', 0, 'SYNCED', 1, '2026-07-23 00:23:29', '2026-07-23 00:23:29', NULL),
('2c3a808c-7fb2-4b4a-8c10-3f983906721f', '6009611170274', '6009611170274', '6009611170274', 'Mara Moja', '1ed51f2c-be00-4b35-a48c-e61c315e82a3', 8.00, 10.00, 10.00, 30, 15, 30, 0.00, 0.00, NULL, 'Mara Moja Kama umeme', '/pos.victoriousgeneralshop.com/uploads/products/d16c29d7-e728-4d79-8592-853b021be386.png;/pos.victoriousgeneralshop.com/uploads/products/eeeccb4d-1617-4ef2-ba87-aeb21eaed29b.png', 'pairs', 'active', 0, 'SYNCED', 2, '2026-07-23 00:26:49', '2026-07-23 00:31:52', NULL),
('5b0635b0-07ea-42c4-8a05-550542d8f55b', '6161105711469', '6161105711469', '6161105711469', 'Kaluma Pain Palm', '7d817fac-4835-443e-a1a7-e907024cfef2', 45.00, 50.00, 50.00, 5, 15, 30, 0.00, 0.00, NULL, 'Kaluma Pain Palm', '/pos.victoriousgeneralshop.com/uploads/products/1a7f59f3-8aa7-439c-b8b6-a7a3f44701bc.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 00:29:37', '2026-07-23 00:29:37', NULL),
('24480d20-68c0-4532-bde2-f9fd388b9f22', '68686102525', '68686102525', '68686102525', 'Panadol Advanced', '7d817fac-4835-443e-a1a7-e907024cfef2', 15.00, 20.00, 20.00, 47, 20, 50, 0.00, 0.00, NULL, 'Panadol Advanced', '/pos.victoriousgeneralshop.com/uploads/products/b394b32e-b07b-40d8-9433-347b965d8165.jpg', 'pairs', 'active', 0, 'MODIFIED', 1, '2026-07-23 00:37:10', '2026-07-24 13:21:52', NULL),
('88e49a6f-c50f-4d7d-bd9b-dd9230e042fe', '6739225235815', '6739225235815', '6739225235815', 'Baida Lighter', 'b60c1464-b5c4-420b-964b-49fbea48c96b', 10.00, 30.00, 30.00, 47, 10, 50, 0.00, 0.00, NULL, 'Baida Lighter', '/pos.victoriousgeneralshop.com/uploads/products/fd961352-5b98-47a4-ab37-08b9afe5ed35.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 00:41:55', '2026-07-23 00:41:55', NULL),
('117fee71-adbe-40bd-9d62-17ab73f2bce7', '6161100882447', '6161100882447', '6161100882447', 'Valon Skin Care 95ml', '7d817fac-4835-443e-a1a7-e907024cfef2', 120.00, 130.00, 130.00, 7, 5, 20, 0.00, 0.00, NULL, 'Valon Skin Care', '/pos.victoriousgeneralshop.com/uploads/products/8efee5df-36a9-49d3-b1d4-7d8f2afcf220.jpg;/pos.victoriousgeneralshop.com/uploads/products/09059a9f-d3cc-4f55-a36a-2d77c600413e.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 00:51:55', '2026-07-23 00:51:55', NULL),
('96ab684c-b6fd-43a8-bda6-3be54992463b', '6008677006053', '6008677006053', '6008677006053', 'Arimis Milking Jelly 200ml', '7d817fac-4835-443e-a1a7-e907024cfef2', 85.00, 100.00, 100.00, 2, 5, 20, 0.00, 0.00, NULL, 'Arimis Milking Jelly', '/pos.victoriousgeneralshop.com/uploads/products/d2356575-0e27-4f34-aae5-4eee9b2da997.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 00:57:24', '2026-07-23 00:57:24', NULL),
('b2288b18-3a3b-4e36-8f82-9ea8b53bc256', '6164004192056', '6164004192056', '6164004192056', 'Laxmi Green Grams 500g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 80.00, 90.00, 90.00, 6, 5, 20, 0.00, 0.00, NULL, 'Laximi Green Grams', '/pos.victoriousgeneralshop.com/uploads/products/fed94015-158b-4cf3-a732-7d21ff71f18f.png', 'packets', 'active', 0, 'SYNCED', 2, '2026-07-23 01:05:23', '2026-07-23 01:06:04', NULL),
('0728ac39-e781-4a0f-b779-6bbd5da4dee9', '6164004676556', '6164004676556', '6164004676556', 'Blueband Spread for bread 100g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 53.00, 70.00, 65.00, 3, 5, 10, 0.00, 0.00, NULL, 'Blueband Low Fat Spread', '/pos.victoriousgeneralshop.com/uploads/products/1c631079-5e70-472a-b3fd-7ce70c5bf2d2.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 01:13:29', '2026-07-23 01:13:29', NULL),
('2cfaba05-01a8-4a8c-8ff8-03466526f694', '6614000', '6614000', '6614000', 'Blueband Original 100g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 60.00, 70.00, 65.00, 6, 5, 20, 0.00, 0.00, NULL, 'Blueband Original 100g', '/pos.victoriousgeneralshop.com/uploads/products/1957fd60-df3c-435a-828f-33e35c586773.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 01:18:41', '2026-07-23 01:18:41', NULL),
('c97fbd5a-7186-45bc-9d2d-feadb41b943b', '8719200286733', '8719200286733', '8719200286733', 'Blueband With Choco 100g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 60.00, 70.00, 65.00, 3, 5, 20, 0.00, 0.00, NULL, 'Blueband with choco', '/pos.victoriousgeneralshop.com/uploads/products/33304c0f-2eff-4327-bb20-fd9663e0738d.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 01:22:33', '2026-07-23 01:22:33', NULL),
('b2713b07-0067-4411-a5d6-114be39ad259', '6161113940127', '6161113940127', '6161113940127', 'Ajab Fortified All Purpose Home Baking Flour 500g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 50.00, 55.00, 55.00, 15, 10, 20, 0.00, 0.00, NULL, 'Ajab Fortified All Purpose Home Baking Wheat Flour', '/pos.victoriousgeneralshop.com/uploads/products/0fd57d11-dff3-4d95-9b26-a0ce312b9468.png', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 01:34:11', '2026-07-24 16:50:25', NULL),
('78c19486-8d62-46a0-8b8d-30f14375f64e', '6161106962662', '6161106962662', '6161106962662', 'Nescafe Classic 1.5 g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 8.00, 10.00, 10.00, 20, 10, 30, 0.00, 0.00, NULL, 'Nescafe Classic 1.5g', '/pos.victoriousgeneralshop.com/uploads/products/3a7495d8-c305-4dfa-bd81-5f583d811f1f.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 01:40:03', '2026-07-23 19:31:45', NULL),
('ad049a23-82cd-45cb-a2cb-35ad2999c562', '6161100460126', '6161100460126', '6161100460126', 'KCC FRESH WHOLE MILK 200ML', '1c69c01f-28f9-40ad-8905-c669107afe6b', 30.00, 35.00, 35.00, 4, 5, 20, 0.00, 0.00, NULL, 'KCC FRESH WHOLE MILK', '/pos.victoriousgeneralshop.com/uploads/products/43000e6b-add3-4d49-b80c-6e3c03968df0.jpg', 'packets', 'active', 0, 'MODIFIED', 1, '2026-07-23 01:44:52', '2026-07-24 15:35:53', NULL),
('ffdfd257-8190-4423-8337-9cc399a50320', '6161109980861', '6161109980861', '6161109980861', 'Fresha Maisha Long Life Whole Milk 200ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 30.00, 35.00, 35.00, 6, 5, 20, 0.00, 0.00, NULL, 'Maisha Long Life Whole Milk', '/pos.victoriousgeneralshop.com/uploads/products/c8c2b7ff-1bad-4b24-94e3-8e621f04021e.jpg', 'packets', 'active', 0, 'SYNCED', 1, '2026-07-23 01:48:21', '2026-07-23 01:48:21', NULL),
('cbc73ad8-8919-4840-88ad-91693f44e42f', '6161100907140', '6161100907140', '6161100907140', 'Menendazi Pure Baking Powder', '1c69c01f-28f9-40ad-8905-c669107afe6b', 30.00, 40.00, 40.00, 6, 5, 10, 0.00, 0.00, NULL, 'Menendazi Pure Baking Powder', '/pos.victoriousgeneralshop.com/uploads/products/3d6544bb-7089-424b-a145-6c891ac50248.jpg', 'packets', 'active', 0, 'SYNCED', 1, '2026-07-23 01:53:50', '2026-07-23 01:53:50', NULL),
('13fa49d0-9946-4d57-9747-fbe0726d435c', '6161100460119', '6161100460119', '6161100460119', 'KCC Fresh Whole Milk 500g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 70.00, 75.00, 75.00, 12, 10, 20, 0.00, 0.00, NULL, 'KCC Fresh Milk', '/pos.victoriousgeneralshop.com/uploads/products/11060e26-7a1b-45fe-b27f-7beda9407533.jpeg', 'packets', 'active', 0, 'SYNCED', 1, '2026-07-23 01:58:01', '2026-07-23 01:58:01', NULL),
('05f2201b-f789-4dce-8dc7-dcef04f4b36c', '16100100077', '16100100077', '16100100077', 'Mount Kenya Milk 500ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 70.00, 75.00, 75.00, 10, 5, 20, 0.00, 0.00, NULL, 'Mount Kenya Milk Dairy Kubwa Choice', NULL, 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 02:02:51', '2026-07-23 02:02:51', NULL),
('91571c23-8e2c-4ac1-8c50-c907b8d74121', '6940988800206', '6940988800206', '6940988800206', 'Cyano Acrylate Adhesive super glue', '7d817fac-4835-443e-a1a7-e907024cfef2', 30.00, 35.00, 35.00, 10, 10, 20, 0.00, 0.00, NULL, 'Acrylate Cyano Adhesice Super Glue', '/pos.victoriousgeneralshop.com/uploads/products/f64c3002-49bb-41c4-8bf5-8d178921e342.jpg', 'pcs', 'active', 0, 'MODIFIED', 2, '2026-07-23 02:07:28', '2026-07-24 15:31:17', NULL),
('2d3a0fbe-014b-4230-81ae-ffc76eea1a4f', '616110571131', '616110571131', '616110571131', 'Kaluma Strong', '7d817fac-4835-443e-a1a7-e907024cfef2', 8.00, 10.00, 10.00, 9, 5, 20, 0.00, 16.00, NULL, 'Kaluma Strong', '/pos.victoriousgeneralshop.com/uploads/products/8c5c0194-1816-4510-a0ce-d37cfea6a49a.png', 'pairs', 'active', 0, 'MODIFIED', 2, '2026-07-23 02:09:50', '2026-07-23 19:24:21', NULL),
('219d5be6-9c47-43b1-b5f4-4b7082cab4fd', '6161100907003', '6161100907003', '6161100907003', 'Menangai Cream Quality Washing Bar 800g', '7d817fac-4835-443e-a1a7-e907024cfef2', 45.00, 50.00, 50.00, 20, 10, 20, 0.00, 0.00, NULL, 'Quality Washing Bar Soap', '/pos.victoriousgeneralshop.com/uploads/products/0a191d5f-c22e-43b7-8097-e303afa8c1d1.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 03:55:46', '2026-07-23 03:55:46', NULL),
('1df8138b-5d43-4e7e-8237-69eb273b6681', '6161100907034', '6161100907034', '6161100907034', 'Menengai Cream Quality Washing Bar 1kg', '7d817fac-4835-443e-a1a7-e907024cfef2', 45.00, 50.00, 50.00, 41, 10, 20, 0.00, 0.00, NULL, 'Quality Washing Bar Soap', '/pos.victoriousgeneralshop.com/uploads/products/16d352dd-f293-483c-8228-0059cfc40ddb.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 03:58:56', '2026-07-24 14:30:52', NULL),
('87fc7dd9-ffc7-4974-979f-1861bdce5f29', '6161110130835', '6161110130835', '6161110130835', 'ZENTA Multipurpose Washing Bar 800 g', '7d817fac-4835-443e-a1a7-e907024cfef2', 30.00, 40.00, 40.00, 52, 20, 50, 0.00, 0.00, NULL, 'Zenta Multipurpose Washing Bar', '/pos.victoriousgeneralshop.com/uploads/products/7c68a572-b056-4d6a-926d-eb5c59993fe1.jpg', 'pcs', 'active', 0, 'SYNCED', 2, '2026-07-23 04:05:09', '2026-07-23 04:08:52', NULL),
('c2c5f064-09de-427d-bd9c-228527dde863', '6161110130675', '6161110130675', '6161110130675', 'Zenta Multipurpose Washing Bar 1kg', '7d817fac-4835-443e-a1a7-e907024cfef2', 38.00, 50.00, 50.00, 25, 10, 50, 0.00, 0.00, NULL, 'Zenta Multi purpose washing soap 1 kg', '/pos.victoriousgeneralshop.com/uploads/products/df2e0cde-719c-43d2-a244-cb456d4eecbb.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 04:08:28', '2026-07-23 04:08:28', NULL),
('e63744cd-09e1-4a40-82cf-75e76647f7b7', '6161101667111', '6161101667111', '6161101667111', 'Jamaa White Washing Bar', '7d817fac-4835-443e-a1a7-e907024cfef2', 30.00, 40.00, 40.00, 3, 10, 30, 0.00, 0.00, NULL, 'Jamaa Washing Bar', '/pos.victoriousgeneralshop.com/uploads/products/48693e41-0214-45b3-8888-a9c1d0921e00.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 04:12:26', '2026-07-24 15:31:17', NULL),
('4b78edeb-402a-4abc-9bba-92ba059cb22e', '09607673321', '09607673321', '09607673321', 'Kifaru Safety Matches', 'ba79ace6-3873-4925-89cd-a73ab9ccd215', 4.00, 5.00, 5.00, 114, 20, 150, 0.00, 0.00, NULL, 'Kifaru Safety Matches', '/pos.victoriousgeneralshop.com/uploads/products/d8c7fabe-3c1f-4d78-bbde-d32f09e40bb4.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 04:16:24', '2026-07-23 04:16:24', NULL),
('565c4288-b7ed-4c23-9e01-9294e1e98279', '6164000015106', '6164000015106', '6164000015106', 'Tobex Bleach 70 ml', '7d817fac-4835-443e-a1a7-e907024cfef2', 30.00, 35.00, 35.00, 2, 5, 20, 0.00, 0.00, NULL, 'Topex Bleach 70ml', '/pos.victoriousgeneralshop.com/uploads/products/58d7c05e-37b0-458c-9259-3f8ad9863f2e.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 04:19:48', '2026-07-24 15:31:17', NULL),
('61d42fb4-9c5a-4501-9233-a784707e13fe', '6164004605426', '6164004605426', '6164004605426', 'Eno Fruit Salt', 'ba79ace6-3873-4925-89cd-a73ab9ccd215', 15.00, 20.00, 20.00, 39, 10, 50, 0.00, 0.00, NULL, 'Eno Fruit Salt Works on six symptoms of heart burn', '/pos.victoriousgeneralshop.com/uploads/products/05bbf55e-bd9e-45a7-9cb2-d5bb1f99245d.jpg', 'sachets', 'active', 0, 'SYNCED', 1, '2026-07-23 04:25:29', '2026-07-23 04:25:29', NULL),
('4920d20d-5ffd-40c9-89cd-d38f2427b222', '6161101571517', '6161101571517', '6161101571517', 'White Dent Advanced ToothBrush', '7d817fac-4835-443e-a1a7-e907024cfef2', 50.00, 60.00, 60.00, 9, 10, 20, 0.00, 0.00, NULL, 'WhiteDent Advanced ToothBrush', '/pos.victoriousgeneralshop.com/uploads/products/a7c1a7df-62bb-486d-b951-08efb5320a54.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 04:29:27', '2026-07-23 04:29:27', NULL),
('80b3844a-fce8-4d3f-a16b-10e3dc2725c0', '5060608740253', '5060608740253', '5060608740253', 'Predator Energy Drink', '1c69c01f-28f9-40ad-8905-c669107afe6b', 68.00, 70.00, 70.00, 17, 10, 30, 0.00, 0.00, NULL, 'Predator Energy Drink', '/pos.victoriousgeneralshop.com/uploads/products/a1d45a05-a442-4e45-ac80-a59fd6dadb71.jpg', 'bottles', 'active', 0, 'SYNCED', 1, '2026-07-23 04:33:45', '2026-07-23 04:33:45', NULL),
('df03622c-eeab-4539-8175-097a390a2514', '6971663563420', '6971663563420', '6971663563420', 'OLEMOL TOOTH BRUH', '7d817fac-4835-443e-a1a7-e907024cfef2', 35.00, 40.00, 40.00, 12, 5, 20, 0.00, 0.00, NULL, 'OLEMOL TOOTHBRUSH', '/pos.victoriousgeneralshop.com/uploads/products/19d4ea0e-e410-445b-9900-27a7f0c65f59.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 04:37:00', '2026-07-23 04:37:00', NULL),
('822c3d74-e988-4c99-b211-b4414304ceee', '6161101830010', '6161101830010', '6161101830010', 'TP Tishu Poa 100 X 125 mm', NULL, 18.00, 20.00, 20.00, 12, 5, 20, 0.00, 0.00, NULL, 'TP Tishu Poa', '/pos.victoriousgeneralshop.com/uploads/products/822c3d74-e988-4c99-b211-b4414304ceee-0-f4ed28a56741.jpg', 'rolls', 'active', 0, 'SYNCED', 3, '2026-07-23 04:42:36', '2026-07-23 17:40:02', '2026-07-23 17:40:02'),
('3d0e94d4-fdde-4406-a4a0-88c67af1484e', '0762497597547', '0762497597547', '0762497597547', 'KWETU Softness You Can Trust', '7d817fac-4835-443e-a1a7-e907024cfef2', 18.00, 20.00, 20.00, 1, 10, 20, 0.00, 0.00, NULL, 'Kwetu Softness You Can Trust', '/pos.victoriousgeneralshop.com/uploads/products/b94e0668-cb4c-43c0-9987-faff07120091.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 04:48:47', '2026-07-23 04:48:47', NULL),
('9ae586d9-73fa-47a1-8b66-2c99c46c3173', '6164002695474', '6164002695474', '6164002695474', 'Mfalme Tissue', '7d817fac-4835-443e-a1a7-e907024cfef2', 18.00, 20.00, 20.00, 3, 5, 20, 0.00, 0.00, NULL, 'Mfalme Tissue', '/pos.victoriousgeneralshop.com/uploads/products/26b3e0ad-a109-40e7-b9d3-96a42783987c.jpg', 'pcs', 'active', 0, 'MODIFIED', 2, '2026-07-23 04:51:45', '2026-07-24 18:09:19', NULL),
('28b8bd45-61d1-473e-ae5d-8af659b94c04', '0735745028520', '0735745028520', '0735745028520', 'Tuffy Feel The Softness 100mm X 125 mm', '7d817fac-4835-443e-a1a7-e907024cfef2', 18.00, 20.00, 20.00, 6, 5, 20, 0.00, 0.00, NULL, 'Feel the softness', '/pos.victoriousgeneralshop.com/uploads/products/18deb5f9-1c44-4a8c-acc7-13e1a3da9f1d.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 04:56:17', '2026-07-23 04:56:17', NULL),
('151b9723-7b8a-4390-8194-6e5a3878cc80', '0792382597352', '0792382597352', '0792382597352', 'Dalia Luxurious Soft White Tissue', '617a4610-d564-45ff-b6aa-30fbb73f9b1c', 18.00, 20.00, 20.00, 10, 10, 20, 0.00, 0.00, NULL, 'Dalia Luxurious sof white tissue', '/pos.victoriousgeneralshop.com/uploads/products/1e2bdf2c-5eed-4457-bb70-355fb4c0d0c0.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 14:58:21', '2026-07-24 14:49:16', NULL),
('82961b1e-6433-4cf6-83e3-110dad731396', '6009614480462', '6009614480462', '6009614480462', 'Rosy Extra Strong Tissue white Colour', NULL, 18.00, 20.00, 20.00, 4, 10, 20, 0.00, 0.00, NULL, 'Rosy Extra Strong Tissue', '/pos.victoriousgeneralshop.com/uploads/products/82961b1e-6433-4cf6-83e3-110dad731396-0-a9646fa71f29.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 15:05:30', '2026-07-23 17:40:02', '2026-07-23 17:40:02'),
('06a0da23-fd56-43b4-818e-707c0837f3f3', '6161100762411', '6161100762411', '6161100762411', 'Rosy Extra Strong Tissue Pink color', 'bb9a2d3c-a3d9-4d54-9360-79bb88d725b6', 18.00, 20.00, 20.00, 22, 10, 20, 0.00, 0.00, NULL, 'Rosy Extra Strong Tissue', '/pos.victoriousgeneralshop.com/uploads/products/59317cf7-fd78-4833-bf20-a28d81952d32.jpg', 'rolls', 'active', 0, 'MODIFIED', 1, '2026-07-23 15:09:34', '2026-07-24 12:27:29', NULL),
('12be2e1f-71b1-45dc-9e3c-4deadc38b96c', '6161101280037', '6161101280037', '6161101280037', 'KENSALT  Iodated Edible Table Salt 200g', '7d817fac-4835-443e-a1a7-e907024cfef2', 10.00, 15.00, 15.00, 59, 10, 20, 0.00, 0.00, NULL, 'Kensalt Iodate Edible Table Salt', '/pos.victoriousgeneralshop.com/uploads/products/863a0889-a34e-4096-9811-d0fd61a9ef57.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 15:58:26', '2026-07-24 16:50:25', NULL),
('e0101a7a-d845-408c-811b-5b9f967a984d', '6161101280013', '6161101280013', '6161101280013', 'Kensalt 500g', '1c69c01f-28f9-40ad-8905-c669107afe6b', 20.00, 25.00, 25.00, 53, 20, 30, 0.00, 0.00, NULL, 'iodated edible table salt', '/pos.victoriousgeneralshop.com/uploads/products/ff53cb88-93cc-4868-92c1-b245e3b84cf8.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 16:43:45', '2026-07-23 16:43:45', NULL),
('3b0d7323-d7d5-45e4-8429-73abb4ad78f7', '40822921', '40822921', '40822921', 'Fanta-Orange Soda  300ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 33.00, 50.00, 36.00, 12, 2, 7, 0.00, 0.00, NULL, 'fanta orange', '/pos.victoriousgeneralshop.com/uploads/products/249cc5c2-f4c8-4660-9756-3e7c97f13f1f.jpg', 'pcs', 'active', 0, 'SYNCED', 2, '2026-07-23 17:08:22', '2026-07-23 17:15:49', NULL),
('f975304d-916e-48a4-8aa2-0d66fcf3385a', '87126037', '87126037', '87126037', 'Coke soda 300 ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 33.00, 50.00, 36.00, 15, 4, 7, 0.00, 0.00, NULL, 'most popular soda', '/pos.victoriousgeneralshop.com/uploads/products/306d79cf-01c3-42b6-b566-d062f292d03f.jpg', 'pcs', 'active', 0, 'SYNCED', 2, '2026-07-23 17:14:52', '2026-07-23 18:27:11', NULL),
('353914f2-3fed-48cb-bfd6-a3e179c9593b', '90495090', '90495090', '90495090', 'Fanta-Orange Soda  200ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 15.00, 25.00, 20.00, 14, 10, 24, 0.00, 0.00, NULL, 'classic 200ml Fanta Orange soda', '/pos.victoriousgeneralshop.com/uploads/products/20bb4c93-c7ba-49fb-9c3c-f6a7e66dfbc7.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 17:20:16', '2026-07-24 15:31:17', NULL),
('e05ba1a5-7ec9-493a-9634-8268c241871b', 'Ana/1/4', 'Ana/1/4', 'Ana/1/4', 'Anab Black Rice 1/4kg', '1c69c01f-28f9-40ad-8905-c669107afe6b', 38.00, 45.00, 43.00, 20, 5, 20, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/357cbb9f-ff0c-4dfa-bf78-fd369eb12e36.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 17:33:00', '2026-07-23 17:33:00', NULL),
('8f82ae51-b4de-4c33-803e-c0ddfcdd59fb', 'Ana/1/2', 'Ana/1/2', 'Ana/1/2', 'Anab Black Rice 1/2kg', '1c69c01f-28f9-40ad-8905-c669107afe6b', 76.00, 85.00, 85.00, 10, 10, 20, 0.00, 0.00, NULL, 'Nutritious Whole Grain', '/pos.victoriousgeneralshop.com/uploads/products/413fd8d1-b1c9-4906-928d-131e79d00a4e.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 17:44:00', '2026-07-23 17:44:00', NULL),
('d09b6727-f43d-4f6c-90b5-e83622ccba88', 'Ana/1', 'Ana/1', 'Ana/1', 'Anab Black Rice 1kg', '1c69c01f-28f9-40ad-8905-c669107afe6b', 150.00, 165.00, 160.00, 10, 5, 10, 0.00, 0.00, NULL, 'Healthy Ancient Grain', '/pos.victoriousgeneralshop.com/uploads/products/bb1e2495-ee68-40a6-a8fe-5aabe3d6a2e0.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 17:47:02', '2026-07-23 17:47:02', NULL),
('cf15b13f-bc32-485c-a946-9bac5fa5a504', 'Kan/1/4', 'Kan/1/4', 'Kan/1/4', 'Kangore Rice 1/4kg', '1c69c01f-28f9-40ad-8905-c669107afe6b', 23.00, 30.00, 30.00, 20, 10, 20, 0.00, 0.00, NULL, 'Naturally Aromatic Rice', '/pos.victoriousgeneralshop.com/uploads/products/e4d2047e-5d9c-4f33-bf03-7d72ff6d3117.jpg', 'pcs', 'active', 0, 'SYNCED', 2, '2026-07-23 17:52:13', '2026-07-23 17:52:58', NULL),
('b38b209d-00a5-4592-bf9c-d209f1646a95', 'Kan/1/2', 'Kan/1/2', 'Kan/1/2', 'Kangore Rice 1/2kg', '1c69c01f-28f9-40ad-8905-c669107afe6b', 46.00, 60.00, 60.00, 10, 0, 0, 0.00, 0.00, NULL, 'Naturally Aromatic Rice', '/pos.victoriousgeneralshop.com/uploads/products/2b9e97e3-6588-43a9-ad28-391d2d918123.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 17:56:00', '2026-07-23 17:56:00', NULL),
('7b221a0d-8e5f-4e02-9af9-206d6e38848e', 'kan/1', 'kan/1', 'kan/1', 'Kangore Rice 1kg', '1c69c01f-28f9-40ad-8905-c669107afe6b', 87.00, 120.00, 120.00, 10, 5, 10, 0.00, 0.00, NULL, 'Naturally Aromatic Rice', '/pos.victoriousgeneralshop.com/uploads/products/08b8a512-ac7d-44e9-839d-3e7a5974e451.jpg', 'pcs', 'active', 0, 'SYNCED', 2, '2026-07-23 17:58:00', '2026-07-23 17:58:16', NULL),
('eb289aa2-c672-4d07-89ba-ca4fc7b56d2e', '5449000004840', '5449000004840', '5449000004840', 'Fanta-Orange 2L Soda', '1c69c01f-28f9-40ad-8905-c669107afe6b', 177.00, 200.00, 195.00, 11, 3, 6, 0.00, 16.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/88c29e1c-5812-472a-b985-6ad05b567942.jpg', 'pcs', 'active', 0, 'SYNCED', 2, '2026-07-23 18:07:16', '2026-07-23 18:43:13', NULL),
('15ee0340-0336-47c7-876b-bfbe4fbc1ae0', '5449000090096', '5449000090096', '5449000090096', 'Fanta-Passion Soda 2L', '1c69c01f-28f9-40ad-8905-c669107afe6b', 177.00, 200.00, 195.00, 6, 3, 6, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/17840a14-98f1-43df-8290-6f5d76e3edf3.jpg', 'pcs', 'active', 0, 'SYNCED', 3, '2026-07-23 18:11:23', '2026-07-23 18:42:47', NULL),
('4618b6b1-f906-4980-909a-35a8bc9e6857', '544900022752', '544900022752', '544900022752', 'Fanta Black-Current Soda 2L', '1c69c01f-28f9-40ad-8905-c669107afe6b', 177.00, 200.00, 195.00, 1, 3, 6, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/62b0a7d7-0063-42a1-90b3-139aac6f0268.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 18:16:49', '2026-07-23 18:16:49', NULL),
('724a63a9-11cc-4ab7-890c-0b72766707ad', '5449000000286', '5449000000286', '5449000000286', 'Coke Soda 2L', '1c69c01f-28f9-40ad-8905-c669107afe6b', 177.00, 200.00, 195.00, 3, 3, 6, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/d82acd2b-d17c-41c6-925f-eb531831be77.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 18:20:54', '2026-07-23 18:20:54', NULL),
('4c0ace44-b8c0-4bdb-9d87-57430dc46d82', '5449000054227', '5449000054227', '5449000054227', 'Coke Soda 1L', '1c69c01f-28f9-40ad-8905-c669107afe6b', 88.00, 100.00, 95.00, 4, 3, 3, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/e067f8d1-3316-4041-8913-7a006057e1ad.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 18:26:44', '2026-07-23 18:26:44', NULL),
('5b075ce4-082f-4d95-84e5-90652c8e13ee', '5449000084460', '5449000084460', '5449000084460', 'Fanta Black-Current Soda 1L', '1c69c01f-28f9-40ad-8905-c669107afe6b', 88.00, 100.00, 95.00, 4, 3, 6, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/428d2ff9-a4d3-4b46-9e31-2bfbfca0632e.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 18:30:01', '2026-07-23 18:30:01', NULL),
('d14e4361-dae2-4ccc-8284-bb7c370fa32d', '50112173', '50112173', '50112173', 'Fanta - Orange Soda 500ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 50.00, 60.00, 60.00, 14, 5, 10, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/9830c297-4c07-48a3-979e-56c5b340938d.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 18:37:03', '2026-07-24 14:28:04', NULL),
('3a66f17f-2d9e-449e-be9e-44e9081d8944', '42117131', '42117131', '42117131', 'Coke soda 350 ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 40.00, 50.00, 50.00, 2, 1, 10, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/26d3db40-13c5-4f3b-bd52-7ae0a6e9a03d.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 18:41:27', '2026-07-23 18:41:27', NULL),
('e84650a6-7ea8-421c-9b54-d2355e14bed9', '54490123', '54490123', '54490123', 'Coke Soda 500ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 50.00, 60.00, 60.00, 12, 5, 12, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/0138a91a-71b1-4341-b38e-372166019d0b.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 18:47:56', '2026-07-23 18:47:56', NULL),
('625f2f14-b4c5-4dbb-8747-5a976def1525', '54491182', '54491182', '54491182', 'Krest Soda 500ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 50.00, 60.00, 60.00, 5, 2, 4, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/bfcafd92-028c-4e93-a934-c8352fafb6b1.jpg', 'pcs', 'active', 0, 'SYNCED', 2, '2026-07-23 18:51:50', '2026-07-23 18:52:42', NULL),
('0da97229-8715-4401-868e-85cae1fb677f', '50112174', '50112174', '50112174', 'Fanta Black-Current Soda 500ml', NULL, 50.00, 60.00, 60.00, 3, 1, 3, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/9fa6cd6b-6d56-4647-bb27-eb692e6f35a3.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 19:02:26', '2026-07-23 19:02:26', NULL),
('711bb791-b492-4240-b2ca-631a9446f8a1', '90492112', '90492112', '90492112', 'krest Soda 300 ml', NULL, 40.00, 50.00, 50.00, 20, 5, 10, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/a0801e7e-122b-42e9-805f-3ad2e6a0abe4.jpg', 'pcs', 'active', 0, 'SYNCED', 1, '2026-07-23 19:06:21', '2026-07-23 19:06:21', NULL),
('e082b326-cf02-4226-8a83-48fb91da8cfd', '54492691', '54492691', '54492691', 'Sprite soda 300ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 40.00, 50.00, 50.00, 7, 5, 5, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/64425b6e-6ca0-4e0f-8667-6e1ec2fda80b.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 19:12:24', '2026-07-24 14:36:59', NULL),
('a4877599-2bca-4e6f-8833-c25a4a9b134e', '40822924', '40822924', '40822924', 'Fanta-passion Soda  300ml', '1c69c01f-28f9-40ad-8905-c669107afe6b', 40.00, 50.00, 50.00, 6, 2, 5, 0.00, 0.00, NULL, NULL, '/pos.victoriousgeneralshop.com/uploads/products/dee207e4-8cdc-432e-a03f-ce30954782d2.jpg', 'pcs', 'active', 0, 'MODIFIED', 1, '2026-07-23 19:17:25', '2026-07-24 12:26:13', NULL),
('9f7bbdf0-fd2b-4761-b4db-88d1e9238870', 'test123', 'test123', 'test123', 'test', 'b57c01d7-5495-4fdb-983b-77668cd1c006', 20.00, 24.00, 20.00, 20, 10, 20, 16.00, 0.00, NULL, 'good', 'C:\\Users\\Victorious\\AppData\\Local\\RetailPOS\\images\\9f7bbdf0-fd2b-4761-b4db-88d1e9238870-0-2260d0a8fabf.png', 'pcs', 'active', 0, 'DELETED', 1, '2026-07-24 16:02:04', '2026-07-24 17:19:57', '2026-07-24 17:19:57');

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

INSERT INTO `sales` (`id`, `receipt_number`, `cashier_id`, `cashier_name`, `customer_id`, `subtotal`, `discount_amount`, `tax_amount`, `grand_total`, `payment_method`, `cash_tendered`, `change_amount`, `payment_reference`, `status`, `sync_status`, `created_at`, `updated_at`) VALUES
('01f045a4-f9df-45ef-9dad-8eeb190c42ef', 'RCP-20260723-0003', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 140.00, 0.00, 0.00, 140.00, 'CASH', 140.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-23 20:38:50', '2026-07-23 20:38:50'),
('1ff28d1d-6631-4827-924e-a1b1ee5ad1bc', 'RCP-20260724-0001', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 50.00, 0.00, 0.00, 50.00, 'MPESA', 50.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 12:26:13', '2026-07-24 12:26:13'),
('23b6388c-8e53-455e-8f5f-52bf3f4f0e97', 'RCP-20260724-0013', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 20.00, 0.00, 0.00, 20.00, 'CASH', 20.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 18:41:51', '2026-07-24 18:41:51'),
('3d8e9bff-9299-4711-823d-3783089bb1a7', 'RCP-20260724-0011', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 70.00, 0.00, 0.00, 70.00, 'CASH', 70.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 16:50:25', '2026-07-24 16:50:25'),
('3f8c5ec2-00e3-4dff-b8b4-24c079a2efbc', 'RCP-20260724-0007', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 50.00, 0.00, 0.00, 50.00, 'CASH', 50.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 14:36:59', '2026-07-24 14:36:59'),
('47131f5f-41b4-4c8c-a123-36bf500462ab', 'RCP-20260723-0004', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 200.00, 0.00, 0.00, 200.00, 'MPESA', 200.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-23 21:20:45', '2026-07-23 21:20:45'),
('54a06e2f-d12f-4df2-8477-70dfa438cd11', 'RCP-20260724-0003', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 25.00, 0.00, 0.00, 25.00, 'MPESA', 25.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 12:53:24', '2026-07-24 12:53:24'),
('89553478-c9d5-4cf7-bf16-0f9e0fb9d719', 'RCP-20260724-0009', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 240.00, 0.00, 0.00, 240.00, 'CASH', 240.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 15:31:17', '2026-07-24 15:31:17'),
('8c37d3b5-023b-4e38-ad96-cab41404dbe6', 'RCP-20260724-0002', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 40.00, 0.00, 0.00, 40.00, 'MPESA', 40.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 12:27:29', '2026-07-24 12:27:29'),
('98faceb0-2506-42da-9ab0-93c50bd34484', 'RCP-20260724-0015', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 10.00, 0.00, 0.00, 10.00, 'MPESA', 10.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 20:27:34', '2026-07-24 20:27:34'),
('a2dadacb-1351-45bf-9b70-f2166eebb2ad', 'RCP-20260724-0012', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 25.00, 0.00, 0.00, 25.00, 'MPESA', 25.00, 0.00, 'UG02DOQB26', 'COMPLETED', 'PENDING', '2026-07-24 18:19:17', '2026-07-24 18:19:17'),
('bd82c0c4-ea21-4f40-bdb0-699f35854434', 'RCP-20260724-0004', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 20.00, 0.00, 0.00, 20.00, 'MPESA', 20.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 13:21:52', '2026-07-24 13:21:52'),
('c9f7c97c-b037-49ec-99f0-0d43fef75e08', 'RCP-20260724-0008', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 20.00, 0.00, 0.00, 20.00, 'MPESA', 20.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 14:49:16', '2026-07-24 14:49:16'),
('d3655701-c4f0-4c3a-a710-729a50f317f3', 'RCP-20260724-0006', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 50.00, 0.00, 0.00, 50.00, 'MPESA', 50.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 14:30:52', '2026-07-24 14:30:52'),
('ded571ff-c92e-475a-8e8f-a3be914a31c2', 'RCP-20260723-0002', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 30.00, 0.00, 0.00, 30.00, 'MPESA', 30.00, 0.00, 'UGNT5N0CMNT', 'COMPLETED', 'PENDING', '2026-07-23 19:31:46', '2026-07-23 19:31:46'),
('e2754f55-6e5a-4e16-aa57-05c049fe6e15', 'RCP-20260723-0001', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 10.00, 0.00, 0.00, 10.00, 'MPESA', 10.00, 0.00, 'UGN7VORL9M', 'COMPLETED', 'PENDING', '2026-07-23 19:24:21', '2026-07-23 19:24:21'),
('e3b98492-e948-4cec-8dfa-6402c73508f4', 'RCP-20260724-0014', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 35.00, 0.00, 0.00, 35.00, 'CASH', 35.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 18:42:16', '2026-07-24 18:42:16'),
('ec7ae38b-00ff-4d47-a9f0-540e354bd711', 'RCP-20260724-0010', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 35.00, 0.00, 0.00, 35.00, 'MPESA', 35.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 15:35:53', '2026-07-24 15:35:53'),
('f7d6495b-f11c-4f14-8e3f-39eb5b012c48', 'RCP-20260724-0005', 'a23a9e6e-df52-49b0-bf8d-577b8d3462af', 'Erick Juma', NULL, 60.00, 0.00, 0.00, 60.00, 'MPESA', 60.00, 0.00, '', 'COMPLETED', 'PENDING', '2026-07-24 14:28:04', '2026-07-24 14:28:04');

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

--
-- Dumping data for table `sale_items`
--

INSERT INTO `sale_items` (`id`, `sale_id`, `product_id`, `product_name`, `product_sku`, `quantity`, `unit_price`, `buying_price`, `discount`, `tax_rate`, `line_total`) VALUES
('ba40edf1-cc55-46e8-b82a-8e1610dfcbf7', 'e2754f55-6e5a-4e16-aa57-05c049fe6e15', '2d3a0fbe-014b-4230-81ae-ffc76eea1a4f', 'Kaluma Strong', '616110571131', 1, 10.00, 8.00, 0.00, 0.00, 10.00),
('a5a43ce3-e339-40db-b69d-80a4e911c464', 'ded571ff-c92e-475a-8e8f-a3be914a31c2', 'd4186bf3-2178-4044-ade4-728a6dd6fc7b', 'Tilly Cooking Fat', '61611101660211', 1, 20.00, 15.00, 0.00, 0.00, 20.00),
('f1ae777c-df9a-4e15-96e7-c41830997f41', 'ded571ff-c92e-475a-8e8f-a3be914a31c2', '78c19486-8d62-46a0-8b8d-30f14375f64e', 'Nescafe Classic 1.5 g', '6161106962662', 1, 10.00, 8.00, 0.00, 0.00, 10.00),
('660c7e03-7e64-45ca-babd-e27f1aa09619', '01f045a4-f9df-45ef-9dad-8eeb190c42ef', '9ae586d9-73fa-47a1-8b66-2c99c46c3173', 'Mfalme Tissue', '6164002695474', 7, 20.00, 18.00, 0.00, 0.00, 140.00),
('0fcbc011-5adc-4e2e-ac50-ada57cabefde', '47131f5f-41b4-4c8c-a123-36bf500462ab', '1df8138b-5d43-4e7e-8237-69eb273b6681', 'Menengai Cream Quality Washing Bar 1kg', '6161100907034', 4, 50.00, 45.00, 0.00, 0.00, 200.00),
('acc0103a-379e-4211-8a51-78247485ff80', '1ff28d1d-6631-4827-924e-a1b1ee5ad1bc', 'a4877599-2bca-4e6f-8833-c25a4a9b134e', 'Fanta-passion Soda  300ml', '40822924', 1, 50.00, 40.00, 0.00, 0.00, 50.00),
('7addb920-586f-4b1c-9176-3b12d7b97964', '8c37d3b5-023b-4e38-ad96-cab41404dbe6', '06a0da23-fd56-43b4-818e-707c0837f3f3', 'Rosy Extra Strong Tissue Pink color', '6161100762411', 2, 20.00, 18.00, 0.00, 0.00, 40.00),
('a20d8c0c-320d-4a0b-be63-d43e6cbc2400', '54a06e2f-d12f-4df2-8477-70dfa438cd11', '353914f2-3fed-48cb-bfd6-a3e179c9593b', 'Fanta-Orange Soda  200ml', '90495090', 1, 25.00, 15.00, 0.00, 0.00, 25.00),
('dd853fe3-b027-4a6e-9535-c7dfbc21bfae', 'bd82c0c4-ea21-4f40-bdb0-699f35854434', '24480d20-68c0-4532-bde2-f9fd388b9f22', 'Panadol Advanced', '68686102525', 1, 20.00, 15.00, 0.00, 0.00, 20.00),
('c278a803-bfc9-4d32-8994-b366844beab2', 'f7d6495b-f11c-4f14-8e3f-39eb5b012c48', 'd14e4361-dae2-4ccc-8284-bb7c370fa32d', 'Fanta - Orange Soda 500ml', '50112173', 1, 60.00, 50.00, 0.00, 0.00, 60.00),
('b6e192f6-09fd-41e3-9af6-c602169fa5b8', 'd3655701-c4f0-4c3a-a710-729a50f317f3', '1df8138b-5d43-4e7e-8237-69eb273b6681', 'Menengai Cream Quality Washing Bar 1kg', '6161100907034', 1, 50.00, 45.00, 0.00, 0.00, 50.00),
('ba1abc5d-d7fc-4709-b549-981147eb780d', '3f8c5ec2-00e3-4dff-b8b4-24c079a2efbc', 'e082b326-cf02-4226-8a83-48fb91da8cfd', 'Sprite soda 300ml', '54492691', 1, 50.00, 40.00, 0.00, 0.00, 50.00),
('acb04667-6f6f-421e-b95d-5498545cdbbb', 'c9f7c97c-b037-49ec-99f0-0d43fef75e08', '151b9723-7b8a-4390-8194-6e5a3878cc80', 'Dalia Luxurious Soft White Tissue', '0792382597352', 1, 20.00, 18.00, 0.00, 0.00, 20.00),
('877bdcd5-3935-4b6f-9965-47b0bbeaa8eb', '89553478-c9d5-4cf7-bf16-0f9e0fb9d719', '565c4288-b7ed-4c23-9e01-9294e1e98279', 'Tobex Bleach 70 ml', '6164000015106', 3, 35.00, 30.00, 0.00, 0.00, 105.00),
('94a2f06b-89b2-499e-9012-58c1a3bccd7e', '89553478-c9d5-4cf7-bf16-0f9e0fb9d719', 'e63744cd-09e1-4a40-82cf-75e76647f7b7', 'Jamaa White Washing Bar', '6161101667111', 1, 40.00, 30.00, 0.00, 0.00, 40.00),
('16f6808c-cbf1-4894-8d91-2ae95bba86b1', '89553478-c9d5-4cf7-bf16-0f9e0fb9d719', '91571c23-8e2c-4ac1-8c50-c907b8d74121', 'Cyano Acrylate Adhesive super glue', '6940988800206', 2, 35.00, 30.00, 0.00, 0.00, 70.00),
('bf592da8-3555-4afc-8b04-a6f6ea8d05bb', '89553478-c9d5-4cf7-bf16-0f9e0fb9d719', '353914f2-3fed-48cb-bfd6-a3e179c9593b', 'Fanta-Orange Soda  200ml', '90495090', 1, 25.00, 15.00, 0.00, 0.00, 25.00),
('314106fa-97a6-48a3-aff3-2adcda540026', 'ec7ae38b-00ff-4d47-a9f0-540e354bd711', 'ad049a23-82cd-45cb-a2cb-35ad2999c562', 'KCC FRESH WHOLE MILK 200ML', '6161100460126', 1, 35.00, 30.00, 0.00, 0.00, 35.00),
('680c0c14-27e9-4fd4-b67c-8171af4b73fe', '3d8e9bff-9299-4711-823d-3783089bb1a7', 'b2713b07-0067-4411-a5d6-114be39ad259', 'Ajab Fortified All Purpose Home Baking Flour 500g', '6161113940127', 1, 55.00, 50.00, 0.00, 0.00, 55.00),
('e5ee85a3-25f7-4ce9-8407-6b20f2c5d55b', '3d8e9bff-9299-4711-823d-3783089bb1a7', '12be2e1f-71b1-45dc-9e3c-4deadc38b96c', 'KENSALT  Iodated Edible Table Salt 200g', '6161101280037', 1, 15.00, 10.00, 0.00, 0.00, 15.00);

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

INSERT INTO `suppliers` (`id`, `name`, `phone`, `email`, `address`, `balance`, `sync_status`, `created_at`, `updated_at`) VALUES
('36c12be2-1d6c-4899-a709-ac24146a29ea', 'Genesis international', NULL, NULL, 'Nakuru,Kenya', 0.00, 'SYNCED', '2026-07-23 00:43:12', '2026-07-23 00:43:12'),
('87e77414-71f2-4453-9382-4435634bf5f7', 'bonface onduso', NULL, 'bonfaceobnduso9@gmail.com', 'Nakuru,kenya', 0.00, 'SYNCED', '2026-07-22 16:11:58', '2026-07-22 23:43:35');

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
-- Table structure for table `suspended_cart_items`
--

CREATE TABLE `suspended_cart_items` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cart_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_sku` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `unit_price` decimal(12,2) DEFAULT NULL,
  `buying_price` decimal(12,2) DEFAULT NULL,
  `discount` decimal(6,2) DEFAULT '0.00',
  `tax_rate` decimal(6,2) DEFAULT '0.00',
  `line_total` decimal(12,2) DEFAULT NULL
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
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password_hash`, `role`, `full_name`, `active`, `failed_login_attempts`, `lockout_until`, `sync_status`, `created_at`, `updated_at`) VALUES
('62770cd8-861f-11f1-baf7-9c7bef765a9e', 'erickmosess', '$2y$12$TcpKIx7THuKtqwo8F5P6q.0KxrhsgqZ0Rycj9DlwJd2HocEj7rS5y', 'ADMIN', 'erick juma', 1, 0, NULL, 'PENDING', '2026-07-23 17:54:06', '2026-07-24 15:59:39'),
('a0000000-0000-0000-0000-000000000001', 'admin', '$2y$10$mTWMBmM5qj6ug./GDsXkUeRiQXKKCgfJfmHrO6I7PpvNLFgaCa/Qm', 'ADMIN', 'Administrator', 1, 0, NULL, 'SYNCED', '2026-07-23 01:08:39', '2026-07-23 01:49:40'),
('a0000000-0000-0000-0000-000000000002', 'cashier', '$2y$12$KIB3WnRnJPMY2IyH9bMKSOzCBCGSGXp7ZLhd3Ln4LvjRn3qTrDr4C', 'CASHIER', 'Default Cashier', 1, 0, NULL, 'SYNCED', '2026-07-23 01:08:39', '2026-07-23 01:08:39'),
('cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10', 'veronica', '', 'CASHIER', 'veronica', 1, 0, NULL, 'PENDING', '2026-07-22 17:20:16', '2026-07-22 18:52:59');

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
-- Indexes for table `suspended_cart_items`
--
ALTER TABLE `suspended_cart_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `cart_id` (`cart_id`);

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

--
-- Constraints for table `suspended_cart_items`
--
ALTER TABLE `suspended_cart_items`
  ADD CONSTRAINT `suspended_cart_items_ibfk_1` FOREIGN KEY (`cart_id`) REFERENCES `suspended_carts` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
