package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "waitlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "schedule_slot_id")
    private ScheduleSlot scheduleSlot;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "priority_weight")
    private Integer priorityWeight = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WaitlistStatus status = WaitlistStatus.ACTIVE;

    @Column(name = "added_at")
    private java.time.LocalDateTime addedAt = java.time.LocalDateTime.now();

    @Column(name = "enrolled_at")
    private java.time.LocalDateTime enrolledAt;

    @Column(name = "bypass_reason")
    private String bypassReason;

    @ManyToOne
    @JoinColumn(name = "conflicting_slot_id")
    private ScheduleSlot conflictingSlot;

    @Column(name = "drop_if_enrolled_course_id")
    private Long dropIfEnrolledCourseId;

    @Column(name = "max_units_exceeded")
    private Boolean maxUnitsExceeded = false;

    @Column(name = "has_hold")
    private Boolean hasHold = false;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    public enum WaitlistStatus {
        ACTIVE,         // Currently on waitlist
        ENROLLED,       // Successfully enrolled from waitlist
        BYPASSED,       // Skipped due to conflict/holds/limits
        EXPIRED,        // Waitlist period ended
        CANCELLED       // Student cancelled
    }
}
