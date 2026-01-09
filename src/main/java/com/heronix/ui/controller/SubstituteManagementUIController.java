package com.heronix.ui.controller;

import com.heronix.model.domain.Substitute;
import com.heronix.model.domain.SubstituteAssignment;
import com.heronix.model.enums.AssignmentStatus;
import com.heronix.model.enums.SubstituteType;
import com.heronix.service.SubstituteManagementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ScrollPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Substitute Management main UI
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Controller
public class SubstituteManagementUIController {

    private static final Logger logger = LoggerFactory.getLogger(SubstituteManagementUIController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    @Autowired
    private SubstituteManagementService substituteManagementService;

    @Autowired
    private com.heronix.service.SubstituteReportService substituteReportService;

    @Autowired
    private ApplicationContext applicationContext;

    // Main Tab Pane
    @FXML private TabPane mainTabPane;

    // ==================== ASSIGNMENTS TAB ====================

    // Filters
    @FXML private DatePicker filterStartDatePicker;
    @FXML private DatePicker filterEndDatePicker;
    @FXML private ComboBox<AssignmentStatus> filterStatusComboBox;
    @FXML private ComboBox<Substitute> filterSubstituteComboBox;

    // Assignments Table
    @FXML private TableView<SubstituteAssignment> assignmentsTable;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentDateColumn;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentSubstituteColumn;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentTimeColumn;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentDurationColumn;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentReplacedStaffColumn;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentReasonColumn;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentRoomColumn;
    @FXML private TableColumn<SubstituteAssignment, String> assignmentStatusColumn;
    @FXML private TableColumn<SubstituteAssignment, Void> assignmentActionsColumn;
    @FXML private Label assignmentCountLabel;

    // ==================== SUBSTITUTES TAB ====================

    // Substitute Search
    @FXML private TextField substituteSearchField;

    // Substitutes Table
    @FXML private TableView<Substitute> substitutesTable;
    @FXML private TableColumn<Substitute, String> subNameColumn;
    @FXML private TableColumn<Substitute, String> subEmployeeIdColumn;
    @FXML private TableColumn<Substitute, String> subTypeColumn;
    @FXML private TableColumn<Substitute, String> subEmailColumn;
    @FXML private TableColumn<Substitute, String> subPhoneColumn;
    @FXML private TableColumn<Substitute, String> subCertificationsColumn;
    @FXML private TableColumn<Substitute, String> subStatusColumn;
    @FXML private TableColumn<Substitute, Void> subActionsColumn;
    @FXML private Label substituteCountLabel;

    // ==================== REPORTS TAB ====================

    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private DatePicker reportStartDatePicker;
    @FXML private DatePicker reportEndDatePicker;

    // Statistics
    @FXML private Label statsThisWeekLabel;
    @FXML private Label statsThisMonthLabel;
    @FXML private Label statsActiveSubsLabel;
    @FXML private Label statsTotalHoursLabel;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Substitute Management UI Controller");

        setupAssignmentsTab();
        setupSubstitutesTab();
        setupReportsTab();

        loadData();
    }

