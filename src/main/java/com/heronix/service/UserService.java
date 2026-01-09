// Location: src/main/java/com/eduscheduler/service/UserService.java
package com.heronix.service;

import com.heronix.model.domain.User;
import com.heronix.model.enums.Permission;
import com.heronix.model.enums.Role;
import java.util.List;
import java.util.Optional;

/**
 * User Service Interface
 * Manages user operations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
public interface UserService {

    /**
     * Create a new user
     */
    User createUser(User user, String rawPassword);

    /**
     * Update existing user
     */
    User updateUser(User user);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by ID
     */
    Optional<User> findById(Long id);

    /**
     * Get all users
     */
    List<User> findAll();

    /**
     * Delete user by ID
     */
    void deleteUser(Long id);

    /**
     * Change user password
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * Reset user password (admin function)
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * Lock user account
     */
    void lockAccount(Long userId);

    /**
     * Unlock user account
     */
    void unlockAccount(Long userId);

    /**
     * Record successful login
     */
    void recordSuccessfulLogin(String username);

    /**
     * Record failed login
     */
    void recordFailedLogin(String username);

    /**
     * Check if user exists
     */
    boolean userExists(String username);

    /**
     * Check if email exists
     */
    boolean emailExists(String email);

    /**
     * Initialize default admin user if no users exist
     */
    void initializeDefaultUsers();

    // ========================================================================
    // ROLE AND PERMISSION MANAGEMENT
    // ========================================================================

    /**
     * Assign role to user
     */
    void assignRole(Long userId, Role role);

    /**
     * Grant permission to user
     */
    void grantPermission(Long userId, Permission permission);

    /**
     * Revoke permission from user
     */
    void revokePermission(Long userId, Permission permission);

    /**
     * Find users by role
     */
    List<User> findByRole(Role role);

    /**
     * Find all administrators
     */
    List<User> findAllAdministrators();

    /**
     * Find all counselors
     */
    List<User> findAllCounselors();

    /**
     * Find all teachers
     */
    List<User> findAllTeachers();

    /**
     * Check if any super admin exists
     */
    boolean existsSuperAdmin();

    /**
     * Authenticate user credentials
     * Returns user if authentication successful, empty if failed
     */
    Optional<User> authenticate(String username, String password);

    /**
     * Validate password strength
     */
    boolean isPasswordValid(String password);

    /**
     * Enable user account
     */
    void enableAccount(Long userId);

    /**
     * Disable user account
     */
    void disableAccount(Long userId);
}
