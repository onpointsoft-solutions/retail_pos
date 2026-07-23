package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main POS Application for Mobile Meals Center
 * Desktop Point of Sale system for restaurants
 */
public class MobileMealsPOS extends JFrame {
    
    // Main UI Components
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel contentPanel;
    private JPanel sidebarPanel;
    private JPanel statusBar;
    
    // Header Components
    private JLabel lblTitle;
    private JLabel lblRestaurantName;
    private JLabel lblDateTime;
    private JLabel lblUser;
    
    // Sidebar Components
    private JButton btnDashboard;
    private JButton btnOrders;
    private JButton btnMenu;
    private JButton btnReports;
    private JButton btnSettings;
    private JButton btnLogout;
    
    // Content Components
    private CardLayout cardLayout;
    private JPanel dashboardPanel;
    private JPanel ordersPanel;
    private JPanel menuPanel;
    private JPanel reportsPanel;
    private JPanel settingsPanel;
    
    // Status Bar Components
    private JLabel lblStatus;
    private JLabel lblConnectionStatus;
    private JLabel lblOrderCount;
    
    // Data Models
    private RestaurantSession restaurantSession;
    private OrderManager orderManager;
    private MenuManager menuManager;
    private ReportManager reportManager;
    
    // Configuration
    private static final String APP_NAME = "Mobile Meals POS";
    private static final String VERSION = "1.0.0";
    private static final String API_BASE_URL = "https://www.mobilemealscenter.co.ke/api/";
    
    public MobileMealsPOS() {
        initializePOS();
        setupUI();
        setupEventHandlers();
        loadInitialData();
    }
    
    private void initializePOS() {
        setTitle(APP_NAME + " v" + VERSION);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null, 100, 50);
        
        // Initialize managers
        restaurantSession = new RestaurantSession();
        orderManager = new OrderManager();
        menuManager = new MenuManager();
        reportManager = new ReportManager();
        
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupUI() {
        mainPanel = new JPanel(new BorderLayout());
        setupHeaderPanel();
        setupSidebarPanel();
        setupContentPanel();
        setupStatusBar();
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(sidebarPanel, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void setupHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setPreferredSize(new Dimension(0, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title Section
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleSection.setOpaque(false);
        
        lblTitle = new JLabel(APP_NAME);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitle.setForeground(Color.WHITE);
        
        lblRestaurantName = new JLabel("Restaurant Name");
        lblRestaurantName.setFont(new Font("Arial", Font.BOLD, 18));
        lblRestaurantName.setForeground(Color.WHITE);
        
        titleSection.add(lblTitle);
        titleSection.add(Box.createHorizontalStrut(20));
        titleSection.add(lblRestaurantName);
        
        // Status Section
        JPanel statusSection = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusSection.setOpaque(false);
        
        lblDateTime = new JLabel();
        lblDateTime.setFont(new Font("Arial", Font.PLAIN, 14));
        lblDateTime.setForeground(Color.WHITE);
        
        lblUser = new JLabel("Staff: Not Logged In");
        lblUser.setFont(new Font("Arial", Font.PLAIN, 14));
        lblUser.setForeground(Color.WHITE);
        
        statusSection.add(lblDateTime);
        statusSection.add(Box.createHorizontalStrut(20));
        statusSection.add(lblUser);
        
        headerPanel.add(titleSection, BorderLayout.WEST);
        headerPanel.add(statusSection, BorderLayout.EAST);
        
        // Update date/time
        updateDateTime();
        Timer timer = new Timer(1000, e -> updateDateTime());
        timer.start();
    }
    
    private void setupSidebarPanel() {
        sidebarPanel = new JPanel();
        sidebarPanel.setPreferredSize(new Dimension(200, 0));
        sidebarPanel.setBackground(Color.LIGHT_GRAY);
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
        
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Navigation Buttons
        btnDashboard = createNavButton("Dashboard", "📊", "Dashboard", Color.BLUE);
        btnOrders = createNavButton("Orders", "📋", "Orders", Color.GREEN);
        btnMenu = createNavButton("Menu", "📱", "Menu Management", Color.ORANGE);
        btnReports = createNavButton("Reports", "📈", "Reports", Color.PURPLE);
        btnSettings = createNavButton("Settings", "⚙️", "Settings", Color.GRAY);
        btnLogout = createNavButton("Logout", "🚪", "Logout", Color.RED);
        
        navPanel.add(Box.createVerticalGlue());
        navPanel.add(btnDashboard);
        navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnOrders);
        navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnMenu);
        navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnReports);
        navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnSettings);
        navPanel.add(Box.createVerticalGlue());
        navPanel.add(btnLogout);
        
