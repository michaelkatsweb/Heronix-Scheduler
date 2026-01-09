package com.heronix.controller;

import com.heronix.model.domain.SchedulerConfiguration;
import com.heronix.model.domain.SpecialCondition;
import com.heronix.model.domain.SpecialCondition.ConditionType;
import com.heronix.repository.SchedulerConfigurationRepository;
import com.heronix.repository.SpecialConditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SchedulerSettingsController
 * Tests business logic and repository interactions
 *
 * Note: This is a JavaFX controller. We test the business logic
 * (configuration management) rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class SchedulerSettingsControllerTest {

    @Autowired
    private SchedulerConfigurationRepository configRepository;

    @Autowired
    private SpecialConditionRepository conditionRepository;

    private SchedulerConfiguration testConfig;

    @BeforeEach
    public void setup() {
        // Create test configuration
        testConfig = new SchedulerConfiguration();
        testConfig.setName("Test Configuration");
        testConfig.setDescription("Test scheduling configuration");
        testConfig.setActive(true);

        // Solver Settings
        testConfig.setMaxSolverMinutes(30);
        testConfig.setUnimprovedSecondsTermination(120);
        testConfig.setEnableMultithreading(true);

        // Teacher Constraints
        testConfig.setMinPeriodsPerTeacher(4);
        testConfig.setMaxPeriodsPerTeacher(7);
        testConfig.setMaxConsecutivePeriods(5);
        testConfig.setMinPlanningPeriods(1);
        testConfig.setMaxPrepsPerTeacher(3);
        testConfig.setAllowBackToBackClasses(true);
        testConfig.setMinimizeTeacherMoves(true);
        testConfig.setRespectTeacherAvailability(true);
        testConfig.setRespectTeacherPreferences(true);

        // Student Constraints
        testConfig.setMaxStudentsPerClass(30);
        testConfig.setMinStudentsPerClass(15);
        testConfig.setMaxBuildingTransitions(2);
        testConfig.setBalanceClassSizes(true);
        testConfig.setHonorStudentRequests(true);
        testConfig.setEnforcePrerequisiteOrder(true);
        testConfig.setMinimizeStudentMoves(true);

        // Special Accommodations
        testConfig.setHonorIepAccommodations(true);
        testConfig.setHonor504Accommodations(true);
        testConfig.setSmallClassForSpecialNeeds(true);
        testConfig.setResourceRoomProximity(true);

        // Time Preferences
        testConfig.setPreferMorningCoreSubjects(true);
        testConfig.setPreferAfternoonElectives(true);
        testConfig.setMinPassingTimeMinutes(5);

        // Constraint Weights
        testConfig.setWeightTeacherConflict(100);
        testConfig.setWeightRoomConflict(90);
        testConfig.setWeightCapacity(80);
        testConfig.setWeightWorkloadBalance(70);
        testConfig.setWeightTeacherQualification(60);
        testConfig.setWeightStudentPreference(50);

        testConfig.setCreatedAt(LocalDateTime.now());
        testConfig.setUpdatedAt(LocalDateTime.now());

        testConfig = configRepository.save(testConfig);
    }

    // ========== CONFIGURATION CRUD ==========

    @Test
    public void testCreateConfiguration_ShouldSaveSuccessfully() {
        // Arrange
        SchedulerConfiguration newConfig = new SchedulerConfiguration();
        newConfig.setName("New Configuration");
        newConfig.setDescription("Newly created configuration");
        newConfig.setActive(false);
        newConfig.setMaxSolverMinutes(45);
        newConfig.setCreatedAt(LocalDateTime.now());

        // Act
        SchedulerConfiguration saved = configRepository.save(newConfig);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("New Configuration", saved.getName());
        assertEquals(45, saved.getMaxSolverMinutes());

        System.out.println("✓ Configuration created successfully");
        System.out.println("  - ID: " + saved.getId());
        System.out.println("  - Name: " + saved.getName());
    }

    @Test
    public void testFindAllConfigurations_ShouldReturnList() {
        // Act
        List<SchedulerConfiguration> configs = configRepository.findAll();

        // Assert
        assertNotNull(configs);
        assertFalse(configs.isEmpty());
        assertTrue(configs.stream().anyMatch(c -> "Test Configuration".equals(c.getName())));

        System.out.println("✓ Found " + configs.size() + " configuration(s)");
    }

    @Test
    public void testFindActiveConfiguration_ShouldReturnActive() {
        // Act
        Optional<SchedulerConfiguration> activeConfig = configRepository.findByActiveTrue();

        // Assert
        assertTrue(activeConfig.isPresent());
        assertEquals("Test Configuration", activeConfig.get().getName());
        assertTrue(activeConfig.get().getActive());

        System.out.println("✓ Active configuration found");
        System.out.println("  - Name: " + activeConfig.get().getName());
    }

    @Test
    public void testUpdateConfiguration_ShouldPersistChanges() {
        // Arrange
        testConfig.setMaxSolverMinutes(60);
        testConfig.setDescription("Updated description");
        testConfig.setUpdatedAt(LocalDateTime.now());

        // Act
        SchedulerConfiguration updated = configRepository.save(testConfig);

        // Assert
        assertEquals(60, updated.getMaxSolverMinutes());
        assertEquals("Updated description", updated.getDescription());

        System.out.println("✓ Configuration updated successfully");
    }

    @Test
    public void testDeleteConfiguration_ShouldRemoveFromDatabase() {
        // Arrange
        Long configId = testConfig.getId();

        // Act
        configRepository.delete(testConfig);

        // Assert
        Optional<SchedulerConfiguration> deleted = configRepository.findById(configId);
        assertFalse(deleted.isPresent());

        System.out.println("✓ Configuration deleted successfully");
    }

    // ========== SOLVER SETTINGS ==========

    @Test
    public void testSolverSettings_ShouldBeConfigurable() {
        // Assert
        assertEquals(30, testConfig.getMaxSolverMinutes());
        assertEquals(120, testConfig.getUnimprovedSecondsTermination());
        assertTrue(testConfig.getEnableMultithreading());

        System.out.println("✓ Solver settings configured correctly");
        System.out.println("  - Max solver time: " + testConfig.getMaxSolverMinutes() + " minutes");
        System.out.println("  - Multithreading: " + testConfig.getEnableMultithreading());
    }

    // ========== TEACHER CONSTRAINTS ==========

    @Test
    public void testTeacherConstraints_ShouldBeConfigurable() {
        // Assert
        assertEquals(4, testConfig.getMinPeriodsPerTeacher());
        assertEquals(7, testConfig.getMaxPeriodsPerTeacher());
        assertEquals(5, testConfig.getMaxConsecutivePeriods());
        assertEquals(1, testConfig.getMinPlanningPeriods());
        assertEquals(3, testConfig.getMaxPrepsPerTeacher());

        System.out.println("✓ Teacher constraints configured correctly");
        System.out.println("  - Periods per teacher: " + testConfig.getMinPeriodsPerTeacher() +
            "-" + testConfig.getMaxPeriodsPerTeacher());
    }

    @Test
    public void testTeacherPreferences_ShouldBeConfigurable() {
        // Assert
        assertTrue(testConfig.getAllowBackToBackClasses());
        assertTrue(testConfig.getMinimizeTeacherMoves());
        assertTrue(testConfig.getRespectTeacherAvailability());
        assertTrue(testConfig.getRespectTeacherPreferences());

        System.out.println("✓ Teacher preferences configured correctly");
    }

    // ========== STUDENT CONSTRAINTS ==========

    @Test
    public void testStudentConstraints_ShouldBeConfigurable() {
        // Assert
        assertEquals(30, testConfig.getMaxStudentsPerClass());
        assertEquals(15, testConfig.getMinStudentsPerClass());
        assertEquals(2, testConfig.getMaxBuildingTransitions());

        System.out.println("✓ Student constraints configured correctly");
        System.out.println("  - Class size: " + testConfig.getMinStudentsPerClass() +
            "-" + testConfig.getMaxStudentsPerClass());
    }

    @Test
    public void testStudentPreferences_ShouldBeConfigurable() {
        // Assert
        assertTrue(testConfig.getBalanceClassSizes());
        assertTrue(testConfig.getHonorStudentRequests());
        assertTrue(testConfig.getEnforcePrerequisiteOrder());
        assertTrue(testConfig.getMinimizeStudentMoves());

        System.out.println("✓ Student preferences configured correctly");
    }

    // ========== SPECIAL ACCOMMODATIONS ==========

    @Test
    public void testSpecialAccommodations_ShouldBeConfigurable() {
        // Assert
        assertTrue(testConfig.getHonorIepAccommodations());
        assertTrue(testConfig.getHonor504Accommodations());
        assertTrue(testConfig.getSmallClassForSpecialNeeds());
        assertTrue(testConfig.getResourceRoomProximity());

        System.out.println("✓ Special accommodations configured correctly");
        System.out.println("  - IEP accommodations: " + testConfig.getHonorIepAccommodations());
        System.out.println("  - 504 accommodations: " + testConfig.getHonor504Accommodations());
    }

    // ========== TIME PREFERENCES ==========

    @Test
    public void testTimePreferences_ShouldBeConfigurable() {
        // Assert
        assertTrue(testConfig.getPreferMorningCoreSubjects());
        assertTrue(testConfig.getPreferAfternoonElectives());
        assertEquals(5, testConfig.getMinPassingTimeMinutes());

        System.out.println("✓ Time preferences configured correctly");
        System.out.println("  - Passing time: " + testConfig.getMinPassingTimeMinutes() + " minutes");
    }

    // ========== CONSTRAINT WEIGHTS ==========

    @Test
    public void testConstraintWeights_ShouldBeConfigurable() {
        // Assert
        assertEquals(100, testConfig.getWeightTeacherConflict());
        assertEquals(90, testConfig.getWeightRoomConflict());
        assertEquals(80, testConfig.getWeightCapacity());
        assertEquals(70, testConfig.getWeightWorkloadBalance());
        assertEquals(60, testConfig.getWeightTeacherQualification());
        assertEquals(50, testConfig.getWeightStudentPreference());

        System.out.println("✓ Constraint weights configured correctly");
        System.out.println("  - Teacher conflict weight: " + testConfig.getWeightTeacherConflict());
    }

    @Test
    public void testConstraintWeights_ShouldBePrioritized() {
        // Assert - weights should be in priority order
        assertTrue(testConfig.getWeightTeacherConflict() > testConfig.getWeightRoomConflict());
        assertTrue(testConfig.getWeightRoomConflict() > testConfig.getWeightCapacity());
        assertTrue(testConfig.getWeightCapacity() > testConfig.getWeightWorkloadBalance());

        System.out.println("✓ Constraint weights properly prioritized");
    }

    // ========== SPECIAL CONDITIONS ==========

    @Test
    public void testSpecialConditions_ShouldBeManageable() {
        // Arrange
        SpecialCondition condition = new SpecialCondition();
        condition.setName("No Friday Afternoon Classes");
        condition.setConditionType(ConditionType.AVOID_TIME);
        condition.setDescription("Teachers prefer no classes on Friday afternoons");
        condition.setActive(true);

        // Act
        SpecialCondition saved = conditionRepository.save(condition);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("No Friday Afternoon Classes", saved.getName());
        assertEquals(ConditionType.AVOID_TIME, saved.getConditionType());
        assertTrue(saved.getActive());

        System.out.println("✓ Special condition created successfully");
        System.out.println("  - Name: " + saved.getName());
        System.out.println("  - Type: " + saved.getConditionType());
    }

    @Test
    public void testFindActiveConditions_ShouldReturnOnlyActive() {
        // Arrange
        SpecialCondition activeCondition = new SpecialCondition();
        activeCondition.setName("Active Condition");
        activeCondition.setConditionType(ConditionType.PREFERRED_TIME);
        activeCondition.setActive(true);
        conditionRepository.save(activeCondition);

        SpecialCondition inactiveCondition = new SpecialCondition();
        inactiveCondition.setName("Inactive Condition");
        inactiveCondition.setConditionType(ConditionType.REQUIRED_TIME);
        inactiveCondition.setActive(false);
        conditionRepository.save(inactiveCondition);

        // Act
        List<SpecialCondition> activeConditions = conditionRepository.findByActiveTrue();

        // Assert
        assertNotNull(activeConditions);
        assertTrue(activeConditions.stream().anyMatch(c -> "Active Condition".equals(c.getName())));
        assertFalse(activeConditions.stream().anyMatch(c -> "Inactive Condition".equals(c.getName())));

        System.out.println("✓ Active conditions filtered correctly");
        System.out.println("  - Found " + activeConditions.size() + " active condition(s)");
    }
}
