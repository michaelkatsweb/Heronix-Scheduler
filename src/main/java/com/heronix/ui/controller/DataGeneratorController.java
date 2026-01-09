package com.heronix.ui.controller;

import com.heronix.service.RealisticDataGeneratorService;
import com.heronix.service.PurgeDatabaseService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for Data Generator UI
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Controller
@Slf4j
public class DataGeneratorController {

    @Autowired
    private RealisticDataGeneratorService dataGeneratorService;

    @Autowired
    private PurgeDatabaseService purgeDatabaseService;

    @FXML
    private VBox mainContainer;

    @FXML
    private TextArea outputArea;

    @FXML
    private Button generateButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        log.info("Data Generator Controller initialized");
        outputArea.setEditable(false);
        progressBar.setVisible(false);
    }

    /**
     * Handle generate data button
     */
    @FXML
    private void handleGenerateData() {
        // Confirm action
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Generate Test Data");
        confirm.setHeaderText("Generate Comprehensive School Data");
        confirm.setContentText(
            "This will generate:\n" +
            "• 1,500 students (grades 9-12)\n" +
            "• 75 teachers (all subjects)\n" +
            "• 15 co-teachers\n" +
            "• 20 paraprofessionals\n" +
            "• 100+ rooms\n" +
            "• 200+ courses\n" +
            "• IEP/504 plans (~10% of students)\n" +
            "• School calendar with events\n\n" +
            "This may take several minutes. Continue?"
        );

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) {
            return;
        }

        // Disable button during generation
        generateButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Generating data...");
        outputArea.clear();
        appendOutput("Starting comprehensive data generation...\n");

        // Run in background thread
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("Generating departments...");
                    dataGeneratorService.generateAllData();
                    updateMessage("Data generation complete!");
                } catch (Exception e) {
                    log.error("Error generating data", e);
                    updateMessage("Error: " + e.getMessage());
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    appendOutput("\n✅ Data generation completed successfully!\n");
                    appendOutput("\nYou can now:\n");
                    appendOutput("• View students in the Students tab\n");
                    appendOutput("• View teachers in the Teachers tab\n");
                    appendOutput("• View courses in the Courses tab\n");
                    appendOutput("• View rooms in the Rooms tab\n");
                    appendOutput("• Generate schedules using the AI generator\n");

                    statusLabel.setText("Complete!");
                    progressBar.setVisible(false);
                    generateButton.setDisable(false);

                    showSuccess("Data Generation Complete",
                        "Successfully generated comprehensive school data!\n\n" +
                        "Check the output for details.");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable error = getException();
                    appendOutput("\n❌ Error: " + error.getMessage() + "\n");
                    statusLabel.setText("Failed");
                    progressBar.setVisible(false);
                    generateButton.setDisable(false);

                    showError("Data Generation Failed",
                        "An error occurred during data generation:\n" + error.getMessage());
                });
            }
        };

        // Monitor progress messages
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> {
                statusLabel.setText(newMsg);
                appendOutput(newMsg + "\n");
            });
        });

        // Start task in background
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Clear all data (with confirmation)
     */
    @FXML
    private void handleClearData() {
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Clear All Data");
        confirm.setHeaderText("⚠️ WARNING: This will delete ALL data!");
        confirm.setContentText(
            "This action will permanently delete:\n" +
            "• All students\n" +
            "• All teachers\n" +
            "• All courses\n" +
            "• All rooms\n" +
            "• All schedules\n" +
            "• All events\n\n" +
            "This action CANNOT be undone!\n\n" +
            "Are you absolutely sure?"
        );

        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) {
            return;
        }

        // Second confirmation
        TextInputDialog secondConfirm = new TextInputDialog();
        secondConfirm.setTitle("Confirm Data Deletion");
        secondConfirm.setHeaderText("Type 'DELETE' to confirm");
        secondConfirm.setContentText("Confirmation:");

        String confirmText = secondConfirm.showAndWait().orElse("");
        if (!"DELETE".equals(confirmText)) {
            showInfo("Cancelled", "Data deletion cancelled.");
            return;
        }

        // Execute data clearing using PurgeDatabaseService
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        statusLabel.setText("Clearing all data...");
        generateButton.setDisable(true);
        outputArea.clear();
        appendOutput("=== CLEARING ALL DATA ===\n\n");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                purgeDatabaseService.purgeAllData(message -> {
                    Platform.runLater(() -> appendOutput(message + "\n"));
                });
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                statusLabel.setText("Data cleared successfully");
                generateButton.setDisable(false);
                appendOutput("\n=== ALL DATA CLEARED ===\n");
                showSuccess("Success", "All data has been cleared successfully.");
            });
        });

        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                statusLabel.setText("Error clearing data");
                generateButton.setDisable(false);
                appendOutput("\nERROR: " + task.getException().getMessage() + "\n");
                showError("Error", "Failed to clear data: " + task.getException().getMessage());
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void appendOutput(String text) {
        outputArea.appendText(text);
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
