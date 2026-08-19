package com.retailpos.view;

import com.retailpos.model.Customer;
import com.retailpos.repository.CustomerRepository;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CustomersPanel extends JPanel implements com.retailpos.ui.Refreshable {
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private final CustomerRepository repo = new CustomerRepository();
    private Timer searchDebounce;

    public CustomersPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        buildUI();
        loadAll();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);

        searchField = RetailThemeManager.styledField();
        searchField.setPreferredSize(new Dimension(300, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, phone, or email…");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        JLabel searchIcon = new JLabel(Icons.get("search", 20));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(searchIcon); left.add(searchField);
        toolbar.add(left, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        JButton addBtn  = RetailThemeManager.primaryButton("Add Customer", "add");
        JButton editBtn = RetailThemeManager.secondaryButton("Edit", "edit");
        addBtn.addActionListener(e -> showForm(null));
        editBtn.addActionListener(e -> { Customer c = getSelected(); if (c != null) showForm(c); });
        btns.add(addBtn); btns.add(editBtn);
        toolbar.add(btns, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] cols = {"Name", "Phone", "Email", "Loyalty Points", "Credit Balance"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = RetailThemeManager.styledTable(tableModel);
        add(RetailThemeManager.scroll(table), BorderLayout.CENTER);
    }

    private void scheduleSearch() {
        if (searchDebounce != null) searchDebounce.stop();
        searchDebounce = new Timer(200, e -> doSearch());
        searchDebounce.setRepeats(false); searchDebounce.start();
    }

    private void doSearch() {
        String q = searchField.getText().trim();
        new SwingWorker<List<Customer>, Void>() {
            @Override protected List<Customer> doInBackground() throws Exception {
                return q.isEmpty() ? repo.findAll() : repo.search(q);
            }
            @Override protected void done() {
                try { populate(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadAll() {
        new SwingWorker<List<Customer>, Void>() {
            @Override protected List<Customer> doInBackground() throws Exception { return repo.findAll(); }
            @Override protected void done() {
                try { populate(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }

    @Override public void refreshData() { loadAll(); }
    @Override public int getRefreshIntervalSeconds() { return 120; }
    @Override public String getPanelDescription() { return "Customers — loyalty & credit"; }

    private void populate(List<Customer> list) {
        tableModel.setRowCount(0);
        for (Customer c : list) {
            tableModel.addRow(new Object[]{
                c.getName(), c.getPhone(), c.getEmail(),
                c.getLoyaltyPoints(),
                String.format("KES %.2f", c.getCreditBalance())
            });
        }
    }

    private Customer getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a customer first"); return null; }
        String phone = (String) tableModel.getValueAt(row, 1);
        try { return repo.findByPhone(phone).orElse(null); } catch (Exception e) { return null; }
    }

    private void showForm(Customer existing) {
        boolean isNew = (existing == null);
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add Customer" : "Edit Customer", true);
        d.setSize(440, 340); d.setLocationRelativeTo(this);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(RetailThemeManager.CARD_BG);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(4, 0, 4, 0);

        JTextField nameF  = formRow(form, g, 0, "Full Name *");
        JTextField phoneF = formRow(form, g, 2, "Phone");
        JTextField emailF = formRow(form, g, 4, "Email");

        if (existing != null) {
            nameF.setText(existing.getName() != null ? existing.getName() : "");
            phoneF.setText(existing.getPhone() != null ? existing.getPhone() : "");
            emailF.setText(existing.getEmail() != null ? existing.getEmail() : "");
        }

        d.add(form, BorderLayout.CENTER);
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancel = RetailThemeManager.secondaryButton("Cancel");
        JButton save   = RetailThemeManager.primaryButton("Save");
        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            String name  = nameF.getText().trim();
            String phone = phoneF.getText().trim();
            String email = emailF.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(d, "Name is required"); return; }
            Customer c = existing != null ? existing : new Customer();
            if (existing == null) {
                c.setId(UUID.randomUUID().toString());
                c.setCreatedAt(LocalDateTime.now());
                c.setSyncStatus("PENDING");
            }
            c.setName(name); c.setPhone(phone.isEmpty() ? null : phone);
            c.setEmail(email.isEmpty() ? null : email);
            c.setUpdatedAt(LocalDateTime.now());
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (isNew) repo.insert(c); else repo.update(c); return null;
                }
                @Override protected void done() {
                    try { get(); d.dispose(); loadAll(); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(d, "Save failed: " + ex.getMessage()); }
                }
            }.execute();
        });
        acts.add(cancel); acts.add(save);
        d.add(acts, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private JTextField formRow(JPanel p, GridBagConstraints g, int row, String label) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(l, g);
        g.gridy = row + 1; g.weightx = 1;
        JTextField f = RetailThemeManager.styledField();
        f.setPreferredSize(new Dimension(360, 36));
        p.add(f, g);
        return f;
    }
}
