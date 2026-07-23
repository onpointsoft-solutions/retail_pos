package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Order Management System for POS
 * Handles order creation, tracking, and status updates
 */
public class OrderManager {
    
    private List<POSOrder> orders;
    private POSOrder currentOrder;
    private OrderListener orderListener;
    
    public OrderManager() {
        orders = new ArrayList<>();
        currentOrder = new POSOrder();
    }
    
    public void setOrderListener(OrderListener listener) {
        this.orderListener = listener;
    }
    
    public POSOrder createNewOrder() {
        currentOrder = new POSOrder();
        currentOrder.setOrderNumber(generateOrderNumber());
        currentOrder.setCreatedTime(LocalDateTime.now());
        currentOrder.setStatus("pending");
        
        return currentOrder;
    }
    
    public void addItemToCurrentOrder(MenuManager.MenuItem item, int quantity) {
        if (currentOrder != null) {
            OrderItem orderItem = new OrderItem(item, quantity);
            currentOrder.addItem(orderItem);
            
            if (orderListener != null) {
                orderListener.onOrderItemAdded(orderItem);
            }
        }
    }
    
    public void removeItemFromCurrentOrder(OrderItem item) {
        if (currentOrder != null) {
            currentOrder.removeItem(item);
            
            if (orderListener != null) {
                orderListener.onOrderItemRemoved(item);
            }
        }
    }
    
    public void updateItemQuantity(OrderItem item, int quantity) {
        if (currentOrder != null) {
            item.setQuantity(quantity);
            
            if (orderListener != null) {
                orderListener.onOrderItemUpdated(item);
            }
        }
    }
    
    public void updateOrderCustomer(String customerName, String customerPhone, String customerAddress) {
        if (currentOrder != null) {
            currentOrder.setCustomerName(customerName);
            currentOrder.setCustomerPhone(customerPhone);
            currentOrder.setCustomerAddress(customerAddress);
        }
    }
    
    public boolean submitCurrentOrder() {
        if (currentOrder != null && currentOrder.getItems().size() > 0) {
            currentOrder.setStatus("submitted");
            currentOrder.setSubmittedTime(LocalDateTime.now());
            
            orders.add(currentOrder);
            
            // Submit to backend API
            boolean success = submitOrderToBackend(currentOrder);
            
            if (success) {
                if (orderListener != null) {
                    orderListener.onOrderSubmitted(currentOrder);
                }
                
                // Create new order for next transaction
                currentOrder = new POSOrder();
                return true;
            }
        }
        
        return false;
    }
    
    public void updateOrderStatus(String orderId, String newStatus) {
        POSOrder order = findOrderById(orderId);
        if (order != null) {
            String oldStatus = order.getStatus();
            order.setStatus(newStatus);
            
            // Update backend
            updateOrderStatusInBackend(orderId, newStatus);
            
            if (orderListener != null) {
                orderListener.onOrderStatusUpdated(order, oldStatus, newStatus);
            }
        }
    }
    
    public void cancelOrder(String orderId) {
        POSOrder order = findOrderById(orderId);
        if (order != null) {
            order.setStatus("cancelled");
            order.setCancelledTime(LocalDateTime.now());
            
            // Update backend
            updateOrderStatusInBackend(orderId, "cancelled");
            
            if (orderListener != null) {
                orderListener.onOrderCancelled(order);
            }
        }
    }
    
    public List<POSOrder> getOrders() {
        return new ArrayList<>(orders);
    }
    
    public List<POSOrder> getOrdersByStatus(String status) {
        List<POSOrder> filteredOrders = new ArrayList<>();
        for (POSOrder order : orders) {
            if (order.getStatus().equals(status)) {
                filteredOrders.add(order);
            }
        }
        return filteredOrders;
    }
    
