package com.heronix.ui.controller;

import com.heronix.model.domain.ImportTemplate;
import com.heronix.repository.ImportTemplateRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Column Mapping Controller
 * Provides visual interface for mapping CSV columns to database fields
 *
 * Features:
 * - Drag-and-drop column mapping
 * - Skyward/PowerSchool templates
 * - Auto-detection of field mappings
 * - Validation before import
 * - Sample data preview
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/ColumnMappingController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
@Component
public class ColumnMappingController {

    // ========================================================================
    // FXML FIELDS
    // ========================================================================

    @FXML private Label fileNameLabel;
    @FXML private Label entityTypeLabel;

    @FXML private Button skywardTemplateBtn;
    @FXML private Button powerSchoolTemplateBtn;
    @FXML private Button customTemplateBtn;
    @FXML private Button autoDetectBtn;

    @FXML private TableView<ColumnMapping> mappingTable;
    @FXML private TableColumn<ColumnMapping, String> csvColumnCol;
    @FXML private TableColumn<ColumnMapping, String> dbFieldCol;
    @FXML private TableColumn<ColumnMapping, String> sampleDataCol;
    @FXML private TableColumn<ColumnMapping, String> statusCol;

    @FXML private Label totalColumnsLabel;
    @FXML private Label mappedColumnsLabel;
    @FXML private Label unmappedColumnsLabel;
    @FXML private Label requiredFieldsLabel;

    @FXML private VBox validationBox;
    @FXML private TextArea validationTextArea;

    @FXML private Button resetButton;
    @FXML private Button validateButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private ImportTemplateRepository importTemplateRepository;

    // ========================================================================
    // STATE VARIABLES
    // ========================================================================

