package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Student Sequence Entity
 *
 * Links students to course sequences they're following and tracks their progress.
 * Enables academic planning, progress monitoring, and sequence completion reporting.
 *
 * Location: src/main/java/com/eduscheduler/model/domain/StudentSequence.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 10, 2025 - Student Sequence Tracking (Audit Item #17)
 */
@Entity
@Table(name = "student_sequences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "course_sequence_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student following this sequence
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Course sequence being followed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_sequence_id", nullable = false)
    private CourseSequence courseSequence;

    /**
     * When student was assigned to this sequence
     */
    @Column(name = "started_date")
    private LocalDateTime startedDate;

    /**
     * When student completed this sequence (reached terminal step or completed all required steps)
     */
    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    /**
     * Is student actively following this sequence?
     * false = student dropped sequence or changed pathways
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Completion percentage (0.0 to 100.0)
     * Calculated based on completed steps vs total required steps
     */
    @Column(name = "completion_percentage")
    @Builder.Default
    private Double completionPercentage = 0.0;

    /**
     * Current step in sequence
     * Null if sequence not started yet
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_step_id")
    private CourseSequenceStep currentStep;

    /**
     * Notes about student's progress in this sequence
     * Example: "Struggling with math concepts", "Excelling in AP track", "May need remediation"
     */
    @Column(columnDefinition = "TEXT")
    private String progressNotes;

    /**
     * Was this sequence recommended by a counselor?
     */
    @Column(name = "is_recommended")
    @Builder.Default
    private Boolean isRecommended = false;

    /**
     * Who recommended this sequence (counselor name or system)
     */
    @Column(name = "recommended_by", length = 100)
    private String recommendedBy;

    /**
     * When this record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this record was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (startedDate == null) {
            startedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if sequence is completed
     */
    public boolean isCompleted() {
        return completedDate != null;
    }

    /**
     * Check if sequence is in progress
     */
    public boolean isInProgress() {
        return Boolean.TRUE.equals(isActive) && !isCompleted();
    }

    /**
     * Mark sequence as completed
     */
    public void markCompleted() {
        this.completedDate = LocalDateTime.now();
        this.completionPercentage = 100.0;
        this.isActive = false;
    }

    /**
     * Update completion percentage
     * @param percentage Completion percentage (0.0 to 100.0)
     */
    public void updateCompletionPercentage(Double percentage) {
        if (percentage < 0.0) {
            this.completionPercentage = 0.0;
        } else if (percentage > 100.0) {
            this.completionPercentage = 100.0;
        } else {
            this.completionPercentage = percentage;
        }

        // Auto-complete if 100%
        if (this.completionPercentage >= 100.0 && completedDate == null) {
            markCompleted();
        }
    }

    /**
     * Get formatted completion percentage
     */
    public String getFormattedCompletionPercentage() {
        if (completionPercentage == null) {
            return "0%";
        }
        return String.format("%.1f%%", completionPercentage);
    }

    /**
     * Get progress status summary
     */
    public String getProgressStatus() {
        if (isCompleted()) {
            return "Completed";
        }
        if (completionPercentage == null || completionPercentage == 0.0) {
            return "Not Started";
        }
        if (completionPercentage < 25.0) {
            return "Just Started";
        }
        if (completionPercentage < 50.0) {
            return "Making Progress";
        }
        if (completionPercentage < 75.0) {
            return "Halfway There";
        }
        if (completionPercentage < 100.0) {
            return "Almost Done";
        }
        return "Completed";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentSequence)) return false;
        StudentSequence that = (StudentSequence) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("StudentSequence[student=%s, sequence=%s, progress=%.1f%%]",
            student != null ? student.getStudentId() : "null",
            courseSequence != null ? courseSequence.getCode() : "null",
            completionPercentage);
    }
}
