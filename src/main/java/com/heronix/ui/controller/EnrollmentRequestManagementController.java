package com.heronix.ui.controller;

import com.heronix.model.domain.CourseEnrollmentRequest;
import com.heronix.model.enums.EnrollmentRequestStatus;
import com.heronix.service.EnrollmentRequestManagementService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enrollment Request Management Controller
 *
 * Provides comprehensive UI for managing enrollment requests:
 * - View all requests in table
 * - Search and filter requests
 * - View detailed statistics
 * - Edit request properties
 * - Cancel or delete requests
 * - Bulk operations
 *
 * Used by administrators to review and manage enrollment requests
 * before running the assignment wizard.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - November 21, 2025
 */
@Slf4j
@Component
public class EnrollmentRequestManagementController {

    @Autowired
    private EnrollmentRequestManagementService managementService;

    // ========================================================================
    // FXML COMPONENTS - Table and Columns
    // ========================================================================

    @FXML
    private TableView<CourseEnrollmentRequest> requestsTable;

    @FXML
    private TableColumn<CourseEnrollmentRequest, Boolean> selectColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> studentIdColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> studentNameColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> courseCodeColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> courseNameColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, Integer> preferenceColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, Integer> priorityColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> statusColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> dateColumn;

    // ========================================================================
    // FXML COMPONENTS - Search and Filters
    // ========================================================================

    @FXML
    private TextField searchStudentField;

    @FXML
    private TextField searchCourseField;

    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private ComboBox<String> gradeFilterCombo;

    @FXML
    private ComboBox<String> preferenceFilterCombo;

    // ========================================================================
    // FXML COMPONENTS - Statistics
    // ========================================================================

    @FXML
    private Label totalRequestsLabel;

    @FXML
    private Label pendingRequestsLabel;

    @FXML
    private Label assignedRequestsLabel;

    @FXML
    private Label cancelledRequestsLabel;

    @FXML
    private Label waitlistRequestsLabel;

    // ========================================================================
    // FXML COMPONENTS - Buttons
    // ========================================================================

    @FXML
    private Button refreshButton;

    @FXML
    private Button editButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearFiltersButton;

    @FXML
    private Button closeButton;

    // ========================================================================
    // STATE
    // ========================================================================

    private ObservableList<CourseEnrollmentRequest> requestsList;
    private ObservableList<CourseEnrollmentRequest> filteredList;
    private Set<Long> selectedRequestIds = new HashSet<>();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing Enrollment Request Management Controller");

