package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.service.GradebookService;
import com.heronix.service.CourseService;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.print.PrinterJob;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gradebook Controller
 *
 * Handles the gradebook UI for:
 * - Assignment management (create, edit, delete)
 * - Grade entry for students
 * - Category management with weighted percentages
 * - Grade calculations and reports
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Slf4j
@Controller
public class GradebookController {

    // ========================================================================
    // FXML COMPONENTS - Header
    // ========================================================================

    @FXML private ComboBox<Course> courseComboBox;
    @FXML private ComboBox<String> termComboBox;
    @FXML private HBox categorySummaryBar;

    // ========================================================================
    // FXML COMPONENTS - Assignments Table
    // ========================================================================

    @FXML private TableView<Assignment> assignmentsTable;
    @FXML private TableColumn<Assignment, String> categoryColumn;
    @FXML private TableColumn<Assignment, String> titleColumn;
    @FXML private TableColumn<Assignment, String> dueDateColumn;
    @FXML private TableColumn<Assignment, Number> maxPointsColumn;
    @FXML private TableColumn<Assignment, String> classAvgColumn;
    @FXML private TableColumn<Assignment, String> gradedColumn;
    @FXML private TableColumn<Assignment, String> statusColumn;
    @FXML private TableColumn<Assignment, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label assignmentCountLabel;

    // ========================================================================
    // FXML COMPONENTS - Grade Entry Panel
    // ========================================================================

    @FXML private VBox gradeEntryPanel;
    @FXML private Label selectedAssignmentLabel;
    @FXML private Label assignmentDetailsLabel;

    @FXML private TableView<StudentGradeRow> studentGradesTable;
    @FXML private TableColumn<StudentGradeRow, String> studentNameColumn;
    @FXML private TableColumn<StudentGradeRow, String> scoreColumn;
    @FXML private TableColumn<StudentGradeRow, String> percentColumn;
    @FXML private TableColumn<StudentGradeRow, String> letterGradeColumn;
    @FXML private TableColumn<StudentGradeRow, String> gradeStatusColumn;

    // Statistics labels
    @FXML private Label classAvgLabel;
    @FXML private Label classHighLabel;
    @FXML private Label classLowLabel;
    @FXML private Label gradedCountLabel;

    @FXML private Label statusLabel;

    // ========================================================================
    // SERVICES
    // ========================================================================

    @Autowired
    private GradebookService gradebookService;

    @Autowired
    private CourseService courseService;

    // ========================================================================
    // DATA
    // ========================================================================

    private ObservableList<Assignment> allAssignments = FXCollections.observableArrayList();
    private FilteredList<Assignment> filteredAssignments;
    private ObservableList<StudentGradeRow> studentGrades = FXCollections.observableArrayList();
    private Assignment selectedAssignment;
    private Course selectedCourse;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing GradebookController");

        setupCourseComboBox();
        setupTermComboBox();
        setupAssignmentsTable();
        setupStudentGradesTable();
        setupFilters();
        setupEventListeners();

