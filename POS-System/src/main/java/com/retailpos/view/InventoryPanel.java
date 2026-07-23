package com.retailpos.view;

import com.retailpos.model.*;
import com.retailpos.repository.InventoryRepository;
import com.retailpos.service.AuthService;
import com.retailpos.service.InventoryService;
import com.retailpos.service.ProductService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InventoryPanel extends JPanel {
    private DefaultTableModel movementsModel;
    private DefaultTableModel lowStockModel;
    private final InventoryService inventoryService = InventoryService.getInstance();
    private final ProductService productService = ProductService.getInstance();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public InventoryPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton adjBtn  = RetailThemeManager.primaryButton("Adjust Stock", "inventory");
        JButton damBtn  = RetailThemeManager.dangerButton("Record Damaged", "delete");
        JButton expBtn  = RetailThemeManager.secondaryButton("Record Expired");
        JButton refreshBtn = RetailThemeManager.secondaryButton("Refresh", "refresh");
        adjBtn.addActionListener(e -> showAdjustDialog());
        damBtn.addActionListener(e -> showDamagedDialog());
        expBtn.addActionListener(e -> showExpiredDialog());
        refreshBtn.addActionListener(e -> loadData());
        toolbar.add(adjBtn); toolbar.add(damBtn); toolbar.add(expBtn); toolbar.add(refreshBtn);
        add(toolbar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Recent movements
        String[] movCols = {"Date", "Product", "Type", "Quantity", "Reason", "Batch#", "User"};
        movementsModel = new DefaultTableModel(movCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable movTable = RetailThemeManager.styledTable(movementsModel);
        tabs.addTab("Recent Movements", RetailThemeManager.scroll(movTable));

        // Low stock
        String[] lsCols = {"Product", "SKU", "Current Stock", "Minimum Stock", "Deficit"};
        lowStockModel = new DefaultTableModel(lsCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable lsTable = RetailThemeManager.styledTable(lowStockModel);
        tabs.addTab("Low Stock Alerts", RetailThemeManager.scroll(lsTable));

        add(tabs, BorderLayout.CENTER);
    }

    private void loadData() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                // Recent movements (last 200)
                List<InventoryMovement> movements = new InventoryRepository()
                    .findByDateRange(java.time.LocalDateTime.now().minusDays(30), java.time.LocalDateTime.now());
                SwingUtilities.invokeLater(() -> {
                    movementsModel.setRowCount(0);
                    for (InventoryMovement m : movements) {
                        movementsModel.addRow(new Object[]{
                            m.getCreatedAt() != null ? DT.format(m.getCreatedAt()) : "",
                            m.getProductName(), m.getType(), m.getQuantity(),
                            m.getReason(), m.getBatchNumber(), m.getUserId()
                        });
                    }
                });
                // Low stock
                List<Product> lowStock = productService.getLowStock();
                SwingUtilities.invokeLater(() -> {
                    lowStockModel.setRowCount(0);
                    for (Product p : lowStock) {
                        lowStockModel.addRow(new Object[]{
                            p.getName(), p.getSku(), p.getCurrentStock(),
                            p.getMinimumStock(), p.getMinimumStock() - p.getCurrentStock()
                        });
                    }
                });
                return null;
            }
            @Override protected void done() {}
        }.execute();
    }

    private void showAdjustDialog() {
        String productName = JOptionPane.showInputDialog(this, "Enter product name or SKU:");
        if (productName == null || productName.isBlank()) return;
        List<Product> results = productService.search(productName);
        if (results.isEmpty()) { JOptionPane.showMessageDialog(this, "Product not found"); return; }
        Product p = results.get(0);
        String qtyStr = JOptionPane.showInputDialog(this, "New stock quantity for '" + p.getName() + "' (current: " + p.getCurrentStock() + "):");
        if (qtyStr == null || qtyStr.isBlank()) return;
        String reason = JOptionPane.showInputDialog(this, "Reason for adjustment (required):");
        if (reason == null || reason.isBlank()) { JOptionPane.showMessageDialog(this, "Reason is required"); return; }
        try {
            int newQty = Integer.parseInt(qtyStr.trim());
            inventoryService.recordAdjustment(p.getId(), newQty, reason, AuthService.getInstance().getCurrentUser().getId());
            loadData(); JOptionPane.showMessageDialog(this, "Adjustment recorded successfully");
        } catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Invalid quantity"); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void showDamagedDialog() {
        String productName = JOptionPane.showInputDialog(this, "Enter product name or SKU:");
        if (productName == null || productName.isBlank()) return;
        List<Product> results = productService.search(productName);
        if (results.isEmpty()) { JOptionPane.showMessageDialog(this, "Product not found"); return; }
        Product p = results.get(0);
        String qtyStr = JOptionPane.showInputDialog(this, "Quantity of damaged units for '" + p.getName() + "':");
        if (qtyStr == null) return;
        try {
            int qty = Integer.parseInt(qtyStr.trim());
            inventoryService.recordDamaged(p.getId(), qty, AuthService.getInstance().getCurrentUser().getId());
            loadData(); JOptionPane.showMessageDialog(this, "Damaged goods recorded");
        } catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Invalid quantity"); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void showExpiredDialog() {
        String productName = JOptionPane.showInputDialog(this, "Enter product name or SKU:");
        if (productName == null || productName.isBlank()) return;
        List<Product> results = productService.search(productName);
        if (results.isEmpty()) { JOptionPane.showMessageDialog(this, "Product not found"); return; }
        Product p = results.get(0);
        String qtyStr = JOptionPane.showInputDialog(this, "Quantity of expired units for '" + p.getName() + "':");
        if (qtyStr == null) return;
        String batchNo = JOptionPane.showInputDialog(this, "Batch number (optional):");
        try {
            int qty = Integer.parseInt(qtyStr.trim());
            inventoryService.recordExpired(p.getId(), qty, batchNo, AuthService.getInstance().getCurrentUser().getId());
            loadData(); JOptionPane.showMessageDialog(this, "Expired goods recorded");
        } catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Invalid quantity"); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }
}
