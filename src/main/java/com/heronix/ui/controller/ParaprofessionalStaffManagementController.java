package com.heronix.ui.controller;

import com.heronix.model.domain.Paraprofessional;
import com.heronix.service.ParaprofessionalManagementService;
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
 * Controller for Paraprofessional Staff Management UI
 * Manages permanent paraprofessional staff members (not substitutes)
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-11-07
 */
@Controller
public class ParaprofessionalStaffManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ParaprofessionalStaffManagementController.class);

    @Autowired
    private ParaprofessionalManagementService paraprofessionalManagementService;

    // Paraprofessionals Tab
    @FXML private TextField paraprofessionalSearchField;
    @FXML private TableView<Paraprofessional> paraprofessionalsTable;
    @FXML private TableColumn<Paraprofessional, String> paraNameColumn;
    @FXML private TableColumn<Paraprofessional, String> paraEmployeeIdColumn;
    @FXML private TableColumn<Paraprofessional, String> paraRoleTypeColumn;
    @FXML private TableColumn<Paraprofessional, String> paraSkillsColumn;
    @FXML private TableColumn<Paraprofessional, String> paraCertificationsColumn;
    @FXML private TableColumn<Paraprofessional, String> paraStatusColumn;
    @FXML private TableColumn<Paraprofessional, Void> paraActionsColumn;
    @FXML private Label paraprofessionalCountLabel;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Paraprofessional Staff Management Controller");
        setupParaprofessionalsTab();
        loadParaprofessionals();
    }

    /**
     * Setup Paraprofessionals Tab
     */
    private void setupParaprofessionalsTab() {
        paraNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));

        paraEmployeeIdColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        paraRoleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("roleType"));

        paraSkillsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getSkillsDisplay()));

        paraCertificationsColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCertificationsDisplay()));

        paraStatusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getActive() ? "Active" : "Inactive"));

        // Add actions column
        addParaprofessionalActionsColumn();
    }

    /**
     * Add actions column to paraprofessionals table
     */
    private void addParaprofessionalActionsColumn() {
        paraActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button viewButton = new Button("View");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");
                viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");

                editButton.setOnAction(event -> {
                    Paraprofessional paraprofessional = getTableView().getItems().get(getIndex());
                    handleEditParaprofessional(paraprofessional);
                });

                viewButton.setOnAction(event -> {
                    Paraprofessional paraprofessional = getTableView().getItems().get(getIndex());
                    handleViewParaprofessional(paraprofessional);
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
     * Load paraprofessionals
     */
    private void loadParaprofessionals() {
        List<Paraprofessional> paraprofessionals = paraprofessionalManagementService.getAllParaprofessionals();
        paraprofessionalsTable.setItems(FXCollections.observableArrayList(paraprofessionals));
        paraprofessionalCountLabel.setText("Total: " + paraprofessionals.size() + " paraprofessionals");
        logger.info("Loaded {} paraprofessionals", paraprofessionals.size());
    }

    /**
     * Handle search paraprofessionals
     */
    @FXML
    private void handleSearchParaprofessionals() {
        String query = paraprofessionalSearchField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadParaprofessionals();
            return;
        }

        List<Paraprofessional> results = paraprofessionalManagementService.searchParaprofessionalsByName(query.trim());
        paraprofessionalsTable.setItems(FXCollections.observableArrayList(results));
        paraprofessionalCountLabel.setText("Found: " + results.size() + " paraprofessionals");
    }

    /**
     * Handle add paraprofessional
     */
    @FXML
    private void handleAddParaprofessional() {
        Dialog<Paraprofessional> dialog = createParaprofessionalDialog(null);
        Optional<Paraprofessional> result = dialog.showAndWait();

        result.ifPresent(paraprofessional -> {
            try {
                Paraprofessional saved = paraprofessionalManagementService.saveParaprofessional(paraprofessional);
                showInfo("Success", "Paraprofessional " + saved.getFullName() + " added successfully!");
                loadParaprofessionals();
            } catch (Exception e) {
                logger.error("Error adding paraprofessional", e);
                showError("Error", "Failed to add paraprofessional: " + e.getMessage());
            }
        });
    }

    /**
     * Handle refresh paraprofessionals
     */
    @FXML
    private void handleRefreshParaprofessionals() {
        paraprofessionalSearchField.clear();
        loadParaprofessionals();
    }

    /**
     * Handle edit paraprofessional
     */
    private void handleEditParaprofessional(Paraprofessional paraprofessional) {
        Dialog<Paraprofessional> dialog = createParaprofessionalDialog(paraprofessional);
        Optional<Paraprofessional> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            try {
                Paraprofessional saved = paraprofessionalManagementService.saveParaprofessional(updated);
                showInfo("Success", "Paraprofessional " + saved.getFullName() + " updated successfully!");
                loadParaprofessionals();
            } catch (Exception e) {
                logger.error("Error updating paraprofessional", e);
                showError("Error", "Failed to update paraprofessional: " + e.getMessage());
            }
        });
    }

    /**
     * Handle view paraprofessional
     */
    private void handleViewParaprofessional(Paraprofessional paraprofessional) {
        showInfo("View Paraprofessional", "Viewing details for: " + paraprofessional.getFullName());
    }

    /**
     * Create paraprofessional dialog for add/edit operations
     */
    private Dialog<Paraprofessional> createParaprofessionalDialog(Paraprofessional existingPara) {
        Dialog<Paraprofessional> dialog = new Dialog<>();
        dialog.setTitle(existingPara == null ? "Add New Paraprofessional" : "Edit Paraprofessional");
        dialog.setHeaderText(existingPara == null ? "Enter paraprofessional information" : "Modify paraprofessional information");

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

        TextField roleTypeField = new TextField();
        roleTypeField.setPromptText("e.g., Special Ed Aide, Classroom Aide, 1:1 Aide");

        TextField assignmentTypeField = new TextField();
        assignmentTypeField.setPromptText("e.g., Student-Specific, Classroom Support, Roving");

        TextField maxStudentsField = new TextField();
        maxStudentsField.setPromptText("1");

        TextField preferredGradesField = new TextField();
        preferredGradesField.setPromptText("e.g., K-5, 6-8");

        TextField workScheduleField = new TextField();
        workScheduleField.setPromptText("e.g., Full Day, Morning Only, Afternoon Only");

        TextArea skillsArea = new TextArea();
        skillsArea.setPromptText("Enter specialized skills (one per line)");
        skillsArea.setPrefRowCount(3);

        TextArea certificationsArea = new TextArea();
        certificationsArea.setPromptText("Enter certifications (one per line)");
        certificationsArea.setPrefRowCount(3);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes, availability, preferences");
        notesArea.setPrefRowCount(3);

        CheckBox medicalTrainingCheckBox = new CheckBox("Medical Training");
        medicalTrainingCheckBox.setSelected(false);

        CheckBox behavioralTrainingCheckBox = new CheckBox("Behavioral Training");
        behavioralTrainingCheckBox.setSelected(false);

        CheckBox activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);

        // Populate if editing
        if (existingPara != null) {
            firstNameField.setText(existingPara.getFirstName());
            lastNameField.setText(existingPara.getLastName());
            employeeIdField.setText(existingPara.getEmployeeId());
            emailField.setText(existingPara.getEmail());
            phoneField.setText(existingPara.getPhoneNumber());
            roleTypeField.setText(existingPara.getRoleType());
            assignmentTypeField.setText(existingPara.getAssignmentType());
            if (existingPara.getMaxStudents() != null) {
                maxStudentsField.setText(String.valueOf(existingPara.getMaxStudents()));
            }
            preferredGradesField.setText(existingPara.getPreferredGrades());
            workScheduleField.setText(existingPara.getWorkSchedule());
            if (existingPara.getSpecializedSkills() != null && !existingPara.getSpecializedSkills().isEmpty()) {
                skillsArea.setText(String.join("\n", existingPara.getSpecializedSkills()));
            }
            if (existingPara.getCertifications() != null && !existingPara.getCertifications().isEmpty()) {
                certificationsArea.setText(String.join("\n", existingPara.getCertifications()));
            }
            notesArea.setText(existingPara.getNotes());
            medicalTrainingCheckBox.setSelected(Boolean.TRUE.equals(existingPara.getMedicalTraining()));
            behavioralTrainingCheckBox.setSelected(Boolean.TRUE.equals(existingPara.getBehavioralTraining()));
            activeCheckBox.setSelected(existingPara.getActive());
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

        grid.add(new Label("Role Type:"), 0, row);
        grid.add(roleTypeField, 1, row);
        row++;

        grid.add(new Label("Assignment Type:"), 0, row);
        grid.add(assignmentTypeField, 1, row);
        row++;

        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row);
        row++;

        grid.add(new Label("Phone:"), 0, row);
        grid.add(phoneField, 1, row);
        row++;

        grid.add(new Label("Max Students:"), 0, row);
        grid.add(maxStudentsField, 1, row);
        row++;

        grid.add(new Label("Preferred Grades:"), 0, row);
        grid.add(preferredGradesField, 1, row);
        row++;

        grid.add(new Label("Work Schedule:"), 0, row);
        grid.add(workScheduleField, 1, row);
        row++;

        grid.add(new Label("Specialized Skills:"), 0, row);
        grid.add(skillsArea, 1, row);
        row++;

        grid.add(new Label("Certifications:"), 0, row);
        grid.add(certificationsArea, 1, row);
        row++;

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row);
        row++;

        grid.add(medicalTrainingCheckBox, 1, row);
        row++;

        grid.add(behavioralTrainingCheckBox, 1, row);
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
                    Paraprofessional para = existingPara != null ? existingPara : new Paraprofessional();

                    para.setFirstName(firstNameField.getText().trim());
                    para.setLastName(lastNameField.getText().trim());
                    para.setEmployeeId(employeeIdField.getText().trim().isEmpty() ? null : employeeIdField.getText().trim());
                    para.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                    para.setPhoneNumber(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
                    para.setRoleType(roleTypeField.getText().trim().isEmpty() ? null : roleTypeField.getText().trim());
                    para.setAssignmentType(assignmentTypeField.getText().trim().isEmpty() ? null : assignmentTypeField.getText().trim());
                    para.setPreferredGrades(preferredGradesField.getText().trim().isEmpty() ? null : preferredGradesField.getText().trim());
                    para.setWorkSchedule(workScheduleField.getText().trim().isEmpty() ? null : workScheduleField.getText().trim());

                    // Parse max students
                    if (!maxStudentsField.getText().trim().isEmpty()) {
                        try {
                            para.setMaxStudents(Integer.parseInt(maxStudentsField.getText().trim()));
                        } catch (NumberFormatException e) {
                            // Invalid number, skip
                        }
                    }

                    // Parse specialized skills
                    if (!skillsArea.getText().trim().isEmpty()) {
                        Set<String> skills = new HashSet<>();
                        for (String line : skillsArea.getText().split("\n")) {
                            if (!line.trim().isEmpty()) {
                                skills.add(line.trim());
                            }
                        }
                        para.setSpecializedSkills(skills);
                    }

                    // Parse certifications
                    if (!certificationsArea.getText().trim().isEmpty()) {
                        Set<String> certs = new HashSet<>();
                        for (String line : certificationsArea.getText().split("\n")) {
                            if (!line.trim().isEmpty()) {
                                certs.add(line.trim());
                            }
                        }
                        para.setCertifications(certs);
                    }

                    para.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                    para.setMedicalTraining(medicalTrainingCheckBox.isSelected());
                    para.setBehavioralTraining(behavioralTrainingCheckBox.isSelected());
                    para.setActive(activeCheckBox.isSelected());

                    return para;
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
