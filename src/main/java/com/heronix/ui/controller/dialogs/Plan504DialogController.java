package com.heronix.ui.controller.dialogs;

import com.heronix.model.domain.Plan504;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.Plan504Status;
import com.heronix.repository.StudentRepository;
import com.heronix.service.Plan504Service;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 504 Plan Dialog Controller
 *
 * Controller for creating and editing 504 Plans.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8D - November 21, 2025
 */
@Controller
@Slf4j
public class Plan504DialogController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private Plan504Service plan504Service;

    // Form Fields
    @FXML private ComboBox<Student> studentComboBox;
    @FXML private Label studentErrorLabel;
    @FXML private TextField planNumberField;
    @FXML private ComboBox<Plan504Status> statusComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private DatePicker reviewDatePicker;
    @FXML private ComboBox<String> disabilityComboBox;
    @FXML private TextField coordinatorField;
    @FXML private TextArea accommodationsTextArea;
    @FXML private TextArea notesTextArea;

    private Plan504 currentPlan;
    private boolean isEditMode = false;

    // Common disabilities/conditions for 504 plans
    private static final String[] COMMON_CONDITIONS = {
        "ADHD",
        "Anxiety Disorder",
        "Asthma",
        "Chronic Fatigue Syndrome",
        "Crohn's Disease",
        "Depression",
        "Diabetes",
        "Dyslexia",
        "Epilepsy",
        "Food Allergies",
        "Hearing Impairment",
        "Heart Condition",
        "Juvenile Arthritis",
        "Migraine",
        "OCD",
        "PTSD",
        "Sickle Cell Disease",
        "Tourette Syndrome",
        "Visual Impairment"
    };

    @FXML
    public void initialize() {
        log.info("Initializing 504 Plan Dialog Controller");
        setupStudentComboBox();
        setupStatusComboBox();
        setupDisabilityComboBox();
        setupDateDefaults();
    }

    private void setupStudentComboBox() {
        List<Student> students = studentRepository.findAll();
        studentComboBox.getItems().addAll(students);

        studentComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Student student) {
                if (student == null) return "";
                return student.getFullName() + " (" + student.getStudentId() + ")";
            }

            @Override
            public Student fromString(String string) {
                return null;
            }
        });

        studentComboBox.setOnAction(e -> validateStudent());
    }

    private void setupStatusComboBox() {
        statusComboBox.getItems().addAll(Plan504Status.values());
        statusComboBox.setValue(Plan504Status.DRAFT);
    }

    private void setupDisabilityComboBox() {
        disabilityComboBox.getItems().addAll(COMMON_CONDITIONS);
    }

    private void setupDateDefaults() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusYears(1));
        reviewDatePicker.setValue(LocalDate.now().plusYears(1).minusDays(30));
    }

    private boolean validateStudent() {
        Student selected = studentComboBox.getValue();
        if (selected == null) {
            return false;
        }

        // Check if student already has an active 504 plan
        if (!isEditMode && plan504Service.hasActivePlan(selected.getId())) {
            studentErrorLabel.setText("This student already has an active 504 Plan");
            studentErrorLabel.setVisible(true);
            studentErrorLabel.setManaged(true);
            return false;
        }

        studentErrorLabel.setVisible(false);
        studentErrorLabel.setManaged(false);
        return true;
    }

    /**
     * Set plan for editing
     */
    public void setPlan(Plan504 plan) {
        this.currentPlan = plan;
        this.isEditMode = true;

        studentComboBox.setValue(plan.getStudent());
        studentComboBox.setDisable(true);
        planNumberField.setText(plan.getPlanNumber());
        statusComboBox.setValue(plan.getStatus());
        startDatePicker.setValue(plan.getStartDate());
        endDatePicker.setValue(plan.getEndDate());
        reviewDatePicker.setValue(plan.getNextReviewDate());
        disabilityComboBox.setValue(plan.getDisability());
        coordinatorField.setText(plan.getCoordinator());
        accommodationsTextArea.setText(plan.getAccommodations());
        notesTextArea.setText(plan.getNotes());
    }

    /**
     * Get the plan from form data
     */
    public Plan504 getPlan() {
        Plan504 plan = currentPlan != null ? currentPlan : new Plan504();

        plan.setStudent(studentComboBox.getValue());
        plan.setPlanNumber(planNumberField.getText().isEmpty() ? null : planNumberField.getText());
        plan.setStatus(statusComboBox.getValue());
        plan.setStartDate(startDatePicker.getValue());
        plan.setEndDate(endDatePicker.getValue());
        plan.setNextReviewDate(reviewDatePicker.getValue());
        plan.setDisability(disabilityComboBox.getValue());
        plan.setCoordinator(coordinatorField.getText());
        plan.setAccommodations(accommodationsTextArea.getText());
        plan.setNotes(notesTextArea.getText());

        return plan;
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

        if (disabilityComboBox.getValue() == null || disabilityComboBox.getValue().isEmpty()) {
            errors.add("Disability/condition is required");
        }

        if (accommodationsTextArea.getText() == null || accommodationsTextArea.getText().trim().isEmpty()) {
            errors.add("At least one accommodation is required");
        }

        if (!validateStudent()) {
            errors.add("Student validation failed");
        }

        return errors;
    }
}
