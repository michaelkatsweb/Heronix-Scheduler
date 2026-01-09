package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.repository.LunchPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for LunchPeriodServiceImpl
 *
 * Service: 28th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/LunchPeriodServiceImplTest.java
 *
 * Tests cover:
 * - CRUD operations for lunch periods
 * - Active/inactive lunch period filtering
 * - Lunch period queries by group, day, grade level
 * - Time conflict detection
 * - Lunch wave assignment and distribution
 * - Capacity validation
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class LunchPeriodServiceImplTest {

    @Mock(lenient = true)
    private LunchPeriodRepository lunchPeriodRepository;

    @InjectMocks
    private LunchPeriodServiceImpl service;

    private LunchPeriod testLunchPeriod;
    private Student testStudent;
    private Teacher testTeacher;
    private Schedule testSchedule;
    private ScheduleSlot testSlot;
    private LunchConfiguration testConfig;

    @BeforeEach
    void setUp() {
        // Create test lunch period
        testLunchPeriod = new LunchPeriod();
        testLunchPeriod.setId(1L);
        testLunchPeriod.setName("Lunch A");
        testLunchPeriod.setLunchGroup("Wave 1");
        testLunchPeriod.setStartTime(LocalTime.of(11, 30));
        testLunchPeriod.setEndTime(LocalTime.of(12, 0));
        testLunchPeriod.setMaxStudents(150);
        testLunchPeriod.setLocation("Cafeteria");
        testLunchPeriod.setActive(true);
        testLunchPeriod.setDayOfWeek(1); // Monday
        testLunchPeriod.setPriority(1);

        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Alice");
        testStudent.setLastName("Johnson");
        testStudent.setGradeLevel("9");
        testStudent.setScheduleSlots(new ArrayList<>());

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("Mr. Smith");
        testTeacher.setScheduleSlots(new ArrayList<>());

        // Create test schedule slot
        testSlot = new ScheduleSlot();
        testSlot.setId(1L);
        testSlot.setIsLunchPeriod(true);
        testSlot.setLunchWaveNumber(1);
        testSlot.setStudents(new ArrayList<>());

        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setName("Test Schedule");
        testSchedule.setSlots(new ArrayList<>());

        // Create test lunch configuration
        testConfig = new LunchConfiguration();
        testConfig.setId(1L);
        testConfig.setMaxStudentsPerPeriod(150);
        testConfig.setNumberOfLunchPeriods(3);
        testConfig.setStaggerByGrade(false);
    }

    // ========== CREATE TESTS ==========

    @Test
    void testCreateLunchPeriod_WithValidData_ShouldCreate() {
        when(lunchPeriodRepository.existsByName("Lunch A")).thenReturn(false);
        when(lunchPeriodRepository.save(any(LunchPeriod.class))).thenAnswer(i -> i.getArgument(0));

        LunchPeriod result = service.createLunchPeriod(testLunchPeriod);

        assertNotNull(result);
        assertEquals("Lunch A", result.getName());
        verify(lunchPeriodRepository).save(testLunchPeriod);
    }

    @Test
    void testCreateLunchPeriod_WithDuplicateName_ShouldThrowException() {
        when(lunchPeriodRepository.existsByName("Lunch A")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            service.createLunchPeriod(testLunchPeriod);
        });

        verify(lunchPeriodRepository, never()).save(any());
    }

    // ========== READ TESTS ==========

    @Test
    void testGetLunchPeriodById_WithExistingId_ShouldReturn() {
        when(lunchPeriodRepository.findById(1L)).thenReturn(Optional.of(testLunchPeriod));

        Optional<LunchPeriod> result = service.getLunchPeriodById(1L);

        assertTrue(result.isPresent());
        assertEquals("Lunch A", result.get().getName());
    }

    @Test
    void testGetLunchPeriodById_WithNonExistentId_ShouldReturnEmpty() {
        when(lunchPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<LunchPeriod> result = service.getLunchPeriodById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetAllLunchPeriods_ShouldReturnAll() {
        List<LunchPeriod> periods = Arrays.asList(testLunchPeriod);
        when(lunchPeriodRepository.findAll()).thenReturn(periods);

        List<LunchPeriod> result = service.getAllLunchPeriods();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetActiveLunchPeriods_ShouldReturnOnlyActive() {
        when(lunchPeriodRepository.findByActiveTrue()).thenReturn(Arrays.asList(testLunchPeriod));

        List<LunchPeriod> result = service.getActiveLunchPeriods();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void testGetLunchPeriodsByGroup_ShouldReturnMatchingGroup() {
        when(lunchPeriodRepository.findByLunchGroup("Wave 1")).thenReturn(Arrays.asList(testLunchPeriod));

        List<LunchPeriod> result = service.getLunchPeriodsByGroup("Wave 1");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Wave 1", result.get(0).getLunchGroup());
    }

    @Test
    void testGetLunchPeriodsForDay_ShouldReturnMatchingDay() {
        when(lunchPeriodRepository.findByDayOfWeek(1)).thenReturn(Arrays.asList(testLunchPeriod));

        List<LunchPeriod> result = service.getLunchPeriodsForDay(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getDayOfWeek());
    }

    @Test
    void testGetLunchPeriodsForGradeLevel_ShouldReturnMatchingGrade() {
        when(lunchPeriodRepository.findByGradeLevel("9")).thenReturn(Arrays.asList(testLunchPeriod));

        List<LunchPeriod> result = service.getLunchPeriodsForGradeLevel("9");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== UPDATE TESTS ==========

    @Test
    void testUpdateLunchPeriod_WithExistingId_ShouldUpdate() {
        LunchPeriod updated = new LunchPeriod();
        updated.setName("Lunch B");
        updated.setLunchGroup("Wave 2");
        updated.setStartTime(LocalTime.of(12, 0));
        updated.setEndTime(LocalTime.of(12, 30));
        updated.setMaxStudents(200);

        when(lunchPeriodRepository.findById(1L)).thenReturn(Optional.of(testLunchPeriod));
        when(lunchPeriodRepository.save(any(LunchPeriod.class))).thenAnswer(i -> i.getArgument(0));

        LunchPeriod result = service.updateLunchPeriod(1L, updated);

        assertEquals("Lunch B", result.getName());
        assertEquals("Wave 2", result.getLunchGroup());
        assertEquals(200, result.getMaxStudents());
    }

    @Test
    void testUpdateLunchPeriod_WithNonExistentId_ShouldThrowException() {
        when(lunchPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        LunchPeriod updated = new LunchPeriod();
        updated.setName("Lunch B");

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateLunchPeriod(999L, updated);
        });
    }

    // ========== DELETE TESTS ==========

    @Test
    void testDeleteLunchPeriod_WithExistingId_ShouldSoftDelete() {
        when(lunchPeriodRepository.findById(1L)).thenReturn(Optional.of(testLunchPeriod));
        when(lunchPeriodRepository.save(any(LunchPeriod.class))).thenAnswer(i -> i.getArgument(0));

        service.deleteLunchPeriod(1L);

        assertFalse(testLunchPeriod.isActive());
        verify(lunchPeriodRepository).save(testLunchPeriod);
    }

    @Test
    void testDeleteLunchPeriod_WithNonExistentId_ShouldThrowException() {
        when(lunchPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteLunchPeriod(999L);
        });
    }

    @Test
    void testActivateLunchPeriod_WithExistingId_ShouldActivate() {
        testLunchPeriod.setActive(false);
        when(lunchPeriodRepository.findById(1L)).thenReturn(Optional.of(testLunchPeriod));
        when(lunchPeriodRepository.save(any(LunchPeriod.class))).thenAnswer(i -> i.getArgument(0));

        service.activateLunchPeriod(1L);

        assertTrue(testLunchPeriod.isActive());
        verify(lunchPeriodRepository).save(testLunchPeriod);
    }

    @Test
    void testActivateLunchPeriod_WithNonExistentId_ShouldThrowException() {
        when(lunchPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.activateLunchPeriod(999L);
        });
    }

    // ========== EXISTS TESTS ==========

    @Test
    void testExistsById_WithExistingId_ShouldReturnTrue() {
        when(lunchPeriodRepository.existsById(1L)).thenReturn(true);

        boolean result = service.existsById(1L);

        assertTrue(result);
    }

    @Test
    void testExistsById_WithNonExistentId_ShouldReturnFalse() {
        when(lunchPeriodRepository.existsById(999L)).thenReturn(false);

        boolean result = service.existsById(999L);

        assertFalse(result);
    }

    // ========== TIME CONFLICT TESTS ==========

    @Test
    void testHasTimeConflict_WithNoOverlap_ShouldReturnFalse() {
        LunchPeriod existing = new LunchPeriod();
        existing.setStartTime(LocalTime.of(11, 0));
        existing.setEndTime(LocalTime.of(11, 30));
        existing.setActive(true);

        when(lunchPeriodRepository.findByLocation("Cafeteria")).thenReturn(Arrays.asList(existing));

        boolean result = service.hasTimeConflict(
                LocalTime.of(11, 30),
                LocalTime.of(12, 0),
                "Cafeteria"
        );

        assertFalse(result);
    }

    @Test
    void testHasTimeConflict_WithOverlap_ShouldReturnTrue() {
        LunchPeriod existing = new LunchPeriod();
        existing.setStartTime(LocalTime.of(11, 0));
        existing.setEndTime(LocalTime.of(11, 45));
        existing.setActive(true);

        when(lunchPeriodRepository.findByLocation("Cafeteria")).thenReturn(Arrays.asList(existing));

        boolean result = service.hasTimeConflict(
                LocalTime.of(11, 30),
                LocalTime.of(12, 0),
                "Cafeteria"
        );

        assertTrue(result);
    }

    @Test
    void testHasTimeConflict_WithExactMatch_ShouldReturnTrue() {
        LunchPeriod existing = new LunchPeriod();
        existing.setStartTime(LocalTime.of(11, 30));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setActive(true);

        when(lunchPeriodRepository.findByLocation("Cafeteria")).thenReturn(Arrays.asList(existing));

        boolean result = service.hasTimeConflict(
                LocalTime.of(11, 30),
                LocalTime.of(12, 0),
                "Cafeteria"
        );

        assertTrue(result);
    }

    @Test
    void testHasTimeConflict_WithInactiveConflict_ShouldReturnFalse() {
        LunchPeriod existing = new LunchPeriod();
        existing.setStartTime(LocalTime.of(11, 30));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setActive(false); // Inactive

        when(lunchPeriodRepository.findByLocation("Cafeteria")).thenReturn(Arrays.asList(existing));

        boolean result = service.hasTimeConflict(
                LocalTime.of(11, 30),
                LocalTime.of(12, 0),
                "Cafeteria"
        );

        assertFalse(result);
    }

    @Test
    void testHasTimeConflict_WithDifferentLocation_ShouldReturnFalse() {
        when(lunchPeriodRepository.findByLocation("GYM")).thenReturn(Collections.emptyList());

        boolean result = service.hasTimeConflict(
                LocalTime.of(11, 30),
                LocalTime.of(12, 0),
                "GYM"
        );

        assertFalse(result);
    }

    // ========== LUNCH ASSIGNMENT TESTS ==========

    @Test
    void testAssignLunchPeriods_WithValidSchedule_ShouldNotCrash() {
        testSchedule.setSlots(Arrays.asList(testSlot));
        testSlot.setStudents(Arrays.asList(testStudent));

        assertDoesNotThrow(() -> {
            service.assignLunchPeriods(testSchedule, testConfig);
        });
    }

    @Test
    void testAssignLunchPeriods_WithNullSlots_ShouldNotCrash() {
        testSchedule.setSlots(null);

        assertDoesNotThrow(() -> {
            service.assignLunchPeriods(testSchedule, testConfig);
        });
    }

    @Test
    void testGetLunchSlotsForStudent_WithLunchSlots_ShouldReturnSlots() {
        testStudent.getScheduleSlots().add(testSlot);

        List<ScheduleSlot> result = service.getLunchSlotsForStudent(testStudent);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsLunchPeriod());
    }

    @Test
    void testGetLunchSlotsForStudent_WithNullSlots_ShouldReturnEmpty() {
        testStudent.setScheduleSlots(null);

        List<ScheduleSlot> result = service.getLunchSlotsForStudent(testStudent);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLunchSlotsForStudent_WithNoLunchSlots_ShouldReturnEmpty() {
        ScheduleSlot regularSlot = new ScheduleSlot();
        regularSlot.setIsLunchPeriod(false);
        testStudent.getScheduleSlots().add(regularSlot);

        List<ScheduleSlot> result = service.getLunchSlotsForStudent(testStudent);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLunchSlotsForTeacher_WithLunchSlots_ShouldReturnSlots() {
        testTeacher.getScheduleSlots().add(testSlot);

        List<ScheduleSlot> result = service.getLunchSlotsForTeacher(testTeacher);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsLunchPeriod());
    }

    @Test
    void testGetLunchSlotsForTeacher_WithNullSlots_ShouldReturnEmpty() {
        testTeacher.setScheduleSlots(null);

        List<ScheduleSlot> result = service.getLunchSlotsForTeacher(testTeacher);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== LUNCH DISTRIBUTION TESTS ==========

    @Test
    void testGetLunchDistribution_WithValidSchedule_ShouldReturnDistribution() {
        testSlot.setStudents(Arrays.asList(testStudent));
        testSchedule.setSlots(Arrays.asList(testSlot));

        Map<Integer, List<Student>> result = service.getLunchDistribution(testSchedule);

        assertNotNull(result);
        assertTrue(result.containsKey(1));
        assertEquals(1, result.get(1).size());
    }

    @Test
    void testGetLunchDistribution_WithNullSlots_ShouldReturnEmpty() {
        testSchedule.setSlots(null);

        Map<Integer, List<Student>> result = service.getLunchDistribution(testSchedule);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLunchDistribution_WithNoLunchSlots_ShouldReturnEmpty() {
        ScheduleSlot regularSlot = new ScheduleSlot();
        regularSlot.setIsLunchPeriod(false);
        testSchedule.setSlots(Arrays.asList(regularSlot));

        Map<Integer, List<Student>> result = service.getLunchDistribution(testSchedule);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== CAPACITY VALIDATION TESTS ==========

    @Test
    void testValidateLunchCapacity_WithinCapacity_ShouldReturnTrue() {
        testSlot.setStudents(Arrays.asList(testStudent));
        testSchedule.setSlots(Arrays.asList(testSlot));

        boolean result = service.validateLunchCapacity(testSchedule, testConfig);

        assertTrue(result);
    }

    @Test
    void testValidateLunchCapacity_WithExactCapacity_ShouldReturnTrue() {
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Student s = new Student();
            s.setId((long) i);
            students.add(s);
        }
        testSlot.setStudents(students);
        testSchedule.setSlots(Arrays.asList(testSlot));

        boolean result = service.validateLunchCapacity(testSchedule, testConfig);

        assertTrue(result);
    }

    @Test
    void testValidateLunchCapacity_OverCapacity_ShouldReturnFalse() {
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Student s = new Student();
            s.setId((long) i);
            students.add(s);
        }
        testSlot.setStudents(students);
        testSchedule.setSlots(Arrays.asList(testSlot));

        boolean result = service.validateLunchCapacity(testSchedule, testConfig);

        assertFalse(result);
    }

    // ========== LUNCH WAVE DETERMINATION TESTS ==========

    @Test
    void testDetermineLunchWave_WithoutGradeStagger_ShouldUseHashCode() {
        testConfig.setStaggerByGrade(false);

        Integer result = service.determineLunchWave(testStudent, testConfig);

        assertNotNull(result);
        assertTrue(result >= 1 && result <= testConfig.getNumberOfLunchPeriods());
    }

    @Test
    void testDetermineLunchWave_WithGradeStagger_ShouldUseGradeLevel() {
        testConfig.setStaggerByGrade(true);
        testStudent.setGradeLevel("9");

        Integer result = service.determineLunchWave(testStudent, testConfig);

        assertNotNull(result);
        assertTrue(result >= 1 && result <= testConfig.getNumberOfLunchPeriods());
    }

    @Test
    void testDetermineLunchWave_WithNullGradeLevel_ShouldReturnDefault() {
        testConfig.setStaggerByGrade(true);
        testStudent.setGradeLevel(null);

        Integer result = service.determineLunchWave(testStudent, testConfig);

        assertEquals(1, result);
    }

    @Test
    void testDetermineLunchWave_WithInvalidGradeLevel_ShouldReturnDefault() {
        testConfig.setStaggerByGrade(true);
        testStudent.setGradeLevel("Kindergarten");

        Integer result = service.determineLunchWave(testStudent, testConfig);

        assertEquals(1, result);
    }

    // ========== STAGGER TESTS ==========

    @Test
    void testStaggerLunchByGrade_ShouldNotCrash() {
        assertDoesNotThrow(() -> {
            service.staggerLunchByGrade(testSchedule, testConfig);
        });
    }

    // ========== CONFLICT DETECTION TESTS ==========

    @Test
    void testHasLunchConflict_WithLunchSlot_ShouldReturnTrue() {
        testSlot.setIsLunchPeriod(true);

        boolean result = service.hasLunchConflict(testSlot);

        assertTrue(result);
    }

    @Test
    void testHasLunchConflict_WithRegularSlot_ShouldReturnFalse() {
        testSlot.setIsLunchPeriod(false);

        boolean result = service.hasLunchConflict(testSlot);

        assertFalse(result);
    }

    @Test
    void testHasLunchConflict_WithNullIsLunchPeriod_ShouldReturnFalse() {
        testSlot.setIsLunchPeriod(null);

        boolean result = service.hasLunchConflict(testSlot);

        assertFalse(result);
    }

    // ========== NULL SAFETY TESTS ==========

    @Test
    void testGetLunchDistribution_WithNullStudentsInSlot_ShouldNotCrash() {
        testSlot.setStudents(null);
        testSchedule.setSlots(Arrays.asList(testSlot));

        Map<Integer, List<Student>> result = service.getLunchDistribution(testSchedule);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLunchDistribution_WithNullWaveNumber_ShouldFilterOut() {
        testSlot.setLunchWaveNumber(null);
        testSlot.setStudents(Arrays.asList(testStudent));
        testSchedule.setSlots(Arrays.asList(testSlot));

        Map<Integer, List<Student>> result = service.getLunchDistribution(testSchedule);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