    /**
     * Setup Assignments Tab
     */
    private void setupAssignmentsTab() {
        // Setup table columns
        assignmentDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getAssignmentDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getAssignmentDate().format(DATE_FORMATTER));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        assignmentSubstituteColumn.setCellValueFactory(cellData -> {
            SubstituteAssignment assignment = cellData.getValue();
            if (assignment != null && assignment.getSubstitute() != null) {
                try {
                    return new javafx.beans.property.SimpleStringProperty(assignment.getSubstitute().getFullName());
                } catch (Exception e) {
                    logger.warn("Could not get substitute name for assignment", e);
                    return new javafx.beans.property.SimpleStringProperty("N/A");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        assignmentTimeColumn.setCellValueFactory(cellData -> {
            String start = cellData.getValue().getStartTime() != null ?
                    cellData.getValue().getStartTime().format(TIME_FORMATTER) : "";
            String end = cellData.getValue().getEndTime() != null ?
                    cellData.getValue().getEndTime().format(TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(start + " - " + end);
        });

        assignmentDurationColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDurationType() != null ?
                                cellData.getValue().getDurationType().getDisplayName() : ""));

        assignmentReplacedStaffColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReplacedPersonName()));

        assignmentReasonColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getAbsenceReason() != null ?
                                cellData.getValue().getAbsenceReason().getDisplayName() : ""));

        assignmentRoomColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getRoom() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRoom().getRoomNumber());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        assignmentStatusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStatus() != null ?
                                cellData.getValue().getStatus().getDisplayName() : ""));

        // Add actions column
        addAssignmentActionsColumn();

        // Setup filter combo boxes
        filterStatusComboBox.setItems(FXCollections.observableArrayList(AssignmentStatus.values()));
        filterStatusComboBox.setConverter(createEnumConverter());

        // Set default date range (current month)
        filterStartDatePicker.setValue(YearMonth.now().atDay(1));
        filterEndDatePicker.setValue(YearMonth.now().atEndOfMonth());
    }

    /**
     * Setup Substitutes Tab
     */
    private void setupSubstitutesTab() {
        subNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));

        subEmployeeIdColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        subTypeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getType() != null ?
                                cellData.getValue().getType().getDisplayName() : ""));

        subEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        subPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));

        subCertificationsColumn.setCellValueFactory(cellData -> {
            Substitute substitute = cellData.getValue();
            if (substitute != null && substitute.getCertifications() != null && !substitute.getCertifications().isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty(String.join(", ", substitute.getCertifications()));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        subStatusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getActive() ? "Active" : "Inactive"));

        // Add actions column
        addSubstituteActionsColumn();
    }

    /**
     * Setup Reports Tab
     */
    private void setupReportsTab() {
        // Setup report types
        reportTypeComboBox.setItems(FXCollections.observableArrayList(
                "Substitute Usage Summary",
                "Assignments by Date Range",
                "Substitute Hours Report",
                "Absence Reasons Report",
                "Cost Analysis Report"
        ));

        // Set default dates
        reportStartDatePicker.setValue(YearMonth.now().atDay(1));
        reportEndDatePicker.setValue(YearMonth.now().atEndOfMonth());
    }

    /**
     * Add actions column to assignments table
     */
    private void addAssignmentActionsColumn() {
        assignmentActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");

                editButton.setOnAction(event -> {
                    SubstituteAssignment assignment = getTableView().getItems().get(getIndex());
                    handleEditAssignment(assignment);
                });

                deleteButton.setOnAction(event -> {
                    SubstituteAssignment assignment = getTableView().getItems().get(getIndex());
                    handleDeleteAssignment(assignment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    /**
     * Add actions column to substitutes table
     */
    private void addSubstituteActionsColumn() {
        subActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button viewButton = new Button("View");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");
                viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");

                editButton.setOnAction(event -> {
                    Substitute substitute = getTableView().getItems().get(getIndex());
                    handleEditSubstitute(substitute);
                });

                viewButton.setOnAction(event -> {
                    Substitute substitute = getTableView().getItems().get(getIndex());
                    handleViewSubstitute(substitute);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editButton, viewButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    /**
     * Create enum string converter
     */
    private <T extends Enum<?>> StringConverter<T> createEnumConverter() {
        return new StringConverter<T>() {
            @Override
            public String toString(T value) {
                if (value == null) return "";
                try {
                    return (String) value.getClass().getMethod("getDisplayName").invoke(value);
                } catch (Exception e) {
                    return value.toString();
                }
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    /**
     * Load all data
     */
    private void loadData() {
        loadAssignments();
        loadSubstitutes();
        loadStatistics();
    }

    /**
     * Load assignments
     */
    private void loadAssignments() {
        LocalDate startDate = filterStartDatePicker.getValue();
        LocalDate endDate = filterEndDatePicker.getValue();

        List<SubstituteAssignment> assignments;

        if (startDate != null && endDate != null) {
            assignments = substituteManagementService.getAssignmentsBetweenDates(startDate, endDate);
        } else {
            assignments = substituteManagementService.getAllAssignments();
        }

        // Apply filters
        if (filterStatusComboBox.getValue() != null) {
            assignments = assignments.stream()
                    .filter(a -> a.getStatus() == filterStatusComboBox.getValue())
                    .collect(Collectors.toList());
        }

        if (filterSubstituteComboBox.getValue() != null) {
            Substitute filterSub = filterSubstituteComboBox.getValue();
            assignments = assignments.stream()
                    .filter(a -> a.getSubstitute() != null && a.getSubstitute().getId().equals(filterSub.getId()))
                    .collect(Collectors.toList());
        }

        assignmentsTable.setItems(FXCollections.observableArrayList(assignments));
        assignmentCountLabel.setText("Total: " + assignments.size() + " assignments");

        logger.info("Loaded {} assignments", assignments.size());
    }

    /**
     * Load substitutes
     */
    private void loadSubstitutes() {
        List<Substitute> substitutes = substituteManagementService.getAllSubstitutes();
        substitutesTable.setItems(FXCollections.observableArrayList(substitutes));
        substituteCountLabel.setText("Total: " + substitutes.size() + " substitutes");

        // Update filter combo box
        filterSubstituteComboBox.setItems(FXCollections.observableArrayList(substitutes));
        filterSubstituteComboBox.setConverter(new StringConverter<Substitute>() {
            @Override
            public String toString(Substitute sub) {
                return sub != null ? sub.getFullName() : "";
            }

            @Override
            public Substitute fromString(String string) {
                return null;
            }
        });

        logger.info("Loaded {} substitutes", substitutes.size());
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(java.time.DayOfWeek.SUNDAY);
        YearMonth currentMonth = YearMonth.now();

        // This week
        long thisWeek = substituteManagementService.getAssignmentsBetweenDates(weekStart, weekEnd).size();
        statsThisWeekLabel.setText(String.valueOf(thisWeek));

        // This month
        long thisMonth = substituteManagementService.getAssignmentsBetweenDates(
                currentMonth.atDay(1), currentMonth.atEndOfMonth()).size();
        statsThisMonthLabel.setText(String.valueOf(thisMonth));

        // Active substitutes
        long activeSubs = substituteManagementService.countActiveSubstitutes();
        statsActiveSubsLabel.setText(String.valueOf(activeSubs));

        // Total hours this month
        List<SubstituteAssignment> monthAssignments = substituteManagementService.getAssignmentsBetweenDates(
                currentMonth.atDay(1), currentMonth.atEndOfMonth());
        double totalHours = monthAssignments.stream()
                .mapToDouble(a -> a.getTotalHours() != null ? a.getTotalHours() : 0.0)
                .sum();
        statsTotalHoursLabel.setText(String.format("%.1f", totalHours));
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Handle new assignment button
     */
    @FXML
    private void handleNewAssignment() {
        openAssignmentForm(null);
    }

    /**
     * Handle import Frontline button
     */
    @FXML
    private void handleImportFrontline() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontlineImport.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Import from Frontline");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh after import
            loadAssignments();
            loadSubstitutes();
            loadStatistics();

        } catch (Exception e) {
            logger.error("Error opening Frontline import dialog", e);
            showError("Error", "Could not open import dialog: " + e.getMessage());
        }
    }

    /**
     * Handle search assignments
     */
    @FXML
    private void handleSearchAssignments() {
        loadAssignments();
    }

    /**
     * Handle clear filters
     */
    @FXML
    private void handleClearFilters() {
        filterStartDatePicker.setValue(YearMonth.now().atDay(1));
        filterEndDatePicker.setValue(YearMonth.now().atEndOfMonth());
        filterStatusComboBox.setValue(null);
        filterSubstituteComboBox.setValue(null);
        loadAssignments();
    }

    /**
     * Handle refresh assignments
     */
    @FXML
    private void handleRefreshAssignments() {
        loadAssignments();
        loadStatistics();
    }

    /**
     * Handle search substitutes
     */
    @FXML
    private void handleSearchSubstitutes() {
        String query = substituteSearchField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadSubstitutes();
            return;
        }

        List<Substitute> results = substituteManagementService.searchSubstitutesByName(query.trim());
        substitutesTable.setItems(FXCollections.observableArrayList(results));
        substituteCountLabel.setText("Found: " + results.size() + " substitutes");
    }

    /**
     * Handle add substitute
     */
    @FXML
    private void handleAddSubstitute() {
        Dialog<Substitute> dialog = createSubstituteDialog(null);
        Optional<Substitute> result = dialog.showAndWait();

        result.ifPresent(substitute -> {
            try {
                Substitute saved = substituteManagementService.saveSubstitute(substitute);
                showInfo("Success", "Substitute " + saved.getFirstName() + " " + saved.getLastName() + " added successfully!");
                loadSubstitutes();
            } catch (Exception e) {
                logger.error("Error adding substitute", e);
                showError("Error", "Failed to add substitute: " + e.getMessage());
            }
        });
    }

    /**
     * Handle refresh substitutes
     */
    @FXML
    private void handleRefreshSubstitutes() {
        substituteSearchField.clear();
        loadSubstitutes();
    }

    /**
     * Handle generate report
     */
    @FXML
    private void handleGenerateReport() {
        try {
            String reportType = reportTypeComboBox.getValue();
            LocalDate startDate = reportStartDatePicker.getValue();
            LocalDate endDate = reportEndDatePicker.getValue();

            // Validation
            if (reportType == null || reportType.isEmpty()) {
                showError("Validation Error", "Please select a report type");
                return;
            }

            if (startDate == null || endDate == null) {
                showError("Validation Error", "Please select start and end dates");
                return;
            }

            if (startDate.isAfter(endDate)) {
                showError("Validation Error", "Start date must be before end date");
                return;
            }

            logger.info("Generating report: {} from {} to {}", reportType, startDate, endDate);

            // Generate report based on type
            String csvContent = null;
            String reportTitle = reportType;

            switch (reportType) {
                case "Substitute Usage Summary":
                    com.heronix.model.dto.SubstituteUsageReport usageReport =
                            substituteReportService.generateUsageSummary(startDate, endDate);
                    csvContent = exportUsageReportToCSV(usageReport, startDate, endDate);
                    break;

                case "Assignments by Date Range":
                    com.heronix.model.dto.SubstituteReport assignmentsReport =
                            substituteReportService.generateAssignmentsByDateRange(startDate, endDate);
                    csvContent = substituteReportService.exportToCSV(assignmentsReport);
                    break;

                case "Substitute Hours Report":
                    com.heronix.model.dto.SubstituteReport hoursReport =
                            substituteReportService.generateHoursReport(startDate, endDate);
                    csvContent = substituteReportService.exportToCSV(hoursReport);
                    break;

                case "Absence Reasons Report":
                    com.heronix.model.dto.AbsenceReasonsReport reasonsReport =
                            substituteReportService.generateAbsenceReasonsReport(startDate, endDate);
                    csvContent = exportAbsenceReasonsToCSV(reasonsReport, startDate, endDate);
                    break;

                case "Cost Analysis Report":
                    com.heronix.model.dto.SubstituteReport costReport =
                            substituteReportService.generateCostAnalysisReport(startDate, endDate);
                    csvContent = substituteReportService.exportToCSV(costReport);
                    break;

                default:
                    showError("Error", "Unknown report type: " + reportType);
                    return;
            }

            // Save CSV file
            if (csvContent != null) {
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Save Report");
                fileChooser.setInitialFileName(reportType.replace(" ", "_") + "_" +
                        startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "_to_" +
                        endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv");
                fileChooser.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

                java.io.File file = fileChooser.showSaveDialog(reportTypeComboBox.getScene().getWindow());

                if (file != null) {
                    java.nio.file.Files.writeString(file.toPath(), csvContent);
                    showInfo("Success", "Report saved successfully to:\n" + file.getAbsolutePath());
                    logger.info("Report saved to: {}", file.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            logger.error("Error generating report", e);
            showError("Error", "Failed to generate report: " + e.getMessage());
        }
    }

    /**
     * Export usage report to CSV
     */
    private String exportUsageReportToCSV(com.heronix.model.dto.SubstituteUsageReport report,
                                          LocalDate startDate, LocalDate endDate) {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("# Substitute Usage Summary Report\n");
        csv.append("# Date Range: ").append(startDate).append(" to ").append(endDate).append("\n");
        csv.append("# Generated: ").append(LocalDate.now()).append("\n");
        csv.append("\n");

        // Summary
        com.heronix.model.dto.SubstituteUsageReport.UsageSummary summary = report.getSummary();
        csv.append("Summary\n");
        csv.append("Total Substitutes,").append(summary.getTotalSubstitutes()).append("\n");
        csv.append("Active Substitutes,").append(summary.getActiveSubstitutes()).append("\n");
        csv.append("Total Assignments,").append(summary.getTotalAssignments()).append("\n");
        csv.append("Total Hours,").append(String.format("%.2f", summary.getTotalHours())).append("\n");
        csv.append("Avg Assignments/Sub,").append(String.format("%.2f", summary.getAverageAssignmentsPerSubstitute())).append("\n");
        csv.append("Avg Hours/Sub,").append(String.format("%.2f", summary.getAverageHoursPerSubstitute())).append("\n");
        csv.append("\n");

        // Column headers
        csv.append("Substitute,Type,Email,Phone,Total Assignments,Total Hours,Avg Hours/Assignment,");
        csv.append("Confirmed,Pending,Cancelled,Most Common Reason\n");

        // Data rows
        for (com.heronix.model.dto.SubstituteUsageReport.SubstituteUsageRow row : report.getSubstitutes()) {
            csv.append(escapeCSV(row.getSubstituteName())).append(",");
            csv.append(escapeCSV(row.getSubstituteType())).append(",");
            csv.append(escapeCSV(row.getEmail())).append(",");
            csv.append(escapeCSV(row.getPhone())).append(",");
            csv.append(row.getTotalAssignments()).append(",");
            csv.append(String.format("%.2f", row.getTotalHours())).append(",");
            csv.append(String.format("%.2f", row.getAverageHoursPerAssignment())).append(",");
            csv.append(row.getConfirmedAssignments()).append(",");
            csv.append(row.getPendingAssignments()).append(",");
            csv.append(row.getCancelledAssignments()).append(",");
            csv.append(escapeCSV(row.getMostCommonAbsenceReason())).append("\n");
        }

        return csv.toString();
    }

    /**
     * Export absence reasons report to CSV
     */
    private String exportAbsenceReasonsToCSV(com.heronix.model.dto.AbsenceReasonsReport report,
                                             LocalDate startDate, LocalDate endDate) {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("# Absence Reasons Report\n");
        csv.append("# Date Range: ").append(startDate).append(" to ").append(endDate).append("\n");
        csv.append("# Generated: ").append(LocalDate.now()).append("\n");
        csv.append("\n");

        // Summary
        com.heronix.model.dto.AbsenceReasonsReport.AbsenceReasonSummary summary = report.getSummary();
        csv.append("Summary\n");
        csv.append("Total Assignments,").append(summary.getTotalAssignments()).append("\n");
        csv.append("Unique Reasons,").append(summary.getUniqueReasons()).append("\n");
        csv.append("Most Common Reason,").append(summary.getMostCommonReason()).append("\n");
        csv.append("Least Common Reason,").append(summary.getLeastCommonReason()).append("\n");
        csv.append("Total Hours,").append(String.format("%.2f", summary.getTotalHours())).append("\n");
        csv.append("\n");

        // Column headers
        csv.append("Reason,Count,Percentage,Total Hours,Avg Hours/Assignment\n");

        // Data rows
        for (com.heronix.model.dto.AbsenceReasonsReport.AbsenceReasonRow row : report.getReasons()) {
            csv.append(escapeCSV(row.getReasonName())).append(",");
            csv.append(row.getCount()).append(",");
            csv.append(String.format("%.2f%%", row.getPercentage())).append(",");
            csv.append(String.format("%.2f", row.getTotalHours())).append(",");
            csv.append(String.format("%.2f", row.getAverageHoursPerAssignment())).append("\n");
        }

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

    /**
     * Handle edit assignment
     */
    private void handleEditAssignment(SubstituteAssignment assignment) {
        openAssignmentForm(assignment);
    }

    /**
     * Handle delete assignment
     */
    private void handleDeleteAssignment(SubstituteAssignment assignment) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Assignment");
        alert.setHeaderText("Delete Assignment");
        alert.setContentText("Are you sure you want to delete this assignment?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            substituteManagementService.deleteAssignment(assignment.getId());
            loadAssignments();
            loadStatistics();
        }
    }

    /**
     * Handle edit substitute
     */
    private void handleEditSubstitute(Substitute substitute) {
        Dialog<Substitute> dialog = createSubstituteDialog(substitute);
        Optional<Substitute> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            try {
                Substitute saved = substituteManagementService.saveSubstitute(updated);
                showInfo("Success", "Substitute " + saved.getFirstName() + " " + saved.getLastName() + " updated successfully!");
                loadSubstitutes();
            } catch (Exception e) {
                logger.error("Error updating substitute", e);
                showError("Error", "Failed to update substitute: " + e.getMessage());
            }
        });
    }

    /**
     * Handle view substitute
     */
    private void handleViewSubstitute(Substitute substitute) {
        showInfo("View Substitute", "Viewing assignments for: " + substitute.getFullName());
    }

    /**
     * Open assignment form dialog
     */
    private void openAssignmentForm(SubstituteAssignment assignment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SubstituteAssignmentForm.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            SubstituteAssignmentController controller = loader.getController();
            if (assignment != null) {
                controller.setAssignment(assignment);
            }
            controller.setOnSaveCallback(() -> {
                loadAssignments();
                loadStatistics();
            });

            Stage stage = new Stage();
            stage.setTitle(assignment == null ? "New Assignment" : "Edit Assignment");

            // Create scene with reasonable size
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Set size constraints to fit on screen
            stage.setWidth(900);  // Fixed width
            stage.setHeight(700); // Fixed height (will fit on most screens)
            stage.setMinWidth(700);
            stage.setMinHeight(600);
            stage.setMaxWidth(1000);
            stage.setMaxHeight(800);

            // Center on screen
            stage.centerOnScreen();

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            logger.error("Error opening assignment form", e);
            showError("Error", "Could not open assignment form: " + e.getMessage());
        }
    }

    /**
     * Show error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Create substitute dialog for add/edit operations
     */
    private Dialog<Substitute> createSubstituteDialog(Substitute existingSub) {
        Dialog<Substitute> dialog = new Dialog<>();
        dialog.setTitle(existingSub == null ? "Add New Substitute" : "Edit Substitute");
        dialog.setHeaderText(existingSub == null ? "Enter substitute information" : "Modify substitute information");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First name");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last name");

        TextField employeeIdField = new TextField();
        employeeIdField.setPromptText("Employee ID (optional)");

        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com");

        TextField phoneField = new TextField();
        phoneField.setPromptText("(555) 123-4567");

        ComboBox<SubstituteType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(SubstituteType.values());
        typeCombo.setPromptText("Select type");

        TextField availabilityField = new TextField();
        availabilityField.setPromptText("e.g., Full-time, Mornings only");

        TextField hourlyRateField = new TextField();
        hourlyRateField.setPromptText("0.00");

        TextField dailyRateField = new TextField();
        dailyRateField.setPromptText("0.00");

        TextArea certificationsArea = new TextArea();
        certificationsArea.setPromptText("Enter certifications (one per line)");
        certificationsArea.setPrefRowCount(3);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes, preferences, restrictions");
        notesArea.setPrefRowCount(3);

        CheckBox activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);

        // Populate if editing
        if (existingSub != null) {
            firstNameField.setText(existingSub.getFirstName());
            lastNameField.setText(existingSub.getLastName());
            employeeIdField.setText(existingSub.getEmployeeId());
            emailField.setText(existingSub.getEmail());
            phoneField.setText(existingSub.getPhoneNumber());
            typeCombo.setValue(existingSub.getType());
            availabilityField.setText(existingSub.getAvailability());
            if (existingSub.getHourlyRate() != null) {
                hourlyRateField.setText(String.valueOf(existingSub.getHourlyRate()));
            }
            if (existingSub.getDailyRate() != null) {
                dailyRateField.setText(String.valueOf(existingSub.getDailyRate()));
            }
            if (existingSub.getCertifications() != null && !existingSub.getCertifications().isEmpty()) {
                certificationsArea.setText(String.join("\n", existingSub.getCertifications()));
            }
            notesArea.setText(existingSub.getNotes());
            activeCheckBox.setSelected(existingSub.getActive());
        }

        // Add fields to grid
        int row = 0;
        grid.add(new Label("First Name: *"), 0, row);
        grid.add(firstNameField, 1, row);
        row++;

        grid.add(new Label("Last Name: *"), 0, row);
        grid.add(lastNameField, 1, row);
        row++;

        grid.add(new Label("Employee ID:"), 0, row);
        grid.add(employeeIdField, 1, row);
        row++;

        grid.add(new Label("Type: *"), 0, row);
        grid.add(typeCombo, 1, row);
        row++;

        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row);
        row++;

        grid.add(new Label("Phone:"), 0, row);
        grid.add(phoneField, 1, row);
        row++;

        grid.add(new Label("Availability:"), 0, row);
        grid.add(availabilityField, 1, row);
        row++;

        grid.add(new Label("Hourly Rate:"), 0, row);
        grid.add(hourlyRateField, 1, row);
        row++;

        grid.add(new Label("Daily Rate:"), 0, row);
        grid.add(dailyRateField, 1, row);
        row++;

        grid.add(new Label("Certifications:"), 0, row);
        grid.add(certificationsArea, 1, row);
        row++;

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row);
        row++;

        grid.add(activeCheckBox, 1, row);

        // Wrap grid in ScrollPane for better layout with large forms
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(600, 500);  // Set reasonable size
        scrollPane.setMaxSize(600, 600);   // Prevent excessive growth

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(650, 550);  // Set dialog size

        // Validation and result converter
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Enable save button when required fields are filled
        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty() || lastNameField.getText().trim().isEmpty() || typeCombo.getValue() == null);
        });
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty() || firstNameField.getText().trim().isEmpty() || typeCombo.getValue() == null);
        });
        typeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue == null || firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty());
        });

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Substitute sub = existingSub != null ? existingSub : new Substitute();

                    sub.setFirstName(firstNameField.getText().trim());
                    sub.setLastName(lastNameField.getText().trim());
                    sub.setEmployeeId(employeeIdField.getText().trim().isEmpty() ? null : employeeIdField.getText().trim());
                    sub.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                    sub.setPhoneNumber(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
                    sub.setType(typeCombo.getValue());
                    sub.setAvailability(availabilityField.getText().trim().isEmpty() ? null : availabilityField.getText().trim());

                    // Parse rates
                    if (!hourlyRateField.getText().trim().isEmpty()) {
                        try {
                            sub.setHourlyRate(Double.parseDouble(hourlyRateField.getText().trim()));
                        } catch (NumberFormatException e) {
                            // Invalid number, skip
                        }
                    }

                    if (!dailyRateField.getText().trim().isEmpty()) {
                        try {
                            sub.setDailyRate(Double.parseDouble(dailyRateField.getText().trim()));
                        } catch (NumberFormatException e) {
                            // Invalid number, skip
                        }
                    }

                    // Parse certifications
                    if (!certificationsArea.getText().trim().isEmpty()) {
                        Set<String> certs = new HashSet<>();
                        for (String line : certificationsArea.getText().split("\n")) {
                            if (!line.trim().isEmpty()) {
                                certs.add(line.trim());
                            }
                        }
                        sub.setCertifications(certs);
                    }

                    sub.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                    sub.setActive(activeCheckBox.isSelected());

                    return sub;
                } catch (Exception e) {
                    logger.error("Error converting dialog result", e);
                    return null;
                }
            }
            return null;
        });

        // Request focus on first field
        javafx.application.Platform.runLater(() -> firstNameField.requestFocus());

        return dialog;
    }
}
