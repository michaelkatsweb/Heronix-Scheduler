package com.heronix.ui.controller;

import com.heronix.service.BulkEnrollmentImportService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Bulk Enrollment Import Controller
 *
 * Wizard-style interface for importing enrollment requests from CSV files.
 * Guides users through file selection, validation, and import process.
 *
 * Steps:
 * 1. File Selection - Choose CSV file and configure options
 * 2. Validation - Preview and validate data
 * 3. Import - Execute import with progress tracking
 * 4. Results - View summary and any errors
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - November 21, 2025
 */
@Slf4j
@Component
public class BulkEnrollmentImportController {

    @Autowired
    private BulkEnrollmentImportService importService;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    // Step 1: File Selection
    @FXML
    private TextField filePathField;

    @FXML
    private Button browseButton;

    @FXML
    private CheckBox skipHeaderCheck;

    @FXML
    private Label fileInfoLabel;

    // Step 2: Validation
    @FXML
    private TextArea validationResultsArea;

    @FXML
    private Label validationStatusLabel;

    @FXML
    private ProgressIndicator validationProgress;

    // Step 3: Import Progress
    @FXML
    private TextArea importLogArea;

    @FXML
    private ProgressBar importProgress;

    @FXML
    private Label importStatusLabel;

    // Step 4: Results
    @FXML
    private TextArea resultsArea;

    @FXML
    private Label resultsSummaryLabel;

    // Navigation
    @FXML
    private Button validateButton;

    @FXML
    private Button importButton;

    @FXML
    private Button downloadTemplateButton;

    @FXML
    private Button generateSampleButton;

    @FXML
    private Button closeButton;

    // ========================================================================
    // STATE
    // ========================================================================

