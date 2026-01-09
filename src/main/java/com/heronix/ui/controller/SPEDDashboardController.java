package com.heronix.ui.controller;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Plan504;
import com.heronix.service.IEPManagementService;
import com.heronix.service.Plan504Service;
import com.heronix.service.PullOutSchedulingService;
import com.heronix.service.SPEDComplianceService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SPED Dashboard Controller
 *
 * Main dashboard for Special Education management showing compliance metrics,
 * expiring IEPs/504 plans, and services needing attention.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8C - November 21, 2025
 */
@Controller
@Slf4j
public class SPEDDashboardController {

    @Autowired
    private IEPManagementService iepManagementService;

    @Autowired
    private Plan504Service plan504Service;

    @Autowired
    private PullOutSchedulingService pullOutSchedulingService;

    @Autowired
    private SPEDComplianceService complianceService;

    // Metrics
    @FXML private Label activeIEPsLabel;
    @FXML private Label active504Label;
    @FXML private Label needsSchedulingLabel;
    @FXML private Label complianceRateLabel;
    @FXML private ProgressBar complianceProgress;
    @FXML private Label actionItemsCountLabel;

    // Expiring IEPs Table
    @FXML private TableView<IEP> expiringIEPsTable;
    @FXML private TableColumn<IEP, String> iepStudentColumn;
    @FXML private TableColumn<IEP, String> iepNumberColumn;
    @FXML private TableColumn<IEP, String> iepEndDateColumn;
    @FXML private TableColumn<IEP, String> iepDaysRemainingColumn;
    @FXML private TableColumn<IEP, String> iepCaseManagerColumn;
    @FXML private TableColumn<IEP, String> iepActionsColumn;

    // Expiring 504 Plans Table
    @FXML private TableView<Plan504> expiring504Table;
    @FXML private TableColumn<Plan504, String> plan504StudentColumn;
    @FXML private TableColumn<Plan504, String> plan504NumberColumn;
    @FXML private TableColumn<Plan504, String> plan504EndDateColumn;
    @FXML private TableColumn<Plan504, String> plan504DaysRemainingColumn;
    @FXML private TableColumn<Plan504, String> plan504CoordinatorColumn;
    @FXML private TableColumn<Plan504, String> plan504ActionsColumn;

    // Services Below Minutes Table
    @FXML private TableView<IEPService> belowMinutesTable;
    @FXML private TableColumn<IEPService, String> serviceStudentColumn;
    @FXML private TableColumn<IEPService, String> serviceTypeColumn;
    @FXML private TableColumn<IEPService, String> requiredMinutesColumn;
    @FXML private TableColumn<IEPService, String> scheduledMinutesColumn;
    @FXML private TableColumn<IEPService, String> compliancePercentColumn;
    @FXML private TableColumn<IEPService, String> serviceActionsColumn;

    // Statistics Containers
    @FXML private VBox iepStatsContainer;
    @FXML private VBox serviceStatsContainer;
    @FXML private TabPane actionItemsTabs;

    @FXML
    public void initialize() {
        log.info("Initializing SPED Dashboard");
        setupTables();
        loadData();
    }

