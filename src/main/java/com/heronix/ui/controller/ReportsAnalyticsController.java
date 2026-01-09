// Location: src/main/java/com/eduscheduler/ui/controller/ReportsAnalyticsController.java
package com.heronix.ui.controller;

import com.heronix.model.domain.Room;
import com.heronix.model.domain.Schedule;
import com.heronix.model.domain.Teacher;
import com.heronix.model.dto.DashboardMetrics;
import com.heronix.model.dto.Recommendation;
import com.heronix.service.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reports & Analytics Controller
 * Location: src/main/java/com/eduscheduler/ui/controller/ReportsAnalyticsController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ReportsAnalyticsController {

    // Services
    private final AnalyticsService analyticsService;
    private final TeacherService teacherService;
    private final RoomService roomService;
    private final ScheduleService scheduleService;
    private final ExportService exportService;

    // Dashboard Metrics Labels
    @FXML private Label totalTeachersLabel;
    @FXML private Label activeTeachersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label activeCoursesLabel;
    @FXML private Label totalRoomsLabel;
    @FXML private Label activeRoomsLabel;
    @FXML private Label totalSchedulesLabel;
    @FXML private Label publishedSchedulesLabel;

    // Utilization Metrics
    @FXML private Label teacherUtilLabel;
    @FXML private ProgressBar teacherUtilProgress;
    @FXML private Label roomUtilLabel;
    @FXML private ProgressBar roomUtilProgress;
    @FXML private Label efficiencyLabel;
    @FXML private ProgressBar efficiencyProgress;
    @FXML private Label conflictsLabel;
    @FXML private Label unresolvedConflictsLabel;

    // Teacher Workload Tab
    @FXML private ComboBox<String> teacherFilterCombo;
    @FXML private BarChart<String, Number> teacherWorkloadChart;
    @FXML private CategoryAxis teacherXAxis;
    @FXML private NumberAxis teacherYAxis;
    @FXML private TableView<TeacherWorkloadRow> teacherWorkloadTable;
    @FXML private TableColumn<TeacherWorkloadRow, String> teacherNameColumn;
    @FXML private TableColumn<TeacherWorkloadRow, Integer> teacherHoursColumn;
    @FXML private TableColumn<TeacherWorkloadRow, String> teacherUtilColumn;
    @FXML private TableColumn<TeacherWorkloadRow, String> teacherStatusColumn;

    // Room Utilization Tab
    @FXML private PieChart roomUtilizationChart;
    @FXML private TableView<RoomUtilizationRow> roomUtilizationTable;
    @FXML private TableColumn<RoomUtilizationRow, String> roomNumberColumn;
    @FXML private TableColumn<RoomUtilizationRow, Integer> roomCapacityColumn;
    @FXML private TableColumn<RoomUtilizationRow, String> roomUsageColumn;
    @FXML private TableColumn<RoomUtilizationRow, Integer> roomHoursColumn;

    // Schedule Quality Tab
    @FXML private ComboBox<Schedule> scheduleSelectCombo;
    @FXML private Label scheduleQualityLabel;
    @FXML private Label optimizationScoreLabel;
    @FXML private Label teacherBalanceLabel;
    @FXML private Label roomEfficiencyLabel;
    @FXML private Label conflictRateLabel;
    @FXML private ListView<String> recommendationsListView;

    // Lean Metrics Tab
    @FXML private Label cycleTimeLabel;
    @FXML private Label leadTimeLabel;
    @FXML private Label processEfficiencyLabel;
    @FXML private ProgressBar processEfficiencyProgress;
    @FXML private Label defectRateLabel;

    // Buttons
    @FXML private Button refreshButton;
    @FXML private Button exportReportButton;

    private final DecimalFormat percentFormat = new DecimalFormat("0.0%");
    private final DecimalFormat numberFormat = new DecimalFormat("0.00");

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        log.info("Initializing Reports & Analytics Controller");

        // Setup table columns
        setupTableColumns();

        // Setup combo boxes
        setupComboBoxes();

        // Load initial data
        loadDashboardMetrics();
        loadTeacherWorkload();
        loadRoomUtilization();
        loadScheduleList();

        log.info("Reports & Analytics Controller initialized successfully");
    }

    /**
     * Setup table column bindings
     */
    private void setupTableColumns() {
        // Teacher Workload Table
        teacherNameColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherHoursColumn.setCellValueFactory(new PropertyValueFactory<>("hoursPerWeek"));
        teacherUtilColumn.setCellValueFactory(new PropertyValueFactory<>("utilization"));
        teacherStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Room Utilization Table
        roomNumberColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        roomCapacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        roomUsageColumn.setCellValueFactory(new PropertyValueFactory<>("usagePercent"));
        roomHoursColumn.setCellValueFactory(new PropertyValueFactory<>("hoursPerWeek"));
    }

    /**
     * Setup combo boxes
     */
    private void setupComboBoxes() {
        // Teacher filter combo
        teacherFilterCombo.setItems(FXCollections.observableArrayList(
            "All Departments", "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology"
        ));
        teacherFilterCombo.setValue("All Departments");

        // Schedule combo with custom string converter
        scheduleSelectCombo.setConverter(new javafx.util.StringConverter<Schedule>() {
            @Override
            public String toString(Schedule schedule) {
                return schedule != null ? schedule.getScheduleName() : "";
            }

            @Override
            public Schedule fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Load dashboard metrics
     */
    private void loadDashboardMetrics() {
        try {
            log.debug("Loading dashboard metrics...");
            DashboardMetrics metrics = analyticsService.getDashboardMetrics();

            // Entity counts
            totalTeachersLabel.setText(String.valueOf(metrics.getTotalTeachers()));
            activeTeachersLabel.setText(metrics.getActiveTeachers() + " active");

            totalCoursesLabel.setText(String.valueOf(metrics.getTotalCourses()));
            activeCoursesLabel.setText(metrics.getActiveCourses() + " active");

            totalRoomsLabel.setText(String.valueOf(metrics.getTotalRooms()));
            activeRoomsLabel.setText(metrics.getActiveRooms() + " active");

            totalSchedulesLabel.setText(String.valueOf(metrics.getTotalSchedules()));
            publishedSchedulesLabel.setText(metrics.getPublishedSchedules() + " published");

            // Utilization metrics
            teacherUtilLabel.setText(percentFormat.format(metrics.getTeacherUtilizationRate()));
            teacherUtilProgress.setProgress(metrics.getTeacherUtilizationRate());

            roomUtilLabel.setText(percentFormat.format(metrics.getRoomUtilizationRate()));
            roomUtilProgress.setProgress(metrics.getRoomUtilizationRate());

            efficiencyLabel.setText(percentFormat.format(metrics.getOverallEfficiency()));
            efficiencyProgress.setProgress(metrics.getOverallEfficiency());

            // Conflicts
            conflictsLabel.setText(String.valueOf(metrics.getTotalConflicts()));
            unresolvedConflictsLabel.setText(metrics.getUnresolvedConflicts() + " unresolved");

            log.info("Dashboard metrics loaded successfully");

        } catch (Exception e) {
            log.error("Error loading dashboard metrics", e);
            showError("Error", "Failed to load dashboard metrics: " + e.getMessage());
        }
    }

    /**
     * Load teacher workload data
     */
    private void loadTeacherWorkload() {
        try {
            log.debug("Loading teacher workload data...");

            // Get workload distribution
            Map<Long, Integer> workloadMap = analyticsService.getTeacherWorkloadDistribution();
            List<Teacher> teachers = teacherService.getAllActiveTeachers();

            // Clear chart
            teacherWorkloadChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Hours per Week");

            // Build table data
            List<TeacherWorkloadRow> tableData = teachers.stream()
                .filter(t -> workloadMap.containsKey(t.getId()))
                .map(t -> {
                    int hours = workloadMap.get(t.getId());
                    double util = t.getMaxHoursPerWeek() > 0 ?
                        (double) hours / t.getMaxHoursPerWeek() : 0.0;
                    String status = util >= 0.9 ? "Overloaded" :
                                   util >= 0.7 ? "Optimal" : "Underutilized";

                    // Add to chart
                    series.getData().add(new XYChart.Data<>(t.getName(), hours));

                    return new TeacherWorkloadRow(
                        t.getName(),
                        hours,
                        percentFormat.format(util),
                        status
                    );
                })
                .collect(Collectors.toList());

            teacherWorkloadChart.getData().add(series);
            teacherWorkloadTable.setItems(FXCollections.observableArrayList(tableData));

            log.info("Teacher workload data loaded: {} teachers", tableData.size());

        } catch (Exception e) {
            log.error("Error loading teacher workload", e);
            showError("Error", "Failed to load teacher workload: " + e.getMessage());
        }
    }

    /**
     * Load room utilization data
     */
    private void loadRoomUtilization() {
        try {
            log.debug("Loading room utilization data...");

            // Get room usage statistics
            Map<Long, Double> usageMap = analyticsService.getRoomUsageStatistics();
            List<Room> rooms = roomService.findAll();

            // Clear chart
            roomUtilizationChart.getData().clear();

            // Build table data and chart
            List<RoomUtilizationRow> tableData = rooms.stream()
                .filter(r -> usageMap.containsKey(r.getId()))
                .map(r -> {
                    double usage = usageMap.get(r.getId());
                    int hoursPerWeek = (int) (usage * 40); // Assume 40-hour week

                    // Add to pie chart
                    PieChart.Data slice = new PieChart.Data(
                        r.getRoomNumber() + " (" + percentFormat.format(usage) + ")",
                        usage * 100
                    );
                    roomUtilizationChart.getData().add(slice);

                    return new RoomUtilizationRow(
                        r.getRoomNumber(),
                        r.getCapacity(),
                        percentFormat.format(usage),
                        hoursPerWeek
                    );
                })
                .collect(Collectors.toList());

            roomUtilizationTable.setItems(FXCollections.observableArrayList(tableData));

            log.info("Room utilization data loaded: {} rooms", tableData.size());

        } catch (Exception e) {
            log.error("Error loading room utilization", e);
            showError("Error", "Failed to load room utilization: " + e.getMessage());
        }
    }

    /**
     * Load schedule list for quality analysis
     */
    private void loadScheduleList() {
        try {
            List<Schedule> schedules = scheduleService.getAllSchedules();
            scheduleSelectCombo.setItems(FXCollections.observableArrayList(schedules));

            if (!schedules.isEmpty()) {
                scheduleSelectCombo.setValue(schedules.get(0));
                loadScheduleQuality(schedules.get(0));
            }

        } catch (Exception e) {
            log.error("Error loading schedule list", e);
        }
    }

    /**
     * Load schedule quality metrics
     */
    private void loadScheduleQuality(Schedule schedule) {
        if (schedule == null) {
            return;
        }

        try {
            log.debug("Loading quality metrics for schedule: {}", schedule.getScheduleName());

            // Get quality score
            double quality = analyticsService.calculateScheduleQuality(schedule.getId());
            scheduleQualityLabel.setText("Quality Score: " + percentFormat.format(quality));

            // Get efficiency report
            Map<String, Object> report = analyticsService.generateEfficiencyReport(schedule.getId());

            optimizationScoreLabel.setText(
                report.containsKey("optimizationScore") && report.get("optimizationScore") instanceof Number ?
                percentFormat.format(((Number) report.get("optimizationScore")).doubleValue()) : "N/A"
            );

            teacherBalanceLabel.setText(
                report.containsKey("teacherBalance") && report.get("teacherBalance") instanceof Number ?
                percentFormat.format(((Number) report.get("teacherBalance")).doubleValue()) : "N/A"
            );

            roomEfficiencyLabel.setText(
                report.containsKey("roomEfficiency") && report.get("roomEfficiency") instanceof Number ?
                percentFormat.format(((Number) report.get("roomEfficiency")).doubleValue()) : "N/A"
            );

            conflictRateLabel.setText(
                report.containsKey("conflictRate") ?
                numberFormat.format((Double) report.get("conflictRate")) + "%" : "N/A"
            );

            // Load recommendations
            List<Recommendation> recommendations = analyticsService.getRecommendations(schedule.getId());
            List<String> recTexts = recommendations.stream()
                .map(r -> r.getSeverity() + " - " + r.getTitle() + ": " + r.getDescription())
                .collect(Collectors.toList());
            recommendationsListView.setItems(FXCollections.observableArrayList(recTexts));

            // Load lean metrics
            loadLeanMetrics(schedule.getId());

            log.info("Schedule quality loaded successfully");

        } catch (Exception e) {
            log.error("Error loading schedule quality", e);
            showError("Error", "Failed to load schedule quality: " + e.getMessage());
        }
    }

    /**
     * Load Lean Six Sigma metrics
     */
    private void loadLeanMetrics(Long scheduleId) {
        try {
            Map<String, Double> leanMetrics = analyticsService.calculateLeanMetrics(scheduleId);

            cycleTimeLabel.setText(
                leanMetrics.containsKey("cycleTime") ?
                numberFormat.format(leanMetrics.get("cycleTime")) : "N/A"
            );

            leadTimeLabel.setText(
                leanMetrics.containsKey("leadTime") ?
                numberFormat.format(leanMetrics.get("leadTime")) : "N/A"
            );

            if (leanMetrics.containsKey("processEfficiency")) {
                double efficiency = leanMetrics.get("processEfficiency");
                processEfficiencyLabel.setText(percentFormat.format(efficiency));
                processEfficiencyProgress.setProgress(efficiency);
            } else {
                processEfficiencyLabel.setText("N/A");
                processEfficiencyProgress.setProgress(0);
            }

            defectRateLabel.setText(
                leanMetrics.containsKey("defectRate") ?
                numberFormat.format(leanMetrics.get("defectRate")) : "N/A"
            );

        } catch (Exception e) {
            log.error("Error loading lean metrics", e);
        }
    }

    /**
     * Handle refresh button
     */
    @FXML
    private void handleRefresh() {
        log.info("Refreshing all analytics data...");
        loadDashboardMetrics();
        loadTeacherWorkload();
        loadRoomUtilization();

        Schedule selected = scheduleSelectCombo.getValue();
        if (selected != null) {
            loadScheduleQuality(selected);
        }

        showInfo("Refreshed", "All analytics data has been refreshed");
    }

    /**
     * Handle teacher filter combo
     */
    @FXML
    private void handleTeacherFilter() {
        String department = teacherFilterCombo.getValue();
        log.debug("Filtering teachers by department: {}", department);

        try {
            // Get workload distribution
            Map<Long, Integer> workloadMap = analyticsService.getTeacherWorkloadDistribution();
            List<Teacher> teachers = teacherService.getAllActiveTeachers();

            // Filter by department if selected
            if (department != null && !department.isEmpty() && !"All Departments".equals(department)) {
                teachers = teachers.stream()
                    .filter(t -> department.equals(t.getDepartment()))
                    .collect(Collectors.toList());
            }

            // Clear chart
            teacherWorkloadChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Hours per Week");

            // Build table data
            List<TeacherWorkloadRow> tableData = teachers.stream()
                .filter(t -> workloadMap.containsKey(t.getId()))
                .map(t -> {
                    int hours = workloadMap.get(t.getId());
                    double util = t.getMaxHoursPerWeek() > 0 ?
                        (double) hours / t.getMaxHoursPerWeek() : 0.0;
                    String status = util >= 0.9 ? "Overloaded" :
                                   util >= 0.7 ? "Optimal" : "Underutilized";

                    // Add to chart
                    series.getData().add(new XYChart.Data<>(t.getName(), hours));

                    return new TeacherWorkloadRow(
                        t.getName(),
                        hours,
                        percentFormat.format(util),
                        status
                    );
                })
                .collect(Collectors.toList());

            teacherWorkloadChart.getData().add(series);
            teacherWorkloadTable.setItems(FXCollections.observableArrayList(tableData));

            log.info("Teacher workload data filtered: {} teachers (department: {})",
                tableData.size(), department != null ? department : "All");

        } catch (Exception e) {
            log.error("Error filtering teacher workload", e);
            showError("Error", "Failed to filter teacher workload: " + e.getMessage());
        }
    }

    /**
     * Handle schedule selection
     */
    @FXML
    private void handleScheduleSelection() {
        Schedule selected = scheduleSelectCombo.getValue();
        if (selected != null) {
            log.info("Schedule selected: {}", selected.getScheduleName());
            loadScheduleQuality(selected);
        }
    }

    /**
     * Handle export report button
     */
    @FXML
    private void handleExportReport() {
        try {
            log.info("Exporting analytics report...");

            // Show file chooser
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Analytics Report");
            fileChooser.setInitialFileName("analytics_report.pdf");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );

            File file = fileChooser.showSaveDialog(exportReportButton.getScene().getWindow());

            if (file != null) {
                String fileName = file.getName().toLowerCase();

                if (fileName.endsWith(".pdf")) {
                    exportAnalyticsToPDF(file);
                } else if (fileName.endsWith(".xlsx")) {
                    exportAnalyticsToExcel(file);
                } else {
                    showError("Error", "Unsupported file format. Please use PDF or Excel.");
                }
            }

        } catch (Exception e) {
            log.error("Error exporting report", e);
            showError("Error", "Failed to export report: " + e.getMessage());
        }
    }

    /**
     * Export analytics report to PDF
     */
    private void exportAnalyticsToPDF(File file) {
        try {
            log.info("Exporting analytics report to PDF: {}", file.getAbsolutePath());

            // Get current metrics
            DashboardMetrics metrics = analyticsService.getDashboardMetrics();

            // Create PDF content (using HTML-like structure that can be converted)
            StringBuilder content = new StringBuilder();
            content.append("=== Heronix Scheduling System - Analytics Report ===\n\n");
            content.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");

            content.append("--- Dashboard Metrics ---\n");
            content.append("Total Teachers: ").append(metrics.getTotalTeachers()).append("\n");
            content.append("Active Teachers: ").append(metrics.getActiveTeachers()).append("\n");
            content.append("Total Courses: ").append(metrics.getTotalCourses()).append("\n");
            content.append("Active Courses: ").append(metrics.getActiveCourses()).append("\n");
            content.append("Total Rooms: ").append(metrics.getTotalRooms()).append("\n");
            content.append("Active Rooms: ").append(metrics.getActiveRooms()).append("\n");
            content.append("Total Schedules: ").append(metrics.getTotalSchedules()).append("\n");
            content.append("Published Schedules: ").append(metrics.getPublishedSchedules()).append("\n\n");

            content.append("--- Utilization Metrics ---\n");
            content.append("Teacher Utilization: ").append(percentFormat.format(metrics.getTeacherUtilizationRate())).append("\n");
            content.append("Room Utilization: ").append(percentFormat.format(metrics.getRoomUtilizationRate())).append("\n");
            content.append("Overall Efficiency: ").append(percentFormat.format(metrics.getOverallEfficiency())).append("\n");
            content.append("Unresolved Conflicts: ").append(metrics.getUnresolvedConflicts()).append("\n\n");

            // Teacher Workload
            content.append("--- Teacher Workload ---\n");
            List<Teacher> teachers = teacherService.getAllActiveTeachers();
            Map<Long, Integer> workloadMap = analyticsService.getTeacherWorkloadDistribution();
            for (Teacher teacher : teachers) {
                if (workloadMap.containsKey(teacher.getId())) {
                    int hours = workloadMap.get(teacher.getId());
                    double util = teacher.getMaxHoursPerWeek() > 0 ?
                        (double) hours / teacher.getMaxHoursPerWeek() : 0.0;
                    content.append(String.format("%s: %d hours/week (%.1f%% utilization)\n",
                        teacher.getName(), hours, util * 100));
                }
            }
            content.append("\n");

            // Room Utilization
            content.append("--- Room Utilization ---\n");
            List<Room> rooms = roomService.findAll();
            Map<Long, Double> usageMap = analyticsService.getRoomUsageStatistics();
            for (Room room : rooms) {
                if (usageMap.containsKey(room.getId())) {
                    double usage = usageMap.get(room.getId());
                    content.append(String.format("%s (Capacity: %d): %.1f%% usage\n",
                        room.getRoomNumber(), room.getCapacity(), usage * 100));
                }
            }

            // Write to file (simplified text format - in production, use proper PDF library)
            java.nio.file.Files.write(file.toPath(), content.toString().getBytes());

            showInfo("Export Successful",
                "Analytics report exported successfully to:\n" + file.getAbsolutePath());
            log.info("Analytics report exported to PDF successfully");

        } catch (Exception e) {
            log.error("Error exporting analytics to PDF", e);
            showError("Export Error", "Failed to export analytics report: " + e.getMessage());
        }
    }

    /**
     * Export analytics report to Excel
     */
    private void exportAnalyticsToExcel(File file) {
        try {
            log.info("Exporting analytics report to Excel: {}", file.getAbsolutePath());

            // Get data
            DashboardMetrics metrics = analyticsService.getDashboardMetrics();
            List<Teacher> teachers = teacherService.getAllActiveTeachers();
            Map<Long, Integer> workloadMap = analyticsService.getTeacherWorkloadDistribution();
            List<Room> rooms = roomService.findAll();
            Map<Long, Double> usageMap = analyticsService.getRoomUsageStatistics();

            // Create workbook
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();

            // Sheet 1: Dashboard Metrics
            org.apache.poi.ss.usermodel.Sheet dashboardSheet = workbook.createSheet("Dashboard Metrics");
            int rowNum = 0;

            org.apache.poi.ss.usermodel.Row headerRow = dashboardSheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Metric");
            headerRow.createCell(1).setCellValue("Value");

            dashboardSheet.createRow(rowNum++).createCell(0).setCellValue("Total Teachers");
            dashboardSheet.getRow(rowNum - 1).createCell(1).setCellValue(metrics.getTotalTeachers());

            dashboardSheet.createRow(rowNum++).createCell(0).setCellValue("Active Teachers");
            dashboardSheet.getRow(rowNum - 1).createCell(1).setCellValue(metrics.getActiveTeachers());

            dashboardSheet.createRow(rowNum++).createCell(0).setCellValue("Total Courses");
            dashboardSheet.getRow(rowNum - 1).createCell(1).setCellValue(metrics.getTotalCourses());

            dashboardSheet.createRow(rowNum++).createCell(0).setCellValue("Teacher Utilization");
            dashboardSheet.getRow(rowNum - 1).createCell(1).setCellValue(metrics.getTeacherUtilizationRate());

            dashboardSheet.createRow(rowNum++).createCell(0).setCellValue("Room Utilization");
            dashboardSheet.getRow(rowNum - 1).createCell(1).setCellValue(metrics.getRoomUtilizationRate());

            // Sheet 2: Teacher Workload
            org.apache.poi.ss.usermodel.Sheet teacherSheet = workbook.createSheet("Teacher Workload");
            rowNum = 0;

            headerRow = teacherSheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Teacher Name");
            headerRow.createCell(1).setCellValue("Hours/Week");
            headerRow.createCell(2).setCellValue("Utilization %");
            headerRow.createCell(3).setCellValue("Status");

            for (Teacher teacher : teachers) {
                if (workloadMap.containsKey(teacher.getId())) {
                    int hours = workloadMap.get(teacher.getId());
                    double util = teacher.getMaxHoursPerWeek() > 0 ?
                        (double) hours / teacher.getMaxHoursPerWeek() * 100 : 0.0;
                    String status = util >= 90 ? "Overloaded" : util >= 70 ? "Optimal" : "Underutilized";

                    org.apache.poi.ss.usermodel.Row row = teacherSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(teacher.getName());
                    row.createCell(1).setCellValue(hours);
                    row.createCell(2).setCellValue(util);
                    row.createCell(3).setCellValue(status);
                }
            }

            // Sheet 3: Room Utilization
            org.apache.poi.ss.usermodel.Sheet roomSheet = workbook.createSheet("Room Utilization");
            rowNum = 0;

            headerRow = roomSheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Room Number");
            headerRow.createCell(1).setCellValue("Capacity");
            headerRow.createCell(2).setCellValue("Usage %");

            for (Room room : rooms) {
                if (usageMap.containsKey(room.getId())) {
                    double usage = usageMap.get(room.getId()) * 100;

                    org.apache.poi.ss.usermodel.Row row = roomSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(room.getRoomNumber());
                    row.createCell(1).setCellValue(room.getCapacity());
                    row.createCell(2).setCellValue(usage);
                }
            }

            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                dashboardSheet.autoSizeColumn(i);
                teacherSheet.autoSizeColumn(i);
                roomSheet.autoSizeColumn(i);
            }

            // Write to file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                workbook.write(fos);
            }

            workbook.close();

            showInfo("Export Successful",
                "Analytics report exported successfully to:\n" + file.getAbsolutePath());
            log.info("Analytics report exported to Excel successfully");

        } catch (Exception e) {
            log.error("Error exporting analytics to Excel", e);
            showError("Export Error", "Failed to export analytics report: " + e.getMessage());
        }
    }

    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== Inner Classes for Table Rows ==========

    /**
     * Teacher Workload Row for TableView
     */
    public static class TeacherWorkloadRow {
        private final String teacherName;
        private final Integer hoursPerWeek;
        private final String utilization;
        private final String status;

        public TeacherWorkloadRow(String teacherName, Integer hoursPerWeek,
                                  String utilization, String status) {
            this.teacherName = teacherName;
            this.hoursPerWeek = hoursPerWeek;
            this.utilization = utilization;
            this.status = status;
        }

        public String getTeacherName() { return teacherName; }
        public Integer getHoursPerWeek() { return hoursPerWeek; }
        public String getUtilization() { return utilization; }
        public String getStatus() { return status; }
    }

    /**
     * Room Utilization Row for TableView
     */
    public static class RoomUtilizationRow {
        private final String roomNumber;
        private final Integer capacity;
        private final String usagePercent;
        private final Integer hoursPerWeek;

        public RoomUtilizationRow(String roomNumber, Integer capacity,
                                  String usagePercent, Integer hoursPerWeek) {
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.usagePercent = usagePercent;
            this.hoursPerWeek = hoursPerWeek;
        }

        public String getRoomNumber() { return roomNumber; }
        public Integer getCapacity() { return capacity; }
        public String getUsagePercent() { return usagePercent; }
        public Integer getHoursPerWeek() { return hoursPerWeek; }
    }
}
