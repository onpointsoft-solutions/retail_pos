package com.retailpos.view;

import com.retailpos.service.AuthService;
import com.retailpos.ui.Icons;
import com.retailpos.ui.RetailThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class LoginDialog extends JDialog {
    private boolean loginSuccessful = false;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginButton;

    public LoginDialog(Frame parent) {
        super(parent, "BizFlow POS — Sign In", true);
        setSize(900, 560);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Left branding panel
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(RetailThemeManager.NAVY);
        brand.setPreferredSize(new Dimension(320, 0));
        brand.setBorder(new EmptyBorder(60, 40, 60, 40));
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.anchor = GridBagConstraints.NORTHWEST; bc.insets = new Insets(0, 0, 10, 0);

        // Drawn cart icon instead of emoji
        JLabel logo = new JLabel(Icons.get("cart", 56));
        brand.add(logo, bc);

        bc.gridy = 1; bc.insets = new Insets(12, 0, 4, 0);
        JLabel title = new JLabel("BizFlow POS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        brand.add(title, bc);

        bc.gridy = 2; bc.insets = new Insets(0, 0, 32, 0);
        JLabel sub = new JLabel("<html><span style='color:#94a3b8'>Fast, reliable, offline-first</span></html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        brand.add(sub, bc);

        bc.gridy = 3; bc.insets = new Insets(0, 0, 6, 0);
        String[] features = {"[v]  Works without internet", "[v]  Auto-sync when online",
                             "[v]  Barcode & QR scanning",  "[v]  Multi-user roles"};
        for (String f : features) {
            JLabel fl = new JLabel(f);
            fl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            fl.setForeground(new Color(148, 163, 184));
            brand.add(fl, bc); bc.gridy++;
        }

        // Right form panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(70, 60, 60, 60));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;

        g.gridy = 0; g.insets = new Insets(0, 0, 4, 0);
        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setForeground(RetailThemeManager.TEXT);
        form.add(welcome, g);

        g.gridy = 1; g.insets = new Insets(0, 0, 32, 0);
        JLabel hint = new JLabel("Sign in to start your cashier session");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hint.setForeground(RetailThemeManager.TEXT_MUTED);
        form.add(hint, g);

        g.gridy = 2; g.insets = new Insets(0, 0, 4, 0);
        form.add(fieldLabel("Username"), g);

        g.gridy = 3; g.insets = new Insets(0, 0, 16, 0);
        usernameField = RetailThemeManager.styledField();
        usernameField.setPreferredSize(new Dimension(300, 44));
        form.add(usernameField, g);

        g.gridy = 4; g.insets = new Insets(0, 0, 4, 0);
        form.add(fieldLabel("Password"), g);

        g.gridy = 5; g.insets = new Insets(0, 0, 8, 0);
        passwordField = RetailThemeManager.styledPasswordField();
        passwordField.setPreferredSize(new Dimension(300, 44));
        form.add(passwordField, g);

        g.gridy = 6; g.insets = new Insets(0, 0, 20, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(RetailThemeManager.DANGER);
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(errorLabel, g);

        g.gridy = 7; g.insets = new Insets(0, 0, 0, 0);
        loginButton = RetailThemeManager.primaryButton("SIGN IN");
        loginButton.setPreferredSize(new Dimension(300, 44));
        form.add(loginButton, g);

        root.add(brand, BorderLayout.WEST);
        root.add(form, BorderLayout.CENTER);
        setContentPane(root);

        // Actions
        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        // Default focus
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { usernameField.requestFocus(); }
        });

        // Escape closes
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(RetailThemeManager.TEXT);
        return l;
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password");
            return;
        }
        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        errorLabel.setText(" ");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            String errorMsg = null;
            @Override protected Boolean doInBackground() {
                try { return AuthService.getInstance().login(username, password); }
                catch (Exception e) { errorMsg = e.getMessage(); return false; }
            }
            @Override protected void done() {
                loginButton.setEnabled(true); loginButton.setText("SIGN IN");
                setCursor(Cursor.getDefaultCursor());
                try {
                    if (get()) {
                        loginSuccessful = true;
                        dispose();
                    } else {
                        errorLabel.setText(errorMsg != null ? errorMsg : "Invalid username or password");
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception e) {
                    errorLabel.setText("Connection error. Please try again.");
                    passwordField.setText("");
                }
            }
        };
        worker.execute();
    }

    public boolean isLoginSuccessful() { return loginSuccessful; }
}
