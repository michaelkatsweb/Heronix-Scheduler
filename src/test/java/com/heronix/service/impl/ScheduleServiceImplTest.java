package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.RoomType;
import com.heronix.model.enums.ScheduleStatus;
import com.heronix.model.enums.SlotStatus;
import com.heronix.repository.*;
import com.heronix.service.ScheduleGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for ScheduleServiceImpl
 *
 * Service: 29th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/ScheduleServiceImplTest.java
 *
 * Tests cover:
 * - CRUD operations for schedules
 * - Active schedule filtering
 * - Teacher and room utilization calculations
 * - Slot queries by teacher, room, and requirements
 * - Schedule publishing and archiving
 * - Schedule cloning with slots
 * - Schedule statistics
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock(lenient = true)
    private ScheduleRepository scheduleRepository;

    @Mock(lenient = true)
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @Mock(lenient = true)
    private RoomRepository roomRepository;

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @Mock(lenient = true)
    private ScheduleGenerationService scheduleGenerationService;

    @InjectMocks
    private ScheduleServiceImpl service;

    private Schedule testSchedule;
    private ScheduleSlot testSlot;
    private Teacher testTeacher;
    private Course testCourse;
    private Room testRoom;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("Mr. Smith");

        // Create test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");
        testCourse.setRequiresLab(false);

        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setType(RoomType.CLASSROOM);

        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Alice");
        testStudent.setLastName("Johnson");

        // Create test schedule slot
        testSlot = new ScheduleSlot();
        testSlot.setId(1L);
        testSlot.setTeacher(testTeacher);
        testSlot.setCourse(testCourse);
        testSlot.setRoom(testRoom);
        testSlot.setStatus(SlotStatus.ACTIVE);
        testSlot.setHasConflict(false);

        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setName("Test Schedule");
        testSchedule.setStatus(ScheduleStatus.DRAFT);
        testSchedule.setStartDate(LocalDate.of(2024, 1, 1));
        testSchedule.setEndDate(LocalDate.of(2024, 6, 30));
        testSchedule.setStartTime(LocalTime.of(8, 0));
        testSchedule.setEndTime(LocalTime.of(15, 0));
    }

    // ========== CRUD TESTS ==========

    @Test
    void testGetAllSchedules_ShouldReturnAll() {
        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(testSchedule));

        List<Schedule> result = service.getAllSchedules();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(scheduleRepository).findAll();
    }

    @Test
    void testGetScheduleById_WithExistingId_ShouldReturn() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        Optional<Schedule> result = service.getScheduleById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Schedule", result.get().getName());
    }

    @Test
    void testGetScheduleById_WithNonExistentId_ShouldReturnEmpty() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Schedule> result = service.getScheduleById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testSaveSchedule_ShouldSaveAndFlush() {
        when(scheduleRepository.saveAndFlush(any(Schedule.class))).thenAnswer(i -> i.getArgument(0));

        Schedule result = service.saveSchedule(testSchedule);

        assertNotNull(result);
        assertEquals("Test Schedule", result.getName());
        verify(scheduleRepository).saveAndFlush(testSchedule);
    }

    @Test
    void testDeleteSchedule_ShouldDeleteSlotsAndSchedule() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        service.deleteSchedule(1L);

        verify(scheduleSlotRepository).deleteAll(anyList());
        verify(scheduleRepository).deleteById(1L);
    }

    // ========== ACTIVE SCHEDULES TESTS ==========

    @Test
    void testGetActiveSchedules_ShouldExcludeArchived() {
        Schedule activeSchedule = new Schedule();
        activeSchedule.setStatus(ScheduleStatus.PUBLISHED);

        Schedule archivedSchedule = new Schedule();
        archivedSchedule.setStatus(ScheduleStatus.ARCHIVED);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(activeSchedule, archivedSchedule));

        List<Schedule> result = service.getActiveSchedules();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ScheduleStatus.PUBLISHED, result.get(0).getStatus());
    }

    @Test
    void testGetActiveSchedules_WithNullStatus_ShouldExclude() {
        Schedule activeSchedule = new Schedule();
        activeSchedule.setStatus(ScheduleStatus.PUBLISHED);

        Schedule nullStatusSchedule = new Schedule();
        nullStatusSchedule.setStatus(null);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(activeSchedule, nullStatusSchedule));

        List<Schedule> result = service.getActiveSchedules();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== UTILIZATION TESTS ==========

    @Test
    void testGetTeacherUtilization_WithFullUtilization_ShouldReturn100() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        double result = service.getTeacherUtilization(1L);

        assertEquals(1.0, result, 0.01);
    }

    @Test
    void testGetTeacherUtilization_WithPartialUtilization_ShouldReturnCorrect() {
        ScheduleSlot emptySlot = new ScheduleSlot();
        emptySlot.setTeacher(null);
        emptySlot.setStatus(SlotStatus.ACTIVE);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot, emptySlot));

        double result = service.getTeacherUtilization(1L);

        assertEquals(0.5, result, 0.01);
    }

    @Test
    void testGetTeacherUtilization_WithNoSlots_ShouldReturn0() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Collections.emptyList());

        double result = service.getTeacherUtilization(1L);

        assertEquals(0.0, result, 0.01);
    }

    @Test
    void testGetRoomUtilization_WithFullUtilization_ShouldReturn100() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        double result = service.getRoomUtilization(1L);

        assertEquals(1.0, result, 0.01);
    }

    @Test
    void testGetRoomUtilization_WithPartialUtilization_ShouldReturnCorrect() {
        ScheduleSlot emptySlot = new ScheduleSlot();
        emptySlot.setRoom(null);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot, emptySlot));

        double result = service.getRoomUtilization(1L);

        assertEquals(0.5, result, 0.01);
    }

    // ========== SLOT QUERY TESTS ==========

    @Test
    void testGetSlotsByTeacher_ShouldReturnMatchingSlots() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getSlotsByTeacher(1L, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTeacher.getId(), result.get(0).getTeacher().getId());
    }

    @Test
    void testGetSlotsByTeacher_WithNullTeacher_ShouldFilterOut() {
        testSlot.setTeacher(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getSlotsByTeacher(1L, 1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSlotsByRoom_ShouldReturnMatchingSlots() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getSlotsByRoom(1L, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRoom.getId(), result.get(0).getRoom().getId());
    }

    @Test
    void testGetSlotsByRoom_WithNullRoom_ShouldFilterOut() {
        testSlot.setRoom(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getSlotsByRoom(1L, 1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== LAB ROOM TESTS ==========

    @Test
    void testGetLabRooms_ShouldReturnLabRooms() {
        Room scienceLab = new Room();
        scienceLab.setType(RoomType.SCIENCE_LAB);

        Room computerLab = new Room();
        computerLab.setType(RoomType.COMPUTER_LAB);

        Room classroom = new Room();
        classroom.setType(RoomType.CLASSROOM);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(scienceLab, computerLab, classroom));

        List<Room> result = service.getLabRooms();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetSlotsNeedingLabs_ShouldReturnLabSlots() {
        testCourse.setRequiresLab(true);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getSlotsNeedingLabs(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetSlotsNeedingLabs_WithNullCourse_ShouldFilterOut() {
        testSlot.setCourse(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getSlotsNeedingLabs(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== UNASSIGNED SLOTS TESTS ==========

    @Test
    void testGetUnassignedSlots_WithMissingTeacher_ShouldReturn() {
        testSlot.setTeacher(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getUnassignedSlots(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetUnassignedSlots_WithMissingRoom_ShouldReturn() {
        testSlot.setRoom(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getUnassignedSlots(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetUnassignedSlots_WithFullyAssigned_ShouldReturnEmpty() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        List<ScheduleSlot> result = service.getUnassignedSlots(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== PUBLISH TESTS ==========

    @Test
    void testPublishSchedule_ShouldUpdateStatus() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> i.getArgument(0));

        service.publishSchedule(1L);

        assertEquals(ScheduleStatus.PUBLISHED, testSchedule.getStatus());
        verify(scheduleRepository).save(testSchedule);
    }

    @Test
    void testPublishSchedule_WithNonExistentId_ShouldThrowException() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            service.publishSchedule(999L);
        });
    }

    // ========== CLONE TESTS ==========

    @Test
    void testCloneSchedule_ShouldCreateCopy() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> {
            Schedule s = i.getArgument(0);
            s.setId(2L);
            return s;
        });
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));
        when(scheduleSlotRepository.save(any(ScheduleSlot.class))).thenAnswer(i -> i.getArgument(0));

        Schedule result = service.cloneSchedule(1L);

        assertNotNull(result);
        assertTrue(result.getName().contains("Copy"));
        assertEquals(ScheduleStatus.DRAFT, result.getStatus());
        verify(scheduleSlotRepository).save(any(ScheduleSlot.class));
    }

    @Test
    void testCloneSchedule_WithNonExistentId_ShouldThrowException() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            service.cloneSchedule(999L);
        });
    }

    @Test
    void testCloneSchedule_WithNoSlots_ShouldStillClone() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> {
            Schedule s = i.getArgument(0);
            s.setId(2L);
            return s;
        });
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Collections.emptyList());

        Schedule result = service.cloneSchedule(1L);

        assertNotNull(result);
        verify(scheduleSlotRepository, never()).save(any(ScheduleSlot.class));
    }

    // ========== ARCHIVE TESTS ==========

    @Test
    void testArchiveSchedule_ShouldUpdateStatus() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> i.getArgument(0));

        service.archiveSchedule(1L);

        assertEquals(ScheduleStatus.ARCHIVED, testSchedule.getStatus());
        verify(scheduleRepository).save(testSchedule);
    }

    @Test
    void testArchiveSchedule_WithNonExistentId_ShouldThrowException() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            service.archiveSchedule(999L);
        });
    }

    // ========== STATISTICS TESTS ==========

    @Test
    void testGetScheduleStatistics_WithFullyAssigned_ShouldReturnCorrect() {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        String result = service.getScheduleStatistics(1L);

        assertTrue(result.contains("Total: 1"));
        assertTrue(result.contains("Assigned: 1"));
        assertTrue(result.contains("Conflicts: 0"));
    }

    @Test
    void testGetScheduleStatistics_WithConflicts_ShouldReportConflicts() {
        testSlot.setHasConflict(true);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        String result = service.getScheduleStatistics(1L);

        assertTrue(result.contains("Conflicts: 1"));
    }

    @Test
    void testGetScheduleStatistics_WithUnassigned_ShouldReportUnassigned() {
        ScheduleSlot unassigned = new ScheduleSlot();
        unassigned.setTeacher(null);
        unassigned.setRoom(null);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot, unassigned));

        String result = service.getScheduleStatistics(1L);

        assertTrue(result.contains("Total: 2"));
        assertTrue(result.contains("Assigned: 1"));
    }

    @Test
    void testGetScheduleStatistics_WithNullHasConflict_ShouldNotCrash() {
        testSlot.setHasConflict(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        String result = service.getScheduleStatistics(1L);

        assertNotNull(result);
        assertTrue(result.contains("Conflicts: 0"));
    }

    // ========== NULL SAFETY TESTS ==========

    @Test
    void testGetTeacherUtilization_WithNullStatus_ShouldNotCount() {
        testSlot.setStatus(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        double result = service.getTeacherUtilization(1L);

        assertEquals(0.0, result, 0.01);
    }

    @Test
    void testGetTeacherUtilization_WithInactiveStatus_ShouldNotCount() {
        testSlot.setStatus(SlotStatus.CANCELLED);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(testSlot));

        double result = service.getTeacherUtilization(1L);

        assertEquals(0.0, result, 0.01);
    }

    @Test
    void testGetActiveSchedules_WithEmptyList_ShouldReturnEmpty() {
        when(scheduleRepository.findAll()).thenReturn(Collections.emptyList());

        List<Schedule> result = service.getActiveSchedules();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
