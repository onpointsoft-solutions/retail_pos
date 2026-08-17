package com.retailpos.view;

import com.retailpos.model.*;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.service.*;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
public class SalesPanel extends JPanel {
    // Cart state
    private final List<Sale.SaleItem> cartItems = new ArrayList<>();
    private double transactionDiscount = 0;
    private String attachedCustomerId = null;
    private String attachedCustomerName = null;

    // UI
    private JTextField searchField;
    private DefaultTableModel cartModel;
    private JTable cartTable;
    private boolean refreshingCart;
    private JLabel subtotalLabel, discountLabel, taxLabel, totalLabel;
    private JLabel customerLabel, stockWarningLabel;
    private JPanel categoryPanel;
    private AppSettings settings;

    // Scanner detection
    private final StringBuilder scanBuffer = new StringBuilder();
    private long lastKeyTime = 0;
    private static final long SCAN_THRESHOLD_MS = 80;

    // Debounce for typed search
    private javax.swing.Timer searchDebounce;

    // Services
    private final ProductService productService = ProductService.getInstance();
    private final SaleService saleService = SaleService.getInstance();
    // Product grid for visual browsing
    private JPanel productGrid;
    private JScrollPane productGridScroll;
    private List<Product> allProducts = new ArrayList<>();
    private String activeCategoryId = null;

