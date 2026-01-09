package com.heronix.ui.controller;

import com.heronix.model.domain.AcademicYear;
import com.heronix.model.dto.ProgressionPreview;
import com.heronix.model.dto.ProgressionResult;
import com.heronix.repository.StudentRepository;
import com.heronix.service.AcademicYearService;
import com.heronix.service.GradeProgressionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Controller for Grade Progression dialog.
 *
 * Handles the workflow for progressing to the next academic year:
 * 1. Shows preview of what will happen
 * 2. Collects new year configuration
 * 3. Executes progression with progress feedback
 * 4. Shows results
 *
 * @author Heronix Scheduler Team
 */
@Controller
public class GradeProgressionController {

    private static final Logger log = LoggerFactory.getLogger(GradeProgressionController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Autowired
    private GradeProgressionService gradeProgressionService;

    @Autowired
    private AcademicYearService academicYearService;

    @Autowired
    private StudentRepository studentRepository;

    private AcademicYear currentYear;

    // Current Year Info
    @FXML
    private Label currentYearLabel;

    @FXML
    private Label currentDatesLabel;

    // Preview Statistics
    @FXML
    private Label grade9CountLabel;

    @FXML
    private Label grade10CountLabel;

    @FXML
    private Label grade11CountLabel;

    @FXML
    private Label grade12CountLabel;

    @FXML
    private Label totalEnrollmentsLabel;

    // New Year Configuration
    @FXML
    private TextField newYearNameField;

    @FXML
    private DatePicker newStartDatePicker;

    @FXML
    private DatePicker newEndDatePicker;

    @FXML
    private DatePicker newGraduationDatePicker;

    // Progress
    @FXML
    private VBox progressBox;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    // Buttons
    @FXML
    private Button cancelButton;

    @FXML
    private Button startButton;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        log.info("Initializing Grade Progression Controller");

        // Set default dates for new year (start next September)
        LocalDate now = LocalDate.now();
        int nextYearStart = now.getMonthValue() >= 9 ? now.getYear() + 1 : now.getYear();
        int nextYearEnd = nextYearStart + 1;

        newYearNameField.setText(nextYearStart + "-" + nextYearEnd);
        newStartDatePicker.setValue(LocalDate.of(nextYearStart, 9, 1));
        newEndDatePicker.setValue(LocalDate.of(nextYearEnd, 6, 30));
        newGraduationDatePicker.setValue(LocalDate.of(nextYearEnd, 6, 15));
    }

    /**
     * Set the current academic year (called by parent controller).
     */
    public void setCurrentYear(AcademicYear year) {
        this.currentYear = year;
        loadCurrentYearInfo();
        loadPreviewStatistics();
    }

    /**
     * Load current year information.
     */
    private void loadCurrentYearInfo() {
        if (currentYear == null) return;

        currentYearLabel.setText(currentYear.getYearName());
        currentDatesLabel.setText(
                currentYear.getStartDate().format(DATE_FORMATTER) +
                        " to " +
                        currentYear.getEndDate().format(DATE_FORMATTER)
        );
    }

    /**
     * Load preview statistics.
     */
    private void loadPreviewStatistics() {
        if (currentYear == null) return;

        log.info("Loading preview statistics");

        ProgressionPreview preview = gradeProgressionService.previewProgression(currentYear);

        grade9CountLabel.setText(String.valueOf(preview.getGrade9Count()));
        grade10CountLabel.setText(String.valueOf(preview.getGrade10Count()));
        grade11CountLabel.setText(String.valueOf(preview.getGrade11Count()));
        grade12CountLabel.setText(String.valueOf(preview.getGrade12Count()));
        totalEnrollmentsLabel.setText(String.valueOf(preview.getTotalEnrollments()));

        log.info("Preview: Grade 9={}, 10={}, 11={}, 12={}, Enrollments={}",
                preview.getGrade9Count(),
                preview.getGrade10Count(),
                preview.getGrade11Count(),
                preview.getGrade12Count(),
                preview.getTotalEnrollments());
    }

    /**
     * Handle Cancel button.
     */
    @FXML
    private void handleCancel() {
        log.info("Grade progression cancelled");
        closeDialog();
    }

    /**
     * Handle Start Progression button.
     */
    @FXML
    private void handleStartProgression() {
        log.info("Starting grade progression");

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get new year configuration
        String newYearName = newYearNameField.getText().trim();
        LocalDate newStartDate = newStartDatePicker.getValue();
        LocalDate newEndDate = newEndDatePicker.getValue();
        LocalDate newGraduationDate = newGraduationDatePicker.getValue();

        // Confirm with user
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Progression");
        confirm.setHeaderText("Are you absolutely sure?");
        confirm.setContentText(
                "This will:\n" +
                        "• Graduate " + grade12CountLabel.getText() + " seniors\n" +
                        "• Promote " + (
                        Integer.parseInt(grade9CountLabel.getText()) +
                                Integer.parseInt(grade10CountLabel.getText()) +
                                Integer.parseInt(grade11CountLabel.getText())
                ) + " students\n" +
                        "• Archive " + totalEnrollmentsLabel.getText() + " enrollments\n" +
                        "• Clear all course assignments\n\n" +
                        "This operation cannot be undone!"
        );

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);

