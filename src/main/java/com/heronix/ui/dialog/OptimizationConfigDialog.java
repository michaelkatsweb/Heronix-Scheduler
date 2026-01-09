package com.heronix.ui.dialog;

import com.heronix.model.domain.OptimizationConfig;
import com.heronix.model.enums.ConstraintType;
import com.heronix.model.enums.OptimizationAlgorithm;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * Optimization Configuration Dialog
 * Allows users to configure optimization settings
 *
 * Location: src/main/java/com/eduscheduler/ui/dialog/OptimizationConfigDialog.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
public class OptimizationConfigDialog extends Dialog<OptimizationConfig> {

    private final OptimizationConfig initialConfig;

    // Algorithm Settings
    private ComboBox<OptimizationAlgorithm> algorithmCombo;
    private Spinner<Integer> populationSizeSpinner;
    private Spinner<Integer> maxGenerationsSpinner;
    private Spinner<Double> mutationRateSpinner;
    private Spinner<Double> crossoverRateSpinner;
    private Spinner<Integer> eliteSizeSpinner;
    private Spinner<Integer> tournamentSizeSpinner;

    // Termination Settings
    private Spinner<Integer> maxRuntimeSpinner;
    private Spinner<Integer> stagnationLimitSpinner;
    private Spinner<Double> targetFitnessSpinner;

    // Performance Settings
    private CheckBox parallelProcessingCheck;
    private Spinner<Integer> threadCountSpinner;

