package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSection;
import com.heronix.service.CourseSectionService;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Controller for Section Management Dialog
 * Displays and manages all sections for a selected course
 */
@Slf4j
@Component
public class SectionManagementDialogController {

    @FXML private Label titleLabel;
    @FXML private Label courseInfoLabel;
    @FXML private Label totalSectionsLabel;
    @FXML private Label totalEnrollmentLabel;
    @FXML private Label availableSeatsLabel;
    @FXML private Button addSectionButton;
    @FXML private TableView<CourseSection> sectionsTable;
    @FXML private TableColumn<CourseSection, String> sectionNumberColumn;
    @FXML private TableColumn<CourseSection, String> teacherColumn;
    @FXML private TableColumn<CourseSection, String> roomColumn;
    @FXML private TableColumn<CourseSection, Integer> periodColumn;
    @FXML private TableColumn<CourseSection, String> enrollmentColumn;
    @FXML private TableColumn<CourseSection, Integer> capacityColumn;
    @FXML private TableColumn<CourseSection, String> statusColumn;
    @FXML private TableColumn<CourseSection, Void> actionsColumn;

    @Autowired
    private CourseSectionService courseSectionService;

    @Autowired
    private ApplicationContext applicationContext;

    private Stage dialogStage;
    private Course course;
    private ObservableList<CourseSection> sectionsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        log.debug("SectionManagementDialogController initialized");
        setupTableColumns();
    }

    private void setupTableColumns() {
        // Section Number
        sectionNumberColumn.setCellValueFactory(new PropertyValueFactory<>("sectionNumber"));

        // Teacher
        teacherColumn.setCellValueFactory(cellData -> {
            CourseSection section = cellData.getValue();
            String teacherName = section.getAssignedTeacher() != null ?
                    section.getAssignedTeacher().getName() : "--";
            return new javafx.beans.property.SimpleStringProperty(teacherName);
        });

        // Room
        roomColumn.setCellValueFactory(cellData -> {
            CourseSection section = cellData.getValue();
            String roomNumber = section.getAssignedRoom() != null ?
                    section.getAssignedRoom().getRoomNumber() : "--";
            return new javafx.beans.property.SimpleStringProperty(roomNumber);
        });

        // Period
        periodColumn.setCellValueFactory(cellData -> {
            CourseSection section = cellData.getValue();
            Integer period = section.getAssignedPeriod();
            return new javafx.beans.property.SimpleObjectProperty<>(period);
        });
        periodColumn.setCellFactory(col -> new TableCell<CourseSection, Integer>() {
            @Override
            protected void updateItem(Integer period, boolean empty) {
                super.updateItem(period, empty);
                if (empty || period == null) {
                    setText("--");
                } else {
                    setText(String.valueOf(period));
                }
            }
        });

        // Enrollment
        enrollmentColumn.setCellValueFactory(cellData -> {
            CourseSection section = cellData.getValue();
            int current = section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0;
            int max = section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 0;
            return new javafx.beans.property.SimpleStringProperty(current + "/" + max);
        });

        // Capacity
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("maxEnrollment"));

        // Status
        statusColumn.setCellValueFactory(cellData -> {
            CourseSection section = cellData.getValue();
            String status = section.getSectionStatus() != null ?
                    section.getSectionStatus().toString() : "UNKNOWN";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        statusColumn.setCellFactory(col -> new TableCell<CourseSection, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    // Color code by status
                    switch (status) {
                        case "OPEN":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "FULL":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        case "SCHEDULED":
                            setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                            break;
                        case "CANCELLED":
                            setStyle("-fx-text-fill: #95a5a6; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // Actions Column
        actionsColumn.setCellFactory(col -> new TableCell<CourseSection, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8;");
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8;");

                editButton.setOnAction(e -> {
                    CourseSection section = getTableView().getItems().get(getIndex());
                    handleEditSection(section);
                });

                deleteButton.setOnAction(e -> {
                    CourseSection section = getTableView().getItems().get(getIndex());
                    handleDeleteSection(section);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });

        // Set table items
        sectionsTable.setItems(sectionsList);
    }

    /**
     * Set the course for which to manage sections
     */
    public void setCourse(Course course) {
        this.course = course;
        updateCourseInfo();
        loadSections();
    }

    private void updateCourseInfo() {
        if (course == null) return;

        String courseInfo = String.format("%s - %s",
                course.getCourseCode() != null ? course.getCourseCode() : "???",
                course.getCourseName() != null ? course.getCourseName() : "Unknown Course");

        courseInfoLabel.setText("Course: " + courseInfo);
    }

    private void loadSections() {
        if (course == null) return;

        try {
            List<CourseSection> sections = courseSectionService.getSectionsByCourseId(course.getId());
            sectionsList.clear();
            sectionsList.addAll(sections);
            updateSummary();
            log.info("Loaded {} sections for course: {}", sections.size(), course.getCourseCode());
        } catch (Exception e) {
            log.error("Error loading sections", e);
            showError("Error", "Failed to load sections: " + e.getMessage());
        }
    }

    private void updateSummary() {
        int totalSections = sectionsList.size();
        int totalEnrolled = sectionsList.stream()
                .mapToInt(s -> s.getCurrentEnrollment() != null ? s.getCurrentEnrollment() : 0)
                .sum();
        int totalCapacity = sectionsList.stream()
                .mapToInt(s -> s.getMaxEnrollment() != null ? s.getMaxEnrollment() : 0)
                .sum();
        int availableSeats = totalCapacity - totalEnrolled;

        totalSectionsLabel.setText(String.valueOf(totalSections));
        totalEnrollmentLabel.setText(totalEnrolled + "/" + totalCapacity);
        availableSeatsLabel.setText(String.valueOf(availableSeats));
    }

    @FXML
    private void handleAddSection() {
        showSectionEditDialog(null);
    }

    private void handleEditSection(CourseSection section) {
        showSectionEditDialog(section);
    }

    private void handleDeleteSection(CourseSection section) {
        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Section");
        confirm.setHeaderText("Delete Section " + section.getSectionNumber() + "?");

        int enrollment = section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0;
        if (enrollment > 0) {
            confirm.setContentText("This section has " + enrollment + " students enrolled. " +
                    "You must remove all students before deleting the section.");
            confirm.getButtonTypes().setAll(ButtonType.OK);
            confirm.showAndWait();
            return;
        }

        confirm.setContentText("This will permanently delete section " + section.getSectionNumber() + ".");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                courseSectionService.deleteSection(section.getId());
                sectionsList.remove(section);
                updateSummary();
                log.info("Section deleted: {}", section.getSectionNumber());
                showInfo("Success", "Section deleted successfully");
            } catch (Exception e) {
                log.error("Error deleting section", e);
                showError("Error", "Failed to delete section: " + e.getMessage());
            }
        }
    }

    private void showSectionEditDialog(CourseSection section) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SectionEditDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            SectionEditDialogController controller = loader.getController();

            // Set up the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle(section == null ? "Add Section" : "Edit Section");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.dialogStage);
            dialogStage.setScene(new Scene(root));

            // Set course and section
            controller.setDialogStage(dialogStage);
            controller.setCourse(course);

            if (section != null) {
                controller.setSection(section);
            }

            // Show and wait
            dialogStage.showAndWait();

            // Refresh if saved
            if (controller.isSaved()) {
                loadSections();
            }

        } catch (Exception e) {
            log.error("Error opening section edit dialog", e);
            showError("Error", "Failed to open section dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> CopyableErrorDialog.showError(title, message));
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> CopyableErrorDialog.showInfo(title, message));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
}
