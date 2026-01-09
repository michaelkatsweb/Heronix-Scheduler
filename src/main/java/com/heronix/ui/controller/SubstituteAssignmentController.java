package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import com.heronix.service.SubstituteManagementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Substitute Assignment Form
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Controller
public class SubstituteAssignmentController {

    private static final Logger logger = LoggerFactory.getLogger(SubstituteAssignmentController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    @Autowired
    private SubstituteManagementService substituteManagementService;

    @Autowired
    private SubstituteRepository substituteRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Autowired
    private CourseRepository courseRepository;

    // Title
    @FXML private Label formTitleLabel;

    // Section 1: Substitute
    @FXML private ComboBox<Substitute> substituteComboBox;
    @FXML private ComboBox<SubstituteType> substituteTypeComboBox;
    @FXML private TextField substituteContactField;

    // Section 2: Date and Time
    @FXML private DatePicker assignmentDatePicker;
    @FXML private ComboBox<AssignmentDuration> durationTypeComboBox;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private VBox multiDaySection;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField totalDaysField;

    // Section 3: Replaced Staff
    @FXML private ComboBox<StaffType> staffTypeComboBox;
    @FXML private ComboBox<Teacher> replacedTeacherComboBox;
    @FXML private ComboBox<AbsenceReason> absenceReasonComboBox;
    @FXML private ComboBox<AssignmentStatus> statusComboBox;
    @FXML private TextField replacedStaffNameField;

    // Section 4: Location and Course
    @FXML private ComboBox<Room> roomComboBox;
    @FXML private ComboBox<Course> courseComboBox;
    @FXML private CheckBox isFloaterCheckbox;

    // Section 5: Additional Details
    @FXML private TextField totalHoursField;
    @FXML private TextField payAmountField;
    @FXML private TextField frontlineJobIdField;
    @FXML private TextArea notesArea;

    // Validation
    @FXML private VBox validationSection;
    @FXML private Label validationMessageLabel;

    // Buttons
    @FXML private Button saveButton;

    private SubstituteAssignment editingAssignment;
    private Runnable onSaveCallback;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Substitute Assignment Controller");

        setupComboBoxes();
        setupListeners();
        loadData();
    }

    /**
     * Setup combo box converters
     */
    private void setupComboBoxes() {
        // Substitute combo box
        substituteComboBox.setConverter(new StringConverter<Substitute>() {
            @Override
            public String toString(Substitute sub) {
                return sub != null ? sub.getFullName() + " (" + sub.getType().getDisplayName() + ")" : "";
            }

            @Override
            public Substitute fromString(String string) {
                return null;
            }
        });

        // Teacher combo box
        replacedTeacherComboBox.setConverter(new StringConverter<Teacher>() {
            @Override
            public String toString(Teacher teacher) {
                return teacher != null ? teacher.getName() : "";
            }

            @Override
            public Teacher fromString(String string) {
                return null;
            }
        });

        // Room combo box
        roomComboBox.setConverter(new StringConverter<Room>() {
            @Override
            public String toString(Room room) {
                return room != null ? room.getRoomNumber() + (room.getBuilding() != null ? " (" + room.getBuilding() + ")" : "") : "";
            }

            @Override
            public Room fromString(String string) {
                return null;
            }
        });

        // Course combo box
        courseComboBox.setConverter(new StringConverter<Course>() {
            @Override
            public String toString(Course course) {
                return course != null ? course.getCourseCode() + " - " + course.getCourseName() : "";
            }

            @Override
            public Course fromString(String string) {
                return null;
            }
        });

        // Enum combo boxes
        substituteTypeComboBox.setConverter(createEnumConverter());
        durationTypeComboBox.setConverter(createEnumConverter());
        staffTypeComboBox.setConverter(createEnumConverter());
        absenceReasonComboBox.setConverter(createEnumConverter());
        statusComboBox.setConverter(createEnumConverter());
    }

