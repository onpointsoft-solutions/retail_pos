package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;

/**
 * Restaurant Session Management for POS
 * Handles authentication, restaurant info, and session state
 */
public class RestaurantSession {
    
    private boolean isLoggedIn;
    private String restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantPhone;
    private String userId;
    private String userName;
    private String userRole;
    private LocalDateTime loginTime;
    private String apiToken;
    private POSApiClient apiClient;

    // API Configuration
    private static final String API_BASE_URL = "https://www.mobilemealscenter.co.ke/api/";
    private static final String LOGIN_ENDPOINT = "auth/login/";
    private static final String RESTAURANT_INFO_ENDPOINT = "restaurants/me/";
    private static final String LOGOUT_ENDPOINT = "auth/logout/";

    public RestaurantSession() {
        this.isLoggedIn = false;
        this.apiClient = new POSApiClient(this);
        loadSavedSession();
    }
    
    public boolean login(String username, String password) {
        try {
            // Simulate API call for login
            Map<String, Object> loginData = authenticateUser(username, password);
            
            if (loginData != null) {
                this.isLoggedIn = true;
                this.userId = (String) loginData.get("user_id");
                this.userName = (String) loginData.get("username");
                this.userRole = (String) loginData.get("role");
                this.apiToken = (String) loginData.get("token");
                this.loginTime = LocalDateTime.now();
                
                // Load restaurant info
                loadRestaurantInfo();
                
                // Save session
                saveSession();
                
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    public void logout() {
        try {
            // Call logout API
            if (isLoggedIn && apiToken != null) {
                logoutFromServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clear session
            clearSession();
        }
    }
    
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    
    public String getRestaurantId() {
        return restaurantId;
    }
    
    public String getRestaurantName() {
        return restaurantName;
    }
    
    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }
    
    public String getRestaurantAddress() {
        return restaurantAddress;
    }
    
    public void setRestaurantAddress(String restaurantAddress) {
        this.restaurantAddress = restaurantAddress;
    }
    
    public String getUsername() {
        return userName;
    }
    
    public String getRestaurantPhone() {
        return restaurantPhone;
    }
    
    public void setRestaurantPhone(String restaurantPhone) {
        this.restaurantPhone = restaurantPhone;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public String getApiToken() {
        return apiToken;
    }
    
    public boolean hasPermission(String permission) {
        // Check user permissions
        if (userRole == null) return false;
        
        switch (userRole.toLowerCase()) {
            case "admin":
                return true; // Admin has all permissions
            case "manager":
                return permission.equals("order_management") || 
                       permission.equals("menu_management") || 
                       permission.equals("report_viewing");
            case "cashier":
                return permission.equals("order_management") || 
                       permission.equals("menu_viewing");
            case "staff":
                return permission.equals("order_viewing");
            default:
                return false;
        }
    }
    
    public boolean isSessionValid() {
        if (!isLoggedIn || loginTime == null) {
            return false;
        }
        
        // Check if session is expired (8 hours)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = loginTime.plusHours(8);
        
        return now.isBefore(expiryTime);
    }
    
    public void refreshSession() {
        if (isLoggedIn && isSessionValid()) {
            try {
                // Refresh token and extend session
                Map<String, Object> refreshData = refreshToken();
                
                if (refreshData != null) {
                    this.apiToken = (String) refreshData.get("token");
                    saveSession();
                }
            } catch (Exception e) {
                e.printStackTrace();
                // If refresh fails, logout
                logout();
            }
        }
    }
    
    private Map<String, Object> authenticateUser(String username, String password) throws POSApiClient.ApiException {
        try {
            // Call backend API for authentication
            Map<String, Object> result = apiClient.login(username, password);

            if (result != null) {
                // Never log JWT access or refresh tokens.  A login response is
                // deliberately treated as sensitive production data.
                
                // Extract JWT access token and user data
                String token = (String) result.get("access"); // JWT uses 'access' field
                Map<String, Object> userObj = (Map<String, Object>) result.get("user");
                
                if (token != null && userObj != null) {
                    // Extract user data from nested user object
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("user_id", String.valueOf(userObj.get("id")));
                    userData.put("username", (String) userObj.getOrDefault("username", username));
                    userData.put("role", (String) userObj.getOrDefault("userType", "staff")); // JWT uses 'userType'
                    userData.put("token", token);
                    userData.put("restaurant_id", result.getOrDefault("restaurant_id", ""));
                    userData.put("email", (String) userObj.get("email"));
                    userData.put("first_name", (String) userObj.get("first_name"));
                    userData.put("last_name", (String) userObj.get("last_name"));
                    userData.put("phone", (String) userObj.get("phone"));

                    return userData;
                } else {
                    System.err.println("JWT token or user data missing. Token: " + token + ", User: " + userObj);
                }
            }
        } catch (POSApiClient.ApiException e) {
            // Error will be handled by the login dialog's error handler
            System.err.println("Authentication failed: " + e.getMessage());
            throw e; // Re-throw to be handled by the UI
        }

        return null;
    }
    
    private void loadRestaurantInfo() {
        try {
            // Call backend API to get restaurant info
            Map<String, Object> restaurantInfo = apiClient.getRestaurantInfo();

            if (restaurantInfo != null) {
                this.restaurantId = String.valueOf(restaurantInfo.getOrDefault("id", ""));
                this.restaurantName = (String) restaurantInfo.getOrDefault("name", "Unknown Restaurant");
                this.restaurantAddress = (String) restaurantInfo.getOrDefault("address", "");
                this.restaurantPhone = (String) restaurantInfo.getOrDefault("phone", "");
            } else {
                // Fallback defaults if API call fails
                this.restaurantId = "unknown";
                this.restaurantName = "Unknown Restaurant";
                this.restaurantAddress = "";
                this.restaurantPhone = "";
            }
        } catch (POSApiClient.ApiException e) {
            System.err.println("Failed to load restaurant info: " + e.getMessage());
            // Use fallback values
            this.restaurantId = "unknown";
            this.restaurantName = "Unknown Restaurant";
        }
    }
    
    private void logoutFromServer() {
        try {
            // Simulate API call to logout
            Thread.sleep(500);
            
            // In production, this would make actual API call
            System.out.println("Logging out from server...");
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private Map<String, Object> refreshToken() {
        try {
            // Simulate API call to refresh token
            Thread.sleep(500);
            
            // For demo purposes, generate new token
            Map<String, Object> refreshData = new HashMap<>();
            refreshData.put("token", "token_" + System.currentTimeMillis());
            
            return refreshData;
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void saveSession() {
        // Save session to local storage
        // In production, this would save to encrypted local storage
        try {
            Properties props = new Properties();
            props.setProperty("isLoggedIn", String.valueOf(isLoggedIn));
            props.setProperty("restaurantId", restaurantId != null ? restaurantId : "");
            props.setProperty("restaurantName", restaurantName != null ? restaurantName : "");
            props.setProperty("restaurantAddress", restaurantAddress != null ? restaurantAddress : "");
            props.setProperty("restaurantPhone", restaurantPhone != null ? restaurantPhone : "");
            props.setProperty("userId", userId != null ? userId : "");
            props.setProperty("userName", userName != null ? userName : "");
            props.setProperty("userRole", userRole != null ? userRole : "");
            props.setProperty("loginTime", loginTime != null ? loginTime.toString() : "");
            props.setProperty("apiToken", apiToken != null ? apiToken : "");
            
            // Save to file (in production, use secure storage)
            // props.store(new FileOutputStream("pos_session.properties"), "POS Session");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadSavedSession() {
        // Load session from local storage
        // In production, this would load from encrypted local storage
        try {
            Properties props = new Properties();
            // props.load(new FileInputStream("pos_session.properties"));
            
            // For demo purposes, don't load from file
            // In production, uncomment the line above and remove the manual setting
            
            // Manual setting for demo
            this.isLoggedIn = false;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void clearSession() {
        this.isLoggedIn = false;
        this.restaurantId = null;
        this.restaurantName = null;
        this.restaurantAddress = null;
        this.restaurantPhone = null;
        this.userId = null;
        this.userName = null;
        this.userRole = null;
        this.loginTime = null;
        this.apiToken = null;
        
        // Clear saved session
        try {
            // Remove session file
            // new File("pos_session.properties").delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // API helper methods
    public String getApiUrl(String endpoint) {
        return API_BASE_URL + endpoint;
    }
    
    public Map<String, String> getApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        if (apiToken != null) {
            headers.put("Authorization", "Bearer " + apiToken);
        }
        
        return headers;
    }
    
    // Session validation dialog with modern UI
    public boolean showLoginDialog(JFrame parent) {
        JDialog loginDialog = new JDialog(parent, "Mobile Meals POS - Login", true);
        Image brandLogo = loadBrandLogo();
        if (brandLogo != null) loginDialog.setIconImage(brandLogo);
        loginDialog.setSize(1080, 650);
        loginDialog.setLocationRelativeTo(parent);
        loginDialog.setResizable(false);

        // Main container with border layout
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(new Color(244, 247, 251));

        // Header panel with branding
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(44, 62, 80));
        headerPanel.setPreferredSize(new Dimension(275, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(44, 30, 44, 30));

        JLabel titleLabel = new JLabel("<html>MOBILE<br>MEALS</html>");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 27));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel subtitleLabel = new JLabel("<html>COUNTER TERMINAL<br>Fast, secure checkout</html>");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(236, 240, 241));
        subtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 12, 12));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        if (brandLogo != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(brandLogo.getScaledInstance(86, 86, Image.SCALE_SMOOTH)));
            logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
            headerPanel.add(logoLabel, BorderLayout.NORTH);
        }

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(60, 54, 50, 54));
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel formTitle = new JLabel("Welcome back");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 30));
        formTitle.setForeground(new Color(22, 29, 39));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 6, 0); formPanel.add(formTitle, gbc);
        JLabel formHint = new JLabel("Sign in to start a new counter session");
        formHint.setFont(new Font("Segoe UI", Font.PLAIN, 14)); formHint.setForeground(new Color(110, 121, 136));
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 34, 0); formPanel.add(formHint, gbc);

        // Username label
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 5, 0);
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userLabel.setForeground(new Color(52, 73, 94));
        formPanel.add(userLabel, gbc);

        // Username field
        gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 15, 0);
        JTextField usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setForeground(new Color(44, 62, 80));
        usernameField.setBackground(new Color(252, 252, 253));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 232), 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        formPanel.add(usernameField, gbc);

        // Password label
        gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 5, 0);
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passLabel.setForeground(new Color(52, 73, 94));
        formPanel.add(passLabel, gbc);

        // Password field
        gbc.gridy = 5; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 25, 0);
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setForeground(new Color(44, 62, 80));
        passwordField.setBackground(new Color(252, 252, 253));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 232), 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        passwordField.setEchoChar('●');
        formPanel.add(passwordField, gbc);

        // Login button
        JCheckBox keepSignedIn = new JCheckBox("Keep me signed in on this terminal");
        keepSignedIn.setBackground(Color.WHITE); keepSignedIn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 20, 0); formPanel.add(keepSignedIn, gbc);

        gbc.gridy = 7; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        JButton loginButton = new JButton("SIGN IN");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(255, 107, 53));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        formPanel.add(loginButton, gbc);

        // Cancel button
        gbc.gridy = 8; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 0, 0, 0);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.setForeground(new Color(127, 140, 141));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        formPanel.add(cancelButton, gbc);

        // Footer with API URL info
        gbc.gridy = 9; gbc.insets = new Insets(24, 0, 0, 0);
        JLabel footerLabel = new JLabel("Connected to: " + API_BASE_URL);
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footerLabel.setForeground(new Color(149, 165, 166));
        formPanel.add(footerLabel, gbc);

        // Assemble dialog
        container.add(headerPanel, BorderLayout.WEST);
        container.add(formPanel, BorderLayout.CENTER);
        container.add(new POSLoginIllustration(), BorderLayout.EAST);
        loginDialog.add(container);

        // Result
        final boolean[] loginResult = {false};

        // Button actions
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog,
                    "Please enter both username and password",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Show loading cursor
            loginDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            loginButton.setEnabled(false);
            loginButton.setText("Signing in...");

            // Perform login in background to avoid freezing UI
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return login(username, password);
                }

                @Override
                protected void done() {
                    loginDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    loginButton.setEnabled(true);
                    loginButton.setText("SIGN IN");

                    try {
                        if (get()) {
                            loginResult[0] = true;
                            loginDialog.dispose();
                        } else {
                            JOptionPane.showMessageDialog(loginDialog,
                                "Invalid username or password. Please try again.",
                                "Login Failed", JOptionPane.ERROR_MESSAGE);
                            passwordField.setText("");
                            passwordField.requestFocus();
                        }
                    } catch (Exception ex) {
                        String errorMsg = ex.getMessage();
                        String displayMsg;

                        // Parse common error types for user-friendly messages
                        if (errorMsg != null && errorMsg.contains("401")) {
                            displayMsg = "The username or password you entered is incorrect.\nPlease check your credentials and try again.";
                        } else if (errorMsg != null && errorMsg.contains("404")) {
                            displayMsg = "Unable to connect to the server.\nPlease check your internet connection and try again.";
                        } else if (errorMsg != null && errorMsg.contains("500")) {
                            displayMsg = "The server is experiencing issues.\nPlease try again later or contact support.";
                        } else if (errorMsg != null && errorMsg.contains("timeout") || errorMsg != null && errorMsg.contains("Timeout")) {
                            displayMsg = "Connection timed out.\nPlease check your internet connection and try again.";
                        } else {
                            displayMsg = "Unable to connect to the server.\nPlease check your connection and try again.";
                        }

                        JOptionPane.showMessageDialog(loginDialog,
                            displayMsg,
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

        // Enter key triggers login
        passwordField.addActionListener(e -> loginButton.doClick());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        cancelButton.addActionListener(e -> {
            loginDialog.dispose();
        });

        loginDialog.setVisible(true);

        return loginResult[0];
    }

    /** Decorative, code-drawn counter illustration for the login screen. */
    private static final class POSLoginIllustration extends JPanel {
        POSLoginIllustration() { setPreferredSize(new Dimension(360, 0)); setOpaque(true); setBackground(new Color(238, 246, 255)); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(255, 222, 195)); g.fillOval(-100, h / 3, w + 180, h);
            g.setColor(new Color(255, 185, 118)); g.fillOval(w / 4, 80, w, h - 100);
            // Counter and terminal
            g.setColor(new Color(207, 120, 67)); g.fillRoundRect(18, h - 145, w - 36, 24, 12, 12);
            g.setColor(new Color(249, 196, 151)); g.fillRect(30, h - 121, w - 60, 121);
            g.setColor(new Color(63, 70, 82)); g.fillRoundRect(95, h - 280, 150, 100, 16, 16);
            g.setColor(new Color(30, 36, 45)); g.fillRoundRect(108, h - 268, 124, 70, 10, 10);
            g.setColor(Color.WHITE); g.fillRoundRect(116, h - 258, 108, 50, 6, 6);
            g.setColor(new Color(79, 84, 92)); g.fillRect(154, h - 180, 30, 35);
            // Cashier figure
            g.setColor(new Color(255, 193, 155)); g.fillOval(232, 138, 76, 92);
            g.setColor(new Color(31, 37, 48)); g.fillRoundRect(220, 110, 96, 58, 28, 28);
            g.setColor(new Color(255, 107, 53)); g.fillRoundRect(205, 215, 128, 166, 54, 54);
            g.setColor(new Color(255, 193, 155)); g.fillOval(188, 275, 48, 27);
            g.setColor(new Color(255, 193, 155)); g.fillOval(286, 284, 49, 27);
            g.setColor(Color.WHITE); g.fillOval(250, 213, 44, 30);
            g.dispose();
        }
    }
    private static Image loadBrandLogo() { try (java.io.InputStream stream = RestaurantSession.class.getResourceAsStream("/logo.png")) { return stream == null ? null : ImageIO.read(stream); } catch (Exception ignored) { return null; } }
}
