package com.heronix.ui.dialog;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import com.heronix.service.ScheduleViolationAnalyzer;
import com.heronix.service.ScheduleViolationAnalyzer.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Interactive Dialog for Fixing Schedule Generation Violations
 *
 * Displays detailed analysis of what's preventing schedule generation
 * and provides interactive options to fix each issue.
 *
 * Features:
 * - Categorized violation list
 * - One-click fixes for common issues
 * - Teacher assignment suggestions
 * - Room sharing configuration
 * - Progress tracking
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8D - November 21, 2025
 */
@Slf4j
@Component
@Scope("prototype")
public class FixViolationsDialog {

    @Autowired
    private ScheduleViolationAnalyzer violationAnalyzer;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    private AnalysisResult analysisResult;
    private TableView<Violation> violationsTable;
    private VBox detailsPane;
    private Label statusLabel;
    private ProgressBar progressBar;
    private int fixedCount = 0;
    private Dialog<Boolean> dialog; // Store dialog reference for other methods

    /**
     * Initialize and show the dialog
     */
    public void showDialog(Window owner) {
        // Create dialog on FX thread
        this.dialog = new Dialog<>();
        dialog.setTitle("Fix Schedule Violations");
        dialog.initOwner(owner);

        // Analyze violations
        this.analysisResult = violationAnalyzer.analyzePreSchedule();

        // Build dialog content
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(buildContent());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefWidth(900);
        dialogPane.setPrefHeight(650);

        // Style OK button
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Continue Scheduling");
        okButton.setDisable(analysisResult.getCriticalCount() > 0);

        // Update OK button state based on remaining critical issues
        updateOkButtonState(okButton);

        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK);