    private Stage dialogStage;
    private File csvFile;
    private String entityType;
    private List<String> csvHeaders;
    private List<List<String>> csvSampleData;
    private ObservableList<ColumnMapping> mappings = FXCollections.observableArrayList();
    private Map<String, String> finalMapping = new HashMap<>();
    private boolean mappingConfirmed = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing Column Mapping Controller");
        setupTable();
    }

    /**
     * Setup table columns and cell factories
     */
    private void setupTable() {
        // CSV Column - non-editable
        csvColumnCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCsvColumn()));

        // Database Field - editable combo box
        dbFieldCol.setCellValueFactory(data -> data.getValue().dbFieldProperty());
        dbFieldCol.setCellFactory(col -> {
            ComboBoxTableCell<ColumnMapping, String> cell = new ComboBoxTableCell<>();
            cell.getItems().addAll(getAvailableDbFields());
            cell.setComboBoxEditable(true);
            return cell;
        });
        dbFieldCol.setOnEditCommit(event -> {
            event.getRowValue().setDbField(event.getNewValue());
            updateStatistics();
            validateMapping();
        });

        // Sample Data - non-editable
        sampleDataCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getSampleData()));

        // Status - color-coded
        statusCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<ColumnMapping, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "✓ Mapped":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "⚠ Unmapped":
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                            break;
                        case "✗ Required":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        mappingTable.setItems(mappings);
        mappingTable.setEditable(true);
    }

    /**
     * Initialize mapping dialog with CSV data
     */
    public void initializeMapping(File csvFile, String entityType,
                                  List<String> headers, List<List<String>> sampleData) {
        this.csvFile = csvFile;
        this.entityType = entityType;
        this.csvHeaders = headers;
        this.csvSampleData = sampleData;

        fileNameLabel.setText(csvFile.getName());
        entityTypeLabel.setText(entityType);

        // Create mappings for each CSV column
        mappings.clear();
        for (int i = 0; i < headers.size(); i++) {
            String csvColumn = headers.get(i);
            String sampleDataStr = getSampleDataString(i);
            mappings.add(new ColumnMapping(csvColumn, "", sampleDataStr));
        }

        // Try auto-detection
        autoDetectMapping();

        updateStatistics();
    }

    // ========================================================================
    // TEMPLATE ACTIONS
    // ========================================================================

    /**
     * Apply Skyward import template
     */
    @FXML
    private void applySkywardTemplate() {
        log.info("Applying Skyward template for entity: {}", entityType);

        Map<String, String> template = getSkywardTemplate(entityType);
        applyTemplate(template);
    }

    /**
     * Apply PowerSchool import template
     */
    @FXML
    private void applyPowerSchoolTemplate() {
        log.info("Applying PowerSchool template for entity: {}", entityType);

        Map<String, String> template = getPowerSchoolTemplate(entityType);
        applyTemplate(template);
    }

    /**
     * Apply custom template - Shows dialog with Load/Save options
     */
    @FXML
    private void applyCustomTemplate() {
        log.info("Opening custom template dialog for entity: {}", entityType);

        // Create choice dialog for Load or Save
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Custom Templates");
        dialog.setHeaderText("Manage Custom Import Templates");

        ButtonType loadButtonType = new ButtonType("Load Template", ButtonBar.ButtonData.LEFT);
        ButtonType saveButtonType = new ButtonType("Save Current Mapping", ButtonBar.ButtonData.RIGHT);
        dialog.getDialogPane().getButtonTypes().addAll(loadButtonType, saveButtonType, ButtonType.CANCEL);

        // Create content with info
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        Label infoLabel = new Label("Choose an action:");
        Label loadInfo = new Label("Load Template: Apply a previously saved custom template");
        Label saveInfo = new Label("Save Mapping: Save current column mappings as a new template");
        loadInfo.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        saveInfo.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        grid.add(infoLabel, 0, 0);
        grid.add(loadInfo, 0, 1);
        grid.add(saveInfo, 0, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loadButtonType) {
                return "LOAD";
            } else if (dialogButton == saveButtonType) {
                return "SAVE";
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(action -> {
            if ("LOAD".equals(action)) {
                showLoadTemplateDialog();
            } else if ("SAVE".equals(action)) {
                showSaveTemplateDialog();
            }
        });
    }

    /**
     * Show dialog to load an existing template
     */
    private void showLoadTemplateDialog() {
        List<ImportTemplate> templates = importTemplateRepository.findByEntityTypeOrderByNameAsc(entityType);

        if (templates.isEmpty()) {
            showInfo("No Templates", "No custom templates found for " + entityType + ".\n\nCreate one by saving your current mapping.");
            return;
        }

        ChoiceDialog<ImportTemplate> dialog = new ChoiceDialog<>(templates.get(0), templates);
        dialog.setTitle("Load Template");
        dialog.setHeaderText("Select a template to load");
        dialog.setContentText("Template:");

        // Custom display
        dialog.getItems().clear();
        dialog.getItems().addAll(templates);

        Optional<ImportTemplate> result = dialog.showAndWait();
        result.ifPresent(template -> {
            // Apply the template mapping
            Map<String, String> templateMapping = template.getColumnMapping();

            for (ColumnMapping mapping : mappings) {
                String csvColumn = mapping.getCsvColumn();
                // Template stores: dbField -> csvColumn, so we need to reverse lookup
                for (Map.Entry<String, String> entry : templateMapping.entrySet()) {
                    if (entry.getValue().equals(csvColumn)) {
                        mapping.setDbField(entry.getKey());
                        break;
                    }
                }
            }

            // Update last used date
            template.setLastUsedDate(new Date());
            importTemplateRepository.save(template);

            updateStatistics();
            showInfo("Template Loaded", "Template '" + template.getName() + "' applied successfully.");
            log.info("Loaded custom template: {}", template.getName());
        });
    }

    /**
     * Show dialog to save current mapping as a template
     */
    private void showSaveTemplateDialog() {
        // Check if there are any mapped columns
        long mappedCount = mappings.stream()
            .filter(m -> m.getDbField() != null && !m.getDbField().isEmpty())
            .count();

        if (mappedCount == 0) {
            showInfo("No Mappings", "Please map at least one column before saving a template.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Save Template");
        dialog.setHeaderText("Save Current Mapping as Template");
        dialog.setContentText("Template name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                showInfo("Invalid Name", "Please enter a template name.");
                return;
            }

            // Check if name already exists
            if (importTemplateRepository.existsByName(name.trim())) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Template Exists");
                confirm.setHeaderText("A template with this name already exists");
                confirm.setContentText("Do you want to overwrite it?");

                Optional<ButtonType> confirmResult = confirm.showAndWait();
                if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
                    return;
                }
            }

            try {
                // Build mapping (dbField -> csvColumn)
                Map<String, String> templateMapping = new HashMap<>();
                for (ColumnMapping mapping : mappings) {
                    if (mapping.getDbField() != null && !mapping.getDbField().isEmpty()) {
                        templateMapping.put(mapping.getDbField(), mapping.getCsvColumn());
                    }
                }

                // Create or update template
                ImportTemplate template = importTemplateRepository.findByName(name.trim())
                    .orElse(new ImportTemplate());

                template.setName(name.trim());
                template.setEntityType(entityType);
                template.setColumnMapping(templateMapping);
                if (template.getCreatedDate() == null) {
                    template.setCreatedDate(new Date());
                }
                template.setLastUsedDate(new Date());

                importTemplateRepository.save(template);

                showInfo("Template Saved", "Template '" + name.trim() + "' saved successfully.\n\n" +
                    "Saved " + templateMapping.size() + " column mappings.");
                log.info("Saved custom template: {} with {} mappings", name.trim(), templateMapping.size());

            } catch (Exception e) {
                log.error("Failed to save template", e);
                showInfo("Error", "Failed to save template: " + e.getMessage());
            }
        });
    }

    /**
     * Auto-detect column mappings
     */
    @FXML
    private void autoDetectMapping() {
        log.info("Auto-detecting column mappings");

        for (ColumnMapping mapping : mappings) {
            String csvColumn = mapping.getCsvColumn().toLowerCase().trim();
            String detectedField = detectDbField(csvColumn);

            if (detectedField != null) {
                mapping.setDbField(detectedField);
            }
        }

        updateStatistics();
        showInfo("Auto-Detection", "Auto-detection complete. Please review and adjust mappings as needed.");
    }

    /**
     * Apply template mapping
     */
    private void applyTemplate(Map<String, String> template) {
        for (ColumnMapping mapping : mappings) {
            String csvColumn = mapping.getCsvColumn();
            String dbField = template.get(csvColumn);

            if (dbField != null) {
                mapping.setDbField(dbField);
            }
        }

        updateStatistics();
        showInfo("Template Applied", "Template mapping applied successfully. Please review the mappings.");
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate current mapping
     */
    @FXML
    private void handleValidate() {
        validateMapping();
    }

    /**
     * Validate the mapping configuration
     */
    private boolean validateMapping() {
        List<String> issues = new ArrayList<>();

        // Check for required fields
        Set<String> requiredFields = getRequiredFields(entityType);
        Set<String> mappedFields = mappings.stream()
            .map(ColumnMapping::getDbField)
            .filter(f -> f != null && !f.isEmpty())
            .collect(Collectors.toSet());

        for (String required : requiredFields) {
            if (!mappedFields.contains(required)) {
                issues.add("Required field '" + required + "' is not mapped");
            }
        }

        // Check for duplicate mappings
        Map<String, Long> fieldCounts = mappings.stream()
            .map(ColumnMapping::getDbField)
            .filter(f -> f != null && !f.isEmpty())
            .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        for (Map.Entry<String, Long> entry : fieldCounts.entrySet()) {
            if (entry.getValue() > 1) {
                issues.add("Field '" + entry.getKey() + "' is mapped multiple times");
            }
        }

        // Update UI
        if (issues.isEmpty()) {
            validationBox.setVisible(false);
            validationBox.setManaged(false);
            saveButton.setDisable(false);
            return true;
        } else {
            validationBox.setVisible(true);
            validationBox.setManaged(true);
            validationTextArea.setText(String.join("\n", issues));
            saveButton.setDisable(true);
            return false;
        }
    }

    // ========================================================================
    // ACTIONS
    // ========================================================================

    /**
     * Reset all mappings
     */
    @FXML
    private void handleReset() {
        for (ColumnMapping mapping : mappings) {
            mapping.setDbField("");
        }
        updateStatistics();
        validationBox.setVisible(false);
        validationBox.setManaged(false);
    }

    /**
     * Cancel mapping
     */
    @FXML
    private void handleCancel() {
        mappingConfirmed = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Save and continue
     */
    @FXML
    private void handleSave() {
        if (validateMapping()) {
            // Build final mapping
            finalMapping.clear();
            for (ColumnMapping mapping : mappings) {
                if (mapping.getDbField() != null && !mapping.getDbField().isEmpty()) {
                    finalMapping.put(mapping.getCsvColumn(), mapping.getDbField());
                }
            }

            mappingConfirmed = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Update statistics labels
     */
    private void updateStatistics() {
        int total = mappings.size();
        int mapped = (int) mappings.stream()
            .filter(m -> m.getDbField() != null && !m.getDbField().isEmpty())
            .count();
        int unmapped = total - mapped;

        Set<String> requiredFields = getRequiredFields(entityType);
        Set<String> mappedRequiredFields = mappings.stream()
            .filter(m -> m.getDbField() != null && requiredFields.contains(m.getDbField()))
            .map(ColumnMapping::getDbField)
            .collect(Collectors.toSet());

        totalColumnsLabel.setText("Total Columns: " + total);
        mappedColumnsLabel.setText("Mapped: " + mapped);
        unmappedColumnsLabel.setText("Unmapped: " + unmapped);
        requiredFieldsLabel.setText(String.format("Required Fields: %d/%d",
            mappedRequiredFields.size(), requiredFields.size()));

        // Update status for each mapping
        for (ColumnMapping mapping : mappings) {
            if (mapping.getDbField() == null || mapping.getDbField().isEmpty()) {
                boolean isRequired = requiredFields.contains(mapping.getCsvColumn());
                mapping.setStatus(isRequired ? "✗ Required" : "⚠ Unmapped");
            } else {
                mapping.setStatus("✓ Mapped");
            }
        }
    }

    /**
     * Get sample data string for column
     */
    private String getSampleDataString(int columnIndex) {
        if (csvSampleData == null || csvSampleData.isEmpty()) {
            return "";
        }

        List<String> samples = new ArrayList<>();
        for (int i = 0; i < Math.min(3, csvSampleData.size()); i++) {
            List<String> row = csvSampleData.get(i);
            if (columnIndex < row.size()) {
                String value = row.get(columnIndex);
                if (value != null && !value.isEmpty()) {
                    samples.add(value);
                }
            }
        }

        return String.join(", ", samples);
    }

    /**
     * Detect database field from CSV column name
     */
    private String detectDbField(String csvColumn) {
        // Common mappings
        Map<String, String> commonMappings = new HashMap<>();

        // Student fields
        commonMappings.put("student id", "studentId");
        commonMappings.put("student_id", "studentId");
        commonMappings.put("first name", "firstName");
        commonMappings.put("first_name", "firstName");
        commonMappings.put("last name", "lastName");
        commonMappings.put("last_name", "lastName");
        commonMappings.put("grade", "gradeLevel");
        commonMappings.put("grade level", "gradeLevel");
        commonMappings.put("email", "email");
        commonMappings.put("phone", "phoneNumber");
        commonMappings.put("date of birth", "dateOfBirth");
        commonMappings.put("dob", "dateOfBirth");

        // Teacher fields
        commonMappings.put("employee id", "employeeId");
        commonMappings.put("teacher name", "name");
        commonMappings.put("department", "department");

        // Course fields
        commonMappings.put("course code", "courseCode");
        commonMappings.put("course name", "courseName");
        commonMappings.put("subject", "subject");
        commonMappings.put("credits", "credits");

        // Room fields
        commonMappings.put("room number", "roomNumber");
        commonMappings.put("capacity", "capacity");
        commonMappings.put("building", "building");

        return commonMappings.get(csvColumn);
    }

    /**
     * Get available database fields for entity type
     */
    private List<String> getAvailableDbFields() {
        List<String> fields = new ArrayList<>();
        fields.add(""); // Allow unmapped

        switch (entityType) {
            case "Student":
                fields.addAll(Arrays.asList(
                    "studentId", "firstName", "lastName", "gradeLevel",
                    "email", "phoneNumber", "dateOfBirth", "gender",
                    "medicalConditions", "medicationInfo", "emergencyContact", "emergencyPhone",
                    "hasIEP", "has504Plan", "accommodationNotes"
                ));
                break;
            case "Teacher":
                fields.addAll(Arrays.asList(
                    "employeeId", "name", "email", "phoneNumber", "department",
                    "specialAssignments"
                ));
                break;
            case "Course":
                fields.addAll(Arrays.asList(
                    "courseCode", "courseName", "subject", "credits", "gradeLevel",
                    "courseType", "description"
                ));
                break;
            case "Room":
                fields.addAll(Arrays.asList(
                    "roomNumber", "building", "capacity", "telephoneExtension",
                    "roomType", "hasProjector", "hasSmartboard"
                ));
                break;
        }

        return fields;
    }

    /**
     * Get required fields for entity type
     */
    private Set<String> getRequiredFields(String entityType) {
        Set<String> required = new HashSet<>();

        switch (entityType) {
            case "Student":
                required.addAll(Arrays.asList("firstName", "lastName", "gradeLevel"));
                break;
            case "Teacher":
                required.addAll(Arrays.asList("name"));
                break;
            case "Course":
                required.addAll(Arrays.asList("courseCode", "courseName"));
                break;
            case "Room":
                required.addAll(Arrays.asList("roomNumber"));
                break;
        }

        return required;
    }

    /**
     * Get Skyward template mapping
     */
    private Map<String, String> getSkywardTemplate(String entityType) {
        Map<String, String> template = new HashMap<>();

        switch (entityType) {
            case "Student":
                template.put("Student_ID", "studentId");
                template.put("First_Name", "firstName");
                template.put("Last_Name", "lastName");
                template.put("Grade", "gradeLevel");
                template.put("Email", "email");
                template.put("Phone", "phoneNumber");
                template.put("DOB", "dateOfBirth");
                template.put("IEP_Status", "hasIEP");
                template.put("Section_504", "has504Plan");
                template.put("Medical_Alert", "medicalConditions");
                template.put("Emergency_Contact", "emergencyContact");
                template.put("Emergency_Phone", "emergencyPhone");
                break;
            case "Teacher":
                template.put("Employee_ID", "employeeId");
                template.put("Name", "name");
                template.put("Department", "department");
                template.put("Email", "email");
                template.put("Phone", "phoneNumber");
                break;
            case "Course":
                template.put("Course_Code", "courseCode");
                template.put("Course_Name", "courseName");
                template.put("Subject", "subject");
                template.put("Credits", "credits");
                break;
            case "Room":
                template.put("Room_Number", "roomNumber");
                template.put("Building", "building");
                template.put("Capacity", "capacity");
                template.put("Phone_Ext", "telephoneExtension");
                break;
        }

        return template;
    }

    /**
     * Get PowerSchool template mapping
     */
    private Map<String, String> getPowerSchoolTemplate(String entityType) {
        Map<String, String> template = new HashMap<>();

        switch (entityType) {
            case "Student":
                template.put("Student Number", "studentId");
                template.put("First", "firstName");
                template.put("Last", "lastName");
                template.put("Grade Level", "gradeLevel");
                template.put("Student Email", "email");
                template.put("Phone", "phoneNumber");
                template.put("DOB", "dateOfBirth");
                break;
            case "Teacher":
                template.put("Teacher Number", "employeeId");
                template.put("Teacher Name", "name");
                template.put("Dept", "department");
                template.put("Teacher Email", "email");
                break;
            case "Course":
                template.put("Course Number", "courseCode");
                template.put("Course Title", "courseName");
                template.put("Department", "subject");
                template.put("Credit Hours", "credits");
                break;
            case "Room":
                template.put("Room", "roomNumber");
                template.put("Building", "building");
                template.put("Max Students", "capacity");
                break;
        }

        return template;
    }

    // ========================================================================
    // GETTERS/SETTERS
    // ========================================================================

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public Map<String, String> getMapping() {
        return finalMapping;
    }

    public boolean isMappingConfirmed() {
        return mappingConfirmed;
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

    // ========================================================================
    // INNER CLASS: Column Mapping
    // ========================================================================

    @Data
    public static class ColumnMapping {
        private final String csvColumn;
        private String dbField;
        private final String sampleData;
        private String status;

        public ColumnMapping(String csvColumn, String dbField, String sampleData) {
            this.csvColumn = csvColumn;
            this.dbField = dbField;
            this.sampleData = sampleData;
            this.status = "⚠ Unmapped";
        }

        public javafx.beans.property.StringProperty dbFieldProperty() {
            return new SimpleStringProperty(dbField);
        }
    }
}
