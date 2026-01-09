package com.heronix.ui.controller;

import com.heronix.model.dto.ValidationIssue;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Import Preview Controller
 * Shows preview of data before importing with validation status
 *
 * Features:
 * - Preview table with dynamic columns
 * - Validation error display
 * - Filter by valid/error rows
 * - Import options configuration
 * - Row count statistics
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/ImportPreviewController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
@Component
public class ImportPreviewController {

    // ========================================================================
    // FXML FIELDS
    // ========================================================================

    @FXML private Label fileNameLabel;
    @FXML private Label entityTypeLabel;
    @FXML private Label totalRowsLabel;
    @FXML private Label validRowsLabel;
    @FXML private Label errorRowsLabel;

    @FXML private RadioButton showAllRadio;
    @FXML private RadioButton showValidRadio;
    @FXML private RadioButton showErrorsRadio;
    @FXML private ComboBox<String> previewLimitCombo;

    @FXML private TableView<Map<String, String>> previewTable;
    @FXML private Label rowCountLabel;

    @FXML private VBox validationIssuesBox;
    @FXML private VBox issuesContainer;

    @FXML private CheckBox skipErrorsCheckBox;
    @FXML private CheckBox createBackupCheckBox;
    @FXML private CheckBox validateDuplicatesCheckBox;

    @FXML private Button backButton;
    @FXML private Button refreshButton;
    @FXML private Button cancelButton;
    @FXML private Button importButton;

    // ========================================================================
    // STATE VARIABLES
    // ========================================================================

    private Stage dialogStage;
    private File csvFile;
    private String entityType;
    private Map<String, String> columnMapping;
    private List<Map<String, String>> allData;
    private List<Map<String, String>> validData;
    private List<Map<String, String>> errorData;
    private Map<Integer, List<String>> validationErrors;
    private boolean importConfirmed = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing Import Preview Controller");

        // Populate preview limit combo
        previewLimitCombo.getItems().addAll("10", "50", "100", "500", "All");
        previewLimitCombo.setValue("100");

