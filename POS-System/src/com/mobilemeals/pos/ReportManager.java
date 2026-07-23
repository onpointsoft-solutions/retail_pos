package com.mobilemeals.pos;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Report Management System for POS
 * Handles sales reports, order reports, and analytics
 */
public class ReportManager {
    
    private List<SalesRecord> salesRecords;
    private List<POSOrder> orderRecords;
    private ReportListener reportListener;
    
    public ReportManager() {
        salesRecords = new ArrayList<>();
        orderRecords = new ArrayList<>();
        loadSampleData();
    }
    
    public void setReportListener(ReportListener listener) {
        this.reportListener = listener;
    }
    
    public SalesReport generateSalesReport(LocalDate startDate, LocalDate endDate) {
        List<SalesRecord> filteredRecords = new ArrayList<>();
        double totalSales = 0.0;
        int totalOrders = 0;
        
        for (SalesRecord record : salesRecords) {
            if (!record.getDate().isBefore(startDate) && !record.getDate().isAfter(endDate)) {
                filteredRecords.add(record);
                totalSales += record.getTotalAmount();
                totalOrders += record.getOrderCount();
            }
        }
        
        return new SalesReport(startDate, endDate, filteredRecords, totalSales, totalOrders);
    }
    
    public OrderReport generateOrderReport(LocalDate startDate, LocalDate endDate) {
        List<POSOrder> filteredOrders = new ArrayList<>();
        Map<String, Integer> statusCounts = new HashMap<>();
        
        for (POSOrder order : orderRecords) {
            if (order.getCreatedTime().toLocalDate().isAfter(startDate.minusDays(1)) && 
                order.getCreatedTime().toLocalDate().isBefore(endDate.plusDays(1))) {
                filteredOrders.add(order);
                
                String status = order.getStatus();
                statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
            }
        }
        
        return new OrderReport(startDate, endDate, filteredOrders, statusCounts);
    }
    
    public RevenueReport generateRevenueReport(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Double> dailyRevenue = new TreeMap<>();
        double totalRevenue = 0.0;
        
        for (SalesRecord record : salesRecords) {
            if (!record.getDate().isBefore(startDate) && !record.getDate().isAfter(endDate)) {
                dailyRevenue.put(record.getDate(), record.getTotalAmount());
                totalRevenue += record.getTotalAmount();
            }
        }
        
        return new RevenueReport(startDate, endDate, dailyRevenue, totalRevenue);
    }
    
    public MenuPerformanceReport generateMenuPerformanceReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> itemSales = new HashMap<>();
        Map<String, Double> itemRevenue = new HashMap<>();
        
        for (POSOrder order : orderRecords) {
            if (order.getCreatedTime().toLocalDate().isAfter(startDate.minusDays(1)) && 
                order.getCreatedTime().toLocalDate().isBefore(endDate.plusDays(1))) {
                
                for (OrderManager.OrderItem item : order.getItems()) {
                    String itemName = item.getMenuItem().getName();
                    itemSales.put(itemName, itemSales.getOrDefault(itemName, 0) + item.getQuantity());
                    itemRevenue.put(itemName, itemRevenue.getOrDefault(itemName, 0.0) + item.getTotalPrice());
                }
            }
        }
        