    public List<POSOrder> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<POSOrder> filteredOrders = new ArrayList<>();
        for (POSOrder order : orders) {
            if (order.getCreatedTime().isAfter(startDate) && order.getCreatedTime().isBefore(endDate)) {
                filteredOrders.add(order);
            }
        }
        return filteredOrders;
    }
    
    public POSOrder getCurrentOrder() {
        return currentOrder;
    }
    
    public POSOrder findOrderById(String orderId) {
        for (POSOrder order : orders) {
            if (order.getOrderNumber().equals(orderId)) {
                return order;
            }
        }
        return null;
    }
    
    public double getCurrentOrderTotal() {
        if (currentOrder != null) {
            return currentOrder.getTotalAmount();
        }
        return 0.0;
    }
    
    public int getCurrentOrderItemCount() {
        if (currentOrder != null) {
            return currentOrder.getItems().size();
        }
        return 0;
    }
    
    public void clearCurrentOrder() {
        currentOrder = new POSOrder();
    }
    
    private String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "POS-" + formatter.format(LocalDateTime.now());
    }
    
    private boolean submitOrderToBackend(POSOrder order) {
        // TODO: Implement API call to submit order to backend
        try {
            // Simulate API call
            Thread.sleep(1000);
            
            // For demo purposes, always return true
            // In production, this would make actual API call
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void updateOrderStatusInBackend(String orderId, String status) {
        // TODO: Implement API call to update order status
        try {
            // Simulate API call
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    // Order Listener Interface
    public interface OrderListener {
        void onOrderItemAdded(OrderItem item);
        void onOrderItemRemoved(OrderItem item);
        void onOrderItemUpdated(OrderItem item);
        void onOrderSubmitted(POSOrder order);
        void onOrderStatusUpdated(POSOrder order, String oldStatus, String newStatus);
        void onOrderCancelled(POSOrder order);
    }
    
    // Order Data Class
    public static class POSOrder {
        private String orderNumber;
        private List<OrderItem> items;
        private String customerName;
        private String customerPhone;
        private String customerAddress;
        private String status;
        private double totalAmount;
        private LocalDateTime createdTime;
        private LocalDateTime submittedTime;
        private LocalDateTime cancelledTime;
        
        public POSOrder() {
            this.items = new ArrayList<>();
            this.status = "pending";
            this.totalAmount = 0.0;
        }
        
        // Getters and Setters
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        
        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
        
        public String getCustomerAddress() { return customerAddress; }
        public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
        
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
        
        public LocalDateTime getSubmittedTime() { return submittedTime; }
        public void setSubmittedTime(LocalDateTime submittedTime) { this.submittedTime = submittedTime; }
        
        public LocalDateTime getCancelledTime() { return cancelledTime; }
        public void setCancelledTime(LocalDateTime cancelledTime) { this.cancelledTime = cancelledTime; }
        
        // Helper methods
        public void addItem(OrderItem item) {
            items.add(item);
            calculateTotal();
        }
        
        public void removeItem(OrderItem item) {
            items.remove(item);
            calculateTotal();
        }
        
        private void calculateTotal() {
            totalAmount = 0.0;
            for (OrderItem item : items) {
                totalAmount += item.getTotalPrice();
            }
        }
    }
    
    // Order Item Data Class
    public static class OrderItem {
        private MenuManager.MenuItem menuItem;
        private int quantity;
        private double unitPrice;
        private double totalPrice;
        
        public OrderItem(MenuManager.MenuItem menuItem, int quantity) {
            this.menuItem = menuItem;
            this.quantity = quantity;
            this.unitPrice = menuItem.getPrice();
            this.totalPrice = unitPrice * quantity;
        }
        
        // Getters and Setters
        public MenuManager.MenuItem getMenuItem() { return menuItem; }
        public void setMenuItem(MenuManager.MenuItem menuItem) { this.menuItem = menuItem; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { 
            this.quantity = quantity; 
            this.totalPrice = unitPrice * quantity;
        }
        
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        
        public double getTotalPrice() { return totalPrice; }
        public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    }
}
