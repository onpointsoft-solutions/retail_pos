package com.mobilemeals.pos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages POS orders in-memory and notifies listeners of lifecycle events.
 */
public class OrderManagerSimple {

    // -------------------------------------------------------------------------
    // Inner class: POSOrder
    // -------------------------------------------------------------------------

    public static class POSOrder {

        public enum Status {
            PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED
        }

        private final String         id;
        private final String         orderNumber;
        private       String         customerName;
        private final List<OrderEntryPanelSimple.OrderItem> items;
        private       Status         status;
        private final LocalDateTime  createdAt;
        private       LocalDateTime  updatedAt;
        private       double         totalAmount;

        public POSOrder(String customerName) {
            this.id           = UUID.randomUUID().toString();
            this.orderNumber  = "ORD-" + System.currentTimeMillis();
            this.customerName = customerName;
            this.items        = new ArrayList<>();
            this.status       = Status.PENDING;
            this.createdAt    = LocalDateTime.now();
            this.updatedAt    = LocalDateTime.now();
        }

        // Getters
        public String          getId()           { return id; }
        public String          getOrderNumber()  { return orderNumber; }
        public String          getCustomerName() { return customerName; }
        public List<OrderEntryPanelSimple.OrderItem> getItems() { return items; }
        public Status          getStatus()       { return status; }
        public LocalDateTime   getCreatedAt()    { return createdAt; }
        public LocalDateTime   getUpdatedAt()    { return updatedAt; }
        public double          getTotalAmount()  { return totalAmount; }

        // Setters
        public void setCustomerName(String name)  { this.customerName = name; }
        public void setStatus(Status status)       { this.status = status; this.updatedAt = LocalDateTime.now(); }
        public void setTotalAmount(double amount)  { this.totalAmount = amount; }

        public void recalculateTotal() {
            totalAmount = items.stream()
                    .mapToDouble(i -> i.getQuantity() * i.getUnitPrice())
                    .sum();
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<POSOrder>        orders   = new ArrayList<>();
    private       OrderManagerListener  listener;

    // -------------------------------------------------------------------------
    // Listener
    // -------------------------------------------------------------------------

    public void setOrderManagerListener(OrderManagerListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Order CRUD
    // -------------------------------------------------------------------------

    public POSOrder createOrder(String customerName) {
        POSOrder order = new POSOrder(customerName);
        orders.add(order);
        if (listener != null) listener.onOrderCreated(order);
        return order;
    }

    public void updateOrderStatus(POSOrder order, POSOrder.Status status) {
        order.setStatus(status);
        if (listener != null) listener.onOrderUpdated(order);
    }

    public void cancelOrder(POSOrder order) {
        order.setStatus(POSOrder.Status.CANCELLED);
        if (listener != null) listener.onOrderCancelled(order);
    }

    public void addItemToOrder(POSOrder order, OrderEntryPanelSimple.OrderItem item) {
        order.getItems().add(item);
        order.recalculateTotal();
        if (listener != null) listener.onOrderItemAdded(item);
    }

    public void removeItemFromOrder(POSOrder order, OrderEntryPanelSimple.OrderItem item) {
        order.getItems().remove(item);
        order.recalculateTotal();
        if (listener != null) listener.onOrderItemRemoved(item);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public List<POSOrder> getAllOrders() {
        return new ArrayList<>(orders);
    }

    public List<POSOrder> getOrdersByStatus(POSOrder.Status status) {
        List<POSOrder> result = new ArrayList<>();
        for (POSOrder o : orders) {
            if (o.getStatus() == status) result.add(o);
        }
        return result;
    }

    public long countTodayOrders() {
        return orders.stream()
                .filter(o -> o.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
                .count();
    }

    public double todayRevenue() {
        return orders.stream()
                .filter(o -> o.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
                .filter(o -> o.getStatus() != POSOrder.Status.CANCELLED)
                .mapToDouble(POSOrder::getTotalAmount)
                .sum();
    }
}