        setupFilterListeners();
    }

    /**
     * Setup filter radio button listeners
     */
    private void setupFilterListeners() {
        showAllRadio.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) updatePreview();
        });
        showValidRadio.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) updatePreview();
        });
        showErrorsRadio.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) updatePreview();
        });

        previewLimitCombo.valueProperty().addListener((obs, old, newVal) -> updatePreview());
    }

    /**
     * Initialize preview with data
     */
    public void initializePreview(File csvFile, String entityType,
                                  Map<String, String> columnMapping,
                                  List<Map<String, String>> data,
                                  Map<Integer, List<String>> validationErrors) {
        this.csvFile = csvFile;
        this.entityType = entityType;
        this.columnMapping = columnMapping;
        this.allData = data;
        this.validationErrors = validationErrors;

        // Split data into valid and error sets
        this.validData = new ArrayList<>();
        this.errorData = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            if (validationErrors.containsKey(i)) {
                errorData.add(data.get(i));
            } else {
                validData.add(data.get(i));
            }
        }

        // Update UI
        fileNameLabel.setText(csvFile.getName());
        entityTypeLabel.setText(entityType);
        totalRowsLabel.setText(String.valueOf(data.size()));
        validRowsLabel.setText(String.valueOf(validData.size()));
        errorRowsLabel.setText(String.valueOf(errorData.size()));

        // Setup table columns
        setupTableColumns();

        // Show validation issues if any
        if (!errorData.isEmpty()) {
            displayValidationIssues();
            validationIssuesBox.setVisible(true);
            validationIssuesBox.setManaged(true);
        }

        // Enable import button if there's valid data
        importButton.setDisable(validData.isEmpty());

        // Update preview
        updatePreview();
    }

    // ========================================================================
    // TABLE SETUP
    // ========================================================================

    /**
     * Setup table columns based on mapped fields
     */
    private void setupTableColumns() {
        previewTable.getColumns().clear();

        // Add row number column
        TableColumn<Map<String, String>, String> rowNumCol = new TableColumn<>("#");
        rowNumCol.setPrefWidth(50);
        rowNumCol.setCellValueFactory(data -> {
            int index = previewTable.getItems().indexOf(data.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });
        previewTable.getColumns().add(rowNumCol);

        // Add status column
        TableColumn<Map<String, String>, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(80);
        statusCol.setCellValueFactory(data -> {
            int index = allData.indexOf(data.getValue());
            return new SimpleStringProperty(validationErrors.containsKey(index) ? "❌ Error" : "✓ Valid");
        });
        statusCol.setCellFactory(col -> new TableCell<Map<String, String>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("✓")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    }
                }
            }
        });
        previewTable.getColumns().add(statusCol);

        // Add columns for each mapped field
        for (Map.Entry<String, String> entry : columnMapping.entrySet()) {
            String csvColumn = entry.getKey();
            String dbField = entry.getValue();

            TableColumn<Map<String, String>, String> column = new TableColumn<>(dbField);
            column.setPrefWidth(120);
            column.setCellValueFactory(data -> {
                String value = data.getValue().get(csvColumn);
                return new SimpleStringProperty(value != null ? value : "");
            });

            previewTable.getColumns().add(column);
        }
    }

    // ========================================================================
    // PREVIEW UPDATE
    // ========================================================================

    /**
     * Update preview table based on selected filter
     */
    private void updatePreview() {
        List<Map<String, String>> dataToShow;

        if (showValidRadio.isSelected()) {
            dataToShow = validData;
        } else if (showErrorsRadio.isSelected()) {
            dataToShow = errorData;
        } else {
            dataToShow = allData;
        }

        // Apply limit
        String limitStr = previewLimitCombo.getValue();
        int limit = "All".equals(limitStr) ? dataToShow.size() : Integer.parseInt(limitStr);
        List<Map<String, String>> limitedData = dataToShow.stream()
            .limit(limit)
            .collect(Collectors.toList());

        previewTable.setItems(FXCollections.observableArrayList(limitedData));
        rowCountLabel.setText(String.format("Showing %d of %d rows", limitedData.size(), dataToShow.size()));
    }

    // ========================================================================
    // VALIDATION ISSUES DISPLAY
    // ========================================================================

    /**
     * Display validation issues in the issues container
     */
    private void displayValidationIssues() {
        issuesContainer.getChildren().clear();

        // Group errors by type
        Map<String, Integer> errorCounts = new HashMap<>();

        for (List<String> errors : validationErrors.values()) {
            for (String error : errors) {
                errorCounts.put(error, errorCounts.getOrDefault(error, 0) + 1);
            }
        }

        // Display summary
        for (Map.Entry<String, Integer> entry : errorCounts.entrySet()) {
            Label errorLabel = new Label(String.format("• %s (%d occurrences)",
                entry.getKey(), entry.getValue()));
            errorLabel.setStyle("-fx-text-fill: #d32f2f;");
            issuesContainer.getChildren().add(errorLabel);
        }

        // Add details link
        if (validationErrors.size() > 0) {
            Hyperlink detailsLink = new Hyperlink("View detailed errors →");
            detailsLink.setOnAction(e -> showDetailedErrors());
            issuesContainer.getChildren().add(detailsLink);
        }
    }

    /**
     * Show detailed error dialog
     */
    private void showDetailedErrors() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Errors");
        alert.setHeaderText("Detailed Error Report");

        StringBuilder content = new StringBuilder();
        for (Map.Entry<Integer, List<String>> entry : validationErrors.entrySet()) {
            content.append(String.format("Row %d:\n", entry.getKey() + 1));
            for (String error : entry.getValue()) {
                content.append(String.format("  • %s\n", error));
            }
            content.append("\n");
        }

        TextArea textArea = new TextArea(content.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(600, 400);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    // ========================================================================
    // ACTIONS
    // ========================================================================

    /**
     * Go back to column mapping
     */
    @FXML
    private void handleBack() {
        importConfirmed = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Refresh preview
     */
    @FXML
    private void handleRefresh() {
        updatePreview();
        showInfo("Preview Refreshed", "Preview data has been refreshed.");
    }

    /**
     * Cancel import
     */
    @FXML
    private void handleCancel() {
        importConfirmed = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Start import
     */
    @FXML
    private void handleImport() {
        // Confirm import
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Import");
        alert.setHeaderText("Ready to Import Data");

        String message = String.format(
            "You are about to import %d rows.\n\n" +
            "Valid rows: %d\n" +
            "Error rows: %d\n\n" +
            "Options:\n" +
            "• Skip errors: %s\n" +
            "• Create backup: %s\n" +
            "• Check duplicates: %s\n\n" +
            "Do you want to continue?",
            allData.size(),
            validData.size(),
            errorData.size(),
            skipErrorsCheckBox.isSelected() ? "Yes" : "No",
            createBackupCheckBox.isSelected() ? "Yes" : "No",
            validateDuplicatesCheckBox.isSelected() ? "Yes" : "No"
        );

        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            importConfirmed = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        }
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public boolean isImportConfirmed() {
        return importConfirmed;
    }

    public boolean shouldSkipErrors() {
        return skipErrorsCheckBox.isSelected();
    }

    public boolean shouldCreateBackup() {
        return createBackupCheckBox.isSelected();
    }

    public boolean shouldCheckDuplicates() {
        return validateDuplicatesCheckBox.isSelected();
    }

    public List<Map<String, String>> getValidData() {
        return validData;
    }

    public List<Map<String, String>> getAllData() {
        return allData;
    }

    // ========================================================================
    // SETTERS
    // ========================================================================

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    // ========================================================================
    // ALERT HELPERS
    // ========================================================================

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
