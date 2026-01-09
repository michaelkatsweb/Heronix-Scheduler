package com.heronix.ui.controller;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.DeliveryModel;
import com.heronix.model.enums.IEPStatus;
import com.heronix.model.enums.ServiceStatus;
import com.heronix.model.enums.ServiceType;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.IEPManagementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * IEP Wizard Controller
 *
 * Comprehensive 7-step wizard for creating and managing Individualized Education Programs (IEPs).
 * Guides users through the complete IEP creation process with validation and auto-calculations.
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since December 15, 2025
 */
@Controller
@Slf4j
public class IEPWizardController {

    // ========== SERVICES AND REPOSITORIES ==========

    @Autowired
    private IEPManagementService iepManagementService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    // ========== FXML COMPONENTS - STEP INDICATORS ==========

    @FXML private VBox step1Indicator, step2Indicator, step3Indicator, step4Indicator;
    @FXML private VBox step5Indicator, step6Indicator, step7Indicator;

    // ========== FXML COMPONENTS - STEP PANELS ==========

    @FXML private StackPane stepContainer;
    @FXML private ScrollPane step1Panel, step2Panel, step3Panel, step4Panel;
    @FXML private ScrollPane step5Panel, step6Panel, step7Panel;

    // ========== FXML COMPONENTS - NAVIGATION ==========

    @FXML private Button previousButton, nextButton, finishButton, cancelButton;
    @FXML private Label studentNameLabel;

    // ========== FXML COMPONENTS - STEP 1: BASIC INFO ==========

    @FXML private ComboBox<Student> studentCombo;
    @FXML private TextField iepNumberField;
    @FXML private DatePicker effectiveDatePicker;
    @FXML private DatePicker expirationDatePicker;
    @FXML private DatePicker annualReviewDatePicker;
    @FXML private ComboBox<IEPStatus> statusCombo;

    // ========== FXML COMPONENTS - STEP 2: DISABILITY ==========

    @FXML private ComboBox<String> disabilityCategoryCombo;
    @FXML private ComboBox<String> secondaryDisabilityCategoryCombo;
    @FXML private DatePicker eligibilityDatePicker;

    // ========== FXML COMPONENTS - STEP 3: SERVICES ==========

    @FXML private CheckBox speechTherapyCheck, occupationalTherapyCheck, physicalTherapyCheck;
    @FXML private CheckBox counselingCheck, behavioralSupportCheck;
    @FXML private TextField speechMinutesField, otMinutesField, ptMinutesField;
    @FXML private TextField counselingMinutesField, behavioralMinutesField;
    @FXML private Label totalServiceMinutesLabel;

    // ========== FXML COMPONENTS - STEP 4: LRE & PLACEMENT ==========

    @FXML private Slider generalEdSlider;
    @FXML private TextField generalEdPercentField;
    @FXML private Label generalEdStatusLabel, specialEdPercentLabel;
    @FXML private ComboBox<String> placementSettingCombo;
    @FXML private TextArea lreJustificationArea;

    // ========== FXML COMPONENTS - STEP 5: ACCOMMODATIONS ==========

    @FXML private TextArea accommodationsArea, modificationsArea, assistiveTechnologyArea;
    @FXML private CheckBox coTeachingCheck, paraprofessionalCheck, pullOutServicesCheck;
    @FXML private CheckBox pushInServicesCheck, transportationCheck, extendedSchoolYearCheck;
    @FXML private TextField paraHoursField;

    // ========== FXML COMPONENTS - STEP 6: GOALS ==========

    @FXML private TextArea annualGoalsArea, parentConcernsArea, transitionPlanArea;
    @FXML private ComboBox<String> progressMonitoringCombo;

    // ========== FXML COMPONENTS - STEP 7: REVIEW ==========

    @FXML private TextField caseManagerNameField, caseManagerEmailField, caseManagerPhoneField;
    @FXML private ComboBox<Teacher> specialEdTeacherCombo;
    @FXML private TextArea notesArea, summaryArea;
    @FXML private CheckBox verificationCheck;

    // ========== STATE VARIABLES ==========

    private int currentStep = 1;
    private IEP currentIEP;
    private Student selectedStudent;
    private boolean editMode = false;

    // ========== CONSTANTS ==========

