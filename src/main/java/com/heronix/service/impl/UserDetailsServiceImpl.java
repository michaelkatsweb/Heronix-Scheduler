// Location: src/main/java/com/eduscheduler/service/impl/UserDetailsServiceImpl.java
package com.heronix.service.impl;

import com.heronix.model.domain.User;
import com.heronix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * UserDetailsService Implementation
 * Loads user-specific data for Spring Security
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ✅ NULL SAFE: Validate username parameter
        if (username == null || username.trim().isEmpty()) {
            log.warn("Cannot load user with null or empty username");
            throw new UsernameNotFoundException("Username cannot be null or empty");
        }

        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        // ✅ NULL SAFE: Validate user object before accessing properties
        if (user == null) {
            throw new UsernameNotFoundException("User object is null for: " + username);
        }

        log.debug("User found: {}, roles: {}", username,
            user.getRoles() != null ? user.getRoles() : "[]");

        // ✅ NULL SAFE: Safe extraction of user properties with defaults
        return new org.springframework.security.core.userdetails.User(
                user.getUsername() != null ? user.getUsername() : username,
                user.getPassword() != null ? user.getPassword() : "",
                user.getEnabled() != null ? user.getEnabled() : false,
                user.getAccountNonExpired() != null ? user.getAccountNonExpired() : true,
                user.getCredentialsNonExpired() != null ? user.getCredentialsNonExpired() : true,
                user.getAccountNonLocked() != null ? user.getAccountNonLocked() : true,
                getAuthorities(user)
        );
    }

    /**
     * Convert user roles to Spring Security authorities
     * ✅ NULL SAFE: This method handles all null scenarios gracefully
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // ✅ NULL SAFE: Check user and roles exist before processing
        if (user == null || user.getRoles() == null) {
            log.debug("User or roles is null, returning empty authorities list");
            return java.util.Collections.emptyList();
        }

        // ✅ NULL SAFE: Filter null/empty roles before creating authorities
        return user.getRoles().stream()
                .filter(role -> role != null && !role.trim().isEmpty())
                .map(role -> {
                    log.trace("Creating authority for role: {}", role);
                    return new SimpleGrantedAuthority(role);
                })
                .collect(Collectors.toList());
    }
}
