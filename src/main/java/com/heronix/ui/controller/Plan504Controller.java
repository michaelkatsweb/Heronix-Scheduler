package com.heronix.ui.controller;

import com.heronix.model.domain.Plan504;
import com.heronix.model.enums.Plan504Status;
import com.heronix.service.Plan504Service;
import com.heronix.ui.controller.dialogs.Plan504DialogController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.print.PrinterJob;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 504 Plan Management Controller
 *
 * Controller for managing 504 Plans - viewing, creating, editing, and tracking status.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8C - November 21, 2025
 */
@Controller
@Slf4j
public class Plan504Controller {

    @Autowired
    private Plan504Service plan504Service;

    @Autowired
    private ApplicationContext applicationContext;

    // Search and Filter Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> coordinatorFilter;

    // Table and Columns
    @FXML private TableView<Plan504> planTable;
    @FXML private TableColumn<Plan504, String> studentColumn;
    @FXML private TableColumn<Plan504, String> planNumberColumn;
    @FXML private TableColumn<Plan504, String> statusColumn;
    @FXML private TableColumn<Plan504, String> startDateColumn;
    @FXML private TableColumn<Plan504, String> endDateColumn;
    @FXML private TableColumn<Plan504, String> daysRemainingColumn;
    @FXML private TableColumn<Plan504, String> disabilityColumn;
    @FXML private TableColumn<Plan504, String> coordinatorColumn;
    @FXML private TableColumn<Plan504, String> accommodationsColumn;
    @FXML private TableColumn<Plan504, String> actionsColumn;

    // Summary Labels
    @FXML private Label totalPlansLabel;
    @FXML private Label activePlansLabel;
    @FXML private Label draftPlansLabel;
    @FXML private Label expiringPlansLabel;
    @FXML private Label selectionLabel;

    // Context Menu
    @FXML private ContextMenu rowContextMenu;

