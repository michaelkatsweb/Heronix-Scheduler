package com.heronix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Report showing breakdown of absence reasons
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceReasonsReport {

    private List<AbsenceReasonRow> reasons;
    private AbsenceReasonSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsenceReasonRow {
        private String reasonName;
        private int count;
        private double percentage;
        private double totalHours;
        private double averageHoursPerAssignment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsenceReasonSummary {
        private int totalAssignments;
        private int uniqueReasons;
        private String mostCommonReason;
        private String leastCommonReason;
        private double totalHours;
    }
}
