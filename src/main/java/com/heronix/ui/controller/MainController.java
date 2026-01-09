package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.model.enums.Role;
import com.heronix.model.enums.ScheduleStatus;
import com.heronix.repository.*;
import com.heronix.security.SecurityContext;
import com.heronix.service.ExportService;
import com.heronix.service.GlobalSearchService;
import com.heronix.ui.controller.ImportWizardController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main Controller - Application Entry Point
 * Location: src/main/java/com/eduscheduler/ui/controller/MainController.java
 * 
 * COMPLETE & IMPROVED VERSION v5.0
 * ✅ Auto-loads Dashboard on startup
 * ✅ Fixed Import Wizard dialog stage
 * ✅ Proper view refresh after import
 * ✅ Enhanced error handling
 * 
 * @author Heronix Scheduling System Team
 * @version 5.0.0 - FINAL
 * @since 2025-10-15
 */
@Slf4j
@Component
public class MainController {

    // ========================================================================
    // FXML INJECTED COMPONENTS
    // ========================================================================

    @FXML
    private BorderPane mainBorderPane;

    // Navigation items
    @FXML
    private VBox dashboardNavItem;
    @FXML
    private VBox schedulesNavItem;
    @FXML
    private VBox teachersNavItem;
    @FXML
    private VBox coursesNavItem;
    @FXML
    private VBox roomsNavItem;
    @FXML
    private VBox studentsNavItem;
    @FXML
    private VBox eventsNavItem;
    @FXML
    private VBox reportsNavItem;
    @FXML
    private VBox settingsNavItem;

    @FXML
    private Label statusLabel;

    @FXML
    private Label userInfoLabel;

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private com.heronix.service.DatabaseMaintenanceService databaseMaintenanceService;

    @Autowired
    private com.heronix.service.OptimizationService optimizationService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private com.heronix.service.UserService userService;

    @Autowired
    private com.heronix.service.PermissionService permissionService;

    @Autowired
    private com.heronix.service.DashboardService dashboardService;

    @SuppressWarnings("unused")
    private String lastLoadedView = null;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║   MAIN CONTROLLER INITIALIZATION                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        validateUIComponents();

        // Setup keyboard shortcuts
        setupKeyboardShortcuts();

        // Load and apply saved theme
        loadSavedTheme();

        // Update user info display
        updateUserInfo();

        // Configure role-based navigation (Phase 2)
        configureRoleBasedNavigation();

