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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

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

    private int retryCount = 0;
    private static final int MAX_RETRIES     = 5;
    private static final int POLL_INTERVAL_S = 30;
    private static final int CONNECT_TIMEOUT = 6_000;
    private static final int READ_TIMEOUT    = 30_000;

    private static final String[] ENTITIES = {
        "products", "product_images", "categories", "suppliers", "customers",
        "sales", "inventory_movements", "purchase_orders", "users"
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
            String apiUrl  = settings.getSyncApiUrl();
            if (apiUrl == null || apiUrl.isBlank()) {
                setState(SyncState.IDLE, "Sync API URL not configured");
                return;
            }
            // Ensure URL ends with /
            if (!apiUrl.endsWith("/")) apiUrl += "/";

            // ── Step 1: Authenticate (or skip if no-auth mode) ────────────────
            String token = getOrRefreshToken(apiUrl, settings);
            if (token == null) {
                // getOrRefreshToken already set the error state (401/429)
                return;
            }
            // token == "" means no-auth mode (REQUIRE_AUTH=false on server)

            // ── Step 2: Upload pending records ────────────────────────────────
            int uploaded = 0;
            for (String entity : ENTITIES) {
                uploaded += uploadEntity(apiUrl, token, entity);
            }

            // ── Step 3: Download changes from server ──────────────────────────
            String since = lastSyncTime != null
                ? lastSyncTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
                : "2000-01-01T00:00:00Z";
            int downloaded = 0;
            for (String entity : ENTITIES) {
                downloaded += downloadEntity(apiUrl, token, entity, since);
            }

            lastSyncTime = LocalDateTime.now();
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
        List<Map<String, Object>> pending = fetchPendingRecords(entityType);
        if (pending.isEmpty()) return 0;

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("entity_type", entityType);
            body.put("records", pending);

            String response = post(apiUrl + "sync/upload", token, gson.toJson(body));

            // Guard: if response is not JSON, log and skip
            if (!response.trim().startsWith("{")) {
                System.err.println("[SyncService] Non-JSON response for upload/" + entityType
                    + ": " + response.substring(0, Math.min(200, response.length())));
                return 0;
            }

            Map<String, Object> result = gson.fromJson(response, Map.class);

            Number success = (Number) result.getOrDefault("success", 0);
            if (success.intValue() > 0) {
                markSynced(entityType, extractIds(pending));
            }

            List<Map<String, Object>> conflicts =
                (List<Map<String, Object>>) result.getOrDefault("conflicts", Collections.emptyList());
            for (Map<String, Object> c : conflicts) {
                AuditLogger.log("SYSTEM", "SYNC_CONFLICT", String.valueOf(c.get("id")),
                    entityType + ": " + c.getOrDefault("reason", ""));
            }
            return success.intValue();
        } catch (Exception e) {
            // 401 = token expired, clear it so next cycle re-authenticates
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                backendToken = null; tokenExpiresAtMs = 0;
            }
            System.err.println("[SyncService] Upload failed for " + entityType + ": " + e.getMessage());
            return 0;
        }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int downloadEntity(String apiUrl, String token, String entityType, String since) {
        try {
            String url = apiUrl + "sync/download/" + entityType + "?since="
                + URLEncoder.encode(since, StandardCharsets.UTF_8);
            String response = get(url, token);

            Map<String, Object> result = gson.fromJson(response, Map.class);
            List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOrDefault("records", Collections.emptyList());
            if (records.isEmpty()) return 0;
            if ("products".equals(entityType)) cacheProductImages(apiUrl, token, records);

            upsertRecords(entityType, records);
            return records.size();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                backendToken = null; tokenExpiresAtMs = 0;
            }
            System.err.println("[SyncService] Download failed for " + entityType + ": " + e.getMessage());
            return 0;
        }
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private List<Map<String, Object>> fetchPendingRecords(String entityType) {
        String sql;
        if ("users".equals(entityType)) {
            // Never upload password_hash
            sql = "SELECT id,username,role,full_name,active,sync_status,created_at,updated_at " +
                  "FROM users WHERE sync_status IN ('PENDING','MODIFIED','DELETED') LIMIT 200";
        } else {
            sql = "SELECT * FROM " + entityType +
                  " WHERE sync_status IN ('PENDING','MODIFIED','DELETED') LIMIT 200";
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) row.put(meta.getColumnName(i), rs.getObject(i));
                if ("products".equals(entityType)) attachImageBlobs(row);
                rows.add(row);
            }
        } catch (Exception e) {
            System.err.println("[SyncService] fetchPending failed for " + entityType + ": " + e.getMessage());
        }
        return rows;
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
        for (Map<String, Object> product : products) {
            Object value = product.get("image_path");
            if (value == null) continue;
            List<String> cached = new ArrayList<>();
            for (String path : value.toString().split(";")) {
                try {
                    if (!path.startsWith("http") && !path.startsWith("uploads/")) { cached.add(path); continue; }
                    String url = path.startsWith("http") ? path : webRoot + path;
                    String name = url.substring(url.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
                    java.nio.file.Path destination = com.retailpos.util.AppPaths.imageDirectory().resolve(name);
                    HttpURLConnection connection = openConn(url, token, "GET");
                    try (InputStream input = connection.getInputStream()) {
                        java.nio.file.Files.copy(input, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    cached.add(destination.toString());
                } catch (Exception ignored) { cached.add(path); }
            }
            product.put("image_path", String.join(";", cached));
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
                    if (localUpdated.compareTo(serverUpdated) > 0) continue;
                }

                // Filter record to only columns that exist in the local SQLite table
                List<String> colList = new ArrayList<>();
                List<Object> vals    = new ArrayList<>();
                for (Map.Entry<String, Object> e : rec.entrySet()) {
                    String col = e.getKey();
                    if (!localColumns.contains(col)) continue; // skip unknown columns
                    colList.add(col);
                    vals.add(e.getValue());
                }

                if (colList.isEmpty()) continue;

                if (localUpdated != null) {
                    // UPDATE existing row
                    StringBuilder sb = new StringBuilder("UPDATE " + table + " SET ");
                    for (int i = 0; i < colList.size(); i++) {
                        sb.append("`").append(colList.get(i)).append("`=?");
                        if (i < colList.size() - 1) sb.append(",");
                    }
                    // Only set sync_status if column exists
                    if (localColumns.contains("sync_status")) sb.append(",sync_status='SYNCED'");
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
                    if (localColumns.contains("sync_status")) {
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
            } catch (Exception e) {
                System.err.println("[SyncService] Upsert failed: " + e.getMessage());
            }
        }
    }

    /** Returns the set of column names that exist in the local SQLite table. */
    private Set<String> getLocalColumns(String table) {
        Set<String> cols = new java.util.LinkedHashSet<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cols.add(rs.getString("name"));
        } catch (Exception e) {
            System.err.println("[SyncService] getLocalColumns failed for " + table + ": " + e.getMessage());
        }
        return cols;
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

    private HttpURLConnection openConn(String url, String token, String method) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json");
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
        if (apiUrl == null || apiUrl.isBlank()) return false;
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
