package com.heronix.ui.dialog;

import com.heronix.model.domain.OptimizationResult;
import com.heronix.model.enums.ConstraintType;
import com.heronix.service.OptimizationService;
import com.heronix.service.OptimizationService.FitnessBreakdown;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Optimization Results Dialog
 * Displays detailed results and fitness breakdown
 *
 * Location: src/main/java/com/eduscheduler/ui/dialog/OptimizationResultsDialog.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
public class OptimizationResultsDialog extends Dialog<Void> {

    private final OptimizationResult result;
    private final FitnessBreakdown breakdown;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public OptimizationResultsDialog(Stage owner, OptimizationResult result, FitnessBreakdown breakdown) {
        this.result = result;
        this.breakdown = breakdown;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Optimization Results");

        // Set header text
        if (result.getSuccessful()) {
            setHeaderText(String.format("Optimization completed successfully in %d seconds",
                result.getRuntimeSeconds()));
        } else {
            setHeaderText("Optimization completed with issues");
        }

        // Build UI
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.setContent(createContent());

        // Set minimum size
        dialogPane.setMinWidth(800);
        dialogPane.setMinHeight(700);
    }

    // ========================================================================
    // UI CREATION
    // ========================================================================

    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().addAll(
            createSummaryTab(),
            createFitnessBreakdownTab(),
            createDetailsTab()
        );

        VBox.setVgrow(tabPane, Priority.ALWAYS);
        content.getChildren().add(tabPane);