        setupTable();
        setupFilters();
        setupButtons();
        loadData();
    }

    /**
     * Setup table columns and selection
     */
    private void setupTable() {
        // Selection column with checkboxes
        selectColumn.setCellValueFactory(cellData -> {
            CourseEnrollmentRequest request = cellData.getValue();
            SimpleBooleanProperty property = new SimpleBooleanProperty(
                selectedRequestIds.contains(request.getId())
            );
            // Store selection state in our set
            property.addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    selectedRequestIds.add(request.getId());
                } else {
                    selectedRequestIds.remove(request.getId());
                }
                updateButtonStates();
            });
            return property;
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // Student ID column
        studentIdColumn.setCellValueFactory(cellData -> {
            String studentId = cellData.getValue().getStudent() != null ?
                cellData.getValue().getStudent().getStudentId() : "N/A";
            return new SimpleStringProperty(studentId);
        });

        // Student name column
        studentNameColumn.setCellValueFactory(cellData -> {
            String studentName = cellData.getValue().getStudent() != null ?
                cellData.getValue().getStudent().getFullName() : "N/A";
            return new SimpleStringProperty(studentName);
        });

        // Course code column
        courseCodeColumn.setCellValueFactory(cellData -> {
            String courseCode = cellData.getValue().getCourse() != null ?
                cellData.getValue().getCourse().getCourseCode() : "N/A";
            return new SimpleStringProperty(courseCode);
        });

        // Course name column
        courseNameColumn.setCellValueFactory(cellData -> {
            String courseName = cellData.getValue().getCourse() != null ?
                cellData.getValue().getCourse().getCourseName() : "N/A";
            return new SimpleStringProperty(courseName);
        });

        // Preference rank column
        preferenceColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().getPreferenceRank()).asObject());

        // Priority score column
        priorityColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().getPriorityScore()).asObject());

        // Status column
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getRequestStatus() != null ?
                cellData.getValue().getRequestStatus().name() : "UNKNOWN";
            return new SimpleStringProperty(status);
        });

        // Date column
        dateColumn.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getCreatedAt() != null ?
                cellData.getValue().getCreatedAt().format(DATE_FORMATTER) : "N/A";
            return new SimpleStringProperty(date);
        });

        // Enable table editing for checkboxes
        requestsTable.setEditable(true);

        // Enable selection listener
        requestsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> updateButtonStates()
        );
    }

    /**
     * Setup filter components
     */
    private void setupFilters() {
        // Status filter
        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "All",
            "PENDING",
            "ASSIGNED",
            "CANCELLED",
            "WAITLIST"
        ));
        statusFilterCombo.setValue("All");

        // Grade filter
        List<String> gradeOptions = new ArrayList<>();
        gradeOptions.add("All");
        for (int i = 9; i <= 12; i++) {
            gradeOptions.add("Grade " + i);
        }
        gradeFilterCombo.setItems(FXCollections.observableArrayList(gradeOptions));
        gradeFilterCombo.setValue("All");

        // Preference filter
        preferenceFilterCombo.setItems(FXCollections.observableArrayList(
            "All",
            "1st Choice",
            "2nd Choice",
            "3rd Choice",
            "4th Choice"
        ));
        preferenceFilterCombo.setValue("All");

        // Add listeners for auto-filter
        searchStudentField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchCourseField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        gradeFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        preferenceFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Setup button states
     */
    private void setupButtons() {
        editButton.setDisable(true);
        cancelButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    /**
     * Update button enabled/disabled states based on selection
     */
    private void updateButtonStates() {
        boolean hasSelection = !requestsTable.getSelectionModel().isEmpty();
        editButton.setDisable(!hasSelection);

        List<CourseEnrollmentRequest> selectedRequests = getSelectedRequests();
        boolean hasCheckedItems = !selectedRequests.isEmpty();
        cancelButton.setDisable(!hasCheckedItems);
        deleteButton.setDisable(!hasCheckedItems);
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load all data (requests and statistics)
     */
    private void loadData() {
        loadRequests();
        loadStatistics();
    }

    /**
     * Load enrollment requests
     */
    private void loadRequests() {
        log.info("Loading enrollment requests");

        Task<List<CourseEnrollmentRequest>> task = new Task<>() {
            @Override
            protected List<CourseEnrollmentRequest> call() {
                return managementService.getAllRequests();
            }

            @Override
            protected void succeeded() {
                List<CourseEnrollmentRequest> requests = getValue();
                requestsList = FXCollections.observableArrayList(requests);
                filteredList = FXCollections.observableArrayList(requests);
                requestsTable.setItems(filteredList);
                log.info("Loaded {} enrollment requests", requests.size());
            }

            @Override
            protected void failed() {
                log.error("Failed to load enrollment requests", getException());
                showError("Failed to load enrollment requests: " + getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        log.info("Loading enrollment request statistics");

        Task<EnrollmentRequestManagementService.RequestStatistics> task = new Task<>() {
            @Override
            protected EnrollmentRequestManagementService.RequestStatistics call() {
                return managementService.getStatistics();
            }

            @Override
            protected void succeeded() {
                EnrollmentRequestManagementService.RequestStatistics stats = getValue();
                updateStatisticsDisplay(stats);
            }

            @Override
            protected void failed() {
                log.error("Failed to load statistics", getException());
            }
        };

        new Thread(task).start();
    }

    /**
     * Update statistics labels
     */
    private void updateStatisticsDisplay(EnrollmentRequestManagementService.RequestStatistics stats) {
        Platform.runLater(() -> {
            totalRequestsLabel.setText(String.valueOf(stats.getTotalRequests()));
            pendingRequestsLabel.setText(String.valueOf(stats.getPendingRequests()));
            assignedRequestsLabel.setText(String.valueOf(stats.getAssignedRequests()));
            cancelledRequestsLabel.setText(String.valueOf(stats.getCancelledRequests()));
            waitlistRequestsLabel.setText(String.valueOf(stats.getWaitlistRequests()));
        });
    }

    // ========================================================================
    // FILTERING AND SEARCH
    // ========================================================================

    /**
     * Apply current filters to the requests list
     */
    private void applyFilters() {
        if (requestsList == null) {
            return;
        }

        EnrollmentRequestManagementService.SearchCriteria criteria =
            new EnrollmentRequestManagementService.SearchCriteria();

        // Student search
        String studentSearch = searchStudentField.getText();
        if (studentSearch != null && !studentSearch.trim().isEmpty()) {
            criteria.setStudentNameOrId(studentSearch.trim());
        }

        // Course search
        String courseSearch = searchCourseField.getText();
        if (courseSearch != null && !courseSearch.trim().isEmpty()) {
            criteria.setCourseCodeOrName(courseSearch.trim());
        }

        // Status filter
        String statusValue = statusFilterCombo.getValue();
        if (statusValue != null && !statusValue.equals("All")) {
            criteria.setStatus(EnrollmentRequestStatus.valueOf(statusValue));
        }

        // Grade filter
        String gradeValue = gradeFilterCombo.getValue();
        if (gradeValue != null && !gradeValue.equals("All")) {
            int grade = Integer.parseInt(gradeValue.replace("Grade ", ""));
            criteria.setGradeLevel(grade);
        }

        // Preference filter
        String prefValue = preferenceFilterCombo.getValue();
        if (prefValue != null && !prefValue.equals("All")) {
            int preference = Integer.parseInt(prefValue.substring(0, 1));
            criteria.setPreferenceRank(preference);
        }

        // Apply filters
        List<CourseEnrollmentRequest> filtered = managementService.searchRequests(criteria);
        filteredList = FXCollections.observableArrayList(filtered);
        requestsTable.setItems(filteredList);

        log.info("Filters applied: {} results", filtered.size());
    }

    // ========================================================================
    // BUTTON HANDLERS
    // ========================================================================

    /**
     * Handle Refresh button
     */
    @FXML
    private void handleRefresh() {
        log.info("Refreshing data");
        loadData();
    }

    /**
     * Handle Clear Filters button
     */
    @FXML
    private void handleClearFilters() {
        searchStudentField.clear();
        searchCourseField.clear();
        statusFilterCombo.setValue("All");
        gradeFilterCombo.setValue("All");
        preferenceFilterCombo.setValue("All");
        applyFilters();
    }

    /**
     * Handle Edit button
     */
    @FXML
    private void handleEdit() {
        CourseEnrollmentRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a request to edit");
            return;
        }

        showEditDialog(selected);
    }

    /**
     * Handle Cancel button (bulk cancel)
     */
    @FXML
    private void handleCancel() {
        List<CourseEnrollmentRequest> selectedRequests = getSelectedRequests();
        if (selectedRequests.isEmpty()) {
            showWarning("Please check the boxes next to requests you want to cancel");
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText("Cancel Enrollment Requests");
        confirm.setContentText(String.format(
            "Are you sure you want to cancel %d enrollment request(s)?\\n\\n" +
            "This action cannot be undone.",
            selectedRequests.size()
        ));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executeBulkCancel(selectedRequests);
            }
        });
    }

    /**
     * Handle Delete button (bulk delete)
     */
    @FXML
    private void handleDelete() {
        List<CourseEnrollmentRequest> selectedRequests = getSelectedRequests();
        if (selectedRequests.isEmpty()) {
            showWarning("Please check the boxes next to requests you want to delete");
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Enrollment Requests");
        confirm.setContentText(String.format(
            "Are you sure you want to PERMANENTLY DELETE %d enrollment request(s)?\\n\\n" +
            "This action CANNOT be undone!",
            selectedRequests.size()
        ));

        ButtonType deleteButton = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteButton, cancelButtonType);

        confirm.showAndWait().ifPresent(response -> {
            if (response == deleteButton) {
                executeBulkDelete(selectedRequests);
            }
        });
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
    // EDIT DIALOG
    // ========================================================================

    /**
     * Show edit dialog for a request
     */
    private void showEditDialog(CourseEnrollmentRequest request) {
        Dialog<CourseEnrollmentRequest> dialog = new Dialog<>();
        dialog.setTitle("Edit Enrollment Request");
        dialog.setHeaderText("Edit Request for " + request.getStudent().getFullName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Student info (read-only)
        TextField studentField = new TextField(request.getStudent().getFullName());
        studentField.setEditable(false);

        // Course info (read-only)
        TextField courseField = new TextField(
            request.getCourse().getCourseCode() + " - " + request.getCourse().getCourseName()
        );
        courseField.setEditable(false);

        // Preference rank (editable)
        ComboBox<Integer> preferenceCombo = new ComboBox<>();
        preferenceCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
        preferenceCombo.setValue(request.getPreferenceRank());

        // Priority score (editable)
        TextField priorityField = new TextField(String.valueOf(request.getPriorityScore()));

        // Status (editable)
        ComboBox<EnrollmentRequestStatus> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList(EnrollmentRequestStatus.values()));
        statusCombo.setValue(request.getRequestStatus());

        grid.add(new Label("Student:"), 0, 0);
        grid.add(studentField, 1, 0);
        grid.add(new Label("Course:"), 0, 1);
        grid.add(courseField, 1, 1);
        grid.add(new Label("Preference Rank:"), 0, 2);
        grid.add(preferenceCombo, 1, 2);
        grid.add(new Label("Priority Score:"), 0, 3);
        grid.add(priorityField, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    request.setPreferenceRank(preferenceCombo.getValue());
                    request.setPriorityScore(Integer.parseInt(priorityField.getText()));
                    request.setRequestStatus(statusCombo.getValue());
                    return request;
                } catch (NumberFormatException e) {
                    showError("Invalid priority score. Must be a number.");
                    return null;
                }
            }
            return null;
        });

        Optional<CourseEnrollmentRequest> result = dialog.showAndWait();
        result.ifPresent(this::saveRequestUpdate);
    }

    /**
     * Save request update
     */
    private void saveRequestUpdate(CourseEnrollmentRequest request) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                managementService.updateRequest(request);
                return null;
            }

            @Override
            protected void succeeded() {
                showInfo("Request updated successfully");
                loadData();
            }

            @Override
            protected void failed() {
                showError("Failed to update request: " + getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    // ========================================================================
    // BULK OPERATIONS
    // ========================================================================

    /**
     * Get list of checked requests
     */
    private List<CourseEnrollmentRequest> getSelectedRequests() {
        return requestsTable.getItems().stream()
            .filter(r -> selectedRequestIds.contains(r.getId()))
            .collect(Collectors.toList());
    }

    /**
     * Execute bulk cancel
     */
    private void executeBulkCancel(List<CourseEnrollmentRequest> requests) {
        List<Long> requestIds = requests.stream()
            .map(CourseEnrollmentRequest::getId)
            .collect(Collectors.toList());

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return managementService.bulkCancelRequests(requestIds, "Administrator cancelled");
            }

            @Override
            protected void succeeded() {
                int count = getValue();
                showInfo(String.format("Successfully cancelled %d of %d requests", count, requests.size()));
                loadData();
            }

            @Override
            protected void failed() {
                showError("Failed to cancel requests: " + getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    /**
     * Execute bulk delete
     */
    private void executeBulkDelete(List<CourseEnrollmentRequest> requests) {
        List<Long> requestIds = requests.stream()
            .map(CourseEnrollmentRequest::getId)
            .collect(Collectors.toList());

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return managementService.bulkDeleteRequests(requestIds);
            }

            @Override
            protected void succeeded() {
                int count = getValue();
                showInfo(String.format("Successfully deleted %d of %d requests", count, requests.size()));
                loadData();
            }

            @Override
            protected void failed() {
                showError("Failed to delete requests: " + getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
