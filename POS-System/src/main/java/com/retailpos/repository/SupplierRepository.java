package com.retailpos.repository;

import com.retailpos.model.Supplier;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SupplierRepository {

    private Supplier map(ResultSet r) throws SQLException {
        Supplier s = new Supplier();
        s.setId(r.getString("id")); s.setName(r.getString("name")); s.setPhone(r.getString("phone"));
        s.setEmail(r.getString("email")); s.setAddress(r.getString("address")); s.setBalance(r.getDouble("balance"));
        s.setSyncStatus(r.getString("sync_status"));
        String ca = r.getString("created_at"); if (ca != null) s.setCreatedAt(LocalDateTime.parse(ca));
        String ua = r.getString("updated_at"); if (ua != null) s.setUpdatedAt(LocalDateTime.parse(ua));
        return s;
    }

    public void insert(Supplier sup) throws SQLException {
        String sql = "INSERT INTO suppliers(id,name,phone,email,address,balance,sync_status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String now = LocalDateTime.now().toString();
            ps.setString(1, sup.getId()); ps.setString(2, sup.getName()); ps.setString(3, sup.getPhone());
            ps.setString(4, sup.getEmail()); ps.setString(5, sup.getAddress()); ps.setDouble(6, sup.getBalance());
            ps.setString(7, "PENDING"); ps.setString(8, now); ps.setString(9, now);
            ps.executeUpdate();
        }
    }

    public void update(Supplier sup) throws SQLException {
        String sql = "UPDATE suppliers SET name=?,phone=?,email=?,address=?,sync_status='MODIFIED',updated_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sup.getName()); ps.setString(2, sup.getPhone()); ps.setString(3, sup.getEmail());
            ps.setString(4, sup.getAddress()); ps.setString(5, LocalDateTime.now().toString()); ps.setString(6, sup.getId());
            ps.executeUpdate();
        }
    }

    public Optional<Supplier> findById(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM suppliers WHERE id=?")) {
            ps.setString(1, id);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public List<Supplier> findAll() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM suppliers ORDER BY name")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public void updateBalance(String id, double balance) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE suppliers SET balance=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setDouble(1, balance); ps.setString(2, LocalDateTime.now().toString()); ps.setString(3, id);
            ps.executeUpdate();
        }
    }
}
