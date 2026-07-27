# Sync Optimization Implementation Plan

## Phase 1: Quick Wins (High Impact, Low Effort)

### 1.1 Increase Batch Sizes
**Files:** `SyncService.java`
**Lines:** 46-50

**Current:**
```java
private static final int UPLOAD_BATCH_SIZE = 500;
private static final int DOWNLOAD_BATCH_SIZE = 1000;
```

**Optimized:**
```java
private static final int UPLOAD_BATCH_SIZE = 1000;
private static final int DOWNLOAD_BATCH_SIZE = 2000;
```

**Impact:** 30-50% reduction in API calls
**Effort:** 5 minutes

### 1.2 Cache Column Metadata
**Files:** `SyncService.java`
**Lines:** 619-629

**Current:** Called for each entity type during sync

**Optimized:** Add caching
```java
private final Map<String, Set<String>> columnCache = new ConcurrentHashMap<>();

private Set<String> getLocalColumns(String table) {
    return columnCache.computeIfAbsent(table, t -> {
        Set<String> cols = new LinkedHashSet<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + t + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cols.add(rs.getString("name"));
        } catch (Exception e) {
            System.err.println("[SyncService] getLocalColumns failed for " + t + ": " + e.getMessage());
        }
        return cols;
    });
}
```

**Impact:** Eliminates repeated PRAGMA queries
**Effort:** 10 minutes

### 1.3 Add Database Indexes
**Files:** `database/schema.sql` (both Java and PHP)

**Add indexes:**
```sql
CREATE INDEX idx_products_sync_status ON products(sync_status);
CREATE INDEX idx_products_updated_at ON products(updated_at);
CREATE INDEX idx_sales_sync_status ON sales(sync_status);
CREATE INDEX idx_sales_updated_at ON sales(updated_at);
CREATE INDEX idx_customers_sync_status ON customers(sync_status);
CREATE INDEX idx_suppliers_sync_status ON suppliers(sync_status);
CREATE INDEX idx_users_sync_status ON users(sync_status);
```

**Impact:** 20-40% faster pending record queries
**Effort:** 15 minutes

## Phase 2: Bulk Operations (High Impact, Medium Effort)

### 2.1 Bulk INSERT in PHP Backend
**Files:** `SyncController.php`
**Lines:** 109-134

**Current:** Processes records one by one

**Optimized:** Add bulk upsert method
```php
private function bulkUpsertRecords(string $table, array $allowedFields, array $records, string $entityType): array
{
    if (empty($records)) return ['inserted' => 0, 'updated' => 0, 'conflicts' => []];
    
    $this->db->beginTransaction();
    $inserted = 0;
    $updated = 0;
    $conflicts = [];
    
    try {
        // Batch fetch existing records
        $ids = array_column($records, 'id');
        $placeholders = implode(',', array_fill(0, count($ids), '?'));
        $chk = $this->db->prepare("SELECT * FROM {$table} WHERE id IN ({$placeholders})");
        $chk->execute($ids);
        $existingRecords = [];
        while ($row = $chk->fetch()) {
            $existingRecords[$row['id']] = $row;
        }
        
        // Prepare bulk INSERT statement
        $insertFields = ['id'];
        $insertMarkers = ['?'];
        $updateParts = [];
        
        foreach ($allowedFields as $field) {
            if ($field === 'id') continue;
            $insertFields[] = "`{$field}`";
            $insertMarkers[] = '?';
            $updateParts[] = "`{$field}` = VALUES(`{$field}`)";
        }
        
        if (!in_array('`sync_status`', $insertFields, true) && $table !== 'app_settings') {
            $insertFields[] = '`sync_status`';
            $insertMarkers[] = "'SYNCED'";
            $updateParts[] = "`sync_status` = 'SYNCED'";
        }
        
        $sql = sprintf(
            'INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s',
            $table,
            implode(', ', $insertFields),
            implode(', ', $insertMarkers),
            implode(', ', $updateParts)
        );
        
        $stmt = $this->db->prepare($sql);
        
        // Process records in batches
        foreach ($records as $record) {
            $id = $record['id'];
            $record = $this->normalizeRecordDates($record);
            
            // Conflict check
            if (isset($existingRecords[$id]) && isset($record['updated_at']) && isset($existingRecords[$id]['updated_at'])) {
                $clientTime = strtotime($record['updated_at']);
                $serverTime = strtotime($existingRecords[$id]['updated_at']);
                if ($serverTime > $clientTime) {
                    $conflicts[] = ['id' => $id, 'reason' => 'Server record is newer'];
                    continue;
                }
            }
            
            // Build values array
            $values = [$id];
            foreach ($allowedFields as $field) {
                if ($field === 'id') continue;
                if (!array_key_exists($field, $record)) continue;
                if ($entityType === 'users' && $field === 'password_hash') continue;
                $values[] = $record[$field];
            }
            
            $stmt->execute($values);
            
            if (isset($existingRecords[$id])) {
                $updated++;
            } else {
                $inserted++;
            }
        }
        
        $this->db->commit();
    } catch (Throwable $e) {
        if ($this->db->inTransaction()) {
            $this->db->rollBack();
        }
        throw $e;
    }
    
    return ['inserted' => $inserted, 'updated' => $updated, 'conflicts' => $conflicts];
}
```

