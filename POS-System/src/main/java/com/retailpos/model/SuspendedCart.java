package com.retailpos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SuspendedCart {
    private String id;
    private String cashierId;
    private List<Sale.SaleItem> items = new ArrayList<>();
    private double discountAmount;
    private String customerId;
    private LocalDateTime suspendedAt;

    public SuspendedCart() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCashierId() { return cashierId; }
    public void setCashierId(String cashierId) { this.cashierId = cashierId; }
    public List<Sale.SaleItem> getItems() { return items; }
    public void setItems(List<Sale.SaleItem> items) { this.items = items; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public LocalDateTime getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(LocalDateTime suspendedAt) { this.suspendedAt = suspendedAt; }

    public double getTotal() {
        return items.stream().mapToDouble(Sale.SaleItem::getLineTotal).sum() - discountAmount;
    }
}