        Platform.runLater(this::loadCourses);
    }

    private void setupCourseComboBox() {
        courseComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Course course) {
                return course != null ? course.getCourseCode() + " - " + course.getCourseName() : "";
            }

            @Override
            public Course fromString(String string) {
                return null;
            }
        });

        courseComboBox.setOnAction(e -> {
            selectedCourse = courseComboBox.getValue();
            if (selectedCourse != null) {
                loadAssignmentsForCourse(selectedCourse.getId());
                loadCategorySummary(selectedCourse.getId());
            }
        });
    }

    private void setupTermComboBox() {
        termComboBox.setItems(FXCollections.observableArrayList(
            "All Terms", "Q1", "Q2", "Q3", "Q4", "Semester 1", "Semester 2", "Full Year"
        ));
        termComboBox.setValue("All Terms");
        termComboBox.setOnAction(e -> applyFilters());
    }

    private void setupAssignmentsTable() {
        // Category column
        categoryColumn.setCellValueFactory(data -> {
            GradingCategory cat = data.getValue().getCategory();
            return new SimpleStringProperty(cat != null ? cat.getName() : "");
        });
        categoryColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Assignment assignment = getTableRow().getItem();
                    if (assignment != null && assignment.getCategory() != null) {
                        String color = assignment.getCategory().getColor();
                        setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Title column
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        // Due date column
        dueDateColumn.setCellValueFactory(data -> {
            LocalDate dueDate = data.getValue().getDueDate();
            return new SimpleStringProperty(dueDate != null ? dueDate.format(DATE_FORMAT) : "—");
        });

        // Max points column
        maxPointsColumn.setCellValueFactory(data -> new SimpleDoubleProperty(
            data.getValue().getMaxPoints() != null ? data.getValue().getMaxPoints() : 0
        ));

        // Class average column
        classAvgColumn.setCellValueFactory(data -> {
            Double avg = data.getValue().getClassAveragePercent();
            return new SimpleStringProperty(avg != null ? String.format("%.1f%%", avg) : "—");
        });

        // Graded count column
        gradedColumn.setCellValueFactory(data -> {
            Assignment a = data.getValue();
            int graded = a.getSubmissionCount();
            int total = a.getCourse() != null && a.getCourse().getStudents() != null
                ? a.getCourse().getStudents().size() : 0;
            return new SimpleStringProperty(graded + "/" + total);
        });

        // Status column with color coding
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    String color = switch (status) {
                        case "Draft" -> "#9E9E9E";
                        case "Past Due" -> "#F44336";
                        case "Due Soon" -> "#FF9800";
                        case "Active" -> "#4CAF50";
                        default -> "#ffffff";
                    };
                    setStyle("-fx-text-fill: " + color + ";");
                }
            }
        });

        // Actions column
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Del");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("button-small");
                deleteBtn.getStyleClass().addAll("button-small", "button-danger");
                box.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> handleEditAssignment(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDeleteAssignment(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Selection listener
        assignmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedAssignment = newVal;
                loadStudentGradesForAssignment(newVal);
            }
        });

        filteredAssignments = new FilteredList<>(allAssignments);
        assignmentsTable.setItems(filteredAssignments);
    }

    private void setupStudentGradesTable() {
        studentNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentName()));

        // Editable score column
        scoreColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScoreDisplay()));
        scoreColumn.setCellFactory(col -> new TableCell<>() {
            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> applyGradeEdit(textField.getText()));
                textField.focusedProperty().addListener((obs, wasFocused, isNow) -> {
                    if (!isNow) {
                        applyGradeEdit(textField.getText());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    StudentGradeRow row = getTableRow().getItem();
                    if (row != null) {
                        textField.setText(row.getScoreDisplay());
                        textField.setPrefWidth(60);
                        setGraphic(textField);
                    }
                }
            }

            private void applyGradeEdit(String value) {
                StudentGradeRow row = getTableRow().getItem();
                if (row != null && value != null && !value.isEmpty()) {
                    try {
                        double score = Double.parseDouble(value);
                        saveGrade(row.getStudentId(), selectedAssignment.getId(), score);
                        row.setScore(score);
                        updateStatistics();
                    } catch (NumberFormatException ex) {
                        // Invalid number, ignore
                    }
                }
            }
        });

        percentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPercentDisplay()));
        letterGradeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLetterGrade()));

        // Status with color
        gradeStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        gradeStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    AssignmentGrade.GradeStatus gradeStatus = AssignmentGrade.GradeStatus.valueOf(status.toUpperCase().replace(" ", "_"));
                    setStyle("-fx-text-fill: " + gradeStatus.getColor() + ";");
                }
            }
        });

        studentGradesTable.setItems(studentGrades);
    }

    private void setupFilters() {
        categoryFilterComboBox.setItems(FXCollections.observableArrayList("All Categories"));
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "All Status", "Draft", "Active", "Due Soon", "Past Due"
        ));
        statusFilterComboBox.setValue("All Status");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        categoryFilterComboBox.setOnAction(e -> applyFilters());
        statusFilterComboBox.setOnAction(e -> applyFilters());
    }

    private void setupEventListeners() {
        // Nothing extra needed here for now
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    private void loadCourses() {
        try {
            List<Course> courses = courseService.getAllActiveCourses();
            courseComboBox.setItems(FXCollections.observableArrayList(courses));
            if (!courses.isEmpty()) {
                courseComboBox.setValue(courses.get(0));
            }
            statusLabel.setText("Loaded " + courses.size() + " courses");
        } catch (Exception e) {
            log.error("Failed to load courses", e);
            showError("Error", "Failed to load courses: " + e.getMessage());
        }
    }

    private void loadAssignmentsForCourse(Long courseId) {
        try {
            List<Assignment> assignments = gradebookService.getAssignmentsForCourse(courseId);
            allAssignments.setAll(assignments);
            assignmentCountLabel.setText(assignments.size() + " assignments");

            // Update category filter options
            Set<String> categories = assignments.stream()
                .filter(a -> a.getCategory() != null)
                .map(a -> a.getCategory().getName())
                .collect(Collectors.toSet());
            List<String> catList = new ArrayList<>();
            catList.add("All Categories");
            catList.addAll(categories);
            categoryFilterComboBox.setItems(FXCollections.observableArrayList(catList));

            // Clear grade entry panel
            selectedAssignment = null;
            selectedAssignmentLabel.setText("Select an assignment");
            assignmentDetailsLabel.setText("");
            studentGrades.clear();
            clearStatistics();

            statusLabel.setText("Loaded " + assignments.size() + " assignments for " + selectedCourse.getCourseName());
        } catch (Exception e) {
            log.error("Failed to load assignments", e);
            showError("Error", "Failed to load assignments: " + e.getMessage());
        }
    }

    private void loadCategorySummary(Long courseId) {
        try {
            List<GradingCategory> categories = gradebookService.getCategoriesForCourse(courseId);
            categorySummaryBar.getChildren().clear();
            categorySummaryBar.getChildren().add(new Label("Categories:"));

            if (categories.isEmpty()) {
                Button createBtn = new Button("Create Default Categories");
                createBtn.getStyleClass().add("button-primary");
                createBtn.setOnAction(e -> createDefaultCategories(courseId));
                categorySummaryBar.getChildren().add(createBtn);
            } else {
                for (GradingCategory cat : categories) {
                    Label label = new Label(cat.getDisplayNameWithWeight());
                    label.setStyle("-fx-background-color: " + cat.getColor() + "22; " +
                        "-fx-text-fill: " + cat.getColor() + "; " +
                        "-fx-padding: 5 10; -fx-background-radius: 3;");
                    categorySummaryBar.getChildren().add(label);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load category summary", e);
        }
    }

    private void loadStudentGradesForAssignment(Assignment assignment) {
        selectedAssignmentLabel.setText(assignment.getTitle());
        assignmentDetailsLabel.setText(String.format("%s | Due: %s | Max: %.0f pts",
            assignment.getCategory() != null ? assignment.getCategory().getName() : "—",
            assignment.getDueDate() != null ? assignment.getDueDate().format(DATE_FORMAT) : "—",
            assignment.getMaxPoints()));

        try {
            // Get enrolled students
            Course course = assignment.getCourse();
            List<Student> students = course.getStudents();
            if (students == null) students = new ArrayList<>();

            List<StudentGradeRow> rows = new ArrayList<>();
            for (Student student : students) {
                // Find existing grade or create placeholder
                AssignmentGrade grade = assignment.getGrades().stream()
                    .filter(g -> g.getStudent().getId().equals(student.getId()))
                    .findFirst()
                    .orElse(null);

                StudentGradeRow row = new StudentGradeRow();
                row.setStudentId(student.getId());
                row.setStudentName(student.getFirstName() + " " + student.getLastName());
                row.setMaxPoints(assignment.getMaxPoints());

                if (grade != null) {
                    row.setScore(grade.getScore());
                    row.setStatus(grade.getStatus().getDisplayName());
                } else {
                    row.setScore(null);
                    row.setStatus("Not Submitted");
                }

                rows.add(row);
            }

            // Sort by name
            rows.sort(Comparator.comparing(StudentGradeRow::getStudentName));
            studentGrades.setAll(rows);

            updateStatistics();
        } catch (Exception e) {
            log.error("Failed to load student grades", e);
            showError("Error", "Failed to load student grades: " + e.getMessage());
        }
    }

    // ========================================================================
    // FILTERS
    // ========================================================================

    private void applyFilters() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String category = categoryFilterComboBox.getValue();
        String status = statusFilterComboBox.getValue();
        String term = termComboBox.getValue();

        filteredAssignments.setPredicate(a -> {
            // Search filter
            if (!search.isEmpty() && !a.getTitle().toLowerCase().contains(search)) {
                return false;
            }

            // Category filter
            if (category != null && !"All Categories".equals(category)) {
                if (a.getCategory() == null || !a.getCategory().getName().equals(category)) {
                    return false;
                }
            }

            // Status filter
            if (status != null && !"All Status".equals(status)) {
                if (!a.getStatus().equals(status)) {
                    return false;
                }
            }

            // Term filter
            if (term != null && !"All Terms".equals(term)) {
                if (a.getTerm() == null || !a.getTerm().equals(term)) {
                    return false;
                }
            }

            return true;
        });

        assignmentCountLabel.setText(filteredAssignments.size() + " assignments");
    }

    // ========================================================================
    // ACTION HANDLERS
    // ========================================================================

    @FXML
    private void handleAddAssignment() {
        if (selectedCourse == null) {
            showWarning("No Course Selected", "Please select a course first.");
            return;
        }

        showAssignmentDialog(null);
    }

    @FXML
    private void handleManageCategories() {
        if (selectedCourse == null) {
            showWarning("No Course Selected", "Please select a course first.");
            return;
        }

        showCategoryDialog();
    }

    @FXML
    private void handleRefresh() {
        if (selectedCourse != null) {
            loadAssignmentsForCourse(selectedCourse.getId());
            loadCategorySummary(selectedCourse.getId());
        }
    }

    @FXML
    private void handleGradeAll() {
        if (selectedAssignment == null) {
            showWarning("No Assignment Selected", "Please select an assignment first.");
            return;
        }

        // Show dialog to enter grade for all students at once
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Grade All Students");
        dialog.setHeaderText("Enter score to apply to all students");
        dialog.setContentText("Score (0-" + selectedAssignment.getMaxPoints().intValue() + "):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double score = Double.parseDouble(input);
                if (score < 0 || score > selectedAssignment.getMaxPoints()) {
                    showWarning("Invalid Score",
                        "Score must be between 0 and " + selectedAssignment.getMaxPoints().intValue());
                    return;
                }

                int count = 0;
                for (StudentGradeRow row : studentGrades) {
                    saveGrade(row.getStudentId(), selectedAssignment.getId(), score);
                    row.setScore(score);
                    count++;
                }

                studentGradesTable.refresh();
                updateStatistics();
                statusLabel.setText("Graded " + count + " students");
            } catch (NumberFormatException e) {
                showWarning("Invalid Input", "Please enter a valid number.");
            }
        });
    }

    @FXML
    private void handleMarkAllMissing() {
        if (selectedAssignment == null) {
            showWarning("No Assignment Selected", "Please select an assignment first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mark All Missing");
        confirm.setHeaderText("Mark all ungraded students as missing?");
        confirm.setContentText("This will mark all students without a grade as 'Missing'.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int count = 0;
                for (StudentGradeRow row : studentGrades) {
                    if (row.getScore() == null) {
                        gradebookService.excuseGrade(row.getStudentId(), selectedAssignment.getId(), "Missing");
                        row.setStatus("Missing");
                        count++;
                    }
                }
                studentGradesTable.refresh();
                statusLabel.setText("Marked " + count + " students as missing");
            }
        });
    }

    @FXML
    private void handleExportGrades() {
        if (selectedCourse == null) {
            showWarning("No Course Selected", "Please select a course first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Grades");
        fileChooser.setInitialFileName(selectedCourse.getCourseCode() + "_grades.csv");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showSaveDialog(assignmentsTable.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                exportGradesToCSV(writer);
                statusLabel.setText("Exported grades to " + file.getName());
                showInfo("Export Complete", "Grades exported successfully.");
            } catch (Exception e) {
                log.error("Failed to export grades", e);
                showError("Export Failed", "Failed to export grades: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handlePrintReport() {
        if (selectedCourse == null) {
            showWarning("No Course Selected", "Please select a course first.");
            return;
        }

        String reportContent = buildGradebookReportContent();

        // Create a TextFlow for printing
        TextFlow textFlow = new TextFlow();
        Text text = new Text(reportContent);
        text.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 9px;");
        textFlow.getChildren().add(text);
        textFlow.setPrefWidth(700);

        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob != null) {
            boolean showDialog = printerJob.showPrintDialog(assignmentsTable.getScene().getWindow());
            if (showDialog) {
                boolean success = printerJob.printPage(textFlow);
                if (success) {
                    printerJob.endJob();
                    log.info("Gradebook printed successfully");
                    statusLabel.setText("Gradebook sent to printer");
                    showInfo("Print Complete", "Gradebook has been sent to the printer.");
                } else {
                    showError("Print Error", "Failed to print gradebook.");
                }
            }
        } else {
            showError("Print Error", "No printer available.");
        }
    }

    /**
     * Build formatted gradebook report content for printing
     */
    private String buildGradebookReportContent() {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

        report.append("================================================================================\n");
        report.append("                          GRADEBOOK REPORT                                      \n");
        report.append("================================================================================\n\n");

        report.append("Report Generated: ").append(LocalDate.now().format(dateFormatter)).append("\n\n");

        // Course Information
        report.append("COURSE INFORMATION\n");
        report.append("--------------------------------------------------------------------------------\n");
        report.append(String.format("%-15s: %s%n", "Course", selectedCourse.getCourseName()));
        report.append(String.format("%-15s: %s%n", "Course Code", selectedCourse.getCourseCode()));
        report.append(String.format("%-15s: %s%n", "Term", termComboBox.getValue() != null ? termComboBox.getValue() : "Current"));
        report.append(String.format("%-15s: %d%n", "Assignments", allAssignments.size()));
        report.append("\n");

        // Assignments Summary
        report.append("ASSIGNMENTS\n");
        report.append("--------------------------------------------------------------------------------\n");
        report.append(String.format("%-30s %-15s %-10s %-10s%n", "Title", "Category", "Points", "Due Date"));
        report.append("--------------------------------------------------------------------------------\n");
        for (Assignment assignment : allAssignments) {
            String dueDate = assignment.getDueDate() != null ?
                assignment.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A";
            report.append(String.format("%-30s %-15s %-10.0f %-10s%n",
                truncate(assignment.getTitle(), 28),
                truncate(assignment.getCategory() != null ? assignment.getCategory().getName() : "N/A", 13),
                assignment.getMaxPoints() != null ? assignment.getMaxPoints() : 0.0,
                dueDate
            ));
        }
        report.append("\n");

        // Student Grades
        if (!studentGrades.isEmpty()) {
            report.append("STUDENT GRADES\n");
            report.append("--------------------------------------------------------------------------------\n");
            report.append(String.format("%-30s %-10s %-15s%n", "Student Name", "Score", "Grade"));
            report.append("--------------------------------------------------------------------------------\n");
            for (StudentGradeRow row : studentGrades) {
                report.append(String.format("%-30s %-10s %-15s%n",
                    truncate(row.getStudentName(), 28),
                    row.getPercentDisplay(),
                    row.getLetterGrade()
                ));
            }
            report.append("\n");

            // Class Statistics
            report.append("CLASS STATISTICS\n");
            report.append("--------------------------------------------------------------------------------\n");
            double classAvg = studentGrades.stream()
                .filter(r -> r.getScore() != null && r.getMaxPoints() != null && r.getMaxPoints() > 0)
                .mapToDouble(r -> (r.getScore() / r.getMaxPoints()) * 100)
                .average()
                .orElse(0.0);
            double highestGrade = studentGrades.stream()
                .filter(r -> r.getScore() != null && r.getMaxPoints() != null && r.getMaxPoints() > 0)
                .mapToDouble(r -> (r.getScore() / r.getMaxPoints()) * 100)
                .max()
                .orElse(0.0);
            double lowestGrade = studentGrades.stream()
                .filter(r -> r.getScore() != null && r.getMaxPoints() != null && r.getMaxPoints() > 0)
                .mapToDouble(r -> (r.getScore() / r.getMaxPoints()) * 100)
                .min()
                .orElse(0.0);

            report.append(String.format("%-20s: %.1f%%%n", "Class Average", classAvg));
            report.append(String.format("%-20s: %.1f%%%n", "Highest Grade", highestGrade));
            report.append(String.format("%-20s: %.1f%%%n", "Lowest Grade", lowestGrade));
            report.append(String.format("%-20s: %d%n", "Total Students", studentGrades.size()));
        }

        report.append("\n");
        report.append("================================================================================\n");
        report.append("                        END OF GRADEBOOK REPORT                                 \n");
        report.append("================================================================================\n");

        return report.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 2) + "..";
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void handleEditAssignment(Assignment assignment) {
        showAssignmentDialog(assignment);
    }

    private void handleDeleteAssignment(Assignment assignment) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Assignment");
        confirm.setHeaderText("Delete '" + assignment.getTitle() + "'?");
        confirm.setContentText("This will also delete all grades for this assignment.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Would call gradebookService.deleteAssignment(assignment.getId());
                allAssignments.remove(assignment);
                statusLabel.setText("Deleted assignment: " + assignment.getTitle());
            }
        });
    }

    private void showAssignmentDialog(Assignment existing) {
        Dialog<Assignment> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New Assignment" : "Edit Assignment");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Form fields
        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        titleField.setPromptText("Assignment title");

        ComboBox<GradingCategory> categoryBox = new ComboBox<>();
        categoryBox.setItems(FXCollections.observableArrayList(
            gradebookService.getCategoriesForCourse(selectedCourse.getId())
        ));
        categoryBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(GradingCategory gc) {
                return gc != null ? gc.getName() : "";
            }

            @Override
            public GradingCategory fromString(String s) {
                return null;
            }
        });
        if (existing != null && existing.getCategory() != null) {
            categoryBox.setValue(existing.getCategory());
        }

        TextField maxPointsField = new TextField(existing != null ?
            String.valueOf(existing.getMaxPoints().intValue()) : "100");
        maxPointsField.setPromptText("Max points");

        DatePicker dueDatePicker = new DatePicker(existing != null ? existing.getDueDate() : LocalDate.now().plusDays(7));

        CheckBox publishedCheck = new CheckBox("Published");
        publishedCheck.setSelected(existing != null && Boolean.TRUE.equals(existing.getPublished()));

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryBox, 1, 1);
        grid.add(new Label("Max Points:"), 0, 2);
        grid.add(maxPointsField, 1, 2);
        grid.add(new Label("Due Date:"), 0, 3);
        grid.add(dueDatePicker, 1, 3);
        grid.add(publishedCheck, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String title = titleField.getText().trim();
                    GradingCategory category = categoryBox.getValue();
                    double maxPoints = Double.parseDouble(maxPointsField.getText());
                    LocalDate dueDate = dueDatePicker.getValue();

                    if (title.isEmpty() || category == null) {
                        showWarning("Validation Error", "Title and Category are required.");
                        return null;
                    }

                    if (existing == null) {
                        Assignment created = gradebookService.createAssignment(
                            selectedCourse.getId(), category.getId(), title, maxPoints, dueDate);
                        if (publishedCheck.isSelected()) {
                            gradebookService.publishAssignment(created.getId());
                        }
                        return created;
                    } else {
                        // Update existing - would need update method in service
                        existing.setTitle(title);
                        existing.setCategory(category);
                        existing.setMaxPoints(maxPoints);
                        existing.setDueDate(dueDate);
                        existing.setPublished(publishedCheck.isSelected());
                        return existing;
                    }
                } catch (NumberFormatException e) {
                    showWarning("Validation Error", "Max points must be a number.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (existing == null) {
                allAssignments.add(result);
            }
            assignmentsTable.refresh();
            statusLabel.setText("Assignment saved: " + result.getTitle());
        });
    }

    private void showCategoryDialog() {
        List<GradingCategory> categories = gradebookService.getCategoriesForCourse(selectedCourse.getId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Categories");
        dialog.setHeaderText("Grading categories for " + selectedCourse.getCourseName());
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        if (categories.isEmpty()) {
            Label emptyLabel = new Label("No categories defined. Click below to create defaults.");
            Button createBtn = new Button("Create Default Categories");
            createBtn.setOnAction(e -> {
                createDefaultCategories(selectedCourse.getId());
                dialog.close();
            });
            content.getChildren().addAll(emptyLabel, createBtn);
        } else {
            Label totalLabel = new Label();
            double total = categories.stream().mapToDouble(GradingCategory::getWeight).sum();
            totalLabel.setText(String.format("Total Weight: %.0f%% %s", total, total == 100 ? "✓" : "(should be 100%)"));
            totalLabel.setStyle(total == 100 ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #F44336;");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(8);

            grid.add(new Label("Category"), 0, 0);
            grid.add(new Label("Weight %"), 1, 0);
            grid.add(new Label("Drop Lowest"), 2, 0);

            int row = 1;
            for (GradingCategory cat : categories) {
                Label nameLabel = new Label(cat.getName());
                nameLabel.setStyle("-fx-text-fill: " + cat.getColor() + ";");

                TextField weightField = new TextField(String.valueOf(cat.getWeight().intValue()));
                weightField.setPrefWidth(60);

                TextField dropField = new TextField(String.valueOf(cat.getDropLowest()));
                dropField.setPrefWidth(60);

                grid.add(nameLabel, 0, row);
                grid.add(weightField, 1, row);
                grid.add(dropField, 2, row);
                row++;
            }

            content.getChildren().addAll(totalLabel, grid);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
        loadCategorySummary(selectedCourse.getId());
    }

    private void createDefaultCategories(Long courseId) {
        try {
            gradebookService.createDefaultCategories(courseId);
            loadCategorySummary(courseId);
            statusLabel.setText("Created default grading categories");
        } catch (Exception e) {
            log.error("Failed to create default categories", e);
            showError("Error", "Failed to create categories: " + e.getMessage());
        }
    }

    private void saveGrade(Long studentId, Long assignmentId, Double score) {
        try {
            gradebookService.enterGrade(studentId, assignmentId, score, LocalDate.now(), null);
        } catch (Exception e) {
            log.error("Failed to save grade", e);
        }
    }

    private void updateStatistics() {
        if (studentGrades.isEmpty()) {
            clearStatistics();
            return;
        }

        List<Double> scores = studentGrades.stream()
            .filter(r -> r.getScore() != null)
            .map(r -> (r.getScore() / r.getMaxPoints()) * 100)
            .collect(Collectors.toList());

        if (scores.isEmpty()) {
            clearStatistics();
            return;
        }

        DoubleSummaryStatistics stats = scores.stream()
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();

        classAvgLabel.setText(String.format("%.1f%%", stats.getAverage()));
        classHighLabel.setText(String.format("%.1f%%", stats.getMax()));
        classLowLabel.setText(String.format("%.1f%%", stats.getMin()));
        gradedCountLabel.setText(scores.size() + "/" + studentGrades.size());
    }

    private void clearStatistics() {
        classAvgLabel.setText("—");
        classHighLabel.setText("—");
        classLowLabel.setText("—");
        gradedCountLabel.setText("0/0");
    }

    private void exportGradesToCSV(PrintWriter writer) {
        // Header
        writer.print("Student");
        for (Assignment a : allAssignments) {
            writer.print("," + a.getTitle() + " (" + a.getMaxPoints().intValue() + ")");
        }
        writer.println(",Final Grade,Letter");

        // Get class gradebook
        GradebookService.ClassGradebook gradebook = gradebookService.getClassGradebook(selectedCourse.getId());

        for (GradebookService.StudentCourseGrade sg : gradebook.getStudentGrades()) {
            // Would need student name lookup
            writer.print("Student " + sg.getStudentId());

            // Print each assignment score (would need to fetch actual grades)
            for (Assignment a : allAssignments) {
                writer.print(",—");
            }

            writer.println("," + String.format("%.1f", sg.getFinalPercentage()) + "," + sg.getLetterGrade());
        }
    }

    // ========================================================================
    // ALERT HELPER METHODS
    // ========================================================================

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            CopyableErrorDialog.showInfo(title, message);
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            CopyableErrorDialog.showError(title, message);
        });
    }

    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            CopyableErrorDialog.showWarning(title, message);
        });
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Row model for student grades table
     */
    public static class StudentGradeRow {
        private Long studentId;
        private String studentName;
        private Double score;
        private Double maxPoints;
        private String status;

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long id) { this.studentId = id; }

        public String getStudentName() { return studentName; }
        public void setStudentName(String name) { this.studentName = name; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }

        public Double getMaxPoints() { return maxPoints; }
        public void setMaxPoints(Double max) { this.maxPoints = max; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getScoreDisplay() {
            return score != null ? String.format("%.1f", score) : "";
        }

        public String getPercentDisplay() {
            if (score == null || maxPoints == null || maxPoints == 0) return "—";
            return String.format("%.0f%%", (score / maxPoints) * 100);
        }

        public String getLetterGrade() {
            if (score == null || maxPoints == null || maxPoints == 0) return "—";
            double pct = (score / maxPoints) * 100;
            if (pct >= 97) return "A+";
            if (pct >= 93) return "A";
            if (pct >= 90) return "A-";
            if (pct >= 87) return "B+";
            if (pct >= 83) return "B";
            if (pct >= 80) return "B-";
            if (pct >= 77) return "C+";
            if (pct >= 73) return "C";
            if (pct >= 70) return "C-";
            if (pct >= 67) return "D+";
            if (pct >= 63) return "D";
            if (pct >= 60) return "D-";
            return "F";
        }
    }
}
