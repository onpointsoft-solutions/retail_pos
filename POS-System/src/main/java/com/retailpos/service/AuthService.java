package com.retailpos.service;

import com.retailpos.model.User;
import com.retailpos.repository.UserRepository;
import com.retailpos.util.AuditLogger;
import com.retailpos.util.PasswordUtil;
import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {
    private static AuthService instance;
    private final UserRepository userRepo = new UserRepository();
    private User currentUser;
    private LocalDateTime sessionStart;
    private LocalDateTime lastActivity;

    private AuthService() {}

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public boolean login(String username, String password) throws Exception {
        Optional<User> opt = userRepo.findByUsername(username);
        if (opt.isEmpty()) {
            AuditLogger.log(null, AuditLogger.FAILED_LOGIN, null, "username=" + username + " (not found)");
            throw new Exception("Invalid username or password");
        }
        User user = opt.get();
        if (!user.isActive()) throw new Exception("Account is inactive. Contact an administrator.");
        if (user.isLockedOut()) {
            throw new Exception("Account is locked due to too many failed attempts. Try again after 15 minutes.");
        }
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            LocalDateTime lockout = attempts >= 5 ? LocalDateTime.now().plusMinutes(15) : null;
            userRepo.updateFailedAttempts(user.getId(), attempts, lockout);
            AuditLogger.log(user.getId(), AuditLogger.FAILED_LOGIN, user.getId(), "attempts=" + attempts);
            if (lockout != null) {
                AuditLogger.log(user.getId(), AuditLogger.ACCOUNT_LOCKED, user.getId(), "locked_until=" + lockout);
                throw new Exception("Too many failed attempts. Account locked for 15 minutes.");
            }
            throw new Exception("Invalid username or password");
        }
        // Success
        userRepo.updateFailedAttempts(user.getId(), 0, null);
        currentUser = user;
        sessionStart = LocalDateTime.now();
        lastActivity = sessionStart;
        AuditLogger.log(user.getId(), AuditLogger.LOGIN, user.getId(), "role=" + user.getRole());
        return true;
    }

    public void logout() {
        if (currentUser != null) {
            AuditLogger.log(currentUser.getId(), AuditLogger.LOGOUT, currentUser.getId(), "");
        }
        currentUser = null;
        sessionStart = null;
        lastActivity = null;
    }

    public void touchActivity() {
        lastActivity = LocalDateTime.now();
    }

    public boolean isSessionActive() {
        if (currentUser == null || lastActivity == null) return false;
        return LocalDateTime.now().isBefore(lastActivity.plusMinutes(30));
    }

    public boolean hasPermission(String permission) {
        if (currentUser == null) return false;
        if (currentUser.isAdmin()) return true;
        if (currentUser.isCashier()) {
            return switch (permission) {
                case "SELL", "SEARCH_PRODUCTS", "PRINT_RECEIPT",
                     "SUSPEND_SALE", "RESUME_SALE", "CASH_INOUT",
                     "VIEW_OWN_SALES", "ATTACH_CUSTOMER" -> true;
                default -> false;
            };
        }
        return false;
    }

    public User getCurrentUser() { return currentUser; }
    public LocalDateTime getSessionStart() { return sessionStart; }
    public boolean isAdmin() { return currentUser != null && currentUser.isAdmin(); }
    public boolean isCashier() { return currentUser != null && currentUser.isCashier(); }
    public boolean isLoggedIn() { return currentUser != null; }

    public void requirePermission(String permission) throws Exception {
        if (!hasPermission(permission)) {
            if (currentUser != null) {
                AuditLogger.log(currentUser.getId(), AuditLogger.PERMISSION_DENIED,
                    null, "permission=" + permission);
            }
            throw new Exception("Access denied. You do not have permission to perform this action.");
        }
    }
}
