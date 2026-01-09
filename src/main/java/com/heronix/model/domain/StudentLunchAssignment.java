package com.heronix.model.domain;

import com.heronix.model.enums.LunchAssignmentMethod;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Maps a student to a specific lunch wave within a schedule
 *
 * Key features:
 * - Tracks which lunch period each student attends
 * - Records how the assignment was made (manual, alphabetical, by grade, etc.)
 * - Supports manual overrides for special cases
 * - Maintains assignment history for auditing
 *
 * Real-world usage:
 * - Weeki Wachee HS: 1,600 students assigned to 3 lunch waves
 * - Parrott MS: Students assigned by grade level to their lunch period
 *
 * Phase 5A: Multiple Rotating Lunch Periods
 * Date: December 1, 2025
 */
@Entity
@Table(name = "student_lunch_assignments",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "schedule_id"})
    },
    indexes = {
        @Index(name = "idx_student_lunch_student", columnList = "student_id"),
        @Index(name = "idx_student_lunch_schedule", columnList = "schedule_id"),
        @Index(name = "idx_student_lunch_wave", columnList = "lunch_wave_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLunchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The student being assigned
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * The schedule this assignment belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    /**
     * The lunch wave this student is assigned to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lunch_wave_id", nullable = false)
    private LunchWave lunchWave;

    /**
     * How this assignment was made
     * Examples: BY_GRADE_LEVEL, ALPHABETICAL, MANUAL, OPTIMIZED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_method", nullable = false)
    private LunchAssignmentMethod assignmentMethod;

    /**
     * Whether this assignment was manually overridden by an administrator
     * True = admin manually changed the assignment
     * False = assignment was made automatically based on the method
     */
    @Column(name = "manual_override")
    @Builder.Default
    private Boolean manualOverride = false;

    /**
     * When this assignment was created
     */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    /**
     * User who made or last modified this assignment
     */
    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    /**
     * When this assignment was last modified
     */
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    /**
     * User who last modified this assignment
     */
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    /**
     * Optional notes about this assignment
     * Examples: "IEP accommodation", "Medical need", "Parent request"
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Priority level for this assignment (higher = more important to maintain)
     * Used when reassigning students or balancing lunch waves
     * 1 = Low priority (can be moved easily)
     * 5 = Medium priority (prefer not to move)
     * 10 = High priority (keep fixed if possible)
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    /**
     * Whether this assignment is locked (cannot be automatically changed)
     * True = admin has locked this assignment, AI cannot modify it
     * False = assignment can be optimized/changed
     */
    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    // ========== Helper Methods ==========

    /**
     * Check if this is a manual assignment
     */
    public boolean isManualAssignment() {
        return assignmentMethod == LunchAssignmentMethod.MANUAL ||
               (manualOverride != null && manualOverride);
    }

    /**
     * Check if this assignment can be automatically changed
     */
    public boolean canBeReassigned() {
        return isLocked == null || !isLocked;
    }

    /**
     * Check if this is a high-priority assignment
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 8;
    }

    /**
     * Check if this is a low-priority assignment
     */
    public boolean isLowPriority() {
        return priority != null && priority <= 3;
    }

    /**
     * Mark this assignment as manually overridden
     */
    public void markAsManualOverride(String username) {
        this.manualOverride = true;
        this.assignmentMethod = LunchAssignmentMethod.MANUAL;
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = username;
    }

    /**
     * Lock this assignment to prevent automatic changes
     */
    public void lock(String username) {
        this.isLocked = true;
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = username;
    }

    /**
     * Unlock this assignment to allow automatic changes
     */
    public void unlock(String username) {
        this.isLocked = false;
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = username;
    }

    /**
     * Update the modification timestamp and user
     */
    public void updateModification(String username) {
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = username;
    }

    /**
     * Get a display string for this assignment
     * Example: "John Doe → Lunch 2 (Alphabetical)"
     */
    public String getDisplayString() {
        String studentName = student != null ? student.getFullName() : "Unknown Student";
        String waveName = lunchWave != null ? lunchWave.getWaveName() : "Unknown Wave";
        String method = assignmentMethod != null ? assignmentMethod.getDisplayName() : "Unknown";

        if (manualOverride != null && manualOverride) {
            method += " [Manual Override]";
        }
        if (isLocked != null && isLocked) {
            method += " [Locked]";
        }

        return String.format("%s → %s (%s)", studentName, waveName, method);
    }

    /**
     * Get a summary for logging
     */
    public String getSummary() {
        return String.format("StudentLunchAssignment{student=%s, wave=%s, method=%s, locked=%s}",
            student != null ? student.getStudentId() : "null",
            lunchWave != null ? lunchWave.getWaveName() : "null",
            assignmentMethod,
            isLocked
        );
    }

    @Override
    public String toString() {
        return String.format("StudentLunchAssignment{id=%d, student=%s, wave=%s, method=%s, override=%s, locked=%s}",
            id,
            student != null ? student.getStudentId() : "null",
            lunchWave != null ? lunchWave.getWaveName() : "null",
            assignmentMethod,
            manualOverride,
            isLocked
        );
    }

    /**
     * JPA lifecycle callback - set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (lastModifiedAt == null) {
            lastModifiedAt = LocalDateTime.now();
        }
    }

    /**
     * JPA lifecycle callback - update modification timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = LocalDateTime.now();
    }
}
