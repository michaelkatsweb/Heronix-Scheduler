package com.heronix.ui.controller;

import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.PullOutSchedule;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.service.PullOutSchedulingService;
import com.heronix.service.SPEDComplianceService;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.ui.controller.dialogs.SessionScheduleDialogController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pull-Out Scheduling Controller
 *
 * Controller for managing pull-out service scheduling with visual schedule grid
 * and conflict detection.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8C - November 21, 2025
 */
@Controller
@Slf4j
public class PullOutSchedulingController {

    @Autowired
    private PullOutSchedulingService pullOutSchedulingService;

    @Autowired
    private SPEDComplianceService complianceService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ApplicationContext applicationContext;

    // Filter Controls
    @FXML private ComboBox<String> serviceTypeFilter;
    @FXML private ComboBox<String> viewSelector;
    @FXML private ComboBox<Student> studentSelector;
    @FXML private ComboBox<Teacher> staffSelector;

    // Unscheduled Services Table
    @FXML private TableView<IEPService> unscheduledServicesTable;
    @FXML private TableColumn<IEPService, String> serviceStudentColumn;
    @FXML private TableColumn<IEPService, String> serviceTypeColumn;
    @FXML private TableColumn<IEPService, String> minutesRequiredColumn;
    @FXML private TableColumn<IEPService, String> providerColumn;
    @FXML private TableColumn<IEPService, String> scheduleActionColumn;

    // Schedule Grid
    @FXML private GridPane scheduleGrid;

    // Scheduled Sessions Table
    @FXML private TableView<PullOutSchedule> scheduledSessionsTable;
    @FXML private TableColumn<PullOutSchedule, String> sessionStudentColumn;
    @FXML private TableColumn<PullOutSchedule, String> sessionServiceColumn;
    @FXML private TableColumn<PullOutSchedule, String> sessionDayColumn;
    @FXML private TableColumn<PullOutSchedule, String> sessionTimeColumn;
    @FXML private TableColumn<PullOutSchedule, String> sessionDurationColumn;
    @FXML private TableColumn<PullOutSchedule, String> sessionStaffColumn;
    @FXML private TableColumn<PullOutSchedule, String> sessionLocationColumn;
    @FXML private TableColumn<PullOutSchedule, String> sessionActionsColumn;

    // Status Labels
    @FXML private Label unscheduledCountLabel;
    @FXML private Label totalServicesLabel;
    @FXML private Label scheduledServicesLabel;
    @FXML private Label conflictsLabel;
    @FXML private Label complianceLabel;

    // Context Menu
    @FXML private ContextMenu sessionContextMenu;

    // Data
    private ObservableList<IEPService> unscheduledServices = FXCollections.observableArrayList();
    private ObservableList<PullOutSchedule> scheduledSessions = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    public void initialize() {
        log.info("Initializing Pull-Out Scheduling Controller");
        setupTables();
        setupFilters();
        setupScheduleGrid();
        loadData();
    }

