package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.service.StudentEnrollmentService;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.CourseRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Student Course Enrollment Management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Controller
public class StudentEnrollmentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentEnrollmentController.class);

    @Autowired
    private StudentEnrollmentService enrollmentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    // ==================== UI COMPONENTS ====================

    // Student Selection
    @FXML private ComboBox<Student> studentComboBox;
    @FXML private TextField studentSearchField;
    @FXML private Label studentInfoLabel;
    @FXML private Label enrollmentStatsLabel;

    // Enrolled Courses Table
    @FXML private TableView<Course> enrolledCoursesTable;
    @FXML private TableColumn<Course, String> enrolledCodeColumn;
    @FXML private TableColumn<Course, String> enrolledNameColumn;
    @FXML private TableColumn<Course, String> enrolledSubjectColumn;
    @FXML private TableColumn<Course, String> enrolledLevelColumn;
    @FXML private TableColumn<Course, String> enrolledTeacherColumn;
    @FXML private TableColumn<Course, Void> enrolledActionsColumn;

    // Available Courses Table
    @FXML private TableView<Course> availableCoursesTable;
    @FXML private TableColumn<Course, String> availableCodeColumn;
    @FXML private TableColumn<Course, String> availableNameColumn;
    @FXML private TableColumn<Course, String> availableSubjectColumn;
    @FXML private TableColumn<Course, String> availableLevelColumn;
    @FXML private TableColumn<Course, String> availableTeacherColumn;
    @FXML private TableColumn<Course, String> availableCapacityColumn;
    @FXML private TableColumn<Course, Void> availableActionsColumn;

    // Filters
    @FXML private ComboBox<String> subjectFilterComboBox;
    @FXML private ComboBox<String> levelFilterComboBox;
    @FXML private CheckBox showFullCoursesCheckBox;

    // Bulk Operations
    @FXML private Button bulkEnrollButton;
    @FXML private Button bulkUnenrollButton;

    private Student selectedStudent;
    private ObservableList<Course> enrolledCourses = FXCollections.observableArrayList();
    private ObservableList<Course> availableCourses = FXCollections.observableArrayList();

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Student Enrollment Controller");

        setupStudentSelection();
        setupEnrolledCoursesTable();
        setupAvailableCoursesTable();
        setupFilters();
        loadStudents();
    }

    /**
     * Setup student selection components
     */
    private void setupStudentSelection() {
        studentComboBox.setConverter(new javafx.util.StringConverter<Student>() {
            @Override
            public String toString(Student student) {
                return student != null ? student.getFullName() + " (" + student.getStudentId() + ")" : "";
            }

            @Override
            public Student fromString(String string) {
                return null;
            }
        });

        studentComboBox.setOnAction(e -> {
            selectedStudent = studentComboBox.getValue();
            if (selectedStudent != null) {
                loadStudentEnrollments();
                updateStudentInfo();
            }
        });
    }

    /**
     * Setup enrolled courses table
     */
    private void setupEnrolledCoursesTable() {
        enrolledCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        enrolledNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        enrolledSubjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));

        enrolledLevelColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLevel() != null ?
                        cellData.getValue().getLevel().toString() : ""));

        enrolledTeacherColumn.setCellValueFactory(cellData -> {
            try {
                return new SimpleStringProperty(cellData.getValue().getTeacher() != null ?
                        cellData.getValue().getTeacher().getName() : "TBD");
            } catch (Exception e) {
                return new SimpleStringProperty("TBD");
            }
        });

        // Add unenroll action button
        enrolledActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button unenrollButton = new Button("Drop");

            {
                unenrollButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                unenrollButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    handleUnenroll(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : unenrollButton);
            }
        });

        enrolledCoursesTable.setItems(enrolledCourses);
    }

    /**
     * Setup available courses table
     */
    private void setupAvailableCoursesTable() {
        availableCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        availableNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        availableSubjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));

        availableLevelColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLevel() != null ?
                        cellData.getValue().getLevel().toString() : ""));

        availableTeacherColumn.setCellValueFactory(cellData -> {
            try {
                return new SimpleStringProperty(cellData.getValue().getTeacher() != null ?
                        cellData.getValue().getTeacher().getName() : "TBD");
            } catch (Exception e) {
                return new SimpleStringProperty("TBD");
            }
        });

        availableCapacityColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue();
            int enrolled = course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0;
            int max = course.getMaxStudents() != null ? course.getMaxStudents() : 30;
            return new SimpleStringProperty(enrolled + "/" + max);
        });

        // Add enroll action button
        availableActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button enrollButton = new Button("Enroll");

            {
                enrollButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                enrollButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    handleEnroll(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()).isFull()) {
                    setGraphic(null);
                } else {
                    setGraphic(enrollButton);
                }
            }
        });

        availableCoursesTable.setItems(availableCourses);
        availableCoursesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * Setup filter components
     */
    private void setupFilters() {
        // Subject filter
        subjectFilterComboBox.getItems().addAll("All Subjects", "Math", "Science", "English",
                "History", "Art", "Music", "PE", "Foreign Language");
        subjectFilterComboBox.setValue("All Subjects");
        subjectFilterComboBox.setOnAction(e -> applyFilters());

        // Level filter
        levelFilterComboBox.getItems().addAll("All Levels", "ELEMENTARY", "MIDDLE_SCHOOL", "HIGH_SCHOOL");
        levelFilterComboBox.setValue("All Levels");
        levelFilterComboBox.setOnAction(e -> applyFilters());

        // Show full courses checkbox
        showFullCoursesCheckBox.setOnAction(e -> applyFilters());
    }

    /**
     * Load all students
     */
    private void loadStudents() {
        List<Student> students = studentRepository.findByActiveTrue();
        studentComboBox.setItems(FXCollections.observableArrayList(students));
        logger.info("Loaded {} students", students.size());
    }

    /**
     * Load enrolled courses for selected student
     */
    private void loadStudentEnrollments() {
        if (selectedStudent == null) return;

        enrolledCourses.clear();
        List<Course> enrolled = enrollmentService.getStudentEnrollments(selectedStudent.getId());
        enrolledCourses.addAll(enrolled);

        loadAvailableCourses();
        logger.info("Student {} enrolled in {} courses", selectedStudent.getStudentId(), enrolled.size());
    }

    /**
     * Load available courses for selected student
     */
    private void loadAvailableCourses() {
        if (selectedStudent == null) return;

        List<Course> available = enrollmentService.getAvailableCoursesForStudent(selectedStudent.getId());
        availableCourses.clear();
        availableCourses.addAll(available);

        applyFilters();
    }

    /**
     * Apply filters to available courses
     */
    private void applyFilters() {
        if (selectedStudent == null) return;

        List<Course> filtered = enrollmentService.getAvailableCoursesForStudent(selectedStudent.getId());

        // Subject filter
        String subject = subjectFilterComboBox.getValue();
        if (subject != null && !subject.equals("All Subjects")) {
            filtered = filtered.stream()
                    .filter(c -> c.getSubject() != null && c.getSubject().equalsIgnoreCase(subject))
                    .collect(Collectors.toList());
        }

        // Level filter
        String level = levelFilterComboBox.getValue();
        if (level != null && !level.equals("All Levels")) {
            filtered = filtered.stream()
                    .filter(c -> c.getLevel() != null && c.getLevel().toString().equals(level))
                    .collect(Collectors.toList());
        }

        // Show full courses
        if (!showFullCoursesCheckBox.isSelected()) {
            filtered = filtered.stream()
                    .filter(c -> !c.isFull())
                    .collect(Collectors.toList());
        }

        availableCourses.clear();
        availableCourses.addAll(filtered);
    }

    /**
     * Update student info label
     */
    private void updateStudentInfo() {
        if (selectedStudent == null) {
            studentInfoLabel.setText("No student selected");
            enrollmentStatsLabel.setText("");
            return;
        }

        studentInfoLabel.setText(String.format("%s - %s (%s)",
                selectedStudent.getFullName(),
                selectedStudent.getGradeLevel() != null ? selectedStudent.getGradeLevel() : "N/A",
                selectedStudent.getStudentId()));

        StudentEnrollmentService.EnrollmentStats stats =
                enrollmentService.getStudentStats(selectedStudent.getId());
        enrollmentStatsLabel.setText(String.format("Enrolled: %d courses | Total Credits: %d",
                stats.getEnrolledCourses(), stats.getTotalCredits()));
    }

    /**
     * Handle enroll button click
     */
    private void handleEnroll(Course course) {
        if (selectedStudent == null) {
            showError("No Student Selected", "Please select a student first.");
            return;
        }

        try {
            enrollmentService.enrollStudent(selectedStudent.getId(), course.getId());
            showInfo("Success", "Enrolled in " + course.getCourseCode() + " - " + course.getCourseName());
            loadStudentEnrollments();
            updateStudentInfo();
        } catch (Exception e) {
            logger.error("Enrollment failed", e);
            showError("Enrollment Failed", e.getMessage());
        }
    }

    /**
     * Handle unenroll button click
     */
    private void handleUnenroll(Course course) {
        if (selectedStudent == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Unenrollment");
        confirm.setHeaderText("Drop Course");
        confirm.setContentText("Are you sure you want to drop " + course.getCourseCode() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                enrollmentService.unenrollStudent(selectedStudent.getId(), course.getId());
                showInfo("Success", "Dropped from " + course.getCourseCode());
                loadStudentEnrollments();
                updateStudentInfo();
            } catch (Exception e) {
                logger.error("Unenrollment failed", e);
                showError("Unenrollment Failed", e.getMessage());
            }
        }
    }

    /**
     * Handle bulk enroll
     */
    @FXML
    private void handleBulkEnroll() {
        if (selectedStudent == null) {
            showError("No Student Selected", "Please select a student first.");
            return;
        }

        List<Course> selected = availableCoursesTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showError("No Courses Selected", "Please select courses to enroll in.");
            return;
        }

        List<Long> courseIds = selected.stream().map(Course::getId).collect(Collectors.toList());

        try {
            enrollmentService.bulkEnrollCourses(selectedStudent.getId(), courseIds);
            showInfo("Success", "Enrolled in " + selected.size() + " courses");
            loadStudentEnrollments();
            updateStudentInfo();
        } catch (Exception e) {
            logger.error("Bulk enrollment failed", e);
            showError("Bulk Enrollment Failed", e.getMessage());
        }
    }

    /**
     * Handle bulk unenroll
     */
    @FXML
    private void handleBulkUnenroll() {
        if (selectedStudent == null) return;

        List<Course> selected = enrolledCoursesTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showError("No Courses Selected", "Please select courses to drop.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Bulk Unenrollment");
        confirm.setHeaderText("Drop Multiple Courses");
        confirm.setContentText("Drop " + selected.size() + " courses?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int successCount = 0;
            for (Course course : selected) {
                try {
                    enrollmentService.unenrollStudent(selectedStudent.getId(), course.getId());
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to drop course {}", course.getCourseCode(), e);
                }
            }

            showInfo("Success", "Dropped " + successCount + " courses");
            loadStudentEnrollments();
            updateStudentInfo();
        }
    }

    /**
     * Handle search students
     */
    @FXML
    private void handleSearchStudents() {
        String query = studentSearchField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadStudents();
            return;
        }

        List<Student> results = studentRepository.searchByName(query.trim());
        studentComboBox.setItems(FXCollections.observableArrayList(results));
    }

    /**
     * Handle refresh
     */
    @FXML
    private void handleRefresh() {
        loadStudents();
        if (selectedStudent != null) {
            loadStudentEnrollments();
            updateStudentInfo();
        }
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
