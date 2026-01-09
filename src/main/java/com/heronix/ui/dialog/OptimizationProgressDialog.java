package com.heronix.ui.dialog;

import com.heronix.service.OptimizationService.OptimizationProgress;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Optimization Progress Dialog
 * Real-time monitoring of optimization progress
 *
 * Location: src/main/java/com/eduscheduler/ui/dialog/OptimizationProgressDialog.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
public class OptimizationProgressDialog extends Dialog<ButtonType> {

    // Progress Components
    private ProgressBar overallProgress;
    private Label statusLabel;
    private Label generationLabel;
    private Label fitnessLabel;
    private Label conflictsLabel;
    private Label elapsedLabel;

    // Charts
    private LineChartPane fitnessChart;
    private LineChartPane conflictChart;

    // Statistics
    private Label initialFitnessValue;
    private Label currentFitnessValue;
    private Label improvementValue;
    private Label initialConflictsValue;
    private Label currentConflictsValue;

    // Control
    private Button cancelButton;
    private volatile boolean cancelled = false;

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public OptimizationProgressDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Schedule Optimization in Progress");
        setHeaderText("Optimizing schedule using Genetic Algorithm");

        // Build UI
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(createContent());

        // Custom buttons
        ButtonType cancelButtonType = new ButtonType("Cancel Optimization", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().add(cancelButtonType);

        // Get cancel button
        cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.setOnAction(e -> {
            cancelled = true;
            statusLabel.setText("Cancelling optimization...");
            cancelButton.setDisable(true);
        });

        // Set minimum size
        dialogPane.setMinWidth(800);
        dialogPane.setMinHeight(600);

        // Make dialog non-closable via X button
        setOnCloseRequest(e -> e.consume());
    }

    // ========================================================================
    // UI CREATION
    // ========================================================================

    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Status Section
        content.getChildren().add(createStatusSection());

        // Progress Bar
        content.getChildren().add(createProgressSection());

        // Statistics
        content.getChildren().add(createStatisticsSection());

        // Charts
        TabPane chartTabs = new TabPane();
        chartTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        fitnessChart = new LineChartPane("Fitness Over Time", "Generation", "Fitness Score");
        conflictChart = new LineChartPane("Conflicts Over Time", "Generation", "Conflict Count");

        Tab fitnessTab = new Tab("Fitness Progress", fitnessChart);
        Tab conflictTab = new Tab("Conflict Reduction", conflictChart);

        chartTabs.getTabs().addAll(fitnessTab, conflictTab);
        VBox.setVgrow(chartTabs, Priority.ALWAYS);

        content.getChildren().add(chartTabs);

