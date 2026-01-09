package com.heronix.ui.controller;

import com.heronix.model.domain.AcademicYear;
import com.heronix.model.domain.GradeProgressionHistory;
import com.heronix.service.AcademicYearService;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.GradeProgressionHistoryRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Academic Year Management screen.
 *
 * Allows administrators to:
 * - View current academic year
 * - Create new academic years
 * - Progress to next year (grade progression)
 * - View statistics and history
 *
 * @author Heronix Scheduler Team
 */
@Controller
public class AcademicYearManagementController {

    private static final Logger log = LoggerFactory.getLogger(AcademicYearManagementController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Autowired
    private AcademicYearService academicYearService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GradeProgressionHistoryRepository progressionHistoryRepository;

    @Autowired
    private ApplicationContext springContext;

    // Current Year Card
    @FXML
    private Label currentYearLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label statusTextLabel;

    @FXML
    private Label startDateLabel;

    @FXML
    private Label endDateLabel;

    @FXML
    private Label graduationDateLabel;

    @FXML
    private Label graduatedLabel;

    // Student Statistics
    @FXML
    private Label grade9CountLabel;

    @FXML
    private Label grade10CountLabel;

    @FXML
    private Label grade11CountLabel;

    @FXML
    private Label grade12CountLabel;

    @FXML
    private Label totalStudentsLabel;

    // Academic Years Table
    @FXML
    private TableView<AcademicYear> yearsTable;

    @FXML
    private TableColumn<AcademicYear, String> yearNameColumn;

    @FXML
    private TableColumn<AcademicYear, LocalDate> startDateColumn;

    @FXML
    private TableColumn<AcademicYear, LocalDate> endDateColumn;

    @FXML
    private TableColumn<AcademicYear, LocalDate> graduationDateColumn;

    @FXML
    private TableColumn<AcademicYear, Boolean> activeColumn;

    @FXML
    private TableColumn<AcademicYear, Boolean> graduatedColumn;

    @FXML
    private TableColumn<AcademicYear, Void> actionsColumn;

    private ObservableList<AcademicYear> academicYears;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        log.info("Initializing Academic Year Management Controller");

        // Initialize table columns
        setupTableColumns();

        // Load data
        loadData();
    }

