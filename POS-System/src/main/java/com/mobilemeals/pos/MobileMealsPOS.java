package com.mobilemeals.pos;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URL;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

/**
 * Desktop front end for the Django restaurant POS workflow.
 *
 * The states deliberately mirror restaurants.models_pos.POSOrder:
 * active -> invoiced -> completed (or cancelled/refunded).  This client is
 * useful on its own for counter sales and keeps the code ready for a REST
 * adapter when the Django POS views are exposed as JSON endpoints.
 */
public final class MobileMealsPOS extends JFrame {
    private static final String FONT_FAMILY = "Segoe UI";
    private static final Color NAVY = new Color(44, 62, 80);       // Mobile Meals dark blue-gray
    private static final Color INK = new Color(44, 62, 80);
    private static final Color BLUE = new Color(255, 107, 53);     // Mobile Meals primary orange
    private static final Color GOLD = new Color(247, 147, 30);     // Mobile Meals secondary orange
    private static final Color GREEN = new Color(24, 142, 92);
    private static final Color AMBER = new Color(220, 142, 34);
    private static final Color BACKGROUND = new Color(243, 245, 248);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final List<MenuItem> menu = new ArrayList<>();
    private final List<PosOrder> orders = new ArrayList<>();
    private final List<CartLine> cart = new ArrayList<>();
    private final List<StaffMember> staffMembers = new ArrayList<>();
    private final Map<String, ImageIcon> mealImages = new ConcurrentHashMap<>();
    private PosSession session;
    private String userRole = "";
    private String restaurantName = "Mobile Meals POS";
    private final POSApiClient apiClient;
    private final Preferences preferences = Preferences.userNodeForPackage(MobileMealsPOS.class);
    private boolean darkMode;
    private JPanel rootPanel;

    private final DefaultTableModel menuModel = model("Item", "Category", "Price");
    private final DefaultTableModel cartModel = model("Item", "Qty", "Price", "Total");
    private final DefaultTableModel orderModel = model("Invoice", "Table", "Customer", "Total", "Status", "Created");
    private final DefaultTableModel staffModel = model("Name", "Username", "Role", "Status", "Joined");
    private final JLabel totalLabel = new JLabel("KES 0.00");
    private final JLabel sessionLabel = new JLabel("No shift open");
    private final JLabel summaryLabel = new JLabel();
    private final JLabel staffStatusLabel = new JLabel("Loading team…");
    private final JPanel productGrid = new JPanel(new GridLayout(0, 3, 12, 12));
    private final SalesAnalyticsChart analyticsChart = new SalesAnalyticsChart();
    private JTable orderTable;

    public MobileMealsPOS() { this(null); }

