package com.heronix.repository;

import com.heronix.model.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Notification entity
 *
 * Location: src/main/java/com/eduscheduler/repository/NotificationRepository.java
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a specific user (unread first)
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.targetUserId = :userId OR n.targetUserId IS NULL) " +
           "AND n.isDismissed = false " +
           "ORDER BY n.isRead ASC, n.createdAt DESC")
    List<Notification> findByTargetUserId(@Param("userId") Long userId);

    /**
     * Find unread notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.targetUserId = :userId OR n.targetUserId IS NULL) " +
           "AND n.isRead = false AND n.isDismissed = false " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Find notifications by role
     */
    List<Notification> findByTargetRoleAndIsDismissedFalseOrderByCreatedAtDesc(String targetRole);

    /**
     * Find notifications by type
     */
    List<Notification> findByTypeAndIsDismissedFalseOrderByCreatedAtDesc(Notification.NotificationType type);

    /**
     * Find notifications related to a specific entity
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "n.relatedEntityType = :entityType AND n.relatedEntityId = :entityId " +
           "AND n.isDismissed = false " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedEntity(@Param("entityType") String entityType,
                                           @Param("entityId") Long entityId);

    /**
     * Count unread notifications for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "(n.targetUserId = :userId OR n.targetUserId IS NULL) " +
           "AND n.isRead = false AND n.isDismissed = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Find expired notifications
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "n.expiresAt IS NOT NULL AND n.expiresAt < :now " +
           "AND n.isDismissed = false")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Find recent notifications (last 7 days)
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "n.createdAt >= :since AND n.isDismissed = false " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("since") LocalDateTime since);

    /**
     * Find high-priority unread notifications
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.targetUserId = :userId OR n.targetUserId IS NULL) " +
           "AND n.isRead = false AND n.isDismissed = false " +
           "AND n.priority >= 3 " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findHighPriorityUnreadByUserId(@Param("userId") Long userId);

    /**
     * Delete old dismissed notifications (cleanup)
     */
    @Query("DELETE FROM Notification n WHERE " +
           "n.isDismissed = true AND n.createdAt < :cutoffDate")
    void deleteOldDismissedNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
