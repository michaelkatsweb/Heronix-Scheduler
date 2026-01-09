package com.heronix.service.impl;

import com.heronix.model.domain.Plan504;
import com.heronix.model.enums.Plan504Status;
import com.heronix.repository.Plan504Repository;
import com.heronix.service.Plan504Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plan 504 Service Implementation
 *
 * Implements business logic for managing Section 504 accommodation plans.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
@Service
@Slf4j
@Transactional
public class Plan504ServiceImpl implements Plan504Service {

    @Autowired
    private Plan504Repository plan504Repository;

    // ========== CRUD OPERATIONS ==========

    @Override
    public Plan504 createPlan(Plan504 plan) {
        // ✅ NULL SAFE: Validate plan parameter
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of student ID for logging
        String studentInfo = (plan.getStudent() != null && plan.getStudent().getId() != null)
            ? plan.getStudent().getId().toString() : "Unknown";
        log.info("Creating new 504 plan for student ID: {}", studentInfo);

        // ✅ BUG FIX: Set initial status BEFORE validation (moved from line 62)
        // This allows validation to pass when status is null
        if (plan.getStatus() == null) {
            plan.setStatus(Plan504Status.DRAFT);
        }

        // Validate before creating
        List<String> errors = validatePlan(plan);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("504 Plan validation failed: " + String.join(", ", errors));
        }

        // Check for existing active plan
        // ✅ NULL SAFE: Check student and student ID exist
        if (plan.getStudent() != null && plan.getStudent().getId() != null &&
            hasActivePlan(plan.getStudent().getId())) {
            throw new IllegalStateException("Student already has an active 504 Plan. Please expire the existing plan first.");
        }

