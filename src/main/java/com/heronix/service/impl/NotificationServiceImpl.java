package com.heronix.service.impl;

import com.heronix.model.domain.Notification;
import com.heronix.repository.NotificationRepository;
import com.heronix.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of NotificationService
 *
 * Location: src/main/java/com/eduscheduler/service/impl/NotificationServiceImpl.java
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    // ========== Create Operations ==========

    @Override
    public Notification createNotification(Notification.NotificationType type,
                                           String title,
                                           String message,
                                           Long targetUserId) {
        logger.info("Creating notification: {} for user {}", type, targetUserId);

        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetUserId(targetUserId);
        notification.setPriority(getDefaultPriority(type));
        notification.setIsRead(false);
        notification.setIsDismissed(false);

        return notificationRepository.save(notification);
    }

    @Override
    public Notification createNotificationForRole(Notification.NotificationType type,
                                                  String title,
                                                  String message,
                                                  String targetRole) {
        logger.info("Creating notification: {} for role {}", type, targetRole);

        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetRole(targetRole);
        notification.setPriority(getDefaultPriority(type));
        notification.setIsRead(false);
        notification.setIsDismissed(false);

        return notificationRepository.save(notification);
    }

    @Override
    public Notification createGlobalNotification(Notification.NotificationType type,
                                                 String title,
                                                 String message) {
        logger.info("Creating global notification: {}", type);

        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPriority(getDefaultPriority(type));
        notification.setIsRead(false);
        notification.setIsDismissed(false);

        return notificationRepository.save(notification);
    }

    @Override
    public Notification createDetailedNotification(Notification.NotificationType type,
                                                   String title,
                                                   String message,
                                                   Integer priority,
                                                   Long targetUserId,
                                                   String targetRole,
                                                   String relatedEntityType,
                                                   Long relatedEntityId,
                                                   String actionUrl) {
        logger.info("Creating detailed notification: {} for user {} with entity {}:{}",
                type, targetUserId, relatedEntityType, relatedEntityId);

        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPriority(priority != null ? priority : getDefaultPriority(type));
        notification.setTargetUserId(targetUserId);
        notification.setTargetRole(targetRole);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setActionUrl(actionUrl);
        notification.setIsRead(false);
        notification.setIsDismissed(false);

        return notificationRepository.save(notification);
    }

    // ========== Read Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        logger.debug("Getting all notifications for user {}", userId);
        return notificationRepository.findByTargetUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        logger.debug("Getting unread notifications for user {}", userId);
        return notificationRepository.findUnreadByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getHighPriorityNotificationsForUser(Long userId) {
        logger.debug("Getting high-priority notifications for user {}", userId);
        return notificationRepository.findHighPriorityUnreadByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByType(Notification.NotificationType type) {
        logger.debug("Getting notifications by type {}", type);
        return notificationRepository.findByTypeAndIsDismissedFalseOrderByCreatedAtDesc(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByEntity(String entityType, Long entityId) {
        logger.debug("Getting notifications for entity {}:{}", entityType, entityId);
        return notificationRepository.findByRelatedEntity(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return notificationRepository.findRecentNotifications(sevenDaysAgo);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUnreadNotifications(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    // ========== Update Operations ==========

    @Override
    public void markAsRead(Long notificationId) {
        logger.debug("Marking notification {} as read", notificationId);

        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.markAsRead();
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        logger.info("Marking all notifications as read for user {}", userId);

        // ✅ NULL SAFE: Filter null notifications before processing
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        for (Notification notification : unreadNotifications) {
            if (notification != null) {
                notification.markAsRead();
            }
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public void dismissNotification(Long notificationId) {
        logger.debug("Dismissing notification {}", notificationId);

        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.dismiss();
            notificationRepository.save(notification);
        }
    }

    @Override
    public void dismissAllForUser(Long userId) {
        logger.info("Dismissing all notifications for user {}", userId);

        List<Notification> userNotifications = notificationRepository.findByTargetUserId(userId);
        for (Notification notification : userNotifications) {
            notification.dismiss();
        }
        notificationRepository.saveAll(userNotifications);
    }

    // ========== Delete Operations ==========

    @Override
    public void deleteNotification(Long notificationId) {
        logger.info("Deleting notification {}", notificationId);
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public void cleanupOldNotifications() {
        logger.info("Cleaning up old notifications");

        // Auto-dismiss expired notifications
        List<Notification> expiredNotifications = notificationRepository.findExpiredNotifications(LocalDateTime.now());
        for (Notification notification : expiredNotifications) {
            notification.dismiss();
        }
        notificationRepository.saveAll(expiredNotifications);

        // Delete dismissed notifications older than 30 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldDismissedNotifications(cutoffDate);

        logger.info("Cleanup complete: {} expired notifications dismissed", expiredNotifications.size());
    }

    // ========== Schedule Change Notifications ==========

    @Override
    public void notifySectionAssigned(Long sectionId, Long teacherId, String courseName, Integer period) {
        String title = "Section Assigned";
        String message = String.format("You have been assigned to teach %s during period %d",
                courseName, period);

        createDetailedNotification(
                Notification.NotificationType.SECTION_ASSIGNED,
                title,
                message,
                2, // Medium priority
                teacherId,
                null,
                "CourseSection",
                sectionId,
                "/sections/" + sectionId
        );
    }

    @Override
    public void notifyTeacherChanged(Long sectionId, String courseName, String oldTeacher, String newTeacher) {
        String title = "Teacher Changed";
        String message = String.format("%s teacher changed from %s to %s",
                courseName, oldTeacher, newTeacher);

        createDetailedNotification(
                Notification.NotificationType.TEACHER_CHANGED,
                title,
                message,
                2, // Medium priority
                null,
                "ADMIN",
                "CourseSection",
                sectionId,
                "/sections/" + sectionId
        );
    }

    @Override
    public void notifyRoomChanged(Long sectionId, String courseName, String oldRoom, String newRoom) {
        String title = "Room Changed";
        String message = String.format("%s room changed from %s to %s",
                courseName, oldRoom, newRoom);

        createDetailedNotification(
                Notification.NotificationType.ROOM_CHANGED,
                title,
                message,
                2, // Medium priority
                null,
                "ADMIN",
                "CourseSection",
                sectionId,
                "/sections/" + sectionId
        );
    }

    @Override
    public void notifyPeriodChanged(Long sectionId, String courseName, Integer oldPeriod, Integer newPeriod) {
        String title = "Period Changed";
        String message = String.format("%s moved from period %d to period %d",
                courseName, oldPeriod, newPeriod);

        createDetailedNotification(
                Notification.NotificationType.PERIOD_CHANGED,
                title,
                message,
                3, // High priority (schedule changes affect students)
                null,
                "ADMIN",
                "CourseSection",
                sectionId,
                "/sections/" + sectionId
        );
    }

    // ========== Conflict Notifications ==========

    @Override
    public void notifyConflictDetected(String conflictType, String description, Long relatedEntityId) {
        String title = "Conflict Detected: " + conflictType;

        createDetailedNotification(
                Notification.NotificationType.CONFLICT_DETECTED,
                title,
                description,
                4, // Critical priority
                null,
                "ADMIN",
                conflictType,
                relatedEntityId,
                "/conflicts"
        );
    }

    @Override
    public void notifyConflictResolved(String conflictType, String description) {
        String title = "Conflict Resolved: " + conflictType;

        createNotificationForRole(
                Notification.NotificationType.CONFLICT_RESOLVED,
                title,
                description,
                "ADMIN"
        );
    }

    // ========== Enrollment Notifications ==========

    @Override
    public void notifyOverEnrollment(Long sectionId, String courseName, int enrolled, int capacity) {
        String title = "Over-Enrollment Warning";
        String message = String.format("%s is over-enrolled: %d students in a %d-seat section",
                courseName, enrolled, capacity);

        createDetailedNotification(
                Notification.NotificationType.OVER_ENROLLMENT,
                title,
                message,
                4, // Critical priority
                null,
                "ADMIN",
                "CourseSection",
                sectionId,
                "/sections/" + sectionId
        );
    }

    @Override
    public void notifyUnderEnrollment(Long sectionId, String courseName, int enrolled, int minRequired) {
        String title = "Under-Enrollment Warning";
        String message = String.format("%s is under-enrolled: only %d students (minimum: %d)",
                courseName, enrolled, minRequired);

        createDetailedNotification(
                Notification.NotificationType.UNDER_ENROLLMENT,
                title,
                message,
                2, // Medium priority
                null,
                "ADMIN",
                "CourseSection",
                sectionId,
                "/sections/" + sectionId
        );
    }

    @Override
    public void notifySectionFull(Long sectionId, String courseName) {
        String title = "Section Full";
        String message = String.format("%s has reached maximum capacity", courseName);

        createDetailedNotification(
                Notification.NotificationType.SECTION_FULL,
                title,
                message,
                2, // Medium priority
                null,
                "ADMIN",
                "CourseSection",
                sectionId,
                "/sections/" + sectionId
        );
    }

    @Override
    public void notifyStudentEnrolled(Long studentId, String courseName, Integer period) {
        String title = "Enrolled in Course";
        String message = String.format("You have been enrolled in %s (Period %d)",
                courseName, period);

        createDetailedNotification(
                Notification.NotificationType.STUDENT_ENROLLED,
                title,
                message,
                2, // Medium priority
                studentId,
                "STUDENT",
                "Student",
                studentId,
                "/my-schedule"
        );
    }

    @Override
    public void notifyStudentDropped(Long studentId, String courseName, String reason) {
        String title = "Dropped from Course";
        String message = String.format("You have been dropped from %s. Reason: %s",
                courseName, reason);

        createDetailedNotification(
                Notification.NotificationType.STUDENT_DROPPED,
                title,
                message,
                3, // High priority
                studentId,
                "STUDENT",
                "Student",
                studentId,
                "/my-schedule"
        );
    }

    // ========== Administrative Notifications ==========

    @Override
    public void notifyTeacherOverload(Long teacherId, String teacherName, int currentLoad, int maxLoad) {
        String title = "Teacher Overload Warning";
        String message = String.format("%s is overloaded: %d sections assigned (max: %d)",
                teacherName, currentLoad, maxLoad);

        createDetailedNotification(
                Notification.NotificationType.TEACHER_OVERLOAD,
                title,
                message,
                3, // High priority
                null,
                "ADMIN",
                "Teacher",
                teacherId,
                "/teachers/" + teacherId
        );
    }

    @Override
    public void notifyRoomConflict(Long roomId, String roomName, Integer period, List<String> conflictingSections) {
        String title = "Room Conflict Detected";
        // ✅ NULL SAFE: Handle null list for conflicting sections
        String sectionsStr = (conflictingSections != null) ? String.join(", ", conflictingSections) : "Unknown";
        String message = String.format("Room %s has multiple sections assigned during period %d: %s",
                roomName, period, sectionsStr);

        createDetailedNotification(
                Notification.NotificationType.ROOM_CONFLICT,
                title,
                message,
                4, // Critical priority
                null,
                "ADMIN",
                "Room",
                roomId,
                "/rooms/" + roomId
        );
    }

    @Override
    public void notifySchedulePublished(String scheduleType, String academicYear) {
        String title = "Schedule Published";
        String message = String.format("%s schedule for %s has been published and is now visible to students",
                scheduleType, academicYear);

        createNotificationForRole(
                Notification.NotificationType.SCHEDULE_PUBLISHED,
                title,
                message,
                "ADMIN"
        );
    }

    @Override
    public void notifyValidationError(String errorType, String description, Long relatedEntityId) {
        String title = "Validation Error: " + errorType;

        createDetailedNotification(
                Notification.NotificationType.VALIDATION_ERROR,
                title,
                description,
                3, // High priority
                null,
                "ADMIN",
                errorType,
                relatedEntityId,
                "/validation"
        );
    }

    // ========== System Notifications ==========

    @Override
    public void sendSystemAlert(String title, String message, Integer priority) {
        logger.warn("System alert: {}", title);

        createDetailedNotification(
                Notification.NotificationType.SYSTEM_ALERT,
                title,
                message,
                priority != null ? priority : 3,
                null,
                "ADMIN",
                null,
                null,
                null
        );
    }

    @Override
    public void notifyExportComplete(Long userId, String exportType, String fileName) {
        String title = "Export Complete";
        String message = String.format("%s export completed successfully: %s", exportType, fileName);

        createDetailedNotification(
                Notification.NotificationType.EXPORT_COMPLETE,
                title,
                message,
                1, // Low priority
                userId,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void notifyImportComplete(Long userId, String importType, int recordsProcessed, int errors) {
        String title = "Import Complete";
        String message = String.format("%s import completed: %d records processed, %d errors",
                importType, recordsProcessed, errors);

        Integer priority = errors > 0 ? 3 : 1; // High priority if errors

        createDetailedNotification(
                Notification.NotificationType.IMPORT_COMPLETE,
                title,
                message,
                priority,
                userId,
                null,
                null,
                null,
                null
        );
    }

    // ========== Helper Methods ==========

    /**
     * Get default priority based on notification type
     */
    private Integer getDefaultPriority(Notification.NotificationType type) {
        switch (type.getSeverity()) {
            case "danger":
                return 4; // Critical
            case "warning":
                return 3; // High
            case "success":
                return 1; // Low
            case "info":
            default:
                return 2; // Medium
        }
    }
}
