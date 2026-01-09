package com.heronix.ui.helper;

import com.heronix.model.domain.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * SCHEDULE VIEW HELPER
 * Helper methods for generating various schedule views
 * ═══════════════════════════════════════════════════════════════
 * 
 * Location: src/main/java/com/eduscheduler/ui/helper/ScheduleViewHelper.java
 * 
 * Provides:
 * ✓ Calendar view generation (daily/weekly/monthly)
 * ✓ Teacher-specific schedule view
 * ✓ Room utilization view
 * ✓ Student schedule view
 * ✓ Color-coded visualizations
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-02
 */
public class ScheduleViewHelper {

    // ═══════════════════════════════════════════════════════════════
    // CALENDAR VIEWS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a weekly calendar view
     */
    public static GridPane createWeeklyView(LocalDate weekStart, List<ScheduleSlot> slots) {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10;");

        // Header row with days
        String[] days = { "Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };
        for (int col = 0; col < days.length; col++) {
            VBox headerCell = createHeaderCell(days[col]);
            grid.add(headerCell, col, 0);
            GridPane.setHgrow(headerCell, Priority.ALWAYS);
        }

        // Get all unique time slots
        List<LocalTime> times = slots.stream()
                .map(ScheduleSlot::getStartTime)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Create time rows
        int row = 1;
        for (LocalTime time : times) {
            // Time column
            VBox timeCell = createTimeCell(time);
            grid.add(timeCell, 0, row);

            // Day columns
            for (int day = 1; day <= 5; day++) {
                final int dayNum = day;
                LocalDate date = weekStart.plusDays(day - 1);

                List<ScheduleSlot> daySlotsAtTime = slots.stream()
                        .filter(s -> s.getDayOfWeek().getValue() == dayNum)
                        .filter(s -> s.getStartTime().equals(time))
                        .collect(Collectors.toList());

                VBox slotCell = createSlotCell(daySlotsAtTime);
                grid.add(slotCell, day, row);
                GridPane.setHgrow(slotCell, Priority.ALWAYS);
            }

            row++;
        }

        return grid;
    }

    /**
     * Create a monthly calendar view
     */
    public static GridPane createMonthlyView(YearMonth month, List<ScheduleSlot> slots) {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10;");

        // Day headers
        String[] dayNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        for (int col = 0; col < 7; col++) {
            Label dayLabel = new Label(dayNames[col]);
            dayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            dayLabel.setStyle(
                    "-fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 10; -fx-alignment: center;");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            grid.add(dayLabel, col, 0);
            GridPane.setHgrow(dayLabel, Priority.ALWAYS);
        }

        // Get first day of month and number of days
        LocalDate firstDay = month.atDay(1);
        int daysInMonth = month.lengthOfMonth();
        int startDayOfWeek = firstDay.getDayOfWeek().getValue() % 7; // Convert to 0=Sunday

        // Group slots by date
        Map<LocalDate, List<ScheduleSlot>> slotsByDate = slots.stream()
                .collect(Collectors.groupingBy(slot -> {
                    // Convert DayOfWeek enum to LocalDate
                    int dayValue = slot.getDayOfWeek().getValue();
                    return firstDay.plusDays(dayValue - 1);
                }));

        // Create day cells
        int row = 1;
        int col = startDayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = month.atDay(day);
            List<ScheduleSlot> daySlots = slotsByDate.getOrDefault(date, new ArrayList<>());

            VBox dayCell = createMonthDayCell(date, daySlots);
            grid.add(dayCell, col, row);
            GridPane.setHgrow(dayCell, Priority.ALWAYS);
            GridPane.setVgrow(dayCell, Priority.ALWAYS);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        return grid;
    }

    // ═══════════════════════════════════════════════════════════════
    // TEACHER VIEW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a teacher-specific schedule view
     */
    public static VBox createTeacherView(Teacher teacher, List<ScheduleSlot> slots) {
        VBox container = new VBox(15);
        container.setStyle("-fx-padding: 20; -fx-background-color: white;");

        // Teacher header
        HBox header = createTeacherHeader(teacher, slots);
        container.getChildren().add(header);

        // Filter slots for this teacher
        List<ScheduleSlot> teacherSlots = slots.stream()
                .filter(s -> s.getTeacher() != null && s.getTeacher().equals(teacher))
                .sorted(Comparator.comparing(ScheduleSlot::getDayOfWeek)
                        .thenComparing(ScheduleSlot::getStartTime))
                .collect(Collectors.toList());

        // Group by day
        Map<java.time.DayOfWeek, List<ScheduleSlot>> slotsByDay = teacherSlots.stream()
                .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek));

