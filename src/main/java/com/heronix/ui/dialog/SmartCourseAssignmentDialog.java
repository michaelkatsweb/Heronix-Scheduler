package com.heronix.ui.dialog;

import com.heronix.dto.CourseAssignmentRecommendation;
import com.heronix.dto.CourseAssignmentRecommendation.RecommendationPriority;
import com.heronix.dto.CourseAssignmentRecommendation.TeacherMatchInfo;
import com.heronix.service.impl.SmartCourseAssignmentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Smart Course Assignment Dialog
 *
 * Displays intelligent course-to-teacher assignment recommendations
 * based on certification matching, workload balancing, and expertise.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Smart Course Assignment
 */
public class SmartCourseAssignmentDialog extends Dialog<Boolean> {

    private final SmartCourseAssignmentService assignmentService;
    private final ObservableList<RecommendationRow> recommendations = FXCollections.observableArrayList();
    private TableView<RecommendationRow> recommendationsTable;
    private Label summaryLabel;

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public SmartCourseAssignmentDialog(Stage owner, SmartCourseAssignmentService assignmentService) {
        this.assignmentService = assignmentService;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Smart Course Assignment");
        setHeaderText("Intelligent Course-to-Teacher Matching");

        // Build UI
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(
            new ButtonType("Apply Selected", ButtonBar.ButtonData.OK_DONE),
            new ButtonType("Apply All", ButtonBar.ButtonData.APPLY),
            ButtonType.CANCEL
        );
        dialogPane.setContent(createContent());

        // Set minimum size
        dialogPane.setMinWidth(1000);
        dialogPane.setMinHeight(700);

        // Load recommendations
        loadRecommendations();

        // Handle button actions
        Button applyAllBtn = (Button) dialogPane.lookupButton(dialogPane.getButtonTypes().get(1));
        applyAllBtn.setOnAction(e -> applyAllRecommendations());
    }

    // ========================================================================
    // UI CREATION
    // ========================================================================

    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Summary section
        summaryLabel = new Label("Loading recommendations...");
        summaryLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        summaryLabel.setTextFill(Color.web("#2c3e50"));

        // Create recommendations table
        recommendationsTable = createRecommendationsTable();

        // Details pane
        TextArea detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setMaxHeight(150);
        detailsArea.setPromptText("Select a recommendation to view details...");

