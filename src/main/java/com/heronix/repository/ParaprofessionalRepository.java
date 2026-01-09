package com.heronix.repository;

import com.heronix.model.domain.Paraprofessional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Paraprofessional Repository
 * Data access layer for paraprofessional management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Repository
public interface ParaprofessionalRepository extends JpaRepository<Paraprofessional, Long> {

    // ========================================================================
    // FIND BY STATUS
    // ========================================================================

    List<Paraprofessional> findByActiveTrue();

    List<Paraprofessional> findByActiveFalse();

    // ========================================================================
    // FIND BY IDENTIFIERS
    // ========================================================================

    Optional<Paraprofessional> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    // ========================================================================
    // FIND BY NAME
    // ========================================================================

    @Query("SELECT p FROM Paraprofessional p WHERE " +
           "LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(CONCAT(p.lastName, ' ', p.firstName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Paraprofessional> findByNameContaining(@Param("name") String name);

    List<Paraprofessional> findByFirstNameContainingIgnoreCase(String firstName);

    List<Paraprofessional> findByLastNameContainingIgnoreCase(String lastName);

    // ========================================================================
    // FIND BY ROLE TYPE
    // ========================================================================

    List<Paraprofessional> findByRoleTypeContainingIgnoreCase(String roleType);

    @Query("SELECT p FROM Paraprofessional p WHERE p.roleType = :roleType AND p.active = true")
    List<Paraprofessional> findActiveByRoleType(@Param("roleType") String roleType);

    // ========================================================================
    // FIND BY ASSIGNMENT TYPE
    // ========================================================================

    List<Paraprofessional> findByAssignmentTypeContainingIgnoreCase(String assignmentType);

    @Query("SELECT p FROM Paraprofessional p WHERE p.assignmentType = :assignmentType AND p.active = true")
    List<Paraprofessional> findActiveByAssignmentType(@Param("assignmentType") String assignmentType);

    // ========================================================================
    // FIND BY SKILLS
    // ========================================================================

    @Query("SELECT p FROM Paraprofessional p JOIN p.specializedSkills skill " +
           "WHERE LOWER(skill) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<Paraprofessional> findBySkill(@Param("skill") String skill);

    @Query("SELECT p FROM Paraprofessional p JOIN p.specializedSkills skill " +
           "WHERE LOWER(skill) LIKE LOWER(CONCAT('%', :skill, '%')) AND p.active = true")
    List<Paraprofessional> findActiveBySkill(@Param("skill") String skill);

    // ========================================================================
    // FIND BY CERTIFICATION
    // ========================================================================

    @Query("SELECT p FROM Paraprofessional p JOIN p.certifications cert " +
           "WHERE LOWER(cert) LIKE LOWER(CONCAT('%', :certification, '%'))")
    List<Paraprofessional> findByCertification(@Param("certification") String certification);

    @Query("SELECT p FROM Paraprofessional p JOIN p.certifications cert " +
           "WHERE LOWER(cert) LIKE LOWER(CONCAT('%', :certification, '%')) AND p.active = true")
    List<Paraprofessional> findActiveByCertification(@Param("certification") String certification);

    // ========================================================================
    // FIND BY TRAINING
    // ========================================================================

    List<Paraprofessional> findByMedicalTrainingTrueAndActiveTrue();

    List<Paraprofessional> findByBehavioralTrainingTrueAndActiveTrue();

    @Query("SELECT p FROM Paraprofessional p WHERE p.active = true " +
           "AND (p.medicalTraining = true OR p.behavioralTraining = true)")
    List<Paraprofessional> findWithSpecialTraining();

    // ========================================================================
    // FIND BY CAPACITY
    // ========================================================================

    /**
     * Find paraprofessionals who are not at capacity
     */
    @Query("SELECT p FROM Paraprofessional p WHERE p.active = true " +
           "AND (p.maxStudents IS NULL OR SIZE(p.assignedStudents) < p.maxStudents)")
    List<Paraprofessional> findAvailableParaprofessionals();

    /**
     * Find paraprofessionals at or over capacity
     */
    @Query("SELECT p FROM Paraprofessional p WHERE p.active = true " +
           "AND p.maxStudents IS NOT NULL AND SIZE(p.assignedStudents) >= p.maxStudents")
    List<Paraprofessional> findParaprofessionalsAtCapacity();

    // ========================================================================
    // STATISTICS
    // ========================================================================

    long countByActiveTrue();

    long countByActiveFalse();

    /**
     * Count paraprofessionals by role type
     */
    @Query("SELECT COUNT(p) FROM Paraprofessional p WHERE p.roleType = :roleType AND p.active = true")
    long countByRoleType(@Param("roleType") String roleType);

    /**
     * Count paraprofessionals by assignment type
     */
    @Query("SELECT COUNT(p) FROM Paraprofessional p WHERE p.assignmentType = :assignmentType AND p.active = true")
    long countByAssignmentType(@Param("assignmentType") String assignmentType);

    /**
     * Find paraprofessionals with most student assignments
     */
    @Query("SELECT p, SIZE(p.assignedStudents) as studentCount FROM Paraprofessional p " +
           "WHERE p.active = true " +
           "GROUP BY p " +
           "ORDER BY studentCount DESC")
    List<Object[]> findParaprofessionalsWithMostAssignments();

    /**
     * Get average student load
     */
    @Query("SELECT AVG(SIZE(p.assignedStudents)) FROM Paraprofessional p WHERE p.active = true")
    Double getAverageStudentLoad();

    // ========================================================================
    // COMPLEX QUERIES
    // ========================================================================

    /**
     * Find available paraprofessionals for a specific role type
     */
    @Query("SELECT p FROM Paraprofessional p WHERE p.active = true " +
           "AND p.roleType = :roleType " +
           "AND (p.maxStudents IS NULL OR SIZE(p.assignedStudents) < p.maxStudents)")
    List<Paraprofessional> findAvailableByRoleType(@Param("roleType") String roleType);

    /**
     * Find paraprofessionals with specific skill and not at capacity
     */
    @Query("SELECT p FROM Paraprofessional p JOIN p.specializedSkills skill " +
           "WHERE LOWER(skill) LIKE LOWER(CONCAT('%', :skill, '%')) " +
           "AND p.active = true " +
           "AND (p.maxStudents IS NULL OR SIZE(p.assignedStudents) < p.maxStudents)")
    List<Paraprofessional> findAvailableWithSkill(@Param("skill") String skill);

    /**
     * Find paraprofessionals with specific certification and not at capacity
     */
    @Query("SELECT p FROM Paraprofessional p JOIN p.certifications cert " +
           "WHERE LOWER(cert) LIKE LOWER(CONCAT('%', :certification, '%')) " +
           "AND p.active = true " +
           "AND (p.maxStudents IS NULL OR SIZE(p.assignedStudents) < p.maxStudents)")
    List<Paraprofessional> findAvailableWithCertification(@Param("certification") String certification);

    /**
     * Find 1:1 paraprofessionals (assignment type or max students = 1)
     */
    @Query("SELECT p FROM Paraprofessional p WHERE p.active = true " +
           "AND (p.assignmentType LIKE '%1:1%' OR p.maxStudents = 1)")
    List<Paraprofessional> findOneToOneParaprofessionals();

    /**
     * Find paraprofessionals available for medical support
     */
    @Query("SELECT p FROM Paraprofessional p WHERE p.active = true " +
           "AND p.medicalTraining = true " +
           "AND (p.maxStudents IS NULL OR SIZE(p.assignedStudents) < p.maxStudents)")
    List<Paraprofessional> findAvailableWithMedicalTraining();

    /**
     * Find paraprofessionals available for behavioral support
     */
    @Query("SELECT p FROM Paraprofessional p WHERE p.active = true " +
           "AND p.behavioralTraining = true " +
           "AND (p.maxStudents IS NULL OR SIZE(p.assignedStudents) < p.maxStudents)")
    List<Paraprofessional> findAvailableWithBehavioralTraining();

    // ========================================================================
    // EAGER LOADING FOR UI (Prevent LazyInitializationException)
    // ========================================================================

    /**
     * Find all paraprofessionals with certifications and skills eagerly loaded
     * Use this to prevent LazyInitializationException when displaying in UI
     */
    @Query("SELECT DISTINCT p FROM Paraprofessional p " +
           "LEFT JOIN FETCH p.certifications " +
           "LEFT JOIN FETCH p.specializedSkills")
    List<Paraprofessional> findAllWithCertificationsAndSkills();

    /**
     * Find paraprofessional by ID with certifications and skills eagerly loaded
     * Use this to prevent LazyInitializationException when displaying in UI
     */
    @Query("SELECT DISTINCT p FROM Paraprofessional p " +
           "LEFT JOIN FETCH p.certifications " +
           "LEFT JOIN FETCH p.specializedSkills " +
           "WHERE p.id = :id")
    Optional<Paraprofessional> findByIdWithCertificationsAndSkills(@Param("id") Long id);

    /**
     * Find all paraprofessionals with all collections eagerly loaded (full load for UI)
     * Use this when you need complete paraprofessional data including student assignments
     */
    @Query("SELECT DISTINCT p FROM Paraprofessional p " +
           "LEFT JOIN FETCH p.certifications " +
           "LEFT JOIN FETCH p.specializedSkills " +
           "LEFT JOIN FETCH p.assignedStudents")
    List<Paraprofessional> findAllWithAllCollections();
}
