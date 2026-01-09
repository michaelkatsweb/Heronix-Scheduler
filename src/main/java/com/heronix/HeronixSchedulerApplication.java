package com.heronix;

import com.heronix.ui.controller.MainController;
import com.heronix.ui.controller.LoginController;
import com.heronix.security.SecurityContext;
import com.heronix.service.UserService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import com.heronix.config.ApplicationProperties;

/**
 * Heronix Scheduling System - Main Application Entry Point
 * Location: src/main/java/com/heronix/HeronixSchedulerApplication.java
 *
 * A product of Heronix Educational Systems LLC
 *
 * COMPLETE & IMPROVED VERSION
 * ✅ Auto-loads Dashboard on startup
 * ✅ Proper Spring context lifecycle
 * ✅ Enhanced error handling
 * ✅ Resource cleanup
 * ✅ Dashboard metrics caching enabled
 *
 * @author Heronix Educational Systems LLC
 * @version 5.0.0 - REBRANDED
 * @since 2025-12-21
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableCaching
public class HeronixSchedulerApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private Stage primaryStage;

    // ========================================================================
    // APPLICATION LIFECYCLE
    // ========================================================================

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        log.info("Initializing Spring Boot context...");

        // Configure Spring Boot for JavaFX desktop application (not web)
        SpringApplication app = new SpringApplication(HeronixSchedulerApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);

        springContext = app.run();
        log.info("Spring Boot context initialized successfully!");

        // Initialize default users (super admin)
        UserService userService = springContext.getBean(UserService.class);
        userService.initializeDefaultUsers();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            log.info("Showing login dialog...");

            // Show login dialog first
            boolean loginSuccessful = showLoginDialog();

            if (!loginSuccessful) {
                log.info("Login cancelled or failed. Exiting application.");
                Platform.exit();
                return;
            }

            log.info("Login successful. Loading main window...");

            // Load FXML with Spring controller factory
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Get controller reference
            MainController mainController = loader.getController();

            // Create scene with stylesheet
            Scene scene = new Scene(root, 1400, 900);
            loadStylesheet(scene);

            // Configure primary stage
            configurePrimaryStage(primaryStage, scene);

            // Show stage
            primaryStage.show();

            // ✅ CRITICAL FIX: Auto-load Dashboard after stage is visible
            Platform.runLater(() -> {
                try {
                    log.info("Auto-loading Dashboard...");
                    mainController.onApplicationReady();
                } catch (Exception e) {
                    log.error("Failed to auto-load Dashboard", e);
                }
            });

            log.info("Heronix Scheduling System started successfully!");

        } catch (Exception e) {
            log.error("Failed to start application", e);
            showErrorAndExit(e);
        }
    }

    @Override
    public void stop() {
        log.info("Shutting down Heronix Scheduling System...");

        if (springContext != null) {
            springContext.close();
            log.info("Spring context closed");
        }

        log.info("Application stopped");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Show login dialog and authenticate user
     * @return true if login successful, false otherwise
     */
    private boolean showLoginDialog() {
        try {
            // Load login dialog FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginDialog.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Get controller
            LoginController loginController = loader.getController();

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Heronix Scheduling System - Login");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);

            // Set scene
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            // Pass stage reference to controller
            loginController.setDialogStage(dialogStage);

            // Show and wait for result
            dialogStage.showAndWait();

            // Check if login was successful
            return loginController.isLoginSuccessful() && SecurityContext.isAuthenticated();

        } catch (Exception e) {
            log.error("Failed to show login dialog", e);
            return false;
        }
    }

    private void configurePrimaryStage(Stage stage, Scene scene) {
        stage.setTitle("Heronix Scheduling System - AI-Powered School Scheduling");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);

        // Set application icon (if available)
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
        } catch (Exception e) {
            log.warn("Application icon not found, using default");
        }

        // Handle window close request
        stage.setOnCloseRequest(event -> {
            log.info("Window close requested");
            Platform.exit();
        });
    }

    private void loadStylesheet(Scene scene) {
        try {
            // Load theme preference from settings
            String themeName = loadThemePreference();
            String cssFile;

            switch (themeName) {
                case "Dark":
                    cssFile = "/css/dark-theme.css";
                    log.info("Loading Dark theme (dark-theme.css)");
                    break;
                case "Light":
                    cssFile = "/css/light-theme.css";
                    log.info("Loading Light theme (light-theme.css)");
                    break;
                case "System":
                    // System theme defaults to light
                    cssFile = "/css/light-theme.css";
                    log.info("Loading System theme (defaulting to light-theme.css)");
                    break;
                default:
                    cssFile = "/css/light-theme.css";
                    log.warn("Unknown theme '{}', defaulting to light-theme.css", themeName);
            }

            String stylesheet = getClass().getResource(cssFile).toExternalForm();
            scene.getStylesheets().add(stylesheet);
            log.info("Loaded theme: {} ({})", themeName, cssFile);
        } catch (Exception e) {
            log.warn("Failed to load stylesheet, using fallback theme", e);
            try {
                // Fallback to light theme
                String fallback = getClass().getResource("/css/light-theme.css").toExternalForm();
                scene.getStylesheets().add(fallback);
            } catch (Exception ex) {
                log.error("Failed to load fallback stylesheet", ex);
            }
        }
    }

    /**
     * Load theme preference from settings file
     * @return Theme name: "Light", "Dark", or "System"
     */
    private String loadThemePreference() {
        try {
            java.io.File settingsFile = new java.io.File("config/app-settings.properties");
            if (settingsFile.exists()) {
                java.util.Properties settings = new java.util.Properties();
                settings.load(new java.io.FileInputStream(settingsFile));
                String theme = settings.getProperty("theme", "Light");
                log.info("Loaded theme preference from settings: {}", theme);
                return theme;
            }
        } catch (Exception e) {
            log.warn("Failed to load theme preference, using default", e);
        }
        return "Light"; // Default to Light theme
    }

    private void showErrorAndExit(Exception e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to start Heronix Scheduling System");
            alert.setContentText(
                "Error: " + e.getMessage() + "\n\n" +
                "Please check the logs for more details.\n" +
                "Application will now exit."
            );
            alert.showAndWait();
            Platform.exit();
        });
    }
}