    private static final String[] IDEA_DISABILITY_CATEGORIES = {
        "Specific Learning Disability (SLD)",
        "Speech or Language Impairment",
        "Other Health Impairment (OHI)",
        "Autism Spectrum Disorder",
        "Emotional Disturbance",
        "Intellectual Disability",
        "Multiple Disabilities",
        "Hearing Impairment (including Deafness)",
        "Orthopedic Impairment",
        "Visual Impairment (including Blindness)",
        "Deaf-Blindness",
        "Traumatic Brain Injury (TBI)",
        "Developmental Delay (ages 3-9)"
    };

    private static final String[] PLACEMENT_SETTINGS = {
        "General Education (80-100% of day)",
        "Resource Room (40-79% in General Ed)",
        "Separate Class (Less than 40% in General Ed)",
        "Separate School (Special Education Facility)",
        "Residential Facility",
        "Homebound/Hospital",
        "Correctional Facility"
    };

    private static final String[] PROGRESS_MONITORING_FREQUENCIES = {
        "Weekly",
        "Bi-Weekly (Every 2 weeks)",
        "Monthly",
        "Quarterly (Every 9 weeks)",
        "Trimester (Every 12 weeks)",
        "Semester (Twice per year)",
        "Annually"
    };

    // ========== INITIALIZATION ==========

    @FXML
    public void initialize() {
        log.info("Initializing IEP Wizard Controller");

        try {
            // Initialize combo boxes
            initializeComboBoxes();

            // Setup step 1 bindings
            setupStep1Bindings();

            // Setup step 3 service bindings
            setupStep3ServiceBindings();

            // Setup step 4 LRE bindings
            setupStep4LREBindings();

            // Setup step 5 support bindings
            setupStep5SupportBindings();

            // Initialize wizard state
            showStep(1);
            updateNavigationButtons();

            log.info("IEP Wizard initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing IEP Wizard", e);
            showError("Initialization Error", "Failed to initialize IEP Wizard: " + e.getMessage());
        }
    }

    /**
     * Initialize all combo boxes with their data
     */
    private void initializeComboBoxes() {
        // Step 1: Load students
        try {
            List<Student> students = studentRepository.findAll();
            studentCombo.setItems(FXCollections.observableArrayList(students));
            studentCombo.setConverter(new javafx.util.StringConverter<Student>() {
                @Override
                public String toString(Student student) {
                    return student != null ? student.getFullName() + " (" + student.getStudentId() + ")" : "";
                }
                @Override
                public Student fromString(String string) {
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Error loading students", e);
        }

        // Status combo
        statusCombo.setItems(FXCollections.observableArrayList(IEPStatus.values()));
        statusCombo.setConverter(new javafx.util.StringConverter<IEPStatus>() {
            @Override
            public String toString(IEPStatus status) {
                return status != null ? status.getDisplayName() : "";
            }
            @Override
            public IEPStatus fromString(String string) {
                return null;
            }
        });
        statusCombo.setValue(IEPStatus.DRAFT);

        // Step 2: Disability categories
        disabilityCategoryCombo.setItems(FXCollections.observableArrayList(IDEA_DISABILITY_CATEGORIES));
        secondaryDisabilityCategoryCombo.setItems(FXCollections.observableArrayList(IDEA_DISABILITY_CATEGORIES));

        // Step 4: Placement settings
        placementSettingCombo.setItems(FXCollections.observableArrayList(PLACEMENT_SETTINGS));

        // Step 6: Progress monitoring
        progressMonitoringCombo.setItems(FXCollections.observableArrayList(PROGRESS_MONITORING_FREQUENCIES));
        progressMonitoringCombo.setValue("Quarterly (Every 9 weeks)");

        // Step 7: Special ed teachers
        try {
            List<Teacher> specialEdTeachers = teacherRepository.findAll().stream()
                .filter(t -> t.getRole() != null && (
                    t.getRole().name().contains("SPECIAL_ED") ||
                    t.getRole().name().contains("SPED") ||
                    t.getRole().name().equals("TEACHER")
                ))
                .toList();
            specialEdTeacherCombo.setItems(FXCollections.observableArrayList(specialEdTeachers));
            specialEdTeacherCombo.setConverter(new javafx.util.StringConverter<Teacher>() {
                @Override
                public String toString(Teacher teacher) {
                    if (teacher == null) return "";
                    return teacher.getName() != null ? teacher.getName() :
                           (teacher.getFirstName() + " " + teacher.getLastName());
                }
                @Override
                public Teacher fromString(String string) {
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Error loading teachers", e);
        }
    }

    /**
     * Setup data bindings for Step 1
     */
    private void setupStep1Bindings() {
        // Student selection updates the label
        studentCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedStudent = newVal;
                studentNameLabel.setText("Student: " + newVal.getFullName());
            }
        });

        // Effective date auto-sets expiration date to 1 year later
        effectiveDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                expirationDatePicker.setValue(newVal.plusYears(1));
                annualReviewDatePicker.setValue(newVal.plusYears(1));
            }
        });
    }

