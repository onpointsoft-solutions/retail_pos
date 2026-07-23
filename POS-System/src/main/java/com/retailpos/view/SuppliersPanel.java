package com.retailpos.view;

import com.retailpos.model.Supplier;
import com.retailpos.repository.SupplierRepository;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SuppliersPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private final SupplierRepository repo = new SupplierRepository();
    private Timer searchDebounce;

    public SuppliersPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        buildUI();
        loadAll();
    }

    private void buildUI() {
        // ── Toolbar ──────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        searchField = RetailThemeManager.styledField();
        searchField.setPreferredSize(new Dimension(280, 42));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, phone, email…");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        left.add(new JLabel(Icons.get("search", 20)));
        left.add(searchField);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        JButton addBtn    = RetailThemeManager.primaryButton("Add Supplier", "add");
        JButton editBtn   = RetailThemeManager.secondaryButton("Edit", "edit");
        JButton refreshBtn = RetailThemeManager.secondaryButton("Refresh", "refresh");
        addBtn.addActionListener(e -> showForm(null));
        editBtn.addActionListener(e -> { Supplier s = getSelected(); if (s != null) showForm(s); });
        refreshBtn.addActionListener(e -> loadAll());
        right.add(addBtn); right.add(editBtn); right.add(refreshBtn);
        toolbar.add(right, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"Name", "Contact", "Phone", "Email", "Address", "Balance"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = RetailThemeManager.styledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        add(RetailThemeManager.scroll(table), BorderLayout.CENTER);
    }

    private void scheduleSearch() {
        if (searchDebounce != null) searchDebounce.stop();
        searchDebounce = new Timer(200, e -> doSearch());
        searchDebounce.setRepeats(false); searchDebounce.start();
    }

    private void doSearch() {
        String q = searchField.getText().trim().toLowerCase();
        new SwingWorker<List<Supplier>, Void>() {
            @Override protected List<Supplier> doInBackground() throws Exception { return repo.findAll(); }
            @Override protected void done() {
                try {
                    List<Supplier> all = get();
                    tableModel.setRowCount(0);
                    for (Supplier s : all) {
                        if (q.isEmpty()
                            || s.getName().toLowerCase().contains(q)
                            || (s.getPhone() != null && s.getPhone().contains(q))
                            || (s.getEmail() != null && s.getEmail().toLowerCase().contains(q))) {
                            addRow(s);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadAll() {
        new SwingWorker<List<Supplier>, Void>() {
            @Override protected List<Supplier> doInBackground() throws Exception { return repo.findAll(); }
            @Override protected void done() {
                try {
                    tableModel.setRowCount(0);
                    for (Supplier s : get()) addRow(s);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void addRow(Supplier s) {
        tableModel.addRow(new Object[]{
            s.getName(),
            nvl(s.getPhone()),   // using phone as contact for now
            nvl(s.getPhone()),
            nvl(s.getEmail()),
            nvl(s.getAddress()),
            String.format("KES %.2f", s.getBalance())
        });
    }

    private Supplier getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a supplier first"); return null; }
        int modelRow = table.convertRowIndexToModel(row);
        String name  = (String) tableModel.getValueAt(modelRow, 0);
        String phone = (String) tableModel.getValueAt(modelRow, 2);
        try {
            return repo.findAll().stream()
                .filter(s -> name.equals(s.getName()))
                .findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private void showForm(Supplier existing) {
        boolean isNew = (existing == null);
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add Supplier" : "Edit Supplier", true);
        d.setSize(500, 440); d.setLocationRelativeTo(this); d.setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        g.insets = new Insets(5, 0, 5, 0);

        JTextField nameF    = field(form, g, 0,  "Company / Supplier Name *");
        JTextField contactF = field(form, g, 2,  "Contact Person");
        JTextField phoneF   = field(form, g, 4,  "Phone Number");
        JTextField emailF   = field(form, g, 6,  "Email Address");
        JTextField addressF = field(form, g, 8,  "Physical Address");

        if (existing != null) {
            nameF.setText(nvl(existing.getName()));
            phoneF.setText(nvl(existing.getPhone()));
            emailF.setText(nvl(existing.getEmail()));
            addressF.setText(nvl(existing.getAddress()));
        }

        JLabel errorLbl = new JLabel(" ");
        errorLbl.setForeground(RetailThemeManager.DANGER);
        errorLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        errorLbl.setBorder(new EmptyBorder(4, 24, 0, 24));
        d.add(errorLbl, BorderLayout.NORTH);
        d.add(form, BorderLayout.CENTER);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        acts.setBackground(RetailThemeManager.SURFACE);
        acts.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RetailThemeManager.BORDER));
        JButton cancel = RetailThemeManager.secondaryButton("Cancel");
        JButton save   = RetailThemeManager.primaryButton("Save Supplier");
        save.setPreferredSize(new Dimension(160, 44));
        cancel.addActionListener(e -> d.dispose());

        save.addActionListener(e -> {
            String nm = nameF.getText().trim();
            if (nm.isEmpty()) { errorLbl.setText("Supplier name is required"); return; }

            Supplier s = existing != null ? existing : new Supplier();
            if (existing == null) {
                s.setId(UUID.randomUUID().toString());
                s.setCreatedAt(LocalDateTime.now());
                s.setSyncStatus("PENDING");
                s.setBalance(0);
            }
            s.setName(nm);
            s.setPhone(phoneF.getText().trim().isEmpty() ? null : phoneF.getText().trim());
            s.setEmail(emailF.getText().trim().isEmpty() ? null : emailF.getText().trim());
            s.setAddress(addressF.getText().trim().isEmpty() ? null : addressF.getText().trim());
            s.setUpdatedAt(LocalDateTime.now());
            if (existing != null) s.setSyncStatus("MODIFIED");

            save.setEnabled(false); save.setText("Saving…");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (isNew) repo.insert(s); else repo.update(s); return null;
                }
                @Override protected void done() {
                    save.setEnabled(true); save.setText("Save Supplier");
                    try {
                        get(); d.dispose(); loadAll();
                        JOptionPane.showMessageDialog(SuppliersPanel.this,
                            isNew ? "Supplier added." : "Supplier updated.");
                    } catch (Exception ex) {
                        errorLbl.setText(ex.getMessage());
                        JOptionPane.showMessageDialog(d, "Save failed:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        acts.add(cancel); acts.add(save);
        d.add(acts, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(save);
        d.setVisible(true);
    }

    private JTextField field(JPanel p, GridBagConstraints g, int row, String label) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(l, g);
        g.gridy = row + 1; g.weightx = 1;
        JTextField f = RetailThemeManager.styledField();
        f.setPreferredSize(new Dimension(420, 42));
        p.add(f, g);
        return f;
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}
