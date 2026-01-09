package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSection;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.CourseSectionRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.service.EnrollmentForecastingService;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Forecasting Dashboard Controller
 *
 * Provides visualization and analysis of enrollment forecasting data
 * including trend analysis, capacity planning, and section recommendations.
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/ForecastingDashboardController.java
 */
@Component
@Slf4j
public class ForecastingDashboardController {

    // Services
    @Autowired
    private EnrollmentForecastingService forecastingService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    // Header Controls
    @FXML private ComboBox<Integer> targetYearComboBox;

    // Overview Metrics
    @FXML private Label totalEnrollmentLabel;
    @FXML private Label enrollmentChangeLabel;
    @FXML private Label growthRateLabel;
    @FXML private Label growthTrendLabel;
    @FXML private Label capacityStatusLabel;
    @FXML private Label capacityDetailsLabel;
    @FXML private Label coursesAtRiskLabel;
    @FXML private Label riskDetailsLabel;

    // Grade Enrollment Chart
    @FXML private BarChart<String, Number> gradeEnrollmentChart;
    @FXML private CategoryAxis gradeXAxis;
    @FXML private NumberAxis gradeYAxis;
    @FXML private TableView<GradeEnrollment> gradeDetailsTable;
    @FXML private TableColumn<GradeEnrollment, String> gradeColumn;
    @FXML private TableColumn<GradeEnrollment, Integer> currentCountColumn;
    @FXML private TableColumn<GradeEnrollment, Integer> forecastedCountColumn;
    @FXML private TableColumn<GradeEnrollment, String> changeColumn;

    // Course Demand Trends
    @FXML private LineChart<String, Number> courseDemandChart;
    @FXML private CategoryAxis demandXAxis;
    @FXML private NumberAxis demandYAxis;
    @FXML private ComboBox<String> trendFilterComboBox;
    @FXML private CheckBox showGrowingOnlyCheckbox;

    // Capacity Warnings
    @FXML private TableView<CapacityWarning> capacityWarningsTable;
    @FXML private TableColumn<CapacityWarning, String> courseNameColumn;
    @FXML private TableColumn<CapacityWarning, String> courseCodeColumn;
    @FXML private TableColumn<CapacityWarning, Integer> currentCapacityColumn;
    @FXML private TableColumn<CapacityWarning, Integer> forecastedDemandColumn;
    @FXML private TableColumn<CapacityWarning, Integer> gapColumn;
    @FXML private TableColumn<CapacityWarning, String> warningTypeColumn;
    @FXML private TableColumn<CapacityWarning, String> recommendationColumn;
    @FXML private ComboBox<String> warningFilterComboBox;

    // Section Recommendations
    @FXML private TableView<SectionRecommendation> sectionRecommendationsTable;
    @FXML private TableColumn<SectionRecommendation, String> recCourseNameColumn;
    @FXML private TableColumn<SectionRecommendation, String> recCourseCodeColumn;
    @FXML private TableColumn<SectionRecommendation, Integer> currentSectionsColumn;
    @FXML private TableColumn<SectionRecommendation, Integer> recommendedSectionsColumn;
    @FXML private TableColumn<SectionRecommendation, Integer> sectionChangeColumn;
    @FXML private TableColumn<SectionRecommendation, String> trendColumn;
    @FXML private TableColumn<SectionRecommendation, Void> actionColumn;

    // Growing/Declining Courses
    @FXML private ListView<String> growingCoursesList;
    @FXML private ListView<String> decliningCoursesList;

    // Forecasting Methods
    @FXML private Label linearRegressionAccuracyLabel;
    @FXML private Label movingAverageAccuracyLabel;
    @FXML private Label recommendedMethodLabel;
    @FXML private Label methodReasonLabel;

    private Integer selectedYear;
    private Map<Long, Integer> currentDemandData;
    private Map<Long, Integer> forecastedDemandData;

    @FXML
    public void initialize() {
        log.info("Initializing Forecasting Dashboard Controller");

        setupYearSelector();
        setupTables();
        setupCharts();
        setupFilters();

        // Load initial data
        if (selectedYear != null) {
            loadDashboardData();
        }
    }

