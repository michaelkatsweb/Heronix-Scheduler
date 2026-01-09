package com.heronix.controller;

import com.heronix.model.domain.CourseRecommendation;
import com.heronix.model.domain.Student;
import com.heronix.service.CourseRecommendationService;
import com.heronix.repository.StudentRepository;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for Course Recommendation Dashboard
 * Manages AI-generated course recommendations for students
 */
@Slf4j
@Controller
public class CourseRecommendationDashboardController {

    @Autowired
    private CourseRecommendationService courseRecommendationService;

    @Autowired
    private StudentRepository studentRepository;

    // ========================================================================
    // FXML Components - Top Section
    // ========================================================================
    @FXML
    private ComboBox<Student> studentFilterComboBox;
    @FXML
    private ComboBox<String> statusFilterComboBox;
    @FXML
    private ComboBox<String> priorityFilterComboBox;
    @FXML
    private ComboBox<String> typeFilterComboBox;

    // Summary Statistics
    @FXML
    private Label totalRecommendationsLabel;
    @FXML
    private Label pendingRecommendationsLabel;
    @FXML
    private Label acceptedRecommendationsLabel;
    @FXML
    private Label rejectedRecommendationsLabel;

    // ========================================================================
    // FXML Components - Table
    // ========================================================================
    @FXML
    private TableView<CourseRecommendation> recommendationsTable;
    @FXML
    private TableColumn<CourseRecommendation, String> studentColumn;
    @FXML
    private TableColumn<CourseRecommendation, String> courseColumn;
    @FXML
    private TableColumn<CourseRecommendation, String> schoolYearColumn;
    @FXML
    private TableColumn<CourseRecommendation, String> typeColumn;
    @FXML
    private TableColumn<CourseRecommendation, Integer> priorityColumn;
    @FXML
    private TableColumn<CourseRecommendation, Double> confidenceColumn;
    @FXML
    private TableColumn<CourseRecommendation, String> statusColumn;
    @FXML
    private TableColumn<CourseRecommendation, String> reasonColumn;

    @FXML
    private Label statusLabel;

    // ========================================================================
    // FXML Components - Details Panel
    // ========================================================================
    // Student Info
    @FXML
    private Label detailStudentNameLabel;
    @FXML
    private Label detailGradeLevelLabel;
    @FXML
    private Label detailGPALabel;

    // Course Info
    @FXML
    private Label detailCourseNameLabel;
    @FXML
    private Label detailCourseCodeLabel;
    @FXML
    private Label detailSubjectLabel;
    @FXML
    private Label detailCreditsLabel;

    // Recommendation Analysis
    @FXML
    private Label detailTypeLabel;
    @FXML
    private Label detailPriorityLabel;
    @FXML
    private ProgressBar detailPriorityBar;
    @FXML
    private Label detailConfidenceLabel;
    @FXML
    private ProgressBar detailConfidenceBar;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailSchoolYearLabel;

    @FXML
    private TextArea detailReasoningText;
    @FXML
    private ListView<String> alternativesListView;

    @FXML
    private Label lastUpdatedLabel;

    // ========================================================================
    // Data
    // ========================================================================
    private ObservableList<CourseRecommendation> allRecommendations;
    private ObservableList<CourseRecommendation> filteredRecommendations;
    private CourseRecommendation selectedRecommendation;

    // ========================================================================
    // Initialization
    // ========================================================================
    @FXML
    public void initialize() {
        log.info("Initializing Course Recommendation Dashboard Controller");

        // Initialize data lists
        allRecommendations = FXCollections.observableArrayList();
        filteredRecommendations = FXCollections.observableArrayList();

        // Setup table columns
        setupTableColumns();

        // Setup filters
        setupFilters();

        // Setup table selection listener
        setupTableSelectionListener();

        // Load initial data
        Platform.runLater(this::loadData);

        log.info("Course Recommendation Dashboard Controller initialized");
    }

