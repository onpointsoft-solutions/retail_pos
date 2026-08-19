package com.retailpos.repository;

import com.retailpos.model.Expense;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ExpenseRepository {

    public void insert(Expense e) throws SQLException {
        String sql = "INSERT INTO expenses(id,category,description,amount,date,reference,created_by,sync_status) " +
                     "VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getCategory());
            ps.setString(3, e.getDescription());
            ps.setDouble(4, e.getAmount());
            ps.setString(5, e.getDate() != null ? e.getDate().toString() : LocalDate.now().toString());
            ps.setString(6, e.getReference());
            ps.setString(7, e.getCreatedBy());
            ps.setString(8, e.getSyncStatus() != null ? e.getSyncStatus() : "PENDING");
            ps.executeUpdate();
        }
    }

    public void update(Expense e) throws SQLException {
        String sql = "UPDATE expenses SET category=?,description=?,amount=?,date=?,reference=?,sync_status='MODIFIED' WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getCategory());
            ps.setString(2, e.getDescription());
            ps.setDouble(3, e.getAmount());
            ps.setString(4, e.getDate() != null ? e.getDate().toString() : LocalDate.now().toString());
            ps.setString(5, e.getReference());
            ps.setString(6, e.getId());
            ps.executeUpdate();
        }
    }

    public void delete(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM expenses WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public List<Expense> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE date >= ? AND date <= ? ORDER BY date DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    /** Returns total expenses grouped by month (YYYY-MM) for a date range. */
    public Map<String, Double> monthlyTotals(LocalDate from, LocalDate to) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT SUBSTR(date,1,7) as month, COALESCE(SUM(amount),0) as total " +
                     "FROM expenses WHERE date >= ? AND date <= ? GROUP BY month ORDER BY month";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) result.put(r.getString("month"), r.getDouble("total"));
        }
        return result;
    }

    /** Returns expense totals grouped by category for a date range. */
    public Map<String, Double> byCategory(LocalDate from, LocalDate to) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT category, COALESCE(SUM(amount),0) as total " +
                     "FROM expenses WHERE date >= ? AND date <= ? GROUP BY category ORDER BY total DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) result.put(r.getString("category"), r.getDouble("total"));
        }
        return result;
    }

    public double sumByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM expenses WHERE date >= ? AND date <= ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getDouble(1) : 0.0;
        }
    }

    private Expense map(ResultSet r) throws SQLException {
        Expense e = new Expense();
        e.setId(r.getString("id"));
        e.setCategory(r.getString("category"));
        e.setDescription(r.getString("description"));
        e.setAmount(r.getDouble("amount"));
        String d = r.getString("date");
        if (d != null && !d.isBlank()) e.setDate(LocalDate.parse(d.substring(0, 10)));
        e.setReference(r.getString("reference"));
        e.setCreatedBy(r.getString("created_by"));
        e.setSyncStatus(r.getString("sync_status"));
        return e;
    }
}
