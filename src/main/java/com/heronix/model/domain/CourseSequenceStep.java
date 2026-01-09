package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Course Sequence Step Entity
 *
 * Represents one step in a course sequence pathway.
 *
 * Example:
 * Step 1: Algebra I (Grade 9, Required)
 * Step 2: Geometry (Grade 10, Required)
 * Step 3: Algebra II (Grade 11, Required)
 * Step 4: Precalculus (Grade 12, Optional - could take Statistics instead)
 *
 * Location: src/main/java/com/eduscheduler/model/domain/CourseSequenceStep.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 6, 2025 - Course Sequencing
 */
@Entity
@Table(name = "course_sequence_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSequenceStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent sequence
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_sequence_id", nullable = false)
    private CourseSequence courseSequence;

    /**
     * Course at this step
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Order in sequence (1, 2, 3, ...)
     */
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    /**
     * Recommended grade level for this step (9, 10, 11, 12)
     */
    @Column(name = "recommended_grade_level")
    private Integer recommendedGradeLevel;

    /**
     * Is this step required or optional in the sequence?
     */
    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;

    /**
     * Alternative courses that can substitute for this step
     * Format: Comma-separated course codes
     * Example: "MATH301,MATH302" (either Algebra II or Algebra II Honors)
     */
    @Column(name = "alternative_courses", length = 500)
    private String alternativeCourses;

    /**
     * Notes about this step
     * Example: "Students may skip if proficient", "Summer course option available"
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Minimum grade required from previous step to continue
     * Example: "C+" - student must get C+ or better in previous course
     */
    @Column(name = "min_grade_from_previous", length = 5)
    private String minGradeFromPrevious;

    /**
     * Credits earned for completing this step
     * Typically 1.0 or 0.5
     */
    @Column(name = "credits")
    private Double credits = 1.0;

    /**
     * Is this a terminal course (ends the sequence if chosen)?
     * Example: Statistics could be terminal - student doesn't need to continue to Calculus
     */
    @Column(name = "is_terminal")
    @Builder.Default
    private Boolean isTerminal = false;

    /**
     * Prerequisite step IDs that must be completed before this step
     * Stored as JSON array of step IDs: "[1, 2]" means steps 1 and 2 must be completed
     *
     * Example Use Cases:
     * - Step 4 (AP Calculus AB) requires Step 3 (Precalculus) to be completed
     * - Step 5 (AP Physics C) requires both Step 2 (Physics I) AND Step 3 (Calculus I)
     * - Step 3 (Chemistry II) requires Step 2 (Chemistry I) with min grade "C+"
     *
     * Format: JSON array stored as VARCHAR
     * Example: "[3]" - requires step 3
     * Example: "[2,3]" - requires both steps 2 and 3
     *
     * If null or empty, no specific prerequisites required (follows sequence order only)
     */
    @Column(name = "prerequisite_step_ids", length = 500)
    private String prerequisiteStepIds;

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if this is the first step in sequence
     */
    public boolean isFirstStep() {
        return stepOrder != null && stepOrder == 1;
    }

    /**
     * Check if this step is required
     */
    public boolean isRequired() {
        return Boolean.TRUE.equals(isRequired);
    }

    /**
     * Check if this step is optional
     */
    public boolean isOptional() {
        return !isRequired();
    }

    /**
     * Check if this step has alternatives
     */
    public boolean hasAlternatives() {
        return alternativeCourses != null && !alternativeCourses.trim().isEmpty();
    }

    /**
     * Get list of alternative course codes
     */
    public String[] getAlternativeCourseList() {
        if (!hasAlternatives()) {
            return new String[0];
        }
        return alternativeCourses.split(",");
    }

    /**
     * Check if this step has prerequisites
     */
    public boolean hasPrerequisites() {
        return prerequisiteStepIds != null && !prerequisiteStepIds.trim().isEmpty()
            && !prerequisiteStepIds.trim().equals("[]");
    }

    /**
     * Get list of prerequisite step IDs
     * Parses JSON array format: "[1,2,3]" → [1, 2, 3]
     *
     * @return Array of prerequisite step IDs, empty array if none
     */
    public Long[] getPrerequisiteStepIdList() {
        if (!hasPrerequisites()) {
            return new Long[0];
        }

        try {
            // Remove brackets and whitespace, split by comma
            String clean = prerequisiteStepIds.trim()
                .replaceAll("[\\[\\]\\s]", "");

            if (clean.isEmpty()) {
                return new Long[0];
            }

            String[] parts = clean.split(",");
            Long[] ids = new Long[parts.length];

            for (int i = 0; i < parts.length; i++) {
                ids[i] = Long.parseLong(parts[i].trim());
            }

            return ids;
        } catch (NumberFormatException e) {
            // Invalid format, return empty array
            return new Long[0];
        }
    }

    /**
     * Set prerequisite step IDs from array
     * Converts array to JSON format: [1, 2, 3] → "[1,2,3]"
     *
     * @param stepIds Array of prerequisite step IDs
     */
    public void setPrerequisiteStepIdList(Long[] stepIds) {
        if (stepIds == null || stepIds.length == 0) {
            this.prerequisiteStepIds = null;
            return;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stepIds.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(stepIds[i]);
        }
        sb.append("]");

        this.prerequisiteStepIds = sb.toString();
    }

    /**
     * Check if a specific step is a prerequisite for this step
     *
     * @param stepId Step ID to check
     * @return true if the given step is a prerequisite
     */
    public boolean isPrerequisiteStep(Long stepId) {
        if (!hasPrerequisites() || stepId == null) {
            return false;
        }

        Long[] prereqs = getPrerequisiteStepIdList();
        for (Long prereqId : prereqs) {
            if (prereqId.equals(stepId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get display string for step
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Step ").append(stepOrder).append(": ");

        if (course != null) {
            sb.append(course.getCourseCode()).append(" - ").append(course.getCourseName());
        }

        if (recommendedGradeLevel != null) {
            sb.append(" (Grade ").append(recommendedGradeLevel).append(")");
        }

        if (!isRequired()) {
            sb.append(" [Optional]");
        }

        if (isTerminal) {
            sb.append(" [Terminal]");
        }

        return sb.toString();
    }

    /**
     * Get summary for this step
     */
    public String getSummary() {
        return String.format("%s (Step %d, Grade %d, %s)",
            course != null ? course.getCourseCode() : "Unknown",
            stepOrder,
            recommendedGradeLevel != null ? recommendedGradeLevel : 0,
            isRequired() ? "Required" : "Optional");
    }

    @Override
    public String toString() {
        return String.format("Step %d: %s (Grade %d%s)",
            stepOrder,
            course != null ? course.getCourseCode() : "null",
            recommendedGradeLevel != null ? recommendedGradeLevel : 0,
            isRequired() ? ", Required" : ", Optional");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseSequenceStep)) return false;
        CourseSequenceStep that = (CourseSequenceStep) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
