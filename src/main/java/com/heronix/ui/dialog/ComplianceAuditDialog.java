package com.heronix.ui.dialog;

import com.heronix.dto.ComplianceViolation;
import com.heronix.dto.ComplianceViolation.ViolationSeverity;
import com.heronix.dto.ComplianceViolation.ViolationType;
import com.heronix.service.impl.ComplianceValidationService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compliance Audit Dialog
 *
 * Displays compliance violations with detailed information about:
 * - Certification mismatches
 * - Legal implications
 * - Regulatory requirements
 * - Links to official resources (FTCE, state agencies, federal)
 *
 * Allows user to review and approve corrections before applying them.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Compliance Validation
 */
public class ComplianceAuditDialog extends Dialog<Boolean> {

    private final ComplianceValidationService complianceService;
    private final ObservableList<ViolationRow> violations = FXCollections.observableArrayList();
    private TableView<ViolationRow> violationsTable;
    private Label summaryLabel;
    private TextArea detailsArea;
    private VBox resourcesPanel;

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public ComplianceAuditDialog(Stage owner, ComplianceValidationService complianceService) {
        this.complianceService = complianceService;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Compliance Audit - Certification Validation");
        setHeaderText("Review Teacher Certification Compliance");

        // Build UI
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(
            new ButtonType("View Regulatory Resources", ButtonBar.ButtonData.HELP),
            new ButtonType("Apply Corrections", ButtonBar.ButtonData.OK_DONE),
            ButtonType.CANCEL
        );
        dialogPane.setContent(createContent());

        // Set minimum size
        dialogPane.setMinWidth(1200);
        dialogPane.setMinHeight(800);

        // Load violations
        loadViolations();

        // Handle button actions
        Button resourcesBtn = (Button) dialogPane.lookupButton(dialogPane.getButtonTypes().get(0));
        resourcesBtn.setOnAction(e -> showRegulatoryResources());

        // Add button for temporary solutions
        ButtonType tempSolutionsBtn = new ButtonType("Temporary Solutions", ButtonBar.ButtonData.HELP_2);
        dialogPane.getButtonTypes().add(1, tempSolutionsBtn);
        Button tempBtn = (Button) dialogPane.lookupButton(tempSolutionsBtn);
        tempBtn.setOnAction(e -> showTemporarySolutions());

        // Prevent dialog close if violations selected but user clicks cancel
        setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return true;
            }
            return false;
        });
    }

    // ========================================================================
    // UI CREATION
    // ========================================================================

    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Summary section with color-coded counts
        summaryLabel = new Label("Loading compliance audit...");
        summaryLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        summaryLabel.setTextFill(Color.web("#2c3e50"));

        // Legend
        HBox legend = createLegend();

        // Create violations table
        violationsTable = createViolationsTable();

        // Details section
        Label detailsLabel = new Label("Violation Details:");
        detailsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setMaxHeight(200);
        detailsArea.setPromptText("Select a violation to view full details, legal implications, and recommended actions...");

        // Listen to selection changes
        violationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                detailsArea.setText(buildDetailText(newSelection.getViolation()));
            }
        });

        VBox.setVgrow(violationsTable, Priority.ALWAYS);

        content.getChildren().addAll(
            summaryLabel,
            legend,
            new Separator(),
            new Label("Compliance Violations:"),
            violationsTable,
            new Separator(),
            detailsLabel,
            detailsArea
        );

        return content;
    }

    private HBox createLegend() {
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(10, 0, 10, 0));

        legend.getChildren().addAll(
            createLegendItem("CRITICAL", "#e74c3c"),
            createLegendItem("HIGH", "#e67e22"),
            createLegendItem("MEDIUM", "#f39c12"),
            createLegendItem("LOW", "#95a5a6"),
            createLegendItem("WARNING", "#3498db")
        );

        return legend;
    }

    private HBox createLegendItem(String text, String color) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER_LEFT);

        Region colorBox = new Region();
        colorBox.setPrefSize(15, 15);
        colorBox.setStyle("-fx-background-color: " + color + "; -fx-border-color: #333; -fx-border-width: 1;");

        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.NORMAL, 11));

        item.getChildren().addAll(colorBox, label);
        return item;
    }

    private TableView<ViolationRow> createViolationsTable() {
        TableView<ViolationRow> table = new TableView<>();

        // Select column
        TableColumn<ViolationRow, Boolean> selectCol = new TableColumn<>("Fix");
        selectCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setMinWidth(50);
        selectCol.setMaxWidth(50);

        // Severity column
        TableColumn<ViolationRow, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getViolation().getSeverity().toString()));
        severityCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "CRITICAL":
                            setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                            break;
                        case "HIGH":
                            setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
                            break;
                        case "MEDIUM":
                            setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
                            break;
                        case "LOW":
                            setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                            break;
                        case "WARNING":
                            setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                            break;
                    }
                }
            }
        });
        severityCol.setMinWidth(90);
        severityCol.setMaxWidth(90);

        // Type column
        TableColumn<ViolationRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(param ->
            new SimpleStringProperty(formatViolationType(param.getValue().getViolation().getType())));
        typeCol.setMinWidth(140);

        // Course column
        TableColumn<ViolationRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getViolation().getCourse().getCourseName()));
        courseCol.setMinWidth(180);

        // Teacher column
        TableColumn<ViolationRow, String> teacherCol = new TableColumn<>("Teacher");
        teacherCol.setCellValueFactory(param ->
            new SimpleStringProperty(getTeacherName(param.getValue().getViolation().getTeacher())));
        teacherCol.setMinWidth(140);

        // Issue column
        TableColumn<ViolationRow, String> issueCol = new TableColumn<>("Issue Summary");
        issueCol.setCellValueFactory(param -> {
            String desc = param.getValue().getViolation().getDescription();
            // Extract first line only
            String firstLine = desc.split("\n")[0];
            return new SimpleStringProperty(firstLine);
        });
        issueCol.setMinWidth(300);

        // Auto-fix available column
        TableColumn<ViolationRow, String> fixCol = new TableColumn<>("Fix Available");
        fixCol.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getViolation().isAutoCorrectAvailable() ? "✓ Yes" : "✗ Manual"));
        fixCol.setMinWidth(100);
        fixCol.setMaxWidth(100);

        table.getColumns().addAll(selectCol, severityCol, typeCol, courseCol, teacherCol, issueCol, fixCol);
        table.setEditable(true);
        table.setItems(violations);

        return table;
    }

    // ========================================================================
    // BUSINESS LOGIC
    // ========================================================================

    private void loadViolations() {
        try {
            List<ComplianceViolation> auditResults = complianceService.auditAllAssignments();

            violations.clear();
            for (ComplianceViolation violation : auditResults) {
                // Pre-select critical and high severity violations
                boolean autoSelect = violation.getSeverity() == ViolationSeverity.CRITICAL ||
                                   violation.getSeverity() == ViolationSeverity.HIGH;
                violations.add(new ViolationRow(violation, autoSelect));
            }

            updateSummary(auditResults);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load compliance audit");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void updateSummary(List<ComplianceViolation> violations) {
        long critical = violations.stream().filter(v -> v.getSeverity() == ViolationSeverity.CRITICAL).count();
        long high = violations.stream().filter(v -> v.getSeverity() == ViolationSeverity.HIGH).count();
        long medium = violations.stream().filter(v -> v.getSeverity() == ViolationSeverity.MEDIUM).count();
        long low = violations.stream().filter(v -> v.getSeverity() == ViolationSeverity.LOW).count();
        long warning = violations.stream().filter(v -> v.getSeverity() == ViolationSeverity.WARNING).count();

        summaryLabel.setText(String.format(
            "Found %d compliance violations: %d CRITICAL, %d HIGH, %d MEDIUM, %d LOW, %d WARNING",
            violations.size(), critical, high, medium, low, warning
        ));
    }

    private String buildDetailText(ComplianceViolation violation) {
        StringBuilder sb = new StringBuilder();

        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("  COMPLIANCE VIOLATION DETAILS\n");
        sb.append("═══════════════════════════════════════════════════════\n\n");

        sb.append("SEVERITY: ").append(violation.getSeverity()).append("\n");
        sb.append("TYPE: ").append(formatViolationType(violation.getType())).append("\n\n");

        sb.append("COURSE: ").append(violation.getCourse().getCourseName()).append("\n");
        sb.append("COURSE CODE: ").append(violation.getCourse().getCourseCode()).append("\n");
        sb.append("SUBJECT: ").append(violation.getCourse().getSubject()).append("\n\n");

        sb.append("TEACHER: ").append(getTeacherName(violation.getTeacher())).append("\n\n");

        sb.append("───────────────────────────────────────────────────────\n");
        sb.append("DESCRIPTION:\n");
        sb.append("───────────────────────────────────────────────────────\n");
        sb.append(violation.getDescription()).append("\n\n");

        sb.append("───────────────────────────────────────────────────────\n");
        sb.append("LEGAL IMPLICATIONS:\n");
        sb.append("───────────────────────────────────────────────────────\n");
        sb.append(violation.getLegalImplications()).append("\n\n");

        sb.append("───────────────────────────────────────────────────────\n");
        sb.append("RECOMMENDED ACTION:\n");
        sb.append("───────────────────────────────────────────────────────\n");
        sb.append(violation.getRecommendedAction()).append("\n\n");

        if (!violation.getReferenceUrls().isEmpty()) {
            sb.append("───────────────────────────────────────────────────────\n");
            sb.append("REGULATORY REFERENCES:\n");
            sb.append("───────────────────────────────────────────────────────\n");
            for (String url : violation.getReferenceUrls()) {
                sb.append("  • ").append(url).append("\n");
            }
            sb.append("\n");
        }

        sb.append("AUTO-CORRECTION: ").append(violation.isAutoCorrectAvailable() ? "Available" : "Not Available").append("\n");

        if (violation.isAutoCorrectAvailable() && !violation.getQualifiedTeachers().isEmpty()) {
            sb.append("\n───────────────────────────────────────────────────────\n");
            sb.append("QUALIFIED TEACHERS:\n");
            sb.append("───────────────────────────────────────────────────────\n");
            for (var teacher : violation.getQualifiedTeachers()) {
                sb.append("  • ").append(getTeacherName(teacher)).append("\n");
            }
        }

        return sb.toString();
    }

    private void showRegulatoryResources() {
        Map<String, String> resources = complianceService.getRegulatoryResources();

        Alert resourceDialog = new Alert(Alert.AlertType.INFORMATION);
        resourceDialog.setTitle("Regulatory Resources");
        resourceDialog.setHeaderText("Official Certification Resources");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        Label intro = new Label("Access official teacher certification requirements from state and federal agencies:");
        intro.setWrapText(true);
        intro.setFont(Font.font("System", FontWeight.NORMAL, 12));

        content.getChildren().add(intro);
        content.getChildren().add(new Separator());

        // Configured State
        if (resources.containsKey("CONFIGURED_STATE")) {
            Label stateLabel = new Label("Configured State: " + resources.get("CONFIGURED_STATE"));
            stateLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            content.getChildren().add(stateLabel);
        }

        // Florida Resources
        content.getChildren().add(new Label("\nFlorida Resources:"));
        content.getChildren().add(createHyperlink("FTCE Home", resources.get("FTCE_HOME")));
        content.getChildren().add(createHyperlink("Florida Certification Requirements", resources.get("FTCE_REQUIREMENTS")));
        content.getChildren().add(createHyperlink("Florida Department of Education", resources.get("FLORIDA_DOE")));

        // Other State Resources
        content.getChildren().add(new Label("\nOther State Resources:"));
        content.getChildren().add(createHyperlink("Texas SBEC", resources.get("TEXAS_SBEC")));
        content.getChildren().add(createHyperlink("California CTC", resources.get("CALIFORNIA_CTC")));
        content.getChildren().add(createHyperlink("New York SED", resources.get("NEW_YORK_SED")));

        // Federal Resources
        content.getChildren().add(new Label("\nFederal Resources:"));
        content.getChildren().add(createHyperlink("ESSA - Every Student Succeeds Act", resources.get("FEDERAL_ESSA")));
        content.getChildren().add(createHyperlink("IDEA - Individuals with Disabilities Education Act", resources.get("FEDERAL_IDEA")));
        content.getChildren().add(createHyperlink("HQT Flexibility Guidance", resources.get("USDOE_HQT")));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

        resourceDialog.getDialogPane().setContent(scrollPane);
        resourceDialog.getDialogPane().setMinWidth(700);
        resourceDialog.showAndWait();
    }

    private Hyperlink createHyperlink(String text, String url) {
        Hyperlink link = new Hyperlink(text);
        link.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to open link");
                alert.setContentText("URL: " + url + "\nError: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        link.setTooltip(new Tooltip(url));
        return link;
    }

    private String formatViolationType(ViolationType type) {
        return switch (type) {
            case NO_CERTIFICATION -> "No Certification";
            case EXPIRED_CERTIFICATION -> "Expired Cert";
            case WRONG_GRADE_LEVEL -> "Wrong Grade Level";
            case MISSING_HQT_REQUIREMENT -> "Missing HQT";
            case MISSING_ENDORSEMENT -> "Missing Endorsement";
            case FEDERAL_VIOLATION -> "Federal Violation";
            case STATE_VIOLATION -> "State Violation";
            case ACCREDITATION_RISK -> "Accreditation Risk";
            case BEST_PRACTICE -> "Best Practice";
        };
    }

    private String getTeacherName(com.heronix.model.domain.Teacher teacher) {
        if (teacher.getFirstName() != null && teacher.getLastName() != null) {
            return teacher.getFirstName() + " " + teacher.getLastName();
        } else if (teacher.getName() != null) {
            return teacher.getName();
        }
        return "Unknown";
    }

    /**
     * Show temporary solutions dialog
     */
    private void showTemporarySolutions() {
        Alert solutionsDialog = new Alert(Alert.AlertType.INFORMATION);
        solutionsDialog.setTitle("Temporary Solutions - No Qualified Teacher Available");
        solutionsDialog.setHeaderText("Legal Options When Certified Teachers Are Unavailable");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label intro = new Label(
            "When no qualified certified teachers are available, the following temporary " +
            "solutions are legally permitted under most state regulations:"
        );
        intro.setWrapText(true);
        intro.setFont(Font.font("System", FontWeight.BOLD, 12));

        content.getChildren().add(intro);
        content.getChildren().add(new Separator());

        // Get temporary solutions from service
        java.util.List<com.heronix.dto.ComplianceAlert> alerts =
            complianceService.generateComplianceAlerts();

        java.util.List<com.heronix.dto.ComplianceAlert.TemporarySolution> solutions =
            new java.util.ArrayList<>();

        if (!alerts.isEmpty()) {
            solutions = alerts.get(0).getTemporarySolutions();
        }

        // Display each solution
        for (com.heronix.dto.ComplianceAlert.TemporarySolution solution : solutions) {
            VBox solutionBox = createSolutionPanel(solution);
            content.getChildren().add(solutionBox);
            content.getChildren().add(new Separator());
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setPrefWidth(800);

        solutionsDialog.getDialogPane().setContent(scrollPane);
        solutionsDialog.getDialogPane().setMinWidth(850);
        solutionsDialog.showAndWait();
    }

    /**
     * Create panel for a temporary solution
     */
    private VBox createSolutionPanel(com.heronix.dto.ComplianceAlert.TemporarySolution solution) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                    "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Title with availability badge
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(solution.getType().toString().replace("_", " "));
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#2c3e50"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label availabilityBadge = new Label(solution.isAvailable() ? "✓ AVAILABLE" : "⚠ Requires Setup");
        availabilityBadge.setStyle(solution.isAvailable() ?
            "-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3 8 3 8; " +
            "-fx-background-radius: 3; -fx-font-size: 10; -fx-font-weight: bold;" :
            "-fx-background-color: #ffc107; -fx-text-fill: #000; -fx-padding: 3 8 3 8; " +
            "-fx-background-radius: 3; -fx-font-size: 10; -fx-font-weight: bold;");

        titleBox.getChildren().addAll(title, spacer, availabilityBadge);

        // Duration badge
        Label durationLabel = new Label("Max Duration: " + solution.getMaxDurationDays() + " days" +
            (solution.getMaxDurationDays() == 0 ? " (permanent)" : ""));
        durationLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6c757d;");

        // Description
        Label desc = new Label(solution.getDescription());
        desc.setWrapText(true);
        desc.setFont(Font.font("System", FontWeight.NORMAL, 12));

        // Legal basis
        Label legalLabel = new Label("Legal Basis:");
        legalLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        legalLabel.setTextFill(Color.web("#495057"));

        Label legal = new Label(solution.getLegalBasis());
        legal.setWrapText(true);
        legal.setFont(Font.font("System", FontWeight.NORMAL, 11));
        legal.setStyle("-fx-text-fill: #6c757d;");

        // Requirements
        if (!solution.getRequirements().isEmpty()) {
            Label reqLabel = new Label("Requirements:");
            reqLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
            reqLabel.setTextFill(Color.web("#495057"));

            VBox reqBox = new VBox(3);
            for (String req : solution.getRequirements()) {
                Label reqItem = new Label("  • " + req);
                reqItem.setWrapText(true);
                reqItem.setFont(Font.font("System", FontWeight.NORMAL, 11));
                reqBox.getChildren().add(reqItem);
            }

            box.getChildren().addAll(reqLabel, reqBox);
        }

        // Application process
        Label processLabel = new Label("Application Process:");
        processLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        processLabel.setTextFill(Color.web("#495057"));

        Label process = new Label(solution.getApplicationProcess());
        process.setWrapText(true);
        process.setFont(Font.font("System", FontWeight.NORMAL, 11));
        process.setStyle("-fx-text-fill: #495057;");

        box.getChildren().addAll(
            titleBox,
            durationLabel,
            desc,
            new Region(), // spacer
            legalLabel,
            legal,
            new Region(), // spacer
            processLabel,
            process
        );

        return box;
    }

    // ========================================================================
    // RESULT HANDLING
    // ========================================================================

    public List<ComplianceViolation> getSelectedViolations() {
        return violations.stream()
            .filter(ViolationRow::isSelected)
            .map(ViolationRow::getViolation)
            .collect(Collectors.toList());
    }

    // ========================================================================
    // ROW MODEL
    // ========================================================================

    public static class ViolationRow {
        private final ComplianceViolation violation;
        private final SimpleBooleanProperty selected;

        public ViolationRow(ComplianceViolation violation, boolean selected) {
            this.violation = violation;
            this.selected = new SimpleBooleanProperty(selected);
        }

        public ComplianceViolation getViolation() {
            return violation;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }
    }
}
