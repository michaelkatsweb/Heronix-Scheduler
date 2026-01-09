package com.heronix.service;

import com.heronix.model.domain.Substitute;
import com.heronix.model.domain.SubstituteAssignment;
import com.heronix.model.enums.SubstituteType;
import com.heronix.repository.SubstituteRepository;
import com.heronix.repository.SubstituteAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Substitute entities and their assignments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Service
@Transactional
public class SubstituteManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SubstituteManagementService.class);

    private final SubstituteRepository substituteRepository;
    private final SubstituteAssignmentRepository assignmentRepository;

    @Autowired
    public SubstituteManagementService(SubstituteRepository substituteRepository,
                                      SubstituteAssignmentRepository assignmentRepository) {
        this.substituteRepository = substituteRepository;
        this.assignmentRepository = assignmentRepository;
    }

    // ==================== SUBSTITUTE CRUD OPERATIONS ====================

    /**
     * Get all substitutes with certifications eagerly loaded
     * Use this to prevent LazyInitializationException in UI
     */
    @Transactional(readOnly = true)
    public List<Substitute> getAllSubstitutes() {
        logger.debug("Fetching all substitutes with certifications");
        return substituteRepository.findAllWithCertifications();
    }

    /**
     * Get all active substitutes
     */
    @Transactional(readOnly = true)
    public List<Substitute> getActiveSubstitutes() {
        logger.debug("Fetching all active substitutes");
        return substituteRepository.findByActiveTrue();
    }

    /**
     * Get substitute by ID
     */
    @Transactional(readOnly = true)
    public Optional<Substitute> getSubstituteById(Long id) {
        logger.debug("Fetching substitute with ID: {}", id);
        return substituteRepository.findById(id);
    }

    /**
     * Get substitute by employee ID
     */
    @Transactional(readOnly = true)
    public Optional<Substitute> getSubstituteByEmployeeId(String employeeId) {
        logger.debug("Fetching substitute with employee ID: {}", employeeId);
        return substituteRepository.findByEmployeeId(employeeId);
    }

    /**
     * Get substitutes by type
     */
    @Transactional(readOnly = true)
    public List<Substitute> getSubstitutesByType(SubstituteType type) {
        logger.debug("Fetching substitutes of type: {}", type);
        return substituteRepository.findByType(type);
    }

    /**
     * Get active substitutes by type
     */
    @Transactional(readOnly = true)
    public List<Substitute> getActiveSubstitutesByType(SubstituteType type) {
        logger.debug("Fetching active substitutes of type: {}", type);
        return substituteRepository.findByTypeAndActiveTrue(type);
    }

    /**
     * Search substitutes by name
     */
    @Transactional(readOnly = true)
    public List<Substitute> searchSubstitutesByName(String name) {
        logger.debug("Searching substitutes by name: {}", name);
        return substituteRepository.findByNameContaining(name);
    }

    /**
     * Get substitutes with specific certification
     */
    @Transactional(readOnly = true)
    public List<Substitute> getSubstitutesWithCertification(String certification) {
        logger.debug("Fetching substitutes with certification: {}", certification);
        return substituteRepository.findByCertification(certification);
    }

    /**
     * Save or update a substitute
     */
    public Substitute saveSubstitute(Substitute substitute) {
        if (substitute == null) {
            logger.warn("Attempted to save null substitute");
            return null;
        }
        if (substitute.getId() == null) {
            logger.info("Creating new substitute: {} {}", substitute.getFirstName(), substitute.getLastName());
        } else {
            logger.info("Updating substitute with ID: {}", substitute.getId());
        }
        return substituteRepository.save(substitute);
    }

    /**
     * Create a new substitute
     */
    public Substitute createSubstitute(String firstName, String lastName, SubstituteType type,
                                      String employeeId, String email, String phoneNumber) {
        logger.info("Creating new substitute: {} {} ({})", firstName, lastName, type);

        if (employeeId != null && substituteRepository.existsByEmployeeId(employeeId)) {
            throw new IllegalArgumentException("Substitute with employee ID " + employeeId + " already exists");
        }

        Substitute substitute = new Substitute(firstName, lastName, type);
        substitute.setEmployeeId(employeeId);
        substitute.setEmail(email);
        substitute.setPhoneNumber(phoneNumber);

        return substituteRepository.save(substitute);
    }

    /**
     * Update substitute information
     */
    public Substitute updateSubstitute(Long id, Substitute updatedSubstitute) {
        if (id == null) {
            logger.warn("Attempted to update substitute with null ID");
            throw new IllegalArgumentException("Substitute ID cannot be null");
        }
        if (updatedSubstitute == null) {
            logger.warn("Attempted to update substitute with null data");
            throw new IllegalArgumentException("Updated substitute data cannot be null");
        }

        logger.info("Updating substitute with ID: {}", id);

        return substituteRepository.findById(id)
                .map(existing -> {
                    existing.setFirstName(updatedSubstitute.getFirstName());
                    existing.setLastName(updatedSubstitute.getLastName());
                    existing.setEmail(updatedSubstitute.getEmail());
                    existing.setPhoneNumber(updatedSubstitute.getPhoneNumber());
                    existing.setType(updatedSubstitute.getType());
                    existing.setActive(updatedSubstitute.getActive());
                    existing.setCertifications(updatedSubstitute.getCertifications());
                    existing.setNotes(updatedSubstitute.getNotes());
                    existing.setAvailability(updatedSubstitute.getAvailability());
                    existing.setHourlyRate(updatedSubstitute.getHourlyRate());
                    existing.setDailyRate(updatedSubstitute.getDailyRate());
                    return substituteRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Substitute not found with ID: " + id));
    }

    /**
     * Deactivate a substitute
     */
    public void deactivateSubstitute(Long id) {
        logger.info("Deactivating substitute with ID: {}", id);
        substituteRepository.findById(id).ifPresent(substitute -> {
            substitute.setActive(false);
            substituteRepository.save(substitute);
        });
    }

    /**
     * Delete a substitute
     */
    public void deleteSubstitute(Long id) {
        logger.warn("Deleting substitute with ID: {}", id);
        substituteRepository.deleteById(id);
    }

    // ==================== ASSIGNMENT OPERATIONS ====================

    /**
     * Get all assignments
     */
    @Transactional(readOnly = true)
    public List<SubstituteAssignment> getAllAssignments() {
        logger.debug("Fetching all assignments");
        return assignmentRepository.findAll();
    }

    /**
     * Get assignment by ID
     */
    @Transactional(readOnly = true)
    public Optional<SubstituteAssignment> getAssignmentById(Long id) {
        logger.debug("Fetching assignment with ID: {}", id);
        return assignmentRepository.findById(id);
    }

    /**
     * Get assignments for a substitute
     */
    @Transactional(readOnly = true)
    public List<SubstituteAssignment> getAssignmentsForSubstitute(Long substituteId) {
        logger.debug("Fetching assignments for substitute ID: {}", substituteId);
        return assignmentRepository.findBySubstituteId(substituteId);
    }

    /**
     * Get assignments for a date
     */
    @Transactional(readOnly = true)
    public List<SubstituteAssignment> getAssignmentsForDate(LocalDate date) {
        logger.debug("Fetching assignments for date: {}", date);
        return assignmentRepository.findByAssignmentDate(date);
    }

    /**
     * Get assignments between dates
     */
    @Transactional(readOnly = true)
    public List<SubstituteAssignment> getAssignmentsBetweenDates(LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching assignments between {} and {}", startDate, endDate);
        return assignmentRepository.findByAssignmentDateBetween(startDate, endDate);
    }

    /**
     * Get assignments for a teacher being replaced
     */
    @Transactional(readOnly = true)
    public List<SubstituteAssignment> getAssignmentsForTeacher(Long teacherId) {
        logger.debug("Fetching assignments for teacher ID: {}", teacherId);
        return assignmentRepository.findByReplacedTeacherId(teacherId);
    }

    /**
     * Get floater assignments for a date
     */
    @Transactional(readOnly = true)
    public List<SubstituteAssignment> getFloaterAssignmentsForDate(LocalDate date) {
        logger.debug("Fetching floater assignments for date: {}", date);
        return assignmentRepository.findByIsFloaterTrueAndAssignmentDate(date);
    }

    /**
     * Save or update an assignment
     */
    public SubstituteAssignment saveAssignment(SubstituteAssignment assignment) {
        if (assignment == null) {
            logger.warn("Attempted to save null assignment");
            return null;
        }
        if (assignment.getId() == null) {
            logger.info("Creating new assignment for substitute: {}",
                    assignment.getSubstitute() != null ? assignment.getSubstitute().getFullName() : "null");
        } else {
            logger.info("Updating assignment with ID: {}", assignment.getId());
        }
        return assignmentRepository.save(assignment);
    }

    /**
     * Delete an assignment
     */
    public void deleteAssignment(Long id) {
        logger.warn("Deleting assignment with ID: {}", id);
        assignmentRepository.deleteById(id);
    }

    // ==================== STATISTICS AND COUNTS ====================

    /**
     * Count active substitutes
     */
    @Transactional(readOnly = true)
    public long countActiveSubstitutes() {
        return substituteRepository.countByActiveTrue();
    }

    /**
     * Count substitutes by type
     */
    @Transactional(readOnly = true)
    public long countSubstitutesByType(SubstituteType type) {
        return substituteRepository.countByType(type);
    }

    /**
     * Count assignments for a date
     */
    @Transactional(readOnly = true)
    public long countAssignmentsForDate(LocalDate date) {
        return assignmentRepository.countByAssignmentDate(date);
    }

    /**
     * Count floater assignments for a date
     */
    @Transactional(readOnly = true)
    public long countFloaterAssignmentsForDate(LocalDate date) {
        return assignmentRepository.countByIsFloaterTrueAndAssignmentDate(date);
    }

    /**
     * Get total hours for a substitute in a date range
     */
    @Transactional(readOnly = true)
    public Double getTotalHoursForSubstitute(Long substituteId, LocalDate startDate, LocalDate endDate) {
        Double total = assignmentRepository.getTotalHoursForSubstituteInDateRange(substituteId, startDate, endDate);
        return total != null ? total : 0.0;
    }

    /**
     * Get total pay for a substitute in a date range
     */
    @Transactional(readOnly = true)
    public Double getTotalPayForSubstitute(Long substituteId, LocalDate startDate, LocalDate endDate) {
        Double total = assignmentRepository.getTotalPayForSubstituteInDateRange(substituteId, startDate, endDate);
        return total != null ? total : 0.0;
    }
}
