package com.heronix.ui.controller;

import com.heronix.service.AssignmentReportService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;

/**
 * Assignment Reports Controller
 *
 * Displays comprehensive analytics and reports for the course assignment system.
 * Provides administrators with insights into assignment effectiveness, demand patterns,
 * and areas requiring attention.
 *
 * Report Types:
 * - Course Demand Analysis
 * - Preference Satisfaction
 * - Waitlist Analysis
 * - Capacity Utilization
 * - Student Completion
 * - Assignment Statistics
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 6 - November 20, 2025
 */
@Component
public class AssignmentReportsController {

    @Autowired
    private AssignmentReportService reportService;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    @FXML
    private ComboBox<String> reportTypeCombo;

    @FXML
    private TextArea reportTextArea;

    @FXML
    private Button generateButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button closeButton;

    // Summary Cards
    @FXML
    private Label summaryCard1Label;

    @FXML
    private Label summaryCard1Value;

    @FXML
    private Label summaryCard2Label;

    @FXML
    private Label summaryCard2Value;

    @FXML
    private Label summaryCard3Label;

    @FXML
    private Label summaryCard3Value;

    @FXML
    private Label summaryCard4Label;

    @FXML
    private Label summaryCard4Value;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        setupReportTypes();
        loadAssignmentStatistics();
    }

    /**
     * Setup report type combo box
     */
    private void setupReportTypes() {
        reportTypeCombo.getItems().addAll(
            "Course Demand Analysis",
            "Preference Satisfaction",
            "Waitlist Analysis",
            "Capacity Utilization",
            "Student Completion",
            "Assignment Statistics"
        );
        reportTypeCombo.getSelectionModel().select(0);
    }

    /**
     * Load overall assignment statistics for summary cards
     */
    private void loadAssignmentStatistics() {
        try {
            AssignmentReportService.AssignmentStatistics stats = reportService.getAssignmentStatistics();

            summaryCard1Label.setText("Total Requests");
            summaryCard1Value.setText(String.valueOf(stats.getTotalRequests()));

            summaryCard2Label.setText("Success Rate");
            summaryCard2Value.setText(String.format("%.1f%%", stats.getSuccessRate()));

            summaryCard3Label.setText("Approved");
            summaryCard3Value.setText(String.valueOf(stats.getApprovedRequests()));

            summaryCard4Label.setText("Waitlisted");
            summaryCard4Value.setText(String.valueOf(stats.getWaitlistedRequests()));

            // Color code success rate
            if (stats.getSuccessRate() >= 90) {
                summaryCard2Value.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else if (stats.getSuccessRate() >= 75) {
                summaryCard2Value.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            } else {
                summaryCard2Value.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }

        } catch (Exception e) {
            showError("Failed to load statistics: " + e.getMessage());
        }
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    /**
     * Handle Generate Report button
     */
    @FXML
    private void handleGenerateReport() {
        String selectedReport = reportTypeCombo.getSelectionModel().getSelectedItem();
        if (selectedReport == null) {
            showError("Please select a report type");
            return;
        }

        try {
            String reportContent = generateReport(selectedReport);
            reportTextArea.setText(reportContent);
            exportButton.setDisable(false);
        } catch (Exception e) {
            showError("Failed to generate report: " + e.getMessage());
        }
    }

    /**
     * Generate report based on selected type
     */
    private String generateReport(String reportType) {
        StringBuilder report = new StringBuilder();

        switch (reportType) {
            case "Course Demand Analysis":
                report.append("COURSE DEMAND ANALYSIS\n");
                report.append("=".repeat(80)).append("\n\n");

                var demandReports = reportService.getCourseDemandAnalysis();
                if (demandReports.isEmpty()) {
                    report.append("No course demand data available.\n");
                } else {
                    report.append(String.format("%-40s %-10s %-10s %-10s %-15s\n",
                        "Course", "Requests", "Capacity", "Ratio", "Demand Level"));
                    report.append("-".repeat(80)).append("\n");

                    for (var dr : demandReports) {
                        report.append(String.format("%-40s %-10d %-10d %-10.2f %-15s\n",
                            dr.getCourseCode() + " - " + truncate(dr.getCourseName(), 30),
                            dr.getTotalRequests(),
                            dr.getMaxCapacity(),
                            dr.getDemandRatio(),
                            dr.getDemandLevel()));
                    }
                }
                break;

            case "Preference Satisfaction":
                report.append("PREFERENCE SATISFACTION REPORT\n");
                report.append("=".repeat(80)).append("\n\n");

                var prefReport = reportService.getPreferenceSatisfactionReport();
                report.append(String.format("Total Approved Requests: %d\n\n", prefReport.getApprovedRequests()));
                report.append(String.format("1st Choice: %d (%.1f%%)\n",
                    prefReport.getFirstChoiceCount(),
                    prefReport.getFirstChoicePercent()));
                report.append(String.format("2nd Choice: %d (%.1f%%)\n",
                    prefReport.getSecondChoiceCount(),
                    prefReport.getSecondChoicePercent()));
                report.append(String.format("3rd Choice: %d (%.1f%%)\n",
                    prefReport.getThirdChoiceCount(),
                    prefReport.getThirdChoicePercent()));
                break;

            case "Waitlist Analysis":
                report.append("WAITLIST ANALYSIS\n");
                report.append("=".repeat(80)).append("\n\n");

                var waitlistReports = reportService.getWaitlistAnalysis();
                if (waitlistReports.isEmpty()) {
                    report.append("No active waitlists.\n");
                } else {
                    report.append(String.format("%-40s %-10s %-15s %-10s\n",
                        "Course", "Waitlist", "Avg Priority", "Critical"));
                    report.append("-".repeat(80)).append("\n");

                    for (var wr : waitlistReports) {
                        report.append(String.format("%-40s %-10d %-15.1f %-10s\n",
                            wr.getCourseCode() + " - " + truncate(wr.getCourseName(), 30),
                            wr.getWaitlistSize(),
                            wr.getAveragePriority(),
                            wr.isCritical() ? "Yes" : "No"));
                    }
                }
                break;

            case "Capacity Utilization":
                report.append("CAPACITY UTILIZATION REPORT\n");
                report.append("=".repeat(80)).append("\n\n");

                var capReport = reportService.getCapacityUtilization();
                report.append(String.format("Total Courses: %d\n\n", capReport.getTotalCourses()));
                report.append(String.format("Over Capacity:  %d courses\n", capReport.getOverCapacity()));
                report.append(String.format("At Capacity:    %d courses\n", capReport.getAtCapacity()));
                report.append(String.format("Optimal:        %d courses\n", capReport.getOptimalCapacity()));
                report.append(String.format("Good:           %d courses\n", capReport.getGoodCapacity()));
                report.append(String.format("Under Capacity: %d courses\n\n", capReport.getUnderCapacity()));
                report.append(String.format("Overall Utilization: %.1f%%\n", capReport.getOverallUtilizationPercent()));
                break;

            case "Student Completion":
                report.append("STUDENT COMPLETION ANALYSIS\n");
                report.append("=".repeat(80)).append("\n\n");

                var compReport = reportService.getStudentCompletionAnalysis();
                report.append(String.format("Total Students with Requests: %d\n\n", compReport.getTotalStudents()));
                report.append(String.format("Complete Schedules (7 courses): %d (%.1f%%)\n",
                    compReport.getCompleteSchedules(),
                    compReport.getCompletionRate()));
                report.append(String.format("Partial Schedules (<7 courses): %d\n",
                    compReport.getPartialSchedules()));
                break;

            case "Assignment Statistics":
                report.append("ASSIGNMENT STATISTICS\n");
                report.append("=".repeat(80)).append("\n\n");

                var stats = reportService.getAssignmentStatistics();
                report.append(String.format("Total Requests: %d\n\n", stats.getTotalRequests()));
                report.append(String.format("Pending:    %d\n", stats.getPendingRequests()));
                report.append(String.format("Approved:   %d\n", stats.getApprovedRequests()));
                report.append(String.format("Waitlisted: %d\n", stats.getWaitlistedRequests()));
                report.append(String.format("Denied:     %d\n\n", stats.getDeniedRequests()));
                report.append(String.format("Success Rate: %.1f%%\n\n", stats.getSuccessRate()));
                report.append(String.format("Average Priority (Approved):   %.1f\n", stats.getAvgPriorityApproved()));
                report.append(String.format("Average Priority (Waitlisted): %.1f\n", stats.getAvgPriorityWaitlisted()));
                report.append(String.format("Average Priority (Denied):     %.1f\n", stats.getAvgPriorityDenied()));
                break;

            default:
                report.append("Unknown report type\n");
        }

        return report.toString();
    }

    /**
     * Handle Export button
     */
    @FXML
    private void handleExport() {
        String content = reportTextArea.getText();
        if (content == null || content.trim().isEmpty()) {
            showError("No report to export. Please generate a report first.");
            return;
        }

        String selectedReport = reportTypeCombo.getSelectionModel().getSelectedItem();
        String fileName = (selectedReport != null ? selectedReport.replace(" ", "_") : "assignment_report")
            + "_" + LocalDate.now() + ".txt";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Assignment Report");
        fileChooser.setInitialFileName(fileName);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Assignment Report: " + (selectedReport != null ? selectedReport : "General"));
            writer.println("Generated: " + LocalDate.now());
            writer.println("================================================================================");
            writer.println();
            writer.print(content);

            showInfo("Report exported successfully to:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Failed to export report: " + e.getMessage());
        }
    }

    /**
     * Handle Close button
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Truncate string to max length
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    /**
     * Show info message
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
