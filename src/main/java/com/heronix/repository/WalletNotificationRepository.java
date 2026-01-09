package com.heronix.repository;

import com.heronix.model.domain.WalletNotification;
import com.heronix.model.domain.WalletNotification.NotificationStatus;
import com.heronix.model.domain.WalletNotification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wallet Notification Repository
 *
 * Data access for wallet notifications
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@Repository
public interface WalletNotificationRepository extends JpaRepository<WalletNotification, Long> {

    /**
     * Find all notifications for a teacher
     */
    List<WalletNotification> findByTeacherIdOrderBySentAtDesc(Long teacherId);

    /**
     * Find unread notifications for a teacher
     */
    List<WalletNotification> findByTeacherIdAndStatusOrderBySentAtDesc(Long teacherId, NotificationStatus status);

    /**
     * Find high-priority notifications for a teacher
     */
    @Query("SELECT n FROM WalletNotification n WHERE n.teacherId = :teacherId AND n.priority >= 3 ORDER BY n.sentAt DESC")
    List<WalletNotification> findHighPriorityByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Find notifications requiring action
     */
    List<WalletNotification> findByTeacherIdAndRequiresActionTrueOrderBySentAtDesc(Long teacherId);

    /**
     * Find notifications by type
     */
    List<WalletNotification> findByTeacherIdAndNotificationTypeOrderBySentAtDesc(Long teacherId, NotificationType type);

    /**
     * Find recent notifications (last N days)
     */
    @Query("SELECT n FROM WalletNotification n WHERE n.teacherId = :teacherId AND n.sentAt >= :since ORDER BY n.sentAt DESC")
    List<WalletNotification> findRecentByTeacherId(@Param("teacherId") Long teacherId, @Param("since") LocalDateTime since);

    /**
     * Count unread notifications for teacher
     */
    long countByTeacherIdAndStatus(Long teacherId, NotificationStatus status);

    /**
     * Count high-priority unread notifications
     */
    @Query("SELECT COUNT(n) FROM WalletNotification n WHERE n.teacherId = :teacherId AND n.status = 'UNREAD' AND n.priority >= 3")
    long countHighPriorityUnreadByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Find notifications related to a transaction
     */
    List<WalletNotification> findByTransactionIdOrderBySentAtDesc(Long transactionId);

    /**
     * Delete old archived notifications
     */
    @Query("DELETE FROM WalletNotification n WHERE n.status = 'ARCHIVED' AND n.archivedAt < :before")
    void deleteOldArchived(@Param("before") LocalDateTime before);
}
