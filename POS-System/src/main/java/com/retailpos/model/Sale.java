package com.retailpos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sale {
    private String id;
    private String receiptNumber;
    private String cashierId;
    private String cashierName;
    private String customerId;
    private List<SaleItem> items = new ArrayList<>();
    private double subtotal;
    private double discountAmount;
    private double taxAmount;
    private double grandTotal;
    private String paymentMethod;
    private double cashTendered;
    private double change;
    private String paymentReference;
    private String status; // COMPLETED / VOIDED / SUSPENDED
    private String syncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static class SaleItem {
        private String productId;
        private String productName;
        private String productSku;
        private int quantity;
        private double unitPrice;
        private double buyingPrice;
        private double discount;
        private double taxRate;
        private double lineTotal;

        public SaleItem() {}

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductSku() { return productSku; }
        public void setProductSku(String productSku) { this.productSku = productSku; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public double getBuyingPrice() { return buyingPrice; }
        public void setBuyingPrice(double buyingPrice) { this.buyingPrice = buyingPrice; }
        public double getDiscount() { return discount; }
        public void setDiscount(double discount) { this.discount = discount; }
        public double getTaxRate() { return taxRate; }
        public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
        public double getLineTotal() { return lineTotal; }
        public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
        public double getProfit() { return (unitPrice - buyingPrice) * quantity; }

        public void recalculate() {
            double base = unitPrice * quantity;
            double discounted = base - discount;
            this.lineTotal = discounted;
        }
    }

    public Sale() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getCashierId() { return cashierId; }
    public void setCashierId(String cashierId) { this.cashierId = cashierId; }
    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }
    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public double getCashTendered() { return cashTendered; }
    public void setCashTendered(double cashTendered) { this.cashTendered = cashTendered; }
    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
