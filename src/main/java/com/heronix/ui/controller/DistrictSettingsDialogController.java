package com.heronix.ui.controller;

import com.heronix.model.DistrictSettings;
import com.heronix.service.DistrictSettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;

/**
 * Controller for District Settings Dialog
 *
 * Manages the configuration of district-wide settings including:
 * - District information
 * - Email generation formats
 * - ID generation rules
 * - Room phone configuration
 * - Print/export settings
 * - Academic defaults
 * - Responsive design (fits all screen sizes)
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/DistrictSettingsDialogController.java
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - Added responsive design support
 * @since Phase 1 - District Configuration System
 */
@Controller
public class DistrictSettingsDialogController extends BaseDialogController {

    @Autowired
    private DistrictSettingsService districtSettingsService;

    private DistrictSettings settings;
    // dialogStage inherited from BaseDialogController
    private boolean saveClicked = false;

    // ========================================================================
    // TAB 1: DISTRICT INFORMATION
    // ========================================================================

    @FXML
    private TextField districtNameField;

    @FXML
    private TextField districtAddressField;

    @FXML
    private TextField districtCityField;

    @FXML
    private TextField districtStateField;

    @FXML
    private TextField districtZipField;

    @FXML
    private TextField districtPhoneField;

    @FXML
    private TextField districtFaxField;

    @FXML
    private TextField districtWebsiteField;

    @FXML
    private TextField districtEmailField;

    // ========================================================================
    // TAB 2: EMAIL SETTINGS
    // ========================================================================

    @FXML
    private TextField teacherEmailDomainField;

    @FXML
    private TextField teacherEmailFormatField;

    @FXML
    private CheckBox autoGenerateTeacherEmailCheck;

    @FXML
    private TextField studentEmailDomainField;

    @FXML
    private TextField studentEmailFormatField;

    @FXML
    private CheckBox autoGenerateStudentEmailCheck;

    // ========================================================================
    // TAB 3: ID GENERATION
    // ========================================================================

    @FXML
    private TextField teacherIdPrefixField;

    @FXML
    private Spinner<Integer> teacherIdStartSpinner;

    @FXML
    private Spinner<Integer> teacherIdPaddingSpinner;

    @FXML
    private Spinner<Integer> teacherIdCurrentSpinner;

    @FXML
    private Label teacherIdPreviewLabel;

    @FXML
    private CheckBox autoGenerateTeacherIdCheck;

    @FXML
    private TextField studentIdFormatField;

    @FXML
    private TextField studentIdPrefixField;

    @FXML
    private Spinner<Integer> studentIdPaddingSpinner;

    @FXML
    private CheckBox autoGenerateStudentIdCheck;

    // ========================================================================
    // TAB 4: ROOM PHONE CONFIGURATION
    // ========================================================================

    @FXML
    private TextField roomPhonePrefixField;

    @FXML
    private Spinner<Integer> roomPhoneExtensionStartSpinner;

    @FXML
    private Label roomPhonePreviewLabel;

    @FXML
    private CheckBox autoGenerateRoomPhoneCheck;

    // ========================================================================
    // TAB 5: PRINT SETTINGS
    // ========================================================================

    @FXML
    private TextField logoPathField;

    @FXML
    private TextField scheduleHeaderTextField;

    @FXML
    private TextField printFooterTextField;

    @FXML
    private CheckBox includeDistrictInfoCheck;

    // ========================================================================
    // TAB 6: ACADEMIC SETTINGS
    // ========================================================================

    @FXML
    private Spinner<Integer> periodDurationSpinner;

    @FXML
    private Spinner<Integer> passingPeriodSpinner;

    @FXML
    private Spinner<Integer> lunchDurationSpinner;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    private void initialize() {
        setupSpinners();
        setupListeners();
    }

