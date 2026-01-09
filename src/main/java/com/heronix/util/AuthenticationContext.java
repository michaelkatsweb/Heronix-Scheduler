// Location: src/main/java/com/eduscheduler/util/AuthenticationContext.java
package com.heronix.util;

import com.heronix.model.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Authentication Context Utility
 * Provides easy access to current user information
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
public class AuthenticationContext {

    // Thread-local storage for current user (for JavaFX desktop app)
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    /**
     * Get the currently authenticated username
     */
    public static String getCurrentUsername() {
        // First check thread-local (JavaFX)
        User user = currentUser.get();
        if (user != null) {
            return user.getUsername();
        }

        // Fall back to Spring Security context
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }
        }

        return "System"; // Default fallback
    }

    /**
     * Get the current user (from thread-local)
     */
    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser.get());
    }

    /**
     * Set the current user (for JavaFX desktop app)
     */
    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }

    /**
     * Clear the current user
     */
    public static void clearCurrentUser() {
        currentUser.remove();
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        // Check thread-local first
        if (currentUser.get() != null) {
            return true;
        }

        // Check Spring Security
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        return authentication != null &&
               authentication.isAuthenticated() &&
               !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(String role) {
        // Check thread-local user
        User user = currentUser.get();
        if (user != null) {
            return user.hasRole(role);
        }

        // Check Spring Security
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(role));
        }

        return false;
    }

    /**
     * Check if current user is admin
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Get current user's full name
     */
    public static String getCurrentUserFullName() {
        User user = currentUser.get();
        if (user != null && user.getFullName() != null) {
            return user.getFullName();
        }

        return getCurrentUsername();
    }

    /**
     * Get current user ID
     */
    public static Long getCurrentUserId() {
        User user = currentUser.get();
        return user != null ? user.getId() : null;
    }

    /**
     * Get the email of the current authenticated user
     * Useful for looking up associated Teacher record
     */
    public static String getCurrentUserEmail() {
        User user = currentUser.get();
        if (user != null && user.getEmail() != null) {
            return user.getEmail();
        }
        // No current user or email, return null
        return null;
    }
}
