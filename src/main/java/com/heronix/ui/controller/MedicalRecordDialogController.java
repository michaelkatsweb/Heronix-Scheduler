package com.heronix.ui.controller;

import com.heronix.model.domain.MedicalRecord;
import com.heronix.model.domain.MedicalRecord.AllergySeverity;
import com.heronix.model.domain.Student;
import com.heronix.repository.MedicalRecordRepository;
import com.heronix.repository.StudentRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * MedicalRecordDialogController - Comprehensive Medical Records Management
 * Location: src/main/java/com/eduscheduler/ui/controller/MedicalRecordDialogController.java
 *
 * Features:
 * - Complete medical record management for students
 * - Real-time validation and status indicators
 * - Interactive field enabling/disabling based on conditions
 * - Medical alert tracking and critical case identification
 * - Medication tracking with administration schedules
 * - Emergency action plan documentation
 * - Physician and insurance information
 * - Review tracking and verification
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0 - Medical Safety Enhancement
 * @since 2025-11-15
 */
@Component
public class MedicalRecordDialogController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordDialogController.class);

    // ========================================================================
    // REPOSITORIES
    // ========================================================================

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private StudentRepository studentRepository;

    // ========================================================================
    // HEADER SECTION
    // ========================================================================

    @FXML
    private Label studentNameLabel;

    @FXML
    private Label studentIdLabel;

    // ========================================================================
    // TAB PANE
    // ========================================================================

    @FXML
    private TabPane medicalTabPane;

    // ========================================================================
    // ALLERGIES TAB
    // ========================================================================

    @FXML
    private ComboBox<AllergySeverity> allergySeverityCombo;

    @FXML
    private Label severityDescriptionLabel;

    @FXML
    private TextArea foodAllergiesArea;

    @FXML
    private TextArea medicationAllergiesArea;

    @FXML
    private TextArea environmentalAllergiesArea;

    @FXML
    private CheckBox hasEpiPenCheck;

    @FXML
    private TextField epiPenLocationField;

    @FXML
    private CheckBox hasInhalerCheck;

    @FXML
    private TextField inhalerLocationField;

    // ========================================================================
    // CHRONIC CONDITIONS TAB
    // ========================================================================

    @FXML
    private CheckBox hasDiabetesCheck;

    @FXML
    private ComboBox<String> diabetesTypeCombo;

    @FXML
    private TextArea diabetesManagementArea;

    @FXML
    private CheckBox hasAsthmaCheck;

    @FXML
    private ComboBox<String> asthmaSeverityCombo;

    @FXML
    private TextArea asthmaTriggersArea;

    @FXML
    private CheckBox hasSeizureDisorderCheck;

    @FXML
    private TextField seizureTypeField;

    @FXML
    private TextArea seizureProtocolArea;

    @FXML
    private CheckBox hasHeartConditionCheck;

    @FXML
    private TextField heartConditionTypeField;

    @FXML
    private TextArea heartConditionNotesArea;

    @FXML
    private TextArea otherConditionsArea;

    // ========================================================================
    // MEDICATIONS TAB
    // ========================================================================

    @FXML
    private TextArea currentMedicationsArea;

    @FXML
    private TextArea medicationScheduleArea;

    @FXML
    private CheckBox selfAdministersCheck;

    @FXML
    private TextArea medicationInstructionsArea;

    // ========================================================================
    // EMERGENCY TAB
    // ========================================================================

    @FXML
    private TextArea medicalAlertArea;

    @FXML
    private TextArea emergencyActionPlanArea;

    @FXML
    private TextArea physicalRestrictionsArea;

    @FXML
    private TextArea dietaryRestrictionsArea;

    // ========================================================================
    // PHYSICIAN & INSURANCE TAB
    // ========================================================================

    @FXML
    private TextField physicianNameField;

    @FXML
    private TextField physicianPhoneField;

    @FXML
    private TextField physicianAddressField;

    @FXML
    private TextField insuranceProviderField;

    @FXML
    private TextField policyNumberField;

    @FXML
    private TextField groupNumberField;

    @FXML
    private TextArea additionalNotesArea;

    @FXML
    private DatePicker lastReviewedDatePicker;

    @FXML
    private TextField reviewedByField;

    @FXML
    private CheckBox verifiedCheck;

    // ========================================================================
    // STATUS INDICATORS
    // ========================================================================

    @FXML
    private Label criticalIndicator;

    @FXML
    private Label hasMedicalConditionsIndicator;

    @FXML
    private Label lastModifiedLabel;

    // ========================================================================
    // DIALOG PANE & BUTTONS
    // ========================================================================

    @FXML
    private DialogPane dialogPane;

    // ========================================================================
    // INSTANCE VARIABLES
    // ========================================================================

    private Student student;
    private MedicalRecord medicalRecord;
    private boolean isNewRecord = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    /**
     * Initialize the controller - called automatically by JavaFX
     */
    @FXML
    public void initialize() {
        logger.info("Initializing MedicalRecordDialogController");

        // Populate combo boxes
        populateComboBoxes();

        // Setup listeners
        setupListeners();

        // Bind save/cancel buttons
        bindButtons();

        logger.info("MedicalRecordDialogController initialized successfully");
    }

    /**
     * Set the student and load their medical record
     * Must be called before showing the dialog
     *
     * @param student The student whose medical record to manage
     */
    public void setStudent(Student student) {
        logger.info("Setting student: {} (ID: {})", student.getFullName(), student.getId());

        this.student = student;

        // Update header labels
        studentNameLabel.setText(student.getFullName());
        studentIdLabel.setText("ID: " + student.getStudentId());

        // Load medical record
        loadMedicalRecord();
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load existing medical record or create a new one
     */
    private void loadMedicalRecord() {
        logger.info("Loading medical record for student ID: {}", student.getId());

        // Try to find existing medical record
        Optional<MedicalRecord> existingRecord = medicalRecordRepository.findByStudent(student);

        if (existingRecord.isPresent()) {
            // Load existing record
            medicalRecord = existingRecord.get();
            isNewRecord = false;
            logger.info("Found existing medical record (ID: {})", medicalRecord.getId());
            populateFormFields();
        } else {
            // Create new record
            medicalRecord = new MedicalRecord();
            medicalRecord.setStudent(student);
            medicalRecord.setAllergySeverity(AllergySeverity.NONE);
            isNewRecord = true;
            logger.info("Creating new medical record for student");

            // Set default values for boolean fields
            medicalRecord.setHasEpiPen(false);
            medicalRecord.setHasInhaler(false);
            medicalRecord.setHasDiabetes(false);
            medicalRecord.setHasAsthma(false);
            medicalRecord.setHasSeizureDisorder(false);
            medicalRecord.setHasHeartCondition(false);
            medicalRecord.setSelfAdministers(false);
            medicalRecord.setNurseVerified(false);

            // Update UI to show "New Record"
            lastModifiedLabel.setText("Last Modified: Never (New Record)");
        }

        // Update status indicators
        updateStatusIndicators();
    }

    /**
     * Populate form fields from medical record entity
     */
    private void populateFormFields() {
        if (medicalRecord == null) {
            logger.warn("Cannot populate form - medical record is null");
            return;
        }

        logger.debug("Populating form fields from medical record");

        // ALLERGIES TAB
        allergySeverityCombo.setValue(medicalRecord.getAllergySeverity());
        foodAllergiesArea.setText(medicalRecord.getFoodAllergies());
        medicationAllergiesArea.setText(medicalRecord.getMedicationAllergies());
        environmentalAllergiesArea.setText(medicalRecord.getEnvironmentalAllergies());

        hasEpiPenCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getHasEpiPen()));
        epiPenLocationField.setText(medicalRecord.getEpiPenLocation());

        hasInhalerCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getHasInhaler()));
        inhalerLocationField.setText(medicalRecord.getInhalerLocation());

        // CHRONIC CONDITIONS TAB
        hasDiabetesCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getHasDiabetes()));
        diabetesTypeCombo.setValue(medicalRecord.getDiabetesType());
        diabetesManagementArea.setText(medicalRecord.getDiabetesManagement());

        hasAsthmaCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getHasAsthma()));
        asthmaSeverityCombo.setValue(medicalRecord.getAsthmaSeverity());
        asthmaTriggersArea.setText(medicalRecord.getAsthmaTriggers());

        hasSeizureDisorderCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getHasSeizureDisorder()));
        seizureTypeField.setText(medicalRecord.getSeizureDetails());
        seizureProtocolArea.setText(medicalRecord.getSeizureDetails());

        hasHeartConditionCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getHasHeartCondition()));
        heartConditionTypeField.setText(medicalRecord.getHeartConditionDetails());
        heartConditionNotesArea.setText(medicalRecord.getHeartConditionDetails());

        otherConditionsArea.setText(medicalRecord.getOtherConditions());

        // MEDICATIONS TAB
        currentMedicationsArea.setText(medicalRecord.getCurrentMedications());
        medicationScheduleArea.setText(medicalRecord.getMedicationSchedule());
        selfAdministersCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getSelfAdministers()));
        medicationInstructionsArea.setText(medicalRecord.getCurrentMedications());

        // EMERGENCY TAB
        medicalAlertArea.setText(medicalRecord.getMedicalAlert());
        emergencyActionPlanArea.setText(medicalRecord.getEmergencyActionPlan());
        physicalRestrictionsArea.setText(medicalRecord.getPhysicalRestrictions());
        dietaryRestrictionsArea.setText(medicalRecord.getDietaryRestrictions());

        // PHYSICIAN & INSURANCE TAB
        physicianNameField.setText(medicalRecord.getPrimaryPhysicianName());
        physicianPhoneField.setText(medicalRecord.getPrimaryPhysicianPhone());
        physicianAddressField.setText("");  // Not in entity, could add if needed

        insuranceProviderField.setText(medicalRecord.getInsuranceProvider());
        policyNumberField.setText(medicalRecord.getInsurancePolicyNumber());
        groupNumberField.setText("");  // Not in entity, could add if needed

        additionalNotesArea.setText(medicalRecord.getAdditionalNotes());

        lastReviewedDatePicker.setValue(medicalRecord.getLastReviewDate());
        reviewedByField.setText(medicalRecord.getLastUpdatedBy());
        verifiedCheck.setSelected(Boolean.TRUE.equals(medicalRecord.getNurseVerified()));

        // Update last modified label
        if (medicalRecord.getLastUpdatedDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            String formattedDate = medicalRecord.getLastUpdatedDate().format(formatter);
            lastModifiedLabel.setText("Last Modified: " + formattedDate);
        } else {
            lastModifiedLabel.setText("Last Modified: Never");
        }
    }

    // ========================================================================
    // COMBO BOX POPULATION
    // ========================================================================

    /**
     * Populate all combo boxes with appropriate values
     */
    private void populateComboBoxes() {
        logger.debug("Populating combo boxes");

        // Allergy Severity
        allergySeverityCombo.getItems().addAll(
            AllergySeverity.NONE,
            AllergySeverity.MILD,
            AllergySeverity.MODERATE,
            AllergySeverity.SEVERE,
            AllergySeverity.LIFE_THREATENING
        );
        allergySeverityCombo.setValue(AllergySeverity.NONE);

        // Diabetes Type
        diabetesTypeCombo.getItems().addAll(
            "Type 1",
            "Type 2",
            "Gestational",
            "Pre-diabetes"
        );

        // Asthma Severity
        asthmaSeverityCombo.getItems().addAll(
            "Mild",
            "Moderate",
            "Severe"
        );
    }

    // ========================================================================
    // LISTENERS & INTERACTIVE BEHAVIOR
    // ========================================================================

    /**
     * Setup change listeners for interactive behavior
     */
    private void setupListeners() {
        logger.debug("Setting up change listeners");

        // Allergy severity description
        allergySeverityCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                severityDescriptionLabel.setText(newVal.getDescription());
                updateStatusIndicators();
            }
        });

        // EpiPen checkbox - enable/disable location field
        hasEpiPenCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            epiPenLocationField.setDisable(!newVal);
            if (!newVal) {
                epiPenLocationField.clear();
            }
            updateStatusIndicators();
        });

        // Inhaler checkbox - enable/disable location field
        hasInhalerCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            inhalerLocationField.setDisable(!newVal);
            if (!newVal) {
                inhalerLocationField.clear();
            }
            updateStatusIndicators();
        });

        // Diabetes checkbox - enable/disable related fields
        hasDiabetesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            diabetesTypeCombo.setDisable(!newVal);
            diabetesManagementArea.setDisable(!newVal);
            if (!newVal) {
                diabetesTypeCombo.setValue(null);
                diabetesManagementArea.clear();
            }
            updateStatusIndicators();
        });

        // Asthma checkbox - enable/disable related fields
        hasAsthmaCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            asthmaSeverityCombo.setDisable(!newVal);
            asthmaTriggersArea.setDisable(!newVal);
            if (!newVal) {
                asthmaSeverityCombo.setValue(null);
                asthmaTriggersArea.clear();
            }
            updateStatusIndicators();
        });

        // Seizure disorder checkbox - enable/disable related fields
        hasSeizureDisorderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            seizureTypeField.setDisable(!newVal);
            seizureProtocolArea.setDisable(!newVal);
            if (!newVal) {
                seizureTypeField.clear();
                seizureProtocolArea.clear();
            }
            updateStatusIndicators();
        });

        // Heart condition checkbox - enable/disable related fields
        hasHeartConditionCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            heartConditionTypeField.setDisable(!newVal);
            heartConditionNotesArea.setDisable(!newVal);
            if (!newVal) {
                heartConditionTypeField.clear();
                heartConditionNotesArea.clear();
            }
            updateStatusIndicators();
        });

        // Medical alert - update status indicators
        medicalAlertArea.textProperty().addListener((obs, oldVal, newVal) -> updateStatusIndicators());

        // Update status indicators when any allergy field changes
        foodAllergiesArea.textProperty().addListener((obs, oldVal, newVal) -> updateStatusIndicators());
        medicationAllergiesArea.textProperty().addListener((obs, oldVal, newVal) -> updateStatusIndicators());
        environmentalAllergiesArea.textProperty().addListener((obs, oldVal, newVal) -> updateStatusIndicators());
        currentMedicationsArea.textProperty().addListener((obs, oldVal, newVal) -> updateStatusIndicators());
        otherConditionsArea.textProperty().addListener((obs, oldVal, newVal) -> updateStatusIndicators());
    }

    /**
     * Update status indicators based on form data
     */
    private void updateStatusIndicators() {
        // Check if this is a critical case
        boolean isCritical = isCriticalCase();
        criticalIndicator.setVisible(isCritical);
        criticalIndicator.setManaged(isCritical);

        // Check if student has any medical conditions
        boolean hasMedicalConditions = hasMedicalConditions();
        hasMedicalConditionsIndicator.setVisible(hasMedicalConditions);
        hasMedicalConditionsIndicator.setManaged(hasMedicalConditions);
    }

    /**
     * Check if this is a critical medical case
     */
    private boolean isCriticalCase() {
        AllergySeverity severity = allergySeverityCombo.getValue();
        boolean severityCheck = AllergySeverity.SEVERE.equals(severity) ||
                               AllergySeverity.LIFE_THREATENING.equals(severity);

        boolean hasEpiPen = hasEpiPenCheck.isSelected();
        boolean hasSeizures = hasSeizureDisorderCheck.isSelected();
        boolean hasHeartCondition = hasHeartConditionCheck.isSelected();

        String medicalAlert = medicalAlertArea.getText();
        boolean hasMedicalAlert = medicalAlert != null && !medicalAlert.trim().isEmpty();

        return severityCheck || hasEpiPen || hasSeizures || hasHeartCondition || hasMedicalAlert;
    }

    /**
     * Check if student has any medical conditions
     */
    private boolean hasMedicalConditions() {
        return hasText(foodAllergiesArea) ||
               hasText(medicationAllergiesArea) ||
               hasText(environmentalAllergiesArea) ||
               hasDiabetesCheck.isSelected() ||
               hasAsthmaCheck.isSelected() ||
               hasSeizureDisorderCheck.isSelected() ||
               hasHeartConditionCheck.isSelected() ||
               hasText(otherConditionsArea) ||
               hasText(currentMedicationsArea);
    }

    /**
     * Helper method to check if a text area has content
     */
    private boolean hasText(TextArea area) {
        String text = area.getText();
        return text != null && !text.trim().isEmpty();
    }

    // ========================================================================
    // BUTTON BINDING & ACTIONS
    // ========================================================================

    /**
     * Bind save and cancel button actions
     */
    private void bindButtons() {
        if (dialogPane == null) {
            logger.warn("DialogPane is null - button binding skipped");
            return;
        }

        // Get save button and add event handler
        Button saveButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (saveButton != null) {
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!handleSave()) {
                    event.consume(); // Prevent dialog from closing if validation fails
                }
            });
        }

        // Cancel button doesn't need special handling - default behavior is fine
    }

    /**
     * Handle save button click
     *
     * @return true if save was successful, false otherwise
     */
    @FXML
    public boolean handleSave() {
        logger.info("Save button clicked");

        // Validate form
        if (!validateForm()) {
            logger.warn("Form validation failed");
            return false;
        }

        try {
            // Collect data from form
            collectFormData();

            // Set last updated information
            medicalRecord.setLastUpdatedDate(LocalDate.now());
            medicalRecord.setLastUpdatedBy(System.getProperty("user.name")); // Could be replaced with actual logged-in user

            // Set nurse verified flag
            medicalRecord.setNurseVerified(verifiedCheck.isSelected());

            // Save to database
            medicalRecord = medicalRecordRepository.save(medicalRecord);

            logger.info("Medical record saved successfully (ID: {})", medicalRecord.getId());

            // Show success message
            showSuccess("Medical record saved successfully!");

            return true;

        } catch (Exception e) {
            logger.error("Error saving medical record", e);
            showError("Failed to save medical record: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle cancel button click
     */
    @FXML
    public void handleCancel() {
        logger.info("Cancel button clicked");
        // Dialog will close automatically - no need to do anything
    }

    // ========================================================================
    // DATA COLLECTION
    // ========================================================================

    /**
     * Collect data from form fields into medical record entity
     */
    private void collectFormData() {
        logger.debug("Collecting form data");

        // ALLERGIES
        medicalRecord.setAllergySeverity(allergySeverityCombo.getValue());
        medicalRecord.setFoodAllergies(getNullableText(foodAllergiesArea));
        medicalRecord.setMedicationAllergies(getNullableText(medicationAllergiesArea));
        medicalRecord.setEnvironmentalAllergies(getNullableText(environmentalAllergiesArea));

        medicalRecord.setHasEpiPen(hasEpiPenCheck.isSelected());
        medicalRecord.setEpiPenLocation(getNullableText(epiPenLocationField));

        medicalRecord.setHasInhaler(hasInhalerCheck.isSelected());
        medicalRecord.setInhalerLocation(getNullableText(inhalerLocationField));

        // CHRONIC CONDITIONS
        medicalRecord.setHasDiabetes(hasDiabetesCheck.isSelected());
        medicalRecord.setDiabetesType(diabetesTypeCombo.getValue());
        medicalRecord.setDiabetesManagement(getNullableText(diabetesManagementArea));

        medicalRecord.setHasAsthma(hasAsthmaCheck.isSelected());
        medicalRecord.setAsthmaSeverity(asthmaSeverityCombo.getValue());
        medicalRecord.setAsthmaTriggers(getNullableText(asthmaTriggersArea));

        medicalRecord.setHasSeizureDisorder(hasSeizureDisorderCheck.isSelected());
        medicalRecord.setSeizureDetails(getNullableText(seizureProtocolArea));

        medicalRecord.setHasHeartCondition(hasHeartConditionCheck.isSelected());
        medicalRecord.setHeartConditionDetails(getNullableText(heartConditionNotesArea));

        medicalRecord.setOtherConditions(getNullableText(otherConditionsArea));

        // MEDICATIONS
        medicalRecord.setCurrentMedications(getNullableText(currentMedicationsArea));
        medicalRecord.setMedicationSchedule(getNullableText(medicationScheduleArea));
        medicalRecord.setSelfAdministers(selfAdministersCheck.isSelected());

        // EMERGENCY
        medicalRecord.setMedicalAlert(getNullableText(medicalAlertArea));
        medicalRecord.setEmergencyActionPlan(getNullableText(emergencyActionPlanArea));
        medicalRecord.setPhysicalRestrictions(getNullableText(physicalRestrictionsArea));
        medicalRecord.setDietaryRestrictions(getNullableText(dietaryRestrictionsArea));

        // PHYSICIAN & INSURANCE
        medicalRecord.setPrimaryPhysicianName(getNullableText(physicianNameField));
        medicalRecord.setPrimaryPhysicianPhone(getNullableText(physicianPhoneField));
        medicalRecord.setInsuranceProvider(getNullableText(insuranceProviderField));
        medicalRecord.setInsurancePolicyNumber(getNullableText(policyNumberField));

        medicalRecord.setAdditionalNotes(getNullableText(additionalNotesArea));
        medicalRecord.setLastReviewDate(lastReviewedDatePicker.getValue());

        // Calculate next review date (1 year from last review)
        if (lastReviewedDatePicker.getValue() != null) {
            medicalRecord.setNextReviewDate(lastReviewedDatePicker.getValue().plusYears(1));
        }
    }

    /**
     * Get text from a TextArea, returning null if empty
     */
    private String getNullableText(TextArea area) {
        if (area == null) return null;
        String text = area.getText();
        return (text != null && !text.trim().isEmpty()) ? text.trim() : null;
    }

    /**
     * Get text from a TextField, returning null if empty
     */
    private String getNullableText(TextField field) {
        if (field == null) return null;
        String text = field.getText();
        return (text != null && !text.trim().isEmpty()) ? text.trim() : null;
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate form data
     *
     * @return true if valid, false otherwise
     */
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        // Validate EpiPen location if checked
        if (hasEpiPenCheck.isSelected()) {
            String location = epiPenLocationField.getText();
            if (location == null || location.trim().isEmpty()) {
                errors.append("- EpiPen location is required when student carries EpiPen\n");
            }
        }

        // Validate inhaler location if checked
        if (hasInhalerCheck.isSelected()) {
            String location = inhalerLocationField.getText();
            if (location == null || location.trim().isEmpty()) {
                errors.append("- Inhaler location is required when student carries inhaler\n");
            }
        }

        // Validate diabetes information if checked
        if (hasDiabetesCheck.isSelected()) {
            if (diabetesTypeCombo.getValue() == null || diabetesTypeCombo.getValue().isEmpty()) {
                errors.append("- Diabetes type is required when diabetes is selected\n");
            }
        }

        // Validate asthma information if checked
        if (hasAsthmaCheck.isSelected()) {
            if (asthmaSeverityCombo.getValue() == null || asthmaSeverityCombo.getValue().isEmpty()) {
                errors.append("- Asthma severity is required when asthma is selected\n");
            }
        }

        // Validate seizure information if checked
        if (hasSeizureDisorderCheck.isSelected()) {
            String seizureType = seizureTypeField.getText();
            if (seizureType == null || seizureType.trim().isEmpty()) {
                errors.append("- Seizure type is required when seizure disorder is selected\n");
            }
        }

        // Validate heart condition information if checked
        if (hasHeartConditionCheck.isSelected()) {
            String heartType = heartConditionTypeField.getText();
            if (heartType == null || heartType.trim().isEmpty()) {
                errors.append("- Heart condition type is required when heart condition is selected\n");
            }
        }

        // Validate physician phone format (basic validation)
        String physicianPhone = physicianPhoneField.getText();
        if (physicianPhone != null && !physicianPhone.trim().isEmpty()) {
            if (!isValidPhoneNumber(physicianPhone)) {
                errors.append("- Physician phone number format is invalid\n");
            }
        }

        // Show errors if any
        if (errors.length() > 0) {
            showValidationError("Please correct the following errors:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    /**
     * Validate phone number format (basic validation)
     */
    private boolean isValidPhoneNumber(String phone) {
        // Allow various formats: (555) 123-4567, 555-123-4567, 5551234567, etc.
        String cleaned = phone.replaceAll("[^0-9]", "");
        return cleaned.length() >= 10 && cleaned.length() <= 15;
    }

    // ========================================================================
    // DIALOGS & MESSAGES
    // ========================================================================

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
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
        alert.setHeaderText("Failed to save medical record");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show validation error message
     */
    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Please correct the following errors:");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========================================================================
    // PUBLIC ACCESSORS
    // ========================================================================

    /**
     * Get the medical record being edited
     */
    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    /**
     * Check if this is a new record
     */
    public boolean isNewRecord() {
        return isNewRecord;
    }
}