    private void setupTables() {
        // Setup Expiring IEPs Table
        iepStudentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudent().getFullName()));
        iepNumberColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIepNumber() != null ? data.getValue().getIepNumber() : "Draft"));
        iepEndDateColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getEndDate().toString()));
        iepDaysRemainingColumn.setCellValueFactory(data -> {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), data.getValue().getEndDate());
            return new SimpleStringProperty(String.valueOf(days));
        });
        iepCaseManagerColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCaseManager() != null ? data.getValue().getCaseManager() : "N/A"));
        iepActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            {
                viewBtn.setOnAction(e -> handleViewIEP(getTableRow().getItem()));
                viewBtn.getStyleClass().add("btn-link");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        // Setup Expiring 504 Plans Table
        plan504StudentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudent().getFullName()));
        plan504NumberColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPlanNumber() != null ? data.getValue().getPlanNumber() : "Draft"));
        plan504EndDateColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getEndDate().toString()));
        plan504DaysRemainingColumn.setCellValueFactory(data -> {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), data.getValue().getEndDate());
            return new SimpleStringProperty(String.valueOf(days));
        });
        plan504CoordinatorColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCoordinator() != null ? data.getValue().getCoordinator() : "N/A"));
        plan504ActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            {
                viewBtn.setOnAction(e -> handleView504Plan(getTableRow().getItem()));
                viewBtn.getStyleClass().add("btn-link");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        // Setup Services Below Minutes Table
        serviceStudentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudent().getFullName()));
        serviceTypeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getServiceType().getDisplayName()));
        requiredMinutesColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getMinutesPerWeek())));
        scheduledMinutesColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getScheduledMinutesPerWeek())));
        compliancePercentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.format("%.1f%%", data.getValue().getCompliancePercentage())));
        serviceActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button scheduleBtn = new Button("Schedule");
            {
                scheduleBtn.setOnAction(e -> handleScheduleService(getTableRow().getItem()));
                scheduleBtn.getStyleClass().add("btn-link");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : scheduleBtn);
            }
        });
    }

    private void loadData() {
        try {
            log.info("Loading SPED dashboard data");

            // Load metrics
            SPEDComplianceService.DashboardMetrics metrics = complianceService.getDashboardMetrics();

            activeIEPsLabel.setText(String.valueOf(metrics.totalActiveIEPs));
            active504Label.setText(String.valueOf(metrics.totalActive504Plans));
            needsSchedulingLabel.setText(String.valueOf(metrics.servicesNeedingScheduling));

            double complianceRate = metrics.overallComplianceRate;
            complianceRateLabel.setText(String.format("%.1f%%", complianceRate));
            complianceProgress.setProgress(complianceRate / 100.0);

            // Load expiring IEPs (30 days threshold)
            var expiringIEPs = complianceService.getExpiringIEPs(30);
            expiringIEPsTable.getItems().setAll(expiringIEPs);

            // Load expiring 504 plans
            var expiring504s = complianceService.getExpiring504Plans(30);
            expiring504Table.getItems().setAll(expiring504s);

            // Load services not meeting minutes
            var belowMinutes = complianceService.getServicesNotMeetingMinutes();
            belowMinutesTable.getItems().setAll(belowMinutes);

            // Update action items count
            int totalActionItems = expiringIEPs.size() + expiring504s.size() + belowMinutes.size();
            actionItemsCountLabel.setText(totalActionItems + " items");

            // Load statistics
            loadStatistics();

            log.info("SPED dashboard data loaded successfully");

        } catch (Exception e) {
            log.error("Error loading SPED dashboard data", e);
            showError("Error Loading Data", "Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void loadStatistics() {
        // Load IEP statistics
        iepStatsContainer.getChildren().clear();
        var iepStats = complianceService.getIEPCountByCategory();
        for (var entry : iepStats.entrySet()) {
            Label statLabel = new Label(entry.getKey() + ": " + entry.getValue());
            statLabel.getStyleClass().add("stat-item");
            iepStatsContainer.getChildren().add(statLabel);
        }

        // Load service statistics
        serviceStatsContainer.getChildren().clear();
        var serviceStats = complianceService.getServiceStatusCounts();
        for (var entry : serviceStats.entrySet()) {
            Label statLabel = new Label(entry.getKey() + ": " + entry.getValue());
            statLabel.getStyleClass().add("stat-item");
            serviceStatsContainer.getChildren().add(statLabel);
        }
    }

    // Event Handlers

    @FXML
    private void handleRefresh() {
        log.info("Refreshing SPED dashboard");
        loadData();
    }

    @FXML
    private void handleExportReport() {
        log.info("Exporting SPED dashboard report");
        showInfo("Export Report", "Dashboard report export functionality will be implemented in a future version.");
    }

    @FXML
    private void handleAddIEP() {
        log.info("Adding new IEP");
        showInfo("Add IEP", "IEP creation wizard will be implemented in a future version.");
    }

    @FXML
    private void handleScheduleAllServices() {
        log.info("Scheduling all unscheduled services");
        showInfo("Schedule All Services", "Bulk service scheduling will be implemented in a future version.");
    }

    @FXML
    private void handleFilterChange() {
        log.info("Filter changed on SPED dashboard");
        // Filter logic will be implemented when tables are fully functional
    }

    @FXML
    private void handleGenerateReport() {
        log.info("Generating compliance report");
        showInfo("Generate Report", "Compliance report generation will be implemented in a future version.");
    }

    @FXML
    private void handleViewIEPs() {
        log.info("Viewing all IEPs");
        showInfo("View IEPs", "IEP list view will be implemented in a future version.");
    }

    @FXML
    private void handleView504Plans() {
        log.info("Viewing all 504 plans");
        showInfo("View 504 Plans", "504 Plan list view will be implemented in a future version.");
    }

    @FXML
    private void handleScheduleServices() {
        log.info("Opening service scheduling");
        showInfo("Schedule Services", "Service scheduling wizard will be implemented in a future version.");
    }

    @FXML
    private void handleCreateIEP() {
        log.info("Creating new IEP");
        showInfo("Create IEP", "IEP creation wizard will be implemented in a future version.");
    }

    @FXML
    private void handleCreate504Plan() {
        log.info("Creating new 504 plan");
        showInfo("Create 504 Plan", "504 Plan creation dialog will be implemented in a future version.");
    }

    @FXML
    private void handleSchedulePullOut() {
        log.info("Opening pull-out scheduling");
        showInfo("Schedule Pull-Out", "Pull-out scheduling interface will be implemented in a future version.");
    }

    @FXML
    private void handleViewSchedules() {
        log.info("Viewing all schedules");
        showInfo("View Schedules", "Schedule view will be implemented in a future version.");
    }

    private void handleViewIEP(IEP iep) {
        if (iep == null) return;
        log.info("Viewing IEP: {}", iep.getId());
        showInfo("View IEP", "IEP detail view will be implemented in a future version.");
    }

    private void handleView504Plan(Plan504 plan) {
        if (plan == null) return;
        log.info("Viewing 504 Plan: {}", plan.getId());
        showInfo("View 504 Plan", "504 Plan detail view will be implemented in a future version.");
    }

    private void handleScheduleService(IEPService service) {
        if (service == null) return;
        log.info("Scheduling service: {}", service.getId());

        try {
            pullOutSchedulingService.scheduleService(service.getId());
            showSuccess("Service Scheduled", "Service scheduled successfully!");
            loadData(); // Refresh
        } catch (Exception e) {
            log.error("Error scheduling service", e);
            showError("Scheduling Error", "Failed to schedule service: " + e.getMessage());
        }
    }

    // Utility Methods

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
