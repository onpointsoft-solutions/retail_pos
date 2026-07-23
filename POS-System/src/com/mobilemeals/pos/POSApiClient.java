package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;
import com.google.gson.*;
import com.google.gson.reflect.*;

/**
 * API Client for POS System
 * Handles communication with Mobile Meals backend
 */
public class POSApiClient {
    
    private static final String BASE_URL = "https://www.mobilemealscenter.co.ke/api/";
    private static final int TIMEOUT = 30000; // 30 seconds timeout
    
    private String authToken;
    private RestaurantSession session;
    
    public POSApiClient(RestaurantSession session) {
        this.session = session;
        this.authToken = session.getApiToken();
    }
    
    // Authentication Methods
    public Map<String, Object> login(String username, String password) throws ApiException {
        try {
            Map<String, String> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("password", password);
            
            String response = postRequest("auth/login/", loginData);
            Map<String, Object> result = parseJsonResponse(response);
            
            if (result.containsKey("token")) {
                this.authToken = (String) result.get("token");
                session.setApiToken(authToken);
            }
            
            return result;
            
        } catch (Exception e) {
            throw new ApiException("Login failed: " + e.getMessage(), e);
        }
    }
    
    public boolean logout() throws ApiException {
        try {
            if (authToken != null) {
                postRequest("auth/logout/", new HashMap<>());
            }
            return true;
        } catch (Exception e) {
            throw new ApiException("Logout failed: " + e.getMessage(), e);
        } finally {
            this.authToken = null;
            session.setApiToken(null);
        }
    }
    
    // Restaurant Methods
    public Map<String, Object> getRestaurantInfo() throws ApiException {
        try {
            String response = getRequest("restaurants/me/");
            return parseJsonResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get restaurant info: " + e.getMessage(), e);
        }
    }
    
    public boolean updateRestaurantInfo(Map<String, Object> restaurantData) throws ApiException {
        try {
            putRequest("restaurants/me/", restaurantData);
            return true;
        } catch (Exception e) {
            throw new ApiException("Failed to update restaurant info: " + e.getMessage(), e);
        }
    }
    
    // Menu Methods
    public List<Map<String, Object>> getMenuItems() throws ApiException {
        try {
            String response = getRequest("restaurants/menu/");
            return parseJsonListResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get menu items: " + e.getMessage(), e);
        }
    }
    
    public boolean addMenuItem(Map<String, Object> itemData) throws ApiException {
        try {
            postRequest("restaurants/menu/", itemData);
            return true;
        } catch (Exception e) {
            throw new ApiException("Failed to add menu item: " + e.getMessage(), e);
        }
    }
    
    public boolean updateMenuItem(String itemId, Map<String, Object> itemData) throws ApiException {
        try {
            putRequest("restaurants/menu/" + itemId + "/", itemData);
            return true;
        } catch (Exception e) {
            throw new ApiException("Failed to update menu item: " + e.getMessage(), e);
        }
    }
    
    public boolean deleteMenuItem(String itemId) throws ApiException {
        try {
            deleteRequest("restaurants/menu/" + itemId + "/");
            return true;
        } catch (Exception e) {
            throw new ApiException("Failed to delete menu item: " + e.getMessage(), e);
        }
    }
    
    // Order Methods
    public Map<String, Object> createOrder(Map<String, Object> orderData) throws ApiException {
        try {
            String response = postRequest("orders/", orderData);
            return parseJsonResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to create order: " + e.getMessage(), e);
        }
    }
    
    public List<Map<String, Object>> getOrders() throws ApiException {
        try {
            String response = getRequest("orders/");
            return parseJsonListResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get orders: " + e.getMessage(), e);
        }
    }
    
    public List<Map<String, Object>> getOrdersByStatus(String status) throws ApiException {
        try {
            String response = getRequest("orders/?status=" + URLEncoder.encode(status, "UTF-8"));
            return parseJsonListResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get orders by status: " + e.getMessage(), e);
        }
    }
    
