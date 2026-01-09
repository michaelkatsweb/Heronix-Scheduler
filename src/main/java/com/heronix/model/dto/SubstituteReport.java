package com.heronix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Base report DTO for substitute management reports
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubstituteReport {

    private String reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate generatedDate;
    private String generatedBy;

    private ReportSummary summary;
    private List<ReportRow> rows;

    /**
     * Report summary statistics
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportSummary {
        private int totalAssignments;
        private int totalSubstitutes;
        private double totalHours;
        private double totalCost;
        private int confirmedAssignments;
        private int pendingAssignments;
        private int cancelledAssignments;
    }

    /**
     * Individual report row
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportRow {
        private LocalDate date;
        private String substituteName;
        private String substituteType;
        private String replacedTeacherName;
        private String absenceReason;
        private String duration;
        private double hours;
        private double cost;
        private String status;
        private String room;
        private String course;
    }
}
