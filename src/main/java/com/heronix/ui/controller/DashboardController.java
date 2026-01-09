package com.heronix.ui.controller;

import com.heronix.model.domain.Schedule;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Dashboard Controller - Enhanced with Special Education & Staff Metrics
 * Location: src/main/java/com/eduscheduler/ui/controller/DashboardController.java
 *
 * @version 2.1.0 - Enhanced Dashboard
 */
@Slf4j
@Controller
public class DashboardController {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private com.heronix.service.SubstituteManagementService substituteManagementService;

    @Autowired
    private com.heronix.service.impl.DashboardAnalyticsService analyticsService;

    @Autowired
    private com.heronix.service.ScheduleHealthService scheduleHealthService;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    // Reference to MainController for navigation
    private MainController mainController;

    @Autowired
    private com.heronix.service.DashboardMetricsService dashboardMetricsService;

    @Autowired
    private com.heronix.service.SmartTeacherAssignmentService smartTeacherAssignmentService;

    @Autowired
    private com.heronix.service.SmartRoomAssignmentService smartRoomAssignmentService;

    @Autowired(required = false)
    private com.heronix.service.BackgroundScheduleAnalyzer backgroundScheduleAnalyzer;

    @Autowired(required = false)
    private com.heronix.service.OllamaAIService ollamaAIService;

    @Autowired
    private com.heronix.service.GradeService gradeService;

    @Autowired
    private com.heronix.repository.StudentGradeRepository studentGradeRepository;

    @Autowired(required = false)
    private com.heronix.service.AcademicPlanningService academicPlanningService;

    @Autowired(required = false)
    private com.heronix.service.CourseRecommendationService courseRecommendationService;

    @Autowired(required = false)
    private com.heronix.repository.AcademicPlanRepository academicPlanRepository;

    @Autowired(required = false)
    private com.heronix.repository.CourseRecommendationRepository courseRecommendationRepository;

    @Autowired
    private com.heronix.service.DashboardService dashboardService;

    @Autowired
    private com.heronix.service.PermissionService permissionService;

    // ========================================================================
    // FXML UI COMPONENTS - Basic Metrics
    // ========================================================================
    @FXML
    private VBox dashboardRoot;
    @FXML
    private Label teacherCountLabel;
    @FXML
    private Label courseCountLabel;
    @FXML
    private Label roomCountLabel;
    @FXML
    private Label studentCountLabel;
    @FXML
    private Label scheduleCountLabel;
    @FXML
    private Label eventCountLabel;
    @FXML
    private Label qualityScoreLabel;
    @FXML
    private Label conflictCountLabel;
    @FXML
    private Label teacherUtilizationLabel;

    // ========================================================================
    // FXML UI COMPONENTS - Schedule Health
    // ========================================================================
    @FXML
    private Label healthScoreLabel;
    @FXML
    private Label healthStatusLabel;
    @FXML
    private Label conflictScoreLabel;
    @FXML
    private Label balanceScoreLabel;
    @FXML
    private Label utilizationScoreLabel;
    @FXML
    private Label coverageScoreLabel;
    @FXML
    private Label healthIssuesLabel;

    // ========================================================================
    // FXML UI COMPONENTS - Teacher Types & Certifications
    // ========================================================================
    @FXML
    private Label certifiedTeachersLabel;
    @FXML
    private Label professionalCertLabel;
    @FXML
    private Label temporaryCertLabel;
    @FXML
    private Label coTeachersLabel;
    @FXML
    private Label paraprofessionalsLabel;

    // ========================================================================
    // FXML UI COMPONENTS - Substitute Tracking
    // ========================================================================
    @FXML
    private Label substitutesWeekLabel;
    @FXML
    private Label substitutesMonthLabel;
    @FXML
    private Label activeSubstitutesLabel;
    @FXML
    private Label substituteHoursLabel;
    @FXML
    private Label pendingAssignmentsLabel;

    // ========================================================================
    // FXML UI COMPONENTS - Para & Co-Teacher Substitute Tracking
    // ========================================================================
    @FXML
    private Label activeParaSubsLabel;
    @FXML
    private Label activeCoTeacherSubsLabel;

    // ========================================================================
    // FXML UI COMPONENTS - Room Utilization
    // ========================================================================
    @FXML
    private Label roomsInUseLabel;
    @FXML
    private Label roomsNotInUseLabel;

    // ========================================================================
    // FXML UI COMPONENTS - IEP Tracking
    // ========================================================================
    @FXML
    private Label iepTodayLabel;
    @FXML
    private Label iepMonthLabel;
    @FXML
    private Label iepYearLabel;

    // ========================================================================
    // FXML UI COMPONENTS - 504 Plan Tracking
    // ========================================================================
    @FXML
    private Label plan504TodayLabel;
    @FXML
    private Label plan504MonthLabel;
    @FXML
    private Label plan504YearLabel;
    @FXML
    private Label totalAccommodationsLabel;

    // ========================================================================
    // FXML UI COMPONENTS - Phase 2: Course Assignment Status Widget
    // ========================================================================
    @FXML
    private Label totalCoursesMetricLabel;
    @FXML
    private Label fullyAssignedLabel;
    @FXML
    private Label fullyAssignedPercentLabel;
    @FXML
    private Label partiallyAssignedLabel;
    @FXML
    private Label partiallyAssignedPercentLabel;
    @FXML
    private Label unassignedLabel;
    @FXML
    private Label unassignedPercentLabel;
    @FXML
    private Label overloadedTeachersIssue;
    @FXML
    private Label certificationIssue;
    @FXML
    private Label labRoomIssue;
    @FXML
    private VBox issuesListContainer;
    @FXML
    private Label assignmentStatusLabel;

    // ========================================================================
    // FXML UI COMPONENTS - AI Schedule Health (Phase 3)
    // ========================================================================
    @FXML
    private VBox aiStatusWidget;
    @FXML
    private Label aiHealthScoreLabel;
    @FXML
    private Label aiHealthStatusLabel;
    @FXML
    private javafx.scene.control.ProgressBar aiHealthProgressBar;
    @FXML
    private Label aiLastAnalysisLabel;
    @FXML
    private Label aiCriticalIssuesLabel;
    @FXML
    private Label aiWarningsLabel;
    @FXML
    private Label aiSuggestionsLabel;
    @FXML
    private Label aiSummaryLabel;
    @FXML
    private javafx.scene.control.Button aiAnalyzeNowButton;

    // ========================================================================
    // FXML UI COMPONENTS - Academic Planning Widget
    // ========================================================================
    @FXML
    private Label totalPlansLabel;
    @FXML
    private Label activePlansLabel;
    @FXML
    private Label activePlansPercentLabel;
    @FXML
    private Label pendingApprovalLabel;
    @FXML
    private Label pendingApprovalPercentLabel;
    @FXML
    private Label atRiskStudentsLabel;
    @FXML
    private Label atRiskStudentsPercentLabel;
    @FXML
    private Label pendingRecommendationsLabel;
    @FXML
    private Label highPriorityRecommendationsLabel;
    @FXML
    private Label acceptedRecommendationsLabel;

    // ========================================================================
    // FXML UI COMPONENTS - Academic Performance (GPA) Widget
    // ========================================================================
    @FXML
    private Label averageGPALabel;
    @FXML
    private Label honorRollCountLabel;
    @FXML
    private Label honorRollPercentLabel;
    @FXML
    private Label highHonorsCountLabel;
    @FXML
    private Label highHonorsPercentLabel;
    @FXML
    private Label academicWarningCountLabel;
    @FXML
    private Label academicWarningPercentLabel;
    @FXML
    private Label studentsWithGradesLabel;
    @FXML
    private Label studentsGradedPercentLabel;
    @FXML
    private Label gradeACountLabel;
    @FXML
    private Label gradeAPercentLabel;
    @FXML
    private Label gradeBCountLabel;
    @FXML
    private Label gradeBPercentLabel;
    @FXML
    private Label gradeCCountLabel;
    @FXML
    private Label gradeCPercentLabel;
    @FXML
    private Label gradeDCountLabel;
    @FXML
    private Label gradeDPercentLabel;
    @FXML
    private Label gradeFCountLabel;
    @FXML
    private Label gradeFPercentLabel;

    /**
     * Set the main controller reference for navigation
     * @param mainController Main controller instance
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Initialize dashboard
     */
    @FXML
    public void initialize() {
        log.info("Dashboard controller initialized - Enhanced version with role-based widgets");
        loadRoleSpecificDashboard(); // Phase 3: Load role-based dashboard
        loadDashboardData();
        loadAIStatus(); // Load AI widget status (Phase 3)
    }

    /**
     * Load role-specific dashboard content (Phase 3: UI/UX Reorganization)
     *
     * This method retrieves the current user's role and loads appropriate
     * dashboard widgets and quick actions from DashboardService.
     *
     * The dashboard is customized based on the user's workflow:
     * - IT Admin: System health, database status, errors
     * - District Admin: District overview, statistics, compliance
     * - Data Entry: Import queue, validation errors, data quality
     * - Registrar: Enrollment statistics, pending registrations
     * - Counselor: At-risk students, IEP/504, graduation tracking
     * - Scheduler: Schedule conflicts, utilization, unscheduled courses
     * - Teacher: My classes, grades pending, at-risk students
     * - Student: My courses, GPA, assignments, attendance
     */
    private void loadRoleSpecificDashboard() {
        if (!com.heronix.security.SecurityContext.isAuthenticated()) {
            log.info("User not authenticated, skipping role-specific dashboard loading");
            return;
        }

        java.util.Optional<com.heronix.model.domain.User> currentUserOpt =
            com.heronix.security.SecurityContext.getCurrentUser();

        if (currentUserOpt.isEmpty()) {
            log.warn("Could not retrieve current user for role-specific dashboard");
            return;
        }

        com.heronix.model.domain.User currentUser = currentUserOpt.get();
        log.info("Loading role-specific dashboard for user: {} with role: {}",
            currentUser.getUsername(), currentUser.getRoleDisplayName());

        // Get role-specific widgets
        java.util.List<com.heronix.service.DashboardService.DashboardWidget> widgets =
            dashboardService.getDashboardWidgets(currentUser);

        log.info("  Loaded {} dashboard widgets for role: {}",
            widgets.size(), currentUser.getRoleDisplayName());

        // Get role-specific quick actions
        java.util.List<com.heronix.service.DashboardService.QuickAction> quickActions =
            dashboardService.getQuickActions(currentUser);

        log.info("  Loaded {} quick actions for role: {}",
            quickActions.size(), currentUser.getRoleDisplayName());

        // Log widget titles for troubleshooting
        for (com.heronix.service.DashboardService.DashboardWidget widget : widgets) {
            log.debug("    Widget: {} = {}", widget.getTitle(), widget.getValue());
        }

        // Log quick action labels for troubleshooting
        for (com.heronix.service.DashboardService.QuickAction action : quickActions) {
            log.debug("    Action: {}", action.getLabel());
        }

        log.info("✓ Role-specific dashboard loaded successfully");

        // TODO Phase 3B: Render widgets to UI (requires FXML updates)
        // For now, widgets are loaded and logged. Next step is to create
        // dynamic UI components or update existing labels with widget data.
    }

