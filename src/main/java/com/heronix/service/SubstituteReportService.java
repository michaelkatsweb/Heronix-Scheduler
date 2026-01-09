package com.heronix.service;

import com.heronix.model.domain.Substitute;
import com.heronix.model.domain.SubstituteAssignment;
import com.heronix.model.dto.AbsenceReasonsReport;
import com.heronix.model.dto.SubstituteReport;
import com.heronix.model.dto.SubstituteUsageReport;
import com.heronix.model.enums.AbsenceReason;
import com.heronix.model.enums.AssignmentStatus;
import com.heronix.repository.SubstituteAssignmentRepository;
import com.heronix.repository.SubstituteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating substitute management reports
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Service
@Transactional(readOnly = true)
public class SubstituteReportService {

    private static final Logger logger = LoggerFactory.getLogger(SubstituteReportService.class);

    @Autowired
    private SubstituteAssignmentRepository assignmentRepository;

    @Autowired
    private SubstituteRepository substituteRepository;

    /**
     * Generate substitute usage summary report
     */
    public SubstituteUsageReport generateUsageSummary(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating substitute usage report from {} to {}", startDate, endDate);

        List<SubstituteAssignment> assignments = assignmentRepository.findByAssignmentDateBetween(startDate, endDate);
        List<Substitute> allSubstitutes = substituteRepository.findAll();

        // Group assignments by substitute
        Map<Substitute, List<SubstituteAssignment>> assignmentsBySubstitute = assignments.stream()
                .filter(a -> a.getSubstitute() != null)
                .collect(Collectors.groupingBy(SubstituteAssignment::getSubstitute));

        // Build usage rows
        List<SubstituteUsageReport.SubstituteUsageRow> rows = new ArrayList<>();

        for (Substitute substitute : allSubstitutes) {
            List<SubstituteAssignment> subAssignments = assignmentsBySubstitute.getOrDefault(substitute, Collections.emptyList());

            int totalAssignments = subAssignments.size();
            int confirmed = (int) subAssignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED ||
                               a.getStatus() == AssignmentStatus.IN_PROGRESS ||
                               a.getStatus() == AssignmentStatus.COMPLETED)
                    .count();
            int pending = (int) subAssignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                    .count();
            int cancelled = (int) subAssignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.CANCELLED)
                    .count();

            double totalHours = subAssignments.stream()
                    .filter(a -> a.getTotalHours() != null)
                    .mapToDouble(SubstituteAssignment::getTotalHours)
                    .sum();

            double avgHours = totalAssignments > 0 ? totalHours / totalAssignments : 0.0;

            // Find most common absence reason
            String mostCommonReason = subAssignments.stream()
                    .filter(a -> a.getAbsenceReason() != null)
                    .collect(Collectors.groupingBy(SubstituteAssignment::getAbsenceReason, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey().getDisplayName())
                    .orElse("N/A");

            SubstituteUsageReport.SubstituteUsageRow row = new SubstituteUsageReport.SubstituteUsageRow(
                    substitute.getFullName(),
                    substitute.getType() != null ? substitute.getType().getDisplayName() : "Unknown",
                    substitute.getEmail(),
                    substitute.getPhoneNumber(),
                    totalAssignments,
                    totalHours,
                    avgHours,
                    confirmed,
                    pending,
                    cancelled,
                    mostCommonReason
            );

            rows.add(row);
        }

        // Sort by total assignments descending
        rows.sort((a, b) -> Integer.compare(b.getTotalAssignments(), a.getTotalAssignments()));

        // Calculate summary
        int totalSubstitutes = allSubstitutes.size();
        int activeSubstitutes = (int) allSubstitutes.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .count();
        int totalAssignments = assignments.size();
        double totalHours = assignments.stream()
                .filter(a -> a.getTotalHours() != null)
                .mapToDouble(SubstituteAssignment::getTotalHours)
                .sum();
        double avgAssignmentsPerSub = totalSubstitutes > 0 ? (double) totalAssignments / totalSubstitutes : 0.0;
        double avgHoursPerSub = totalSubstitutes > 0 ? totalHours / totalSubstitutes : 0.0;

        SubstituteUsageReport.UsageSummary summary = new SubstituteUsageReport.UsageSummary(
                totalSubstitutes,
                activeSubstitutes,
                totalAssignments,
                totalHours,
                avgAssignmentsPerSub,
                avgHoursPerSub
        );

        logger.info("Usage report generated: {} substitutes, {} assignments", totalSubstitutes, totalAssignments);