    private void setupTables() {
        // Unscheduled Services Table
        serviceStudentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudent().getFullName()));

        serviceTypeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getServiceType().getDisplayName()));

        minutesRequiredColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getMinutesPerWeek())));

        providerColumn.setCellValueFactory(data -> {
            Teacher staff = data.getValue().getAssignedStaff();
            return new SimpleStringProperty(staff != null ? staff.getName() : "Unassigned");
        });

        scheduleActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button scheduleBtn = new Button("Schedule");
            {
                scheduleBtn.setOnAction(e -> handleScheduleService(getTableRow().getItem()));
                scheduleBtn.getStyleClass().add("btn-link");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : scheduleBtn);
            }
        });

        unscheduledServicesTable.setItems(unscheduledServices);
        unscheduledServicesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Scheduled Sessions Table
        sessionStudentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudent().getFullName()));

        sessionServiceColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIepService().getServiceType().getDisplayName()));

        sessionDayColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDayOfWeek()));

        sessionTimeColumn.setCellValueFactory(data -> {
            LocalTime start = data.getValue().getStartTime();
            LocalTime end = data.getValue().getEndTime();
            return new SimpleStringProperty(start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT));
        });

        sessionDurationColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDurationMinutes() + " min"));

        sessionStaffColumn.setCellValueFactory(data -> {
            Teacher staff = data.getValue().getStaff();
            return new SimpleStringProperty(staff != null ? staff.getName() : "N/A");
        });

        sessionLocationColumn.setCellValueFactory(data -> {
            String location = data.getValue().getLocationDescription();
            if (location == null && data.getValue().getRoom() != null) {
                location = data.getValue().getRoom().getRoomNumber();
            }
            return new SimpleStringProperty(location != null ? location : "TBD");
        });

        sessionActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final HBox buttonBox = new HBox(5);
            private final Button editBtn = new Button("Edit");
            private final Button cancelBtn = new Button("X");
            {
                editBtn.setOnAction(e -> handleEditSession(getTableRow().getItem()));
                editBtn.getStyleClass().add("btn-link");
                cancelBtn.setOnAction(e -> handleCancelSession(getTableRow().getItem()));
                cancelBtn.getStyleClass().addAll("btn-link", "btn-danger");
                buttonBox.getChildren().addAll(editBtn, cancelBtn);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
            }
        });

        scheduledSessionsTable.setItems(scheduledSessions);

        // Context menu for sessions
        scheduledSessionsTable.setRowFactory(tv -> {
            TableRow<PullOutSchedule> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    scheduledSessionsTable.getSelectionModel().select(row.getItem());
                    sessionContextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    private void setupFilters() {
        // Service type filter
        serviceTypeFilter.getItems().addAll(
            "All Service Types",
            "Speech Therapy",
            "Occupational Therapy",
            "Physical Therapy",
            "Resource Room",
            "Reading Support",
            "Math Support",
            "Counseling"
        );
        serviceTypeFilter.setValue("All Service Types");
        serviceTypeFilter.setOnAction(e -> applyFilters());

        // View selector
        viewSelector.getItems().addAll(
            "All Sessions",
            "By Student",
            "By Staff"
        );
        viewSelector.setValue("All Sessions");
        viewSelector.setOnAction(e -> handleViewChange());

        // Student selector (will be populated dynamically)
        studentSelector.setDisable(true);

        // Staff selector (will be populated dynamically)
        staffSelector.setDisable(true);
    }

    private void setupScheduleGrid() {
        // Add time slots (8 AM to 3:30 PM in 30-minute increments)
        LocalTime time = LocalTime.of(8, 0);
        int row = 1;
        while (time.isBefore(LocalTime.of(15, 30))) {
            Label timeLabel = new Label(time.format(TIME_FORMAT));
            timeLabel.getStyleClass().add("grid-time");
            scheduleGrid.add(timeLabel, 0, row);

            // Add empty cells for each day
            for (int col = 1; col <= 5; col++) {
                VBox cell = new VBox();
                cell.getStyleClass().add("grid-cell");
                cell.setMinWidth(100);
                cell.setMinHeight(30);
                scheduleGrid.add(cell, col, row);
            }

            time = time.plusMinutes(30);
            row++;
        }
    }

    private void loadData() {
        try {
            log.info("Loading pull-out scheduling data");

            // Load unscheduled services
            List<IEPService> needsScheduling = complianceService.getServicesNeedingScheduling();
            unscheduledServices.clear();
            unscheduledServices.addAll(needsScheduling);
            unscheduledCountLabel.setText(needsScheduling.size() + " unscheduled");

            // Load scheduled sessions
            List<PullOutSchedule> allSessions = pullOutSchedulingService.findAllActiveSchedules();
            scheduledSessions.clear();
            scheduledSessions.addAll(allSessions);

            // Populate student/staff selectors
            populateSelectors();

            // Update schedule grid
            updateScheduleGrid();

            // Update status bar
            updateStatusBar();

            log.info("Loaded {} unscheduled services and {} scheduled sessions",
                needsScheduling.size(), allSessions.size());

        } catch (Exception e) {
            log.error("Error loading pull-out scheduling data", e);
            showError("Error Loading Data", "Failed to load scheduling data: " + e.getMessage());
        }
    }

    private void populateSelectors() {
        // Populate student selector
        List<Student> students = scheduledSessions.stream()
            .map(PullOutSchedule::getStudent)
            .distinct()
            .sorted((a, b) -> a.getFullName().compareTo(b.getFullName()))
            .collect(Collectors.toList());
        studentSelector.getItems().clear();
        studentSelector.getItems().addAll(students);

        // Populate staff selector
        List<Teacher> staffList = scheduledSessions.stream()
            .map(PullOutSchedule::getStaff)
            .filter(s -> s != null)
            .distinct()
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .collect(Collectors.toList());
        staffSelector.getItems().clear();
        staffSelector.getItems().addAll(staffList);
    }

    private void updateScheduleGrid() {
        // Clear existing session displays (but keep structure)
        for (int row = 1; row < scheduleGrid.getRowCount(); row++) {
            for (int col = 1; col <= 5; col++) {
                VBox cell = (VBox) getNodeFromGridPane(row, col);
                if (cell != null) {
                    cell.getChildren().clear();
                    cell.getStyleClass().removeAll("has-session", "has-conflict");
                }
            }
        }

        // Add sessions to grid
        for (PullOutSchedule session : scheduledSessions) {
            addSessionToGrid(session);
        }
    }

    private void addSessionToGrid(PullOutSchedule session) {
        int dayColumn = getDayColumn(session.getDayOfWeek());
        int startRow = getTimeRow(session.getStartTime());

        if (dayColumn > 0 && startRow > 0) {
            VBox cell = (VBox) getNodeFromGridPane(startRow, dayColumn);
            if (cell != null) {
                Label sessionLabel = new Label(
                    session.getStudent().getLastName() + " - " +
                    session.getIepService().getServiceType().getDisplayName());
                sessionLabel.getStyleClass().add("session-label");
                sessionLabel.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(sessionLabel);
                cell.getStyleClass().add("has-session");
            }
        }
    }

    private int getDayColumn(String dayOfWeek) {
        return switch (dayOfWeek.toUpperCase()) {
            case "MONDAY" -> 1;
            case "TUESDAY" -> 2;
            case "WEDNESDAY" -> 3;
            case "THURSDAY" -> 4;
            case "FRIDAY" -> 5;
            default -> 0;
        };
    }

    private int getTimeRow(LocalTime time) {
        int hourOffset = time.getHour() - 8;
        int minuteOffset = time.getMinute() / 30;
        return 1 + (hourOffset * 2) + minuteOffset;
    }

    private javafx.scene.Node getNodeFromGridPane(int row, int col) {
        for (javafx.scene.Node node : scheduleGrid.getChildren()) {
            Integer nodeRow = GridPane.getRowIndex(node);
            Integer nodeCol = GridPane.getColumnIndex(node);
            if (nodeRow != null && nodeCol != null && nodeRow == row && nodeCol == col) {
                return node;
            }
        }
        return null;
    }

    private void updateStatusBar() {
        int total = unscheduledServices.size() + scheduledSessions.size();
        int scheduled = scheduledSessions.size();

        totalServicesLabel.setText("Total Services: " + total);
        scheduledServicesLabel.setText("Scheduled: " + scheduled);

        // Count conflicts (simplified - would need proper conflict detection)
        conflictsLabel.setText("Conflicts: 0");

        // Compliance rate
        double compliance = total > 0 ? (scheduled * 100.0 / total) : 100.0;
        complianceLabel.setText(String.format("Compliance: %.1f%%", compliance));
    }

    private void applyFilters() {
        String selectedType = serviceTypeFilter.getValue();
        loadData(); // Reload and filter

        if (!"All Service Types".equals(selectedType)) {
            unscheduledServices.removeIf(service ->
                !service.getServiceType().getDisplayName().equals(selectedType));
        }
    }

    private void handleViewChange() {
        String view = viewSelector.getValue();
        studentSelector.setDisable(!"By Student".equals(view));
        staffSelector.setDisable(!"By Staff".equals(view));

        if ("By Student".equals(view)) {
            studentSelector.setOnAction(e -> filterByStudent());
        } else if ("By Staff".equals(view)) {
            staffSelector.setOnAction(e -> filterByStaff());
        } else {
            loadData();
        }
    }

    private void filterByStudent() {
        Student selected = studentSelector.getValue();
        if (selected != null) {
            scheduledSessions.removeIf(s ->
                !s.getStudent().getId().equals(selected.getId()));
            updateScheduleGrid();
        }
    }

    private void filterByStaff() {
        Teacher selected = staffSelector.getValue();
        if (selected != null) {
            scheduledSessions.removeIf(s ->
                s.getStaff() == null || !s.getStaff().getId().equals(selected.getId()));
            updateScheduleGrid();
        }
    }

    // Event Handlers

    @FXML
    private void handleRefresh() {
        log.info("Refreshing pull-out scheduling data");
        loadData();
    }

    @FXML
    private void handleAutoScheduleAll() {
        log.info("Auto-scheduling all unscheduled services");

        int scheduled = 0;
        int failed = 0;

        for (IEPService service : unscheduledServices) {
            try {
                pullOutSchedulingService.scheduleService(service.getId());
                scheduled++;
            } catch (Exception e) {
                log.warn("Failed to auto-schedule service {}: {}", service.getId(), e.getMessage());
                failed++;
            }
        }

        if (scheduled > 0) {
            showSuccess("Auto-Scheduling Complete",
                String.format("Scheduled %d services. %d could not be scheduled.", scheduled, failed));
            loadData();
        } else if (failed > 0) {
            showError("Auto-Scheduling Failed",
                "Could not find available time slots for any services. Please schedule manually.");
        } else {
            showInfo("Nothing to Schedule", "All services are already scheduled.");
        }
    }

    @FXML
    private void handleCheckConflicts() {
        log.info("Checking for scheduling conflicts");

        if (scheduledSessions.isEmpty()) {
            showInfo("No Schedules", "There are no scheduled sessions to check for conflicts.");
            return;
        }

        StringBuilder conflictReport = new StringBuilder();
        conflictReport.append("Conflict Analysis Report\n");
        conflictReport.append("========================\n\n");

        int studentConflicts = 0;
        int staffConflicts = 0;

        // Check each session against all others for overlaps
        for (int i = 0; i < scheduledSessions.size(); i++) {
            PullOutSchedule session1 = scheduledSessions.get(i);

            for (int j = i + 1; j < scheduledSessions.size(); j++) {
                PullOutSchedule session2 = scheduledSessions.get(j);

                // Check if same day
                if (!session1.getDayOfWeek().equals(session2.getDayOfWeek())) {
                    continue;
                }

                // Check for time overlap
                boolean overlaps = session1.getStartTime().isBefore(session2.getEndTime()) &&
                                  session2.getStartTime().isBefore(session1.getEndTime());

                if (!overlaps) continue;

                // Check for student conflict
                if (session1.getStudent() != null && session2.getStudent() != null &&
                    session1.getStudent().getId().equals(session2.getStudent().getId())) {
                    studentConflicts++;
                    conflictReport.append(String.format("STUDENT CONFLICT: %s%n", session1.getStudent().getFullName()));
                    conflictReport.append(String.format("  - %s: %s %s-%s%n",
                        session1.getServiceType(), session1.getDayOfWeek(),
                        session1.getStartTime(), session1.getEndTime()));
                    conflictReport.append(String.format("  - %s: %s %s-%s%n%n",
                        session2.getServiceType(), session2.getDayOfWeek(),
                        session2.getStartTime(), session2.getEndTime()));
                }

                // Check for staff conflict
                if (session1.getStaff() != null && session2.getStaff() != null &&
                    session1.getStaff().getId().equals(session2.getStaff().getId())) {
                    staffConflicts++;
                    conflictReport.append(String.format("STAFF CONFLICT: %s%n", session1.getStaff().getName()));
                    conflictReport.append(String.format("  - %s (%s): %s %s-%s%n",
                        session1.getStudent() != null ? session1.getStudent().getFullName() : "Unknown",
                        session1.getServiceType(), session1.getDayOfWeek(),
                        session1.getStartTime(), session1.getEndTime()));
                    conflictReport.append(String.format("  - %s (%s): %s %s-%s%n%n",
                        session2.getStudent() != null ? session2.getStudent().getFullName() : "Unknown",
                        session2.getServiceType(), session2.getDayOfWeek(),
                        session2.getStartTime(), session2.getEndTime()));
                }
            }
        }

        if (studentConflicts == 0 && staffConflicts == 0) {
            conflictsLabel.setText("Conflicts: 0");
            showSuccess("No Conflicts Found", "All scheduled sessions are conflict-free!");
        } else {
            conflictsLabel.setText("Conflicts: " + (studentConflicts + staffConflicts));
            conflictReport.insert(0, String.format("Found %d student conflicts and %d staff conflicts.%n%n",
                studentConflicts, staffConflicts));

            // Show in a scrollable dialog
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Conflicts Detected");
            alert.setHeaderText("Scheduling Conflicts Found");

            TextArea textArea = new TextArea(conflictReport.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefRowCount(15);
            textArea.setPrefColumnCount(50);

            alert.getDialogPane().setExpandableContent(textArea);
            alert.getDialogPane().setExpanded(true);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleScheduleSelected() {
        List<IEPService> selected = unscheduledServicesTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showInfo("No Selection", "Please select one or more services to schedule.");
            return;
        }

        int scheduled = 0;
        for (IEPService service : selected) {
            try {
                pullOutSchedulingService.scheduleService(service.getId());
                scheduled++;
            } catch (Exception e) {
                log.warn("Failed to schedule service {}: {}", service.getId(), e.getMessage());
            }
        }

        showSuccess("Scheduling Complete", "Scheduled " + scheduled + " of " + selected.size() + " services.");
        loadData();
    }

    @FXML
    private void handleFindBestSlots() {
        IEPService selected = unscheduledServicesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("No Selection", "Please select a service to find best time slots.");
            return;
        }

        try {
            var bestSlot = pullOutSchedulingService.findBestTimeSlot(selected.getId());
            if (bestSlot.isPresent()) {
                var slot = bestSlot.get();
                showInfo("Best Time Slot Found",
                    String.format("Best slot: %s from %s to %s (Score: %.2f)",
                        slot.dayOfWeek,
                        slot.startTime.format(TIME_FORMAT),
                        slot.endTime.format(TIME_FORMAT),
                        slot.score));
            } else {
                showInfo("No Slots Available", "No available time slots found for this service.");
            }
        } catch (Exception e) {
            log.error("Error finding best slot", e);
            showError("Error", "Failed to find best time slot: " + e.getMessage());
        }
    }

    private void handleScheduleService(IEPService service) {
        if (service == null) return;
        log.info("Opening schedule dialog for service: {}", service.getId());
        showSessionDialog(service, null);
    }

    private void showSessionDialog(IEPService service, PullOutSchedule schedule) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/SessionScheduleDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            DialogPane dialogPane = loader.load();

            SessionScheduleDialogController controller = loader.getController();
            if (schedule != null) {
                controller.setSchedule(schedule);
            } else if (service != null) {
                controller.setService(service);
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(schedule == null ? "Schedule Session" : "Edit Session");

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    List<String> errors = controller.validate();
                    if (!errors.isEmpty()) {
                        showError("Validation Error", String.join("\n", errors));
                        return;
                    }

                    try {
                        PullOutSchedule savedSchedule = controller.getSchedule();
                        if (schedule == null) {
                            pullOutSchedulingService.createSchedule(savedSchedule);
                            showSuccess("Session Scheduled", "Session has been scheduled successfully.");
                        } else {
                            pullOutSchedulingService.updateSchedule(savedSchedule);
                            showSuccess("Session Updated", "Session has been updated successfully.");
                        }
                        loadData();
                    } catch (Exception e) {
                        log.error("Error saving session", e);
                        showError("Save Error", "Failed to save session: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            log.error("Error loading session dialog", e);
            showError("Dialog Error", "Failed to open session dialog: " + e.getMessage());
        }
    }

    // Context Menu Handlers

    @FXML
    private void handleViewSessionDetails() {
        PullOutSchedule selected = scheduledSessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Viewing session details: {}", selected.getId());

        StringBuilder details = new StringBuilder();
        details.append("Session Details\n");
        details.append("===============\n\n");
        details.append("Student: ").append(selected.getStudent() != null ? selected.getStudent().getFullName() : "N/A").append("\n");
        details.append("Service: ").append(selected.getServiceType()).append("\n");
        details.append("Provider: ").append(selected.getStaff() != null ? selected.getStaff().getName() : "Unassigned").append("\n");
        details.append("Day: ").append(selected.getDayOfWeek()).append("\n");
        details.append("Time: ").append(selected.getStartTime().format(TIME_FORMAT)).append(" - ")
               .append(selected.getEndTime().format(TIME_FORMAT)).append("\n");
        details.append("Duration: ").append(selected.getDurationMinutes()).append(" minutes\n");
        details.append("Location: ").append(selected.getLocationDisplay()).append("\n");
        details.append("Status: ").append(selected.getStatus()).append("\n");
        details.append("Recurring: ").append(selected.getRecurring() ? "Yes" : "No").append("\n");
        details.append("Start Date: ").append(selected.getStartDate() != null ? selected.getStartDate().toString() : "N/A").append("\n");
        details.append("End Date: ").append(selected.getEndDate() != null ? selected.getEndDate().toString() : "Ongoing").append("\n");

        if (selected.getNotes() != null && !selected.getNotes().isEmpty()) {
            details.append("\nNotes:\n").append(selected.getNotes());
        }

        if (selected.getIepService() != null) {
            IEPService service = selected.getIepService();
            details.append("\n\nService Compliance\n");
            details.append("------------------\n");
            details.append("Required: ").append(service.getMinutesPerWeek()).append(" min/week\n");
            details.append("Scheduled: ").append(service.getScheduledMinutesPerWeek()).append(" min/week\n");
            details.append("Compliance: ").append(String.format("%.1f%%", service.getCompliancePercentage()));
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Details");
        alert.setHeaderText(selected.getStudent() != null ? selected.getStudent().getFullName() + " - " + selected.getServiceType() : "Session Details");

        TextArea textArea = new TextArea(details.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(18);
        textArea.setPrefColumnCount(40);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    @FXML
    private void handleEditSession() {
        PullOutSchedule selected = scheduledSessionsTable.getSelectionModel().getSelectedItem();
        handleEditSession(selected);
    }

    private void handleEditSession(PullOutSchedule session) {
        if (session == null) return;
        log.info("Editing session: {}", session.getId());
        showSessionDialog(null, session);
    }

    @FXML
    private void handleRescheduleSession() {
        PullOutSchedule selected = scheduledSessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Rescheduling session: {}", selected.getId());

        // Open the edit dialog for rescheduling
        showSessionDialog(null, selected);
    }

    @FXML
    private void handleFindAlternative() {
        PullOutSchedule selected = scheduledSessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log.info("Finding alternative slot for session: {}", selected.getId());

        if (selected.getIepService() == null) {
            showError("Error", "Cannot find alternative - no IEP service associated with this session.");
            return;
        }

        try {
            var bestSlot = pullOutSchedulingService.findBestTimeSlot(selected.getIepService().getId());
            if (bestSlot.isPresent()) {
                var slot = bestSlot.get();

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Alternative Time Slot Found");
                alert.setHeaderText("Found an alternative time slot");
                alert.setContentText(String.format(
                    "Current: %s %s - %s\n\nAlternative: %s %s - %s (Score: %.2f)\n\nWould you like to reschedule to this slot?",
                    selected.getDayOfWeek(),
                    selected.getStartTime().format(TIME_FORMAT),
                    selected.getEndTime().format(TIME_FORMAT),
                    slot.dayOfWeek,
                    slot.startTime.format(TIME_FORMAT),
                    slot.endTime.format(TIME_FORMAT),
                    slot.score
                ));

                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            pullOutSchedulingService.reschedule(
                                selected.getId(),
                                slot.dayOfWeek,
                                slot.startTime,
                                slot.endTime
                            );
                            showSuccess("Session Rescheduled", "Session has been moved to the new time slot.");
                            loadData();
                        } catch (Exception e) {
                            log.error("Error rescheduling session", e);
                            showError("Reschedule Error", "Failed to reschedule: " + e.getMessage());
                        }
                    }
                });
            } else {
                showInfo("No Alternatives", "No alternative time slots are available for this session.");
            }
        } catch (Exception e) {
            log.error("Error finding alternative slot", e);
            showError("Error", "Failed to find alternative slot: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelSession() {
        PullOutSchedule selected = scheduledSessionsTable.getSelectionModel().getSelectedItem();
        handleCancelSession(selected);
    }

    private void handleCancelSession(PullOutSchedule session) {
        if (session == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Session");
        confirmation.setHeaderText("Confirm Session Cancellation");
        confirmation.setContentText("Are you sure you want to cancel this scheduled session?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                log.info("Cancelling session: {}", session.getId());
                pullOutSchedulingService.cancelSchedule(session.getId());
                showSuccess("Session Cancelled", "Session has been cancelled.");
                loadData();
            } catch (Exception e) {
                log.error("Error cancelling session", e);
                showError("Cancellation Error", "Failed to cancel session: " + e.getMessage());
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
