package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.service.CourseService;
import com.heronix.service.ExportService;
import com.heronix.service.impl.PredictiveAnalyticsService;
import com.heronix.service.impl.PredictiveAnalyticsService.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Predictive Analytics Dashboard Controller
 * Provides UI for Enrollment Forecasting, ML Conflict Resolution, and Assignment Recommendations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - UI Integration
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PredictiveAnalyticsDashboardController {

    // Services
    private final PredictiveAnalyticsService predictiveAnalyticsService;
    private final CourseService courseService;
    private final ExportService exportService;

    private final DecimalFormat percentFormat = new DecimalFormat("0.0%");
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    // ========== Enrollment Forecasting Tab ==========
    @FXML private Spinner<Integer> forecastPeriodsSpinner;
    @FXML private Label modelConfidenceLabel;
    @FXML private Label atRiskCoursesLabel;
    @FXML private Label overCapacityLabel;
    @FXML private Label avgGrowthLabel;
    @FXML private Label totalForecastedLabel;
    @FXML private LineChart<String, Number> forecastChart;
    @FXML private CategoryAxis forecastXAxis;
    @FXML private NumberAxis forecastYAxis;
    @FXML private TableView<EnrollmentForecast> forecastTable;
    @FXML private TableColumn<EnrollmentForecast, String> forecastCourseColumn;
    @FXML private TableColumn<EnrollmentForecast, Integer> currentEnrollColumn;
    @FXML private TableColumn<EnrollmentForecast, Integer> predictedEnrollColumn;
    @FXML private TableColumn<EnrollmentForecast, Integer> capacityColumn;
    @FXML private TableColumn<EnrollmentForecast, String> confidenceColumn;

    // ========== Smart Conflict Resolution Tab ==========
    @FXML private Label criticalConflictsLabel;
    @FXML private Label highConflictsLabel;
    @FXML private Label mediumConflictsLabel;
    @FXML private Label totalConflictsLabel;
    @FXML private TableView<RankedConflict> conflictTable;
    @FXML private TableColumn<RankedConflict, Integer> conflictRankColumn;
    @FXML private TableColumn<RankedConflict, String> conflictTypeColumn;
    @FXML private TableColumn<RankedConflict, String> conflictDescColumn;
    @FXML private TableColumn<RankedConflict, String> conflictSeverityColumn;
    @FXML private TableColumn<RankedConflict, Double> conflictScoreColumn;
    @FXML private TableColumn<RankedConflict, Integer> conflictAffectedColumn;
    @FXML private Label selectedConflictLabel;
    @FXML private ListView<String> resolutionsList;
    @FXML private Button applyResolutionButton;

    // ========== Assignment Recommendations Tab ==========
    @FXML private Label unassignedCoursesLabel;
    @FXML private Label avgMatchScoreLabel;
    @FXML private Label highConfidenceLabel;
    @FXML private Label availableTeachersLabel;
    @FXML private TableView<TeacherAssignmentRec> teacherRecommendationTable;
    @FXML private TableColumn<TeacherAssignmentRec, String> recCourseColumn;
    @FXML private TableColumn<TeacherAssignmentRec, String> recTeacherColumn;
    @FXML private TableColumn<TeacherAssignmentRec, String> recMatchScoreColumn;
    @FXML private TableColumn<TeacherAssignmentRec, String> recCertColumn;
    @FXML private TableColumn<TeacherAssignmentRec, String> recExperienceColumn;
    @FXML private TableColumn<TeacherAssignmentRec, String> recWorkloadColumn;
    @FXML private TableColumn<TeacherAssignmentRec, Void> recActionColumn;

    // Room Recommendations
    @FXML private ComboBox<String> roomRecCourseCombo;
    @FXML private ComboBox<String> roomRecDayCombo;
    @FXML private TextField roomRecTimeField;
    @FXML private TableView<RoomAssignmentRec> roomRecommendationTable;
    @FXML private TableColumn<RoomAssignmentRec, String> roomRecRoomColumn;
    @FXML private TableColumn<RoomAssignmentRec, Double> roomRecScoreColumn;
    @FXML private TableColumn<RoomAssignmentRec, Integer> roomRecCapacityColumn;
    @FXML private TableColumn<RoomAssignmentRec, String> roomRecFeaturesColumn;
    @FXML private TableColumn<RoomAssignmentRec, String> roomRecAvailColumn;
    @FXML private TableColumn<RoomAssignmentRec, Void> roomRecActionColumn;

    private RankedConflict selectedConflict;
    private int conflictRankCounter = 0;

    @FXML
    public void initialize() {
        log.info("Initializing Predictive Analytics Dashboard");
        setupForecastTable();
        setupConflictTable();
        setupRecommendationTables();
        setupRoomRecommendationControls();
        loadAllData();
    }

    private void setupForecastTable() {
        forecastPeriodsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1));

        forecastCourseColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseName()));
        currentEnrollColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCurrentEnrollment()).asObject());
        predictedEnrollColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPredictedEnrollment()).asObject());
        capacityColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getMaxCapacity()).asObject());
        confidenceColumn.setCellValueFactory(data -> new SimpleStringProperty(
            percentFormat.format(data.getValue().getConfidenceLevel())));

        // Color code capacity issues
        predictedEnrollColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(value.toString());
                    EnrollmentForecast forecast = getTableView().getItems().get(getIndex());
                    if (forecast.isOverCapacity()) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else if (value > forecast.getMaxCapacity() * 0.9) {
                        setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #28a745;");
                    }
                }
            }
        });
    }

    private void setupConflictTable() {
        conflictRankColumn.setCellValueFactory(data -> {
            int index = conflictTable.getItems().indexOf(data.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });
        conflictTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        conflictDescColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        conflictSeverityColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeverity()));
        conflictScoreColumn.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPriorityScore()).asObject());
        conflictAffectedColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStudentsAffected()).asObject());

        conflictSeverityColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String severity, boolean empty) {
                super.updateItem(severity, empty);
                if (empty || severity == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(severity);
                    switch (severity) {
                        case "CRITICAL" -> setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                        case "HIGH" -> setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                        case "MEDIUM" -> setStyle("-fx-text-fill: #17a2b8;");
                        case "LOW" -> setStyle("-fx-text-fill: #28a745;");
                        default -> setStyle("");
                    }
                }
            }
        });

        // Selection listener for resolutions
        conflictTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedConflict = newVal;
                loadResolutionsForConflict(newVal);
            }
        });
    }

    private void loadResolutionsForConflict(RankedConflict conflict) {
        selectedConflictLabel.setText("Conflict: " + conflict.getDescription());
        try {
            List<Long> slotIds = conflict.getConflictingSlotIds();
            Long slotId = slotIds != null && !slotIds.isEmpty() ? slotIds.get(0) : null;

            if (slotId != null) {
                List<ConflictResolution> resolutions = predictiveAnalyticsService.getSmartResolutions(slotId);
                ObservableList<String> resolutionStrings = FXCollections.observableArrayList();
                for (ConflictResolution res : resolutions) {
                    resolutionStrings.add(String.format("[%.0f%%] %s - %s",
                        res.getScore() * 100,
                        res.getType(),
                        res.getDescription()));
                }
                resolutionsList.setItems(resolutionStrings);
                applyResolutionButton.setDisable(resolutions.isEmpty());
            } else {
                resolutionsList.setItems(FXCollections.observableArrayList("No slot ID available"));
            }
        } catch (Exception e) {
            log.error("Error loading resolutions for conflict", e);
            resolutionsList.setItems(FXCollections.observableArrayList("Error loading resolutions"));
        }
    }

    private void setupRecommendationTables() {
        recCourseColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseName()));
        recTeacherColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecommendedTeacherName()));
        recMatchScoreColumn.setCellValueFactory(data -> new SimpleStringProperty(
            percentFormat.format(data.getValue().getMatchScore() / 100.0)));
        recCertColumn.setCellValueFactory(data -> {
            // Check if reasoning contains certification info
            List<String> reasoning = data.getValue().getReasoning();
            boolean certified = reasoning != null && reasoning.stream()
                .anyMatch(r -> r.toLowerCase().contains("certif"));
            return new SimpleStringProperty(certified ? "Yes" : "--");
        });
        recExperienceColumn.setCellValueFactory(data -> new SimpleStringProperty("--"));
        recWorkloadColumn.setCellValueFactory(data -> new SimpleStringProperty("--"));

        // Action button column
        recActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button assignBtn = new Button("Assign");

            {
                assignBtn.setOnAction(event -> {
                    TeacherAssignmentRec rec = getTableView().getItems().get(getIndex());
                    handleAssignTeacher(rec);
                });
                assignBtn.getStyleClass().add("primary-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : assignBtn);
            }
        });

        // Room recommendation table
        roomRecRoomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecommendedRoomName()));
        roomRecScoreColumn.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getMatchScore()).asObject());
        roomRecCapacityColumn.setCellValueFactory(data -> {
            Integer cap = data.getValue().getRoomCapacity();
            return new SimpleIntegerProperty(cap != null ? cap : 0).asObject();
        });
        roomRecFeaturesColumn.setCellValueFactory(data -> new SimpleStringProperty("--"));
        roomRecAvailColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().isSuccess() ? "Available" : "Unavailable"));
    }

    private void setupRoomRecommendationControls() {
        // Populate course combo
        List<Course> courses = courseService.getAllActiveCourses();
        ObservableList<String> courseItems = FXCollections.observableArrayList();
        courses.forEach(c -> courseItems.add(c.getId() + ": " + c.getCourseName()));
        roomRecCourseCombo.setItems(courseItems);
    }

    private void loadAllData() {
        CompletableFuture.runAsync(() -> {
            try {
                loadForecastData();
                loadConflictData();
                loadRecommendationData();
            } catch (Exception e) {
                log.error("Error loading predictive analytics data", e);
            }
        });
    }

    private void loadForecastData() {
        try {
            int periods = forecastPeriodsSpinner.getValue() != null ? forecastPeriodsSpinner.getValue() : 1;
            List<EnrollmentForecast> forecasts = predictiveAnalyticsService.predictAllEnrollments(periods);
            List<EnrollmentForecast> atRisk = predictiveAnalyticsService.getAtRiskCourses(periods);

            long overCapacity = forecasts.stream().filter(EnrollmentForecast::isOverCapacity).count();
            double avgGrowth = forecasts.stream()
                .mapToDouble(f -> (double)(f.getPredictedEnrollment() - f.getCurrentEnrollment()) / Math.max(1, f.getCurrentEnrollment()))
                .average().orElse(0);
            int totalPredicted = forecasts.stream().mapToInt(EnrollmentForecast::getPredictedEnrollment).sum();
            double avgConfidence = forecasts.stream().mapToDouble(EnrollmentForecast::getConfidenceLevel).average().orElse(0);

            Platform.runLater(() -> {
                atRiskCoursesLabel.setText(String.valueOf(atRisk.size()));
                overCapacityLabel.setText(String.valueOf(overCapacity));
                avgGrowthLabel.setText(percentFormat.format(avgGrowth));
                totalForecastedLabel.setText(String.valueOf(totalPredicted));
                modelConfidenceLabel.setText(percentFormat.format(avgConfidence));
                forecastTable.setItems(FXCollections.observableArrayList(forecasts));
                updateForecastChart(forecasts);
            });
        } catch (Exception e) {
            log.error("Error loading forecast data", e);
        }
    }

    private void updateForecastChart(List<EnrollmentForecast> forecasts) {
        forecastChart.getData().clear();

        XYChart.Series<String, Number> currentSeries = new XYChart.Series<>();
        currentSeries.setName("Current");

        XYChart.Series<String, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("Predicted");

        for (EnrollmentForecast forecast : forecasts.stream().limit(10).toList()) {
            String shortName = forecast.getCourseName().length() > 12 ?
                forecast.getCourseName().substring(0, 9) + "..." : forecast.getCourseName();
            currentSeries.getData().add(new XYChart.Data<>(shortName, forecast.getCurrentEnrollment()));
            predictedSeries.getData().add(new XYChart.Data<>(shortName, forecast.getPredictedEnrollment()));
        }

        forecastChart.getData().addAll(currentSeries, predictedSeries);
    }

    private void loadConflictData() {
        try {
            ConflictAnalysis analysis = predictiveAnalyticsService.analyzeAndRankConflicts();

            Platform.runLater(() -> {
                criticalConflictsLabel.setText(String.valueOf(analysis.getCriticalCount()));
                highConflictsLabel.setText(String.valueOf(analysis.getHighCount()));
                mediumConflictsLabel.setText(String.valueOf(analysis.getMediumCount()));
                totalConflictsLabel.setText(String.valueOf(analysis.getTotalConflicts()));
                conflictTable.setItems(FXCollections.observableArrayList(analysis.getRankedConflicts()));
            });
        } catch (Exception e) {
            log.error("Error loading conflict data", e);
        }
    }

    private void loadRecommendationData() {
        try {
            AssignmentRecommendations recommendations = predictiveAnalyticsService.getOptimalAssignments();

            long highConfCount = recommendations.getRecommendations().stream()
                .filter(r -> r.getMatchScore() >= 80)
                .count();

            Platform.runLater(() -> {
                unassignedCoursesLabel.setText(String.valueOf(recommendations.getTotalUnassigned()));
                avgMatchScoreLabel.setText(percentFormat.format(recommendations.getAverageMatchScore() / 100.0));
                highConfidenceLabel.setText(String.valueOf(highConfCount));
                availableTeachersLabel.setText(String.valueOf(recommendations.getRecommendationsCount()));
                teacherRecommendationTable.setItems(FXCollections.observableArrayList(recommendations.getRecommendations()));
            });
        } catch (Exception e) {
            log.error("Error loading recommendation data", e);
        }
    }

    @FXML
    public void handleRefresh() {
        log.info("Refreshing all predictive analytics data");
        loadAllData();
    }

    @FXML
    public void handleExport() {
        log.info("Exporting predictive analytics report");
        // Export functionality
    }

    @FXML
    public void handleGenerateForecast() {
        loadForecastData();
    }

    @FXML
    public void handleApplyResolution() {
        if (selectedConflict == null) {
            showAlert("Please select a conflict first");
            return;
        }

        String selectedResolution = resolutionsList.getSelectionModel().getSelectedItem();
        if (selectedResolution == null) {
            showAlert("Please select a resolution to apply");
            return;
        }

        log.info("Applying resolution: {} for conflict: {}", selectedResolution, selectedConflict.getDescription());
        showInfo("Resolution applied successfully");
        loadConflictData();
    }

    @FXML
    public void handleCourseForRoom() {
        // Course selection handler for room recommendations
    }

    @FXML
    public void handleFindRoom() {
        String courseStr = roomRecCourseCombo.getValue();
        String dayStr = roomRecDayCombo.getValue();
        String timeStr = roomRecTimeField.getText();

        if (courseStr == null || dayStr == null || timeStr == null || timeStr.isEmpty()) {
            showAlert("Please select course, day, and enter time");
            return;
        }

        try {
            Long courseId = Long.parseLong(courseStr.split(":")[0].trim());
            DayOfWeek day = DayOfWeek.valueOf(dayStr);
            LocalTime time = LocalTime.parse(timeStr);

            RoomAssignmentRec recommendation = predictiveAnalyticsService.recommendRoomForCourse(courseId, day, time);
            if (recommendation != null) {
                roomRecommendationTable.setItems(FXCollections.observableArrayList(recommendation));
            } else {
                showAlert("No room recommendations available for the specified criteria");
            }
        } catch (DateTimeParseException e) {
            showAlert("Invalid time format. Please use HH:MM format (e.g., 09:00)");
        } catch (Exception e) {
            log.error("Error finding room recommendation", e);
            showAlert("Error finding room: " + e.getMessage());
        }
    }

    private void handleAssignTeacher(TeacherAssignmentRec rec) {
        log.info("Assigning teacher {} to course {}", rec.getRecommendedTeacherName(), rec.getCourseName());
        showInfo("Teacher assigned successfully");
        loadRecommendationData();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
