package com.heronix.ui.components;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Room;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.RoomRepository;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Editable TableCell for inline room assignment
 * Provides smart suggestions based on room type and capacity
 *
 * Features:
 * - Auto-detects required room type (Science → Lab, PE → Gym)
 * - Capacity-aware suggestions
 * - Visual capacity indicators
 * - Inline validation with warnings
 * - Instant save on selection
 * - Brief visual feedback on success
 *
 * @since Phase 3 - Smart Data Entry
 * @version 1.0.0
 */
@Slf4j
public class EditableRoomCell extends TableCell<Course, Room> {

    private ComboBox<Room> comboBox;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;

    public EditableRoomCell(ApplicationContext context) {
        this.roomRepository = context.getBean(RoomRepository.class);
        this.courseRepository = context.getBean(CourseRepository.class);
    }

    @Override
    public void startEdit() {
        super.startEdit();

        if (comboBox == null) {
            createComboBox();
        }

        Course course = getTableRow().getItem();
        if (course == null) {
            log.warn("Cannot start edit: course is null");
            return;
        }

        // Load smart suggestions
        List<Room> suggestedRooms = getSmartRoomSuggestions(course);
        comboBox.setItems(FXCollections.observableArrayList(suggestedRooms));
        comboBox.getSelectionModel().select(getItem());

        setText(null);
        setGraphic(comboBox);
        comboBox.show();
        comboBox.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getDisplayText());
        setGraphic(null);
        setStyle("-fx-cursor: hand;");
    }

    @Override
    public void updateItem(Room room, boolean empty) {
        super.updateItem(room, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
            setTooltip(null);
            setStyle("");
        } else {
            if (isEditing()) {
                setText(null);
                setGraphic(comboBox);
            } else {
                setText(getDisplayText());
                setGraphic(null);

                // Add visual cue that cell is editable
                setStyle("-fx-cursor: hand;");

                // Add helpful tooltip
                Course course = getTableRow().getItem();
                if (course != null) {
                    String tooltipText = room == null
                        ? String.format("Double-click to assign room\nRecommended: %s (capacity: %d+)",
                            determineRequiredRoomType(course).getDisplayName(),
                            course.getMaxStudents() != null ? course.getMaxStudents() : 30)
                        : String.format("Current: %s (capacity: %d)\nDouble-click to change",
                            room.getRoomNumber(), room.getCapacity());
                    setTooltip(new Tooltip(tooltipText));
                }
            }
        }
    }

    private void createComboBox() {
        comboBox = new ComboBox<>();
        comboBox.setEditable(true); // Enable search/filter
        comboBox.setPrefWidth(250);
        comboBox.setPromptText("Type to search or select...");

        // Custom display format
        comboBox.setConverter(new StringConverter<Room>() {
            @Override
            public String toString(Room room) {
                if (room == null) return "";
                return String.format("%s - %s (Cap: %d)",
                    room.getRoomNumber(),
                    room.getRoomType() != null ? room.getRoomType().getDisplayName() : "N/A",
                    room.getCapacity());
            }

            @Override
            public Room fromString(String string) {
                return null;
            }
        });

        // Custom cell factory for dropdown items
        comboBox.setCellFactory(lv -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%s - %s (Capacity: %d)",
                        room.getRoomNumber(),
                        room.getRoomType() != null ? room.getRoomType().getDisplayName() : "N/A",
                        room.getCapacity()));

                    // Color-code by capacity match
                    Course course = getTableRow() != null ? getTableRow().getItem() : null;
                    if (course != null && course.getMaxStudents() != null) {
                        int required = course.getMaxStudents();
                        int available = room.getCapacity();

                        String style;
                        if (available >= required && available <= required * 1.2) {
                            style = "-fx-background-color: rgba(76, 175, 80, 0.2);"; // Perfect fit
                        } else if (available >= required) {
                            style = "-fx-background-color: rgba(255, 193, 7, 0.2);"; // Oversized
                        } else {
                            style = "-fx-background-color: rgba(244, 67, 54, 0.2);"; // Too small
                        }
                        setStyle(style);
                    }
                }
            }
        });

        // Handle selection
        comboBox.setOnAction(event -> {
            Room selected = comboBox.getValue();
            if (selected != null) {
                Course course = getTableRow().getItem();
                if (course == null) {
                    log.warn("Cannot save: course is null");
                    cancelEdit();
                    return;
                }

                log.info("Room selected for course {}: {}", course.getCourseCode(), selected.getRoomNumber());

                // Validate before saving
                ValidationResult validation = validateAssignment(course, selected);
                if (validation.isValid()) {
                    // Save immediately
                    if (saveAssignment(course, selected)) {
                        commitEdit(selected);
                        showSuccessFeedback();
                    } else {
                        cancelEdit();
                    }
                } else {
                    // Show warning and allow override
                    boolean proceed = showWarningDialog(course, selected, validation);
                    if (proceed) {
                        if (saveAssignment(course, selected)) {
                            commitEdit(selected);
                            showSuccessFeedback();
                        } else {
                            cancelEdit();
                        }
                    } else {
                        cancelEdit();
                    }
                }
            }
        });

        // Handle ESC key to cancel
        comboBox.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    private String getDisplayText() {
        if (getItem() == null) {
            return "[Click to assign]";
        }
        return String.format("%s (%d)", getItem().getRoomNumber(), getItem().getCapacity());
    }

    /**
     * Get smart room suggestions sorted by:
     * 1. Room type match
     * 2. Capacity (smallest suitable room for efficiency)
     * 3. Room number (alphabetical)
     */
    private List<Room> getSmartRoomSuggestions(Course course) {
        try {
            List<Room> allRooms = roomRepository.findAll();
            RoomType requiredType = determineRequiredRoomType(course);
            int requiredCapacity = course.getMaxStudents() != null ? course.getMaxStudents() : 30;

            return allRooms.stream()
                .filter(r -> r.getActive()) // Only active rooms
                .sorted(Comparator
                    // 1. Exact room type match first
                    .comparing((Room r) -> r.getRoomType() != requiredType)
                    // 2. Has sufficient capacity
                    .thenComparing(r -> r.getCapacity() < requiredCapacity)
                    // 3. Prefer smaller rooms that fit (efficient space usage)
                    .thenComparing(Room::getCapacity)
                    // 4. Alphabetical by room number
                    .thenComparing(Room::getRoomNumber))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error loading smart room suggestions", e);
            return List.of();
        }
    }

    /**
     * Auto-detect required room type based on course subject
     */
    private RoomType determineRequiredRoomType(Course course) {
        String subject = course.getSubject() != null ? course.getSubject().toLowerCase() : "";
        String courseName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";

        // Physical Education
        if (subject.contains("physical education") || subject.contains("pe") ||
            courseName.contains("physical education") || courseName.contains("gym")) {
            return RoomType.GYMNASIUM;
        }

        // Science labs
        if (course.isRequiresLab() || subject.contains("chemistry") ||
            subject.contains("biology") || subject.contains("physics") ||
            courseName.contains("lab")) {
            return RoomType.SCIENCE_LAB;
        }

        // Computer science
        if (subject.contains("computer") || subject.contains("technology") ||
            courseName.contains("computer")) {
            return RoomType.COMPUTER_LAB;
        }

        // Music
        if (subject.contains("music") || subject.contains("band") ||
            subject.contains("orchestra") || subject.contains("chorus")) {
            return RoomType.MUSIC_ROOM;
        }

        // Art
        if (subject.contains("art")) {
            return RoomType.ART_STUDIO;
        }

        // Default to standard classroom
        return RoomType.CLASSROOM;
    }

    /**
     * Validate room assignment
     */
    private ValidationResult validateAssignment(Course course, Room room) {
        ValidationResult result = new ValidationResult();

        // Check capacity
        int required = course.getMaxStudents() != null ? course.getMaxStudents() : 30;
        int available = room.getCapacity();

        if (available < required) {
            result.addWarning("Insufficient Capacity",
                String.format("Room %s has capacity %d but course needs %d students",
                    room.getRoomNumber(), available, required));
        }

        // Check room type mismatch
        RoomType requiredType = determineRequiredRoomType(course);
        if (room.getRoomType() != requiredType && requiredType != RoomType.CLASSROOM) {
            result.addWarning("Room Type Mismatch",
                String.format("Course requires %s but %s is a %s",
                    requiredType.getDisplayName(),
                    room.getRoomNumber(),
                    room.getRoomType() != null ? room.getRoomType().getDisplayName() : "Unknown"));
        }

        return result;
    }

    private boolean showWarningDialog(Course course, Room room, ValidationResult validation) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Assignment Warning");
        alert.setHeaderText("Potential issues detected");

        StringBuilder content = new StringBuilder();
        for (String warning : validation.getWarnings()) {
            content.append("⚠️ ").append(warning).append("\n\n");
        }
        content.append("Do you want to proceed anyway?");

        alert.setContentText(content.toString());

        ButtonType proceedButton = new ButtonType("Proceed Anyway");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(proceedButton, cancelButton);

        return alert.showAndWait().orElse(cancelButton) == proceedButton;
    }

    /**
     * Save assignment to database
     */
    private boolean saveAssignment(Course course, Room room) {
        try {
            course.setRoom(room);
            courseRepository.save(course);

            log.info("Successfully assigned room {} to course {}",
                room.getRoomNumber(), course.getCourseCode());

            return true;
        } catch (Exception e) {
            log.error("Error saving room assignment", e);
            showErrorDialog("Error Saving Assignment",
                "Could not save assignment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Show brief success feedback (green checkmark for 1 second)
     */
    private void showSuccessFeedback() {
        Platform.runLater(() -> {
            String originalText = getText();
            String originalStyle = getStyle();

            setText(originalText + " ✓");
            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                setText(originalText);
                setStyle(originalStyle);
            });
            pause.play();
        });
    }

    private void showErrorDialog(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Validation result holder
     */
    private static class ValidationResult {
        private final List<String> warnings = new java.util.ArrayList<>();

        public void addWarning(String title, String message) {
            warnings.add(title + ": " + message);
        }

        public boolean isValid() {
            return warnings.isEmpty();
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
