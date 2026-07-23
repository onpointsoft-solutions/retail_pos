package com.retailpos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    private String id;
    private String supplierId;
    private String supplierName;
    private String status; // ORDERED / PARTIALLY_RECEIVED / RECEIVED
    private List<PurchaseOrderItem> items = new ArrayList<>();
    private LocalDate expectedDeliveryDate;
    private String notes;
    private String syncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static class PurchaseOrderItem {
        private String productId;
        private String productName;
        private int orderedQty;
        private int receivedQty;
        private double buyingPrice;

        public PurchaseOrderItem() {}

        public PurchaseOrderItem(String productId, String productName, int orderedQty, double buyingPrice) {
            this.productId = productId;
            this.productName = productName;
            this.orderedQty = orderedQty;
            this.buyingPrice = buyingPrice;
            this.receivedQty = 0;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getOrderedQty() { return orderedQty; }
        public void setOrderedQty(int orderedQty) { this.orderedQty = orderedQty; }
        public int getReceivedQty() { return receivedQty; }
        public void setReceivedQty(int receivedQty) { this.receivedQty = receivedQty; }
        public double getBuyingPrice() { return buyingPrice; }
        public void setBuyingPrice(double buyingPrice) { this.buyingPrice = buyingPrice; }
        public int getOutstandingQty() { return orderedQty - receivedQty; }
        public double getLineTotal() { return orderedQty * buyingPrice; }
    }

    public PurchaseOrder() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public double getTotal() {
        return items.stream().mapToDouble(PurchaseOrderItem::getLineTotal).sum();
    }
}
