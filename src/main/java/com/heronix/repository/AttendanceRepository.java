package com.heronix.repository;

import com.heronix.model.domain.AttendanceRecord;
import com.heronix.model.domain.AttendanceRecord.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for AttendanceRecord entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Attendance System
 */
@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByStudentIdAndAttendanceDate(Long studentId, LocalDate date);

    List<AttendanceRecord> findByStudentIdAndAttendanceDateBetween(
        Long studentId, LocalDate startDate, LocalDate endDate);

    List<AttendanceRecord> findByCourseIdAndAttendanceDate(Long courseId, LocalDate date);

    List<AttendanceRecord> findByAttendanceDateAndCampusId(LocalDate date, Long campusId);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.student.id = :studentId " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND a.status IN :statuses")
    List<AttendanceRecord> findByStudentAndDateRangeAndStatuses(
        @Param("studentId") Long studentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuses") List<AttendanceStatus> statuses);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.student.id = :studentId " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND a.status = :status")
    long countByStudentAndDateRangeAndStatus(
        @Param("studentId") Long studentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") AttendanceStatus status);

    @Query("SELECT a.student.id, COUNT(a) FROM AttendanceRecord a " +
           "WHERE a.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND a.status IN ('ABSENT', 'UNEXCUSED_ABSENT') " +
           "GROUP BY a.student.id HAVING COUNT(a) >= :threshold")
    List<Object[]> findStudentsWithChronicAbsences(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("threshold") long threshold);

    List<AttendanceRecord> findByVerifiedFalseAndAttendanceDateBefore(LocalDate date);
}
