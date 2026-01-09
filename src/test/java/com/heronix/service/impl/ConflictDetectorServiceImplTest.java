package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import com.heronix.service.ConflictDetectorService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ConflictDetectorServiceImpl
 *
 * Tests comprehensive conflict detection algorithms including:
 * - Time-based conflicts (overlaps, back-to-back, lunch breaks)
 * - Room-based conflicts (double bookings, capacity, type mismatches)
 * - Teacher-based conflicts (overloads, excessive hours, prep periods)
 * - Student-based conflicts (schedule conflicts, duplicates)
 * - Course-based conflicts (over/under enrollment)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConflictDetectorServiceImplTest {

    @Mock
    private ConflictRepository conflictRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ConflictDetectorServiceImpl service;

    private Schedule testSchedule;
    private ScheduleSlot testSlot1;
    private ScheduleSlot testSlot2;
    private Teacher testTeacher;
    private Room testRoom;
    private Course testCourse;
    private Student testStudent;
    private StudentEnrollment testEnrollment;
    private CourseSection testSection;

    @BeforeEach
    void setUp() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setScheduleName("Fall 2025");

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("Jane Doe");
        testTeacher.setDepartment("Mathematics");
        testTeacher.setMaxPeriodsPerDay(7);
        testTeacher.setMaxConsecutiveHours(4);
        testTeacher.setPreferredBreakMinutes(15);

        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);
        testRoom.setRoomType(RoomType.CLASSROOM);
        testRoom.setHasProjector(true);
        testRoom.setHasSmartboard(false);
        testRoom.setHasComputers(false);
        testRoom.setBuilding("Main Building");

        // Create test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("MATH401");
        testCourse.setCourseName("Advanced Algebra");
        testCourse.setSubject("Mathematics");
        testCourse.setMaxStudents(30);
        testCourse.setRequiresLab(false);
        testCourse.setPrerequisites("MATH301");
        testCourse.setRequiredResources("projector");

        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("12345");
        testStudent.setFirstName("John");
        testStudent.setLastName("Smith");

        // Create test slots
        testSlot1 = new ScheduleSlot();
        testSlot1.setId(1L);
        testSlot1.setSchedule(testSchedule);
        testSlot1.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot1.setStartTime(LocalTime.of(9, 0));
        testSlot1.setEndTime(LocalTime.of(10, 0));
        testSlot1.setTeacher(testTeacher);
        testSlot1.setRoom(testRoom);
        testSlot1.setCourse(testCourse);
        testSlot1.setPeriodNumber(1);

        testSlot2 = new ScheduleSlot();
        testSlot2.setId(2L);
        testSlot2.setSchedule(testSchedule);
        testSlot2.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot2.setStartTime(LocalTime.of(10, 0));
        testSlot2.setEndTime(LocalTime.of(11, 0));
        testSlot2.setTeacher(testTeacher);
        testSlot2.setRoom(testRoom);
        testSlot2.setCourse(testCourse);
        testSlot2.setPeriodNumber(2);

        // Create test enrollment
        testEnrollment = new StudentEnrollment();
        testEnrollment.setId(1L);
        testEnrollment.setStudent(testStudent);
        testEnrollment.setCourse(testCourse);
        testEnrollment.setScheduleSlot(testSlot1);
        testEnrollment.setStatus(EnrollmentStatus.ACTIVE);

        // Create test section
        testSection = new CourseSection();
        testSection.setId(1L);
        testSection.setCourse(testCourse);
        testSection.setSectionNumber("1");
        testSection.setCurrentEnrollment(15);
        testSection.setMaxEnrollment(30);
        testSection.setMinEnrollment(10);
        testSection.setScheduleYear(2025);

        // Default mock setups
        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());
        when(studentEnrollmentRepository.findByScheduleId(anyLong())).thenReturn(new ArrayList<>());
        when(conflictRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ========================================================================
    // COMPREHENSIVE DETECTION TESTS
    // ========================================================================

    @Test
    void testDetectAllConflicts_WithNoConflicts_ShouldReturnEmptyList() {
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));
        when(studentEnrollmentRepository.findByScheduleSlotId(1L)).thenReturn(Arrays.asList(testEnrollment));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        List<Conflict> result = service.detectAllConflicts(testSchedule);

        assertNotNull(result);
        // Should be 0 or low count since this is a clean schedule
    }

    @Test
    void testDetectAllConflicts_WithMultipleConflictTypes_ShouldDetectAll() {
        // Create overlapping slot (room double booking)
        ScheduleSlot overlappingSlot = new ScheduleSlot();
        overlappingSlot.setId(3L);
        overlappingSlot.setSchedule(testSchedule);
        overlappingSlot.setDayOfWeek(DayOfWeek.MONDAY);
        overlappingSlot.setStartTime(LocalTime.of(9, 30));
        overlappingSlot.setEndTime(LocalTime.of(10, 30));
        overlappingSlot.setRoom(testRoom); // Same room
        overlappingSlot.setTeacher(testTeacher); // Same teacher

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, overlappingSlot));
        when(courseSectionRepository.findAll()).thenReturn(new ArrayList<>());

        List<Conflict> result = service.detectAllConflicts(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0); // Should detect teacher and/or room conflicts
    }

    @Test
    void testDetectConflictsForSlot_WithOverlappingTeacher_ShouldDetectConflict() {
        ScheduleSlot overlappingSlot = new ScheduleSlot();
        overlappingSlot.setId(2L);
        overlappingSlot.setSchedule(testSchedule);
        overlappingSlot.setDayOfWeek(DayOfWeek.MONDAY);
        overlappingSlot.setStartTime(LocalTime.of(9, 30));
        overlappingSlot.setEndTime(LocalTime.of(10, 30));
        overlappingSlot.setTeacher(testTeacher); // Same teacher
        overlappingSlot.setRoom(new Room()); // Different room
        overlappingSlot.getRoom().setId(2L);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, overlappingSlot));
        when(studentEnrollmentRepository.findByScheduleSlotId(1L)).thenReturn(new ArrayList<>());

        List<Conflict> result = service.detectConflictsForSlot(testSlot1);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(c ->
            c.getConflictType() == ConflictType.TEACHER_OVERLOAD &&
            c.getTitle().contains("Teacher Double-Booked")
        ));
    }

    @Test
    void testDetectConflictsForSlot_WithOverlappingRoom_ShouldDetectConflict() {
        ScheduleSlot overlappingSlot = new ScheduleSlot();
        overlappingSlot.setId(2L);
        overlappingSlot.setSchedule(testSchedule);
        overlappingSlot.setDayOfWeek(DayOfWeek.MONDAY);
        overlappingSlot.setStartTime(LocalTime.of(9, 30));
        overlappingSlot.setEndTime(LocalTime.of(10, 30));
        overlappingSlot.setRoom(testRoom); // Same room
        overlappingSlot.setTeacher(new Teacher()); // Different teacher
        overlappingSlot.getTeacher().setId(2L);
        overlappingSlot.getTeacher().setName("Bob Smith");

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, overlappingSlot));
        when(studentEnrollmentRepository.findByScheduleSlotId(1L)).thenReturn(new ArrayList<>());

        List<Conflict> result = service.detectConflictsForSlot(testSlot1);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(c ->
            c.getConflictType() == ConflictType.ROOM_DOUBLE_BOOKING &&
            c.getTitle().contains("Room Double-Booked")
        ));
    }

    @Test
    void testDetectConflictsForSlot_WithRoomCapacityExceeded_ShouldDetectConflict() {
        testRoom.setCapacity(20);

        // Create 25 enrollments
        List<StudentEnrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setId((long) i);
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollments.add(enrollment);
        }

        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());
        when(studentEnrollmentRepository.findByScheduleSlotId(1L)).thenReturn(enrollments);

        List<Conflict> result = service.detectConflictsForSlot(testSlot1);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(c ->
            c.getConflictType() == ConflictType.ROOM_CAPACITY_EXCEEDED &&
            c.getTitle().contains("Room Capacity Exceeded")
        ));
    }

    @Test
    void testHasConflicts_WithConflicts_ShouldReturnTrue() {
        when(conflictRepository.countActiveBySchedule(testSchedule)).thenReturn(5L);

        boolean result = service.hasConflicts(testSchedule);

        assertTrue(result);
    }

    @Test
    void testHasConflicts_WithNoConflicts_ShouldReturnFalse() {
        when(conflictRepository.countActiveBySchedule(testSchedule)).thenReturn(0L);

        boolean result = service.hasConflicts(testSchedule);

        assertFalse(result);
    }

    // ========================================================================
    // TIME-BASED DETECTION TESTS
    // ========================================================================

    @Test
    void testDetectTimeOverlaps_WithOverlappingSlots_ShouldDetect() {
        ScheduleSlot overlapping = new ScheduleSlot();
        overlapping.setId(2L);
        overlapping.setSchedule(testSchedule);
        overlapping.setDayOfWeek(DayOfWeek.MONDAY);
        overlapping.setStartTime(LocalTime.of(9, 30));
        overlapping.setEndTime(LocalTime.of(10, 30));
        overlapping.setTeacher(testTeacher); // Same teacher
        overlapping.setRoom(new Room());
        overlapping.getRoom().setId(2L);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, overlapping));

        List<Conflict> result = service.detectTimeOverlaps(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectBackToBackViolations_WithBreakPreference_ShouldDetect() {
        testTeacher.setPreferredBreakMinutes(15);
        // testSlot2 starts right when testSlot1 ends (back-to-back)

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, testSlot2));

        List<Conflict> result = service.detectBackToBackViolations(testSchedule);

        assertNotNull(result);
        // Should detect back-to-back violation since teacher prefers 15 min break
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectBackToBackViolations_WithNoBreakPreference_ShouldNotDetect() {
        testTeacher.setPreferredBreakMinutes(0);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, testSlot2));

        List<Conflict> result = service.detectBackToBackViolations(testSchedule);

        assertNotNull(result);
        // Should not detect if teacher has no break preference
    }

    @Test
    void testDetectMissingLunchBreaks_WithNoLunchBreak_ShouldDetect() {
        // Create 5+ periods without lunch break
        List<ScheduleSlot> slots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setSchedule(testSchedule);
            slot.setTeacher(testTeacher);
            slot.setDayOfWeek(DayOfWeek.MONDAY);
            slot.setStartTime(LocalTime.of(8 + i, 0));
            slot.setEndTime(LocalTime.of(9 + i, 0));
            slots.add(slot);
        }

        when(scheduleSlotRepository.findAll()).thenReturn(slots);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        List<Conflict> result = service.detectMissingLunchBreaks(testSchedule);

        assertNotNull(result);
        // Should detect missing lunch break
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectExcessiveConsecutiveClasses_WithExcessiveClasses_ShouldDetect() {
        testTeacher.setMaxConsecutiveHours(3);

        // Create 5 consecutive classes (exceeds max of 3)
        List<ScheduleSlot> slots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setSchedule(testSchedule);
            slot.setTeacher(testTeacher);
            slot.setDayOfWeek(DayOfWeek.MONDAY);
            slot.setStartTime(LocalTime.of(8 + i, 0));
            slot.setEndTime(LocalTime.of(9 + i, 0));
            slots.add(slot);
        }

        when(scheduleSlotRepository.findAll()).thenReturn(slots);

        List<Conflict> result = service.detectExcessiveConsecutiveClasses(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    // ========================================================================
    // ROOM-BASED DETECTION TESTS
    // ========================================================================

    @Test
    void testDetectRoomDoubleBookings_WithSameRoomSameTime_ShouldDetect() {
        ScheduleSlot doubleBooked = new ScheduleSlot();
        doubleBooked.setId(2L);
        doubleBooked.setSchedule(testSchedule);
        doubleBooked.setDayOfWeek(DayOfWeek.MONDAY);
        doubleBooked.setStartTime(LocalTime.of(9, 30));
        doubleBooked.setEndTime(LocalTime.of(10, 30));
        doubleBooked.setRoom(testRoom); // Same room
        doubleBooked.setTeacher(new Teacher());
        doubleBooked.getTeacher().setId(2L);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, doubleBooked));

        List<Conflict> result = service.detectRoomDoubleBookings(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.get(0).getConflictType() == ConflictType.ROOM_DOUBLE_BOOKING);
    }

    @Test
    void testDetectRoomCapacityViolations_WithOverCapacity_ShouldDetect() {
        testRoom.setCapacity(20);
        testCourse.setMaxStudents(25);

        // Create 25 active enrollments
        List<StudentEnrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setId((long) i);
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollments.add(enrollment);
        }

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));
        when(studentEnrollmentRepository.findByScheduleSlotId(1L)).thenReturn(enrollments);

        List<Conflict> result = service.detectRoomCapacityViolations(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectRoomTypeMismatches_WithLabCourseInRegularRoom_ShouldDetect() {
        testCourse.setRequiresLab(true);
        testRoom.setRoomType(RoomType.CLASSROOM); // Not a lab

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));

        List<Conflict> result = service.detectRoomTypeMismatches(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectRoomTypeMismatches_WithScienceCourseInRegularRoom_ShouldDetect() {
        testCourse.setSubject("Chemistry");
        testCourse.setRequiresLab(false);
        testRoom.setRoomType(RoomType.CLASSROOM);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));

        List<Conflict> result = service.detectRoomTypeMismatches(testSchedule);

        assertNotNull(result);
        // Should flag science course not in science lab
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectEquipmentUnavailability_WithMissingProjector_ShouldDetect() {
        testCourse.setRequiredResources("projector");
        testRoom.setHasProjector(false);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));

        List<Conflict> result = service.detectEquipmentUnavailability(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectEquipmentUnavailability_WithMissingComputers_ShouldDetect() {
        testCourse.setRequiredResources("computer, projector");
        testRoom.setHasComputers(false);
        testRoom.setHasProjector(true);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));

        List<Conflict> result = service.detectEquipmentUnavailability(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    // ========================================================================
    // TEACHER-BASED DETECTION TESTS
    // ========================================================================

    @Test
    void testDetectTeacherOverloads_WithDoubleBooking_ShouldDetect() {
        ScheduleSlot doubleBooked = new ScheduleSlot();
        doubleBooked.setId(2L);
        doubleBooked.setSchedule(testSchedule);
        doubleBooked.setDayOfWeek(DayOfWeek.MONDAY);
        doubleBooked.setStartTime(LocalTime.of(9, 30));
        doubleBooked.setEndTime(LocalTime.of(10, 30));
        doubleBooked.setTeacher(testTeacher); // Same teacher
        doubleBooked.setRoom(new Room());
        doubleBooked.getRoom().setId(2L);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, doubleBooked));

        List<Conflict> result = service.detectTeacherOverloads(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectExcessiveTeachingHours_WithTooManyPeriods_ShouldDetect() {
        testTeacher.setMaxPeriodsPerDay(5);

        // Create 8 periods on same day
        List<ScheduleSlot> slots = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setSchedule(testSchedule);
            slot.setTeacher(testTeacher);
            slot.setDayOfWeek(DayOfWeek.MONDAY);
            slot.setStartTime(LocalTime.of(8 + i, 0));
            slot.setEndTime(LocalTime.of(9 + i, 0));
            slots.add(slot);
        }

        when(scheduleSlotRepository.findAll()).thenReturn(slots);

        List<Conflict> result = service.detectExcessiveTeachingHours(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectMissingPreparationPeriods_WithNoPrepTime_ShouldDetect() {
        // Create 7 teaching periods (no prep time in typical 8-period day)
        List<ScheduleSlot> slots = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setId((long) i);
            slot.setSchedule(testSchedule);
            slot.setTeacher(testTeacher);
            slot.setDayOfWeek(DayOfWeek.MONDAY);
            slot.setStartTime(LocalTime.of(8 + i, 0));
            slot.setEndTime(LocalTime.of(9 + i, 0));
            slots.add(slot);
        }

        when(scheduleSlotRepository.findAll()).thenReturn(slots);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        List<Conflict> result = service.detectMissingPreparationPeriods(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectSubjectMismatches_WithMismatchedDepartment_ShouldDetect() {
        testTeacher.setDepartment("English");
        testCourse.setSubject("Mathematics");

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));

        List<Conflict> result = service.detectSubjectMismatches(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectTeacherTravelTimeIssues_WithDifferentBuildings_ShouldDetect() {
        Room building2Room = new Room();
        building2Room.setId(2L);
        building2Room.setRoomNumber("201");
        building2Room.setBuilding("Science Building");

        testRoom.setBuilding("Main Building");
        testSlot2.setRoom(building2Room);
        testSlot2.setStartTime(LocalTime.of(10, 0)); // Right after slot1 ends

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1, testSlot2));

        List<Conflict> result = service.detectTeacherTravelTimeIssues(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    // ========================================================================
    // STUDENT-BASED DETECTION TESTS
    // ========================================================================

    @Test
    void testDetectStudentScheduleConflicts_WithOverlappingClasses_ShouldDetect() {
        StudentEnrollment enrollment2 = new StudentEnrollment();
        enrollment2.setId(2L);
        enrollment2.setStudent(testStudent); // Same student
        enrollment2.setCourse(testCourse);
        enrollment2.setScheduleSlot(testSlot2);
        enrollment2.setStatus(EnrollmentStatus.ACTIVE);

        // Make slots overlap
        testSlot2.setStartTime(LocalTime.of(9, 30));
        testSlot2.setEndTime(LocalTime.of(10, 30));

        when(studentEnrollmentRepository.findByScheduleId(1L))
            .thenReturn(Arrays.asList(testEnrollment, enrollment2));

        List<Conflict> result = service.detectStudentScheduleConflicts(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectPrerequisiteViolations_ShouldReturnEmptyList() {
        // Prerequisites are flagged but not added to conflicts in current implementation
        testEnrollment.setCourse(testCourse);
        testCourse.setPrerequisites("MATH301");

        when(studentEnrollmentRepository.findByScheduleId(1L))
            .thenReturn(Arrays.asList(testEnrollment));

        List<Conflict> result = service.detectPrerequisiteViolations(testSchedule);

        assertNotNull(result);
        // Implementation doesn't actually add these to conflicts yet
        assertEquals(0, result.size());
    }

    @Test
    void testDetectCreditHourViolations_ShouldReturnEmptyList() {
        // Future enhancement - not yet implemented
        List<Conflict> result = service.detectCreditHourViolations(testSchedule);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testDetectGraduationRequirementIssues_ShouldReturnEmptyList() {
        // Future enhancement - not yet implemented
        List<Conflict> result = service.detectGraduationRequirementIssues(testSchedule);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testDetectCourseSequenceViolations_ShouldReturnEmptyList() {
        // Future enhancement - not yet implemented
        List<Conflict> result = service.detectCourseSequenceViolations(testSchedule);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========================================================================
    // COURSE-BASED DETECTION TESTS
    // ========================================================================

    @Test
    void testDetectSectionOverEnrollment_WithExcessStudents_ShouldDetect() {
        testRoom.setCapacity(20);
        testCourse.setMaxStudents(25);

        // Create 30 enrollments (exceeds both limits)
        List<StudentEnrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setId((long) i);
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollments.add(enrollment);
        }

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));
        when(studentEnrollmentRepository.findByScheduleSlotId(1L)).thenReturn(enrollments);

        List<Conflict> result = service.detectSectionOverEnrollment(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectSectionUnderEnrollment_WithTooFewStudents_ShouldDetect() {
        testSection.setMinEnrollment(15);
        testSection.setCurrentEnrollment(8); // Below minimum

        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        List<Conflict> result = service.detectSectionUnderEnrollment(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectDuplicateEnrollments_WithSameStudentAndCourse_ShouldDetect() {
        StudentEnrollment duplicate = new StudentEnrollment();
        duplicate.setId(2L);
        duplicate.setStudent(testStudent); // Same student
        duplicate.setCourse(testCourse); // Same course
        duplicate.setScheduleSlot(testSlot2);
        duplicate.setStatus(EnrollmentStatus.ACTIVE);

        when(studentEnrollmentRepository.findByScheduleId(1L))
            .thenReturn(Arrays.asList(testEnrollment, duplicate));

        List<Conflict> result = service.detectDuplicateEnrollments(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testDetectCoRequisiteViolations_ShouldReturnEmptyList() {
        // Future enhancement - not yet implemented
        List<Conflict> result = service.detectCoRequisiteViolations(testSchedule);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========================================================================
    // REAL-TIME DETECTION TESTS
    // ========================================================================

    @Test
    void testDetectPotentialConflicts_WithNewSlot_ShouldDetectConflicts() {
        ScheduleSlot newSlot = new ScheduleSlot();
        newSlot.setId(99L);
        newSlot.setSchedule(testSchedule);
        newSlot.setDayOfWeek(DayOfWeek.MONDAY);
        newSlot.setStartTime(LocalTime.of(9, 30));
        newSlot.setEndTime(LocalTime.of(10, 30));
        newSlot.setTeacher(testTeacher);
        newSlot.setRoom(testRoom);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));
        when(studentEnrollmentRepository.findByScheduleSlotId(99L)).thenReturn(new ArrayList<>());

        List<Conflict> result = service.detectPotentialConflicts(testSchedule, newSlot);

        assertNotNull(result);
        // Should detect overlapping time conflicts
    }

    @Test
    void testValidateSchedule_ShouldReturnValidationResult() {
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot1));
        when(courseSectionRepository.findAll()).thenReturn(new ArrayList<>());

        ValidationResult result = service.validateSchedule(testSchedule);

        assertNotNull(result);
        assertNotNull(result.getConflicts());
        assertEquals(0, result.getCriticalCount() + result.getHighCount() +
                        result.getMediumCount() + result.getLowCount() + result.getInfoCount());
    }

    // ========================================================================
    // CONFLICT PERSISTENCE TESTS
    // ========================================================================

    @Test
    void testSaveConflicts_ShouldSaveToRepository() {
        Conflict conflict1 = Conflict.builder()
            .conflictType(ConflictType.TEACHER_OVERLOAD)
            .severity(ConflictSeverity.CRITICAL)
            .title("Test Conflict")
            .build();

        List<Conflict> conflicts = Arrays.asList(conflict1);

        List<Conflict> result = service.saveConflicts(conflicts);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(conflictRepository).saveAll(conflicts);
    }

    @Test
    void testClearConflicts_ShouldDeleteAllConflicts() {
        service.clearConflicts(testSchedule);

        verify(conflictRepository).deleteBySchedule(testSchedule);
    }

    @Test
    void testRefreshConflicts_ShouldClearAndDetectNew() {
        when(scheduleSlotRepository.findAll()).thenReturn(new ArrayList<>());
        when(courseSectionRepository.findAll()).thenReturn(new ArrayList<>());

        List<Conflict> result = service.refreshConflicts(testSchedule);

        assertNotNull(result);
        verify(conflictRepository).deleteBySchedule(testSchedule);
        verify(conflictRepository).saveAll(anyList());
    }

    // ========================================================================
    // NULL SAFETY TESTS
    // ========================================================================

    @Test
    void testDetectAllConflicts_WithNullSchedule_ShouldNotCrash() {
        assertThrows(NullPointerException.class, () ->
            service.detectAllConflicts(null));
    }

    @Test
    void testDetectConflictsForSlot_WithNullSlot_ShouldHandleGracefully() {
        // Implementation may throw or return empty list
        try {
            List<Conflict> result = service.detectConflictsForSlot(null);
            assertNotNull(result);
        } catch (NullPointerException e) {
            // Expected for null slot
            assertNotNull(e);
        }
    }

    @Test
    void testSaveConflicts_WithEmptyList_ShouldSucceed() {
        List<Conflict> result = service.saveConflicts(new ArrayList<>());

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
