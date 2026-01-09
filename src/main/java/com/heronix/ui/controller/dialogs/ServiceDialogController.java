package com.heronix.ui.controller.dialogs;

import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Room;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.DeliveryModel;
import com.heronix.model.enums.ServiceStatus;
import com.heronix.model.enums.ServiceType;
import com.heronix.repository.RoomRepository;
import com.heronix.repository.TeacherRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * Service Dialog Controller
 *
 * Controller for creating and editing IEP services.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8D - November 21, 2025
 */
@Controller
@Slf4j
public class ServiceDialogController {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    // Form Fields
    @FXML private ComboBox<ServiceType> serviceTypeComboBox;
    @FXML private Label serviceDescriptionLabel;
    @FXML private Spinner<Integer> minutesPerWeekSpinner;
    @FXML private Spinner<Integer> sessionDurationSpinner;
    @FXML private ComboBox<String> frequencyComboBox;
    @FXML private ComboBox<DeliveryModel> deliveryModelComboBox;
    @FXML private Label deliveryDescriptionLabel;
    @FXML private ComboBox<String> locationComboBox;
    @FXML private ComboBox<Teacher> assignedStaffComboBox;
    @FXML private TextArea descriptionTextArea;

    private IEPService currentService;
    private boolean isEditMode = false;

    // Common frequency options
    private static final String[] FREQUENCY_OPTIONS = {
        "1x per week",
        "2x per week",
        "3x per week",
        "4x per week",
        "5x per week (Daily)",
        "Every other day",
        "Monthly"
    };

    @FXML
    public void initialize() {
        log.info("Initializing Service Dialog Controller");
        setupServiceTypeComboBox();
        setupDeliveryModelComboBox();
        setupFrequencyComboBox();
        setupLocationComboBox();
        setupStaffComboBox();
        setupSpinners();
    }

    private void setupServiceTypeComboBox() {
        serviceTypeComboBox.getItems().addAll(ServiceType.values());

        serviceTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ServiceType type) {
                return type == null ? "" : type.getDisplayName();
            }

            @Override
            public ServiceType fromString(String string) {
                return null;
            }
        });

        // Update description when service type changes
        serviceTypeComboBox.setOnAction(e -> {
            ServiceType selected = serviceTypeComboBox.getValue();
            if (selected != null) {
                serviceDescriptionLabel.setText(selected.getDescription());
            } else {
                serviceDescriptionLabel.setText("");
            }
        });
    }

    private void setupDeliveryModelComboBox() {
        deliveryModelComboBox.getItems().addAll(DeliveryModel.values());

        deliveryModelComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DeliveryModel model) {
                return model == null ? "" : model.getDisplayName();
            }

            @Override
            public DeliveryModel fromString(String string) {
                return null;
            }
        });

        // Update description when delivery model changes
        deliveryModelComboBox.setOnAction(e -> {
            DeliveryModel selected = deliveryModelComboBox.getValue();
            if (selected != null) {
                deliveryDescriptionLabel.setText(selected.getDescription());
            } else {
                deliveryDescriptionLabel.setText("");
            }
        });

        // Default to individual
        deliveryModelComboBox.setValue(DeliveryModel.INDIVIDUAL);
    }

    private void setupFrequencyComboBox() {
        frequencyComboBox.getItems().addAll(FREQUENCY_OPTIONS);
        frequencyComboBox.setValue("2x per week");
    }

    private void setupLocationComboBox() {
        // Add common SPED locations
        locationComboBox.getItems().addAll(
            "Speech Room",
            "OT Room",
            "PT Room",
            "Counseling Office",
            "Resource Room",
            "Sensory Room",
            "General Classroom"
        );

        // Also add rooms from database
        try {
            List<Room> rooms = roomRepository.findAll();
            for (Room room : rooms) {
                String roomName = room.getRoomNumber();
                if (!locationComboBox.getItems().contains(roomName)) {
                    locationComboBox.getItems().add(roomName);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load rooms for location dropdown: {}", e.getMessage());
        }
    }

    private void setupStaffComboBox() {
        try {
            List<Teacher> teachers = teacherRepository.findAllActive();
            assignedStaffComboBox.getItems().add(null); // Allow no assignment
            assignedStaffComboBox.getItems().addAll(teachers);

            assignedStaffComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(Teacher teacher) {
                    if (teacher == null) return "(Not assigned)";
                    return teacher.getName();
                }

                @Override
                public Teacher fromString(String string) {
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("Could not load staff for dropdown: {}", e.getMessage());
        }
    }

    private void setupSpinners() {
        // Minutes per week spinner (15 to 600 minutes, default 60)
        SpinnerValueFactory<Integer> minutesFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 600, 60, 15);
        minutesPerWeekSpinner.setValueFactory(minutesFactory);

        // Session duration spinner (10 to 120 minutes, default 30)
        SpinnerValueFactory<Integer> durationFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 120, 30, 5);
        sessionDurationSpinner.setValueFactory(durationFactory);
    }

    /**
     * Set service for editing
     */
    public void setService(IEPService service) {
        this.currentService = service;
        this.isEditMode = true;

        serviceTypeComboBox.setValue(service.getServiceType());
        minutesPerWeekSpinner.getValueFactory().setValue(service.getMinutesPerWeek());
        sessionDurationSpinner.getValueFactory().setValue(service.getSessionDurationMinutes());

        if (service.getFrequency() != null) {
            frequencyComboBox.setValue(service.getFrequency());
        }

        deliveryModelComboBox.setValue(service.getDeliveryModel());

        if (service.getLocation() != null) {
            locationComboBox.setValue(service.getLocation());
        }

        assignedStaffComboBox.setValue(service.getAssignedStaff());
        descriptionTextArea.setText(service.getServiceDescription());
    }

    /**
     * Get the service from form data
     */
    public IEPService getService() {
        IEPService service = currentService != null ? currentService : new IEPService();

        service.setServiceType(serviceTypeComboBox.getValue());
        service.setMinutesPerWeek(minutesPerWeekSpinner.getValue());
        service.setSessionDurationMinutes(sessionDurationSpinner.getValue());
        service.setFrequency(frequencyComboBox.getValue());
        service.setDeliveryModel(deliveryModelComboBox.getValue());
        service.setLocation(locationComboBox.getValue());
        service.setAssignedStaff(assignedStaffComboBox.getValue());
        service.setServiceDescription(descriptionTextArea.getText());

        // Set default status for new services
        if (service.getStatus() == null) {
            service.setStatus(ServiceStatus.NOT_SCHEDULED);
        }

        return service;
    }

    /**
     * Validate the form
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (serviceTypeComboBox.getValue() == null) {
            errors.add("Service type is required");
        }

        if (minutesPerWeekSpinner.getValue() == null || minutesPerWeekSpinner.getValue() < 15) {
            errors.add("Minutes per week must be at least 15");
        }

        if (sessionDurationSpinner.getValue() == null || sessionDurationSpinner.getValue() < 10) {
            errors.add("Session duration must be at least 10 minutes");
        }

        if (deliveryModelComboBox.getValue() == null) {
            errors.add("Delivery model is required");
        }

        // Validate session duration vs minutes per week
        if (minutesPerWeekSpinner.getValue() != null && sessionDurationSpinner.getValue() != null) {
            int weekly = minutesPerWeekSpinner.getValue();
            int session = sessionDurationSpinner.getValue();
            if (session > weekly) {
                errors.add("Session duration cannot exceed weekly minutes");
            }
        }

        return errors;
    }

    /**
     * Check if this is edit mode
     */
    public boolean isEditMode() {
        return isEditMode;
    }
}
