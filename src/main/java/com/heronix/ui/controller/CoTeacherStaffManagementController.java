package com.heronix.ui.controller;

import com.heronix.model.domain.CoTeacher;
import com.heronix.service.CoTeacherManagementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.*;

/**
 * Controller for Co-Teacher Staff Management UI
 * Manages permanent co-teacher staff members (not substitutes)
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-11-07
 */
@Controller
public class CoTeacherStaffManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CoTeacherStaffManagementController.class);

    @Autowired
    private CoTeacherManagementService coTeacherManagementService;

    // Co-Teachers Tab
    @FXML private TextField coTeacherSearchField;
    @FXML private TableView<CoTeacher> coTeachersTable;
    @FXML private TableColumn<CoTeacher, String> ctNameColumn;
    @FXML private TableColumn<CoTeacher, String> ctEmployeeIdColumn;
    @FXML private TableColumn<CoTeacher, String> ctSpecializationColumn;
    @FXML private TableColumn<CoTeacher, String> ctEmailColumn;
    @FXML private TableColumn<CoTeacher, String> ctPhoneColumn;
    @FXML private TableColumn<CoTeacher, String> ctCertificationsColumn;
    @FXML private TableColumn<CoTeacher, String> ctStatusColumn;
    @FXML private TableColumn<CoTeacher, Void> ctActionsColumn;
    @FXML private Label coTeacherCountLabel;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Co-Teacher Staff Management Controller");
        setupCoTeachersTab();
        loadCoTeachers();
    }

    /**
     * Setup Co-Teachers Tab
     */
    private void setupCoTeachersTab() {
        ctNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));

        ctEmployeeIdColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        ctSpecializationColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));

        ctEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        ctPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));

        ctCertificationsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCertificationsDisplay()));

        ctStatusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getActive() ? "Active" : "Inactive"));

        // Add actions column
        addCoTeacherActionsColumn();
    }

    /**
     * Add actions column to co-teachers table
     */
    private void addCoTeacherActionsColumn() {
        ctActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button viewButton = new Button("View");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");
                viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");

                editButton.setOnAction(event -> {
                    CoTeacher coTeacher = getTableView().getItems().get(getIndex());
                    handleEditCoTeacher(coTeacher);
                });

                viewButton.setOnAction(event -> {
                    CoTeacher coTeacher = getTableView().getItems().get(getIndex());
                    handleViewCoTeacher(coTeacher);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editButton, viewButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    /**
     * Load co-teachers
     */
    private void loadCoTeachers() {
        List<CoTeacher> coTeachers = coTeacherManagementService.getAllCoTeachers();
        coTeachersTable.setItems(FXCollections.observableArrayList(coTeachers));
        coTeacherCountLabel.setText("Total: " + coTeachers.size() + " co-teachers");
        logger.info("Loaded {} co-teachers", coTeachers.size());
    }

    /**
     * Handle search co-teachers
     */
    @FXML
    private void handleSearchCoTeachers() {
        String query = coTeacherSearchField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadCoTeachers();
            return;
        }

        List<CoTeacher> results = coTeacherManagementService.searchCoTeachersByName(query.trim());
        coTeachersTable.setItems(FXCollections.observableArrayList(results));
        coTeacherCountLabel.setText("Found: " + results.size() + " co-teachers");
    }

    /**
     * Handle add co-teacher
     */
    @FXML
    private void handleAddCoTeacher() {
        Dialog<CoTeacher> dialog = createCoTeacherDialog(null);
        Optional<CoTeacher> result = dialog.showAndWait();

        result.ifPresent(coTeacher -> {
            try {
                CoTeacher saved = coTeacherManagementService.saveCoTeacher(coTeacher);
                showInfo("Success", "Co-Teacher " + saved.getFullName() + " added successfully!");
                loadCoTeachers();
            } catch (Exception e) {
                logger.error("Error adding co-teacher", e);
                showError("Error", "Failed to add co-teacher: " + e.getMessage());
            }
        });
    }

    /**
     * Handle refresh co-teachers
     */
    @FXML
    private void handleRefreshCoTeachers() {
        coTeacherSearchField.clear();
        loadCoTeachers();
    }

    /**
     * Handle edit co-teacher
     */
    private void handleEditCoTeacher(CoTeacher coTeacher) {
        Dialog<CoTeacher> dialog = createCoTeacherDialog(coTeacher);
        Optional<CoTeacher> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            try {
                CoTeacher saved = coTeacherManagementService.saveCoTeacher(updated);
                showInfo("Success", "Co-Teacher " + saved.getFullName() + " updated successfully!");
                loadCoTeachers();
            } catch (Exception e) {
                logger.error("Error updating co-teacher", e);
                showError("Error", "Failed to update co-teacher: " + e.getMessage());
            }
        });
    }

    /**
     * Handle view co-teacher
     */
    private void handleViewCoTeacher(CoTeacher coTeacher) {
        showInfo("View Co-Teacher", "Viewing details for: " + coTeacher.getFullName());
    }

    /**
     * Create co-teacher dialog for add/edit operations
     */
    private Dialog<CoTeacher> createCoTeacherDialog(CoTeacher existingCT) {
        Dialog<CoTeacher> dialog = new Dialog<>();
        dialog.setTitle(existingCT == null ? "Add New Co-Teacher" : "Edit Co-Teacher");
        dialog.setHeaderText(existingCT == null ? "Enter co-teacher information" : "Modify co-teacher information");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First name");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last name");

        TextField employeeIdField = new TextField();
        employeeIdField.setPromptText("Employee ID (optional)");

        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com");

        TextField phoneField = new TextField();
        phoneField.setPromptText("(555) 123-4567");

        TextField specializationField = new TextField();
        specializationField.setPromptText("e.g., Special Education, ESL, Math Support");

        TextField supportLevelField = new TextField();
        supportLevelField.setPromptText("e.g., Full Time, Part Time");

        TextField maxClassesField = new TextField();
        maxClassesField.setPromptText("5");

        TextField preferredGradesField = new TextField();
        preferredGradesField.setPromptText("e.g., K-5, 6-8");

        TextField preferredSubjectsField = new TextField();
        preferredSubjectsField.setPromptText("e.g., Math, Science");

        TextArea certificationsArea = new TextArea();
        certificationsArea.setPromptText("Enter certifications (one per line)");
        certificationsArea.setPrefRowCount(3);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes, availability, preferences");
        notesArea.setPrefRowCount(3);

        CheckBox activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);

        // Populate if editing
        if (existingCT != null) {
            firstNameField.setText(existingCT.getFirstName());
            lastNameField.setText(existingCT.getLastName());
            employeeIdField.setText(existingCT.getEmployeeId());
            emailField.setText(existingCT.getEmail());
            phoneField.setText(existingCT.getPhoneNumber());
            specializationField.setText(existingCT.getSpecialization());
            supportLevelField.setText(existingCT.getSupportLevel());
            if (existingCT.getMaxClasses() != null) {
                maxClassesField.setText(String.valueOf(existingCT.getMaxClasses()));
            }
            preferredGradesField.setText(existingCT.getPreferredGrades());
            preferredSubjectsField.setText(existingCT.getPreferredSubjects());
            if (existingCT.getCertifications() != null && !existingCT.getCertifications().isEmpty()) {
                certificationsArea.setText(String.join("\n", existingCT.getCertifications()));
            }
            notesArea.setText(existingCT.getNotes());
            activeCheckBox.setSelected(existingCT.getActive());
        }

        // Add fields to grid
        int row = 0;
        grid.add(new Label("First Name: *"), 0, row);
        grid.add(firstNameField, 1, row);
        row++;

        grid.add(new Label("Last Name: *"), 0, row);
        grid.add(lastNameField, 1, row);
        row++;

        grid.add(new Label("Employee ID:"), 0, row);
        grid.add(employeeIdField, 1, row);
        row++;

        grid.add(new Label("Specialization:"), 0, row);
        grid.add(specializationField, 1, row);
        row++;

        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row);
        row++;

        grid.add(new Label("Phone:"), 0, row);
        grid.add(phoneField, 1, row);
        row++;

        grid.add(new Label("Support Level:"), 0, row);
        grid.add(supportLevelField, 1, row);
        row++;

        grid.add(new Label("Max Classes:"), 0, row);
        grid.add(maxClassesField, 1, row);
        row++;

        grid.add(new Label("Preferred Grades:"), 0, row);
        grid.add(preferredGradesField, 1, row);
        row++;

        grid.add(new Label("Preferred Subjects:"), 0, row);
        grid.add(preferredSubjectsField, 1, row);
        row++;

        grid.add(new Label("Certifications:"), 0, row);
        grid.add(certificationsArea, 1, row);
        row++;

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row);
        row++;

        grid.add(activeCheckBox, 1, row);

        dialog.getDialogPane().setContent(grid);

        // Validation and result converter
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Enable save button when required fields are filled
        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty() || lastNameField.getText().trim().isEmpty());
        });
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty() || firstNameField.getText().trim().isEmpty());
        });

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    CoTeacher ct = existingCT != null ? existingCT : new CoTeacher();

                    ct.setFirstName(firstNameField.getText().trim());
                    ct.setLastName(lastNameField.getText().trim());
                    ct.setEmployeeId(employeeIdField.getText().trim().isEmpty() ? null : employeeIdField.getText().trim());
                    ct.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                    ct.setPhoneNumber(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
                    ct.setSpecialization(specializationField.getText().trim().isEmpty() ? null : specializationField.getText().trim());
                    ct.setSupportLevel(supportLevelField.getText().trim().isEmpty() ? null : supportLevelField.getText().trim());
                    ct.setPreferredGrades(preferredGradesField.getText().trim().isEmpty() ? null : preferredGradesField.getText().trim());
                    ct.setPreferredSubjects(preferredSubjectsField.getText().trim().isEmpty() ? null : preferredSubjectsField.getText().trim());

                    // Parse max classes
                    if (!maxClassesField.getText().trim().isEmpty()) {
                        try {
                            ct.setMaxClasses(Integer.parseInt(maxClassesField.getText().trim()));
                        } catch (NumberFormatException e) {
                            // Invalid number, skip
                        }
                    }

                    // Parse certifications
                    if (!certificationsArea.getText().trim().isEmpty()) {
                        Set<String> certs = new HashSet<>();
                        for (String line : certificationsArea.getText().split("\n")) {
                            if (!line.trim().isEmpty()) {
                                certs.add(line.trim());
                            }
                        }
                        ct.setCertifications(certs);
                    }

                    ct.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                    ct.setActive(activeCheckBox.isSelected());

                    return ct;
                } catch (Exception e) {
                    logger.error("Error converting dialog result", e);
                    return null;
                }
            }
            return null;
        });

        // Request focus on first field
        javafx.application.Platform.runLater(() -> firstNameField.requestFocus());

        return dialog;
    }

    /**
     * Show error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
