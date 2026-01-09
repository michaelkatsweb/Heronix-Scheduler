package com.heronix.repository;

import com.heronix.model.domain.QrAttendanceLog;
import com.heronix.model.domain.QrAttendanceLog.VerificationStatus;
import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for QR Attendance Log operations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - QR Attendance System Phase 1
 */
@Repository
public interface QrAttendanceLogRepository extends JpaRepository<QrAttendanceLog, Long> {

    /**
     * Find all attendance logs for a specific student
     */
    List<QrAttendanceLog> findByStudentOrderByScanTimestampDesc(Student student);

    /**
     * Find all attendance logs for a student by ID
     */
    List<QrAttendanceLog> findByStudent_IdOrderByScanTimestampDesc(Long studentId);

    /**
     * Find attendance logs for a specific period
     */
    List<QrAttendanceLog> findByPeriodOrderByScanTimestampDesc(Integer period);

    /**
     * Find attendance logs by verification status
     */
    List<QrAttendanceLog> findByVerificationStatusOrderByScanTimestampDesc(VerificationStatus status);

    /**
     * Find attendance logs pending admin review
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.verificationStatus IN ('PENDING', 'FLAGGED') ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findPendingReview();

    /**
     * Find attendance logs for a student on a specific date
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.student.id = :studentId " +
           "AND q.scanTimestamp >= :startDate AND q.scanTimestamp < :endDate " +
           "ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findByStudentAndDate(
        @Param("studentId") Long studentId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find attendance logs for a specific date range
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.scanTimestamp >= :startDate " +
           "AND q.scanTimestamp < :endDate ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find attendance logs for a room on a specific date
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.roomNumber = :roomNumber " +
           "AND q.scanTimestamp >= :startDate AND q.scanTimestamp < :endDate " +
           "ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findByRoomAndDate(
        @Param("roomNumber") String roomNumber,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find attendance logs for a teacher on a specific date
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.teacher.id = :teacherId " +
           "AND q.scanTimestamp >= :startDate AND q.scanTimestamp < :endDate " +
           "ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findByTeacherAndDate(
        @Param("teacherId") Long teacherId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find logs with low face match scores (potential security issues)
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.faceMatchScore < :threshold " +
           "ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findLowConfidenceMatches(@Param("threshold") Double threshold);

    /**
     * Find logs where parent has not been notified
     */
    List<QrAttendanceLog> findByParentNotifiedFalseOrderByScanTimestampDesc();

    /**
     * Count attendance logs for a student in a date range
     */
    @Query("SELECT COUNT(q) FROM QrAttendanceLog q WHERE q.student.id = :studentId " +
           "AND q.scanTimestamp >= :startDate AND q.scanTimestamp < :endDate " +
           "AND q.verificationStatus IN ('AUTO_VERIFIED', 'ADMIN_VERIFIED')")
    Long countVerifiedAttendance(
        @Param("studentId") Long studentId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find most recent scan for a student
     */
    Optional<QrAttendanceLog> findFirstByStudentOrderByScanTimestampDesc(Student student);

    /**
     * Check for duplicate scans within time window (anti-fraud)
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.student.id = :studentId " +
           "AND q.period = :period " +
           "AND q.scanTimestamp >= :afterTime " +
           "ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findRecentScansForPeriod(
        @Param("studentId") Long studentId,
        @Param("period") Integer period,
        @Param("afterTime") LocalDateTime afterTime
    );

    /**
     * Get attendance summary statistics
     */
    @Query("SELECT q.verificationStatus, COUNT(q) FROM QrAttendanceLog q " +
           "WHERE q.scanTimestamp >= :startDate AND q.scanTimestamp < :endDate " +
           "GROUP BY q.verificationStatus")
    List<Object[]> getAttendanceStatistics(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find flagged or rejected scans (security monitoring)
     */
    @Query("SELECT q FROM QrAttendanceLog q WHERE q.verificationStatus IN ('FLAGGED', 'REJECTED') " +
           "ORDER BY q.scanTimestamp DESC")
    List<QrAttendanceLog> findSecurityAlerts();
}
