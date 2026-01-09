package com.heronix.ui.controller;

import com.heronix.model.domain.Campus;
import com.heronix.model.domain.District;
import com.heronix.service.impl.CampusFederationService;
import com.heronix.service.impl.CampusFederationService.*;
import com.heronix.repository.DistrictRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Multi-Campus Federation Dashboard
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - Multi-Campus Federation
 */
@Slf4j
@Component
public class FederationDashboardController implements Initializable {

    @Autowired
    private CampusFederationService federationService;

    @Autowired
    private DistrictRepository districtRepository;

    // Header
    @FXML private ComboBox<District> districtCombo;
    @FXML private Button refreshButton;
    @FXML private Button newDistrictButton;

    // Summary Cards
    @FXML private Label totalCampusesLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label availableCapacityLabel;
    @FXML private Label utilizationLabel;

    // Tab Pane
    @FXML private TabPane federationTabPane;

    // Campus Tab
    @FXML private TextField campusSearchField;
    @FXML private TableView<CampusSummary> campusTable;
    @FXML private TableColumn<CampusSummary, String> campusCodeColumn;
    @FXML private TableColumn<CampusSummary, String> campusNameColumn;
    @FXML private TableColumn<CampusSummary, String> campusTypeColumn;
    @FXML private TableColumn<CampusSummary, String> gradeLevelsColumn;
    @FXML private TableColumn<CampusSummary, Integer> enrollmentColumn;
    @FXML private TableColumn<CampusSummary, Integer> capacityColumn;
    @FXML private TableColumn<CampusSummary, String> campusUtilColumn;
    @FXML private TableColumn<CampusSummary, String> campusStatusColumn;
    @FXML private TableColumn<CampusSummary, Void> campusActionsColumn;

    // Shared Teachers Tab
    @FXML private Label availableTeachersLabel;
    @FXML private Label currentlySharedLabel;
    @FXML private Label sharingEnabledLabel;
    @FXML private TableView<SharedTeacherInfo> availableTeachersTable;
    @FXML private TableColumn<SharedTeacherInfo, String> teacherNameColumn;
    @FXML private TableColumn<SharedTeacherInfo, String> homeCampusColumn;
    @FXML private TableColumn<SharedTeacherInfo, String> certificationsColumn;
    @FXML private TableColumn<SharedTeacherInfo, Integer> maxPeriodsColumn;
    @FXML private TableColumn<SharedTeacherInfo, Void> shareActionColumn;
    @FXML private ListView<String> sharingAssignmentsList;
    @FXML private Button removeSharingButton;

    // Cross-Enrollment Tab
    @FXML private Label crossEnrolledLabel;
    @FXML private Label availableProgramsLabel;
    @FXML private Label transportationNeededLabel;
    @FXML private TextField studentSearchField;
    @FXML private TableView<CrossCampusEnrollmentOption> enrollmentOptionsTable;
    @FXML private TableColumn<CrossCampusEnrollmentOption, String> optionCampusColumn;
    @FXML private TableColumn<CrossCampusEnrollmentOption, String> optionGradesColumn;
    @FXML private TableColumn<CrossCampusEnrollmentOption, String> optionProgramsColumn;
    @FXML private TableColumn<CrossCampusEnrollmentOption, Integer> optionAvailColumn;
    @FXML private TableColumn<CrossCampusEnrollmentOption, Void> optionEnrollColumn;
    @FXML private ListView<String> crossEnrollmentsList;
    @FXML private Button cancelEnrollmentButton;

    // Capacity Analysis Tab
    @FXML private Label overcrowdedLabel;
    @FXML private Label nearCapacityLabel;
    @FXML private Label healthyCapacityLabel;
    @FXML private Label underutilizedLabel;
    @FXML private TableView<CampusCapacityInfo> capacityTable;
    @FXML private TableColumn<CampusCapacityInfo, String> capCampusColumn;
    @FXML private TableColumn<CampusCapacityInfo, Integer> capEnrollmentColumn;
    @FXML private TableColumn<CampusCapacityInfo, Integer> capMaxColumn;
    @FXML private TableColumn<CampusCapacityInfo, Integer> capAvailableColumn;
    @FXML private TableColumn<CampusCapacityInfo, String> capUtilColumn;
    @FXML private TableColumn<CampusCapacityInfo, String> capStatusColumn;
    @FXML private ListView<String> recommendationsList;
    @FXML private Button applyRecommendationButton;

