package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.model.enums.IEPStatus;
import com.heronix.service.IEPManagementService;
import com.heronix.service.Plan504Service;
import com.heronix.service.PullOutSchedulingService;
import com.heronix.service.SPEDComplianceService;
import com.heronix.service.SPEDExportService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SPED Scheduling Dashboard Controller
 *
 * Comprehensive dashboard for managing IEP services and special education scheduling.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 9 - November 21, 2025
 */
@Controller
@Slf4j
public class SPEDSchedulingDashboardController {

    @Autowired
    private IEPManagementService iepManagementService;

    @Autowired
    private Plan504Service plan504Service;

    @Autowired
    private PullOutSchedulingService pullOutSchedulingService;

    @Autowired
    private SPEDComplianceService complianceService;

    @Autowired
    private SPEDExportService spedExportService;

    // Header buttons
    @FXML private Button refreshButton;
    @FXML private Button generateReportButton;

    // Quick Stats Labels
    @FXML private Label totalIEPsLabel;
    @FXML private Label servicesScheduledLabel;
    @FXML private Label complianceRateLabel;
    @FXML private Label conflictsLabel;
    @FXML private Label unscheduledLabel;

    // Main Tab Pane
    @FXML private TabPane mainTabPane;

    // Students Tab Controls
    @FXML private TextField studentSearchField;
    @FXML private ComboBox<String> gradeFilterCombo;
    @FXML private ComboBox<String> disabilityFilterCombo;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> studentIdColumn;
    @FXML private TableColumn<Student, String> studentNameColumn;
    @FXML private TableColumn<Student, String> studentGradeColumn;
    @FXML private TableColumn<Student, String> iepStatusColumn;
    @FXML private TableColumn<Student, String> disabilityColumn;
    @FXML private TableColumn<Student, String> servicesCountColumn;
    @FXML private TableColumn<Student, String> complianceColumn;
    @FXML private TableColumn<Student, String> caseManagerColumn;
    @FXML private TableColumn<Student, Void> studentActionsColumn;

    // Student Details Panel
    @FXML private VBox studentDetailsPanel;
    @FXML private Label iepEffectiveDateLabel;
    @FXML private Label iepExpirationDateLabel;
    @FXML private Label annualReviewDateLabel;
    @FXML private Label lreSettingLabel;

    // Student Services Table
    @FXML private TableView<IEPService> studentServicesTable;
    @FXML private TableColumn<IEPService, String> serviceTypeColumn;
    @FXML private TableColumn<IEPService, String> providerColumn;
    @FXML private TableColumn<IEPService, String> minutesPerWeekColumn;
    @FXML private TableColumn<IEPService, String> scheduledStatusColumn;
    @FXML private TableColumn<IEPService, String> serviceComplianceColumn;
    @FXML private TableColumn<IEPService, Void> serviceActionsColumn;

    // Service Scheduling Tab
    @FXML private ComboBox<String> serviceTypeFilterCombo;
    @FXML private ComboBox<String> providerFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<IEPService> servicesTable;
    @FXML private TableColumn<IEPService, String> serviceStudentColumn;
    @FXML private TableColumn<IEPService, String> serviceColumn;
    @FXML private TableColumn<IEPService, String> serviceProviderColumn;
    @FXML private TableColumn<IEPService, String> minPerSessionColumn;
    @FXML private TableColumn<IEPService, String> sessionsPerWeekColumn;
    @FXML private TableColumn<IEPService, String> totalMinWeekColumn;
    @FXML private TableColumn<IEPService, String> scheduleStatusColumn;
    @FXML private TableColumn<IEPService, Void> scheduleActionsColumn;

    // Pull-Out Schedules Tab
    @FXML private ComboBox<String> dayFilterCombo;
    @FXML private TextField pullOutStudentSearch;
    @FXML private ComboBox<String> impactFilterCombo;
    @FXML private TableView<PullOutSchedule> pullOutSchedulesTable;
    @FXML private TableColumn<PullOutSchedule, String> pullOutStudentColumn;
    @FXML private TableColumn<PullOutSchedule, String> pullOutServiceColumn;
    @FXML private TableColumn<PullOutSchedule, String> pullOutProviderColumn;
    @FXML private TableColumn<PullOutSchedule, String> pullOutDayColumn;
    @FXML private TableColumn<PullOutSchedule, String> pullOutTimeColumn;
    @FXML private TableColumn<PullOutSchedule, String> pullOutDurationColumn;
    @FXML private TableColumn<PullOutSchedule, String> missedClassColumn;
    @FXML private TableColumn<PullOutSchedule, String> academicImpactColumn;
    @FXML private TableColumn<PullOutSchedule, String> pullOutComplianceColumn;
    @FXML private TableColumn<PullOutSchedule, Void> pullOutActionsColumn;

