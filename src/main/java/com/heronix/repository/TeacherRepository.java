package com.heronix.repository;

import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.PriorityLevel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Teacher Repository
 * Location: src/main/java/com/eduscheduler/repository/TeacherRepository.java
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    // Find teacher by employee ID
    Optional<Teacher> findByEmployeeId(String employeeId);

    // Find teacher by ID with eager loading of collections
    // NOTE: We fetch collections separately to avoid Hibernate's MultipleBagFetchException
    // which occurs when fetching multiple List collections simultaneously with JOIN FETCH.
    // First query fetches the teacher with certifications (ElementCollection)
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN FETCH t.certifications WHERE t.id = :id")
    Optional<Teacher> findByIdWithCollections(@Param("id") Long id);

    // Second query to fetch teacher with subject certifications
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN FETCH t.subjectCertifications WHERE t.id = :id")
    Optional<Teacher> findByIdWithSubjectCertifications(@Param("id") Long id);

    // Third query to fetch teacher with special duty assignments
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN FETCH t.specialDutyAssignments WHERE t.id = :id")
    Optional<Teacher> findByIdWithSpecialAssignments(@Param("id") Long id);

    // Fourth query to fetch teacher with assigned courses
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN FETCH t.courses WHERE t.id = :id")
    Optional<Teacher> findByIdWithCourses(@Param("id") Long id);

    // Find teacher by email
    Optional<Teacher> findByEmail(String email);

    // Check if email exists (for duplicate detection)
    boolean existsByEmail(String email);

    // Check if employee ID exists (for duplicate detection)
    boolean existsByEmployeeId(String employeeId);

    // Find all active teachers (excluding soft-deleted)
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND (t.deleted = false OR t.deleted IS NULL)")
    List<Teacher> findByActiveTrue();

    // Find teachers by name (case-insensitive, partial match)
    List<Teacher> findByNameContainingIgnoreCase(String name);

    // Find teachers by department
    List<Teacher> findByDepartment(String department);

    // Find teachers by priority level
    List<Teacher> findByPriorityLevel(PriorityLevel priorityLevel);

    // Find teachers with certification
    @Query("SELECT t FROM Teacher t JOIN t.certifications c WHERE c = :certification")
    List<Teacher> findByCertification(@Param("certification") String certification);

    // ✅ FIXED: Find teachers with availability below max hours
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND t.currentWeekHours < t.maxHoursPerWeek")
    List<Teacher> findAvailableTeachers();

    // Find overloaded teachers (at or above max hours)
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND t.currentWeekHours >= t.maxHoursPerWeek")
    List<Teacher> findOverloadedTeachers();

    // Find teachers by utilization range (calculated in query)
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND " +
            "(t.currentWeekHours / t.maxHoursPerWeek) BETWEEN :minRate AND :maxRate")
    List<Teacher> findByUtilizationRange(@Param("minRate") double minRate, @Param("maxRate") double maxRate);

    // Reset all teachers' weekly hours (called at start of new week)
    @Modifying
    @Query("UPDATE Teacher t SET t.currentWeekHours = 0")
    void resetAllWeeklyHours();

    // Get teacher statistics
    @Query("SELECT " +
            "COUNT(t) as totalTeachers, " +
            "AVG(t.currentWeekHours) as avgHours, " +
            "MAX(t.currentWeekHours) as maxHours, " +
            "MIN(t.currentWeekHours) as minHours " +
            "FROM Teacher t WHERE t.active = true")
    Object getTeacherStatistics();

    // Find teachers with low utilization (< 50%)
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND " +
            "(t.currentWeekHours / t.maxHoursPerWeek) < 0.5")
    List<Teacher> findUnderutilizedTeachers();

    // Find teachers approaching max hours (>= 90%)
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND " +
            "(t.currentWeekHours / t.maxHoursPerWeek) >= 0.9")
    List<Teacher> findTeachersNearMaxHours();

    // ========================================================================
    // EAGER LOADING FOR SCHEDULE GENERATION
    // ========================================================================

    /**
     * Find all teachers with courses eagerly loaded for schedule generation
     * Use this to prevent LazyInitializationException during scheduling
     */
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN FETCH t.courses")
    List<Teacher> findAllWithCourses();

    /**
     * Find active teachers with courses eagerly loaded for schedule generation
     * Use this to prevent LazyInitializationException during scheduling
     */
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN FETCH t.courses WHERE t.active = true")
    List<Teacher> findActiveTeachersWithCourses();

    /**
     * Find all teachers with subject certifications eagerly loaded for OptaPlanner
     * CRITICAL: This prevents lazy loading issues during constraint evaluation
     * Use this method when loading teachers for AI schedule generation
     */
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN FETCH t.subjectCertifications")
    List<Teacher> findAllWithCertifications();

    /**
     * Find all teachers with both courses and certifications for OptaPlanner
     * CRITICAL: Loads all data needed for teacher qualification checking
     * Two-step query to avoid MultipleBagFetchException:
     * 1. This query loads teachers with certifications
     * 2. Then separately initialize courses collection
     */
    @Query("SELECT DISTINCT t FROM Teacher t " +
           "LEFT JOIN FETCH t.subjectCertifications " +
           "WHERE t.active = true AND (t.deleted = false OR t.deleted IS NULL)")
    List<Teacher> findAllWithCertificationsForScheduling();

    /**
     * Find all teachers (for UI display)
     * Returns basic teacher data without eager loading collections
     * Use service layer to initialize collections in transaction
     */
    @Query("SELECT DISTINCT t FROM Teacher t WHERE t.deleted = false OR t.deleted IS NULL")
    List<Teacher> findAllForUI();

    // ========================================================================
    // SOFT DELETE SUPPORT
    // ========================================================================

    /**
     * Find all non-deleted teachers (active and inactive)
     * Use this instead of findAll() to exclude soft-deleted records
     */
    @Query("SELECT t FROM Teacher t WHERE t.deleted = false OR t.deleted IS NULL")
    List<Teacher> findAllNonDeleted();

    /**
     * Find all active teachers excluding soft-deleted
     */
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND (t.deleted = false OR t.deleted IS NULL)")
    List<Teacher> findAllActive();

    /**
     * Find soft-deleted teachers (for audit/recovery purposes)
     */
    @Query("SELECT t FROM Teacher t WHERE t.deleted = true")
    List<Teacher> findDeleted();

    /**
     * Find teachers deleted by a specific user
     */
    @Query("SELECT t FROM Teacher t WHERE t.deleted = true AND t.deletedBy = :username")
    List<Teacher> findDeletedByUser(@Param("username") String username);

    // ========================================================================
    // COUNT METHODS FOR ACTIVE TEACHERS
    // ========================================================================

    /**
     * Count only active teachers (excluding soft-deleted)
     * Use this instead of count() for accurate metrics
     * Spring Data JPA will auto-implement this based on naming convention
     */
    long countByActiveTrue();

    /**
     * Count only active teachers excluding soft-deleted
     * More explicit query for counting active teachers
     */
    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.active = true AND (t.deleted = false OR t.deleted IS NULL)")
    long countActiveNonDeletedTeachers();

}