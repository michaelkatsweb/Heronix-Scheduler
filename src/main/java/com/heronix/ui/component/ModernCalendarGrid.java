package com.heronix.ui.component;

import com.heronix.model.domain.ScheduleSlot;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.model.domain.Room;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ MODERN CALENDAR GRID COMPONENT 
 * ║  Schedule Display 
 * ╚══════════════════════════════════════════════════════════════════════════╝
 * 
 * Location: src/main/java/com/eduscheduler/ui/component/ModernCalendarGrid.java
 * 
 * Purpose:
 * - Render schedules in modern, color-coded calendar format
 * - Support weekly and daily views
 * - Interactive hover tooltips
 * - Subject-based color coding
 * - Conflict highlighting
 * 
 * Features:
 * ✓ Clean, modern visual design
 * ✓ Color-coded by subject area
 * ✓ Hover tooltips with full details
 * ✓ Responsive grid layout
 * ✓ Conflict indicators
 * ✓ Empty slot handling
 * 
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-10-21
 */
public class ModernCalendarGrid {

    // ════════════════════════════════════════════════════════════════════════
    // COLOR SCHEME ENUM
    // ════════════════════════════════════════════════════════════════════════

    public enum ColorScheme {
        MATHEMATICS("#2563eb", "Blue"),
        SCIENCE("#10b981", "Green"),
        ENGLISH("#f59e0b", "Orange"),
        HISTORY("#ef4444", "Red"),
        PHYSICAL_EDUCATION("#06b6d4", "Cyan"),
        ARTS("#a855f7", "Purple"),
        TECHNOLOGY("#8b5cf6", "Violet"),
        BUSINESS("#14b8a6", "Teal"),
        LUNCH("#f3f4f6", "Light Gray"),
        OTHER("#6b7280", "Gray");

        private final String colorHex;
        private final String displayName;

        ColorScheme(String colorHex, String displayName) {
            this.colorHex = colorHex;
            this.displayName = displayName;
        }

