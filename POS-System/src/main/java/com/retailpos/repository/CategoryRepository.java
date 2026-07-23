package com.retailpos.repository;

import com.retailpos.model.Category;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CategoryRepository {

    private Category map(ResultSet r) throws SQLException {
        Category c = new Category();
        c.setId(r.getString("id")); c.setName(r.getString("name")); c.setDescription(r.getString("description"));
        c.setSyncStatus(r.getString("sync_status"));
        String ca = r.getString("created_at"); if (ca != null) c.setCreatedAt(LocalDateTime.parse(ca));
        String ua = r.getString("updated_at"); if (ua != null) c.setUpdatedAt(LocalDateTime.parse(ua));
        return c;
    }

    public void insert(Category cat) throws SQLException {
        String sql = "INSERT INTO categories(id,name,description,sync_status,created_at,updated_at) VALUES(?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String now = LocalDateTime.now().toString();
            ps.setString(1, cat.getId()); ps.setString(2, cat.getName()); ps.setString(3, cat.getDescription());
            ps.setString(4, "PENDING"); ps.setString(5, now); ps.setString(6, now);
            ps.executeUpdate();
        }
    }

    public void update(Category cat) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE categories SET name=?,description=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setString(1, cat.getName()); ps.setString(2, cat.getDescription());
            ps.setString(3, LocalDateTime.now().toString()); ps.setString(4, cat.getId());
            ps.executeUpdate();
        }
    }

    public Optional<Category> findById(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM categories WHERE id=?")) {
            ps.setString(1, id);
            ResultSet r = ps.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        }
    }

    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM categories ORDER BY name")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }
}