    public boolean updateOrderStatus(String orderId, String status) throws ApiException {
        try {
            Map<String, String> statusData = new HashMap<>();
            statusData.put("status", status);
            
            patchRequest("orders/" + orderId + "/", statusData);
            return true;
        } catch (Exception e) {
            throw new ApiException("Failed to update order status: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getOrder(String orderId) throws ApiException {
        try {
            String response = getRequest("orders/" + orderId + "/");
            return parseJsonResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get order: " + e.getMessage(), e);
        }
    }
    
    // Report Methods
    public Map<String, Object> getSalesReport(String startDate, String endDate) throws ApiException {
        try {
            String url = String.format("reports/sales/?start_date=%s&end_date=%s", 
                URLEncoder.encode(startDate, "UTF-8"), 
                URLEncoder.encode(endDate, "UTF-8"));
            String response = getRequest(url);
            return parseJsonResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get sales report: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getOrderReport(String startDate, String endDate) throws ApiException {
        try {
            String url = String.format("reports/orders/?start_date=%s&end_date=%s", 
                URLEncoder.encode(startDate, "UTF-8"), 
                URLEncoder.encode(endDate, "UTF-8"));
            String response = getRequest(url);
            return parseJsonResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get order report: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getRevenueReport(String startDate, String endDate) throws ApiException {
        try {
            String url = String.format("reports/revenue/?start_date=%s&end_date=%s", 
                URLEncoder.encode(startDate, "UTF-8"), 
                URLEncoder.encode(endDate, "UTF-8"));
            String response = getRequest(url);
            return parseJsonResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get revenue report: " + e.getMessage(), e);
        }
    }
    
    // HTTP Request Methods
    private String getRequest(String endpoint) throws ApiException {
        return makeRequest("GET", endpoint, null);
    }
    
    private String postRequest(String endpoint, Map<String, Object> data) throws ApiException {
        return makeRequest("POST", endpoint, data);
    }
    
    private String putRequest(String endpoint, Map<String, Object> data) throws ApiException {
        return makeRequest("PUT", endpoint, data);
    }
    
    private String patchRequest(String endpoint, Map<String, Object> data) throws ApiException {
        return makeRequest("PATCH", endpoint, data);
    }
    
    private String deleteRequest(String endpoint) throws ApiException {
        return makeRequest("DELETE", endpoint, null);
    }
    
    private String makeRequest(String method, String endpoint, Map<String, Object> data) throws ApiException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            
            // Set request method
            connection.setRequestMethod(method);
            
            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            
            if (authToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            
            // Set timeout
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            // Send request body for POST, PUT, PATCH
            if (data != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                connection.setDoOutput(true);
                
                String jsonBody = toJsonString(data);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonBody.getBytes("UTF-8"));
                }
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            InputStream inputStream;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
            
            String response = readInputStream(inputStream);
            
            if (responseCode >= 400) {
                throw new ApiException("HTTP " + responseCode + ": " + response);
            }
            
            return response;
            
        } catch (Exception e) {
            throw new ApiException("Request failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    // Utility Methods
    private String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        
        return response.toString();
    }
    
    private String toJsonString(Map<String, Object> data) {
        Gson gson = new Gson();
        return gson.toJson(data);
    }
    
    private Map<String, Object> parseJsonResponse(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(json, type);
    }
    
    private List<Map<String, Object>> parseJsonListResponse(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        return gson.fromJson(json, type);
    }
    
    // Test Connection
    public boolean testConnection() {
        try {
            String response = getRequest("health/");
            return response.contains("OK");
        } catch (Exception e) {
            return false;
        }
    }
    
    // Get Server Info
    public Map<String, Object> getServerInfo() throws ApiException {
        try {
            String response = getRequest("info/");
            return parseJsonResponse(response);
        } catch (Exception e) {
            throw new ApiException("Failed to get server info: " + e.getMessage(), e);
        }
    }
    
    // Custom Exception Class
    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
        
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    // Response Wrapper Class
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private Map<String, Object> errors;
        
        public ApiResponse(boolean success, String message, T data, Map<String, Object> errors) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.errors = errors;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        
        public Map<String, Object> getErrors() { return errors; }
        public void setErrors(Map<String, Object> errors) { this.errors = errors; }
    }
    
    // Batch Operations
    public List<Map<String, Object>> createMultipleOrders(List<Map<String, Object>> orders) throws ApiException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map<String, Object> order : orders) {
            try {
                Map<String, Object> result = createOrder(order);
                results.add(result);
            } catch (ApiException e) {
                // Log error but continue with other orders
                System.err.println("Failed to create order: " + e.getMessage());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", e.getMessage());
                results.add(errorResult);
            }
        }
        
        return results;
    }
    
    // Sync Methods
    public Map<String, Object> syncData() throws ApiException {
        Map<String, Object> syncData = new HashMap<>();
        
        try {
            // Sync menu items
            syncData.put("menu_items", getMenuItems());
            
            // Sync orders
            syncData.put("orders", getOrders());
            
            // Sync restaurant info
            syncData.put("restaurant", getRestaurantInfo());
            
            return syncData;
            
        } catch (Exception e) {
            throw new ApiException("Sync failed: " + e.getMessage(), e);
        }
    }
}