    // Settings Tab
    @FXML private CheckBox allowCrossEnrollmentCheck;
    @FXML private CheckBox allowSharedTeachersCheck;
    @FXML private CheckBox centralizedSchedulingCheck;
    @FXML private ComboBox<District.CalendarType> calendarTypeCombo;
    @FXML private TextField fiscalYearField;

    private District selectedDistrict;
    private ObservableList<CampusSummary> campusData = FXCollections.observableArrayList();
    private ObservableList<SharedTeacherInfo> teacherData = FXCollections.observableArrayList();
    private ObservableList<CampusCapacityInfo> capacityData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing Federation Dashboard Controller");

        setupDistrictCombo();
        setupCampusTable();
        setupTeachersTable();
        setupCapacityTable();
        setupSettings();

        loadDistricts();
    }

    private void setupDistrictCombo() {
        districtCombo.setCellFactory(lv -> new ListCell<District>() {
            @Override
            protected void updateItem(District item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " (" + item.getDistrictCode() + ")");
            }
        });
        districtCombo.setButtonCell(new ListCell<District>() {
            @Override
            protected void updateItem(District item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select District" : item.getName());
            }
        });
    }

    private void setupCampusTable() {
        campusCodeColumn.setCellValueFactory(new PropertyValueFactory<>("campusCode"));
        campusNameColumn.setCellValueFactory(new PropertyValueFactory<>("campusName"));
        campusTypeColumn.setCellValueFactory(new PropertyValueFactory<>("campusType"));
        gradeLevelsColumn.setCellValueFactory(new PropertyValueFactory<>("gradeLevels"));
        enrollmentColumn.setCellValueFactory(new PropertyValueFactory<>("currentEnrollment"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("maxCapacity"));
        campusUtilColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.1f%%", cellData.getValue().getUtilizationRate())));
        campusStatusColumn.setCellValueFactory(cellData -> {
            double util = cellData.getValue().getUtilizationRate();
            String status = util > 95 ? "Overcrowded" : util > 85 ? "Near Capacity" :
                           util > 50 ? "Healthy" : "Underutilized";
            return new SimpleStringProperty(status);
        });

        campusTable.setItems(campusData);
    }

    private void setupTeachersTable() {
        teacherNameColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        homeCampusColumn.setCellValueFactory(new PropertyValueFactory<>("homeCampusName"));
        certificationsColumn.setCellValueFactory(new PropertyValueFactory<>("certifications"));
        maxPeriodsColumn.setCellValueFactory(new PropertyValueFactory<>("maxPeriodsPerDay"));

        availableTeachersTable.setItems(teacherData);
    }

    private void setupCapacityTable() {
        capCampusColumn.setCellValueFactory(new PropertyValueFactory<>("campusName"));
        capEnrollmentColumn.setCellValueFactory(new PropertyValueFactory<>("currentEnrollment"));
        capMaxColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        capAvailableColumn.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));
        capUtilColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.1f%%", cellData.getValue().getUtilizationPercentage())));
        capStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        capacityTable.setItems(capacityData);
    }

    private void setupSettings() {
        calendarTypeCombo.setItems(FXCollections.observableArrayList(District.CalendarType.values()));
    }

    private void loadDistricts() {
        CompletableFuture.supplyAsync(() -> {
            return federationService.getAllActiveDistricts();
        }).thenAccept(districts -> {
            Platform.runLater(() -> {
                districtCombo.setItems(FXCollections.observableArrayList(districts));
                if (!districts.isEmpty()) {
                    districtCombo.getSelectionModel().selectFirst();
                    handleDistrictChange();
                }
            });
        }).exceptionally(ex -> {
            log.error("Error loading districts", ex);
            return null;
        });
    }

    @FXML
    private void handleDistrictChange() {
        selectedDistrict = districtCombo.getValue();
        if (selectedDistrict != null) {
            loadDistrictData();
        }
    }

    private void loadDistrictData() {
        if (selectedDistrict == null) return;

        Long districtId = selectedDistrict.getId();

        CompletableFuture.supplyAsync(() -> {
            return federationService.getDistrictSummary(districtId);
        }).thenAccept(summary -> {
            Platform.runLater(() -> updateSummaryCards(summary));
        }).exceptionally(ex -> {
            log.error("Error loading district summary", ex);
            return null;
        });

        loadCampuses();
        loadSharedTeachers();
        loadCapacityAnalysis();
        loadSettings();
    }

    private void updateSummaryCards(DistrictSummary summary) {
        totalCampusesLabel.setText(String.valueOf(summary.getTotalCampuses()));
        totalStudentsLabel.setText(String.valueOf(summary.getTotalStudents()));
        int available = summary.getTotalCapacity() - summary.getTotalStudents();
        availableCapacityLabel.setText(String.valueOf(available));
        utilizationLabel.setText(String.format("%.1f%%", summary.getUtilizationRate()));

        campusData.clear();
        campusData.addAll(summary.getCampusSummaries());
    }

    private void loadCampuses() {
        if (selectedDistrict == null) return;

        CompletableFuture.supplyAsync(() -> {
            return federationService.getDistrictSummary(selectedDistrict.getId());
        }).thenAccept(summary -> {
            Platform.runLater(() -> {
                campusData.clear();
                campusData.addAll(summary.getCampusSummaries());
            });
        });
    }

    private void loadSharedTeachers() {
        if (selectedDistrict == null) return;

        CompletableFuture.supplyAsync(() -> {
            return federationService.getAvailableSharedTeachers(selectedDistrict.getId());
        }).thenAccept(teachers -> {
            Platform.runLater(() -> {
                teacherData.clear();
                teacherData.addAll(teachers);
                availableTeachersLabel.setText(String.valueOf(
                    teachers.stream().filter(SharedTeacherInfo::isAvailableForSharing).count()));
                currentlySharedLabel.setText("0"); // Would come from actual sharing assignments
                sharingEnabledLabel.setText(
                    Boolean.TRUE.equals(selectedDistrict.getAllowSharedTeachers()) ? "Yes" : "No");
            });
        });
    }

    private void loadCapacityAnalysis() {
        if (selectedDistrict == null) return;

        CompletableFuture.supplyAsync(() -> {
            return federationService.analyzeDistrictCapacity(selectedDistrict.getId());
        }).thenAccept(analysis -> {
            Platform.runLater(() -> {
                capacityData.clear();
                capacityData.addAll(analysis.getCampusDetails());

                overcrowdedLabel.setText(String.valueOf(analysis.getOvercrowdedCampuses()));
                underutilizedLabel.setText(String.valueOf(analysis.getUnderutilizedCampuses()));

                long nearCap = analysis.getCampusDetails().stream()
                    .filter(c -> "NEAR_CAPACITY".equals(c.getStatus())).count();
                long healthy = analysis.getCampusDetails().stream()
                    .filter(c -> "HEALTHY".equals(c.getStatus())).count();

                nearCapacityLabel.setText(String.valueOf(nearCap));
                healthyCapacityLabel.setText(String.valueOf(healthy));
            });
        });

        // Load recommendations
        CompletableFuture.supplyAsync(() -> {
            return federationService.getResourceSharingRecommendations(selectedDistrict.getId());
        }).thenAccept(recommendations -> {
            Platform.runLater(() -> {
                recommendationsList.getItems().clear();
                for (ResourceSharingRecommendation rec : recommendations) {
                    String item = String.format("[%s] %s - %s",
                        rec.getPriority(), rec.getType(), rec.getReason());
                    recommendationsList.getItems().add(item);
                }
            });
        });
    }

    private void loadSettings() {
        if (selectedDistrict == null) return;

        Platform.runLater(() -> {
            allowCrossEnrollmentCheck.setSelected(
                Boolean.TRUE.equals(selectedDistrict.getAllowCrossCampusEnrollment()));
            allowSharedTeachersCheck.setSelected(
                Boolean.TRUE.equals(selectedDistrict.getAllowSharedTeachers()));
            centralizedSchedulingCheck.setSelected(
                Boolean.TRUE.equals(selectedDistrict.getCentralizedScheduling()));
            calendarTypeCombo.setValue(selectedDistrict.getCalendarType());
            fiscalYearField.setText(selectedDistrict.getFiscalYear());
        });
    }

    @FXML
    private void handleRefresh() {
        loadDistrictData();
    }

    @FXML
    private void handleNewDistrict() {
        // Show dialog to create new district
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New District");
        dialog.setHeaderText("Create a New District");
        dialog.setContentText("District Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                District newDistrict = District.builder()
                    .name(name)
                    .districtCode(generateDistrictCode(name))
                    .active(true)
                    .build();

                try {
                    District created = federationService.createDistrict(newDistrict);
                    loadDistricts();
                    showInfo("District created: " + created.getName());
                } catch (Exception e) {
                    showError("Failed to create district: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleAddCampus() {
        if (selectedDistrict == null) {
            showError("Please select a district first");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Campus");
        dialog.setHeaderText("Create a New Campus in " + selectedDistrict.getName());
        dialog.setContentText("Campus Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Campus newCampus = Campus.builder()
                    .name(name)
                    .campusCode(generateCampusCode(name))
                    .campusType(Campus.CampusType.COMPREHENSIVE)
                    .active(true)
                    .build();

                try {
                    Campus created = federationService.createCampus(selectedDistrict.getId(), newCampus);
                    loadCampuses();
                    showInfo("Campus created: " + created.getName());
                } catch (Exception e) {
                    showError("Failed to create campus: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRemoveSharing() {
        // Remove selected sharing assignment
        String selected = sharingAssignmentsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            sharingAssignmentsList.getItems().remove(selected);
            showInfo("Sharing assignment removed");
        }
    }

    @FXML
    private void handleStudentSearch() {
        String searchText = studentSearchField.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            // Would search for student and load cross-enrollment options
            log.info("Searching for student: {}", searchText);
        }
    }

    @FXML
    private void handleNewCrossEnrollment() {
        showInfo("Cross-enrollment wizard would open here");
    }

    @FXML
    private void handleCancelEnrollment() {
        String selected = crossEnrollmentsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            crossEnrollmentsList.getItems().remove(selected);
            showInfo("Cross-enrollment cancelled");
        }
    }

    @FXML
    private void handleApplyRecommendation() {
        String selected = recommendationsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showInfo("Recommendation would be applied: " + selected);
        }
    }

    @FXML
    private void handleResetSettings() {
        if (selectedDistrict != null) {
            loadSettings();
        }
    }

    @FXML
    private void handleSaveSettings() {
        if (selectedDistrict == null) return;

        selectedDistrict.setAllowCrossCampusEnrollment(allowCrossEnrollmentCheck.isSelected());
        selectedDistrict.setAllowSharedTeachers(allowSharedTeachersCheck.isSelected());
        selectedDistrict.setCentralizedScheduling(centralizedSchedulingCheck.isSelected());
        selectedDistrict.setCalendarType(calendarTypeCombo.getValue());
        selectedDistrict.setFiscalYear(fiscalYearField.getText());

        try {
            districtRepository.save(selectedDistrict);
            showInfo("Settings saved successfully");
        } catch (Exception e) {
            showError("Failed to save settings: " + e.getMessage());
        }
    }

    private String generateDistrictCode(String name) {
        String code = name.toUpperCase().replaceAll("[^A-Z]", "");
        return code.length() > 4 ? code.substring(0, 4) : code;
    }

    private String generateCampusCode(String name) {
        String code = name.toUpperCase().replaceAll("[^A-Z]", "");
        return code.length() > 6 ? code.substring(0, 6) : code;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
