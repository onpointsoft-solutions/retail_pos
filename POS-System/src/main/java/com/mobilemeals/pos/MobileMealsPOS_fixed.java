package com.mobilemeals.pos;

import com.mobilemeals.pos.ui.ThemeManager;
import com.mobilemeals.pos.RestaurantSession;
import com.mobilemeals.pos.OrderManager;
import com.mobilemeals.pos.MenuManager;
import com.mobilemeals.pos.ReportManager;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main POS Application for Mobile Meals Center
 * Desktop Point of Sale system for restaurants
 */
class MobileMealsPOSFixed extends JFrame {
    
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
    private JLabel lblStatus;
    
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
    private JLabel lblConnectionStatus;
    private JLabel lblOrderCount;
    private JLabel lblSystemStatus;
    
    // Data Models
    private RestaurantSession restaurantSession;
    private OrderManager orderManager;
    private MenuManager menuManager;
    private ReportManager reportManager;
    
    // Configuration
    private static final String APP_NAME = "Mobile Meals POS";
    private static final String VERSION = "1.0.0";
    
    public MobileMealsPOSFixed() {
        initializePOS();
        setupUI();
        setupEventHandlers();
        loadInitialData();
        setupMenuBar();
    }
    
    private void initializePOS() {
        setTitle(APP_NAME + " v" + VERSION);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1200, 700));
        
        // Initialize managers
        restaurantSession = new RestaurantSession();
        orderManager = new OrderManager();
        menuManager = new MenuManager();
        reportManager = new ReportManager();
        
        // Apply Mobile Meals theme
        ThemeManager.getInstance().initializeTheme();
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
        headerPanel = ThemeManager.createHeaderPanel();
        
        // Left side - Title and Restaurant
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        
        lblTitle = ThemeManager.createHeaderLabel(APP_NAME);
        lblTitle.setForeground(Color.WHITE);
        
        lblRestaurantName = ThemeManager.createSubHeaderLabel("Restaurant Name");
        lblRestaurantName.setForeground(Color.WHITE);
        
        leftPanel.add(lblTitle);
        leftPanel.add(Box.createHorizontalStrut(20));
        leftPanel.add(lblRestaurantName);
        
        // Right side - User info and status
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        
        lblStatus = ThemeManager.createBodyLabel("Online");
        lblStatus.setForeground(Color.WHITE);
        
        lblUser = ThemeManager.createBodyLabel("Staff: Not Logged In");
        lblUser.setForeground(Color.WHITE);
        
        lblDateTime = ThemeManager.createBodyLabel("");
        lblDateTime.setForeground(Color.WHITE);
        
        rightPanel.add(lblStatus);
        rightPanel.add(Box.createHorizontalStrut(20));
        rightPanel.add(lblUser);
        rightPanel.add(Box.createHorizontalStrut(20));
        rightPanel.add(lblDateTime);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        // Update date/time
        updateDateTime();
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> updateDateTime());
        timer.start();
    }
    
    private void setupSidebarPanel() {
        sidebarPanel = ThemeManager.createSidePanel();
        sidebarPanel.setPreferredSize(new Dimension(250, 0));
        
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        navPanel.setOpaque(false);
        
        // Logo/Title
        JLabel logoLabel = ThemeManager.createHeaderLabel("Mobile Meals");
        logoLabel.setForeground(ThemeManager.PRIMARY_COLOR);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        navPanel.add(logoLabel);
        navPanel.add(Box.createVerticalStrut(30));
        
        // Navigation Buttons
        btnDashboard = createNavButton("Dashboard", "Dashboard");
        btnOrders = createNavButton("Orders", "Orders");
        btnMenu = createNavButton("Menu", "Menu Management");
        btnReports = createNavButton("Reports", "Reports");
        btnSettings = createNavButton("Settings", "Settings");
        btnLogout = createNavButton("Logout", "Logout", ThemeManager.ACCENT_COLOR);
        
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
    
    private JButton createNavButton(String title, String tooltip, Color color) {
        JButton button = new JButton(title);
        button.setToolTipText(tooltip);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        button.setFocusPainted(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private JButton createNavButton(String title, String tooltip) {
        return createNavButton(title, tooltip, ThemeManager.PRIMARY_COLOR);
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
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout());
        
        // Header
        JLabel headerLabel = ThemeManager.createHeaderLabel("Dashboard");
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // Stats Grid
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        statsPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        // Today's Orders
        JPanel ordersCard = createStatCard("Today's Orders", "0", ThemeManager.PRIMARY_COLOR);
        statsPanel.add(ordersCard);
        
        // Today's Revenue
        JPanel revenueCard = createStatCard("Today's Revenue", "KES 0.00", ThemeManager.SUCCESS_COLOR);
        statsPanel.add(revenueCard);
        
        // Pending Orders
        JPanel pendingCard = createStatCard("Pending Orders", "0", ThemeManager.WARNING_COLOR);
        statsPanel.add(pendingCard);
        
        // Completed Orders
        JPanel completedCard = createStatCard("Completed Orders", "0", ThemeManager.INFO_COLOR);
        statsPanel.add(completedCard);
        
        // Recent Orders Table
        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBorder(BorderFactory.createTitledBorder("Recent Orders"));
        
        String[] columns = {"Order #", "Customer", "Status", "Amount", "Time"};
        Object[][] data = {};
        
        JTable recentOrdersTable = new JTable(data, columns);
        recentOrdersTable.setRowHeight(30);
        recentOrdersTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recentOrdersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JScrollPane scrollPane = new JScrollPane(recentOrdersTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        recentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Quick Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JButton btnNewOrder = ThemeManager.createPrimaryButton("New Order");
        JButton btnRefresh = ThemeManager.createSecondaryButton("Refresh");
        JButton btnViewOrders = ThemeManager.createSuccessButton("View All Orders");
        
        actionsPanel.add(btnNewOrder);
        actionsPanel.add(btnRefresh);
        actionsPanel.add(btnViewOrders);
        
        // Combine all sections
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.add(statsPanel, BorderLayout.NORTH);
        mainContent.add(recentPanel, BorderLayout.CENTER);
        mainContent.add(actionsPanel, BorderLayout.SOUTH);
        
        panel.add(mainContent, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel titleLabel = ThemeManager.createSubHeaderLabel(title);
        titleLabel.setForeground(color);
        
        JLabel valueLabel = ThemeManager.createHeaderLabel(value);
        valueLabel.setForeground(color);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createOrdersPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel headerLabel = ThemeManager.createHeaderLabel("Orders Management");
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Filter and Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{
            "All Orders", "Pending", "Confirmed", "Preparing", "Ready", "Delivered", "Cancelled"
        });
        statusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JButton btnNewOrder = ThemeManager.createPrimaryButton("New Order");
        JButton btnRefresh = ThemeManager.createSecondaryButton("Refresh");
        JButton btnExport = ThemeManager.createSuccessButton("Export");
        
        actionsPanel.add(new JLabel("Filter: "));
        actionsPanel.add(statusFilter);
        actionsPanel.add(Box.createHorizontalStrut(10));
        actionsPanel.add(btnNewOrder);
        actionsPanel.add(btnRefresh);
        actionsPanel.add(btnExport);
        
        headerPanel.add(actionsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Orders Table
        String[] columns = {"Order #", "Customer", "Items", "Total", "Status", "Time", "Actions"};
        Object[][] data = {};
        
        JTable ordersTable = new JTable(data, columns);
        ordersTable.setRowHeight(35);
        ordersTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ordersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMenuPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout());
        
        // Header
        JLabel headerLabel = ThemeManager.createHeaderLabel("Menu Management");
        headerLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // Menu Categories and Items
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        
        // Categories
        JPanel categoriesPanel = new JPanel(new BorderLayout());
        categoriesPanel.setBorder(BorderFactory.createTitledBorder("Categories"));
        
        DefaultListModel<String> categoryModel = new DefaultListModel<>();
        categoryModel.addElement("Main Courses");
        categoryModel.addElement("Appetizers");
        categoryModel.addElement("Beverages");
        categoryModel.addElement("Desserts");
        
        JList<String> categoryList = new JList<>(categoryModel);
        categoryList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoriesPanel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        
        // Menu Items
        JPanel menuItemsPanel = new JPanel(new BorderLayout());
        menuItemsPanel.setBorder(BorderFactory.createTitledBorder("Menu Items"));
        
        String[] columns = {"Code", "Name", "Category", "Price", "Available"};
        Object[][] data = {};
        
        JTable menuTable = new JTable(data, columns);
        menuTable.setRowHeight(30);
        menuTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        menuTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JScrollPane menuScrollPane = new JScrollPane(menuTable);
        menuItemsPanel.add(menuScrollPane, BorderLayout.CENTER);
        
        // Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JButton btnAddItem = ThemeManager.createPrimaryButton("Add Item");
        JButton btnEditItem = ThemeManager.createSecondaryButton("Edit Item");
        JButton btnDeleteItem = ThemeManager.createDangerButton("Delete Item");
        JButton btnRefresh = ThemeManager.createSecondaryButton("Refresh");
        
        actionsPanel.add(btnAddItem);
        actionsPanel.add(btnEditItem);
        actionsPanel.add(btnDeleteItem);
        actionsPanel.add(btnRefresh);
        
        menuItemsPanel.add(actionsPanel, BorderLayout.SOUTH);
        
        splitPane.setLeftComponent(categoriesPanel);
        splitPane.setRightComponent(menuItemsPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createReportsPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout());
        
        // Header
        JLabel headerLabel = ThemeManager.createHeaderLabel("Reports & Analytics");
        headerLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // Report Type Selection
        JPanel reportTypePanel = new JPanel(new GridLayout(1, 4, 20, 10));
        reportTypePanel.setBorder(BorderFactory.createTitledBorder("Select Report Type"));
        
        JRadioButton rbSalesReport = new JRadioButton("Sales Report");
        JRadioButton rbOrderReport = new JRadioButton("Order Report");
        JRadioButton rbRevenueReport = new JRadioButton("Revenue Report");
        JRadioButton rbMenuReport = new JRadioButton("Menu Performance");
        
        ButtonGroup reportGroup = new ButtonGroup();
        reportGroup.add(rbSalesReport);
        reportGroup.add(rbOrderReport);
        reportGroup.add(rbRevenueReport);
        reportGroup.add(rbMenuReport);
        
        reportTypePanel.add(rbSalesReport);
        reportTypePanel.add(rbOrderReport);
        reportTypePanel.add(rbRevenueReport);
        reportTypePanel.add(rbMenuReport);
        
        // Date Range Selection
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.setBorder(BorderFactory.createTitledBorder("Date Range"));
        
        datePanel.add(new JLabel("From: "));
        datePanel.add(new JTextField(10));
        datePanel.add(new JLabel("To: "));
        datePanel.add(new JTextField(10));
        
        JButton btnGenerate = ThemeManager.createPrimaryButton("Generate Report");
        datePanel.add(btnGenerate);
        
        // Report Display Area
        JPanel reportDisplayPanel = new JPanel(new BorderLayout());
        reportDisplayPanel.setBorder(BorderFactory.createTitledBorder("Report Preview"));
        
        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        reportArea.setBackground(ThemeManager.SURFACE_COLOR);
        
        JScrollPane scrollPane = new JScrollPane(reportArea);
        reportDisplayPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Report Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JButton btnPrint = ThemeManager.createPrimaryButton("Print Report");
        JButton btnExportPDF = ThemeManager.createSuccessButton("Export PDF");
        JButton btnExportExcel = ThemeManager.createSecondaryButton("Export Excel");
        
        actionsPanel.add(btnPrint);
        actionsPanel.add(btnExportPDF);
        actionsPanel.add(btnExportExcel);
        
        // Combine sections
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.add(reportTypePanel, BorderLayout.NORTH);
        mainContent.add(datePanel, BorderLayout.CENTER);
        mainContent.add(reportDisplayPanel, BorderLayout.SOUTH);
        
        panel.add(mainContent, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = ThemeManager.createCardPanel();
        panel.setLayout(new BorderLayout());
        
        // Header
        JLabel headerLabel = ThemeManager.createHeaderLabel("Settings");
        headerLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // Settings Categories
        JPanel settingsCategoriesPanel = new JPanel(new GridLayout(1, 4, 20, 10));
        settingsCategoriesPanel.setBorder(BorderFactory.createTitledBorder("Settings Categories"));
        
        JRadioButton rbGeneral = new JRadioButton("General");
        JRadioButton rbAPI = new JRadioButton("API Configuration");
        JRadioButton rbPrinter = new JRadioButton("Printer Settings");
        JRadioButton rbTheme = new JRadioButton("Theme Settings");
        
        ButtonGroup settingsGroup = new ButtonGroup();
        settingsGroup.add(rbGeneral);
        settingsGroup.add(rbAPI);
        settingsGroup.add(rbPrinter);
        settingsGroup.add(rbTheme);
        
        settingsCategoriesPanel.add(rbGeneral);
        settingsCategoriesPanel.add(rbAPI);
        settingsCategoriesPanel.add(rbPrinter);
        settingsCategoriesPanel.add(rbTheme);
        
        // Settings Content Area
        JPanel settingsContentPanel = new JPanel(new BorderLayout());
        settingsContentPanel.setBorder(BorderFactory.createTitledBorder("Settings Configuration"));
        
        // General Settings (default)
        JPanel generalSettings = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        generalSettings.add(ThemeManager.createBodyLabel("Restaurant Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        generalSettings.add(ThemeManager.createTextField(), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        generalSettings.add(ThemeManager.createBodyLabel("Restaurant Phone:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        generalSettings.add(ThemeManager.createTextField(), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        generalSettings.add(ThemeManager.createBodyLabel("Restaurant Address:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        generalSettings.add(ThemeManager.createTextArea(), gbc);
        
        settingsContentPanel.add(generalSettings, BorderLayout.CENTER);
        
        // Save Button
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        savePanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JButton btnSave = ThemeManager.createSuccessButton("Save Settings");
        JButton btnReset = ThemeManager.createSecondaryButton("Reset to Default");
        
        savePanel.add(btnSave);
        savePanel.add(btnReset);
        
        panel.add(settingsCategoriesPanel, BorderLayout.NORTH);
        panel.add(settingsContentPanel, BorderLayout.CENTER);
        panel.add(savePanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void setupStatusBar() {
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(ThemeManager.SURFACE_COLOR);
        statusBar.setPreferredSize(new Dimension(0, 30));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.LIGHT_COLOR));
        
        // Left side - Status
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        
        lblConnectionStatus = ThemeManager.createCaptionLabel("Connected");
        leftPanel.add(lblConnectionStatus);
        
        statusBar.add(leftPanel, BorderLayout.WEST);
        
        // Right side - Info
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        
        lblOrderCount = ThemeManager.createCaptionLabel("Orders: 0");
        lblSystemStatus = ThemeManager.createCaptionLabel("System: Ready");
        
        rightPanel.add(lblOrderCount);
        rightPanel.add(Box.createHorizontalStrut(20));
        rightPanel.add(lblSystemStatus);
        
        statusBar.add(rightPanel, BorderLayout.EAST);
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("New Order"));
        fileMenu.add(new JMenuItem("Open Order"));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Print Receipt"));
        fileMenu.add(new JMenuItem("Export Data"));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Exit"));
        
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem("Undo"));
        editMenu.add(new JMenuItem("Redo"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Cut"));
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));
        
        // View Menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(new JMenuItem("Dashboard"));
        viewMenu.add(new JMenuItem("Orders"));
        viewMenu.add(new JMenuItem("Menu"));
        viewMenu.add(new JMenuItem("Reports"));
        viewMenu.add(new JMenuItem("Settings"));
        
        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.add(new JMenuItem("Database Backup"));
        toolsMenu.add(new JMenuItem("Database Restore"));
        toolsMenu.add(new JMenuItem("Clear Cache"));
        toolsMenu.addSeparator();
        toolsMenu.add(new JMenuItem("System Diagnostics"));
        
        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem("User Manual"));
        helpMenu.add(new JMenuItem("Keyboard Shortcuts"));
        helpMenu.add(new JMenuItem("About"));
        
        // Theme Menu
        JMenu themeMenu = new JMenu("Theme");
        ThemeManager.createThemeMenu(themeMenu);
        menuBar.add(themeMenu);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
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
                    MobileMealsPOSFixed.this,
                    "Are you sure you want to exit?",
                    "Confirm Exit",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    restaurantSession.logout();
                    dispose();
                    // Show login dialog or close application
                }
            }
        });
    }
    
    private void showPanel(String panelName) {
        cardLayout.show(contentPanel, panelName);
        updateNavButtonStates(panelName);
    }
    
    private void updateNavButtonStates(String activePanel) {
        // Reset all buttons
        resetNavButton(btnDashboard);
        resetNavButton(btnOrders);
        resetNavButton(btnMenu);
        resetNavButton(btnReports);
        resetNavButton(btnSettings);
        
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
        button.setBackground(ThemeManager.PRIMARY_COLOR);
    }
    
    private void highlightNavButton(JButton button) {
        button.setBackground(ThemeManager.PRIMARY_DARK);
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
        lblRestaurantName.setText("Demo Restaurant");
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
        javax.swing.Timer refreshTimer = new javax.swing.Timer(30000, e -> {
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
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new MobileMealsPOSFixed().setVisible(true);
        });
    }
}
