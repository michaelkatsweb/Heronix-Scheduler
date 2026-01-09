// Location: src/main/java/com/eduscheduler/service/AuthenticationService.java
package com.heronix.service;

import com.heronix.model.domain.User;

/**
 * Authentication Service Interface
 * Handles user authentication for JavaFX desktop application
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
public interface AuthenticationService {

    /**
     * Authenticate user with username and password
     *
     * @param username Username
     * @param password Raw password
     * @return User object if authentication successful
     * @throws IllegalArgumentException if authentication fails
     */
    User authenticate(String username, String password);

    /**
     * Logout current user
     */
    void logout();

    /**
     * Get currently logged in user
     *
     * @return Current user or null if not logged in
     */
    User getCurrentUser();

    /**
     * Check if user is logged in
     *
     * @return true if user is authenticated
     */
    boolean isAuthenticated();
}
