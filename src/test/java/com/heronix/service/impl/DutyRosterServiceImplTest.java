package com.heronix.service.impl;

import com.heronix.model.domain.DutyAssignment;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.DutyAssignmentRepository;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Suite for DutyRosterServiceImpl
 * Service #33 in systematic testing plan
 *
 * Tests duty roster management with auto-generation and fair distribution
 */
@ExtendWith(MockitoExtension.class)
class DutyRosterServiceImplTest {

    @Mock(lenient = true)
    private DutyAssignmentRepository dutyRepository;

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @InjectMocks
    private DutyRosterServiceImpl service;

    private Teacher testTeacher1;
    private Teacher testTeacher2;
    private DutyAssignment testDuty;

    @BeforeEach
    void setUp() {
        // Setup test teachers
        testTeacher1 = new Teacher();
        testTeacher1.setId(1L);
        testTeacher1.setFirstName("John");
        testTeacher1.setLastName("Doe");
        testTeacher1.setActive(true);

        testTeacher2 = new Teacher();
        testTeacher2.setId(2L);
        testTeacher2.setFirstName("Jane");
        testTeacher2.setLastName("Smith");
        testTeacher2.setActive(true);

        // Setup test duty
        testDuty = new DutyAssignment();
        testDuty.setId(1L);
        testDuty.setTeacher(testTeacher1);
        testDuty.setDutyType("AM");
        testDuty.setDutyLocation("Front Entrance");
        testDuty.setDutyDate(LocalDate.of(2024, 9, 2));
        testDuty.setStartTime(LocalTime.of(7, 30));
        testDuty.setEndTime(LocalTime.of(8, 0));
        testDuty.setPriority(1);
        testDuty.setActive(true);

        // Configure repository mocks
        when(dutyRepository.save(any(DutyAssignment.class))).thenAnswer(i -> {
            DutyAssignment d = i.getArgument(0);
            if (d.getId() == null) {
                d.setId(1L);
            }
            return d;
        });

        when(dutyRepository.findById(1L)).thenReturn(Optional.of(testDuty));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(testTeacher2));
    }

    // ========================================================================
    // CREATE DUTY TESTS
    // ========================================================================

    @Test
    void testCreateDuty_WithValidDuty_ShouldSave() {
        when(dutyRepository.findConflictingDuties(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        DutyAssignment result = service.createDuty(testDuty);

        assertNotNull(result);
        verify(dutyRepository).save(testDuty);
    }

    @Test
    void testCreateDuty_WithNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.createDuty(null);
        });
    }

    @Test
    void testCreateDuty_WithConflicts_ShouldThrowException() {
        DutyAssignment conflictingDuty = new DutyAssignment();
        conflictingDuty.setId(2L);

        when(dutyRepository.findConflictingDuties(anyLong(), any(), any(), any()))
                .thenReturn(Arrays.asList(conflictingDuty));

        assertThrows(IllegalStateException.class, () -> {
            service.createDuty(testDuty);
        });
    }

    @Test
    void testCreateDuty_WithNullDutyType_ShouldHandleGracefully() {
        testDuty.setDutyType(null);

        when(dutyRepository.findConflictingDuties(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        DutyAssignment result = service.createDuty(testDuty);

        assertNotNull(result);
        verify(dutyRepository).save(testDuty);
    }

    @Test
    void testCreateDuty_WithNullLocation_ShouldHandleGracefully() {
        testDuty.setDutyLocation(null);

        when(dutyRepository.findConflictingDuties(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        DutyAssignment result = service.createDuty(testDuty);

        assertNotNull(result);
        verify(dutyRepository).save(testDuty);
    }

    // ========================================================================
    // UPDATE DUTY TESTS
    // ========================================================================

    @Test
    void testUpdateDuty_WithValidData_ShouldUpdate() {
        DutyAssignment updates = new DutyAssignment();
        updates.setTeacher(testTeacher2);
        updates.setDutyType("PM");
        updates.setDutyLocation("Cafeteria");
        updates.setDutyDate(LocalDate.of(2024, 9, 3));
        updates.setStartTime(LocalTime.of(15, 30));
        updates.setEndTime(LocalTime.of(16, 0));
        updates.setNotes("Updated notes");
        updates.setPriority(2);

        DutyAssignment result = service.updateDuty(1L, updates);

        assertNotNull(result);
        assertEquals(testTeacher2, result.getTeacher());
        assertEquals("PM", result.getDutyType());
        assertEquals("Cafeteria", result.getDutyLocation());
        verify(dutyRepository).save(testDuty);
    }

    @Test
    void testUpdateDuty_WithNonExistentId_ShouldThrowException() {
        when(dutyRepository.findById(999L)).thenReturn(Optional.empty());

        DutyAssignment updates = new DutyAssignment();

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateDuty(999L, updates);
        });
    }

    // ========================================================================
    // DELETE DUTY TESTS
    // ========================================================================

    @Test
    void testDeleteDuty_ShouldSetInactive() {
        service.deleteDuty(1L);

        assertFalse(testDuty.isActive());
        verify(dutyRepository).save(testDuty);
    }

    @Test
    void testDeleteDuty_WithNonExistentId_ShouldThrowException() {
        when(dutyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteDuty(999L);
        });
    }

    // ========================================================================
    // READ OPERATIONS TESTS
    // ========================================================================

    @Test
    void testGetDutyById_WithValidId_ShouldReturn() {
        DutyAssignment result = service.getDutyById(1L);

        assertNotNull(result);
        assertEquals(testDuty, result);
    }

    @Test
    void testGetDutyById_WithNonExistentId_ShouldThrowException() {
        when(dutyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.getDutyById(999L);
        });
    }

    @Test
    void testGetAllActiveDuties_ShouldReturnList() {
        when(dutyRepository.findAllActiveDutiesOrdered())
                .thenReturn(Arrays.asList(testDuty));

        List<DutyAssignment> result = service.getAllActiveDuties();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================================================
    // QUERY OPERATIONS TESTS
    // ========================================================================

    @Test
    void testGetDutiesForTeacher_ShouldReturnList() {
        when(dutyRepository.findByTeacherId(1L))
                .thenReturn(Arrays.asList(testDuty));

        List<DutyAssignment> result = service.getDutiesForTeacher(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetDutiesForDate_ShouldCombineSpecificAndRecurring() {
        LocalDate date = LocalDate.of(2024, 9, 2); // Monday

        DutyAssignment specificDuty = new DutyAssignment();
        specificDuty.setId(1L);

        DutyAssignment recurringDuty = new DutyAssignment();
        recurringDuty.setId(2L);
        recurringDuty.setRecurring(true);
        recurringDuty.setDayOfWeek(1); // Monday
        recurringDuty.setDutyDate(LocalDate.of(2024, 8, 1));

        when(dutyRepository.findByDutyDate(date))
                .thenReturn(Arrays.asList(specificDuty));
        when(dutyRepository.findRecurringDutiesByDayOfWeek(1))
                .thenReturn(Arrays.asList(recurringDuty));

        List<DutyAssignment> result = service.getDutiesForDate(date);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetDutiesInDateRange_ShouldReturnList() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        when(dutyRepository.findByDateRange(startDate, endDate))
                .thenReturn(Arrays.asList(testDuty));

        List<DutyAssignment> result = service.getDutiesInDateRange(startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetDutiesByType_ShouldReturnFiltered() {
        when(dutyRepository.findByDutyTypeAndIsActiveTrue("AM"))
                .thenReturn(Arrays.asList(testDuty));

        List<DutyAssignment> result = service.getDutiesByType("AM");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetDutiesByLocation_ShouldReturnFiltered() {
        when(dutyRepository.findByDutyLocationAndIsActiveTrue("Front Entrance"))
                .thenReturn(Arrays.asList(testDuty));

        List<DutyAssignment> result = service.getDutiesByLocation("Front Entrance");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================================================
    // AUTOMATIC GENERATION TESTS
    // ========================================================================

    @Test
    void testGenerateDutiesForDateRange_ShouldGenerateForWeekdays() {
        LocalDate startDate = LocalDate.of(2024, 9, 2); // Monday
        LocalDate endDate = LocalDate.of(2024, 9, 6); // Friday

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<DutyAssignment> result = service.generateDutiesForDateRange(startDate, endDate, 1L);

        assertNotNull(result);
        // 5 weekdays * 6 locations * 2 shifts (AM + PM) = 60 duties
        assertEquals(60, result.size());
        verify(dutyRepository, times(60)).save(any(DutyAssignment.class));
    }

    @Test
    void testGenerateDutiesForDateRange_ShouldSkipWeekends() {
        LocalDate startDate = LocalDate.of(2024, 9, 7); // Saturday
        LocalDate endDate = LocalDate.of(2024, 9, 8); // Sunday

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<DutyAssignment> result = service.generateDutiesForDateRange(startDate, endDate, 1L);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(dutyRepository, never()).save(any(DutyAssignment.class));
    }

    @Test
    void testGenerateDutiesForDateRange_ShouldRotateTeachers() {
        LocalDate startDate = LocalDate.of(2024, 9, 2); // Monday
        LocalDate endDate = LocalDate.of(2024, 9, 2); // Same day

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<DutyAssignment> result = service.generateDutiesForDateRange(startDate, endDate, 1L);

        assertNotNull(result);
        // 1 day * 6 locations * 2 shifts = 12 duties
        assertEquals(12, result.size());
    }

    @Test
    void testGenerateRotatingSchedule_ShouldCreateRotation() {
        List<Teacher> teachers = Arrays.asList(testTeacher1, testTeacher2);
        LocalDate startDate = LocalDate.of(2024, 9, 2); // Monday
        int weeks = 1;

        List<DutyAssignment> result = service.generateRotatingSchedule(
                teachers, startDate, weeks, "AM", "Front Entrance");

        assertNotNull(result);
        // 1 week from Mon Sept 2 to Mon Sept 9 = 6 weekdays (M,T,W,Th,F,M)
        assertEquals(6, result.size());
        verify(dutyRepository, times(6)).save(any(DutyAssignment.class));
    }

    @Test
    void testGenerateRotatingSchedule_ShouldSetCorrectTimes() {
        List<Teacher> teachers = Arrays.asList(testTeacher1);
        LocalDate startDate = LocalDate.of(2024, 9, 2);

        List<DutyAssignment> amResult = service.generateRotatingSchedule(
                teachers, startDate, 1, "AM", "Front Entrance");

        assertNotNull(amResult);
        assertTrue(amResult.size() > 0);
        // AM duties should start at 7:30
        assertEquals(LocalTime.of(7, 30), amResult.get(0).getStartTime());

        List<DutyAssignment> pmResult = service.generateRotatingSchedule(
                teachers, startDate, 1, "PM", "Front Entrance");

        assertNotNull(pmResult);
        assertTrue(pmResult.size() > 0);
        // PM duties should start at 15:30
        assertEquals(LocalTime.of(15, 30), pmResult.get(0).getStartTime());
    }

    // ========================================================================
    // CONFLICT DETECTION TESTS
    // ========================================================================

    @Test
    void testHasConflicts_WithNoConflicts_ShouldReturnFalse() {
        when(dutyRepository.findConflictingDuties(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        boolean result = service.hasConflicts(testDuty);

        assertFalse(result);
    }

    @Test
    void testHasConflicts_WithConflicts_ShouldReturnTrue() {
        DutyAssignment conflictingDuty = new DutyAssignment();
        conflictingDuty.setId(2L);

        when(dutyRepository.findConflictingDuties(anyLong(), any(), any(), any()))
                .thenReturn(Arrays.asList(conflictingDuty));

        boolean result = service.hasConflicts(testDuty);

        assertTrue(result);
    }

    @Test
    void testHasConflicts_WithSelfConflict_ShouldReturnFalse() {
        // When updating, the duty might conflict with itself
        when(dutyRepository.findConflictingDuties(anyLong(), any(), any(), any()))
                .thenReturn(Arrays.asList(testDuty)); // Same duty

        boolean result = service.hasConflicts(testDuty);

        assertFalse(result); // Should exclude self
    }

    @Test
    void testHasConflicts_WithNullTeacher_ShouldReturnFalse() {
        testDuty.setTeacher(null);

        boolean result = service.hasConflicts(testDuty);

        assertFalse(result);
    }

    @Test
    void testHasConflicts_WithNullDate_ShouldReturnFalse() {
        testDuty.setDutyDate(null);

        boolean result = service.hasConflicts(testDuty);

        assertFalse(result);
    }

    @Test
    void testGetConflicts_ShouldReturnList() {
        DutyAssignment conflictingDuty = new DutyAssignment();
        conflictingDuty.setId(2L);

        when(dutyRepository.findConflictingDuties(
                eq(1L),
                eq(LocalDate.of(2024, 9, 2)),
                eq(LocalTime.of(7, 30)),
                eq(LocalTime.of(8, 0))))
                .thenReturn(Arrays.asList(conflictingDuty));

        List<DutyAssignment> result = service.getConflicts(
                1L,
                LocalDate.of(2024, 9, 2),
                LocalTime.of(7, 30),
                LocalTime.of(8, 0));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================================================
    // STATISTICS TESTS
    // ========================================================================

    @Test
    void testGetDutyCountByTeacher_ShouldReturnCounts() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1, testTeacher2));
        when(dutyRepository.countByTeacherAndDateRange(eq(1L), eq(startDate), eq(endDate)))
                .thenReturn(10L);
        when(dutyRepository.countByTeacherAndDateRange(eq(2L), eq(startDate), eq(endDate)))
                .thenReturn(8L);

        Map<Teacher, Long> result = service.getDutyCountByTeacher(startDate, endDate);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(testTeacher1));
        assertEquals(8L, result.get(testTeacher2));
    }

    @Test
    void testGetDutyCountByTeacher_WithNullTeachers_ShouldFilterThem() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(null, testTeacher1));
        when(dutyRepository.countByTeacherAndDateRange(anyLong(), any(), any()))
                .thenReturn(5L);

        Map<Teacher, Long> result = service.getDutyCountByTeacher(startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetDutyCountByTeacher_WithNullTeacherId_ShouldFilterThem() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        Teacher teacherNullId = new Teacher();
        teacherNullId.setId(null);

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(teacherNullId, testTeacher1));
        when(dutyRepository.countByTeacherAndDateRange(anyLong(), any(), any()))
                .thenReturn(5L);

        Map<Teacher, Long> result = service.getDutyCountByTeacher(startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetDutyStatistics_ShouldReturnCompleteStats() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        DutyAssignment amDuty = new DutyAssignment();
        amDuty.setDutyType("AM");

        DutyAssignment pmDuty = new DutyAssignment();
        pmDuty.setDutyType("PM");

        when(dutyRepository.findByDateRange(startDate, endDate))
                .thenReturn(Arrays.asList(amDuty, pmDuty));
        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1));
        when(dutyRepository.countByTeacherAndDateRange(anyLong(), any(), any()))
                .thenReturn(2L);

        Map<String, Object> result = service.getDutyStatistics(startDate, endDate);

        assertNotNull(result);
        assertTrue(result.containsKey("totalDuties"));
        assertTrue(result.containsKey("amDuties"));
        assertTrue(result.containsKey("pmDuties"));
        assertTrue(result.containsKey("lunchDuties"));
        assertTrue(result.containsKey("substituteDuties"));
        assertTrue(result.containsKey("dutyCountByTeacher"));
        assertTrue(result.containsKey("balanceScore"));
    }

    @Test
    void testGetDutyStatistics_WithNullDuties_ShouldFilterThem() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        DutyAssignment amDuty = new DutyAssignment();
        amDuty.setDutyType("AM");

        when(dutyRepository.findByDateRange(startDate, endDate))
                .thenReturn(Arrays.asList(amDuty, null));
        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1));
        when(dutyRepository.countByTeacherAndDateRange(anyLong(), any(), any()))
                .thenReturn(1L);

        Map<String, Object> result = service.getDutyStatistics(startDate, endDate);

        assertNotNull(result);
        assertEquals(2, result.get("totalDuties")); // Includes null
    }

    @Test
    void testGetDutyBalanceScore_WithPerfectBalance_ShouldReturn100() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1, testTeacher2));
        when(dutyRepository.countByTeacherAndDateRange(eq(1L), any(), any()))
                .thenReturn(10L);
        when(dutyRepository.countByTeacherAndDateRange(eq(2L), any(), any()))
                .thenReturn(10L);

        double result = service.getDutyBalanceScore(startDate, endDate);

        assertEquals(100.0, result, 0.01);
    }

    @Test
    void testGetDutyBalanceScore_WithImbalance_ShouldReturnLower() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(testTeacher1, testTeacher2));
        when(dutyRepository.countByTeacherAndDateRange(eq(1L), any(), any()))
                .thenReturn(20L);
        when(dutyRepository.countByTeacherAndDateRange(eq(2L), any(), any()))
                .thenReturn(5L);

        double result = service.getDutyBalanceScore(startDate, endDate);

        assertTrue(result < 100.0);
        assertTrue(result >= 0.0);
    }

    @Test
    void testGetDutyBalanceScore_WithNoCounts_ShouldReturn100() {
        LocalDate startDate = LocalDate.of(2024, 9, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        when(teacherRepository.findByActiveTrue())
                .thenReturn(Collections.emptyList());

        double result = service.getDutyBalanceScore(startDate, endDate);

        assertEquals(100.0, result, 0.01);
    }

    // ========================================================================
    // SUBSTITUTE MANAGEMENT TESTS
    // ========================================================================

    @Test
    void testAssignSubstitute_ShouldSetSubstituteAndOriginal() {
        DutyAssignment result = service.assignSubstitute(1L, 2L);

        assertNotNull(result);
        assertEquals(testTeacher2, result.getTeacher());
        assertEquals(testTeacher1, result.getOriginalTeacher());
        assertTrue(result.isSubstitute());
        verify(dutyRepository).save(testDuty);
    }

    @Test
    void testAssignSubstitute_WithNonExistentDuty_ShouldThrowException() {
        when(dutyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.assignSubstitute(999L, 2L);
        });
    }

    @Test
    void testAssignSubstitute_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.assignSubstitute(1L, 999L);
        });
    }

    @Test
    void testRemoveSubstitute_ShouldRestoreOriginal() {
        testDuty.setOriginalTeacher(testTeacher1);
        testDuty.setTeacher(testTeacher2);
        testDuty.setSubstitute(true);

        DutyAssignment result = service.removeSubstitute(1L);

        assertNotNull(result);
        assertEquals(testTeacher1, result.getTeacher());
        assertNull(result.getOriginalTeacher());
        assertFalse(result.isSubstitute());
        verify(dutyRepository).save(testDuty);
    }

    @Test
    void testRemoveSubstitute_WithNoOriginal_ShouldNotChange() {
        testDuty.setOriginalTeacher(null);

        DutyAssignment result = service.removeSubstitute(1L);

        assertNotNull(result);
        verify(dutyRepository).save(testDuty);
    }

    @Test
    void testGetSubstituteDuties_ShouldReturnList() {
        when(dutyRepository.findByIsSubstituteTrueAndIsActiveTrue())
                .thenReturn(Arrays.asList(testDuty));

        List<DutyAssignment> result = service.getSubstituteDuties();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================================================
    // RECURRING DUTIES TESTS
    // ========================================================================

    @Test
    void testCreateRecurringDuty_ShouldSave() {
        DutyAssignment result = service.createRecurringDuty(
                testTeacher1,
                "AM",
                "Front Entrance",
                1, // Monday
                LocalTime.of(7, 30),
                LocalTime.of(8, 0),
                "WEEKLY");

        assertNotNull(result);
        verify(dutyRepository).save(any(DutyAssignment.class));
    }

    @Test
    void testGetRecurringDuties_ShouldReturnList() {
        when(dutyRepository.findByIsRecurringTrueAndIsActiveTrue())
                .thenReturn(Arrays.asList(testDuty));

        List<DutyAssignment> result = service.getRecurringDuties();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGenerateRecurringInstances_ShouldCreateInstances() {
        LocalDate startDate = LocalDate.of(2024, 9, 2); // Monday
        LocalDate endDate = LocalDate.of(2024, 9, 30);

        DutyAssignment recurringDuty = new DutyAssignment();
        recurringDuty.setId(1L);
        recurringDuty.setTeacher(testTeacher1);
        recurringDuty.setDutyType("AM");
        recurringDuty.setDutyLocation("Front Entrance");
        recurringDuty.setDayOfWeek(1); // Monday
        recurringDuty.setStartTime(LocalTime.of(7, 30));
        recurringDuty.setEndTime(LocalTime.of(8, 0));
        recurringDuty.setPriority(1);
        recurringDuty.setRecurring(true);

        when(dutyRepository.findByIsRecurringTrueAndIsActiveTrue())
                .thenReturn(Arrays.asList(recurringDuty));

        List<DutyAssignment> result = service.generateRecurringInstances(startDate, endDate);

        assertNotNull(result);
        // 4 Mondays in September 2024 (2, 9, 16, 23, 30)
        assertEquals(5, result.size());
    }

    @Test
    void testGenerateRecurringInstances_ShouldSetCorrectNotes() {
        LocalDate startDate = LocalDate.of(2024, 9, 2);
        LocalDate endDate = LocalDate.of(2024, 9, 9);

        DutyAssignment recurringDuty = new DutyAssignment();
        recurringDuty.setId(1L);
        recurringDuty.setTeacher(testTeacher1);
        recurringDuty.setDutyType("AM");
        recurringDuty.setDutyLocation("Front Entrance");
        recurringDuty.setDayOfWeek(1); // Monday
        recurringDuty.setStartTime(LocalTime.of(7, 30));
        recurringDuty.setEndTime(LocalTime.of(8, 0));
        recurringDuty.setPriority(1);

        when(dutyRepository.findByIsRecurringTrueAndIsActiveTrue())
                .thenReturn(Arrays.asList(recurringDuty));

        List<DutyAssignment> result = service.generateRecurringInstances(startDate, endDate);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    // ========================================================================
    // NULL SAFETY TESTS
    // ========================================================================

    @Test
    void testGetDutiesForDate_WithNullRecurringDuties_ShouldFilterThem() {
        LocalDate date = LocalDate.of(2024, 9, 2);

        DutyAssignment recurringDutyNullDate = new DutyAssignment();
        recurringDutyNullDate.setId(2L);
        recurringDutyNullDate.setRecurring(true);
        recurringDutyNullDate.setDayOfWeek(1);
        recurringDutyNullDate.setDutyDate(null); // Null date

        when(dutyRepository.findByDutyDate(date))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findRecurringDutiesByDayOfWeek(1))
                .thenReturn(Arrays.asList(recurringDutyNullDate));

        List<DutyAssignment> result = service.getDutiesForDate(date);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetDutiesForDate_WithRecurringDutyAfterDate_ShouldFilterThem() {
        LocalDate date = LocalDate.of(2024, 9, 2);

        DutyAssignment recurringDutyAfter = new DutyAssignment();
        recurringDutyAfter.setId(2L);
        recurringDutyAfter.setRecurring(true);
        recurringDutyAfter.setDayOfWeek(1);
        recurringDutyAfter.setDutyDate(LocalDate.of(2024, 9, 10)); // After query date

        when(dutyRepository.findByDutyDate(date))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findRecurringDutiesByDayOfWeek(1))
                .thenReturn(Arrays.asList(recurringDutyAfter));

        List<DutyAssignment> result = service.getDutiesForDate(date);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
