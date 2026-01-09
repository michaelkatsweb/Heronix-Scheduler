package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.enums.DayType;
import com.heronix.model.enums.ScheduleStatus;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.*;
import com.heronix.service.impl.BlockScheduleServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for BlockScheduleService
 * Tests block schedule generation, ODD/EVEN day assignment, and day type utilities
 *
 * @author Heronix Scheduling System Team
 * @since Block Scheduling MVP - November 26, 2025
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "logging.level.com.eduscheduler=DEBUG"
})
class BlockScheduleServiceTest {

    @Autowired
    private BlockScheduleService blockScheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    // Test data
    private Student student1;
    private Student student2;
    private Course mathCourse;
    private Course scienceCourse;
    private Course englishCourse;
    private Course historyCourse;
    private Teacher mathTeacher;
    private Teacher scienceTeacher;
    private Room room101;
    private Room room102;

    @BeforeEach
    @Transactional
    void setupTestData() {
        System.out.println("\n========== SETTING UP BLOCK SCHEDULE TEST DATA ==========\n");

        // Clean up any existing test data - order matters due to foreign keys!
        scheduleSlotRepository.deleteAll();
        scheduleRepository.deleteAll();
        waitlistRepository.deleteAll();  // Delete waitlists before students
        courseSectionRepository.deleteAll();  // Delete course sections before courses
        studentRepository.deleteAll();
        courseRepository.deleteAll();
        teacherRepository.deleteAll();
        roomRepository.deleteAll();

        // Create teachers
        mathTeacher = createTeacher("Math", "Teacher", "Mathematics");
        scienceTeacher = createTeacher("Science", "Teacher", "Science");

        // Create rooms
        room101 = createRoom("101", 30);
        room102 = createRoom("102", 25);

        // Create courses
        mathCourse = createCourse("Algebra I", "MATH101", "Mathematics", mathTeacher, 9);
        scienceCourse = createCourse("Biology", "SCI201", "Science", scienceTeacher, 9);
        englishCourse = createCourse("English I", "ENG101", "English", mathTeacher, 9);
        historyCourse = createCourse("World History", "HIST101", "History", scienceTeacher, 9);

        // Create students
        student1 = createStudent("John", "Doe", "S001", 9);
        student2 = createStudent("Jane", "Smith", "S002", 9);

        // Enroll students in courses
        student1.getEnrolledCourses().add(mathCourse);
        student1.getEnrolledCourses().add(scienceCourse);
        student1 = studentRepository.save(student1);

        student2.getEnrolledCourses().add(englishCourse);
        student2.getEnrolledCourses().add(historyCourse);
        student2 = studentRepository.save(student2);

        System.out.println("Test data setup complete!");
    }

    @AfterEach
    @Transactional
    void cleanup() {
        System.out.println("\n========== CLEANING UP BLOCK SCHEDULE TEST DATA ==========\n");

        // Clean up in proper order to avoid foreign key constraints
        scheduleSlotRepository.deleteAll();
        scheduleRepository.deleteAll();
        studentRepository.deleteAll();
        courseRepository.deleteAll();
        teacherRepository.deleteAll();
        roomRepository.deleteAll();
    }

    // ========================================================================
    // TEST: isOddDay() and isEvenDay()
    // ========================================================================

    @Test
    @DisplayName("Test 1: isOddDay() calculates ODD days correctly")
    void testIsOddDay() {
        // September 1 is day 0, so September 2 is day 1 (ODD)
        LocalDate sep2 = LocalDate.of(2024, 9, 2);
        assertThat(blockScheduleService.isOddDay(sep2))
            .as("September 2, 2024 should be an ODD day")
            .isTrue();

        // September 1 is day 0 (EVEN)
        LocalDate sep1 = LocalDate.of(2024, 9, 1);
        assertThat(blockScheduleService.isOddDay(sep1))
            .as("September 1, 2024 should be an EVEN day")
            .isFalse();

        // September 3 is day 2 (EVEN)
        LocalDate sep3 = LocalDate.of(2024, 9, 3);
        assertThat(blockScheduleService.isOddDay(sep3))
            .as("September 3, 2024 should be an EVEN day")
            .isFalse();
    }

