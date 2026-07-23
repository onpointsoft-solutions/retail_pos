package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.text.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Order Entry Panel for POS
 * Handles order creation and item management
 */
public class OrderEntryPanel extends JPanel {
    
    private OrderManager orderManager;
    private MenuManager menuManager;
    private RestaurantSession session;
    
    // UI Components
    private JPanel customerInfoPanel;
    private JPanel menuItemsPanel;
    private JPanel orderItemsPanel;
    private JPanel totalsPanel;
    private JPanel actionsPanel;
    
    // Customer Info Components
    private JTextField txtCustomerName;
    private JTextField txtCustomerPhone;
    private JTextArea txtCustomerAddress;
    
    // Menu Items Components
    private JTabbedPane categoryTabs;
    private Map<String, JPanel> categoryPanels;
    
    // Order Items Components
    private JTable orderItemsTable;
    private OrderItemsTableModel tableModel;
    
    // Totals Components
    private JLabel lblSubtotal;
    private JLabel lblTax;
    private JLabel lblDeliveryFee;
    private JLabel lblTotal;
    
    // Actions Components
    private JButton btnAddItem;
    private JButton btnRemoveItem;
    private JButton btnClearOrder;
    private JButton btnSubmitOrder;
    private JButton btnPrintReceipt;
    
    // Constants
    private static final double TAX_RATE = 0.16; // 16% VAT
    private static final double DELIVERY_FEE = 50.0;
    
    public OrderEntryPanel(OrderManager orderManager, MenuManager menuManager, RestaurantSession session) {
        this.orderManager = orderManager;
        this.menuManager = menuManager;
        this.session = session;
        
        initializePanel();
        setupUI();
        setupEventHandlers();
        loadMenuItems();
    }
    