        // Create day sections
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            if (slotsByDay.containsKey(day)) {
                VBox daySection = createTeacherDaySection(day, slotsByDay.get(day));
                container.getChildren().add(daySection);
            }
        }

        return container;
    }

    private static HBox createTeacherHeader(Teacher teacher, List<ScheduleSlot> slots) {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #3498db; -fx-padding: 15; -fx-background-radius: 5;");

        VBox teacherInfo = new VBox(5);

        Label nameLabel = new Label(teacher.getName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.WHITE);

        Label emailLabel = new Label(teacher.getEmail());
        emailLabel.setFont(Font.font("Arial", 12));
        emailLabel.setTextFill(Color.web("#ecf0f1"));

        teacherInfo.getChildren().addAll(nameLabel, emailLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox stats = new VBox(5);
        stats.setAlignment(Pos.CENTER_RIGHT);

        long totalSlots = slots.stream()
                .filter(s -> s.getTeacher() != null && s.getTeacher().equals(teacher))
                .count();

        Label slotsLabel = new Label("Total Classes: " + totalSlots);
        slotsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        slotsLabel.setTextFill(Color.WHITE);

        stats.getChildren().add(slotsLabel);

        header.getChildren().addAll(teacherInfo, spacer, stats);

        return header;
    }

    private static VBox createTeacherDaySection(java.time.DayOfWeek day, List<ScheduleSlot> slots) {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 15;");

        Label dayLabel = new Label(day.getDisplayName(TextStyle.FULL, Locale.US));
        dayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        dayLabel.setTextFill(Color.web("#2c3e50"));

        section.getChildren().add(dayLabel);

        for (ScheduleSlot slot : slots) {
            HBox slotBox = createTeacherSlotBox(slot);
            section.getChildren().add(slotBox);
        }

        return section;
    }

    private static HBox createTeacherSlotBox(ScheduleSlot slot) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 3;");

        // Time
        VBox timeBox = new VBox(2);
        Label startLabel = new Label(slot.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a")));
        startLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label endLabel = new Label(slot.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a")));
        endLabel.setFont(Font.font("Arial", 10));
        endLabel.setTextFill(Color.GRAY);
        timeBox.getChildren().addAll(startLabel, endLabel);
        timeBox.setMinWidth(80);

        // Course
        VBox courseBox = new VBox(2);
        Label courseLabel = new Label(slot.getCourse() != null ? slot.getCourse().getCourseName() : "No Course");
        courseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        Label codeLabel = new Label(slot.getCourse() != null ? slot.getCourse().getCourseCode() : "");
        codeLabel.setFont(Font.font("Arial", 10));
        codeLabel.setTextFill(Color.GRAY);
        courseBox.getChildren().addAll(courseLabel, codeLabel);
        HBox.setHgrow(courseBox, Priority.ALWAYS);

        // Room
        VBox roomBox = new VBox(2);
        roomBox.setAlignment(Pos.CENTER_RIGHT);
        Label roomLabel = new Label(slot.getRoom() != null ? slot.getRoom().getRoomNumber() : "No Room");
        roomLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label studentsLabel = new Label(slot.getEnrolledStudents() + " students");
        studentsLabel.setFont(Font.font("Arial", 10));
        studentsLabel.setTextFill(Color.GRAY);
        roomBox.getChildren().addAll(roomLabel, studentsLabel);
        roomBox.setMinWidth(100);

        box.getChildren().addAll(timeBox, courseBox, roomBox);

        return box;
    }

    // ═══════════════════════════════════════════════════════════════
    // ROOM VIEW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a room utilization view
     */
    public static VBox createRoomView(Room room, List<ScheduleSlot> slots) {
        VBox container = new VBox(15);
        container.setStyle("-fx-padding: 20; -fx-background-color: white;");

        // Room header
        HBox header = createRoomHeader(room, slots);
        container.getChildren().add(header);

        // Filter slots for this room
        List<ScheduleSlot> roomSlots = slots.stream()
                .filter(s -> s.getRoom() != null && s.getRoom().equals(room))
                .sorted(Comparator.comparing(ScheduleSlot::getDayOfWeek)
                        .thenComparing(ScheduleSlot::getStartTime))
                .collect(Collectors.toList());

        // Create weekly grid
        GridPane weekGrid = createWeeklyView(LocalDate.now(), roomSlots);
        container.getChildren().add(weekGrid);

        // Utilization statistics
        VBox stats = createRoomStatistics(roomSlots);
        container.getChildren().add(stats);

        return container;
    }

    private static HBox createRoomHeader(Room room, List<ScheduleSlot> slots) {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #e74c3c; -fx-padding: 15; -fx-background-radius: 5;");

        VBox roomInfo = new VBox(5);

        Label nameLabel = new Label("Room " + room.getRoomNumber());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.WHITE);

        Label typeLabel = new Label(room.getRoomType().toString());
        typeLabel.setFont(Font.font("Arial", 12));
        typeLabel.setTextFill(Color.web("#ecf0f1"));

        roomInfo.getChildren().addAll(nameLabel, typeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox stats = new VBox(5);
        stats.setAlignment(Pos.CENTER_RIGHT);

        Label capacityLabel = new Label("Capacity: " + room.getCapacity());
        capacityLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        capacityLabel.setTextFill(Color.WHITE);

        stats.getChildren().add(capacityLabel);

        header.getChildren().addAll(roomInfo, spacer, stats);

        return header;
    }

    private static VBox createRoomStatistics(List<ScheduleSlot> roomSlots) {
        VBox stats = new VBox(10);
        stats.setStyle("-fx-border-color: #95a5a6; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 15;");

        Label title = new Label("Utilization Statistics");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        int totalSlots = roomSlots.size();
        int maxPossibleSlots = 40; // 8 periods × 5 days
        double utilization = (double) totalSlots / maxPossibleSlots * 100;

        Label utilizationLabel = new Label(String.format("Utilization: %.1f%% (%d/%d periods)",
                utilization, totalSlots, maxPossibleSlots));
        utilizationLabel.setFont(Font.font("Arial", 14));

        stats.getChildren().addAll(title, utilizationLabel);

        return stats;
    }

    // ═══════════════════════════════════════════════════════════════
    // STUDENT VIEW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a student schedule view
     */
    public static VBox createStudentView(Student student, List<ScheduleSlot> slots) {
        VBox container = new VBox(15);
        container.setStyle("-fx-padding: 20; -fx-background-color: white;");

        // Student header
        HBox header = createStudentHeader(student);
        container.getChildren().add(header);

        // Filter slots for this student
        List<ScheduleSlot> studentSlots = slots.stream()
                .filter(s -> s.getStudents() != null && s.getStudents().contains(student))
                .sorted(Comparator.comparing(ScheduleSlot::getDayOfWeek)
                        .thenComparing(ScheduleSlot::getStartTime))
                .collect(Collectors.toList());

        // Group by day
        Map<java.time.DayOfWeek, List<ScheduleSlot>> slotsByDay = studentSlots.stream()
                .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek));

        // Create day sections
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            if (slotsByDay.containsKey(day)) {
                VBox daySection = createStudentDaySection(day, slotsByDay.get(day));
                container.getChildren().add(daySection);
            }
        }

        return container;
    }

    private static HBox createStudentHeader(Student student) {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #16a085; -fx-padding: 15; -fx-background-radius: 5;");

        VBox studentInfo = new VBox(5);

        Label nameLabel = new Label(student.getFirstName() + " " + student.getLastName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.WHITE);

        Label gradeLabel = new Label("Grade: " + student.getGradeLevel());
        gradeLabel.setFont(Font.font("Arial", 12));
        gradeLabel.setTextFill(Color.web("#ecf0f1"));

        studentInfo.getChildren().addAll(nameLabel, gradeLabel);

        header.getChildren().add(studentInfo);

        return header;
    }

    private static VBox createStudentDaySection(java.time.DayOfWeek day, List<ScheduleSlot> slots) {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 15;");

        Label dayLabel = new Label(day.getDisplayName(TextStyle.FULL, Locale.US));
        dayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        dayLabel.setTextFill(Color.web("#2c3e50"));

        section.getChildren().add(dayLabel);

        for (ScheduleSlot slot : slots) {
            HBox slotBox = createStudentSlotBox(slot);
            section.getChildren().add(slotBox);
        }

        return section;
    }

    private static HBox createStudentSlotBox(ScheduleSlot slot) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #e8f8f5; -fx-padding: 10; -fx-background-radius: 3;");

        // Time
        Label timeLabel = new Label(slot.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a")));
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        timeLabel.setMinWidth(80);

        // Course
        VBox courseBox = new VBox(2);
        Label courseLabel = new Label(slot.getCourse() != null ? slot.getCourse().getCourseName() : "");
        courseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        Label teacherLabel = new Label(slot.getTeacher() != null ? slot.getTeacher().getName() : "");
        teacherLabel.setFont(Font.font("Arial", 10));
        teacherLabel.setTextFill(Color.GRAY);
        courseBox.getChildren().addAll(courseLabel, teacherLabel);
        HBox.setHgrow(courseBox, Priority.ALWAYS);

        // Room
        Label roomLabel = new Label(slot.getRoom() != null ? slot.getRoom().getRoomNumber() : "");
        roomLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        roomLabel.setMinWidth(80);
        roomLabel.setAlignment(Pos.CENTER_RIGHT);

        box.getChildren().addAll(timeLabel, courseBox, roomLabel);

        return box;
    }

    // ═══════════════════════════════════════════════════════════════
    // CELL CREATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static VBox createHeaderCell(String text) {
        VBox cell = new VBox();
        cell.setAlignment(Pos.CENTER);
        cell.setStyle("-fx-background-color: #34495e; -fx-padding: 10;");

        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.WHITE);

        cell.getChildren().add(label);
        cell.setMinWidth(150);

        return cell;
    }

    private static VBox createTimeCell(LocalTime time) {
        VBox cell = new VBox();
        cell.setAlignment(Pos.CENTER);
        cell.setStyle("-fx-background-color: #95a5a6; -fx-padding: 10;");

        Label label = new Label(time.format(DateTimeFormatter.ofPattern("h:mm a")));
        label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        label.setTextFill(Color.WHITE);

        cell.getChildren().add(label);
        cell.setMinHeight(80);

        return cell;
    }

    private static VBox createSlotCell(List<ScheduleSlot> slots) {
        VBox cell = new VBox(5);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 8;");
        cell.setMinHeight(80);
        cell.setMinWidth(150);

        for (ScheduleSlot slot : slots) {
            VBox slotInfo = new VBox(2);

            Label courseLabel = new Label(slot.getCourse() != null ? slot.getCourse().getCourseName() : "");
            courseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            courseLabel.setWrapText(true);

            Label teacherLabel = new Label(slot.getTeacher() != null ? slot.getTeacher().getName() : "");
            teacherLabel.setFont(Font.font("Arial", 9));
            teacherLabel.setTextFill(Color.GRAY);

            Label roomLabel = new Label(slot.getRoom() != null ? slot.getRoom().getRoomNumber() : "");
            roomLabel.setFont(Font.font("Arial", 9));
            roomLabel.setTextFill(Color.GRAY);

            slotInfo.getChildren().addAll(courseLabel, teacherLabel, roomLabel);

            // Color coding
            if (slot.getCourse() != null && slot.getCourse().getSubject() != null) {
                String bgColor = getSubjectColorHex(slot.getCourse().getSubject());
                slotInfo.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 5; -fx-background-radius: 3;");
            }

            cell.getChildren().add(slotInfo);
        }

        return cell;
    }

    private static VBox createMonthDayCell(LocalDate date, List<ScheduleSlot> slots) {
        VBox cell = new VBox(5);
        cell.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 8;");
        cell.setMinHeight(100);
        cell.setMinWidth(120);

        Label dateLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        dateLabel.setTextFill(Color.web("#2c3e50"));

        cell.getChildren().add(dateLabel);

        for (ScheduleSlot slot : slots.stream().limit(3).collect(Collectors.toList())) {
            Label slotLabel = new Label(slot.getCourse() != null ? slot.getCourse().getCourseCode() : "Class");
            slotLabel.setFont(Font.font("Arial", 9));
            slotLabel.setStyle(
                    "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 2; -fx-background-radius: 3;");
            cell.getChildren().add(slotLabel);
        }

        if (slots.size() > 3) {
            Label moreLabel = new Label("+" + (slots.size() - 3) + " more");
            moreLabel.setFont(Font.font("Arial", 8));
            moreLabel.setTextFill(Color.GRAY);
            cell.getChildren().add(moreLabel);
        }

        return cell;
    }

    private static String getSubjectColorHex(String subject) {
        if (subject == null)
            return "#ffffff";

        return switch (subject.toLowerCase()) {
            case "math" -> "#add8e6";
            case "science" -> "#90ee90";
            case "english" -> "#ffdab9";
            case "history" -> "#dda0dd";
            case "pe", "physical education" -> "#ffffe0";
            case "art" -> "#ffb6c1";
            case "music" -> "#e6e6fa";
            default -> "#f0f0f0";
        };
    }
}
