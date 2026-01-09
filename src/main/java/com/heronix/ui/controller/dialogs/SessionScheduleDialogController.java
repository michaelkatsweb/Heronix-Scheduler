package com.heronix.ui.controller.dialogs;

import com.heronix.model.domain.*;
import com.heronix.model.enums.PullOutStatus;
import com.heronix.repository.RoomRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.PullOutSchedulingService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Session Schedule Dialog Controller
 *
 * Controller for scheduling pull-out sessions.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8D - November 21, 2025
 */
@Controller
@Slf4j
public class SessionScheduleDialogController {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PullOutSchedulingService schedulingService;

    // Info Labels
    @FXML private Label studentLabel;
    @FXML private Label serviceLabel;
    @FXML private Label minutesRequiredLabel;

    // Schedule Fields
    @FXML private ComboBox<String> dayOfWeekComboBox;
    @FXML private ComboBox<LocalTime> startTimeComboBox;
    @FXML private ComboBox<LocalTime> endTimeComboBox;
    @FXML private Label durationLabel;
    @FXML private ComboBox<Teacher> staffComboBox;
    @FXML private ComboBox<Room> roomComboBox;
    @FXML private TextField locationField;
    @FXML private CheckBox recurringCheckBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private CheckBox groupSessionCheckBox;
    @FXML private ListView<Student> otherStudentsListView;
    @FXML private TextArea notesTextArea;
    @FXML private Label conflictWarningLabel;

    private IEPService currentService;
    private PullOutSchedule currentSchedule;
    private boolean isEditMode = false;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    public void initialize() {
        log.info("Initializing Session Schedule Dialog Controller");
        setupDayOfWeekComboBox();
        setupTimeComboBoxes();
        setupStaffComboBox();
        setupRoomComboBox();
        setupDateDefaults();
        setupGroupSessionToggle();
        setupConflictCheck();
    }

    private void setupDayOfWeekComboBox() {
        dayOfWeekComboBox.getItems().addAll(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"
        );

        dayOfWeekComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String day) {
                if (day == null) return "";
                return day.substring(0, 1) + day.substring(1).toLowerCase();
            }

