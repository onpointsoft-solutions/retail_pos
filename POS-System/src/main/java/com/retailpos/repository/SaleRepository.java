package com.retailpos.repository;

import com.retailpos.model.Sale;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SaleRepository {

    private Sale mapSale(ResultSet r) throws SQLException {
        Sale s = new Sale();
        s.setId(r.getString("id"));
        s.setReceiptNumber(r.getString("receipt_number"));
        s.setCashierId(r.getString("cashier_id"));
        s.setCashierName(r.getString("cashier_name"));
        s.setCustomerId(r.getString("customer_id"));
        s.setSubtotal(r.getDouble("subtotal"));
        s.setDiscountAmount(r.getDouble("discount_amount"));
        s.setTaxAmount(r.getDouble("tax_amount"));
        s.setGrandTotal(r.getDouble("grand_total"));
        s.setPaymentMethod(r.getString("payment_method"));
        s.setCashTendered(r.getDouble("cash_tendered"));
        s.setChange(r.getDouble("change_amount"));
        s.setPaymentReference(r.getString("payment_reference"));
        s.setStatus(r.getString("status"));
        s.setSyncStatus(r.getString("sync_status"));
        String ca = r.getString("created_at"); if (ca != null) s.setCreatedAt(LocalDateTime.parse(ca));
        String ua = r.getString("updated_at"); if (ua != null) s.setUpdatedAt(LocalDateTime.parse(ua));
        return s;
    }

    private Sale.SaleItem mapItem(ResultSet r) throws SQLException {
        Sale.SaleItem i = new Sale.SaleItem();
        i.setProductId(r.getString("product_id"));
        i.setProductName(r.getString("product_name"));
        i.setProductSku(r.getString("product_sku"));
        i.setQuantity(r.getInt("quantity"));
        i.setUnitPrice(r.getDouble("unit_price"));
        i.setBuyingPrice(r.getDouble("buying_price"));
        i.setDiscount(r.getDouble("discount"));
        i.setTaxRate(r.getDouble("tax_rate"));
        i.setLineTotal(r.getDouble("line_total"));
        return i;
    }

    public void insert(Sale sale) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                String saleSql = "INSERT INTO sales(id,receipt_number,cashier_id,cashier_name,customer_id," +
                    "subtotal,discount_amount,tax_amount,grand_total,payment_method,cash_tendered," +
                    "change_amount,payment_reference,status,sync_status,created_at,updated_at) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(saleSql)) {
                    String now = LocalDateTime.now().toString();
                    ps.setString(1, sale.getId()); ps.setString(2, sale.getReceiptNumber());
                    ps.setString(3, sale.getCashierId()); ps.setString(4, sale.getCashierName());
                    ps.setString(5, sale.getCustomerId()); ps.setDouble(6, sale.getSubtotal());
                    ps.setDouble(7, sale.getDiscountAmount()); ps.setDouble(8, sale.getTaxAmount());
                    ps.setDouble(9, sale.getGrandTotal()); ps.setString(10, sale.getPaymentMethod());
                    ps.setDouble(11, sale.getCashTendered()); ps.setDouble(12, sale.getChange());
                    ps.setString(13, sale.getPaymentReference()); ps.setString(14, sale.getStatus());
                    ps.setString(15, sale.getSyncStatus() != null ? sale.getSyncStatus() : "PENDING");
                    ps.setString(16, sale.getCreatedAt() != null ? sale.getCreatedAt().toString() : now);
                    ps.setString(17, now);
                    ps.executeUpdate();
                }
                String itemSql = "INSERT INTO sale_items(id,sale_id,product_id,product_name,product_sku," +
                    "quantity,unit_price,buying_price,discount,tax_rate,line_total) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
                for (Sale.SaleItem item : sale.getItems()) {
                    try (PreparedStatement ps = c.prepareStatement(itemSql)) {
                        ps.setString(1, java.util.UUID.randomUUID().toString());
                        ps.setString(2, sale.getId()); ps.setString(3, item.getProductId());
                        ps.setString(4, item.getProductName()); ps.setString(5, item.getProductSku());
                        ps.setInt(6, item.getQuantity()); ps.setDouble(7, item.getUnitPrice());
                        ps.setDouble(8, item.getBuyingPrice()); ps.setDouble(9, item.getDiscount());
                        ps.setDouble(10, item.getTaxRate()); ps.setDouble(11, item.getLineTotal());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void loadItems(Sale sale) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM sale_items WHERE sale_id=?")) {
            ps.setString(1, sale.getId());
            ResultSet r = ps.executeQuery();
            List<Sale.SaleItem> items = new ArrayList<>();
            while (r.next()) items.add(mapItem(r));
            sale.setItems(items);
        }
    }

    public Optional<Sale> findById(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM sales WHERE id=?")) {
            ps.setString(1, id);
            ResultSet r = ps.executeQuery();
            if (!r.next()) return Optional.empty();
            Sale s = mapSale(r);
            loadItems(s);
            return Optional.of(s);
        }
    }

    public Optional<Sale> findByReceiptNumber(String receiptNumber) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM sales WHERE receipt_number=?")) {
            ps.setString(1, receiptNumber);
            ResultSet r = ps.executeQuery();
            if (!r.next()) return Optional.empty();
            Sale s = mapSale(r);
            loadItems(s);
            return Optional.of(s);
        }
    }

    public List<Sale> findByDateRange(LocalDateTime from, LocalDateTime to) throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE created_at >= ? AND created_at <= ? AND status='COMPLETED' ORDER BY created_at DESC";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from.toString()); ps.setString(2, to.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) { Sale s = mapSale(r); loadItems(s); list.add(s); }
        }
        return list;
    }

    public List<Sale> findByCashierId(String cashierId, LocalDateTime from, LocalDateTime to) throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE cashier_id=? AND created_at >= ? AND created_at <= ? AND status='COMPLETED' ORDER BY created_at DESC";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cashierId); ps.setString(2, from.toString()); ps.setString(3, to.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) { Sale s = mapSale(r); list.add(s); }
        }
        return list;
    }

    public List<Sale> findRecentCompleted(int limit) throws SQLException {
        List<Sale> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM sales WHERE status='COMPLETED' ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(mapSale(r));
        }
        return list;
    }

    public List<Sale> findPendingSync() throws SQLException {
        List<Sale> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM sales WHERE sync_status IN ('PENDING','MODIFIED')")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(mapSale(r));
        }
        return list;
    }

    public double sumRevenueToday() throws SQLException {
        String today = LocalDateTime.now().toLocalDate().toString();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(grand_total),0) FROM sales WHERE status='COMPLETED' AND created_at LIKE ?")) {
            ps.setString(1, today + "%");
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getDouble(1) : 0;
        }
    }

    public double sumProfitToday() throws SQLException {
        String today = LocalDateTime.now().toLocalDate().toString();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM((si.unit_price - si.buying_price) * si.quantity),0) " +
                "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
                "WHERE s.status='COMPLETED' AND s.created_at LIKE ?")) {
            ps.setString(1, today + "%");
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getDouble(1) : 0;
        }
    }

    public int countToday() throws SQLException {
        String today = LocalDateTime.now().toLocalDate().toString();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM sales WHERE status='COMPLETED' AND created_at LIKE ?")) {
            ps.setString(1, today + "%");
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }

    public List<Map<String,Object>> topProductsToday(int limit) throws SQLException {
        String today = LocalDateTime.now().toLocalDate().toString();
        List<Map<String,Object>> list = new ArrayList<>();
        String sql = "SELECT si.product_name, SUM(si.quantity) as total_qty, SUM(si.line_total) as total_rev " +
            "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
            "WHERE s.status='COMPLETED' AND s.created_at LIKE ? " +
            "GROUP BY si.product_id, si.product_name ORDER BY total_qty DESC LIMIT ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, today + "%"); ps.setInt(2, limit);
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("product_name", r.getString("product_name"));
                row.put("total_qty", r.getInt("total_qty"));
                row.put("total_rev", r.getDouble("total_rev"));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String,Object>> salesLast7Days() throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String day = LocalDateTime.now().minusDays(i).toLocalDate().toString();
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(grand_total),0) FROM sales WHERE status='COMPLETED' AND created_at LIKE ?")) {
                ps.setString(1, day + "%");
                ResultSet r = ps.executeQuery();
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("date", day);
                row.put("revenue", r.next() ? r.getDouble(1) : 0.0);
                list.add(row);
            }
        }
        return list;
    }

    public int countPendingSync() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM sales WHERE sync_status IN ('PENDING','MODIFIED')")) {
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }
}
