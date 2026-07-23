package com.retailpos.model;

import java.time.LocalDateTime;

public class Customer {
    private String id;
    private String name;
    private String phone;
    private String email;
    private int loyaltyPoints;
    private double creditBalance;
    private String syncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    public double getCreditBalance() { return creditBalance; }
    public void setCreditBalance(double creditBalance) { this.creditBalance = creditBalance; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return name + (phone != null ? " - " + phone : ""); }
}
