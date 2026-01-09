package com.heronix.repository;

import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.PullOutSchedule;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.PullOutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository for PullOutSchedule entity
 *
 * Provides data access for pull-out service schedules including:
 * - Finding schedules by student, staff, day, time
 * - Detecting scheduling conflicts
 * - Active schedule management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Repository
public interface PullOutScheduleRepository extends JpaRepository<PullOutSchedule, Long> {

    // ========== BASIC QUERIES ==========

    /**
     * Find all pull-out schedules for a student
     */
    List<PullOutSchedule> findByStudent(Student student);

    /**
     * Find all pull-out schedules for a staff member
     */
    List<PullOutSchedule> findByStaff(Teacher staff);

    /**
     * Find all pull-out schedules for an IEP service
     */
    List<PullOutSchedule> findByIepService(IEPService iepService);

    /**
     * Find schedules by status
     */
    List<PullOutSchedule> findByStatus(PullOutStatus status);

    // ========== DAY AND TIME QUERIES ==========

    /**
     * Find all schedules on a specific day of week
     */
    List<PullOutSchedule> findByDayOfWeek(String dayOfWeek);

    /**
     * Find active schedules on a specific day
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.dayOfWeek = :dayOfWeek " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<PullOutSchedule> findActiveByDayOfWeek(
        @Param("dayOfWeek") String dayOfWeek,
        @Param("today") LocalDate today
    );

    /**
     * Find schedules for a student on a specific day
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.student = :student " +
           "AND p.dayOfWeek = :dayOfWeek " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<PullOutSchedule> findByStudentAndDayOfWeek(
        @Param("student") Student student,
        @Param("dayOfWeek") String dayOfWeek,
        @Param("today") LocalDate today
    );

    /**
     * Find schedules for a staff member on a specific day
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.staff = :staff " +
           "AND p.dayOfWeek = :dayOfWeek " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<PullOutSchedule> findByStaffAndDayOfWeek(
        @Param("staff") Teacher staff,
        @Param("dayOfWeek") String dayOfWeek,
        @Param("today") LocalDate today
    );

    // ========== TIME CONFLICT DETECTION ==========

    /**
     * Find schedules that overlap with a given time slot for a student
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.student = :student " +
           "AND p.dayOfWeek = :dayOfWeek " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today) " +
           "AND ((p.startTime <= :startTime AND p.endTime > :startTime) " +
           "OR (p.startTime < :endTime AND p.endTime >= :endTime) " +
           "OR (p.startTime >= :startTime AND p.endTime <= :endTime))")
    List<PullOutSchedule> findConflictingSchedulesForStudent(
        @Param("student") Student student,
        @Param("dayOfWeek") String dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("today") LocalDate today
    );

    /**
     * Find schedules that overlap with a given time slot for a staff member
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.staff = :staff " +
           "AND p.dayOfWeek = :dayOfWeek " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today) " +
           "AND ((p.startTime <= :startTime AND p.endTime > :startTime) " +
           "OR (p.startTime < :endTime AND p.endTime >= :endTime) " +
           "OR (p.startTime >= :startTime AND p.endTime <= :endTime))")
    List<PullOutSchedule> findConflictingSchedulesForStaff(
        @Param("staff") Teacher staff,
        @Param("dayOfWeek") String dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("today") LocalDate today
    );

    // ========== DATE RANGE QUERIES ==========

    /**
     * Find schedules effective during a date range
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.startDate <= :endDate AND " +
           "(p.endDate IS NULL OR p.endDate >= :startDate)")
    List<PullOutSchedule> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find active schedules effective during a date range
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.status = 'ACTIVE' AND " +
           "p.startDate <= :endDate AND " +
           "(p.endDate IS NULL OR p.endDate >= :startDate)")
    List<PullOutSchedule> findActiveByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ========== STATUS QUERIES ==========

    /**
     * Find all active schedules
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<PullOutSchedule> findAllActive(@Param("today") LocalDate today);

    /**
     * Find all active schedules for a student
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.student = :student " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<PullOutSchedule> findActiveByStudent(
        @Param("student") Student student,
        @Param("today") LocalDate today
    );

    /**
     * Find all active schedules for a staff member
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.staff = :staff " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<PullOutSchedule> findActiveByStaff(
        @Param("staff") Teacher staff,
        @Param("today") LocalDate today
    );

    // ========== GROUP SESSIONS ==========

    /**
     * Find all group sessions
     */
    @Query("SELECT p FROM PullOutSchedule p WHERE " +
           "p.otherStudents IS NOT NULL " +
           "AND p.otherStudents != '' " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<PullOutSchedule> findGroupSessions(@Param("today") LocalDate today);

    // ========== STATISTICS & REPORTING ==========

    /**
     * Count active schedules for a student
     */
    @Query("SELECT COUNT(p) FROM PullOutSchedule p WHERE " +
           "p.student = :student " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    Long countActiveByStudent(
        @Param("student") Student student,
        @Param("today") LocalDate today
    );

    /**
     * Count active schedules for a staff member
     */
    @Query("SELECT COUNT(p) FROM PullOutSchedule p WHERE " +
           "p.staff = :staff " +
           "AND p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    Long countActiveByStaff(
        @Param("staff") Teacher staff,
        @Param("today") LocalDate today
    );

    /**
     * Calculate total pull-out minutes per week for a student
     */
    @Query("SELECT SUM(p.durationMinutes) FROM PullOutSchedule p WHERE " +
           "p.student = :student " +
           "AND p.status = 'ACTIVE' " +
           "AND p.recurring = true " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    Long sumWeeklyMinutesForStudent(
        @Param("student") Student student,
        @Param("today") LocalDate today
    );

    /**
     * Calculate total pull-out minutes per week for a staff member
     */
    @Query("SELECT SUM(p.durationMinutes) FROM PullOutSchedule p WHERE " +
           "p.staff = :staff " +
           "AND p.status = 'ACTIVE' " +
           "AND p.recurring = true " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    Long sumWeeklyMinutesForStaff(
        @Param("staff") Teacher staff,
        @Param("today") LocalDate today
    );
}
