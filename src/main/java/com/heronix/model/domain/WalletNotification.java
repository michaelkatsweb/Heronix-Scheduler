package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wallet Notification Entity
 *
 * Tracks notifications for fund availability, approvals, and wallet events.
 *
 * Features:
 * - Multiple notification types
 * - Priority levels
 * - Read/unread tracking
 * - Action required flags
 * - Complete audit trail
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - Class Wallet Vendor Management
 */
@Entity
@Table(name = "wallet_notifications", indexes = {
    @Index(name = "idx_notification_teacher", columnList = "teacher_id"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_priority", columnList = "priority"),
    @Index(name = "idx_notification_sent", columnList = "sent_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // TARGET
    // ========================================================================

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "wallet_id")
    private Long walletId;

    // ========================================================================
    // NOTIFICATION DETAILS
    // ========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    // ========================================================================
    // RELATED TRANSACTION
    // ========================================================================

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    // ========================================================================
    // STATUS
    // ========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 2; // 1=Low, 2=Medium, 3=High, 4=Urgent

    // ========================================================================
    // ACTION REQUIRED
    // ========================================================================

    @Column(name = "requires_action")
    @Builder.Default
    private Boolean requiresAction = false;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "action_label", length = 100)
    private String actionLabel;

    // ========================================================================
    // TIMESTAMPS
    // ========================================================================

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    // ========================================================================
    // METADATA
    // ========================================================================

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional context

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum NotificationType {
        FUND_ALLOCATED("Fund Allocated"),
        FUND_APPROVED("Fund Approved"),
        FUND_DENIED("Fund Denied"),
        FUND_EXPIRING("Fund Expiring Soon"),
        PURCHASE_APPROVED("Purchase Approved"),
        PURCHASE_DENIED("Purchase Denied"),
        PURCHASE_PENDING("Purchase Pending Review"),
        BALANCE_LOW("Balance Running Low"),
        VENDOR_ADDED("New Vendor Available"),
        VENDOR_REMOVED("Vendor No Longer Available"),
        SYSTEM_ALERT("System Alert");

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum NotificationStatus {
        UNREAD("Unread"),
        READ("Read"),
        ARCHIVED("Archived");

        private final String displayName;

        NotificationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ========================================================================
    // BUSINESS METHODS
    // ========================================================================

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Archive notification
     */
    public void archive() {
        this.status = NotificationStatus.ARCHIVED;
        this.archivedAt = LocalDateTime.now();
    }

    /**
     * Check if notification is unread
     */
    public boolean isUnread() {
        return status == NotificationStatus.UNREAD;
    }

    /**
     * Check if notification is high priority
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 3;
    }

    /**
     * Get priority label
     */
    public String getPriorityLabel() {
        if (priority == null) return "Medium";
        return switch (priority) {
            case 1 -> "Low";
            case 2 -> "Medium";
            case 3 -> "High";
            case 4 -> "Urgent";
            default -> "Medium";
        };
    }

    /**
     * Get notification icon based on type
     */
    public String getIcon() {
        if (notificationType == null) return "🔔";
        return switch (notificationType) {
            case FUND_ALLOCATED -> "💰";
            case FUND_APPROVED -> "✅";
            case FUND_DENIED -> "❌";
            case FUND_EXPIRING -> "⏰";
            case PURCHASE_APPROVED -> "🛒";
            case PURCHASE_DENIED -> "🚫";
            case PURCHASE_PENDING -> "⏳";
            case BALANCE_LOW -> "⚠️";
            case VENDOR_ADDED -> "🏪";
            case VENDOR_REMOVED -> "🔒";
            case SYSTEM_ALERT -> "📢";
        };
    }

    @Override
    public String toString() {
        return String.format("WalletNotification{id=%d, teacherId=%d, type=%s, status=%s, priority=%d}",
            id, teacherId, notificationType, status, priority);
    }
}
