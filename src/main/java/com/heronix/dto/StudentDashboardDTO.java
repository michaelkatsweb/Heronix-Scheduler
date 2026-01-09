package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Student Dashboard Data Transfer Object
 * Contains summary information for student dashboard
 *
 * @author Heronix Scheduling System Team
 * @since December 13, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDashboardDTO {
    // Student basic info
    private Long studentId;
    private String studentName;
    private String gradeLevel;
    private String photoPath;

    // Academic summary
    private Double currentGPA;
    private Double unweightedGPA;
    private Double weightedGPA;
    private String academicStanding;
    private String honorRollStatus;
    private Double creditsEarned;
    private Double creditsRequired;
    private String graduationStatus; // "On Track", "At Risk", "Retention Risk"

    // Quick stats
    private int totalCourses;
    private int upcomingAssignmentsCount;
    private int missingAssignmentsCount;

    // Attendance summary
    private int totalDaysPresent;
    private int totalDaysAbsent;
    private int totalDaysTardy;
    private Double attendanceRate;

    // Grade average
    private String averageLetterGrade;
    private Double averageNumericGrade;

    // Alerts
    private int activeAlertsCount;
    private boolean hasIEP;
    private boolean has504Plan;
}