        return content;
    }

    private VBox createStatusSection() {
        VBox section = new VBox(8);

        statusLabel = new Label("Initializing optimization...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox infoBox = new HBox(30);

        generationLabel = new Label("Generation: 0 / 0");
        fitnessLabel = new Label("Fitness: 0.00");
        conflictsLabel = new Label("Conflicts: 0");
        elapsedLabel = new Label("Elapsed: 0s");

        infoBox.getChildren().addAll(generationLabel, fitnessLabel, conflictsLabel, elapsedLabel);

        section.getChildren().addAll(statusLabel, infoBox);

        return section;
    }

    private VBox createProgressSection() {
        VBox section = new VBox(5);

        Label progressLabel = new Label("Overall Progress:");
        progressLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        overallProgress = new ProgressBar(0);
        overallProgress.setMaxWidth(Double.MAX_VALUE);
        overallProgress.setPrefHeight(25);

        section.getChildren().addAll(progressLabel, overallProgress);

        return section;
    }

    private GridPane createStatisticsSection() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        int row = 0;

        // Headers
        Label fitnessHeader = new Label("Fitness Statistics");
        fitnessHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        grid.add(fitnessHeader, 0, row, 2, 1);

        Label conflictHeader = new Label("Conflict Statistics");
        conflictHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        grid.add(conflictHeader, 2, row++, 2, 1);

        // Initial values
        grid.add(new Label("Initial:"), 0, row);
        initialFitnessValue = new Label("0.00");
        grid.add(initialFitnessValue, 1, row);

        grid.add(new Label("Initial:"), 2, row);
        initialConflictsValue = new Label("0");
        grid.add(initialConflictsValue, 3, row++);

        // Current values
        grid.add(new Label("Current:"), 0, row);
        currentFitnessValue = new Label("0.00");
        currentFitnessValue.setStyle("-fx-font-weight: bold;");
        grid.add(currentFitnessValue, 1, row);

        grid.add(new Label("Current:"), 2, row);
        currentConflictsValue = new Label("0");
        currentConflictsValue.setStyle("-fx-font-weight: bold;");
        grid.add(currentConflictsValue, 3, row++);

        // Improvement
        grid.add(new Label("Improvement:"), 0, row);
        improvementValue = new Label("0%");
        improvementValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
        grid.add(improvementValue, 1, row);

        return grid;
    }

    // ========================================================================
    // PUBLIC API
    // ========================================================================

    /**
     * Update progress display
     */
    public void updateProgress(OptimizationProgress progress) {
        Platform.runLater(() -> {
            // Update labels
            statusLabel.setText(progress.getStatusMessage());
            generationLabel.setText(String.format("Generation: %d / %d",
                progress.getCurrentGeneration(), progress.getTotalGenerations()));
            fitnessLabel.setText(String.format("Fitness: %.2f", progress.getBestFitness()));
            conflictsLabel.setText(String.format("Conflicts: %d", progress.getConflictCount()));
            elapsedLabel.setText(String.format("Elapsed: %ds", progress.getElapsedSeconds()));

            // Update progress bar
            double progressPercent = (double) progress.getCurrentGeneration() / progress.getTotalGenerations();
            overallProgress.setProgress(progressPercent);

            // Update statistics
            if (progress.getCurrentGeneration() == 0) {
                initialFitnessValue.setText(String.format("%.2f", progress.getCurrentFitness()));
                initialConflictsValue.setText(String.valueOf(progress.getConflictCount()));
            }

            currentFitnessValue.setText(String.format("%.2f", progress.getBestFitness()));
            currentConflictsValue.setText(String.valueOf(progress.getConflictCount()));

            // Calculate improvement
            double initialFitness = parseDouble(initialFitnessValue.getText());
            if (initialFitness > 0) {
                double improvement = ((progress.getBestFitness() - initialFitness) / initialFitness) * 100;
                improvementValue.setText(String.format("%.1f%%", improvement));

                // Color based on improvement
                if (improvement > 0) {
                    improvementValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
                } else if (improvement < 0) {
                    improvementValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #c62828;");
                } else {
                    improvementValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #757575;");
                }
            }

            // Update charts
            fitnessChart.addDataPoint(progress.getCurrentGeneration(), progress.getBestFitness());
            conflictChart.addDataPoint(progress.getCurrentGeneration(), progress.getConflictCount());
        });
    }

    /**
     * Mark optimization as complete
     */
    public void markComplete(boolean success, String message) {
        Platform.runLater(() -> {
            if (success) {
                statusLabel.setText("Optimization completed successfully!");
                statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            } else {
                statusLabel.setText("Optimization failed: " + message);
                statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c62828;");
            }

            overallProgress.setProgress(1.0);

            // Change cancel button to close
            cancelButton.setText("Close");
            cancelButton.setDisable(false);
            cancelButton.setOnAction(e -> close());
        });
    }

    /**
     * Check if user cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Simple line chart pane for progress visualization
     */
    private static class LineChartPane extends VBox {
        private javafx.scene.chart.LineChart<Number, Number> chart;
        private javafx.scene.chart.XYChart.Series<Number, Number> series;

        public LineChartPane(String title, String xLabel, String yLabel) {
            javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
            xAxis.setLabel(xLabel);
            xAxis.setAutoRanging(true);

            javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
            yAxis.setLabel(yLabel);
            yAxis.setAutoRanging(true);

            chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
            chart.setTitle(title);
            chart.setCreateSymbols(false);
            chart.setLegendVisible(false);

            series = new javafx.scene.chart.XYChart.Series<>();
            chart.getData().add(series);

            getChildren().add(chart);
            VBox.setVgrow(chart, Priority.ALWAYS);
        }

        public void addDataPoint(int x, double y) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(x, y));

            // Limit data points to prevent memory issues
            if (series.getData().size() > 500) {
                series.getData().remove(0);
            }
        }
    }
}
