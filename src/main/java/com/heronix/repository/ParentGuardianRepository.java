package com.heronix.repository;

import com.heronix.model.domain.ParentGuardian;
import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ParentGuardian Repository - K-12 Enrollment Enhancement
 * Location: src/main/java/com/eduscheduler/repository/ParentGuardianRepository.java
 *
 * Data access layer for parent/guardian operations.
 * Provides CRUD operations and custom queries for parent/guardian management.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 14, 2025
 */
@Repository
public interface ParentGuardianRepository extends JpaRepository<ParentGuardian, Long> {

    /**
     * Find all parents/guardians for a specific student
     * Ordered by priority (primary custodian first, then by priority order)
     *
     * @param student The student entity
     * @return List of parent/guardian records
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student = :student " +
           "ORDER BY pg.isPrimaryCustodian DESC, pg.priorityOrder ASC")
    List<ParentGuardian> findByStudentOrderByPriority(@Param("student") Student student);

    /**
     * Find all parents/guardians for a specific student ID
     *
     * @param studentId The student ID
     * @return List of parent/guardian records
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student.id = :studentId " +
           "ORDER BY pg.isPrimaryCustodian DESC, pg.priorityOrder ASC")
    List<ParentGuardian> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Find primary custodian for a student
     *
     * @param student The student entity
     * @return Primary custodian (if exists)
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student = :student " +
           "AND pg.isPrimaryCustodian = true")
    Optional<ParentGuardian> findPrimaryCustodian(@Param("student") Student student);

    /**
     * Find all parents/guardians with legal custody for a student
     *
     * @param studentId The student ID
     * @return List of parents with legal custody
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student.id = :studentId " +
           "AND pg.hasLegalCustody = true " +
           "ORDER BY pg.isPrimaryCustodian DESC, pg.priorityOrder ASC")
    List<ParentGuardian> findLegalCustodiansForStudent(@Param("studentId") Long studentId);

    /**
     * Find all parents/guardians authorized to pick up student
     *
     * @param studentId The student ID
     * @return List of parents authorized for pickup
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student.id = :studentId " +
           "AND pg.canPickUpStudent = true")
    List<ParentGuardian> findAuthorizedForPickup(@Param("studentId") Long studentId);

    /**
     * Find parents/guardians living with student
     *
     * @param studentId The student ID
     * @return List of parents living with student
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student.id = :studentId " +
           "AND pg.livesWithStudent = true")
    List<ParentGuardian> findLivingWithStudent(@Param("studentId") Long studentId);

    /**
     * Find parents/guardians by email (for account linking)
     *
     * @param email Email address
     * @return List of parent/guardian records with this email
     */
    List<ParentGuardian> findByEmail(String email);

    /**
     * Find parents/guardians by cell phone
     *
     * @param cellPhone Cell phone number
     * @return List of parent/guardian records with this phone
     */
    List<ParentGuardian> findByCellPhone(String cellPhone);

    /**
     * Search parents/guardians by name (first or last)
     *
     * @param searchTerm Search term
     * @return List of matching parent/guardian records
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE " +
           "LOWER(pg.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(pg.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ParentGuardian> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find all parents/guardians with court restrictions
     *
     * @return List of parents with court restrictions
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.hasCourtRestriction = true")
    List<ParentGuardian> findWithCourtRestrictions();

    /**
     * Count parents/guardians for a student
     *
     * @param studentId The student ID
     * @return Number of parent/guardian records
     */
    @Query("SELECT COUNT(pg) FROM ParentGuardian pg WHERE pg.student.id = :studentId")
    Long countByStudentId(@Param("studentId") Long studentId);

    /**
     * Check if student has a primary custodian assigned
     *
     * @param studentId The student ID
     * @return true if primary custodian exists
     */
    @Query("SELECT CASE WHEN COUNT(pg) > 0 THEN true ELSE false END " +
           "FROM ParentGuardian pg WHERE pg.student.id = :studentId " +
           "AND pg.isPrimaryCustodian = true")
    boolean hasPrimaryCustodian(@Param("studentId") Long studentId);

    /**
     * Find parents/guardians who should receive report cards
     *
     * @param studentId The student ID
     * @return List of parents who should receive report cards
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student.id = :studentId " +
           "AND pg.receivesReportCards = true " +
           "ORDER BY pg.isPrimaryCustodian DESC, pg.priorityOrder ASC")
    List<ParentGuardian> findReportCardRecipients(@Param("studentId") Long studentId);

    /**
     * Find parents/guardians authorized for emergency contact
     *
     * @param studentId The student ID
     * @return List of parents authorized for emergency contact
     */
    @Query("SELECT pg FROM ParentGuardian pg WHERE pg.student.id = :studentId " +
           "AND pg.authorizedForEmergency = true " +
           "ORDER BY pg.isPrimaryCustodian DESC, pg.priorityOrder ASC")
    List<ParentGuardian> findEmergencyContacts(@Param("studentId") Long studentId);

    /**
     * Delete all parents/guardians for a student
     * (Used when student is deleted - cascade should handle this automatically)
     *
     * @param studentId The student ID
     */
    @Query("DELETE FROM ParentGuardian pg WHERE pg.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
