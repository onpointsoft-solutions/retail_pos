BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "app_settings" (
	"key"	TEXT,
	"value"	TEXT,
	PRIMARY KEY("key")
);
CREATE TABLE IF NOT EXISTS "audit_logs" (
	"id"	TEXT,
	"user_id"	TEXT,
	"event_type"	TEXT,
	"entity_id"	TEXT,
	"details"	TEXT,
	"created_at"	TEXT,
	PRIMARY KEY("id")
);
CREATE TABLE IF NOT EXISTS "categories" (
	"id"	TEXT,
	"name"	TEXT NOT NULL,
	"description"	TEXT,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	"updated_at"	TEXT,
	PRIMARY KEY("id")
);
CREATE TABLE IF NOT EXISTS "customers" (
	"id"	TEXT,
	"name"	TEXT NOT NULL,
	"phone"	TEXT UNIQUE,
	"email"	TEXT UNIQUE,
	"loyalty_points"	INTEGER DEFAULT 0,
	"credit_balance"	REAL DEFAULT 0,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	"updated_at"	TEXT,
	PRIMARY KEY("id")
);
CREATE TABLE IF NOT EXISTS "inventory_movements" (
	"id"	TEXT,
	"product_id"	TEXT NOT NULL,
	"product_name"	TEXT,
	"type"	TEXT NOT NULL,
	"quantity"	INTEGER,
	"reason"	TEXT,
	"batch_number"	TEXT,
	"expiry_date"	TEXT,
	"user_id"	TEXT,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	PRIMARY KEY("id"),
	FOREIGN KEY("product_id") REFERENCES "products"("id")
);
CREATE TABLE IF NOT EXISTS "product_images" (
	"id"	TEXT,
	"product_id"	TEXT NOT NULL,
	"image_path"	TEXT NOT NULL,
	"display_order"	INTEGER DEFAULT 0,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	"updated_at"	TEXT,
	"deleted_at"	TEXT,
	PRIMARY KEY("id"),
	FOREIGN KEY("product_id") REFERENCES "products"("id")
);
CREATE TABLE IF NOT EXISTS "products" (
	"id"	TEXT,
	"barcode"	TEXT,
	"qr_code"	TEXT,
	"sku"	TEXT NOT NULL,
	"name"	TEXT NOT NULL,
	"category_id"	TEXT,
	"buying_price"	REAL DEFAULT 0,
	"selling_price"	REAL NOT NULL,
	"wholesale_price"	REAL DEFAULT 0,
	"current_stock"	INTEGER DEFAULT 0,
	"minimum_stock"	INTEGER DEFAULT 0,
	"tax_rate"	REAL DEFAULT 0,
	"discount"	REAL DEFAULT 0,
	"supplier_id"	TEXT,
	"description"	TEXT,
	"image_path"	TEXT,
	"unit"	TEXT DEFAULT 'pcs',
	"status"	TEXT DEFAULT 'active',
	"track_expiry"	INTEGER DEFAULT 0,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"version"	INTEGER DEFAULT 1,
	"created_at"	TEXT,
	"updated_at"	TEXT,
	"deleted_at"	TEXT,
	"preferred_order_quantity"	INTEGER DEFAULT 0,
	PRIMARY KEY("id"),
	FOREIGN KEY("category_id") REFERENCES "categories"("id")
);
CREATE TABLE IF NOT EXISTS "purchase_order_items" (
	"id"	TEXT,
	"po_id"	TEXT NOT NULL,
	"product_id"	TEXT,
	"product_name"	TEXT,
	"ordered_qty"	INTEGER,
	"received_qty"	INTEGER DEFAULT 0,
	"buying_price"	REAL,
	PRIMARY KEY("id"),
	FOREIGN KEY("po_id") REFERENCES "purchase_orders"("id")
);
CREATE TABLE IF NOT EXISTS "purchase_orders" (
	"id"	TEXT,
	"supplier_id"	TEXT,
	"supplier_name"	TEXT,
	"status"	TEXT DEFAULT 'ORDERED',
	"expected_delivery_date"	TEXT,
	"notes"	TEXT,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	"updated_at"	TEXT,
	PRIMARY KEY("id"),
	FOREIGN KEY("supplier_id") REFERENCES "suppliers"("id")
);
CREATE TABLE IF NOT EXISTS "sale_items" (
	"id"	TEXT,
	"sale_id"	TEXT NOT NULL,
	"product_id"	TEXT,
	"product_name"	TEXT,
	"product_sku"	TEXT,
	"quantity"	INTEGER,
	"unit_price"	REAL,
	"buying_price"	REAL,
	"discount"	REAL DEFAULT 0,
	"tax_rate"	REAL DEFAULT 0,
	"line_total"	REAL,
	PRIMARY KEY("id"),
	FOREIGN KEY("sale_id") REFERENCES "sales"("id")
);
CREATE TABLE IF NOT EXISTS "sales" (
	"id"	TEXT,
	"receipt_number"	TEXT NOT NULL UNIQUE,
	"cashier_id"	TEXT,
	"cashier_name"	TEXT,
	"customer_id"	TEXT,
	"subtotal"	REAL,
	"discount_amount"	REAL DEFAULT 0,
	"tax_amount"	REAL DEFAULT 0,
	"grand_total"	REAL,
	"payment_method"	TEXT,
	"cash_tendered"	REAL DEFAULT 0,
	"change_amount"	REAL DEFAULT 0,
	"payment_reference"	TEXT,
	"status"	TEXT DEFAULT 'COMPLETED',
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	"updated_at"	TEXT,
	PRIMARY KEY("id"),
	FOREIGN KEY("customer_id") REFERENCES "customers"("id")
);
CREATE TABLE IF NOT EXISTS "suppliers" (
	"id"	TEXT,
	"name"	TEXT NOT NULL,
	"phone"	TEXT,
	"email"	TEXT,
	"address"	TEXT,
	"balance"	REAL DEFAULT 0,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	"updated_at"	TEXT,
	PRIMARY KEY("id")
);
CREATE TABLE IF NOT EXISTS "suspended_cart_items" (
	"id"	TEXT,
	"cart_id"	TEXT NOT NULL,
	"product_id"	TEXT,
	"product_name"	TEXT,
	"product_sku"	TEXT,
	"quantity"	INTEGER,
	"unit_price"	REAL,
	"buying_price"	REAL,
	"discount"	REAL DEFAULT 0,
	"tax_rate"	REAL DEFAULT 0,
	"line_total"	REAL,
	PRIMARY KEY("id"),
	FOREIGN KEY("cart_id") REFERENCES "suspended_carts"("id")
);
CREATE TABLE IF NOT EXISTS "suspended_carts" (
	"id"	TEXT,
	"cashier_id"	TEXT,
	"customer_id"	TEXT,
	"discount_amount"	REAL DEFAULT 0,
	"suspended_at"	TEXT,
	PRIMARY KEY("id")
);
CREATE TABLE IF NOT EXISTS "users" (
	"id"	TEXT,
	"username"	TEXT NOT NULL UNIQUE,
	"password_hash"	TEXT NOT NULL,
	"role"	TEXT NOT NULL,
	"full_name"	TEXT,
	"active"	INTEGER DEFAULT 1,
	"failed_login_attempts"	INTEGER DEFAULT 0,
	"lockout_until"	TEXT,
	"sync_status"	TEXT DEFAULT 'PENDING',
	"created_at"	TEXT,
	"updated_at"	TEXT,
	PRIMARY KEY("id")
);
INSERT INTO "app_settings" VALUES ('store_name','victorious general shop');
INSERT INTO "app_settings" VALUES ('store_address','kabarak,Nakuru');
INSERT INTO "app_settings" VALUES ('store_phone','0742071810');
INSERT INTO "app_settings" VALUES ('store_footer','Thank you for shopping with us!');
INSERT INTO "app_settings" VALUES ('logo_path','C:\Users\Victorious\AppData\Local\RetailPOS\images\logo-b27b2708-2f14-42e9-863e-0ef97a870580.png');
INSERT INTO "app_settings" VALUES ('printer_name','(Default printer)');
INSERT INTO "app_settings" VALUES ('paper_width','80');
INSERT INTO "app_settings" VALUES ('tax_rate','0.0');
INSERT INTO "app_settings" VALUES ('loyalty_earning_rate','1.0');
INSERT INTO "app_settings" VALUES ('sync_api_url','https://pos.victoriousgeneralshop.com/api/');
INSERT INTO "app_settings" VALUES ('sync_api_token','eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiNjI3NzBjZDgtODYxZi0xMWYxLWJhZjctOWM3YmVmNzY1YTllIiwidXNlcm5hbWUiOiJlcmlja21vc2VzcyIsInJvbGUiOiJBRE1JTiIsInN0b3JlX2lkIjpudWxsLCJpYXQiOjE3ODQ4MDI4MzcsImV4cCI6MTc4NDg4OTIzN30.Qv8PVt0lVHMmTuSt8dZJO_sHBjQubv0Nns-b1373O7o');
INSERT INTO "app_settings" VALUES ('sync_api_username','erickmosess');
INSERT INTO "app_settings" VALUES ('sync_api_password','erick2030');
INSERT INTO "app_settings" VALUES ('auto_sync','true');
INSERT INTO "app_settings" VALUES ('dark_mode','false');
INSERT INTO "app_settings" VALUES ('primary_color','#D97706');
INSERT INTO "app_settings" VALUES ('backup_path','backups');
INSERT INTO "app_settings" VALUES ('backup_time','23:00');
INSERT INTO "app_settings" VALUES ('auto_print_receipt','true');
INSERT INTO "app_settings" VALUES ('setup_complete','true');
INSERT INTO "app_settings" VALUES ('last_successful_sync','2026-07-24 15:09:34');
INSERT INTO "audit_logs" VALUES ('b177ef39-0a69-47b9-a497-92347ab432ac','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T17:01:18.499339800');
INSERT INTO "audit_logs" VALUES ('c42070a5-801c-4118-b0ea-3f2f9d06d33f','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','38fca219-6abc-4c73-a517-94a3b9e10d62','name=KABRAS SUGAR 1 KG','2026-07-22T17:18:55.200138700');
INSERT INTO "audit_logs" VALUES ('c4ede2b8-b5e5-4dcb-addf-6c62ce852bdc','a23a9e6e-df52-49b0-bf8d-577b8d3462af','USER_CREATED','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','username=veronica,role=CASHIER','2026-07-22T17:20:16.910811900');
INSERT INTO "audit_logs" VALUES ('52147873-e351-4ff6-b442-b66b6cc1f94c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-22T17:20:27.499608600');
INSERT INTO "audit_logs" VALUES ('d5e55d38-3968-4475-9dec-1d515be13607','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','LOGIN','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','role=CASHIER','2026-07-22T17:20:40.290196700');
INSERT INTO "audit_logs" VALUES ('ff12cb02-3a72-4473-9559-8e47a3636a65','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','SALE_COMPLETED','630c0219-e187-4309-9005-175c7afc929d','receipt=RCP-20260722-0001,total=754.0','2026-07-22T17:22:23.820309400');
INSERT INTO "audit_logs" VALUES ('986ff40c-38be-4e59-aace-d71d4773a88a','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','LOGOUT','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','','2026-07-22T17:23:46.827014900');
INSERT INTO "audit_logs" VALUES ('90c4d9e4-fe86-40be-85c6-c43aa6d3ab14','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T17:23:56.604123');
INSERT INTO "audit_logs" VALUES ('cfa6d78d-170f-44fb-9160-8242d6797d4e','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SETTINGS_CHANGED',NULL,'Settings saved','2026-07-22T17:25:08.587855');
INSERT INTO "audit_logs" VALUES ('e4e7c356-5127-43fa-97bd-aa592b9bd39b','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','7510247a-9f8a-4247-b20a-b6418f7c1d52','name=vasiline pj cocoa butter','2026-07-22T17:34:46.202965100');
INSERT INTO "audit_logs" VALUES ('a3197083-f617-4f09-9a49-a9f636b6e886','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','31188683-ab59-4bc1-90b9-323cbf7970ba','name=office glue','2026-07-22T17:38:56.749681200');
INSERT INTO "audit_logs" VALUES ('ef617a39-af7b-49d3-9d19-981195c9b841','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','a4e78689-4d98-436e-b819-b7454fcea201','name=unga number 2','2026-07-22T17:46:34.359939400');
INSERT INTO "audit_logs" VALUES ('bf3d7ae7-29f1-461c-a30b-eb997e779bc7','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','ff2ffded-38fd-48aa-b82c-f6d119817712','name=unga number 2(1/2kg)','2026-07-22T17:50:39.216952400');
INSERT INTO "audit_logs" VALUES ('581dedee-6c9e-4a88-a5e3-b0d1ca1b28cb','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','a4e78689-4d98-436e-b819-b7454fcea201','name=unga number 2(1 kg)','2026-07-22T17:51:37.212396100');
INSERT INTO "audit_logs" VALUES ('53ac8a1a-2c60-4311-b583-e31553b81001','a23a9e6e-df52-49b0-bf8d-577b8d3462af','FAILED_LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','attempts=1','2026-07-22T17:57:58.775191800');
INSERT INTO "audit_logs" VALUES ('db4ec0b9-51dd-469e-8cb1-fb0b987b959a','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T17:58:15.632046500');
INSERT INTO "audit_logs" VALUES ('07cd81a9-f39a-490b-962d-aa642cfdc0a5','a23a9e6e-df52-49b0-bf8d-577b8d3462af','FAILED_LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','attempts=1','2026-07-22T18:26:03.573434600');
INSERT INTO "audit_logs" VALUES ('ee648857-4f81-4014-91ed-a6ae56a93e56','a23a9e6e-df52-49b0-bf8d-577b8d3462af','FAILED_LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','attempts=2','2026-07-22T18:26:09.544211500');
INSERT INTO "audit_logs" VALUES ('a5ff9ccc-d743-4245-9785-573432bc5aea','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T18:26:21.774892400');
INSERT INTO "audit_logs" VALUES ('32deaddc-a171-41ad-85b2-e25e5dfd17fd','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','0676a995-2dce-4187-97cd-31e51c64972f','receipt=RCP-20260722-0002,total=284.2','2026-07-22T18:35:37.265840500');
INSERT INTO "audit_logs" VALUES ('390e6958-275e-42f9-bae4-77ab72856664','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T18:41:10.814716600');
INSERT INTO "audit_logs" VALUES ('0333fecb-85ad-4371-981a-413645552a6e','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-22T18:51:23.788040500');
INSERT INTO "audit_logs" VALUES ('43dba565-df50-4e4a-b3bd-4468e77d44d2','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','FAILED_LOGIN','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','attempts=1','2026-07-22T18:52:03.466027100');
INSERT INTO "audit_logs" VALUES ('3b261192-3ad2-438b-934e-1fef7a776a40','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','FAILED_LOGIN','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','attempts=2','2026-07-22T18:52:14.900368400');
INSERT INTO "audit_logs" VALUES ('f2b35220-623d-4ff5-8b1e-837c947f4323','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','FAILED_LOGIN','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','attempts=3','2026-07-22T18:52:27.420916900');
INSERT INTO "audit_logs" VALUES ('924f9575-5cf1-4019-a16c-db407d66e2ec','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','FAILED_LOGIN','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','attempts=4','2026-07-22T18:52:37.670851800');
INSERT INTO "audit_logs" VALUES ('a51ba72e-09b1-4445-b4db-ad7f38ac681b','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','FAILED_LOGIN','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','attempts=5','2026-07-22T18:52:59.202177300');
INSERT INTO "audit_logs" VALUES ('b10e1586-6207-4008-a601-daf0ee648b4b','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','ACCOUNT_LOCKED','cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','locked_until=2026-07-22T19:07:59.129323200','2026-07-22T18:52:59.269215500');
INSERT INTO "audit_logs" VALUES ('0d3e0d8c-14aa-431b-8d78-3f4dde1a66d7','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T18:54:12.620572400');
INSERT INTO "audit_logs" VALUES ('4606f002-09eb-4d27-8ed9-230d44dd65d6','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T19:31:17.103929900');
INSERT INTO "audit_logs" VALUES ('6598acdf-5863-46a6-9199-f0e06903d3f1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','bb27c5f2-3e56-4bf4-add4-ce56a6cd5551','receipt=RCP-20260722-0003,total=197.2','2026-07-22T19:38:23.698386100');
INSERT INTO "audit_logs" VALUES ('b9e40411-c70f-4095-9cbc-a7b6603fba3e','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','5f6f7dfd-e8ee-4ac3-b7c9-44ee041a70ff','receipt=RCP-20260722-0004,total=46.4','2026-07-22T19:46:56.177586400');
INSERT INTO "audit_logs" VALUES ('0b8402dc-9226-4533-afe0-74e37d0d62cb','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','e1d05bd9-0969-4143-b192-faee0dc3f03d','receipt=RCP-20260722-0005,total=81.2','2026-07-22T19:54:31.838628400');
INSERT INTO "audit_logs" VALUES ('6b37425a-2c71-446a-8e5c-ebbea1a51788','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','01bf1227-164f-412e-a6b9-d56538aa7fcf','receipt=RCP-20260722-0006,total=197.2','2026-07-22T19:55:39.343485300');
INSERT INTO "audit_logs" VALUES ('a81bcfa2-55ff-486d-9d0a-900286f818b6','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','f4d6766d-3dd0-498a-866a-e343de439ad7','receipt=RCP-20260722-0007,total=185.6','2026-07-22T22:00:18.008236800');
INSERT INTO "audit_logs" VALUES ('1f41ca75-acbb-421f-b924-ecbe232a1fb2','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','433b31ca-b65e-4d0b-a70a-0ade57b1124d','receipt=RCP-20260722-0008,total=81.2','2026-07-22T22:01:37.585865400');
INSERT INTO "audit_logs" VALUES ('f46f050b-3642-435d-96d7-632102a894d1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','7ad2c965-d0e1-4ba7-bf5d-ab78544224a0','receipt=RCP-20260722-0009,total=139.2','2026-07-22T22:02:30.897995900');
INSERT INTO "audit_logs" VALUES ('6fbb7327-1147-41dd-8451-06d3673bda38','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','64a3308a-1397-45eb-9818-33b0191fda74','receipt=RCP-20260722-0010,total=197.2','2026-07-22T22:03:36.068806500');
INSERT INTO "audit_logs" VALUES ('1a2e3604-8761-4442-9621-a265d287fe50','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','9f08f212-3847-494b-a846-6710d1124e68','receipt=RCP-20260722-0011,total=516.2','2026-07-22T22:07:55.424519');
INSERT INTO "audit_logs" VALUES ('bee61e81-7203-43d8-b5f7-27ffa2a83005','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','7b18ba07-fdc5-46d2-8021-dd29c8def10d','receipt=RCP-20260722-0012,total=817.8','2026-07-22T22:13:26.433516800');
INSERT INTO "audit_logs" VALUES ('86e3e546-495f-4955-bf7d-6ed4714af6ce','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T22:39:00.859930400');
INSERT INTO "audit_logs" VALUES ('4c7c7f94-5278-4027-a290-0207e1544175','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_DELETED','38fca219-6abc-4c73-a517-94a3b9e10d62','','2026-07-22T22:40:02.009162400');
INSERT INTO "audit_logs" VALUES ('31ba2ba5-e41d-4405-84c7-4f797e68bec4','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_DELETED','31188683-ab59-4bc1-90b9-323cbf7970ba','','2026-07-22T22:40:07.493121400');
INSERT INTO "audit_logs" VALUES ('7dcf6fdf-9ad2-4e16-bddc-06c10f9d04d4','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_DELETED','a4e78689-4d98-436e-b819-b7454fcea201','','2026-07-22T22:40:15.992470200');
INSERT INTO "audit_logs" VALUES ('63e80be2-b85c-4bf3-940e-501945a1dab5','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_DELETED','ff2ffded-38fd-48aa-b82c-f6d119817712','','2026-07-22T22:40:22.288942700');
INSERT INTO "audit_logs" VALUES ('13293bea-44b0-4d1d-a58c-739998dedeb2','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_DELETED','7510247a-9f8a-4247-b20a-b6418f7c1d52','','2026-07-22T22:40:26.163661100');
INSERT INTO "audit_logs" VALUES ('9e3a7241-b62d-47ab-9b9e-dd2195d8e332','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T22:53:02.190910900');
INSERT INTO "audit_logs" VALUES ('b9e323e1-0152-4ce5-90bb-73bd35a10abd','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-22T22:58:25.238445600');
INSERT INTO "audit_logs" VALUES ('6955e7dc-eea9-4864-a96d-ac4df5447e78','SYSTEM','FAILED_LOGIN',NULL,'username=erickosess (not found)','2026-07-22T22:58:43.781994400');
INSERT INTO "audit_logs" VALUES ('e7062f69-fe6c-4433-9eaf-b9fbae89de6d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T22:58:56.171659');
INSERT INTO "audit_logs" VALUES ('498eb7ac-b51f-4077-a9fb-0750568f9cb5','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-22T22:59:09.671060800');
INSERT INTO "audit_logs" VALUES ('6d52bc71-6bbe-4449-a6f8-de9eb61325a5','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T22:59:21.888379700');
INSERT INTO "audit_logs" VALUES ('7d9feffc-b78d-4968-95c7-b93ba6ac046b','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SETTINGS_CHANGED',NULL,'Settings saved','2026-07-22T23:00:31.259213500');
INSERT INTO "audit_logs" VALUES ('e301c244-5fb7-4f95-964c-9722011687c1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-22T23:04:11.141411700');
INSERT INTO "audit_logs" VALUES ('17b750a7-a879-42b6-bcfe-af30bc80904c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-22T23:04:22.796998400');
INSERT INTO "audit_logs" VALUES ('5098db2e-ee39-4f0c-a664-6e562d1848b1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','703ce993-c0c6-4988-b761-0be633222e18','name=Ajab Fortified All Purpose Home Baking Wheat Flour','2026-07-22T23:33:41.805373300');
INSERT INTO "audit_logs" VALUES ('4fd74e02-54f5-49d9-a205-9145fad01cfb','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','bfbf8bcc-41ab-46dd-baa8-3afa5ac0bb1c','name=ASIS Premium Tangawizi-15G','2026-07-22T23:42:10.439337800');
INSERT INTO "audit_logs" VALUES ('571c7825-7137-4215-bc2b-940a4e8e8148','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','c0ec287b-220d-467a-a2a6-ce52746ed6bf','name=Vaseline BlueSeal Cocoa Butter 95g','2026-07-22T23:46:49.121084700');
INSERT INTO "audit_logs" VALUES ('f35a3271-6983-4a66-bb95-b5c2f3eff4b8','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','bfbf8bcc-41ab-46dd-baa8-3afa5ac0bb1c','name=ASIS Premium Tangawizi-15G','2026-07-22T23:47:33.973427200');
INSERT INTO "audit_logs" VALUES ('ead082c9-e856-4b89-8e23-2e6222a934cf','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','aac1f838-5ad0-402d-9c3b-105e3df34d22','name=KIMBO 500G','2026-07-22T23:52:12.983024300');
INSERT INTO "audit_logs" VALUES ('8ee5000b-bbff-463c-a9fa-f33c4aea1881','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','d4186bf3-2178-4044-ade4-728a6dd6fc7b','name=Tilly Cooking Fat','2026-07-22T23:58:51.040803800');
INSERT INTO "audit_logs" VALUES ('cb9c6ca1-7322-4f9c-b087-648f5f295bd4','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','126560a6-2315-44f3-bb4c-12528dc8b9b9','name=Colgate','2026-07-23T00:08:06.793400500');
INSERT INTO "audit_logs" VALUES ('8a49292e-8424-4266-85cb-1c70f8120a26','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','6f36e781-fb79-4824-8513-a19c4d652a07','name=Pure Glucose','2026-07-23T00:16:52.034276700');
INSERT INTO "audit_logs" VALUES ('03d672f2-1879-4046-886a-65c5a4817185','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','b3843ebe-3392-4e56-b4e5-5f7620292172','name=Barjot Office Glue 90gms','2026-07-23T00:23:29.245959');
INSERT INTO "audit_logs" VALUES ('56150d73-a90c-4823-8683-4011c2aec37a','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','2c3a808c-7fb2-4b4a-8c10-3f983906721f','name=Mara Moja','2026-07-23T00:26:48.932733200');
INSERT INTO "audit_logs" VALUES ('6d1b8125-88ac-403a-a665-77b9b95ab763','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','5b0635b0-07ea-42c4-8a05-550542d8f55b','name=Kaluma Pain Palm','2026-07-23T00:29:37.606928600');
INSERT INTO "audit_logs" VALUES ('9793f813-7842-4b68-9e88-dd7185d88f1f','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','2c3a808c-7fb2-4b4a-8c10-3f983906721f','name=Mara Moja','2026-07-23T00:31:52.329253');
INSERT INTO "audit_logs" VALUES ('0c754ffe-4363-43a8-a3c8-898795e44694','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','24480d20-68c0-4532-bde2-f9fd388b9f22','name=Panadol Advanced','2026-07-23T00:37:10.161233');
INSERT INTO "audit_logs" VALUES ('8b5f77e5-9a0f-441b-8d2c-fe69f742d78b','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','88e49a6f-c50f-4d7d-bd9b-dd9230e042fe','name=Baida Lighter','2026-07-23T00:41:55.267020800');
INSERT INTO "audit_logs" VALUES ('c78eace0-068f-4baa-8f1a-8112ecf47060','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','117fee71-adbe-40bd-9d62-17ab73f2bce7','name=Valon Skin Care 95ml','2026-07-23T00:51:55.444910100');
INSERT INTO "audit_logs" VALUES ('ad403d62-f24b-47ea-8506-61ea038ab84e','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','96ab684c-b6fd-43a8-bda6-3be54992463b','name=Arimis Milking Jelly 200ml','2026-07-23T00:57:24.192015800');
INSERT INTO "audit_logs" VALUES ('f64f9f71-6a2a-42fd-9c77-871900386bbb','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','b2288b18-3a3b-4e36-8f82-9ea8b53bc256','name=Green Grams','2026-07-23T01:05:23.993036600');
INSERT INTO "audit_logs" VALUES ('ec8ea0a3-29f2-456a-a41d-de7390e97523','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','b2288b18-3a3b-4e36-8f82-9ea8b53bc256','name=Laxmi Green Grams 500g','2026-07-23T01:06:04.116426600');
INSERT INTO "audit_logs" VALUES ('babbd912-433f-4c03-b42c-57a7a5a9646c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','0728ac39-e781-4a0f-b779-6bbd5da4dee9','name=Blueband Spread for bread 100g','2026-07-23T01:13:29.750960500');
INSERT INTO "audit_logs" VALUES ('a25f01e4-0712-4db3-acef-283dc19c3fb1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','2cfaba05-01a8-4a8c-8ff8-03466526f694','name=Blueband Original 100g','2026-07-23T01:18:41.776378700');
INSERT INTO "audit_logs" VALUES ('54c9c6c7-34b9-424d-a930-5e51c7a0af92','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','c97fbd5a-7186-45bc-9d2d-feadb41b943b','name=Blueband With Choco 100g','2026-07-23T01:22:33.442519800');
INSERT INTO "audit_logs" VALUES ('5da33d84-7eda-43be-a913-fed6729dc642','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','b2713b07-0067-4411-a5d6-114be39ad259','name=Ajab Fortified All Purpose Home Baking Flour 500g','2026-07-23T01:34:11.678188200');
INSERT INTO "audit_logs" VALUES ('355ead1f-e86f-439b-b969-7693b46310ca','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','703ce993-c0c6-4988-b761-0be633222e18','name=Ajab Fortified All Purpose Home Baking Wheat Flour','2026-07-23T01:34:55.159656900');
INSERT INTO "audit_logs" VALUES ('0c4c96c5-ea0c-45c0-b3be-e2b9f9393baf','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','78c19486-8d62-46a0-8b8d-30f14375f64e','name=Nescafe Classic 1.5 g','2026-07-23T01:40:03.950134');
INSERT INTO "audit_logs" VALUES ('bb7dc895-790d-413a-a22e-c4fa46be30c8','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','ad049a23-82cd-45cb-a2cb-35ad2999c562','name=KCC FRESH WHOLE MILK 200ML','2026-07-23T01:44:52.814927700');
INSERT INTO "audit_logs" VALUES ('62e16e91-1db3-4a61-bf05-664034ac0bfd','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','ffdfd257-8190-4423-8337-9cc399a50320','name=Fresha Maisha Long Life Whole Milk 200ml','2026-07-23T01:48:21.769714700');
INSERT INTO "audit_logs" VALUES ('2a5fc34a-17c7-45e0-b8ab-d7bce9d63897','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','cbc73ad8-8919-4840-88ad-91693f44e42f','name=Menendazi Pure Baking Powder','2026-07-23T01:53:50.697654200');
INSERT INTO "audit_logs" VALUES ('3741b5d1-6d9f-4fed-ba23-0b7a47890753','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','13fa49d0-9946-4d57-9747-fbe0726d435c','name=KCC Fresh Whole Milk 500g','2026-07-23T01:58:01.771421800');
INSERT INTO "audit_logs" VALUES ('aa2ae14d-ed09-4017-81ad-12e40b51abff','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','05f2201b-f789-4dce-8dc7-dcef04f4b36c','name=Mount Kenya Milk 500ml','2026-07-23T02:02:51.835058100');
INSERT INTO "audit_logs" VALUES ('cc1852d0-4659-4d17-91af-219b64ea6699','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','91571c23-8e2c-4ac1-8c50-c907b8d74121','name=Cyano Acrylate Adhesive super glue','2026-07-23T02:07:28.572394700');
INSERT INTO "audit_logs" VALUES ('b171d30f-6f1e-43e9-9d70-6fd055340a2f','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','2d3a0fbe-014b-4230-81ae-ffc76eea1a4f','name=Kaluma Strong','2026-07-23T02:09:50.247931400');
INSERT INTO "audit_logs" VALUES ('17484c23-e213-4222-837f-c44d04a713cd','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T03:43:07.819110800');
INSERT INTO "audit_logs" VALUES ('e052a630-2f66-4a7d-9680-f35a004db834','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','91571c23-8e2c-4ac1-8c50-c907b8d74121','name=Cyano Acrylate Adhesive super glue','2026-07-23T03:46:11.068790500');
INSERT INTO "audit_logs" VALUES ('1e7dbc9a-7039-46aa-831b-3108c8340d52','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','2d3a0fbe-014b-4230-81ae-ffc76eea1a4f','name=Kaluma Strong','2026-07-23T03:48:29.278877900');
INSERT INTO "audit_logs" VALUES ('b08006ce-3abb-415e-a05e-5a000ab78aca','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','219d5be6-9c47-43b1-b5f4-4b7082cab4fd','name=Menangai Cream Quality Washing Bar 800g','2026-07-23T03:55:46.258827600');
INSERT INTO "audit_logs" VALUES ('2fb81bcc-a3db-49c3-956f-73d0aa6a6f18','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','1df8138b-5d43-4e7e-8237-69eb273b6681','name=Menengai Cream Quality Washing Bar 1kg','2026-07-23T03:58:57.052919900');
INSERT INTO "audit_logs" VALUES ('d056f817-2d0c-46dc-95f8-0d1c2e21c593','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','87fc7dd9-ffc7-4974-979f-1861bdce5f29','name=ZENTA Multipurpose Washing Bar','2026-07-23T04:05:09.165881500');
INSERT INTO "audit_logs" VALUES ('b1c9291f-e137-4910-9ef8-4eaf09baba2d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','c2c5f064-09de-427d-bd9c-228527dde863','name=Zenta Multipurpose Washing Bar 1kg','2026-07-23T04:08:28.480874400');
INSERT INTO "audit_logs" VALUES ('2c960100-7845-4a6c-9d7d-96b6b3cb7f84','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','87fc7dd9-ffc7-4974-979f-1861bdce5f29','name=ZENTA Multipurpose Washing Bar 800 g','2026-07-23T04:08:52.927526700');
INSERT INTO "audit_logs" VALUES ('759d6597-8a19-4413-999e-1738798146c2','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','e63744cd-09e1-4a40-82cf-75e76647f7b7','name=Jamaa White Washing Bar','2026-07-23T04:12:26.357153800');
INSERT INTO "audit_logs" VALUES ('51e46ddb-f76a-4be1-9b7b-e0c93f8188fe','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','4b78edeb-402a-4abc-9bba-92ba059cb22e','name=Kifaru Safety Matches','2026-07-23T04:16:24.619692200');
INSERT INTO "audit_logs" VALUES ('3dd3e32c-6c37-4857-9db5-333f1d8788b2','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','565c4288-b7ed-4c23-9e01-9294e1e98279','name=Tobex Bleach 70 ml','2026-07-23T04:19:49.107980700');
INSERT INTO "audit_logs" VALUES ('6d9edda4-5e38-46e4-ab60-7b2593c52422','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','61d42fb4-9c5a-4501-9233-a784707e13fe','name=Eno Fruit Salt','2026-07-23T04:25:29.068351100');
INSERT INTO "audit_logs" VALUES ('71c6f091-7143-4995-9f6f-64464fd8f460','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','4920d20d-5ffd-40c9-89cd-d38f2427b222','name=White Dent Advanced ToothBrush','2026-07-23T04:29:28.094237500');
INSERT INTO "audit_logs" VALUES ('d082b9e5-e5b3-4e3a-b003-8363ab1313cd','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','80b3844a-fce8-4d3f-a16b-10e3dc2725c0','name=Predator Energy Drink','2026-07-23T04:33:46.056536500');
INSERT INTO "audit_logs" VALUES ('79485cfb-4d5f-4c95-a6c8-bcf43b591cf8','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','df03622c-eeab-4539-8175-097a390a2514','name=OLEMOL TOOTH BRUH','2026-07-23T04:37:00.372339500');
INSERT INTO "audit_logs" VALUES ('4aeda099-5f17-4ed1-a45d-35ad466fdaaf','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','822c3d74-e988-4c99-b211-b4414304ceee','name=TP Tishu Poa 100 X 125 mm','2026-07-23T04:42:35.685971400');
INSERT INTO "audit_logs" VALUES ('047dee81-cfea-4004-b9fd-06cf3e5e6423','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','3d0e94d4-fdde-4406-a4a0-88c67af1484e','name=KWETU Softness You Can Trust','2026-07-23T04:48:47.782410400');
INSERT INTO "audit_logs" VALUES ('7ee5ea6f-f44f-4245-a6b6-6393e66b2aea','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','9ae586d9-73fa-47a1-8b66-2c99c46c3173','name=Mfalme Tissue','2026-07-23T04:51:46.097239800');
INSERT INTO "audit_logs" VALUES ('f0687a16-bc0d-47a9-9c38-1eb27e970277','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','28b8bd45-61d1-473e-ae5d-8af659b94c04','name=Tuffy Feel The Softness 100mm X 125 mm','2026-07-23T04:56:18.063504200');
INSERT INTO "audit_logs" VALUES ('acc24ca3-afcf-458d-8a40-fa9356e7fecd','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T14:51:25.322880200');
INSERT INTO "audit_logs" VALUES ('21ad74fd-82fb-4983-ab43-fea422c85a8c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','151b9723-7b8a-4390-8194-6e5a3878cc80','name=Dalia Luxurious Soft White Tissue','2026-07-23T14:58:21.425869600');
INSERT INTO "audit_logs" VALUES ('78d50a39-333b-4b3b-ad75-180633948976','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-23T14:59:26.039885200');
INSERT INTO "audit_logs" VALUES ('511b723e-e3b8-447e-8874-ee629dc185bb','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T14:59:37.381041900');
INSERT INTO "audit_logs" VALUES ('7d40a1c6-0626-4a5c-ba7b-551abd2d0dd3','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','82961b1e-6433-4cf6-83e3-110dad731396','name=Rosy Extra Strong Tissue white Colour','2026-07-23T15:05:29.720122100');
INSERT INTO "audit_logs" VALUES ('18484cf4-f600-4108-ae0e-60c06398f478','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','06a0da23-fd56-43b4-818e-707c0837f3f3','name=Rosy Extra Strong Tissue Pink color','2026-07-23T15:09:34.546584');
INSERT INTO "audit_logs" VALUES ('634d9000-6328-4a8c-b839-aca16168af61','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','822c3d74-e988-4c99-b211-b4414304ceee','name=TP Tishu Poa 100 X 125 mm','2026-07-23T15:10:28.372853800');
INSERT INTO "audit_logs" VALUES ('2f3feb93-098f-478e-ae78-58330e99db82','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','822c3d74-e988-4c99-b211-b4414304ceee','name=TP Tishu Poa 100 X 125 mm','2026-07-23T15:10:59.205445700');
INSERT INTO "audit_logs" VALUES ('a33299ed-27a4-4820-aa58-c70a3ecc20f1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T15:16:33.619346300');
INSERT INTO "audit_logs" VALUES ('a2984dc5-96fb-4197-beaa-ba890e7a7789','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-23T15:18:35.463171300');
INSERT INTO "audit_logs" VALUES ('b2225942-a2d7-43a7-8a0e-769cc83c1d74','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T15:18:46.389020100');
INSERT INTO "audit_logs" VALUES ('9baed47d-dc2d-478c-8226-26c87092ef12','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-23T15:20:19.685868');
INSERT INTO "audit_logs" VALUES ('9fde277e-cdcc-4915-9aa8-60c3486d5e54','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T15:20:28.809791100');
INSERT INTO "audit_logs" VALUES ('ae877b0e-7087-4556-b5d6-9677b6ef2589','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T15:21:09.198012500');
INSERT INTO "audit_logs" VALUES ('029d0e5d-ad07-4379-b58f-ac90750a7974','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-23T15:26:13.982105900');
INSERT INTO "audit_logs" VALUES ('0453e328-c541-4cf2-a6e4-43ab68eeddf1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T15:26:23.278280400');
INSERT INTO "audit_logs" VALUES ('eec1a767-6366-484e-a2ea-574a889a887d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-23T15:31:36.513347900');
INSERT INTO "audit_logs" VALUES ('d2d914dc-dd14-4001-89b5-cb27cc6e81c4','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T15:31:50.086094500');
INSERT INTO "audit_logs" VALUES ('a8f7f68b-7901-4981-801a-ff705581f402','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','bfbf8bcc-41ab-46dd-baa8-3afa5ac0bb1c','name=ASIS Premium Tangawizi-15G','2026-07-23T15:46:16.494591800');
INSERT INTO "audit_logs" VALUES ('0ba49050-7c88-4443-8283-28b843c9d0a3','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','12be2e1f-71b1-45dc-9e3c-4deadc38b96c','name=KENSALT  Iodated Edible Table Salt 200g','2026-07-23T15:58:26.222598200');
INSERT INTO "audit_logs" VALUES ('ca56d292-ce03-4427-b307-90c0f6fb7217','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','e0101a7a-d845-408c-811b-5b9f967a984d','name=Kensalt 500g','2026-07-23T16:43:45.610876700');
INSERT INTO "audit_logs" VALUES ('21257954-9dfe-45af-b2fa-bc8c3c6b651a','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','3b0d7323-d7d5-45e4-8429-73abb4ad78f7','name=Soda Fanta Orange 300ml','2026-07-23T17:08:23.055039300');
INSERT INTO "audit_logs" VALUES ('f5c7d693-d167-4675-a441-29278561b5e5','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','f975304d-916e-48a4-8aa2-0d66fcf3385a','name=Coca-Cola soda 300 ml','2026-07-23T17:14:52.561415400');
INSERT INTO "audit_logs" VALUES ('abe2e57a-2f62-4294-a95d-085c645f2d14','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','3b0d7323-d7d5-45e4-8429-73abb4ad78f7','name=Fanta-Orange Soda  300ml','2026-07-23T17:15:49.126080200');
INSERT INTO "audit_logs" VALUES ('6d3cf05c-9d82-4030-a89d-befb5239dc99','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','353914f2-3fed-48cb-bfd6-a3e179c9593b','name=Fanta-Orange Soda  200ml','2026-07-23T17:20:17.024964600');
INSERT INTO "audit_logs" VALUES ('36bf41e8-6644-476f-a0ee-8451bf7456e3','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','e05ba1a5-7ec9-493a-9634-8268c241871b','name=Anab Black Rice 1/4kg','2026-07-23T17:33:01.018830700');
INSERT INTO "audit_logs" VALUES ('28ead13d-9fca-4b1e-b839-502bb62b46c9','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','8f82ae51-b4de-4c33-803e-c0ddfcdd59fb','name=Anab Black Rice 1/2kg','2026-07-23T17:44:00.181628200');
INSERT INTO "audit_logs" VALUES ('757c333b-0188-4993-ae2c-f9c0b7058ef2','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','d09b6727-f43d-4f6c-90b5-e83622ccba88','name=Anab Black Rice 1kg','2026-07-23T17:47:02.565871200');
INSERT INTO "audit_logs" VALUES ('db072d44-7043-4e47-838f-63c974623d60','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','cf15b13f-bc32-485c-a946-9bac5fa5a504','name=Kangore Rice','2026-07-23T17:52:13.412594');
INSERT INTO "audit_logs" VALUES ('74ab6468-cd37-4744-8a0a-77818d5b02cd','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','cf15b13f-bc32-485c-a946-9bac5fa5a504','name=Kangore Rice 1/4kg','2026-07-23T17:52:58.181198200');
INSERT INTO "audit_logs" VALUES ('23f1b299-76f9-4475-ac56-f24f2db31976','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','b38b209d-00a5-4592-bf9c-d209f1646a95','name=Kangore Rice 1/2kg','2026-07-23T17:56:00.627785400');
INSERT INTO "audit_logs" VALUES ('a496c148-ca73-42ca-8e66-d9126ec3d6d3','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','7b221a0d-8e5f-4e02-9af9-206d6e38848e','name=Kangore Rice 1/4kg','2026-07-23T17:58:00.831537600');
INSERT INTO "audit_logs" VALUES ('1c1ee040-2a45-40e3-bcca-7887801bd4e9','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','7b221a0d-8e5f-4e02-9af9-206d6e38848e','name=Kangore Rice 1kg','2026-07-23T17:58:16.290822');
INSERT INTO "audit_logs" VALUES ('c61ec5f8-cf27-4952-869b-7503dcca0c2d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','eb289aa2-c672-4d07-89ba-ca4fc7b56d2e','name=Fanta-Orange 2L Soda','2026-07-23T18:07:16.801835700');
INSERT INTO "audit_logs" VALUES ('f8bfbebc-8d42-466a-950f-0fb564a52980','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','15ee0340-0336-47c7-876b-bfbe4fbc1ae0','name=Fanta-Passion Soda 2L','2026-07-23T18:11:23.424664800');
INSERT INTO "audit_logs" VALUES ('2040af62-1f6c-44a5-b806-8d92b21ed7f3','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','15ee0340-0336-47c7-876b-bfbe4fbc1ae0','name=Fanta-Passion Soda 2L','2026-07-23T18:11:55.299404');
INSERT INTO "audit_logs" VALUES ('c5b27bef-8ec6-4c3a-8f24-ac7365dbf036','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','4618b6b1-f906-4980-909a-35a8bc9e6857','name=Fanta Black-Current Soda 2L','2026-07-23T18:16:49.696064800');
INSERT INTO "audit_logs" VALUES ('38f948c6-3833-49a8-b5f7-e66976f443e2','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','724a63a9-11cc-4ab7-890c-0b72766707ad','name=Coke Soda 2L','2026-07-23T18:20:54.721432900');
INSERT INTO "audit_logs" VALUES ('41223dfd-556e-4d40-a949-2fcfbd54d655','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','4c0ace44-b8c0-4bdb-9d87-57430dc46d82','name=Coke Soda 1L','2026-07-23T18:26:44.728697300');
INSERT INTO "audit_logs" VALUES ('ef29f347-3efe-41d8-b484-4348f3975df5','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','f975304d-916e-48a4-8aa2-0d66fcf3385a','name=Coke soda 300 ml','2026-07-23T18:27:12.128729200');
INSERT INTO "audit_logs" VALUES ('88f496d4-4da4-4229-8b8d-f51c9f6c0087','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','5b075ce4-082f-4d95-84e5-90652c8e13ee','name=Fanta Black-Current Soda 1L','2026-07-23T18:30:01.752418700');
INSERT INTO "audit_logs" VALUES ('391fa149-bdea-4e89-9a78-30fad85dc0ef','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','d14e4361-dae2-4ccc-8284-bb7c370fa32d','name=Fanta - Orange Soda 500ml','2026-07-23T18:37:03.580702400');
INSERT INTO "audit_logs" VALUES ('34615997-af1a-4856-bba3-bd48e15af500','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','3a66f17f-2d9e-449e-be9e-44e9081d8944','name=Coke soda 350 ml','2026-07-23T18:41:27.783846100');
INSERT INTO "audit_logs" VALUES ('f1be7979-5fbd-4086-8304-212576671845','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','15ee0340-0336-47c7-876b-bfbe4fbc1ae0','name=Fanta-Passion Soda 2L','2026-07-23T18:42:47.987486');
INSERT INTO "audit_logs" VALUES ('70c6f946-5c90-48fe-afe9-700cb13539c3','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','eb289aa2-c672-4d07-89ba-ca4fc7b56d2e','name=Fanta-Orange 2L Soda','2026-07-23T18:43:13.388989');
INSERT INTO "audit_logs" VALUES ('dc91724f-2bcc-4ad0-b39c-8c585f5bb6c0','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','e84650a6-7ea8-421c-9b54-d2355e14bed9','name=Coke Soda 500ml','2026-07-23T18:47:56.784128900');
INSERT INTO "audit_logs" VALUES ('a0291bd2-02d4-44db-ae00-d4bd8920324c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','625f2f14-b4c5-4dbb-8747-5a976def1525','name=Krest Soda 500ml','2026-07-23T18:51:50.467678100');
INSERT INTO "audit_logs" VALUES ('e39afa26-e5c0-4a84-9370-e3caaf59a273','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','625f2f14-b4c5-4dbb-8747-5a976def1525','name=Krest Soda 500ml','2026-07-23T18:52:42.518547200');
INSERT INTO "audit_logs" VALUES ('599970a9-da9e-4188-ae9e-215e98f36feb','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','0da97229-8715-4401-868e-85cae1fb677f','name=Fanta Black-Current Soda 500ml','2026-07-23T19:02:26.748861700');
INSERT INTO "audit_logs" VALUES ('66969db3-b907-4447-bfb8-0b00f35ef05e','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','711bb791-b492-4240-b2ca-631a9446f8a1','name=krest Soda 300 ml','2026-07-23T19:06:21.143222800');
INSERT INTO "audit_logs" VALUES ('801d7efe-4f81-429b-bcd0-4a14efaedf1d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','e082b326-cf02-4226-8a83-48fb91da8cfd','name=Sprite soda 300ml','2026-07-23T19:12:24.924885800');
INSERT INTO "audit_logs" VALUES ('164e4786-dfb5-4c87-8614-ef1ae95f3a52','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_CREATED','a4877599-2bca-4e6f-8833-c25a4a9b134e','name=Fanta-passion Soda  300ml','2026-07-23T19:17:25.352320700');
INSERT INTO "audit_logs" VALUES ('81aed77e-9742-4c4e-ab60-cd01e75c1d3a','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SETTINGS_CHANGED',NULL,'Settings saved','2026-07-23T19:19:55.378949700');
INSERT INTO "audit_logs" VALUES ('d416b4f8-e1ee-4f9f-bcd7-7d8976ae3416','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGOUT','a23a9e6e-df52-49b0-bf8d-577b8d3462af','','2026-07-23T19:20:18.456965500');
INSERT INTO "audit_logs" VALUES ('7d969f56-74be-42b2-81c1-1f9e31d6ff24','a23a9e6e-df52-49b0-bf8d-577b8d3462af','FAILED_LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','attempts=1','2026-07-23T19:20:32.237383800');
INSERT INTO "audit_logs" VALUES ('613c4f1b-8f71-4082-a046-24247104fc2d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T19:20:36.893087');
INSERT INTO "audit_logs" VALUES ('da672094-0438-4f3f-af79-7ee61480351d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','e2754f55-6e5a-4e16-aa57-05c049fe6e15','receipt=RCP-20260723-0001,total=10.0','2026-07-23T19:24:21.597413400');
INSERT INTO "audit_logs" VALUES ('71610970-97a1-401f-9ce3-b2c85ac1737c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','ded571ff-c92e-475a-8e8f-a3be914a31c2','receipt=RCP-20260723-0002,total=30.0','2026-07-23T19:31:46.066594900');
INSERT INTO "audit_logs" VALUES ('eac708fe-5ee3-4a17-8ff2-a912e2b6660b','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T19:41:22.045887600');
INSERT INTO "audit_logs" VALUES ('30b568e9-4ce4-4995-96a5-834e1c5c130d','a23a9e6e-df52-49b0-bf8d-577b8d3462af','BACKUP_CREATED',NULL,'backup=C:\Users\Victorious\AppData\Local\RetailPOS\backups\retail_pos_backup_20260723_194349.db','2026-07-23T19:43:49.880611500');
INSERT INTO "audit_logs" VALUES ('1e0746ac-53ac-489a-a10e-b4c4fe339b18','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','01f045a4-f9df-45ef-9dad-8eeb190c42ef','receipt=RCP-20260723-0003,total=140.0','2026-07-23T20:38:50.423718800');
INSERT INTO "audit_logs" VALUES ('5e4d0a42-1f5e-44db-ae7a-1e96646ec851','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-23T20:49:23.708055400');
INSERT INTO "audit_logs" VALUES ('b33213b5-3105-4be8-891d-93c6bcf80741','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','47131f5f-41b4-4c8c-a123-36bf500462ab','receipt=RCP-20260723-0004,total=200.0','2026-07-23T21:20:45.941421500');
INSERT INTO "audit_logs" VALUES ('9d3332bb-b7d2-4c80-bfc4-47f157d7a69c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-24T08:12:35.308755500');
INSERT INTO "audit_logs" VALUES ('f5ae9d8c-19cd-4afe-8453-c9fc83aca91e','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-24T08:32:24.594493200');
INSERT INTO "audit_logs" VALUES ('67c6b6fd-b6d0-4d85-bb9c-21b19e5eb3d6','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','1ff28d1d-6631-4827-924e-a1b1ee5ad1bc','receipt=RCP-20260724-0001,total=50.0','2026-07-24T12:26:13.167533');
INSERT INTO "audit_logs" VALUES ('f689683a-90b8-4a7e-a469-f7638f71bc91','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','8c37d3b5-023b-4e38-ad96-cab41404dbe6','receipt=RCP-20260724-0002,total=40.0','2026-07-24T12:27:29.173080300');
INSERT INTO "audit_logs" VALUES ('1dd13f1a-b2a3-4dbc-bf2a-ea6ee0e0d085','SYSTEM','FAILED_LOGIN',NULL,'username=erick moses (not found)','2026-07-24T12:31:44.438219100');
INSERT INTO "audit_logs" VALUES ('8e45c707-23d9-4ea0-b1eb-d40398910b21','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-24T12:50:24.894054500');
INSERT INTO "audit_logs" VALUES ('0311f6f8-7e82-42b0-b874-17e9d56f8620','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','54a06e2f-d12f-4df2-8477-70dfa438cd11','receipt=RCP-20260724-0003,total=25.0','2026-07-24T12:53:24.447990');
INSERT INTO "audit_logs" VALUES ('1d5e16d5-210f-454e-a6d2-85ffa4587afc','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','bd82c0c4-ea21-4f40-bdb0-699f35854434','receipt=RCP-20260724-0004,total=20.0','2026-07-24T13:21:52.302324200');
INSERT INTO "audit_logs" VALUES ('2ce0839c-2089-4b43-976d-28c9fc7ea304','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','f7d6495b-f11c-4f14-8e3f-39eb5b012c48','receipt=RCP-20260724-0005,total=60.0','2026-07-24T14:28:05.015665500');
INSERT INTO "audit_logs" VALUES ('d0af1562-6174-47d5-a5c6-cc539c60cf27','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','d3655701-c4f0-4c3a-a710-729a50f317f3','receipt=RCP-20260724-0006,total=50.0','2026-07-24T14:30:52.216444500');
INSERT INTO "audit_logs" VALUES ('6ac3badc-cf86-48cb-843b-453b0f1c978a','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','3f8c5ec2-00e3-4dff-b8b4-24c079a2efbc','receipt=RCP-20260724-0007,total=50.0','2026-07-24T14:36:59.727531700');
INSERT INTO "audit_logs" VALUES ('e0f5e15c-c3f5-4986-8b3a-324e5544cff0','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','c9f7c97c-b037-49ec-99f0-0d43fef75e08','receipt=RCP-20260724-0008,total=20.0','2026-07-24T14:49:16.377861800');
INSERT INTO "audit_logs" VALUES ('ed85caf5-dc7e-4147-b5af-5984a5a893d9','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','89553478-c9d5-4cf7-bf16-0f9e0fb9d719','receipt=RCP-20260724-0009,total=240.0','2026-07-24T15:31:17.948761400');
INSERT INTO "audit_logs" VALUES ('e2a609ea-281c-4fe9-8737-1479837f243c','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','ec7ae38b-00ff-4d47-a9f0-540e354bd711','receipt=RCP-20260724-0010,total=35.0','2026-07-24T15:35:53.845606100');
INSERT INTO "audit_logs" VALUES ('5344f887-e67c-4238-a25c-06969a25bcf5','a23a9e6e-df52-49b0-bf8d-577b8d3462af','SALE_COMPLETED','3d8e9bff-9299-4711-823d-3783089bb1a7','receipt=RCP-20260724-0011,total=70.0','2026-07-24T16:50:25.334011100');
INSERT INTO "audit_logs" VALUES ('f9e2e0d2-f403-405c-b939-58b77ea66aa1','a23a9e6e-df52-49b0-bf8d-577b8d3462af','LOGIN','a23a9e6e-df52-49b0-bf8d-577b8d3462af','role=ADMIN','2026-07-24T18:01:14.338284400');
INSERT INTO "audit_logs" VALUES ('322b79f3-91bc-4ca6-9eda-e09179710201','a23a9e6e-df52-49b0-bf8d-577b8d3462af','PRODUCT_UPDATED','9ae586d9-73fa-47a1-8b66-2c99c46c3173','name=Mfalme Tissue','2026-07-24T18:09:19.736099400');
INSERT INTO "categories" VALUES ('7d817fac-4835-443e-a1a7-e907024cfef2','General',NULL,'SYNCED','2026-07-22T17:01:00.391543500','2026-07-22T17:01:00.391543500');
INSERT INTO "categories" VALUES ('1c69c01f-28f9-40ad-8905-c669107afe6b','Food & Beverages',NULL,'SYNCED','2026-07-22T17:01:00.521507300','2026-07-22T17:01:00.521507300');
INSERT INTO "categories" VALUES ('617a4610-d564-45ff-b6aa-30fbb73f9b1c','Electronics',NULL,'SYNCED','2026-07-22T17:01:00.598935500','2026-07-22T17:01:00.598935500');
INSERT INTO "categories" VALUES ('ba79ace6-3873-4925-89cd-a73ab9ccd215','Clothing',NULL,'SYNCED','2026-07-22T17:01:00.680853600','2026-07-22T17:01:00.680853600');
INSERT INTO "categories" VALUES ('b60c1464-b5c4-420b-964b-49fbea48c96b','Household',NULL,'SYNCED','2026-07-22T17:01:00.750973100','2026-07-22T17:01:00.750973100');
INSERT INTO "categories" VALUES ('3e7701be-013e-49dd-ab72-708ab21cd095','General',NULL,'SYNCED','2026-07-22T17:01:00.750973100','2026-07-22T17:01:00.391543500');
INSERT INTO "categories" VALUES ('1ed51f2c-be00-4b35-a48c-e61c315e82a3','Household',NULL,'SYNCED','2026-07-22T17:01:00.750973100','2026-07-22T17:01:00.391543500');
INSERT INTO "categories" VALUES ('98439c44-dc22-432b-ac08-9ea35f80ee84','Food & Beverages',NULL,'SYNCED','2026-07-22T17:01:00.750973100','2026-07-22T17:01:00.391543500');
INSERT INTO "categories" VALUES ('bb9a2d3c-a3d9-4d54-9360-79bb88d725b6','Clothing',NULL,'SYNCED','2026-07-22T17:01:00.750973100','2026-07-22T17:01:00.391543500');
INSERT INTO "categories" VALUES ('2cbdaf4d-2a5c-40db-89b6-baea41b69dda','Clothing',NULL,'SYNCED','2026-07-23T17:54:06','2026-07-23T17:54:06');
INSERT INTO "categories" VALUES ('6e1814a1-9176-408f-b0cc-a9a92bc3c357','General',NULL,'SYNCED','2026-07-23T17:54:06','2026-07-23T17:54:06');
INSERT INTO "categories" VALUES ('929395f4-a7f5-4bbf-843c-96673a8a7bec','Household',NULL,'SYNCED','2026-07-23T17:54:06','2026-07-23T17:54:06');
INSERT INTO "categories" VALUES ('b57c01d7-5495-4fdb-983b-77668cd1c006','Food & Beverages',NULL,'SYNCED','2026-07-23T17:54:06','2026-07-23T17:54:06');
INSERT INTO "categories" VALUES ('c537aea9-f4ec-40c1-814e-da16ef76cc63','Electronics',NULL,'SYNCED','2026-07-23T17:54:06','2026-07-23T17:54:06');
INSERT INTO "products" VALUES ('703ce993-c0c6-4988-b761-0be633222e18','6161113940134',NULL,'6161113940134','Ajab Fortified All Purpose Home Baking Wheat Flour','1c69c01f-28f9-40ad-8905-c669107afe6b',90.0,100.0,100.0,27,10,0.0,0.0,NULL,'Fortified All Purpose Home Baking Flour','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-42b2db21-8847-4df4-80f0-c65066e54b8d.png;C:\Users\Victorious\AppData\Local\RetailPOS\images\product-21728c4a-b6b2-405a-93c4-c2ce951c98d7.png','packets','active',1,'SYNCED',2,'2026-07-22T23:33:41.722084900','2026-07-23T01:34:55.044925600',NULL,50);
INSERT INTO "products" VALUES ('bfbf8bcc-41ab-46dd-baa8-3afa5ac0bb1c','7171109930154',NULL,'7171109930079','ASIS Premium Tangawizi-15G','1c69c01f-28f9-40ad-8905-c669107afe6b',5.0,10.0,10.0,3,20,0.0,0.0,NULL,'15 g Asis High Quality Kenya Tea','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-2b681a62-1c53-45c4-9047-b051765785ab.png','sachets','active',0,'SYNCED',3,'2026-07-22T23:42:10.374967500','2026-07-23T15:46:16.369602600',NULL,20);
INSERT INTO "products" VALUES ('c0ec287b-220d-467a-a2a6-ce52746ed6bf','61614208',NULL,'61614208','Vaseline BlueSeal Cocoa Butter 95g','7d817fac-4835-443e-a1a7-e907024cfef2',130.0,140.0,140.0,12,10,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-fe4246a8-735e-4c3f-928b-7582daac9595.png','pcs','active',0,'SYNCED',1,'2026-07-22T23:46:49.089781600','2026-07-22T23:46:49.089781600',NULL,20);
INSERT INTO "products" VALUES ('aac1f838-5ad0-402d-9c3b-105e3df34d22','6161107774059',NULL,'6161107774059','KIMBO 500G','1c69c01f-28f9-40ad-8905-c669107afe6b',165.0,180.0,180.0,6,15,0.0,0.0,NULL,'Pure vegetable fat','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-2c409c8e-3ee0-46d0-9ec7-7e3fb4438e02.png','pcs','active',0,'SYNCED',1,'2026-07-22T23:52:12.917425700','2026-07-22T23:52:12.917425700',NULL,50);
INSERT INTO "products" VALUES ('d4186bf3-2178-4044-ade4-728a6dd6fc7b','61611101660211',NULL,'61611101660211','Tilly Cooking Fat','1c69c01f-28f9-40ad-8905-c669107afe6b',15.0,20.0,20.0,27,15,0.0,0.0,NULL,'Fortified Edible Vegetable Cooking Fat','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-6fce86c7-01c2-4368-8fe8-70958fc4523d.png','sachets','active',0,'MODIFIED',1,'2026-07-22T23:58:50.973386100','2026-07-23T19:31:45.932485600',NULL,30);
INSERT INTO "products" VALUES ('126560a6-2315-44f3-bb4c-12528dc8b9b9','8718951308213',NULL,'8718951308213','Colgate','7d817fac-4835-443e-a1a7-e907024cfef2',30.0,40.0,40.0,24,10,0.0,0.0,NULL,'Colgate Maximum Cavity Protection','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-1906b4ed-f2bf-407a-8b37-2d69014ea048.jpg','sachets','active',0,'SYNCED',1,'2026-07-23T00:08:06.706908','2026-07-23T00:08:06.706908',NULL,20);
INSERT INTO "products" VALUES ('6f36e781-fb79-4824-8513-a19c4d652a07','6008677002277',NULL,'6008677002277','Pure Glucose','1c69c01f-28f9-40ad-8905-c669107afe6b',20.0,25.0,25.0,14,10,0.0,0.0,NULL,'Pure glucose Instant energy','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-356fd0a9-6587-4842-83b8-12dde8ba26d4.jpg','boxes','active',0,'SYNCED',1,'2026-07-23T00:16:51.924956300','2026-07-23T00:16:51.924956300',NULL,20);
INSERT INTO "products" VALUES ('b3843ebe-3392-4e56-b4e5-5f7620292172','6164001283344',NULL,'6164001283344','Barjot Office Glue 90gms','7d817fac-4835-443e-a1a7-e907024cfef2',30.0,40.0,40.0,10,5,0.0,0.0,NULL,'Barjot Office glue','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-0e18b136-c71e-41be-baa9-8d3f6cee6b6b.jpg','bottles','active',0,'SYNCED',1,'2026-07-23T00:23:29.183462900','2026-07-23T00:23:29.183462900',NULL,20);
INSERT INTO "products" VALUES ('2c3a808c-7fb2-4b4a-8c10-3f983906721f','6009611170274',NULL,'6009611170274','Mara Moja','1ed51f2c-be00-4b35-a48c-e61c315e82a3',8.0,10.0,10.0,30,15,0.0,0.0,NULL,'Mara Moja Kama umeme','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-d16c29d7-e728-4d79-8592-853b021be386.png;C:\Users\Victorious\AppData\Local\RetailPOS\images\product-eeeccb4d-1617-4ef2-ba87-aeb21eaed29b.png','pairs','active',0,'SYNCED',2,'2026-07-23T00:26:48.863978100','2026-07-23T00:31:52.244562200',NULL,30);
INSERT INTO "products" VALUES ('5b0635b0-07ea-42c4-8a05-550542d8f55b','6161105711469',NULL,'6161105711469','Kaluma Pain Palm','7d817fac-4835-443e-a1a7-e907024cfef2',45.0,50.0,50.0,5,15,0.0,0.0,NULL,'Kaluma Pain Palm','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-1a7f59f3-8aa7-439c-b8b6-a7a3f44701bc.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T00:29:37.575688200','2026-07-23T00:29:37.575688200',NULL,30);
INSERT INTO "products" VALUES ('24480d20-68c0-4532-bde2-f9fd388b9f22','68686102525',NULL,'68686102525','Panadol Advanced','7d817fac-4835-443e-a1a7-e907024cfef2',15.0,20.0,20.0,47,20,0.0,0.0,NULL,'Panadol Advanced','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-b394b32e-b07b-40d8-9433-347b965d8165.jpg','pairs','active',0,'MODIFIED',1,'2026-07-23T00:37:10.129930200','2026-07-24T13:21:52.192904800',NULL,50);
INSERT INTO "products" VALUES ('88e49a6f-c50f-4d7d-bd9b-dd9230e042fe','6739225235815',NULL,'6739225235815','Baida Lighter','b60c1464-b5c4-420b-964b-49fbea48c96b',10.0,30.0,30.0,47,10,0.0,0.0,NULL,'Baida Lighter','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-fd961352-5b98-47a4-ab37-08b9afe5ed35.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T00:41:55.204524900','2026-07-23T00:41:55.204524900',NULL,50);
INSERT INTO "products" VALUES ('117fee71-adbe-40bd-9d62-17ab73f2bce7','6161100882447',NULL,'6161100882447','Valon Skin Care 95ml','7d817fac-4835-443e-a1a7-e907024cfef2',120.0,130.0,130.0,7,5,0.0,0.0,NULL,'Valon Skin Care','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-8efee5df-36a9-49d3-b1d4-7d8f2afcf220.jpg;C:\Users\Victorious\AppData\Local\RetailPOS\images\product-09059a9f-d3cc-4f55-a36a-2d77c600413e.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T00:51:55.362663500','2026-07-23T00:51:55.362663500',NULL,20);
INSERT INTO "products" VALUES ('96ab684c-b6fd-43a8-bda6-3be54992463b','6008677006053',NULL,'6008677006053','Arimis Milking Jelly 200ml','7d817fac-4835-443e-a1a7-e907024cfef2',85.0,100.0,100.0,2,5,0.0,0.0,NULL,'Arimis Milking Jelly','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-d2356575-0e27-4f34-aae5-4eee9b2da997.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T00:57:24.157166300','2026-07-23T00:57:24.157166300',NULL,20);
INSERT INTO "products" VALUES ('b2288b18-3a3b-4e36-8f82-9ea8b53bc256','6164004192056',NULL,'6164004192056','Laxmi Green Grams 500g','1c69c01f-28f9-40ad-8905-c669107afe6b',80.0,90.0,90.0,6,5,0.0,0.0,NULL,'Laximi Green Grams','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-fed94015-158b-4cf3-a732-7d21ff71f18f.png','packets','active',0,'SYNCED',2,'2026-07-23T01:05:23.925413300','2026-07-23T01:06:04.053996',NULL,20);
INSERT INTO "products" VALUES ('0728ac39-e781-4a0f-b779-6bbd5da4dee9','6164004676556',NULL,'6164004676556','Blueband Spread for bread 100g','1c69c01f-28f9-40ad-8905-c669107afe6b',53.0,70.0,65.0,3,5,0.0,0.0,NULL,'Blueband Low Fat Spread','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-1c631079-5e70-472a-b3fd-7ce70c5bf2d2.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T01:13:29.684161700','2026-07-23T01:13:29.684161700',NULL,10);
INSERT INTO "products" VALUES ('2cfaba05-01a8-4a8c-8ff8-03466526f694','6614000',NULL,'6614000','Blueband Original 100g','1c69c01f-28f9-40ad-8905-c669107afe6b',60.0,70.0,65.0,6,5,0.0,0.0,NULL,'Blueband Original 100g','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-1957fd60-df3c-435a-828f-33e35c586773.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T01:18:41.676366100','2026-07-23T01:18:41.676366100',NULL,20);
INSERT INTO "products" VALUES ('c97fbd5a-7186-45bc-9d2d-feadb41b943b','8719200286733',NULL,'8719200286733','Blueband With Choco 100g','1c69c01f-28f9-40ad-8905-c669107afe6b',60.0,70.0,65.0,3,5,0.0,0.0,NULL,'Blueband with choco','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-33304c0f-2eff-4327-bb20-fd9663e0738d.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T01:22:33.359275100','2026-07-23T01:22:33.359275100',NULL,20);
INSERT INTO "products" VALUES ('b2713b07-0067-4411-a5d6-114be39ad259','6161113940127',NULL,'6161113940127','Ajab Fortified All Purpose Home Baking Flour 500g','1c69c01f-28f9-40ad-8905-c669107afe6b',50.0,55.0,55.0,15,10,0.0,0.0,NULL,'Ajab Fortified All Purpose Home Baking Wheat Flour','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-0fd57d11-dff3-4d95-9b26-a0ce312b9468.png','pcs','active',0,'MODIFIED',1,'2026-07-23T01:34:11.594281900','2026-07-24T16:50:25.224585300',NULL,20);
INSERT INTO "products" VALUES ('78c19486-8d62-46a0-8b8d-30f14375f64e','6161106962662',NULL,'6161106962662','Nescafe Classic 1.5 g','1c69c01f-28f9-40ad-8905-c669107afe6b',8.0,10.0,10.0,20,10,0.0,0.0,NULL,'Nescafe Classic 1.5g','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-3a7495d8-c305-4dfa-bd81-5f583d811f1f.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T01:40:03.878846','2026-07-23T19:31:45.971786200',NULL,30);
INSERT INTO "products" VALUES ('ad049a23-82cd-45cb-a2cb-35ad2999c562','6161100460126',NULL,'6161100460126','KCC FRESH WHOLE MILK 200ML','1c69c01f-28f9-40ad-8905-c669107afe6b',30.0,35.0,35.0,4,5,0.0,0.0,NULL,'KCC FRESH WHOLE MILK','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-43000e6b-add3-4d49-b80c-6e3c03968df0.jpg','packets','active',0,'MODIFIED',1,'2026-07-23T01:44:52.732268400','2026-07-24T15:35:53.711060700',NULL,20);
INSERT INTO "products" VALUES ('ffdfd257-8190-4423-8337-9cc399a50320','6161109980861',NULL,'6161109980861','Fresha Maisha Long Life Whole Milk 200ml','1c69c01f-28f9-40ad-8905-c669107afe6b',30.0,35.0,35.0,6,5,0.0,0.0,NULL,'Maisha Long Life Whole Milk','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-c8c2b7ff-1bad-4b24-94e3-8e621f04021e.jpg','packets','active',0,'SYNCED',1,'2026-07-23T01:48:21.675987','2026-07-23T01:48:21.675987',NULL,20);
INSERT INTO "products" VALUES ('cbc73ad8-8919-4840-88ad-91693f44e42f','6161100907140',NULL,'6161100907140','Menendazi Pure Baking Powder','1c69c01f-28f9-40ad-8905-c669107afe6b',30.0,40.0,40.0,6,5,0.0,0.0,NULL,'Menendazi Pure Baking Powder','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-3d6544bb-7089-424b-a145-6c891ac50248.jpg','packets','active',0,'SYNCED',1,'2026-07-23T01:53:50.682078700','2026-07-23T01:53:50.682078700',NULL,10);
INSERT INTO "products" VALUES ('13fa49d0-9946-4d57-9747-fbe0726d435c','6161100460119',NULL,'6161100460119','KCC Fresh Whole Milk 500g','1c69c01f-28f9-40ad-8905-c669107afe6b',70.0,75.0,75.0,12,10,0.0,0.0,NULL,'KCC Fresh Milk','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-11060e26-7a1b-45fe-b27f-7beda9407533.jpeg','packets','active',0,'SYNCED',1,'2026-07-23T01:58:01.687921300','2026-07-23T01:58:01.687921300',NULL,20);
INSERT INTO "products" VALUES ('05f2201b-f789-4dce-8dc7-dcef04f4b36c','16100100077',NULL,'16100100077','Mount Kenya Milk 500ml','1c69c01f-28f9-40ad-8905-c669107afe6b',70.0,75.0,75.0,10,5,0.0,0.0,NULL,'Mount Kenya Milk Dairy Kubwa Choice',NULL,'pcs','active',0,'SYNCED',1,'2026-07-23T02:02:51.803784300','2026-07-23T02:02:51.803784300',NULL,20);
INSERT INTO "products" VALUES ('91571c23-8e2c-4ac1-8c50-c907b8d74121','6940988800206',NULL,'6940988800206','Cyano Acrylate Adhesive super glue','7d817fac-4835-443e-a1a7-e907024cfef2',30.0,35.0,35.0,10,10,0.0,0.0,NULL,'Acrylate Cyano Adhesice Super Glue','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-f64c3002-49bb-41c4-8bf5-8d178921e342.jpg','pcs','active',0,'MODIFIED',2,'2026-07-23T02:07:28.525528100','2026-07-24T15:31:17.855093700',NULL,20);
INSERT INTO "products" VALUES ('2d3a0fbe-014b-4230-81ae-ffc76eea1a4f','616110571131',NULL,'616110571131','Kaluma Strong','7d817fac-4835-443e-a1a7-e907024cfef2',8.0,10.0,10.0,9,5,16.0,0.0,NULL,'Kaluma Strong','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-8c5c0194-1816-4510-a0ce-d37cfea6a49a.png','pairs','active',0,'MODIFIED',2,'2026-07-23T02:09:50.165919400','2026-07-23T19:24:21.361783100',NULL,20);
INSERT INTO "products" VALUES ('219d5be6-9c47-43b1-b5f4-4b7082cab4fd','6161100907003',NULL,'6161100907003','Menangai Cream Quality Washing Bar 800g','7d817fac-4835-443e-a1a7-e907024cfef2',45.0,50.0,50.0,20,10,0.0,0.0,NULL,'Quality Washing Bar Soap','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-0a191d5f-c22e-43b7-8097-e303afa8c1d1.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T03:55:46.173519600','2026-07-23T03:55:46.173519600',NULL,20);
INSERT INTO "products" VALUES ('1df8138b-5d43-4e7e-8237-69eb273b6681','6161100907034',NULL,'6161100907034','Menengai Cream Quality Washing Bar 1kg','7d817fac-4835-443e-a1a7-e907024cfef2',45.0,50.0,50.0,41,10,0.0,0.0,NULL,'Quality Washing Bar Soap','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-16d352dd-f293-483c-8228-0059cfc40ddb.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T03:58:56.957497500','2026-07-24T14:30:52.132165600',NULL,20);
INSERT INTO "products" VALUES ('87fc7dd9-ffc7-4974-979f-1861bdce5f29','6161110130835',NULL,'6161110130835','ZENTA Multipurpose Washing Bar 800 g','7d817fac-4835-443e-a1a7-e907024cfef2',30.0,40.0,40.0,52,20,0.0,0.0,NULL,'Zenta Multipurpose Washing Bar','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-7c68a572-b056-4d6a-926d-eb5c59993fe1.jpg','pcs','active',0,'SYNCED',2,'2026-07-23T04:05:09.076387100','2026-07-23T04:08:52.851917200',NULL,50);
INSERT INTO "products" VALUES ('c2c5f064-09de-427d-bd9c-228527dde863','6161110130675',NULL,'6161110130675','Zenta Multipurpose Washing Bar 1kg','7d817fac-4835-443e-a1a7-e907024cfef2',38.0,50.0,50.0,25,10,0.0,0.0,NULL,'Zenta Multi purpose washing soap 1 kg','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-df2e0cde-719c-43d2-a244-cb456d4eecbb.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T04:08:28.419684800','2026-07-23T04:08:28.419684800',NULL,50);
INSERT INTO "products" VALUES ('e63744cd-09e1-4a40-82cf-75e76647f7b7','6161101667111',NULL,'6161101667111','Jamaa White Washing Bar','7d817fac-4835-443e-a1a7-e907024cfef2',30.0,40.0,40.0,3,10,0.0,0.0,NULL,'Jamaa Washing Bar','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-48693e41-0214-45b3-8888-a9c1d0921e00.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T04:12:26.290634500','2026-07-24T15:31:17.827099900',NULL,30);
INSERT INTO "products" VALUES ('4b78edeb-402a-4abc-9bba-92ba059cb22e','09607673321',NULL,'09607673321','Kifaru Safety Matches','ba79ace6-3873-4925-89cd-a73ab9ccd215',4.0,5.0,5.0,114,20,0.0,0.0,NULL,'Kifaru Safety Matches','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-d8c7fabe-3c1f-4d78-bbde-d32f09e40bb4.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T04:16:24.466226100','2026-07-23T04:16:24.466226100',NULL,150);
INSERT INTO "products" VALUES ('565c4288-b7ed-4c23-9e01-9294e1e98279','6164000015106',NULL,'6164000015106','Tobex Bleach 70 ml','7d817fac-4835-443e-a1a7-e907024cfef2',30.0,35.0,35.0,2,5,0.0,0.0,NULL,'Topex Bleach 70ml','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-58d7c05e-37b0-458c-9259-3f8ad9863f2e.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T04:19:48.993640300','2026-07-24T15:31:17.758295900',NULL,20);
INSERT INTO "products" VALUES ('61d42fb4-9c5a-4501-9233-a784707e13fe','6164004605426',NULL,'6164004605426','Eno Fruit Salt','ba79ace6-3873-4925-89cd-a73ab9ccd215',15.0,20.0,20.0,39,10,0.0,0.0,NULL,'Eno Fruit Salt Works on six symptoms of heart burn','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-05bbf55e-bd9e-45a7-9cb2-d5bb1f99245d.jpg','sachets','active',0,'SYNCED',1,'2026-07-23T04:25:29.005863300','2026-07-23T04:25:29.005863300',NULL,50);
INSERT INTO "products" VALUES ('4920d20d-5ffd-40c9-89cd-d38f2427b222','6161101571517',NULL,'6161101571517','White Dent Advanced ToothBrush','7d817fac-4835-443e-a1a7-e907024cfef2',50.0,60.0,60.0,9,10,0.0,0.0,NULL,'WhiteDent Advanced ToothBrush','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-a7c1a7df-62bb-486d-b951-08efb5320a54.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T04:29:27.984871600','2026-07-23T04:29:27.984871600',NULL,20);
INSERT INTO "products" VALUES ('80b3844a-fce8-4d3f-a16b-10e3dc2725c0','5060608740253',NULL,'5060608740253','Predator Energy Drink','1c69c01f-28f9-40ad-8905-c669107afe6b',68.0,70.0,70.0,17,10,0.0,0.0,NULL,'Predator Energy Drink','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-a1d45a05-a442-4e45-ac80-a59fd6dadb71.jpg','bottles','active',0,'SYNCED',1,'2026-07-23T04:33:45.962792200','2026-07-23T04:33:45.962792200',NULL,30);
INSERT INTO "products" VALUES ('df03622c-eeab-4539-8175-097a390a2514','6971663563420',NULL,'6971663563420','OLEMOL TOOTH BRUH','7d817fac-4835-443e-a1a7-e907024cfef2',35.0,40.0,40.0,12,5,0.0,0.0,NULL,'OLEMOL TOOTHBRUSH','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-19d4ea0e-e410-445b-9900-27a7f0c65f59.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T04:37:00.231694500','2026-07-23T04:37:00.231694500',NULL,20);
INSERT INTO "products" VALUES ('822c3d74-e988-4c99-b211-b4414304ceee','6161101830010',NULL,'6161101830010','TP Tishu Poa 100 X 125 mm',NULL,18.0,20.0,20.0,12,5,0.0,0.0,NULL,'TP Tishu Poa','/pos.victoriousgeneralshop.com/uploads/products/822c3d74-e988-4c99-b211-b4414304ceee-0-f4ed28a56741.jpg','rolls','active',0,'SYNCED',3,'2026-07-23T04:42:36','2026-07-23T17:40:02','2026-07-23T17:40:02',20);
INSERT INTO "products" VALUES ('3d0e94d4-fdde-4406-a4a0-88c67af1484e','0762497597547',NULL,'0762497597547','KWETU Softness You Can Trust','7d817fac-4835-443e-a1a7-e907024cfef2',18.0,20.0,20.0,1,10,0.0,0.0,NULL,'Kwetu Softness You Can Trust','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-b94e0668-cb4c-43c0-9987-faff07120091.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T04:48:47.657413800','2026-07-23T04:48:47.657413800',NULL,20);
INSERT INTO "products" VALUES ('9ae586d9-73fa-47a1-8b66-2c99c46c3173','6164002695474',NULL,'6164002695474','Mfalme Tissue','7d817fac-4835-443e-a1a7-e907024cfef2',18.0,20.0,20.0,3,5,0.0,0.0,NULL,'Mfalme Tissue','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-26b3e0ad-a109-40e7-b9d3-96a42783987c.jpg','pcs','active',0,'MODIFIED',2,'2026-07-23T04:51:45.972224800','2026-07-24T18:09:19.673604500',NULL,20);
INSERT INTO "products" VALUES ('28b8bd45-61d1-473e-ae5d-8af659b94c04','0735745028520',NULL,'0735745028520','Tuffy Feel The Softness 100mm X 125 mm','7d817fac-4835-443e-a1a7-e907024cfef2',18.0,20.0,20.0,6,5,0.0,0.0,NULL,'Feel the softness','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-18deb5f9-1c44-4a8c-acc7-13e1a3da9f1d.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T04:56:17.922863200','2026-07-23T04:56:17.922863200',NULL,20);
INSERT INTO "products" VALUES ('151b9723-7b8a-4390-8194-6e5a3878cc80','0792382597352',NULL,'0792382597352','Dalia Luxurious Soft White Tissue','617a4610-d564-45ff-b6aa-30fbb73f9b1c',18.0,20.0,20.0,10,10,0.0,0.0,NULL,'Dalia Luxurious sof white tissue','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-1e2bdf2c-5eed-4457-bb70-355fb4c0d0c0.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T14:58:21.396203200','2026-07-24T14:49:16.267767600',NULL,20);
INSERT INTO "products" VALUES ('82961b1e-6433-4cf6-83e3-110dad731396','6009614480462',NULL,'6009614480462','Rosy Extra Strong Tissue white Colour',NULL,18.0,20.0,20.0,4,10,0.0,0.0,NULL,'Rosy Extra Strong Tissue','/pos.victoriousgeneralshop.com/uploads/products/82961b1e-6433-4cf6-83e3-110dad731396-0-a9646fa71f29.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T15:05:30','2026-07-23T17:40:02','2026-07-23T17:40:02',20);
INSERT INTO "products" VALUES ('06a0da23-fd56-43b4-818e-707c0837f3f3','6161100762411',NULL,'6161100762411','Rosy Extra Strong Tissue Pink color','bb9a2d3c-a3d9-4d54-9360-79bb88d725b6',18.0,20.0,20.0,22,10,0.0,0.0,NULL,'Rosy Extra Strong Tissue','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-59317cf7-fd78-4833-bf20-a28d81952d32.jpg','rolls','active',0,'MODIFIED',1,'2026-07-23T15:09:34.480020700','2026-07-24T12:27:29.094959200',NULL,20);
INSERT INTO "products" VALUES ('12be2e1f-71b1-45dc-9e3c-4deadc38b96c','6161101280037',NULL,'6161101280037','KENSALT  Iodated Edible Table Salt 200g','7d817fac-4835-443e-a1a7-e907024cfef2',10.0,15.0,15.0,59,10,0.0,0.0,NULL,'Kensalt Iodate Edible Table Salt','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-863a0889-a34e-4096-9811-d0fd61a9ef57.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T15:58:26.050707','2026-07-24T16:50:25.287139700',NULL,20);
INSERT INTO "products" VALUES ('e0101a7a-d845-408c-811b-5b9f967a984d','6161101280013',NULL,'6161101280013','Kensalt 500g','1c69c01f-28f9-40ad-8905-c669107afe6b',20.0,25.0,25.0,53,20,0.0,0.0,NULL,'iodated edible table salt','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-ff53cb88-93cc-4868-92c1-b245e3b84cf8.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T16:43:45.501508400','2026-07-23T16:43:45.501508400',NULL,30);
INSERT INTO "products" VALUES ('3b0d7323-d7d5-45e4-8429-73abb4ad78f7','40822921',NULL,'40822921','Fanta-Orange Soda  300ml','1c69c01f-28f9-40ad-8905-c669107afe6b',33.0,50.0,36.0,12,2,0.0,0.0,NULL,'fanta orange','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-249cc5c2-f4c8-4660-9756-3e7c97f13f1f.jpg','pcs','active',0,'SYNCED',2,'2026-07-23T17:08:22.945662400','2026-07-23T17:15:49.079201300',NULL,7);
INSERT INTO "products" VALUES ('f975304d-916e-48a4-8aa2-0d66fcf3385a','87126037',NULL,'87126037','Coke soda 300 ml','1c69c01f-28f9-40ad-8905-c669107afe6b',33.0,50.0,36.0,15,4,0.0,0.0,NULL,'most popular soda','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-306d79cf-01c3-42b6-b566-d062f292d03f.jpg','pcs','active',0,'SYNCED',2,'2026-07-23T17:14:52.483320500','2026-07-23T18:27:11.846118900',NULL,7);
INSERT INTO "products" VALUES ('353914f2-3fed-48cb-bfd6-a3e179c9593b','90495090',NULL,'90495090','Fanta-Orange Soda  200ml','1c69c01f-28f9-40ad-8905-c669107afe6b',15.0,25.0,20.0,14,10,0.0,0.0,NULL,'classic 200ml Fanta Orange soda','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-20bb4c93-c7ba-49fb-9c3c-f6a7e66dfbc7.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T17:20:16.900002500','2026-07-24T15:31:17.882327700',NULL,24);
INSERT INTO "products" VALUES ('e05ba1a5-7ec9-493a-9634-8268c241871b','Ana/1/4',NULL,'Ana/1/4','Anab Black Rice 1/4kg','1c69c01f-28f9-40ad-8905-c669107afe6b',38.0,45.0,43.0,20,5,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-357cbb9f-ff0c-4dfa-bf78-fd369eb12e36.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T17:33:00.847178600','2026-07-23T17:33:00.847178600',NULL,20);
INSERT INTO "products" VALUES ('8f82ae51-b4de-4c33-803e-c0ddfcdd59fb','Ana/1/2',NULL,'Ana/1/2','Anab Black Rice 1/2kg','1c69c01f-28f9-40ad-8905-c669107afe6b',76.0,85.0,85.0,10,10,0.0,0.0,NULL,'Nutritious Whole Grain','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-413fd8d1-b1c9-4906-928d-131e79d00a4e.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T17:44:00.080785800','2026-07-23T17:44:00.080785800',NULL,20);
INSERT INTO "products" VALUES ('d09b6727-f43d-4f6c-90b5-e83622ccba88','Ana/1',NULL,'Ana/1','Anab Black Rice 1kg','1c69c01f-28f9-40ad-8905-c669107afe6b',150.0,165.0,160.0,10,5,0.0,0.0,NULL,'Healthy Ancient Grain','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-bb1e2495-ee68-40a6-a8fe-5aabe3d6a2e0.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T17:47:02.549205400','2026-07-23T17:47:02.549205400',NULL,10);
INSERT INTO "products" VALUES ('cf15b13f-bc32-485c-a946-9bac5fa5a504','Kan/1/4',NULL,'Kan/1/4','Kangore Rice 1/4kg','1c69c01f-28f9-40ad-8905-c669107afe6b',23.0,30.0,30.0,20,10,0.0,0.0,NULL,'Naturally Aromatic Rice','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-e4d2047e-5d9c-4f33-bf03-7d72ff6d3117.jpg','pcs','active',0,'SYNCED',2,'2026-07-23T17:52:13.346583300','2026-07-23T17:52:58.143252700',NULL,20);
INSERT INTO "products" VALUES ('b38b209d-00a5-4592-bf9c-d209f1646a95','Kan/1/2',NULL,'Kan/1/2','Kangore Rice 1/2kg','1c69c01f-28f9-40ad-8905-c669107afe6b',46.0,60.0,60.0,10,0,0.0,0.0,NULL,'Naturally Aromatic Rice','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-2b9e97e3-6588-43a9-ad28-391d2d918123.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T17:56:00.534254400','2026-07-23T17:56:00.534254400',NULL,0);
INSERT INTO "products" VALUES ('7b221a0d-8e5f-4e02-9af9-206d6e38848e','kan/1',NULL,'kan/1','Kangore Rice 1kg','1c69c01f-28f9-40ad-8905-c669107afe6b',87.0,120.0,120.0,10,5,0.0,0.0,NULL,'Naturally Aromatic Rice','C:\Users\Victorious\AppData\Local\RetailPOS\images\product-08b8a512-ac7d-44e9-839d-3e7a5974e451.jpg','pcs','active',0,'SYNCED',2,'2026-07-23T17:58:00.736842800','2026-07-23T17:58:16.255288400',NULL,10);
INSERT INTO "products" VALUES ('eb289aa2-c672-4d07-89ba-ca4fc7b56d2e','5449000004840',NULL,'5449000004840','Fanta-Orange 2L Soda','1c69c01f-28f9-40ad-8905-c669107afe6b',177.0,200.0,195.0,11,3,16.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-88c29e1c-5812-472a-b985-6ad05b567942.jpg','pcs','active',0,'SYNCED',2,'2026-07-23T18:07:16.722344500','2026-07-23T18:43:13.315001700',NULL,6);
INSERT INTO "products" VALUES ('15ee0340-0336-47c7-876b-bfbe4fbc1ae0','5449000090096',NULL,'5449000090096','Fanta-Passion Soda 2L','1c69c01f-28f9-40ad-8905-c669107afe6b',177.0,200.0,195.0,6,3,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-17840a14-98f1-43df-8290-6f5d76e3edf3.jpg','pcs','active',0,'SYNCED',3,'2026-07-23T18:11:23.346486','2026-07-23T18:42:47.925412300',NULL,6);
INSERT INTO "products" VALUES ('4618b6b1-f906-4980-909a-35a8bc9e6857','544900022752',NULL,'544900022752','Fanta Black-Current Soda 2L','1c69c01f-28f9-40ad-8905-c669107afe6b',177.0,200.0,195.0,1,3,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-62b0a7d7-0063-42a1-90b3-139aac6f0268.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T18:16:49.675757600','2026-07-23T18:16:49.675757600',NULL,6);
INSERT INTO "products" VALUES ('724a63a9-11cc-4ab7-890c-0b72766707ad','5449000000286',NULL,'5449000000286','Coke Soda 2L','1c69c01f-28f9-40ad-8905-c669107afe6b',177.0,200.0,195.0,3,3,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-d82acd2b-d17c-41c6-925f-eb531831be77.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T18:20:54.613544100','2026-07-23T18:20:54.613544100',NULL,6);
INSERT INTO "products" VALUES ('4c0ace44-b8c0-4bdb-9d87-57430dc46d82','5449000054227',NULL,'5449000054227','Coke Soda 1L','1c69c01f-28f9-40ad-8905-c669107afe6b',88.0,100.0,95.0,4,3,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-e067f8d1-3316-4041-8913-7a006057e1ad.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T18:26:44.643278','2026-07-23T18:26:44.643278',NULL,3);
INSERT INTO "products" VALUES ('5b075ce4-082f-4d95-84e5-90652c8e13ee','5449000084460',NULL,'5449000084460','Fanta Black-Current Soda 1L','1c69c01f-28f9-40ad-8905-c669107afe6b',88.0,100.0,95.0,4,3,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-428d2ff9-a4d3-4b46-9e31-2bfbfca0632e.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T18:30:01.674245600','2026-07-23T18:30:01.674245600',NULL,6);
INSERT INTO "products" VALUES ('d14e4361-dae2-4ccc-8284-bb7c370fa32d','50112173',NULL,'50112173','Fanta - Orange Soda 500ml','1c69c01f-28f9-40ad-8905-c669107afe6b',50.0,60.0,60.0,14,5,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-9830c297-4c07-48a3-979e-56c5b340938d.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T18:37:03.502535700','2026-07-24T14:28:04.965982',NULL,10);
INSERT INTO "products" VALUES ('3a66f17f-2d9e-449e-be9e-44e9081d8944','42117131',NULL,'42117131','Coke soda 350 ml','1c69c01f-28f9-40ad-8905-c669107afe6b',40.0,50.0,50.0,2,1,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-26d3db40-13c5-4f3b-bd52-7ae0a6e9a03d.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T18:41:27.752559600','2026-07-23T18:41:27.752559600',NULL,10);
INSERT INTO "products" VALUES ('e84650a6-7ea8-421c-9b54-d2355e14bed9','54490123',NULL,'54490123','Coke Soda 500ml','1c69c01f-28f9-40ad-8905-c669107afe6b',50.0,60.0,60.0,12,5,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-0138a91a-71b1-4341-b38e-372166019d0b.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T18:47:56.721950200','2026-07-23T18:47:56.721950200',NULL,12);
INSERT INTO "products" VALUES ('625f2f14-b4c5-4dbb-8747-5a976def1525','54491182',NULL,'54491182','Krest Soda 500ml','1c69c01f-28f9-40ad-8905-c669107afe6b',50.0,60.0,60.0,5,2,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-bfcafd92-028c-4e93-a934-c8352fafb6b1.jpg','pcs','active',0,'SYNCED',2,'2026-07-23T18:51:50.393324100','2026-07-23T18:52:42.471723800',NULL,4);
INSERT INTO "products" VALUES ('0da97229-8715-4401-868e-85cae1fb677f','50112174',NULL,'50112174','Fanta Black-Current Soda 500ml',NULL,50.0,60.0,60.0,3,1,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-9fa6cd6b-6d56-4647-bb27-eb692e6f35a3.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T19:02:26.676023700','2026-07-23T19:02:26.676023700',NULL,3);
INSERT INTO "products" VALUES ('711bb791-b492-4240-b2ca-631a9446f8a1','90492112',NULL,'90492112','krest Soda 300 ml',NULL,40.0,50.0,50.0,20,5,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-a0801e7e-122b-42e9-805f-3ad2e6a0abe4.jpg','pcs','active',0,'SYNCED',1,'2026-07-23T19:06:21.018233600','2026-07-23T19:06:21.018233600',NULL,10);
INSERT INTO "products" VALUES ('e082b326-cf02-4226-8a83-48fb91da8cfd','54492691',NULL,'54492691','Sprite soda 300ml','1c69c01f-28f9-40ad-8905-c669107afe6b',40.0,50.0,50.0,7,5,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-64425b6e-6ca0-4e0f-8667-6e1ec2fda80b.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T19:12:24.873075800','2026-07-24T14:36:59.623106600',NULL,5);
INSERT INTO "products" VALUES ('a4877599-2bca-4e6f-8833-c25a4a9b134e','40822924',NULL,'40822924','Fanta-passion Soda  300ml','1c69c01f-28f9-40ad-8905-c669107afe6b',40.0,50.0,50.0,6,2,0.0,0.0,NULL,NULL,'C:\Users\Victorious\AppData\Local\RetailPOS\images\product-dee207e4-8cdc-432e-a03f-ce30954782d2.jpg','pcs','active',0,'MODIFIED',1,'2026-07-23T19:17:25.316043200','2026-07-24T12:26:13.026919300',NULL,5);
INSERT INTO "products" VALUES ('9f7bbdf0-fd2b-4761-b4db-88d1e9238870','test123',NULL,'test123','test','b57c01d7-5495-4fdb-983b-77668cd1c006',20.0,24.0,20.0,20,10,16.0,0.0,NULL,'good','C:\Users\Victorious\AppData\Local\RetailPOS\images\9f7bbdf0-fd2b-4761-b4db-88d1e9238870-0-2260d0a8fabf.png','pcs','active',0,'DELETED',1,'2026-07-24T16:02:04','2026-07-24T17:19:57','2026-07-24T17:19:57',20);
INSERT INTO "purchase_order_items" VALUES ('7352fc5e-82c6-4a70-bbf7-3f60173f59f2','7fd76498-82fe-4889-8391-d6343dcd90d9','c0ec287b-220d-467a-a2a6-ce52746ed6bf','Vaseline BlueSeal Cocoa Butter 95g',20,0,130.0);
INSERT INTO "purchase_order_items" VALUES ('22550062-1b65-447b-8431-56ae30bdb175','7fd76498-82fe-4889-8391-d6343dcd90d9','88e49a6f-c50f-4d7d-bd9b-dd9230e042fe','Baida Lighter',50,0,10.0);
INSERT INTO "purchase_orders" VALUES ('7fd76498-82fe-4889-8391-d6343dcd90d9','36c12be2-1d6c-4899-a709-ac24146a29ea','Genesis international','CANCELLED','2026-07-24',NULL,'SYNCED','2026-07-23T00:45:12.936852400','2026-07-23T00:45:51.361904500');
INSERT INTO "purchase_orders" VALUES ('cf141dd9-0871-442c-acec-dfa0b3a2b23b','87e77414-71f2-4453-9382-4435634bf5f7','bonface onduso','RECEIVED','2026-07-29',NULL,'SYNCED','2026-07-23T00:45:12.936852400','2026-07-23T00:45:51.361904500');
INSERT INTO "sale_items" VALUES ('ba40edf1-cc55-46e8-b82a-8e1610dfcbf7','e2754f55-6e5a-4e16-aa57-05c049fe6e15','2d3a0fbe-014b-4230-81ae-ffc76eea1a4f','Kaluma Strong','616110571131',1,10.0,8.0,0.0,0.0,10.0);
INSERT INTO "sale_items" VALUES ('a5a43ce3-e339-40db-b69d-80a4e911c464','ded571ff-c92e-475a-8e8f-a3be914a31c2','d4186bf3-2178-4044-ade4-728a6dd6fc7b','Tilly Cooking Fat','61611101660211',1,20.0,15.0,0.0,0.0,20.0);
INSERT INTO "sale_items" VALUES ('f1ae777c-df9a-4e15-96e7-c41830997f41','ded571ff-c92e-475a-8e8f-a3be914a31c2','78c19486-8d62-46a0-8b8d-30f14375f64e','Nescafe Classic 1.5 g','6161106962662',1,10.0,8.0,0.0,0.0,10.0);
INSERT INTO "sale_items" VALUES ('660c7e03-7e64-45ca-babd-e27f1aa09619','01f045a4-f9df-45ef-9dad-8eeb190c42ef','9ae586d9-73fa-47a1-8b66-2c99c46c3173','Mfalme Tissue','6164002695474',7,20.0,18.0,0.0,0.0,140.0);
INSERT INTO "sale_items" VALUES ('0fcbc011-5adc-4e2e-ac50-ada57cabefde','47131f5f-41b4-4c8c-a123-36bf500462ab','1df8138b-5d43-4e7e-8237-69eb273b6681','Menengai Cream Quality Washing Bar 1kg','6161100907034',4,50.0,45.0,0.0,0.0,200.0);
INSERT INTO "sale_items" VALUES ('acc0103a-379e-4211-8a51-78247485ff80','1ff28d1d-6631-4827-924e-a1b1ee5ad1bc','a4877599-2bca-4e6f-8833-c25a4a9b134e','Fanta-passion Soda  300ml','40822924',1,50.0,40.0,0.0,0.0,50.0);
INSERT INTO "sale_items" VALUES ('7addb920-586f-4b1c-9176-3b12d7b97964','8c37d3b5-023b-4e38-ad96-cab41404dbe6','06a0da23-fd56-43b4-818e-707c0837f3f3','Rosy Extra Strong Tissue Pink color','6161100762411',2,20.0,18.0,0.0,0.0,40.0);
INSERT INTO "sale_items" VALUES ('a20d8c0c-320d-4a0b-be63-d43e6cbc2400','54a06e2f-d12f-4df2-8477-70dfa438cd11','353914f2-3fed-48cb-bfd6-a3e179c9593b','Fanta-Orange Soda  200ml','90495090',1,25.0,15.0,0.0,0.0,25.0);
INSERT INTO "sale_items" VALUES ('dd853fe3-b027-4a6e-9535-c7dfbc21bfae','bd82c0c4-ea21-4f40-bdb0-699f35854434','24480d20-68c0-4532-bde2-f9fd388b9f22','Panadol Advanced','68686102525',1,20.0,15.0,0.0,0.0,20.0);
INSERT INTO "sale_items" VALUES ('c278a803-bfc9-4d32-8994-b366844beab2','f7d6495b-f11c-4f14-8e3f-39eb5b012c48','d14e4361-dae2-4ccc-8284-bb7c370fa32d','Fanta - Orange Soda 500ml','50112173',1,60.0,50.0,0.0,0.0,60.0);
INSERT INTO "sale_items" VALUES ('b6e192f6-09fd-41e3-9af6-c602169fa5b8','d3655701-c4f0-4c3a-a710-729a50f317f3','1df8138b-5d43-4e7e-8237-69eb273b6681','Menengai Cream Quality Washing Bar 1kg','6161100907034',1,50.0,45.0,0.0,0.0,50.0);
INSERT INTO "sale_items" VALUES ('ba1abc5d-d7fc-4709-b549-981147eb780d','3f8c5ec2-00e3-4dff-b8b4-24c079a2efbc','e082b326-cf02-4226-8a83-48fb91da8cfd','Sprite soda 300ml','54492691',1,50.0,40.0,0.0,0.0,50.0);
INSERT INTO "sale_items" VALUES ('acb04667-6f6f-421e-b95d-5498545cdbbb','c9f7c97c-b037-49ec-99f0-0d43fef75e08','151b9723-7b8a-4390-8194-6e5a3878cc80','Dalia Luxurious Soft White Tissue','0792382597352',1,20.0,18.0,0.0,0.0,20.0);
INSERT INTO "sale_items" VALUES ('877bdcd5-3935-4b6f-9965-47b0bbeaa8eb','89553478-c9d5-4cf7-bf16-0f9e0fb9d719','565c4288-b7ed-4c23-9e01-9294e1e98279','Tobex Bleach 70 ml','6164000015106',3,35.0,30.0,0.0,0.0,105.0);
INSERT INTO "sale_items" VALUES ('94a2f06b-89b2-499e-9012-58c1a3bccd7e','89553478-c9d5-4cf7-bf16-0f9e0fb9d719','e63744cd-09e1-4a40-82cf-75e76647f7b7','Jamaa White Washing Bar','6161101667111',1,40.0,30.0,0.0,0.0,40.0);
INSERT INTO "sale_items" VALUES ('16f6808c-cbf1-4894-8d91-2ae95bba86b1','89553478-c9d5-4cf7-bf16-0f9e0fb9d719','91571c23-8e2c-4ac1-8c50-c907b8d74121','Cyano Acrylate Adhesive super glue','6940988800206',2,35.0,30.0,0.0,0.0,70.0);
INSERT INTO "sale_items" VALUES ('bf592da8-3555-4afc-8b04-a6f6ea8d05bb','89553478-c9d5-4cf7-bf16-0f9e0fb9d719','353914f2-3fed-48cb-bfd6-a3e179c9593b','Fanta-Orange Soda  200ml','90495090',1,25.0,15.0,0.0,0.0,25.0);
INSERT INTO "sale_items" VALUES ('314106fa-97a6-48a3-aff3-2adcda540026','ec7ae38b-00ff-4d47-a9f0-540e354bd711','ad049a23-82cd-45cb-a2cb-35ad2999c562','KCC FRESH WHOLE MILK 200ML','6161100460126',1,35.0,30.0,0.0,0.0,35.0);
INSERT INTO "sale_items" VALUES ('680c0c14-27e9-4fd4-b67c-8171af4b73fe','3d8e9bff-9299-4711-823d-3783089bb1a7','b2713b07-0067-4411-a5d6-114be39ad259','Ajab Fortified All Purpose Home Baking Flour 500g','6161113940127',1,55.0,50.0,0.0,0.0,55.0);
INSERT INTO "sale_items" VALUES ('e5ee85a3-25f7-4ce9-8407-6b20f2c5d55b','3d8e9bff-9299-4711-823d-3783089bb1a7','12be2e1f-71b1-45dc-9e3c-4deadc38b96c','KENSALT  Iodated Edible Table Salt 200g','6161101280037',1,15.0,10.0,0.0,0.0,15.0);
INSERT INTO "sales" VALUES ('e2754f55-6e5a-4e16-aa57-05c049fe6e15','RCP-20260723-0001','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,10.0,0.0,0.0,10.0,'MPESA',10.0,0.0,'UGN7VORL9M','COMPLETED','SYNCED','2026-07-23T19:24:21.442372300','2026-07-23T19:24:21.456286600');
INSERT INTO "sales" VALUES ('ded571ff-c92e-475a-8e8f-a3be914a31c2','RCP-20260723-0002','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,30.0,0.0,0.0,30.0,'MPESA',30.0,0.0,'UGNT5N0CMNT','COMPLETED','SYNCED','2026-07-23T19:31:46.018656800','2026-07-23T19:31:46.018656800');
INSERT INTO "sales" VALUES ('01f045a4-f9df-45ef-9dad-8eeb190c42ef','RCP-20260723-0003','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,140.0,0.0,0.0,140.0,'CASH',140.0,0.0,'','COMPLETED','SYNCED','2026-07-23T20:38:50.330434','2026-07-23T20:38:50.330434');
INSERT INTO "sales" VALUES ('47131f5f-41b4-4c8c-a123-36bf500462ab','RCP-20260723-0004','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,200.0,0.0,0.0,200.0,'MPESA',200.0,0.0,'','COMPLETED','SYNCED','2026-07-23T21:20:45.892971400','2026-07-23T21:20:45.894969500');
INSERT INTO "sales" VALUES ('1ff28d1d-6631-4827-924e-a1b1ee5ad1bc','RCP-20260724-0001','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,50.0,0.0,0.0,50.0,'MPESA',50.0,0.0,'','COMPLETED','SYNCED','2026-07-24T12:26:13.089466800','2026-07-24T12:26:13.089466800');
INSERT INTO "sales" VALUES ('8c37d3b5-023b-4e38-ad96-cab41404dbe6','RCP-20260724-0002','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,40.0,0.0,0.0,40.0,'MPESA',40.0,0.0,'','COMPLETED','SYNCED','2026-07-24T12:27:29.157460800','2026-07-24T12:27:29.157460800');
INSERT INTO "sales" VALUES ('54a06e2f-d12f-4df2-8477-70dfa438cd11','RCP-20260724-0003','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,25.0,0.0,0.0,25.0,'MPESA',25.0,0.0,'','COMPLETED','SYNCED','2026-07-24T12:53:24.432367500','2026-07-24T12:53:24.432367500');
INSERT INTO "sales" VALUES ('bd82c0c4-ea21-4f40-bdb0-699f35854434','RCP-20260724-0004','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,20.0,0.0,0.0,20.0,'MPESA',20.0,0.0,'','COMPLETED','SYNCED','2026-07-24T13:21:52.271078100','2026-07-24T13:21:52.271078100');
INSERT INTO "sales" VALUES ('f7d6495b-f11c-4f14-8e3f-39eb5b012c48','RCP-20260724-0005','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,60.0,0.0,0.0,60.0,'MPESA',60.0,0.0,'','COMPLETED','SYNCED','2026-07-24T14:28:04.994946400','2026-07-24T14:28:04.994946400');
INSERT INTO "sales" VALUES ('d3655701-c4f0-4c3a-a710-729a50f317f3','RCP-20260724-0006','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,50.0,0.0,0.0,50.0,'MPESA',50.0,0.0,'','COMPLETED','SYNCED','2026-07-24T14:30:52.192509900','2026-07-24T14:30:52.192509900');
INSERT INTO "sales" VALUES ('3f8c5ec2-00e3-4dff-b8b4-24c079a2efbc','RCP-20260724-0007','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,50.0,0.0,0.0,50.0,'CASH',50.0,0.0,'','COMPLETED','SYNCED','2026-07-24T14:36:59.688183','2026-07-24T14:36:59.688183');
INSERT INTO "sales" VALUES ('c9f7c97c-b037-49ec-99f0-0d43fef75e08','RCP-20260724-0008','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,20.0,0.0,0.0,20.0,'MPESA',20.0,0.0,'','COMPLETED','SYNCED','2026-07-24T14:49:16.339034900','2026-07-24T14:49:16.339034900');
INSERT INTO "sales" VALUES ('89553478-c9d5-4cf7-bf16-0f9e0fb9d719','RCP-20260724-0009','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,240.0,0.0,0.0,240.0,'CASH',240.0,0.0,'','COMPLETED','SYNCED','2026-07-24T15:31:17.922729500','2026-07-24T15:31:17.922729500');
INSERT INTO "sales" VALUES ('ec7ae38b-00ff-4d47-a9f0-540e354bd711','RCP-20260724-0010','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,35.0,0.0,0.0,35.0,'MPESA',35.0,0.0,'','COMPLETED','SYNCED','2026-07-24T15:35:53.778964500','2026-07-24T15:35:53.778964500');
INSERT INTO "sales" VALUES ('3d8e9bff-9299-4711-823d-3783089bb1a7','RCP-20260724-0011','a23a9e6e-df52-49b0-bf8d-577b8d3462af','Erick Juma',NULL,70.0,0.0,0.0,70.0,'CASH',70.0,0.0,'','COMPLETED','SYNCED','2026-07-24T16:50:25.318388400','2026-07-24T16:50:25.318388400');
INSERT INTO "suppliers" VALUES ('36c12be2-1d6c-4899-a709-ac24146a29ea','Genesis international',NULL,'Nakuru,Kenya',NULL,0.0,'SYNCED','2026-07-23T00:43:12.901282200','2026-07-23T00:43:12.901282200');
INSERT INTO "suppliers" VALUES ('87e77414-71f2-4453-9382-4435634bf5f7','bonface onduso','bonfaceobnduso9@gmail.com','Nakuru,kenya',NULL,0.0,'SYNCED','2026-07-22 16:11:58','2026-07-22 23:43:35');
INSERT INTO "users" VALUES ('a23a9e6e-df52-49b0-bf8d-577b8d3462af','erickmosess','$2a$12$IsJRjrtuJaTLls0/5WMQn.xc29CjpwvL1pqZevritZGw48hWAyOtm','ADMIN','Erick Juma',1,0,NULL,'SYNCED','2026-07-22T17:00:59.837718200','2026-07-24T18:01:14.260178500');
INSERT INTO "users" VALUES ('cc6e55ec-28a1-4ec9-86f7-d45ecc43fe10','veronica','$2a$12$.M4gYBLbFzOAwIGys0vkB.83ImYjCW.NnR0b1Xew96bJ0VIxugR7C','CASHIER','veronica',1,5,'2026-07-22T19:07:59.129323200','SYNCED','2026-07-22T17:20:16.797857','2026-07-22T18:52:59.129323200');
CREATE INDEX IF NOT EXISTS "idx_audit_created_at" ON "audit_logs" (
	"created_at"
);
CREATE INDEX IF NOT EXISTS "idx_audit_event_type" ON "audit_logs" (
	"event_type"
);
CREATE INDEX IF NOT EXISTS "idx_audit_user_id" ON "audit_logs" (
	"user_id"
);
CREATE INDEX IF NOT EXISTS "idx_categories_name" ON "categories" (
	"name"
);
CREATE INDEX IF NOT EXISTS "idx_customers_name" ON "customers" (
	"name"
);
CREATE INDEX IF NOT EXISTS "idx_customers_phone" ON "customers" (
	"phone"
);
CREATE INDEX IF NOT EXISTS "idx_inv_created_at" ON "inventory_movements" (
	"created_at"
);
CREATE INDEX IF NOT EXISTS "idx_inv_product_id" ON "inventory_movements" (
	"product_id"
);
CREATE INDEX IF NOT EXISTS "idx_inv_type" ON "inventory_movements" (
	"type"
);
CREATE INDEX IF NOT EXISTS "idx_po_supplier_id" ON "purchase_orders" (
	"supplier_id"
);
CREATE INDEX IF NOT EXISTS "idx_poi_po_id" ON "purchase_order_items" (
	"po_id"
);
CREATE INDEX IF NOT EXISTS "idx_product_images_product_id" ON "product_images" (
	"product_id"
);
CREATE INDEX IF NOT EXISTS "idx_product_images_sync_status" ON "product_images" (
	"sync_status"
);
CREATE INDEX IF NOT EXISTS "idx_products_barcode" ON "products" (
	"barcode"
);
CREATE INDEX IF NOT EXISTS "idx_products_category_id" ON "products" (
	"category_id"
);
CREATE INDEX IF NOT EXISTS "idx_products_name" ON "products" (
	"name"
);
CREATE INDEX IF NOT EXISTS "idx_products_qr_code" ON "products" (
	"qr_code"
);
CREATE INDEX IF NOT EXISTS "idx_products_sku" ON "products" (
	"sku"
);
CREATE INDEX IF NOT EXISTS "idx_products_status" ON "products" (
	"status"
);
CREATE INDEX IF NOT EXISTS "idx_products_sync_status" ON "products" (
	"sync_status"
);
CREATE INDEX IF NOT EXISTS "idx_products_updated_at" ON "products" (
	"updated_at"
);
CREATE INDEX IF NOT EXISTS "idx_sale_items_product_id" ON "sale_items" (
	"product_id"
);
CREATE INDEX IF NOT EXISTS "idx_sale_items_sale_id" ON "sale_items" (
	"sale_id"
);
CREATE INDEX IF NOT EXISTS "idx_sales_cashier_id" ON "sales" (
	"cashier_id"
);
CREATE INDEX IF NOT EXISTS "idx_sales_created_at" ON "sales" (
	"created_at"
);
CREATE INDEX IF NOT EXISTS "idx_sales_receipt" ON "sales" (
	"receipt_number"
);
CREATE INDEX IF NOT EXISTS "idx_sales_sync_status" ON "sales" (
	"sync_status"
);
CREATE INDEX IF NOT EXISTS "idx_users_username" ON "users" (
	"username"
);
COMMIT;
