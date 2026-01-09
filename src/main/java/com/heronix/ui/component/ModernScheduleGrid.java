package com.heronix.ui.component;

import com.heronix.model.domain.ScheduleSlot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Modern, visually stunning schedule grid component
 * Location: src/main/java/com/eduscheduler/ui/component/ModernScheduleGrid.java
 */
public class ModernScheduleGrid extends GridPane {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final Map<String, String> SUBJECT_COLORS = new HashMap<>();

    static {
        SUBJECT_COLORS.put("Mathematics", "#2563eb");
        SUBJECT_COLORS.put("Algebra", "#2563eb");
        SUBJECT_COLORS.put("Geometry", "#2563eb");
        SUBJECT_COLORS.put("Science", "#10b981");
        SUBJECT_COLORS.put("Biology", "#10b981");
        SUBJECT_COLORS.put("Chemistry", "#10b981");
        SUBJECT_COLORS.put("Physics", "#10b981");
        SUBJECT_COLORS.put("English", "#f59e0b");
        SUBJECT_COLORS.put("Literature", "#f59e0b");
        SUBJECT_COLORS.put("Spanish", "#f59e0b");
        SUBJECT_COLORS.put("History", "#ef4444");
        SUBJECT_COLORS.put("World History", "#ef4444");
        SUBJECT_COLORS.put("Physical Education", "#06b6d4");
        SUBJECT_COLORS.put("Art", "#a855f7");
        SUBJECT_COLORS.put("Music", "#a855f7");
        SUBJECT_COLORS.put("Programming", "#8b5cf6");
        SUBJECT_COLORS.put("Introduction", "#8b5cf6");
    }

    public ModernScheduleGrid() {
        setupGrid();
    }

    private void setupGrid() {
        setHgap(12);
        setVgap(12);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8fafc;");
    }

    public void displaySchedule(List<ScheduleSlot> slots) {
        getChildren().clear();

        // Create header
        createHeaders();

        // Group slots by time and day
        Map<String, Map<DayOfWeek, ScheduleSlot>> timeSlots = groupSlotsByTimeAndDay(slots);

        // Sort times
        List<String> sortedTimes = new ArrayList<>(timeSlots.keySet());
        sortedTimes.sort((a, b) -> {
            try {
                return java.time.LocalTime.parse(a, TIME_FORMATTER)
                        .compareTo(java.time.LocalTime.parse(b, TIME_FORMATTER));
            } catch (Exception e) {
                return a.compareTo(b);
            }
        });

        int row = 1;
        for (String time : sortedTimes) {
            Map<DayOfWeek, ScheduleSlot> daySlots = timeSlots.get(time);

            // Time label
            Label timeLabel = createTimeLabel(time);
            add(timeLabel, 0, row);

            // Day slots
            for (int col = 0; col < 5; col++) {
                DayOfWeek day = DayOfWeek.of(col + 1);
                ScheduleSlot slot = daySlots.get(day);

                VBox slotCard = createSlotCard(slot);
                add(slotCard, col + 1, row);
            }

            row++;
        }
    }

    private void createHeaders() {
        // Empty top-left cell
        add(new Label(""), 0, 0);

        // Day headers
        String[] days = { "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" };
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            dayLabel.setTextFill(Color.web("#1e293b"));
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #f8fafc, #f1f5f9);" +
                            "-fx-padding: 16px;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: #e2e8f0;" +
                            "-fx-border-width: 1px;" +
                            "-fx-border-radius: 8px;");
            add(dayLabel, i + 1, 0);
            GridPane.setHgrow(dayLabel, Priority.ALWAYS);
        }
    }

    private Label createTimeLabel(String time) {
        Label label = new Label(time);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        label.setTextFill(Color.web("#64748b"));
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setPadding(new Insets(0, 16, 0, 0));
        return label;
    }

    private VBox createSlotCard(ScheduleSlot slot) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);
        card.setMinHeight(120);
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);

        if (slot == null) {
            // Empty slot
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: #e2e8f0;" +
                            "-fx-border-width: 1px;" +
                            "-fx-border-radius: 8px;" +
                            "-fx-border-style: dashed;");
            Label emptyLabel = new Label("No Class");
            emptyLabel.setTextFill(Color.web("#94a3b8"));
            emptyLabel.setFont(Font.font("Segoe UI", 12));
            card.getChildren().add(emptyLabel);
        } else {
            // Get subject color
            String courseName = slot.getCourse() != null ? slot.getCourse().getCourseName() : "";
            String color = getSubjectColor(courseName);

            // Styled slot card
            String baseStyle = "-fx-background-color: white;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-border-color: " + color + ";" +
                    "-fx-border-width: 0 0 0 4px;" +
                    "-fx-border-radius: 8px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);";
            card.setStyle(baseStyle);

            // Course name
            Label courseLabel = new Label(courseName);
            courseLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            courseLabel.setTextFill(Color.web("#1e293b"));
            courseLabel.setWrapText(true);

            // Teacher name with icon
            String teacherName = slot.getTeacher() != null ? slot.getTeacher().getName() : "";
            HBox teacherBox = new HBox(6);
            teacherBox.setAlignment(Pos.CENTER_LEFT);
            Label teacherIcon = new Label("👨‍🏫");
            Label teacherLabel = new Label(teacherName);
            teacherLabel.setFont(Font.font("Segoe UI", 13));
            teacherLabel.setTextFill(Color.web("#64748b"));
            teacherBox.getChildren().addAll(teacherIcon, teacherLabel);

            // Room badge
            String roomName = slot.getRoom() != null ? slot.getRoom().getRoomNumber() : "";
            Label roomBadge = new Label(roomName);
            roomBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            roomBadge.setTextFill(Color.WHITE);
            roomBadge.setStyle(
                    "-fx-background-color: " + color + ";" +
                            "-fx-padding: 4px 10px;" +
                            "-fx-background-radius: 12px;");

            card.getChildren().addAll(courseLabel, teacherBox, roomBadge);

            // Store base style for hover effects
            final String finalBaseStyle = baseStyle;

            // Hover effect
            card.setOnMouseEntered(e -> card.setStyle(
                    finalBaseStyle.replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);",
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);") +
                            "-fx-cursor: hand;"));

            card.setOnMouseExited(e -> card.setStyle(finalBaseStyle));
        }

        return card;
    }

    private String getSubjectColor(String courseName) {
        for (Map.Entry<String, String> entry : SUBJECT_COLORS.entrySet()) {
            if (courseName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "#64748b"; // Default gray
    }

    private Map<String, Map<DayOfWeek, ScheduleSlot>> groupSlotsByTimeAndDay(List<ScheduleSlot> slots) {
        Map<String, Map<DayOfWeek, ScheduleSlot>> result = new LinkedHashMap<>();

        for (ScheduleSlot slot : slots) {
            if (slot.getTimeSlot() != null && slot.getTimeSlot().getStartTime() != null) {
                String timeKey = slot.getTimeSlot().getStartTime().format(TIME_FORMATTER);
                DayOfWeek day = slot.getTimeSlot().getDayOfWeek();

                result.putIfAbsent(timeKey, new HashMap<>());
                result.get(timeKey).put(day, slot);
            }
        }

        return result;
    }
}