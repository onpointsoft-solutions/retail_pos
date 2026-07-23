package com.retailpos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InventoryMovement {
    public static final String STOCK_IN      = "STOCK_IN";
    public static final String STOCK_OUT     = "STOCK_OUT";
    public static final String TRANSFER      = "TRANSFER";
    public static final String ADJUSTMENT    = "ADJUSTMENT";
    public static final String RETURN        = "RETURN";
    public static final String DAMAGED_GOODS = "DAMAGED_GOODS";
    public static final String EXPIRED_GOODS = "EXPIRED_GOODS";

    private String id;
    private String productId;
    private String productName;
    private String type;
    private int quantity;
    private String reason;
    private String batchNumber;
    private LocalDate expiryDate;
    private String userId;
    private String syncStatus;
    private LocalDateTime createdAt;

    public InventoryMovement() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
