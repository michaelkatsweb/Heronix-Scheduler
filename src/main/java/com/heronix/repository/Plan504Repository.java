package com.heronix.repository;

import com.heronix.model.domain.Plan504;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.Plan504Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Plan 504 Repository
 *
 * Provides data access methods for 504 Plan entities including:
 * - Finding plans by student, status, and dates
 * - Querying active/expired plans
 * - Finding plans needing review
 * - Searching by disability
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Repository
public interface Plan504Repository extends JpaRepository<Plan504, Long> {

    // ========== BASIC QUERIES ==========

    /**
     * Find 504 plan by student
     */
    Optional<Plan504> findByStudent(Student student);

    /**
     * Find 504 plan by student ID
     */
    Optional<Plan504> findByStudentId(Long studentId);

    /**
     * Find all plans by status
     */
    List<Plan504> findByStatus(Plan504Status status);

    /**
     * Find all active 504 plans
     */
    @Query("SELECT p FROM Plan504 p WHERE p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND p.endDate >= :today")
    List<Plan504> findAllActivePlans(@Param("today") LocalDate today);

    /**
     * Find expired plans (past end date but not marked as expired)
     */
    @Query("SELECT p FROM Plan504 p WHERE p.status = 'ACTIVE' " +
           "AND p.endDate < :today")
    List<Plan504> findExpiredPlans(@Param("today") LocalDate today);

    // ========== REVIEW & RENEWAL QUERIES ==========

    /**
     * Find plans needing review (within threshold days)
     */
    @Query("SELECT p FROM Plan504 p WHERE p.status = 'ACTIVE' " +
           "AND p.endDate BETWEEN :today AND :thresholdDate")
    List<Plan504> findPlansNeedingRenewal(
        @Param("today") LocalDate today,
        @Param("thresholdDate") LocalDate thresholdDate
    );

    /**
     * Find plans with overdue reviews
     */
    @Query("SELECT p FROM Plan504 p WHERE p.status = 'ACTIVE' " +
           "AND p.nextReviewDate IS NOT NULL " +
           "AND p.nextReviewDate < :today")
    List<Plan504> findPlansWithOverdueReview(@Param("today") LocalDate today);

    // ========== DISABILITY QUERIES ==========

    /**
     * Find plans by disability
     */
    List<Plan504> findByDisability(String disability);

    // ========== STATISTICS QUERIES ==========

    /**
     * Count active 504 plans
     */
    @Query("SELECT COUNT(p) FROM Plan504 p WHERE p.status = 'ACTIVE' " +
           "AND p.startDate <= :today " +
           "AND p.endDate >= :today")
    long countActivePlans(@Param("today") LocalDate today);

    /**
     * Count plans by disability
     */
    @Query("SELECT p.disability, COUNT(p) FROM Plan504 p " +
           "WHERE p.status = 'ACTIVE' " +
           "GROUP BY p.disability")
    List<Object[]> countByDisability();

    // ========== SEARCH QUERIES ==========

    /**
     * Search plans by student name
     */
    @Query("SELECT p FROM Plan504 p WHERE " +
           "LOWER(p.student.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.student.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Plan504> searchByStudentName(@Param("searchTerm") String searchTerm);

    /**
     * Search plans by plan number
     */
    Optional<Plan504> findByPlanNumber(String planNumber);

    /**
     * Find plans by coordinator
     */
    List<Plan504> findByCoordinator(String coordinator);
}
