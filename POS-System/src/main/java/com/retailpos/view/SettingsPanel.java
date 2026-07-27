package com.retailpos.view;

import com.retailpos.model.AppSettings;
import com.retailpos.model.User;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.repository.UserRepository;
import com.retailpos.service.AuthService;
import com.retailpos.service.PrintService;
import com.retailpos.service.LicenseService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import com.retailpos.util.AuditLogger;
import com.retailpos.util.PasswordUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SettingsPanel extends JPanel {
    private AppSettings settings;
    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final UserRepository userRepo = new UserRepository();

    // Store fields
    private JTextField storeNameF, storeAddressF, storePhoneF, storeFooterF, logoPathF;
    // Printer fields
    private JComboBox<String> printerCombo;
    private JComboBox<Integer> paperWidthCombo;
    // Tax / loyalty
    private JTextField taxRateF, loyaltyRateF;
    // Sync fields
    private JTextField syncUrlF, syncTokenF, syncUserF, syncPassF;
    private JCheckBox autoSyncCb;
    // Backup
    private JTextField backupPathF, backupTimeF;
    // Dark mode
    private JCheckBox darkModeCb;

    public SettingsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(RetailThemeManager.SURFACE);
        loadSettings();
        buildUI();
    }

    private void loadSettings() {
        try { settings = settingsRepo.load(); }
        catch (Exception e) { settings = new AppSettings(); }
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Store Info", buildStoreTab());
        tabs.addTab("Printer", buildPrinterTab());
        tabs.addTab("Tax & Loyalty", buildTaxTab());
        tabs.addTab("Sync", buildSyncTab());
        tabs.addTab("License", buildLicenseTab());
        tabs.addTab("Backup", buildBackupTab());
        tabs.addTab("Users", buildUsersTab());
        add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton saveBtn = RetailThemeManager.primaryButton("Save Settings", "save");
        saveBtn.addActionListener(e -> saveSettings());
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildLicenseTab() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = RetailThemeManager.headerLabel("BizFlow POS License");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel description = RetailThemeManager.subLabel(
            "Your free trial lasts 30 days. Paid licenses validate daily and support a seven-day offline grace period."
        );
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statusCard = RetailThemeManager.card();
        statusCard.setLayout(new GridLayout(4, 1, 0, 8));
        statusCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        statusCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel status = new JLabel("Checking license…");
        status.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel plan = new JLabel("Plan: —");
        JLabel expiry = new JLabel("Expiry: —");
        JLabel workstation = new JLabel(
            "Workstation: " + LicenseService.getInstance().getMachineId().substring(0, 16).toUpperCase() + "…"
        );
        statusCard.add(status);
        statusCard.add(plan);
        statusCard.add(expiry);
        statusCard.add(workstation);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton activate = RetailThemeManager.primaryButton("Activate or change license");
        JButton validate = RetailThemeManager.secondaryButton("Validate now");
        actions.add(activate);
        actions.add(validate);

        Runnable refresh = () -> new SwingWorker<LicenseService.LicenseSnapshot, Void>() {
            @Override protected LicenseService.LicenseSnapshot doInBackground() {
                return LicenseService.getInstance().checkAccess();
            }
            @Override protected void done() {
                try {
                    LicenseService.LicenseSnapshot snapshot = get();
                    status.setText(snapshot.getDisplayText());
                    status.setForeground(switch (snapshot.getStatus()) {
                        case ACTIVE -> RetailThemeManager.ACCENT;
                        case TRIAL, GRACE -> RetailThemeManager.WARNING;
                        case EXPIRED, INVALID -> RetailThemeManager.DANGER;
                    });
                    plan.setText("Plan: " + snapshot.getPlanName());
                    expiry.setText("Expires: " + (snapshot.getExpiresAt() == null
                        ? "—" : java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
                            .withZone(java.time.ZoneId.systemDefault()).format(snapshot.getExpiresAt())));
                } catch (Exception exception) {
                    status.setText("Could not load license status");
                    status.setForeground(RetailThemeManager.DANGER);
                }
            }
        }.execute();

        activate.addActionListener(event -> {
            LicenseService.LicenseSnapshot snapshot = LicenseService.getInstance().checkAccess();
            Window owner = SwingUtilities.getWindowAncestor(this);
            LicenseActivationDialog dialog = new LicenseActivationDialog(owner, snapshot, true);
            dialog.setVisible(true);
            refresh.run();
        });
        validate.addActionListener(event -> {
            validate.setEnabled(false);
            new SwingWorker<LicenseService.LicenseSnapshot, Void>() {
                @Override protected LicenseService.LicenseSnapshot doInBackground() throws Exception {
                    return LicenseService.getInstance().refreshNow();
                }
                @Override protected void done() {
                    validate.setEnabled(true);
                    try {
                        get();
                        refresh.run();
                    } catch (Exception exception) {
                        JOptionPane.showMessageDialog(
                            SettingsPanel.this,
                            exception.getCause() == null ? exception.getMessage()
                                : exception.getCause().getMessage(),
                            "License Validation",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }.execute();
        });

        content.add(title);
        content.add(Box.createVerticalStrut(6));
        content.add(description);
        content.add(Box.createVerticalStrut(20));
        content.add(statusCard);
        content.add(Box.createVerticalStrut(16));
        content.add(actions);
        refresh.run();
        return wrap(content);
    }

    private JPanel buildStoreTab() {
        JPanel p = formPanel();
        storeNameF    = row(p, 0,  "Store Name");
        storeAddressF = row(p, 2,  "Store Address");
        storePhoneF   = row(p, 4,  "Store Phone");
        storeFooterF  = row(p, 6,  "Receipt Footer Message");
        logoPathF     = row(p, 8,  "Logo Image Path");

        storeNameF.setText(settings.getStoreName());
        storeAddressF.setText(settings.getStoreAddress());
        storePhoneF.setText(settings.getStorePhone());
        storeFooterF.setText(settings.getStoreFooter());
        logoPathF.setText(settings.getLogoPath());

        JButton browseLogo = RetailThemeManager.secondaryButton("Upload logo");
        browseLogo.addActionListener(e -> chooseImage(logoPathF, "logo"));
        GridBagConstraints logoButtonConstraints = new GridBagConstraints();
        logoButtonConstraints.gridx = 0; logoButtonConstraints.gridy = 10;
        logoButtonConstraints.anchor = GridBagConstraints.WEST; logoButtonConstraints.insets = new Insets(5, 0, 5, 0);
        p.add(browseLogo, logoButtonConstraints);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 11; g.fill = GridBagConstraints.NONE; g.insets = new Insets(8, 0, 4, 0);
        JCheckBox darkCb = new JCheckBox("Enable Dark Mode");
        darkCb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        darkCb.setSelected(settings.isDarkMode());
        darkCb.addActionListener(e -> {
            settings.setDarkMode(darkCb.isSelected());
            RetailThemeManager.getInstance().apply(darkCb.isSelected());
        });
        darkModeCb = darkCb;
        p.add(darkCb, g);
        return wrap(p);
    }

    private JPanel buildPrinterTab() {
        JPanel p = formPanel();
        GridBagConstraints g = baseGbc(0);

        g.gridy = 0;
        p.add(boldLabel("Printer Name:"), g);
        g.gridy = 1; g.weightx = 1;
        List<String> printers = PrintService.getInstance().getAvailablePrinters();
        printers.add(0, "(Default printer)");
        printerCombo = new JComboBox<>(printers.toArray(new String[0]));
        printerCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (settings.getPrinterName() != null && !settings.getPrinterName().isBlank())
            printerCombo.setSelectedItem(settings.getPrinterName());
        p.add(printerCombo, g);

        g.gridy = 2; g.weightx = 0;
        p.add(boldLabel("Paper Width (mm):"), g);
        g.gridy = 3; g.weightx = 1;
        paperWidthCombo = new JComboBox<>(new Integer[]{58, 80});
        paperWidthCombo.setSelectedItem(settings.getPaperWidth());
        p.add(paperWidthCombo, g);

        g.gridy = 4; g.weightx = 0;
        JButton testPrint = RetailThemeManager.secondaryButton("Test Print", "print");
        testPrint.addActionListener(e -> {
            try { PrintService.getInstance().testPrint(settings); JOptionPane.showMessageDialog(this, "Test print sent"); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Test print failed: " + ex.getMessage()); }
        });
        p.add(testPrint, g);
        return wrap(p);
    }

    private JPanel buildTaxTab() {
        JPanel p = formPanel();
        taxRateF    = row(p, 0, "Default Tax Rate (%)");
        loyaltyRateF = row(p, 2, "Loyalty Points per KES 1 spent");
        taxRateF.setText(String.valueOf(settings.getTaxRate()));
        loyaltyRateF.setText(String.valueOf(settings.getLoyaltyEarningRate()));
        return wrap(p);
    }

    private JPanel buildSyncTab() {
        JPanel p = formPanel();
        syncUrlF   = row(p, 0, "Sync API URL");
        syncUserF  = row(p, 2, "API Username (for login)");
        syncPassF  = row(p, 4, "API Password");
        syncTokenF = row(p, 6, "API Token (auto-filled after first sync)");
        syncUrlF.setText(settings.getSyncApiUrl());
        syncUserF.setText(settings.getSyncApiUsername() != null ? settings.getSyncApiUsername() : "");
        syncPassF.setText(settings.getSyncApiPassword() != null ? settings.getSyncApiPassword() : "");
        syncTokenF.setText(settings.getSyncApiToken());
        GridBagConstraints hintG = baseGbc(8);
        JLabel syncHint = new JLabel("<html>For many computers, every workstation must use the same backend URL with the server IP, not localhost. Example: http://192.168.1.20/retail-pos-api/api/</html>");
        syncHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        syncHint.setForeground(RetailThemeManager.TEXT_MUTED);
        p.add(syncHint, hintG);
        GridBagConstraints g = baseGbc(9);
        autoSyncCb = new JCheckBox("Enable automatic synchronisation");
        autoSyncCb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        autoSyncCb.setSelected(settings.isAutoSync());
        p.add(autoSyncCb, g);
        // Manual sync trigger button
        g.gridy = 10; g.insets = new Insets(12, 0, 4, 0);
        JPanel syncActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        syncActions.setOpaque(false);
        JButton testConnection = RetailThemeManager.secondaryButton("Test Backend", "online");
        JButton syncNow = RetailThemeManager.secondaryButton("Sync Now", "sync");
        testConnection.addActionListener(e -> testSyncConnection(testConnection));
        syncNow.addActionListener(e -> {
            syncNow.setEnabled(false); syncNow.setText("Syncing…");
            com.retailpos.sync.SyncService.getInstance().triggerSync();
            javax.swing.Timer t = new javax.swing.Timer(3000, ev -> {
                syncNow.setEnabled(true); syncNow.setText("Sync Now");
            });
            t.setRepeats(false); t.start();
        });
        syncActions.add(testConnection);
        syncActions.add(syncNow);
        p.add(syncActions, g);
        return wrap(p);
    }

    private void testSyncConnection(JButton button) {
        String url = syncUrlF.getText().trim();
        button.setEnabled(false);
        button.setText("Testing...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return com.retailpos.sync.SyncService.getInstance().testConnection(url);
            }

            @Override protected void done() {
                button.setEnabled(true);
                button.setText("Test Backend");
                try {
                    JOptionPane.showMessageDialog(SettingsPanel.this, get(), "Backend Test", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(SettingsPanel.this, "Backend test failed: " + exception.getMessage(), "Backend Test", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private JPanel buildBackupTab() {
        JPanel p = formPanel();
        backupPathF = row(p, 0, "Backup Directory");
        backupTimeF = row(p, 2, "Daily Backup Time (HH:mm)");
        backupPathF.setText(settings.getBackupPath());
        backupTimeF.setText(settings.getBackupTime());

        GridBagConstraints g = baseGbc(4);
        JButton backupNow = RetailThemeManager.primaryButton("Backup Now", "backup");
        backupNow.addActionListener(e -> doManualBackup());
        p.add(backupNow, g);
        g.gridy = 5;
        JButton importTransactions = RetailThemeManager.secondaryButton("Import Transactions From Backup");
        importTransactions.addActionListener(e -> importTransactions());
        p.add(importTransactions, g);
        return wrap(p);
    }

    private JPanel buildUsersTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"Username", "Full Name", "Role", "Active"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable userTable = RetailThemeManager.styledTable(model);

        new SwingWorker<List<User>, Void>() {
            @Override protected List<User> doInBackground() throws Exception { return userRepo.findAll(); }
            @Override protected void done() {
                try {
                    for (User u : get())
                        model.addRow(new Object[]{u.getUsername(), u.getFullName(), u.getRole(), u.isActive() ? "Yes" : "No"});
                } catch (Exception ignored) {}
            }
        }.execute();

        p.add(RetailThemeManager.scroll(userTable), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        JButton addUser = RetailThemeManager.primaryButton("Add User", "add");
        addUser.addActionListener(e -> showAddUserDialog(model));
        btns.add(addUser);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    private void showAddUserDialog(DefaultTableModel model) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add User", true);
        d.setSize(440, 360); d.setLocationRelativeTo(this);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(4, 0, 4, 0);

        g.gridx = 0; g.gridy = 0; form.add(boldLabel("Username:"), g);
        g.gridy = 1; JTextField userF = RetailThemeManager.styledField(); userF.setPreferredSize(new Dimension(340, 36)); form.add(userF, g);
        g.gridy = 2; form.add(boldLabel("Full Name:"), g);
        g.gridy = 3; JTextField nameF = RetailThemeManager.styledField(); nameF.setPreferredSize(new Dimension(340, 36)); form.add(nameF, g);
        g.gridy = 4; form.add(boldLabel("Password:"), g);
        g.gridy = 5; JPasswordField passF = RetailThemeManager.styledPasswordField(); passF.setPreferredSize(new Dimension(340, 36)); form.add(passF, g);
        g.gridy = 6; form.add(boldLabel("Role:"), g);
        g.gridy = 7; JComboBox<String> roleCombo = new JComboBox<>(new String[]{"CASHIER", "ADMIN"}); form.add(roleCombo, g);

        d.add(form, BorderLayout.CENTER);
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = RetailThemeManager.secondaryButton("Cancel");
        JButton save = RetailThemeManager.primaryButton("Add User");
        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            String username = userF.getText().trim();
            String fullName = nameF.getText().trim();
            String password = new String(passF.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Username and password are required"); return;
            }
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    User u = new User();
                    u.setId(UUID.randomUUID().toString()); u.setUsername(username);
                    u.setPasswordHash(PasswordUtil.hash(password)); u.setRole(role);
                    u.setFullName(fullName.isEmpty() ? username : fullName);
                    u.setActive(true); u.setCreatedAt(LocalDateTime.now()); u.setUpdatedAt(LocalDateTime.now());
                    userRepo.insert(u);
                    AuditLogger.log(AuthService.getInstance().getCurrentUser().getId(),
                        AuditLogger.USER_CREATED, u.getId(), "username=" + username + ",role=" + role);
                    return null;
                }
                @Override protected void done() {
                    try { get(); d.dispose(); model.addRow(new Object[]{username, fullName, role, "Yes"}); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(d, "Error: " + ex.getMessage()); }
                }
            }.execute();
        });
        acts.add(cancel); acts.add(save);
        d.add(acts, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void saveSettings() {
        settings.setStoreName(storeNameF.getText().trim());
        settings.setStoreAddress(storeAddressF.getText().trim());
        settings.setStorePhone(storePhoneF.getText().trim());
        settings.setStoreFooter(storeFooterF.getText().trim());
        settings.setLogoPath(logoPathF.getText().trim());
        settings.setPrinterName(printerCombo.getSelectedItem() != null ? printerCombo.getSelectedItem().toString() : "");
        settings.setPaperWidth((Integer) paperWidthCombo.getSelectedItem());
        try { settings.setTaxRate(Double.parseDouble(taxRateF.getText().trim())); } catch (Exception ignored) {}
        try { settings.setLoyaltyEarningRate(Double.parseDouble(loyaltyRateF.getText().trim())); } catch (Exception ignored) {}
        settings.setSyncApiUrl(syncUrlF.getText().trim());
        settings.setSyncApiUsername(syncUserF.getText().trim());
        settings.setSyncApiPassword(syncPassF.getText().trim());
        settings.setSyncApiToken(syncTokenF.getText().trim());
        settings.setAutoSync(autoSyncCb.isSelected());
        settings.setBackupPath(backupPathF.getText().trim());
        settings.setBackupTime(backupTimeF.getText().trim());

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { settingsRepo.save(settings); return null; }
            @Override protected void done() {
                try {
                    get();
                    AuditLogger.log(AuthService.getInstance().getCurrentUser().getId(),
                        AuditLogger.SETTINGS_CHANGED, null, "Settings saved");
                    JOptionPane.showMessageDialog(SettingsPanel.this, "Settings saved successfully");
                } catch (Exception e) { JOptionPane.showMessageDialog(SettingsPanel.this, "Save failed: " + e.getMessage()); }
            }
        }.execute();
    }

    private void doManualBackup() {
        new SwingWorker<Void, Void>() {
            String backupFile;
            @Override protected Void doInBackground() throws Exception {
                java.nio.file.Path backupDir = com.retailpos.util.AppPaths.resolveDataPath(backupPathF.getText().trim());
                java.nio.file.Path backupPath = backupDir.resolve("retail_pos_backup_" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".db");
                com.retailpos.util.DatabaseManager.backupTo(backupPath);
                backupFile = backupPath.toString();
                AuditLogger.log(AuthService.getInstance().getCurrentUser().getId(),
                    AuditLogger.BACKUP_CREATED, null, "backup=" + backupFile);
                return null;
            }
            @Override protected void done() {
                try { get(); JOptionPane.showMessageDialog(SettingsPanel.this, "Backup created: " + backupFile); }
                catch (Exception e) {
                    AuditLogger.log(AuthService.getInstance().getCurrentUser().getId(),
                        AuditLogger.BACKUP_FAILED, null, e.getMessage());
                    JOptionPane.showMessageDialog(SettingsPanel.this, "Backup failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void importTransactions() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select BizFlow POS backup database");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQLite database", "db", "sqlite", "sqlite3"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        int confirmation = JOptionPane.showConfirmDialog(this,
            "Import sales from this backup? Existing sales are kept and duplicate sale IDs are skipped.",
            "Import transactions", JOptionPane.YES_NO_OPTION);
        if (confirmation != JOptionPane.YES_OPTION) return;
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return com.retailpos.service.TransactionImportService.importSales(chooser.getSelectedFile().toPath());
            }
            @Override protected void done() {
                try { JOptionPane.showMessageDialog(SettingsPanel.this, "Imported " + get() + " new transactions. Existing data was not replaced."); }
                catch (Exception exception) { JOptionPane.showMessageDialog(SettingsPanel.this, "Transaction import failed: " + exception.getMessage(), "Import error", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void chooseImage(JTextField destinationField, String prefix) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            destinationField.setText(com.retailpos.util.AppPaths.copyImage(chooser.getSelectedFile().toPath(), prefix).toString());
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Image upload failed: " + exception.getMessage(), "Upload error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helpers
    private JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        return p;
    }

    private JPanel wrap(JPanel p) {
        JScrollPane sp = new JScrollPane(p); sp.setBorder(null);
        JPanel w = new JPanel(new BorderLayout()); w.setBackground(Color.WHITE); w.add(sp); return w;
    }

    private JTextField row(JPanel p, int rowIndex, String label) {
        GridBagConstraints g = baseGbc(rowIndex);
        g.weightx = 0; p.add(boldLabel(label + ":"), g);
        g.gridy = rowIndex + 1; g.weightx = 1;
        JTextField f = RetailThemeManager.styledField();
        f.setPreferredSize(new Dimension(400, 36));
        p.add(f, g);
        return f;
    }

    private GridBagConstraints baseGbc(int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = row; g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1; g.insets = new Insets(4, 0, 4, 0);
        return g;
    }

    private JLabel boldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return l;
    }
}