        Plan504 saved = plan504Repository.save(plan);
        log.info("Created 504 Plan with ID: {}", saved.getId());
        return saved;
    }

    @Override
    public Plan504 updatePlan(Plan504 plan) {
        // ✅ NULL SAFE: Validate plan parameter
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of plan ID for logging
        String planIdStr = (plan.getId() != null) ? plan.getId().toString() : "null";
        log.info("Updating 504 Plan ID: {}", planIdStr);

        if (plan.getId() == null) {
            throw new IllegalArgumentException("Plan ID cannot be null for update");
        }

        if (!plan504Repository.existsById(plan.getId())) {
            throw new IllegalArgumentException("504 Plan not found with ID: " + plan.getId());
        }

        // Validate before updating
        List<String> errors = validatePlan(plan);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("504 Plan validation failed: " + String.join(", ", errors));
        }

        Plan504 updated = plan504Repository.save(plan);
        log.info("Updated 504 Plan ID: {}", updated.getId());
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan504> findById(Long id) {
        return plan504Repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan504> findByPlanNumber(String planNumber) {
        return plan504Repository.findByPlanNumber(planNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan504> findActivePlanForStudent(Long studentId) {
        // ✅ NULL SAFE: Filter null plans before checking active status
        return plan504Repository.findByStudentId(studentId)
            .filter(plan -> plan != null && plan.isActive());
    }

    @Override
    public void deletePlan(Long id) {
        log.info("Deleting 504 Plan ID: {}", id);

        Plan504 plan = plan504Repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("504 Plan not found with ID: " + id));

        // ✅ NULL SAFE: Validate plan exists before accessing status
        if (plan == null) {
            throw new IllegalArgumentException("Plan object is null for ID: " + id);
        }

        // Only allow deleting draft plans
        // ✅ NULL SAFE: Safe extraction of status with default
        Plan504Status status = plan.getStatus() != null ? plan.getStatus() : Plan504Status.DRAFT;
        if (status != Plan504Status.DRAFT) {
            throw new IllegalStateException("Cannot delete 504 Plan with status: " + status + ". Only DRAFT plans can be deleted.");
        }

        plan504Repository.delete(plan);
        log.info("Deleted 504 Plan ID: {}", id);
    }

    // ========== STATUS MANAGEMENT ==========

    @Override
    public Plan504 activatePlan(Long id) {
        log.info("Activating 504 Plan ID: {}", id);

        Plan504 plan = plan504Repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("504 Plan not found with ID: " + id));

        if (!canActivate(id)) {
            throw new IllegalStateException("504 Plan cannot be activated. Please check that all required fields are populated.");
        }

        plan.setStatus(Plan504Status.ACTIVE);
        Plan504 saved = plan504Repository.save(plan);
        log.info("Activated 504 Plan ID: {}", id);
        return saved;
    }

    @Override
    public Plan504 expirePlan(Long id) {
        log.info("Expiring 504 Plan ID: {}", id);

        Plan504 plan = plan504Repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("504 Plan not found with ID: " + id));

        plan.setStatus(Plan504Status.EXPIRED);
        Plan504 saved = plan504Repository.save(plan);
        log.info("Expired 504 Plan ID: {}", id);
        return saved;
    }

    @Override
    public Plan504 markForReview(Long id) {
        log.info("Marking 504 Plan ID {} for review", id);

        Plan504 plan = plan504Repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("504 Plan not found with ID: " + id));

        plan.setStatus(Plan504Status.PENDING_REVIEW);
        Plan504 saved = plan504Repository.save(plan);
        log.info("Marked 504 Plan ID {} for review", id);
        return saved;
    }

    @Override
    public int expireOutdatedPlans() {
        log.info("Running scheduled job to expire outdated 504 plans");

        LocalDate today = LocalDate.now();
        List<Plan504> expiredPlans = plan504Repository.findExpiredPlans(today);

        int count = 0;
        for (Plan504 plan : expiredPlans) {
            // ✅ NULL SAFE: Skip null plans
            if (plan == null) continue;

            plan.setStatus(Plan504Status.EXPIRED);
            plan504Repository.save(plan);
            count++;
        }

        log.info("Expired {} outdated 504 plans", count);
        return count;
    }

    // ========== QUERIES ==========

    @Override
    @Transactional(readOnly = true)
    public List<Plan504> findAllActivePlans() {
        return plan504Repository.findAllActivePlans(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan504> findByStatus(Plan504Status status) {
        return plan504Repository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan504> findPlansNeedingRenewal(int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(daysThreshold);
        return plan504Repository.findPlansNeedingRenewal(today, thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan504> findPlansWithOverdueReview() {
        return plan504Repository.findPlansWithOverdueReview(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan504> findByCoordinator(String coordinator) {
        return plan504Repository.findByCoordinator(coordinator);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan504> findByDisability(String disability) {
        return plan504Repository.findByDisability(disability);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan504> searchByStudentName(String searchTerm) {
        return plan504Repository.searchByStudentName(searchTerm);
    }

    // ========== STATISTICS ==========

    @Override
    @Transactional(readOnly = true)
    public long countActivePlans() {
        return plan504Repository.countActivePlans(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getPlanCountByDisability() {
        return plan504Repository.countByDisability();
    }

    // ========== VALIDATION ==========

    @Override
    public List<String> validatePlan(Plan504 plan) {
        List<String> errors = new ArrayList<>();

        if (plan.getStudent() == null) {
            errors.add("Student is required");
        }

        if (plan.getStartDate() == null) {
            errors.add("Start date is required");
        }

        if (plan.getEndDate() == null) {
            errors.add("End date is required");
        }

        if (plan.getStartDate() != null && plan.getEndDate() != null) {
            if (plan.getEndDate().isBefore(plan.getStartDate())) {
                errors.add("End date must be after start date");
            }
        }

        if (plan.getAccommodations() == null || plan.getAccommodations().trim().isEmpty()) {
            errors.add("Accommodations are required");
        }

        if (plan.getStatus() == null) {
            errors.add("Status is required");
        }

        return errors;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canActivate(Long id) {
        Plan504 plan = plan504Repository.findById(id).orElse(null);
        if (plan == null) {
            return false;
        }

        // Check validation
        List<String> errors = validatePlan(plan);
        if (!errors.isEmpty()) {
            return false;
        }

        // Can only activate DRAFT or PENDING_REVIEW plans
        return plan.getStatus() == Plan504Status.DRAFT || plan.getStatus() == Plan504Status.PENDING_REVIEW;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActivePlan(Long studentId) {
        return findActivePlanForStudent(studentId).isPresent();
    }
}
