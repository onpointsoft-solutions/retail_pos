package com.retailpos.model;

import java.time.LocalDateTime;

public class AuditLog {
    private String id;
    private String userId;
    private String eventType;
    private String entityId;
    private String details;
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(String id, String userId, String eventType, String entityId, String details, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.eventType = eventType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
