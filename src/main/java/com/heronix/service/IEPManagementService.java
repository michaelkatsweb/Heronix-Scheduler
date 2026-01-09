package com.heronix.service;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.IEPStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * IEP Management Service Interface
 *
 * Provides business logic for managing Individualized Education Programs (IEPs).
 * Handles IEP creation, updates, service tracking, and compliance monitoring.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
public interface IEPManagementService {

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new IEP
     *
     * @param iep IEP to create
     * @return Created IEP with ID
     */
    IEP createIEP(IEP iep);

    /**
     * Update an existing IEP
     *
     * @param iep IEP to update
     * @return Updated IEP
     */
    IEP updateIEP(IEP iep);

    /**
     * Find IEP by ID
     *
     * @param id IEP ID
     * @return Optional containing IEP if found
     */
    Optional<IEP> findById(Long id);

    /**
     * Find IEP by IEP number
     *
     * @param iepNumber IEP number
     * @return Optional containing IEP if found
     */
    Optional<IEP> findByIepNumber(String iepNumber);

    /**
     * Find active IEP for a student
     *
     * @param studentId Student ID
     * @return Optional containing active IEP if found
     */
    Optional<IEP> findActiveIEPForStudent(Long studentId);

    /**
     * Find all IEPs for a student (active and historical)
     *
     * @param studentId Student ID
     * @return List of IEPs
     */
    List<IEP> findAllIEPsForStudent(Long studentId);

    /**
     * Delete an IEP
     * Note: Should only be used for draft IEPs. Active IEPs should be expired instead.
     *
     * @param id IEP ID
     */
    void deleteIEP(Long id);

    // ========== STATUS MANAGEMENT ==========

    /**
     * Activate an IEP (change status from DRAFT to ACTIVE)
     *
     * @param id IEP ID
     * @return Updated IEP
     */
    IEP activateIEP(Long id);

    /**
     * Expire an IEP (change status to EXPIRED)
     *
     * @param id IEP ID
     * @return Updated IEP
     */
    IEP expireIEP(Long id);

    /**
     * Mark IEP as pending review
     *
     * @param id IEP ID
     * @return Updated IEP
     */
    IEP markForReview(Long id);

    /**
     * Automatically expire IEPs that have passed their end date
     * Should be run as a scheduled job
     *
     * @return Number of IEPs expired
     */
    int expireOutdatedIEPs();

    // ========== SERVICE MANAGEMENT ==========

    /**
     * Add a service to an IEP
     *
     * @param iepId IEP ID
     * @param service Service to add
     * @return Updated IEP
     */
    IEP addService(Long iepId, IEPService service);

    /**
     * Remove a service from an IEP
     *
     * @param iepId IEP ID
     * @param serviceId Service ID
     * @return Updated IEP
     */
    IEP removeService(Long iepId, Long serviceId);

    /**
     * Update a service within an IEP
     *
     * @param iepId IEP ID
     * @param service Service to update
     * @return Updated IEP
     */
    IEP updateService(Long iepId, IEPService service);

    // ========== QUERIES ==========

    /**
     * Find all active IEPs
     *
     * @return List of active IEPs
     */
    List<IEP> findAllActiveIEPs();

    /**
     * Find IEPs by status
     *
     * @param status IEP status
     * @return List of IEPs with that status
     */
    List<IEP> findByStatus(IEPStatus status);

    /**
     * Find IEPs needing renewal (ending soon)
     *
     * @param daysThreshold Number of days before expiration
     * @return List of IEPs needing renewal
     */
    List<IEP> findIEPsNeedingRenewal(int daysThreshold);

    /**
     * Find IEPs with review due
     *
     * @return List of IEPs with review date in past or today
     */
    List<IEP> findIEPsWithReviewDue();

    /**
     * Find IEPs by case manager
     *
     * @param caseManager Case manager name
     * @return List of IEPs managed by this person
     */
    List<IEP> findByCaseManager(String caseManager);

    /**
     * Find IEPs with services that need scheduling
     *
     * @return List of IEPs with unscheduled services
     */
    List<IEP> findIEPsWithUnscheduledServices();

    /**
     * Search IEPs by student name
     *
     * @param searchTerm Search term (partial match)
     * @return List of matching IEPs
     */
    List<IEP> searchByStudentName(String searchTerm);

    // ========== STATISTICS ==========

    /**
     * Count active IEPs
     *
     * @return Number of active IEPs
     */
    long countActiveIEPs();

    /**
     * Get IEP count by eligibility category
     *
     * @return Map of category to count
     */
    List<Object[]> getIEPCountByEligibilityCategory();

    /**
     * Get total service minutes per week across all active IEPs
     *
     * @return Total minutes
     */
    int getTotalServiceMinutesPerWeek();

    // ========== VALIDATION ==========

    /**
     * Validate IEP data before saving
     *
     * @param iep IEP to validate
     * @return List of validation errors (empty if valid)
     */
    List<String> validateIEP(IEP iep);

    /**
     * Check if an IEP can be activated
     *
     * @param id IEP ID
     * @return true if IEP can be activated
     */
    boolean canActivate(Long id);

    /**
     * Check if a student already has an active IEP
     *
     * @param studentId Student ID
     * @return true if student has an active IEP
     */
    boolean hasActiveIEP(Long studentId);
}
