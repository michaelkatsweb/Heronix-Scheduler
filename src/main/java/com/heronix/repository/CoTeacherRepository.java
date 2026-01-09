package com.heronix.repository;

import com.heronix.model.domain.CoTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Co-Teacher Repository
 * Data access layer for co-teacher management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Repository
public interface CoTeacherRepository extends JpaRepository<CoTeacher, Long> {

    // ========================================================================
    // FIND BY STATUS
    // ========================================================================

    List<CoTeacher> findByActiveTrue();

    List<CoTeacher> findByActiveFalse();

    // ========================================================================
    // FIND BY IDENTIFIERS
    // ========================================================================

    Optional<CoTeacher> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    // ========================================================================
    // FIND BY NAME
    // ========================================================================

    @Query("SELECT ct FROM CoTeacher ct WHERE " +
           "LOWER(CONCAT(ct.firstName, ' ', ct.lastName)) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(CONCAT(ct.lastName, ' ', ct.firstName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<CoTeacher> findByNameContaining(@Param("name") String name);

    List<CoTeacher> findByFirstNameContainingIgnoreCase(String firstName);

    List<CoTeacher> findByLastNameContainingIgnoreCase(String lastName);

    // ========================================================================
    // FIND BY SPECIALIZATION
    // ========================================================================

    List<CoTeacher> findBySpecializationContainingIgnoreCase(String specialization);

    @Query("SELECT ct FROM CoTeacher ct WHERE ct.specialization = :specialization AND ct.active = true")
    List<CoTeacher> findActiveBySpecialization(@Param("specialization") String specialization);

    // ========================================================================
    // FIND BY CERTIFICATION
    // ========================================================================

    @Query("SELECT ct FROM CoTeacher ct JOIN ct.certifications cert " +
           "WHERE LOWER(cert) LIKE LOWER(CONCAT('%', :certification, '%'))")
    List<CoTeacher> findByCertification(@Param("certification") String certification);

    @Query("SELECT ct FROM CoTeacher ct JOIN ct.certifications cert " +
           "WHERE LOWER(cert) LIKE LOWER(CONCAT('%', :certification, '%')) AND ct.active = true")
    List<CoTeacher> findActiveByCertification(@Param("certification") String certification);

    // ========================================================================
    // FIND BY CAPACITY
    // ========================================================================

    /**
     * Find co-teachers who are not at capacity
     */
    @Query("SELECT ct FROM CoTeacher ct WHERE ct.active = true " +
           "AND (ct.maxClasses IS NULL OR SIZE(ct.scheduleSlots) < ct.maxClasses)")
    List<CoTeacher> findAvailableCoTeachers();

    /**
     * Find co-teachers at or over capacity
     */
    @Query("SELECT ct FROM CoTeacher ct WHERE ct.active = true " +
           "AND ct.maxClasses IS NOT NULL AND SIZE(ct.scheduleSlots) >= ct.maxClasses")
    List<CoTeacher> findCoTeachersAtCapacity();

    // ========================================================================
    // STATISTICS
    // ========================================================================

    long countByActiveTrue();

    long countByActiveFalse();

    /**
     * Count co-teachers by specialization
     */
    @Query("SELECT COUNT(ct) FROM CoTeacher ct WHERE ct.specialization = :specialization AND ct.active = true")
    long countBySpecialization(@Param("specialization") String specialization);

    /**
     * Find co-teachers with most assignments
     */
    @Query("SELECT ct, SIZE(ct.scheduleSlots) as slotCount FROM CoTeacher ct " +
           "WHERE ct.active = true " +
           "GROUP BY ct " +
           "ORDER BY slotCount DESC")
    List<Object[]> findCoTeachersWithMostAssignments();

    /**
     * Get average class load
     */
    @Query("SELECT AVG(SIZE(ct.scheduleSlots)) FROM CoTeacher ct WHERE ct.active = true")
    Double getAverageClassLoad();

    // ========================================================================
    // FIND BY PREFERENCES
    // ========================================================================

    List<CoTeacher> findByPreferredGradesContainingIgnoreCase(String grade);

    List<CoTeacher> findByPreferredSubjectsContainingIgnoreCase(String subject);

    // ========================================================================
    // COMPLEX QUERIES
    // ========================================================================

    /**
     * Find co-teachers available for a specific specialization
     */
    @Query("SELECT ct FROM CoTeacher ct WHERE ct.active = true " +
           "AND ct.specialization = :specialization " +
           "AND (ct.maxClasses IS NULL OR SIZE(ct.scheduleSlots) < ct.maxClasses)")
    List<CoTeacher> findAvailableBySpecialization(@Param("specialization") String specialization);

    /**
     * Find co-teachers with specific certification and not at capacity
     */
    @Query("SELECT ct FROM CoTeacher ct JOIN ct.certifications cert " +
           "WHERE LOWER(cert) LIKE LOWER(CONCAT('%', :certification, '%')) " +
           "AND ct.active = true " +
           "AND (ct.maxClasses IS NULL OR SIZE(ct.scheduleSlots) < ct.maxClasses)")
    List<CoTeacher> findAvailableWithCertification(@Param("certification") String certification);
}
