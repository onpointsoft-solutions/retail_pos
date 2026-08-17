package com.retailpos.view;

import com.retailpos.model.*;
import com.retailpos.service.*;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Services module panel — shown as a tab in MainFrame (admin only).
 *
 * Layout
 * ──────
 *  Top toolbar  : New Job Card | Edit | Quotation | Invoice | Status | Refresh + Search
 *  Status chips : Open / In Progress / Awaiting Parts / Completed / Cancelled / Invoiced counts
 *  Main table   : Job Card list
 *  Bottom strip : selected job card detail + linked quotation summary
 */
public class ServicesPanel extends JPanel {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Services ──────────────────────────────────────────────────────────────
    private final ServiceService svc = ServiceService.getInstance();
    private final ProductService productSvc = ProductService.getInstance();

    // ── State ──────────────────────────────────────────────────────────────────
    private List<JobCard> allJobCards = new ArrayList<>();
    private JobCard.Status filterStatus = null; // null = all

    // ── Main table ─────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable jobTable;
    private JTextField searchField;

    // ── Status counters ────────────────────────────────────────────────────────
    private final Map<JobCard.Status, JButton> statusChips = new LinkedHashMap<>();

    // ── Detail strip ───────────────────────────────────────────────────────────
    private JLabel detailJobNum, detailCustomer, detailAsset, detailStatus,
                   detailProblem, detailTechnician, detailDue, detailQuotation;

    // ── Toolbar buttons ────────────────────────────────────────────────────────
    private JButton editBtn, quotationBtn, invoiceBtn, statusBtn;

