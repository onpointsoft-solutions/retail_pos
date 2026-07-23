package com.retailpos.view;

import com.retailpos.model.Sale;
import com.retailpos.service.AuthService;
import com.retailpos.service.SaleService;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel implements SaleService.SaleListener {
    private JLabel salesCountLabel, revenueLabel, profitLabel, stockValueLabel;
    private JLabel pendingSyncLabel, lowStockLabel;
    private DefaultTableModel recentSalesModel;
    private DefaultTableModel topProductsModel;
    private TrendChart trendChart;
    private Timer refreshTimer;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(RetailThemeManager.SURFACE);
        build();
        SaleService.getInstance().addListener(this);
        startRefreshTimer();
        loadData();
    }

    private void build() {
        // Top metric cards
        JPanel metrics = new JPanel(new GridLayout(1, 6, 10, 0));
        metrics.setOpaque(false);
        salesCountLabel  = buildMetricCard(metrics, "Today's Sales", "0", RetailThemeManager.PRIMARY);
        revenueLabel     = buildMetricCard(metrics, "Revenue", "KES 0.00", RetailThemeManager.ACCENT);
        profitLabel      = buildMetricCard(metrics, "Profit", "KES 0.00", new Color(124, 58, 237));
        stockValueLabel  = buildMetricCard(metrics, "Stock Value", "KES 0.00", RetailThemeManager.WARNING);
        pendingSyncLabel = buildMetricCard(metrics, "Pending Sync", "0", RetailThemeManager.DANGER);
        lowStockLabel    = buildMetricCard(metrics, "Low Stock", "0", RetailThemeManager.DANGER);
        add(metrics, BorderLayout.NORTH);

        // Middle: recent sales + top products
        JSplitPane middle = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        middle.setResizeWeight(0.5);
        middle.setDividerSize(8);
        middle.setOpaque(false);

        // Recent sales
        String[] saleCols = {"Receipt#", "Time", "Total", "Payment", "Cashier"};
        recentSalesModel = new DefaultTableModel(saleCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable recentTable = RetailThemeManager.styledTable(recentSalesModel);
        JPanel recentPanel = createSection("Recent Sales (Last 10)", RetailThemeManager.scroll(recentTable));
        middle.setLeftComponent(recentPanel);

        // Top products
        String[] prodCols = {"Product", "Qty Sold", "Revenue"};
        topProductsModel = new DefaultTableModel(prodCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable topTable = RetailThemeManager.styledTable(topProductsModel);
        JPanel topPanel = createSection("Top Products Today", RetailThemeManager.scroll(topTable));
        middle.setRightComponent(topPanel);
        add(middle, BorderLayout.CENTER);

        // Bottom: trend chart
        trendChart = new TrendChart();
        JPanel chartPanel = createSection("Sales Trend (Last 7 Days)", trendChart);
        chartPanel.setPreferredSize(new Dimension(0, 180));
        add(chartPanel, BorderLayout.SOUTH);
    }

    private JLabel buildMetricCard(JPanel parent, String title, String defaultVal, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RetailThemeManager.BORDER, 1, true),
            new EmptyBorder(14, 14, 14, 14)));
        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tl.setForeground(RetailThemeManager.TEXT_MUTED);
        JLabel vl = new JLabel(defaultVal);
        vl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        vl.setForeground(color);
        card.add(tl, BorderLayout.NORTH);
        card.add(vl, BorderLayout.CENTER);
        parent.add(card);
        return vl;
    }

    private JPanel createSection(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RetailThemeManager.BORDER, 1, true),
            new EmptyBorder(12, 12, 12, 12)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(RetailThemeManager.TEXT);
        p.add(t, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private void startRefreshTimer() {
        refreshTimer = new Timer(60000, e -> loadData());
        refreshTimer.start();
    }

    private void loadData() {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override protected Map<String, Object> doInBackground() throws Exception {
                return SaleService.getInstance().getDashboardMetrics();
            }
            @Override protected void done() {
                try { updateUI(get()); } catch (Exception e) {
                    System.err.println("[Dashboard] Load failed: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    @SuppressWarnings("unchecked")
    private void updateUI(Map<String, Object> m) {
        salesCountLabel.setText(String.valueOf(m.getOrDefault("sales_count_today", 0)));
        revenueLabel.setText(String.format("KES %.2f", ((Number) m.getOrDefault("revenue_today", 0.0)).doubleValue()));
        profitLabel.setText(String.format("KES %.2f", ((Number) m.getOrDefault("profit_today", 0.0)).doubleValue()));
        stockValueLabel.setText(String.format("KES %.2f", ((Number) m.getOrDefault("stock_value", 0.0)).doubleValue()));
        pendingSyncLabel.setText(String.valueOf(m.getOrDefault("pending_sync", 0)));
        lowStockLabel.setText(String.valueOf(m.getOrDefault("low_stock_count", 0)));

        recentSalesModel.setRowCount(0);
        Object recent = m.get("recent_sales");
        if (recent instanceof List) {
            for (Object o : (List<?>) recent) {
                if (o instanceof Sale) {
                    Sale s = (Sale) o;
                    recentSalesModel.addRow(new Object[]{
                        s.getReceiptNumber(),
                        s.getCreatedAt() != null ? FMT.format(s.getCreatedAt()) : "",
                        String.format("KES %.2f", s.getGrandTotal()),
                        s.getPaymentMethod(),
                        s.getCashierName()
                    });
                }
            }
        }

        topProductsModel.setRowCount(0);
        Object top = m.get("top_products_today");
        if (top instanceof List) {
            for (Object o : (List<?>) top) {
                if (o instanceof Map) {
                    Map<String,Object> row = (Map<String,Object>) o;
                    topProductsModel.addRow(new Object[]{
                        row.get("product_name"),
                        row.get("total_qty"),
                        String.format("KES %.2f", ((Number) row.getOrDefault("total_rev", 0.0)).doubleValue())
                    });
                }
            }
        }

        // Trend chart
        Object trend = m.get("sales_last_7_days");
        if (trend instanceof List) {
            List<Map<String,Object>> trendData = (List<Map<String,Object>>) trend;
            trendChart.setData(trendData);
        }
    }

    @Override
    public void onSaleCompleted(Sale sale) {
        // Force refresh within 5 seconds
        javax.swing.Timer t = new javax.swing.Timer(1000, e -> loadData());
        t.setRepeats(false); t.start();
    }

    // Simple bar chart for 7-day trend
    static class TrendChart extends JPanel {
        private java.util.List<Map<String, Object>> data;

        TrendChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(0, 120));
        }

        void setData(java.util.List<Map<String, Object>> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g2d) {
            super.paintComponent(g2d);
            if (data == null || data.isEmpty()) return;
            Graphics2D g = (Graphics2D) g2d.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int pad = 30, barW = (w - 2 * pad) / data.size();
            double maxVal = data.stream().mapToDouble(d -> ((Number) d.getOrDefault("revenue", 0.0)).doubleValue()).max().orElse(1.0);
            if (maxVal == 0) maxVal = 1;
            int maxBarH = h - pad - 20;
            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> d = data.get(i);
                double rev = ((Number) d.getOrDefault("revenue", 0.0)).doubleValue();
                int barH = (int) (rev / maxVal * maxBarH);
                int x = pad + i * barW + barW / 4;
                int bw = barW / 2;
                int y = h - pad - barH;
                g.setColor(RetailThemeManager.PRIMARY);
                g.fillRoundRect(x, y, bw, barH, 4, 4);
                // Date label
                String date = (String) d.getOrDefault("date", "");
                g.setColor(RetailThemeManager.TEXT_MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g.drawString(date.length() >= 10 ? date.substring(5) : date, x - 2, h - 8);
            }
            g.dispose();
        }
    }
}
