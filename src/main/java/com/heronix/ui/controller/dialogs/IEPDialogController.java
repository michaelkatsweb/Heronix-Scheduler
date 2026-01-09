package com.heronix.ui.controller.dialogs;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.IEPStatus;
import com.heronix.repository.StudentRepository;
import com.heronix.service.IEPManagementService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IEP Dialog Controller
 *
 * Controller for creating and editing IEPs with service management.
 *
 * Features:
 * - IEP creation and editing
 * - Service management
 * - Compliance tracking
 * - Responsive design (fits all screen sizes)
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - Added responsive design support
 * @since Phase 8D - November 21, 2025
 */
@Controller
@Slf4j
public class IEPDialogController extends com.heronix.ui.controller.BaseDialogController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private IEPManagementService iepManagementService;

    // Form Fields
    @FXML private ComboBox<Student> studentComboBox;
    @FXML private Label studentErrorLabel;
    @FXML private TextField iepNumberField;
    @FXML private ComboBox<IEPStatus> statusComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private DatePicker reviewDatePicker;
    @FXML private ComboBox<String> eligibilityCategoryComboBox;
    @FXML private ComboBox<String> primaryDisabilityComboBox;
    @FXML private TextField caseManagerField;
    @FXML private TextArea goalsTextArea;
    @FXML private TextArea accommodationsTextArea;
    @FXML private TextArea notesTextArea;

    // Services Table
    @FXML private Button addServiceButton;
    @FXML private TableView<IEPService> servicesTable;
    @FXML private TableColumn<IEPService, String> serviceTypeColumn;
    @FXML private TableColumn<IEPService, String> minutesColumn;
    @FXML private TableColumn<IEPService, String> frequencyColumn;
    @FXML private TableColumn<IEPService, String> deliveryColumn;
    @FXML private TableColumn<IEPService, String> serviceActionsColumn;

    private IEP currentIEP;
    private boolean isEditMode = false;
    private ObservableList<IEPService> services = FXCollections.observableArrayList();

    // Standard eligibility categories (IDEA categories)
    private static final String[] ELIGIBILITY_CATEGORIES = {
        "Autism",
        "Deaf-Blindness",
        "Deafness",
        "Developmental Delay",
        "Emotional Disturbance",
        "Hearing Impairment",
        "Intellectual Disability",
        "Multiple Disabilities",
        "Orthopedic Impairment",
        "Other Health Impairment",
        "Specific Learning Disability",
        "Speech or Language Impairment",
        "Traumatic Brain Injury",
        "Visual Impairment"
    };

    private static final String[] PRIMARY_DISABILITIES = {
        "ADHD",
        "Anxiety Disorder",
        "Asperger's Syndrome",
        "Auditory Processing Disorder",
        "Autism Spectrum Disorder",
        "Cerebral Palsy",
        "Down Syndrome",
        "Dyslexia",
        "Dyscalculia",
        "Dysgraphia",
        "Epilepsy",
        "Hearing Loss",
        "Intellectual Disability",
        "Language Delay",
        "Motor Impairment",
        "Oppositional Defiant Disorder",
        "Speech Impairment",
        "Visual Impairment"
    };

    @FXML
    public void initialize() {
        log.info("Initializing IEP Dialog Controller");
        setupStudentComboBox();
        setupStatusComboBox();
        setupCategoryComboBoxes();
        setupServicesTable();
        setupDateDefaults();
    }

    private void setupStudentComboBox() {
        // Load all students
        List<Student> students = studentRepository.findAll();
        studentComboBox.getItems().addAll(students);

        // Configure display
        studentComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Student student) {
                if (student == null) return "";
                return student.getFullName() + " (" + student.getStudentId() + ")";
            }

            @Override
            public Student fromString(String string) {
                return null; // Not needed for non-editable combo box
            }
        });

        // Validate on selection
        studentComboBox.setOnAction(e -> validateStudent());
    }

    private void setupStatusComboBox() {
        statusComboBox.getItems().addAll(IEPStatus.values());
        statusComboBox.setValue(IEPStatus.DRAFT);
    }

    private void setupCategoryComboBoxes() {
        eligibilityCategoryComboBox.getItems().addAll(ELIGIBILITY_CATEGORIES);
        primaryDisabilityComboBox.getItems().addAll(PRIMARY_DISABILITIES);
    }

    private void setupDateDefaults() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusYears(1));
        reviewDatePicker.setValue(LocalDate.now().plusYears(1).minusDays(30));
    }

    private void setupServicesTable() {
        serviceTypeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getServiceType().getDisplayName()));

        minutesColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getMinutesPerWeek())));

        frequencyColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getFrequency() != null ?
                data.getValue().getFrequency() : "N/A"));

        deliveryColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDeliveryModel().getDisplayName()));

        serviceActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final HBox buttonBox = new HBox(5);
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("X");

            {
                editBtn.setOnAction(e -> handleEditService(getTableRow().getItem()));
                editBtn.getStyleClass().add("btn-link");
                deleteBtn.setOnAction(e -> handleDeleteService(getTableRow().getItem()));
                deleteBtn.getStyleClass().addAll("btn-link", "btn-danger");
                buttonBox.getChildren().addAll(editBtn, deleteBtn);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
            }
        });

        servicesTable.setItems(services);
    }

    private boolean validateStudent() {
        Student selected = studentComboBox.getValue();
        if (selected == null) {
            return false;
        }

        // Check if student already has an active IEP (only for new IEPs)
        if (!isEditMode && iepManagementService.hasActiveIEP(selected.getId())) {
            studentErrorLabel.setText("This student already has an active IEP");
            studentErrorLabel.setVisible(true);
            studentErrorLabel.setManaged(true);
            return false;
        }

        studentErrorLabel.setVisible(false);
        studentErrorLabel.setManaged(false);
        return true;
    }

    /**
     * Set IEP for editing
     */
    public void setIEP(IEP iep) {
        this.currentIEP = iep;
        this.isEditMode = true;

        // Populate form fields
        studentComboBox.setValue(iep.getStudent());
        studentComboBox.setDisable(true); // Can't change student on existing IEP
        iepNumberField.setText(iep.getIepNumber());
        statusComboBox.setValue(iep.getStatus());
        startDatePicker.setValue(iep.getStartDate());
        endDatePicker.setValue(iep.getEndDate());
        reviewDatePicker.setValue(iep.getNextReviewDate());

        if (iep.getEligibilityCategory() != null) {
            eligibilityCategoryComboBox.setValue(iep.getEligibilityCategory());
        }
        if (iep.getPrimaryDisability() != null) {
            primaryDisabilityComboBox.setValue(iep.getPrimaryDisability());
        }
        caseManagerField.setText(iep.getCaseManager());
        goalsTextArea.setText(iep.getGoals());
        accommodationsTextArea.setText(iep.getAccommodations());
        notesTextArea.setText(iep.getNotes());

        // Load services
        services.clear();
        if (iep.getServices() != null) {
            services.addAll(iep.getServices());
        }
    }

    /**
     * Get the IEP from form data
     */
    public IEP getIEP() {
        IEP iep = currentIEP != null ? currentIEP : new IEP();

        iep.setStudent(studentComboBox.getValue());
        iep.setIepNumber(iepNumberField.getText().isEmpty() ? null : iepNumberField.getText());
        iep.setStatus(statusComboBox.getValue());
        iep.setStartDate(startDatePicker.getValue());
        iep.setEndDate(endDatePicker.getValue());
        iep.setNextReviewDate(reviewDatePicker.getValue());
        iep.setEligibilityCategory(eligibilityCategoryComboBox.getValue());
        iep.setPrimaryDisability(primaryDisabilityComboBox.getValue());
        iep.setCaseManager(caseManagerField.getText());
        iep.setGoals(goalsTextArea.getText());
        iep.setAccommodations(accommodationsTextArea.getText());
        iep.setNotes(notesTextArea.getText());

        // Set services
        if (iep.getServices() == null) {
            iep.setServices(new ArrayList<>());
        }
        iep.getServices().clear();
        for (IEPService service : services) {
            iep.addService(service);
        }

        return iep;
    }

    /**
     * Validate the form
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (studentComboBox.getValue() == null) {
            errors.add("Student is required");
        }

        if (startDatePicker.getValue() == null) {
            errors.add("Start date is required");
        }

        if (endDatePicker.getValue() == null) {
            errors.add("End date is required");
        }

        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                errors.add("End date must be after start date");
            }
        }

        if (eligibilityCategoryComboBox.getValue() == null ||
            eligibilityCategoryComboBox.getValue().isEmpty()) {
            errors.add("Eligibility category is required");
        }

        if (!validateStudent()) {
            errors.add("Student validation failed");
        }

        return errors;
    }

    // Service management handlers

    @FXML
    private void handleAddService() {
        log.info("Adding new service");
        showInfo("Add Service", "Service dialog will be implemented. For now, services can be added after saving the IEP.");
    }

    private void handleEditService(IEPService service) {
        if (service == null) return;
        log.info("Editing service: {}", service.getId());
        showInfo("Edit Service", "Service editing dialog will be implemented.");
    }

    private void handleDeleteService(IEPService service) {
        if (service == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Service");
        confirm.setHeaderText("Confirm Deletion");
        confirm.setContentText("Are you sure you want to remove this service?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            services.remove(service);
            log.info("Removed service from IEP");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Configure responsive dialog size
     */
    @Override
    protected void configureResponsiveSize() {
        // IEP Dialog is complex with many SPED requirements - use large dialog (70% x 80%)
        configureLargeDialogSize();
    }
}
