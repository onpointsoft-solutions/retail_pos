package com.retailpos.view;

import com.retailpos.model.AppSettings;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.service.AuthService;
import com.retailpos.service.LicenseService;
import com.retailpos.sync.SyncService;
import com.retailpos.ui.AnimatedTabHost;
import com.retailpos.ui.Icons;
import com.retailpos.ui.Refreshable;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

public class MainFrame extends JFrame {

    private static final DateTimeFormatter TIME_FMT    = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final DateTimeFormatter REFRESH_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── status-bar labels ─────────────────────────────────────────────────────
    private JLabel statusUserLabel;
    private JLabel statusTimeLabel;
    private JLabel statusSyncLabel;
    private JLabel statusOnlineLabel;
    private JLabel statusLicenseLabel;
    private JLabel statusRefreshLabel;   // shows "Refreshed HH:mm:ss" or "● live"
    private JLabel statusPanelLabel;     // current panel description

    // ── core UI ───────────────────────────────────────────────────────────────
    private JTabbedPane    tabs;
    private AnimatedTabHost tabHost;     // wires animation onto tabs
    private SalesPanel     salesPanel;
    private AppSettings    settings;

    // ── smart refresh ─────────────────────────────────────────────────────────
    /** Single shared timer; ticks every second and decides which panel to refresh. */
    private Timer smartRefreshTimer;
    /** Seconds elapsed since the visible panel was last refreshed. */
    private int   secondsSinceRefresh = 0;

    // ─────────────────────────────────────────────────────────────────────────

    public MainFrame() {
        super("BizFlow POS");
        loadSettings();
        buildUI();
        com.retailpos.service.MpesaUdpBridge.getInstance().start();
        startSyncService();
        startStatusTimer();
        startSmartRefreshTimer();
        startLicenseEnforcementTimer();
        applyTheme();
    }

