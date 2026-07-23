package com.retailpos.model;

import java.time.LocalDateTime;

public class User {
    private String id;
    private String username;
    private String passwordHash;
    private String role; // "ADMIN" / "CASHIER"
    private String fullName;
    private boolean active;
    private int failedLoginAttempts;
    private LocalDateTime lockoutUntil;
    private String syncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(LocalDateTime lockoutUntil) { this.lockoutUntil = lockoutUntil; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isCashier() { return "CASHIER".equals(role); }
    public boolean isLockedOut() {
        return lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
    }

    @Override
    public String toString() { return fullName != null ? fullName : username; }
}
