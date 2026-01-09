package com.heronix.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Main View Controller - Application Dashboard
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/MainViewController.java
 * 
 * Central hub for navigating to all features:
 * - View Schedule
 * - Generate Schedule (AI)
 * - Import Data
 * - Manage Teachers
 * - Manage Courses
 * - Manage Rooms
 * - Reports & Analytics
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-10
 */
@Slf4j
@Controller
public class MainViewController {

    private ConfigurableApplicationContext springContext;

    @FXML
    private BorderPane mainContainer;
    @FXML
    private StackPane contentArea;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label subtitleLabel;

    /**
     * Set Spring context for loading other views
     */
    public void setSpringContext(ConfigurableApplicationContext context) {
        this.springContext = context;
    }

    /**
     * Initialize the main view
     */
    @FXML
    public void initialize() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║   EDUSCHEDULER PRO - MAIN DASHBOARD                            ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        welcomeLabel.setText("Welcome to Heronix Scheduling System");
        subtitleLabel.setText("Your Intelligent School Scheduling Solution");

        log.info("✓ Main Dashboard initialized");
    }

    /**
     * Navigate to View Schedule
     */
    @FXML
    private void handleViewSchedule() {
        log.info("Navigating to Schedule View...");
        loadView("/fxml/ScheduleView.fxml", "📅 Master Schedule View");
    }

    /**
     * Navigate to Generate Schedule (AI)
     */
    @FXML
    private void handleGenerateSchedule() {
        log.info("Navigating to Generate Schedule...");
        loadView("/fxml/ScheduleGenerator.fxml", "🤖 AI Schedule Generator");
    }

    /**
     * Navigate to Import Wizard
     */
    @FXML
    private void handleImportData() {
        log.info("Navigating to Import Wizard...");
        loadView("/fxml/ImportWizard.fxml", "📥 Import Wizard");
    }

    /**
     * Navigate to Manage Teachers
     */
    @FXML
    private void handleManageTeachers() {
        log.info("Navigating to Manage Teachers...");
        loadView("/fxml/TeacherManagement.fxml", "Teacher Management");
    }

    /**
     * Navigate to Manage Courses
     */
    @FXML
    private void handleManageCourses() {
        log.info("Navigating to Manage Courses...");
        loadView("/fxml/CourseManagement.fxml", "Course Management");
    }

    /**
     * Navigate to Manage Rooms
     */
    @FXML
    private void handleManageRooms() {
        log.info("Navigating to Manage Rooms...");
        loadView("/fxml/RoomManagement.fxml", "Room Management");
    }

    /**
     * Navigate to Reports
     */
    @FXML
    private void handleReports() {
        log.info("Navigating to Reports...");
        loadView("/fxml/ReportsAnalytics.fxml", "Reports & Analytics");
    }

    /**
     * Navigate to At-Risk Students Report
     */
    @FXML
    private void handleAtRiskStudents() {
        log.info("Navigating to At-Risk Students Report...");
        loadView("/fxml/at-risk-students.fxml", "At-Risk Students Report");
    }

    /**
     * Show settings
     */
    @FXML
    private void handleSettings() {
        log.info("Opening settings...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Settings.fxml"));
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();

            // Get controller and set the dialog stage
            SettingsController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Application Settings");
            dialogStage.setScene(new Scene(root));
            dialogStage.setMinWidth(900);
            dialogStage.setMinHeight(700);

            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            log.info("Settings dialog closed");

        } catch (Exception e) {
            log.error("Error loading Settings dialog", e);
            showError("Navigation Error",
                    "Failed to load Settings.\n\n" +
                            "Error: " + e.getMessage());
        }
    }

    /**
     * Show about dialog
     */
    @FXML
    private void handleAbout() {
        showInfo("About Heronix Scheduling System",
                "Heronix Scheduling System v1.0.0\n\n" +
                        "Intelligent School Scheduling System\n" +
                        "Powered by AI (OptaPlanner)\n\n" +
                        "© 2025 Heronix Scheduling System Team\n" +
                        "All rights reserved.\n\n" +
                        "Built with:\n" +
                        "• Java 17+\n" +
                        "• Spring Boot 3\n" +
                        "• JavaFX 21\n" +
                        "• OptaPlanner 9.40");
    }

    /**
     * Exit application
     */
    @FXML
    private void handleExit() {
        log.info("Exit requested");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Application");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved changes will be lost.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                log.info("Exiting application...");
                System.exit(0);
            }
        });
    }

    /**
     * Show Teachers tab - wrapper for menu compatibility
     */
    @FXML
    private void showTeachersTab() {
        handleManageTeachers();
    }

    /**
     * Show Co-Teacher Staff Management
     */
    @FXML
    private void showCoTeacherStaffTab() {
        log.info("Navigating to Co-Teacher Staff Management...");
        loadView("/fxml/CoTeacherStaffManagement.fxml", "Co-Teacher Staff Management");
    }

    /**
     * Show Paraprofessional Staff Management tab
     */
    @FXML
    private void showParaprofessionalStaffTab() {
        log.info("Navigating to Paraprofessional Staff Management...");
        loadView("/fxml/ParaprofessionalStaffManagement.fxml", "Paraprofessional Staff Management");
    }

    /**
     * Show Courses tab - wrapper for menu compatibility
     */
    @FXML
    private void showCoursesTab() {
        handleManageCourses();
    }

    /**
     * Show Rooms tab - wrapper for menu compatibility
     */
    @FXML
    private void showRoomsTab() {
        handleManageRooms();
    }

    /**
     * Show Student Enrollment Management
     */
    @FXML
    private void showStudentEnrollmentTab() {
        log.info("Navigating to Student Enrollment...");
        loadView("/fxml/StudentEnrollment.fxml", "Student Course Enrollment");
    }

    /**
     * Show Lunch Period Management
     */
    @FXML
    private void handleLunchPeriods() {
        log.info("Navigating to Lunch Period Management...");
        loadView("/fxml/LunchPeriodManagement.fxml", "Lunch Period Management");
    }

    /**
     * Show Special Event Blocks Management
     */
    @FXML
    private void handleSpecialEventBlocks() {
        log.info("Navigating to Special Event Blocks...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SpecialEventBlockDialog.fxml"));
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();

            // Get controller and set the dialog stage
            SpecialEventBlockController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Special Event Blocks Management");
            dialogStage.setScene(new Scene(root));
            dialogStage.setMinWidth(800);
            dialogStage.setMinHeight(600);

            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            log.info("Special Event Blocks dialog closed");

        } catch (Exception e) {
            log.error("Error loading Special Event Blocks dialog", e);
            showError("Navigation Error",
                    "Failed to load Special Event Blocks.\n\n" +
                            "Error: " + e.getMessage());
        }
    }

    /**
     * Load a view in a new window
     */
    private void loadView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            log.info("✓ Loaded view: {}", title);

        } catch (Exception e) {
            log.error("Error loading view: {}", fxmlPath, e);
            showError("Navigation Error",
                    "Failed to load view.\n\n" +
                            "Error: " + e.getMessage());
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
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Navigate to Notification Center
     */
    @FXML
    private void handleNotificationCenter() {
        log.info("Navigating to Notification Center...");
        loadView("/fxml/NotificationCenter.fxml", "🔔 Notification Center");
    }

    /**
     * Navigate to SIS Export
     */
    @FXML
    private void handleSISExport() {
        log.info("Navigating to SIS Export...");
        loadView("/fxml/SISExport.fxml", "📤 SIS Export");
    }

    /**
     * Navigate to SPED Scheduling Dashboard
     */
    @FXML
    private void handleSPEDScheduling() {
        log.info("Navigating to SPED Scheduling Dashboard...");
        loadView("/fxml/SPEDSchedulingDashboard.fxml", "📋 SPED Scheduling");
    }

    /**
     * Navigate to Enrollment Forecasting Dashboard
     */
    @FXML
    private void handleEnrollmentForecasting() {
        log.info("Navigating to Enrollment Forecasting Dashboard...");
        loadView("/fxml/ForecastingDashboard.fxml", "📊 Enrollment Forecasting");
    }
}