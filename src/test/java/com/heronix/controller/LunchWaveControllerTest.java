package com.heronix.controller;

import com.heronix.controller.LunchWaveController.*;
import com.heronix.model.domain.LunchWave;
import com.heronix.model.domain.Schedule;
import com.heronix.model.enums.SchedulePeriod;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.LunchWaveRepository;
import com.heronix.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LunchWaveController
 * Tests lunch wave management REST API endpoints
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class LunchWaveControllerTest {

    @Autowired
    private LunchWaveController lunchWaveController;

    @Autowired
    private LunchWaveRepository lunchWaveRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private Schedule testSchedule;
    private LunchWave testWave1;
    private LunchWave testWave2;

    @BeforeEach
    public void setup() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setScheduleName("Test Schedule - Lunch Waves");
        testSchedule.setStartDate(LocalDate.now());
        testSchedule.setEndDate(LocalDate.now().plusMonths(6));
        testSchedule.setPeriod(SchedulePeriod.SEMESTER);
        testSchedule.setScheduleType(ScheduleType.TRADITIONAL);
        testSchedule.setActive(true);
        testSchedule = scheduleRepository.save(testSchedule);

        // Create first test lunch wave
        testWave1 = new LunchWave();
        testWave1.setSchedule(testSchedule);
        testWave1.setWaveName("Lunch Wave 1");
        testWave1.setWaveOrder(1);
        testWave1.setStartTime(LocalTime.of(10, 4));
        testWave1.setEndTime(LocalTime.of(10, 34));
        testWave1.setMaxCapacity(250);
        testWave1.setCurrentAssignments(0);
        testWave1.setIsActive(true);
        testWave1 = lunchWaveRepository.save(testWave1);

        // Create second test lunch wave (inactive)
        testWave2 = new LunchWave();
        testWave2.setSchedule(testSchedule);
        testWave2.setWaveName("Lunch Wave 2");
        testWave2.setWaveOrder(2);
        testWave2.setStartTime(LocalTime.of(10, 58));
        testWave2.setEndTime(LocalTime.of(11, 28));
        testWave2.setMaxCapacity(250);
        testWave2.setCurrentAssignments(100);
        testWave2.setIsActive(false);
        testWave2 = lunchWaveRepository.save(testWave2);
    }

    // ========== QUERY OPERATIONS ==========

    @Test
    public void testGetLunchWaves_WithValidScheduleId_ShouldReturnWaves() {
        // Act
        ResponseEntity<List<LunchWave>> response =
            lunchWaveController.getLunchWaves(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 2, "Should return at least two waves");

        System.out.println("✓ Lunch waves retrieved successfully");
        System.out.println("  - Total waves: " + response.getBody().size());
    }

    @Test
    public void testGetLunchWaveById_WithValidId_ShouldReturnWave() {
        // Act
        ResponseEntity<LunchWave> response =
            lunchWaveController.getLunchWaveById(testWave1.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lunch Wave 1", response.getBody().getWaveName());
        assertEquals(1, response.getBody().getWaveOrder());
        assertEquals(250, response.getBody().getMaxCapacity());

        System.out.println("✓ Lunch wave retrieved by ID successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Name: " + response.getBody().getWaveName());
        System.out.println("  - Capacity: " + response.getBody().getMaxCapacity());
    }

    @Test
    public void testGetLunchWaveById_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<LunchWave> response =
            lunchWaveController.getLunchWaveById(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Non-existent lunch wave correctly returns 404");
    }

    @Test
    public void testGetActiveLunchWaves_ShouldReturnOnlyActiveWaves() {
        // Act
        ResponseEntity<List<LunchWave>> response =
            lunchWaveController.getActiveLunchWaves(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 1, "Should have at least one active wave");

        // Verify all returned waves are active
        boolean allActive = response.getBody().stream().allMatch(LunchWave::getIsActive);
        assertTrue(allActive, "All returned waves should be active");

        System.out.println("✓ Active lunch waves retrieved successfully");
        System.out.println("  - Active waves: " + response.getBody().size());
    }

    @Test
    public void testGetAvailableLunchWaves_ShouldReturnWavesWithCapacity() {
        // Act
        ResponseEntity<List<LunchWave>> response =
            lunchWaveController.getAvailableLunchWaves(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        System.out.println("✓ Available lunch waves retrieved successfully");
        System.out.println("  - Available waves: " + response.getBody().size());
    }

    @Test
    public void testGetLunchWaveStats_ShouldReturnStatistics() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            lunchWaveController.getLunchWaveStats(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> stats = response.getBody();
        assertTrue(stats.containsKey("totalCapacity"));
        assertTrue(stats.containsKey("totalAssignments"));
        assertTrue(stats.containsKey("utilizationPercent"));
        assertTrue(stats.containsKey("activeWaveCount"));
        assertTrue(stats.containsKey("isConfigurationValid"));

        System.out.println("✓ Lunch wave statistics retrieved");
        System.out.println("  - Total Capacity: " + stats.get("totalCapacity"));
        System.out.println("  - Total Assignments: " + stats.get("totalAssignments"));
        System.out.println("  - Active Waves: " + stats.get("activeWaveCount"));
    }

    // ========== CREATE OPERATIONS ==========

    @Test
    public void testCreateLunchWave_WithValidData_ShouldSucceed() {
        // Arrange
        CreateLunchWaveRequest request = new CreateLunchWaveRequest();
        request.setScheduleId(testSchedule.getId());
        request.setWaveName("Lunch Wave 3");
        request.setWaveOrder(3);
        request.setStartTime(LocalTime.of(11, 52));
        request.setEndTime(LocalTime.of(12, 22));
        request.setMaxCapacity(250);

        // Act
        ResponseEntity<LunchWave> response = lunchWaveController.createLunchWave(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("Lunch Wave 3", response.getBody().getWaveName());
        assertEquals(3, response.getBody().getWaveOrder());
        assertEquals(250, response.getBody().getMaxCapacity());

        System.out.println("✓ Lunch wave created successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Name: " + response.getBody().getWaveName());
    }

    @Test
    public void testCreateLunchWavesBulk_WithWeekiWacheeTemplate_ShouldSucceed() {
        // Arrange
        CreateBulkLunchWavesRequest request = new CreateBulkLunchWavesRequest();
        request.setScheduleId(testSchedule.getId());
        request.setTemplate("WEEKI_WACHEE");

        // Act
        ResponseEntity<List<LunchWave>> response =
            lunchWaveController.createLunchWavesBulk(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() > 0, "Should create at least one wave");

        System.out.println("✓ Bulk lunch waves created with Weeki Wachee template");
        System.out.println("  - Waves created: " + response.getBody().size());
    }

    @Test
    public void testCreateLunchWavesBulk_WithParrottMSTemplate_ShouldSucceed() {
        // Arrange
        CreateBulkLunchWavesRequest request = new CreateBulkLunchWavesRequest();
        request.setScheduleId(testSchedule.getId());
        request.setTemplate("PARROTT_MS");

        // Act
        ResponseEntity<List<LunchWave>> response =
            lunchWaveController.createLunchWavesBulk(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() > 0, "Should create at least one wave");

        System.out.println("✓ Bulk lunch waves created with Parrott MS template");
        System.out.println("  - Waves created: " + response.getBody().size());
    }

    @Test
    public void testCreateLunchWavesBulk_WithCustomConfig_ShouldSucceed() {
        // Arrange
        CustomConfig customConfig = new CustomConfig();
        customConfig.setWaveCount(3);
        customConfig.setFirstLunchStart(LocalTime.of(10, 0));
        customConfig.setLunchDuration(30);
        customConfig.setGapBetweenLunches(24);
        customConfig.setCapacity(200);

        CreateBulkLunchWavesRequest request = new CreateBulkLunchWavesRequest();
        request.setScheduleId(testSchedule.getId());
        request.setTemplate("CUSTOM");
        request.setCustomConfig(customConfig);

        // Act
        ResponseEntity<List<LunchWave>> response =
            lunchWaveController.createLunchWavesBulk(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size(), "Should create exactly 3 waves");

        System.out.println("✓ Bulk lunch waves created with custom config");
        System.out.println("  - Waves created: " + response.getBody().size());
    }

    @Test
    public void testCreateLunchWavesBulk_WithInvalidTemplate_ShouldReturn400() {
        // Arrange
        CreateBulkLunchWavesRequest request = new CreateBulkLunchWavesRequest();
        request.setScheduleId(testSchedule.getId());
        request.setTemplate("INVALID_TEMPLATE");

        // Act
        ResponseEntity<List<LunchWave>> response =
            lunchWaveController.createLunchWavesBulk(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        System.out.println("✓ Invalid template correctly returns 400");
    }

    // ========== UPDATE OPERATIONS ==========

    @Test
    public void testUpdateLunchWave_WithValidData_ShouldSucceed() {
        // Arrange
        UpdateLunchWaveRequest request = new UpdateLunchWaveRequest();
        request.setWaveName("Updated Lunch Wave 1");
        request.setMaxCapacity(300);

        // Act
        ResponseEntity<LunchWave> response =
            lunchWaveController.updateLunchWave(testWave1.getId(), request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Lunch Wave 1", response.getBody().getWaveName());
        assertEquals(300, response.getBody().getMaxCapacity());

        System.out.println("✓ Lunch wave updated successfully");
        System.out.println("  - New name: " + response.getBody().getWaveName());
        System.out.println("  - New capacity: " + response.getBody().getMaxCapacity());
    }

    @Test
    public void testUpdateLunchWave_WithNonExistentId_ShouldReturn404() {
        // Arrange
        UpdateLunchWaveRequest request = new UpdateLunchWaveRequest();
        request.setWaveName("Test");

        // Act
        ResponseEntity<LunchWave> response =
            lunchWaveController.updateLunchWave(99999L, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Update non-existent wave correctly returns 404");
    }

    @Test
    public void testUpdateCapacity_WithValidCapacity_ShouldSucceed() {
        // Act
        ResponseEntity<LunchWave> response =
            lunchWaveController.updateCapacity(testWave1.getId(), Map.of("capacity", 350));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(350, response.getBody().getMaxCapacity());

        System.out.println("✓ Capacity updated successfully");
        System.out.println("  - New capacity: " + response.getBody().getMaxCapacity());
    }

    @Test
    public void testUpdateCapacity_WithNegativeCapacity_ShouldReturn400() {
        // Act
        ResponseEntity<LunchWave> response =
            lunchWaveController.updateCapacity(testWave1.getId(), Map.of("capacity", -10));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        System.out.println("✓ Negative capacity correctly returns 400");
    }

    @Test
    public void testUpdateTimes_WithValidTimes_ShouldSucceed() {
        // Arrange
        UpdateTimesRequest request = new UpdateTimesRequest();
        request.setStartTime(LocalTime.of(10, 15));
        request.setEndTime(LocalTime.of(10, 45));

        // Act
        ResponseEntity<LunchWave> response =
            lunchWaveController.updateTimes(testWave1.getId(), request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(LocalTime.of(10, 15), response.getBody().getStartTime());
        assertEquals(LocalTime.of(10, 45), response.getBody().getEndTime());

        System.out.println("✓ Times updated successfully");
        System.out.println("  - New start: " + response.getBody().getStartTime());
        System.out.println("  - New end: " + response.getBody().getEndTime());
    }

    @Test
    public void testActivateWave_WithValidId_ShouldSucceed() {
        // Act (activate the inactive wave)
        ResponseEntity<LunchWave> response =
            lunchWaveController.activateWave(testWave2.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsActive(), "Wave should be active");

        System.out.println("✓ Wave activated successfully");
        System.out.println("  - Wave ID: " + response.getBody().getId());
        System.out.println("  - Active: " + response.getBody().getIsActive());
    }

    @Test
    public void testDeactivateWave_WithValidId_ShouldSucceed() {
        // Act (deactivate the active wave)
        ResponseEntity<LunchWave> response =
            lunchWaveController.deactivateWave(testWave1.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getIsActive(), "Wave should be inactive");

        System.out.println("✓ Wave deactivated successfully");
        System.out.println("  - Wave ID: " + response.getBody().getId());
        System.out.println("  - Active: " + response.getBody().getIsActive());
    }

    // ========== DELETE OPERATIONS ==========

    @Test
    public void testDeleteLunchWave_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<Void> response =
            lunchWaveController.deleteLunchWave(testWave1.getId());

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify deletion
        boolean exists = lunchWaveRepository.existsById(testWave1.getId());
        assertFalse(exists, "Wave should be deleted");

        System.out.println("✓ Lunch wave deleted successfully");
    }

    @Test
    public void testDeleteLunchWave_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Void> response =
            lunchWaveController.deleteLunchWave(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Delete non-existent wave correctly returns 404");
    }

    @Test
    public void testDeleteAllLunchWaves_ShouldDeleteAllWavesForSchedule() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            lunchWaveController.deleteAllLunchWaves(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("deletedCount"));

        int deletedCount = (int) response.getBody().get("deletedCount");
        assertTrue(deletedCount >= 2, "Should delete at least 2 waves");

        System.out.println("✓ All lunch waves deleted for schedule");
        System.out.println("  - Deleted count: " + deletedCount);
    }
}