    @Test
    @DisplayName("Test 2: isEvenDay() calculates EVEN days correctly")
    void testIsEvenDay() {
        // September 1 is day 0 (EVEN)
        LocalDate sep1 = LocalDate.of(2024, 9, 1);
        assertThat(blockScheduleService.isEvenDay(sep1))
            .as("September 1, 2024 should be an EVEN day")
            .isTrue();

        // September 2 is day 1 (ODD)
        LocalDate sep2 = LocalDate.of(2024, 9, 2);
        assertThat(blockScheduleService.isEvenDay(sep2))
            .as("September 2, 2024 should not be an EVEN day")
            .isFalse();
    }

    // ========================================================================
    // TEST: getDayType()
    // ========================================================================

    @Test
    @DisplayName("Test 3: getDayType() returns correct DayType")
    void testGetDayType() {
        LocalDate oddDay = LocalDate.of(2024, 9, 2);
        assertThat(blockScheduleService.getDayType(oddDay))
            .as("Day type for September 2 should be ODD")
            .isEqualTo(DayType.ODD);

        LocalDate evenDay = LocalDate.of(2024, 9, 1);
        assertThat(blockScheduleService.getDayType(evenDay))
            .as("Day type for September 1 should be EVEN")
            .isEqualTo(DayType.EVEN);
    }

    // ========================================================================
    // TEST: assignCoursesToDays()
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Test 4: assignCoursesToDays() creates slots with correct DayType")
    void testAssignCoursesToDays() {
        // Prepare course lists
        List<Course> oddDayCourses = new ArrayList<>();
        oddDayCourses.add(mathCourse);

        List<Course> evenDayCourses = new ArrayList<>();
        evenDayCourses.add(scienceCourse);

        // Assign courses to ODD/EVEN days
        blockScheduleService.assignCoursesToDays(student1, oddDayCourses, evenDayCourses);

        // Query slots from repository (don't rely on lazy-loaded collection)
        List<ScheduleSlot> studentSlots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getStudents() != null && slot.getStudents().contains(student1))
            .collect(java.util.stream.Collectors.toList());

        // Verify schedule slots were created
        assertThat(studentSlots)
            .as("Student should have schedule slots")
            .isNotNull()
            .isNotEmpty();

        // Count ODD and EVEN day slots
        long oddDaySlots = studentSlots.stream()
            .filter(slot -> slot.getDayType() == DayType.ODD)
            .count();

        long evenDaySlots = studentSlots.stream()
            .filter(slot -> slot.getDayType() == DayType.EVEN)
            .count();

        assertThat(oddDaySlots)
            .as("Should have 1 ODD day slot")
            .isEqualTo(1);

        assertThat(evenDaySlots)
            .as("Should have 1 EVEN day slot")
            .isEqualTo(1);

        // Verify courses are correctly assigned
        ScheduleSlot oddSlot = studentSlots.stream()
            .filter(slot -> slot.getDayType() == DayType.ODD)
            .findFirst()
            .orElseThrow();

        assertThat(oddSlot.getCourse())
            .as("ODD day slot should have math course")
            .isEqualTo(mathCourse);

        ScheduleSlot evenSlot = studentSlots.stream()
            .filter(slot -> slot.getDayType() == DayType.EVEN)
            .findFirst()
            .orElseThrow();

