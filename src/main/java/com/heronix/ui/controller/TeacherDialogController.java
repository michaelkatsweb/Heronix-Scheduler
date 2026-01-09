package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.SpecialDutyAssignment;
import com.heronix.model.domain.SubjectCertification;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.CertificationType;
import com.heronix.model.enums.DutyType;
import com.heronix.model.enums.PriorityLevel;
import com.heronix.model.enums.USState;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.SpecialDutyAssignmentRepository;
import com.heronix.repository.SubjectCertificationRepository;
import com.heronix.repository.TeacherRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TeacherDialogController - Handles Add/Edit teacher form
 * Supports adding and editing teacher information with responsive design.
 *
 * Features:
 * - Teacher demographic information
 * - Subject certifications and qualifications
 * - Course assignments
 * - Special duty assignments
 * - Responsive design (fits all screen sizes)
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/TeacherDialogController.java
 *
 * @version 2.0.0 - Added responsive design support
 */
@Component
public class TeacherDialogController extends BaseDialogController {

    private static final Logger log = LoggerFactory.getLogger(TeacherDialogController.class);

    @FXML
    private Label dialogTitle;
    @FXML
    private TextField nameField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField employeeIdField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<String> departmentCombo;
    @FXML
    private Spinner<Integer> maxHoursSpinner;
    @FXML
    private Spinner<Integer> maxConsecutiveSpinner;
    @FXML
    private Spinner<Integer> preferredBreakSpinner;
    @FXML
    private ComboBox<String> priorityCombo;
    @FXML
    private ComboBox<CertificationType> certificationTypeCombo;
    @FXML
    private ComboBox<String> teacherRoleCombo;
    @FXML
    private TextArea manualCertificationsArea;
    @FXML
    private ListView<SubjectCertification> certificationsListView;
    @FXML
    private ListView<Course> assignedCoursesListView;
    @FXML
    private CheckBox activeCheckbox;
    @FXML
    private TextArea notesArea;
    @FXML
    private TextArea specialAssignmentArea;

    @FXML
    private Label nameError;
    @FXML
    private Label employeeIdError;
    @FXML
    private Label emailError;
    @FXML
    private Label departmentError;

    // ========================================================================
    // CREDENTIALS & COMPLIANCE - FXML BINDINGS (Phase: Teacher Personnel)
    // ========================================================================

    @FXML
    private TabPane teacherTabPane;
    @FXML
    private TabPane complianceSubTabPane;

    // GridPanes for compliance tabs (will be populated programmatically)
    @FXML
    private GridPane certificationGrid;
    @FXML
    private GridPane backgroundGrid;
    @FXML
    private GridPane healthGrid;
    @FXML
    private GridPane educationGrid;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectCertificationRepository certificationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SpecialDutyAssignmentRepository specialDutyAssignmentRepository;

    @Autowired
    private com.heronix.service.DistrictSettingsService districtSettingsService;

    private Teacher teacher; // For editing existing teacher
    private boolean isEditMode = false;
    private boolean saved = false;
    // dialogStage inherited from BaseDialogController

    private ObservableList<SubjectCertification> certifications = FXCollections.observableArrayList();
    private ObservableList<Course> assignedCourses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup department dropdown
        departmentCombo.getItems().addAll(
                "Mathematics",
                "Science",
                "English",
                "History",
                "Computer Science",
                "Arts",
                "Physical Education",
                "Foreign Languages",
                "Special Education",
                "Music",
                "Other");

        // Setup priority dropdown - use actual PriorityLevel enum values
        priorityCombo.getItems().addAll(
                "Q1_URGENT_IMPORTANT",
                "Q2_IMPORTANT_NOT_URGENT",
                "Q3_URGENT_NOT_IMPORTANT",
                "Q4_NEITHER",
                "CRITICAL",
                "HIGH",
                "NORMAL",
                "LOW");
        priorityCombo.getSelectionModel().select("NORMAL");