    private void initializePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    private void setupUI() {
        // Create main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(400);
        
        // Left panel - Customer Info and Menu Items
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(createCustomerInfoPanel(), BorderLayout.NORTH);
        leftPanel.add(createMenuItemsPanel(), BorderLayout.CENTER);
        
        // Right panel - Order Items and Totals
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(createOrderItemsPanel(), BorderLayout.CENTER);
        rightPanel.add(createTotalsPanel(), BorderLayout.SOUTH);
        
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        
        // Add actions panel at bottom
        add(mainSplitPane, BorderLayout.CENTER);
        add(createActionsPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createCustomerInfoPanel() {
        customerInfoPanel = new JPanel(new GridBagLayout());
        customerInfoPanel.setBorder(BorderFactory.createTitledBorder("Customer Information"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Customer Name
        gbc.gridx = 0; gbc.gridy = 0;
        customerInfoPanel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtCustomerName = new JTextField(20);
        customerInfoPanel.add(txtCustomerName, gbc);
        
        // Customer Phone
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        customerInfoPanel.add(new JLabel("Phone:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtCustomerPhone = new JTextField(20);
        customerInfoPanel.add(txtCustomerPhone, gbc);
        
        // Customer Address
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        customerInfoPanel.add(new JLabel("Address:"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JScrollPane addressScrollPane = new JScrollPane(txtCustomerAddress = new JTextArea(3, 20));
        customerInfoPanel.add(addressScrollPane, gbc);
        
        return customerInfoPanel;
    }
    
    private JPanel createMenuItemsPanel() {
        menuItemsPanel = new JPanel(new BorderLayout());
        menuItemsPanel.setBorder(BorderFactory.createTitledBorder("Menu Items"));
        
        categoryTabs = new JTabbedPane();
        categoryPanels = new HashMap<>();
        
        return menuItemsPanel;
    }
    
    private JPanel createOrderItemsPanel() {
        orderItemsPanel = new JPanel(new BorderLayout());
        orderItemsPanel.setBorder(BorderFactory.createTitledBorder("Order Items"));
        
        // Create table model
        tableModel = new OrderItemsTableModel();
        
        // Create table
        orderItemsTable = new JTable(tableModel);
        orderItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderItemsTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        orderItemsTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Qty
        orderItemsTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Item
        orderItemsTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Price
        orderItemsTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Total
        orderItemsTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // Actions
        
        JScrollPane scrollPane = new JScrollPane(orderItemsTable);
        orderItemsPanel.add(scrollPane, BorderLayout.CENTER);
        
        return orderItemsPanel;
    }
    
    private JPanel createTotalsPanel() {
        totalsPanel = new JPanel(new GridBagLayout());
        totalsPanel.setBorder(BorderFactory.createTitledBorder("Order Totals"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Subtotal
        gbc.gridx = 0; gbc.gridy = 0;
        totalsPanel.add(new JLabel("Subtotal:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 1.0;
        lblSubtotal = new JLabel("KES 0.00");
        lblSubtotal.setFont(new Font("Arial", Font.BOLD, 14));
        totalsPanel.add(lblSubtotal, gbc);
        
        // Tax
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.0;
        totalsPanel.add(new JLabel("Tax (16%):"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 1.0;
        lblTax = new JLabel("KES 0.00");
        totalsPanel.add(lblTax, gbc);
        
        // Delivery Fee
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.0;
        totalsPanel.add(new JLabel("Delivery:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 1.0;
        lblDeliveryFee = new JLabel("KES 50.00");
        totalsPanel.add(lblDeliveryFee, gbc);
        
        // Total
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        JSeparator separator = new JSeparator();
        totalsPanel.add(separator, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        totalsPanel.add(new JLabel("Total:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST;
        lblTotal = new JLabel("KES 0.00");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 16));
        lblTotal.setForeground(Color.BLUE);
        totalsPanel.add(lblTotal, gbc);
        
        return totalsPanel;
    }
    
    private JPanel createActionsPanel() {
        actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        btnAddItem = new JButton("Add Item");
        btnRemoveItem = new JButton("Remove Item");
        btnClearOrder = new JButton("Clear Order");
        btnSubmitOrder = new JButton("Submit Order");
        btnPrintReceipt = new JButton("Print Receipt");
        
        // Set button colors
        btnAddItem.setBackground(Color.GREEN);
        btnAddItem.setForeground(Color.WHITE);
        
        btnRemoveItem.setBackground(Color.ORANGE);
        btnRemoveItem.setForeground(Color.WHITE);
        
        btnClearOrder.setBackground(Color.GRAY);
        btnClearOrder.setForeground(Color.WHITE);
        
        btnSubmitOrder.setBackground(Color.BLUE);
        btnSubmitOrder.setForeground(Color.WHITE);
        
        btnPrintReceipt.setBackground(new Color(0, 128, 0));
        btnPrintReceipt.setForeground(Color.WHITE);
        
        actionsPanel.add(btnAddItem);
        actionsPanel.add(btnRemoveItem);
        actionsPanel.add(btnClearOrder);
        actionsPanel.add(btnSubmitOrder);
        actionsPanel.add(btnPrintReceipt);
        
        return actionsPanel;
    }
    
    private void setupEventHandlers() {
        // Order manager listener
        orderManager.setOrderListener(new OrderManager.OrderListener() {
            @Override
            public void onOrderItemAdded(OrderManager.OrderItem item) {
                tableModel.addItem(item);
                updateTotals();
            }
            
            @Override
            public void onOrderItemRemoved(OrderManager.OrderItem item) {
                tableModel.removeItem(item);
                updateTotals();
            }
            
            @Override
            public void onOrderItemUpdated(OrderManager.OrderItem item) {
                tableModel.updateItem(item);
                updateTotals();
            }
            
            @Override
            public void onOrderSubmitted(OrderManager.OrderManager.POSOrder order) {
                JOptionPane.showMessageDialog(OrderEntryPanel.this, 
                    "Order submitted successfully!\nOrder Number: " + order.getOrderNumber(), 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                clearOrder();
            }
            
            @Override
            public void onOrderStatusUpdated(OrderManager.POSOrder order, String oldStatus, String newStatus) {
                // Handle status updates
            }
            
            @Override
            public void onOrderCancelled(OrderManager.OrderManager.POSOrder order) {
                // Handle order cancellation
            }
        });
        
        // Button listeners
        btnAddItem.addActionListener(e -> showAddItemDialog());
        btnRemoveItem.addActionListener(e -> removeSelectedItem());
        btnClearOrder.addActionListener(e -> clearOrder());
        btnSubmitOrder.addActionListener(e -> submitOrder());
        btnPrintReceipt.addActionListener(e -> printReceipt());
    }
    
    private void loadMenuItems() {
        // Load menu items by category
        Map<String, List<MenuManager.MenuItem>> categories = menuManager.getMenuCategories();
        
        for (Map.Entry<String, List<MenuManager.MenuItem>> entry : categories.entrySet()) {
            String categoryName = entry.getKey();
            List<MenuManager.MenuItem> items = entry.getValue();
            
            JPanel categoryPanel = createCategoryPanel(items);
            categoryTabs.addTab(categoryName, categoryPanel);
            categoryPanels.put(categoryName, categoryPanel);
        }
    }
    
    private JPanel createCategoryPanel(List<MenuManager.MenuItem> items) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (MenuManager.MenuItem item : items) {
            if (item.isAvailable()) {
                JButton itemButton = createMenuItemButton(item);
                panel.add(itemButton);
            }
        }
        
        // Add scroll pane if too many items
        if (panel.getComponentCount() > 10) {
            JScrollPane scrollPane = new JScrollPane(panel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            return scrollPane;
        }
        
        return panel;
    }
    
    private JButton createMenuItemButton(MenuManager.MenuItem item) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        button.setBackground(Color.WHITE);
        
        // Item name
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Item price
        JLabel priceLabel = new JLabel(item.getFormattedPrice());
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(Color.BLUE);
        
        button.add(nameLabel, BorderLayout.CENTER);
        button.add(priceLabel, BorderLayout.SOUTH);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        button.addActionListener(e -> addMenuItemToOrder(item));
        
        return button;
    }
    
    private void addMenuItemToOrder(MenuManager.MenuItem item) {
        // Show quantity dialog
        String quantityStr = JOptionPane.showInputDialog(this, 
            "Enter quantity for " + item.getName() + ":", "1");
        
        if (quantityStr != null && !quantityStr.trim().isEmpty()) {
            try {
                int quantity = Integer.parseInt(quantityStr.trim());
                if (quantity > 0) {
                    orderManager.addItemToCurrentOrder(item, quantity);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Quantity must be greater than 0", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid quantity", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void showAddItemDialog() {
        // Show dialog to add custom item
        JOptionPane.showMessageDialog(this, 
            "Custom item addition not implemented yet", 
            "Info", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void removeSelectedItem() {
        int selectedRow = orderItemsTable.getSelectedRow();
        if (selectedRow >= 0) {
            OrderManager.OrderItem item = tableModel.getItemAt(selectedRow);
            orderManager.removeItemFromCurrentOrder(item);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select an item to remove", 
                "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void clearOrder() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to clear the current order?", 
            "Clear Order", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            orderManager.clearCurrentOrder();
            tableModel.clearItems();
            updateTotals();
            
            // Clear customer info
            txtCustomerName.setText("");
            txtCustomerPhone.setText("");
            txtCustomerAddress.setText("");
        }
    }
    
    private void submitOrder() {
        if (tableModel.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please add items to the order", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Update customer info
        String customerName = txtCustomerName.getText().trim();
        String customerPhone = txtCustomerPhone.getText().trim();
        String customerAddress = txtCustomerAddress.getText().trim();
        
        orderManager.updateOrderCustomer(customerName, customerPhone, customerAddress);
        
        // Submit order
        if (orderManager.submitCurrentOrder()) {
            // Success is handled in the listener
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to submit order", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void printReceipt() {
        if (tableModel.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "No order to print", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create receipt printer
        ReceiptPrinter printer = new ReceiptPrinter();
        printer.printOrderReceipt(orderManager.getCurrentOrder(), session);
    }
    
    private void updateTotals() {
        double subtotal = orderManager.getCurrentOrderTotal();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax + DELIVERY_FEE;
        
        DecimalFormat df = new DecimalFormat("KES #,##0.00");
        
        lblSubtotal.setText(df.format(subtotal));
        lblTax.setText(df.format(tax));
        lblTotal.setText(df.format(total));
    }
    
    // Custom Table Model for Order Items
    private class OrderItemsTableModel extends javax.swing.table.AbstractTableModel {
        private final String[] columnNames = {"Qty", "Item", "Price", "Total", "Actions"};
        private List<OrderManager.OrderItem> items = new ArrayList<>();
        
        @Override
        public int getRowCount() {
            return items.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            OrderManager.OrderItem item = items.get(rowIndex);
            
            switch (columnIndex) {
                case 0: return item.getQuantity();
                case 1: return item.getMenuItem().getName();
                case 2: return String.format("KES %.2f", item.getUnitPrice());
                case 3: return String.format("KES %.2f", item.getTotalPrice());
                case 4: return "Remove";
                default: return null;
            }
        }
        
        public void addItem(OrderManager.OrderItem item) {
            items.add(item);
            fireTableRowsInserted(items.size() - 1, items.size() - 1);
        }
        
        public void removeItem(OrderManager.OrderItem item) {
            int index = items.indexOf(item);
            if (index >= 0) {
                items.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }
        
        public void updateItem(OrderManager.OrderItem item) {
            int index = items.indexOf(item);
            if (index >= 0) {
                fireTableRowsUpdated(index, index);
            }
        }
        
        public void clearItems() {
            int size = items.size();
            items.clear();
            fireTableRowsDeleted(0, size - 1);
        }
        
        public OrderManager.OrderItem getItemAt(int row) {
            return items.get(row);
        }
        
        public int getItemCount() {
            return items.size();
        }
    }
}