            @Override
            public String fromString(String string) {
                return string != null ? string.toUpperCase() : null;
            }
        });
    }

    private void setupTimeComboBoxes() {
        // Generate time slots from 7:00 AM to 4:00 PM in 15-minute increments
        List<LocalTime> times = new ArrayList<>();
        LocalTime time = LocalTime.of(7, 0);
        LocalTime endOfDay = LocalTime.of(16, 0);

        while (!time.isAfter(endOfDay)) {
            times.add(time);
            time = time.plusMinutes(15);
        }

        startTimeComboBox.getItems().addAll(times);
        endTimeComboBox.getItems().addAll(times);

        StringConverter<LocalTime> timeConverter = new StringConverter<>() {
            @Override
            public String toString(LocalTime t) {
                return t == null ? "" : t.format(TIME_FORMAT);
            }

            @Override
            public LocalTime fromString(String string) {
                return null;
            }
        };

        startTimeComboBox.setConverter(timeConverter);
        endTimeComboBox.setConverter(timeConverter);

        // Update duration when times change
        startTimeComboBox.setOnAction(e -> updateDuration());
        endTimeComboBox.setOnAction(e -> updateDuration());
    }

    private void updateDuration() {
        LocalTime start = startTimeComboBox.getValue();
        LocalTime end = endTimeComboBox.getValue();

        if (start != null && end != null) {
            long minutes = ChronoUnit.MINUTES.between(start, end);
            if (minutes > 0) {
                durationLabel.setText(minutes + " minutes");
            } else {
                durationLabel.setText("Invalid (end before start)");
            }
        } else {
            durationLabel.setText("0 minutes");
        }

        checkForConflicts();
    }

    private void setupStaffComboBox() {
        try {
            List<Teacher> teachers = teacherRepository.findAllActive();
            staffComboBox.getItems().addAll(teachers);

            staffComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(Teacher teacher) {
                    return teacher == null ? "" : teacher.getName();
                }

                @Override
                public Teacher fromString(String string) {
                    return null;
                }
            });

            staffComboBox.setOnAction(e -> checkForConflicts());
        } catch (Exception e) {
            log.warn("Could not load staff: {}", e.getMessage());
        }
    }

    private void setupRoomComboBox() {
        try {
            List<Room> rooms = roomRepository.findAll();
            roomComboBox.getItems().add(null); // Allow no room
            roomComboBox.getItems().addAll(rooms);

            roomComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(Room room) {
                    if (room == null) return "(No specific room)";
                    return room.getRoomNumber();
                }

                @Override
                public Room fromString(String string) {
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("Could not load rooms: {}", e.getMessage());
        }
    }

    private void setupDateDefaults() {
        startDatePicker.setValue(LocalDate.now());
        // Default end date to end of school year (June)
        LocalDate schoolYearEnd = LocalDate.now()
            .withMonth(6)
            .withDayOfMonth(30);
        if (schoolYearEnd.isBefore(LocalDate.now())) {
            schoolYearEnd = schoolYearEnd.plusYears(1);
        }
        endDatePicker.setValue(schoolYearEnd);
    }

    private void setupGroupSessionToggle() {
        groupSessionCheckBox.setOnAction(e -> {
            boolean isGroup = groupSessionCheckBox.isSelected();
            otherStudentsListView.setVisible(isGroup);
            otherStudentsListView.setManaged(isGroup);

            if (isGroup && otherStudentsListView.getItems().isEmpty()) {
                loadStudentsForGroupSelection();
            }
        });
    }

    private void loadStudentsForGroupSelection() {
        try {
            List<Student> students = studentRepository.findAll();
            otherStudentsListView.getItems().clear();
            otherStudentsListView.getItems().addAll(students);
            otherStudentsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        } catch (Exception e) {
            log.warn("Could not load students for group selection: {}", e.getMessage());
        }
    }

    private void setupConflictCheck() {
        // Check for conflicts when key fields change
        dayOfWeekComboBox.setOnAction(e -> checkForConflicts());
    }

    private void checkForConflicts() {
        if (currentService == null) return;

        String day = dayOfWeekComboBox.getValue();
        LocalTime start = startTimeComboBox.getValue();
        LocalTime end = endTimeComboBox.getValue();
        Teacher staff = staffComboBox.getValue();

        if (day == null || start == null || end == null) {
            conflictWarningLabel.setVisible(false);
            conflictWarningLabel.setManaged(false);
            return;
        }

        List<String> conflicts = new ArrayList<>();

        // Check student conflicts
        if (currentService.getStudent() != null) {
            try {
                List<PullOutSchedule> studentConflicts = schedulingService.checkStudentConflicts(
                        currentService.getStudent().getId(), day, start, end);

                // Exclude current schedule if editing
                if (isEditMode && currentSchedule != null) {
                    studentConflicts = studentConflicts.stream()
                            .filter(s -> !s.getId().equals(currentSchedule.getId()))
                            .toList();
                }

                if (!studentConflicts.isEmpty()) {
                    conflicts.add("Student has " + studentConflicts.size() + " conflicting session(s)");
                }
            } catch (Exception e) {
                log.warn("Could not check student conflicts: {}", e.getMessage());
            }
        }

        // Check staff conflicts
        if (staff != null) {
            try {
                List<PullOutSchedule> staffConflicts = schedulingService.checkStaffConflicts(
                        staff.getId(), day, start, end);

                // Exclude current schedule if editing
                if (isEditMode && currentSchedule != null) {
                    staffConflicts = staffConflicts.stream()
                            .filter(s -> !s.getId().equals(currentSchedule.getId()))
                            .toList();
                }

                if (!staffConflicts.isEmpty()) {
                    conflicts.add("Staff has " + staffConflicts.size() + " conflicting session(s)");
                }
            } catch (Exception e) {
                log.warn("Could not check staff conflicts: {}", e.getMessage());
            }
        }

        // Display conflicts
        if (!conflicts.isEmpty()) {
            conflictWarningLabel.setText("⚠ Conflicts: " + String.join("; ", conflicts));
            conflictWarningLabel.setVisible(true);
            conflictWarningLabel.setManaged(true);
            conflictWarningLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
        } else {
            conflictWarningLabel.setVisible(false);
            conflictWarningLabel.setManaged(false);
        }
    }

    /**
     * Set the IEP service for this session
     */
    public void setService(IEPService service) {
        this.currentService = service;

        // Populate info labels
        if (service.getStudent() != null) {
            studentLabel.setText(service.getStudent().getFullName());
        }
        serviceLabel.setText(service.getServiceType().getDisplayName() +
            " (" + service.getDeliveryModel().getDisplayName() + ")");
        minutesRequiredLabel.setText(service.getMinutesPerWeek() + " min/week, " +
            service.getSessionDurationMinutes() + " min/session");

        // Pre-populate session duration
        int sessionMinutes = service.getSessionDurationMinutes();
        LocalTime defaultStart = LocalTime.of(9, 0);
        startTimeComboBox.setValue(defaultStart);
        endTimeComboBox.setValue(defaultStart.plusMinutes(sessionMinutes));

        // Pre-assign staff if set on service
        if (service.getAssignedStaff() != null) {
            staffComboBox.setValue(service.getAssignedStaff());
        }

        // Pre-set location if known
        if (service.getLocation() != null) {
            locationField.setText(service.getLocation());
        }
    }

    /**
     * Set schedule for editing
     */
    public void setSchedule(PullOutSchedule schedule) {
        this.currentSchedule = schedule;
        this.isEditMode = true;

        // Set the service first
        if (schedule.getIepService() != null) {
            setService(schedule.getIepService());
        }

        // Populate form fields
        dayOfWeekComboBox.setValue(schedule.getDayOfWeek());
        startTimeComboBox.setValue(schedule.getStartTime());
        endTimeComboBox.setValue(schedule.getEndTime());
        staffComboBox.setValue(schedule.getStaff());
        roomComboBox.setValue(schedule.getRoom());
        locationField.setText(schedule.getLocationDescription());
        recurringCheckBox.setSelected(schedule.getRecurring());
        startDatePicker.setValue(schedule.getStartDate());
        endDatePicker.setValue(schedule.getEndDate());
        notesTextArea.setText(schedule.getNotes());

        if (schedule.getOtherStudents() != null && !schedule.getOtherStudents().isEmpty()) {
            groupSessionCheckBox.setSelected(true);
            otherStudentsListView.setVisible(true);
            otherStudentsListView.setManaged(true);

            // Parse comma-separated student IDs and select them in the list
            String[] studentIds = schedule.getOtherStudents().split(",");
            loadStudentsForGroupSelection();

            for (String studentIdStr : studentIds) {
                try {
                    Long studentId = Long.parseLong(studentIdStr.trim());
                    for (Student student : otherStudentsListView.getItems()) {
                        if (student.getId().equals(studentId)) {
                            otherStudentsListView.getSelectionModel().select(student);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid student ID in otherStudents: {}", studentIdStr);
                }
            }
        }

        updateDuration();
    }

    /**
     * Get the schedule from form data
     */
    public PullOutSchedule getSchedule() {
        PullOutSchedule schedule = currentSchedule != null ? currentSchedule : new PullOutSchedule();

        schedule.setIepService(currentService);
        schedule.setStudent(currentService != null ? currentService.getStudent() : null);
        schedule.setDayOfWeek(dayOfWeekComboBox.getValue());
        schedule.setStartTime(startTimeComboBox.getValue());
        schedule.setEndTime(endTimeComboBox.getValue());
        schedule.setStaff(staffComboBox.getValue());
        schedule.setRoom(roomComboBox.getValue());

        // Use location field if no room selected
        if (roomComboBox.getValue() == null && !locationField.getText().isEmpty()) {
            schedule.setLocationDescription(locationField.getText());
        }

        schedule.setRecurring(recurringCheckBox.isSelected());
        schedule.setStartDate(startDatePicker.getValue());
        schedule.setEndDate(endDatePicker.getValue());
        schedule.setNotes(notesTextArea.getText());

        // Calculate duration
        if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(schedule.getStartTime(), schedule.getEndTime());
            schedule.setDurationMinutes((int) minutes);
        }

        // Set default status
        if (schedule.getStatus() == null) {
            schedule.setStatus(PullOutStatus.ACTIVE);
        }

        // Handle group session
        if (groupSessionCheckBox.isSelected()) {
            List<Student> selected = otherStudentsListView.getSelectionModel().getSelectedItems();
            if (!selected.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Student s : selected) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(s.getId());
                }
                schedule.setOtherStudents(sb.toString());
            }
        }

        return schedule;
    }

    /**
     * Validate the form
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (dayOfWeekComboBox.getValue() == null) {
            errors.add("Day of week is required");
        }

        if (startTimeComboBox.getValue() == null) {
            errors.add("Start time is required");
        }

        if (endTimeComboBox.getValue() == null) {
            errors.add("End time is required");
        }

        if (startTimeComboBox.getValue() != null && endTimeComboBox.getValue() != null) {
            if (!endTimeComboBox.getValue().isAfter(startTimeComboBox.getValue())) {
                errors.add("End time must be after start time");
            }
        }

        if (staffComboBox.getValue() == null) {
            errors.add("Staff assignment is required");
        }

        if (startDatePicker.getValue() == null) {
            errors.add("Start date is required");
        }

        // Location check - need either room or location description
        if (roomComboBox.getValue() == null &&
            (locationField.getText() == null || locationField.getText().isEmpty())) {
            errors.add("Location is required (select room or enter description)");
        }

        return errors;
    }

    /**
     * Check if this is edit mode
     */
    public boolean isEditMode() {
        return isEditMode;
    }
}