    // ========================================================================
    // Setup Methods
    // ========================================================================
    private void setupTableColumns() {
        // Student column
        studentColumn.setCellValueFactory(cellData -> {
            Student student = cellData.getValue().getStudent();
            return new SimpleStringProperty(student != null ?
                student.getFirstName() + " " + student.getLastName() : "Unknown");
        });

        // Course column
        courseColumn.setCellValueFactory(cellData -> {
            var course = cellData.getValue().getCourse();
            return new SimpleStringProperty(course != null ?
                course.getCourseName() : "Unknown");
        });

        // School Year column
        schoolYearColumn.setCellValueFactory(new PropertyValueFactory<>("recommendedSchoolYear"));

        // Type column
        typeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getRecommendationType().toString()));

        // Priority column
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityColumn.setCellFactory(col -> new TableCell<CourseRecommendation, Integer>() {
            @Override
            protected void updateItem(Integer priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(priority));
                    // Color code by priority: 1-3=Low(Green), 4-6=Med(Amber), 7-8=High(Orange), 9-10=Critical(Red)
                    if (priority >= 9) {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    } else if (priority >= 7) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else if (priority >= 4) {
                        setStyle("-fx-text-fill: #FFC107;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50;");
                    }
                }
            }
        });

        // Confidence column
        confidenceColumn.setCellValueFactory(new PropertyValueFactory<>("confidenceScore"));
        confidenceColumn.setCellFactory(col -> new TableCell<CourseRecommendation, Double>() {
            @Override
            protected void updateItem(Double confidence, boolean empty) {
                super.updateItem(confidence, empty);
                if (empty || confidence == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.0f%%", confidence));
                    // Color code: 80-100%=Green, 50-79%=Amber, 0-49%=Orange
                    if (confidence >= 80) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else if (confidence >= 50) {
                        setStyle("-fx-text-fill: #FFC107;");
                    } else {
                        setStyle("-fx-text-fill: #FF9800;");
                    }
                }
            }
        });

        // Status column
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusColumn.setCellFactory(col -> new TableCell<CourseRecommendation, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "ACCEPTED":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "REJECTED":
                            setStyle("-fx-text-fill: #F44336;");
                            break;
                        case "PENDING":
                            setStyle("-fx-text-fill: #FFC107;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // Reason column
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonColumn.setCellFactory(col -> new TableCell<CourseRecommendation, String>() {
            @Override
            protected void updateItem(String reason, boolean empty) {
                super.updateItem(reason, empty);
                if (empty || reason == null) {
                    setText(null);
                } else {
                    // Truncate long reasons
                    setText(reason.length() > 50 ? reason.substring(0, 47) + "..." : reason);
                }
            }
        });

        // Bind table to filtered list
        recommendationsTable.setItems(filteredRecommendations);
    }

    private void setupFilters() {
        // Status filter
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "All", "PENDING", "ACCEPTED", "REJECTED"
        ));
        statusFilterComboBox.setValue("All");

        // Priority filter
        priorityFilterComboBox.setItems(FXCollections.observableArrayList(
            "All", "High (7-10)", "Medium (4-6)", "Low (1-3)"
        ));
        priorityFilterComboBox.setValue("All");

        // Type filter
        typeFilterComboBox.setItems(FXCollections.observableArrayList(
            "All Types", "CORE_REQUIREMENT", "ELECTIVE", "ADVANCED_PLACEMENT",
            "HONORS", "PREREQUISITE_BASED", "CAREER_PATHWAY", "GRADUATION_REQUIREMENT"
        ));
        typeFilterComboBox.setValue("All Types");

        // Student filter (will be populated when data loads)
        studentFilterComboBox.setPromptText("All Students");
    }

    private void setupTableSelectionListener() {
        recommendationsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedRecommendation = newSelection;
                    loadRecommendationDetails(newSelection);
                }
            }
        );
    }

    // ========================================================================
    // Data Loading
    // ========================================================================
    @FXML
    public void loadData() {
        log.info("Loading course recommendations data");
        statusLabel.setText("Loading data...");

        try {
            // Load all recommendations
            List<CourseRecommendation> recommendations = courseRecommendationService.getAllRecommendations();
            allRecommendations.setAll(recommendations);

            // Load students for filter
            List<Student> students = studentRepository.findAll();
            studentFilterComboBox.setItems(FXCollections.observableArrayList(students));

            // Apply filters
            applyFilters();

            // Update statistics
            updateStatistics();

            // Update timestamp
            lastUpdatedLabel.setText("Last updated: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ));

            statusLabel.setText("Ready - " + filteredRecommendations.size() + " recommendations");
            log.info("Loaded {} recommendations", allRecommendations.size());

        } catch (Exception e) {
            log.error("Error loading recommendations data", e);
            CopyableErrorDialog.showError("Load Error",
                "Failed to load recommendations: " + e.getMessage());
            statusLabel.setText("Error loading data");
        }
    }

    private void loadRecommendationDetails(CourseRecommendation rec) {
        // Student Info
        Student student = rec.getStudent();
        if (student != null) {
            detailStudentNameLabel.setText(student.getFirstName() + " " + student.getLastName());
            detailGradeLevelLabel.setText(String.valueOf(student.getGradeLevel()));
            detailGPALabel.setText(student.getCurrentGPA() != null ?
                String.format("%.2f", student.getCurrentGPA()) : "N/A");
        }

        // Course Info
        var course = rec.getCourse();
        if (course != null) {
            detailCourseNameLabel.setText(course.getCourseName());
            detailCourseCodeLabel.setText(course.getCourseCode());
            detailSubjectLabel.setText(course.getSubject() != null ? course.getSubject() : "N/A");
            // Credits are determined when course is added to academic plan
            detailCreditsLabel.setText("TBD");
        }

        // Recommendation Analysis
        detailTypeLabel.setText(rec.getRecommendationType().toString());
        detailPriorityLabel.setText(String.valueOf(rec.getPriority()));
        detailPriorityBar.setProgress(rec.getPriority() / 10.0);
        detailConfidenceLabel.setText(String.format("%.0f%%", rec.getConfidenceScore()));
        detailConfidenceBar.setProgress(rec.getConfidenceScore() / 100.0);
        detailStatusLabel.setText(rec.getStatus().toString());
        detailSchoolYearLabel.setText(rec.getRecommendedSchoolYear());

        // Reasoning
        detailReasoningText.setText(rec.getReason() != null ? rec.getReason() : "No reasoning provided");

        // Alternative Recommendations
        loadAlternatives(rec);
    }

    private void loadAlternatives(CourseRecommendation currentRec) {
        try {
            // Get other recommendations for the same student and school year
            List<String> alternatives = courseRecommendationService
                .getRecommendationsForStudent(currentRec.getStudent().getId())
                .stream()
                .filter(r -> !r.getId().equals(currentRec.getId()))
                .filter(r -> r.getRecommendedSchoolYear().equals(currentRec.getRecommendedSchoolYear()))
                .map(r -> String.format("%s (Priority: %d, Confidence: %.0f%%)",
                    r.getCourse().getCourseName(),
                    r.getPriority(),
                    r.getConfidenceScore()))
                .limit(5)
                .collect(Collectors.toList());

            alternativesListView.setItems(FXCollections.observableArrayList(alternatives));
        } catch (Exception e) {
            log.error("Error loading alternative recommendations", e);
        }
    }

    private void updateStatistics() {
        totalRecommendationsLabel.setText(String.valueOf(allRecommendations.size()));

        long pending = allRecommendations.stream()
            .filter(r -> r.getStatus() == CourseRecommendation.RecommendationStatus.PENDING)
            .count();
        pendingRecommendationsLabel.setText(String.valueOf(pending));

        long accepted = allRecommendations.stream()
            .filter(r -> r.getStatus() == CourseRecommendation.RecommendationStatus.ACCEPTED)
            .count();
        acceptedRecommendationsLabel.setText(String.valueOf(accepted));

        long rejected = allRecommendations.stream()
            .filter(r -> r.getStatus() == CourseRecommendation.RecommendationStatus.REJECTED)
            .count();
        rejectedRecommendationsLabel.setText(String.valueOf(rejected));
    }

    // ========================================================================
    // Filter Methods
    // ========================================================================
    @FXML
    public void handleStudentFilterChange() {
        applyFilters();
    }

    @FXML
    public void handleStatusFilterChange() {
        applyFilters();
    }

    @FXML
    public void handlePriorityFilterChange() {
        applyFilters();
    }

    @FXML
    public void handleTypeFilterChange() {
        applyFilters();
    }

    @FXML
    public void handleClearFilters() {
        studentFilterComboBox.setValue(null);
        statusFilterComboBox.setValue("All");
        priorityFilterComboBox.setValue("All");
        typeFilterComboBox.setValue("All Types");
        applyFilters();
    }

    private void applyFilters() {
        List<CourseRecommendation> filtered = allRecommendations.stream()
            .filter(this::matchesStudentFilter)
            .filter(this::matchesStatusFilter)
            .filter(this::matchesPriorityFilter)
            .filter(this::matchesTypeFilter)
            .collect(Collectors.toList());

        filteredRecommendations.setAll(filtered);
        statusLabel.setText("Showing " + filtered.size() + " of " + allRecommendations.size() + " recommendations");
    }

    private boolean matchesStudentFilter(CourseRecommendation rec) {
        Student selectedStudent = studentFilterComboBox.getValue();
        return selectedStudent == null || rec.getStudent().getId().equals(selectedStudent.getId());
    }

    private boolean matchesStatusFilter(CourseRecommendation rec) {
        String selectedStatus = statusFilterComboBox.getValue();
        return selectedStatus == null || selectedStatus.equals("All") ||
               rec.getStatus().toString().equals(selectedStatus);
    }

    private boolean matchesPriorityFilter(CourseRecommendation rec) {
        String selectedPriority = priorityFilterComboBox.getValue();
        if (selectedPriority == null || selectedPriority.equals("All")) {
            return true;
        }

        int priority = rec.getPriority();
        switch (selectedPriority) {
            case "High (7-10)":
                return priority >= 7;
            case "Medium (4-6)":
                return priority >= 4 && priority <= 6;
            case "Low (1-3)":
                return priority <= 3;
            default:
                return true;
        }
    }

    private boolean matchesTypeFilter(CourseRecommendation rec) {
        String selectedType = typeFilterComboBox.getValue();
        return selectedType == null || selectedType.equals("All Types") ||
               rec.getRecommendationType().toString().equals(selectedType);
    }

    // ========================================================================
    // Action Handlers
    // ========================================================================
    @FXML
    public void handleGenerateRecommendations() {
        log.info("Generate recommendations requested");
        statusLabel.setText("Generating recommendations...");

        try {
            // Get current school year (would come from settings in real implementation)
            String currentSchoolYear = "2025-2026";

            // Generate recommendations for all students without them
            List<Student> allStudents = studentRepository.findAll();
            int generated = 0;

            for (Student student : allStudents) {
                try {
                    List<CourseRecommendation> studentRecs =
                        courseRecommendationService.getRecommendationsForStudent(student.getId());

                    if (studentRecs.isEmpty()) {
                        courseRecommendationService.generateRecommendationsForStudent(
                            student.getId(), currentSchoolYear);
                        generated++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate recommendations for student {}: {}",
                        student.getId(), e.getMessage());
                }
            }

            showInfo("Recommendations Generated",
                String.format("Generated recommendations for %d students", generated));
            loadData(); // Reload data

        } catch (Exception e) {
            log.error("Error generating recommendations", e);
            CopyableErrorDialog.showError("Generation Error",
                "Failed to generate recommendations: " + e.getMessage());
            statusLabel.setText("Error generating recommendations");
        }
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    @FXML
    public void handleAcceptSelected() {
        CourseRecommendation selected = recommendationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a recommendation to accept");
            return;
        }

        acceptRecommendation(selected);
    }

    @FXML
    public void handleRejectSelected() {
        CourseRecommendation selected = recommendationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a recommendation to reject");
            return;
        }

        rejectRecommendation(selected);
    }

    @FXML
    public void handleViewDetails() {
        CourseRecommendation selected = recommendationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a recommendation to view");
            return;
        }

        // Details already shown in right panel when selected
        showInfo("Recommendation Details", "Details are displayed in the right panel");
    }

    @FXML
    public void handleAcceptCurrent() {
        if (selectedRecommendation != null) {
            acceptRecommendation(selectedRecommendation);
        } else {
            showWarning("No Selection", "Please select a recommendation first");
        }
    }

    @FXML
    public void handleRejectCurrent() {
        if (selectedRecommendation != null) {
            rejectRecommendation(selectedRecommendation);
        } else {
            showWarning("No Selection", "Please select a recommendation first");
        }
    }

    @FXML
    public void handleViewStudentPlan() {
        if (selectedRecommendation == null) {
            showWarning("No Selection", "Please select a recommendation first");
            return;
        }

        // Get student info for navigation
        Student student = selectedRecommendation.getStudent();
        String studentName = student.getFirstName() + " " + student.getLastName();
        String studentId = student.getStudentId() != null ? student.getStudentId() : "N/A";

        showInfo("Navigate to Academic Planning",
            "Please navigate to:\n\n" +
            "  Main Menu → Academic Planning\n\n" +
            "Then filter for student:\n" +
            "  • Student ID: " + studentId + "\n" +
            "  • Name: " + studentName + "\n\n" +
            "This will show all academic plans for " + student.getFirstName() + ".");
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================
    private void acceptRecommendation(CourseRecommendation rec) {
        try {
            // Accept by student (counselors can also use this workflow)
            courseRecommendationService.acceptByStudent(rec.getId());

            showInfo("Accepted", "Recommendation accepted successfully");
            loadData(); // Reload data

        } catch (Exception e) {
            log.error("Error accepting recommendation", e);
            CopyableErrorDialog.showError("Accept Error",
                "Failed to accept recommendation: " + e.getMessage());
        }
    }

    private void rejectRecommendation(CourseRecommendation rec) {
        // Prompt for rejection reason (optional - for future enhancement)
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Recommendation");
        dialog.setHeaderText("Reject Recommendation");
        dialog.setContentText("Reason for rejection (optional):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            try {
                // Reject by student (counselors can also use this workflow)
                courseRecommendationService.rejectByStudent(rec.getId());

                // Store rejection reason in counselorNotes field if provided
                if (!reason.trim().isEmpty()) {
                    CourseRecommendation updates = new CourseRecommendation();
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    String existingNotes = rec.getCounselorNotes() != null ? rec.getCounselorNotes() : "";
                    String newNote = "[" + timestamp + "] Rejection reason: " + reason;
                    updates.setCounselorNotes(existingNotes.isEmpty() ? newNote : existingNotes + "\n" + newNote);
                    courseRecommendationService.updateRecommendation(rec.getId(), updates);
                    log.info("Stored rejection reason for recommendation {}: {}", rec.getId(), reason);
                }

                showInfo("Rejected", "Recommendation rejected successfully");
                loadData(); // Reload data

            } catch (Exception e) {
                log.error("Error rejecting recommendation", e);
                CopyableErrorDialog.showError("Reject Error",
                    "Failed to reject recommendation: " + e.getMessage());
            }
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
