package com.heronix.service.impl;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.enums.IEPStatus;
import com.heronix.repository.IEPRepository;
import com.heronix.repository.IEPServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * IEP Service Implementation
 *
 * Implements business logic for managing Individualized Education Programs (IEPs).
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
@Service
@Slf4j
@Transactional
public class IEPServiceImpl implements com.heronix.service.IEPManagementService {

    @Autowired
    private IEPRepository iepRepository;

    @Autowired
    private IEPServiceRepository iepServiceRepository;

    // ========== CRUD OPERATIONS ==========

    @Override
    public IEP createIEP(IEP iep) {
        // ✅ NULL SAFE: Validate IEP parameter
        if (iep == null) {
            throw new IllegalArgumentException("IEP cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of student ID for logging
        String studentInfo = (iep.getStudent() != null && iep.getStudent().getId() != null)
            ? iep.getStudent().getId().toString() : "Unknown";
        log.info("Creating new IEP for student ID: {}", studentInfo);

        // ✅ BUG FIX #2: Set initial status BEFORE validation (moved from line 66)
        // This allows validation to pass when status is null
        if (iep.getStatus() == null) {
            iep.setStatus(IEPStatus.DRAFT);
        }

        // Validate before creating
        List<String> errors = validateIEP(iep);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("IEP validation failed: " + String.join(", ", errors));
        }

        // Check for existing active IEP
        // ✅ NULL SAFE: Check student and student ID exist
        if (iep.getStudent() != null && iep.getStudent().getId() != null &&
            hasActiveIEP(iep.getStudent().getId())) {
            throw new IllegalStateException("Student already has an active IEP. Please expire the existing IEP first.");
        }

        IEP saved = iepRepository.save(iep);
        // ✅ BUG FIX #3: Safe extraction of IEP ID for logging (handle null saved or null ID)
        String savedIdStr = (saved != null && saved.getId() != null) ? saved.getId().toString() : "null";
        log.info("Created IEP with ID: {}", savedIdStr);
        return saved;
    }

    @Override
    public IEP updateIEP(IEP iep) {
        // ✅ NULL SAFE: Validate IEP parameter
        if (iep == null) {
            throw new IllegalArgumentException("IEP cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of IEP ID for logging
        String iepIdStr = (iep.getId() != null) ? iep.getId().toString() : "null";
        log.info("Updating IEP ID: {}", iepIdStr);

        if (iep.getId() == null) {
            throw new IllegalArgumentException("IEP ID cannot be null for update");
        }

        if (!iepRepository.existsById(iep.getId())) {
            throw new IllegalArgumentException("IEP not found with ID: " + iep.getId());
        }

        // Validate before updating
        List<String> errors = validateIEP(iep);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("IEP validation failed: " + String.join(", ", errors));
        }

        IEP updated = iepRepository.save(iep);
        log.info("Updated IEP ID: {}", updated.getId());
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IEP> findById(Long id) {
        return iepRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IEP> findByIepNumber(String iepNumber) {
        return iepRepository.findByIepNumber(iepNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IEP> findActiveIEPForStudent(Long studentId) {
        // ✅ NULL SAFE: Filter null IEPs before checking active status
        return iepRepository.findByStudentId(studentId)
            .filter(iep -> iep != null && iep.isActive());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IEP> findAllIEPsForStudent(Long studentId) {
        return iepRepository.findByStudentId(studentId)
            .map(List::of)
            .orElse(new ArrayList<>());
    }

    @Override
    public void deleteIEP(Long id) {
        log.info("Deleting IEP ID: {}", id);

        IEP iep = iepRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("IEP not found with ID: " + id));

        // ✅ NULL SAFE: Validate IEP exists before accessing status
        if (iep == null) {
            throw new IllegalArgumentException("IEP object is null for ID: " + id);
        }

        // Only allow deleting draft IEPs
        // ✅ NULL SAFE: Safe extraction of status with default
        IEPStatus status = iep.getStatus() != null ? iep.getStatus() : IEPStatus.DRAFT;
        if (status != IEPStatus.DRAFT) {
            throw new IllegalStateException("Cannot delete IEP with status: " + status + ". Only DRAFT IEPs can be deleted.");
        }

        iepRepository.delete(iep);
        log.info("Deleted IEP ID: {}", id);
    }

    // ========== STATUS MANAGEMENT ==========

    @Override
    public IEP activateIEP(Long id) {
        log.info("Activating IEP ID: {}", id);

        IEP iep = iepRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("IEP not found with ID: " + id));

        if (!canActivate(id)) {
            throw new IllegalStateException("IEP cannot be activated. Please check that all required fields are populated.");
        }

        iep.setStatus(IEPStatus.ACTIVE);
        IEP saved = iepRepository.save(iep);
        log.info("Activated IEP ID: {}", id);
        return saved;
    }

    @Override
    public IEP expireIEP(Long id) {
        log.info("Expiring IEP ID: {}", id);

        IEP iep = iepRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("IEP not found with ID: " + id));

        iep.setStatus(IEPStatus.EXPIRED);
        IEP saved = iepRepository.save(iep);
        log.info("Expired IEP ID: {}", id);
        return saved;
    }

    @Override
    public IEP markForReview(Long id) {
        log.info("Marking IEP ID {} for review", id);

        IEP iep = iepRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("IEP not found with ID: " + id));

        iep.setStatus(IEPStatus.PENDING_REVIEW);
        IEP saved = iepRepository.save(iep);
        log.info("Marked IEP ID {} for review", id);
        return saved;
    }

    @Override
    public int expireOutdatedIEPs() {
        log.info("Running scheduled job to expire outdated IEPs");

        LocalDate today = LocalDate.now();
        List<IEP> expiredIEPs = iepRepository.findExpiredIEPs(today);

        int count = 0;
        for (IEP iep : expiredIEPs) {
            // ✅ NULL SAFE: Skip null IEPs
            if (iep == null) continue;

            iep.setStatus(IEPStatus.EXPIRED);
            iepRepository.save(iep);
            count++;
        }

        log.info("Expired {} outdated IEPs", count);
        return count;
    }

    // ========== SERVICE MANAGEMENT ==========

    @Override
    public IEP addService(Long iepId, IEPService service) {
        log.info("Adding service to IEP ID: {}", iepId);

        IEP iep = iepRepository.findById(iepId)
            .orElseThrow(() -> new IllegalArgumentException("IEP not found with ID: " + iepId));

        // ✅ NULL SAFE: Validate service parameter
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }

        iep.addService(service);
        IEP saved = iepRepository.save(iep);
        log.info("Added service to IEP ID: {}", iepId);
        return saved;
    }

    @Override
    public IEP removeService(Long iepId, Long serviceId) {
        log.info("Removing service ID {} from IEP ID: {}", serviceId, iepId);

        IEP iep = iepRepository.findById(iepId)
            .orElseThrow(() -> new IllegalArgumentException("IEP not found with ID: " + iepId));

        IEPService service = iepServiceRepository.findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Service not found with ID: " + serviceId));

        iep.removeService(service);
        IEP saved = iepRepository.save(iep);
        log.info("Removed service ID {} from IEP ID: {}", serviceId, iepId);
        return saved;
    }

    @Override
    public IEP updateService(Long iepId, IEPService service) {
        // ✅ NULL SAFE: Validate service parameter
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of service ID for logging
        String serviceIdStr = (service.getId() != null) ? service.getId().toString() : "null";
        log.info("Updating service ID {} in IEP ID: {}", serviceIdStr, iepId);

        IEP iep = iepRepository.findById(iepId)
            .orElseThrow(() -> new IllegalArgumentException("IEP not found with ID: " + iepId));

        if (service.getId() == null) {
            throw new IllegalArgumentException("Service ID cannot be null for update");
        }

        // Service will be updated due to cascade
        IEP saved = iepRepository.save(iep);
        log.info("Updated service ID {} in IEP ID: {}", service.getId(), iepId);
        return saved;
    }

    // ========== QUERIES ==========

    @Override
    @Transactional(readOnly = true)
    public List<IEP> findAllActiveIEPs() {
        return iepRepository.findAllActiveIEPs(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IEP> findByStatus(IEPStatus status) {
        return iepRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IEP> findIEPsNeedingRenewal(int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(daysThreshold);
        return iepRepository.findIEPsNeedingRenewal(today, thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IEP> findIEPsWithReviewDue() {
        return iepRepository.findIEPsWithReviewDue(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IEP> findByCaseManager(String caseManager) {
        return iepRepository.findByCaseManager(caseManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IEP> findIEPsWithUnscheduledServices() {
        return iepRepository.findIEPsWithUnscheduledServices();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IEP> searchByStudentName(String searchTerm) {
        return iepRepository.searchByStudentName(searchTerm);
    }

    // ========== STATISTICS ==========

    @Override
    @Transactional(readOnly = true)
    public long countActiveIEPs() {
        return iepRepository.countActiveIEPs(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getIEPCountByEligibilityCategory() {
        return iepRepository.countByEligibilityCategory();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalServiceMinutesPerWeek() {
        List<IEP> activeIEPs = findAllActiveIEPs();
        // ✅ NULL SAFE: Filter null IEPs before mapping to minutes
        return activeIEPs.stream()
            .filter(iep -> iep != null)
            .mapToInt(IEP::getTotalMinutesPerWeek)
            .sum();
    }

    // ========== VALIDATION ==========

    @Override
    public List<String> validateIEP(IEP iep) {
        List<String> errors = new ArrayList<>();

        if (iep.getStudent() == null) {
            errors.add("Student is required");
        }

        if (iep.getStartDate() == null) {
            errors.add("Start date is required");
        }

        if (iep.getEndDate() == null) {
            errors.add("End date is required");
        }

        if (iep.getStartDate() != null && iep.getEndDate() != null) {
            if (iep.getEndDate().isBefore(iep.getStartDate())) {
                errors.add("End date must be after start date");
            }
        }

        if (iep.getStatus() == null) {
            errors.add("Status is required");
        }

        return errors;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canActivate(Long id) {
        IEP iep = iepRepository.findById(id).orElse(null);
        if (iep == null) {
            return false;
        }

        // Check validation
        List<String> errors = validateIEP(iep);
        if (!errors.isEmpty()) {
            return false;
        }

        // Can only activate DRAFT or PENDING_REVIEW IEPs
        return iep.getStatus() == IEPStatus.DRAFT || iep.getStatus() == IEPStatus.PENDING_REVIEW;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveIEP(Long studentId) {
        return findActiveIEPForStudent(studentId).isPresent();
    }
}
