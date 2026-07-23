package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.lang.reflect.Type;
import java.net.*;
import java.io.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

/**
 * API Client for POS System
 * Handles communication with Mobile Meals backend
 */
public class POSApiClient {
    
    private static final String BASE_URL = System.getProperty(
            "pos.api.url", "https://www.mobilemealscenter.co.ke/api/");
    private static final int TIMEOUT = 30000; // 30 seconds timeout
    
    private String authToken;
    private String refreshToken;
    private RestaurantSession session;
    
    public POSApiClient(RestaurantSession session) {
        this.session = session;
        this.authToken = session.getApiToken();
    }
    
    // Authentication Methods
    public Map<String, Object> login(String username, String password) throws ApiException {
        try {
            Map<String, Object> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("password", password);

            // Use JWT endpoint for proper token authentication
            String response = postRequest("auth/token/", loginData);
            Map<String, Object> result = parseJsonResponse(response);
            
            // JWT tokens are returned as 'access' and 'refresh'
            if (result.containsKey("access")) {
                this.authToken = (String) result.get("access");
                this.refreshToken = (String) result.get("refresh");
            }
            
            return result;
            
        } catch (Exception e) {
            throw new ApiException("Login failed: " + e.getMessage(), e);
        }
    }

    /** Native POS API: one bootstrap call replaces location-based restaurant guessing. */
    public Map<String, Object> getPosBootstrap() throws ApiException {
        return parseJsonResponse(getRequest("pos/bootstrap/"));
    }

    public Map<String, Object> getPosStaff() throws ApiException {
        return parseJsonResponse(getRequest("pos/staff/"));
    }

    public Map<String, Object> createPosStaff(Map<String, Object> staff) throws ApiException {
        return parseJsonResponse(postRequest("pos/staff/add/", staff));
    }

    /** Creates an order with server-side prices; clients must never submit prices. */
    public Map<String, Object> createPosOrder(Map<String, Object> order) throws ApiException {
        return parseJsonResponse(postRequest("pos/orders/", order));
    }

    public Map<String, Object> invoicePosOrder(String orderId) throws ApiException {
        return parseJsonResponse(postRequest("pos/orders/" + orderId + "/invoice/", new HashMap<>()));
    }

    public Map<String, Object> payPosOrder(String orderId, String paymentMethod) throws ApiException {
        Map<String, Object> body = new HashMap<>();
        body.put("payment_method", paymentMethod);
        return parseJsonResponse(postRequest("pos/orders/" + orderId + "/pay/", body));
    }

    public Map<String, Object> closePosSession(String closingBalance) throws ApiException {
        Map<String, Object> body = new HashMap<>();
        body.put("closing_balance", closingBalance);
        return parseJsonResponse(postRequest("pos/sessions/close/", body));
    }

    public Map<String, Object> refreshAccessToken() throws ApiException {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ApiException("No refresh token is available.");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("refresh", refreshToken);
        Map<String, Object> result = parseJsonResponse(postRequest("auth/token/refresh/", body));
        this.authToken = (String) result.get("access");
        return result;
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
        }
    }
    
    // Restaurant Methods
    public Map<String, Object> getRestaurantInfo() throws ApiException {
        try {
            Map<String, Object> bootstrap = getPosBootstrap();
            Object restaurant = bootstrap.get("restaurant");
            if (restaurant instanceof Map) return (Map<String, Object>) restaurant;
            throw new ApiException("POS bootstrap did not return a restaurant.");
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
            Object meals = getPosBootstrap().get("meals");
            if (meals instanceof List) return (List<Map<String, Object>>) meals;
            throw new ApiException("POS bootstrap did not return menu items.");
        } catch (Exception e) {
            throw new ApiException("Failed to get menu items: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getRestaurantWithMenu() throws ApiException {
        try {
            // Get restaurant info first to get restaurant ID
            Map<String, Object> restaurantInfo = getRestaurantInfo();
            String restaurantId = String.valueOf(restaurantInfo.getOrDefault("id", "1"));
            
            // Get restaurant details with menu
            String response = getRequest("restaurants/" + restaurantId + "/meals/?lat=-1.2921&lng=36.8219");
            Map<String, Object> result = parseJsonResponse(response);
            
            // Combine restaurant info with menu
            Map<String, Object> combined = new java.util.HashMap<>(restaurantInfo);
            if (result.containsKey("meals")) {
                combined.put("meals", result.get("meals"));
            } else if (result instanceof List) {
                combined.put("meals", result);
            }
            
            return combined;
        } catch (Exception e) {
            throw new ApiException("Failed to get restaurant with menu: " + e.getMessage(), e);
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
            Map<String, Object> statusData = new HashMap<>();
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
    
    // Dashboard-specific methods
    public List<Map<String, Object>> getTodayOrders() throws ApiException {
        try {
            // Use customer orders endpoint
            String response = getRequest("orders/");
            Map<String, Object> result = parseJsonResponse(response);
            
            if (result.containsKey("orders")) {
                return (List<Map<String, Object>>) result.get("orders");
            }
            
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            throw new ApiException("Failed to get today's orders: " + e.getMessage(), e);
        }
    }
    
    public List<Map<String, Object>> getRecentOrders(int limit) throws ApiException {
        try {
            // Use customer orders endpoint
            String response = getRequest("orders/");
            Map<String, Object> result = parseJsonResponse(response);
            
            if (result.containsKey("orders")) {
                List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
                // Limit results if needed
                if (orders.size() > limit) {
                    return orders.subList(0, limit);
                }
                return orders;
            }
            
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            throw new ApiException("Failed to get recent orders: " + e.getMessage(), e);
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
                // Use standard Bearer token for JWT authentication
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
