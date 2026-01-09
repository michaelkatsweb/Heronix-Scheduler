package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.model.dto.CourseSuggestion;
import com.heronix.model.dto.StudentSearchCriteria;
import com.heronix.repository.CourseRepository;
import com.heronix.service.BulkEnrollmentService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Bulk Enrollment Manager.
 *
 * Provides smart search and bulk course assignment for students.
 *
 * @author Heronix Scheduler Team
 */
@Controller
public class BulkEnrollmentController {

    private static final Logger log = LoggerFactory.getLogger(BulkEnrollmentController.class);

    @Autowired
    private BulkEnrollmentService bulkEnrollmentService;

    @Autowired
    private CourseRepository courseRepository;

    // Search Filters
    @FXML
    private ComboBox<String> gradeLevelComboBox;

    @FXML
    private ComboBox<String> iepComboBox;

    @FXML
    private ComboBox<String> plan504ComboBox;

    @FXML
    private ComboBox<String> graduatedComboBox;

    @FXML
    private TextField minCoursesField;

    @FXML
    private TextField maxCoursesField;

    @FXML
    private TextField nameSearchField;

    @FXML
    private TextField studentIdField;

    // Results
    @FXML
    private Label resultsCountLabel;

    @FXML
    private TableView<StudentSelection> studentsTable;

    @FXML
    private TableColumn<StudentSelection, Boolean> selectColumn;

    @FXML
    private TableColumn<StudentSelection, String> studentIdColumn;

    @FXML
    private TableColumn<StudentSelection, String> nameColumn;

    @FXML
    private TableColumn<StudentSelection, String> gradeColumn;

    @FXML
    private TableColumn<StudentSelection, Integer> coursesColumn;

    @FXML
    private TableColumn<StudentSelection, String> iepColumn;

    @FXML
    private TableColumn<StudentSelection, String> plan504Column;

    @FXML
    private TableColumn<StudentSelection, String> graduatedStatusColumn;

    // Actions
    @FXML
    private ComboBox<Course> courseComboBox;

    @FXML
    private Label courseInfoLabel;

    @FXML
    private CheckBox applyRequiredCheckbox;

    @FXML
    private CheckBox applyRecommendedCheckbox;

    private ObservableList<StudentSelection> studentSelections;
    private List<Course> allCourses;

    /**
     * Wrapper class for student with selection state.
     */
    public static class StudentSelection {
        private final SimpleBooleanProperty selected;
        private final Student student;

        public StudentSelection(Student student) {
            this.student = student;
            this.selected = new SimpleBooleanProperty(false);
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean value) {
            selected.set(value);
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public Student getStudent() {
            return student;
        }

        // Convenience getters for table columns
        public String getStudentId() {
            return student.getStudentId();
        }

        public String getName() {
            return student.getFirstName() + " " + student.getLastName();
        }

        public String getGradeLevel() {
            return student.getGradeLevel();
        }

        public Integer getEnrolledCourseCount() {
            return student.getEnrolledCourseCount();
        }

        public String getHasIEP() {
            return student.getHasIEP() != null && student.getHasIEP() ? "Yes" : "No";
        }

        public String getHas504() {
            return student.getHas504Plan() != null && student.getHas504Plan() ? "Yes" : "No";
        }

        public String getGraduatedStatus() {
            if (student.getGraduated() != null && student.getGraduated()) {
                return "Graduated";
            }
            return "Active";
        }
    }

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        log.info("Initializing Bulk Enrollment Controller");

        // Setup table columns
        setupTableColumns();

        // Load courses
        loadCourses();

        // Populate ComboBox items
        gradeLevelComboBox.getItems().addAll("All", "9", "10", "11", "12");

        // Set default filter values
        gradeLevelComboBox.setValue("All");
        iepComboBox.setValue("Any");
        plan504ComboBox.setValue("Any");
        graduatedComboBox.setValue("Active Only");

        // Initialize table
        studentSelections = FXCollections.observableArrayList();
        studentsTable.setItems(studentSelections);
    }

