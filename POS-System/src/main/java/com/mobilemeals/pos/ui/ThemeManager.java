package com.mobilemeals.pos.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Theme Manager for Mobile Meals POS
 * Handles consistent theming with Mobile Meals brand colors
 */
public class ThemeManager {
    
    // Mobile Meals Brand Colors
    public static final Color PRIMARY_COLOR = new Color(41, 128, 185);        // #2980b9
    public static final Color PRIMARY_DARK = new Color(31, 97, 141);          // #1f618d
    public static final Color PRIMARY_LIGHT = new Color(52, 152, 219);        // #3498db
    public static final Color ACCENT_COLOR = new Color(231, 76, 60);          // #e74c3c
    public static final Color SUCCESS_COLOR = new Color(39, 174, 96);          // #27ae60
    public static final Color WARNING_COLOR = new Color(243, 156, 18);         // #f39c12
    public static final Color INFO_COLOR = new Color(52, 152, 219);           // #3498db
    public static final Color LIGHT_COLOR = new Color(236, 240, 241);         // #ecf0f1
    public static final Color DARK_COLOR = new Color(44, 62, 80);            // #2c3e50
    public static final Color TEXT_COLOR = new Color(52, 73, 94);             // #34495e
    public static final Color BACKGROUND_COLOR = new Color(255, 255, 255);     // #ffffff
    public static final Color SURFACE_COLOR = new Color(248, 249, 250);      // #f8f9fa
    
    // Semantic Colors
    public static final Color ERROR_COLOR = ACCENT_COLOR;
    public static final Color VALID_COLOR = SUCCESS_COLOR;
    public static final Color PENDING_COLOR = WARNING_COLOR;
    public static final Color COMPLETED_COLOR = SUCCESS_COLOR;
    public static final Color CANCELLED_COLOR = new Color(149, 165, 166);    // #95a5a6
    
    // Order Status Colors
    public static final Map<String, Color> ORDER_STATUS_COLORS = new HashMap<>();
    static {
        ORDER_STATUS_COLORS.put("pending", WARNING_COLOR);
        ORDER_STATUS_COLORS.put("confirmed", INFO_COLOR);
        ORDER_STATUS_COLORS.put("preparing", PRIMARY_COLOR);
        ORDER_STATUS_COLORS.put("ready", SUCCESS_COLOR);
        ORDER_STATUS_COLORS.put("picked_up", PRIMARY_LIGHT);
        ORDER_STATUS_COLORS.put("delivering", PRIMARY_DARK);
        ORDER_STATUS_COLORS.put("delivered", SUCCESS_COLOR);
        ORDER_STATUS_COLORS.put("cancelled", CANCELLED_COLOR);
    }
    
    private static ThemeManager instance;
    private String currentTheme = "light";
    
