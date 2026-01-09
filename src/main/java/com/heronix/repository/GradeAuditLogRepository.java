package com.heronix.repository;

import com.heronix.model.domain.GradeAuditLog;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for GradeAuditLog entity
 *
 * Provides data access methods for grade audit trail tracking.
 */
@Repository
public interface GradeAuditLogRepository extends JpaRepository<GradeAuditLog, Long> {

    /**
     * Find all audit logs for a specific grade
     */
    List<GradeAuditLog> findByStudentGradeOrderByTimestampDesc(StudentGrade studentGrade);

    /**
     * Find all audit logs by an admin
     */
    Page<GradeAuditLog> findByEditedByAdminOrderByTimestampDesc(User admin, Pageable pageable);

    /**
     * Find recent audit logs (last N entries)
     */
    List<GradeAuditLog> findTop100ByOrderByTimestampDesc();

    /**
     * Find audit logs within date range
     */
    @Query("SELECT a FROM GradeAuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<GradeAuditLog> findByTimestampBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find pending approval audit logs
     */
    @Query("SELECT a FROM GradeAuditLog a WHERE a.approvalStatus = 'PENDING' ORDER BY a.timestamp ASC")
    List<GradeAuditLog> findPendingApprovals();

    /**
     * Find audit logs by edit type
     */
    List<GradeAuditLog> findByEditTypeOrderByTimestampDesc(GradeAuditLog.EditType editType);

    /**
     * Count audit logs for a student's grade
     */
    long countByStudentGrade(StudentGrade studentGrade);

    /**
     * Find all audit logs for a student (across all grades)
     */
    @Query("SELECT a FROM GradeAuditLog a WHERE a.studentGrade.student.id = :studentId ORDER BY a.timestamp DESC")
    List<GradeAuditLog> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Find all audit logs for a course
     */
    @Query("SELECT a FROM GradeAuditLog a WHERE a.studentGrade.course.id = :courseId ORDER BY a.timestamp DESC")
    List<GradeAuditLog> findByCourseId(@Param("courseId") Long courseId);

    /**
     * Find audit logs that can be reversed
     */
    @Query("SELECT a FROM GradeAuditLog a WHERE a.isReversible = true AND a.isReversed = false ORDER BY a.timestamp DESC")
    List<GradeAuditLog> findReversibleAudits();

    /**
     * Count recent edits (last 24 hours)
     */
    @Query("SELECT COUNT(a) FROM GradeAuditLog a WHERE a.timestamp >= :since")
    long countRecentEdits(@Param("since") LocalDateTime since);

    /**
     * Find audit logs by field name
     */
    List<GradeAuditLog> findByFieldNameOrderByTimestampDesc(String fieldName);

    /**
     * Search audit logs with filters
     */
    @Query("SELECT a FROM GradeAuditLog a WHERE " +
           "(:studentId IS NULL OR a.studentGrade.student.id = :studentId) AND " +
           "(:courseId IS NULL OR a.studentGrade.course.id = :courseId) AND " +
           "(:adminId IS NULL OR a.editedByAdmin.id = :adminId) AND " +
           "(:editType IS NULL OR a.editType = :editType) AND " +
           "(:approvalStatus IS NULL OR a.approvalStatus = :approvalStatus) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<GradeAuditLog> searchAuditLogs(
        @Param("studentId") Long studentId,
        @Param("courseId") Long courseId,
        @Param("adminId") Long adminId,
        @Param("editType") GradeAuditLog.EditType editType,
        @Param("approvalStatus") GradeAuditLog.ApprovalStatus approvalStatus,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Get audit statistics for reporting
     */
    @Query("SELECT a.editType, COUNT(a) FROM GradeAuditLog a GROUP BY a.editType")
    List<Object[]> getEditTypeStatistics();

    /**
     * Find most active admins (by number of edits)
     */
    @Query("SELECT a.editedByAdmin, COUNT(a) as editCount FROM GradeAuditLog a " +
           "GROUP BY a.editedByAdmin ORDER BY editCount DESC")
    List<Object[]> getMostActiveAdmins();
}