        dialog.showAndWait();
    }

    private VBox buildContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Header with summary
        content.getChildren().add(buildHeader());

        // Main split: violations list and details
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);

        // Left: Violations table
        VBox leftPane = buildViolationsTable();

        // Right: Details and actions
        detailsPane = buildDetailsPane();

        splitPane.getItems().addAll(leftPane, detailsPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        content.getChildren().add(splitPane);

        // Footer with status
        content.getChildren().add(buildFooter());

        return content;
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);

        // Title
        Label title = new Label("Schedule Generation Issues Detected");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Summary bar
        HBox summaryBar = new HBox(20);
        summaryBar.setAlignment(Pos.CENTER_LEFT);

        Label criticalLabel = createCountLabel(
            analysisResult.getCriticalCount(), "Critical", Color.RED);
        Label warningLabel = createCountLabel(
            analysisResult.getWarningCount(), "Warnings", Color.ORANGE);
        Label infoLabel = createCountLabel(
            analysisResult.getInfoCount(), "Info", Color.BLUE);

        summaryBar.getChildren().addAll(criticalLabel, warningLabel, infoLabel);

        // Instructions
        Label instructions = new Label(
            "Select an issue to see details and available fixes. Critical issues must be resolved before scheduling.");
        instructions.setWrapText(true);
        instructions.setStyle("-fx-text-fill: gray;");

        header.getChildren().addAll(title, summaryBar, instructions);
        return header;
    }

    private Label createCountLabel(int count, String type, Color color) {
        Label label = new Label(count + " " + type);
        label.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15;",
            toRgbString(color)));
        return label;
    }

    private String toRgbString(Color color) {
        return String.format("rgb(%d, %d, %d)",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    @SuppressWarnings("unchecked")
    private VBox buildViolationsTable() {
        VBox pane = new VBox(10);

        Label label = new Label("Issues Found");
        label.setFont(Font.font("System", FontWeight.BOLD, 14));

        violationsTable = new TableView<>();
        violationsTable.setItems(FXCollections.observableArrayList(analysisResult.getViolations()));

        // Severity column
        TableColumn<Violation, String> severityCol = new TableColumn<>("Sev");
        severityCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getSeverity() == 3 ? "🔴" :
            cd.getValue().getSeverity() == 2 ? "🟡" : "🔵"));
        severityCol.setPrefWidth(40);

        // Type column
        TableColumn<Violation, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getType().name().replace("_", " ")));
        typeCol.setPrefWidth(120);

        // Entity column
        TableColumn<Violation, String> entityCol = new TableColumn<>("Course/Entity");
        entityCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEntityName()));
        entityCol.setPrefWidth(150);

        violationsTable.getColumns().addAll(severityCol, typeCol, entityCol);
        VBox.setVgrow(violationsTable, Priority.ALWAYS);

        // Selection listener
        violationsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showViolationDetails(newVal);
                }
            });

        pane.getChildren().addAll(label, violationsTable);
        return pane;
    }

    private VBox buildDetailsPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        Label label = new Label("Details & Actions");
        label.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label placeholder = new Label("Select an issue to see details and available fixes.");
        placeholder.setStyle("-fx-text-fill: gray;");
        placeholder.setWrapText(true);

        pane.getChildren().addAll(label, placeholder);
        return pane;
    }

    private void showViolationDetails(Violation violation) {
        detailsPane.getChildren().clear();

        // Title
        Label title = new Label(violation.getType().getDescription());
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Description
        TextArea description = new TextArea(violation.getDescription());
        description.setWrapText(true);
        description.setEditable(false);
        description.setPrefRowCount(3);
        description.setStyle("-fx-control-inner-background: #f5f5f5;");

        // Suggested fix
        Label fixLabel = new Label("Suggested Fix:");
        fixLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));

        Label fixText = new Label(violation.getSuggestedFix());
        fixText.setWrapText(true);
        fixText.setStyle("-fx-text-fill: green;");

        detailsPane.getChildren().addAll(title, description, fixLabel, fixText);

        // Available actions
        if (violation.getActions() != null && !violation.getActions().isEmpty()) {
            Label actionsLabel = new Label("Available Actions:");
            actionsLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            actionsLabel.setPadding(new Insets(10, 0, 5, 0));

            VBox actionsBox = new VBox(5);

            for (SuggestedAction action : violation.getActions()) {
                Button actionBtn = new Button(action.getLabel());
                actionBtn.setMaxWidth(Double.MAX_VALUE);
                actionBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

                actionBtn.setOnAction(e -> executeAction(action, violation));

                actionsBox.getChildren().add(actionBtn);

                // Limit to 5 actions to avoid clutter
                if (actionsBox.getChildren().size() >= 5) {
                    Label moreLabel = new Label("... and " +
                        (violation.getActions().size() - 5) + " more options");
                    moreLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    actionsBox.getChildren().add(moreLabel);
                    break;
                }
            }

            detailsPane.getChildren().addAll(actionsLabel, actionsBox);
        } else {
            Label noActions = new Label("No automatic fixes available. Manual intervention required.");
            noActions.setStyle("-fx-text-fill: red;");
            detailsPane.getChildren().add(noActions);
        }
    }

    private void executeAction(SuggestedAction action, Violation violation) {
        log.info("Executing action: {} for {}", action.getActionType(), violation.getEntityName());

        progressBar.setVisible(true);
        statusLabel.setText("Applying fix...");

        CompletableFuture.runAsync(() -> {
            try {
                boolean success = false;

                switch (action.getActionType()) {
                    case "ASSIGN_TEACHER":
                        success = assignTeacherToCourse(action);
                        break;
                    case "ENABLE_SHARING":
                        success = enableRoomSharing(action);
                        break;
                    case "ASSIGN_ROOM":
                        success = assignRoomToCourse(action);
                        break;
                    case "REASSIGN_COURSE":
                        success = reassignCourse(action);
                        break;
                    default:
                        log.warn("Unknown action type: {}", action.getActionType());
                }

                final boolean finalSuccess = success;
                Platform.runLater(() -> {
                    if (finalSuccess) {
                        fixedCount++;
                        statusLabel.setText("Fix applied successfully! " + fixedCount + " issue(s) fixed.");

                        // Remove from table
                        violationsTable.getItems().remove(violation);

                        // Update analysis
                        refreshAnalysis();
                    } else {
                        statusLabel.setText("Failed to apply fix. See logs for details.");
                    }
                    progressBar.setVisible(false);
                });

            } catch (Exception e) {
                log.error("Error executing action", e);
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        });
    }

    private boolean assignTeacherToCourse(SuggestedAction action) {
        Long courseId = ((Number) action.getParameters().get("courseId")).longValue();
        Long teacherId = ((Number) action.getParameters().get("teacherId")).longValue();

        Optional<Course> courseOpt = courseRepository.findById(courseId);
        Optional<Teacher> teacherOpt = teacherRepository.findById(teacherId);

        if (courseOpt.isPresent() && teacherOpt.isPresent()) {
            Course course = courseOpt.get();
            Teacher teacher = teacherOpt.get();

            course.setTeacher(teacher);
            if (teacher.getCourses() == null) {
                teacher.setCourses(new ArrayList<>());
            }
            teacher.getCourses().add(course);

            courseRepository.save(course);
            teacherRepository.save(teacher);

            log.info("Assigned teacher {} to course {}", teacher.getName(), course.getCourseName());
            return true;
        }

        return false;
    }

    private boolean enableRoomSharing(SuggestedAction action) {
        Long roomId = ((Number) action.getParameters().get("roomId")).longValue();
        int maxConcurrent = ((Number) action.getParameters().get("maxConcurrent")).intValue();

        Optional<Room> roomOpt = roomRepository.findById(roomId);

        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.setAllowSharing(true);
            room.setMaxConcurrentClasses(maxConcurrent);
            roomRepository.save(room);

            log.info("Enabled room sharing for {} with max {} concurrent classes",
                room.getRoomNumber(), maxConcurrent);
            return true;
        }

        return false;
    }

    private boolean assignRoomToCourse(SuggestedAction action) {
        // Room assignment would be handled differently in scheduling
        // For now, just mark as acknowledged
        log.info("Room assignment noted: {}", action.getParameters());
        return true;
    }

    private boolean reassignCourse(SuggestedAction action) {
        Long courseId = ((Number) action.getParameters().get("courseId")).longValue();
        Long fromTeacherId = ((Number) action.getParameters().get("fromTeacherId")).longValue();
        Long toTeacherId = ((Number) action.getParameters().get("toTeacherId")).longValue();

        Optional<Course> courseOpt = courseRepository.findById(courseId);
        Optional<Teacher> fromTeacherOpt = teacherRepository.findById(fromTeacherId);
        Optional<Teacher> toTeacherOpt = teacherRepository.findById(toTeacherId);

        if (courseOpt.isPresent() && fromTeacherOpt.isPresent() && toTeacherOpt.isPresent()) {
            Course course = courseOpt.get();
            Teacher fromTeacher = fromTeacherOpt.get();
            Teacher toTeacher = toTeacherOpt.get();

            // Remove from old teacher
            if (fromTeacher.getCourses() != null) {
                fromTeacher.getCourses().remove(course);
            }

            // Add to new teacher
            if (toTeacher.getCourses() == null) {
                toTeacher.setCourses(new ArrayList<>());
            }
            toTeacher.getCourses().add(course);
            course.setTeacher(toTeacher);

            courseRepository.save(course);
            teacherRepository.save(fromTeacher);
            teacherRepository.save(toTeacher);

            log.info("Reassigned course {} from {} to {}",
                course.getCourseName(), fromTeacher.getName(), toTeacher.getName());
            return true;
        }

        return false;
    }

    private void refreshAnalysis() {
        // Re-analyze and update counts
        this.analysisResult = violationAnalyzer.analyzePreSchedule();

        Platform.runLater(() -> {
            violationsTable.setItems(FXCollections.observableArrayList(analysisResult.getViolations()));
            updateOkButtonState((Button) dialog.getDialogPane().lookupButton(ButtonType.OK));
        });
    }

    private void updateOkButtonState(Button okButton) {
        if (okButton != null) {
            boolean hasCritical = analysisResult.getCriticalCount() > 0;
            okButton.setDisable(hasCritical);

            if (hasCritical) {
                okButton.setText("Fix Critical Issues First");
            } else {
                okButton.setText("Continue Scheduling (" + fixedCount + " fixed)");
            }
        }
    }

    private HBox buildFooter() {
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(150);

        statusLabel = new Label("Select an issue to see available fixes.");
        statusLabel.setStyle("-fx-text-fill: gray;");

        footer.getChildren().addAll(progressBar, statusLabel);
        return footer;
    }
}
