package com.retailpos.repository;

import com.retailpos.model.Product;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ProductRepository {

    private Product map(ResultSet r) throws SQLException {
        Product p = new Product();
        p.setId(r.getString("id"));
        p.setBarcode(r.getString("barcode"));
        p.setQrCode(r.getString("qr_code"));
        p.setSku(r.getString("sku"));
        p.setName(r.getString("name"));
        p.setCategoryId(r.getString("category_id"));
        p.setBuyingPrice(r.getDouble("buying_price"));
        p.setSellingPrice(r.getDouble("selling_price"));
        p.setWholesalePrice(r.getDouble("wholesale_price"));
        p.setCurrentStock(r.getInt("current_stock"));
        p.setMinimumStock(r.getInt("minimum_stock"));
        p.setPreferredOrderQuantity(r.getInt("preferred_order_quantity"));
        p.setTaxRate(r.getDouble("tax_rate"));
        p.setDiscount(r.getDouble("discount"));
        p.setSupplierId(r.getString("supplier_id"));
        p.setDescription(r.getString("description"));
        p.setImagePath(r.getString("image_path"));
        p.setUnit(r.getString("unit"));
        p.setStatus(r.getString("status"));
        p.setTrackExpiry(r.getInt("track_expiry") == 1);
        p.setSyncStatus(r.getString("sync_status"));
        p.setVersion(r.getLong("version"));
        String ca = r.getString("created_at"); if (ca != null) p.setCreatedAt(LocalDateTime.parse(ca));
        String ua = r.getString("updated_at"); if (ua != null) p.setUpdatedAt(LocalDateTime.parse(ua));
        String da = r.getString("deleted_at"); if (da != null) p.setDeletedAt(LocalDateTime.parse(da));
        return p;
    }

    public void insert(Product p) throws SQLException {
        String sql = "INSERT INTO products(id,barcode,qr_code,sku,name,category_id,buying_price," +
            "selling_price,wholesale_price,current_stock,minimum_stock,preferred_order_quantity,tax_rate,discount," +
            "supplier_id,description,image_path,unit,status,track_expiry,sync_status,version,created_at,updated_at) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId()); ps.setString(2, p.getBarcode()); ps.setString(3, p.getQrCode());
            ps.setString(4, p.getSku()); ps.setString(5, p.getName()); ps.setString(6, p.getCategoryId());
            ps.setDouble(7, p.getBuyingPrice()); ps.setDouble(8, p.getSellingPrice());
            ps.setDouble(9, p.getWholesalePrice()); ps.setInt(10, p.getCurrentStock());
            ps.setInt(11, p.getMinimumStock()); ps.setInt(12, p.getPreferredOrderQuantity()); ps.setDouble(13, p.getTaxRate());
            ps.setDouble(14, p.getDiscount()); ps.setString(15, p.getSupplierId());
            ps.setString(16, p.getDescription()); ps.setString(17, p.getImagePath());
            ps.setString(18, p.getUnit());
            ps.setString(19, p.getStatus()); ps.setInt(20, p.isTrackExpiry() ? 1 : 0);
            ps.setString(21, p.getSyncStatus()); ps.setLong(22, p.getVersion());
            ps.setString(23, p.getCreatedAt() != null ? p.getCreatedAt().toString() : java.time.LocalDateTime.now().toString());
            ps.setString(24, p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : java.time.LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public void update(Product p) throws SQLException {
        String sql = "UPDATE products SET barcode=?,qr_code=?,sku=?,name=?,category_id=?,buying_price=?," +
            "selling_price=?,wholesale_price=?,current_stock=?,minimum_stock=?,preferred_order_quantity=?,tax_rate=?,discount=?," +
            "supplier_id=?,description=?,image_path=?,unit=?,status=?,track_expiry=?,sync_status=?," +
            "version=version+1,updated_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getBarcode()); ps.setString(2, p.getQrCode()); ps.setString(3, p.getSku());
            ps.setString(4, p.getName()); ps.setString(5, p.getCategoryId()); ps.setDouble(6, p.getBuyingPrice());
            ps.setDouble(7, p.getSellingPrice()); ps.setDouble(8, p.getWholesalePrice());
            ps.setInt(9, p.getCurrentStock()); ps.setInt(10, p.getMinimumStock()); ps.setInt(11, p.getPreferredOrderQuantity());
            ps.setDouble(12, p.getTaxRate()); ps.setDouble(13, p.getDiscount());
            ps.setString(14, p.getSupplierId()); ps.setString(15, p.getDescription());
            ps.setString(16, p.getImagePath()); ps.setString(17, p.getUnit());
            ps.setString(18, p.getStatus());
            ps.setInt(19, p.isTrackExpiry() ? 1 : 0); ps.setString(20, p.getSyncStatus());
            ps.setString(21, LocalDateTime.now().toString()); ps.setString(22, p.getId());
            ps.executeUpdate();
        }
    }

    public void softDelete(String id) throws SQLException {
        String sql = "UPDATE products SET sync_status='DELETED',deleted_at=?,updated_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String now = LocalDateTime.now().toString();
            ps.setString(1, now); ps.setString(2, now); ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public Optional<Product> findById(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM products WHERE id=?")) {
            ps.setString(1, id);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public Optional<Product> findByBarcode(String barcode) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM products WHERE barcode=? AND sync_status!='DELETED' AND status='active' LIMIT 1")) {
            ps.setString(1, barcode);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public Optional<Product> findByQrCode(String qrCode) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM products WHERE qr_code=? AND sync_status!='DELETED' AND status='active' LIMIT 1")) {
            ps.setString(1, qrCode);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public Optional<Product> findBySku(String sku) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM products WHERE sku=? AND sync_status!='DELETED' AND status='active' LIMIT 1")) {
            ps.setString(1, sku);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public List<Product> search(String query, int limit, int offset) throws SQLException {
        String q = "%" + query.toLowerCase() + "%";
        String sql = "SELECT * FROM products WHERE sync_status!='DELETED' AND status='active' " +
            "AND (lower(name) LIKE ? OR barcode LIKE ? OR sku LIKE ?) ORDER BY name LIMIT ? OFFSET ?";
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            ps.setInt(4, limit); ps.setInt(5, offset);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<Product> findAll(int limit, int offset) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE sync_status!='DELETED' ORDER BY name LIMIT ? OFFSET ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit); ps.setInt(2, offset);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<Product> findActive() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM products WHERE status='active' AND sync_status!='DELETED' ORDER BY name")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<Product> findByCategoryId(String categoryId, int limit, int offset) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE category_id=? AND status='active' AND sync_status!='DELETED' " +
            "ORDER BY name LIMIT ? OFFSET ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, categoryId); ps.setInt(2, limit); ps.setInt(3, offset);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<Product> findLowStock() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM products WHERE status='active' AND sync_status!='DELETED' " +
                "AND current_stock <= minimum_stock ORDER BY current_stock ASC")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<Product> findPendingSync() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM products WHERE sync_status IN ('PENDING','MODIFIED','DELETED')")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public void updateStock(String id, int newStock) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE products SET current_stock=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setInt(1, newStock); ps.setString(2, LocalDateTime.now().toString()); ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public int countActive() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE status='active' AND sync_status!='DELETED'")) {
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }

    public double getTotalStockValue() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(buying_price * current_stock),0) FROM products " +
                "WHERE status='active' AND sync_status!='DELETED'")) {
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getDouble(1) : 0;
        }
    }

    public int countPendingSync() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE sync_status IN ('PENDING','MODIFIED','DELETED')")) {
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }
}
