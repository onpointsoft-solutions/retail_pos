# Sync Logic Bottleneck Analysis

## Overview
Analysis of the sync logic between Java desktop client (SyncService.java) and PHP backend (SyncController.php) to identify performance bottlenecks.

## Java Client Bottlenecks (SyncService.java)

### 1. Sequential Entity Processing
**Location:** Lines 139-150
```java
for (String entity : ENTITIES) {
    uploaded += uploadEntity(apiUrl, token, entity);
}
for (String entity : ENTITIES) {
    downloaded += downloadEntity(apiUrl, token, entity, since);
}
```
**Issue:** Entities are processed sequentially rather than in parallel. With 9 entities, this creates a linear bottleneck.

**Impact:** High - Multiplies sync time by number of entities

### 2. Image Processing During Upload
**Location:** Lines 385-401
```java
private void attachImageBlobs(Map<String, Object> product) {
    // Base64 encodes images up to 5MB each
    image.put("content", Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
}
```
**Issue:** 
- Base64 encoding increases payload size by ~33%
- Images read into memory entirely before encoding
- No image compression or resizing
- 5MB limit per image, but multiple images per product

**Impact:** High - Significantly increases upload bandwidth and memory usage

### 3. Sequential Image Download
**Location:** Lines 403-424
```java
private void cacheProductImages(String apiUrl, String token, List<Map<String, Object>> products) {
    for (Map<String, Object> product : products) {
        for (String path : value.toString().split(";")) {
            HttpURLConnection connection = openConn(url, token, "GET");
            // Downloads each image sequentially
        }
    }
}
```
**Issue:** Downloads images one at a time with HTTP connection overhead per image.

**Impact:** Medium - Slows down sync when many products have images

### 4. Individual Record Processing
**Location:** Lines 490-568
```java
private void upsertRecords(String entityType, List<Map<String, Object>> records) {
    for (Map<String, Object> rec : records) {
        // Processes each record individually
        // Checks existence, compares timestamps, performs INSERT/UPDATE
    }
}
```
**Issue:** Each record requires:
- Database query to check existence (line 497-506)
- Timestamp comparison
- Individual INSERT/UPDATE statement

**Impact:** High - O(n) database queries for n records

### 5. Child Record Fetching Per Parent
**Location:** Lines 361-383
```java
private void attachChildRows(Map<String, Object> parent, String childTable, String foreignKey) {
    // Executes separate query for each parent record
    PreparedStatement ps = c.prepareStatement("SELECT * FROM " + childTable + " WHERE " + foreignKey + "=?");
}
```
**Issue:** Fetches child records (sale_items, purchase_order_items) with one query per parent record.

**Impact:** Medium - N+1 query problem for child records

### 6. Column Metadata Queries
**Location:** Lines 619-629
```java
private Set<String> getLocalColumns(String table) {
    // Executes PRAGMA table_info for each entity type
    PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + table + ")");
}
```
**Issue:** Called for each entity type during sync, could be cached.

**Impact:** Low - Minor overhead but repeated unnecessarily

### 7. HTTP Connection Overhead
**Location:** Lines 646-658
```java
private HttpURLConnection openConn(String url, String token, String method) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    // Creates new connection for each request
}
```
**Issue:** No connection pooling, creates new HTTP connection per request.

**Impact:** Medium - TCP handshake overhead for each API call

### 8. Small Batch Sizes
**Location:** Lines 46-50
```java
private static final int UPLOAD_BATCH_SIZE = 500;
private static final int DOWNLOAD_BATCH_SIZE = 1000;
```
**Issue:** Fixed batch sizes may not be optimal for all network conditions.

**Impact:** Medium - Could be larger for faster connections

## PHP Backend Bottlenecks (SyncController.php)

### 1. Sequential Record Processing in Upload
**Location:** Lines 109-134
```php
foreach ($records as $record) {
    $result = $this->upsertRecord($table, $fields, $record, $entityType);
}
```
**Issue:** Processes each record individually with no bulk operations.

**Impact:** High - O(n) database operations for n records

