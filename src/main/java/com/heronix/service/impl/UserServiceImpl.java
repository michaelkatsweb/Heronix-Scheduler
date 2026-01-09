// Location: src/main/java/com/eduscheduler/service/impl/UserServiceImpl.java
package com.heronix.service.impl;

import com.heronix.model.domain.User;
import com.heronix.model.enums.Permission;
import com.heronix.model.enums.Role;
import com.heronix.repository.UserRepository;
import com.heronix.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * User Service Implementation
 * Manages user CRUD operations and authentication
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Initialize default users on startup
     */
    @PostConstruct
    public void init() {
        initializeDefaultUsers();
    }

    @Override
    @Transactional
    public User createUser(User user, String rawPassword) {
        // ✅ NULL SAFE: Check user and required fields exist
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        log.info("Creating new user: {}", user.getUsername());

        // Validate
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(rawPassword));

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());

        return savedUser;
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        // ✅ NULL SAFE: Check user and ID exist
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and User ID cannot be null");
        }

        log.info("Updating user: {}", user.getUsername() != null ? user.getUsername() : "Unknown");

        if (!userRepository.existsById(user.getId())) {
            throw new IllegalArgumentException("User not found with ID: " + user.getId());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully");
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // ✅ NULL SAFE: Check parameters exist
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (oldPassword == null || newPassword == null) {
            throw new IllegalArgumentException("Passwords cannot be null");
        }

        log.info("Changing password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify old password
        // ✅ NULL SAFE: Check password field exists
        if (user.getPassword() == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        log.info("Resetting password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setCredentialsNonExpired(true);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void lockAccount(Long userId) {
        log.info("Locking account for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setAccountNonLocked(false);
        userRepository.save(user);

        log.info("Account locked: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void unlockAccount(Long userId) {
        log.info("Unlocking account for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setAccountNonLocked(true);
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        log.info("Account unlocked: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String username) {
        // ✅ NULL SAFE: Check username exists
        if (username == null) return;

        userRepository.findByUsername(username).ifPresent(user -> {
            user.updateLastLogin();
            userRepository.save(user);
            log.debug("Recorded successful login for: {}", username);
        });
    }

    @Override
    @Transactional
    public void recordFailedLogin(String username) {
        // ✅ NULL SAFE: Check username exists
        if (username == null) return;

        userRepository.findByUsername(username).ifPresent(user -> {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            log.warn("Failed login attempt for: {}. Total attempts: {}",
                    username, user.getFailedLoginAttempts());
        });
    }

    @Override
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    @Transactional
    public void initializeDefaultUsers() {
        log.info("Checking for default users...");

        try {
            // Check if admin user already exists by username or email
            if (userRepository.existsByUsername("admin") ||
                userRepository.existsByEmail("admin@eduscheduler.com")) {
                log.info("Admin user already exists. Skipping default user creation.");
                return;
            }

            // Double-check if any super admin exists
            if (!userRepository.existsSuperAdmin()) {
                log.info("No super admin found. Creating default super admin user...");

                // Create default super admin user
                User superAdmin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("Admin@123"))
                        .email("admin@eduscheduler.com")
                        .fullName("System Administrator")
                        .primaryRole(Role.SUPER_ADMIN)
                        .enabled(true)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .build();

                // Add legacy roles for backward compatibility
                superAdmin.addRole("ROLE_ADMIN");
                superAdmin.addRole("ROLE_USER");

                userRepository.save(superAdmin);
                log.info("Default super admin user created successfully");
                log.info("Username: admin, Password: Admin@123");
                log.warn("IMPORTANT: Change the default admin password immediately!");
            } else {
                log.info("Super admin already exists. Skipping default user creation.");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize default users (likely already exist): {}", e.getMessage());
            // Don't propagate the error - this is not critical if users already exist
        }
    }

    // ========================================================================
    // ROLE AND PERMISSION MANAGEMENT
    // ========================================================================

    @Override
    @Transactional
    public void assignRole(Long userId, Role role) {
        log.info("Assigning role {} to user ID: {}", role, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPrimaryRole(role);
        userRepository.save(user);

        log.info("Role {} assigned to user: {}", role, user.getUsername());
    }

    @Override
    @Transactional
    public void grantPermission(Long userId, Permission permission) {
        log.info("Granting permission {} to user ID: {}", permission, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.addPermission(permission);
        userRepository.save(user);

        log.info("Permission {} granted to user: {}", permission, user.getUsername());
    }

    @Override
    @Transactional
    public void revokePermission(Long userId, Permission permission) {
        log.info("Revoking permission {} from user ID: {}", permission, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.removePermission(permission);
        userRepository.save(user);

        log.info("Permission {} revoked from user: {}", permission, user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByPrimaryRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllAdministrators() {
        return userRepository.findAllAdministrators();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllCounselors() {
        return userRepository.findAllCounselors();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllTeachers() {
        return userRepository.findAllTeachers();
    }

    @Override
    public boolean existsSuperAdmin() {
        return userRepository.existsSuperAdmin();
    }

    // ========================================================================
    // AUTHENTICATION
    // ========================================================================

    @Override
    @Transactional
    public Optional<User> authenticate(String username, String password) {
        log.info("Authenticating user: {}", username);

        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);

        if (userOpt.isEmpty()) {
            log.warn("Authentication failed - user not found: {}", username);
            return Optional.empty();
        }

        User user = userOpt.get();

        // Check if account is enabled
        // ✅ NULL SAFE: Check enabled field exists
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("Authentication failed - account disabled: {}", username);
            return Optional.empty();
        }

        // Check if account is locked
        // ✅ NULL SAFE: Check accountNonLocked field exists
        if (!Boolean.TRUE.equals(user.getAccountNonLocked())) {
            log.warn("Authentication failed - account locked: {}", username);
            return Optional.empty();
        }

        // Verify password
        // ✅ NULL SAFE: Check password field exists
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Authentication failed - incorrect password: {}", username);
            recordFailedLogin(username);
            return Optional.empty();
        }

        // Authentication successful
        recordSuccessfulLogin(username);
        log.info("Authentication successful: {}", username);

        return Optional.of(user);
    }

    @Override
    public boolean isPasswordValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        // Password requirements:
        // - At least 8 characters
        // - At least one uppercase letter
        // - At least one lowercase letter
        // - At least one digit
        // - At least one special character

        if (password.length() < 8) {
            return false;
        }

        // Check for uppercase
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            return false;
        }

        // Check for lowercase
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            return false;
        }

        // Check for digit
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            return false;
        }

        // Check for special character
        if (!Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find()) {
            return false;
        }

        return true;
    }

    // ========================================================================
    // ACCOUNT MANAGEMENT
    // ========================================================================

    @Override
    @Transactional
    public void enableAccount(Long userId) {
        log.info("Enabling account for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEnabled(true);
        userRepository.save(user);

        log.info("Account enabled: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void disableAccount(Long userId) {
        log.info("Disabling account for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEnabled(false);
        userRepository.save(user);

        log.info("Account disabled: {}", user.getUsername());
    }
}
