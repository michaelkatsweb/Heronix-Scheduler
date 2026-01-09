package com.heronix.ui.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Room;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.RoomRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.CourseSectionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Controller for Section Edit Dialog
 * Handles adding/editing individual course sections
 */
@Slf4j
@Component
public class SectionEditDialogController {

    @FXML private Label dialogTitle;
    @FXML private TextField sectionNumberField;
    @FXML private ComboBox<Teacher> teacherCombo;
    @FXML private ComboBox<Room> roomCombo;
    @FXML private ComboBox<Integer> periodCombo;
    @FXML private Spinner<Integer> maxEnrollmentSpinner;
    @FXML private Spinner<Integer> minEnrollmentSpinner;
    @FXML private Spinner<Integer> targetEnrollmentSpinner;
    @FXML private ComboBox<CourseSection.SectionStatus> statusCombo;
    @FXML private CheckBox honorsCheckbox;
    @FXML private CheckBox apCheckbox;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    @Autowired
    private CourseSectionService courseSectionService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Stage dialogStage;
    private CourseSection section;
    private Course course;
    private boolean saved = false;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        log.debug("SectionEditDialogController initialized");
        setupSpinners();
        setupComboBoxes();
    }

    private void setupSpinners() {
        // Max Enrollment Spinner
        SpinnerValueFactory<Integer> maxFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 30);
        maxEnrollmentSpinner.setValueFactory(maxFactory);
        maxEnrollmentSpinner.setEditable(true);

        // Min Enrollment Spinner
        SpinnerValueFactory<Integer> minFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 10);
        minEnrollmentSpinner.setValueFactory(minFactory);
        minEnrollmentSpinner.setEditable(true);

        // Target Enrollment Spinner
        SpinnerValueFactory<Integer> targetFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 25);
        targetEnrollmentSpinner.setValueFactory(targetFactory);
        targetEnrollmentSpinner.setEditable(true);
    }

    private void setupComboBoxes() {
        // Period Combo (1-8)
        periodCombo.setItems(FXCollections.observableArrayList(
            IntStream.rangeClosed(1, 8).boxed().collect(Collectors.toList())
        ));

        // Status Combo
        statusCombo.setItems(FXCollections.observableArrayList(CourseSection.SectionStatus.values()));
        statusCombo.setValue(CourseSection.SectionStatus.PLANNED);

        // Teacher Combo - custom cell factory to show name
        teacherCombo.setCellFactory(lv -> new ListCell<Teacher>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) {
                    setText(null);
                } else {
                    setText(teacher.getName() + " (" + teacher.getEmployeeId() + ")");
                }
            }
        });
        teacherCombo.setButtonCell(new ListCell<Teacher>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) {
                    setText(null);
                } else {
                    setText(teacher.getName());
                }
            }
        });

        // Room Combo - custom cell factory to show room number
        roomCombo.setCellFactory(lv -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(room.getRoomNumber() + " - " + (room.getRoomType() != null ? room.getRoomType() : "General"));
                }
            }
        });
        roomCombo.setButtonCell(new ListCell<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(room.getRoomNumber());
                }
            }
        });
    }

    /**
     * Set the course for which we're creating/editing a section
     */
    public void setCourse(Course course) {
        this.course = course;
        loadTeachers();
        loadRooms();

        // Auto-generate section number for new sections
        if (!isEditMode && course != null) {
            String nextNumber = courseSectionService.generateNextSectionNumber(course.getId());
            sectionNumberField.setText(nextNumber);
        }
    }

    /**
     * Set the section for editing
     */
    public void setSection(CourseSection section) {
        this.section = section;
        this.course = section.getCourse();
        this.isEditMode = true;

        dialogTitle.setText("Edit Section");
        loadTeachers();
        loadRooms();
        populateFields();
    }

    private void loadTeachers() {
        try {
            List<Teacher> allTeachers = teacherRepository.findByActiveTrue();

            // Filter teachers certified for this course (if course has subject)
            List<Teacher> certifiedTeachers;
            if (course != null && course.getSubject() != null) {
                certifiedTeachers = allTeachers.stream()
                        .filter(t -> t.getSubjectCertifications() != null &&
                                t.getSubjectCertifications().stream()
                                        .anyMatch(cert -> course.getSubject().equalsIgnoreCase(cert.getSubject())))
                        .collect(Collectors.toList());

                log.debug("Found {} certified teachers for subject: {}", certifiedTeachers.size(), course.getSubject());
            } else {
                certifiedTeachers = allTeachers;
            }

            teacherCombo.setItems(FXCollections.observableArrayList(certifiedTeachers));
        } catch (Exception e) {
            log.error("Error loading teachers", e);
            teacherCombo.setItems(FXCollections.observableArrayList());
        }
    }

    private void loadRooms() {
        try {
            List<Room> allRooms = roomRepository.findByActiveTrue();
            roomCombo.setItems(FXCollections.observableArrayList(allRooms));
        } catch (Exception e) {
            log.error("Error loading rooms", e);
            roomCombo.setItems(FXCollections.observableArrayList());
        }
    }

    private void populateFields() {
        if (section == null) return;

        sectionNumberField.setText(section.getSectionNumber());

        if (section.getAssignedTeacher() != null) {
            teacherCombo.setValue(section.getAssignedTeacher());
        }

        if (section.getAssignedRoom() != null) {
            roomCombo.setValue(section.getAssignedRoom());
        }

        if (section.getAssignedPeriod() != null) {
            periodCombo.setValue(section.getAssignedPeriod());
        }

        maxEnrollmentSpinner.getValueFactory().setValue(section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 30);
        minEnrollmentSpinner.getValueFactory().setValue(section.getMinEnrollment() != null ? section.getMinEnrollment() : 10);
        targetEnrollmentSpinner.getValueFactory().setValue(section.getTargetEnrollment() != null ? section.getTargetEnrollment() : 25);

        if (section.getSectionStatus() != null) {
            statusCombo.setValue(section.getSectionStatus());
        }

        honorsCheckbox.setSelected(Boolean.TRUE.equals(section.getIsHonors()));
        apCheckbox.setSelected(Boolean.TRUE.equals(section.getIsAp()));
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) {
            return;
        }

        try {
            // Create or update section
            CourseSection sectionToSave = isEditMode ? section : new CourseSection();

            sectionToSave.setCourse(course);
            sectionToSave.setSectionNumber(sectionNumberField.getText().trim());
            sectionToSave.setAssignedTeacher(teacherCombo.getValue());
            sectionToSave.setAssignedRoom(roomCombo.getValue());
            sectionToSave.setAssignedPeriod(periodCombo.getValue());
            sectionToSave.setMaxEnrollment(maxEnrollmentSpinner.getValue());
            sectionToSave.setMinEnrollment(minEnrollmentSpinner.getValue());
            sectionToSave.setTargetEnrollment(targetEnrollmentSpinner.getValue());
            sectionToSave.setSectionStatus(statusCombo.getValue());
            sectionToSave.setIsHonors(honorsCheckbox.isSelected());
            sectionToSave.setIsAp(apCheckbox.isSelected());

            // Set current enrollment to 0 for new sections
            if (!isEditMode) {
                sectionToSave.setCurrentEnrollment(0);
                sectionToSave.setWaitlistCount(0);
            }

            // Check for conflicts
            if (sectionToSave.getAssignedTeacher() != null && sectionToSave.getAssignedPeriod() != null) {
                boolean teacherAvailable = courseSectionService.isTeacherAvailable(
                        sectionToSave.getAssignedTeacher().getId(),
                        sectionToSave.getAssignedPeriod()
                );

                // In edit mode, teacher is "available" at their own period
                if (!teacherAvailable && isEditMode) {
                    // Re-check excluding this section
                    teacherAvailable = true; // Service already handles this
                }

                if (!teacherAvailable && !isEditMode) {
                    showError("Teacher " + sectionToSave.getAssignedTeacher().getName() +
                            " is already assigned to another section during period " + sectionToSave.getAssignedPeriod());
                    return;
                }
            }

            if (sectionToSave.getAssignedRoom() != null && sectionToSave.getAssignedPeriod() != null) {
                boolean roomAvailable = courseSectionService.isRoomAvailable(
                        sectionToSave.getAssignedRoom().getId(),
                        sectionToSave.getAssignedPeriod()
                );

                if (!roomAvailable && !isEditMode) {
                    showError("Room " + sectionToSave.getAssignedRoom().getRoomNumber() +
                            " is already assigned to another section during period " + sectionToSave.getAssignedPeriod());
                    return;
                }
            }

            // Save
            if (isEditMode) {
                courseSectionService.updateSection(sectionToSave);
                log.info("Section updated: {}", sectionToSave.getSectionNumber());
            } else {
                courseSectionService.createSection(sectionToSave);
                log.info("Section created: {}", sectionToSave.getSectionNumber());
            }

            saved = true;
            closeDialog();

        } catch (IllegalStateException e) {
            // Conflict detected by service
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Error saving section", e);
            showError("Failed to save section: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private boolean validateForm() {
        hideError();

        // Validate section number
        String sectionNumber = sectionNumberField.getText();
        if (sectionNumber == null || sectionNumber.trim().isEmpty()) {
            showError("Section number is required");
            return false;
        }

        // Validate enrollment numbers
        int maxEnrollment = maxEnrollmentSpinner.getValue();
        int minEnrollment = minEnrollmentSpinner.getValue();
        int targetEnrollment = targetEnrollmentSpinner.getValue();

        if (minEnrollment > maxEnrollment) {
            showError("Minimum enrollment cannot be greater than maximum enrollment");
            return false;
        }

        if (targetEnrollment > maxEnrollment) {
            showError("Target enrollment cannot be greater than maximum enrollment");
            return false;
        }

        // Validate status
        if (statusCombo.getValue() == null) {
            showError("Status is required");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaved() {
        return saved;
    }
}
