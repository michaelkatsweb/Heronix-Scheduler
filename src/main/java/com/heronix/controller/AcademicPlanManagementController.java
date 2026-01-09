package com.heronix.controller;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import com.heronix.service.*;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Academic Plan Management Controller
 *
 * Provides comprehensive four-year academic planning functionality integrating:
 * - Phase 1: Subject Area hierarchies
 * - Phase 2: Course Sequences
 * - Phase 3: AI-powered Recommendations
 * - Phase 4: Four-Year Academic Planning
 *
 * Location: src/main/java/com/eduscheduler/controller/AcademicPlanManagementController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 UI Implementation - December 6, 2025
 */
@Slf4j
@Controller
public class AcademicPlanManagementController {

    // ========================================================================
    // SERVICES
    // ========================================================================

    @Autowired
    private AcademicPlanningService planningService;

    @Autowired
    private CourseRecommendationService recommendationService;

    @Autowired
    private CourseSequenceService sequenceService;

    @Autowired
    private SubjectAreaService subjectAreaService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationContext applicationContext;

    // ========================================================================
    // FXML COMPONENTS - Top Section
    // ========================================================================

    @FXML private ComboBox<Student> studentSelector;
    @FXML private ComboBox<AcademicPlan> planSelector;
    @FXML private Button setPrimaryButton;
    @FXML private Label planStatusLabel;
    @FXML private Label planTypeLabel;
    @FXML private ProgressBar completionProgress;
    @FXML private Label completionLabel;
    @FXML private Label creditsLabel;
    @FXML private Label graduationLabel;

    // Approval Labels
    @FXML private Label approvalStudentLabel;
    @FXML private Label approvalParentLabel;
    @FXML private Label approvalCounselorLabel;

    // ========================================================================
    // FXML COMPONENTS - Year Lists
    // ========================================================================

    // Grade 9
    @FXML private Label grade9YearLabel;
    @FXML private Label grade9CreditsLabel;
    @FXML private ListView<PlannedCourse> grade9FallList;
    @FXML private ListView<PlannedCourse> grade9SpringList;
    @FXML private ListView<PlannedCourse> grade9FullYearList;

    // Grade 10
    @FXML private Label grade10YearLabel;
    @FXML private Label grade10CreditsLabel;
    @FXML private ListView<PlannedCourse> grade10FallList;
    @FXML private ListView<PlannedCourse> grade10SpringList;
    @FXML private ListView<PlannedCourse> grade10FullYearList;

    // Grade 11
    @FXML private Label grade11YearLabel;
    @FXML private Label grade11CreditsLabel;
    @FXML private ListView<PlannedCourse> grade11FallList;
    @FXML private ListView<PlannedCourse> grade11SpringList;
    @FXML private ListView<PlannedCourse> grade11FullYearList;

    // Grade 12
    @FXML private Label grade12YearLabel;
    @FXML private Label grade12CreditsLabel;
    @FXML private ListView<PlannedCourse> grade12FallList;
    @FXML private ListView<PlannedCourse> grade12SpringList;
    @FXML private ListView<PlannedCourse> grade12FullYearList;

    // ========================================================================
    // FXML COMPONENTS - Right Panel
    // ========================================================================

    @FXML private TableView<GraduationRequirement> requirementsTable;
    @FXML private TableColumn<GraduationRequirement, String> reqSubjectColumn;
    @FXML private TableColumn<GraduationRequirement, Double> reqRequiredColumn;
    @FXML private TableColumn<GraduationRequirement, Double> reqCompletedColumn;
    @FXML private TableColumn<GraduationRequirement, String> reqStatusColumn;

    @FXML private TextArea notesArea;
    @FXML private VBox alertsBox;
    @FXML private ListView<String> alertsList;

    @FXML private Label statusLabel;
    @FXML private Button counselorApprovalButton;

    // ========================================================================
    // STATE
    // ========================================================================

    private Student currentStudent;
    private AcademicPlan currentPlan;
    private ObservableList<PlannedCourse> allPlannedCourses = FXCollections.observableArrayList();

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing AcademicPlanManagementController");

        // Initialize student selector
        initializeStudentSelector();

        // Initialize course list cell factories
        initializeCourseLists();

        // Initialize requirements table
        initializeRequirementsTable();

        // Initial data load
        loadStudents();

