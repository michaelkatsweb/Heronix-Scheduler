package com.heronix.model.domain;

import com.heronix.model.enums.LunchAssignmentMethod;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Maps a teacher to a specific lunch wave within a schedule
 *
 * Key considerations for teachers:
 * - Teachers may have lunch duty during other waves
 * - Teachers may need to teach during split periods (4A, 4B, 4C)
 * - Some teachers get a "duty-free lunch" (contractual requirement)
 * - Department heads may need specific lunch times for meetings
 *
 * Real-world examples:
 * - Weeki Wachee: Teachers assigned to supervise specific lunch waves
 * - Many schools: Teachers rotate lunch duty or have permanent assignments
 *
 * Phase 5A: Multiple Rotating Lunch Periods
 * Date: December 1, 2025
 */
@Entity
@Table(name = "teacher_lunch_assignments",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"teacher_id", "schedule_id"})
    },
    indexes = {
        @Index(name = "idx_teacher_lunch_teacher", columnList = "teacher_id"),
        @Index(name = "idx_teacher_lunch_schedule", columnList = "schedule_id"),
        @Index(name = "idx_teacher_lunch_wave", columnList = "lunch_wave_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherLunchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The teacher being assigned
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    /**
     * The schedule this assignment belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    /**
     * The lunch wave when this teacher has their lunch break
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lunch_wave_id", nullable = false)
    private LunchWave lunchWave;

    /**
     * How this assignment was made
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_method", nullable = false)
    private LunchAssignmentMethod assignmentMethod;

    /**
     * Whether this teacher has cafeteria duty during OTHER lunch waves
     * True = teacher supervises cafeteria during waves they're not eating
     * False = teacher is free during other lunch waves (or teaching)
     */
    @Column(name = "has_duty_during_other_waves")
    @Builder.Default
    private Boolean hasDutyDuringOtherWaves = false;

    /**
     * Whether this is a duty-free lunch (contractual requirement)
     * True = teacher must not have any duties during their lunch
     * False = teacher may be assigned duties during their lunch if needed
     */
    @Column(name = "is_duty_free")
    @Builder.Default
    private Boolean isDutyFree = true;

    /**
     * Whether this teacher is assigned to supervise cafeteria during their wave
     * True = teacher supervises students during their own lunch time
     * False = teacher has free lunch time
     */
    @Column(name = "has_supervision_duty")
    @Builder.Default
    private Boolean hasSupervisionDuty = false;

    /**
     * Location where teacher supervises (if applicable)
     * Examples: "Main Cafeteria", "Courtyard", "Library Lunch Area"
     */
    @Column(name = "supervision_location", length = 100)
    private String supervisionLocation;

    /**
     * Whether this assignment was manually set by an administrator
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
     * Examples: "Department meeting during Lunch 2", "Medical restriction"
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Priority level for this assignment (higher = more important to maintain)
     * 1 = Low priority (can be moved easily)
     * 5 = Medium priority (prefer not to move)
     * 10 = High priority (keep fixed if possible)
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    /**
     * Whether this assignment is locked (cannot be automatically changed)
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
     * Check if teacher has any cafeteria duties
     */
    public boolean hasAnyDuties() {
        return (hasSupervisionDuty != null && hasSupervisionDuty) ||
               (hasDutyDuringOtherWaves != null && hasDutyDuringOtherWaves);
    }

    /**
     * Check if teacher is truly duty-free
     */
    public boolean isTrulyDutyFree() {
        return (isDutyFree != null && isDutyFree) && !hasAnyDuties();
    }

    /**
     * Check if this is a high-priority assignment
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 8;
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
     * Assign supervision duty at a location
     */
    public void assignSupervisionDuty(String location, String username) {
        this.hasSupervisionDuty = true;
        this.supervisionLocation = location;
        this.isDutyFree = false;
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = username;
    }

    /**
     * Remove supervision duty
     */
    public void removeSupervisionDuty(String username) {
        this.hasSupervisionDuty = false;
        this.supervisionLocation = null;
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
     * Example: "Ms. Johnson → Lunch 2 (Duty-Free)" or "Mr. Smith → Lunch 1 (Cafeteria Duty)"
     */
    public String getDisplayString() {
        String teacherName = teacher != null ? teacher.getName() : "Unknown Teacher";
        String waveName = lunchWave != null ? lunchWave.getWaveName() : "Unknown Wave";

        StringBuilder status = new StringBuilder();
        if (hasSupervisionDuty != null && hasSupervisionDuty) {
            status.append("Supervision");
            if (supervisionLocation != null && !supervisionLocation.isEmpty()) {
                status.append(" @ ").append(supervisionLocation);
            }
        } else if (isDutyFree != null && isDutyFree) {
            status.append("Duty-Free");
        } else {
            status.append(assignmentMethod != null ? assignmentMethod.getDisplayName() : "Unknown");
        }

        if (manualOverride != null && manualOverride) {
            status.append(" [Manual]");
        }
        if (isLocked != null && isLocked) {
            status.append(" [Locked]");
        }

        return String.format("%s → %s (%s)", teacherName, waveName, status);
    }

    /**
     * Get duty status description
     */
    public String getDutyStatus() {
        if (hasSupervisionDuty != null && hasSupervisionDuty) {
            return "Supervision during lunch";
        } else if (hasDutyDuringOtherWaves != null && hasDutyDuringOtherWaves) {
            return "Duty during other waves";
        } else if (isDutyFree != null && isDutyFree) {
            return "Duty-free lunch";
        } else {
            return "Regular lunch";
        }
    }

    /**
     * Get a summary for logging
     */
    public String getSummary() {
        return String.format("TeacherLunchAssignment{teacher=%s, wave=%s, method=%s, dutyFree=%s, locked=%s}",
            teacher != null ? teacher.getEmployeeId() : "null",
            lunchWave != null ? lunchWave.getWaveName() : "null",
            assignmentMethod,
            isDutyFree,
            isLocked
        );
    }

    @Override
    public String toString() {
        return String.format("TeacherLunchAssignment{id=%d, teacher=%s, wave=%s, dutyFree=%s, supervision=%s}",
            id,
            teacher != null ? teacher.getEmployeeId() : "null",
            lunchWave != null ? lunchWave.getWaveName() : "null",
            isDutyFree,
            hasSupervisionDuty
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