    private void loadSettings() {
        try { settings = new SettingsRepository().load(); }
        catch (Exception e) { settings = new AppSettings(); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI construction
    // ═════════════════════════════════════════════════════════════════════════

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
        root.add(buildHeader(), BorderLayout.NORTH);

        // ── tabs ──────────────────────────────────────────────────────────────
        tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "fill");
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        salesPanel = new SalesPanel();
        tabs.addTab("Sales", Icons.get("cart", 18), salesPanel, "F2 — Sales screen");

        if (AuthService.getInstance().isAdmin()) {
            tabs.addTab("Dashboard",  Icons.get("dashboard",  18), new DashboardPanel());
            tabs.addTab("Products",   Icons.get("products",   18), new ProductsPanel());
            tabs.addTab("Suppliers",  Icons.get("purchases",  18), new SuppliersPanel());
            tabs.addTab("Customers",  Icons.get("customers",  18), new CustomersPanel());
            tabs.addTab("Inventory",  Icons.get("inventory",  18), new InventoryPanel());
            tabs.addTab("Purchases",  Icons.get("purchases",  18), new PurchasesPanel());
            tabs.addTab("Services",   Icons.get("services",   18), new ServicesPanel());
            tabs.addTab("Analytics",  Icons.get("reports",    18), new AnalyticsPanel());
            tabs.addTab("Reports",    Icons.get("reports",    18), new ReportsPanel());
            tabs.addTab("Settings",   Icons.get("settings",   18), new SettingsPanel());
        } else {
            tabs.addTab("Customers",  Icons.get("customers",  18), new CustomersPanel());
        }

        // Attach animation host — installs change listener for slide/fade
        tabHost = new AnimatedTabHost(tabs);

        // Update status bar description + reset refresh counter on tab change
        tabs.addChangeListener(e -> {
            secondsSinceRefresh = 0;
            updatePanelLabel();
        });

        root.add(tabs, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
        registerGlobalShortcuts();
        updatePanelLabel();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Smart auto-refresh
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Ticks every second.  Each tick:
     * - increments the "stale" counter for the visible panel.
     * - when that counter reaches the panel's requested interval, calls
     *   {@link Refreshable#refreshData()} and resets the counter.
     * - updates the "last refreshed" label in the status bar.
     */
    private void startSmartRefreshTimer() {
        smartRefreshTimer = new Timer(1000, e -> {
            secondsSinceRefresh++;
            updateRefreshLabel();

            Component panel = tabs.getSelectedComponent();
            if (!(panel instanceof Refreshable r)) return;
            int interval = r.getRefreshIntervalSeconds();
            if (interval > 0 && secondsSinceRefresh >= interval) {
                secondsSinceRefresh = 0;
                SwingUtilities.invokeLater(r::refreshData);
                markRefreshTime();
            }
        });
        smartRefreshTimer.setCoalesce(true);
        smartRefreshTimer.start();
    }

    /** Refresh only the currently visible panel immediately (called after sync). */
    private void refreshVisiblePanel() {
        Component panel = tabs.getSelectedComponent();
        if (panel instanceof Refreshable r) {
            SwingUtilities.invokeLater(r::refreshData);
            secondsSinceRefresh = 0;
            markRefreshTime();
        }
    }

    /** Refresh all panels that implement Refreshable (called on full sync completion). */
    private void refreshAllPanels() {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component panel = tabs.getComponentAt(i);
            if (panel instanceof Refreshable r && r.getRefreshIntervalSeconds() > 0) {
                SwingUtilities.invokeLater(r::refreshData);
            }
        }
        markRefreshTime();
    }

    private LocalDateTime lastRefreshedAt = null;

    private void markRefreshTime() {
        lastRefreshedAt = LocalDateTime.now();
        updateRefreshLabel();
    }

    private void updateRefreshLabel() {
        if (statusRefreshLabel == null) return;
        if (lastRefreshedAt == null) {
            statusRefreshLabel.setText("Not yet refreshed");
            statusRefreshLabel.setForeground(RetailThemeManager.TEXT_MUTED);
            return;
        }
        Component panel = tabs.getSelectedComponent();
        int interval = panel instanceof Refreshable r ? r.getRefreshIntervalSeconds() : 0;

        if (interval == 0) {
            statusRefreshLabel.setText("● on-demand");
            statusRefreshLabel.setForeground(RetailThemeManager.TEXT_MUTED);
        } else {
            int remaining = Math.max(0, interval - secondsSinceRefresh);
            Color col = remaining < 10
                ? RetailThemeManager.WARNING
                : RetailThemeManager.ACCENT;
            statusRefreshLabel.setText("↻ " + REFRESH_FMT.format(lastRefreshedAt)
                + "  (next in " + remaining + "s)");
            statusRefreshLabel.setForeground(col);
        }
    }

    private void updatePanelLabel() {
        if (statusPanelLabel == null) return;
        Component panel = tabs.getSelectedComponent();
        String desc = panel instanceof Refreshable r ? r.getPanelDescription() : "";
        statusPanelLabel.setText(desc.isEmpty() ? "" : "  |  " + desc);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Header
    // ═════════════════════════════════════════════════════════════════════════

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(RetailThemeManager.NAVY);
        header.setBorder(new EmptyBorder(12, 22, 12, 22));
        header.setPreferredSize(new Dimension(0, 78));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        JLabel icon = new JLabel(Icons.get("cart", 26), SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(48, 48));
        icon.setOpaque(true);
        icon.setBackground(new Color(30, 64, 175));
        icon.setBorder(BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true));
        String logoPath = settings.getLogoPath();
        if (logoPath != null && !logoPath.isBlank() && new File(logoPath).isFile())
            icon.setIcon(new ImageIcon(new ImageIcon(logoPath).getImage()
                .getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        JLabel titleLbl = new JLabel(settings.getStoreName());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 21));
        titleLbl.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("POINT OF SALE  •  BUSINESS CONTROL CENTRE");
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        subtitle.setForeground(new Color(148, 163, 184));
        titleBlock.add(titleLbl);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);
        brand.add(icon); brand.add(titleBlock);
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        statusSyncLabel   = headerBadge("Sync ready", Icons.get("sync", 14));
        statusSyncLabel.setForeground(new Color(148, 163, 184));

        statusOnlineLabel = headerBadge("Offline", Icons.get("offline", 12));
        statusOnlineLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusOnlineLabel.setForeground(new Color(248, 113, 113));

        JLabel bridgeAddr = headerBadge(
            "Bridge  " + localIpv4Address() + ":45876", Icons.get("sync", 12));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setIcon(Icons.get("logout", 16));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBackground(new Color(51, 65, 85));
        logoutBtn.setOpaque(true);
        logoutBtn.setContentAreaFilled(true);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setBorder(new EmptyBorder(9, 12, 9, 12));
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> doLogout());

