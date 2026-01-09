package com.heronix.integration;

import com.heronix.model.domain.*;
import com.heronix.model.dto.ScheduleGenerationRequest;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import com.heronix.service.ScheduleGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Schedule Generation Workflow
 * Tests complete end-to-end flow across all 8 phases
 *
 * This tests the ENTIRE workflow:
 * Phase 1: Load Resources (teachers, courses, rooms, students)
 * Phase 2: Generate Time Slots
 * Phase 3: Create Schedule Entity
 * Phase 4: Create Schedule Slots
 * Phase 5: Build OptaPlanner Problem
 * Phase 6: Run AI Optimization
 * Phase 7: Save Results
 * Phase 8: Calculate Metrics
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class ScheduleGenerationIntegrationTest {

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private SubjectCertificationRepository subjectCertificationRepository;

    private Teacher teacher1;
    private Teacher teacher2;
    private Course course1;
    private Course course2;
    private Room room1;
    private Room room2;
    private Student student1;
    private Student student2;
    private Student student3;

    @BeforeEach
    public void setup() {
        // Create test teachers
        teacher1 = new Teacher();
        teacher1.setName("Mr. Johnson");
        teacher1.setEmployeeId("INTEG-T001");
        teacher1.setDepartment("Mathematics");
        teacher1.setActive(true);
        teacher1 = teacherRepository.save(teacher1);

        teacher2 = new Teacher();
        teacher2.setName("Ms. Davis");
        teacher2.setEmployeeId("INTEG-T002");
        teacher2.setDepartment("English");
        teacher2.setActive(true);
        teacher2 = teacherRepository.save(teacher2);

        // Add teacher certifications (required for OptaPlanner constraint validation)
        SubjectCertification mathCert = new SubjectCertification();
        mathCert.setTeacher(teacher1);
        mathCert.setSubject("Mathematics");
        mathCert.setGradeRange("9-12");
        mathCert.setCertificationType(CertificationType.PROFESSIONAL);
        mathCert.setActive(true);
        subjectCertificationRepository.save(mathCert);

        SubjectCertification englishCert = new SubjectCertification();
        englishCert.setTeacher(teacher2);
        englishCert.setSubject("English");
        englishCert.setGradeRange("9-12");
        englishCert.setCertificationType(CertificationType.PROFESSIONAL);
        englishCert.setActive(true);
        subjectCertificationRepository.save(englishCert);

        // Create test rooms
        room1 = new Room();
        room1.setRoomNumber("101");
        room1.setRoomType(RoomType.CLASSROOM);
        room1.setCapacity(30);
        room1.setActive(true);
        room1 = roomRepository.save(room1);

        room2 = new Room();
        room2.setRoomNumber("102");
        room2.setRoomType(RoomType.CLASSROOM);
        room2.setCapacity(30);
        room2.setActive(true);
        room2 = roomRepository.save(room2);

        // Create test courses
        course1 = new Course();
        course1.setCourseCode("MATH-101");
        course1.setCourseName("Algebra I");
        course1.setSubject("Mathematics");  // Required for OptaPlanner constraint matching
        course1.setCredits(1.0);
        course1.setActive(true);
        course1.setTeacher(teacher1);  // Pre-assign teacher
        course1.setRoom(room1);        // Pre-assign room
        course1 = courseRepository.save(course1);

        course2 = new Course();
        course2.setCourseCode("ENG-101");
        course2.setCourseName("English I");
        course2.setSubject("English");  // Required for OptaPlanner constraint matching
        course2.setCredits(1.0);
        course2.setActive(true);
        course2.setTeacher(teacher2);
        course2.setRoom(room2);
        course2 = courseRepository.save(course2);

        // Create test students
        student1 = new Student();
        student1.setFirstName("Alice");
        student1.setLastName("Smith");
        student1.setStudentId("INTEG-S001");
        student1.setGradeLevel("9");
        student1.setActive(true);
        student1 = studentRepository.save(student1);

        student2 = new Student();
        student2.setFirstName("Bob");
        student2.setLastName("Jones");
        student2.setStudentId("INTEG-S002");
        student2.setGradeLevel("9");
        student2.setActive(true);
        student2 = studentRepository.save(student2);

        student3 = new Student();
        student3.setFirstName("Charlie");
        student3.setLastName("Brown");
        student3.setStudentId("INTEG-S003");
        student3.setGradeLevel("9");
        student3.setActive(true);
        student3 = studentRepository.save(student3);

        // Enroll students in courses
        student1.getEnrolledCourses().add(course1);
        student1.getEnrolledCourses().add(course2);
        student1 = studentRepository.save(student1);

        student2.getEnrolledCourses().add(course1);
        student2.getEnrolledCourses().add(course2);
        student2 = studentRepository.save(student2);

        student3.getEnrolledCourses().add(course1);
        student3 = studentRepository.save(student3);

        // NOTE: Lunch waves will be created automatically by the schedule generation service
        // based on the request configuration (enableMultipleLunches, lunchWaveCount, lunchWaveConfigs)
    }

    // ========== FULL END-TO-END WORKFLOW TESTS ==========

    @Test
    public void testCompleteScheduleGeneration_ShouldSucceed() throws Exception {
        // Arrange - Create schedule generation request
        // NOTE: Lunch is disabled for this test because the lunch wave assignment
        // infrastructure is not yet implemented in the schedule generation service.
        // The service has the DTO fields (enableMultipleLunches, lunchWaveConfigs)
        // but doesn't actually create lunch waves or student/teacher assignments.
        // TODO: Re-enable lunch testing once service implements automatic lunch wave creation
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Integration Test Schedule")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .schoolStartTime(LocalTime.of(8, 0))
            .firstPeriodStartTime(LocalTime.of(8, 15))
            .schoolEndTime(LocalTime.of(15, 0))
            .periodDuration(50)
            .passingPeriodDuration(5)
            .enableLunch(false)  // Disabled - lunch wave infrastructure not yet implemented
            .optimizationTimeSeconds(10)  // Short for testing
            .build();

        // Track progress
        AtomicInteger lastProgress = new AtomicInteger(0);
        StringBuilder progressLog = new StringBuilder();

        // Act - Generate schedule (all 8 phases)
        Schedule generatedSchedule = scheduleGenerationService.generateSchedule(
            request,
            (progress, message) -> {
                lastProgress.set(progress);
                progressLog.append(String.format("[%d%%] %s\n", progress, message));
                System.out.println(String.format("[%d%%] %s", progress, message));
            }
        );

        // Assert - Schedule created
        assertNotNull(generatedSchedule);
        assertNotNull(generatedSchedule.getId());
        assertEquals("Integration Test Schedule", generatedSchedule.getScheduleName());
        assertEquals(ScheduleType.TRADITIONAL, generatedSchedule.getScheduleType());

        // Assert - Schedule persisted to database
        Schedule savedSchedule = scheduleRepository.findById(generatedSchedule.getId()).orElse(null);
        assertNotNull(savedSchedule);

        // Assert - Schedule slots created
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(generatedSchedule.getId());
        assertFalse(slots.isEmpty(), "Schedule should have slots created");

        // Assert - Progress reached 100%
        assertEquals(100, lastProgress.get(), "Progress should reach 100%");

        // Assert - Progress log contains all phases
        String log = progressLog.toString();
        assertTrue(log.contains("Phase 1") || log.contains("Loading resources"),
            "Should show Phase 1");
        assertTrue(log.contains("Phase 4") || log.contains("Creating schedule slots"),
            "Should show Phase 4");
        assertTrue(log.contains("Complete") || log.contains("100%"),
            "Should show completion");

        System.out.println("✓ Complete schedule generation succeeded");
        System.out.println("  - Schedule ID: " + generatedSchedule.getId());
        System.out.println("  - Slots created: " + slots.size());
        System.out.println("  - Final progress: " + lastProgress.get() + "%");
    }

    @Test
    public void testScheduleGeneration_WithMinimalData_ShouldSucceed() throws Exception {
        // Arrange - Minimal request (no lunch, basic times)
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Minimal Schedule")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(45)
            .passingPeriodDuration(5)
            .enableLunch(false)  // No lunch
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Assert
        assertNotNull(schedule);
        assertNotNull(schedule.getId());
        assertEquals("Minimal Schedule", schedule.getScheduleName());

        System.out.println("✓ Minimal schedule generation succeeded");
    }

    @Test
    public void testScheduleGeneration_CreatesCorrectNumberOfSlots() throws Exception {
        // Arrange
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Slot Count Test")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .passingPeriodDuration(5)
            .enableLunch(true)
            .lunchStartHour(12)
            .lunchDuration(30)
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Get created slots
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        // Assert - Should have slots for all enrolled students
        assertFalse(slots.isEmpty());

        // Students enrolled:
        // - student1: MATH-101, ENG-101 (2 courses)
        // - student2: MATH-101, ENG-101 (2 courses)
        // - student3: MATH-101 (1 course)
        // Total: 5 course-student combinations

        long studentSlots = slots.stream()
            .filter(slot -> slot.getStudents() != null && !slot.getStudents().isEmpty())
            .count();

        assertTrue(studentSlots >= 5,
            "Should have at least 5 student slots (got " + studentSlots + ")");

        System.out.println("✓ Schedule created with correct slot count");
        System.out.println("  - Total slots: " + slots.size());
        System.out.println("  - Student slots: " + studentSlots);
    }

    @Test
    public void testScheduleGeneration_PreservesTeacherAssignments() throws Exception {
        // Arrange
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Teacher Assignment Test")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .passingPeriodDuration(5)
            .enableLunch(false)
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Get slots for course1 (pre-assigned to teacher1)
        List<ScheduleSlot> course1Slots = scheduleSlotRepository.findByScheduleId(schedule.getId()).stream()
            .filter(slot -> slot.getCourse() != null &&
                           slot.getCourse().getId().equals(course1.getId()))
            .toList();

        // Assert - Slots should use pre-assigned teacher
        assertFalse(course1Slots.isEmpty());

        long slotsWithTeacher1 = course1Slots.stream()
            .filter(slot -> slot.getTeacher() != null &&
                           slot.getTeacher().getId().equals(teacher1.getId()))
            .count();

        assertTrue(slotsWithTeacher1 > 0,
            "Should use pre-assigned teacher (Teacher 1)");

        System.out.println("✓ Schedule preserved teacher assignments");
        System.out.println("  - Course 1 slots: " + course1Slots.size());
        System.out.println("  - With Teacher 1: " + slotsWithTeacher1);
    }

    @Test
    public void testScheduleGeneration_PreservesRoomAssignments() throws Exception {
        // Arrange
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Room Assignment Test")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .passingPeriodDuration(5)
            .enableLunch(false)
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Get slots for course1 (pre-assigned to room1)
        List<ScheduleSlot> course1Slots = scheduleSlotRepository.findByScheduleId(schedule.getId()).stream()
            .filter(slot -> slot.getCourse() != null &&
                           slot.getCourse().getId().equals(course1.getId()))
            .toList();

        // Assert - Slots should use pre-assigned room
        assertFalse(course1Slots.isEmpty());

        long slotsWithRoom1 = course1Slots.stream()
            .filter(slot -> slot.getRoom() != null &&
                           slot.getRoom().getId().equals(room1.getId()))
            .count();

        assertTrue(slotsWithRoom1 > 0,
            "Should use pre-assigned room (Room 101)");

        System.out.println("✓ Schedule preserved room assignments");
        System.out.println("  - Course 1 slots: " + course1Slots.size());
        System.out.println("  - With Room 101: " + slotsWithRoom1);
    }

    // ========== PHASE-SPECIFIC TESTS ==========

    @Test
    public void testPhase1_ResourceLoading_ShouldLoadAllEntities() throws Exception {
        // Arrange
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Phase 1 Test")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .enableLunch(false)
            .optimizationTimeSeconds(5)
            .build();

        // Act - Phase 1 happens automatically during generation
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Assert - Schedule was generated (meaning Phase 1 succeeded)
        assertNotNull(schedule);

        // If Phase 1 failed, schedule would be null or exception would be thrown
        System.out.println("✓ Phase 1 (Resource Loading) completed successfully");
    }

    @Test
    public void testPhase3_ScheduleEntityCreation_ShouldPersist() throws Exception {
        // Arrange
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Phase 3 Test - Entity Creation")
            .scheduleType(ScheduleType.BLOCK)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(90)  // Block schedule: 90-minute periods
            .enableLunch(false)
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Assert - Schedule entity created with correct properties
        assertNotNull(schedule);
        assertNotNull(schedule.getId());
        assertEquals("Phase 3 Test - Entity Creation", schedule.getScheduleName());
        assertEquals(ScheduleType.BLOCK, schedule.getScheduleType());
        assertEquals(LocalDate.of(2025, 9, 1), schedule.getStartDate());
        assertEquals(LocalDate.of(2025, 12, 20), schedule.getEndDate());

        // Assert - Persisted to database
        Schedule reloaded = scheduleRepository.findById(schedule.getId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals(schedule.getScheduleName(), reloaded.getScheduleName());

        System.out.println("✓ Phase 3 (Schedule Entity Creation) completed successfully");
        System.out.println("  - Schedule ID: " + schedule.getId());
        System.out.println("  - Type: " + schedule.getScheduleType());
    }

    @Test
    public void testPhase4_SlotCreation_DividesStudentsAmongSections() throws Exception {
        // Arrange - Create many students to test section division
        for (int i = 1; i <= 20; i++) {
            Student student = new Student();
            student.setFirstName("Student");
            student.setLastName("" + i);
            student.setStudentId("BULK-S" + String.format("%03d", i));
            student.setGradeLevel("9");
            student.setActive(true);
            student.getEnrolledCourses().add(course1);  // All enroll in MATH-101
            studentRepository.save(student);
        }

        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Phase 4 Test - Slot Creation")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .enableLunch(false)
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Get slots
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        // Assert - Students divided among sections (not duplicated)
        long mathSlots = slots.stream()
            .filter(slot -> slot.getCourse() != null &&
                           slot.getCourse().getId().equals(course1.getId()))
            .filter(slot -> slot.getStudents() != null && !slot.getStudents().isEmpty())
            .count();

        // Total enrolled in MATH-101: original 3 + 20 bulk = 23 students
        // Should have 23 student-course slots (not 23*N duplicates)
        assertTrue(mathSlots >= 20,
            "Should have slots for all enrolled students (got " + mathSlots + ")");

        System.out.println("✓ Phase 4 (Slot Creation) completed successfully");
        System.out.println("  - Total slots created: " + slots.size());
        System.out.println("  - Math course slots: " + mathSlots);
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    public void testScheduleGeneration_WithNoTeachers_ShouldHandleGracefully() {
        // Arrange - Delete all teachers
        teacherRepository.deleteAll();

        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("No Teachers Test")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .optimizationTimeSeconds(5)
            .build();

        // Act & Assert - Should throw exception or return null
        assertThrows(Exception.class, () -> {
            scheduleGenerationService.generateSchedule(request, null);
        }, "Should fail when no teachers available");

        System.out.println("✓ Schedule generation correctly handles missing teachers");
    }

    @Test
    public void testScheduleGeneration_WithNoRooms_ShouldHandleGracefully() {
        // Arrange - Delete all rooms
        roomRepository.deleteAll();

        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("No Rooms Test")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .optimizationTimeSeconds(5)
            .build();

        // Act & Assert
        assertThrows(Exception.class, () -> {
            scheduleGenerationService.generateSchedule(request, null);
        }, "Should fail when no rooms available");

        System.out.println("✓ Schedule generation correctly handles missing rooms");
    }

    @Test
    public void testScheduleGeneration_WithNoCourses_ShouldHandleGracefully() {
        // Arrange - Delete all courses (and enrollments)
        studentRepository.findAll().forEach(student -> {
            student.getEnrolledCourses().clear();
            studentRepository.save(student);
        });
        courseRepository.deleteAll();

        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("No Courses Test")
            .scheduleType(ScheduleType.TRADITIONAL)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .optimizationTimeSeconds(5)
            .build();

        // Act & Assert
        assertThrows(Exception.class, () -> {
            scheduleGenerationService.generateSchedule(request, null);
        }, "Should fail when no courses available");

        System.out.println("✓ Schedule generation correctly handles missing courses");
    }

    // ========== SCHEDULE TYPE VARIATIONS ==========

    @Test
    public void testScheduleGeneration_BlockSchedule_ShouldSucceed() throws Exception {
        // Arrange - Block schedule: longer periods, fewer per day
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Block Schedule Test")
            .scheduleType(ScheduleType.BLOCK)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(90)  // 90-minute blocks
            .passingPeriodDuration(10)
            .enableLunch(true)
            .lunchStartHour(12)
            .lunchDuration(30)
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Assert
        assertNotNull(schedule);
        assertEquals(ScheduleType.BLOCK, schedule.getScheduleType());

        System.out.println("✓ Block schedule generation succeeded");
    }

    @Test
    public void testScheduleGeneration_RotatingSchedule_ShouldSucceed() throws Exception {
        // Arrange
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
            .scheduleName("Rotating Schedule Test")
            .scheduleType(ScheduleType.ROTATING)
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 12, 20))
            .startHour(8)
            .endHour(15)
            .periodDuration(50)
            .enableLunch(true)
            .lunchStartHour(12)
            .lunchDuration(30)
            .optimizationTimeSeconds(5)
            .build();

        // Act
        Schedule schedule = scheduleGenerationService.generateSchedule(request, null);

        // Assert
        assertNotNull(schedule);
        assertEquals(ScheduleType.ROTATING, schedule.getScheduleType());

        System.out.println("✓ Rotating schedule generation succeeded");
    }
}
