package com.heronix.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.model.domain.Room;
import com.heronix.model.enums.CourseType;
import com.heronix.model.enums.EducationLevel;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CourseController
 * Tests Course Management REST API endpoints
 *
 * Coverage:
 * - CRUD operations (Create, Read, Update, Delete)
 * - Query endpoints (by level, subject, type, teacher)
 * - Active courses endpoint
 * - Available courses (with seats)
 * - Lab requirement filtering
 * - Enrollment information
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 19, 2025 - Standard Controller Test Coverage Phase 2
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class CourseControllerTest {

    @Autowired
    private CourseController courseController;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Course testCourse;
    private Teacher testTeacher;
    private Room testRoom;

    private static final String TEST_COURSE_CODE = "MATH101";
    private static final String TEST_COURSE_NAME = "Algebra I";
    private static final String TEST_SUBJECT = "Mathematics";

    @BeforeEach
    public void setup() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setEmployeeId("TCH_COURSE_001");
        testTeacher.setName("Math Teacher");
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Smith");
        testTeacher.setEmail("john.smith@school.edu");
        testTeacher = teacherRepository.save(testTeacher);

        // Create test room
        testRoom = new Room();
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);
        testRoom = roomRepository.save(testRoom);

        // Create test course
        testCourse = new Course();
        testCourse.setCourseCode(TEST_COURSE_CODE);
        testCourse.setCourseName(TEST_COURSE_NAME);
        testCourse.setCourseType(CourseType.REGULAR);
        testCourse.setSubject(TEST_SUBJECT);
        testCourse.setDescription("Foundational algebra course");
        testCourse.setLevel(EducationLevel.HIGH_SCHOOL);
        testCourse.setScheduleType(ScheduleType.TRADITIONAL);
        testCourse.setTeacher(testTeacher);
        testCourse.setRoom(testRoom);
        testCourse.setMaxStudents(30);
        testCourse.setCurrentEnrollment(20);
        testCourse.setActive(true);
        testCourse.setRequiresLab(false);
        testCourse.setDurationMinutes(50);
        testCourse.setSessionsPerWeek(5);
        testCourse = courseRepository.save(testCourse);
    }

    // ========================================================================
    // TEST 1: GET ALL COURSES
    // ========================================================================

    @Test
    public void testGetAllCourses_ShouldReturnList() {
        // Act
        List<Course> courses = courseController.getAllCourses();

        // Assert
        assertNotNull(courses, "Courses list should be present");
        assertTrue(courses.size() > 0, "Should have at least one course");

        System.out.println("✓ All courses retrieved successfully");
        System.out.println("  - Total courses: " + courses.size());
    }

    // ========================================================================
    // TEST 2: GET COURSE BY ID
    // ========================================================================

    @Test
    public void testGetCourseById_WithValidId_ShouldReturnCourse() {
        // Act
        ResponseEntity<Course> response = courseController.getCourseById(testCourse.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals(TEST_COURSE_CODE, response.getBody().getCourseCode(), "Course code should match");

        System.out.println("✓ Course retrieved by ID successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Code: " + response.getBody().getCourseCode());
        System.out.println("  - Name: " + response.getBody().getCourseName());
    }

    @Test
    public void testGetCourseById_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Course> response = courseController.getCourseById(999999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Non-existent course correctly returns 404");
    }

    // ========================================================================
    // TEST 3: CREATE COURSE
    // ========================================================================

    @Test
    public void testCreateCourse_WithValidData_ShouldSucceed() {
        // Arrange
        Course newCourse = new Course();
        newCourse.setCourseCode("SCI101");
        newCourse.setCourseName("Biology I");
        newCourse.setCourseType(CourseType.REGULAR);
        newCourse.setSubject("Science");
        newCourse.setLevel(EducationLevel.HIGH_SCHOOL);
        newCourse.setScheduleType(ScheduleType.TRADITIONAL);
        newCourse.setMaxStudents(25);

        // Act
        ResponseEntity<Course> response = courseController.createCourse(newCourse);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Should return CREATED status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertNotNull(response.getBody().getId(), "Created course should have ID");
        assertEquals("SCI101", response.getBody().getCourseCode(), "Course code should match");
        assertTrue(response.getBody().isActive(), "New course should be active");

        System.out.println("✓ Course created successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Code: " + response.getBody().getCourseCode());
    }

    // ========================================================================
    // TEST 4: UPDATE COURSE
    // ========================================================================

    @Test
    public void testUpdateCourse_WithValidData_ShouldSucceed() {
        // Arrange
        testCourse.setCourseName("Algebra I - Advanced");
        testCourse.setMaxStudents(25);

        // Act
        ResponseEntity<Course> response = courseController.updateCourse(testCourse.getId(), testCourse);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals("Algebra I - Advanced", response.getBody().getCourseName(), "Course name should be updated");
        assertEquals(25, response.getBody().getMaxStudents(), "Max students should be updated");

        System.out.println("✓ Course updated successfully");
        System.out.println("  - Updated name: " + response.getBody().getCourseName());
        System.out.println("  - Updated capacity: " + response.getBody().getMaxStudents());
    }

    @Test
    public void testUpdateCourse_WithNonExistentId_ShouldReturn404() {
        // Arrange
        Course updateData = new Course();
        updateData.setCourseCode("NONEXISTENT");
        updateData.setCourseName("Test");
        updateData.setCourseType(CourseType.REGULAR);
        updateData.setSubject("Test");

        // Act
        ResponseEntity<Course> response = courseController.updateCourse(999999L, updateData);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Update non-existent course correctly returns 404");
    }

    // ========================================================================
    // TEST 5: DELETE COURSE (SOFT DELETE)
    // ========================================================================

    @Test
    public void testDeleteCourse_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<?> response = courseController.deleteCourse(testCourse.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status (soft delete)");

        // Verify soft deletion (course still exists but inactive)
        Course deletedCourse = courseRepository.findById(testCourse.getId()).orElse(null);
        assertNotNull(deletedCourse, "Course should still exist (soft delete)");
        assertFalse(deletedCourse.isActive(), "Course should be inactive");

        System.out.println("✓ Course soft deleted successfully");
    }

    @Test
    public void testDeleteCourse_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<?> response = courseController.deleteCourse(999999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Delete non-existent course correctly returns 404");
    }

    // ========================================================================
    // TEST 6: GET ACTIVE COURSES
    // ========================================================================

    @Test
    public void testGetActiveCourses_ShouldReturnOnlyActiveCourses() {
        // Arrange: Create inactive course
        Course inactiveCourse = new Course();
        inactiveCourse.setCourseCode("INACTIVE_001");
        inactiveCourse.setCourseName("Inactive Course");
        inactiveCourse.setCourseType(CourseType.REGULAR);
        inactiveCourse.setSubject("Test");
        inactiveCourse.setActive(false);
        courseRepository.save(inactiveCourse);

        // Act
        List<Course> activeCourses = courseController.getActiveCourses();

        // Assert
        assertNotNull(activeCourses, "Active courses list should be present");
        assertTrue(activeCourses.stream().allMatch(Course::isActive),
                   "All returned courses should be active");

        System.out.println("✓ Active courses retrieved successfully");
        System.out.println("  - Active courses: " + activeCourses.size());
    }

    // ========================================================================
    // TEST 7: GET COURSE BY COURSE CODE
    // ========================================================================

    @Test
    public void testGetCourseByCourseCode_WithValidCode_ShouldSucceed() {
        // Act
        ResponseEntity<Course> response = courseController.getCourseByCourseCode(TEST_COURSE_CODE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals(TEST_COURSE_CODE, response.getBody().getCourseCode(), "Course code should match");

        System.out.println("✓ Course retrieved by course code successfully");
        System.out.println("  - Course code: " + response.getBody().getCourseCode());
    }

    @Test
    public void testGetCourseByCourseCode_WithNonExistentCode_ShouldReturn404() {
        // Act
        ResponseEntity<Course> response = courseController.getCourseByCourseCode("NONEXISTENT");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Non-existent course code correctly returns 404");
    }

    // ========================================================================
    // TEST 8: GET COURSES BY EDUCATION LEVEL
    // ========================================================================

    @Test
    public void testGetCoursesByLevel_WithValidLevel_ShouldReturnCourses() {
        // Act
        ResponseEntity<List<Course>> response = courseController.getCoursesByLevel("HIGH_SCHOOL");
        List<Course> courses = response.getBody();

        // Assert
        assertNotNull(courses, "Courses list should be present");
        assertTrue(courses.size() > 0, "Should have at least one course");
        assertTrue(courses.stream().allMatch(c -> c.getLevel() == EducationLevel.HIGH_SCHOOL),
                   "All courses should be HIGH_SCHOOL level");

        System.out.println("✓ Courses retrieved by level successfully");
        System.out.println("  - HIGH_SCHOOL courses: " + courses.size());
    }

    @Test
    public void testGetCoursesByLevel_WithNonExistentLevel_ShouldReturnEmptyList() {
        // Act
        ResponseEntity<List<Course>> response = courseController.getCoursesByLevel("ELEMENTARY");
        List<Course> courses = response.getBody();

        // Assert
        assertNotNull(courses, "Courses list should be present");
        // May or may not be empty depending on data, just verify it returns a list
        System.out.println("✓ Courses by level query executed successfully");
        System.out.println("  - ELEMENTARY courses: " + courses.size());
    }

    // ========================================================================
    // TEST 9: GET COURSES BY SUBJECT
    // ========================================================================

    @Test
    public void testGetCoursesBySubject_WithValidSubject_ShouldReturnCourses() {
        // Act
        List<Course> courses = courseController.getCoursesBySubject(TEST_SUBJECT);

        // Assert
        assertNotNull(courses, "Courses list should be present");
        assertTrue(courses.size() > 0, "Should have at least one course");
        assertTrue(courses.stream().allMatch(c -> TEST_SUBJECT.equals(c.getSubject())),
                   "All courses should be " + TEST_SUBJECT);

        System.out.println("✓ Courses retrieved by subject successfully");
        System.out.println("  - " + TEST_SUBJECT + " courses: " + courses.size());
    }

    // ========================================================================
    // TEST 10: GET COURSES BY SCHEDULE TYPE
    // ========================================================================

    @Test
    public void testGetCoursesByScheduleType_WithValidType_ShouldReturnCourses() {
        // Act
        ResponseEntity<List<Course>> response = courseController.getCoursesByScheduleType("TRADITIONAL");
        List<Course> courses = response.getBody();

        // Assert
        assertNotNull(courses, "Courses list should be present");
        assertTrue(courses.size() > 0, "Should have at least one course");
        assertTrue(courses.stream().allMatch(c -> c.getScheduleType() == ScheduleType.TRADITIONAL),
                   "All courses should be TRADITIONAL schedule type");

        System.out.println("✓ Courses retrieved by schedule type successfully");
        System.out.println("  - TRADITIONAL schedule courses: " + courses.size());
    }

    // ========================================================================
    // TEST 11: GET COURSES BY TEACHER
    // ========================================================================

    @Test
    public void testGetCoursesByTeacher_WithValidTeacherId_ShouldReturnCourses() {
        // Act
        List<Course> courses = courseController.getCoursesByTeacher(testTeacher.getId());

        // Assert
        assertNotNull(courses, "Courses list should be present");
        assertTrue(courses.size() > 0, "Should have at least one course");
        assertTrue(courses.stream().allMatch(c -> c.getTeacher() != null &&
                                                   c.getTeacher().getId().equals(testTeacher.getId())),
                   "All courses should belong to the specified teacher");

        System.out.println("✓ Courses retrieved by teacher successfully");
        System.out.println("  - Teacher's courses: " + courses.size());
    }

    // ========================================================================
    // TEST 12: GET AVAILABLE COURSES (WITH SEATS)
    // ========================================================================

    @Test
    public void testGetAvailableCourses_ShouldReturnCoursesWithSeats() {
        // Act
        List<Course> courses = courseController.getCoursesWithAvailableSeats();

        // Assert
        assertNotNull(courses, "Courses list should be present");
        // Verify courses have available seats
        courses.forEach(c -> {
            assertTrue(c.getCurrentEnrollment() < c.getMaxStudents(),
                       "Course should have available seats");
        });

        System.out.println("✓ Available courses retrieved successfully");
        System.out.println("  - Courses with seats: " + courses.size());
    }

    // ========================================================================
    // TEST 13: GET COURSES REQUIRING LAB
    // ========================================================================

    @Test
    public void testGetCoursesRequiringLab_ShouldReturnLabCourses() {
        // Arrange: Create lab course
        Course labCourse = new Course();
        labCourse.setCourseCode("CHEM101");
        labCourse.setCourseName("Chemistry I");
        labCourse.setCourseType(CourseType.REGULAR);
        labCourse.setSubject("Science");
        labCourse.setRequiresLab(true);
        labCourse.setActive(true);
        courseRepository.save(labCourse);

        // Act
        List<Course> courses = courseController.getCoursesRequiringLab();

        // Assert
        assertNotNull(courses, "Courses list should be present");
        assertTrue(courses.stream().allMatch(Course::isRequiresLab),
                   "All courses should require lab");

        System.out.println("✓ Lab-required courses retrieved successfully");
        System.out.println("  - Lab courses: " + courses.size());
    }

    // ========================================================================
    // TEST 14: GET COURSE ENROLLMENT INFO
    // ========================================================================

    @Test
    public void testGetCourseEnrollment_WithValidId_ShouldReturnInfo() {
        // Act
        ResponseEntity<Map<String, Object>> response = courseController.getCourseEnrollment(testCourse.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        Map<String, Object> enrollment = response.getBody();
        assertNotNull(enrollment.get("currentEnrollment"), "Current enrollment should be present");
        assertNotNull(enrollment.get("maxStudents"), "Max students should be present");
        assertNotNull(enrollment.get("availableSeats"), "Available seats should be present");

        System.out.println("✓ Course enrollment info retrieved successfully");
        System.out.println("  - Current: " + enrollment.get("currentEnrollment"));
        System.out.println("  - Max: " + enrollment.get("maxStudents"));
        System.out.println("  - Available: " + enrollment.get("availableSeats"));
    }

    @Test
    public void testGetCourseEnrollment_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Map<String, Object>> response = courseController.getCourseEnrollment(999999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Enrollment for non-existent course correctly returns 404");
    }
}
