package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified Receipt Printer for POS
 * Handles receipt printing functionality
 */
public class ReceiptPrinterSimple {
    
    public void printOrder(String orderNumber, String customerName, List<OrderEntryPanelSimple.OrderItem> items, double total) {
        // This is a simplified implementation
        System.out.println("=== MOBILE MEALS RECEIPT ===");
        System.out.println("Order #: " + orderNumber);
        System.out.println("Customer: " + customerName);
        System.out.println("Date: " + java.time.LocalDateTime.now().toString());
        System.out.println("------------------------------");
        System.out.println("ITEMS:");
        
        for (OrderEntryPanelSimple.OrderItem item : items) {
            System.out.println("  " + item.getQuantity() + " x " + item.getName() + 
                             " @ KES " + String.format("%.2f", item.getPrice()) + 
                             " = KES " + String.format("%.2f", item.getTotal()));
        }
        
        System.out.println("------------------------------");
        System.out.println("Subtotal: KES " + String.format("%.2f", items.stream().mapToDouble(item -> item.getTotal()).sum()));
        System.out.println("Tax (16%): KES " + String.format("%.2f", items.stream().mapToDouble(item -> item.getTotal()).sum() * 0.16));
        System.out.println("TOTAL: KES " + String.format("%.2f", total));
        System.out.println("===============================");
    }
    
    public void printSalesReport(double revenue, int orderCount) {
        System.out.println("=== SALES REPORT ===");
        System.out.println("Date: " + java.time.LocalDateTime.now().toString());
        System.out.println("------------------------------");
        System.out.println("Total Revenue: KES " + String.format("%.2f", revenue));
        System.out.println("Total Orders: " + orderCount);
        System.out.println("===============================");
    }
}
