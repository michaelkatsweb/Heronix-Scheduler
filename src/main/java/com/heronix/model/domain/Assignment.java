package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Assignment Entity
 *
 * Represents an individual assignment within a course.
 * Examples: "Chapter 5 Quiz", "Midterm Exam", "Science Fair Project"
 *
 * Features:
 * - Belongs to a grading category
 * - Point-based or percentage-based scoring
 * - Due dates with late penalty support
 * - Standards alignment (optional)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Entity
@Table(name = "assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Course this assignment belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Grading category (Tests, Homework, etc.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private GradingCategory category;

    /**
     * Assignment title
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Assignment description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Maximum points possible
     */
    @Column(name = "max_points", nullable = false)
    @Builder.Default
    private Double maxPoints = 100.0;

    /**
     * Date assigned
     */
    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    /**
     * Due date
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Academic term (e.g., "Fall 2024", "Q1 2024-25")
     */
    @Column(name = "term", length = 50)
    private String term;

    /**
     * Whether this is an extra credit assignment
     */
    @Column(name = "is_extra_credit")
    @Builder.Default
    private Boolean isExtraCredit = false;

    /**
     * Whether to count this in the final grade
     */
    @Column(name = "count_in_grade")
    @Builder.Default
    private Boolean countInGrade = true;

    /**
     * Late penalty per day (percentage, e.g., 10 = 10% off per day)
     */
    @Column(name = "late_penalty_per_day")
    @Builder.Default
    private Double latePenaltyPerDay = 0.0;

    /**
     * Maximum late penalty (percentage, e.g., 30 = max 30% off)
     */
    @Column(name = "max_late_penalty")
    @Builder.Default
    private Double maxLatePenalty = 0.0;

    /**
     * Whether assignment is published (visible to students)
     */
    @Column(name = "published")
    @Builder.Default
    private Boolean published = false;

    /**
     * Display order within category
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Created timestamp
     */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Last updated timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Student grades for this assignment
     */
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AssignmentGrade> grades = new ArrayList<>();

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if assignment is past due
     */
    public boolean isPastDue() {
        return dueDate != null && LocalDate.now().isAfter(dueDate);
    }

    /**
     * Get days until due (negative if past due)
     */
    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /**
     * Calculate late penalty for a submission date
     */
    public double calculateLatePenalty(LocalDate submissionDate) {
        if (dueDate == null || submissionDate == null) return 0.0;
        if (!submissionDate.isAfter(dueDate)) return 0.0;

        long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, submissionDate);
        double penalty = daysLate * latePenaltyPerDay;

        if (maxLatePenalty > 0) {
            penalty = Math.min(penalty, maxLatePenalty);
        }

        return penalty;
    }

    /**
     * Get number of students who have submitted
     */
    public int getSubmissionCount() {
        return grades != null ? (int) grades.stream().filter(g -> g.getScore() != null).count() : 0;
    }

    /**
     * Get class average score
     */
    public Double getClassAverage() {
        if (grades == null || grades.isEmpty()) return null;

        List<Double> scores = grades.stream()
            .filter(g -> g.getScore() != null)
            .map(AssignmentGrade::getScore)
            .toList();

        if (scores.isEmpty()) return null;

        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * Get class average as percentage
     */
    public Double getClassAveragePercent() {
        Double avg = getClassAverage();
        if (avg == null || maxPoints == null || maxPoints == 0) return null;
        return (avg / maxPoints) * 100;
    }

    /**
     * Get status string for display
     */
    public String getStatus() {
        if (!Boolean.TRUE.equals(published)) return "Draft";
        if (isPastDue()) return "Past Due";
        if (dueDate != null && getDaysUntilDue() <= 3) return "Due Soon";
        return "Active";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
