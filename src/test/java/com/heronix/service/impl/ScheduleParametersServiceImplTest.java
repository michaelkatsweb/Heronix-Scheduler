package com.heronix.service.impl;

import com.heronix.model.dto.ScheduleParameters;
import com.heronix.model.enums.ScheduleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ScheduleParametersServiceImpl
 *
 * Tests JSON-based parameter persistence and default value generation.
 */
class ScheduleParametersServiceImplTest {

    private ScheduleParametersServiceImpl service;
    private File paramsFile;

    @BeforeEach
    void setUp() {
        service = new ScheduleParametersServiceImpl();

        // Get the params file location for cleanup
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            userHome = System.getProperty("java.io.tmpdir");
        }
        File configDir = new File(userHome, ".eduscheduler");
        paramsFile = new File(configDir, "schedule_parameters.json");
    }

    @AfterEach
    void tearDown() {
        // Clean up test file
        if (paramsFile != null && paramsFile.exists()) {
            paramsFile.delete();
        }
    }

    // ========== DEFAULT PARAMETERS TESTS ==========

    @Test
    void testGetDefaultParameters_ShouldReturnValidDefaults() {
        ScheduleParameters params = service.getDefaultParameters();

        assertNotNull(params);
        assertEquals(LocalTime.of(8, 0), params.getSchoolStartTime());
        assertEquals(LocalTime.of(15, 0), params.getSchoolEndTime());
        assertEquals(ScheduleType.TRADITIONAL, params.getScheduleType());
        assertEquals(50, params.getPeriodDuration());
        assertEquals(7, params.getPeriodsPerDay());
        assertTrue(params.isLunchEnabled());
        assertEquals(LocalTime.of(12, 0), params.getLunchStartTime());
        assertEquals(30, params.getLunchDuration());
        assertEquals(1, params.getLunchWaves());
        assertEquals(5, params.getPassingPeriodDuration());
    }

    @Test
    void testGetDefaultParameters_ShouldSetTeacherConstraints() {
        ScheduleParameters params = service.getDefaultParameters();

        assertNotNull(params);
        assertEquals(3, params.getMaxConsecutiveHours());
        assertEquals(6, params.getMaxClassesPerDay());
        assertEquals(8, params.getMaxDailyHours());
        assertEquals(1, params.getMinPrepPeriods());
        assertTrue(params.isRequireLunchBreak());
    }

    @Test
    void testGetDefaultParameters_ShouldSetRoomSettings() {
        ScheduleParameters params = service.getDefaultParameters();

        assertNotNull(params);
        assertEquals(30, params.getDefaultRoomCapacity());
        assertEquals(10.0, params.getCapacityBufferPercent());
        assertFalse(params.isAllowRoomSharing());
    }

    @Test
    void testGetDefaultParameters_ShouldBeConsistent() {
        ScheduleParameters params1 = service.getDefaultParameters();
        ScheduleParameters params2 = service.getDefaultParameters();

        assertNotNull(params1);
        assertNotNull(params2);
        assertEquals(params1.getSchoolStartTime(), params2.getSchoolStartTime());
        assertEquals(params1.getScheduleType(), params2.getScheduleType());
        assertEquals(params1.getPeriodDuration(), params2.getPeriodDuration());
    }

    // ========== SAVE PARAMETERS TESTS ==========

    @Test
    void testSaveParameters_WithValidParameters_ShouldSaveSuccessfully() {
        ScheduleParameters params = new ScheduleParameters();
        params.setSchoolStartTime(LocalTime.of(9, 0));
        params.setSchoolEndTime(LocalTime.of(16, 0));
        params.setScheduleType(ScheduleType.BLOCK);
        params.setPeriodDuration(90);
        params.setPeriodsPerDay(4);

        service.saveParameters(params);

        assertTrue(paramsFile.exists());
        assertTrue(paramsFile.length() > 0);
    }

    @Test
    void testSaveParameters_WithNullParameters_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> service.saveParameters(null));
    }

    @Test
    void testSaveParameters_ShouldOverwriteExistingFile() {
        // Save first parameters
        ScheduleParameters params1 = new ScheduleParameters();
        params1.setSchoolStartTime(LocalTime.of(8, 0));
        params1.setScheduleType(ScheduleType.TRADITIONAL);

        service.saveParameters(params1);
        long firstFileSize = paramsFile.length();

        // Save different parameters
        ScheduleParameters params2 = new ScheduleParameters();
        params2.setSchoolStartTime(LocalTime.of(9, 0));
        params2.setScheduleType(ScheduleType.BLOCK);
        params2.setPeriodDuration(90);

        service.saveParameters(params2);

        assertTrue(paramsFile.exists());
        // File size might differ due to different values
        assertNotEquals(0, paramsFile.length());
    }

    // ========== LOAD PARAMETERS TESTS ==========

    @Test
    void testLoadParameters_WithNoSavedFile_ShouldReturnDefaults() {
        // Ensure no file exists
        if (paramsFile.exists()) {
            paramsFile.delete();
        }

        ScheduleParameters params = service.loadParameters();

        assertNotNull(params);
        assertEquals(LocalTime.of(8, 0), params.getSchoolStartTime());
        assertEquals(ScheduleType.TRADITIONAL, params.getScheduleType());
    }

    @Test
    void testLoadParameters_AfterSave_ShouldReturnSavedValues() {
        ScheduleParameters savedParams = new ScheduleParameters();
        savedParams.setSchoolStartTime(LocalTime.of(7, 30));
        savedParams.setSchoolEndTime(LocalTime.of(14, 30));
        savedParams.setScheduleType(ScheduleType.BLOCK);
        savedParams.setPeriodDuration(90);
        savedParams.setPeriodsPerDay(4);
        savedParams.setLunchEnabled(true);
        savedParams.setLunchStartTime(LocalTime.of(11, 30));
        savedParams.setLunchDuration(45);

        service.saveParameters(savedParams);

        ScheduleParameters loadedParams = service.loadParameters();

        assertNotNull(loadedParams);
        assertEquals(LocalTime.of(7, 30), loadedParams.getSchoolStartTime());
        assertEquals(LocalTime.of(14, 30), loadedParams.getSchoolEndTime());
        assertEquals(ScheduleType.BLOCK, loadedParams.getScheduleType());
        assertEquals(90, loadedParams.getPeriodDuration());
        assertEquals(4, loadedParams.getPeriodsPerDay());
        assertTrue(loadedParams.isLunchEnabled());
        assertEquals(LocalTime.of(11, 30), loadedParams.getLunchStartTime());
        assertEquals(45, loadedParams.getLunchDuration());
    }

    @Test
    void testLoadParameters_WithComplexParameters_ShouldPreserveAllValues() {
        ScheduleParameters savedParams = service.getDefaultParameters();
        savedParams.setMaxConsecutiveHours(4);
        savedParams.setMaxClassesPerDay(7);
        savedParams.setMaxDailyHours(9);
        savedParams.setMinPrepPeriods(2);
        savedParams.setRequireLunchBreak(false);
        savedParams.setDefaultRoomCapacity(25);
        savedParams.setCapacityBufferPercent(15.0);
        savedParams.setAllowRoomSharing(true);

        service.saveParameters(savedParams);

        ScheduleParameters loadedParams = service.loadParameters();

        assertNotNull(loadedParams);
        assertEquals(4, loadedParams.getMaxConsecutiveHours());
        assertEquals(7, loadedParams.getMaxClassesPerDay());
        assertEquals(9, loadedParams.getMaxDailyHours());
        assertEquals(2, loadedParams.getMinPrepPeriods());
        assertFalse(loadedParams.isRequireLunchBreak());
        assertEquals(25, loadedParams.getDefaultRoomCapacity());
        assertEquals(15.0, loadedParams.getCapacityBufferPercent());
        assertTrue(loadedParams.isAllowRoomSharing());
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testSaveAndLoad_MultipleTimes_ShouldMaintainConsistency() {
        // First save/load cycle
        ScheduleParameters params1 = new ScheduleParameters();
        params1.setSchoolStartTime(LocalTime.of(8, 15));
        params1.setScheduleType(ScheduleType.BLOCK);

        service.saveParameters(params1);
        ScheduleParameters loaded1 = service.loadParameters();

        assertEquals(LocalTime.of(8, 15), loaded1.getSchoolStartTime());
        assertEquals(ScheduleType.BLOCK, loaded1.getScheduleType());

        // Second save/load cycle
        ScheduleParameters params2 = new ScheduleParameters();
        params2.setSchoolStartTime(LocalTime.of(9, 0));
        params2.setScheduleType(ScheduleType.TRADITIONAL);

        service.saveParameters(params2);
        ScheduleParameters loaded2 = service.loadParameters();

        assertEquals(LocalTime.of(9, 0), loaded2.getSchoolStartTime());
        assertEquals(ScheduleType.TRADITIONAL, loaded2.getScheduleType());
    }

    @Test
    void testLoadParameters_WithPartialData_ShouldHandleGracefully() {
        // Save minimal parameters
        ScheduleParameters params = new ScheduleParameters();
        params.setSchoolStartTime(LocalTime.of(8, 0));
        params.setScheduleType(ScheduleType.TRADITIONAL);

        service.saveParameters(params);

        ScheduleParameters loaded = service.loadParameters();

        assertNotNull(loaded);
        assertEquals(LocalTime.of(8, 0), loaded.getSchoolStartTime());
        assertEquals(ScheduleType.TRADITIONAL, loaded.getScheduleType());
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void testDefaultParameters_EdgeTimes_ShouldBeValid() {
        ScheduleParameters params = service.getDefaultParameters();

        assertNotNull(params);
        assertTrue(params.getSchoolStartTime().isBefore(params.getSchoolEndTime()));
        assertTrue(params.getLunchStartTime().isAfter(params.getSchoolStartTime()));
        assertTrue(params.getLunchStartTime().isBefore(params.getSchoolEndTime()));
    }

    @Test
    void testSaveParameters_WithEdgeValues_ShouldHandle() {
        ScheduleParameters params = new ScheduleParameters();
        params.setSchoolStartTime(LocalTime.MIN);
        params.setSchoolEndTime(LocalTime.MAX);
        params.setPeriodDuration(1);
        params.setPeriodsPerDay(1);
        params.setLunchDuration(0);
        params.setPassingPeriodDuration(0);

        // Should not throw exception
        assertDoesNotThrow(() -> service.saveParameters(params));
    }

    @Test
    void testSaveParameters_WithLargeValues_ShouldHandle() {
        ScheduleParameters params = new ScheduleParameters();
        params.setPeriodDuration(999);
        params.setPeriodsPerDay(100);
        params.setMaxClassesPerDay(50);
        params.setDefaultRoomCapacity(1000);
        params.setCapacityBufferPercent(100.0);

        assertDoesNotThrow(() -> service.saveParameters(params));
    }

    // ========== FILE SYSTEM TESTS ==========

    @Test
    void testSaveParameters_CreatesDirectoryIfNeeded() {
        // Directory should be created in constructor
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            userHome = System.getProperty("java.io.tmpdir");
        }
        File configDir = new File(userHome, ".eduscheduler");

        assertTrue(configDir.exists());
        assertTrue(configDir.isDirectory());
    }

    @Test
    void testLoadParameters_WithCorruptedFile_ShouldReturnDefaults() {
        // Create corrupted file
        try {
            java.nio.file.Files.write(paramsFile.toPath(), "{ invalid json }".getBytes());
        } catch (Exception e) {
            fail("Failed to create test file: " + e.getMessage());
        }

        ScheduleParameters params = service.loadParameters();

        assertNotNull(params);
        // Should return defaults due to corruption
        assertEquals(ScheduleType.TRADITIONAL, params.getScheduleType());
    }
}
