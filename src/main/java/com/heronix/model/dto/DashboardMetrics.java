// Location: src/main/java/com/eduscheduler/model/dto/DashboardMetrics.java
package com.heronix.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Dashboard metrics for display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {

    // Entity counts
    private long totalTeachers;
    private long totalCourses;
    private long totalRooms;
    private long totalStudents;
    private long totalSchedules;

    // Active entities
    private long activeTeachers;
    private long activeCourses;
    private long activeRooms;
    private long activeStudents;

    // Utilization metrics
    private double teacherUtilizationRate;
    private double roomUtilizationRate;
    private double overallEfficiency;

    // Schedule quality
    private int totalConflicts;
    private int unresolvedConflicts;
    private double averageOptimizationScore;

    // Status counts
    private long draftSchedules;
    private long publishedSchedules;
    private long archivedSchedules;

    // Recent activity
    private int recentImports;
    private int recentExports;
    private int recentScheduleGenerations;

    // ========================================================================
    // PHASE 2: Course Assignment Status Metrics
    // ========================================================================

    // Assignment status counts
    private long fullyAssignedCourses;      // Both teacher AND room assigned
    private long partiallyAssignedCourses;  // Only teacher OR room assigned
    private long unassignedCourses;         // Neither teacher nor room assigned

    // Assignment status percentages
    private double fullyAssignedPercent;
    private double partiallyAssignedPercent;
    private double unassignedPercent;

    // Issue counts for "Attention Required" section
    private long overloadedTeachersCount;         // Teachers with 6+ courses
    private long underutilizedTeachersCount;      // Teachers with 0 courses
    private long certificationMismatchCount;      // Courses taught by non-certified teachers
    private long labRoomIssuesCount;              // Lab courses without lab rooms
    private long capacityIssuesCount;             // Rooms too small for enrollment

    // Issue descriptions for display
    private String overloadedTeachersMessage;
    private String underutilizedTeachersMessage;
    private String certificationMismatchMessage;
    private String labRoomIssuesMessage;
    private String capacityIssuesMessage;
}