    // Constraint Weights
    private Map<ConstraintType, Spinner<Integer>> constraintWeightSpinners;

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public OptimizationConfigDialog(Stage owner, OptimizationConfig config) {
        this.initialConfig = config;
        this.constraintWeightSpinners = new HashMap<>();

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Optimization Configuration");
        setHeaderText("Configure Schedule Optimization Settings");

        // Build UI
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setContent(createContent());

        // Set minimum size
        dialogPane.setMinWidth(700);
        dialogPane.setMinHeight(600);

        // Load initial values
        loadConfigValues();

        // Result converter
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return buildConfig();
            }
            return null;
        });
    }

    // ========================================================================
    // UI CREATION
    // ========================================================================

    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tabs
        tabPane.getTabs().addAll(
            createAlgorithmTab(),
            createTerminationTab(),
            createConstraintWeightsTab(),
            createPerformanceTab()
        );

        content.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        return content;
    }

    private Tab createAlgorithmTab() {
        Tab tab = new Tab("Algorithm");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Algorithm Selection
        Label algoLabel = new Label("Algorithm:");
        algorithmCombo = new ComboBox<>();
        algorithmCombo.getItems().addAll(OptimizationAlgorithm.values());
        algorithmCombo.setMaxWidth(Double.MAX_VALUE);
        algorithmCombo.setOnAction(e -> updateAlgorithmDefaults());

        grid.add(algoLabel, 0, row);
        grid.add(algorithmCombo, 1, row++);

        // Population Size
        Label popLabel = new Label("Population Size:");
        populationSizeSpinner = new Spinner<>(10, 500, 100, 10);
        populationSizeSpinner.setEditable(true);
        populationSizeSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(popLabel, 0, row);
        grid.add(populationSizeSpinner, 1, row++);

        // Max Generations
        Label genLabel = new Label("Max Generations:");
        maxGenerationsSpinner = new Spinner<>(100, 10000, 1000, 100);
        maxGenerationsSpinner.setEditable(true);
        maxGenerationsSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(genLabel, 0, row);
        grid.add(maxGenerationsSpinner, 1, row++);

        // Mutation Rate
        Label mutLabel = new Label("Mutation Rate:");
        mutationRateSpinner = new Spinner<>(0.0, 1.0, 0.1, 0.05);
        mutationRateSpinner.setEditable(true);
        mutationRateSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(mutLabel, 0, row);
        grid.add(mutationRateSpinner, 1, row++);

        // Crossover Rate
        Label crossLabel = new Label("Crossover Rate:");
        crossoverRateSpinner = new Spinner<>(0.0, 1.0, 0.8, 0.05);
        crossoverRateSpinner.setEditable(true);
        crossoverRateSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(crossLabel, 0, row);
        grid.add(crossoverRateSpinner, 1, row++);

        // Elite Size
        Label eliteLabel = new Label("Elite Size:");
        eliteSizeSpinner = new Spinner<>(1, 50, 5, 1);
        eliteSizeSpinner.setEditable(true);
        eliteSizeSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(eliteLabel, 0, row);
        grid.add(eliteSizeSpinner, 1, row++);

        // Tournament Size
        Label tournLabel = new Label("Tournament Size:");
        tournamentSizeSpinner = new Spinner<>(2, 20, 5, 1);
        tournamentSizeSpinner.setEditable(true);
        tournamentSizeSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(tournLabel, 0, row);
        grid.add(tournamentSizeSpinner, 1, row++);

        // Make grid columns grow
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(150);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        tab.setContent(grid);
        return tab;
    }

    private Tab createTerminationTab() {
        Tab tab = new Tab("Termination");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Max Runtime
        Label runtimeLabel = new Label("Max Runtime (seconds):");
        maxRuntimeSpinner = new Spinner<>(10, 3600, 300, 30);
        maxRuntimeSpinner.setEditable(true);
        maxRuntimeSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(runtimeLabel, 0, row);
        grid.add(maxRuntimeSpinner, 1, row++);

        // Stagnation Limit
        Label stagnLabel = new Label("Stagnation Limit:");
        Label stagnDesc = new Label("(Stop if no improvement for N generations)");
        stagnDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        stagnationLimitSpinner = new Spinner<>(10, 1000, 100, 10);
        stagnationLimitSpinner.setEditable(true);
        stagnationLimitSpinner.setMaxWidth(Double.MAX_VALUE);

        VBox stagnBox = new VBox(5, stagnationLimitSpinner, stagnDesc);

        grid.add(stagnLabel, 0, row);
        grid.add(stagnBox, 1, row++);

        // Target Fitness
        Label targetLabel = new Label("Target Fitness:");
        Label targetDesc = new Label("(Stop when fitness reaches target, 0 = disabled)");
        targetDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        targetFitnessSpinner = new Spinner<>(0.0, 10000.0, 0.0, 100.0);
        targetFitnessSpinner.setEditable(true);
        targetFitnessSpinner.setMaxWidth(Double.MAX_VALUE);

        VBox targetBox = new VBox(5, targetFitnessSpinner, targetDesc);

        grid.add(targetLabel, 0, row);
        grid.add(targetBox, 1, row++);

        // Make grid columns grow
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(150);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        tab.setContent(grid);
        return tab;
    }

    private Tab createConstraintWeightsTab() {
        Tab tab = new Tab("Constraint Weights");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label header = new Label("Adjust the importance of each constraint (higher = more important)");
        header.setStyle("-fx-font-weight: bold;");

        // Reset button
        Button resetButton = new Button("Reset to Defaults");
        resetButton.setOnAction(e -> resetConstraintWeights());

        HBox headerBox = new HBox(10, header, new Region(), resetButton);
        HBox.setHgrow(headerBox.getChildren().get(1), Priority.ALWAYS);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().add(headerBox);

        // Scrollable constraint list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        VBox constraintBox = new VBox(8);
        constraintBox.setPadding(new Insets(10));

        // Group by category
        Label hardLabel = new Label("Hard Constraints (Must Satisfy)");
        hardLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        constraintBox.getChildren().add(hardLabel);

        for (ConstraintType type : ConstraintType.values()) {
            if (type.getCategory() == ConstraintType.ConstraintCategory.HARD) {
                constraintBox.getChildren().add(createConstraintWeightRow(type));
            }
        }

        // Separator
        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));
        constraintBox.getChildren().add(separator);

        Label softLabel = new Label("Soft Constraints (Prefer to Satisfy)");
        softLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        constraintBox.getChildren().add(softLabel);

        for (ConstraintType type : ConstraintType.values()) {
            if (type.getCategory() == ConstraintType.ConstraintCategory.SOFT) {
                constraintBox.getChildren().add(createConstraintWeightRow(type));
            }
        }

        scrollPane.setContent(constraintBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        content.getChildren().add(scrollPane);

        tab.setContent(content);
        return tab;
    }

    private HBox createConstraintWeightRow(ConstraintType type) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5));

        // Icon and name
        Label iconLabel = new Label(type.getIcon());
        iconLabel.setStyle("-fx-font-size: 16px;");
        iconLabel.setMinWidth(30);

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(type.getDisplayName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label descLabel = new Label(type.getDescription());
        descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        nameBox.getChildren().addAll(nameLabel, descLabel);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        // Weight spinner
        Spinner<Integer> weightSpinner = new Spinner<>(0, 5000, type.getDefaultWeight(), 50);
        weightSpinner.setEditable(true);
        weightSpinner.setPrefWidth(120);

        constraintWeightSpinners.put(type, weightSpinner);

        row.getChildren().addAll(iconLabel, nameBox, weightSpinner);

        return row;
    }

    private Tab createPerformanceTab() {
        Tab tab = new Tab("Performance");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Parallel Processing
        Label parallelLabel = new Label("Parallel Processing:");
        parallelProcessingCheck = new CheckBox("Enable multi-threaded optimization");
        parallelProcessingCheck.setOnAction(e -> threadCountSpinner.setDisable(!parallelProcessingCheck.isSelected()));

        grid.add(parallelLabel, 0, row);
        grid.add(parallelProcessingCheck, 1, row++);

        // Thread Count
        Label threadLabel = new Label("Thread Count:");
        threadCountSpinner = new Spinner<>(1, Runtime.getRuntime().availableProcessors(), 4, 1);
        threadCountSpinner.setEditable(true);
        threadCountSpinner.setMaxWidth(Double.MAX_VALUE);

        grid.add(threadLabel, 0, row);
        grid.add(threadCountSpinner, 1, row++);

        // Make grid columns grow
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(150);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        tab.setContent(grid);
        return tab;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void loadConfigValues() {
        if (initialConfig == null) return;

        // Algorithm settings
        algorithmCombo.setValue(initialConfig.getAlgorithm());
        populationSizeSpinner.getValueFactory().setValue(initialConfig.getPopulationSize());
        maxGenerationsSpinner.getValueFactory().setValue(initialConfig.getMaxGenerations());
        mutationRateSpinner.getValueFactory().setValue(initialConfig.getMutationRate());
        crossoverRateSpinner.getValueFactory().setValue(initialConfig.getCrossoverRate());
        eliteSizeSpinner.getValueFactory().setValue(initialConfig.getEliteSize());
        tournamentSizeSpinner.getValueFactory().setValue(initialConfig.getTournamentSize());

        // Termination settings
        maxRuntimeSpinner.getValueFactory().setValue(initialConfig.getMaxRuntimeSeconds());
        stagnationLimitSpinner.getValueFactory().setValue(initialConfig.getStagnationLimit());
        targetFitnessSpinner.getValueFactory().setValue(
            initialConfig.getTargetFitness() != null ? initialConfig.getTargetFitness() : 0.0
        );

        // Performance settings
        parallelProcessingCheck.setSelected(initialConfig.getUseParallelProcessing());
        threadCountSpinner.getValueFactory().setValue(initialConfig.getThreadCount());
        threadCountSpinner.setDisable(!initialConfig.getUseParallelProcessing());

        // Constraint weights
        for (Map.Entry<ConstraintType, Spinner<Integer>> entry : constraintWeightSpinners.entrySet()) {
            int weight = initialConfig.getConstraintWeight(entry.getKey());
            entry.getValue().getValueFactory().setValue(weight);
        }
    }

    private void updateAlgorithmDefaults() {
        OptimizationAlgorithm algo = algorithmCombo.getValue();
        if (algo != null) {
            populationSizeSpinner.getValueFactory().setValue(algo.getRecommendedPopulationSize());
            maxGenerationsSpinner.getValueFactory().setValue(algo.getRecommendedIterations());
        }
    }

    private void resetConstraintWeights() {
        for (Map.Entry<ConstraintType, Spinner<Integer>> entry : constraintWeightSpinners.entrySet()) {
            entry.getValue().getValueFactory().setValue(entry.getKey().getDefaultWeight());
        }
    }

    private OptimizationConfig buildConfig() {
        OptimizationConfig.OptimizationConfigBuilder builder = OptimizationConfig.builder();

        // Copy ID if editing existing config
        if (initialConfig != null && initialConfig.getId() != null) {
            builder.id(initialConfig.getId());
        }

        // Algorithm settings
        builder.algorithm(algorithmCombo.getValue())
               .populationSize(populationSizeSpinner.getValue())
               .maxGenerations(maxGenerationsSpinner.getValue())
               .mutationRate(mutationRateSpinner.getValue())
               .crossoverRate(crossoverRateSpinner.getValue())
               .eliteSize(eliteSizeSpinner.getValue())
               .tournamentSize(tournamentSizeSpinner.getValue());

        // Termination settings
        builder.maxRuntimeSeconds(maxRuntimeSpinner.getValue())
               .stagnationLimit(stagnationLimitSpinner.getValue());

        Double targetFitness = targetFitnessSpinner.getValue();
        if (targetFitness > 0) {
            builder.targetFitness(targetFitness);
        }

        // Performance settings
        builder.useParallelProcessing(parallelProcessingCheck.isSelected())
               .threadCount(threadCountSpinner.getValue());

        // Build config
        OptimizationConfig config = builder.build();

        // Set constraint weights
        for (Map.Entry<ConstraintType, Spinner<Integer>> entry : constraintWeightSpinners.entrySet()) {
            config.setConstraintWeight(entry.getKey(), entry.getValue().getValue());
        }

        return config;
    }
}
