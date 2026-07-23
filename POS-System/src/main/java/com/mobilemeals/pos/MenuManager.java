package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Menu Management System for POS
 * Handles menu items, categories, and pricing
 */
public class MenuManager {
    
    private Map<String, List<MenuItem>> menuCategories;
    private List<MenuItem> allMenuItems;
    private MenuListener menuListener;
    
    public MenuManager() {
        menuCategories = new HashMap<>();
        allMenuItems = new ArrayList<>();
        loadDefaultMenu();
    }
    
    public void setMenuListener(MenuListener listener) {
        this.menuListener = listener;
    }
    
    public void addCategory(String categoryName) {
        if (!menuCategories.containsKey(categoryName)) {
            menuCategories.put(categoryName, new ArrayList<>());
            
            if (menuListener != null) {
                menuListener.onCategoryAdded(categoryName);
            }
        }
    }
    
    public void addMenuItem(MenuItem item) {
        allMenuItems.add(item);
        
        // Add to category
        String category = item.getCategory();
        if (!menuCategories.containsKey(category)) {
            menuCategories.put(category, new ArrayList<>());
        }
        menuCategories.get(category).add(item);
        
        if (menuListener != null) {
            menuListener.onMenuItemAdded(item);
        }
    }
    
    public void removeMenuItem(String itemId) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            allMenuItems.remove(item);
            menuCategories.get(item.getCategory()).remove(item);
            
