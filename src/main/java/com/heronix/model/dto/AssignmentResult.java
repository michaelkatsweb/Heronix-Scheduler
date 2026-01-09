package com.heronix.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assignment Result DTO
 *
 * Contains comprehensive results from the AI course assignment algorithm.
 * Used for reporting, analytics, and UI display after running automated
 * course assignments.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - November 20, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResult {

    // ========================================================================
    // EXECUTION METADATA
    // ========================================================================

    /**
     * When the assignment run started
     */
    private LocalDateTime startTime;

    /**
     * When the assignment run completed
     */
    private LocalDateTime endTime;

    /**
     * Duration in milliseconds
     */
    private Long durationMs;

    /**
     * Who initiated the assignment (username)
     */
    private String initiatedBy;

    /**
     * Academic year ID for this assignment
     */
    private Long academicYearId;

    /**
     * Was this a simulation (dry run) or actual assignment?
     */
    private Boolean isSimulation = false;

    // ========================================================================
    // OVERALL STATISTICS
    // ========================================================================

    /**
     * Total number of enrollment requests processed
     */
    private int totalRequestsProcessed = 0;

    /**
     * Number of requests approved (students enrolled)
     */
    private int requestsApproved = 0;

    /**
     * Number of requests waitlisted
     */
    private int requestsWaitlisted = 0;

    /**
     * Number of requests denied
     */
    private int requestsDenied = 0;

    /**
     * Number of students assigned to alternate courses
     */
    private int requestsAlternateAssigned = 0;

    /**
     * Number of students who got their 1st choice
     */
    private int studentsGotFirstChoice = 0;

    /**
     * Number of students who got their 2nd choice
     */
    private int studentsGotSecondChoice = 0;

    /**
     * Number of students who got their 3rd choice
     */
    private int studentsGotThirdChoice = 0;

    /**
     * Number of students who got their 4th choice (alternate)
     */
    private int studentsGotFourthChoice = 0;

    // ========================================================================
    // STUDENT STATISTICS
    // ========================================================================

    /**
     * Total number of students processed
     */
    private int totalStudentsProcessed = 0;

    /**
     * Students with complete schedules (all courses assigned)
     */
    private int studentsWithCompleteSchedules = 0;

    /**
     * Students with partial schedules (some courses missing)
     */
    private int studentsWithPartialSchedules = 0;

    /**
     * Students with no assignments
     */
    private int studentsWithNoAssignments = 0;

    /**
     * Average number of courses assigned per student
     */
    private double averageCoursesPerStudent = 0.0;

    // ========================================================================
    // COURSE STATISTICS
    // ========================================================================

    /**
     * Total number of courses processed
     */
    private int totalCoursesProcessed = 0;

    /**
     * Courses that are now full (enrollment >= max)
     */
    private int coursesNowFull = 0;

    /**
     * Courses at optimal capacity
     */
    private int coursesAtOptimal = 0;

    /**
     * Courses below minimum capacity (may need to be cancelled)
     */
    private int coursesBelowMinimum = 0;

    /**
     * Courses with waitlists
     */
    private int coursesWithWaitlists = 0;

    /**
     * Average enrollment per course
     */
    private double averageEnrollmentPerCourse = 0.0;

    // ========================================================================
    // SUCCESS METRICS
    // ========================================================================

    /**
     * Overall success rate (approved / total requests) * 100
     */
    private double successRate = 0.0;

    /**
     * First choice satisfaction rate
     */
    private double firstChoiceSatisfactionRate = 0.0;

    /**
     * Priority-based fairness score (0-100)
     * Measures how well high-priority students got their choices
     */
    private double fairnessScore = 0.0;

    /**
     * Class size balance score (0-100)
     * Measures how evenly students are distributed across sections
     */
    private double balanceScore = 0.0;

    // ========================================================================
    // DETAILED RESULTS
    // ========================================================================

    /**
     * List of students who got complete schedules (all 7 courses)
     */
    private List<String> studentsWithCompleteSchedulesList = new ArrayList<>();

    /**
     * List of students who need manual review (incomplete schedules)
     */
    private List<String> studentsNeedingReview = new ArrayList<>();

    /**
     * List of courses below minimum capacity (may be cancelled)
     */
    private List<String> coursesBelowMinimumList = new ArrayList<>();

    /**
     * List of courses with long waitlists (>10 students)
     */
    private List<String> coursesWithLongWaitlists = new ArrayList<>();

    // ========================================================================
    // ISSUES AND WARNINGS
    // ========================================================================

    /**
     * List of issues encountered during assignment
     */
    private List<String> issues = new ArrayList<>();

    /**
     * List of warnings (non-critical issues)
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * Number of conflicts detected
     */
    private int conflictsDetected = 0;

    /**
     * Number of conflicts resolved
     */
    private int conflictsResolved = 0;

    /**
     * List of unresolved conflicts
     */
    private List<String> unresolvedConflicts = new ArrayList<>();

    // ========================================================================
    // COURSE-SPECIFIC RESULTS
    // ========================================================================

    /**
     * Map: Course ID → Course assignment details
     */
    private Map<Long, CourseAssignmentDetail> courseDetails = new HashMap<>();

    /**
     * Map: Student ID → Student assignment details
     */
    private Map<Long, StudentAssignmentDetail> studentDetails = new HashMap<>();

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Calculate derived metrics from raw counts
     */
    public void calculateDerivedMetrics() {
        // Success rate
        if (totalRequestsProcessed > 0) {
            successRate = (double) requestsApproved / totalRequestsProcessed * 100;
        }

        // First choice satisfaction
        if (requestsApproved > 0) {
            firstChoiceSatisfactionRate = (double) studentsGotFirstChoice / requestsApproved * 100;
        }

        // Average courses per student
        if (totalStudentsProcessed > 0) {
            averageCoursesPerStudent = (double) requestsApproved / totalStudentsProcessed;
        }

        // Average enrollment per course
        if (totalCoursesProcessed > 0) {
            averageEnrollmentPerCourse = (double) requestsApproved / totalCoursesProcessed;
        }

        // Duration
        if (startTime != null && endTime != null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * Add an issue to the list
     */
    public void addIssue(String issue) {
        this.issues.add(issue);
    }

    /**
     * Add a warning to the list
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Add a student needing review
     */
    public void addStudentNeedingReview(String studentInfo) {
        this.studentsNeedingReview.add(studentInfo);
    }

    /**
     * Add a course below minimum
     */
    public void addCourseBelowMinimum(String courseInfo) {
        this.coursesBelowMinimumList.add(courseInfo);
    }

    /**
     * Check if assignment was successful overall
     */
    public boolean isSuccessful() {
        return successRate >= 80.0 && issues.isEmpty();
    }

    /**
     * Check if there are critical issues
     */
    public boolean hasCriticalIssues() {
        return !issues.isEmpty() || unresolvedConflicts.size() > 5;
    }

    /**
     * Get summary string
     */
    public String getSummary() {
        return String.format(
            "Assignment %s: %d/%d requests approved (%.1f%%), %d waitlisted, %d denied. " +
            "Duration: %dms. Issues: %d, Warnings: %d",
            isSuccessful() ? "SUCCESSFUL" : "COMPLETED WITH ISSUES",
            requestsApproved,
            totalRequestsProcessed,
            successRate,
            requestsWaitlisted,
            requestsDenied,
            durationMs != null ? durationMs : 0,
            issues.size(),
            warnings.size()
        );
    }

    /**
     * Get detailed report as formatted string
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=".repeat(80)).append("\n");
        report.append("INTELLIGENT COURSE ASSIGNMENT REPORT\n");
        report.append("=".repeat(80)).append("\n\n");

        // Execution info
        report.append("Execution Information:\n");
        report.append(String.format("  Started: %s\n", startTime));
        report.append(String.format("  Completed: %s\n", endTime));
        report.append(String.format("  Duration: %dms\n", durationMs != null ? durationMs : 0));
        report.append(String.format("  Initiated by: %s\n", initiatedBy));
        report.append(String.format("  Mode: %s\n\n", isSimulation ? "SIMULATION" : "ACTUAL"));

        // Overall statistics
        report.append("Overall Statistics:\n");
        report.append(String.format("  Total requests processed: %d\n", totalRequestsProcessed));
        report.append(String.format("  Approved: %d (%.1f%%)\n", requestsApproved, successRate));
        report.append(String.format("  Waitlisted: %d\n", requestsWaitlisted));
        report.append(String.format("  Denied: %d\n", requestsDenied));
        report.append(String.format("  Alternate assigned: %d\n\n", requestsAlternateAssigned));

        // Student statistics
        report.append("Student Statistics:\n");
        report.append(String.format("  Total students: %d\n", totalStudentsProcessed));
        report.append(String.format("  Complete schedules: %d\n", studentsWithCompleteSchedules));
        report.append(String.format("  Partial schedules: %d\n", studentsWithPartialSchedules));
        report.append(String.format("  Need review: %d\n", studentsNeedingReview.size()));
        report.append(String.format("  Avg courses/student: %.1f\n\n", averageCoursesPerStudent));

        // Preference satisfaction
        report.append("Preference Satisfaction:\n");
        report.append(String.format("  1st choice: %d (%.1f%%)\n",
            studentsGotFirstChoice, firstChoiceSatisfactionRate));
        report.append(String.format("  2nd choice: %d\n", studentsGotSecondChoice));
        report.append(String.format("  3rd choice: %d\n", studentsGotThirdChoice));
        report.append(String.format("  4th choice: %d\n\n", studentsGotFourthChoice));

        // Course statistics
        report.append("Course Statistics:\n");
        report.append(String.format("  Total courses: %d\n", totalCoursesProcessed));
        report.append(String.format("  Full courses: %d\n", coursesNowFull));
        report.append(String.format("  At optimal: %d\n", coursesAtOptimal));
        report.append(String.format("  Below minimum: %d\n", coursesBelowMinimum));
        report.append(String.format("  With waitlists: %d\n", coursesWithWaitlists));
        report.append(String.format("  Avg enrollment/course: %.1f\n\n", averageEnrollmentPerCourse));

        // Issues
        if (!issues.isEmpty()) {
            report.append("Critical Issues:\n");
            for (String issue : issues) {
                report.append(String.format("  - %s\n", issue));
            }
            report.append("\n");
        }

        // Warnings
        if (!warnings.isEmpty()) {
            report.append("Warnings:\n");
            for (String warning : warnings) {
                report.append(String.format("  - %s\n", warning));
            }
            report.append("\n");
        }

        // Students needing review
        if (!studentsNeedingReview.isEmpty()) {
            report.append("Students Needing Manual Review:\n");
            for (String student : studentsNeedingReview) {
                report.append(String.format("  - %s\n", student));
            }
            report.append("\n");
        }

        // Courses below minimum
        if (!coursesBelowMinimumList.isEmpty()) {
            report.append("Courses Below Minimum Enrollment:\n");
            for (String course : coursesBelowMinimumList) {
                report.append(String.format("  - %s\n", course));
            }
            report.append("\n");
        }

        report.append("=".repeat(80)).append("\n");
        return report.toString();
    }

    // ========================================================================
    // NESTED DETAIL CLASSES
    // ========================================================================

    /**
     * Detailed results for a specific course
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseAssignmentDetail {
        private Long courseId;
        private String courseCode;
        private String courseName;
        private int requestsReceived;
        private int studentsEnrolled;
        private int waitlistCount;
        private int minCapacity;
        private int optimalCapacity;
        private int maxCapacity;
        private String capacityStatus;
        private int firstChoiceRequests;
        private int secondChoiceRequests;
    }

    /**
     * Detailed results for a specific student
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAssignmentDetail {
        private Long studentId;
        private String studentName;
        private int coursesAssigned;
        private int coursesWaitlisted;
        private int coursesDenied;
        private boolean hasCompleteSchedule;
        private List<String> assignedCourses = new ArrayList<>();
        private List<String> waitlistedCourses = new ArrayList<>();
        private List<String> deniedCourses = new ArrayList<>();
    }
}