    public ServicesPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(RetailThemeManager.SURFACE);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        // Register listener so background sync refreshes the table
        svc.addListener(new ServiceService.ServiceListener() {
            @Override public void onJobCardChanged(JobCard jc) {
                SwingUtilities.invokeLater(() -> loadData());
            }
            @Override public void onQuotationChanged(Quotation q) {
                SwingUtilities.invokeLater(() -> refreshDetailStrip());
            }
        });
        loadData();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI CONSTRUCTION
    // ═════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        add(buildToolbar(),     BorderLayout.NORTH);
        add(buildCentrePanel(), BorderLayout.CENTER);
        add(buildDetailStrip(), BorderLayout.SOUTH);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 8, 0));

        // Left: action buttons
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        left.setOpaque(false);

        JButton newBtn = RetailThemeManager.primaryButton("New Job Card", "services");
        newBtn.addActionListener(e -> openJobCardForm(null));

        editBtn = RetailThemeManager.secondaryButton("Edit", "edit");
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> openJobCardForm(selectedJobCard()));

        quotationBtn = RetailThemeManager.secondaryButton("Quotation", "purchases");
        quotationBtn.setEnabled(false);
        quotationBtn.setToolTipText("Create or edit quotation (parts from store)");
        quotationBtn.addActionListener(e -> openQuotationDialog(selectedJobCard()));

        invoiceBtn = RetailThemeManager.successButton("Invoice", "pay");
        invoiceBtn.setEnabled(false);
        invoiceBtn.setToolTipText("Convert approved quotation to invoice");
        invoiceBtn.addActionListener(e -> doInvoice(selectedJobCard()));

        statusBtn = RetailThemeManager.secondaryButton("Status", "check");
        statusBtn.setEnabled(false);
        statusBtn.addActionListener(e -> changeStatus(selectedJobCard()));

        JButton refreshBtn = RetailThemeManager.secondaryButton("Refresh", "refresh");
        refreshBtn.addActionListener(e -> loadData());

        left.add(newBtn); left.add(editBtn); left.add(quotationBtn);
        left.add(invoiceBtn); left.add(statusBtn); left.add(refreshBtn);

        // Right: search
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        right.setOpaque(false);
        searchField = RetailThemeManager.styledField();
        searchField.setPreferredSize(new Dimension(220, 34));
        searchField.putClientProperty("JTextField.placeholderText", "Search job cards…");
        searchField.addActionListener(e -> doSearch());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        right.add(new JLabel(Icons.get("search", 16)));
        right.add(searchField);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        // Status-filter chips row
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chips.setOpaque(false);
        chips.setBorder(new EmptyBorder(4, 0, 0, 0));

        JButton allChip = chipButton("All", null);
        chips.add(allChip);
        for (JobCard.Status s : JobCard.Status.values()) {
            JButton chip = chipButton(s.display(), s);
            chips.add(chip);
        }

        JPanel north = new JPanel(new BorderLayout(0, 2));
        north.setOpaque(false);
        north.add(bar, BorderLayout.NORTH);
        north.add(chips, BorderLayout.SOUTH);
        return north;
    }

    private JButton chipButton(String label, JobCard.Status status) {
        JButton b = new JButton(label);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RetailThemeManager.BORDER, 1, true),
            new EmptyBorder(3, 10, 3, 10)));
        b.setBackground(RetailThemeManager.CARD_BG);
        b.setForeground(RetailThemeManager.TEXT_MUTED);
        b.addActionListener(e -> {
            filterStatus = status;
            applyFilter();
        });
        if (status != null) statusChips.put(status, b);
        return b;
    }

    // ── Centre: table ─────────────────────────────────────────────────────────

    private JComponent buildCentrePanel() {
        String[] cols = {"Job #", "Customer", "Asset / Item", "Problem", "Technician", "Status", "Due", "Created"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        jobTable = RetailThemeManager.styledTable(tableModel);
        jobTable.setRowHeight(28);
        jobTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        jobTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        jobTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        jobTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        jobTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        jobTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        jobTable.getColumnModel().getColumn(6).setPreferredWidth(85);
        jobTable.getColumnModel().getColumn(7).setPreferredWidth(100);

        // Colour-code the Status column
        jobTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                if (!sel) {
                    String status = v == null ? "" : v.toString();
                    lbl.setForeground(statusColour(status));
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                }
                return lbl;
            }
        });

        jobTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });
        jobTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openJobCardForm(selectedJobCard());
            }
        });

        JScrollPane scroll = RetailThemeManager.scroll(jobTable);
        scroll.setBorder(BorderFactory.createLineBorder(RetailThemeManager.BORDER));
        return scroll;
    }

    // ── Detail strip ──────────────────────────────────────────────────────────

    private JComponent buildDetailStrip() {
        JPanel strip = new JPanel(new BorderLayout(10, 0));
        strip.setBackground(RetailThemeManager.CARD_BG);
        strip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER),
            new EmptyBorder(8, 12, 8, 12)));
        strip.setPreferredSize(new Dimension(0, 72));

        // Left: job info
        JPanel left = new JPanel(new GridLayout(2, 4, 16, 2));
        left.setOpaque(false);
        detailJobNum    = detailLabel("—");
        detailCustomer  = detailLabel("—");
        detailAsset     = detailLabel("—");
        detailStatus    = detailLabel("—");
        detailProblem   = detailLabel("—");
        detailTechnician= detailLabel("—");
        detailDue       = detailLabel("—");
        detailQuotation = detailLabel("—");
        left.add(captionLabel("Job #"));       left.add(captionLabel("Customer"));
        left.add(captionLabel("Asset"));       left.add(captionLabel("Status"));
        left.add(detailJobNum);                left.add(detailCustomer);
        left.add(detailAsset);                 left.add(detailStatus);

        JPanel right = new JPanel(new GridLayout(2, 4, 16, 2));
        right.setOpaque(false);
        right.add(captionLabel("Problem"));    right.add(captionLabel("Technician"));
        right.add(captionLabel("Due"));        right.add(captionLabel("Quotation"));
        right.add(detailProblem);              right.add(detailTechnician);
        right.add(detailDue);                  right.add(detailQuotation);

        strip.add(left,  BorderLayout.WEST);
        strip.add(right, BorderLayout.CENTER);
        return strip;
    }

    private JLabel detailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(RetailThemeManager.TEXT);
        return l;
    }

    private JLabel captionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(RetailThemeManager.TEXT_MUTED);
        return l;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ═════════════════════════════════════════════════════════════════════════

    public void loadData() {
        new SwingWorker<List<JobCard>, Void>() {
            @Override protected List<JobCard> doInBackground() throws Exception {
                return svc.getAllJobCards();
            }
            @Override protected void done() {
                try {
                    allJobCards = get();
                    applyFilter();
                    refreshStatusChips();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ServicesPanel.this,
                        "Failed to load job cards: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        for (JobCard jc : allJobCards) {
            if (filterStatus != null && jc.getStatus() != filterStatus) continue;
            if (!query.isEmpty()) {
                String hay = (jc.getJobNumber() + " " + jc.getCustomerName() + " "
                    + jc.getCustomerPhone() + " " + jc.getAssetDescription()
                    + " " + safe(jc.getAssetSerial())).toLowerCase();
                if (!hay.contains(query)) continue;
            }
            tableModel.addRow(new Object[]{
                jc.getJobNumber(),
                jc.getCustomerName(),
                jc.getAssetDescription() + (blank(jc.getAssetSerial()) ? "" : " [" + jc.getAssetSerial() + "]"),
                truncate(jc.getProblemDescription(), 50),
                blank(jc.getTechnicianName()) ? "—" : jc.getTechnicianName(),
                jc.getStatus().display(),
                jc.getDueDate() != null ? jc.getDueDate().format(DATE) : "—",
                jc.getCreatedAt() != null ? jc.getCreatedAt().format(DT) : "—"
            });
        }
        clearDetail();
        updateToolbarButtons(null);
    }

    private javax.swing.Timer searchTimer;
    private void scheduleSearch() {
        if (searchTimer != null && searchTimer.isRunning()) searchTimer.stop();
        searchTimer = new javax.swing.Timer(200, e -> applyFilter());
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    private void doSearch() { applyFilter(); }

    private void refreshStatusChips() {
        Map<String, Integer> counts = svc.getJobCardStatusCounts();
        for (JobCard.Status s : JobCard.Status.values()) {
            JButton chip = statusChips.get(s);
            if (chip != null) {
                int n = counts.getOrDefault(s.display(), 0);
                chip.setText(s.display() + " (" + n + ")");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SELECTION
    // ═════════════════════════════════════════════════════════════════════════

    private JobCard selectedJobCard() {
        int row = jobTable.getSelectedRow();
        if (row < 0) return null;
        String jobNum = (String) tableModel.getValueAt(row, 0);
        return allJobCards.stream()
            .filter(jc -> jc.getJobNumber().equals(jobNum))
            .findFirst().orElse(null);
    }

    private void onRowSelected() {
        JobCard jc = selectedJobCard();
        updateToolbarButtons(jc);
        if (jc != null) populateDetail(jc);
        else clearDetail();
    }

    private void updateToolbarButtons(JobCard jc) {
        boolean has = jc != null;
        boolean editable = has && jc.getStatus() != JobCard.Status.INVOICED
                                && jc.getStatus() != JobCard.Status.CANCELLED;
        editBtn.setEnabled(editable);
        quotationBtn.setEnabled(editable);
        statusBtn.setEnabled(editable);

        // Invoice only when there is an APPROVED quotation
        boolean canInvoice = false;
        if (has && jc.getActiveQuotationId() != null) {
            try {
                Optional<Quotation> q = svc.findQuotationById(jc.getActiveQuotationId());
                canInvoice = jc.getStatus() == JobCard.Status.COMPLETED
                    && q.isPresent() && q.get().getStatus() == Quotation.Status.APPROVED;
            } catch (Exception ignored) {}
        }
        invoiceBtn.setEnabled(canInvoice);
    }

    private void populateDetail(JobCard jc) {
        detailJobNum.setText(jc.getJobNumber());
        detailCustomer.setText(jc.getCustomerName()
            + (blank(jc.getCustomerPhone()) ? "" : " · " + jc.getCustomerPhone()));
        detailAsset.setText(jc.getAssetDescription()
            + (blank(jc.getAssetSerial()) ? "" : " [" + jc.getAssetSerial() + "]"));
        detailStatus.setText(jc.getStatus().display());
        detailStatus.setForeground(statusColour(jc.getStatus().display()));
        detailProblem.setText(truncate(jc.getProblemDescription(), 45));
        detailTechnician.setText(blank(jc.getTechnicianName()) ? "—" : jc.getTechnicianName());
        detailDue.setText(jc.getDueDate() != null ? jc.getDueDate().format(DATE) : "—");

        // Quotation summary
        if (jc.getActiveQuotationId() != null) {
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception {
                    return svc.findQuotationById(jc.getActiveQuotationId())
                        .map(q -> q.getQuotationNumber() + " · "
                            + q.getStatus().display()
                            + (q.getInvoiceSaleId() == null ? "" : " · Invoice saved")
                            + " · KES " + String.format("%,.2f", q.getGrandTotal()))
                        .orElse("Not found");
                }
                @Override protected void done() {
                    try { detailQuotation.setText(get()); }
                    catch (Exception e) { detailQuotation.setText("—"); }
                }
            }.execute();
        } else {
            detailQuotation.setText("None");
        }
    }

    private void clearDetail() {
        detailJobNum.setText("—"); detailCustomer.setText("—");
        detailAsset.setText("—");  detailStatus.setText("—");
        detailStatus.setForeground(RetailThemeManager.TEXT_MUTED);
        detailProblem.setText("—"); detailTechnician.setText("—");
        detailDue.setText("—");    detailQuotation.setText("—");
    }

    private void refreshDetailStrip() {
        JobCard jc = selectedJobCard();
        if (jc != null) populateDetail(jc);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DIALOGS
    // ═════════════════════════════════════════════════════════════════════════

    // ── Job Card Form ─────────────────────────────────────────────────────────

    private void openJobCardForm(JobCard existing) {
        boolean isNew = existing == null;
        JDialog dlg = new JDialog(parentFrame(), isNew ? "New Job Card" : "Edit Job Card — " + existing.getJobNumber(), true);
        dlg.setSize(620, 680);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(RetailThemeManager.SURFACE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(RetailThemeManager.NAVY);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel(isNew ? "New Job Card" : "Edit — " + existing.getJobNumber());
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        title.setIcon(Icons.get("services", 20));
        header.add(title, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Form fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(RetailThemeManager.SURFACE);
        form.setBorder(new EmptyBorder(16, 20, 8, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 6, 5, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField fName    = field(existing != null ? existing.getCustomerName()   : "");
        JTextField fPhone   = field(existing != null ? existing.getCustomerPhone()  : "");
        JTextField fAsset   = field(existing != null ? existing.getAssetDescription(): "");
        JTextField fSerial  = field(existing != null ? existing.getAssetSerial()    : "");
        JTextArea  fProblem = area(existing != null ? existing.getProblemDescription(): "", 3);
        JTextArea  fDiagnosis = area(existing != null ? safe(existing.getDiagnosis()): "", 2);
        JTextArea  fResolution= area(existing != null ? safe(existing.getResolution()): "", 2);
        JTextField fTech    = field(existing != null ? safe(existing.getTechnicianName()): "");
        JTextField fLabour  = field(existing != null ? String.format("%.2f", existing.getLabourCharge()) : "0.00");
        JTextField fDue     = field(existing != null && existing.getDueDate() != null
                                    ? existing.getDueDate().format(DATE) : "");
        fDue.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        // Status combo (hidden for new)
        String[] statuses = Arrays.stream(JobCard.Status.values())
            .filter(s -> s != JobCard.Status.INVOICED)
            .map(JobCard.Status::display).toArray(String[]::new);
        JComboBox<String> statusCombo = new JComboBox<>(statuses);
        if (existing != null && existing.getStatus() != JobCard.Status.INVOICED)
            statusCombo.setSelectedItem(existing.getStatus().display());

        int r = 0;
        row(form, gc, r++, "Customer Name *",   fName);
        row(form, gc, r++, "Customer Phone",     fPhone);
        row(form, gc, r++, "Asset / Item *",     fAsset);
        row(form, gc, r++, "Serial / Plate / IMEI", fSerial);
        rowArea(form, gc, r++, "Problem Description *", fProblem);
        rowArea(form, gc, r++, "Diagnosis",      fDiagnosis);
        rowArea(form, gc, r++, "Resolution",     fResolution);
        row(form, gc, r++, "Technician",         fTech);
        row(form, gc, r++, "Labour Charge (KES)",fLabour);
        row(form, gc, r++, "Due Date",           fDue);
        if (!isNew) row(form, gc, r, "Status",   statusCombo);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        root.add(formScroll, BorderLayout.CENTER);

        // Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        footer.setBackground(RetailThemeManager.SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));
        JButton cancel = RetailThemeManager.secondaryButton("Cancel");
        JButton save   = RetailThemeManager.primaryButton(isNew ? "Open Job Card" : "Save Changes");
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            try {
                String name    = fName.getText().trim();
                String phone   = fPhone.getText().trim();
                String asset   = fAsset.getText().trim();
                String serial  = fSerial.getText().trim();
                String problem = fProblem.getText().trim();
                String diag    = fDiagnosis.getText().trim();
                String resol   = fResolution.getText().trim();
                String tech    = fTech.getText().trim();
                double labour  = parseDouble(fLabour.getText(), 0);
                LocalDateTime due = null;
                if (!fDue.getText().isBlank()) {
                    try { due = LocalDateTime.parse(fDue.getText().trim() + " 00:00",
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
                    catch (Exception ignored) {}
                }

                if (isNew) {
                    svc.createJobCard(name, phone, null, asset, serial,
                            problem, tech, null, labour, due, new ArrayList<>());
                } else {
                    JobCard.Status newSt = Arrays.stream(JobCard.Status.values())
                        .filter(s -> s.display().equals(statusCombo.getSelectedItem()))
                        .findFirst().orElse(existing.getStatus());
                    svc.updateJobCard(existing, name, phone, existing.getCustomerId(),
                            asset, serial, problem, diag, resol, tech,
                            existing.getTechnicianId(), labour, due, newSt, null);
                }
                dlg.dispose();
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        footer.add(cancel); footer.add(save);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ── Quotation Dialog ──────────────────────────────────────────────────────

    private void openQuotationDialog(JobCard jc) {
        if (jc == null) return;

        // Load existing quotation if present
        Quotation existing = null;
        if (jc.getActiveQuotationId() != null) {
            try { existing = svc.findQuotationById(jc.getActiveQuotationId()).orElse(null); }
            catch (Exception ignored) {}
        }
        final Quotation existingQt = existing;

        JDialog dlg = new JDialog(parentFrame(),
            "Quotation — " + jc.getJobNumber() + " · " + jc.getCustomerName(), true);
        dlg.setSize(820, 680);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(RetailThemeManager.SURFACE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(RetailThemeManager.NAVY);
        header.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel title = new JLabel("Quotation · " + jc.getJobNumber());
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        title.setIcon(Icons.get("purchases", 18));
        JLabel sub = new JLabel(jc.getCustomerName()
            + "  ·  Asset: " + jc.getAssetDescription());
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(148, 163, 184));
        JPanel htxt = new JPanel(new GridLayout(2, 1, 0, 2));
        htxt.setOpaque(false);
        htxt.add(title); htxt.add(sub);
        header.add(htxt, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // ── Parts table ───────────────────────────────────────────────────────
        String[] cols = {"SKU", "Product / Part", "Qty", "Unit Price", "Discount", "Total"};
        DefaultTableModel partsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2 || c == 3 || c == 4; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 2) ? Integer.class : String.class;
            }
        };
        JTable partsTable = RetailThemeManager.styledTable(partsModel);
        partsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        partsTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        partsTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        partsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        partsTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        partsTable.getColumnModel().getColumn(5).setPreferredWidth(90);

        // Keep a parallel list of full QuotationItem objects (for productId etc.)
        List<Quotation.QuotationItem> lineItems = new ArrayList<>();

        // Populate from existing quotation
        if (existingQt != null) {
            for (Quotation.QuotationItem qi : existingQt.getItems()) {
                lineItems.add(qi);
                partsModel.addRow(new Object[]{
                    safe(qi.getProductSku()), qi.getProductName(),
                    qi.getQuantity(),
                    String.format("%.2f", qi.getUnitPrice()),
                    String.format("%.2f", qi.getDiscount()),
                    String.format("%.2f", qi.getLineTotal())
                });
            }
        }

        // Recalc line total when qty/price/discount edited
        partsModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || row >= lineItems.size()) return;
            if (col == 2 || col == 3 || col == 4) {
                try {
                    Quotation.QuotationItem qi = lineItems.get(row);
                    Object qv = partsModel.getValueAt(row, 2);
                    qi.setQuantity(qv instanceof Integer ? (Integer)qv : Integer.parseInt(qv.toString()));
                    qi.setUnitPrice(parseDouble(partsModel.getValueAt(row, 3).toString(), qi.getUnitPrice()));
                    qi.setDiscount(parseDouble(partsModel.getValueAt(row, 4).toString(), 0));
                    qi.recalculate();
                    partsModel.setValueAt(String.format("%.2f", qi.getLineTotal()), row, 5);
                } catch (Exception ignored) {}
            }
        });

        JScrollPane partsScroll = RetailThemeManager.scroll(partsTable);
        partsScroll.setBorder(BorderFactory.createLineBorder(RetailThemeManager.BORDER));
        partsScroll.setPreferredSize(new Dimension(0, 260));

        // ── Product search bar ────────────────────────────────────────────────
        JTextField productSearch = RetailThemeManager.styledField();
        productSearch.putClientProperty("JTextField.placeholderText",
            "Type product name or SKU then press Enter to add…");
        productSearch.addActionListener(e -> {
            String q = productSearch.getText().trim();
            if (q.isBlank()) return;
            List<Product> found = productSvc.search(q);
            if (found.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "No product found for: " + q, "Not found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Product chosen;
            if (found.size() == 1) {
                chosen = found.get(0);
            } else {
                String[] opts = found.stream()
                    .map(p -> p.getSku() + " — " + p.getName()
                        + " · KES " + String.format("%.2f", p.getSellingPrice())
                        + " · Stock: " + p.getCurrentStock())
                    .toArray(String[]::new);
                String sel = (String) JOptionPane.showInputDialog(dlg,
                    "Select product:", "Multiple matches",
                    JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
                if (sel == null) return;
                int idx = Arrays.asList(opts).indexOf(sel);
                chosen = found.get(idx);
            }

            // Check if already in list — increment qty
            for (int i = 0; i < lineItems.size(); i++) {
                Quotation.QuotationItem qi = lineItems.get(i);
                if (chosen.getId().equals(qi.getProductId())) {
                    qi.setQuantity(qi.getQuantity() + 1);
                    qi.recalculate();
                    partsModel.setValueAt(qi.getQuantity(), i, 2);
                    partsModel.setValueAt(String.format("%.2f", qi.getLineTotal()), i, 5);
                    productSearch.setText("");
                    return;
                }
            }

            Quotation.QuotationItem qi = new Quotation.QuotationItem();
            qi.setId(UUID.randomUUID().toString());
            qi.setProductId(chosen.getId());
            qi.setProductName(chosen.getName());
            qi.setProductSku(chosen.getSku());
            qi.setQuantity(1);
            qi.setUnitPrice(chosen.getSellingPrice());
            qi.setBuyingPrice(chosen.getBuyingPrice());
            qi.setDiscount(0);
            qi.setTaxRate(chosen.getTaxRate());
            qi.recalculate();
            lineItems.add(qi);
            partsModel.addRow(new Object[]{
                chosen.getSku(), chosen.getName(), 1,
                String.format("%.2f", chosen.getSellingPrice()),
                "0.00",
                String.format("%.2f", qi.getLineTotal())
            });
            productSearch.setText("");
        });

        JButton removeLineBtn = RetailThemeManager.dangerButton("Remove line", "delete");
        removeLineBtn.addActionListener(e -> {
            int row = partsTable.getSelectedRow();
            if (row >= 0 && row < lineItems.size()) {
                lineItems.remove(row);
                partsModel.removeRow(row);
            }
        });

        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setOpaque(false);
        searchBar.add(new JLabel(Icons.get("search", 16)), BorderLayout.WEST);
        searchBar.add(productSearch, BorderLayout.CENTER);
        searchBar.add(removeLineBtn, BorderLayout.EAST);

        // ── Totals + notes ────────────────────────────────────────────────────
        JTextField fDiscount = field(existingQt != null
            ? String.format("%.2f", existingQt.getDiscountAmount()) : "0.00");
        JTextArea  fNotes    = area(existingQt != null ? safe(existingQt.getNotes()) : "", 2);
        JTextField fValidUntil = field(existingQt != null && existingQt.getValidUntil() != null
            ? existingQt.getValidUntil().format(DATE) : "");
        fValidUntil.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        JLabel labourLine = new JLabel("Labour / Service: KES "
            + String.format("%,.2f", jc.getTotalLabour()));
        labourLine.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        labourLine.setForeground(RetailThemeManager.TEXT_MUTED);

        JPanel totalsPanel = new JPanel(new GridBagLayout());
        totalsPanel.setOpaque(false);
        totalsPanel.setBorder(new EmptyBorder(4, 0, 4, 0));
        GridBagConstraints tgc = new GridBagConstraints();
        tgc.insets = new Insets(3, 6, 3, 6);
        tgc.anchor = GridBagConstraints.WEST;
        tgc.gridy = 0; tgc.gridx = 0; totalsPanel.add(new JLabel("Discount (KES):"), tgc);
        tgc.gridx = 1; totalsPanel.add(fDiscount, tgc);
        tgc.gridx = 2; totalsPanel.add(new JLabel("Valid Until:"), tgc);
        tgc.gridx = 3; totalsPanel.add(fValidUntil, tgc);
        tgc.gridy = 1; tgc.gridx = 0; tgc.gridwidth = 2; totalsPanel.add(new JLabel("Notes:"), tgc);
        tgc.gridx = 2; tgc.gridwidth = 2;
        totalsPanel.add(new JScrollPane(fNotes) {{ setPreferredSize(new Dimension(250, 50)); }}, tgc);

        // ── Centre assembly ───────────────────────────────────────────────────
        JPanel centre = new JPanel(new BorderLayout(0, 6));
        centre.setOpaque(false);
        centre.setBorder(new EmptyBorder(10, 14, 6, 14));
        centre.add(searchBar,   BorderLayout.NORTH);
        centre.add(partsScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        bottom.add(labourLine, BorderLayout.NORTH);
        bottom.add(totalsPanel, BorderLayout.CENTER);
        centre.add(bottom, BorderLayout.SOUTH);
        root.add(centre, BorderLayout.CENTER);

        // ── Footer buttons ────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        footer.setBackground(RetailThemeManager.SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));

        JButton cancelBtn  = RetailThemeManager.secondaryButton("Cancel");
        JButton draftBtn   = RetailThemeManager.secondaryButton("Save Draft");
        JButton approveBtn = RetailThemeManager.successButton("Save & Approve");

        cancelBtn.addActionListener(e -> dlg.dispose());

        draftBtn.addActionListener(e -> saveQuotation(dlg, jc, existingQt,
                lineItems, fDiscount, fNotes, fValidUntil, Quotation.Status.DRAFT));

        approveBtn.addActionListener(e -> saveQuotation(dlg, jc, existingQt,
                lineItems, fDiscount, fNotes, fValidUntil, Quotation.Status.APPROVED));

        // If already approved show Reject button
        if (existingQt != null && existingQt.getStatus() == Quotation.Status.APPROVED) {
            productSearch.setEnabled(false);
            partsTable.setEnabled(false);
            fDiscount.setEditable(false);
            fNotes.setEditable(false);
            fValidUntil.setEditable(false);
            draftBtn.setEnabled(false);
            approveBtn.setEnabled(false);
            JButton rejectBtn = RetailThemeManager.dangerButton("Reject", "delete");
            rejectBtn.addActionListener(e -> {
                try {
                    svc.rejectQuotation(existingQt);
                    dlg.dispose();
                    loadData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            footer.add(rejectBtn);
        }

        footer.add(cancelBtn); footer.add(draftBtn); footer.add(approveBtn);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void saveQuotation(JDialog dlg, JobCard jc, Quotation existing,
            List<Quotation.QuotationItem> lineItems,
            JTextField fDiscount, JTextArea fNotes, JTextField fValidUntil,
            Quotation.Status targetStatus) {
        try {
            double disc = parseDouble(fDiscount.getText(), 0);
            String notes = fNotes.getText().trim();
            LocalDateTime validUntil = null;
            if (!fValidUntil.getText().isBlank()) {
                try {
                    validUntil = LocalDateTime.parse(fValidUntil.getText().trim() + " 00:00",
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                } catch (Exception ignored) {}
            }

            Quotation saved;
            if (existing == null) {
                saved = svc.createQuotation(jc, new ArrayList<>(lineItems), disc, notes, validUntil);
            } else {
                saved = svc.updateQuotation(existing, new ArrayList<>(lineItems), disc, notes, validUntil);
            }
            if (targetStatus == Quotation.Status.APPROVED
                    && saved.getStatus() != Quotation.Status.APPROVED) {
                svc.approveQuotation(saved);
            }
            dlg.dispose();
            loadData();
            JOptionPane.showMessageDialog(this,
                "Quotation " + saved.getQuotationNumber() + " saved ("
                    + saved.getStatus().display() + ").\n"
                    + "Grand Total: KES " + String.format("%,.2f", saved.getGrandTotal()),
                "Quotation Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Invoice ───────────────────────────────────────────────────────────────

    private void doInvoice(JobCard jc) {
        if (jc == null) return;
        Quotation qt = null;
        try { qt = svc.findQuotationById(jc.getActiveQuotationId()).orElse(null); }
        catch (Exception ignored) {}
        if (qt == null || qt.getStatus() != Quotation.Status.APPROVED) {
            JOptionPane.showMessageDialog(this,
                "No APPROVED quotation found for this job card.",
                "Cannot Invoice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Mini payment dialog
        String[] methods = {"CASH", "MPESA", "CARD", "CREDIT"};
        JComboBox<String> methodCombo = new JComboBox<>(methods);
        JTextField cashField = RetailThemeManager.styledField();
        cashField.putClientProperty("JTextField.placeholderText", "Cash tendered (for CASH)");
        JTextField refField  = RetailThemeManager.styledField();
        refField.putClientProperty("JTextField.placeholderText", "M-Pesa code / reference");

        JPanel pay = new JPanel(new GridLayout(3, 2, 8, 8));
        pay.add(new JLabel("Payment method:")); pay.add(methodCombo);
        pay.add(new JLabel("Cash tendered:"));  pay.add(cashField);
        pay.add(new JLabel("Reference:"));       pay.add(refField);

        double grandTotal = qt.getGrandTotal();
        int ok = JOptionPane.showConfirmDialog(this, pay,
            "Invoice  ·  KES " + String.format("%,.2f", grandTotal),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        final Quotation finalQt = qt;
        new SwingWorker<Sale, Void>() {
            @Override protected Sale doInBackground() throws Exception {
                String method   = (String) methodCombo.getSelectedItem();
                double tendered = parseDouble(cashField.getText(), grandTotal);
                String ref      = refField.getText().trim();
                return svc.convertToInvoice(finalQt, jc, method, tendered, ref);
            }
            @Override protected void done() {
                try {
                    Sale sale = get();
                    loadData();
                    JOptionPane.showMessageDialog(ServicesPanel.this,
                        "Invoice created.\nReceipt: " + sale.getReceiptNumber()
                        + "\nTotal: KES " + String.format("%,.2f", sale.getGrandTotal()),
                        "Invoiced", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServicesPanel.this,
                        ex.getMessage(), "Invoice Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Status change ─────────────────────────────────────────────────────────

    private void changeStatus(JobCard jc) {
        if (jc == null) return;
        JobCard.Status[] allowed = Arrays.stream(JobCard.Status.values())
            .filter(s -> s != JobCard.Status.INVOICED)
            .toArray(JobCard.Status[]::new);
        String[] labels = Arrays.stream(allowed).map(JobCard.Status::display).toArray(String[]::new);
        String choice = (String) JOptionPane.showInputDialog(this,
            "Change status of " + jc.getJobNumber() + ":",
            "Change Status", JOptionPane.PLAIN_MESSAGE, null, labels, jc.getStatus().display());
        if (choice == null) return;
        JobCard.Status newStatus = Arrays.stream(allowed)
            .filter(s -> s.display().equals(choice)).findFirst().orElse(jc.getStatus());
        try {
            svc.updateJobCardStatus(jc, newStatus);
            loadData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ═════════════════════════════════════════════════════════════════════════

    private Frame parentFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private JTextField field(String val) {
        JTextField f = RetailThemeManager.styledField();
        f.setText(val == null ? "" : val);
        return f;
    }

    private JTextArea area(String val, int rows) {
        JTextArea a = new JTextArea(val == null ? "" : val, rows, 0);
        a.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(4, 6, 4, 6));
        return a;
    }

    private void row(JPanel p, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridy = row; gc.gridx = 0; gc.weightx = 0;
        gc.anchor = GridBagConstraints.EAST;
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(RetailThemeManager.TEXT_MUTED);
        p.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 1;
        gc.anchor = GridBagConstraints.WEST;
        p.add(field, gc);
    }

    private void rowArea(JPanel p, GridBagConstraints gc, int row, String label, JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(350, area.getRows() * 22 + 10));
        sp.setBorder(BorderFactory.createLineBorder(RetailThemeManager.BORDER));
        row(p, gc, row, label, sp);
    }

    private static String safe(String s)  { return s == null ? "" : s; }
    private static boolean blank(String s){ return s == null || s.isBlank(); }
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.replace(",", "").trim()); }
        catch (Exception e) { return def; }
    }

    private Color statusColour(String status) {
        return switch (status) {
            case "Open"           -> RetailThemeManager.PRIMARY;
            case "In Progress"    -> new Color(217, 119, 6);   // amber
            case "Awaiting Parts" -> new Color(168, 85, 247);  // purple
            case "Completed"      -> RetailThemeManager.ACCENT;
            case "Cancelled"      -> RetailThemeManager.DANGER;
            case "Invoiced"       -> new Color(100, 116, 139); // slate
            default               -> RetailThemeManager.TEXT_MUTED;
        };
    }
}
