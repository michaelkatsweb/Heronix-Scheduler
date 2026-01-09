package com.heronix.service;

import com.heronix.model.domain.Paraprofessional;
import com.heronix.repository.ParaprofessionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Paraprofessional entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Service
@Transactional
public class ParaprofessionalManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ParaprofessionalManagementService.class);

    private final ParaprofessionalRepository paraprofessionalRepository;

    @Autowired
    public ParaprofessionalManagementService(ParaprofessionalRepository paraprofessionalRepository) {
        this.paraprofessionalRepository = paraprofessionalRepository;
    }

    // ========================================================================
    // CRUD OPERATIONS
    // ========================================================================

    /**
     * Get all paraprofessionals with certifications and skills eagerly loaded
     * Use this to prevent LazyInitializationException in UI
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getAllParaprofessionals() {
        logger.debug("Fetching all paraprofessionals with certifications and skills");
        return paraprofessionalRepository.findAllWithCertificationsAndSkills();
    }

    /**
     * Get all active paraprofessionals
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getActiveParaprofessionals() {
        logger.debug("Fetching all active paraprofessionals");
        return paraprofessionalRepository.findByActiveTrue();
    }

    /**
     * Get paraprofessional by ID
     */
    @Transactional(readOnly = true)
    public Optional<Paraprofessional> getParaprofessionalById(Long id) {
        logger.debug("Fetching paraprofessional with ID: {}", id);
        return paraprofessionalRepository.findById(id);
    }

    /**
     * Get paraprofessional by employee ID
     */
    @Transactional(readOnly = true)
    public Optional<Paraprofessional> getParaprofessionalByEmployeeId(String employeeId) {
        logger.debug("Fetching paraprofessional with employee ID: {}", employeeId);
        return paraprofessionalRepository.findByEmployeeId(employeeId);
    }

    /**
     * Search paraprofessionals by name
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> searchParaprofessionalsByName(String name) {
        logger.debug("Searching paraprofessionals by name: {}", name);
        return paraprofessionalRepository.findByNameContaining(name);
    }

    /**
     * Save or update a paraprofessional
     */
    public Paraprofessional saveParaprofessional(Paraprofessional paraprofessional) {
        if (paraprofessional.getId() == null) {
            logger.info("Creating new paraprofessional: {} {}",
                paraprofessional.getFirstName(), paraprofessional.getLastName());
        } else {
            logger.info("Updating paraprofessional with ID: {}", paraprofessional.getId());
        }
        return paraprofessionalRepository.save(paraprofessional);
    }

    /**
     * Deactivate a paraprofessional
     */
    public void deactivateParaprofessional(Long id) {
        logger.info("Deactivating paraprofessional with ID: {}", id);
        paraprofessionalRepository.findById(id).ifPresent(paraprofessional -> {
            paraprofessional.setActive(false);
            paraprofessionalRepository.save(paraprofessional);
        });
    }

    /**
     * Delete a paraprofessional
     */
    public void deleteParaprofessional(Long id) {
        logger.warn("Deleting paraprofessional with ID: {}", id);
        paraprofessionalRepository.deleteById(id);
    }

    // ========================================================================
    // ROLE TYPE QUERIES
    // ========================================================================

    /**
     * Get paraprofessionals by role type
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsByRoleType(String roleType) {
        logger.debug("Fetching paraprofessionals with role type: {}", roleType);
        return paraprofessionalRepository.findByRoleTypeContainingIgnoreCase(roleType);
    }

    /**
     * Get active paraprofessionals by role type
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getActiveParaprofessionalsByRoleType(String roleType) {
        logger.debug("Fetching active paraprofessionals with role type: {}", roleType);
        return paraprofessionalRepository.findActiveByRoleType(roleType);
    }

    // ========================================================================
    // ASSIGNMENT TYPE QUERIES
    // ========================================================================

    /**
     * Get paraprofessionals by assignment type
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsByAssignmentType(String assignmentType) {
        logger.debug("Fetching paraprofessionals with assignment type: {}", assignmentType);
        return paraprofessionalRepository.findByAssignmentTypeContainingIgnoreCase(assignmentType);
    }

    /**
     * Get active paraprofessionals by assignment type
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getActiveParaprofessionalsByAssignmentType(String assignmentType) {
        logger.debug("Fetching active paraprofessionals with assignment type: {}", assignmentType);
        return paraprofessionalRepository.findActiveByAssignmentType(assignmentType);
    }

    /**
     * Get 1:1 paraprofessionals
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getOneToOneParaprofessionals() {
        logger.debug("Fetching 1:1 paraprofessionals");
        return paraprofessionalRepository.findOneToOneParaprofessionals();
    }

    // ========================================================================
    // SKILL QUERIES
    // ========================================================================

    /**
     * Get paraprofessionals with specific skill
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsWithSkill(String skill) {
        logger.debug("Fetching paraprofessionals with skill: {}", skill);
        return paraprofessionalRepository.findBySkill(skill);
    }

    /**
     * Get active paraprofessionals with specific skill
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getActiveParaprofessionalsWithSkill(String skill) {
        logger.debug("Fetching active paraprofessionals with skill: {}", skill);
        return paraprofessionalRepository.findActiveBySkill(skill);
    }

    // ========================================================================
    // CERTIFICATION QUERIES
    // ========================================================================

    /**
     * Get paraprofessionals with specific certification
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsWithCertification(String certification) {
        logger.debug("Fetching paraprofessionals with certification: {}", certification);
        return paraprofessionalRepository.findByCertification(certification);
    }

    /**
     * Get active paraprofessionals with specific certification
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getActiveParaprofessionalsWithCertification(String certification) {
        logger.debug("Fetching active paraprofessionals with certification: {}", certification);
        return paraprofessionalRepository.findActiveByCertification(certification);
    }

    // ========================================================================
    // TRAINING QUERIES
    // ========================================================================

    /**
     * Get paraprofessionals with medical training
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsWithMedicalTraining() {
        logger.debug("Fetching paraprofessionals with medical training");
        return paraprofessionalRepository.findByMedicalTrainingTrueAndActiveTrue();
    }

    /**
     * Get paraprofessionals with behavioral training
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsWithBehavioralTraining() {
        logger.debug("Fetching paraprofessionals with behavioral training");
        return paraprofessionalRepository.findByBehavioralTrainingTrueAndActiveTrue();
    }

    /**
     * Get paraprofessionals with any special training
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsWithSpecialTraining() {
        logger.debug("Fetching paraprofessionals with special training");
        return paraprofessionalRepository.findWithSpecialTraining();
    }

    // ========================================================================
    // AVAILABILITY QUERIES
    // ========================================================================

    /**
     * Get available paraprofessionals (not at capacity)
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getAvailableParaprofessionals() {
        logger.debug("Fetching available paraprofessionals");
        return paraprofessionalRepository.findAvailableParaprofessionals();
    }

    /**
     * Get paraprofessionals at capacity
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getParaprofessionalsAtCapacity() {
        logger.debug("Fetching paraprofessionals at capacity");
        return paraprofessionalRepository.findParaprofessionalsAtCapacity();
    }

    /**
     * Get available paraprofessionals with specific role type
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getAvailableParaprofessionalsByRoleType(String roleType) {
        logger.debug("Fetching available paraprofessionals with role type: {}", roleType);
        return paraprofessionalRepository.findAvailableByRoleType(roleType);
    }

    /**
     * Get available paraprofessionals with specific skill
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getAvailableParaprofessionalsWithSkill(String skill) {
        logger.debug("Fetching available paraprofessionals with skill: {}", skill);
        return paraprofessionalRepository.findAvailableWithSkill(skill);
    }

    /**
     * Get available paraprofessionals with specific certification
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getAvailableParaprofessionalsWithCertification(String certification) {
        logger.debug("Fetching available paraprofessionals with certification: {}", certification);
        return paraprofessionalRepository.findAvailableWithCertification(certification);
    }

    /**
     * Get available paraprofessionals with medical training
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getAvailableParaprofessionalsWithMedicalTraining() {
        logger.debug("Fetching available paraprofessionals with medical training");
        return paraprofessionalRepository.findAvailableWithMedicalTraining();
    }

    /**
     * Get available paraprofessionals with behavioral training
     */
    @Transactional(readOnly = true)
    public List<Paraprofessional> getAvailableParaprofessionalsWithBehavioralTraining() {
        logger.debug("Fetching available paraprofessionals with behavioral training");
        return paraprofessionalRepository.findAvailableWithBehavioralTraining();
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Count active paraprofessionals
     */
    @Transactional(readOnly = true)
    public long countActiveParaprofessionals() {
        return paraprofessionalRepository.countByActiveTrue();
    }

    /**
     * Count inactive paraprofessionals
     */
    @Transactional(readOnly = true)
    public long countInactiveParaprofessionals() {
        return paraprofessionalRepository.countByActiveFalse();
    }

    /**
     * Count paraprofessionals by role type
     */
    @Transactional(readOnly = true)
    public long countByRoleType(String roleType) {
        return paraprofessionalRepository.countByRoleType(roleType);
    }

    /**
     * Count paraprofessionals by assignment type
     */
    @Transactional(readOnly = true)
    public long countByAssignmentType(String assignmentType) {
        return paraprofessionalRepository.countByAssignmentType(assignmentType);
    }

    /**
     * Get average student load
     */
    @Transactional(readOnly = true)
    public Double getAverageStudentLoad() {
        Double avg = paraprofessionalRepository.getAverageStudentLoad();
        return avg != null ? avg : 0.0;
    }

    /**
     * Get paraprofessionals with most student assignments
     */
    @Transactional(readOnly = true)
    public List<Object[]> getParaprofessionalsWithMostAssignments() {
        return paraprofessionalRepository.findParaprofessionalsWithMostAssignments();
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Check if employee ID already exists
     */
    @Transactional(readOnly = true)
    public boolean employeeIdExists(String employeeId) {
        return paraprofessionalRepository.existsByEmployeeId(employeeId);
    }

    /**
     * Validate paraprofessional before save
     */
    public void validateParaprofessional(Paraprofessional paraprofessional) {
        if (paraprofessional.getFirstName() == null || paraprofessional.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (paraprofessional.getLastName() == null || paraprofessional.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        // Check for duplicate employee ID (if provided)
        if (paraprofessional.getEmployeeId() != null && !paraprofessional.getEmployeeId().trim().isEmpty()) {
            Optional<Paraprofessional> existing = paraprofessionalRepository.findByEmployeeId(paraprofessional.getEmployeeId());
            boolean isDuplicate = existing
                .map(p -> !p.getId().equals(paraprofessional.getId()))
                .orElse(false);
            if (isDuplicate) {
                throw new IllegalArgumentException("Employee ID " + paraprofessional.getEmployeeId() + " already exists");
            }
        }
    }
}