        sidebarPanel.add(navPanel, BorderLayout.NORTH);
    }
    
    private JButton createNavButton(String title, String icon, String tooltip, Color color) {
        JButton button = new JButton(icon + " " " + title);
        button.setToolTipText(tooltip);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        button.setFocusPainted(false);
        button.setMaximumSize(Integer.MAX_VALUE, button.getPreferredSize().height);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }
    
    private void setupContentPanel() {
        contentPanel = new JPanel();
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);
        
        // Create panels
        dashboardPanel = createDashboardPanel();
        ordersPanel = createOrdersPanel();
        menuPanel = createMenuPanel();
        reportsPanel = createReportsPanel();
        settingsPanel = createSettingsPanel();
        
        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(ordersPanel, "Orders");
        contentPanel.add(menuPanel, "Menu");
        contentPanel.add(reportsPanel, "Reports");
        contentPanel.add(settingsPanel, "Settings");
        
        // Show dashboard by default
        showPanel("Dashboard");
    }
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Dashboard Stats
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Today's Statistics"));
        
        JLabel lblTotalOrders = new JLabel("0", SwingConstants.CENTER);
        JLabel lblTotalRevenue = new JLabel("KES 0.00", SwingConstants.CENTER);
        JLabel lblPendingOrders = new JLabel("0", SwingConstants.CENTER);
        JLabel lblCompletedOrders = new JLabel("0", SwingConstants.CENTER);
        
        statsPanel.add(createStatCard("Total Orders", lblTotalOrders));
        statsPanel.add(createStatCard("Total Revenue", lblTotalRevenue));
        statsPanel.add(createStatCard("Pending Orders", lblPendingOrders));
        statsPanel.add(createStatCard("Completed Orders", lblCompletedOrders));
        
        // Recent Orders Table
        JPanel recentOrdersPanel = new JPanel(new BorderLayout());
        recentOrdersPanel.setBorder(BorderFactory.createTitledBorder("Recent Orders"));
        
        String[] columns = {"Order #", "Customer", "Status", "Amount", "Time"};
        Object[][] data = {};
        
        JTable recentOrdersTable = new JTable(data, columns);
        JScrollPane scrollPane = new JScrollPane(recentOrdersTable);
        
        recentOrdersPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(statsPanel, BorderLayout.NORTH);
        panel.add(recentOrdersPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Orders Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Orders Management"));
        headerPanel.add(Box.createHorizontalGlue());
        
        JButton btnNewOrder = new JButton("New Order");
        JButton btnRefresh = new JButton("Refresh");
        JButton btnFilter = new JButton("Filter");
        
        headerPanel.add(btnNewOrder);
        headerPanel.add(btnRefresh);
        headerPanel.add(btnFilter);
        
        // Orders Table
        String[] columns = {"Order #", "Customer", "Items", "Total", "Status", "Time", "Actions"};
        Object[][] data = {};
        
        JTable ordersTable = new JTable(data, columns);
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Menu Categories
        JPanel categoriesPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        categoriesPanel.setBorder(BorderFactory.createTitledBorder("Menu Categories"));
        
        // Menu Items Table
        JPanel menuItemsPanel = new JPanel(new BorderLayout());
        menuItemsPanel.setBorder(BorderFactory.createTitledBorder("Menu Items"));
        
        String[] columns = {"Code", "Name", "Category", "Price", "Status"};
        Object[][] data = {};
        
        JTable menuTable = new JTable(data, columns);
        JScrollPane scrollPane = new JScrollPane(menuTable);
        
        menuItemsPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(categoriesPanel, BorderLayout.NORTH);
        panel.add(menuItemsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Report Type Selection
        JPanel reportTypePanel = new JPanel(new GridLayout(1, 4, 10, 10));
        reportTypePanel.setBorder(BorderFactory.createTitledBorder("Select Report Type"));
        
        JRadioButton rbSalesReport = new JRadioButton("Sales Report");
        JRadioButton rbOrderReport = new JRadioButton("Order Report");
        JRadioButton rbRevenueReport = new JRadioButton("Revenue Report");
        JRadioButton rbInventoryReport = new JRadioButton("Inventory Report");
        
        ButtonGroup reportGroup = new ButtonGroup();
        reportGroup.add(rbSalesReport);
        reportGroup.add(rbOrderReport);
        reportGroup.add(rbRevenueReport);
        reportGroup.add(rbInventoryReport);
        
        reportTypePanel.add(rbSalesReport);
        reportTypePanel.add(rbOrderReport);
        reportTypePanel.add(rbRevenueReport);
        reportTypePanel.add(rbInventoryReport);
        
        // Report Display Area
        JPanel reportDisplayPanel = new JPanel(new BorderLayout());
        reportDisplayPanel.setBorder(BorderFactory.createTitledBorder("Report Preview"));
        
        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(reportArea);
        reportDisplayPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Report Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnGenerateReport = new JButton("Generate Report");
        JButton btnPrintReport = new JButton("Print Report");
        JButton btnExportPDF = new JButton("Export PDF");
        
        actionsPanel.add(btnGenerateReport);
        actionsPanel.add(btnPrintReport);
        actionsPanel.add(btnExportPDF);
        
        panel.add(reportTypePanel, BorderLayout.NORTH);
        panel.add(reportDisplayPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Settings Categories
        JPanel settingsCategoriesPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        settingsCategoriesPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        
        JRadioButton rbGeneral = new JRadioButton("General");
        JRadioButton rbAPI = new JRadioButton("API Configuration");
        JRadioButton rbPrinter = new JRadioButton("Printer Settings");
        JRadioButton rbDatabase = new JRadioButton("Database");
        
        ButtonGroup settingsGroup = new ButtonGroup();
        settingsGroup.add(rbGeneral);
        settingsGroup.add(rbAPI);
        settingsGroup.add(rbPrinter);
        settingsGroup.add(rbDatabase);
        
        settingsCategoriesPanel.add(rbGeneral);
        settingsCategoriesPanel.add(rbAPI);
        settingsCategoriesPanel.add(rbPrinter);
        settingsCategoriesPanel.add(rbDatabase);
        
        // Settings Content
        JPanel settingsContentPanel = new JPanel(new BorderLayout());
        settingsContentPanel.setBorder(BorderFactory.createTitledBorder("Settings Configuration"));
        
        JPanel generalSettings = new JPanel(new GridLayout(0, 2, 10, 10));
        generalSettings.add(new JLabel("Restaurant Name:"));
        generalSettings.add(new JTextField());
        generalSettings.add(new JLabel("Restaurant Phone:"));
        generalSettings.add(new JTextField());
        generalSettings.add(new JLabel("Restaurant Address:"));
        generalSettings.add(new JTextField());
        generalSettings.add(new JLabel("Default Currency:"));
        generalSettings.add(new JComboBox<>(new String[]{"KES", "USD", "EUR"}));
        
        settingsContentPanel.add(generalSettings);
        
        panel.add(settingsCategoriesPanel, BorderLayout.NORTH);
        panel.add(settingsContentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupStatusBar() {
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(240, 240, 240));
        statusBar.setPreferredSize(new Dimension(0, 25));
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        lblStatus = new JLabel("Ready");
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        
        lblConnectionStatus = new JLabel("🟢 Connected");
        lblConnectionStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        
        lblOrderCount = new JLabel("Orders: 0");
        lblOrderCount.setFont(new Font("Arial", Font.PLAIN, 12));
        
        statusBar.add(lblStatus, BorderLayout.WEST);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(lblConnectionStatus);
        statusBar.add(Box.createHorizontalStrut(20));
        statusBar.add(lblOrderCount);
    }
    
    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        card.setPreferredSize(new Dimension(0, 80));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setForeground(Color.DARK_GRAY);
        
        valueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setForeground(Color.BLUE);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void showPanel(String panelName) {
        cardLayout.show(contentPanel, panelName);
        
        // Update button states
        updateNavButtonStates(panelName);
    }
    
    private void updateNavButtonStates(String activePanel) {
        // Reset all buttons
        resetNavButton(btnDashboard);
        resetNavButton(btnOrders);
        resetNavButton(btnMenu);
        resetNavButton(btnReports);
        resetNavButton(btnSettings);
        resetNavButton(btnLogout);
        
        // Highlight active button
        switch (activePanel) {
            case "Dashboard":
                highlightNavButton(btnDashboard);
                break;
            case "Orders":
                highlightNavButton(btnOrders);
                break;
            case "Menu":
                highlightNavButton(btnMenu);
                break;
            case "Reports":
                highlightNavButton(btnReports);
                break;
            case "Settings":
                highlightNavButton(btnSettings);
                break;
        }
    }
    
    private void resetNavButton(JButton button) {
        button.setBackground(Color.LIGHT_GRAY);
    }
    
    private void highlightNavButton(JButton button) {
        button.setBackground(new Color(41, 128, 185));
    }
    
    private void setupEventHandlers() {
        // Navigation buttons
        btnDashboard.addActionListener(e -> showPanel("Dashboard"));
        btnOrders.addActionListener(e -> showPanel("Orders"));
        btnMenu.addActionListener(e -> showPanel("Menu"));
        btnReports.addActionListener(e -> showPanel("Reports"));
        btnSettings.addActionListener(e -> showPanel("Settings"));
        btnLogout.addActionListener(e -> logout());
        
        // Window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(
                    MobileMealsPOS.this,
                    "Are you sure you want to exit?",
                    "Confirm Exit",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });
    }
    
    private void loadInitialData() {
        // Load initial data from API
        loadRestaurantInfo();
        loadTodayStats();
        loadRecentOrders();
        loadMenuItems();
        
        // Start background refresh
        startBackgroundRefresh();
    }
    
    private void loadRestaurantInfo() {
        // TODO: Load restaurant info from API
        lblRestaurantName.setText("Restaurant Name");
    }
    
    private void loadTodayStats() {
        // TODO: Load today's statistics from API
        // This would make API calls to get today's orders and revenue
    }
    
    private void loadRecentOrders() {
        // TODO: Load recent orders from API
        // This would make API calls to get recent orders
    }
    
    private void loadMenuItems() {
        // TODO: Load menu items from API
        // This would make API calls to get restaurant menu
    }
    
    private void startBackgroundRefresh() {
        // Start background thread for data refresh
        Timer refreshTimer = new Timer(30000, e -> {
            // Refresh data every 30 seconds
            loadTodayStats();
            loadRecentOrders();
        });
        refreshTimer.start();
    }
    
    private void updateDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        lblDateTime.setText(formatter.format(LocalDateTime.now()));
    }
    
    private void logout() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            restaurantSession.logout();
            dispose();
            // Show login dialog or close application
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new MobileMealsPOS().setVisible(true);
        });
    }
}
