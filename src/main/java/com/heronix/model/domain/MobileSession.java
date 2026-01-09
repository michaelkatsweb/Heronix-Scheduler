package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Mobile Session Entity
 * Tracks mobile app sessions for the student/parent mobile app.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 17 - Mobile API Foundation
 */
@Entity
@Table(name = "mobile_sessions", indexes = {
    @Index(name = "idx_mobile_session_token", columnList = "access_token"),
    @Index(name = "idx_mobile_session_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private PortalUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student; // For student self-access

    @Column(name = "access_token", nullable = false, unique = true, length = 512)
    private String accessToken;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    @Builder.Default
    private DeviceType deviceType = DeviceType.UNKNOWN;

    @Column(name = "device_os")
    private String deviceOs; // iOS 17.0, Android 14

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "push_token", length = 512)
    private String pushNotificationToken;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "revoked")
    @Builder.Default
    private Boolean revoked = false;

    public enum DeviceType {
        IOS,
        ANDROID,
        WEB,
        TABLET,
        UNKNOWN
    }

    @Transient
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @Transient
    public boolean isValid() {
        return active && !revoked && !isExpired();
    }
}
