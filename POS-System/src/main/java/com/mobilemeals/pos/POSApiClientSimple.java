package com.mobilemeals.pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified API Client for POS
 * Handles basic API communication
 */
public class POSApiClientSimple {
    
    private String apiToken;
    private String baseUrl;
    
    public POSApiClientSimple() {
        this.baseUrl = "https://www.mobilemealscenter.co.ke/api/";
        this.apiToken = "demo-token";
    }
    
    public void setApiToken(String token) {
        this.apiToken = token;
    }
    
    public String getApiToken() {
        return apiToken;
    }
    
    public boolean login(String username, String password) {
        // Simulate login
        if ("admin".equals(username) && "password".equals(password)) {
            this.apiToken = "valid-token";
            return true;
        }
        return false;
    }
    
    public List<String> getMenuCategories() {
        // Return sample categories
        List<String> categories = new ArrayList<>();
        categories.add("Main Courses");
        categories.add("Appetizers");
        categories.add("Beverages");
        categories.add("Desserts");
        return categories;
    }
    
    public Map<String, Object> generateSalesReport(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        report.put("totalRevenue", 15000.0);
        report.put("totalOrders", 25);
        report.put("startDate", startDate.toString());
        report.put("endDate", endDate.toString());
        return report;
    }
    
    public boolean submitOrder(String customerName, List<OrderEntryPanelSimple.OrderItem> items) {
        // Simulate order submission
        System.out.println("Order submitted for: " + customerName);
        return true;
    }
}
