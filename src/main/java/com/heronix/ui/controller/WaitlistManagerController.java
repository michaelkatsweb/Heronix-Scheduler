package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseEnrollmentRequest;
import com.heronix.model.domain.Student;
import com.heronix.repository.CourseEnrollmentRequestRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.service.IntelligentCourseAssignmentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Waitlist Manager Controller
 *
 * Allows administrators to view and manage course waitlists.
 * Provides tools to promote students from waitlist when seats become available.
 *
 * Features:
 * - View all courses with waitlists
 * - View waitlist details for specific course
 * - Promote students from waitlist
 * - Remove students from waitlist
 * - Bulk promotion operations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 6 - November 20, 2025
 */
@Component
public class WaitlistManagerController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRequestRepository enrollmentRequestRepository;

    @Autowired
    private IntelligentCourseAssignmentService assignmentService;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    // Left Panel: Course List
    @FXML
    private TableView<CourseWaitlistSummary> courseTable;

    @FXML
    private TableColumn<CourseWaitlistSummary, String> courseCodeColumn;

    @FXML
    private TableColumn<CourseWaitlistSummary, String> courseNameColumn;

    @FXML
    private TableColumn<CourseWaitlistSummary, Integer> waitlistSizeColumn;

    @FXML
    private TableColumn<CourseWaitlistSummary, Integer> availableSeatsColumn;

    @FXML
    private TableColumn<CourseWaitlistSummary, String> statusColumn;

    // Right Panel: Waitlist Details
    @FXML
    private Label selectedCourseLabel;

    @FXML
    private Label enrollmentLabel;

    @FXML
    private Label availableSeatsLabel;

    @FXML
    private TableView<CourseEnrollmentRequest> waitlistTable;

    @FXML
    private TableColumn<CourseEnrollmentRequest, Integer> positionColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> studentNameColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> studentIdColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, Integer> priorityColumn;

    @FXML
    private TableColumn<CourseEnrollmentRequest, String> preferenceColumn;

    // Buttons
    @FXML
    private Button promoteOneButton;

    @FXML
    private Button promoteAllButton;

    @FXML
    private Button removeFromWaitlistButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button closeButton;

    // ========================================================================
    // DATA
    // ========================================================================

    private ObservableList<CourseWaitlistSummary> courseSummaries;
    private ObservableList<CourseEnrollmentRequest> waitlistItems;
    private CourseWaitlistSummary selectedCourseSummary;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        setupCoursesTable();
        setupWaitlistTable();
        loadCourses();
        setupListeners();
    }

    /**
     * Setup courses table columns
     */
    private void setupCoursesTable() {
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        waitlistSizeColumn.setCellValueFactory(new PropertyValueFactory<>("waitlistSize"));
        availableSeatsColumn.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    /**
     * Setup waitlist table columns
     */
    private void setupWaitlistTable() {
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("waitlistPosition"));

        studentNameColumn.setCellValueFactory(cellData -> {
            Student student = cellData.getValue().getStudent();
            return new SimpleStringProperty(
                student != null ? student.getFullName() : "Unknown"
            );
        });

        studentIdColumn.setCellValueFactory(cellData -> {
            Student student = cellData.getValue().getStudent();
            return new SimpleStringProperty(
                student != null ? student.getStudentId() : "Unknown"
            );
        });

        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priorityScore"));

        preferenceColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPreferenceRankLabel())
        );
    }

    /**
     * Load all courses with waitlists
     */
    private void loadCourses() {
        List<Course> allCourses = courseRepository.findByActiveTrue();
        courseSummaries = FXCollections.observableArrayList();

        for (Course course : allCourses) {
            long waitlistCount = enrollmentRequestRepository.countByCourseAndIsWaitlistTrue(course);

            if (waitlistCount > 0) {
                CourseWaitlistSummary summary = new CourseWaitlistSummary();
                summary.setCourse(course);
                summary.setCourseCode(course.getCourseCode());
                summary.setCourseName(course.getCourseName());
                summary.setWaitlistSize((int) waitlistCount);
                summary.setAvailableSeats(course.getAvailableSeats());
                summary.setCurrentEnrollment(course.getCurrentEnrollment());
                summary.setMaxCapacity(course.getMaxStudents());

                // Determine status
                if (course.hasAvailableSeats()) {
                    summary.setStatus("✅ Seats Available");
                } else {
                    summary.setStatus("❌ Full");
                }

                courseSummaries.add(summary);
            }
        }

        courseTable.setItems(courseSummaries);
    }

    /**
     * Setup event listeners
     */
    private void setupListeners() {
        // When course selected, load its waitlist
        courseTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadWaitlistForCourse(newSelection);
                }
            }
        );

        // Enable/disable buttons based on selections
        waitlistTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                boolean hasSelection = newSelection != null;
                promoteOneButton.setDisable(!hasSelection || !canPromote());
                removeFromWaitlistButton.setDisable(!hasSelection);
            }
        );

        promoteAllButton.disableProperty().bind(
            courseTable.getSelectionModel().selectedItemProperty().isNull()
        );
    }

    /**
     * Load waitlist for selected course
     */
    private void loadWaitlistForCourse(CourseWaitlistSummary summary) {
        selectedCourseSummary = summary;
        Course course = summary.getCourse();

        // Update info labels
        selectedCourseLabel.setText(course.getCourseCode() + " - " + course.getCourseName());
        enrollmentLabel.setText(String.format("Enrollment: %d / %d",
            course.getCurrentEnrollment(), course.getMaxStudents()));
        availableSeatsLabel.setText("Available Seats: " + course.getAvailableSeats());

        // Load waitlist
        List<CourseEnrollmentRequest> waitlist = enrollmentRequestRepository
            .findByCourseAndIsWaitlistTrueOrderByWaitlistPositionAsc(course);

        waitlistItems = FXCollections.observableArrayList(waitlist);
        waitlistTable.setItems(waitlistItems);

        // Enable/disable promote all button
        promoteAllButton.setDisable(!canPromote());
    }

    /**
     * Check if promotion is possible
     */
    private boolean canPromote() {
        if (selectedCourseSummary == null) return false;
        return selectedCourseSummary.getCourse().hasAvailableSeats();
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    /**
     * Handle Promote One button
     */
    @FXML
    private void handlePromoteOne() {
        CourseEnrollmentRequest selected = waitlistTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Course course = selectedCourseSummary.getCourse();

        if (!course.hasAvailableSeats()) {
            showError("No available seats in course");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Promotion");
        confirm.setHeaderText("Promote from Waitlist");
        confirm.setContentText(String.format(
            "Promote %s to %s?",
            selected.getStudent().getFullName(),
            course.getCourseCode()
        ));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Promote student
                assignmentService.promoteFromWaitlist(course.getId(), 1);

                // Refresh displays
                loadCourses();
                loadWaitlistForCourse(selectedCourseSummary);

                showInfo("Student promoted successfully");
            } catch (Exception e) {
                showError("Failed to promote student: " + e.getMessage());
            }
        }
    }

    /**
     * Handle Promote All button
     */
    @FXML
    private void handlePromoteAll() {
        if (selectedCourseSummary == null) return;

        Course course = selectedCourseSummary.getCourse();
        int availableSeats = course.getAvailableSeats();

        if (availableSeats <= 0) {
            showError("No available seats in course");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Bulk Promotion");
        confirm.setHeaderText("Promote Multiple Students");
        confirm.setContentText(String.format(
            "Promote up to %d students from waitlist for %s?",
            availableSeats,
            course.getCourseCode()
        ));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Promote available seats worth of students
                assignmentService.promoteFromWaitlist(course.getId(), availableSeats);

                // Refresh displays
                loadCourses();
                loadWaitlistForCourse(selectedCourseSummary);

                showInfo(String.format("Promoted %d students successfully", availableSeats));
            } catch (Exception e) {
                showError("Failed to promote students: " + e.getMessage());
            }
        }
    }

    /**
     * Handle Remove from Waitlist button
     */
    @FXML
    private void handleRemoveFromWaitlist() {
        CourseEnrollmentRequest selected = waitlistTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove from Waitlist");
        confirm.setContentText(String.format(
            "Remove %s from waitlist for %s?",
            selected.getStudent().getFullName(),
            selectedCourseSummary.getCourse().getCourseCode()
        ));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Remove from waitlist (cancel request)
                selected.setRequestStatus(com.heronix.model.enums.EnrollmentRequestStatus.CANCELLED);
                selected.setStatusReason("Removed from waitlist by administrator");
                selected.setIsWaitlist(false);
                selected.setWaitlistPosition(null);
                enrollmentRequestRepository.save(selected);

                // Refresh displays
                loadCourses();
                loadWaitlistForCourse(selectedCourseSummary);

                showInfo("Student removed from waitlist");
            } catch (Exception e) {
                showError("Failed to remove student: " + e.getMessage());
            }
        }
    }

    /**
     * Handle Refresh button
     */
    @FXML
    private void handleRefresh() {
        loadCourses();
        if (selectedCourseSummary != null) {
            // Find updated summary for same course
            Optional<CourseWaitlistSummary> updated = courseSummaries.stream()
                .filter(s -> s.getCourse().getId().equals(selectedCourseSummary.getCourse().getId()))
                .findFirst();

            updated.ifPresent(this::loadWaitlistForCourse);
        }
        showInfo("Refreshed waitlist data");
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
        alert.setTitle("Information");
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

    // ========================================================================
    // INNER CLASS: CourseWaitlistSummary
    // ========================================================================

    public static class CourseWaitlistSummary {
        private Course course;
        private String courseCode;
        private String courseName;
        private int waitlistSize;
        private int availableSeats;
        private Integer currentEnrollment;
        private Integer maxCapacity;
        private String status;

        // Getters and Setters
        public Course getCourse() { return course; }
        public void setCourse(Course course) { this.course = course; }

        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public int getWaitlistSize() { return waitlistSize; }
        public void setWaitlistSize(int waitlistSize) { this.waitlistSize = waitlistSize; }

        public int getAvailableSeats() { return availableSeats; }
        public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

        public Integer getCurrentEnrollment() { return currentEnrollment; }
        public void setCurrentEnrollment(Integer currentEnrollment) { this.currentEnrollment = currentEnrollment; }

        public Integer getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
