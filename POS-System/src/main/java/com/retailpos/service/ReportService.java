package com.retailpos.service;

import com.retailpos.model.Product;
import com.retailpos.repository.SaleRepository;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ReportService {
    private static ReportService instance;
    private final SaleRepository saleRepo = new SaleRepository();

    private ReportService() {}

    public static synchronized ReportService getInstance() {
        if (instance == null) instance = new ReportService();
        return instance;
    }

    public Map<String, Object> generateDailySalesReport(LocalDate date) throws Exception {
        return generatePeriodReport(date, date);
    }

    public Map<String, Object> generatePeriodReport(LocalDate from, LocalDate to) throws Exception {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("from", from.toString()); report.put("to", to.toString());

        try (Connection c = DatabaseManager.getConnection()) {
            // Sales totals
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) as cnt, COALESCE(SUM(grand_total),0) as rev, " +
                "COALESCE(SUM(discount_amount),0) as disc, COALESCE(SUM(tax_amount),0) as tax " +
                "FROM sales WHERE status='COMPLETED' AND created_at >= ? AND created_at <= ?")) {
                ps.setString(1, start.toString()); ps.setString(2, end.toString());
                ResultSet r = ps.executeQuery();
                if (r.next()) {
                    report.put("transaction_count", r.getInt("cnt"));
                    report.put("total_revenue", r.getDouble("rev"));
                    report.put("total_discount", r.getDouble("disc"));
                    report.put("total_tax", r.getDouble("tax"));
                }
            }
            // Profit
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM((si.unit_price - si.buying_price) * si.quantity),0) " +
                "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
                "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ?")) {
                ps.setString(1, start.toString()); ps.setString(2, end.toString());
                ResultSet r = ps.executeQuery();
                report.put("total_profit", r.next() ? r.getDouble(1) : 0.0);
            }
            // By payment method
            Map<String, Double> byMethod = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT payment_method, COALESCE(SUM(grand_total),0) as total " +
                "FROM sales WHERE status='COMPLETED' AND created_at >= ? AND created_at <= ? " +
                "GROUP BY payment_method ORDER BY total DESC")) {
                ps.setString(1, start.toString()); ps.setString(2, end.toString());
                ResultSet r = ps.executeQuery();
                while (r.next()) byMethod.put(r.getString("payment_method"), r.getDouble("total"));
            }
            report.put("by_payment_method", byMethod);
            // Top products
            report.put("top_products", saleRepo.topProductsToday(10));
        }
        return report;
    }

    public List<Map<String, Object>> getBestSellingProducts(LocalDate from, LocalDate to, int limit) throws Exception {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT si.product_id, si.product_name, SUM(si.quantity) as total_qty, " +
            "SUM(si.line_total) as total_rev, SUM((si.unit_price-si.buying_price)*si.quantity) as profit " +
            "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
            "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ? " +
            "GROUP BY si.product_id,si.product_name ORDER BY total_qty DESC LIMIT ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString()); ps.setInt(3, limit);
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("product_id", r.getString("product_id"));
                row.put("product_name", r.getString("product_name"));
                row.put("total_qty", r.getInt("total_qty"));
                row.put("total_rev", r.getDouble("total_rev"));
                row.put("profit", r.getDouble("profit"));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getLowStockReport() throws Exception {
        List<Product> low = ProductService.getInstance().getLowStock();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Product p : low) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product_id", p.getId()); row.put("product_name", p.getName());
            row.put("sku", p.getSku()); row.put("current_stock", p.getCurrentStock());
            row.put("minimum_stock", p.getMinimumStock()); row.put("barcode", p.getBarcode());
            list.add(row);
        }
        return list;
    }

    public Map<String, Object> getProfitReport(LocalDate from, LocalDate to) throws Exception {
        return generatePeriodReport(from, to);
    }

    public Map<String, Object> getSalesByCategoryReport(LocalDate from, LocalDate to) throws Exception {
        LocalDateTime start = from.atStartOfDay(); LocalDateTime end = to.atTime(23, 59, 59);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("from", from.toString()); report.put("to", to.toString());
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT c.name as category, COALESCE(SUM(si.quantity),0) as qty, " +
            "COALESCE(SUM(si.line_total),0) as rev FROM sale_items si " +
            "JOIN sales s ON si.sale_id=s.id " +
            "JOIN products p ON si.product_id=p.id " +
            "JOIN categories c ON p.category_id=c.id " +
            "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ? " +
            "GROUP BY c.name ORDER BY rev DESC";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("category", r.getString("category"));
                row.put("qty", r.getInt("qty")); row.put("revenue", r.getDouble("rev"));
                rows.add(row);
            }
        }
        report.put("categories", rows);
        return report;
    }

    public Map<String, Object> getSalesByPaymentMethod(LocalDate from, LocalDate to) throws Exception {
        Map<String, Object> report = generatePeriodReport(from, to);
        return report;
    }

    public Map<String, Object> getCashierPerformanceReport(LocalDate from, LocalDate to) throws Exception {
        LocalDateTime start = from.atStartOfDay(); LocalDateTime end = to.atTime(23, 59, 59);
        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT cashier_name, COUNT(*) as tx, COALESCE(SUM(grand_total),0) as rev " +
            "FROM sales WHERE status='COMPLETED' AND created_at >= ? AND created_at <= ? " +
            "GROUP BY cashier_id, cashier_name ORDER BY rev DESC";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("cashier_name", r.getString("cashier_name"));
                row.put("transactions", r.getInt("tx")); row.put("revenue", r.getDouble("rev"));
                rows.add(row);
            }
        }
        report.put("cashiers", rows); report.put("from", from.toString()); report.put("to", to.toString());
        return report;
    }

    public Map<String, Object> getTaxReport(LocalDate from, LocalDate to) throws Exception {
        LocalDateTime start = from.atStartOfDay(); LocalDateTime end = to.atTime(23, 59, 59);
        Map<String, Object> report = new LinkedHashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(grand_total),0) as rev, COALESCE(SUM(tax_amount),0) as tax, " +
                "COALESCE(SUM(subtotal),0) as pre_tax FROM sales " +
                "WHERE status='COMPLETED' AND created_at >= ? AND created_at <= ?")) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            if (r.next()) {
                report.put("total_revenue", r.getDouble("rev"));
                report.put("total_tax_collected", r.getDouble("tax"));
                report.put("pre_tax_revenue", r.getDouble("pre_tax"));
            }
        }
        report.put("from", from.toString()); report.put("to", to.toString());
        return report;
    }

    public Map<String, Object> getInventoryValuationReport() throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT p.name, p.sku, p.buying_price, p.selling_price, p.current_stock, " +
            "(p.buying_price * p.current_stock) as cost_value, " +
            "(p.selling_price * p.current_stock) as sell_value FROM products " +
            "WHERE status='active' AND sync_status!='DELETED' ORDER BY cost_value DESC";
        double totalCost = 0, totalSell = 0;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", r.getString("name")); row.put("sku", r.getString("sku"));
                row.put("buying_price", r.getDouble("buying_price"));
                row.put("selling_price", r.getDouble("selling_price"));
                row.put("stock", r.getInt("current_stock"));
                row.put("cost_value", r.getDouble("cost_value"));
                row.put("sell_value", r.getDouble("sell_value"));
                totalCost += r.getDouble("cost_value"); totalSell += r.getDouble("sell_value");
                items.add(row);
            }
        }
        report.put("items", items); report.put("total_cost_value", totalCost);
        report.put("total_sell_value", totalSell); report.put("potential_profit", totalSell - totalCost);
        return report;
    }
}