        return new MenuPerformanceReport(startDate, endDate, itemSales, itemRevenue);
    }
    
    public void addSalesRecord(SalesRecord record) {
        salesRecords.add(record);
        
        if (reportListener != null) {
            reportListener.onSalesRecordAdded(record);
        }
    }
    
    public void addOrderRecord(POSOrder order) {
        orderRecords.add(order);
        
        if (reportListener != null) {
            reportListener.onOrderRecordAdded(order);
        }
    }
    
    public List<SalesRecord> getSalesRecords() {
        return new ArrayList<>(salesRecords);
    }
    
    public List<POSOrder> getOrderRecords() {
        return new ArrayList<>(orderRecords);
    }
    
    public Map<String, Object> getTodayStats() {
        LocalDate today = LocalDate.now();
        double todayRevenue = 0.0;
        int todayOrders = 0;
        int completedOrders = 0;
        
        for (SalesRecord record : salesRecords) {
            if (record.getDate().equals(today)) {
                todayRevenue += record.getTotalAmount();
                todayOrders += record.getOrderCount();
            }
        }
        
        for (POSOrder order : orderRecords) {
            if (order.getCreatedTime().toLocalDate().equals(today)) {
                if (order.getStatus().equals("completed")) {
                    completedOrders++;
                }
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("revenue", todayRevenue);
        stats.put("orders", todayOrders);
        stats.put("completed", completedOrders);
        stats.put("pending", todayOrders - completedOrders);
        
        return stats;
    }
    
    private void loadSampleData() {
        // Load sample data for demo purposes
        LocalDate today = LocalDate.now();
        
        // Sample sales records
        salesRecords.add(new SalesRecord(today, 15000.00, 25));
        salesRecords.add(new SalesRecord(today.minusDays(1), 12000.00, 20));
        salesRecords.add(new SalesRecord(today.minusDays(2), 18000.00, 30));
        
        // Sample order records
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            POSOrder order = new POSOrder();
            order.setOrderNumber("POS-" + String.format("%06d", i + 1));
            order.setCustomerName("Customer " + (i + 1));
            order.setTotalAmount(random.nextDouble() * 1000 + 100);
            order.setCreatedTime(LocalDateTime.now().minusDays(random.nextInt(30)));
            order.setStatus(getRandomStatus());
            orderRecords.add(order);
        }
    }
    
    private String getRandomStatus() {
        String[] statuses = {"pending", "confirmed", "preparing", "ready", "completed", "cancelled"};
        return statuses[new java.util.Random().nextInt(statuses.length)];
    }
    
    // Report Listener Interface
    public interface ReportListener {
        void onSalesRecordAdded(SalesRecord record);
        void onOrderRecordAdded(POSOrder order);
    }
    
    // SalesRecord Data Class
    public static class SalesRecord {
        private LocalDate date;
        private double totalAmount;
        private int orderCount;
        
        public SalesRecord(LocalDate date, double totalAmount, int orderCount) {
            this.date = date;
            this.totalAmount = totalAmount;
            this.orderCount = orderCount;
        }
        
        // Getters and Setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
        
        public int getOrderCount() { return orderCount; }
        public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
        
        public String getFormattedAmount() {
            return String.format("KES %.2f", totalAmount);
        }
    }
    
    // SalesReport Data Class
    public static class SalesReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<SalesRecord> records;
        private double totalSales;
        private int totalOrders;
        
        public SalesReport(LocalDate startDate, LocalDate endDate, List<SalesRecord> records, double totalSales, int totalOrders) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.records = records;
            this.totalSales = totalSales;
            this.totalOrders = totalOrders;
        }
        
        // Getters
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public List<SalesRecord> getRecords() { return records; }
        public double getTotalSales() { return totalSales; }
        public int getTotalOrders() { return totalOrders; }
        
        public String getFormattedTotalSales() {
            return String.format("KES %.2f", totalSales);
        }
        
        public double getAverageOrderValue() {
            return totalOrders > 0 ? totalSales / totalOrders : 0.0;
        }
    }
    
    // OrderReport Data Class
    public static class OrderReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<POSOrder> orders;
        private Map<String, Integer> statusCounts;
        
        public OrderReport(LocalDate startDate, LocalDate endDate, List<POSOrder> orders, Map<String, Integer> statusCounts) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.orders = orders;
            this.statusCounts = statusCounts;
        }
        
        // Getters
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public List<POSOrder> getOrders() { return orders; }
        public Map<String, Integer> getStatusCounts() { return statusCounts; }
        
        public int getTotalOrders() {
            return orders.size();
        }
        
        public int getCompletedOrders() {
            return statusCounts.getOrDefault("completed", 0);
        }
        
        public int getPendingOrders() {
            return statusCounts.getOrDefault("pending", 0) + 
                   statusCounts.getOrDefault("confirmed", 0) + 
                   statusCounts.getOrDefault("preparing", 0) + 
                   statusCounts.getOrDefault("ready", 0);
        }
    }
    
    // RevenueReport Data Class
    public static class RevenueReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private Map<LocalDate, Double> dailyRevenue;
        private double totalRevenue;
        
        public RevenueReport(LocalDate startDate, LocalDate endDate, Map<LocalDate, Double> dailyRevenue, double totalRevenue) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.dailyRevenue = dailyRevenue;
            this.totalRevenue = totalRevenue;
        }
        
        // Getters
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public Map<LocalDate, Double> getDailyRevenue() { return dailyRevenue; }
        public double getTotalRevenue() { return totalRevenue; }
        
        public double getAverageDailyRevenue() {
            return dailyRevenue.size() > 0 ? totalRevenue / dailyRevenue.size() : 0.0;
        }
        
        public LocalDate getBestDay() {
            return dailyRevenue.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
    }
    
    // MenuPerformanceReport Data Class
    public static class MenuPerformanceReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private Map<String, Integer> itemSales;
        private Map<String, Double> itemRevenue;
        
        public MenuPerformanceReport(LocalDate startDate, LocalDate endDate, Map<String, Integer> itemSales, Map<String, Double> itemRevenue) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.itemSales = itemSales;
            this.itemRevenue = itemRevenue;
        }
        
        // Getters
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public Map<String, Integer> getItemSales() { return itemSales; }
        public Map<String, Double> getItemRevenue() { return itemRevenue; }
        
        public String getBestSellingItem() {
            return itemSales.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        public String getHighestRevenueItem() {
            return itemRevenue.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        public List<String> getTopSellingItems(int count) {
            return itemSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        }
    }
}
