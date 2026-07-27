package com.retailpos.view;

import com.retailpos.model.AppSettings;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.service.AuthService;
import com.retailpos.service.LicenseService;
import com.retailpos.sync.SyncService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainFrame extends JFrame {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private JLabel statusUserLabel;
    private JLabel statusTimeLabel;
    private JLabel statusSyncLabel;
    private JLabel statusOnlineLabel;
    private JLabel statusLicenseLabel;
    private JTabbedPane tabs;
    private SalesPanel salesPanel;
    private AppSettings settings;

    public MainFrame() {
        super("BizFlow POS");
        loadSettings();
        buildUI();
        com.retailpos.service.MpesaUdpBridge.getInstance().start();
        startSyncService();
        startStatusTimer();
        startLicenseEnforcementTimer();
        startPanelRefreshTimer();
        applyTheme();
    }

    private void loadSettings() {
        try { settings = new SettingsRepository().load(); }
        catch (Exception e) { settings = new AppSettings(); }
    }

    private void buildUI() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        setSize(1400, 860);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(MainFrame.this,
                    "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    SyncService.getInstance().stop();
                    com.retailpos.util.DatabaseManager.close();
                    System.exit(0);
                }
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(RetailThemeManager.SURFACE);

        // Header
        root.add(buildHeader(), BorderLayout.NORTH);

        // Tabs
        tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "fill");
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        salesPanel = new SalesPanel();
        JLabel salesTabIcon = new JLabel("Sales", Icons.get("cart", 18), SwingConstants.LEFT);
        tabs.addTab(null, Icons.get("cart", 18), salesPanel, "F2 — Sales screen");
        tabs.setTitleAt(0, "Sales");

        if (AuthService.getInstance().isAdmin()) {
            tabs.addTab("Dashboard",  Icons.get("dashboard",  18), new DashboardPanel());
            tabs.addTab("Products",   Icons.get("products",   18), new ProductsPanel());
            tabs.addTab("Suppliers",  Icons.get("purchases",  18), new SuppliersPanel());
            tabs.addTab("Customers",  Icons.get("customers",  18), new CustomersPanel());
            tabs.addTab("Inventory",  Icons.get("inventory",  18), new InventoryPanel());
            tabs.addTab("Purchases",  Icons.get("purchases",  18), new PurchasesPanel());
            tabs.addTab("Reports",    Icons.get("reports",    18), new ReportsPanel());
            tabs.addTab("Settings",   Icons.get("settings",   18), new SettingsPanel());
        } else {
            tabs.addTab("Customers",  Icons.get("customers",  18), new CustomersPanel());
        }

        root.add(tabs, BorderLayout.CENTER);

        // Status bar
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);

        // Global keyboard shortcuts
        registerGlobalShortcuts();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(RetailThemeManager.NAVY);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Logo + title
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setOpaque(false);
        // Header icon — drawn, not emoji
        JLabel icon = new JLabel(Icons.get("cart", 28));
        JLabel title = new JLabel("  " + settings.getStoreName() + "  |  Point of Sale");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        brand.add(icon); brand.add(title);
        header.add(brand, BorderLayout.WEST);

        // Right controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        statusSyncLabel = new JLabel("Sync: Ready");
        statusSyncLabel.setIcon(Icons.get("sync", 14));
        statusSyncLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusSyncLabel.setForeground(new Color(148, 163, 184));

        statusOnlineLabel = new JLabel("Offline");
        statusOnlineLabel.setIcon(Icons.get("offline", 12));
        statusOnlineLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusOnlineLabel.setForeground(new Color(248, 113, 113));

        JLabel bridgeAddress = new JLabel("Phone UDP: " + localIpv4Address() + ":45876");
        bridgeAddress.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bridgeAddress.setForeground(new Color(148, 163, 184));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setIcon(Icons.get("logout", 16));
        logoutBtn.setForeground(Color.WHITE); logoutBtn.setOpaque(false);
        logoutBtn.setContentAreaFilled(false); logoutBtn.setBorderPainted(false);
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> doLogout());

        right.add(statusSyncLabel); right.add(statusOnlineLabel); right.add(bridgeAddress); right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(new Color(30, 41, 59));
        bar.setBorder(new EmptyBorder(5, 16, 5, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);

        AuthService auth = AuthService.getInstance();
        statusUserLabel = new JLabel(auth.getCurrentUser().getFullName() +
            " (" + auth.getCurrentUser().getRole() + ")");
        statusUserLabel.setIcon(Icons.get("user", 14));
        statusUserLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusUserLabel.setForeground(new Color(148, 163, 184));

        statusTimeLabel = new JLabel();
        statusTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusTimeLabel.setForeground(new Color(148, 163, 184));
        updateSessionTime();

        statusLicenseLabel = new JLabel();
        statusLicenseLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLicenseLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        statusLicenseLabel.setToolTipText("Click to manage your BizFlow POS license");
        statusLicenseLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                LicenseService.LicenseSnapshot snapshot =
                    LicenseService.getInstance().checkAccess();
                LicenseActivationDialog dialog =
                    new LicenseActivationDialog(MainFrame.this, snapshot, true);
                dialog.setVisible(true);
                updateLicenseStatus();
            }
        });
        updateLicenseStatus();

        left.add(statusUserLabel);
        left.add(statusTimeLabel);
        left.add(statusLicenseLabel);
        bar.add(left, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("BizFlow POS v2.0  |  Offline-ready");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        versionLabel.setForeground(new Color(71, 85, 105));
        bar.add(versionLabel, BorderLayout.EAST);
        return bar;
    }

    private void updateLicenseStatus() {
        LicenseService.LicenseSnapshot license = LicenseService.getInstance().checkAccess();
        applyLicenseSnapshot(license);
    }

    private void applyLicenseSnapshot(LicenseService.LicenseSnapshot license) {
        statusLicenseLabel.setText(license.getDisplayText());
        statusLicenseLabel.setForeground(switch (license.getStatus()) {
            case ACTIVE -> RetailThemeManager.ACCENT;
            case TRIAL, GRACE -> RetailThemeManager.WARNING;
            case EXPIRED, INVALID -> RetailThemeManager.DANGER;
        });
    }

    private void startLicenseEnforcementTimer() {
        Timer timer = new Timer(5 * 60 * 1000, event ->
            new SwingWorker<LicenseService.LicenseSnapshot, Void>() {
                @Override protected LicenseService.LicenseSnapshot doInBackground() {
                    return LicenseService.getInstance().checkAccess();
                }

                @Override protected void done() {
                    try {
                        LicenseService.LicenseSnapshot snapshot = get();
                        applyLicenseSnapshot(snapshot);
                        if (!snapshot.isAllowed()) {
                            LicenseActivationDialog dialog =
                                new LicenseActivationDialog(MainFrame.this, snapshot, false);
                            dialog.setVisible(true);
                            if (!dialog.isActivated()) {
                                SyncService.getInstance().stop();
                                com.retailpos.util.DatabaseManager.close();
                                dispose();
                                System.exit(0);
                            }
                            updateLicenseStatus();
                        }
                    } catch (Exception ignored) {
                        // Cached paid-license grace and local trial rules are handled by LicenseService.
                    }
                }
            }.execute()
        );
        timer.setCoalesce(true);
        timer.start();
    }

    private void registerGlobalShortcuts() {
        // F2: focus sales tab
        KeyStroke f2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f2, "sales");
        getRootPane().getActionMap().put("sales", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                tabs.setSelectedIndex(0);
                salesPanel.focusSearch();
            }
        });
    }

    private void startSyncService() {
        SyncService sync = SyncService.getInstance();
        sync.addStateListener((state, message) -> SwingUtilities.invokeLater(() -> {
            switch (state) {
                case IDLE -> {
                    statusSyncLabel.setIcon(Icons.get("online", 12));
                    statusSyncLabel.setText(message);
                    statusSyncLabel.setForeground(new Color(148, 163, 184));
                    statusOnlineLabel.setIcon(Icons.get("online", 12));
                    statusOnlineLabel.setText("Online");
                    statusOnlineLabel.setForeground(new Color(74, 222, 128));
                    // Refresh all panels when sync completes with new data
                    if (message.contains("down:") || message.contains("up:")) {
                        refreshAllPanels();
                    }
                }
                case SYNCING -> {
                    statusSyncLabel.setIcon(Icons.get("syncing", 12));
                    statusSyncLabel.setText("Syncing...");
                    statusSyncLabel.setForeground(new Color(251, 191, 36));
                    statusOnlineLabel.setIcon(Icons.get("online", 12));
                    statusOnlineLabel.setText("Online");
                    statusOnlineLabel.setForeground(new Color(74, 222, 128));
                }
                case ERROR -> {
                    statusSyncLabel.setIcon(Icons.get("warning", 14));
                    statusSyncLabel.setText(message);
                    statusSyncLabel.setForeground(new Color(248, 113, 113));
                    statusOnlineLabel.setIcon(Icons.get("offline", 12));
                    statusOnlineLabel.setText("Offline");
                    statusOnlineLabel.setForeground(new Color(248, 113, 113));
                }
            }
        }));
        if (settings.isAutoSync()) sync.start();
    }

    private void startStatusTimer() {
        Timer t = new Timer(30000, e -> updateSessionTime());
        t.start();
    }

    private void startPanelRefreshTimer() {
        // Panel refresh now triggered by sync events when data changes
        // Removed 5-second auto-refresh to reduce unnecessary updates
    }

    private void refreshVisiblePanel() {
        Component panel = tabs.getSelectedComponent();
        if (panel == null || !panel.isShowing()) return;
        for (String methodName : new String[] {"loadData", "loadAll", "doSearch", "loadCategoriesAndProducts", "loadSettings", "generateReport"}) {
            try {
                java.lang.reflect.Method method = panel.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                method.invoke(panel);
                return;
            } catch (NoSuchMethodException ignored) {
                // Try the next conventional panel refresh method.
            } catch (Exception ignored) {
                return;
            }
        }
    }

    private void refreshAllPanels() {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component panel = tabs.getComponentAt(i);
            if (panel == null) continue;
            for (String methodName : new String[] {"loadData", "loadAll", "doSearch", "loadCategoriesAndProducts", "loadSettings", "generateReport"}) {
                try {
                    java.lang.reflect.Method method = panel.getClass().getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    method.invoke(panel);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // Try the next conventional panel refresh method.
                } catch (Exception ignored) {
                    break;
                }
            }
        }
    }

    private void updateSessionTime() {
        LocalDateTime start = AuthService.getInstance().getSessionStart();
        if (start != null) {
            statusTimeLabel.setText("  Session since: " + TIME_FMT.format(start));
        }
    }

    private String localIpv4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                Enumeration<java.net.InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) return address.getHostAddress();
                }
            }
        } catch (Exception ignored) { }
        return "Unavailable";
    }

    private void applyTheme() {
        if (settings.isDarkMode()) RetailThemeManager.getInstance().apply(true);
    }

    private void doLogout() {
        int r = JOptionPane.showConfirmDialog(this,
            "Log out of this session?", "Logout", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        AuthService.getInstance().logout();
        SyncService.getInstance().stop();
        dispose();
        SwingUtilities.invokeLater(() -> {
            try {
                com.formdev.flatlaf.FlatLightLaf.setup();
                LoginDialog login = new LoginDialog(null);
                login.setVisible(true);
                if (login.isLoginSuccessful()) {
                    new MainFrame().setVisible(true);
                } else {
                    com.retailpos.util.DatabaseManager.close();
                    System.exit(0);
                }
            } catch (Exception e) {
                System.exit(1);
            }
        });
    }
}