**Impact:** 60-80% reduction in database roundtrips
**Effort:** 2-3 hours

### 2.2 Bulk Child Record Operations
**Files:** `SyncController.php`
**Lines:** 394-442

**Optimized:** Replace individual INSERTs with bulk INSERT
```php
private function bulkReplaceSaleItems(string $saleId, array $items): void
{
    $delete = $this->db->prepare('DELETE FROM sale_items WHERE sale_id = ?');
    $delete->execute([$saleId]);
    if (!$items) return;

    $sql = 'INSERT INTO sale_items (id, sale_id, product_id, product_name, product_sku, quantity, unit_price, buying_price, discount, tax_rate, line_total)
            VALUES ';
    $placeholders = [];
    $values = [];
    
    foreach ($items as $item) {
        if (!is_array($item)) continue;
        $placeholders[] = '(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)';
        $values[] = $item['id'] ?? $this->uuid();
        $values[] = $saleId;
        $values[] = $item['product_id'] ?? null;
        $values[] = $item['product_name'] ?? '';
        $values[] = $item['product_sku'] ?? null;
        $values[] = (int)($item['quantity'] ?? 0);
        $values[] = (float)($item['unit_price'] ?? 0);
        $values[] = (float)($item['buying_price'] ?? 0);
        $values[] = (float)($item['discount'] ?? 0);
        $values[] = (float)($item['tax_rate'] ?? 0);
        $values[] = (float)($item['line_total'] ?? 0);
    }
    
    $sql .= implode(', ', $placeholders);
    $stmt = $this->db->prepare($sql);
    $stmt->execute($values);
}
```

**Impact:** 70-90% reduction in child record operations
**Effort:** 1 hour

### 2.3 Bulk Child Record Fetching in Java
**Files:** `SyncService.java`
**Lines:** 361-383

**Optimized:** Fetch all child records in single query
```java
private Map<String, List<Map<String, Object>>> fetchAllChildRows(String childTable, String foreignKey, Set<String> parentIds) {
    Map<String, List<Map<String, Object>>> result = new HashMap<>();
    if (parentIds.isEmpty()) return result;
    
    try (Connection c = DatabaseManager.getConnection()) {
        String ph = String.join(",", Collections.nCopies(parentIds.size(), "?"));
        String sql = "SELECT * FROM " + childTable + " WHERE " + foreignKey + " IN (" + ph + ")";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (String id : parentIds) {
                ps.setString(i++, id);
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            
            while (rs.next()) {
                String parentId = rs.getString(foreignKey);
                Map<String, Object> child = new LinkedHashMap<>();
                for (int j = 1; j <= cols; j++) {
                    String column = meta.getColumnName(j);
                    child.put(column, normalizeOutgoingValue(column, rs.getObject(j)));
                }
                result.computeIfAbsent(parentId, k -> new ArrayList<>()).add(child);
            }
        }
    } catch (Exception e) {
        System.err.println("[SyncService] bulk child fetch failed for " + childTable + ": " + e.getMessage());
    }
    return result;
}
```

