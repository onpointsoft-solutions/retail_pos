package com.mobilemeals.pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple Restaurant Session for POS
 */
public class RestaurantSessionSimple {
    
    private String restaurantName;
    private String username;
    private boolean loggedIn;
    private String apiToken;
    
    public RestaurantSessionSimple() {
        this.restaurantName = "Demo Restaurant";
        this.username = "Staff";
        this.loggedIn = false;
        this.apiToken = "demo-token";
    }
    
    public String getRestaurantName() {
        return restaurantName;
    }
    
    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public boolean isLoggedIn() {
        return loggedIn;
    }
    
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
    
    public String getApiToken() {
        return apiToken;
    }
    
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
    
    public void login(String username, String password) {
        this.username = username;
        this.loggedIn = true;
    }
    
    public void logout() {
        this.loggedIn = false;
        this.username = "Staff";
    }
}
