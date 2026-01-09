package com.heronix.service;

import com.heronix.model.domain.WalletNotification;
import com.heronix.model.domain.WalletNotification.NotificationStatus;
import com.heronix.model.domain.WalletNotification.NotificationType;
import com.heronix.repository.WalletNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Wallet Notification Service
 *
 * Business logic for sending and managing wallet notifications to teachers
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WalletNotificationService {

    private final WalletNotificationRepository notificationRepository;

    // ============================================================================
    // NOTIFICATION CREATION & SENDING
    // ============================================================================

    /**
     * Send fund allocated notification
     */
    public WalletNotification notifyFundAllocated(Long teacherId, Long walletId, BigDecimal amount, String source) {
        log.info("Sending fund allocated notification to teacher: {} for amount: {}", teacherId, amount);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .walletId(walletId)
            .notificationType(NotificationType.FUND_ALLOCATED)
            .title("Funds Allocated to Your Classroom Wallet")
            .message(String.format("You have been allocated $%.2f for classroom supplies from %s. " +
                "You can now use these funds to purchase approved items from district vendors.",
                amount, source))
            .amount(amount)
            .priority(3) // High priority
            .requiresAction(true)
            .actionUrl("/class-wallet")
            .actionLabel("View Wallet")
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send fund approved notification
     */
    public WalletNotification notifyFundApproved(Long teacherId, Long walletId, BigDecimal amount, String approvedBy) {
        log.info("Sending fund approved notification to teacher: {}", teacherId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .walletId(walletId)
            .notificationType(NotificationType.FUND_APPROVED)
            .title("Fund Request Approved")
            .message(String.format("Your fund request for $%.2f has been approved by %s. " +
                "The funds are now available in your classroom wallet.",
                amount, approvedBy))
            .amount(amount)
            .priority(3) // High priority
            .requiresAction(true)
            .actionUrl("/class-wallet")
            .actionLabel("View Wallet")
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send fund denied notification
     */
    public WalletNotification notifyFundDenied(Long teacherId, Long walletId, BigDecimal amount, String reason) {
        log.info("Sending fund denied notification to teacher: {}", teacherId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .walletId(walletId)
            .notificationType(NotificationType.FUND_DENIED)
            .title("Fund Request Denied")
            .message(String.format("Your fund request for $%.2f has been denied. Reason: %s. " +
                "Please contact your administrator for more information.",
                amount, reason))
            .amount(amount)
            .priority(3) // High priority
            .requiresAction(true)
            .actionUrl("/class-wallet")
            .actionLabel("View Details")
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send purchase approved notification
     */
    public WalletNotification notifyPurchaseApproved(Long teacherId, Long transactionId,
                                                      String vendorName, BigDecimal amount) {
        log.info("Sending purchase approved notification to teacher: {} for transaction: {}",
            teacherId, transactionId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .transactionId(transactionId)
            .notificationType(NotificationType.PURCHASE_APPROVED)
            .title("Purchase Approved")
            .message(String.format("Your purchase from %s for $%.2f has been approved. " +
                "You may proceed with placing your order.",
                vendorName, amount))
            .amount(amount)
            .priority(2) // Medium priority
            .requiresAction(true)
            .actionUrl("/class-wallet/transactions/" + transactionId)
            .actionLabel("View Transaction")
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send purchase denied notification
     */
    public WalletNotification notifyPurchaseDenied(Long teacherId, Long transactionId,
                                                     String vendorName, BigDecimal amount, String reason) {
        log.info("Sending purchase denied notification to teacher: {} for transaction: {}",
            teacherId, transactionId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .transactionId(transactionId)
            .notificationType(NotificationType.PURCHASE_DENIED)
            .title("Purchase Denied")
            .message(String.format("Your purchase from %s for $%.2f has been denied. Reason: %s. " +
                "Please contact your administrator if you have questions.",
                vendorName, amount, reason))
            .amount(amount)
            .priority(3) // High priority
            .requiresAction(true)
            .actionUrl("/class-wallet/transactions/" + transactionId)
            .actionLabel("View Details")
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send purchase pending notification
     */
    public WalletNotification notifyPurchasePending(Long teacherId, Long transactionId,
                                                      String vendorName, BigDecimal amount) {
        log.info("Sending purchase pending notification to teacher: {}", teacherId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .transactionId(transactionId)
            .notificationType(NotificationType.PURCHASE_PENDING)
            .title("Purchase Awaiting Approval")
            .message(String.format("Your purchase from %s for $%.2f is pending approval. " +
                "You will be notified once a decision is made.",
                vendorName, amount))
            .amount(amount)
            .priority(1) // Low priority
            .requiresAction(false)
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send balance low notification
     */
    public WalletNotification notifyBalanceLow(Long teacherId, Long walletId, BigDecimal currentBalance,
                                                 BigDecimal threshold) {
        log.info("Sending balance low notification to teacher: {}, balance: {}", teacherId, currentBalance);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .walletId(walletId)
            .notificationType(NotificationType.BALANCE_LOW)
            .title("Wallet Balance Running Low")
            .message(String.format("Your classroom wallet balance is now $%.2f, which is below the $%.2f threshold. " +
                "Consider planning your purchases accordingly or requesting additional funds.",
                currentBalance, threshold))
            .amount(currentBalance)
            .priority(2) // Medium priority
            .requiresAction(false)
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send fund expiring notification
     */
    public WalletNotification notifyFundExpiring(Long teacherId, Long walletId, BigDecimal amount,
                                                   LocalDateTime expirationDate) {
        log.info("Sending fund expiring notification to teacher: {}", teacherId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .walletId(walletId)
            .notificationType(NotificationType.FUND_EXPIRING)
            .title("Funds Expiring Soon")
            .message(String.format("Your classroom wallet funds of $%.2f will expire on %s. " +
                "Please use these funds before the expiration date or they will be forfeited.",
                amount, expirationDate.toLocalDate()))
            .amount(amount)
            .priority(4) // Urgent
            .requiresAction(true)
            .actionUrl("/class-wallet")
            .actionLabel("Use Funds Now")
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send vendor added notification
     */
    public WalletNotification notifyVendorAdded(Long teacherId, String vendorName, String categoryName) {
        log.info("Sending vendor added notification to teacher: {}", teacherId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .notificationType(NotificationType.VENDOR_ADDED)
            .title("New Vendor Available")
            .message(String.format("A new vendor '%s' in the %s category is now available for purchases. " +
                "Check out their offerings for your classroom needs.",
                vendorName, categoryName))
            .priority(1) // Low priority
            .requiresAction(false)
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send vendor removed notification
     */
    public WalletNotification notifyVendorRemoved(Long teacherId, String vendorName, String reason) {
        log.info("Sending vendor removed notification to teacher: {}", teacherId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .notificationType(NotificationType.VENDOR_REMOVED)
            .title("Vendor No Longer Available")
            .message(String.format("The vendor '%s' is no longer available for purchases. Reason: %s. " +
                "Please select an alternative vendor for your classroom needs.",
                vendorName, reason))
            .priority(2) // Medium priority
            .requiresAction(false)
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send system alert notification
     */
    public WalletNotification notifySystemAlert(Long teacherId, String title, String message, int priority) {
        log.info("Sending system alert to teacher: {}", teacherId);

        WalletNotification notification = WalletNotification.builder()
            .teacherId(teacherId)
            .notificationType(NotificationType.SYSTEM_ALERT)
            .title(title)
            .message(message)
            .priority(priority)
            .requiresAction(false)
            .status(NotificationStatus.UNREAD)
            .sentAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Create custom notification
     */
    public WalletNotification createNotification(WalletNotification notification) {
        log.info("Creating custom notification for teacher: {}", notification.getTeacherId());

        notification.setStatus(NotificationStatus.UNREAD);
        notification.setSentAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    // ============================================================================
    // NOTIFICATION MANAGEMENT
    // ============================================================================

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);

        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications as read for teacher
     */
    public void markAllAsRead(Long teacherId) {
        log.info("Marking all notifications as read for teacher: {}", teacherId);

        List<WalletNotification> unreadNotifications = notificationRepository
            .findByTeacherIdAndStatusOrderBySentAtDesc(teacherId, NotificationStatus.UNREAD);

        unreadNotifications.forEach(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });

        log.info("Marked {} notifications as read", unreadNotifications.size());
    }

    /**
     * Archive notification
     */
    public void archiveNotification(Long notificationId) {
        log.info("Archiving notification: {}", notificationId);

        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.archive();
            notificationRepository.save(notification);
        });
    }

    /**
     * Delete notification
     */
    public void deleteNotification(Long notificationId) {
        log.info("Deleting notification: {}", notificationId);
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Delete old archived notifications
     */
    public void cleanupOldNotifications(int daysToKeep) {
        log.info("Cleaning up archived notifications older than {} days", daysToKeep);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        notificationRepository.deleteOldArchived(cutoffDate);

        log.info("Old archived notifications cleaned up");
    }

    // ============================================================================
    // NOTIFICATION RETRIEVAL
    // ============================================================================

    /**
     * Get notification by ID
     */
    @Transactional(readOnly = true)
    public Optional<WalletNotification> getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    /**
     * Get all notifications for teacher
     */
    @Transactional(readOnly = true)
    public List<WalletNotification> getNotificationsForTeacher(Long teacherId) {
        return notificationRepository.findByTeacherIdOrderBySentAtDesc(teacherId);
    }

    /**
     * Get unread notifications for teacher
     */
    @Transactional(readOnly = true)
    public List<WalletNotification> getUnreadNotifications(Long teacherId) {
        return notificationRepository.findByTeacherIdAndStatusOrderBySentAtDesc(
            teacherId, NotificationStatus.UNREAD);
    }

    /**
     * Get high-priority notifications
     */
    @Transactional(readOnly = true)
    public List<WalletNotification> getHighPriorityNotifications(Long teacherId) {
        return notificationRepository.findHighPriorityByTeacherId(teacherId);
    }

    /**
     * Get notifications requiring action
     */
    @Transactional(readOnly = true)
    public List<WalletNotification> getNotificationsRequiringAction(Long teacherId) {
        return notificationRepository.findByTeacherIdAndRequiresActionTrueOrderBySentAtDesc(teacherId);
    }

    /**
     * Get notifications by type
     */
    @Transactional(readOnly = true)
    public List<WalletNotification> getNotificationsByType(Long teacherId, NotificationType type) {
        return notificationRepository.findByTeacherIdAndNotificationTypeOrderBySentAtDesc(teacherId, type);
    }

    /**
     * Get recent notifications (last N days)
     */
    @Transactional(readOnly = true)
    public List<WalletNotification> getRecentNotifications(Long teacherId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return notificationRepository.findRecentByTeacherId(teacherId, since);
    }

    /**
     * Get notifications for transaction
     */
    @Transactional(readOnly = true)
    public List<WalletNotification> getNotificationsForTransaction(Long transactionId) {
        return notificationRepository.findByTransactionIdOrderBySentAtDesc(transactionId);
    }

    // ============================================================================
    // NOTIFICATION COUNTS & STATISTICS
    // ============================================================================

    /**
     * Count unread notifications
     */
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long teacherId) {
        return notificationRepository.countByTeacherIdAndStatus(teacherId, NotificationStatus.UNREAD);
    }

    /**
     * Count high-priority unread notifications
     */
    @Transactional(readOnly = true)
    public long countHighPriorityUnread(Long teacherId) {
        return notificationRepository.countHighPriorityUnreadByTeacherId(teacherId);
    }

    /**
     * Check if teacher has unread notifications
     */
    @Transactional(readOnly = true)
    public boolean hasUnreadNotifications(Long teacherId) {
        return countUnreadNotifications(teacherId) > 0;
    }

    /**
     * Check if teacher has high-priority notifications
     */
    @Transactional(readOnly = true)
    public boolean hasHighPriorityNotifications(Long teacherId) {
        return countHighPriorityUnread(teacherId) > 0;
    }

    // ============================================================================
    // BULK OPERATIONS
    // ============================================================================

    /**
     * Send notification to multiple teachers
     */
    public void notifyMultipleTeachers(List<Long> teacherIds, String title, String message,
                                        NotificationType type, int priority) {
        log.info("Sending notification to {} teachers", teacherIds.size());

        teacherIds.forEach(teacherId -> {
            WalletNotification notification = WalletNotification.builder()
                .teacherId(teacherId)
                .notificationType(type)
                .title(title)
                .message(message)
                .priority(priority)
                .status(NotificationStatus.UNREAD)
                .sentAt(LocalDateTime.now())
                .build();

            notificationRepository.save(notification);
        });

        log.info("Sent notification to {} teachers", teacherIds.size());
    }

    /**
     * Send notification to all teachers
     */
    public void notifyAllTeachers(String title, String message, NotificationType type, int priority) {
        log.info("Sending notification to all teachers");
        // This would need TeacherRepository to get all teacher IDs
        // Implementation would be similar to notifyMultipleTeachers
        log.warn("notifyAllTeachers not fully implemented - requires TeacherRepository integration");
    }
}