        // Listen to selection changes
        recommendationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                detailsArea.setText(buildDetailText(newSelection.getRecommendation()));
            }
        });

        VBox.setVgrow(recommendationsTable, Priority.ALWAYS);

        content.getChildren().addAll(
            summaryLabel,
            new Separator(),
            new Label("Recommendations:"),
            recommendationsTable,
            new Label("Details:"),
            detailsArea
        );

        return content;
    }

    private TableView<RecommendationRow> createRecommendationsTable() {
        TableView<RecommendationRow> table = new TableView<>();

        // Select column
        TableColumn<RecommendationRow, Boolean> selectCol = new TableColumn<>("Apply");
        selectCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setMinWidth(60);
        selectCol.setMaxWidth(60);

        // Priority column
        TableColumn<RecommendationRow, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(param -> {
            RecommendationPriority priority = param.getValue().getRecommendation().getPriority();
            return new SimpleStringProperty(priority.toString());
        });
        priorityCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "CRITICAL":
                            setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                            break;
                        case "HIGH":
                            setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
                            break;
                        case "MEDIUM":
                            setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
                            break;
                        case "LOW":
                            setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                            break;
                    }
                }
            }
        });
        priorityCol.setMinWidth(90);
        priorityCol.setMaxWidth(90);

        // Course column
        TableColumn<RecommendationRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getRecommendation().getCourse().getCourseName()));
        courseCol.setMinWidth(180);

        // Current Teacher column
        TableColumn<RecommendationRow, String> currentCol = new TableColumn<>("Current Teacher");
        currentCol.setCellValueFactory(param -> {
            var teacher = param.getValue().getRecommendation().getCurrentTeacher();
            return new SimpleStringProperty(teacher != null ? getTeacherName(teacher) : "Unassigned");
        });
        currentCol.setMinWidth(140);

        // Recommended Teacher column
        TableColumn<RecommendationRow, String> recommendedCol = new TableColumn<>("Recommended Teacher");
        recommendedCol.setCellValueFactory(param -> {
            var teacher = param.getValue().getRecommendation().getRecommendedTeacher();
            return new SimpleStringProperty(teacher != null ? getTeacherName(teacher) : "N/A");
        });
        recommendedCol.setMinWidth(160);

        // Match Score column
        TableColumn<RecommendationRow, String> scoreCol = new TableColumn<>("Match");
        scoreCol.setCellValueFactory(param -> new SimpleStringProperty(
            param.getValue().getRecommendation().getMatchScore() + "%"));
        scoreCol.setMinWidth(70);
        scoreCol.setMaxWidth(70);

        // Reason column
        TableColumn<RecommendationRow, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(param -> {
            String reasoning = param.getValue().getRecommendation().getReasoning();
            // Extract first line only for table
            String firstLine = reasoning.split("\n")[0];
            return new SimpleStringProperty(firstLine);
        });
        reasonCol.setMinWidth(250);

        table.getColumns().addAll(selectCol, priorityCol, courseCol, currentCol, recommendedCol, scoreCol, reasonCol);
        table.setEditable(true);
        table.setItems(recommendations);

        return table;
    }

    // ========================================================================
    // BUSINESS LOGIC
    // ========================================================================

    private void loadRecommendations() {
        try {
            List<CourseAssignmentRecommendation> recs = assignmentService.analyzeAndRecommend();

            recommendations.clear();
            for (CourseAssignmentRecommendation rec : recs) {
                recommendations.add(new RecommendationRow(rec, true)); // Pre-select all
            }

            updateSummary(recs);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load recommendations");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void updateSummary(List<CourseAssignmentRecommendation> recs) {
        long critical = recs.stream().filter(r -> r.getPriority() == RecommendationPriority.CRITICAL).count();
        long high = recs.stream().filter(r -> r.getPriority() == RecommendationPriority.HIGH).count();
        long medium = recs.stream().filter(r -> r.getPriority() == RecommendationPriority.MEDIUM).count();
        long low = recs.stream().filter(r -> r.getPriority() == RecommendationPriority.LOW).count();

        summaryLabel.setText(String.format(
            "Found %d recommendations: %d CRITICAL, %d HIGH, %d MEDIUM, %d LOW",
            recs.size(), critical, high, medium, low
        ));
    }

    private String buildDetailText(CourseAssignmentRecommendation rec) {
        StringBuilder sb = new StringBuilder();

        sb.append("COURSE: ").append(rec.getCourse().getCourseName()).append("\n");
        sb.append("CODE: ").append(rec.getCourse().getCourseCode()).append("\n");
        sb.append("SUBJECT: ").append(rec.getCourse().getSubject()).append("\n\n");

        sb.append("PRIORITY: ").append(rec.getPriority()).append("\n\n");

        sb.append("CURRENT TEACHER: ");
        if (rec.getCurrentTeacher() != null) {
            sb.append(getTeacherName(rec.getCurrentTeacher())).append("\n");
        } else {
            sb.append("Unassigned\n");
        }

        sb.append("RECOMMENDED TEACHER: ");
        if (rec.getRecommendedTeacher() != null) {
            sb.append(getTeacherName(rec.getRecommendedTeacher())).append("\n");
            sb.append("Match Score: ").append(rec.getMatchScore()).append("%\n");
        } else {
            sb.append("None available\n");
        }

        sb.append("\nREASONING:\n").append(rec.getReasoning()).append("\n");

        if (!rec.getMatchingCertifications().isEmpty()) {
            sb.append("\nMATCHING CERTIFICATIONS:\n");
            rec.getMatchingCertifications().forEach(cert -> sb.append("  ✓ ").append(cert).append("\n"));
        }

        if (!rec.getMissingCertifications().isEmpty()) {
            sb.append("\nMISSING CERTIFICATIONS:\n");
            rec.getMissingCertifications().forEach(cert -> sb.append("  ✗ ").append(cert).append("\n"));
        }

        if (!rec.getAlternativeTeachers().isEmpty()) {
            sb.append("\nALTERNATIVE TEACHERS:\n");
            for (TeacherMatchInfo alt : rec.getAlternativeTeachers()) {
                sb.append("  • ").append(getTeacherName(alt.getTeacher()))
                    .append(" (").append(alt.getMatchScore()).append("%) - ")
                    .append(alt.getCurrentCourseLoad()).append(" courses\n");
            }
        }

        return sb.toString();
    }

    private void applyAllRecommendations() {
        // Select all rows
        recommendations.forEach(row -> row.setSelected(true));
        recommendationsTable.refresh();
    }

    private String getTeacherName(com.heronix.model.domain.Teacher teacher) {
        if (teacher.getFirstName() != null && teacher.getLastName() != null) {
            return teacher.getFirstName() + " " + teacher.getLastName();
        } else if (teacher.getName() != null) {
            return teacher.getName();
        }
        return "Unknown";
    }

    // ========================================================================
    // RESULT HANDLING
    // ========================================================================

    public List<CourseAssignmentRecommendation> getSelectedRecommendations() {
        return recommendations.stream()
            .filter(RecommendationRow::isSelected)
            .map(RecommendationRow::getRecommendation)
            .collect(Collectors.toList());
    }

    // ========================================================================
    // ROW MODEL
    // ========================================================================

    public static class RecommendationRow {
        private final CourseAssignmentRecommendation recommendation;
        private boolean selected;

        public RecommendationRow(CourseAssignmentRecommendation recommendation, boolean selected) {
            this.recommendation = recommendation;
            this.selected = selected;
        }

        public CourseAssignmentRecommendation getRecommendation() {
            return recommendation;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
