package com.heronix.ui.controller;

import com.heronix.model.domain.Substitute;
import com.heronix.model.domain.SubstituteAssignment;
import com.heronix.model.enums.AssignmentStatus;
import com.heronix.model.enums.StaffType;
import com.heronix.model.enums.SubstituteType;
import com.heronix.service.SubstituteManagementService;
import com.heronix.service.SubstituteReportService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

/**
 * Controller for Co-Teacher Management UI
 * Manages co-teacher substitutes and temporary assignments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Controller
public class CoTeacherManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CoTeacherManagementController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    @Autowired
    private SubstituteManagementService substituteManagementService;

    @Autowired
    private SubstituteReportService substituteReportService;

    @Autowired
    private ApplicationContext applicationContext;

    // Quick Stats Labels
    @FXML private Label activeCoTeacherSubsLabel;
    @FXML private Label todaysAssignmentsLabel;
    @FXML private Label pendingAssignmentsLabel;
    @FXML private Label monthlyHoursLabel;

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
    @FXML private TableColumn<Substitute, String> substituteNameColumn;
    @FXML private TableColumn<Substitute, String> substituteTypeColumn;
    @FXML private TableColumn<Substitute, String> substituteEmailColumn;
    @FXML private TableColumn<Substitute, String> substitutePhoneColumn;
    @FXML private TableColumn<Substitute, String> substituteStatusColumn;
    @FXML private TableColumn<Substitute, Integer> substituteAssignmentsColumn;
    @FXML private TableColumn<Substitute, Void> substituteActionsColumn;
    @FXML private Label substituteCountLabel;

    // ==================== REPORTS TAB ====================

    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private DatePicker reportStartDatePicker;
    @FXML private DatePicker reportEndDatePicker;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Co-Teacher Management UI Controller");

        setupQuickStats();
        setupAssignmentsTab();
        setupSubstitutesTab();
        setupReportsTab();

        loadData();
    }

    /**
     * Setup quick stats
     */
    private void setupQuickStats() {
        // Stats will be loaded in loadData()
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
            Substitute sub = cellData.getValue().getSubstitute();
            return new javafx.beans.property.SimpleStringProperty(sub != null ? sub.getFullName() : "");
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
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getReplacedPersonName()));

        assignmentReasonColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getAbsenceReason() != null ?
                                cellData.getValue().getAbsenceReason().getDisplayName() : ""));

        assignmentRoomColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getRoom() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getRoom().getRoomNumber());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        assignmentStatusColumn.setCellValueFactory(cellData -> {
            AssignmentStatus status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(
                    status != null ? status.getDisplayName() : "");
        });

        // Setup filter ComboBoxes
        filterStatusComboBox.setItems(FXCollections.observableArrayList(AssignmentStatus.values()));
        filterStatusComboBox.setConverter(new StringConverter<AssignmentStatus>() {
            @Override
            public String toString(AssignmentStatus status) {
                return status != null ? status.getDisplayName() : "";
            }

            @Override
            public AssignmentStatus fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Setup Substitutes Tab
     */
    private void setupSubstitutesTab() {
        substituteNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));

        substituteTypeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getType() != null ?
                                cellData.getValue().getType().getDisplayName() : ""));

        substituteEmailColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));

        substitutePhoneColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPhoneNumber()));

        substituteStatusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        Boolean.TRUE.equals(cellData.getValue().getActive()) ? "Active" : "Inactive"));

        substituteAssignmentsColumn.setCellValueFactory(cellData -> {
            // Count assignments for this substitute
            int count = (int) substituteManagementService.getAllAssignments().stream()
                    .filter(a -> a.getSubstitute() != null &&
                            a.getSubstitute().getId().equals(cellData.getValue().getId()))
                    .count();
            return new javafx.beans.property.SimpleIntegerProperty(count).asObject();
        });
    }

    /**
     * Setup Reports Tab
     */
    private void setupReportsTab() {
        reportTypeComboBox.setItems(FXCollections.observableArrayList(
                "Co-Teacher Usage Summary",
                "Co-Teacher Assignments by Date Range",
                "Co-Teacher Hours Report",
                "Co-Teacher Cost Analysis"
        ));

        // Set default dates
        reportStartDatePicker.setValue(LocalDate.now().minusMonths(1));
        reportEndDatePicker.setValue(LocalDate.now());
    }

    /**
     * Load all data
     */
    private void loadData() {
        loadQuickStats();
        loadAssignments();
        loadSubstitutes();
    }

    /**
     * Load quick stats
     */
    private void loadQuickStats() {
        try {
            // Get all co-teacher assignments
            List<SubstituteAssignment> allAssignments = substituteManagementService.getAllAssignments()
                    .stream()
                    .filter(a -> a.getReplacedStaffType() == StaffType.CO_TEACHER)
                    .collect(Collectors.toList());

            // Active co-teacher substitutes
            long activeCoTeacherSubs = substituteManagementService.getAllSubstitutes()
                    .stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getType() != null &&
                            s.getType().name().contains("CO_TEACHER"))
                    .count();
            activeCoTeacherSubsLabel.setText(String.valueOf(activeCoTeacherSubs));

            // Today's assignments
            LocalDate today = LocalDate.now();
            long todayAssignments = allAssignments.stream()
                    .filter(a -> a.getAssignmentDate() != null &&
                            a.getAssignmentDate().equals(today))
                    .count();
            todaysAssignmentsLabel.setText(String.valueOf(todayAssignments));

            // Pending assignments
            long pending = allAssignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                    .count();
            pendingAssignmentsLabel.setText(String.valueOf(pending));

            // Monthly hours
            YearMonth currentMonth = YearMonth.now();
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();
            double monthlyHours = allAssignments.stream()
                    .filter(a -> a.getAssignmentDate() != null &&
                            !a.getAssignmentDate().isBefore(monthStart) &&
                            !a.getAssignmentDate().isAfter(monthEnd))
                    .filter(a -> a.getTotalHours() != null)
                    .mapToDouble(SubstituteAssignment::getTotalHours)
                    .sum();
            monthlyHoursLabel.setText(String.format("%.1f", monthlyHours));

        } catch (Exception e) {
            logger.error("Error loading quick stats", e);
        }
    }

    /**
     * Load assignments filtered by co-teacher staff type
     */
    private void loadAssignments() {
        try {
            List<SubstituteAssignment> allAssignments = substituteManagementService.getAllAssignments();

            // Filter for co-teacher assignments only
            List<SubstituteAssignment> coTeacherAssignments = allAssignments.stream()
                    .filter(a -> a.getReplacedStaffType() == StaffType.CO_TEACHER)
                    .collect(Collectors.toList());

            assignmentsTable.setItems(FXCollections.observableArrayList(coTeacherAssignments));
            assignmentCountLabel.setText("Total: " + coTeacherAssignments.size() + " assignments");

            // Load co-teacher substitutes for filter
            List<Substitute> coTeacherSubs = substituteManagementService.getAllSubstitutes()
                    .stream()
                    .filter(s -> s.getType() != null && s.getType().name().contains("CO_TEACHER"))
                    .collect(Collectors.toList());

            filterSubstituteComboBox.setItems(FXCollections.observableArrayList(coTeacherSubs));
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

        } catch (Exception e) {
            logger.error("Error loading co-teacher assignments", e);
            showError("Error", "Failed to load assignments: " + e.getMessage());
        }
    }

    /**
     * Load co-teacher substitutes
     */
    private void loadSubstitutes() {
        try {
            List<Substitute> allSubstitutes = substituteManagementService.getAllSubstitutes();

            // Filter for co-teacher substitutes only
            List<Substitute> coTeacherSubs = allSubstitutes.stream()
                    .filter(s -> s.getType() != null && s.getType().name().contains("CO_TEACHER"))
                    .collect(Collectors.toList());

            substitutesTable.setItems(FXCollections.observableArrayList(coTeacherSubs));
            substituteCountLabel.setText("Total: " + coTeacherSubs.size() + " co-teacher substitutes");

        } catch (Exception e) {
            logger.error("Error loading co-teacher substitutes", e);
            showError("Error", "Failed to load substitutes: " + e.getMessage());
        }
    }

    // ==================== EVENT HANDLERS ====================

    @FXML
    private void handleNewAssignment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SubstituteAssignmentDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // Note: These methods don't exist yet in SubstituteAssignmentController
            // SubstituteAssignmentController controller = loader.getController();
            // controller.setParentController(this);
            // controller.setStaffTypeFilter(StaffType.CO_TEACHER);

            Stage stage = new Stage();
            stage.setTitle("New Co-Teacher Assignment");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            logger.error("Error opening new assignment dialog", e);
            showError("Error", "Failed to open assignment dialog: " + e.getMessage());
        }
    }


    @FXML
    private void handleSearchAssignments() {
        // Implement search logic
        loadAssignments();
    }

    @FXML
    private void handleClearFilters() {
        filterStartDatePicker.setValue(null);
        filterEndDatePicker.setValue(null);
        filterStatusComboBox.setValue(null);
        filterSubstituteComboBox.setValue(null);
        loadAssignments();
    }

    @FXML
    private void handleRefreshAssignments() {
        loadAssignments();
    }

    @FXML
    private void handleSearchSubstitutes() {
        // Implement search logic
        loadSubstitutes();
    }

    @FXML
    private void handleAddSubstitute() {
        Dialog<Substitute> dialog = createCoTeacherSubstituteDialog(null);
        dialog.showAndWait().ifPresent(substitute -> {
            try {
                Substitute saved = substituteManagementService.saveSubstitute(substitute);
                loadSubstitutes();
                loadQuickStats();
                showInfo("Success", "Co-teacher substitute '" + saved.getFullName() + "' added successfully.");
            } catch (Exception e) {
                logger.error("Error saving substitute", e);
                showError("Error", "Failed to save substitute: " + e.getMessage());
            }
        });
    }

    private Dialog<Substitute> createCoTeacherSubstituteDialog(Substitute existingSub) {
        Dialog<Substitute> dialog = new Dialog<>();
        dialog.setTitle(existingSub == null ? "Add Co-Teacher Substitute" : "Edit Co-Teacher Substitute");
        dialog.setHeaderText(existingSub == null ? "Enter co-teacher substitute information" : "Modify co-teacher substitute information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

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
        typeCombo.setValue(SubstituteType.CERTIFIED_TEACHER);
        typeCombo.setPromptText("Select type");

        TextField availabilityField = new TextField();
        availabilityField.setPromptText("e.g., Full-time, Mornings only");

        TextField hourlyRateField = new TextField();
        hourlyRateField.setPromptText("0.00");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes, preferences, restrictions");
        notesArea.setPrefRowCount(3);

        CheckBox activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);

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
            notesArea.setText(existingSub.getNotes());
            activeCheckBox.setSelected(Boolean.TRUE.equals(existingSub.getActive()));
        }

        int row = 0;
        grid.add(new Label("First Name: *"), 0, row);
        grid.add(firstNameField, 1, row++);
        grid.add(new Label("Last Name: *"), 0, row);
        grid.add(lastNameField, 1, row++);
        grid.add(new Label("Employee ID:"), 0, row);
        grid.add(employeeIdField, 1, row++);
        grid.add(new Label("Type: *"), 0, row);
        grid.add(typeCombo, 1, row++);
        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row++);
        grid.add(new Label("Phone:"), 0, row);
        grid.add(phoneField, 1, row++);
        grid.add(new Label("Availability:"), 0, row);
        grid.add(availabilityField, 1, row++);
        grid.add(new Label("Hourly Rate:"), 0, row);
        grid.add(hourlyRateField, 1, row++);
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);
        grid.add(activeCheckBox, 1, row);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(500, 400);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(550, 450);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        firstNameField.textProperty().addListener((obs, old, val) ->
            saveButton.setDisable(val.trim().isEmpty() || lastNameField.getText().trim().isEmpty() || typeCombo.getValue() == null));
        lastNameField.textProperty().addListener((obs, old, val) ->
            saveButton.setDisable(val.trim().isEmpty() || firstNameField.getText().trim().isEmpty() || typeCombo.getValue() == null));
        typeCombo.valueProperty().addListener((obs, old, val) ->
            saveButton.setDisable(val == null || firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty()));

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
                    if (!hourlyRateField.getText().trim().isEmpty()) {
                        try {
                            sub.setHourlyRate(Double.parseDouble(hourlyRateField.getText().trim()));
                        } catch (NumberFormatException ignored) {}
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

        javafx.application.Platform.runLater(() -> firstNameField.requestFocus());
        return dialog;
    }

    @FXML
    private void handleRefreshSubstitutes() {
        loadSubstitutes();
    }

    @FXML
    private void handleGenerateReport() {
        String reportType = reportTypeComboBox.getValue();
        if (reportType == null) {
            showError("No Report Selected", "Please select a report type.");
            return;
        }

        LocalDate startDate = reportStartDatePicker.getValue();
        LocalDate endDate = reportEndDatePicker.getValue();
        if (startDate == null || endDate == null) {
            showError("Missing Dates", "Please select both start and end dates.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Co-Teacher Report");
        fileChooser.setInitialFileName("coteacher_report_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(reportTypeComboBox.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            List<SubstituteAssignment> assignments = substituteManagementService.getAllAssignments().stream()
                .filter(a -> a.getReplacedStaffType() == StaffType.CO_TEACHER)
                .filter(a -> a.getAssignmentDate() != null &&
                    !a.getAssignmentDate().isBefore(startDate) &&
                    !a.getAssignmentDate().isAfter(endDate))
                .collect(Collectors.toList());

            writer.println("Co-Teacher " + reportType);
            writer.println("Date Range: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER));
            writer.println("Generated: " + LocalDate.now().format(DATE_FORMATTER));
            writer.println();

            writer.println("Date,Substitute,Start Time,End Time,Hours,Replaced Staff,Reason,Room,Status");
            double totalHours = 0;
            for (SubstituteAssignment a : assignments) {
                String subName = a.getSubstitute() != null ? a.getSubstitute().getFullName() : "";
                String date = a.getAssignmentDate() != null ? a.getAssignmentDate().format(DATE_FORMATTER) : "";
                String start = a.getStartTime() != null ? a.getStartTime().format(TIME_FORMATTER) : "";
                String end = a.getEndTime() != null ? a.getEndTime().format(TIME_FORMATTER) : "";
                double hours = a.getTotalHours() != null ? a.getTotalHours() : 0;
                totalHours += hours;
                String replaced = a.getReplacedPersonName() != null ? a.getReplacedPersonName() : "";
                String reason = a.getAbsenceReason() != null ? a.getAbsenceReason().getDisplayName() : "";
                String room = a.getRoom() != null ? a.getRoom().getRoomNumber() : "";
                String status = a.getStatus() != null ? a.getStatus().getDisplayName() : "";

                writer.println(String.format("%s,%s,%s,%s,%.2f,%s,%s,%s,%s",
                    escapeCSV(date), escapeCSV(subName), escapeCSV(start), escapeCSV(end),
                    hours, escapeCSV(replaced), escapeCSV(reason), escapeCSV(room), escapeCSV(status)));
            }

            writer.println();
            writer.println("Summary");
            writer.println("Total Assignments," + assignments.size());
            writer.println("Total Hours," + String.format("%.2f", totalHours));

            showInfo("Report Generated", "Report saved successfully to:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error generating report", e);
            showError("Error", "Failed to generate report: " + e.getMessage());
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Refresh data from parent window
     */
    public void refreshData() {
        loadData();
    }

    // ==================== HELPER METHODS ====================

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
