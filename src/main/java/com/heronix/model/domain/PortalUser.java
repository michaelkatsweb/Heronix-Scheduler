package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Portal User Entity
 * Represents parent/guardian accounts for the student portal.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Parent/Student Portal
 */
@Entity
@Table(name = "portal_users", indexes = {
    @Index(name = "idx_portal_user_email", columnList = "email", unique = true),
    @Index(name = "idx_portal_user_username", columnList = "username", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    @Builder.Default
    private PortalUserType userType = PortalUserType.PARENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship")
    private ParentRelationship relationship;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "portal_user_students",
        joinColumns = @JoinColumn(name = "portal_user_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private Set<Student> linkedStudents = new HashSet<>();

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_attempts")
    @Builder.Default
    private Integer loginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires")
    private LocalDateTime passwordResetExpires;

    @Column(name = "email_notification_enabled")
    @Builder.Default
    private Boolean emailNotificationEnabled = true;

    @Column(name = "sms_notification_enabled")
    @Builder.Default
    private Boolean smsNotificationEnabled = false;

    @Column(name = "language_preference")
    @Builder.Default
    private String languagePreference = "en";

    @Column(name = "timezone")
    @Builder.Default
    private String timezone = "America/New_York";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PortalUserType {
        STUDENT,   // Student self-access
        PARENT,    // Parent/Guardian
        GUARDIAN,  // Legal guardian
        STAFF      // Staff with portal access
    }

    public enum ParentRelationship {
        MOTHER,
        FATHER,
        STEPMOTHER,
        STEPFATHER,
        GRANDMOTHER,
        GRANDFATHER,
        AUNT,
        UNCLE,
        FOSTER_PARENT,
        LEGAL_GUARDIAN,
        OTHER
    }

    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Transient
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }
}
