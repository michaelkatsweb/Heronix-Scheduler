package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.CourseAssignmentStatus;
import com.heronix.model.enums.EducationLevel;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Courses Controller - FIXED
 * Location: src/main/java/com/eduscheduler/ui/controller/CoursesController.java
 */
@Slf4j
@Controller
public class CoursesController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private com.heronix.repository.RoomRepository roomRepository;

    @Autowired
    private com.heronix.service.CourseService courseService;

    @Autowired
    private com.heronix.service.ExportService exportService;

    @Autowired
    private com.heronix.service.impl.SmartCourseAssignmentService smartAssignmentService;

    @Autowired
    private com.heronix.service.impl.ComplianceValidationService complianceValidationService;

    @Autowired
    private com.heronix.controller.MultiRoomAssignmentDialogController multiRoomDialogController;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> levelFilter;
    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> statusColumn;
    @FXML private TableColumn<Course, String> codeColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> subjectColumn;
    @FXML private TableColumn<Course, String> levelColumn;
    @FXML private TableColumn<Course, String> creditsColumn;
    @FXML private TableColumn<Course, String> durationColumn;
    @FXML private TableColumn<Course, String> maxStudentsColumn;
    @FXML private TableColumn<Course, String> complexityColumn;
    @FXML private TableColumn<Course, String> qualificationColumn; // ENHANCED: Multi-Subject Teaching MVP
    @FXML private TableColumn<Course, Void> quickAssignColumn; // ENHANCED: Bug Fix #7 - Quick Teacher Assignment
    @FXML private TableColumn<Course, Void> actionsColumn;
    @FXML private Label recordCountLabel;

    // Status Filter Buttons
    @FXML private Button showAllButton;
    @FXML private Button showUnassignedButton;
    @FXML private Button showPartialButton;
    @FXML private Button showCompleteButton;
    @FXML private Label statusCountLabel;

    private ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private ObservableList<Course> allCourses = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Course> filteredCourses;

    @FXML
    public void initialize() {
        log.info("CoursesController initialized");
        setupTableColumns();
        setupStatusColumn();
        setupQualificationColumn(); // ENHANCED: Multi-Subject Teaching MVP
        setupElectiveRowStyling(); // ENHANCED: Phase 20 - Kanban-style elective styling
        setupFilters();
        setupQuickAssignColumn(); // ENHANCED: Bug Fix #7 - Quick Teacher Assignment
        setupActionsColumn();
        setupStatusFilters();

        // Delay loading courses until after JavaFX initialization completes
        Platform.runLater(() -> {
            try {
                loadCourses();
            } catch (Exception e) {
                log.error("Error during initial course load", e);
            }
        });
    }

    private void setupTableColumns() {
        codeColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getCourseCode()));
        
        nameColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getCourseName()));
        
        subjectColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getSubject()));
        
        levelColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getLevel() != null ? 
                data.getValue().getLevel().toString() : "N/A"));
        
        creditsColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getSessionsPerWeek() != null ?
                data.getValue().getSessionsPerWeek() + "/wk" : "N/A"));
        
        durationColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDurationMinutes() + " min"));
        
        maxStudentsColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getMaxStudents())));
        
        complexityColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getComplexityScore())));
    }

    private void setupStatusColumn() {
        // Status indicator column
        statusColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStatusIndicator()));

        // Add custom cell factory for colored background and tooltips
        statusColumn.setCellFactory(column -> new TableCell<Course, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(item);

                    // Set style with proper alignment and background
                    Course course = getTableRow().getItem();
                    if (course != null) {
                        String style = "-fx-alignment: CENTER; -fx-font-size: 20px; -fx-padding: 5px;";
                        String bgColor = switch (item) {
                            case "🟢" -> "-fx-background-color: rgba(76, 175, 80, 0.3);";   // Green tint
                            case "🟡" -> "-fx-background-color: rgba(255, 193, 7, 0.3);";   // Yellow tint
                            case "🔴" -> "-fx-background-color: rgba(244, 67, 54, 0.3);";   // Red tint
                            default -> "";
                        };
                        setStyle(style + bgColor);

                        // Add tooltip with detailed status information
                        String tooltipText = switch (item) {
                            case "🟢" -> String.format("Fully Assigned\n✓ Teacher: %s\n✓ Room: %s",
                                course.getTeacher() != null ? course.getTeacher().getName() : "Unknown",
                                course.getRoom() != null ? course.getRoom().getRoomNumber() : "Unknown");
                            case "🟡" -> {
                                if (course.getTeacher() != null && course.getRoom() == null) {
                                    yield String.format("Partially Assigned\n✓ Teacher: %s\n✗ Room: Not assigned",
                                        course.getTeacher().getName());
                                } else if (course.getTeacher() == null && course.getRoom() != null) {
                                    yield String.format("Partially Assigned\n✗ Teacher: Not assigned\n✓ Room: %s",
                                        course.getRoom().getRoomNumber());
                                } else {
                                    yield "Partially Assigned\nNeeds teacher or room";
                                }
                            }
                            case "🔴" -> String.format("Unassigned\n✗ Teacher: Not assigned\n✗ Room: Not assigned\n\nCourse: %s",
                                course.getCourseName());
                            default -> "Unknown Status";
                        };

                        Tooltip tooltip = new Tooltip(tooltipText);
                        tooltip.setShowDelay(javafx.util.Duration.millis(300));
                        setTooltip(tooltip);
                    } else {
                        setStyle("-fx-alignment: CENTER; -fx-font-size: 20px; -fx-padding: 5px;");
                        setTooltip(null);
                    }
                }
            }
        });
    }

    /**
     * ENHANCED: Multi-Subject Teaching MVP - Phase 2
     * Sets up qualification indicator column showing teacher certification status
     */
    private void setupQualificationColumn() {
        // Qualification indicator column
        qualificationColumn.setCellValueFactory(data -> {
            Course course = data.getValue();
            com.heronix.model.domain.Teacher teacher = course.getTeacher();

            if (teacher == null) {
                return new SimpleStringProperty("—");
            }

            // ✅ FIX: Use same qualification logic as OptaPlanner (4-check system)
            boolean isQualified = isTeacherQualifiedForCourse(teacher, course);

            if (isQualified) {
                if (teacher.hasExpiringCertifications()) {
                    return new SimpleStringProperty("⚠");  // Warning: Expiring soon
                }
                return new SimpleStringProperty("✓");  // Fully qualified
            } else {
                return new SimpleStringProperty("✗");  // Not qualified
            }
        });

        // Add custom cell factory for colored background and tooltips
        qualificationColumn.setCellFactory(column -> new TableCell<Course, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(item);

                    // Set style with proper alignment and background
                    Course course = getTableRow().getItem();
                    if (course != null) {
                        com.heronix.model.domain.Teacher teacher = course.getTeacher();
                        String style = "-fx-alignment: CENTER; -fx-font-size: 18px; -fx-padding: 5px; -fx-font-weight: bold;";
                        String bgColor = switch (item) {
                            case "✓" -> "-fx-background-color: rgba(76, 175, 80, 0.3); -fx-text-fill: #2e7d32;";   // Green
                            case "⚠" -> "-fx-background-color: rgba(255, 193, 7, 0.3); -fx-text-fill: #f57c00;";   // Orange
                            case "✗" -> "-fx-background-color: rgba(244, 67, 54, 0.3); -fx-text-fill: #c62828;";   // Red
                            default -> "-fx-text-fill: #757575;";  // Gray for "—"
                        };
                        setStyle(style + bgColor);

                        // Add tooltip with detailed qualification information
                        String tooltipText;
                        if (teacher == null) {
                            tooltipText = "No Teacher Assigned\n\nPlease assign a teacher to check qualifications.";
                        } else {
                            // ✅ FIX: Use same qualification logic as OptaPlanner (4-check system)
                            boolean isQualified = isTeacherQualifiedForCourse(teacher, course);

                            // Grade level for display purposes in tooltip only
                            String gradeLevel = course.getLevel() != null ? course.getLevel().toString() : "9";

                            if (isQualified && teacher.hasExpiringCertifications()) {
                                tooltipText = String.format(
                                    "⚠ Certified (Expiring Soon)\n\nTeacher: %s\nSubject: %s\nGrade Level: %s\n\n" +
                                    "One or more certifications expire within 90 days.\nPlease renew certifications.",
                                    teacher.getName(),
                                    course.getSubject(),
                                    gradeLevel
                                );
                            } else if (isQualified) {
                                java.util.List<String> certifiedSubjects = teacher.getCertifiedSubjects();
                                tooltipText = String.format(
                                    "✓ Fully Qualified\n\nTeacher: %s\nSubject: %s\nGrade Level: %s\n\n" +
                                    "Certified for: %s",
                                    teacher.getName(),
                                    course.getSubject(),
                                    gradeLevel,
                                    String.join(", ", certifiedSubjects)
                                );
                            } else {
                                java.util.List<String> certifiedSubjects = teacher.getCertifiedSubjects();
                                String certsList = certifiedSubjects.isEmpty() ? "None" : String.join(", ", certifiedSubjects);
                                tooltipText = String.format(
                                    "✗ Not Qualified\n\nTeacher: %s\nRequired: %s (Grade %s)\n\n" +
                                    "Teacher is certified for: %s\n\n" +
                                    "⚠ This assignment may violate compliance requirements.",
                                    teacher.getName(),
                                    course.getSubject(),
                                    gradeLevel,
                                    certsList
                                );
                            }
                        }

                        Tooltip tooltip = new Tooltip(tooltipText);
                        tooltip.setShowDelay(javafx.util.Duration.millis(300));
                        tooltip.setWrapText(true);
                        tooltip.setMaxWidth(400);
                        setTooltip(tooltip);
                    } else {
                        setStyle("-fx-alignment: CENTER; -fx-font-size: 18px; -fx-padding: 5px;");
                        setTooltip(null);
                    }
                }
            }
        });
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════╗
     * ║ PHASE 20: KANBAN-STYLE COLOR CODING FOR ELECTIVES                ║
     * ╚═══════════════════════════════════════════════════════════════════╝
     *
     * Apply visual distinction to elective courses using Kanban methodology.
     * Electives (isCoreRequired=false) get a subtle grey background to make
     * them stand out from core required courses.
     *
     * Visual Hierarchy:
     * - Core Required Courses: White background (default)
     * - Elective Courses: Light grey background (#F5F5F5)
     *
     * This follows Kanban board principles where visual cues help
     * administrators quickly categorize and prioritize work items.
     *
     * @see Course#getIsCoreRequired()
     * @author Heronix Scheduling System Team
     * @since Phase 20 - November 25, 2025
     */
    private void setupElectiveRowStyling() {
        coursesTable.setRowFactory(tv -> new TableRow<Course>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);

                if (empty || course == null) {
                    setStyle("");
                    setTooltip(null);
                } else {
                    // Check if course is an elective (NOT a core required course)
                    Boolean isCoreRequired = course.getIsCoreRequired();

                    if (isCoreRequired != null && !isCoreRequired) {
                        // Elective course - apply grey background (Kanban style)
                        getStyleClass().add("elective-row");

                        // Optional: Add tooltip for clarity
                        Tooltip tooltip = new Tooltip("Elective Course");
                        tooltip.setShowDelay(javafx.util.Duration.millis(500));
                        setTooltip(tooltip);
                    } else {
                        // Core required course - default background
                        getStyleClass().removeAll("elective-row");
                        setTooltip(null);
                    }
                }
            }
        });
    }

    private void setupFilters() {
        subjectFilter.getItems().addAll("All", "Math", "Science", "English", "History",
            "Art", "Music", "PE", "Languages", "Computer Science");
        subjectFilter.setValue("All");

        levelFilter.getItems().addAll("All", "PRE_K", "KINDERGARTEN", "ELEMENTARY",
            "MIDDLE_SCHOOL", "HIGH_SCHOOL", "COLLEGE", "GRADUATE");
        levelFilter.setValue("All");
    }

    /**
     * Setup Quick Teacher Assignment Column - Bug Fix #7
     * Provides a dropdown ComboBox in the table for quick teacher assignment
     * Shows only for unassigned courses to speed up bulk assignment workflow
     */
    private void setupQuickAssignColumn() {
        if (quickAssignColumn == null) {
            log.warn("⚠️ quickAssignColumn is null - column may not be defined in FXML");
            return;
        }

        quickAssignColumn.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<Teacher> teacherCombo = new ComboBox<>();

            {
                teacherCombo.setPromptText("Assign Teacher...");
                teacherCombo.setPrefWidth(180);
                teacherCombo.setConverter(new StringConverter<Teacher>() {
                    @Override
                    public String toString(Teacher teacher) {
                        if (teacher == null) return "";
                        return teacher.getName() + " (" + teacher.getEmployeeId() + ")";
                    }

                    @Override
                    public Teacher fromString(String string) {
                        return null; // Not used
                    }
                });

                // Handle teacher selection
                teacherCombo.setOnAction(e -> {
                    Course course = getTableRow().getItem();
                    Teacher selectedTeacher = teacherCombo.getValue();
                    if (course != null && selectedTeacher != null) {
                        handleQuickAssign(course, selectedTeacher);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Course course = getTableRow().getItem();

                // Only show for unassigned or partially assigned courses
                if (course.getTeacher() == null ||
                    course.getAssignmentStatus() == CourseAssignmentStatus.UNASSIGNED ||
                    course.getAssignmentStatus() == CourseAssignmentStatus.PARTIAL) {

                    // Load teachers asynchronously to avoid UI blocking
                    if (teacherCombo.getItems().isEmpty()) {
                        CompletableFuture.runAsync(() -> {
                            List<Teacher> activeTeachers = teacherRepository.findByActiveTrue();
                            Platform.runLater(() -> {
                                teacherCombo.getItems().clear();
                                teacherCombo.getItems().addAll(activeTeachers);
                            });
                        });
                    }

                    setGraphic(teacherCombo);
                } else {
                    // Course already assigned - show current teacher name
                    Label assignedLabel = new Label("✓ " + course.getTeacher().getName());
                    assignedLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
                    setGraphic(assignedLabel);
                }
            }
        });
    }

    /**
     * Handle quick teacher assignment from dropdown
     */
    private void handleQuickAssign(Course course, Teacher teacher) {
        try {
            log.info("📋 Quick assigning teacher {} to course {}", teacher.getName(), course.getCourseName());

            // Assign teacher to course
            course.setTeacher(teacher);

            // Save to database
            courseRepository.save(course);

            // Refresh table to show updated assignment
            coursesTable.refresh();

            log.info("✅ Teacher {} assigned to course {} successfully",
                teacher.getName(), course.getCourseName());

        } catch (Exception e) {
            log.error("❌ Error assigning teacher to course", e);
            showError("Assignment Failed",
                "Could not assign teacher to course: " + e.getMessage());
        }
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("👁️ View");
            private final Button editBtn = new Button("✏️ Edit");
            private final Button sectionsBtn = new Button("📚 Sections");
            private final Button multiRoomBtn = new Button("🏫 Multi-Room");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, viewBtn, editBtn, sectionsBtn, multiRoomBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);
                viewBtn.setOnAction(e -> handleView(getTableRow().getItem()));
                editBtn.setOnAction(e -> handleEdit(getTableRow().getItem()));
                sectionsBtn.setOnAction(e -> handleManageSections(getTableRow().getItem()));
                sectionsBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 6 3 6; -fx-background-color: #9b59b6; -fx-text-fill: white;");
                sectionsBtn.setTooltip(new Tooltip("Manage course sections (assign teachers, rooms, periods)"));
                multiRoomBtn.setOnAction(e -> handleMultiRoom(getTableRow().getItem()));
                multiRoomBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 6 3 6;");
                multiRoomBtn.setTooltip(new Tooltip("Configure multi-room assignments for this course"));
                deleteBtn.setOnAction(e -> handleDelete(getTableRow().getItem()));
                deleteBtn.getStyleClass().add("button-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadCourses() {
        try {
            // Call service directly - @Transactional will manage session
            // This runs on JavaFX thread but should be fast enough
            List<Course> courses = courseService.findAllWithTeacherCoursesForUI();

            coursesList.clear();
            coursesList.addAll(courses);
            allCourses.clear();
            allCourses.addAll(courses);
            // coursesTable.setItems is now handled by filteredCourses in setupStatusFilters()
            updateRecordCount();
            log.info("Loaded {} courses", courses.size());
        } catch (Exception e) {
            log.error("Error loading courses", e);
            showError("Error", "Failed to load courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            loadCourses();
            return;
        }

        try {
            List<Course> allCoursesList = courseService.findAllWithTeacherCoursesForUI();

            List<Course> filtered = allCoursesList.stream()
                .filter(c -> c.getCourseCode().toLowerCase().contains(query) ||
                            c.getCourseName().toLowerCase().contains(query) ||
                            (c.getSubject() != null && c.getSubject().toLowerCase().contains(query)))
                .toList();

            coursesList.clear();
            coursesList.addAll(filtered);
            updateRecordCount();
        } catch (Exception e) {
            log.error("Error searching courses", e);
            showError("Error", "Failed to search courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        String subject = subjectFilter.getValue();
        String level = levelFilter.getValue();

        try {
            List<Course> allCoursesList = courseService.findAllWithTeacherCoursesForUI();

            List<Course> filtered = allCoursesList.stream()
                .filter(c -> ("All".equals(subject) || (c.getSubject() != null && c.getSubject().equals(subject))))
                .filter(c -> ("All".equals(level) || (c.getLevel() != null && c.getLevel().toString().equals(level))))
                .toList();

            coursesList.clear();
            coursesList.addAll(filtered);
            updateRecordCount();
        } catch (Exception e) {
            log.error("Error filtering courses", e);
            showError("Error", "Failed to filter courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        subjectFilter.setValue("All");
        levelFilter.setValue("All");
        loadCourses();
    }

    @FXML
    private void handleAddCourse() {
        log.info("Add course clicked");
        
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle("Add New Course");
        dialog.setHeaderText("Enter course details");

        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = createCourseForm(null);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                return extractCourseFromForm(grid, null);
            }
            return null;
        });

        Optional<Course> result = dialog.showAndWait();
        result.ifPresent(course -> {
            courseRepository.save(course);
            loadCourses();
            log.info("Course added: {}", course.getCourseCode());
        });
    }

    private void handleView(Course course) {
        if (course == null) return;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Course Details");
        alert.setHeaderText(course.getCourseCode() + " - " + course.getCourseName());
        
        String content = String.format("""
            Subject: %s
            Level: %s
            Sessions/Week: %d
            Duration: %d minutes
            Max Students: %d
            Current Enrollment: %d
            Complexity: %d
            Active: %s
            
            Description:
            %s
            """,
            course.getSubject() != null ? course.getSubject() : "N/A",
            course.getLevel() != null ? course.getLevel() : "N/A",
            course.getSessionsPerWeek() != null ? course.getSessionsPerWeek() : 0,
            course.getDurationMinutes() != null ? course.getDurationMinutes() : 0,
            course.getMaxStudents() != null ? course.getMaxStudents() : 0,
            course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0,
            course.getComplexityScore() != null ? course.getComplexityScore() : 0,
            course.isActive() ? "Yes" : "No",
            course.getDescription() != null ? course.getDescription() : "No description");
        
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleEdit(Course course) {
        if (course == null) return;
        
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle("Edit Course");
        dialog.setHeaderText("Edit " + course.getCourseCode());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = createCourseForm(course);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                return extractCourseFromForm(grid, course);
            }
            return null;
        });

        Optional<Course> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            courseRepository.save(updated);
            loadCourses();
            log.info("Course updated: {}", updated.getCourseCode());
        });
    }

    private void handleDelete(Course course) {
        if (course == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Course");
        confirm.setHeaderText("Delete " + course.getCourseCode() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    courseRepository.delete(course);
                    loadCourses();
                    log.info("Course deleted: {}", course.getCourseCode());
                } catch (Exception e) {
                    log.error("Error deleting course", e);
                    showError("Delete Error", "Failed to delete course: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Handle Multi-Room Assignment button click
     * Phase 6E Integration - December 3, 2025
     */
    private void handleMultiRoom(Course course) {
        if (course == null) return;

        try {
            log.info("🏫 Opening Multi-Room Assignment Dialog for course: {}", course.getCourseCode());

            // Load FXML for Multi-Room Assignment Dialog
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/MultiRoomAssignmentDialog.fxml")
            );

            // Set the controller instance (Spring-managed)
            loader.setController(multiRoomDialogController);

            // Load the dialog pane
            DialogPane dialogPane = loader.load();

            // Create and configure dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Multi-Room Course Assignment");

            // Initialize dialog with course data
            multiRoomDialogController.initializeWithCourse(course);

            // Show dialog and handle result
            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Validate assignments before saving
                if (multiRoomDialogController.validateAssignments()) {
                    // Save assignments
                    multiRoomDialogController.saveAssignments();

                    // Refresh course list to show updated multi-room status
                    loadCourses();

                    log.info("✅ Multi-room assignments saved for course: {}", course.getCourseCode());

                    // Show success message
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Success");
                    success.setHeaderText("Multi-Room Assignments Saved");
                    success.setContentText("Room assignments have been configured for " + course.getCourseCode());
                    success.showAndWait();
                } else {
                    log.warn("⚠ Multi-room assignment validation failed for course: {}", course.getCourseCode());
                }
            } else {
                log.info("Multi-room assignment dialog cancelled for course: {}", course.getCourseCode());
            }

        } catch (Exception e) {
            log.error("❌ Error opening Multi-Room Assignment Dialog", e);
            showError("Dialog Error",
                "Failed to open Multi-Room Assignment Dialog: " + e.getMessage());
        }
    }

    /**
     * Handle Manage Sections button click
     * Opens Section Management Dialog for creating and managing course sections
     * December 21, 2025
     */
    private void handleManageSections(Course course) {
        if (course == null) return;

        try {
            log.info("📚 Opening Section Management Dialog for course: {}", course.getCourseCode());

            // Load FXML for Section Management Dialog
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/SectionManagementDialog.fxml")
            );

            // Use Spring's ApplicationContext to create controller
            loader.setControllerFactory(applicationContext::getBean);

            // Load the dialog content
            javafx.scene.Parent root = loader.load();

            // Get the controller
            SectionManagementDialogController controller = loader.getController();

            // Create dialog stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Manage Sections - " + course.getCourseCode() + " - " + course.getCourseName());
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(coursesTable.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));

            // Initialize controller with course data
            controller.setDialogStage(dialogStage);
            controller.setCourse(course);

            // Show dialog and wait for it to close
            dialogStage.showAndWait();

            // Refresh course list to show updated section counts
            loadCourses();

            log.info("✅ Section Management Dialog closed for course: {}", course.getCourseCode());

        } catch (Exception e) {
            log.error("❌ Error opening Section Management Dialog", e);
            showError("Dialog Error",
                "Failed to open Section Management Dialog: " + e.getMessage());
        }
    }

    private GridPane createCourseForm(Course course) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField codeField = new TextField(course != null ? course.getCourseCode() : "");
        TextField nameField = new TextField(course != null ? course.getCourseName() : "");
        TextField subjectField = new TextField(course != null ? course.getSubject() : "");
        ComboBox<EducationLevel> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll(EducationLevel.values());
        levelCombo.setValue(course != null ? course.getLevel() : EducationLevel.HIGH_SCHOOL);
        levelCombo.setConverter(new StringConverter<EducationLevel>() {
            @Override
            public String toString(EducationLevel level) {
                return level != null ? level.getDisplayName() : "";
            }
            @Override
            public EducationLevel fromString(String string) {
                return null;
            }
        });
        Spinner<Integer> sessionsSpinner = new Spinner<>(1, 10, course != null ? course.getSessionsPerWeek() : 5);
        Spinner<Integer> durationSpinner = new Spinner<>(30, 180, course != null ? course.getDurationMinutes() : 50, 5);
        Spinner<Integer> maxStudentsSpinner = new Spinner<>(1, 100, course != null ? course.getMaxStudents() : 30);
        TextArea descArea = new TextArea(course != null ? course.getDescription() : "");
        descArea.setPrefRowCount(3);

        // Room Assignment ComboBox - Load all available rooms
        ComboBox<com.heronix.model.domain.Room> roomCombo = new ComboBox<>();
        roomCombo.setPromptText("No Room Assigned");

        // Load rooms from database
        try {
            List<com.heronix.model.domain.Room> rooms = roomRepository.findAll();
            roomCombo.getItems().add(null); // Add "No Room Assigned" option
            roomCombo.getItems().addAll(rooms);

            // Set current room if editing
            if (course != null && course.getRoom() != null) {
                roomCombo.setValue(course.getRoom());
            }

            // Display room number and building in dropdown
            roomCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(com.heronix.model.domain.Room room, boolean empty) {
                    super.updateItem(room, empty);
                    if (empty || room == null) {
                        setText("No Room Assigned");
                    } else {
                        String building = room.getBuilding() != null ? room.getBuilding() + " - " : "";
                        String type = room.getRoomType() != null ? " (" + room.getRoomType() + ")" : "";
                        String capacity = room.getCapacity() != null ? " [Cap: " + room.getCapacity() + "]" : "";
                        setText(building + room.getRoomNumber() + type + capacity);
                    }
                }
            });

            roomCombo.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(com.heronix.model.domain.Room room, boolean empty) {
                    super.updateItem(room, empty);
                    if (empty || room == null) {
                        setText("No Room Assigned");
                    } else {
                        String building = room.getBuilding() != null ? room.getBuilding() + " - " : "";
                        String type = room.getRoomType() != null ? " (" + room.getRoomType() + ")" : "";
                        String capacity = room.getCapacity() != null ? " [Cap: " + room.getCapacity() + "]" : "";
                        setText(building + room.getRoomNumber() + type + capacity);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to load rooms for course assignment", e);
        }

        grid.add(new Label("Course Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Course Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Subject:"), 0, 2);
        grid.add(subjectField, 1, 2);
        grid.add(new Label("Level:"), 0, 3);
        grid.add(levelCombo, 1, 3);
        grid.add(new Label("Sessions/Week:"), 0, 4);
        grid.add(sessionsSpinner, 1, 4);
        grid.add(new Label("Duration (min):"), 0, 5);
        grid.add(durationSpinner, 1, 5);
        grid.add(new Label("Max Students:"), 0, 6);
        grid.add(maxStudentsSpinner, 1, 6);
        grid.add(new Label("Assigned Room:"), 0, 7);
        grid.add(roomCombo, 1, 7);
        grid.add(new Label("Description:"), 0, 8);
        grid.add(descArea, 1, 8);

        return grid;
    }

    private Course extractCourseFromForm(GridPane grid, Course existing) {
        Course course = existing != null ? existing : new Course();
        
        course.setCourseCode(((TextField) grid.getChildren().get(1)).getText());
        course.setCourseName(((TextField) grid.getChildren().get(3)).getText());
        course.setSubject(((TextField) grid.getChildren().get(5)).getText());
        course.setLevel(((ComboBox<EducationLevel>) grid.getChildren().get(7)).getValue());
        course.setSessionsPerWeek(((Spinner<Integer>) grid.getChildren().get(9)).getValue());
        course.setDurationMinutes(((Spinner<Integer>) grid.getChildren().get(11)).getValue());
        course.setMaxStudents(((Spinner<Integer>) grid.getChildren().get(13)).getValue());
        course.setRoom(((ComboBox<com.heronix.model.domain.Room>) grid.getChildren().get(15)).getValue()); // Room assignment
        course.setDescription(((TextArea) grid.getChildren().get(17)).getText());
        course.setActive(true);

        return course;
    }

    @FXML
    private void handleRefresh() {
        log.info("Refresh clicked");
        loadCourses();
    }

    @FXML
    private void handleExport() {
        log.info("Export clicked");

        try {
            if (coursesList.isEmpty()) {
                showWarning("No Data", "There are no courses to export.");
                return;
            }

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Courses");
            fileChooser.setInitialFileName("courses_export.csv");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );

            java.io.File file = fileChooser.showSaveDialog(coursesTable.getScene().getWindow());

            if (file != null) {
                byte[] csvData = exportService.exportCoursesToCSV(coursesList);
                java.nio.file.Files.write(file.toPath(), csvData);

                showInfo("Export Successful",
                    String.format("Exported %d courses to %s", coursesList.size(), file.getName()));
                log.info("Exported {} courses to {}", coursesList.size(), file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("Failed to export courses", e);
            showError("Export Failed", "Failed to export courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleStatistics() {
        log.info("Statistics clicked");
        
        long total = coursesList.size();
        long activeCount = coursesList.stream().filter(Course::isActive).count();
        
        showInfo("Course Statistics", 
            String.format("Total Courses: %d\nActive: %d\nInactive: %d", 
                total, activeCount, total - activeCount));
    }

    private void updateRecordCount() {
        recordCountLabel.setText("Total: " + coursesList.size());
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // STATUS FILTER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Setup status filter buttons
     */
    private void setupStatusFilters() {
        // Initialize filtered list
        filteredCourses = new javafx.collections.transformation.FilteredList<>(allCourses, p -> true);
        coursesTable.setItems(filteredCourses);

        // Set initial button states
        updateFilterButtonStates("all");
    }

    /**
     * Show all courses
     */
    @FXML
    private void handleShowAll() {
        filteredCourses.setPredicate(course -> true);
        updateStatusCount("All", coursesList.size());
        updateFilterButtonStates("all");
    }

    /**
     * Show only unassigned courses
     */
    @FXML
    private void handleShowUnassigned() {
        filteredCourses.setPredicate(Course::isUnassigned);
        long count = coursesList.stream().filter(Course::isUnassigned).count();
        updateStatusCount("Unassigned", (int) count);
        updateFilterButtonStates("unassigned");
    }

    /**
     * Show only partially assigned courses
     */
    @FXML
    private void handleShowPartial() {
        filteredCourses.setPredicate(Course::isPartiallyAssigned);
        long count = coursesList.stream().filter(Course::isPartiallyAssigned).count();
        updateStatusCount("Partial", (int) count);
        updateFilterButtonStates("partial");
    }

    /**
     * Show only completely assigned courses
     */
    @FXML
    private void handleShowComplete() {
        filteredCourses.setPredicate(Course::isFullyAssigned);
        long count = coursesList.stream().filter(Course::isFullyAssigned).count();
        updateStatusCount("Complete", (int) count);
        updateFilterButtonStates("complete");
    }

    /**
     * Update status count label
     */
    private void updateStatusCount(String filter, int count) {
        statusCountLabel.setText(String.format("%s: %d courses", filter, count));
    }

    /**
     * Update button styles to show active filter
     */
    private void updateFilterButtonStates(String activeFilter) {
        // Reset all buttons to default style
        showAllButton.getStyleClass().removeAll("button-active");
        showUnassignedButton.getStyleClass().removeAll("button-active");
        showPartialButton.getStyleClass().removeAll("button-active");
        showCompleteButton.getStyleClass().removeAll("button-active");

        // Add active style to selected button
        switch (activeFilter) {
            case "all" -> showAllButton.getStyleClass().add("button-active");
            case "unassigned" -> showUnassignedButton.getStyleClass().add("button-active");
            case "partial" -> showPartialButton.getStyleClass().add("button-active");
            case "complete" -> showCompleteButton.getStyleClass().add("button-active");
        }
    }

    /**
     * Handle Smart Course Assignment
     * Opens dialog to show intelligent teacher-course matching recommendations
     */
    @FXML
    private void handleSmartAssignment() {
        log.info("Smart Course Assignment clicked");

        try {
            // Open dialog
            com.heronix.ui.dialog.SmartCourseAssignmentDialog dialog =
                new com.heronix.ui.dialog.SmartCourseAssignmentDialog(
                    (javafx.stage.Stage) coursesTable.getScene().getWindow(),
                    smartAssignmentService
                );

            Optional<Boolean> result = dialog.showAndWait();

            if (result.isPresent() && result.get()) {
                // User clicked "Apply Selected" button
                java.util.List<com.heronix.dto.CourseAssignmentRecommendation> selectedRecs =
                    dialog.getSelectedRecommendations();

                if (!selectedRecs.isEmpty()) {
                    // Apply the selected recommendations
                    int appliedCount = smartAssignmentService.applyRecommendations(selectedRecs);

                    showInfo("Assignments Applied",
                        String.format("Successfully applied %d course assignments!", appliedCount));

                    // Refresh the table
                    loadCourses();
                } else {
                    showWarning("No Selection", "No recommendations were selected.");
                }
            }

        } catch (Exception e) {
            log.error("Smart assignment failed", e);
            showError("Smart Assignment Failed",
                "Failed to perform smart assignment: " + e.getMessage());
        }
    }

    /**
     * Handle Compliance Audit
     * Opens dialog to show certification compliance violations with detailed
     * regulatory information and links to state/federal resources
     */
    @FXML
    private void handleComplianceAudit() {
        log.info("Compliance Audit clicked");

        try {
            // Open compliance audit dialog
            com.heronix.ui.dialog.ComplianceAuditDialog dialog =
                new com.heronix.ui.dialog.ComplianceAuditDialog(
                    (javafx.stage.Stage) coursesTable.getScene().getWindow(),
                    complianceValidationService
                );

            Optional<Boolean> result = dialog.showAndWait();

            if (result.isPresent() && result.get()) {
                // User clicked "Apply Corrections" button
                java.util.List<com.heronix.dto.ComplianceViolation> selectedViolations =
                    dialog.getSelectedViolations();

                if (!selectedViolations.isEmpty()) {
                    // Build recommendations from violations that have qualified teachers
                    java.util.List<com.heronix.dto.CourseAssignmentRecommendation> recommendations =
                        new java.util.ArrayList<>();

                    for (com.heronix.dto.ComplianceViolation violation : selectedViolations) {
                        if (violation.isAutoCorrectAvailable() &&
                            !violation.getQualifiedTeachers().isEmpty()) {

                            // Create recommendation for the first qualified teacher
                            com.heronix.model.domain.Teacher bestTeacher =
                                violation.getQualifiedTeachers().get(0);

                            com.heronix.dto.CourseAssignmentRecommendation rec =
                                com.heronix.dto.CourseAssignmentRecommendation.builder()
                                    .course(violation.getCourse())
                                    .currentTeacher(violation.getTeacher())
                                    .recommendedTeacher(bestTeacher)
                                    .matchScore(100) // Qualified teacher
                                    .priority(com.heronix.dto.CourseAssignmentRecommendation.RecommendationPriority.CRITICAL)
                                    .reasoning("Compliance violation fix: " + violation.getDescription())
                                    .build();

                            recommendations.add(rec);
                        }
                    }

                    if (!recommendations.isEmpty()) {
                        // Apply the corrections
                        int appliedCount = smartAssignmentService.applyRecommendations(recommendations);

                        showInfo("Compliance Corrections Applied",
                            String.format("Successfully corrected %d compliance violations!\n" +
                                "Teachers have been reassigned to meet certification requirements.",
                                appliedCount));

                        // Refresh the table
                        loadCourses();
                    } else {
                        showWarning("No Auto-Corrections Available",
                            "The selected violations cannot be automatically corrected.\n" +
                            "Manual intervention required (hire certified teachers, provide training, etc.)");
                    }
                } else {
                    showWarning("No Selection", "No violations were selected for correction.");
                }
            }

        } catch (Exception e) {
            log.error("Compliance audit failed", e);
            showError("Compliance Audit Failed",
                "Failed to perform compliance audit: " + e.getMessage());
        }
    }

    // ========================================================================
    // HELPER METHODS - OptaPlanner Alignment
    // ========================================================================

    /**
     * Check if teacher is qualified for a course using OptaPlanner's 4-check system
     * This ensures UI qualification display matches OptaPlanner's actual assignment logic
     *
     * @param teacher The teacher to check
     * @param course The course to check qualification for
     * @return true if teacher is qualified via any of the 4 checks
     */
    private boolean isTeacherQualifiedForCourse(com.heronix.model.domain.Teacher teacher, Course course) {
        if (teacher == null || course == null) {
            return false;
        }

        // CHECK #1: Explicit course assignment (highest priority)
        // Teacher has been manually assigned to teach this specific course
        if (teacher.getCourses() != null && teacher.getCourses().contains(course)) {
            return true;
        }

        // CHECK #2: SubjectCertification (primary certification system)
        // Teacher has valid certification for the course's subject (no grade requirement)
        if (course.getSubject() != null && teacher.hasCertificationForSubject(course.getSubject())) {
            return true;
        }

        // CHECK #3: Legacy certifications (fallback for backward compatibility)
        // String-based certification list with substring matching
        if (teacher.getCertifications() != null && course.getSubject() != null) {
            String courseSubject = course.getSubject().toLowerCase().trim();
            for (String cert : teacher.getCertifications()) {
                if (cert != null && cert.toLowerCase().trim().contains(courseSubject)) {
                    return true;
                }
            }
        }

        // CHECK #4: Department-based inference
        // Teacher's department matches course subject (e.g., "Physical Education" → "Weights")
        if (teacher.getDepartment() != null && course.getSubject() != null) {
            String department = teacher.getDepartment().toLowerCase().trim();
            String subject = course.getSubject().toLowerCase().trim();

            if (department.contains(subject) || subject.contains(department)) {
                return true;  // Valid via department inference
            }
        }

        // No qualification found via any of the 4 checks
        return false;
    }
}