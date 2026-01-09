package com.heronix.ui.controller;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.enums.IEPStatus;
import com.heronix.service.IEPManagementService;
import com.heronix.ui.controller.dialogs.IEPDialogController;
import com.heronix.ui.controller.dialogs.ServiceDialogController;
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
 * IEP Management Controller
 *
 * Controller for managing IEPs - viewing, creating, editing, and tracking status.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8C - November 21, 2025
 */
@Controller
@Slf4j
public class IEPManagementController {

    @Autowired
    private IEPManagementService iepManagementService;

    @Autowired
    private ApplicationContext applicationContext;

    // Search and Filter Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> caseManagerFilter;

    // Table and Columns
    @FXML private TableView<IEP> iepTable;
    @FXML private TableColumn<IEP, String> studentColumn;
    @FXML private TableColumn<IEP, String> iepNumberColumn;
    @FXML private TableColumn<IEP, String> statusColumn;
    @FXML private TableColumn<IEP, String> startDateColumn;
    @FXML private TableColumn<IEP, String> endDateColumn;
    @FXML private TableColumn<IEP, String> daysRemainingColumn;
    @FXML private TableColumn<IEP, String> categoryColumn;
    @FXML private TableColumn<IEP, String> caseManagerColumn;
    @FXML private TableColumn<IEP, String> servicesColumn;
    @FXML private TableColumn<IEP, String> actionsColumn;

    // Summary Labels
    @FXML private Label totalIEPsLabel;
    @FXML private Label activeIEPsLabel;
    @FXML private Label draftIEPsLabel;
    @FXML private Label expiringIEPsLabel;
    @FXML private Label selectionLabel;

    // Context Menu
    @FXML private ContextMenu rowContextMenu;