    private ThemeManager() {
        initializeTheme();
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public void initializeTheme() {
        try {
            // Apply FlatLaf with Mobile Meals colors
            UIManager.setLookAndFeel(new FlatLightLaf());
            
            // Customize colors to match Mobile Meals brand
            customizeLookAndFeel();
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to system look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void customizeLookAndFeel() {
        // Primary colors
        UIManager.put("Button.background", PRIMARY_COLOR);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.focus", PRIMARY_DARK);
        UIManager.put("Button.hover", PRIMARY_DARK);
        UIManager.put("Button.pressed", PRIMARY_DARK);
        
        // Secondary buttons
        UIManager.put("Button.secondary.background", LIGHT_COLOR);
        UIManager.put("Button.secondary.foreground", TEXT_COLOR);
        UIManager.put("Button.secondary.focus", SURFACE_COLOR);
        UIManager.put("Button.secondary.hover", SURFACE_COLOR);
        UIManager.put("Button.secondary.pressed", SURFACE_COLOR);
        
        // Success buttons
        UIManager.put("Button.success.background", SUCCESS_COLOR);
        UIManager.put("Button.success.foreground", Color.WHITE);
        UIManager.put("Button.success.focus", new Color(34, 153, 84));
        UIManager.put("Button.success.hover", new Color(34, 153, 84));
        UIManager.put("Button.success.pressed", new Color(34, 153, 84));
        
        // Warning buttons
        UIManager.put("Button.warning.background", WARNING_COLOR);
        UIManager.put("Button.warning.foreground", Color.WHITE);
        UIManager.put("Button.warning.focus", new Color(230, 126, 34));
        UIManager.put("Button.warning.hover", new Color(230, 126, 34));
        UIManager.put("Button.warning.pressed", new Color(230, 126, 34));
        
        // Danger buttons
        UIManager.put("Button.danger.background", ACCENT_COLOR);
        UIManager.put("Button.danger.foreground", Color.WHITE);
        UIManager.put("Button.danger.focus", new Color(192, 57, 43));
        UIManager.put("Button.danger.hover", new Color(192, 57, 43));
        UIManager.put("Button.danger.pressed", new Color(192, 57, 43));
        
        // Table colors
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", TEXT_COLOR);
        UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", LIGHT_COLOR);
        UIManager.put("TableHeader.background", SURFACE_COLOR);
        UIManager.put("TableHeader.foreground", TEXT_COLOR);
        
        // Panel colors
        UIManager.put("Panel.background", BACKGROUND_COLOR);
        UIManager.put("Panel.foreground", TEXT_COLOR);
        
        // Label colors
        UIManager.put("Label.foreground", TEXT_COLOR);
        UIManager.put("Label.disabledForeground", LIGHT_COLOR);
        
        // Text field colors
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", TEXT_COLOR);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(LIGHT_COLOR));
        UIManager.put("TextField.focusBorder", BorderFactory.createLineBorder(PRIMARY_COLOR));
        
        // Text area colors
        UIManager.put("TextArea.background", Color.WHITE);
        UIManager.put("TextArea.foreground", TEXT_COLOR);
        UIManager.put("TextArea.border", BorderFactory.createLineBorder(LIGHT_COLOR));
        UIManager.put("TextArea.focusBorder", BorderFactory.createLineBorder(PRIMARY_COLOR));
        
        // Combo box colors
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", TEXT_COLOR);
        UIManager.put("ComboBox.buttonBackground", LIGHT_COLOR);
        UIManager.put("ComboBox.selectionBackground", PRIMARY_COLOR);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        
        // Tab colors
        UIManager.put("TabbedPane.background", BACKGROUND_COLOR);
        UIManager.put("TabbedPane.foreground", TEXT_COLOR);
        UIManager.put("TabbedPane.selectedBackground", PRIMARY_COLOR);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
        UIManager.put("TabbedPane.focus", PRIMARY_DARK);
        
        // Scroll pane colors
        UIManager.put("ScrollPane.background", BACKGROUND_COLOR);
        UIManager.put("ScrollPane.foreground", TEXT_COLOR);
        
        // Menu colors
        UIManager.put("Menu.background", Color.WHITE);
        UIManager.put("Menu.foreground", TEXT_COLOR);
        UIManager.put("MenuItem.background", Color.WHITE);
        UIManager.put("MenuItem.foreground", TEXT_COLOR);
        UIManager.put("MenuItem.selectionBackground", PRIMARY_COLOR);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
    }
    
    public void setTheme(String themeName) {
        currentTheme = themeName;
        
        try {
            switch (themeName.toLowerCase()) {
                case "dark":
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    customizeDarkTheme();
                    break;
                case "intellij":
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    customizeIntelliJTheme();
                    break;
                case "mac":
                    UIManager.setLookAndFeel(new FlatMacLightLaf());
                    customizeMacTheme();
                    break;
                default:
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    customizeLookAndFeel();
                    break;
            }
            
            // Update all UI components
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
                window.pack();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void customizeDarkTheme() {
        UIManager.put("Panel.background", DARK_COLOR);
        UIManager.put("Panel.foreground", LIGHT_COLOR);
        UIManager.put("Table.background", DARK_COLOR);
        UIManager.put("Table.foreground", LIGHT_COLOR);
        UIManager.put("TextField.background", new Color(52, 73, 94));
        UIManager.put("TextField.foreground", LIGHT_COLOR);
        UIManager.put("TextArea.background", new Color(52, 73, 94));
        UIManager.put("TextArea.foreground", LIGHT_COLOR);
    }
    
    private void customizeIntelliJTheme() {
        // IntelliJ-specific customizations
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
    }
    
    private void customizeMacTheme() {
        // macOS-specific customizations
        UIManager.put("Button.arc", 6);
        UIManager.put("Component.arc", 6);
    }
    
    // UI Component Factory Methods
    
    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return button;
    }
    
    public static JButton createSuccessButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SUCCESS_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return button;
    }
    
    public static JButton createWarningButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(WARNING_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return button;
    }
    
    public static JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return button;
    }
    
    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(LIGHT_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(LIGHT_COLOR));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return button;
    }
    
    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setForeground(PRIMARY_COLOR);
        return label;
    }
    
