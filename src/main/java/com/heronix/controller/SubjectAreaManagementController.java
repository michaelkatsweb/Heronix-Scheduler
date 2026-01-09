package com.heronix.controller;

import com.heronix.model.domain.SubjectArea;
import com.heronix.service.SubjectAreaService;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
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
 * Subject Area Management Controller
 *
 * Manages the Subject Area Management UI for creating and editing
 * academic subject areas and their hierarchical relationships.
 *
 * Location: src/main/java/com/eduscheduler/controller/SubjectAreaManagementController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 6, 2025
 */
@Slf4j
@Controller
public class SubjectAreaManagementController {

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private SubjectAreaService subjectAreaService;

    // ========================================================================
    // FXML COMPONENTS - TOP SECTION
    // ========================================================================

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private TextField searchField;

    @FXML
    private Label totalSubjectsLabel;

    @FXML
    private Label topLevelLabel;

    @FXML
    private Label subSubjectsLabel;

    @FXML
    private Label totalCoursesLabel;

    @FXML
    private Label activeFilterLabel;

    // ========================================================================
    // FXML COMPONENTS - TABLE
    // ========================================================================

    @FXML
    private TableView<SubjectArea> subjectsTable;

    @FXML
    private TableColumn<SubjectArea, String> codeColumn;

    @FXML
    private TableColumn<SubjectArea, String> nameColumn;

    @FXML
    private TableColumn<SubjectArea, String> departmentColumn;

    @FXML
    private TableColumn<SubjectArea, String> parentColumn;

    @FXML
    private TableColumn<SubjectArea, Integer> levelColumn;

    @FXML
    private TableColumn<SubjectArea, Integer> coursesColumn;

    @FXML
    private TableColumn<SubjectArea, String> activeColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private Label selectionLabel;

    // ========================================================================
    // FXML COMPONENTS - DETAILS PANEL
    // ========================================================================

    @FXML
    private Label detailCodeLabel;

    @FXML
    private Label detailNameLabel;

    @FXML
    private Label detailDepartmentLabel;

    @FXML
    private Label detailParentLabel;

    @FXML
    private Label detailLevelLabel;

    @FXML
    private Label detailColorLabel;

    @FXML
    private Region colorPreview;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label detailCoursesLabel;

    @FXML
    private Label detailTotalCoursesLabel;

    @FXML
    private Label detailChildrenLabel;

    @FXML
    private Label detailStatusLabel;

    @FXML
    private ListView<String> childSubjectsListView;

    @FXML
    private Label lastUpdatedLabel;

    // ========================================================================
    // STATE
    // ========================================================================

    private ObservableList<SubjectArea> allSubjects = FXCollections.observableArrayList();
    private ObservableList<SubjectArea> filteredSubjects = FXCollections.observableArrayList();
    private SubjectArea currentSubjectArea;

    private enum FilterType {
        ALL,
        TOP_LEVEL,
        SUB_SUBJECTS,
        ACTIVE_ONLY,
        INACTIVE
    }

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing SubjectAreaManagementController");

        setupTableColumns();
        setupFilters();
        setupSearchListener();
        loadData();