        if (result != ButtonType.OK) {
            log.info("Progression cancelled by user");
            return;
        }

        // Disable inputs and show progress
        disableInputs();
        showProgress();

        // Execute progression in background thread
        new Thread(() -> {
            try {
                log.info("Executing grade progression");

                // Create progress callback
                Consumer<String> progressCallback = message -> {
                    Platform.runLater(() -> {
                        progressLabel.setText(message);
                        log.info("Progress: {}", message);
                    });
                };

                // Execute progression
                ProgressionResult progressionResult = gradeProgressionService.progressToNextYear(
                        currentYear,
                        newYearName,
                        newStartDate,
                        newEndDate,
                        progressCallback
                );

                // Set graduation date if provided
                if (newGraduationDate != null && progressionResult.getNewAcademicYearId() != null) {
                    academicYearService.scheduleGraduation(
                            progressionResult.getNewAcademicYearId(),
                            newGraduationDate
                    );
                }

                // Show success
                Platform.runLater(() -> {
                    if (progressionResult.isSuccessful()) {
                        showSuccess(progressionResult);
                    } else {
                        showError("Progression Failed", progressionResult.getMessage());
                        enableInputs();
                        hideProgress();
                    }
                });

            } catch (Exception e) {
                log.error("Grade progression failed", e);
                Platform.runLater(() -> {
                    showError("Progression Error", "Failed to progress to next year: " + e.getMessage());
                    enableInputs();
                    hideProgress();
                });
            }
        }).start();
    }

    /**
     * Validate user inputs.
     */
    private boolean validateInputs() {
        // Check year name
        if (newYearNameField.getText() == null || newYearNameField.getText().trim().isEmpty()) {
            showError("Invalid Input", "Please enter a year name.");
            return false;
        }

        // Check dates are set
        if (newStartDatePicker.getValue() == null) {
            showError("Invalid Input", "Please select a start date.");
            return false;
        }

        if (newEndDatePicker.getValue() == null) {
            showError("Invalid Input", "Please select an end date.");
            return false;
        }

        // Check end date is after start date
        if (newEndDatePicker.getValue().isBefore(newStartDatePicker.getValue())) {
            showError("Invalid Dates", "End date must be after start date.");
            return false;
        }

        // Check graduation date is between start and end if provided
        if (newGraduationDatePicker.getValue() != null) {
            LocalDate gradDate = newGraduationDatePicker.getValue();
            if (gradDate.isBefore(newStartDatePicker.getValue()) ||
                    gradDate.isAfter(newEndDatePicker.getValue())) {
                showError("Invalid Date", "Graduation date must be between start and end dates.");
                return false;
            }
        }

        return true;
    }

    /**
     * Disable inputs during progression.
     */
    private void disableInputs() {
        newYearNameField.setDisable(true);
        newStartDatePicker.setDisable(true);
        newEndDatePicker.setDisable(true);
        newGraduationDatePicker.setDisable(true);
        startButton.setDisable(true);
        cancelButton.setDisable(true);
    }

    /**
     * Enable inputs after progression.
     */
    private void enableInputs() {
        newYearNameField.setDisable(false);
        newStartDatePicker.setDisable(false);
        newEndDatePicker.setDisable(false);
        newGraduationDatePicker.setDisable(false);
        startButton.setDisable(false);
        cancelButton.setDisable(false);
    }

    /**
     * Show progress box.
     */
    private void showProgress() {
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressLabel.setText("Initializing progression...");
    }

    /**
     * Hide progress box.
     */
    private void hideProgress() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
    }

    /**
     * Show success message and close dialog.
     */
    private void showSuccess(ProgressionResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Progression Successful");
        alert.setHeaderText("Grade Progression Complete!");
        alert.setContentText(
                "Academic Year: " + result.getNewYearName() + "\n\n" +
                        "Results:\n" +
                        "• " + result.getSeniorsGraduated() + " seniors graduated\n" +
                        "• " + result.getStudentsPromoted() + " students promoted\n" +
                        "• " + result.getEnrollmentsArchived() + " enrollments archived\n\n" +
                        "You can now assign courses to students for the new year."
        );
        alert.showAndWait();
        closeDialog();
    }

    /**
     * Show error alert.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Close the dialog.
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
