package com.retailpos.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.retailpos.model.AppSettings;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.util.AuditLogger;
import com.retailpos.util.DatabaseManager;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background sync service — auto-authenticates with the PHP backend,
 * uploads PENDING/MODIFIED/DELETED records, then downloads changes.
 */
public class SyncService {

    public enum SyncState { IDLE, SYNCING, ERROR }

    private static SyncService instance;
    private volatile SyncState     state        = SyncState.IDLE;
    private volatile String        stateMessage = "Ready";
    private volatile LocalDateTime lastSyncTime = null;

    // Cached JWT from backend login (refreshed if expired)
    private volatile String  backendToken       = null;
    private volatile long    tokenExpiresAtMs   = 0;

    private ScheduledExecutorService scheduler;
    private final List<SyncStateListener> listeners    = new CopyOnWriteArrayList<>();
    private final SettingsRepository      settingsRepo = new SettingsRepository();
    private final Gson gson = new GsonBuilder().create();
    private final Map<String, Set<String>> columnCache = new ConcurrentHashMap<>();

    private int retryCount = 0;
    private static final int MAX_RETRIES     = 5;
    private static final int POLL_INTERVAL_S = 30;
    private static final int CONNECT_TIMEOUT = 6_000;
    private static final int READ_TIMEOUT    = 120_000;
    private static final int UPLOAD_BATCH_SIZE = 1000;
    private static final int DOWNLOAD_BATCH_SIZE = 2000;
    private static final DateTimeFormatter SQL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter UTC_SQL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final String[] SYNC_ORDER = {
        "categories", "suppliers", "customers", "users", "products",
        "product_images", "purchase_orders", "sales", "inventory_movements",
        "mpesa_transactions"
    };

    private SyncService() {}

    public static synchronized SyncService getInstance() {
        if (instance == null) instance = new SyncService();
        return instance;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RetailPOS-Sync");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkAndSync, 10, POLL_INTERVAL_S, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
        setState(SyncState.IDLE, "Sync stopped");
    }

    public void triggerSync() {
        Thread t = new Thread(this::performSync, "RetailPOS-Sync-Manual");
        t.setDaemon(true); t.start();
    }

