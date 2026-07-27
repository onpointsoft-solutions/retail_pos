package com.retailpos.view;

import com.retailpos.model.Product;
import com.retailpos.service.ProductService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class StockCheckPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ProductService productService = ProductService.getInstance();
    private JTextField skuField;
    private JLabel nameValue;
    private JLabel skuValue;
    private JLabel barcodeValue;
    private JLabel stockValue;
    private JLabel minimumValue;
    private JLabel preferredValue;
    private JLabel priceValue;
    private JLabel stockStatusValue;
    private JLabel statusLabel;
    private DefaultTableModel historyModel;

    public StockCheckPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        buildUI();
    }

    private void buildUI() {
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        search.setOpaque(false);
        search.add(new JLabel(Icons.get("barcode", 22)));
        skuField = RetailThemeManager.styledField();
        skuField.setPreferredSize(new Dimension(360, 44));
        skuField.putClientProperty("JTextField.placeholderText", "Scan or enter SKU");
        JButton checkButton = RetailThemeManager.primaryButton("Check Stock", "search");
        JButton clearButton = RetailThemeManager.secondaryButton("Clear");
        checkButton.addActionListener(e -> checkStock());
        clearButton.addActionListener(e -> clear());
        skuField.addActionListener(e -> checkStock());
        search.add(skuField);
        search.add(checkButton);
        search.add(clearButton);
        top.add(search, BorderLayout.WEST);

        statusLabel = RetailThemeManager.subLabel("Ready to scan SKU");
        top.add(statusLabel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel card = RetailThemeManager.card();
        card.setLayout(new GridLayout(4, 2, 14, 12));
        nameValue = valueLabel("-");
        skuValue = valueLabel("-");
        barcodeValue = valueLabel("-");
        stockValue = metricLabel("-");
        minimumValue = valueLabel("-");
        preferredValue = valueLabel("-");
        priceValue = valueLabel("-");
        card.add(info("Product", nameValue));
        card.add(info("Current Stock", stockValue));
        card.add(info("SKU", skuValue));
        card.add(info("Barcode", barcodeValue));
        card.add(info("Minimum Stock", minimumValue));
        card.add(info("Preferred Order Qty", preferredValue));
        card.add(info("Selling Price", priceValue));
        stockStatusValue = valueLabel("-");
        card.add(info("Status", stockStatusValue));
        add(card, BorderLayout.CENTER);

        historyModel = new DefaultTableModel(new String[]{"Time", "SKU", "Product", "Stock", "Min", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable historyTable = RetailThemeManager.styledTable(historyModel);
        JPanel history = new JPanel(new BorderLayout(0, 8));
        history.setOpaque(false);
        JLabel title = RetailThemeManager.headerLabel("Recent Stock Checks");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        history.add(title, BorderLayout.NORTH);
        history.add(RetailThemeManager.scroll(historyTable), BorderLayout.CENTER);
        add(history, BorderLayout.SOUTH);
    }

    private JPanel info(String label, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel caption = RetailThemeManager.subLabel(label);
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(caption, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JLabel valueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(RetailThemeManager.TEXT);
        return label;
    }

    private JLabel metricLabel(String text) {
        JLabel label = valueLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 28));
        label.setForeground(RetailThemeManager.PRIMARY);
        return label;
    }

    private void checkStock() {
        String sku = skuField.getText().trim();
        if (sku.isEmpty()) {
            statusLabel.setText("Enter or scan a SKU first");
            statusLabel.setForeground(RetailThemeManager.DANGER);
            skuField.requestFocusInWindow();
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Optional<Product>, Void>() {
            @Override protected Optional<Product> doInBackground() {
                return productService.findBySku(sku);
            }

            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Optional<Product> product = get();
                    if (product.isEmpty()) {
                        showMissing(sku);
                        return;
                    }
                    showProduct(product.get());
                } catch (Exception exception) {
                    statusLabel.setText("Stock check failed: " + exception.getMessage());
                    statusLabel.setForeground(RetailThemeManager.DANGER);
                }
            }
        }.execute();
    }

    private void showProduct(Product product) {
        nameValue.setText(product.getName());
        skuValue.setText(product.getSku());
        barcodeValue.setText(product.getBarcode() != null ? product.getBarcode() : "-");
        stockValue.setText(String.valueOf(product.getCurrentStock()));
        minimumValue.setText(String.valueOf(product.getMinimumStock()));
        preferredValue.setText(String.valueOf(product.getPreferredOrderQuantity()));
        priceValue.setText(String.format("KES %.2f", product.getSellingPrice()));

        String stockStatus = product.getCurrentStock() <= 0
            ? "Out of stock"
            : product.getCurrentStock() <= product.getMinimumStock() ? "Low stock" : "Available";
        stockStatusValue.setText(stockStatus);
        stockValue.setForeground(product.getCurrentStock() <= 0
            ? RetailThemeManager.DANGER
            : product.getCurrentStock() <= product.getMinimumStock() ? RetailThemeManager.WARNING : RetailThemeManager.ACCENT);
        stockStatusValue.setForeground(product.getCurrentStock() <= product.getMinimumStock()
            ? RetailThemeManager.WARNING : RetailThemeManager.ACCENT);
        statusLabel.setText(stockStatus + " - " + product.getName());
        statusLabel.setForeground(product.getCurrentStock() <= product.getMinimumStock()
            ? RetailThemeManager.WARNING : RetailThemeManager.ACCENT);
        addHistory(product.getSku(), product.getName(), product.getCurrentStock(), product.getMinimumStock(), stockStatus);
        skuField.selectAll();
        skuField.requestFocusInWindow();
    }

    private void showMissing(String sku) {
        resetProductFields();
        skuValue.setText(sku);
        stockStatusValue.setText("Not found");
        stockStatusValue.setForeground(RetailThemeManager.DANGER);
        stockValue.setForeground(RetailThemeManager.DANGER);
        statusLabel.setText("No active product found for SKU: " + sku);
        statusLabel.setForeground(RetailThemeManager.DANGER);
        addHistory(sku, "-", 0, 0, "Not found");
        skuField.selectAll();
        skuField.requestFocusInWindow();
    }

    private void resetProductFields() {
        nameValue.setText("-");
        skuValue.setText("-");
        barcodeValue.setText("-");
        stockValue.setText("-");
        minimumValue.setText("-");
        preferredValue.setText("-");
        priceValue.setText("-");
        stockStatusValue.setText("-");
        stockStatusValue.setForeground(RetailThemeManager.TEXT);
        stockValue.setForeground(RetailThemeManager.PRIMARY);
    }

    private void addHistory(String sku, String productName, int stock, int minimum, String status) {
        historyModel.insertRow(0, new Object[]{
            TIME_FORMAT.format(LocalDateTime.now()), sku, productName, stock, minimum, status
        });
        while (historyModel.getRowCount() > 30) historyModel.removeRow(historyModel.getRowCount() - 1);
    }

    private void clear() {
        skuField.setText("");
        resetProductFields();
        historyModel.setRowCount(0);
        statusLabel.setText("Ready to scan SKU");
        statusLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        skuField.requestFocusInWindow();
    }
}
