package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Planned Course Entity
 *
 * Represents a single course planned for a specific year/semester in a student's academic plan.
 *
 * Location: src/main/java/com/eduscheduler/model/domain/PlannedCourse.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - December 6, 2025 - Four-Year Academic Planning
 */
@Entity
@Table(name = "planned_courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Academic plan this course belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_plan_id", nullable = false)
    private AcademicPlan academicPlan;

    /**
     * The course being planned
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * School year (e.g., "2025-2026")
     */
    @Column(name = "school_year", nullable = false, length = 20)
    private String schoolYear;

    /**
     * Grade level (9, 10, 11, 12)
     */
    @Column(name = "grade_level")
    private Integer gradeLevel;

    /**
     * Semester (1=Fall, 2=Spring, 0=Full Year)
     */
    @Column(name = "semester")
    @Builder.Default
    private Integer semester = 0;

    /**
     * Credits for this course
     */
    @Column(name = "credits")
    @Builder.Default
    private Double credits = 1.0;

    /**
     * Is this course required for graduation?
     */
    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    /**
     * Is this course completed?
     */
    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    /**
     * Grade earned (if completed)
     */
    @Column(name = "grade_earned", length = 5)
    private String gradeEarned;

    /**
     * Status of this planned course
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.PLANNED;

    /**
     * Related course recommendation (if any)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private CourseRecommendation recommendation;

    /**
     * Related course sequence step (if following a pathway)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_step_id")
    private CourseSequenceStep sequenceStep;

    /**
     * Alternative courses if this one is unavailable
     * Format: Comma-separated course codes
     */
    @Column(name = "alternatives", length = 500)
    private String alternatives;

    /**
     * Notes about this planned course
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Prerequisites met for this course
     */
    @Column(name = "prerequisites_met")
    @Builder.Default
    private Boolean prerequisitesMet = true;

    /**
     * Is there a schedule conflict?
     */
    @Column(name = "has_conflict")
    @Builder.Default
    private Boolean hasConflict = false;

    /**
     * Conflict details (if any)
     */
    @Column(name = "conflict_details", length = 500)
    private String conflictDetails;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum CourseStatus {
        PLANNED,        // Future course
        ENROLLED,       // Currently enrolled
        IN_PROGRESS,    // Currently taking
        COMPLETED,      // Finished with grade
        DROPPED,        // Dropped/withdrawn
        FAILED,         // Failed, needs retake
        SUBSTITUTED     // Replaced with alternative
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if this is a full-year course
     */
    public boolean isFullYear() {
        return semester != null && semester == 0;
    }

    /**
     * Check if this is a fall semester course
     */
    public boolean isFallSemester() {
        return semester != null && semester == 1;
    }

    /**
     * Check if this is a spring semester course
     */
    public boolean isSpringSemester() {
        return semester != null && semester == 2;
    }

    /**
     * Check if course is completed
     */
    public boolean isCompleted() {
        return Boolean.TRUE.equals(isCompleted) ||
               CourseStatus.COMPLETED.equals(status);
    }

    /**
     * Check if course is in progress
     */
    public boolean isInProgress() {
        return CourseStatus.IN_PROGRESS.equals(status) ||
               CourseStatus.ENROLLED.equals(status);
    }

    /**
     * Check if course is planned (future)
     */
    public boolean isPlanned() {
        return CourseStatus.PLANNED.equals(status);
    }

    /**
     * Check if prerequisites are met
     */
    public boolean hasMetPrerequisites() {
        return Boolean.TRUE.equals(prerequisitesMet);
    }

    /**
     * Check if there's a conflict
     */
    public boolean hasConflict() {
        return Boolean.TRUE.equals(hasConflict);
    }

    /**
     * Mark as completed
     */
    public void markCompleted(String grade) {
        this.isCompleted = true;
        this.gradeEarned = grade;
        this.status = CourseStatus.COMPLETED;
    }

    /**
     * Mark as in progress
     */
    public void markInProgress() {
        this.status = CourseStatus.IN_PROGRESS;
    }

    /**
     * Mark as dropped
     */
    public void markDropped() {
        this.status = CourseStatus.DROPPED;
    }

    /**
     * Get semester name
     */
    public String getSemesterName() {
        if (semester == null) return "Unknown";
        return switch (semester) {
            case 0 -> "Full Year";
            case 1 -> "Fall";
            case 2 -> "Spring";
            default -> "Semester " + semester;
        };
    }

    /**
     * Get display string
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (course != null) {
            sb.append(course.getCourseCode())
              .append(" - ")
              .append(course.getCourseName());
        }

        sb.append(" (")
          .append(schoolYear)
          .append(", ")
          .append(getSemesterName());

        if (gradeLevel != null) {
            sb.append(", Grade ").append(gradeLevel);
        }

        sb.append(")");

        if (isCompleted()) {
            sb.append(" ✓");
            if (gradeEarned != null) {
                sb.append(" - Grade: ").append(gradeEarned);
            }
        }

        return sb.toString();
    }

    /**
     * Get alternative course list
     */
    public String[] getAlternativesList() {
        if (alternatives == null || alternatives.trim().isEmpty()) {
            return new String[0];
        }
        return alternatives.split(",");
    }

    @Override
    public String toString() {
        return String.format("PlannedCourse[id=%d, course=%s, year=%s, semester=%s, status=%s]",
            id,
            course != null ? course.getCourseCode() : "null",
            schoolYear,
            getSemesterName(),
            status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlannedCourse)) return false;
        PlannedCourse that = (PlannedCourse) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