    /**
     * Setup table columns.
     */
    private void setupTableColumns() {
        // Select column with checkbox
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // Student ID
        studentIdColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStudentId()));

        // Name
        nameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getName()));

        // Grade
        gradeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getGradeLevel()));

        // Courses count
        coursesColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledCourseCount"));

        // IEP
        iepColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getHasIEP()));

        // 504
        plan504Column.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getHas504()));

        // Status
        graduatedStatusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getGraduatedStatus()));

        // Make table editable for checkboxes
        studentsTable.setEditable(true);
    }

    /**
     * Load all courses into dropdown.
     */
    private void loadCourses() {
        allCourses = courseRepository.findAll();
        courseComboBox.setItems(FXCollections.observableArrayList(allCourses));

        // Custom string converter for course display
        courseComboBox.setConverter(new StringConverter<Course>() {
            @Override
            public String toString(Course course) {
                if (course == null) return "";
                return course.getCourseCode() + " - " + course.getCourseName();
            }

            @Override
            public Course fromString(String string) {
                return null;
            }
        });

        // Update course info label when selection changes
        courseComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateCourseInfo(newVal);
            } else {
                courseInfoLabel.setText("");
            }
        });

        log.info("Loaded {} courses", allCourses.size());
    }

    /**
     * Update course info label.
     */
    private void updateCourseInfo(Course course) {
        int currentEnrollment = getCurrentEnrollmentCount(course);
        String info = String.format("Enrolled: %d/%d | %s | Type: %s",
                currentEnrollment,
                course.getMaxStudents(),
                course.getSubject(),
                course.getCourseType());
        courseInfoLabel.setText(info);
    }

    /**
     * Get current enrollment count for a course.
     */
    private int getCurrentEnrollmentCount(Course course) {
        // This is a simplified version - in real implementation would query database
        return 0;
    }

    /**
     * Handle Clear Filters button.
     */
    @FXML
    private void handleClearFilters() {
        log.info("Clearing filters");

        gradeLevelComboBox.setValue("All");
        iepComboBox.setValue("Any");
        plan504ComboBox.setValue("Any");
        graduatedComboBox.setValue("Active Only");
        minCoursesField.clear();
        maxCoursesField.clear();
        nameSearchField.clear();
        studentIdField.clear();

        // Clear results
        studentSelections.clear();
        resultsCountLabel.setText("0 students found");
    }

    /**
     * Handle Search Students button.
     */
    @FXML
    private void handleSearchStudents() {
        log.info("Searching students with filters");

        try {
            // Build search criteria
            StudentSearchCriteria criteria = buildSearchCriteria();

            // Execute search
            List<Student> students = bulkEnrollmentService.searchStudents(criteria);

            // Convert to StudentSelection objects
            studentSelections.clear();
            for (Student student : students) {
                studentSelections.add(new StudentSelection(student));
            }

            // Update count
            resultsCountLabel.setText(students.size() + " students found");

            log.info("Search complete: {} students found", students.size());

            if (students.isEmpty()) {
                showInfo("No Results", "No students match the selected criteria.");
            }

        } catch (Exception e) {
            log.error("Search failed", e);
            showError("Search Error", "Failed to search students: " + e.getMessage());
        }
    }

    /**
     * Build search criteria from UI filters.
     */
    private StudentSearchCriteria buildSearchCriteria() {
        StudentSearchCriteria criteria = new StudentSearchCriteria();

        // Grade level
        String gradeLevel = gradeLevelComboBox.getValue();
        if (gradeLevel != null && !"All".equals(gradeLevel)) {
            criteria.setGradeLevel(Integer.parseInt(gradeLevel));
        }

        // IEP
        String iep = iepComboBox.getValue();
        if ("Has IEP".equals(iep)) {
            criteria.setHasIEP(true);
        } else if ("No IEP".equals(iep)) {
            criteria.setHasIEP(false);
        }

        // 504 Plan
        String plan504 = plan504ComboBox.getValue();
        if ("Has 504".equals(plan504)) {
            criteria.setHas504(true);
        } else if ("No 504".equals(plan504)) {
            criteria.setHas504(false);
        }

        // Graduated
        String graduated = graduatedComboBox.getValue();
        if ("Active Only".equals(graduated)) {
            criteria.setGraduated(false);
        } else if ("Graduated".equals(graduated)) {
            criteria.setGraduated(true);
        }

        // Course count
        if (!minCoursesField.getText().trim().isEmpty()) {
            try {
                criteria.setMinCoursesEnrolled(Integer.parseInt(minCoursesField.getText().trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid min courses: {}", minCoursesField.getText());
            }
        }

        if (!maxCoursesField.getText().trim().isEmpty()) {
            try {
                criteria.setMaxCoursesEnrolled(Integer.parseInt(maxCoursesField.getText().trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid max courses: {}", maxCoursesField.getText());
            }
        }

        // Name search
        if (!nameSearchField.getText().trim().isEmpty()) {
            criteria.setNamePattern(nameSearchField.getText().trim());
        }

        // Student ID
        if (!studentIdField.getText().trim().isEmpty()) {
            criteria.setStudentIdPattern(studentIdField.getText().trim());
        }

        return criteria;
    }

    /**
     * Handle Select All button.
     */
    @FXML
    private void handleSelectAll() {
        log.info("Selecting all students");
        for (StudentSelection selection : studentSelections) {
            selection.setSelected(true);
        }
        studentsTable.refresh();
    }

    /**
     * Handle Deselect All button.
     */
    @FXML
    private void handleDeselectAll() {
        log.info("Deselecting all students");
        for (StudentSelection selection : studentSelections) {
            selection.setSelected(false);
        }
        studentsTable.refresh();
    }

    /**
     * Handle Enroll Selected button.
     */
    @FXML
    private void handleEnrollSelected() {
        log.info("Enrolling selected students");

        // Get selected course
        Course course = courseComboBox.getValue();
        if (course == null) {
            showError("No Course Selected", "Please select a course to enroll students in.");
            return;
        }

        // Get selected students
        List<Student> selectedStudents = getSelectedStudents();
        if (selectedStudents.isEmpty()) {
            showError("No Students Selected", "Please select at least one student.");
            return;
        }

        // Confirm
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Enrollment");
        confirm.setHeaderText("Enroll Students in Course?");
        confirm.setContentText(String.format(
                "Enroll %d students in:\n%s - %s",
                selectedStudents.size(),
                course.getCourseCode(),
                course.getCourseName()));

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) {
            return;
        }

        // Execute enrollment
        try {
            int enrolled = bulkEnrollmentService.bulkEnroll(selectedStudents, course);

            showInfo("Enrollment Complete",
                    String.format("Successfully enrolled %d out of %d students in %s.",
                            enrolled, selectedStudents.size(), course.getCourseCode()));

            // Refresh search
            handleSearchStudents();

        } catch (Exception e) {
            log.error("Enrollment failed", e);
            showError("Enrollment Error", "Failed to enroll students: " + e.getMessage());
        }
    }

    /**
     * Handle Unenroll Selected button.
     */
    @FXML
    private void handleUnenrollSelected() {
        log.info("Unenrolling selected students");

        // Get selected course
        Course course = courseComboBox.getValue();
        if (course == null) {
            showError("No Course Selected", "Please select a course to unenroll students from.");
            return;
        }

        // Get selected students
        List<Student> selectedStudents = getSelectedStudents();
        if (selectedStudents.isEmpty()) {
            showError("No Students Selected", "Please select at least one student.");
            return;
        }

        // Confirm
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Unenrollment");
        confirm.setHeaderText("Unenroll Students from Course?");
        confirm.setContentText(String.format(
                "Unenroll %d students from:\n%s - %s",
                selectedStudents.size(),
                course.getCourseCode(),
                course.getCourseName()));

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) {
            return;
        }

        // Execute unenrollment
        try {
            int unenrolled = bulkEnrollmentService.bulkUnenroll(selectedStudents, course);

            showInfo("Unenrollment Complete",
                    String.format("Successfully unenrolled %d students from %s.",
                            unenrolled, course.getCourseCode()));

            // Refresh search
            handleSearchStudents();

        } catch (Exception e) {
            log.error("Unenrollment failed", e);
            showError("Unenrollment Error", "Failed to unenroll students: " + e.getMessage());
        }
    }

    /**
     * Handle Auto-Enroll button.
     */
    @FXML
    private void handleAutoEnroll() {
        log.info("Auto-enrolling selected students");

        // Get selected students
        List<Student> selectedStudents = getSelectedStudents();
        if (selectedStudents.isEmpty()) {
            showError("No Students Selected", "Please select at least one student.");
            return;
        }

        boolean applyRequired = applyRequiredCheckbox.isSelected();
        boolean applyRecommended = applyRecommendedCheckbox.isSelected();

        if (!applyRequired && !applyRecommended) {
            showError("No Options Selected", "Please select at least one enrollment option (Required or Recommended).");
            return;
        }

        // Show preview
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Auto-Enrollment");
        confirm.setHeaderText("Auto-Enroll Students?");
        confirm.setContentText(String.format(
                "Auto-enroll %d students using intelligent course suggestions.\n\n" +
                        "Options:\n" +
                        "• Required Courses: %s\n" +
                        "• Recommended Courses: %s\n\n" +
                        "This will analyze each student's grade level and course history.",
                selectedStudents.size(),
                applyRequired ? "Yes" : "No",
                applyRecommended ? "Yes" : "No"));

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) {
            return;
        }

        // Execute auto-enrollment
        try {
            int totalEnrolled = bulkEnrollmentService.autoEnrollStudents(
                    selectedStudents,
                    applyRequired,
                    applyRecommended);

            showInfo("Auto-Enrollment Complete",
                    String.format("Successfully created %d course enrollments across %d students.",
                            totalEnrolled, selectedStudents.size()));

            // Refresh search
            handleSearchStudents();

        } catch (Exception e) {
            log.error("Auto-enrollment failed", e);
            showError("Auto-Enrollment Error", "Failed to auto-enroll students: " + e.getMessage());
        }
    }

    /**
     * Get list of selected students.
     */
    private List<Student> getSelectedStudents() {
        return studentSelections.stream()
                .filter(StudentSelection::isSelected)
                .map(StudentSelection::getStudent)
                .collect(Collectors.toList());
    }

    /**
     * Show info alert.
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
     * Show error alert.
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
}
