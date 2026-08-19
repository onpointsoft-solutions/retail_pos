package com.retailpos.view;

import com.retailpos.model.*;
import com.retailpos.repository.*;
import com.retailpos.service.AuthService;
import com.retailpos.service.InventoryService;
import com.retailpos.service.ProductService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Optional;
import java.util.List;

/**
 * Professional Purchase Orders panel.
 * - Lists all POs with status colour-coding
 * - Full PO creation dialog: supplier, delivery date, notes, product lines with search
 * - Receive dialog: shows all line items, supports partial delivery per line
 * - View detail dialog for any existing PO
 */
public class PurchasesPanel extends JPanel implements com.retailpos.ui.Refreshable {

    // Status badge colours
    private static final Map<String, Color> STATUS_COLORS = Map.of(
        "ORDERED",            new Color(59, 130, 246),   // blue
        "PARTIALLY_RECEIVED", new Color(245, 158, 11),   // amber
        "RECEIVED",           new Color(22, 163, 74),    // green
        "CANCELLED",          new Color(156, 163, 175)   // grey
    );

    private DefaultTableModel tableModel;
    private JTable table;
    private List<PurchaseOrder> poList = new ArrayList<>();

    private final PurchaseOrderRepository poRepo    = new PurchaseOrderRepository();
    private final SupplierRepository      supRepo   = new SupplierRepository();
    private final ProductService          prodSvc   = ProductService.getInstance();
    private final InventoryService        invSvc    = InventoryService.getInstance();

    public PurchasesPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        buildUI();
        loadData();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JButton newPO    = RetailThemeManager.primaryButton("New PO", "add");
        JButton viewBtn  = RetailThemeManager.secondaryButton("View / Edit", "edit");
        JButton receiveBtn = RetailThemeManager.successButton("Receive Stock", "check");
        JButton cancelBtn  = RetailThemeManager.dangerButton("Cancel PO", "delete");
        JButton refreshBtn = RetailThemeManager.secondaryButton("Refresh", "refresh");

        newPO.addActionListener(e -> showNewPODialog());
        viewBtn.addActionListener(e -> { PurchaseOrder po = getSelected(); if (po != null) showViewDialog(po); });
        receiveBtn.addActionListener(e -> { PurchaseOrder po = getSelected(); if (po != null) showReceiveDialog(po); });
        cancelBtn.addActionListener(e -> cancelSelected());
        refreshBtn.addActionListener(e -> loadData());

