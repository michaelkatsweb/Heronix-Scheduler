package com.heronix.ui.controller;

import com.heronix.service.PurgeDatabaseService;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;

import javax.sql.DataSource;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Database Management Controller
 *
 * Provides integrated database management interface for:
 * - H2 Console access (embedded web server)
 * - PostgreSQL connection management
 * - Database backup/restore operations
 * - Connection information display
 * - Maintenance utilities
 *
 * @author Heronix Scheduler Team
 * @version 1.0
 */
@Slf4j
@Controller
public class DatabaseManagementController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private PurgeDatabaseService purgeDatabaseService;

    // H2 Console Server
    private Server h2WebServer;
    private static final int H2_CONSOLE_PORT = 8082;

    // FXML Components - Connection Info
    @FXML private Label connectionStatusLabel;
    @FXML private Label dbTypeLabel;
    @FXML private TextField jdbcUrlField;
    @FXML private Label usernameLabel;
    @FXML private Label dbSizeLabel;
    @FXML private Label tableCountLabel;
    @FXML private Label lastBackupLabel;

    // FXML Components - H2 Console
    @FXML private Label h2ConsoleStatusLabel;
    @FXML private Label h2ConsoleUrlLabel;
    @FXML private Button startH2ConsoleBtn;
    @FXML private Button stopH2ConsoleBtn;
    @FXML private Button openH2BrowserBtn;
    @FXML private TextArea h2ConsoleLogArea;

    // FXML Components - PostgreSQL
    @FXML private TextField pgHostField;
    @FXML private TextField pgPortField;
    @FXML private TextField pgDatabaseField;
    @FXML private TextField pgUsernameField;
    @FXML private PasswordField pgPasswordField;
    @FXML private Label pgStatusLabel;

    // FXML Components - Backup
    @FXML private Label backupStatusLabel;

    @FXML
    public void initialize() {
        log.info("Initializing Database Management Controller");

        // Load current database connection info
        loadConnectionInfo();

        // Check if H2 Console is already running
        checkH2ConsoleStatus();

        // Load PostgreSQL config if exists
        loadPostgreSQLConfig();

        log.info("Database Management Controller initialized successfully");
    }

    // ========================================================================
    // Connection Info
    // ========================================================================

    private void loadConnectionInfo() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Database type
            String dbProductName = metaData.getDatabaseProductName();
            dbTypeLabel.setText(dbProductName + " " + metaData.getDatabaseProductVersion());

            // JDBC URL
            String jdbcUrl = metaData.getURL();
            jdbcUrlField.setText(jdbcUrl);

            // Username
            String username = metaData.getUserName();
            usernameLabel.setText(username != null ? username : "N/A");

            // Connection status
            connectionStatusLabel.setText("● Connected");
            connectionStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");

            // Get table count
            loadTableCount(conn);

            // Calculate database size
            calculateDatabaseSize();

            log.info("✅ Database connection info loaded: {} at {}", dbProductName, jdbcUrl);

        } catch (Exception e) {
            log.error("Failed to load database connection info", e);
            connectionStatusLabel.setText("● Disconnected");
            connectionStatusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
            showError("Connection Error", "Failed to load database information: " + e.getMessage());
        }
    }

    private void loadTableCount(Connection conn) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            int count = 0;
            while (tables.next()) {
                count++;
            }
            tableCountLabel.setText(String.valueOf(count));
            log.info("📊 Found {} tables in database", count);
        } catch (Exception e) {
            log.error("Failed to count tables", e);
            tableCountLabel.setText("Error");
        }
    }

    private void calculateDatabaseSize() {
        CompletableFuture.runAsync(() -> {
            try {
                String jdbcUrl = environment.getProperty("spring.datasource.url");
                if (jdbcUrl != null && jdbcUrl.contains("h2:file:")) {
                    // Extract file path from JDBC URL
                    String filePath = jdbcUrl.replace("jdbc:h2:file:", "").split(";")[0];
                    Path dbFile = Paths.get(filePath + ".mv.db");

                    if (Files.exists(dbFile)) {
                        long sizeBytes = Files.size(dbFile);
                        String sizeFormatted = formatFileSize(sizeBytes);
                        Platform.runLater(() -> dbSizeLabel.setText(sizeFormatted));
                        log.info("📦 Database size: {} ({})", sizeFormatted, sizeBytes);
                    } else {
                        Platform.runLater(() -> dbSizeLabel.setText("File not found"));
                    }
                } else {
                    Platform.runLater(() -> dbSizeLabel.setText("N/A (Remote DB)"));
                }
            } catch (Exception e) {
                log.error("Failed to calculate database size", e);
                Platform.runLater(() -> dbSizeLabel.setText("Error"));
            }
        });
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @FXML
    private void handleCopyJdbcUrl() {
        String jdbcUrl = jdbcUrlField.getText();
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(jdbcUrl);
        clipboard.setContent(content);

        showInfo("Copied", "JDBC URL copied to clipboard!");
        log.info("📋 Copied JDBC URL to clipboard: {}", jdbcUrl);
    }

    @FXML
    private void handleRefreshInfo() {
        log.info("🔄 Refreshing database connection info");
        loadConnectionInfo();
        showInfo("Refreshed", "Database connection information refreshed successfully!");
    }

    // ========================================================================
    // H2 Console Management
    // ========================================================================

    private void checkH2ConsoleStatus() {
        if (h2WebServer != null && h2WebServer.isRunning(false)) {
            updateH2ConsoleStatus(true);
        } else {
            updateH2ConsoleStatus(false);
        }
    }

    @FXML
    private void handleStartH2Console() {
        log.info("🚀 Starting H2 Console web server on port {}", H2_CONSOLE_PORT);

        CompletableFuture.runAsync(() -> {
            try {
                // Start H2 web server
                h2WebServer = Server.createWebServer(
                    "-webPort", String.valueOf(H2_CONSOLE_PORT),
                    "-webAllowOthers", "false",  // Only localhost access
                    "-ifNotExists"  // Create database if not exists
                );

                h2WebServer.start();

                Platform.runLater(() -> {
                    updateH2ConsoleStatus(true);
                    appendH2Log("✅ H2 Console started successfully on port " + H2_CONSOLE_PORT);
                    appendH2Log("🌐 Access at: http://localhost:" + H2_CONSOLE_PORT);
                    appendH2Log("📊 JDBC URL: " + jdbcUrlField.getText());
                    appendH2Log("👤 Username: sa");
                    appendH2Log("🔑 Password: (leave empty)");
                    showInfo("H2 Console Started", "H2 Console is now running at:\nhttp://localhost:" + H2_CONSOLE_PORT);
                });

                log.info("✅ H2 Console web server started successfully");

            } catch (Exception e) {
                log.error("❌ Failed to start H2 Console", e);
                Platform.runLater(() -> {
                    appendH2Log("❌ ERROR: " + e.getMessage());
                    showError("H2 Console Error", "Failed to start H2 Console: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    private void handleStopH2Console() {
        log.info("⏹️ Stopping H2 Console web server");

        if (h2WebServer != null && h2WebServer.isRunning(false)) {
            h2WebServer.stop();
            h2WebServer.shutdown();
            h2WebServer = null;

            updateH2ConsoleStatus(false);
            appendH2Log("⏹️ H2 Console stopped");

            log.info("✅ H2 Console web server stopped successfully");
            showInfo("H2 Console Stopped", "H2 Console has been stopped.");
        }
    }

    @FXML
    private void handleOpenH2InBrowser() {
        try {
            String url = "http://localhost:" + H2_CONSOLE_PORT;
            Desktop.getDesktop().browse(new URI(url));
            appendH2Log("🌐 Opened browser to " + url);
            log.info("🌐 Opened H2 Console in browser: {}", url);
        } catch (Exception e) {
            log.error("Failed to open H2 Console in browser", e);
            showError("Browser Error", "Failed to open browser: " + e.getMessage());
        }
    }

    private void updateH2ConsoleStatus(boolean running) {
        Platform.runLater(() -> {
            if (running) {
                h2ConsoleStatusLabel.setText("● Running");
                h2ConsoleStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                h2ConsoleUrlLabel.setText("http://localhost:" + H2_CONSOLE_PORT);
                startH2ConsoleBtn.setDisable(true);
                stopH2ConsoleBtn.setDisable(false);
                openH2BrowserBtn.setDisable(false);
            } else {
                h2ConsoleStatusLabel.setText("● Stopped");
                h2ConsoleStatusLabel.setStyle("-fx-text-fill: #F44336;");
                h2ConsoleUrlLabel.setText("");
                startH2ConsoleBtn.setDisable(false);
                stopH2ConsoleBtn.setDisable(true);
                openH2BrowserBtn.setDisable(true);
            }
        });
    }

    private void appendH2Log(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            h2ConsoleLogArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }

    // ========================================================================
    // PostgreSQL Management
    // ========================================================================

    private void loadPostgreSQLConfig() {
        // Load saved PostgreSQL configuration from properties file (if exists)
        String home = System.getProperty("user.home");
        Path configFile = Paths.get(home, ".eduscheduler", "postgresql.properties");

        if (Files.exists(configFile)) {
            try {
                Properties props = new Properties();
                props.load(Files.newInputStream(configFile));

                pgHostField.setText(props.getProperty("host", "localhost"));
                pgPortField.setText(props.getProperty("port", "5432"));
                pgDatabaseField.setText(props.getProperty("database", "eduscheduler"));
                pgUsernameField.setText(props.getProperty("username", "postgres"));

                log.info("📂 Loaded PostgreSQL config from {}", configFile);
            } catch (Exception e) {
                log.warn("Failed to load PostgreSQL config: {}", e.getMessage());
            }
        }
    }

    @FXML
    private void handleTestPostgreSQL() {
        log.info("🔌 Testing PostgreSQL connection");
        pgStatusLabel.setText("Testing connection...");
        pgStatusLabel.setStyle("-fx-text-fill: #2196F3;");

        CompletableFuture.runAsync(() -> {
            try {
                String host = pgHostField.getText();
                String port = pgPortField.getText();
                String database = pgDatabaseField.getText();
                String username = pgUsernameField.getText();
                String password = pgPasswordField.getText();

                String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);

                // Test connection
                Class.forName("org.postgresql.Driver");
                try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                    DatabaseMetaData metaData = conn.getMetaData();
                    String version = metaData.getDatabaseProductVersion();

                    Platform.runLater(() -> {
                        pgStatusLabel.setText("✅ Connected successfully! PostgreSQL " + version);
                        pgStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                        showInfo("Connection Successful", "Successfully connected to PostgreSQL database!\nVersion: " + version);
                    });

                    log.info("✅ PostgreSQL connection test successful: {}", jdbcUrl);
                }

            } catch (ClassNotFoundException e) {
                Platform.runLater(() -> {
                    pgStatusLabel.setText("❌ PostgreSQL JDBC driver not found! Add dependency to pom.xml");
                    pgStatusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    showError("Driver Not Found", "PostgreSQL JDBC driver not available.\nPlease ensure the driver is included in your dependencies.");
                });
                log.error("❌ PostgreSQL JDBC driver not found", e);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    pgStatusLabel.setText("❌ Connection failed: " + e.getMessage());
                    pgStatusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    showError("Connection Failed", "Failed to connect to PostgreSQL:\n" + e.getMessage());
                });
                log.error("❌ PostgreSQL connection test failed", e);
            }
        });
    }

    @FXML
    private void handleSavePostgreSQLConfig() {
        try {
            String home = System.getProperty("user.home");
            Path configDir = Paths.get(home, ".eduscheduler");
            Files.createDirectories(configDir);

            Path configFile = configDir.resolve("postgresql.properties");

            Properties props = new Properties();
            props.setProperty("host", pgHostField.getText());
            props.setProperty("port", pgPortField.getText());
            props.setProperty("database", pgDatabaseField.getText());
            props.setProperty("username", pgUsernameField.getText());
            // Note: Password is NOT saved for security reasons

            props.store(Files.newOutputStream(configFile), "PostgreSQL Configuration");

            showInfo("Configuration Saved", "PostgreSQL configuration saved successfully!\nNote: Password is not saved for security reasons.");
            log.info("💾 Saved PostgreSQL config to {}", configFile);

        } catch (Exception e) {
            log.error("Failed to save PostgreSQL config", e);
            showError("Save Error", "Failed to save PostgreSQL configuration: " + e.getMessage());
        }
    }

    @FXML
    private void handleSwitchToPostgreSQL() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Switch to PostgreSQL");
        confirmation.setHeaderText("Switch Database to PostgreSQL");
        confirmation.setContentText("This will change the application database from H2 to PostgreSQL.\n\n" +
                "You will need to:\n" +
                "1. Stop the application\n" +
                "2. Update application.properties\n" +
                "3. Restart the application\n\n" +
                "Would you like to see instructions?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showPostgreSQLMigrationInstructions();
            }
        });
    }

    private void showPostgreSQLMigrationInstructions() {
        Alert instructions = new Alert(Alert.AlertType.INFORMATION);
        instructions.setTitle("PostgreSQL Migration Instructions");
        instructions.setHeaderText("How to Switch to PostgreSQL");
        instructions.setContentText(
                "1. Ensure PostgreSQL is installed and running\n" +
                "2. Create database: CREATE DATABASE eduscheduler;\n" +
                "3. Stop Heronix Scheduler application\n" +
                "4. Edit application.properties:\n" +
                "   - Comment out H2 datasource properties\n" +
                "   - Uncomment PostgreSQL datasource properties\n" +
                "   - Update host, port, username, password\n" +
                "5. Restart Heronix Scheduler application\n" +
                "6. Data will be automatically migrated via Hibernate\n\n" +
                "For detailed instructions, see documentation."
        );
        instructions.showAndWait();
    }

    // ========================================================================
    // Database Backup/Restore
    // ========================================================================

    @FXML
    private void handleBackupDatabase() {
        log.info("💾 Starting database backup");

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Backup Location");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedDir = dirChooser.showDialog(null);
        if (selectedDir == null) {
            return;
        }

        backupStatusLabel.setText("Creating backup...");

        CompletableFuture.runAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String backupFileName = "eduscheduler_backup_" + timestamp;

                // Get current database file path
                String jdbcUrl = environment.getProperty("spring.datasource.url");
                if (jdbcUrl != null && jdbcUrl.contains("h2:file:")) {
                    String dbPath = jdbcUrl.replace("jdbc:h2:file:", "").split(";")[0];
                    Path sourceFile = Paths.get(dbPath + ".mv.db");

                    if (Files.exists(sourceFile)) {
                        Path backupFile = selectedDir.toPath().resolve(backupFileName + ".mv.db");
                        Files.copy(sourceFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

                        // Also backup trace file if exists
                        Path traceFile = Paths.get(dbPath + ".trace.db");
                        if (Files.exists(traceFile)) {
                            Path backupTrace = selectedDir.toPath().resolve(backupFileName + ".trace.db");
                            Files.copy(traceFile, backupTrace, StandardCopyOption.REPLACE_EXISTING);
                        }

                        long backupSize = Files.size(backupFile);

                        Platform.runLater(() -> {
                            backupStatusLabel.setText("✅ Backup created: " + backupFileName + " (" + formatFileSize(backupSize) + ")");
                            lastBackupLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                            lastBackupLabel.setStyle("-fx-text-fill: #4CAF50;");
                            showInfo("Backup Successful", "Database backup created successfully!\n\nLocation: " + backupFile);
                        });

                        log.info("✅ Database backup created: {}", backupFile);
                    } else {
                        throw new IOException("Database file not found: " + sourceFile);
                    }
                } else {
                    throw new UnsupportedOperationException("Backup not supported for remote databases");
                }

            } catch (Exception e) {
                log.error("❌ Database backup failed", e);
                Platform.runLater(() -> {
                    backupStatusLabel.setText("❌ Backup failed: " + e.getMessage());
                    showError("Backup Failed", "Failed to create database backup:\n" + e.getMessage());
                });
            }
        });
    }

    @FXML
    private void handleRestoreDatabase() {
        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setTitle("Restore Database");
        warning.setHeaderText("⚠️ WARNING: This will overwrite your current database!");
        warning.setContentText("All current data will be replaced with the backup.\nMake sure you have a recent backup of your current data.\n\nContinue?");

        warning.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performDatabaseRestore();
            }
        });
    }

    private void performDatabaseRestore() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Backup File");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("H2 Database Files", "*.mv.db")
        );

        File backupFile = fileChooser.showOpenDialog(null);
        if (backupFile == null) {
            return;
        }

        // Restore requires application restart
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Restore Requires Restart");
        info.setHeaderText("Database Restore");
        info.setContentText("To restore the database, the application needs to be restarted.\n\n" +
                "Please:\n" +
                "1. Close the application\n" +
                "2. Replace the database file manually\n" +
                "3. Restart the application\n\n" +
                "Backup file selected: " + backupFile.getName());
        info.showAndWait();

        log.info("📂 User selected backup file for restore: {}", backupFile);
    }

    @FXML
    private void handleOpenBackupFolder() {
        try {
            String home = System.getProperty("user.home");
            Desktop.getDesktop().open(new File(home));
            log.info("📁 Opened backup folder: {}", home);
        } catch (Exception e) {
            log.error("Failed to open backup folder", e);
            showError("Error", "Failed to open backup folder: " + e.getMessage());
        }
    }

    // ========================================================================
    // Database Maintenance
    // ========================================================================

    @FXML
    private void handleViewStats() {
        showInfo("Database Statistics", "Feature coming soon!\n\nWill display:\n- Table row counts\n- Index information\n- Storage statistics\n- Performance metrics");
    }

    @FXML
    private void handleAnalyzeTables() {
        showInfo("Analyze Tables", "Feature coming soon!\n\nWill analyze:\n- Table structures\n- Index usage\n- Query performance\n- Optimization suggestions");
    }

    @FXML
    private void handleVacuumDatabase() {
        showInfo("Vacuum Database", "Feature coming soon!\n\nWill perform:\n- Database cleanup\n- Space reclamation\n- Index optimization\n- Performance tuning");
    }

    @FXML
    private void handlePurgeDatabase() {
        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setTitle("⚠️ DANGER: Purge Database");
        warning.setHeaderText("This will DELETE ALL DATA!");
        warning.setContentText("All teachers, students, courses, rooms, schedules, and settings will be permanently deleted.\n\nThis action CANNOT be undone!\n\nType 'DELETE ALL DATA' to confirm:");

        TextField confirmField = new TextField();
        warning.getDialogPane().setContent(confirmField);

        warning.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && "DELETE ALL DATA".equals(confirmField.getText())) {
                // Call existing purge database functionality
                try {
                    log.warn("⚠️ User confirmed database purge - executing deletion of all data");

                    // Show progress alert
                    Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
                    progressAlert.setTitle("Purging Database");
                    progressAlert.setHeaderText("Deleting all data...");
                    TextArea progressArea = new TextArea();
                    progressArea.setEditable(false);
                    progressArea.setPrefRowCount(10);
                    progressAlert.getDialogPane().setContent(progressArea);
                    progressAlert.show();

                    // Execute purge in background thread
                    new Thread(() -> {
                        try {
                            purgeDatabaseService.purgeAllData(message -> {
                                Platform.runLater(() -> {
                                    progressArea.appendText(message + "\n");
                                    log.info("Purge progress: " + message);
                                });
                            });

                            Platform.runLater(() -> {
                                progressAlert.close();
                                showInfo("Purge Complete", "All data has been successfully deleted from the database.\n\nThe database is now empty and ready for fresh data.");
                                log.info("✅ Database purge completed successfully");
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                progressAlert.close();
                                log.error("❌ Database purge failed", e);
                                CopyableErrorDialog.showError("Purge Failed", "Failed to purge database: " + e.getMessage());
                            });
                        }
                    }).start();

                } catch (Exception e) {
                    log.error("Error initiating database purge", e);
                    CopyableErrorDialog.showError("Purge Error", "Failed to start database purge: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleResetDatabase() {
        showInfo("Reset Database", "Feature coming soon!\n\nWill:\n- Clear all data\n- Recreate tables\n- Load default settings\n- Create admin user");
    }

    // ========================================================================
    // Quick Actions
    // ========================================================================

    @FXML
    private void handleExportSchema() {
        showInfo("Export Schema", "Feature coming soon!\n\nWill export:\n- Table structures\n- Indexes\n- Constraints\n- Relationships");
    }

    @FXML
    private void handleImportSQL() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select SQL File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("SQL Files", "*.sql")
        );

        File sqlFile = fileChooser.showOpenDialog(null);
        if (sqlFile != null) {
            showInfo("Import SQL", "Feature coming soon!\n\nSelected file: " + sqlFile.getName());
            log.info("📥 User selected SQL file for import: {}", sqlFile);
        }
    }

    @FXML
    private void handleViewDocumentation() {
        try {
            // Open DATABASE_ACCESS_GUIDE.md
            Path docPath = Paths.get("H:/Heronix Scheduler/DATABASE_ACCESS_GUIDE.md");
            if (Files.exists(docPath)) {
                Desktop.getDesktop().open(docPath.toFile());
                log.info("📖 Opened database documentation");
            } else {
                showInfo("Documentation", "Database Access Guide\n\nDocumentation file not found at:\n" + docPath);
            }
        } catch (Exception e) {
            log.error("Failed to open documentation", e);
            showError("Error", "Failed to open documentation: " + e.getMessage());
        }
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Cleanup on controller destruction
    public void shutdown() {
        if (h2WebServer != null && h2WebServer.isRunning(false)) {
            log.info("Shutting down H2 Console web server");
            h2WebServer.stop();
            h2WebServer.shutdown();
        }
    }
}
