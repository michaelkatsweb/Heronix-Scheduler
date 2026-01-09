package com.heronix.ui.controller;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.ParentGuardian;
import com.heronix.model.domain.EmergencyContact;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.StudentGrade;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.MedicalRecordRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.service.GradeService;
import com.heronix.service.StudentPlacementService;
import com.heronix.ui.component.EditableTableCell;
import com.heronix.ui.controller.MedicalRecordDialogController;
import com.heronix.ui.controller.StudentGradeDialogController;
import com.heronix.ui.controller.GradeImportDialogController;
import com.heronix.service.GradeImportService;
import com.heronix.ui.util.CopyableErrorDialog;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.DialogPane;
import org.springframework.context.ApplicationContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.io.File;

/**
 * Students Controller - ENHANCED with full CRUD operations
 * Location: src/main/java/com/eduscheduler/ui/controller/StudentsController.java
 *
 * ✅ NEW FEATURES:
 * - Edit Student functionality with comprehensive form
 * - Delete Student functionality with confirmation
 * - Enhanced Add Student form with all entity fields
 * - Action buttons column (Edit/Delete)
 * - Improved form validation
 * - Tabbed dialog for organized data entry
 *
 * @version 4.0.0 - Complete CRUD Implementation
 */
@Slf4j
@Controller
public class StudentsController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private StudentPlacementService placementService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private com.heronix.service.DistrictSettingsService districtSettingsService;

    @Autowired
    private com.heronix.service.GraduationRequirementsService graduationRequirementsService;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> gradeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> medicalFilter;
    @FXML private ComboBox<String> spedFilter;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> studentIdColumn;
    @FXML private TableColumn<Student, Void> photoColumn;
    @FXML private TableColumn<Student, Void> qrCodeColumn;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, String> gradeColumn;
    @FXML private TableColumn<Student, String> emailColumn;
    @FXML private TableColumn<Student, Double> gpaColumn;
    @FXML private TableColumn<Student, Double> creditsColumn;
    @FXML private TableColumn<Student, String> academicStandingColumn;
    @FXML private TableColumn<Student, String> coursesColumn;
    @FXML private TableColumn<Student, String> iepColumn;
    @FXML private TableColumn<Student, String> plan504Column;
    @FXML private TableColumn<Student, String> statusColumn;
    @FXML private TableColumn<Student, Void> actionsColumn;
    @FXML private Label recordCountLabel;

    private ObservableList<Student> studentsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        log.debug("StudentsController.initialize() START");
        log.info("Initializing StudentsController");

        try {
            // Null checks
            if (studentsTable == null) {
                log.error("studentsTable is NULL!");
                return;
            }
            if (studentRepository == null) {
                log.error("studentRepository is NULL!");
                return;
            }

            log.debug("All components non-null");

            // Enable table editing
            studentsTable.setEditable(true);

            // Setup table columns with inline editing
            studentIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStudentId()));

            // Photo column - thumbnail with click to enlarge
            photoColumn.setCellFactory(col -> new TableCell<Student, Void>() {
                private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();

                {
                    imageView.setFitWidth(40);
                    imageView.setFitHeight(40);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setStyle("-fx-cursor: hand;");

                    // Click to enlarge
                    imageView.setOnMouseClicked(event -> {
                        Student student = getTableRow().getItem();
                        if (student != null && student.getPhotoPath() != null) {
                            showPhotoEnlarged(student);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Student student = getTableRow().getItem();
                        if (student != null && student.getPhotoPath() != null) {
                            try {
                                File photoFile = new File(student.getPhotoPath());
                                if (photoFile.exists()) {
                                    javafx.scene.image.Image image = new javafx.scene.image.Image(
                                        photoFile.toURI().toString(), 40, 40, true, true);
                                    imageView.setImage(image);
                                    setGraphic(imageView);
                                } else {
                                    setGraphic(createPlaceholderIcon());
                                }
                            } catch (Exception e) {
                                setGraphic(createPlaceholderIcon());
                            }
                        } else {
                            setGraphic(createPlaceholderIcon());
                        }
                    }
                }

                private javafx.scene.Node createPlaceholderIcon() {
                    Label placeholder = new Label("👤");
                    placeholder.setStyle("-fx-font-size: 24px; -fx-text-fill: #999;");
                    return placeholder;
                }
            });

            // QR Code column - mini preview with click to enlarge and print
            qrCodeColumn.setCellFactory(col -> new TableCell<Student, Void>() {
                private final javafx.scene.image.ImageView qrView = new javafx.scene.image.ImageView();

                {
                    qrView.setFitWidth(40);
                    qrView.setFitHeight(40);
                    qrView.setPreserveRatio(true);
                    qrView.setSmooth(false); // QR codes should be crisp
                    qrView.setStyle("-fx-cursor: hand;");

                    // Click to open QR code dialog
                    qrView.setOnMouseClicked(event -> {
                        Student student = getTableRow().getItem();
                        if (student != null) {
                            openQRCodeDialog(student);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Student student = getTableRow().getItem();
                        if (student != null) {
                            // Generate mini QR code
                            try {
                                if (student.getQrCodeId() == null || student.getQrCodeId().isEmpty()) {
                                    setGraphic(createQRPlaceholder());
                                } else {
                                    String qrContent = String.format("%s:%s:%s",
                                        student.getStudentId(),
                                        student.getEmail() != null ? student.getEmail() : "",
                                        student.getQrCodeId());

                                    byte[] qrBytes = generateMiniQRCode(qrContent, 40);
                                    javafx.scene.image.Image qrImage = new javafx.scene.image.Image(
                                        new java.io.ByteArrayInputStream(qrBytes));
                                    qrView.setImage(qrImage);
                                    setGraphic(qrView);
                                }
                            } catch (Exception e) {
                                setGraphic(createQRPlaceholder());
                            }
                        } else {
                            setGraphic(null);
                        }
                    }
                }

                private javafx.scene.Node createQRPlaceholder() {
                    Label placeholder = new Label("⬜");
                    placeholder.setStyle("-fx-font-size: 24px; -fx-text-fill: #999;");
                    placeholder.setTooltip(new Tooltip("No QR code - click to generate"));
                    placeholder.setStyle("-fx-cursor: hand;");
                    placeholder.setOnMouseClicked(event -> {
                        Student student = getTableRow().getItem();
                        if (student != null) {
                            openQRCodeDialog(student);
                        }
                    });
                    return placeholder;
                }
            });

            // Name column - read-only (use Edit button to modify)
            nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFirstName() + " " + data.getValue().getLastName()));

            // Grade column - editable
            gradeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getGradeLevel()));
            gradeColumn.setEditable(true);
            gradeColumn.setCellFactory(col -> EditableTableCell.forStringColumn((student, newGrade) -> {
                student.setGradeLevel(newGrade);
                studentRepository.save(student);
                log.info("Updated student {} grade to: {}", student.getStudentId(), newGrade);
            }));

            // Email column - editable with validation
            emailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail() != null ? data.getValue().getEmail() : ""));
            emailColumn.setEditable(true);
            emailColumn.setCellFactory(col -> EditableTableCell.forEmailColumn((student, newEmail) -> {
                student.setEmail(newEmail);
                studentRepository.save(student);
                log.info("Updated student {} email to: {}", student.getStudentId(), newEmail);
            }));

            // GPA and Academic columns
            gpaColumn.setCellValueFactory(new PropertyValueFactory<>("currentGPA"));
            gpaColumn.setCellFactory(col -> new TableCell<Student, Double>() {
                @Override
                protected void updateItem(Double gpa, boolean empty) {
                    super.updateItem(gpa, empty);
                    if (empty || gpa == null) {
                        setText("");
                        setStyle("");
                    } else {
                        setText(String.format("%.2f", gpa));
                        // Color code based on GPA
                        getStyleClass().clear();
                        if (gpa >= 3.75) {
                            getStyleClass().addAll("success-text", "text-bold"); // High honors - green
                        } else if (gpa >= 3.5) {
                            getStyleClass().add("success-text"); // Honors - light green
                        } else if (gpa >= 2.0) {
                            // Good standing - use default theme color
                        } else {
                            getStyleClass().addAll("danger-text", "text-bold"); // Warning - red
                        }
                    }
                }
            });

            creditsColumn.setCellValueFactory(new PropertyValueFactory<>("creditsEarned"));
            creditsColumn.setCellFactory(col -> new TableCell<Student, Double>() {
                @Override
                protected void updateItem(Double credits, boolean empty) {
                    super.updateItem(credits, empty);
                    if (empty || credits == null) {
                        setText("");
                    } else {
                        setText(String.format("%.1f", credits));
                    }
                }
            });

            // Academic Standing Column - Enhanced with Graduation Requirements
            academicStandingColumn.setCellValueFactory(data -> {
                Student student = data.getValue();
                if (graduationRequirementsService != null) {
                    String status = graduationRequirementsService.getAcademicStandingStatus(student);
                    String icon = graduationRequirementsService.getStandingIcon(student);
                    return new SimpleStringProperty(icon + " " + status);
                } else {
                    return new SimpleStringProperty(student.getAcademicStanding() != null ? student.getAcademicStanding() : "");
                }
            });
            academicStandingColumn.setCellFactory(col -> new TableCell<Student, String>() {
                @Override
                protected void updateItem(String standing, boolean empty) {
                    super.updateItem(standing, empty);
                    Student student = getTableRow().getItem();
                    if (empty || standing == null || student == null) {
                        setText("");
                        setStyle("");
                        setTooltip(null);
                    } else {
                        setText(standing);

                        // Use graduation requirements service for color coding
                        if (graduationRequirementsService != null) {
                            String color = graduationRequirementsService.getStandingColorCode(student);
                            getStyleClass().clear();
                            getStyleClass().add("text-bold");
                            // Map color codes to CSS classes
                            if (color.contains("4CAF50") || color.contains("green")) {
                                getStyleClass().add("success-text");
                            } else if (color.contains("FF9800") || color.contains("orange")) {
                                getStyleClass().add("warning-text");
                            } else if (color.contains("f44336") || color.contains("red")) {
                                getStyleClass().add("danger-text");
                            } else if (color.contains("2196F3") || color.contains("blue")) {
                                getStyleClass().add("primary-text");
                            }

                            // Add detailed tooltip
                            String tooltipText = graduationRequirementsService.getStandingTooltip(student);
                            Tooltip tooltip = new Tooltip(tooltipText);
                            tooltip.setWrapText(true);
                            tooltip.setMaxWidth(400);
                            setTooltip(tooltip);
                        } else {
                            // Fallback to old logic if service not available
                            getStyleClass().clear();
                            getStyleClass().add("text-bold");
                            if (standing.contains("Good") || standing.contains("On Track")) {
                                getStyleClass().add("success-text");
                            } else if (standing.contains("Warning") || standing.contains("At Risk")) {
                                getStyleClass().add("warning-text");
                            } else if (standing.contains("Probation") || standing.contains("Retention Risk")) {
                                getStyleClass().add("danger-text");
                            }
                        }
                    }
                }
            });

            // Non-editable columns
            coursesColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getEnrolledCourseCount())));

            // IEP Column - Shows "Yes" with visual indicator for students with IEP
            iepColumn.setCellValueFactory(data ->
                new SimpleStringProperty(Boolean.TRUE.equals(data.getValue().getHasIEP()) ? "Yes" : ""));
            iepColumn.setCellFactory(col -> new TableCell<Student, String>() {
                @Override
                protected void updateItem(String hasIep, boolean empty) {
                    super.updateItem(hasIep, empty);
                    if (empty || hasIep == null || hasIep.isEmpty()) {
                        setText("");
                        setStyle("");
                        setGraphic(null);
                    } else {
                        setText("✓ IEP");
                        getStyleClass().clear();
                        getStyleClass().addAll("warning-text", "text-bold"); // Orange for IEP
                        setTooltip(new Tooltip("Student has an Individual Education Plan"));
                    }
                }
            });

            // 504 Plan Column - Shows "Yes" with visual indicator for students with 504 plan
            plan504Column.setCellValueFactory(data ->
                new SimpleStringProperty(Boolean.TRUE.equals(data.getValue().getHas504Plan()) ? "Yes" : ""));
            plan504Column.setCellFactory(col -> new TableCell<Student, String>() {
                @Override
                protected void updateItem(String has504, boolean empty) {
                    super.updateItem(has504, empty);
                    if (empty || has504 == null || has504.isEmpty()) {
                        setText("");
                        setStyle("");
                        setGraphic(null);
                    } else {
                        setText("✓ 504");
                        getStyleClass().clear();
                        getStyleClass().addAll("primary-text", "text-bold"); // Blue for 504
                        setTooltip(new Tooltip("Student has a 504 Accommodation Plan"));
                    }
                }
            });

            statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getActive() ? "Active" : "Inactive"));

            log.debug("Columns configured with inline editing and GPA display");

            // Setup action buttons column
            setupActionButtons();
            log.debug("Action buttons configured");

            // ✅ NEW: Setup row factory for graduation requirements color coding
            setupRowFactory();
            log.debug("Row factory configured for graduation requirements");

            // Setup filters - using same values as stored in database
            if (gradeFilter != null) {
                gradeFilter.getItems().addAll(
                    "All", "K", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"
                );
                gradeFilter.setValue("All");
            }
            if (statusFilter != null) {
                statusFilter.getItems().addAll("All", "Active", "Inactive");
                statusFilter.setValue("All");
            }
            if (spedFilter != null) {
                spedFilter.getItems().addAll("All", "Has IEP", "Has 504", "Has IEP or 504", "No SPED");
                spedFilter.setValue("All");
            }
            log.debug("Filters configured");

            // Load data
            loadStudents();

            log.debug("StudentsController.initialize() COMPLETE");

        } catch (Exception e) {
            log.error("EXCEPTION in StudentsController.initialize()", e);
            throw e;
        }
    }

    /**
     * ✅ NEW: Setup action buttons in the Actions column
     */
    private void setupActionButtons() {
        actionsColumn.setCellFactory(new Callback<TableColumn<Student, Void>, TableCell<Student, Void>>() {
            @Override
            public TableCell<Student, Void> call(TableColumn<Student, Void> param) {
                return            new TableCell<Student, Void>() {
                    private final Button editBtn = new Button("✏️");
                    private final Button gradesBtn = new Button("📝");
                    private final Button coursesBtn = new Button("📚");
                    private final Button deleteBtn = new Button("🗑️");
                    private final HBox pane = new HBox(5, editBtn, gradesBtn, coursesBtn, deleteBtn);

                    {
                        pane.setAlignment(Pos.CENTER);
                        editBtn.getStyleClass().add("button-success");
                        editBtn.setStyle("-fx-cursor: hand; -fx-font-size: 11px;");
                        gradesBtn.getStyleClass().add("button-accent");
                        gradesBtn.setStyle("-fx-cursor: hand; -fx-font-size: 11px;");
                        coursesBtn.getStyleClass().add("button-primary");
                        coursesBtn.setStyle("-fx-cursor: hand; -fx-font-size: 11px;");
                        deleteBtn.getStyleClass().add("button-danger");
                        deleteBtn.setStyle("-fx-cursor: hand; -fx-font-size: 11px;");

                        editBtn.setTooltip(new Tooltip("Edit Student"));
                        gradesBtn.setTooltip(new Tooltip("Add/View Grades"));
                        coursesBtn.setTooltip(new Tooltip("Manage Courses"));
                        deleteBtn.setTooltip(new Tooltip("Delete Student"));

                        editBtn.setOnAction(event -> {
                            Student student = getTableView().getItems().get(getIndex());
                            handleEditStudent(student);
                        });

                        gradesBtn.setOnAction(event -> {
                            Student student = getTableView().getItems().get(getIndex());
                            handleAddGradeForStudent(student);
                        });

                        coursesBtn.setOnAction(event -> {
                            Student student = getTableView().getItems().get(getIndex());
                            handleViewCourses(student);
                        });

                        deleteBtn.setOnAction(event -> {
                            Student student = getTableView().getItems().get(getIndex());
                            handleDeleteStudent(student);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        });
    }

    /**
     * ✅ NEW: Setup row factory for graduation requirements visual indicators
     * Color-codes entire rows based on academic standing
     */
    private void setupRowFactory() {
        studentsTable.setRowFactory(tv -> new TableRow<Student>() {
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);

                if (empty || student == null || graduationRequirementsService == null) {
                    setStyle("");
                    setTooltip(null);
                } else {
                    // Use default table row styling (no custom background colors)
                    setStyle("");

                    // Add tooltip with graduation status summary
                    String status = graduationRequirementsService.getAcademicStandingStatus(student);
                    double creditsBehind = graduationRequirementsService.getCreditsBehind(student);
                    Double gpa = student.getCurrentGPA();
                    String gpaStr = gpa != null ? String.format("%.2f", gpa) : "N/A";

                    StringBuilder tooltipText = new StringBuilder();
                    tooltipText.append(String.format("%s %s\n",
                        graduationRequirementsService.getStandingIcon(student), status));
                    tooltipText.append(String.format("GPA: %s\n", gpaStr));

                    if (creditsBehind > 0) {
                        tooltipText.append(String.format("Credits Behind: %.1f\n", creditsBehind));
                    }

                    if ("Retention Risk".equals(status)) {
                        tooltipText.append("\n⚠️ URGENT: Immediate intervention required");
                    } else if ("At Risk".equals(status)) {
                        tooltipText.append("\n⚠️ Monitor progress closely");
                    }

                    Tooltip tooltip = new Tooltip(tooltipText.toString());
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void loadStudents() {
        try {
            log.debug("Loading students from database...");

            // Use findAllWithEnrolledCourses() to eagerly load courses for course count display
            List<Student> students = studentRepository.findAllWithEnrolledCourses();
            log.info("Found {} students", students.size());

            studentsList.clear();
            studentsList.addAll(students);
            studentsTable.setItems(studentsList);

            if (recordCountLabel != null) {
                recordCountLabel.setText("Total: " + students.size());
            }

            log.debug("Students loaded and displayed");

        } catch (Exception e) {
            log.error("EXCEPTION in loadStudents()", e);
        }
    }

    // ========== EVENT HANDLERS ==========

    @FXML
    private void handleAddStudent() {
        log.info("Add student clicked");
        showStudentDialog(null); // null means "add new"
    }

    /**
     * Open Medical Record Dialog for selected student
     */
    @FXML
    private void handleOpenMedicalRecord() {
        log.info("Medical Record button clicked");

        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            showWarning("No Selection", "Please select a student first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MedicalRecordDialog.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);

            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Medical Record - " + selectedStudent.getFirstName() + " " + selectedStudent.getLastName());

            MedicalRecordDialogController controller = loader.getController();
            controller.setStudent(selectedStudent);

            Optional<ButtonType> result = dialog.showAndWait();
            result.filter(btn -> btn.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .ifPresent(btn -> {
                    log.info("Medical record saved for student: {} {}",
                            selectedStudent.getFirstName(), selectedStudent.getLastName());
                    handleRefresh(); // Refresh to show updated medical indicators
                });
        } catch (Exception e) {
            log.error("Error opening medical record dialog", e);
            showError("Error", "Failed to open medical record dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Print QR Code for selected student
     */
    @FXML
    private void handlePrintQRCode() {
        log.info("Print QR Code button clicked");

        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            showWarning("No Selection", "Please select a student first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/StudentQRCodeDialog.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);

            VBox dialogContent = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("QR Code - " + selectedStudent.getFirstName() + " " + selectedStudent.getLastName());
            dialogStage.setScene(new javafx.scene.Scene(dialogContent));
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(studentsTable.getScene().getWindow());

            StudentQRCodeDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setStudent(selectedStudent);

            dialogStage.showAndWait();

            log.info("QR code dialog closed for student: {} {}",
                    selectedStudent.getFirstName(), selectedStudent.getLastName());

        } catch (Exception e) {
            log.error("Error opening QR code dialog", e);
            showError("Error", "Failed to open QR code dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Upload photo for selected student
     */
    @FXML
    private void handleUploadPhoto() {
        log.info("Upload Photo button clicked");

        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            showWarning("No Selection", "Please select a student first.");
            return;
        }

        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Student Photo");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"),
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File selectedFile = fileChooser.showOpenDialog(studentsTable.getScene().getWindow());

            if (selectedFile != null) {
                // Check file size (5MB limit)
                long fileSizeInMB = selectedFile.length() / (1024 * 1024);
                if (fileSizeInMB > 5) {
                    showError("File Too Large", "Photo must be less than 5MB. Selected file is " + fileSizeInMB + "MB.");
                    return;
                }

                // Create photo directory if it doesn't exist
                String photoDir = "./data/student-photos";
                java.nio.file.Path photoDirPath = java.nio.file.Paths.get(photoDir);
                if (!java.nio.file.Files.exists(photoDirPath)) {
                    java.nio.file.Files.createDirectories(photoDirPath);
                }

                // Get file extension
                String fileName = selectedFile.getName();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

                // Copy file to student photos directory
                String newFileName = selectedStudent.getStudentId() + "." + extension;
                java.nio.file.Path targetPath = photoDirPath.resolve(newFileName);
                java.nio.file.Files.copy(selectedFile.toPath(), targetPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Update student record
                String relativePath = photoDir + "/" + newFileName;
                selectedStudent.setPhotoPath(relativePath);
                studentRepository.save(selectedStudent);

                log.info("Photo uploaded for student: {} -> {}",
                    selectedStudent.getStudentId(), relativePath);

                showInfo("Success", "Photo uploaded successfully!");
                handleRefresh();
            }

        } catch (Exception e) {
            log.error("Error uploading photo", e);
            showError("Error", "Failed to upload photo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ NEW: Edit existing student
     */
    private void handleEditStudent(Student student) {
        log.info("Edit student: {} {}", student.getFirstName(), student.getLastName());
        showStudentDialog(student); // pass existing student to edit
    }

    /**
     * ✅ NEW: Delete student with confirmation
     */
    private void handleDeleteStudent(Student student) {
        log.info("Delete student requested: {} {}", student.getFirstName(), student.getLastName());

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Student");
        confirmAlert.setContentText("Are you sure you want to delete student:\n" +
                                   student.getFirstName() + " " + student.getLastName() +
                                   " (" + student.getStudentId() + ")?\n\n" +
                                   "This action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                studentRepository.delete(student);
                log.info("Student deleted: {} {} (ID: {})",
                        student.getFirstName(), student.getLastName(), student.getStudentId());

                loadStudents(); // Reload table

                showInfo("Success", "Student '" + student.getFirstName() + " " +
                        student.getLastName() + "' deleted successfully!");

            } catch (Exception e) {
                log.error("Error deleting student", e);
                showError("Error", "Failed to delete student: " + e.getMessage());
            }
        }
    }

    /**
     * ✅ ENHANCED: Comprehensive student dialog for Add/Edit with all fields
     */
    private void showStudentDialog(Student existingStudent) {
        boolean isEdit = (existingStudent != null);

        // Create dialog
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Student" : "Add New Student");
        dialog.setHeaderText(isEdit ? "Update student information" : "Enter student information");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create tabbed form for organized input
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // TAB 1: Basic Information
        GridPane basicGrid = createBasicInfoGrid(existingStudent);
        Tab basicTab = new Tab("Basic Info", basicGrid);

        // TAB 2: Demographics (NEW - Phase 2)
        GridPane demographicsGrid = createDemographicsGrid(existingStudent);
        Tab demographicsTab = new Tab("Demographics", demographicsGrid);

        // TAB 3: Home Address & Residency (NEW - Phase 2)
        GridPane addressGrid = createAddressGrid(existingStudent);
        Tab addressTab = new Tab("Address", addressGrid);

        // TAB 4: Special Circumstances (NEW - Phase 2)
        GridPane specialCircumstancesGrid = createSpecialCircumstancesGrid(existingStudent);
        Tab specialCircumstancesTab = new Tab("Special Circumstances", specialCircumstancesGrid);

        // TAB 5: Academic Details
        GridPane academicGrid = createAcademicInfoGrid(existingStudent);
        Tab academicTab = new Tab("Academic", academicGrid);

        // TAB 6: Special Education & Medical
        GridPane specialEdGrid = createSpecialEdGrid(existingStudent);
        Tab specialEdTab = new Tab("Special Ed & Medical", specialEdGrid);

        // TAB 7: Parents/Guardians (NEW - TableView)
        VBox parentsVBox = createParentsGuardiansTab(existingStudent);
        Tab parentsTab = new Tab("Parents/Guardians", parentsVBox);

        // TAB 8: Emergency Contacts (NEW - TableView with Priority)
        VBox emergencyContactsVBox = createEmergencyContactsTab(existingStudent);
        Tab emergencyContactsTab = new Tab("Emergency Contacts", emergencyContactsVBox);

        tabPane.getTabs().addAll(basicTab, demographicsTab, addressTab, specialCircumstancesTab, academicTab, specialEdTab, parentsTab, emergencyContactsTab);

        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(700);

        // Enable/disable save button based on required fields
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        TextField studentIdField = (TextField) basicGrid.lookup("#studentIdField");
        TextField firstNameField = (TextField) basicGrid.lookup("#firstNameField");
        TextField lastNameField = (TextField) basicGrid.lookup("#lastNameField");
        ComboBox<String> gradeLevelCombo = (ComboBox<String>) basicGrid.lookup("#gradeLevelCombo");

        // Validation
        Runnable validateForm = () -> {
            boolean valid = !studentIdField.getText().trim().isEmpty() &&
                          !firstNameField.getText().trim().isEmpty() &&
                          !lastNameField.getText().trim().isEmpty() &&
                          gradeLevelCombo.getValue() != null;
            saveButton.setDisable(!valid);
        };

        studentIdField.textProperty().addListener((obs, old, val) -> validateForm.run());
        firstNameField.textProperty().addListener((obs, old, val) -> validateForm.run());
        lastNameField.textProperty().addListener((obs, old, val) -> validateForm.run());
        gradeLevelCombo.valueProperty().addListener((obs, old, val) -> validateForm.run());

        // Initial validation
        validateForm.run();

        // Request focus
        Platform.runLater(() -> studentIdField.requestFocus());

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Student student = isEdit ? existingStudent : new Student();

                // Basic Info
                student.setStudentId(studentIdField.getText().trim());
                student.setFirstName(firstNameField.getText().trim());
                student.setLastName(lastNameField.getText().trim());
                student.setGradeLevel(gradeLevelCombo.getValue());

                TextField emailField = (TextField) basicGrid.lookup("#emailField");
                student.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());

                CheckBox activeCheckBox = (CheckBox) basicGrid.lookup("#activeCheckBox");
                student.setActive(activeCheckBox.isSelected());

                // Academic Details
                CheckBox isSeniorCheckBox = (CheckBox) academicGrid.lookup("#isSeniorCheckBox");
                student.setIsSenior(isSeniorCheckBox.isSelected());

                TextField priorityWeightField = (TextField) academicGrid.lookup("#priorityWeightField");
                try {
                    student.setPriorityWeight(Integer.parseInt(priorityWeightField.getText().trim()));
                } catch (NumberFormatException e) {
                    student.setPriorityWeight(0);
                }

                TextField gradYearField = (TextField) academicGrid.lookup("#gradYearField");
                try {
                    if (!gradYearField.getText().trim().isEmpty()) {
                        student.setGraduationYear(Integer.parseInt(gradYearField.getText().trim()));
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }

                // Special Education
                CheckBox hasIEPCheckBox = (CheckBox) specialEdGrid.lookup("#hasIEPCheckBox");
                student.setHasIEP(hasIEPCheckBox.isSelected());

                CheckBox has504CheckBox = (CheckBox) specialEdGrid.lookup("#has504CheckBox");
                student.setHas504Plan(has504CheckBox.isSelected());

                CheckBox isGiftedCheckBox = (CheckBox) specialEdGrid.lookup("#isGiftedCheckBox");
                student.setIsGifted(isGiftedCheckBox.isSelected());

                TextArea accomNotesArea = (TextArea) specialEdGrid.lookup("#accomNotesArea");
                student.setAccommodationNotes(accomNotesArea.getText().trim().isEmpty() ? null : accomNotesArea.getText().trim());

                TextArea medConditionsArea = (TextArea) specialEdGrid.lookup("#medConditionsArea");
                student.setMedicalConditions(medConditionsArea.getText().trim().isEmpty() ? null : medConditionsArea.getText().trim());

                // NOTE: Contact information is now handled via TableViews below (parents + emergency contacts)

                // ========================================================================
                // NEW FIELDS - Phase 2: Demographics
                // ========================================================================
                DatePicker dateOfBirthPicker = (DatePicker) demographicsGrid.lookup("#dateOfBirthPicker");
                student.setDateOfBirth(dateOfBirthPicker.getValue());

                ComboBox<String> genderCombo = (ComboBox<String>) demographicsGrid.lookup("#genderCombo");
                student.setGender(genderCombo.getValue());

                ComboBox<String> ethnicityCombo = (ComboBox<String>) demographicsGrid.lookup("#ethnicityCombo");
                student.setEthnicity(ethnicityCombo.getValue());

                ComboBox<String> raceCombo = (ComboBox<String>) demographicsGrid.lookup("#raceCombo");
                student.setRace(raceCombo.getValue());

                ComboBox<String> languageCombo = (ComboBox<String>) demographicsGrid.lookup("#languageCombo");
                student.setPreferredLanguage(languageCombo.getValue());

                CheckBox isELLCheckBox = (CheckBox) demographicsGrid.lookup("#isELLCheckBox");
                student.setIsEnglishLearner(isELLCheckBox.isSelected());

                TextField birthCityField = (TextField) demographicsGrid.lookup("#birthCityField");
                student.setBirthCity(birthCityField.getText().trim().isEmpty() ? null : birthCityField.getText().trim());

                TextField birthStateField = (TextField) demographicsGrid.lookup("#birthStateField");
                student.setBirthState(birthStateField.getText().trim().isEmpty() ? null : birthStateField.getText().trim());

                TextField birthCountryField = (TextField) demographicsGrid.lookup("#birthCountryField");
                student.setBirthCountry(birthCountryField.getText().trim().isEmpty() ? null : birthCountryField.getText().trim());

                // ========================================================================
                // NEW FIELDS - Phase 2: Home Address & Residency
                // ========================================================================
                TextField homeStreetField = (TextField) addressGrid.lookup("#homeStreetField");
                student.setHomeStreetAddress(homeStreetField.getText().trim().isEmpty() ? null : homeStreetField.getText().trim());

                TextField homeCityField = (TextField) addressGrid.lookup("#homeCityField");
                student.setHomeCity(homeCityField.getText().trim().isEmpty() ? null : homeCityField.getText().trim());

                TextField homeStateField = (TextField) addressGrid.lookup("#homeStateField");
                student.setHomeState(homeStateField.getText().trim().isEmpty() ? null : homeStateField.getText().trim());

                TextField homeZipField = (TextField) addressGrid.lookup("#homeZipField");
                student.setHomeZipCode(homeZipField.getText().trim().isEmpty() ? null : homeZipField.getText().trim());

                TextField countyField = (TextField) addressGrid.lookup("#countyField");
                student.setHomeCounty(countyField.getText().trim().isEmpty() ? null : countyField.getText().trim());

                // Mailing Address
                TextField mailingStreetField = (TextField) addressGrid.lookup("#mailingStreetField");
                student.setMailingStreetAddress(mailingStreetField.getText().trim().isEmpty() ? null : mailingStreetField.getText().trim());

                TextField mailingCityField = (TextField) addressGrid.lookup("#mailingCityField");
                student.setMailingCity(mailingCityField.getText().trim().isEmpty() ? null : mailingCityField.getText().trim());

                TextField mailingStateField = (TextField) addressGrid.lookup("#mailingStateField");
                student.setMailingState(mailingStateField.getText().trim().isEmpty() ? null : mailingStateField.getText().trim());

                TextField mailingZipField = (TextField) addressGrid.lookup("#mailingZipField");
                student.setMailingZipCode(mailingZipField.getText().trim().isEmpty() ? null : mailingZipField.getText().trim());

                // Residency Verification
                CheckBox residencyVerifiedCheckBox = (CheckBox) addressGrid.lookup("#residencyVerifiedCheckBox");
                student.setResidencyVerified(residencyVerifiedCheckBox.isSelected());

                DatePicker residencyVerifiedDatePicker = (DatePicker) addressGrid.lookup("#residencyVerifiedDatePicker");
                student.setResidencyVerifiedDate(residencyVerifiedDatePicker.getValue());

                // ========================================================================
                // NEW FIELDS - Phase 2: Special Circumstances
                // ========================================================================

                // Federal Compliance
                CheckBox isFosterCareCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isFosterCareCheckBox");
                student.setIsFosterCare(isFosterCareCheckBox.isSelected());

                TextField fosterAgencyField = (TextField) specialCircumstancesGrid.lookup("#fosterAgencyField");
                student.setFosterCareAgency(fosterAgencyField.getText().trim().isEmpty() ? null : fosterAgencyField.getText().trim());

                TextField fosterCaseworkerField = (TextField) specialCircumstancesGrid.lookup("#fosterCaseworkerField");
                student.setFosterCaseWorkerName(fosterCaseworkerField.getText().trim().isEmpty() ? null : fosterCaseworkerField.getText().trim());

                CheckBox isHomelessCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isHomelessCheckBox");
                student.setIsHomeless(isHomelessCheckBox.isSelected());

                TextField homelessShelterField = (TextField) specialCircumstancesGrid.lookup("#homelessShelterField");
                student.setHomelessSituationType(homelessShelterField.getText().trim().isEmpty() ? null : homelessShelterField.getText().trim());

                CheckBox isOrphanCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isOrphanCheckBox");
                student.setIsOrphan(isOrphanCheckBox.isSelected());

                CheckBox isWardOfCourtCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isWardOfCourtCheckBox");
                student.setIsWardOfCourt(isWardOfCourtCheckBox.isSelected());

                CheckBox isRefugeeCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isRefugeeCheckBox");
                student.setIsRefugeeAsylee(isRefugeeCheckBox.isSelected());

                CheckBox isUnaccompaniedYouthCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isUnaccompaniedYouthCheckBox");
                student.setIsUnaccompaniedYouth(isUnaccompaniedYouthCheckBox.isSelected());

                // Family Situation
                CheckBox isMilitaryFamilyCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isMilitaryFamilyCheckBox");
                student.setIsMilitaryFamily(isMilitaryFamilyCheckBox.isSelected());

                CheckBox isMigrantFamilyCheckBox = (CheckBox) specialCircumstancesGrid.lookup("#isMigrantFamilyCheckBox");
                student.setIsMigrantFamily(isMigrantFamilyCheckBox.isSelected());

                TextField custodyArrangementField = (TextField) specialCircumstancesGrid.lookup("#custodyArrangementField");
                student.setCustodyArrangement(custodyArrangementField.getText().trim().isEmpty() ? null : custodyArrangementField.getText().trim());

                // Previous School
                TextField previousSchoolNameField = (TextField) specialCircumstancesGrid.lookup("#previousSchoolNameField");
                student.setPreviousSchoolName(previousSchoolNameField.getText().trim().isEmpty() ? null : previousSchoolNameField.getText().trim());

                TextField previousSchoolAddressField = (TextField) specialCircumstancesGrid.lookup("#previousSchoolAddressField");
                student.setPreviousSchoolAddress(previousSchoolAddressField.getText().trim().isEmpty() ? null : previousSchoolAddressField.getText().trim());

                TextField previousSchoolPhoneField = (TextField) specialCircumstancesGrid.lookup("#previousSchoolPhoneField");
                student.setPreviousSchoolPhone(previousSchoolPhoneField.getText().trim().isEmpty() ? null : previousSchoolPhoneField.getText().trim());

                // NEW Phase 2: Save Parents/Guardians from TableView
                TableView<ParentGuardian> parentsTable = (TableView<ParentGuardian>) parentsVBox.lookup("#parentsTable");
                if (parentsTable != null) {
                    // Clear existing parents and add all from table
                    if (student.getParents() != null) {
                        student.getParents().clear();
                    } else {
                        student.setParents(new java.util.ArrayList<>());
                    }
                    for (ParentGuardian parent : parentsTable.getItems()) {
                        parent.setStudent(student);  // Ensure bidirectional relationship
                        student.getParents().add(parent);
                    }
                }

                // NEW Phase 2: Save Emergency Contacts from TableView
                TableView<EmergencyContact> contactsTable = (TableView<EmergencyContact>) emergencyContactsVBox.lookup("#emergencyContactsTable");
                if (contactsTable != null) {
                    // Clear existing contacts and add all from table
                    if (student.getEmergencyContacts() != null) {
                        student.getEmergencyContacts().clear();
                    } else {
                        student.setEmergencyContacts(new java.util.ArrayList<>());
                    }
                    for (EmergencyContact contact : contactsTable.getItems()) {
                        contact.setStudent(student);  // Ensure bidirectional relationship
                        student.getEmergencyContacts().add(contact);
                    }
                }

                return            student;
            }
            return            null;
        });

        // Show dialog and save
        dialog.showAndWait().ifPresent(student -> {
            try {
                // Check for duplicate student ID (only when adding or changing ID)
                if (!isEdit || !student.getStudentId().equals(existingStudent.getStudentId())) {
                    if (studentRepository.findByStudentId(student.getStudentId()).isPresent()) {
                        showError("Duplicate Student ID", "A student with ID '" + student.getStudentId() + "' already exists.");
                        return;
                    }
                }

                Student saved = studentRepository.save(student);
                log.info("{} student: {} {} (ID: {})",
                        isEdit ? "Updated" : "Added",
                        saved.getFirstName(), saved.getLastName(), saved.getStudentId());

                // Reload students
                loadStudents();

                // Show success message
                showInfo("Success", "Student '" + saved.getFirstName() + " " + saved.getLastName() +
                        "' " + (isEdit ? "updated" : "added") + " successfully!");

            } catch (Exception e) {
                log.error("Error saving student", e);
                showError("Error", "Failed to save student: " + e.getMessage());
            }
        });
    }

    /**
     * ✅ NEW: Create basic information grid
     */
    private GridPane createBasicInfoGrid(Student student) {
        boolean isEdit = (student != null);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField studentIdField = new TextField();
        studentIdField.setId("studentIdField");
        studentIdField.setPromptText("Student ID (e.g., S2024001)");
        if (student != null) studentIdField.setText(student.getStudentId());

        TextField firstNameField = new TextField();
        firstNameField.setId("firstNameField");
        firstNameField.setPromptText("First Name");
        if (student != null) firstNameField.setText(student.getFirstName());

        TextField lastNameField = new TextField();
        lastNameField.setId("lastNameField");
        lastNameField.setPromptText("Last Name");
        if (student != null) lastNameField.setText(student.getLastName());

        ComboBox<String> gradeLevelCombo = new ComboBox<>();
        gradeLevelCombo.setId("gradeLevelCombo");
        gradeLevelCombo.getItems().addAll("K", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        gradeLevelCombo.setPromptText("Select Grade");
        if (student != null) gradeLevelCombo.setValue(student.getGradeLevel());

        TextField emailField = new TextField();
        emailField.setId("emailField");
        emailField.setPromptText("Email (optional)");
        if (student != null && student.getEmail() != null) emailField.setText(student.getEmail());

        CheckBox activeCheckBox = new CheckBox("Active");
        activeCheckBox.setId("activeCheckBox");
        activeCheckBox.setSelected(student == null || student.getActive());

        // ========================================================================
        // AUTO-GENERATION SETUP
        // ========================================================================

        // Auto-generate student ID for new students
        if (!isEdit && districtSettingsService != null) {
            Platform.runLater(() -> {
                try {
                    // Get graduation year from grade level
                    String gradeLevel = gradeLevelCombo.getValue();
                    if (gradeLevel != null && !gradeLevel.isEmpty()) {
                        int gradYear = calculateGraduationYear(gradeLevel);

                        String generatedId = districtSettingsService.generateNextStudentId(gradYear);
                        if (generatedId != null && !generatedId.isEmpty()) {
                            studentIdField.setText(generatedId);
                            studentIdField.getStyleClass().add("primary-text"); // Blue = auto-generated
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to generate student ID", e);
                }
            });
        }

        // Update student ID when grade level changes (for new students only)
        if (!isEdit && districtSettingsService != null) {
            gradeLevelCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isEmpty()) {
                    try {
                        int gradYear = calculateGraduationYear(newVal);
                        String generatedId = districtSettingsService.generateNextStudentId(gradYear);
                        if (generatedId != null && !generatedId.isEmpty()) {
                            studentIdField.setText(generatedId);
                            studentIdField.getStyleClass().removeAll("primary-text");
                            studentIdField.getStyleClass().add("primary-text");
                        }
                    } catch (Exception e) {
                        log.error("Failed to generate student ID on grade change", e);
                    }
                }
            });
        }

        // Reset text color when user manually edits student ID
        studentIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal) && oldVal != null && !oldVal.isEmpty()) {
                studentIdField.getStyleClass().removeAll("primary-text"); // User edited - normal color
            }
        });

        // Setup email auto-generation based on name changes
        if (districtSettingsService != null) {
            javafx.beans.value.ChangeListener<String> nameChangeListener = (obs, oldVal, newVal) -> {
                String firstName = firstNameField.getText();
                String lastName = lastNameField.getText();
                String gradeLevel = gradeLevelCombo.getValue();

                if (firstName != null && !firstName.trim().isEmpty() &&
                    lastName != null && !lastName.trim().isEmpty() &&
                    gradeLevel != null) {

                    // Only auto-generate for new students or when email is empty
                    if (!isEdit || emailField.getText() == null || emailField.getText().trim().isEmpty()) {
                        try {
                            int gradYear = calculateGraduationYear(gradeLevel);
                            String generatedEmail = districtSettingsService.generateStudentEmail(
                                firstName, lastName, gradYear, studentIdField.getText());

                            if (generatedEmail != null && !generatedEmail.isEmpty()) {
                                emailField.setText(generatedEmail);
                                emailField.getStyleClass().add("primary-text"); // Blue = auto-generated
                            }
                        } catch (Exception e) {
                            log.error("Failed to generate email", e);
                        }
                    }
                }
            };

            firstNameField.textProperty().addListener(nameChangeListener);
            lastNameField.textProperty().addListener(nameChangeListener);
            gradeLevelCombo.valueProperty().addListener((obs, oldVal, newVal) -> nameChangeListener.changed(obs, null, newVal));

            // Reset text color when user manually edits email
            emailField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal) && oldVal != null && !oldVal.isEmpty()) {
                    emailField.getStyleClass().removeAll("primary-text"); // User edited - normal color
                }
            });
        }

        grid.add(new Label("* Student ID:"), 0, 0);
        grid.add(studentIdField, 1, 0);
        grid.add(new Label("* First Name:"), 0, 1);
        grid.add(firstNameField, 1, 1);
        grid.add(new Label("* Last Name:"), 0, 2);
        grid.add(lastNameField, 1, 2);
        grid.add(new Label("* Grade Level:"), 0, 3);
        grid.add(gradeLevelCombo, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(emailField, 1, 4);
        grid.add(activeCheckBox, 1, 5);

        return            grid;
    }

    /**
     * Calculate graduation year based on grade level
     * Example: Grade 9 in 2025 → graduates 2029 (9th + 4 years)
     */
    private int calculateGraduationYear(String gradeLevel) {
        int currentYear = java.time.Year.now().getValue();

        return switch (gradeLevel) {
            case "K" -> currentYear + 13; // Kindergarten + 13 years
            case "1" -> currentYear + 12;
            case "2" -> currentYear + 11;
            case "3" -> currentYear + 10;
            case "4" -> currentYear + 9;
            case "5" -> currentYear + 8;
            case "6" -> currentYear + 7;
            case "7" -> currentYear + 6;
            case "8" -> currentYear + 5;
            case "9" -> currentYear + 4;
            case "10" -> currentYear + 3;
            case "11" -> currentYear + 2;
            case "12" -> currentYear + 1;
            default -> currentYear + 4; // Default to 4 years
        };
    }

    /**
     * ✅ NEW: Create academic information grid
     */
    private GridPane createAcademicInfoGrid(Student student) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        CheckBox isSeniorCheckBox = new CheckBox("Is Senior");
        isSeniorCheckBox.setId("isSeniorCheckBox");
        if (student != null && student.getIsSenior() != null) isSeniorCheckBox.setSelected(student.getIsSenior());

        TextField priorityWeightField = new TextField();
        priorityWeightField.setId("priorityWeightField");
        priorityWeightField.setPromptText("Priority Weight (0-10)");
        if (student != null) priorityWeightField.setText(String.valueOf(student.getPriorityWeight()));

        TextField gradYearField = new TextField();
        gradYearField.setId("gradYearField");
        gradYearField.setPromptText("Expected Graduation Year (e.g., 2027)");
        if (student != null && student.getGraduationYear() != null) {
            gradYearField.setText(String.valueOf(student.getGraduationYear()));
        }

        grid.add(isSeniorCheckBox, 0, 0, 2, 1);
        grid.add(new Label("Priority Weight:"), 0, 1);
        grid.add(priorityWeightField, 1, 1);
        grid.add(new Label("Graduation Year:"), 0, 2);
        grid.add(gradYearField, 1, 2);
        grid.add(new Label("(Higher priority gets better scheduling)"), 0, 3, 2, 1);

        return            grid;
    }

    /**
     * ✅ NEW: Create special education and medical grid
     */
    private GridPane createSpecialEdGrid(Student student) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        CheckBox hasIEPCheckBox = new CheckBox("Has IEP (Individual Education Plan)");
        hasIEPCheckBox.setId("hasIEPCheckBox");
        if (student != null && student.getHasIEP() != null) hasIEPCheckBox.setSelected(student.getHasIEP());

        CheckBox has504CheckBox = new CheckBox("Has 504 Plan");
        has504CheckBox.setId("has504CheckBox");
        if (student != null && student.getHas504Plan() != null) has504CheckBox.setSelected(student.getHas504Plan());

        CheckBox isGiftedCheckBox = new CheckBox("Gifted & Talented");
        isGiftedCheckBox.setId("isGiftedCheckBox");
        if (student != null && student.getIsGifted() != null) isGiftedCheckBox.setSelected(student.getIsGifted());

        TextArea accomNotesArea = new TextArea();
        accomNotesArea.setId("accomNotesArea");
        accomNotesArea.setPromptText("Accommodation notes...");
        accomNotesArea.setPrefRowCount(3);
        accomNotesArea.setWrapText(true);
        if (student != null && student.getAccommodationNotes() != null) {
            accomNotesArea.setText(student.getAccommodationNotes());
        }

        TextArea medConditionsArea = new TextArea();
        medConditionsArea.setId("medConditionsArea");
        medConditionsArea.setPromptText("Medical conditions, allergies, medications...");
        medConditionsArea.setPrefRowCount(3);
        medConditionsArea.setWrapText(true);
        if (student != null && student.getMedicalConditions() != null) {
            medConditionsArea.setText(student.getMedicalConditions());
        }

        grid.add(hasIEPCheckBox, 0, 0, 2, 1);
        grid.add(has504CheckBox, 0, 1, 2, 1);
        grid.add(isGiftedCheckBox, 0, 2, 2, 1);
        grid.add(new Label("Accommodation Notes:"), 0, 3);
        grid.add(accomNotesArea, 0, 4, 2, 1);
        grid.add(new Label("Medical Conditions:"), 0, 5);
        grid.add(medConditionsArea, 0, 6, 2, 1);

        return            grid;
    }

    /**
     * ✅ NEW: Create contact information grid
     */
    private GridPane createContactGrid(Student student) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField emergencyContactField = new TextField();
        emergencyContactField.setId("emergencyContactField");
        emergencyContactField.setPromptText("Emergency Contact Name");
        if (student != null && student.getEmergencyContact() != null) {
            emergencyContactField.setText(student.getEmergencyContact());
        }

        TextField emergencyPhoneField = new TextField();
        emergencyPhoneField.setId("emergencyPhoneField");
        emergencyPhoneField.setPromptText("Emergency Phone Number");
        if (student != null && student.getEmergencyPhone() != null) {
            emergencyPhoneField.setText(student.getEmergencyPhone());
        }

        grid.add(new Label("Emergency Contact:"), 0, 0);
        grid.add(emergencyContactField, 1, 0);
        grid.add(new Label("Emergency Phone:"), 0, 1);
        grid.add(emergencyPhoneField, 1, 1);
        grid.add(new Label("(Person to contact in case of emergency)"), 0, 2, 2, 1);

        return            grid;
    }

    /**
     * ✅ NEW: Create Parents/Guardians tab with TableView (Phase 2 - K-12 Enrollment Enhancement)
     * Allows managing multiple parents/guardians with full details
     */
    private VBox createParentsGuardiansTab(Student student) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));

        // Title
        Label titleLabel = new Label("Parents / Guardians");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // TableView for parents/guardians
        TableView<ParentGuardian> parentsTable = new TableView<>();
        parentsTable.setId("parentsTable");
        parentsTable.setPlaceholder(new Label("No parents/guardians added yet. Click 'Add Parent' to begin."));

        // Columns
        TableColumn<ParentGuardian, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(150);
        nameCol.setCellValueFactory(cellData -> {
            ParentGuardian parent = cellData.getValue();
            String fullName = (parent.getFirstName() != null ? parent.getFirstName() : "") + " " +
                              (parent.getLastName() != null ? parent.getLastName() : "");
            return new javafx.beans.property.SimpleStringProperty(fullName.trim());
        });

        TableColumn<ParentGuardian, String> relationshipCol = new TableColumn<>("Relationship");
        relationshipCol.setPrefWidth(100);
        relationshipCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getRelationship() != null ? cellData.getValue().getRelationship() : ""));

        TableColumn<ParentGuardian, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setPrefWidth(120);
        phoneCol.setCellValueFactory(cellData -> {
            ParentGuardian parent = cellData.getValue();
            String phone = parent.getCellPhone() != null ? parent.getCellPhone() :
                          (parent.getHomePhone() != null ? parent.getHomePhone() : "");
            return new javafx.beans.property.SimpleStringProperty(phone);
        });

        TableColumn<ParentGuardian, String> emailCol = new TableColumn<>("Email");
        emailCol.setPrefWidth(150);
        emailCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getEmail() != null ? cellData.getValue().getEmail() : ""));

        TableColumn<ParentGuardian, String> custodyCol = new TableColumn<>("Custody");
        custodyCol.setPrefWidth(80);
        custodyCol.setCellValueFactory(cellData -> {
            boolean hasCustody = cellData.getValue().getHasLegalCustody() != null &&
                                 cellData.getValue().getHasLegalCustody();
            return new javafx.beans.property.SimpleStringProperty(hasCustody ? "Yes" : "No");
        });

        parentsTable.getColumns().addAll(nameCol, relationshipCol, phoneCol, emailCol, custodyCol);

        // Load existing parents if editing
        if (student != null && student.getParents() != null) {
            parentsTable.getItems().addAll(student.getParents());
        }

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button addParentBtn = new Button("Add Parent/Guardian");
        addParentBtn.setOnAction(e -> {
            ParentGuardian newParent = showParentGuardianDialog(null, student);
            if (newParent != null) {
                parentsTable.getItems().add(newParent);
            }
        });

        Button editParentBtn = new Button("Edit Selected");
        editParentBtn.setDisable(true);
        editParentBtn.setOnAction(e -> {
            ParentGuardian selected = parentsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ParentGuardian edited = showParentGuardianDialog(selected, student);
                if (edited != null) {
                    int index = parentsTable.getItems().indexOf(selected);
                    parentsTable.getItems().set(index, edited);
                }
            }
        });

        Button deleteParentBtn = new Button("Delete Selected");
        deleteParentBtn.setDisable(true);
        deleteParentBtn.setOnAction(e -> {
            ParentGuardian selected = parentsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Parent/Guardian");
                confirm.setHeaderText("Are you sure?");
                confirm.setContentText("This will remove " + selected.getFirstName() + " " +
                                      selected.getLastName() + " from the student's records.");
                if (confirm.showAndWait().get() == ButtonType.OK) {
                    parentsTable.getItems().remove(selected);
                }
            }
        });

        // Enable/disable edit and delete buttons based on selection
        parentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selected = newVal != null;
            editParentBtn.setDisable(!selected);
            deleteParentBtn.setDisable(!selected);
        });

        buttonBox.getChildren().addAll(addParentBtn, editParentBtn, deleteParentBtn);

        // Help text
        Label helpLabel = new Label("Tip: Add all parents/guardians for comprehensive student records. " +
                                   "Mark legal custody and pickup authorization as needed.");
        helpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
        helpLabel.setWrapText(true);

        vbox.getChildren().addAll(titleLabel, parentsTable, buttonBox, helpLabel);
        VBox.setVgrow(parentsTable, javafx.scene.layout.Priority.ALWAYS);

        return vbox;
    }

    /**
     * ✅ NEW: Create Emergency Contacts tab with TableView and Priority Ordering (Phase 2 - K-12 Enrollment Enhancement)
     * Allows managing 2-5 emergency contacts with priority ordering
     */
    private VBox createEmergencyContactsTab(Student student) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));

        // Title
        Label titleLabel = new Label("Emergency Contacts (Priority Order 1-5)");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // TableView for emergency contacts
        TableView<EmergencyContact> contactsTable = new TableView<>();
        contactsTable.setId("emergencyContactsTable");
        contactsTable.setPlaceholder(new Label("No emergency contacts added yet. REQUIRED: Minimum 2 contacts."));

        // Columns
        TableColumn<EmergencyContact, Integer> priorityCol = new TableColumn<>("Priority");
        priorityCol.setPrefWidth(60);
        priorityCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getPriorityOrder() != null ? cellData.getValue().getPriorityOrder() : 99).asObject());

        TableColumn<EmergencyContact, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(140);
        nameCol.setCellValueFactory(cellData -> {
            EmergencyContact contact = cellData.getValue();
            String fullName = (contact.getFirstName() != null ? contact.getFirstName() : "") + " " +
                              (contact.getLastName() != null ? contact.getLastName() : "");
            return new javafx.beans.property.SimpleStringProperty(fullName.trim());
        });

        TableColumn<EmergencyContact, String> relationshipCol = new TableColumn<>("Relationship");
        relationshipCol.setPrefWidth(90);
        relationshipCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getRelationship() != null ? cellData.getValue().getRelationship() : ""));

        TableColumn<EmergencyContact, String> phoneCol = new TableColumn<>("Primary Phone");
        phoneCol.setPrefWidth(110);
        phoneCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPrimaryPhone() != null ? cellData.getValue().getPrimaryPhone() : ""));

        TableColumn<EmergencyContact, String> secondaryPhoneCol = new TableColumn<>("Secondary Phone");
        secondaryPhoneCol.setPrefWidth(110);
        secondaryPhoneCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSecondaryPhone() != null ? cellData.getValue().getSecondaryPhone() : ""));

        TableColumn<EmergencyContact, String> pickupCol = new TableColumn<>("Pickup?");
        pickupCol.setPrefWidth(60);
        pickupCol.setCellValueFactory(cellData -> {
            boolean authorized = cellData.getValue().getAuthorizedToPickUp() != null &&
                                cellData.getValue().getAuthorizedToPickUp();
            return new javafx.beans.property.SimpleStringProperty(authorized ? "Yes" : "No");
        });

        contactsTable.getColumns().addAll(priorityCol, nameCol, relationshipCol, phoneCol, secondaryPhoneCol, pickupCol);

        // Load existing contacts if editing
        if (student != null && student.getEmergencyContacts() != null) {
            contactsTable.getItems().addAll(student.getEmergencyContacts());
            // Sort by priority
            contactsTable.getItems().sort((c1, c2) -> {
                int p1 = c1.getPriorityOrder() != null ? c1.getPriorityOrder() : 99;
                int p2 = c2.getPriorityOrder() != null ? c2.getPriorityOrder() : 99;
                return Integer.compare(p1, p2);
            });
        }

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button addContactBtn = new Button("Add Contact");
        addContactBtn.setOnAction(e -> {
            EmergencyContact newContact = showEmergencyContactDialog(null, student, contactsTable.getItems().size() + 1);
            if (newContact != null) {
                contactsTable.getItems().add(newContact);
                contactsTable.sort();
            }
        });

        Button editContactBtn = new Button("Edit Selected");
        editContactBtn.setDisable(true);
        editContactBtn.setOnAction(e -> {
            EmergencyContact selected = contactsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                EmergencyContact edited = showEmergencyContactDialog(selected, student, selected.getPriorityOrder());
                if (edited != null) {
                    int index = contactsTable.getItems().indexOf(selected);
                    contactsTable.getItems().set(index, edited);
                    contactsTable.sort();
                }
            }
        });

        Button deleteContactBtn = new Button("Delete Selected");
        deleteContactBtn.setDisable(true);
        deleteContactBtn.setOnAction(e -> {
            EmergencyContact selected = contactsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Emergency Contact");
                confirm.setHeaderText("Are you sure?");
                confirm.setContentText("This will remove " + selected.getFirstName() + " " +
                                      selected.getLastName() + " from emergency contacts.");
                if (confirm.showAndWait().get() == ButtonType.OK) {
                    contactsTable.getItems().remove(selected);
                    // Renumber priorities
                    reorderEmergencyContactPriorities(contactsTable.getItems());
                }
            }
        });

        Button moveUpBtn = new Button("Move Up");
        moveUpBtn.setDisable(true);
        moveUpBtn.setOnAction(e -> {
            EmergencyContact selected = contactsTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getPriorityOrder() != null && selected.getPriorityOrder() > 1) {
                int currentPriority = selected.getPriorityOrder();
                // Swap priorities with the item above
                for (EmergencyContact contact : contactsTable.getItems()) {
                    if (contact.getPriorityOrder() != null && contact.getPriorityOrder() == currentPriority - 1) {
                        contact.setPriorityOrder(currentPriority);
                    }
                }
                selected.setPriorityOrder(currentPriority - 1);
                contactsTable.sort();
                contactsTable.getSelectionModel().select(selected);
            }
        });

        Button moveDownBtn = new Button("Move Down");
        moveDownBtn.setDisable(true);
        moveDownBtn.setOnAction(e -> {
            EmergencyContact selected = contactsTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getPriorityOrder() != null &&
                selected.getPriorityOrder() < contactsTable.getItems().size()) {
                int currentPriority = selected.getPriorityOrder();
                // Swap priorities with the item below
                for (EmergencyContact contact : contactsTable.getItems()) {
                    if (contact.getPriorityOrder() != null && contact.getPriorityOrder() == currentPriority + 1) {
                        contact.setPriorityOrder(currentPriority);
                    }
                }
                selected.setPriorityOrder(currentPriority + 1);
                contactsTable.sort();
                contactsTable.getSelectionModel().select(selected);
            }
        });

        // Enable/disable buttons based on selection
        contactsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selected = newVal != null;
            editContactBtn.setDisable(!selected);
            deleteContactBtn.setDisable(!selected);

            if (newVal != null && newVal.getPriorityOrder() != null) {
                moveUpBtn.setDisable(newVal.getPriorityOrder() <= 1);
                moveDownBtn.setDisable(newVal.getPriorityOrder() >= contactsTable.getItems().size());
            } else {
                moveUpBtn.setDisable(true);
                moveDownBtn.setDisable(true);
            }
        });

        buttonBox.getChildren().addAll(addContactBtn, editContactBtn, deleteContactBtn, moveUpBtn, moveDownBtn);

        // Help text
        Label helpLabel = new Label("REQUIRED: Add at least 2 emergency contacts. Priority 1 = first to call. " +
                                   "Use Move Up/Down to reorder contacts.");
        helpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
        helpLabel.setWrapText(true);

        vbox.getChildren().addAll(titleLabel, contactsTable, buttonBox, helpLabel);
        VBox.setVgrow(contactsTable, javafx.scene.layout.Priority.ALWAYS);

        return vbox;
    }

    /**
     * Helper method to renumber emergency contact priorities after deletion
     */
    private void reorderEmergencyContactPriorities(javafx.collections.ObservableList<EmergencyContact> contacts) {
        contacts.sort((c1, c2) -> {
            int p1 = c1.getPriorityOrder() != null ? c1.getPriorityOrder() : 99;
            int p2 = c2.getPriorityOrder() != null ? c2.getPriorityOrder() : 99;
            return Integer.compare(p1, p2);
        });
        for (int i = 0; i < contacts.size(); i++) {
            contacts.get(i).setPriorityOrder(i + 1);
        }
    }

    /**
     * ✅ NEW: Show dialog for adding/editing parent/guardian
     */
    private ParentGuardian showParentGuardianDialog(ParentGuardian existingParent, Student student) {
        Dialog<ParentGuardian> dialog = new Dialog<>();
        dialog.setTitle(existingParent == null ? "Add Parent/Guardian" : "Edit Parent/Guardian");
        dialog.setHeaderText(existingParent == null ? "Enter parent/guardian information" : "Modify parent/guardian information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form with tabs for organization (40+ fields)
        TabPane formTabPane = new TabPane();
        formTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Basic Info & Contact
        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(10);
        basicGrid.setVgap(10);
        basicGrid.setPadding(new Insets(15));

        TextField firstNameField = new TextField();
        firstNameField.setId("parentFirstName");
        firstNameField.setPromptText("First Name");
        if (existingParent != null && existingParent.getFirstName() != null) {
            firstNameField.setText(existingParent.getFirstName());
        }

        TextField lastNameField = new TextField();
        lastNameField.setId("parentLastName");
        lastNameField.setPromptText("Last Name");
        if (existingParent != null && existingParent.getLastName() != null) {
            lastNameField.setText(existingParent.getLastName());
        }

        TextField middleNameField = new TextField();
        middleNameField.setId("parentMiddleName");
        middleNameField.setPromptText("Middle Name");
        if (existingParent != null && existingParent.getMiddleName() != null) {
            middleNameField.setText(existingParent.getMiddleName());
        }

        ComboBox<String> relationshipCombo = new ComboBox<>();
        relationshipCombo.setId("parentRelationship");
        relationshipCombo.getItems().addAll("Mother", "Father", "Stepmother", "Stepfather",
                                            "Grandmother", "Grandfather", "Aunt", "Uncle",
                                            "Foster Parent", "Legal Guardian", "Other");
        if (existingParent != null && existingParent.getRelationship() != null) {
            relationshipCombo.setValue(existingParent.getRelationship());
        }

        TextField emailField = new TextField();
        emailField.setId("parentEmail");
        emailField.setPromptText("Email Address");
        if (existingParent != null && existingParent.getEmail() != null) {
            emailField.setText(existingParent.getEmail());
        }

        TextField homePhoneField = new TextField();
        homePhoneField.setId("parentHomePhone");
        homePhoneField.setPromptText("(555) 123-4567");
        if (existingParent != null && existingParent.getHomePhone() != null) {
            homePhoneField.setText(existingParent.getHomePhone());
        }

        TextField cellPhoneField = new TextField();
        cellPhoneField.setId("parentCellPhone");
        cellPhoneField.setPromptText("(555) 123-4567");
        if (existingParent != null && existingParent.getCellPhone() != null) {
            cellPhoneField.setText(existingParent.getCellPhone());
        }

        TextField workPhoneField = new TextField();
        workPhoneField.setId("parentWorkPhone");
        workPhoneField.setPromptText("(555) 123-4567");
        if (existingParent != null && existingParent.getWorkPhone() != null) {
            workPhoneField.setText(existingParent.getWorkPhone());
        }

        int row = 0;
        basicGrid.add(new Label("* First Name:"), 0, row);
        basicGrid.add(firstNameField, 1, row++);
        basicGrid.add(new Label("* Last Name:"), 0, row);
        basicGrid.add(lastNameField, 1, row++);
        basicGrid.add(new Label("Middle Name:"), 0, row);
        basicGrid.add(middleNameField, 1, row++);
        basicGrid.add(new Label("* Relationship:"), 0, row);
        basicGrid.add(relationshipCombo, 1, row++);
        basicGrid.add(new Label("Email:"), 0, row);
        basicGrid.add(emailField, 1, row++);
        basicGrid.add(new Label("Home Phone:"), 0, row);
        basicGrid.add(homePhoneField, 1, row++);
        basicGrid.add(new Label("* Cell Phone:"), 0, row);
        basicGrid.add(cellPhoneField, 1, row++);
        basicGrid.add(new Label("Work Phone:"), 0, row);
        basicGrid.add(workPhoneField, 1, row++);

        Tab basicInfoTab = new Tab("Basic Info", basicGrid);

        // Tab 2: Work & Legal
        GridPane legalGrid = new GridPane();
        legalGrid.setHgap(10);
        legalGrid.setVgap(10);
        legalGrid.setPadding(new Insets(15));

        TextField employerField = new TextField();
        employerField.setId("parentEmployer");
        employerField.setPromptText("Employer Name");
        if (existingParent != null && existingParent.getEmployer() != null) {
            employerField.setText(existingParent.getEmployer());
        }

        TextField occupationField = new TextField();
        occupationField.setId("parentOccupation");
        occupationField.setPromptText("Job Title");
        if (existingParent != null && existingParent.getOccupation() != null) {
            occupationField.setText(existingParent.getOccupation());
        }

        CheckBox primaryCustodianBox = new CheckBox("Primary Custodian");
        primaryCustodianBox.setId("parentPrimaryCustodian");
        if (existingParent != null && existingParent.getIsPrimaryCustodian() != null) {
            primaryCustodianBox.setSelected(existingParent.getIsPrimaryCustodian());
        }

        CheckBox legalCustodyBox = new CheckBox("Has Legal Custody");
        legalCustodyBox.setId("parentLegalCustody");
        if (existingParent != null && existingParent.getHasLegalCustody() != null) {
            legalCustodyBox.setSelected(existingParent.getHasLegalCustody());
        }

        CheckBox canPickUpBox = new CheckBox("Authorized to Pick Up Student");
        canPickUpBox.setId("parentCanPickUp");
        if (existingParent != null && existingParent.getCanPickUpStudent() != null) {
            canPickUpBox.setSelected(existingParent.getCanPickUpStudent());
        }

        CheckBox livesWithStudentBox = new CheckBox("Lives with Student");
        livesWithStudentBox.setId("parentLivesWithStudent");
        if (existingParent != null && existingParent.getLivesWithStudent() != null) {
            livesWithStudentBox.setSelected(existingParent.getLivesWithStudent());
        }

        row = 0;
        legalGrid.add(new Label("Employer:"), 0, row);
        legalGrid.add(employerField, 1, row++);
        legalGrid.add(new Label("Occupation:"), 0, row);
        legalGrid.add(occupationField, 1, row++);
        legalGrid.add(primaryCustodianBox, 0, row++, 2, 1);
        legalGrid.add(legalCustodyBox, 0, row++, 2, 1);
        legalGrid.add(canPickUpBox, 0, row++, 2, 1);
        legalGrid.add(livesWithStudentBox, 0, row++, 2, 1);

        Tab legalTab = new Tab("Work & Legal", legalGrid);

        formTabPane.getTabs().addAll(basicInfoTab, legalTab);

        dialog.getDialogPane().setContent(formTabPane);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(500);

        // Enable/disable save button
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable validateParent = () -> {
            boolean valid = !firstNameField.getText().trim().isEmpty() &&
                           !lastNameField.getText().trim().isEmpty() &&
                           relationshipCombo.getValue() != null &&
                           !cellPhoneField.getText().trim().isEmpty();
            saveButton.setDisable(!valid);
        };
        firstNameField.textProperty().addListener((obs, old, newVal) -> validateParent.run());
        lastNameField.textProperty().addListener((obs, old, newVal) -> validateParent.run());
        relationshipCombo.valueProperty().addListener((obs, old, newVal) -> validateParent.run());
        cellPhoneField.textProperty().addListener((obs, old, newVal) -> validateParent.run());
        validateParent.run();

        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                ParentGuardian parent = existingParent != null ? existingParent : new ParentGuardian();

                parent.setStudent(student);
                parent.setFirstName(firstNameField.getText().trim());
                parent.setLastName(lastNameField.getText().trim());
                parent.setMiddleName(middleNameField.getText().trim().isEmpty() ? null : middleNameField.getText().trim());
                parent.setRelationship(relationshipCombo.getValue());
                parent.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                parent.setHomePhone(homePhoneField.getText().trim().isEmpty() ? null : homePhoneField.getText().trim());
                parent.setCellPhone(cellPhoneField.getText().trim());
                parent.setWorkPhone(workPhoneField.getText().trim().isEmpty() ? null : workPhoneField.getText().trim());
                parent.setEmployer(employerField.getText().trim().isEmpty() ? null : employerField.getText().trim());
                parent.setOccupation(occupationField.getText().trim().isEmpty() ? null : occupationField.getText().trim());
                parent.setIsPrimaryCustodian(primaryCustodianBox.isSelected());
                parent.setHasLegalCustody(legalCustodyBox.isSelected());
                parent.setCanPickUpStudent(canPickUpBox.isSelected());
                parent.setLivesWithStudent(livesWithStudentBox.isSelected());

                return parent;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    /**
     * ✅ NEW: Show dialog for adding/editing emergency contact
     */
    private EmergencyContact showEmergencyContactDialog(EmergencyContact existingContact, Student student, int suggestedPriority) {
        Dialog<EmergencyContact> dialog = new Dialog<>();
        dialog.setTitle(existingContact == null ? "Add Emergency Contact" : "Edit Emergency Contact");
        dialog.setHeaderText(existingContact == null ? "Enter emergency contact information" : "Modify emergency contact information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.setId("contactPriority");
        priorityCombo.getItems().addAll(1, 2, 3, 4, 5);
        if (existingContact != null && existingContact.getPriorityOrder() != null) {
            priorityCombo.setValue(existingContact.getPriorityOrder());
        } else {
            priorityCombo.setValue(suggestedPriority);
        }

        TextField firstNameField = new TextField();
        firstNameField.setId("contactFirstName");
        firstNameField.setPromptText("First Name");
        if (existingContact != null && existingContact.getFirstName() != null) {
            firstNameField.setText(existingContact.getFirstName());
        }

        TextField lastNameField = new TextField();
        lastNameField.setId("contactLastName");
        lastNameField.setPromptText("Last Name");
        if (existingContact != null && existingContact.getLastName() != null) {
            lastNameField.setText(existingContact.getLastName());
        }

        ComboBox<String> relationshipCombo = new ComboBox<>();
        relationshipCombo.setId("contactRelationship");
        relationshipCombo.getItems().addAll("Mother", "Father", "Grandparent", "Aunt", "Uncle",
                                            "Sibling", "Neighbor", "Friend", "Other");
        if (existingContact != null && existingContact.getRelationship() != null) {
            relationshipCombo.setValue(existingContact.getRelationship());
        }

        TextField primaryPhoneField = new TextField();
        primaryPhoneField.setId("contactPrimaryPhone");
        primaryPhoneField.setPromptText("(555) 123-4567");
        if (existingContact != null && existingContact.getPrimaryPhone() != null) {
            primaryPhoneField.setText(existingContact.getPrimaryPhone());
        }

        TextField secondaryPhoneField = new TextField();
        secondaryPhoneField.setId("contactSecondaryPhone");
        secondaryPhoneField.setPromptText("(555) 123-4567");
        if (existingContact != null && existingContact.getSecondaryPhone() != null) {
            secondaryPhoneField.setText(existingContact.getSecondaryPhone());
        }

        CheckBox authorizedPickUpBox = new CheckBox("Authorized to Pick Up Student");
        authorizedPickUpBox.setId("contactAuthorizedPickUp");
        if (existingContact != null && existingContact.getAuthorizedToPickUp() != null) {
            authorizedPickUpBox.setSelected(existingContact.getAuthorizedToPickUp());
        }

        CheckBox livesWithStudentBox = new CheckBox("Lives with Student");
        livesWithStudentBox.setId("contactLivesWithStudent");
        if (existingContact != null && existingContact.getLivesWithStudent() != null) {
            livesWithStudentBox.setSelected(existingContact.getLivesWithStudent());
        }

        int row = 0;
        grid.add(new Label("* Priority (1-5):"), 0, row);
        grid.add(priorityCombo, 1, row++);
        grid.add(new Label("* First Name:"), 0, row);
        grid.add(firstNameField, 1, row++);
        grid.add(new Label("* Last Name:"), 0, row);
        grid.add(lastNameField, 1, row++);
        grid.add(new Label("* Relationship:"), 0, row);
        grid.add(relationshipCombo, 1, row++);
        grid.add(new Label("* Primary Phone:"), 0, row);
        grid.add(primaryPhoneField, 1, row++);
        grid.add(new Label("Secondary Phone:"), 0, row);
        grid.add(secondaryPhoneField, 1, row++);
        grid.add(authorizedPickUpBox, 0, row++, 2, 1);
        grid.add(livesWithStudentBox, 0, row++, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(450);

        // Enable/disable save button
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable validateContact = () -> {
            boolean valid = priorityCombo.getValue() != null &&
                           !firstNameField.getText().trim().isEmpty() &&
                           !lastNameField.getText().trim().isEmpty() &&
                           relationshipCombo.getValue() != null &&
                           !primaryPhoneField.getText().trim().isEmpty();
            saveButton.setDisable(!valid);
        };
        priorityCombo.valueProperty().addListener((obs, old, newVal) -> validateContact.run());
        firstNameField.textProperty().addListener((obs, old, newVal) -> validateContact.run());
        lastNameField.textProperty().addListener((obs, old, newVal) -> validateContact.run());
        relationshipCombo.valueProperty().addListener((obs, old, newVal) -> validateContact.run());
        primaryPhoneField.textProperty().addListener((obs, old, newVal) -> validateContact.run());
        validateContact.run();

        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                EmergencyContact contact = existingContact != null ? existingContact : new EmergencyContact();

                contact.setStudent(student);
                contact.setPriorityOrder(priorityCombo.getValue());
                contact.setFirstName(firstNameField.getText().trim());
                contact.setLastName(lastNameField.getText().trim());
                contact.setRelationship(relationshipCombo.getValue());
                contact.setPrimaryPhone(primaryPhoneField.getText().trim());
                contact.setSecondaryPhone(secondaryPhoneField.getText().trim().isEmpty() ? null : secondaryPhoneField.getText().trim());
                contact.setAuthorizedToPickUp(authorizedPickUpBox.isSelected());
                contact.setLivesWithStudent(livesWithStudentBox.isSelected());
                contact.setIsActive(true);

                return contact;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    /**
     * ✅ NEW: Create demographics grid (Phase 2 - K-12 Enrollment Enhancement)
     */
    private GridPane createDemographicsGrid(Student student) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Date of Birth (REQUIRED for enrollment)
        DatePicker dateOfBirthPicker = new DatePicker();
        dateOfBirthPicker.setId("dateOfBirthPicker");
        dateOfBirthPicker.setPromptText("MM/DD/YYYY");
        if (student != null && student.getDateOfBirth() != null) {
            dateOfBirthPicker.setValue(student.getDateOfBirth());
        }

        // Gender
        ComboBox<String> genderCombo = new ComboBox<>();
        genderCombo.setId("genderCombo");
        genderCombo.getItems().addAll("Male", "Female");
        genderCombo.setPromptText("Select Gender");
        if (student != null && student.getGender() != null) {
            genderCombo.setValue(student.getGender());
        }

        // Ethnicity (Federal reporting requirement)
        ComboBox<String> ethnicityCombo = new ComboBox<>();
        ethnicityCombo.setId("ethnicityCombo");
        ethnicityCombo.getItems().addAll("Hispanic or Latino", "Not Hispanic or Latino");
        ethnicityCombo.setPromptText("Select Ethnicity");
        if (student != null && student.getEthnicity() != null) {
            ethnicityCombo.setValue(student.getEthnicity());
        }

        // Race (Federal NCES reporting)
        ComboBox<String> raceCombo = new ComboBox<>();
        raceCombo.setId("raceCombo");
        raceCombo.getItems().addAll(
            "American Indian or Alaska Native",
            "Asian",
            "Black or African American",
            "Native Hawaiian or Other Pacific Islander",
            "White",
            "Two or More Races"
        );
        raceCombo.setPromptText("Select Race");
        if (student != null && student.getRace() != null) {
            raceCombo.setValue(student.getRace());
        }

        // Preferred Language
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.setId("languageCombo");
        languageCombo.getItems().addAll("English", "Spanish", "Chinese", "French", "Arabic", "Vietnamese", "Other");
        languageCombo.setPromptText("Select Language");
        if (student != null && student.getPreferredLanguage() != null) {
            languageCombo.setValue(student.getPreferredLanguage());
        }

        // English Language Learner
        CheckBox isELLCheckBox = new CheckBox("English Language Learner (ELL)");
        isELLCheckBox.setId("isELLCheckBox");
        if (student != null && student.getIsEnglishLearner() != null) {
            isELLCheckBox.setSelected(student.getIsEnglishLearner());
        }

        // Birth Location
        TextField birthCityField = new TextField();
        birthCityField.setId("birthCityField");
        birthCityField.setPromptText("Birth City");
        if (student != null && student.getBirthCity() != null) {
            birthCityField.setText(student.getBirthCity());
        }

        TextField birthStateField = new TextField();
        birthStateField.setId("birthStateField");
        birthStateField.setPromptText("State (2 letters, e.g., NY, CA)");
        birthStateField.setPrefColumnCount(2);
        if (student != null && student.getBirthState() != null) {
            birthStateField.setText(student.getBirthState());
        }
        // Add validation: max 2 characters, auto-uppercase
        birthStateField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 2) {
                birthStateField.setText(oldVal);
            } else if (newVal != null && !newVal.isEmpty()) {
                birthStateField.setText(newVal.toUpperCase());
            }
        });

        TextField birthCountryField = new TextField();
        birthCountryField.setId("birthCountryField");
        birthCountryField.setPromptText("Birth Country");
        if (student != null && student.getBirthCountry() != null) {
            birthCountryField.setText(student.getBirthCountry());
        }

        // Layout
        int row = 0;
        grid.add(new Label("* Date of Birth:"), 0, row);
        grid.add(dateOfBirthPicker, 1, row++);

        grid.add(new Label("Gender:"), 0, row);
        grid.add(genderCombo, 1, row++);

        grid.add(new Label("Ethnicity (Federal):"), 0, row);
        grid.add(ethnicityCombo, 1, row++);

        grid.add(new Label("Race (Federal):"), 0, row);
        grid.add(raceCombo, 1, row++);

        grid.add(new Label("Preferred Language:"), 0, row);
        grid.add(languageCombo, 1, row++);

        grid.add(isELLCheckBox, 1, row++);

        grid.add(new Label("Birth City:"), 0, row);
        grid.add(birthCityField, 1, row++);

        grid.add(new Label("Birth State:"), 0, row);
        grid.add(birthStateField, 1, row++);

        grid.add(new Label("Birth Country:"), 0, row);
        grid.add(birthCountryField, 1, row++);

        return grid;
    }

    /**
     * ✅ NEW: Create home address grid (Phase 2 - K-12 Enrollment Enhancement)
     */
    private GridPane createAddressGrid(Student student) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Home Address (REQUIRED for proof of residency)
        TextField homeStreetField = new TextField();
        homeStreetField.setId("homeStreetField");
        homeStreetField.setPromptText("Street Address");
        if (student != null && student.getHomeStreetAddress() != null) {
            homeStreetField.setText(student.getHomeStreetAddress());
        }

        TextField homeCityField = new TextField();
        homeCityField.setId("homeCityField");
        homeCityField.setPromptText("City");
        if (student != null && student.getHomeCity() != null) {
            homeCityField.setText(student.getHomeCity());
        }

        TextField homeStateField = new TextField();
        homeStateField.setId("homeStateField");
        homeStateField.setPromptText("State (2 letters, e.g., NY, CA)");
        homeStateField.setPrefColumnCount(2);
        if (student != null && student.getHomeState() != null) {
            homeStateField.setText(student.getHomeState());
        }
        // Add validation: max 2 characters, auto-uppercase
        homeStateField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 2) {
                homeStateField.setText(oldVal);
            } else if (newVal != null && !newVal.isEmpty()) {
                homeStateField.setText(newVal.toUpperCase());
            }
        });

        TextField homeZipField = new TextField();
        homeZipField.setId("homeZipField");
        homeZipField.setPromptText("ZIP Code");
        if (student != null && student.getHomeZipCode() != null) {
            homeZipField.setText(student.getHomeZipCode());
        }

        TextField countyField = new TextField();
        countyField.setId("countyField");
        countyField.setPromptText("County");
        if (student != null && student.getHomeCounty() != null) {
            countyField.setText(student.getHomeCounty());
        }

        // Mailing Address (if different)
        Label mailingLabel = new Label("Mailing Address (if different from home):");
        mailingLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        TextField mailingStreetField = new TextField();
        mailingStreetField.setId("mailingStreetField");
        mailingStreetField.setPromptText("Mailing Street Address");
        if (student != null && student.getMailingStreetAddress() != null) {
            mailingStreetField.setText(student.getMailingStreetAddress());
        }

        TextField mailingCityField = new TextField();
        mailingCityField.setId("mailingCityField");
        mailingCityField.setPromptText("Mailing City");
        if (student != null && student.getMailingCity() != null) {
            mailingCityField.setText(student.getMailingCity());
        }

        TextField mailingStateField = new TextField();
        mailingStateField.setId("mailingStateField");
        mailingStateField.setPromptText("State (2 letters, e.g., NY, CA)");
        mailingStateField.setPrefColumnCount(2);
        if (student != null && student.getMailingState() != null) {
            mailingStateField.setText(student.getMailingState());
        }
        // Add validation: max 2 characters, auto-uppercase
        mailingStateField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 2) {
                mailingStateField.setText(oldVal);
            } else if (newVal != null && !newVal.isEmpty()) {
                mailingStateField.setText(newVal.toUpperCase());
            }
        });

        TextField mailingZipField = new TextField();
        mailingZipField.setId("mailingZipField");
        mailingZipField.setPromptText("ZIP Code");
        if (student != null && student.getMailingZipCode() != null) {
            mailingZipField.setText(student.getMailingZipCode());
        }

        // Residency Verification
        CheckBox residencyVerifiedCheckBox = new CheckBox("Residency Verified (Proof of Address)");
        residencyVerifiedCheckBox.setId("residencyVerifiedCheckBox");
        if (student != null && student.getResidencyVerified() != null) {
            residencyVerifiedCheckBox.setSelected(student.getResidencyVerified());
        }

        DatePicker residencyVerifiedDatePicker = new DatePicker();
        residencyVerifiedDatePicker.setId("residencyVerifiedDatePicker");
        residencyVerifiedDatePicker.setPromptText("Verification Date");
        if (student != null && student.getResidencyVerifiedDate() != null) {
            residencyVerifiedDatePicker.setValue(student.getResidencyVerifiedDate());
        }

        // Layout
        int row = 0;
        grid.add(new Label("* Home Street Address:"), 0, row);
        grid.add(homeStreetField, 1, row++);

        grid.add(new Label("* Home City:"), 0, row);
        grid.add(homeCityField, 1, row++);

        HBox stateZipBox = new HBox(10);
        stateZipBox.getChildren().addAll(
            new Label("* State:"), homeStateField,
            new Label("* ZIP:"), homeZipField
        );
        grid.add(stateZipBox, 0, row++, 2, 1);

        grid.add(new Label("County:"), 0, row);
        grid.add(countyField, 1, row++);

        // Separator
        grid.add(mailingLabel, 0, row++, 2, 1);

        grid.add(new Label("Mailing Street:"), 0, row);
        grid.add(mailingStreetField, 1, row++);

        grid.add(new Label("Mailing City:"), 0, row);
        grid.add(mailingCityField, 1, row++);

        HBox mailStateZipBox = new HBox(10);
        mailStateZipBox.getChildren().addAll(
            new Label("State:"), mailingStateField,
            new Label("ZIP:"), mailingZipField
        );
        grid.add(mailStateZipBox, 0, row++, 2, 1);

        // Separator
        Label verificationLabel = new Label("Residency Verification:");
        verificationLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        grid.add(verificationLabel, 0, row++, 2, 1);

        grid.add(residencyVerifiedCheckBox, 0, row++, 2, 1);
        grid.add(new Label("Verification Date:"), 0, row);
        grid.add(residencyVerifiedDatePicker, 1, row++);

        return grid;
    }

    /**
     * ✅ NEW: Create special circumstances grid (Phase 2 - K-12 Enrollment Enhancement)
     */
    private GridPane createSpecialCircumstancesGrid(Student student) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // ========================================================================
        // FEDERAL COMPLIANCE FIELDS
        // ========================================================================

        Label federalLabel = new Label("Federal Compliance (FAFSA, McKinney-Vento):");
        federalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0 0 5 0;");

        // Foster Care
        CheckBox isFosterCareCheckBox = new CheckBox("Student is in Foster Care");
        isFosterCareCheckBox.setId("isFosterCareCheckBox");
        if (student != null && student.getIsFosterCare() != null) {
            isFosterCareCheckBox.setSelected(student.getIsFosterCare());
        }

        TextField fosterAgencyField = new TextField();
        fosterAgencyField.setId("fosterAgencyField");
        fosterAgencyField.setPromptText("Foster Care Agency");
        if (student != null && student.getFosterCareAgency() != null) {
            fosterAgencyField.setText(student.getFosterCareAgency());
        }

        TextField fosterCaseworkerField = new TextField();
        fosterCaseworkerField.setId("fosterCaseworkerField");
        fosterCaseworkerField.setPromptText("Case Worker Name");
        if (student != null && student.getFosterCaseWorkerName() != null) {
            fosterCaseworkerField.setText(student.getFosterCaseWorkerName());
        }

        // Homeless
        CheckBox isHomelessCheckBox = new CheckBox("Student is Homeless (McKinney-Vento)");
        isHomelessCheckBox.setId("isHomelessCheckBox");
        if (student != null && student.getIsHomeless() != null) {
            isHomelessCheckBox.setSelected(student.getIsHomeless());
        }

        TextField homelessShelterField = new TextField();
        homelessShelterField.setId("homelessShelterField");
        homelessShelterField.setPromptText("Shelter/Temporary Address");
        if (student != null && student.getHomelessSituationType() != null) {
            homelessShelterField.setText(student.getHomelessSituationType());
        }

        // Orphan
        CheckBox isOrphanCheckBox = new CheckBox("Student is an Orphan (Both parents deceased)");
        isOrphanCheckBox.setId("isOrphanCheckBox");
        if (student != null && student.getIsOrphan() != null) {
            isOrphanCheckBox.setSelected(student.getIsOrphan());
        }

        // Ward of Court
        CheckBox isWardOfCourtCheckBox = new CheckBox("Student is a Ward of the Court");
        isWardOfCourtCheckBox.setId("isWardOfCourtCheckBox");
        if (student != null && student.getIsWardOfCourt() != null) {
            isWardOfCourtCheckBox.setSelected(student.getIsWardOfCourt());
        }

        // Refugee/Asylee
        CheckBox isRefugeeCheckBox = new CheckBox("Student is a Refugee or Asylee");
        isRefugeeCheckBox.setId("isRefugeeCheckBox");
        if (student != null && student.getIsRefugeeAsylee() != null) {
            isRefugeeCheckBox.setSelected(student.getIsRefugeeAsylee());
        }

        // Unaccompanied Youth
        CheckBox isUnaccompaniedYouthCheckBox = new CheckBox("Unaccompanied Youth (Lives alone)");
        isUnaccompaniedYouthCheckBox.setId("isUnaccompaniedYouthCheckBox");
        if (student != null && student.getIsUnaccompaniedYouth() != null) {
            isUnaccompaniedYouthCheckBox.setSelected(student.getIsUnaccompaniedYouth());
        }

        // ========================================================================
        // FAMILY SITUATION
        // ========================================================================

        Label familyLabel = new Label("Family Situation:");
        familyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");

        // Military Family
        CheckBox isMilitaryFamilyCheckBox = new CheckBox("Military Family");
        isMilitaryFamilyCheckBox.setId("isMilitaryFamilyCheckBox");
        if (student != null && student.getIsMilitaryFamily() != null) {
            isMilitaryFamilyCheckBox.setSelected(student.getIsMilitaryFamily());
        }

        // Migrant Family
        CheckBox isMigrantFamilyCheckBox = new CheckBox("Migrant Worker Family");
        isMigrantFamilyCheckBox.setId("isMigrantFamilyCheckBox");
        if (student != null && student.getIsMigrantFamily() != null) {
            isMigrantFamilyCheckBox.setSelected(student.getIsMigrantFamily());
        }

        // Custody Arrangement
        TextField custodyArrangementField = new TextField();
        custodyArrangementField.setId("custodyArrangementField");
        custodyArrangementField.setPromptText("Custody Arrangement (e.g., Joint, Sole, Split)");
        if (student != null && student.getCustodyArrangement() != null) {
            custodyArrangementField.setText(student.getCustodyArrangement());
        }

        // ========================================================================
        // PREVIOUS SCHOOL
        // ========================================================================

        Label previousSchoolLabel = new Label("Previous School Information:");
        previousSchoolLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");

        TextField previousSchoolNameField = new TextField();
        previousSchoolNameField.setId("previousSchoolNameField");
        previousSchoolNameField.setPromptText("Previous School Name");
        if (student != null && student.getPreviousSchoolName() != null) {
            previousSchoolNameField.setText(student.getPreviousSchoolName());
        }

        TextField previousSchoolAddressField = new TextField();
        previousSchoolAddressField.setId("previousSchoolAddressField");
        previousSchoolAddressField.setPromptText("Previous School Address");
        if (student != null && student.getPreviousSchoolAddress() != null) {
            previousSchoolAddressField.setText(student.getPreviousSchoolAddress());
        }

        TextField previousSchoolPhoneField = new TextField();
        previousSchoolPhoneField.setId("previousSchoolPhoneField");
        previousSchoolPhoneField.setPromptText("Previous School Phone");
        if (student != null && student.getPreviousSchoolPhone() != null) {
            previousSchoolPhoneField.setText(student.getPreviousSchoolPhone());
        }

        // Layout
        int row = 0;

        // Federal Compliance Section
        grid.add(federalLabel, 0, row++, 2, 1);
        grid.add(isFosterCareCheckBox, 0, row++, 2, 1);
        grid.add(new Label("  Foster Care Agency:"), 0, row);
        grid.add(fosterAgencyField, 1, row++);
        grid.add(new Label("  Case Worker:"), 0, row);
        grid.add(fosterCaseworkerField, 1, row++);

        grid.add(isHomelessCheckBox, 0, row++, 2, 1);
        grid.add(new Label("  Shelter/Address:"), 0, row);
        grid.add(homelessShelterField, 1, row++);

        grid.add(isOrphanCheckBox, 0, row++, 2, 1);
        grid.add(isWardOfCourtCheckBox, 0, row++, 2, 1);
        grid.add(isRefugeeCheckBox, 0, row++, 2, 1);
        grid.add(isUnaccompaniedYouthCheckBox, 0, row++, 2, 1);

        // Family Situation Section
        grid.add(familyLabel, 0, row++, 2, 1);
        grid.add(isMilitaryFamilyCheckBox, 0, row++, 2, 1);
        grid.add(isMigrantFamilyCheckBox, 0, row++, 2, 1);
        grid.add(new Label("Custody Arrangement:"), 0, row);
        grid.add(custodyArrangementField, 1, row++);

        // Previous School Section
        grid.add(previousSchoolLabel, 0, row++, 2, 1);
        grid.add(new Label("School Name:"), 0, row);
        grid.add(previousSchoolNameField, 1, row++);
        grid.add(new Label("School Address:"), 0, row);
        grid.add(previousSchoolAddressField, 1, row++);
        grid.add(new Label("School Phone:"), 0, row);
        grid.add(previousSchoolPhoneField, 1, row++);

        return grid;
    }

    @FXML
    private void handleRefresh() {
        log.info("Refresh clicked");
        loadStudents();
    }

    @FXML
    private void handleStatistics() {
        log.info("Statistics clicked");

        long total = studentsList.size();
        long active = studentsList.stream().filter(Student::getActive).count();
        long withIEP = studentsList.stream().filter(s -> s.getHasIEP() != null && s.getHasIEP()).count();
        long with504 = studentsList.stream().filter(s -> s.getHas504Plan() != null && s.getHas504Plan()).count();
        long gifted = studentsList.stream().filter(s -> s.getIsGifted() != null && s.getIsGifted()).count();

        showInfo("Student Statistics",
            String.format("Total Students: %d\n" +
                        "Active: %d\n" +
                        "Inactive: %d\n\n" +
                        "Special Programs:\n" +
                        "  - IEP: %d\n" +
                        "  - 504 Plan: %d\n" +
                        "  - Gifted & Talented: %d",
                total, active, total - active, withIEP, with504, gifted));
    }

    @FXML
    private void handleExport() {
        log.info("Export clicked");

        // Create file chooser
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Students to CSV");
        fileChooser.setInitialFileName("students_export.csv");
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        // Show save dialog
        java.io.File file = fileChooser.showSaveDialog(studentsTable.getScene().getWindow());

        if (file != null) {
            try {
                exportToCSV(file);
                showInfo("Export Successful", "Students exported to: " + file.getAbsolutePath());
            } catch (Exception e) {
                log.error("Error exporting students", e);
                showError("Export Failed", "Failed to export students: " + e.getMessage());
            }
        }
    }

    private void exportToCSV(java.io.File file) throws java.io.IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
            // Write header
            writer.println("Student ID,First Name,Last Name,Grade Level,Email,Status,Enrolled Courses,IEP,504 Plan,Gifted");

            // Write student data
            for (Student student : studentsList) {
                writer.printf("%s,%s,%s,%s,%s,%s,%d,%s,%s,%s%n",
                    escapeCsv(student.getStudentId()),
                    escapeCsv(student.getFirstName()),
                    escapeCsv(student.getLastName()),
                    escapeCsv(student.getGradeLevel()),
                    escapeCsv(student.getEmail() != null ? student.getEmail() : ""),
                    student.getActive() ? "Active" : "Inactive",
                    student.getEnrolledCourseCount(),
                    (student.getHasIEP() != null && student.getHasIEP()) ? "Yes" : "No",
                    (student.getHas504Plan() != null && student.getHas504Plan()) ? "Yes" : "No",
                    (student.getIsGifted() != null && student.getIsGifted()) ? "Yes" : "No"
                );
            }

            log.info("Exported {} students to {}", studentsList.size(), file.getAbsolutePath());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return            "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return            "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return            value;
    }

    @FXML
    private void handleSearch() {
        log.info("Search: " + searchField.getText());
        applyFilters();
    }

    @FXML
    private void handleFilter() {
        log.info("Filter changed");
        applyFilters();
    }

    /**
     * Apply both search and filter criteria together
     */
    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String grade = gradeFilter.getValue();
        String status = statusFilter.getValue();
        String sped = spedFilter != null ? spedFilter.getValue() : "All";

        try {
            List<Student> filtered = studentRepository.findAll().stream()
                // Apply search filter
                .filter(s -> {
                    if (query.isEmpty()) {
                        return            true;
                    }
                    return            s.getStudentId().toLowerCase().contains(query) ||
                           s.getFirstName().toLowerCase().contains(query) ||
                           s.getLastName().toLowerCase().contains(query) ||
                           (s.getEmail() != null && s.getEmail().toLowerCase().contains(query));
                })
                // Apply grade filter
                .filter(s -> grade == null || "All".equals(grade) || s.getGradeLevel().equals(grade))
                // Apply status filter
                .filter(s ->
                    status == null || "All".equals(status) ||
                    ("Active".equals(status) && s.getActive()) ||
                    ("Inactive".equals(status) && !s.getActive())
                )
                // Apply SPED filter
                .filter(s -> {
                    if (sped == null || "All".equals(sped)) return            true;
                    boolean hasIep = Boolean.TRUE.equals(s.getHasIEP());
                    boolean has504 = Boolean.TRUE.equals(s.getHas504Plan());
                    return            switch (sped) {
                        case "Has IEP" -> hasIep;
                        case "Has 504" -> has504;
                        case "Has IEP or 504" -> hasIep || has504;
                        case "No SPED" -> !hasIep && !has504;
                        default -> true;
                    };
                })
                .toList();

            studentsList.clear();
            studentsList.addAll(filtered);
            studentsTable.refresh();

            if (recordCountLabel != null) {
                recordCountLabel.setText("Total: " + filtered.size());
            }
        } catch (Exception e) {
            log.error("Error applying filters", e);
        }
    }

    @FXML
    private void handleClearFilters() {
        log.info("Clear filters clicked");
        searchField.clear();
        gradeFilter.setValue("All");
        statusFilter.setValue("All");
        if (spedFilter != null) {
            spedFilter.setValue("All");
        }
        loadStudents();
    }

    // ========== HELPER METHODS ==========

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

    /**
     * Handle View Courses button - Show student's enrolled courses
     */
    private void handleViewCourses(Student student) {
        log.info("Viewing courses for student: {} {}", student.getFirstName(), student.getLastName());

        try {
            // Reload student with enrolled courses to avoid LazyInitializationException
            Student studentWithCourses = studentRepository.findByIdWithEnrolledCourses(student.getId())
                    .orElse(student);

            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Enrolled Courses - " + studentWithCourses.getFirstName() + " " + studentWithCourses.getLastName());
            dialog.setHeaderText("Student ID: " + studentWithCourses.getStudentId() + " | Grade: " + studentWithCourses.getGradeLevel());

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setPrefWidth(600);

            // Current courses list
            Label currentLabel = new Label("📚 Currently Enrolled Courses:");
            currentLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            ListView<Course> enrolledListView = new ListView<>();
            enrolledListView.setPrefHeight(200);
            if (studentWithCourses.getEnrolledCourses() != null) {
                enrolledListView.getItems().addAll(studentWithCourses.getEnrolledCourses());
            }

            // Custom cell factory to show course details
            enrolledListView.setCellFactory(lv -> new ListCell<Course>() {
                @Override
                protected void updateItem(Course course, boolean empty) {
                    super.updateItem(course, empty);
                    if (empty || course == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s - %s (%s, %d min)",
                            course.getCourseCode(),
                            course.getCourseName(),
                            course.getSubject() != null ? course.getSubject() : "No subject",
                            course.getDurationMinutes() != null ? course.getDurationMinutes() : 0));
                    }
                }
            });

            // Available courses list
            Label availableLabel = new Label("➕ Available Courses:");
            availableLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            ListView<Course> availableListView = new ListView<>();
            availableListView.setPrefHeight(200);

            // Get courses not yet enrolled
            List<Course> allCourses = courseRepository.findAll();
            List<Course> enrolledCourses = studentWithCourses.getEnrolledCourses() != null
                ? studentWithCourses.getEnrolledCourses()
                : new java.util.ArrayList<>();
            List<Course> availableCourses = allCourses.stream()
                .filter(c -> !enrolledCourses.contains(c))
                .toList();
            availableListView.getItems().addAll(availableCourses);

            availableListView.setCellFactory(lv -> new ListCell<Course>() {
                @Override
                protected void updateItem(Course course, boolean empty) {
                    super.updateItem(course, empty);
                    if (empty || course == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s - %s (%s)",
                            course.getCourseCode(),
                            course.getCourseName(),
                            course.getSubject() != null ? course.getSubject() : "No subject"));
                    }
                }
            });

            // Action buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);

            Button addButton = new Button("➕ Enroll in Selected");
            addButton.getStyleClass().add("button-success");
            addButton.setOnAction(e -> {
                Course selected = availableListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // Initialize enrolled courses list if null
                    if (studentWithCourses.getEnrolledCourses() == null) {
                        studentWithCourses.setEnrolledCourses(new java.util.ArrayList<>());
                    }
                    studentWithCourses.getEnrolledCourses().add(selected);
                    studentRepository.save(studentWithCourses);
                    enrolledListView.getItems().add(selected);
                    availableListView.getItems().remove(selected);
                    showInfo("Success", "Course added successfully");
                    loadStudents(); // Refresh table
                } else {
                    showWarning("No Selection", "Please select a course to enroll");
                }
            });

            Button removeButton = new Button("➖ Drop Selected");
            removeButton.getStyleClass().add("button-danger");
            removeButton.setOnAction(e -> {
                Course selected = enrolledListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (studentWithCourses.getEnrolledCourses() != null) {
                        studentWithCourses.getEnrolledCourses().remove(selected);
                        studentRepository.save(studentWithCourses);
                        enrolledListView.getItems().remove(selected);
                        availableListView.getItems().add(selected);
                        showInfo("Success", "Course dropped successfully");
                        loadStudents(); // Refresh table
                    }
                } else {
                    showWarning("No Selection", "Please select a course to drop");
                }
            });

            buttonBox.getChildren().addAll(addButton, removeButton);

            // Add all to content
            content.getChildren().addAll(
                currentLabel,
                enrolledListView,
                buttonBox,
                availableLabel,
                availableListView
            );

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Error showing courses", e);
            showError("Error", "Failed to load courses: " + e.getMessage());
        }
    }

    // ========== GRADE MANAGEMENT METHODS ==========

    /**
     * Handle Add Grade button (toolbar) - Opens grade entry for selected student
     */
    @FXML
    private void handleAddGrade() {
        log.info("Add Grade button clicked");
        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();

        if (selectedStudent == null) {
            showWarning("No Selection", "Please select a student first.");
            return;
        }

        handleAddGradeForStudent(selectedStudent);
    }

    /**
     * Handle View Grades button (toolbar) - Shows grade history for selected student
     */
    @FXML
    private void handleViewGrades() {
        log.info("View Grades button clicked");
        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();

        if (selectedStudent == null) {
            showWarning("No Selection", "Please select a student first.");
            return;
        }

        showGradeHistory(selectedStudent);
    }

    /**
     * Handle Recommend Courses button (toolbar) - Shows AI recommendations for selected student
     */
    @FXML
    private void handleRecommendCourses() {
        log.info("Recommend Courses button clicked");
        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();

        if (selectedStudent == null) {
            showWarning("No Selection", "Please select a student first.");
            return;
        }

        showCourseRecommendations(selectedStudent);
    }

    /**
     * Handle Import Grades button (toolbar) - Opens CSV import dialog
     */
    @FXML
    private void handleImportGrades() {
        log.info("Import Grades button clicked");

        try {
            // Load FXML (root is VBox, not DialogPane)
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/GradeImportDialog.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);
            DialogPane dialogPane = loader.load();

            // Get controller
            GradeImportDialogController controller = loader.getController();

            // Show dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Import Grades from CSV");
            dialog.setResizable(true);
            dialog.showAndWait();

            // Refresh if import was successful
            if (controller.isImportCompleted()) {
                loadStudents(); // Reload students table to show updated GPAs

                GradeImportService.ImportResult result = controller.getImportResult();
                if (result != null && result.getSuccessCount() > 0) {
                    showInfo("Import Complete",
                        String.format("Successfully imported %d grades!\n\n" +
                            "Student GPAs have been updated.\n" +
                            "Errors: %d\n" +
                            "Warnings: %d",
                            result.getSuccessCount(),
                            result.getErrorCount(),
                            result.getWarningCount()));
                }
            }

        } catch (Exception e) {
            log.error("Error opening import dialog", e);
            showError("Error", "Failed to open import dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Add grade for specific student (called from action button)
     */
    private void handleAddGradeForStudent(Student student) {
        log.info("Opening grade dialog for student: {} {}", student.getFirstName(), student.getLastName());

        try {
            // Load FXML (root is VBox, not DialogPane)
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/StudentGradeDialog.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);
            VBox content = loader.load();

            // Get controller and set student
            StudentGradeDialogController controller = loader.getController();
            controller.setStudent(student);

            // Show dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Add Grade - " + student.getFirstName() + " " + student.getLastName());

            // Set the VBox as dialog content
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(content);
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait();

            // Refresh if saved
            if (controller.isSaved()) {
                loadStudents(); // Reload students table to show updated GPA

                StudentGrade savedGrade = controller.getGrade();
                showInfo("Grade Saved",
                    String.format("Grade %s saved for %s in %s\n\nStudent's GPA updated to: %.2f",
                        savedGrade.getLetterGrade(),
                        student.getFirstName() + " " + student.getLastName(),
                        savedGrade.getCourse().getCourseName(),
                        student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0));
            }

        } catch (Exception e) {
            log.error("Error opening grade dialog", e);
            showError("Error", "Failed to open grade dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show grade history for student
     */
    private void showGradeHistory(Student student) {
        log.info("Viewing grade history for: {} {}", student.getFirstName(), student.getLastName());

        try {
            // Reload student to get grade history
            Student studentWithGrades = studentRepository.findById(student.getId())
                .orElse(student);

            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Grade History - " + studentWithGrades.getFirstName() + " " + studentWithGrades.getLastName());
            dialog.setHeaderText(String.format("Student ID: %s | Grade: %s | GPA: %.2f",
                studentWithGrades.getStudentId(),
                studentWithGrades.getGradeLevel(),
                studentWithGrades.getCurrentGPA() != null ? studentWithGrades.getCurrentGPA() : 0.0));

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setPrefWidth(800);

            // GPA Summary Panel
            GridPane summaryGrid = new GridPane();
            summaryGrid.setHgap(20);
            summaryGrid.setVgap(10);
            summaryGrid.getStyleClass().add("summary-panel");
            summaryGrid.setStyle("-fx-padding: 15; -fx-background-radius: 5;");

            Label gpaLabel = new Label(String.format("%.2f",
                studentWithGrades.getCurrentGPA() != null ? studentWithGrades.getCurrentGPA() : 0.0));
            gpaLabel.getStyleClass().addAll("success-text", "text-bold");
            gpaLabel.setStyle("-fx-font-size: 28px;");

            Label creditsLabel = new Label(String.format("%.1f credits",
                studentWithGrades.getCreditsEarned() != null ? studentWithGrades.getCreditsEarned() : 0.0));
            creditsLabel.setStyle("-fx-font-size: 14px;");

            Label standingLabel = new Label(
                studentWithGrades.getAcademicStanding() != null ? studentWithGrades.getAcademicStanding() : "Good Standing");
            standingLabel.setStyle("-fx-font-size: 14px;");

            VBox gpaBox = new VBox(5, new Label("Current GPA"), gpaLabel);
            VBox creditsBox = new VBox(5, new Label("Credits Earned"), creditsLabel);
            VBox standingBox = new VBox(5, new Label("Academic Standing"), standingLabel);

            summaryGrid.add(gpaBox, 0, 0);
            summaryGrid.add(creditsBox, 1, 0);
            summaryGrid.add(standingBox, 2, 0);

            // Grades Table
            TableView<StudentGrade> gradesTable = new TableView<>();
            gradesTable.setPrefHeight(400);

            TableColumn<StudentGrade, String> courseCol = new TableColumn<>("Course");
            courseCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCourse() != null ? data.getValue().getCourse().getCourseName() : ""));
            courseCol.setPrefWidth(200);

            TableColumn<StudentGrade, String> termCol = new TableColumn<>("Term");
            termCol.setCellValueFactory(new PropertyValueFactory<>("term"));
            termCol.setPrefWidth(120);

            TableColumn<StudentGrade, String> letterCol = new TableColumn<>("Grade");
            letterCol.setCellValueFactory(new PropertyValueFactory<>("letterGrade"));
            letterCol.setPrefWidth(80);

            TableColumn<StudentGrade, Double> numericalCol = new TableColumn<>("Percent");
            numericalCol.setCellValueFactory(new PropertyValueFactory<>("numericalGrade"));
            numericalCol.setPrefWidth(80);

            TableColumn<StudentGrade, Double> gpaPointsCol = new TableColumn<>("GPA Points");
            gpaPointsCol.setCellValueFactory(new PropertyValueFactory<>("gpaPoints"));
            gpaPointsCol.setPrefWidth(100);

            TableColumn<StudentGrade, Double> creditsCol = new TableColumn<>("Credits");
            creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));
            creditsCol.setPrefWidth(80);

            TableColumn<StudentGrade, String> typeCol = new TableColumn<>("Type");
            typeCol.setCellValueFactory(new PropertyValueFactory<>("gradeType"));
            typeCol.setPrefWidth(100);

            gradesTable.getColumns().addAll(courseCol, termCol, letterCol, numericalCol, gpaPointsCol, creditsCol, typeCol);

            // Load grades
            List<StudentGrade> grades = gradeService.getStudentGrades(student.getId());
            gradesTable.getItems().addAll(grades);

            // Action buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);

            Button addGradeButton = new Button("➕ Add Grade");
            addGradeButton.getStyleClass().add("button-success");
            addGradeButton.setOnAction(e -> {
                dialog.close();
                handleAddGradeForStudent(student);
            });

            Button closeButton = new Button("Close");
            closeButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white;");
            closeButton.setOnAction(e -> dialog.close());

            buttonBox.getChildren().addAll(addGradeButton, closeButton);

            // Add all to content
            Label historyLabel = new Label("📊 Grade History:");
            historyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            content.getChildren().addAll(summaryGrid, historyLabel, gradesTable, buttonBox);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            // Make the built-in close button invisible (we use custom buttons)
            Node closeButtonNode = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            if (closeButtonNode != null) {
                closeButtonNode.setVisible(false);
                closeButtonNode.setManaged(false);
            }

            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Error showing grade history", e);
            showError("Error", "Failed to load grade history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show course recommendations for student
     */
    private void showCourseRecommendations(Student student) {
        log.info("Showing recommendations for: {} {}", student.getFirstName(), student.getLastName());

        try {
            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Course Recommendations - " + student.getFirstName() + " " + student.getLastName());
            dialog.setHeaderText(String.format("AI-Powered Recommendations | GPA: %.2f",
                student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0));

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setPrefWidth(700);

            // Get recommendations
            List<Course> recommendedElectives = placementService.recommendElectives(student);
            List<Course> recommendedGifted = student.getIsGifted() != null && student.getIsGifted()
                ? placementService.recommendGiftedCourses(student)
                : new java.util.ArrayList<>();
            String aiRecommendations = placementService.getAIRecommendations(student);

            // Electives Section
            if (!recommendedElectives.isEmpty()) {
                Label electivesLabel = new Label("📚 Recommended Electives:");
                electivesLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                ListView<Course> electivesListView = new ListView<>();
                electivesListView.setPrefHeight(150);
                electivesListView.getItems().addAll(recommendedElectives);
                electivesListView.setCellFactory(lv -> new ListCell<Course>() {
                    @Override
                    protected void updateItem(Course course, boolean empty) {
                        super.updateItem(course, empty);
                        if (empty || course == null) {
                            setText(null);
                        } else {
                            setText(String.format("%s - %s (%s)",
                                course.getCourseCode(),
                                course.getCourseName(),
                                course.getSubject() != null ? course.getSubject() : "No subject"));
                        }
                    }
                });

                content.getChildren().addAll(electivesLabel, electivesListView);
            }

            // Gifted/Honors Section
            if (!recommendedGifted.isEmpty()) {
                Label giftedLabel = new Label("⭐ Recommended Honors/AP Courses:");
                giftedLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                ListView<Course> giftedListView = new ListView<>();
                giftedListView.setPrefHeight(150);
                giftedListView.getItems().addAll(recommendedGifted);
                giftedListView.setCellFactory(lv -> new ListCell<Course>() {
                    @Override
                    protected void updateItem(Course course, boolean empty) {
                        super.updateItem(course, empty);
                        if (empty || course == null) {
                            setText(null);
                        } else {
                            setText(String.format("%s - %s",
                                course.getCourseCode(),
                                course.getCourseName()));
                        }
                    }
                });

                content.getChildren().addAll(giftedLabel, giftedListView);
            }

            // AI Insights Section
            Label aiLabel = new Label("💡 AI Insights:");
            aiLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            TextArea aiTextArea = new TextArea(aiRecommendations);
            aiTextArea.setWrapText(true);
            aiTextArea.setEditable(false);
            aiTextArea.setPrefHeight(200);
            aiTextArea.setStyle("-fx-background-color: #2d2d2d; -fx-control-inner-background: #2d2d2d;");

            content.getChildren().addAll(aiLabel, aiTextArea);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Error showing recommendations", e);
            showError("Error", "Failed to load recommendations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show enlarged photo in a dialog
     */
    private void showPhotoEnlarged(Student student) {
        try {
            File photoFile = new File(student.getPhotoPath());
            if (!photoFile.exists()) {
                showError("Photo Not Found", "Photo file does not exist: " + student.getPhotoPath());
                return;
            }

            javafx.scene.image.Image image = new javafx.scene.image.Image(photoFile.toURI().toString());

            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(600);
            imageView.setFitHeight(600);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Student Photo - " + student.getFirstName() + " " + student.getLastName());
            dialog.setHeaderText(student.getStudentId());

            VBox content = new VBox(15);
            content.setAlignment(javafx.geometry.Pos.CENTER);
            content.getChildren().add(imageView);

            HBox buttons = new HBox(10);
            buttons.setAlignment(javafx.geometry.Pos.CENTER);

            Button changePhotoBtn = new Button("Change Photo");
            changePhotoBtn.setOnAction(e -> {
                dialog.close();
                handleUploadPhoto();
            });

            Button closeBtn = new Button("Close");
            closeBtn.setOnAction(e -> dialog.close());

            buttons.getChildren().addAll(changePhotoBtn, closeBtn);
            content.getChildren().add(buttons);

            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Error showing enlarged photo", e);
            showError("Error", "Failed to display photo: " + e.getMessage());
        }
    }

    /**
     * Open QR code dialog for a student
     */
    private void openQRCodeDialog(Student student) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/StudentQRCodeDialog.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);

            VBox dialogContent = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("QR Code - " + student.getFirstName() + " " + student.getLastName());
            dialogStage.setScene(new javafx.scene.Scene(dialogContent));
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(studentsTable.getScene().getWindow());

            com.heronix.ui.controller.StudentQRCodeDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setStudent(student);

            dialogStage.showAndWait();

            // Refresh table to show updated QR code
            handleRefresh();

        } catch (Exception e) {
            log.error("Error opening QR code dialog", e);
            showError("Error", "Failed to open QR code dialog: " + e.getMessage());
        }
    }

    /**
     * Generate mini QR code for table display
     */
    private byte[] generateMiniQRCode(String content, int size) {
        try {
            com.google.zxing.qrcode.QRCodeWriter qrWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrWriter.encode(
                content,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size
            );

            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "PNG", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating mini QR code", e);
            return new byte[0];
        }
    }
}
