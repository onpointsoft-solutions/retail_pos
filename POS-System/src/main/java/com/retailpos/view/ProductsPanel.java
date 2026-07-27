package com.retailpos.view;

import com.retailpos.model.Category;
import com.retailpos.model.Product;
import com.retailpos.model.Supplier;
import com.retailpos.repository.CategoryRepository;
import com.retailpos.repository.SupplierRepository;
import com.retailpos.service.AuthService;
import com.retailpos.service.ProductService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import com.retailpos.util.BarcodeUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductsPanel extends JPanel {

    // Standard unit-of-measure options
    public static final String[] UNITS = {
        "pcs", "kg", "g", "litres", "ml", "bars",
        "boxes", "dozens", "metres", "pairs", "sachets",
        "packets", "bottles", "cans", "cartons", "rolls"
    };

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private List<Category> categories;
    private List<Supplier> suppliers;
    // Quick-lookup maps: id -> name for display in table
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, String> supplierNames = new HashMap<>();

    private int currentPage = 0;
    private static final int PAGE_SIZE = 100;
    private final ProductService productService = ProductService.getInstance();
    private Timer searchDebounce;

    public ProductsPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        buildUI();
        loadReferenceData();   // loads categories + suppliers, then products
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        // ── Toolbar ──────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        searchField = RetailThemeManager.styledField();
        searchField.setPreferredSize(new Dimension(300, 42));
        searchField.putClientProperty("JTextField.placeholderText", "Search name, barcode, SKU…");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("All Categories");
        categoryFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        categoryFilter.setPreferredSize(new Dimension(190, 42));
        categoryFilter.addActionListener(e -> { currentPage = 0; loadData(); });
        JButton clearFiltersBtn = RetailThemeManager.secondaryButton("Clear Filters");
        clearFiltersBtn.addActionListener(e -> clearFilters());
        left.add(new JLabel(Icons.get("search", 20)));
        left.add(searchField);
        left.add(categoryFilter);
        left.add(clearFiltersBtn);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        JButton addBtn     = RetailThemeManager.primaryButton("Add Product", "add");
        JButton editBtn    = RetailThemeManager.secondaryButton("Edit", "edit");
        JButton deleteBtn  = RetailThemeManager.dangerButton("Delete Selected", "delete");
        JButton barcodeBtn = RetailThemeManager.secondaryButton("Barcode/QR", "barcode");
        JButton prevBtn    = RetailThemeManager.secondaryButton("Prev");
        JButton nextBtn    = RetailThemeManager.secondaryButton("Next");

        addBtn.addActionListener(e -> showProductForm(null));
        editBtn.addActionListener(e -> {
            Product p = getSelectedProduct();
            if (p != null) showProductForm(p);
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        barcodeBtn.addActionListener(e -> {
            Product p = getSelectedProduct();
            if (p != null) showBarcodeDialog(p);
        });
        prevBtn.addActionListener(e -> { if (currentPage > 0) { currentPage--; loadData(); } });
        nextBtn.addActionListener(e -> { currentPage++; loadData(); });

        right.add(barcodeBtn); right.add(prevBtn); right.add(nextBtn);
        right.add(addBtn); right.add(editBtn); right.add(deleteBtn);
        toolbar.add(right, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"Image", "Name", "SKU", "Barcode", "Category", "Unit",
                         "Buy Price", "Sell Price", "Stock", "Min", "Supplier", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Icon.class : Object.class; }
        };
        table = RetailThemeManager.styledTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowHeight(56);
        table.getColumnModel().getColumn(0).setPreferredWidth(62);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
        table.getColumnModel().getColumn(7).setPreferredWidth(60);
        table.getColumnModel().getColumn(8).setPreferredWidth(50);
        table.getColumnModel().getColumn(9).setPreferredWidth(120);
        table.getColumnModel().getColumn(10).setPreferredWidth(65);
        // Highlight low-stock rows
        table.setDefaultRenderer(Object.class, new LowStockRenderer());
        add(RetailThemeManager.scroll(table), BorderLayout.CENTER);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadReferenceData() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                categories = new CategoryRepository().findAll();
                suppliers  = new SupplierRepository().findAll();
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    categoryNames.clear();
                    categoryFilter.removeAllItems();
                    categoryFilter.addItem("All Categories");
                    for (Category c : categories) {
                        categoryNames.put(c.getId(), c.getName());
                        categoryFilter.addItem(c.getName());
                    }
                    supplierNames.clear();
                    for (Supplier s : suppliers) supplierNames.put(s.getId(), s.getName());
                    loadData();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProductsPanel.this,
                        "Failed to load reference data: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void scheduleSearch() {
        if (searchDebounce != null) searchDebounce.stop();
        searchDebounce = new Timer(150, e -> { currentPage = 0; loadData(); });
        searchDebounce.setRepeats(false);
        searchDebounce.start();
    }

    private void clearFilters() {
        if (searchDebounce != null) searchDebounce.stop();
        searchField.setText("");
        if (categoryFilter.getItemCount() > 0) categoryFilter.setSelectedIndex(0);
        currentPage = 0;
        loadData();
    }

    private void loadData() {
        String query   = searchField.getText().trim();
        String catSel  = (String) categoryFilter.getSelectedItem();
        String catId   = null;
        if (catSel != null && !catSel.equals("All Categories") && categories != null) {
            for (Category c : categories) {
                if (c.getName().equals(catSel)) { catId = c.getId(); break; }
            }
        }
        final String finalCatId = catId;

        new SwingWorker<List<Product>, Void>() {
            @Override protected List<Product> doInBackground() throws Exception {
                if (!query.isEmpty()) return productService.search(query);
                if (finalCatId != null) return new com.retailpos.repository.ProductRepository()
                    .findByCategoryId(finalCatId, PAGE_SIZE, currentPage * PAGE_SIZE);
                return productService.getAll(PAGE_SIZE, currentPage * PAGE_SIZE);
            }
            @Override protected void done() {
                try {
                    tableModel.setRowCount(0);
                    for (Product p : get()) {
                        String catName = categoryNames.getOrDefault(p.getCategoryId(), p.getCategoryId() != null ? "?" : "");
                        String supName = supplierNames.getOrDefault(p.getSupplierId(), "");
                        tableModel.addRow(new Object[]{
                            productThumbnail(p.getImagePath()),
                            p.getName(),
                            p.getSku(),
                            p.getBarcode() != null ? p.getBarcode() : "",
                            catName,
                            p.getUnit(),
                            String.format("%.2f", p.getBuyingPrice()),
                            String.format("%.2f", p.getSellingPrice()),
                            p.getCurrentStock(),
                            p.getMinimumStock(),
                            supName,
                            p.getStatus()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProductsPanel.this,
                        "Load error: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ── Selection helper ──────────────────────────────────────────────────────

    private Product getSelectedProduct() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first");
            return null;
        }
        return getProductAtModelRow(table.convertRowIndexToModel(row));
    }

    private Product getProductAtModelRow(int modelRow) {
        String sku = (String) tableModel.getValueAt(modelRow, 2);
        try {
            return productService.search(sku).stream()
                .filter(p -> sku.equals(p.getSku())).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private List<Product> getSelectedProducts() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) return java.util.Collections.emptyList();
        java.util.List<Product> selectedProducts = new java.util.ArrayList<>();
        java.util.Set<String> seenIds = new java.util.LinkedHashSet<>();
        for (int row : selectedRows) {
            Product product = getProductAtModelRow(table.convertRowIndexToModel(row));
            if (product != null && seenIds.add(product.getId())) selectedProducts.add(product);
        }
        return selectedProducts;
    }

    private Icon productThumbnail(String imagePaths) {
        if (imagePaths == null || imagePaths.isBlank()) return null;
        try {
            Image image = javax.imageio.ImageIO.read(new java.io.File(imagePaths.split(";")[0]));
            return image == null ? null : new ImageIcon(image.getScaledInstance(44, 44, Image.SCALE_SMOOTH));
        } catch (Exception ignored) { return null; }
    }

    private void deleteSelected() {
        List<Product> selectedProducts = getSelectedProducts();
        if (selectedProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select one or more products first");
            return;
        }
        String prompt = selectedProducts.size() == 1
            ? "Delete '" + selectedProducts.get(0).getName() + "'?"
            : "Delete " + selectedProducts.size() + " selected products?";
        int r = JOptionPane.showConfirmDialog(this,
            prompt, "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String userId = AuthService.getInstance().getCurrentUser().getId();
                for (Product product : selectedProducts) {
                    productService.deleteProduct(product.getId(), userId);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    loadData();
                    JOptionPane.showMessageDialog(ProductsPanel.this,
                        selectedProducts.size() + " product" + (selectedProducts.size() == 1 ? "" : "s") + " deleted.");
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(ProductsPanel.this,
                        "Delete failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ── Barcode / QR dialog ───────────────────────────────────────────────────

    private void showBarcodeDialog(Product p) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Barcode / QR — " + p.getName(), true);
        d.setSize(440, 340); d.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setBackground(Color.WHITE);

        JLabel barLabel = new JLabel("No barcode set", SwingConstants.CENTER);
        JLabel qrLabel  = new JLabel("No QR set",     SwingConstants.CENTER);
        barLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        qrLabel.setFont(new Font("Segoe UI",  Font.ITALIC, 12));

        if (p.getBarcode() != null && !p.getBarcode().isBlank()) {
            try { barLabel.setIcon(new ImageIcon(BarcodeUtil.generateBarcode(p.getBarcode(), 300, 60)));
                  barLabel.setText("");
            } catch (Exception ignored) { barLabel.setText("Barcode: " + p.getBarcode()); }
        }
        if (p.getSku() != null && !p.getSku().isBlank()) {
            try { qrLabel.setIcon(new ImageIcon(BarcodeUtil.generateQRCode(p.getSku(), 120)));
                  qrLabel.setText("");
            } catch (Exception ignored) { qrLabel.setText("QR: " + p.getSku()); }
        }

        JPanel imgs = new JPanel(new GridLayout(1, 2, 12, 0));
        imgs.setOpaque(false); imgs.add(barLabel); imgs.add(qrLabel);
        JLabel title = new JLabel(p.getName() + "  |  " + p.getSku(), SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);
        panel.add(imgs, BorderLayout.CENTER);
        JButton close = RetailThemeManager.primaryButton("Close");
        close.addActionListener(e -> d.dispose());
        panel.add(close, BorderLayout.SOUTH);
        d.setContentPane(panel); d.setVisible(true);
    }

    // ── Product form ──────────────────────────────────────────────────────────

    private void showProductForm(Product existing) {
        boolean isNew = (existing == null);
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add Product" : "Edit Product", true);
        d.setSize(600, 720); d.setLocationRelativeTo(this); d.setResizable(true);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        g.insets = new Insets(5, 0, 5, 0);

        // Text fields
        JTextField nameF    = formField(form, g, 0,  "Product Name *");
        JTextField skuF     = formField(form, g, 2,  "SKU *");
        JTextField barcodeF = formField(form, g, 4,  "Barcode (optional)");
        JTextField buyF     = formField(form, g, 6,  "Buying Price *");
        JTextField sellF    = formField(form, g, 8,  "Selling Price *");
        JTextField whlF     = formField(form, g, 10, "Wholesale Price");
        JTextField stockF   = formField(form, g, 12, "Current Stock");
        JTextField minF     = formField(form, g, 14, "Minimum Stock");
        JTextField preferredOrderF = formField(form, g, 16, "Preferred Order Quantity");
        JTextField taxF     = formField(form, g, 18, "Tax Rate (%)");
        JTextField descF    = formField(form, g, 20, "Description");

        // Unit combo
        JComboBox<String> unitCombo = formCombo(form, g, 22, "Unit of Measure", UNITS);

        // Category combo
        JComboBox<String> catCombo = new JComboBox<>();
        catCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        catCombo.setPreferredSize(new Dimension(400, 42));
        catCombo.addItem("-- Select Category --");
        if (categories != null) for (Category c : categories) catCombo.addItem(c.getName());
        g.gridx = 0; g.gridy = 24; g.weightx = 0;
        JLabel cl = new JLabel("Category:");
        cl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(cl, g);
        g.gridy = 25; g.weightx = 1; form.add(catCombo, g);

        // Supplier combo
        JComboBox<String> supCombo = new JComboBox<>();
        supCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        supCombo.setPreferredSize(new Dimension(400, 42));
        supCombo.addItem("-- No Supplier --");
        if (suppliers != null) for (Supplier s : suppliers) supCombo.addItem(s.getName());
        g.gridx = 0; g.gridy = 26; g.weightx = 0;
        JLabel sl = new JLabel("Supplier:");
        sl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(sl, g);
        g.gridy = 27; g.weightx = 1; form.add(supCombo, g);

        // Track expiry checkbox
        JCheckBox expiryBox = new JCheckBox("Track expiry dates / batch numbers");
        expiryBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        expiryBox.setBackground(Color.WHITE);
        g.gridx = 0; g.gridy = 28; g.weightx = 1;
        g.insets = new Insets(10, 0, 5, 0);
        form.add(expiryBox, g);
        g.insets = new Insets(5, 0, 5, 0);

        JTextField imagePathF = RetailThemeManager.styledField();
        imagePathF.setEditable(false);
        JButton uploadImageBtn = RetailThemeManager.secondaryButton("Upload product image");
        uploadImageBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp"));
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(d) != JFileChooser.APPROVE_OPTION) return;
            try {
                java.util.List<String> imagePaths = new java.util.ArrayList<>();
                if (!imagePathF.getText().isBlank()) imagePaths.addAll(java.util.Arrays.asList(imagePathF.getText().split(";")));
                for (java.io.File selectedFile : chooser.getSelectedFiles()) {
                    imagePaths.add(com.retailpos.util.AppPaths.copyImage(selectedFile.toPath(), "product").toString());
                }
                imagePathF.setText(String.join(";", imagePaths));
            }
            catch (Exception ex) { JOptionPane.showMessageDialog(d, "Image upload failed: " + ex.getMessage(), "Upload error", JOptionPane.ERROR_MESSAGE); }
        });
        g.gridx = 0; g.gridy = 30; g.weightx = 1; form.add(uploadImageBtn, g);
        g.gridy = 31; form.add(imagePathF, g);

        // Pre-fill for edit
        if (existing != null) {
            nameF.setText(nvl(existing.getName()));
            skuF.setText(nvl(existing.getSku()));
            barcodeF.setText(nvl(existing.getBarcode()));
            buyF.setText(fmt(existing.getBuyingPrice()));
            sellF.setText(fmt(existing.getSellingPrice()));
            whlF.setText(fmt(existing.getWholesalePrice()));
            stockF.setText(String.valueOf(existing.getCurrentStock()));
            minF.setText(String.valueOf(existing.getMinimumStock()));
            preferredOrderF.setText(String.valueOf(existing.getPreferredOrderQuantity()));
            taxF.setText(fmt(existing.getTaxRate()));
            descF.setText(nvl(existing.getDescription()));
            imagePathF.setText(nvl(existing.getImagePath()));
            expiryBox.setSelected(existing.isTrackExpiry());
            selectComboByValue(unitCombo, existing.getUnit());
            if (categories != null && existing.getCategoryId() != null) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId().equals(existing.getCategoryId())) {
                        catCombo.setSelectedIndex(i + 1); break;
                    }
                }
            }
            if (suppliers != null && existing.getSupplierId() != null) {
                for (int i = 0; i < suppliers.size(); i++) {
                    if (suppliers.get(i).getId().equals(existing.getSupplierId())) {
                        supCombo.setSelectedIndex(i + 1); break;
                    }
                }
            }
        } else {
            stockF.setText("0"); minF.setText("0"); preferredOrderF.setText("0");
            buyF.setText("0.00"); sellF.setText("0.00");
            whlF.setText("0.00"); taxF.setText("16.0");
        }

        // Error label (top)
        JLabel errorLbl = new JLabel(" ");
        errorLbl.setForeground(RetailThemeManager.DANGER);
        errorLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        errorLbl.setBorder(new EmptyBorder(4, 24, 4, 24));

        d.add(errorLbl, BorderLayout.NORTH);
        d.add(new JScrollPane(form), BorderLayout.CENTER);

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(RetailThemeManager.SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));
        JButton cancelBtn = RetailThemeManager.secondaryButton("Cancel");
        JButton saveBtn   = RetailThemeManager.primaryButton("Save Product");
        saveBtn.setPreferredSize(new Dimension(160, 44));
        cancelBtn.addActionListener(e -> d.dispose());

        saveBtn.addActionListener(e -> {
            String name    = nameF.getText().trim();
            String sku     = skuF.getText().trim();
            String barcode = barcodeF.getText().trim();
            String sellTxt = sellF.getText().trim();
            if (name.isEmpty())    { errorLbl.setText("Product name is required"); return; }
            if (sellTxt.isEmpty()) { errorLbl.setText("Selling price is required"); return; }

            Product p = existing != null ? existing : new Product();
            try {
                p.setName(name); p.setSku(sku.isEmpty() ? productService.generateSku(name) : sku);
                p.setBarcode(barcode.isEmpty() ? null : barcode);
                p.setQrCode(null);
                p.setBuyingPrice(buyF.getText().isBlank() ? 0 : Double.parseDouble(buyF.getText().trim()));
                p.setSellingPrice(Double.parseDouble(sellTxt));
                p.setWholesalePrice(whlF.getText().isBlank() ? 0 : Double.parseDouble(whlF.getText().trim()));
                p.setCurrentStock(stockF.getText().isBlank() ? 0 : Integer.parseInt(stockF.getText().trim()));
                p.setMinimumStock(minF.getText().isBlank() ? 0 : Integer.parseInt(minF.getText().trim()));
                p.setPreferredOrderQuantity(preferredOrderF.getText().isBlank() ? 0 : Integer.parseInt(preferredOrderF.getText().trim()));
                p.setTaxRate(taxF.getText().isBlank() ? 0 : Double.parseDouble(taxF.getText().trim()));
                p.setDescription(descF.getText().trim().isEmpty() ? null : descF.getText().trim());
                p.setImagePath(imagePathF.getText().trim().isEmpty() ? null : imagePathF.getText().trim());
                p.setUnit((String) unitCombo.getSelectedItem());
                p.setStatus("active");
                p.setTrackExpiry(expiryBox.isSelected());
                // Category
                int ci = catCombo.getSelectedIndex();
                p.setCategoryId(ci > 0 && categories != null && ci - 1 < categories.size()
                    ? categories.get(ci - 1).getId() : null);
                // Supplier
                int si = supCombo.getSelectedIndex();
                p.setSupplierId(si > 0 && suppliers != null && si - 1 < suppliers.size()
                    ? suppliers.get(si - 1).getId() : null);
                errorLbl.setText(" ");
            } catch (NumberFormatException ex) {
                errorLbl.setText("Invalid number: " + ex.getMessage()); return;
            }

            saveBtn.setEnabled(false); saveBtn.setText("Saving…");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    productService.saveProduct(p, AuthService.getInstance().getCurrentUser().getId());
                    return null;
                }
                @Override protected void done() {
                    saveBtn.setEnabled(true); saveBtn.setText("Save Product");
                    try {
                        get(); d.dispose(); loadData();
                        JOptionPane.showMessageDialog(ProductsPanel.this,
                            isNew ? "Product added successfully." : "Product updated.");
                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        errorLbl.setText(msg != null ? msg : "Save failed");
                        JOptionPane.showMessageDialog(d, "Save failed:\n" + msg,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        footer.add(cancelBtn); footer.add(saveBtn);
        d.add(footer, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(saveBtn);
        d.setVisible(true);
    }

    // ── Form helpers ──────────────────────────────────────────────────────────

    private JTextField formField(JPanel p, GridBagConstraints g, int row, String label) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(l, g);
        g.gridy = row + 1; g.weightx = 1;
        JTextField f = RetailThemeManager.styledField();
        f.setPreferredSize(new Dimension(400, 42));
        p.add(f, g);
        return f;
    }

    private JComboBox<String> formCombo(JPanel p, GridBagConstraints g,
                                         int row, String label, String[] items) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(l, g);
        g.gridy = row + 1; g.weightx = 1;
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(400, 42));
        p.add(cb, g);
        return cb;
    }

    private void selectComboByValue(JComboBox<String> cb, String value) {
        if (value == null) return;
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (value.equals(cb.getItemAt(i))) { cb.setSelectedIndex(i); return; }
        }
    }

    private static String nvl(String s) { return s != null ? s : ""; }
    private static String fmt(double v) { return String.format("%.2f", v); }

    // ── Low-stock row renderer ────────────────────────────────────────────────

    private static class LowStockRenderer extends javax.swing.table.DefaultTableCellRenderer {
        private static final Color LOW   = new Color(255, 243, 205);  // amber tint
        private static final Color ZERO  = new Color(255, 220, 220);  // red tint
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focused, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, col);
            if (!selected) {
                try {
                    int stock = Integer.parseInt(t.getValueAt(row, 8).toString());
                    int min   = Integer.parseInt(t.getValueAt(row, 9).toString());
                    if (stock == 0)          c.setBackground(ZERO);
                    else if (stock <= min)   c.setBackground(LOW);
                    else                     c.setBackground(Color.WHITE);
                } catch (Exception ignored) { c.setBackground(Color.WHITE); }
            }
            return c;
        }
    }
}
