package com.heronix.security;

import com.heronix.model.domain.User;
import com.heronix.model.enums.Permission;
import com.heronix.model.enums.Role;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Security Context
 * Manages the current logged-in user session
 *
 * This is a singleton that holds the currently authenticated user.
 * In a JavaFX desktop application, we use a ThreadLocal to support
 * potential multi-window scenarios.
 *
 * Location: src/main/java/com/eduscheduler/security/SecurityContext.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
public class SecurityContext {

    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<LocalDateTime> loginTime = new ThreadLocal<>();
    private static final ThreadLocal<String> sessionId = new ThreadLocal<>();

    // Private constructor to prevent instantiation
    private SecurityContext() {
    }

    /**
     * Set the current authenticated user
     */
    public static void setCurrentUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        currentUser.set(user);
        loginTime.set(LocalDateTime.now());
        sessionId.set(generateSessionId());

        log.info("User logged in: {} (Role: {})", user.getUsername(), user.getRoleDisplayName());
    }

    /**
     * Get the current authenticated user
     */
    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser.get());
    }

    /**
     * Get the current user (throws exception if not authenticated)
     */
    public static User requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No user is currently authenticated"));
    }

    /**
     * Check if a user is currently authenticated
     */
    public static boolean isAuthenticated() {
        return currentUser.get() != null;
    }

    /**
     * Get the current user's username
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(User::getUsername);
    }

    /**
     * Get the current user's role
     */
    public static Optional<Role> getCurrentRole() {
        return getCurrentUser().map(User::getPrimaryRole);
    }

    /**
     * Get the login time
     */
    public static Optional<LocalDateTime> getLoginTime() {
        return Optional.ofNullable(loginTime.get());
    }

    /**
     * Get the session ID
     */
    public static Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId.get());
    }

    /**
     * Clear the current user (logout)
     */
    public static void clearCurrentUser() {
        User user = currentUser.get();
        if (user != null) {
            log.info("User logged out: {}", user.getUsername());
        }

        currentUser.remove();
        loginTime.remove();
        sessionId.remove();
    }

    // ========================================================================
    // PERMISSION CHECKS
    // ========================================================================

    /**
     * Check if current user has a specific permission
     */
    public static boolean hasPermission(Permission permission) {
        return getCurrentUser()
                .map(user -> user.hasPermission(permission))
                .orElse(false);
    }

    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(Role role) {
        return getCurrentUser()
                .map(user -> user.getPrimaryRole() == role)
                .orElse(false);
    }

    /**
     * Check if current user has any of the specified roles
     */
    public static boolean hasAnyRole(Role... roles) {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty()) {
            return false;
        }

        Role userRole = user.get().getPrimaryRole();
        for (Role role : roles) {
            if (userRole == role) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user is an administrator
     */
    public static boolean isAdmin() {
        return getCurrentUser()
                .map(User::isAdmin)
                .orElse(false);
    }

    /**
     * Check if current user can edit data
     */
    public static boolean canEdit() {
        return getCurrentUser()
                .map(User::canEdit)
                .orElse(false);
    }

    /**
     * Check if current user can view all data
     */
    public static boolean canViewAll() {
        return getCurrentUser()
                .map(User::canViewAll)
                .orElse(false);
    }

    /**
     * Check if current user can access medical data
     */
    public static boolean canAccessMedicalData() {
        return getCurrentUser()
                .map(User::canAccessMedicalData)
                .orElse(false);
    }

    /**
     * Check if current user can access IEP/504 data
     */
    public static boolean canAccessIEPData() {
        return getCurrentUser()
                .map(User::canAccessIEPData)
                .orElse(false);
    }

    // ========================================================================
    // AUTHORIZATION HELPERS
    // ========================================================================

    /**
     * Require that a user is authenticated (throws exception if not)
     */
    public static void requireAuthentication() {
        if (!isAuthenticated()) {
            throw new SecurityException("Authentication required");
        }
    }

    /**
     * Require that current user has a specific permission
     */
    public static void requirePermission(Permission permission) {
        requireAuthentication();
        if (!hasPermission(permission)) {
            throw new SecurityException("Permission denied: " + permission);
        }
    }

    /**
     * Require that current user has a specific role
     */
    public static void requireRole(Role role) {
        requireAuthentication();
        if (!hasRole(role)) {
            throw new SecurityException("Role required: " + role);
        }
    }

    /**
     * Require that current user has any of the specified roles
     */
    public static void requireAnyRole(Role... roles) {
        requireAuthentication();
        if (!hasAnyRole(roles)) {
            StringBuilder sb = new StringBuilder("One of the following roles required: ");
            for (int i = 0; i < roles.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(roles[i]);
            }
            throw new SecurityException(sb.toString());
        }
    }

    /**
     * Require that current user is an administrator
     */
    public static void requireAdmin() {
        requireAuthentication();
        if (!isAdmin()) {
            throw new SecurityException("Administrator privileges required");
        }
    }

    // ========================================================================
    // SESSION MANAGEMENT
    // ========================================================================

    /**
     * Generate a unique session ID
     */
    private static String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Get session duration in seconds
     */
    public static long getSessionDurationSeconds() {
        return getLoginTime()
                .map(lt -> java.time.Duration.between(lt, LocalDateTime.now()).getSeconds())
                .orElse(0L);
    }

    /**
     * Check if session has expired (8 hours)
     */
    public static boolean isSessionExpired() {
        long maxSessionSeconds = 8 * 60 * 60; // 8 hours
        return getSessionDurationSeconds() > maxSessionSeconds;
    }

    /**
     * Validate session and clear if expired
     */
    public static void validateSession() {
        if (isAuthenticated() && isSessionExpired()) {
            log.warn("Session expired for user: {}", getCurrentUsername().orElse("unknown"));
            clearCurrentUser();
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Get a summary of the current session
     */
    public static String getSessionSummary() {
        if (!isAuthenticated()) {
            return "No active session";
        }

        User user = requireCurrentUser();
        long durationSeconds = getSessionDurationSeconds();
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;

        return String.format(
                "User: %s | Role: %s | Session Duration: %dh %dm | Session ID: %s",
                user.getUsername(),
                user.getRoleDisplayName(),
                hours,
                minutes,
                getSessionId().orElse("unknown")
        );
    }
}
