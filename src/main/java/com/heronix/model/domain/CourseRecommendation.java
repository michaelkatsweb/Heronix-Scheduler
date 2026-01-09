package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Course Recommendation Entity
 *
 * Represents an AI-generated or manual course recommendation for a student.
 *
 * Recommendations are based on:
 * - Student GPA and academic performance
 * - Completed courses and prerequisites
 * - Current course sequence/pathway
 * - Subject area relationships
 * - Graduation requirements
 * - Student interests and goals
 *
 * Location: src/main/java/com/eduscheduler/model/domain/CourseRecommendation.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 3 - December 6, 2025 - Intelligent Course Recommendations
 */
@Entity
@Table(name = "course_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student this recommendation is for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Recommended course
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Course sequence this recommendation aligns with (optional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_sequence_id")
    private CourseSequence courseSequence;

    /**
     * Recommended for which school year
     */
    @Column(name = "recommended_school_year", length = 20)
    private String recommendedSchoolYear; // e.g., "2025-2026"

    /**
     * Recommended grade level (9, 10, 11, 12)
     */
    @Column(name = "recommended_grade_level")
    private Integer recommendedGradeLevel;

    /**
     * Priority level (1=highest, 10=lowest)
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    /**
     * Confidence score (0.0-1.0)
     * How confident the system is in this recommendation
     */
    @Column(name = "confidence_score")
    @Builder.Default
    private Double confidenceScore = 0.5;

    /**
     * Recommendation type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false)
    @Builder.Default
    private RecommendationType recommendationType = RecommendationType.AI_GENERATED;

    /**
     * Recommendation status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RecommendationStatus status = RecommendationStatus.PENDING;

    /**
     * Why this course is recommended
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * Prerequisites met/not met
     */
    @Column(name = "prerequisites_met")
    @Builder.Default
    private Boolean prerequisitesMet = true;

    /**
     * GPA requirement met/not met
     */
    @Column(name = "gpa_requirement_met")
    @Builder.Default
    private Boolean gpaRequirementMet = true;

    /**
     * Schedule conflicts detected
     */
    @Column(name = "has_schedule_conflict")
    @Builder.Default
    private Boolean hasScheduleConflict = false;

    /**
     * Alternative courses if this one is not available
     * Format: Comma-separated course codes
     */
    @Column(name = "alternative_courses", length = 500)
    private String alternativeCourses;

    /**
     * Counselor who made/approved recommendation (if manual)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id")
    private User counselor;

    /**
     * Counselor notes
     */
    @Column(columnDefinition = "TEXT")
    private String counselorNotes;

    /**
     * Student accepted/rejected/pending
     */
    @Column(name = "student_accepted")
    private Boolean studentAccepted;

    /**
     * Parent accepted/rejected/pending
     */
    @Column(name = "parent_accepted")
    private Boolean parentAccepted;

    /**
     * When student responded
     */
    @Column(name = "student_response_date")
    private LocalDateTime studentResponseDate;

    /**
     * When parent responded
     */
    @Column(name = "parent_response_date")
    private LocalDateTime parentResponseDate;

    /**
     * Recommendation created timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Recommendation last updated
     */
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Active recommendation
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum RecommendationType {
        AI_GENERATED,           // Generated by AI algorithm
        COUNSELOR_MANUAL,       // Manually created by counselor
        SEQUENCE_BASED,         // Based on course sequence pathway
        PREREQUISITE_BASED,     // Based on completed prerequisites
        GRADUATION_REQUIREMENT, // Needed for graduation
        ENRICHMENT,            // Enrichment/elective suggestion
        REMEDIAL               // Remedial/intervention suggestion
    }

    public enum RecommendationStatus {
        PENDING,      // Not yet reviewed
        ACCEPTED,     // Student/parent accepted
        REJECTED,     // Student/parent rejected
        ENROLLED,     // Student enrolled in course
        CANCELLED,    // Recommendation cancelled
        EXPIRED       // No longer applicable
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if recommendation is pending review
     */
    public boolean isPending() {
        return RecommendationStatus.PENDING.equals(status);
    }

    /**
     * Check if recommendation was accepted
     */
    public boolean isAccepted() {
        return RecommendationStatus.ACCEPTED.equals(status);
    }

    /**
     * Check if recommendation was rejected
     */
    public boolean isRejected() {
        return RecommendationStatus.REJECTED.equals(status);
    }

    /**
     * Check if student enrolled in recommended course
     */
    public boolean isEnrolled() {
        return RecommendationStatus.ENROLLED.equals(status);
    }

    /**
     * Check if recommendation is AI-generated
     */
    public boolean isAIGenerated() {
        return RecommendationType.AI_GENERATED.equals(recommendationType);
    }

    /**
     * Check if recommendation is manual (counselor-created)
     */
    public boolean isManual() {
        return RecommendationType.COUNSELOR_MANUAL.equals(recommendationType);
    }

    /**
     * Check if recommendation is high priority
     */
    public boolean isHighPriority() {
        return priority != null && priority <= 3;
    }

    /**
     * Check if recommendation has high confidence
     */
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore >= 0.7;
    }

    /**
     * Check if all requirements are met
     */
    public boolean meetsAllRequirements() {
        return Boolean.TRUE.equals(prerequisitesMet) &&
               Boolean.TRUE.equals(gpaRequirementMet) &&
               !Boolean.TRUE.equals(hasScheduleConflict);
    }

    /**
     * Check if student/parent approval is needed
     */
    public boolean needsApproval() {
        return isPending() && (studentAccepted == null || parentAccepted == null);
    }

    /**
     * Check if both student and parent accepted
     */
    public boolean fullyApproved() {
        return Boolean.TRUE.equals(studentAccepted) && Boolean.TRUE.equals(parentAccepted);
    }

    /**
     * Mark as accepted by student
     */
    public void acceptByStudent() {
        this.studentAccepted = true;
        this.studentResponseDate = LocalDateTime.now();
        updateStatus();
    }

    /**
     * Mark as accepted by parent
     */
    public void acceptByParent() {
        this.parentAccepted = true;
        this.parentResponseDate = LocalDateTime.now();
        updateStatus();
    }

    /**
     * Mark as rejected by student
     */
    public void rejectByStudent() {
        this.studentAccepted = false;
        this.studentResponseDate = LocalDateTime.now();
        this.status = RecommendationStatus.REJECTED;
    }

    /**
     * Mark as rejected by parent
     */
    public void rejectByParent() {
        this.parentAccepted = false;
        this.parentResponseDate = LocalDateTime.now();
        this.status = RecommendationStatus.REJECTED;
    }

    /**
     * Update status based on approvals
     */
    private void updateStatus() {
        if (fullyApproved()) {
            this.status = RecommendationStatus.ACCEPTED;
        }
    }

    /**
     * Get summary string
     */
    public String getSummary() {
        return String.format("%s for %s (Grade %d, Priority %d, Confidence %.0f%%)",
            course != null ? course.getCourseCode() : "Unknown",
            student != null ? student.getFullName() : "Unknown",
            recommendedGradeLevel != null ? recommendedGradeLevel : 0,
            priority,
            confidenceScore != null ? confidenceScore * 100 : 0);
    }

    /**
     * Get list of alternative course codes
     */
    public String[] getAlternativeCourseList() {
        if (alternativeCourses == null || alternativeCourses.trim().isEmpty()) {
            return new String[0];
        }
        return alternativeCourses.split(",");
    }

    @Override
    public String toString() {
        return String.format("CourseRecommendation[id=%d, student=%s, course=%s, type=%s, status=%s, priority=%d]",
            id,
            student != null ? student.getFullName() : "null",
            course != null ? course.getCourseCode() : "null",
            recommendationType,
            status,
            priority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseRecommendation)) return false;
        CourseRecommendation that = (CourseRecommendation) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
