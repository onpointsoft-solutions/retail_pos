package com.retailpos.repository;

import com.retailpos.model.User;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UserRepository {

    private User map(ResultSet r) throws SQLException {
        User u = new User();
        u.setId(r.getString("id"));
        u.setUsername(r.getString("username"));
        u.setPasswordHash(r.getString("password_hash"));
        u.setRole(r.getString("role"));
        u.setFullName(r.getString("full_name"));
        u.setActive(r.getInt("active") == 1);
        u.setFailedLoginAttempts(r.getInt("failed_login_attempts"));
        String lu = r.getString("lockout_until");
        if (lu != null) u.setLockoutUntil(LocalDateTime.parse(lu));
        u.setSyncStatus(r.getString("sync_status"));
        String ca = r.getString("created_at"); if (ca != null) u.setCreatedAt(LocalDateTime.parse(ca));
        String ua = r.getString("updated_at"); if (ua != null) u.setUpdatedAt(LocalDateTime.parse(ua));
        return u;
    }

    public void insert(User u) throws SQLException {
        String sql = "INSERT INTO users(id,username,password_hash,role,full_name,active," +
            "failed_login_attempts,sync_status,created_at,updated_at) VALUES(?,?,?,?,?,?,0,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getId()); ps.setString(2, u.getUsername());
            ps.setString(3, u.getPasswordHash()); ps.setString(4, u.getRole());
            ps.setString(5, u.getFullName()); ps.setInt(6, u.isActive() ? 1 : 0);
            ps.setString(7, u.getSyncStatus() != null ? u.getSyncStatus() : "PENDING");
            ps.setString(8, LocalDateTime.now().toString()); ps.setString(9, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public void update(User u) throws SQLException {
        String sql = "UPDATE users SET username=?,role=?,full_name=?,active=?,sync_status=?,updated_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsername()); ps.setString(2, u.getRole());
            ps.setString(3, u.getFullName()); ps.setInt(4, u.isActive() ? 1 : 0);
            ps.setString(5, "MODIFIED"); ps.setString(6, LocalDateTime.now().toString());
            ps.setString(7, u.getId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(String id, String newHash) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET password_hash=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setString(1, newHash); ps.setString(2, LocalDateTime.now().toString()); ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public void configureInitialAdmin(String fullName, String username, String passwordHash) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET username=?,password_hash=?,full_name=?,active=1,updated_at=? WHERE role='ADMIN'")) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, fullName);
            ps.setString(4, LocalDateTime.now().toString());
            if (ps.executeUpdate() == 0) throw new SQLException("Initial administrator account was not found");
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username=?")) {
            ps.setString(1, username);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public Optional<User> findById(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setString(1, id);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public void updateFailedAttempts(String id, int attempts, LocalDateTime lockoutUntil) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET failed_login_attempts=?,lockout_until=?,updated_at=? WHERE id=?")) {
            ps.setInt(1, attempts);
            ps.setString(2, lockoutUntil != null ? lockoutUntil.toString() : null);
            ps.setString(3, LocalDateTime.now().toString());
            ps.setString(4, id);
            ps.executeUpdate();
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users ORDER BY full_name")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }
}
