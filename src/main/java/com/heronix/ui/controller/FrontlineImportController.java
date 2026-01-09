package com.heronix.ui.controller;

import com.heronix.model.dto.FrontlineImportDTO;
import com.heronix.service.FrontlineImportService;
import com.heronix.service.FrontlineImportService.ImportResult;
import com.heronix.service.FrontlineImportService.ImportError;
import com.heronix.service.parser.FrontlineCSVParser;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Frontline CSV Import dialog
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Controller
public class FrontlineImportController {

    private static final Logger logger = LoggerFactory.getLogger(FrontlineImportController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    @Autowired
    private FrontlineCSVParser csvParser;

    @Autowired
    private FrontlineImportService importService;

    // File Selection
    @FXML private TextField filePathField;
    @FXML private Button previewButton;
    @FXML private Button importButton;

    // Options
    @FXML private CheckBox createSubstitutesCheckbox;
    @FXML private CheckBox updateExistingCheckbox;
    @FXML private CheckBox skipDuplicatesCheckbox;
    @FXML private CheckBox autoGenerateSchedulesCheckbox;

    // Preview Section
    @FXML private VBox previewSection;
    @FXML private Label totalRecordsLabel;
    @FXML private Label validRecordsLabel;
    @FXML private Label invalidRecordsLabel;
    @FXML private TableView<FrontlineImportDTO> previewTable;
    @FXML private TableColumn<FrontlineImportDTO, Integer> rowColumn;
    @FXML private TableColumn<FrontlineImportDTO, String> substituteColumn;
    @FXML private TableColumn<FrontlineImportDTO, String> dateColumn;
    @FXML private TableColumn<FrontlineImportDTO, String> timeColumn;
    @FXML private TableColumn<FrontlineImportDTO, String> replacedStaffColumn;
    @FXML private TableColumn<FrontlineImportDTO, String> statusColumn;

    // Progress Section
    @FXML private VBox progressSection;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    // Results Section
    @FXML private VBox resultsSection;
    @FXML private Label createdLabel;
    @FXML private Label updatedLabel;
    @FXML private Label skippedLabel;
    @FXML private Label errorsLabel;
    @FXML private VBox errorDetailsSection;
    @FXML private TextArea errorDetailsArea;

    private File selectedFile;
    private List<FrontlineImportDTO> previewData;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Substitute Data Import Controller");
        setupTableColumns();
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        rowColumn.setCellValueFactory(new PropertyValueFactory<>("rowNumber"));

        substituteColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSubstituteFullName()));

        dateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getAssignmentDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getAssignmentDate().format(DATE_FORMATTER));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        timeColumn.setCellValueFactory(cellData -> {
            String start = cellData.getValue().getStartTime() != null ?
                    cellData.getValue().getStartTime().format(TIME_FORMATTER) : "";
            String end = cellData.getValue().getEndTime() != null ?
                    cellData.getValue().getEndTime().format(TIME_FORMATTER) : "";
            return new javafx.beans.property.SimpleStringProperty(start + " - " + end);
        });

        replacedStaffColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReplacedStaffFullName()));

        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().isValid() ? "✓ Valid" : "✗ Invalid";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        // Style status column
        statusColumn.setCellFactory(column -> new TableCell<FrontlineImportDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Valid")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    /**
     * Handle browse file button
     */
    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Frontline CSV File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) filePathField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());
            previewButton.setDisable(false);

            // Reset sections
            previewSection.setVisible(false);
            previewSection.setManaged(false);
            progressSection.setVisible(false);
            progressSection.setManaged(false);
            resultsSection.setVisible(false);
            resultsSection.setManaged(false);

            logger.info("Selected file: {}", file.getName());
        }
    }

    /**
     * Handle preview button
     */
    @FXML
    private void handlePreview() {
        if (selectedFile == null) {
            showError("Please select a file first");
            return;
        }

        logger.info("Previewing file: {}", selectedFile.getName());

        // Show progress
        progressSection.setVisible(true);
        progressSection.setManaged(true);
        progressBar.setProgress(-1); // Indeterminate
        progressLabel.setText("Loading preview...");
        previewButton.setDisable(true);

        // Parse file in background
        Task<List<FrontlineImportDTO>> parseTask = new Task<>() {
            @Override
            protected List<FrontlineImportDTO> call() throws Exception {
                return csvParser.parseCSV(selectedFile);
            }
        };

        parseTask.setOnSucceeded(event -> {
            previewData = parseTask.getValue();
            displayPreview();
            progressSection.setVisible(false);
            progressSection.setManaged(false);
            previewButton.setDisable(false);
        });

        parseTask.setOnFailed(event -> {
            logger.error("Error parsing CSV", parseTask.getException());
            showError("Error parsing CSV: " + parseTask.getException().getMessage());
            progressSection.setVisible(false);
            progressSection.setManaged(false);
            previewButton.setDisable(false);
        });

        new Thread(parseTask).start();
    }

    /**
     * Display preview data
     */
    private void displayPreview() {
        long validCount = previewData.stream().filter(FrontlineImportDTO::isValid).count();
        long invalidCount = previewData.size() - validCount;

        totalRecordsLabel.setText(String.valueOf(previewData.size()));
        validRecordsLabel.setText(String.valueOf(validCount));
        invalidRecordsLabel.setText(String.valueOf(invalidCount));

        previewTable.getItems().clear();
        previewTable.getItems().addAll(previewData);

        previewSection.setVisible(true);
        previewSection.setManaged(true);

        // Enable import button if there are valid records
        importButton.setDisable(validCount == 0);

        logger.info("Preview loaded: {} total, {} valid, {} invalid", previewData.size(), validCount, invalidCount);
    }

    /**
     * Handle import button
     */
    @FXML
    private void handleImport() {
        if (selectedFile == null) {
            showError("Please select a file first");
            return;
        }

        // Confirm import
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Import");
        confirmAlert.setHeaderText("Import Frontline Data");
        confirmAlert.setContentText(String.format(
                "Import %d records from %s?\n\nThis will create substitute assignments in the system.",
                previewData != null ? previewData.size() : 0,
                selectedFile.getName()));

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        logger.info("Starting import from: {}", selectedFile.getName());

        // Hide preview, show progress
        previewSection.setVisible(false);
        previewSection.setManaged(false);
        resultsSection.setVisible(false);
        resultsSection.setManaged(false);
        progressSection.setVisible(true);
        progressSection.setManaged(true);
        progressBar.setProgress(-1);
        progressLabel.setText("Importing data...");

        // Disable buttons
        previewButton.setDisable(true);
        importButton.setDisable(true);

        // Import in background
        Task<ImportResult> importTask = new Task<>() {
            @Override
            protected ImportResult call() throws Exception {
                return importService.importFromCSV(selectedFile);
            }
        };

        importTask.setOnSucceeded(event -> {
            ImportResult result = importTask.getValue();
            displayResults(result);
            progressSection.setVisible(false);
            progressSection.setManaged(false);
            previewButton.setDisable(false);
            importButton.setDisable(true); // Keep disabled after import
        });

        importTask.setOnFailed(event -> {
            logger.error("Error during import", importTask.getException());
            showError("Error during import: " + importTask.getException().getMessage());
            progressSection.setVisible(false);
            progressSection.setManaged(false);
            previewButton.setDisable(false);
            importButton.setDisable(false);
        });

        new Thread(importTask).start();
    }

    /**
     * Display import results
     */
    private void displayResults(ImportResult result) {
        createdLabel.setText(String.valueOf(result.getCreatedCount()));
        updatedLabel.setText(String.valueOf(result.getUpdatedCount()));
        skippedLabel.setText(String.valueOf(result.getSkippedCount()));
        errorsLabel.setText(String.valueOf(result.getErrorCount()));

        // Show error details if any
        if (result.hasErrors()) {
            StringBuilder errorText = new StringBuilder();
            for (ImportError error : result.getErrors()) {
                errorText.append(error.toString()).append("\n");
            }
            errorDetailsArea.setText(errorText.toString());
            errorDetailsSection.setVisible(true);
            errorDetailsSection.setManaged(true);
        } else {
            errorDetailsSection.setVisible(false);
            errorDetailsSection.setManaged(false);
        }

        resultsSection.setVisible(true);
        resultsSection.setManaged(true);

        logger.info("Import completed: {}", result.getSummary());

        // Show success message
        if (result.getErrorCount() == 0) {
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Import Successful");
            successAlert.setHeaderText("Import Completed Successfully");
            successAlert.setContentText(result.getSummary());
            successAlert.showAndWait();
        } else {
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("Import Completed with Errors");
            warningAlert.setHeaderText("Import Completed with Some Errors");
            warningAlert.setContentText(result.getSummary() + "\n\nSee error details below.");
            warningAlert.showAndWait();
        }
    }

    /**
     * Handle close button
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) filePathField.getScene().getWindow();
        stage.close();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Import Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