    /**
     * Setup table columns.
     */
    private void setupTableColumns() {
        yearNameColumn.setCellValueFactory(new PropertyValueFactory<>("yearName"));

        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startDateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DATE_FORMATTER));
                }
            }
        });

        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        endDateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DATE_FORMATTER));
                }
            }
        });

        graduationDateColumn.setCellValueFactory(new PropertyValueFactory<>("graduationDate"));
        graduationDateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("-");
                } else {
                    setText(date.format(DATE_FORMATTER));
                }
            }
        });

        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(active ? "✓ Active" : "");
                    setStyle(active ? "-fx-text-fill: #4CAF50; -fx-font-weight: bold;" : "");
                }
            }
        });

        graduatedColumn.setCellValueFactory(new PropertyValueFactory<>("graduated"));
        graduatedColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean graduated, boolean empty) {
                super.updateItem(graduated, empty);
                if (empty || graduated == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(graduated ? "✓ Graduated" : "In Progress");
                    setStyle(graduated ? "-fx-text-fill: #666;" : "-fx-text-fill: #2196F3;");
                }
            }
        });

        // Actions column with Set Active button
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button setActiveButton = new Button("Set Active");

            {
                setActiveButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                setActiveButton.setOnAction(event -> {
                    AcademicYear year = getTableView().getItems().get(getIndex());
                    handleSetActive(year);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AcademicYear year = getTableView().getItems().get(getIndex());
                    if (!year.isActive() && !year.isGraduated()) {
                        setGraphic(setActiveButton);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    /**
     * Load all data for the screen.
     */
    private void loadData() {
        log.info("Loading academic year data");

        // Load current academic year
        loadCurrentYear();

        // Load student statistics
        loadStudentStatistics();

        // Load all academic years
        loadAcademicYears();
    }

    /**
     * Load current academic year information.
     */
    private void loadCurrentYear() {
        AcademicYear currentYear = academicYearService.getActiveYear();

        if (currentYear != null) {
            AcademicYear year = currentYear;

            currentYearLabel.setText(year.getYearName());
            startDateLabel.setText(year.getStartDate().format(DATE_FORMATTER));
            endDateLabel.setText(year.getEndDate().format(DATE_FORMATTER));

            if (year.getGraduationDate() != null) {
                graduationDateLabel.setText(year.getGraduationDate().format(DATE_FORMATTER));
            } else {
                graduationDateLabel.setText("Not Set");
            }

            graduatedLabel.setText(year.isGraduated() ? "Yes" : "No");

            // Status indicator
            if (year.isGraduated()) {
                statusLabel.setText("●");
                statusLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 16px;");
                statusTextLabel.setText("Graduated");
                statusTextLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #999;");
            } else {
                statusLabel.setText("●");
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16px;");
                statusTextLabel.setText("Active");
                statusTextLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");
            }
        } else {
            currentYearLabel.setText("No Active Year");
            startDateLabel.setText("-");
            endDateLabel.setText("-");
            graduationDateLabel.setText("-");
            graduatedLabel.setText("-");

            statusLabel.setText("●");
            statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 16px;");
            statusTextLabel.setText("Not Set");
            statusTextLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F44336;");
        }
    }

    /**
     * Load student statistics by grade level.
     */
    private void loadStudentStatistics() {
        int grade9 = studentRepository.countByGradeLevelAndActiveTrue("9");
        int grade10 = studentRepository.countByGradeLevelAndActiveTrue("10");
        int grade11 = studentRepository.countByGradeLevelAndActiveTrue("11");
        int grade12 = studentRepository.countByGradeLevelAndActiveTrue("12");
        int total = grade9 + grade10 + grade11 + grade12;

        grade9CountLabel.setText(String.valueOf(grade9));
        grade10CountLabel.setText(String.valueOf(grade10));
        grade11CountLabel.setText(String.valueOf(grade11));
        grade12CountLabel.setText(String.valueOf(grade12));
        totalStudentsLabel.setText(String.valueOf(total));

        log.info("Student statistics: Grade 9={}, 10={}, 11={}, 12={}, Total={}",
                grade9, grade10, grade11, grade12, total);
    }

    /**
     * Load all academic years into table.
     */
    private void loadAcademicYears() {
        List<AcademicYear> years = academicYearService.getAllYears();
        academicYears = FXCollections.observableArrayList(years);
        yearsTable.setItems(academicYears);

        log.info("Loaded {} academic years", years.size());
    }

    /**
     * Handle Refresh button.
     */
    @FXML
    private void handleRefresh() {
        log.info("Refreshing academic year data");
        loadData();
        showInfo("Refreshed", "Academic year data has been refreshed.");
    }

    /**
     * Handle Progress to Next Year button.
     */
    @FXML
    private void handleProgressToNextYear() {
        log.info("Opening grade progression dialog");

        AcademicYear currentYear = academicYearService.getActiveYear();

        if (currentYear == null) {
            showError("No Active Year", "Please create and activate an academic year before progressing.");
            return;
        }

        if (currentYear.isGraduated()) {
            showError("Already Graduated", "The current academic year has already graduated. Please create a new year.");
            return;
        }

        try {
            // Load Grade Progression dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GradeProgression.fxml"));
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();

            GradeProgressionController controller = loader.getController();
            controller.setCurrentYear(currentYear);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Grade Progression");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));

            dialogStage.showAndWait();

            // Refresh data after dialog closes
            loadData();

        } catch (Exception e) {
            log.error("Failed to open grade progression dialog", e);
            showError("Dialog Error", "Failed to open grade progression dialog: " + e.getMessage());
        }
    }

    /**
     * Handle Create New Academic Year button.
     */
    @FXML
    private void handleCreateNewYear() {
        log.info("Creating new academic year");

        // Create dialog for new year
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Academic Year");
        dialog.setHeaderText("Create New Academic Year");
        dialog.setContentText("Year Name (e.g., 2024-2025):");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String yearName = result.get().trim();

            // Check if year already exists
            if (academicYearService.getYearByName(yearName) != null) {
                showError("Duplicate Year", "An academic year with this name already exists.");
                return;
            }

            // Ask for start date
            LocalDate startDate = askForDate("Start Date", "Enter start date (YYYY-MM-DD):");
            if (startDate == null) return;

            // Ask for end date
            LocalDate endDate = askForDate("End Date", "Enter end date (YYYY-MM-DD):");
            if (endDate == null) return;

            // Validate dates
            if (endDate.isBefore(startDate)) {
                showError("Invalid Dates", "End date must be after start date.");
                return;
            }

            // Ask for graduation date
            LocalDate graduationDate = askForDate("Graduation Date (Optional)", "Enter graduation date (YYYY-MM-DD) or leave empty:");

            // Create the academic year
            try {
                AcademicYear newYear = academicYearService.createAcademicYear(
                        yearName,
                        startDate,
                        endDate
                );

                // Set graduation date if provided
                if (graduationDate != null) {
                    academicYearService.scheduleGraduation(newYear.getId(), graduationDate);
                }

                log.info("Created new academic year: {}", yearName);
                showInfo("Success", "Academic year '" + yearName + "' created successfully.");

                // Ask if user wants to set it as active
                Alert confirmActive = new Alert(Alert.AlertType.CONFIRMATION);
                confirmActive.setTitle("Set Active?");
                confirmActive.setHeaderText("Set as Active Year?");
                confirmActive.setContentText("Do you want to set this as the active academic year?");

                Optional<ButtonType> confirmResult = confirmActive.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                    academicYearService.setActiveYear(newYear.getId());
                    showInfo("Activated", "Academic year '" + yearName + "' is now active.");
                }

                loadData();

            } catch (Exception e) {
                log.error("Failed to create academic year", e);
                showError("Creation Failed", "Failed to create academic year: " + e.getMessage());
            }
        }
    }

    /**
     * Handle View History button.
     */
    @FXML
    private void handleViewHistory() {
        log.info("Viewing grade progression history");

        List<GradeProgressionHistory> history = progressionHistoryRepository.findAllOrderByProgressionDateDesc();

        if (history.isEmpty()) {
            showInfo("No History", "No grade progression history found.");
            return;
        }

        // Create a simple text display of history
        StringBuilder sb = new StringBuilder();
        sb.append("Grade Progression History\n");
        sb.append("=".repeat(50)).append("\n\n");

        for (GradeProgressionHistory record : history) {
            sb.append("Year: ").append(record.getAcademicYear().getYearName()).append("\n");
            sb.append("Progression Date: ").append(record.getProgressionDate().format(DATE_FORMATTER)).append("\n");
            sb.append("Seniors Graduated: ").append(record.getSeniorsGraduated()).append("\n");
            sb.append("Students Promoted: ").append(record.getStudentsPromoted()).append("\n");
            sb.append("Enrollments Archived: ").append(record.getEnrollmentsArchived()).append("\n");
            sb.append("-".repeat(50)).append("\n");
        }

        // Show in alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Grade Progression History");
        alert.setHeaderText("Grade Progression History");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    /**
     * Handle Set Active for a year in the table.
     */
    private void handleSetActive(AcademicYear year) {
        log.info("Setting active year: {}", year.getYearName());

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Set Active Year");
        confirm.setHeaderText("Set Active Academic Year?");
        confirm.setContentText("Do you want to set '" + year.getYearName() + "' as the active academic year?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                academicYearService.setActiveYear(year.getId());
                showInfo("Success", "Academic year '" + year.getYearName() + "' is now active.");
                loadData();
            } catch (Exception e) {
                log.error("Failed to set active year", e);
                showError("Failed", "Failed to set active year: " + e.getMessage());
            }
        }
    }

    /**
     * Ask user for a date.
     */
    private LocalDate askForDate(String title, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(message);

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                return LocalDate.parse(result.get().trim());
            } catch (Exception e) {
                showError("Invalid Date", "Please enter date in YYYY-MM-DD format.");
                return null;
            }
        }

        // Allow empty for optional dates
        return null;
    }

    /**
     * Show info alert.
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show error alert.
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
