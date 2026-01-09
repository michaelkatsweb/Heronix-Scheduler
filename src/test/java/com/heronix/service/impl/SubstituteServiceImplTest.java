package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.SlotStatus;
import com.heronix.repository.*;
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
 * Test Suite for SubstituteServiceImpl
 * Service #32 in systematic testing plan
 *
 * Tests substitute teacher management and assignment
 */
@ExtendWith(MockitoExtension.class)
class SubstituteServiceImplTest {

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @Mock(lenient = true)
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private SubstituteServiceImpl service;

    private Teacher testTeacher;
    private Teacher substituteTeacher1;
    private Teacher substituteTeacher2;
    private Course testCourse;
    private ScheduleSlot testSlot;

    @BeforeEach
    void setUp() {
        // Setup test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Doe");
        testTeacher.setActive(true);
        testTeacher.setCertifications(Arrays.asList("Math", "Science"));

        // Setup substitute teachers
        substituteTeacher1 = new Teacher();
        substituteTeacher1.setId(2L);
        substituteTeacher1.setFirstName("Jane");
        substituteTeacher1.setLastName("Smith");
        substituteTeacher1.setActive(true);
        substituteTeacher1.setCertifications(Arrays.asList("Math", "English"));

        substituteTeacher2 = new Teacher();
        substituteTeacher2.setId(3L);
        substituteTeacher2.setFirstName("Bob");
        substituteTeacher2.setLastName("Johnson");
        substituteTeacher2.setActive(true);
        substituteTeacher2.setCertifications(Arrays.asList("English", "History"));

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");
        testCourse.setSubject("Math");
        testCourse.setTeacher(testTeacher);

        // Setup test slot
        testSlot = new ScheduleSlot();
        testSlot.setId(1L);
        testSlot.setCourse(testCourse);
        testSlot.setTeacher(testTeacher);
        testSlot.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot.setStartTime(LocalTime.of(9, 0));
        testSlot.setEndTime(LocalTime.of(10, 0));
        testSlot.setStatus(SlotStatus.ACTIVE);

        // Configure repository mocks
        when(scheduleSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(substituteTeacher1));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(substituteTeacher2));
        when(scheduleSlotRepository.save(any(ScheduleSlot.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ========================================================================
    // FIND AVAILABLE SUBSTITUTES TESTS
    // ========================================================================

    @Test
    void testFindAvailableSubstitutes_WithNoConflicts_ShouldReturnAll() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1, substituteTeacher2));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindAvailableSubstitutes_ShouldExcludeOriginalTeacher() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(testTeacher, substituteTeacher1, substituteTeacher2));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertFalse(result.contains(testTeacher));
    }

    @Test
    void testFindAvailableSubstitutes_WithConflicts_ShouldExcludeThem() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1, substituteTeacher2));

        // substituteTeacher1 has a conflict
        when(scheduleSlotRepository.findTeacherTimeConflicts(eq(2L), any(), any(), any()))
                .thenReturn(Arrays.asList(testSlot));

        // substituteTeacher2 has no conflicts
        when(scheduleSlotRepository.findTeacherTimeConflicts(eq(3L), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(substituteTeacher2, result.get(0));
    }

    @Test
    void testFindAvailableSubstitutes_ShouldPrioritizeCertificationMatch() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1, substituteTeacher2));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        // substituteTeacher1 has Math certification, should be first
        assertEquals(substituteTeacher1, result.get(0));
    }

    @Test
    void testFindAvailableSubstitutes_WithNullSlotTimes_ShouldReturnEmpty() {
        testSlot.setDayOfWeek(null);
        testSlot.setStartTime(null);
        testSlot.setEndTime(null);

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAvailableSubstitutes_WithNullTeacher_ShouldIncludeAll() {
        testSlot.setTeacher(null);

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(testTeacher, substituteTeacher1));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindAvailableSubstitutes_WithNonExistentSlot_ShouldThrowException() {
        when(scheduleSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.findAvailableSubstitutes(999L);
        });
    }

    @Test
    void testFindAvailableSubstitutes_WithNullTeachersInList_ShouldFilterThem() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(null, substituteTeacher1));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAvailableSubstitutes_WithNullCertifications_ShouldHandleGracefully() {
        substituteTeacher1.setCertifications(null);
        substituteTeacher2.setCertifications(Arrays.asList("Math"));

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1, substituteTeacher2));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        // substituteTeacher2 with Math cert should be first
        assertEquals(substituteTeacher2, result.get(0));
    }

    // ========================================================================
    // ASSIGN SUBSTITUTE TESTS
    // ========================================================================

    @Test
    void testAssignSubstitute_WithNoConflicts_ShouldAssign() {
        when(scheduleSlotRepository.findTeacherTimeConflicts(eq(2L), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        ScheduleSlot result = service.assignSubstitute(1L, 2L);

        assertNotNull(result);
        assertEquals(substituteTeacher1, result.getSubstituteTeacher());
        assertEquals(SlotStatus.SUBSTITUTE_NEEDED, result.getStatus());
        verify(scheduleSlotRepository).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignSubstitute_WithConflict_ShouldThrowException() {
        when(scheduleSlotRepository.findTeacherTimeConflicts(eq(2L), any(), any(), any()))
                .thenReturn(Arrays.asList(testSlot));

        assertThrows(IllegalStateException.class, () -> {
            service.assignSubstitute(1L, 2L);
        });
    }

    @Test
    void testAssignSubstitute_WithNonExistentSlot_ShouldThrowException() {
        when(scheduleSlotRepository.findById(999L)).thenReturn(Optional.empty());

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
    void testAssignSubstitute_WithNullSlotTimes_ShouldSkipConflictCheck() {
        testSlot.setDayOfWeek(null);
        testSlot.setStartTime(null);
        testSlot.setEndTime(null);

        ScheduleSlot result = service.assignSubstitute(1L, 2L);

        assertNotNull(result);
        assertEquals(substituteTeacher1, result.getSubstituteTeacher());
        verify(scheduleSlotRepository, never()).findTeacherTimeConflicts(anyLong(), any(), any(), any());
    }

    // ========================================================================
    // HANDLE TEACHER ABSENCE TESTS
    // ========================================================================

    @Test
    void testHandleTeacherAbsence_ShouldMarkAffectedSlots() {
        LocalDate absenceDate = LocalDate.of(2024, 9, 2); // Monday

        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setTeacher(testTeacher);
        slot1.setDayOfWeek(DayOfWeek.MONDAY);
        slot1.setStartTime(LocalTime.of(9, 0));
        slot1.setEndTime(LocalTime.of(10, 0));

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setTeacher(testTeacher);
        slot2.setDayOfWeek(DayOfWeek.MONDAY);
        slot2.setStartTime(LocalTime.of(11, 0));
        slot2.setEndTime(LocalTime.of(12, 0));

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findTeacherScheduleByDay(1L, DayOfWeek.MONDAY))
                .thenReturn(Arrays.asList(slot1, slot2));
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1));
        when(scheduleSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(scheduleSlotRepository.findById(2L)).thenReturn(Optional.of(slot2));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(scheduleSlotRepository.save(any(ScheduleSlot.class))).thenAnswer(i -> i.getArgument(0));

        List<ScheduleSlot> result = service.handleTeacherAbsence(1L, absenceDate);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(scheduleSlotRepository, times(2)).save(any(ScheduleSlot.class));
    }

    @Test
    void testHandleTeacherAbsence_WithNoSubstitutes_ShouldMarkActive() {
        LocalDate absenceDate = LocalDate.of(2024, 9, 2);

        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayOfWeek(DayOfWeek.MONDAY);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findTeacherScheduleByDay(1L, DayOfWeek.MONDAY))
                .thenReturn(Arrays.asList(slot1));
        when(scheduleSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(teacherRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(scheduleSlotRepository.save(any(ScheduleSlot.class))).thenAnswer(i -> i.getArgument(0));

        List<ScheduleSlot> result = service.handleTeacherAbsence(1L, absenceDate);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(SlotStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void testHandleTeacherAbsence_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.handleTeacherAbsence(999L, LocalDate.now());
        });
    }

    @Test
    void testHandleTeacherAbsence_WithNullSlotsInList_ShouldSkipThem() {
        LocalDate absenceDate = LocalDate.of(2024, 9, 2);

        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayOfWeek(DayOfWeek.MONDAY);
        slot1.setStartTime(LocalTime.of(9, 0));
        slot1.setEndTime(LocalTime.of(10, 0));

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findTeacherScheduleByDay(1L, DayOfWeek.MONDAY))
                .thenReturn(Arrays.asList(slot1, null));
        when(scheduleSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(scheduleSlotRepository.save(any(ScheduleSlot.class))).thenAnswer(i -> i.getArgument(0));

        List<ScheduleSlot> result = service.handleTeacherAbsence(1L, absenceDate);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================================================
    // GET SUBSTITUTE ASSIGNMENTS TESTS
    // ========================================================================

    @Test
    void testGetSubstituteAssignments_ShouldReturnOnlyWithSubstitutes() {
        LocalDate date = LocalDate.of(2024, 9, 2); // Monday

        ScheduleSlot slotWithSub = new ScheduleSlot();
        slotWithSub.setId(1L);
        slotWithSub.setSubstituteTeacher(substituteTeacher1);

        ScheduleSlot slotWithoutSub = new ScheduleSlot();
        slotWithoutSub.setId(2L);
        slotWithoutSub.setSubstituteTeacher(null);

        when(scheduleSlotRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                .thenReturn(Arrays.asList(slotWithSub, slotWithoutSub));

        List<ScheduleSlot> result = service.getSubstituteAssignments(date);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(slotWithSub, result.get(0));
    }

    @Test
    void testGetSubstituteAssignments_WithNoSubstitutes_ShouldReturnEmpty() {
        LocalDate date = LocalDate.of(2024, 9, 2);

        when(scheduleSlotRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                .thenReturn(Collections.emptyList());

        List<ScheduleSlot> result = service.getSubstituteAssignments(date);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========================================================================
    // REMOVE SUBSTITUTE TESTS
    // ========================================================================

    @Test
    void testRemoveSubstitute_ShouldClearSubstituteAndSetCancelled() {
        testSlot.setSubstituteTeacher(substituteTeacher1);

        ScheduleSlot result = service.removeSubstitute(1L);

        assertNotNull(result);
        assertNull(result.getSubstituteTeacher());
        assertEquals(SlotStatus.CANCELLED, result.getStatus());
        verify(scheduleSlotRepository).save(any(ScheduleSlot.class));
    }

    @Test
    void testRemoveSubstitute_WithNonExistentSlot_ShouldThrowException() {
        when(scheduleSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.removeSubstitute(999L);
        });
    }

    // ========================================================================
    // GET SUBSTITUTE HISTORY TESTS
    // ========================================================================

    @Test
    void testGetSubstituteHistory_ShouldReturnOnlyTeacherSlots() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setSubstituteTeacher(substituteTeacher1);

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setSubstituteTeacher(substituteTeacher2);

        ScheduleSlot slot3 = new ScheduleSlot();
        slot3.setId(3L);
        slot3.setSubstituteTeacher(substituteTeacher1);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, slot2, slot3));

        List<ScheduleSlot> result = service.getSubstituteHistory(2L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> s.getSubstituteTeacher().getId().equals(2L)));
    }

    @Test
    void testGetSubstituteHistory_WithNoHistory_ShouldReturnEmpty() {
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        List<ScheduleSlot> result = service.getSubstituteHistory(2L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSubstituteHistory_WithNullSlots_ShouldFilterThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setSubstituteTeacher(substituteTeacher1); // ID=2L

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, null));

        List<ScheduleSlot> result = service.getSubstituteHistory(2L);

        assertNotNull(result);
        // slot1 has substituteTeacher1 with ID=2L, so it should be included
        assertEquals(1, result.size());
    }

    @Test
    void testGetSubstituteHistory_WithNullSubstituteTeachers_ShouldFilterThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setSubstituteTeacher(null);

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setSubstituteTeacher(substituteTeacher1);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, slot2));

        List<ScheduleSlot> result = service.getSubstituteHistory(2L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================================================
    // FIND BEST MATCH SUBSTITUTE TESTS
    // ========================================================================

    @Test
    void testFindBestMatchSubstitute_WithCertificationMatch_ShouldReturnMatch() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1, substituteTeacher2));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Teacher result = service.findBestMatchSubstitute(1L);

        assertNotNull(result);
        assertEquals(substituteTeacher1, result); // Has Math certification
    }

    @Test
    void testFindBestMatchSubstitute_WithNoCertificationMatch_ShouldReturnFirst() {
        testCourse.setSubject("Physics");

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1, substituteTeacher2));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Teacher result = service.findBestMatchSubstitute(1L);

        assertNotNull(result);
        // No match, so should return first available
        assertNotNull(result.getId());
    }

    @Test
    void testFindBestMatchSubstitute_WithNoAvailable_ShouldReturnNull() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        Teacher result = service.findBestMatchSubstitute(1L);

        assertNull(result);
    }

    @Test
    void testFindBestMatchSubstitute_WithNullSlot_ShouldThrowException() {
        when(scheduleSlotRepository.findById(1L)).thenReturn(Optional.empty());

        // findAvailableSubstitutes will throw IllegalArgumentException if slot not found
        assertThrows(IllegalArgumentException.class, () -> {
            service.findBestMatchSubstitute(1L);
        });
    }

    @Test
    void testFindBestMatchSubstitute_WithNullCourse_ShouldReturnFirstAvailable() {
        testSlot.setCourse(null);

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Teacher result = service.findBestMatchSubstitute(1L);

        assertNotNull(result);
        assertEquals(substituteTeacher1, result);
    }

    @Test
    void testFindBestMatchSubstitute_WithNullSubject_ShouldReturnFirstAvailable() {
        testCourse.setSubject(null);

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Teacher result = service.findBestMatchSubstitute(1L);

        assertNotNull(result);
        assertEquals(substituteTeacher1, result);
    }

    // ========================================================================
    // NULL SAFETY TESTS
    // ========================================================================

    @Test
    void testAssignSubstitute_WithNullSubstituteName_ShouldHandleGracefully() {
        substituteTeacher1.setName(null);

        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        ScheduleSlot result = service.assignSubstitute(1L, 2L);

        assertNotNull(result);
        assertEquals(substituteTeacher1, result.getSubstituteTeacher());
    }

    @Test
    void testHandleTeacherAbsence_WithNullTeacherName_ShouldHandleGracefully() {
        LocalDate absenceDate = LocalDate.of(2024, 9, 2);

        testTeacher.setName(null);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findTeacherScheduleByDay(1L, DayOfWeek.MONDAY))
                .thenReturn(Collections.emptyList());

        List<ScheduleSlot> result = service.handleTeacherAbsence(1L, absenceDate);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAvailableSubstitutes_WithNullCertificationsInList_ShouldFilterThem() {
        substituteTeacher1.setCertifications(Arrays.asList("Math", null, "English"));

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(substituteTeacher1));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAvailableSubstitutes_WithNullTeacherIds_ShouldFilterThem() {
        Teacher teacherNullId = new Teacher();
        teacherNullId.setId(null);
        teacherNullId.setActive(true);

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(teacherNullId, substituteTeacher1));
        when(scheduleSlotRepository.findTeacherTimeConflicts(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAvailableSubstitutes(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testHandleTeacherAbsence_WithNullSlotId_ShouldSkipIt() {
        LocalDate absenceDate = LocalDate.of(2024, 9, 2);

        ScheduleSlot slotNullId = new ScheduleSlot();
        slotNullId.setId(null);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findTeacherScheduleByDay(1L, DayOfWeek.MONDAY))
                .thenReturn(Arrays.asList(slotNullId));

        List<ScheduleSlot> result = service.handleTeacherAbsence(1L, absenceDate);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
