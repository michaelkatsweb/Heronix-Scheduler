package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import com.heronix.service.AdminGradebookService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Administrator Gradebook Controller
 *
 * Provides comprehensive grade management for administrators including:
 * - District-wide grade viewing and searching
 * - Grade editing with audit trail
 * - Transfer student grade entry
 * - Bulk operations
 * - Audit log viewing
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-28
 */
@Controller
public class AdminGradebookController {

    private static final Logger logger = LoggerFactory.getLogger(AdminGradebookController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private AdminGradebookService adminGradebookService;

    @Autowired
    private StudentGradeRepository studentGradeRepository;

    @Autowired
    private GradeAuditLogRepository gradeAuditLogRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    // ==================== FILTER COMPONENTS ====================

    @FXML private ComboBox<Integer> academicYearComboBox;
    @FXML private ComboBox<String> termComboBox;
    @FXML private ComboBox<Course> courseComboBox;
    @FXML private ComboBox<Teacher> teacherComboBox;
    @FXML private TextField studentSearchField;
    @FXML private ComboBox<String> minGradeComboBox;
    @FXML private ComboBox<String> maxGradeComboBox;
    @FXML private CheckBox showFailingCheckBox;
    @FXML private CheckBox showIncompleteCheckBox;
    @FXML private CheckBox showTransferCheckBox;

    // ==================== SUMMARY LABELS ====================

    @FXML private Label totalGradesLabel;
    @FXML private Label averageGpaLabel;
    @FXML private Label failingGradesLabel;
    @FXML private Label recentEditsLabel;

    // ==================== GRADES TABLE ====================

    @FXML private TableView<StudentGrade> gradesTable;
    @FXML private TableColumn<StudentGrade, Long> gradeIdColumn;
    @FXML private TableColumn<StudentGrade, String> studentNameColumn;
    @FXML private TableColumn<StudentGrade, String> studentIdColumn;
    @FXML private TableColumn<StudentGrade, String> courseColumn;
    @FXML private TableColumn<StudentGrade, String> teacherColumn;
    @FXML private TableColumn<StudentGrade, String> termColumn;
    @FXML private TableColumn<StudentGrade, Integer> yearColumn;
    @FXML private TableColumn<StudentGrade, String> letterGradeColumn;
    @FXML private TableColumn<StudentGrade, Double> numericalGradeColumn;
    @FXML private TableColumn<StudentGrade, Double> gpaColumn;
    @FXML private TableColumn<StudentGrade, Double> creditsColumn;
    @FXML private TableColumn<StudentGrade, String> statusColumn;
    @FXML private TableColumn<StudentGrade, String> lastEditedColumn;
    @FXML private TableColumn<StudentGrade, Void> actionsColumn;

    // ==================== PAGINATION ====================

    @FXML private Label recordCountLabel;
    @FXML private Label pageInfoLabel;
    @FXML private ComboBox<String> pageSizeComboBox;

    // ==================== AUDIT LOG ====================

    @FXML private TableView<GradeAuditLog> auditLogTable;
    @FXML private TableColumn<GradeAuditLog, String> auditTimestampColumn;
    @FXML private TableColumn<GradeAuditLog, String> auditAdminColumn;
    @FXML private TableColumn<GradeAuditLog, String> auditStudentColumn;
    @FXML private TableColumn<GradeAuditLog, String> auditCourseColumn;
    @FXML private TableColumn<GradeAuditLog, String> auditFieldColumn;
    @FXML private TableColumn<GradeAuditLog, String> auditOldValueColumn;
    @FXML private TableColumn<GradeAuditLog, String> auditNewValueColumn;
    @FXML private TableColumn<GradeAuditLog, String> auditReasonColumn;
    @FXML private ComboBox<String> auditLogLimitComboBox;

    // ==================== STATUS BAR ====================

    @FXML private Label lastRefreshLabel;

    // ==================== DATA ====================

    private ObservableList<StudentGrade> grades = FXCollections.observableArrayList();
    private ObservableList<GradeAuditLog> auditLogs = FXCollections.observableArrayList();
    private int currentPage = 0;
    private int pageSize = 50;
    private int totalPages = 0;
    private User currentAdmin;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Admin Gradebook Controller");

        // Get current admin user (in production, this would come from authentication)
        currentAdmin = getCurrentAdmin();

        setupFilters();
        setupGradesTable();
        setupAuditLogTable();
        setupPagination();

        loadData();
    }

