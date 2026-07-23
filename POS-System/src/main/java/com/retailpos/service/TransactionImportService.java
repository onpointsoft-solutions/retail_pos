package com.retailpos.service;

import com.retailpos.util.DatabaseManager;
import java.nio.file.Path;
import java.sql.*;

/** Imports sales from a SQLite backup without replacing local data. */
public final class TransactionImportService {
    private TransactionImportService() { }

    public static int importSales(Path backupFile) throws Exception {
        if (!java.nio.file.Files.isRegularFile(backupFile)) throw new IllegalArgumentException("Backup file was not found");
        int imported = 0;
        try (Connection source = DriverManager.getConnection("jdbc:sqlite:" + backupFile.toAbsolutePath());
             Connection target = DatabaseManager.getConnection()) {
            target.setAutoCommit(false);
            try {
                try (PreparedStatement sales = source.prepareStatement("SELECT * FROM sales"); ResultSet rows = sales.executeQuery()) {
                while (rows.next()) {
                    String saleId = rows.getString("id");
                    if (exists(target, "SELECT 1 FROM sales WHERE id=?", saleId)) continue;
                    String customerId = rows.getString("customer_id");
                    if (customerId != null && !exists(target, "SELECT 1 FROM customers WHERE id=?", customerId)) customerId = null;
                    try (PreparedStatement insert = target.prepareStatement(
                        "INSERT INTO sales(id,receipt_number,cashier_id,cashier_name,customer_id,subtotal,discount_amount,tax_amount,grand_total,payment_method,cash_tendered,change_amount,payment_reference,status,sync_status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                        insert.setString(1, saleId); insert.setString(2, rows.getString("receipt_number"));
                        insert.setString(3, rows.getString("cashier_id")); insert.setString(4, rows.getString("cashier_name")); insert.setString(5, customerId);
                        insert.setDouble(6, rows.getDouble("subtotal")); insert.setDouble(7, rows.getDouble("discount_amount")); insert.setDouble(8, rows.getDouble("tax_amount"));
                        insert.setDouble(9, rows.getDouble("grand_total")); insert.setString(10, rows.getString("payment_method")); insert.setDouble(11, rows.getDouble("cash_tendered"));
                        insert.setDouble(12, rows.getDouble("change_amount")); insert.setString(13, rows.getString("payment_reference")); insert.setString(14, rows.getString("status"));
                        insert.setString(15, "PENDING"); insert.setString(16, rows.getString("created_at")); insert.setString(17, rows.getString("updated_at")); insert.executeUpdate();
                    }
                    copyItems(source, target, saleId);
                    imported++;
                }
                }
                target.commit();
            } catch (Exception exception) {
                target.rollback();
                throw exception;
            } finally {
                target.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw exception;
        }
        return imported;
    }

    private static void copyItems(Connection source, Connection target, String saleId) throws SQLException {
        try (PreparedStatement read = source.prepareStatement("SELECT * FROM sale_items WHERE sale_id=?"); PreparedStatement write = target.prepareStatement(
            "INSERT OR IGNORE INTO sale_items(id,sale_id,product_id,product_name,product_sku,quantity,unit_price,buying_price,discount,tax_rate,line_total) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            read.setString(1, saleId); ResultSet items = read.executeQuery();
            while (items.next()) {
                write.setString(1, items.getString("id")); write.setString(2, saleId); write.setString(3, items.getString("product_id")); write.setString(4, items.getString("product_name")); write.setString(5, items.getString("product_sku"));
                write.setInt(6, items.getInt("quantity")); write.setDouble(7, items.getDouble("unit_price")); write.setDouble(8, items.getDouble("buying_price")); write.setDouble(9, items.getDouble("discount")); write.setDouble(10, items.getDouble("tax_rate")); write.setDouble(11, items.getDouble("line_total")); write.executeUpdate();
            }
        }
    }

    private static boolean exists(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement(sql)) { check.setString(1, id); return check.executeQuery().next(); }
    }
}
