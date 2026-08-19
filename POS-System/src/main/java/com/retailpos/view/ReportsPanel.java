package com.retailpos.view;

import com.retailpos.service.ReportService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import com.retailpos.model.AppSettings;
import com.retailpos.repository.SettingsRepository;
import java.util.*;
import java.util.List;

public class ReportsPanel extends JPanel implements com.retailpos.ui.Refreshable {
    private static final String[] REPORT_TYPES = {
        "Daily Sales", "Period Sales", "Profit Report", "Tax Report",
        "Best Selling Products", "Low Stock Report", "Inventory Valuation",
        "Sales by Category", "Sales by Payment Method", "Cashier Performance"
    };
    private JComboBox<String> reportTypeCombo;
    private JSpinner fromDateSpinner, toDateSpinner;
    private DefaultTableModel resultModel;
    private JTable resultTable;
    private JLabel statusLabel;
    private final ReportService reportService = ReportService.getInstance();

    public ReportsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        buildUI();
    }

    private void buildUI() {
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RetailThemeManager.BORDER, 1),
            new EmptyBorder(8, 12, 8, 12)));

        reportTypeCombo = new JComboBox<>(REPORT_TYPES);
        reportTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reportTypeCombo.setPreferredSize(new Dimension(220, 36));

        // Date spinners
        fromDateSpinner = createDateSpinner(LocalDate.now().withDayOfMonth(1));
        toDateSpinner   = createDateSpinner(LocalDate.now());

        JButton generateBtn = RetailThemeManager.primaryButton("Generate", "reports");
        JButton exportPdf   = RetailThemeManager.secondaryButton("Export PDF", "save");
        JButton exportExcel = RetailThemeManager.secondaryButton("Export Excel", "save");
        JButton printBtn    = RetailThemeManager.secondaryButton("Print", "print");

        generateBtn.addActionListener(e -> generateReport());
        exportPdf.addActionListener(e -> exportPdf());
        exportExcel.addActionListener(e -> exportExcel());
        printBtn.addActionListener(e -> printReport());

        toolbar.add(new JLabel("Report:")); toolbar.add(reportTypeCombo);
        toolbar.add(new JLabel("From:")); toolbar.add(fromDateSpinner);
        toolbar.add(new JLabel("To:")); toolbar.add(toDateSpinner);
        toolbar.add(generateBtn);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(exportPdf); toolbar.add(exportExcel); toolbar.add(printBtn);
        add(toolbar, BorderLayout.NORTH);

        // Results table
        resultModel = new DefaultTableModel(new String[]{"Report not yet generated"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultTable = RetailThemeManager.styledTable(resultModel);
        add(RetailThemeManager.scroll(resultTable), BorderLayout.CENTER);

        // Status bar
        statusLabel = RetailThemeManager.subLabel("Select a report type and click Generate");
        add(statusLabel, BorderLayout.SOUTH);
    }

    @Override public void refreshData() { /* reports are on-demand — no auto-refresh */ }
    @Override public int getRefreshIntervalSeconds() { return 0; }
    @Override public String getPanelDescription() { return "Reports — on-demand"; }

    private JSpinner createDateSpinner(LocalDate defaultDate) {
        SpinnerDateModel model = new SpinnerDateModel();
        Calendar cal = Calendar.getInstance();
        cal.set(defaultDate.getYear(), defaultDate.getMonthValue() - 1, defaultDate.getDayOfMonth());
        model.setValue(cal.getTime());
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(120, 36));
        return spinner;
    }

    private LocalDate getDate(JSpinner spinner) {
        java.util.Date d = (java.util.Date) spinner.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private void generateReport() {
        String type = (String) reportTypeCombo.getSelectedItem();
        LocalDate from = getDate(fromDateSpinner);
        LocalDate to   = getDate(toDateSpinner);
        if (from.isAfter(to)) {
            JOptionPane.showMessageDialog(this, "From date must be before To date");
            return;
        }
        statusLabel.setText("Generating report...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            Map<String, Object> report = null;
            List<Map<String, Object>> listReport = null;

            @Override protected Void doInBackground() throws Exception {
                switch (type) {
                    case "Daily Sales"           -> report = reportService.generateDailySalesReport(from);
                    case "Period Sales"          -> report = reportService.generatePeriodReport(from, to);
                    case "Profit Report"         -> report = reportService.getProfitReport(from, to);
                    case "Tax Report"            -> report = reportService.getTaxReport(from, to);
                    case "Best Selling Products" -> listReport = reportService.getBestSellingProducts(from, to, 50);
                    case "Low Stock Report"      -> listReport = reportService.getLowStockReport();
                    case "Inventory Valuation"   -> report = reportService.getInventoryValuationReport();
                    case "Sales by Category"     -> report = reportService.getSalesByCategoryReport(from, to);
                    case "Sales by Payment Method" -> report = reportService.getSalesByPaymentMethod(from, to);
                    case "Cashier Performance"   -> report = reportService.getCashierPerformanceReport(from, to);
                }
                return null;
            }

            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    if (listReport != null && !listReport.isEmpty()) {
                        renderListReport(listReport);
                    } else if (report != null) {
                        renderMapReport(report);
                    } else {
                        statusLabel.setText("No data available for the selected period");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    JOptionPane.showMessageDialog(ReportsPanel.this, "Report generation failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void renderListReport(List<Map<String, Object>> data) {
        if (data.isEmpty()) { statusLabel.setText("No records found"); return; }
        String[] cols = data.get(0).keySet().toArray(new String[0]);
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Map<String, Object> row : data) {
            m.addRow(row.values().stream().map(v -> v != null ? v.toString() : "").toArray());
        }
        resultTable.setModel(m);
        statusLabel.setText("Showing " + data.size() + " rows");
    }

    private void renderMapReport(Map<String, Object> report) {
        for (Object value : report.values()) {
            if (value instanceof List<?> nested && !nested.isEmpty() && nested.get(0) instanceof Map) {
                @SuppressWarnings("unchecked") List<Map<String, Object>> rows = (List<Map<String, Object>>) nested;
                renderListReport(rows);
                statusLabel.setText("Report generated with " + rows.size() + " detail rows");
                return;
            }
        }
        DefaultTableModel m = new DefaultTableModel(new String[]{"Metric", "Value"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Map.Entry<String, Object> entry : report.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof List) continue; // skip nested lists
            String formatted = val instanceof Number ?
                String.format("%.2f", ((Number) val).doubleValue()) : String.valueOf(val);
            m.addRow(new Object[]{formatKey(entry.getKey()), formatted});
        }
        resultTable.setModel(m);
        // Also add nested lists
        for (Map.Entry<String, Object> entry : report.entrySet()) {
            if (entry.getValue() instanceof List) {
                List<?> list = (List<?>) entry.getValue();
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    statusLabel.setText("Report generated — scroll right for details");
                    break;
                }
            }
        }
        statusLabel.setText("Report generated for period: " +
            report.getOrDefault("from", "") + " to " + report.getOrDefault("to", ""));
    }

    private String formatKey(String key) {
        return key.replace("_", " ").substring(0, 1).toUpperCase() + key.replace("_", " ").substring(1);
    }

    private void exportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PDF Report");
        chooser.setSelectedFile(new java.io.File("report.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            AppSettings settings = new SettingsRepository().load();
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4.rotate(), 28, 28, 28, 28);
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(chooser.getSelectedFile()));
            doc.open();
            javax.swing.table.TableModel m = resultTable.getModel();
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font subTitleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Paragraph store = new com.itextpdf.text.Paragraph(settings.getStoreName(), titleFont);
            store.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER); doc.add(store);
            com.itextpdf.text.Paragraph reportTitle = new com.itextpdf.text.Paragraph(reportTypeCombo.getSelectedItem() + " Summary", subTitleFont);
            reportTitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER); doc.add(reportTitle);
            com.itextpdf.text.Paragraph criteria = new com.itextpdf.text.Paragraph("From: " + getDate(fromDateSpinner) + "    To: " + getDate(toDateSpinner) + "    Currency: KES");
            criteria.setSpacingAfter(10); doc.add(criteria);
            com.itextpdf.text.pdf.PdfPTable pdfTable = new com.itextpdf.text.pdf.PdfPTable(m.getColumnCount());
            pdfTable.setWidthPercentage(100);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
            com.itextpdf.text.Font bodyFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8);
            for (int c = 0; c < m.getColumnCount(); c++) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(m.getColumnName(c), headerFont));
                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(30, 64, 105)); cell.setPadding(5); pdfTable.addCell(cell);
            }
            double[] totals = new double[m.getColumnCount()];
            for (int r = 0; r < m.getRowCount(); r++) {
                for (int c = 0; c < m.getColumnCount(); c++) {
                    Object value = m.getValueAt(r, c); String text = value == null ? "" : value.toString();
                    try { totals[c] += Double.parseDouble(text.replace(",", "")); } catch (NumberFormatException ignored) { }
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(text, bodyFont));
                    cell.setPadding(4); if (c > 1) cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT); pdfTable.addCell(cell);
                }
            }
            doc.add(pdfTable);
            doc.add(new com.itextpdf.text.Paragraph("Generated " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))));
            doc.close();
            JOptionPane.showMessageDialog(this, "PDF exported: " + chooser.getSelectedFile().getName());
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "PDF export failed: " + e.getMessage()); }
    }

    private void exportExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Excel Report");
        chooser.setSelectedFile(new java.io.File("report.xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Report");
            javax.swing.table.TableModel m = resultTable.getModel();
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int c = 0; c < m.getColumnCount(); c++) header.createCell(c).setCellValue(m.getColumnName(c));
            for (int r = 0; r < m.getRowCount(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
                for (int c = 0; c < m.getColumnCount(); c++) {
                    Object val = m.getValueAt(r, c);
                    row.createCell(c).setCellValue(val != null ? val.toString() : "");
                }
            }
            java.io.FileOutputStream fos = new java.io.FileOutputStream(chooser.getSelectedFile());
            wb.write(fos); fos.close(); wb.close();
            JOptionPane.showMessageDialog(this, "Excel exported: " + chooser.getSelectedFile().getName());
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Excel export failed: " + e.getMessage()); }
    }

    private void printReport() {
        try {
            resultTable.print(JTable.PrintMode.FIT_WIDTH, null, null);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Print failed: " + e.getMessage()); }
    }
}
