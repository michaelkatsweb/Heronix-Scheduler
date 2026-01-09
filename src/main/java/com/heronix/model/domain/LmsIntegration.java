package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * LMS Integration Entity
 * Stores configuration for Learning Management System integrations.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 17 - LMS Integration Foundation
 */
@Entity
@Table(name = "lms_integrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "lms_type", nullable = false)
    private LmsType lmsType;

    @Column(name = "api_base_url")
    private String apiBaseUrl;

    @Column(name = "api_key", length = 1024)
    private String apiKey;

    @Column(name = "api_secret", length = 1024)
    private String apiSecret;

    @Column(name = "oauth_token", length = 2048)
    private String oauthToken;

    @Column(name = "oauth_refresh_token", length = 1024)
    private String oauthRefreshToken;

    @Column(name = "oauth_expires_at")
    private LocalDateTime oauthExpiresAt;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret", length = 1024)
    private String clientSecret;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 512)
    private String webhookSecret;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sync_grades")
    @Builder.Default
    private Boolean syncGrades = true;

    @Column(name = "sync_assignments")
    @Builder.Default
    private Boolean syncAssignments = true;

    @Column(name = "sync_attendance")
    @Builder.Default
    private Boolean syncAttendance = false;

    @Column(name = "sync_roster")
    @Builder.Default
    private Boolean syncRoster = true;

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    @Column(name = "sync_status")
    private String syncStatus;

    @Column(name = "sync_errors", columnDefinition = "TEXT")
    private String syncErrors;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LmsType {
        CANVAS,
        BLACKBOARD,
        MOODLE,
        SCHOOLOGY,
        GOOGLE_CLASSROOM,
        MICROSOFT_TEAMS,
        BRIGHTSPACE_D2L,
        POWERSCHOOL,
        CLEVER,
        CLASSLINK,
        OTHER
    }

    @Transient
    public boolean isOauthExpired() {
        return oauthExpiresAt != null && LocalDateTime.now().isAfter(oauthExpiresAt);
    }

    @Transient
    public boolean needsReauth() {
        return !active || isOauthExpired() ||
               (oauthToken == null && apiKey == null);
    }

    // Configuration helpers for different LMS types
    @Transient
    public Map<String, String> getConnectionConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("type", lmsType.name());
        config.put("baseUrl", apiBaseUrl);
        config.put("active", String.valueOf(active));
        return config;
    }
}
