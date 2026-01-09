package com.heronix.repository;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.IEPStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * IEP Repository
 *
 * Provides data access methods for IEP entities including:
 * - Finding IEPs by student, status, and dates
 * - Querying active/expired IEPs
 * - Finding IEPs needing review
 * - Searching by IEP number
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Repository
public interface IEPRepository extends JpaRepository<IEP, Long> {

    // ========== BASIC QUERIES ==========

    /**
     * Find IEP by student
     */
    Optional<IEP> findByStudent(Student student);

    /**
     * Find IEP by student ID
     */
    Optional<IEP> findByStudentId(Long studentId);

    /**
     * Find all IEPs by status
     */
    List<IEP> findByStatus(IEPStatus status);

    /**
     * Find all active IEPs
     */
    @Query("SELECT i FROM IEP i WHERE i.status = 'ACTIVE' " +
           "AND i.startDate <= :today " +
           "AND i.endDate >= :today")
    List<IEP> findAllActiveIEPs(@Param("today") LocalDate today);

    /**
     * Find expired IEPs (past end date but not marked as expired)
     */
    @Query("SELECT i FROM IEP i WHERE i.status = 'ACTIVE' " +
           "AND i.endDate < :today")
    List<IEP> findExpiredIEPs(@Param("today") LocalDate today);

    // ========== REVIEW & RENEWAL QUERIES ==========

    /**
     * Find IEPs needing renewal (within threshold days of expiration)
     */
    @Query("SELECT i FROM IEP i WHERE i.status = 'ACTIVE' " +
           "AND i.endDate BETWEEN :today AND :thresholdDate")
    List<IEP> findIEPsNeedingRenewal(
        @Param("today") LocalDate today,
        @Param("thresholdDate") LocalDate thresholdDate
    );

    /**
     * Find IEPs with review due
     */
    @Query("SELECT i FROM IEP i WHERE i.status = 'ACTIVE' " +
           "AND i.nextReviewDate IS NOT NULL " +
           "AND i.nextReviewDate <= :today")
    List<IEP> findIEPsWithReviewDue(@Param("today") LocalDate today);

    // ========== STATISTICS QUERIES ==========

    /**
     * Count active IEPs
     */
    @Query("SELECT COUNT(i) FROM IEP i WHERE i.status = 'ACTIVE' " +
           "AND i.startDate <= :today " +
           "AND i.endDate >= :today")
    long countActiveIEPs(@Param("today") LocalDate today);

    /**
     * Count IEPs by eligibility category
     */
    @Query("SELECT i.eligibilityCategory, COUNT(i) FROM IEP i " +
           "WHERE i.status = 'ACTIVE' " +
           "GROUP BY i.eligibilityCategory")
    List<Object[]> countByEligibilityCategory();

    // ========== SEARCH QUERIES ==========

    /**
     * Search IEPs by student name
     */
    @Query("SELECT i FROM IEP i WHERE " +
           "LOWER(i.student.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.student.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<IEP> searchByStudentName(@Param("searchTerm") String searchTerm);

    /**
     * Search IEPs by IEP number
     */
    Optional<IEP> findByIepNumber(String iepNumber);

    /**
     * Find all IEPs with services that need scheduling
     */
    @Query("SELECT DISTINCT i FROM IEP i " +
           "JOIN i.services s " +
           "WHERE i.status = 'ACTIVE' " +
           "AND s.status = 'NOT_SCHEDULED'")
    List<IEP> findIEPsWithUnscheduledServices();

    /**
     * Find IEPs by case manager
     */
    List<IEP> findByCaseManager(String caseManager);
}
