package com.heronix.service;

import com.heronix.model.domain.CoTeacher;
import com.heronix.repository.CoTeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing CoTeacher entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Service
@Transactional
public class CoTeacherManagementService {

    private static final Logger logger = LoggerFactory.getLogger(CoTeacherManagementService.class);

    private final CoTeacherRepository coTeacherRepository;

    @Autowired
    public CoTeacherManagementService(CoTeacherRepository coTeacherRepository) {
        this.coTeacherRepository = coTeacherRepository;
    }

    // ========================================================================
    // CRUD OPERATIONS
    // ========================================================================

    /**
     * Get all co-teachers
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getAllCoTeachers() {
        logger.debug("Fetching all co-teachers");
        return coTeacherRepository.findAll();
    }

    /**
     * Get all active co-teachers
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getActiveCoTeachers() {
        logger.debug("Fetching all active co-teachers");
        return coTeacherRepository.findByActiveTrue();
    }

    /**
     * Get co-teacher by ID
     */
    @Transactional(readOnly = true)
    public Optional<CoTeacher> getCoTeacherById(Long id) {
        logger.debug("Fetching co-teacher with ID: {}", id);
        return coTeacherRepository.findById(id);
    }

    /**
     * Get co-teacher by employee ID
     */
    @Transactional(readOnly = true)
    public Optional<CoTeacher> getCoTeacherByEmployeeId(String employeeId) {
        logger.debug("Fetching co-teacher with employee ID: {}", employeeId);
        return coTeacherRepository.findByEmployeeId(employeeId);
    }

    /**
     * Search co-teachers by name
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> searchCoTeachersByName(String name) {
        logger.debug("Searching co-teachers by name: {}", name);
        return coTeacherRepository.findByNameContaining(name);
    }

    /**
     * Save or update a co-teacher
     */
    public CoTeacher saveCoTeacher(CoTeacher coTeacher) {
        if (coTeacher.getId() == null) {
            logger.info("Creating new co-teacher: {} {}", coTeacher.getFirstName(), coTeacher.getLastName());
        } else {
            logger.info("Updating co-teacher with ID: {}", coTeacher.getId());
        }
        return coTeacherRepository.save(coTeacher);
    }

    /**
     * Deactivate a co-teacher
     */
    public void deactivateCoTeacher(Long id) {
        logger.info("Deactivating co-teacher with ID: {}", id);
        coTeacherRepository.findById(id).ifPresent(coTeacher -> {
            coTeacher.setActive(false);
            coTeacherRepository.save(coTeacher);
        });
    }

    /**
     * Delete a co-teacher
     */
    public void deleteCoTeacher(Long id) {
        logger.warn("Deleting co-teacher with ID: {}", id);
        coTeacherRepository.deleteById(id);
    }

    // ========================================================================
    // SPECIALIZATION QUERIES
    // ========================================================================

    /**
     * Get co-teachers by specialization
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getCoTeachersBySpecialization(String specialization) {
        logger.debug("Fetching co-teachers with specialization: {}", specialization);
        return coTeacherRepository.findBySpecializationContainingIgnoreCase(specialization);
    }

    /**
     * Get active co-teachers by specialization
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getActiveCoTeachersBySpecialization(String specialization) {
        logger.debug("Fetching active co-teachers with specialization: {}", specialization);
        return coTeacherRepository.findActiveBySpecialization(specialization);
    }

    // ========================================================================
    // CERTIFICATION QUERIES
    // ========================================================================

    /**
     * Get co-teachers with specific certification
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getCoTeachersWithCertification(String certification) {
        logger.debug("Fetching co-teachers with certification: {}", certification);
        return coTeacherRepository.findByCertification(certification);
    }

    /**
     * Get active co-teachers with specific certification
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getActiveCoTeachersWithCertification(String certification) {
        logger.debug("Fetching active co-teachers with certification: {}", certification);
        return coTeacherRepository.findActiveByCertification(certification);
    }

    // ========================================================================
    // AVAILABILITY QUERIES
    // ========================================================================

    /**
     * Get available co-teachers (not at capacity)
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getAvailableCoTeachers() {
        logger.debug("Fetching available co-teachers");
        return coTeacherRepository.findAvailableCoTeachers();
    }

    /**
     * Get co-teachers at capacity
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getCoTeachersAtCapacity() {
        logger.debug("Fetching co-teachers at capacity");
        return coTeacherRepository.findCoTeachersAtCapacity();
    }

    /**
     * Get available co-teachers with specific specialization
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getAvailableCoTeachersBySpecialization(String specialization) {
        logger.debug("Fetching available co-teachers with specialization: {}", specialization);
        return coTeacherRepository.findAvailableBySpecialization(specialization);
    }

    /**
     * Get available co-teachers with specific certification
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getAvailableCoTeachersWithCertification(String certification) {
        logger.debug("Fetching available co-teachers with certification: {}", certification);
        return coTeacherRepository.findAvailableWithCertification(certification);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Count active co-teachers
     */
    @Transactional(readOnly = true)
    public long countActiveCoTeachers() {
        return coTeacherRepository.countByActiveTrue();
    }

    /**
     * Count inactive co-teachers
     */
    @Transactional(readOnly = true)
    public long countInactiveCoTeachers() {
        return coTeacherRepository.countByActiveFalse();
    }

    /**
     * Count co-teachers by specialization
     */
    @Transactional(readOnly = true)
    public long countBySpecialization(String specialization) {
        return coTeacherRepository.countBySpecialization(specialization);
    }

    /**
     * Get average class load
     */
    @Transactional(readOnly = true)
    public Double getAverageClassLoad() {
        Double avg = coTeacherRepository.getAverageClassLoad();
        return avg != null ? avg : 0.0;
    }

    /**
     * Get co-teachers with most assignments
     */
    @Transactional(readOnly = true)
    public List<Object[]> getCoTeachersWithMostAssignments() {
        return coTeacherRepository.findCoTeachersWithMostAssignments();
    }

    // ========================================================================
    // PREFERENCE QUERIES
    // ========================================================================

    /**
     * Get co-teachers by preferred grade
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getCoTeachersByPreferredGrade(String grade) {
        logger.debug("Fetching co-teachers with preferred grade: {}", grade);
        return coTeacherRepository.findByPreferredGradesContainingIgnoreCase(grade);
    }

    /**
     * Get co-teachers by preferred subject
     */
    @Transactional(readOnly = true)
    public List<CoTeacher> getCoTeachersByPreferredSubject(String subject) {
        logger.debug("Fetching co-teachers with preferred subject: {}", subject);
        return coTeacherRepository.findByPreferredSubjectsContainingIgnoreCase(subject);
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Check if employee ID already exists
     */
    @Transactional(readOnly = true)
    public boolean employeeIdExists(String employeeId) {
        return coTeacherRepository.existsByEmployeeId(employeeId);
    }

    /**
     * Validate co-teacher before save
     */
    public void validateCoTeacher(CoTeacher coTeacher) {
        if (coTeacher.getFirstName() == null || coTeacher.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (coTeacher.getLastName() == null || coTeacher.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        // Check for duplicate employee ID (if provided)
        if (coTeacher.getEmployeeId() != null && !coTeacher.getEmployeeId().trim().isEmpty()) {
            Optional<CoTeacher> existing = coTeacherRepository.findByEmployeeId(coTeacher.getEmployeeId());
            boolean isDuplicate = existing
                .map(ct -> !ct.getId().equals(coTeacher.getId()))
                .orElse(false);
            if (isDuplicate) {
                throw new IllegalArgumentException("Employee ID " + coTeacher.getEmployeeId() + " already exists");
            }
        }
    }
}
