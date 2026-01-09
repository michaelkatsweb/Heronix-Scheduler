package com.heronix.service.impl;

import com.heronix.model.domain.User;
import com.heronix.model.enums.Permission;
import com.heronix.model.enums.Role;
import com.heronix.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for UserServiceImpl
 *
 * Service: 18th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/UserServiceImplTest.java
 *
 * Tests cover:
 * - User CRUD operations
 * - Password management (create, change, reset)
 * - Authentication
 * - Password validation
 * - Account management (lock, unlock, enable, disable)
 * - Role and permission management
 * - Login attempt tracking
 * - Default user initialization
 * - Edge cases and null handling
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock(lenient = true)
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl service;

    private User testUser;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // Create test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password(passwordEncoder.encode("Test@123"))
                .email("test@example.com")
                .fullName("Test User")
                .primaryRole(Role.TEACHER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .failedLoginAttempts(0)
                .build();
    }

    // ========== CREATE USER TESTS ==========

    @Test
    void testCreateUser_WithValidData_ShouldReturnUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User newUser = User.builder()
                .username("newuser")
                .email("new@example.com")
                .fullName("New User")
                .build();

        User result = service.createUser(newUser, "Password@123");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertNotNull(result.getPassword());
        assertNotEquals("Password@123", result.getPassword()); // Should be encoded
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_WithNullUser_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.createUser(null, "Password@123"));
    }

    @Test
    void testCreateUser_WithNullUsername_ShouldThrowException() {
        User user = User.builder().build();
        assertThrows(IllegalArgumentException.class, () ->
            service.createUser(user, "Password@123"));
    }

    @Test
    void testCreateUser_WithEmptyUsername_ShouldThrowException() {
        User user = User.builder().username("   ").build();
        assertThrows(IllegalArgumentException.class, () ->
            service.createUser(user, "Password@123"));
    }

    @Test
    void testCreateUser_WithNullPassword_ShouldThrowException() {
        User user = User.builder().username("testuser").build();
        assertThrows(IllegalArgumentException.class, () ->
            service.createUser(user, null));
    }

    @Test
    void testCreateUser_WithEmptyPassword_ShouldThrowException() {
        User user = User.builder().username("testuser").build();
        assertThrows(IllegalArgumentException.class, () ->
            service.createUser(user, "   "));
    }

    @Test
    void testCreateUser_WithExistingUsername_ShouldThrowException() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        User user = User.builder().username("existinguser").build();

        assertThrows(IllegalArgumentException.class, () ->
            service.createUser(user, "Password@123"));
    }

    @Test
    void testCreateUser_WithExistingEmail_ShouldThrowException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        User user = User.builder()
                .username("newuser")
                .email("existing@example.com")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
            service.createUser(user, "Password@123"));
    }

    // ========== UPDATE USER TESTS ==========

    @Test
    void testUpdateUser_WithValidUser_ShouldReturnUpdatedUser() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        testUser.setFullName("Updated Name");
        User result = service.updateUser(testUser);

        assertNotNull(result);
        assertEquals("Updated Name", result.getFullName());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUser_WithNullUser_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.updateUser(null));
    }

    @Test
    void testUpdateUser_WithNullUserId_ShouldThrowException() {
        testUser.setId(null);
        assertThrows(IllegalArgumentException.class, () ->
            service.updateUser(testUser));
    }

    @Test
    void testUpdateUser_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.existsById(999L)).thenReturn(false);
        testUser.setId(999L);

        assertThrows(IllegalArgumentException.class, () ->
            service.updateUser(testUser));
    }

    // ========== FIND USER TESTS ==========

    @Test
    void testFindByUsername_WithExistingUser_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = service.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void testFindByUsername_WithNonExistentUser_ShouldReturnEmpty() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = service.findByUsername("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindById_WithExistingUser_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testFindById_WithNonExistentUser_ShouldReturnEmpty() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = service.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));

        List<User> result = service.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== DELETE USER TESTS ==========

    @Test
    void testDeleteUser_WithExistingUser_ShouldDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> service.deleteUser(1L));

        verify(userRepository).deleteById(1L);
    }

    @Test
    void testDeleteUser_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            service.deleteUser(999L));
    }

    // ========== CHANGE PASSWORD TESTS ==========

    @Test
    void testChangePassword_WithCorrectOldPassword_ShouldChangePassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() ->
            service.changePassword(1L, "Test@123", "NewPass@123"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePassword_WithNullUserId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(null, "old", "new"));
    }

    @Test
    void testChangePassword_WithNullOldPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(1L, null, "new"));
    }

    @Test
    void testChangePassword_WithNullNewPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(1L, "old", null));
    }

    @Test
    void testChangePassword_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(999L, "old", "new"));
    }

    @Test
    void testChangePassword_WithIncorrectOldPassword_ShouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(1L, "WrongPassword@123", "NewPass@123"));
    }

    @Test
    void testChangePassword_WithNullUserPassword_ShouldThrowException() {
        testUser.setPassword(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(1L, "Test@123", "NewPass@123"));
    }

    // ========== RESET PASSWORD TESTS ==========

    @Test
    void testResetPassword_WithValidUser_ShouldResetPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() ->
            service.resetPassword(1L, "NewPass@123"));

        verify(userRepository).save(any(User.class));
        assertTrue(testUser.getCredentialsNonExpired());
    }

    @Test
    void testResetPassword_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.resetPassword(999L, "NewPass@123"));
    }

    // ========== LOCK/UNLOCK ACCOUNT TESTS ==========

    @Test
    void testLockAccount_WithValidUser_ShouldLockAccount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.lockAccount(1L);

        assertFalse(testUser.getAccountNonLocked());
        verify(userRepository).save(testUser);
    }

    @Test
    void testLockAccount_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.lockAccount(999L));
    }

    @Test
    void testUnlockAccount_WithValidUser_ShouldUnlockAccount() {
        testUser.setAccountNonLocked(false);
        testUser.setFailedLoginAttempts(5);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.unlockAccount(1L);

        assertTrue(testUser.getAccountNonLocked());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUnlockAccount_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.unlockAccount(999L));
    }

    // ========== ENABLE/DISABLE ACCOUNT TESTS ==========

    @Test
    void testEnableAccount_WithValidUser_ShouldEnableAccount() {
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.enableAccount(1L);

        assertTrue(testUser.getEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void testEnableAccount_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.enableAccount(999L));
    }

    @Test
    void testDisableAccount_WithValidUser_ShouldDisableAccount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.disableAccount(1L);

        assertFalse(testUser.getEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void testDisableAccount_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.disableAccount(999L));
    }

    // ========== LOGIN TRACKING TESTS ==========

    @Test
    void testRecordSuccessfulLogin_WithValidUsername_ShouldUpdateUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.recordSuccessfulLogin("testuser");

        verify(userRepository).save(testUser);
    }

    @Test
    void testRecordSuccessfulLogin_WithNullUsername_ShouldNotCrash() {
        assertDoesNotThrow(() -> service.recordSuccessfulLogin(null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRecordSuccessfulLogin_WithNonExistentUser_ShouldNotCrash() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.recordSuccessfulLogin("nonexistent"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRecordFailedLogin_WithValidUsername_ShouldIncrementAttempts() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.recordFailedLogin("testuser");

        verify(userRepository).save(testUser);
    }

    @Test
    void testRecordFailedLogin_WithNullUsername_ShouldNotCrash() {
        assertDoesNotThrow(() -> service.recordFailedLogin(null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRecordFailedLogin_WithNonExistentUser_ShouldNotCrash() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.recordFailedLogin("nonexistent"));
        verify(userRepository, never()).save(any());
    }

    // ========== USER EXISTS TESTS ==========

    @Test
    void testUserExists_WithExistingUsername_ShouldReturnTrue() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        boolean result = service.userExists("testuser");

        assertTrue(result);
    }

    @Test
    void testUserExists_WithNonExistentUsername_ShouldReturnFalse() {
        when(userRepository.existsByUsername("nonexistent")).thenReturn(false);

        boolean result = service.userExists("nonexistent");

        assertFalse(result);
    }

    @Test
    void testEmailExists_WithExistingEmail_ShouldReturnTrue() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        boolean result = service.emailExists("test@example.com");

        assertTrue(result);
    }

    @Test
    void testEmailExists_WithNonExistentEmail_ShouldReturnFalse() {
        when(userRepository.existsByEmailIgnoreCase("nonexistent@example.com")).thenReturn(false);

        boolean result = service.emailExists("nonexistent@example.com");

        assertFalse(result);
    }

    // ========== ROLE AND PERMISSION TESTS ==========

    @Test
    void testAssignRole_WithValidUser_ShouldAssignRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.assignRole(1L, Role.ADMIN);

        assertEquals(Role.ADMIN, testUser.getPrimaryRole());
        verify(userRepository).save(testUser);
    }

    @Test
    void testAssignRole_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.assignRole(999L, Role.ADMIN));
    }

    @Test
    void testGrantPermission_WithValidUser_ShouldGrantPermission() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.grantPermission(1L, Permission.USER_MANAGEMENT);

        verify(userRepository).save(testUser);
    }

    @Test
    void testGrantPermission_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.grantPermission(999L, Permission.USER_MANAGEMENT));
    }

    @Test
    void testRevokePermission_WithValidUser_ShouldRevokePermission() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.revokePermission(1L, Permission.USER_MANAGEMENT);

        verify(userRepository).save(testUser);
    }

    @Test
    void testRevokePermission_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.revokePermission(999L, Permission.USER_MANAGEMENT));
    }

    @Test
    void testFindByRole_ShouldReturnUsersWithRole() {
        when(userRepository.findByPrimaryRole(Role.TEACHER)).thenReturn(Arrays.asList(testUser));

        List<User> result = service.findByRole(Role.TEACHER);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllAdministrators_ShouldReturnAdmins() {
        when(userRepository.findAllAdministrators()).thenReturn(Arrays.asList(testUser));

        List<User> result = service.findAllAdministrators();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testFindAllCounselors_ShouldReturnCounselors() {
        when(userRepository.findAllCounselors()).thenReturn(Arrays.asList(testUser));

        List<User> result = service.findAllCounselors();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testFindAllTeachers_ShouldReturnTeachers() {
        when(userRepository.findAllTeachers()).thenReturn(Arrays.asList(testUser));

        List<User> result = service.findAllTeachers();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testExistsSuperAdmin_WhenExists_ShouldReturnTrue() {
        when(userRepository.existsSuperAdmin()).thenReturn(true);

        boolean result = service.existsSuperAdmin();

        assertTrue(result);
    }

    @Test
    void testExistsSuperAdmin_WhenNotExists_ShouldReturnFalse() {
        when(userRepository.existsSuperAdmin()).thenReturn(false);

        boolean result = service.existsSuperAdmin();

        assertFalse(result);
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testAuthenticate_WithValidCredentials_ShouldReturnUser() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Optional<User> result = service.authenticate("testuser", "Test@123");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void testAuthenticate_WithNonExistentUser_ShouldReturnEmpty() {
        when(userRepository.findByUsernameIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = service.authenticate("nonexistent", "Password@123");

        assertFalse(result.isPresent());
    }

    @Test
    void testAuthenticate_WithDisabledAccount_ShouldReturnEmpty() {
        testUser.setEnabled(false);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = service.authenticate("testuser", "Test@123");

        assertFalse(result.isPresent());
    }

    @Test
    void testAuthenticate_WithLockedAccount_ShouldReturnEmpty() {
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = service.authenticate("testuser", "Test@123");

        assertFalse(result.isPresent());
    }

    @Test
    void testAuthenticate_WithIncorrectPassword_ShouldReturnEmpty() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = service.authenticate("testuser", "WrongPassword@123");

        assertFalse(result.isPresent());
    }

    @Test
    void testAuthenticate_WithNullUserPassword_ShouldReturnEmpty() {
        testUser.setPassword(null);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = service.authenticate("testuser", "Test@123");

        assertFalse(result.isPresent());
    }

    @Test
    void testAuthenticate_WithNullEnabledField_ShouldReturnEmpty() {
        testUser.setEnabled(null);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = service.authenticate("testuser", "Test@123");

        assertFalse(result.isPresent());
    }

    @Test
    void testAuthenticate_WithNullAccountNonLockedField_ShouldReturnEmpty() {
        testUser.setAccountNonLocked(null);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = service.authenticate("testuser", "Test@123");

        assertFalse(result.isPresent());
    }

    // ========== PASSWORD VALIDATION TESTS ==========

    @Test
    void testIsPasswordValid_WithValidPassword_ShouldReturnTrue() {
        boolean result = service.isPasswordValid("Password@123");
        assertTrue(result);
    }

    @Test
    void testIsPasswordValid_WithNullPassword_ShouldReturnFalse() {
        boolean result = service.isPasswordValid(null);
        assertFalse(result);
    }

    @Test
    void testIsPasswordValid_WithEmptyPassword_ShouldReturnFalse() {
        boolean result = service.isPasswordValid("");
        assertFalse(result);
    }

    @Test
    void testIsPasswordValid_WithTooShortPassword_ShouldReturnFalse() {
        boolean result = service.isPasswordValid("Pass@1");
        assertFalse(result);
    }

    @Test
    void testIsPasswordValid_WithoutUppercase_ShouldReturnFalse() {
        boolean result = service.isPasswordValid("password@123");
        assertFalse(result);
    }

    @Test
    void testIsPasswordValid_WithoutLowercase_ShouldReturnFalse() {
        boolean result = service.isPasswordValid("PASSWORD@123");
        assertFalse(result);
    }

    @Test
    void testIsPasswordValid_WithoutDigit_ShouldReturnFalse() {
        boolean result = service.isPasswordValid("Password@abc");
        assertFalse(result);
    }

    @Test
    void testIsPasswordValid_WithoutSpecialChar_ShouldReturnFalse() {
        boolean result = service.isPasswordValid("Password123");
        assertFalse(result);
    }

    // ========== DEFAULT USER INITIALIZATION TESTS ==========

    @Test
    void testInitializeDefaultUsers_WhenAdminExists_ShouldSkip() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertDoesNotThrow(() -> service.initializeDefaultUsers());

        verify(userRepository, never()).save(any());
    }

    @Test
    void testInitializeDefaultUsers_WhenEmailExists_ShouldSkip() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@eduscheduler.com")).thenReturn(true);

        assertDoesNotThrow(() -> service.initializeDefaultUsers());

        verify(userRepository, never()).save(any());
    }

    @Test
    void testInitializeDefaultUsers_WhenNoSuperAdmin_ShouldCreateAdmin() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@eduscheduler.com")).thenReturn(false);
        when(userRepository.existsSuperAdmin()).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.initializeDefaultUsers();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testInitializeDefaultUsers_WhenSuperAdminExists_ShouldSkip() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@eduscheduler.com")).thenReturn(false);
        when(userRepository.existsSuperAdmin()).thenReturn(true);

        assertDoesNotThrow(() -> service.initializeDefaultUsers());

        verify(userRepository, never()).save(any());
    }

    @Test
    void testInitializeDefaultUsers_WithException_ShouldNotPropagate() {
        when(userRepository.existsByUsername("admin")).thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> service.initializeDefaultUsers());
    }
}