    // Provider Workload Tab
    @FXML private ComboBox<String> providerServiceTypeFilter;
    @FXML private TableView<Teacher> providersTable;
    @FXML private TableColumn<Teacher, String> providerNameColumn;
    @FXML private TableColumn<Teacher, String> certificationColumn;
    @FXML private TableColumn<Teacher, String> studentsServedColumn;
    @FXML private TableColumn<Teacher, String> providerMinutesColumn;
    @FXML private TableColumn<Teacher, String> providerSessionsColumn;
    @FXML private TableColumn<Teacher, String> workloadStatusColumn;
    @FXML private TableColumn<Teacher, Void> providerActionsColumn;

    // Compliance Tab
    @FXML private Label fullyCompliantLabel;
    @FXML private Label partiallyCompliantLabel;
    @FXML private Label nonCompliantLabel;
    @FXML private Label dueForRenewalLabel;
    @FXML private TableView<ComplianceAlert> complianceAlertsTable;
    @FXML private TableColumn<ComplianceAlert, String> severityColumn;
    @FXML private TableColumn<ComplianceAlert, String> alertStudentColumn;
    @FXML private TableColumn<ComplianceAlert, String> alertMessageColumn;
    @FXML private TableColumn<ComplianceAlert, String> alertComplianceColumn;
    @FXML private TableColumn<ComplianceAlert, Void> alertActionsColumn;

    // Bottom Action Bar
    @FXML private Label statusLabel;
    @FXML private Button scheduleServiceButton;
    @FXML private Button viewIEPButton;

    // Data collections
    private ObservableList<Student> studentsList = FXCollections.observableArrayList();
    private ObservableList<IEPService> servicesList = FXCollections.observableArrayList();
    private ObservableList<PullOutSchedule> schedulesList = FXCollections.observableArrayList();
    private ObservableList<Teacher> providersList = FXCollections.observableArrayList();

    private Student selectedStudent;

    @FXML
    public void initialize() {
        log.info("Initializing SPED Scheduling Dashboard");
        setupFilters();
        setupStudentsTable();
        setupServicesTable();
        setupPullOutSchedulesTable();
        setupProvidersTable();
        setupComplianceAlertsTable();
        setupSelectionListeners();
        loadData();
    }

    private void setupFilters() {
        // Grade filter
        gradeFilterCombo.getItems().addAll("All Grades", "K", "1", "2", "3", "4", "5",
                "6", "7", "8", "9", "10", "11", "12");
        gradeFilterCombo.setValue("All Grades");

        // Disability filter
        disabilityFilterCombo.getItems().addAll("All", "Autism", "Learning Disability",
                "Speech/Language", "Emotional Disturbance", "Other Health Impairment",
                "Intellectual Disability", "Multiple Disabilities");
        disabilityFilterCombo.setValue("All");

        // Service type filter
        serviceTypeFilterCombo.getItems().addAll("All Services", "Speech Therapy",
                "Occupational Therapy", "Physical Therapy", "Counseling",
                "Special Instruction", "Paraprofessional Support");
        serviceTypeFilterCombo.setValue("All Services");

        // Provider filter
        providerFilterCombo.getItems().add("All Providers");
        providerFilterCombo.setValue("All Providers");

        // Status filter
        statusFilterCombo.getItems().addAll("All Statuses", "Scheduled", "Unscheduled",
                "Partially Scheduled");
        statusFilterCombo.setValue("All Statuses");

        // Day filter
        dayFilterCombo.getItems().add("All Days");
        for (DayOfWeek day : DayOfWeek.values()) {
            dayFilterCombo.getItems().add(day.toString());
        }
        dayFilterCombo.setValue("All Days");

        // Impact filter
        impactFilterCombo.getItems().addAll("All Impacts", "Low", "Medium", "High");
        impactFilterCombo.setValue("All Impacts");

        // Provider service type filter
        providerServiceTypeFilter.getItems().addAll("All Types", "Speech Therapist",
                "Occupational Therapist", "Physical Therapist", "Counselor",
                "Special Education Teacher", "Paraprofessional");
        providerServiceTypeFilter.setValue("All Types");
    }

