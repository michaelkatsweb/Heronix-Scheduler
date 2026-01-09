package com.heronix.ui.components;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Editable TableCell for inline teacher assignment
 * Provides smart suggestions based on certifications and workload
 *
 * Features:
 * - Smart teacher suggestions (certified first, balanced workload)
 * - Visual workload indicators (🟢🟡🔴)
 * - Inline validation with warnings
 * - Instant save on selection
 * - Brief visual feedback on success
 *
 * @since Phase 3 - Smart Data Entry
 * @version 1.0.0
 */
@Slf4j
public class EditableTeacherCell extends TableCell<Course, Teacher> {

    private ComboBox<Teacher> comboBox;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    public EditableTeacherCell(ApplicationContext context) {
        this.teacherRepository = context.getBean(TeacherRepository.class);
        this.courseRepository = context.getBean(CourseRepository.class);
    }

    @Override
    public void startEdit() {
        super.startEdit();

        if (comboBox == null) {
            createComboBox();
        }

        Course course = getTableRow().getItem();
        if (course == null) {
            log.warn("Cannot start edit: course is null");
            return;
        }

        // Load smart suggestions
        List<Teacher> suggestedTeachers = getSmartTeacherSuggestions(course);
        comboBox.setItems(FXCollections.observableArrayList(suggestedTeachers));
        comboBox.getSelectionModel().select(getItem());

        setText(null);
        setGraphic(comboBox);
        comboBox.show();
        comboBox.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getDisplayText());
        setGraphic(null);
        setStyle("-fx-cursor: hand;");
    }

    @Override
    public void updateItem(Teacher teacher, boolean empty) {
        super.updateItem(teacher, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
            setTooltip(null);
            setStyle("");
        } else {
            if (isEditing()) {
                setText(null);
                setGraphic(comboBox);
            } else {
                setText(getDisplayText());
                setGraphic(null);

                // Add visual cue that cell is editable
                setStyle("-fx-cursor: hand; -fx-underline: false;");

                // Add helpful tooltip
                Course course = getTableRow().getItem();
                if (course != null) {
                    String tooltipText = teacher == null
                        ? "Double-click to assign teacher\nCertified teachers will be suggested first"
                        : String.format("Current: %s (%d courses)\nDouble-click to change",
                            teacher.getName(),
                            teacher.getCourses() != null ? teacher.getCourses().size() : 0);
                    setTooltip(new Tooltip(tooltipText));
                }
            }
        }
    }

    private void createComboBox() {
        comboBox = new ComboBox<>();
        comboBox.setEditable(true); // Enable search/filter
        comboBox.setPrefWidth(250);
        comboBox.setPromptText("Type to search or select...");

        // Custom display format with workload indicator
        comboBox.setConverter(new StringConverter<Teacher>() {
            @Override
            public String toString(Teacher teacher) {
                if (teacher == null) return "";
                int courseCount = teacher.getCourses() != null ? teacher.getCourses().size() : 0;
                String indicator = getWorkloadIndicator(courseCount);
                String department = teacher.getDepartment() != null ? teacher.getDepartment() : "N/A";
                return String.format("%s %s (%s) - %d courses",
                    indicator, teacher.getName(), department, courseCount);
            }

            @Override
            public Teacher fromString(String string) {
                return null;
            }
        });

        // Custom cell factory for dropdown items
        comboBox.setCellFactory(lv -> new ListCell<Teacher>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) {
                    setText(null);
                    setStyle("");
                } else {
                    int courseCount = teacher.getCourses() != null ? teacher.getCourses().size() : 0;
                    String indicator = getWorkloadIndicator(courseCount);
                    String department = teacher.getDepartment() != null ? teacher.getDepartment() : "N/A";

                    setText(String.format("%s %s (%s) - %d courses",
                        indicator, teacher.getName(), department, courseCount));

                    // Color-code by workload
                    String style = switch (indicator) {
                        case "🟢" -> "-fx-background-color: rgba(76, 175, 80, 0.2);";
                        case "🟡" -> "-fx-background-color: rgba(255, 193, 7, 0.2);";
                        case "🔴" -> "-fx-background-color: rgba(244, 67, 54, 0.2);";
                        default -> "";
                    };
                    setStyle(style);
                }
            }
        });

        // Handle selection
        comboBox.setOnAction(event -> {
            Teacher selected = comboBox.getValue();
            if (selected != null) {
                Course course = getTableRow().getItem();
                if (course == null) {
                    log.warn("Cannot save: course is null");
                    cancelEdit();
                    return;
                }

                log.info("Teacher selected for course {}: {}", course.getCourseCode(), selected.getName());

                // Validate before saving
                ValidationResult validation = validateAssignment(course, selected);
                if (validation.isValid()) {
                    // Save immediately
                    if (saveAssignment(course, selected)) {
                        commitEdit(selected);
                        showSuccessFeedback();
                    } else {
                        cancelEdit();
                    }
                } else {
                    // Show warning and allow override
                    boolean proceed = showWarningDialog(course, selected, validation);
                    if (proceed) {
                        if (saveAssignment(course, selected)) {
                            commitEdit(selected);
                            showSuccessFeedback();
                        } else {
                            cancelEdit();
                        }
                    } else {
                        cancelEdit();
                    }
                }
            }
        });

        // Handle ESC key to cancel
        comboBox.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

        // Handle focus loss (optional: auto-cancel on focus loss)
        comboBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && isEditing()) {
                // Commented out to allow clicking warning dialogs
                // cancelEdit();
            }
        });
    }

    private String getDisplayText() {
        if (getItem() == null) {
            return "[Click to assign]";
        }
        int courseCount = getItem().getCourses() != null ? getItem().getCourses().size() : 0;
        String indicator = getWorkloadIndicator(courseCount);
        return String.format("%s %s (%d)", indicator, getItem().getName(), courseCount);
    }

    /**
     * Get smart teacher suggestions sorted by:
     * 1. Certification match (certified teachers first)
     * 2. Workload (lower load preferred)
     * 3. Alphabetical order
     */
    private List<Teacher> getSmartTeacherSuggestions(Course course) {
        try {
            List<Teacher> allTeachers = teacherRepository.findAllWithCourses();

            return allTeachers.stream()
                .filter(t -> t.getActive()) // Only active teachers
                .sorted(Comparator
                    // 1. Certified teachers first
                    .comparing((Teacher t) -> !isCertified(t, course))
                    // 2. Not overloaded (< 6 courses)
                    .thenComparing(t -> (t.getCourses() != null ? t.getCourses().size() : 0) >= 6)
                    // 3. Lower workload preferred
                    .thenComparing(t -> t.getCourses() != null ? t.getCourses().size() : 0)
                    // 4. Alphabetical
                    .thenComparing(Teacher::getName))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error loading smart teacher suggestions", e);
            return List.of();
        }
    }

    /**
     * Check if teacher is certified for the course subject
     */
    private boolean isCertified(Teacher teacher, Course course) {
        if (teacher.getCertifiedSubjects() == null ||
            teacher.getCertifiedSubjects().isEmpty() ||
            course.getSubject() == null) {
            return false;
        }

        String subject = course.getSubject().toLowerCase();
        return teacher.getCertifiedSubjects().stream()
            .anyMatch(cert -> {
                String certLower = cert.toLowerCase();
                // Check for exact match or contains
                return certLower.equals(subject) ||
                       certLower.contains(subject) ||
                       subject.contains(certLower) ||
                       isInSameSubjectFamily(certLower, subject);
            });
    }

    /**
     * Check if two subjects are in the same family
     */
    private boolean isInSameSubjectFamily(String cert, String subject) {
        // Science family
        if ((cert.contains("science") || cert.contains("biology") || cert.contains("chemistry") || cert.contains("physics")) &&
            (subject.contains("science") || subject.contains("biology") || subject.contains("chemistry") || subject.contains("physics"))) {
            return true;
        }

        // Math family
        if ((cert.contains("math") || cert.contains("algebra") || cert.contains("geometry") || cert.contains("calculus")) &&
            (subject.contains("math") || subject.contains("algebra") || subject.contains("geometry") || subject.contains("calculus"))) {
            return true;
        }

        // English family
        if ((cert.contains("english") || cert.contains("literature") || cert.contains("language arts")) &&
            (subject.contains("english") || subject.contains("literature") || subject.contains("language arts"))) {
            return true;
        }

        return false;
    }

    private String getWorkloadIndicator(int courseCount) {
        if (courseCount == 0) return "⚪";
        if (courseCount <= 5) return "🟢";
        if (courseCount <= 7) return "🟡";
        return "🔴";
    }

    /**
     * Validate teacher assignment
     */
    private ValidationResult validateAssignment(Course course, Teacher teacher) {
        ValidationResult result = new ValidationResult();

        // Check certification
        if (!isCertified(teacher, course)) {
            result.addWarning("Certification Mismatch",
                teacher.getName() + " is not certified to teach " + course.getSubject());
        }

        // Check workload
        int currentLoad = teacher.getCourses() != null ? teacher.getCourses().size() : 0;
        if (currentLoad >= 6) {
            result.addWarning("High Workload",
                teacher.getName() + " already has " + currentLoad + " courses assigned (limit: 6)");
        }

        return result;
    }

    private boolean showWarningDialog(Course course, Teacher teacher, ValidationResult validation) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Assignment Warning");
        alert.setHeaderText("Potential issues detected");

        StringBuilder content = new StringBuilder();
        for (String warning : validation.getWarnings()) {
            content.append("⚠️ ").append(warning).append("\n\n");
        }
        content.append("Do you want to proceed anyway?");

        alert.setContentText(content.toString());

        ButtonType proceedButton = new ButtonType("Proceed Anyway");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(proceedButton, cancelButton);

        return alert.showAndWait().orElse(cancelButton) == proceedButton;
    }

    /**
     * Save assignment to database
     */
    private boolean saveAssignment(Course course, Teacher teacher) {
        try {
            course.setTeacher(teacher);
            courseRepository.save(course);

            log.info("Successfully assigned teacher {} to course {}",
                teacher.getName(), course.getCourseCode());

            return true;
        } catch (Exception e) {
            log.error("Error saving teacher assignment", e);
            showErrorDialog("Error Saving Assignment",
                "Could not save assignment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Show brief success feedback (green checkmark for 1 second)
     */
    private void showSuccessFeedback() {
        Platform.runLater(() -> {
            String originalText = getText();
            String originalStyle = getStyle();

            setText(originalText + " ✓");
            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                setText(originalText);
                setStyle(originalStyle);
            });
            pause.play();
        });
    }

    private void showErrorDialog(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Validation result holder
     */
    private static class ValidationResult {
        private final List<String> warnings = new java.util.ArrayList<>();

        public void addWarning(String title, String message) {
            warnings.add(title + ": " + message);
        }

        public boolean isValid() {
            return warnings.isEmpty();
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
