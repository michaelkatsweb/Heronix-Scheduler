package com.heronix.ui.component;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.util.function.BiConsumer;

/**
 * Editable Table Cell - Generic inline editing component
 *
 * Supports:
 * - Single-click to edit
 * - Enter to save
 * - ESC to cancel
 * - Tab to move to next cell
 * - Auto-save on focus loss
 * - Custom validation
 * - Custom converters
 *
 * @param <S> Table row type (e.g., Student, Teacher, Course)
 * @param <T> Cell value type (e.g., String, Integer, Boolean)
 *
 * @author Heronix Scheduler Team
 * @version 1.0
 */
public class EditableTableCell<S, T> extends TableCell<S, T> {

    private TextField textField;
    private final StringConverter<T> converter;
    private final BiConsumer<S, T> onCommit;
    private final ObjectProperty<T> editingValue = new SimpleObjectProperty<>();

    /**
     * Create editable cell with default String converter
     */
    public EditableTableCell() {
        this(null, null);
    }

    /**
     * Create editable cell with custom converter
     *
     * @param converter Converts between display String and actual type T
     * @param onCommit Callback when value is committed (rowItem, newValue)
     */
    public EditableTableCell(StringConverter<T> converter, BiConsumer<S, T> onCommit) {
        this.converter = converter != null ? converter : getDefaultConverter();
        this.onCommit = onCommit;

        // Enable editing on single click
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && !isEmpty()) {
                getTableView().edit(getIndex(), getTableColumn());
            }
        });

        // Set style
        getStyleClass().add("editable-table-cell");
    }

    /**
     * Start editing mode
     */
    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }

        super.startEdit();

        if (textField == null) {
            createTextField();
        }

        editingValue.set(getItem());
        textField.setText(converter.toString(getItem()));
        setText(null);
        setGraphic(textField);

        // Select all text and focus
        Platform.runLater(() -> {
            textField.selectAll();
            textField.requestFocus();
        });
    }

    /**
     * Cancel editing mode
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(converter.toString(getItem()));
        setGraphic(null);
        editingValue.set(null);
    }

    /**
     * Update cell display
     */
    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(converter.toString(getItem()));
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(converter.toString(item));
                setGraphic(null);
            }
        }
    }

    /**
     * Create and configure the text field
     */
    private void createTextField() {
        textField = new TextField();
        textField.setMinWidth(getWidth() - getGraphicTextGap() * 2);

        // Commit on Enter
        textField.setOnAction(event -> commitEdit());

        // Handle keyboard events
        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                event.consume();
            } else if (event.getCode() == KeyCode.TAB) {
                commitEdit();
                // Tab will naturally move to next cell
            }
        });

        // Auto-save on focus loss
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && isEditing()) {
                commitEdit();
            }
        });
    }

    /**
     * Commit the edited value
     */
    private void commitEdit() {
        try {
            T newValue = converter.fromString(textField.getText());

            // Validate that value actually changed
            T oldValue = getItem();
            if ((newValue == null && oldValue == null) ||
                (newValue != null && newValue.equals(oldValue))) {
                cancelEdit();
                return;
            }

            // Commit to table
            super.commitEdit(newValue);

            // Call custom commit handler if provided
            if (onCommit != null && getTableRow() != null) {
                S rowItem = getTableRow().getItem();
                if (rowItem != null) {
                    onCommit.accept(rowItem, newValue);
                }
            }

        } catch (Exception e) {
            // Show error and revert
            showError("Invalid input: " + e.getMessage());
            cancelEdit();
        }
    }

    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Default String converter (for String type cells)
     */
    @SuppressWarnings("unchecked")
    private StringConverter<T> getDefaultConverter() {
        return (StringConverter<T>) new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object != null ? object : "";
            }

            @Override
            public String fromString(String string) {
                return string != null ? string.trim() : "";
            }
        };
    }

    /**
     * Integer converter utility
     */
    public static class IntegerStringConverter extends StringConverter<Integer> {
        @Override
        public String toString(Integer object) {
            return object != null ? object.toString() : "";
        }

        @Override
        public Integer fromString(String string) {
            if (string == null || string.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Must be a valid integer");
            }
        }
    }

    /**
     * Double converter utility
     */
    public static class DoubleStringConverter extends StringConverter<Double> {
        @Override
        public String toString(Double object) {
            return object != null ? object.toString() : "";
        }

        @Override
        public Double fromString(String string) {
            if (string == null || string.trim().isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(string.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Must be a valid number");
            }
        }
    }

    /**
     * Boolean converter utility
     */
    public static class BooleanStringConverter extends StringConverter<Boolean> {
        @Override
        public String toString(Boolean object) {
            return object != null ? (object ? "Yes" : "No") : "No";
        }

        @Override
        public Boolean fromString(String string) {
            if (string == null || string.trim().isEmpty()) {
                return false;
            }
            String lower = string.trim().toLowerCase();
            return lower.equals("yes") || lower.equals("true") || lower.equals("1");
        }
    }

    /**
     * Email validation converter
     */
    public static class EmailStringConverter extends StringConverter<String> {
        private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        @Override
        public String toString(String object) {
            return object != null ? object : "";
        }

        @Override
        public String fromString(String string) {
            if (string == null || string.trim().isEmpty()) {
                return "";
            }
            String trimmed = string.trim();
            if (!trimmed.matches(EMAIL_REGEX)) {
                throw new IllegalArgumentException("Invalid email format");
            }
            return trimmed;
        }
    }

    /**
     * Factory method for String cells
     */
    public static <S> EditableTableCell<S, String> forStringColumn(BiConsumer<S, String> onCommit) {
        return new EditableTableCell<>(null, onCommit);
    }

    /**
     * Factory method for Integer cells
     */
    public static <S> EditableTableCell<S, Integer> forIntegerColumn(BiConsumer<S, Integer> onCommit) {
        return new EditableTableCell<>(new IntegerStringConverter(), onCommit);
    }

    /**
     * Factory method for Email cells
     */
    public static <S> EditableTableCell<S, String> forEmailColumn(BiConsumer<S, String> onCommit) {
        return new EditableTableCell<>(new EmailStringConverter(), onCommit);
    }
}
