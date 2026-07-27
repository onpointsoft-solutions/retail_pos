-- Sync Optimization Indexes
-- These indexes improve sync performance by speeding up queries on sync_status and updated_at columns
-- Run this on existing databases to add the missing indexes

-- Suppliers table
CREATE INDEX IF NOT EXISTS idx_suppliers_sync_status ON suppliers(sync_status);
CREATE INDEX IF NOT EXISTS idx_suppliers_updated_at ON suppliers(updated_at);

-- Customers table
CREATE INDEX IF NOT EXISTS idx_customers_sync_status ON customers(sync_status);
CREATE INDEX IF NOT EXISTS idx_customers_updated_at ON customers(updated_at);

-- Sales table
CREATE INDEX IF NOT EXISTS idx_sales_updated_at ON sales(updated_at);

-- Purchase orders table
CREATE INDEX IF NOT EXISTS idx_purchase_orders_sync_status ON purchase_orders(sync_status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_updated_at ON purchase_orders(updated_at);

-- Inventory movements table
CREATE INDEX IF NOT EXISTS idx_inventory_movements_sync_status ON inventory_movements(sync_status);

-- Product images table
CREATE INDEX IF NOT EXISTS idx_product_images_updated_at ON product_images(updated_at);
