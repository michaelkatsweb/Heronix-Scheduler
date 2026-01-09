package com.heronix.ui.controller;

import com.heronix.model.domain.AcademicYear;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseEnrollmentRequest;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.EnrollmentRequestStatus;
import com.heronix.repository.CourseEnrollmentRequestRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.service.AcademicYearService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Student Enrollment Request Controller
 *
 * Allows administrators to create enrollment requests for students.
 * Students can select up to 4 elective preferences which will be processed
 * by the intelligent assignment algorithm.
 *
 * Features:
 * - Student search and selection
 * - Course browsing with filters
 * - Drag-and-drop preference ordering
 * - Priority score preview
 * - Bulk request creation (4 requests per student)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7A - November 21, 2025
 */
@Slf4j
@Component
public class StudentEnrollmentRequestController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRequestRepository enrollmentRequestRepository;

    @Autowired
    private AcademicYearService academicYearService;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    // Left Panel: Student Selection
    @FXML
    private TextField studentSearchField;

    @FXML
    private ListView<Student> studentListView;

    @FXML
    private Label selectedStudentLabel;

    @FXML
    private Label studentGPALabel;

    @FXML
    private Label studentGradeLabel;

    @FXML
    private Label basePriorityLabel;

    // Middle Panel: Available Courses
    @FXML
    private TextField courseSearchField;

    @FXML
    private ComboBox<String> subjectFilterCombo;

    @FXML
    private CheckBox showFullCoursesCheck;

    @FXML
    private TableView<Course> availableCoursesTable;

    @FXML
    private TableColumn<Course, String> courseCodeColumn;

    @FXML
    private TableColumn<Course, String> courseNameColumn;

    @FXML
    private TableColumn<Course, String> subjectColumn;

    @FXML
    private TableColumn<Course, Integer> enrollmentColumn;

    @FXML
    private TableColumn<Course, String> gpaReqColumn;

    // Right Panel: Preferences
    @FXML
    private ListView<Course> preference1List;

    @FXML
    private ListView<Course> preference2List;

    @FXML
    private ListView<Course> preference3List;

    @FXML
    private ListView<Course> preference4List;

    @FXML
    private Label priority1Label;

    @FXML
    private Label priority2Label;

    @FXML
    private Label priority3Label;

    @FXML
    private Label priority4Label;

    // Buttons
    @FXML
    private Button createRequestsButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button closeButton;

    // ========================================================================
    // DATA
    // ========================================================================

    private Student selectedStudent;
    private ObservableList<Student> students;
    private ObservableList<Course> availableCourses;
    private String currentAcademicYear;

    /**
     * Get the current academic year from the service
     */
    private String getCurrentAcademicYear() {
        if (currentAcademicYear == null) {
            AcademicYear activeYear = academicYearService.getActiveYear();
            currentAcademicYear = activeYear != null ? activeYear.getYearName() : "2024-2025";
        }
        return currentAcademicYear;
    }

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        setupStudentList();
        setupCourseTable();
        setupPreferenceLists();
        setupFilters();
        loadData();
    }

    /**
     * Setup student list view
     */
    private void setupStudentList() {
        studentListView.setCellFactory(lv -> new ListCell<Student>() {
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);
                if (empty || student == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (%s) - Grade %d - GPA: %.2f",
                        student.getFullName(),
                        student.getStudentId(),
                        student.getGradeLevel() != null ? student.getGradeLevel() : 0,
                        student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0));
                }
            }
        });

        studentListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectStudent(newSelection);
                }
            }
        );
    }

    /**
     * Setup course table
     */
    private void setupCourseTable() {
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));

        enrollmentColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue();
            String enrollment = String.format("%d/%d",
                course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0,
                course.getMaxStudents() != null ? course.getMaxStudents() : 0);
            return new javafx.beans.property.SimpleObjectProperty<>(
                course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0);
        });

        gpaReqColumn.setCellValueFactory(cellData -> {
            // GPA requirement field to be added in future phase
            return new SimpleStringProperty("N/A");
        });

        // Enable drag from course table
        availableCoursesTable.setRowFactory(tv -> {
            TableRow<Course> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(row.getItem().getId().toString());
                    db.setContent(content);
                    event.consume();
                }
            });
            return row;
        });
    }

    /**
     * Setup preference list views with drag-and-drop
     */
    private void setupPreferenceLists() {
        setupPreferenceList(preference1List, 1);
        setupPreferenceList(preference2List, 2);
        setupPreferenceList(preference3List, 3);
        setupPreferenceList(preference4List, 4);
    }

    /**
     * Setup individual preference list
     */
    private void setupPreferenceList(ListView<Course> listView, int preferenceRank) {
        listView.setCellFactory(lv -> new ListCell<Course>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("%s - %s", course.getCourseCode(), course.getCourseName()));
                }
            }
        });

        // Accept drops
        listView.setOnDragOver(event -> {
            if (event.getGestureSource() != listView && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        listView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    Long courseId = Long.parseLong(db.getString());
                    Course course = courseRepository.findById(courseId).orElse(null);
                    if (course != null) {
                        // Check if already in this preference
                        if (listView.getItems().isEmpty()) {
                            listView.getItems().add(course);
                            updatePriorityPreview(preferenceRank, course);
                            success = true;
                        } else {
                            showWarning("Only one course per preference slot");
                        }
                    }
                } catch (NumberFormatException e) {
                    log.error("Invalid course ID in drag data", e);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Setup search and filter controls
     */
    private void setupFilters() {
        // Student search
        studentSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterStudents(newVal));

        // Course search
        courseSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterCourses());

        // Subject filter
        subjectFilterCombo.getItems().addAll("All Subjects", "Art", "Music", "Drama", "Physical Education", "Technology", "Foreign Language");
        subjectFilterCombo.getSelectionModel().select(0);
        subjectFilterCombo.setOnAction(e -> filterCourses());

        // Show full courses checkbox
        showFullCoursesCheck.setOnAction(e -> filterCourses());
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load initial data
     */
    private void loadData() {
        loadStudents();
        loadCourses();
    }

    /**
     * Load all students
     */
    private void loadStudents() {
        List<Student> allStudents = studentRepository.findByActiveTrue();
        students = FXCollections.observableArrayList(allStudents);
        studentListView.setItems(students);
    }

    /**
     * Load available courses (electives only)
     */
    private void loadCourses() {
        // Load only active elective courses
        List<Course> courses = courseRepository.findByActiveTrue();

        // Filter to electives (assuming electives have isElective = true or certain subjects)
        availableCourses = FXCollections.observableArrayList(
            courses.stream()
                .filter(c -> isElectiveCourse(c))
                .toList()
        );

        availableCoursesTable.setItems(availableCourses);
    }

    /**
     * Check if course is an elective
     */
    private boolean isElectiveCourse(Course course) {
        // Simple heuristic: if subject is in typical elective subjects
        String subject = course.getSubject();
        if (subject == null) return false;

        return subject.equalsIgnoreCase("Art") ||
               subject.equalsIgnoreCase("Music") ||
               subject.equalsIgnoreCase("Drama") ||
               subject.equalsIgnoreCase("Physical Education") ||
               subject.equalsIgnoreCase("Technology") ||
               subject.equalsIgnoreCase("Foreign Language") ||
               subject.contains("Elective");
    }

    // ========================================================================
    // FILTERING
    // ========================================================================

    /**
     * Filter students by search text
     */
    private void filterStudents(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            studentListView.setItems(students);
            return;
        }

        String search = searchText.toLowerCase();
        ObservableList<Student> filtered = students.filtered(student ->
            student.getFullName().toLowerCase().contains(search) ||
            student.getStudentId().toLowerCase().contains(search)
        );
        studentListView.setItems(filtered);
    }

    /**
     * Filter courses by search text, subject, and availability
     */
    private void filterCourses() {
        String searchText = courseSearchField.getText();
        String subject = subjectFilterCombo.getSelectionModel().getSelectedItem();
        boolean showFull = showFullCoursesCheck.isSelected();

        ObservableList<Course> filtered = availableCourses.filtered(course -> {
            // Search filter
            if (searchText != null && !searchText.trim().isEmpty()) {
                String search = searchText.toLowerCase();
                boolean matchesSearch = course.getCourseCode().toLowerCase().contains(search) ||
                                      course.getCourseName().toLowerCase().contains(search);
                if (!matchesSearch) return false;
            }

            // Subject filter
            if (subject != null && !subject.equals("All Subjects")) {
                if (course.getSubject() == null || !course.getSubject().equalsIgnoreCase(subject)) {
                    return false;
                }
            }

            // Availability filter
            if (!showFull) {
                if (!course.hasAvailableSeats()) {
                    return false;
                }
            }

            return true;
        });

        availableCoursesTable.setItems(filtered);
    }

    // ========================================================================
    // STUDENT SELECTION
    // ========================================================================

    /**
     * Select a student and display their info
     */
    private void selectStudent(Student student) {
        selectedStudent = student;

        selectedStudentLabel.setText(student.getFullName() + " (" + student.getStudentId() + ")");
        studentGPALabel.setText("GPA: " + (student.getCurrentGPA() != null ? String.format("%.2f", student.getCurrentGPA()) : "N/A"));
        studentGradeLabel.setText("Grade: " + (student.getGradeLevel() != null ? student.getGradeLevel() : "N/A"));

        // Calculate and display base priority
        int basePriority = student.calculatePriorityScore();
        basePriorityLabel.setText("Base Priority: " + basePriority + " points");

        // Enable request creation if preferences are set
        updateCreateButton();
    }

    // ========================================================================
    // PRIORITY PREVIEW
    // ========================================================================

    /**
     * Update priority preview for a preference
     */
    private void updatePriorityPreview(int preferenceRank, Course course) {
        if (selectedStudent == null) return;

        int basePriority = selectedStudent.calculatePriorityScore();

        // Add preference bonus
        int preferenceBonus = switch (preferenceRank) {
            case 1 -> 100;
            case 2 -> 75;
            case 3 -> 50;
            case 4 -> 25;
            default -> 0;
        };

        int totalPriority = basePriority + preferenceBonus;

        // Check GPA requirement
        boolean meetsGPA = true;
        String warning = "";
        if (course != null && course.getMinGPARequired() != null && course.getMinGPARequired() > 0) {
            Double studentGPA = selectedStudent.getCurrentGPA();
            if (studentGPA == null || studentGPA < course.getMinGPARequired()) {
                meetsGPA = false;
                warning = String.format(" ⚠ Requires %.2f GPA", course.getMinGPARequired());
            }
        }

        Label priorityLabel = switch (preferenceRank) {
            case 1 -> priority1Label;
            case 2 -> priority2Label;
            case 3 -> priority3Label;
            case 4 -> priority4Label;
            default -> null;
        };

        if (priorityLabel != null) {
            priorityLabel.setText(String.format("Priority: %d pts%s", totalPriority, warning));
            priorityLabel.setStyle(meetsGPA ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        }

        updateCreateButton();
    }

    // ========================================================================
    // REQUEST CREATION
    // ========================================================================

    /**
     * Update create button enabled state
     */
    private void updateCreateButton() {
        boolean hasStudent = selectedStudent != null;
        boolean hasAtLeastOne = !preference1List.getItems().isEmpty() ||
                               !preference2List.getItems().isEmpty() ||
                               !preference3List.getItems().isEmpty() ||
                               !preference4List.getItems().isEmpty();

        createRequestsButton.setDisable(!hasStudent || !hasAtLeastOne);
    }

    /**
     * Handle Create Requests button
     */
    @FXML
    private void handleCreateRequests() {
        if (selectedStudent == null) {
            showError("Please select a student");
            return;
        }

        // Get preferences
        Course pref1 = preference1List.getItems().isEmpty() ? null : preference1List.getItems().get(0);
        Course pref2 = preference2List.getItems().isEmpty() ? null : preference2List.getItems().get(0);
        Course pref3 = preference3List.getItems().isEmpty() ? null : preference3List.getItems().get(0);
        Course pref4 = preference4List.getItems().isEmpty() ? null : preference4List.getItems().get(0);

        if (pref1 == null && pref2 == null && pref3 == null && pref4 == null) {
            showError("Please select at least one course preference");
            return;
        }

        // Confirm
        int prefCount = (pref1 != null ? 1 : 0) + (pref2 != null ? 1 : 0) + (pref3 != null ? 1 : 0) + (pref4 != null ? 1 : 0);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Request Creation");
        confirm.setHeaderText("Create Enrollment Requests");
        confirm.setContentText(String.format(
            "Create %d enrollment request(s) for %s?\n\n" +
            "1st Choice: %s\n" +
            "2nd Choice: %s\n" +
            "3rd Choice: %s\n" +
            "4th Choice: %s",
            prefCount,
            selectedStudent.getFullName(),
            pref1 != null ? pref1.getCourseCode() : "None",
            pref2 != null ? pref2.getCourseCode() : "None",
            pref3 != null ? pref3.getCourseCode() : "None",
            pref4 != null ? pref4.getCourseCode() : "None"
        ));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                List<CourseEnrollmentRequest> requests = createEnrollmentRequests(pref1, pref2, pref3, pref4);
                enrollmentRequestRepository.saveAll(requests);

                showInfo(String.format("Successfully created %d enrollment request(s) for %s",
                    requests.size(), selectedStudent.getFullName()));

                // Clear preferences
                handleClear();

            } catch (Exception e) {
                log.error("Error creating enrollment requests", e);
                showError("Failed to create requests: " + e.getMessage());
            }
        }
    }

    /**
     * Create enrollment request objects
     */
    private List<CourseEnrollmentRequest> createEnrollmentRequests(Course pref1, Course pref2, Course pref3, Course pref4) {
        List<CourseEnrollmentRequest> requests = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int basePriority = selectedStudent.calculatePriorityScore();

        if (pref1 != null) {
            requests.add(createRequest(pref1, 1, basePriority + 100, now));
        }
        if (pref2 != null) {
            requests.add(createRequest(pref2, 2, basePriority + 75, now));
        }
        if (pref3 != null) {
            requests.add(createRequest(pref3, 3, basePriority + 50, now));
        }
        if (pref4 != null) {
            requests.add(createRequest(pref4, 4, basePriority + 25, now));
        }

        return requests;
    }

    /**
     * Create individual enrollment request
     */
    private CourseEnrollmentRequest createRequest(Course course, int preferenceRank, int priorityScore, LocalDateTime requestedDate) {
        CourseEnrollmentRequest request = new CourseEnrollmentRequest();
        request.setStudent(selectedStudent);
        request.setCourse(course);
        // Set academic year from the active academic year
        AcademicYear activeYear = academicYearService.getActiveYear();
        if (activeYear != null) {
            request.setAcademicYearId(activeYear.getId());
        }
        request.setPreferenceRank(preferenceRank);
        request.setPriorityScore(priorityScore);
        request.setCreatedAt(requestedDate);
        request.setRequestStatus(EnrollmentRequestStatus.PENDING);
        request.setIsWaitlist(false);
        return request;
    }

    /**
     * Handle Clear button
     */
    @FXML
    private void handleClear() {
        preference1List.getItems().clear();
        preference2List.getItems().clear();
        preference3List.getItems().clear();
        preference4List.getItems().clear();

        priority1Label.setText("Priority: -");
        priority2Label.setText("Priority: -");
        priority3Label.setText("Priority: -");
        priority4Label.setText("Priority: -");

        updateCreateButton();
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
    // UTILITY METHODS
    // ========================================================================

    /**
     * Show info message
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning message
     */
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