    /** A null client is intentionally supported only for an offline demo. */
    public MobileMealsPOS(POSApiClient apiClient) {
        super("Mobile Meals POS");
        this.apiClient = apiClient;
        Image brandLogo = loadBrandLogo();
        if (brandLogo != null) setIconImage(brandLogo);
        this.darkMode = preferences.getBoolean("darkMode", false);
        seedMenu();
        loadBackendData();
        buildUi();
        refreshAll();
        applyTheme(darkMode);
        if (apiClient != null) new javax.swing.Timer(15_000, e -> syncBackendAsync()).start();
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) { public boolean isCellEditable(int row, int col) { return false; } };
    }

    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1280, 780);
        setLocationRelativeTo(null);
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(BACKGROUND);
        rootPanel.add(header(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.setBackground(Color.WHITE);
        tabs.setForeground(INK);
        tabs.addTab("Sell", salesPanel());
        tabs.addTab("Cashier queue", cashierPanel());
        tabs.addTab("Orders", ordersPanel());
        tabs.addTab("Shift & reports", reportsPanel());
        tabs.addTab("My team", staffPanel());
        rootPanel.add(tabs, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(NAVY); panel.setBorder(new EmptyBorder(16, 24, 16, 24));
        JLabel logo = new JLabel(); Image headerLogo = loadBrandLogo(); if (headerLogo != null) logo.setIcon(new ImageIcon(headerLogo.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
        JLabel title = new JLabel(" MOBILE MEALS");
        title.setForeground(Color.WHITE); title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JLabel terminal = new JLabel("   COUNTER TERMINAL");
        terminal.setForeground(new Color(155, 186, 218)); terminal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); brand.setOpaque(false); brand.add(logo); brand.add(title); brand.add(terminal);
        sessionLabel.setForeground(new Color(210, 224, 240)); sessionLabel.putClientProperty("headerControl", true);
        JToggleButton themeToggle = new JToggleButton("Dark mode", new ThemeModeIcon(darkMode));
        themeToggle.putClientProperty("headerControl", true); themeToggle.setSelected(darkMode); themeToggle.setForeground(Color.WHITE); themeToggle.setOpaque(false); themeToggle.setContentAreaFilled(false); themeToggle.setBorder(BorderFactory.createLineBorder(new Color(255,179,71),1,true)); themeToggle.setFocusPainted(false);
        themeToggle.addActionListener(e -> {
            boolean enabled = themeToggle.isSelected();
            themeToggle.setIcon(new ThemeModeIcon(enabled));
            applyTheme(enabled);
        });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); controls.setOpaque(false); controls.add(sessionLabel); controls.add(themeToggle);
        panel.add(brand, BorderLayout.WEST); panel.add(controls, BorderLayout.EAST);
        return panel;
    }

    private JComponent salesPanel() {
        JPanel panel = padded(new BorderLayout(18, 0));
        JPanel left = new JPanel(new BorderLayout(0, 12)); left.setOpaque(false);
        JPanel menuHeader = new JPanel(new BorderLayout()); menuHeader.setOpaque(false);
        JPanel headings = new JPanel(new GridLayout(2, 1)); headings.setOpaque(false);
        JLabel welcome = new JLabel("New sale"); welcome.setFont(new Font("Segoe UI", Font.BOLD, 25)); welcome.setForeground(INK);
        JLabel choose = new JLabel("Choose products to add to the current order"); choose.setForeground(new Color(105, 115, 128));
        headings.add(welcome); headings.add(choose);
        JTextField search = new JTextField(); search.putClientProperty("JTextField.placeholderText", "Search menu"); search.setPreferredSize(new Dimension(220, 38));
        styleField(search);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { renderProductGrid(search.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { renderProductGrid(search.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { renderProductGrid(search.getText()); }
        });
        menuHeader.add(headings, BorderLayout.WEST); menuHeader.add(search, BorderLayout.EAST);
        productGrid.setBackground(BACKGROUND); productGrid.setBorder(new EmptyBorder(4, 2, 4, 2));
        left.add(menuHeader, BorderLayout.NORTH); left.add(new JScrollPane(productGrid), BorderLayout.CENTER);

        JTable cartTable = table(cartModel);
        JPanel right = new JPanel(new BorderLayout(0, 10));
        JLabel cartTitle = new JLabel("CURRENT ORDER"); cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); cartTitle.setForeground(new Color(90, 101, 116));
        right.add(section(cartTitle, new JScrollPane(cartTable)), BorderLayout.CENTER);
        JPanel checkout = new JPanel(new GridLayout(0, 1, 0, 8));
        checkout.setOpaque(false); totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 30)); totalLabel.setForeground(INK); totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        JButton remove = secondary("Remove selected line"); remove.addActionListener(e -> removeCartLine(cartTable.getSelectedRow()));
        JButton invoice = primary("CHARGE / SEND TO CASHIER"); invoice.addActionListener(e -> invoice());
        checkout.add(totalLabel); checkout.add(remove); checkout.add(invoice); right.add(checkout, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right); split.setResizeWeight(.5); panel.add(split);
        return panel;
    }

    private JComponent cashierPanel() {
        JPanel panel = padded(new BorderLayout(0, 12));
        JLabel hint = new JLabel("Cashier: select an invoiced order, choose payment, then complete it.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 15)); panel.add(hint, BorderLayout.NORTH);
        orderTable = table(orderModel); panel.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JComboBox<String> payment = new JComboBox<>(new String[]{"cash", "mpesa", "card"});
        JButton paid = primary("Mark paid & print receipt"); paid.addActionListener(e -> markPaid((String) payment.getSelectedItem()));
        JButton cancel = secondary("Cancel invoice"); cancel.addActionListener(e -> changeSelectedStatus("cancelled"));
        actions.add(new JLabel("Payment:")); actions.add(payment); actions.add(cancel); actions.add(paid); panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent ordersPanel() {
        JPanel panel = padded(new BorderLayout());
        JTable table = table(orderModel); panel.add(new JScrollPane(table));
        return panel;
    }

    private JComponent reportsPanel() {
        JPanel panel = padded(new BorderLayout(0, 16));
        JLabel title = new JLabel("Sales intelligence"); title.setFont(new Font("Segoe UI", Font.BOLD, 25)); title.setForeground(INK);
        panel.add(title, BorderLayout.NORTH);
        summaryLabel.setVerticalAlignment(SwingConstants.TOP); summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        JPanel display = new JPanel(new GridLayout(1, 2, 16, 0)); display.setOpaque(false);
        display.add(section("Shift performance", summaryLabel)); display.add(section("Payment mix", analyticsChart));
        panel.add(display, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton open = primary("Open shift"); open.addActionListener(e -> openShift());
        JButton close = secondary("Close shift"); close.addActionListener(e -> closeShift());
        actions.add(open); actions.add(close); panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent staffPanel() {
        JPanel panel = padded(new BorderLayout(0, 14));
        JLabel title = new JLabel("Your POS team"); title.setFont(new Font("Segoe UI", Font.BOLD, 25)); title.setForeground(INK);
        JLabel sub = new JLabel("Waiters create invoices; cashiers collect payment and reconcile sales."); sub.setForeground(new Color(105,115,128));
        JPanel header = new JPanel(new GridLayout(2, 1)); header.setOpaque(false); header.add(title); header.add(sub); panel.add(header, BorderLayout.NORTH);
        JTable staffTable = table(staffModel); panel.add(section("Staff members", new JScrollPane(staffTable)), BorderLayout.CENTER);
        staffStatusLabel.setForeground(new Color(105,115,128));
        JButton refresh = primary("REFRESH TEAM"); refresh.addActionListener(e -> refreshStaffAsync());
        JButton add = secondary("ADD TEAM MEMBER"); add.addActionListener(e -> showAddStaffDialog());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setOpaque(false); actions.add(staffStatusLabel); actions.add(add); actions.add(refresh); panel.add(actions, BorderLayout.SOUTH);
        SwingUtilities.invokeLater(this::refreshStaffAsync);
        return panel;
    }

    private JPanel padded(LayoutManager layout) { JPanel p = new JPanel(layout); p.setBorder(new EmptyBorder(22, 24, 22, 24)); p.setBackground(BACKGROUND); return p; }
    private JComponent section(Object title, JComponent body) { JPanel p = new JPanel(new BorderLayout(0, 8)); p.setBackground(Color.WHITE); p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225,229,235)), new EmptyBorder(14,14,14,14))); if (title instanceof String) { JLabel label=new JLabel((String)title); label.setFont(new Font("Segoe UI",Font.BOLD,14)); p.add(label,BorderLayout.NORTH); } else p.add((JComponent)title, BorderLayout.NORTH); p.add(body); return p; }
    private JTable table(DefaultTableModel m) { JTable t = new JTable(m); t.setRowHeight(38); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setAutoCreateRowSorter(true); t.setShowVerticalLines(false); t.setGridColor(new Color(235,238,242)); t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); t.getTableHeader().setBackground(new Color(247,248,250)); return t; }
    private JButton primary(String text) { JButton b = new JButton(text); b.putClientProperty("primary", true); b.setBackground(BLUE); b.setForeground(Color.WHITE); b.setFont(new Font("Segoe UI",Font.BOLD,13)); b.setFocusPainted(false); b.setBorder(new EmptyBorder(12,16,12,16)); return b; }
    private JButton secondary(String text) { JButton b = new JButton(text); b.setBackground(Color.WHITE); b.setForeground(INK); b.setFocusPainted(false); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210,216,224)),new EmptyBorder(10,14,10,14))); return b; }
    private void styleField(JTextComponent field) { field.setFont(new Font("Segoe UI", Font.PLAIN, 14)); field.setForeground(INK); field.setBackground(Color.WHITE); field.setCaretColor(BLUE); field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(214,220,228), 1, true), new EmptyBorder(10,12,10,12))); }

    /** Drawn locally so the theme control never depends on an emoji font being installed. */
    private static final class ThemeModeIcon implements Icon {
        private final boolean dark;

        private ThemeModeIcon(boolean dark) { this.dark = dark; }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (dark) {
                    g.setColor(Color.WHITE);
                    g.fillOval(x + 2, y + 2, 14, 14);
                    g.setColor(NAVY);
                    g.fillOval(x + 7, y, 14, 14);
                } else {
                    g.setColor(new Color(255, 190, 64));
                    g.fillOval(x + 5, y + 5, 8, 8);
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.toRadians(i * 45.0);
                        int x1 = x + 9 + (int) Math.round(Math.cos(angle) * 6);
                        int y1 = y + 9 + (int) Math.round(Math.sin(angle) * 6);
                        int x2 = x + 9 + (int) Math.round(Math.cos(angle) * 8);
                        int y2 = y + 9 + (int) Math.round(Math.sin(angle) * 8);
                        g.drawLine(x1, y1, x2, y2);
                    }
                }
            } finally { g.dispose(); }
        }
    }

    private void applyTheme(boolean dark) {
        darkMode = dark; preferences.putBoolean("darkMode", dark);
        if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        if (rootPanel != null) applyThemeTo(rootPanel);
        SwingUtilities.updateComponentTreeUI(this); repaint();
    }
    private void applyThemeTo(Component component) {
        Color surface = darkMode ? new Color(31, 36, 43) : Color.WHITE;
        Color canvas = darkMode ? new Color(23, 27, 32) : BACKGROUND;
        Color text = darkMode ? new Color(235, 238, 242) : INK;
        if (component == rootPanel) component.setBackground(canvas);
        else if (component instanceof JPanel && component.getBackground() != NAVY) component.setBackground(surface);
        if (component instanceof JTable) { JTable table = (JTable) component; table.setBackground(surface); table.setForeground(text); table.setGridColor(darkMode ? new Color(58,65,74) : new Color(235,238,242)); }
        if (component instanceof JTextComponent) { component.setBackground(darkMode ? new Color(43,49,58) : Color.WHITE); component.setForeground(text); }
        if (component instanceof JLabel && !Boolean.TRUE.equals(((JComponent)component).getClientProperty("headerControl")) && component.getForeground() != Color.WHITE) component.setForeground(text);
        if (component instanceof JButton && !Boolean.TRUE.equals(((JButton)component).getClientProperty("primary")) && !Boolean.TRUE.equals(((JButton)component).getClientProperty("headerControl"))) { component.setBackground(surface); component.setForeground(text); }
        if (component instanceof Container) for (Component child : ((Container)component).getComponents()) applyThemeTo(child);
    }

    private void seedMenu() {
        menu.add(new MenuItem("Beef burger", "Mains", 650)); menu.add(new MenuItem("Chicken wrap", "Mains", 550));
        menu.add(new MenuItem("Pilau plate", "Local favourites", 450)); menu.add(new MenuItem("Fresh juice", "Drinks", 180));
    }
    @SuppressWarnings("unchecked")
    private void loadBackendData() {
        if (apiClient == null) return;
        try {
            java.util.Map<String, Object> data = apiClient.getPosBootstrap();
            userRole = String.valueOf(data.getOrDefault("role", ""));
            Object restaurant = data.get("restaurant");
            if (restaurant instanceof Map) {
                Object name = ((Map<?, ?>) restaurant).get("name");
                if (name != null && !String.valueOf(name).trim().isEmpty()) restaurantName = String.valueOf(name).trim();
            }
            Object meals = data.get("meals");
            if (meals instanceof List) {
                menu.clear();
                for (Object raw : (List<?>) meals) {
                    java.util.Map<String, Object> meal = (java.util.Map<String, Object>) raw;
                    menu.add(new MenuItem(normalizeId(meal.get("id")), String.valueOf(meal.get("name")),
                            String.valueOf(meal.get("category")), new BigDecimal(String.valueOf(meal.get("price"))),
                            String.valueOf(meal.getOrDefault("image_url", ""))));
                }
            }
            Object remoteOrders = data.get("orders");
            if (remoteOrders instanceof List) {
                orders.clear();
                for (Object rawOrder : (List<?>) remoteOrders) {
                    java.util.Map<String, Object> remote = (java.util.Map<String, Object>) rawOrder;
                    List<CartLine> lines = new ArrayList<>(); Object remoteItems = remote.get("items");
                    if (remoteItems instanceof List) for (Object rawItem : (List<?>) remoteItems) {
                        java.util.Map<String, Object> item = (java.util.Map<String, Object>) rawItem;
                        String mealId = String.valueOf(item.get("meal_id")); MenuItem meal = findMenuItem(mealId);
                        if (meal == null) meal = new MenuItem(mealId, String.valueOf(item.get("name")), "", new BigDecimal(String.valueOf(item.get("price"))), "");
                        CartLine line = new CartLine(meal); line.qty = ((Number)item.get("quantity")).intValue(); lines.add(line);
                    }
                    PosOrder order = new PosOrder(String.valueOf(remote.get("id")), String.valueOf(remote.get("table_number")), String.valueOf(remote.get("customer_name")), lines);
                    order.status = String.valueOf(remote.get("status")); order.paymentMethod = String.valueOf(remote.get("payment_method")); orders.add(order);
                }
            }
            session = new PosSession(BigDecimal.ZERO);
        } catch (Exception error) {
            throw new IllegalStateException("Could not load the authenticated POS terminal: " + error.getMessage(), error);
        }
    }
    private MenuItem findMenuItem(String id) { for (MenuItem item : menu) if (id.equals(item.id)) return item; return null; }
    private String normalizeId(Object id) { return id instanceof Number ? String.valueOf(((Number) id).longValue()) : String.valueOf(id).replaceAll("\\.0$", ""); }
    @SuppressWarnings("unchecked")
    private void refreshStaffAsync() {
        if (apiClient == null) { staffStatusLabel.setText("Offline mode — no team data"); return; }
        if (!"owner".equals(userRole)) { staffStatusLabel.setText("Owner access required"); return; }
        staffStatusLabel.setText("Loading team…");
        new SwingWorker<List<StaffMember>, Void>() {
            protected List<StaffMember> doInBackground() throws Exception { return fetchStaff(); }
            protected void done() { try { showStaff(get()); } catch (Exception error) { staffStatusLabel.setText("Could not load team"); message("Unable to load staff: " + error.getMessage()); } }
        }.execute();
    }
    @SuppressWarnings("unchecked")
    private List<StaffMember> fetchStaff() throws Exception {
        List<StaffMember> result = new ArrayList<>();
        Object staff = apiClient.getPosStaff().get("staff");
        if (staff instanceof List) for (Object raw : (List<?>) staff) {
            java.util.Map<String, Object> person = (java.util.Map<String, Object>) raw;
            result.add(new StaffMember(String.valueOf(person.get("name")), String.valueOf(person.get("username")),
                String.valueOf(person.get("role")), Boolean.TRUE.equals(person.get("is_active")), String.valueOf(person.get("created_at")).replace('T', ' ')));
        }
        return result;
    }
    private void showStaff(List<StaffMember> members) {
        staffMembers.clear(); staffMembers.addAll(members); staffModel.setRowCount(0);
        for (StaffMember member : staffMembers) staffModel.addRow(new Object[]{member.name, member.username, capitalize(member.role), member.active ? "Active" : "Inactive", member.joined});
        staffStatusLabel.setText(members.isEmpty() ? "No team members yet" : members.size() + " team member" + (members.size() == 1 ? "" : "s"));
    }
    private void showAddStaffDialog() {
        if (!"owner".equals(userRole)) { message("Only the restaurant owner can add team members."); return; }
        JTextField firstName = new JTextField(), lastName = new JTextField(), username = new JTextField(), email = new JTextField();
        JPasswordField password = new JPasswordField(); JComboBox<String> role = new JComboBox<>(new String[]{"waiter", "cashier"});
        for (JTextComponent field : new JTextComponent[]{firstName, lastName, username, email, password}) styleField(field);
        role.setFont(new Font("Segoe UI", Font.PLAIN, 14)); role.setBackground(Color.WHITE);
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("First name")); form.add(firstName); form.add(new JLabel("Last name")); form.add(lastName);
        form.add(new JLabel("Username *")); form.add(username); form.add(new JLabel("Email")); form.add(email);
        form.add(new JLabel("Temporary password *")); form.add(password); form.add(new JLabel("Role *")); form.add(role);
        if (JOptionPane.showConfirmDialog(this, form, "Add team member", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (username.getText().trim().isEmpty() || password.getPassword().length < 8) { message("Username and a password of at least 8 characters are required."); return; }
        java.util.Map<String, Object> data = new java.util.HashMap<>(); data.put("first_name", firstName.getText().trim()); data.put("last_name", lastName.getText().trim()); data.put("username", username.getText().trim()); data.put("email", email.getText().trim()); data.put("password", new String(password.getPassword())); data.put("role", role.getSelectedItem());
        new SwingWorker<Void, Void>() { protected Void doInBackground() throws Exception { apiClient.createPosStaff(data); return null; } protected void done() { try { get(); message("Team member added successfully."); refreshStaffAsync(); } catch (Exception error) { message("Unable to add team member: " + error.getMessage()); } } }.execute();
    }
    private void renderProductGrid(String query) { productGrid.removeAll(); String term=query.toLowerCase().trim(); int count=0; for (MenuItem item : menu) if (term.isEmpty() || item.name.toLowerCase().contains(term) || item.category.toLowerCase().contains(term)) { productGrid.add(productCard(item)); count++; } productGrid.setPreferredSize(new Dimension(1, Math.max(1, (int)Math.ceil(count / 3.0)) * 190)); productGrid.revalidate(); productGrid.repaint(); }
    private JButton productCard(MenuItem item) { JButton card = new JButton("<html><div style='width:130px;padding:5px'><b>"+item.name+"</b><br><span style='color:#768394'>"+item.category+"</span><br><span style='color:#e55a2b;font-size:15px'><b>"+money(item.price)+"</b></span></div></html>"); card.setPreferredSize(new Dimension(180,180)); card.setMinimumSize(new Dimension(130,180)); card.setHorizontalAlignment(SwingConstants.CENTER); card.setVerticalTextPosition(SwingConstants.BOTTOM); card.setHorizontalTextPosition(SwingConstants.CENTER); card.setIconTextGap(4); card.setBackground(Color.WHITE); card.setForeground(INK); card.setFont(new Font(FONT_FAMILY,Font.PLAIN,14)); card.setFocusPainted(false); card.setCursor(new Cursor(Cursor.HAND_CURSOR)); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225,229,235)),new EmptyBorder(8,8,8,8))); ImageIcon image=mealImages.get(item.id); if(image != null) card.setIcon(image); else if(!item.imageUrl.isEmpty()) loadMealImage(item, card); card.addActionListener(e -> addMenuItem(item)); return card; }
    private void loadMealImage(MenuItem item, JButton card) { new SwingWorker<ImageIcon, Void>() { protected ImageIcon doInBackground() { try { BufferedImage source=ImageIO.read(new URL(item.imageUrl)); if(source == null) return null; Image scaled=source.getScaledInstance(128, 92, Image.SCALE_SMOOTH); return new ImageIcon(scaled); } catch(Exception ignored) { return null; } } protected void done() { try { ImageIcon icon=get(); if(icon != null) { mealImages.put(item.id, icon); card.setIcon(icon); card.revalidate(); card.repaint(); } } catch(Exception ignored) {} } }.execute(); }
    private void addSelectedMenuItem(int row) { if (row >= 0 && row < menu.size()) addMenuItem(menu.get(row)); }
    private void addMenuItem(MenuItem item) { for (CartLine line : cart) if (line.item == item) { line.qty++; refreshCart(); return; } cart.add(new CartLine(item)); refreshCart(); }
    private void removeCartLine(int row) { if (row >= 0) { cart.remove(row); refreshCart(); } }
    private void invoice() {
        if (session == null || !session.active) { message("Open a POS shift before creating an invoice."); return; }
        if (cart.isEmpty()) { message("Add at least one menu item."); return; }
        String table = JOptionPane.showInputDialog(this, "Table / counter reference (optional):", "Counter");
        if (table == null) return;
        String customer = JOptionPane.showInputDialog(this, "Customer name (optional):", "");
        String remoteId = null;
        if (apiClient != null) try {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("table_number", table); body.put("customer_name", customer == null ? "" : customer);
            List<java.util.Map<String, Object>> items = new ArrayList<>();
            for (CartLine line : cart) {
                if (line.item.id == null) throw new IllegalStateException("Offline menu items cannot be sent to the server.");
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("meal_id", integerId(line.item.id)); item.put("quantity", line.qty); items.add(item);
            }
            body.put("items", items);
            java.util.Map<String, Object> created = apiClient.createPosOrder(body);
            java.util.Map<String, Object> remote = (java.util.Map<String, Object>) created.get("order");
            remoteId = String.valueOf(remote.get("id"));
            apiClient.invoicePosOrder(remoteId);
        } catch (Exception error) { message("The invoice was not saved: " + error.getMessage()); return; }
        PosOrder order = new PosOrder(remoteId, table, customer == null ? "" : customer, new ArrayList<>(cart));
        order.status = "invoiced"; orders.add(order); cart.clear(); refreshAll();
        if (JOptionPane.showConfirmDialog(this, "Invoice " + order.number() + " is ready. Print it for the customer?", "Invoice ready", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) printWaiterInvoice(order);
    }
    private void markPaid(String method) {
        PosOrder order = selectedOrder(); if (order == null) return;
        if (!"invoiced".equals(order.status)) { message("Only invoiced orders can be paid."); return; }
        if (apiClient != null) try { apiClient.payPosOrder(order.id, method); }
        catch (Exception error) { message("Payment was not saved: " + error.getMessage()); return; }
        order.status = "completed"; order.paymentMethod = method; order.completedAt = LocalDateTime.now(); refreshAll(); receipt(order);
    }
    private void changeSelectedStatus(String status) { PosOrder order = selectedOrder(); if (order != null && "invoiced".equals(order.status)) { order.status = status; refreshAll(); } }
    private PosOrder selectedOrder() { if (orderTable == null || orderTable.getSelectedRow() < 0) { message("Select an order first."); return null; } int view = orderTable.convertRowIndexToModel(orderTable.getSelectedRow()); return orders.get(view); }
    private void openShift() { if (session != null && session.active) { message("A shift is already open."); return; } String amount = JOptionPane.showInputDialog(this, "Opening cash balance (KES):", "0"); try { session = new PosSession(new BigDecimal(amount)); refreshAll(); } catch (Exception e) { message("Enter a valid amount."); } }
    private void closeShift() { if (session == null || !session.active) { message("There is no active shift."); return; } String amount = JOptionPane.showInputDialog(this, "Closing cash balance (KES):", session.expectedCash(cashSales()).toPlainString()); try { if (apiClient != null) apiClient.closePosSession(amount); session.closing = new BigDecimal(amount); session.active = false; refreshAll(); } catch (Exception e) { message("The shift was not closed: " + e.getMessage()); } }
    private void refreshAll() { refreshMenu(); refreshCart(); refreshOrders(); refreshReport(); sessionLabel.setText(session == null ? "No shift open" : (session.active ? "Shift open • " + TIME.format(session.opened) : "Shift closed")); }
    private void refreshMenu() { menuModel.setRowCount(0); for (MenuItem x : menu) menuModel.addRow(new Object[]{x.name, x.category, money(x.price)}); renderProductGrid(""); }
    private void refreshCart() { cartModel.setRowCount(0); for (CartLine x : cart) cartModel.addRow(new Object[]{x.item.name, x.qty, money(x.item.price), money(x.total())}); totalLabel.setText(money(cartTotal())); }
    private void refreshOrders() { orderModel.setRowCount(0); for (PosOrder x : orders) orderModel.addRow(new Object[]{x.number(), x.table, x.customer, money(x.total()), x.status, TIME.format(x.created)}); }
    private void refreshReport() { BigDecimal cash=BigDecimal.ZERO, mpesa=BigDecimal.ZERO, card=BigDecimal.ZERO; int completed=0, invoiced=0; for (PosOrder o:orders) { if ("invoiced".equals(o.status)) invoiced++; if ("completed".equals(o.status)) { completed++; if ("cash".equals(o.paymentMethod)) cash=cash.add(o.total()); if ("mpesa".equals(o.paymentMethod)) mpesa=mpesa.add(o.total()); if ("card".equals(o.paymentMethod)) card=card.add(o.total()); }} BigDecimal total=cash.add(mpesa).add(card); BigDecimal average=completed == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP); String shift = session == null ? "No POS session has been opened." : (session.active ? "Open since " + TIME.format(session.opened) : "Closed shift"); summaryLabel.setText("<html><h2>" + shift + "</h2><p><b>"+completed+"</b> completed &nbsp; • &nbsp; <b>"+invoiced+"</b> awaiting payment</p><p>Average sale<br><b>"+money(average)+"</b></p><h2>Today: "+money(total)+"</h2></html>"); analyticsChart.setValues(cash, mpesa, card); }
    private BigDecimal cartTotal() { BigDecimal t=BigDecimal.ZERO; for (CartLine x:cart) t=t.add(x.total()); return t; }
    private BigDecimal cashSales() { BigDecimal total=BigDecimal.ZERO; for (PosOrder o:orders) if ("completed".equals(o.status) && "cash".equals(o.paymentMethod)) total=total.add(o.total()); return total; }
    private String money(BigDecimal n) { return "KES " + n.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private Integer integerId(String id) { return new BigDecimal(id).intValueExact(); }
    private String capitalize(String value) { return value.isEmpty() ? "" : value.substring(0, 1).toUpperCase() + value.substring(1); }
    private void message(String text) { JOptionPane.showMessageDialog(this, text, "Mobile Meals POS", JOptionPane.INFORMATION_MESSAGE); }
    private void receipt(PosOrder o) { message("Receipt " + o.number() + "\n" + money(o.total()) + " paid by " + o.paymentMethod + ".\nReady to print or email through the Django receipt service."); }
    private void printWaiterInvoice(PosOrder order) {
        try {
            PrinterJob job = PrinterJob.getPrinterJob(); job.setJobName("Invoice " + order.number());
            job.setPrintable((graphics, format, page) -> {
                if (page > 0) return Printable.NO_SUCH_PAGE;
                Graphics2D g = (Graphics2D) graphics; g.translate(format.getImageableX(), format.getImageableY());
                g.setColor(INK); int y=24; g.setFont(new Font("Monospaced", Font.BOLD, 15)); g.drawString(restaurantName, 24, y); y+=22;
                g.setFont(new Font("Monospaced", Font.PLAIN, 10)); g.drawString("WAITER INVOICE  #"+order.number(), 24, y); y+=18; g.drawString("Table: "+order.table+"   "+TIME.format(order.created), 24, y); y+=16; g.drawLine(24,y,260,y); y+=18;
                for (CartLine line : order.lines) { g.drawString(line.qty+" x "+line.item.name, 24, y); g.drawString(money(line.total()), 185, y); y+=17; }
                g.drawLine(24,y,260,y); y+=22; g.setFont(new Font("Monospaced", Font.BOLD, 13)); g.drawString("TOTAL", 24, y); g.drawString(money(order.total()), 185, y); y+=28;
                g.setFont(new Font("Monospaced", Font.PLAIN, 10)); g.drawString("Please pay at the cashier.", 24, y); return Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) job.print();
        } catch (PrinterException error) { message("Could not print invoice: " + error.getMessage()); }
    }

    private static final class MenuItem { final String id, name, category, imageUrl; final BigDecimal price; MenuItem(String n,String c,int p){this(null,n,c,BigDecimal.valueOf(p),"");} MenuItem(String id,String n,String c,BigDecimal p){this(id,n,c,p,"");} MenuItem(String id,String n,String c,BigDecimal p,String image){this.id=id;name=n;category=c;price=p;imageUrl=image;} }
    private static final class CartLine { final MenuItem item; int qty=1; CartLine(MenuItem i){item=i;} BigDecimal total(){return item.price.multiply(BigDecimal.valueOf(qty));} }
    private static final class StaffMember { final String name, username, role, joined; final boolean active; StaffMember(String n,String u,String r,boolean a,String j){name=n;username=u;role=r;active=a;joined=j;} }
    private static final class PosOrder { final String id, table, customer; final List<CartLine> lines; final LocalDateTime created=LocalDateTime.now(); String status="active", paymentMethod=""; LocalDateTime completedAt; PosOrder(String t,String c,List<CartLine> l){this(null,t,c,l);} PosOrder(String remoteId,String t,String c,List<CartLine> l){id=remoteId == null ? UUID.randomUUID().toString() : remoteId;table=t;customer=c;lines=l;} String number(){return id.substring(0,8).toUpperCase();} BigDecimal total(){BigDecimal n=BigDecimal.ZERO;for(CartLine l:lines)n=n.add(l.total());return n;} }
    private static final class PosSession { final LocalDateTime opened=LocalDateTime.now(); final BigDecimal opening; BigDecimal closing; boolean active=true; PosSession(BigDecimal o){opening=o;} BigDecimal expectedCash(BigDecimal cashSales){return opening.add(cashSales);} }
    private static final class SalesAnalyticsChart extends JPanel {
        private BigDecimal cash=BigDecimal.ZERO, mpesa=BigDecimal.ZERO, card=BigDecimal.ZERO;
        SalesAnalyticsChart() { setBackground(Color.WHITE); setPreferredSize(new Dimension(300, 260)); }
        void setValues(BigDecimal c, BigDecimal m, BigDecimal k) { cash=c; mpesa=m; card=k; repaint(); }
        protected void paintComponent(Graphics graphics) { super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(), h=getHeight(), left=50, base=h-42, maxH=h-95; BigDecimal max=cash.max(mpesa).max(card); if (max.signum()==0) max=BigDecimal.ONE; BigDecimal[] values={cash,mpesa,card}; String[] labels={"Cash","M-Pesa","Card"}; Color[] colors={BLUE,new Color(39,174,96),GOLD}; for(int i=0;i<3;i++){ int x=left+i*82; int bh=values[i].multiply(BigDecimal.valueOf(maxH)).divide(max,0,RoundingMode.HALF_UP).intValue(); g.setColor(colors[i]); g.fillRoundRect(x,base-bh,48,bh,10,10); g.setColor(new Color(91,101,115)); g.setFont(new Font("Segoe UI",Font.BOLD,11)); g.drawString(labels[i],x,base+18); g.setFont(new Font("Segoe UI",Font.PLAIN,10)); g.drawString("KES "+values[i].setScale(0,RoundingMode.HALF_UP),x,base-bh-7); } g.setColor(new Color(230,233,238)); g.drawLine(30,base,w-20,base); g.dispose(); }
    }
    private void syncBackendAsync() { new SwingWorker<Void, Void>() { protected Void doInBackground() { loadBackendData(); return null; } protected void done() { try { get(); refreshAll(); } catch (Exception ignored) { /* retain the last known data while offline */ } } }.execute(); }
    private static void applyAppFont() { FontUIResource font = new FontUIResource(FONT_FAMILY, Font.PLAIN, 14); Enumeration<Object> keys = UIManager.getDefaults().keys(); while (keys.hasMoreElements()) { Object key=keys.nextElement(); Object value=UIManager.get(key); if (value instanceof FontUIResource) UIManager.put(key, font); } }
    private static Image loadBrandLogo() { try (java.io.InputStream stream = MobileMealsPOS.class.getResourceAsStream("/logo.png")) { return stream == null ? null : ImageIO.read(stream); } catch (Exception ignored) { return null; } }
    public static void main(String[] args) { FlatLightLaf.setup(); applyAppFont(); SwingUtilities.invokeLater(() -> {
        RestaurantSession session = new RestaurantSession();
        if (!session.showLoginDialog(null)) return;
        try { new MobileMealsPOS(new POSApiClient(session)).setVisible(true); }
        catch (Exception error) { JOptionPane.showMessageDialog(null, error.getMessage(), "POS connection failed", JOptionPane.ERROR_MESSAGE); }
    }); }
}
