package com.heronix.model.domain;

import com.heronix.model.enums.PullOutStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Pull-Out Schedule Entity
 *
 * Represents a scheduled pull-out session for an IEP service.
 * Students are "pulled out" of their general education class to
 * receive specialized services (speech therapy, OT, PT, etc.)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Entity
@Table(name = "pull_out_schedules",
       indexes = {
           @Index(name = "idx_pullout_service", columnList = "iep_service_id"),
           @Index(name = "idx_pullout_student", columnList = "student_id"),
           @Index(name = "idx_pullout_staff", columnList = "staff_id"),
           @Index(name = "idx_pullout_day", columnList = "day_of_week"),
           @Index(name = "idx_pullout_time", columnList = "start_time"),
           @Index(name = "idx_pullout_status", columnList = "status")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PullOutSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The IEP service this session fulfills
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iep_service_id", nullable = false)
    private IEPService iepService;

    /**
     * The student receiving the service (denormalized for quick lookup)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * SPED staff providing the service
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Teacher staff;

    /**
     * Day of the week
     * Values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
     */
    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    /**
     * Session start time
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Session end time
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Duration in minutes (calculated field for convenience)
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /**
     * Room where service is provided (optional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    /**
     * Location description (used if room not specified)
     */
    @Column(name = "location_description", length = 200)
    private String locationDescription;

    /**
     * Does this schedule repeat weekly?
     */
    @Column(name = "recurring", nullable = false)
    private Boolean recurring = true;

    /**
     * Date when this schedule starts
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Date when this schedule ends (null = continues indefinitely)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Other students in this session (for group sessions)
     * Comma-separated student IDs
     */
    @Column(name = "other_students", length = 500)
    private String otherStudents;

    /**
     * Current status of this schedule
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PullOutStatus status = PullOutStatus.ACTIVE;

    /**
     * Notes about this scheduled session
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * When this schedule was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this schedule was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;

        // Calculate duration if not set
        if (durationMinutes == null && startTime != null && endTime != null) {
            durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Recalculate duration if times changed
        if (startTime != null && endTime != null) {
            durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();
        }
    }

    /**
     * Check if this schedule is currently active
     */
    public boolean isActive() {
        if (status != PullOutStatus.ACTIVE) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) {
            return false; // Not started yet
        }

        if (endDate != null && today.isAfter(endDate)) {
            return false; // Already ended
        }

        return true;
    }

    /**
     * Get the IEP this schedule belongs to (convenience method)
     */
    public IEP getIEP() {
        return iepService != null ? iepService.getIep() : null;
    }

    /**
     * Get service type (convenience method)
     */
    public String getServiceType() {
        return iepService != null ? iepService.getServiceType().getDisplayName() : "Unknown";
    }

    /**
     * Get location display string
     */
    public String getLocationDisplay() {
        if (room != null) {
            return room.getRoomNumber();
        }
        return locationDescription != null ? locationDescription : "TBD";
    }

    /**
     * Check if this is a group session
     */
    public boolean isGroupSession() {
        return otherStudents != null && !otherStudents.trim().isEmpty();
    }

    /**
     * Get count of students in this session
     */
    public int getStudentCount() {
        if (!isGroupSession()) {
            return 1; // Just the primary student
        }
        // Count comma-separated student IDs
        return otherStudents.split(",").length + 1; // +1 for primary student
    }

    /**
     * Get display string for this schedule
     */
    public String getDisplayString() {
        return String.format("%s - %s %s-%s (%d min) - %s",
            student != null ? student.getFullName() : "Unknown",
            dayOfWeek,
            startTime,
            endTime,
            durationMinutes,
            getServiceType());
    }

    @Override
    public String toString() {
        return String.format("PullOutSchedule{id=%d, student=%s, day=%s, time=%s-%s, service=%s, status=%s}",
            id,
            student != null ? student.getStudentId() : "null",
            dayOfWeek,
            startTime,
            endTime,
            iepService != null ? iepService.getServiceType() : "null",
            status);
    }
}