    public SyncState     getState()        { return state; }
    public String        getStateMessage() { return stateMessage; }
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }

    public void addStateListener(SyncStateListener l)    { listeners.add(l); }
    public void removeStateListener(SyncStateListener l) { listeners.remove(l); }

    // ── Core logic ────────────────────────────────────────────────────────────

    private void checkAndSync() {
        if (state == SyncState.SYNCING) return;
        try {
            AppSettings s = settingsRepo.load();
            if (!s.isAutoSync()) return;
            if (isOnline(s.getSyncApiUrl())) {
                retryCount = 0;
                performSync();
            } else {
                setState(SyncState.IDLE, "Offline");
            }
        } catch (Exception e) {
            setState(SyncState.IDLE, "Config error");
        }
    }

    @SuppressWarnings("unchecked")
    private void performSync() {
        if (state == SyncState.SYNCING) return;
        setState(SyncState.SYNCING, "Synchronising…");
        try {
            AppSettings settings = settingsRepo.load();
            String apiUrl  = normalizeApiUrl(settings.getSyncApiUrl());
            if (apiUrl == null || apiUrl.isBlank()) {
                setState(SyncState.IDLE, "Sync API URL not configured");
                return;
            }
            if (usesLocalhost(apiUrl)) {
                setState(SyncState.ERROR, "Use the backend computer IP, not localhost, for multi-computer sync");
                return;
            }

            // ── Step 1: Authenticate (or skip if no-auth mode) ────────────────
            String token = getOrRefreshToken(apiUrl, settings);
            if (token == null) {
                // getOrRefreshToken already set the error state (401/429)
                return;
            }
            // token == "" means no-auth mode (REQUIRE_AUTH=false on server)
            String nextSyncCursor = fetchServerCursor(apiUrl, token);

            // ── Step 2: Upload pending records in parallel ─────────────────────
            int uploaded = 0;
            for (String entity : SYNC_ORDER) {
                uploaded += uploadEntity(apiUrl, token, entity);
            }

            // ── Step 3: Download changes from server in parallel ──────────────
            String since = !isBlank(settings.getLastSuccessfulSync())
                ? settings.getLastSuccessfulSync()
                : "2000-01-01 00:00:00";

            int downloaded = 0;
            for (String entity : SYNC_ORDER) {
                downloaded += downloadEntity(apiUrl, token, entity, since);
            }

            lastSyncTime = LocalDateTime.now();
            settings.setLastSuccessfulSync(nextSyncCursor);
            try { settingsRepo.save(settings); } catch (Exception ignored) {}
            String msg = "Synced " + lastSyncTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (uploaded   > 0) msg += " | up: "   + uploaded;
            if (downloaded > 0) msg += " | down: " + downloaded;
            setState(SyncState.IDLE, msg);
            retryCount = 0;

        } catch (Exception e) {
            retryCount++;
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            AuditLogger.log("SYSTEM", "SYNC_ERROR", null, msg);
            if (retryCount >= MAX_RETRIES) {
                setState(SyncState.ERROR, "Sync failed after " + MAX_RETRIES + " attempts");
                retryCount = 0;
            } else {
                setState(SyncState.ERROR, "Sync error (retry " + retryCount + "): " + msg);
            }
        }
    }

    // ── Authentication ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String getOrRefreshToken(String apiUrl, AppSettings settings) {
        // 1. Use in-memory token if still valid (with 60 s buffer)
        if (backendToken != null && System.currentTimeMillis() < tokenExpiresAtMs - 60_000L) {
            return backendToken;
        }

        // 2. Try stored token from settings (e.g. manually pasted)
        String stored = settings.getSyncApiToken();
        if (stored != null && !stored.isBlank()) {
            backendToken     = stored;
            tokenExpiresAtMs = System.currentTimeMillis() + 23L * 3600 * 1000;
            return backendToken;
        }

        // 3. No credentials and no stored token → try unauthenticated (backend may allow it)
        String username = settings.getSyncApiUsername();
        String password = settings.getSyncApiPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            // Return empty string — SyncService will send requests without Authorization header.
            // The backend will succeed if REQUIRE_AUTH=false, or fail with 401 if it requires auth.
            System.out.println("[SyncService] No credentials configured — attempting unauthenticated sync.");
            return ""; // empty = no header sent
        }

        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("username", username);
            body.put("password", password);
            String response = post(apiUrl + "auth/login", null, gson.toJson(body));

            Map<String, Object> result = gson.fromJson(response, Map.class);
            String token = (String) result.get("access_token");
            Number expiresIn = (Number) result.getOrDefault("expires_in", 86400);

            if (token != null && !token.isBlank()) {
                backendToken     = token;
                tokenExpiresAtMs = System.currentTimeMillis() + (expiresIn.longValue() * 1000L);
                // Persist so next run skips re-login
                settings.setSyncApiToken(token);
                try { settingsRepo.save(settings); } catch (Exception ignored) {}
                System.out.println("[SyncService] Authenticated successfully as: " + username);
                return backendToken;
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // 401 = wrong password → stop retrying, tell user to fix credentials
            if (msg.contains("401")) {
                setState(SyncState.ERROR,
                    "Sync login failed: wrong username or password. Fix in Settings > Sync.");
                retryCount = MAX_RETRIES; // prevent further retry this cycle
            }
            // 429 = account locked → stop retrying until lock expires
            else if (msg.contains("429")) {
                setState(SyncState.ERROR,
                    "Sync account locked (too many failed attempts). Wait 15 min or reset in DB.");
                retryCount = MAX_RETRIES;
            } else {
                System.err.println("[SyncService] Login failed: " + msg);
            }
        }
        return null;
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int uploadEntity(String apiUrl, String token, String entityType) {
        int uploaded = 0;
        while (true) {
            List<Map<String, Object>> pending = fetchPendingRecords(entityType, UPLOAD_BATCH_SIZE);
            if (pending.isEmpty()) return uploaded;

            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("entity_type", entityType);
                body.put("records", pending);

                String response = post(apiUrl + "sync/upload", token, gson.toJson(body));

                if (!response.trim().startsWith("{")) {
                    System.err.println("[SyncService] Non-JSON response for upload/" + entityType
                        + ": " + response.substring(0, Math.min(200, response.length())));
                    throw new IllegalStateException("Backend returned non-JSON for " + entityType);
                }

                Map<String, Object> result = gson.fromJson(response, Map.class);

                List<String> acceptedIds = extractAcceptedIds(result, pending);
                if (!acceptedIds.isEmpty()) {
                    markSynced(entityType, acceptedIds);
                }

                List<Map<String, Object>> conflicts =
                    (List<Map<String, Object>>) result.getOrDefault("conflicts", Collections.emptyList());
                for (Map<String, Object> c : conflicts) {
                    AuditLogger.log("SYSTEM", "SYNC_CONFLICT", String.valueOf(c.get("id")),
                        entityType + ": " + c.getOrDefault("reason", ""));
                }

                uploaded += acceptedIds.size();
                if (pending.size() < UPLOAD_BATCH_SIZE || acceptedIds.isEmpty()) return uploaded;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("401")) {
                    backendToken = null; tokenExpiresAtMs = 0;
                }
                throw new IllegalStateException("Upload failed for " + entityType + ": " + e.getMessage(), e);
            }
        }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int downloadEntity(String apiUrl, String token, String entityType, String since) {
        try {
            int downloaded = 0;
            int offset = 0;
            while (true) {
                String url = apiUrl + "sync/download/" + entityType + "?since="
                    + URLEncoder.encode(since, StandardCharsets.UTF_8)
                    + "&limit=" + DOWNLOAD_BATCH_SIZE + "&offset=" + offset;
                String response = get(url, token);

                Map<String, Object> result = gson.fromJson(response, Map.class);
                List<Map<String, Object>> records =
                    (List<Map<String, Object>>) result.getOrDefault("records", Collections.emptyList());
                if (records.isEmpty()) return downloaded;
                if ("products".equals(entityType)) cacheProductImages(apiUrl, token, records);

                upsertRecords(entityType, records);
                downloaded += records.size();
                if (records.size() < DOWNLOAD_BATCH_SIZE) return downloaded;
                offset += records.size();
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                backendToken = null; tokenExpiresAtMs = 0;
            }
            throw new IllegalStateException("Download failed for " + entityType + ": " + e.getMessage(), e);
        }
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private List<Map<String, Object>> fetchPendingRecords(String entityType, int limit) {
        Set<String> columns = getLocalColumns(entityType);
        String orderBy = columns.contains("updated_at") ? "updated_at" : columns.contains("created_at") ? "created_at" : "id";
        String sql;
        sql = "SELECT * FROM " + entityType +
              " WHERE sync_status IN ('PENDING','MODIFIED','DELETED') ORDER BY " + orderBy + ", id LIMIT ?";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String column = meta.getColumnName(i);
                    row.put(column, normalizeOutgoingValue(column, rs.getObject(i)));
                }
                if ("products".equals(entityType)) attachImageBlobs(row);
                rows.add(row);
            }
            rs.close();

            // Bulk fetch child records to avoid N+1 query problem
            if ("sales".equals(entityType)) {
                Set<String> saleIds = rows.stream().map(r -> String.valueOf(r.get("id"))).collect(java.util.stream.Collectors.toSet());
                Map<String, List<Map<String, Object>>> allItems = fetchAllChildRows("sale_items", "sale_id", saleIds);
                for (Map<String, Object> row : rows) {
                    String id = String.valueOf(row.get("id"));
                    if (allItems.containsKey(id)) row.put("items", allItems.get(id));
                }
            }
            if ("purchase_orders".equals(entityType)) {
                Set<String> poIds = rows.stream().map(r -> String.valueOf(r.get("id"))).collect(java.util.stream.Collectors.toSet());
                Map<String, List<Map<String, Object>>> allItems = fetchAllChildRows("purchase_order_items", "po_id", poIds);
                for (Map<String, Object> row : rows) {
                    String id = String.valueOf(row.get("id"));
                    if (allItems.containsKey(id)) row.put("items", allItems.get(id));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read pending " + entityType + " records", e);
        }
        return rows;
    }

    private void attachChildRows(Map<String, Object> parent, String childTable, String foreignKey) {
        Object parentId = parent.get("id");
        if (parentId == null) return;
        List<Map<String, Object>> children = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM " + childTable + " WHERE " + foreignKey + "=?")) {
            ps.setString(1, parentId.toString());
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> child = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String column = meta.getColumnName(i);
                    child.put(column, normalizeOutgoingValue(column, rs.getObject(i)));
                }
                children.add(child);
            }
        } catch (Exception e) {
            System.err.println("[SyncService] child fetch failed for " + childTable + ": " + e.getMessage());
        }
        if (!children.isEmpty()) parent.put("items", children);
    }

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
            throw new IllegalStateException("Could not read child records from " + childTable, e);
        }
        return result;
    }

    private void attachImageBlobs(Map<String, Object> product) {
        Object paths = product.get("image_path");
        if (paths == null) return;
        List<Map<String, String>> images = new ArrayList<>();
        for (String imagePath : paths.toString().split(";")) {
            try {
                if (imagePath.isBlank() || imagePath.startsWith("http") || imagePath.startsWith("uploads/")) continue;
                java.nio.file.Path file = java.nio.file.Path.of(imagePath);
                if (!java.nio.file.Files.isRegularFile(file) || java.nio.file.Files.size(file) > 5_000_000) continue;
                Map<String, String> image = new LinkedHashMap<>();
                image.put("name", file.getFileName().toString());
                image.put("content", Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(file)));
                images.add(image);
            } catch (Exception ignored) { }
        }
        if (!images.isEmpty()) product.put("image_blobs", images);
    }

    private void cacheProductImages(String apiUrl, String token, List<Map<String, Object>> products) {
        String webRoot = apiUrl.replaceFirst("api/?$", "");

        // Collect all image URLs to download
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<String, List<String>> productImageMap = new HashMap<>();
        AtomicBoolean imageDownloadFailed = new AtomicBoolean(false);

        for (Map<String, Object> product : products) {
            Object value = product.get("image_path");
            if (value == null) continue;

            String productId = String.valueOf(product.get("id"));
            List<String> paths = Arrays.asList(value.toString().split(";"));
            productImageMap.put(productId, new ArrayList<>());

            for (String path : paths) {
                if (!path.startsWith("http") && !path.startsWith("uploads/")) {
                    productImageMap.get(productId).add(path);
                    continue;
                }

                String url = path.startsWith("http") ? path : webRoot + path;
                String name = url.substring(url.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");

                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        java.nio.file.Path destination = com.retailpos.util.AppPaths.imageDirectory().resolve(name);
                        HttpURLConnection connection = openConn(url, token, "GET");
                        try (InputStream input = connection.getInputStream()) {
                            java.nio.file.Files.copy(input, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        synchronized (productImageMap) {
                            productImageMap.get(productId).add(destination.toString());
                        }
                    } catch (Exception ignored) {
                        imageDownloadFailed.set(true);
                        synchronized (productImageMap) {
                            productImageMap.get(productId).add(path);
                        }
                    }
                }));
            }
        }

        // Wait for all downloads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        if (imageDownloadFailed.get()) {
            throw new IllegalStateException("One or more product images could not be downloaded");
        }

        // Update product image paths
        for (Map<String, Object> product : products) {
            String productId = String.valueOf(product.get("id"));
            if (productImageMap.containsKey(productId)) {
                product.put("image_path", String.join(";", productImageMap.get(productId)));
            }
        }
    }

    private void markSynced(String table, List<String> ids) {
        if (ids.isEmpty()) return;
        try (Connection c = DatabaseManager.getConnection()) {
            String ph  = String.join(",", Collections.nCopies(ids.size(), "?"));
            String sql = "UPDATE " + table + " SET sync_status='SYNCED' WHERE id IN (" + ph + ")";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) ps.setString(i + 1, ids.get(i));
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[SyncService] markSynced failed: " + e.getMessage());
        }
    }

    private List<String> extractIds(List<Map<String, Object>> records) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> r : records) {
            Object id = r.get("id");
            if (id != null) ids.add(id.toString());
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAcceptedIds(Map<String, Object> result, List<Map<String, Object>> pending) {
        Object accepted = result.get("accepted_ids");
        if (accepted instanceof List<?> list) {
            List<String> ids = new ArrayList<>();
            for (Object id : list) if (id != null) ids.add(id.toString());
            return ids;
        }

        Set<String> blocked = new HashSet<>();
        for (String key : List.of("conflicts", "errors")) {
            Object rows = result.get(key);
            if (rows instanceof List<?> list) {
                for (Object row : list) {
                    if (row instanceof Map<?, ?> map && map.get("id") != null) {
                        blocked.add(map.get("id").toString());
                    }
                }
            }
        }

        List<String> ids = new ArrayList<>();
        for (Map<String, Object> record : pending) {
            Object id = record.get("id");
            if (id != null && !blocked.contains(id.toString())) ids.add(id.toString());
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private void upsertRecords(String entityType, List<Map<String, Object>> records) {
        String table = entityType;
        if (records.isEmpty()) return;

        // Get the local SQLite column set for this table once
        Set<String> localColumns = getLocalColumns(table);
        if (localColumns.isEmpty()) {
            System.err.println("[SyncService] Cannot upsert " + table + ": table not found locally");
            return;
        }

        for (Map<String, Object> rec : records) {
            try {
                Object idObj = rec.get("id");
                if (idObj == null || "null".equals(idObj.toString())) continue;
                String id = idObj.toString();

                // Check if record exists locally and compare timestamps
                String localUpdated = null;
                try (Connection c = DatabaseManager.getConnection();
                     PreparedStatement ps = c.prepareStatement(
                        "SELECT updated_at FROM " + table + " WHERE id=?")) {
                    ps.setString(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) localUpdated = rs.getString(1);
                } catch (Exception ignored) {
                    // Table may not have updated_at (e.g. sale_items)
                }

                // Conflict resolution: keep local if newer
                String serverUpdated = String.valueOf(rec.getOrDefault("updated_at", ""));
                if (localUpdated != null && !serverUpdated.equals("null") && !serverUpdated.isEmpty()) {
                    Instant localInstant = parseFlexibleInstant(localUpdated);
                    Instant serverInstant = parseFlexibleInstant(serverUpdated);
                    if (localInstant != null && serverInstant != null && localInstant.isAfter(serverInstant)) continue;
                }

                // Filter record to only columns that exist in the local SQLite table
                List<String> colList = new ArrayList<>();
                List<Object> vals    = new ArrayList<>();
                for (Map.Entry<String, Object> e : rec.entrySet()) {
                    String col = e.getKey();
                    if (!localColumns.contains(col)) continue; // skip unknown columns
                    colList.add(col);
                    vals.add(normalizeIncomingValue(col, e.getValue()));
                }

                if (colList.isEmpty()) continue;

                if (localUpdated != null) {
                    // UPDATE existing row
                    StringBuilder sb = new StringBuilder("UPDATE " + table + " SET ");
                    for (int i = 0; i < colList.size(); i++) {
                        sb.append("`").append(colList.get(i)).append("`=?");
                        if (i < colList.size() - 1) sb.append(",");
                    }
                    if (localColumns.contains("sync_status") && !colList.contains("sync_status")) sb.append(",sync_status='SYNCED'");
                    sb.append(" WHERE id=?");
                    try (Connection c = DatabaseManager.getConnection();
                         PreparedStatement ps = c.prepareStatement(sb.toString())) {
                        for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
                        ps.setString(vals.size() + 1, id);
                        ps.executeUpdate();
                    }
                } else {
                    // INSERT new row
                    StringBuilder cols = new StringBuilder();
                    StringBuilder phs  = new StringBuilder();
                    for (int i = 0; i < colList.size(); i++) {
                        cols.append("`").append(colList.get(i)).append("`");
                        phs.append("?");
                        if (i < colList.size() - 1) { cols.append(","); phs.append(","); }
                    }
                    if (localColumns.contains("sync_status") && !colList.contains("sync_status")) {
                        cols.append(",sync_status");
                        phs.append(",'SYNCED'");
                    }
                    String sql = "INSERT OR IGNORE INTO " + table + " (" + cols + ") VALUES (" + phs + ")";
                    try (Connection c = DatabaseManager.getConnection();
                         PreparedStatement ps = c.prepareStatement(sql)) {
                        for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
                        ps.executeUpdate();
                    }
                }
                if ("sales".equals(entityType)) upsertChildRecords("sale_items", "sale_id", id, rec.get("items"));
                if ("purchase_orders".equals(entityType)) upsertChildRecords("purchase_order_items", "po_id", id, rec.get("items"));
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Could not apply " + entityType + " record " + rec.get("id"), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void upsertChildRecords(String table, String parentColumn, String parentId, Object rawItems) {
        if (!(rawItems instanceof List<?> items) || items.isEmpty()) return;
        Set<String> localColumns = getLocalColumns(table);
        if (localColumns.isEmpty()) return;

        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement delete = c.prepareStatement("DELETE FROM " + table + " WHERE " + parentColumn + "=?")) {
                    delete.setString(1, parentId);
                    delete.executeUpdate();
                }
                for (Object item : items) {
                    if (!(item instanceof Map<?, ?> raw)) continue;
                    List<String> cols = new ArrayList<>();
                    List<Object> vals = new ArrayList<>();
                    for (Map.Entry<?, ?> entry : raw.entrySet()) {
                        String col = String.valueOf(entry.getKey());
                        if (!localColumns.contains(col)) continue;
                        cols.add(col);
                        vals.add(normalizeIncomingValue(col, entry.getValue()));
                    }
                    if (!cols.contains(parentColumn) && localColumns.contains(parentColumn)) {
                        cols.add(parentColumn);
                        vals.add(parentId);
                    }
                    if (cols.isEmpty()) continue;
                    String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
                    String sql = "INSERT OR REPLACE INTO " + table + " (`" + String.join("`,`", cols) + "`) VALUES (" + placeholders + ")";
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        for (int i = 0; i < vals.size(); i++) ps.setObject(i + 1, vals.get(i));
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not apply child records for " + table, e);
        }
    }

    /** Returns the set of column names that exist in the local SQLite table. */
    private Set<String> getLocalColumns(String table) {
        return columnCache.computeIfAbsent(table, t -> {
            Set<String> cols = new java.util.LinkedHashSet<>();
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

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private String post(String url, String token, String json) throws Exception {
        HttpURLConnection conn = openConn(url, token, "POST");
        conn.setDoOutput(true);
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(b.length));
        try (OutputStream os = conn.getOutputStream()) { os.write(b); }
        return readResponse(conn);
    }

    private String get(String url, String token) throws Exception {
        return readResponse(openConn(url, token, "GET"));
    }

    @SuppressWarnings("unchecked")
    private String fetchServerCursor(String apiUrl, String token) throws Exception {
        Map<String, Object> status = gson.fromJson(get(apiUrl + "sync/status", token), Map.class);
        Instant serverTime = parseFlexibleInstant(String.valueOf(status.get("server_time")));
        if (serverTime == null) {
            throw new IllegalStateException("Backend did not return a valid server time");
        }
        return formatForSqlUtc(serverTime.minusSeconds(2));
    }

    private HttpURLConnection openConn(String url, String token, String method) throws Exception {
        // Enable HTTP connection pooling and keep-alive
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "20");
        System.setProperty("http.maxRedirects", "5");

        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Accept-Encoding", "gzip"); // Enable compression
        // Only send Authorization header when a non-blank token is available
        if (token != null && !token.isBlank()) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        c.setConnectTimeout(CONNECT_TIMEOUT);
        c.setReadTimeout(READ_TIMEOUT);
        return c;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) return "{}";
        
        // Handle gzip decompression
        String encoding = conn.getContentEncoding();
        if (encoding != null && encoding.contains("gzip")) {
            is = new java.util.zip.GZIPInputStream(is);
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String body = sb.toString();
            conn.disconnect();
            if (code >= 400) throw new Exception("HTTP " + code + ": " + body);
            return body;
        }
    }

    private boolean isOnline(String apiUrl) {
        apiUrl = normalizeApiUrl(apiUrl);
        if (apiUrl == null || apiUrl.isBlank() || usesLocalhost(apiUrl)) return false;
        try {
            String healthUrl = apiUrl.endsWith("/") ? apiUrl + "health" : apiUrl + "/health";
            HttpURLConnection c = (HttpURLConnection) new URL(healthUrl).openConnection();
            c.setConnectTimeout(CONNECT_TIMEOUT); c.setReadTimeout(5_000);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            c.disconnect();
            return code < 500;
        } catch (Exception e) { return false; }
    }

    public String testConnection(String apiUrl) {
        apiUrl = normalizeApiUrl(apiUrl);
        if (apiUrl == null || apiUrl.isBlank()) return "Sync API URL is empty.";
        if (usesLocalhost(apiUrl)) {
            return "This URL uses localhost. For many computers, use the backend server IP, for example http://192.168.1.20/retail-pos-api/api/";
        }
        try {
            String healthUrl = apiUrl + "health";
            HttpURLConnection connection = openConn(healthUrl, null, "GET");
            connection.setReadTimeout(10_000);
            String response = readResponse(connection);
            return response.trim().startsWith("{")
                ? "Connected successfully to shared backend."
                : "Backend responded, but not with JSON. Check the /api/ path.";
        } catch (Exception exception) {
            return "Connection failed: " + exception.getMessage();
        }
    }

    private String normalizeApiUrl(String apiUrl) {
        if (apiUrl == null) return null;
        String normalized = apiUrl.trim();
        if (normalized.isEmpty()) return normalized;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private boolean usesLocalhost(String apiUrl) {
        String lower = apiUrl == null ? "" : apiUrl.toLowerCase(Locale.ROOT);
        return lower.contains("://localhost") || lower.contains("://127.0.0.1") || lower.contains("://0.0.0.0");
    }

    private Object normalizeOutgoingValue(String column, Object value) {
        if (value == null || !isDateTimeColumn(column)) return value;
        return normalizeDateForSql(value.toString());
    }

    private Object normalizeIncomingValue(String column, Object value) {
        if (value == null || !isDateTimeColumn(column)) return value;
        String normalized = normalizeDateForLocal(value.toString());
        return normalized != null ? normalized : value;
    }

    private boolean isDateTimeColumn(String column) {
        return column != null && (column.endsWith("_at") || "lockout_until".equals(column));
    }

    private String normalizeDateForSql(String value) {
        LocalDateTime local = parseFlexibleLocal(value);
        return local != null ? formatForSql(local) : value;
    }

    private String normalizeDateForLocal(String value) {
        LocalDateTime local = parseFlexibleLocal(value);
        return local != null ? local.toString() : value;
    }

    private String formatForSql(LocalDateTime value) {
        return value.withNano(0).format(SQL_DATE_TIME);
    }

    private String formatForSqlUtc(Instant value) {
        return UTC_SQL_DATE_TIME.format(value);
    }

    private LocalDateTime parseFlexibleLocal(String value) {
        if (isBlank(value) || "null".equalsIgnoreCase(value)) return null;
        String trimmed = value.trim();
        try { return LocalDateTime.parse(trimmed); } catch (DateTimeParseException ignored) { }
        try { return LocalDateTime.parse(trimmed.replace(' ', 'T')); } catch (DateTimeParseException ignored) { }
        Instant instant = parseFlexibleInstant(trimmed);
        if (instant != null) return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return null;
    }

    private Instant parseFlexibleInstant(String value) {
        if (isBlank(value) || "null".equalsIgnoreCase(value)) return null;
        String trimmed = value.trim();
        try { return Instant.parse(trimmed); } catch (DateTimeParseException ignored) { }
        try { return LocalDateTime.parse(trimmed).atZone(ZoneId.systemDefault()).toInstant(); } catch (DateTimeParseException ignored) { }
        try { return LocalDateTime.parse(trimmed.replace(' ', 'T')).atZone(ZoneId.systemDefault()).toInstant(); } catch (DateTimeParseException ignored) { }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private void setState(SyncState s, String msg) {
        state = s; stateMessage = msg;
        for (SyncStateListener l : listeners) {
            try { l.onSyncStateChanged(s, msg); } catch (Exception ignored) {}
        }
    }

    public interface SyncStateListener {
        void onSyncStateChanged(SyncState state, String message);
    }
}
