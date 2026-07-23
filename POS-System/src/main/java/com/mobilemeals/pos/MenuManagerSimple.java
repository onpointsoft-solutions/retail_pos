package com.mobilemeals.pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple Menu Manager for POS
 */
public class MenuManagerSimple {
    
    public static class MenuItem {
        public String code;
        public String name;
        public String category;
        public double price;
        public boolean available;
        
        public MenuItem() {}
        
        public MenuItem(String code, String name, String category, double price, boolean available) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.price = price;
            this.available = available;
        }
        
        public boolean isAvailable() {
            return available;
        }
    }
    
    private Map<String, List<MenuItem>> menuItems;
    
    public MenuManagerSimple() {
        this.menuItems = new HashMap<>();
        initializeSampleMenu();
    }
    
    private void initializeSampleMenu() {
        // Add sample menu items
        List<MenuItem> mainCourses = new ArrayList<>();
        mainCourses.add(new MenuItem("MC001", "Ugali", "Main Courses", 800.0, true));
        mainCourses.add(new MenuItem("MC002", "Nyama Choma", "Main Courses", 600.0, true));
        mainCourses.add(new MenuItem("MC003", "Sukuma Wiki", "Main Courses", 750.0, true));
        
        List<MenuItem> appetizers = new ArrayList<>();
        appetizers.add(new MenuItem("AP001", "Samosa", "Appetizers", 150.0, true));
        appetizers.add(new MenuItem("AP002", "Kachumbari", "Appetizers", 200.0, true));
        
        List<MenuItem> beverages = new ArrayList<>();
        beverages.add(new MenuItem("BV001", "Soda", "Beverages", 50.0, true));
        beverages.add(new MenuItem("BV002", "Juice", "Beverages", 80.0, true));
        
        menuItems.put("Main Courses", mainCourses);
        menuItems.put("Appetizers", appetizers);
        menuItems.put("Beverages", beverages);
    }
    
    public Map<String, List<MenuItem>> getMenuCategories() {
        return new HashMap<>(menuItems);
    }
    
    public MenuItem getMenuItem(String itemCode) {
        for (List<MenuItem> items : menuItems.values()) {
            for (MenuItem item : items) {
                if (item.code.equals(itemCode)) {
                    return item;
                }
            }
        }
        return null;
    }
    
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> allItems = new ArrayList<>();
        for (List<MenuItem> items : menuItems.values()) {
            allItems.addAll(items);
        }
        return allItems;
    }
}
