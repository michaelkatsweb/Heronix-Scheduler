package com.heronix.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSequence;
import com.heronix.model.domain.CourseSequenceStep;
import com.heronix.model.domain.SubjectArea;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.SubjectAreaRepository;
import com.heronix.service.CourseSequenceService;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Course Sequence Editor Controller
 *
 * Manages the Course Sequence Editor UI for creating and editing
 * academic pathways/progressions.
 *
 * Location: src/main/java/com/eduscheduler/controller/CourseSequenceEditorController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 6, 2025
 */
@Slf4j
@Controller
public class CourseSequenceEditorController {

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private CourseSequenceService courseSequenceService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectAreaRepository subjectAreaRepository;

    // ========================================================================
    // FXML COMPONENTS - TOP SECTION
    // ========================================================================

    @FXML
    private ComboBox<CourseSequence> sequenceSelector;

    @FXML
    private Label sequenceTypeLabel;

    @FXML
    private Label subjectAreaLabel;

    @FXML
    private Label totalStepsLabel;

    @FXML
    private Label totalCreditsLabel;

    @FXML
    private Label minGPALabel;

    @FXML
    private Label enrolledStudentsLabel;

    // ========================================================================
    // FXML COMPONENTS - STEPS TABLE
    // ========================================================================

    @FXML
    private TableView<CourseSequenceStep> stepsTable;

    @FXML
    private TableColumn<CourseSequenceStep, Integer> orderColumn;

    @FXML
    private TableColumn<CourseSequenceStep, String> courseCodeColumn;

    @FXML
    private TableColumn<CourseSequenceStep, String> courseNameColumn;

    @FXML
    private TableColumn<CourseSequenceStep, Integer> gradeColumn;

    @FXML
    private TableColumn<CourseSequenceStep, String> semesterColumn;

    @FXML
    private TableColumn<CourseSequenceStep, Double> creditsColumn;

    @FXML
    private TableColumn<CourseSequenceStep, String> requiredColumn;

    @FXML
    private Label statusLabel;

    // ========================================================================
    // FXML COMPONENTS - DETAILS PANEL
    // ========================================================================

    @FXML
    private Label detailNameLabel;

    @FXML
    private Label detailCodeLabel;

    @FXML
    private Label detailTypeLabel;

    @FXML
    private Label detailSubjectLabel;

    @FXML
    private Label detailMinGPALabel;

    @FXML
    private Label detailRecGPALabel;

    @FXML
    private Label detailPrereqLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label detailStepsLabel;

    @FXML
    private Label detailCreditsLabel;

    @FXML
    private Label detailCompletionLabel;

    @FXML
    private Label detailEnrolledLabel;

    @FXML
    private Label lastSavedLabel;

    // ========================================================================
    // STATE
    // ========================================================================

    private ObservableList<CourseSequence> allSequences = FXCollections.observableArrayList();
    private ObservableList<CourseSequenceStep> currentSteps = FXCollections.observableArrayList();
    private CourseSequence currentSequence;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing CourseSequenceEditorController");

        setupTableColumns();
        setupSequenceSelector();
        loadData();

