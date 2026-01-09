package com.heronix.ui.controller;

import com.heronix.dto.StudentCourseGradeDTO;
import com.heronix.model.domain.*;
import com.heronix.repository.*;
import com.heronix.service.StudentProgressMonitoringService;
import com.heronix.service.AlertGenerationService;
import com.heronix.service.StudentProgressReportService;
import com.heronix.util.AuthenticationContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Teacher Quick Entry Dashboard Controller
 *
 * Provides teachers with a fast, intuitive interface for entering:
 * - Classroom grades and assignments
 * - Behavior incidents (positive and negative)
 * - Teacher observation notes
 * - Viewing comprehensive student progress
 *
 * This controller integrates with the Phase 1 Multi-Level Monitoring system
 * to enable real-time student progress tracking and intervention alerting.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 2025 - Phase 2A
 */
@Slf4j
@Controller
public class TeacherQuickEntryController {

    // ========================================================================
    // SPRING DEPENDENCIES (Phase 1 Services)
    // ========================================================================

    @Autowired
    private StudentProgressMonitoringService progressMonitoringService;

    @Autowired
    private AlertGenerationService alertGenerationService;

    @Autowired
    private StudentProgressReportService reportService;

    @Autowired
    private ClassroomGradeEntryRepository gradeEntryRepository;

    @Autowired
    private BehaviorIncidentRepository behaviorIncidentRepository;

    @Autowired
    private TeacherObservationNoteRepository observationNoteRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    // ========================================================================
    // FXML UI COMPONENTS - HEADER & NAVIGATION
    // ========================================================================

    @FXML private Label teacherNameLabel;
    @FXML private Label academicYearLabel;
    @FXML private Label studentCountLabel;
    @FXML private ComboBox<Course> courseSelector;

    // ========================================================================
    // FXML UI COMPONENTS - STUDENT SIDEBAR
    // ========================================================================

    @FXML private TextField studentSearchField;
    @FXML private ListView<Student> studentListView;
    @FXML private VBox selectedStudentCard;
    @FXML private Label selectedStudentName;
    @FXML private Label selectedStudentId;
    @FXML private Label selectedStudentGrade;
    @FXML private Label selectedStudentRisk;

    // ========================================================================
    // FXML UI COMPONENTS - TAB PANE
    // ========================================================================

    @FXML private TabPane mainTabPane;

    // ========================================================================
    // FXML UI COMPONENTS - GRADE ENTRY TAB
    // ========================================================================

    @FXML private TextField assignmentNameField;
    @FXML private ComboBox<ClassroomGradeEntry.AssignmentType> assignmentTypeCombo;
    @FXML private TextField pointsPossibleField;
    @FXML private TextField pointsEarnedField;
    @FXML private DatePicker assignmentDatePicker;
    @FXML private Label calculatedPercentageLabel;
    @FXML private Label calculatedLetterGradeLabel;
    @FXML private CheckBox missingWorkCheckbox;
    @FXML private CheckBox benchmarkAssessmentCheckbox;
    @FXML private CheckBox extraCreditCheckbox;
    @FXML private Button saveGradeButton;
    @FXML private TableView<ClassroomGradeEntry> recentGradesTable;
    @FXML private TableColumn<ClassroomGradeEntry, LocalDate> gradeTableDateCol;
    @FXML private TableColumn<ClassroomGradeEntry, String> gradeTableAssignmentCol;
    @FXML private TableColumn<ClassroomGradeEntry, String> gradeTableTypeCol;
    @FXML private TableColumn<ClassroomGradeEntry, String> gradeTablePointsCol;
    @FXML private TableColumn<ClassroomGradeEntry, Double> gradeTablePercentageCol;
    @FXML private TableColumn<ClassroomGradeEntry, String> gradeTableLetterCol;
    @FXML private TableColumn<ClassroomGradeEntry, String> gradeTableFlagsCol;

    // ========================================================================
    // FXML UI COMPONENTS - BEHAVIOR INCIDENT TAB
    // ========================================================================

    @FXML private ComboBox<BehaviorIncident.BehaviorType> behaviorTypeCombo;
    @FXML private ComboBox<BehaviorIncident.SeverityLevel> severityLevelCombo;
    @FXML private TextArea behaviorDescriptionArea;
    @FXML private TextField actionTakenField;
    @FXML private CheckBox adminReferralCheckbox;
    @FXML private Button saveBehaviorButton;
    @FXML private TableView<BehaviorIncident> recentBehaviorTable;
    @FXML private TableColumn<BehaviorIncident, LocalDate> behaviorDateCol;
    @FXML private TableColumn<BehaviorIncident, String> behaviorTypeCol;
    @FXML private TableColumn<BehaviorIncident, String> behaviorCategoryCol;
    @FXML private TableColumn<BehaviorIncident, String> behaviorSeverityCol;
    @FXML private TableColumn<BehaviorIncident, String> behaviorDescriptionCol;
    @FXML private TableColumn<BehaviorIncident, String> behaviorActionCol;

    // ========================================================================
    // FXML UI COMPONENTS - TEACHER OBSERVATIONS TAB
    // ========================================================================

    @FXML private ComboBox<TeacherObservationNote.ObservationCategory> observationCategoryCombo;
    @FXML private ToggleGroup ratingToggleGroup;
    @FXML private TextArea observationNotesArea;
    @FXML private CheckBox flagForInterventionCheckbox;
    @FXML private TextField interventionTypeField;
    @FXML private Button saveObservationButton;
    @FXML private TableView<TeacherObservationNote> recentObservationsTable;
    @FXML private TableColumn<TeacherObservationNote, LocalDate> observationDateCol;
    @FXML private TableColumn<TeacherObservationNote, String> observationCategoryCol;
    @FXML private TableColumn<TeacherObservationNote, String> observationRatingCol;
    @FXML private TableColumn<TeacherObservationNote, String> observationNotesCol;
    @FXML private TableColumn<TeacherObservationNote, String> observationInterventionCol;

    // ========================================================================
    // FXML UI COMPONENTS - STUDENT VIEW TAB
    // ========================================================================

    @FXML private Label studentViewNameLabel;
    @FXML private Label studentViewIdLabel;
    @FXML private Label studentViewGradeLabel;
    @FXML private Label studentViewEmailLabel;
    @FXML private Label studentViewGPALabel;
    @FXML private HBox riskAssessmentAlert;
    @FXML private Label riskAssessmentIcon;
    @FXML private Label riskAssessmentTitle;
    @FXML private Label riskAssessmentMessage;
    @FXML private TableView<StudentCourseGradeDTO> studentCurrentGradesTable;
    @FXML private TableColumn<StudentCourseGradeDTO, String> studentGradeCourseCol;
    @FXML private TableColumn<StudentCourseGradeDTO, Double> studentGradePercentCol;
    @FXML private TableColumn<StudentCourseGradeDTO, String> studentGradeLetterCol;
    @FXML private TableColumn<StudentCourseGradeDTO, Integer> studentGradeMissingCol;
    @FXML private TableColumn<StudentCourseGradeDTO, String> studentGradeStatusCol;
    @FXML private Label attendancePresentLabel;
    @FXML private Label attendanceTardiesLabel;
    @FXML private Label attendanceAbsencesLabel;
    @FXML private Label behaviorPositiveLabel;
    @FXML private Label behaviorNegativeLabel;
    @FXML private Label behaviorMajorLabel;
    @FXML private VBox observationTimelineView;

    // ========================================================================
    // FXML UI COMPONENTS - STATUS BAR
    // ========================================================================

    @FXML private Label statusLabel;
    @FXML private Label lastSavedLabel;
    @FXML private Label connectionStatusLabel;

    // ========================================================================
    // STATE MANAGEMENT
    // ========================================================================

    private Teacher currentTeacher;
    private Course selectedCourse;
    private Student selectedStudent;
    private Campus currentCampus;
    private String currentAcademicYear = "2025-2026";

    private ObservableList<Student> studentsList = FXCollections.observableArrayList();
    private ObservableList<Course> coursesList = FXCollections.observableArrayList();

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing TeacherQuickEntryController");

