package com.heronix.ui.controller;

import com.heronix.model.enums.ScheduleStatus;
import com.heronix.service.DatabaseCleanupService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;

/**
 * Database Cleanup Dialog Controller
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/DatabaseCleanupController.java
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DatabaseCleanupController {

    private final DatabaseCleanupService cleanupService;

    @FXML
    private DatePicker purgeBeforeDatePicker;
    @FXML
    private CheckBox archiveInsteadCheckBox;
    @FXML
    private CheckBox purgeDraftsCheckBox;
    @FXML
    private CheckBox purgeArchivedCheckBox;
    @FXML
    private CheckBox cleanOrphanedCheckBox;
    @FXML
    private TextArea previewTextArea;
    @FXML
    private Button executeButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressIndicator progressIndicator;

    private Stage dialogStage;

    @FXML
    private void initialize() {
        // Set default date to 1 year ago
        purgeBeforeDatePicker.setValue(LocalDate.now().minusYears(1));

        // Initially hide progress indicator
        progressIndicator.setVisible(false);

        // Update preview when selections change
        purgeBeforeDatePicker.valueProperty().addListener((obs, old, newVal) -> updatePreview());
        archiveInsteadCheckBox.selectedProperty().addListener((obs, old, newVal) -> updatePreview());
        purgeDraftsCheckBox.selectedProperty().addListener((obs, old, newVal) -> updatePreview());
        purgeArchivedCheckBox.selectedProperty().addListener((obs, old, newVal) -> updatePreview());
        cleanOrphanedCheckBox.selectedProperty().addListener((obs, old, newVal) -> updatePreview());

        updatePreview();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void updatePreview() {
        StringBuilder preview = new StringBuilder();
        preview.append("=== Cleanup Operations ===\n\n");

        if (purgeBeforeDatePicker.getValue() != null) {
            if (archiveInsteadCheckBox.isSelected()) {
                preview.append("✓ Archive schedules before ")
                        .append(purgeBeforeDatePicker.getValue())
                        .append("\n");
            } else {
                preview.append("✓ DELETE schedules before ")
                        .append(purgeBeforeDatePicker.getValue())
                        .append("\n");
            }
        }

        if (purgeDraftsCheckBox.isSelected()) {
            preview.append("✓ DELETE all DRAFT schedules\n");
        }

        if (purgeArchivedCheckBox.isSelected()) {
            preview.append("✓ DELETE all ARCHIVED schedules\n");
        }

        if (cleanOrphanedCheckBox.isSelected()) {
            preview.append("✓ Clean orphaned schedule slots\n");
        }

        preview.append("\n⚠️ Warning: Deletions are permanent and cannot be undone!");

        previewTextArea.setText(preview.toString());
    }

    @FXML
    private void handleExecute() {
        // Confirm action
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Cleanup");
        confirmAlert.setHeaderText("Execute Database Cleanup?");
        confirmAlert.setContentText("This operation cannot be undone. Are you sure?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        executeButton.setDisable(true);
        progressIndicator.setVisible(true);

        // Run cleanup in background thread
        new Thread(() -> {
            try {
                int totalDeleted = 0;
                int totalArchived = 0;

                LocalDate beforeDate = purgeBeforeDatePicker.getValue();

                if (beforeDate != null) {
                    if (archiveInsteadCheckBox.isSelected()) {
                        totalArchived = cleanupService.archiveSchedulesBefore(beforeDate);
                    } else {
                        totalDeleted += cleanupService.purgeSchedulesBefore(beforeDate);
                    }
                }

                if (purgeDraftsCheckBox.isSelected()) {
                    totalDeleted += cleanupService.purgeSchedulesByStatus(ScheduleStatus.DRAFT);
                }

                if (purgeArchivedCheckBox.isSelected()) {
                    totalDeleted += cleanupService.purgeSchedulesByStatus(ScheduleStatus.ARCHIVED);
                }

                if (cleanOrphanedCheckBox.isSelected()) {
                    totalDeleted += cleanupService.cleanupOrphanedSlots();
                }

                int finalDeleted = totalDeleted;
                int finalArchived = totalArchived;

                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    executeButton.setDisable(false);

                    Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
                    resultAlert.setTitle("Cleanup Complete");
                    resultAlert.setHeaderText("Database Cleanup Successful");
                    resultAlert.setContentText(
                            "Deleted: " + finalDeleted + " items\n" +
                                    "Archived: " + finalArchived + " schedules");
                    resultAlert.showAndWait();

                    dialogStage.close();
                });

            } catch (Exception e) {
                log.error("Error during cleanup", e);
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    executeButton.setDisable(false);

                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Cleanup Failed");
                    errorAlert.setHeaderText("Error During Cleanup");
                    errorAlert.setContentText(e.getMessage());
                    errorAlert.showAndWait();
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}