package com.heronix.service;

import com.heronix.model.domain.Notification;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing notifications
 *
 * Location: src/main/java/com/eduscheduler/service/NotificationService.java
 */
public interface NotificationService {

    // ========== Create Operations ==========

    /**
     * Create a notification for a specific user
     */
    Notification createNotification(Notification.NotificationType type,
                                    String title,
                                    String message,
                                    Long targetUserId);

    /**
     * Create a notification for a user role
     */
    Notification createNotificationForRole(Notification.NotificationType type,
                                           String title,
                                           String message,
                                           String targetRole);

    /**
     * Create a global notification (all users)
     */
    Notification createGlobalNotification(Notification.NotificationType type,
                                          String title,
                                          String message);

    /**
     * Create a notification with full details
     */
    Notification createDetailedNotification(Notification.NotificationType type,
                                            String title,
                                            String message,
                                            Integer priority,
                                            Long targetUserId,
                                            String targetRole,
                                            String relatedEntityType,
                                            Long relatedEntityId,
                                            String actionUrl);

    // ========== Read Operations ==========

    /**
     * Get all notifications for a user
     */
    List<Notification> getNotificationsForUser(Long userId);

    /**
     * Get unread notifications for a user
     */
    List<Notification> getUnreadNotificationsForUser(Long userId);

    /**
     * Get high-priority unread notifications for a user
     */
    List<Notification> getHighPriorityNotificationsForUser(Long userId);

    /**
     * Get notification by ID
     */
    Optional<Notification> getNotificationById(Long notificationId);

    /**
     * Get notifications by type
     */
    List<Notification> getNotificationsByType(Notification.NotificationType type);

    /**
     * Get notifications related to a specific entity
     */
    List<Notification> getNotificationsByEntity(String entityType, Long entityId);

    /**
     * Get recent notifications (last 7 days)
     */
    List<Notification> getRecentNotifications();

    /**
     * Count unread notifications for a user
     */
    Long countUnreadNotifications(Long userId);

    // ========== Update Operations ==========

    /**
     * Mark notification as read
     */
    void markAsRead(Long notificationId);

    /**
     * Mark all notifications as read for a user
     */
    void markAllAsRead(Long userId);

    /**
     * Dismiss notification
     */
    void dismissNotification(Long notificationId);

    /**
     * Dismiss all notifications for a user
     */
    void dismissAllForUser(Long userId);

    // ========== Delete Operations ==========

    /**
     * Delete notification
     */
    void deleteNotification(Long notificationId);

    /**
     * Clean up expired and old dismissed notifications
     */
    void cleanupOldNotifications();

    // ========== Schedule Change Notifications ==========

    /**
     * Notify about section assignment change
     */
    void notifySectionAssigned(Long sectionId, Long teacherId, String courseName, Integer period);

    /**
     * Notify about teacher change
     */
    void notifyTeacherChanged(Long sectionId, String courseName, String oldTeacher, String newTeacher);

    /**
     * Notify about room change
     */
    void notifyRoomChanged(Long sectionId, String courseName, String oldRoom, String newRoom);

    /**
     * Notify about period change
     */
    void notifyPeriodChanged(Long sectionId, String courseName, Integer oldPeriod, Integer newPeriod);

    // ========== Conflict Notifications ==========

    /**
     * Notify about detected conflict
     */
    void notifyConflictDetected(String conflictType, String description, Long relatedEntityId);

    /**
     * Notify about resolved conflict
     */
    void notifyConflictResolved(String conflictType, String description);

    // ========== Enrollment Notifications ==========

    /**
     * Notify about over-enrollment in section
     */
    void notifyOverEnrollment(Long sectionId, String courseName, int enrolled, int capacity);

    /**
     * Notify about under-enrollment in section
     */
    void notifyUnderEnrollment(Long sectionId, String courseName, int enrolled, int minRequired);

    /**
     * Notify about section full
     */
    void notifySectionFull(Long sectionId, String courseName);

    /**
     * Notify student about enrollment
     */
    void notifyStudentEnrolled(Long studentId, String courseName, Integer period);

    /**
     * Notify student about drop
     */
    void notifyStudentDropped(Long studentId, String courseName, String reason);

    // ========== Administrative Notifications ==========

    /**
     * Notify about teacher overload
     */
    void notifyTeacherOverload(Long teacherId, String teacherName, int currentLoad, int maxLoad);

    /**
     * Notify about room conflict
     */
    void notifyRoomConflict(Long roomId, String roomName, Integer period, List<String> conflictingSections);

    /**
     * Notify about schedule published
     */
    void notifySchedulePublished(String scheduleType, String academicYear);

    /**
     * Notify about validation error
     */
    void notifyValidationError(String errorType, String description, Long relatedEntityId);

    // ========== System Notifications ==========

    /**
     * Send system alert to administrators
     */
    void sendSystemAlert(String title, String message, Integer priority);

    /**
     * Notify about export completion
     */
    void notifyExportComplete(Long userId, String exportType, String fileName);

    /**
     * Notify about import completion
     */
    void notifyImportComplete(Long userId, String importType, int recordsProcessed, int errors);
}
