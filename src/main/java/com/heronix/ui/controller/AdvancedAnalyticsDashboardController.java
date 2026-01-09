package com.heronix.ui.controller;

import com.heronix.service.TeacherService;
import com.heronix.service.RoomService;
import com.heronix.service.ExportService;
import com.heronix.service.impl.AdvancedAnalyticsService;
import com.heronix.service.impl.AdvancedAnalyticsService.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced Analytics Dashboard Controller
 * Provides UI for Teacher Burnout Risk, SPED Compliance, What-If Scenarios, and Prep Time Equity
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - UI Integration
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdvancedAnalyticsDashboardController {

    // Services
    private final AdvancedAnalyticsService advancedAnalyticsService;
    private final TeacherService teacherService;
    private final RoomService roomService;
    private final ExportService exportService;

    private final DecimalFormat percentFormat = new DecimalFormat("0.0%");
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    // ========== Burnout Risk Tab ==========
    @FXML private Label highRiskCountLabel;
    @FXML private Label moderateRiskCountLabel;
    @FXML private Label lowRiskCountLabel;
    @FXML private Label avgBurnoutScoreLabel;
    @FXML private TableView<BurnoutRiskResult> burnoutRiskTable;
    @FXML private TableColumn<BurnoutRiskResult, String> burnoutTeacherColumn;
    @FXML private TableColumn<BurnoutRiskResult, Integer> burnoutScoreColumn;
    @FXML private TableColumn<BurnoutRiskResult, String> burnoutLevelColumn;
    @FXML private TableColumn<BurnoutRiskResult, Double> workloadColumn;
    @FXML private TableColumn<BurnoutRiskResult, Integer> backToBackColumn;
    @FXML private TableColumn<BurnoutRiskResult, Integer> prepCountColumn;
    @FXML private TableColumn<BurnoutRiskResult, String> spedPercentColumn;
    @FXML private TableColumn<BurnoutRiskResult, String> burnoutRecommendationColumn;

    // ========== SPED Compliance Tab ==========
    @FXML private Label totalIEPStudentsLabel;
    @FXML private Label compliantStudentsLabel;
    @FXML private Label nonCompliantStudentsLabel;
    @FXML private Label complianceRateLabel;
    @FXML private ProgressBar complianceProgress;
    @FXML private TableView<SPEDComplianceResult> spedComplianceTable;
    @FXML private TableColumn<SPEDComplianceResult, String> spedStudentColumn;
    @FXML private TableColumn<SPEDComplianceResult, Integer> requiredMinutesColumn;
    @FXML private TableColumn<SPEDComplianceResult, Integer> scheduledMinutesColumn;
    @FXML private TableColumn<SPEDComplianceResult, String> compliancePercentColumn;
    @FXML private TableColumn<SPEDComplianceResult, String> complianceStatusColumn;
    @FXML private TableColumn<SPEDComplianceResult, Integer> shortfallColumn;
    @FXML private TableColumn<SPEDComplianceResult, String> serviceTypesColumn;

    // ========== What-If Tab ==========
    @FXML private ComboBox<String> scenarioTypeCombo;
    @FXML private ComboBox<String> scenarioEntityCombo;
    @FXML private Spinner<Integer> scenarioParamSpinner;
    @FXML private TextField scenarioDescField;
    @FXML private Label impactScoreLabel;
    @FXML private Label affectedClassesLabel;
    @FXML private Label newConflictsLabel;
    @FXML private Label feasibilityLabel;
    @FXML private ListView<String> whatIfRecommendationsList;

    // ========== Prep Equity Tab ==========
    @FXML private Label equityIndexLabel;
    @FXML private Label avgPrepMinutesLabel;
    @FXML private Label belowAvgCountLabel;
    @FXML private Label prepStdDevLabel;
    @FXML private ProgressBar equityProgress;
    @FXML private BarChart<String, Number> prepTimeChart;
    @FXML private CategoryAxis prepXAxis;
    @FXML private NumberAxis prepYAxis;
    @FXML private TableView<TeacherPrepTime> prepEquityTable;
    @FXML private TableColumn<TeacherPrepTime, String> prepTeacherColumn;
    @FXML private TableColumn<TeacherPrepTime, Integer> prepMinutesColumn;
    @FXML private TableColumn<TeacherPrepTime, String> prepDifferenceColumn;
    @FXML private TableColumn<TeacherPrepTime, String> prepStatusColumn;

    private double averagePrepMinutes = 0;

    @FXML
    public void initialize() {
        log.info("Initializing Advanced Analytics Dashboard");
        setupBurnoutTable();
        setupSpedTable();
        setupWhatIfControls();
        setupPrepEquityTable();
        loadAllData();
    }

    private void setupBurnoutTable() {
        burnoutTeacherColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTeacherName()));
        burnoutScoreColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRiskScore()).asObject());
        burnoutLevelColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRiskLevel()));
        workloadColumn.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getClassesPerDay()).asObject());
        backToBackColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBackToBackCount()).asObject());
        prepCountColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getUniquePreps()).asObject());
        spedPercentColumn.setCellValueFactory(data -> new SimpleStringProperty(
            percentFormat.format(data.getValue().getSpedPercentage() / 100.0)));
        burnoutRecommendationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));

        // Color code risk levels
        burnoutLevelColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String level, boolean empty) {
                super.updateItem(level, empty);
                if (empty || level == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(level);
                    switch (level) {
                        case "HIGH" -> setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                        case "MODERATE" -> setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                        case "LOW" -> setStyle("-fx-text-fill: #28a745;");
                        default -> setStyle("");
                    }
                }
            }
        });
    }

    private void setupSpedTable() {
        spedStudentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentName()));
        requiredMinutesColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRequiredMinutesWeekly()).asObject());
        scheduledMinutesColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getScheduledMinutesWeekly()).asObject());
        compliancePercentColumn.setCellValueFactory(data -> new SimpleStringProperty(
            percentFormat.format(data.getValue().getCompliancePercentage() / 100.0)));
        complianceStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().isCompliant() ? "Compliant" : "Non-Compliant"));
        shortfallColumn.setCellValueFactory(data -> {
            int shortfall = data.getValue().getRequiredMinutesWeekly() - data.getValue().getScheduledMinutesWeekly();
            return new SimpleIntegerProperty(Math.max(0, shortfall)).asObject();
        });
        serviceTypesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));

        complianceStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Compliant".equals(status)) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupWhatIfControls() {
        // Populate scenario type ComboBox
        scenarioTypeCombo.getItems().addAll(
            "TEACHER_REMOVAL",
            "ROOM_CLOSURE",
            "ENROLLMENT_INCREASE",
            "TIME_CHANGE"
        );

        scenarioParamSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));

        scenarioTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateEntityCombo(newVal);
            }
        });
    }

    private void updateEntityCombo(String scenarioType) {
        ObservableList<String> entities = FXCollections.observableArrayList();
        switch (scenarioType) {
            case "TEACHER_REMOVAL" -> {
                teacherService.getAllActiveTeachers().stream()
                    .forEach(t -> entities.add(t.getId() + ": " + t.getFirstName() + " " + t.getLastName()));
            }
            case "ROOM_CLOSURE" -> {
                roomService.findAll().stream()
                    .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                    .forEach(r -> entities.add(r.getId() + ": " + r.getRoomNumber()));
            }
            case "ENROLLMENT_INCREASE", "TIME_CHANGE" -> {
                entities.add("All Courses");
            }
        }
        scenarioEntityCombo.setItems(entities);
        if (!entities.isEmpty()) {
            scenarioEntityCombo.getSelectionModel().selectFirst();
        }
    }

    private void setupPrepEquityTable() {
        prepTeacherColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTeacherName()));
        prepMinutesColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPrepMinutesWeekly()).asObject());
        prepDifferenceColumn.setCellValueFactory(data -> {
            int diff = data.getValue().getPrepMinutesWeekly() - (int) averagePrepMinutes;
            String text = diff >= 0 ? "+" + diff : String.valueOf(diff);
            return new SimpleStringProperty(text);
        });
        prepStatusColumn.setCellValueFactory(data -> {
            int mins = data.getValue().getPrepMinutesWeekly();
            return new SimpleStringProperty(mins < averagePrepMinutes ? "Below Avg" : "Above Avg");
        });

        prepStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Below Avg".equals(status)) {
                        setStyle("-fx-text-fill: #dc3545;");
                    } else if ("Above Avg".equals(status)) {
                        setStyle("-fx-text-fill: #28a745;");
                    } else {
                        setStyle("-fx-text-fill: #666666;");
                    }
                }
            }
        });
    }

    private void loadAllData() {
        CompletableFuture.runAsync(() -> {
            try {
                loadBurnoutData();
                loadSpedComplianceData();
                loadPrepEquityData();
            } catch (Exception e) {
                log.error("Error loading analytics data", e);
            }
        });
    }

    private void loadBurnoutData() {
        try {
            List<BurnoutRiskResult> results = advancedAnalyticsService.getAllTeacherBurnoutRisks();

            long highRisk = results.stream().filter(r -> "HIGH".equals(r.getRiskLevel())).count();
            long moderateRisk = results.stream().filter(r -> "MODERATE".equals(r.getRiskLevel())).count();
            long lowRisk = results.stream().filter(r -> "LOW".equals(r.getRiskLevel())).count();
            double avgScore = results.stream().mapToInt(BurnoutRiskResult::getRiskScore).average().orElse(0);

            Platform.runLater(() -> {
                highRiskCountLabel.setText(String.valueOf(highRisk));
                moderateRiskCountLabel.setText(String.valueOf(moderateRisk));
                lowRiskCountLabel.setText(String.valueOf(lowRisk));
                avgBurnoutScoreLabel.setText(String.valueOf((int) avgScore));
                burnoutRiskTable.setItems(FXCollections.observableArrayList(results));
            });
        } catch (Exception e) {
            log.error("Error loading burnout data", e);
        }
    }

    private void loadSpedComplianceData() {
        try {
            SPEDComplianceSummary summary = advancedAnalyticsService.getSPEDComplianceSummary();

            Platform.runLater(() -> {
                totalIEPStudentsLabel.setText(String.valueOf(summary.getTotalStudentsWithIEP()));
                compliantStudentsLabel.setText(String.valueOf(summary.getCompliantCount()));
                nonCompliantStudentsLabel.setText(String.valueOf(summary.getNonCompliantCount()));
                double rate = summary.getAverageCompliancePercentage();
                complianceRateLabel.setText(percentFormat.format(rate / 100.0));
                complianceProgress.setProgress(rate / 100.0);
            });
        } catch (Exception e) {
            log.error("Error loading SPED compliance data", e);
        }
    }

    private void loadPrepEquityData() {
        try {
            PrepTimeEquityResult result = advancedAnalyticsService.calculatePrepTimeEquity();
            averagePrepMinutes = result.getAveragePrepMinutes();

            Platform.runLater(() -> {
                equityIndexLabel.setText(decimalFormat.format(result.getEquityIndex()));
                avgPrepMinutesLabel.setText(String.valueOf((int) result.getAveragePrepMinutes()));
                belowAvgCountLabel.setText(String.valueOf(result.getBelowAverageCount()));
                // Calculate standard deviation from data
                double variance = 0;
                List<TeacherPrepTime> times = result.getTeacherPrepTimes();
                if (times != null && !times.isEmpty()) {
                    double avg = result.getAveragePrepMinutes();
                    for (TeacherPrepTime t : times) {
                        variance += Math.pow(t.getPrepMinutesWeekly() - avg, 2);
                    }
                    variance /= times.size();
                }
                prepStdDevLabel.setText(decimalFormat.format(Math.sqrt(variance)));
                equityProgress.setProgress(result.getEquityIndex() / 100.0);

                // Update chart and table
                if (times != null) {
                    updatePrepTimeChart(times);
                    prepEquityTable.setItems(FXCollections.observableArrayList(times));
                }
            });
        } catch (Exception e) {
            log.error("Error loading prep equity data", e);
        }
    }

    private void updatePrepTimeChart(List<TeacherPrepTime> teacherPrepTimes) {
        prepTimeChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Prep Minutes");

        for (TeacherPrepTime t : teacherPrepTimes) {
            String shortName = t.getTeacherName().length() > 15 ?
                t.getTeacherName().substring(0, 12) + "..." : t.getTeacherName();
            series.getData().add(new XYChart.Data<>(shortName, t.getPrepMinutesWeekly()));
        }

        prepTimeChart.getData().add(series);
    }

    @FXML
    public void handleRefresh() {
        log.info("Refreshing all analytics data");
        loadAllData();
    }

    @FXML
    public void handleExport() {
        log.info("Exporting analytics report");
        // Export functionality
    }

    @FXML
    public void handleRunSimulation() {
        String type = scenarioTypeCombo.getValue();
        String entity = scenarioEntityCombo.getValue();
        Integer param = scenarioParamSpinner.getValue();
        String desc = scenarioDescField.getText();

        if (type == null || entity == null) {
            showAlert("Please select scenario type and entity");
            return;
        }

        try {
            Long entityId = extractEntityId(entity);
            WhatIfScenario scenario = WhatIfScenario.builder()
                .type(ScenarioType.valueOf(type))
                .entityId(entityId)
                .parameter(param)
                .description(desc)
                .build();

            WhatIfResult result = advancedAnalyticsService.simulateScenario(scenario);

            // Count impacts
            List<ImpactItem> impacts = result.getImpacts();
            int totalImpacts = impacts != null ? impacts.size() : 0;
            long criticalImpacts = impacts != null ?
                impacts.stream().filter(i -> "HIGH".equals(i.getSeverity())).count() : 0;

            impactScoreLabel.setText(String.valueOf(totalImpacts));
            affectedClassesLabel.setText(String.valueOf(totalImpacts));
            newConflictsLabel.setText(String.valueOf(criticalImpacts));
            feasibilityLabel.setText(result.isFeasible() ? "FEASIBLE" : "NOT FEASIBLE");
            feasibilityLabel.setStyle(result.isFeasible() ?
                "-fx-text-fill: #28a745; -fx-font-weight: bold;" :
                "-fx-text-fill: #dc3545; -fx-font-weight: bold;");

            // Show recommendation and message
            ObservableList<String> recommendations = FXCollections.observableArrayList();
            if (result.getRecommendation() != null) {
                recommendations.add(result.getRecommendation());
            }
            if (result.getMessage() != null) {
                recommendations.add(result.getMessage());
            }
            whatIfRecommendationsList.setItems(recommendations);

        } catch (Exception e) {
            log.error("Error running simulation", e);
            showAlert("Error running simulation: " + e.getMessage());
        }
    }

    private Long extractEntityId(String entity) {
        if (entity == null || entity.equals("All Courses")) {
            return 0L;
        }
        String[] parts = entity.split(":");
        return Long.parseLong(parts[0].trim());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
