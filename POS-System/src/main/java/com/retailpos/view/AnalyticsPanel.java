package com.retailpos.view;

import com.retailpos.model.Expense;
import com.retailpos.repository.ExpenseRepository;
import com.retailpos.service.AuthService;
import com.retailpos.service.ReportService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Advanced analytics panel — Income vs Expenses, P&L Statement, Market Basket.
 */
public class AnalyticsPanel extends JPanel implements com.retailpos.ui.Refreshable {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yy");
    private static final String[] EXPENSE_CATS = {
        "RENT", "UTILITIES", "SALARIES", "SUPPLIES", "MAINTENANCE",
        "TRANSPORT", "MARKETING", "INSURANCE", "OTHER"
    };

    // date range shared across all sub-panels
    private JSpinner fromSpinner, toSpinner;

    // sub-chart panels (repainted on load)
    private IncomeExpenseChart incomeChart;
    private PLStatementPanel   plPanel;
    private MarketBasketPanel  basketPanel;
    private DonutChart         categoryDonut;
    private SparkLineChart     sparkLine;

    private JLabel statusLabel;
    private final ReportService   reportSvc   = ReportService.getInstance();
    private final ExpenseRepository expRepo   = new ExpenseRepository();


    public AnalyticsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(RetailThemeManager.SURFACE);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        add(buildToolbar(),    BorderLayout.NORTH);
        add(buildContent(),    BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bar.setBackground(RetailThemeManager.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, RetailThemeManager.BORDER),
            new EmptyBorder(4, 12, 4, 12)));

        fromSpinner = dateSpinner(LocalDate.now().withDayOfMonth(1).minusMonths(5));
        toSpinner   = dateSpinner(LocalDate.now());

        JButton loadBtn = RetailThemeManager.primaryButton("Analyse", "reports");
        loadBtn.addActionListener(e -> loadAll());

        JButton expenseBtn = RetailThemeManager.secondaryButton("Add Expense", "add");
        expenseBtn.addActionListener(e -> showExpenseDialog(null));

        JButton manageBtn = RetailThemeManager.secondaryButton("Manage Expenses", "edit");
        manageBtn.addActionListener(e -> showManageExpensesDialog());

        bar.add(new JLabel(Icons.get("reports", 18)));
        bar.add(boldLabel("Advanced Analytics"));
        bar.add(Box.createHorizontalStrut(20));
        bar.add(new JLabel("From:")); bar.add(fromSpinner);
        bar.add(new JLabel("To:"));   bar.add(toSpinner);
        bar.add(loadBtn);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(expenseBtn);
        bar.add(manageBtn);
        return bar;
    }


    // ── Content layout ────────────────────────────────────────────────────────

    private JPanel buildContent() {
        incomeChart   = new IncomeExpenseChart();
        plPanel       = new PLStatementPanel();
        basketPanel   = new MarketBasketPanel();
        categoryDonut = new DonutChart();
        sparkLine     = new SparkLineChart();

        // ── Top row: bar chart (fills width) + donut sidebar ─────────────────
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        JPanel incomeCard = titled("Income vs Expenses — Monthly", incomeChart);
        incomeChart.setPreferredSize(new Dimension(0, 260));

        JPanel donutCard = titled("Revenue by Category", categoryDonut);
        donutCard.setPreferredSize(new Dimension(270, 0));

        topRow.add(incomeCard, BorderLayout.CENTER);
        topRow.add(donutCard,  BorderLayout.EAST);

        // ── Bottom tabs: P&L | Market Basket ─────────────────────────────────
        JTabbedPane bottomTabs = new JTabbedPane();
        bottomTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // P&L tab — table fills the whole pane
        JScrollPane plScroll = RetailThemeManager.scroll(plPanel.table);
        plScroll.setBorder(null);
        JPanel plTab = new JPanel(new BorderLayout(0, 6));
        plTab.setBackground(RetailThemeManager.SURFACE);
        plTab.setBorder(new EmptyBorder(6, 8, 6, 8));
        JLabel plHint = new JLabel("Waterfall P&L for the selected period. Green = positive, Red = negative.");
        plHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        plHint.setForeground(RetailThemeManager.TEXT_MUTED);
        plTab.add(plHint,    BorderLayout.NORTH);
        plTab.add(plScroll,  BorderLayout.CENTER);
        bottomTabs.addTab("Profit & Loss Statement", Icons.get("reports", 14), plTab);

        // Market Basket tab — heatmap on top, table below in split pane
        JScrollPane basketTableScroll = RetailThemeManager.scroll(basketPanel.table);
        basketTableScroll.setBorder(null);
        JSplitPane basketSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            basketPanel.heatmap, basketTableScroll);
        basketSplit.setResizeWeight(0.40);
        basketSplit.setDividerSize(6);
        basketSplit.setOpaque(false);
        JPanel basketTab = new JPanel(new BorderLayout(0, 4));
        basketTab.setBackground(RetailThemeManager.SURFACE);
        basketTab.setBorder(new EmptyBorder(6, 8, 6, 8));
        JLabel basketHint = new JLabel(
            "Heatmap: darker cell = more co-purchases. Table: Lift > 3 (green) = strong association.");
        basketHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        basketHint.setForeground(RetailThemeManager.TEXT_MUTED);
        basketTab.add(basketHint,  BorderLayout.NORTH);
        basketTab.add(basketSplit, BorderLayout.CENTER);
        bottomTabs.addTab("Market Basket — Products Bought Together",
            Icons.get("products", 14), basketTab);

        // ── Spark line strip ─────────────────────────────────────────────────
        JPanel sparkCard = titled("Daily Revenue & Profit Trend", sparkLine);
        sparkLine.setPreferredSize(new Dimension(0, 120));
        sparkCard.setPreferredSize(new Dimension(0, 155));

        // ── Outer vertical split: top row vs bottom tabs ─────────────────────
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topRow, bottomTabs);
        mainSplit.setResizeWeight(0.45);
        mainSplit.setDividerSize(7);
        mainSplit.setOpaque(false);

        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(10, 12, 6, 12));
        outer.add(mainSplit,  BorderLayout.CENTER);
        outer.add(sparkCard,  BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(RetailThemeManager.SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));
        statusLabel = new JLabel("Select a date range and click Analyse.");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        bar.add(statusLabel);
        return bar;
    }

    private JPanel titled(String title, JComponent inner) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(RetailThemeManager.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RetailThemeManager.BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(RetailThemeManager.TEXT);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        card.add(lbl,   BorderLayout.NORTH);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }


    // ── Data loading ──────────────────────────────────────────────────────────

    @Override public void refreshData() { /* analytics are on-demand */ }
    @Override public int getRefreshIntervalSeconds() { return 0; }
    @Override public String getPanelDescription() { return "Analytics — P&L, expenses, market basket"; }

    private void loadAll() {
        LocalDate from = spinnerDate(fromSpinner);
        LocalDate to   = spinnerDate(toSpinner);
        if (from.isAfter(to)) {
            JOptionPane.showMessageDialog(this, "From date must be before To date.");
            return;
        }
        statusLabel.setText("Loading analytics…");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() throws Exception {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("monthly",  reportSvc.getMonthlyIncomeVsExpenses(from, to));
                data.put("pl",       reportSvc.getProfitLossStatement(from, to));
                data.put("basket",   reportSvc.getMarketBasketAnalysis(from, to, 40));
                data.put("breakdown",reportSvc.getRevenueBreakdown(from, to, 8));
                data.put("daily",    reportSvc.getDailyTrend(from, to));
                return data;
            }
            @SuppressWarnings("unchecked")
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Map<String, Object> data = get();
                    incomeChart.setData((List<Map<String,Object>>) data.get("monthly"));
                    plPanel.setData((Map<String,Object>) data.get("pl"));
                    basketPanel.setData((List<Map<String,Object>>) data.get("basket"));
                    Map<String,Object> bd = (Map<String,Object>) data.get("breakdown");
                    categoryDonut.setData((List<Map<String,Object>>) bd.get("categories"));
                    sparkLine.setData((List<Map<String,Object>>) data.get("daily"));
                    Map<String,Object> pl = (Map<String,Object>) data.get("pl");
                    double net = ((Number) pl.getOrDefault("net_profit", 0)).doubleValue();
                    double rev = ((Number) pl.getOrDefault("net_revenue", 0)).doubleValue();
                    statusLabel.setText(String.format(
                        "Period: %s → %s  |  Revenue: KES %,.0f  |  Net Profit: KES %,.0f",
                        from, to, rev, net));
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(AnalyticsPanel.this,
                        "Analytics load failed: " + ex.getMessage());
                }
            }
        }.execute();
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  CHART 1 — Grouped bar chart: Income vs COGS vs Expenses + net profit line
    // ═════════════════════════════════════════════════════════════════════════

    static class IncomeExpenseChart extends JPanel {
        private List<Map<String, Object>> data;
        private String tooltip = null;
        private int tooltipX, tooltipY;

        IncomeExpenseChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 240));
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    tooltip = null;
                    if (data == null || data.isEmpty()) return;
                    int pad = 48, barGroupW = (getWidth() - pad * 2) / data.size();
                    for (int i = 0; i < data.size(); i++) {
                        int gx = pad + i * barGroupW;
                        if (e.getX() >= gx && e.getX() < gx + barGroupW) {
                            Map<String,Object> r = data.get(i);
                            tooltip = String.format("<html><b>%s</b><br>Income: KES %,.0f<br>"
                                + "COGS: KES %,.0f<br>Expenses: KES %,.0f<br>"
                                + "Net: KES %,.0f<br>Margin: %.1f%%</html>",
                                r.get("month"),
                                ((Number)r.getOrDefault("income",0)).doubleValue(),
                                ((Number)r.getOrDefault("cogs",0)).doubleValue(),
                                ((Number)r.getOrDefault("expenses",0)).doubleValue(),
                                ((Number)r.getOrDefault("net_profit",0)).doubleValue(),
                                ((Number)r.getOrDefault("net_margin",0)).doubleValue());
                            tooltipX = e.getX(); tooltipY = e.getY();
                            break;
                        }
                    }
                    repaint();
                }
            });
        }

        void setData(List<Map<String, Object>> d) { this.data = d; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int padL = 64, padR = 20, padT = 16, padB = 40;
            int chartW = w - padL - padR, chartH = h - padT - padB;
            if (data == null || data.isEmpty()) {
                drawEmpty(g, w, h, "No data — click Analyse");
                g.dispose(); return;
            }

            // find max value
            double maxVal = data.stream().mapToDouble(r ->
                Math.max(((Number)r.getOrDefault("income",0)).doubleValue(),
                         ((Number)r.getOrDefault("cogs",0)).doubleValue() +
                         ((Number)r.getOrDefault("expenses",0)).doubleValue()))
                .max().orElse(1.0);
            if (maxVal == 0) maxVal = 1;

            // grid lines
            g.setColor(RetailThemeManager.BORDER);
            g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1, new float[]{4, 4}, 0));
            int gridLines = 5;
            for (int i = 0; i <= gridLines; i++) {
                int y = padT + chartH - (int)(chartH * i / (double)gridLines);
                g.drawLine(padL, y, padL + chartW, y);
                g.setColor(RetailThemeManager.TEXT_MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                String lbl = formatK(maxVal * i / gridLines);
                g.drawString(lbl, padL - g.getFontMetrics().stringWidth(lbl) - 4, y + 4);
                g.setColor(RetailThemeManager.BORDER);
            }
            g.setStroke(new BasicStroke(1));

            int n = data.size();
            double groupW = chartW / (double) n;
            double barW = groupW * 0.22;
            Color cIncome   = RetailThemeManager.ACCENT;
            Color cCogs     = RetailThemeManager.WARNING;
            Color cExpenses = RetailThemeManager.DANGER;

            // profit line points
            int[] lineX = new int[n], lineY = new int[n];

            for (int i = 0; i < n; i++) {
                Map<String,Object> row = data.get(i);
                double income   = ((Number)row.getOrDefault("income",0)).doubleValue();
                double cogs     = ((Number)row.getOrDefault("cogs",0)).doubleValue();
                double expenses = ((Number)row.getOrDefault("expenses",0)).doubleValue();
                double net      = ((Number)row.getOrDefault("net_profit",0)).doubleValue();

                int gx = padL + (int)(i * groupW);
                int cx = gx + (int)(groupW / 2);

                // income bar
                int bh = (int)(income / maxVal * chartH);
                g.setColor(withAlpha(cIncome, 200));
                fillRoundBar(g, (int)(cx - barW*1.6), padT + chartH - bh, (int)barW, bh, 3);

                // COGS bar
                int bh2 = (int)(cogs / maxVal * chartH);
                g.setColor(withAlpha(cCogs, 200));
                fillRoundBar(g, (int)(cx - barW*0.4), padT + chartH - bh2, (int)barW, bh2, 3);

                // Expenses bar
                int bh3 = (int)(expenses / maxVal * chartH);
                g.setColor(withAlpha(cExpenses, 180));
                fillRoundBar(g, (int)(cx + barW*0.8), padT + chartH - bh3, (int)barW, bh3, 3);

                // profit line point
                lineX[i] = cx;
                double clampedNet = Math.max(-maxVal, Math.min(maxVal, net));
                lineY[i] = padT + chartH / 2 - (int)(clampedNet / maxVal * (chartH / 2));

                // x label
                g.setColor(RetailThemeManager.TEXT_MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                String month = row.getOrDefault("month","").toString();
                if (month.length() >= 7) {
                    try {
                        month = YearMonth.parse(month)
                            .format(DateTimeFormatter.ofPattern("MMM yy"));
                    } catch (Exception ignored) {}
                }
                int lw = g.getFontMetrics().stringWidth(month);
                g.drawString(month, cx - lw/2, padT + chartH + 14);
            }

            // zero line (mid for profit)
            g.setColor(RetailThemeManager.BORDER);
            g.setStroke(new BasicStroke(1));

            // draw net profit polyline
            g.setColor(RetailThemeManager.PRIMARY);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < n; i++)
                g.drawLine(lineX[i-1], lineY[i-1], lineX[i], lineY[i]);
            g.setStroke(new BasicStroke(1));
            // dots
            for (int i = 0; i < n; i++) {
                g.setColor(RetailThemeManager.CARD_BG);
                g.fillOval(lineX[i]-4, lineY[i]-4, 9, 9);
                g.setColor(RetailThemeManager.PRIMARY);
                g.drawOval(lineX[i]-4, lineY[i]-4, 9, 9);
            }

            // legend
            drawLegend(g, w - padR - 200, padT,
                new Color[]{cIncome, cCogs, cExpenses, RetailThemeManager.PRIMARY},
                new String[]{"Income","COGS","Expenses","Net Profit"});

            // tooltip
            if (tooltip != null) {
                JLabel tip = new JLabel(tooltip);
                tip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                Dimension ps = tip.getPreferredSize();
                int tx = Math.min(tooltipX + 10, w - ps.width - 8);
                int ty = Math.max(tooltipY - ps.height - 10, padT);
                g.setColor(new Color(0,0,0,140));
                g.fillRoundRect(tx-6, ty-4, ps.width+12, ps.height+12, 8, 8);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                for (String line : tooltip.replaceAll("<[^>]+>","").split("\n")) {
                    g.drawString(line.trim(), tx, ty += 14);
                }
            }
            g.dispose();
        }
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  CHART 2 — Donut chart (revenue by category)
    // ═════════════════════════════════════════════════════════════════════════

    static class DonutChart extends JPanel {
        private List<Map<String, Object>> data;
        private static final Color[] PALETTE = {
            new Color(37,99,235), new Color(16,185,129), new Color(245,158,11),
            new Color(239,68,68), new Color(139,92,246), new Color(20,184,166),
            new Color(249,115,22), new Color(100,116,139)
        };

        DonutChart() { setOpaque(false); }
        void setData(List<Map<String, Object>> d) { this.data = d; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (data == null || data.isEmpty()) { drawEmpty(g,w,h,"No data"); g.dispose(); return; }

            double total = data.stream()
                .mapToDouble(r -> ((Number)r.getOrDefault("revenue",0)).doubleValue()).sum();
            if (total <= 0) { drawEmpty(g,w,h,"No sales data"); g.dispose(); return; }

            int diameter = Math.min(w - 10, h - 80);
            int x = (w - diameter) / 2, y = 10;
            int inner = (int)(diameter * 0.45);
            int ix = x + (diameter - inner) / 2, iy = y + (diameter - inner) / 2;

            double start = -90;
            for (int i = 0; i < Math.min(data.size(), 8); i++) {
                double rev = ((Number)data.get(i).getOrDefault("revenue",0)).doubleValue();
                double arc = rev / total * 360;
                g.setColor(PALETTE[i % PALETTE.length]);
                g.fill(new Arc2D.Double(x, y, diameter, diameter, start, arc, Arc2D.PIE));
                start += arc;
            }
            // cut inner circle (donut hole)
            g.setColor(RetailThemeManager.CARD_BG);
            g.fillOval(ix, iy, inner, inner);

            // centre label
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.setColor(RetailThemeManager.TEXT_MUTED);
            String centreLabel = "Revenue";
            int clw = g.getFontMetrics().stringWidth(centreLabel);
            g.drawString(centreLabel, w/2 - clw/2, iy + inner/2 - 4);
            g.setColor(RetailThemeManager.TEXT);
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            String centreVal = formatK(total);
            int cvw = g.getFontMetrics().stringWidth(centreVal);
            g.drawString(centreVal, w/2 - cvw/2, iy + inner/2 + 14);

            // legend below
            int ly = y + diameter + 12;
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int col = 0, lx = 8;
            for (int i = 0; i < Math.min(data.size(), 8); i++) {
                String cat = data.get(i).getOrDefault("category","?").toString();
                double pct = ((Number)data.get(i).getOrDefault("revenue",0)).doubleValue() / total * 100;
                if (cat.length() > 14) cat = cat.substring(0,13)+"…";
                g.setColor(PALETTE[i % PALETTE.length]);
                g.fillRoundRect(lx, ly+1, 9, 9, 3, 3);
                g.setColor(RetailThemeManager.TEXT_MUTED);
                g.drawString(String.format("%s %.0f%%", cat, pct), lx+12, ly+10);
                ly += 15;
                if (++col == 4) { col = 0; lx += w/2; ly = y + diameter + 12; }
            }
            g.dispose();
        }
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  CHART 3 — P&L Statement (waterfall-style table with colour coding)
    // ═════════════════════════════════════════════════════════════════════════

    static class PLStatementPanel extends JPanel {
        final DefaultTableModel model;
        final JTable table;

        PLStatementPanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
            String[] cols = {"Line Item", "KES Amount", "% of Revenue"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = RetailThemeManager.styledTable(model);
            table.setRowHeight(26);
            table.getColumnModel().getColumn(0).setPreferredWidth(210);
            table.getColumnModel().getColumn(1).setPreferredWidth(110);
            table.getColumnModel().getColumn(2).setPreferredWidth(90);
            table.setDefaultRenderer(Object.class, new PLCellRenderer());
            add(RetailThemeManager.scroll(table), BorderLayout.CENTER);
        }

        @SuppressWarnings("unchecked")
        void setData(Map<String, Object> pl) {
            model.setRowCount(0);
            if (pl == null) return;
            double rev  = num(pl, "net_revenue");
            double cogs = num(pl, "cost_of_goods_sold");
            double gp   = num(pl, "gross_profit");
            double opex = num(pl, "total_operating_expenses");
            double net  = num(pl, "net_profit");
            double tax  = num(pl, "tax_collected");
            int    txCt = ((Number) pl.getOrDefault("transaction_count", 0)).intValue();

            addHeader("REVENUE");
            addRow("  Gross Revenue",         rev, rev, false);
            addRow("  Discounts Given",       -num(pl,"total_discounts"), rev, false);
            addRow("  Net Revenue",            rev, rev, true);
            addSep();

            addHeader("COST OF GOODS SOLD");
            addRow("  Cost of Goods Sold",   -cogs, rev, false);
            addRow("  Gross Profit",           gp,  rev, true);
            addRow("  Gross Margin",           0,   rev, false,
                String.format("%.1f%%", num(pl,"gross_margin_pct")));
            addSep();

            addHeader("OPERATING EXPENSES");
            Map<String,Double> expCat = (Map<String,Double>) pl.getOrDefault("expenses_by_category", new LinkedHashMap<>());
            for (Map.Entry<String,Double> e : expCat.entrySet())
                addRow("  " + e.getKey(), -e.getValue(), rev, false);
            addRow("  Total Operating Expenses", -opex, rev, false);
            addSep();

            addHeader("NET PROFIT");
            addRow("  Net Profit / (Loss)",   net,  rev, true);
            addRow("  Net Margin",             0,   rev, false,
                String.format("%.1f%%", num(pl,"net_margin_pct")));
            addSep();

            addHeader("OTHER INFO");
            addRow("  Tax Collected",          tax, rev, false);
            addRow("  Transaction Count",      0,   rev, false,
                String.valueOf(txCt));
        }

        private void addHeader(String title) {
            model.addRow(new Object[]{"▸ " + title, "", ""});
        }
        private void addSep() {
            model.addRow(new Object[]{"", "", ""});
        }
        private void addRow(String name, double amount, double rev, boolean bold) {
            String pct = rev != 0 && amount != 0
                ? String.format("%.1f%%", amount / rev * 100) : "";
            model.addRow(new Object[]{name,
                amount != 0 ? String.format("KES %,.0f", amount) : "",
                pct});
        }
        private void addRow(String name, double amount, double rev, boolean bold, String override) {
            model.addRow(new Object[]{name, override, ""});
        }
        private double num(Map<String,Object> m, String k) {
            Object v = m.get(k);
            return v instanceof Number ? ((Number)v).doubleValue() : 0.0;
        }

        // cell renderer: header rows = bold surface bg; totals = accent/danger; negative = red
        static class PLCellRenderer extends DefaultTableCellRenderer {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String text = val != null ? val.toString() : "";
                boolean isHeader  = text.startsWith("▸");
                boolean isTotalRow = row > 0 &&
                    t.getValueAt(row,0).toString().contains("Profit") ||
                    t.getValueAt(row,0).toString().contains("Revenue") && col==0;
                boolean isNeg  = col == 1 && text.startsWith("KES -");
                boolean isPos  = col == 1 && text.startsWith("KES") && !isNeg;
                if (!sel) {
                    if (isHeader) {
                        c.setBackground(RetailThemeManager.SURFACE);
                        c.setForeground(RetailThemeManager.TEXT);
                        ((JLabel)c).setFont(((JLabel)c).getFont().deriveFont(Font.BOLD));
                    } else if (isNeg) {
                        c.setBackground(RetailThemeManager.CARD_BG);
                        c.setForeground(RetailThemeManager.DANGER);
                    } else if (isPos && col==1) {
                        c.setBackground(RetailThemeManager.CARD_BG);
                        c.setForeground(RetailThemeManager.ACCENT);
                    } else {
                        c.setBackground(RetailThemeManager.CARD_BG);
                        c.setForeground(RetailThemeManager.TEXT);
                    }
                }
                return c;
            }
        }
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  CHART 4 — Market Basket heatmap grid + table
    // ═════════════════════════════════════════════════════════════════════════

    static class MarketBasketPanel extends JPanel {
        private List<Map<String, Object>> data;
        final DefaultTableModel tableModel;
        final JTable table;
        final HeatmapGrid heatmap;

        MarketBasketPanel() {
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);

            heatmap = new HeatmapGrid();
            heatmap.setPreferredSize(new Dimension(0, 160));

            String[] cols = {"Product A","Product B","Co-Sales","Support %","Conf A→B %","Lift"};
            tableModel = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = RetailThemeManager.styledTable(tableModel);
            table.setRowHeight(24);
            table.setDefaultRenderer(Object.class, new LiftCellRenderer());
            // NOTE: heatmap and table are laid out by the parent AnalyticsPanel
            // buildContent() method via a JSplitPane — not added here.
        }

        void setData(List<Map<String, Object>> d) {
            this.data = d;
            heatmap.setData(d);
            tableModel.setRowCount(0);
            if (d == null) return;
            for (Map<String, Object> row : d) {
                tableModel.addRow(new Object[]{
                    row.get("product_a"),
                    row.get("product_b"),
                    row.get("co_occurrences"),
                    row.get("support"),
                    row.get("confidence_a_b"),
                    row.get("lift")
                });
            }
        }

        // Colour cells by lift value
        static class LiftCellRenderer extends DefaultTableCellRenderer {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                if (!sel && col == 5) {
                    try {
                        double lift = Double.parseDouble(val.toString());
                        boolean dark = RetailThemeManager.getInstance().isDark();
                        if (lift >= 3.0)
                            c.setBackground(dark ? new Color(20,80,30)  : new Color(187,247,208));
                        else if (lift >= 1.5)
                            c.setBackground(dark ? new Color(50,70,10)  : new Color(254,249,195));
                        else
                            c.setBackground(RetailThemeManager.CARD_BG);
                        c.setForeground(RetailThemeManager.TEXT);
                    } catch (Exception ignored) { c.setBackground(RetailThemeManager.CARD_BG); }
                }
                return c;
            }
        }
    }

    // ── Heatmap grid (top N products × N products, cell = co-occurrence) ─────

    static class HeatmapGrid extends JPanel {
        private List<Map<String, Object>> data;
        private String tooltip;
        private int ttX, ttY;

        HeatmapGrid() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    tooltip = hitTest(e.getX(), e.getY());
                    ttX = e.getX(); ttY = e.getY();
                    repaint();
                }
            });
        }

        void setData(List<Map<String, Object>> d) { this.data = d; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (data == null || data.isEmpty()) {
                drawEmpty(g,w,h,"No co-purchase data yet — needs ≥2 products per transaction");
                g.dispose(); return;
            }

            // Build unique product list from top pairs
            List<String> products = new ArrayList<>();
            for (Map<String,Object> r : data) {
                String a = r.get("product_a").toString(), b = r.get("product_b").toString();
                if (!products.contains(a)) products.add(a);
                if (!products.contains(b)) products.add(b);
                if (products.size() >= 10) break;
            }

            // Build co-occurrence matrix
            Map<String, Integer> matrix = new HashMap<>();
            int maxCount = 1;
            for (Map<String,Object> r : data) {
                String a = r.get("product_a").toString(), b = r.get("product_b").toString();
                int cnt = ((Number)r.getOrDefault("co_occurrences",0)).intValue();
                matrix.put(a+"|"+b, cnt);
                matrix.put(b+"|"+a, cnt);
                maxCount = Math.max(maxCount, cnt);
            }

            int n = products.size();
            int labelW = 80, labelH = 14;
            int cellSize = Math.min((w - labelW) / Math.max(n,1), (h - labelH) / Math.max(n,1));
            cellSize = Math.max(cellSize, 12);
            int offX = labelW, offY = labelH;

            // column labels (rotated)
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g.setColor(RetailThemeManager.TEXT_MUTED);
            for (int j = 0; j < n; j++) {
                String lbl = shorten(products.get(j), 10);
                Graphics2D gt = (Graphics2D) g.create();
                gt.translate(offX + j * cellSize + cellSize/2, offY - 2);
                gt.rotate(-Math.PI/4);
                gt.drawString(lbl, 0, 0);
                gt.dispose();
            }

            // row labels + cells
            for (int i = 0; i < n; i++) {
                String rowLabel = shorten(products.get(i), 12);
                g.setColor(RetailThemeManager.TEXT_MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                int fw = g.getFontMetrics().stringWidth(rowLabel);
                g.drawString(rowLabel, offX - fw - 3, offY + i*cellSize + cellSize/2 + 4);

                for (int j = 0; j < n; j++) {
                    int cx = offX + j*cellSize, cy = offY + i*cellSize;
                    if (i == j) {
                        g.setColor(RetailThemeManager.SURFACE);
                        g.fillRect(cx, cy, cellSize-1, cellSize-1);
                        continue;
                    }
                    int cnt = matrix.getOrDefault(products.get(i)+"|"+products.get(j), 0);
                    float alpha = cnt > 0 ? 0.15f + 0.75f * cnt / (float)maxCount : 0.05f;
                    Color base = RetailThemeManager.PRIMARY;
                    g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                        (int)(alpha * 255)));
                    g.fillRoundRect(cx+1, cy+1, cellSize-2, cellSize-2, 3, 3);
                    if (cnt > 0 && cellSize > 18) {
                        g.setColor(RetailThemeManager.TEXT);
                        g.setFont(new Font("Segoe UI", Font.BOLD, 8));
                        String ct = String.valueOf(cnt);
                        int tw = g.getFontMetrics().stringWidth(ct);
                        g.drawString(ct, cx + (cellSize-tw)/2, cy + cellSize/2 + 4);
                    }
                }
            }

            // tooltip
            if (tooltip != null) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                int tw2 = g.getFontMetrics().stringWidth(tooltip) + 16;
                int tx = Math.min(ttX+10, w-tw2-4), ty = Math.max(ttY-22, 4);
                g.setColor(new Color(0,0,0,150));
                g.fillRoundRect(tx, ty, tw2, 22, 6, 6);
                g.setColor(Color.WHITE);
                g.drawString(tooltip, tx+8, ty+15);
            }
            g.dispose();
        }

        private String hitTest(int mx, int my) {
            if (data == null || data.isEmpty()) return null;
            List<String> products = new ArrayList<>();
            for (Map<String,Object> r : data) {
                String a = r.get("product_a").toString(), b = r.get("product_b").toString();
                if (!products.contains(a)) products.add(a);
                if (!products.contains(b)) products.add(b);
                if (products.size() >= 10) break;
            }
            int n = products.size(); if (n == 0) return null;
            int labelW = 80, labelH = 14;
            int cellSize = Math.min((getWidth()-labelW)/Math.max(n,1),(getHeight()-labelH)/Math.max(n,1));
            cellSize = Math.max(cellSize,12);
            int j = (mx - labelW) / cellSize;
            int i = (my - labelH) / cellSize;
            if (i<0||j<0||i>=n||j>=n||i==j) return null;
            Map<String,Integer> matrix = new HashMap<>();
            for (Map<String,Object> r : data) {
                String a = r.get("product_a").toString(), b = r.get("product_b").toString();
                int cnt = ((Number)r.getOrDefault("co_occurrences",0)).intValue();
                matrix.put(a+"|"+b,cnt); matrix.put(b+"|"+a,cnt);
            }
            int cnt = matrix.getOrDefault(products.get(i)+"|"+products.get(j),0);
            return String.format("%s + %s → %d co-sales",
                shorten(products.get(i),16), shorten(products.get(j),16), cnt);
        }

        private static String shorten(String s, int max) {
            return s.length() > max ? s.substring(0,max-1)+"…" : s;
        }
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  CHART 5 — Dual-area spark line (revenue + profit, daily)
    // ═════════════════════════════════════════════════════════════════════════

    static class SparkLineChart extends JPanel {
        private List<Map<String, Object>> data;

        SparkLineChart() { setOpaque(false); }
        void setData(List<Map<String, Object>> d) { this.data = d; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int padL=44, padR=12, padT=10, padB=28;
            int cw = w-padL-padR, ch = h-padT-padB;
            if (data==null||data.isEmpty()||cw<=0||ch<=0) {
                drawEmpty(g,w,h,"No daily data"); g.dispose(); return;
            }
            double maxRev = data.stream()
                .mapToDouble(r->((Number)r.getOrDefault("revenue",0)).doubleValue()).max().orElse(1);
            if (maxRev==0) maxRev=1;
            int n = data.size();
            double step = cw / (double)Math.max(n-1,1);

            // Build revenue + profit paths
            GeneralPath revPath = new GeneralPath();
            GeneralPath profPath = new GeneralPath();
            GeneralPath revFill = new GeneralPath();
            GeneralPath profFill = new GeneralPath();
            int baseline = padT+ch;
            for (int i=0;i<n;i++) {
                double rev  = ((Number)data.get(i).getOrDefault("revenue",0)).doubleValue();
                double prof = ((Number)data.get(i).getOrDefault("profit",0)).doubleValue();
                int px = padL+(int)(i*step);
                int ry = padT+(int)((1-rev/maxRev)*ch);
                int py = padT+(int)((1-Math.min(Math.max(prof/maxRev,0),1))*ch);
                if(i==0) { revPath.moveTo(px,ry); profPath.moveTo(px,py);
                           revFill.moveTo(px,baseline); revFill.lineTo(px,ry);
                           profFill.moveTo(px,baseline); profFill.lineTo(px,py);
                } else    { revPath.lineTo(px,ry); profPath.lineTo(px,py);
                            revFill.lineTo(px,ry); profFill.lineTo(px,py); }
            }
            revFill.lineTo(padL+(int)((n-1)*step),baseline); revFill.closePath();
            profFill.lineTo(padL+(int)((n-1)*step),baseline); profFill.closePath();

            // fill areas
            Color revColor  = RetailThemeManager.PRIMARY;
            Color profColor = RetailThemeManager.ACCENT;
            g.setColor(new Color(revColor.getRed(),revColor.getGreen(),revColor.getBlue(),30));
            g.fill(revFill);
            g.setColor(new Color(profColor.getRed(),profColor.getGreen(),profColor.getBlue(),40));
            g.fill(profFill);
            // lines
            g.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.setColor(revColor);  g.draw(revPath);
            g.setColor(profColor); g.draw(profPath);
            g.setStroke(new BasicStroke(1));

            // x labels (every N days)
            g.setFont(new Font("Segoe UI",Font.PLAIN,9));
            g.setColor(RetailThemeManager.TEXT_MUTED);
            int skip = Math.max(1, n/10);
            for (int i=0;i<n;i+=skip) {
                String day = data.get(i).getOrDefault("date","").toString();
                if (day.length()>=10) day=day.substring(5);
                int px = padL+(int)(i*step);
                int fw = g.getFontMetrics().stringWidth(day);
                g.drawString(day, px-fw/2, padT+ch+16);
            }
            // y axis max label
            g.drawString(formatK(maxRev), 2, padT+10);

            // legend
            drawLegend(g, w-padR-120, padT,
                new Color[]{revColor, profColor},
                new String[]{"Revenue","Profit"});
            g.dispose();
        }
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  Expense dialogs
    // ═════════════════════════════════════════════════════════════════════════

    private void showExpenseDialog(Expense existing) {
        boolean isNew = existing == null;
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
            isNew ? "Add Expense" : "Edit Expense", true);
        d.setSize(560, 560);
        d.setMinimumSize(new Dimension(480, 480));
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

        // ── Header strip ─────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(RetailThemeManager.CARD_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, RetailThemeManager.BORDER),
            new EmptyBorder(14, 20, 14, 20)));
        JLabel headerTitle = new JLabel(isNew ? "  Add Expense" : "  Edit Expense");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerTitle.setForeground(RetailThemeManager.TEXT);
        headerTitle.setIcon(Icons.get("add", 18));
        header.add(headerTitle, BorderLayout.WEST);
        d.add(header, BorderLayout.NORTH);

        // ── Scrollable form ───────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(RetailThemeManager.CARD_BG);
        form.setBorder(new EmptyBorder(18, 24, 12, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1; gc.gridx = 0;
        gc.insets = new Insets(3, 0, 3, 0);

        // Category
        gc.gridy = 0; form.add(fieldLabel("Category"), gc);
        gc.gridy = 1;
        JComboBox<String> catCombo = new JComboBox<>(EXPENSE_CATS);
        catCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        catCombo.setPreferredSize(new Dimension(480, 40));
        form.add(catCombo, gc);

        // Description
        gc.gridy = 2; gc.insets = new Insets(10, 0, 3, 0);
        form.add(fieldLabel("Description *"), gc);
        gc.gridy = 3; gc.insets = new Insets(3, 0, 3, 0);
        JTextField descF = RetailThemeManager.styledField();
        descF.setPreferredSize(new Dimension(480, 40));
        descF.putClientProperty("JTextField.placeholderText", "e.g. Monthly rent — Shop A");
        form.add(descF, gc);

        // Amount
        gc.gridy = 4; gc.insets = new Insets(10, 0, 3, 0);
        form.add(fieldLabel("Amount (KES) *"), gc);
        gc.gridy = 5; gc.insets = new Insets(3, 0, 3, 0);
        JTextField amtF = RetailThemeManager.styledField();
        amtF.setPreferredSize(new Dimension(480, 40));
        amtF.putClientProperty("JTextField.placeholderText", "0.00");
        form.add(amtF, gc);

        // Date row — field + today button
        gc.gridy = 6; gc.insets = new Insets(10, 0, 3, 0);
        form.add(fieldLabel("Date *"), gc);
        gc.gridy = 7; gc.insets = new Insets(3, 0, 3, 0);
        JPanel dateRow = new JPanel(new BorderLayout(8, 0));
        dateRow.setOpaque(false);
        JTextField dateF = RetailThemeManager.styledField();
        dateF.setText(LocalDate.now().toString());
        dateF.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        JButton todayBtn = RetailThemeManager.secondaryButton("Today");
        todayBtn.setPreferredSize(new Dimension(72, 40));
        todayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        todayBtn.addActionListener(ev -> dateF.setText(LocalDate.now().toString()));
        dateRow.add(dateF, BorderLayout.CENTER);
        dateRow.add(todayBtn, BorderLayout.EAST);
        form.add(dateRow, gc);

        // Reference
        gc.gridy = 8; gc.insets = new Insets(10, 0, 3, 0);
        form.add(fieldLabel("Reference / Receipt No. (optional)"), gc);
        gc.gridy = 9; gc.insets = new Insets(3, 0, 3, 0);
        JTextField refF = RetailThemeManager.styledField();
        refF.setPreferredSize(new Dimension(480, 40));
        refF.putClientProperty("JTextField.placeholderText", "e.g. KCB-TX-00123");
        form.add(refF, gc);

        // Notes
        gc.gridy = 10; gc.insets = new Insets(10, 0, 3, 0);
        form.add(fieldLabel("Notes (optional)"), gc);
        gc.gridy = 11; gc.insets = new Insets(3, 0, 3, 0); gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        JTextArea notesArea = new JTextArea(3, 0);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notesArea.setBackground(RetailThemeManager.getInstance().fieldBg());
        notesArea.setForeground(RetailThemeManager.TEXT);
        notesArea.setCaretColor(RetailThemeManager.TEXT);
        notesArea.setLineWrap(true); notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RetailThemeManager.BORDER, 1, true),
            new EmptyBorder(8, 10, 8, 10)));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(null);
        notesScroll.setPreferredSize(new Dimension(480, 70));
        form.add(notesScroll, gc);
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weighty = 0;

        // Pre-fill for edit
        if (existing != null) {
            catCombo.setSelectedItem(existing.getCategory());
            descF.setText(existing.getDescription() != null ? existing.getDescription() : "");
            amtF.setText(String.valueOf(existing.getAmount()));
            dateF.setText(existing.getDate() != null ? existing.getDate().toString() : "");
            refF.setText(existing.getReference() != null ? existing.getReference() : "");
        }

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        d.add(formScroll, BorderLayout.CENTER);

        // ── Error + footer ────────────────────────────────────────────────────
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(RetailThemeManager.CARD_BG);
        south.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));

        JLabel errLbl = new JLabel(" ");
        errLbl.setForeground(RetailThemeManager.DANGER);
        errLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        errLbl.setBorder(new EmptyBorder(6, 20, 0, 20));
        south.add(errLbl, BorderLayout.NORTH);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setOpaque(false);
        JButton cancel = RetailThemeManager.secondaryButton("Cancel");
        JButton save   = RetailThemeManager.primaryButton(isNew ? "Add Expense" : "Save Changes");
        save.setPreferredSize(new Dimension(160, 44));
        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            String desc = descF.getText().trim();
            String amt  = amtF.getText().trim();
            String dt   = dateF.getText().trim();
            if (desc.isEmpty()) { errLbl.setText("Description is required"); return; }
            double amount;
            try { amount = Double.parseDouble(amt); if (amount < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { errLbl.setText("Enter a valid positive amount"); return; }
            LocalDate date;
            try { date = LocalDate.parse(dt); }
            catch (Exception ex) { errLbl.setText("Date must be YYYY-MM-DD (e.g. 2026-08-16)"); return; }

            Expense exp = isNew ? new Expense() : existing;
            if (isNew) exp.setId(java.util.UUID.randomUUID().toString());
            exp.setCategory((String) catCombo.getSelectedItem());
            exp.setDescription(desc);
            exp.setAmount(amount);
            exp.setDate(date);
            exp.setReference(refF.getText().trim().isEmpty() ? null : refF.getText().trim());
            // store notes in reference field if reference is empty, or append
            String notes = notesArea.getText().trim();
            if (!notes.isEmpty() && exp.getReference() == null) exp.setReference(notes);
            exp.setCreatedBy(AuthService.getInstance().getCurrentUser().getId());

            save.setEnabled(false); save.setText("Saving…");
            new SwingWorker<Void,Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (isNew) expRepo.insert(exp); else expRepo.update(exp);
                    return null;
                }
                @Override protected void done() {
                    save.setEnabled(true); save.setText(isNew ? "Add Expense" : "Save Changes");
                    try { get(); d.dispose(); }
                    catch (Exception ex) { errLbl.setText("Save failed: " + ex.getMessage()); }
                }
            }.execute();
        });
        footer.add(cancel); footer.add(save);
        south.add(footer, BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);

        d.getRootPane().setDefaultButton(save);
        d.setVisible(true);
    }


    private void showManageExpensesDialog() {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
            "Manage Expenses", true);
        d.setSize(760, 480); d.setLocationRelativeTo(this);

        String[] cols = {"ID","Date","Category","Description","Amount (KES)","Reference"};
        DefaultTableModel mdl = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = RetailThemeManager.styledTable(mdl);
        tbl.getColumnModel().getColumn(0).setMinWidth(0);
        tbl.getColumnModel().getColumn(0).setMaxWidth(0);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(0);

        Runnable reload = () -> new SwingWorker<List<com.retailpos.model.Expense>,Void>() {
            @Override protected List<com.retailpos.model.Expense> doInBackground() throws Exception {
                return expRepo.findByDateRange(
                    LocalDate.now().minusYears(2), LocalDate.now().plusDays(1));
            }
            @Override protected void done() {
                try {
                    mdl.setRowCount(0);
                    for (com.retailpos.model.Expense e : get()) {
                        mdl.addRow(new Object[]{
                            e.getId(),
                            e.getDate(),
                            e.getCategory(),
                            e.getDescription(),
                            String.format("%.2f", e.getAmount()),
                            e.getReference() != null ? e.getReference() : ""
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
        reload.run();

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btns.setBackground(RetailThemeManager.SURFACE);
        JButton addBtn  = RetailThemeManager.primaryButton("Add", "add");
        JButton editBtn = RetailThemeManager.secondaryButton("Edit", "edit");
        JButton delBtn  = RetailThemeManager.dangerButton("Delete", "delete");

        addBtn.addActionListener(e -> {
            d.setVisible(false);
            showExpenseDialog(null);
            reload.run();
            d.setVisible(true);
        });
        editBtn.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(d, "Select a row first."); return; }
            String id = mdl.getValueAt(row,0).toString();
            try {
                List<com.retailpos.model.Expense> all =
                    expRepo.findByDateRange(LocalDate.now().minusYears(2), LocalDate.now().plusDays(1));
                all.stream().filter(ex -> ex.getId().equals(id)).findFirst().ifPresent(ex -> {
                    d.setVisible(false);
                    showExpenseDialog(ex);
                    reload.run();
                    d.setVisible(true);
                });
            } catch (Exception ex) { JOptionPane.showMessageDialog(d, "Load failed: " + ex.getMessage()); }
        });
        delBtn.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(d, "Select a row first."); return; }
            String id   = mdl.getValueAt(row,0).toString();
            String desc = mdl.getValueAt(row,3).toString();
            int ok = JOptionPane.showConfirmDialog(d,
                "Delete expense: '" + desc + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void,Void>() {
                @Override protected Void doInBackground() throws Exception {
                    expRepo.delete(id); return null;
                }
                @Override protected void done() {
                    try { get(); reload.run(); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(d, "Delete failed: "+ex.getMessage()); }
                }
            }.execute();
        });
        btns.add(addBtn); btns.add(editBtn); btns.add(delBtn);

        d.setLayout(new BorderLayout());
        d.add(RetailThemeManager.scroll(tbl), BorderLayout.CENTER);
        d.add(btns, BorderLayout.SOUTH);
        d.setVisible(true);
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  Shared static helpers (used by inner chart classes via static calls)
    // ═════════════════════════════════════════════════════════════════════════

    static void drawEmpty(Graphics2D g, int w, int h, String msg) {
        g.setColor(RetailThemeManager.TEXT_MUTED);
        g.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, (w - fm.stringWidth(msg))/2, h/2);
    }

    static void drawLegend(Graphics2D g, int x, int y, Color[] colors, String[] labels) {
        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        int lx = x, ly = y;
        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fillRoundRect(lx, ly+1, 9, 9, 3, 3);
            g.setColor(RetailThemeManager.TEXT_MUTED);
            g.drawString(labels[i], lx+13, ly+10);
            lx += g.getFontMetrics().stringWidth(labels[i]) + 26;
        }
    }

    static void fillRoundBar(Graphics2D g, int x, int y, int w, int h, int arc) {
        if (h <= 0) return;
        g.fillRoundRect(x, y, w, h, arc, arc);
    }

    static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    static String formatK(double v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.1fM", v/1_000_000);
        if (Math.abs(v) >= 1_000)     return String.format("%.0fK", v/1_000);
        return String.format("%.0f", v);
    }

    // ── Instance helpers ──────────────────────────────────────────────────────

    private JSpinner dateSpinner(LocalDate d) {
        SpinnerDateModel m = new SpinnerDateModel();
        Calendar cal = Calendar.getInstance();
        cal.set(d.getYear(), d.getMonthValue()-1, d.getDayOfMonth());
        m.setValue(cal.getTime());
        JSpinner sp = new JSpinner(m);
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        sp.setPreferredSize(new Dimension(110, 34));
        return sp;
    }

    private LocalDate spinnerDate(JSpinner sp) {
        java.util.Date dt = (java.util.Date) sp.getValue();
        return dt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private JLabel boldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(RetailThemeManager.TEXT);
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(RetailThemeManager.TEXT_MUTED);
        return l;
    }
}
