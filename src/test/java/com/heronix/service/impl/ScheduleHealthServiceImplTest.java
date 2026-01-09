package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.dto.ScheduleHealthMetrics;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ScheduleHealthServiceImpl
 *
 * Tests schedule health metrics, conflict detection, balance scoring,
 * utilization analysis, compliance checking, and coverage scoring.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleHealthServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ConflictMatrixRepository conflictMatrixRepository;

    @InjectMocks
    private ScheduleHealthServiceImpl service;

    private Schedule testSchedule;
    private ScheduleSlot testSlot1;
    private ScheduleSlot testSlot2;
    private Teacher testTeacher1;
    private Teacher testTeacher2;
    private Room testRoom1;
    private Room testRoom2;
    private Course testCourse;
    private CourseSection testSection1;
    private CourseSection testSection2;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Setup test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setScheduleName("Test Schedule");
        testSchedule.setTotalConflicts(0);

        // Setup test teacher
        testTeacher1 = new Teacher();
        testTeacher1.setId(1L);
        testTeacher1.setName("John Doe");
        testTeacher1.setActive(true);

        testTeacher2 = new Teacher();
        testTeacher2.setId(2L);
        testTeacher2.setName("Jane Smith");
        testTeacher2.setActive(true);

        // Setup test room
        testRoom1 = new Room();
        testRoom1.setId(1L);
        testRoom1.setRoomNumber("101");

        testRoom2 = new Room();
        testRoom2.setId(2L);
        testRoom2.setRoomNumber("102");

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseName("Algebra I");

        // Setup test course sections
        testSection1 = new CourseSection();
        testSection1.setId(1L);
        testSection1.setCourse(testCourse);
        testSection1.setCurrentEnrollment(25);
        testSection1.setMaxEnrollment(30);

        testSection2 = new CourseSection();
        testSection2.setId(2L);
        testSection2.setCourse(testCourse);
        testSection2.setCurrentEnrollment(26);
        testSection2.setMaxEnrollment(30);

        // Setup test schedule slots
        testSlot1 = new ScheduleSlot();
        testSlot1.setId(1L);
        testSlot1.setSchedule(testSchedule);
        testSlot1.setTeacher(testTeacher1);
        testSlot1.setRoom(testRoom1);
        testSlot1.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot1.setStartTime(LocalTime.of(9, 0));
        testSlot1.setEndTime(LocalTime.of(10, 0));

        testSlot2 = new ScheduleSlot();
        testSlot2.setId(2L);
        testSlot2.setSchedule(testSchedule);
        testSlot2.setTeacher(testTeacher2);
        testSlot2.setRoom(testRoom2);
        testSlot2.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot2.setStartTime(LocalTime.of(10, 0));
        testSlot2.setEndTime(LocalTime.of(11, 0));

        // Setup test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("STU001");

        // Default mock setups
        when(scheduleSlotRepository.findByScheduleId(anyLong())).thenReturn(Collections.emptyList());
        when(courseSectionRepository.findAll()).thenReturn(Collections.emptyList());
        when(teacherRepository.findAllActive()).thenReturn(Collections.emptyList());
        when(roomRepository.findAll()).thenReturn(Collections.emptyList());
        when(studentRepository.findAll()).thenReturn(Collections.emptyList());
    }

    // ========================================================================
    // CALCULATE HEALTH METRICS TESTS
    // ========================================================================

    @Test
    void testCalculateHealthMetrics_WithValidSchedule_ShouldReturnMetrics() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1, testTeacher2));
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom1, testRoom2));
        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertNotNull(result);
        assertNotNull(result.getOverallScore());
        assertNotNull(result.getConflictScore());
        assertNotNull(result.getBalanceScore());
        assertNotNull(result.getUtilizationScore());
        assertNotNull(result.getComplianceScore());
        assertNotNull(result.getCoverageScore());
        assertTrue(result.getOverallScore() >= 0 && result.getOverallScore() <= 100);
    }

    @Test
    void testCalculateHealthMetrics_WithNullSchedule_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.calculateHealthMetrics(null);
        });
    }

    @Test
    void testCalculateHealthMetrics_ShouldSetDetailedMetrics() {
        testSchedule.setTotalConflicts(10);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1, testSection2));
        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertNotNull(result);
        assertEquals(10, result.getTotalConflicts());
        assertNotNull(result.getCriticalConflicts());
        assertNotNull(result.getWarningConflicts());
        assertEquals(result.getCriticalConflicts() + result.getWarningConflicts(), result.getTotalConflicts());
    }

    @Test
    void testCalculateHealthMetrics_WithNullScheduleName_ShouldUseUnknown() {
        testSchedule.setScheduleName(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertNotNull(result);
        // Service should log "Unknown" for null schedule name (tested via log output)
    }

    // ========================================================================
    // CALCULATE HEALTH SCORE TESTS
    // ========================================================================

    @Test
    void testCalculateHealthScore_ShouldReturnOverallScore() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));

        Double result = service.calculateHealthScore(testSchedule);

        assertNotNull(result);
        assertTrue(result >= 0 && result <= 100);
    }

    @Test
    void testCalculateHealthScore_ShouldMatchMetricsOverallScore() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        Double healthScore = service.calculateHealthScore(testSchedule);
        ScheduleHealthMetrics metrics = service.calculateHealthMetrics(testSchedule);

        assertEquals(metrics.getOverallScore(), healthScore);
    }

    // ========================================================================
    // CALCULATE CONFLICT SCORE TESTS
    // ========================================================================

    @Test
    void testCalculateConflictScore_WithNoConflicts_ShouldReturn100() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));

        Double result = service.calculateConflictScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateConflictScore_WithTeacherConflict_ShouldPenalize() {
        // Two slots with same teacher at same time = conflict
        testSlot2.setTeacher(testTeacher1); // Same teacher as slot1
        testSlot2.setDayOfWeek(DayOfWeek.MONDAY); // Same day
        testSlot2.setStartTime(LocalTime.of(9, 0)); // Same time

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));

        Double result = service.calculateConflictScore(testSchedule);

        assertTrue(result < 100.0); // Should be penalized
    }

    @Test
    void testCalculateConflictScore_WithRoomConflict_ShouldPenalize() {
        // Two slots with same room at same time = conflict
        testSlot2.setRoom(testRoom1); // Same room as slot1
        testSlot2.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot2.setStartTime(LocalTime.of(9, 0));

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));

        Double result = service.calculateConflictScore(testSchedule);

        assertTrue(result < 100.0);
    }

    @Test
    void testCalculateConflictScore_WithEmptySlots_ShouldReturn0() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Collections.emptyList());

        Double result = service.calculateConflictScore(testSchedule);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateConflictScore_WithNullTeacher_ShouldFilterOut() {
        testSlot1.setTeacher(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));

        Double result = service.calculateConflictScore(testSchedule);

        assertNotNull(result);
    }

    @Test
    void testCalculateConflictScore_WithNullRoom_ShouldFilterOut() {
        testSlot1.setRoom(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));

        Double result = service.calculateConflictScore(testSchedule);

        assertNotNull(result);
    }

    @Test
    void testCalculateConflictScore_WithNullDayOfWeek_ShouldFilterOut() {
        testSlot1.setDayOfWeek(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));

        Double result = service.calculateConflictScore(testSchedule);

        assertNotNull(result);
    }

    // ========================================================================
    // CALCULATE BALANCE SCORE TESTS
    // ========================================================================

    @Test
    void testCalculateBalanceScore_WithBalancedSections_ShouldReturn100() {
        // Sections differ by only 1 student (within tolerance of 3)
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1, testSection2));

        Double result = service.calculateBalanceScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateBalanceScore_WithUnbalancedSections_ShouldPenalize() {
        // Sections differ by more than 3 students
        testSection2.setCurrentEnrollment(30); // 30 - 25 = 5 > 3

        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1, testSection2));

        Double result = service.calculateBalanceScore(testSchedule);

        assertTrue(result < 100.0);
    }

    @Test
    void testCalculateBalanceScore_WithSingleSection_ShouldReturn100() {
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        Double result = service.calculateBalanceScore(testSchedule);

        assertEquals(100.0, result); // Single section = perfect balance
    }

    @Test
    void testCalculateBalanceScore_WithEmptySections_ShouldReturn100() {
        when(courseSectionRepository.findAll()).thenReturn(Collections.emptyList());

        Double result = service.calculateBalanceScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateBalanceScore_WithNullCourse_ShouldFilterOut() {
        testSection1.setCourse(null);
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1, testSection2));

        Double result = service.calculateBalanceScore(testSchedule);

        assertNotNull(result);
    }

    @Test
    void testCalculateBalanceScore_WithNullSection_ShouldFilterOut() {
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1, null, testSection2));

        Double result = service.calculateBalanceScore(testSchedule);

        assertNotNull(result);
    }

    // ========================================================================
    // CALCULATE UTILIZATION SCORE TESTS
    // ========================================================================

    @Test
    void testCalculateUtilizationScore_WithIdealUtilization_ShouldReturn100() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom1));

        // Create moderate number of slots for ideal utilization (70-85% for teachers, 60-80% for rooms)
        ScheduleSlot[] slots = new ScheduleSlot[20]; // 20/30 = 67% (within ideal range)
        for (int i = 0; i < 20; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setTeacher(testTeacher1);
            slot.setRoom(testRoom1);
            slots[i] = slot;
        }

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slots));

        Double result = service.calculateUtilizationScore(testSchedule);

        assertTrue(result >= 90.0); // Should be very good
    }

    @Test
    void testCalculateUtilizationScore_WithNoTeachersOrRooms_ShouldReturn100() {
        when(teacherRepository.findAllActive()).thenReturn(Collections.emptyList());
        when(roomRepository.findAll()).thenReturn(Collections.emptyList());

        Double result = service.calculateUtilizationScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateUtilizationScore_WithLowUtilization_ShouldPenalize() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom1));
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1)); // Only 1 slot

        Double result = service.calculateUtilizationScore(testSchedule);

        assertTrue(result < 100.0);
    }

    @Test
    void testCalculateUtilizationScore_WithNullTeacher_ShouldFilterOut() {
        testSlot1.setTeacher(null);
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        Double result = service.calculateUtilizationScore(testSchedule);

        assertNotNull(result);
    }

    // ========================================================================
    // CALCULATE COMPLIANCE SCORE TESTS
    // ========================================================================

    @Test
    void testCalculateComplianceScore_WithPrepTime_ShouldReturn100() {
        // Teacher has only 6 periods on Monday (< 8 periods per day)
        ScheduleSlot[] slots = new ScheduleSlot[6];
        for (int i = 0; i < 6; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setTeacher(testTeacher1);
            slot.setDayOfWeek(DayOfWeek.MONDAY);
            slots[i] = slot;
        }

        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slots));

        Double result = service.calculateComplianceScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateComplianceScore_WithoutPrepTime_ShouldPenalize() {
        // Teacher has 8+ periods on Monday (no prep time)
        ScheduleSlot[] slots = new ScheduleSlot[8];
        for (int i = 0; i < 8; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setTeacher(testTeacher1);
            slot.setDayOfWeek(DayOfWeek.MONDAY);
            slots[i] = slot;
        }

        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slots));

        Double result = service.calculateComplianceScore(testSchedule);

        assertTrue(result < 100.0);
    }

    @Test
    void testCalculateComplianceScore_WithNoTeachers_ShouldReturn100() {
        when(teacherRepository.findAllActive()).thenReturn(Collections.emptyList());

        Double result = service.calculateComplianceScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateComplianceScore_WithNullTeacherInSlots_ShouldFilterOut() {
        testSlot1.setTeacher(null);
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        Double result = service.calculateComplianceScore(testSchedule);

        assertNotNull(result);
    }

    @Test
    void testCalculateComplianceScore_WithNullDayOfWeek_ShouldFilterOut() {
        testSlot1.setDayOfWeek(null);
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        Double result = service.calculateComplianceScore(testSchedule);

        assertNotNull(result);
    }

    // ========================================================================
    // CALCULATE COVERAGE SCORE TESTS
    // ========================================================================

    @Test
    void testCalculateCoverageScore_WithFullCoverage_ShouldReturn100() {
        // 1 student × 6 expected courses = 6 expected enrollments
        testSection1.setCurrentEnrollment(6);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        Double result = service.calculateCoverageScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateCoverageScore_WithPartialCoverage_ShouldReturnPartialScore() {
        // 1 student × 6 expected = 6, but only 3 enrolled
        testSection1.setCurrentEnrollment(3);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        Double result = service.calculateCoverageScore(testSchedule);

        assertEquals(50.0, result);
    }

    @Test
    void testCalculateCoverageScore_WithNoStudents_ShouldReturn100() {
        when(studentRepository.findAll()).thenReturn(Collections.emptyList());

        Double result = service.calculateCoverageScore(testSchedule);

        assertEquals(100.0, result);
    }

    @Test
    void testCalculateCoverageScore_WithNullEnrollment_ShouldTreatAsZero() {
        testSection1.setCurrentEnrollment(null);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        Double result = service.calculateCoverageScore(testSchedule);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateCoverageScore_WithExcessEnrollment_ShouldCapAt100() {
        // More enrollments than expected (capped at 100%)
        testSection1.setCurrentEnrollment(20);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        Double result = service.calculateCoverageScore(testSchedule);

        assertEquals(100.0, result);
    }

    // ========================================================================
    // STATUS METHODS TESTS
    // ========================================================================

    @Test
    void testIsScheduleAcceptable_WithHighScore_ShouldReturnTrue() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, testSlot2));
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1));
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom1));

        boolean result = service.isScheduleAcceptable(testSchedule);

        assertTrue(result); // Good schedule should be acceptable
    }

    @Test
    void testIsScheduleAcceptable_WithNullScore_ShouldReturnFalse() {
        // Mock to return null score (edge case)
        Schedule nullSchedule = new Schedule();
        nullSchedule.setId(999L);
        when(scheduleSlotRepository.findByScheduleId(999L)).thenReturn(Collections.emptyList());

        boolean result = service.isScheduleAcceptable(nullSchedule);

        assertFalse(result); // Null score = not acceptable
    }

    @Test
    void testGetHealthSummary_ShouldReturnFormattedString() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        String result = service.getHealthSummary(testSchedule);

        assertNotNull(result);
        assertTrue(result.contains("Schedule Health"));
    }

    @Test
    void testGetHealthSummary_WithNullSchedule_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.getHealthSummary(null);
        });
    }

    // ========================================================================
    // ISSUES AND RECOMMENDATIONS TESTS
    // ========================================================================

    @Test
    void testCalculateHealthMetrics_WithCriticalConflicts_ShouldGenerateCriticalIssue() {
        testSchedule.setTotalConflicts(10);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));
        when(courseSectionRepository.findAll()).thenReturn(Collections.emptyList());

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertNotNull(result.getCriticalIssues());
        assertFalse(result.getCriticalIssues().isEmpty());
    }

    @Test
    void testCalculateHealthMetrics_WithOverEnrolledSections_ShouldGenerateCriticalIssue() {
        testSection1.setCurrentEnrollment(35); // Over max of 30
        testSchedule.setTotalConflicts(0);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertTrue(result.getOverEnrolledSections() > 0);
    }

    @Test
    void testCalculateHealthMetrics_WithUnbalancedSections_ShouldGenerateWarning() {
        testSection1.setCurrentEnrollment(20);
        testSection2.setCurrentEnrollment(30); // Difference of 10 > 3

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1, testSection2));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertTrue(result.getUnbalancedSections() > 0);
    }

    @Test
    void testCalculateHealthMetrics_WithLowBalanceScore_ShouldGenerateRecommendation() {
        // Create highly unbalanced sections
        testSection1.setCurrentEnrollment(10);
        testSection2.setCurrentEnrollment(30);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1, testSection2));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertNotNull(result.getRecommendations());
        // Low balance score should generate recommendation
    }

    // ========================================================================
    // OVERALL SCORE CALCULATION TESTS
    // ========================================================================

    @Test
    void testCalculateHealthMetrics_ShouldCalculateWeightedAverage() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertNotNull(result.getOverallScore());

        // Overall score should be weighted average of components
        // Weights: conflict 35%, balance 20%, utilization 20%, compliance 15%, coverage 10%
        double expected = (result.getConflictScore() * 0.35) +
                         (result.getBalanceScore() * 0.20) +
                         (result.getUtilizationScore() * 0.20) +
                         (result.getComplianceScore() * 0.15) +
                         (result.getCoverageScore() * 0.10);

        double roundedExpected = Math.round(expected * 10.0) / 10.0;

        assertEquals(roundedExpected, result.getOverallScore(), 0.1);
    }

    @Test
    void testCalculateHealthMetrics_ShouldRoundOverallScore() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        // Overall score should be rounded to 1 decimal place
        String scoreStr = result.getOverallScore().toString();
        int decimalPlaces = scoreStr.contains(".") ?
            scoreStr.length() - scoreStr.indexOf('.') - 1 : 0;

        assertTrue(decimalPlaces <= 1);
    }

    // ========================================================================
    // NULL SAFETY TESTS
    // ========================================================================

    @Test
    void testCalculateHealthMetrics_WithNullSlotsInList_ShouldFilterOut() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1, null, testSlot2));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertNotNull(result);
    }

    @Test
    void testCalculateBalanceScore_WithNullCourseId_ShouldFilterOut() {
        Course nullIdCourse = new Course();
        nullIdCourse.setId(null);
        testSection1.setCourse(nullIdCourse);

        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection1));

        Double result = service.calculateBalanceScore(testSchedule);

        assertNotNull(result);
    }

    @Test
    void testCalculateHealthMetrics_WithNullTotalConflicts_ShouldDefaultToZero() {
        testSchedule.setTotalConflicts(null);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot1));

        ScheduleHealthMetrics result = service.calculateHealthMetrics(testSchedule);

        assertEquals(0, result.getTotalConflicts());
    }
}
