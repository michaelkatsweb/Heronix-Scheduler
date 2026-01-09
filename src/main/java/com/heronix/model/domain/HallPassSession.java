package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Hall Pass Session Entity
 * Tracks student hall pass requests with QR code and facial recognition verification.
 *
 * Features:
 * - Two-factor authentication for departure and return
 * - Automatic duration tracking
 * - Multiple destination options
 * - Overdue detection and alerts
 * - Parent notification for departures and returns
 * - Complete audit trail
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - QR Attendance System Phase 1
 */
@Entity
@Table(name = "hall_pass_sessions", indexes = {
    @Index(name = "idx_hall_pass_student", columnList = "student_id"),
    @Index(name = "idx_hall_pass_status", columnList = "status"),
    @Index(name = "idx_hall_pass_departure", columnList = "departure_time"),
    @Index(name = "idx_hall_pass_active", columnList = "student_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallPassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student using the hall pass
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @ToString.Exclude
    private Student student;

    /**
     * Time when student departed classroom
     */
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    /**
     * Time when student returned to classroom
     * Null if still out (status = ACTIVE)
     */
    @Column(name = "return_time")
    private LocalDateTime returnTime;

    /**
     * Total duration in minutes (calculated when student returns)
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * Destination where student is going
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "destination", nullable = false, length = 50)
    private Destination destination;

    /**
     * Room/classroom student departed from
     */
    @Column(name = "departure_room", length = 50)
    private String departureRoom;

    /**
     * Room/classroom student arrived at (may differ from destination)
     */
    @Column(name = "arrival_room", length = 50)
    private String arrivalRoom;

    /**
     * Teacher who approved the hall pass
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @ToString.Exclude
    private Teacher teacher;

    /**
     * Period number when hall pass was issued
     */
    @Column(name = "period")
    private Integer period;

    /**
     * Photo captured when student departed (facial verification)
     */
    @Lob
    @Column(name = "departure_photo", columnDefinition = "BLOB")
    private byte[] departurePhoto;

    /**
     * Photo captured when student returned (facial verification)
     */
    @Lob
    @Column(name = "return_photo", columnDefinition = "BLOB")
    private byte[] returnPhoto;

    /**
     * Facial recognition match score at departure (0.0 to 1.0)
     */
    @Column(name = "departure_face_match_score")
    private Double departureFaceMatchScore;

    /**
     * Facial recognition match score at return (0.0 to 1.0)
     */
    @Column(name = "return_face_match_score")
    private Double returnFaceMatchScore;

    /**
     * Current status of this hall pass session
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private SessionStatus status = SessionStatus.ACTIVE;

    /**
     * Additional notes about this hall pass
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Has parent been notified of departure?
     */
    @Column(name = "parent_notified_departure")
    private Boolean parentNotifiedDeparture = false;

    /**
     * Has parent been notified of return?
     */
    @Column(name = "parent_notified_return")
    private Boolean parentNotifiedReturn = false;

    /**
     * Has overdue alert been sent?
     */
    @Column(name = "overdue_alert_sent")
    private Boolean overdueAlertSent = false;

    /**
     * Record creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Hall pass destination enum
     */
    public enum Destination {
        BATHROOM("Bathroom"),
        CLINIC("Nurse/Clinic"),
        ADMIN_OFFICE("Admin Office"),
        COUNSELOR("Counselor"),
        LIBRARY("Library"),
        CAFETERIA("Cafeteria"),
        ANOTHER_CLASSROOM("Another Classroom"),
        LOCKER("Locker"),
        WATER_FOUNTAIN("Water Fountain"),
        OFFICE("Main Office"),
        OTHER("Other");

        private final String displayName;

        Destination(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Hall pass session status enum
     */
    public enum SessionStatus {
        /**
         * Student has departed, not yet returned
         */
        ACTIVE,

        /**
         * Student has returned, session complete
         */
        COMPLETED,

        /**
         * Student has exceeded maximum allowed time
         */
        OVERDUE,

        /**
         * Student never returned (requires admin follow-up)
         */
        ABANDONED,

        /**
         * Emergency situation (lockdown, etc.)
         */
        EMERGENCY
    }

    /**
     * JPA lifecycle callback - set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.departureTime == null) {
            this.departureTime = LocalDateTime.now();
        }
    }

    /**
     * JPA lifecycle callback - update timestamp on modification
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate duration in minutes
     * If still active, calculates time since departure
     * If completed, returns stored duration
     */
    public int calculateDuration() {
        if (this.durationMinutes != null) {
            return this.durationMinutes;
        }

        LocalDateTime endTime = (returnTime != null) ? returnTime : LocalDateTime.now();
        return (int) Duration.between(departureTime, endTime).toMinutes();
    }

    /**
     * Complete the hall pass session (student has returned)
     */
    public void completeSession(byte[] returnPhoto, Double faceMatchScore) {
        this.returnTime = LocalDateTime.now();
        this.returnPhoto = returnPhoto;
        this.returnFaceMatchScore = faceMatchScore;
        this.durationMinutes = calculateDuration();
        this.status = SessionStatus.COMPLETED;
    }

    /**
     * Mark session as overdue
     */
    public void markOverdue() {
        if (this.status == SessionStatus.ACTIVE) {
            this.status = SessionStatus.OVERDUE;
        }
    }

    /**
     * Check if session is currently active
     */
    public boolean isActive() {
        return this.status == SessionStatus.ACTIVE || this.status == SessionStatus.OVERDUE;
    }

    /**
     * Check if session exceeds maximum allowed duration
     */
    public boolean isOverdue(int maxDurationMinutes) {
        if (this.status != SessionStatus.ACTIVE) {
            return false;
        }
        return calculateDuration() > maxDurationMinutes;
    }

    /**
     * Get formatted duration string
     */
    public String getFormattedDuration() {
        int minutes = calculateDuration();
        if (minutes < 60) {
            return minutes + " minutes";
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            return String.format("%d hour%s %d minute%s",
                hours, hours == 1 ? "" : "s",
                remainingMinutes, remainingMinutes == 1 ? "" : "s");
        }
    }
}
