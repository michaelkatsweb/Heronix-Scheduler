package com.heronix.ui.controller;

import com.heronix.service.GradeImportService;
import com.heronix.service.GradeImportService.ImportResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Grade Import Dialog Controller
 *
 * Handles CSV/JSON import of grades from external SIS platforms:
 * - Skyward
 * - PowerSchool
 * - Infinite Campus
 * - Custom CSV files
 *
 * Features:
 * - File upload with preview
 * - Auto-detection of SIS platform
 * - Field mapping configuration
 * - Progress tracking
 * - Error reporting
 * - Success summary
 *
 * @version 1.0.0
 */
@Slf4j
@Controller
public class GradeImportDialogController {

    @Autowired
    private GradeImportService importService;

    // UI Components
    @FXML private Label fileNameLabel;
    @FXML private Label detectedPlatformLabel;
    @FXML private Label recordCountLabel;
    @FXML private TextArea previewTextArea;
    @FXML private VBox fieldMappingSection;
    @FXML private CheckBox autoDetectCheckBox;
    @FXML private ProgressBar importProgressBar;
    @FXML private Label progressLabel;
    @FXML private TextArea resultTextArea;
    @FXML private Button selectFileButton;
    @FXML private Button importButton;
    @FXML private Button closeButton;

    // State
    private File selectedFile;
    private String[] headers;
    private Map<String, String> fieldMapping;
    private boolean importCompleted = false;
    private ImportResult lastResult;

    @FXML
    public void initialize() {
        log.info("Initializing GradeImportDialogController");

        // Initial state
        importButton.setDisable(true);
        importProgressBar.setVisible(false);
        progressLabel.setVisible(false);
        resultTextArea.setVisible(false);
        fieldMappingSection.setVisible(false);

        // Auto-detect enabled by default
        autoDetectCheckBox.setSelected(true);

        // Setup preview
        previewTextArea.setEditable(false);
        previewTextArea.setWrapText(false);
        previewTextArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");

        // Setup result area
        resultTextArea.setEditable(false);
        resultTextArea.setWrapText(true);
    }