        public String getColorHex() {
            return colorHex;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONSTANTS - Color scheme and formatting
    // ════════════════════════════════════════════════════════════════════════

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    // Subject color mapping (Material Design inspired colors)
    private static final Map<String, String> SUBJECT_COLORS = new HashMap<>();

    static {
        // Mathematics & STEM
        SUBJECT_COLORS.put("Mathematics", "#2563eb"); // Blue
        SUBJECT_COLORS.put("Algebra", "#2563eb");
        SUBJECT_COLORS.put("Geometry", "#2563eb");
        SUBJECT_COLORS.put("Calculus", "#2563eb");
        SUBJECT_COLORS.put("Statistics", "#2563eb");

        // Sciences
        SUBJECT_COLORS.put("Science", "#10b981"); // Green
        SUBJECT_COLORS.put("Biology", "#10b981");
        SUBJECT_COLORS.put("Chemistry", "#059669");
        SUBJECT_COLORS.put("Physics", "#047857");
        SUBJECT_COLORS.put("Environmental Science", "#10b981");

        // Languages & Literature
        SUBJECT_COLORS.put("English", "#f59e0b"); // Orange
        SUBJECT_COLORS.put("Literature", "#f59e0b");
        SUBJECT_COLORS.put("Reading", "#f59e0b");
        SUBJECT_COLORS.put("Spanish", "#fb923c");
        SUBJECT_COLORS.put("French", "#fb923c");
        SUBJECT_COLORS.put("Language Arts", "#f59e0b");

        // Social Studies
        SUBJECT_COLORS.put("History", "#ef4444"); // Red
        SUBJECT_COLORS.put("World History", "#ef4444");
        SUBJECT_COLORS.put("US History", "#ef4444");
        SUBJECT_COLORS.put("Geography", "#dc2626");
        SUBJECT_COLORS.put("Social Studies", "#ef4444");

        // Physical Education & Health
        SUBJECT_COLORS.put("Physical Education", "#06b6d4"); // Cyan
        SUBJECT_COLORS.put("PE", "#06b6d4");
        SUBJECT_COLORS.put("Health", "#06b6d4");
        SUBJECT_COLORS.put("Athletics", "#06b6d4");

        // Arts
        SUBJECT_COLORS.put("Art", "#a855f7"); // Purple
        SUBJECT_COLORS.put("Music", "#9333ea");
        SUBJECT_COLORS.put("Drama", "#a855f7");
        SUBJECT_COLORS.put("Theater", "#a855f7");

        // Technology & Computer Science
        SUBJECT_COLORS.put("Programming", "#8b5cf6"); // Violet
        SUBJECT_COLORS.put("Computer Science", "#8b5cf6");
        SUBJECT_COLORS.put("Technology", "#8b5cf6");
        SUBJECT_COLORS.put("Robotics", "#8b5cf6");

        // Business & Career
        SUBJECT_COLORS.put("Business", "#14b8a6"); // Teal
        SUBJECT_COLORS.put("Economics", "#14b8a6");
        SUBJECT_COLORS.put("Career", "#14b8a6");

        // Electives & Other
        SUBJECT_COLORS.put("Study Hall", "#6b7280"); // Gray
        SUBJECT_COLORS.put("Lunch", "#f3f4f6"); // Light Gray
        SUBJECT_COLORS.put("Advisory", "#6b7280");
        SUBJECT_COLORS.put("Other", "#9ca3af");
    }

    // Grid styling constants
    private static final String HEADER_STYLE = "-fx-background-color: #1e293b; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: center; " +
            "-fx-padding: 12px;";

    private static final String TIME_LABEL_STYLE = "-fx-background-color: #f1f5f9; " +
            "-fx-text-fill: #475569; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: center-right; " +
            "-fx-padding: 8px 12px;";

    private static final String EMPTY_CELL_STYLE = "-fx-background-color: #ffffff; " +
            "-fx-border-color: #e2e8f0; " +
            "-fx-border-width: 1px;";

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC API - Main rendering methods
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Get the subject to color mapping
     * Used by ColorLegend and other components
     * 
     * @return Map of subject names to hex color codes
     */
    public static Map<String, String> getSubjectColors() {
        return new HashMap<>(SUBJECT_COLORS);
    }

    /**
     * Render a weekly calendar grid (Monday-Friday)
     * 
     * @param slots               Schedule slots to display
     * @param startTime           Start time of school day
     * @param endTime             End time of school day
     * @param slotDurationMinutes Duration of each time slot in minutes
     * @return GridPane containing the rendered calendar
     */
    public static GridPane renderWeeklyGrid(
            List<ScheduleSlot> slots,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes) {

        GridPane grid = new GridPane();
        setupGrid(grid);

        // Create headers (Monday - Friday)
        createWeekHeaders(grid);

        // Group slots by time and day
        Map<LocalTime, Map<DayOfWeek, List<ScheduleSlot>>> timeSlots = groupSlotsByTimeAndDay(slots, startTime, endTime,
                slotDurationMinutes);

        // Render each time slot
        int row = 1;
        for (LocalTime time : generateTimeSlots(startTime, endTime, slotDurationMinutes)) {
            // Add time label
            Label timeLabel = createTimeLabel(time);
            grid.add(timeLabel, 0, row);

            // Add day cells
            Map<DayOfWeek, List<ScheduleSlot>> daySlots = timeSlots.get(time);
            if (daySlots == null) {
                daySlots = new HashMap<>();
            }

            for (int col = 0; col < 5; col++) {
                DayOfWeek day = DayOfWeek.of(col + 1); // Monday = 1, Friday = 5
                List<ScheduleSlot> slotsForCell = daySlots.getOrDefault(day, new ArrayList<>());

                VBox cell = createCell(slotsForCell, time, slotDurationMinutes);
                grid.add(cell, col + 1, row);
            }

            row++;
        }

        return grid;
    }

    /**
     * Render a daily calendar grid (single day)
     * 
     * @param slots               Schedule slots for the day
     * @param day                 Day of week to display
     * @param startTime           Start time of school day
     * @param endTime             End time of school day
     * @param slotDurationMinutes Duration of each time slot in minutes
     * @return GridPane containing the rendered calendar
     */
    public static GridPane renderDailyGrid(
            List<ScheduleSlot> slots,
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes) {

        GridPane grid = new GridPane();
        setupGrid(grid);

        // Create single day header
        createDailyHeader(grid, day);

        // Group slots by time
        Map<LocalTime, List<ScheduleSlot>> timeSlots = groupSlotsByTime(slots, startTime, endTime, slotDurationMinutes);

        // Render each time slot
        int row = 1;
        for (LocalTime time : generateTimeSlots(startTime, endTime, slotDurationMinutes)) {
            // Add time label
            Label timeLabel = createTimeLabel(time);
            grid.add(timeLabel, 0, row);

            // Add cell for this time
            List<ScheduleSlot> slotsForCell = timeSlots.getOrDefault(time, new ArrayList<>());
            VBox cell = createCell(slotsForCell, time, slotDurationMinutes);
            grid.add(cell, 1, row);

            row++;
        }

        return grid;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS - Grid setup and cell creation
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Setup basic grid properties
     */
    private static void setupGrid(GridPane grid) {
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: #f8fafc;");
    }

    /**
     * Create week headers (Mon-Fri)
     */
    private static void createWeekHeaders(GridPane grid) {
        // Time column header (empty)
        Label timeHeader = new Label("Time");
        timeHeader.setStyle(HEADER_STYLE);
        timeHeader.setMinWidth(80);
        grid.add(timeHeader, 0, 0);

        // Day headers
        String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };
        for (int col = 0; col < days.length; col++) {
            Label dayHeader = new Label(days[col]);
            dayHeader.setStyle(HEADER_STYLE);
            dayHeader.setMinWidth(180);
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(dayHeader, Priority.ALWAYS);
            grid.add(dayHeader, col + 1, 0);
        }
    }

    /**
     * Create daily view header
     */
    private static void createDailyHeader(GridPane grid, DayOfWeek day) {
        // Time column header
        Label timeHeader = new Label("Time");
        timeHeader.setStyle(HEADER_STYLE);
        timeHeader.setMinWidth(80);
        grid.add(timeHeader, 0, 0);

        // Day header
        Label dayHeader = new Label(day.toString());
        dayHeader.setStyle(HEADER_STYLE);
        dayHeader.setMinWidth(400);
        dayHeader.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(dayHeader, Priority.ALWAYS);
        grid.add(dayHeader, 1, 0);
    }

    /**
     * Create time label for row header
     */
    private static Label createTimeLabel(LocalTime time) {
        Label label = new Label(time.format(TIME_FORMATTER));
        label.setStyle(TIME_LABEL_STYLE);
        label.setMinWidth(80);
        label.setMinHeight(60);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        return label;
    }

    /**
     * Create a cell for a specific time slot
     */
    private static VBox createCell(List<ScheduleSlot> slots, LocalTime time, int duration) {
        VBox cell = new VBox(5);
        cell.setMinHeight(60);
        cell.setMinWidth(180);
        cell.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        cell.setPadding(new Insets(8));

        if (slots.isEmpty()) {
            // Empty cell
            cell.setStyle(EMPTY_CELL_STYLE);
        } else if (slots.size() == 1) {
            // Single slot - display full info
            ScheduleSlot slot = slots.get(0);
            styleSlotCell(cell, slot);
            addSlotContent(cell, slot);
            addTooltip(cell, slot);
        } else {
            // Multiple slots (conflict!) - show warning
            cell.setStyle(
                    "-fx-background-color: #fef2f2; " +
                            "-fx-border-color: #dc2626; " +
                            "-fx-border-width: 2px;");

            Label conflictLabel = new Label("⚠️ CONFLICT");
            conflictLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            cell.getChildren().add(conflictLabel);

            for (ScheduleSlot slot : slots) {
                Label slotLabel = new Label(getShortSlotDescription(slot));
                slotLabel.setStyle("-fx-font-size: 10px;");
                cell.getChildren().add(slotLabel);
            }
        }

        return cell;
    }

    /**
     * Style a cell based on slot subject
     */
    private static void styleSlotCell(VBox cell, ScheduleSlot slot) {
        String subject = getSlotSubject(slot);
        String color = SUBJECT_COLORS.getOrDefault(subject, "#6b7280");

        cell.setStyle(
                "-fx-background-color: " + color + "18; " + // 18 = 10% opacity
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // Hover effect
        cell.setOnMouseEntered(e -> {
            cell.setStyle(
                    "-fx-background-color: " + color + "28; " + // 28 = 16% opacity
                            "-fx-border-color: " + color + "; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 4px; " +
                            "-fx-background-radius: 4px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3); " +
                            "-fx-cursor: hand;");
        });

        cell.setOnMouseExited(e -> {
            styleSlotCell(cell, slot);
        });
    }

    /**
     * Add content labels to slot cell
     */
    private static void addSlotContent(VBox cell, ScheduleSlot slot) {
        Course course = slot.getCourse();
        Teacher teacher = slot.getTeacher();
        Room room = slot.getRoom();

        // Course name (bold, larger)
        if (course != null) {
            Label courseLabel = new Label(course.getCourseName());
            courseLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            courseLabel.setStyle("-fx-text-fill: #1e293b;");
            courseLabel.setWrapText(true);
            cell.getChildren().add(courseLabel);
        }

        // Teacher name
        if (teacher != null) {
            Label teacherLabel = new Label(" " + teacher.getName());
            teacherLabel.setFont(Font.font("Segoe UI", 10));
            teacherLabel.setStyle("-fx-text-fill: #475569;");
            cell.getChildren().add(teacherLabel);
        }

        // Room number
        if (room != null) {
            Label roomLabel = new Label(" Room " + room.getRoomNumber());
            roomLabel.setFont(Font.font("Segoe UI", 10));
            roomLabel.setStyle("-fx-text-fill: #64748b;");
            cell.getChildren().add(roomLabel);
        }
    }

    /**
     * Add hover tooltip with full slot details
     */
    private static void addTooltip(VBox cell, ScheduleSlot slot) {
        StringBuilder tooltipText = new StringBuilder();

        Course course = slot.getCourse();
        if (course != null) {
            tooltipText.append("📚 ").append(course.getCourseName()).append("\n");
            if (course.getCourseCode() != null) {
                tooltipText.append("Code: ").append(course.getCourseCode()).append("\n");
            }
        }

        Teacher teacher = slot.getTeacher();
        if (teacher != null) {
            tooltipText.append("\n👨‍🏫 Teacher: ").append(teacher.getName());
            if (teacher.getDepartment() != null) {
                tooltipText.append(" (").append(teacher.getDepartment()).append(")");
            }
            tooltipText.append("\n");
        }

        Room room = slot.getRoom();
        if (room != null) {
            tooltipText.append("\n📍 Room: ").append(room.getRoomNumber());
            if (room.getBuilding() != null) {
                tooltipText.append(" - ").append(room.getBuilding());
            }
            tooltipText.append("\n   Capacity: ").append(room.getCapacity()).append(" students\n");
        }

        if (slot.getStartTime() != null && slot.getEndTime() != null) {
            tooltipText.append("\n⏰ Time: ")
                    .append(slot.getStartTime().format(TIME_FORMATTER))
                    .append(" - ")
                    .append(slot.getEndTime().format(TIME_FORMATTER))
                    .append("\n");
        }

        tooltipText.append("\n👥 Students: ")
                .append(slot.getEnrolledStudents());
        if (course != null) {
            tooltipText.append(" / ").append(course.getMaxStudents());
        }

        Tooltip tooltip = new Tooltip(tooltipText.toString());
        tooltip.setShowDelay(Duration.millis(200));
        tooltip.setStyle(
                "-fx-font-size: 11px; " +
                        "-fx-background-color: #1e293b; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10px; " +
                        "-fx-background-radius: 6px;");

        Tooltip.install(cell, tooltip);
    }

    // ════════════════════════════════════════════════════════════════════════
    // DATA PROCESSING METHODS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Group slots by time and day for weekly view
     */
    private static Map<LocalTime, Map<DayOfWeek, List<ScheduleSlot>>> groupSlotsByTimeAndDay(
            List<ScheduleSlot> slots,
            LocalTime startTime,
            LocalTime endTime,
            int slotDuration) {

        Map<LocalTime, Map<DayOfWeek, List<ScheduleSlot>>> grouped = new HashMap<>();

        // Initialize all time slots
        for (LocalTime time : generateTimeSlots(startTime, endTime, slotDuration)) {
            grouped.put(time, new HashMap<>());
        }

        // Group actual slots
        for (ScheduleSlot slot : slots) {
            LocalTime slotTime = findMatchingTimeSlot(
                    slot.getStartTime(), startTime, endTime, slotDuration);

            if (slotTime != null && slot.getDayOfWeek() != null) {
                grouped.computeIfAbsent(slotTime, k -> new HashMap<>())
                        .computeIfAbsent(slot.getDayOfWeek(), k -> new ArrayList<>())
                        .add(slot);
            }
        }

        return grouped;
    }

    /**
     * Group slots by time only for daily view
     */
    private static Map<LocalTime, List<ScheduleSlot>> groupSlotsByTime(
            List<ScheduleSlot> slots,
            LocalTime startTime,
            LocalTime endTime,
            int slotDuration) {

        Map<LocalTime, List<ScheduleSlot>> grouped = new HashMap<>();

        // Initialize all time slots
        for (LocalTime time : generateTimeSlots(startTime, endTime, slotDuration)) {
            grouped.put(time, new ArrayList<>());
        }

        // Group actual slots
        for (ScheduleSlot slot : slots) {
            LocalTime slotTime = findMatchingTimeSlot(
                    slot.getStartTime(), startTime, endTime, slotDuration);

            if (slotTime != null) {
                grouped.computeIfAbsent(slotTime, k -> new ArrayList<>()).add(slot);
            }
        }

        return grouped;
    }

    /**
     * Generate list of time slots for the day
     */
    private static List<LocalTime> generateTimeSlots(
            LocalTime startTime,
            LocalTime endTime,
            int slotDuration) {

        List<LocalTime> timeSlots = new ArrayList<>();
        LocalTime current = startTime;

        while (current.isBefore(endTime)) {
            timeSlots.add(current);
            current = current.plusMinutes(slotDuration);
        }

        return timeSlots;
    }

    /**
     * Find which time slot a given start time belongs to
     */
    private static LocalTime findMatchingTimeSlot(
            LocalTime startTime,
            LocalTime dayStart,
            LocalTime dayEnd,
            int slotDuration) {

        if (startTime == null || startTime.isBefore(dayStart) || startTime.isAfter(dayEnd)) {
            return null;
        }

        LocalTime current = dayStart;
        while (current.isBefore(dayEnd)) {
            LocalTime slotEnd = current.plusMinutes(slotDuration);
            if (!startTime.isBefore(current) && startTime.isBefore(slotEnd)) {
                return current;
            }
            current = slotEnd;
        }

        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Get subject from slot (for color coding)
     */
    private static String getSlotSubject(ScheduleSlot slot) {
        if (slot == null || slot.getCourse() == null) {
            return "Other";
        }

        String subject = slot.getCourse().getSubject();
        return subject != null ? subject : "Other";
    }

    /**
     * Get short description for conflict display
     */
    private static String getShortSlotDescription(ScheduleSlot slot) {
        if (slot.getCourse() != null) {
            return slot.getCourse().getCourseName();
        }
        return "Unknown Course";
    }
}