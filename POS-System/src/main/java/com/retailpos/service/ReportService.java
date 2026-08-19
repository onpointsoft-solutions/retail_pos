package com.retailpos.service;

import com.retailpos.model.Product;
import com.retailpos.repository.ExpenseRepository;
import com.retailpos.repository.SaleRepository;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ReportService {
    private static ReportService instance;
    private final SaleRepository saleRepo = new SaleRepository();
    private final ExpenseRepository expenseRepo = new ExpenseRepository();

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
            "WHERE status='active' AND sync_status!='DELETED' AND deleted_at IS NULL ORDER BY cost_value DESC";
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

    // ═════════════════════════════════════════════════════════════════════════
    //  NEW — Income vs Expenses / P&L / Market Basket
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns monthly income (revenue), COGS, gross profit, expenses, and net profit
     * for the given date range, grouped by month (YYYY-MM).
     *
     * Map keys per entry:
     *   month, income, cogs, gross_profit, expenses, net_profit, gross_margin, net_margin
     */
    public List<Map<String, Object>> getMonthlyIncomeVsExpenses(LocalDate from, LocalDate to) throws Exception {
        // 1. Monthly revenue + COGS from sales
        String revSql =
            "SELECT SUBSTR(s.created_at,1,7) as month, " +
            "  COALESCE(SUM(s.grand_total),0) as income, " +
            "  COALESCE(SUM(si.buying_price * si.quantity),0) as cogs " +
            "FROM sales s " +
            "LEFT JOIN sale_items si ON si.sale_id = s.id " +
            "WHERE s.status='COMPLETED' " +
            "  AND s.created_at >= ? AND s.created_at <= ? " +
            "GROUP BY month ORDER BY month";

        Map<String, double[]> monthly = new LinkedHashMap<>();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23, 59, 59);

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(revSql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                String m = r.getString("month");
                // [0]=income [1]=cogs [2]=expenses
                monthly.put(m, new double[]{ r.getDouble("income"), r.getDouble("cogs"), 0.0 });
            }
        }

        // 2. Monthly expenses
        Map<String, Double> expMonthly = expenseRepo.monthlyTotals(from, to);
        for (Map.Entry<String, Double> e : expMonthly.entrySet()) {
            monthly.computeIfAbsent(e.getKey(), k -> new double[3])[2] = e.getValue();
        }

        // 3. Fill months with no data to make the chart continuous
        fillMissingMonths(monthly, from, to);

        // 4. Build result list
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, double[]> e : monthly.entrySet()) {
            double income      = e.getValue()[0];
            double cogs        = e.getValue()[1];
            double expenses    = e.getValue()[2];
            double grossProfit = income - cogs;
            double netProfit   = grossProfit - expenses;
            double grossMargin = income > 0 ? (grossProfit / income) * 100.0 : 0.0;
            double netMargin   = income > 0 ? (netProfit   / income) * 100.0 : 0.0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month",        e.getKey());
            row.put("income",       income);
            row.put("cogs",         cogs);
            row.put("gross_profit", grossProfit);
            row.put("expenses",     expenses);
            row.put("net_profit",   netProfit);
            row.put("gross_margin", Math.round(grossMargin * 10.0) / 10.0);
            row.put("net_margin",   Math.round(netMargin   * 10.0) / 10.0);
            result.add(row);
        }
        return result;
    }

    /**
     * Full Profit & Loss statement for a period — waterfall-style rows.
     *
     * Sections: Revenue, Cost of Goods Sold, Gross Profit,
     *           Operating Expenses (by category), Net Profit.
     */
    public Map<String, Object> getProfitLossStatement(LocalDate from, LocalDate to) throws Exception {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23, 59, 59);
        Map<String, Object> pl = new LinkedHashMap<>();
        pl.put("period_from", from.toString());
        pl.put("period_to",   to.toString());

        // ── Revenue section ───────────────────────────────────────────────────
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(grand_total),0) as rev, " +
                "       COALESCE(SUM(discount_amount),0) as disc, " +
                "       COALESCE(SUM(tax_amount),0) as tax, " +
                "       COUNT(*) as tx_count " +
                "FROM sales WHERE status='COMPLETED' " +
                "  AND created_at >= ? AND created_at <= ?")) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            if (r.next()) {
                pl.put("gross_revenue",      r.getDouble("rev"));
                pl.put("total_discounts",    r.getDouble("disc"));
                pl.put("net_revenue",        r.getDouble("rev")); // discounts already deducted in grand_total
                pl.put("tax_collected",      r.getDouble("tax"));
                pl.put("transaction_count",  r.getInt("tx_count"));
            }
        }

        // ── Revenue by payment method ─────────────────────────────────────────
        Map<String, Double> byMethod = new LinkedHashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT payment_method, COALESCE(SUM(grand_total),0) as total " +
                "FROM sales WHERE status='COMPLETED' AND created_at >= ? AND created_at <= ? " +
                "GROUP BY payment_method ORDER BY total DESC")) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) byMethod.put(r.getString("payment_method"), r.getDouble("total"));
        }
        pl.put("revenue_by_payment", byMethod);

        // ── COGS ──────────────────────────────────────────────────────────────
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(si.buying_price * si.quantity),0) as cogs, " +
                "       COALESCE(SUM(si.line_total),0) as sold_value " +
                "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
                "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ?")) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            if (r.next()) {
                double cogs       = r.getDouble("cogs");
                double netRev     = ((Number) pl.getOrDefault("net_revenue", 0.0)).doubleValue();
                double grossProfit = netRev - cogs;
                double grossMargin = netRev > 0 ? (grossProfit / netRev) * 100.0 : 0.0;
                pl.put("cost_of_goods_sold", cogs);
                pl.put("gross_profit",       grossProfit);
                pl.put("gross_margin_pct",   Math.round(grossMargin * 10.0) / 10.0);
            }
        }

        // ── Operating expenses (by category) ──────────────────────────────────
        Map<String, Double> expByCat = expenseRepo.byCategory(from, to);
        double totalExpenses = expByCat.values().stream().mapToDouble(Double::doubleValue).sum();
        pl.put("expenses_by_category", expByCat);
        pl.put("total_operating_expenses", totalExpenses);

        // ── Stock-movement cost (purchase orders received in period) ──────────
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(poi.received_qty * poi.buying_price),0) as po_cost " +
                "FROM purchase_order_items poi JOIN purchase_orders po ON poi.po_id=po.id " +
                "WHERE po.status='RECEIVED' AND po.updated_at >= ? AND po.updated_at <= ?")) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            pl.put("purchase_orders_cost", r.next() ? r.getDouble("po_cost") : 0.0);
        }

        // ── Net profit ────────────────────────────────────────────────────────
        double grossProfit  = ((Number) pl.getOrDefault("gross_profit", 0.0)).doubleValue();
        double netProfit    = grossProfit - totalExpenses;
        double netRev       = ((Number) pl.getOrDefault("net_revenue",  0.0)).doubleValue();
        double netMargin    = netRev > 0 ? (netProfit / netRev) * 100.0 : 0.0;
        pl.put("net_profit",          netProfit);
        pl.put("net_margin_pct",      Math.round(netMargin * 10.0) / 10.0);
        pl.put("is_profitable",       netProfit >= 0);

        return pl;
    }

    /**
     * Market basket — products bought together.
     *
     * Returns pairs of products with:
     *   product_a, product_b, co_occurrence_count,
     *   support (fraction of transactions both appear in),
     *   confidence_a_to_b (given A, how often B is also bought)
     *
     * Sorted by co_occurrence_count DESC, limited to top {@code limit} pairs.
     */
    public List<Map<String, Object>> getMarketBasketAnalysis(LocalDate from, LocalDate to, int limit) throws Exception {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23, 59, 59);

        // Count total transactions in period
        int totalTx;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM sales WHERE status='COMPLETED' " +
                "  AND created_at >= ? AND created_at <= ?")) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            totalTx = r.next() ? r.getInt(1) : 1;
        }
        if (totalTx == 0) return Collections.emptyList();

        // Self-join sale_items to find co-occurring product pairs in the same sale
        String sql =
            "SELECT a.product_name as product_a, b.product_name as product_b, " +
            "       COUNT(DISTINCT a.sale_id) as co_count, " +
            "       COUNT(DISTINCT a.sale_id) * 1.0 / ? as support " +
            "FROM sale_items a " +
            "JOIN sale_items b ON a.sale_id = b.sale_id AND a.product_id < b.product_id " +
            "JOIN sales s ON a.sale_id = s.id " +
            "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ? " +
            "GROUP BY a.product_id, b.product_id " +
            "HAVING co_count >= 2 " +
            "ORDER BY co_count DESC " +
            "LIMIT ?";

        // Also fetch individual product frequencies for confidence calculation
        Map<String, Integer> productFreq = new HashMap<>();
        String freqSql =
            "SELECT si.product_name, COUNT(DISTINCT si.sale_id) as freq " +
            "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
            "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ? " +
            "GROUP BY si.product_id";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(freqSql)) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) productFreq.put(r.getString("product_name"), r.getInt("freq"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, totalTx);
            ps.setString(2, start.toString());
            ps.setString(3, end.toString());
            ps.setInt(4, limit);
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                String pA      = r.getString("product_a");
                String pB      = r.getString("product_b");
                int    coCount = r.getInt("co_count");
                double support = r.getDouble("support");

                int freqA = productFreq.getOrDefault(pA, 1);
                int freqB = productFreq.getOrDefault(pB, 1);
                double confAtoB = freqA > 0 ? (coCount * 1.0 / freqA) : 0;
                double confBtoA = freqB > 0 ? (coCount * 1.0 / freqB) : 0;
                // Lift = support / (P(A) * P(B))
                double pA_val = freqA * 1.0 / totalTx;
                double pB_val = freqB * 1.0 / totalTx;
                double lift   = (pA_val * pB_val) > 0 ? support / (pA_val * pB_val) : 0;

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("product_a",        pA);
                row.put("product_b",        pB);
                row.put("co_occurrences",   coCount);
                row.put("support",          Math.round(support * 1000.0) / 10.0);   // percent
                row.put("confidence_a_b",   Math.round(confAtoB * 1000.0) / 10.0);  // percent
                row.put("confidence_b_a",   Math.round(confBtoA * 1000.0) / 10.0);
                row.put("lift",             Math.round(lift * 100.0) / 100.0);
                row.put("freq_a",           freqA);
                row.put("freq_b",           freqB);
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Daily revenue + profit for a sparkline / area chart (last N days).
     */
    public List<Map<String, Object>> getDailyTrend(LocalDate from, LocalDate to) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            String day = d.toString();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day);
            try (Connection c = DatabaseManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(s.grand_total),0) as rev, " +
                    "       COALESCE(SUM((si.unit_price-si.buying_price)*si.quantity),0) as profit " +
                    "FROM sales s LEFT JOIN sale_items si ON si.sale_id=s.id " +
                    "WHERE s.status='COMPLETED' AND s.created_at LIKE ?")) {
                ps.setString(1, day + "%");
                ResultSet r = ps.executeQuery();
                if (r.next()) {
                    row.put("revenue", r.getDouble("rev"));
                    row.put("profit",  r.getDouble("profit"));
                } else {
                    row.put("revenue", 0.0);
                    row.put("profit",  0.0);
                }
            }
            result.add(row);
            d = d.plusDays(1);
        }
        return result;
    }

    /**
     * Returns top N products by revenue for the period, plus category breakdown for a donut.
     */
    public Map<String, Object> getRevenueBreakdown(LocalDate from, LocalDate to, int topN) throws Exception {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23, 59, 59);
        Map<String, Object> result = new LinkedHashMap<>();

        // Top products
        List<Map<String, Object>> topProducts = new ArrayList<>();
        String prodSql =
            "SELECT si.product_name, COALESCE(SUM(si.line_total),0) as rev, " +
            "       COALESCE(SUM(si.quantity),0) as qty, " +
            "       COALESCE(SUM((si.unit_price-si.buying_price)*si.quantity),0) as profit " +
            "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
            "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ? " +
            "GROUP BY si.product_id,si.product_name ORDER BY rev DESC LIMIT ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(prodSql)) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString()); ps.setInt(3, topN);
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name",    r.getString("product_name"));
                row.put("revenue", r.getDouble("rev"));
                row.put("qty",     r.getInt("qty"));
                row.put("profit",  r.getDouble("profit"));
                topProducts.add(row);
            }
        }
        result.put("top_products", topProducts);

        // Category breakdown
        List<Map<String, Object>> categories = new ArrayList<>();
        String catSql =
            "SELECT COALESCE(c.name,'Uncategorised') as cat, " +
            "       COALESCE(SUM(si.line_total),0) as rev " +
            "FROM sale_items si JOIN sales s ON si.sale_id=s.id " +
            "LEFT JOIN products p ON si.product_id=p.id " +
            "LEFT JOIN categories c ON p.category_id=c.id " +
            "WHERE s.status='COMPLETED' AND s.created_at >= ? AND s.created_at <= ? " +
            "GROUP BY cat ORDER BY rev DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(catSql)) {
            ps.setString(1, start.toString()); ps.setString(2, end.toString());
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("category", r.getString("cat"));
                row.put("revenue",  r.getDouble("rev"));
                categories.add(row);
            }
        }
        result.put("categories", categories);
        result.put("period_from", from.toString());
        result.put("period_to",   to.toString());
        return result;
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Ensures every month between from and to has an entry (zero-filled). */
    private void fillMissingMonths(Map<String, double[]> map, LocalDate from, LocalDate to) {
        YearMonth cursor = YearMonth.from(from);
        YearMonth last   = YearMonth.from(to);
        while (!cursor.isAfter(last)) {
            map.computeIfAbsent(cursor.toString(), k -> new double[3]);
            cursor = cursor.plusMonths(1);
        }
        // Re-sort
        List<Map.Entry<String, double[]>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        map.clear();
        entries.forEach(e -> map.put(e.getKey(), e.getValue()));
    }
}