        try {
            // Initialize teacher from authentication context
            loadCurrentTeacher();

            // Initialize dropdowns
            initializeAssignmentTypeCombo();
            initializeBehaviorTypeCombo();
            initializeSeverityLevelCombo();
            initializeObservationCategoryCombo();

            // Set default date to today
            assignmentDatePicker.setValue(LocalDate.now());

            // Set up listeners
            setupFieldListeners();
            setupValidation();

            // Configure table cell factories
            configureGradeTableColumns();
            configureBehaviorTableColumns();
            configureObservationTableColumns();
            configureStudentGradesTableColumns();

            // Load teacher's courses
            loadTeacherCourses();

            // Update UI
            updateHeaderInfo();
            updateStatusBar("Ready");

            log.info("TeacherQuickEntryController initialized successfully");

        } catch (Exception e) {
            log.error("Error initializing TeacherQuickEntryController", e);
            showError("Initialization Error", "Failed to initialize dashboard: " + e.getMessage());
        }
    }

    // ========================================================================
    // INITIALIZATION HELPERS
    // ========================================================================

    private void loadCurrentTeacher() {
        // Get teacher from authentication context
        String currentUserEmail = AuthenticationContext.getCurrentUserEmail();

        if (currentUserEmail != null) {
            // Lookup teacher by email from authenticated user
            Optional<Teacher> teacherOpt = teacherRepository.findByEmail(currentUserEmail);

            if (teacherOpt.isPresent()) {
                currentTeacher = teacherOpt.get();
                log.info("Loaded authenticated teacher: {} ({})", currentTeacher.getName(), currentUserEmail);
            } else {
                log.warn("No teacher found for authenticated user email: {}", currentUserEmail);
                // Fallback to first teacher as safety measure
                currentTeacher = teacherRepository.findAll().stream().findFirst().orElse(null);
                if (currentTeacher != null) {
                    log.warn("Using fallback teacher: {}", currentTeacher.getName());
                }
            }
        } else {
            // No authenticated user - fallback to first teacher (for development/testing)
            log.warn("No authenticated user found - using fallback teacher");
            currentTeacher = teacherRepository.findAll().stream().findFirst().orElse(null);
            if (currentTeacher != null) {
                log.info("Loaded fallback teacher: {}", currentTeacher.getName());
            } else {
                log.error("No teachers found in database");
            }
        }

        // Load campus
        List<Campus> campuses = campusRepository.findAll();
        if (!campuses.isEmpty()) {
            currentCampus = campuses.get(0);
        }
    }

    private void initializeAssignmentTypeCombo() {
        assignmentTypeCombo.setItems(FXCollections.observableArrayList(
            ClassroomGradeEntry.AssignmentType.values()
        ));
        assignmentTypeCombo.setValue(ClassroomGradeEntry.AssignmentType.HOMEWORK);
    }

    private void initializeBehaviorTypeCombo() {
        behaviorTypeCombo.setItems(FXCollections.observableArrayList(
            BehaviorIncident.BehaviorType.values()
        ));
    }

    private void initializeSeverityLevelCombo() {
        severityLevelCombo.setItems(FXCollections.observableArrayList(
            BehaviorIncident.SeverityLevel.values()
        ));
        severityLevelCombo.setValue(BehaviorIncident.SeverityLevel.MINOR);
    }

    private void initializeObservationCategoryCombo() {
        observationCategoryCombo.setItems(FXCollections.observableArrayList(
            TeacherObservationNote.ObservationCategory.values()
        ));
    }

    private void setupFieldListeners() {
        // Grade calculation listener
        pointsPossibleField.textProperty().addListener((obs, old, newVal) -> updateGradeCalculation());
        pointsEarnedField.textProperty().addListener((obs, old, newVal) -> updateGradeCalculation());

        // Intervention flag listener
        flagForInterventionCheckbox.selectedProperty().addListener((obs, old, newVal) -> {
            interventionTypeField.setDisable(!newVal);
        });
    }

    private void setupValidation() {
        // Enable save buttons only when forms are valid

        // ================================================================
        // GRADE ENTRY VALIDATION
        // ================================================================

        // Numeric validation for points possible
        pointsPossibleField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !newValue.matches("\\d*\\.?\\d*")) {
                pointsPossibleField.setText(oldValue);
            }
            // Visual feedback
            validatePointsFields();
        });

        // Numeric validation for points earned
        pointsEarnedField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !newValue.matches("\\d*\\.?\\d*")) {
                pointsEarnedField.setText(oldValue);
            }
            // Visual feedback
            validatePointsFields();
        });

        // Assignment name required
        assignmentNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.trim().length() > 0) {
                assignmentNameField.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1px;");
            } else {
                assignmentNameField.setStyle("-fx-border-color: #F44336; -fx-border-width: 2px;");
            }
        });

        // ================================================================
        // BEHAVIOR INCIDENT VALIDATION
        // ================================================================

        // Action taken field validation (required if admin referral or parent contacted)
        actionTakenField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.trim().length() > 0) {
                actionTakenField.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1px;");
            } else if (actionTakenField.isVisible()) {
                actionTakenField.setStyle("-fx-border-color: #FF9800; -fx-border-width: 1px;");
            }
        });

        // ================================================================
        // OBSERVATION VALIDATION
        // ================================================================

        // Intervention type required when flagged
        interventionTypeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.trim().length() > 0) {
                interventionTypeField.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1px;");
            } else if (interventionTypeField.isVisible()) {
                interventionTypeField.setStyle("-fx-border-color: #FF9800; -fx-border-width: 1px;");
            }
        });

        log.info("Validation setup complete for all input fields");
    }

    /**
     * Validates points fields and provides visual feedback
     * Returns true if validation passes
     */
    private boolean validatePointsFields() {
        boolean valid = true;

        try {
            String possibleText = pointsPossibleField.getText();
            String earnedText = pointsEarnedField.getText();

            if (possibleText.isEmpty() || earnedText.isEmpty()) {
                // Reset styling if fields are empty
                pointsPossibleField.setStyle("");
                pointsEarnedField.setStyle("");
                return false;
            }

            double pointsPossible = Double.parseDouble(possibleText);
            double pointsEarned = Double.parseDouble(earnedText);

            // Validate points possible > 0
            if (pointsPossible <= 0) {
                pointsPossibleField.setStyle("-fx-border-color: #F44336; -fx-border-width: 2px;");
                valid = false;
            } else {
                pointsPossibleField.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1px;");
            }

            // Validate points earned >= 0
            if (pointsEarned < 0) {
                pointsEarnedField.setStyle("-fx-border-color: #F44336; -fx-border-width: 2px;");
                valid = false;
            } else if (pointsEarned > pointsPossible) {
                // Warning: points earned > points possible (possible but unusual)
                pointsEarnedField.setStyle("-fx-border-color: #FF9800; -fx-border-width: 2px;");
                // Don't mark as invalid, just warn
            } else {
                pointsEarnedField.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1px;");
            }

        } catch (NumberFormatException e) {
            // Invalid number format
            valid = false;
        }

        return valid;
    }

    private void configureGradeTableColumns() {
        // Configure Date column
        gradeTableDateCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAssignmentDate())
        );

        // Configure Assignment column
        gradeTableAssignmentCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAssignmentName())
        );

        // Configure Type column
        gradeTableTypeCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getAssignmentType().toString()
            )
        );

        // Configure Points column (shows "earned/possible")
        gradeTablePointsCol.setCellValueFactory(cellData -> {
            ClassroomGradeEntry entry = cellData.getValue();
            String pointsStr = entry.getPointsEarned() != null
                ? String.format("%.0f/%.0f", entry.getPointsEarned(), entry.getPointsPossible())
                : String.format("—/%.0f", entry.getPointsPossible());
            return new javafx.beans.property.SimpleStringProperty(pointsStr);
        });

        // Configure Percentage column
        gradeTablePercentageCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPercentageGrade())
        );
        gradeTablePercentageCol.setCellFactory(col -> new javafx.scene.control.TableCell<ClassroomGradeEntry, Double>() {
            @Override
            protected void updateItem(Double percentage, boolean empty) {
                super.updateItem(percentage, empty);
                if (empty || percentage == null) {
                    setText("—");
                    setStyle("");
                } else {
                    setText(String.format("%.1f%%", percentage));
                    // Color code by grade
                    if (percentage >= 90) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); // Green for A
                    } else if (percentage >= 80) {
                        setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); // Blue for B
                    } else if (percentage >= 70) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); // Orange for C
                    } else if (percentage >= 60) {
                        setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold;"); // Red-orange for D
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;"); // Red for F
                    }
                }
            }
        });

        // Configure Letter Grade column
        gradeTableLetterCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLetterGrade())
        );
        gradeTableLetterCol.setCellFactory(col -> new javafx.scene.control.TableCell<ClassroomGradeEntry, String>() {
            @Override
            protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) {
                    setText("—");
                    setStyle("");
                } else {
                    setText(grade);
                    // Apply grade badge styling
                    String badgeStyle = "-fx-padding: 4px 8px; -fx-background-radius: 4px; " +
                                       "-fx-font-weight: bold; -fx-alignment: center;";
                    switch (grade) {
                        case "A":
                            setStyle(badgeStyle + "-fx-background-color: rgba(76, 175, 80, 0.3); -fx-text-fill: #4CAF50;");
                            break;
                        case "B":
                            setStyle(badgeStyle + "-fx-background-color: rgba(33, 150, 243, 0.3); -fx-text-fill: #2196F3;");
                            break;
                        case "C":
                            setStyle(badgeStyle + "-fx-background-color: rgba(255, 152, 0, 0.3); -fx-text-fill: #FF9800;");
                            break;
                        case "D":
                            setStyle(badgeStyle + "-fx-background-color: rgba(255, 87, 34, 0.3); -fx-text-fill: #FF5722;");
                            break;
                        case "F":
                            setStyle(badgeStyle + "-fx-background-color: rgba(244, 67, 54, 0.3); -fx-text-fill: #F44336;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // Configure Flags column (shows missing work, benchmark icons)
        gradeTableFlagsCol.setCellValueFactory(cellData -> {
            ClassroomGradeEntry entry = cellData.getValue();
            StringBuilder flags = new StringBuilder();
            if (entry.getIsMissingWork()) flags.append("❌ ");
            if (entry.getIsBenchmarkAssessment()) flags.append("📊 ");
            return new javafx.beans.property.SimpleStringProperty(flags.toString().trim());
        });

        log.info("Grade table columns configured");
    }

    private void configureBehaviorTableColumns() {
        // Configure Date column
        behaviorDateCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getIncidentDate())
        );

        // Configure Type column
        behaviorTypeCol.setCellValueFactory(cellData -> {
            BehaviorIncident incident = cellData.getValue();
            String typeText = incident.getBehaviorType() == BehaviorIncident.BehaviorType.POSITIVE
                ? "✅ Positive"
                : "⚠️ Negative";
            return new javafx.beans.property.SimpleStringProperty(typeText);
        });
        behaviorTypeCol.setCellFactory(col -> new javafx.scene.control.TableCell<BehaviorIncident, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(type);
                    if (type.contains("Positive")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Configure Category column
        behaviorCategoryCol.setCellValueFactory(cellData -> {
            BehaviorIncident incident = cellData.getValue();
            String category = incident.getBehaviorCategory().toString()
                .replace("_", " ")
                .toLowerCase();
            return new javafx.beans.property.SimpleStringProperty(
                category.substring(0, 1).toUpperCase() + category.substring(1)
            );
        });

        // Configure Severity column
        behaviorSeverityCol.setCellValueFactory(cellData -> {
            BehaviorIncident incident = cellData.getValue();
            if (incident.getBehaviorType() == BehaviorIncident.BehaviorType.NEGATIVE
                && incident.getSeverityLevel() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    incident.getSeverityLevel().toString()
                );
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
        behaviorSeverityCol.setCellFactory(col -> new javafx.scene.control.TableCell<BehaviorIncident, String>() {
            @Override
            protected void updateItem(String severity, boolean empty) {
                super.updateItem(severity, empty);
                if (empty || severity == null || severity.equals("—")) {
                    setText("—");
                    setStyle("");
                } else {
                    setText(severity);
                    switch (severity) {
                        case "MINOR":
                            setStyle("-fx-text-fill: #FF9800;");
                            break;
                        case "MODERATE":
                            setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold;");
                            break;
                        case "MAJOR":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });

        // Configure Description column (truncated)
        behaviorDescriptionCol.setCellValueFactory(cellData -> {
            String description = cellData.getValue().getIncidentDescription();
            if (description != null && description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            return new javafx.beans.property.SimpleStringProperty(description);
        });

        // Configure Action column (intervention/action taken)
        behaviorActionCol.setCellValueFactory(cellData -> {
            String action = cellData.getValue().getInterventionApplied();
            if (action != null && action.length() > 30) {
                action = action.substring(0, 27) + "...";
            }
            return new javafx.beans.property.SimpleStringProperty(action != null ? action : "—");
        });

        log.info("Behavior table columns configured");
    }

    private void configureObservationTableColumns() {
        // Configure Date column
        observationDateCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getObservationDate())
        );

        // Configure Category column
        observationCategoryCol.setCellValueFactory(cellData -> {
            TeacherObservationNote note = cellData.getValue();
            String categoryText = formatObservationCategory(note.getObservationCategory());
            return new javafx.beans.property.SimpleStringProperty(categoryText);
        });

        // Configure Rating column with color coding
        observationRatingCol.setCellValueFactory(cellData -> {
            TeacherObservationNote note = cellData.getValue();
            String ratingText = formatObservationRating(note.getObservationRating());
            return new javafx.beans.property.SimpleStringProperty(ratingText);
        });
        observationRatingCol.setCellFactory(col -> new javafx.scene.control.TableCell<TeacherObservationNote, String>() {
            @Override
            protected void updateItem(String rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(rating);
                    if (rating.contains("Excellent")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); // Green
                    } else if (rating.contains("Good")) {
                        setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); // Blue
                    } else if (rating.contains("Needs Improvement")) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); // Orange
                    } else if (rating.contains("Concern")) {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;"); // Red
                    }
                }
            }
        });

        // Configure Notes column (truncated)
        observationNotesCol.setCellValueFactory(cellData -> {
            String notes = cellData.getValue().getObservationNotes();
            if (notes != null && notes.length() > 50) {
                notes = notes.substring(0, 47) + "...";
            }
            return new javafx.beans.property.SimpleStringProperty(notes != null ? notes : "—");
        });

        // Configure Intervention column with icon
        observationInterventionCol.setCellValueFactory(cellData -> {
            TeacherObservationNote note = cellData.getValue();
            if (note.getIsFlagForIntervention() != null && note.getIsFlagForIntervention()) {
                String intervention = note.getInterventionTypeSuggested();
                if (intervention != null && intervention.length() > 20) {
                    intervention = intervention.substring(0, 17) + "...";
                }
                return new javafx.beans.property.SimpleStringProperty("🚩 " + (intervention != null ? intervention : "Yes"));
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });

        log.info("Observation table columns configured");
    }

    private String formatObservationCategory(TeacherObservationNote.ObservationCategory category) {
        if (category == null) return "—";
        switch (category) {
            case COMPREHENSION: return "📖 Comprehension";
            case EFFORT: return "💪 Effort";
            case ENGAGEMENT: return "🎯 Engagement";
            case SOCIAL_EMOTIONAL: return "❤️ Social-Emotional";
            case BEHAVIOR: return "⚖️ Behavior";
            case ATTENDANCE_PATTERNS: return "📅 Attendance";
            case PEER_INTERACTIONS: return "🤝 Peer Interactions";
            case MOTIVATION: return "🔥 Motivation";
            default: return category.toString();
        }
    }

    private String formatObservationRating(TeacherObservationNote.ObservationRating rating) {
        if (rating == null) return "—";
        switch (rating) {
            case EXCELLENT: return "⭐ Excellent";
            case GOOD: return "✓ Good";
            case NEEDS_IMPROVEMENT: return "⚠️ Needs Improvement";
            case CONCERN: return "❌ Concern";
            default: return rating.toString();
        }
    }

    private void loadTeacherCourses() {
        if (currentTeacher == null) {
            log.warn("Cannot load courses: No teacher loaded");
            return;
        }

        // Get courses for current teacher only
        List<Course> courses = courseRepository.findByTeacherId(currentTeacher.getId());
        coursesList.setAll(courses);
        courseSelector.setItems(coursesList);

        log.info("Loaded {} courses for teacher: {}", courses.size(), currentTeacher.getName());
    }

    private void updateHeaderInfo() {
        if (currentTeacher != null) {
            teacherNameLabel.setText("Welcome, " + currentTeacher.getName());
        }
        academicYearLabel.setText(currentAcademicYear);
    }

    // ========================================================================
    // EVENT HANDLERS - HEADER & NAVIGATION
    // ========================================================================

    @FXML
    private void handleSaveAll() {
        log.info("Save All clicked - refreshing all data");
        updateStatusBar("Refreshing all data...");

        // This dashboard uses immediate saves (each tab has individual save buttons)
        // So "Save All" refreshes all displayed data to ensure it's current
        try {
            if (selectedStudent != null) {
                // Refresh all student data (methods use selectedStudent instance variable)
                loadRecentGrades();
                loadRecentBehaviorIncidents();
                loadRecentObservations();

                // Refresh student view tab data if implemented
                refreshSelectedStudent();

                updateStatusBar("All data refreshed successfully");
                lastSavedLabel.setText("Refreshed: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                ));
                log.info("All data refreshed successfully for student: {}", selectedStudent.getFullName());
            } else {
                updateStatusBar("No student selected");
                log.warn("Save All clicked but no student selected");
            }
        } catch (Exception e) {
            log.error("Error refreshing data", e);
            showError("Refresh Error", "Failed to refresh data: " + e.getMessage());
            updateStatusBar("Refresh failed");
        }
    }

    @FXML
    private void handleRefresh() {
        log.info("Refresh clicked");
        loadStudentsForCourse();
        refreshSelectedStudent();
        updateStatusBar("Refreshed");
    }

    @FXML
    private void handleCourseSelected() {
        selectedCourse = courseSelector.getValue();
        if (selectedCourse != null) {
            log.info("Course selected: {}", selectedCourse.getCourseName());
            loadStudentsForCourse();
        }
    }

    // ========================================================================
    // EVENT HANDLERS - STUDENT SIDEBAR
    // ========================================================================

    @FXML
    private void handleSearchStudents() {
        log.info("Search students clicked");
        // TODO: Implement advanced search
    }

    @FXML
    private void handleStudentSearch() {
        String searchText = studentSearchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            studentListView.setItems(studentsList);
        } else {
            ObservableList<Student> filtered = studentsList.stream()
                .filter(s -> s.getFullName().toLowerCase().contains(searchText) ||
                            s.getStudentId().toLowerCase().contains(searchText))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
            studentListView.setItems(filtered);
        }
    }

    @FXML
    private void handleStudentSelected() {
        selectedStudent = studentListView.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            log.info("Student selected: {}", selectedStudent.getFullName());
            updateSelectedStudentCard();
            refreshSelectedStudent();
        }
    }

    // ========================================================================
    // EVENT HANDLERS - GRADE ENTRY
    // ========================================================================

    @FXML
    private void handlePointsChanged() {
        updateGradeCalculation();
        validateGradeForm();
    }

    @FXML
    private void handleSaveGrade() {
        log.info("Save Grade clicked");
        if (selectedStudent == null || selectedCourse == null) {
            showError("No Selection", "Please select a student and course first.");
            return;
        }

        if (currentTeacher == null) {
            showError("No Teacher", "Current teacher not loaded.");
            return;
        }

        try {
            // Validate form fields
            String assignmentName = assignmentNameField.getText().trim();
            if (assignmentName.isEmpty()) {
                showError("Validation Error", "Please enter an assignment name.");
                return;
            }

            ClassroomGradeEntry.AssignmentType assignmentType = assignmentTypeCombo.getValue();
            if (assignmentType == null) {
                showError("Validation Error", "Please select an assignment type.");
                return;
            }

            LocalDate assignmentDate = assignmentDatePicker.getValue();
            if (assignmentDate == null) {
                showError("Validation Error", "Please select an assignment date.");
                return;
            }

            String pointsPossibleStr = pointsPossibleField.getText().trim();
            String pointsEarnedStr = pointsEarnedField.getText().trim();

            if (pointsPossibleStr.isEmpty()) {
                showError("Validation Error", "Please enter points possible.");
                return;
            }

            double pointsPossible;
            Double pointsEarned = null;
            try {
                pointsPossible = Double.parseDouble(pointsPossibleStr);
                if (pointsPossible <= 0) {
                    showError("Validation Error", "Points possible must be greater than 0.");
                    return;
                }

                if (!pointsEarnedStr.isEmpty()) {
                    pointsEarned = Double.parseDouble(pointsEarnedStr);
                    if (pointsEarned < 0) {
                        showError("Validation Error", "Points earned cannot be negative.");
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                showError("Validation Error", "Please enter valid numbers for points.");
                return;
            }

            // Calculate percentage and letter grade if points earned is provided
            Double percentageGrade = null;
            String letterGrade = null;
            if (pointsEarned != null) {
                percentageGrade = (pointsEarned / pointsPossible) * 100.0;
                letterGrade = calculateLetterGrade(percentageGrade);
            }

            // Get campus (use first campus if available, or null)
            Campus campus = campusRepository.findAll().stream().findFirst().orElse(null);

            // Build grade entry
            ClassroomGradeEntry gradeEntry = ClassroomGradeEntry.builder()
                .student(selectedStudent)
                .course(selectedCourse)
                .teacher(currentTeacher)
                .campus(campus)
                .assignmentName(assignmentName)
                .assignmentType(assignmentType)
                .assignmentDate(assignmentDate)
                .pointsPossible(pointsPossible)
                .pointsEarned(pointsEarned)
                .percentageGrade(percentageGrade)
                .letterGrade(letterGrade)
                .isMissingWork(missingWorkCheckbox.isSelected())
                .isBenchmarkAssessment(benchmarkAssessmentCheckbox.isSelected())
                .enteredByStaffId(currentTeacher.getId())
                .entryTimestamp(LocalDateTime.now())
                .build();

            // Save to database
            gradeEntry = gradeEntryRepository.save(gradeEntry);

            log.info("Grade entry saved: {} - {} for student {}",
                assignmentName, pointsEarned != null ? pointsEarned + "/" + pointsPossible : "Missing",
                selectedStudent.getFullName());

            updateStatusBar("Grade saved successfully");
            lastSavedLabel.setText("Last saved: " + LocalDateTime.now().toString().substring(11, 19));
            handleClearGradeForm();
            loadRecentGrades(); // Reload table
            updateRiskAssessment(); // Update risk status

        } catch (Exception e) {
            log.error("Error saving grade", e);
            showError("Save Error", "Failed to save grade: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearGradeForm() {
        assignmentNameField.clear();
        pointsPossibleField.setText("100");
        pointsEarnedField.clear();
        assignmentDatePicker.setValue(LocalDate.now());
        missingWorkCheckbox.setSelected(false);
        benchmarkAssessmentCheckbox.setSelected(false);
        extraCreditCheckbox.setSelected(false);
        calculatedPercentageLabel.setText("—");
        calculatedLetterGradeLabel.setText("");
    }

    @FXML
    private void handleBulkGradeEntry() {
        log.info("Bulk Grade Entry clicked");
        // TODO: Open bulk entry dialog
        showInfo("Coming Soon", "Bulk grade entry will allow you to enter grades for all students at once.");
    }

    // ========================================================================
    // EVENT HANDLERS - BEHAVIOR INCIDENT
    // ========================================================================

    @FXML
    private void handlePresetBehavior(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        String behaviorCategoryStr = (String) source.getUserData();
        log.info("Preset behavior selected: {}", behaviorCategoryStr);

        try {
            // Map PHONE_USE to TECHNOLOGY_MISUSE
            if ("PHONE_USE".equals(behaviorCategoryStr)) {
                behaviorCategoryStr = "TECHNOLOGY_MISUSE";
            }

            BehaviorIncident.BehaviorCategory category = BehaviorIncident.BehaviorCategory.valueOf(behaviorCategoryStr);

            // Determine behavior type from category
            BehaviorIncident.BehaviorType type = category.getBehaviorType().equals("Positive")
                ? BehaviorIncident.BehaviorType.POSITIVE
                : BehaviorIncident.BehaviorType.NEGATIVE;

            // Auto-fill form
            behaviorTypeCombo.setValue(type);

            // Set severity for negative behaviors
            if (type == BehaviorIncident.BehaviorType.NEGATIVE) {
                severityLevelCombo.setValue(BehaviorIncident.SeverityLevel.MINOR);
            }

            // Auto-fill description based on category
            String description = generateBehaviorDescription(category);
            behaviorDescriptionArea.setText(description);

            // Focus on description so teacher can customize
            behaviorDescriptionArea.requestFocus();
            behaviorDescriptionArea.selectAll();

            log.info("Auto-filled behavior form for: {}", category);

        } catch (Exception e) {
            log.error("Error handling preset behavior", e);
            showError("Preset Error", "Failed to auto-fill behavior form: " + e.getMessage());
        }
    }

    private String generateBehaviorDescription(BehaviorIncident.BehaviorCategory category) {
        switch (category) {
            case PARTICIPATION:
                return "Excellent class participation and engagement.";
            case COLLABORATION:
                return "Demonstrated outstanding collaboration with peers.";
            case LEADERSHIP:
                return "Showed leadership qualities in group activities.";
            case IMPROVEMENT:
                return "Significant improvement in academic performance.";
            case HELPING_OTHERS:
                return "Helped classmates understand material.";
            case DISRUPTION:
                return "Disrupted class instruction.";
            case TARDINESS:
                return "Arrived late to class.";
            case NON_COMPLIANCE:
                return "Did not follow classroom instructions.";
            case TECHNOLOGY_MISUSE:
                return "Used phone/technology during class without permission.";
            default:
                return "";
        }
    }

    @FXML
    private void handleSaveBehavior() {
        log.info("Save Behavior clicked");

        // Validation
        if (selectedStudent == null) {
            showError("No Student Selected", "Please select a student first.");
            return;
        }

        if (behaviorTypeCombo.getValue() == null) {
            showError("Missing Behavior Type", "Please select a behavior type (Positive/Negative).");
            return;
        }

        String description = behaviorDescriptionArea.getText();
        if (description == null || description.trim().length() < 3) {
            showError("Missing Description", "Please provide a description (at least 3 characters).");
            return;
        }

        if (description.length() > 500) {
            showError("Description Too Long", "Description must be 500 characters or less.");
            return;
        }

        BehaviorIncident.BehaviorType type = behaviorTypeCombo.getValue();

        // Validate negative behavior requirements
        if (type == BehaviorIncident.BehaviorType.NEGATIVE) {
            if (severityLevelCombo.getValue() == null) {
                showError("Missing Severity", "Please select a severity level for negative behaviors.");
                return;
            }

            if (adminReferralCheckbox.isSelected() && description.length() < 20) {
                showError("Insufficient Description", "Admin referrals require a detailed description (at least 20 characters).");
                return;
            }
        }

        if (currentTeacher == null) {
            showError("No Teacher", "Cannot save: No teacher loaded.");
            return;
        }

        try {
            // Infer behavior category from description or use a default
            BehaviorIncident.BehaviorCategory category = inferBehaviorCategory(description, type);

            // Create behavior incident
            BehaviorIncident incident = BehaviorIncident.builder()
                .student(selectedStudent)
                .course(selectedCourse)  // Can be null for non-classroom incidents
                .reportingTeacher(currentTeacher)
                .incidentDate(LocalDate.now())
                .incidentTime(LocalTime.now())
                .incidentLocation(BehaviorIncident.IncidentLocation.CLASSROOM)
                .behaviorType(type)
                .behaviorCategory(category)
                .severityLevel(type == BehaviorIncident.BehaviorType.NEGATIVE ? severityLevelCombo.getValue() : null)
                .incidentDescription(description.trim())
                .interventionApplied(actionTakenField.getText())
                .adminReferralRequired(adminReferralCheckbox.isSelected())
                .campus(currentCampus)
                .enteredByStaffId(currentTeacher.getId())
                .build();

            // Save to database
            behaviorIncidentRepository.save(incident);

            log.info("Saved behavior incident for student: {}", selectedStudent.getFullName());

            // Update UI
            updateStatusBar("Behavior incident saved successfully");
            handleClearBehaviorForm();
            loadRecentBehaviorIncidents();

        } catch (Exception e) {
            log.error("Error saving behavior incident", e);
            showError("Save Error", "Failed to save behavior incident: " + e.getMessage());
        }
    }

    private BehaviorIncident.BehaviorCategory inferBehaviorCategory(String description, BehaviorIncident.BehaviorType type) {
        String lowerDesc = description.toLowerCase();

        if (type == BehaviorIncident.BehaviorType.POSITIVE) {
            if (lowerDesc.contains("participat")) return BehaviorIncident.BehaviorCategory.PARTICIPATION;
            if (lowerDesc.contains("collaborat") || lowerDesc.contains("teamwork")) return BehaviorIncident.BehaviorCategory.COLLABORATION;
            if (lowerDesc.contains("leader")) return BehaviorIncident.BehaviorCategory.LEADERSHIP;
            if (lowerDesc.contains("improv")) return BehaviorIncident.BehaviorCategory.IMPROVEMENT;
            if (lowerDesc.contains("help")) return BehaviorIncident.BehaviorCategory.HELPING_OTHERS;
            return BehaviorIncident.BehaviorCategory.PARTICIPATION; // Default positive
        } else {
            if (lowerDesc.contains("disrupt") || lowerDesc.contains("talking")) return BehaviorIncident.BehaviorCategory.DISRUPTION;
            if (lowerDesc.contains("late") || lowerDesc.contains("tardy")) return BehaviorIncident.BehaviorCategory.TARDINESS;
            if (lowerDesc.contains("phone") || lowerDesc.contains("technology")) return BehaviorIncident.BehaviorCategory.TECHNOLOGY_MISUSE;
            if (lowerDesc.contains("bully")) return BehaviorIncident.BehaviorCategory.BULLYING;
            if (lowerDesc.contains("fight")) return BehaviorIncident.BehaviorCategory.FIGHTING;
            if (lowerDesc.contains("defian") || lowerDesc.contains("refus")) return BehaviorIncident.BehaviorCategory.DEFIANCE;
            return BehaviorIncident.BehaviorCategory.DISRUPTION; // Default negative
        }
    }

    @FXML
    private void handleClearBehaviorForm() {
        behaviorTypeCombo.setValue(null);
        severityLevelCombo.setValue(BehaviorIncident.SeverityLevel.MINOR);
        behaviorDescriptionArea.clear();
        actionTakenField.clear();
        adminReferralCheckbox.setSelected(false);
    }

    // ========================================================================
    // EVENT HANDLERS - TEACHER OBSERVATIONS
    // ========================================================================

    @FXML
    private void handleInterventionFlagChanged() {
        boolean flagged = flagForInterventionCheckbox.isSelected();
        interventionTypeField.setDisable(!flagged);
        if (!flagged) {
            interventionTypeField.clear();
        }
    }

    @FXML
    private void handleSaveObservation() {
        log.info("Save Observation clicked");

        // Validation - Rule 1: Student must be selected
        if (selectedStudent == null) {
            showError("No Student Selected", "Please select a student first.");
            return;
        }

        // Validation - Rule 2: Category required
        if (observationCategoryCombo.getValue() == null) {
            showError("Missing Category", "Please select an observation category.");
            return;
        }

        // Validation - Rule 3: Rating required
        Toggle selectedRating = ratingToggleGroup.getSelectedToggle();
        if (selectedRating == null) {
            showError("Missing Rating", "Please select an observation rating.");
            return;
        }

        // Validation - Rule 4: Notes required (minimum 10 characters)
        String notes = observationNotesArea.getText();
        if (notes == null || notes.trim().length() < 10) {
            showError("Insufficient Notes", "Please provide observation notes (at least 10 characters).");
            return;
        }

        // Validation - Rule 5: Notes maximum 1000 characters
        if (notes.length() > 1000) {
            showError("Notes Too Long", "Observation notes must be 1000 characters or less.");
            return;
        }

        // Validation - Rule 6: If flagged for intervention, intervention type required
        if (flagForInterventionCheckbox.isSelected()) {
            String interventionType = interventionTypeField.getText();
            if (interventionType == null || interventionType.trim().isEmpty()) {
                showError("Missing Intervention Type", "Please specify the suggested intervention type.");
                return;
            }
        }

        // Validation - Rule 7: Teacher must be loaded
        if (currentTeacher == null) {
            showError("No Teacher", "Cannot save: No teacher loaded.");
            return;
        }

        try {
            // Get selected rating value
            String ratingValue = (String) selectedRating.getUserData();
            TeacherObservationNote.ObservationRating rating =
                TeacherObservationNote.ObservationRating.valueOf(ratingValue);

            // Create observation note
            TeacherObservationNote observation = TeacherObservationNote.builder()
                .student(selectedStudent)
                .course(selectedCourse)  // Can be null if general observation
                .teacher(currentTeacher)
                .observationDate(LocalDate.now())
                .observationCategory(observationCategoryCombo.getValue())
                .observationRating(rating)
                .observationNotes(notes.trim())
                .isFlagForIntervention(flagForInterventionCheckbox.isSelected())
                .interventionTypeSuggested(
                    flagForInterventionCheckbox.isSelected() ?
                    interventionTypeField.getText().trim() : null
                )
                .campus(currentCampus)
                .build();

            // Save to database
            observationNoteRepository.save(observation);

            log.info("Saved observation for student: {} - Category: {} - Rating: {}",
                selectedStudent.getFullName(),
                observation.getObservationCategory(),
                observation.getObservationRating());

            // Update UI
            updateStatusBar("Observation saved successfully");
            handleClearObservationForm();
            loadRecentObservations();

        } catch (Exception e) {
            log.error("Error saving observation", e);
            showError("Save Error", "Failed to save observation: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearObservationForm() {
        observationCategoryCombo.setValue(null);
        ratingToggleGroup.selectToggle(null);
        observationNotesArea.clear();
        flagForInterventionCheckbox.setSelected(false);
        interventionTypeField.clear();
        interventionTypeField.setDisable(true);
    }

    // ========================================================================
    // EVENT HANDLERS - STUDENT VIEW
    // ========================================================================

    @FXML
    private void handleContactParent() {
        log.info("Contact Parent clicked");
        // TODO: Open parent contact dialog
        showInfo("Coming Soon", "Parent contact functionality will be available soon.");
    }

    @FXML
    private void handleGenerateReport() {
        log.info("Generate Report clicked");
        if (selectedStudent == null) {
            showError("No Selection", "Please select a student first.");
            return;
        }

        try {
            // TODO: Use StudentProgressReportService to generate report
            showInfo("Report Generated", "Weekly progress report generated for " + selectedStudent.getFullName());
        } catch (Exception e) {
            log.error("Error generating report", e);
            showError("Report Error", "Failed to generate report: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateAlert() {
        log.info("Create Alert clicked");
        if (selectedStudent == null) {
            showError("No Selection", "Please select a student first.");
            return;
        }

        try {
            // TODO: Use AlertGenerationService to create alert
            showInfo("Alert Created", "Alert created for " + selectedStudent.getFullName());
        } catch (Exception e) {
            log.error("Error creating alert", e);
            showError("Alert Error", "Failed to create alert: " + e.getMessage());
        }
    }

    // ========================================================================
    // HELPER METHODS - DATA LOADING
    // ========================================================================

    private void loadStudentsForCourse() {
        if (selectedCourse == null) {
            studentsList.clear();
            studentListView.setItems(studentsList);
            studentCountLabel.setText("0");
            log.info("No course selected - student list cleared");
            return;
        }

        try {
            // Get all enrollments for the selected course
            List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByCourseId(selectedCourse.getId());

            // Extract unique students from enrollments
            List<Student> students = enrollments.stream()
                .map(StudentEnrollment::getStudent)
                .distinct()
                .sorted((s1, s2) -> {
                    // Sort by last name, then first name
                    int lastNameCompare = s1.getLastName().compareToIgnoreCase(s2.getLastName());
                    if (lastNameCompare != 0) {
                        return lastNameCompare;
                    }
                    return s1.getFirstName().compareToIgnoreCase(s2.getFirstName());
                })
                .collect(Collectors.toList());

            studentsList.setAll(students);
            studentListView.setItems(studentsList);
            studentCountLabel.setText(String.valueOf(students.size()));

            log.info("Loaded {} students for course '{}' (ID: {})",
                students.size(), selectedCourse.getCourseName(), selectedCourse.getId());

            // Clear selection if the previously selected student is not in this course
            if (selectedStudent != null && !students.contains(selectedStudent)) {
                studentListView.getSelectionModel().clearSelection();
                selectedStudent = null;
                log.info("Previous student selection cleared - not enrolled in this course");
            }

        } catch (Exception e) {
            log.error("Error loading students for course {}", selectedCourse.getCourseName(), e);
            showError("Load Error", "Failed to load students for course: " + e.getMessage());
            studentsList.clear();
            studentCountLabel.setText("0");
        }
    }

    private void refreshSelectedStudent() {
        if (selectedStudent == null) {
            return;
        }

        updateSelectedStudentCard();
        loadRecentGrades();
        loadRecentBehaviorIncidents();
        loadRecentObservations();
        updateStudentView();
    }

    private void updateSelectedStudentCard() {
        if (selectedStudent != null) {
            selectedStudentCard.setVisible(true);
            selectedStudentName.setText(selectedStudent.getFullName());
            selectedStudentId.setText("ID: " + selectedStudent.getStudentId());
            selectedStudentGrade.setText("Grade " + selectedStudent.getGradeLevel());

            // TODO: Get risk assessment
            selectedStudentRisk.setText("Risk: Low");
        } else {
            selectedStudentCard.setVisible(false);
        }
    }

    private void loadRecentGrades() {
        if (selectedStudent == null || selectedCourse == null) {
            recentGradesTable.getItems().clear();
            log.info("Cannot load grades - no student or course selected");
            return;
        }

        try {
            // Load grades from past 30 days
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            List<ClassroomGradeEntry> allGrades =
                gradeEntryRepository.findByStudentAndAssignmentDateBetween(
                    selectedStudent, startDate, endDate);

            // Filter grades for the selected course only
            List<ClassroomGradeEntry> courseGrades = allGrades.stream()
                .filter(grade -> grade.getCourse().getId().equals(selectedCourse.getId()))
                .sorted((g1, g2) -> g2.getAssignmentDate().compareTo(g1.getAssignmentDate())) // Newest first
                .collect(Collectors.toList());

            recentGradesTable.getItems().setAll(courseGrades);

            log.info("Loaded {} recent grades for student '{}' in course '{}' (past 30 days)",
                courseGrades.size(), selectedStudent.getFullName(), selectedCourse.getCourseName());

        } catch (Exception e) {
            log.error("Error loading recent grades for student {} in course {}",
                selectedStudent.getFullName(), selectedCourse.getCourseName(), e);
            showError("Load Error", "Failed to load recent grades: " + e.getMessage());
            recentGradesTable.getItems().clear();
        }
    }

    private void loadRecentBehaviorIncidents() {
        if (selectedStudent == null) {
            recentBehaviorTable.getItems().clear();
            log.info("Cannot load behavior incidents - no student selected");
            return;
        }

        try {
            // Load behavior incidents from past 30 days
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            List<BehaviorIncident> incidents =
                behaviorIncidentRepository.findByStudentAndIncidentDateBetween(
                    selectedStudent, startDate, endDate);

            // Sort by date DESC (newest first)
            incidents.sort((i1, i2) -> {
                int dateCompare = i2.getIncidentDate().compareTo(i1.getIncidentDate());
                if (dateCompare != 0) return dateCompare;
                return i2.getIncidentTime().compareTo(i1.getIncidentTime());
            });

            recentBehaviorTable.getItems().setAll(incidents);

            log.info("Loaded {} recent behavior incidents for student '{}' (past 30 days)",
                incidents.size(), selectedStudent.getFullName());

        } catch (Exception e) {
            log.error("Error loading recent behavior incidents for student {}",
                selectedStudent.getFullName(), e);
            showError("Load Error", "Failed to load recent behavior incidents: " + e.getMessage());
            recentBehaviorTable.getItems().clear();
        }
    }

    private void loadRecentObservations() {
        if (selectedStudent == null) {
            recentObservationsTable.getItems().clear();
            log.info("Cannot load observations - no student selected");
            return;
        }

        try {
            // Load observations from past 30 days
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            List<TeacherObservationNote> observations =
                observationNoteRepository.findByStudentAndObservationDateBetween(
                    selectedStudent, startDate, endDate);

            // Sort by date DESC (newest first)
            observations.sort((o1, o2) -> o2.getObservationDate().compareTo(o1.getObservationDate()));

            recentObservationsTable.getItems().setAll(observations);

            log.info("Loaded {} recent observations for student '{}' (past 30 days)",
                observations.size(), selectedStudent.getFullName());

        } catch (Exception e) {
            log.error("Error loading recent observations for student {}",
                selectedStudent.getFullName(), e);
            showError("Load Error", "Failed to load recent observations: " + e.getMessage());
            recentObservationsTable.getItems().clear();
        }
    }

    private void updateStudentView() {
        if (selectedStudent == null) {
            clearStudentView();
            return;
        }

        // Update student info
        studentViewNameLabel.setText(selectedStudent.getFullName());
        studentViewIdLabel.setText("ID: " + selectedStudent.getStudentId());
        studentViewGradeLabel.setText("Grade " + selectedStudent.getGradeLevel());
        studentViewEmailLabel.setText(selectedStudent.getEmail());

        // Load all comprehensive data
        calculateAndDisplayGPA();
        loadCurrentGradesAcrossAllCourses();
        loadAttendanceSummary();
        loadBehaviorSummary();
        loadObservationTimeline();
        updateRiskAssessment();

        log.info("Student view fully updated for: {}", selectedStudent.getFullName());
    }

    private void updateRiskAssessment() {
        if (selectedStudent == null) {
            riskAssessmentAlert.setVisible(false);
            riskAssessmentAlert.setManaged(false);
            return;
        }

        try {
            // Calculate simple risk based on available data
            boolean hasFailingGrades = false;
            boolean hasHighAbsences = false;
            boolean hasMajorBehavior = false;
            boolean hasConcernObservations = false;

            // Check failing grades
            List<ClassroomGradeEntry> recentGrades = gradeEntryRepository.findByStudent(selectedStudent);
            Map<Long, Double> courseAverages = new HashMap<>();

            for (ClassroomGradeEntry grade : recentGrades) {
                if (grade.getCourse() == null || grade.getIsMissingWork()) continue;
                Long courseId = grade.getCourse().getId();
                if (!courseAverages.containsKey(courseId)) {
                    courseAverages.put(courseId, 0.0);
                }
                double currentAvg = courseAverages.get(courseId);
                if (grade.getPercentageGrade() != null) {
                    courseAverages.put(courseId, currentAvg + grade.getPercentageGrade());
                }
            }

            for (Double avg : courseAverages.values()) {
                if (avg < 60) {
                    hasFailingGrades = true;
                    break;
                }
            }

            // Check absences
            LocalDate startDate = LocalDate.now().minusDays(30);
            List<AttendanceRecord> attendance = attendanceRepository.findByStudentIdAndAttendanceDateBetween(
                selectedStudent.getId(), startDate, LocalDate.now());
            long absences = attendance.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT)
                .count();
            hasHighAbsences = absences > 3;

            // Check major behavior incidents
            List<BehaviorIncident> majorIncidents = behaviorIncidentRepository.findCriticalIncidentsSince(
                selectedStudent, startDate);
            hasMajorBehavior = !majorIncidents.isEmpty();

            // Check concern observations
            List<TeacherObservationNote> concernObs = observationNoteRepository.findByStudentAndObservationDateBetween(
                selectedStudent, startDate, LocalDate.now());
            hasConcernObservations = concernObs.stream()
                .anyMatch(o -> o.getObservationRating() == TeacherObservationNote.ObservationRating.CONCERN);

            // Determine risk level
            int riskFactors = 0;
            if (hasFailingGrades) riskFactors++;
            if (hasHighAbsences) riskFactors++;
            if (hasMajorBehavior) riskFactors++;
            if (hasConcernObservations) riskFactors++;

            if (riskFactors == 0) {
                riskAssessmentAlert.setVisible(false);
                riskAssessmentAlert.setManaged(false);
            } else {
                riskAssessmentAlert.setVisible(true);
                riskAssessmentAlert.setManaged(true);

                StringBuilder message = new StringBuilder();
                message.append("Student showing signs of concern: ");
                if (hasFailingGrades) message.append("Failing grades. ");
                if (hasHighAbsences) message.append(absences).append(" absences in 30 days. ");
                if (hasMajorBehavior) message.append("Major behavior incidents. ");
                if (hasConcernObservations) message.append("Concern-level observations. ");

                if (riskFactors >= 3) {
                    riskAssessmentIcon.setText("🚨");
                    riskAssessmentTitle.setText("Critical Risk - Immediate Attention Needed");
                    riskAssessmentAlert.setStyle(riskAssessmentAlert.getStyle() + " -fx-background-color: #FFEBEE; -fx-border-color: #F44336;");
                } else if (riskFactors == 2) {
                    riskAssessmentIcon.setText("⚠️⚠️");
                    riskAssessmentTitle.setText("High Risk - Intervention Recommended");
                    riskAssessmentAlert.setStyle(riskAssessmentAlert.getStyle() + " -fx-background-color: #FFF3E0; -fx-border-color: #FF9800;");
                } else {
                    riskAssessmentIcon.setText("⚠️");
                    riskAssessmentTitle.setText("Medium Risk - Monitor Closely");
                    riskAssessmentAlert.setStyle(riskAssessmentAlert.getStyle() + " -fx-background-color: #FFF9C4; -fx-border-color: #FFC107;");
                }

                riskAssessmentMessage.setText(message.toString());

                log.info("Risk assessment for student '{}': {} risk factors", selectedStudent.getFullName(), riskFactors);
            }

        } catch (Exception e) {
            log.error("Error calculating risk assessment for student {}", selectedStudent.getFullName(), e);
            riskAssessmentAlert.setVisible(false);
            riskAssessmentAlert.setManaged(false);
        }
    }

    private void clearStudentView() {
        studentViewNameLabel.setText("No Student Selected");
        studentViewIdLabel.setText("");
        studentViewGradeLabel.setText("");
        studentViewEmailLabel.setText("");
        studentViewGPALabel.setText("—");
        riskAssessmentAlert.setVisible(false);
        riskAssessmentAlert.setManaged(false);
    }

    // ========================================================================
    // STUDENT VIEW - COMPREHENSIVE DATA LOADING
    // ========================================================================

    private void configureStudentGradesTableColumns() {
        // Course column
        studentGradeCourseCol.setCellValueFactory(cellData -> {
            StudentCourseGradeDTO dto = cellData.getValue();
            String display = dto.getCourseName();
            if (dto.getCourseCode() != null) {
                display += " (" + dto.getCourseCode() + ")";
            }
            return new javafx.beans.property.SimpleStringProperty(display);
        });

        // Percentage column with color coding
        studentGradePercentCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAveragePercentage())
        );
        studentGradePercentCol.setCellFactory(col -> new javafx.scene.control.TableCell<StudentCourseGradeDTO, Double>() {
            @Override
            protected void updateItem(Double percentage, boolean empty) {
                super.updateItem(percentage, empty);
                if (empty || percentage == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(String.format("%.1f%%", percentage));
                    if (percentage >= 90) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); // Green
                    } else if (percentage >= 80) {
                        setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); // Blue
                    } else if (percentage >= 70) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); // Orange
                    } else if (percentage >= 60) {
                        setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold;"); // Orange-Red
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;"); // Red
                    }
                }
            }
        });

        // Letter grade column with badges
        studentGradeLetterCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLetterGrade())
        );
        studentGradeLetterCol.setCellFactory(col -> new javafx.scene.control.TableCell<StudentCourseGradeDTO, String>() {
            @Override
            protected void updateItem(String letter, boolean empty) {
                super.updateItem(letter, empty);
                if (empty || letter == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(letter);
                    switch (letter) {
                        case "A": setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-background-radius: 4px;"); break;
                        case "B": setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-background-radius: 4px;"); break;
                        case "C": setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-background-radius: 4px;"); break;
                        case "D": setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-background-radius: 4px;"); break;
                        case "F": setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-background-radius: 4px;"); break;
                    }
                }
            }
        });

        // Missing assignments column
        studentGradeMissingCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getMissingAssignments())
        );
        studentGradeMissingCol.setCellFactory(col -> new javafx.scene.control.TableCell<StudentCourseGradeDTO, Integer>() {
            @Override
            protected void updateItem(Integer missing, boolean empty) {
                super.updateItem(missing, empty);
                if (empty || missing == null) {
                    setText("");
                    setStyle("");
                } else {
                    if (missing > 0) {
                        setText("❌ " + missing);
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    } else {
                        setText("✓");
                        setStyle("-fx-text-fill: #4CAF50;");
                    }
                }
            }
        });

        // Status column
        studentGradeStatusCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus())
        );

        log.info("Student grades table columns configured");
    }

    private void calculateAndDisplayGPA() {
        if (selectedStudent == null) {
            studentViewGPALabel.setText("—");
            return;
        }

        try {
            // Get all grade entries for the student
            List<ClassroomGradeEntry> allGrades = gradeEntryRepository.findByStudent(selectedStudent);

            if (allGrades.isEmpty()) {
                studentViewGPALabel.setText("—");
                studentViewGPALabel.setStyle("-fx-text-fill: #9E9E9E;");
                return;
            }

            // Group by course and calculate average per course
            Map<Long, List<ClassroomGradeEntry>> gradesByCourse = allGrades.stream()
                .filter(g -> g.getCourse() != null)
                .collect(Collectors.groupingBy(g -> g.getCourse().getId()));

            List<Double> courseAverages = new ArrayList<>();

            for (List<ClassroomGradeEntry> courseGrades : gradesByCourse.values()) {
                // Calculate course average
                double sum = 0;
                int count = 0;
                for (ClassroomGradeEntry grade : courseGrades) {
                    if (!grade.getIsMissingWork() && grade.getPercentageGrade() != null) {
                        sum += grade.getPercentageGrade();
                        count++;
                    }
                }
                if (count > 0) {
                    courseAverages.add(sum / count);
                }
            }

            if (courseAverages.isEmpty()) {
                studentViewGPALabel.setText("—");
                studentViewGPALabel.setStyle("-fx-text-fill: #9E9E9E;");
                return;
            }

            // Calculate GPA (4.0 scale)
            double totalGPA = 0;
            for (Double avg : courseAverages) {
                if (avg >= 90) totalGPA += 4.0;
                else if (avg >= 80) totalGPA += 3.0;
                else if (avg >= 70) totalGPA += 2.0;
                else if (avg >= 60) totalGPA += 1.0;
                // else 0.0
            }

            double gpa = totalGPA / courseAverages.size();
            studentViewGPALabel.setText(String.format("%.2f", gpa));

            // Color code GPA
            if (gpa >= 3.5) {
                studentViewGPALabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 24px;"); // Green
            } else if (gpa >= 3.0) {
                studentViewGPALabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold; -fx-font-size: 24px;"); // Blue
            } else if (gpa >= 2.0) {
                studentViewGPALabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 24px;"); // Orange
            } else {
                studentViewGPALabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold; -fx-font-size: 24px;"); // Red
            }

            log.info("Calculated GPA for student '{}': {}", selectedStudent.getFullName(), gpa);

        } catch (Exception e) {
            log.error("Error calculating GPA for student {}", selectedStudent.getFullName(), e);
            studentViewGPALabel.setText("Error");
            studentViewGPALabel.setStyle("-fx-text-fill: #F44336;");
        }
    }

    private void loadCurrentGradesAcrossAllCourses() {
        if (selectedStudent == null) {
            studentCurrentGradesTable.getItems().clear();
            return;
        }

        try {
            // Get all enrollments for the student
            List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudentId(selectedStudent.getId());

            List<StudentCourseGradeDTO> dtoList = new ArrayList<>();

            for (StudentEnrollment enrollment : enrollments) {
                Course course = enrollment.getCourse();
                if (course == null) continue;

                // Get all grades for this course
                List<ClassroomGradeEntry> courseGrades = gradeEntryRepository.findByStudentAndCourse(
                    selectedStudent, course);

                // Calculate average
                double sum = 0;
                int count = 0;
                int missingCount = 0;

                for (ClassroomGradeEntry grade : courseGrades) {
                    if (grade.getIsMissingWork()) {
                        missingCount++;
                    } else if (grade.getPercentageGrade() != null) {
                        sum += grade.getPercentageGrade();
                        count++;
                    }
                }

                StudentCourseGradeDTO dto = new StudentCourseGradeDTO();
                dto.setCourseName(course.getCourseName());
                dto.setCourseCode(course.getCourseCode());
                dto.setMissingAssignments(missingCount);

                if (count > 0) {
                    double average = sum / count;
                    dto.setAveragePercentage(average);
                    dto.setLetterGrade(StudentCourseGradeDTO.calculateLetterGrade(average));
                    dto.setStatus(StudentCourseGradeDTO.calculateStatus(average));
                } else {
                    dto.setAveragePercentage(null);
                    dto.setLetterGrade("—");
                    dto.setStatus("No Grades");
                }

                dtoList.add(dto);
            }

            // Sort by course name
            dtoList.sort(Comparator.comparing(StudentCourseGradeDTO::getCourseName));

            studentCurrentGradesTable.getItems().setAll(dtoList);

            log.info("Loaded grades across {} courses for student '{}'",
                dtoList.size(), selectedStudent.getFullName());

        } catch (Exception e) {
            log.error("Error loading current grades for student {}", selectedStudent.getFullName(), e);
            studentCurrentGradesTable.getItems().clear();
        }
    }

    private void loadAttendanceSummary() {
        if (selectedStudent == null) {
            attendancePresentLabel.setText("—");
            attendanceTardiesLabel.setText("—");
            attendanceAbsencesLabel.setText("—");
            return;
        }

        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            List<AttendanceRecord> attendanceRecords = attendanceRepository.findByStudentIdAndAttendanceDateBetween(
                selectedStudent.getId(), startDate, endDate);

            long presentCount = attendanceRecords.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
                .count();

            long tardyCount = attendanceRecords.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.TARDY)
                .count();

            long absentCount = attendanceRecords.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT)
                .count();

            attendancePresentLabel.setText(String.valueOf(presentCount));
            attendanceTardiesLabel.setText(String.valueOf(tardyCount));
            attendanceAbsencesLabel.setText(String.valueOf(absentCount));

            log.info("Loaded attendance summary for student '{}': Present={}, Tardy={}, Absent={}",
                selectedStudent.getFullName(), presentCount, tardyCount, absentCount);

        } catch (Exception e) {
            log.error("Error loading attendance summary for student {}", selectedStudent.getFullName(), e);
            attendancePresentLabel.setText("Error");
            attendanceTardiesLabel.setText("Error");
            attendanceAbsencesLabel.setText("Error");
        }
    }

    private void loadBehaviorSummary() {
        if (selectedStudent == null) {
            behaviorPositiveLabel.setText("—");
            behaviorNegativeLabel.setText("—");
            behaviorMajorLabel.setText("—");
            return;
        }

        try {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            Long positiveCount = behaviorIncidentRepository.countByStudentAndBehaviorTypeAndDateBetween(
                selectedStudent, BehaviorIncident.BehaviorType.POSITIVE, startDate, endDate);

            Long negativeCount = behaviorIncidentRepository.countByStudentAndBehaviorTypeAndDateBetween(
                selectedStudent, BehaviorIncident.BehaviorType.NEGATIVE, startDate, endDate);

            List<BehaviorIncident> majorIncidents = behaviorIncidentRepository.findCriticalIncidentsSince(
                selectedStudent, startDate);

            behaviorPositiveLabel.setText(String.valueOf(positiveCount != null ? positiveCount : 0));
            behaviorNegativeLabel.setText(String.valueOf(negativeCount != null ? negativeCount : 0));
            behaviorMajorLabel.setText(String.valueOf(majorIncidents.size()));

            log.info("Loaded behavior summary for student '{}': Positive={}, Negative={}, Major={}",
                selectedStudent.getFullName(), positiveCount, negativeCount, majorIncidents.size());

        } catch (Exception e) {
            log.error("Error loading behavior summary for student {}", selectedStudent.getFullName(), e);
            behaviorPositiveLabel.setText("Error");
            behaviorNegativeLabel.setText("Error");
            behaviorMajorLabel.setText("Error");
        }
    }

    private void loadObservationTimeline() {
        if (selectedStudent == null || observationTimelineView == null) {
            return;
        }

        try {
            // Clear existing observations
            observationTimelineView.getChildren().clear();

            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            List<TeacherObservationNote> observations =
                observationNoteRepository.findByStudentAndObservationDateBetween(
                    selectedStudent, startDate, endDate);

            // Sort by date DESC and limit to 5
            observations.sort((o1, o2) -> o2.getObservationDate().compareTo(o1.getObservationDate()));
            List<TeacherObservationNote> recentObservations = observations.stream()
                .limit(5)
                .collect(Collectors.toList());

            if (recentObservations.isEmpty()) {
                Label emptyLabel = new Label("No observations recorded in the past 30 days");
                emptyLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-style: italic;");
                observationTimelineView.getChildren().add(emptyLabel);
                return;
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

            for (TeacherObservationNote obs : recentObservations) {
                VBox card = new VBox(6);
                card.setPadding(new Insets(12));
                card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                             "-fx-border-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

                // Set border color based on rating
                String borderColor = "#CCCCCC";
                if (obs.getObservationRating() != null) {
                    switch (obs.getObservationRating()) {
                        case EXCELLENT: borderColor = "#4CAF50"; break;
                        case GOOD: borderColor = "#2196F3"; break;
                        case NEEDS_IMPROVEMENT: borderColor = "#FF9800"; break;
                        case CONCERN: borderColor = "#F44336"; break;
                    }
                }
                card.setStyle(card.getStyle() + " -fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4px;");

                // Header: Category and Rating
                HBox header = new HBox(8);
                Label categoryLabel = new Label(formatObservationCategory(obs.getObservationCategory()));
                categoryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                Label ratingLabel = new Label(formatObservationRating(obs.getObservationRating()));
                ratingLabel.setStyle("-fx-font-size: 12px;");
                header.getChildren().addAll(categoryLabel, new Label("-"), ratingLabel);

                // Date
                Label dateLabel = new Label(obs.getObservationDate().format(dateFormatter));
                dateLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

                // Notes (truncated)
                String notes = obs.getObservationNotes();
                if (notes != null && notes.length() > 80) {
                    notes = notes.substring(0, 77) + "...";
                }
                Label notesLabel = new Label(notes);
                notesLabel.setWrapText(true);
                notesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");

                // Intervention flag
                if (obs.getIsFlagForIntervention() != null && obs.getIsFlagForIntervention()) {
                    Label interventionLabel = new Label("🚩 Intervention: " +
                        (obs.getInterventionTypeSuggested() != null ? obs.getInterventionTypeSuggested() : "Yes"));
                    interventionLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold; -fx-font-size: 11px;");
                    card.getChildren().addAll(header, dateLabel, notesLabel, interventionLabel);
                } else {
                    card.getChildren().addAll(header, dateLabel, notesLabel);
                }

                observationTimelineView.getChildren().add(card);
            }

            log.info("Loaded {} observation cards for student '{}'",
                recentObservations.size(), selectedStudent.getFullName());

        } catch (Exception e) {
            log.error("Error loading observation timeline for student {}", selectedStudent.getFullName(), e);
            observationTimelineView.getChildren().clear();
            Label errorLabel = new Label("Error loading observations");
            errorLabel.setStyle("-fx-text-fill: #F44336;");
            observationTimelineView.getChildren().add(errorLabel);
        }
    }

    // ========================================================================
    // HELPER METHODS - VALIDATION & CALCULATION
    // ========================================================================

    private void updateGradeCalculation() {
        try {
            String possibleStr = pointsPossibleField.getText();
            String earnedStr = pointsEarnedField.getText();

            if (possibleStr.isEmpty() || earnedStr.isEmpty()) {
                calculatedPercentageLabel.setText("—");
                calculatedLetterGradeLabel.setText("");
                return;
            }

            double possible = Double.parseDouble(possibleStr);
            double earned = Double.parseDouble(earnedStr);

            if (possible <= 0) {
                calculatedPercentageLabel.setText("Invalid");
                calculatedLetterGradeLabel.setText("");
                return;
            }

            double percentage = (earned / possible) * 100.0;
            String letterGrade = calculateLetterGrade(percentage);

            calculatedPercentageLabel.setText(String.format("%.1f%%", percentage));
            calculatedLetterGradeLabel.setText(letterGrade);

        } catch (NumberFormatException e) {
            calculatedPercentageLabel.setText("Invalid");
            calculatedLetterGradeLabel.setText("");
        }
    }

    private String calculateLetterGrade(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    private void validateGradeForm() {
        boolean valid = !assignmentNameField.getText().trim().isEmpty()
                     && assignmentTypeCombo.getValue() != null
                     && !pointsPossibleField.getText().isEmpty()
                     && !pointsEarnedField.getText().isEmpty()
                     && assignmentDatePicker.getValue() != null;

        saveGradeButton.setDisable(!valid);
    }

    // ========================================================================
    // HELPER METHODS - UI UPDATES
    // ========================================================================

    private void updateStatusBar(String message) {
        statusLabel.setText(message);
    }

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

    // ========================================================================
    // ADVANCED FEATURES - REPORTS AND ALERTS
    // ========================================================================

    /**
     * Generate comprehensive progress report for selected student
     * Uses StudentProgressReportService to create detailed weekly report
     */
    @FXML
    private void generateStudentProgressReport() {
        if (selectedStudent == null) {
            showError("No Student Selected", "Please select a student to generate a progress report.");
            return;
        }

        try {
            log.info("Generating progress report for student: {}", selectedStudent.getFullName());
            updateStatusBar("Generating progress report...");

            // Generate report using Phase 1 service
            var report = reportService.generateStudentWeeklyReport(selectedStudent);

            if (report != null) {
                // Extract data from nested objects
                String riskLevel = report.getRiskAssessment() != null ?
                    report.getRiskAssessment().getRiskLevel().toString() : "Unknown";

                double averageGrade = 0.0;
                if (report.getAcademicSummary() != null) {
                    averageGrade = report.getAcademicSummary().getAverageGrade();
                }

                // Show success message with report summary
                String summary = String.format(
                    "Progress Report Generated for %s\n\n" +
                    "Report Period: %s to %s\n" +
                    "Risk Level: %s\n" +
                    "Average Grade: %.1f%%\n\n" +
                    "Report saved to database.",
                    selectedStudent.getFullName(),
                    report.getStartDate(),
                    report.getEndDate(),
                    riskLevel,
                    averageGrade
                );
                showInfo("Report Generated", summary);
                updateStatusBar("Progress report generated successfully");
                log.info("Progress report generated successfully for student ID: {}", selectedStudent.getId());
            } else {
                showError("Report Generation Failed", "Unable to generate progress report. Please check student data.");
                updateStatusBar("Report generation failed");
            }

        } catch (Exception e) {
            log.error("Error generating progress report for student: {}", selectedStudent.getId(), e);
            showError("Report Generation Error", "An error occurred while generating the report: " + e.getMessage());
            updateStatusBar("Report generation failed");
        }
    }

    /**
     * Generate immediate alert for selected student
     * Uses AlertGenerationService to create alert and notify relevant staff
     */
    @FXML
    private void generateStudentAlert() {
        if (selectedStudent == null) {
            showError("No Student Selected", "Please select a student to generate an alert.");
            return;
        }

        try {
            log.info("Generating alert for student: {}", selectedStudent.getFullName());
            updateStatusBar("Generating alert...");

            // Generate alert using Phase 1 service
            alertGenerationService.generateAlert(selectedStudent);

            // Show success message
            String message = String.format(
                "Alert generated for %s\n\n" +
                "Relevant staff members have been notified.\n" +
                "Alert saved to database and visible in monitoring dashboard.",
                selectedStudent.getFullName()
            );
            showInfo("Alert Generated", message);
            updateStatusBar("Alert generated successfully");
            log.info("Alert generated successfully for student ID: {}", selectedStudent.getId());

        } catch (Exception e) {
            log.error("Error generating alert for student: {}", selectedStudent.getId(), e);
            showError("Alert Generation Error", "An error occurred while generating the alert: " + e.getMessage());
            updateStatusBar("Alert generation failed");
        }
    }
}