        right.add(statusSyncLabel);
        right.add(statusOnlineLabel);
        right.add(bridgeAddr);
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JLabel headerBadge(String text, Icon icon) {
        JLabel badge = new JLabel(text, icon, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(new Color(30, 41, 59));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(71, 85, 105), 1, true),
            new EmptyBorder(7, 10, 7, 10)));
        badge.setIconTextGap(6);
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        badge.setForeground(new Color(203, 213, 225));
        return badge;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Status bar
    // ═════════════════════════════════════════════════════════════════════════

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(new Color(30, 41, 59));
        bar.setBorder(new EmptyBorder(5, 16, 5, 16));

        // left cluster
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        AuthService auth = AuthService.getInstance();
        statusUserLabel = new JLabel(
            auth.getCurrentUser().getFullName() + " (" + auth.getCurrentUser().getRole() + ")");
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
            @Override public void mouseClicked(MouseEvent ev) {
                LicenseService.LicenseSnapshot snap = LicenseService.getInstance().checkAccess();
                new LicenseActivationDialog(MainFrame.this, snap, true).setVisible(true);
                updateLicenseStatus();
            }
        });
        updateLicenseStatus();

        // refresh indicator
        statusRefreshLabel = new JLabel("Not yet refreshed");
        statusRefreshLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusRefreshLabel.setForeground(RetailThemeManager.TEXT_MUTED);

        // panel description
        statusPanelLabel = new JLabel();
        statusPanelLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusPanelLabel.setForeground(new Color(100, 116, 139));

        left.add(statusUserLabel);
        left.add(statusTimeLabel);
        left.add(statusLicenseLabel);
        left.add(statusRefreshLabel);
        left.add(statusPanelLabel);
        bar.add(left, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("BizFlow POS v2.0  |  Offline-ready");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        versionLabel.setForeground(new Color(71, 85, 105));
        bar.add(versionLabel, BorderLayout.EAST);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  License
    // ═════════════════════════════════════════════════════════════════════════

    private void updateLicenseStatus() {
        applyLicenseSnapshot(LicenseService.getInstance().checkAccess());
    }

    private void applyLicenseSnapshot(LicenseService.LicenseSnapshot license) {
        statusLicenseLabel.setText(license.getDisplayText());
        statusLicenseLabel.setForeground(switch (license.getStatus()) {
            case ACTIVE          -> RetailThemeManager.ACCENT;
            case TRIAL, GRACE    -> RetailThemeManager.WARNING;
            case EXPIRED, INVALID-> RetailThemeManager.DANGER;
        });
    }

    private void startLicenseEnforcementTimer() {
        Timer t = new Timer(5 * 60_000, ev ->
            new SwingWorker<LicenseService.LicenseSnapshot, Void>() {
                @Override protected LicenseService.LicenseSnapshot doInBackground() {
                    return LicenseService.getInstance().checkAccess();
                }
                @Override protected void done() {
                    try {
                        LicenseService.LicenseSnapshot snap = get();
                        applyLicenseSnapshot(snap);
                        if (!snap.isAllowed()) {
                            LicenseActivationDialog dlg =
                                new LicenseActivationDialog(MainFrame.this, snap, false);
                            dlg.setVisible(true);
                            if (!dlg.isActivated()) {
                                SyncService.getInstance().stop();
                                com.retailpos.util.DatabaseManager.close();
                                dispose();
                                System.exit(0);
                            }
                            updateLicenseStatus();
                        }
                    } catch (Exception ignored) {}
                }
            }.execute());
        t.setCoalesce(true);
        t.start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Sync service  — refreshes panels after data lands
    // ═════════════════════════════════════════════════════════════════════════

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
                    // Only trigger panel refresh if new data actually arrived
                    if (message.contains("down:") || message.contains("up:")) {
                        refreshAllPanels();
                    }
                }
                case SYNCING -> {
                    statusSyncLabel.setIcon(Icons.get("syncing", 12));
                    statusSyncLabel.setText("Syncing…");
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

    // ═════════════════════════════════════════════════════════════════════════
    //  Misc timers / helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void startStatusTimer() {
        new Timer(30_000, e -> updateSessionTime()).start();
    }

    private void updateSessionTime() {
        LocalDateTime start = AuthService.getInstance().getSessionStart();
        if (start != null)
            statusTimeLabel.setText("  Session: " + TIME_FMT.format(start));
    }

    private void registerGlobalShortcuts() {
        KeyStroke f2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f2, "sales");
        getRootPane().getActionMap().put("sales", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                tabs.setSelectedIndex(0);
                salesPanel.focusSearch();
            }
        });
    }

    private String localIpv4Address() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "Unavailable";
    }

    private void applyTheme() {
        if (settings.isDarkMode()) RetailThemeManager.getInstance().apply(true);
    }

    private void doLogout() {
        int r = JOptionPane.showConfirmDialog(this,
            "Log out of this session?", "Logout", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        smartRefreshTimer.stop();
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
            } catch (Exception e) { System.exit(1); }
        });
    }
}
