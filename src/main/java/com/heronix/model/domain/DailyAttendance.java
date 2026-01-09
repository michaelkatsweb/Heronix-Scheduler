package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Daily Attendance Summary Entity
 * Aggregates attendance for a student for an entire school day.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Attendance System
 */
@Entity
@Table(name = "daily_attendance", indexes = {
    @Index(name = "idx_daily_attendance_student", columnList = "student_id, attendance_date"),
    @Index(name = "idx_daily_attendance_date", columnList = "attendance_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    @Builder.Default
    private OverallStatus overallStatus = OverallStatus.PRESENT;

    @Column(name = "periods_present")
    @Builder.Default
    private Integer periodsPresent = 0;

    @Column(name = "periods_absent")
    @Builder.Default
    private Integer periodsAbsent = 0;

    @Column(name = "periods_tardy")
    @Builder.Default
    private Integer periodsTardy = 0;

    @Column(name = "total_periods")
    @Builder.Default
    private Integer totalPeriods = 0;

    @Column(name = "first_arrival")
    private LocalTime firstArrival;

    @Column(name = "last_departure")
    private LocalTime lastDeparture;

    @Column(name = "total_minutes_present")
    private Integer totalMinutesPresent;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    public enum OverallStatus {
        PRESENT,           // Full day present
        ABSENT,            // Full day absent
        PARTIAL,           // Some periods attended
        TARDY,             // Arrived late
        HALF_DAY,          // Only half day
        REMOTE             // Virtual attendance
    }

    // Calculated fields
    @Transient
    public double getAttendanceRate() {
        if (totalPeriods == null || totalPeriods == 0) return 0;
        return (double) periodsPresent / totalPeriods * 100;
    }

    @Transient
    public boolean isChronicAbsent() {
        return getAttendanceRate() < 90;
    }
}
