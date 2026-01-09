package com.heronix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Enrollment Result DTO - PRODUCTION VERSION
 * Location: src/main/java/com/eduscheduler/model/dto/EnrollmentResult.java
 * 
 * Uses Lombok for clean, maintainable, production-ready code.
 * 
 * Example usage:
 * 
 * <pre>
 * EnrollmentResult result = EnrollmentResult.builder()
 *         .scheduleId(1L)
 *         .success(true)
 *         .totalStudents(1400)
 *         .totalEnrollments(9000)
 *         .averageCoursesPerStudent(6.4)
 *         .build();
 * </pre>
 * 
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - Production with Builder Pattern
 * @since 2025-10-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResult {

    private Long scheduleId;
    private boolean success;
    private int totalStudents;
    private int totalEnrollments;
    private int failedEnrollments;
    private double averageCoursesPerStudent;

    @Builder.Default
    private Map<String, Integer> enrollmentsByGrade = new HashMap<>();

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * Increment enrollment count for a grade level
     */
    public void incrementStudentEnrollment(String gradeLevel, int count) {
        enrollmentsByGrade.put(gradeLevel,
                enrollmentsByGrade.getOrDefault(gradeLevel, 0) + count);
    }

    /**
     * Get formatted summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║           STUDENT ENROLLMENT COMPLETED                       ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        sb.append(String.format("Status: %s\n", success ? "✅ SUCCESS" : "❌ FAILED"));
        sb.append(String.format("Total Students: %d\n", totalStudents));
        sb.append(String.format("Total Enrollments: %d\n", totalEnrollments));
        sb.append(String.format("Avg Courses per Student: %.1f\n", averageCoursesPerStudent));
        sb.append(String.format("Failed: %d\n", failedEnrollments));

        if (!enrollmentsByGrade.isEmpty()) {
            sb.append("\nEnrollments by Grade:\n");
            enrollmentsByGrade
                    .forEach((grade, count) -> sb.append(String.format("  Grade %s: %d courses\n", grade, count)));
        }

        if (startTime != null && endTime != null) {
            long seconds = java.time.Duration.between(startTime, endTime).getSeconds();
            sb.append(String.format("\nProcessing Time: %d seconds\n", seconds));
        }

        return sb.toString();
    }
}