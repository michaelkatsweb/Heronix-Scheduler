package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Student Schedule View Controller
 * Displays individual student class schedules with all details
 * Inspired by Skyward and other Student Information Systems
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/StudentScheduleViewController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
@Component
public class StudentScheduleViewController {

    @FXML private ComboBox<Student> studentComboBox;
    @FXML private VBox studentInfoBox;
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label gradeLabel;
    @FXML private Label specialNeedsLabel;
    @FXML private Label giftedLabel;

    @FXML private TableView<ScheduleSlotDisplay> scheduleTable;
    @FXML private TableColumn<ScheduleSlotDisplay, String> periodColumn;
    @FXML private TableColumn<ScheduleSlotDisplay, String> timeColumn;
    @FXML private TableColumn<ScheduleSlotDisplay, String> courseColumn;
    @FXML private TableColumn<ScheduleSlotDisplay, String> courseCodeColumn;
    @FXML private TableColumn<ScheduleSlotDisplay, String> teacherColumn;
    @FXML private TableColumn<ScheduleSlotDisplay, String> roomColumn;
    @FXML private TableColumn<ScheduleSlotDisplay, String> extensionColumn;
    @FXML private TableColumn<ScheduleSlotDisplay, String> notesColumn;

    @FXML private TableView<EventDisplay> eventsTable;
    @FXML private TableColumn<EventDisplay, String> eventDayColumn;
    @FXML private TableColumn<EventDisplay, String> eventTimeColumn;
    @FXML private TableColumn<EventDisplay, String> eventTypeColumn;
    @FXML private TableColumn<EventDisplay, String> eventNameColumn;
    @FXML private TableColumn<EventDisplay, String> eventLocationColumn;
    @FXML private TableColumn<EventDisplay, String> eventNotesColumn;

    @FXML private Label totalClassesLabel;
    @FXML private Label totalCreditsLabel;
    @FXML private Label accommodationsLabel;
    @FXML private Label medicalAlertsLabel;
    @FXML private Label statusLabel;
    @FXML private Label timestampLabel;

    @Autowired private StudentRepository studentRepository;
    @Autowired private ScheduleSlotRepository scheduleSlotRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private com.heronix.service.export.PdfExportService pdfExportService;

    private Student selectedStudent;
    private Stage dialogStage;
    private List<ScheduleSlot> currentScheduleSlots;

    @FXML
    public void initialize() {
        log.info("Initializing Student Schedule View Controller");

        setupStudentComboBox();
        setupScheduleTable();
        setupEventsTable();
        loadStudents();
        updateTimestamp();
    }

    /**
     * Setup student selection combo box
     */
    private void setupStudentComboBox() {
        studentComboBox.setConverter(new StringConverter<Student>() {
            @Override
            public String toString(Student student) {
                if (student == null) return "";
                return String.format("%s %s (ID: %s) - Grade %s",
                    student.getFirstName(),
                    student.getLastName(),
                    student.getStudentId(),
                    student.getGradeLevel());
            }

            @Override
            public Student fromString(String string) {
                return null;
            }
        });

        // Make it searchable
        studentComboBox.setEditable(true);
    }

