package com.retailpos.repository;

import com.retailpos.model.InventoryMovement;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class InventoryRepository {

    private InventoryMovement map(ResultSet r) throws SQLException {
        InventoryMovement m = new InventoryMovement();
        m.setId(r.getString("id")); m.setProductId(r.getString("product_id"));
        m.setProductName(r.getString("product_name")); m.setType(r.getString("type"));
        m.setQuantity(r.getInt("quantity")); m.setReason(r.getString("reason"));
        m.setBatchNumber(r.getString("batch_number")); m.setUserId(r.getString("user_id"));
        m.setSyncStatus(r.getString("sync_status"));
        String ed = r.getString("expiry_date"); if (ed != null) m.setExpiryDate(LocalDate.parse(ed));
        String ca = r.getString("created_at"); if (ca != null) m.setCreatedAt(LocalDateTime.parse(ca));
        return m;
    }

    public void insertMovement(InventoryMovement m) throws SQLException {
        String sql = "INSERT INTO inventory_movements(id,product_id,product_name,type,quantity,reason," +
            "batch_number,expiry_date,user_id,sync_status,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getId()); ps.setString(2, m.getProductId()); ps.setString(3, m.getProductName());
            ps.setString(4, m.getType()); ps.setInt(5, m.getQuantity()); ps.setString(6, m.getReason());
            ps.setString(7, m.getBatchNumber());
            ps.setString(8, m.getExpiryDate() != null ? m.getExpiryDate().toString() : null);
            ps.setString(9, m.getUserId()); ps.setString(10, m.getSyncStatus() != null ? m.getSyncStatus() : "PENDING");
            ps.setString(11, m.getCreatedAt() != null ? m.getCreatedAt().toString() : LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public List<InventoryMovement> findByProductId(String productId) throws SQLException {
        List<InventoryMovement> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM inventory_movements WHERE product_id=? ORDER BY created_at DESC")) {
            ps.setString(1, productId);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<InventoryMovement> findByDateRange(LocalDateTime from, LocalDateTime to) throws SQLException {
        List<InventoryMovement> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM inventory_movements WHERE created_at >= ? AND created_at <= ? ORDER BY created_at DESC")) {
            ps.setString(1, from.toString()); ps.setString(2, to.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<InventoryMovement> findExpiredBatches() throws SQLException {
        List<InventoryMovement> list = new ArrayList<>();
        String today = LocalDate.now().toString();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM inventory_movements WHERE type='STOCK_IN' AND expiry_date IS NOT NULL " +
                "AND expiry_date <= ? ORDER BY expiry_date ASC")) {
            ps.setString(1, today);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }
}