    public static JLabel createSubHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    public static JLabel createBodyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    public static JLabel createCaptionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        label.setForeground(new Color(127, 140, 141));
        return label;
    }
    
    public static JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_COLOR),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textField.setBackground(Color.WHITE);
        return textField;
    }
    
    public static JTextField createTextField(String placeholder) {
        JTextField textField = createTextField();
        // Note: For true placeholder support, we'd need a custom component
        // This is a basic implementation
        return textField;
    }
    
    public static JTextArea createTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_COLOR),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textArea.setBackground(Color.WHITE);
        return textArea;
    }
    
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_COLOR),
            new EmptyBorder(16, 16, 16, 16)
        ));
        return panel;
    }
    
    public static JPanel createSidePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE_COLOR);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, LIGHT_COLOR));
        return panel;
    }
    
    public static JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY_COLOR);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }
    
    public static JSeparator createSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(LIGHT_COLOR);
        return separator;
    }
    
    public static Icon createIcon(String iconCode, int size) {
        // Simple icon creation using basic shapes and text
        // This is a basic implementation without external icon libraries
        try {
            if (iconCode.equals("home")) {
                return createTextIcon("🏠", size);
            } else if (iconCode.equals("settings")) {
                return createTextIcon("⚙️", size);
            } else if (iconCode.equals("logout")) {
                return createTextIcon("🚪", size);
            } else if (iconCode.equals("add")) {
                return createTextIcon("+", size);
            } else if (iconCode.equals("delete")) {
                return createTextIcon("×", size);
            } else if (iconCode.equals("edit")) {
                return createTextIcon("✏️", size);
            } else if (iconCode.equals("save")) {
                return createTextIcon("💾", size);
            } else if (iconCode.equals("print")) {
                return createTextIcon("🖨️", size);
            } else if (iconCode.equals("refresh")) {
                return createTextIcon("🔄", size);
            } else if (iconCode.equals("search")) {
                return createTextIcon("🔍", size);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static Icon createTextIcon(String text, int size) {
        // Create a simple icon using a label
        return new Icon() {
            private final Font font = new Font("Segoe UI", Font.PLAIN, size);
            private final FontMetrics fontMetrics = new JLabel().getFontMetrics(font);
            private final int width = fontMetrics.stringWidth(text);
            private final int height = fontMetrics.getHeight();
            
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setFont(font);
                g2d.setColor(TEXT_COLOR);
                g2d.drawString(text, x, y + height - fontMetrics.getDescent());
                g2d.dispose();
            }
            
            @Override
            public int getIconWidth() {
                return width;
            }
            
            @Override
            public int getIconHeight() {
                return height;
            }
        };
    }
    
    public static Color getOrderStatusColor(String status) {
        return ORDER_STATUS_COLORS.getOrDefault(status.toLowerCase(), TEXT_COLOR);
    }
    
    public static String getCurrentTheme() {
        return getInstance().currentTheme;
    }
    
    public static void applyThemeToComponent(JComponent component) {
        component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        component.setForeground(TEXT_COLOR);
        component.setBackground(BACKGROUND_COLOR);
    }
    
    public static void createThemeMenu(JMenu menu) {
        menu.setText("Theme");
        
        // Light theme
        JRadioButtonMenuItem lightTheme = new JRadioButtonMenuItem("Light");
        lightTheme.addActionListener(e -> getInstance().setTheme("light"));
        menu.add(lightTheme);
        
        // Dark theme
        JRadioButtonMenuItem darkTheme = new JRadioButtonMenuItem("Dark");
        darkTheme.addActionListener(e -> getInstance().setTheme("dark"));
        menu.add(darkTheme);
        
        // IntelliJ theme
        JRadioButtonMenuItem intellijTheme = new JRadioButtonMenuItem("IntelliJ");
        intellijTheme.addActionListener(e -> getInstance().setTheme("intellij"));
        menu.add(intellijTheme);
        
        // macOS theme
        JRadioButtonMenuItem macTheme = new JRadioButtonMenuItem("macOS");
        macTheme.addActionListener(e -> getInstance().setTheme("mac"));
        menu.add(macTheme);
        
        // Button group
        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightTheme);
        themeGroup.add(darkTheme);
        themeGroup.add(intellijTheme);
        themeGroup.add(macTheme);
        
        // Set current selection
        switch (getInstance().currentTheme) {
            case "dark":
                darkTheme.setSelected(true);
                break;
            case "intellij":
                intellijTheme.setSelected(true);
                break;
            case "mac":
                macTheme.setSelected(true);
                break;
            default:
                lightTheme.setSelected(true);
                break;
        }
    }
}