    // Data
    private ObservableList<IEP> allIEPs = FXCollections.observableArrayList();
    private ObservableList<IEP> filteredIEPs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        log.info("Initializing IEP Management Controller");
        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
        // Configure columns
        studentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudent().getFullName()));

        iepNumberColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIepNumber() != null ?
                data.getValue().getIepNumber() : "DRAFT"));

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

        categoryColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getEligibilityCategory() != null ?
                data.getValue().getEligibilityCategory() : "N/A"));

        caseManagerColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCaseManager() != null ?
                data.getValue().getCaseManager() : "N/A"));

        servicesColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getServices().size())));

        // Setup actions column with buttons
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final HBox buttonBox = new HBox(5);
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");

            {
                viewBtn.setOnAction(e -> handleViewDetails(getTableRow().getItem()));
                viewBtn.getStyleClass().add("btn-link");
                editBtn.setOnAction(e -> handleEditIEP(getTableRow().getItem()));
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
        iepTable.setRowFactory(tv -> {
            TableRow<IEP> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    iepTable.getSelectionModel().select(row.getItem());
                    rowContextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });

            row.itemProperty().addListener((obs, oldIEP, newIEP) -> {
                row.getStyleClass().removeAll("status-active", "status-draft", "status-expired", "status-expiring");
                if (newIEP != null) {
                    if (newIEP.getStatus() == IEPStatus.EXPIRED) {
                        row.getStyleClass().add("status-expired");
                    } else if (newIEP.getStatus() == IEPStatus.DRAFT) {
                        row.getStyleClass().add("status-draft");
                    } else if (newIEP.getStatus() == IEPStatus.ACTIVE) {
                        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), newIEP.getEndDate());
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
        iepTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectionLabel.setText("Selected: " + newSelection.getStudent().getFullName() +
                    " (" + newSelection.getStatus().getDisplayName() + ")");
            } else {
                selectionLabel.setText("No IEP selected");
            }
        });

        iepTable.setItems(filteredIEPs);
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

        // Case manager filter will be populated dynamically
        caseManagerFilter.getItems().add("All Case Managers");
        caseManagerFilter.setValue("All Case Managers");
    }

    private void loadData() {
        try {
            log.info("Loading IEP data");

            // Load all active IEPs
            List<IEP> activeIEPs = iepManagementService.findAllActiveIEPs();

            // Also load draft and pending review IEPs
            List<IEP> draftIEPs = iepManagementService.findByStatus(IEPStatus.DRAFT);
            List<IEP> pendingIEPs = iepManagementService.findByStatus(IEPStatus.PENDING_REVIEW);

            allIEPs.clear();
            allIEPs.addAll(activeIEPs);
            allIEPs.addAll(draftIEPs);
            allIEPs.addAll(pendingIEPs);

            // Populate case manager filter
            updateCaseManagerFilter();

            // Apply filters
            applyFilters();

            // Update summary
            updateSummary();

            log.info("Loaded {} IEPs", allIEPs.size());

        } catch (Exception e) {
            log.error("Error loading IEP data", e);
            showError("Error Loading Data", "Failed to load IEPs: " + e.getMessage());
        }
    }

    private void updateCaseManagerFilter() {
        List<String> caseManagers = allIEPs.stream()
            .map(IEP::getCaseManager)
            .filter(cm -> cm != null && !cm.isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        caseManagerFilter.getItems().clear();
        caseManagerFilter.getItems().add("All Case Managers");
        caseManagerFilter.getItems().addAll(caseManagers);
        caseManagerFilter.setValue("All Case Managers");
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();
        String caseManagerValue = caseManagerFilter.getValue();

        filteredIEPs.clear();
        filteredIEPs.addAll(allIEPs.stream()
            .filter(iep -> {
                // Search filter
                if (!searchText.isEmpty()) {
                    String studentName = iep.getStudent().getFullName().toLowerCase();
                    String iepNumber = iep.getIepNumber() != null ? iep.getIepNumber().toLowerCase() : "";
                    if (!studentName.contains(searchText) && !iepNumber.contains(searchText)) {
                        return false;
                    }
                }

                // Status filter
                if (!"All Statuses".equals(statusValue)) {
                    if (!iep.getStatus().getDisplayName().equals(statusValue)) {
                        return false;
                    }
                }

                // Case manager filter
                if (!"All Case Managers".equals(caseManagerValue)) {
                    if (!caseManagerValue.equals(iep.getCaseManager())) {
                        return false;
                    }
                }

                return true;
            })
            .collect(Collectors.toList()));

        updateSummary();
    }

    private void updateSummary() {
        int total = filteredIEPs.size();
        long active = filteredIEPs.stream().filter(iep -> iep.getStatus() == IEPStatus.ACTIVE).count();
        long draft = filteredIEPs.stream().filter(iep -> iep.getStatus() == IEPStatus.DRAFT).count();
        long expiring = filteredIEPs.stream()
            .filter(iep -> iep.getStatus() == IEPStatus.ACTIVE)
            .filter(iep -> ChronoUnit.DAYS.between(LocalDate.now(), iep.getEndDate()) <= 30)
            .count();

        totalIEPsLabel.setText("Total: " + total);
        activeIEPsLabel.setText("Active: " + active);
        draftIEPsLabel.setText("Draft: " + draft);
        expiringIEPsLabel.setText("Expiring Soon: " + expiring);
    }

    // Event Handlers

    @FXML
    private void handleRefresh() {
        log.info("Refreshing IEP data");
        loadData();
    }

    @FXML
    private void handleCreateIEP() {
        log.info("Creating new IEP");
        showIEPDialog(null);
    }

    @FXML
    private void handleExportReport() {
        log.info("Exporting IEP report");

        if (filteredIEPs.isEmpty()) {
            showInfo("No Data", "There are no IEPs to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export IEP Report");
        fileChooser.setInitialFileName("iep_report_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(iepTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write CSV header
            writer.println("Student Name,Student ID,Grade,IEP Number,Status,Start Date,End Date,Days Remaining,Primary Disability,Case Manager,Services Count,Goals");

            // Write data rows
            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            for (IEP iep : filteredIEPs) {
                long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), iep.getEndDate());
                String daysText = daysRemaining < 0 ? "EXPIRED" : String.valueOf(daysRemaining);

                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                    escapeCSV(iep.getStudent() != null ? iep.getStudent().getFullName() : ""),
                    escapeCSV(iep.getStudent() != null ? iep.getStudent().getStudentId() : ""),
                    iep.getStudent() != null ? iep.getStudent().getGradeLevel() : "",
                    escapeCSV(iep.getIepNumber() != null ? iep.getIepNumber() : "DRAFT"),
                    iep.getStatus() != null ? iep.getStatus().getDisplayName() : "",
                    iep.getStartDate() != null ? iep.getStartDate().format(dateFormatter) : "",
                    iep.getEndDate() != null ? iep.getEndDate().format(dateFormatter) : "",
                    daysText,
                    escapeCSV(iep.getPrimaryDisability() != null ? iep.getPrimaryDisability() : ""),
                    escapeCSV(iep.getCaseManager() != null ? iep.getCaseManager() : ""),
                    iep.getServices() != null ? iep.getServices().size() : 0,
                    escapeCSV(iep.getGoals() != null ? iep.getGoals().replace("\n", " | ") : "")
                );
            }

            log.info("Exported {} IEPs to {}", filteredIEPs.size(), file.getAbsolutePath());
            showSuccess("Export Complete", "Exported " + filteredIEPs.size() + " IEPs to:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            log.error("Error exporting IEP report", e);
            showError("Export Error", "Failed to export IEP report: " + e.getMessage());
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
        caseManagerFilter.setValue("All Case Managers");
        applyFilters();
    }

    // Context Menu Actions

    @FXML
    private void handleViewDetails() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        handleViewDetails(selected);
    }

    private void handleViewDetails(IEP iep) {
        if (iep == null) return;
        log.info("Viewing IEP details: {}", iep.getId());

        // Build comprehensive IEP detail view
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════\n");
        details.append("              IEP DETAILS              \n");
        details.append("═══════════════════════════════════════\n\n");

        // Student Information
        details.append("STUDENT INFORMATION\n");
        details.append("───────────────────────────────────────\n");
        if (iep.getStudent() != null) {
            details.append("Name: ").append(iep.getStudent().getFullName()).append("\n");
            details.append("Student ID: ").append(iep.getStudent().getStudentId()).append("\n");
            details.append("Grade: ").append(iep.getStudent().getGradeLevel()).append("\n");
        }
        details.append("\n");

        // IEP Status
        details.append("IEP STATUS\n");
        details.append("───────────────────────────────────────\n");
        details.append("Status: ").append(iep.getStatus() != null ? iep.getStatus().getDisplayName() : "N/A").append("\n");
        details.append("Start Date: ").append(iep.getStartDate()).append("\n");
        details.append("End Date: ").append(iep.getEndDate()).append("\n");
        details.append("Next Review: ").append(iep.getNextReviewDate()).append("\n");
        details.append("Case Manager: ").append(iep.getCaseManager() != null ? iep.getCaseManager() : "Unassigned").append("\n");
        details.append("\n");

        // Disability Information
        details.append("DISABILITY & ELIGIBILITY\n");
        details.append("───────────────────────────────────────\n");
        details.append("Primary Disability: ").append(iep.getPrimaryDisability() != null ? iep.getPrimaryDisability() : "N/A").append("\n");
        details.append("\n");

        // Services
        details.append("SERVICES\n");
        details.append("───────────────────────────────────────\n");
        if (iep.getServices() != null && !iep.getServices().isEmpty()) {
            int i = 1;
            for (IEPService service : iep.getServices()) {
                details.append(i++).append(". ").append(service.getServiceType().getDisplayName()).append("\n");
                details.append("   Delivery: ").append(service.getDeliveryModel().getDisplayName()).append("\n");
                details.append("   Minutes/Week: ").append(service.getMinutesPerWeek()).append("\n");
                details.append("   Provider: ").append(service.getAssignedStaff() != null ? service.getAssignedStaff().getName() : "Unassigned").append("\n");
                details.append("\n");
            }
        } else {
            details.append("No services defined.\n\n");
        }

        // Goals (stored as text)
        details.append("GOALS\n");
        details.append("───────────────────────────────────────\n");
        if (iep.getGoals() != null && !iep.getGoals().isEmpty()) {
            details.append(iep.getGoals()).append("\n");
        } else {
            details.append("No goals defined.\n");
        }
        details.append("\n");

        // Notes
        details.append("NOTES\n");
        details.append("───────────────────────────────────────\n");
        if (iep.getNotes() != null && !iep.getNotes().isEmpty()) {
            details.append(iep.getNotes()).append("\n");
        } else {
            details.append("No notes.\n");
        }

        // Show in scrollable dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("IEP Details");
        alert.setHeaderText("IEP for " + (iep.getStudent() != null ? iep.getStudent().getFullName() : "Unknown Student"));

        TextArea textArea = new TextArea(details.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
        textArea.setPrefWidth(600);
        textArea.setPrefHeight(500);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setMinWidth(650);
        alert.showAndWait();
    }

    @FXML
    private void handleEditIEP() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        handleEditIEP(selected);
    }

    private void handleEditIEP(IEP iep) {
        if (iep == null) return;
        log.info("Editing IEP: {}", iep.getId());
        showIEPDialog(iep);
    }

    private void showIEPDialog(IEP iep) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/IEPDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            DialogPane dialogPane = loader.load();

            IEPDialogController controller = loader.getController();
            if (iep != null) {
                controller.setIEP(iep);
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(iep == null ? "Create New IEP" : "Edit IEP");

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    List<String> errors = controller.validate();
                    if (!errors.isEmpty()) {
                        showError("Validation Error", String.join("\n", errors));
                        return;
                    }

                    try {
                        IEP savedIEP = controller.getIEP();
                        if (iep == null) {
                            iepManagementService.createIEP(savedIEP);
                            showSuccess("IEP Created", "IEP has been created successfully.");
                        } else {
                            iepManagementService.updateIEP(savedIEP);
                            showSuccess("IEP Updated", "IEP has been updated successfully.");
                        }
                        loadData();
                    } catch (Exception e) {
                        log.error("Error saving IEP", e);
                        showError("Save Error", "Failed to save IEP: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            log.error("Error loading IEP dialog", e);
            showError("Dialog Error", "Failed to open IEP dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddService() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Adding service to IEP: {}", selected.getId());
        showServiceDialog(selected, null);
    }

    private void showServiceDialog(IEP iep, IEPService service) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/ServiceDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            DialogPane dialogPane = loader.load();

            ServiceDialogController controller = loader.getController();
            if (service != null) {
                controller.setService(service);
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(service == null ? "Add Service" : "Edit Service");

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    List<String> errors = controller.validate();
                    if (!errors.isEmpty()) {
                        showError("Validation Error", String.join("\n", errors));
                        return;
                    }

                    try {
                        IEPService savedService = controller.getService();
                        if (service == null) {
                            iep.addService(savedService);
                            iepManagementService.updateIEP(iep);
                            showSuccess("Service Added", "Service has been added to the IEP.");
                        } else {
                            iepManagementService.updateIEP(iep);
                            showSuccess("Service Updated", "Service has been updated.");
                        }
                        loadData();
                    } catch (Exception e) {
                        log.error("Error saving service", e);
                        showError("Save Error", "Failed to save service: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            log.error("Error loading service dialog", e);
            showError("Dialog Error", "Failed to open service dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleManageServices() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Managing services for IEP: {}", selected.getId());

        // Create a dialog for managing services
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage IEP Services");
        dialog.setHeaderText("Services for " + selected.getStudent().getFullName());

        // Create services list view
        ListView<IEPService> servicesListView = new ListView<>();
        servicesListView.setPrefWidth(500);
        servicesListView.setPrefHeight(300);

        // Custom cell factory to display service details
        servicesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(IEPService service, boolean empty) {
                super.updateItem(service, empty);
                if (empty || service == null) {
                    setText(null);
                } else {
                    String provider = service.getAssignedStaff() != null ?
                        service.getAssignedStaff().getName() : "Unassigned";
                    setText(String.format("%s | %s | %d min/week | %s",
                        service.getServiceType().getDisplayName(),
                        service.getDeliveryModel().getDisplayName(),
                        service.getMinutesPerWeek(),
                        provider));
                }
            }
        });

        // Populate list
        servicesListView.getItems().addAll(selected.getServices());

        // Action buttons
        Button addBtn = new Button("Add Service");
        addBtn.setOnAction(e -> {
            dialog.close();
            showServiceDialog(selected, null);
        });

        Button editBtn = new Button("Edit Service");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> {
            IEPService selectedService = servicesListView.getSelectionModel().getSelectedItem();
            if (selectedService != null) {
                dialog.close();
                showServiceDialog(selected, selectedService);
            }
        });

        Button deleteBtn = new Button("Delete Service");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> {
            IEPService selectedService = servicesListView.getSelectionModel().getSelectedItem();
            if (selectedService != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Service");
                confirm.setHeaderText("Confirm Service Deletion");
                confirm.setContentText("Are you sure you want to delete this service?\n\n" +
                    selectedService.getServiceType().getDisplayName());

                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    try {
                        selected.removeService(selectedService);
                        iepManagementService.updateIEP(selected);
                        servicesListView.getItems().remove(selectedService);
                        showSuccess("Service Deleted", "Service has been removed from the IEP.");
                        loadData();
                    } catch (Exception ex) {
                        log.error("Error deleting service", ex);
                        showError("Delete Error", "Failed to delete service: " + ex.getMessage());
                    }
                }
            }
        });

        // Enable/disable buttons based on selection
        servicesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);
        });

        // Layout
        HBox buttonBox = new HBox(10, addBtn, editBtn, deleteBtn);
        buttonBox.setStyle("-fx-padding: 10 0 0 0;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10, servicesListView, buttonBox);
        content.setStyle("-fx-padding: 10;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    @FXML
    private void handleActivateIEP() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            log.info("Activating IEP: {}", selected.getId());
            iepManagementService.activateIEP(selected.getId());
            showSuccess("IEP Activated", "IEP has been activated successfully.");
            loadData();
        } catch (Exception e) {
            log.error("Error activating IEP", e);
            showError("Activation Error", "Failed to activate IEP: " + e.getMessage());
        }
    }

    @FXML
    private void handleMarkForReview() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            log.info("Marking IEP for review: {}", selected.getId());
            iepManagementService.markForReview(selected.getId());
            showSuccess("Marked for Review", "IEP has been marked for review.");
            loadData();
        } catch (Exception e) {
            log.error("Error marking IEP for review", e);
            showError("Review Error", "Failed to mark IEP for review: " + e.getMessage());
        }
    }

    @FXML
    private void handleExpireIEP() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Expire IEP");
        confirmation.setHeaderText("Confirm IEP Expiration");
        confirmation.setContentText("Are you sure you want to expire this IEP?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                log.info("Expiring IEP: {}", selected.getId());
                iepManagementService.expireIEP(selected.getId());
                showSuccess("IEP Expired", "IEP has been expired.");
                loadData();
            } catch (Exception e) {
                log.error("Error expiring IEP", e);
                showError("Expiration Error", "Failed to expire IEP: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleGenerateIEPReport() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Generating report for IEP: {}", selected.getId());

        String reportContent = buildIEPReportContent(selected);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save IEP Report");
        String studentName = selected.getStudent() != null ?
            selected.getStudent().getFullName().replaceAll("[^a-zA-Z0-9]", "_") : "Unknown";
        fileChooser.setInitialFileName("IEP_Report_" + studentName + "_" +
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(iepTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(reportContent);
            log.info("Generated IEP report: {}", file.getAbsolutePath());
            showSuccess("Report Generated", "IEP report saved to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error generating IEP report", e);
            showError("Report Error", "Failed to generate IEP report: " + e.getMessage());
        }
    }

    @FXML
    private void handlePrintIEP() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Printing IEP: {}", selected.getId());

        String reportContent = buildIEPReportContent(selected);

        // Create a TextFlow for printing
        TextFlow textFlow = new TextFlow();
        Text text = new Text(reportContent);
        text.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 10px;");
        textFlow.getChildren().add(text);
        textFlow.setPrefWidth(550);

        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob != null) {
            boolean showDialog = printerJob.showPrintDialog(iepTable.getScene().getWindow());
            if (showDialog) {
                boolean success = printerJob.printPage(textFlow);
                if (success) {
                    printerJob.endJob();
                    log.info("IEP printed successfully");
                    showSuccess("Print Complete", "IEP has been sent to the printer.");
                } else {
                    showError("Print Error", "Failed to print IEP.");
                }
            }
        } else {
            showError("Print Error", "No printer available.");
        }
    }

    /**
     * Build formatted IEP report content for export/print
     */
    private String buildIEPReportContent(IEP iep) {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

        report.append("================================================================================\n");
        report.append("                    INDIVIDUALIZED EDUCATION PROGRAM (IEP)                      \n");
        report.append("                              CONFIDENTIAL DOCUMENT                              \n");
        report.append("================================================================================\n\n");

        report.append("Report Generated: ").append(LocalDate.now().format(dateFormatter)).append("\n\n");

        // Student Information
        report.append("STUDENT INFORMATION\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (iep.getStudent() != null) {
            report.append(String.format("%-20s: %s%n", "Name", iep.getStudent().getFullName()));
            report.append(String.format("%-20s: %s%n", "Student ID", iep.getStudent().getStudentId()));
            report.append(String.format("%-20s: %s%n", "Grade Level", iep.getStudent().getGradeLevel()));
        }
        report.append("\n");

        // IEP Information
        report.append("IEP STATUS INFORMATION\n");
        report.append("--------------------------------------------------------------------------------\n");
        report.append(String.format("%-20s: %s%n", "IEP Number", iep.getIepNumber() != null ? iep.getIepNumber() : "DRAFT"));
        report.append(String.format("%-20s: %s%n", "Status", iep.getStatus() != null ? iep.getStatus().getDisplayName() : "N/A"));
        report.append(String.format("%-20s: %s%n", "Start Date", iep.getStartDate() != null ? iep.getStartDate().format(dateFormatter) : "N/A"));
        report.append(String.format("%-20s: %s%n", "End Date", iep.getEndDate() != null ? iep.getEndDate().format(dateFormatter) : "N/A"));
        if (iep.getNextReviewDate() != null) {
            report.append(String.format("%-20s: %s%n", "Next Review", iep.getNextReviewDate().format(dateFormatter)));
        }
        report.append(String.format("%-20s: %s%n", "Case Manager", iep.getCaseManager() != null ? iep.getCaseManager() : "Unassigned"));
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), iep.getEndDate());
        report.append(String.format("%-20s: %s%n", "Days Remaining", daysRemaining < 0 ? "EXPIRED" : String.valueOf(daysRemaining)));
        report.append("\n");

        // Disability & Eligibility
        report.append("DISABILITY & ELIGIBILITY INFORMATION\n");
        report.append("--------------------------------------------------------------------------------\n");
        report.append(String.format("%-20s: %s%n", "Primary Disability", iep.getPrimaryDisability() != null ? iep.getPrimaryDisability() : "N/A"));
        report.append(String.format("%-20s: %s%n", "Eligibility Category", iep.getEligibilityCategory() != null ? iep.getEligibilityCategory() : "N/A"));
        report.append("\n");

        // Services
        report.append("SPECIAL EDUCATION SERVICES\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (iep.getServices() != null && !iep.getServices().isEmpty()) {
            int i = 1;
            for (IEPService service : iep.getServices()) {
                report.append(String.format("%d. %s%n", i++, service.getServiceType().getDisplayName()));
                report.append(String.format("   %-17s: %s%n", "Delivery Model", service.getDeliveryModel().getDisplayName()));
                report.append(String.format("   %-17s: %d minutes%n", "Minutes/Week", service.getMinutesPerWeek()));
                report.append(String.format("   %-17s: %s%n", "Provider",
                    service.getAssignedStaff() != null ? service.getAssignedStaff().getName() : "Unassigned"));
                if (service.getLocation() != null) {
                    report.append(String.format("   %-17s: %s%n", "Location", service.getLocation()));
                }
                report.append("\n");
            }
        } else {
            report.append("No services defined.\n\n");
        }

        // Goals
        report.append("ANNUAL GOALS\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (iep.getGoals() != null && !iep.getGoals().isEmpty()) {
            report.append(iep.getGoals()).append("\n");
        } else {
            report.append("No goals defined.\n");
        }
        report.append("\n");

        // Notes
        report.append("ADDITIONAL NOTES\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (iep.getNotes() != null && !iep.getNotes().isEmpty()) {
            report.append(iep.getNotes()).append("\n");
        } else {
            report.append("No additional notes.\n");
        }
        report.append("\n");

        report.append("================================================================================\n");
        report.append("                         END OF IEP REPORT                                      \n");
        report.append("================================================================================\n");

        return report.toString();
    }

    @FXML
    private void handleDeleteIEP() {
        IEP selected = iepTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete IEP");
        confirmation.setHeaderText("Confirm IEP Deletion");
        confirmation.setContentText("Are you sure you want to delete this IEP? This action cannot be undone.\n\n" +
            "Note: Only DRAFT IEPs can be deleted.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                log.info("Deleting IEP: {}", selected.getId());
                iepManagementService.deleteIEP(selected.getId());
                showSuccess("IEP Deleted", "IEP has been deleted.");
                loadData();
            } catch (Exception e) {
                log.error("Error deleting IEP", e);
                showError("Deletion Error", "Failed to delete IEP: " + e.getMessage());
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
