package com.retailpos;

import com.formdev.flatlaf.FlatLightLaf;
import com.retailpos.service.AuthService;
import com.retailpos.service.LicenseService;
import com.retailpos.ui.RetailThemeManager;
import com.retailpos.util.DatabaseManager;
import com.retailpos.util.AppPaths;
import com.retailpos.view.LoginDialog;
import com.retailpos.view.LicenseActivationDialog;
import com.retailpos.view.MainFrame;
import com.retailpos.view.SetupWizard;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.nio.file.Path;
import java.util.Enumeration;

public class RetailPOS {

    public static void main(String[] args) {
        // Setup Look & Feel before any UI components
        FlatLightLaf.setup();
        applyGlobalFont();

        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize database
                Path databaseFile = AppPaths.databaseFile();
                DatabaseManager.initialize(databaseFile.toString());
                DatabaseManager.createAllTables();
                // Check if setup is complete
                com.retailpos.repository.SettingsRepository settingsRepo = new com.retailpos.repository.SettingsRepository();
                com.retailpos.model.AppSettings settings = settingsRepo.load();
                RetailThemeManager.getInstance().applyPrimaryColor(settings.getPrimaryColor());
                
                if (!settings.isSetupComplete()) {
                    // Show setup wizard
                    SetupWizard wizard = new SetupWizard(null);
                    wizard.setVisible(true);
                    if (!wizard.isSetupComplete()) {
                        DatabaseManager.close();
                        System.exit(0);
                        return;
                    }
                    // Reload settings after setup
                    settings = settingsRepo.load();
                }

                LicenseService.LicenseSnapshot license =
                    LicenseService.getInstance().checkAccess();
                if (!license.isAllowed()) {
                    LicenseActivationDialog activation =
                        new LicenseActivationDialog(null, license, false);
                    activation.setVisible(true);
                    if (!activation.isActivated()) {
                        DatabaseManager.close();
                        System.exit(0);
                        return;
                    }
                }

                // Show login
                LoginDialog login = new LoginDialog(null);
                login.setVisible(true);

                if (!login.isLoginSuccessful()) {
                    DatabaseManager.close();
                    System.exit(0);
                    return;
                }

                // Apply saved theme
                try {
                    com.retailpos.repository.SettingsRepository sr = new com.retailpos.repository.SettingsRepository();
                    com.retailpos.model.AppSettings s = sr.load();
                    if (s.isDarkMode()) RetailThemeManager.getInstance().apply(true);
                } catch (Exception ignored) {}

                // Launch main window
                MainFrame frame = new MainFrame();
                frame.setVisible(true);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Failed to start BizFlow POS:\n" + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    private static void applyGlobalFont() {
        // Segoe UI at 14pt — clear, readable on all Windows DPI settings
        FontUIResource font = new FontUIResource("Segoe UI", Font.PLAIN, 14);
        FontUIResource boldFont = new FontUIResource("Segoe UI", Font.BOLD, 14);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource existing) {
                boolean isBold = existing.isBold();
                UIManager.put(key, isBold ? boldFont : font);
            }
        }
        // Ensure table header is always readable
        UIManager.put("TableHeader.font", boldFont);
        UIManager.put("Table.font", font);
        UIManager.put("TabbedPane.font", boldFont);
        UIManager.put("Button.font", boldFont);
        UIManager.put("Label.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("List.font", font);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("TabbedPane.tabArc", 10);
        UIManager.put("TabbedPane.tabInsets", new Insets(12, 18, 12, 18));
        UIManager.put("TabbedPane.selectedBackground", new Color(219, 234, 254));
        UIManager.put("TabbedPane.underlineColor", RetailThemeManager.PRIMARY);
        UIManager.put("ScrollBar.width", 12);
    }
}