    // Data
    private ObservableList<Plan504> allPlans = FXCollections.observableArrayList();
    private ObservableList<Plan504> filteredPlans = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        log.info("Initializing 504 Plan Management Controller");
        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
        // Configure columns
        studentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudent().getFullName()));

        planNumberColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPlanNumber() != null ?
                data.getValue().getPlanNumber() : "DRAFT"));

        statusColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));

        startDateColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStartDate().toString()));

        endDateColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getEndDate().toString()));

        daysRemainingColumn.setCellValueFactory(data -> {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), data.getValue().getEndDate());
            String text = days < 0 ? "EXPIRED" : String.valueOf(days);
            return new SimpleStringProperty(text);
        });

        disabilityColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDisability() != null ?
                data.getValue().getDisability() : "N/A"));

        coordinatorColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCoordinator() != null ?
                data.getValue().getCoordinator() : "N/A"));

        // Accommodations is a text field, show truncated preview
        accommodationsColumn.setCellValueFactory(data -> {
            String acc = data.getValue().getAccommodations();
            if (acc == null || acc.isEmpty()) {
                return new SimpleStringProperty("None");
            }
            // Count lines as approximate accommodation count
            int count = acc.split("\n").length;
            return new SimpleStringProperty(count + " item(s)");
        });

        // Setup actions column with buttons
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final HBox buttonBox = new HBox(5);
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");

            {
                viewBtn.setOnAction(e -> handleViewDetails(getTableRow().getItem()));
                viewBtn.getStyleClass().add("btn-link");
                editBtn.setOnAction(e -> handleEditPlan(getTableRow().getItem()));
                editBtn.getStyleClass().add("btn-link");
                buttonBox.getChildren().addAll(viewBtn, editBtn);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
            }
        });

        // Apply row styling based on status
        planTable.setRowFactory(tv -> {
            TableRow<Plan504> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    planTable.getSelectionModel().select(row.getItem());
                    rowContextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });

            row.itemProperty().addListener((obs, oldPlan, newPlan) -> {
                row.getStyleClass().removeAll("status-active", "status-draft", "status-expired", "status-expiring");
                if (newPlan != null) {
                    if (newPlan.getStatus() == Plan504Status.EXPIRED) {
                        row.getStyleClass().add("status-expired");
                    } else if (newPlan.getStatus() == Plan504Status.DRAFT) {
                        row.getStyleClass().add("status-draft");
                    } else if (newPlan.getStatus() == Plan504Status.ACTIVE) {
                        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), newPlan.getEndDate());
                        if (daysRemaining <= 30) {
                            row.getStyleClass().add("status-expiring");
                        } else {
                            row.getStyleClass().add("status-active");
                        }
                    }
                }
            });
            return row;
        });

        // Selection listener
        planTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectionLabel.setText("Selected: " + newSelection.getStudent().getFullName() +
                    " (" + newSelection.getStatus().getDisplayName() + ")");
            } else {
                selectionLabel.setText("No plan selected");
            }
        });

        planTable.setItems(filteredPlans);
    }

    private void setupFilters() {
        // Setup status filter
        statusFilter.getItems().addAll(
            "All Statuses",
            "Active",
            "Draft",
            "Pending Review",
            "Expired"
        );
        statusFilter.setValue("All Statuses");

        // Coordinator filter will be populated dynamically
        coordinatorFilter.getItems().add("All Coordinators");
        coordinatorFilter.setValue("All Coordinators");
    }

    private void loadData() {
        try {
            log.info("Loading 504 Plan data");

            // Load all active plans
            List<Plan504> activePlans = plan504Service.findAllActivePlans();

            // Also load draft and pending review plans
            List<Plan504> draftPlans = plan504Service.findByStatus(Plan504Status.DRAFT);
            List<Plan504> pendingPlans = plan504Service.findByStatus(Plan504Status.PENDING_REVIEW);

            allPlans.clear();
            allPlans.addAll(activePlans);
            allPlans.addAll(draftPlans);
            allPlans.addAll(pendingPlans);

            // Populate coordinator filter
            updateCoordinatorFilter();

            // Apply filters
            applyFilters();

            // Update summary
            updateSummary();

            log.info("Loaded {} 504 Plans", allPlans.size());

        } catch (Exception e) {
            log.error("Error loading 504 Plan data", e);
            showError("Error Loading Data", "Failed to load 504 Plans: " + e.getMessage());
        }
    }

    private void updateCoordinatorFilter() {
        List<String> coordinators = allPlans.stream()
            .map(Plan504::getCoordinator)
            .filter(c -> c != null && !c.isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        coordinatorFilter.getItems().clear();
        coordinatorFilter.getItems().add("All Coordinators");
        coordinatorFilter.getItems().addAll(coordinators);
        coordinatorFilter.setValue("All Coordinators");
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();
        String coordinatorValue = coordinatorFilter.getValue();

        filteredPlans.clear();
        filteredPlans.addAll(allPlans.stream()
            .filter(plan -> {
                // Search filter
                if (!searchText.isEmpty()) {
                    String studentName = plan.getStudent().getFullName().toLowerCase();
                    String planNumber = plan.getPlanNumber() != null ? plan.getPlanNumber().toLowerCase() : "";
                    if (!studentName.contains(searchText) && !planNumber.contains(searchText)) {
                        return false;
                    }
                }

                // Status filter
                if (!"All Statuses".equals(statusValue)) {
                    if (!plan.getStatus().getDisplayName().equals(statusValue)) {
                        return false;
                    }
                }

                // Coordinator filter
                if (!"All Coordinators".equals(coordinatorValue)) {
                    if (!coordinatorValue.equals(plan.getCoordinator())) {
                        return false;
                    }
                }

                return true;
            })
            .collect(Collectors.toList()));

        updateSummary();
    }

    private void updateSummary() {
        int total = filteredPlans.size();
        long active = filteredPlans.stream().filter(p -> p.getStatus() == Plan504Status.ACTIVE).count();
        long draft = filteredPlans.stream().filter(p -> p.getStatus() == Plan504Status.DRAFT).count();
        long expiring = filteredPlans.stream()
            .filter(p -> p.getStatus() == Plan504Status.ACTIVE)
            .filter(p -> ChronoUnit.DAYS.between(LocalDate.now(), p.getEndDate()) <= 30)
            .count();

        totalPlansLabel.setText("Total: " + total);
        activePlansLabel.setText("Active: " + active);
        draftPlansLabel.setText("Draft: " + draft);
        expiringPlansLabel.setText("Expiring Soon: " + expiring);
    }

    // Event Handlers

    @FXML
    private void handleRefresh() {
        log.info("Refreshing 504 Plan data");
        loadData();
    }

    @FXML
    private void handleCreatePlan() {
        log.info("Creating new 504 Plan");
        showPlanDialog(null);
    }

    @FXML
    private void handleExportReport() {
        log.info("Exporting 504 Plan report");

        if (filteredPlans.isEmpty()) {
            showInfo("No Data", "There are no 504 Plans to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export 504 Plan Report");
        fileChooser.setInitialFileName("504_plan_report_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(planTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write CSV header
            writer.println("Student Name,Student ID,Grade,Plan Number,Status,Start Date,End Date,Days Remaining,Disability,Coordinator,Accommodations");

            // Write data rows
            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            for (Plan504 plan : filteredPlans) {
                long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), plan.getEndDate());
                String daysText = daysRemaining < 0 ? "EXPIRED" : String.valueOf(daysRemaining);

                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    escapeCSV(plan.getStudent() != null ? plan.getStudent().getFullName() : ""),
                    escapeCSV(plan.getStudent() != null ? plan.getStudent().getStudentId() : ""),
                    plan.getStudent() != null ? plan.getStudent().getGradeLevel() : "",
                    escapeCSV(plan.getPlanNumber() != null ? plan.getPlanNumber() : "DRAFT"),
                    plan.getStatus() != null ? plan.getStatus().getDisplayName() : "",
                    plan.getStartDate() != null ? plan.getStartDate().format(dateFormatter) : "",
                    plan.getEndDate() != null ? plan.getEndDate().format(dateFormatter) : "",
                    daysText,
                    escapeCSV(plan.getDisability() != null ? plan.getDisability() : ""),
                    escapeCSV(plan.getCoordinator() != null ? plan.getCoordinator() : ""),
                    escapeCSV(plan.getAccommodations() != null ? plan.getAccommodations().replace("\n", " | ") : "")
                );
            }

            log.info("Exported {} 504 Plans to {}", filteredPlans.size(), file.getAbsolutePath());
            showSuccess("Export Complete", "Exported " + filteredPlans.size() + " 504 Plans to:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            log.error("Error exporting 504 Plan report", e);
            showError("Export Error", "Failed to export 504 Plan report: " + e.getMessage());
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    @FXML
    private void handleSearch(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || searchField.getText().isEmpty()) {
            applyFilters();
        }
    }

    @FXML
    private void handleFilterChange() {
        applyFilters();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        statusFilter.setValue("All Statuses");
        coordinatorFilter.setValue("All Coordinators");
        applyFilters();
    }

    // Context Menu Actions

    @FXML
    private void handleViewDetails() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        handleViewDetails(selected);
    }

    private void handleViewDetails(Plan504 plan) {
        if (plan == null) return;
        log.info("Viewing 504 Plan details: {}", plan.getId());

        // Build comprehensive 504 Plan detail view
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════\n");
        details.append("          504 PLAN DETAILS             \n");
        details.append("═══════════════════════════════════════\n\n");

        // Student Information
        details.append("STUDENT INFORMATION\n");
        details.append("───────────────────────────────────────\n");
        if (plan.getStudent() != null) {
            details.append("Name: ").append(plan.getStudent().getFullName()).append("\n");
            details.append("Student ID: ").append(plan.getStudent().getStudentId()).append("\n");
            details.append("Grade: ").append(plan.getStudent().getGradeLevel()).append("\n");
        }
        details.append("\n");

        // Plan Status
        details.append("PLAN STATUS\n");
        details.append("───────────────────────────────────────\n");
        details.append("Plan Number: ").append(plan.getPlanNumber() != null ? plan.getPlanNumber() : "DRAFT").append("\n");
        details.append("Status: ").append(plan.getStatus() != null ? plan.getStatus().getDisplayName() : "N/A").append("\n");
        details.append("Start Date: ").append(plan.getStartDate()).append("\n");
        details.append("End Date: ").append(plan.getEndDate()).append("\n");
        if (plan.getNextReviewDate() != null) {
            details.append("Next Review: ").append(plan.getNextReviewDate()).append("\n");
        }
        details.append("Coordinator: ").append(plan.getCoordinator() != null ? plan.getCoordinator() : "Unassigned").append("\n");
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), plan.getEndDate());
        details.append("Days Remaining: ").append(daysRemaining < 0 ? "EXPIRED" : daysRemaining).append("\n");
        details.append("\n");

        // Disability Information
        details.append("DISABILITY INFORMATION\n");
        details.append("───────────────────────────────────────\n");
        details.append("Disability: ").append(plan.getDisability() != null ? plan.getDisability() : "N/A").append("\n");
        details.append("\n");

        // Accommodations
        details.append("ACCOMMODATIONS\n");
        details.append("───────────────────────────────────────\n");
        if (plan.getAccommodations() != null && !plan.getAccommodations().isEmpty()) {
            details.append(plan.getAccommodations()).append("\n");
        } else {
            details.append("No accommodations defined.\n");
        }
        details.append("\n");

        // Notes
        details.append("NOTES\n");
        details.append("───────────────────────────────────────\n");
        if (plan.getNotes() != null && !plan.getNotes().isEmpty()) {
            details.append(plan.getNotes()).append("\n");
        } else {
            details.append("No notes.\n");
        }

        // Show in scrollable dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("504 Plan Details");
        alert.setHeaderText("504 Plan for " + (plan.getStudent() != null ? plan.getStudent().getFullName() : "Unknown Student"));

        TextArea textArea = new TextArea(details.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
        textArea.setPrefWidth(600);
        textArea.setPrefHeight(450);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setMinWidth(650);
        alert.showAndWait();
    }

    @FXML
    private void handleEditPlan() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        handleEditPlan(selected);
    }

    private void handleEditPlan(Plan504 plan) {
        if (plan == null) return;
        log.info("Editing 504 Plan: {}", plan.getId());
        showPlanDialog(plan);
    }

    private void showPlanDialog(Plan504 plan) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/Plan504Dialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            DialogPane dialogPane = loader.load();

            Plan504DialogController controller = loader.getController();
            if (plan != null) {
                controller.setPlan(plan);
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(plan == null ? "Create New 504 Plan" : "Edit 504 Plan");

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    List<String> errors = controller.validate();
                    if (!errors.isEmpty()) {
                        showError("Validation Error", String.join("\n", errors));
                        return;
                    }

                    try {
                        Plan504 savedPlan = controller.getPlan();
                        if (plan == null) {
                            plan504Service.createPlan(savedPlan);
                            showSuccess("Plan Created", "504 Plan has been created successfully.");
                        } else {
                            plan504Service.updatePlan(savedPlan);
                            showSuccess("Plan Updated", "504 Plan has been updated successfully.");
                        }
                        loadData();
                    } catch (Exception e) {
                        log.error("Error saving 504 Plan", e);
                        showError("Save Error", "Failed to save 504 Plan: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            log.error("Error loading 504 Plan dialog", e);
            showError("Dialog Error", "Failed to open 504 Plan dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleManageAccommodations() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Managing accommodations for 504 Plan: {}", selected.getId());

        // Create dialog for editing accommodations
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Manage Accommodations");
        dialog.setHeaderText("504 Plan Accommodations for " + selected.getStudent().getFullName());

        // Create text area for accommodations
        TextArea accommodationsArea = new TextArea(selected.getAccommodations() != null ? selected.getAccommodations() : "");
        accommodationsArea.setPrefWidth(500);
        accommodationsArea.setPrefHeight(300);
        accommodationsArea.setWrapText(true);
        accommodationsArea.setPromptText("Enter each accommodation on a new line:\n- Extended time for tests\n- Preferential seating\n- Audio recordings of lectures");

        // Layout
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().addAll(
            new Label("Enter accommodations (one per line):"),
            accommodationsArea
        );
        content.setStyle("-fx-padding: 10;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Convert result
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return accommodationsArea.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(accommodations -> {
            try {
                selected.setAccommodations(accommodations);
                plan504Service.updatePlan(selected);
                showSuccess("Accommodations Updated", "Accommodations have been saved successfully.");
                loadData();
            } catch (Exception e) {
                log.error("Error saving accommodations", e);
                showError("Save Error", "Failed to save accommodations: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleActivatePlan() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            log.info("Activating 504 Plan: {}", selected.getId());
            plan504Service.activatePlan(selected.getId());
            showSuccess("Plan Activated", "504 Plan has been activated successfully.");
            loadData();
        } catch (Exception e) {
            log.error("Error activating 504 Plan", e);
            showError("Activation Error", "Failed to activate 504 Plan: " + e.getMessage());
        }
    }

    @FXML
    private void handleMarkForReview() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            log.info("Marking 504 Plan for review: {}", selected.getId());
            plan504Service.markForReview(selected.getId());
            showSuccess("Marked for Review", "504 Plan has been marked for review.");
            loadData();
        } catch (Exception e) {
            log.error("Error marking 504 Plan for review", e);
            showError("Review Error", "Failed to mark 504 Plan for review: " + e.getMessage());
        }
    }

    @FXML
    private void handleExpirePlan() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Expire 504 Plan");
        confirmation.setHeaderText("Confirm Plan Expiration");
        confirmation.setContentText("Are you sure you want to expire this 504 Plan?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                log.info("Expiring 504 Plan: {}", selected.getId());
                plan504Service.expirePlan(selected.getId());
                showSuccess("Plan Expired", "504 Plan has been expired.");
                loadData();
            } catch (Exception e) {
                log.error("Error expiring 504 Plan", e);
                showError("Expiration Error", "Failed to expire 504 Plan: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleGeneratePlanReport() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Generating report for 504 Plan: {}", selected.getId());

        String reportContent = build504PlanReportContent(selected);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save 504 Plan Report");
        String studentName = selected.getStudent() != null ?
            selected.getStudent().getFullName().replaceAll("[^a-zA-Z0-9]", "_") : "Unknown";
        fileChooser.setInitialFileName("504_Plan_Report_" + studentName + "_" +
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(planTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(reportContent);
            log.info("Generated 504 Plan report: {}", file.getAbsolutePath());
            showSuccess("Report Generated", "504 Plan report saved to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error generating 504 Plan report", e);
            showError("Report Error", "Failed to generate 504 Plan report: " + e.getMessage());
        }
    }

    @FXML
    private void handlePrintPlan() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Printing 504 Plan: {}", selected.getId());

        String reportContent = build504PlanReportContent(selected);

        // Create a TextFlow for printing
        TextFlow textFlow = new TextFlow();
        Text text = new Text(reportContent);
        text.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 10px;");
        textFlow.getChildren().add(text);
        textFlow.setPrefWidth(550);

        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob != null) {
            boolean showDialog = printerJob.showPrintDialog(planTable.getScene().getWindow());
            if (showDialog) {
                boolean success = printerJob.printPage(textFlow);
                if (success) {
                    printerJob.endJob();
                    log.info("504 Plan printed successfully");
                    showSuccess("Print Complete", "504 Plan has been sent to the printer.");
                } else {
                    showError("Print Error", "Failed to print 504 Plan.");
                }
            }
        } else {
            showError("Print Error", "No printer available.");
        }
    }

    /**
     * Build formatted 504 Plan report content for export/print
     */
    private String build504PlanReportContent(Plan504 plan) {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

        report.append("================================================================================\n");
        report.append("                         SECTION 504 PLAN                                       \n");
        report.append("                        CONFIDENTIAL DOCUMENT                                   \n");
        report.append("================================================================================\n\n");

        report.append("Report Generated: ").append(LocalDate.now().format(dateFormatter)).append("\n\n");

        // Student Information
        report.append("STUDENT INFORMATION\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (plan.getStudent() != null) {
            report.append(String.format("%-20s: %s%n", "Name", plan.getStudent().getFullName()));
            report.append(String.format("%-20s: %s%n", "Student ID", plan.getStudent().getStudentId()));
            report.append(String.format("%-20s: %s%n", "Grade Level", plan.getStudent().getGradeLevel()));
        }
        report.append("\n");

        // Plan Information
        report.append("PLAN STATUS INFORMATION\n");
        report.append("--------------------------------------------------------------------------------\n");
        report.append(String.format("%-20s: %s%n", "Plan Number", plan.getPlanNumber() != null ? plan.getPlanNumber() : "DRAFT"));
        report.append(String.format("%-20s: %s%n", "Status", plan.getStatus() != null ? plan.getStatus().getDisplayName() : "N/A"));
        report.append(String.format("%-20s: %s%n", "Start Date", plan.getStartDate() != null ? plan.getStartDate().format(dateFormatter) : "N/A"));
        report.append(String.format("%-20s: %s%n", "End Date", plan.getEndDate() != null ? plan.getEndDate().format(dateFormatter) : "N/A"));
        if (plan.getNextReviewDate() != null) {
            report.append(String.format("%-20s: %s%n", "Next Review", plan.getNextReviewDate().format(dateFormatter)));
        }
        report.append(String.format("%-20s: %s%n", "Coordinator", plan.getCoordinator() != null ? plan.getCoordinator() : "Unassigned"));
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), plan.getEndDate());
        report.append(String.format("%-20s: %s%n", "Days Remaining", daysRemaining < 0 ? "EXPIRED" : String.valueOf(daysRemaining)));
        report.append("\n");

        // Disability Information
        report.append("DISABILITY INFORMATION\n");
        report.append("--------------------------------------------------------------------------------\n");
        report.append(String.format("%-20s: %s%n", "Disability", plan.getDisability() != null ? plan.getDisability() : "N/A"));
        report.append("\n");

        // Accommodations
        report.append("ACCOMMODATIONS\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (plan.getAccommodations() != null && !plan.getAccommodations().isEmpty()) {
            report.append(plan.getAccommodations()).append("\n");
        } else {
            report.append("No accommodations defined.\n");
        }
        report.append("\n");

        // Notes
        report.append("ADDITIONAL NOTES\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (plan.getNotes() != null && !plan.getNotes().isEmpty()) {
            report.append(plan.getNotes()).append("\n");
        } else {
            report.append("No additional notes.\n");
        }
        report.append("\n");

        report.append("================================================================================\n");
        report.append("                       END OF 504 PLAN REPORT                                   \n");
        report.append("================================================================================\n");

        return report.toString();
    }

    @FXML
    private void handleDeletePlan() {
        Plan504 selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete 504 Plan");
        confirmation.setHeaderText("Confirm Plan Deletion");
        confirmation.setContentText("Are you sure you want to delete this 504 Plan? This action cannot be undone.\n\n" +
            "Note: Only DRAFT plans can be deleted.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                log.info("Deleting 504 Plan: {}", selected.getId());
                plan504Service.deletePlan(selected.getId());
                showSuccess("Plan Deleted", "504 Plan has been deleted.");
                loadData();
            } catch (Exception e) {
                log.error("Error deleting 504 Plan", e);
                showError("Deletion Error", "Failed to delete 504 Plan: " + e.getMessage());
            }
        }
    }

    // Utility Methods

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
