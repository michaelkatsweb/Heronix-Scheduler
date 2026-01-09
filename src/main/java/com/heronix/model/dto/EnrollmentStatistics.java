package com.heronix.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Enrollment Statistics DTO
 * Location: src/main/java/com/eduscheduler/model/dto/EnrollmentStatistics.java
 * 
 * Statistics about student course enrollments
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
@Data
@NoArgsConstructor
public class EnrollmentStatistics {

    private int totalEnrollments;
    private int activeEnrollments;
    private Map<String, Long> enrollmentsByCourse;

    /**
     * Get formatted summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Enrollment Statistics:\n");
        sb.append(String.format("  Total Enrollments: %d\n", totalEnrollments));
        sb.append(String.format("  Active Enrollments: %d\n", activeEnrollments));

        if (enrollmentsByCourse != null && !enrollmentsByCourse.isEmpty()) {
            sb.append("\nEnrollments by Course:\n");
            enrollmentsByCourse
                    .forEach((course, count) -> sb.append(String.format("  %s: %d students\n", course, count)));
        }

        return sb.toString();
    }
}