package com.heronix.ui.controller;

import com.heronix.model.domain.Room;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.RoomRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Data Review Controller - Shows incomplete/missing data
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/DataReviewController.java
 * 
 * Displays records that were imported but have missing or invalid data.
 * Allows users to review and edit these records.
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Slf4j
@Component
public class DataReviewController {

    @FXML
    private TableView<Room> incompleteRoomsTable;
    @FXML
    private TableColumn<Room, String> roomNumberColumn;
    @FXML
    private TableColumn<Room, String> buildingColumn;
    @FXML
    private TableColumn<Room, Integer> capacityColumn;
    @FXML
    private TableColumn<Room, String> missingDataColumn;
    @FXML
    private Label issueCountLabel;
    @FXML
    private Button editButton;
    @FXML
    private Button refreshButton;

    private final RoomRepository roomRepository;

    public DataReviewController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @FXML
    public void initialize() {
        log.info("Initializing DataReviewController");
        setupTable();
        loadIncompleteData();
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        if (roomNumberColumn != null) {
            roomNumberColumn.setCellValueFactory(
                    cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRoomNumber()));
        }

        if (buildingColumn != null) {
            buildingColumn.setCellValueFactory(
                    cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getBuilding()));
        }

        if (capacityColumn != null) {
            capacityColumn.setCellValueFactory(
                    cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getCapacity()));
        }

        if (missingDataColumn != null) {
            missingDataColumn.setCellValueFactory(
                    cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMissingData()));
        }
    }

    /**
     * Load all records needing review
     */
    private void loadIncompleteData() {
        try {
            List<Room> incompleteRooms = roomRepository.findByNeedsReviewTrue();

            if (incompleteRoomsTable != null) {
                incompleteRoomsTable.getItems().setAll(incompleteRooms);
            }

            if (issueCountLabel != null) {
                issueCountLabel.setText(incompleteRooms.size() + " records need attention");
            }

            log.info("Loaded {} incomplete records", incompleteRooms.size());

        } catch (Exception e) {
            log.error("Error loading incomplete data", e);
            showError("Error loading data", "Could not load incomplete records: " + e.getMessage());
        }
    }

    /**
     * Handle edit button click
     */
    @FXML
    private void handleEdit() {
        Room selected = incompleteRoomsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openEditDialog(selected);
        } else {
            showWarning("No Selection", "Please select a room to edit.");
        }
    }

    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        loadIncompleteData();
    }

    /**
     * Open edit dialog for a room
     */
    private void openEditDialog(Room room) {
        log.info("Opening edit dialog for room: {}", room.getRoomNumber());

        // Create dialog
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Complete missing information for: " + room.getRoomNumber());

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Room Number
        TextField roomNumberField = new TextField(room.getRoomNumber());
        roomNumberField.setPromptText("Room Number");
        grid.add(new Label("Room Number:"), 0, 0);
        grid.add(roomNumberField, 1, 0);

        // Building
        TextField buildingField = new TextField(room.getBuilding());
        buildingField.setPromptText("Building");
        grid.add(new Label("Building:"), 0, 1);
        grid.add(buildingField, 1, 1);

        // Capacity
        TextField capacityField = new TextField(String.valueOf(room.getCapacity()));
        capacityField.setPromptText("Capacity");
        grid.add(new Label("Capacity:"), 0, 2);
        grid.add(capacityField, 1, 2);

        // Room Type
        ComboBox<RoomType> roomTypeCombo = new ComboBox<>();
        roomTypeCombo.getItems().addAll(RoomType.values());
        roomTypeCombo.setValue(room.getRoomType() != null ? room.getRoomType() : RoomType.CLASSROOM);
        grid.add(new Label("Room Type:"), 0, 3);
        grid.add(roomTypeCombo, 1, 3);

        // Floor
        TextField floorField = new TextField(room.getFloor() != null ? String.valueOf(room.getFloor()) : "");
        floorField.setPromptText("Floor");
        grid.add(new Label("Floor:"), 0, 4);
        grid.add(floorField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Convert result to Room when save button clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    room.setRoomNumber(roomNumberField.getText().trim());
                    room.setBuilding(buildingField.getText().trim());

                    String capacityText = capacityField.getText().trim();
                    if (!capacityText.isEmpty()) {
                        room.setCapacity(Integer.parseInt(capacityText));
                    }

                    room.setRoomType(roomTypeCombo.getValue());

                    String floorText = floorField.getText().trim();
                    if (!floorText.isEmpty()) {
                        room.setFloor(Integer.parseInt(floorText));
                    }

                    // Clear needs review flag
                    room.setNeedsReview(false);
                    room.setMissingData(null);

                    return room;
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Capacity and Floor must be valid numbers.");
                    return null;
                }
            }
            return null;
        });

        // Show dialog and process result
        Optional<Room> result = dialog.showAndWait();

        result.ifPresent(editedRoom -> {
            try {
                roomRepository.save(editedRoom);
                log.info("Room updated successfully: {}", editedRoom.getRoomNumber());
                showInfo("Success", "Room updated successfully!");
                loadIncompleteData(); // Refresh table
            } catch (Exception e) {
                log.error("Error saving room", e);
                showError("Save Failed", "Could not save room: " + e.getMessage());
            }
        });
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}