            if (menuListener != null) {
                menuListener.onMenuItemRemoved(item);
            }
        }
    }
    
    public void updateMenuItem(MenuItem item) {
        MenuItem existingItem = findMenuItemById(item.getId());
        if (existingItem != null) {
            String oldCategory = existingItem.getCategory();
            String newCategory = item.getCategory();
            
            // Update item
            existingItem.setName(item.getName());
            existingItem.setPrice(item.getPrice());
            existingItem.setCategory(newCategory);
            existingItem.setDescription(item.getDescription());
            existingItem.setAvailable(item.isAvailable());
            
            // Handle category change
            if (!oldCategory.equals(newCategory)) {
                menuCategories.get(oldCategory).remove(existingItem);
                
                if (!menuCategories.containsKey(newCategory)) {
                    menuCategories.put(newCategory, new ArrayList<>());
                }
                menuCategories.get(newCategory).add(existingItem);
            }
            
            if (menuListener != null) {
                menuListener.onMenuItemUpdated(existingItem);
            }
        }
    }
    
    public MenuItem findMenuItemById(String itemId) {
        for (MenuItem item : allMenuItems) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }
    
    public List<MenuItem> findMenuItemsByName(String name) {
        List<MenuItem> results = new ArrayList<>();
        for (MenuItem item : allMenuItems) {
            if (item.getName().toLowerCase().contains(name.toLowerCase())) {
                results.add(item);
            }
        }
        return results;
    }
    
    public List<MenuItem> findMenuItemsByCategory(String category) {
        return menuCategories.getOrDefault(category, new ArrayList<>());
    }
    
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> allItems = new ArrayList<>();
        for (List<MenuItem> items : menuCategories.values()) {
            allItems.addAll(items);
        }
        return allItems;
    }
    
    public List<MenuItem> getAvailableMenuItems() {
        List<MenuItem> availableItems = new ArrayList<>();
        for (MenuItem item : allMenuItems) {
            if (item.isAvailable()) {
                availableItems.add(item);
            }
        }
        return availableItems;
    }

    public Set<String> getCategories() {
        return menuCategories.keySet();
    }
    
    public Map<String, List<MenuItem>> getMenuCategories() {
        return new HashMap<>(menuCategories);
    }
    
    public void toggleItemAvailability(String itemId) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            item.setAvailable(!item.isAvailable());
            
            if (menuListener != null) {
                menuListener.onMenuItemAvailabilityToggled(item);
            }
        }
    }
    
    public void updateItemPrice(String itemId, double newPrice) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            item.setPrice(newPrice);
            
            if (menuListener != null) {
                menuListener.onMenuItemPriceUpdated(item);
            }
        }
    }
    
    private void loadDefaultMenu() {
        // Load default menu items for demo
        addCategory("Main Courses");
        addCategory("Appetizers");
        addCategory("Beverages");
        addCategory("Desserts");
        
        // Main Courses
        addMenuItem(new MenuItem("MC001", "Chicken Burger", "Main Courses", 450.00, 
            "Juicy chicken patty with lettuce, tomato, and special sauce", true));
        addMenuItem(new MenuItem("MC002", "Beef Burger", "Main Courses", 550.00, 
            "Premium beef patty with cheese, lettuce, and tomato", true));
        addMenuItem(new MenuItem("MC003", "Pizza Margherita", "Main Courses", 650.00, 
            "Classic Italian pizza with mozzarella and tomato sauce", true));
        addMenuItem(new MenuItem("MC004", "Grilled Chicken", "Main Courses", 750.00, 
            "Grilled chicken breast with vegetables", true));
        
        // Appetizers
        addMenuItem(new MenuItem("AP001", "French Fries", "Appetizers", 150.00, 
            "Crispy golden french fries", true));
        addMenuItem(new MenuItem("AP002", "Onion Rings", "Appetizers", 180.00, 
            "Crispy battered onion rings", true));
        addMenuItem(new MenuItem("AP003", "Chicken Wings", "Appetizers", 350.00, 
            "6 pieces of spicy chicken wings", true));
        addMenuItem(new MenuItem("AP004", "Salad", "Appetizers", 250.00, 
            "Fresh garden salad with dressing", true));
        
        // Beverages
        addMenuItem(new MenuItem("BV001", "Coca Cola", "Beverages", 80.00, 
            "Refreshing cola drink", true));
        addMenuItem(new MenuItem("BV002", "Orange Juice", "Beverages", 120.00, 
            "Fresh orange juice", true));
        addMenuItem(new MenuItem("BV003", "Lemonade", "Beverages", 100.00, 
            "Homemade lemonade", true));
        addMenuItem(new MenuItem("BV004", "Water", "Beverages", 50.00, 
            "Mineral water", true));
        
        // Desserts
        addMenuItem(new MenuItem("DS001", "Ice Cream", "Desserts", 150.00, 
            "Vanilla ice cream with chocolate sauce", true));
        addMenuItem(new MenuItem("DS002", "Cake", "Desserts", 200.00, 
            "Chocolate cake slice", true));
        addMenuItem(new MenuItem("DS003", "Fruit Salad", "Desserts", 180.00, 
            "Fresh mixed fruits", true));
    }
    
    // Menu Listener Interface
    public interface MenuListener {
        void onCategoryAdded(String categoryName);
        void onMenuItemAdded(MenuItem item);
        void onMenuItemRemoved(MenuItem item);
        void onMenuItemUpdated(MenuItem item);
        void onMenuItemAvailabilityToggled(MenuItem item);
        void onMenuItemPriceUpdated(MenuItem item);
    }
    
    // MenuItem Data Class
    public static class MenuItem {
        private String id;
        private String name;
        private String category;
        private double price;
        private String description;
        private boolean available;
        private String imageUrl;
        
        public MenuItem() {
            this.available = true;
        }
        
        public MenuItem(String id, String name, String category, double price, String description, boolean available) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.description = description;
            this.available = available;
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isAvailable() {
            return available;
        }
        
        public String getFormattedPrice() {
            return String.format("KES %.2f", price);
        }
        
        public void setAvailable(boolean available) { this.available = available; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        // Helper methods
        
        public String getAvailabilityStatus() {
            return available ? "Available" : "Unavailable";
        }
        
        @Override
        public String toString() {
            return name + " - " + getFormattedPrice();
        }
    }
}