    // ========================================================================
    // SETUP METHODS
    // ========================================================================

    /**
     * Setup filter components
     */
    private void setupFilters() {
        // Academic Years
        List<Integer> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            years.add(i);
        }
        academicYearComboBox.setItems(FXCollections.observableArrayList(years));
        academicYearComboBox.setValue(currentYear);

        // Terms
        termComboBox.setItems(FXCollections.observableArrayList(
            "Fall", "Spring", "Summer", "Q1", "Q2", "Q3", "Q4"
        ));

        // Grade ranges
        List<String> letterGrades = Arrays.asList("A+", "A", "A-", "B+", "B", "B-",
            "C+", "C", "C-", "D+", "D", "D-", "F");
        minGradeComboBox.setItems(FXCollections.observableArrayList(letterGrades));
        maxGradeComboBox.setItems(FXCollections.observableArrayList(letterGrades));

        // Courses
        List<Course> courses = courseRepository.findAll();
        courseComboBox.setItems(FXCollections.observableArrayList(courses));
        courseComboBox.setConverter(new javafx.util.StringConverter<Course>() {
            @Override
            public String toString(Course course) {
                return course != null ? course.getCourseCode() + " - " + course.getCourseName() : "";
            }
            @Override
            public Course fromString(String string) {
                return null;
            }
        });

        // Teachers
        List<Teacher> teachers = teacherRepository.findAllActive();
        teacherComboBox.setItems(FXCollections.observableArrayList(teachers));
        teacherComboBox.setConverter(new javafx.util.StringConverter<Teacher>() {
            @Override
            public String toString(Teacher teacher) {
                return teacher != null ? teacher.getName() : "";
            }
            @Override
            public Teacher fromString(String string) {
                return null;
            }
        });