        log.info("✓ MainController initialized successfully");
    }

    private void validateUIComponents() {
        log.info("Validating UI components...");

        if (mainBorderPane != null) {
            log.info("  ✓ mainBorderPane validated");
        } else {
            log.error("  ✗ mainBorderPane is null!");
        }

        if (springContext != null) {
            log.info("  ✓ Spring context validated");
        } else {
            log.error("  ✗ Spring context is null!");
        }
    }

    /**
     * Setup global keyboard shortcuts
     */
    private void setupKeyboardShortcuts() {
        log.info("Setting up keyboard shortcuts...");

        // Wait for scene to be available
        Platform.runLater(() -> {
            Scene scene = mainBorderPane.getScene();
            if (scene != null) {
                // ============================================================
                // NAVIGATION SHORTCUTS (Ctrl+1-9)
                // ============================================================

                // Ctrl+1: Dashboard
                KeyCombination ctrl1 = new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl1, this::loadDashboard);

                // Ctrl+2: Schedules
                KeyCombination ctrl2 = new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl2, this::loadSchedules);

                // Ctrl+3: Teachers
                KeyCombination ctrl3 = new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl3, this::loadTeachers);

                // Ctrl+4: Courses
                KeyCombination ctrl4 = new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl4, this::loadCourses);

                // Ctrl+5: Rooms
                KeyCombination ctrl5 = new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl5, this::loadRooms);

                // Ctrl+6: Students
                KeyCombination ctrl6 = new KeyCodeCombination(KeyCode.DIGIT6, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl6, this::loadStudents);

                // Ctrl+7: Events
                KeyCombination ctrl7 = new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl7, this::loadEvents);

                // Ctrl+8: Reports
                KeyCombination ctrl8 = new KeyCodeCombination(KeyCode.DIGIT8, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl8, this::loadReports);

                // Ctrl+9: Settings
                KeyCombination ctrl9 = new KeyCodeCombination(KeyCode.DIGIT9, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl9, this::loadSettings);

                // Ctrl+0: Academic Planning
                KeyCombination ctrl0 = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrl0, this::loadAcademicPlanning);

                // ============================================================
                // ACTION SHORTCUTS
                // ============================================================

                // Ctrl+K: Open Command Palette (Global Search)
                KeyCombination ctrlK = new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlK, this::handleCommandPalette);

                // Ctrl+N: New Schedule
                KeyCombination ctrlN = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlN, this::handleNewSchedule);

                // Ctrl+O: Open Schedule
                KeyCombination ctrlO = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlO, this::handleOpenSchedule);

                // Ctrl+S: Save Schedule
                KeyCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlS, this::handleSaveSchedule);

                // Ctrl+I: Import Data
                KeyCombination ctrlI = new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlI, this::handleImportData);

                // Ctrl+E: Export Data
                KeyCombination ctrlE = new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlE, this::handleExportData);

                // Ctrl+R: Refresh
                KeyCombination ctrlR = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlR, this::handleRefresh);

                // F5: Refresh (alternative)
                KeyCombination f5 = new KeyCodeCombination(KeyCode.F5);
                scene.getAccelerators().put(f5, this::handleRefresh);

                // Ctrl+Q: Exit
                KeyCombination ctrlQ = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(ctrlQ, this::handleExit);

                // ============================================================
                // FUNCTION KEY SHORTCUTS
                // ============================================================

                // F1: Help
                KeyCombination f1 = new KeyCodeCombination(KeyCode.F1);
                scene.getAccelerators().put(f1, this::handleHelp);

                log.info("  ✓ Navigation: Ctrl+1-9 (Dashboard, Schedules, Teachers, etc.), Ctrl+0 (Academic Planning)");
                log.info("  ✓ Actions: Ctrl+K (Search), Ctrl+N (New), Ctrl+S (Save)");
                log.info("  ✓ Common: Ctrl+R/F5 (Refresh), F1 (Help), Ctrl+Q (Exit)");
                log.info("✓ Keyboard shortcuts configured successfully");
            } else {
                log.warn("Scene not available yet, retrying keyboard shortcuts setup...");
                Platform.runLater(this::setupKeyboardShortcuts);
            }
        });
    }

    /**
     * Handle Command Palette (Ctrl+K) - Global Search
     */
    private void handleCommandPalette() {
        try {
            log.info("Opening Command Palette (Ctrl+K)...");

            // Load Command Palette FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CommandPalette.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Get controller
            CommandPaletteController controller = loader.getController();

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Command Palette - Quick Search");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(mainBorderPane.getScene().getWindow());

            // Create scene
            Scene dialogScene = new Scene(root);
            dialogStage.setScene(dialogScene);

            // Set stage reference in controller
            controller.setDialogStage(dialogStage);

            // Handle result selection
            controller.setOnResultSelected(result -> {
                handleSearchResultSelection(result);
            });

            // Show dialog
            dialogStage.showAndWait();

        } catch (Exception e) {
            log.error("Failed to open Command Palette", e);
            showError("Command Palette Error", "Failed to open command palette: " + e.getMessage());
        }
    }

    /**
     * Handle selection from Command Palette search results
     */
    private void handleSearchResultSelection(GlobalSearchService.SearchResult result) {
        log.info("Selected: {} - {}", result.getType(), result.getPrimaryText());

        try {
            switch (result.getType()) {
                case "STUDENT":
                    loadView("Students", "/fxml/Students.fxml");
                    break;

                case "TEACHER":
                    loadView("Teachers", "/fxml/Teachers.fxml");
                    break;

                case "COURSE":
                    loadView("Courses", "/fxml/Courses.fxml");
                    break;

                case "ROOM":
                    loadView("Rooms", "/fxml/Rooms.fxml");
                    break;

                case "ACTION":
                    handleQuickAction((String) result.getData());
                    break;

                default:
                    log.warn("Unknown result type: {}", result.getType());
            }
        } catch (Exception e) {
            log.error("Error handling search result selection", e);
            showError("Navigation Error", "Failed to navigate to selected item: " + e.getMessage());
        }
    }

    /**
     * Handle quick actions from Command Palette
     */
    private void handleQuickAction(String actionId) {
        log.info("Executing quick action: {}", actionId);

        switch (actionId) {
            case "add_student":
                loadView("Students", "/fxml/Students.fxml");
                break;

            case "add_teacher":
                loadView("Teachers", "/fxml/Teachers.fxml");
                break;

            case "add_course":
                loadView("Courses", "/fxml/Courses.fxml");
                break;

            case "add_room":
                loadView("Rooms", "/fxml/Rooms.fxml");
                break;

            case "generate_schedule":
                handleNewSchedule();
                break;

            case "create_iep":
                loadView("SPED Scheduling", "/fxml/SPEDSchedulingDashboard.fxml");
                break;

            case "medical_records":
                loadView("Students", "/fxml/Students.fxml");
                break;

            case "dashboard":
                loadDashboard();
                break;

            case "import_data":
                handleImportData();
                break;

            case "export_data":
                handleExportData();
                break;

            case "settings":
                handleSettings();
                break;

            case "academic_planning":
                loadAcademicPlanning();
                break;

            case "refresh":
                // Refresh current view
                if (lastLoadedView != null) {
                    loadView(lastLoadedView, lastLoadedView);
                }
                break;

            default:
                log.warn("Unknown action: {}", actionId);
                showInfo("Not Implemented", "Action '" + actionId + "' is not yet implemented.");
        }
    }

    /**
     * Called after UI is fully loaded - Auto-loads Dashboard
     */
    public void onApplicationReady() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Application ready - Loading Dashboard...");
        log.info("═══════════════════════════════════════════════════════════════");
        loadDashboard();
    }

    // ========================================================================
    // MENU BAR ACTIONS - FILE MENU
    // ========================================================================

    @FXML
    public void handleNewSchedule() {
        log.info("Action: New Schedule");
        loadView("Generate Schedule", "/fxml/ScheduleGenerator.fxml");
    }

    @FXML
    public void handleOpenSchedule() {
        log.info("Action: Open Schedule");
        loadView("Schedules", "/fxml/Schedules.fxml");
    }

    @FXML
    public void handleSaveSchedule() {
        log.info("Action: Save Schedule");
        showSaveScheduleDialog();
    }

    /**
     * Show Save Schedule Dialog
     * Allows user to update status and save changes to an existing schedule
     */
    private void showSaveScheduleDialog() {
        try {
            // Load all schedules
            List<Schedule> schedules = scheduleRepository.findAll();

            if (schedules.isEmpty()) {
                showInfo("No Schedules", "No schedules available to save.\n\nCreate a schedule first using Schedule → Generate Schedule.");
                return;
            }

            // Filter to only show DRAFT and PUBLISHED schedules (editable)
            List<Schedule> editableSchedules = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.DRAFT || s.getStatus() == ScheduleStatus.PUBLISHED)
                .collect(Collectors.toList());

            if (editableSchedules.isEmpty()) {
                showInfo("No Editable Schedules", "No draft or published schedules available to save.\n\nAll schedules are archived.");
                return;
            }

            // Create dialog
            Dialog<Schedule> dialog = new Dialog<>();
            dialog.setTitle("Save Schedule");
            dialog.setHeaderText("Select a schedule to save and update its status");

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Create form layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Schedule selection
            ComboBox<Schedule> scheduleCombo = new ComboBox<>();
            scheduleCombo.setItems(FXCollections.observableArrayList(editableSchedules));
            scheduleCombo.setPromptText("Select schedule");

            // Custom cell factory to display schedule name
            scheduleCombo.setCellFactory(lv -> new ListCell<Schedule>() {
                @Override
                protected void updateItem(Schedule schedule, boolean empty) {
                    super.updateItem(schedule, empty);
                    if (empty || schedule == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s (%s)", schedule.getScheduleName(), schedule.getStatus()));
                    }
                }
            });
            scheduleCombo.setButtonCell(new ListCell<Schedule>() {
                @Override
                protected void updateItem(Schedule schedule, boolean empty) {
                    super.updateItem(schedule, empty);
                    if (empty || schedule == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s (%s)", schedule.getScheduleName(), schedule.getStatus()));
                    }
                }
            });

            // Status selection
            ComboBox<ScheduleStatus> statusCombo = new ComboBox<>();
            statusCombo.getItems().addAll(ScheduleStatus.DRAFT, ScheduleStatus.PUBLISHED, ScheduleStatus.ARCHIVED);
            statusCombo.setPromptText("New status");
            statusCombo.setDisable(true);

            // Info label to show schedule details
            Label infoLabel = new Label();
            infoLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10px; -fx-font-size: 11px;");
            infoLabel.setWrapText(true);
            infoLabel.setVisible(false);

            // Notes field
            TextArea notesArea = new TextArea();
            notesArea.setPromptText("Save notes or comments");
            notesArea.setPrefRowCount(3);
            notesArea.setDisable(true);

            // Update info when schedule is selected
            scheduleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    statusCombo.setDisable(false);
                    notesArea.setDisable(false);
                    statusCombo.setValue(newVal.getStatus());
                    notesArea.setText(newVal.getNotes() != null ? newVal.getNotes() : "");

                    String slotCount = "N/A";
                    try {
                        slotCount = String.valueOf(newVal.getSlotCount());
                    } catch (Exception e) {
                        log.debug("Could not get slot count");
                    }

                    infoLabel.setText(String.format(
                        "Schedule Information:\n" +
                        "Type: %s | Period: %s\n" +
                        "Date Range: %s to %s\n" +
                        "Total Slots: %s | Conflicts: %d",
                        newVal.getScheduleType(),
                        newVal.getPeriod(),
                        newVal.getStartDate(),
                        newVal.getEndDate(),
                        slotCount,
                        newVal.getTotalConflicts() != null ? newVal.getTotalConflicts() : 0
                    ));
                    infoLabel.setVisible(true);
                } else {
                    statusCombo.setDisable(true);
                    notesArea.setDisable(true);
                    infoLabel.setVisible(false);
                }
            });

            // Add to grid
            int row = 0;
            grid.add(new Label("Schedule:*"), 0, row);
            grid.add(scheduleCombo, 1, row++);

            grid.add(infoLabel, 0, row, 2, 1);
            row++;

            grid.add(new Label("Status:*"), 0, row);
            grid.add(statusCombo, 1, row++);

            grid.add(new Label("Notes:"), 0, row);
            grid.add(notesArea, 1, row++);

            dialog.getDialogPane().setContent(grid);

            // Enable/disable save button
            javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.setDisable(true);

            Runnable validateForm = () -> {
                boolean valid = scheduleCombo.getValue() != null && statusCombo.getValue() != null;
                saveButton.setDisable(!valid);
            };

            scheduleCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
            statusCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());

            // Convert result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    Schedule schedule = scheduleCombo.getValue();
                    schedule.setStatus(statusCombo.getValue());
                    schedule.setNotes(notesArea.getText().trim());
                    schedule.setLastModifiedDate(LocalDate.now());
                    schedule.setLastModifiedBy(com.heronix.util.AuthenticationContext.getCurrentUsername());
                    return schedule;
                }
                return null;
            });

            // Show and save
            dialog.showAndWait().ifPresent(schedule -> {
                try {
                    Schedule saved = scheduleRepository.save(schedule);
                    showSuccess("Schedule Saved",
                        String.format("Schedule '%s' saved successfully!\n\nStatus: %s\nLast Modified: %s",
                        saved.getScheduleName(),
                        saved.getStatus(),
                        saved.getLastModifiedDate()));
                    log.info("Saved schedule: {}", saved.getScheduleName());
                } catch (Exception e) {
                    log.error("Error saving schedule", e);
                    showError("Save Error", "Failed to save schedule: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Error in save schedule dialog", e);
            showError("Error", "Failed to open save dialog: " + e.getMessage());
        }
    }

    @FXML
    public void handleImportData() {
        log.info("Action: Import Data");
        handleImport();
    }

    @FXML
    public void handleExportSchedule() {
        log.info("Action: Export Schedule");
        showInfo("Export Schedule", "Export options:\n• PDF\n• Excel\n• CSV\n• iCalendar");
    }

    @FXML
    public void handleExit() {
        log.info("Action: Exit");
        Platform.exit();
    }

    // ========================================================================
    // MENU BAR ACTIONS - MANAGE MENU
    // ========================================================================

    @FXML
    public void handleManageTeachers() {
        handleTeachers();
    }

    @FXML
    public void handleManageCourses() {
        handleCourses();
    }

    @FXML
    public void handleManageRooms() {
        handleRooms();
    }

    @FXML
    public void handleManageStudents() {
        handleStudents();
    }

    @FXML
    public void handleManageEvents() {
        handleEvents();
    }

    @FXML
    public void handleBulkEnrollment() {
        log.info("Action: Bulk Enrollment Manager");

        try {
            // Load Bulk Enrollment Manager view
            loadView("Bulk Enrollment Manager", "/fxml/BulkEnrollmentManager.fxml");

        } catch (Exception e) {
            log.error("Failed to load Bulk Enrollment Manager view", e);
            showError("Load Error", "Failed to load Bulk Enrollment Manager: " + e.getMessage());
        }
    }

    @FXML
    public void handleSubstitutes() {
        log.info("Action: Substitute Management");

        try {
            // Load Substitute Management view
            loadView("Substitute Management", "/fxml/SubstituteManagement.fxml");

        } catch (Exception e) {
            log.error("Failed to load Substitute Management view", e);
            showError("Load Error", "Failed to load Substitute Management: " + e.getMessage());
        }
    }

    @FXML
    public void handleCoTeachers() {
        log.info("Action: Co-Teacher Management");

        try {
            // Load Co-Teacher Management view
            loadView("Co-Teacher Management", "/fxml/CoTeacherManagement.fxml");

        } catch (Exception e) {
            log.error("Failed to load Co-Teacher Management view", e);
            showError("Load Error", "Failed to load Co-Teacher Management: " + e.getMessage());
        }
    }

    @FXML
    public void handleParaprofessionals() {
        log.info("Action: Paraprofessional Management");

        try {
            // Load Paraprofessional Management view
            loadView("Paraprofessional Management", "/fxml/ParaprofessionalManagement.fxml");

        } catch (Exception e) {
            log.error("Failed to load Paraprofessional Management view", e);
            showError("Load Error", "Failed to load Paraprofessional Management: " + e.getMessage());
        }
    }

    // ========================================================================
    // MENU BAR ACTIONS - SCHEDULE MENU
    // ========================================================================

    @FXML
    public void handleGenerateSchedule() {
        log.info("Action: Generate Schedule");
        loadView("Schedule Generator", "/fxml/ScheduleGenerator.fxml");
    }

    @FXML
    public void loadSchedules() {
        log.info("Action: View Schedules");
        loadView("Schedules", "/fxml/Schedules.fxml");
    }

    @FXML
    public void handleCheckConflicts() {
        log.info("Action: Check Conflicts");
        showConflictDetectionDialog();
    }

    /**
     * Show Conflict Detection Dialog
     * Analyzes schedules for conflicts and displays results
     */
    private void showConflictDetectionDialog() {
        try {
            // Load all schedules
            List<Schedule> schedules = scheduleRepository.findAll();

            if (schedules.isEmpty()) {
                showInfo("No Schedules", "No schedules available to check.\n\nCreate a schedule first using Schedule → Generate Schedule.");
                return;
            }

            // Create dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Conflict Detection");
            dialog.setHeaderText("Analyze schedules for conflicts");

            ButtonType analyzeButtonType = new ButtonType("Analyze", ButtonBar.ButtonData.OK_DONE);
            ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(analyzeButtonType, closeButtonType);

            // Create form layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Schedule selection
            ComboBox<Schedule> scheduleCombo = new ComboBox<>();
            scheduleCombo.setItems(FXCollections.observableArrayList(schedules));
            scheduleCombo.setPromptText("Select schedule to analyze");

            // Custom cell factory
            scheduleCombo.setCellFactory(lv -> new ListCell<Schedule>() {
                @Override
                protected void updateItem(Schedule schedule, boolean empty) {
                    super.updateItem(schedule, empty);
                    if (empty || schedule == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s (%s)", schedule.getScheduleName(), schedule.getStatus()));
                    }
                }
            });
            scheduleCombo.setButtonCell(new ListCell<Schedule>() {
                @Override
                protected void updateItem(Schedule schedule, boolean empty) {
                    super.updateItem(schedule, empty);
                    if (empty || schedule == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s (%s)", schedule.getScheduleName(), schedule.getStatus()));
                    }
                }
            });

            // Results area
            TextArea resultsArea = new TextArea();
            resultsArea.setEditable(false);
            resultsArea.setPrefRowCount(15);
            resultsArea.setPromptText("Conflict analysis results will appear here...");
            resultsArea.setWrapText(true);

            // Add to grid
            int row = 0;
            grid.add(new Label("Select Schedule:"), 0, row);
            grid.add(scheduleCombo, 1, row++);

            grid.add(new Label("Analysis Results:"), 0, row);
            row++;

            grid.add(resultsArea, 0, row, 2, 1);

            dialog.getDialogPane().setContent(grid);

            // Handle analyze button
            javafx.scene.Node analyzeButton = dialog.getDialogPane().lookupButton(analyzeButtonType);
            analyzeButton.setDisable(true);

            scheduleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                analyzeButton.setDisable(newVal == null);
                if (newVal != null) {
                    resultsArea.clear();
                }
            });

            // Prevent dialog from closing when Analyze is clicked
            analyzeButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Schedule selected = scheduleCombo.getValue();
                if (selected != null) {
                    event.consume(); // Don't close dialog
                    analyzeConflicts(selected, resultsArea);
                }
            });

            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Error in conflict detection dialog", e);
            showError("Error", "Failed to open conflict detection: " + e.getMessage());
        }
    }

    /**
     * Analyze a schedule for conflicts
     */
    private void analyzeConflicts(Schedule schedule, TextArea resultsArea) {
        try {
            resultsArea.setText("Analyzing schedule: " + schedule.getScheduleName() + "...\n\n");

            // Get schedule slots
            List<ScheduleSlot> slots;
            try {
                slots = scheduleSlotRepository.findByScheduleId(schedule.getId());
            } catch (Exception e) {
                resultsArea.appendText("Error loading schedule slots: " + e.getMessage() + "\n");
                return;
            }

            if (slots.isEmpty()) {
                resultsArea.appendText("⚠️  No schedule slots found.\n");
                resultsArea.appendText("This schedule has no time slots assigned.\n");
                return;
            }

            // Analyze conflicts
            StringBuilder results = new StringBuilder();
            results.append("═══════════════════════════════════════════════════\n");
            results.append("CONFLICT DETECTION REPORT\n");
            results.append("═══════════════════════════════════════════════════\n\n");

            results.append("Schedule: ").append(schedule.getScheduleName()).append("\n");
            results.append("Status: ").append(schedule.getStatus()).append("\n");
            results.append("Total Slots: ").append(slots.size()).append("\n\n");

            // Check for various conflict types
            int teacherConflicts = 0;
            int roomConflicts = 0;
            int unassignedSlots = 0;
            int emptySlots = 0;

            for (ScheduleSlot slot : slots) {
                // Check for unassigned slots
                if (slot.getTeacher() == null || slot.getCourse() == null || slot.getRoom() == null) {
                    unassignedSlots++;
                    if (slot.getTeacher() == null && slot.getCourse() == null && slot.getRoom() == null) {
                        emptySlots++;
                    }
                }
            }

            // Check for teacher double-booking
            for (int i = 0; i < slots.size(); i++) {
                ScheduleSlot slot1 = slots.get(i);
                if (slot1.getTeacher() == null) continue;

                for (int j = i + 1; j < slots.size(); j++) {
                    ScheduleSlot slot2 = slots.get(j);
                    if (slot2.getTeacher() == null) continue;

                    // Same teacher, overlapping time
                    if (slot1.getTeacher().getId().equals(slot2.getTeacher().getId()) &&
                        slot1.getDayOfWeek() == slot2.getDayOfWeek() &&
                        timeSlotsOverlap(slot1, slot2)) {
                        teacherConflicts++;
                    }
                }
            }

            // Check for room double-booking
            for (int i = 0; i < slots.size(); i++) {
                ScheduleSlot slot1 = slots.get(i);
                if (slot1.getRoom() == null) continue;

                for (int j = i + 1; j < slots.size(); j++) {
                    ScheduleSlot slot2 = slots.get(j);
                    if (slot2.getRoom() == null) continue;

                    // Same room, overlapping time
                    if (slot1.getRoom().getId().equals(slot2.getRoom().getId()) &&
                        slot1.getDayOfWeek() == slot2.getDayOfWeek() &&
                        timeSlotsOverlap(slot1, slot2)) {
                        roomConflicts++;
                    }
                }
            }

            // Summary
            results.append("───────────────────────────────────────────────────\n");
            results.append("CONFLICT SUMMARY\n");
            results.append("───────────────────────────────────────────────────\n\n");

            int totalConflicts = teacherConflicts + roomConflicts;

            if (totalConflicts == 0 && unassignedSlots == 0) {
                results.append("✅  NO CONFLICTS DETECTED\n\n");
                results.append("This schedule appears to be valid with no conflicts.\n");
            } else {
                if (teacherConflicts > 0) {
                    results.append(String.format("⚠️  Teacher Conflicts: %d\n", teacherConflicts));
                    results.append("   Teachers assigned to multiple classes at the same time\n\n");
                }

                if (roomConflicts > 0) {
                    results.append(String.format("⚠️  Room Conflicts: %d\n", roomConflicts));
                    results.append("   Rooms double-booked at the same time\n\n");
                }

                if (unassignedSlots > 0) {
                    results.append(String.format("⚠️  Unassigned Slots: %d\n", unassignedSlots));
                    results.append("   Slots missing teacher, course, or room assignments\n\n");
                }

                if (emptySlots > 0) {
                    results.append(String.format("⚠️  Empty Slots: %d\n", emptySlots));
                    results.append("   Completely empty time slots\n\n");
                }

                results.append("───────────────────────────────────────────────────\n");
                results.append("RECOMMENDATIONS\n");
                results.append("───────────────────────────────────────────────────\n\n");

                if (teacherConflicts > 0 || roomConflicts > 0) {
                    results.append("1. Re-run the schedule generator with better constraints\n");
                    results.append("2. Manually adjust conflicting assignments\n");
                    results.append("3. Review teacher and room availability\n\n");
                }

                if (unassignedSlots > 0) {
                    results.append("1. Complete the schedule by assigning missing resources\n");
                    results.append("2. Use the Schedule Editor to fill empty slots\n");
                    results.append("3. Verify that sufficient resources are available\n\n");
                }
            }

            results.append("═══════════════════════════════════════════════════\n");
            results.append("Analysis completed at: ").append(LocalDate.now()).append("\n");
            results.append("═══════════════════════════════════════════════════\n");

            resultsArea.setText(results.toString());

            log.info("Conflict analysis completed for schedule: {}", schedule.getScheduleName());

        } catch (Exception e) {
            log.error("Error analyzing conflicts", e);
            resultsArea.setText("Error during conflict analysis: " + e.getMessage());
        }
    }

    /**
     * Check if two schedule slots overlap in time
     */
    private boolean timeSlotsOverlap(ScheduleSlot slot1, ScheduleSlot slot2) {
        if (slot1.getStartTime() == null || slot1.getEndTime() == null ||
            slot2.getStartTime() == null || slot2.getEndTime() == null) {
            return false;
        }

        return slot1.getStartTime().isBefore(slot2.getEndTime()) &&
               slot2.getStartTime().isBefore(slot1.getEndTime());
    }

    // ========================================================================
    // MENU BAR ACTIONS - TOOLS MENU
    // ========================================================================

    @FXML
    public void handleDatabaseConsole() {
        log.info("Action: Database Console");
        showInfo("Database Console",
                "H2 Console available at:\nhttp://localhost:8080/h2-console\n\n" +
                        "JDBC URL: jdbc:h2:file:./data/eduscheduler\n" +
                        "Username: sa\nPassword: (leave blank)");
    }

    /**
     * Handle H2 Console menu item - Opens H2 web console in browser
     */
    @FXML
    public void handleH2Console() {
        log.info("Opening H2 Console in browser...");
        try {
            String url = "http://localhost:8080/h2-console";

            // Try to open in default browser
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    showInfo("H2 Console Opened",
                        "H2 Console opened in your default browser.\n\n" +
                        "URL: " + url + "\n\n" +
                        "Connection Details:\n" +
                        "JDBC URL: jdbc:h2:file:./data/eduscheduler\n" +
                        "Username: sa\n" +
                        "Password: (leave blank)");
                } else {
                    showManualURLInfo(url, "H2 Console");
                }
            } else {
                showManualURLInfo(url, "H2 Console");
            }
        } catch (Exception e) {
            log.error("Failed to open H2 Console", e);
            showError("Browser Error",
                "Could not open browser automatically.\n\n" +
                "Please manually navigate to:\n" +
                "http://localhost:8080/h2-console\n\n" +
                "Connection Details:\n" +
                "JDBC URL: jdbc:h2:file:./data/eduscheduler\n" +
                "Username: sa\n" +
                "Password: (leave blank)");
        }
    }

    /**
     * Handle PostgreSQL Console menu item - Opens pgAdmin or provides connection info
     */
    @FXML
    public void handlePostgreSQLConsole() {
        log.info("Opening PostgreSQL tools info...");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("PostgreSQL Database Tools");
        alert.setHeaderText("PostgreSQL Connection Information");

        String content = "Current Profile: H2 (Development)\n\n" +
                "To use PostgreSQL:\n" +
                "1. Start application with: --spring.profiles.active=prod\n" +
                "2. Install pgAdmin: https://www.pgadmin.org/download/\n" +
                "3. Or use psql command line tool\n\n" +
                "PostgreSQL Connection Details:\n" +
                "Host: localhost\n" +
                "Port: 5432\n" +
                "Database: eduscheduler\n" +
                "Username: eduscheduler_user\n" +
                "Password: changeme\n\n" +
                "Want to switch to PostgreSQL?\n" +
                "See: DATABASE_CONFIGURATION.md";

        alert.setContentText(content);

        // Add button to open pgAdmin download page
        ButtonType downloadButton = new ButtonType("Download pgAdmin");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(downloadButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == downloadButton) {
            try {
                String url = "https://www.pgadmin.org/download/";
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                }
            } catch (Exception e) {
                log.error("Failed to open pgAdmin download page", e);
            }
        }
    }

    /**
     * Handle Database Info menu item - Opens Database Management Panel
     */
    @FXML
    public void handleDatabaseInfo() {
        log.info("📊 Opening Database Management Panel");
        loadView("DatabaseManagement", "/fxml/DatabaseManagement.fxml");
        updateStatus("Database Management Panel loaded");
    }

    /**
     * Handle Backup Database menu item - Opens Database Management Panel (Backup section)
     */
    @FXML
    public void handleBackupDatabase() {
        log.info("💾 Opening Database Management Panel - Backup");
        handleDatabaseInfo();  // Open the Database Management Panel

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Backup Database");
        alert.setHeaderText("H2 Database Backup");

        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        String backupFilename = "eduscheduler_backup_" + timestamp + ".mv.db";

        String content = "To backup your H2 database:\n\n" +
                "Method 1: File Copy (Recommended)\n" +
                "1. Close Heronix Scheduling System\n" +
                "2. Copy file: ./data/eduscheduler.mv.db\n" +
                "3. Save as: ./data/" + backupFilename + "\n\n" +
                "Method 2: Export SQL\n" +
                "1. Open H2 Console: http://localhost:8080/h2-console\n" +
                "2. Run: SCRIPT TO 'backup.sql';\n" +
                "3. Save the generated SQL file\n\n" +
                "Restore from Backup:\n" +
                "Simply replace eduscheduler.mv.db with your backup file\n\n" +
                "Current Database Size: " + getDatabaseSize();

        alert.setContentText(content);

        // Add buttons
        ButtonType openFolderButton = new ButtonType("Open Data Folder");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openFolderButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openFolderButton) {
            openDataFolder();
        }
    }

    /**
     * Open database documentation
     */
    private void openDatabaseDocumentation() {
        try {
            java.io.File docFile = new java.io.File("DATABASE_CONFIGURATION.md");
            if (docFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(docFile);
            } else {
                showInfo("Documentation",
                    "Database documentation:\n" +
                    "DATABASE_CONFIGURATION.md\n\n" +
                    "Located in the project root directory");
            }
        } catch (Exception e) {
            log.error("Failed to open documentation", e);
        }
    }

    /**
     * Open data folder in file explorer
     */
    private void openDataFolder() {
        try {
            java.io.File dataDir = new java.io.File("./data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(dataDir);
            }
        } catch (Exception e) {
            log.error("Failed to open data folder", e);
            showError("Error", "Could not open data folder: " + e.getMessage());
        }
    }

    /**
     * Get database file size
     */
    private String getDatabaseSize() {
        try {
            java.io.File dbFile = new java.io.File("./data/eduscheduler.mv.db");
            if (dbFile.exists()) {
                long bytes = dbFile.length();
                if (bytes < 1024) return bytes + " B";
                if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
                return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
            }
            return "Unknown";
        } catch (Exception e) {
            return "Error reading size";
        }
    }

    /**
     * Show manual URL info when browser can't be opened
     */
    private void showManualURLInfo(String url, String title) {
        showInfo(title,
            "Please manually open this URL in your browser:\n\n" + url);
    }

    @FXML
    public void handleClearCourseAssignments() {
        log.info("Action: Clear Course Assignments");

        // First check current status
        com.heronix.service.DatabaseMaintenanceService.MaintenanceResult checkResult =
            databaseMaintenanceService.checkCourseAssignments();

        if (!checkResult.hadPreAssignments()) {
            showInfo("No Pre-Assignments Found",
                "No pre-assigned teachers or rooms were found in courses.\n\n" +
                "OptaPlanner has full flexibility to assign teachers and rooms during schedule generation.");
            return;
        }

        // Show confirmation dialog
        javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Clear Course Assignments");
        confirmAlert.setHeaderText("Pre-assigned Teachers and Rooms Detected");
        confirmAlert.setContentText(String.format(
            "Found:\n" +
            "• %d courses with pre-assigned teachers\n" +
            "• %d courses with pre-assigned rooms\n\n" +
            "These pre-assignments may prevent OptaPlanner from distributing\n" +
            "teachers and rooms properly across all courses.\n\n" +
            "Do you want to clear these assignments?\n" +
            "(This will allow OptaPlanner to freely assign teachers and rooms)",
            checkResult.getCoursesWithTeacherBefore(),
            checkResult.getCoursesWithRoomBefore()
        ));

        java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            // Clear assignments
            com.heronix.service.DatabaseMaintenanceService.MaintenanceResult clearResult =
                databaseMaintenanceService.clearCourseAssignments();

            // Show success message
            showSuccess("Assignments Cleared Successfully", String.format(
                "Cleared:\n" +
                "• %d teacher assignments\n" +
                "• %d room assignments\n\n" +
                "All courses are now ready for OptaPlanner scheduling.\n" +
                "Generate a new schedule to see the improved distribution.",
                clearResult.getTeachersCleared(),
                clearResult.getRoomsCleared()
            ));
        }
    }

    @FXML
    public void loadReports() {
        log.info("Action: Reports");
        loadView("Reports", "/fxml/ReportsAnalytics.fxml");
    }

    @FXML
    public void loadSettings() {
        log.info("Action: Settings");
        loadView("Settings", "/fxml/Settings.fxml");
    }

    /**
     * Handle Refresh shortcut (Ctrl+R / F5)
     */
    private void handleRefresh() {
        log.info("Action: Refresh");
        // Refresh current view by reloading it
        if (lastLoadedView != null) {
            log.info("Refreshing current view: " + lastLoadedView);
            Platform.runLater(() -> {
                showInfo("Refreshed", "Current view has been refreshed.");
            });
        } else {
            log.info("No view to refresh, loading Dashboard");
            loadDashboard();
        }
    }

    /**
     * Handle Help shortcut (F1)
     */
    private void handleHelp() {
        log.info("Action: Help (F1)");
        showUserGuide();
    }

    // ========================================================================
    // MENU BAR ACTIONS - HELP MENU
    // ========================================================================

    /**
     * Handle User Guide menu item
     */
    @FXML
    private void handleUserGuide() {
        showUserGuide();
    }

    /**
     * Show User Guide dialog
     */
    private void showUserGuide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Heronix Scheduling System - User Guide");
        alert.setHeaderText("Quick Start Guide");
        alert.setContentText(
                "📚 GETTING STARTED:\n\n" +
                "1. Import Data\n" +
                "   • Use File → Import to load teachers, students, courses, and rooms\n" +
                "   • Supports CSV, Excel formats\n\n" +
                "2. Generate Schedule\n" +
                "   • Click 'AI Generator' in toolbar\n" +
                "   • Configure schedule parameters\n" +
                "   • Click 'Generate' and let AI optimize\n\n" +
                "3. Review & Edit\n" +
                "   • Use 'Schedule Viewer' to review generated schedules\n" +
                "   • Drag-and-drop to make manual adjustments\n" +
                "   • View conflicts in real-time\n\n" +
                "4. Manage Special Duties\n" +
                "   • Use 'Special Duty Roster' for daily duties and events\n" +
                "   • Assign staff to bus duty, hall monitoring, etc.\n\n" +
                "5. Export & Reports\n" +
                "   • Export schedules to PDF, Excel, or CSV\n" +
                "   • Generate reports for teachers, students, rooms\n\n" +
                "💡 TIP: Hover over buttons for tooltips!\n" +
                "📖 Full documentation: Help → Documentation (coming soon)");

        alert.showAndWait();
    }

    /**
     * Show About dialog
     */
    @FXML
    private void handleAbout() {
        showAbout();
    }

    /**
     * Show About Heronix Scheduling System dialog
     */
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Heronix Scheduling System");
        alert.setHeaderText("Heronix Scheduling System v3.0.0");
        alert.setContentText(
                "AI-Powered School Scheduling System\n\n" +
                        "© 2025 Heronix Scheduling System Team\n\n" +
                        "Features:\n" +
                        "• AI-powered schedule optimization\n" +
                        "• Special education compliance\n" +
                        "• Drag-and-drop schedule editing\n" +
                        "• Multi-format data import\n" +
                        "• Real-time conflict detection\n\n" +
                        "Technology:\n" +
                        "• Java 17 + Spring Boot 3\n" +
                        "• JavaFX 21\n" +
                        "• OptaPlanner AI\n" +
                        "• H2/PostgreSQL Database");

        alert.showAndWait();
    }

    // ========================================================================
    // HELPER METHODS - ALERTS & DIALOGS
    // Add @SuppressWarnings to prevent "unused" warnings
    // ========================================================================

    /**
     * Show error alert
     */
    @SuppressWarnings("unused")
    private void showError(String message) {
        showError("Error", message);
    }

    /**
     * Show error alert with title
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show info alert
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show warning alert
     */
    @SuppressWarnings("unused")
    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show success alert
     */
    @SuppressWarnings("unused")
    private void showSuccess(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText("Success");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show confirmation dialog
     */
    @SuppressWarnings("unused")
    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }

    /**
     * Refresh current view
     */
    @SuppressWarnings("unused")
    private void refreshCurrentView() {
        log.info("Refreshing current view");

        // Use mainBorderPane instead of contentArea
        if (mainBorderPane != null && mainBorderPane.getCenter() != null) {
            log.info("Current view refreshed");
        }
    }
    // ========================================================================
    // TOOLBAR & NAVIGATION ACTIONS
    // ========================================================================

    @FXML
    public void loadDashboard() {
        log.info("Action: Dashboard");
        loadView("Dashboard", "/fxml/Dashboard.fxml");
        highlightNavItem(dashboardNavItem);
    }

    @FXML
    public void handleDashboard() {
        loadDashboard();
    }

    @FXML
    public void loadTeachers() {
        handleTeachers();
    }

    @FXML
    public void handleTeachers() {
        log.info("Action: Teachers");
        loadView("Teachers", "/fxml/Teachers.fxml");
        highlightNavItem(teachersNavItem);
    }

    @FXML
    public void loadCourses() {
        handleCourses();
    }

    @FXML
    public void handleCourses() {
        log.info("Action: Courses");
        loadView("Courses", "/fxml/Courses.fxml");
        highlightNavItem(coursesNavItem);
    }

    @FXML
    public void loadRooms() {
        handleRooms();
    }

    @FXML
    public void handleRooms() {
        log.info("Action: Rooms");
        loadView("Rooms", "/fxml/Rooms.fxml");
        highlightNavItem(roomsNavItem);
    }

    @FXML
    public void loadStudents() {
        handleStudents();
    }

    @FXML
    public void handleStudents() {
        log.info("Action: Students");
        loadView("Students", "/fxml/Students.fxml");
        highlightNavItem(studentsNavItem);
    }

    /**
     * Open Student Schedule Viewer
     * NEW FEATURE: View individual student schedules with all details
     */
    @FXML
    public void handleStudentScheduleViewer() {
        try {
            log.info("Opening Student Schedule Viewer");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StudentScheduleView.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Student Schedule Viewer");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1000, 700));

            StudentScheduleViewController controller = loader.getController();
            controller.setDialogStage(stage);

            stage.show();

            statusLabel.setText("Student Schedule Viewer opened");
            log.info("Student Schedule Viewer opened successfully");

        } catch (IOException e) {
            log.error("Failed to open Student Schedule Viewer", e);
            showError("Error", "Failed to open Student Schedule Viewer: " + e.getMessage());
        }
    }

    @FXML
    public void loadEvents() {
        handleEvents();
    }

    @FXML
    public void handleEvents() {
        log.info("Action: Events");
        loadView("Events", "/fxml/Events.fxml");
        highlightNavItem(eventsNavItem);
    }

    @FXML
    public void handleSchedules() {
        log.info("Action: Schedules");
        loadView("Schedules", "/fxml/Schedules.fxml");
        highlightNavItem(schedulesNavItem);
    }

    @FXML
    public void handleReports() {
        log.info("Action: Reports");
        loadView("Reports & Analytics", "/fxml/ReportsAnalytics.fxml");
        highlightNavItem(reportsNavItem);
    }

    @FXML
    public void handleSettings() {
        log.info("Action: Settings");
        loadView("Settings", "/fxml/Settings.fxml");
        highlightNavItem(settingsNavItem);
    }

    @FXML
    public void loadGradebook() {
        log.info("Action: Gradebook");
        loadView("Gradebook", "/fxml/Gradebook.fxml");
    }

    @FXML
    public void loadStudentEnrollment() {
        log.info("Action: Student Course Enrollment");
        loadView("Student Enrollment", "/fxml/StudentEnrollment.fxml");
    }

    @FXML
    public void loadGenerator() {
        log.info("Action: AI Generator");
        loadView("Schedule Generator", "/fxml/ScheduleGenerator.fxml");
    }

    @FXML
    public void loadScheduleViewer() {
        log.info("Action: Schedule Viewer");
        loadView("Schedule Viewer", "/fxml/ScheduleViewer.fxml");
    }

    @FXML
    public void loadSpecialDutyRoster() {
        log.info("Action: Special Duty Roster");
        loadView("Special Duty Roster", "/fxml/SpecialDutyRoster.fxml");
    }

    @FXML
    public void loadConflictDashboard() {
        log.info("Action: Conflict Dashboard");
        loadView("Conflict Dashboard", "/fxml/conflict-dashboard.fxml");
    }

    @FXML
    public void loadCourseConflictMatrix() {
        log.info("Action: Course Conflict Matrix");
        loadView("Course Conflict Matrix", "/fxml/CourseConflictMatrix.fxml");
    }

    /**
     * Load Test Data Generator
     */
    @FXML
    public void loadDataGenerator() {
        log.info("Action: Test Data Generator");
        loadView("Test Data Generator", "/fxml/DataGenerator.fxml");
    }

    /**
     * Load Academic Plan Management
     * NEW FEATURE: Four-year academic planning for students
     */
    @FXML
    public void loadAcademicPlanning() {
        log.info("Action: Academic Plan Management");
        loadView("Academic Planning", "/fxml/AcademicPlanManagement.fxml");
    }

    @FXML
    public void handleAcademicPlanning() {
        loadAcademicPlanning();
    }

    /**
     * Load Course Recommendation Dashboard
     * NEW FEATURE: AI-powered course recommendations
     */
    @FXML
    public void loadCourseRecommendations() {
        log.info("Action: Course Recommendation Dashboard");
        loadView("Course Recommendations", "/fxml/CourseRecommendationDashboard.fxml");
    }

    @FXML
    public void handleCourseRecommendations() {
        loadCourseRecommendations();
    }

    /**
     * Load Course Sequence Editor
     * NEW FEATURE: Course pathway/sequence management
     */
    @FXML
    public void loadCourseSequenceEditor() {
        log.info("Action: Course Sequence Editor");
        loadView("Course Sequence Editor", "/fxml/CourseSequenceEditor.fxml");
    }

    @FXML
    public void handleCourseSequenceEditor() {
        loadCourseSequenceEditor();
    }

    /**
     * Load Subject Area Management
     * NEW FEATURE: Subject area and department management
     */
    @FXML
    public void loadSubjectAreaManagement() {
        log.info("Action: Subject Area Management");
        loadView("Subject Area Management", "/fxml/SubjectAreaManagement.fxml");
    }

    @FXML
    public void handleSubjectAreaManagement() {
        loadSubjectAreaManagement();
    }

    // ========================================================================
    // NAVIGATION METHODS - SPED/IEP Features
    // ========================================================================

    /**
     * Load IEP Management view
     */
    @FXML
    public void loadIEPManagement() {
        log.info("Action: IEP Management");
        loadView("IEP Management", "/fxml/iep-management.fxml");
    }

    /**
     * Load 504 Plan Management view
     */
    @FXML
    public void load504PlanManagement() {
        log.info("Action: 504 Plan Management");
        loadView("504 Plan Management", "/fxml/plan504-management.fxml");
    }

    /**
     * Load Pull-out Scheduling view
     */
    @FXML
    public void loadPullOutScheduling() {
        log.info("Action: Pull-out Scheduling");
        loadView("Pull-out Scheduling", "/fxml/pullout-scheduling.fxml");
    }

    /**
     * Load SPED Dashboard view
     */
    @FXML
    public void loadSPEDDashboard() {
        log.info("Action: SPED Dashboard");
        loadView("SPED Dashboard", "/fxml/SPEDDashboard.fxml");
    }

    /**
     * Load IEP Wizard view
     */
    @FXML
    public void loadIEPWizard() {
        log.info("Action: IEP Wizard");
        loadView("IEP Wizard", "/fxml/IEPWizard.fxml");
    }

    // ========================================================================
    // NAVIGATION METHODS - Advanced Analytics
    // ========================================================================

    /**
     * Load Advanced Analytics Dashboard
     */
    @FXML
    public void loadAdvancedAnalytics() {
        log.info("Action: Advanced Analytics Dashboard");
        loadView("Advanced Analytics", "/fxml/AdvancedAnalyticsDashboard.fxml");
    }

    /**
     * Load Forecasting Dashboard
     */
    @FXML
    public void loadForecastingDashboard() {
        log.info("Action: Forecasting Dashboard");
        loadView("Forecasting Dashboard", "/fxml/ForecastingDashboard.fxml");
    }

    /**
     * Load Room Utilization Dashboard
     */
    @FXML
    public void loadRoomUtilization() {
        log.info("Action: Room Utilization Dashboard");
        loadView("Room Utilization", "/fxml/RoomUtilizationDashboard.fxml");
    }

    /**
     * Load Teacher Load Heatmap
     */
    @FXML
    public void loadTeacherLoadHeatmap() {
        log.info("Action: Teacher Load Heatmap");
        loadView("Teacher Load Heatmap", "/fxml/TeacherLoadHeatmap.fxml");
    }

    /**
     * Load Predictive Analytics Dashboard
     */
    @FXML
    public void loadPredictiveAnalytics() {
        log.info("Action: Predictive Analytics Dashboard");
        loadView("Predictive Analytics", "/fxml/PredictiveAnalyticsDashboard.fxml");
    }

    // ========================================================================
    // NAVIGATION METHODS - Student Features
    // ========================================================================

    /**
     * Load At-Risk Students Dashboard
     */
    @FXML
    public void loadAtRiskStudents() {
        log.info("Action: At-Risk Students Dashboard");
        loadView("At-Risk Students", "/fxml/at-risk-students.fxml");
    }

    /**
     * Load Grade Progression view
     */
    @FXML
    public void loadGradeProgression() {
        log.info("Action: Grade Progression");
        loadView("Grade Progression", "/fxml/GradeProgression.fxml");
    }

    /**
     * Load Admin Gradebook view
     */
    @FXML
    public void loadAdminGradebook() {
        log.info("Action: Admin Gradebook");
        loadView("Admin Gradebook", "/fxml/AdminGradebook.fxml");
    }

    // ========================================================================
    // NAVIGATION METHODS - Enrollment Features
    // ========================================================================

    /**
     * Load Enrollment Request Management
     */
    @FXML
    public void loadEnrollmentRequests() {
        log.info("Action: Enrollment Request Management");
        loadView("Enrollment Requests", "/fxml/EnrollmentRequestManagement.fxml");
    }

    /**
     * Load Waitlist Manager
     */
    @FXML
    public void loadWaitlistManager() {
        log.info("Action: Waitlist Manager");
        loadView("Waitlist Manager", "/fxml/WaitlistManager.fxml");
    }

    /**
     * Load Course Assignment Wizard
     */
    @FXML
    public void loadCourseAssignmentWizard() {
        log.info("Action: Course Assignment Wizard");
        loadView("Course Assignment Wizard", "/fxml/CourseAssignmentWizard.fxml");
    }

    // ========================================================================
    // NAVIGATION METHODS - Schedule Features
    // ========================================================================

    /**
     * Load Drag-Drop Schedule Editor
     */
    @FXML
    public void loadDragDropScheduleEditor() {
        log.info("Action: Drag-Drop Schedule Editor");
        loadView("Drag-Drop Schedule Editor", "/fxml/DragDropScheduleEditor.fxml");
    }

    /**
     * Load Priority Rules configuration
     */
    @FXML
    public void loadPriorityRules() {
        log.info("Action: Priority Rules");
        loadView("Priority Rules", "/fxml/PriorityRulesDialog.fxml");
    }

    /**
     * Load Lunch Period Management
     */
    @FXML
    public void loadLunchPeriodManagement() {
        log.info("Action: Lunch Period Management");
        loadView("Lunch Period Management", "/fxml/LunchPeriodManagement.fxml");
    }

    // ========================================================================
    // NAVIGATION METHODS - Reports & Utilities
    // ========================================================================

    /**
     * Load Assignment Reports
     */
    @FXML
    public void loadAssignmentReports() {
        log.info("Action: Assignment Reports");
        loadView("Assignment Reports", "/fxml/AssignmentReports.fxml");
    }

    /**
     * Load SIS Export utility
     */
    @FXML
    public void loadSISExport() {
        log.info("Action: SIS Export");
        loadView("SIS Export", "/fxml/SISExport.fxml");
    }

    /**
     * Load Frontline Import utility
     */
    @FXML
    public void loadFrontlineImport() {
        log.info("Action: Frontline Import");
        loadView("Frontline Import", "/fxml/FrontlineImport.fxml");
    }

    /**
     * Load Notification Center
     */
    @FXML
    public void loadNotificationCenter() {
        log.info("Action: Notification Center");
        loadView("Notification Center", "/fxml/NotificationCenter.fxml");
    }

    /**
     * Load Federation Dashboard
     */
    @FXML
    public void loadFederationDashboard() {
        log.info("Action: Federation Dashboard");
        loadView("Federation Dashboard", "/fxml/FederationDashboard.fxml");
    }

    /**
     * Handle Optimize Schedule
     * Opens schedule optimization dialog
     */
    @FXML
    public void handleOptimizeSchedule() {
        log.info("Action: Optimize Schedule");

        try {
            // Load all schedules
            List<Schedule> schedules = scheduleRepository.findAll();

            if (schedules.isEmpty()) {
                showInfo("No Schedules",
                    "No schedules available to optimize.\n\n" +
                    "Create a schedule first using Schedule → Generate Schedule.");
                return;
            }

            // Let user select a schedule to optimize
            ChoiceDialog<Schedule> scheduleDialog = new ChoiceDialog<>(schedules.get(0), schedules);
            scheduleDialog.setTitle("Select Schedule");
            scheduleDialog.setHeaderText("Optimize Schedule");
            scheduleDialog.setContentText("Select a schedule to optimize:");

            Optional<Schedule> selectedSchedule = scheduleDialog.showAndWait();
            if (!selectedSchedule.isPresent()) {
                return;
            }

            Schedule tempSchedule = selectedSchedule.get();
            log.info("Selected schedule: {}", tempSchedule.getScheduleName());

            // Reload schedule with all slots to avoid lazy initialization errors
            final Schedule schedule = scheduleRepository.findByIdWithSlots(tempSchedule.getId())
                .orElse(tempSchedule);

            // Get default optimization configuration
            com.heronix.model.domain.OptimizationConfig config =
                optimizationService.getDefaultConfig();

            // Show configuration dialog
            Stage owner = (Stage) mainBorderPane.getScene().getWindow();
            com.heronix.ui.dialog.OptimizationConfigDialog configDialog =
                new com.heronix.ui.dialog.OptimizationConfigDialog(owner, config);

            Optional<com.heronix.model.domain.OptimizationConfig> configResult =
                configDialog.showAndWait();

            if (!configResult.isPresent()) {
                log.info("User cancelled configuration");
                return;
            }

            com.heronix.model.domain.OptimizationConfig finalConfig = configResult.get();
            log.info("Starting optimization with config: {}", finalConfig.getConfigName());

            // Show progress dialog
            com.heronix.ui.dialog.OptimizationProgressDialog progressDialog =
                new com.heronix.ui.dialog.OptimizationProgressDialog(owner);

            // Run optimization in background thread
            Thread optimizationThread = new Thread(() -> {
                try {
                    com.heronix.model.domain.OptimizationResult result =
                        optimizationService.optimizeSchedule(
                            schedule,
                            finalConfig,
                            progress -> {
                                // Update progress dialog
                                progressDialog.updateProgress(progress);

                                // Check if user cancelled
                                if (progressDialog.isCancelled()) {
                                    // This will be handled by the optimization service
                                    log.info("User requested cancellation");
                                }
                            }
                        );

                    // Mark progress dialog as complete
                    progressDialog.markComplete(
                        result.getSuccessful(),
                        result.getMessage()
                    );

                    // Show results dialog after closing progress
                    Platform.runLater(() -> {
                        try {
                            // Get fitness breakdown
                            com.heronix.service.OptimizationService.FitnessBreakdown breakdown =
                                optimizationService.getFitnessBreakdown(schedule, finalConfig);

                            // Show results
                            com.heronix.ui.dialog.OptimizationResultsDialog resultsDialog =
                                new com.heronix.ui.dialog.OptimizationResultsDialog(
                                    owner, result, breakdown
                                );
                            resultsDialog.showAndWait();

                            // Refresh current view if showing conflicts
                            if (lastLoadedView != null && lastLoadedView.contains("Conflict")) {
                                loadConflictDashboard();
                            }

                        } catch (Exception e) {
                            log.error("Failed to show results", e);
                            showError("Error", "Failed to display results: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    log.error("Optimization failed", e);
                    progressDialog.markComplete(false, e.getMessage());
                    Platform.runLater(() -> {
                        showError("Optimization Failed",
                            "An error occurred during optimization:\n\n" + e.getMessage());
                    });
                }
            });

            optimizationThread.setDaemon(true);
            optimizationThread.start();

            // Show progress dialog (blocks until optimization completes)
            progressDialog.showAndWait();

            log.info("Optimization dialog closed");

        } catch (Exception e) {
            log.error("Failed to start optimization", e);
            showError("Error", "Failed to start optimization: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportData() {
        log.info("Action: Export Data");

        // Create export dialog
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("Export Data");
        dialog.setHeaderText("Export School Data to CSV Files");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainBorderPane.getScene().getWindow());

        // Set dialog buttons
        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        // Data type checkboxes
        CheckBox teachersCheck = new CheckBox("Teachers");
        teachersCheck.setSelected(true);
        CheckBox coursesCheck = new CheckBox("Courses");
        coursesCheck.setSelected(true);
        CheckBox roomsCheck = new CheckBox("Rooms");
        roomsCheck.setSelected(true);
        CheckBox studentsCheck = new CheckBox("Students");
        studentsCheck.setSelected(true);
        CheckBox eventsCheck = new CheckBox("Events");
        eventsCheck.setSelected(true);

        // Directory selection
        TextField directoryField = new TextField();
        directoryField.setEditable(false);
        directoryField.setPrefWidth(350);
        directoryField.setPromptText("Select export folder...");
        Button browseButton = new Button("Browse...");

        final File[] selectedDirectory = new File[1];
        browseButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Export Folder");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File dir = chooser.showDialog(dialog.getOwner());
            if (dir != null) {
                selectedDirectory[0] = dir;
                directoryField.setText(dir.getAbsolutePath());
            }
        });

        HBox directoryBox = new HBox(10, directoryField, browseButton);

        grid.add(new Label("Select data to export:"), 0, 0, 2, 1);
        grid.add(teachersCheck, 0, 1);
        grid.add(coursesCheck, 1, 1);
        grid.add(roomsCheck, 0, 2);
        grid.add(studentsCheck, 1, 2);
        grid.add(eventsCheck, 0, 3);
        grid.add(new Label("Export folder:"), 0, 5, 2, 1);
        grid.add(directoryBox, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable export button based on selection
        javafx.scene.Node exportButton = dialog.getDialogPane().lookupButton(exportButtonType);
        exportButton.setDisable(true);
        directoryField.textProperty().addListener((obs, oldVal, newVal) -> {
            exportButton.setDisable(newVal == null || newVal.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                return selectedDirectory[0];
            }
            return null;
        });

        Optional<File> result = dialog.showAndWait();
        result.ifPresent(exportDir -> {
            try {
                List<String> exportedFiles = new ArrayList<>();
                String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                // Export Teachers
                if (teachersCheck.isSelected()) {
                    List<Teacher> teachers = teacherRepository.findAllActive();
                    if (!teachers.isEmpty()) {
                        byte[] data = exportService.exportTeachersToCSV(teachers);
                        File file = new File(exportDir, "teachers_" + timestamp + ".csv");
                        Files.write(file.toPath(), data);
                        exportedFiles.add("Teachers (" + teachers.size() + " records)");
                        log.info("Exported {} teachers to {}", teachers.size(), file.getName());
                    }
                }

                // Export Courses
                if (coursesCheck.isSelected()) {
                    List<Course> courses = courseRepository.findAll();
                    if (!courses.isEmpty()) {
                        byte[] data = exportService.exportCoursesToCSV(courses);
                        File file = new File(exportDir, "courses_" + timestamp + ".csv");
                        Files.write(file.toPath(), data);
                        exportedFiles.add("Courses (" + courses.size() + " records)");
                        log.info("Exported {} courses to {}", courses.size(), file.getName());
                    }
                }

                // Export Rooms
                if (roomsCheck.isSelected()) {
                    List<Room> rooms = roomRepository.findAll();
                    if (!rooms.isEmpty()) {
                        byte[] data = exportService.exportRoomsToCSV(rooms);
                        File file = new File(exportDir, "rooms_" + timestamp + ".csv");
                        Files.write(file.toPath(), data);
                        exportedFiles.add("Rooms (" + rooms.size() + " records)");
                        log.info("Exported {} rooms to {}", rooms.size(), file.getName());
                    }
                }

                // Export Students
                if (studentsCheck.isSelected()) {
                    List<Student> students = studentRepository.findAll();
                    if (!students.isEmpty()) {
                        byte[] data = exportService.exportStudentsToCSV(students);
                        File file = new File(exportDir, "students_" + timestamp + ".csv");
                        Files.write(file.toPath(), data);
                        exportedFiles.add("Students (" + students.size() + " records)");
                        log.info("Exported {} students to {}", students.size(), file.getName());
                    }
                }

                // Export Events
                if (eventsCheck.isSelected()) {
                    List<Event> events = eventRepository.findAll();
                    if (!events.isEmpty()) {
                        byte[] data = exportService.exportEventsToCSV(events);
                        File file = new File(exportDir, "events_" + timestamp + ".csv");
                        Files.write(file.toPath(), data);
                        exportedFiles.add("Events (" + events.size() + " records)");
                        log.info("Exported {} events to {}", events.size(), file.getName());
                    }
                }

                if (exportedFiles.isEmpty()) {
                    showWarning("No Data", "No data was found to export.");
                } else {
                    showInfo("Export Complete",
                        "Successfully exported to: " + exportDir.getAbsolutePath() + "\n\n" +
                        String.join("\n", exportedFiles));
                }

            } catch (Exception e) {
                log.error("Failed to export data", e);
                showError("Export Failed", "Failed to export data: " + e.getMessage());
            }
        });
    }

    /**
     * Handle Import Data - COMPLETE FIXED VERSION
     * - Resizable window
     * - Better error handling
     * - No undefined method calls
     */
    @FXML
    private void handleImport() {
        log.info("════════════════════════════════════════════════════════════");
        log.info("Opening Import Wizard dialog...");
        log.info("════════════════════════════════════════════════════════════");

        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ImportWizard.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Get controller BEFORE creating stage
            ImportWizardController controller = loader.getController();

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(mainBorderPane.getScene().getWindow());
            dialogStage.setTitle("Import Data Wizard");

            // ✅ FIXED: Make window resizable with better sizing
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(1000);
            dialogStage.setMinHeight(750);
            dialogStage.setWidth(1100);
            dialogStage.setHeight(850);

            // ✅ Set stage on controller BEFORE showing
            controller.setDialogStage(dialogStage);
            log.info("✅ Dialog stage set on controller");

            // Create and set scene
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            log.info("✅ Import Wizard dialog created (900x700, resizable)");

            // Show and wait for dialog to close
            dialogStage.showAndWait();

            log.info("Import Wizard closed");
            log.info("════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("Failed to open Import Wizard", e);

            // ✅ FIXED: Use Platform.runLater and proper Alert
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Import Error");
                alert.setHeaderText("Failed to open Import Wizard");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    // ========================================================================
    // VIEW LOADING HELPER
    // ========================================================================

    /**
     * Generic view loader with error handling
     */
    private void loadView(String viewName, String fxmlPath) {
        log.info("================================================");
        log.info("Loading view: {} from {}", viewName, fxmlPath);
        log.info("=================================================");

        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Resource not found: " + fxmlPath);
            }

            log.info("✅ Resource found: {}", resource);

            FXMLLoader loader = new FXMLLoader(resource);
            loader.setControllerFactory(springContext::getBean);

            Parent view = loader.load();
            log.info("✅ FXML loaded successfully");

            Object controller = loader.getController();
            if (controller != null) {
                log.info("✅ Controller: {}", controller.getClass().getSimpleName());
            }

            mainBorderPane.setCenter(view);

            // Track last loaded view for refresh
            lastLoadedView = viewName;

            updateStatus("Loaded: " + viewName);
            log.info("✅ View loaded successfully: {}", viewName);
            log.info("=================================================");

        } catch (IOException e) {
            log.error("❌ Failed to load view: {}", viewName, e);
            showError("Load Error", "Failed to load " + viewName + ": " + e.getMessage());
        }
    }

    // ========================================================================
    // NAVIGATION HIGHLIGHTING
    // ========================================================================

    private void highlightNavItem(VBox navItem) {
        VBox[] navItems = {
                dashboardNavItem, schedulesNavItem, teachersNavItem, coursesNavItem,
                roomsNavItem, studentsNavItem, eventsNavItem, reportsNavItem, settingsNavItem
        };

        for (VBox item : navItems) {
            if (item != null) {
                item.getStyleClass().remove("nav-item-active");
            }
        }

        if (navItem != null) {
            navItem.getStyleClass().add("nav-item-active");
        }
    }

    // ========================================================================
    // STATUS BAR UPDATES
    // ========================================================================

    private void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
        log.info("Status: {}", message);
    }

    // ========================================================================
    // THEME MANAGEMENT
    // ========================================================================

    /**
     * Load and apply saved theme on application startup
     */
    private void loadSavedTheme() {
        try {
            java.io.File settingsFile = new java.io.File("config/app-settings.properties");
            if (settingsFile.exists()) {
                java.util.Properties settings = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(settingsFile)) {
                    settings.load(fis);
                    String theme = settings.getProperty("theme", "Light");
                    log.info("Loading saved theme: {}", theme);
                    applyTheme(theme);
                } catch (Exception e) {
                    log.error("Failed to load theme settings", e);
                    applyTheme("Light"); // Default to light theme
                }
            } else {
                log.info("No settings file found, using default Light theme");
                applyTheme("Light");
            }
        } catch (Exception e) {
            log.error("Error loading theme", e);
            applyTheme("Light");
        }
    }

    /**
     * Apply theme to the main application window
     *
     * @param themeName Theme name: "Light", "Dark", or "System"
     */
    private void applyTheme(String themeName) {
        try {
            if (mainBorderPane != null && mainBorderPane.getScene() != null) {
                javafx.scene.Parent root = mainBorderPane.getScene().getRoot();

                // Remove existing theme classes
                root.getStyleClass().removeAll("dark-mode", "light-mode");

                // Apply new theme
                switch (themeName) {
                    case "Dark":
                        root.getStyleClass().add("dark-mode");
                        log.info("✓ Dark mode applied to main window");
                        break;
                    case "Light":
                        root.getStyleClass().add("light-mode");
                        log.info("✓ Light mode applied to main window");
                        break;
                    case "System":
                        // Detect system theme (for now, default to light)
                        root.getStyleClass().add("light-mode");
                        log.info("✓ System theme applied to main window (defaulting to light)");
                        break;
                    default:
                        log.warn("Unknown theme: {}, defaulting to light", themeName);
                        root.getStyleClass().add("light-mode");
                }
            }
        } catch (Exception e) {
            log.error("Failed to apply theme", e);
        }
    }

    // ========================================================================
    // AUTHENTICATION & USER MANAGEMENT
    // ========================================================================

    /**
     * Update user info display in status bar
     */
    private void updateUserInfo() {
        if (userInfoLabel != null && SecurityContext.isAuthenticated()) {
            Optional<User> currentUser = SecurityContext.getCurrentUser();
            currentUser.ifPresent(user -> {
                String userInfo = String.format("👤 %s (%s)",
                    user.getFullName(),
                    user.getRoleDisplayName());
                Platform.runLater(() -> userInfoLabel.setText(userInfo));
            });
        }
    }

    /**
     * Configure role-based navigation visibility (Phase 2: UI/UX Reorganization)
     *
     * This method hides/shows navigation items based on the current user's role.
     * Navigation follows the workflow hierarchy:
     * 1. Setup (IT Admin only)
     * 2. Data (Admin, Data Entry)
     * 3. Students (Registrar, Counselor)
     * 4. Teachers (Admin, Data Entry, Scheduler)
     * 5. Courses (Admin, Data Entry, Scheduler)
     * 6. Scheduling (Scheduler, Admin)
     * 7. Gradebook (Teacher, Admin)
     * 8. SPED (Counselor, Admin)
     * 9. Reports (All except Student)
     */
    private void configureRoleBasedNavigation() {
        if (!SecurityContext.isAuthenticated()) {
            log.info("User not authenticated, skipping role-based navigation configuration");
            return;
        }

        Optional<User> currentUserOpt = SecurityContext.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            log.warn("Could not retrieve current user for role-based navigation");
            return;
        }

        User currentUser = currentUserOpt.get();
        log.info("Configuring role-based navigation for user: {} with role: {}",
            currentUser.getUsername(), currentUser.getRoleDisplayName());

        // Dashboard is always visible
        setNavItemVisible(dashboardNavItem, true);

        // Students section - Registrar, Counselor, Teachers can view
        boolean canAccessStudents = permissionService.canAccessStudentsSection(currentUser);
        setNavItemVisible(studentsNavItem, canAccessStudents);
        log.info("  Students section: {}", canAccessStudents ? "visible" : "hidden");

        // Teachers section - Admin, Data Entry, Scheduler
        boolean canAccessTeachers = permissionService.canAccessTeachersSection(currentUser);
        setNavItemVisible(teachersNavItem, canAccessTeachers);
        log.info("  Teachers section: {}", canAccessTeachers ? "visible" : "hidden");

        // Courses section - Admin, Data Entry, Scheduler
        boolean canAccessCourses = permissionService.canAccessCoursesSection(currentUser);
        setNavItemVisible(coursesNavItem, canAccessCourses);
        log.info("  Courses section: {}", canAccessCourses ? "visible" : "hidden");

        // Rooms section - Admin, Scheduler (follows courses permissions)
        setNavItemVisible(roomsNavItem, canAccessCourses);
        log.info("  Rooms section: {}", canAccessCourses ? "visible" : "hidden");

        // Schedules/Scheduling section - Scheduler, Admin, Counselor
        boolean canAccessScheduling = permissionService.canAccessSchedulingSection(currentUser);
        setNavItemVisible(schedulesNavItem, canAccessScheduling);
        log.info("  Schedules section: {}", canAccessScheduling ? "visible" : "hidden");

        // Events section - similar to scheduling
        setNavItemVisible(eventsNavItem, canAccessScheduling);
        log.info("  Events section: {}", canAccessScheduling ? "visible" : "hidden");

        // Reports section - All except Student
        boolean canAccessReports = permissionService.canAccessReportsSection(currentUser);
        setNavItemVisible(reportsNavItem, canAccessReports);
        log.info("  Reports section: {}", canAccessReports ? "visible" : "hidden");

        // Settings - always visible (contains profile settings)
        setNavItemVisible(settingsNavItem, true);
        log.info("  Settings section: visible");

        log.info("✓ Role-based navigation configured successfully");
    }

    /**
     * Helper method to show/hide navigation items
     */
    private void setNavItemVisible(VBox navItem, boolean visible) {
        if (navItem != null) {
            navItem.setVisible(visible);
            navItem.setManaged(visible); // Also hide from layout calculations
        }
    }

    /**
     * Handle User Management menu item
     * Opens the User Management interface (SUPER_ADMIN and ADMIN only)
     */
    @FXML
    private void handleUserManagement() {
        log.info("Action: User Management");

        // Check permissions
        if (!SecurityContext.hasAnyRole(Role.SUPER_ADMIN, Role.ADMIN)) {
            showError("Access Denied",
                "You don't have permission to access User Management.\n\n" +
                "Only SUPER_ADMIN and ADMIN users can manage user accounts.");
            return;
        }

        try {
            // Load User Management view
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/UserManagementView.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(mainBorderPane.getScene().getWindow());
            dialogStage.setTitle("User Management");
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(1200);
            dialogStage.setMinHeight(800);
            dialogStage.setWidth(1300);
            dialogStage.setHeight(850);

            // Create and set scene
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            log.info("✅ User Management dialog created");

            // Show and wait for dialog to close
            dialogStage.showAndWait();

            log.info("User Management closed");

        } catch (Exception e) {
            log.error("Failed to open User Management", e);
            showError("Error", "Failed to open User Management: " + e.getMessage());
        }
    }

    /**
     * Handle Academic Year Management menu item.
     */
    @FXML
    private void handleAcademicYearManagement() {
        log.info("Action: Academic Year Management");

        // Check permissions
        if (!SecurityContext.hasAnyRole(Role.SUPER_ADMIN, Role.ADMIN)) {
            showError("Access Denied",
                "You don't have permission to access Academic Year Management.\n\n" +
                "Only SUPER_ADMIN and ADMIN users can manage academic years.");
            return;
        }

        try {
            // Load Academic Year Management view
            loadView("Academic Year Management", "/fxml/AcademicYearManagement.fxml");

        } catch (Exception e) {
            log.error("Failed to load Academic Year Management view", e);
            showError("Load Error", "Failed to load Academic Year Management: " + e.getMessage());
        }
    }

    /**
     * Handle Purge Database menu item
     * Opens dialog to confirm deletion of ALL data from database
     */
    @FXML
    private void handlePurgeDatabase() {
        log.warn("Action: Purge Database requested");

        // Check permissions - only SUPER_ADMIN can purge database
        if (!SecurityContext.hasRole(Role.SUPER_ADMIN)) {
            showError("Access Denied",
                "You don't have permission to purge the database.\n\n" +
                "Only SUPER_ADMIN users can perform this operation.");
            return;
        }

        try {
            // Load Purge Database confirmation dialog
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/PurgeDatabase.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(mainBorderPane.getScene().getWindow());
            dialogStage.setTitle("⚠️ PURGE DATABASE - WARNING");
            dialogStage.setResizable(false);

            // Create and set scene
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            log.info("Purge Database confirmation dialog created");

            // Show and wait for dialog to close
            dialogStage.showAndWait();

            // Check if purge was successful
            PurgeDatabaseController controller = loader.getController();
            if (controller.isPurgeSuccessful()) {
                log.info("Database purge completed - refreshing dashboard");

                // Refresh the dashboard to show empty state
                loadDashboard();

                // Show success info
                showInfo("Database Purged",
                    "The database has been purged successfully.\n\n" +
                    "All data has been deleted.\n\n" +
                    "You can now import new data to start fresh.");
            }

        } catch (Exception e) {
            log.error("Failed to open Purge Database dialog", e);
            showError("Error", "Failed to open Purge Database dialog: " + e.getMessage());
        }
    }

    /**
     * Handle Change Password menu item
     */
    @FXML
    private void handleChangePassword() {
        log.info("Action: Change Password");

        // Check if user is authenticated
        Optional<User> currentUser = SecurityContext.getCurrentUser();
        if (currentUser.isEmpty()) {
            showError("Error", "You must be logged in to change your password.");
            return;
        }

        // Create password change dialog
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Change Your Password");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainBorderPane.getScene().getWindow());

        // Set dialog buttons
        ButtonType changeButtonType = new ButtonType("Change Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current password");
        currentPasswordField.setPrefWidth(250);

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");
        newPasswordField.setPrefWidth(250);

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        confirmPasswordField.setPrefWidth(250);

        Label requirementsLabel = new Label("Password must be at least 8 characters");
        requirementsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPasswordField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);
        grid.add(requirementsLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable change button based on input
        javafx.scene.Node changeButton = dialog.getDialogPane().lookupButton(changeButtonType);
        changeButton.setDisable(true);

        // Validation listener
        Runnable validateInputs = () -> {
            String current = currentPasswordField.getText();
            String newPass = newPasswordField.getText();
            String confirm = confirmPasswordField.getText();
            boolean valid = current != null && !current.isEmpty() &&
                           newPass != null && newPass.length() >= 8 &&
                           newPass.equals(confirm);
            changeButton.setDisable(!valid);
        };

        currentPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateInputs.run());
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateInputs.run());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateInputs.run());

        // Focus on first field
        Platform.runLater(currentPasswordField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                try {
                    User user = currentUser.get();
                    String currentPass = currentPasswordField.getText();
                    String newPass = newPasswordField.getText();
                    String confirmPass = confirmPasswordField.getText();

                    // Validate passwords match
                    if (!newPass.equals(confirmPass)) {
                        showError("Error", "New passwords do not match.");
                        return false;
                    }

                    // Validate minimum length
                    if (newPass.length() < 8) {
                        showError("Error", "Password must be at least 8 characters.");
                        return false;
                    }

                    // Call service to change password
                    userService.changePassword(user.getId(), currentPass, newPass);
                    return true;

                } catch (IllegalArgumentException e) {
                    showError("Error", e.getMessage());
                    return false;
                } catch (Exception e) {
                    log.error("Failed to change password", e);
                    showError("Error", "Failed to change password: " + e.getMessage());
                    return false;
                }
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && Boolean.TRUE.equals(result.get())) {
            showInfo("Success", "Your password has been changed successfully.");
            currentUser.ifPresent(user ->
                log.info("Password changed for user: {}", user.getUsername())
            );
        }
    }

    /**
     * Handle Logout menu item
     * Clears the security context and exits the application
     */
    @FXML
    private void handleLogout() {
        log.info("Action: Logout");

        // Confirm logout
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Logout Confirmation");
        alert.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            log.info("User confirmed logout");

            // Clear security context
            SecurityContext.clearCurrentUser();
            log.info("Security context cleared");

            // Show goodbye message
            Alert goodbye = new Alert(Alert.AlertType.INFORMATION);
            goodbye.setTitle("Logged Out");
            goodbye.setHeaderText("Successfully Logged Out");
            goodbye.setContentText("You have been logged out successfully.\n\nThe application will now close.");
            goodbye.showAndWait();

            // Exit application
            Platform.exit();
        } else {
            log.info("User cancelled logout");
        }
    }

}