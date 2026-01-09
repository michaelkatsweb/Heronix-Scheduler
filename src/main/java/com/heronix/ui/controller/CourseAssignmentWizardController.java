package com.heronix.ui.controller;

import com.heronix.model.dto.AssignmentResult;
import com.heronix.repository.CourseEnrollmentRequestRepository;
import com.heronix.repository.PriorityRuleRepository;
import com.heronix.security.SecurityContext;
import com.heronix.service.IntelligentCourseAssignmentService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Course Assignment Wizard Controller
 *
 * Multi-step wizard for running intelligent automated course assignments.
 * Guides administrators through the assignment process with clear feedback
 * and simulation options.
 *
 * Wizard Steps:
 * 1. Pre-Assignment Check - Verify system readiness
 * 2. Configuration - Choose assignment mode and options
 * 3. Simulation - Preview results without committing
 * 4. Execution - Run actual assignment
 * 5. Results - View detailed results and next steps
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 5 - November 20, 2025
 */
@Component
public class CourseAssignmentWizardController {

    @Autowired
    private IntelligentCourseAssignmentService assignmentService;

    @Autowired
    private CourseEnrollmentRequestRepository enrollmentRequestRepository;

    @Autowired
    private PriorityRuleRepository priorityRuleRepository;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    @FXML
    private VBox step1Panel;

    @FXML
    private VBox step2Panel;

    @FXML
    private VBox step3Panel;

    @FXML
    private VBox step4Panel;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    // Step 1: Pre-Check
    @FXML
    private Label pendingRequestsLabel;

    @FXML
    private Label activeRulesLabel;

    @FXML
    private Label coursesLabel;

    @FXML
    private Label studentsLabel;

    @FXML
    private Label preCheckStatusLabel;

    // Step 2: Configuration
    @FXML
    private RadioButton runSimulationRadio;

    @FXML
    private RadioButton runActualRadio;

    @FXML
    private CheckBox applyRulesCheck;

    @FXML
    private CheckBox enableWaitlistCheck;

    @FXML
    private CheckBox allowAlternatesCheck;

    // Step 3: Simulation Results
    @FXML
    private TextArea simulationResultsArea;

    @FXML
    private Label simSuccessRateLabel;

    @FXML
    private Label simApprovedLabel;

    @FXML
    private Label simWaitlistedLabel;

    @FXML
    private Label simDeniedLabel;

    // Step 4: Execution Progress
    @FXML
    private TextArea executionLogArea;

    @FXML
    private ProgressIndicator executionProgress;

    // Buttons
    @FXML
    private Button nextButton;

    @FXML
    private Button backButton;

    @FXML
    private Button cancelButton;

    // ========================================================================
    // STATE
    // ========================================================================

    private int currentStep = 1;
    private AssignmentResult simulationResult;
    private AssignmentResult actualResult;
    private String currentUser;