    private File selectedFile;
    private BulkEnrollmentImportService.ImportResult validationResult;
    private BulkEnrollmentImportService.ImportResult importResult;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        setupButtons();
        resetUI();
    }

    /**
     * Setup button states
     */
    private void setupButtons() {
        validateButton.setDisable(true);
        importButton.setDisable(true);
        skipHeaderCheck.setSelected(true);
    }

    /**
     * Reset UI to initial state
     */
    private void resetUI() {
        filePathField.clear();
        fileInfoLabel.setText("No file selected");
        validationResultsArea.clear();
        importLogArea.clear();
        resultsArea.clear();
        validationStatusLabel.setText("");
        importStatusLabel.setText("");
        resultsSummaryLabel.setText("");
        validationProgress.setVisible(false);
        importProgress.setProgress(0);

        selectedFile = null;
        validationResult = null;
        importResult = null;

        validateButton.setDisable(true);
        importButton.setDisable(true);
    }

    // ========================================================================
    // FILE SELECTION
    // ========================================================================

    /**
     * Handle Browse button
     */
    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());
            updateFileInfo(file);
            validateButton.setDisable(false);

            // Clear previous results
            validationResultsArea.clear();
            importLogArea.clear();
            resultsArea.clear();
            importButton.setDisable(true);
        }
    }

    /**
     * Update file information label
     */
    private void updateFileInfo(File file) {
        long fileSizeKB = file.length() / 1024;
        fileInfoLabel.setText(String.format("File: %s (%.1f KB)", file.getName(), fileSizeKB / 1024.0));
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Handle Validate button
     */
    @FXML
    private void handleValidate() {
        if (selectedFile == null) {
            showError("Please select a CSV file first");
            return;
        }

        validateButton.setDisable(true);
        validationProgress.setVisible(true);
        validationResultsArea.setText("Validating CSV file...\n");

        Task<BulkEnrollmentImportService.ImportResult> task = new Task<>() {
            @Override
            protected BulkEnrollmentImportService.ImportResult call() {
                return importService.validateCSV(selectedFile, skipHeaderCheck.isSelected());
            }

            @Override
            protected void succeeded() {
                validationResult = getValue();
                displayValidationResults();
                validationProgress.setVisible(false);
                validateButton.setDisable(false);

                if (validationResult.isSuccess()) {
                    importButton.setDisable(false);
                }
            }

            @Override
            protected void failed() {
                validationProgress.setVisible(false);
                validateButton.setDisable(false);
                showError("Validation failed: " + getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    /**
     * Display validation results
     */
    private void displayValidationResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("VALIDATION RESULTS\n");
        sb.append("=".repeat(60)).append("\n\n");

        sb.append(String.format("Total Rows: %d\n", validationResult.getTotalRows()));
        sb.append(String.format("Valid Rows: %d\n", validationResult.getSuccessfulRows()));
        sb.append(String.format("Invalid Rows: %d\n", validationResult.getFailedRows()));

        if (validationResult.isSuccess()) {
            sb.append("\n✅ Validation passed! File is ready to import.\n");
            validationStatusLabel.setText("✅ Valid");
            validationStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            sb.append("\n❌ Validation failed! Please fix errors before importing.\n");
            validationStatusLabel.setText("❌ Invalid");
            validationStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }

        if (!validationResult.getErrors().isEmpty()) {
            sb.append("\nERRORS:\n");
            for (String error : validationResult.getErrors()) {
                sb.append("  ❌ ").append(error).append("\n");
            }
        }

        if (!validationResult.getWarnings().isEmpty()) {
            sb.append("\nWARNINGS:\n");
            for (String warning : validationResult.getWarnings()) {
                sb.append("  ⚠️  ").append(warning).append("\n");
            }
        }

        validationResultsArea.setText(sb.toString());
    }

    // ========================================================================
    // IMPORT
    // ========================================================================

    /**
     * Handle Import button
     */
    @FXML
    private void handleImport() {
        if (selectedFile == null) {
            showError("Please select a CSV file first");
            return;
        }

        if (validationResult == null || !validationResult.isSuccess()) {
            showError("Please validate the file first and ensure it passes validation");
            return;
        }

        // Confirm import
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Import");
        confirm.setHeaderText("Import Enrollment Requests");
        confirm.setContentText(String.format(
            "This will create enrollment requests for %d students.\n\n" +
            "This action cannot be undone. Continue?",
            validationResult.getSuccessfulRows()
        ));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executeImport();
            }
        });
    }

    /**
     * Execute the import
     */
    private void executeImport() {
        importButton.setDisable(true);
        validateButton.setDisable(true);
        importProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        importLogArea.setText("Starting import...\n");

        Task<BulkEnrollmentImportService.ImportResult> task = new Task<>() {
            @Override
            protected BulkEnrollmentImportService.ImportResult call() throws Exception {
                updateMessage("Reading CSV file...");
                Thread.sleep(300);

                updateMessage("Validating data...");
                Thread.sleep(300);

                updateMessage("Creating enrollment requests...");
                BulkEnrollmentImportService.ImportResult result =
                    importService.importFromCSV(selectedFile, skipHeaderCheck.isSelected());

                updateMessage("Saving to database...");
                Thread.sleep(500);

                return result;
            }

            @Override
            protected void updateMessage(String message) {
                Platform.runLater(() -> {
                    importLogArea.appendText(message + "\n");
                    importStatusLabel.setText(message);
                });
            }

            @Override
            protected void succeeded() {
                importResult = getValue();
                displayImportResults();
                importProgress.setProgress(1.0);
                importStatusLabel.setText("Import Complete");
                importButton.setDisable(false);
                validateButton.setDisable(false);
            }

            @Override
            protected void failed() {
                importProgress.setProgress(0);
                importStatusLabel.setText("Import Failed");
                showError("Import failed: " + getException().getMessage());
                importButton.setDisable(false);
                validateButton.setDisable(false);
            }
        };

        new Thread(task).start();
    }

    /**
     * Display import results
     */
    private void displayImportResults() {
        String summary = importResult.getSummary();
        resultsArea.setText(summary);

        if (importResult.isSuccess()) {
            resultsSummaryLabel.setText(String.format("✅ Successfully imported %d enrollment requests",
                importResult.getRequestsCreated()));
            resultsSummaryLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

            showInfo(String.format(
                "Import Complete!\n\n" +
                "Successfully created %d enrollment requests for %d students.\n" +
                "Processing time: %d ms",
                importResult.getRequestsCreated(),
                importResult.getSuccessfulRows(),
                importResult.getProcessingTimeMs()
            ));
        } else {
            resultsSummaryLabel.setText("❌ Import completed with errors");
            resultsSummaryLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

            showWarning(String.format(
                "Import completed with errors.\n\n" +
                "Successful: %d rows\n" +
                "Failed: %d rows\n\n" +
                "See results tab for details.",
                importResult.getSuccessfulRows(),
                importResult.getFailedRows()
            ));
        }
    }

    // ========================================================================
    // TEMPLATE & SAMPLE GENERATION
    // ========================================================================

    /**
     * Handle Download Template button
     */
    @FXML
    private void handleDownloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save CSV Template");
        fileChooser.setInitialFileName("enrollment_requests_template.csv");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showSaveDialog(downloadTemplateButton.getScene().getWindow());
        if (file != null) {
            try {
                importService.generateCSVTemplate(file, true);
                showInfo("Template downloaded successfully!\n\nLocation: " + file.getAbsolutePath());
            } catch (Exception e) {
                showError("Failed to generate template: " + e.getMessage());
            }
        }
    }

    /**
     * Handle Generate Sample Data button
     */
    @FXML
    private void handleGenerateSample() {
        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Generate Sample Data");
        dialog.setHeaderText("Generate Sample CSV Data");
        dialog.setContentText("Number of students:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                int count = Integer.parseInt(input);
                if (count <= 0 || count > 10000) {
                    showError("Please enter a number between 1 and 10000");
                    return;
                }

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Sample Data");
                fileChooser.setInitialFileName(String.format("sample_enrollment_requests_%d.csv", count));
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv")
                );

                File file = fileChooser.showSaveDialog(generateSampleButton.getScene().getWindow());
                if (file != null) {
                    importService.generateSampleData(file, count);
                    showInfo(String.format(
                        "Sample data generated successfully!\n\n" +
                        "Students: %d\n" +
                        "Location: %s",
                        count,
                        file.getAbsolutePath()
                    ));
                }
            } catch (NumberFormatException e) {
                showError("Invalid number format");
            } catch (Exception e) {
                showError("Failed to generate sample data: " + e.getMessage());
            }
        });
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
