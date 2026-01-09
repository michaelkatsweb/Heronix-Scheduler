// Location: src/main/java/com/eduscheduler/model/domain/User.java
package com.heronix.model.domain;

import com.heronix.model.enums.Permission;
import com.heronix.model.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity - Represents system users with authentication
 * Location: src/main/java/com/eduscheduler/model/domain/User.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password; // BCrypt encoded

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 100)
    private String fullName;

    @Column(length = 20)
    private String phoneNumber;

    // Primary role - using enum for type safety
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_role", length = 20)
    private Role primaryRole;

    // Legacy string-based roles (kept for backward compatibility)
    @ElementCollection
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    // Granular permissions
    @ElementCollection
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add("ROLE_USER");
        }
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Add a role to user
     */
    public void addRole(String role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * Remove a role from user
     */
    public void removeRole(String role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
        this.failedLoginAttempts = 0;
    }

    /**
     * Increment failed login attempts
     */
    public void incrementFailedLoginAttempts() {
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        failedLoginAttempts++;

        // Lock account after 5 failed attempts
        if (failedLoginAttempts >= 5) {
            accountNonLocked = false;
        }
    }

    /**
     * Reset failed login attempts
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountNonLocked = true;
    }

    // ========================================================================
    // NEW PERMISSION-BASED METHODS
    // ========================================================================

    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        // Super admin has all permissions
        if (primaryRole == Role.SUPER_ADMIN) {
            return true;
        }
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Add a permission to user
     */
    public void addPermission(Permission permission) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }

    /**
     * Remove a permission from user
     */
    public void removePermission(Permission permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }

    /**
     * Check if user can access student medical data
     */
    public boolean canAccessMedicalData() {
        return primaryRole == Role.SUPER_ADMIN ||
               primaryRole == Role.ADMIN ||
               primaryRole == Role.COUNSELOR ||
               hasPermission(Permission.STUDENT_MEDICAL_VIEW);
    }

    /**
     * Check if user can access IEP/504 data
     */
    public boolean canAccessIEPData() {
        return primaryRole == Role.SUPER_ADMIN ||
               primaryRole == Role.ADMIN ||
               primaryRole == Role.COUNSELOR ||
               hasPermission(Permission.STUDENT_IEP_VIEW);
    }

    /**
     * Check if user has edit capabilities
     */
    public boolean canEdit() {
        if (primaryRole == null) return false;
        return primaryRole.canEditData();
    }

    /**
     * Check if user can view all data
     */
    public boolean canViewAll() {
        if (primaryRole == null) return false;
        return primaryRole.canViewAllData();
    }

    /**
     * Get role display name
     */
    public String getRoleDisplayName() {
        return primaryRole != null ? primaryRole.getDisplayName() : "No Role";
    }
}
