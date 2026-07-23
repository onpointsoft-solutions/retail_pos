package com.retailpos.service;

import com.retailpos.model.*;
import com.retailpos.repository.*;
import com.retailpos.util.AuditLogger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SaleService {
    private static SaleService instance;
    private final SaleRepository saleRepo = new SaleRepository();
    private final CustomerRepository customerRepo = new CustomerRepository();
    private final ProductService productService = ProductService.getInstance();
    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final List<SaleListener> listeners = new CopyOnWriteArrayList<>();
    private int todayReceiptCounter = 0;

    private SaleService() {}

    public static synchronized SaleService getInstance() {
        if (instance == null) instance = new SaleService();
        return instance;
    }

    public void addListener(SaleListener l) { listeners.add(l); }
    public void removeListener(SaleListener l) { listeners.remove(l); }

    public Sale completeSale(List<Sale.SaleItem> items, String paymentMethod,
                              double cashTendered, String paymentRef,
                              String customerId, double transactionDiscount,
                              boolean adminStockOverride) throws Exception {
        if (items == null || items.isEmpty()) throw new Exception("Cart is empty");
        AuthService auth = AuthService.getInstance();
        if (!auth.isLoggedIn()) throw new Exception("Not logged in");

        AppSettings settings = settingsRepo.load();
        double taxRate = settings.getTaxRate();

        // Validate stock and build sale items
        double subtotal = 0;
        for (Sale.SaleItem item : items) {
            item.setTaxRate(taxRate);
            productService.decrementStock(item.getProductId(), item.getQuantity(), adminStockOverride);
            subtotal += item.getLineTotal();
        }

        double taxAmount = (subtotal - transactionDiscount) * taxRate / 100.0;
        double grandTotal = subtotal - transactionDiscount + taxAmount;

        if ("CASH".equalsIgnoreCase(paymentMethod) && cashTendered < grandTotal) {
            throw new Exception("Cash tendered (" + cashTendered + ") is less than grand total (" + grandTotal + ")");
        }

        Sale sale = new Sale();
        sale.setId(UUID.randomUUID().toString());
        sale.setReceiptNumber(generateReceiptNumber());
        sale.setCashierId(auth.getCurrentUser().getId());
        sale.setCashierName(auth.getCurrentUser().getFullName());
        sale.setCustomerId(customerId);
        sale.setItems(items);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(transactionDiscount);
        sale.setTaxAmount(taxAmount);
        sale.setGrandTotal(grandTotal);
        sale.setPaymentMethod(paymentMethod);
        sale.setCashTendered(cashTendered);
        sale.setChange(Math.max(0, cashTendered - grandTotal));
        sale.setPaymentReference(paymentRef);
        sale.setStatus("COMPLETED");
        sale.setSyncStatus("PENDING");
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());

        saleRepo.insert(sale);

        // Award loyalty points if customer attached
        if (customerId != null && !customerId.isBlank()) {
            awardLoyaltyPoints(customerId, grandTotal, settings.getLoyaltyEarningRate());
        }

        AuditLogger.log(auth.getCurrentUser().getId(), AuditLogger.SALE_COMPLETED,
            sale.getId(), "receipt=" + sale.getReceiptNumber() + ",total=" + grandTotal);

        for (SaleListener l : listeners) {
            try { l.onSaleCompleted(sale); } catch (Exception ignored) {}
        }
        return sale;
    }

    private String generateReceiptNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        todayReceiptCounter++;
        try {
            int count = saleRepo.countToday() + 1;
            return String.format("RCP-%s-%04d", date, count);
        } catch (Exception e) {
            return String.format("RCP-%s-%04d", date, todayReceiptCounter);
        }
    }

    private void awardLoyaltyPoints(String customerId, double grandTotal, double earningRate) {
        try {
            customerRepo.findById(customerId).ifPresent(customer -> {
                try {
                    int earned = (int) Math.floor(grandTotal * earningRate);
                    customerRepo.updateLoyaltyPoints(customerId, customer.getLoyaltyPoints() + earned);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public SuspendedCart suspendSale(String cashierId, List<Sale.SaleItem> items,
                                      double discount, String customerId) throws Exception {
        // Count existing suspended carts for this cashier
        long count;
        try (var c = com.retailpos.util.DatabaseManager.getConnection();
             var ps = c.prepareStatement("SELECT COUNT(*) FROM suspended_carts WHERE cashier_id=?")) {
            ps.setString(1, cashierId);
            var r = ps.executeQuery();
            count = r.next() ? r.getLong(1) : 0;
        }
        if (count >= 20) throw new Exception("Maximum suspended carts (20) reached. Please resume a cart first.");

        SuspendedCart cart = new SuspendedCart();
        cart.setId(UUID.randomUUID().toString());
        cart.setCashierId(cashierId);
        cart.setItems(items);
        cart.setDiscountAmount(discount);
        cart.setCustomerId(customerId);
        cart.setSuspendedAt(LocalDateTime.now());

        try (var c = com.retailpos.util.DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (var ps = c.prepareStatement(
                    "INSERT INTO suspended_carts(id,cashier_id,customer_id,discount_amount,suspended_at) VALUES(?,?,?,?,?)")) {
                    ps.setString(1, cart.getId()); ps.setString(2, cashierId); ps.setString(3, customerId);
                    ps.setDouble(4, discount); ps.setString(5, cart.getSuspendedAt().toString());
                    ps.executeUpdate();
                }
                for (Sale.SaleItem item : items) {
                    try (var ps = c.prepareStatement(
                        "INSERT INTO suspended_cart_items(id,cart_id,product_id,product_name,product_sku," +
                        "quantity,unit_price,buying_price,discount,tax_rate,line_total) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                        ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, cart.getId());
                        ps.setString(3, item.getProductId()); ps.setString(4, item.getProductName());
                        ps.setString(5, item.getProductSku()); ps.setInt(6, item.getQuantity());
                        ps.setDouble(7, item.getUnitPrice()); ps.setDouble(8, item.getBuyingPrice());
                        ps.setDouble(9, item.getDiscount()); ps.setDouble(10, item.getTaxRate());
                        ps.setDouble(11, item.getLineTotal());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
        return cart;
    }

    public List<SuspendedCart> getSuspendedCarts(String cashierId) throws Exception {
        List<SuspendedCart> carts = new ArrayList<>();
        try (var c = com.retailpos.util.DatabaseManager.getConnection();
             var ps = c.prepareStatement("SELECT * FROM suspended_carts WHERE cashier_id=? ORDER BY suspended_at DESC")) {
            ps.setString(1, cashierId);
            var r = ps.executeQuery();
            while (r.next()) {
                SuspendedCart cart = new SuspendedCart();
                cart.setId(r.getString("id")); cart.setCashierId(r.getString("cashier_id"));
                cart.setCustomerId(r.getString("customer_id")); cart.setDiscountAmount(r.getDouble("discount_amount"));
                String sa = r.getString("suspended_at"); if (sa != null) cart.setSuspendedAt(LocalDateTime.parse(sa));
                loadCartItems(cart);
                carts.add(cart);
            }
        }
        return carts;
    }

    private void loadCartItems(SuspendedCart cart) throws Exception {
        try (var c = com.retailpos.util.DatabaseManager.getConnection();
             var ps = c.prepareStatement("SELECT * FROM suspended_cart_items WHERE cart_id=?")) {
            ps.setString(1, cart.getId());
            var r = ps.executeQuery();
            List<Sale.SaleItem> items = new ArrayList<>();
            while (r.next()) {
                Sale.SaleItem item = new Sale.SaleItem();
                item.setProductId(r.getString("product_id")); item.setProductName(r.getString("product_name"));
                item.setProductSku(r.getString("product_sku")); item.setQuantity(r.getInt("quantity"));
                item.setUnitPrice(r.getDouble("unit_price")); item.setBuyingPrice(r.getDouble("buying_price"));
                item.setDiscount(r.getDouble("discount")); item.setTaxRate(r.getDouble("tax_rate"));
                item.setLineTotal(r.getDouble("line_total"));
                items.add(item);
            }
            cart.setItems(items);
        }
    }

    public SuspendedCart resumeSale(String cartId) throws Exception {
        SuspendedCart cart = null;
        try (var c = com.retailpos.util.DatabaseManager.getConnection();
             var ps = c.prepareStatement("SELECT * FROM suspended_carts WHERE id=?")) {
            ps.setString(1, cartId);
            var r = ps.executeQuery();
            if (r.next()) {
                cart = new SuspendedCart();
                cart.setId(r.getString("id")); cart.setCashierId(r.getString("cashier_id"));
                cart.setCustomerId(r.getString("customer_id")); cart.setDiscountAmount(r.getDouble("discount_amount"));
                String sa = r.getString("suspended_at"); if (sa != null) cart.setSuspendedAt(LocalDateTime.parse(sa));
                loadCartItems(cart);
            }
        }
        if (cart == null) throw new Exception("Suspended cart not found");
        // Delete from DB
        try (var c = com.retailpos.util.DatabaseManager.getConnection()) {
            try (var ps = c.prepareStatement("DELETE FROM suspended_cart_items WHERE cart_id=?")) {
                ps.setString(1, cartId); ps.executeUpdate();
            }
            try (var ps = c.prepareStatement("DELETE FROM suspended_carts WHERE id=?")) {
                ps.setString(1, cartId); ps.executeUpdate();
            }
        }
        return cart;
    }

    public Map<String,Object> getDashboardMetrics() throws Exception {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("sales_count_today", saleRepo.countToday());
        m.put("revenue_today", saleRepo.sumRevenueToday());
        m.put("profit_today", saleRepo.sumProfitToday());
        m.put("stock_value", productService.getTotalStockValue());
        m.put("pending_sync", productService.countPendingSync() + saleRepo.countPendingSync());
        m.put("low_stock_count", productService.getLowStock().size());
        m.put("recent_sales", saleRepo.findRecentCompleted(10));
        m.put("top_products_today", saleRepo.topProductsToday(10));
        m.put("sales_last_7_days", saleRepo.salesLast7Days());
        return m;
    }

    public interface SaleListener {
        void onSaleCompleted(Sale sale);
    }
}
