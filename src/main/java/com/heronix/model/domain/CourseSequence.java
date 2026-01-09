package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Course Sequence Entity
 *
 * Defines typical progression pathways through a subject area.
 *
 * Example:
 * Traditional Mathematics Pathway:
 * 1. Algebra I (9th grade)
 * 2. Geometry (10th grade)
 * 3. Algebra II (11th grade)
 * 4. Precalculus (12th grade)
 *
 * Location: src/main/java/com/eduscheduler/model/domain/CourseSequence.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 6, 2025 - Course Sequencing
 */
@Entity
@Table(name = "course_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sequence name (e.g., "Traditional Math Pathway", "AP Science Track")
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Unique sequence code (e.g., "MATH-TRAD", "SCI-AP")
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Subject area this sequence belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_area_id", nullable = false)
    private SubjectArea subjectArea;

    /**
     * Sequence type/difficulty level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sequence_type", length = 50)
    private SequenceType sequenceType;

    /**
     * Detailed description
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Target student profile (e.g., "College-bound students")
     */
    @Column(name = "target_profile", length = 200)
    private String targetProfile;

    /**
     * Minimum GPA recommended for this sequence
     */
    @Column(name = "min_gpa_recommended")
    private Double minGPARecommended;

    /**
     * Total years this sequence typically takes
     * Example: 4 (for 4-year high school sequence)
     */
    @Column(name = "total_years")
    private Integer totalYears;

    /**
     * Total credits earned upon completion
     */
    @Column(name = "total_credits")
    private Double totalCredits;

    /**
     * Steps in this sequence (ordered)
     */
    @OneToMany(mappedBy = "courseSequence", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<CourseSequenceStep> steps = new ArrayList<>();

    /**
     * Is this sequence currently active/offered?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Recommended for specific grade levels
     * Example: "9-12" for high school
     */
    @Column(name = "grade_level_range", length = 20)
    private String gradeLevelRange;

    /**
     * Is this sequence appropriate for students with IEPs?
     * IEP-friendly sequences typically have modified pacing or additional support
     */
    @Column(name = "iep_friendly")
    @Builder.Default
    private Boolean iepFriendly = false;

    /**
     * Prerequisite skills or knowledge required for this sequence
     * Examples: "Algebra I proficiency", "Reading at 9th grade level", "Basic computer literacy"
     * Can be used to filter sequences by student skill level
     */
    @Column(name = "prerequisite_skills", columnDefinition = "TEXT")
    private String prerequisiteSkills;

    /**
     * Graduation requirement category this sequence fulfills
     * Examples: "English", "Mathematics", "Science", "Social Studies", "Physical Education",
     * "Fine Arts", "World Language", "Electives"
     * Used to link sequences to graduation requirements
     */
    @Column(name = "graduation_requirement_category", length = 100)
    private String graduationRequirementCategory;

    /**
     * Estimated total cost for completing this sequence
     * Includes all fees, materials, AP exams, etc. across all steps
     * Examples: 0.00 (free), 150.00 (AP exam fees), 500.00 (lab fees + materials)
     * Used for financial planning and scholarship allocation
     */
    @Column(name = "estimated_total_cost")
    private Double estimatedTotalCost = 0.0;

    /**
     * Description of costs associated with this sequence
     * Examples: "Includes 2 AP exam fees ($94 each)", "Lab fees $50/year", "Art supplies $100"
     * Provides transparency for parents and students
     */
    @Column(name = "cost_description", columnDefinition = "TEXT")
    private String costDescription;

    /**
     * When this sequence was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this sequence was last modified
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Types of course sequences
     */
    public enum SequenceType {
        /**
         * Standard progression for most students
         */
        TRADITIONAL("Standard progression"),

        /**
         * Accelerated/honors track for high-achieving students
         */
        HONORS("Accelerated/honors track"),

        /**
         * Advanced Placement track for college credit
         */
        AP("Advanced Placement track"),

        /**
         * International Baccalaureate track
         */
        IB("International Baccalaureate track"),

        /**
         * Career/technical education track
         */
        TECHNICAL("Career/technical track"),

        /**
         * Foundation/support track for students needing extra help
         */
        REMEDIAL("Foundation/support track"),

        /**
         * Dual enrollment (high school + college)
         */
        DUAL_ENROLLMENT("Dual enrollment track");

        private final String description;

        SequenceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get total number of courses in sequence
     */
    public int getCourseCount() {
        return steps != null ? steps.size() : 0;
    }

    /**
     * Get courses in order
     */
    public List<Course> getCoursesInOrder() {
        List<Course> courses = new ArrayList<>();
        if (steps != null) {
            for (CourseSequenceStep step : steps) {
                courses.add(step.getCourse());
            }
        }
        return courses;
    }

    /**
     * Get required courses only (exclude optional steps)
     */
    public List<Course> getRequiredCourses() {
        if (steps == null) return new ArrayList<>();

        return steps.stream()
            .filter(step -> Boolean.TRUE.equals(step.getIsRequired()))
            .map(CourseSequenceStep::getCourse)
            .toList();
    }

    /**
     * Get optional courses only
     */
    public List<Course> getOptionalCourses() {
        if (steps == null) return new ArrayList<>();

        return steps.stream()
            .filter(step -> !Boolean.TRUE.equals(step.getIsRequired()))
            .map(CourseSequenceStep::getCourse)
            .toList();
    }

    /**
     * Check if student has completed this sequence
     *
     * @param completedCourses List of courses student has completed
     * @return true if all required courses are completed
     */
    public boolean isCompletedByStudent(List<Course> completedCourses) {
        List<Course> required = getRequiredCourses();
        return completedCourses.containsAll(required);
    }

    /**
     * Get next recommended course for student
     *
     * @param completedCourses List of courses student has completed
     * @return Next course in sequence, or null if completed
     */
    public Course getNextCourse(List<Course> completedCourses) {
        if (steps == null) return null;

        for (CourseSequenceStep step : steps) {
            if (!completedCourses.contains(step.getCourse())) {
                return step.getCourse();
            }
        }
        return null; // Sequence completed
    }

    /**
     * Get student's progress through sequence (percentage)
     *
     * @param completedCourses List of courses student has completed
     * @return Progress percentage (0-100)
     */
    public double getProgressPercentage(List<Course> completedCourses) {
        if (steps == null || steps.isEmpty()) {
            return 0.0;
        }

        long completedCount = steps.stream()
            .filter(step -> completedCourses.contains(step.getCourse()))
            .count();

        return (double) completedCount / steps.size() * 100.0;
    }

    /**
     * Get courses completed by student in this sequence
     *
     * @param completedCourses List of courses student has completed
     * @return List of courses from this sequence that student completed
     */
    public List<Course> getCompletedCoursesInSequence(List<Course> completedCourses) {
        List<Course> sequenceCourses = getCoursesInOrder();
        return sequenceCourses.stream()
            .filter(completedCourses::contains)
            .toList();
    }

    /**
     * Get courses remaining in sequence for student
     *
     * @param completedCourses List of courses student has completed
     * @return List of courses from this sequence not yet completed
     */
    public List<Course> getRemainingCourses(List<Course> completedCourses) {
        List<Course> sequenceCourses = getCoursesInOrder();
        return sequenceCourses.stream()
            .filter(course -> !completedCourses.contains(course))
            .toList();
    }

    /**
     * Get step by order number
     *
     * @param stepOrder Step order number (1, 2, 3, ...)
     * @return Course sequence step, or null if not found
     */
    public CourseSequenceStep getStepByOrder(int stepOrder) {
        if (steps == null) return null;

        return steps.stream()
            .filter(step -> step.getStepOrder() == stepOrder)
            .findFirst()
            .orElse(null);
    }

    /**
     * Add step to sequence
     *
     * @param course Course to add
     * @param order Step order
     * @param recommendedGrade Recommended grade level
     * @param required Is this step required?
     */
    public void addStep(Course course, int order, Integer recommendedGrade, boolean required) {
        if (steps == null) {
            steps = new ArrayList<>();
        }

        CourseSequenceStep step = CourseSequenceStep.builder()
            .courseSequence(this)
            .course(course)
            .stepOrder(order)
            .recommendedGradeLevel(recommendedGrade)
            .isRequired(required)
            .build();

        steps.add(step);
    }

    /**
     * Check if sequence is honors/advanced level
     */
    public boolean isAdvancedSequence() {
        return sequenceType == SequenceType.HONORS ||
               sequenceType == SequenceType.AP ||
               sequenceType == SequenceType.IB ||
               sequenceType == SequenceType.DUAL_ENROLLMENT;
    }

    /**
     * Check if this sequence has associated costs
     */
    public boolean hasCosts() {
        return estimatedTotalCost != null && estimatedTotalCost > 0.0;
    }

    /**
     * Get formatted cost string for display
     * @return Cost formatted as currency (e.g., "$150.00") or "Free"
     */
    public String getFormattedCost() {
        if (!hasCosts()) {
            return "Free";
        }
        return String.format("$%.2f", estimatedTotalCost);
    }

    /**
     * Check if sequence is free (no costs)
     */
    public boolean isFree() {
        return !hasCosts();
    }

    /**
     * Get cost category for grouping/filtering
     * @return "Free", "Low Cost" (<$100), "Moderate Cost" ($100-$300), "High Cost" (>$300)
     */
    public String getCostCategory() {
        if (isFree()) {
            return "Free";
        }
        if (estimatedTotalCost < 100.0) {
            return "Low Cost";
        }
        if (estimatedTotalCost < 300.0) {
            return "Moderate Cost";
        }
        return "High Cost";
    }

    /**
     * Get display string for UI
     */
    public String getDisplayString() {
        return String.format("%s (%s)", name, sequenceType);
    }

    /**
     * Get summary string
     */
    public String getSummary() {
        return String.format("%s - %d courses, %s",
            name,
            getCourseCount(),
            sequenceType != null ? sequenceType.getDescription() : "Standard");
    }

    @Override
    public String toString() {
        return String.format("CourseSequence{id=%d, name='%s', type=%s, courses=%d, subject=%s}",
            id, name, sequenceType, getCourseCount(),
            subjectArea != null ? subjectArea.getCode() : "null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseSequence)) return false;
        CourseSequence that = (CourseSequence) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