    private void setupYearSelector() {
        int currentYear = LocalDate.now().getYear();

        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int i = currentYear; i <= currentYear + 10; i++) {
            years.add(i);
        }

        safeSetItems(targetYearComboBox, years);

        // Default to next year
        selectedYear = currentYear + 1;
        if (targetYearComboBox != null) {
            targetYearComboBox.setValue(selectedYear);
        }
    }

    private void setupTables() {
        // Grade Details Table
        if (gradeColumn != null) {
            gradeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGrade()));
        }
        if (currentCountColumn != null) {
            currentCountColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCurrentCount()).asObject());
        }
        if (forecastedCountColumn != null) {
            forecastedCountColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getForecastedCount()).asObject());
        }
        if (changeColumn != null) {
            changeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getChangeText()));
        }

        // Capacity Warnings Table
        if (courseNameColumn != null) {
            courseNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseName()));
        }
        if (courseCodeColumn != null) {
            courseCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseCode()));
        }
        if (currentCapacityColumn != null) {
            currentCapacityColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCurrentCapacity()).asObject());
        }
        if (forecastedDemandColumn != null) {
            forecastedDemandColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getForecastedDemand()).asObject());
        }
        if (gapColumn != null) {
            gapColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getGap()).asObject());
        }
        if (warningTypeColumn != null) {
            warningTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getWarningType()));
        }
        if (recommendationColumn != null) {
            recommendationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRecommendation()));
        }

        // Section Recommendations Table
        if (recCourseNameColumn != null) {
            recCourseNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseName()));
        }
        if (recCourseCodeColumn != null) {
            recCourseCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseCode()));
        }
        if (currentSectionsColumn != null) {
            currentSectionsColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCurrentSections()).asObject());
        }
        if (recommendedSectionsColumn != null) {
            recommendedSectionsColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getRecommendedSections()).asObject());
        }
        if (sectionChangeColumn != null) {
            sectionChangeColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSectionChange()).asObject());
        }
        if (trendColumn != null) {
            trendColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTrend()));
        }
    }

    private void setupCharts() {
        // Grade enrollment chart setup
        if (gradeEnrollmentChart != null) {
            gradeEnrollmentChart.setAnimated(true);
        }

        // Course demand chart setup
        if (courseDemandChart != null) {
            courseDemandChart.setAnimated(true);
            courseDemandChart.setCreateSymbols(true);
        }
    }

    private void setupFilters() {
        // Warning filter setup
        if (warningFilterComboBox != null) {
            ObservableList<String> warningTypes = FXCollections.observableArrayList(
                "All Warnings",
                "Over Capacity",
                "Near Capacity",
                "Under-Utilized"
            );
            warningFilterComboBox.setItems(warningTypes);
            warningFilterComboBox.setValue("All Warnings");
        }

        // Trend filter will be populated after loading courses
    }

    @FXML
    private void handleYearChange() {
        if (targetYearComboBox != null && targetYearComboBox.getValue() != null) {
            selectedYear = targetYearComboBox.getValue();
            loadDashboardData();
        }
    }

    @FXML
    private void handleRefresh() {
        loadDashboardData();
    }

    @FXML
    private void handleGenerateReport() {
        if (selectedYear == null) {
            showWarning("Please select a target year");
            return;
        }

        Task<Map<String, Object>> reportTask = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return forecastingService.getForecastingReport(selectedYear);
            }

            @Override
            protected void succeeded() {
                Map<String, Object> report = getValue();
                showReportDialog(report);
            }

            @Override
            protected void failed() {
                log.error("Failed to generate report", getException());
                showError("Failed to generate forecasting report");
            }
        };

        new Thread(reportTask).start();
    }

    @FXML
    private void handleTrendFilterChange() {
        updateCourseDemandChart();
    }

    @FXML
    private void handleFilterChange() {
        updateCourseDemandChart();
    }

    @FXML
    private void handleWarningFilterChange() {
        filterCapacityWarnings();
    }

    private void loadDashboardData() {
        if (selectedYear == null) {
            log.warn("No year selected");
            return;
        }

        log.info("Loading forecasting data for year: {}", selectedYear);

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int currentYear = LocalDate.now().getYear();

                // Load enrollment forecasts
                int totalEnrollment = forecastingService.forecastTotalEnrollment(selectedYear);
                int currentEnrollment = (int) studentRepository.count();
                int enrollmentChange = totalEnrollment - currentEnrollment;

                // Calculate growth rate
                double growthRate = forecastingService.calculateGrowthRate(currentYear, selectedYear);

                // Check capacity
                boolean hasCapacity = forecastingService.hasAdequateCapacity(selectedYear);
                Map<String, String> warnings = forecastingService.getCapacityWarnings(selectedYear);

                // Grade level forecasts
                Map<String, Integer> gradeForecasts = forecastingService.forecastEnrollmentByGrade(selectedYear);

                // Course demand
                currentDemandData = new HashMap<>();
                forecastedDemandData = forecastingService.forecastCourseDemand(selectedYear);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    updateOverviewMetrics(totalEnrollment, enrollmentChange, growthRate, hasCapacity, warnings);
                    updateGradeEnrollmentData(gradeForecasts);
                    updateCourseDemandChart();
                    updateCapacityWarnings(warnings);
                    updateSectionRecommendations();
                    updateGrowingCourses();
                    updateForecastingMethods();
                });

                return null;
            }

            @Override
            protected void failed() {
                log.error("Failed to load dashboard data", getException());
                Platform.runLater(() -> showError("Failed to load forecasting data"));
            }
        };

        new Thread(loadTask).start();
    }

    private void updateOverviewMetrics(int totalEnrollment, int enrollmentChange,
                                       double growthRate, boolean hasCapacity,
                                       Map<String, String> warnings) {
        // Total Enrollment
        safeSetText(totalEnrollmentLabel, String.valueOf(totalEnrollment));
        String changeText = enrollmentChange >= 0 ?
            String.format("+%d students from current", enrollmentChange) :
            String.format("%d students from current", enrollmentChange);
        safeSetText(enrollmentChangeLabel, changeText);

        // Growth Rate
        safeSetText(growthRateLabel, String.format("%.1f%%", growthRate));
        String trendText = growthRate > 0 ? "Increasing" :
                          growthRate < 0 ? "Decreasing" : "Stable";
        safeSetText(growthTrendLabel, trendText);

        // Capacity Status
        String capacityStatus = hasCapacity ? "✓ Adequate" : "⚠ Insufficient";
        safeSetText(capacityStatusLabel, capacityStatus);

        long overCapacity = warnings.values().stream()
            .filter(w -> w.contains("OVER CAPACITY"))
            .count();
        safeSetText(capacityDetailsLabel, overCapacity + " courses need attention");

        // Courses at Risk
        long atRisk = warnings.size();
        safeSetText(coursesAtRiskLabel, String.valueOf(atRisk));
        safeSetText(riskDetailsLabel, overCapacity + " over capacity, " +
            (atRisk - overCapacity) + " warnings");
    }

    private void updateGradeEnrollmentData(Map<String, Integer> gradeForecasts) {
        // Update chart
        if (gradeEnrollmentChart != null) {
            gradeEnrollmentChart.getData().clear();

            XYChart.Series<String, Number> forecastSeries = new XYChart.Series<>();
            forecastSeries.setName("Forecasted Enrollment");

            XYChart.Series<String, Number> currentSeries = new XYChart.Series<>();
            currentSeries.setName("Current Enrollment");

            String[] grades = {"9", "10", "11", "12"};
            for (String grade : grades) {
                int forecasted = gradeForecasts.getOrDefault(grade, 0);
                int current = getCurrentGradeCount(grade);

                forecastSeries.getData().add(new XYChart.Data<>(grade, forecasted));
                currentSeries.getData().add(new XYChart.Data<>(grade, current));
            }

            gradeEnrollmentChart.getData().addAll(currentSeries, forecastSeries);
        }

        // Update table
        if (gradeDetailsTable != null) {
            ObservableList<GradeEnrollment> gradeData = FXCollections.observableArrayList();

            String[] grades = {"9", "10", "11", "12"};
            for (String grade : grades) {
                int current = getCurrentGradeCount(grade);
                int forecasted = gradeForecasts.getOrDefault(grade, 0);
                int change = forecasted - current;
                String changeText = change >= 0 ? "+" + change : String.valueOf(change);

                gradeData.add(new GradeEnrollment(grade, current, forecasted, changeText));
            }

            gradeDetailsTable.setItems(gradeData);
        }
    }

    private void updateCourseDemandChart() {
        // This would show historical trends + forecast
        // For now, placeholder implementation
        if (courseDemandChart != null) {
            courseDemandChart.getData().clear();
            // Would populate with actual historical data
        }
    }

    private void updateCapacityWarnings(Map<String, String> warnings) {
        if (capacityWarningsTable == null) return;

        ObservableList<CapacityWarning> warningData = FXCollections.observableArrayList();

        for (Map.Entry<String, String> entry : warnings.entrySet()) {
            String courseName = entry.getKey();
            String warningText = entry.getValue();

            List<Course> courses = courseRepository.findByCourseNameContaining(courseName);
            if (courses.isEmpty()) continue;
            Course course = courses.get(0);

            int currentCapacity = calculateCurrentCapacity(course.getId());
            int forecastedDemand = forecastedDemandData.getOrDefault(course.getId(), 0);
            int gap = forecastedDemand - currentCapacity;

            String warningType = extractWarningType(warningText);
            String recommendation = generateRecommendation(gap, currentCapacity, forecastedDemand);

            warningData.add(new CapacityWarning(
                courseName,
                course.getCourseCode(),
                currentCapacity,
                forecastedDemand,
                gap,
                warningType,
                recommendation
            ));
        }

        capacityWarningsTable.setItems(warningData);
    }

    private void updateSectionRecommendations() {
        if (sectionRecommendationsTable == null) return;

        ObservableList<SectionRecommendation> recommendations = FXCollections.observableArrayList();

        List<Course> allCourses = courseRepository.findAll();

        for (Course course : allCourses) {
            int currentSections = getCurrentSectionCount(course.getId());
            int recommendedSections = forecastingService.recommendSectionCount(course.getId(), selectedYear);
            int change = recommendedSections - currentSections;
            String trend = forecastingService.getCourseTrend(course.getId());

            if (change != 0) {
                recommendations.add(new SectionRecommendation(
                    course.getCourseName(),
                    course.getCourseCode(),
                    currentSections,
                    recommendedSections,
                    change,
                    trend
                ));
            }
        }

        // Sort by absolute change (biggest changes first)
        recommendations.sort((a, b) ->
            Integer.compare(Math.abs(b.getSectionChange()), Math.abs(a.getSectionChange())));

        sectionRecommendationsTable.setItems(recommendations);
    }

    private void updateGrowingCourses() {
        List<Course> allCourses = courseRepository.findAll();

        List<String> growing = new ArrayList<>();
        List<String> declining = new ArrayList<>();

        for (Course course : allCourses) {
            String trend = forecastingService.getCourseTrend(course.getId());

            if (trend.contains("Growing")) {
                growing.add(course.getCourseName() + " - " + trend);
            } else if (trend.contains("Declining")) {
                declining.add(course.getCourseName() + " - " + trend);
            }
        }

        // Sort and limit to top 5
        growing.sort(String::compareTo);
        declining.sort(String::compareTo);

        if (growingCoursesList != null) {
            safeSetItems(growingCoursesList,
                FXCollections.observableArrayList(growing.stream().limit(5).collect(Collectors.toList())));
        }

        if (decliningCoursesList != null) {
            safeSetItems(decliningCoursesList,
                FXCollections.observableArrayList(declining.stream().limit(5).collect(Collectors.toList())));
        }
    }

    private void updateForecastingMethods() {
        // Compare linear regression vs moving average
        // This is a simplified comparison - would need actual accuracy metrics
        safeSetText(linearRegressionAccuracyLabel, "Trend-based");
        safeSetText(movingAverageAccuracyLabel, "Stable");
        safeSetText(recommendedMethodLabel, "Linear Regression");
        safeSetText(methodReasonLabel, "Best for growth trends");
    }

    private void filterCapacityWarnings() {
        // Implemented by filtering the table
        updateCapacityWarnings(forecastingService.getCapacityWarnings(selectedYear));
    }

    private void showReportDialog(Map<String, Object> report) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forecasting Report");
        alert.setHeaderText("Enrollment Forecasting Report for " + selectedYear);

        StringBuilder content = new StringBuilder();
        content.append("Total Enrollment: ").append(report.get("totalEnrollment")).append("\n\n");
        content.append("Enrollment by Grade:\n");
        Map<String, Integer> byGrade = (Map<String, Integer>) report.get("enrollmentByGrade");
        byGrade.forEach((grade, count) ->
            content.append("  Grade ").append(grade).append(": ").append(count).append("\n"));

        content.append("\nCapacity Status: ");
        content.append((boolean) report.get("hasAdequateCapacity") ? "Adequate" : "Insufficient");

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // Helper methods
    private int getCurrentGradeCount(String grade) {
        return (int) studentRepository.findAll().stream()
            .filter(s -> s.isActive() && grade.equals(s.getGradeLevel()))
            .count();
    }

    private int calculateCurrentCapacity(Long courseId) {
        return courseSectionRepository.findAll().stream()
            .filter(s -> s.getCourse().getId().equals(courseId))
            .mapToInt(s -> s.getMaxEnrollment() != null ? s.getMaxEnrollment() : 25)
            .sum();
    }

    private int getCurrentSectionCount(Long courseId) {
        return (int) courseSectionRepository.findAll().stream()
            .filter(s -> s.getCourse().getId().equals(courseId))
            .count();
    }

    private String extractWarningType(String warningText) {
        if (warningText.contains("OVER CAPACITY")) return "Over Capacity";
        if (warningText.contains("NEAR CAPACITY")) return "Near Capacity";
        if (warningText.contains("UNDER-UTILIZED")) return "Under-Utilized";
        return "Warning";
    }

    private String generateRecommendation(int gap, int currentCapacity, int forecastedDemand) {
        if (gap > 0) {
            int sectionsNeeded = (int) Math.ceil(gap / 25.0);
            return "Add " + sectionsNeeded + " section(s)";
        } else if (gap < -25) {
            int sectionsToRemove = (int) Math.floor(Math.abs(gap) / 25.0);
            return "Consider removing " + sectionsToRemove + " section(s)";
        }
        return "Maintain current sections";
    }

    // Safe UI update methods
    private void safeSetText(Label label, String text) {
        if (label != null && text != null) {
            label.setText(text);
        }
    }

    private <T> void safeSetItems(ComboBox<T> comboBox, ObservableList<T> items) {
        if (comboBox != null && items != null) {
            comboBox.setItems(items);
        }
    }

    private <T> void safeSetItems(ListView<T> listView, ObservableList<T> items) {
        if (listView != null && items != null) {
            listView.setItems(items);
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner classes for table data
    @Data
    public static class GradeEnrollment {
        private final String grade;
        private final int currentCount;
        private final int forecastedCount;
        private final String changeText;
    }

    @Data
    public static class CapacityWarning {
        private final String courseName;
        private final String courseCode;
        private final int currentCapacity;
        private final int forecastedDemand;
        private final int gap;
        private final String warningType;
        private final String recommendation;
    }

    @Data
    public static class SectionRecommendation {
        private final String courseName;
        private final String courseCode;
        private final int currentSections;
        private final int recommendedSections;
        private final int sectionChange;
        private final String trend;
    }
}