    /**
     * Setup all spinners with their value factories
     */
    private void setupSpinners() {
        // Teacher ID spinners
        teacherIdStartSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999, 1)
        );
        teacherIdPaddingSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4)
        );
        teacherIdCurrentSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999, 1)
        );

        // Student ID spinner
        studentIdPaddingSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4)
        );

        // Room phone spinner
        roomPhoneExtensionStartSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 9999, 4000)
        );

        // Academic settings spinners
        periodDurationSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 120, 50)
        );
        passingPeriodSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 15, 5)
        );
        lunchDurationSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 60, 30)
        );
    }

    /**
     * Setup listeners for live preview updates
     */
    private void setupListeners() {
        // Teacher ID preview update
        teacherIdPrefixField.textProperty().addListener((obs, old, newVal) -> updateTeacherIdPreview());
        teacherIdPaddingSpinner.valueProperty().addListener((obs, old, newVal) -> updateTeacherIdPreview());
        teacherIdCurrentSpinner.valueProperty().addListener((obs, old, newVal) -> updateTeacherIdPreview());

        // Room phone preview update
        roomPhonePrefixField.textProperty().addListener((obs, old, newVal) -> updateRoomPhonePreview());
        roomPhoneExtensionStartSpinner.valueProperty().addListener((obs, old, newVal) -> updateRoomPhonePreview());
    }

    /**
     * Update teacher ID preview label
     */
    private void updateTeacherIdPreview() {
        try {
            String prefix = teacherIdPrefixField.getText() != null ? teacherIdPrefixField.getText() : "";
            int current = teacherIdCurrentSpinner.getValue();
            int padding = teacherIdPaddingSpinner.getValue();

            String paddedNumber = String.format("%0" + padding + "d", current);
            String preview = prefix + paddedNumber;

            teacherIdPreviewLabel.setText("Preview: " + preview);
        } catch (Exception e) {
            teacherIdPreviewLabel.setText("Preview: --");
        }
    }

    /**
     * Update room phone preview label
     */
    private void updateRoomPhonePreview() {
        try {
            String prefix = roomPhonePrefixField.getText() != null ? roomPhonePrefixField.getText() : "";
            int extensionStart = roomPhoneExtensionStartSpinner.getValue();

            // Example: room 101
            int exampleExtension = extensionStart + 101;
            String preview = prefix + exampleExtension;

            roomPhonePreviewLabel.setText("Preview: " + preview + " (for room 101)");
        } catch (Exception e) {
            roomPhonePreviewLabel.setText("Preview: --");
        }
    }

    /**
     * Set the dialog stage with responsive sizing
     */
    @Override
    public void setDialogStage(Stage stage) {
        super.setDialogStage(stage); // Call base class method for responsive sizing
    }

    /**
     * Configure responsive dialog size
     */
    @Override
    protected void configureResponsiveSize() {
        // DistrictSettings has many tabs and fields - use large dialog (70% x 80%)
        configureLargeDialogSize();
    }

    /**
     * Load and display district settings
     */
    public void loadSettings() {
        settings = districtSettingsService.getOrCreateDistrictSettings();
        populateFields();
        updateTeacherIdPreview();
        updateRoomPhonePreview();
    }

    /**
     * Populate all fields from settings
     */
    private void populateFields() {
        if (settings == null) return;

        // District Information
        districtNameField.setText(settings.getDistrictName());
        districtAddressField.setText(settings.getDistrictAddress());
        districtCityField.setText(settings.getDistrictCity());
        districtStateField.setText(settings.getDistrictState());
        districtZipField.setText(settings.getDistrictZip());
        districtPhoneField.setText(settings.getDistrictPhone());
        districtFaxField.setText(settings.getDistrictFax());
        districtWebsiteField.setText(settings.getDistrictWebsite());
        districtEmailField.setText(settings.getDistrictEmail());

        // Email Settings
        teacherEmailDomainField.setText(settings.getTeacherEmailDomain());
        teacherEmailFormatField.setText(settings.getTeacherEmailFormat());
        autoGenerateTeacherEmailCheck.setSelected(
            settings.getAutoGenerateTeacherEmail() != null && settings.getAutoGenerateTeacherEmail()
        );

        studentEmailDomainField.setText(settings.getStudentEmailDomain());
        studentEmailFormatField.setText(settings.getStudentEmailFormat());
        autoGenerateStudentEmailCheck.setSelected(
            settings.getAutoGenerateStudentEmail() != null && settings.getAutoGenerateStudentEmail()
        );

        // ID Generation
        teacherIdPrefixField.setText(settings.getTeacherIdPrefix());
        if (settings.getTeacherIdStartNumber() != null) {
            teacherIdStartSpinner.getValueFactory().setValue(settings.getTeacherIdStartNumber());
        }
        if (settings.getTeacherIdPadding() != null) {
            teacherIdPaddingSpinner.getValueFactory().setValue(settings.getTeacherIdPadding());
        }
        if (settings.getTeacherIdCurrentNumber() != null) {
            teacherIdCurrentSpinner.getValueFactory().setValue(settings.getTeacherIdCurrentNumber());
        }
        autoGenerateTeacherIdCheck.setSelected(
            settings.getAutoGenerateTeacherId() != null && settings.getAutoGenerateTeacherId()
        );

        studentIdFormatField.setText(settings.getStudentIdFormat());
        studentIdPrefixField.setText(settings.getStudentIdPrefix());
        if (settings.getStudentIdPadding() != null) {
            studentIdPaddingSpinner.getValueFactory().setValue(settings.getStudentIdPadding());
        }
        autoGenerateStudentIdCheck.setSelected(
            settings.getAutoGenerateStudentId() != null && settings.getAutoGenerateStudentId()
        );

        // Room Phone
        roomPhonePrefixField.setText(settings.getRoomPhonePrefix());
        if (settings.getRoomPhoneExtensionStart() != null) {
            roomPhoneExtensionStartSpinner.getValueFactory().setValue(settings.getRoomPhoneExtensionStart());
        }
        autoGenerateRoomPhoneCheck.setSelected(
            settings.getAutoGenerateRoomPhone() != null && settings.getAutoGenerateRoomPhone()
        );

        // Print Settings
        logoPathField.setText(settings.getLogoPath());
        scheduleHeaderTextField.setText(settings.getScheduleHeaderText());
        printFooterTextField.setText(settings.getPrintFooterText());
        includeDistrictInfoCheck.setSelected(
            settings.getIncludeDistrictInfoOnPrint() != null && settings.getIncludeDistrictInfoOnPrint()
        );

        // Academic Settings
        if (settings.getDefaultPeriodDuration() != null) {
            periodDurationSpinner.getValueFactory().setValue(settings.getDefaultPeriodDuration());
        }
        if (settings.getDefaultPassingPeriod() != null) {
            passingPeriodSpinner.getValueFactory().setValue(settings.getDefaultPassingPeriod());
        }
        if (settings.getDefaultLunchDuration() != null) {
            lunchDurationSpinner.getValueFactory().setValue(settings.getDefaultLunchDuration());
        }
    }

    /**
     * Save all field values to settings object
     */
    private void saveFieldsToSettings() {
        if (settings == null) {
            settings = new DistrictSettings();
        }

        // District Information
        settings.setDistrictName(districtNameField.getText());
        settings.setDistrictAddress(districtAddressField.getText());
        settings.setDistrictCity(districtCityField.getText());
        settings.setDistrictState(districtStateField.getText());
        settings.setDistrictZip(districtZipField.getText());
        settings.setDistrictPhone(districtPhoneField.getText());
        settings.setDistrictFax(districtFaxField.getText());
        settings.setDistrictWebsite(districtWebsiteField.getText());
        settings.setDistrictEmail(districtEmailField.getText());

        // Email Settings
        settings.setTeacherEmailDomain(teacherEmailDomainField.getText());
        settings.setTeacherEmailFormat(teacherEmailFormatField.getText());
        settings.setAutoGenerateTeacherEmail(autoGenerateTeacherEmailCheck.isSelected());

        settings.setStudentEmailDomain(studentEmailDomainField.getText());
        settings.setStudentEmailFormat(studentEmailFormatField.getText());
        settings.setAutoGenerateStudentEmail(autoGenerateStudentEmailCheck.isSelected());

        // ID Generation
        settings.setTeacherIdPrefix(teacherIdPrefixField.getText());
        settings.setTeacherIdStartNumber(teacherIdStartSpinner.getValue());
        settings.setTeacherIdPadding(teacherIdPaddingSpinner.getValue());
        settings.setTeacherIdCurrentNumber(teacherIdCurrentSpinner.getValue());
        settings.setAutoGenerateTeacherId(autoGenerateTeacherIdCheck.isSelected());

        settings.setStudentIdFormat(studentIdFormatField.getText());
        settings.setStudentIdPrefix(studentIdPrefixField.getText());
        settings.setStudentIdPadding(studentIdPaddingSpinner.getValue());
        settings.setAutoGenerateStudentId(autoGenerateStudentIdCheck.isSelected());

        // Room Phone
        settings.setRoomPhonePrefix(roomPhonePrefixField.getText());
        settings.setRoomPhoneExtensionStart(roomPhoneExtensionStartSpinner.getValue());
        settings.setAutoGenerateRoomPhone(autoGenerateRoomPhoneCheck.isSelected());

        // Print Settings
        settings.setLogoPath(logoPathField.getText());
        settings.setScheduleHeaderText(scheduleHeaderTextField.getText());
        settings.setPrintFooterText(printFooterTextField.getText());
        settings.setIncludeDistrictInfoOnPrint(includeDistrictInfoCheck.isSelected());

        // Academic Settings
        settings.setDefaultPeriodDuration(periodDurationSpinner.getValue());
        settings.setDefaultPassingPeriod(passingPeriodSpinner.getValue());
        settings.setDefaultLunchDuration(lunchDurationSpinner.getValue());
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    /**
     * Handle Save button click
     */
    @FXML
    private void handleSave() {
        if (validateInput()) {
            saveFieldsToSettings();
            settings = districtSettingsService.updateDistrictSettings(settings);
            saveClicked = true;
            dialogStage.close();

            showSuccessAlert();
        }
    }

    /**
     * Handle Cancel button click
     */
    @FXML
    @Override
    protected void handleCancel() {
        saveClicked = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Handle Reset to Defaults button click
     */
    @FXML
    private void handleResetDefaults() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset to Defaults");
        alert.setHeaderText("Reset all settings to defaults?");
        alert.setContentText("This will restore all default values. Your current settings will be lost.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                settings = DistrictSettings.builder()
                    .districtName("Heronix Scheduler District")
                    .teacherEmailDomain("@school.edu")
                    .studentEmailDomain("@students.school.edu")
                    .teacherEmailFormat("{lastname}_{firstname_initial}")
                    .studentEmailFormat("{firstname}.{lastname}")
                    .teacherIdPrefix("T")
                    .teacherIdStartNumber(1)
                    .teacherIdPadding(4)
                    .teacherIdCurrentNumber(1)
                    .studentIdFormat("{grad_year}-{sequence}")
                    .studentIdPadding(4)
                    .roomPhonePrefix("(555) 123-")
                    .roomPhoneExtensionStart(4000)
                    .autoGenerateTeacherEmail(true)
                    .autoGenerateStudentEmail(true)
                    .autoGenerateTeacherId(true)
                    .autoGenerateStudentId(true)
                    .autoGenerateRoomPhone(true)
                    .defaultPeriodDuration(50)
                    .defaultPassingPeriod(5)
                    .defaultLunchDuration(30)
                    .includeDistrictInfoOnPrint(true)
                    .build();

                populateFields();
            }
        });
    }

    /**
     * Handle Browse Logo button click
     */
    @FXML
    private void handleBrowseLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select School Logo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            logoPathField.setText(file.getAbsolutePath());
        }
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate user input
     */
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // District name is required
        if (districtNameField.getText() == null || districtNameField.getText().trim().isEmpty()) {
            errors.append("• District Name is required\n");
        }

        // Email format validation (if auto-generate is enabled)
        if (autoGenerateTeacherEmailCheck.isSelected()) {
            if (teacherEmailDomainField.getText() == null || teacherEmailDomainField.getText().trim().isEmpty()) {
                errors.append("• Teacher Email Domain is required when auto-generate is enabled\n");
            }
            if (teacherEmailFormatField.getText() == null || teacherEmailFormatField.getText().trim().isEmpty()) {
                errors.append("• Teacher Email Format is required when auto-generate is enabled\n");
            } else if (!districtSettingsService.isValidEmailFormat(teacherEmailFormatField.getText())) {
                errors.append("• Teacher Email Format must contain valid placeholders\n");
            }
        }

        if (autoGenerateStudentEmailCheck.isSelected()) {
            if (studentEmailDomainField.getText() == null || studentEmailDomainField.getText().trim().isEmpty()) {
                errors.append("• Student Email Domain is required when auto-generate is enabled\n");
            }
            if (studentEmailFormatField.getText() == null || studentEmailFormatField.getText().trim().isEmpty()) {
                errors.append("• Student Email Format is required when auto-generate is enabled\n");
            } else if (!districtSettingsService.isValidEmailFormat(studentEmailFormatField.getText())) {
                errors.append("• Student Email Format must contain valid placeholders\n");
            }
        }

        // ID format validation
        if (autoGenerateTeacherIdCheck.isSelected()) {
            if (teacherIdPrefixField.getText() == null || teacherIdPrefixField.getText().trim().isEmpty()) {
                errors.append("• Teacher ID Prefix is required when auto-generate is enabled\n");
            }
        }

        if (autoGenerateStudentIdCheck.isSelected()) {
            if (studentIdFormatField.getText() == null || studentIdFormatField.getText().trim().isEmpty()) {
                errors.append("• Student ID Format is required when auto-generate is enabled\n");
            } else if (!districtSettingsService.isValidIdFormat(studentIdFormatField.getText())) {
                errors.append("• Student ID Format must contain valid placeholders\n");
            }
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Please correct the following errors:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    /**
     * Show success alert
     */
    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings Saved");
        alert.setHeaderText("District Settings Saved Successfully");
        alert.setContentText("Your district settings have been saved and will be applied to all new entries.");
        alert.showAndWait();
    }

    /**
     * Check if save was clicked
     */
    public boolean isSaveClicked() {
        return saveClicked;
    }

    /**
     * Get the updated settings
     */
    public DistrictSettings getSettings() {
        return settings;
    }
}