    public SalesPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(RetailThemeManager.SURFACE);
        loadSettings();
        buildUI();
        registerKeyboardShortcuts();
        loadCategoriesAndProducts(); // load everything at once
    }

    private void loadSettings() {
        try { settings = new SettingsRepository().load(); }
        catch (Exception e) { settings = new AppSettings(); }
    }

    private void buildUI() {
        // ── LEFT: search + category bar + product grid ────────────────────────
        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setOpaque(false);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        JLabel searchIcon = new JLabel(Icons.get("search", 20));
        searchField = RetailThemeManager.styledField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        searchField.putClientProperty("JTextField.placeholderText",
            "Scan barcode or type product name / SKU  [F3]");
        searchBar.add(searchIcon, BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);

        // Category quick-filter bar
        categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        categoryPanel.setOpaque(false);
        JScrollPane catScroll = new JScrollPane(categoryPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        catScroll.setOpaque(false); catScroll.getViewport().setOpaque(false);
        catScroll.setBorder(null); catScroll.setPreferredSize(new Dimension(0, 44));

        // Product grid panel (visual cards — loaded from DB)
        productGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        productGrid.setBackground(RetailThemeManager.SURFACE);
        productGridScroll = new JScrollPane(productGrid,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        productGridScroll.setBorder(BorderFactory.createLineBorder(RetailThemeManager.BORDER, 1));
        productGridScroll.getVerticalScrollBar().setUnitIncrement(20);

        JPanel leftTop = new JPanel(new BorderLayout(0, 4));
        leftTop.setOpaque(false);
        leftTop.add(searchBar, BorderLayout.NORTH);
        leftTop.add(catScroll, BorderLayout.CENTER);
        
        // Sort control
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        sortPanel.setOpaque(false);
        JLabel sortLabel = new JLabel("Sort:");
        sortLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sortLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        String[] sortOptions = {"Name", "Price (Low-High)", "Price (High-Low)", "Stock"};
        JComboBox<String> sortCombo = new JComboBox<>(sortOptions);
        sortCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sortCombo.addActionListener(e -> sortProductGrid((String) sortCombo.getSelectedItem()));
        sortPanel.add(sortLabel);
        sortPanel.add(sortCombo);
        
        JButton clearFiltersBtn = RetailThemeManager.secondaryButton("Clear");
        clearFiltersBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearFiltersBtn.addActionListener(e -> clearFilters());
        sortPanel.add(clearFiltersBtn);
        
        leftTop.add(sortPanel, BorderLayout.SOUTH);
        
        left.add(leftTop, BorderLayout.NORTH);
        left.add(productGridScroll, BorderLayout.CENTER);

        // Status/warning label
        stockWarningLabel = new JLabel("Loading products…");
        stockWarningLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        stockWarningLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        left.add(stockWarningLabel, BorderLayout.SOUTH);

        // RIGHT: Cart
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(480, 0));

        // Cart table
        String[] cols = {"Product", "Qty", "Price", "Disc", "Total"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1 || c == 3; }
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? Integer.class : String.class; }
        };
        cartTable = RetailThemeManager.styledTable(cartModel);
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        cartTable.setAutoCreateRowSorter(false);

        // Qty editing: commit on Enter
        cartModel.addTableModelListener(e -> {
            if (!refreshingCart && e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 1) {
                int row = e.getFirstRow();
                if (row >= 0 && row < cartItems.size()) {
                    try {
                        Object val = cartModel.getValueAt(row, 1);
                        int qty = val instanceof Integer ? (Integer) val : Integer.parseInt(val.toString());
                        if (qty <= 0) { removeCartRow(row); return; }
                        cartItems.get(row).setQuantity(qty);
                        cartItems.get(row).recalculate();
                        refreshCartRow(row);
                        updateTotals();
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        // Delete key removes selected row
        cartTable.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int row = cartTable.getSelectedRow();
                    if (row >= 0) removeCartRow(row);
                }
            }
        });

        JPanel cartArea = new JPanel(new BorderLayout(0, 6));
        cartArea.setOpaque(false);
        cartArea.add(RetailThemeManager.scroll(cartTable), BorderLayout.CENTER);
        JPanel quantityActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        quantityActions.setOpaque(false);
        JButton decreaseQuantity = RetailThemeManager.secondaryButton("−");
        JButton increaseQuantity = RetailThemeManager.primaryButton("+");
        decreaseQuantity.setToolTipText("Decrease selected item quantity");
        increaseQuantity.setToolTipText("Increase selected item quantity");
        decreaseQuantity.addActionListener(e -> changeSelectedCartQuantity(-1));
        increaseQuantity.addActionListener(e -> changeSelectedCartQuantity(1));
        quantityActions.add(decreaseQuantity); quantityActions.add(increaseQuantity);
        cartArea.add(quantityActions, BorderLayout.SOUTH);
        right.add(cartArea, BorderLayout.CENTER);

        // Totals panel
        JPanel totalsPanel = new JPanel(new GridLayout(4, 2, 4, 2));
        totalsPanel.setBackground(RetailThemeManager.CARD_BG);
        totalsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RetailThemeManager.BORDER),
            new EmptyBorder(10, 12, 10, 12)));
        subtotalLabel = addTotalRow(totalsPanel, "Subtotal:", "0.00");
        discountLabel = addTotalRow(totalsPanel, "Discount:", "0.00");
        taxLabel      = addTotalRow(totalsPanel, "VAT (" + String.format("%.0f%%", settings.getTaxRate()) + "):", "0.00");
        totalLabel    = addTotalRow(totalsPanel, "TOTAL:", "KES 0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setForeground(RetailThemeManager.PRIMARY);

        // Customer bar
        JPanel custBar = new JPanel(new BorderLayout(8, 0));
        custBar.setOpaque(false);
        customerLabel = new JLabel("No customer attached");
        customerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        customerLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        JButton attachCust = RetailThemeManager.secondaryButton("Customer", "user");
        attachCust.addActionListener(e -> showCustomerSearch());
        custBar.add(customerLabel, BorderLayout.CENTER);
        custBar.add(attachCust, BorderLayout.EAST);

        // Discount row
        JPanel discRow = new JPanel(new BorderLayout(8, 0));
        discRow.setOpaque(false);
        JTextField discountField = RetailThemeManager.styledField();
        discountField.putClientProperty("JTextField.placeholderText", "Transaction discount (KES)");
        discountField.setPreferredSize(new Dimension(160, 36));
        JButton applyDisc = RetailThemeManager.secondaryButton("Apply");
        applyDisc.addActionListener(e -> {
            try {
                transactionDiscount = Double.parseDouble(discountField.getText().trim());
                updateTotals();
            } catch (NumberFormatException ex) { transactionDiscount = 0; updateTotals(); }
        });
        discRow.add(discountField, BorderLayout.CENTER);
        discRow.add(applyDisc, BorderLayout.EAST);

        // Action buttons
        JPanel actionRow = new JPanel(new GridLayout(2, 2, 6, 6));
        actionRow.setOpaque(false);
        JButton payBtn     = RetailThemeManager.successButton("PAY", "pay");
        JButton suspendBtn = RetailThemeManager.secondaryButton("Suspend", "suspend");
        JButton resumeBtn  = RetailThemeManager.secondaryButton("Resume", "resume");
        JButton clearBtn   = RetailThemeManager.dangerButton("Clear", "delete");
        payBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        payBtn.addActionListener(e -> openPaymentDialog());
        suspendBtn.addActionListener(e -> suspendSale());
        resumeBtn.addActionListener(e -> resumeSale());
        clearBtn.addActionListener(e -> clearCart());
        actionRow.add(payBtn); actionRow.add(suspendBtn);
        actionRow.add(resumeBtn); actionRow.add(clearBtn);

        JPanel rightBottom = new JPanel();
        rightBottom.setLayout(new BoxLayout(rightBottom, BoxLayout.Y_AXIS));
        rightBottom.setOpaque(false);
        rightBottom.add(totalsPanel);
        rightBottom.add(Box.createVerticalStrut(6));
        rightBottom.add(custBar);
        rightBottom.add(Box.createVerticalStrut(4));
        rightBottom.add(discRow);
        rightBottom.add(Box.createVerticalStrut(6));
        rightBottom.add(actionRow);

        right.add(rightBottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private JLabel addTotalRow(JPanel parent, String name, String val) {
        JLabel lname = new JLabel(name);
        lname.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lname.setForeground(RetailThemeManager.TEXT_MUTED);
        JLabel lval = new JLabel(val);
        lval.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lval.setHorizontalAlignment(SwingConstants.RIGHT);
        parent.add(lname); parent.add(lval);
        return lval;
    }

    private void registerKeyboardShortcuts() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { searchField.requestFocus(); searchField.selectAll(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "pay");
        am.put("pay", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openPaymentDialog(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "suspend");
        am.put("suspend", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { suspendSale(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "resume");
        am.put("resume", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { resumeSale(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "clear");
        am.put("clear", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clearCart(); }
        });

        // Search field key listener for barcode scanner detection
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastKeyTime < SCAN_THRESHOLD_MS) {
                    scanBuffer.append(e.getKeyChar());
                } else {
                    scanBuffer.setLength(0);
                    if (e.getKeyChar() != '\n' && e.getKeyChar() != '\r') {
                        scanBuffer.append(e.getKeyChar());
                    }
                }
                lastKeyTime = now;

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String scanned = scanBuffer.toString().trim();
                    if (!scanned.isEmpty()) {
                        handleBarcodeInput(scanned);
                        scanBuffer.setLength(0);
                        searchField.setText("");
                    } else {
                        String typed = searchField.getText().trim();
                        if (!typed.isEmpty()) triggerSearch(typed);
                    }
                }
            }
        });

        // Debounced search while typing
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
    }

    private void scheduleSearch() {
        if (searchDebounce != null && searchDebounce.isRunning()) searchDebounce.stop();
        searchDebounce = new javax.swing.Timer(150, e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) triggerSearch(query);
        });
        searchDebounce.setRepeats(false);
        searchDebounce.start();
    }

    private void handleBarcodeInput(String code) {
        SwingWorker<Optional<Product>, Void> w = new SwingWorker<>() {
            @Override protected Optional<Product> doInBackground() {
                Optional<Product> p = productService.findByBarcode(code);
                if (p.isEmpty()) p = productService.findByQrCode(code);
                return p;
            }
            @Override protected void done() {
                try {
                    Optional<Product> opt = get();
                    if (opt.isPresent()) {
                        addToCart(opt.get());
                        stockWarningLabel.setForeground(RetailThemeManager.ACCENT);
                        stockWarningLabel.setText("Added: " + opt.get().getName());
                    } else {
                        stockWarningLabel.setForeground(RetailThemeManager.DANGER);
                        stockWarningLabel.setText("Product not found: " + code);
                        Toolkit.getDefaultToolkit().beep();
                    }
                } catch (Exception ex) {
                    stockWarningLabel.setText("⚠ Lookup error: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void triggerSearch(String query) {
        SwingWorker<List<Product>, Void> w = new SwingWorker<>() {
            @Override protected List<Product> doInBackground() {
                List<Product> results = productService.search(query);
                // Apply category filter if one is active
                if (activeCategoryId != null) {
                    results = results.stream()
                        .filter(p -> activeCategoryId.equals(p.getCategoryId()))
                        .collect(java.util.stream.Collectors.toList());
                }
                return results;
            }
            @Override protected void done() {
                try {
                    List<Product> results = get();
                    // Update product grid to show search results
                    renderProductGrid(results);
                    stockWarningLabel.setForeground(RetailThemeManager.TEXT_MUTED);
                    stockWarningLabel.setText(results.isEmpty()
                        ? "No products found for: " + query
                        : results.size() + " result(s)");

                    // Auto-add if exactly one active, in-stock result
                    if (results.size() == 1) {
                        Product p = results.get(0);
                        if (p.isActive() && p.getCurrentStock() > 0) {
                            addToCart(p);
                            searchField.setText("");
                            stockWarningLabel.setForeground(RetailThemeManager.ACCENT);
                            stockWarningLabel.setText("Added: " + p.getName());
                        } else {
                            stockWarningLabel.setForeground(RetailThemeManager.DANGER);
                            stockWarningLabel.setText(p.getName() +
                                (p.isActive() ? " — Out of stock" : " — Inactive"));
                        }
                    }
                } catch (Exception ex) {
                    stockWarningLabel.setForeground(RetailThemeManager.DANGER);
                    stockWarningLabel.setText("Search error: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void showProductSelection(List<Product> products) {
        String[] names = products.stream()
            .map(p -> p.getName() + " — KES " + String.format("%.2f", p.getSellingPrice()) + " [" + p.getSku() + "]")
            .toArray(String[]::new);
        String choice = (String) JOptionPane.showInputDialog(this,
            "Multiple products found. Select one:",
            "Product Selection", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (choice != null) {
            int idx = Arrays.asList(names).indexOf(choice);
            if (idx >= 0) addToCart(products.get(idx));
        }
    }

    public void addToCart(Product p) {
        // Check if already in cart
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getProductId().equals(p.getId())) {
                int newQty = cartItems.get(i).getQuantity() + 1;
                cartItems.get(i).setQuantity(newQty);
                cartItems.get(i).recalculate();
                refreshCartRow(i);
                updateTotals();
                return;
            }
        }
        // New item
        Sale.SaleItem item = new Sale.SaleItem();
        item.setProductId(p.getId()); item.setProductName(p.getName());
        item.setProductSku(p.getSku()); item.setQuantity(1);
        item.setUnitPrice(p.getSellingPrice()); item.setBuyingPrice(p.getBuyingPrice());
        item.setDiscount(0); item.setTaxRate(p.getTaxRate());
        item.recalculate();
        cartItems.add(item);
        cartModel.addRow(new Object[]{
            p.getName(), 1,
            String.format("%.2f", p.getSellingPrice()),
            "0.00",
            String.format("%.2f", item.getLineTotal())
        });
        updateTotals();
        stockWarningLabel.setText(" ");
    }

    private void refreshCartRow(int row) {
        if (row < 0 || row >= cartItems.size()) return;
        Sale.SaleItem item = cartItems.get(row);
        refreshingCart = true;
        try {
            cartModel.setValueAt(item.getQuantity(), row, 1);
            cartModel.setValueAt(String.format("%.2f", item.getUnitPrice()), row, 2);
            cartModel.setValueAt(String.format("%.2f", item.getDiscount()), row, 3);
            cartModel.setValueAt(String.format("%.2f", item.getLineTotal()), row, 4);
        } finally {
            refreshingCart = false;
        }
    }

    private void changeSelectedCartQuantity(int change) {
        if (cartTable.isEditing()) cartTable.getCellEditor().stopCellEditing();
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= cartItems.size()) {
            JOptionPane.showMessageDialog(this, "Select a cart item first.");
            return;
        }
        Sale.SaleItem item = cartItems.get(row);
        int quantity = item.getQuantity() + change;
        if (quantity <= 0) { removeCartRow(row); return; }
        item.setQuantity(quantity);
        item.recalculate();
        refreshCartRow(row);
        cartTable.setRowSelectionInterval(row, row);
        updateTotals();
    }

    private void removeCartRow(int row) {
        if (row < 0 || row >= cartItems.size()) return;
        cartItems.remove(row);
        cartModel.removeRow(row);
        updateTotals();
    }

    private void updateTotals() {
        double subtotal = cartItems.stream().mapToDouble(Sale.SaleItem::getLineTotal).sum();
        double taxAmount = (subtotal - transactionDiscount) * settings.getTaxRate() / 100.0;
        double grandTotal = subtotal - transactionDiscount + taxAmount;
        subtotalLabel.setText(String.format("%.2f", subtotal));
        discountLabel.setText(String.format("%.2f", transactionDiscount));
        taxLabel.setText(String.format("%.2f", taxAmount));
        totalLabel.setText(String.format("KES %.2f", grandTotal));
    }

    private void openPaymentDialog() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty", "No Items", JOptionPane.WARNING_MESSAGE);
            return;
        }
        loadSettings();
        PaymentDialog dialog = new PaymentDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            new ArrayList<>(cartItems), transactionDiscount, attachedCustomerId, settings);
        dialog.setVisible(true);
        if (dialog.isSaleCompleted()) {
            Sale sale = dialog.getCompletedSale();
            resetSalesScreen();
            java.nio.file.Path receiptPdf = null;
            try { receiptPdf = PrintService.getInstance().saveReceiptPdf(sale, settings); }
            catch (Exception e) { System.err.println("[SalesPanel] Receipt PDF failed: " + e.getMessage()); }
            if (settings.isAutoPrintReceipt()) {
                try { PrintService.getInstance().printReceipt(sale, settings); }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                        "Sale completed. Print failed: " + e.getMessage() +
                        "\nUse 'Reprint' from Sales History to retry.",
                        "Print Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
            JOptionPane.showMessageDialog(this, "Transaction saved successfully.\nReceipt: " + sale.getReceiptNumber()
                + (receiptPdf != null ? "\nPDF: " + receiptPdf : ""), "Sale completed", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void resetSalesScreen() {
        cartItems.clear();
        cartModel.setRowCount(0);
        transactionDiscount = 0;
        attachedCustomerId = null; attachedCustomerName = null;
        customerLabel.setText("No customer attached");
        updateTotals();
        stockWarningLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        stockWarningLabel.setText(allProducts.size() + " products available");
        // Restore full product grid
        renderProductGrid(allProducts);
        searchField.setText(""); searchField.requestFocus();
    }

    private void clearCart() {
        if (cartItems.isEmpty()) return;
        int r = JOptionPane.showConfirmDialog(this,
            "Clear the current cart?", "Clear Cart", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) resetSalesScreen();
    }

    private void suspendSale() {
        if (cartItems.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty"); return; }
        SwingWorker<SuspendedCart, Void> w = new SwingWorker<>() {
            @Override protected SuspendedCart doInBackground() throws Exception {
                String uid = AuthService.getInstance().getCurrentUser().getId();
                return saleService.suspendSale(uid, new ArrayList<>(cartItems), transactionDiscount, attachedCustomerId);
            }
            @Override protected void done() {
                try { get(); resetSalesScreen(); JOptionPane.showMessageDialog(SalesPanel.this, "Cart suspended. Resume with F8."); }
                catch (Exception e) { JOptionPane.showMessageDialog(SalesPanel.this, "Suspend failed: " + e.getMessage()); }
            }
        };
        w.execute();
    }

    private void resumeSale() {
        String uid = AuthService.getInstance().getCurrentUser().getId();
        SwingWorker<List<SuspendedCart>, Void> w = new SwingWorker<>() {
            @Override protected List<SuspendedCart> doInBackground() throws Exception {
                return saleService.getSuspendedCarts(uid);
            }
            @Override protected void done() {
                try {
                    List<SuspendedCart> carts = get();
                    if (carts.isEmpty()) { JOptionPane.showMessageDialog(SalesPanel.this, "No suspended carts"); return; }
                    if (!cartItems.isEmpty()) {
                        int r = JOptionPane.showConfirmDialog(SalesPanel.this,
                            "Resuming will replace the current cart. Continue?", "Resume Sale", JOptionPane.YES_NO_OPTION);
                        if (r != JOptionPane.YES_OPTION) return;
                    }
                    SuspendedCart cart;
                    if (carts.size() == 1) {
                        cart = carts.get(0);
                    } else {
                        String[] opts = carts.stream()
                            .map(c -> c.getSuspendedAt().toString().substring(0,16) + " — " + c.getItems().size() + " items")
                            .toArray(String[]::new);
                        String choice = (String) JOptionPane.showInputDialog(SalesPanel.this,
                            "Select cart to resume:", "Resume Cart",
                            JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
                        if (choice == null) return;
                        cart = carts.get(Arrays.asList(opts).indexOf(choice));
                    }
                    final SuspendedCart selected = cart;
                    new SwingWorker<SuspendedCart, Void>() {
                        @Override protected SuspendedCart doInBackground() throws Exception {
                            return saleService.resumeSale(selected.getId());
                        }
                        @Override protected void done() {
                            try {
                                SuspendedCart resumed = get();
                                resetSalesScreen();
                                for (Sale.SaleItem item : resumed.getItems()) {
                                    cartItems.add(item);
                                    cartModel.addRow(new Object[]{item.getProductName(), item.getQuantity(),
                                        String.format("%.2f", item.getUnitPrice()),
                                        String.format("%.2f", item.getDiscount()),
                                        String.format("%.2f", item.getLineTotal())});
                                }
                                transactionDiscount = resumed.getDiscountAmount();
                                attachedCustomerId = resumed.getCustomerId();
                                updateTotals();
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(SalesPanel.this, "Resume failed: " + ex.getMessage());
                            }
                        }
                    }.execute();
                } catch (Exception e) { JOptionPane.showMessageDialog(SalesPanel.this, "Error: " + e.getMessage()); }
            }
        };
        w.execute();
    }

    private void showCustomerSearch() {
        String query = JOptionPane.showInputDialog(this, "Search customer (name, phone, or email):");
        if (query == null || query.isBlank()) return;
        SwingWorker<List<Customer>, Void> w = new SwingWorker<>() {
            @Override protected List<Customer> doInBackground() throws Exception {
                return new com.retailpos.repository.CustomerRepository().search(query);
            }
            @Override protected void done() {
                try {
                    List<Customer> customers = get();
                    if (customers.isEmpty()) {
                        JOptionPane.showMessageDialog(SalesPanel.this, "Customer not found");
                        return;
                    }
                    Customer c;
                    if (customers.size() == 1) {
                        c = customers.get(0);
                    } else {
                        String[] opts = customers.stream()
                            .map(cu -> cu.getName() + " — " + cu.getPhone()).toArray(String[]::new);
                        String choice = (String) JOptionPane.showInputDialog(SalesPanel.this,
                            "Select customer:", "Customers", JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
                        if (choice == null) return;
                        c = customers.get(Arrays.asList(opts).indexOf(choice));
                    }
                    attachedCustomerId = c.getId();
                    attachedCustomerName = c.getName();
                    customerLabel.setText(c.getName() + " | Points: " + c.getLoyaltyPoints() +
                        " | Credit: KES " + String.format("%.2f", c.getCreditBalance()));
                } catch (Exception e) { JOptionPane.showMessageDialog(SalesPanel.this, "Search error: " + e.getMessage()); }
            }
        };
        w.execute();
    }

    private void loadCategoriesAndProducts() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            @SuppressWarnings("unchecked")
            protected Map<String, Object> doInBackground() throws Exception {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("categories", new com.retailpos.repository.CategoryRepository().findAll());
                data.put("products",   productService.getAllActive());
                return data;
            }
            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    List<com.retailpos.model.Category> cats =
                        (List<com.retailpos.model.Category>) data.get("categories");
                    allProducts = (List<Product>) data.get("products");

                    // Build category bar
                    categoryPanel.removeAll();
                    JButton allBtn = categoryButton("All", null);
                    allBtn.setBackground(RetailThemeManager.PRIMARY);
                    allBtn.setForeground(Color.WHITE);
                    categoryPanel.add(allBtn);

                    // Build category id -> name lookup for product cards
                    Map<String, String> catNames = new HashMap<>();
                    for (com.retailpos.model.Category c : cats) {
                        catNames.put(c.getId(), c.getName());
                        categoryPanel.add(categoryButton(c.getName(), c.getId()));
                    }
                    categoryPanel.revalidate(); categoryPanel.repaint();

                    // Store cat names for display
                    SalesPanel.this.categoryNames = catNames;

                    // Show all products in grid
                    renderProductGrid(allProducts);
                    stockWarningLabel.setText(allProducts.isEmpty()
                        ? "No products found. Add products in the Products tab."
                        : allProducts.size() + " products available");
                } catch (Exception e) {
                    stockWarningLabel.setText("Failed to load products: " + e.getMessage());
                }
            }
        }.execute();
    }

    private Map<String, String> categoryNames = new HashMap<>();

    private JButton categoryButton(String label, String categoryId) {
        boolean isAll = (categoryId == null);
        JButton btn = isAll
            ? RetailThemeManager.primaryButton(label)
            : RetailThemeManager.secondaryButton(label);
        btn.setPreferredSize(new Dimension(Math.max(70, label.length() * 9), 36));
        btn.addActionListener(e -> {
            activeCategoryId = categoryId;
            // Reset all button styles
            for (Component c : categoryPanel.getComponents()) {
                if (c instanceof JButton b) {
                    b.setBackground(RetailThemeManager.SURFACE);
                    b.setForeground(RetailThemeManager.TEXT);
                }
            }
            btn.setBackground(RetailThemeManager.PRIMARY);
            btn.setForeground(Color.WHITE);
            // Filter grid
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                triggerSearch(query);
            } else {
                filterProductGrid();
            }
        });
        return btn;
    }

    private void filterProductGrid() {
        List<Product> filtered = activeCategoryId == null ? allProducts
            : allProducts.stream()
                .filter(p -> activeCategoryId.equals(p.getCategoryId()))
                .collect(java.util.stream.Collectors.toList());
        renderProductGrid(filtered);
    }

    private void renderProductGrid(List<Product> products) {
        productGrid.removeAll();
        for (Product p : products) {
            productGrid.add(createProductCard(p));
        }
        productGrid.revalidate();
        productGrid.repaint();
        if (productGridScroll != null) {
            productGridScroll.getVerticalScrollBar().setValue(0);
        }
    }

    private JPanel createProductCard(Product p) {
        String catName   = categoryNames.getOrDefault(p.getCategoryId(), "");
        boolean lowStock  = p.isLowStock();
        boolean outOfStock = p.getCurrentStock() <= 0;

        // ── accent colours per stock state — resolved dynamically at paint time ─
        // We do NOT capture 'dark' here; instead all colour decisions are
        // deferred into paintComponent so they always reflect the current theme.
        Color accentColor = outOfStock ? RetailThemeManager.DANGER
                          : lowStock   ? RetailThemeManager.WARNING
                          :              RetailThemeManager.ACCENT;

        // ── load thumbnail ────────────────────────────────────────────────────
        ImageIcon thumb = productImage(p.getImagePath(), 44, 44);

        // ── card panel ────────────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                // Resolve theme-sensitive colours at paint time so dark-mode
                // toggles are reflected without recreating the card.
                boolean dm = RetailThemeManager.getInstance().isDark();
                Color cardBg  = outOfStock ? (dm ? new Color(60, 18, 18)  : new Color(255, 241, 241))
                              : lowStock   ? (dm ? new Color(58, 40,  8)  : new Color(255, 252, 235))
                              :               RetailThemeManager.CARD_BG;
                Color borderC = outOfStock ? (dm ? new Color(160, 50, 50)  : new Color(252, 165, 165))
                              : lowStock   ? (dm ? new Color(160, 110, 20) : new Color(253, 230, 138))
                              :               RetailThemeManager.BORDER;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(borderC);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(148, 152));
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ── image or icon placeholder ─────────────────────────────────────────
        JPanel imageArea = new JPanel(new GridBagLayout());
        imageArea.setOpaque(false);
        imageArea.setPreferredSize(new Dimension(60, 56));
        if (thumb != null) {
            JLabel imgLbl = new JLabel(thumb);
            imgLbl.setOpaque(false);
            imageArea.add(imgLbl);
        } else {
            // coloured circle with first letter
            JPanel circle = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    boolean dm = RetailThemeManager.getInstance().isDark();
                    Color liveAccent = outOfStock ? RetailThemeManager.DANGER
                                     : lowStock   ? RetailThemeManager.WARNING
                                     :              RetailThemeManager.ACCENT;
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(liveAccent.getRed(), liveAccent.getGreen(),
                                         liveAccent.getBlue(), dm ? 60 : 30));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            circle.setOpaque(false);
            circle.setPreferredSize(new Dimension(44, 44));
            String initial = p.getName() != null && !p.getName().isEmpty()
                ? String.valueOf(p.getName().charAt(0)).toUpperCase() : "?";
            JLabel initLbl = new JLabel(initial);
            initLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
            initLbl.setForeground(accentColor);
            circle.add(initLbl);
            imageArea.add(circle);
        }

        // ── text block ────────────────────────────────────────────────────────
        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);

        JLabel nameLabel = new JLabel("<html><b>" + escHtml(p.getName()) + "</b></html>");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(RetailThemeManager.TEXT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel catLabel = new JLabel(catName.isEmpty() ? " " : catName);
        catLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        catLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        catLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel("KES " + String.format("%.0f", p.getSellingPrice()));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        priceLabel.setForeground(RetailThemeManager.PRIMARY);
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // stock pill
        String stockTxt = outOfStock ? "Out of stock"
                        : lowStock   ? p.getCurrentStock() + " " + p.getUnit() + " (low)"
                        :              p.getCurrentStock() + " " + p.getUnit();
        JLabel stockLabel = new JLabel(stockTxt) {
            @Override protected void paintComponent(Graphics g) {
                boolean dm = RetailThemeManager.getInstance().isDark();
                Color liveAccent = outOfStock ? RetailThemeManager.DANGER
                                 : lowStock   ? RetailThemeManager.WARNING
                                 :              RetailThemeManager.ACCENT;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(liveAccent.getRed(), liveAccent.getGreen(),
                                     liveAccent.getBlue(), dm ? 55 : 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        stockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        // Foreground is set dynamically via FlatLaf theme propagation; use accent as a fallback
        stockLabel.setForeground(accentColor);
        stockLabel.setOpaque(false);
        stockLabel.setBorder(new EmptyBorder(1, 5, 1, 5));
        stockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBlock.add(nameLabel);
        textBlock.add(Box.createVerticalStrut(2));
        textBlock.add(catLabel);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(priceLabel);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(stockLabel);

        card.add(imageArea,  BorderLayout.WEST);
        card.add(textBlock,  BorderLayout.CENTER);

        // ── hover effect ──────────────────────────────────────────────────────
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(RetailThemeManager.PRIMARY, 2, true),
                    new EmptyBorder(7, 7, 7, 7)));
                card.repaint();
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBorder(new EmptyBorder(8, 8, 8, 8));
                card.repaint();
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (outOfStock) {
                    stockWarningLabel.setForeground(RetailThemeManager.DANGER);
                    stockWarningLabel.setText("Out of stock: " + p.getName());
                } else {
                    addToCart(p);
                    stockWarningLabel.setForeground(RetailThemeManager.ACCENT);
                    stockWarningLabel.setText("Added: " + p.getName());
                }
            }
        });
        return card;
    }

    private ImageIcon productImage(String imagePaths, int width, int height) {
        if (imagePaths == null || imagePaths.isBlank()) return null;
        try {
            String firstPath = imagePaths.split(";")[0];
            Image image = javax.imageio.ImageIO.read(new java.io.File(firstPath));
            return image == null ? null : new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (Exception ignored) { return null; }
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    /** A FlowLayout that wraps rows properly inside a scroll pane. */
    private static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container target) {
            synchronized (target.getTreeLock()) {
                Dimension dim = super.preferredLayoutSize(target);
                int targetWidth = target.getParent() != null
                    ? target.getParent().getWidth() : target.getWidth();
                if (targetWidth == 0) return dim;
                int nmembers = target.getComponentCount();
                int x = 0; int y = 0; int rowH = 0;
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - getHgap() * 2;
                for (int i = 0; i < nmembers; i++) {
                    Component c = target.getComponent(i);
                    if (!c.isVisible()) continue;
                    Dimension d = c.getPreferredSize();
                    if (x + d.width > maxWidth) { x = 0; y += rowH + getVgap(); rowH = 0; }
                    x += d.width + getHgap();
                    rowH = Math.max(rowH, d.height);
                }
                return new Dimension(targetWidth, insets.top + insets.bottom + y + rowH + getVgap() * 2);
            }
        }
    }


    public void focusSearch() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    private void sortProductGrid(String sortBy) {
        List<Product> filtered = activeCategoryId == null ? allProducts
            : allProducts.stream()
                .filter(p -> activeCategoryId.equals(p.getCategoryId()))
                .collect(java.util.stream.Collectors.toList());

        switch (sortBy) {
            case "Name":
                filtered.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "Price (Low-High)":
                filtered.sort(Comparator.comparing(Product::getSellingPrice));
                break;
            case "Price (High-Low)":
                filtered.sort(Comparator.comparing(Product::getSellingPrice).reversed());
                break;
            case "Stock":
                filtered.sort(Comparator.comparing(Product::getCurrentStock).reversed());
                break;
        }
        renderProductGrid(filtered);
    }

    private void clearFilters() {
        if (searchDebounce != null) searchDebounce.stop();
        searchField.setText("");
        activeCategoryId = null;
        // Reset category button styles
        for (Component comp : categoryPanel.getComponents()) {
            if (comp instanceof JButton) {
                comp.setBackground(RetailThemeManager.SURFACE);
                comp.setForeground(RetailThemeManager.TEXT);
            }
        }
        // Highlight "All" button
        if (categoryPanel.getComponentCount() > 0) {
            Component first = categoryPanel.getComponent(0);
            if (first instanceof JButton) {
                first.setBackground(RetailThemeManager.PRIMARY);
                first.setForeground(Color.WHITE);
            }
        }
        renderProductGrid(allProducts);
        stockWarningLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        stockWarningLabel.setText(allProducts.size() + " products available");
    }
}
