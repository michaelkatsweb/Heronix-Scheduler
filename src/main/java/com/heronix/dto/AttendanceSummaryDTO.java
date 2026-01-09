package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Attendance Summary Data Transfer Object
 * Contains aggregated attendance statistics
 *
 * @author Heronix Scheduling System Team
 * @since December 13, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryDTO {
    private Long studentId;
    private String studentName;

    // Overall statistics
    private int totalDays;
    private int daysPresent;
    private int daysAbsent;
    private int daysExcused;
    private int daysUnexcused;
    private int daysTardy;

    // Percentages
    private Double attendanceRate;
    private Double absenceRate;
    private Double tardyRate;

    // Period-based statistics
    private int totalPeriods;
    private int periodsPresent;
    private int periodsAbsent;

    // Alerts
    private boolean hasChronicAbsence; // 10%+ absence rate
    private boolean hasExcessiveTardies; // 5+ tardies
    private String alertLevel; // "NONE", "WARNING", "CRITICAL"
}