    /**
     * Handle Select File button
     */
    @FXML
    private void handleSelectFile() {
        log.info("Select file clicked");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Grade Import File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) selectFileButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedFile = file;
            loadFilePreview(file);
        }
    }

    /**
     * Load file preview and detect format
     */
    private void loadFilePreview(File file) {
        try {
            log.info("Loading preview for file: {}", file.getName());

            // Read first few lines for preview
            StringBuilder preview = new StringBuilder();
            String[] previewLines = new String[10];
            int lineCount = 0;

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null && lineCount < 10) {
                    preview.append(line).append("\n");
                    previewLines[lineCount] = line;
                    lineCount++;
                }
            }

            // Parse headers
            if (lineCount > 0) {
                headers = parseCSVLine(previewLines[0]);

                // Detect platform if auto-detect enabled
                if (autoDetectCheckBox.isSelected()) {
                    String platform = importService.detectSISPlatform(headers);
                    detectedPlatformLabel.setText("Detected: " + platform);

                    // Auto-map fields
                    fieldMapping = importService.autoDetectFieldMapping(headers);
                    log.info("Auto-detected {} field mappings", fieldMapping.size());
                }
            }

            // Update UI
            fileNameLabel.setText(file.getName());
            previewTextArea.setText(preview.toString());
            recordCountLabel.setText(String.format("≈ %d records", lineCount - 1)); // Exclude header

            // Show field mapping section if manual mapping needed
            if (!autoDetectCheckBox.isSelected()) {
                fieldMappingSection.setVisible(true);
                populateFieldMappingControls();
            }

            // Enable import button
            importButton.setDisable(false);

            log.info("File preview loaded successfully");

        } catch (Exception e) {
            log.error("Error loading file preview", e);
            showError("Error", "Failed to load file: " + e.getMessage());
        }
    }

    /**
     * Populate field mapping controls for manual mapping
     */
    private void populateFieldMappingControls() {
        fieldMappingSection.getChildren().clear();

        // Required field names and their display labels
        String[][] fields = {
            {"studentId", "Student ID *"},
            {"courseCode", "Course Code *"},
            {"courseName", "Course Name"},
            {"letterGrade", "Letter Grade *"},
            {"numericalGrade", "Numerical Grade"},
            {"credits", "Credits"},
            {"term", "Term/Semester"},
            {"academicYear", "Academic Year"},
            {"teacherName", "Teacher Name"},
            {"absences", "Absences"},
            {"tardies", "Tardies"},
            {"comments", "Comments"}
        };

        // Create header
        Label headerLabel = new Label("Map CSV Columns to Grade Fields:");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        fieldMappingSection.getChildren().add(headerLabel);

        // Initialize field mapping if null
        if (fieldMapping == null) {
            fieldMapping = new HashMap<>();
        }

        // Create combo boxes for each field
        for (String[] field : fields) {
            String fieldName = field[0];
            String displayLabel = field[1];

            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label label = new Label(displayLabel);
            label.setPrefWidth(120);

            ComboBox<String> combo = new ComboBox<>();
            combo.setPrefWidth(200);
            combo.getItems().add("-- Not Mapped --");
            if (headers != null) {
                for (String header : headers) {
                    combo.getItems().add(header);
                }
            }
            combo.setValue("-- Not Mapped --");

            // Store mapping when selection changes
            final String fName = fieldName;
            combo.setOnAction(e -> {
                String selected = combo.getValue();
                if (selected != null && !selected.equals("-- Not Mapped --")) {
                    fieldMapping.put(selected, fName);
                } else {
                    // Remove any existing mapping for this field
                    fieldMapping.entrySet().removeIf(entry -> entry.getValue().equals(fName));
                }
            });

            row.getChildren().addAll(label, combo);
            fieldMappingSection.getChildren().add(row);
        }

        // Add note about required fields
        Label noteLabel = new Label("* Required fields must be mapped");
        noteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        fieldMappingSection.getChildren().add(noteLabel);
    }

    /**
     * Parse CSV line (handles quoted fields)
     */
    private String[] parseCSVLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        return result.toArray(new String[0]);
    }

    /**
     * Handle Import button
     */
    @FXML
    private void handleImport() {
        log.info("Import button clicked");

        if (selectedFile == null) {
            showError("No File", "Please select a file to import.");
            return;
        }

        // Disable buttons during import
        selectFileButton.setDisable(true);
        importButton.setDisable(true);

        // Show progress
        importProgressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressLabel.setText("Importing grades...");

        // Run import in background thread
        Task<ImportResult> importTask = new Task<ImportResult>() {
            @Override
            protected ImportResult call() throws Exception {
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    // Use auto-detected mapping or manual mapping
                    Map<String, String> mapping = autoDetectCheckBox.isSelected()
                        ? fieldMapping
                        : null;

                    return importService.importFromCSV(fis, mapping);
                }
            }

            @Override
            protected void succeeded() {
                ImportResult result = getValue();
                lastResult = result;
                importCompleted = true;
                handleImportComplete(result);
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                log.error("Import failed", exception);
                handleImportError(exception);
            }
        };

        // Start import
        new Thread(importTask).start();
    }

    /**
     * Handle successful import
     */
    private void handleImportComplete(ImportResult result) {
        Platform.runLater(() -> {
            log.info("Import complete: {}", result.getSummary());

            // Hide progress
            importProgressBar.setVisible(false);
            progressLabel.setVisible(false);

            // Show results
            resultTextArea.setVisible(true);
            StringBuilder resultText = new StringBuilder();

            resultText.append("╔══════════════════════════════════════╗\n");
            resultText.append("║     GRADE IMPORT COMPLETE           ║\n");
            resultText.append("╚══════════════════════════════════════╝\n\n");

            resultText.append(String.format("📊 SUMMARY\n"));
            resultText.append(String.format("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"));
            resultText.append(String.format("Total Records:    %d\n", result.getTotalCount()));
            resultText.append(String.format("✓ Successful:     %d\n", result.getSuccessCount()));
            resultText.append(String.format("✗ Errors:         %d\n", result.getErrorCount()));
            resultText.append(String.format("⚠ Warnings:       %d\n", result.getWarningCount()));
            resultText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            // Show errors if any
            if (result.hasErrors()) {
                resultText.append("❌ ERRORS:\n");
                int errorCount = 0;
                for (String error : result.getErrorMessages()) {
                    if (errorCount++ < 20) { // Show first 20 errors
                        resultText.append("  • ").append(error).append("\n");
                    }
                }
                if (result.getErrorCount() > 20) {
                    resultText.append(String.format("  ... and %d more errors\n",
                        result.getErrorCount() - 20));
                }
                resultText.append("\n");
            }

            // Show warnings if any
            if (result.hasWarnings()) {
                resultText.append("⚠️ WARNINGS:\n");
                int warningCount = 0;
                for (String warning : result.getWarningMessages()) {
                    if (warningCount++ < 10) { // Show first 10 warnings
                        resultText.append("  • ").append(warning).append("\n");
                    }
                }
                if (result.getWarningCount() > 10) {
                    resultText.append(String.format("  ... and %d more warnings\n",
                        result.getWarningCount() - 10));
                }
                resultText.append("\n");
            }

            // Success message
            if (result.getSuccessCount() > 0) {
                resultText.append("✅ SUCCESS\n");
                resultText.append(String.format("Imported %d grades successfully!\n",
                    result.getSuccessCount()));
                resultText.append("All student GPAs have been updated.\n\n");
            }

            resultText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            resultText.append("Import completed. You can close this dialog.\n");

            resultTextArea.setText(resultText.toString());

            // Re-enable close button
            closeButton.setDisable(false);

            // Show success dialog
            if (!result.hasErrors()) {
                showInfo("Import Successful",
                    String.format("Successfully imported %d grades!\n\n%s",
                        result.getSuccessCount(),
                        "Student GPAs have been updated."));
            } else {
                showWarning("Import Completed with Errors",
                    String.format("Imported %d of %d records.\n\n%d errors occurred. See details below.",
                        result.getSuccessCount(),
                        result.getTotalCount(),
                        result.getErrorCount()));
            }
        });
    }

    /**
     * Handle import error
     */
    private void handleImportError(Throwable exception) {
        Platform.runLater(() -> {
            // Hide progress
            importProgressBar.setVisible(false);
            progressLabel.setVisible(false);

            // Show error
            resultTextArea.setVisible(true);
            resultTextArea.setText(String.format(
                "❌ IMPORT FAILED\n\n" +
                "Error: %s\n\n" +
                "Please check:\n" +
                "  • File format is correct (CSV)\n" +
                "  • File is not corrupted\n" +
                "  • Required fields are present\n" +
                "  • Data values are valid\n\n" +
                "Details:\n%s",
                exception.getMessage(),
                exception.toString()
            ));

            // Re-enable buttons
            selectFileButton.setDisable(false);
            importButton.setDisable(false);
            closeButton.setDisable(false);

            showError("Import Failed", "Failed to import grades: " + exception.getMessage());
        });
    }

    /**
     * Handle Cancel/Close button
     */
    @FXML
    private void handleClose() {
        log.info("Close button clicked");
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Check if import was completed
     */
    public boolean isImportCompleted() {
        return importCompleted;
    }

    /**
     * Get import result
     */
    public ImportResult getImportResult() {
        return lastResult;
    }

    // Helper methods
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
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
}
