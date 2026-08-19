package com.retailpos.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.event.ActionEvent;

public class RetailThemeManager {
    // Retail brand palette — light mode base
    public static Color PRIMARY    = new Color(37, 99, 235);   // #2563eb blue
    public static Color PRIMARY_DK = new Color(29, 78, 216);   // darker blue
    public static Color ACCENT     = new Color(16, 185, 129);  // #10b981 emerald
    public static Color DANGER     = new Color(239, 68,  68);  // #ef4444 red
    public static Color WARNING    = new Color(217, 119,  6);  // #d97706 amber
    public static Color NAVY       = new Color(15, 23, 42);    // header bg
    public static Color SURFACE    = new Color(248, 250, 252); // light bg
    public static Color CARD_BG    = Color.WHITE;
    public static Color TEXT       = new Color(15,  23, 42);
    public static Color TEXT_MUTED = new Color(100, 116, 139);
    public static Color BORDER     = new Color(203, 213, 225);

    // Dark mode palette — tuned for contrast/eye comfort on dark backgrounds
    // (softer, slightly desaturated accents; true near-black surfaces; layered card elevation)
    private static final Color DARK_PRIMARY   = new Color(96, 165, 250);  // #60a5fa lighter blue
    private static final Color DARK_PRIMARY_DK= new Color(59, 130, 246);
    private static final Color DARK_ACCENT    = new Color(52, 211, 153);  // #34d399 emerald
    private static final Color DARK_DANGER    = new Color(248, 113, 113); // #f87171 lighter red
    private static final Color DARK_WARNING   = new Color(250, 204, 21);  // #facc15 lighter amber
    private static final Color DARK_NAVY      = new Color(2,   6,  23);   // #020617
    private static final Color DARK_SURFACE   = new Color(15,  23,  42);  // #0f172a
    private static final Color DARK_CARD      = new Color(30,  41,  59);  // #1e293b
    private static final Color DARK_FIELD_BG  = new Color(17,  24,  39);  // #111827
    private static final Color DARK_HEADER_BG = new Color(2,   6,  23);
    private static final Color DARK_TEXT      = new Color(241, 245, 249); // #f1f5f9
    private static final Color DARK_TEXT_MUTED= new Color(148, 163, 184); // #94a3b8
    private static final Color DARK_BORDER    = new Color(51,  65,  85);  // #334155
    private static final Color DARK_SELECTION_BG = new Color(30, 58, 95);
    private static final Color DARK_SELECTION_FG = Color.WHITE;

    // Light mode selection
    private static final Color LIGHT_SELECTION_BG = new Color(219, 234, 254);
    private static final Color LIGHT_SELECTION_FG  = new Color(15, 23, 42);

    // Light mode originals, kept so toggling back to light restores exact values
    private static final Color LIGHT_PRIMARY   = new Color(37, 99, 235);
    private static final Color LIGHT_PRIMARY_DK= new Color(29, 78, 216);
    private static final Color LIGHT_ACCENT    = new Color(16, 185, 129);
    private static final Color LIGHT_DANGER    = new Color(239, 68, 68);
    private static final Color LIGHT_WARNING   = new Color(217, 119, 6);
    private static final Color LIGHT_NAVY      = new Color(15, 23, 42);
    private static final Color LIGHT_SURFACE   = new Color(248, 250, 252);
    private static final Color LIGHT_CARD      = Color.WHITE;
    private static final Color LIGHT_TEXT      = new Color(15, 23, 42);
    private static final Color LIGHT_TEXT_MUTED= new Color(100, 116, 139);
    private static final Color LIGHT_BORDER    = new Color(203, 213, 225);

    private static RetailThemeManager instance;
    private boolean dark = false;
    private String customPrimaryHex = null; // remembers a user-applied brand color across theme toggles

    private RetailThemeManager() {}

    public static RetailThemeManager getInstance() {
        if (instance == null) instance = new RetailThemeManager();
        return instance;
    }