    /**
     * Setup bindings for Step 3 (Services)
     */
    private void setupStep3ServiceBindings() {
        // Enable/disable minute fields based on checkbox
        speechTherapyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            speechMinutesField.setDisable(!newVal);
            if (!newVal) speechMinutesField.setText("0");
            updateTotalServiceMinutes();
        });

        occupationalTherapyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            otMinutesField.setDisable(!newVal);
            if (!newVal) otMinutesField.setText("0");
            updateTotalServiceMinutes();
        });

        physicalTherapyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            ptMinutesField.setDisable(!newVal);
            if (!newVal) ptMinutesField.setText("0");
            updateTotalServiceMinutes();
        });

        counselingCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            counselingMinutesField.setDisable(!newVal);
            if (!newVal) counselingMinutesField.setText("0");
            updateTotalServiceMinutes();
        });

        behavioralSupportCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            behavioralMinutesField.setDisable(!newVal);
            if (!newVal) behavioralMinutesField.setText("0");
            updateTotalServiceMinutes();
        });

        // Update total when any minute field changes
        speechMinutesField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalServiceMinutes());
        otMinutesField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalServiceMinutes());
        ptMinutesField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalServiceMinutes());
        counselingMinutesField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalServiceMinutes());
        behavioralMinutesField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalServiceMinutes());
    }

    /**
     * Setup bindings for Step 4 (LRE)
     */
    private void setupStep4LREBindings() {
        // Sync slider with text field
        generalEdSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int percent = newVal.intValue();
            generalEdPercentField.setText(String.valueOf(percent));

            // Update special ed percent (inverse)
            int specialEdPercent = 100 - percent;
            specialEdPercentLabel.setText(specialEdPercent + "%");

            // Update status label with color coding
            updateLREStatusLabel(percent);
        });

        // Sync text field with slider
        generalEdPercentField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int percent = Integer.parseInt(newVal);
                if (percent >= 0 && percent <= 100) {
                    generalEdSlider.setValue(percent);
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        });
    }

    /**
     * Setup bindings for Step 5 (Support Services)
     */
    private void setupStep5SupportBindings() {
        // Enable/disable paraprofessional hours field
        paraprofessionalCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            paraHoursField.setDisable(!newVal);
            if (!newVal) paraHoursField.setText("0.0");
        });
    }

    /**
     * Update total service minutes label
     */
    private void updateTotalServiceMinutes() {
        int total = 0;

        try {
            if (speechTherapyCheck.isSelected()) {
                total += Integer.parseInt(speechMinutesField.getText().trim());
            }
            if (occupationalTherapyCheck.isSelected()) {
                total += Integer.parseInt(otMinutesField.getText().trim());
            }
            if (physicalTherapyCheck.isSelected()) {
                total += Integer.parseInt(ptMinutesField.getText().trim());
            }
            if (counselingCheck.isSelected()) {
                total += Integer.parseInt(counselingMinutesField.getText().trim());
            }
            if (behavioralSupportCheck.isSelected()) {
                total += Integer.parseInt(behavioralMinutesField.getText().trim());
            }
        } catch (NumberFormatException e) {
            // Ignore invalid numbers
        }

        totalServiceMinutesLabel.setText(total + " minutes");
    }

    /**
     * Update LRE status label with color coding
     */
    private void updateLREStatusLabel(int genEdPercent) {
        if (genEdPercent >= 80) {
            generalEdStatusLabel.setText("Highly Inclusive (80%+ in General Ed)");
            generalEdStatusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        } else if (genEdPercent >= 40) {
            generalEdStatusLabel.setText("Moderately Inclusive (40-79% in General Ed)");
            generalEdStatusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        } else {
            generalEdStatusLabel.setText("Restrictive Setting (Less than 40% in General Ed)");
            generalEdStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }
    }

    // ========== NAVIGATION ==========

    /**
     * Handle Next button - move to next wizard step
     */
    @FXML
    private void handleNext() {
        log.info("Next button clicked - Current step: {}", currentStep);

        // Validate current step before proceeding
        if (!validateCurrentStep()) {
            return;
        }

        // Move to next step
        if (currentStep < 7) {
            showStep(currentStep + 1);
        }
    }

    /**
     * Handle Previous button - move to previous wizard step
     */
    @FXML
    private void handlePrevious() {
        log.info("Previous button clicked - Current step: {}", currentStep);

        // Move to previous step
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    /**
     * Handle Cancel button - close wizard
     */
    @FXML
    private void handleCancel() {
        log.info("Cancel button clicked");

        // Confirm cancellation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel IEP Wizard");
        confirm.setHeaderText("Are you sure you want to cancel?");
        confirm.setContentText("All unsaved data will be lost.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                closeWizard();
            }
        });
    }

    /**
     * Handle Finish button - complete wizard and save IEP
     */
    @FXML
    private void handleFinish() {
        log.info("Finish button clicked");

        // Validate all data
        if (!verificationCheck.isSelected()) {
            showError("Verification Required",
                "Please check the verification box to confirm that the IEP information is accurate and complete.");
            return;
        }

        try {
            // Build IEP object from form data
            IEP iep = buildIEPFromFormData();

            // Save IEP
            IEP savedIEP;
            if (editMode && currentIEP != null) {
                savedIEP = iepManagementService.updateIEP(iep);
                log.info("Updated IEP ID: {}", savedIEP.getId());
            } else {
                savedIEP = iepManagementService.createIEP(iep);
                log.info("Created new IEP ID: {}", savedIEP.getId());
            }

            // Show success message
            showInfo("IEP Saved Successfully",
                "IEP " + savedIEP.getIepNumber() + " has been saved successfully.\n\n" +
                "Student: " + savedIEP.getStudent().getFullName() + "\n" +
                "Status: " + savedIEP.getStatus().getDisplayName() + "\n" +
                "Services: " + savedIEP.getServiceCount() + " services\n" +
                "Total Minutes/Week: " + savedIEP.getTotalMinutesPerWeek());

            // Close wizard
            closeWizard();

        } catch (Exception e) {
            log.error("Error saving IEP", e);
            showError("Error Saving IEP", "An error occurred while saving the IEP: " + e.getMessage());
        }
    }

    /**
     * Show specific step
     */
    private void showStep(int step) {
        currentStep = step;

        // Hide all panels
        step1Panel.setVisible(false);
        step2Panel.setVisible(false);
        step3Panel.setVisible(false);
        step4Panel.setVisible(false);
        step5Panel.setVisible(false);
        step6Panel.setVisible(false);
        step7Panel.setVisible(false);

        // Show current panel
        switch (step) {
            case 1 -> step1Panel.setVisible(true);
            case 2 -> step2Panel.setVisible(true);
            case 3 -> step3Panel.setVisible(true);
            case 4 -> step4Panel.setVisible(true);
            case 5 -> step5Panel.setVisible(true);
            case 6 -> step6Panel.setVisible(true);
            case 7 -> {
                step7Panel.setVisible(true);
                generateSummary();
            }
        }

        // Update step indicators
        updateStepIndicators();

        // Update navigation buttons
        updateNavigationButtons();

        log.info("Showing step {}", step);
    }

    /**
     * Update step indicators (visual progress)
     */
    private void updateStepIndicators() {
        // Reset all indicators
        step1Indicator.getStyleClass().removeAll("wizard-step-active", "wizard-step-completed");
        step2Indicator.getStyleClass().removeAll("wizard-step-active", "wizard-step-completed");
        step3Indicator.getStyleClass().removeAll("wizard-step-active", "wizard-step-completed");
        step4Indicator.getStyleClass().removeAll("wizard-step-active", "wizard-step-completed");
        step5Indicator.getStyleClass().removeAll("wizard-step-active", "wizard-step-completed");
        step6Indicator.getStyleClass().removeAll("wizard-step-active", "wizard-step-completed");
        step7Indicator.getStyleClass().removeAll("wizard-step-active", "wizard-step-completed");

        // Mark completed steps
        if (currentStep > 1) step1Indicator.getStyleClass().add("wizard-step-completed");
        if (currentStep > 2) step2Indicator.getStyleClass().add("wizard-step-completed");
        if (currentStep > 3) step3Indicator.getStyleClass().add("wizard-step-completed");
        if (currentStep > 4) step4Indicator.getStyleClass().add("wizard-step-completed");
        if (currentStep > 5) step5Indicator.getStyleClass().add("wizard-step-completed");
        if (currentStep > 6) step6Indicator.getStyleClass().add("wizard-step-completed");

        // Mark current step as active
        switch (currentStep) {
            case 1 -> step1Indicator.getStyleClass().add("wizard-step-active");
            case 2 -> step2Indicator.getStyleClass().add("wizard-step-active");
            case 3 -> step3Indicator.getStyleClass().add("wizard-step-active");
            case 4 -> step4Indicator.getStyleClass().add("wizard-step-active");
            case 5 -> step5Indicator.getStyleClass().add("wizard-step-active");
            case 6 -> step6Indicator.getStyleClass().add("wizard-step-active");
            case 7 -> step7Indicator.getStyleClass().add("wizard-step-active");
        }
    }

    /**
     * Update navigation button states
     */
    private void updateNavigationButtons() {
        previousButton.setDisable(currentStep == 1);
        nextButton.setVisible(currentStep < 7);
        finishButton.setVisible(currentStep == 7);
    }

    // ========== VALIDATION ==========

    /**
     * Validate current step before proceeding
     */
    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1 -> {
                if (studentCombo.getValue() == null) {
                    showError("Student Required", "Please select a student.");
                    return false;
                }
                if (effectiveDatePicker.getValue() == null) {
                    showError("Effective Date Required", "Please select an effective date.");
                    return false;
                }
                if (expirationDatePicker.getValue() == null) {
                    showError("Expiration Date Required", "Please select an expiration date.");
                    return false;
                }
                return true;
            }
            case 2 -> {
                if (disabilityCategoryCombo.getValue() == null) {
                    showError("Disability Category Required", "Please select a primary disability category.");
                    return false;
                }
                return true;
            }
            case 3 -> {
                // At least one service should be selected
                boolean hasService = speechTherapyCheck.isSelected() ||
                                   occupationalTherapyCheck.isSelected() ||
                                   physicalTherapyCheck.isSelected() ||
                                   counselingCheck.isSelected() ||
                                   behavioralSupportCheck.isSelected();
                if (!hasService) {
                    showWarning("No Services Selected",
                        "It's recommended to select at least one related service. Do you want to continue anyway?");
                }
                return true;
            }
            case 4 -> {
                if (placementSettingCombo.getValue() == null) {
                    showError("Placement Setting Required", "Please select a placement setting.");
                    return false;
                }
                if (lreJustificationArea.getText().trim().isEmpty()) {
                    showWarning("LRE Justification Recommended",
                        "It's highly recommended to provide a justification for the placement. Do you want to continue anyway?");
                }
                return true;
            }
            case 5, 6 -> {
                // Optional fields, no validation needed
                return true;
            }
            case 7 -> {
                if (caseManagerNameField.getText().trim().isEmpty()) {
                    showError("Case Manager Required", "Please enter the case manager's name.");
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    // ========== DATA BUILDING ==========

    /**
     * Build IEP object from form data
     */
    private IEP buildIEPFromFormData() {
        IEP iep = new IEP();

        // If editing, preserve the ID
        if (editMode && currentIEP != null) {
            iep.setId(currentIEP.getId());
        }

        // Step 1: Basic Info
        iep.setStudent(studentCombo.getValue());
        iep.setIepNumber(iepNumberField.getText().trim().isEmpty() ?
            generateIEPNumber() : iepNumberField.getText().trim());
        iep.setStartDate(effectiveDatePicker.getValue());
        iep.setEndDate(expirationDatePicker.getValue());
        iep.setNextReviewDate(annualReviewDatePicker.getValue());
        iep.setStatus(statusCombo.getValue());

        // Step 2: Disability
        iep.setEligibilityCategory(disabilityCategoryCombo.getValue());
        iep.setPrimaryDisability(secondaryDisabilityCategoryCombo.getValue());

        // Step 3: Services
        List<IEPService> services = buildServicesFromFormData();
        services.forEach(iep::addService);

        // Step 4: LRE
        String lreInfo = String.format("General Ed: %s%%, Special Ed: %s%%\nPlacement: %s\nJustification: %s",
            generalEdPercentField.getText(),
            specialEdPercentLabel.getText().replace("%", ""),
            placementSettingCombo.getValue(),
            lreJustificationArea.getText());

        // Step 5: Accommodations (combine all into notes)
        StringBuilder accommodations = new StringBuilder();
        if (!accommodationsArea.getText().trim().isEmpty()) {
            accommodations.append("Accommodations:\n").append(accommodationsArea.getText()).append("\n\n");
        }
        if (!modificationsArea.getText().trim().isEmpty()) {
            accommodations.append("Modifications:\n").append(modificationsArea.getText()).append("\n\n");
        }
        if (!assistiveTechnologyArea.getText().trim().isEmpty()) {
            accommodations.append("Assistive Technology:\n").append(assistiveTechnologyArea.getText()).append("\n\n");
        }
        iep.setAccommodations(accommodations.toString());

        // Step 6: Goals
        iep.setGoals(annualGoalsArea.getText());

        // Step 7: Case Manager and Notes
        iep.setCaseManager(caseManagerNameField.getText() + " (" + caseManagerEmailField.getText() + ")");

        StringBuilder notes = new StringBuilder();
        notes.append(lreInfo).append("\n\n");
        if (coTeachingCheck.isSelected()) notes.append("Requires Co-Teaching\n");
        if (paraprofessionalCheck.isSelected()) {
            notes.append("Requires Paraprofessional Support: ").append(paraHoursField.getText()).append(" hours/day\n");
        }
        if (pullOutServicesCheck.isSelected()) notes.append("Requires Pull-Out Services\n");
        if (pushInServicesCheck.isSelected()) notes.append("Requires Push-In Services\n");
        if (transportationCheck.isSelected()) notes.append("Requires Transportation\n");
        if (extendedSchoolYearCheck.isSelected()) notes.append("Eligible for Extended School Year (ESY)\n");
        notes.append("\n");
        if (!parentConcernsArea.getText().trim().isEmpty()) {
            notes.append("Parent Concerns:\n").append(parentConcernsArea.getText()).append("\n\n");
        }
        if (!transitionPlanArea.getText().trim().isEmpty()) {
            notes.append("Transition Plan:\n").append(transitionPlanArea.getText()).append("\n\n");
        }
        notes.append(notesArea.getText());
        iep.setNotes(notes.toString());

        iep.setCreatedBy("IEP Wizard"); // TODO: Get current user

        return iep;
    }

    /**
     * Build IEP services from form data
     */
    private List<IEPService> buildServicesFromFormData() {
        List<IEPService> services = new ArrayList<>();

        if (speechTherapyCheck.isSelected()) {
            services.add(createService(ServiceType.SPEECH_THERAPY, speechMinutesField.getText()));
        }
        if (occupationalTherapyCheck.isSelected()) {
            services.add(createService(ServiceType.OCCUPATIONAL_THERAPY, otMinutesField.getText()));
        }
        if (physicalTherapyCheck.isSelected()) {
            services.add(createService(ServiceType.PHYSICAL_THERAPY, ptMinutesField.getText()));
        }
        if (counselingCheck.isSelected()) {
            services.add(createService(ServiceType.COUNSELING, counselingMinutesField.getText()));
        }
        if (behavioralSupportCheck.isSelected()) {
            services.add(createService(ServiceType.BEHAVIORAL_SUPPORT, behavioralMinutesField.getText()));
        }

        return services;
    }

    /**
     * Create IEP service object
     */
    private IEPService createService(ServiceType type, String minutesText) {
        IEPService service = new IEPService();
        service.setServiceType(type);

        try {
            int minutes = Integer.parseInt(minutesText.trim());
            service.setMinutesPerWeek(minutes);

            // Default session duration (can be customized later)
            if (type == ServiceType.SPEECH_THERAPY || type == ServiceType.COUNSELING) {
                service.setSessionDurationMinutes(30);
                service.setFrequency(minutes / 30 + "x per week");
            } else {
                service.setSessionDurationMinutes(45);
                service.setFrequency(minutes / 45 + "x per week");
            }
        } catch (NumberFormatException e) {
            service.setMinutesPerWeek(0);
            service.setSessionDurationMinutes(30);
            service.setFrequency("To be determined");
        }

        // Default delivery model (typically pull-out for these services)
        service.setDeliveryModel(DeliveryModel.INDIVIDUAL);
        service.setStatus(ServiceStatus.NOT_SCHEDULED);
        service.setServiceDescription(type.getDescription());

        return service;
    }

    /**
     * Generate unique IEP number
     */
    private String generateIEPNumber() {
        int year = LocalDate.now().getYear();
        // This is a simplified version - in production, should check database for uniqueness
        int sequence = (int) (Math.random() * 999999) + 1;
        return String.format("IEP-%04d-%06d", year, sequence);
    }

    /**
     * Generate summary for Step 7
     */
    private void generateSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("=== IEP SUMMARY ===\n\n");

        // Student Info
        if (studentCombo.getValue() != null) {
            summary.append("STUDENT: ").append(studentCombo.getValue().getFullName())
                .append(" (").append(studentCombo.getValue().getStudentId()).append(")\n");
        }

        // IEP Number and Dates
        summary.append("IEP NUMBER: ").append(iepNumberField.getText().trim().isEmpty() ?
            "[Auto-generate]" : iepNumberField.getText()).append("\n");
        summary.append("EFFECTIVE: ").append(formatDate(effectiveDatePicker.getValue())).append("\n");
        summary.append("EXPIRES: ").append(formatDate(expirationDatePicker.getValue())).append("\n");
        summary.append("REVIEW DATE: ").append(formatDate(annualReviewDatePicker.getValue())).append("\n");
        summary.append("STATUS: ").append(statusCombo.getValue() != null ?
            statusCombo.getValue().getDisplayName() : "").append("\n\n");

        // Disability
        summary.append("PRIMARY DISABILITY: ").append(disabilityCategoryCombo.getValue() != null ?
            disabilityCategoryCombo.getValue() : "").append("\n");
        if (secondaryDisabilityCategoryCombo.getValue() != null) {
            summary.append("SECONDARY DISABILITY: ").append(secondaryDisabilityCategoryCombo.getValue()).append("\n");
        }
        summary.append("\n");

        // Services
        summary.append("RELATED SERVICES:\n");
        int totalMinutes = 0;
        if (speechTherapyCheck.isSelected()) {
            int min = safeParseInt(speechMinutesField.getText());
            summary.append("  • Speech Therapy: ").append(min).append(" min/week\n");
            totalMinutes += min;
        }
        if (occupationalTherapyCheck.isSelected()) {
            int min = safeParseInt(otMinutesField.getText());
            summary.append("  • Occupational Therapy: ").append(min).append(" min/week\n");
            totalMinutes += min;
        }
        if (physicalTherapyCheck.isSelected()) {
            int min = safeParseInt(ptMinutesField.getText());
            summary.append("  • Physical Therapy: ").append(min).append(" min/week\n");
            totalMinutes += min;
        }
        if (counselingCheck.isSelected()) {
            int min = safeParseInt(counselingMinutesField.getText());
            summary.append("  • Counseling: ").append(min).append(" min/week\n");
            totalMinutes += min;
        }
        if (behavioralSupportCheck.isSelected()) {
            int min = safeParseInt(behavioralMinutesField.getText());
            summary.append("  • Behavioral Support: ").append(min).append(" min/week\n");
            totalMinutes += min;
        }
        summary.append("TOTAL SERVICE MINUTES: ").append(totalMinutes).append(" per week\n\n");

        // LRE
        summary.append("PLACEMENT:\n");
        summary.append("  • Time in General Ed: ").append(generalEdPercentField.getText()).append("%\n");
        summary.append("  • Time in Special Ed: ").append(specialEdPercentLabel.getText()).append("\n");
        summary.append("  • Setting: ").append(placementSettingCombo.getValue() != null ?
            placementSettingCombo.getValue() : "").append("\n\n");

        // Support Services
        summary.append("SUPPORT SERVICES:\n");
        if (coTeachingCheck.isSelected()) summary.append("  • Co-Teaching\n");
        if (paraprofessionalCheck.isSelected()) {
            summary.append("  • Paraprofessional: ").append(paraHoursField.getText()).append(" hours/day\n");
        }
        if (pullOutServicesCheck.isSelected()) summary.append("  • Pull-Out Services\n");
        if (pushInServicesCheck.isSelected()) summary.append("  • Push-In Services\n");
        if (transportationCheck.isSelected()) summary.append("  • Transportation\n");
        if (extendedSchoolYearCheck.isSelected()) summary.append("  • Extended School Year (ESY)\n");
        summary.append("\n");

        // Case Manager
        summary.append("CASE MANAGER: ").append(caseManagerNameField.getText()).append("\n");
        if (!caseManagerEmailField.getText().trim().isEmpty()) {
            summary.append("EMAIL: ").append(caseManagerEmailField.getText()).append("\n");
        }
        if (!caseManagerPhoneField.getText().trim().isEmpty()) {
            summary.append("PHONE: ").append(caseManagerPhoneField.getText()).append("\n");
        }

        summaryArea.setText(summary.toString());
    }

    // ========== UTILITY METHODS ==========

    /**
     * Handle Generate IEP Number button
     */
    @FXML
    private void handleGenerateIEPNumber() {
        String number = generateIEPNumber();
        iepNumberField.setText(number);
        log.info("Generated IEP number: {}", number);
    }

    /**
     * Handle Save Draft button - save current progress
     */
    @FXML
    private void handleSaveDraft() {
        log.info("Save Draft button clicked - Not yet implemented");
        showInfo("Save Draft", "Draft saving functionality will be implemented in a future version.");
    }

    /**
     * Close wizard window
     */
    private void closeWizard() {
        try {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            log.error("Error closing wizard", e);
        }
    }

    /**
     * Format date for display
     */
    private String formatDate(LocalDate date) {
        if (date == null) return "Not set";
        return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    /**
     * Safely parse integer from text field
     */
    private int safeParseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning dialog
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== PUBLIC API ==========

    /**
     * Set student for new IEP (called from parent controller)
     */
    public void setStudent(Student student) {
        this.selectedStudent = student;
        this.studentCombo.setValue(student);
        this.studentCombo.setDisable(true);
        this.studentNameLabel.setText("Student: " + student.getFullName());
    }

    /**
     * Load existing IEP for editing
     */
    public void loadIEP(IEP iep) {
        this.currentIEP = iep;
        this.editMode = true;

        // TODO: Populate all form fields from existing IEP
        log.info("Loading existing IEP for editing: {}", iep.getId());
        showInfo("Edit Mode", "IEP editing functionality will be fully implemented in next version.\n\nFor now, create a new IEP.");
    }

    // ========== PLACEHOLDER EVENT HANDLERS (from FXML) ==========

    @FXML
    private void handleAddService() {
        log.info("Add Service - Not applicable in this wizard design");
    }

    @FXML
    private void handleRemoveService() {
        log.info("Remove Service - Not applicable in this wizard design");
    }

    @FXML
    private void handleAddGoal() {
        log.info("Add Goal - Not applicable in this wizard design");
    }

    @FXML
    private void handleRemoveGoal() {
        log.info("Remove Goal - Not applicable in this wizard design");
    }

    @FXML
    private void handleAddAccommodation() {
        log.info("Add Accommodation - Not applicable in this wizard design");
    }

    @FXML
    private void handleRemoveAccommodation() {
        log.info("Remove Accommodation - Not applicable in this wizard design");
    }

    @FXML
    private void handleAddTeamMember() {
        log.info("Add Team Member - Not applicable in this wizard design");
    }

    @FXML
    private void handleRemoveTeamMember() {
        log.info("Remove Team Member - Not applicable in this wizard design");
    }

    @FXML
    private void handleSearchStudent() {
        log.info("Search Student - Not applicable in this wizard design");
    }

    @FXML
    private void handleCalculateMinutes() {
        updateTotalServiceMinutes();
    }

    @FXML
    private void handleValidate() {
        log.info("Validate IEP - checking current step");
        validateCurrentStep();
    }

    @FXML
    private void handlePreview() {
        log.info("Preview IEP");
        generateSummary();
        showStep(7);
    }

    @FXML
    private void handlePrint() {
        log.info("Print IEP - Not yet implemented");
        showInfo("Print IEP", "IEP printing will be implemented in a future version.");
    }

    @FXML
    private void handleFilterChange() {
        log.info("Filter changed");
    }

    @FXML
    private void handleSelectionChange() {
        log.info("Selection changed");
    }
}