        // Selection listener for table
        stepsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                log.debug("Step selected: {}", newVal.getStepOrder());
            }
        });

        updateStatusLabel("Ready");
    }

    /**
     * Setup table columns with custom cell factories
     */
    private void setupTableColumns() {
        // Order column
        orderColumn.setCellValueFactory(new PropertyValueFactory<>("stepOrder"));

        // Course code column
        courseCodeColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new SimpleStringProperty(course != null ? course.getCourseCode() : "Unknown");
        });

        // Course name column
        courseNameColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new SimpleStringProperty(course != null ? course.getCourseName() : "Unknown");
        });

        // Grade column
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("recommendedGradeLevel"));

        // Semester column (placeholder - not in entity)
        semesterColumn.setCellValueFactory(cellData -> new SimpleStringProperty("Fall/Spring"));

        // Credits column
        creditsColumn.setCellValueFactory(new PropertyValueFactory<>("credits"));

        // Required column
        requiredColumn.setCellValueFactory(cellData -> {
            Boolean required = cellData.getValue().getIsRequired();
            return new SimpleStringProperty(Boolean.TRUE.equals(required) ? "Yes" : "No");
        });

        // Bind data
        stepsTable.setItems(currentSteps);
    }

    /**
     * Setup sequence selector ComboBox
     */
    private void setupSequenceSelector() {
        sequenceSelector.setItems(allSequences);

        // Custom cell factory for dropdown display
        sequenceSelector.setCellFactory(param -> new ListCell<CourseSequence>() {
            @Override
            protected void updateItem(CourseSequence item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getCode() + ")");
                }
            }
        });

        // Custom button cell for selected display
        sequenceSelector.setButtonCell(new ListCell<CourseSequence>() {
            @Override
            protected void updateItem(CourseSequence item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getCode() + ")");
                }
            }
        });
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load all sequences from database
     */
    private void loadData() {
        try {
            log.info("Loading course sequences");

            List<CourseSequence> sequences = courseSequenceService.getActiveSequences();
            allSequences.clear();
            allSequences.addAll(sequences);

            updateStatusLabel("Loaded " + sequences.size() + " sequences");
            log.info("Loaded {} sequences", sequences.size());

        } catch (Exception e) {
            log.error("Error loading sequences", e);
            CopyableErrorDialog.showError("Load Error", "Failed to load sequences: " + e.getMessage());
            updateStatusLabel("Error loading data");
        }
    }

    /**
     * Load steps for selected sequence
     */
    private void loadSequenceSteps(CourseSequence sequence) {
        try {
            log.info("Loading steps for sequence: {}", sequence.getId());

            List<CourseSequenceStep> steps = courseSequenceService.getStepsForSequence(sequence.getId());
            currentSteps.clear();
            currentSteps.addAll(steps);

            log.info("Loaded {} steps", steps.size());

        } catch (Exception e) {
            log.error("Error loading steps", e);
            CopyableErrorDialog.showError("Load Error", "Failed to load sequence steps: " + e.getMessage());
        }
    }

    /**
     * Load sequence details into details panel
     */
    private void loadSequenceDetails(CourseSequence sequence) {
        try {
            // Basic Info
            detailNameLabel.setText(sequence.getName() != null ? sequence.getName() : "-");
            detailCodeLabel.setText(sequence.getCode() != null ? sequence.getCode() : "-");
            detailTypeLabel.setText(sequence.getSequenceType() != null ?
                sequence.getSequenceType().getDescription() : "-");

            // Subject area
            SubjectArea subject = sequence.getSubjectArea();
            detailSubjectLabel.setText(subject != null ? subject.getName() : "-");

            // Requirements
            detailMinGPALabel.setText(sequence.getMinGPARecommended() != null ?
                String.format("%.2f", sequence.getMinGPARecommended()) : "N/A");
            detailRecGPALabel.setText(sequence.getMinGPARecommended() != null ?
                String.format("%.2f", sequence.getMinGPARecommended() + 0.5) : "N/A");
            detailPrereqLabel.setText("None"); // Placeholder

            // Description
            descriptionArea.setText(sequence.getDescription() != null ?
                sequence.getDescription() : "No description available");

            // Statistics
            detailStepsLabel.setText(String.valueOf(sequence.getCourseCount()));
            detailCreditsLabel.setText(String.format("%.1f",
                sequence.getTotalCredits() != null ? sequence.getTotalCredits() : 0.0));
            detailCompletionLabel.setText("85%"); // Placeholder
            detailEnrolledLabel.setText("0"); // Placeholder - requires student query

        } catch (Exception e) {
            log.error("Error loading sequence details", e);
        }
    }

    /**
     * Update info bar labels
     */
    private void updateInfoBar(CourseSequence sequence) {
        try {
            // Total steps
            totalStepsLabel.setText(String.valueOf(sequence.getCourseCount()));

            // Total credits
            totalCreditsLabel.setText(String.format("%.1f",
                sequence.getTotalCredits() != null ? sequence.getTotalCredits() : 0.0));

            // Min GPA
            minGPALabel.setText(sequence.getMinGPARecommended() != null ?
                String.format("%.2f", sequence.getMinGPARecommended()) : "N/A");

            // Enrolled students (placeholder)
            enrolledStudentsLabel.setText("0");

            // Type and subject
            sequenceTypeLabel.setText(sequence.getSequenceType() != null ?
                sequence.getSequenceType().getDescription() : "-");

            SubjectArea subject = sequence.getSubjectArea();
            subjectAreaLabel.setText(subject != null ? subject.getName() : "-");

        } catch (Exception e) {
            log.error("Error updating info bar", e);
        }
    }

    // ========================================================================
    // EVENT HANDLERS - TOP ACTIONS
    // ========================================================================

    @FXML
    public void handleNewSequence() {
        log.info("Action: New Sequence");

        try {
            // Create new sequence dialog
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Course Sequence");
            dialog.setHeaderText("Create New Course Sequence");
            dialog.setContentText("Sequence Name:");

            Optional<String> result = dialog.showAndWait();
            result.filter(s -> !s.trim().isEmpty())
                .ifPresent(rawName -> {
                String name = rawName.trim();

                //Create basic sequence
                CourseSequence newSequence = new CourseSequence();
                newSequence.setName(name);
                newSequence.setDescription("New sequence - " + name);
                newSequence.setActive(true);
                newSequence.setMinGPARecommended(2.0);

                // Save sequence
                CourseSequence saved = courseSequenceService.createSequence(newSequence);

                // Reload data and select new sequence
                loadData();
                sequenceSelector.setValue(saved);
                handleSequenceChange(saved);

                showInfo("Success", "Created new sequence: " + name);
                updateStatusLabel("Created: " + name);
            });
        } catch (Exception e) {
            log.error("Error creating sequence", e);
            CopyableErrorDialog.showError("Create Error", "Failed to create sequence: " + e.getMessage());
        }
    }

    @FXML
    public void handleImportSequence() {
        log.info("Action: Import Sequence");

        showInfo("Import from CSV", "To import sequences:\n\n" +
                "1. Prepare CSV with columns: name, description, subject, minGPA\n" +
                "2. Go to Data Management → Import → Course Sequences\n" +
                "3. Select your CSV file\n\n" +
                "Or use the main Import Wizard for bulk imports.");
    }

    @FXML
    public void handleRefresh() {
        log.info("Action: Refresh");
        loadData();

        if (currentSequence != null) {
            // Reload current sequence
            Optional<CourseSequence> refreshed = courseSequenceService.getSequenceById(currentSequence.getId());
            refreshed.ifPresent(this::handleSequenceChange);
        }
    }

    @FXML
    public void handleSequenceChange() {
        CourseSequence selected = sequenceSelector.getValue();
        if (selected != null) {
            handleSequenceChange(selected);
        }
    }

    private void handleSequenceChange(CourseSequence sequence) {
        log.info("Sequence changed: {}", sequence.getName());

        currentSequence = sequence;
        loadSequenceSteps(sequence);
        loadSequenceDetails(sequence);
        updateInfoBar(sequence);

        updateStatusLabel("Viewing: " + sequence.getName());
    }

    @FXML
    public void handleEditSequence() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence to edit");
            return;
        }

        log.info("Action: Edit Sequence");

        try {
            // Create edit dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Sequence");
            dialog.setHeaderText("Edit: " + currentSequence.getName());

            // Create form fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            TextField nameField = new TextField(currentSequence.getName());
            TextArea descField = new TextArea(currentSequence.getDescription());
            descField.setPrefRowCount(3);
            Double currentGPA = currentSequence.getMinGPARecommended() != null ? currentSequence.getMinGPARecommended() : 2.0;
            TextField minGPAField = new TextField(String.valueOf(currentGPA));

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Description:"), 0, 1);
            grid.add(descField, 1, 1);
            grid.add(new Label("Min GPA:"), 0, 2);
            grid.add(minGPAField, 1, 2);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                currentSequence.setName(nameField.getText().trim());
                currentSequence.setDescription(descField.getText().trim());
                try {
                    currentSequence.setMinGPARecommended(Double.parseDouble(minGPAField.getText().trim()));
                } catch (NumberFormatException e) {
                    currentSequence.setMinGPARecommended(2.0);
                }

                courseSequenceService.updateSequence(currentSequence.getId(), currentSequence);
                loadData();
                updateInfoBar(currentSequence);
                showInfo("Success", "Sequence updated successfully");
            }
        } catch (Exception e) {
            log.error("Error editing sequence", e);
            CopyableErrorDialog.showError("Edit Error", "Failed to edit sequence: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeleteSequence() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence to delete");
            return;
        }

        log.info("Action: Delete Sequence");

        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Sequence");
        confirm.setHeaderText("Delete " + currentSequence.getName() + "?");
        confirm.setContentText("This will deactivate the sequence (soft delete). Continue?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                courseSequenceService.deleteSequence(currentSequence.getId());
                showInfo("Success", "Sequence deleted successfully");

                // Clear selection and refresh
                currentSequence = null;
                sequenceSelector.setValue(null);
                currentSteps.clear();
                loadData();

            } catch (Exception e) {
                log.error("Error deleting sequence", e);
                CopyableErrorDialog.showError("Delete Error", "Failed to delete sequence: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleVisualize() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence to visualize");
            return;
        }

        log.info("Action: Visualize Pathway");

        // Generate text-based pathway view
        StringBuilder pathway = new StringBuilder();
        pathway.append("COURSE SEQUENCE PATHWAY\n");
        pathway.append("═══════════════════════════════════════\n\n");
        pathway.append("Sequence: ").append(currentSequence.getName()).append("\n");
        Double minGPA = currentSequence.getMinGPARecommended() != null ? currentSequence.getMinGPARecommended() : 2.0;
        pathway.append("Min GPA Required: ").append(minGPA).append("\n");
        pathway.append("Total Credits: ").append(currentSequence.getTotalCredits()).append("\n\n");
        pathway.append("PATHWAY:\n");
        pathway.append("═══════════════════════════════════════\n\n");

        for (CourseSequenceStep step : currentSteps) {
            pathway.append(String.format("Step %d: %s (%s)\n",
                    step.getStepOrder(),
                    step.getCourse().getCourseName(),
                    step.getCourse().getCourseCode()));
            pathway.append(String.format("   Grade: %d | Credits: %.1f\n",
                    step.getRecommendedGradeLevel() != null ? step.getRecommendedGradeLevel() : 9,
                    step.getCredits() != null ? step.getCredits() : 1.0));
            if (step.getNotes() != null && !step.getNotes().isEmpty()) {
                pathway.append("   Notes: " + step.getNotes() + "\n");
            }
            pathway.append("\n");
        }

        // Show in dialog
        TextArea textArea = new TextArea(pathway.toString());
        textArea.setEditable(false);
        textArea.setPrefSize(600, 400);
        textArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pathway Visualization");
        alert.setHeaderText(currentSequence.getName() + " - Course Pathway");
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    // ========================================================================
    // EVENT HANDLERS - STEP MANAGEMENT
    // ========================================================================

    @FXML
    public void handleAddCourse() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence first");
            return;
        }

        log.info("Action: Add Course to Sequence");

        try {
            // Get all active courses and create display strings
            List<Course> allCourses = courseRepository.findByActiveTrue();

            // Create map of course display string to course object
            java.util.Map<String, Course> courseMap = new java.util.HashMap<>();
            List<String> courseOptions = new ArrayList<>();
            for (Course c : allCourses) {
                String display = c.getCourseCode() + " - " + c.getCourseName();
                courseOptions.add(display);
                courseMap.put(display, c);
            }

            // Create choice dialog with course strings
            ChoiceDialog<String> dialog = new ChoiceDialog<>(null, courseOptions);
            dialog.setTitle("Add Course");
            dialog.setHeaderText("Add Course to Sequence: " + currentSequence.getName());
            dialog.setContentText("Select Course:");

            Optional<String> resultStr = dialog.showAndWait();
            Optional<Course> result = resultStr.map(courseMap::get);
            if (result.isPresent()) {
                Course course = result.get();

                // Create new step
                CourseSequenceStep step = new CourseSequenceStep();
                step.setCourseSequence(currentSequence);
                step.setCourse(course);
                step.setStepOrder(currentSteps.size() + 1);
                step.setRecommendedGradeLevel(9); // Default to 9th grade
                step.setIsRequired(true);
                step.setCredits(1.0);

                courseSequenceService.addStepToSequence(currentSequence.getId(), step);
                loadSequenceSteps(currentSequence);

                // Refresh sequence
                Optional<CourseSequence> refreshed = courseSequenceService.getSequenceById(currentSequence.getId());
                refreshed.ifPresent(seq -> {
                    currentSequence = seq;
                    updateInfoBar(seq);
                });

                showInfo("Success", "Added: " + course.getCourseName());
                updateStatusLabel("Added course");
            }
        } catch (Exception e) {
            log.error("Error adding course", e);
            CopyableErrorDialog.showError("Add Error", "Failed to add course: " + e.getMessage());
        }
    }

    @FXML
    public void handleMoveUp() {
        CourseSequenceStep selected = stepsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a step to move");
            return;
        }

        if (selected.getStepOrder() == 1) {
            showWarning("Cannot Move", "Step is already at the top");
            return;
        }

        try {
            log.info("Action: Move Step Up - {}", selected.getStepOrder());

            courseSequenceService.reorderStep(selected.getId(), selected.getStepOrder() - 1);
            loadSequenceSteps(currentSequence);
            updateStatusLabel("Step moved up");

        } catch (Exception e) {
            log.error("Error moving step", e);
            CopyableErrorDialog.showError("Move Error", "Failed to move step: " + e.getMessage());
        }
    }

    @FXML
    public void handleMoveDown() {
        CourseSequenceStep selected = stepsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a step to move");
            return;
        }

        int maxOrder = currentSteps.size();
        if (selected.getStepOrder() >= maxOrder) {
            showWarning("Cannot Move", "Step is already at the bottom");
            return;
        }

        try {
            log.info("Action: Move Step Down - {}", selected.getStepOrder());

            courseSequenceService.reorderStep(selected.getId(), selected.getStepOrder() + 1);
            loadSequenceSteps(currentSequence);
            updateStatusLabel("Step moved down");

        } catch (Exception e) {
            log.error("Error moving step", e);
            CopyableErrorDialog.showError("Move Error", "Failed to move step: " + e.getMessage());
        }
    }

    @FXML
    public void handleRemoveCourse() {
        CourseSequenceStep selected = stepsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a step to remove");
            return;
        }

        // Confirm removal
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Step");
        confirm.setHeaderText("Remove step " + selected.getStepOrder() + "?");
        confirm.setContentText("Course: " + selected.getCourse().getCourseName());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                log.info("Action: Remove Step - {}", selected.getStepOrder());

                courseSequenceService.deleteStep(selected.getId());
                loadSequenceSteps(currentSequence);

                // Refresh sequence to get updated totals
                Optional<CourseSequence> refreshed = courseSequenceService.getSequenceById(currentSequence.getId());
                refreshed.ifPresent(seq -> {
                    currentSequence = seq;
                    updateInfoBar(seq);
                    loadSequenceDetails(seq);
                });

                updateStatusLabel("Step removed");

            } catch (Exception e) {
                log.error("Error removing step", e);
                CopyableErrorDialog.showError("Remove Error", "Failed to remove step: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleSaveSequence() {
        if (currentSequence == null) {
            showWarning("No Selection", "No sequence selected to save");
            return;
        }

        try {
            log.info("Action: Save Sequence - {}", currentSequence.getId());

            // Steps are saved individually, so just update the last saved timestamp
            updateLastSavedLabel();
            showInfo("Success", "Sequence saved successfully");
            updateStatusLabel("Saved");

        } catch (Exception e) {
            log.error("Error saving sequence", e);
            CopyableErrorDialog.showError("Save Error", "Failed to save sequence: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportCSV() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence to export");
            return;
        }

        log.info("Action: Export CSV");

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Sequence to CSV");
            fileChooser.setInitialFileName(currentSequence.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".csv");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

            java.io.File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                // Create CSV content
                StringBuilder csv = new StringBuilder();
                csv.append("Step,Course Code,Course Name,Grade,Credits,Required,Notes\n");

                for (CourseSequenceStep step : currentSteps) {
                    csv.append(step.getStepOrder()).append(",");
                    csv.append("\"").append(step.getCourse().getCourseCode()).append("\",");
                    csv.append("\"").append(step.getCourse().getCourseName()).append("\",");
                    csv.append(step.getRecommendedGradeLevel() != null ? step.getRecommendedGradeLevel() : 9).append(",");
                    csv.append(step.getCredits() != null ? step.getCredits() : 1.0).append(",");
                    csv.append(step.getIsRequired() != null && step.getIsRequired() ? "Yes" : "No").append(",");
                    csv.append("\"").append(step.getNotes() != null ? step.getNotes() : "").append("\"\n");
                }

                // Write to file
                java.nio.file.Files.writeString(file.toPath(), csv.toString());

                showInfo("Success", "Exported sequence to:\n" + file.getAbsolutePath());
                updateStatusLabel("Exported to CSV");
            }
        } catch (Exception e) {
            log.error("Error exporting CSV", e);
            CopyableErrorDialog.showError("Export Error", "Failed to export CSV: " + e.getMessage());
        }
    }

    // ========================================================================
    // EVENT HANDLERS - DETAILS PANEL
    // ========================================================================

    @FXML
    public void handleGeneratePlans() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence first");
            return;
        }

        log.info("Action: Generate Academic Plans");

        showInfo("Generate Plans", "To generate academic plans from this sequence:\n\n" +
                "1. Go to Academic Planning → Academic Plan Management\n" +
                "2. Click 'New Plan' for each student\n" +
                "3. Select this sequence: " + currentSequence.getName() + "\n" +
                "4. The system will auto-populate courses\n\n" +
                "Batch plan generation will be available in a future update.");
    }

    @FXML
    public void handleViewStudents() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence first");
            return;
        }

        log.info("Action: View Students in Sequence");

        String count = enrolledStudentsLabel.getText();
        showInfo("View Students", "Students enrolled in '" + currentSequence.getName() + "': " + count + "\n\n" +
                "To view the full student list:\n\n" +
                "1. Go to Academic Planning → Academic Plan Management\n" +
                "2. Filter by Sequence: " + currentSequence.getName() + "\n" +
                "3. View all students with plans using this sequence\n\n" +
                "Direct student listing from sequences will be available in a future update.");
    }

    @FXML
    public void handlePreviewFlowchart() {
        if (currentSequence == null) {
            showWarning("No Selection", "Please select a sequence first");
            return;
        }

        log.info("Action: Preview Flowchart");

        // Use the existing visualize method which shows pathway
        handleVisualize();
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Update status label
     */
    private void updateStatusLabel(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    /**
     * Update last saved label
     */
    private void updateLastSavedLabel() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        Platform.runLater(() ->
            lastSavedLabel.setText("Last saved: " + LocalDateTime.now().format(formatter)));
    }

    /**
     * Show info dialog
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
     * Show warning dialog
     */
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
