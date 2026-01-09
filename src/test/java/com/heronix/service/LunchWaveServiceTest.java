package com.heronix.service;

import com.heronix.model.domain.LunchWave;
import com.heronix.model.domain.Schedule;
import com.heronix.model.dto.ScheduleGenerationRequest;
import com.heronix.model.enums.SchedulePeriod;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.LunchWaveRepository;
import com.heronix.repository.ScheduleRepository;
import com.heronix.service.impl.LunchWaveServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for LunchWaveService
 *
 * Phase 5E: Testing & Validation
 *
 * Tests:
 * - Lunch wave creation (single, template, custom)
 * - Lunch wave updates (capacity, times, activation)
 * - Lunch wave queries (active, available, by grade)
 * - Statistics and validation
 * - Edge cases and error handling
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-01
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "logging.level.com.eduscheduler=DEBUG"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LunchWaveServiceTest {

    @Autowired
    private LunchWaveService lunchWaveService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    // Test data
    private Schedule testSchedule;

    @BeforeAll
    void setUp() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setName("Test Schedule - Lunch Waves");
        testSchedule.setPeriod(SchedulePeriod.MASTER);
        testSchedule.setScheduleType(ScheduleType.TRADITIONAL);
        testSchedule.setStartDate(LocalDate.of(2025, 8, 18));
        testSchedule.setEndDate(LocalDate.of(2025, 12, 20));
        testSchedule = scheduleRepository.save(testSchedule);
    }

    @AfterEach
    void cleanUpAfterEach() {
        // Delete all lunch waves after each test
        lunchWaveService.deleteAllLunchWaves(testSchedule.getId());
    }

    // ========== Creation Tests ==========

    @Test
    @Order(1)
    @DisplayName("Create single lunch wave")
    void testCreateLunchWave() {
        // Given
        String waveName = "Lunch 1";
        LocalTime startTime = LocalTime.of(10, 4);
        LocalTime endTime = LocalTime.of(10, 34);
        int capacity = 250;

        // When
        LunchWave wave = lunchWaveService.createLunchWave(
            testSchedule, waveName, 1, startTime, endTime, capacity
        );

        // Then
        assertThat(wave).isNotNull();
        assertThat(wave.getId()).isNotNull();
        assertThat(wave.getWaveName()).isEqualTo(waveName);
        assertThat(wave.getWaveOrder()).isEqualTo(1);
        assertThat(wave.getStartTime()).isEqualTo(startTime);
        assertThat(wave.getEndTime()).isEqualTo(endTime);
        assertThat(wave.getMaxCapacity()).isEqualTo(capacity);
        assertThat(wave.getCurrentAssignments()).isEqualTo(0);
        assertThat(wave.getIsActive()).isTrue();
        assertThat(wave.getGradeLevelRestriction()).isNull();
    }

    @Test
    @Order(2)
    @DisplayName("Create lunch wave with grade restriction")
    void testCreateLunchWaveWithGradeRestriction() {
        // Given
        Integer gradeLevel = 9;

        // When
        LunchWave wave = lunchWaveService.createLunchWave(
            testSchedule, "9th Grade Lunch", 1,
            LocalTime.of(11, 0), LocalTime.of(11, 30),
            300, gradeLevel
        );

        // Then
        assertThat(wave).isNotNull();
        assertThat(wave.getGradeLevelRestriction()).isEqualTo(gradeLevel);
    }

    @Test
    @Order(3)
    @DisplayName("Create Weeki Wachee template (3 waves)")
    void testCreateWeekiWacheeTemplate() {
        // When
        List<LunchWave> waves = lunchWaveService.createWeekiWacheeTemplate(testSchedule);

        // Then
        assertThat(waves).hasSize(3);

        // Verify Lunch 1
        LunchWave lunch1 = waves.get(0);
        assertThat(lunch1.getWaveName()).isEqualTo("Lunch 1");
        assertThat(lunch1.getWaveOrder()).isEqualTo(1);
        assertThat(lunch1.getStartTime()).isEqualTo(LocalTime.of(10, 4));
        assertThat(lunch1.getEndTime()).isEqualTo(LocalTime.of(10, 34));
        assertThat(lunch1.getMaxCapacity()).isEqualTo(250);

        // Verify Lunch 2
        LunchWave lunch2 = waves.get(1);
        assertThat(lunch2.getWaveName()).isEqualTo("Lunch 2");
        assertThat(lunch2.getWaveOrder()).isEqualTo(2);
        assertThat(lunch2.getStartTime()).isEqualTo(LocalTime.of(10, 58));

        // Verify Lunch 3
        LunchWave lunch3 = waves.get(2);
        assertThat(lunch3.getWaveName()).isEqualTo("Lunch 3");
        assertThat(lunch3.getWaveOrder()).isEqualTo(3);
        assertThat(lunch3.getStartTime()).isEqualTo(LocalTime.of(11, 52));
    }

    @Test
    @Order(4)
    @DisplayName("Create Parrott MS template (grade-level lunches)")
    void testCreateParrottMSTemplate() {
        // When
        List<LunchWave> waves = lunchWaveService.createParrottMSTemplate(testSchedule);

        // Then
        assertThat(waves).hasSize(3);

        // Verify 6th grade lunch
        LunchWave lunch6th = waves.get(0);
        assertThat(lunch6th.getWaveName()).isEqualTo("6th Grade Lunch");
        assertThat(lunch6th.getGradeLevelRestriction()).isEqualTo(6);
        assertThat(lunch6th.getMaxCapacity()).isEqualTo(300);

        // Verify 7th grade lunch
        LunchWave lunch7th = waves.get(1);
        assertThat(lunch7th.getGradeLevelRestriction()).isEqualTo(7);

        // Verify 8th grade lunch
        LunchWave lunch8th = waves.get(2);
        assertThat(lunch8th.getGradeLevelRestriction()).isEqualTo(8);
    }

    @Test
    @Order(5)
    @DisplayName("Create custom lunch waves")
    void testCreateCustomLunchWaves() {
        // Given
        int waveCount = 4;
        LocalTime firstLunchStart = LocalTime.of(11, 15);
        int lunchDuration = 25;
        int gapBetweenLunches = 20;
        int capacity = 200;

        // When
        List<LunchWave> waves = lunchWaveService.createCustomLunchWaves(
            testSchedule, waveCount, firstLunchStart, lunchDuration, gapBetweenLunches, capacity
        );

        // Then
        assertThat(waves).hasSize(4);

        // Verify wave 1 times
        LunchWave wave1 = waves.get(0);
        assertThat(wave1.getStartTime()).isEqualTo(LocalTime.of(11, 15));
        assertThat(wave1.getEndTime()).isEqualTo(LocalTime.of(11, 40)); // +25 min
        assertThat(wave1.getMaxCapacity()).isEqualTo(200);

        // Verify wave 2 starts after gap
        LunchWave wave2 = waves.get(1);
        assertThat(wave2.getStartTime()).isEqualTo(LocalTime.of(12, 0)); // 11:40 + 20 min gap
        assertThat(wave2.getEndTime()).isEqualTo(LocalTime.of(12, 25));
    }

    // ========== Update Tests ==========

    @Test
    @Order(10)
    @DisplayName("Update lunch wave capacity")
    void testUpdateCapacity() {
        // Given
        LunchWave wave = lunchWaveService.createLunchWave(
            testSchedule, "Lunch 1", 1,
            LocalTime.of(10, 0), LocalTime.of(10, 30), 250
        );
        int newCapacity = 300;

        // When
        LunchWave updated = lunchWaveService.updateCapacity(wave.getId(), newCapacity);

        // Then
        assertThat(updated.getMaxCapacity()).isEqualTo(newCapacity);
    }

    @Test
    @Order(11)
    @DisplayName("Update lunch wave times")
    void testUpdateTimes() {
        // Given
        LunchWave wave = lunchWaveService.createLunchWave(
            testSchedule, "Lunch 1", 1,
            LocalTime.of(10, 0), LocalTime.of(10, 30), 250
        );
        LocalTime newStart = LocalTime.of(10, 15);
        LocalTime newEnd = LocalTime.of(10, 45);

        // When
        LunchWave updated = lunchWaveService.updateTimes(wave.getId(), newStart, newEnd);

        // Then
        assertThat(updated.getStartTime()).isEqualTo(newStart);
        assertThat(updated.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    @Order(12)
    @DisplayName("Activate lunch wave")
    void testActivateWave() {
        // Given
        LunchWave wave = lunchWaveService.createLunchWave(
            testSchedule, "Lunch 1", 1,
            LocalTime.of(10, 0), LocalTime.of(10, 30), 250
        );
        wave.setIsActive(false);
        wave = lunchWaveService.updateLunchWave(wave);

        // When
        LunchWave activated = lunchWaveService.activateWave(wave.getId());

        // Then
        assertThat(activated.getIsActive()).isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("Deactivate lunch wave")
    void testDeactivateWave() {
        // Given
        LunchWave wave = lunchWaveService.createLunchWave(
            testSchedule, "Lunch 1", 1,
            LocalTime.of(10, 0), LocalTime.of(10, 30), 250
        );

        // When
        LunchWave deactivated = lunchWaveService.deactivateWave(wave.getId());

        // Then
        assertThat(deactivated.getIsActive()).isFalse();
    }

    // ========== Query Tests ==========

    @Test
    @Order(20)
    @DisplayName("Get all lunch waves for schedule")
    void testGetAllLunchWaves() {
        // Given
        lunchWaveService.createWeekiWacheeTemplate(testSchedule);

        // When
        List<LunchWave> waves = lunchWaveService.getAllLunchWaves(testSchedule.getId());

        // Then
        assertThat(waves).hasSize(3);
        assertThat(waves).isSortedAccordingTo((w1, w2) ->
            Integer.compare(w1.getWaveOrder(), w2.getWaveOrder())
        );
    }

    @Test
    @Order(21)
    @DisplayName("Get active lunch waves only")
    void testGetActiveLunchWaves() {
        // Given
        List<LunchWave> allWaves = lunchWaveService.createWeekiWacheeTemplate(testSchedule);
        LunchWave wave = allWaves.get(0);
        lunchWaveService.deactivateWave(wave.getId());

        // When
        List<LunchWave> activeWaves = lunchWaveService.getActiveLunchWaves(testSchedule.getId());

        // Then
        assertThat(activeWaves).hasSize(2);
        assertThat(activeWaves).noneMatch(w -> w.getId().equals(wave.getId()));
    }

    @Test
    @Order(22)
    @DisplayName("Get available lunch waves (not full)")
    void testGetAvailableLunchWaves() {
        // Given
        List<LunchWave> waves = lunchWaveService.createWeekiWacheeTemplate(testSchedule);

        // Simulate wave 1 being full
        LunchWave wave1 = waves.get(0);
        wave1.setCurrentAssignments(wave1.getMaxCapacity());
        lunchWaveService.updateLunchWave(wave1);

        // When
        List<LunchWave> available = lunchWaveService.getAvailableLunchWaves(testSchedule.getId());

        // Then
        assertThat(available).hasSize(2);
        assertThat(available).noneMatch(w -> w.getId().equals(wave1.getId()));
    }

    @Test
    @Order(23)
    @DisplayName("Find wave with most capacity")
    void testFindWaveWithMostCapacity() {
        // Given
        List<LunchWave> waves = lunchWaveService.createWeekiWacheeTemplate(testSchedule);

        // Wave 1: 50/250 = 200 available
        waves.get(0).setCurrentAssignments(50);
        lunchWaveService.updateLunchWave(waves.get(0));

        // Wave 2: 100/250 = 150 available
        waves.get(1).setCurrentAssignments(100);
        lunchWaveService.updateLunchWave(waves.get(1));

        // Wave 3: 30/250 = 220 available (most)
        waves.get(2).setCurrentAssignments(30);
        lunchWaveService.updateLunchWave(waves.get(2));

        // When
        Optional<LunchWave> waveWithMost = lunchWaveService.findWaveWithMostCapacity(testSchedule.getId());

        // Then
        assertThat(waveWithMost).isPresent();
        assertThat(waveWithMost.get().getId()).isEqualTo(waves.get(2).getId());
        assertThat(waveWithMost.get().getAvailableSeats()).isEqualTo(220);
    }

    @Test
    @Order(24)
    @DisplayName("Get available waves for specific grade level")
    void testGetAvailableWavesForGradeLevel() {
        // Given
        List<LunchWave> waves = lunchWaveService.createParrottMSTemplate(testSchedule);
        // Parrott template has grade restrictions: 6th, 7th, 8th

        // When
        List<LunchWave> waves6th = lunchWaveService.getAvailableWavesForGradeLevel(testSchedule.getId(), 6);
        List<LunchWave> waves7th = lunchWaveService.getAvailableWavesForGradeLevel(testSchedule.getId(), 7);

        // Then
        assertThat(waves6th).hasSize(1);
        assertThat(waves6th.get(0).getGradeLevelRestriction()).isEqualTo(6);

        assertThat(waves7th).hasSize(1);
        assertThat(waves7th.get(0).getGradeLevelRestriction()).isEqualTo(7);
    }

    // ========== Statistics Tests ==========

    @Test
    @Order(30)
    @DisplayName("Get total capacity across all waves")
    void testGetTotalCapacity() {
        // Given
        lunchWaveService.createWeekiWacheeTemplate(testSchedule); // 3 waves @ 250 = 750

        // When
        int totalCapacity = lunchWaveService.getTotalCapacity(testSchedule.getId());

        // Then
        assertThat(totalCapacity).isEqualTo(750);
    }

    @Test
    @Order(31)
    @DisplayName("Get total assignments across all waves")
    void testGetTotalAssignments() {
        // Given
        List<LunchWave> waves = lunchWaveService.createWeekiWacheeTemplate(testSchedule);
        waves.get(0).setCurrentAssignments(245);
        waves.get(1).setCurrentAssignments(248);
        waves.get(2).setCurrentAssignments(242);
        waves.forEach(w -> lunchWaveService.updateLunchWave(w));

        // When
        int totalAssignments = lunchWaveService.getTotalAssignments(testSchedule.getId());

        // Then
        assertThat(totalAssignments).isEqualTo(735); // 245 + 248 + 242
    }

    @Test
    @Order(32)
    @DisplayName("Get overall utilization percentage")
    void testGetOverallUtilization() {
        // Given
        List<LunchWave> waves = lunchWaveService.createWeekiWacheeTemplate(testSchedule);
        waves.get(0).setCurrentAssignments(245);
        waves.get(1).setCurrentAssignments(248);
        waves.get(2).setCurrentAssignments(242);
        waves.forEach(w -> lunchWaveService.updateLunchWave(w));

        // When
        double utilization = lunchWaveService.getOverallUtilization(testSchedule.getId());

        // Then
        assertThat(utilization).isCloseTo(98.0, within(0.1)); // 735/750 = 98%
    }

    @Test
    @Order(33)
    @DisplayName("Validate lunch wave configuration - valid")
    void testIsLunchWaveConfigurationValid_Valid() {
        // Given
        lunchWaveService.createWeekiWacheeTemplate(testSchedule);

        // When
        boolean isValid = lunchWaveService.isLunchWaveConfigurationValid(testSchedule.getId());

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @Order(34)
    @DisplayName("Validate lunch wave configuration - overlapping times")
    void testIsLunchWaveConfigurationValid_OverlappingTimes() {
        // Given
        lunchWaveService.createLunchWave(testSchedule, "Lunch 1", 1,
            LocalTime.of(10, 0), LocalTime.of(10, 30), 250);
        lunchWaveService.createLunchWave(testSchedule, "Lunch 2", 2,
            LocalTime.of(10, 20), LocalTime.of(10, 50), 250); // Overlaps with Lunch 1

        // When
        boolean isValid = lunchWaveService.isLunchWaveConfigurationValid(testSchedule.getId());

        // Then
        assertThat(isValid).isFalse();
    }

    // ========== Delete Tests ==========

    @Test
    @Order(40)
    @DisplayName("Delete single lunch wave")
    void testDeleteLunchWave() {
        // Given
        LunchWave wave = lunchWaveService.createLunchWave(
            testSchedule, "Lunch 1", 1,
            LocalTime.of(10, 0), LocalTime.of(10, 30), 250
        );
        Long waveId = wave.getId();

        // When
        lunchWaveService.deleteLunchWave(waveId);

        // Then
        Optional<LunchWave> deleted = lunchWaveService.getLunchWaveById(waveId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @Order(41)
    @DisplayName("Delete all lunch waves for schedule")
    void testDeleteAllLunchWaves() {
        // Given
        lunchWaveService.createWeekiWacheeTemplate(testSchedule);
        assertThat(lunchWaveService.getAllLunchWaves(testSchedule.getId())).hasSize(3);

        // When
        lunchWaveService.deleteAllLunchWaves(testSchedule.getId());

        // Then
        List<LunchWave> remaining = lunchWaveService.getAllLunchWaves(testSchedule.getId());
        assertThat(remaining).isEmpty();
    }

    // ========== Edge Cases ==========

    @Test
    @Order(50)
    @DisplayName("Update non-existent wave throws exception")
    void testUpdateNonExistentWave() {
        // When/Then
        assertThatThrownBy(() -> lunchWaveService.updateCapacity(99999L, 300))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @Order(51)
    @DisplayName("Empty schedule has no lunch waves")
    void testEmptyScheduleHasNoLunchWaves() {
        // When
        List<LunchWave> waves = lunchWaveService.getAllLunchWaves(testSchedule.getId());

        // Then
        assertThat(waves).isEmpty();
        assertThat(lunchWaveService.hasLunchWaves(testSchedule.getId())).isFalse();
    }

    @Test
    @Order(52)
    @DisplayName("Schedule has lunch waves after creation")
    void testScheduleHasLunchWaves() {
        // Given
        lunchWaveService.createWeekiWacheeTemplate(testSchedule);

        // When/Then
        assertThat(lunchWaveService.hasLunchWaves(testSchedule.getId())).isTrue();
        assertThat(lunchWaveService.countActiveLunchWaves(testSchedule.getId())).isEqualTo(3);
    }

    @AfterAll
    void tearDown() {
        // Clean up test schedule
        if (testSchedule != null && testSchedule.getId() != null) {
            lunchWaveService.deleteAllLunchWaves(testSchedule.getId());
            scheduleRepository.delete(testSchedule);
        }
    }
}
