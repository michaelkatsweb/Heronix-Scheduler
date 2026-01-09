package com.heronix.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification Entity
 * Represents system notifications for users about schedule changes, conflicts, and alerts
 *
 * Location: src/main/java/com/eduscheduler/domain/Notification.java
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of notification
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * Notification title
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Notification message content
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Priority level (1=Low, 2=Medium, 3=High, 4=Critical)
     */
    @Column(nullable = false)
    private Integer priority = 2;

    /**
     * Target user role (null = all users)
     */
    @Column(length = 50)
    private String targetRole;

    /**
     * Target user ID (null = all users with role)
     */
    @Column(name = "target_user_id")
    private Long targetUserId;

    /**
     * Related entity type (e.g., "CourseSection", "Teacher", "Student")
     */
    @Column(length = 100)
    private String relatedEntityType;

    /**
     * Related entity ID for navigation
     */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /**
     * Is the notification read?
     */
    @Column(nullable = false)
    private Boolean isRead = false;

    /**
     * Is the notification dismissed?
     */
    @Column(nullable = false)
    private Boolean isDismissed = false;

    /**
     * Action URL/path for "View Details" button
     */
    @Column(length = 500)
    private String actionUrl;

    /**
     * When the notification was created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the notification was read
     */
    private LocalDateTime readAt;

    /**
     * Optional: When the notification expires/should be auto-dismissed
     */
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Notification Types
     */
    public enum NotificationType {
        // Schedule Changes
        SCHEDULE_CHANGE("Schedule Change", "info"),
        SECTION_ASSIGNED("Section Assigned", "success"),
        SECTION_UNASSIGNED("Section Unassigned", "warning"),
        TEACHER_CHANGED("Teacher Changed", "info"),
        ROOM_CHANGED("Room Changed", "info"),
        PERIOD_CHANGED("Period Changed", "info"),

        // Conflicts & Warnings
        CONFLICT_DETECTED("Conflict Detected", "danger"),
        CONFLICT_RESOLVED("Conflict Resolved", "success"),
        OVER_ENROLLMENT("Over Enrollment", "danger"),
        UNDER_ENROLLMENT("Under Enrollment", "warning"),
        CAPACITY_WARNING("Capacity Warning", "warning"),

        // Administrative Alerts
        SECTION_FULL("Section Full", "warning"),
        SECTION_CANCELLED("Section Cancelled", "danger"),
        TEACHER_OVERLOAD("Teacher Overload", "warning"),
        ROOM_CONFLICT("Room Conflict", "danger"),
        SCHEDULE_PUBLISHED("Schedule Published", "success"),

        // Student-Specific
        STUDENT_ENROLLED("Student Enrolled", "success"),
        STUDENT_DROPPED("Student Dropped", "info"),
        COURSE_REQUEST_DENIED("Course Request Denied", "warning"),
        ALTERNATE_ASSIGNED("Alternate Course Assigned", "info"),

        // System
        SYSTEM_ALERT("System Alert", "info"),
        VALIDATION_ERROR("Validation Error", "danger"),
        EXPORT_COMPLETE("Export Complete", "success"),
        IMPORT_COMPLETE("Import Complete", "success");

        private final String displayName;
        private final String severity; // info, success, warning, danger

        NotificationType(String displayName, String severity) {
            this.displayName = displayName;
            this.severity = severity;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSeverity() {
            return severity;
        }
    }

    // Helper methods

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Mark notification as dismissed
     */
    public void dismiss() {
        this.isDismissed = true;
    }

    /**
     * Check if notification is expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Get formatted time ago string
     */
    public String getTimeAgo() {
        if (createdAt == null) {
            return "Unknown";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createdAt, now).toMinutes();

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (minutes < 1440) {
            long hours = minutes / 60;
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else {
            long days = minutes / 1440;
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }
    }

    /**
     * Get CSS class for notification type
     */
    public String getCssClass() {
        return "notification-" + type.getSeverity();
    }

    /**
     * Get icon for notification type
     */
    public String getIcon() {
        switch (type.getSeverity()) {
            case "success":
                return "✓";
            case "warning":
                return "⚠";
            case "danger":
                return "✖";
            case "info":
            default:
                return "ℹ";
        }
    }
}
