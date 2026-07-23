package com.retailpos.model;

import java.time.LocalDateTime;

public class Product {
    private String id;
    private String barcode;
    private String qrCode;
    private String sku;
    private String name;
    private String categoryId;
    private double buyingPrice;
    private double sellingPrice;
    private double wholesalePrice;
    private int currentStock;
    private int minimumStock;
    private int preferredOrderQuantity;
    private double taxRate;
    private double discount;
    private String supplierId;
    private String description;
    private String imagePath;
    private String unit; // pieces, kg, g, litres, ml, bars, boxes, dozens, metres, pairs
    private String status; // "active" / "inactive"
    private boolean trackExpiry;
    private String syncStatus; // PENDING / SYNCED / MODIFIED / DELETED
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public double getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(double buyingPrice) { this.buyingPrice = buyingPrice; }
    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
    public double getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(double wholesalePrice) { this.wholesalePrice = wholesalePrice; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getMinimumStock() { return minimumStock; }
    public void setMinimumStock(int minimumStock) { this.minimumStock = minimumStock; }
    public int getPreferredOrderQuantity() { return preferredOrderQuantity; }
    public void setPreferredOrderQuantity(int preferredOrderQuantity) { this.preferredOrderQuantity = preferredOrderQuantity; }
    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getUnit() { return unit != null ? unit : "pcs"; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isTrackExpiry() { return trackExpiry; }
    public void setTrackExpiry(boolean trackExpiry) { this.trackExpiry = trackExpiry; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public boolean isActive() { return "active".equals(status); }
    public boolean isLowStock() { return currentStock <= minimumStock; }

    @Override
    public String toString() { return name + " (" + sku + ")"; }
}
