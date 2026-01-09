package com.heronix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for displaying student grades across all courses in the Student View tab
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2E - Student View Enhancements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseGradeDTO {

    /**
     * Course name
     */
    private String courseName;

    /**
     * Course code (e.g., "ENG101")
     */
    private String courseCode;

    /**
     * Average percentage across all assignments in the course
     * Null if no grades entered yet
     */
    private Double averagePercentage;

    /**
     * Letter grade (A/B/C/D/F)
     * Null if no grades entered yet
     */
    private String letterGrade;

    /**
     * Count of missing assignments in the course
     */
    private Integer missingAssignments;

    /**
     * Student status in the course: "On Track", "At Risk", "Failing"
     */
    private String status;

    /**
     * Calculate letter grade from percentage
     */
    public static String calculateLetterGrade(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    /**
     * Calculate status from percentage
     */
    public static String calculateStatus(double percentage) {
        if (percentage >= 70) return "On Track";
        if (percentage >= 60) return "At Risk";
        return "Failing";
    }
}
