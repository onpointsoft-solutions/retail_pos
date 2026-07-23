package com.mobilemeals.pos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple POS Order data class
 */
public class POSOrder {
    private String id;
    private String customerName;
    private String status;
    private double totalAmount;
    private LocalDateTime orderTime;
    private List<OrderItem> items;
    
    public POSOrder() {
        this.items = new ArrayList<>();
    }
    
    public POSOrder(String id, String customerName, String status, double totalAmount) {
        this.id = id;
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = new ArrayList<>();
        this.orderTime = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    // Aliases for ReportManager compatibility
    public LocalDateTime getCreatedTime() { return orderTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.orderTime = createdTime; }

    public String getOrderNumber() { return id; }
    public void setOrderNumber(String orderNumber) { this.id = orderNumber; }

    @Override
    public String toString() {
        return "POSOrder{id='" + id + "', customer='" + customerName + "', status='" + status + "'}";
    }
    
    /**
     * Simple OrderItem inner class
     */
    public static class OrderItem {
        private String name;
        private int quantity;
        private double price;
        
        public OrderItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        
        public double getTotal() {
            return quantity * price;
        }
        
        @Override
        public String toString() {
            return name + " x" + quantity + " = KES " + getTotal();
        }
    }
}
