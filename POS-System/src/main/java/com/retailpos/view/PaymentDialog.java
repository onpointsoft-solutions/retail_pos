package com.retailpos.view;

import com.retailpos.model.AppSettings;
import com.retailpos.model.Sale;
import com.retailpos.service.AuthService;
import com.retailpos.service.SaleService;
import com.retailpos.service.MpesaUdpBridge;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class PaymentDialog extends JDialog {
    private final List<Sale.SaleItem> items;
    private final double transactionDiscount;
    private final String customerId;
    private final AppSettings settings;
    private Sale completedSale;

    private JLabel totalLabel;
    private JTabbedPane methodTabs;
    // Cash tab
    private JTextField cashTenderedField;
    private JLabel changeLabel;
    // Reference tabs
    private JTextField mpesaReferenceField, cardReferenceField, bankReferenceField;
    // Split tab
    private JTextField splitCashField, splitMpesaField, splitCardField;
    private JLabel splitRemainingLabel;

    private double grandTotal;

    public PaymentDialog(Frame parent, List<Sale.SaleItem> items,
                         double transactionDiscount, String customerId, AppSettings settings) {
        super(parent, "Complete Payment", true);
        this.items = items;
        this.transactionDiscount = transactionDiscount;
        this.customerId = customerId;
        this.settings = settings;
        this.grandTotal = calcGrandTotal();
        setSize(520, 500);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    private double calcGrandTotal() {
        double subtotal = items.stream().mapToDouble(Sale.SaleItem::getLineTotal).sum();
        double taxRate = settings.getTaxRate();
        return (subtotal - transactionDiscount) * (1 + taxRate / 100.0);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header total
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        JLabel ttl = new JLabel("Total Due");
        ttl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ttl.setForeground(RetailThemeManager.TEXT_MUTED);
        totalLabel = new JLabel(String.format("KES %.2f", grandTotal));
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        totalLabel.setForeground(RetailThemeManager.PRIMARY);
        header.add(ttl); header.add(totalLabel);
        root.add(header, BorderLayout.NORTH);

        // Tabs
        methodTabs = new JTabbedPane();
        methodTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        methodTabs.addTab("Cash", buildCashTab());
        methodTabs.addTab("M-Pesa", buildRefTab("M-Pesa confirmation reference:", "MPESA"));
        methodTabs.addTab("Card", buildRefTab("Card Reference / Last 4 digits:", "CARD"));
        methodTabs.addTab("Bank", buildRefTab("Bank Reference / Slip Number:", "BANK"));
        methodTabs.addTab("Split", buildSplitTab());
        root.add(methodTabs, BorderLayout.CENTER);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = RetailThemeManager.secondaryButton("Cancel");
        JButton complete = RetailThemeManager.successButton("✔  COMPLETE SALE");
        complete.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cancel.addActionListener(e -> dispose());
        complete.addActionListener(e -> completeSale());
        actions.add(cancel); actions.add(complete);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);

        // F4 or Enter completes
        getRootPane().setDefaultButton(complete);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private JPanel buildCashTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 10, 10, 10));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(6, 0, 6, 0);

        g.gridy = 0;
        JLabel lbl = new JLabel("Cash Tendered (KES):");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(lbl, g);

        g.gridy = 1;
        cashTenderedField = RetailThemeManager.styledField();
        cashTenderedField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        cashTenderedField.setPreferredSize(new Dimension(300, 48));
        cashTenderedField.setText(String.format("%.2f", grandTotal));
        cashTenderedField.selectAll();
        p.add(cashTenderedField, g);

        g.gridy = 2;
        changeLabel = new JLabel("Change: KES 0.00");
        changeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        changeLabel.setForeground(RetailThemeManager.ACCENT);
        p.add(changeLabel, g);

        cashTenderedField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
        });
        return p;
    }

    private void updateChange() {
        try {
            double tendered = Double.parseDouble(cashTenderedField.getText().trim());
            double change = tendered - grandTotal;
            changeLabel.setText(String.format("Change: KES %.2f", Math.max(0, change)));
            changeLabel.setForeground(change >= 0 ? RetailThemeManager.ACCENT : RetailThemeManager.DANGER);
        } catch (NumberFormatException ignored) {
            changeLabel.setText("Change: KES 0.00");
        }
    }

    private JPanel buildRefTab(String labelText, String method) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(RetailThemeManager.CARD_BG);
        p.setBorder(new EmptyBorder(20, 10, 10, 10));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(6, 0, 6, 0);
        g.gridy = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(lbl, g);
        g.gridy = 1;
        JTextField referenceField = RetailThemeManager.styledField();
        referenceField.setPreferredSize(new Dimension(300, 44));
        assignReferenceField(method, referenceField);
        p.add(referenceField, g);
        if ("MPESA".equals(method)) {
            g.gridy = 2;
            JButton recentPayments = RetailThemeManager.primaryButton("Select verified phone payment");
            recentPayments.addActionListener(e -> selectRecentMpesaPayment());
            p.add(recentPayments, g);
            g.gridy = 3;
        } else {
            g.gridy = 2;
        }
        JLabel hint = new JLabel("MPESA".equals(method)
            ? "Use a recent BizFlow Bridge payment above. Enter a reference manually only when the phone bridge is unavailable."
            : "(Optional) Enter transaction reference for records");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(RetailThemeManager.TEXT_MUTED);
        p.add(hint, g);
        return p;
    }

    private void assignReferenceField(String method, JTextField field) {
        switch (method) {
            case "MPESA" -> mpesaReferenceField = field;
            case "CARD" -> cardReferenceField = field;
            case "BANK" -> bankReferenceField = field;
            default -> { }
        }
    }

    private void selectRecentMpesaPayment() {
        List<MpesaUdpBridge.PaymentNotice> payments = MpesaUdpBridge.getInstance().recentPayments();
        if (payments.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No M-Pesa payments received from BizFlow Bridge yet.\nEnsure the phone is on the same Wi-Fi network and points to UDP port " + MpesaUdpBridge.PORT + ".");
            return;
        }
        MpesaUdpBridge.PaymentNotice selected = (MpesaUdpBridge.PaymentNotice) JOptionPane.showInputDialog(
            this, "Select the matching M-Pesa payment:", "Recent M-Pesa payments", JOptionPane.PLAIN_MESSAGE,
            null, payments.toArray(), payments.get(0));
        if (selected != null && mpesaReferenceField != null) {
            mpesaReferenceField.setText(selected.code);
            mpesaReferenceField.requestFocusInWindow();
            mpesaReferenceField.selectAll();
        }
    }

    private JPanel buildSplitTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(12, 10, 10, 10));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(4, 0, 4, 0);

        JLabel hint = new JLabel(String.format("Split payment must total: KES %.2f", grandTotal));
        hint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        hint.setForeground(RetailThemeManager.TEXT_MUTED);
        g.gridx = 0; g.gridwidth = 2; g.gridy = 0; p.add(hint, g);
        g.gridwidth = 1;

        splitCashField  = addSplitRow(p, g, 1, "Cash (KES):");
        splitMpesaField = addSplitRow(p, g, 2, "M-Pesa (KES):");
        splitCardField  = addSplitRow(p, g, 3, "Card (KES):");

        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        splitRemainingLabel = new JLabel("Remaining: KES " + String.format("%.2f", grandTotal));
        splitRemainingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        splitRemainingLabel.setForeground(RetailThemeManager.DANGER);
        p.add(splitRemainingLabel, g);

        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSplitRemaining(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSplitRemaining(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSplitRemaining(); }
        };
        splitCashField.getDocument().addDocumentListener(dl);
        splitMpesaField.getDocument().addDocumentListener(dl);
        splitCardField.getDocument().addDocumentListener(dl);
        return p;
    }

    private JTextField addSplitRow(JPanel p, GridBagConstraints g, int row, String lbl) {
        g.gridx = 0; g.gridy = row; g.weightx = 0.4;
        JLabel l = new JLabel(lbl);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(l, g);
        g.gridx = 1; g.weightx = 0.6;
        JTextField f = RetailThemeManager.styledField();
        f.setText("0.00"); f.setPreferredSize(new Dimension(140, 36));
        p.add(f, g);
        return f;
    }

    private void updateSplitRemaining() {
        double total = parseSafe(splitCashField) + parseSafe(splitMpesaField) + parseSafe(splitCardField);
        double remaining = grandTotal - total;
        splitRemainingLabel.setText(String.format("Remaining: KES %.2f", remaining));
        splitRemainingLabel.setForeground(Math.abs(remaining) < 0.01 ? RetailThemeManager.ACCENT : RetailThemeManager.DANGER);
    }

    private double parseSafe(JTextField f) {
        try { return Double.parseDouble(f.getText().trim()); } catch (Exception e) { return 0; }
    }

    private void completeSale() {
        int tab = methodTabs.getSelectedIndex();
        String method; double tendered = grandTotal; String ref = "";

        try {
            switch (tab) {
                case 0 -> { // Cash
                    method = "CASH";
                    tendered = Double.parseDouble(cashTenderedField.getText().trim());
                    if (tendered < grandTotal) {
                        JOptionPane.showMessageDialog(this,
                            String.format("Cash tendered (%.2f) is less than total (%.2f)", tendered, grandTotal),
                            "Insufficient Cash", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                case 1 -> { method = "MPESA"; ref = textOf(mpesaReferenceField); }
                case 2 -> { method = "CARD";  ref = textOf(cardReferenceField); }
                case 3 -> { method = "BANK";  ref = textOf(bankReferenceField); }
                case 4 -> { // Split
                    method = "SPLIT";
                    double total = parseSafe(splitCashField) + parseSafe(splitMpesaField) + parseSafe(splitCardField);
                    if (Math.abs(total - grandTotal) > 0.01) {
                        JOptionPane.showMessageDialog(this,
                            String.format("Split amounts (%.2f) must equal total (%.2f)", total, grandTotal),
                            "Amount Mismatch", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    ref = String.format("Cash:%.2f,MPesa:%.2f,Card:%.2f",
                        parseSafe(splitCashField), parseSafe(splitMpesaField), parseSafe(splitCardField));
                    tendered = total;
                }
                default -> method = "CASH";
            }

            final String finalMethod = method;
            final double finalTendered = tendered;
            final String finalRef = ref;

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingWorker<Sale, Void> worker = new SwingWorker<>() {
                @Override protected Sale doInBackground() throws Exception {
                    return SaleService.getInstance().completeSale(
                        items, finalMethod, finalTendered, finalRef,
                        customerId, transactionDiscount, false);
                }
                @Override protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        completedSale = get();
                        dispose();
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("Insufficient stock")) {
                            // Ask for admin override
                            String pin = JOptionPane.showInputDialog(PaymentDialog.this,
                                "Stock warning: " + msg + "\n\nEnter Admin PIN to override:", "Stock Override",
                                JOptionPane.WARNING_MESSAGE);
                            if (pin != null) {
                                attemptWithOverride(finalMethod, finalTendered, finalRef);
                            }
                        } else {
                            JOptionPane.showMessageDialog(PaymentDialog.this,
                                "Sale failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            };
            worker.execute();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount entered", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void attemptWithOverride(String method, double tendered, String ref) {
        SwingWorker<Sale, Void> worker = new SwingWorker<>() {
            @Override protected Sale doInBackground() throws Exception {
                return SaleService.getInstance().completeSale(
                    items, method, tendered, ref, customerId, transactionDiscount, true);
            }
            @Override protected void done() {
                try { completedSale = get(); dispose(); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(PaymentDialog.this,
                        "Sale failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String textOf(JTextField field) {
        return field != null ? field.getText().trim() : "";
    }

    public Sale getCompletedSale() { return completedSale; }
    public boolean isSaleCompleted() { return completedSale != null; }
}
