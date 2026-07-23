package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    
    // API Configuration
    private static final String API_BASE_URL = "https://www.mobilemealscenter.co.ke/api/";
    private static final String LOGIN_ENDPOINT = "auth/login/";
    private static final String RESTAURANT_INFO_ENDPOINT = "restaurants/me/";
    private static final String LOGOUT_ENDPOINT = "auth/logout/";
    
    public RestaurantSession() {
        this.isLoggedIn = false;
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
    
    private Map<String, Object> authenticateUser(String username, String password) {
        try {
            // Simulate API call
            Thread.sleep(1000);
            
            // For demo purposes, accept any credentials
            // In production, this would make actual API call
            if (username != null && password != null && password.length() >= 4) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("user_id", "user_" + System.currentTimeMillis());
                userData.put("username", username);
                userData.put("role", "manager"); // Default role for demo
                userData.put("token", "token_" + System.currentTimeMillis());
                userData.put("restaurant_id", "restaurant_001");
                
                return userData;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void loadRestaurantInfo() {
        try {
            // Simulate API call to get restaurant info
            Thread.sleep(500);
            
            // For demo purposes, use default restaurant info
            this.restaurantId = "restaurant_001";
            this.restaurantName = "Demo Restaurant";
            this.restaurantAddress = "123 Main Street, Nairobi, Kenya";
            this.restaurantPhone = "+254 712 345 678";
            
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            props.setProperty("restaurantId", restaurantId);
            props.setProperty("restaurantName", restaurantName);
            props.setProperty("restaurantAddress", restaurantAddress);
            props.setProperty("restaurantPhone", restaurantPhone);
            props.setProperty("userId", userId);
            props.setProperty("userName", userName);
            props.setProperty("userRole", userRole);
            props.setProperty("loginTime", loginTime != null ? loginTime.toString() : "");
            props.setProperty("apiToken", apiToken);
            
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
    
    // Session validation dialog
    public boolean showLoginDialog(JFrame parent) {
        JDialog loginDialog = new JDialog(parent, "Login to Mobile Meals POS", true);
        loginDialog.setSize(400, 300);
        loginDialog.setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.insets = new Insets(0, 0, 20, 0);
        JLabel titleLabel = new JLabel("Mobile Meals POS Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, gbc);
        
        // Username
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JTextField usernameField = new JTextField(20);
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        panel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; 
        gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(20, 0, 0, 0);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");
        
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);
        
        // Result
        final boolean[] loginResult = {false};
        
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, 
                    "Please enter username and password", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (login(username, password)) {
                loginResult[0] = true;
                loginDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(loginDialog, 
                    "Invalid username or password", 
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> {
            loginDialog.dispose();
        });
        
        loginDialog.add(panel);
        loginDialog.setVisible(true);
        
        return loginResult[0];
    }
}
