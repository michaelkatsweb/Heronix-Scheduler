package com.heronix.service.impl;

import com.heronix.model.domain.User;
import com.heronix.repository.UserRepository;
import com.heronix.service.UserService;
import com.heronix.util.AuthenticationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for AuthenticationServiceImpl
 *
 * Service: 22nd of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/AuthenticationServiceImplTest.java
 *
 * Tests cover:
 * - User authentication (username/password validation)
 * - Account status checks (enabled, locked)
 * - Password encoding and matching
 * - Login attempt tracking (successful/failed)
 * - Authentication context management
 * - Logout functionality
 * - Current user retrieval
 * - Authentication status checking
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock(lenient = true)
    private UserRepository userRepository;

    @Mock(lenient = true)
    private UserService userService;

    @Mock(lenient = true)
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationServiceImpl service;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clear authentication context before each test
        AuthenticationContext.clearCurrentUser();

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPasswordHash"); // BCrypt encoded password
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
    }

    @AfterEach
    void tearDown() {
        // Clear authentication context after each test
        AuthenticationContext.clearCurrentUser();
    }

    // ========== AUTHENTICATE TESTS ==========

    @Test
    void testAuthenticate_WithValidCredentials_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);

        User result = service.authenticate("testuser", "password123");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userService).recordSuccessfulLogin("testuser");
        assertTrue(AuthenticationContext.isAuthenticated());
    }

    @Test
    void testAuthenticate_WithNullUsername_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate(null, "password123");
        });
    }

    @Test
    void testAuthenticate_WithEmptyUsername_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("   ", "password123");
        });
    }

    @Test
    void testAuthenticate_WithNullPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", null);
        });
    }

    @Test
    void testAuthenticate_WithEmptyPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "   ");
        });
    }

    @Test
    void testAuthenticate_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("nonexistent", "password123");
        });
    }

    @Test
    void testAuthenticate_WithDisabledAccount_ShouldThrowException() {
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "password123");
        });

        verify(userService).recordFailedLogin("testuser");
    }

    @Test
    void testAuthenticate_WithNullEnabledFlag_ShouldThrowException() {
        testUser.setEnabled(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "password123");
        });

        verify(userService).recordFailedLogin("testuser");
    }

    @Test
    void testAuthenticate_WithLockedAccount_ShouldThrowException() {
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "password123");
        });

        // Should NOT record failed login for locked accounts
        verify(userService, never()).recordFailedLogin(anyString());
    }

    @Test
    void testAuthenticate_WithNullAccountNonLockedFlag_ShouldThrowException() {
        testUser.setAccountNonLocked(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "password123");
        });

        // Should NOT record failed login for locked accounts
        verify(userService, never()).recordFailedLogin(anyString());
    }

    @Test
    void testAuthenticate_WithInvalidPassword_ShouldThrowException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "wrongpassword");
        });

        verify(userService).recordFailedLogin("testuser");
    }

    @Test
    void testAuthenticate_WithNullStoredPassword_ShouldThrowException() {
        testUser.setPassword(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "password123");
        });

        verify(userService).recordFailedLogin("testuser");
    }

    @Test
    void testAuthenticate_ShouldSetAuthenticationContext() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);

        service.authenticate("testuser", "password123");

        assertTrue(AuthenticationContext.isAuthenticated());
        User currentUser = AuthenticationContext.getCurrentUser().orElse(null);
        assertNotNull(currentUser);
        assertEquals("testuser", currentUser.getUsername());
    }

    // ========== LOGOUT TESTS ==========

    @Test
    void testLogout_WithAuthenticatedUser_ShouldClearContext() {
        // First authenticate
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        service.authenticate("testuser", "password123");

        // Verify authenticated
        assertTrue(AuthenticationContext.isAuthenticated());

        // Logout
        service.logout();

        // Verify context cleared
        assertFalse(AuthenticationContext.isAuthenticated());
        assertFalse(AuthenticationContext.getCurrentUser().isPresent());
    }

    @Test
    void testLogout_WithNoAuthenticatedUser_ShouldNotCrash() {
        assertDoesNotThrow(() -> {
            service.logout();
        });

        assertFalse(AuthenticationContext.isAuthenticated());
    }

    @Test
    void testLogout_WithUserHavingNullUsername_ShouldNotCrash() {
        // Manually set a user with null username
        User userWithNullUsername = new User();
        userWithNullUsername.setUsername(null);
        AuthenticationContext.setCurrentUser(userWithNullUsername);

        assertDoesNotThrow(() -> {
            service.logout();
        });

        assertFalse(AuthenticationContext.isAuthenticated());
    }

    // ========== GET CURRENT USER TESTS ==========

    @Test
    void testGetCurrentUser_WithAuthenticatedUser_ShouldReturnUser() {
        // First authenticate
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        service.authenticate("testuser", "password123");

        User result = service.getCurrentUser();

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testGetCurrentUser_WithNoAuthenticatedUser_ShouldReturnNull() {
        User result = service.getCurrentUser();

        assertNull(result);
    }

    @Test
    void testGetCurrentUser_AfterLogout_ShouldReturnNull() {
        // First authenticate
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        service.authenticate("testuser", "password123");

        // Verify authenticated
        assertNotNull(service.getCurrentUser());

        // Logout
        service.logout();

        // Verify current user is null
        assertNull(service.getCurrentUser());
    }

    // ========== IS AUTHENTICATED TESTS ==========

    @Test
    void testIsAuthenticated_WithAuthenticatedUser_ShouldReturnTrue() {
        // First authenticate
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        service.authenticate("testuser", "password123");

        boolean result = service.isAuthenticated();

        assertTrue(result);
    }

    @Test
    void testIsAuthenticated_WithNoAuthenticatedUser_ShouldReturnFalse() {
        boolean result = service.isAuthenticated();

        assertFalse(result);
    }

    @Test
    void testIsAuthenticated_AfterLogout_ShouldReturnFalse() {
        // First authenticate
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        service.authenticate("testuser", "password123");

        // Verify authenticated
        assertTrue(service.isAuthenticated());

        // Logout
        service.logout();

        // Verify not authenticated
        assertFalse(service.isAuthenticated());
    }

    // ========== MULTIPLE AUTHENTICATION SCENARIOS ==========

    @Test
    void testAuthenticate_MultipleSuccessfulLogins_ShouldReplaceContext() {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("$2a$10$hash1");
        user1.setEnabled(true);
        user1.setAccountNonLocked(true);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("$2a$10$hash2");
        user2.setEnabled(true);
        user2.setAccountNonLocked(true);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Login as user1
        service.authenticate("user1", "password1");
        assertEquals("user1", service.getCurrentUser().getUsername());

        // Login as user2 (should replace user1)
        service.authenticate("user2", "password2");
        assertEquals("user2", service.getCurrentUser().getUsername());
    }

    @Test
    void testAuthenticate_WithPasswordEncoderException_ShouldPropagateException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenThrow(new RuntimeException("Encoding error"));

        assertThrows(RuntimeException.class, () -> {
            service.authenticate("testuser", "password123");
        });
    }

    // ========== EDGE CASES ==========

    @Test
    void testAuthenticate_WithWhitespaceOnlyUsername_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("   \t\n   ", "password123");
        });
    }

    @Test
    void testAuthenticate_WithWhitespaceOnlyPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("testuser", "   \t\n   ");
        });
    }

    @Test
    void testAuthenticate_WithCaseSensitiveUsername_ShouldRespectRepository() {
        // This tests that the service correctly delegates to repository
        // The repository may or may not be case-sensitive
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.authenticate("TestUser", "password123");
        });

        verify(userRepository).findByUsername("TestUser");
    }
}
