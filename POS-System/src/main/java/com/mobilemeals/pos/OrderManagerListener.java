package com.mobilemeals.pos;

/**
 * Listener for order lifecycle events fired by {@link OrderManagerSimple}.
 */
public interface OrderManagerListener {

    void onOrderCreated(OrderManagerSimple.POSOrder order);

    void onOrderUpdated(OrderManagerSimple.POSOrder order);

    void onOrderCancelled(OrderManagerSimple.POSOrder order);

    void onOrderItemAdded(OrderEntryPanelSimple.OrderItem item);

    void onOrderItemRemoved(OrderEntryPanelSimple.OrderItem item);

    void onOrderItemUpdated(OrderEntryPanelSimple.OrderItem item);
}