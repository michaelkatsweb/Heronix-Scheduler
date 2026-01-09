package com.heronix.ui.controller;

import com.heronix.model.dto.CleanupResult;
import com.heronix.model.dto.DatabaseStats;
import com.heronix.service.DataManagementService;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.util.Optional;

/**
 * Data Management Controller - FIXED
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/DataManagementController.java
 * 
 * Manages deletion of students, teachers, courses, rooms
 * (Your existing DatabaseCleanupController handles schedules)
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DataManagementController {

    private final DataManagementService dataManagementService;

    @FXML
    private Label statsLabel;
    @FXML
    private TextArea resultsTextArea;
    @FXML
    private ComboBox<String> gradeLevelComboBox;
    @FXML
    private ComboBox<String> departmentComboBox;

    @FXML
    public void initialize() {
        log.info("ðŸ“Š DataManagementController initialized");
        gradeLevelComboBox.getItems().addAll("9", "10", "11", "12", "K");
        departmentComboBox.getItems().addAll("Math", "Science", "English", "General");
        refreshStats();
    }

    @FXML
    private void refreshStats() {
        DatabaseStats stats = dataManagementService.getDatabaseStats();
        statsLabel.setText(String.format(
                "Students: %,d | Teachers: %,d | Courses: %,d | Rooms: %,d | Total: %,d",
                stats.getStudentCount(), stats.getTeacherCount(),
                stats.getCourseCount(), stats.getRoomCount(), stats.getTotalRecords()));
    }

    @FXML
    private void deleteAllStudents() {
        if (confirmDeletion("all students")) {
            CleanupResult result = dataManagementService.deleteAllStudents();
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteAllTeachers() {
        if (confirmDeletion("all teachers")) {
            CleanupResult result = dataManagementService.deleteAllTeachers();
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteAllCourses() {
        if (confirmDeletion("all courses")) {
            CleanupResult result = dataManagementService.deleteAllCourses();
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteAllRooms() {
        if (confirmDeletion("all rooms")) {
            CleanupResult result = dataManagementService.deleteAllRooms();
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteAllSchedules() {
        // Note: This should call your existing DatabaseCleanupService
        showError("Use Tools â†’ Database Cleanup for schedule management");
    }

    @FXML
    private void deleteStudentsByGrade() {
        String grade = gradeLevelComboBox.getValue();
        if (grade == null) {
            showError("Please select a grade level");
            return;
        }
        if (confirmDeletion("all grade " + grade + " students")) {
            CleanupResult result = dataManagementService.deleteStudentsByGradeLevel(grade);
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteTeachersByDepartment() {
        String dept = departmentComboBox.getValue();
        if (dept == null) {
            showError("Please select a department");
            return;
        }
        if (confirmDeletion("all " + dept + " teachers")) {
            CleanupResult result = dataManagementService.deleteTeachersByDepartment(dept);
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteInactiveStudents() {
        if (confirmDeletion("all inactive students")) {
            CleanupResult result = dataManagementService.deleteInactiveStudents();
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteInactiveTeachers() {
        if (confirmDeletion("all inactive teachers")) {
            CleanupResult result = dataManagementService.deleteInactiveTeachers();
            displayResult(result);
            refreshStats();
        }
    }

    @FXML
    private void deleteAllData() {
        showError("Nuclear option removed for safety. Delete each entity type separately.");
    }

    private boolean confirmDeletion(String what) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete " + what + "?");
        alert.setContentText("This action cannot be undone!");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void displayResult(CleanupResult result) {
        resultsTextArea.setText(result.getSummary());

        if (result.isSuccess()) {
            showSuccess("Deleted " + result.getDeletedCount() + " records");
        } else {
            showError("Deletion failed: " + result.getErrorMessage());
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
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