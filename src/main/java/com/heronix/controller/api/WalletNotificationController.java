package com.heronix.controller.api;

import com.heronix.model.domain.WalletNotification;
import com.heronix.model.domain.WalletNotification.NotificationStatus;
import com.heronix.model.domain.WalletNotification.NotificationType;
import com.heronix.service.WalletNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wallet Notification REST API Controller
 *
 * Provides REST endpoints for wallet notification management,
 * notification sending, and statistics
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@RestController
@RequestMapping("/api/wallet-notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow EduPro-Teacher portal access
public class WalletNotificationController {

    private final WalletNotificationService notificationService;

    // ============================================================================
    // NOTIFICATION RETRIEVAL ENDPOINTS
    // ============================================================================

    /**
     * Get all notifications for teacher
     * GET /api/wallet-notifications/teacher/{teacherId}
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<WalletNotification>> getNotificationsForTeacher(@PathVariable Long teacherId) {
        log.info("GET /api/wallet-notifications/teacher/{} - Fetching notifications", teacherId);
        List<WalletNotification> notifications = notificationService.getNotificationsForTeacher(teacherId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for teacher
     * GET /api/wallet-notifications/teacher/{teacherId}/unread
     */
    @GetMapping("/teacher/{teacherId}/unread")
    public ResponseEntity<List<WalletNotification>> getUnreadNotifications(@PathVariable Long teacherId) {
        log.info("GET /api/wallet-notifications/teacher/{}/unread - Fetching unread", teacherId);
        List<WalletNotification> notifications = notificationService.getUnreadNotifications(teacherId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get high-priority notifications for teacher
     * GET /api/wallet-notifications/teacher/{teacherId}/high-priority
     */
    @GetMapping("/teacher/{teacherId}/high-priority")
    public ResponseEntity<List<WalletNotification>> getHighPriorityNotifications(@PathVariable Long teacherId) {
        log.info("GET /api/wallet-notifications/teacher/{}/high-priority", teacherId);
        List<WalletNotification> notifications = notificationService.getHighPriorityNotifications(teacherId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications requiring action
     * GET /api/wallet-notifications/teacher/{teacherId}/action-required
     */
    @GetMapping("/teacher/{teacherId}/action-required")
    public ResponseEntity<List<WalletNotification>> getNotificationsRequiringAction(@PathVariable Long teacherId) {
        log.info("GET /api/wallet-notifications/teacher/{}/action-required", teacherId);
        List<WalletNotification> notifications = notificationService.getNotificationsRequiringAction(teacherId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications by type
     * GET /api/wallet-notifications/teacher/{teacherId}/type/{type}
     */
    @GetMapping("/teacher/{teacherId}/type/{type}")
    public ResponseEntity<List<WalletNotification>> getNotificationsByType(@PathVariable Long teacherId,
                                                                             @PathVariable NotificationType type) {
        log.info("GET /api/wallet-notifications/teacher/{}/type/{}", teacherId, type);
        List<WalletNotification> notifications = notificationService.getNotificationsByType(teacherId, type);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get recent notifications (last N days)
     * GET /api/wallet-notifications/teacher/{teacherId}/recent?days=7
     */
    @GetMapping("/teacher/{teacherId}/recent")
    public ResponseEntity<List<WalletNotification>> getRecentNotifications(@PathVariable Long teacherId,
                                                                             @RequestParam(defaultValue = "7") int days) {
        log.info("GET /api/wallet-notifications/teacher/{}/recent?days={}", teacherId, days);
        List<WalletNotification> notifications = notificationService.getRecentNotifications(teacherId, days);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications for transaction
     * GET /api/wallet-notifications/transaction/{transactionId}
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<WalletNotification>> getNotificationsForTransaction(@PathVariable Long transactionId) {
        log.info("GET /api/wallet-notifications/transaction/{}", transactionId);
        List<WalletNotification> notifications = notificationService.getNotificationsForTransaction(transactionId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notification by ID
     * GET /api/wallet-notifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<WalletNotification> getNotificationById(@PathVariable Long id) {
        log.info("GET /api/wallet-notifications/{}", id);
        return notificationService.getNotificationById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================================
    // NOTIFICATION MANAGEMENT ENDPOINTS
    // ============================================================================

    /**
     * Mark notification as read
     * POST /api/wallet-notifications/{id}/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        log.info("POST /api/wallet-notifications/{}/read - Marking as read", id);
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mark all notifications as read for teacher
     * POST /api/wallet-notifications/teacher/{teacherId}/read-all
     */
    @PostMapping("/teacher/{teacherId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long teacherId) {
        log.info("POST /api/wallet-notifications/teacher/{}/read-all", teacherId);
        try {
            notificationService.markAllAsRead(teacherId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error marking all notifications as read", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Archive notification
     * POST /api/wallet-notifications/{id}/archive
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archiveNotification(@PathVariable Long id) {
        log.info("POST /api/wallet-notifications/{}/archive", id);
        try {
            notificationService.archiveNotification(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error archiving notification", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete notification
     * DELETE /api/wallet-notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        log.info("DELETE /api/wallet-notifications/{}", id);
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting notification", e);
            return ResponseEntity.notFound().build();
        }
    }

    // ============================================================================
    // NOTIFICATION SENDING ENDPOINTS
    // ============================================================================

    /**
     * Send fund allocated notification
     * POST /api/wallet-notifications/send/fund-allocated
     */
    @PostMapping("/send/fund-allocated")
    public ResponseEntity<WalletNotification> sendFundAllocatedNotification(
            @RequestParam Long teacherId,
            @RequestParam Long walletId,
            @RequestParam BigDecimal amount,
            @RequestParam String source) {
        log.info("POST /api/wallet-notifications/send/fund-allocated - teacher: {}, amount: {}",
            teacherId, amount);
        WalletNotification notification = notificationService.notifyFundAllocated(
            teacherId, walletId, amount, source);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Send fund approved notification
     * POST /api/wallet-notifications/send/fund-approved
     */
    @PostMapping("/send/fund-approved")
    public ResponseEntity<WalletNotification> sendFundApprovedNotification(
            @RequestParam Long teacherId,
            @RequestParam Long walletId,
            @RequestParam BigDecimal amount,
            @RequestParam String approvedBy) {
        log.info("POST /api/wallet-notifications/send/fund-approved - teacher: {}, amount: {}",
            teacherId, amount);
        WalletNotification notification = notificationService.notifyFundApproved(
            teacherId, walletId, amount, approvedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Send purchase approved notification
     * POST /api/wallet-notifications/send/purchase-approved
     */
    @PostMapping("/send/purchase-approved")
    public ResponseEntity<WalletNotification> sendPurchaseApprovedNotification(
            @RequestParam Long teacherId,
            @RequestParam Long transactionId,
            @RequestParam String vendorName,
            @RequestParam BigDecimal amount) {
        log.info("POST /api/wallet-notifications/send/purchase-approved - teacher: {}, vendor: {}",
            teacherId, vendorName);
        WalletNotification notification = notificationService.notifyPurchaseApproved(
            teacherId, transactionId, vendorName, amount);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Send purchase denied notification
     * POST /api/wallet-notifications/send/purchase-denied
     */
    @PostMapping("/send/purchase-denied")
    public ResponseEntity<WalletNotification> sendPurchaseDeniedNotification(
            @RequestParam Long teacherId,
            @RequestParam Long transactionId,
            @RequestParam String vendorName,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {
        log.info("POST /api/wallet-notifications/send/purchase-denied - teacher: {}, reason: {}",
            teacherId, reason);
        WalletNotification notification = notificationService.notifyPurchaseDenied(
            teacherId, transactionId, vendorName, amount, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Send balance low notification
     * POST /api/wallet-notifications/send/balance-low
     */
    @PostMapping("/send/balance-low")
    public ResponseEntity<WalletNotification> sendBalanceLowNotification(
            @RequestParam Long teacherId,
            @RequestParam Long walletId,
            @RequestParam BigDecimal currentBalance,
            @RequestParam BigDecimal threshold) {
        log.info("POST /api/wallet-notifications/send/balance-low - teacher: {}, balance: {}",
            teacherId, currentBalance);
        WalletNotification notification = notificationService.notifyBalanceLow(
            teacherId, walletId, currentBalance, threshold);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Send vendor added notification
     * POST /api/wallet-notifications/send/vendor-added
     */
    @PostMapping("/send/vendor-added")
    public ResponseEntity<WalletNotification> sendVendorAddedNotification(
            @RequestParam Long teacherId,
            @RequestParam String vendorName,
            @RequestParam String categoryName) {
        log.info("POST /api/wallet-notifications/send/vendor-added - teacher: {}, vendor: {}",
            teacherId, vendorName);
        WalletNotification notification = notificationService.notifyVendorAdded(
            teacherId, vendorName, categoryName);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Send custom notification
     * POST /api/wallet-notifications
     */
    @PostMapping
    public ResponseEntity<WalletNotification> createNotification(@Valid @RequestBody WalletNotification notification) {
        log.info("POST /api/wallet-notifications - Creating custom notification for teacher: {}",
            notification.getTeacherId());
        WalletNotification created = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Send notification to multiple teachers
     * POST /api/wallet-notifications/send/bulk
     */
    @PostMapping("/send/bulk")
    public ResponseEntity<Void> sendBulkNotification(
            @RequestParam List<Long> teacherIds,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam NotificationType type,
            @RequestParam(defaultValue = "2") int priority) {
        log.info("POST /api/wallet-notifications/send/bulk - Sending to {} teachers", teacherIds.size());
        try {
            notificationService.notifyMultipleTeachers(teacherIds, title, message, type, priority);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            log.error("Error sending bulk notification", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================================================================
    // STATISTICS ENDPOINTS
    // ============================================================================

    /**
     * Get notification counts for teacher
     * GET /api/wallet-notifications/teacher/{teacherId}/counts
     */
    @GetMapping("/teacher/{teacherId}/counts")
    public ResponseEntity<Map<String, Object>> getNotificationCounts(@PathVariable Long teacherId) {
        log.info("GET /api/wallet-notifications/teacher/{}/counts", teacherId);

        Map<String, Object> counts = new HashMap<>();
        counts.put("unread", notificationService.countUnreadNotifications(teacherId));
        counts.put("highPriorityUnread", notificationService.countHighPriorityUnread(teacherId));
        counts.put("hasUnread", notificationService.hasUnreadNotifications(teacherId));
        counts.put("hasHighPriority", notificationService.hasHighPriorityNotifications(teacherId));

        return ResponseEntity.ok(counts);
    }

    /**
     * Get unread count badge
     * GET /api/wallet-notifications/teacher/{teacherId}/unread-count
     */
    @GetMapping("/teacher/{teacherId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long teacherId) {
        log.info("GET /api/wallet-notifications/teacher/{}/unread-count", teacherId);
        long count = notificationService.countUnreadNotifications(teacherId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // MAINTENANCE ENDPOINTS
    // ============================================================================

    /**
     * Cleanup old archived notifications
     * POST /api/wallet-notifications/cleanup?daysToKeep=90
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldNotifications(
            @RequestParam(defaultValue = "90") int daysToKeep) {
        log.info("POST /api/wallet-notifications/cleanup?daysToKeep={}", daysToKeep);
        try {
            notificationService.cleanupOldNotifications(daysToKeep);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error cleaning up notifications", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