        log.info("AcademicPlanManagementController initialized successfully");
    }

    private void initializeStudentSelector() {
        studentSelector.setConverter(new StringConverter<Student>() {
            @Override
            public String toString(Student student) {
                if (student == null) return "";
                return String.format("%s, %s (Grade %s)",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getGradeLevel() != null ? student.getGradeLevel() : "N/A");
            }

            @Override
            public Student fromString(String string) {
                return null;
            }
        });

        planSelector.setConverter(new StringConverter<AcademicPlan>() {
            @Override
            public String toString(AcademicPlan plan) {
                if (plan == null) return "";
                String primary = Boolean.TRUE.equals(plan.getIsPrimary()) ? "⭐ " : "";
                return String.format("%s%s (%s)",
                    primary,
                    plan.getPlanName(),
                    plan.getStatus());
            }

            @Override
            public AcademicPlan fromString(String string) {
                return null;
            }
        });
    }

    private void initializeCourseLists() {
        // Create a custom cell factory for all lists
        javafx.util.Callback<ListView<PlannedCourse>, ListCell<PlannedCourse>> cellFactory =
            lv -> new PlannedCourseListCell();

        // Apply to all grade 9 lists
        grade9FallList.setCellFactory(cellFactory);
        grade9SpringList.setCellFactory(cellFactory);
        grade9FullYearList.setCellFactory(cellFactory);

        // Apply to all grade 10 lists
        grade10FallList.setCellFactory(cellFactory);
        grade10SpringList.setCellFactory(cellFactory);
        grade10FullYearList.setCellFactory(cellFactory);

        // Apply to all grade 11 lists
        grade11FallList.setCellFactory(cellFactory);
        grade11SpringList.setCellFactory(cellFactory);
        grade11FullYearList.setCellFactory(cellFactory);

        // Apply to all grade 12 lists
        grade12FallList.setCellFactory(cellFactory);
        grade12SpringList.setCellFactory(cellFactory);
        grade12FullYearList.setCellFactory(cellFactory);

        // Add context menus to all lists
        addContextMenuToList(grade9FallList);
        addContextMenuToList(grade9SpringList);
        addContextMenuToList(grade9FullYearList);
        addContextMenuToList(grade10FallList);
        addContextMenuToList(grade10SpringList);
        addContextMenuToList(grade10FullYearList);
        addContextMenuToList(grade11FallList);
        addContextMenuToList(grade11SpringList);
        addContextMenuToList(grade11FullYearList);
        addContextMenuToList(grade12FallList);
        addContextMenuToList(grade12SpringList);
        addContextMenuToList(grade12FullYearList);
    }

    private void addContextMenuToList(ListView<PlannedCourse> listView) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem viewDetails = new MenuItem("View Course Details");
        viewDetails.setOnAction(e -> {
            PlannedCourse course = listView.getSelectionModel().getSelectedItem();
            if (course != null) {
                showCourseDetails(course);
            }
        });

        MenuItem markComplete = new MenuItem("Mark as Completed");
        markComplete.setOnAction(e -> {
            PlannedCourse course = listView.getSelectionModel().getSelectedItem();
            if (course != null) {
                handleMarkCourseCompleted(course);
            }
        });

        MenuItem removeCourse = new MenuItem("Remove from Plan");
        removeCourse.setOnAction(e -> {
            PlannedCourse course = listView.getSelectionModel().getSelectedItem();
            if (course != null) {
                handleRemoveCourse(course);
            }
        });

        contextMenu.getItems().addAll(viewDetails, markComplete, new SeparatorMenuItem(), removeCourse);
        listView.setContextMenu(contextMenu);
    }

    private void initializeRequirementsTable() {
        reqSubjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        reqRequiredColumn.setCellValueFactory(new PropertyValueFactory<>("required"));
        reqCompletedColumn.setCellValueFactory(new PropertyValueFactory<>("completed"));
        reqStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    private void loadStudents() {
        try {
            List<Student> students = studentRepository.findAll();
            students.sort(Comparator.comparing(Student::getLastName)
                .thenComparing(Student::getFirstName));
            studentSelector.setItems(FXCollections.observableArrayList(students));

            if (!students.isEmpty()) {
                studentSelector.getSelectionModel().selectFirst();
                handleStudentChange();
            }
        } catch (Exception e) {
            log.error("Error loading students", e);
            showError("Error loading students: " + e.getMessage());
        }
    }

    @FXML
    private void handleStudentChange() {
        currentStudent = studentSelector.getValue();
        if (currentStudent != null) {
            loadPlansForStudent();
        }
    }

    private void loadPlansForStudent() {
        try {
            List<AcademicPlan> plans = planningService.getPlansForStudent(currentStudent.getId());
            planSelector.setItems(FXCollections.observableArrayList(plans));

            if (!plans.isEmpty()) {
                // Select primary plan if exists, otherwise first plan
                Optional<AcademicPlan> primary = plans.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsPrimary()))
                    .findFirst();

                if (primary.isPresent()) {
                    planSelector.setValue(primary.get());
                } else {
                    planSelector.getSelectionModel().selectFirst();
                }
                handlePlanChange();
            } else {
                clearPlanView();
            }
        } catch (Exception e) {
            log.error("Error loading plans for student", e);
            showError("Error loading plans: " + e.getMessage());
        }
    }

    @FXML
    private void handlePlanChange() {
        currentPlan = planSelector.getValue();
        if (currentPlan != null) {
            loadPlanDetails();
        } else {
            clearPlanView();
        }
    }

    private void loadPlanDetails() {
        try {
            // Update plan info
            planTypeLabel.setText(currentPlan.getPlanType().toString());
            planStatusLabel.setText(currentPlan.getStatus().toString());

            // Update completion progress
            double completion = currentPlan.getCompletionPercentage();
            completionProgress.setProgress(completion / 100.0);
            completionLabel.setText(String.format("%.1f%%", completion));

            // Update credits
            double completed = currentPlan.getTotalCreditsCompleted() != null ? currentPlan.getTotalCreditsCompleted() : 0.0;
            double planned = currentPlan.getTotalCreditsPlanned() != null ? currentPlan.getTotalCreditsPlanned() : 0.0;
            creditsLabel.setText(String.format("%.1f / %.1f", completed, planned));

            // Update graduation status
            boolean meets = Boolean.TRUE.equals(currentPlan.getMeetsGraduationRequirements());
            graduationLabel.setText(meets ? "✓ Yes" : "⚠ No");

            // Update approval status
            updateApprovalStatus();

            // Load notes
            notesArea.setText(currentPlan.getNotes());

            // Load planned courses
            loadPlannedCourses();

            // Load graduation requirements
            loadGraduationRequirements();

            // Load alerts
            loadAlerts();

        } catch (Exception e) {
            log.error("Error loading plan details", e);
            showError("Error loading plan details: " + e.getMessage());
        }
    }

    private void updateApprovalStatus() {
        approvalStudentLabel.setText(Boolean.TRUE.equals(currentPlan.getStudentAccepted()) ?
            "Student: ✓" : "Student: ⚪");

        approvalParentLabel.setText(Boolean.TRUE.equals(currentPlan.getParentAccepted()) ?
            "Parent: ✓" : "Parent: ⚪");

        approvalCounselorLabel.setText(currentPlan.getApprovedByCounselor() != null ?
            "Counselor: ✓" : "Counselor: ⚪");
    }

    private void loadPlannedCourses() {
        // Clear all lists
        grade9FallList.getItems().clear();
        grade9SpringList.getItems().clear();
        grade9FullYearList.getItems().clear();
        grade10FallList.getItems().clear();
        grade10SpringList.getItems().clear();
        grade10FullYearList.getItems().clear();
        grade11FallList.getItems().clear();
        grade11SpringList.getItems().clear();
        grade11FullYearList.getItems().clear();
        grade12FallList.getItems().clear();
        grade12SpringList.getItems().clear();
        grade12FullYearList.getItems().clear();

        // Load courses and distribute them
        allPlannedCourses.clear();
        if (currentPlan.getPlannedCourses() != null) {
            allPlannedCourses.addAll(currentPlan.getPlannedCourses());

            for (PlannedCourse course : allPlannedCourses) {
                Integer grade = course.getGradeLevel();
                Integer semester = course.getSemester() != null ? course.getSemester() : 0;

                if (grade == null) continue;

                switch (grade) {
                    case 9:
                        addCourseToLists(course, semester,
                            grade9FallList, grade9SpringList, grade9FullYearList);
                        break;
                    case 10:
                        addCourseToLists(course, semester,
                            grade10FallList, grade10SpringList, grade10FullYearList);
                        break;
                    case 11:
                        addCourseToLists(course, semester,
                            grade11FallList, grade11SpringList, grade11FullYearList);
                        break;
                    case 12:
                        addCourseToLists(course, semester,
                            grade12FallList, grade12SpringList, grade12FullYearList);
                        break;
                }
            }

            // Update credit labels for each grade
            updateGradeCredits();
        }
    }

    private void addCourseToLists(PlannedCourse course, Integer semester,
                                   ListView<PlannedCourse> fallList,
                                   ListView<PlannedCourse> springList,
                                   ListView<PlannedCourse> fullYearList) {
        if (semester == 1) {
            fallList.getItems().add(course);
        } else if (semester == 2) {
            springList.getItems().add(course);
        } else {
            fullYearList.getItems().add(course);
        }
    }

    private void updateGradeCredits() {
        grade9CreditsLabel.setText(calculateCreditsForGrade(9) + " credits");
        grade10CreditsLabel.setText(calculateCreditsForGrade(10) + " credits");
        grade11CreditsLabel.setText(calculateCreditsForGrade(11) + " credits");
        grade12CreditsLabel.setText(calculateCreditsForGrade(12) + " credits");
    }

    private double calculateCreditsForGrade(int grade) {
        return allPlannedCourses.stream()
            .filter(c -> c.getGradeLevel() != null && c.getGradeLevel() == grade)
            .mapToDouble(c -> c.getCredits() != null ? c.getCredits() : 0.0)
            .sum();
    }

    private void loadGraduationRequirements() {
        ObservableList<GraduationRequirement> requirements = FXCollections.observableArrayList();

        if (currentStudent == null) {
            requirementsTable.setItems(requirements);
            return;
        }

        // Calculate completed credits by subject area from all planned courses
        Map<String, Double> completedBySubject = new HashMap<>();
        for (PlannedCourse pc : allPlannedCourses) {
            if (pc.getCourse() != null && pc.getCourse().getSubjectArea() != null) {
                // Get subject area name from SubjectArea entity
                String subject = pc.getCourse().getSubjectArea().getName();
                double credits = pc.getCredits() != null ? pc.getCredits() : 0.0;
                completedBySubject.merge(subject, credits, Double::sum);
            }
        }

        // Standard K-12 graduation requirements (typical U.S. high school)
        requirements.add(new GraduationRequirement("English/Language Arts", 4.0, completedBySubject.getOrDefault("English", 0.0)));
        requirements.add(new GraduationRequirement("Mathematics", 3.0, completedBySubject.getOrDefault("Math", 0.0)));
        requirements.add(new GraduationRequirement("Science", 3.0, completedBySubject.getOrDefault("Science", 0.0)));
        requirements.add(new GraduationRequirement("Social Studies", 3.0, completedBySubject.getOrDefault("Social Studies", 0.0)));
        requirements.add(new GraduationRequirement("Physical Education", 2.0, completedBySubject.getOrDefault("PE", 0.0)));
        requirements.add(new GraduationRequirement("Fine Arts", 1.0, completedBySubject.getOrDefault("Arts", 0.0)));
        requirements.add(new GraduationRequirement("World Language", 2.0, completedBySubject.getOrDefault("World Language", 0.0)));
        requirements.add(new GraduationRequirement("Electives", 6.0, completedBySubject.getOrDefault("Electives", 0.0)));

        requirementsTable.setItems(requirements);
        log.info("Loaded {} graduation requirements for student {}", requirements.size(),
            currentStudent.getStudentId() != null ? currentStudent.getStudentId() : currentStudent.getId());
    }

    private void loadAlerts() {
        ObservableList<String> alerts = FXCollections.observableArrayList();

        // Check for prerequisite issues
        long prereqIssues = allPlannedCourses.stream()
            .filter(c -> !Boolean.TRUE.equals(c.getPrerequisitesMet()))
            .count();
        if (prereqIssues > 0) {
            alerts.add("⚠ " + prereqIssues + " course(s) with unmet prerequisites");
        }

        // Check for conflicts
        long conflicts = allPlannedCourses.stream()
            .filter(c -> Boolean.TRUE.equals(c.getHasConflict()))
            .count();
        if (conflicts > 0) {
            alerts.add("⚠ " + conflicts + " course(s) with scheduling conflicts");
        }

        // Check graduation requirements
        if (!Boolean.TRUE.equals(currentPlan.getMeetsGraduationRequirements())) {
            alerts.add("⚠ Plan does not meet graduation requirements");
        }

        if (alerts.isEmpty()) {
            alerts.add("✓ No alerts - plan looks good!");
        }

        alertsList.setItems(alerts);
    }

    private void clearPlanView() {
        planTypeLabel.setText("-");
        planStatusLabel.setText("-");
        completionProgress.setProgress(0);
        completionLabel.setText("0%");
        creditsLabel.setText("0.0 / 0.0");
        graduationLabel.setText("No");
        notesArea.clear();
        grade9FallList.getItems().clear();
        grade9SpringList.getItems().clear();
        grade9FullYearList.getItems().clear();
        grade10FallList.getItems().clear();
        grade10SpringList.getItems().clear();
        grade10FullYearList.getItems().clear();
        grade11FallList.getItems().clear();
        grade11SpringList.getItems().clear();
        grade11FullYearList.getItems().clear();
        grade12FallList.getItems().clear();
        grade12SpringList.getItems().clear();
        grade12FullYearList.getItems().clear();
        requirementsTable.getItems().clear();
        alertsList.getItems().clear();
    }

    // ========================================================================
    // ACTION HANDLERS - Plan Operations
    // ========================================================================

    @FXML
    private void handleNewPlan() {
        if (currentStudent == null) {
            showWarning("Please select a student first");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("New Academic Plan");
        dialog.setTitle("Create New Plan");
        dialog.setHeaderText("Create a new academic plan for " + currentStudent.getFirstName() + " " + currentStudent.getLastName());
        dialog.setContentText("Plan name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                AcademicPlan newPlan = AcademicPlan.builder()
                    .student(currentStudent)
                    .planName(name)
                    .planType(AcademicPlan.PlanType.STANDARD)
                    .status(AcademicPlan.PlanStatus.DRAFT)
                    .startYear("2025-2026")
                    .isPrimary(false)
                    .active(true)
                    .totalCreditsPlanned(0.0)
                    .totalCreditsCompleted(0.0)
                    .meetsGraduationRequirements(false)
                    .build();

                AcademicPlan savedPlan = planningService.createPlan(newPlan);
                loadPlansForStudent();
                planSelector.setValue(savedPlan);
                handlePlanChange();

                showInfo("Plan created successfully!");
            } catch (Exception e) {
                log.error("Error creating plan", e);
                showError("Error creating plan: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleGenerateFromSequence() {
        if (currentStudent == null) {
            showWarning("Please select a student first");
            return;
        }

        // Get available sequences
        List<CourseSequence> sequences = sequenceService.getAllSequences();
        if (sequences.isEmpty()) {
            showWarning("No course sequences available");
            return;
        }

        // Show selection dialog
        ChoiceDialog<CourseSequence> dialog = new ChoiceDialog<>(sequences.get(0), sequences);
        dialog.setTitle("Generate Plan from Sequence");
        dialog.setHeaderText("Select a course sequence to generate the plan");
        dialog.setContentText("Sequence:");

        // Custom converter for display
        dialog.getSelectedItem();

        Optional<CourseSequence> result = dialog.showAndWait();
        result.ifPresent(sequence -> {
            try {
                AcademicPlan generatedPlan = planningService.generatePlanFromSequence(
                    currentStudent.getId(),
                    sequence.getId(),
                    "2025-2026",
                    sequence.getName() + " - Generated Plan"
                );

                loadPlansForStudent();
                planSelector.setValue(generatedPlan);
                handlePlanChange();

                showInfo("Plan generated successfully from sequence!");
            } catch (Exception e) {
                log.error("Error generating plan from sequence", e);
                showError("Error generating plan: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleGenerateFromRecommendations() {
        if (currentStudent == null) {
            showWarning("Please select a student first");
            return;
        }

        try {
            // First, generate recommendations if needed
            List<CourseRecommendation> recommendations =
                recommendationService.getRecommendationsForStudent(currentStudent.getId());

            if (recommendations.isEmpty()) {
                // Generate new recommendations
                recommendations = recommendationService.generateRecommendationsForStudent(
                    currentStudent.getId(),
                    "2026-2027"
                );
            }

            if (recommendations.isEmpty()) {
                showWarning("No recommendations available for this student");
                return;
            }

            // Generate plan from recommendations
            AcademicPlan generatedPlan = planningService.generatePlanFromRecommendations(
                currentStudent.getId(),
                "2025-2026",
                "AI-Generated Plan"
            );

            loadPlansForStudent();
            planSelector.setValue(generatedPlan);
            handlePlanChange();

            showInfo("Plan generated successfully from AI recommendations!");
        } catch (Exception e) {
            log.error("Error generating plan from recommendations", e);
            showError("Error generating plan: " + e.getMessage());
        }
    }

    @FXML
    private void handleSetPrimary() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        try {
            AcademicPlan updated = AcademicPlan.builder()
                .isPrimary(true)
                .build();

            planningService.updatePlan(currentPlan.getId(), updated);
            loadPlansForStudent();
            showInfo("Plan set as primary!");
        } catch (Exception e) {
            log.error("Error setting primary plan", e);
            showError("Error setting primary: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditPlan() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        try {
            // Create edit dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Academic Plan");
            dialog.setHeaderText("Edit Plan: " + currentPlan.getPlanName());

            // Create form fields with current values
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            TextField planNameField = new TextField(currentPlan.getPlanName());
            planNameField.setPrefColumnCount(30);

            ComboBox<AcademicPlan.PlanType> planTypeCombo = new ComboBox<>();
            planTypeCombo.getItems().addAll(AcademicPlan.PlanType.values());
            planTypeCombo.setValue(currentPlan.getPlanType());

            ComboBox<AcademicPlan.PlanStatus> planStatusCombo = new ComboBox<>();
            planStatusCombo.getItems().addAll(AcademicPlan.PlanStatus.values());
            planStatusCombo.setValue(currentPlan.getStatus());

            TextField startYearField = new TextField(currentPlan.getStartYear() != null ? currentPlan.getStartYear() : "");
            TextField gradYearField = new TextField(currentPlan.getExpectedGraduationYear() != null ? currentPlan.getExpectedGraduationYear() : "");

            CheckBox isPrimaryCheckBox = new CheckBox();
            isPrimaryCheckBox.setSelected(Boolean.TRUE.equals(currentPlan.getIsPrimary()));

            CheckBox meetsRequirementsCheckBox = new CheckBox();
            meetsRequirementsCheckBox.setSelected(Boolean.TRUE.equals(currentPlan.getMeetsGraduationRequirements()));

            // Layout form (6 fields)
            grid.add(new Label("Plan Name:"), 0, 0);
            grid.add(planNameField, 1, 0);
            grid.add(new Label("Plan Type:"), 0, 1);
            grid.add(planTypeCombo, 1, 1);
            grid.add(new Label("Plan Status:"), 0, 2);
            grid.add(planStatusCombo, 1, 2);
            grid.add(new Label("Start Year:"), 0, 3);
            grid.add(startYearField, 1, 3);
            grid.add(new Label("Graduation Year:"), 0, 4);
            grid.add(gradYearField, 1, 4);
            grid.add(new Label("Is Primary Plan:"), 0, 5);
            grid.add(isPrimaryCheckBox, 1, 5);
            grid.add(new Label("Meets Graduation Requirements:"), 0, 6);
            grid.add(meetsRequirementsCheckBox, 1, 6);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Validate required fields
                if (planNameField.getText().trim().isEmpty()) {
                    showWarning("Plan Name is required");
                    return;
                }

                // Update plan
                currentPlan.setPlanName(planNameField.getText().trim());
                currentPlan.setPlanType(planTypeCombo.getValue());
                currentPlan.setStatus(planStatusCombo.getValue());
                currentPlan.setStartYear(startYearField.getText().trim());
                currentPlan.setExpectedGraduationYear(gradYearField.getText().trim());
                currentPlan.setIsPrimary(isPrimaryCheckBox.isSelected());
                currentPlan.setMeetsGraduationRequirements(meetsRequirementsCheckBox.isSelected());

                // Save changes
                planningService.updatePlan(currentPlan.getId(), currentPlan);

                // Reload and refresh display
                loadPlansForStudent();
                loadPlanDetails();
                showInfo("Plan updated successfully!");

                log.info("Updated academic plan: {}", currentPlan.getPlanName());
            }
        } catch (Exception e) {
            log.error("Error editing plan", e);
            CopyableErrorDialog.showError("Edit Error", "Failed to edit plan: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeletePlan() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Plan");
        alert.setHeaderText("Are you sure you want to delete this plan?");
        alert.setContentText(currentPlan.getPlanName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                planningService.deletePlan(currentPlan.getId());
                loadPlansForStudent();
                showInfo("Plan deleted successfully!");
            } catch (Exception e) {
                log.error("Error deleting plan", e);
                showError("Error deleting plan: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRefresh() {
        if (currentStudent != null) {
            loadPlansForStudent();
        }
    }

    // ========================================================================
    // ACTION HANDLERS - Course Operations
    // ========================================================================

    @FXML
    private void handleAddCourse() {
        showInfo("Please use the '+Add Course' buttons under each semester");
    }

    @FXML
    private void handleAddCourseGrade9Fall() {
        handleAddCourseToSemester(9, 1);
    }

    @FXML
    private void handleAddCourseGrade9Spring() {
        handleAddCourseToSemester(9, 2);
    }

    @FXML
    private void handleAddCourseGrade9FullYear() {
        handleAddCourseToSemester(9, 0);
    }

    @FXML
    private void handleAddCourseGrade10Fall() {
        handleAddCourseToSemester(10, 1);
    }

    @FXML
    private void handleAddCourseGrade10Spring() {
        handleAddCourseToSemester(10, 2);
    }

    @FXML
    private void handleAddCourseGrade10FullYear() {
        handleAddCourseToSemester(10, 0);
    }

    @FXML
    private void handleAddCourseGrade11Fall() {
        handleAddCourseToSemester(11, 1);
    }

    @FXML
    private void handleAddCourseGrade11Spring() {
        handleAddCourseToSemester(11, 2);
    }

    @FXML
    private void handleAddCourseGrade11FullYear() {
        handleAddCourseToSemester(11, 0);
    }

    @FXML
    private void handleAddCourseGrade12Fall() {
        handleAddCourseToSemester(12, 1);
    }

    @FXML
    private void handleAddCourseGrade12Spring() {
        handleAddCourseToSemester(12, 2);
    }

    @FXML
    private void handleAddCourseGrade12FullYear() {
        handleAddCourseToSemester(12, 0);
    }

    private void handleAddCourseToSemester(int gradeLevel, int semester) {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        // Get available courses
        List<Course> courses = courseRepository.findByActiveTrue();
        if (courses.isEmpty()) {
            showWarning("No courses available");
            return;
        }

        // Show course selection dialog
        ChoiceDialog<Course> dialog = new ChoiceDialog<>(courses.get(0), courses);
        dialog.setTitle("Add Course");
        dialog.setHeaderText("Add course to Grade " + gradeLevel + ", " + getSemesterName(semester));
        dialog.setContentText("Select course:");

        Optional<Course> result = dialog.showAndWait();
        result.ifPresent(course -> {
            try {
                PlannedCourse plannedCourse = PlannedCourse.builder()
                    .course(course)
                    .schoolYear(calculateSchoolYear(gradeLevel))
                    .gradeLevel(gradeLevel)
                    .semester(semester)
                    .credits(1.0) // Default
                    .isRequired(false)
                    .status(PlannedCourse.CourseStatus.PLANNED)
                    .prerequisitesMet(true)
                    .hasConflict(false)
                    .build();

                planningService.addCourseToPlan(currentPlan.getId(), plannedCourse);
                handlePlanChange(); // Reload
                showInfo("Course added successfully!");
            } catch (Exception e) {
                log.error("Error adding course to plan", e);
                showError("Error adding course: " + e.getMessage());
            }
        });
    }

    private String getSemesterName(int semester) {
        return switch (semester) {
            case 1 -> "Fall";
            case 2 -> "Spring";
            default -> "Full Year";
        };
    }

    private String calculateSchoolYear(int gradeLevel) {
        // Simple calculation - would be enhanced in production
        int startYear = 2025 + (gradeLevel - 9);
        return startYear + "-" + (startYear + 1);
    }

    private void handleRemoveCourse(PlannedCourse course) {
        if (currentPlan == null || course == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Course");
        alert.setHeaderText("Remove this course from the plan?");
        alert.setContentText(course.getCourse().getCourseCode() + " - " + course.getCourse().getCourseName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                planningService.removeCourseFromPlan(currentPlan.getId(), course.getId());
                handlePlanChange(); // Reload
                showInfo("Course removed successfully!");
            } catch (Exception e) {
                log.error("Error removing course", e);
                showError("Error removing course: " + e.getMessage());
            }
        }
    }

    private void handleMarkCourseCompleted(PlannedCourse course) {
        if (course == null) return;

        TextInputDialog dialog = new TextInputDialog("A");
        dialog.setTitle("Mark Course Completed");
        dialog.setHeaderText("Enter grade earned for:");
        dialog.setContentText(course.getCourse().getCourseCode() + " - Grade:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(grade -> {
            try {
                planningService.markCourseCompleted(course.getId(), grade);
                handlePlanChange(); // Reload
                showInfo("Course marked as completed!");
            } catch (Exception e) {
                log.error("Error marking course completed", e);
                showError("Error marking course completed: " + e.getMessage());
            }
        });
    }

    private void showCourseDetails(PlannedCourse course) {
        if (course == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Course Details");
        alert.setHeaderText(course.getCourse().getCourseCode() + " - " + course.getCourse().getCourseName());

        String details = String.format(
            "Grade Level: %d\n" +
            "Semester: %s\n" +
            "Credits: %.1f\n" +
            "Status: %s\n" +
            "Required: %s\n" +
            "Prerequisites Met: %s\n" +
            "Has Conflict: %s\n" +
            "%s",
            course.getGradeLevel(),
            getSemesterName(course.getSemester()),
            course.getCredits(),
            course.getStatus(),
            course.getIsRequired() ? "Yes" : "No",
            course.getPrerequisitesMet() ? "Yes" : "No",
            course.getHasConflict() ? "Yes" : "No",
            course.getNotes() != null ? "\nNotes: " + course.getNotes() : ""
        );

        alert.setContentText(details);
        alert.showAndWait();
    }

    // ========================================================================
    // ACTION HANDLERS - Other
    // ========================================================================

    @FXML
    private void handleViewRecommendations() {
        if (currentStudent == null) {
            showWarning("Please select a student first");
            return;
        }

        showInfo("Course Recommendations view will open in a separate dialog (not yet implemented)");
    }

    @FXML
    private void handleExportPlan() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        showInfo("Export functionality not yet implemented");
    }

    @FXML
    private void handlePrintPlan() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        showInfo("Print functionality not yet implemented");
    }

    @FXML
    private void handleSaveNotes() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        try {
            AcademicPlan updated = AcademicPlan.builder()
                .notes(notesArea.getText())
                .build();

            planningService.updatePlan(currentPlan.getId(), updated);
            showInfo("Notes saved successfully!");
        } catch (Exception e) {
            log.error("Error saving notes", e);
            showError("Error saving notes: " + e.getMessage());
        }
    }

    @FXML
    private void handleSubmitForApproval() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        try {
            AcademicPlan updated = AcademicPlan.builder()
                .status(AcademicPlan.PlanStatus.PENDING_APPROVAL)
                .build();

            planningService.updatePlan(currentPlan.getId(), updated);
            handlePlanChange(); // Reload
            showInfo("Plan submitted for approval!");
        } catch (Exception e) {
            log.error("Error submitting plan", e);
            showError("Error submitting plan: " + e.getMessage());
        }
    }

    @FXML
    private void handleCounselorApproval() {
        if (currentPlan == null) {
            showWarning("Please select a plan first");
            return;
        }

        try {
            // For now, use a mock counselor ID - in production, get from logged-in user
            planningService.approveByCounselor(currentPlan.getId(), 1L);
            handlePlanChange(); // Reload
            showInfo("Plan approved by counselor!");
        } catch (Exception e) {
            log.error("Error approving plan", e);
            showError("Error approving plan: " + e.getMessage());
        }
    }

    // ========================================================================
    // CUSTOM LIST CELL
    // ========================================================================

    private static class PlannedCourseListCell extends ListCell<PlannedCourse> {
        @Override
        protected void updateItem(PlannedCourse course, boolean empty) {
            super.updateItem(course, empty);

            if (empty || course == null || course.getCourse() == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                String status = "";
                String style = "";

                // Add visual indicators
                if (course.isCompleted()) {
                    status = "✓ ";
                    style = "-fx-text-fill: green; -fx-font-weight: bold;";
                } else if (course.isInProgress()) {
                    status = "▶ ";
                    style = "-fx-text-fill: blue;";
                } else if (!course.hasMetPrerequisites()) {
                    status = "⚠ ";
                    style = "-fx-text-fill: orange;";
                } else if (course.hasConflict()) {
                    status = "⚠ ";
                    style = "-fx-text-fill: red;";
                }

                if (Boolean.TRUE.equals(course.getIsRequired())) {
                    status += "★ ";
                }

                String text = String.format("%s%s - %s (%.1f cr)",
                    status,
                    course.getCourse().getCourseCode(),
                    course.getCourse().getCourseName(),
                    course.getCredits() != null ? course.getCredits() : 0.0
                );

                if (course.getGradeEarned() != null) {
                    text += " [" + course.getGradeEarned() + "]";
                }

                setText(text);
                setStyle(style);
            }
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showWarning(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            CopyableErrorDialog.showError("Error", message);
        });
    }

    // ========================================================================
    // Inner Classes
    // ========================================================================

    /**
     * Simple POJO for Graduation Requirements display
     * Represents a subject area requirement with required vs completed credits
     */
    public static class GraduationRequirement {
        private final String subject;
        private final double required;
        private final double completed;
        private final String status;

        public GraduationRequirement(String subject, double required, double completed) {
            this.subject = subject;
            this.required = required;
            this.completed = completed;

            // Calculate status
            if (completed >= required) {
                this.status = "✅ Complete";
            } else if (completed >= required * 0.5) {
                this.status = "⚠️ In Progress";
            } else {
                this.status = "❌ Not Started";
            }
        }

        public String getSubject() {
            return subject;
        }

        public Double getRequired() {
            return required;
        }

        public Double getCompleted() {
            return completed;
        }

        public String getStatus() {
            return status;
        }
    }
}
