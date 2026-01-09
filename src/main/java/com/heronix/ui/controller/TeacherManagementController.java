// ============================================================================
// FILE: TeacherManagementController.java - COMPLETE WITH ALL METHODS
// LOCATION: src/main/java/com/eduscheduler/ui/controller/TeacherManagementController.java
// ============================================================================

package com.heronix.ui.controller;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TeacherManagementController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> departmentFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private TableView<Teacher> teacherTable;
    @FXML
    private Label recordCountLabel;
    @FXML
    private HBox selectionToolbar;
    @FXML
    private TableColumn<Teacher, String> workloadColumn;
    @FXML
    private TableColumn<Teacher, Long> idColumn;
    @FXML
    private TableColumn<Teacher, String> nameColumn;
    @FXML
    private TableColumn<Teacher, String> employeeIdColumn;
    @FXML
    private TableColumn<Teacher, String> departmentColumn;
    @FXML
    private TableColumn<Teacher, String> emailColumn;
    @FXML
    private TableColumn<Teacher, String> phoneColumn;
    @FXML
    private TableColumn<Teacher, Integer> courseCountColumn;
    @FXML
    private TableColumn<Teacher, String> certificationsColumn;
    @FXML
    private TableColumn<Teacher, String> assignedCoursesColumn;
    @FXML
    private TableColumn<Teacher, Integer> maxHoursColumn;
    @FXML
    private TableColumn<Teacher, Boolean> activeColumn;
    @FXML
    private TableColumn<Teacher, Void> actionsColumn;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private com.heronix.service.TeacherService teacherService;

    @Autowired
    private com.heronix.service.ExportService exportService;

    @Autowired
    private com.heronix.service.IntelligentTeacherAssignmentService intelligentAssignmentService;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupWorkloadColumn();
        setupFilters();
        setupActionsColumn();
        setupBulkSelection();

        // Delay loading teachers until after JavaFX initialization completes
        Platform.runLater(() -> {
            try {
                loadTeachers();
            } catch (Exception e) {
                log.error("Error during initial teacher load", e);
            }
        });
    }

    private void setupBulkSelection() {
        // Enable multi-selection
        com.heronix.ui.util.TableSelectionHelper.enableMultiSelection(teacherTable);

        // Create selection toolbar
        HBox toolbar = com.heronix.ui.util.TableSelectionHelper.createSelectionToolbar(
            teacherTable,
            this::handleBulkDelete,
            "Teachers"
        );

        // Replace the placeholder with the actual toolbar
        if (selectionToolbar != null) {
            selectionToolbar.getChildren().setAll(toolbar.getChildren());
            selectionToolbar.setPadding(toolbar.getPadding());
            selectionToolbar.setSpacing(toolbar.getSpacing());
            selectionToolbar.setStyle(toolbar.getStyle());
        }
    }

    private void handleBulkDelete(List<Teacher> teachers) {
        try {
            for (Teacher teacher : teachers) {
                teacherRepository.delete(teacher);
                log.info("Deleted teacher: {} (ID: {})", teacher.getName(), teacher.getId());
            }

            // Reload the table
            loadTeachers();

            log.info("Bulk delete completed: {} teachers deleted", teachers.size());
        } catch (Exception e) {
            log.error("Error during bulk delete", e);
            throw e; // Let TableSelectionHelper show the error dialog
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        employeeIdColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        // Department - EDITABLE TextField for quick updates
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        departmentColumn.setCellFactory(col -> new TableCell<Teacher, String>() {
            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> {
                    Teacher teacher = getTableRow().getItem();
                    if (teacher != null) {
                        teacher.setDepartment(textField.getText());
                        teacherRepository.save(teacher);
                        commitEdit(textField.getText());
                        log.info("Updated department for teacher {} to '{}'", teacher.getName(), textField.getText());
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item != null ? item : "");
                    textField.setPromptText("Department");
                    setGraphic(textField);
                }
            }
        });

        // Email - EDITABLE TextField for quick updates
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailColumn.setCellFactory(col -> new TableCell<Teacher, String>() {
            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> {
                    Teacher teacher = getTableRow().getItem();
                    if (teacher != null) {
                        teacher.setEmail(textField.getText());
                        teacherRepository.save(teacher);
                        commitEdit(textField.getText());
                        log.info("Updated email for teacher {} to '{}'", teacher.getName(), textField.getText());
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item != null ? item : "");
                    textField.setPromptText("email@school.edu");
                    setGraphic(textField);
                }
            }
        });

        // Phone - EDITABLE TextField for quick updates
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        phoneColumn.setCellFactory(col -> new TableCell<Teacher, String>() {
            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> {
                    Teacher teacher = getTableRow().getItem();
                    if (teacher != null) {
                        teacher.setPhoneNumber(textField.getText());
                        teacherRepository.save(teacher);
                        commitEdit(textField.getText());
                        log.info("Updated phone for teacher {} to '{}'", teacher.getName(), textField.getText());
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item != null ? item : "");
                    textField.setPromptText("555-1234");
                    textField.setStyle("-fx-max-width: 120px;");
                    setGraphic(textField);
                }
            }
        });

        // Certifications column - show certified subjects (handle lazy loading gracefully)
        certificationsColumn.setCellValueFactory(cellData -> {
            Teacher teacher = cellData.getValue();
            try {
                String display = teacher.getCertificationsDisplay();
                return new javafx.beans.property.SimpleStringProperty(display);
            } catch (org.hibernate.LazyInitializationException e) {
                return new javafx.beans.property.SimpleStringProperty("--");
            }
        });
        certificationsColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        // Course Count column - numeric display
        courseCountColumn.setCellValueFactory(new PropertyValueFactory<>("courseCount"));
        courseCountColumn.setStyle("-fx-alignment: CENTER;");

        // Assigned courses column - show count (handle lazy loading gracefully)
        assignedCoursesColumn.setCellValueFactory(cellData -> {
            Teacher teacher = cellData.getValue();
            try {
                int count = (teacher.getCourses() != null) ? teacher.getCourses().size() : 0;
                if (count == 0) {
                    return new javafx.beans.property.SimpleStringProperty("None");
                }
                return new javafx.beans.property.SimpleStringProperty(count + " course" + (count > 1 ? "s" : ""));
            } catch (org.hibernate.LazyInitializationException e) {
                return new javafx.beans.property.SimpleStringProperty("--");
            }
        });
        assignedCoursesColumn.setStyle("-fx-alignment: CENTER;");

        // Max Hours - EDITABLE TextField for quick updates
        maxHoursColumn.setCellValueFactory(new PropertyValueFactory<>("maxHoursPerWeek"));
        maxHoursColumn.setCellFactory(col -> new TableCell<Teacher, Integer>() {
            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> {
                    try {
                        int hours = Integer.parseInt(textField.getText());
                        Teacher teacher = getTableRow().getItem();
                        if (teacher != null) {
                            teacher.setMaxHoursPerWeek(hours);
                            teacherRepository.save(teacher);
                            commitEdit(hours);
                            log.info("Updated max hours for teacher {} to {}", teacher.getName(), hours);
                        }
                    } catch (NumberFormatException ex) {
                        textField.setText(String.valueOf(getItem()));
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item != null ? String.valueOf(item) : "40");
                    textField.setStyle("-fx-max-width: 50px;");
                    setGraphic(textField);
                }
            }
        });

        // Active - EDITABLE ComboBox for quick status updates
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeColumn.setCellFactory(col -> new TableCell<Teacher, Boolean>() {
            private final ComboBox<String> comboBox = new ComboBox<>();

            {
                comboBox.getItems().addAll("✓ Active", "✗ Inactive");
                comboBox.setOnAction(e -> {
                    Teacher teacher = getTableRow().getItem();
                    if (teacher != null) {
                        boolean isActive = comboBox.getValue().startsWith("✓");
                        teacher.setActive(isActive);
                        teacherRepository.save(teacher);
                        commitEdit(isActive);
                        log.info("Updated active status for teacher {} to {}", teacher.getName(), isActive);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item != null && item ? "✓ Active" : "✗ Inactive");
                    comboBox.setStyle("-fx-max-width: 100px;");
                    setGraphic(comboBox);
                }
            }
        });
    }

    private void setupFilters() {
        List<String> departments = teacherRepository.findAllActive().stream()
                .map(Teacher::getDepartment)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        departmentFilter.setItems(FXCollections.observableArrayList("All Departments"));
        departmentFilter.getItems().addAll(departments);
        departmentFilter.setValue("All Departments");

        statusFilter.setItems(FXCollections.observableArrayList("All", "Active", "Inactive"));
        statusFilter.setValue("Active");  // Default to showing only active teachers
    }

    private void setupWorkloadColumn() {
        // Workload indicator column
        workloadColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getWorkloadIndicator()));

        // Add custom cell factory for colored background and tooltips
        workloadColumn.setCellFactory(column -> new TableCell<Teacher, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(item);

                    // Set style with proper alignment and background
                    Teacher teacher = getTableRow().getItem();
                    if (teacher != null) {
                        String style = "-fx-alignment: CENTER; -fx-font-size: 20px; -fx-padding: 5px;";
                        String bgColor = switch (item) {
                            case "🟢" -> "-fx-background-color: rgba(76, 175, 80, 0.3);";   // Green tint (light load)
                            case "🟡" -> "-fx-background-color: rgba(255, 193, 7, 0.3);";   // Yellow tint (normal)
                            case "🔴" -> "-fx-background-color: rgba(244, 67, 54, 0.3);";   // Red tint (heavy/none)
                            default -> "";
                        };
                        setStyle(style + bgColor);

                        // Add tooltip with detailed workload information
                        int courseCount = teacher.getCourseCount();

                        String tooltipText;
                        if (courseCount == 0) {
                            tooltipText = String.format("Underutilized\n✗ No courses assigned\n\nTeacher: %s\nStatus: Available for assignment",
                                teacher.getName());
                        } else if (courseCount <= 3) {
                            tooltipText = String.format("Light Workload\n✓ %d course%s assigned\n\nTeacher: %s\nStatus: Can take more courses",
                                courseCount, courseCount == 1 ? "" : "s", teacher.getName());
                        } else if (courseCount <= 5) {
                            tooltipText = String.format("Normal Workload\n✓ %d courses assigned\n\nTeacher: %s\nStatus: Healthy workload",
                                courseCount, teacher.getName());
                        } else {
                            tooltipText = String.format("Overloaded\n⚠ %d courses assigned\n\nTeacher: %s\nStatus: Consider redistributing courses",
                                courseCount, teacher.getName());
                        }

                        Tooltip tooltip = new Tooltip(tooltipText);
                        tooltip.setShowDelay(javafx.util.Duration.millis(300));
                        setTooltip(tooltip);
                    } else {
                        setStyle("-fx-alignment: CENTER; -fx-font-size: 20px; -fx-padding: 5px;");
                        setTooltip(null);
                    }
                }
            }
        });
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Teacher, Void>, TableCell<Teacher, Void>> cellFactory = param -> {
            return new TableCell<>() {
                private final Button editBtn = new Button("Edit");
                private final Button availabilityBtn = new Button("Availability");
                private final Button roomPrefsBtn = new Button("Room Prefs");
                private final Button deleteBtn = new Button("Delete");
                private final HBox pane = new HBox(5, editBtn, availabilityBtn, roomPrefsBtn, deleteBtn);

                {
                    pane.setAlignment(Pos.CENTER);
                    editBtn.setOnAction(e -> {
                        Teacher teacher = getTableView().getItems().get(getIndex());
                        handleEdit(teacher);
                    });
                    availabilityBtn.setOnAction(e -> {
                        Teacher teacher = getTableView().getItems().get(getIndex());
                        handleAvailability(teacher);
                    });
                    roomPrefsBtn.setOnAction(e -> {
                        Teacher teacher = getTableView().getItems().get(getIndex());
                        handleRoomPreferences(teacher);
                    });
                    deleteBtn.setOnAction(e -> {
                        Teacher teacher = getTableView().getItems().get(getIndex());
                        handleDelete(teacher);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : pane);
                }
            };
        };
        actionsColumn.setCellFactory(cellFactory);
    }

    // ========================================================================
    // FXML EVENT HANDLERS - ALL METHODS NEEDED BY FXML
    // ========================================================================

    @FXML
    private void handleSearch() {
        log.info("Search triggered");
        filterTeachers();
    }

    @FXML
    private void handleFilter() {
        log.info("Filter triggered");
        filterTeachers();
    }

    @FXML
    private void handleAddTeacher() {
        log.info("Add teacher clicked");
        try {
            log.debug("Loading TeacherDialog.fxml for new teacher...");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/TeacherDialog.fxml")
            );

            if (loader.getLocation() == null) {
                throw new RuntimeException("TeacherDialog.fxml not found at /fxml/TeacherDialog.fxml");
            }

            loader.setControllerFactory(applicationContext::getBean);

            log.debug("Loading FXML...");
            javafx.scene.Parent root = loader.load();
            TeacherDialogController controller = loader.getController();

            if (controller == null) {
                throw new RuntimeException("TeacherDialogController not initialized by FXML loader");
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add New Teacher");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(teacherTable.getScene().getWindow());

            controller.setDialogStage(stage);

            log.debug("Showing dialog...");
            stage.showAndWait();

            if (controller.isSaved()) {
                loadTeachers();
                log.info("Teacher added successfully");
            }
        } catch (Exception e) {
            log.error("Error opening teacher dialog for new teacher", e);
            showError("Failed to Open Add Teacher Dialog",
                     "An error occurred while trying to open the add teacher dialog.\n\n" +
                     "Error: " + e.getMessage(),
                     e);
        }
    }

    @FXML
    private void handleRefresh() {
        log.info("Refresh clicked");
        loadTeachers();
    }

        @FXML
    private void handleExport() {
        log.info("Export clicked");

        try {
            if (teacherTable.getItems().isEmpty()) {{
                showWarning("No Data", "There are no teachers to export.");
                return;
            }}

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Teachers");
            fileChooser.setInitialFileName("teachers_export.xlsx");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );

            java.io.File file = fileChooser.showSaveDialog(teacherTable.getScene().getWindow());

            if (file != null) {
                byte[] data = exportService.exportTeachersToExcel(teacherTable.getItems());
                java.nio.file.Files.write(file.toPath(), data);

                showInfo("Export Successful",
                    String.format("Exported %d teachers to %s", teacherTable.getItems().size(), file.getName()));
                log.info("Exported {} teachers to {}", teacherTable.getItems().size(), file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("Failed to export teachers", e);
            showError("Export Failed", "Failed to export teachers: " + e.getMessage());
        }
    }

    /**
     * Auto-assign courses and rooms to teachers based on certifications and subjects
     */
    @FXML
    private void handleAutoAssign() {
        log.info("Auto-Assign clicked");

        try {
            // Confirm with user
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Auto-Assign Teachers");
            confirm.setHeaderText("Intelligent Auto-Assignment");
            confirm.setContentText(
                "This will automatically:\n\n" +
                "1. Assign courses to teachers based on their certifications and subject expertise\n" +
                "2. Assign appropriate rooms to teachers based on their department\n" +
                "   (e.g., Science teachers → Science Labs, PE teachers → Gymnasium)\n\n" +
                "This operation is safe and will only assign to unassigned courses/teachers.\n\n" +
                "Continue?"
            );

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            // Show progress dialog
            Alert progress = new Alert(Alert.AlertType.INFORMATION);
            progress.setTitle("Auto-Assignment");
            progress.setHeaderText("Processing...");
            progress.setContentText("Auto-assigning courses and rooms to teachers...");
            progress.show();

            // Perform assignment in background thread
            javafx.concurrent.Task<java.util.Map<String, Object>> task = new javafx.concurrent.Task<>() {
                @Override
                protected java.util.Map<String, Object> call() {
                    return intelligentAssignmentService.performCompleteAutoAssignment();
                }
            };

            task.setOnSucceeded(e -> {
                progress.close();
                java.util.Map<String, Object> results = task.getValue();

                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> courseResults =
                    (java.util.Map<String, Object>) results.get("courseAssignment");
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> roomResults =
                    (java.util.Map<String, Object>) results.get("roomAssignment");

                int coursesAssigned = (int) courseResults.get("totalAssigned");
                int coursesFailed = (int) courseResults.get("totalFailed");
                int roomsAssigned = (int) roomResults.get("totalAssigned");
                int roomsFailed = (int) roomResults.get("totalFailed");

                // Show results
                Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
                resultAlert.setTitle("Auto-Assignment Complete");
                resultAlert.setHeaderText("✅ Assignment Successful!");
                resultAlert.setContentText(String.format(
                    "Course Assignment:\n" +
                    "  ✅ Successfully assigned: %d courses\n" +
                    "  ⚠️  Could not assign: %d courses\n\n" +
                    "Room Assignment:\n" +
                    "  ✅ Successfully assigned: %d rooms\n" +
                    "  ⚠️  Could not assign: %d rooms\n\n" +
                    "Total Operations: %d",
                    coursesAssigned, coursesFailed,
                    roomsAssigned, roomsFailed,
                    coursesAssigned + roomsAssigned
                ));
                resultAlert.showAndWait();

                // Refresh the teacher table
                loadTeachers();
                log.info("Auto-assignment completed: {} courses, {} rooms assigned",
                    coursesAssigned, roomsAssigned);
            });

            task.setOnFailed(e -> {
                progress.close();
                Throwable ex = task.getException();
                log.error("Auto-assignment failed", ex);
                showError("Auto-Assignment Failed",
                    "Failed to auto-assign teachers:\n\n" + ex.getMessage(), ex);
            });

            new Thread(task).start();

        } catch (Exception e) {
            log.error("Error during auto-assignment", e);
            showError("Auto-Assignment Error",
                "An error occurred during auto-assignment:\n\n" + e.getMessage(), e);
        }
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    private void handleEdit(Teacher teacher) {
        log.info("Edit teacher clicked: {}", teacher.getName());
        try {
            // Load teacher with all collections using service method
            // Service layer handles @Transactional to keep transaction management out of JavaFX controller
            // (Adding @Transactional to controller causes Spring CGLIB proxy that breaks @FXML injection)
            Teacher teacherWithCollections = teacherService.loadTeacherWithCollections(teacher.getId());

            log.debug("Loading TeacherDialog.fxml...");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/TeacherDialog.fxml")
            );

            if (loader.getLocation() == null) {
                throw new RuntimeException("TeacherDialog.fxml not found at /fxml/TeacherDialog.fxml");
            }

            loader.setControllerFactory(applicationContext::getBean);

            log.debug("Loading FXML...");
            javafx.scene.Parent root = loader.load();
            TeacherDialogController controller = loader.getController();

            if (controller == null) {
                throw new RuntimeException("TeacherDialogController not initialized by FXML loader");
            }

            // Set the teacher to edit
            log.debug("Setting teacher data...");
            controller.setTeacher(teacherWithCollections);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Teacher - " + teacherWithCollections.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(teacherTable.getScene().getWindow());

            controller.setDialogStage(stage);

            log.debug("Showing dialog...");
            stage.showAndWait();

            if (controller.isSaved()) {
                loadTeachers();
                log.info("Teacher updated successfully: {}", teacher.getName());
            }
        } catch (Exception e) {
            log.error("Error opening teacher edit dialog for teacher: {}", teacher.getName(), e);
            showError("Failed to Open Teacher Edit Dialog",
                     "An error occurred while trying to open the teacher edit dialog.\n\n" +
                     "Teacher: " + teacher.getName() + "\n" +
                     "Error: " + e.getMessage(),
                     e);
        }
    }

    private void handleDelete(Teacher teacher) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Teacher");
        alert.setHeaderText("Are you sure you want to delete this teacher?");
        alert.setContentText(teacher.getName());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                teacher.setActive(false);
                teacherRepository.save(teacher);
                loadTeachers();
                log.info("Teacher deleted: {}", teacher.getName());
            }
        });
    }

    private void handleAvailability(Teacher teacher) {
        log.info("Availability button clicked for teacher: {}", teacher.getName());
        try {
            // Load teacher with all collections
            Teacher teacherWithCollections = teacherService.loadTeacherWithCollections(teacher.getId());

            log.debug("Loading TeacherAvailabilityDialog.fxml...");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/eduscheduler/view/TeacherAvailabilityDialog.fxml")
            );

            if (loader.getLocation() == null) {
                throw new RuntimeException("TeacherAvailabilityDialog.fxml not found at /com/eduscheduler/view/TeacherAvailabilityDialog.fxml");
            }

            loader.setControllerFactory(applicationContext::getBean);

            log.debug("Loading FXML...");
            javafx.scene.Parent root = loader.load();
            com.heronix.controller.TeacherAvailabilityDialogController controller = loader.getController();

            if (controller == null) {
                throw new RuntimeException("TeacherAvailabilityDialogController not initialized by FXML loader");
            }

            // Set the teacher
            log.debug("Setting teacher data...");
            controller.setTeacher(teacherWithCollections);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Teacher Availability - " + teacherWithCollections.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(teacherTable.getScene().getWindow());

            log.debug("Showing availability dialog...");
            stage.showAndWait();

            // Refresh table in case availability affects any display
            loadTeachers();
            log.info("Teacher availability dialog closed for: {}", teacher.getName());

        } catch (Exception e) {
            log.error("Error opening teacher availability dialog for teacher: {}", teacher.getName(), e);
            showError("Failed to Open Availability Dialog",
                     "An error occurred while trying to open the teacher availability dialog.\n\n" +
                     "Teacher: " + teacher.getName() + "\n" +
                     "Error: " + e.getMessage(),
                     e);
        }
    }

    /**
     * Handle Room Preferences button click
     * Phase 6B-4: Room Preferences UI Integration
     */
    private void handleRoomPreferences(Teacher teacher) {
        log.info("Room Preferences button clicked for teacher: {}", teacher.getName());
        try {
            log.debug("Loading TeacherRoomPreferencesDialog.fxml...");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/TeacherRoomPreferencesDialog.fxml")
            );

            if (loader.getLocation() == null) {
                throw new RuntimeException("TeacherRoomPreferencesDialog.fxml not found at /fxml/TeacherRoomPreferencesDialog.fxml");
            }

            loader.setControllerFactory(applicationContext::getBean);

            log.debug("Loading FXML...");
            DialogPane dialogPane = loader.load();
            com.heronix.controller.TeacherRoomPreferencesDialogController controller = loader.getController();

            if (controller == null) {
                throw new RuntimeException("TeacherRoomPreferencesDialogController not initialized by FXML loader");
            }

            // Initialize with teacher
            log.debug("Setting teacher data...");
            controller.initializeWithTeacher(teacher);

            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Room Preferences - " + teacher.getName());
            dialog.setDialogPane(dialogPane);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.initOwner(teacherTable.getScene().getWindow());

            log.debug("Showing room preferences dialog...");
            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    // Validate
                    if (!controller.validate()) {
                        return;
                    }

                    // Get preferences from dialog
                    com.heronix.model.dto.RoomPreferences preferences = controller.getRoomPreferences();

                    // Save to teacher
                    teacher.setRoomPreferences(preferences);
                    teacherRepository.save(teacher);

                    log.info("Room preferences saved for teacher: {}", teacher.getName());

                    // Show success alert
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Room Preferences Saved");
                    alert.setHeaderText(null);
                    alert.setContentText("Room preferences have been successfully saved for " + teacher.getName());
                    alert.showAndWait();

                    // Refresh table
                    loadTeachers();
                }
            });

            log.info("Room preferences dialog closed for: {}", teacher.getName());

        } catch (Exception e) {
            log.error("Error opening room preferences dialog for teacher: {}", teacher.getName(), e);
            showError("Failed to Open Room Preferences Dialog",
                     "An error occurred while trying to open the room preferences dialog.\n\n" +
                     "Teacher: " + teacher.getName() + "\n" +
                     "Error: " + e.getMessage(),
                     e);
        }
    }

    private void loadTeachers() {
        // Apply current filter settings to respect status filter (Active/Inactive/All)
        // This ensures deleted (inactive) teachers are properly filtered based on user's selection
        filterTeachers();
    }

    private void filterTeachers() {
        String searchText = searchField.getText().toLowerCase();
        String department = departmentFilter.getValue();
        String status = statusFilter.getValue();

        try {
            // Call service directly - @Transactional will manage session
            // This runs on JavaFX thread but should be fast enough
            List<Teacher> teachers = teacherService.findAllWithCollectionsForUI();

            List<Teacher> filtered = teachers.stream()
                    .filter(t -> searchText.isEmpty() ||
                            t.getName().toLowerCase().contains(searchText) ||
                            (t.getEmployeeId() != null && t.getEmployeeId().toLowerCase().contains(searchText)))
                    .filter(t -> "All Departments".equals(department) ||
                            department.equals(t.getDepartment()))
                    .filter(t -> "All".equals(status) ||
                            ("Active".equals(status) && Boolean.TRUE.equals(t.getActive())) ||
                            ("Inactive".equals(status) && !Boolean.TRUE.equals(t.getActive())))
                    .collect(Collectors.toList());

            teacherTable.setItems(FXCollections.observableArrayList(filtered));
            recordCountLabel.setText(filtered.size() + " teachers");
            log.info("Loaded {} teachers", filtered.size());
        } catch (Exception e) {
            log.error("Error loading teachers", e);
            showError("Error", "Failed to load teachers: " + e.getMessage());
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        com.heronix.ui.util.CopyableErrorDialog.showError(title, message);
    }

    private void showError(String title, String message, Throwable exception) {
        com.heronix.ui.util.CopyableErrorDialog.showError(title, message, exception);
    }
}