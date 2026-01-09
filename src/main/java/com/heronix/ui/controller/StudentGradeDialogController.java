package com.heronix.ui.controller;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.GradeService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Student Grade Dialog Controller
 * Location: src/main/java/com/eduscheduler/ui/controller/StudentGradeDialogController.java
 *
 * Dialog for entering or editing student grades.
 * Supports letter grades, numerical grades, and automatic GPA calculation.
 *
 * Features:
 * - Grade entry (letter and numerical)
 * - Course and teacher selection
 * - Term/academic year tracking
 * - Weighted course flag
 * - Grade type selection
 * - Comments and attendance
 * - Auto-GPA calculation on save
 * - Responsive design (fits all screen sizes)
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - Added responsive design support
 * @since 2025-11-20
 */
@Slf4j
@Controller
public class StudentGradeDialogController extends BaseDialogController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    // Student and Grade
    private Student student;
    private StudentGrade grade;
    private boolean isNewGrade = true;

    // Dialog result
    private boolean saved = false;

    // FXML Controls
    @FXML private Label studentNameLabel;
    @FXML private ComboBox<Course> courseComboBox;
    @FXML private ComboBox<String> letterGradeComboBox;
    @FXML private TextField numericalGradeField;
    @FXML private TextField creditsField;
    @FXML private ComboBox<String> termComboBox;
    @FXML private TextField academicYearField;
    @FXML private ComboBox<String> gradeTypeComboBox;
    @FXML private CheckBox isWeightedCheckBox;
    @FXML private CheckBox isFinalCheckBox;
    @FXML private DatePicker gradeDatePicker;
    @FXML private ComboBox<Teacher> teacherComboBox;
    @FXML private TextField absencesField;
    @FXML private TextField tardiesField;
    @FXML private TextArea commentsArea;
    @FXML private Label gpaPointsLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // Letter grades
    private static final List<String> LETTER_GRADES = Arrays.asList(
        "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-",
        "D+", "D", "D-", "F", "I", "W", "P"
    );

    // Grade types
    private static final List<String> GRADE_TYPES = Arrays.asList(
        "Final", "Midterm", "Quarter 1", "Quarter 2", "Quarter 3", "Quarter 4",
        "Progress Report", "Interim"
    );

    // Terms
    private static final List<String> TERMS = Arrays.asList(
        "Fall 2024", "Spring 2025", "Summer 2025",
        "Q1 2024-25", "Q2 2024-25", "Q3 2024-25", "Q4 2024-25"
    );

    /**
     * Initialize the dialog
     */
    @FXML
    public void initialize() {
        log.debug("Initializing StudentGradeDialogController");

        // Setup letter grade combo box
        letterGradeComboBox.getItems().addAll(LETTER_GRADES);
        letterGradeComboBox.setOnAction(e -> updateGPAPoints());

        // Setup term combo box
        termComboBox.getItems().addAll(TERMS);
        termComboBox.setValue(getCurrentTerm());

        // Setup grade type combo box
        gradeTypeComboBox.getItems().addAll(GRADE_TYPES);
        gradeTypeComboBox.setValue("Final");

        // Setup academic year
        academicYearField.setText(String.valueOf(LocalDate.now().getYear()));

        // Setup date picker
        gradeDatePicker.setValue(LocalDate.now());

        // Setup credits
        creditsField.setText("1.0");

        // Setup numerical grade field listener
        numericalGradeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                try {
                    double numerical = Double.parseDouble(newVal);
                    if (numerical >= 0 && numerical <= 100) {
                        String letterGrade = StudentGrade.numericalGradeToLetterGrade(numerical);
                        letterGradeComboBox.setValue(letterGrade);
                    }
                } catch (NumberFormatException e) {
                    // Invalid number, ignore
                }
            }
        });

        // Setup weighted checkbox listener
        isWeightedCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateGPAPoints();
        });

        // Load courses
        loadCourses();

        // Load teachers
        loadTeachers();
    }

    /**
     * Set the student for this grade
     */
    public void setStudent(Student student) {
        this.student = student;
        if (studentNameLabel != null) {
            studentNameLabel.setText("Student: " + student.getFullName());
        }
        log.debug("Student set: {}", student.getFullName());
    }

    /**
     * Set existing grade for editing
     */
    public void setGrade(StudentGrade grade) {
        this.grade = grade;
        this.isNewGrade = false;
        populateFields();
        log.debug("Editing existing grade: {}", grade.getId());
    }

    /**
     * Populate fields from existing grade
     */
    private void populateFields() {
        if (grade == null) return;

        courseComboBox.setValue(grade.getCourse());
        letterGradeComboBox.setValue(grade.getLetterGrade());

        if (grade.getNumericalGrade() != null) {
            numericalGradeField.setText(String.format("%.1f", grade.getNumericalGrade()));
        }

        creditsField.setText(String.format("%.1f", grade.getCredits()));
        termComboBox.setValue(grade.getTerm());
        academicYearField.setText(String.valueOf(grade.getAcademicYear()));
        gradeTypeComboBox.setValue(grade.getGradeType());

        if (grade.getIsWeighted() != null) {
            isWeightedCheckBox.setSelected(grade.getIsWeighted());
        }

        if (grade.getIsFinal() != null) {
            isFinalCheckBox.setSelected(grade.getIsFinal());
        }

        gradeDatePicker.setValue(grade.getGradeDate());
        teacherComboBox.setValue(grade.getTeacher());

        if (grade.getAbsences() != null) {
            absencesField.setText(String.valueOf(grade.getAbsences()));
        }

        if (grade.getTardies() != null) {
            tardiesField.setText(String.valueOf(grade.getTardies()));
        }

        if (grade.getComments() != null) {
            commentsArea.setText(grade.getComments());
        }

        updateGPAPoints();
    }

    /**
     * Load available courses
     */
    private void loadCourses() {
        List<Course> courses = courseRepository.findAll();
        courseComboBox.getItems().clear();
        courseComboBox.getItems().addAll(courses);

        // Custom cell factory for display
        courseComboBox.setCellFactory(param -> new ListCell<Course>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s", item.getCourseCode(), item.getCourseName()));
                }
            }
        });

        courseComboBox.setButtonCell(new ListCell<Course>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s", item.getCourseCode(), item.getCourseName()));
                }
            }
        });

        log.debug("Loaded {} courses", courses.size());
    }

    /**
     * Load available teachers
     */
    private void loadTeachers() {
        List<Teacher> teachers = teacherRepository.findAllActive();
        teacherComboBox.getItems().clear();
        teacherComboBox.getItems().addAll(teachers);

        // Custom cell factory for display
        teacherComboBox.setCellFactory(param -> new ListCell<Teacher>() {
            @Override
            protected void updateItem(Teacher item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFirstName() + " " + item.getLastName());
                }
            }
        });

        teacherComboBox.setButtonCell(new ListCell<Teacher>() {
            @Override
            protected void updateItem(Teacher item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFirstName() + " " + item.getLastName());
                }
            }
        });

        log.debug("Loaded {} teachers", teachers.size());
    }

    /**
     * Update GPA points display
     */
    private void updateGPAPoints() {
        String letterGrade = letterGradeComboBox.getValue();
        if (letterGrade != null) {
            Double gpaPoints = StudentGrade.letterGradeToGpaPoints(letterGrade);
            if (gpaPoints != null) {
                double displayPoints = gpaPoints;
                if (isWeightedCheckBox.isSelected()) {
                    displayPoints = Math.min(gpaPoints + 1.0, 5.0);
                }
                gpaPointsLabel.setText(String.format("GPA Points: %.2f", displayPoints));
            } else {
                gpaPointsLabel.setText("GPA Points: N/A (Pass/Fail)");
            }
        }
    }

    /**
     * Get current term (Fall/Spring based on current date)
     */
    private String getCurrentTerm() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        if (month >= 8 && month <= 12) {
            return "Fall " + year;
        } else if (month >= 1 && month <= 5) {
            return "Spring " + year;
        } else {
            return "Summer " + year;
        }
    }

    /**
     * Handle Save button
     */
    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            // Create or update grade
            if (isNewGrade) {
                grade = new StudentGrade();
                grade.setStudent(student);
            }

            // Set values from form
            grade.setCourse(courseComboBox.getValue());
            grade.setLetterGrade(letterGradeComboBox.getValue());

            String numericalText = numericalGradeField.getText();
            if (!numericalText.isEmpty()) {
                grade.setNumericalGrade(Double.parseDouble(numericalText));
            }

            grade.setCredits(Double.parseDouble(creditsField.getText()));
            grade.setTerm(termComboBox.getValue());
            grade.setAcademicYear(Integer.parseInt(academicYearField.getText()));
            grade.setGradeType(gradeTypeComboBox.getValue());
            grade.setIsWeighted(isWeightedCheckBox.isSelected());
            grade.setIsFinal(isFinalCheckBox.isSelected());
            grade.setGradeDate(gradeDatePicker.getValue());
            grade.setTeacher(teacherComboBox.getValue());

            String absencesText = absencesField.getText();
            if (!absencesText.isEmpty()) {
                grade.setAbsences(Integer.parseInt(absencesText));
            }

            String tardiesText = tardiesField.getText();
            if (!tardiesText.isEmpty()) {
                grade.setTardies(Integer.parseInt(tardiesText));
            }

            grade.setComments(commentsArea.getText());

            // Calculate GPA points
            Double gpaPoints = StudentGrade.letterGradeToGpaPoints(grade.getLetterGrade());
            if (gpaPoints != null) {
                grade.setGpaPoints(gpaPoints);
            } else {
                grade.setGpaPoints(0.0);
                grade.setIncludeInGPA(false); // Pass/Fail doesn't count in GPA
            }

            // Save grade (GPA will be recalculated automatically)
            gradeService.saveGrade(grade);

            log.info("Grade saved successfully: {} - {} in {}",
                student.getFullName(), grade.getLetterGrade(), grade.getCourseName());

            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Grade Saved");
            alert.setHeaderText("Grade Saved Successfully");
            alert.setContentText(String.format(
                "Grade %s saved for %s in %s\nStudent's GPA updated to: %.2f",
                grade.getLetterGrade(),
                student.getFullName(),
                grade.getCourseName(),
                student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0
            ));
            alert.showAndWait();

            saved = true;
            closeDialog();

        } catch (Exception e) {
            log.error("Error saving grade", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to Save Grade");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Handle Cancel button
     */
    @FXML
    @Override
    protected void handleCancel() {
        saved = false;
        closeDialog();
    }

    /**
     * Validate input fields
     */
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (courseComboBox.getValue() == null) {
            errors.append("• Course is required\n");
        }

        if (letterGradeComboBox.getValue() == null || letterGradeComboBox.getValue().isEmpty()) {
            errors.append("• Letter grade is required\n");
        }

        String creditsText = creditsField.getText();
        if (creditsText.isEmpty()) {
            errors.append("• Credits is required\n");
        } else {
            try {
                double credits = Double.parseDouble(creditsText);
                if (credits <= 0) {
                    errors.append("• Credits must be greater than 0\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Credits must be a valid number\n");
            }
        }

        if (termComboBox.getValue() == null || termComboBox.getValue().isEmpty()) {
            errors.append("• Term is required\n");
        }

        String yearText = academicYearField.getText();
        if (yearText.isEmpty()) {
            errors.append("• Academic year is required\n");
        } else {
            try {
                int year = Integer.parseInt(yearText);
                if (year < 2000 || year > 2100) {
                    errors.append("• Academic year must be between 2000 and 2100\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Academic year must be a valid number\n");
            }
        }

        String numericalText = numericalGradeField.getText();
        if (!numericalText.isEmpty()) {
            try {
                double numerical = Double.parseDouble(numericalText);
                if (numerical < 0 || numerical > 100) {
                    errors.append("• Numerical grade must be between 0 and 100\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Numerical grade must be a valid number\n");
            }
        }

        if (gradeDatePicker.getValue() == null) {
            errors.append("• Grade date is required\n");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Please correct the following errors:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    /**
     * Close the dialog
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            // Fallback if dialogStage not set (backward compatibility)
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Configure responsive dialog size
     */
    @Override
    protected void configureResponsiveSize() {
        // StudentGradeDialog is medium complexity - use default size (50% x 60%)
        super.configureResponsiveSize();
    }

    /**
     * Check if grade was saved
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * Get the saved grade
     */
    public StudentGrade getGrade() {
        return grade;
    }
}