        toolbar.add(newPO); toolbar.add(viewBtn);
        toolbar.add(receiveBtn); toolbar.add(cancelBtn); toolbar.add(refreshBtn);
        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"PO #", "Supplier", "Status", "Lines", "Total (KES)", "Expected", "Created"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row) && col == 2) {
                    String status = (String) getValueAt(row, col);
                    Color bg = STATUS_COLORS.getOrDefault(status, Color.WHITE);
                    c.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 40));
                    c.setForeground(bg.darker());
                    ((JLabel) c).setFont(((JLabel) c).getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        };
        table.setRowHeight(42);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBackground(RetailThemeManager.SURFACE);
        table.setShowVerticalLines(false);
        table.setGridColor(RetailThemeManager.BORDER);
        table.setSelectionBackground(RetailThemeManager.getInstance().selectionBg());
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(55);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    PurchaseOrder po = getSelected();
                    if (po != null) showViewDialog(po);
                }
            }
        });
        add(RetailThemeManager.scroll(table), BorderLayout.CENTER);

        // Summary bar
        JLabel hint = new JLabel("Double-click a row to view details. Select a row then click Receive Stock to update inventory.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(RetailThemeManager.TEXT_MUTED);
        add(hint, BorderLayout.SOUTH);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadData() {
        new SwingWorker<List<PurchaseOrder>, Void>() {
            @Override protected List<PurchaseOrder> doInBackground() throws Exception {
                return poRepo.findAll();
            }
            @Override protected void done() {
                try {
                    poList = get();
                    tableModel.setRowCount(0);
                    for (PurchaseOrder po : poList) {
                        tableModel.addRow(new Object[]{
                            po.getId().substring(0, 8).toUpperCase(),
                            po.getSupplierName() != null ? po.getSupplierName() : "",
                            po.getStatus(),
                            po.getItems().size(),
                            String.format("%.2f", po.getTotal()),
                            po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate().toString() : "",
                            po.getCreatedAt() != null ? po.getCreatedAt().toLocalDate().toString() : ""
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PurchasesPanel.this,
                        "Failed to load POs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    @Override public void refreshData() { loadData(); }
    @Override public int getRefreshIntervalSeconds() { return 120; }
    @Override public String getPanelDescription() { return "Purchases — purchase orders"; }

    private PurchaseOrder getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a purchase order first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        return modelRow < poList.size() ? poList.get(modelRow) : null;
    }

    // ── Cancel PO ─────────────────────────────────────────────────────────────

    private void cancelSelected() {
        PurchaseOrder po = getSelected();
        if (po == null) return;
        if ("RECEIVED".equals(po.getStatus())) {
            JOptionPane.showMessageDialog(this, "Cannot cancel a fully received purchase order.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
            "Cancel PO from '" + po.getSupplierName() + "'?\nThis cannot be undone.",
            "Cancel PO", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                poRepo.updateStatus(po.getId(), "CANCELLED");
                return null;
            }
            @Override protected void done() {
                try { get(); loadData(); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(PurchasesPanel.this, "Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ── NEW PO DIALOG ─────────────────────────────────────────────────────────

    private void showNewPODialog() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                return new Object[]{ supRepo.findAll(), prodSvc.getAllActive() };
            }
            @Override @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] data = get();
                    List<Supplier> suppliers = (List<Supplier>) data[0];
                    List<Product>  products  = (List<Product>)  data[1];

                    if (suppliers.isEmpty()) {
                        JOptionPane.showMessageDialog(PurchasesPanel.this,
                            "No suppliers found. Add suppliers in the Suppliers tab first.");
                        return;
                    }

                    JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(PurchasesPanel.this),
                        "New Purchase Order", true);
                    d.setSize(740, 620); d.setLocationRelativeTo(PurchasesPanel.this);
                    d.setResizable(true);

                    // ── Header form ───────────────────────────────────────────
                    JPanel header = new JPanel(new GridBagLayout());
                    header.setBackground(RetailThemeManager.CARD_BG);
                    header.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(RetailThemeManager.BORDER),
                        new EmptyBorder(14, 16, 14, 16)));
                    GridBagConstraints g = new GridBagConstraints();
                    g.insets = new Insets(5, 6, 5, 6); g.fill = GridBagConstraints.HORIZONTAL;

                    // Row 1: Supplier + Delivery Date
                    g.gridx = 0; g.gridy = 0; g.weightx = 0;
                    header.add(bold("Supplier: *"), g);
                    g.gridx = 1; g.weightx = 1;
                    JComboBox<String> supCombo = new JComboBox<>(
                        suppliers.stream().map(Supplier::getName).toArray(String[]::new));
                    supCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    supCombo.setPreferredSize(new Dimension(200, 40));
                    header.add(supCombo, g);

                    g.gridx = 2; g.weightx = 0;
                    header.add(bold("Expected Delivery:"), g);
                    g.gridx = 3; g.weightx = 0.5;
                    JTextField deliveryF = RetailThemeManager.styledField();
                    deliveryF.setPreferredSize(new Dimension(130, 40));
                    deliveryF.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
                    LocalDate nextWeek = LocalDate.now().plusDays(7);
                    deliveryF.setText(nextWeek.toString());
                    header.add(deliveryF, g);

                    // Row 2: Notes
                    g.gridx = 0; g.gridy = 1; g.weightx = 0;
                    header.add(bold("Notes:"), g);
                    g.gridx = 1; g.gridwidth = 3; g.weightx = 1;
                    JTextField notesF = RetailThemeManager.styledField();
                    notesF.putClientProperty("JTextField.placeholderText", "Optional notes for this order");
                    header.add(notesF, g);
                    g.gridwidth = 1;

                    // ── Line items table ──────────────────────────────────────
                    String[] itemCols = {"Product *", "Unit", "Qty *", "Unit Cost (KES) *", "Line Total"};
                    DefaultTableModel itemModel = new DefaultTableModel(itemCols, 0) {
                        @Override public boolean isCellEditable(int r, int c) { return c < 4; }
                    };
                    JTable itemTable = new JTable(itemModel);
                    itemTable.setRowHeight(36);
                    itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
                    itemTable.getColumnModel().getColumn(0).setPreferredWidth(220);
                    itemTable.getColumnModel().getColumn(1).setPreferredWidth(70);
                    itemTable.getColumnModel().getColumn(2).setPreferredWidth(60);
                    itemTable.getColumnModel().getColumn(3).setPreferredWidth(130);
                    itemTable.getColumnModel().getColumn(4).setPreferredWidth(110);
                    itemTable.setShowVerticalLines(false);
                    itemTable.setGridColor(RetailThemeManager.BORDER);

                    // Product autocomplete via combobox editor on col 0
                    JComboBox<String> prodCombo = new JComboBox<>();
                    prodCombo.setEditable(true);
                    prodCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    for (Product p : products) prodCombo.addItem(p.getName() + "  [" + p.getSku() + "]");
                    itemTable.getColumnModel().getColumn(0)
                        .setCellEditor(new DefaultCellEditor(prodCombo));

                    // Auto-calc total when qty/price changes
                    itemModel.addTableModelListener(e -> {
                        if (e.getColumn() == 2 || e.getColumn() == 3) {
                            int row = e.getFirstRow();
                            if (row < 0 || row >= itemModel.getRowCount()) return;
                            try {
                                double qty   = Double.parseDouble(itemModel.getValueAt(row, 2).toString());
                                double price = Double.parseDouble(itemModel.getValueAt(row, 3).toString());
                                itemModel.setValueAt(String.format("%.2f", qty * price), row, 4);
                            } catch (Exception ignored) {}
                        }
                    });

                    // Add one empty row
                    itemModel.addRow(new Object[]{"", "pcs", "1", "0.00", "0.00"});

                    // Toolbar for items
                    JPanel itemToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
                    itemToolbar.setOpaque(false);
                    JButton addLine = RetailThemeManager.secondaryButton("+ Add Line");
                    JButton delLine = RetailThemeManager.dangerButton("Remove Line", "delete");
                    JLabel totalLbl = new JLabel("  Order Total: KES 0.00");
                    totalLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    totalLbl.setForeground(RetailThemeManager.PRIMARY);

                    addLine.addActionListener(ev -> itemModel.addRow(new Object[]{"", "pcs", "1", "0.00", "0.00"}));
                    delLine.addActionListener(ev -> {
                        int row = itemTable.getSelectedRow();
                        if (row >= 0) itemModel.removeRow(row);
                        updateOrderTotal(itemModel, totalLbl);
                    });
                    itemModel.addTableModelListener(ev -> updateOrderTotal(itemModel, totalLbl));
                    itemToolbar.add(addLine); itemToolbar.add(delLine); itemToolbar.add(totalLbl);

                    JPanel itemPanel = new JPanel(new BorderLayout(0, 4));
                    itemPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
                    itemPanel.setOpaque(false);
                    JLabel itemHeader = new JLabel("Line Items");
                    itemHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    itemPanel.add(itemHeader, BorderLayout.NORTH);
                    itemPanel.add(RetailThemeManager.scroll(itemTable), BorderLayout.CENTER);
                    itemPanel.add(itemToolbar, BorderLayout.SOUTH);

                    // ── Main layout ───────────────────────────────────────────
                    JPanel content = new JPanel(new BorderLayout(0, 8));
                    content.setBackground(RetailThemeManager.SURFACE);
                    content.setBorder(new EmptyBorder(12, 14, 12, 14));
                    content.add(header, BorderLayout.NORTH);
                    content.add(itemPanel, BorderLayout.CENTER);

                    // ── Error label ───────────────────────────────────────────
                    JLabel errLbl = new JLabel(" ");
                    errLbl.setForeground(RetailThemeManager.DANGER);
                    errLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                    errLbl.setBorder(new EmptyBorder(0, 14, 0, 14));
                    d.add(errLbl, BorderLayout.NORTH);
                    d.add(content, BorderLayout.CENTER);

                    // ── Footer buttons ────────────────────────────────────────
                    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
                    footer.setBackground(RetailThemeManager.SURFACE);
                    footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));
                    JButton cancelBtn2 = RetailThemeManager.secondaryButton("Cancel");
                    JButton saveBtn    = RetailThemeManager.primaryButton("Create Purchase Order");
                    saveBtn.setPreferredSize(new Dimension(220, 44));
                    cancelBtn2.addActionListener(ev -> d.dispose());

                    saveBtn.addActionListener(ev -> {
                        // Validate supplier
                        int supIdx = supCombo.getSelectedIndex();
                        if (supIdx < 0) { errLbl.setText("Select a supplier"); return; }
                        Supplier sup = suppliers.get(supIdx);

                        // Parse delivery date
                        LocalDate delDate = null;
                        try {
                            if (!deliveryF.getText().isBlank())
                                delDate = LocalDate.parse(deliveryF.getText().trim());
                        } catch (Exception ex) {
                            errLbl.setText("Invalid date format. Use YYYY-MM-DD.");
                            return;
                        }

                        // Build items
                        List<PurchaseOrder.PurchaseOrderItem> items = new ArrayList<>();
                        for (int i = 0; i < itemModel.getRowCount(); i++) {
                            String prodText = itemModel.getValueAt(i, 0).toString().trim();
                            if (prodText.isEmpty()) continue;
                            try {
                                int qty = Integer.parseInt(itemModel.getValueAt(i, 2).toString().trim());
                                double price = Double.parseDouble(itemModel.getValueAt(i, 3).toString().trim());
                                if (qty <= 0)    { errLbl.setText("Row " + (i+1) + ": quantity must be > 0"); return; }
                                if (price < 0)   { errLbl.setText("Row " + (i+1) + ": price cannot be negative"); return; }

                                // Resolve product
                                String pid = null, pname = prodText;
                                String skuInBracket = prodText.contains("[")
                                    ? prodText.replaceAll(".*\\[(.*)\\].*", "$1").trim() : prodText;
                                List<Product> found = prodSvc.search(skuInBracket);
                                if (!found.isEmpty()) { pid = found.get(0).getId(); pname = found.get(0).getName(); }

                                String unit = itemModel.getValueAt(i, 1).toString().trim();
                                items.add(new PurchaseOrder.PurchaseOrderItem(pid, pname, qty, price));
                            } catch (NumberFormatException ex) {
                                errLbl.setText("Row " + (i+1) + ": invalid quantity or price");
                                return;
                            }
                        }
                        if (items.isEmpty()) { errLbl.setText("Add at least one product line"); return; }

                        final LocalDate finalDelDate = delDate;
                        saveBtn.setEnabled(false); saveBtn.setText("Saving…");
                        new SwingWorker<Void, Void>() {
                            @Override protected Void doInBackground() throws Exception {
                                PurchaseOrder po = new PurchaseOrder();
                                po.setId(UUID.randomUUID().toString());
                                po.setSupplierId(sup.getId());
                                po.setSupplierName(sup.getName());
                                po.setStatus("ORDERED");
                                po.setItems(items);
                                po.setExpectedDeliveryDate(finalDelDate);
                                po.setNotes(notesF.getText().trim().isEmpty() ? null : notesF.getText().trim());
                                po.setSyncStatus("PENDING");
                                po.setCreatedAt(LocalDateTime.now());
                                po.setUpdatedAt(LocalDateTime.now());
                                poRepo.insert(po);
                                return null;
                            }
                            @Override protected void done() {
                                saveBtn.setEnabled(true); saveBtn.setText("Create Purchase Order");
                                try { get(); d.dispose(); loadData();
                                    JOptionPane.showMessageDialog(PurchasesPanel.this, "Purchase order created.");
                                } catch (Exception ex) {
                                    errLbl.setText("Save failed: " + ex.getMessage());
                                    JOptionPane.showMessageDialog(d, "Save failed:\n" + ex.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }.execute();
                    });

                    footer.add(cancelBtn2); footer.add(saveBtn);
                    d.add(footer, BorderLayout.SOUTH);
                    d.getRootPane().setDefaultButton(saveBtn);
                    d.setVisible(true);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PurchasesPanel.this,
                        "Error opening PO dialog: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void updateOrderTotal(DefaultTableModel m, JLabel lbl) {
        double total = 0;
        for (int i = 0; i < m.getRowCount(); i++) {
            try {
                total += Double.parseDouble(m.getValueAt(i, 4).toString());
            } catch (Exception ignored) {}
        }
        lbl.setText("  Order Total: KES " + String.format("%.2f", total));
    }

    // ── VIEW / DETAIL DIALOG ──────────────────────────────────────────────────

    private void showViewDialog(PurchaseOrder po) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "PO Detail — " + po.getId().substring(0, 8).toUpperCase(), true);
        d.setSize(640, 480); d.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(RetailThemeManager.CARD_BG);
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Header info
        JPanel info = new JPanel(new GridLayout(0, 2, 8, 6));
        info.setBackground(RetailThemeManager.CARD_BG);
        addInfoRow(info, "PO Number:",  po.getId().substring(0, 8).toUpperCase());
        addInfoRow(info, "Supplier:",   po.getSupplierName() != null ? po.getSupplierName() : "");
        addInfoRow(info, "Status:",     po.getStatus());
        addInfoRow(info, "Expected:",   po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate().toString() : "—");
        addInfoRow(info, "Created:",    po.getCreatedAt() != null ? po.getCreatedAt().toLocalDate().toString() : "");
        addInfoRow(info, "Notes:",      po.getNotes() != null ? po.getNotes() : "");
        panel.add(info, BorderLayout.NORTH);

        // Items table
        String[] cols = {"Product", "Ordered", "Received", "Outstanding", "Unit Cost", "Line Total"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (PurchaseOrder.PurchaseOrderItem item : po.getItems()) {
            m.addRow(new Object[]{
                item.getProductName(),
                item.getOrderedQty(),
                item.getReceivedQty(),
                item.getOutstandingQty(),
                String.format("%.2f", item.getBuyingPrice()),
                String.format("%.2f", item.getLineTotal())
            });
        }
        JTable itemTable = RetailThemeManager.styledTable(m);
        panel.add(RetailThemeManager.scroll(itemTable), BorderLayout.CENTER);

        // Total
        JLabel total = new JLabel("Order Total: KES " + String.format("%.2f", po.getTotal()),
            SwingConstants.RIGHT);
        total.setFont(new Font("Segoe UI", Font.BOLD, 15));
        total.setForeground(RetailThemeManager.PRIMARY);
        panel.add(total, BorderLayout.SOUTH);

        d.setContentPane(panel);
        JButton close = RetailThemeManager.primaryButton("Close");
        close.addActionListener(e -> d.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(close);
        d.add(foot, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void addInfoRow(JPanel p, String label, String value) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(RetailThemeManager.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(l); p.add(v);
    }

    // ── RECEIVE DIALOG ────────────────────────────────────────────────────────

    private void showReceiveDialog(PurchaseOrder po) {
        if ("RECEIVED".equals(po.getStatus())) {
            JOptionPane.showMessageDialog(this, "This PO has already been fully received.");
            return;
        }
        if ("CANCELLED".equals(po.getStatus())) {
            JOptionPane.showMessageDialog(this, "Cannot receive a cancelled PO.");
            return;
        }
        if (po.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, "This PO has no line items.");
            return;
        }

        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Receive Stock — PO " + po.getId().substring(0, 8).toUpperCase(), true);
        d.setSize(820, 560); d.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(RetailThemeManager.CARD_BG);
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Info header
        JLabel hdr = new JLabel("Supplier: " + po.getSupplierName()
            + "   |   PO Status: " + po.getStatus());
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hdr.setForeground(RetailThemeManager.TEXT);
        panel.add(hdr, BorderLayout.NORTH);

        // Receipt table columns:
        //   0 Product | 1 Ordered | 2 Already Received | 3 Outstanding
        //   4 Receiving Now * | 5 Cost Price | 6 Markup % | 7 Selling Price *
        String[] cols = {
            "Product", "Ordered", "Rcvd", "Outstanding",
            "Receiving Now *", "Cost (KES)", "Markup %", "Selling Price (KES) *"
        };
        DefaultTableModel receiveModel = new DefaultTableModel(cols, 0) {
            // columns 4 (Receiving Now), 6 (Markup %), 7 (Selling Price) are editable
            @Override public boolean isCellEditable(int r, int c) { return c == 4 || c == 6 || c == 7; }
            @Override public Class<?> getColumnClass(int c) {
                return (c >= 1 && c <= 4) ? Integer.class : String.class;
            }
        };

        // Pre-fetch current selling prices for each item
        List<Double> currentSellingPrices = new ArrayList<>();
        for (PurchaseOrder.PurchaseOrderItem item : po.getItems()) {
            double sp = 0;
            try {
                if (item.getProductId() != null) {
                    sp = prodSvc.findById(item.getProductId())
                                .map(com.retailpos.model.Product::getSellingPrice)
                                .orElse(0.0);
                }
            } catch (Exception ignored) {}
            currentSellingPrices.add(sp);
        }

        for (int i = 0; i < po.getItems().size(); i++) {
            PurchaseOrder.PurchaseOrderItem item = po.getItems().get(i);
            double cost = item.getBuyingPrice();
            double sp   = currentSellingPrices.get(i);
            // Compute current markup from stored prices (if cost > 0)
            double markup = (cost > 0 && sp > 0) ? ((sp - cost) / cost) * 100.0 : 0.0;
            receiveModel.addRow(new Object[]{
                item.getProductName(),
                item.getOrderedQty(),
                item.getReceivedQty(),
                item.getOutstandingQty(),
                item.getOutstandingQty(),          // Receiving Now — default all outstanding
                String.format("%.2f", cost),        // Cost Price (read-only display)
                String.format("%.1f", markup),      // Markup %  (editable)
                String.format("%.2f", sp)           // Selling Price (editable)
            });
        }

        JTable receiveTable = new JTable(receiveModel);
        receiveTable.setRowHeight(38);
        receiveTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        receiveTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        receiveTable.getTableHeader().setBackground(RetailThemeManager.getInstance().isDark()
            ? RetailThemeManager.NAVY : RetailThemeManager.SURFACE);
        receiveTable.setBackground(RetailThemeManager.CARD_BG);
        receiveTable.setForeground(RetailThemeManager.TEXT);
        receiveTable.setGridColor(RetailThemeManager.BORDER);
        receiveTable.setSelectionBackground(RetailThemeManager.getInstance().selectionBg());
        receiveTable.setSelectionForeground(RetailThemeManager.getInstance().selectionFg());
        receiveTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        receiveTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        receiveTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        receiveTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        receiveTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        receiveTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        receiveTable.getColumnModel().getColumn(6).setPreferredWidth(75);
        receiveTable.getColumnModel().getColumn(7).setPreferredWidth(140);
        receiveTable.setShowVerticalLines(false);

        // Highlight editable columns (4, 6, 7) with a tinted renderer
        javax.swing.table.DefaultTableCellRenderer editableRenderer =
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                    Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                    if (!sel) {
                        boolean dm = RetailThemeManager.getInstance().isDark();
                        comp.setBackground(dm
                            ? new Color(30, 58, 95)          // dark-mode tint
                            : new Color(219, 234, 254));      // light-mode tint
                        comp.setForeground(RetailThemeManager.TEXT);
                    }
                    return comp;
                }
            };
        receiveTable.getColumnModel().getColumn(4).setCellRenderer(editableRenderer);
        receiveTable.getColumnModel().getColumn(6).setCellRenderer(editableRenderer);
        receiveTable.getColumnModel().getColumn(7).setCellRenderer(editableRenderer);

        // When Markup % changes → auto-recalculate Selling Price
        // When Selling Price changes → auto-recalculate Markup %
        // Prevent re-entrant updates with a simple flag
        boolean[] updating = {false};
        receiveModel.addTableModelListener(e -> {
            if (updating[0]) return;
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || row >= receiveModel.getRowCount()) return;
            if (col == 6) {
                // Markup % changed → update Selling Price
                try {
                    double cost   = Double.parseDouble(receiveModel.getValueAt(row, 5).toString());
                    double markup = Double.parseDouble(receiveModel.getValueAt(row, 6).toString());
                    double sp     = cost * (1 + markup / 100.0);
                    updating[0] = true;
                    receiveModel.setValueAt(String.format("%.2f", sp), row, 7);
                    updating[0] = false;
                } catch (Exception ignored) {}
            } else if (col == 7) {
                // Selling Price changed → update Markup %
                try {
                    double cost = Double.parseDouble(receiveModel.getValueAt(row, 5).toString());
                    double sp   = Double.parseDouble(receiveModel.getValueAt(row, 7).toString());
                    if (cost > 0) {
                        double markup = ((sp - cost) / cost) * 100.0;
                        updating[0] = true;
                        receiveModel.setValueAt(String.format("%.1f", markup), row, 6);
                        updating[0] = false;
                    }
                } catch (Exception ignored) {}
            }
        });

        panel.add(RetailThemeManager.scroll(receiveTable), BorderLayout.CENTER);

        // Batch / expiry (optional)
        JPanel extra = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        extra.setOpaque(false);
        JTextField batchF = RetailThemeManager.styledField();
        batchF.setPreferredSize(new Dimension(160, 36));
        batchF.putClientProperty("JTextField.placeholderText", "Batch # (optional)");
        JTextField expiryF = RetailThemeManager.styledField();
        expiryF.setPreferredSize(new Dimension(130, 36));
        expiryF.putClientProperty("JTextField.placeholderText", "Expiry YYYY-MM-DD");
        JLabel batchLbl  = new JLabel("Batch:");  batchLbl.setForeground(RetailThemeManager.TEXT_MUTED);
        JLabel expiryLbl = new JLabel("Expiry:"); expiryLbl.setForeground(RetailThemeManager.TEXT_MUTED);
        extra.add(batchLbl);  extra.add(batchF);
        extra.add(expiryLbl); extra.add(expiryF);

        JLabel infoLbl = new JLabel(
            "<html><i style='color:gray'>Tip: Edit <b>Markup %</b> or <b>Selling Price</b> — "
            + "they auto-calculate each other. New prices are saved when you confirm.</i></html>");
        infoLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLbl.setForeground(RetailThemeManager.TEXT_MUTED);

        JPanel southPanel = new JPanel(new BorderLayout(0, 4));
        southPanel.setOpaque(false);
        southPanel.add(extra, BorderLayout.NORTH);
        southPanel.add(infoLbl, BorderLayout.SOUTH);
        panel.add(southPanel, BorderLayout.SOUTH);

        d.setContentPane(panel);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(RetailThemeManager.SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));
        JLabel errLbl = new JLabel(" ");
        errLbl.setForeground(RetailThemeManager.DANGER);
        errLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        JButton cancelBtn = RetailThemeManager.secondaryButton("Cancel");
        JButton confirmBtn = RetailThemeManager.successButton("Confirm Receipt");
        confirmBtn.setPreferredSize(new Dimension(180, 44));

        cancelBtn.addActionListener(e -> d.dispose());
        confirmBtn.addActionListener(e -> {
            // Commit any active cell edit
            if (receiveTable.isEditing()) receiveTable.getCellEditor().stopCellEditing();

            // Parse batch/expiry
            String batch = batchF.getText().trim().isEmpty() ? null : batchF.getText().trim();
            java.time.LocalDate expiry = null;
            if (!expiryF.getText().isBlank()) {
                try { expiry = java.time.LocalDate.parse(expiryF.getText().trim()); }
                catch (Exception ex) { errLbl.setText("Invalid expiry date (use YYYY-MM-DD)"); return; }
            }
            final java.time.LocalDate finalExpiry = expiry;

            // Collect receive quantities and new selling prices
            List<int[]> receiveQtys = new ArrayList<>(); // [itemIndex, qty]
            List<Double> newSellingPrices = new ArrayList<>();
            for (int i = 0; i < receiveModel.getRowCount(); i++) {
                try {
                    int outstanding = (Integer) receiveModel.getValueAt(i, 3);
                    Object val = receiveModel.getValueAt(i, 4);
                    int qty = val instanceof Integer ? (Integer) val
                        : Integer.parseInt(val.toString().trim());
                    if (qty < 0) { errLbl.setText("Row " + (i+1) + ": quantity cannot be negative"); return; }
                    if (qty > outstanding) {
                        errLbl.setText("Row " + (i+1) + ": cannot receive more than outstanding (" + outstanding + ")");
                        return;
                    }
                    receiveQtys.add(new int[]{ i, qty });

                    // Parse new selling price
                    double newSp = Double.parseDouble(receiveModel.getValueAt(i, 7).toString().trim());
                    if (newSp < 0) { errLbl.setText("Row " + (i+1) + ": selling price cannot be negative"); return; }
                    newSellingPrices.add(newSp);
                } catch (NumberFormatException ex) {
                    errLbl.setText("Row " + (i+1) + ": invalid quantity or price");
                    return;
                }
            }

            boolean anyReceived = receiveQtys.stream().anyMatch(r2 -> r2[1] > 0);
            if (!anyReceived) { errLbl.setText("Enter a quantity > 0 for at least one item"); return; }

            confirmBtn.setEnabled(false); confirmBtn.setText("Processing…");

            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    String userId = AuthService.getInstance().getCurrentUser().getId();
                    boolean allFullyReceived = true;

                    for (int[] rec : receiveQtys) {
                        int idx = rec[0];
                        int qty = rec[1];

                        PurchaseOrder.PurchaseOrderItem item = po.getItems().get(idx);

                        if (item.getProductId() != null) {
                            // Update inventory stock
                            if (qty > 0) {
                                invSvc.recordStockIn(item.getProductId(), qty, batch, finalExpiry, userId);
                            }

                            // Update selling price if it changed
                            double newSp = newSellingPrices.get(idx);
                            Optional<com.retailpos.model.Product> optP =
                                prodSvc.findById(item.getProductId());
                            if (optP.isPresent()) {
                                com.retailpos.model.Product prod = optP.get();
                                if (Math.abs(prod.getSellingPrice() - newSp) > 0.001) {
                                    prod.setSellingPrice(newSp);
                                    prodSvc.saveProduct(prod, userId);
                                }
                            }
                        }

                        // Update received qty on PO item
                        int newReceived = item.getReceivedQty() + qty;
                        poRepo.updateReceivedQty(po.getId(), item.getProductId(), newReceived);
                        item.setReceivedQty(newReceived);

                        if (item.getOutstandingQty() > 0) allFullyReceived = false;
                    }

                    // Determine new PO status
                    String newStatus = allFullyReceived ? "RECEIVED" : "PARTIALLY_RECEIVED";
                    poRepo.updateStatus(po.getId(), newStatus);
                    return null;
                }
                @Override protected void done() {
                    confirmBtn.setEnabled(true); confirmBtn.setText("Confirm Receipt");
                    try {
                        get(); d.dispose(); loadData();
                        JOptionPane.showMessageDialog(PurchasesPanel.this,
                            "Stock updated successfully.");
                    } catch (Exception ex) {
                        errLbl.setText("Error: " + ex.getMessage());
                        JOptionPane.showMessageDialog(d, "Error: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        footer.add(errLbl); footer.add(cancelBtn); footer.add(confirmBtn);
        d.add(footer, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(confirmBtn);
        d.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return l;
    }
}
