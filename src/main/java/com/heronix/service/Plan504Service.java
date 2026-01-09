package com.heronix.service;

import com.heronix.model.domain.Plan504;
import com.heronix.model.enums.Plan504Status;

import java.util.List;
import java.util.Optional;

/**
 * Plan 504 Service Interface
 *
 * Provides business logic for managing Section 504 accommodation plans.
 * Handles plan creation, updates, and compliance monitoring.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
public interface Plan504Service {

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new 504 plan
     *
     * @param plan Plan to create
     * @return Created plan with ID
     */
    Plan504 createPlan(Plan504 plan);

    /**
     * Update an existing 504 plan
     *
     * @param plan Plan to update
     * @return Updated plan
     */
    Plan504 updatePlan(Plan504 plan);

    /**
     * Find plan by ID
     *
     * @param id Plan ID
     * @return Optional containing plan if found
     */
    Optional<Plan504> findById(Long id);

    /**
     * Find plan by plan number
     *
     * @param planNumber Plan number
     * @return Optional containing plan if found
     */
    Optional<Plan504> findByPlanNumber(String planNumber);

    /**
     * Find active 504 plan for a student
     *
     * @param studentId Student ID
     * @return Optional containing active plan if found
     */
    Optional<Plan504> findActivePlanForStudent(Long studentId);

    /**
     * Delete a 504 plan
     * Note: Should only be used for draft plans. Active plans should be expired instead.
     *
     * @param id Plan ID
     */
    void deletePlan(Long id);

    // ========== STATUS MANAGEMENT ==========

    /**
     * Activate a 504 plan (change status from DRAFT to ACTIVE)
     *
     * @param id Plan ID
     * @return Updated plan
     */
    Plan504 activatePlan(Long id);

    /**
     * Expire a 504 plan (change status to EXPIRED)
     *
     * @param id Plan ID
     * @return Updated plan
     */
    Plan504 expirePlan(Long id);

    /**
     * Mark plan as pending review
     *
     * @param id Plan ID
     * @return Updated plan
     */
    Plan504 markForReview(Long id);

    /**
     * Automatically expire plans that have passed their end date
     * Should be run as a scheduled job
     *
     * @return Number of plans expired
     */
    int expireOutdatedPlans();

    // ========== QUERIES ==========

    /**
     * Find all active 504 plans
     *
     * @return List of active plans
     */
    List<Plan504> findAllActivePlans();

    /**
     * Find plans by status
     *
     * @param status Plan status
     * @return List of plans with that status
     */
    List<Plan504> findByStatus(Plan504Status status);

    /**
     * Find plans needing renewal (ending soon)
     *
     * @param daysThreshold Number of days before expiration
     * @return List of plans needing renewal
     */
    List<Plan504> findPlansNeedingRenewal(int daysThreshold);

    /**
     * Find plans with review overdue
     *
     * @return List of plans with review date in past
     */
    List<Plan504> findPlansWithOverdueReview();

    /**
     * Find plans by coordinator
     *
     * @param coordinator Coordinator name
     * @return List of plans coordinated by this person
     */
    List<Plan504> findByCoordinator(String coordinator);

    /**
     * Find plans by disability
     *
     * @param disability Disability name
     * @return List of plans for this disability
     */
    List<Plan504> findByDisability(String disability);

    /**
     * Search plans by student name
     *
     * @param searchTerm Search term (partial match)
     * @return List of matching plans
     */
    List<Plan504> searchByStudentName(String searchTerm);

    // ========== STATISTICS ==========

    /**
     * Count active 504 plans
     *
     * @return Number of active plans
     */
    long countActivePlans();

    /**
     * Get plan count by disability
     *
     * @return Map of disability to count
     */
    List<Object[]> getPlanCountByDisability();

    // ========== VALIDATION ==========

    /**
     * Validate 504 plan data before saving
     *
     * @param plan Plan to validate
     * @return List of validation errors (empty if valid)
     */
    List<String> validatePlan(Plan504 plan);

    /**
     * Check if a plan can be activated
     *
     * @param id Plan ID
     * @return true if plan can be activated
     */
    boolean canActivate(Long id);

    /**
     * Check if a student already has an active 504 plan
     *
     * @param studentId Student ID
     * @return true if student has an active 504 plan
     */
    boolean hasActivePlan(Long studentId);
}