    private void setupStudentsTable() {
        studentIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStudentId()));

        studentNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));

        // Use getGradeLevel() instead of getGrade()
        studentGradeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getGradeLevel() != null ?
                        data.getValue().getGradeLevel() : "N/A"));

        iepStatusColumn.setCellValueFactory(data -> {
            IEP iep = getActiveIEP(data.getValue());
            return new SimpleStringProperty(iep != null ? iep.getStatus().toString() : "No IEP");
        });

        disabilityColumn.setCellValueFactory(data -> {
            IEP iep = getActiveIEP(data.getValue());
            return new SimpleStringProperty(iep != null && iep.getPrimaryDisability() != null ?
                    iep.getPrimaryDisability() : "N/A");
        });

        servicesCountColumn.setCellValueFactory(data -> {
            IEP iep = getActiveIEP(data.getValue());
            int count = iep != null && iep.getServices() != null ? iep.getServices().size() : 0;
            return new SimpleStringProperty(String.valueOf(count));
        });

        complianceColumn.setCellValueFactory(data -> {
            IEP iep = getActiveIEP(data.getValue());
            if (iep != null) {
                double compliance = calculateComplianceForIEP(iep);
                return new SimpleStringProperty(String.format("%.1f%%", compliance));
            }
            return new SimpleStringProperty("N/A");
        });

        caseManagerColumn.setCellValueFactory(data -> {
            IEP iep = getActiveIEP(data.getValue());
            return new SimpleStringProperty(iep != null && iep.getCaseManager() != null ?
                    iep.getCaseManager() : "N/A");
        });

        setupStudentActionsColumn();
        studentsTable.setItems(studentsList);
    }

    private void setupStudentActionsColumn() {
        studentActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button scheduleBtn = new Button("Schedule");

            {
                viewBtn.getStyleClass().add("btn-link");
                scheduleBtn.getStyleClass().add("btn-link");
                viewBtn.setOnAction(e -> handleViewStudentDetails(getTableRow().getItem()));
                scheduleBtn.setOnAction(e -> handleScheduleStudentServices(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5, viewBtn, scheduleBtn);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void setupServicesTable() {
        serviceStudentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStudent() != null ?
                        data.getValue().getStudent().getFullName() : "N/A"));

        serviceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getServiceType() != null ?
                        data.getValue().getServiceType().getDisplayName() : "N/A"));

        // Use getAssignedStaff() instead of getProvider()
        serviceProviderColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAssignedStaff() != null ?
                        data.getValue().getAssignedStaff().getName() : "Unassigned"));

        // Use getSessionDurationMinutes() instead of getMinutesPerSession()
        minPerSessionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getSessionDurationMinutes())));

        // Calculate sessions per week from frequency
        sessionsPerWeekColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFrequency() != null ?
                        data.getValue().getFrequency() : "N/A"));

        totalMinWeekColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getMinutesPerWeek())));

        scheduleStatusColumn.setCellValueFactory(data -> {
            int scheduled = data.getValue().getScheduledMinutesPerWeek();
            int required = data.getValue().getMinutesPerWeek();
            if (scheduled >= required) {
                return new SimpleStringProperty("Fully Scheduled");
            } else if (scheduled > 0) {
                return new SimpleStringProperty("Partial");
            }
            return new SimpleStringProperty("Unscheduled");
        });

        setupScheduleActionsColumn();
        servicesTable.setItems(servicesList);
    }

    private void setupScheduleActionsColumn() {
        scheduleActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button scheduleBtn = new Button("Schedule");

            {
                scheduleBtn.getStyleClass().add("btn-primary");
                scheduleBtn.setOnAction(e -> handleScheduleService(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : scheduleBtn);
            }
        });
    }

    private void setupPullOutSchedulesTable() {
        pullOutStudentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStudent() != null ?
                        data.getValue().getStudent().getFullName() : "N/A"));

        // Use getIepService().getServiceType() instead of getService()
        pullOutServiceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getIepService() != null ?
                        data.getValue().getIepService().getServiceType().getDisplayName() : "N/A"));

        // Use getStaff() instead of getAssignedStaff()
        pullOutProviderColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStaff() != null ?
                        data.getValue().getStaff().getName() : "Unassigned"));

        pullOutDayColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDayOfWeek() != null ?
                        data.getValue().getDayOfWeek() : "N/A"));

        pullOutTimeColumn.setCellValueFactory(data -> {
            var schedule = data.getValue();
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                return new SimpleStringProperty(schedule.getStartTime().toString() + " - " +
                        schedule.getEndTime().toString());
            }
            return new SimpleStringProperty("N/A");
        });

        pullOutDurationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDurationMinutes() + " min"));

        // Show location instead of missed class
        missedClassColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLocationDisplay()));

        // Show status instead of academic impact
        academicImpactColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus() != null ?
                        data.getValue().getStatus().toString() : "Unknown"));

        // Calculate compliance from service
        pullOutComplianceColumn.setCellValueFactory(data -> {
            IEPService service = data.getValue().getIepService();
            if (service != null) {
                return new SimpleStringProperty(String.format("%.1f%%", service.getCompliancePercentage()));
            }
            return new SimpleStringProperty("N/A");
        });

        setupPullOutActionsColumn();
        pullOutSchedulesTable.setItems(schedulesList);
    }

    private void setupPullOutActionsColumn() {
        pullOutActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.getStyleClass().add("btn-link");
                deleteBtn.getStyleClass().addAll("btn-link", "btn-danger");
                editBtn.setOnAction(e -> handleEditSchedule(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDeleteSchedule(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5, editBtn, deleteBtn);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void setupProvidersTable() {
        providerNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        // Use department or specialization instead of certification
        certificationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDepartment() != null ?
                        data.getValue().getDepartment() : "N/A"));

        studentsServedColumn.setCellValueFactory(data -> {
            Teacher provider = data.getValue();
            long count = schedulesList.stream()
                    .filter(s -> s.getStaff() != null && s.getStaff().equals(provider))
                    .map(s -> s.getStudent())
                    .filter(s -> s != null)
                    .distinct()
                    .count();
            return new SimpleStringProperty(String.valueOf(count));
        });

        providerMinutesColumn.setCellValueFactory(data -> {
            Teacher provider = data.getValue();
            int totalMinutes = schedulesList.stream()
                    .filter(s -> s.getStaff() != null && s.getStaff().equals(provider))
                    .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                    .sum();
            return new SimpleStringProperty(String.valueOf(totalMinutes));
        });

        providerSessionsColumn.setCellValueFactory(data -> {
            Teacher provider = data.getValue();
            long count = schedulesList.stream()
                    .filter(s -> s.getStaff() != null && s.getStaff().equals(provider))
                    .count();
            return new SimpleStringProperty(String.valueOf(count));
        });

        workloadStatusColumn.setCellValueFactory(data -> {
            Teacher provider = data.getValue();
            int totalMinutes = schedulesList.stream()
                    .filter(s -> s.getStaff() != null && s.getStaff().equals(provider))
                    .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                    .sum();
            // Calculate average workload across all providers
            int avgMinutes = providersList.isEmpty() ? 0 : schedulesList.stream()
                    .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                    .sum() / Math.max(1, providersList.size());

            if (totalMinutes > avgMinutes * 1.3) {
                return new SimpleStringProperty("Overloaded");
            } else if (totalMinutes > avgMinutes * 1.1) {
                return new SimpleStringProperty("High");
            } else if (totalMinutes < avgMinutes * 0.7) {
                return new SimpleStringProperty("Low");
            }
            return new SimpleStringProperty("Normal");
        });

        providersTable.setItems(providersList);
    }

    private void setupComplianceAlertsTable() {
        if (complianceAlertsTable != null) {
            complianceAlertsTable.setPlaceholder(new Label("No compliance alerts"));
        }
    }

    private void setupSelectionListeners() {
        studentsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    selectedStudent = newVal;
                    updateButtonStates();
                    if (newVal != null) {
                        showStudentDetails(newVal);
                    } else {
                        hideStudentDetails();
                    }
                });

        servicesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateButtonStates());
    }

    private void updateButtonStates() {
        boolean hasStudentSelected = selectedStudent != null;
        boolean hasServiceSelected = servicesTable.getSelectionModel().getSelectedItem() != null;

        if (viewIEPButton != null) {
            viewIEPButton.setDisable(!hasStudentSelected);
        }
        if (scheduleServiceButton != null) {
            scheduleServiceButton.setDisable(!hasServiceSelected);
        }
    }

    private void loadData() {
        try {
            log.info("Loading SPED Scheduling Dashboard data");
            statusLabel.setText("Loading...");

            // Load metrics
            SPEDComplianceService.DashboardMetrics metrics = complianceService.getDashboardMetrics();
            updateMetrics(metrics);

            // Load students with IEPs using findAllActiveIEPs()
            List<IEP> activeIEPs = iepManagementService.findAllActiveIEPs();
            List<Student> studentsWithIEPs = activeIEPs.stream()
                    .map(IEP::getStudent)
                    .distinct()
                    .collect(Collectors.toList());
            studentsList.setAll(studentsWithIEPs);

            // Load all services
            List<IEPService> allServices = activeIEPs.stream()
                    .flatMap(iep -> iep.getServices().stream())
                    .collect(Collectors.toList());
            servicesList.setAll(allServices);

            // Load schedules using findAllActiveSchedules()
            List<PullOutSchedule> allSchedules = pullOutSchedulingService.findAllActiveSchedules();
            schedulesList.setAll(allSchedules);

            // Load compliance stats
            loadComplianceStats();

            statusLabel.setText("Ready - " + studentsWithIEPs.size() + " students with IEPs");
            log.info("SPED Scheduling Dashboard data loaded successfully");

        } catch (Exception e) {
            log.error("Error loading SPED Scheduling Dashboard data", e);
            statusLabel.setText("Error loading data");
            showError("Load Error", "Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void updateMetrics(SPEDComplianceService.DashboardMetrics metrics) {
        totalIEPsLabel.setText(String.valueOf(metrics.totalActiveIEPs));
        servicesScheduledLabel.setText(String.valueOf(
                metrics.totalActiveIEPs * 2 - metrics.servicesNeedingScheduling));
        complianceRateLabel.setText(String.format("%.0f%%", metrics.overallComplianceRate));
        conflictsLabel.setText("0");
        unscheduledLabel.setText(String.valueOf(metrics.servicesNeedingScheduling));
    }

    private void loadComplianceStats() {
        try {
            var expiringIEPs = complianceService.getExpiringIEPs(30);

            int fullyCompliant = (int) servicesList.stream()
                    .filter(s -> s.getCompliancePercentage() >= 100).count();
            int partiallyCompliant = (int) servicesList.stream()
                    .filter(s -> s.getCompliancePercentage() > 0 && s.getCompliancePercentage() < 100).count();
            int nonCompliant = (int) servicesList.stream()
                    .filter(s -> s.getCompliancePercentage() == 0).count();

            if (fullyCompliantLabel != null) fullyCompliantLabel.setText(String.valueOf(fullyCompliant));
            if (partiallyCompliantLabel != null) partiallyCompliantLabel.setText(String.valueOf(partiallyCompliant));
            if (nonCompliantLabel != null) nonCompliantLabel.setText(String.valueOf(nonCompliant));
            if (dueForRenewalLabel != null) dueForRenewalLabel.setText(String.valueOf(expiringIEPs.size()));

        } catch (Exception e) {
            log.error("Error loading compliance stats", e);
        }
    }

    private IEP getActiveIEP(Student student) {
        if (student == null || student.getIeps() == null) return null;
        return student.getIeps().stream()
                .filter(iep -> iep.getStatus() == IEPStatus.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    private double calculateComplianceForIEP(IEP iep) {
        if (iep == null || iep.getServices() == null || iep.getServices().isEmpty()) {
            return 0.0;
        }
        return iep.getServices().stream()
                .mapToDouble(IEPService::getCompliancePercentage)
                .average()
                .orElse(0.0);
    }

    private void showStudentDetails(Student student) {
        if (studentDetailsPanel != null) {
            studentDetailsPanel.setVisible(true);

            IEP iep = getActiveIEP(student);
            if (iep != null) {
                if (iepEffectiveDateLabel != null)
                    iepEffectiveDateLabel.setText(iep.getStartDate() != null ? iep.getStartDate().toString() : "N/A");
                if (iepExpirationDateLabel != null)
                    iepExpirationDateLabel.setText(iep.getEndDate() != null ? iep.getEndDate().toString() : "N/A");
                if (annualReviewDateLabel != null)
                    annualReviewDateLabel.setText(iep.getNextReviewDate() != null ? iep.getNextReviewDate().toString() : "N/A");
                if (lreSettingLabel != null)
                    lreSettingLabel.setText("General Education"); // Default

                if (studentServicesTable != null && iep.getServices() != null) {
                    studentServicesTable.getItems().setAll(iep.getServices());
                }
            }
        }
    }

    private void hideStudentDetails() {
        if (studentDetailsPanel != null) {
            studentDetailsPanel.setVisible(false);
        }
    }

    // Event Handlers

    @FXML
    private void handleRefresh() {
        log.info("Refreshing SPED Scheduling Dashboard");
        loadData();
    }

    @FXML
    private void handleGenerateReport() {
        log.info("Generating SPED compliance report");

        // Show format selection dialog
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Excel", "Excel", "Text Report");
        dialog.setTitle("Generate Report");
        dialog.setHeaderText("Select Report Format");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            statusLabel.setText("Generating report...");

            new Thread(() -> {
                try {
                    java.io.File reportFile;
                    if ("Excel".equals(format)) {
                        reportFile = spedExportService.exportComplianceReportExcel();
                    } else {
                        reportFile = spedExportService.exportComplianceReportPdf();
                    }

                    final java.io.File finalFile = reportFile;
                    Platform.runLater(() -> {
                        statusLabel.setText("Ready");
                        showSuccess("Report Generated",
                            "Report saved to:\n" + finalFile.getAbsolutePath());

                        // Offer to open the file
                        Alert openAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        openAlert.setTitle("Open Report");
                        openAlert.setHeaderText("Report generated successfully");
                        openAlert.setContentText("Would you like to open the report?");
                        openAlert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                try {
                                    java.awt.Desktop.getDesktop().open(finalFile);
                                } catch (Exception e) {
                                    log.error("Error opening report", e);
                                }
                            }
                        });
                    });
                } catch (Exception e) {
                    log.error("Error generating report", e);
                    Platform.runLater(() -> {
                        statusLabel.setText("Error generating report");
                        showError("Report Error", "Failed to generate report: " + e.getMessage());
                    });
                }
            }).start();
        });
    }

    @FXML
    private void handleClearFilters() {
        gradeFilterCombo.setValue("All Grades");
        disabilityFilterCombo.setValue("All");
        studentSearchField.clear();
        loadData();
    }

    @FXML
    private void handleScheduleAll() {
        log.info("Scheduling all unscheduled services");

        // Find unscheduled services
        List<IEPService> unscheduled = servicesList.stream()
            .filter(s -> s.getScheduledMinutesPerWeek() < s.getMinutesPerWeek())
            .toList();

        if (unscheduled.isEmpty()) {
            showInfo("All Scheduled", "All services are already fully scheduled.");
            return;
        }

        // Check for services without assigned staff
        long noStaffCount = unscheduled.stream()
            .filter(s -> s.getAssignedStaff() == null)
            .count();

        String warningText = "This will attempt to automatically schedule all unscheduled services. Continue?";
        if (noStaffCount > 0) {
            warningText = String.format(
                "Warning: %d of %d services have no assigned staff and will be skipped.\n\n%s",
                noStaffCount, unscheduled.size(), warningText);
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Schedule All Services");
        confirm.setHeaderText("Batch Schedule " + unscheduled.size() + " Services");
        confirm.setContentText(warningText);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                statusLabel.setText("Scheduling services...");

                new Thread(() -> {
                    int scheduled = 0;
                    int skippedNoStaff = 0;
                    int failedConflict = 0;
                    StringBuilder failureDetails = new StringBuilder();

                    for (IEPService service : unscheduled) {
                        // Skip services without assigned staff
                        if (service.getAssignedStaff() == null) {
                            skippedNoStaff++;
                            continue;
                        }

                        try {
                            pullOutSchedulingService.scheduleService(service.getId());
                            scheduled++;
                        } catch (Exception e) {
                            log.warn("Failed to schedule service {}: {}", service.getId(), e.getMessage());
                            failedConflict++;
                            if (failureDetails.length() < 500) { // Limit message size
                                failureDetails.append("- ").append(service.getStudent() != null ?
                                    service.getStudent().getFullName() : "Unknown").append(": ")
                                    .append(e.getMessage()).append("\n");
                            }
                        }
                    }

                    final int finalScheduled = scheduled;
                    final int finalSkippedNoStaff = skippedNoStaff;
                    final int finalFailedConflict = failedConflict;
                    final String details = failureDetails.toString();

                    Platform.runLater(() -> {
                        statusLabel.setText("Ready");
                        StringBuilder message = new StringBuilder();
                        message.append(String.format("Scheduled: %d services\n", finalScheduled));
                        if (finalSkippedNoStaff > 0) {
                            message.append(String.format("Skipped (no staff): %d services\n", finalSkippedNoStaff));
                        }
                        if (finalFailedConflict > 0) {
                            message.append(String.format("Failed (conflicts): %d services\n", finalFailedConflict));
                        }

                        if (finalScheduled > 0) {
                            if (finalFailedConflict > 0 || finalSkippedNoStaff > 0) {
                                showInfo("Batch Scheduling Partial", message.toString());
                            } else {
                                showSuccess("Batch Scheduling Complete", message.toString());
                            }
                            loadData();
                        } else if (finalSkippedNoStaff == unscheduled.size()) {
                            showError("Scheduling Failed",
                                "All services are missing assigned staff.\nPlease assign staff to services before scheduling.");
                        } else {
                            showError("Scheduling Failed",
                                "Could not schedule any services.\n\n" + details);
                        }
                    });
                }).start();
            }
        });
    }

    @FXML
    private void handleExportPullOutSchedule() {
        log.info("Exporting pull-out schedule");

        if (schedulesList.isEmpty()) {
            showInfo("No Data", "There are no schedules to export.");
            return;
        }

        // Show format selection dialog
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Excel", "Excel", "CSV");
        dialog.setTitle("Export Schedules");
        dialog.setHeaderText("Select Export Format");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            statusLabel.setText("Exporting schedules...");

            new Thread(() -> {
                try {
                    java.io.File exportFile;
                    if ("Excel".equals(format)) {
                        exportFile = spedExportService.exportPullOutSchedulesExcel(new java.util.ArrayList<>(schedulesList));
                    } else {
                        exportFile = spedExportService.exportPullOutSchedulesCsv(new java.util.ArrayList<>(schedulesList));
                    }

                    final java.io.File finalFile = exportFile;
                    Platform.runLater(() -> {
                        statusLabel.setText("Ready");
                        showSuccess("Export Complete", "Schedules exported to:\n" + finalFile.getAbsolutePath());
                    });
                } catch (Exception e) {
                    log.error("Error exporting schedules", e);
                    Platform.runLater(() -> {
                        statusLabel.setText("Error");
                        showError("Export Error", "Failed to export schedules: " + e.getMessage());
                    });
                }
            }).start();
        });
    }

    @FXML
    private void handleBalanceWorkload() {
        log.info("Balancing provider workload");

        // Calculate workload statistics for display
        StringBuilder sb = new StringBuilder();
        sb.append("Current Provider Workload:\n\n");

        java.util.Map<Teacher, Integer> workloads = new java.util.HashMap<>();
        for (PullOutSchedule schedule : schedulesList) {
            if (schedule.getStaff() != null && schedule.getDurationMinutes() != null) {
                workloads.merge(schedule.getStaff(), schedule.getDurationMinutes(), (a, b) -> a + b);
            }
        }

        if (workloads.isEmpty()) {
            showInfo("No Data", "No scheduled sessions found to analyze workload.");
            return;
        }

        int totalMinutes = workloads.values().stream().mapToInt(Integer::intValue).sum();
        int avgMinutes = totalMinutes / workloads.size();

        workloads.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(entry -> {
                String status = entry.getValue() > avgMinutes * 1.2 ? " (HIGH)" :
                               entry.getValue() < avgMinutes * 0.8 ? " (LOW)" : "";
                sb.append(String.format("- %s: %d min/week%s%n",
                    entry.getKey().getName(), entry.getValue(), status));
            });

        sb.append(String.format("%nAverage: %d min/week", avgMinutes));
        sb.append("\n\nTo balance workload, reassign sessions from HIGH to LOW providers using the schedule editor.");

        showInfo("Provider Workload Analysis", sb.toString());
    }

    @FXML
    private void handleScheduleService() {
        IEPService selected = servicesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            handleScheduleService(selected);
        }
    }

    @FXML
    private void handleViewIEP() {
        if (selectedStudent != null) {
            handleViewStudentDetails(selectedStudent);
        }
    }

    @FXML
    private void handleExportComplianceReport() {
        log.info("Exporting compliance report");

        // Show format selection dialog
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Excel", "Excel", "Text Report");
        dialog.setTitle("Export Compliance Report");
        dialog.setHeaderText("Select Export Format");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            statusLabel.setText("Exporting compliance report...");

            new Thread(() -> {
                try {
                    java.io.File reportFile;
                    if ("Excel".equals(format)) {
                        reportFile = spedExportService.exportComplianceReportExcel();
                    } else {
                        reportFile = spedExportService.exportComplianceReportPdf();
                    }

                    final java.io.File finalFile = reportFile;
                    Platform.runLater(() -> {
                        statusLabel.setText("Ready");
                        showSuccess("Export Complete", "Compliance report exported to:\n" + finalFile.getAbsolutePath());
                    });
                } catch (Exception e) {
                    log.error("Error exporting compliance report", e);
                    Platform.runLater(() -> {
                        statusLabel.setText("Error");
                        showError("Export Error", "Failed to export compliance report: " + e.getMessage());
                    });
                }
            }).start();
        });
    }

    @FXML
    private void handleHelp() {
        showInfo("SPED Scheduling Help",
                "This dashboard provides comprehensive tools for managing IEP services and pull-out scheduling.\n\n" +
                        "Students Tab: View all students with active IEPs\n" +
                        "Service Scheduling: Schedule and manage IEP services\n" +
                        "Pull-Out Schedules: View and edit scheduled sessions\n" +
                        "Provider Workload: Monitor staff caseloads\n" +
                        "Compliance: Track service delivery compliance");
    }

    private void handleViewStudentDetails(Student student) {
        if (student == null) return;
        log.info("Viewing student details: {}", student.getFullName());
        showStudentDetails(student);
    }

    private void handleScheduleStudentServices(Student student) {
        if (student == null) return;
        log.info("Scheduling services for student: {}", student.getFullName());
        showInfo("Schedule Services", "Service scheduling for " + student.getFullName() +
                " will open the scheduling dialog.");
    }

    private void handleScheduleService(IEPService service) {
        if (service == null) return;
        log.info("Scheduling service: {} for {}", service.getServiceType(),
                service.getStudent() != null ? service.getStudent().getFullName() : "Unknown");

        try {
            pullOutSchedulingService.scheduleService(service.getId());
            showSuccess("Service Scheduled", "Service has been scheduled successfully.");
            loadData();
        } catch (Exception e) {
            log.error("Error scheduling service", e);
            showError("Scheduling Error", "Failed to schedule service: " + e.getMessage());
        }
    }

    private void handleEditSchedule(PullOutSchedule schedule) {
        if (schedule == null) return;
        log.info("Editing schedule: {}", schedule.getId());

        // Create a simple edit dialog
        Dialog<PullOutSchedule> dialog = new Dialog<>();
        dialog.setTitle("Edit Schedule");
        dialog.setHeaderText("Edit Pull-Out Schedule for " +
            (schedule.getStudent() != null ? schedule.getStudent().getFullName() : "Unknown"));

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        ComboBox<String> dayCombo = new ComboBox<>();
        dayCombo.getItems().addAll("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        dayCombo.setValue(schedule.getDayOfWeek());

        TextField startTimeField = new TextField(schedule.getStartTime() != null ?
            schedule.getStartTime().toString() : "09:00");
        TextField endTimeField = new TextField(schedule.getEndTime() != null ?
            schedule.getEndTime().toString() : "09:30");
        TextField locationField = new TextField(schedule.getLocationDisplay());
        TextArea notesArea = new TextArea(schedule.getNotes());
        notesArea.setPrefRowCount(3);

        grid.add(new Label("Day:"), 0, 0);
        grid.add(dayCombo, 1, 0);
        grid.add(new Label("Start Time (HH:MM):"), 0, 1);
        grid.add(startTimeField, 1, 1);
        grid.add(new Label("End Time (HH:MM):"), 0, 2);
        grid.add(endTimeField, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(locationField, 1, 3);
        grid.add(new Label("Notes:"), 0, 4);
        grid.add(notesArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    schedule.setDayOfWeek(dayCombo.getValue());
                    schedule.setStartTime(java.time.LocalTime.parse(startTimeField.getText()));
                    schedule.setEndTime(java.time.LocalTime.parse(endTimeField.getText()));
                    schedule.setLocationDescription(locationField.getText());
                    schedule.setNotes(notesArea.getText());
                    return schedule;
                } catch (Exception e) {
                    showError("Invalid Input", "Please check time format (HH:MM)");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedSchedule -> {
            try {
                pullOutSchedulingService.updateSchedule(updatedSchedule);
                showSuccess("Schedule Updated", "Schedule has been updated successfully.");
                loadData();
            } catch (Exception e) {
                log.error("Error updating schedule", e);
                showError("Update Error", "Failed to update schedule: " + e.getMessage());
            }
        });
    }

    private void handleDeleteSchedule(PullOutSchedule schedule) {
        if (schedule == null) return;
        log.info("Deleting schedule: {}", schedule.getId());

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Schedule");
        confirm.setHeaderText("Delete this scheduled session?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    pullOutSchedulingService.deleteSchedule(schedule.getId());
                    showSuccess("Schedule Deleted", "Scheduled session has been deleted.");
                    loadData();
                } catch (Exception e) {
                    log.error("Error deleting schedule", e);
                    showError("Delete Error", "Failed to delete schedule: " + e.getMessage());
                }
            }
        });
    }

    // Utility methods

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

    /**
     * Inner class to represent compliance alerts
     */
    public static class ComplianceAlert {
        private String severity;
        private Student student;
        private String message;
        private double compliancePercentage;

        public ComplianceAlert(String severity, Student student, String message, double compliancePercentage) {
            this.severity = severity;
            this.student = student;
            this.message = message;
            this.compliancePercentage = compliancePercentage;
        }

        public String getSeverity() { return severity; }
        public Student getStudent() { return student; }
        public String getMessage() { return message; }
        public double getCompliancePercentage() { return compliancePercentage; }
    }
}
