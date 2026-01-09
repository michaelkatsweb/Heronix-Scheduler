package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSequence;
import com.heronix.model.domain.CourseSequenceStep;
import com.heronix.model.domain.SubjectArea;
import com.heronix.repository.CourseSequenceRepository;
import com.heronix.repository.CourseSequenceStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing Course Sequences and Pathways
 *
 * Location: src/main/java/com/eduscheduler/service/CourseSequenceService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 6, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseSequenceService {

    private final CourseSequenceRepository sequenceRepository;
    private final CourseSequenceStepRepository stepRepository;

    // ========================================================================
    // BASIC CRUD OPERATIONS - COURSE SEQUENCE
    // ========================================================================

    /**
     * Get all course sequences
     *
     * @return List of all sequences
     */
    public List<CourseSequence> getAllSequences() {
        return sequenceRepository.findAll();
    }

    /**
     * Get all active sequences
     *
     * @return List of active sequences
     */
    public List<CourseSequence> getActiveSequences() {
        return sequenceRepository.findByActiveTrue();
    }

    /**
     * Get sequence by ID
     *
     * @param id Sequence ID
     * @return Optional containing sequence if found
     */
    public Optional<CourseSequence> getSequenceById(Long id) {
        return sequenceRepository.findById(id);
    }

    /**
     * Get sequence by code
     *
     * @param code Sequence code
     * @return Optional containing sequence if found
     */
    public Optional<CourseSequence> getSequenceByCode(String code) {
        return sequenceRepository.findByCode(code);
    }

    /**
     * Create new course sequence
     *
     * @param sequence Course sequence to create
     * @return Created sequence
     */
    @Transactional
    public CourseSequence createSequence(CourseSequence sequence) {
        log.info("Creating course sequence: {} ({})", sequence.getName(), sequence.getCode());

        // Check for duplicate code
        if (sequence.getCode() != null && sequenceRepository.existsByCodeIgnoreCase(sequence.getCode())) {
            throw new IllegalArgumentException(
                "Course sequence with code '" + sequence.getCode() + "' already exists");
        }

        CourseSequence saved = sequenceRepository.save(sequence);
        log.info("Created course sequence: {}", saved);
        return saved;
    }

    /**
     * Update course sequence
     *
     * @param id Sequence ID
     * @param updates Updated sequence data
     * @return Updated sequence
     */
    @Transactional
    public CourseSequence updateSequence(Long id, CourseSequence updates) {
        CourseSequence existing = sequenceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Course sequence not found: " + id));

        log.info("Updating course sequence: {}", id);

        // Update fields
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getCode() != null && !updates.getCode().equals(existing.getCode())) {
            if (sequenceRepository.existsByCodeIgnoreCase(updates.getCode())) {
                throw new IllegalArgumentException(
                    "Course sequence with code '" + updates.getCode() + "' already exists");
            }
            existing.setCode(updates.getCode());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getSubjectArea() != null) {
            existing.setSubjectArea(updates.getSubjectArea());
        }
        if (updates.getSequenceType() != null) {
            existing.setSequenceType(updates.getSequenceType());
        }
        if (updates.getMinGPARecommended() != null) {
            existing.setMinGPARecommended(updates.getMinGPARecommended());
        }
        if (updates.getTotalYears() != null) {
            existing.setTotalYears(updates.getTotalYears());
        }
        if (updates.getTotalCredits() != null) {
            existing.setTotalCredits(updates.getTotalCredits());
        }
        if (updates.getActive() != null) {
            existing.setActive(updates.getActive());
        }

        return sequenceRepository.save(existing);
    }

    /**
     * Delete course sequence (soft delete by setting active=false)
     *
     * @param id Sequence ID
     */
    @Transactional
    public void deleteSequence(Long id) {
        CourseSequence sequence = sequenceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Course sequence not found: " + id));

        log.info("Deleting (deactivating) course sequence: {}", id);
        sequence.setActive(false);
        sequenceRepository.save(sequence);
    }

    // ========================================================================
    // QUERY OPERATIONS - COURSE SEQUENCE
    // ========================================================================

    /**
     * Get sequences by subject area
     *
     * @param subjectArea Subject area
     * @return List of sequences
     */
    public List<CourseSequence> getSequencesBySubjectArea(SubjectArea subjectArea) {
        return sequenceRepository.findBySubjectAreaAndActiveTrue(subjectArea);
    }

    /**
     * Get sequences by type
     *
     * @param type Sequence type
     * @return List of sequences
     */
    public List<CourseSequence> getSequencesByType(CourseSequence.SequenceType type) {
        return sequenceRepository.findBySequenceTypeAndActiveTrue(type);
    }

    /**
     * Get sequences by subject and type
     *
     * @param subjectArea Subject area
     * @param type Sequence type
     * @return List of sequences
     */
    public List<CourseSequence> getSequencesBySubjectAndType(SubjectArea subjectArea, CourseSequence.SequenceType type) {
        return sequenceRepository.findBySubjectAreaAndSequenceType(subjectArea, type);
    }

    /**
     * Search sequences by term
     *
     * @param searchTerm Search term
     * @return List of matching sequences
     */
    public List<CourseSequence> searchSequences(String searchTerm) {
        return sequenceRepository.searchSequences(searchTerm);
    }

    /**
     * Get sequences suitable for student GPA
     *
     * @param studentGPA Student's GPA
     * @return List of suitable sequences
     */
    public List<CourseSequence> getSequencesForStudentGPA(Double studentGPA) {
        return sequenceRepository.findSequencesForStudentGPA(studentGPA);
    }

    /**
     * Get sequences containing a course
     *
     * @param courseId Course ID
     * @return List of sequences
     */
    public List<CourseSequence> getSequencesContainingCourse(Long courseId) {
        return sequenceRepository.findSequencesContainingCourse(courseId);
    }

    /**
     * Get sequences for grade level
     *
     * @param gradeLevel Grade level (9, 10, 11, 12)
     * @return List of sequences
     */
    public List<CourseSequence> getSequencesForGradeLevel(Integer gradeLevel) {
        return sequenceRepository.findSequencesForGradeLevel(gradeLevel);
    }

    /**
     * Get IEP-friendly sequences
     *
     * @return List of active IEP-friendly sequences
     */
    public List<CourseSequence> getIEPFriendlySequences() {
        return sequenceRepository.findByIepFriendlyTrueAndActiveTrue();
    }

    /**
     * Find sequences by prerequisite skill level
     *
     * Searches for sequences where the prerequisite skills contain the specified skill level.
     * Useful for matching students to appropriate sequences based on their current abilities.
     *
     * @param skillLevel Skill level to search for (e.g., "Algebra I", "reading level 9", "computer literate")
     * @return List of active sequences with matching prerequisite skills
     */
    public List<CourseSequence> getSequencesByPrerequisite(String skillLevel) {
        if (skillLevel == null || skillLevel.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return sequenceRepository.findSequencesByPrerequisite(skillLevel.trim());
    }

    /**
     * Find sequences by graduation requirement category
     *
     * Searches for sequences that fulfill a specific graduation requirement category.
     * Useful for finding sequences that count toward specific diploma requirements.
     *
     * @param category Graduation requirement category (e.g., "Mathematics", "Science", "English", "Social Studies")
     * @return List of active sequences matching the graduation requirement category
     */
    public List<CourseSequence> getSequencesByGraduationRequirement(String category) {
        if (category == null || category.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return sequenceRepository.findByGraduationRequirementCategory(category.trim());
    }

    // ========================================================================
    // BASIC CRUD OPERATIONS - SEQUENCE STEPS
    // ========================================================================

    /**
     * Get all steps for a sequence
     *
     * @param sequenceId Sequence ID
     * @return List of steps in order
     */
    public List<CourseSequenceStep> getStepsForSequence(Long sequenceId) {
        CourseSequence sequence = sequenceRepository.findById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        return stepRepository.findByCourseSequenceOrderByStepOrderAsc(sequence);
    }

    /**
     * Get step by ID
     *
     * @param stepId Step ID
     * @return Optional containing step if found
     */
    public Optional<CourseSequenceStep> getStepById(Long stepId) {
        return stepRepository.findById(stepId);
    }

    /**
     * Add step to sequence
     *
     * @param sequenceId Sequence ID
     * @param step Step to add
     * @return Created step
     */
    @Transactional
    public CourseSequenceStep addStepToSequence(Long sequenceId, CourseSequenceStep step) {
        CourseSequence sequence = sequenceRepository.findById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        log.info("Adding step to sequence {}: {}", sequenceId, step.getCourse().getCourseCode());

        // Set the sequence
        step.setCourseSequence(sequence);

        // If no step order provided, add at end
        if (step.getStepOrder() == null) {
            Integer maxOrder = stepRepository.getMaxStepOrder(sequence);
            step.setStepOrder(maxOrder != null ? maxOrder + 1 : 1);
        }

        // Check for duplicate step order
        if (stepRepository.existsByCourseSequenceAndStepOrder(sequence, step.getStepOrder())) {
            throw new IllegalArgumentException(
                "Step order " + step.getStepOrder() + " already exists in sequence");
        }

        // Check for duplicate course
        if (stepRepository.existsByCourseSequenceAndCourse(sequence, step.getCourse())) {
            throw new IllegalArgumentException(
                "Course " + step.getCourse().getCourseCode() + " already exists in sequence");
        }

        CourseSequenceStep saved = stepRepository.save(step);

        // Update sequence totals
        recalculateSequenceTotals(sequence);

        return saved;
    }

    /**
     * Update sequence step
     *
     * @param stepId Step ID
     * @param updates Updated step data
     * @return Updated step
     */
    @Transactional
    public CourseSequenceStep updateStep(Long stepId, CourseSequenceStep updates) {
        CourseSequenceStep existing = stepRepository.findById(stepId)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));

        log.info("Updating step: {}", stepId);

        // Update fields
        if (updates.getCourse() != null) {
            existing.setCourse(updates.getCourse());
        }
        if (updates.getStepOrder() != null) {
            existing.setStepOrder(updates.getStepOrder());
        }
        if (updates.getRecommendedGradeLevel() != null) {
            existing.setRecommendedGradeLevel(updates.getRecommendedGradeLevel());
        }
        if (updates.getIsRequired() != null) {
            existing.setIsRequired(updates.getIsRequired());
        }
        if (updates.getAlternativeCourses() != null) {
            existing.setAlternativeCourses(updates.getAlternativeCourses());
        }
        if (updates.getNotes() != null) {
            existing.setNotes(updates.getNotes());
        }
        if (updates.getMinGradeFromPrevious() != null) {
            existing.setMinGradeFromPrevious(updates.getMinGradeFromPrevious());
        }
        if (updates.getCredits() != null) {
            existing.setCredits(updates.getCredits());
        }
        if (updates.getIsTerminal() != null) {
            existing.setIsTerminal(updates.getIsTerminal());
        }

        CourseSequenceStep saved = stepRepository.save(existing);

        // Recalculate sequence totals
        recalculateSequenceTotals(existing.getCourseSequence());

        return saved;
    }

    /**
     * Delete step from sequence
     *
     * @param stepId Step ID
     */
    @Transactional
    public void deleteStep(Long stepId) {
        CourseSequenceStep step = stepRepository.findById(stepId)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));

        CourseSequence sequence = step.getCourseSequence();

        log.info("Deleting step: {}", stepId);
        stepRepository.delete(step);

        // Recalculate sequence totals
        recalculateSequenceTotals(sequence);
    }

    /**
     * Reorder step in sequence
     *
     * @param stepId Step ID
     * @param newOrder New step order
     * @return Updated step
     */
    @Transactional
    public CourseSequenceStep reorderStep(Long stepId, Integer newOrder) {
        CourseSequenceStep step = stepRepository.findById(stepId)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));

        Integer oldOrder = step.getStepOrder();
        if (oldOrder.equals(newOrder)) {
            return step; // No change needed
        }

        CourseSequence sequence = step.getCourseSequence();
        List<CourseSequenceStep> allSteps = stepRepository.findByCourseSequenceOrderByStepOrderAsc(sequence);

        log.info("Reordering step {} from order {} to {}", stepId, oldOrder, newOrder);

        // Shift other steps
        for (CourseSequenceStep s : allSteps) {
            if (s.getId().equals(stepId)) continue;

            int currentOrder = s.getStepOrder();
            if (oldOrder < newOrder) {
                // Moving down: shift steps up
                if (currentOrder > oldOrder && currentOrder <= newOrder) {
                    s.setStepOrder(currentOrder - 1);
                    stepRepository.save(s);
                }
            } else {
                // Moving up: shift steps down
                if (currentOrder >= newOrder && currentOrder < oldOrder) {
                    s.setStepOrder(currentOrder + 1);
                    stepRepository.save(s);
                }
            }
        }

        // Update target step
        step.setStepOrder(newOrder);
        return stepRepository.save(step);
    }

    // ========================================================================
    // QUERY OPERATIONS - SEQUENCE STEPS
    // ========================================================================

    /**
     * Get first step in sequence
     *
     * @param sequenceId Sequence ID
     * @return Optional containing first step if found
     */
    public Optional<CourseSequenceStep> getFirstStep(Long sequenceId) {
        CourseSequence sequence = sequenceRepository.findById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        return stepRepository.findFirstStep(sequence);
    }

    /**
     * Get last step in sequence
     *
     * @param sequenceId Sequence ID
     * @return Optional containing last step if found
     */
    public Optional<CourseSequenceStep> getLastStep(Long sequenceId) {
        CourseSequence sequence = sequenceRepository.findById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        return stepRepository.findLastStep(sequence);
    }

    /**
     * Get next step after current
     *
     * @param currentStepId Current step ID
     * @return Optional containing next step if found
     */
    public Optional<CourseSequenceStep> getNextStep(Long currentStepId) {
        CourseSequenceStep currentStep = stepRepository.findById(currentStepId)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + currentStepId));

        return stepRepository.findNextStep(currentStep.getCourseSequence(), currentStep.getStepOrder());
    }

    /**
     * Get previous step before current
     *
     * @param currentStepId Current step ID
     * @return Optional containing previous step if found
     */
    public Optional<CourseSequenceStep> getPreviousStep(Long currentStepId) {
        CourseSequenceStep currentStep = stepRepository.findById(currentStepId)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + currentStepId));

        return stepRepository.findPreviousStep(currentStep.getCourseSequence(), currentStep.getStepOrder());
    }

    /**
     * Get required steps for sequence
     *
     * @param sequenceId Sequence ID
     * @return List of required steps
     */
    public List<CourseSequenceStep> getRequiredSteps(Long sequenceId) {
        CourseSequence sequence = sequenceRepository.findById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        return stepRepository.findByCourseSequenceAndIsRequiredTrue(sequence);
    }

    /**
     * Get optional steps for sequence
     *
     * @param sequenceId Sequence ID
     * @return List of optional steps
     */
    public List<CourseSequenceStep> getOptionalSteps(Long sequenceId) {
        CourseSequence sequence = sequenceRepository.findById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        return stepRepository.findByCourseSequenceAndIsRequiredFalse(sequence);
    }

    /**
     * Get steps for grade level
     *
     * @param sequenceId Sequence ID
     * @param gradeLevel Grade level
     * @return List of steps
     */
    public List<CourseSequenceStep> getStepsForGradeLevel(Long sequenceId, Integer gradeLevel) {
        CourseSequence sequence = sequenceRepository.findById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        return stepRepository.findByCourseSequenceAndRecommendedGradeLevel(sequence, gradeLevel);
    }

    // ========================================================================
    // UTILITY OPERATIONS
    // ========================================================================

    /**
     * Recalculate sequence totals (years, credits)
     *
     * @param sequence Course sequence
     */
    @Transactional
    public void recalculateSequenceTotals(CourseSequence sequence) {
        List<CourseSequenceStep> steps = stepRepository.findByCourseSequenceOrderByStepOrderAsc(sequence);

        if (steps.isEmpty()) {
            sequence.setTotalYears(0);
            sequence.setTotalCredits(0.0);
        } else {
            // Calculate total credits
            Double totalCredits = stepRepository.getTotalCredits(sequence);
            sequence.setTotalCredits(totalCredits != null ? totalCredits : 0.0);

            // Calculate total years (max grade level - min grade level + 1)
            List<Integer> gradeLevels = steps.stream()
                .map(CourseSequenceStep::getRecommendedGradeLevel)
                .filter(gl -> gl != null)
                .distinct()
                .collect(Collectors.toList());

            if (!gradeLevels.isEmpty()) {
                int minGrade = gradeLevels.stream().min(Integer::compareTo).orElse(9);
                int maxGrade = gradeLevels.stream().max(Integer::compareTo).orElse(12);
                sequence.setTotalYears(maxGrade - minGrade + 1);
            } else {
                sequence.setTotalYears(steps.size()); // Fallback
            }
        }

        sequenceRepository.save(sequence);
        log.debug("Recalculated totals for sequence {}: {} years, {} credits",
            sequence.getId(), sequence.getTotalYears(), sequence.getTotalCredits());
    }

    /**
     * Get statistics about sequences
     *
     * @return Statistics DTO
     */
    public SequenceStatistics getStatistics() {
        long totalSequences = sequenceRepository.count();
        long activeSequences = sequenceRepository.countByActiveTrue();
        long totalSteps = stepRepository.count();

        long traditionalCount = sequenceRepository.countBySequenceTypeAndActiveTrue(CourseSequence.SequenceType.TRADITIONAL);
        long honorsCount = sequenceRepository.countBySequenceTypeAndActiveTrue(CourseSequence.SequenceType.HONORS);
        long apCount = sequenceRepository.countBySequenceTypeAndActiveTrue(CourseSequence.SequenceType.AP);

        return SequenceStatistics.builder()
            .totalSequences(totalSequences)
            .activeSequences(activeSequences)
            .totalSteps(totalSteps)
            .traditionalSequences(traditionalCount)
            .honorsSequences(honorsCount)
            .apSequences(apCount)
            .build();
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SequenceStatistics {
        private long totalSequences;
        private long activeSequences;
        private long totalSteps;
        private long traditionalSequences;
        private long honorsSequences;
        private long apSequences;

        @Override
        public String toString() {
            return String.format(
                "Sequence Statistics: Total=%d, Active=%d, Steps=%d, Traditional=%d, Honors=%d, AP=%d",
                totalSequences, activeSequences, totalSteps,
                traditionalSequences, honorsSequences, apSequences);
        }
    }
}