        return new SubstituteUsageReport(rows, summary);
    }

    /**
     * Generate assignments by date range report
     */
    public SubstituteReport generateAssignmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating assignments report from {} to {}", startDate, endDate);

        List<SubstituteAssignment> assignments = assignmentRepository.findByAssignmentDateBetween(startDate, endDate);

        // Build report rows
        List<SubstituteReport.ReportRow> rows = assignments.stream()
                .map(this::convertAssignmentToRow)
                .collect(Collectors.toList());

        // Calculate summary
        SubstituteReport.ReportSummary summary = calculateSummary(assignments);

        SubstituteReport report = new SubstituteReport();
        report.setReportType("Assignments by Date Range");
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedDate(LocalDate.now());
        report.setGeneratedBy("System");
        report.setSummary(summary);
        report.setRows(rows);

        logger.info("Assignments report generated: {} records", rows.size());

        return report;
    }

    /**
     * Generate substitute hours report
     */
    public SubstituteReport generateHoursReport(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating hours report from {} to {}", startDate, endDate);

        List<SubstituteAssignment> assignments = assignmentRepository.findByAssignmentDateBetween(startDate, endDate)
                .stream()
                .filter(a -> a.getTotalHours() != null && a.getTotalHours() > 0)
                .collect(Collectors.toList());

        // Build report rows sorted by hours descending
        List<SubstituteReport.ReportRow> rows = assignments.stream()
                .map(this::convertAssignmentToRow)
                .sorted((a, b) -> Double.compare(b.getHours(), a.getHours()))
                .collect(Collectors.toList());

        // Calculate summary
        SubstituteReport.ReportSummary summary = calculateSummary(assignments);

        SubstituteReport report = new SubstituteReport();
        report.setReportType("Substitute Hours Report");
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedDate(LocalDate.now());
        report.setGeneratedBy("System");
        report.setSummary(summary);
        report.setRows(rows);

        logger.info("Hours report generated: {} records, {} total hours", rows.size(), summary.getTotalHours());

        return report;
    }

    /**
     * Generate absence reasons report
     */
    public AbsenceReasonsReport generateAbsenceReasonsReport(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating absence reasons report from {} to {}", startDate, endDate);

        List<SubstituteAssignment> assignments = assignmentRepository.findByAssignmentDateBetween(startDate, endDate)
                .stream()
                .filter(a -> a.getAbsenceReason() != null)
                .collect(Collectors.toList());

        // Group by absence reason
        Map<AbsenceReason, List<SubstituteAssignment>> byReason = assignments.stream()
                .collect(Collectors.groupingBy(SubstituteAssignment::getAbsenceReason));

        int totalAssignments = assignments.size();

        // Build reason rows
        List<AbsenceReasonsReport.AbsenceReasonRow> rows = byReason.entrySet().stream()
                .map(entry -> {
                    AbsenceReason reason = entry.getKey();
                    List<SubstituteAssignment> reasonAssignments = entry.getValue();

                    int count = reasonAssignments.size();
                    double percentage = totalAssignments > 0 ? (count * 100.0) / totalAssignments : 0.0;

                    double totalHours = reasonAssignments.stream()
                            .filter(a -> a.getTotalHours() != null)
                            .mapToDouble(SubstituteAssignment::getTotalHours)
                            .sum();

                    double avgHours = count > 0 ? totalHours / count : 0.0;

                    return new AbsenceReasonsReport.AbsenceReasonRow(
                            reason.getDisplayName(),
                            count,
                            percentage,
                            totalHours,
                            avgHours
                    );
                })
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        // Calculate summary
        String mostCommon = rows.isEmpty() ? "N/A" : rows.get(0).getReasonName();
        String leastCommon = rows.isEmpty() ? "N/A" : rows.get(rows.size() - 1).getReasonName();
        double totalHours = assignments.stream()
                .filter(a -> a.getTotalHours() != null)
                .mapToDouble(SubstituteAssignment::getTotalHours)
                .sum();

        AbsenceReasonsReport.AbsenceReasonSummary summary = new AbsenceReasonsReport.AbsenceReasonSummary(
                totalAssignments,
                byReason.size(),
                mostCommon,
                leastCommon,
                totalHours
        );

        logger.info("Absence reasons report generated: {} unique reasons", byReason.size());

        return new AbsenceReasonsReport(rows, summary);
    }

    /**
     * Generate cost analysis report
     */
    public SubstituteReport generateCostAnalysisReport(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating cost analysis report from {} to {}", startDate, endDate);

        List<SubstituteAssignment> assignments = assignmentRepository.findByAssignmentDateBetween(startDate, endDate)
                .stream()
                .filter(a -> a.getPayAmount() != null && a.getPayAmount() > 0)
                .collect(Collectors.toList());

        // Build report rows sorted by cost descending
        List<SubstituteReport.ReportRow> rows = assignments.stream()
                .map(this::convertAssignmentToRow)
                .sorted((a, b) -> Double.compare(b.getCost(), a.getCost()))
                .collect(Collectors.toList());

        // Calculate summary
        SubstituteReport.ReportSummary summary = calculateSummary(assignments);

        SubstituteReport report = new SubstituteReport();
        report.setReportType("Cost Analysis Report");
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedDate(LocalDate.now());
        report.setGeneratedBy("System");
        report.setSummary(summary);
        report.setRows(rows);

        logger.info("Cost analysis report generated: {} records, ${} total cost",
                rows.size(), summary.getTotalCost());

        return report;
    }

    /**
     * Convert assignment to report row
     */
    private SubstituteReport.ReportRow convertAssignmentToRow(SubstituteAssignment assignment) {
        SubstituteReport.ReportRow row = new SubstituteReport.ReportRow();

        row.setDate(assignment.getAssignmentDate());
        row.setSubstituteName(assignment.getSubstitute() != null ?
                assignment.getSubstitute().getFullName() : "Unknown");
        row.setSubstituteType(assignment.getSubstitute() != null && assignment.getSubstitute().getType() != null ?
                assignment.getSubstitute().getType().getDisplayName() : "Unknown");
        row.setReplacedTeacherName(assignment.getReplacedTeacher() != null ?
                assignment.getReplacedTeacher().getName() : "Unknown");
        row.setAbsenceReason(assignment.getAbsenceReason() != null ?
                assignment.getAbsenceReason().getDisplayName() : "N/A");
        row.setDuration(assignment.getDurationType() != null ?
                assignment.getDurationType().getDisplayName() : "N/A");
        row.setHours(assignment.getTotalHours() != null ? assignment.getTotalHours() : 0.0);
        row.setCost(assignment.getPayAmount() != null ? assignment.getPayAmount() : 0.0);
        row.setStatus(assignment.getStatus() != null ?
                assignment.getStatus().getDisplayName() : "Unknown");
        row.setRoom(assignment.getRoom() != null ?
                assignment.getRoom().getRoomNumber() : "N/A");
        row.setCourse(assignment.getCourse() != null ?
                assignment.getCourse().getCourseName() : "N/A");

        return row;
    }

    /**
     * Calculate summary statistics
     */
    private SubstituteReport.ReportSummary calculateSummary(List<SubstituteAssignment> assignments) {
        SubstituteReport.ReportSummary summary = new SubstituteReport.ReportSummary();

        summary.setTotalAssignments(assignments.size());

        summary.setTotalSubstitutes((int) assignments.stream()
                .map(SubstituteAssignment::getSubstitute)
                .filter(Objects::nonNull)
                .distinct()
                .count());

        summary.setTotalHours(assignments.stream()
                .filter(a -> a.getTotalHours() != null)
                .mapToDouble(SubstituteAssignment::getTotalHours)
                .sum());

        summary.setTotalCost(assignments.stream()
                .filter(a -> a.getPayAmount() != null)
                .mapToDouble(SubstituteAssignment::getPayAmount)
                .sum());

        summary.setConfirmedAssignments((int) assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED ||
                           a.getStatus() == AssignmentStatus.IN_PROGRESS ||
                           a.getStatus() == AssignmentStatus.COMPLETED)
                .count());

        summary.setPendingAssignments((int) assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                .count());

        summary.setCancelledAssignments((int) assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.CANCELLED)
                .count());

        return summary;
    }

    /**
     * Export report to CSV format
     */
    public String exportToCSV(SubstituteReport report) {
        logger.info("Exporting report to CSV: {}", report.getReportType());

        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("# ").append(report.getReportType()).append("\n");
        csv.append("# Date Range: ").append(report.getStartDate())
           .append(" to ").append(report.getEndDate()).append("\n");
        csv.append("# Generated: ").append(report.getGeneratedDate()).append("\n");
        csv.append("\n");

        // Summary
        SubstituteReport.ReportSummary summary = report.getSummary();
        csv.append("Summary\n");
        csv.append("Total Assignments,").append(summary.getTotalAssignments()).append("\n");
        csv.append("Total Substitutes,").append(summary.getTotalSubstitutes()).append("\n");
        csv.append("Total Hours,").append(String.format("%.2f", summary.getTotalHours())).append("\n");
        csv.append("Total Cost,$").append(String.format("%.2f", summary.getTotalCost())).append("\n");
        csv.append("Confirmed,").append(summary.getConfirmedAssignments()).append("\n");
        csv.append("Pending,").append(summary.getPendingAssignments()).append("\n");
        csv.append("Cancelled,").append(summary.getCancelledAssignments()).append("\n");
        csv.append("\n");

        // Column headers
        csv.append("Date,Substitute,Type,Replaced Teacher,Absence Reason,Duration,Hours,Cost,Status,Room,Course\n");

        // Data rows
        for (SubstituteReport.ReportRow row : report.getRows()) {
            csv.append(row.getDate()).append(",");
            csv.append(escapeCSV(row.getSubstituteName())).append(",");
            csv.append(escapeCSV(row.getSubstituteType())).append(",");
            csv.append(escapeCSV(row.getReplacedTeacherName())).append(",");
            csv.append(escapeCSV(row.getAbsenceReason())).append(",");
            csv.append(escapeCSV(row.getDuration())).append(",");
            csv.append(String.format("%.2f", row.getHours())).append(",");
            csv.append(String.format("%.2f", row.getCost())).append(",");
            csv.append(escapeCSV(row.getStatus())).append(",");
            csv.append(escapeCSV(row.getRoom())).append(",");
            csv.append(escapeCSV(row.getCourse())).append("\n");
        }

        logger.info("CSV export completed: {} rows", report.getRows().size());

        return csv.toString();
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
