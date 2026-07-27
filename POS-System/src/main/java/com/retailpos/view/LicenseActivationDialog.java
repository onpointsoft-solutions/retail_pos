package com.retailpos.view;

import com.retailpos.service.LicenseService;
import com.retailpos.service.LicenseService.LicenseSnapshot;
import com.retailpos.ui.RetailThemeManager;
import java.awt.*;
import java.net.URI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LicenseActivationDialog extends JDialog {
    public static final String PACKAGES_URL =
        "https://pos.mobilemealscenter.co.ke/public/licensing.php";

    private final LicenseService licenseService = LicenseService.getInstance();
    private final JTextField backendUrlField = RetailThemeManager.styledField();
    private final JTextField licenseKeyField = RetailThemeManager.styledField();
    private final JLabel statusLabel = new JLabel(" ");
    private boolean activated;

    public LicenseActivationDialog(Window parent, LicenseSnapshot snapshot, boolean allowClose) {
        super(parent, "Activate BizFlow POS", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(allowClose ? DISPOSE_ON_CLOSE : DO_NOTHING_ON_CLOSE);
        setSize(840, 680);
        setMinimumSize(new Dimension(760, 620));
        setLocationRelativeTo(parent);
        setContentPane(buildContent(snapshot, allowClose));
    }

    public boolean isActivated() {
        return activated;
    }

    private JComponent buildContent(LicenseSnapshot snapshot, boolean allowClose) {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(RetailThemeManager.SURFACE);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel(snapshot.isAllowed() ? "LICENSE MANAGEMENT" : "ACTIVATION REQUIRED");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        eyebrow.setForeground(snapshot.isAllowed()
            ? RetailThemeManager.PRIMARY : RetailThemeManager.DANGER);
        JLabel title = new JLabel(snapshot.isAllowed()
            ? "Manage your BizFlow POS license"
            : "Your free trial or license has expired");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(RetailThemeManager.TEXT);
        JLabel description = new JLabel(
            "<html><div style='width:680px'>" + escape(snapshot.getMessage())
                + " Choose a package and enter the activation key issued to your shop.</div></html>"
        );
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        description.setForeground(RetailThemeManager.TEXT_MUTED);
        heading.add(eyebrow);
        heading.add(Box.createVerticalStrut(6));
        heading.add(title);
        heading.add(Box.createVerticalStrut(8));
        heading.add(description);
        root.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(buildPlans());
        center.add(Box.createVerticalStrut(18));
        center.add(buildActivationForm());
        root.add(new JScrollPane(center) {{
            setBorder(null);
            setOpaque(false);
            getViewport().setOpaque(false);
            getVerticalScrollBar().setUnitIncrement(16);
        }}, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        JButton packagesButton = RetailThemeManager.secondaryButton("View packages online");
        packagesButton.addActionListener(event -> openPackagesPage());
        actions.add(packagesButton, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        if (allowClose) {
            JButton close = RetailThemeManager.secondaryButton("Close");
            close.addActionListener(event -> dispose());
            right.add(close);
        } else {
            JButton exit = RetailThemeManager.secondaryButton("Exit BizFlow POS");
            exit.addActionListener(event -> dispose());
            right.add(exit);
        }
        JButton activate = RetailThemeManager.primaryButton("Activate license");
        activate.addActionListener(event -> activate(activate));
        right.add(activate);
        actions.add(right, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);
        return root;
    }

    private JComponent buildPlans() {
        JPanel plans = new JPanel(new GridLayout(1, 3, 12, 0));
        plans.setOpaque(false);
        plans.add(planCard("STARTER", "KES 2,500", "month", "1 computer",
            "POS, stock, receipts, reports and image sync", false));
        plans.add(planCard("BUSINESS", "KES 5,500", "month", "Up to 5 computers",
            "Full synchronization, M-Pesa Bridge and priority support", true));
        plans.add(planCard("ENTERPRISE", "KES 12,000", "month", "Up to 20 computers",
            "Multi-branch rollout, onboarding and priority support", false));
        return plans;
    }

    private JComponent planCard(
        String name,
        String price,
        String period,
        String devices,
        String description,
        boolean popular
    ) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(RetailThemeManager.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                popular ? RetailThemeManager.PRIMARY : RetailThemeManager.BORDER,
                popular ? 2 : 1,
                true
            ),
            new EmptyBorder(16, 16, 16, 16)
        ));
        if (popular) {
            JLabel badge = new JLabel("MOST POPULAR");
            badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            badge.setForeground(RetailThemeManager.PRIMARY);
            badge.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(badge);
            card.add(Box.createVerticalStrut(6));
        }
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(RetailThemeManager.TEXT);
        JLabel priceLabel = new JLabel(price);
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        priceLabel.setForeground(RetailThemeManager.PRIMARY);
        JLabel periodLabel = new JLabel("per " + period + " · " + devices);
        periodLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        periodLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        JLabel details = new JLabel("<html><div style='width:180px'>" + description + "</div></html>");
        details.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        details.setForeground(RetailThemeManager.TEXT_MUTED);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(priceLabel);
        card.add(periodLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(details);
        return card;
    }

    private JComponent buildActivationForm() {
        JPanel form = RetailThemeManager.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(5, 0, 5, 0);

        JLabel formTitle = new JLabel("Activate this workstation");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        formTitle.setForeground(RetailThemeManager.TEXT);
        constraints.gridy = 0;
        form.add(formTitle, constraints);

        constraints.gridy = 1;
        form.add(label("Licensing backend URL"), constraints);
        backendUrlField.setText(licenseService.getApiUrl());
        constraints.gridy = 2;
        form.add(backendUrlField, constraints);

        constraints.gridy = 3;
        form.add(label("License key"), constraints);
        licenseKeyField.setFont(new Font("Consolas", Font.BOLD, 15));
        constraints.gridy = 4;
        form.add(licenseKeyField, constraints);

        JLabel machine = new JLabel(
            "Workstation ID: " + licenseService.getMachineId().substring(0, 16).toUpperCase() + "…"
        );
        machine.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        machine.setForeground(RetailThemeManager.TEXT_MUTED);
        constraints.gridy = 5;
        form.add(machine, constraints);

        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        constraints.gridy = 6;
        form.add(statusLabel, constraints);
        return form;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(RetailThemeManager.TEXT);
        return label;
    }

    private void activate(JButton button) {
        String key = licenseKeyField.getText().trim();
        String apiUrl = backendUrlField.getText().trim();
        button.setEnabled(false);
        statusLabel.setText("Contacting licensing server…");
        statusLabel.setForeground(RetailThemeManager.PRIMARY);
        new SwingWorker<LicenseSnapshot, Void>() {
            @Override
            protected LicenseSnapshot doInBackground() throws Exception {
                return licenseService.activate(key, apiUrl);
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                try {
                    LicenseSnapshot result = get();
                    activated = result.isAllowed();
                    statusLabel.setText("Activated: " + result.getPlanName());
                    statusLabel.setForeground(RetailThemeManager.ACCENT);
                    JOptionPane.showMessageDialog(
                        LicenseActivationDialog.this,
                        "BizFlow POS is activated successfully on this workstation.",
                        "Activation Complete",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    dispose();
                } catch (Exception exception) {
                    String message = exception.getCause() != null
                        ? exception.getCause().getMessage() : exception.getMessage();
                    statusLabel.setText(message == null ? "Activation failed" : message);
                    statusLabel.setForeground(RetailThemeManager.DANGER);
                }
            }
        }.execute();
    }

    private void openPackagesPage() {
        try {
            Desktop.getDesktop().browse(URI.create(PACKAGES_URL));
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(
                this,
                "Open " + PACKAGES_URL + " to view license packages.",
                "License Packages",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
