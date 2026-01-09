package com.heronix.ui.components;

import com.heronix.util.FlexibleTimeParser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;

import java.time.LocalTime;

/**
 * TimePickerField - Custom time picker with hour and minute spinners
 * Provides a fast, user-friendly way to input time values
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-06
 */
public class TimePickerField extends HBox {

    private final Spinner<Integer> hourSpinner;
    private final Spinner<Integer> minuteSpinner;
    private final ObjectProperty<LocalTime> time = new SimpleObjectProperty<>();

    /**
     * Create a new TimePickerField with default time (current time)
     */
    public TimePickerField() {
        this(LocalTime.now());
    }

    /**
     * Create a new TimePickerField with specified initial time
     */
    public TimePickerField(LocalTime initialTime) {
        super(5);
        setAlignment(Pos.CENTER_LEFT);

        // Hour spinner (0-23)
        hourSpinner = new Spinner<>();
        SpinnerValueFactory<Integer> hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23,
                initialTime != null ? initialTime.getHour() : LocalTime.now().getHour());
        hourFactory.setWrapAround(true);
        hourSpinner.setValueFactory(hourFactory);
        hourSpinner.setPrefWidth(70);
        hourSpinner.setEditable(true);
        hourSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);

        // Minute spinner (0-59)
        minuteSpinner = new Spinner<>();
        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59,
                initialTime != null ? initialTime.getMinute() : LocalTime.now().getMinute());
        minuteFactory.setWrapAround(true);
        minuteSpinner.setValueFactory(minuteFactory);
        minuteSpinner.setPrefWidth(70);
        minuteSpinner.setEditable(true);
        minuteSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);

        // Labels
        Label colonLabel = new Label(":");
        colonLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Add listeners to update the time property
        hourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTime());
        minuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTime());

        // Build the layout
        getChildren().addAll(hourSpinner, colonLabel, minuteSpinner);

        // Set initial time
        if (initialTime != null) {
            setTime(initialTime);
        }

        // Style
        setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 3; -fx-padding: 2;");
    }

    /**
     * Update the time property based on spinner values
     */
    private void updateTime() {
        try {
            int hour = hourSpinner.getValue();
            int minute = minuteSpinner.getValue();
            time.set(LocalTime.of(hour, minute));
        } catch (Exception e) {
            // Invalid time, ignore
        }
    }

    /**
     * Get the current time value
     */
    public LocalTime getTime() {
        return time.get();
    }

    /**
     * Set the time value
     */
    public void setTime(LocalTime time) {
        if (time != null) {
            hourSpinner.getValueFactory().setValue(time.getHour());
            minuteSpinner.getValueFactory().setValue(time.getMinute());
            this.time.set(time);
        }
    }

    /**
     * Get the time property for binding
     */
    public ObjectProperty<LocalTime> timeProperty() {
        return time;
    }

    /**
     * Set prompt text style (optional, for consistency)
     */
    public void setPromptText(String text) {
        // For compatibility with TextField API
        // Could add a label above or tooltip
    }

    /**
     * Clear the time pickers
     */
    public void clear() {
        hourSpinner.getValueFactory().setValue(0);
        minuteSpinner.getValueFactory().setValue(0);
    }

    /**
     * Set editable state
     */
    public void setEditable(boolean editable) {
        hourSpinner.setDisable(!editable);
        minuteSpinner.setDisable(!editable);
    }

    /**
     * Check if empty (always returns false for spinners, but included for compatibility)
     */
    public boolean isEmpty() {
        return time.get() == null;
    }

    /**
     * Get text representation (HH:mm format)
     */
    public String getText() {
        LocalTime t = getTime();
        if (t != null) {
            return String.format("%02d:%02d", t.getHour(), t.getMinute());
        }
        return "";
    }

    /**
     * Set time from text (supports flexible formats: 12-hour with AM/PM, 24-hour, etc.)
     * Examples: "9:00 AM", "9:00am", "2:30 PM", "14:30", "9 AM"
     */
    public void setText(String text) {
        if (text != null && !text.isEmpty()) {
            LocalTime t = FlexibleTimeParser.parse(text);
            if (t != null) {
                setTime(t);
            }
        }
    }
}
