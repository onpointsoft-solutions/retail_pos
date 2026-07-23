package com.retailpos.util;

import com.retailpos.model.AuditLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLogger {

    private AuditLogger() {}

    public static void log(String userId, String eventType, String entityId, String details) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "INSERT INTO audit_logs(id,user_id,event_type,entity_id,details,created_at) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, userId != null ? userId : "SYSTEM");
            ps.setString(3, eventType);
            ps.setString(4, entityId);
            ps.setString(5, details);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            // Logging must never crash the application
            System.err.println("[AuditLogger] Failed to log event: " + e.getMessage());
        }
    }

    // Common event type constants
    public static final String LOGIN             = "LOGIN";
    public static final String LOGOUT            = "LOGOUT";
    public static final String FAILED_LOGIN      = "FAILED_LOGIN";
    public static final String ACCOUNT_LOCKED    = "ACCOUNT_LOCKED";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String SALE_COMPLETED    = "SALE_COMPLETED";
    public static final String PRODUCT_CREATED   = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED   = "PRODUCT_UPDATED";
    public static final String PRODUCT_DELETED   = "PRODUCT_DELETED";
    public static final String STOCK_ADJUSTMENT  = "STOCK_ADJUSTMENT";
    public static final String USER_CREATED      = "USER_CREATED";
    public static final String USER_UPDATED      = "USER_UPDATED";
    public static final String USER_DELETED      = "USER_DELETED";
    public static final String SETTINGS_CHANGED  = "SETTINGS_CHANGED";
    public static final String BACKUP_CREATED    = "BACKUP_CREATED";
    public static final String BACKUP_FAILED     = "BACKUP_FAILED";
    public static final String STOCK_OVERRIDE    = "STOCK_OVERRIDE";
}