    /**
     * Apply theme synchronously (used at startup).
     */
    public void apply(boolean dark) {
        this.dark = dark;
        try {
            if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        applyPalette(dark);
        if (customPrimaryHex != null) applyPrimaryColor(customPrimaryHex);
        applyUiDefaults();
        for (Window w : Window.getWindows()) {
            applyComponentTheme(w);
            SwingUtilities.updateComponentTreeUI(w);
            w.repaint();
        }
    }

    /**
     * Apply theme with a modern fullscreen overlay spinner.
     * Call from UI toggle actions so the user sees feedback during the switch.
     */
    public void applyWithOverlay(boolean dark, Window owner) {
        Color spinColor = dark ? new Color(96, 165, 250) : new Color(37, 99, 235);
        Color boxBg     = dark ? new Color(30, 41, 59)   : Color.WHITE;
        Color mutedFg   = dark ? new Color(148, 163, 184) : new Color(100, 116, 139);

        JWindow overlay = new JWindow(owner);
        overlay.setBackground(new Color(0, 0, 0, 0));

        JPanel glass = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        glass.setOpaque(false);

        JPanel spinnerBox = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(boxBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        spinnerBox.setOpaque(false);
        spinnerBox.setPreferredSize(new Dimension(180, 130));
        spinnerBox.setBorder(new EmptyBorder(24, 24, 16, 24));

        ThemeSpinner spinner = new ThemeSpinner(spinColor);
        JLabel lbl = new JLabel(dark ? "Switching to Dark Mode…" : "Switching to Light Mode…");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(mutedFg);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        spinnerBox.add(spinner, BorderLayout.CENTER);
        spinnerBox.add(lbl, BorderLayout.SOUTH);
        glass.add(spinnerBox);

        overlay.setContentPane(glass);
        if (owner != null) overlay.setBounds(owner.getBounds());
        else overlay.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        overlay.setVisible(true);
        spinner.start();

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { apply(dark); return null; }
            @Override protected void done() { spinner.stop(); overlay.dispose(); }
        }.execute();
    }

    public boolean isDark() { return dark; }

    public void applyPrimaryColor(String hexColor) {
        try {
            Color base = Color.decode(hexColor);
            customPrimaryHex = hexColor;
            PRIMARY = dark ? base.brighter() : base;
            PRIMARY_DK = base.darker();
            applyUiDefaults();
        } catch (Exception ignored) {
            customPrimaryHex = null;
            PRIMARY = dark ? DARK_PRIMARY : LIGHT_PRIMARY;
            PRIMARY_DK = dark ? DARK_PRIMARY_DK : LIGHT_PRIMARY_DK;
            applyUiDefaults();
        }
    }

    private void applyPalette(boolean darkMode) {
        if (darkMode) {
            PRIMARY    = DARK_PRIMARY;
            PRIMARY_DK = DARK_PRIMARY_DK;
            ACCENT     = DARK_ACCENT;
            DANGER     = DARK_DANGER;
            WARNING    = DARK_WARNING;
            NAVY       = DARK_NAVY;
            SURFACE    = DARK_SURFACE;
            CARD_BG    = DARK_CARD;
            TEXT       = DARK_TEXT;
            TEXT_MUTED = DARK_TEXT_MUTED;
            BORDER     = DARK_BORDER;
        } else {
            PRIMARY    = LIGHT_PRIMARY;
            PRIMARY_DK = LIGHT_PRIMARY_DK;
            ACCENT     = LIGHT_ACCENT;
            DANGER     = LIGHT_DANGER;
            WARNING    = LIGHT_WARNING;
            NAVY       = LIGHT_NAVY;
            SURFACE    = LIGHT_SURFACE;
            CARD_BG    = LIGHT_CARD;
            TEXT       = LIGHT_TEXT;
            TEXT_MUTED = LIGHT_TEXT_MUTED;
            BORDER     = LIGHT_BORDER;
        }
    }

    private void applyUiDefaults() {
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Component.focusColor", PRIMARY);

        // ── Tab selection ──────────────────────────────────────────────────────
        UIManager.put("TabbedPane.underlineColor",        PRIMARY);
        UIManager.put("TabbedPane.showTabSeparators",     false);
        UIManager.put("TabbedPane.selectedBackground",    dark ? DARK_CARD   : new Color(239, 246, 255));
        UIManager.put("TabbedPane.selectedForeground",    PRIMARY);
        UIManager.put("TabbedPane.hoverColor",            dark ? new Color(30, 41, 59, 200) : new Color(219, 234, 254));
        UIManager.put("TabbedPane.focusColor",            dark ? DARK_CARD   : new Color(239, 246, 255));
        UIManager.put("TabbedPane.background",            SURFACE);
        UIManager.put("TabbedPane.foreground",            TEXT);

        // ── Button hover / pressed ─────────────────────────────────────────────
        UIManager.put("Button.default.hoverBackground",   PRIMARY_DK);
        UIManager.put("Button.hoverBorderColor",          PRIMARY);
        UIManager.put("Button.pressedBackground",         dark ? new Color(37, 99, 235) : new Color(29, 78, 216));
        UIManager.put("Button.selectedBackground",        dark ? DARK_SELECTION_BG : LIGHT_SELECTION_BG);
        UIManager.put("Button.selectedForeground",        TEXT);
        UIManager.put("Button.background",                CARD_BG);
        UIManager.put("Button.foreground",                TEXT);

        // ── Panels / containers ────────────────────────────────────────────────
        UIManager.put("Panel.background",         SURFACE);
        UIManager.put("Viewport.background",      SURFACE);
        UIManager.put("ScrollPane.background",    SURFACE);

        // ── Table ──────────────────────────────────────────────────────────────
        UIManager.put("Table.background",            CARD_BG);
        UIManager.put("Table.foreground",            TEXT);
        UIManager.put("Table.gridColor",             BORDER);
        UIManager.put("Table.selectionBackground",   selectionBg());
        UIManager.put("Table.selectionForeground",   selectionFg());
        UIManager.put("TableHeader.background",      dark ? DARK_HEADER_BG : SURFACE);
        UIManager.put("TableHeader.foreground",      TEXT);

        // ── Input fields ───────────────────────────────────────────────────────
        UIManager.put("TextField.background",      fieldBg());
        UIManager.put("TextField.foreground",      TEXT);
        UIManager.put("PasswordField.background",  fieldBg());
        UIManager.put("PasswordField.foreground",  TEXT);
        UIManager.put("TextArea.background",       fieldBg());
        UIManager.put("TextArea.foreground",       TEXT);
        UIManager.put("ComboBox.background",       fieldBg());
        UIManager.put("ComboBox.foreground",       TEXT);
        UIManager.put("Spinner.background",        fieldBg());
        UIManager.put("Spinner.foreground",        TEXT);

        // ── Misc ───────────────────────────────────────────────────────────────
        UIManager.put("ToolTip.background",    CARD_BG);
        UIManager.put("ToolTip.foreground",    TEXT);
        UIManager.put("ToolTip.border",        BorderFactory.createLineBorder(BORDER));
        UIManager.put("Separator.foreground",  BORDER);
        UIManager.put("Label.foreground",      TEXT);
        UIManager.put("CheckBox.foreground",   TEXT);
        UIManager.put("RadioButton.foreground",TEXT);
        UIManager.put("List.background",       CARD_BG);
        UIManager.put("List.foreground",       TEXT);
        UIManager.put("List.selectionBackground", selectionBg());
        UIManager.put("List.selectionForeground", selectionFg());
        UIManager.put("Menu.background",       CARD_BG);
        UIManager.put("Menu.foreground",       TEXT);
        UIManager.put("MenuBar.background",    SURFACE);
        UIManager.put("MenuItem.background",   CARD_BG);
        UIManager.put("MenuItem.foreground",   TEXT);
        UIManager.put("PopupMenu.background",  CARD_BG);
        UIManager.put("PopupMenu.border",      BorderFactory.createLineBorder(BORDER));
    }

    private void applyComponentTheme(Component component) {
        if (component instanceof JDialog dialog) {
            dialog.getContentPane().setBackground(SURFACE);
        } else if (component instanceof JPanel panel) {
            // Only repaint opaque panels — transparent ones inherit from parent
            if (panel.isOpaque()) {
                panel.setBackground(panel.getBorder() != null ? CARD_BG : SURFACE);
            }
        } else if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBackground(SURFACE);
            scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            scrollPane.getViewport().setBackground(SURFACE);
        } else if (component instanceof JTable table) {
            table.setBackground(CARD_BG);
            table.setForeground(TEXT);
            table.setGridColor(BORDER);
            table.setSelectionBackground(selectionBg());
            table.setSelectionForeground(selectionFg());
            table.getTableHeader().setBackground(dark ? DARK_HEADER_BG : SURFACE);
            table.getTableHeader().setForeground(TEXT);
        } else if (component instanceof JPasswordField passwordField) {
            passwordField.setBackground(fieldBg());
            passwordField.setForeground(TEXT);
            passwordField.setCaretColor(TEXT);
        } else if (component instanceof JTextField textField) {
            textField.setBackground(fieldBg());
            textField.setForeground(TEXT);
            textField.setCaretColor(TEXT);
            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(10, 14, 10, 14)));
        } else if (component instanceof JTextArea textArea) {
            textArea.setBackground(fieldBg());
            textArea.setForeground(TEXT);
            textArea.setCaretColor(TEXT);
        } else if (component instanceof JComboBox<?> comboBox) {
            comboBox.setBackground(fieldBg());
            comboBox.setForeground(TEXT);
        } else if (component instanceof JSpinner spinner) {
            spinner.setBackground(fieldBg());
            spinner.setForeground(TEXT);
            if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
                editor.getTextField().setBackground(fieldBg());
                editor.getTextField().setForeground(TEXT);
                editor.getTextField().setCaretColor(TEXT);
            }
        } else if (component instanceof JCheckBox checkBox) {
            checkBox.setForeground(TEXT);
            // Let the checkbox inherit its parent's background rather than forcing SURFACE
            if (checkBox.getParent() != null) {
                checkBox.setBackground(checkBox.getParent().getBackground());
            }
        } else if (component instanceof JTabbedPane tabbedPane) {
            tabbedPane.setBackground(SURFACE);
            tabbedPane.setForeground(TEXT);
        } else if (component instanceof JButton btn) {
            // Re-apply colours for factory-built buttons based on their current bg hue
            Color bg = btn.getBackground();
            if (bg != null) {
                if (isColorClose(bg, LIGHT_PRIMARY) || isColorClose(bg, DARK_PRIMARY)) {
                    btn.setBackground(PRIMARY);
                    btn.setForeground(dark ? DARK_NAVY : Color.WHITE);
                } else if (isColorClose(bg, LIGHT_ACCENT) || isColorClose(bg, DARK_ACCENT)) {
                    btn.setBackground(ACCENT);
                    btn.setForeground(dark ? DARK_NAVY : Color.WHITE);
                } else if (isColorClose(bg, LIGHT_DANGER) || isColorClose(bg, DARK_DANGER)) {
                    btn.setBackground(DANGER);
                    btn.setForeground(dark ? DARK_NAVY : Color.WHITE);
                } else {
                    // secondary button
                    btn.setBackground(CARD_BG);
                    btn.setForeground(TEXT);
                }
            }
        } else if (component instanceof JLabel label && isManagedLabelColor(label.getForeground())) {
            label.setForeground(label.getFont().isBold() ? TEXT : TEXT_MUTED);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) applyComponentTheme(child);
        }
    }

    // ── Button factory ────────────────────────────────────────────────────────

    public static JButton primaryButton(String text) { return primaryButton(text, null); }

    public static JButton primaryButton(String text, String iconName) {
        JButton b = new JButton(text);
        if (iconName != null) b.setIcon(Icons.get(iconName, 18));
        b.setBackground(PRIMARY);
        b.setForeground(getInstance().isDark() ? DARK_NAVY : Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setIconTextGap(8);
        b.setBorder(new EmptyBorder(11, 20, 11, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMinimumSize(new Dimension(100, 44));
        return b;
    }

    public static JButton successButton(String text) { return successButton(text, null); }

    public static JButton successButton(String text, String iconName) {
        JButton b = new JButton(text);
        if (iconName != null) b.setIcon(Icons.get(iconName, 18));
        b.setBackground(ACCENT);
        b.setForeground(getInstance().isDark() ? DARK_NAVY : Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setIconTextGap(8);
        b.setBorder(new EmptyBorder(11, 20, 11, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMinimumSize(new Dimension(100, 44));
        return b;
    }

    public static JButton dangerButton(String text) { return dangerButton(text, null); }

    public static JButton dangerButton(String text, String iconName) {
        JButton b = new JButton(text);
        if (iconName != null) b.setIcon(Icons.get(iconName, 18));
        b.setBackground(DANGER);
        b.setForeground(getInstance().isDark() ? DARK_NAVY : Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setIconTextGap(8);
        b.setBorder(new EmptyBorder(11, 20, 11, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMinimumSize(new Dimension(100, 44));
        return b;
    }

    public static JButton secondaryButton(String text) { return secondaryButton(text, null); }

    public static JButton secondaryButton(String text, String iconName) {
        JButton b = new JButton(text);
        if (iconName != null) b.setIcon(Icons.get(iconName, 16));
        b.setBackground(CARD_BG); b.setForeground(TEXT);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setIconTextGap(7);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(10, 16, 10, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMinimumSize(new Dimension(80, 42));
        return b;
    }

    // ── Field factory ─────────────────────────────────────────────────────────

    public static JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(getInstance().fieldBg());
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setPreferredSize(new Dimension(200, 44));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(10, 14, 10, 14)));
        return f;
    }

    public static JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(getInstance().fieldBg());
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setPreferredSize(new Dimension(200, 44));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(10, 14, 10, 14)));
        return f;
    }

    // ── Label factory ─────────────────────────────────────────────────────────

    public static JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 24));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel subLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    // ── Panel / table helpers ─────────────────────────────────────────────────

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(16, 16, 16, 16)));
        return p;
    }

    public static JTable styledTable(javax.swing.table.TableModel model) {
        JTable t = new JTable(model) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        t.setRowHeight(42);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
        t.getTableHeader().setBackground(getInstance().isDark() ? DARK_HEADER_BG : SURFACE);
        t.getTableHeader().setForeground(TEXT);
        t.setBackground(CARD_BG);
        t.setForeground(TEXT);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDER);
        t.setSelectionBackground(getInstance().selectionBg());
        t.setSelectionForeground(getInstance().selectionFg());
        t.setAutoCreateRowSorter(true);
        return t;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Metric card for dashboard: title + big value */
    public static JPanel metricCard(String title, String value, Color valueColor) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(18, 20, 18, 20)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setForeground(TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 26));
        v.setForeground(valueColor != null ? valueColor : TEXT);
        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    public Color fieldBg() { return dark ? DARK_FIELD_BG : Color.WHITE; }
    public Color selectionBg() { return dark ? DARK_SELECTION_BG : LIGHT_SELECTION_BG; }
    public Color selectionFg() { return dark ? DARK_SELECTION_FG : LIGHT_SELECTION_FG; }

    private boolean isManagedLabelColor(Color color) {
        return color == null
            || Color.BLACK.equals(color)
            || LIGHT_TEXT.equals(color)
            || LIGHT_TEXT_MUTED.equals(color)
            || DARK_TEXT.equals(color)
            || DARK_TEXT_MUTED.equals(color);
    }

    /** Returns true if two colors are within a small Euclidean distance (tolerance ~40). */
    private static boolean isColorClose(Color a, Color b) {
        if (a == null || b == null) return false;
        int dr = a.getRed()   - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue()  - b.getBlue();
        return (dr*dr + dg*dg + db*db) < 1600; // 40^2
    }

    // ── Animated arc spinner used during theme switching ──────────────────────

    public static class ThemeSpinner extends JPanel {
        private final Color color;
        private float angle = 0;
        private Timer timer;

        public ThemeSpinner(Color color) {
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(48, 48));
        }

        public void start() {
            timer = new Timer(16, (ActionEvent e) -> {
                angle = (angle + 6) % 360;
                repaint();
            });
            timer.start();
        }

        public void stop() {
            if (timer != null) timer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 6;
            int x = (getWidth()  - size) / 2;
            int y = (getHeight() - size) / 2;
            float stroke = size * 0.12f;
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Track
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
            g2.drawOval(x, y, size, size);
            // Arc
            g2.setColor(color);
            g2.draw(new Arc2D.Float(x, y, size, size, angle, 100, Arc2D.OPEN));
            g2.dispose();
        }
    }
}
