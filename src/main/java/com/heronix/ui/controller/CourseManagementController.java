// ============================================================================
// FILE: CourseManagementController.java - COMPLETE WITH ALL METHODS
// LOCATION: src/main/java/com/eduscheduler/ui/controller/CourseManagementController.java
// ============================================================================

package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.model.domain.Room;
import com.heronix.model.enums.EducationLevel;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.repository.RoomRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CourseManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> levelFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Course> courseTable;
    @FXML private Label recordCountLabel;
    @FXML private TableColumn<Course, Long> idColumn;
    @FXML private TableColumn<Course, String> courseCodeColumn;
    @FXML private TableColumn<Course, String> courseNameColumn;
    @FXML private TableColumn<Course, String> subjectColumn;
    @FXML private TableColumn<Course, String> levelColumn;
    @FXML private TableColumn<Course, String> teacherColumn;
    @FXML private TableColumn<Course, Integer> maxStudentsColumn;
    @FXML private TableColumn<Course, Integer> enrollmentColumn;
    @FXML private TableColumn<Course, Boolean> activeColumn;
    @FXML private HBox selectionToolbar;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private com.heronix.service.ExportService exportService;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        setupBulkSelection();
        loadCourses();
    }

    private void setupBulkSelection() {
        // Enable multi-selection
        com.heronix.ui.util.TableSelectionHelper.enableMultiSelection(courseTable);

        // Create selection toolbar
        HBox toolbar = com.heronix.ui.util.TableSelectionHelper.createSelectionToolbar(
            courseTable,
            this::handleBulkDelete,
            "Courses"
        );

        // Replace the placeholder with the actual toolbar
        if (selectionToolbar != null) {
            selectionToolbar.getChildren().setAll(toolbar.getChildren());
            selectionToolbar.setPadding(toolbar.getPadding());
            selectionToolbar.setSpacing(toolbar.getSpacing());
            selectionToolbar.setStyle(toolbar.getStyle());
        }
    }

    private void handleBulkDelete(List<Course> courses) {
        try {
            for (Course course : courses) {
                courseRepository.delete(course);
                log.info("Deleted course: {} (ID: {})", course.getCourseName(), course.getId());
            }

            // Reload the table
            loadCourses();

            log.info("Bulk delete completed: {} courses deleted", courses.size());
        } catch (Exception e) {
            log.error("Error during bulk delete", e);
            throw e; // Let TableSelectionHelper show the error dialog
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        
        levelColumn.setCellValueFactory(cellData -> {
            EducationLevel level = cellData.getValue().getLevel();
            return new javafx.beans.property.SimpleStringProperty(
                    level != null ? level.getDisplayName() : "N/A");
        });

        teacherColumn.setCellValueFactory(cellData -> {
            Teacher teacher = cellData.getValue().getTeacher();
            return new javafx.beans.property.SimpleStringProperty(
                    teacher != null ? teacher.getName() : "Unassigned");
        });

        maxStudentsColumn.setCellValueFactory(new PropertyValueFactory<>("maxStudents"));
        enrollmentColumn.setCellValueFactory(new PropertyValueFactory<>("currentEnrollment"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
    }

    private void setupFilters() {
        List<String> subjects = courseRepository.findAll().stream()
                .map(Course::getSubject)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        subjectFilter.setItems(FXCollections.observableArrayList("All Subjects"));
        subjectFilter.getItems().addAll(subjects);
        subjectFilter.setValue("All Subjects");

        levelFilter.setItems(FXCollections.observableArrayList("All Levels"));
        for (EducationLevel level : EducationLevel.values()) {
            levelFilter.getItems().add(level.getDisplayName());
        }
        levelFilter.setValue("All Levels");

        statusFilter.setItems(FXCollections.observableArrayList("All", "Active", "Inactive"));
        statusFilter.setValue("All");
    }

    // ========================================================================
    // FXML EVENT HANDLERS
    // ========================================================================

    @FXML
    private void handleSearch() {
        log.info("Search triggered");
        filterCourses();
    }

    @FXML
    private void handleFilter() {
        log.info("Filter triggered");
        filterCourses();
    }

    @FXML
    private void handleAddCourse() {
        log.info("Add course clicked");

        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle("Add New Course");
        dialog.setHeaderText("Enter course information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField courseCodeField = new TextField();
        courseCodeField.setPromptText("e.g., MATH-101");
        TextField courseNameField = new TextField();
        courseNameField.setPromptText("e.g., Algebra I");
        TextField subjectField = new TextField();
        subjectField.setPromptText("e.g., Mathematics");

        // Activity Type field (Phase 5F) - for PE courses
        ComboBox<String> activityTypeCombo = new ComboBox<>();
        activityTypeCombo.setItems(FXCollections.observableArrayList(
            "General PE", "Basketball", "Volleyball", "Soccer", "Indoor Soccer", "Badminton",
            "Weights", "Strength Training", "Conditioning", "Powerlifting",
            "Dance", "Aerobics", "Yoga", "Zumba",
            "Karate", "Martial Arts", "Self Defense", "Boxing",
            "Wrestling", "Grappling",
            "Track & Field", "Running",
            "Swimming", "Water Polo",
            "Gymnastics", "Other"
        ));
        activityTypeCombo.setEditable(true);
        activityTypeCombo.setPromptText("Select activity type");
        Label activityTypeHelp = new Label("For PE/specialized courses only");
        activityTypeHelp.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        // Hide activity type by default, show only for PE courses
        activityTypeCombo.setVisible(false);
        activityTypeCombo.setManaged(false);
        activityTypeHelp.setVisible(false);
        activityTypeHelp.setManaged(false);

        ComboBox<EducationLevel> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll(EducationLevel.values());
        levelCombo.setPromptText("Select level");
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
        TextField durationField = new TextField("50");
        durationField.setPromptText("Duration in minutes");
        TextField maxStudentsField = new TextField("30");
        maxStudentsField.setPromptText("Maximum students");
        CheckBox requiresLabCheck = new CheckBox();
        CheckBox activeCheck = new CheckBox();
        activeCheck.setSelected(true);

        // Phase 6D: Equipment Requirements
        CheckBox requiresProjectorCheck = new CheckBox();
        CheckBox requiresSmartboardCheck = new CheckBox();
        CheckBox requiresComputersCheck = new CheckBox();

        ComboBox<RoomType> requiredRoomTypeCombo = new ComboBox<>();
        requiredRoomTypeCombo.getItems().add(null);  // "Not Required" option
        requiredRoomTypeCombo.getItems().addAll(RoomType.values());
        requiredRoomTypeCombo.setPromptText("Not Required");
        requiredRoomTypeCombo.setConverter(new StringConverter<RoomType>() {
            @Override
            public String toString(RoomType roomType) {
                return roomType != null ? roomType.getDisplayName() : "Not Required";
            }
            @Override
            public RoomType fromString(String string) {
                return null;
            }
        });

        TextField additionalEquipmentField = new TextField();
        additionalEquipmentField.setPromptText("e.g., Microscopes, 3D Printer (comma-separated)");
        Label equipmentHelp = new Label("Specialized equipment needs beyond standard projector/computers");
        equipmentHelp.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        // Show/hide activity type based on subject
        subjectField.textProperty().addListener((obs, old, newVal) -> {
            boolean isPE = newVal != null && (newVal.toLowerCase().contains("physical") ||
                                             newVal.toLowerCase().contains("pe") ||
                                             newVal.toLowerCase().contains("athletics"));
            activityTypeCombo.setVisible(isPE);
            activityTypeCombo.setManaged(isPE);
            activityTypeHelp.setVisible(isPE);
            activityTypeHelp.setManaged(isPE);
        });

        grid.add(new Label("Course Code:*"), 0, 0);
        grid.add(courseCodeField, 1, 0);
        grid.add(new Label("Course Name:*"), 0, 1);
        grid.add(courseNameField, 1, 1);
        grid.add(new Label("Subject:"), 0, 2);
        grid.add(subjectField, 1, 2);
        grid.add(new Label("Activity Type:"), 0, 3);
        grid.add(activityTypeCombo, 1, 3);
        grid.add(new Label(""), 0, 4);
        grid.add(activityTypeHelp, 1, 4);
        grid.add(new Label("Level:"), 0, 5);
        grid.add(levelCombo, 1, 5);
        grid.add(new Label("Duration (min):"), 0, 6);
        grid.add(durationField, 1, 6);
        grid.add(new Label("Max Students:"), 0, 7);
        grid.add(maxStudentsField, 1, 7);
        grid.add(new Label("Requires Lab:"), 0, 8);
        grid.add(requiresLabCheck, 1, 8);

        // Phase 6D: Equipment Requirements Section
        grid.add(new Label(""), 0, 9);  // Spacer
        Label equipmentHeader = new Label("Equipment Requirements:");
        equipmentHeader.setStyle("-fx-font-weight: bold;");
        grid.add(equipmentHeader, 0, 10);

        grid.add(new Label("Requires Projector:"), 0, 11);
        grid.add(requiresProjectorCheck, 1, 11);
        grid.add(new Label("Requires Smartboard:"), 0, 12);
        grid.add(requiresSmartboardCheck, 1, 12);
        grid.add(new Label("Requires Computers:"), 0, 13);
        grid.add(requiresComputersCheck, 1, 13);
        grid.add(new Label("Required Room Type:"), 0, 14);
        grid.add(requiredRoomTypeCombo, 1, 14);
        grid.add(new Label("Additional Equipment:"), 0, 15);
        grid.add(additionalEquipmentField, 1, 15);
        grid.add(new Label(""), 0, 16);
        grid.add(equipmentHelp, 1, 16);

        grid.add(new Label("Active:"), 0, 17);
        grid.add(activeCheck, 1, 17);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable save button
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        courseCodeField.textProperty().addListener((obs, old, newVal) ->
            saveButton.setDisable(newVal.trim().isEmpty() || courseNameField.getText().trim().isEmpty()));
        courseNameField.textProperty().addListener((obs, old, newVal) ->
            saveButton.setDisable(newVal.trim().isEmpty() || courseCodeField.getText().trim().isEmpty()));

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Course course = new Course();
                course.setCourseCode(courseCodeField.getText().trim());
                course.setCourseName(courseNameField.getText().trim());
                course.setSubject(subjectField.getText().trim());
                course.setLevel(levelCombo.getValue());

                // Set activity type (Phase 5F)
                if (activityTypeCombo.getValue() != null && !activityTypeCombo.getValue().isEmpty()) {
                    course.setActivityType(activityTypeCombo.getValue());
                }

                try {
                    course.setDurationMinutes(Integer.parseInt(durationField.getText().trim()));
                } catch (NumberFormatException e) {
                    course.setDurationMinutes(50);
                }

                try {
                    course.setMaxStudents(Integer.parseInt(maxStudentsField.getText().trim()));
                } catch (NumberFormatException e) {
                    course.setMaxStudents(30);
                }

                course.setRequiresLab(requiresLabCheck.isSelected());
                course.setActive(activeCheck.isSelected());

                // Phase 6D: Set equipment requirements
                course.setRequiresProjector(requiresProjectorCheck.isSelected());
                course.setRequiresSmartboard(requiresSmartboardCheck.isSelected());
                course.setRequiresComputers(requiresComputersCheck.isSelected());
                course.setRequiredRoomType(requiredRoomTypeCombo.getValue());
                if (additionalEquipmentField.getText() != null && !additionalEquipmentField.getText().trim().isEmpty()) {
                    course.setAdditionalEquipment(additionalEquipmentField.getText().trim());
                }

                return course;
            }
            return null;
        });

        // Show dialog and save
        dialog.showAndWait().ifPresent(course -> {
            try {
                // Check for duplicate course code
                if (courseRepository.findByCourseCode(course.getCourseCode()).isPresent()) {
                    showError("Duplicate Course", "A course with code '" + course.getCourseCode() + "' already exists.");
                    return;
                }

                Course saved = courseRepository.save(course);
                loadCourses();
                showInfo("Success", "Course '" + saved.getCourseName() + "' added successfully!");
                log.info("Added course: {}", saved.getCourseCode());

            } catch (Exception e) {
                log.error("Failed to save course", e);
                showError("Error", "Failed to save course: " + e.getMessage());
            }
        });
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
            List<Course> coursesToExport = courseTable.getItems();

            if (coursesToExport.isEmpty()) {
                showWarning("No Data", "There are no courses to export.");
                return;
            }

            // Create file chooser
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Courses");
            fileChooser.setInitialFileName("courses_export.xlsx");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );

            java.io.File file = fileChooser.showSaveDialog(courseTable.getScene().getWindow());

            if (file != null) {
                byte[] excelData = exportService.exportCoursesToExcel(coursesToExport);
                java.nio.file.Files.write(file.toPath(), excelData);

                showInfo("Export Successful",
                    String.format("Exported %d courses to %s", coursesToExport.size(), file.getName()));
                log.info("Exported {} courses to {}", coursesToExport.size(), file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("Failed to export courses", e);
            showError("Export Failed", "Failed to export courses: " + e.getMessage());
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void loadCourses() {
        List<Course> courses = courseRepository.findAll();
        courseTable.setItems(FXCollections.observableArrayList(courses));
        recordCountLabel.setText(courses.size() + " courses");
    }

    private void filterCourses() {
        String searchText = searchField.getText().toLowerCase();
        String subject = subjectFilter.getValue();
        String level = levelFilter.getValue();
        String status = statusFilter.getValue();

        List<Course> filtered = courseRepository.findAll().stream()
                .filter(c -> searchText.isEmpty() ||
                        c.getCourseCode().toLowerCase().contains(searchText) ||
                        c.getCourseName().toLowerCase().contains(searchText))
                .filter(c -> "All Subjects".equals(subject) ||
                        subject.equals(c.getSubject()))
                .filter(c -> "All Levels".equals(level) ||
                        (c.getLevel() != null && level.equals(c.getLevel().getDisplayName())))
                .filter(c -> "All".equals(status) ||
                        ("Active".equals(status) && c.isActive()) ||
                        ("Inactive".equals(status) && !c.isActive()))
                .collect(Collectors.toList());

        courseTable.setItems(FXCollections.observableArrayList(filtered));
        recordCountLabel.setText(filtered.size() + " courses");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}