    /**
     * Setup schedule table columns
     */
    private void setupScheduleTable() {
        periodColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPeriod()));
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime()));
        courseColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseName()));
        courseCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseCode()));
        teacherColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTeacher()));
        roomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoom()));
        extensionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExtension()));
        notesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes()));

        // Apply color coding to rows based on subject (and highlight empty slots)
        scheduleTable.setRowFactory(tv -> new TableRow<ScheduleSlotDisplay>() {
            @Override
            protected void updateItem(ScheduleSlotDisplay item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                } else if (item.isEmpty()) {
                    // Style empty slots with gray background and italic text
                    setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #9e9e9e; -fx-font-style: italic;");
                } else if (item.getSubject() == null) {
                    setStyle("");
                } else {
                    String style = com.heronix.ui.util.ScheduleColorScheme.getLightBackgroundStyle(item.getSubject());
                    setStyle(style);
                }
            }
        });
    }

    /**
     * Setup events table columns
     */
    private void setupEventsTable() {
        eventDayColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDay()));
        eventTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime()));
        eventTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        eventNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        eventLocationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));
        eventNotesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes()));
    }

    /**
     * Load all active students
     */
    private void loadStudents() {
        try {
            List<Student> students = studentRepository.findAll().stream()
                .filter(Student::isActive)
                .sorted(Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName))
                .collect(Collectors.toList());

            studentComboBox.setItems(FXCollections.observableArrayList(students));
            log.info("Loaded {} students", students.size());
            statusLabel.setText(String.format("Loaded %d students", students.size()));
        } catch (Exception e) {
            log.error("Error loading students", e);
            showError("Error", "Failed to load students: " + e.getMessage());
        }
    }

    /**
     * Handle student selection
     */
    @FXML
    private void handleStudentSelection() {
        selectedStudent = studentComboBox.getValue();
        if (selectedStudent != null) {
            displayStudentInfo();
            loadStudentSchedule();
            loadStudentEvents();
        }
    }

    /**
     * Display student information
     */
    private void displayStudentInfo() {
        if (selectedStudent == null) return;

        studentNameLabel.setText(selectedStudent.getFirstName() + " " + selectedStudent.getLastName());
        studentIdLabel.setText(selectedStudent.getStudentId());
        gradeLabel.setText(selectedStudent.getGradeLevel());

        // IEP/504 status
        List<String> specialNeeds = new ArrayList<>();
        if (Boolean.TRUE.equals(selectedStudent.getHasIEP())) {
            specialNeeds.add("IEP");
        }
        if (Boolean.TRUE.equals(selectedStudent.getHas504Plan())) {
            specialNeeds.add("504 Plan");
        }
        specialNeedsLabel.setText(specialNeeds.isEmpty() ? "None" : String.join(", ", specialNeeds));

        // Gifted status
        giftedLabel.setText(Boolean.TRUE.equals(selectedStudent.getIsGifted()) ? "Yes" : "No");
        if (Boolean.TRUE.equals(selectedStudent.getIsGifted())) {
            giftedLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else {
            giftedLabel.setStyle("");
        }

        // Accommodations
        String accommodations = selectedStudent.getAccommodationNotes();
        accommodationsLabel.setText(
            (accommodations != null && !accommodations.trim().isEmpty())
                ? accommodations
                : "No special accommodations"
        );

        // Medical alerts
        String medical = selectedStudent.getMedicalConditions();
        medicalAlertsLabel.setText(
            (medical != null && !medical.trim().isEmpty())
                ? medical
                : "None"
        );

        studentInfoBox.setVisible(true);
    }

    /**
     * Load student schedule from schedule slots
     * Shows both enrolled courses AND empty periods for incomplete schedules
     */
    private void loadStudentSchedule() {
        if (selectedStudent == null) return;

        try {
            // Get all schedule slots for this student
            List<ScheduleSlot> enrolledSlots = scheduleSlotRepository.findAll().stream()
                .filter(slot -> slot.getStudents() != null &&
                    slot.getStudents().stream()
                        .anyMatch(s -> s.getId().equals(selectedStudent.getId())))
                .sorted(Comparator.comparing(ScheduleSlot::getDayOfWeek)
                    .thenComparing(ScheduleSlot::getStartTime))
                .collect(Collectors.toList());

            // Get all possible time slots in the schedule (to show empty periods)
            List<ScheduleSlot> allPossibleSlots = scheduleSlotRepository.findAll().stream()
                .filter(slot -> slot.getCourse() != null) // Only slots with courses
                .sorted(Comparator.comparing(ScheduleSlot::getDayOfWeek)
                    .thenComparing(ScheduleSlot::getStartTime))
                .collect(Collectors.toList());

            // Group by period/time to identify unique periods
            java.util.Map<String, ScheduleSlot> periodMap = new java.util.LinkedHashMap<>();
            for (ScheduleSlot slot : allPossibleSlots) {
                String periodKey = getPeriodKey(slot);
                if (!periodMap.containsKey(periodKey)) {
                    periodMap.put(periodKey, slot);
                }
            }

            // Create display list with enrolled courses and empty slots
            List<ScheduleSlotDisplay> displayList = new ArrayList<>();
            java.util.Set<String> enrolledPeriodKeys = enrolledSlots.stream()
                .map(this::getPeriodKey)
                .collect(java.util.stream.Collectors.toSet());

            // Add enrolled courses
            for (ScheduleSlot slot : enrolledSlots) {
                displayList.add(new ScheduleSlotDisplay(slot, false)); // not empty
            }

            // Add empty slots for unenrolled periods
            for (java.util.Map.Entry<String, ScheduleSlot> entry : periodMap.entrySet()) {
                if (!enrolledPeriodKeys.contains(entry.getKey())) {
                    // Create empty slot display
                    displayList.add(new ScheduleSlotDisplay(entry.getValue(), true)); // is empty
                }
            }

            // Sort the combined list
            displayList.sort(Comparator.comparing(ScheduleSlotDisplay::getSortKey));

            scheduleTable.setItems(FXCollections.observableArrayList(displayList));

            // Store current schedule slots for PDF export (only enrolled)
            currentScheduleSlots = enrolledSlots;

            // Update summary - show enrolled vs total periods
            int enrolledCount = enrolledSlots.size();
            int totalPeriods = periodMap.size();
            totalClassesLabel.setText(String.format("%d / %d", enrolledCount, totalPeriods));

            // Calculate credits (only for enrolled courses)
            double totalCredits = enrolledCount * 0.5; // Example: 0.5 credits per class
            totalCreditsLabel.setText(String.format("%.1f", totalCredits));

            // Show schedule completeness status
            if (enrolledCount < totalPeriods) {
                statusLabel.setText(String.format("⚠ Partial schedule for %s %s (%d of %d periods filled)",
                    selectedStudent.getFirstName(),
                    selectedStudent.getLastName(),
                    enrolledCount,
                    totalPeriods));
                statusLabel.setStyle("-fx-text-fill: #FF9800;"); // Orange warning color
            } else {
                statusLabel.setText(String.format("✓ Complete schedule for %s %s (%d classes)",
                    selectedStudent.getFirstName(),
                    selectedStudent.getLastName(),
                    enrolledCount));
                statusLabel.setStyle("-fx-text-fill: #4CAF50;"); // Green success color
            }

            log.info("Loaded schedule for student: {} ({} enrolled / {} total periods)",
                selectedStudent.getStudentId(), enrolledCount, totalPeriods);

        } catch (Exception e) {
            log.error("Error loading student schedule", e);
            showError("Error", "Failed to load schedule: " + e.getMessage());
        }
    }

    /**
     * Generate unique key for a period (for grouping/comparing)
     */
    private String getPeriodKey(ScheduleSlot slot) {
        return String.format("%s_%s_%s",
            slot.getDayOfWeek() != null ? slot.getDayOfWeek() : "NONE",
            slot.getPeriodNumber() != null ? slot.getPeriodNumber() : "0",
            slot.getStartTime() != null ? slot.getStartTime().toString() : "00:00"
        );
    }

    /**
     * Load student events and IEP meetings for the current week
     */
    private void loadStudentEvents() {
        if (selectedStudent == null) return;

        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
            java.time.LocalDate endOfWeek = today.with(java.time.DayOfWeek.FRIDAY);

            // Get all events for this week
            List<Event> allEvents = eventRepository.findAll().stream()
                .filter(event -> event.getStartDateTime() != null)
                .filter(event -> {
                    java.time.LocalDate eventDate = event.getStartDateTime().toLocalDate();
                    return !eventDate.isBefore(startOfWeek) && !eventDate.isAfter(endOfWeek);
                })
                .sorted(Comparator.comparing(Event::getStartDateTime))
                .collect(Collectors.toList());

            // Filter for IEP meetings or events relevant to this student
            List<Event> studentEvents = allEvents.stream()
                .filter(event -> {
                    // Include IEP meetings if student has IEP
                    if (Boolean.TRUE.equals(selectedStudent.getHasIEP()) &&
                        event.getEventType() != null &&
                        event.getEventType().toString().contains("IEP")) {
                        return true;
                    }
                    // Include general school events
                    if (event.getEventType() != null &&
                        (event.getEventType().toString().contains("ASSEMBLY") ||
                         event.getEventType().toString().contains("SCHOOL") ||
                         event.getIsBlocksScheduling() != null && event.getIsBlocksScheduling())) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

            // Convert to display objects
            List<EventDisplay> displayList = studentEvents.stream()
                .map(EventDisplay::new)
                .collect(Collectors.toList());

            eventsTable.setItems(FXCollections.observableArrayList(displayList));

            log.info("Loaded {} events for student this week", displayList.size());

        } catch (Exception e) {
            log.error("Error loading student events", e);
            // Don't show error to user - events are optional
        }
    }

    /**
     * Handle print schedule
     */
    @FXML
    private void handlePrint() {
        if (selectedStudent == null) {
            showWarning("No Student Selected", "Please select a student first.");
            return;
        }

        if (currentScheduleSlots == null || currentScheduleSlots.isEmpty()) {
            showWarning("No Schedule Data", "No schedule data available to print.");
            return;
        }

        try {
            log.info("Printing schedule for student: {}", selectedStudent.getStudentId());

            // Generate PDF
            java.io.ByteArrayOutputStream pdfStream = pdfExportService
                .generateStudentSchedulePdf(selectedStudent, currentScheduleSlots);

            // Print the PDF
            pdfExportService.printPdf(pdfStream);

            statusLabel.setText("Schedule sent to printer");
            showInfo("Success", "Schedule sent to printer successfully.");

        } catch (Exception e) {
            log.error("Failed to print schedule", e);
            showError("Print Failed", "Could not print schedule: " + e.getMessage());
        }
    }

    /**
     * Handle export to PDF
     */
    @FXML
    private void handleExportPDF() {
        if (selectedStudent == null) {
            showWarning("No Student Selected", "Please select a student first.");
            return;
        }

        if (currentScheduleSlots == null || currentScheduleSlots.isEmpty()) {
            showWarning("No Schedule Data", "No schedule data available to export.");
            return;
        }

        try {
            log.info("Exporting PDF for student: {}", selectedStudent.getStudentId());

            // Generate PDF
            java.io.ByteArrayOutputStream pdfStream = pdfExportService
                .generateStudentSchedulePdf(selectedStudent, currentScheduleSlots);

            // Show save dialog
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save Student Schedule PDF");
            fileChooser.setInitialFileName(
                selectedStudent.getLastName() + "_" +
                selectedStudent.getFirstName() + "_Schedule.pdf"
            );
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );

            java.io.File file = fileChooser.showSaveDialog(dialogStage);
            if (file != null) {
                pdfExportService.exportToFile(pdfStream, file);
                statusLabel.setText("PDF exported to: " + file.getName());
                showInfo("Success", "Schedule exported successfully to:\n" + file.getAbsolutePath());
                log.info("PDF exported to: {}", file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("Failed to export PDF", e);
            showError("Export Failed", "Could not export PDF: " + e.getMessage());
        }
    }

    /**
     * Handle close
     */
    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Update timestamp
     */
    private void updateTimestamp() {
        timestampLabel.setText("Generated: " +
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * Set dialog stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
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

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========================================================================
    // INNER CLASS: Schedule Slot Display
    // ========================================================================

    /**
     * Display wrapper for schedule slots
     * Supports both enrolled courses and empty periods
     */
    public static class ScheduleSlotDisplay {
        private final ScheduleSlot slot;
        private final boolean isEmpty;

        public ScheduleSlotDisplay(ScheduleSlot slot, boolean isEmpty) {
            this.slot = slot;
            this.isEmpty = isEmpty;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public String getPeriod() {
            if (slot.getPeriodNumber() != null) {
                String periodText = "Period " + slot.getPeriodNumber();
                return isEmpty ? periodText + " (Empty)" : periodText;
            }
            if (slot.getDayOfWeek() != null) {
                return slot.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            }
            return "--";
        }

        public String getTime() {
            LocalTime start = slot.getStartTime();
            LocalTime end = slot.getEndTime();
            if (start != null && end != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
                return start.format(formatter) + " - " + end.format(formatter);
            }
            return "--";
        }

        public String getCourseName() {
            if (isEmpty) {
                return "-- No Course Assigned --";
            }
            Course course = slot.getCourse();
            if (course != null) {
                String name = course.getCourseName();
                // Add course type if available
                if (course.getCourseType() != null && course.getCourseType() != com.heronix.model.enums.CourseType.REGULAR) {
                    name += " (" + course.getCourseType().getDisplayName() + ")";
                }
                return name;
            }
            return "--";
        }

        public String getCourseCode() {
            if (isEmpty) {
                return "";
            }
            Course course = slot.getCourse();
            return (course != null && course.getCourseCode() != null)
                ? course.getCourseCode()
                : "--";
        }

        public String getTeacher() {
            if (isEmpty) {
                return "";
            }
            Teacher teacher = slot.getTeacher();
            return (teacher != null) ? teacher.getName() : "--";
        }

        public String getRoom() {
            if (isEmpty) {
                return "";
            }
            Room room = slot.getRoom();
            return (room != null && room.getRoomNumber() != null)
                ? room.getRoomNumber()
                : "--";
        }

        public String getExtension() {
            if (isEmpty) {
                return "";
            }
            Room room = slot.getRoom();
            return (room != null && room.getTelephoneExtension() != null)
                ? room.getTelephoneExtension()
                : "--";
        }

        public String getNotes() {
            if (isEmpty) {
                return "⚠ This period needs course assignment";
            }
            String notes = slot.getNotes();
            if (Boolean.TRUE.equals(slot.getIsSpecialEvent())) {
                String eventNote = "Special Event";
                if (slot.getSpecialEventType() != null) {
                    eventNote = slot.getSpecialEventType().getDisplayName();
                }
                notes = (notes != null) ? eventNote + ": " + notes : eventNote;
            }
            return (notes != null && !notes.trim().isEmpty()) ? notes : "";
        }

        public String getSubject() {
            if (isEmpty) {
                return "EMPTY"; // Special subject for styling empty slots
            }
            Course course = slot.getCourse();
            return (course != null) ? course.getSubject() : null;
        }

        /**
         * Get sort key for ordering slots chronologically
         */
        public String getSortKey() {
            return String.format("%s_%03d_%s",
                slot.getDayOfWeek() != null ? slot.getDayOfWeek().getValue() : 0,
                slot.getPeriodNumber() != null ? slot.getPeriodNumber() : 0,
                slot.getStartTime() != null ? slot.getStartTime().toString() : "00:00:00"
            );
        }
    }

    // ========================================================================
    // INNER CLASS: Event Display
    // ========================================================================

    /**
     * Display wrapper for events and IEP meetings
     */
    public static class EventDisplay {
        private final Event event;

        public EventDisplay(Event event) {
            this.event = event;
        }

        public String getDay() {
            if (event.getStartDateTime() != null) {
                DayOfWeek dayOfWeek = event.getStartDateTime().getDayOfWeek();
                return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault());
            }
            return "--";
        }

        public String getTime() {
            if (event.getStartDateTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
                String start = event.getStartDateTime().toLocalTime().format(formatter);
                if (event.getEndDateTime() != null) {
                    String end = event.getEndDateTime().toLocalTime().format(formatter);
                    return start + " - " + end;
                }
                return start;
            }
            return "--";
        }

        public String getType() {
            if (event.getEventType() != null) {
                return event.getEventType().getDisplayName();
            }
            return "Event";
        }

        public String getName() {
            return (event.getName() != null) ? event.getName() : "--";
        }

        public String getLocation() {
            return (event.getLocation() != null && !event.getLocation().trim().isEmpty())
                ? event.getLocation()
                : "--";
        }

        public String getNotes() {
            String notes = event.getDescription();
            if (Boolean.TRUE.equals(event.getIsBlocksScheduling())) {
                notes = (notes != null) ? "Blocks Scheduling: " + notes : "Blocks Scheduling";
            }
            return (notes != null && !notes.trim().isEmpty()) ? notes : "";
        }
    }
}