        // Setup spinners with value factories
        SpinnerValueFactory<Integer> hoursFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 50, 40, 5);
        maxHoursSpinner.setValueFactory(hoursFactory);

        SpinnerValueFactory<Integer> consecutiveFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 8,
                1);
        maxConsecutiveSpinner.setValueFactory(consecutiveFactory);

        SpinnerValueFactory<Integer> breakFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 30, 5);
        preferredBreakSpinner.setValueFactory(breakFactory);

        // Setup certification type dropdown
        certificationTypeCombo.setItems(FXCollections.observableArrayList(CertificationType.values()));
        certificationTypeCombo.setConverter(new StringConverter<CertificationType>() {
            @Override
            public String toString(CertificationType type) {
                return type != null ? type.getDisplayName() : "";
            }
            @Override
            public CertificationType fromString(String string) {
                return null;
            }
        });
        certificationTypeCombo.getSelectionModel().select(CertificationType.CERTIFIED);

        // Setup teacher role dropdown
        teacherRoleCombo.getItems().addAll(
                "TEACHER",
                "CO_TEACHER",
                "PARAPROFESSIONAL",
                "SUBSTITUTE");
        teacherRoleCombo.getSelectionModel().select("TEACHER");

        // Setup certifications ListView
        certificationsListView.setItems(certifications);
        certificationsListView.setCellFactory(lv -> new ListCell<SubjectCertification>() {
            @Override
            protected void updateItem(SubjectCertification cert, boolean empty) {
                super.updateItem(cert, empty);
                if (empty || cert == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(cert.getDisplayString() + " - " + cert.getStatusString());
                    if (cert.isExpired()) {
                        setStyle("-fx-text-fill: red;");
                    } else if (cert.isExpiringSoon()) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });

        // Add context menu to ListView for deleting certifications
        certificationsListView.setContextMenu(createCertificationContextMenu());

        // Setup assigned courses ListView
        assignedCoursesListView.setItems(assignedCourses);
        assignedCoursesListView.setCellFactory(lv -> new ListCell<Course>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(course.getCourseCode() + " - " + course.getCourseName() + " (" + course.getSubject() + ")");
                    setStyle("-fx-text-fill: #2c3e50;");
                }
            }
        });

        // Add context menu to ListView for removing course assignments
        assignedCoursesListView.setContextMenu(createCourseContextMenu());

        // Setup email auto-generation listeners
        setupEmailAutoGeneration();

        // Setup ID auto-generation (will be called when dialog opens in add mode)

        // ========================================================================
        // INITIALIZE COMPLIANCE GRIDS (Phase: Teacher Personnel)
        // ========================================================================
        if (certificationGrid != null) {
            populateCertificationGrid();
        }
        if (backgroundGrid != null) {
            populateBackgroundGrid();
        }
        if (healthGrid != null) {
            populateHealthGrid();
        }
        if (educationGrid != null) {
            populateEducationGrid();
        }
    }

    /**
     * Setup email auto-generation based on first and last name
     */
    private void setupEmailAutoGeneration() {
        if (districtSettingsService == null) {
            return; // Service not available
        }

        // Add listeners to first and last name fields
        javafx.beans.value.ChangeListener<String> nameChangeListener = (obs, oldVal, newVal) -> {
            // Only auto-generate if both fields have values and email field is empty or was auto-generated
            String firstName = firstNameField.getText();
            String lastName = lastNameField.getText();

            if (firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty()) {

                // Only auto-fill if email is empty or in edit mode
                if (!isEditMode || emailField.getText() == null || emailField.getText().trim().isEmpty()) {
                    try {
                        String generatedEmail = districtSettingsService.generateTeacherEmail(firstName, lastName);
                        if (generatedEmail != null && !generatedEmail.isEmpty()) {
                            emailField.setText(generatedEmail);
                            emailField.setStyle("-fx-text-fill: #2196F3;"); // Blue text to indicate auto-generated
                        }
                    } catch (Exception e) {
                        // Silently ignore if generation fails
                        log.warn("Failed to generate email: {}", e.getMessage());
                    }
                }
            }
        };

        firstNameField.textProperty().addListener(nameChangeListener);
        lastNameField.textProperty().addListener(nameChangeListener);

        // Reset text color when user manually edits email
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                // User manually edited - change to normal color
                emailField.setStyle("");
            }
        });
    }

    /**
     * Auto-generate employee ID for new teacher
     */
    private void autoGenerateEmployeeId() {
        if (districtSettingsService == null || isEditMode) {
            return; // Don't auto-generate in edit mode
        }

        try {
            String generatedId = districtSettingsService.generateNextTeacherId();
            if (generatedId != null && !generatedId.isEmpty()) {
                employeeIdField.setText(generatedId);
                employeeIdField.setStyle("-fx-text-fill: #2196F3;"); // Blue text to indicate auto-generated
            }
        } catch (Exception e) {
            // Silently ignore if generation fails
            log.warn("Failed to generate employee ID: {}", e.getMessage());
        }
    }

    private ContextMenu createCertificationContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Remove Certification");
        deleteItem.setOnAction(e -> {
            SubjectCertification selected = certificationsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                certifications.remove(selected);
            }
        });
        menu.getItems().add(deleteItem);
        return menu;
    }

    private ContextMenu createCourseContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem removeItem = new MenuItem("Remove Course Assignment");
        removeItem.setOnAction(e -> {
            Course selected = assignedCoursesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // IMPORTANT: If in edit mode, remove from database immediately
                if (isEditMode && teacher != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Course Assignment");
                    confirm.setHeaderText("Remove " + selected.getCourseName() + "?");
                    confirm.setContentText("This will immediately unassign this course from " + teacher.getName());

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        try {
                            // Remove from database
                            selected.setTeacher(null);
                            teacher.getCourses().remove(selected);
                            courseRepository.save(selected);
                            teacherRepository.save(teacher);

                            // Remove from UI
                            assignedCourses.remove(selected);

                            showSuccess("Course removed successfully");
                        } catch (Exception ex) {
                            showError("Error removing course: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                } else {
                    // In add mode, just remove from UI list
                    assignedCourses.remove(selected);
                }
            }
        });
        menu.getItems().add(removeItem);
        return menu;
    }

    /**
     * Prepare form for adding a new teacher (call before showing dialog)
     */
    public void prepareForNew() {
        this.teacher = null;
        this.isEditMode = false;
        dialogTitle.setText("Add New Teacher");

        // Clear all form fields
        clearAllFields();

        // Generate next employee ID
        String nextEmployeeId = generateNextEmployeeId();
        employeeIdField.setText(nextEmployeeId);
        employeeIdField.setEditable(true);
        employeeIdField.setStyle("");

        // Set defaults
        activeCheckbox.setSelected(true);
        priorityCombo.setValue("NORMAL");
        teacherRoleCombo.setValue("TEACHER");
        certificationTypeCombo.getSelectionModel().select(CertificationType.CERTIFIED);

        // Focus on first name field
        javafx.application.Platform.runLater(() -> firstNameField.requestFocus());
    }

    /**
     * Clear all form fields
     */
    private void clearAllFields() {
        // Clear text fields
        nameField.clear();
        firstNameField.clear();
        lastNameField.clear();
        employeeIdField.clear();
        emailField.clear();
        phoneField.clear();

        // Clear text areas
        manualCertificationsArea.clear();
        notesArea.clear();
        specialAssignmentArea.clear();

        // Reset combo boxes to defaults
        departmentCombo.setValue(null);
        priorityCombo.setValue("NORMAL");
        certificationTypeCombo.getSelectionModel().select(CertificationType.CERTIFIED);
        teacherRoleCombo.setValue("TEACHER");

        // Reset spinners to defaults
        maxHoursSpinner.getValueFactory().setValue(40);
        maxConsecutiveSpinner.getValueFactory().setValue(8);
        preferredBreakSpinner.getValueFactory().setValue(30);

        // Clear observable lists
        certifications.clear();
        assignedCourses.clear();

        // Reset checkbox
        activeCheckbox.setSelected(true);

        // Clear error labels
        if (nameError != null) nameError.setText("");
        if (employeeIdError != null) employeeIdError.setText("");
        if (emailError != null) emailError.setText("");
        if (departmentError != null) departmentError.setText("");
    }

    /**
     * Generate next employee ID (T001, T002, T003, etc.)
     */
    private String generateNextEmployeeId() {
        try {
            List<Teacher> allTeachers = teacherRepository.findAllActive();

            if (allTeachers == null || allTeachers.isEmpty()) {
                return "T001";
            }

            // Find highest employee number
            int maxNumber = allTeachers.stream()
                .map(Teacher::getEmployeeId)
                .filter(id -> id != null && id.matches("T\\d+"))
                .map(id -> Integer.parseInt(id.substring(1)))
                .max(Integer::compareTo)
                .orElse(0);

            return String.format("T%03d", maxNumber + 1);
        } catch (Exception e) {
            // If error, return default
            return "T001";
        }
    }

    /**
     * Set teacher for editing (call before showing dialog)
     */
    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
        this.isEditMode = true;
        dialogTitle.setText("Edit Teacher");

        // Populate fields
        nameField.setText(teacher.getName());
        firstNameField.setText(teacher.getFirstName());
        lastNameField.setText(teacher.getLastName());
        employeeIdField.setText(teacher.getEmployeeId());
        emailField.setText(teacher.getEmail());
        phoneField.setText(teacher.getPhoneNumber());
        departmentCombo.setValue(teacher.getDepartment());
        maxHoursSpinner.getValueFactory().setValue(teacher.getMaxHoursPerWeek());
        maxConsecutiveSpinner.getValueFactory().setValue(teacher.getMaxConsecutiveHours());

        // Set preferred break minutes
        if (teacher.getPreferredBreakMinutes() != null) {
            preferredBreakSpinner.getValueFactory().setValue(teacher.getPreferredBreakMinutes());
        }

        if (teacher.getPriorityLevel() != null) {
            priorityCombo.setValue(teacher.getPriorityLevel().name());
        }

        activeCheckbox.setSelected(teacher.getActive());
        notesArea.setText(teacher.getNotes());
        specialAssignmentArea.setText(teacher.getSpecialAssignment());

        // Set certification type
        if (teacher.getCertificationType() != null) {
            certificationTypeCombo.setValue(teacher.getCertificationType());
        }

        // Set teacher role
        if (teacher.getTeacherRole() != null) {
            teacherRoleCombo.setValue(teacher.getTeacherRole());
        }

        // Load manual certifications (simple text list)
        if (teacher.getCertifications() != null && !teacher.getCertifications().isEmpty()) {
            manualCertificationsArea.setText(String.join("\n", teacher.getCertifications()));
        }

        // Load certifications
        certifications.clear();
        if (teacher.getSubjectCertifications() != null) {
            certifications.addAll(teacher.getSubjectCertifications());
        }

        // Load assigned courses
        assignedCourses.clear();
        if (teacher.getCourses() != null) {
            assignedCourses.addAll(teacher.getCourses());
        }

        // Make employee ID read-only when editing
        employeeIdField.setEditable(false);
        employeeIdField.setStyle("-fx-background-color: #f0f0f0;");
    }

    @FXML
    private void handleSave() {
        if (validateForm()) {
            try {
                if (isEditMode) {
                    // Update existing teacher
                    updateTeacherFromForm(teacher);
                    teacherRepository.save(teacher);
                    showSuccess("Teacher updated successfully!");
                } else {
                    // Create new teacher
                    Teacher newTeacher = new Teacher();
                    updateTeacherFromForm(newTeacher);
                    teacherRepository.save(newTeacher);
                    showSuccess("Teacher created successfully!");
                }

                saved = true;
                closeDialog();

            } catch (Exception e) {
                showError("Error saving teacher: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void updateTeacherFromForm(Teacher teacher) {
        // Set name (required field)
        if (nameField.getText() != null && !nameField.getText().trim().isEmpty()) {
            teacher.setName(nameField.getText().trim());
        }

        // Set first name and last name
        if (firstNameField.getText() != null && !firstNameField.getText().trim().isEmpty()) {
            teacher.setFirstName(firstNameField.getText().trim());
        }
        if (lastNameField.getText() != null && !lastNameField.getText().trim().isEmpty()) {
            teacher.setLastName(lastNameField.getText().trim());
        }

        // Set employee ID (with null check)
        if (employeeIdField.getText() != null) {
            teacher.setEmployeeId(employeeIdField.getText().trim());
        }

        // Set email (with null check)
        if (emailField.getText() != null) {
            teacher.setEmail(emailField.getText().trim());
        }

        // Set phone number (with null check)
        if (phoneField.getText() != null) {
            teacher.setPhoneNumber(phoneField.getText().trim());
        }
        teacher.setDepartment(departmentCombo.getValue());
        teacher.setMaxHoursPerWeek(maxHoursSpinner.getValue());
        teacher.setMaxConsecutiveHours(maxConsecutiveSpinner.getValue());
        teacher.setPreferredBreakMinutes(preferredBreakSpinner.getValue());

        String priorityStr = priorityCombo.getValue();
        if (priorityStr != null && !priorityStr.isEmpty()) {
            teacher.setPriorityLevel(PriorityLevel.valueOf(priorityStr));
        }

        // Set certification type
        if (certificationTypeCombo.getValue() != null) {
            teacher.setCertificationType(certificationTypeCombo.getValue());
        }

        // Set teacher role
        if (teacherRoleCombo.getValue() != null) {
            teacher.setTeacherRole(teacherRoleCombo.getValue());
        }

        // Parse and save manual certifications (simple text list)
        teacher.getCertifications().clear();
        String manualCertsText = manualCertificationsArea.getText();
        if (manualCertsText != null && !manualCertsText.trim().isEmpty()) {
            // Split by newlines or commas
            String[] lines = manualCertsText.split("[\\n,]");
            for (String line : lines) {
                String cert = line.trim();
                if (!cert.isEmpty()) {
                    teacher.getCertifications().add(cert);
                }
            }
        }

        // Update certifications
        teacher.getSubjectCertifications().clear();
        for (SubjectCertification cert : certifications) {
            cert.setTeacher(teacher);
            teacher.getSubjectCertifications().add(cert);
        }

        // Update course assignments
        // CRITICAL FIX: Only update course assignments when EDITING an existing teacher
        // When creating a new teacher, course assignments should be done through:
        // 1. Courses view (assign teacher to course)
        // 2. Smart Assignment feature
        // 3. Auto-assignment feature
        // Modifying course.setTeacher() here would STEAL courses from other teachers!
        if (isEditMode) {
            // Edit mode: Update existing teacher's course list
            teacher.getCourses().clear();
            for (Course course : assignedCourses) {
                course.setTeacher(teacher);
                teacher.getCourses().add(course);
            }
        } else {
            // Create mode: Do NOT modify courses
            // New teachers start with no course assignments
            teacher.getCourses().clear();
            log.info("New teacher created without course assignments. Use Courses view or Smart Assignment to assign courses.");
        }

        teacher.setActive(activeCheckbox.isSelected());
        teacher.setNotes(notesArea.getText());
        teacher.setSpecialAssignment(specialAssignmentArea.getText());

        // ========================================================================
        // SAVE COMPLIANCE FIELDS (Phase: Teacher Personnel)
        // ========================================================================
        saveComplianceFields(teacher);

        // SYNC: If special assignment text is provided, create/update SpecialDutyAssignment
        syncSpecialAssignmentToRoster(teacher);
    }

    /**
     * Synchronize the Special Assignment text field to the Special Duty Roster
     * Creates a SpecialDutyAssignment entry so it appears in the roster
     */
    private void syncSpecialAssignmentToRoster(Teacher teacher) {
        String specialAssignmentText = specialAssignmentArea.getText();

        if (specialAssignmentText != null && !specialAssignmentText.trim().isEmpty()) {
            // Check if teacher already has a special duty assignment for this description
            boolean exists = teacher.getSpecialDutyAssignments().stream()
                .anyMatch(duty -> duty.getCustomDutyName() != null &&
                                 duty.getCustomDutyName().equals(specialAssignmentText.trim()));

            if (!exists) {
                // Create new SpecialDutyAssignment
                SpecialDutyAssignment dutyAssignment = new SpecialDutyAssignment();
                dutyAssignment.setTeacher(teacher);
                dutyAssignment.setDutyType(DutyType.CUSTOM);
                dutyAssignment.setCustomDutyName(specialAssignmentText.trim());
                dutyAssignment.setStaffType("TEACHER");
                dutyAssignment.setIsRecurring(false); // One-time assignment by default
                dutyAssignment.setIsMandatory(false); // Optional by default
                dutyAssignment.setActive(true);
                dutyAssignment.setConfirmedByStaff(false);
                dutyAssignment.setPriority(3); // Low priority by default

                // Add notes indicating it was created from teacher profile
                dutyAssignment.setNotes("Created from teacher profile Special Assignment field");

                // Add to teacher's special duty assignments
                teacher.getSpecialDutyAssignments().add(dutyAssignment);

                // Save to database
                try {
                    specialDutyAssignmentRepository.save(dutyAssignment);
                } catch (Exception e) {
                    log.error("Error saving special duty assignment: {}", e.getMessage(), e);
                    // Log but don't fail the teacher save
                }
            }
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        // Reset error messages
        hideError(nameError);
        hideError(employeeIdError);
        hideError(emailError);
        hideError(departmentError);

        // Validate name
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showFieldError(nameError, "Name is required");
            valid = false;
        }

        // Validate employee ID
        String empId = employeeIdField.getText();
        if (empId == null || empId.trim().isEmpty()) {
            showFieldError(employeeIdError, "Employee ID is required");
            valid = false;
        } else if (!isEditMode) {
            // Check for duplicate employee ID (only when creating new)
            if (teacherRepository.findByEmployeeId(empId.trim()).isPresent()) {
                showFieldError(employeeIdError, "Employee ID already exists");
                valid = false;
            }
        }

        // Validate email
        String email = emailField.getText();
        if (email == null || email.trim().isEmpty()) {
            showFieldError(emailError, "Email is required");
            valid = false;
        } else if (!isValidEmail(email)) {
            showFieldError(emailError, "Invalid email format");
            valid = false;
        } else if (!isEditMode || !email.equals(teacher.getEmail())) {
            // Check for duplicate email (only when creating new or changing email)
            if (teacherRepository.findByEmail(email.trim()).isPresent()) {
                showFieldError(emailError, "Email already exists");
                valid = false;
            }
        }

        // Validate department
        if (departmentCombo.getValue() == null || departmentCombo.getValue().isEmpty()) {
            showFieldError(departmentError, "Department is required");
            valid = false;
        }

        return valid;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    @Override
    protected void handleCancel() {
        if (hasUnsavedChanges()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes");
            alert.setContentText("Are you sure you want to cancel?");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                closeDialog();
            }
        } else {
            closeDialog();
        }
    }

    private boolean hasUnsavedChanges() {
        // Simple check - if any field has content (with null safety)
        return (nameField.getText() != null && !nameField.getText().isEmpty()) ||
                (employeeIdField.getText() != null && !employeeIdField.getText().isEmpty()) ||
                (emailField.getText() != null && !emailField.getText().isEmpty());
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Set the dialog stage with responsive sizing
     */
    @Override
    public void setDialogStage(Stage stage) {
        super.setDialogStage(stage); // Call base class method for responsive sizing

        // Auto-generate employee ID for new teacher (not in edit mode)
        if (!isEditMode) {
            autoGenerateEmployeeId();
        }
    }

    /**
     * Configure responsive dialog size
     */
    @Override
    protected void configureResponsiveSize() {
        // TeacherDialog is complex with many fields - use large dialog (70% x 80%)
        configureLargeDialogSize();
    }

    /**
     * Check if teacher was saved
     */
    public boolean isSaved() {
        return saved;
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Failed to save teacher");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean wasSaved() {
        return saved;
    }

    /**
     * Handle adding a new certification
     */
    @FXML
    private void handleAddCertification() {
        // Create a dialog for adding certification
        Dialog<SubjectCertification> dialog = new Dialog<>();
        dialog.setTitle("Add Subject Certification");
        dialog.setHeaderText("Enter FLDOE Certification Details");

        // Set button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        // State selection dropdown
        ComboBox<USState> stateCombo = new ComboBox<>();
        stateCombo.setItems(FXCollections.observableArrayList(USState.values()));
        stateCombo.setConverter(new StringConverter<USState>() {
            @Override
            public String toString(USState state) {
                return state != null ? state.getFullDisplayName() : "";
            }
            @Override
            public USState fromString(String string) {
                return null;
            }
        });
        stateCombo.getSelectionModel().select(USState.FL); // Default to Florida

        ComboBox<String> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(
                "Mathematics",
                "Science",
                "Biology",
                "Chemistry",
                "Physics",
                "English/Language Arts",
                "Reading",
                "History",
                "Social Studies",
                "Computer Science",
                "Technology",
                "Physical Education",
                "Health",
                "Music",
                "Art",
                "Foreign Languages",
                "Spanish",
                "French",
                "Special Education (ESE/SPED)",
                "ESOL/ELL",
                "Gifted Education",
                "Career and Technical Education (CTE)"
        );
        subjectCombo.setEditable(true);

        ComboBox<String> gradeRangeCombo = new ComboBox<>();
        gradeRangeCombo.getItems().addAll(
                "K-6",      // Elementary (common)
                "K-8",      // Some states
                "K-12",     // All grades
                "6-12",     // Secondary (common)
                "7-12",     // Texas, some states
                "9-12",     // High School only
                "PreK-3",   // Early childhood
                "4-8",      // Middle grades
                "5-9"       // Some states
        );
        gradeRangeCombo.setEditable(true);

        TextField certNumberField = new TextField();
        certNumberField.setPromptText("e.g., 1234567");

        ComboBox<CertificationType> certTypeCombo = new ComboBox<>();
        certTypeCombo.setItems(FXCollections.observableArrayList(CertificationType.values()));
        certTypeCombo.setConverter(new StringConverter<CertificationType>() {
            @Override
            public String toString(CertificationType type) {
                return type != null ? type.getDisplayName() : "";
            }
            @Override
            public CertificationType fromString(String string) {
                return null;
            }
        });
        certTypeCombo.getSelectionModel().select(CertificationType.CERTIFIED);

        DatePicker issueDatePicker = new DatePicker(LocalDate.now());
        DatePicker expirationDatePicker = new DatePicker(LocalDate.now().plusYears(5));

        TextField specializationsField = new TextField();
        specializationsField.setPromptText("e.g., AP Calculus, Gifted, ESE");

        // Add info label about state-specific naming
        Label infoLabel = new Label("Note: Each state has different certification names and grade ranges.");
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-wrap-text: true;");
        infoLabel.setMaxWidth(400);

        grid.add(new Label("Issuing State: *"), 0, 0);
        grid.add(stateCombo, 1, 0);
        grid.add(new Label("Subject: *"), 0, 1);
        grid.add(subjectCombo, 1, 1);
        grid.add(new Label("Grade Range: *"), 0, 2);
        grid.add(gradeRangeCombo, 1, 2);
        grid.add(new Label("Cert Number:"), 0, 3);
        grid.add(certNumberField, 1, 3);
        grid.add(new Label("Cert Type: *"), 0, 4);
        grid.add(certTypeCombo, 1, 4);
        grid.add(new Label("Issue Date:"), 0, 5);
        grid.add(issueDatePicker, 1, 5);
        grid.add(new Label("Expiration:"), 0, 6);
        grid.add(expirationDatePicker, 1, 6);
        grid.add(new Label("Specializations:"), 0, 7);
        grid.add(specializationsField, 1, 7);
        grid.add(infoLabel, 0, 8, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert result when Add button clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (subjectCombo.getValue() == null || subjectCombo.getValue().isEmpty()) {
                    showError("Subject is required");
                    return null;
                }
                if (gradeRangeCombo.getValue() == null || gradeRangeCombo.getValue().isEmpty()) {
                    showError("Grade range is required");
                    return null;
                }

                SubjectCertification cert = new SubjectCertification();
                cert.setIssuingState(stateCombo.getValue());
                cert.setSubject(subjectCombo.getValue());
                cert.setGradeRange(gradeRangeCombo.getValue());
                cert.setCertificationNumber(certNumberField.getText());
                cert.setCertificationType(certTypeCombo.getValue());
                cert.setIssueDate(issueDatePicker.getValue());
                cert.setExpirationDate(expirationDatePicker.getValue());
                cert.setSpecializations(specializationsField.getText());
                // Issuing agency will be auto-populated from state via @PrePersist
                cert.setActive(true);

                return cert;
            }
            return null;
        });

        // Show dialog and add certification if valid
        dialog.showAndWait().ifPresent(cert -> {
            if (cert != null) {
                certifications.add(cert);
            }
        });
    }

    /**
     * Handle assigning a course to the teacher
     */
    @FXML
    private void handleAssignCourse() {
        // CRITICAL FIX: Prevent course assignment when creating new teacher
        // Course assignment should only be done for existing teachers
        if (!isEditMode) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Cannot Assign Courses");
            warning.setHeaderText("Course Assignment Not Available for New Teachers");
            warning.setContentText(
                "Please save the teacher first, then use one of these methods to assign courses:\n\n" +
                "1. Edit the teacher and use 'Assign Course' button\n" +
                "2. Go to Courses view and assign teacher to courses\n" +
                "3. Use 'Smart Assignment' feature in Teachers view\n" +
                "4. Use 'Auto-Assignment' feature\n\n" +
                "This prevents accidentally reassigning courses from other teachers."
            );
            warning.showAndWait();
            return;
        }

        // Get all available courses from database
        List<Course> allCourses = courseRepository.findByActiveTrue();

        if (allCourses == null || allCourses.isEmpty()) {
            showError("No active courses found in the database. Please add courses first.");
            return;
        }

        // Filter out courses already assigned
        List<Course> availableCourses = allCourses.stream()
                .filter(course -> !assignedCourses.contains(course))
                .collect(java.util.stream.Collectors.toList());

        if (availableCourses.isEmpty()) {
            showError("All courses have already been assigned to this teacher.");
            return;
        }

        // Create dialog with list of courses
        Dialog<List<Course>> dialog = new Dialog<>();
        dialog.setTitle("Assign Courses to Teacher");
        dialog.setHeaderText("Select courses this teacher is qualified to teach");

        ButtonType assignButtonType = new ButtonType("Assign Selected", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assignButtonType, ButtonType.CANCEL);

        // Create a ListView with checkboxes for multi-select
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(20));

        Label instructionLabel = new Label("Select one or more courses:");
        instructionLabel.setStyle("-fx-font-weight: bold;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search courses by code, name, or subject...");
        searchField.setPrefWidth(500);

        ListView<Course> courseListView = new ListView<>();
        courseListView.setPrefHeight(400);
        courseListView.setPrefWidth(500);
        courseListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ObservableList<Course> coursesObservableList = FXCollections.observableArrayList(availableCourses);
        courseListView.setItems(coursesObservableList);

        // Custom cell factory to show course details
        courseListView.setCellFactory(lv -> new ListCell<Course>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (%s) - Level: %s",
                        course.getCourseCode(),
                        course.getCourseName(),
                        course.getSubject() != null ? course.getSubject() : "N/A",
                        course.getLevel() != null ? course.getLevel().name() : "N/A"));
                }
            }
        });

        // Add search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                courseListView.setItems(coursesObservableList);
            } else {
                String searchLower = newVal.toLowerCase();
                ObservableList<Course> filtered = coursesObservableList.filtered(course ->
                    course.getCourseCode().toLowerCase().contains(searchLower) ||
                    course.getCourseName().toLowerCase().contains(searchLower) ||
                    (course.getSubject() != null && course.getSubject().toLowerCase().contains(searchLower))
                );
                courseListView.setItems(filtered);
            }
        });

        Label helpLabel = new Label("Tip: Hold Ctrl (or Cmd on Mac) to select multiple courses");
        helpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        vbox.getChildren().addAll(instructionLabel, searchField, courseListView, helpLabel);
        dialog.getDialogPane().setContent(vbox);

        // Convert result when Assign button clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == assignButtonType) {
                return courseListView.getSelectionModel().getSelectedItems().stream()
                    .collect(java.util.stream.Collectors.toList());
            }
            return null;
        });

        // Show dialog and add selected courses
        dialog.showAndWait().ifPresent(selectedCourses -> {
            if (selectedCourses != null && !selectedCourses.isEmpty()) {
                // IMPORTANT: In edit mode, save assignments immediately to database
                // This ensures they persist if dialog is closed and reopened
                if (isEditMode && teacher != null) {
                    try {
                        // Add courses to teacher and save
                        // FIX: Reload course from database to avoid "detached entity" error
                        for (Course course : selectedCourses) {
                            // Reload the course to attach it to current Hibernate session
                            Course managedCourse = courseRepository.findById(course.getId())
                                .orElseThrow(() -> new RuntimeException("Course not found: " + course.getId()));

                            managedCourse.setTeacher(teacher);
                            teacher.getCourses().add(managedCourse);
                            courseRepository.save(managedCourse);
                        }
                        teacherRepository.save(teacher);

                        // Update UI list (use managed courses)
                        // Reload teacher's courses to get managed entities
                        Teacher refreshedTeacher = teacherRepository.findById(teacher.getId())
                            .orElseThrow(() -> new RuntimeException("Teacher not found"));
                        assignedCourses.clear();
                        assignedCourses.addAll(refreshedTeacher.getCourses());

                        showSuccess(String.format("Successfully assigned %d course(s) to %s",
                            selectedCourses.size(), teacher.getName()));
                    } catch (Exception e) {
                        showError("Error assigning courses: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    // In add mode, just update UI list (will be saved when teacher is saved)
                    assignedCourses.addAll(selectedCourses);
                }
            }
        });
    }

    // ========================================================================
    // COMPLIANCE GRID POPULATION METHODS (Phase: Teacher Personnel)
    // ========================================================================

    /**
     * Populate Certification Grid (Sub-Tab 1)
     */
    private void populateCertificationGrid() {
        int row = 0;

        // State Certification Number
        certificationGrid.add(new Label("* State Certification Number:"), 0, row);
        TextField certNumField = new TextField();
        certNumField.setId("stateCertificationNumberField");
        certNumField.setPromptText("e.g., 1234567");
        certNumField.setPrefWidth(300);
        certificationGrid.add(certNumField, 1, row++);

        // Certification State
        certificationGrid.add(new Label("* Certification State:"), 0, row);
        ComboBox<String> certStateCombo = new ComboBox<>();
        certStateCombo.setId("certificationStateCombo");
        certStateCombo.getItems().addAll("FL", "CA", "TX", "NY", "GA", "NC", "SC", "AL", "TN", "Other");
        certStateCombo.setEditable(true);
        certStateCombo.setPrefWidth(300);
        certificationGrid.add(certStateCombo, 1, row++);

        // Certification Issue Date
        certificationGrid.add(new Label("Certification Issue Date:"), 0, row);
        DatePicker issueDate = new DatePicker();
        issueDate.setId("certificationIssueDatePicker");
        issueDate.setPrefWidth(300);
        certificationGrid.add(issueDate, 1, row++);

        // Certification Expiration Date
        certificationGrid.add(new Label("* Certification Expiration Date:"), 0, row);
        DatePicker expirationDate = new DatePicker();
        expirationDate.setId("certificationExpirationDatePicker");
        expirationDate.setPrefWidth(300);
        certificationGrid.add(expirationDate, 1, row++);

        // Certification Status
        certificationGrid.add(new Label("* Certification Status:"), 0, row);
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setId("certificationStatusCombo");
        statusCombo.getItems().addAll("Active", "Expired", "Suspended", "Revoked", "Pending");
        statusCombo.setValue("Active");
        statusCombo.setPrefWidth(300);
        certificationGrid.add(statusCombo, 1, row++);

        // Subject Areas
        certificationGrid.add(new Label("Subject Areas:"), 0, row);
        TextArea subjectAreasArea = new TextArea();
        subjectAreasArea.setId("certificationSubjectAreasArea");
        subjectAreasArea.setPromptText("e.g., Mathematics, Science, Special Education");
        subjectAreasArea.setPrefRowCount(2);
        subjectAreasArea.setPrefWidth(300);
        subjectAreasArea.setWrapText(true);
        certificationGrid.add(subjectAreasArea, 1, row++);

        // Endorsements
        certificationGrid.add(new Label("Endorsements:"), 0, row);
        TextArea endorsementsArea = new TextArea();
        endorsementsArea.setId("certificationEndorsementsArea");
        endorsementsArea.setPromptText("e.g., ESL, Gifted, Reading Specialist");
        endorsementsArea.setPrefRowCount(2);
        endorsementsArea.setPrefWidth(300);
        endorsementsArea.setWrapText(true);
        certificationGrid.add(endorsementsArea, 1, row++);

        // Document Path
        certificationGrid.add(new Label("Certification Document Path:"), 0, row);
        TextField docPathField = new TextField();
        docPathField.setId("certificationDocumentPathField");
        docPathField.setPromptText("./documents/certifications/cert-1234567.pdf");
        docPathField.setPrefWidth(300);
        certificationGrid.add(docPathField, 1, row++);
    }

    /**
     * Populate Background & I-9 Grid (Sub-Tab 2)
     */
    private void populateBackgroundGrid() {
        int row = 0;

        // Section Header: I-9 Verification
        Label i9Header = new Label("Form I-9 - Employment Eligibility Verification");
        i9Header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        backgroundGrid.add(i9Header, 0, row++, 2, 1);

        // I-9 Completion Date
        backgroundGrid.add(new Label("* I-9 Completion Date:"), 0, row);
        DatePicker i9Date = new DatePicker();
        i9Date.setId("i9CompletionDatePicker");
        i9Date.setPrefWidth(300);
        backgroundGrid.add(i9Date, 1, row++);

        // I-9 Document Type
        backgroundGrid.add(new Label("I-9 Document Type:"), 0, row);
        ComboBox<String> i9DocTypeCombo = new ComboBox<>();
        i9DocTypeCombo.setId("i9DocumentTypeCombo");
        i9DocTypeCombo.getItems().addAll("Passport", "Driver's License + Social Security Card",
                "Permanent Resident Card", "Employment Authorization Document", "Other");
        i9DocTypeCombo.setPrefWidth(300);
        backgroundGrid.add(i9DocTypeCombo, 1, row++);

        // I-9 Document Number
        backgroundGrid.add(new Label("I-9 Document Number:"), 0, row);
        TextField i9DocNumField = new TextField();
        i9DocNumField.setId("i9DocumentNumberField");
        i9DocNumField.setPromptText("Document number");
        i9DocNumField.setPrefWidth(300);
        backgroundGrid.add(i9DocNumField, 1, row++);

        // I-9 Status
        backgroundGrid.add(new Label("* I-9 Status:"), 0, row);
        ComboBox<String> i9StatusCombo = new ComboBox<>();
        i9StatusCombo.setId("i9StatusCombo");
        i9StatusCombo.getItems().addAll("Completed", "Pending", "Expired");
        i9StatusCombo.setValue("Completed");
        i9StatusCombo.setPrefWidth(300);
        backgroundGrid.add(i9StatusCombo, 1, row++);

        // Section Header: Background Checks
        Label bgHeader = new Label("Background Checks & Fingerprinting");
        bgHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-padding: 20 0 0 0;");
        backgroundGrid.add(bgHeader, 0, row++, 2, 1);

        // Background Check Date
        backgroundGrid.add(new Label("* Background Check Date:"), 0, row);
        DatePicker bgDate = new DatePicker();
        bgDate.setId("backgroundCheckDatePicker");
        bgDate.setPrefWidth(300);
        backgroundGrid.add(bgDate, 1, row++);

        // Background Check Status
        backgroundGrid.add(new Label("* Background Check Status:"), 0, row);
        ComboBox<String> bgStatusCombo = new ComboBox<>();
        bgStatusCombo.setId("backgroundCheckStatusCombo");
        bgStatusCombo.getItems().addAll("Passed", "Failed", "Pending", "Expired");
        bgStatusCombo.setValue("Passed");
        bgStatusCombo.setPrefWidth(300);
        backgroundGrid.add(bgStatusCombo, 1, row++);

        // Background Check Expiration
        backgroundGrid.add(new Label("Background Check Expiration:"), 0, row);
        DatePicker bgExpiration = new DatePicker();
        bgExpiration.setId("backgroundCheckExpirationPicker");
        bgExpiration.setPrefWidth(300);
        backgroundGrid.add(bgExpiration, 1, row++);

        // Background Check Type
        backgroundGrid.add(new Label("Background Check Type:"), 0, row);
        ComboBox<String> bgTypeCombo = new ComboBox<>();
        bgTypeCombo.setId("backgroundCheckTypeCombo");
        bgTypeCombo.getItems().addAll("FBI", "State", "Local", "All");
        bgTypeCombo.setPrefWidth(300);
        backgroundGrid.add(bgTypeCombo, 1, row++);

        // Fingerprint Date
        backgroundGrid.add(new Label("Fingerprint Date:"), 0, row);
        DatePicker fingerprintDate = new DatePicker();
        fingerprintDate.setId("fingerprintDatePicker");
        fingerprintDate.setPrefWidth(300);
        backgroundGrid.add(fingerprintDate, 1, row++);

        // Criminal History Result
        backgroundGrid.add(new Label("Criminal History Result:"), 0, row);
        ComboBox<String> criminalResultCombo = new ComboBox<>();
        criminalResultCombo.setId("criminalHistoryResultCombo");
        criminalResultCombo.getItems().addAll("Clear", "Flagged", "Under Review");
        criminalResultCombo.setValue("Clear");
        criminalResultCombo.setPrefWidth(300);
        backgroundGrid.add(criminalResultCombo, 1, row++);

        // NASDTEC Check
        CheckBox nasdtecCheck = new CheckBox("NASDTEC Clearinghouse Check Completed");
        nasdtecCheck.setId("nasdtecCheckBox");
        backgroundGrid.add(nasdtecCheck, 0, row++, 2, 1);
    }

    /**
     * Populate Health & Safety Grid (Sub-Tab 3)
     */
    private void populateHealthGrid() {
        int row = 0;

        // Section Header: TB Test
        Label tbHeader = new Label("TB Test / Health Screening");
        tbHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        healthGrid.add(tbHeader, 0, row++, 2, 1);

        // TB Test Date
        healthGrid.add(new Label("TB Test Date:"), 0, row);
        DatePicker tbDate = new DatePicker();
        tbDate.setId("tbTestDatePicker");
        tbDate.setPrefWidth(300);
        healthGrid.add(tbDate, 1, row++);

        // TB Test Result
        healthGrid.add(new Label("TB Test Result:"), 0, row);
        ComboBox<String> tbResultCombo = new ComboBox<>();
        tbResultCombo.setId("tbTestResultCombo");
        tbResultCombo.getItems().addAll("Negative", "Positive", "Pending");
        tbResultCombo.setValue("Negative");
        tbResultCombo.setPrefWidth(300);
        healthGrid.add(tbResultCombo, 1, row++);

        // TB Test Expiration
        healthGrid.add(new Label("TB Test Expiration:"), 0, row);
        DatePicker tbExpiration = new DatePicker();
        tbExpiration.setId("tbTestExpirationPicker");
        tbExpiration.setPrefWidth(300);
        healthGrid.add(tbExpiration, 1, row++);

        // Physical Exam Date
        healthGrid.add(new Label("Physical Exam Date:"), 0, row);
        DatePicker physicalDate = new DatePicker();
        physicalDate.setId("physicalExamDatePicker");
        physicalDate.setPrefWidth(300);
        healthGrid.add(physicalDate, 1, row++);

        // Physical Exam Status
        healthGrid.add(new Label("Physical Exam Status:"), 0, row);
        ComboBox<String> physicalStatusCombo = new ComboBox<>();
        physicalStatusCombo.setId("physicalExamStatusCombo");
        physicalStatusCombo.getItems().addAll("Passed", "Failed", "Pending");
        physicalStatusCombo.setValue("Passed");
        physicalStatusCombo.setPrefWidth(300);
        healthGrid.add(physicalStatusCombo, 1, row++);

        // Section Header: Emergency Contact
        Label emergencyHeader = new Label("Emergency Contact Information");
        emergencyHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-padding: 20 0 0 0;");
        healthGrid.add(emergencyHeader, 0, row++, 2, 1);

        // Emergency Contact Name
        healthGrid.add(new Label("Emergency Contact Name:"), 0, row);
        TextField emergencyNameField = new TextField();
        emergencyNameField.setId("emergencyContactNameField");
        emergencyNameField.setPromptText("Full name");
        emergencyNameField.setPrefWidth(300);
        healthGrid.add(emergencyNameField, 1, row++);

        // Emergency Contact Relationship
        healthGrid.add(new Label("Relationship:"), 0, row);
        TextField relationshipField = new TextField();
        relationshipField.setId("emergencyContactRelationshipField");
        relationshipField.setPromptText("e.g., Spouse, Parent, Sibling");
        relationshipField.setPrefWidth(300);
        healthGrid.add(relationshipField, 1, row++);

        // Emergency Contact Phone
        healthGrid.add(new Label("Emergency Contact Phone:"), 0, row);
        TextField emergencyPhoneField = new TextField();
        emergencyPhoneField.setId("emergencyContactPhoneField");
        emergencyPhoneField.setPromptText("(555) 123-4567");
        emergencyPhoneField.setPrefWidth(300);
        healthGrid.add(emergencyPhoneField, 1, row++);

        // Medical Conditions
        healthGrid.add(new Label("Medical Conditions/Allergies:"), 0, row);
        TextArea medicalArea = new TextArea();
        medicalArea.setId("medicalConditionsArea");
        medicalArea.setPromptText("Any medical conditions, allergies, or medications");
        medicalArea.setPrefRowCount(3);
        medicalArea.setPrefWidth(300);
        medicalArea.setWrapText(true);
        healthGrid.add(medicalArea, 1, row++);

        // Health Insurance Provider
        healthGrid.add(new Label("Health Insurance Provider:"), 0, row);
        TextField insuranceField = new TextField();
        insuranceField.setId("healthInsuranceProviderField");
        insuranceField.setPromptText("e.g., Blue Cross Blue Shield");
        insuranceField.setPrefWidth(300);
        healthGrid.add(insuranceField, 1, row++);

        // Health Insurance Policy Number
        healthGrid.add(new Label("Policy Number:"), 0, row);
        TextField policyNumField = new TextField();
        policyNumField.setId("healthInsurancePolicyNumberField");
        policyNumField.setPromptText("Policy number");
        policyNumField.setPrefWidth(300);
        healthGrid.add(policyNumField, 1, row++);
    }

    /**
     * Populate Education & Employment Grid (Sub-Tab 4)
     */
    private void populateEducationGrid() {
        int row = 0;

        // Section Header: Education
        Label eduHeader = new Label("Education & Degrees");
        eduHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        educationGrid.add(eduHeader, 0, row++, 2, 1);

        // Highest Degree
        educationGrid.add(new Label("Highest Degree:"), 0, row);
        ComboBox<String> degreeCombo = new ComboBox<>();
        degreeCombo.setId("highestDegreeCombo");
        degreeCombo.getItems().addAll("Bachelor's", "Master's", "Doctorate", "Associate's", "Other");
        degreeCombo.setPrefWidth(300);
        educationGrid.add(degreeCombo, 1, row++);

        // Degree Field
        educationGrid.add(new Label("Degree Field/Major:"), 0, row);
        TextField degreeFieldField = new TextField();
        degreeFieldField.setId("degreeFieldField");
        degreeFieldField.setPromptText("e.g., Mathematics, Education, Biology");
        degreeFieldField.setPrefWidth(300);
        educationGrid.add(degreeFieldField, 1, row++);

        // University
        educationGrid.add(new Label("University/College:"), 0, row);
        TextField universityField = new TextField();
        universityField.setId("universityField");
        universityField.setPromptText("Name of institution");
        universityField.setPrefWidth(300);
        educationGrid.add(universityField, 1, row++);

        // Graduation Date
        educationGrid.add(new Label("Graduation Date:"), 0, row);
        DatePicker graduationDate = new DatePicker();
        graduationDate.setId("graduationDatePicker");
        graduationDate.setPrefWidth(300);
        educationGrid.add(graduationDate, 1, row++);

        // Transcript Verified
        CheckBox transcriptCheck = new CheckBox("Official Transcript Verified");
        transcriptCheck.setId("transcriptVerifiedCheckBox");
        educationGrid.add(transcriptCheck, 0, row++, 2, 1);

        // Section Header: Employment
        Label employHeader = new Label("Employment History");
        employHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-padding: 20 0 0 0;");
        educationGrid.add(employHeader, 0, row++, 2, 1);

        // Hire Date
        educationGrid.add(new Label("Hire Date:"), 0, row);
        DatePicker hireDate = new DatePicker();
        hireDate.setId("hireDatePicker");
        hireDate.setPrefWidth(300);
        educationGrid.add(hireDate, 1, row++);

        // Start Date
        educationGrid.add(new Label("Start Date (First Day):"), 0, row);
        DatePicker startDate = new DatePicker();
        startDate.setId("startDatePicker");
        startDate.setPrefWidth(300);
        educationGrid.add(startDate, 1, row++);

        // Contract Type
        educationGrid.add(new Label("Contract Type:"), 0, row);
        ComboBox<String> contractTypeCombo = new ComboBox<>();
        contractTypeCombo.setId("contractTypeCombo");
        contractTypeCombo.getItems().addAll("Full-time", "Part-time", "Substitute", "Contract");
        contractTypeCombo.setPrefWidth(300);
        educationGrid.add(contractTypeCombo, 1, row++);

        // Employment Status
        educationGrid.add(new Label("Employment Status:"), 0, row);
        ComboBox<String> employmentStatusCombo = new ComboBox<>();
        employmentStatusCombo.setId("employmentStatusCombo");
        employmentStatusCombo.getItems().addAll("Active", "On Leave", "Resigned", "Terminated");
        employmentStatusCombo.setValue("Active");
        employmentStatusCombo.setPrefWidth(300);
        educationGrid.add(employmentStatusCombo, 1, row++);

        // Years of Experience
        educationGrid.add(new Label("Years of Experience:"), 0, row);
        Spinner<Integer> experienceSpinner = new Spinner<>(0, 50, 0);
        experienceSpinner.setId("yearsOfExperienceSpinner");
        experienceSpinner.setPrefWidth(100);
        experienceSpinner.setEditable(true);
        educationGrid.add(experienceSpinner, 1, row++);

        // Section Header: Salary
        Label salaryHeader = new Label("Salary & Compensation");
        salaryHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-padding: 20 0 0 0;");
        educationGrid.add(salaryHeader, 0, row++, 2, 1);

        // Annual Salary
        educationGrid.add(new Label("Annual Salary:"), 0, row);
        TextField salaryField = new TextField();
        salaryField.setId("salaryField");
        salaryField.setPromptText("e.g., 55000.00");
        salaryField.setPrefWidth(300);
        educationGrid.add(salaryField, 1, row++);

        // Pay Grade
        educationGrid.add(new Label("Pay Grade:"), 0, row);
        TextField payGradeField = new TextField();
        payGradeField.setId("payGradeField");
        payGradeField.setPromptText("e.g., Step 5, Column B");
        payGradeField.setPrefWidth(300);
        educationGrid.add(payGradeField, 1, row++);

        // Contracted Days
        educationGrid.add(new Label("Contracted Days/Year:"), 0, row);
        Spinner<Integer> contractedDaysSpinner = new Spinner<>(160, 240, 180);
        contractedDaysSpinner.setId("contractedDaysSpinner");
        contractedDaysSpinner.setPrefWidth(100);
        contractedDaysSpinner.setEditable(true);
        educationGrid.add(contractedDaysSpinner, 1, row++);
    }

    // ========================================================================
    // COMPLIANCE FIELDS SAVE METHOD (Phase: Teacher Personnel)
    // ========================================================================

    /**
     * Save all compliance fields from UI to Teacher entity
     */
    private void saveComplianceFields(Teacher teacher) {
        if (certificationGrid == null || backgroundGrid == null ||
            healthGrid == null || educationGrid == null) {
            return; // Grids not initialized
        }

        // ========== Certification Fields ==========
        TextField stateCertNum = (TextField) certificationGrid.lookup("#stateCertificationNumberField");
        if (stateCertNum != null && !stateCertNum.getText().trim().isEmpty()) {
            teacher.setStateCertificationNumber(stateCertNum.getText().trim());
        }

        ComboBox<String> certState = (ComboBox<String>) certificationGrid.lookup("#certificationStateCombo");
        if (certState != null && certState.getValue() != null) {
            teacher.setCertificationState(certState.getValue());
        }

        DatePicker certIssueDate = (DatePicker) certificationGrid.lookup("#certificationIssueDatePicker");
        if (certIssueDate != null) {
            teacher.setCertificationIssueDate(certIssueDate.getValue());
        }

        DatePicker certExpDate = (DatePicker) certificationGrid.lookup("#certificationExpirationDatePicker");
        if (certExpDate != null) {
            teacher.setCertificationExpirationDate(certExpDate.getValue());
        }

        ComboBox<String> certStatus = (ComboBox<String>) certificationGrid.lookup("#certificationStatusCombo");
        if (certStatus != null && certStatus.getValue() != null) {
            teacher.setCertificationStatus(certStatus.getValue());
        }

        TextArea subjectAreas = (TextArea) certificationGrid.lookup("#certificationSubjectAreasArea");
        if (subjectAreas != null && !subjectAreas.getText().trim().isEmpty()) {
            teacher.setCertificationSubjectAreas(subjectAreas.getText().trim());
        }

        TextArea endorsements = (TextArea) certificationGrid.lookup("#certificationEndorsementsArea");
        if (endorsements != null && !endorsements.getText().trim().isEmpty()) {
            teacher.setCertificationEndorsements(endorsements.getText().trim());
        }

        TextField certDocPath = (TextField) certificationGrid.lookup("#certificationDocumentPathField");
        if (certDocPath != null && !certDocPath.getText().trim().isEmpty()) {
            teacher.setCertificationDocumentPath(certDocPath.getText().trim());
        }

        // ========== I-9 Fields ==========
        DatePicker i9Date = (DatePicker) backgroundGrid.lookup("#i9CompletionDatePicker");
        if (i9Date != null) {
            teacher.setI9CompletionDate(i9Date.getValue());
        }

        ComboBox<String> i9DocType = (ComboBox<String>) backgroundGrid.lookup("#i9DocumentTypeCombo");
        if (i9DocType != null && i9DocType.getValue() != null) {
            teacher.setI9DocumentType(i9DocType.getValue());
        }

        TextField i9DocNum = (TextField) backgroundGrid.lookup("#i9DocumentNumberField");
        if (i9DocNum != null && !i9DocNum.getText().trim().isEmpty()) {
            teacher.setI9DocumentNumber(i9DocNum.getText().trim());
        }

        ComboBox<String> i9Status = (ComboBox<String>) backgroundGrid.lookup("#i9StatusCombo");
        if (i9Status != null && i9Status.getValue() != null) {
            teacher.setI9Status(i9Status.getValue());
        }

        // ========== Background Check Fields ==========
        DatePicker bgDate = (DatePicker) backgroundGrid.lookup("#backgroundCheckDatePicker");
        if (bgDate != null) {
            teacher.setBackgroundCheckDate(bgDate.getValue());
        }

        ComboBox<String> bgStatus = (ComboBox<String>) backgroundGrid.lookup("#backgroundCheckStatusCombo");
        if (bgStatus != null && bgStatus.getValue() != null) {
            teacher.setBackgroundCheckStatus(bgStatus.getValue());
        }

        DatePicker bgExpiration = (DatePicker) backgroundGrid.lookup("#backgroundCheckExpirationPicker");
        if (bgExpiration != null) {
            teacher.setBackgroundCheckExpiration(bgExpiration.getValue());
        }

        ComboBox<String> bgType = (ComboBox<String>) backgroundGrid.lookup("#backgroundCheckTypeCombo");
        if (bgType != null && bgType.getValue() != null) {
            teacher.setBackgroundCheckType(bgType.getValue());
        }

        DatePicker fingerprintDate = (DatePicker) backgroundGrid.lookup("#fingerprintDatePicker");
        if (fingerprintDate != null) {
            teacher.setFingerprintDate(fingerprintDate.getValue());
        }

        ComboBox<String> criminalResult = (ComboBox<String>) backgroundGrid.lookup("#criminalHistoryResultCombo");
        if (criminalResult != null && criminalResult.getValue() != null) {
            teacher.setCriminalHistoryResult(criminalResult.getValue());
        }

        CheckBox nasdtec = (CheckBox) backgroundGrid.lookup("#nasdtecCheckBox");
        if (nasdtec != null) {
            teacher.setNasdtecCheck(nasdtec.isSelected());
        }

        // ========== Health & Safety Fields ==========
        DatePicker tbDate = (DatePicker) healthGrid.lookup("#tbTestDatePicker");
        if (tbDate != null) {
            teacher.setTbTestDate(tbDate.getValue());
        }

        ComboBox<String> tbResult = (ComboBox<String>) healthGrid.lookup("#tbTestResultCombo");
        if (tbResult != null && tbResult.getValue() != null) {
            teacher.setTbTestResult(tbResult.getValue());
        }

        DatePicker tbExpiration = (DatePicker) healthGrid.lookup("#tbTestExpirationPicker");
        if (tbExpiration != null) {
            teacher.setTbTestExpiration(tbExpiration.getValue());
        }

        DatePicker physicalDate = (DatePicker) healthGrid.lookup("#physicalExamDatePicker");
        if (physicalDate != null) {
            teacher.setPhysicalExamDate(physicalDate.getValue());
        }

        ComboBox<String> physicalStatus = (ComboBox<String>) healthGrid.lookup("#physicalExamStatusCombo");
        if (physicalStatus != null && physicalStatus.getValue() != null) {
            teacher.setPhysicalExamStatus(physicalStatus.getValue());
        }

        // Emergency Contact
        TextField emergencyName = (TextField) healthGrid.lookup("#emergencyContactNameField");
        if (emergencyName != null && !emergencyName.getText().trim().isEmpty()) {
            teacher.setEmergencyContactName(emergencyName.getText().trim());
        }

        TextField relationship = (TextField) healthGrid.lookup("#emergencyContactRelationshipField");
        if (relationship != null && !relationship.getText().trim().isEmpty()) {
            teacher.setEmergencyContactRelationship(relationship.getText().trim());
        }

        TextField emergencyPhone = (TextField) healthGrid.lookup("#emergencyContactPhoneField");
        if (emergencyPhone != null && !emergencyPhone.getText().trim().isEmpty()) {
            teacher.setEmergencyContactPhone(emergencyPhone.getText().trim());
        }

        TextArea medical = (TextArea) healthGrid.lookup("#medicalConditionsArea");
        if (medical != null && !medical.getText().trim().isEmpty()) {
            teacher.setMedicalConditions(medical.getText().trim());
        }

        TextField insurance = (TextField) healthGrid.lookup("#healthInsuranceProviderField");
        if (insurance != null && !insurance.getText().trim().isEmpty()) {
            teacher.setHealthInsuranceProvider(insurance.getText().trim());
        }

        TextField policyNum = (TextField) healthGrid.lookup("#healthInsurancePolicyNumberField");
        if (policyNum != null && !policyNum.getText().trim().isEmpty()) {
            teacher.setHealthInsurancePolicyNumber(policyNum.getText().trim());
        }

        // ========== Education & Employment Fields ==========
        ComboBox<String> degree = (ComboBox<String>) educationGrid.lookup("#highestDegreeCombo");
        if (degree != null && degree.getValue() != null) {
            teacher.setHighestDegree(degree.getValue());
        }

        TextField degreeField = (TextField) educationGrid.lookup("#degreeFieldField");
        if (degreeField != null && !degreeField.getText().trim().isEmpty()) {
            teacher.setDegreeField(degreeField.getText().trim());
        }

        TextField university = (TextField) educationGrid.lookup("#universityField");
        if (university != null && !university.getText().trim().isEmpty()) {
            teacher.setUniversity(university.getText().trim());
        }

        DatePicker graduationDate = (DatePicker) educationGrid.lookup("#graduationDatePicker");
        if (graduationDate != null) {
            teacher.setGraduationDate(graduationDate.getValue());
        }

        CheckBox transcriptVerified = (CheckBox) educationGrid.lookup("#transcriptVerifiedCheckBox");
        if (transcriptVerified != null) {
            teacher.setTranscriptVerified(transcriptVerified.isSelected());
            if (transcriptVerified.isSelected()) {
                teacher.setTranscriptVerificationDate(LocalDate.now());
            }
        }

        // Employment
        DatePicker hireDate = (DatePicker) educationGrid.lookup("#hireDatePicker");
        if (hireDate != null) {
            teacher.setHireDate(hireDate.getValue());
        }

        DatePicker startDate = (DatePicker) educationGrid.lookup("#startDatePicker");
        if (startDate != null) {
            teacher.setStartDate(startDate.getValue());
        }

        ComboBox<String> contractType = (ComboBox<String>) educationGrid.lookup("#contractTypeCombo");
        if (contractType != null && contractType.getValue() != null) {
            teacher.setContractType(contractType.getValue());
        }

        ComboBox<String> employmentStatus = (ComboBox<String>) educationGrid.lookup("#employmentStatusCombo");
        if (employmentStatus != null && employmentStatus.getValue() != null) {
            teacher.setEmploymentStatus(employmentStatus.getValue());
        }

        Spinner<Integer> experience = (Spinner<Integer>) educationGrid.lookup("#yearsOfExperienceSpinner");
        if (experience != null) {
            teacher.setYearsOfExperience(experience.getValue());
        }

        // Salary
        TextField salaryField = (TextField) educationGrid.lookup("#salaryField");
        if (salaryField != null && !salaryField.getText().trim().isEmpty()) {
            try {
                teacher.setSalary(new java.math.BigDecimal(salaryField.getText().trim()));
            } catch (NumberFormatException e) {
                // Invalid number, skip
            }
        }

        TextField payGrade = (TextField) educationGrid.lookup("#payGradeField");
        if (payGrade != null && !payGrade.getText().trim().isEmpty()) {
            teacher.setPayGrade(payGrade.getText().trim());
        }

        Spinner<Integer> contractedDays = (Spinner<Integer>) educationGrid.lookup("#contractedDaysSpinner");
        if (contractedDays != null) {
            teacher.setContractedDays(contractedDays.getValue());
        }
    }
}