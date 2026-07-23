package com.mobilemeals.pos;

/**
 * Simple POS Order data class
 */
public class POSOrderSimple {
    private String id;
    private String customerName;
    private String status;
    private double totalAmount;
    private java.time.LocalDateTime orderTime;
    
    public POSOrderSimple() {
    }

    public POSOrderSimple(String id, String customerName, String status, double totalAmount) {
        this.id = id;
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderTime = java.time.LocalDateTime.now();
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
    
    public java.time.LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(java.time.LocalDateTime orderTime) { this.orderTime = orderTime; }
    
    @Override
    public String toString() {
        return "POSOrder{id='" + id + "', customer='" + customerName + "', status='" + status + "'}";
    }
}