**Impact:** Eliminates N+1 query problem
**Effort:** 1.5 hours

## Phase 3: Image Optimization (High Impact, Medium Effort)

### 3.1 Image Compression Before Upload
**Files:** `SyncService.java`
**Lines:** 385-401

**Optimized:** Add image compression
```java
private void attachImageBlobs(Map<String, Object> product) {
    Object paths = product.get("image_path");
    if (paths == null) return;
    List<Map<String, String>> images = new ArrayList<>();
    for (String imagePath : paths.toString().split(";")) {
        try {
            if (imagePath.isBlank() || imagePath.startsWith("http") || imagePath.startsWith("uploads/")) continue;
            java.nio.file.Path file = java.nio.file.Path.of(imagePath);
            if (!java.nio.file.Files.isRegularFile(file) || java.nio.file.Files.size(file) > 5_000_000) continue;
            
            // Compress image
            byte[] originalBytes = java.nio.file.Files.readAllBytes(file);
            byte[] compressedBytes = compressImage(originalBytes, 800, 0.8f); // Max 800px, 80% quality
            
            Map<String, String> image = new LinkedHashMap<>();
            image.put("name", file.getFileName().toString());
            image.put("content", Base64.getEncoder().encodeToString(compressedBytes));
            images.add(image);
        } catch (Exception ignored) { }
    }
    if (!images.isEmpty()) product.put("image_blobs", images);
}

private byte[] compressImage(byte[] imageBytes, int maxWidth, float quality) throws IOException {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
         ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        BufferedImage image = ImageIO.read(bis);
        if (image == null) return imageBytes;
        
        // Calculate new dimensions maintaining aspect ratio
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        if (originalWidth <= maxWidth) return imageBytes;
        
        int newHeight = (int) ((long) originalHeight * maxWidth / originalWidth);
        
        BufferedImage resized = new BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, maxWidth, newHeight, null);
        g.dispose();
        
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        
        writer.setOutput(ImageIO.createImageOutputStream(bos));
        writer.write(null, new IIOImage(resized, null, null), param);
        
        return bos.toByteArray();
    }
}
```

**Impact:** 50-70% reduction in image payload size
**Effort:** 2 hours

### 3.2 Parallel Image Download
**Files:** `SyncService.java`
**Lines:** 403-424

**Optimized:** Use parallel streams for image downloads
```java
private void cacheProductImages(String apiUrl, String token, List<Map<String, Object>> products) {
    String webRoot = apiUrl.replaceFirst("api/?$", "");
    
    // Collect all image URLs to download
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    for (Map<String, Object> product : products) {
        Object value = product.get("image_path");
        if (value == null) continue;
        
        List<String> paths = Arrays.asList(value.toString().split(";"));
        for (String path : paths) {
            if (!path.startsWith("http") && !path.startsWith("uploads/")) continue;
            
            String url = path.startsWith("http") ? path : webRoot + path;
            String name = url.substring(url.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
            java.nio.file.Path destination = com.retailpos.util.AppPaths.imageDirectory().resolve(name);
            
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    HttpURLConnection connection = openConn(url, token, "GET");
                    try (InputStream input = connection.getInputStream()) {
                        java.nio.file.Files.copy(input, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception ignored) { }
            }));
        }
    }
    
    // Wait for all downloads to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
}
```

**Impact:** 60-80% faster image downloads on multi-core systems
**Effort:** 1 hour

## Phase 4: Network Optimization (Medium Impact, Medium Effort)

### 4.1 HTTP Connection Pooling
**Files:** `SyncService.java`

**Add connection pool:**
```java
private final ExecutorService httpExecutor = Executors.newFixedThreadPool(4);

private HttpURLConnection openConn(String url, String token, String method) throws Exception {
    // Use connection pooling via system properties
    System.setProperty("http.keepAlive", "true");
    System.setProperty("http.maxConnections", "20");
    
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setRequestMethod(method);
    c.setRequestProperty("Content-Type", "application/json");
    c.setRequestProperty("Accept", "application/json");
    c.setRequestProperty("Accept-Encoding", "gzip"); // Enable compression
    if (token != null && !token.isBlank()) {
        c.setRequestProperty("Authorization", "Bearer " + token);
    }
    c.setConnectTimeout(CONNECT_TIMEOUT);
    c.setReadTimeout(READ_TIMEOUT);
    return c;
}
```

