// Location: src/main/java/com/eduscheduler/service/impl/AuthenticationServiceImpl.java
package com.heronix.service.impl;

import com.heronix.model.domain.User;
import com.heronix.repository.UserRepository;
import com.heronix.service.AuthenticationService;
import com.heronix.service.UserService;
import com.heronix.util.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service Implementation
 * Handles authentication for JavaFX desktop application
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User authenticate(String username, String password) {
        // ✅ NULL SAFE: Check username and password are provided
        if (username == null || username.trim().isEmpty()) {
            log.warn("Authentication failed: Username is null or empty");
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.trim().isEmpty()) {
            log.warn("Authentication failed: Password is null or empty");
            throw new IllegalArgumentException("Password is required");
        }

        log.info("Attempting authentication for user: {}", username);

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User not found - {}", username);
                    return new IllegalArgumentException("Invalid username or password");
                });

        // Check if account is enabled
        // ✅ NULL SAFE: Check enabled field exists
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("Authentication failed: Account disabled - {}", username);
            userService.recordFailedLogin(username);
            throw new IllegalArgumentException("Account is disabled");
        }

        // Check if account is locked
        // ✅ NULL SAFE: Check accountNonLocked field exists
        if (!Boolean.TRUE.equals(user.getAccountNonLocked())) {
            log.warn("Authentication failed: Account locked - {}", username);
            throw new IllegalArgumentException("Account is locked due to too many failed login attempts");
        }

        // Verify password
        // ✅ NULL SAFE: Check password field exists
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Authentication failed: Invalid password - {}", username);
            userService.recordFailedLogin(username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Authentication successful
        log.info("Authentication successful for user: {}", username);
        userService.recordSuccessfulLogin(username);

        // Set in authentication context
        AuthenticationContext.setCurrentUser(user);

        return user;
    }

    @Override
    public void logout() {
        User currentUser = AuthenticationContext.getCurrentUser().orElse(null);
        // ✅ NULL SAFE: Check user and username exist before logging
        if (currentUser != null && currentUser.getUsername() != null) {
            log.info("Logging out user: {}", currentUser.getUsername());
        }

        AuthenticationContext.clearCurrentUser();
    }

    @Override
    public User getCurrentUser() {
        return AuthenticationContext.getCurrentUser().orElse(null);
    }

    @Override
    public boolean isAuthenticated() {
        return AuthenticationContext.isAuthenticated();
    }
}