        return content;
    }

    private Tab createSummaryTab() {
        Tab tab = new Tab("Summary");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Status Card
        content.getChildren().add(createStatusCard());

        // Metrics Grid
        content.getChildren().add(createMetricsGrid());

        // Performance Chart
        if (result.getSuccessful()) {
            content.getChildren().add(createImprovementChart());
        }

        tab.setContent(content);
        return tab;
    }

    private VBox createStatusCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " +
            (result.getSuccessful() ? "#e8f5e9" : "#ffebee") +
            "; -fx-background-radius: 5;");

        Label statusLabel = new Label(result.getSuccessful() ? "SUCCESS" : "FAILED");
        statusLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " +
            (result.getSuccessful() ? "#2e7d32" : "#c62828"));

        Label messageLabel = new Label(result.getMessage());
        messageLabel.setStyle("-fx-font-size: 14px;");
        messageLabel.setWrapText(true);

        card.getChildren().addAll(statusLabel, messageLabel);

        return card;
    }

    private GridPane createMetricsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        int row = 0;

        // Fitness Metrics
        addMetricRow(grid, row++, "Initial Fitness:", String.format("%.2f", result.getInitialFitness()));
        addMetricRow(grid, row++, "Final Fitness:", String.format("%.2f", result.getFinalFitness()));
        addMetricRow(grid, row++, "Best Fitness:", String.format("%.2f", result.getBestFitness()));
        addMetricRow(grid, row++, "Improvement:", String.format("%.1f%%", result.getImprovementPercentage()));

        // Separator
        Separator sep1 = new Separator();
        grid.add(sep1, 0, row++, 2, 1);

        // Conflict Metrics
        addMetricRow(grid, row++, "Initial Conflicts:", String.valueOf(result.getInitialConflicts()));
        addMetricRow(grid, row++, "Final Conflicts:", String.valueOf(result.getFinalConflicts()));
        addMetricRow(grid, row++, "Critical Conflicts:", String.valueOf(result.getCriticalConflicts()));

        int conflictReduction = result.getInitialConflicts() - result.getFinalConflicts();
        String conflictText = conflictReduction >= 0 ?
            String.format("-%d (%.1f%%)", conflictReduction,
                result.getInitialConflicts() > 0 ?
                    (conflictReduction * 100.0 / result.getInitialConflicts()) : 0) :
            String.format("+%d", Math.abs(conflictReduction));
        addMetricRow(grid, row++, "Conflicts Reduced:", conflictText);

        // Separator
        Separator sep2 = new Separator();
        grid.add(sep2, 0, row++, 2, 1);

        // Algorithm Metrics
        addMetricRow(grid, row++, "Algorithm:", result.getAlgorithm().getDisplayName());
        addMetricRow(grid, row++, "Generations:", String.valueOf(result.getGenerationsExecuted()));
        addMetricRow(grid, row++, "Runtime:", result.getRuntimeSeconds() + " seconds");

        return grid;
    }

    private void addMetricRow(GridPane grid, int row, String label, String value) {
        Label labelWidget = new Label(label);
        labelWidget.setStyle("-fx-font-weight: bold;");

        Label valueWidget = new Label(value);
        valueWidget.setStyle("-fx-font-size: 14px;");

        grid.add(labelWidget, 0, row);
        grid.add(valueWidget, 1, row);
    }

    private VBox createImprovementChart() {
        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(15));
        chartBox.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        Label title = new Label("Optimization Improvement");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Progress bar showing improvement
        ProgressBar improvementBar = new ProgressBar();
        improvementBar.setMaxWidth(Double.MAX_VALUE);
        improvementBar.setPrefHeight(30);

        double maxImprovement = 100.0; // 100% improvement
        double actualImprovement = Math.min(result.getImprovementPercentage(), maxImprovement);
        improvementBar.setProgress(actualImprovement / maxImprovement);

        // Color based on improvement level
        String color;
        if (actualImprovement > 50) {
            color = "#2e7d32"; // Green
        } else if (actualImprovement > 20) {
            color = "#f57c00"; // Orange
        } else if (actualImprovement > 0) {
            color = "#fbc02d"; // Yellow
        } else {
            color = "#c62828"; // Red
        }

        improvementBar.setStyle("-fx-accent: " + color + ";");

        HBox legendBox = new HBox(20);
        legendBox.setAlignment(Pos.CENTER);

        legendBox.getChildren().addAll(
            createLegendItem("Fitness", String.format("%.0f → %.0f",
                result.getInitialFitness(), result.getFinalFitness())),
            createLegendItem("Conflicts", String.format("%d → %d",
                result.getInitialConflicts(), result.getFinalConflicts()))
        );

        chartBox.getChildren().addAll(title, improvementBar, legendBox);

        return chartBox;
    }

    private HBox createLegendItem(String label, String value) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER);

        Label labelWidget = new Label(label + ":");
        labelWidget.setStyle("-fx-font-weight: bold;");

        Label valueWidget = new Label(value);

        item.getChildren().addAll(labelWidget, valueWidget);

        return item;
    }

    private Tab createFitnessBreakdownTab() {
        Tab tab = new Tab("Fitness Breakdown");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Overall scores
        GridPane overallGrid = new GridPane();
        overallGrid.setHgap(20);
        overallGrid.setVgap(10);
        overallGrid.setPadding(new Insets(15));
        overallGrid.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        addMetricRow(overallGrid, 0, "Total Fitness:", String.format("%.2f", breakdown.getTotalFitness()));
        addMetricRow(overallGrid, 1, "Hard Constraint Score:", String.format("%.2f", breakdown.getHardConstraintScore()));
        addMetricRow(overallGrid, 2, "Soft Constraint Score:", String.format("%.2f", breakdown.getSoftConstraintScore()));

        content.getChildren().add(overallGrid);

        // Constraint breakdown table
        Label tableLabel = new Label("Constraint Violations:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TableView<ConstraintRow> table = createConstraintTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        content.getChildren().addAll(tableLabel, table);

        tab.setContent(content);
        return tab;
    }

    private TableView<ConstraintRow> createConstraintTable() {
        TableView<ConstraintRow> table = new TableView<>();

        // Category column
        TableColumn<ConstraintRow, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);

        // Constraint column
        TableColumn<ConstraintRow, String> constraintCol = new TableColumn<>("Constraint");
        constraintCol.setCellValueFactory(new PropertyValueFactory<>("constraint"));
        constraintCol.setPrefWidth(250);

        // Violations column
        TableColumn<ConstraintRow, Integer> violationsCol = new TableColumn<>("Violations");
        violationsCol.setCellValueFactory(new PropertyValueFactory<>("violations"));
        violationsCol.setPrefWidth(100);
        violationsCol.setStyle("-fx-alignment: CENTER;");

        // Score column
        TableColumn<ConstraintRow, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setPrefWidth(100);
        scoreCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(categoryCol, constraintCol, violationsCol, scoreCol);

        // Populate data
        for (Map.Entry<ConstraintType, Double> entry : breakdown.getConstraintScores().entrySet()) {
            ConstraintType type = entry.getKey();
            double score = entry.getValue();
            int violations = breakdown.getViolationCounts().getOrDefault(type, 0);

            table.getItems().add(new ConstraintRow(
                type.getCategory().getDisplayName(),
                type.getDisplayName(),
                violations,
                String.format("%.2f", score)
            ));
        }

        return table;
    }

    private Tab createDetailsTab() {
        Tab tab = new Tab("Details");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Execution details
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);
        detailsGrid.setPadding(new Insets(15));
        detailsGrid.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        int row = 0;

        if (result.getStartedAt() != null) {
            addMetricRow(detailsGrid, row++, "Started:", result.getStartedAt().format(TIME_FORMATTER));
        }

        if (result.getCompletedAt() != null) {
            addMetricRow(detailsGrid, row++, "Completed:", result.getCompletedAt().format(TIME_FORMATTER));
        }

        addMetricRow(detailsGrid, row++, "Status:", result.getStatus().name());
        addMetricRow(detailsGrid, row++, "Algorithm:", result.getAlgorithm().getDisplayName());
        addMetricRow(detailsGrid, row++, "Generations Executed:", String.valueOf(result.getGenerationsExecuted()));
        addMetricRow(detailsGrid, row++, "Runtime:", result.getRuntimeSeconds() + " seconds");

        if (result.getBestGeneration() != null) {
            addMetricRow(detailsGrid, row++, "Best Generation:", String.valueOf(result.getBestGeneration()));
        }

        content.getChildren().add(detailsGrid);

        // Configuration used
        if (result.getConfig() != null) {
            Label configLabel = new Label("Configuration Used:");
            configLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            TextArea configText = new TextArea(result.getConfig().getSummary());
            configText.setEditable(false);
            configText.setPrefRowCount(8);
            VBox.setVgrow(configText, Priority.ALWAYS);

            content.getChildren().addAll(configLabel, configText);
        }

        // Logs
        if (result.getOptimizationLog() != null && !result.getOptimizationLog().isEmpty()) {
            Label logLabel = new Label("Optimization Log:");
            logLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            TextArea logText = new TextArea(result.getOptimizationLog());
            logText.setEditable(false);
            logText.setPrefRowCount(10);
            VBox.setVgrow(logText, Priority.ALWAYS);

            content.getChildren().addAll(logLabel, logText);
        }

        // Error details
        if (result.getErrorDetails() != null && !result.getErrorDetails().isEmpty()) {
            Label errorLabel = new Label("Error Details:");
            errorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #c62828;");

            TextArea errorText = new TextArea(result.getErrorDetails());
            errorText.setEditable(false);
            errorText.setPrefRowCount(6);
            errorText.setStyle("-fx-text-fill: #c62828;");

            content.getChildren().addAll(errorLabel, errorText);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        tab.setContent(scrollPane);
        return tab;
    }

    // ========================================================================
    // SUPPORTING CLASSES
    // ========================================================================

    /**
     * Table row for constraint breakdown
     */
    public static class ConstraintRow {
        private final String category;
        private final String constraint;
        private final Integer violations;
        private final String score;

        public ConstraintRow(String category, String constraint, Integer violations, String score) {
            this.category = category;
            this.constraint = constraint;
            this.violations = violations;
            this.score = score;
        }

        public String getCategory() { return category; }
        public String getConstraint() { return constraint; }
        public Integer getViolations() { return violations; }
        public String getScore() { return score; }
    }
}
