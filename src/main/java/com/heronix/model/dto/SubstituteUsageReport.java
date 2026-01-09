package com.heronix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Report showing substitute usage statistics
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubstituteUsageReport {

    private List<SubstituteUsageRow> substitutes;
    private UsageSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubstituteUsageRow {
        private String substituteName;
        private String substituteType;
        private String email;
        private String phone;
        private int totalAssignments;
        private double totalHours;
        private double averageHoursPerAssignment;
        private int confirmedAssignments;
        private int pendingAssignments;
        private int cancelledAssignments;
        private String mostCommonAbsenceReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageSummary {
        private int totalSubstitutes;
        private int activeSubstitutes;
        private int totalAssignments;
        private double totalHours;
        private double averageAssignmentsPerSubstitute;
        private double averageHoursPerSubstitute;
    }
}
