package com.heronix.ui.util;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Table Selection Helper Utility
 * Provides bulk selection and deletion capabilities for TableView components
 *
 * Location: src/main/java/com/eduscheduler/ui/util/TableSelectionHelper.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
public class TableSelectionHelper {

    /**
     * Enable multi-selection on a table view
     */
    public static <T> void enableMultiSelection(TableView<T> table) {
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        log.debug("Enabled multi-selection for table");
    }

    /**
     * Create a selection toolbar with Select All, Delete Selected, and info label
     *
     * @param table The table view
     * @param onDelete Callback to execute when delete is clicked (receives selected items)
     * @param itemTypeName Name of the items (e.g., "Teachers", "Students")
     * @return HBox containing the toolbar
     */
    public static <T> HBox createSelectionToolbar(
            TableView<T> table,
            Consumer<List<T>> onDelete,
            String itemTypeName) {

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        // Select All checkbox
        CheckBox selectAllCheckbox = new CheckBox("Select All");
        selectAllCheckbox.setOnAction(e -> {
            if (selectAllCheckbox.isSelected()) {
                table.getSelectionModel().selectAll();
            } else {
                table.getSelectionModel().clearSelection();
            }
        });

        // Selection count label
        Label selectionLabel = new Label("0 selected");
        selectionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");

        // Delete button
        Button deleteButton = new Button("Delete Selected");
        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteButton.setDisable(true);

        // Update label and button state when selection changes
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            int selectedCount = table.getSelectionModel().getSelectedItems().size();
            selectionLabel.setText(selectedCount + " selected");
            deleteButton.setDisable(selectedCount == 0);

            // Update select all checkbox
            if (selectedCount == 0) {
                selectAllCheckbox.setSelected(false);
                selectAllCheckbox.setIndeterminate(false);
            } else if (selectedCount == table.getItems().size()) {
                selectAllCheckbox.setSelected(true);
                selectAllCheckbox.setIndeterminate(false);
            } else {
                selectAllCheckbox.setIndeterminate(true);
            }
        });

        // Delete button action
        deleteButton.setOnAction(e -> {
            ObservableList<T> selectedItems = table.getSelectionModel().getSelectedItems();
            if (selectedItems.isEmpty()) {
                return;
            }

            // Show confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Deletion");
            confirmDialog.setHeaderText("Delete " + selectedItems.size() + " " + itemTypeName + "?");
            confirmDialog.setContentText(
                "This action cannot be undone. Are you sure you want to delete " +
                selectedItems.size() + " " + itemTypeName.toLowerCase() + "?"
            );

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Create a copy of selected items (to avoid concurrent modification)
                List<T> itemsToDelete = List.copyOf(selectedItems);

                // Execute delete callback
                try {
                    onDelete.accept(itemsToDelete);
                    log.info("Deleted {} {}", itemsToDelete.size(), itemTypeName);

                    // Show success message
                    showSuccess("Deleted",
                        "Successfully deleted " + itemsToDelete.size() + " " + itemTypeName.toLowerCase());

                } catch (Exception ex) {
                    log.error("Error deleting " + itemTypeName, ex);
                    showError("Delete Failed",
                        "Failed to delete " + itemTypeName + ": " + ex.getMessage());
                }
            }
        });

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Add components to toolbar
        toolbar.getChildren().addAll(
            selectAllCheckbox,
            selectionLabel,
            deleteButton,
            spacer
        );

        // Add keyboard shortcut for delete (Ctrl+D)
        table.setOnKeyPressed(event -> {
            KeyCombination deleteShortcut = new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN);
            if (deleteShortcut.match(event)) {
                if (!deleteButton.isDisabled()) {
                    deleteButton.fire();
                }
            }
        });

        return toolbar;
    }

    /**
     * Get count of selected items
     */
    public static <T> int getSelectionCount(TableView<T> table) {
        return table.getSelectionModel().getSelectedItems().size();
    }

    /**
     * Get selected items as a list
     */
    public static <T> List<T> getSelectedItems(TableView<T> table) {
        return List.copyOf(table.getSelectionModel().getSelectedItems());
    }

    /**
     * Clear selection
     */
    public static <T> void clearSelection(TableView<T> table) {
        table.getSelectionModel().clearSelection();
    }

    /**
     * Show success alert
     */
    private static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error alert
     */
    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
