package com.heronix.ui.controller;

import com.heronix.service.PurgeDatabaseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Purge Database confirmation dialog.
 *
 * Provides a nuclear option to delete ALL data from the database with proper
 * confirmation and safety measures.
 *
 * @author Heronix Scheduler Team
 */
@Controller
public class PurgeDatabaseController {

    private static final Logger log = LoggerFactory.getLogger(PurgeDatabaseController.class);
    private static final String CONFIRMATION_TEXT = "DELETE ALL DATA";

    @Autowired
    private PurgeDatabaseService purgeDatabaseService;

    @FXML
    private TextField confirmationField;

    @FXML
    private CheckBox confirmCheckbox;

    @FXML
    private Button purgeButton;

    @FXML
    private Label errorLabel;

    @FXML
    private VBox progressContainer;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    private boolean purgeSuccessful = false;

    /**
     * Initializes the controller.
     * Sets up listeners for confirmation field and checkbox.
     */
    @FXML
    public void initialize() {
        log.info("Initializing Purge Database dialog");

        // Enable purge button only when both confirmations are complete
        confirmationField.textProperty().addListener((obs, oldVal, newVal) -> updatePurgeButton());
        confirmCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> updatePurgeButton());
    }

    /**
     * Updates the purge button enabled state based on confirmation requirements.
     */
    private void updatePurgeButton() {
        boolean textMatches = CONFIRMATION_TEXT.equals(confirmationField.getText());
        boolean checkboxSelected = confirmCheckbox.isSelected();

        purgeButton.setDisable(!textMatches || !checkboxSelected);

        // Clear error message when user starts typing
        if (errorLabel.isVisible() && !confirmationField.getText().isEmpty()) {
            errorLabel.setVisible(false);
        }
    }

    /**
     * Handles the purge button action.
     * Performs final confirmation and executes database purge.
     */
    @FXML
    private void handlePurge() {
        log.warn("User initiated database purge");

        // Final confirmation dialog
        Alert finalConfirm = new Alert(Alert.AlertType.WARNING);
        finalConfirm.setTitle("FINAL WARNING");
        finalConfirm.setHeaderText("Are you absolutely sure?");
        finalConfirm.setContentText(
            "This will permanently DELETE ALL DATA from the database.\n\n" +
            "This action CANNOT be undone!\n\n" +
            "Click OK to proceed with database purge, or Cancel to abort."
        );

        ButtonType result = finalConfirm.showAndWait().orElse(ButtonType.CANCEL);

        if (result == ButtonType.OK) {
            executePurge();
        } else {
            log.info("User cancelled database purge at final confirmation");
        }
    }

    /**
     * Executes the database purge operation in background thread.
     */
    private void executePurge() {
        log.warn("Executing database purge - ALL DATA WILL BE DELETED");

        // Disable all controls
        confirmationField.setDisable(true);
        confirmCheckbox.setDisable(true);
        purgeButton.setDisable(true);

        // Show progress indicator
        progressContainer.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressLabel.setText("Purging database...");

        // Execute purge in background thread
        new Thread(() -> {
            try {
                // Call service to delete all data
                purgeDatabaseService.purgeAllData(this::updateProgress);

                // Success
                Platform.runLater(() -> {
                    log.info("Database purge completed successfully");
                    purgeSuccessful = true;

                    // Show success message
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Database Purged");
                    success.setHeaderText("Database purge completed successfully");
                    success.setContentText(
                        "All data has been deleted from the database.\n\n" +
                        "The application will now refresh.\n\n" +
                        "You can import new data or start fresh."
                    );
                    success.showAndWait();

                    // Close dialog
                    closeDialog();
                });

            } catch (Exception e) {
                log.error("Error during database purge", e);

                Platform.runLater(() -> {
                    // Hide progress
                    progressContainer.setVisible(false);

                    // Re-enable controls
                    confirmationField.setDisable(false);
                    confirmCheckbox.setDisable(false);
                    updatePurgeButton();

                    // Show error
                    errorLabel.setText("❌ Error: " + e.getMessage());
                    errorLabel.setVisible(true);

                    // Show error dialog
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Purge Failed");
                    error.setHeaderText("Database purge failed");
                    error.setContentText("Error: " + e.getMessage());
                    error.showAndWait();
                });
            }
        }).start();
    }

    /**
     * Updates the progress indicator with current operation.
     *
     * @param message Progress message
     */
    private void updateProgress(String message) {
        Platform.runLater(() -> {
            progressLabel.setText(message);
            log.info("Purge progress: {}", message);
        });
    }

    /**
     * Handles the cancel button action.
     * Closes the dialog without purging.
     */
    @FXML
    private void handleCancel() {
        log.info("User cancelled database purge");
        closeDialog();
    }

    /**
     * Closes the dialog window.
     */
    private void closeDialog() {
        Stage stage = (Stage) confirmationField.getScene().getWindow();
        stage.close();
    }

    /**
     * Returns whether the purge was successful.
     *
     * @return true if purge completed successfully
     */
    public boolean isPurgeSuccessful() {
        return purgeSuccessful;
    }
}
