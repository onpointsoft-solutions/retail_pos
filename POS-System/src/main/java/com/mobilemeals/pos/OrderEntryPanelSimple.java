package com.mobilemeals.pos;

import com.mobilemeals.pos.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel for entering a new order – menu item selector + line-item table.
 */
public class OrderEntryPanelSimple extends JPanel {

    // -------------------------------------------------------------------------
    // Inner class: OrderItem
    // -------------------------------------------------------------------------

    public static class OrderItem {

        private final String code;
        private final String name;
        private       int    quantity;
        private final double unitPrice;
        private       String notes;

        public OrderItem(String code, String name, int quantity, double unitPrice) {
            this.code      = code;
            this.name      = name;
            this.quantity  = quantity;
            this.unitPrice = unitPrice;
            this.notes     = "";
        }

        // Getters
        public String getCode()       { return code; }
        public String getName()       { return name; }
        public int    getQuantity()   { return quantity; }
        public double getUnitPrice()  { return unitPrice; }
        public String getNotes()      { return notes; }
        public double getLineTotal()  { return quantity * unitPrice; }

        // Aliases for compatibility
        public double getPrice() { return unitPrice; }
        public double getTotal() { return getLineTotal(); }

        // Setters
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setNotes(String notes)    { this.notes = notes; }

        @Override
        public String toString() {
            return name + " x" + quantity;
        }
    }

    // -------------------------------------------------------------------------
    // UI Fields
    // -------------------------------------------------------------------------

    private final OrderTableModel tableModel;
    private       JTable          orderTable;
    private       JLabel          lblTotal;

    private final OrderManagerSimple orderManager;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public OrderEntryPanelSimple(OrderManagerSimple orderManager) {
        this.orderManager = orderManager;
        this.tableModel   = new OrderTableModel();
        buildUI();
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Table
        orderTable = new JTable(tableModel);
        orderTable.setRowHeight(30);
        orderTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        orderTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // Footer – total + action buttons
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));

        lblTotal = ThemeManager.createHeaderLabel("Total: KES 0.00");
        lblTotal.setForeground(ThemeManager.PRIMARY_COLOR);
        footer.add(lblTotal, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRemove  = ThemeManager.createDangerButton("Remove Selected");
        JButton btnClear   = ThemeManager.createSecondaryButton("Clear All");
        JButton btnConfirm = ThemeManager.createSuccessButton("Confirm Order");

        btnRemove.addActionListener(e -> removeSelectedRow());
        btnClear.addActionListener(e  -> clearOrder());
        btnConfirm.addActionListener(e -> confirmOrder());

        btnPanel.add(btnRemove);
        btnPanel.add(btnClear);
        btnPanel.add(btnConfirm);
        footer.add(btnPanel, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    public void addItem(OrderItem item) {
        tableModel.addItem(item);
        refreshTotal();
    }

    private void removeSelectedRow() {
        int row = orderTable.getSelectedRow();
        if (row >= 0) {
            tableModel.removeRow(row);
            refreshTotal();
        }
    }

    private void clearOrder() {
        tableModel.clearItems();
        refreshTotal();
    }

    private void confirmOrder() {
        if (tableModel.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please add at least one item before confirming.",
                    "Empty Order", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String customer = JOptionPane.showInputDialog(this,
                "Enter customer name:", "Customer", JOptionPane.PLAIN_MESSAGE);
        if (customer == null || customer.isBlank()) return;

        OrderManagerSimple.POSOrder order = orderManager.createOrder(customer.trim());
        for (OrderItem item : tableModel.getItems()) {
            orderManager.addItemToOrder(order, item);
        }
        clearOrder();
        JOptionPane.showMessageDialog(this,
                "Order " + order.getOrderNumber() + " created successfully!",
                "Order Confirmed", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTotal() {
        lblTotal.setText(String.format("Total: KES %.2f", tableModel.getGrandTotal()));
    }
}