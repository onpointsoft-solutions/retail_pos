package com.retailpos.repository;

import com.retailpos.model.Customer;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CustomerRepository {

    private Customer map(ResultSet r) throws SQLException {
        Customer c = new Customer();
        c.setId(r.getString("id")); c.setName(r.getString("name"));
        c.setPhone(r.getString("phone")); c.setEmail(r.getString("email"));
        c.setLoyaltyPoints(r.getInt("loyalty_points")); c.setCreditBalance(r.getDouble("credit_balance"));
        c.setSyncStatus(r.getString("sync_status"));
        String ca = r.getString("created_at"); if (ca != null) c.setCreatedAt(LocalDateTime.parse(ca));
        String ua = r.getString("updated_at"); if (ua != null) c.setUpdatedAt(LocalDateTime.parse(ua));
        return c;
    }

    public void insert(Customer cu) throws SQLException {
        String sql = "INSERT INTO customers(id,name,phone,email,loyalty_points,credit_balance,sync_status,created_at,updated_at) " +
            "VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String now = LocalDateTime.now().toString();
            ps.setString(1, cu.getId()); ps.setString(2, cu.getName()); ps.setString(3, cu.getPhone());
            ps.setString(4, cu.getEmail()); ps.setInt(5, cu.getLoyaltyPoints()); ps.setDouble(6, cu.getCreditBalance());
            ps.setString(7, "PENDING"); ps.setString(8, now); ps.setString(9, now);
            ps.executeUpdate();
        }
    }

    public void update(Customer cu) throws SQLException {
        String sql = "UPDATE customers SET name=?,phone=?,email=?,sync_status='MODIFIED',updated_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cu.getName()); ps.setString(2, cu.getPhone()); ps.setString(3, cu.getEmail());
            ps.setString(4, LocalDateTime.now().toString()); ps.setString(5, cu.getId());
            ps.executeUpdate();
        }
    }

    public Optional<Customer> findById(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM customers WHERE id=?")) {
            ps.setString(1, id);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public Optional<Customer> findByPhone(String phone) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM customers WHERE phone=?")) {
            ps.setString(1, phone);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public List<Customer> search(String query) throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE lower(name) LIKE ? OR phone=? OR lower(email)=? ORDER BY name LIMIT 20";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String q = query.toLowerCase();
            ps.setString(1, q + "%"); ps.setString(2, query); ps.setString(3, q);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM customers ORDER BY name")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public void updateLoyaltyPoints(String id, int points) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE customers SET loyalty_points=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setInt(1, points); ps.setString(2, LocalDateTime.now().toString()); ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public void updateCreditBalance(String id, double balance) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE customers SET credit_balance=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setDouble(1, balance); ps.setString(2, LocalDateTime.now().toString()); ps.setString(3, id);
            ps.executeUpdate();
        }
    }
}
