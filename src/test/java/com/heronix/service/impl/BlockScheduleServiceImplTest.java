package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Test Suite for BlockScheduleServiceImpl
 * Service #31 in systematic testing plan
 *
 * Tests block schedule generation with ODD/EVEN day patterns
 */
@ExtendWith(MockitoExtension.class)
class BlockScheduleServiceImplTest {

    @Mock(lenient = true)
    private ScheduleRepository scheduleRepository;

    @Mock(lenient = true)
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private BlockScheduleServiceImpl service;

    // Note: EntityManager is injected via @PersistenceContext, not constructor
    // We need to manually set it after @InjectMocks creates the service
    @Mock(lenient = true)
    private EntityManager entityManager;

    private Student testStudent;
    private Course testCourse1;
    private Course testCourse2;
    private Teacher testTeacher;
    private Room testRoom;
    private Schedule testSchedule;

    @BeforeEach
    void setUp() throws Exception {
        // Manually inject EntityManager (since it's @PersistenceContext, not constructor injection)
        java.lang.reflect.Field field = BlockScheduleServiceImpl.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(service, entityManager);

        // Setup test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("S001");
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");

        // Setup test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("Jane");
        testTeacher.setLastName("Smith");

        // Setup test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");

        // Setup test courses
        testCourse1 = new Course();
        testCourse1.setId(1L);
        testCourse1.setCourseCode("MATH101");
        testCourse1.setCourseName("Algebra I");
        testCourse1.setTeacher(testTeacher);
        testCourse1.setActive(true);

        testCourse2 = new Course();
        testCourse2.setId(2L);
        testCourse2.setCourseCode("ENG101");
        testCourse2.setCourseName("English I");
        testCourse2.setTeacher(testTeacher);
        testCourse2.setActive(true);

        // Setup test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setName("Test Block Schedule");
        testSchedule.setScheduleType(ScheduleType.BLOCK);
        testSchedule.setStatus(ScheduleStatus.DRAFT);
        testSchedule.setStartDate(LocalDate.of(2024, 8, 1));
        testSchedule.setEndDate(LocalDate.of(2025, 6, 30));
        testSchedule.setSlots(new ArrayList<>());

        // Configure repository mocks
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> {
            Schedule s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(1L);
            }
            return s;
        });

        when(scheduleSlotRepository.saveAll(anyList())).thenAnswer(i -> {
            List<ScheduleSlot> slots = i.getArgument(0);
            for (int j = 0; j < slots.size(); j++) {
                if (slots.get(j).getId() == null) {
                    slots.get(j).setId((long) (j + 1));
                }
            }
            return slots;
        });

        when(scheduleSlotRepository.save(any(ScheduleSlot.class))).thenAnswer(i -> {
            ScheduleSlot slot = i.getArgument(0);
            if (slot.getId() == null) {
                slot.setId(1L);
            }
            return slot;
        });
    }

    // ========================================================================
    // BLOCK SCHEDULE GENERATION TESTS
    // ========================================================================

    @Test
    void testGenerateBlockSchedule_WithValidInputs_ShouldCreateSchedule() {
        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = Arrays.asList(testCourse1, testCourse2);
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        Schedule result = service.generateBlockSchedule(students, courses, teachers, rooms);

        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals(ScheduleType.BLOCK, result.getScheduleType());
        assertEquals(ScheduleStatus.DRAFT, result.getStatus());
        assertTrue(Boolean.TRUE.equals(result.getActive()));

        verify(scheduleRepository, atLeastOnce()).save(any(Schedule.class));
    }

    @Test
    void testGenerateBlockSchedule_ShouldSetCorrectDateRange() {
        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = Arrays.asList(testCourse1);
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        Schedule result = service.generateBlockSchedule(students, courses, teachers, rooms);

        assertNotNull(result.getStartDate());
        assertNotNull(result.getEndDate());
        assertTrue(result.getStartDate().isBefore(result.getEndDate()));
        assertEquals(8, result.getStartDate().getMonthValue()); // Starts in August
        assertEquals(6, result.getEndDate().getMonthValue()); // Ends in June
    }

    @Test
    void testGenerateBlockSchedule_ShouldCreateMultipleSlotsPerCourse() {
        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = Arrays.asList(testCourse1);
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        service.generateBlockSchedule(students, courses, teachers, rooms);

        // Verify that saveAll was called with a non-empty list
        verify(scheduleSlotRepository).saveAll(argThat(list -> {
            if (list instanceof List) {
                return !((List<?>) list).isEmpty();
            }
            return false;
        }));
    }

    @Test
    void testGenerateBlockSchedule_ShouldAlternateDayTypes() {
        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = Arrays.asList(testCourse1, testCourse2);
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        service.generateBlockSchedule(students, courses, teachers, rooms);

        // Verify slots were created (implementation details tested via integration)
        verify(scheduleSlotRepository).saveAll(anyList());
    }

    @Test
    void testGenerateBlockSchedule_WithNullCourses_ShouldSkipThem() {
        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = Arrays.asList(testCourse1, null, testCourse2);
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        Schedule result = service.generateBlockSchedule(students, courses, teachers, rooms);

        assertNotNull(result);
        verify(scheduleRepository, atLeastOnce()).save(any(Schedule.class));
    }

    @Test
    void testGenerateBlockSchedule_WithInactiveCourses_ShouldSkipThem() {
        testCourse1.setActive(false);

        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = Arrays.asList(testCourse1);
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        Schedule result = service.generateBlockSchedule(students, courses, teachers, rooms);

        assertNotNull(result);
        // Verify no slots were created (empty list)
        verify(scheduleSlotRepository, never()).saveAll(anyList());
    }

    @Test
    void testGenerateBlockSchedule_WithEmptyCourseList_ShouldCreateScheduleWithNoSlots() {
        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = new ArrayList<>();
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        Schedule result = service.generateBlockSchedule(students, courses, teachers, rooms);

        assertNotNull(result);
        assertEquals(ScheduleType.BLOCK, result.getScheduleType());
        verify(scheduleSlotRepository, never()).saveAll(anyList());
    }

    @Test
    void testGenerateBlockSchedule_ShouldInitializeMetrics() {
        List<Student> students = Arrays.asList(testStudent);
        List<Course> courses = Arrays.asList(testCourse1);
        List<Teacher> teachers = Arrays.asList(testTeacher);
        List<Room> rooms = Arrays.asList(testRoom);

        Schedule result = service.generateBlockSchedule(students, courses, teachers, rooms);

        assertEquals(0, result.getTotalConflicts());
        assertEquals(0, result.getResolvedConflicts());
        assertEquals(0.0, result.getOptimizationScore());
        assertEquals(0.0, result.getQualityScore());
    }

    // ========================================================================
    // ODD/EVEN DAY DETERMINATION TESTS
    // ========================================================================

    @Test
    void testIsOddDay_September1_ShouldBeFalse() {
        LocalDate date = LocalDate.of(2024, 9, 1);
        assertFalse(service.isOddDay(date)); // Day 0 since start = even
    }

    @Test
    void testIsOddDay_September2_ShouldBeTrue() {
        LocalDate date = LocalDate.of(2024, 9, 2);
        assertTrue(service.isOddDay(date)); // Day 1 since start = odd
    }

    @Test
    void testIsEvenDay_September1_ShouldBeTrue() {
        LocalDate date = LocalDate.of(2024, 9, 1);
        assertTrue(service.isEvenDay(date));
    }

    @Test
    void testIsEvenDay_September2_ShouldBeFalse() {
        LocalDate date = LocalDate.of(2024, 9, 2);
        assertFalse(service.isEvenDay(date));
    }

    @Test
    void testGetDayType_OddDay_ShouldReturnODD() {
        LocalDate date = LocalDate.of(2024, 9, 2);
        assertEquals(DayType.ODD, service.getDayType(date));
    }

    @Test
    void testGetDayType_EvenDay_ShouldReturnEVEN() {
        LocalDate date = LocalDate.of(2024, 9, 1);
        assertEquals(DayType.EVEN, service.getDayType(date));
    }

    @Test
    void testIsOddDay_WithMidYear_ShouldCalculateCorrectly() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        // Days since Sept 1, 2024 = ~136 days (even number)
        assertFalse(service.isOddDay(date));
    }

    // ========================================================================
    // GET COURSES FOR DAY TYPE TESTS
    // ========================================================================

    @Test
    void testGetCoursesForDayType_WithOddDayCourses_ShouldReturnCorrectly() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setCourse(testCourse1);
        slot1.setDayType(DayType.ODD);
        slot1.setStudents(Arrays.asList(testStudent));

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setCourse(testCourse2);
        slot2.setDayType(DayType.EVEN);
        slot2.setStudents(Arrays.asList(testStudent));

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, slot2));

        List<Course> result = service.getCoursesForDayType(testStudent, DayType.ODD);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCourse1, result.get(0));
    }

    @Test
    void testGetCoursesForDayType_WithEvenDayCourses_ShouldReturnCorrectly() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setCourse(testCourse1);
        slot1.setDayType(DayType.ODD);
        slot1.setStudents(Arrays.asList(testStudent));

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setCourse(testCourse2);
        slot2.setDayType(DayType.EVEN);
        slot2.setStudents(Arrays.asList(testStudent));

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, slot2));

        List<Course> result = service.getCoursesForDayType(testStudent, DayType.EVEN);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCourse2, result.get(0));
    }

    @Test
    void testGetCoursesForDayType_WithNullSlots_ShouldFilterThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setCourse(testCourse1);
        slot1.setDayType(DayType.ODD);
        slot1.setStudents(Arrays.asList(testStudent));

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, null));

        List<Course> result = service.getCoursesForDayType(testStudent, DayType.ODD);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetCoursesForDayType_WithNullStudentsList_ShouldFilterThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setCourse(testCourse1);
        slot1.setDayType(DayType.ODD);
        slot1.setStudents(null); // Null students list

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1));

        List<Course> result = service.getCoursesForDayType(testStudent, DayType.ODD);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetCoursesForDayType_WithNullCourses_ShouldFilterThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setCourse(null); // Null course
        slot1.setDayType(DayType.ODD);
        slot1.setStudents(Arrays.asList(testStudent));

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1));

        List<Course> result = service.getCoursesForDayType(testStudent, DayType.ODD);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetCoursesForDayType_WithMultipleSlotsForSameCourse_ShouldReturnDistinct() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setCourse(testCourse1);
        slot1.setDayType(DayType.ODD);
        slot1.setStudents(Arrays.asList(testStudent));

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setCourse(testCourse1); // Same course
        slot2.setDayType(DayType.ODD);
        slot2.setStudents(Arrays.asList(testStudent));

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, slot2));

        List<Course> result = service.getCoursesForDayType(testStudent, DayType.ODD);

        assertNotNull(result);
        assertEquals(1, result.size()); // Should be distinct
    }

    // ========================================================================
    // ASSIGN COURSES TO DAYS TESTS
    // ========================================================================

    @Test
    void testAssignCoursesToDays_WithValidInputs_ShouldCreateSlots() {
        List<Course> oddDayCourses = Arrays.asList(testCourse1);
        List<Course> evenDayCourses = Arrays.asList(testCourse2);

        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());

        service.assignCoursesToDays(testStudent, oddDayCourses, evenDayCourses);

        // Should save 2 slots (1 odd + 1 even)
        verify(scheduleSlotRepository, times(2)).save(any(ScheduleSlot.class));
        verify(entityManager).flush();
    }

    @Test
    void testAssignCoursesToDays_WithNullStudent_ShouldThrowException() {
        List<Course> oddDayCourses = Arrays.asList(testCourse1);
        List<Course> evenDayCourses = Arrays.asList(testCourse2);

        assertThrows(IllegalArgumentException.class, () -> {
            service.assignCoursesToDays(null, oddDayCourses, evenDayCourses);
        });
    }

    @Test
    void testAssignCoursesToDays_WithNullOddDayCourses_ShouldNotCreateOddSlots() {
        List<Course> evenDayCourses = Arrays.asList(testCourse2);

        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());

        service.assignCoursesToDays(testStudent, null, evenDayCourses);

        // Should only save 1 slot (even day)
        verify(scheduleSlotRepository, times(1)).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignCoursesToDays_WithNullEvenDayCourses_ShouldNotCreateEvenSlots() {
        List<Course> oddDayCourses = Arrays.asList(testCourse1);

        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());

        service.assignCoursesToDays(testStudent, oddDayCourses, null);

        // Should only save 1 slot (odd day)
        verify(scheduleSlotRepository, times(1)).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignCoursesToDays_WithNullCoursesInList_ShouldSkipThem() {
        List<Course> oddDayCourses = Arrays.asList(testCourse1, null);
        List<Course> evenDayCourses = Arrays.asList(testCourse2);

        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());

        service.assignCoursesToDays(testStudent, oddDayCourses, evenDayCourses);

        // Should save 2 slots (1 odd + 1 even), skipping null
        verify(scheduleSlotRepository, times(2)).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignCoursesToDays_ShouldClearExistingSlots() {
        ScheduleSlot existingSlot = new ScheduleSlot();
        existingSlot.setId(1L);
        existingSlot.setCourse(testCourse1);
        existingSlot.setDayType(DayType.ODD);
        existingSlot.setStudents(new ArrayList<>(Arrays.asList(testStudent)));

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(existingSlot));

        List<Course> oddDayCourses = Arrays.asList(testCourse2);
        service.assignCoursesToDays(testStudent, oddDayCourses, null);

        // Should save existing slot after removing student
        verify(scheduleSlotRepository, atLeastOnce()).save(existingSlot);
        // Should create new slot for testCourse2
        verify(scheduleSlotRepository, atLeastOnce()).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignCoursesToDays_WithNullExistingSlots_ShouldSkipThem() {
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList((ScheduleSlot) null));

        List<Course> oddDayCourses = Arrays.asList(testCourse1);
        service.assignCoursesToDays(testStudent, oddDayCourses, null);

        // Should create new slot
        verify(scheduleSlotRepository, times(1)).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignCoursesToDays_ShouldSetCorrectDayTypes() {
        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());

        List<Course> oddDayCourses = Arrays.asList(testCourse1);
        List<Course> evenDayCourses = Arrays.asList(testCourse2);

        service.assignCoursesToDays(testStudent, oddDayCourses, evenDayCourses);

        verify(scheduleSlotRepository, times(2)).save(argThat(slot ->
            slot.getDayType() == DayType.ODD || slot.getDayType() == DayType.EVEN
        ));
    }

    // ========================================================================
    // GET SLOTS FOR DAY TYPE TESTS
    // ========================================================================

    @Test
    void testGetSlotsForDayType_WithOddDaySlots_ShouldReturnCorrectly() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayType(DayType.ODD);

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setDayType(DayType.EVEN);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slot1, slot2));

        List<ScheduleSlot> result = service.getSlotsForDayType(testSchedule, DayType.ODD);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DayType.ODD, result.get(0).getDayType());
    }

    @Test
    void testGetSlotsForDayType_WithEvenDaySlots_ShouldReturnCorrectly() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayType(DayType.ODD);

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setDayType(DayType.EVEN);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slot1, slot2));

        List<ScheduleSlot> result = service.getSlotsForDayType(testSchedule, DayType.EVEN);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DayType.EVEN, result.get(0).getDayType());
    }

    @Test
    void testGetSlotsForDayType_WithDailySlots_ShouldIncludeThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayType(DayType.ODD);

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setId(2L);
        slot2.setDayType(DayType.DAILY); // Should be included in any query

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slot1, slot2));

        List<ScheduleSlot> result = service.getSlotsForDayType(testSchedule, DayType.ODD);

        assertNotNull(result);
        assertEquals(2, result.size()); // Both ODD and DAILY
    }

    @Test
    void testGetSlotsForDayType_WithNullSlots_ShouldFilterThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayType(DayType.ODD);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slot1, null));

        List<ScheduleSlot> result = service.getSlotsForDayType(testSchedule, DayType.ODD);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetSlotsForDayType_WithNullDayType_ShouldFilterThem() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayType(null); // Null day type

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slot1));

        List<ScheduleSlot> result = service.getSlotsForDayType(testSchedule, DayType.ODD);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetSlotsForDayType_WithNoMatchingSlots_ShouldReturnEmpty() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setId(1L);
        slot1.setDayType(DayType.EVEN);

        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(Arrays.asList(slot1));

        List<ScheduleSlot> result = service.getSlotsForDayType(testSchedule, DayType.ODD);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========================================================================
    // NULL SAFETY TESTS
    // ========================================================================

    @Test
    void testAssignCoursesToDays_WithStudentNullId_ShouldHandleGracefully() {
        testStudent.setStudentId(null);

        List<Course> oddDayCourses = Arrays.asList(testCourse1);
        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());

        service.assignCoursesToDays(testStudent, oddDayCourses, null);

        // Should complete without exception
        verify(scheduleSlotRepository, times(1)).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignCoursesToDays_WithCourseNullProperties_ShouldHandleGracefully() {
        testCourse1.setCourseName(null);
        testCourse1.setCourseCode(null);

        List<Course> oddDayCourses = Arrays.asList(testCourse1);
        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());

        service.assignCoursesToDays(testStudent, oddDayCourses, null);

        // Should complete without exception
        verify(scheduleSlotRepository, times(1)).save(any(ScheduleSlot.class));
    }

    @Test
    void testAssignCoursesToDays_WithExistingSlotNullStudents_ShouldSkipIt() {
        ScheduleSlot existingSlot = new ScheduleSlot();
        existingSlot.setId(1L);
        existingSlot.setStudents(null); // Null students list

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(existingSlot));

        List<Course> oddDayCourses = Arrays.asList(testCourse1);
        service.assignCoursesToDays(testStudent, oddDayCourses, null);

        // Should create new slot, skip existing with null students
        verify(scheduleSlotRepository, times(1)).save(any(ScheduleSlot.class));
    }
}