        // Selection listener for table
        subjectsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSubjectArea = newVal;
                loadSubjectAreaDetails(newVal);
                updateSelectionLabel(newVal);
            } else {
                currentSubjectArea = null;
                clearDetails();
                selectionLabel.setText("No selection");
            }
        });

        updateStatusLabel("Ready");
    }

    /**
     * Setup table columns with custom cell factories
     */
    private void setupTableColumns() {
        // Code column
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Department column
        departmentColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDepartment() != null ?
                cellData.getValue().getDepartment() : "-"));

        // Parent column
        parentColumn.setCellValueFactory(cellData -> {
            SubjectArea parent = cellData.getValue().getParentSubject();
            return new SimpleStringProperty(parent != null ? parent.getCode() : "-");
        });

        // Level column
        levelColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getHierarchyLevel()));

        // Courses column
        coursesColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getCourseCount()));

        // Active column
        activeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(Boolean.TRUE.equals(cellData.getValue().getActive()) ? "Yes" : "No"));

        // Bind data
        subjectsTable.setItems(filteredSubjects);
    }

    /**
     * Setup filter ComboBox
     */
    private void setupFilters() {
        filterComboBox.setItems(FXCollections.observableArrayList(
            "All Subject Areas",
            "Top-Level Only",
            "Sub-Subjects Only",
            "Active Only",
            "Inactive Only"
        ));
        filterComboBox.setValue("All Subject Areas");
    }

    /**
     * Setup search field listener
     */
    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load all subject areas from database
     */
    private void loadData() {
        try {
            log.info("Loading subject areas");

            List<SubjectArea> subjects = subjectAreaService.getAllSubjectAreas();
            allSubjects.clear();
            allSubjects.addAll(subjects);

            applyFilters();
            updateStatistics();
            updateLastUpdatedLabel();

            updateStatusLabel("Loaded " + subjects.size() + " subject areas");
            log.info("Loaded {} subject areas", subjects.size());

        } catch (Exception e) {
            log.error("Error loading subject areas", e);
            CopyableErrorDialog.showError("Load Error", "Failed to load subject areas: " + e.getMessage());
            updateStatusLabel("Error loading data");
        }
    }

    /**
     * Load subject area details into details panel
     */
    private void loadSubjectAreaDetails(SubjectArea subject) {
        try {
            // Basic Info
            detailCodeLabel.setText(subject.getCode() != null ? subject.getCode() : "-");
            detailNameLabel.setText(subject.getName() != null ? subject.getName() : "-");
            detailDepartmentLabel.setText(subject.getDepartment() != null ? subject.getDepartment() : "-");

            // Parent
            SubjectArea parent = subject.getParentSubject();
            detailParentLabel.setText(parent != null ? parent.getName() + " (" + parent.getCode() + ")" : "None (Top-Level)");

            // Level
            detailLevelLabel.setText(String.valueOf(subject.getHierarchyLevel()));

            // Color
            String color = subject.getDisplayColor();
            if (color != null && !color.isEmpty()) {
                detailColorLabel.setText(color);
                colorPreview.setStyle("-fx-background-color: " + color + "; -fx-border-color: #ccc; -fx-border-width: 1;");
            } else {
                detailColorLabel.setText("Not set");
                colorPreview.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-width: 1;");
            }

            // Description
            descriptionArea.setText(subject.getDescription() != null ?
                subject.getDescription() : "No description available");

            // Statistics
            detailCoursesLabel.setText(String.valueOf(subject.getCourseCount()));
            detailTotalCoursesLabel.setText(String.valueOf(subject.getTotalCourseCount()));
            detailChildrenLabel.setText(String.valueOf(subject.hasChildren() ? subject.getChildSubjects().size() : 0));
            detailStatusLabel.setText(Boolean.TRUE.equals(subject.getActive()) ? "Active" : "Inactive");

            // Child subjects
            loadChildSubjects(subject);

        } catch (Exception e) {
            log.error("Error loading subject area details", e);
        }
    }

    /**
     * Load child subjects into ListView
     */
    private void loadChildSubjects(SubjectArea subject) {
        try {
            if (subject.hasChildren()) {
                List<String> children = subject.getChildSubjects().stream()
                    .map(child -> child.getName() + " (" + child.getCode() + ")")
                    .collect(Collectors.toList());
                childSubjectsListView.setItems(FXCollections.observableArrayList(children));
            } else {
                childSubjectsListView.setItems(FXCollections.observableArrayList("No child subjects"));
            }
        } catch (Exception e) {
            log.error("Error loading child subjects", e);
        }
    }

    /**
     * Clear details panel
     */
    private void clearDetails() {
        detailCodeLabel.setText("-");
        detailNameLabel.setText("-");
        detailDepartmentLabel.setText("-");
        detailParentLabel.setText("-");
        detailLevelLabel.setText("-");
        detailColorLabel.setText("-");
        colorPreview.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-width: 1;");
        descriptionArea.setText("");
        detailCoursesLabel.setText("0");
        detailTotalCoursesLabel.setText("0");
        detailChildrenLabel.setText("0");
        detailStatusLabel.setText("-");
        childSubjectsListView.setItems(FXCollections.observableArrayList());
    }

    /**
     * Update statistics in info bar
     */
    private void updateStatistics() {
        try {
            int total = allSubjects.size();
            int topLevel = (int) allSubjects.stream().filter(SubjectArea::isTopLevel).count();
            int subSubjects = total - topLevel;
            int totalCourses = allSubjects.stream().mapToInt(SubjectArea::getTotalCourseCount).sum();

            totalSubjectsLabel.setText(String.valueOf(total));
            topLevelLabel.setText(String.valueOf(topLevel));
            subSubjectsLabel.setText(String.valueOf(subSubjects));
            totalCoursesLabel.setText(String.valueOf(totalCourses));

        } catch (Exception e) {
            log.error("Error updating statistics", e);
        }
    }

    /**
     * Apply filters to subject list
     */
    private void applyFilters() {
        try {
            String filterValue = filterComboBox.getValue();
            String searchText = searchField.getText().toLowerCase();

            List<SubjectArea> filtered = allSubjects.stream()
                .filter(subject -> matchesFilter(subject, filterValue))
                .filter(subject -> matchesSearch(subject, searchText))
                .collect(Collectors.toList());

            filteredSubjects.clear();
            filteredSubjects.addAll(filtered);

            activeFilterLabel.setText("Showing: " + filtered.size() + " of " + allSubjects.size());

        } catch (Exception e) {
            log.error("Error applying filters", e);
        }
    }

    /**
     * Check if subject matches filter
     */
    private boolean matchesFilter(SubjectArea subject, String filterValue) {
        if (filterValue == null || filterValue.equals("All Subject Areas")) {
            return true;
        }

        switch (filterValue) {
            case "Top-Level Only":
                return subject.isTopLevel();
            case "Sub-Subjects Only":
                return !subject.isTopLevel();
            case "Active Only":
                return Boolean.TRUE.equals(subject.getActive());
            case "Inactive Only":
                return !Boolean.TRUE.equals(subject.getActive());
            default:
                return true;
        }
    }

    /**
     * Check if subject matches search text
     */
    private boolean matchesSearch(SubjectArea subject, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        return subject.getName().toLowerCase().contains(searchText) ||
               subject.getCode().toLowerCase().contains(searchText) ||
               (subject.getDepartment() != null && subject.getDepartment().toLowerCase().contains(searchText));
    }

    // ========================================================================
    // EVENT HANDLERS - TOP ACTIONS
    // ========================================================================

    @FXML
    public void handleNewSubjectArea() {
        log.info("Action: New Subject Area");

        try {
            // Create new subject area dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("New Subject Area");
            dialog.setHeaderText("Create New Subject Area");

            // Create form fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            TextField codeField = new TextField();
            TextField nameField = new TextField();
            TextField departmentField = new TextField();
            TextArea descriptionField = new TextArea();
            descriptionField.setPrefRowCount(3);
            ComboBox<String> parentComboBox = new ComboBox<>();
            ColorPicker colorPicker = new ColorPicker(Color.BLUE);

            // Populate parent subject options
            List<String> parentOptions = new ArrayList<>();
            parentOptions.add("(None - Top Level)");
            allSubjects.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .forEach(s -> parentOptions.add(s.getCode() + " - " + s.getName()));
            parentComboBox.setItems(FXCollections.observableArrayList(parentOptions));
            parentComboBox.setValue("(None - Top Level)");

            grid.add(new Label("Code:"), 0, 0);
            grid.add(codeField, 1, 0);
            grid.add(new Label("Name:"), 0, 1);
            grid.add(nameField, 1, 1);
            grid.add(new Label("Department:"), 0, 2);
            grid.add(departmentField, 1, 2);
            grid.add(new Label("Parent Subject:"), 0, 3);
            grid.add(parentComboBox, 1, 3);
            grid.add(new Label("Color:"), 0, 4);
            grid.add(colorPicker, 1, 4);
            grid.add(new Label("Description:"), 0, 5);
            grid.add(descriptionField, 1, 5);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Validate required fields
                if (codeField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
                    showWarning("Validation Error", "Code and Name are required fields");
                    return;
                }

                // Create new subject area
                SubjectArea newSubject = new SubjectArea();
                newSubject.setCode(codeField.getText().trim().toUpperCase());
                newSubject.setName(nameField.getText().trim());
                newSubject.setDepartment(departmentField.getText().trim());
                newSubject.setDescription(descriptionField.getText().trim());
                newSubject.setActive(true);

                // Set color
                Color selectedColor = colorPicker.getValue();
                String hexColor = String.format("#%02X%02X%02X",
                    (int)(selectedColor.getRed() * 255),
                    (int)(selectedColor.getGreen() * 255),
                    (int)(selectedColor.getBlue() * 255));
                newSubject.setDisplayColor(hexColor);

                // Set parent if selected
                String parentSelection = parentComboBox.getValue();
                if (parentSelection != null && !parentSelection.equals("(None - Top Level)")) {
                    String parentCode = parentSelection.split(" - ")[0];
                    Optional<SubjectArea> parent = allSubjects.stream()
                        .filter(s -> s.getCode().equals(parentCode))
                        .findFirst();
                    parent.ifPresent(newSubject::setParentSubject);
                }

                // Save
                SubjectArea saved = subjectAreaService.createSubjectArea(newSubject);
                showInfo("Success", "Created new subject area: " + saved.getName());

                // Reload and select new subject
                loadData();
                subjectsTable.getSelectionModel().select(saved);
                updateStatusLabel("Created: " + saved.getName());
            }
        } catch (Exception e) {
            log.error("Error creating subject area", e);
            CopyableErrorDialog.showError("Create Error", "Failed to create subject area: " + e.getMessage());
        }
    }

    @FXML
    public void handleImportSubjects() {
        log.info("Action: Import Subjects");

        // Show CSV import instructions
        String instructions = "Subject Area CSV Import:\n\n" +
            "1. Prepare a CSV file with these columns:\n" +
            "   - code (required)\n" +
            "   - name (required)\n" +
            "   - department\n" +
            "   - parent_code (for sub-subjects)\n" +
            "   - display_color (hex format, e.g., #3498DB)\n" +
            "   - description\n\n" +
            "2. Use Courses → Import → CSV Import\n" +
            "3. Map columns to match your CSV structure\n\n" +
            "Example:\n" +
            "MATH,Mathematics,Math Dept,,#3498DB,Mathematics courses\n" +
            "ALG,Algebra,Math Dept,MATH,#2ECC71,Algebra courses";

        showInfo("CSV Import Instructions", instructions);
    }

    @FXML
    public void handleRefresh() {
        log.info("Action: Refresh");
        loadData();

        if (currentSubjectArea != null) {
            // Reload current subject area
            Optional<SubjectArea> refreshed = subjectAreaService.getSubjectAreaById(currentSubjectArea.getId());
            refreshed.ifPresent(this::loadSubjectAreaDetails);
        }
    }

    @FXML
    public void handleFilterChange() {
        applyFilters();
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        applyFilters();
    }

    // ========================================================================
    // EVENT HANDLERS - TABLE ACTIONS
    // ========================================================================

    @FXML
    public void handleEdit() {
        if (currentSubjectArea == null) {
            showWarning("No Selection", "Please select a subject area to edit");
            return;
        }

        log.info("Action: Edit Subject Area");

        try {
            // Create edit dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Subject Area");
            dialog.setHeaderText("Edit: " + currentSubjectArea.getName());

            // Create form fields with current values
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            TextField codeField = new TextField(currentSubjectArea.getCode());
            TextField nameField = new TextField(currentSubjectArea.getName());
            TextField departmentField = new TextField(currentSubjectArea.getDepartment() != null ? currentSubjectArea.getDepartment() : "");
            TextArea descriptionField = new TextArea(currentSubjectArea.getDescription() != null ? currentSubjectArea.getDescription() : "");
            descriptionField.setPrefRowCount(3);
            ComboBox<String> parentComboBox = new ComboBox<>();
            CheckBox activeCheckBox = new CheckBox();
            activeCheckBox.setSelected(Boolean.TRUE.equals(currentSubjectArea.getActive()));

            // Color picker - parse hex color or use default
            Color initialColor = Color.BLUE;
            if (currentSubjectArea.getDisplayColor() != null && !currentSubjectArea.getDisplayColor().isEmpty()) {
                try {
                    initialColor = Color.web(currentSubjectArea.getDisplayColor());
                } catch (Exception e) {
                    log.warn("Could not parse color: " + currentSubjectArea.getDisplayColor());
                }
            }
            ColorPicker colorPicker = new ColorPicker(initialColor);

            // Populate parent subject options
            List<String> parentOptions = new ArrayList<>();
            parentOptions.add("(None - Top Level)");
            allSubjects.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .filter(s -> !s.getId().equals(currentSubjectArea.getId())) // Don't allow self as parent
                .forEach(s -> parentOptions.add(s.getCode() + " - " + s.getName()));
            parentComboBox.setItems(FXCollections.observableArrayList(parentOptions));

            // Set current parent selection
            if (currentSubjectArea.getParentSubject() != null) {
                String currentParent = currentSubjectArea.getParentSubject().getCode() + " - " + currentSubjectArea.getParentSubject().getName();
                parentComboBox.setValue(currentParent);
            } else {
                parentComboBox.setValue("(None - Top Level)");
            }

            grid.add(new Label("Code:"), 0, 0);
            grid.add(codeField, 1, 0);
            grid.add(new Label("Name:"), 0, 1);
            grid.add(nameField, 1, 1);
            grid.add(new Label("Department:"), 0, 2);
            grid.add(departmentField, 1, 2);
            grid.add(new Label("Parent Subject:"), 0, 3);
            grid.add(parentComboBox, 1, 3);
            grid.add(new Label("Color:"), 0, 4);
            grid.add(colorPicker, 1, 4);
            grid.add(new Label("Active:"), 0, 5);
            grid.add(activeCheckBox, 1, 5);
            grid.add(new Label("Description:"), 0, 6);
            grid.add(descriptionField, 1, 6);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Validate required fields
                if (codeField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
                    showWarning("Validation Error", "Code and Name are required fields");
                    return;
                }

                // Update subject area
                currentSubjectArea.setCode(codeField.getText().trim().toUpperCase());
                currentSubjectArea.setName(nameField.getText().trim());
                currentSubjectArea.setDepartment(departmentField.getText().trim());
                currentSubjectArea.setDescription(descriptionField.getText().trim());
                currentSubjectArea.setActive(activeCheckBox.isSelected());

                // Set color
                Color selectedColor = colorPicker.getValue();
                String hexColor = String.format("#%02X%02X%02X",
                    (int)(selectedColor.getRed() * 255),
                    (int)(selectedColor.getGreen() * 255),
                    (int)(selectedColor.getBlue() * 255));
                currentSubjectArea.setDisplayColor(hexColor);

                // Update parent if changed
                String parentSelection = parentComboBox.getValue();
                if (parentSelection != null && !parentSelection.equals("(None - Top Level)")) {
                    String parentCode = parentSelection.split(" - ")[0];
                    Optional<SubjectArea> parent = allSubjects.stream()
                        .filter(s -> s.getCode().equals(parentCode))
                        .findFirst();
                    parent.ifPresent(currentSubjectArea::setParentSubject);
                } else {
                    currentSubjectArea.setParentSubject(null);
                }

                // Save
                subjectAreaService.updateSubjectArea(currentSubjectArea.getId(), currentSubjectArea);
                showInfo("Success", "Subject area updated successfully");

                // Reload and reselect
                loadData();
                loadSubjectAreaDetails(currentSubjectArea);
                updateStatusLabel("Updated: " + currentSubjectArea.getName());
            }
        } catch (Exception e) {
            log.error("Error editing subject area", e);
            CopyableErrorDialog.showError("Edit Error", "Failed to edit subject area: " + e.getMessage());
        }
    }

    @FXML
    public void handleDelete() {
        if (currentSubjectArea == null) {
            showWarning("No Selection", "Please select a subject area to delete");
            return;
        }

        log.info("Action: Delete Subject Area");

        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Subject Area");
        confirm.setHeaderText("Delete " + currentSubjectArea.getName() + "?");
        confirm.setContentText("This will deactivate the subject area (soft delete). " +
            "Courses will not be deleted. Continue?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                subjectAreaService.deleteSubjectArea(currentSubjectArea.getId());
                showInfo("Success", "Subject area deleted successfully");

                // Clear selection and refresh
                currentSubjectArea = null;
                subjectsTable.getSelectionModel().clearSelection();
                loadData();

            } catch (Exception e) {
                log.error("Error deleting subject area", e);
                CopyableErrorDialog.showError("Delete Error", "Failed to delete subject area: " + e.getMessage());
            }
        }
    }

    // ========================================================================
    // EVENT HANDLERS - DETAILS PANEL ACTIONS
    // ========================================================================

    @FXML
    public void handleViewCourses() {
        if (currentSubjectArea == null) {
            showWarning("No Selection", "Please select a subject area first");
            return;
        }

        log.info("Action: View Courses - {}", currentSubjectArea.getName());

        // Show navigation instructions to view courses
        String instructions = "View Courses for " + currentSubjectArea.getName() + ":\n\n" +
            "1. Navigate to: Courses Module\n" +
            "2. Use the filter dropdown to select: \"" + currentSubjectArea.getName() + "\"\n" +
            "3. Or use the search field to search for: \"" + currentSubjectArea.getCode() + "\"\n\n" +
            "Course count: " + currentSubjectArea.getCourseCount() + " direct courses\n" +
            "Total with sub-subjects: " + currentSubjectArea.getTotalCourseCount() + " courses";

        showInfo("Navigate to Courses", instructions);
        updateStatusLabel("View courses for: " + currentSubjectArea.getName());
    }

    @FXML
    public void handleViewSequences() {
        if (currentSubjectArea == null) {
            showWarning("No Selection", "Please select a subject area first");
            return;
        }

        log.info("Action: View Sequences - {}", currentSubjectArea.getName());

        // Show navigation instructions to view course sequences
        String instructions = "View Course Sequences for " + currentSubjectArea.getName() + ":\n\n" +
            "1. Navigate to: Curriculum → Course Sequence Editor\n" +
            "2. Look for sequences containing courses from this subject area\n" +
            "3. Subject code to filter by: " + currentSubjectArea.getCode() + "\n\n" +
            "Tip: Course sequences define the recommended pathway of courses\n" +
            "students should take within this subject area.";

        showInfo("Navigate to Course Sequences", instructions);
        updateStatusLabel("View sequences for: " + currentSubjectArea.getName());
    }

    @FXML
    public void handleManageRequirements() {
        if (currentSubjectArea == null) {
            showWarning("No Selection", "Please select a subject area first");
            return;
        }

        log.info("Action: Manage Requirements - {}", currentSubjectArea.getName());

        // Show navigation instructions for graduation requirements
        String instructions = "Manage Graduation Requirements for " + currentSubjectArea.getName() + ":\n\n" +
            "1. Navigate to: Curriculum → Graduation Requirements\n" +
            "2. Configure credit requirements for: " + currentSubjectArea.getName() + "\n" +
            "3. Set minimum credits needed for graduation\n" +
            "4. Define which courses satisfy requirements\n\n" +
            "Example: Mathematics subject area might require:\n" +
            "- 4 credits minimum\n" +
            "- Algebra I (required)\n" +
            "- Geometry (required)\n" +
            "- Advanced math electives (2 credits)\n\n" +
            "Subject Code: " + currentSubjectArea.getCode();

        showInfo("Navigate to Graduation Requirements", instructions);
        updateStatusLabel("Manage requirements for: " + currentSubjectArea.getName());
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
     * Update selection label
     */
    private void updateSelectionLabel(SubjectArea subject) {
        Platform.runLater(() ->
            selectionLabel.setText("Selected: " + subject.getName() + " (" + subject.getCode() + ")"));
    }

    /**
     * Update last updated label
     */
    private void updateLastUpdatedLabel() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        Platform.runLater(() ->
            lastUpdatedLabel.setText("Last updated: " + LocalDateTime.now().format(formatter)));
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
