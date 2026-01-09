package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import com.heronix.service.ConflictDetectionService;
import com.heronix.service.NotificationService;
import com.heronix.model.domain.Notification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Substitute Assignment Dialog
 * Handles creation of new substitute assignments with comprehensive validation
 *
 * @author Heronix Scheduling System Team
 * @version 1.5.0
 * @since Phase 2
 */
@Slf4j
@Component
public class SubstituteAssignmentDialogController {

    // ========================================================================
    // FXML FIELDS
    // ========================================================================

    @FXML private Label dialogTitle;

    // Assignment Details
    @FXML private DatePicker assignmentDatePicker;
    @FXML private ComboBox<String> startTimeCombo;
    @FXML private ComboBox<String> endTimeCombo;
    @FXML private CheckBox fullDayCheckbox;
    @FXML private ComboBox<String> reasonCombo;

    // Absent Staff
    @FXML private ComboBox<String> staffTypeCombo;
    @FXML private HBox teacherSelectorBox;
    @FXML private ComboBox<Teacher> absentTeacherCombo;
    @FXML private HBox staffNameBox;
    @FXML private TextField staffNameField;
    @FXML private VBox teacherScheduleBox;
    @FXML private TextArea teacherScheduleArea;

    // Substitute Selection
    @FXML private ComboBox<Substitute> substituteCombo;
    @FXML private CheckBox onlyAvailableCheckbox;
    @FXML private CheckBox sameDepartmentCheckbox;
    @FXML private CheckBox certifiedOnlyCheckbox;
    @FXML private VBox substituteInfoBox;
    @FXML private Label subDepartmentLabel;
    @FXML private Label subCertificationsLabel;
    @FXML private Label subWeekHoursLabel;
    @FXML private Label subMonthHoursLabel;
    @FXML private VBox conflictsBox;
    @FXML private TextArea conflictsArea;

    // Additional Options
    @FXML private TextArea notesArea;
    @FXML private CheckBox notifySubstituteCheckbox;
    @FXML private CheckBox notifyAdminCheckbox;
    @FXML private CheckBox notifyDepartmentCheckbox;

    // Summary
    @FXML private TextArea summaryArea;

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired private SubstituteRepository substituteRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SubstituteAssignmentRepository substituteAssignmentRepository;
    @Autowired private ConflictDetectionService conflictDetectionService;
    @Autowired private ScheduleSlotRepository scheduleSlotRepository;
    @Autowired(required = false) private NotificationService notificationService;

    // ========================================================================
    // STATE
    // ========================================================================

    private Stage dialogStage;
    private SubstituteAssignment assignment;
    private boolean saved = false;
    private SubstituteAssignment existingAssignment; // For edit mode

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing SubstituteAssignmentDialogController");

        setupTimeComboBoxes();
        setupReasonComboBox();
        setupStaffTypeComboBox();
        setupTeacherComboBox();
        setupSubstituteComboBox();
        setupDatePickerDefaults();