    /**
     * Load all dashboard data
     */
    private void loadDashboardData() {
        CompletableFuture.runAsync(() -> {
            try {
                // Load basic counts - FIXED: Count only active/available resources
                long teacherCount = teacherRepository.countByActiveTrue();  // Only active teachers
                long courseCount = courseRepository.count();  // All courses (active filtering done elsewhere)
                long roomCount = roomRepository.count();      // All rooms (available filtering done elsewhere)
                long studentCount = studentRepository.count(); // All students (active filtering done elsewhere)
                long scheduleCount = scheduleRepository.count();

                // Count only upcoming/future events (scheduled events)
                LocalDateTime now = LocalDateTime.now();
                long eventCount = eventRepository.findUpcomingEvents(now).size();

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    safeSetText(teacherCountLabel, String.valueOf(teacherCount));
                    safeSetText(courseCountLabel, String.valueOf(courseCount));
                    safeSetText(roomCountLabel, String.valueOf(roomCount));
                    safeSetText(studentCountLabel, String.valueOf(studentCount));
                    safeSetText(scheduleCountLabel, String.valueOf(scheduleCount));
                    safeSetText(eventCountLabel, String.valueOf(eventCount));
                });

                // Load enhanced metrics
                loadTeacherMetrics();
                loadSubstituteMetrics();
                loadRoomMetrics();
                loadAccommodationMetrics();
                loadLatestScheduleMetrics();
                loadAdvancedAnalytics();
                loadScheduleHealth();
                loadCourseAssignmentMetrics();  // Phase 2: Course assignment status widget
                loadGPAStatistics();  // Academic Performance widget
                loadAcademicPlanningMetrics();  // Academic Planning widget

                log.info("Dashboard data loaded successfully");

            } catch (Exception e) {
                log.error("Error loading dashboard data", e);
                Platform.runLater(() -> {
                    safeSetText(teacherCountLabel, "Error");
                    safeSetText(courseCountLabel, "Error");
                    safeSetText(roomCountLabel, "Error");
                    safeSetText(studentCountLabel, "Error");
                });
            }
        });
    }

    /**
     * Load teacher certification and role metrics
     */
    private void loadTeacherMetrics() {
        try {
            List<Teacher> allTeachers = teacherRepository.findAllActive();

            long certified = allTeachers.stream()
                .filter(t -> t.getCertificationType() != null &&
                            t.getCertificationType().name().equals("CERTIFIED"))
                .count();

            long professional = allTeachers.stream()
                .filter(t -> t.getCertificationType() != null &&
                            t.getCertificationType().name().equals("PROFESSIONAL"))
                .count();

            long temporary = allTeachers.stream()
                .filter(t -> t.getCertificationType() != null &&
                            t.getCertificationType().name().equals("TEMPORARY"))
                .count();

            long coTeachers = allTeachers.stream()
                .filter(t -> "CO_TEACHER".equalsIgnoreCase(t.getTeacherRole()))
                .count();

            long paraprofessionals = allTeachers.stream()
                .filter(t -> "PARAPROFESSIONAL".equalsIgnoreCase(t.getTeacherRole()))
                .count();

            Platform.runLater(() -> {
                safeSetText(certifiedTeachersLabel, String.valueOf(certified));
                safeSetText(professionalCertLabel, String.valueOf(professional));
                safeSetText(temporaryCertLabel, String.valueOf(temporary));
                safeSetText(coTeachersLabel, String.valueOf(coTeachers));
                safeSetText(paraprofessionalsLabel, String.valueOf(paraprofessionals));
            });

            log.info("Teacher metrics loaded: {} certified, {} professional, {} temporary",
                certified, professional, temporary);

        } catch (Exception e) {
            log.error("Error loading teacher metrics", e);
            Platform.runLater(() -> {
                safeSetText(certifiedTeachersLabel, "0");
                safeSetText(professionalCertLabel, "0");
                safeSetText(temporaryCertLabel, "0");
                safeSetText(coTeachersLabel, "0");
                safeSetText(paraprofessionalsLabel, "0");
            });
        }
    }

    /**
     * Load substitute teacher metrics for week and month
     * Uses SubstituteManagementService for accurate assignment tracking
     */
    private void loadSubstituteMetrics() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = today.with(java.time.DayOfWeek.SUNDAY);
            YearMonth currentMonth = YearMonth.from(today);
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            // Get actual substitute assignments from the new system
            java.util.List<com.heronix.model.domain.SubstituteAssignment> weekAssignments =
                substituteManagementService.getAssignmentsBetweenDates(weekStart, weekEnd);

            java.util.List<com.heronix.model.domain.SubstituteAssignment> monthAssignments =
                substituteManagementService.getAssignmentsBetweenDates(monthStart, monthEnd);

            long substitutesWeek = weekAssignments.stream()
                .filter(a -> a.getStatus() == com.heronix.model.enums.AssignmentStatus.CONFIRMED ||
                           a.getStatus() == com.heronix.model.enums.AssignmentStatus.IN_PROGRESS ||
                           a.getStatus() == com.heronix.model.enums.AssignmentStatus.COMPLETED)
                .count();

            long substitutesMonth = monthAssignments.stream()
                .filter(a -> a.getStatus() == com.heronix.model.enums.AssignmentStatus.CONFIRMED ||
                           a.getStatus() == com.heronix.model.enums.AssignmentStatus.IN_PROGRESS ||
                           a.getStatus() == com.heronix.model.enums.AssignmentStatus.COMPLETED)
                .count();

            // Count active substitutes
            long activeSubstitutes = substituteManagementService.countActiveSubstitutes();

            // Calculate total hours this month
            double totalHours = monthAssignments.stream()
                .filter(a -> a.getStatus() == com.heronix.model.enums.AssignmentStatus.CONFIRMED ||
                           a.getStatus() == com.heronix.model.enums.AssignmentStatus.IN_PROGRESS ||
                           a.getStatus() == com.heronix.model.enums.AssignmentStatus.COMPLETED)
                .filter(a -> a.getTotalHours() != null)
                .mapToDouble(com.heronix.model.domain.SubstituteAssignment::getTotalHours)
                .sum();

            // Count pending assignments (all dates)
            long pendingAssignments = substituteManagementService.getAllAssignments().stream()
                .filter(a -> a.getStatus() == com.heronix.model.enums.AssignmentStatus.PENDING)
                .count();

            // Count active para and co-teacher substitutes
            long activeParaSubs = substituteManagementService.getAllSubstitutes().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getType() != null &&
                        s.getType().name().contains("PARA"))
                .count();

            long activeCoTeacherSubs = substituteManagementService.getAllSubstitutes().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getType() != null &&
                        s.getType().name().contains("CO_TEACHER"))
                .count();

            Platform.runLater(() -> {
                safeSetText(substitutesWeekLabel, String.valueOf(substitutesWeek));
                safeSetText(substitutesMonthLabel, String.valueOf(substitutesMonth));
                safeSetText(activeSubstitutesLabel, String.valueOf(activeSubstitutes));
                safeSetText(substituteHoursLabel, String.format("%.1f", totalHours));
                safeSetText(pendingAssignmentsLabel, String.valueOf(pendingAssignments));
                safeSetText(activeParaSubsLabel, String.valueOf(activeParaSubs));
                safeSetText(activeCoTeacherSubsLabel, String.valueOf(activeCoTeacherSubs));
            });

            log.info("Substitute metrics loaded: {} assignments this week, {} this month, {} active subs, {:.1f} hours, {} pending",
                substitutesWeek, substitutesMonth, activeSubstitutes, totalHours, pendingAssignments);

        } catch (Exception e) {
            log.error("Error loading substitute metrics", e);
            Platform.runLater(() -> {
                safeSetText(substitutesWeekLabel, "0");
                safeSetText(substitutesMonthLabel, "0");
                safeSetText(activeSubstitutesLabel, "0");
                safeSetText(substituteHoursLabel, "0.0");
                safeSetText(pendingAssignmentsLabel, "0");
                safeSetText(activeParaSubsLabel, "0");
                safeSetText(activeCoTeacherSubsLabel, "0");
            });
        }
    }

    /**
     * Load room utilization metrics
     */
    private void loadRoomMetrics() {
        try {
            long totalRooms = roomRepository.count();

            // Count rooms that are assigned to schedule slots
            long roomsInUse = scheduleSlotRepository.findAll().stream()
                .filter(slot -> slot.getRoom() != null)
                .map(slot -> slot.getRoom().getId())
                .distinct()
                .count();

            long roomsNotInUse = totalRooms - roomsInUse;

            Platform.runLater(() -> {
                safeSetText(roomsInUseLabel, String.valueOf(roomsInUse));
                safeSetText(roomsNotInUseLabel, String.valueOf(roomsNotInUse));
            });

            log.info("Room metrics loaded: {} in use, {} available", roomsInUse, roomsNotInUse);

        } catch (Exception e) {
            log.error("Error loading room metrics", e);
            Platform.runLater(() -> {
                safeSetText(roomsInUseLabel, "0");
                safeSetText(roomsNotInUseLabel, "0");
            });
        }
    }

    /**
     * Load IEP and 504 Plan accommodation metrics
     */
    private void loadAccommodationMetrics() {
        try {
            List<Student> allStudents = studentRepository.findAll();
            LocalDate today = LocalDate.now();
            YearMonth currentMonth = YearMonth.from(today);
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            // Assuming school year ends June 30
            final LocalDate yearEnd = today.getMonthValue() > 6
                ? LocalDate.of(today.getYear() + 1, 6, 30)
                : LocalDate.of(today.getYear(), 6, 30);

            // IEP Metrics
            long iepToday = allStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHasIEP()))
                .filter(s -> s.getAccommodationReviewDate() != null)
                .filter(s -> s.getAccommodationReviewDate().isEqual(today))
                .count();

            long iepMonth = allStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHasIEP()))
                .filter(s -> s.getAccommodationReviewDate() != null)
                .filter(s -> !s.getAccommodationReviewDate().isBefore(monthStart) &&
                           !s.getAccommodationReviewDate().isAfter(monthEnd))
                .count();

            long iepYear = allStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHasIEP()))
                .filter(s -> s.getAccommodationReviewDate() != null)
                .filter(s -> !s.getAccommodationReviewDate().isBefore(today) &&
                           !s.getAccommodationReviewDate().isAfter(yearEnd))
                .count();

            // 504 Plan Metrics
            long plan504Today = allStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHas504Plan()))
                .filter(s -> s.getAccommodationReviewDate() != null)
                .filter(s -> s.getAccommodationReviewDate().isEqual(today))
                .count();

            long plan504Month = allStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHas504Plan()))
                .filter(s -> s.getAccommodationReviewDate() != null)
                .filter(s -> !s.getAccommodationReviewDate().isBefore(monthStart) &&
                           !s.getAccommodationReviewDate().isAfter(monthEnd))
                .count();

            long plan504Year = allStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHas504Plan()))
                .filter(s -> s.getAccommodationReviewDate() != null)
                .filter(s -> !s.getAccommodationReviewDate().isBefore(today) &&
                           !s.getAccommodationReviewDate().isAfter(yearEnd))
                .count();

            // Total students with accommodations
            long totalAccommodations = allStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.getHasIEP()) || Boolean.TRUE.equals(s.getHas504Plan()))
                .count();

            Platform.runLater(() -> {
                safeSetText(iepTodayLabel, String.valueOf(iepToday));
                safeSetText(iepMonthLabel, String.valueOf(iepMonth));
                safeSetText(iepYearLabel, String.valueOf(iepYear));
                safeSetText(plan504TodayLabel, String.valueOf(plan504Today));
                safeSetText(plan504MonthLabel, String.valueOf(plan504Month));
                safeSetText(plan504YearLabel, String.valueOf(plan504Year));
                safeSetText(totalAccommodationsLabel, String.valueOf(totalAccommodations));
            });

            log.info("Accommodation metrics loaded: {} IEP today, {} 504 today, {} total",
                iepToday, plan504Today, totalAccommodations);

        } catch (Exception e) {
            log.error("Error loading accommodation metrics", e);
            Platform.runLater(() -> {
                safeSetText(iepTodayLabel, "0");
                safeSetText(iepMonthLabel, "0");
                safeSetText(iepYearLabel, "0");
                safeSetText(plan504TodayLabel, "0");
                safeSetText(plan504MonthLabel, "0");
                safeSetText(plan504YearLabel, "0");
                safeSetText(totalAccommodationsLabel, "0");
            });
        }
    }

    /**
     * Load metrics from the latest schedule
     */
    private void loadLatestScheduleMetrics() {
        try {
            List<Schedule> schedules = scheduleRepository.findAll();
            if (!schedules.isEmpty()) {
                Schedule latest = schedules.get(schedules.size() - 1);

                Platform.runLater(() -> {
                    safeSetText(qualityScoreLabel,
                        latest.getOptimizationScore() != null ?
                        String.format("%.1f%%", latest.getOptimizationScore()) : "N/A");

                    safeSetText(conflictCountLabel,
                        latest.getTotalConflicts() != null ?
                        String.valueOf(latest.getTotalConflicts()) : "0");

                    safeSetText(teacherUtilizationLabel,
                        latest.getTeacherUtilization() != null ?
                        String.format("%.1f%%", latest.getTeacherUtilization()) : "N/A");
                });
            } else {
                Platform.runLater(() -> {
                    safeSetText(qualityScoreLabel, "No schedules");
                    safeSetText(conflictCountLabel, "N/A");
                    safeSetText(teacherUtilizationLabel, "N/A");
                });
            }
        } catch (Exception e) {
            log.error("Error loading schedule metrics", e);
            Platform.runLater(() -> {
                safeSetText(qualityScoreLabel, "Error");
                safeSetText(conflictCountLabel, "Error");
                safeSetText(teacherUtilizationLabel, "Error");
            });
        }
    }

    /**
     * Safely set text on a label (null-safe)
     */
    private void safeSetText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        } else {
            // Debug level - some labels were removed during dashboard reorganization
            log.debug("Label not present in current dashboard layout: {}", text);
        }
    }

    /**
     * Refresh dashboard data
     */
    @FXML
    public void refreshDashboard() {
        log.info("Refreshing dashboard");
        loadDashboardData();
    }

    /**
     * Open the District Settings dialog
     */
    @FXML
    public void openDistrictSettings() {
        try {
            log.info("Opening District Settings dialog");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/DistrictSettingsDialog.fxml")
            );

            // Use Spring context for controller factory if available
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();

            // Get the controller and set up the dialog
            com.heronix.ui.controller.DistrictSettingsDialogController controller = loader.getController();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("District Settings");
            stage.setScene(new javafx.scene.Scene(root, 800, 700));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            controller.setDialogStage(stage);
            controller.loadSettings();

            stage.showAndWait();

            // Refresh dashboard if settings were saved
            if (controller.isSaveClicked()) {
                refreshDashboard();
            }

        } catch (Exception e) {
            log.error("Error opening District Settings", e);
            showError("Failed to open District Settings: " + e.getMessage());
        }
    }

    /**
     * Open the Substitute Management interface
     */
    @FXML
    public void openSubstituteManagement() {
        try {
            log.info("Opening Substitute Management interface");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/SubstituteManagement.fxml")
            );

            // Use Spring context for controller factory if available
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Substitute Management");
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Substitute Management", e);
            showError("Failed to open Substitute Management: " + e.getMessage());
        }
    }

    /**
     * Open the Paraprofessional Substitute Management interface
     */
    @FXML
    public void openParaprofessionalManagement() {
        try {
            log.info("Opening Paraprofessional Management interface");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/ParaprofessionalManagement.fxml")
            );

            // Use Spring context for controller factory if available
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Paraprofessional Substitute Management");
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Paraprofessional Management", e);
            showError("Failed to open Paraprofessional Management: " + e.getMessage());
        }
    }

    /**
     * Open the Co-Teacher Substitute Management interface
     */
    @FXML
    public void openCoTeacherManagement() {
        try {
            log.info("Opening Co-Teacher Management interface");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/CoTeacherManagement.fxml")
            );

            // Use Spring context for controller factory if available
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Co-Teacher Substitute Management");
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Co-Teacher Management", e);
            showError("Failed to open Co-Teacher Management: " + e.getMessage());
        }
    }

    /**
     * Open Priority Rules Configuration dialog
     * Phase 6 Feature - Course Assignment System
     */
    @FXML
    public void openPriorityRulesConfig() {
        try {
            log.info("Opening Priority Rules Configuration");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/PriorityRulesDialog.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Priority Rules Configuration");
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Priority Rules Configuration", e);
            showError("Failed to open Priority Rules Configuration: " + e.getMessage());
        }
    }

    /**
     * Open Course Assignment Wizard
     * Phase 6 Feature - Course Assignment System
     */
    @FXML
    public void openCourseAssignmentWizard() {
        try {
            log.info("Opening Course Assignment Wizard");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/CourseAssignmentWizard.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Course Assignment Wizard");
            stage.setScene(new javafx.scene.Scene(root, 900, 700));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Course Assignment Wizard", e);
            showError("Failed to open Course Assignment Wizard: " + e.getMessage());
        }
    }

    /**
     * Open Waitlist Manager
     * Phase 6 Feature - Course Assignment System
     */
    @FXML
    public void openWaitlistManager() {
        try {
            log.info("Opening Waitlist Manager");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/WaitlistManager.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Waitlist Manager");
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Waitlist Manager", e);
            showError("Failed to open Waitlist Manager: " + e.getMessage());
        }
    }

    /**
     * Open Assignment Reports
     * Phase 6 Feature - Course Assignment System
     */
    @FXML
    public void openAssignmentReports() {
        try {
            log.info("Opening Assignment Reports");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/AssignmentReports.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Assignment Reports & Analytics");
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Assignment Reports", e);
            showError("Failed to open Assignment Reports: " + e.getMessage());
        }
    }

    /**
     * Open Student Enrollment Request Creation
     * Phase 7A Feature - Request Creation
     */
    @FXML
    public void openEnrollmentRequestCreation() {
        try {
            log.info("Opening Student Enrollment Request Creation");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/StudentEnrollmentRequestDialog.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Create Enrollment Requests");
            stage.setScene(new javafx.scene.Scene(root, 1400, 800));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Enrollment Request Creation", e);
            showError("Failed to open Enrollment Request Creation: " + e.getMessage());
        }
    }

    /**
     * Open Bulk Enrollment Import
     * Phase 7B Feature - CSV Bulk Import
     */
    @FXML
    public void openBulkEnrollmentImport() {
        try {
            log.info("Opening Bulk Enrollment Import");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/BulkEnrollmentImport.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Bulk Enrollment Import");
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Bulk Enrollment Import", e);
            showError("Failed to open Bulk Enrollment Import: " + e.getMessage());
        }
    }

    /**
     * Open Enrollment Request Management
     * Phase 7C Feature - Manage All Enrollment Requests
     */
    @FXML
    public void openEnrollmentRequestManagement() {
        try {
            log.info("Opening Enrollment Request Management");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/EnrollmentRequestManagement.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Enrollment Request Management");
            stage.setScene(new javafx.scene.Scene(root, 1400, 900));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Enrollment Request Management", e);
            showError("Failed to open Enrollment Request Management: " + e.getMessage());
        }
    }

    /**
     * Show error dialog
     */
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Load advanced analytics data from DashboardAnalyticsService
     */
    private void loadAdvancedAnalytics() {
        try {
            // Load performance metrics
            loadPerformanceMetrics();

            // Load trend data
            loadTrendData();

            // Load and display alerts
            displayAlerts();

            log.info("Advanced analytics loaded successfully");

        } catch (Exception e) {
            log.error("Error loading advanced analytics", e);
        }
    }

    /**
     * Load performance metrics (quality, workload, capacity)
     */
    private void loadPerformanceMetrics() {
        try {
            // Get quality metrics
            com.heronix.service.impl.DashboardAnalyticsService.QualityMetrics qualityMetrics =
                analyticsService.getScheduleQualityMetrics();

            if (qualityMetrics.isHasData()) {
                log.info("Quality Metrics - Optimization: {:.1f}%, Conflicts: {}, Conflict-free: {:.1f}%",
                    qualityMetrics.getOptimizationScore(),
                    qualityMetrics.getConflictCount(),
                    qualityMetrics.getConflictFreePercentage());
            }

            // Get workload metrics
            com.heronix.service.impl.DashboardAnalyticsService.WorkloadMetrics workloadMetrics =
                analyticsService.getWorkloadMetrics();

            log.info("Workload Metrics - Avg: {:.1f} slots, Min: {}, Max: {}, Balance: {:.1f}%",
                workloadMetrics.getAverageSlots(),
                workloadMetrics.getMinSlots(),
                workloadMetrics.getMaxSlots(),
                workloadMetrics.getBalanceScore());

            // Get capacity metrics
            com.heronix.service.impl.DashboardAnalyticsService.CapacityMetrics capacityMetrics =
                analyticsService.getCapacityMetrics();

            log.info("Capacity Metrics - Utilization: {:.1f}% ({} / {} slots)",
                capacityMetrics.getUtilizationPercentage(),
                capacityMetrics.getUsedSlots(),
                capacityMetrics.getTotalCapacity());

            // Get week-over-week comparison
            com.heronix.service.impl.DashboardAnalyticsService.ComparisonMetrics comparison =
                analyticsService.getWeekOverWeekComparison();

            log.info("Week Comparison - This week: {}, Last week: {}, Change: {:.1f}%",
                comparison.getThisWeekSubstitutes(),
                comparison.getLastWeekSubstitutes(),
                comparison.getPercentageChange());

        } catch (Exception e) {
            log.error("Error loading performance metrics", e);
        }
    }

    /**
     * Load trend data for analytics
     */
    private void loadTrendData() {
        try {
            // Get 6-month teacher utilization trend
            java.util.Map<String, Double> utilizationTrend =
                analyticsService.getTeacherUtilizationTrend(6);

            log.info("Teacher Utilization Trend (6 months): {}", utilizationTrend);

            // Get 6-month substitute usage trend
            java.util.Map<String, Long> substituteTrend =
                analyticsService.getSubstituteUsageTrend(6);

            log.info("Substitute Usage Trend (6 months): {}", substituteTrend);

            // Get room utilization data
            java.util.Map<String, Object> roomUtilization =
                analyticsService.getRoomUtilizationData();

            Object avgUtil = roomUtilization.get("avgUtilization");
            if (avgUtil != null) {
                log.info("Room Utilization - Avg: " + String.format("%.1f%%", avgUtil));
            }

        } catch (Exception e) {
            log.error("Error loading trend data", e);
        }
    }

    /**
     * Display alerts from analytics service
     */
    private void displayAlerts() {
        try {
            java.util.List<com.heronix.service.impl.DashboardAnalyticsService.Alert> alerts =
                analyticsService.getAlerts();

            if (!alerts.isEmpty()) {
                log.info("=== DASHBOARD ALERTS ===");
                for (com.heronix.service.impl.DashboardAnalyticsService.Alert alert : alerts) {
                    log.info("[{}] {}: {}", alert.getSeverity(), alert.getTitle(), alert.getMessage());
                }
                log.info("========================");
            } else {
                log.info("No alerts - system healthy");
            }

        } catch (Exception e) {
            log.error("Error displaying alerts", e);
        }
    }

    // ========================================================================
    // SCHEDULE HEALTH METHODS
    // ========================================================================

    /**
     * Load schedule health metrics and update UI
     */
    private void loadScheduleHealth() {
        CompletableFuture.runAsync(() -> {
            try {
                // Get the most recent schedule
                List<Schedule> schedules = scheduleRepository.findAll();
                if (schedules.isEmpty()) {
                    Platform.runLater(() -> updateHealthUINoSchedule());
                    return;
                }

                // Get first schedule (or most recent)
                Schedule schedule = schedules.get(0);

                // Calculate health metrics
                com.heronix.model.dto.ScheduleHealthMetrics metrics =
                    scheduleHealthService.calculateHealthMetrics(schedule);

                // Update UI on JavaFX thread
                Platform.runLater(() -> updateHealthUI(metrics));

            } catch (Exception e) {
                log.error("Error loading schedule health", e);
                Platform.runLater(() -> updateHealthUIError());
            }
        });
    }

    /**
     * Update health UI with metrics
     */
    private void updateHealthUI(com.heronix.model.dto.ScheduleHealthMetrics metrics) {
        // Overall score
        safeSetText(healthScoreLabel, String.format("%.0f", metrics.getOverallScore()));
        safeSetText(healthStatusLabel, metrics.getHealthStatus());

        // Apply color styling based on health status
        String colorStyle = "";
        switch (metrics.getHealthStatus()) {
            case "EXCELLENT":
                colorStyle = "-fx-text-fill: #00ff88;"; // Green
                break;
            case "GOOD":
                colorStyle = "-fx-text-fill: #ffee00;"; // Yellow
                break;
            case "FAIR":
                colorStyle = "-fx-text-fill: #ff9900;"; // Orange
                break;
            case "POOR":
                colorStyle = "-fx-text-fill: #ff4444;"; // Red
                break;
        }

        if (healthScoreLabel != null) {
            healthScoreLabel.setStyle(colorStyle);
        }
        if (healthStatusLabel != null) {
            healthStatusLabel.setStyle(colorStyle);
        }

        // Component scores
        safeSetText(conflictScoreLabel, String.format("%.0f", metrics.getConflictScore()));
        safeSetText(balanceScoreLabel, String.format("%.0f", metrics.getBalanceScore()));
        safeSetText(utilizationScoreLabel, String.format("%.0f", metrics.getUtilizationScore()));
        safeSetText(coverageScoreLabel, String.format("%.0f", metrics.getCoverageScore()));

        // Issues summary
        if (metrics.getCriticalIssues().isEmpty()) {
            safeSetText(healthIssuesLabel, "✓ No critical issues detected");
            if (healthIssuesLabel != null) {
                healthIssuesLabel.setStyle("-fx-text-fill: #00ff88;");
            }
        } else {
            String issuesText = "⚠ " + metrics.getCriticalIssues().size() + " critical issue(s): " +
                String.join(", ", metrics.getCriticalIssues());
            safeSetText(healthIssuesLabel, issuesText);
            if (healthIssuesLabel != null) {
                healthIssuesLabel.setStyle("-fx-text-fill: #ff4444;");
            }
        }

        log.info("Schedule health updated: {} ({})", metrics.getOverallScore(), metrics.getHealthStatus());
    }

    /**
     * Update health UI when no schedule exists
     */
    private void updateHealthUINoSchedule() {
        safeSetText(healthScoreLabel, "--");
        safeSetText(healthStatusLabel, "No Schedule");
        safeSetText(conflictScoreLabel, "--");
        safeSetText(balanceScoreLabel, "--");
        safeSetText(utilizationScoreLabel, "--");
        safeSetText(coverageScoreLabel, "--");
        safeSetText(healthIssuesLabel, "ℹ No schedule found. Generate a schedule to see health metrics.");
    }

    /**
     * Update health UI on error
     */
    private void updateHealthUIError() {
        safeSetText(healthScoreLabel, "ERR");
        safeSetText(healthStatusLabel, "Error");
        safeSetText(healthIssuesLabel, "⚠ Error calculating health metrics");
    }

    /**
     * Handle Sanity Check button - comprehensive schedule validation
     */
    @FXML
    public void handleSanityCheck() {
        log.info("Running sanity check...");

        CompletableFuture.runAsync(() -> {
            try {
                // Get the most recent schedule
                List<Schedule> schedules = scheduleRepository.findAll();
                if (schedules.isEmpty()) {
                    Platform.runLater(() -> showSanityCheckDialog("No Schedule Found",
                        "No schedule exists to validate. Please generate a schedule first."));
                    return;
                }

                Schedule schedule = schedules.get(0);

                // Calculate comprehensive health metrics
                com.heronix.model.dto.ScheduleHealthMetrics metrics =
                    scheduleHealthService.calculateHealthMetrics(schedule);

                // Update health display
                Platform.runLater(() -> {
                    updateHealthUI(metrics);
                    showSanityCheckResults(metrics);
                });

            } catch (Exception e) {
                log.error("Error running sanity check", e);
                Platform.runLater(() -> showSanityCheckDialog("Sanity Check Error",
                    "An error occurred while running the sanity check:\n\n" + e.getMessage()));
            }
        });
    }

    /**
     * Show sanity check results dialog
     */
    private void showSanityCheckResults(com.heronix.model.dto.ScheduleHealthMetrics metrics) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Schedule Sanity Check Results");
        alert.setHeaderText(String.format("Overall Health: %s (%.0f/100)",
            metrics.getHealthStatus(), metrics.getOverallScore()));

        // Build detailed report
        StringBuilder content = new StringBuilder();

        // Component scores
        content.append("═══ COMPONENT SCORES ═══\n\n");
        content.append(String.format("✓ Conflicts:    %.0f/100\n", metrics.getConflictScore()));
        content.append(String.format("✓ Balance:      %.0f/100\n", metrics.getBalanceScore()));
        content.append(String.format("✓ Utilization:  %.0f/100\n", metrics.getUtilizationScore()));
        content.append(String.format("✓ Compliance:   %.0f/100\n", metrics.getComplianceScore()));
        content.append(String.format("✓ Coverage:     %.0f/100\n\n", metrics.getCoverageScore()));

        // Critical issues
        if (!metrics.getCriticalIssues().isEmpty()) {
            content.append("⚠ CRITICAL ISSUES (" + metrics.getCriticalIssues().size() + "):\n\n");
            for (String issue : metrics.getCriticalIssues()) {
                content.append("  • " + issue + "\n");
            }
            content.append("\n");
        }

        // Warnings
        if (!metrics.getWarnings().isEmpty()) {
            content.append("⚠ WARNINGS (" + metrics.getWarnings().size() + "):\n\n");
            for (String warning : metrics.getWarnings()) {
                content.append("  • " + warning + "\n");
            }
            content.append("\n");
        }

        // Recommendations
        if (!metrics.getRecommendations().isEmpty()) {
            content.append("💡 RECOMMENDATIONS:\n\n");
            for (String rec : metrics.getRecommendations()) {
                content.append("  • " + rec + "\n");
            }
            content.append("\n");
        }

        // Overall assessment
        content.append("═══ OVERALL ASSESSMENT ═══\n\n");
        if (metrics.isAcceptable()) {
            content.append("✓ Schedule is ACCEPTABLE for production use.\n");
            content.append("  You can proceed with finalizing and publishing.");
        } else {
            content.append("⚠ Schedule needs IMPROVEMENT before production.\n");
            content.append("  Please address critical issues and warnings.");
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Show simple sanity check dialog
     */
    private void showSanityCheckDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========================================================================
    // PHASE 2: COURSE ASSIGNMENT METRICS METHODS
    // ========================================================================

    /**
     * Load course assignment status metrics for the dashboard widget
     */
    private void loadCourseAssignmentMetrics() {
        try {
            com.heronix.model.dto.DashboardMetrics metrics = dashboardMetricsService.calculateMetrics();

            Platform.runLater(() -> {
                // Update total courses
                safeSetText(totalCoursesMetricLabel, String.valueOf(metrics.getTotalCourses()));

                // Update fully assigned
                safeSetText(fullyAssignedLabel, String.valueOf(metrics.getFullyAssignedCourses()));
                safeSetText(fullyAssignedPercentLabel, String.format("(%.0f%%)", metrics.getFullyAssignedPercent()));

                // Update partially assigned
                safeSetText(partiallyAssignedLabel, String.valueOf(metrics.getPartiallyAssignedCourses()));
                safeSetText(partiallyAssignedPercentLabel, String.format("(%.0f%%)", metrics.getPartiallyAssignedPercent()));

                // Update unassigned
                safeSetText(unassignedLabel, String.valueOf(metrics.getUnassignedCourses()));
                safeSetText(unassignedPercentLabel, String.format("(%.0f%%)", metrics.getUnassignedPercent()));

                // Update issues
                safeSetText(overloadedTeachersIssue, metrics.getOverloadedTeachersMessage());
                safeSetText(certificationIssue, metrics.getCertificationMismatchMessage());
                safeSetText(labRoomIssue, metrics.getLabRoomIssuesMessage());
            });

            log.info("Course assignment metrics loaded: {} total, {} fully assigned, {} partial, {} unassigned",
                metrics.getTotalCourses(), metrics.getFullyAssignedCourses(),
                metrics.getPartiallyAssignedCourses(), metrics.getUnassignedCourses());

        } catch (Exception e) {
            log.error("Error loading course assignment metrics", e);
            Platform.runLater(() -> {
                safeSetText(totalCoursesMetricLabel, "Error");
                safeSetText(fullyAssignedLabel, "0");
                safeSetText(partiallyAssignedLabel, "0");
                safeSetText(unassignedLabel, "0");
            });
        }
    }

    /**
     * Open the Courses view (button handler)
     */
    @FXML
    public void openCoursesView() {
        try {
            log.info("Opening Courses view from dashboard");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/Courses.fxml")
            );

            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Courses Management");
            stage.setScene(new javafx.scene.Scene(root, 1400, 900));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Courses view", e);
            showError("Failed to open Courses view: " + e.getMessage());
        }
    }

    /**
     * View unassigned courses list (button handler)
     */
    @FXML
    public void viewUnassignedCourses() {
        try {
            log.info("Opening Courses view filtered to unassigned");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/Courses.fxml")
            );

            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();

            // Get the controller and trigger unassigned filter
            Object controller = loader.getController();
            if (controller instanceof com.heronix.ui.controller.CoursesController) {
                // Note: Would need to add a public method to CoursesController
                // to programmatically trigger the unassigned filter
                log.debug("CoursesController loaded successfully");
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Unassigned Courses");
            stage.setScene(new javafx.scene.Scene(root, 1400, 900));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening unassigned courses view", e);
            showError("Failed to open unassigned courses view: " + e.getMessage());
        }
    }

    // ========================================================================
    // SMART ASSIGNMENT METHODS
    // ========================================================================

    /**
     * Smart assign teachers to all unassigned courses
     */
    @FXML
    public void smartAssignTeachers() {
        log.info("Starting smart teacher assignment from dashboard");

        Platform.runLater(() -> safeSetText(assignmentStatusLabel, "Assigning teachers..."));

        CompletableFuture.runAsync(() -> {
            try {
                com.heronix.service.SmartTeacherAssignmentService.AssignmentResult result =
                    smartTeacherAssignmentService.smartAssignAllTeachers();

                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Complete");
                    showAssignmentResult("Teacher Assignment", result);

                    // Refresh dashboard metrics
                    loadCourseAssignmentMetrics();
                });

            } catch (Exception e) {
                log.error("Error during smart teacher assignment", e);
                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Error");
                    showError("Smart teacher assignment failed: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Smart assign rooms to all unassigned courses
     */
    @FXML
    public void smartAssignRooms() {
        log.info("Starting smart room assignment from dashboard");

        Platform.runLater(() -> safeSetText(assignmentStatusLabel, "Assigning rooms..."));

        CompletableFuture.runAsync(() -> {
            try {
                com.heronix.service.SmartTeacherAssignmentService.AssignmentResult result =
                    smartRoomAssignmentService.smartAssignAllRooms();

                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Complete");
                    showAssignmentResult("Room Assignment", result);

                    // Refresh dashboard metrics
                    loadCourseAssignmentMetrics();
                });

            } catch (Exception e) {
                log.error("Error during smart room assignment", e);
                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Error");
                    showError("Smart room assignment failed: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Smart assign both teachers and rooms to all unassigned courses
     */
    @FXML
    public void smartAssignAll() {
        log.info("Starting smart assignment (teachers + rooms) from dashboard");

        // Show confirmation dialog
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Smart Assign All");
        confirm.setHeaderText("Assign Teachers and Rooms Automatically?");
        confirm.setContentText(
            "This will automatically assign:\n" +
            "• Teachers to courses (based on certifications and workload)\n" +
            "• Rooms to courses (based on type and capacity)\n\n" +
            "Do you want to proceed?"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                performSmartAssignAll();
            }
        });
    }

    /**
     * Perform the actual smart assignment (teachers + rooms)
     */
    private void performSmartAssignAll() {
        Platform.runLater(() -> safeSetText(assignmentStatusLabel, "Assigning all..."));

        CompletableFuture.runAsync(() -> {
            try {
                // Assign teachers first
                com.heronix.service.SmartTeacherAssignmentService.AssignmentResult teacherResult =
                    smartTeacherAssignmentService.smartAssignAllTeachers();

                // Then assign rooms
                com.heronix.service.SmartTeacherAssignmentService.AssignmentResult roomResult =
                    smartRoomAssignmentService.smartAssignAllRooms();

                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Complete");
                    showCombinedAssignmentResult(teacherResult, roomResult);

                    // Refresh dashboard metrics
                    loadCourseAssignmentMetrics();
                });

            } catch (Exception e) {
                log.error("Error during smart assignment (all)", e);
                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Error");
                    showError("Smart assignment failed: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Clear all course assignments (teachers AND rooms)
     * ADDED December 14, 2025: Requested by user to easily reset assignments
     */
    @FXML
    public void clearAllAssignments() {
        log.info("User requested to clear all course assignments");

        // Show confirmation dialog
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear All Assignments");
        confirm.setHeaderText("⚠️ Clear ALL Teacher and Room Assignments?");
        confirm.setContentText(
            "This will remove:\n" +
            "• ALL teacher assignments from courses\n" +
            "• ALL room assignments from courses\n\n" +
            "This action cannot be undone!\n\n" +
            "Are you sure you want to proceed?"
        );

        // Add custom button types
        javafx.scene.control.ButtonType buttonTypeClear = new javafx.scene.control.ButtonType("Clear All");
        javafx.scene.control.ButtonType buttonTypeCancel = new javafx.scene.control.ButtonType("Cancel",
            javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(buttonTypeClear, buttonTypeCancel);

        confirm.showAndWait().ifPresent(response -> {
            if (response == buttonTypeClear) {
                performClearAllAssignments();
            }
        });
    }

    /**
     * Perform the actual clearing of all assignments
     */
    private void performClearAllAssignments() {
        Platform.runLater(() -> safeSetText(assignmentStatusLabel, "Clearing assignments..."));

        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // Get all courses
                List<com.heronix.model.domain.Course> allCourses = courseRepository.findAll();
                int totalCourses = allCourses.size();
                int clearedCount = 0;

                // Clear teacher and room assignments
                for (com.heronix.model.domain.Course course : allCourses) {
                    boolean hadAssignment = course.getTeacher() != null || course.getRoom() != null;

                    course.setTeacher(null);
                    course.setRoom(null);
                    courseRepository.save(course);

                    if (hadAssignment) {
                        clearedCount++;
                    }
                }

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                int finalClearedCount = clearedCount;
                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Complete");
                    showClearAssignmentResult(totalCourses, finalClearedCount, duration);

                    // Refresh dashboard metrics
                    loadCourseAssignmentMetrics();
                });

                log.info("Cleared assignments from {} of {} courses in {}ms",
                    clearedCount, totalCourses, duration);

            } catch (Exception e) {
                log.error("Error clearing course assignments", e);
                Platform.runLater(() -> {
                    safeSetText(assignmentStatusLabel, "Error");
                    showError("Failed to clear assignments: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Show result of clearing assignments
     */
    private void showClearAssignmentResult(int totalCourses, int clearedCount, long durationMs) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);

        alert.setTitle("Clear All Assignments Complete");
        alert.setHeaderText("✓ All assignments have been cleared");

        StringBuilder content = new StringBuilder();
        content.append(String.format("Total courses: %d\n", totalCourses));
        content.append(String.format("Assignments cleared: %d\n", clearedCount));
        content.append(String.format("Already unassigned: %d\n", totalCourses - clearedCount));
        content.append(String.format("⏱ Duration: %dms\n\n", durationMs));
        content.append("All courses are now ready for reassignment.");

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Show assignment result dialog
     */
    private void showAssignmentResult(String title,
                                      com.heronix.service.SmartTeacherAssignmentService.AssignmentResult result) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            result.isSuccessful() ?
                javafx.scene.control.Alert.AlertType.INFORMATION :
                javafx.scene.control.Alert.AlertType.WARNING
        );

        alert.setTitle(title + " Complete");
        alert.setHeaderText(result.getMessage());

        StringBuilder content = new StringBuilder();
        content.append(String.format("Processed: %d courses\n", result.getTotalCoursesProcessed()));
        content.append(String.format("✓ Assigned: %d\n", result.getCoursesAssigned()));
        content.append(String.format("✗ Failed: %d\n", result.getCoursesFailed()));
        content.append(String.format("⏱ Duration: %dms\n\n", result.getDurationMs()));

        if (result.hasWarnings()) {
            content.append("⚠ WARNINGS:\n");
            for (String warning : result.getWarnings()) {
                content.append("  • " + warning + "\n");
            }
            content.append("\n");
        }

        if (result.hasErrors()) {
            content.append("❌ ERRORS:\n");
            for (String error : result.getErrors()) {
                content.append("  • " + error + "\n");
            }
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Show combined assignment result (teachers + rooms)
     */
    private void showCombinedAssignmentResult(
        com.heronix.service.SmartTeacherAssignmentService.AssignmentResult teacherResult,
        com.heronix.service.SmartTeacherAssignmentService.AssignmentResult roomResult) {

        // Create custom dialog with ScrollPane to handle long content
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Smart Assignment Complete");
        dialog.setHeaderText("Teachers and Rooms Assigned");

        // Build content text
        StringBuilder content = new StringBuilder();

        content.append("═══ TEACHER ASSIGNMENT ═══\n");
        content.append(String.format("✓ Assigned: %d of %d courses\n",
            teacherResult.getCoursesAssigned(), teacherResult.getTotalCoursesProcessed()));
        content.append(String.format("✗ Failed: %d\n", teacherResult.getCoursesFailed()));
        content.append(String.format("⏱ Duration: %dms\n\n", teacherResult.getDurationMs()));

        content.append("═══ ROOM ASSIGNMENT ═══\n");
        content.append(String.format("✓ Assigned: %d of %d courses\n",
            roomResult.getCoursesAssigned(), roomResult.getTotalCoursesProcessed()));
        content.append(String.format("✗ Failed: %d\n", roomResult.getCoursesFailed()));
        content.append(String.format("⏱ Duration: %dms\n\n", roomResult.getDurationMs()));

        int totalAssigned = teacherResult.getCoursesAssigned() + roomResult.getCoursesAssigned();
        int totalFailed = teacherResult.getCoursesFailed() + roomResult.getCoursesFailed();

        content.append("═══ SUMMARY ═══\n");
        content.append(String.format("Total Assignments: %d\n", totalAssigned));
        content.append(String.format("Total Failed: %d\n", totalFailed));

        if (teacherResult.hasWarnings() || roomResult.hasWarnings()) {
            content.append("\n⚠ WARNINGS:\n");
            for (String warning : teacherResult.getWarnings()) {
                content.append("  • [Teacher] " + warning + "\n");
            }
            for (String warning : roomResult.getWarnings()) {
                content.append("  • [Room] " + warning + "\n");
            }
        }

        // Create TextArea with content (read-only, monospaced font)
        javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(content.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        textArea.setPrefWidth(600);
        textArea.setPrefHeight(400);

        // Wrap in ScrollPane (though TextArea already has scrolling, this ensures proper sizing)
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefViewportWidth(600);
        scrollPane.setPrefViewportHeight(400);
        scrollPane.setMaxHeight(600); // Limit max height to fit most screens

        // Set dialog content
        dialog.getDialogPane().setContent(scrollPane);

        // Add OK button
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.OK);

        dialog.showAndWait();
    }

    // ========================================================================
    // AI Schedule Health Methods (Phase 3)
    // ========================================================================

    /**
     * Load AI status and show/hide widget based on availability
     */
    private void loadAIStatus() {
        // Check if AI is available
        if (backgroundScheduleAnalyzer == null || ollamaAIService == null) {
            // AI not enabled - hide widget
            if (aiStatusWidget != null) {
                aiStatusWidget.setVisible(false);
                aiStatusWidget.setManaged(false);
            }
            log.debug("AI services not available - widget hidden");
            return;
        }

        // Show widget
        if (aiStatusWidget != null) {
            aiStatusWidget.setVisible(true);
            aiStatusWidget.setManaged(true);
        }

        // Load last analysis
        CompletableFuture.runAsync(() -> {
            try {
                com.heronix.model.dto.AIAnalysisResult lastAnalysis =
                    backgroundScheduleAnalyzer.getLastAnalysis();
                java.time.LocalDateTime lastAnalysisTime =
                    backgroundScheduleAnalyzer.getLastAnalysisTime();

                Platform.runLater(() -> updateAIDisplay(lastAnalysis, lastAnalysisTime));

            } catch (Exception e) {
                log.error("Error loading AI status", e);
                Platform.runLater(() -> {
                    if (aiHealthScoreLabel != null) aiHealthScoreLabel.setText("--");
                    if (aiHealthStatusLabel != null) aiHealthStatusLabel.setText("Error");
                });
            }
        });
    }

    /**
     * Update AI display with analysis results
     */
    private void updateAIDisplay(com.heronix.model.dto.AIAnalysisResult analysis,
                                  java.time.LocalDateTime analysisTime) {

        if (analysis == null) {
            // No analysis yet
            safeSetText(aiHealthScoreLabel, "--");
            safeSetText(aiHealthStatusLabel, "Not analyzed");
            safeSetText(aiLastAnalysisLabel, "Last analysis: Never");
            safeSetText(aiCriticalIssuesLabel, "0");
            safeSetText(aiWarningsLabel, "0");
            safeSetText(aiSuggestionsLabel, "0");
            safeSetText(aiSummaryLabel, "No analysis available. Click 'Analyze Now' to start.");

            if (aiHealthProgressBar != null) {
                aiHealthProgressBar.setProgress(0);
            }
            return;
        }

        // Health score
        int healthScore = analysis.getHealthScore();
        safeSetText(aiHealthScoreLabel, String.valueOf(healthScore));

        // Health status with icon
        String statusText;
        String statusColor;
        if (healthScore >= 90) {
            statusText = "✅ Excellent";
            statusColor = "-fx-text-fill: #4CAF50;";
        } else if (healthScore >= 75) {
            statusText = "🟢 Good";
            statusColor = "-fx-text-fill: #8BC34A;";
        } else if (healthScore >= 60) {
            statusText = "🟡 Fair";
            statusColor = "-fx-text-fill: #FFC107;";
        } else if (healthScore >= 40) {
            statusText = "🟠 Poor";
            statusColor = "-fx-text-fill: #FF9800;";
        } else {
            statusText = "🔴 Critical";
            statusColor = "-fx-text-fill: #F44336;";
        }

        if (aiHealthStatusLabel != null) {
            aiHealthStatusLabel.setText(statusText);
            aiHealthStatusLabel.setStyle(statusColor);
        }

        if (aiHealthScoreLabel != null) {
            aiHealthScoreLabel.setStyle(statusColor);
        }

        // Progress bar
        if (aiHealthProgressBar != null) {
            aiHealthProgressBar.setProgress(healthScore / 100.0);

            // Color based on health
            String barStyle;
            if (healthScore >= 75) {
                barStyle = "-fx-accent: #4CAF50;"; // Green
            } else if (healthScore >= 50) {
                barStyle = "-fx-accent: #FFC107;"; // Yellow
            } else {
                barStyle = "-fx-accent: #F44336;"; // Red
            }
            aiHealthProgressBar.setStyle(barStyle);
        }

        // Last analysis time
        if (analysisTime != null) {
            java.time.Duration timeSince = java.time.Duration.between(analysisTime, java.time.LocalDateTime.now());
            String timeText;
            if (timeSince.toMinutes() < 1) {
                timeText = "Last analysis: Just now";
            } else if (timeSince.toMinutes() < 60) {
                timeText = String.format("Last analysis: %d min ago", timeSince.toMinutes());
            } else {
                timeText = String.format("Last analysis: %d hours ago", timeSince.toHours());
            }
            safeSetText(aiLastAnalysisLabel, timeText);
        }

        // Issue counts
        safeSetText(aiCriticalIssuesLabel, String.valueOf(analysis.getCriticalIssues().size()));
        safeSetText(aiWarningsLabel, String.valueOf(analysis.getWarnings().size()));
        safeSetText(aiSuggestionsLabel, String.valueOf(analysis.getSuggestions().size()));

        // Summary (first 500 chars)
        String summary = analysis.getSummary();
        if (summary != null && summary.length() > 500) {
            summary = summary.substring(0, 497) + "...";
        }
        safeSetText(aiSummaryLabel, summary != null ? summary : "Analysis complete.");

        // Show alert if critical issues detected
        if (analysis.hasCriticalIssues()) {
            showCriticalIssuesAlert(analysis);
        }
    }

    /**
     * Trigger manual AI analysis
     */
    @FXML
    public void triggerAIAnalysis() {
        if (backgroundScheduleAnalyzer == null) {
            showError("AI analysis not available. Please enable AI in configuration.");
            return;
        }

        log.info("Manual AI analysis triggered from Dashboard");

        // Disable button during analysis
        if (aiAnalyzeNowButton != null) {
            aiAnalyzeNowButton.setDisable(true);
            aiAnalyzeNowButton.setText("⏳ Analyzing...");
        }

        CompletableFuture.runAsync(() -> {
            try {
                com.heronix.model.dto.AIAnalysisResult result =
                    backgroundScheduleAnalyzer.analyzeNow();

                Platform.runLater(() -> {
                    updateAIDisplay(result, java.time.LocalDateTime.now());

                    if (aiAnalyzeNowButton != null) {
                        aiAnalyzeNowButton.setDisable(false);
                        aiAnalyzeNowButton.setText("🔄 Analyze Now");
                    }

                    // Show completion notification
                    javafx.scene.control.Alert info = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                    info.setTitle("Analysis Complete");
                    info.setHeaderText("AI Schedule Analysis Finished");
                    info.setContentText(String.format("Health Score: %d/100\nIssues: %d critical, %d warnings",
                        result.getHealthScore(),
                        result.getCriticalIssues().size(),
                        result.getWarnings().size()));
                    info.show();
                });

            } catch (Exception e) {
                log.error("Error during AI analysis", e);
                Platform.runLater(() -> {
                    if (aiAnalyzeNowButton != null) {
                        aiAnalyzeNowButton.setDisable(false);
                        aiAnalyzeNowButton.setText("🔄 Analyze Now");
                    }
                    showError("Analysis failed: " + e.getMessage());
                });
            }
        });
    }

    /**
     * View AI details dialog
     */
    @FXML
    public void viewAIDetails() {
        if (backgroundScheduleAnalyzer == null) {
            showError("AI analysis not available.");
            return;
        }

        com.heronix.model.dto.AIAnalysisResult lastAnalysis =
            backgroundScheduleAnalyzer.getLastAnalysis();

        if (lastAnalysis == null) {
            javafx.scene.control.Alert info = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
            info.setTitle("AI Analysis");
            info.setHeaderText("No Analysis Available");
            info.setContentText("No analysis available yet.\nClick 'Analyze Now' to run first analysis.");
            info.showAndWait();
            return;
        }

        // Show detailed dialog
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);

        alert.setTitle("AI Schedule Analysis Details");
        alert.setHeaderText(String.format("Health Score: %d/100 - %s",
            lastAnalysis.getHealthScore(),
            lastAnalysis.getHealthScore() >= 75 ? "Good" : "Needs Attention"));

        StringBuilder content = new StringBuilder();

        // Critical Issues
        if (!lastAnalysis.getCriticalIssues().isEmpty()) {
            content.append("🔴 CRITICAL ISSUES (" + lastAnalysis.getCriticalIssues().size() + "):\n");
            for (com.heronix.model.dto.AIIssue issue : lastAnalysis.getCriticalIssues()) {
                content.append(String.format("  • %s: %s\n", issue.getType(), issue.getDescription()));
                if (issue.getSuggestedAction() != null) {
                    content.append(String.format("    → %s\n", issue.getSuggestedAction()));
                }
            }
            content.append("\n");
        }

        // Warnings
        if (!lastAnalysis.getWarnings().isEmpty()) {
            content.append("⚠️ WARNINGS (" + lastAnalysis.getWarnings().size() + "):\n");
            for (com.heronix.model.dto.AIIssue issue : lastAnalysis.getWarnings()) {
                content.append(String.format("  • %s: %s\n", issue.getType(), issue.getDescription()));
            }
            content.append("\n");
        }

        // Suggestions
        if (!lastAnalysis.getSuggestions().isEmpty()) {
            content.append("💡 SUGGESTIONS (" + lastAnalysis.getSuggestions().size() + "):\n");
            for (com.heronix.model.dto.AIIssue issue : lastAnalysis.getSuggestions()) {
                content.append(String.format("  • %s\n", issue.getDescription()));
            }
            content.append("\n");
        }

        if (content.length() == 0) {
            content.append("✅ No issues detected!\nSchedule is in excellent condition.");
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Show alert for critical issues
     */
    private void showCriticalIssuesAlert(com.heronix.model.dto.AIAnalysisResult analysis) {
        // Only show once per analysis (track last shown)
        if (analysis.getTimestamp().equals(lastCriticalAlertTime)) {
            return;
        }
        lastCriticalAlertTime = analysis.getTimestamp();

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.WARNING);

        alert.setTitle("⚠️ Critical Schedule Issues Detected");
        alert.setHeaderText(String.format("Health Score: %d/100 - %d Critical Issues Found",
            analysis.getHealthScore(), analysis.getCriticalIssues().size()));

        StringBuilder content = new StringBuilder();
        content.append("The following critical issues require immediate attention:\n\n");

        int count = 0;
        for (com.heronix.model.dto.AIIssue issue : analysis.getCriticalIssues()) {
            if (count++ >= 5) {
                content.append(String.format("\n... and %d more issues",
                    analysis.getCriticalIssues().size() - 5));
                break;
            }
            content.append(String.format("🔴 %s\n", issue.getDescription()));
            if (issue.getSuggestedAction() != null) {
                content.append(String.format("   → %s\n\n", issue.getSuggestedAction()));
            }
        }

        content.append("\nClick 'View Details' in the AI widget for complete analysis.");

        alert.setContentText(content.toString());
        alert.show(); // Non-blocking
    }

    // Track last alert time to avoid duplicate alerts
    private java.time.LocalDateTime lastCriticalAlertTime = null;

    // ========================================================================
    // ACADEMIC PERFORMANCE (GPA) WIDGET METHODS
    // ========================================================================

    /**
     * Load GPA statistics for dashboard widget
     */
    private void loadGPAStatistics() {
        try {
            List<Student> allStudents = studentRepository.findAll();
            long totalStudents = allStudents.size();

            if (totalStudents == 0) {
                // No students - show zero state
                Platform.runLater(() -> updateGPAUINoStudents());
                return;
            }

            // Get students with GPA > 0 (have grades)
            List<Student> studentsWithGrades = allStudents.stream()
                .filter(s -> s.getCurrentGPA() != null && s.getCurrentGPA() > 0)
                .collect(Collectors.toList());

            long studentsWithGradesCount = studentsWithGrades.size();
            double studentsGradedPercent = totalStudents > 0
                ? (studentsWithGradesCount * 100.0 / totalStudents)
                : 0;

            // Calculate average GPA across all students with grades
            double averageGPA = studentsWithGrades.stream()
                .filter(s -> s.getCurrentGPA() != null && s.getCurrentGPA() > 0)
                .mapToDouble(Student::getCurrentGPA)
                .average()
                .orElse(0.0);

            // Honor Roll (GPA >= 3.5)
            long honorRollCount = studentsWithGrades.stream()
                .filter(s -> s.getCurrentGPA() >= 3.5)
                .count();
            double honorRollPercent = studentsWithGradesCount > 0
                ? (honorRollCount * 100.0 / studentsWithGradesCount)
                : 0;

            // High Honors (GPA >= 3.75)
            long highHonorsCount = studentsWithGrades.stream()
                .filter(s -> s.getCurrentGPA() >= 3.75)
                .count();
            double highHonorsPercent = studentsWithGradesCount > 0
                ? (highHonorsCount * 100.0 / studentsWithGradesCount)
                : 0;

            // Academic Warning (GPA < 2.0)
            long academicWarningCount = studentsWithGrades.stream()
                .filter(s -> s.getCurrentGPA() < 2.0)
                .count();
            double academicWarningPercent = studentsWithGradesCount > 0
                ? (academicWarningCount * 100.0 / studentsWithGradesCount)
                : 0;

            // Grade Distribution - count final grades by letter
            List<com.heronix.model.domain.StudentGrade> allGrades =
                studentGradeRepository.findAll();

            List<com.heronix.model.domain.StudentGrade> finalGrades = allGrades.stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsFinal()))
                .filter(g -> Boolean.TRUE.equals(g.getIncludeInGPA()))
                .collect(Collectors.toList());

            long totalGrades = finalGrades.size();

            long gradeACount = finalGrades.stream()
                .filter(g -> g.getLetterGrade() != null && g.getLetterGrade().startsWith("A"))
                .count();
            double gradeAPercent = totalGrades > 0 ? (gradeACount * 100.0 / totalGrades) : 0;

            long gradeBCount = finalGrades.stream()
                .filter(g -> g.getLetterGrade() != null && g.getLetterGrade().startsWith("B"))
                .count();
            double gradeBPercent = totalGrades > 0 ? (gradeBCount * 100.0 / totalGrades) : 0;

            long gradeCCount = finalGrades.stream()
                .filter(g -> g.getLetterGrade() != null && g.getLetterGrade().startsWith("C"))
                .count();
            double gradeCPercent = totalGrades > 0 ? (gradeCCount * 100.0 / totalGrades) : 0;

            long gradeDCount = finalGrades.stream()
                .filter(g -> g.getLetterGrade() != null && g.getLetterGrade().startsWith("D"))
                .count();
            double gradeDPercent = totalGrades > 0 ? (gradeDCount * 100.0 / totalGrades) : 0;

            long gradeFCount = finalGrades.stream()
                .filter(g -> g.getLetterGrade() != null && g.getLetterGrade().equals("F"))
                .count();
            double gradeFPercent = totalGrades > 0 ? (gradeFCount * 100.0 / totalGrades) : 0;

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                // Average GPA
                safeSetText(averageGPALabel, String.format("%.2f", averageGPA));

                // Honor Roll
                safeSetText(honorRollCountLabel, String.valueOf(honorRollCount));
                safeSetText(honorRollPercentLabel, String.format("(%.0f%%)", honorRollPercent));

                // High Honors
                safeSetText(highHonorsCountLabel, String.valueOf(highHonorsCount));
                safeSetText(highHonorsPercentLabel, String.format("(%.0f%%)", highHonorsPercent));

                // Academic Warning
                safeSetText(academicWarningCountLabel, String.valueOf(academicWarningCount));
                safeSetText(academicWarningPercentLabel, String.format("(%.0f%%)", academicWarningPercent));

                // Students with Grades
                safeSetText(studentsWithGradesLabel, String.valueOf(studentsWithGradesCount));
                safeSetText(studentsGradedPercentLabel, String.format("(%.0f%%)", studentsGradedPercent));

                // Grade Distribution
                safeSetText(gradeACountLabel, String.valueOf(gradeACount));
                safeSetText(gradeAPercentLabel, String.format("(%.0f%%)", gradeAPercent));

                safeSetText(gradeBCountLabel, String.valueOf(gradeBCount));
                safeSetText(gradeBPercentLabel, String.format("(%.0f%%)", gradeBPercent));

                safeSetText(gradeCCountLabel, String.valueOf(gradeCCount));
                safeSetText(gradeCPercentLabel, String.format("(%.0f%%)", gradeCPercent));

                safeSetText(gradeDCountLabel, String.valueOf(gradeDCount));
                safeSetText(gradeDPercentLabel, String.format("(%.0f%%)", gradeDPercent));

                safeSetText(gradeFCountLabel, String.valueOf(gradeFCount));
                safeSetText(gradeFPercentLabel, String.format("(%.0f%%)", gradeFPercent));
            });

            log.info("GPA statistics loaded: Avg GPA: {:.2f}, Honor Roll: {}, High Honors: {}, Warnings: {}, Students with grades: {}",
                averageGPA, honorRollCount, highHonorsCount, academicWarningCount, studentsWithGradesCount);

        } catch (Exception e) {
            log.error("Error loading GPA statistics", e);
            Platform.runLater(() -> updateGPAUIError());
        }
    }

    /**
     * Update GPA UI when no students exist
     */
    private void updateGPAUINoStudents() {
        safeSetText(averageGPALabel, "0.00");
        safeSetText(honorRollCountLabel, "0");
        safeSetText(honorRollPercentLabel, "(0%)");
        safeSetText(highHonorsCountLabel, "0");
        safeSetText(highHonorsPercentLabel, "(0%)");
        safeSetText(academicWarningCountLabel, "0");
        safeSetText(academicWarningPercentLabel, "(0%)");
        safeSetText(studentsWithGradesLabel, "0");
        safeSetText(studentsGradedPercentLabel, "(0%)");
        safeSetText(gradeACountLabel, "0");
        safeSetText(gradeAPercentLabel, "(0%)");
        safeSetText(gradeBCountLabel, "0");
        safeSetText(gradeBPercentLabel, "(0%)");
        safeSetText(gradeCCountLabel, "0");
        safeSetText(gradeCPercentLabel, "(0%)");
        safeSetText(gradeDCountLabel, "0");
        safeSetText(gradeDPercentLabel, "(0%)");
        safeSetText(gradeFCountLabel, "0");
        safeSetText(gradeFPercentLabel, "(0%)");
    }

    /**
     * Update GPA UI on error
     */
    private void updateGPAUIError() {
        safeSetText(averageGPALabel, "ERR");
        safeSetText(honorRollCountLabel, "0");
        safeSetText(honorRollPercentLabel, "(0%)");
        safeSetText(highHonorsCountLabel, "0");
        safeSetText(highHonorsPercentLabel, "(0%)");
        safeSetText(academicWarningCountLabel, "0");
        safeSetText(academicWarningPercentLabel, "(0%)");
        safeSetText(studentsWithGradesLabel, "0");
        safeSetText(studentsGradedPercentLabel, "(0%)");
    }

    /**
     * Open Students view from GPA widget button
     */
    @FXML
    public void openStudentsView() {
        try {
            log.info("Opening Gradebook view from Academic Performance widget");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/Gradebook.fxml")
            );

            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Gradebook - Assignment Grades");
            stage.setScene(new javafx.scene.Scene(root, 1600, 900));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Gradebook view", e);
            showError("Failed to open Gradebook view: " + e.getMessage());
        }
    }

    // ========================================================================
    // SPED MANAGEMENT NAVIGATION - Phase 8C
    // ========================================================================

    /**
     * Open SPED Dashboard
     */
    @FXML
    public void openSPEDDashboard() {
        try {
            log.info("Opening SPED Dashboard");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/sped-dashboard.fxml")
            );

            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("SPED Dashboard - Special Education Management");
            stage.setScene(new javafx.scene.Scene(root, 1400, 900));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening SPED Dashboard", e);
            showError("Failed to open SPED Dashboard: " + e.getMessage());
        }
    }

    /**
     * Open IEP Management
     */
    @FXML
    public void openIEPManagement() {
        try {
            log.info("Opening IEP Management");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/iep-management.fxml")
            );

            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("IEP Management");
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening IEP Management", e);
            showError("Failed to open IEP Management: " + e.getMessage());
        }
    }

    /**
     * Open 504 Plan Management
     */
    @FXML
    public void open504PlanManagement() {
        try {
            log.info("Opening 504 Plan Management");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/plan504-management.fxml")
            );

            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("504 Plan Management");
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening 504 Plan Management", e);
            showError("Failed to open 504 Plan Management: " + e.getMessage());
        }
    }

    /**
     * Open Pull-Out Scheduling
     */
    @FXML
    public void openPullOutScheduling() {
        try {
            log.info("Opening Pull-Out Scheduling");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/pullout-scheduling.fxml")
            );

            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Pull-Out Service Scheduling");
            stage.setScene(new javafx.scene.Scene(root, 1400, 900));
            stage.show();

        } catch (Exception e) {
            log.error("Error opening Pull-Out Scheduling", e);
            showError("Failed to open Pull-Out Scheduling: " + e.getMessage());
        }
    }

    /**
     * Open SPED Compliance Reports
     */
    @FXML
    public void openSPEDComplianceReports() {
        log.info("Opening SPED Compliance Reports");
        showInfo("SPED Compliance Reports", "Compliance report generation will be implemented in a future version.");
    }

    private void showInfo(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========================================================================
    // BLOCK SCHEDULING METHODS - Phase 3: Block Scheduling MVP
    // ========================================================================

    /**
     * Open Block Schedule Generator Dialog
     */
    @FXML
    public void openBlockScheduleGenerator() {
        try {
            log.info("Opening Block Schedule Generator Dialog");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/dialogs/BlockScheduleGeneratorDialog.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.control.DialogPane dialogPane = loader.load();

            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog =
                new javafx.scene.control.Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Block Schedule Generator");

            // Show dialog and refresh on close
            dialog.showAndWait().ifPresent(response -> {
                if (response.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {
                    log.info("Block schedule generated successfully");
                    refreshDashboard();
                }
            });

        } catch (Exception e) {
            log.error("Error opening Block Schedule Generator", e);
            showError("Failed to open Block Schedule Generator: " + e.getMessage());
        }
    }

    /**
     * Open ODD/EVEN Day Assignment Dialog
     */
    @FXML
    public void openOddEvenDayAssignment() {
        try {
            log.info("Opening ODD/EVEN Day Assignment Dialog");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/dialogs/OddEvenDayAssignmentDialog.fxml")
            );

            // Use Spring context for controller factory
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }

            javafx.scene.control.DialogPane dialogPane = loader.load();

            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog =
                new javafx.scene.control.Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("ODD/EVEN Day Assignment");

            // Show dialog and refresh on close
            dialog.showAndWait().ifPresent(response -> {
                if (response.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {
                    log.info("ODD/EVEN day assignments updated successfully");
                    refreshDashboard();
                }
            });

        } catch (Exception e) {
            log.error("Error opening ODD/EVEN Day Assignment Dialog", e);
            showError("Failed to open ODD/EVEN Day Assignment Dialog: " + e.getMessage());
        }
    }

    // ========================================================================
    // GRADEBOOK NAVIGATION METHODS
    // ========================================================================

    /**
     * Open Admin Gradebook
     * Provides district-wide grade management with full audit trail
     */
    @FXML
    public void openAdminGradebook() {
        try {
            log.info("Opening Admin Gradebook from dashboard");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/AdminGradebook.fxml")
            );

            loader.setControllerFactory(applicationContext::getBean);

            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Administrator Gradebook - Heronix Scheduling System");
            stage.setScene(new javafx.scene.Scene(root, 1400, 900));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(dashboardRoot.getScene().getWindow());

            stage.show();

            log.info("Admin Gradebook opened successfully");

        } catch (Exception e) {
            log.error("Error opening Admin Gradebook", e);
            showError("Failed to open Admin Gradebook: " + e.getMessage());
        }
    }

    /**
     * Open Teacher Gradebook (Phase 3)
     * Placeholder for future teacher gradebook implementation
     */
    @FXML
    public void openTeacherGradebook() {
        log.info("Opening Teacher Gradebook from dashboard");
        if (mainController != null) {
            mainController.loadGradebook();
        } else {
            showInfo("Navigate to Gradebook", "Please navigate to:\n\nGrades → Gradebook\n\nThe Teacher Gradebook allows you to:\n• View and edit grades for all courses\n• Track student progress\n• Generate grade reports\n• Manage assignment weights");
        }
    }

    /**
     * Open Student Portal (Phase 5)
     * Placeholder for future student portal implementation
     */
    @FXML
    public void openStudentPortal() {
        log.info("Opening Student Portal information from dashboard");
        showInfo("Student Portal",
            "Student Portal Features:\n\n" +
            "The Student Portal will provide:\n" +
            "• View class schedules\n" +
            "• Check current grades\n" +
            "• Access course materials\n" +
            "• View attendance records\n" +
            "• Submit assignments\n\n" +
            "Portal Access:\n" +
            "Students can access the portal via:\n" +
            "• Web browser (coming soon)\n" +
            "• Mobile app (coming soon)\n\n" +
            "For now, use Students → View Student to see individual student information.");
    }

    // ========================================================================
    // ACADEMIC PLANNING WIDGET HANDLERS
    // ========================================================================

    /**
     * Open Academic Planning view
     */
    @FXML
    public void openAcademicPlanning() {
        log.info("Opening Academic Planning from dashboard");
        if (mainController != null) {
            mainController.loadAcademicPlanning();
        }
    }

    /**
     * View at-risk students without academic plans
     */
    @FXML
    public void viewAtRiskStudents() {
        log.info("Viewing at-risk students from dashboard");
        if (mainController != null) {
            mainController.loadStudents();
            showInfo("Filter At-Risk Students",
                "Students module loaded!\n\n" +
                "To view at-risk students:\n\n" +
                "1. Look for students with colored row backgrounds:\n" +
                "   • Light Red = Retention Risk (critically behind)\n" +
                "   • Light Orange = At Risk (behind schedule)\n" +
                "   • Light Green = On Track\n\n" +
                "2. Use the search/filter to find specific students\n\n" +
                "3. Click on a student to view detailed academic standing\n\n" +
                "Tip: Hover over student rows to see graduation progress tooltips");
        } else {
            showInfo("Navigate to Students",
                "To view at-risk students:\n\n" +
                "1. Navigate to: Students Module\n\n" +
                "2. Look for color-coded rows:\n" +
                "   • Red = Retention Risk\n" +
                "   • Orange = At Risk\n" +
                "   • Green = On Track\n\n" +
                "3. Use filters to show only at-risk students");
        }
    }

    /**
     * View all AI-generated course recommendations
     */
    @FXML
    public void viewRecommendations() {
        log.info("Opening Course Recommendations from dashboard");
        if (mainController != null) {
            mainController.loadCourseRecommendations();
        }
    }

    /**
     * Load academic planning metrics for dashboard widget
     */
    private void loadAcademicPlanningMetrics() {
        if (academicPlanRepository == null || courseRecommendationRepository == null) {
            return;
        }

        try {
            // Total Plans
            long totalPlans = academicPlanRepository.count();

            // Active Plans
            long activePlans = academicPlanRepository.countByActiveTrue();
            int activePlansPercent = totalPlans > 0 ? (int) ((activePlans * 100.0) / totalPlans) : 0;

            // Pending Approval
            long pendingApproval = academicPlanRepository.countByStatusAndActiveTrue(
                com.heronix.model.domain.AcademicPlan.PlanStatus.PENDING_APPROVAL);
            int pendingApprovalPercent = totalPlans > 0 ? (int) ((pendingApproval * 100.0) / totalPlans) : 0;

            // At-Risk Students - Students without academic plans
            long studentsWithoutPlans = academicPlanRepository != null ? academicPlanRepository.countStudentsWithoutPlans() : 0;
            long totalStudents = studentRepository.count();
            int atRiskPercent = totalStudents > 0 ? (int) ((studentsWithoutPlans * 100.0) / totalStudents) : 0;

            // AI Recommendations
            long pending = courseRecommendationRepository.countByStatusAndActiveTrue(
                com.heronix.model.domain.CourseRecommendation.RecommendationStatus.PENDING);

            // High Priority (priority 1-3)
            long highPriorityCount = courseRecommendationRepository.findAll().stream()
                    .filter(r -> r.getPriority() != null && r.getPriority() <= 3)
                    .filter(r -> r.getActive())
                    .count();

            // Accepted
            long accepted = courseRecommendationRepository.countByStatusAndActiveTrue(
                com.heronix.model.domain.CourseRecommendation.RecommendationStatus.ACCEPTED);

            // Update UI on JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                safeSetText(totalPlansLabel, String.valueOf(totalPlans));
                safeSetText(activePlansLabel, String.valueOf(activePlans));
                if (totalPlans > 0) {
                    safeSetText(activePlansPercentLabel, String.format("(%d%%)", activePlansPercent));
                }

                safeSetText(pendingApprovalLabel, String.valueOf(pendingApproval));
                if (totalPlans > 0) {
                    safeSetText(pendingApprovalPercentLabel, String.format("(%d%%)", pendingApprovalPercent));
                }

                safeSetText(atRiskStudentsLabel, String.valueOf(studentsWithoutPlans));
                if (totalStudents > 0) {
                    safeSetText(atRiskStudentsPercentLabel, String.format("(%d%%)", atRiskPercent));
                } else {
                    safeSetText(atRiskStudentsPercentLabel, "(0%)");
                }

                safeSetText(pendingRecommendationsLabel, String.valueOf(pending));
                safeSetText(highPriorityRecommendationsLabel, String.valueOf(highPriorityCount));
                safeSetText(acceptedRecommendationsLabel, String.valueOf(accepted));
            });

        } catch (Exception e) {
            log.error("Error loading academic planning metrics", e);
        }
    }
}
