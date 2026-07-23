package com.retailpos.service;

import com.retailpos.model.Product;
import com.retailpos.repository.ProductRepository;
import com.retailpos.util.AuditLogger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ProductService {
    private static ProductService instance;
    private final ProductRepository repo = new ProductRepository();
    private volatile List<Product> productCache;
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static final int CACHE_TTL_SECONDS = 300;
    private volatile long cacheTimestamp = 0;

    private ProductService() {}

    public static synchronized ProductService getInstance() {
        if (instance == null) instance = new ProductService();
        return instance;
    }

    private List<Product> getCache() {
        cacheLock.readLock().lock();
        try {
            if (productCache != null &&
                System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_SECONDS * 1000L) {
                return productCache;
            }
        } finally { cacheLock.readLock().unlock(); }
        return null;
    }

    public void invalidateCache() {
        cacheLock.writeLock().lock();
        try { productCache = null; cacheTimestamp = 0; }
        finally { cacheLock.writeLock().unlock(); }
    }

    private void refreshCache() {
        cacheLock.writeLock().lock();
        try {
            productCache = new CopyOnWriteArrayList<>(repo.findActive());
            cacheTimestamp = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("[ProductService] Cache refresh failed: " + e.getMessage());
        } finally { cacheLock.writeLock().unlock(); }
    }

    public List<Product> search(String query) {
        if (query == null || query.isBlank()) {
            List<Product> cached = getCache();
            if (cached != null) return cached.stream().limit(50).collect(Collectors.toList());
        }
        try {
            List<Product> cached = getCache();
            if (cached == null) { refreshCache(); cached = getCache(); }
            if (cached == null) return repo.search(query, 50, 0);
            final String q = query.trim().toLowerCase();
            return cached.stream()
                .filter(p -> p.getName().toLowerCase().contains(q)
                    || (p.getBarcode() != null && p.getBarcode().contains(q))
                    || (p.getSku() != null && p.getSku().toLowerCase().contains(q)))
                .limit(50)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Optional<Product> findByBarcode(String barcode) {
        try {
            List<Product> cached = getCache();
            if (cached != null) {
                return cached.stream()
                    .filter(p -> barcode.equals(p.getBarcode()) && p.isActive())
                    .findFirst();
            }
            return repo.findByBarcode(barcode);
        } catch (Exception e) { return Optional.empty(); }
    }

    public Optional<Product> findByQrCode(String qrCode) {
        try {
            List<Product> cached = getCache();
            if (cached != null) {
                return cached.stream()
                    .filter(p -> qrCode.equals(p.getQrCode()) && p.isActive())
                    .findFirst();
            }
            return repo.findByQrCode(qrCode);
        } catch (Exception e) { return Optional.empty(); }
    }

    public Optional<Product> findById(String id) {
        try { return repo.findById(id); } catch (Exception e) { return Optional.empty(); }
    }

    public void saveProduct(Product p, String userId) throws Exception {
        if (p.getSku() == null || p.getSku().isBlank()) p.setSku(generateSku(p.getName()));
        validateProduct(p);
        boolean isNew = (p.getId() == null || p.getId().isBlank());
        if (isNew) {
            p.setId(java.util.UUID.randomUUID().toString());
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            p.setSyncStatus("PENDING");
            if (p.getStatus() == null || p.getStatus().isBlank()) p.setStatus("active");
            if (p.getVersion() == 0) p.setVersion(1);
            repo.insert(p);
            AuditLogger.log(userId, AuditLogger.PRODUCT_CREATED, p.getId(), "name=" + p.getName());
        } else {
            p.setUpdatedAt(LocalDateTime.now());
            p.setSyncStatus("MODIFIED");
            repo.update(p);
            AuditLogger.log(userId, AuditLogger.PRODUCT_UPDATED, p.getId(), "name=" + p.getName());
        }
        invalidateCache();
    }

    public String generateSku(String productName) {
        String prefix = productName == null ? "PRD" : productName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        prefix = (prefix + "PRD").substring(0, 3);
        return prefix + "-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"))
            + "-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void validateProduct(Product p) throws Exception {
        if (p.getName() == null || p.getName().isBlank()) throw new Exception("Product name is required");
        if (p.getSku() == null || p.getSku().isBlank()) throw new Exception("SKU is required");
        if (p.getSellingPrice() < 0.01) throw new Exception("Selling price must be at least 0.01");
        if (p.getName().length() > 255) throw new Exception("Name cannot exceed 255 characters");
        if (p.getSku().length() > 50) throw new Exception("SKU cannot exceed 50 characters");
        // Treat empty string as null to avoid spurious unique violations
        if (p.getBarcode() != null && p.getBarcode().isBlank()) p.setBarcode(null);
        if (p.getQrCode()  != null && p.getQrCode().isBlank())  p.setQrCode(null);
        if (p.getBarcode() != null && p.getBarcode().length() > 50) throw new Exception("Barcode cannot exceed 50 characters");
        if (p.getQrCode()  != null && p.getQrCode().length() > 100) throw new Exception("QR Code cannot exceed 100 characters");
        // Check duplicate barcode
        if (p.getBarcode() != null) {
            Optional<Product> existing = repo.findByBarcode(p.getBarcode());
            if (existing.isPresent() && !existing.get().getId().equals(p.getId())) {
                throw new Exception("Barcode '" + p.getBarcode() + "' is already used by: " + existing.get().getName());
            }
        }
        // Check duplicate QR
        if (p.getQrCode() != null) {
            Optional<Product> existing = repo.findByQrCode(p.getQrCode());
            if (existing.isPresent() && !existing.get().getId().equals(p.getId())) {
                throw new Exception("QR Code is already used by: " + existing.get().getName());
            }
        }
    }

    public void deleteProduct(String id, String userId) throws Exception {
        repo.softDelete(id);
        AuditLogger.log(userId, AuditLogger.PRODUCT_DELETED, id, "");
        invalidateCache();
    }

    public void deactivate(String id, String userId) throws Exception {
        Optional<Product> opt = repo.findById(id);
        if (opt.isEmpty()) throw new Exception("Product not found");
        Product p = opt.get();
        p.setStatus("inactive");
        p.setSyncStatus("MODIFIED");
        repo.update(p);
        invalidateCache();
    }

    /** Return ALL active products (for full catalogue display) — bypasses cache TTL */
    public List<Product> getAllActive() {
        try { return repo.findActive(); } catch (Exception e) { return Collections.emptyList(); }
    }

    /** Warm the cache in the background (call on app startup / panel open). */
    public void warmCache() {
        if (getCache() != null) return; // already warm
        refreshCache();
    }

    public List<Product> getLowStock() {
        try { return repo.findLowStock(); } catch (Exception e) { return Collections.emptyList(); }
    }

    public List<Product> getAll(int limit, int offset) {
        try { return repo.findAll(limit, offset); } catch (Exception e) { return Collections.emptyList(); }
    }

    public void decrementStock(String productId, int qty, boolean adminOverride) throws Exception {
        Optional<Product> opt = repo.findById(productId);
        if (opt.isEmpty()) throw new Exception("Product not found: " + productId);
        Product p = opt.get();
        int newStock = p.getCurrentStock() - qty;
        if (newStock < 0 && !adminOverride) {
            throw new StockException("Insufficient stock for '" + p.getName() +
                "'. Available: " + p.getCurrentStock() + ", required: " + qty);
        }
        repo.updateStock(productId, newStock);
        invalidateCache();
    }

    public void incrementStock(String productId, int qty) throws Exception {
        Optional<Product> opt = repo.findById(productId);
        if (opt.isEmpty()) throw new Exception("Product not found: " + productId);
        Product p = opt.get();
        repo.updateStock(productId, p.getCurrentStock() + qty);
        invalidateCache();
    }

    public double getTotalStockValue() {
        try { return repo.getTotalStockValue(); } catch (Exception e) { return 0; }
    }

    public int countPendingSync() {
        try { return repo.countPendingSync(); } catch (Exception e) { return 0; }
    }

    public static class StockException extends Exception {
        public StockException(String message) { super(message); }
    }
}
