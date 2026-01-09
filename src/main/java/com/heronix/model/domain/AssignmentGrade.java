package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Assignment Grade Entity
 *
 * Represents a student's score on a specific assignment.
 * Links students to assignments with their individual scores.
 *
 * Features:
 * - Score tracking (points earned)
 * - Late submission handling
 * - Excused/missing status
 * - Teacher comments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Entity
@Table(name = "assignment_grades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "assignment_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student who received this grade
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Assignment this grade is for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /**
     * Points earned (null = not graded yet)
     */
    @Column(name = "score")
    private Double score;

    /**
     * Status of this grade
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private GradeStatus status = GradeStatus.NOT_SUBMITTED;

    /**
     * Date submitted (for late penalty calculation)
     */
    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    /**
     * Late penalty applied (percentage)
     */
    @Column(name = "late_penalty")
    @Builder.Default
    private Double latePenalty = 0.0;

    /**
     * Is this excused (doesn't count against student)
     */
    @Column(name = "excused")
    @Builder.Default
    private Boolean excused = false;

    /**
     * Teacher comments/feedback
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /**
     * Date graded
     */
    @Column(name = "graded_date")
    private LocalDate gradedDate;

    /**
     * Teacher who graded (for audit trail)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private Teacher gradedBy;

    /**
     * Created timestamp
     */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Updated timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Grade status options
     */
    public enum GradeStatus {
        NOT_SUBMITTED("Not Submitted", "#9E9E9E"),
        SUBMITTED("Submitted", "#2196F3"),
        GRADED("Graded", "#4CAF50"),
        LATE("Late", "#FF9800"),
        MISSING("Missing", "#F44336"),
        EXCUSED("Excused", "#9C27B0"),
        INCOMPLETE("Incomplete", "#607D8B");

        private final String displayName;
        private final String color;

        GradeStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get percentage score (score / max points * 100)
     */
    public Double getPercentage() {
        if (score == null || assignment == null || assignment.getMaxPoints() == null) {
            return null;
        }
        if (assignment.getMaxPoints() == 0) return 0.0;
        return (score / assignment.getMaxPoints()) * 100;
    }

    /**
     * Get adjusted score after late penalty
     */
    public Double getAdjustedScore() {
        if (score == null) return null;
        if (latePenalty == null || latePenalty == 0) return score;

        double penaltyMultiplier = 1.0 - (latePenalty / 100.0);
        return score * penaltyMultiplier;
    }

    /**
     * Get adjusted percentage after late penalty
     */
    public Double getAdjustedPercentage() {
        Double adjusted = getAdjustedScore();
        if (adjusted == null || assignment == null || assignment.getMaxPoints() == null) {
            return null;
        }
        if (assignment.getMaxPoints() == 0) return 0.0;
        return (adjusted / assignment.getMaxPoints()) * 100;
    }

    /**
     * Get letter grade for this assignment
     */
    public String getLetterGrade() {
        Double pct = getAdjustedPercentage();
        if (pct == null) return "-";
        if (Boolean.TRUE.equals(excused)) return "EX";

        if (pct >= 97) return "A+";
        if (pct >= 93) return "A";
        if (pct >= 90) return "A-";
        if (pct >= 87) return "B+";
        if (pct >= 83) return "B";
        if (pct >= 80) return "B-";
        if (pct >= 77) return "C+";
        if (pct >= 73) return "C";
        if (pct >= 70) return "C-";
        if (pct >= 67) return "D+";
        if (pct >= 63) return "D";
        if (pct >= 60) return "D-";
        return "F";
    }

    /**
     * Check if this grade should be included in calculations
     */
    public boolean countsInGrade() {
        if (Boolean.TRUE.equals(excused)) return false;
        if (assignment != null && !Boolean.TRUE.equals(assignment.getCountInGrade())) return false;
        return score != null;
    }

    /**
     * Mark as graded with current date
     */
    public void markGraded(Double score, Teacher teacher) {
        this.score = score;
        this.status = GradeStatus.GRADED;
        this.gradedDate = LocalDate.now();
        this.gradedBy = teacher;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as excused
     */
    public void markExcused(String reason) {
        this.excused = true;
        this.status = GradeStatus.EXCUSED;
        this.comments = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as missing
     */
    public void markMissing() {
        this.status = GradeStatus.MISSING;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