        assertThat(evenSlot.getCourse())
            .as("EVEN day slot should have science course")
            .isEqualTo(scienceCourse);
    }

    @Test
    @Transactional
    @DisplayName("Test 5: assignCoursesToDays() clears existing slots before assigning")
    void testAssignCoursesToDaysClearsExistingSlots() {
        // First assignment
        List<Course> firstOddCourses = new ArrayList<>();
        firstOddCourses.add(mathCourse);

        blockScheduleService.assignCoursesToDays(student2, firstOddCourses, new ArrayList<>());

        // Query slots from repository
        List<ScheduleSlot> firstAssignmentSlots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getStudents() != null && slot.getStudents().contains(student2))
            .collect(java.util.stream.Collectors.toList());

        assertThat(firstAssignmentSlots.size())
            .as("Should have slots from first assignment")
            .isGreaterThan(0);

        // Second assignment with different courses
        List<Course> secondEvenCourses = new ArrayList<>();
        secondEvenCourses.add(englishCourse);

        blockScheduleService.assignCoursesToDays(student2, new ArrayList<>(), secondEvenCourses);

        // Query slots again after reassignment
        List<ScheduleSlot> secondAssignmentSlots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getStudents() != null && slot.getStudents().contains(student2))
            .collect(java.util.stream.Collectors.toList());

        // Should have only EVEN day slots now
        long oddSlots = secondAssignmentSlots.stream()
            .filter(slot -> slot.getDayType() == DayType.ODD)
            .count();

        long evenSlots = secondAssignmentSlots.stream()
            .filter(slot -> slot.getDayType() == DayType.EVEN)
            .count();

        assertThat(oddSlots)
            .as("Should have no ODD day slots after reassignment")
            .isEqualTo(0);

        assertThat(evenSlots)
            .as("Should have EVEN day slots")
            .isGreaterThan(0);
    }

    // ========================================================================
    // TEST: generateBlockSchedule()
    // ========================================================================

    @Test
    @DisplayName("Test 6: generateBlockSchedule() creates Schedule entity with correct metadata")
    void testGenerateBlockScheduleCreatesScheduleEntity() {
        // Prepare data
        List<Student> students = List.of(student1, student2);
        List<Course> courses = List.of(mathCourse, scienceCourse, englishCourse, historyCourse);
        List<Teacher> teachers = List.of(mathTeacher, scienceTeacher);
        List<Room> rooms = List.of(room101, room102);

        // Generate block schedule
        Schedule schedule = blockScheduleService.generateBlockSchedule(students, courses, teachers, rooms);

        // Verify schedule was created
        assertThat(schedule)
            .as("Schedule should not be null")
            .isNotNull();

        assertThat(schedule.getId())
            .as("Schedule should have ID (saved to database)")
            .isNotNull();

        assertThat(schedule.getScheduleType())
            .as("Schedule type should be BLOCK")
            .isEqualTo(ScheduleType.BLOCK);

        assertThat(schedule.getStatus())
            .as("Schedule status should be DRAFT")
            .isEqualTo(ScheduleStatus.DRAFT);

        assertThat(schedule.getStartDate())
            .as("Schedule should have start date")
            .isNotNull();

        assertThat(schedule.getEndDate())
            .as("Schedule should have end date")
            .isNotNull();
    }

    @Test
    @DisplayName("Test 7: generateBlockSchedule() creates schedule slots for all active courses")
    void testGenerateBlockScheduleCreatesSlots() {
        // Prepare data
        List<Student> students = List.of(student1, student2);
        List<Course> courses = List.of(mathCourse, scienceCourse);
        List<Teacher> teachers = List.of(mathTeacher, scienceTeacher);
        List<Room> rooms = List.of(room101, room102);

        // Generate block schedule
        Schedule schedule = blockScheduleService.generateBlockSchedule(students, courses, teachers, rooms);

        // Verify schedule slots were created
        assertThat(schedule.getSlots())
            .as("Schedule should have slots")
            .isNotNull()
            .isNotEmpty();

        // Block schedule creates 3 periods per week per course
        int expectedSlots = courses.size() * 3; // 2 courses * 3 periods = 6 slots
        assertThat(schedule.getSlots().size())
            .as("Should have 3 periods per course")
            .isEqualTo(expectedSlots);
    }

    @Test
    @DisplayName("Test 8: generateBlockSchedule() alternates courses between ODD and EVEN days")
    void testGenerateBlockScheduleAlternatesDays() {
        // Prepare data with 4 courses
        List<Student> students = List.of(student1, student2);
        List<Course> courses = List.of(mathCourse, scienceCourse, englishCourse, historyCourse);
        List<Teacher> teachers = List.of(mathTeacher, scienceTeacher);
        List<Room> rooms = List.of(room101, room102);

        // Generate block schedule
        Schedule schedule = blockScheduleService.generateBlockSchedule(students, courses, teachers, rooms);

        // Get slots and group by day type
        long oddDaySlots = schedule.getSlots().stream()
            .filter(slot -> slot.getDayType() == DayType.ODD)
            .count();

        long evenDaySlots = schedule.getSlots().stream()
            .filter(slot -> slot.getDayType() == DayType.EVEN)
            .count();

        // With 4 courses alternating, should have 2 ODD and 2 EVEN courses (6 slots each)
        assertThat(oddDaySlots)
            .as("Should have ODD day slots (2 courses * 3 periods)")
            .isEqualTo(6);

        assertThat(evenDaySlots)
            .as("Should have EVEN day slots (2 courses * 3 periods)")
            .isEqualTo(6);
    }

    @Test
    @Transactional
    @DisplayName("Test 9: generateBlockSchedule() assigns enrolled students to slots")
    void testGenerateBlockScheduleAssignsStudents() {
        // Prepare data
        List<Student> students = List.of(student1);
        List<Course> courses = List.of(mathCourse); // student1 is enrolled in mathCourse
        List<Teacher> teachers = List.of(mathTeacher);
        List<Room> rooms = List.of(room101);

        // Generate block schedule
        Schedule schedule = blockScheduleService.generateBlockSchedule(students, courses, teachers, rooms);

        // Verify slots have enrolled students
        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getCourse().equals(mathCourse)) {
                assertThat(slot.getStudents())
                    .as("Slot should have student1 enrolled")
                    .contains(student1);
            }
        }
    }

    @Test
    @DisplayName("Test 10: generateBlockSchedule() creates 90-minute block periods")
    void testGenerateBlockScheduleBlockPeriods() {
        // Prepare data
        List<Student> students = List.of(student1);
        List<Course> courses = List.of(mathCourse);
        List<Teacher> teachers = List.of(mathTeacher);
        List<Room> rooms = List.of(room101);

        // Generate block schedule
        Schedule schedule = blockScheduleService.generateBlockSchedule(students, courses, teachers, rooms);

        // Verify block period duration (90 minutes)
        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getStartTime() != null && slot.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(
                    slot.getStartTime(),
                    slot.getEndTime()
                ).toMinutes();

                assertThat(durationMinutes)
                    .as("Block period should be 90 minutes")
                    .isEqualTo(90);
            }
        }
    }

    // ========================================================================
    // TEST: getCoursesForDayType()
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Test 11: getCoursesForDayType() returns correct courses for ODD day")
    void testGetCoursesForOddDay() {
        // Assign courses to student
        List<Course> oddCourses = List.of(mathCourse);
        List<Course> evenCourses = List.of(scienceCourse);
        blockScheduleService.assignCoursesToDays(student1, oddCourses, evenCourses);

        // Refresh student
        student1 = studentRepository.findById(student1.getId()).orElseThrow();

        // Get ODD day courses
        List<Course> retrievedOddCourses = blockScheduleService.getCoursesForDayType(student1, DayType.ODD);

        assertThat(retrievedOddCourses)
            .as("Should return ODD day courses")
            .hasSize(1)
            .contains(mathCourse);
    }

    @Test
    @Transactional
    @DisplayName("Test 12: getCoursesForDayType() returns correct courses for EVEN day")
    void testGetCoursesForEvenDay() {
        // Assign courses to student
        List<Course> oddCourses = List.of(mathCourse);
        List<Course> evenCourses = List.of(scienceCourse);
        blockScheduleService.assignCoursesToDays(student1, oddCourses, evenCourses);

        // Refresh student
        student1 = studentRepository.findById(student1.getId()).orElseThrow();

        // Get EVEN day courses
        List<Course> retrievedEvenCourses = blockScheduleService.getCoursesForDayType(student1, DayType.EVEN);

        assertThat(retrievedEvenCourses)
            .as("Should return EVEN day courses")
            .hasSize(1)
            .contains(scienceCourse);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Teacher createTeacher(String firstName, String lastName, String department) {
        Teacher teacher = new Teacher();
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setName(firstName + " " + lastName);
        teacher.setDepartment(department);
        teacher.setActive(true);
        return teacherRepository.save(teacher);
    }

    private Room createRoom(String roomNumber, int capacity) {
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setCapacity(capacity);
        return roomRepository.save(room);
    }

    private Course createCourse(String name, String code, String subject, Teacher teacher, int level) {
        Course course = new Course();
        course.setCourseName(name);
        course.setCourseCode(code);
        course.setSubject(subject);
        course.setTeacher(teacher);
        course.setLevel(com.eduscheduler.model.enums.EducationLevel.HIGH_SCHOOL);
        course.setActive(true);
        return courseRepository.save(course);
    }

    private Student createStudent(String firstName, String lastName, String studentId, int gradeLevel) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setStudentId(studentId);
        student.setGradeLevel(String.valueOf(gradeLevel));
        student.setEnrolledCourses(new ArrayList<>());
        student.setScheduleSlots(new ArrayList<>());
        return studentRepository.save(student);
    }
}