### 2. Transaction Per Record
**Location:** Lines 340-389
```php
private function upsertRecord(...) {
    $this->db->beginTransaction();
    try {
        // Single record operations
        $this->db->commit();
    }
}
```
**Issue:** Transaction overhead for each individual record.

**Impact:** High - Transaction commit overhead per record

### 3. Image Storage Sequential Processing
**Location:** Lines 145-162
```php
private function storeProductImages(string $productId, array $images) {
    foreach (array_slice($images, 0, 8) as $index => $image) {
        $bytes = base64_decode(...);
        file_put_contents($directory . '/' . $filename, $bytes);
    }
}
```
**Issue:** Stores images sequentially with file I/O for each.

**Impact:** Medium - File I/O blocking

### 4. Individual Conflict Resolution
**Location:** Lines 332-338
```php
if ($serverRecord && isset($record['updated_at']) && isset($serverRecord['updated_at'])) {
    $clientTime = strtotime($record['updated_at']);
    $serverTime = strtotime($serverRecord['updated_at']);
    if ($serverTime > $clientTime) {
        return 'conflict';
    }
}
```
**Issue:** Timestamp comparison for each record individually.

**Impact:** Low - Minor CPU overhead

### 5. Child Record Replacement
**Location:** Lines 394-442
```php
private function replaceSaleItems(string $saleId, array $items) {
    $delete = $this->db->prepare('DELETE FROM sale_items WHERE sale_id = ?');
    $delete->execute([$saleId]);
    foreach ($items as $item) {
        $stmt->execute([...]); // Individual INSERT per item
    }
}
```
**Issue:** 
- DELETE + multiple INSERTs per parent record
- No bulk INSERT for child records

**Impact:** High - Multiple database operations per sale/purchase order

### 6. Missing Bulk Operations
**Issue:** No bulk INSERT/UPDATE operations in PHP backend.

**Impact:** High - Could reduce database roundtrips significantly

### 7. No Connection Pooling
**Issue:** Each request creates new database connection (Database.php).

**Impact:** Medium - Connection establishment overhead

## Network Bottlenecks

### 1. Round-Trip Latency
- Multiple API calls per sync cycle (auth + upload per entity + download per entity)
- Each call adds network latency (typically 10-100ms on LAN, 100-500ms on WAN)

### 2. Payload Size
- Base64 encoded images increase payload by 33%
- JSON serialization overhead
- No compression enabled

## Database Bottlenecks

### 1. Missing Indexes
Potential missing indexes on:
- `sync_status` columns (for pending record queries)
- `updated_at` columns (for incremental sync queries)
- Composite indexes for common query patterns

### 2. SQLite Write Locking
- SQLite uses database-level write locks
- Multiple concurrent writes can cause contention

## Priority Recommendations

### High Priority (Major Impact)
1. **Implement bulk operations** in both Java and PHP
2. **Use batch INSERT/UPDATE** instead of per-record operations
3. **Optimize image handling** - compress, resize, or use separate image sync
4. **Parallel entity processing** where possible
5. **Implement connection pooling** for HTTP and database

### Medium Priority (Moderate Impact)
1. **Cache column metadata** in Java client
2. **Use bulk child record operations** (batch INSERT for sale_items)
3. **Increase batch sizes** based on network conditions
4. **Add database indexes** on sync_status and updated_at
5. **Implement request compression** (gzip)

### Low Priority (Minor Impact)
1. **Optimize timestamp comparisons** - cache parsed values
2. **Reuse HTTP connections** with keep-alive
3. **Add sync progress metrics** for monitoring
4. **Implement delta sync** for large tables

## Estimated Performance Gains

Implementing high-priority optimizations could reduce sync time by:
- **Bulk operations**: 60-80% reduction in database roundtrips
- **Image optimization**: 50-70% reduction in bandwidth
- **Parallel processing**: 40-60% reduction for multi-entity sync
- **Connection pooling**: 10-20% reduction in connection overhead

**Overall potential improvement**: 70-90% faster sync cycles