    /**
     * Get the current logged-in username from security context
     */
    private String getCurrentUser() {
        if (currentUser == null) {
            currentUser = SecurityContext.getCurrentUsername().orElse("admin");
        }
        return currentUser;
    }

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        setupRadioButtons();
        showStep(1);
        runPreCheck();
    }

    /**
     * Setup radio button toggle group
     */
    private void setupRadioButtons() {
        ToggleGroup modeGroup = new ToggleGroup();
        runSimulationRadio.setToggleGroup(modeGroup);
        runActualRadio.setToggleGroup(modeGroup);
        runSimulationRadio.setSelected(true);

        // Default options
        applyRulesCheck.setSelected(true);
        enableWaitlistCheck.setSelected(true);
        allowAlternatesCheck.setSelected(true);
    }

    // ========================================================================
    // STEP NAVIGATION
    // ========================================================================

    /**
     * Show specific step
     */
    private void showStep(int step) {
        currentStep = step;

        // Hide all panels
        step1Panel.setVisible(false);
        step2Panel.setVisible(false);
        step3Panel.setVisible(false);
        step4Panel.setVisible(false);

        // Show current step
        switch (step) {
            case 1:
                step1Panel.setVisible(true);
                statusLabel.setText("Step 1: Pre-Assignment Check");
                backButton.setDisable(true);
                nextButton.setText("Next");
                nextButton.setDisable(false);
                break;

            case 2:
                step2Panel.setVisible(true);
                statusLabel.setText("Step 2: Configuration");
                backButton.setDisable(false);
                nextButton.setText("Run");
                nextButton.setDisable(false);
                break;

            case 3:
                step3Panel.setVisible(true);
                statusLabel.setText("Step 3: Review Simulation");
                backButton.setDisable(false);
                nextButton.setText(actualResult != null ? "View Results" : "Run Actual Assignment");
                nextButton.setDisable(simulationResult == null);
                break;

            case 4:
                step4Panel.setVisible(true);
                statusLabel.setText("Step 4: Assignment Complete");
                backButton.setDisable(true);
                nextButton.setText("Close");
                nextButton.setDisable(false);
                break;
        }

        updateProgress();
    }

    /**
     * Update progress indicator
     */
    private void updateProgress() {
        double progress = currentStep / 4.0;
        progressBar.setProgress(progress);
        progressLabel.setText("Step " + currentStep + " of 4");
    }

    /**
     * Handle Next button
     */
    @FXML
    private void handleNext() {
        switch (currentStep) {
            case 1:
                // Move to configuration
                showStep(2);
                break;

            case 2:
                // Run simulation or actual assignment
                if (runSimulationRadio.isSelected()) {
                    runSimulation();
                } else {
                    runActualAssignment();
                }
                break;

            case 3:
                // Move to execution or show results
                if (actualResult != null) {
                    showStep(4);
                } else {
                    runActualAssignment();
                }
                break;

            case 4:
                // Close wizard
                handleCancel();
                break;
        }
    }

    /**
     * Handle Back button
     */
    @FXML
    private void handleBack() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    /**
     * Handle Cancel button
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    // ========================================================================
    // STEP 1: PRE-CHECK
    // ========================================================================

    /**
     * Run pre-assignment system check
     */
    private void runPreCheck() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Platform.runLater(() -> {
                    // Count pending requests
                    long pendingCount = enrollmentRequestRepository.countByRequestStatus(
                        com.heronix.model.enums.EnrollmentRequestStatus.PENDING
                    );
                    pendingRequestsLabel.setText(pendingCount + " pending requests");

                    // Count active rules
                    long activeRulesCount = priorityRuleRepository.countByActiveTrue();
                    activeRulesLabel.setText(activeRulesCount + " active priority rules");

                    // Check status
                    if (pendingCount == 0) {
                        preCheckStatusLabel.setText("⚠️ No pending requests found. Create enrollment requests first.");
                        preCheckStatusLabel.setStyle("-fx-text-fill: orange;");
                        nextButton.setDisable(true);
                    } else if (activeRulesCount == 0) {
                        preCheckStatusLabel.setText("⚠️ No active priority rules. Results will use base priority only.");
                        preCheckStatusLabel.setStyle("-fx-text-fill: orange;");
                    } else {
                        preCheckStatusLabel.setText("✅ System ready for assignment");
                        preCheckStatusLabel.setStyle("-fx-text-fill: green;");
                    }
                });
                return null;
            }
        };

        new Thread(task).start();
    }

    // ========================================================================
    // STEP 2 & 3: SIMULATION
    // ========================================================================

    /**
     * Run simulation
     */
    private void runSimulation() {
        nextButton.setDisable(true);
        simulationResultsArea.setText("Running simulation...\n");

        Task<AssignmentResult> task = new Task<>() {
            @Override
            protected AssignmentResult call() {
                return assignmentService.runAutomatedAssignment(
                    null,           // Academic year (null = all)
                    currentUser,    // Initiated by
                    true            // Simulation mode
                );
            }

            @Override
            protected void succeeded() {
                simulationResult = getValue();
                displaySimulationResults();
                showStep(3);
            }

            @Override
            protected void failed() {
                showError("Simulation failed: " + getException().getMessage());
                nextButton.setDisable(false);
            }
        };

        new Thread(task).start();
    }

    /**
     * Display simulation results
     */
    private void displaySimulationResults() {
        if (simulationResult == null) return;

        simulationResultsArea.setText(simulationResult.getDetailedReport());

        simSuccessRateLabel.setText(String.format("%.1f%%", simulationResult.getSuccessRate()));
        simApprovedLabel.setText(String.valueOf(simulationResult.getRequestsApproved()));
        simWaitlistedLabel.setText(String.valueOf(simulationResult.getRequestsWaitlisted()));
        simDeniedLabel.setText(String.valueOf(simulationResult.getRequestsDenied()));

        // Color code success rate
        if (simulationResult.getSuccessRate() >= 90) {
            simSuccessRateLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else if (simulationResult.getSuccessRate() >= 75) {
            simSuccessRateLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            simSuccessRateLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    // ========================================================================
    // STEP 3 & 4: ACTUAL ASSIGNMENT
    // ========================================================================

    /**
     * Run actual assignment
     */
    private void runActualAssignment() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Assignment");
        confirm.setHeaderText("Run Actual Course Assignment");
        confirm.setContentText(
            "This will assign students to courses based on priority scores.\n\n" +
            "This action cannot be undone automatically. Continue?"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executeActualAssignment();
            }
        });
    }

    /**
     * Execute actual assignment
     */
    private void executeActualAssignment() {
        showStep(4);
        nextButton.setDisable(true);
        backButton.setDisable(true);
        executionLogArea.setText("Starting automated course assignment...\n");
        executionProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Task<AssignmentResult> task = new Task<>() {
            @Override
            protected AssignmentResult call() throws Exception {
                updateMessage("Loading pending requests...");
                Thread.sleep(500);

                updateMessage("Applying priority rules...");
                Thread.sleep(500);

                updateMessage("Processing assignments...");
                AssignmentResult result = assignmentService.runAutomatedAssignment(
                    null,
                    currentUser,
                    false  // NOT a simulation
                );

                updateMessage("Generating report...");
                Thread.sleep(500);

                return result;
            }

            @Override
            protected void updateMessage(String message) {
                Platform.runLater(() -> {
                    executionLogArea.appendText(message + "\n");
                });
            }

            @Override
            protected void succeeded() {
                actualResult = getValue();
                displayActualResults();
                executionProgress.setProgress(1.0);
                nextButton.setDisable(false);
            }

            @Override
            protected void failed() {
                executionLogArea.appendText("\n❌ ERROR: " + getException().getMessage() + "\n");
                executionProgress.setProgress(0);
                showError("Assignment failed: " + getException().getMessage());
                backButton.setDisable(false);
            }
        };

        new Thread(task).start();
    }

    /**
     * Display actual assignment results
     */
    private void displayActualResults() {
        if (actualResult == null) return;

        executionLogArea.appendText("\n" + actualResult.getDetailedReport());

        if (actualResult.isSuccessful()) {
            executionLogArea.appendText("\n✅ Assignment completed successfully!\n");
        } else {
            executionLogArea.appendText("\n⚠️ Assignment completed with issues. Review report above.\n");
        }

        // Show students needing review
        if (!actualResult.getStudentsNeedingReview().isEmpty()) {
            executionLogArea.appendText("\n⚠️ Students needing manual review:\n");
            for (String student : actualResult.getStudentsNeedingReview()) {
                executionLogArea.appendText("  - " + student + "\n");
            }
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