        // Audit log limit
        auditLogLimitComboBox.setValue("25");
    }

    /**
     * Setup grades table
     */
    private void setupGradesTable() {
        // Configure columns
        gradeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        studentNameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStudentName()));

        studentIdColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStudent() != null ?
                cellData.getValue().getStudent().getStudentId() : ""));

        courseColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCourseName()));

        teacherColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTeacher() != null ?
                cellData.getValue().getTeacher().getName() : ""));

        termColumn.setCellValueFactory(new PropertyValueFactory<>("term"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("academicYear"));
        letterGradeColumn.setCellValueFactory(new PropertyValueFactory<>("letterGrade"));
        numericalGradeColumn.setCellValueFactory(new PropertyValueFactory<>("numericalGrade"));
        gpaColumn.setCellValueFactory(new PropertyValueFactory<>("gpaPoints"));
        creditsColumn.setCellValueFactory(new PropertyValueFactory<>("credits"));

        statusColumn.setCellValueFactory(cellData -> {
            StudentGrade grade = cellData.getValue();
            String status = grade.isPassing() ? "Passing" : "Failing";
            if ("I".equals(grade.getLetterGrade())) status = "Incomplete";
            if ("W".equals(grade.getLetterGrade())) status = "Withdrawn";
            return new SimpleStringProperty(status);
        });

        lastEditedColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getGradeDate() != null ?
                cellData.getValue().getGradeDate().format(DATE_FORMATTER) : ""));

        // Actions column with Edit and View History buttons
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button historyButton = new Button("History");
            private final HBox container = new HBox(5, editButton, historyButton);

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11;");
                historyButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 11;");
                container.setAlignment(Pos.CENTER);

                editButton.setOnAction(event -> {
                    StudentGrade grade = getTableView().getItems().get(getIndex());
                    handleEditGrade(grade);
                });

                historyButton.setOnAction(event -> {
                    StudentGrade grade = getTableView().getItems().get(getIndex());
                    handleViewGradeHistory(grade);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        gradesTable.setItems(grades);
    }

    /**
     * Setup audit log table
     */
    private void setupAuditLogTable() {
        auditTimestampColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTimestamp().format(DATE_TIME_FORMATTER)));

        auditAdminColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getAdminName()));

        auditStudentColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStudentName()));

        auditCourseColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCourseName()));

        auditFieldColumn.setCellValueFactory(new PropertyValueFactory<>("fieldName"));
        auditOldValueColumn.setCellValueFactory(new PropertyValueFactory<>("oldValue"));
        auditNewValueColumn.setCellValueFactory(new PropertyValueFactory<>("newValue"));
        auditReasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));

        auditLogTable.setItems(auditLogs);
    }

    /**
     * Setup pagination
     */
    private void setupPagination() {
        // Populate page size options
        pageSizeComboBox.getItems().addAll("25", "50", "100", "200");
        pageSizeComboBox.setValue("50");
        pageSizeComboBox.setOnAction(e -> {
            pageSize = Integer.parseInt(pageSizeComboBox.getValue());
            currentPage = 0;
            loadGrades();
        });
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load all data
     */
    private void loadData() {
        loadGrades();
        loadAuditLogs();
        updateSummaryStats();
        updateLastRefresh();
    }

    /**
     * Load grades with current filters
     */
    private void loadGrades() {
        try {
            // For now, load all grades (in production, use filtering and pagination)
            List<StudentGrade> allGrades = studentGradeRepository.findAll();

            // Apply filters
            allGrades = applyFilters(allGrades);

            // Update pagination
            totalPages = (int) Math.ceil((double) allGrades.size() / pageSize);
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, allGrades.size());

            List<StudentGrade> pageGrades = allGrades.subList(start, end);
            grades.setAll(pageGrades);

            recordCountLabel.setText("Showing: " + pageGrades.size() + " of " + allGrades.size() + " records");
            pageInfoLabel.setText("Page " + (currentPage + 1) + " of " + Math.max(1, totalPages));

            logger.info("Loaded {} grades (page {}/{})", pageGrades.size(), currentPage + 1, totalPages);
        } catch (Exception e) {
            logger.error("Error loading grades", e);
            showError("Error Loading Grades", "Failed to load grade records: " + e.getMessage());
        }
    }

    /**
     * Apply current filters to grade list
     */
    private List<StudentGrade> applyFilters(List<StudentGrade> allGrades) {
        return allGrades.stream()
            .filter(grade -> {
                // Academic year filter
                if (academicYearComboBox.getValue() != null &&
                    !academicYearComboBox.getValue().equals(grade.getAcademicYear())) {
                    return false;
                }

                // Term filter
                if (termComboBox.getValue() != null && !termComboBox.getValue().isEmpty() &&
                    !termComboBox.getValue().equals(grade.getTerm())) {
                    return false;
                }

                // Course filter
                if (courseComboBox.getValue() != null &&
                    !courseComboBox.getValue().getId().equals(grade.getCourse().getId())) {
                    return false;
                }

                // Teacher filter
                if (teacherComboBox.getValue() != null && grade.getTeacher() != null &&
                    !teacherComboBox.getValue().getId().equals(grade.getTeacher().getId())) {
                    return false;
                }

                // Student search
                if (studentSearchField.getText() != null && !studentSearchField.getText().trim().isEmpty()) {
                    String search = studentSearchField.getText().toLowerCase();
                    String studentName = grade.getStudentName().toLowerCase();
                    String studentId = grade.getStudent() != null ? grade.getStudent().getStudentId().toLowerCase() : "";
                    if (!studentName.contains(search) && !studentId.contains(search)) {
                        return false;
                    }
                }

                // Failing grades filter
                if (showFailingCheckBox.isSelected() && grade.isPassing()) {
                    return false;
                }

                // Incomplete filter
                if (showIncompleteCheckBox.isSelected() && !"I".equals(grade.getLetterGrade())) {
                    return false;
                }

                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * Load audit logs
     */
    private void loadAuditLogs() {
        try {
            int limit = Integer.parseInt(auditLogLimitComboBox.getValue());
            List<GradeAuditLog> logs = gradeAuditLogRepository.findTop100ByOrderByTimestampDesc();

            auditLogs.setAll(logs.stream().limit(limit).collect(Collectors.toList()));

            logger.info("Loaded {} audit log entries", auditLogs.size());
        } catch (Exception e) {
            logger.error("Error loading audit logs", e);
        }
    }

    /**
     * Update summary statistics
     */
    private void updateSummaryStats() {
        try {
            List<StudentGrade> allGrades = studentGradeRepository.findAll();

            totalGradesLabel.setText(String.valueOf(allGrades.size()));

            // Calculate average GPA
            double avgGpa = allGrades.stream()
                .filter(g -> g.getGpaPoints() != null && g.getIncludeInGPA())
                .mapToDouble(StudentGrade::getGpaPoints)
                .average()
                .orElse(0.0);
            averageGpaLabel.setText(String.format("%.2f", avgGpa));

            // Count failing grades
            long failingCount = allGrades.stream()
                .filter(g -> !g.isPassing())
                .count();
            failingGradesLabel.setText(String.valueOf(failingCount));

            // Count recent edits (last 24 hours)
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            long recentEdits = gradeAuditLogRepository.countRecentEdits(since);
            recentEditsLabel.setText(String.valueOf(recentEdits));

        } catch (Exception e) {
            logger.error("Error updating summary stats", e);
        }
    }

    /**
     * Update last refresh timestamp
     */
    private void updateLastRefresh() {
        lastRefreshLabel.setText("Last refresh: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    @FXML
    private void handleFilterChange() {
        currentPage = 0;
        loadGrades();
    }

    @FXML
    private void handleClearFilters() {
        academicYearComboBox.setValue(LocalDate.now().getYear());
        termComboBox.setValue(null);
        courseComboBox.setValue(null);
        teacherComboBox.setValue(null);
        studentSearchField.clear();
        minGradeComboBox.setValue(null);
        maxGradeComboBox.setValue(null);
        showFailingCheckBox.setSelected(false);
        showIncompleteCheckBox.setSelected(false);
        showTransferCheckBox.setSelected(false);

        handleFilterChange();
    }

    @FXML
    private void handleRefresh() {
        loadData();
        showInfo("Refreshed", "Data refreshed successfully");
    }

    @FXML
    private void handleAddTransferGrade() {
        showTransferGradeDialog();
    }

    @FXML
    private void handleExportGrades() {
        showInfo("Export", "Export functionality will be implemented in Phase 3");
    }

    @FXML
    private void handleBulkEdit() {
        showInfo("Bulk Edit", "Bulk edit functionality will be implemented in Phase 3");
    }

    @FXML
    private void handleFirstPage() {
        currentPage = 0;
        loadGrades();
    }

    @FXML
    private void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadGrades();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadGrades();
        }
    }

    @FXML
    private void handleLastPage() {
        currentPage = totalPages - 1;
        loadGrades();
    }

    @FXML
    private void handlePageSizeChange() {
        pageSize = Integer.parseInt(pageSizeComboBox.getValue());
        currentPage = 0;
        loadGrades();
    }

    @FXML
    private void handleAuditLogRefresh() {
        loadAuditLogs();
    }

    @FXML
    private void handleViewFullAuditLog() {
        showInfo("Audit Log", "Full audit log viewer will be implemented in Phase 3");
    }

    // ========================================================================
    // DIALOG METHODS
    // ========================================================================

    /**
     * Show edit grade dialog
     */
    private void handleEditGrade(StudentGrade grade) {
        Dialog<StudentGrade> dialog = new Dialog<>();
        dialog.setTitle("Edit Grade");
        dialog.setHeaderText("Edit grade for " + grade.getStudentName() + " in " + grade.getCourseName());

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField letterGradeField = new TextField(grade.getLetterGrade());
        TextField numericalGradeField = new TextField(grade.getNumericalGrade() != null ?
            grade.getNumericalGrade().toString() : "");
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Reason for change (required)");
        reasonArea.setPrefRowCount(3);

        grid.add(new Label("Letter Grade:"), 0, 0);
        grid.add(letterGradeField, 1, 0);
        grid.add(new Label("Numerical Grade:"), 0, 1);
        grid.add(numericalGradeField, 1, 1);
        grid.add(new Label("Reason:"), 0, 2);
        grid.add(reasonArea, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                if (reasonArea.getText() == null || reasonArea.getText().trim().isEmpty()) {
                    showError("Validation Error", "Reason for change is required");
                    return null;
                }

                // Save changes with audit
                saveGradeWithAudit(grade, letterGradeField.getText(),
                    numericalGradeField.getText(), reasonArea.getText());
                return grade;
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Save grade changes with audit trail
     */
    private void saveGradeWithAudit(StudentGrade grade, String newLetterGrade,
                                    String newNumericalGrade, String reason) {
        try {
            // Create audit logs for each change
            if (!newLetterGrade.equals(grade.getLetterGrade())) {
                GradeAuditLog audit = GradeAuditLog.builder()
                    .studentGrade(grade)
                    .editedByAdmin(currentAdmin)
                    .fieldName("letterGrade")
                    .oldValue(grade.getLetterGrade())
                    .newValue(newLetterGrade)
                    .reason(reason)
                    .editType(GradeAuditLog.EditType.MANUAL)
                    .build();
                gradeAuditLogRepository.save(audit);
            }

            if (newNumericalGrade != null && !newNumericalGrade.isEmpty()) {
                double newNumGrade = Double.parseDouble(newNumericalGrade);
                if (grade.getNumericalGrade() == null ||
                    !grade.getNumericalGrade().equals(newNumGrade)) {
                    GradeAuditLog audit = GradeAuditLog.builder()
                        .studentGrade(grade)
                        .editedByAdmin(currentAdmin)
                        .fieldName("numericalGrade")
                        .oldValue(grade.getNumericalGrade() != null ?
                            grade.getNumericalGrade().toString() : "null")
                        .newValue(String.valueOf(newNumGrade))
                        .reason(reason)
                        .editType(GradeAuditLog.EditType.MANUAL)
                        .build();
                    gradeAuditLogRepository.save(audit);
                }
                grade.setNumericalGrade(newNumGrade);
            }

            // Update grade
            grade.setLetterGrade(newLetterGrade);
            grade.setGpaPoints(StudentGrade.letterGradeToGpaPoints(newLetterGrade));
            studentGradeRepository.save(grade);

            loadData();
            showInfo("Success", "Grade updated successfully");

            logger.info("Grade updated for {} by {}: {} -> {}",
                grade.getStudentName(), currentAdmin.getUsername(),
                grade.getLetterGrade(), newLetterGrade);

        } catch (Exception e) {
            logger.error("Error saving grade", e);
            showError("Error", "Failed to save grade: " + e.getMessage());
        }
    }

    /**
     * Show grade history dialog
     */
    private void handleViewGradeHistory(StudentGrade grade) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Grade History");
        dialog.setHeaderText("Grade history for " + grade.getStudentName() + " in " + grade.getCourseName());

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<GradeAuditLog> historyTable = new TableView<>();

        TableColumn<GradeAuditLog, String> timestampCol = new TableColumn<>("Date/Time");
        timestampCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTimestamp().format(DATE_TIME_FORMATTER)));

        TableColumn<GradeAuditLog, String> adminCol = new TableColumn<>("Admin");
        adminCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getAdminName()));

        TableColumn<GradeAuditLog, String> fieldCol = new TableColumn<>("Field");
        fieldCol.setCellValueFactory(new PropertyValueFactory<>("fieldName"));

        TableColumn<GradeAuditLog, String> oldCol = new TableColumn<>("Old Value");
        oldCol.setCellValueFactory(new PropertyValueFactory<>("oldValue"));

        TableColumn<GradeAuditLog, String> newCol = new TableColumn<>("New Value");
        newCol.setCellValueFactory(new PropertyValueFactory<>("newValue"));

        TableColumn<GradeAuditLog, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));

        historyTable.getColumns().addAll(timestampCol, adminCol, fieldCol, oldCol, newCol, reasonCol);

        List<GradeAuditLog> history = gradeAuditLogRepository.findByStudentGradeOrderByTimestampDesc(grade);
        historyTable.setItems(FXCollections.observableArrayList(history));

        historyTable.setPrefHeight(400);
        historyTable.setPrefWidth(900);

        dialog.getDialogPane().setContent(historyTable);
        dialog.showAndWait();
    }

    /**
     * Show transfer grade dialog
     */
    private void showTransferGradeDialog() {
        Dialog<StudentGrade> dialog = new Dialog<>();
        dialog.setTitle("Add Transfer Student Grade");
        dialog.setHeaderText("Enter grade for transfer student");

        ButtonType addButton = new ButtonType("Add Grade", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Student> studentCombo = new ComboBox<>();
        studentCombo.setItems(FXCollections.observableArrayList(studentRepository.findAll()));
        studentCombo.setConverter(new javafx.util.StringConverter<Student>() {
            @Override
            public String toString(Student student) {
                return student != null ? student.getFullName() + " (" + student.getStudentId() + ")" : "";
            }
            @Override
            public Student fromString(String string) {
                return null;
            }
        });

        ComboBox<Course> courseCombo = new ComboBox<>();
        courseCombo.setItems(FXCollections.observableArrayList(courseRepository.findAll()));
        courseCombo.setConverter(new javafx.util.StringConverter<Course>() {
            @Override
            public String toString(Course course) {
                return course != null ? course.getCourseCode() + " - " + course.getCourseName() : "";
            }
            @Override
            public Course fromString(String string) {
                return null;
            }
        });

        TextField termField = new TextField();
        TextField yearField = new TextField(String.valueOf(LocalDate.now().getYear()));
        TextField letterGradeField = new TextField();
        TextField numericalGradeField = new TextField();
        TextField creditsField = new TextField("1.0");
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Transfer information (school, district, date, etc.)");

        grid.add(new Label("Student:"), 0, 0);
        grid.add(studentCombo, 1, 0);
        grid.add(new Label("Course:"), 0, 1);
        grid.add(courseCombo, 1, 1);
        grid.add(new Label("Term:"), 0, 2);
        grid.add(termField, 1, 2);
        grid.add(new Label("Academic Year:"), 0, 3);
        grid.add(yearField, 1, 3);
        grid.add(new Label("Letter Grade:"), 0, 4);
        grid.add(letterGradeField, 1, 4);
        grid.add(new Label("Numerical Grade:"), 0, 5);
        grid.add(numericalGradeField, 1, 5);
        grid.add(new Label("Credits:"), 0, 6);
        grid.add(creditsField, 1, 6);
        grid.add(new Label("Transfer Notes:"), 0, 7);
        grid.add(notesArea, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                try {
                    StudentGrade newGrade = new StudentGrade();
                    newGrade.setStudent(studentCombo.getValue());
                    newGrade.setCourse(courseCombo.getValue());
                    newGrade.setTerm(termField.getText());
                    newGrade.setAcademicYear(Integer.parseInt(yearField.getText()));
                    newGrade.setLetterGrade(letterGradeField.getText());
                    newGrade.setNumericalGrade(Double.parseDouble(numericalGradeField.getText()));
                    newGrade.setGpaPoints(StudentGrade.letterGradeToGpaPoints(letterGradeField.getText()));
                    newGrade.setCredits(Double.parseDouble(creditsField.getText()));
                    newGrade.setGradeDate(LocalDate.now());
                    newGrade.setComments(notesArea.getText());
                    newGrade.setGradeType("Transfer");

                    // Save grade
                    newGrade = studentGradeRepository.save(newGrade);

                    // Create audit log
                    GradeAuditLog audit = GradeAuditLog.builder()
                        .studentGrade(newGrade)
                        .editedByAdmin(currentAdmin)
                        .fieldName("transfer_entry")
                        .oldValue(null)
                        .newValue(letterGradeField.getText())
                        .reason("Transfer student grade entry: " + notesArea.getText())
                        .editType(GradeAuditLog.EditType.TRANSFER)
                        .build();
                    gradeAuditLogRepository.save(audit);

                    loadData();
                    showInfo("Success", "Transfer grade added successfully");

                    return newGrade;
                } catch (Exception e) {
                    logger.error("Error adding transfer grade", e);
                    showError("Error", "Failed to add transfer grade: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Get current admin user (placeholder for authentication)
     */
    private User getCurrentAdmin() {
        // In production, get from Spring Security context
        List<User> admins = userRepository.findByRole("ADMIN");
        return admins.isEmpty() ? null : admins.get(0);
    }

    /**
     * Show information alert
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error alert
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
