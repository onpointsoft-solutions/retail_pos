package com.retailpos.service;

import com.retailpos.model.InventoryMovement;
import com.retailpos.model.Product;
import com.retailpos.repository.InventoryRepository;
import com.retailpos.util.AuditLogger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class InventoryService {
    private static InventoryService instance;
    private final InventoryRepository repo = new InventoryRepository();
    private final ProductService productService = ProductService.getInstance();

    private InventoryService() {}

    public static synchronized InventoryService getInstance() {
        if (instance == null) instance = new InventoryService();
        return instance;
    }

    public void recordStockIn(String productId, int qty, String batchNumber,
                               LocalDate expiryDate, String userId) throws Exception {
        validateQty(qty);
        Optional<Product> opt = productService.findById(productId);
        if (opt.isEmpty()) throw new Exception("Product not found");
        Product p = opt.get();

        InventoryMovement m = new InventoryMovement();
        m.setId(UUID.randomUUID().toString());
        m.setProductId(productId); m.setProductName(p.getName());
        m.setType(InventoryMovement.STOCK_IN); m.setQuantity(qty);
        m.setBatchNumber(batchNumber); m.setExpiryDate(expiryDate);
        m.setUserId(userId); m.setSyncStatus("PENDING"); m.setCreatedAt(LocalDateTime.now());
        repo.insertMovement(m);

        productService.incrementStock(productId, qty);
    }

    public void recordAdjustment(String productId, int newQty, String reason, String userId) throws Exception {
        if (reason == null || reason.isBlank()) throw new Exception("Reason is required for stock adjustments");
        if (reason.length() > 255) throw new Exception("Reason cannot exceed 255 characters");
        if (newQty < 0) throw new Exception("New quantity cannot be negative");
        Optional<Product> opt = productService.findById(productId);
        if (opt.isEmpty()) throw new Exception("Product not found");
        Product p = opt.get();
        int delta = newQty - p.getCurrentStock();
        int qty = Math.abs(delta);

        InventoryMovement m = new InventoryMovement();
        m.setId(UUID.randomUUID().toString());
        m.setProductId(productId); m.setProductName(p.getName());
        m.setType(InventoryMovement.ADJUSTMENT); m.setQuantity(qty);
        m.setReason(reason + " (delta=" + (delta >= 0 ? "+" : "") + delta + ")");
        m.setUserId(userId); m.setSyncStatus("PENDING"); m.setCreatedAt(LocalDateTime.now());
        repo.insertMovement(m);

        AuditLogger.log(userId, AuditLogger.STOCK_ADJUSTMENT, productId,
            "product=" + p.getName() + ",old=" + p.getCurrentStock() + ",new=" + newQty + ",reason=" + reason);

        // Update stock directly
        com.retailpos.util.DatabaseManager.getConnection();
        try (var c = com.retailpos.util.DatabaseManager.getConnection();
             var ps = c.prepareStatement("UPDATE products SET current_stock=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setInt(1, newQty); ps.setString(2, LocalDateTime.now().toString()); ps.setString(3, productId);
            ps.executeUpdate();
        }
        productService.invalidateCache();
    }

    public void recordDamaged(String productId, int qty, String userId) throws Exception {
        validateQty(qty);
        Optional<Product> opt = productService.findById(productId);
        if (opt.isEmpty()) throw new Exception("Product not found");
        Product p = opt.get();

        InventoryMovement m = new InventoryMovement();
        m.setId(UUID.randomUUID().toString());
        m.setProductId(productId); m.setProductName(p.getName());
        m.setType(InventoryMovement.DAMAGED_GOODS); m.setQuantity(qty);
        m.setReason("Damaged goods recorded"); m.setUserId(userId);
        m.setSyncStatus("PENDING"); m.setCreatedAt(LocalDateTime.now());
        repo.insertMovement(m);
        productService.decrementStock(productId, qty, true);
    }

    public void recordExpired(String productId, int qty, String batchNumber, String userId) throws Exception {
        validateQty(qty);
        Optional<Product> opt = productService.findById(productId);
        if (opt.isEmpty()) throw new Exception("Product not found");
        Product p = opt.get();

        InventoryMovement m = new InventoryMovement();
        m.setId(UUID.randomUUID().toString());
        m.setProductId(productId); m.setProductName(p.getName());
        m.setType(InventoryMovement.EXPIRED_GOODS); m.setQuantity(qty);
        m.setBatchNumber(batchNumber); m.setReason("Expired goods recorded");
        m.setUserId(userId); m.setSyncStatus("PENDING"); m.setCreatedAt(LocalDateTime.now());
        repo.insertMovement(m);
        productService.decrementStock(productId, qty, true);
    }

    public List<InventoryMovement> getMovements(String productId) throws Exception {
        return repo.findByProductId(productId);
    }

    public List<InventoryMovement> getExpiredBatches() {
        try { return repo.findExpiredBatches(); } catch (Exception e) { return Collections.emptyList(); }
    }

    private void validateQty(int qty) throws Exception {
        if (qty <= 0) throw new Exception("Quantity must be greater than zero");
        if (qty > 999999) throw new Exception("Quantity exceeds maximum allowed value");
    }
}