    /**
     * Create enum string converter
     */
    private <T extends Enum<?>> StringConverter<T> createEnumConverter() {
        return new StringConverter<T>() {
            @Override
            public String toString(T value) {
                if (value == null) return "";
                try {
                    return (String) value.getClass().getMethod("getDisplayName").invoke(value);
                } catch (Exception e) {
                    return value.toString();
                }
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    /**
     * Setup event listeners
     */
    private void setupListeners() {
        // Substitute selection changed
        substituteComboBox.setOnAction(e -> {
            Substitute selected = substituteComboBox.getValue();
            if (selected != null) {
                substituteTypeComboBox.setValue(selected.getType());
                String contact = "";
                if (selected.getEmail() != null) contact += selected.getEmail();
                if (selected.getPhoneNumber() != null) {
                    if (!contact.isEmpty()) contact += " | ";
                    contact += selected.getPhoneNumber();
                }
                substituteContactField.setText(contact);
            }
        });

        // Duration type changed
        durationTypeComboBox.setOnAction(e -> {
            AssignmentDuration duration = durationTypeComboBox.getValue();
            boolean isMultiDay = duration == AssignmentDuration.MULTI_DAY || duration == AssignmentDuration.LONG_TERM;
            multiDaySection.setVisible(isMultiDay);
            multiDaySection.setManaged(isMultiDay);
        });

        // Date changed - calculate total days
        assignmentDatePicker.setOnAction(e -> calculateTotalDays());
        endDatePicker.setOnAction(e -> calculateTotalDays());

        // Time changed - calculate hours
        startTimeField.textProperty().addListener((obs, old, newVal) -> calculateTotalHours());
        endTimeField.textProperty().addListener((obs, old, newVal) -> calculateTotalHours());
    }

    /**
     * Load data into combo boxes
     */
    private void loadData() {
        // Load substitutes
        List<Substitute> substitutes = substituteRepository.findByActiveTrue();
        substituteComboBox.setItems(FXCollections.observableArrayList(substitutes));

        // Load teachers
        List<Teacher> teachers = teacherRepository.findByActiveTrue();
        replacedTeacherComboBox.setItems(FXCollections.observableArrayList(teachers));

        // Load rooms
        List<Room> rooms = roomRepository.findAll();
        roomComboBox.setItems(FXCollections.observableArrayList(rooms));

        // Load courses
        List<Course> courses = courseRepository.findAll();
        courseComboBox.setItems(FXCollections.observableArrayList(courses));

        // Load enums
        substituteTypeComboBox.setItems(FXCollections.observableArrayList(SubstituteType.values()));
        durationTypeComboBox.setItems(FXCollections.observableArrayList(AssignmentDuration.values()));
        staffTypeComboBox.setItems(FXCollections.observableArrayList(StaffType.values()));
        absenceReasonComboBox.setItems(FXCollections.observableArrayList(AbsenceReason.values()));
        statusComboBox.setItems(FXCollections.observableArrayList(AssignmentStatus.values()));

        // Set defaults
        durationTypeComboBox.setValue(AssignmentDuration.FULL_DAY);
        staffTypeComboBox.setValue(StaffType.TEACHER);
        absenceReasonComboBox.setValue(AbsenceReason.SICK_LEAVE);
        statusComboBox.setValue(AssignmentStatus.CONFIRMED);
        assignmentDatePicker.setValue(LocalDate.now());

        logger.info("Loaded {} substitutes, {} teachers, {} rooms, {} courses",
                substitutes.size(), teachers.size(), rooms.size(), courses.size());
    }

    /**
     * Set assignment to edit
     */
    public void setAssignment(SubstituteAssignment assignment) {
        this.editingAssignment = assignment;
        formTitleLabel.setText("Edit Substitute Assignment");

        // Populate form fields
        substituteComboBox.setValue(assignment.getSubstitute());
        assignmentDatePicker.setValue(assignment.getAssignmentDate());
        durationTypeComboBox.setValue(assignment.getDurationType());

        if (assignment.getStartTime() != null) {
            startTimeField.setText(assignment.getStartTime().format(TIME_FORMATTER));
        }
        if (assignment.getEndTime() != null) {
            endTimeField.setText(assignment.getEndTime().format(TIME_FORMATTER));
        }

        if (assignment.getEndDate() != null) {
            endDatePicker.setValue(assignment.getEndDate());
        }

        staffTypeComboBox.setValue(assignment.getReplacedStaffType());
        replacedTeacherComboBox.setValue(assignment.getReplacedTeacher());
        absenceReasonComboBox.setValue(assignment.getAbsenceReason());
        statusComboBox.setValue(assignment.getStatus());
        replacedStaffNameField.setText(assignment.getReplacedStaffName());

        roomComboBox.setValue(assignment.getRoom());
        courseComboBox.setValue(assignment.getCourse());
        isFloaterCheckbox.setSelected(assignment.getIsFloater());

        if (assignment.getTotalHours() != null) {
            totalHoursField.setText(String.format("%.2f", assignment.getTotalHours()));
        }
        if (assignment.getPayAmount() != null) {
            payAmountField.setText(String.format("%.2f", assignment.getPayAmount()));
        }
        frontlineJobIdField.setText(assignment.getFrontlineJobId());
        notesArea.setText(assignment.getNotes());
    }

    /**
     * Set callback for save action
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    /**
     * Calculate total days for multi-day assignments
     */
    private void calculateTotalDays() {
        LocalDate start = assignmentDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start != null && end != null && !end.isBefore(start)) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            totalDaysField.setText(String.valueOf(days));
        } else {
            totalDaysField.setText("");
        }
    }

    /**
     * Calculate total hours from start/end time
     */
    private void calculateTotalHours() {
        try {
            LocalTime start = parseTime(startTimeField.getText());
            LocalTime end = parseTime(endTimeField.getText());

            if (start != null && end != null) {
                Duration duration = Duration.between(start, end);
                double hours = duration.toMinutes() / 60.0;
                totalHoursField.setText(String.format("%.2f", hours));
            }
        } catch (Exception e) {
            // Ignore parsing errors during typing
        }
    }

    /**
     * Parse time string
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalTime.parse(timeStr.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Try without formatter
            try {
                return LocalTime.parse(timeStr.trim());
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /**
     * Handle new substitute button
     * Opens the substitute assignment dialog for creating a new assignment
     */
    @FXML
    private void handleNewSubstitute() {
        try {
            // Load dialog FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/SubstituteAssignmentDialog.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);

            javafx.scene.Parent root = loader.load();
            SubstituteAssignmentDialogController controller = loader.getController();

            // Create stage
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("New Substitute Assignment");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            // Get current window as owner (from any FXML component)
            javafx.stage.Window owner = substituteComboBox != null && substituteComboBox.getScene() != null
                ? substituteComboBox.getScene().getWindow()
                : null;
            if (owner != null) {
                stage.initOwner(owner);
            }

            controller.setDialogStage(stage);

            // Show and wait
            stage.showAndWait();

            // If saved successfully, show confirmation
            if (controller.isSaved()) {
                logger.info("Substitute assignment created successfully via dialog");
                showInfo("Success", "Substitute assignment has been created successfully!");
            }

        } catch (Exception e) {
            logger.error("Error opening substitute assignment dialog", e);
            showError("Error", "Failed to open substitute assignment dialog: " + e.getMessage());
        }
    }

    /**
     * Handle save button
     */
    @FXML
    private void handleSave() {
        if (!validateForm()) {
            return;
        }

        try {
            SubstituteAssignment assignment = editingAssignment != null ? editingAssignment : new SubstituteAssignment();

            // Set substitute
            assignment.setSubstitute(substituteComboBox.getValue());

            // Set date/time
            assignment.setAssignmentDate(assignmentDatePicker.getValue());
            assignment.setStartTime(parseTime(startTimeField.getText()));
            assignment.setEndTime(parseTime(endTimeField.getText()));
            assignment.setDurationType(durationTypeComboBox.getValue());

            if (endDatePicker.getValue() != null) {
                assignment.setEndDate(endDatePicker.getValue());
            }

            // Set replaced staff
            assignment.setReplacedStaffType(staffTypeComboBox.getValue());
            assignment.setReplacedTeacher(replacedTeacherComboBox.getValue());
            assignment.setReplacedStaffName(replacedStaffNameField.getText());
            assignment.setAbsenceReason(absenceReasonComboBox.getValue());
            assignment.setStatus(statusComboBox.getValue());

            // Set location/course
            assignment.setRoom(roomComboBox.getValue());
            assignment.setCourse(courseComboBox.getValue());
            assignment.setIsFloater(isFloaterCheckbox.isSelected());

            // Set additional details
            assignment.setAssignmentSource(AssignmentSource.MANUAL);

            if (!totalHoursField.getText().isEmpty()) {
                try {
                    assignment.setTotalHours(Double.parseDouble(totalHoursField.getText()));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            if (!payAmountField.getText().isEmpty()) {
                try {
                    assignment.setPayAmount(Double.parseDouble(payAmountField.getText()));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            assignment.setFrontlineJobId(frontlineJobIdField.getText());
            assignment.setNotes(notesArea.getText());

            // Save
            substituteManagementService.saveAssignment(assignment);

            logger.info("Saved assignment: {}", assignment);

            showSuccess("Assignment Saved", "Substitute assignment has been saved successfully.");

            // Callback
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            // Close
            handleCancel();

        } catch (Exception e) {
            logger.error("Error saving assignment", e);
            showError("Save Error", "Error saving assignment: " + e.getMessage());
        }
    }

    /**
     * Validate form
     */
    private boolean validateForm() {
        List<String> errors = new ArrayList<>();

        if (substituteComboBox.getValue() == null) {
            errors.add("Substitute is required");
        }

        if (assignmentDatePicker.getValue() == null) {
            errors.add("Assignment date is required");
        }

        if (durationTypeComboBox.getValue() == null) {
            errors.add("Duration type is required");
        }

        if (startTimeField.getText().isEmpty()) {
            errors.add("Start time is required");
        } else if (parseTime(startTimeField.getText()) == null) {
            errors.add("Start time format is invalid (use h:mm AM/PM)");
        }

        if (endTimeField.getText().isEmpty()) {
            errors.add("End time is required");
        } else if (parseTime(endTimeField.getText()) == null) {
            errors.add("End time format is invalid (use h:mm AM/PM)");
        }

        if (staffTypeComboBox.getValue() == null) {
            errors.add("Staff type is required");
        }

        if (absenceReasonComboBox.getValue() == null) {
            errors.add("Absence reason is required");
        }

        if (statusComboBox.getValue() == null) {
            errors.add("Assignment status is required");
        }

        // Multi-day validation
        AssignmentDuration duration = durationTypeComboBox.getValue();
        if ((duration == AssignmentDuration.MULTI_DAY || duration == AssignmentDuration.LONG_TERM) &&
            endDatePicker.getValue() == null) {
            errors.add("End date is required for multi-day assignments");
        }

        if (!errors.isEmpty()) {
            validationMessageLabel.setText(String.join("\n", errors));
            validationSection.setVisible(true);
            validationSection.setManaged(true);
            return false;
        }

        validationSection.setVisible(false);
        validationSection.setManaged(false);
        return true;
    }

    /**
     * Handle cancel button
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Show success message
     */
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
