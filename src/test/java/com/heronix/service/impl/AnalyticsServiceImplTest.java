package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.dto.DashboardMetrics;
import com.heronix.model.dto.Recommendation;
import com.heronix.model.enums.ScheduleStatus;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for AnalyticsServiceImpl
 *
 * Service: 16th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/AnalyticsServiceImplTest.java
 *
 * Tests cover:
 * - Dashboard metrics calculation (null safety)
 * - Teacher utilization calculations
 * - Room utilization calculations
 * - Schedule quality calculations
 * - Recommendation generation (workload, room, quality)
 * - Efficiency reports
 * - Lean metrics
 * - Statistical calculations
 * - Edge cases and null handling
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock(lenient = true)
    private ScheduleRepository scheduleRepository;

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @Mock(lenient = true)
    private RoomRepository roomRepository;

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @Mock(lenient = true)
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private AnalyticsServiceImpl service;

    private Teacher testTeacher;
    private Room testRoom;
    private Course testCourse;
    private Schedule testSchedule;
    private ScheduleSlot testSlot;

    @BeforeEach
    void setUp() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Doe");
        testTeacher.setActive(true);
        testTeacher.setMaxHoursPerWeek(35);

        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setActive(true);
        testRoom.setCapacity(30);

        // Create test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");
        testCourse.setActive(true);

        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setScheduleName("Fall 2025");
        testSchedule.setStatus(ScheduleStatus.DRAFT);
        testSchedule.setOptimizationScore(85.5);
        testSchedule.setTeacherUtilization(75.0);
        testSchedule.setRoomUtilization(80.0);
        testSchedule.setEfficiencyRate(82.0);
        testSchedule.setTotalConflicts(5);
        testSchedule.setResolvedConflicts(3);

        // Create test schedule slot
        testSlot = new ScheduleSlot();
        testSlot.setId(1L);
        testSlot.setTeacher(testTeacher);
        testSlot.setRoom(testRoom);
        testSlot.setCourse(testCourse);
        testSlot.setPeriodNumber(1);
        testSlot.setDayOfWeek(DayOfWeek.MONDAY);

        testSchedule.setSlots(new ArrayList<>(Arrays.asList(testSlot)));
    }

    // ========== DASHBOARD METRICS TESTS ==========

    @Test
    void testGetDashboardMetrics_WithValidData_ShouldReturnMetrics() {
        when(teacherRepository.countByActiveTrue()).thenReturn(50L);
        when(courseRepository.count()).thenReturn(100L);
        when(roomRepository.count()).thenReturn(30L);
        when(scheduleRepository.count()).thenReturn(5L);

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(testTeacher));
        when(courseRepository.findByActiveTrue()).thenReturn(Arrays.asList(testCourse));
        when(roomRepository.findByActiveTrue()).thenReturn(Arrays.asList(testRoom));

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(testSchedule));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        DashboardMetrics result = service.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(50L, result.getTotalTeachers());
        assertEquals(100L, result.getTotalCourses());
        assertEquals(30L, result.getTotalRooms());
        assertEquals(5L, result.getTotalSchedules());
        assertEquals(1, result.getActiveTeachers());
        assertEquals(1, result.getActiveCourses());
        assertEquals(1, result.getActiveRooms());
    }

    @Test
    void testGetDashboardMetrics_WithNullSchedules_ShouldNotCrash() {
        when(teacherRepository.countByActiveTrue()).thenReturn(0L);
        when(courseRepository.count()).thenReturn(0L);
        when(roomRepository.count()).thenReturn(0L);
        when(scheduleRepository.count()).thenReturn(0L);

        when(teacherRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(courseRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(roomRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList((Schedule) null));

        DashboardMetrics result = service.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(0.0, result.getAverageOptimizationScore());
    }

    @Test
    void testGetDashboardMetrics_WithSchedulesHavingNullStatus_ShouldFilterCorrectly() {
        Schedule scheduleNullStatus = new Schedule();
        scheduleNullStatus.setId(2L);
        scheduleNullStatus.setStatus(null);

        when(teacherRepository.countByActiveTrue()).thenReturn(0L);
        when(teacherRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(courseRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(roomRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(testSchedule, scheduleNullStatus));

        DashboardMetrics result = service.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(1, result.getDraftSchedules()); // Only testSchedule counted
    }

    @Test
    void testGetDashboardMetrics_WithSchedulesHavingNullOptimizationScore_ShouldReturnZero() {
        testSchedule.setOptimizationScore(null);

        when(teacherRepository.countByActiveTrue()).thenReturn(0L);
        when(teacherRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(courseRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(roomRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(testSchedule));

        DashboardMetrics result = service.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(0.0, result.getAverageOptimizationScore());
    }

    @Test
    void testGetDashboardMetrics_WithExceptionThrown_ShouldReturnEmptyMetrics() {
        when(teacherRepository.countByActiveTrue()).thenThrow(new RuntimeException("Database error"));

        DashboardMetrics result = service.getDashboardMetrics();

        assertNotNull(result);
        // Should return a new DashboardMetrics object, not crash
    }

    // ========== TEACHER UTILIZATION TESTS ==========

    @Test
    void testCalculateTeacherUtilization_WithValidTeacher_ShouldReturnPercentage() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot, testSlot, testSlot));

        double result = service.calculateTeacherUtilization(1L);

        assertTrue(result > 0.0);
        assertEquals((3 * 100.0) / 35, result, 0.01);
    }

    @Test
    void testCalculateTeacherUtilization_WithNullTeacherId_ShouldReturnZero() {
        double result = service.calculateTeacherUtilization(null);

        assertEquals(0.0, result);
        verify(teacherRepository, never()).findById(anyLong());
    }

    @Test
    void testCalculateTeacherUtilization_WithNonExistentTeacher_ShouldReturnZero() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        double result = service.calculateTeacherUtilization(999L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateTeacherUtilization_WithNullMaxHours_ShouldReturnZero() {
        testTeacher.setMaxHoursPerWeek(null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        double result = service.calculateTeacherUtilization(1L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateTeacherUtilization_WithZeroMaxHours_ShouldReturnZero() {
        testTeacher.setMaxHoursPerWeek(0);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        double result = service.calculateTeacherUtilization(1L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateTeacherUtilization_WithNullSlotTeacher_ShouldNotCount() {
        testSlot.setTeacher(null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        double result = service.calculateTeacherUtilization(1L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateTeacherUtilization_WithNullSlotTeacherId_ShouldNotCount() {
        Teacher teacherNullId = new Teacher();
        teacherNullId.setId(null);
        testSlot.setTeacher(teacherNullId);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        double result = service.calculateTeacherUtilization(1L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateTeacherUtilization_WithExceptionThrown_ShouldReturnZero() {
        when(teacherRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        double result = service.calculateTeacherUtilization(1L);

        assertEquals(0.0, result);
    }

    // ========== ROOM UTILIZATION TESTS ==========

    @Test
    void testCalculateRoomUtilization_WithValidRoom_ShouldReturnPercentage() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot, testSlot));

        double result = service.calculateRoomUtilization(1L);

        assertTrue(result > 0.0);
        assertEquals((2 * 100.0) / 35, result, 0.01);
    }

    @Test
    void testCalculateRoomUtilization_WithNullRoomId_ShouldReturnZero() {
        double result = service.calculateRoomUtilization(null);

        assertEquals(0.0, result);
        verify(roomRepository, never()).findById(anyLong());
    }

    @Test
    void testCalculateRoomUtilization_WithNonExistentRoom_ShouldReturnZero() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        double result = service.calculateRoomUtilization(999L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateRoomUtilization_WithNullSlotRoom_ShouldNotCount() {
        testSlot.setRoom(null);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        double result = service.calculateRoomUtilization(1L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateRoomUtilization_WithNullSlotRoomId_ShouldNotCount() {
        Room roomNullId = new Room();
        roomNullId.setId(null);
        testSlot.setRoom(roomNullId);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        double result = service.calculateRoomUtilization(1L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateRoomUtilization_WithExceptionThrown_ShouldReturnZero() {
        when(roomRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        double result = service.calculateRoomUtilization(1L);

        assertEquals(0.0, result);
    }

    // ========== SCHEDULE QUALITY TESTS ==========

    @Test
    void testCalculateScheduleQuality_WithOptimizationScore_ShouldReturnScore() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        double result = service.calculateScheduleQuality(1L);

        assertEquals(85.5, result);
    }

    @Test
    void testCalculateScheduleQuality_WithoutOptimizationScore_ShouldCalculateAverage() {
        testSchedule.setOptimizationScore(null);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        double result = service.calculateScheduleQuality(1L);

        double expected = (75.0 + 80.0 + 82.0) / 3.0;
        assertEquals(expected, result, 0.01);
    }

    @Test
    void testCalculateScheduleQuality_WithNullScheduleId_ShouldReturnZero() {
        double result = service.calculateScheduleQuality(null);

        assertEquals(0.0, result);
        verify(scheduleRepository, never()).findById(anyLong());
    }

    @Test
    void testCalculateScheduleQuality_WithNonExistentSchedule_ShouldReturnZero() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        double result = service.calculateScheduleQuality(999L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateScheduleQuality_WithAllNullMetrics_ShouldReturnZero() {
        testSchedule.setOptimizationScore(null);
        testSchedule.setTeacherUtilization(null);
        testSchedule.setRoomUtilization(null);
        testSchedule.setEfficiencyRate(null);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        double result = service.calculateScheduleQuality(1L);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateScheduleQuality_WithExceptionThrown_ShouldReturnZero() {
        when(scheduleRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        double result = service.calculateScheduleQuality(1L);

        assertEquals(0.0, result);
    }

    // ========== RECOMMENDATIONS TESTS ==========

    @Test
    void testGetRecommendations_WithValidSchedule_ShouldReturnList() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        // Result may be empty or contain recommendations depending on schedule state
    }

    @Test
    void testGetRecommendations_WithNullScheduleId_ShouldReturnEmptyList() {
        List<Recommendation> result = service.getRecommendations(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(scheduleRepository, never()).findById(anyLong());
    }

    @Test
    void testGetRecommendations_WithNonExistentSchedule_ShouldReturnEmptyList() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        List<Recommendation> result = service.getRecommendations(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRecommendations_WithOverloadedTeacher_ShouldIncludeWorkloadRec() {
        // Create 36 slots for same teacher to trigger overload warning
        List<ScheduleSlot> manySlots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setTeacher(testTeacher);
            slot.setRoom(testRoom);
            manySlots.add(slot);
        }
        testSchedule.setSlots(manySlots);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(r -> "WORKLOAD_BALANCE".equals(r.getType())));
    }

    @Test
    void testGetRecommendations_WithUnderutilizedRoom_ShouldIncludeRoomRec() {
        // Create schedule with room used only 5 times
        List<ScheduleSlot> fewSlots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setTeacher(testTeacher);
            slot.setRoom(testRoom);
            fewSlots.add(slot);
        }
        testSchedule.setSlots(fewSlots);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(r -> "ROOM_OPTIMIZATION".equals(r.getType())));
    }

    @Test
    void testGetRecommendations_WithLowOptimizationScore_ShouldIncludeQualityRec() {
        testSchedule.setOptimizationScore(65.0); // Below 70 threshold

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(r -> "SCHEDULE_QUALITY".equals(r.getType())));
    }

    @Test
    void testGetRecommendations_WithNullSlots_ShouldNotCrash() {
        testSchedule.setSlots(null);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        // Should not crash, may have quality recommendation
    }

    @Test
    void testGetRecommendations_WithExceptionThrown_ShouldReturnEmptyList() {
        when(scheduleRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== EFFICIENCY REPORT TESTS ==========

    @Test
    void testGenerateEfficiencyReport_WithValidSchedule_ShouldReturnMap() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Map<String, Object> result = service.generateEfficiencyReport(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("scheduleId"));
        assertEquals("Fall 2025", result.get("scheduleName"));
        assertEquals(85.5, result.get("optimizationScore"));
        assertEquals(75.0, result.get("teacherUtilization"));
        assertEquals(80.0, result.get("roomUtilization"));
        assertEquals(82.0, result.get("efficiencyRate"));
        assertEquals(1, result.get("totalSlots"));
        assertEquals(5, result.get("conflicts"));
        assertEquals(3, result.get("resolvedConflicts"));
    }

    @Test
    void testGenerateEfficiencyReport_WithNullScheduleId_ShouldReturnEmptyMap() {
        Map<String, Object> result = service.generateEfficiencyReport(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(scheduleRepository, never()).findById(anyLong());
    }

    @Test
    void testGenerateEfficiencyReport_WithNonExistentSchedule_ShouldReturnEmptyMap() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.generateEfficiencyReport(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGenerateEfficiencyReport_WithNullScheduleName_ShouldUseDefault() {
        testSchedule.setScheduleName(null);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Map<String, Object> result = service.generateEfficiencyReport(1L);

        assertNotNull(result);
        assertEquals("Unknown", result.get("scheduleName"));
    }

    @Test
    void testGenerateEfficiencyReport_WithAllNullMetrics_ShouldUseDefaults() {
        testSchedule.setOptimizationScore(null);
        testSchedule.setTeacherUtilization(null);
        testSchedule.setRoomUtilization(null);
        testSchedule.setEfficiencyRate(null);
        testSchedule.setSlots(null);
        testSchedule.setTotalConflicts(null);
        testSchedule.setResolvedConflicts(null);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Map<String, Object> result = service.generateEfficiencyReport(1L);

        assertNotNull(result);
        assertEquals(0.0, result.get("optimizationScore"));
        assertEquals(0.0, result.get("teacherUtilization"));
        assertEquals(0.0, result.get("roomUtilization"));
        assertEquals(0.0, result.get("efficiencyRate"));
        assertEquals(0, result.get("totalSlots"));
        assertEquals(0, result.get("conflicts"));
        assertEquals(0, result.get("resolvedConflicts"));
    }

    @Test
    void testGenerateEfficiencyReport_WithExceptionThrown_ShouldReturnEmptyMap() {
        when(scheduleRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        Map<String, Object> result = service.generateEfficiencyReport(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== WORKLOAD DISTRIBUTION TESTS ==========

    @Test
    void testGetTeacherWorkloadDistribution_WithValidData_ShouldReturnMap() {
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot, testSlot, testSlot));

        Map<Long, Integer> result = service.getTeacherWorkloadDistribution();

        assertNotNull(result);
        assertEquals(3, result.get(1L));
    }

    @Test
    void testGetTeacherWorkloadDistribution_WithNoSlots_ShouldReturnEmptyMap() {
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        Map<Long, Integer> result = service.getTeacherWorkloadDistribution();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTeacherWorkloadDistribution_WithNullSlots_ShouldNotCount() {
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList((ScheduleSlot) null, testSlot));

        Map<Long, Integer> result = service.getTeacherWorkloadDistribution();

        assertNotNull(result);
        assertEquals(1, result.get(1L));
    }

    @Test
    void testGetTeacherWorkloadDistribution_WithNullTeacher_ShouldNotCount() {
        testSlot.setTeacher(null);
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        Map<Long, Integer> result = service.getTeacherWorkloadDistribution();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTeacherWorkloadDistribution_WithExceptionThrown_ShouldReturnEmptyMap() {
        when(scheduleSlotRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        Map<Long, Integer> result = service.getTeacherWorkloadDistribution();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== ROOM USAGE STATISTICS TESTS ==========

    @Test
    void testGetRoomUsageStatistics_WithValidData_ShouldReturnMap() {
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot, testSlot));

        Map<Long, Double> result = service.getRoomUsageStatistics();

        assertNotNull(result);
        assertTrue(result.containsKey(1L));
        assertEquals((2 * 100.0) / 35.0, result.get(1L), 0.01);
    }

    @Test
    void testGetRoomUsageStatistics_WithNoRooms_ShouldReturnEmptyMap() {
        when(roomRepository.findAll()).thenReturn(Collections.emptyList());
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        Map<Long, Double> result = service.getRoomUsageStatistics();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRoomUsageStatistics_WithNullRoom_ShouldSkip() {
        when(roomRepository.findAll()).thenReturn(Arrays.asList((Room) null, testRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        Map<Long, Double> result = service.getRoomUsageStatistics();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(1L));
    }

    @Test
    void testGetRoomUsageStatistics_WithNullRoomId_ShouldSkip() {
        Room roomNullId = new Room();
        roomNullId.setId(null);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(roomNullId, testRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot));

        Map<Long, Double> result = service.getRoomUsageStatistics();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(1L));
    }

    @Test
    void testGetRoomUsageStatistics_WithExceptionThrown_ShouldReturnEmptyMap() {
        when(roomRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        Map<Long, Double> result = service.getRoomUsageStatistics();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== LEAN METRICS TESTS ==========

    @Test
    void testCalculateLeanMetrics_WithValidSchedule_ShouldReturnMap() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Map<String, Double> result = service.calculateLeanMetrics(1L);

        assertNotNull(result);
        assertEquals(75.0, result.get("teacherUtilization"));
        assertEquals(80.0, result.get("roomUtilization"));
        assertEquals(82.0, result.get("efficiencyRate"));
        assertEquals(85.5, result.get("optimizationScore"));
        assertTrue(result.containsKey("wasteRate"));
    }

    @Test
    void testCalculateLeanMetrics_WithNullScheduleId_ShouldReturnEmptyMap() {
        Map<String, Double> result = service.calculateLeanMetrics(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(scheduleRepository, never()).findById(anyLong());
    }

    @Test
    void testCalculateLeanMetrics_WithNonExistentSchedule_ShouldReturnEmptyMap() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, Double> result = service.calculateLeanMetrics(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCalculateLeanMetrics_WithNullSlots_ShouldUseZeroWaste() {
        testSchedule.setSlots(null);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Map<String, Double> result = service.calculateLeanMetrics(1L);

        assertNotNull(result);
        assertEquals(0.0, result.get("wasteRate"));
    }

    @Test
    void testCalculateLeanMetrics_WithEmptySlotsTeacherOrRoom_ShouldCalculateWaste() {
        ScheduleSlot emptySlot = new ScheduleSlot();
        emptySlot.setId(2L);
        emptySlot.setTeacher(null); // Empty teacher
        emptySlot.setRoom(testRoom);

        testSchedule.setSlots(Arrays.asList(testSlot, emptySlot));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Map<String, Double> result = service.calculateLeanMetrics(1L);

        assertNotNull(result);
        assertEquals(50.0, result.get("wasteRate")); // 1 empty out of 2 = 50%
    }

    @Test
    void testCalculateLeanMetrics_WithAllNullMetrics_ShouldUseDefaults() {
        testSchedule.setTeacherUtilization(null);
        testSchedule.setRoomUtilization(null);
        testSchedule.setEfficiencyRate(null);
        testSchedule.setOptimizationScore(null);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Map<String, Double> result = service.calculateLeanMetrics(1L);

        assertNotNull(result);
        assertEquals(0.0, result.get("teacherUtilization"));
        assertEquals(0.0, result.get("roomUtilization"));
        assertEquals(0.0, result.get("efficiencyRate"));
        assertEquals(0.0, result.get("optimizationScore"));
    }

    @Test
    void testCalculateLeanMetrics_WithExceptionThrown_ShouldReturnEmptyMap() {
        when(scheduleRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        Map<String, Double> result = service.calculateLeanMetrics(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== PRIVATE METHOD INTEGRATION TESTS ==========

    @Test
    void testWorkloadRecommendations_WithNullTeacherName_ShouldUseDefault() {
        testTeacher.setFirstName(null);
        testTeacher.setLastName(null);

        List<ScheduleSlot> manySlots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setTeacher(testTeacher);
            manySlots.add(slot);
        }
        testSchedule.setSlots(manySlots);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(r ->
            r.getDescription().contains("Unknown Teacher") ||
            r.getDescription().contains("null")));
    }

    @Test
    void testRoomOptimizationRecommendations_WithNullRoomNumber_ShouldUseDefault() {
        testRoom.setRoomNumber(null);

        List<ScheduleSlot> fewSlots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setRoom(testRoom);
            fewSlots.add(slot);
        }
        testSchedule.setSlots(fewSlots);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(r ->
            r.getDescription().contains("Unknown Room")));
    }

    @Test
    void testQualityRecommendations_WithNullScheduleName_ShouldUseDefault() {
        testSchedule.setScheduleName(null);
        testSchedule.setOptimizationScore(65.0);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        List<Recommendation> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(r ->
            "Unknown Schedule".equals(r.getAffectedEntityName())));
    }
}