        updateSummary();
    }

    private void setupTimeComboBoxes() {
        List<String> times = new ArrayList<>();
        for (int hour = 7; hour <= 17; hour++) {
            times.add(String.format("%02d:00", hour));
            times.add(String.format("%02d:30", hour));
        }

        startTimeCombo.setItems(FXCollections.observableArrayList(times));
        endTimeCombo.setItems(FXCollections.observableArrayList(times));

        // Set defaults
        startTimeCombo.setValue("08:00");
        endTimeCombo.setValue("15:00");
    }

    private void setupReasonComboBox() {
        reasonCombo.setItems(FXCollections.observableArrayList(
            "Sick Leave",
            "Personal Day",
            "Professional Development",
            "Family Emergency",
            "Medical Appointment",
            "Jury Duty",
            "Bereavement",
            "Other"
        ));
        reasonCombo.setValue("Sick Leave");
    }

    private void setupStaffTypeComboBox() {
        staffTypeCombo.setItems(FXCollections.observableArrayList(
            "TEACHER",
            "CO_TEACHER",
            "PARAPROFESSIONAL",
            "OTHER"
        ));
        staffTypeCombo.setValue("TEACHER");
    }

    private void setupTeacherComboBox() {
        List<Teacher> teachers = teacherRepository.findByActiveTrue();
        absentTeacherCombo.setItems(FXCollections.observableArrayList(teachers));

        // Custom cell factory to show teacher name and department
        absentTeacherCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) {
                    setText(null);
                } else {
                    setText(teacher.getName() + " (" + teacher.getDepartment() + ")");
                }
            }
        });

        absentTeacherCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) {
                    setText(null);
                } else {
                    setText(teacher.getName() + " (" + teacher.getDepartment() + ")");
                }
            }
        });
    }

    private void setupSubstituteComboBox() {
        loadSubstitutes();

        // Custom cell factory for substitutes
        substituteCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Substitute sub, boolean empty) {
                super.updateItem(sub, empty);
                if (empty || sub == null) {
                    setText(null);
                } else {
                    String availability = sub.getActive() ? "✓ Available" : "✗ Busy";
                    setText(String.format("%s - %s", sub.getFullName(), availability));
                }
            }
        });

        substituteCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Substitute sub, boolean empty) {
                super.updateItem(sub, empty);
                if (empty || sub == null) {
                    setText(null);
                } else {
                    setText(sub.getFullName());
                }
            }
        });
    }

    private void setupDatePickerDefaults() {
        // Set today as default
        assignmentDatePicker.setValue(LocalDate.now());
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    @FXML
    private void handleFullDayToggle() {
        boolean fullDay = fullDayCheckbox.isSelected();
        startTimeCombo.setDisable(fullDay);
        endTimeCombo.setDisable(fullDay);

        if (fullDay) {
            startTimeCombo.setValue("08:00");
            endTimeCombo.setValue("15:00");
        }

        updateSummary();
    }

    @FXML
    private void handleStaffTypeChange() {
        String staffType = staffTypeCombo.getValue();
        boolean isTeacher = "TEACHER".equals(staffType);

        // Show/hide appropriate fields
        teacherSelectorBox.setVisible(isTeacher);
        teacherSelectorBox.setManaged(isTeacher);
        staffNameBox.setVisible(!isTeacher);
        staffNameBox.setManaged(!isTeacher);

        updateSummary();
    }

    @FXML
    private void handleTeacherSelection() {
        Teacher teacher = absentTeacherCombo.getValue();
        if (teacher != null && assignmentDatePicker.getValue() != null) {
            loadTeacherSchedule(teacher, assignmentDatePicker.getValue());
        }
        updateSummary();
    }

    @FXML
    private void handleSubstituteSelection() {
        Substitute substitute = substituteCombo.getValue();
        if (substitute != null) {
            showSubstituteInfo(substitute);
            checkForConflicts();
        }
        updateSummary();
    }

    @FXML
    private void handleFilterChange() {
        loadSubstitutes();
    }

    @FXML
    private void handleShowAllSubstitutes() {
        onlyAvailableCheckbox.setSelected(false);
        sameDepartmentCheckbox.setSelected(false);
        certifiedOnlyCheckbox.setSelected(false);
        loadSubstitutes();
    }

    @FXML
    private void handlePreview() {
        if (!validateForm()) {
            return;
        }

        // Build preview message
        StringBuilder preview = new StringBuilder();
        preview.append("=== SUBSTITUTE ASSIGNMENT PREVIEW ===\n\n");

        preview.append("Date: ").append(assignmentDatePicker.getValue()).append("\n");
        preview.append("Time: ").append(startTimeCombo.getValue())
               .append(" - ").append(endTimeCombo.getValue()).append("\n");
        preview.append("Reason: ").append(reasonCombo.getValue()).append("\n\n");

        if ("TEACHER".equals(staffTypeCombo.getValue())) {
            Teacher teacher = absentTeacherCombo.getValue();
            preview.append("Absent Teacher: ").append(teacher.getName()).append("\n");
            preview.append("Department: ").append(teacher.getDepartment()).append("\n");
        } else {
            preview.append("Absent Staff: ").append(staffNameField.getText()).append("\n");
            preview.append("Type: ").append(staffTypeCombo.getValue()).append("\n");
        }

        Substitute sub = substituteCombo.getValue();
        preview.append("\nSubstitute: ").append(sub.getFullName()).append("\n");

        if (!notesArea.getText().isEmpty()) {
            preview.append("\nNotes: ").append(notesArea.getText()).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Assignment Preview");
        alert.setHeaderText("Review Substitute Assignment");
        alert.setContentText(preview.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleSave() {
        log.info("Save substitute assignment clicked");

        if (!validateForm()) {
            return;
        }

        try {
            // Create or update assignment
            if (assignment == null) {
                assignment = new SubstituteAssignment();
            }

            // Set basic fields
            assignment.setAssignmentDate(assignmentDatePicker.getValue());
            assignment.setStartTime(LocalTime.parse(startTimeCombo.getValue()));
            assignment.setEndTime(LocalTime.parse(endTimeCombo.getValue()));

            // Set duration type based on full day checkbox
            AssignmentDuration durationType = fullDayCheckbox.isSelected()
                ? AssignmentDuration.FULL_DAY
                : AssignmentDuration.HOURLY;
            assignment.setDurationType(durationType);

            // Map string reason to AbsenceReason enum
            AbsenceReason absenceReason = mapReasonStringToEnum(reasonCombo.getValue());
            assignment.setAbsenceReason(absenceReason);
            assignment.setNotes(notesArea.getText());

            // Set staff type
            StaffType staffType = StaffType.valueOf(staffTypeCombo.getValue());
            assignment.setReplacedStaffType(staffType);

            // Set replaced teacher or staff name
            if (staffType == StaffType.TEACHER) {
                assignment.setReplacedTeacher(absentTeacherCombo.getValue());
            } else {
                assignment.setReplacedStaffName(staffNameField.getText());
            }

            // Set substitute
            assignment.setSubstitute(substituteCombo.getValue());

            // Set status and confirmation date
            assignment.setStatus(AssignmentStatus.CONFIRMED);
            assignment.setDateConfirmed(LocalDateTime.now());

            // Set assignment source as MANUAL (required field)
            assignment.setAssignmentSource(AssignmentSource.MANUAL);

            // Save to database
            substituteAssignmentRepository.save(assignment);

            log.info("Substitute assignment saved successfully: {}", assignment.getId());

            // Send notifications if requested
            sendNotifications(assignment);

            saved = true;
            showSuccess("Assignment Saved", "Substitute assignment has been saved successfully!");
            closeDialog();

        } catch (Exception e) {
            log.error("Error saving substitute assignment", e);
            showError("Save Failed", "Failed to save substitute assignment: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (hasUnsavedChanges()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes");
            alert.setContentText("Are you sure you want to close without saving?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    closeDialog();
                }
            });
        } else {
            closeDialog();
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void loadSubstitutes() {
        List<Substitute> substitutes = substituteRepository.findAll();

        // Apply filters
        if (onlyAvailableCheckbox.isSelected()) {
            substitutes = substitutes.stream()
                .filter(Substitute::getActive)
                .collect(Collectors.toList());
        }

        // Note: Department filtering removed - Substitute entity doesn't have department field
        // if (sameDepartmentCheckbox.isSelected() && absentTeacherCombo.getValue() != null) {
        //     String department = absentTeacherCombo.getValue().getDepartment();
        //     substitutes = substitutes.stream()
        //         .filter(sub -> department.equals(sub.getDepartment()))
        //         .collect(Collectors.toList());
        // }

        if (certifiedOnlyCheckbox.isSelected()) {
            // Check if substitute has any certifications (instead of getCertified() which doesn't exist)
            substitutes = substitutes.stream()
                .filter(sub -> sub.getCertifications() != null && !sub.getCertifications().isEmpty())
                .collect(Collectors.toList());
        }

        substituteCombo.setItems(FXCollections.observableArrayList(substitutes));
    }

    private void loadTeacherSchedule(Teacher teacher, LocalDate date) {
        if (teacher == null || date == null) {
            teacherScheduleBox.setVisible(false);
            teacherScheduleBox.setManaged(false);
            return;
        }

        try {
            List<ScheduleSlot> slots = scheduleSlotRepository.findByTeacherAndDate(teacher.getId(), date);

            if (slots.isEmpty()) {
                teacherScheduleArea.setText("No classes scheduled for this date.");
                teacherScheduleBox.setVisible(true);
                teacherScheduleBox.setManaged(true);
            } else {
                StringBuilder schedule = new StringBuilder();
                schedule.append("Teacher's Schedule for ").append(date).append(":\n\n");

                slots.forEach(slot -> {
                    String startTime = slot.getStartTime() != null ? slot.getStartTime().toString() : "TBD";
                    String endTime = slot.getEndTime() != null ? slot.getEndTime().toString() : "TBD";
                    String courseName = slot.getCourse() != null ? slot.getCourse().getCourseName() : "Unknown Course";
                    String roomNumber = (slot.getRoom() != null && slot.getRoom().getRoomNumber() != null)
                        ? slot.getRoom().getRoomNumber()
                        : "TBD";
                    String dayOfWeek = slot.getDayOfWeek() != null ? slot.getDayOfWeek().toString() : "";

                    schedule.append(String.format("%s %s - %s: %s (Room %s)\n",
                        dayOfWeek,
                        startTime,
                        endTime,
                        courseName,
                        roomNumber
                    ));
                });

                teacherScheduleArea.setText(schedule.toString());
                teacherScheduleBox.setVisible(true);
                teacherScheduleBox.setManaged(true);
            }
        } catch (Exception e) {
            log.error("Error loading teacher schedule: {}", e.getMessage(), e);
            teacherScheduleArea.setText("Error loading teacher schedule. Please try again.");
            teacherScheduleBox.setVisible(true);
            teacherScheduleBox.setManaged(true);
        }
    }

    private void showSubstituteInfo(Substitute substitute) {
        // Note: Department field doesn't exist in Substitute entity
        subDepartmentLabel.setText("N/A");

        String certs = substitute.getCertifications() != null && !substitute.getCertifications().isEmpty()
            ? String.join(", ", substitute.getCertifications())
            : "None";
        subCertificationsLabel.setText(certs);

        // Get assignment counts (simplified - could be enhanced with actual service calls)
        subWeekHoursLabel.setText("0 hours"); // Placeholder
        subMonthHoursLabel.setText("0 hours"); // Placeholder

        substituteInfoBox.setVisible(true);
        substituteInfoBox.setManaged(true);
    }

    private void checkForConflicts() {
        Substitute substitute = substituteCombo.getValue();
        LocalDate date = assignmentDatePicker.getValue();

        if (substitute == null || date == null) {
            conflictsBox.setVisible(false);
            conflictsBox.setManaged(false);
            return;
        }

        try {
            // Use existing repository method - it DOES exist!
            List<SubstituteAssignment> existingAssignments =
                substituteAssignmentRepository.findBySubstituteAndAssignmentDate(substitute, date);

            if (!existingAssignments.isEmpty()) {
                StringBuilder conflicts = new StringBuilder();
                conflicts.append("⚠️ Substitute already has assignments on this date:\n\n");

                existingAssignments.forEach(assignment -> {
                    String teacher = assignment.getReplacedTeacher() != null
                        ? assignment.getReplacedTeacher().getName()
                        : "Unknown Teacher";
                    String startTime = assignment.getStartTime() != null
                        ? assignment.getStartTime().toString()
                        : "All Day";
                    String endTime = assignment.getEndTime() != null
                        ? assignment.getEndTime().toString()
                        : "";
                    String status = assignment.getStatus() != null
                        ? assignment.getStatus().toString()
                        : "PENDING";

                    if (assignment.getIsFloater() != null && assignment.getIsFloater()) {
                        conflicts.append(String.format("• Floater Assignment - Status: %s\n", status));
                    } else {
                        conflicts.append(String.format("• %s - %s (replacing %s) - Status: %s\n",
                            startTime,
                            endTime.isEmpty() ? "End of Day" : endTime,
                            teacher,
                            status));
                    }
                });

                conflictsArea.setText(conflicts.toString());
                conflictsBox.setVisible(true);
                conflictsBox.setManaged(true);
            } else {
                conflictsBox.setVisible(false);
                conflictsBox.setManaged(false);
            }
        } catch (Exception e) {
            log.error("Error checking for conflicts: {}", e.getMessage(), e);
            conflictsBox.setVisible(false);
            conflictsBox.setManaged(false);
        }
    }

    private void updateSummary() {
        StringBuilder summary = new StringBuilder();

        if (assignmentDatePicker.getValue() != null) {
            summary.append("Assignment Date: ")
                   .append(assignmentDatePicker.getValue().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")))
                   .append("\n");
        }

        if (startTimeCombo.getValue() != null && endTimeCombo.getValue() != null) {
            summary.append("Time: ")
                   .append(startTimeCombo.getValue())
                   .append(" - ")
                   .append(endTimeCombo.getValue());
            if (fullDayCheckbox.isSelected()) {
                summary.append(" (Full Day)");
            }
            summary.append("\n");
        }

        if ("TEACHER".equals(staffTypeCombo.getValue()) && absentTeacherCombo.getValue() != null) {
            summary.append("Replacing: ").append(absentTeacherCombo.getValue().getName()).append("\n");
        } else if (!staffNameField.getText().isEmpty()) {
            summary.append("Replacing: ").append(staffNameField.getText()).append("\n");
        }

        if (substituteCombo.getValue() != null) {
            summary.append("Substitute: ").append(substituteCombo.getValue().getFullName()).append("\n");
        }

        summaryArea.setText(summary.toString());
    }

    private boolean validateForm() {
        List<String> errors = new ArrayList<>();

        if (assignmentDatePicker.getValue() == null) {
            errors.add("Assignment date is required");
        }

        if (startTimeCombo.getValue() == null || endTimeCombo.getValue() == null) {
            errors.add("Start and end times are required");
        }

        if (staffTypeCombo.getValue() == null) {
            errors.add("Staff type is required");
        } else if ("TEACHER".equals(staffTypeCombo.getValue()) && absentTeacherCombo.getValue() == null) {
            errors.add("Please select the absent teacher");
        } else if (!"TEACHER".equals(staffTypeCombo.getValue()) && staffNameField.getText().trim().isEmpty()) {
            errors.add("Please enter the staff member name");
        }

        if (substituteCombo.getValue() == null) {
            errors.add("Please select a substitute");
        }

        if (!errors.isEmpty()) {
            showError("Validation Error", String.join("\n", errors));
            return false;
        }

        return true;
    }

    private boolean hasUnsavedChanges() {
        // Check if any fields have been modified
        return assignmentDatePicker.getValue() != null ||
               absentTeacherCombo.getValue() != null ||
               substituteCombo.getValue() != null ||
               !notesArea.getText().isEmpty();
    }

    // ========================================================================
    // PUBLIC METHODS
    // ========================================================================

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setAssignment(SubstituteAssignment assignment) {
        this.existingAssignment = assignment;
        this.assignment = assignment;
        dialogTitle.setText("Edit Substitute Assignment");

        // Populate fields from existing assignment
        populateFieldsFromAssignment(assignment);
    }

    /**
     * Populate form fields from existing assignment for edit mode
     */
    private void populateFieldsFromAssignment(SubstituteAssignment assignment) {
        if (assignment == null) {
            return;
        }

        try {
            // Set date
            if (assignment.getAssignmentDate() != null) {
                assignmentDatePicker.setValue(assignment.getAssignmentDate());
            }

            // Set staff type
            if (assignment.getReplacedStaffType() != null) {
                staffTypeCombo.setValue(assignment.getReplacedStaffType().toString());
            }

            // Set replaced teacher if applicable
            if (assignment.getReplacedTeacher() != null) {
                absentTeacherCombo.setValue(assignment.getReplacedTeacher());
            }

            // Set staff name if not a teacher
            if (assignment.getReplacedStaffName() != null && !assignment.getReplacedStaffName().isEmpty()) {
                staffNameField.setText(assignment.getReplacedStaffName());
            }

            // Set substitute
            if (assignment.getSubstitute() != null) {
                substituteCombo.setValue(assignment.getSubstitute());
            }

            // Set times
            if (assignment.getStartTime() != null) {
                startTimeCombo.setValue(assignment.getStartTime().toString());
            }
            if (assignment.getEndTime() != null) {
                endTimeCombo.setValue(assignment.getEndTime().toString());
            }

            // Set absence reason
            if (assignment.getAbsenceReason() != null) {
                String reasonDisplay = mapEnumToReasonString(assignment.getAbsenceReason());
                reasonCombo.setValue(reasonDisplay);
            }

            // Set notes
            if (assignment.getNotes() != null && !assignment.getNotes().isEmpty()) {
                notesArea.setText(assignment.getNotes());
            }

            // Update summary after populating fields
            updateSummary();

        } catch (Exception e) {
            log.error("Error populating fields from assignment: {}", e.getMessage(), e);
        }
    }

    /**
     * Maps AbsenceReason enum to display string for ComboBox
     */
    private String mapEnumToReasonString(com.heronix.model.enums.AbsenceReason reason) {
        if (reason == null) {
            return "Other";
        }

        switch (reason) {
            case SICK_LEAVE:
                return "Sick Leave";
            case PERSONAL_DAY:
                return "Personal Day";
            case PROFESSIONAL_DEV:
                return "Professional Development";
            case FAMILY_EMERGENCY:
                return "Family Emergency";
            case JURY_DUTY:
                return "Jury Duty";
            case BEREAVEMENT:
                return "Bereavement";
            case OTHER:
            default:
                return "Other";
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Maps the string reason from ComboBox to AbsenceReason enum
     */
    private AbsenceReason mapReasonStringToEnum(String reason) {
        if (reason == null) {
            return AbsenceReason.OTHER;
        }

        switch (reason) {
            case "Sick Leave":
                return AbsenceReason.SICK_LEAVE;
            case "Personal Day":
                return AbsenceReason.PERSONAL_DAY;
            case "Professional Development":
                return AbsenceReason.PROFESSIONAL_DEV;
            case "Family Emergency":
                return AbsenceReason.FAMILY_EMERGENCY;
            case "Medical Appointment":
                return AbsenceReason.SICK_LEAVE; // Map medical appointment to sick leave
            case "Jury Duty":
                return AbsenceReason.JURY_DUTY;
            case "Bereavement":
                return AbsenceReason.BEREAVEMENT;
            default:
                return AbsenceReason.OTHER;
        }
    }

    /**
     * Send notifications based on user selections
     */
    private void sendNotifications(SubstituteAssignment assignment) {
        if (notificationService == null) {
            log.warn("NotificationService not available, skipping notifications");
            return;
        }

        String dateStr = assignment.getAssignmentDate() != null
            ? assignment.getAssignmentDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            : "TBD";
        String substituteStr = assignment.getSubstitute() != null
            ? assignment.getSubstitute().getFullName()
            : "Unknown";
        String absentStaffStr = assignment.getReplacedTeacher() != null
            ? assignment.getReplacedTeacher().getName()
            : (assignment.getReplacedStaffName() != null ? assignment.getReplacedStaffName() : "Unknown");

        try {
            // Notify substitute if checkbox is selected
            if (notifySubstituteCheckbox.isSelected()) {
                log.info("Sending notification to substitute: {}", substituteStr);
                notificationService.createGlobalNotification(
                    Notification.NotificationType.SCHEDULE_CHANGE,
                    "Substitute Assignment Confirmed",
                    String.format("You have been assigned to cover for %s on %s.", absentStaffStr, dateStr)
                );
            }

            // Notify admin if checkbox is selected
            if (notifyAdminCheckbox.isSelected()) {
                log.info("Sending notification to admin");
                notificationService.createNotificationForRole(
                    Notification.NotificationType.TEACHER_CHANGED,
                    "Substitute Assignment Created",
                    String.format("%s will cover for %s on %s.", substituteStr, absentStaffStr, dateStr),
                    "ADMIN"
                );
            }

            // Notify department if checkbox is selected
            if (notifyDepartmentCheckbox.isSelected() && assignment.getReplacedTeacher() != null) {
                String department = assignment.getReplacedTeacher().getDepartment();
                log.info("Sending notification to department: {}", department);
                notificationService.createGlobalNotification(
                    Notification.NotificationType.TEACHER_CHANGED,
                    "Department Substitute Notice",
                    String.format("%s (%s) will be covered by %s on %s.",
                        absentStaffStr, department != null ? department : "N/A", substituteStr, dateStr)
                );
            }

            log.info("Notifications sent successfully");
        } catch (Exception e) {
            log.error("Error sending notifications", e);
            // Don't fail the save operation if notifications fail
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
