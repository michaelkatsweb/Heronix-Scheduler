package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Attendance Record Entity
 * Tracks student attendance for each class period.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Attendance System
 */
@Entity
@Table(name = "attendance_records", indexes = {
    @Index(name = "idx_attendance_student_date", columnList = "student_id, attendance_date"),
    @Index(name = "idx_attendance_course_date", columnList = "course_id, attendance_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_slot_id")
    private ScheduleSlot scheduleSlot;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "period_number")
    private Integer periodNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "minutes_absent")
    private Integer minutesAbsent;

    @Column(name = "excuse_code")
    private String excuseCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "verified")
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "parent_notified")
    @Builder.Default
    private Boolean parentNotified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    /**
     * Timestamp when this record was last updated
     * Used for conflict detection and audit trails
     */
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    /**
     * Timestamp when this record was created
     */
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    public enum AttendanceStatus {
        PRESENT,           // Student attended
        ABSENT,            // Student did not attend
        TARDY,             // Student arrived late
        EXCUSED_ABSENT,    // Excused absence (doctor, etc.)
        UNEXCUSED_ABSENT,  // Unexcused absence
        EARLY_DEPARTURE,   // Left early
        SCHOOL_ACTIVITY,   // Field trip, sports, etc.
        SUSPENDED,         // Disciplinary
        REMOTE,            // Virtual attendance
        HALF_DAY           // Partial day attendance
    }

    // Calculated fields
    @Transient
    public boolean isAbsent() {
        return status == AttendanceStatus.ABSENT ||
               status == AttendanceStatus.EXCUSED_ABSENT ||
               status == AttendanceStatus.UNEXCUSED_ABSENT;
    }

    @Transient
    public boolean isPresent() {
        return status == AttendanceStatus.PRESENT ||
               status == AttendanceStatus.TARDY ||
               status == AttendanceStatus.REMOTE;
    }

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    /**
     * Called before entity is persisted to database
     * Sets creation and update timestamps
     */
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * Called before entity is updated in database
     * Updates the update timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
