package com.retailpos.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    public static synchronized void initialize(String dbPath) {
        if (dataSource != null) return;
        try {
            Path databasePath = Path.of(dbPath).toAbsolutePath();
            Path parentDirectory = databasePath.getParent();
            if (parentDirectory != null) Files.createDirectories(parentDirectory);
            dbPath = databasePath.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to prepare the database directory", e);
        }
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        config.addDataSourceProperty("busy_timeout", "5000");
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DatabaseManager not initialized.");
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) { dataSource.close(); dataSource = null; }
    }

    public static void createAllTables() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON;");
            s.execute("PRAGMA journal_mode = WAL;");
            createCategoriesTable(s);
            createSuppliersTable(s);
            createUsersTable(s);
            createProductsTable(s);
            createProductImagesTable(s);
            createCustomersTable(s);
            createSalesTable(s);
            createSaleItemsTable(s);
            createInventoryMovementsTable(s);
            createPurchaseOrdersTable(s);
            createPurchaseOrderItemsTable(s);
            createSuspendedCartsTable(s);
            createSuspendedCartItemsTable(s);
            createMpesaTransactionsTable(s);
            createAuditLogsTable(s);
            createAppSettingsTable(s);
            createJobCardsTable(s);
            createJobCardServiceItemsTable(s);
            createQuotationsTable(s);
            createQuotationItemsTable(s);
            migrateServiceTables(s);
            insertDefaultAdminIfEmpty(c);
            insertDefaultSettingsIfEmpty(c);
            migrateDefaultBackendUrl(c);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create tables: " + e.getMessage(), e);
        }
    }

    public static void backupTo(Path backupFile) throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
            String escapedPath = backupFile.toAbsolutePath().toString().replace("'", "''");
            statement.execute("VACUUM INTO '" + escapedPath + "'");
        }
    }

    private static void createCategoriesTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS categories (" +
            "id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT, " +
            "sync_status TEXT DEFAULT 'PENDING', created_at TEXT, updated_at TEXT)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name)");
    }

    private static void createSuppliersTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS suppliers (" +
            "id TEXT PRIMARY KEY, name TEXT NOT NULL, phone TEXT, email TEXT, address TEXT, " +
            "balance REAL DEFAULT 0, sync_status TEXT DEFAULT 'PENDING', " +
            "created_at TEXT, updated_at TEXT)");
    }

    private static void createUsersTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS users (" +
            "id TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, " +
            "role TEXT NOT NULL, full_name TEXT, active INTEGER DEFAULT 1, " +
            "permissions TEXT, " +
            "failed_login_attempts INTEGER DEFAULT 0, lockout_until TEXT, " +
            "sync_status TEXT DEFAULT 'PENDING', created_at TEXT, updated_at TEXT)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
        try { s.execute("ALTER TABLE users ADD COLUMN permissions TEXT"); } catch (SQLException ignored) {}
    }

    private static void createMpesaTransactionsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS mpesa_transactions (" +
            "id TEXT PRIMARY KEY, code TEXT NOT NULL UNIQUE, customer_name TEXT, amount REAL DEFAULT 0, " +
            "received_at INTEGER NOT NULL, sync_status TEXT DEFAULT 'PENDING', " +
            "created_at TEXT, updated_at TEXT)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_mpesa_received_at ON mpesa_transactions(received_at)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_mpesa_sync_status ON mpesa_transactions(sync_status)");
    }

    private static void createProductsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS products (" +
            "id TEXT PRIMARY KEY, barcode TEXT, qr_code TEXT, sku TEXT NOT NULL, " +
            "name TEXT NOT NULL, category_id TEXT, buying_price REAL DEFAULT 0, " +
            "selling_price REAL NOT NULL, wholesale_price REAL DEFAULT 0, " +
            "current_stock INTEGER DEFAULT 0, minimum_stock INTEGER DEFAULT 0, preferred_order_quantity INTEGER DEFAULT 0, " +
            "tax_rate REAL DEFAULT 0, discount REAL DEFAULT 0, supplier_id TEXT, " +
            "description TEXT, image_path TEXT, unit TEXT DEFAULT 'pcs', " +
            "status TEXT DEFAULT 'active', " +
            "track_expiry INTEGER DEFAULT 0, sync_status TEXT DEFAULT 'PENDING', " +
            "version INTEGER DEFAULT 1, created_at TEXT, updated_at TEXT, deleted_at TEXT, " +
            "FOREIGN KEY(category_id) REFERENCES categories(id))");
        // Add unit column to existing installs (safe to run on existing DB)
        try { s.execute("ALTER TABLE products ADD COLUMN unit TEXT DEFAULT 'pcs'"); }
        catch (Exception ignored) {}
        try { s.execute("ALTER TABLE products ADD COLUMN preferred_order_quantity INTEGER DEFAULT 0"); }
        catch (Exception ignored) {}
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_qr_code ON products(qr_code)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_name ON products(name)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_sync_status ON products(sync_status)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_updated_at ON products(updated_at)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_products_status ON products(status)");
    }

    private static void createCustomersTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS customers (" +
            "id TEXT PRIMARY KEY, name TEXT NOT NULL, phone TEXT UNIQUE, email TEXT UNIQUE, " +
            "loyalty_points INTEGER DEFAULT 0, credit_balance REAL DEFAULT 0, " +
            "sync_status TEXT DEFAULT 'PENDING', created_at TEXT, updated_at TEXT)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name)");
    }

    private static void createProductImagesTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS product_images (" +
            "id TEXT PRIMARY KEY, product_id TEXT NOT NULL, image_path TEXT NOT NULL, display_order INTEGER DEFAULT 0, " +
            "sync_status TEXT DEFAULT 'PENDING', created_at TEXT, updated_at TEXT, deleted_at TEXT, " +
            "FOREIGN KEY(product_id) REFERENCES products(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_product_images_sync_status ON product_images(sync_status)");
    }

    private static void createSalesTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS sales (" +
            "id TEXT PRIMARY KEY, receipt_number TEXT UNIQUE NOT NULL, cashier_id TEXT, cashier_name TEXT, " +
            "customer_id TEXT, subtotal REAL, discount_amount REAL DEFAULT 0, " +
            "tax_amount REAL DEFAULT 0, grand_total REAL, payment_method TEXT, " +
            "cash_tendered REAL DEFAULT 0, change_amount REAL DEFAULT 0, " +
            "payment_reference TEXT, status TEXT DEFAULT 'COMPLETED', " +
            "sync_status TEXT DEFAULT 'PENDING', created_at TEXT, updated_at TEXT, " +
            "FOREIGN KEY(customer_id) REFERENCES customers(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_sales_cashier_id ON sales(cashier_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_sales_sync_status ON sales(sync_status)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_sales_receipt ON sales(receipt_number)");
    }

    private static void createSaleItemsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS sale_items (" +
            "id TEXT PRIMARY KEY, sale_id TEXT NOT NULL, product_id TEXT, product_name TEXT, " +
            "product_sku TEXT, quantity INTEGER, unit_price REAL, buying_price REAL, " +
            "discount REAL DEFAULT 0, tax_rate REAL DEFAULT 0, line_total REAL, " +
            "FOREIGN KEY(sale_id) REFERENCES sales(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id)");
    }

    private static void createInventoryMovementsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS inventory_movements (" +
            "id TEXT PRIMARY KEY, product_id TEXT NOT NULL, product_name TEXT, type TEXT NOT NULL, " +
            "quantity INTEGER, reason TEXT, batch_number TEXT, expiry_date TEXT, user_id TEXT, " +
            "sync_status TEXT DEFAULT 'PENDING', created_at TEXT, " +
            "FOREIGN KEY(product_id) REFERENCES products(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_inv_product_id ON inventory_movements(product_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_inv_created_at ON inventory_movements(created_at)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_inv_type ON inventory_movements(type)");
    }

    private static void createPurchaseOrdersTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS purchase_orders (" +
            "id TEXT PRIMARY KEY, supplier_id TEXT, supplier_name TEXT, status TEXT DEFAULT 'ORDERED', " +
            "expected_delivery_date TEXT, notes TEXT, " +
            "sync_status TEXT DEFAULT 'PENDING', created_at TEXT, updated_at TEXT, " +
            "FOREIGN KEY(supplier_id) REFERENCES suppliers(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_po_supplier_id ON purchase_orders(supplier_id)");
    }

    private static void createPurchaseOrderItemsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS purchase_order_items (" +
            "id TEXT PRIMARY KEY, po_id TEXT NOT NULL, product_id TEXT, product_name TEXT, " +
            "ordered_qty INTEGER, received_qty INTEGER DEFAULT 0, buying_price REAL, " +
            "FOREIGN KEY(po_id) REFERENCES purchase_orders(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_poi_po_id ON purchase_order_items(po_id)");
    }

    private static void createSuspendedCartsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS suspended_carts (" +
            "id TEXT PRIMARY KEY, cashier_id TEXT, customer_id TEXT, " +
            "discount_amount REAL DEFAULT 0, suspended_at TEXT)");
    }

    private static void createSuspendedCartItemsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS suspended_cart_items (" +
            "id TEXT PRIMARY KEY, cart_id TEXT NOT NULL, product_id TEXT, product_name TEXT, " +
            "product_sku TEXT, quantity INTEGER, unit_price REAL, buying_price REAL, " +
            "discount REAL DEFAULT 0, tax_rate REAL DEFAULT 0, line_total REAL, " +
            "FOREIGN KEY(cart_id) REFERENCES suspended_carts(id))");
    }

    private static void createAuditLogsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
            "id TEXT PRIMARY KEY, user_id TEXT, event_type TEXT, entity_id TEXT, " +
            "details TEXT, created_at TEXT)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs(event_type)");
    }

    private static void createAppSettingsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS app_settings (" +
            "key TEXT PRIMARY KEY, value TEXT)");
    }

    // ── Services module ───────────────────────────────────────────────────────

    private static void createJobCardsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS job_cards (" +
            "id TEXT PRIMARY KEY, " +
            "job_number TEXT NOT NULL UNIQUE, " +
            "customer_id TEXT, " +
            "customer_name TEXT NOT NULL, " +
            "customer_phone TEXT, " +
            "asset_description TEXT NOT NULL, " +
            "asset_serial TEXT, " +
            "problem_description TEXT NOT NULL, " +
            "diagnosis TEXT, " +
            "resolution TEXT, " +
            "technician_id TEXT, " +
            "technician_name TEXT, " +
            "labour_charge REAL DEFAULT 0, " +
            "status TEXT NOT NULL DEFAULT 'OPEN', " +
            "active_quotation_id TEXT, " +
            "due_date TEXT, " +
            "sync_status TEXT DEFAULT 'PENDING', " +
            "created_at TEXT NOT NULL, " +
            "updated_at TEXT NOT NULL)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_job_cards_status      ON job_cards(status)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_job_cards_customer    ON job_cards(customer_name)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_job_cards_created_at  ON job_cards(created_at)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_job_cards_job_number  ON job_cards(job_number)");
    }

    private static void createJobCardServiceItemsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS job_card_service_items (" +
            "id TEXT PRIMARY KEY, " +
            "job_card_id TEXT NOT NULL, " +
            "description TEXT NOT NULL, " +
            "charge REAL DEFAULT 0, " +
            "quantity INTEGER DEFAULT 1, " +
            "FOREIGN KEY(job_card_id) REFERENCES job_cards(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_jcsi_job_card_id ON job_card_service_items(job_card_id)");
    }

    private static void createQuotationsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS quotations (" +
            "id TEXT PRIMARY KEY, " +
            "quotation_number TEXT NOT NULL UNIQUE, " +
            "job_card_id TEXT NOT NULL, " +
            "job_card_number TEXT, " +
            "invoice_sale_id TEXT, " +
            "customer_id TEXT, " +
            "customer_name TEXT NOT NULL, " +
            "customer_phone TEXT, " +
            "subtotal REAL DEFAULT 0, " +
            "discount_amount REAL DEFAULT 0, " +
            "tax_amount REAL DEFAULT 0, " +
            "labour_total REAL DEFAULT 0, " +
            "grand_total REAL DEFAULT 0, " +
            "notes TEXT, " +
            "status TEXT NOT NULL DEFAULT 'DRAFT', " +
            "created_by_id TEXT, " +
            "created_by_name TEXT, " +
            "valid_until TEXT, " +
            "sync_status TEXT DEFAULT 'PENDING', " +
            "created_at TEXT NOT NULL, " +
            "updated_at TEXT NOT NULL, " +
            "FOREIGN KEY(job_card_id) REFERENCES job_cards(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_quotations_job_card_id ON quotations(job_card_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_quotations_status      ON quotations(status)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_quotations_created_at  ON quotations(created_at)");
    }

    /** Adds service columns safely for databases created before this module. */
    private static void migrateServiceTables(Statement s) throws SQLException {
        try { s.execute("ALTER TABLE quotations ADD COLUMN invoice_sale_id TEXT"); }
        catch (SQLException ignored) { /* column already exists */ }
        s.execute("CREATE INDEX IF NOT EXISTS idx_quotations_invoice_sale_id ON quotations(invoice_sale_id)");
    }

    private static void createQuotationItemsTable(Statement s) throws SQLException {
        s.execute("CREATE TABLE IF NOT EXISTS quotation_items (" +
            "id TEXT PRIMARY KEY, " +
            "quotation_id TEXT NOT NULL, " +
            "product_id TEXT, " +
            "product_name TEXT NOT NULL, " +
            "product_sku TEXT, " +
            "quantity INTEGER DEFAULT 1, " +
            "unit_price REAL DEFAULT 0, " +
            "buying_price REAL DEFAULT 0, " +
            "discount REAL DEFAULT 0, " +
            "tax_rate REAL DEFAULT 0, " +
            "line_total REAL DEFAULT 0, " +
            "FOREIGN KEY(quotation_id) REFERENCES quotations(id))");
        s.execute("CREATE INDEX IF NOT EXISTS idx_quotation_items_quotation_id ON quotation_items(quotation_id)");
        s.execute("CREATE INDEX IF NOT EXISTS idx_quotation_items_product_id   ON quotation_items(product_id)");
    }

    private static void insertDefaultAdminIfEmpty(Connection c) throws SQLException {
        try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM users WHERE role='ADMIN'")) {
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                String id = UUID.randomUUID().toString();
                String now = LocalDateTime.now().toString();
                String hash = PasswordUtil.hash("admin123");
                try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO users(id,username,password_hash,role,full_name,active,sync_status,created_at,updated_at) " +
                    "VALUES(?,?,?,'ADMIN','Administrator',1,'PENDING',?,?)")) {
                    ins.setString(1, id);
                    ins.setString(2, "admin");
                    ins.setString(3, hash);
                    ins.setString(4, now);
                    ins.setString(5, now);
                    ins.executeUpdate();
                }
            }
        }
    }

    private static void insertDefaultSettingsIfEmpty(Connection c) throws SQLException {
        try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM app_settings")) {
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                String[][] defaults = {
                    {"store_name","Retail Shop"}, {"store_address",""}, {"store_phone",""},
                    {"store_footer","Thank you for shopping with us!"},{"logo_path",""},
                    {"printer_name",""},{"paper_width","80"},{"tax_rate","16.0"},
                    {"loyalty_earning_rate","1.0"},{"sync_api_url","https://pos.mobilemealscenter.co.ke/api/"},
                    {"sync_api_token",""},{"sync_api_username","admin"},{"sync_api_password",""},{"auto_sync","true"},{"dark_mode","false"},
                    {"backup_path","backups"},{"backup_time","23:00"},{"auto_print_receipt","true"},{"last_successful_sync",""}
                };
                for (String[] kv : defaults) {
                    try (PreparedStatement ins = c.prepareStatement(
                        "INSERT OR IGNORE INTO app_settings(key,value) VALUES(?,?)")) {
                        ins.setString(1, kv[0]);
                        ins.setString(2, kv[1]);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    private static void migrateDefaultBackendUrl(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE app_settings SET value=? WHERE key='sync_api_url' AND value IN (?,?)")) {
            statement.setString(1, "https://pos.mobilemealscenter.co.ke/api/");
            statement.setString(2, "https://pos.victoriousgeneralshop.com/api/");
            statement.setString(3, "http://localhost/retail-pos-api/api/");
            statement.executeUpdate();
        }
    }
}