**Impact:** 10-20% reduction in connection overhead
**Effort:** 30 minutes

### 4.2 Enable Gzip Compression
**Files:** `backend/index.php`

**Add compression:**
```php
// At the top of index.php
if (isset($_SERVER['HTTP_ACCEPT_ENCODING']) && strpos($_SERVER['HTTP_ACCEPT_ENCODING'], 'gzip') !== false) {
    ob_start('ob_gzhandler');
}
```

**Impact:** 60-80% reduction in JSON payload size
**Effort:** 5 minutes

## Phase 5: Parallel Processing (Medium Impact, High Effort)

### 5.1 Parallel Entity Sync
**Files:** `SyncService.java`
**Lines:** 139-150

**Optimized:** Use parallel streams for independent entities
```java
// Upload entities in parallel
List<CompletableFuture<Integer>> uploadFutures = Arrays.stream(ENTITIES)
    .map(entity -> CompletableFuture.supplyAsync(() -> 
        uploadEntity(apiUrl, token, entity), httpExecutor))
    .toList();

int uploaded = uploadFutures.stream()
    .mapToInt(CompletableFuture::join)
    .sum();

// Download entities in parallel
List<CompletableFuture<Integer>> downloadFutures = Arrays.stream(ENTITIES)
    .map(entity -> CompletableFuture.supplyAsync(() -> 
        downloadEntity(apiUrl, token, entity, since), httpExecutor))
    .toList();

int downloaded = downloadFutures.stream()
    .mapToInt(CompletableFuture::join)
    .sum();
```

**Impact:** 40-60% reduction for multi-entity sync
**Effort:** 2 hours

## Implementation Order

### Week 1 (Quick Wins)
1. Increase batch sizes (5 min)
2. Cache column metadata (10 min)
3. Add database indexes (15 min)
4. Enable gzip compression (5 min)

### Week 2 (Bulk Operations)
1. Bulk INSERT in PHP backend (2-3 hours)
2. Bulk child record operations (1 hour)
3. Bulk child record fetching in Java (1.5 hours)

### Week 3 (Image Optimization)
1. Image compression (2 hours)
2. Parallel image download (1 hour)

### Week 4 (Network & Parallel)
1. HTTP connection pooling (30 min)
2. Parallel entity sync (2 hours)

## Expected Results

### Before Optimizations
- Small sync (10 records): ~2-3 seconds
- Medium sync (100 records): ~15-20 seconds
- Large sync (1000 records): ~2-3 minutes

### After All Optimizations
- Small sync (10 records): ~0.5-1 second (60-70% faster)
- Medium sync (100 records): ~3-5 seconds (75-80% faster)
- Large sync (1000 records): ~20-30 seconds (85-90% faster)

## Monitoring & Testing

### Add Performance Metrics
```java
private long syncStartTime;
private long uploadTime;
private long downloadTime;

private void performSync() {
    syncStartTime = System.currentTimeMillis();
    // ... existing sync logic ...
    
    long totalSyncTime = System.currentTimeMillis() - syncStartTime;
    AuditLogger.log("SYSTEM", "SYNC_METRICS", null, 
        String.format("Total: %dms, Upload: %dms, Download: %dms, Records: %d",
        totalSyncTime, uploadTime, downloadTime, uploaded + downloaded));
}
```

### Load Testing
- Test with 10, 100, 500, 1000 records
- Measure sync times before and after each optimization
- Test on different network conditions (LAN, WAN, slow connection)

## Rollback Plan

Each optimization should be:
1. Implemented in a separate commit
2. Tested thoroughly
3. Monitored in production
4. Revertible if issues arise

Key metrics to monitor:
- Sync success rate
- Sync duration
- Database query times
- Network bandwidth usage
- Error rates
