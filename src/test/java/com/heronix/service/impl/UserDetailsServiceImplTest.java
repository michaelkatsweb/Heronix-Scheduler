package com.heronix.service.impl;

import com.heronix.model.domain.User;
import com.heronix.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for UserDetailsServiceImpl
 *
 * Tests Spring Security UserDetailsService implementation for loading user data.
 * Focuses on username lookup, role conversion, and null safety.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user with all fields populated
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setEmail("john.doe@example.com");
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setCredentialsNonExpired(true);
        testUser.setAccountNonLocked(true);

        Set<String> roles = new HashSet<>(Arrays.asList("ROLE_USER", "ROLE_TEACHER"));
        testUser.setRoles(roles);
    }

    // ========== LOAD USER BY USERNAME TESTS ==========

    @Test
    void testLoadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertEquals("johndoe", result.getUsername());
        assertEquals("$2a$10$hashedpassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isCredentialsNonExpired());
        assertTrue(result.isAccountNonLocked());
        verify(userRepository, times(1)).findByUsername("johndoe");
    }

    @Test
    void testLoadUserByUsername_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
            service.loadUserByUsername("unknown"));

        verify(userRepository, times(1)).findByUsername("unknown");
    }

    @Test
    void testLoadUserByUsername_WithNullUsername_ShouldThrowException() {
        assertThrows(UsernameNotFoundException.class, () ->
            service.loadUserByUsername(null));

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void testLoadUserByUsername_WithEmptyUsername_ShouldThrowException() {
        assertThrows(UsernameNotFoundException.class, () ->
            service.loadUserByUsername(""));

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void testLoadUserByUsername_WithWhitespaceUsername_ShouldThrowException() {
        assertThrows(UsernameNotFoundException.class, () ->
            service.loadUserByUsername("   "));

        verify(userRepository, never()).findByUsername(any());
    }

    // ========== AUTHORITIES/ROLES TESTS ==========

    @Test
    void testLoadUserByUsername_ShouldConvertRolesToAuthorities() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities);
        assertEquals(2, authorities.size());

        Set<String> authorityNames = new HashSet<>();
        authorities.forEach(auth -> authorityNames.add(auth.getAuthority()));
        assertTrue(authorityNames.contains("ROLE_USER"));
        assertTrue(authorityNames.contains("ROLE_TEACHER"));
    }

    @Test
    void testLoadUserByUsername_WithNoRoles_ShouldReturnEmptyAuthorities() {
        testUser.setRoles(new HashSet<>());
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void testLoadUserByUsername_WithNullRoles_ShouldReturnEmptyAuthorities() {
        testUser.setRoles(null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void testLoadUserByUsername_WithSingleRole_ShouldReturnSingleAuthority() {
        testUser.setRoles(new HashSet<>(Collections.singletonList("ROLE_ADMIN")));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void testLoadUserByUsername_WithMultipleRoles_ShouldReturnAllAuthorities() {
        Set<String> roles = new HashSet<>(Arrays.asList(
            "ROLE_USER", "ROLE_TEACHER", "ROLE_ADMIN", "ROLE_PRINCIPAL"
        ));
        testUser.setRoles(roles);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(4, authorities.size());
    }

    // ========== ACCOUNT STATUS TESTS ==========

    @Test
    void testLoadUserByUsername_WithDisabledAccount_ShouldReturnDisabledUserDetails() {
        testUser.setEnabled(false);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertFalse(result.isEnabled());
    }

    @Test
    void testLoadUserByUsername_WithExpiredAccount_ShouldReturnExpiredUserDetails() {
        testUser.setAccountNonExpired(false);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertFalse(result.isAccountNonExpired());
    }

    @Test
    void testLoadUserByUsername_WithExpiredCredentials_ShouldReturnExpiredCredentialsUserDetails() {
        testUser.setCredentialsNonExpired(false);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertFalse(result.isCredentialsNonExpired());
    }

    @Test
    void testLoadUserByUsername_WithLockedAccount_ShouldReturnLockedUserDetails() {
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertFalse(result.isAccountNonLocked());
    }

    // ========== NULL SAFETY TESTS ==========

    @Test
    void testLoadUserByUsername_WithNullPassword_ShouldUseEmptyString() {
        testUser.setPassword(null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertEquals("", result.getPassword());
    }

    @Test
    void testLoadUserByUsername_WithNullEnabled_ShouldDefaultToFalse() {
        testUser.setEnabled(null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertFalse(result.isEnabled());
    }

    @Test
    void testLoadUserByUsername_WithNullAccountNonExpired_ShouldDefaultToTrue() {
        testUser.setAccountNonExpired(null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertTrue(result.isAccountNonExpired());
    }

    @Test
    void testLoadUserByUsername_WithNullCredentialsNonExpired_ShouldDefaultToTrue() {
        testUser.setCredentialsNonExpired(null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertTrue(result.isCredentialsNonExpired());
    }

    @Test
    void testLoadUserByUsername_WithNullAccountNonLocked_ShouldDefaultToTrue() {
        testUser.setAccountNonLocked(null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        assertTrue(result.isAccountNonLocked());
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void testLoadUserByUsername_WithRolesContainingNulls_ShouldFilterNulls() {
        Set<String> rolesWithNulls = new HashSet<>(Arrays.asList("ROLE_USER", null, "ROLE_TEACHER"));
        testUser.setRoles(rolesWithNulls);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(2, authorities.size()); // null should be filtered out
    }

    @Test
    void testLoadUserByUsername_WithRolesContainingEmptyStrings_ShouldFilterEmptyStrings() {
        Set<String> rolesWithEmpty = new HashSet<>(Arrays.asList("ROLE_USER", "", "   ", "ROLE_TEACHER"));
        testUser.setRoles(rolesWithEmpty);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(2, authorities.size()); // empty strings should be filtered out
    }

    @Test
    void testLoadUserByUsername_WithCaseSensitiveUsername_ShouldMatchExact() {
        when(userRepository.findByUsername("JohnDoe")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
            service.loadUserByUsername("JohnDoe"));

        verify(userRepository, times(1)).findByUsername("JohnDoe");
        verify(userRepository, never()).findByUsername("johndoe");
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testLoadUserByUsername_WithCompleteUser_ShouldReturnCompleteUserDetails() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("johndoe");

        // Verify all fields are correctly mapped
        assertNotNull(result);
        assertEquals("johndoe", result.getUsername());
        assertEquals("$2a$10$hashedpassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isCredentialsNonExpired());
        assertTrue(result.isAccountNonLocked());

        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities);
        assertEquals(2, authorities.size());
    }

    @Test
    void testLoadUserByUsername_WithMinimalUser_ShouldReturnUserDetailsWithDefaults() {
        // Create minimal user with only required fields
        User minimalUser = new User();
        minimalUser.setUsername("minimal");
        minimalUser.setPassword("pass");
        // User entity has default: enabled = true

        when(userRepository.findByUsername("minimal")).thenReturn(Optional.of(minimalUser));

        UserDetails result = service.loadUserByUsername("minimal");

        assertNotNull(result);
        assertEquals("minimal", result.getUsername());
        assertEquals("pass", result.getPassword());
        // User entity defaults: enabled=true, accountNonExpired=true, etc.
        assertTrue(result.isEnabled()); // User default is true
        assertTrue(result.isAccountNonExpired()); // User default is true
        assertTrue(result.isCredentialsNonExpired()); // null → true (service default)
        assertTrue(result.isAccountNonLocked()); // User default is true
    }

    @Test
    void testLoadUserByUsername_MultipleCallsSameUser_ShouldQueryRepositoryEachTime() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        service.loadUserByUsername("johndoe");
        service.loadUserByUsername("johndoe");
        service.loadUserByUsername("johndoe");

        // Should query repository 3 times (no caching in service)
        verify(userRepository, times(3)).findByUsername("johndoe");
    }

    @Test
    void testLoadUserByUsername_DifferentUsers_ShouldReturnDifferentUserDetails() {
        User user2 = new User();
        user2.setUsername("janedoe");
        user2.setPassword("$2a$10$differenthash");
        user2.setRoles(new HashSet<>(Collections.singletonList("ROLE_ADMIN")));

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(user2));

        UserDetails result1 = service.loadUserByUsername("johndoe");
        UserDetails result2 = service.loadUserByUsername("janedoe");

        assertNotEquals(result1.getUsername(), result2.getUsername());
        assertNotEquals(result1.getPassword(), result2.getPassword());
        assertNotEquals(result1.getAuthorities().size(), result2.getAuthorities().size());
    }
}
