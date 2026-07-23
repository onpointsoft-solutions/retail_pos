package com.retailpos.view;

import com.retailpos.model.AppSettings;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.repository.UserRepository;
import com.retailpos.ui.RetailThemeManager;
import com.retailpos.util.AppPaths;
import com.retailpos.util.PasswordUtil;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** First-launch configuration for the store, branding, and administrator account. */
public class SetupWizard extends JDialog {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MIN_USERNAME_LENGTH = 3;

    private static final String[] COLOR_OPTIONS = {
        "Professional Blue (#2563EB)", "Emerald (#059669)", "Violet (#7C3AED)", "Amber (#D97706)", "Rose (#E11D48)"
    };

    private boolean setupComplete;
    private final List<JComponent> formControls = new ArrayList<>();
    private final Border defaultFieldBorder;

    public SetupWizard(Frame parent) {
        super(parent, "Welcome to Retail POS", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(720, 760));
        setSize(780, 860);
        setLocationRelativeTo(parent);
        defaultFieldBorder = RetailThemeManager.styledField().getBorder();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                JOptionPane.showMessageDialog(SetupWizard.this,
                    "Setup must be completed before you can use Retail POS.",
                    "Setup required", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        buildUi();
    }

    public boolean isSetupComplete() { return setupComplete; }

    private void buildUi() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(RetailThemeManager.SURFACE);
        content.setBorder(new EmptyBorder(26, 36, 26, 36));

        JLabel title = RetailThemeManager.headerLabel("Set up your retail workspace");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        JLabel subtitle = RetailThemeManager.subLabel("This takes a minute. You can refine these settings later.");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(Box.createVerticalStrut(4)); content.add(subtitle); content.add(Box.createVerticalStrut(18));

        // --- Store details ---
        JPanel storeCard = section("Store details", "Shown on receipts and throughout the POS.");
        JTextField shopName = (JTextField) addField(storeCard, "Shop name *", false);
        JTextField address = (JTextField) addField(storeCard, "Shop address", false);
        JTextField phone = (JTextField) addField(storeCard, "Shop phone", false);
        JTextField logoPath = (JTextField) addField(storeCard, "Store logo", true);

        JPanel logoRow = new JPanel();
        logoRow.setLayout(new BoxLayout(logoRow, BoxLayout.X_AXIS));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton uploadLogo = RetailThemeManager.secondaryButton("Upload logo");
        JLabel logoPreview = new JLabel();
        logoPreview.setPreferredSize(new Dimension(48, 48));
        logoPreview.setBorder(new LineBorder(new Color(0, 0, 0, 30), 1));
        logoRow.add(uploadLogo);
        logoRow.add(Box.createHorizontalStrut(10));
        logoRow.add(logoPreview);
        storeCard.add(logoRow);
        uploadLogo.addActionListener(e -> chooseLogo(logoPath, logoPreview));
        formControls.add(uploadLogo);
        content.add(storeCard); content.add(Box.createVerticalStrut(14));

        // --- Appearance ---
        JPanel brandCard = section("Appearance", "Choose a primary brand color and display mode.");
        JComboBox<String> primaryColor = new JComboBox<>(COLOR_OPTIONS);
        primaryColor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        primaryColor.setRenderer(new ColorSwatchRenderer());
        brandCard.add(new JLabel("Primary color")); brandCard.add(Box.createVerticalStrut(4)); brandCard.add(primaryColor);
        JCheckBox darkMode = new JCheckBox("Use dark mode"); darkMode.setOpaque(false); darkMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandCard.add(Box.createVerticalStrut(10)); brandCard.add(darkMode);
        formControls.add(primaryColor); formControls.add(darkMode);
        content.add(brandCard); content.add(Box.createVerticalStrut(14));

        // --- Administrator account ---
        JPanel adminCard = section("Administrator account", "Use these credentials to sign in after setup.");
        JTextField adminName = (JTextField) addField(adminCard, "Administrator name *", false);
        JTextField username = (JTextField) addField(adminCard, "Username *", false);
        JPasswordField password = addPasswordField(adminCard, "Password (at least 8 characters) *");
        JLabel strengthHint = smallHint(" ");
        adminCard.add(strengthHint); adminCard.add(Box.createVerticalStrut(6));
        JPasswordField confirmation = addPasswordField(adminCard, "Confirm password *");
        JLabel matchHint = smallHint(" ");
        adminCard.add(matchHint);
        content.add(adminCard); content.add(Box.createVerticalStrut(12));

        password.getDocument().addDocumentListener(onChange(() -> {
            clearFieldError(password);
            int len = password.getPassword().length;
            if (len == 0) { strengthHint.setText(" "); }
            else if (len < MIN_PASSWORD_LENGTH) { strengthHint.setForeground(RetailThemeManager.DANGER); strengthHint.setText("Needs at least " + MIN_PASSWORD_LENGTH + " characters."); }
            else { strengthHint.setForeground(new Color(5, 150, 105)); strengthHint.setText("Looks good."); }
            updateMatchHint(password, confirmation, matchHint);
        }));
        confirmation.getDocument().addDocumentListener(onChange(() -> {
            clearFieldError(confirmation);
            updateMatchHint(password, confirmation, matchHint);
        }));
        shopName.getDocument().addDocumentListener(onChange(() -> clearFieldError(shopName)));
        adminName.getDocument().addDocumentListener(onChange(() -> clearFieldError(adminName)));
        username.getDocument().addDocumentListener(onChange(() -> clearFieldError(username)));

        JLabel error = new JLabel(" "); error.setForeground(RetailThemeManager.DANGER); error.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(error);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        content.add(Box.createVerticalStrut(6)); content.add(progress); content.add(Box.createVerticalStrut(6));

        JButton finish = RetailThemeManager.primaryButton("Complete setup"); finish.setAlignmentX(Component.LEFT_ALIGNMENT);
        finish.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48)); content.add(finish);
        formControls.add(shopName); formControls.add(address); formControls.add(phone); formControls.add(logoPath);
        formControls.add(adminName); formControls.add(username); formControls.add(password); formControls.add(confirmation);
        formControls.add(finish);

        finish.addActionListener(e -> save(shopName, address, phone, logoPath, primaryColor, darkMode,
            adminName, username, password, confirmation, error, finish, progress));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(18);
        setContentPane(scroll);
        getRootPane().setDefaultButton(finish);
    }

    private JPanel section(String heading, String description) {
        JPanel panel = RetailThemeManager.card();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel(heading); title.setFont(new Font("Segoe UI", Font.BOLD, 16)); title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel detail = RetailThemeManager.subLabel(description); detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title); panel.add(Box.createVerticalStrut(3)); panel.add(detail); panel.add(Box.createVerticalStrut(12));
        return panel;
    }

    private JLabel smallHint(String text) {
        JLabel hint = new JLabel(text);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        return hint;
    }

    private JComponent addField(JPanel parent, String label, boolean readOnly) {
        JLabel fieldLabel = new JLabel(label); fieldLabel.setFont(new Font("Segoe UI", Font.BOLD, 12)); fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField field = RetailThemeManager.styledField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setEditable(!readOnly);
        parent.add(fieldLabel); parent.add(Box.createVerticalStrut(4)); parent.add(field); parent.add(Box.createVerticalStrut(10));
        return field;
    }

    private JPasswordField addPasswordField(JPanel parent, String label) {
        JLabel fieldLabel = new JLabel(label); fieldLabel.setFont(new Font("Segoe UI", Font.BOLD, 12)); fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPasswordField field = RetailThemeManager.styledPasswordField();
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        JToggleButton toggle = new JToggleButton("Show");
        toggle.setFocusable(false);
        char hiddenChar = field.getEchoChar();
        toggle.addActionListener(e -> {
            field.setEchoChar(toggle.isSelected() ? (char) 0 : hiddenChar);
            toggle.setText(toggle.isSelected() ? "Hide" : "Show");
        });

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.add(field, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);

        parent.add(fieldLabel); parent.add(Box.createVerticalStrut(4)); parent.add(row); parent.add(Box.createVerticalStrut(6));
        return field;
    }

    private DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    private void updateMatchHint(JPasswordField password, JPasswordField confirmation, JLabel matchHint) {
        if (confirmation.getPassword().length == 0) { matchHint.setText(" "); return; }
        boolean matches = java.util.Arrays.equals(password.getPassword(), confirmation.getPassword());
        matchHint.setForeground(matches ? new Color(5, 150, 105) : RetailThemeManager.DANGER);
        matchHint.setText(matches ? "Passwords match." : "Passwords do not match.");
    }

    private void markFieldError(JComponent field) {
        field.setBorder(new LineBorder(RetailThemeManager.DANGER, 2, true));
        field.requestFocusInWindow();
    }

    private void clearFieldError(JComponent field) {
        if (field.getBorder() instanceof LineBorder) field.setBorder(defaultFieldBorder);
    }

    private void clearAllFieldErrors(JComponent... fields) {
        for (JComponent field : fields) clearFieldError(field);
    }

    private void chooseLogo(JTextField logoPath, JLabel preview) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String saved = AppPaths.copyImage(chooser.getSelectedFile().toPath(), "logo").toString();
            logoPath.setText(saved);
            ImageIcon icon = new ImageIcon(saved);
            Image scaled = icon.getImage().getScaledInstance(46, 46, Image.SCALE_SMOOTH);
            preview.setIcon(new ImageIcon(scaled));
        } catch (Exception exception) {
            preview.setIcon(null);
            JOptionPane.showMessageDialog(this, "Logo upload failed: " + exception.getMessage(), "Upload error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void save(JTextField shopName, JTextField address, JTextField phone, JTextField logoPath, JComboBox<String> primaryColor,
                      JCheckBox darkMode, JTextField adminName, JTextField username, JPasswordField password,
                      JPasswordField confirmation, JLabel error, JButton finish, JProgressBar progress) {
        clearAllFieldErrors(shopName, adminName, username, password, confirmation);
        error.setText(" ");

        String shopNameValue = shopName.getText().trim();
        String addressValue = address.getText().trim();
        String phoneValue = phone.getText().trim();
        String logoValue = logoPath.getText().trim();
        String adminNameValue = adminName.getText().trim();
        String usernameValue = username.getText().trim();
        String passwordValue = new String(password.getPassword());
        String confirmationValue = new String(confirmation.getPassword());
        String colorValue = colorValue((String) primaryColor.getSelectedItem());
        boolean darkModeValue = darkMode.isSelected();

        if (shopNameValue.isEmpty()) { error.setText("Shop name is required."); markFieldError(shopName); return; }
        if (adminNameValue.isEmpty()) { error.setText("Administrator name is required."); markFieldError(adminName); return; }
        if (usernameValue.isEmpty()) { error.setText("Username is required."); markFieldError(username); return; }
        if (usernameValue.contains(" ")) { error.setText("Username cannot contain spaces."); markFieldError(username); return; }
        if (usernameValue.length() < MIN_USERNAME_LENGTH) { error.setText("Username must be at least " + MIN_USERNAME_LENGTH + " characters."); markFieldError(username); return; }
        if (passwordValue.length() < MIN_PASSWORD_LENGTH) { error.setText("Use a password of at least " + MIN_PASSWORD_LENGTH + " characters."); markFieldError(password); return; }
        if (!passwordValue.equals(confirmationValue)) { error.setText("Passwords do not match."); markFieldError(confirmation); return; }

        setFormEnabled(false);
        progress.setVisible(true);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                AppSettings settings = new SettingsRepository().load();
                settings.setStoreName(shopNameValue); settings.setStoreAddress(addressValue);
                settings.setStorePhone(phoneValue); settings.setLogoPath(logoValue);
                settings.setPrimaryColor(colorValue); settings.setDarkMode(darkModeValue);
                settings.setSetupComplete(true); new SettingsRepository().save(settings);
                new UserRepository().configureInitialAdmin(adminNameValue, usernameValue, PasswordUtil.hash(passwordValue));
                return null;
            }
            @Override protected void done() {
                progress.setVisible(false);
                try {
                    get();
                    RetailThemeManager.getInstance().applyPrimaryColor(colorValue);
                    setupComplete = true;
                    dispose();
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
                    String message = cause.getMessage() != null ? cause.getMessage() : "An unexpected error occurred.";
                    if (message.toLowerCase().contains("username")) {
                        error.setText("That username is already taken. Please choose another.");
                        markFieldError(username);
                    } else {
                        error.setText("Setup failed: " + message);
                    }
                    setFormEnabled(true);
                }
            }
        }.execute();
    }

    private void setFormEnabled(boolean enabled) {
        for (JComponent component : formControls) component.setEnabled(enabled);
    }

    private String colorValue(String value) { return value.substring(value.indexOf('#'), value.indexOf(')')); }

    /** Renders each color option in the dropdown with a small filled swatch matching its hex value. */
    private class ColorSwatchRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setIconTextGap(8);
            try {
                Color swatch = Color.decode(colorValue((String) value));
                label.setIcon(new Icon() {
                    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                        g.setColor(swatch);
                        g.fillOval(x, y, 14, 14);
                    }
                    @Override public int getIconWidth() { return 14; }
                    @Override public int getIconHeight() { return 14; }
                });
            } catch (Exception ignored) {
                label.setIcon(null);
            }
            return label;
        }
    }
}