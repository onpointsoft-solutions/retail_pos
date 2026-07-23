package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.text.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Receipt Printer for POS
 * Handles receipt printing and formatting
 */
public class ReceiptPrinter {
    
    private static final int RECEIPT_WIDTH = 300;
    private static final int MARGIN = 20;
    
    public void printOrderReceipt(OrderManager.POSOrder order, RestaurantSession session) {
        try {
            // Create printable component
            ReceiptComponent receipt = new ReceiptComponent(order, session);
            
            // Create printer job
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Order Receipt - " + order.getOrderNumber());
            
            // Set printable
            job.setPrintable(receipt);
            
            // Show print dialog
            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(null, 
                    "Receipt printed successfully!", 
                    "Print Success", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null, 
                "Failed to print receipt: " + e.getMessage(), 
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void printSalesReport(ReportManager.SalesReport report, RestaurantSession session) {
        try {
            // Create printable component
            SalesReportComponent reportComponent = new SalesReportComponent(report, session);
            
            // Create printer job
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Sales Report");
            
            // Set printable
            job.setPrintable(reportComponent);
            
            // Show print dialog
            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(null, 
                    "Sales report printed successfully!", 
                    "Print Success", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null, 
                "Failed to print report: " + e.getMessage(), 
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void printKitchenTicket(OrderManager.POSOrder order, RestaurantSession session) {
        try {
            // Create printable component
            KitchenTicketComponent kitchenTicket = new KitchenTicketComponent(order, session);
            
            // Create printer job
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Kitchen Ticket - " + order.getOrderNumber());
            
            // Set printable
            job.setPrintable(kitchenTicket);
            
            // Print without dialog (kitchen printer)
            job.print();
            
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null, 
                "Failed to print kitchen ticket: " + e.getMessage(), 
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Receipt Component for printing
    private static class ReceiptComponent implements Printable {
        private OrderManager.POSOrder order;
        private RestaurantSession session;
        
        public ReceiptComponent(OrderManager.POSOrder order, RestaurantSession session) {
            this.order = order;
            this.session = session;
        }
        
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            
            // Set font
            Font headerFont = new Font("Monospaced", Font.BOLD, 12);
            Font normalFont = new Font("Monospaced", Font.PLAIN, 10);
            Font smallFont = new Font("Monospaced", Font.PLAIN, 8);
            
            int y = MARGIN;
            int lineHeight = 15;
            
            // Restaurant Header
            g2d.setFont(headerFont);
            g2d.drawString(session.getRestaurantName(), MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(smallFont);
            g2d.drawString(session.getRestaurantAddress(), MARGIN, y);
            y += lineHeight;
            g2d.drawString("Tel: " + session.getRestaurantPhone(), MARGIN, y);
            y += lineHeight + 10;
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Order Info
            g2d.setFont(normalFont);
            g2d.drawString("ORDER RECEIPT", MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(smallFont);
            g2d.drawString("Order #: " + order.getOrderNumber(), MARGIN, y);
            y += lineHeight;
            
            if (order.getCreatedTime() != null) {
                g2d.drawString("Date: " + order.getCreatedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), MARGIN, y);
                y += lineHeight;
            }
            
            g2d.drawString("Cashier: " + session.getUserName(), MARGIN, y);
            y += lineHeight + 10;
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Order Items
            g2d.setFont(normalFont);
            g2d.drawString("ITEMS", MARGIN, y);
            y += lineHeight;
            
            double subtotal = 0.0;
            for (OrderManager.OrderItem item : order.getItems()) {
                // Item name and quantity
                g2d.setFont(smallFont);
                String itemLine = String.format("%d x %s", item.getQuantity(), item.getMenuItem().getName());
                g2d.drawString(itemLine, MARGIN, y);
                y += lineHeight;
                
                // Item price
                String priceLine = String.format("    @ %s", String.format("KES %.2f", item.getUnitPrice()));
                g2d.drawString(priceLine, MARGIN, y);
                y += lineHeight;
                
                // Item total
                String totalLine = String.format("    %s", String.format("KES %.2f", item.getTotalPrice()));
                g2d.setFont(normalFont);
                g2d.drawString(totalLine, RECEIPT_WIDTH - MARGIN - 60, y);
                y += lineHeight + 5;
                
                subtotal += item.getTotalPrice();
            }
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Totals
            double tax = subtotal * 0.16;
            double delivery = 50.0;
            double total = subtotal + tax + delivery;
            
            g2d.setFont(normalFont);
            g2d.drawString("Subtotal:", MARGIN, y);
            g2d.drawString(String.format("KES %.2f", subtotal), RECEIPT_WIDTH - MARGIN - 60, y);
            y += lineHeight;
            
            g2d.drawString("Tax (16%):", MARGIN, y);
            g2d.drawString(String.format("KES %.2f", tax), RECEIPT_WIDTH - MARGIN - 60, y);
            y += lineHeight;
            
            g2d.drawString("Delivery:", MARGIN, y);
            g2d.drawString(String.format("KES %.2f", delivery), RECEIPT_WIDTH - MARGIN - 60, y);
            y += lineHeight + 5;
            
            // Total
            g2d.setFont(headerFont);
            g2d.drawString("TOTAL:", MARGIN, y);
            g2d.drawString(String.format("KES %.2f", total), RECEIPT_WIDTH - MARGIN - 60, y);
            y += lineHeight + 10;
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Customer Info
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                g2d.setFont(normalFont);
                g2d.drawString("CUSTOMER", MARGIN, y);
                y += lineHeight;
                
                g2d.setFont(smallFont);
                g2d.drawString("Name: " + order.getCustomerName(), MARGIN, y);
                y += lineHeight;
                
                if (order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty()) {
                    g2d.drawString("Phone: " + order.getCustomerPhone(), MARGIN, y);
                    y += lineHeight;
                }
                
                if (order.getCustomerAddress() != null && !order.getCustomerAddress().isEmpty()) {
                    g2d.drawString("Address: " + order.getCustomerAddress(), MARGIN, y);
                    y += lineHeight;
                }
                y += lineHeight + 5;
            }
            
            // Footer
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(smallFont);
            g2d.drawString("Thank you for your order!", MARGIN, y);
            y += lineHeight;
            g2d.drawString("Please come again", MARGIN, y);
            y += lineHeight;
            g2d.drawString("Powered by Mobile Meals", MARGIN, y);
            
            return PAGE_EXISTS;
        }
    }
    
    // Sales Report Component for printing
    private static class SalesReportComponent implements Printable {
        private ReportManager.SalesReport report;
        private RestaurantSession session;
        
        public SalesReportComponent(ReportManager.SalesReport report, RestaurantSession session) {
            this.report = report;
            this.session = session;
        }
        
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            
            // Set font
            Font headerFont = new Font("Monospaced", Font.BOLD, 12);
            Font normalFont = new Font("Monospaced", Font.PLAIN, 10);
            Font smallFont = new Font("Monospaced", Font.PLAIN, 8);
            
            int y = MARGIN;
            int lineHeight = 15;
            
            // Report Header
            g2d.setFont(headerFont);
            g2d.drawString(session.getRestaurantName(), MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(normalFont);
            g2d.drawString("SALES REPORT", MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(smallFont);
            g2d.drawString("Period: " + report.getStartDate() + " to " + report.getEndDate(), MARGIN, y);
            y += lineHeight;
            g2d.drawString("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), MARGIN, y);
            y += lineHeight + 10;
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Summary
            g2d.setFont(normalFont);
            g2d.drawString("SUMMARY", MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(smallFont);
            g2d.drawString("Total Orders: " + report.getTotalOrders(), MARGIN, y);
            y += lineHeight;
            g2d.drawString("Total Sales: " + report.getFormattedTotalSales(), MARGIN, y);
            y += lineHeight;
            g2d.drawString("Average Order: " + String.format("KES %.2f", report.getAverageOrderValue()), MARGIN, y);
            y += lineHeight + 10;
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Daily Breakdown
            g2d.setFont(normalFont);
            g2d.drawString("DAILY BREAKDOWN", MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(smallFont);
            for (ReportManager.SalesRecord record : report.getRecords()) {
                g2d.drawString(record.getDate() + ": " + record.getFormattedAmount() + " (" + record.getOrderCount() + " orders)", MARGIN, y);
                y += lineHeight;
            }
            
            return PAGE_EXISTS;
        }
    }
    
    // Kitchen Ticket Component for printing
    private static class KitchenTicketComponent implements Printable {
        private OrderManager.POSOrder order;
        private RestaurantSession session;
        
        public KitchenTicketComponent(OrderManager.POSOrder order, RestaurantSession session) {
            this.order = order;
            this.session = session;
        }
        
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            
            // Set font
            Font headerFont = new Font("Monospaced", Font.BOLD, 14);
            Font normalFont = new Font("Monospaced", Font.PLAIN, 10);
            Font smallFont = new Font("Monospaced", Font.PLAIN, 8);
            
            int y = MARGIN;
            int lineHeight = 15;
            
            // Kitchen Header
            g2d.setFont(headerFont);
            g2d.drawString("KITCHEN ORDER", MARGIN, y);
            y += lineHeight;
            
            g2d.setFont(normalFont);
            g2d.drawString("ORDER #: " + order.getOrderNumber(), MARGIN, y);
            y += lineHeight;
            
            if (order.getCreatedTime() != null) {
                g2d.drawString("TIME: " + order.getCreatedTime().format(DateTimeFormatter.ofPattern("HH:mm")), MARGIN, y);
                y += lineHeight;
            }
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Order Items (for kitchen)
            g2d.setFont(headerFont);
            g2d.drawString("ITEMS TO PREPARE", MARGIN, y);
            y += lineHeight;
            
            for (OrderManager.OrderItem item : order.getItems()) {
                // Item name and quantity (larger for kitchen)
                g2d.setFont(normalFont);
                String itemLine = String.format("%d x %s", item.getQuantity(), item.getMenuItem().getName().toUpperCase());
                g2d.drawString(itemLine, MARGIN, y);
                y += lineHeight + 5;
                
                // Notes if any
                if (item.getMenuItem().getDescription() != null && !item.getMenuItem().getDescription().isEmpty()) {
                    g2d.setFont(smallFont);
                    g2d.drawString("  Note: " + item.getMenuItem().getDescription(), MARGIN, y);
                    y += lineHeight;
                }
            }
            
            // Separator
            g2d.drawLine(MARGIN, y, RECEIPT_WIDTH - MARGIN, y);
            y += lineHeight;
            
            // Customer info for delivery
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                g2d.setFont(normalFont);
                g2d.drawString("DELIVERY TO:", MARGIN, y);
                y += lineHeight;
                
                g2d.setFont(smallFont);
                g2d.drawString(order.getCustomerName(), MARGIN, y);
                y += lineHeight;
                
                if (order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty()) {
                    g2d.drawString("Tel: " + order.getCustomerPhone(), MARGIN, y);
                    y += lineHeight;
                }
                
                if (order.getCustomerAddress() != null && !order.getCustomerAddress().isEmpty()) {
                    g2d.drawString(order.getCustomerAddress(), MARGIN, y);
                    y += lineHeight;
                }
            }
            
            return PAGE_EXISTS;
        }
    }
}
