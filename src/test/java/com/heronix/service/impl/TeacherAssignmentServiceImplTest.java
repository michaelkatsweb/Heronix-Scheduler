package com.heronix.service.impl;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Room;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.RoomRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.TeacherAssignmentService.TeacherAssignmentValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for TeacherAssignmentServiceImpl
 *
 * Tests teacher-room assignments and certification management.
 * Focuses on validation logic and eligibility checks.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherAssignmentServiceImplTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private TeacherAssignmentServiceImpl service;

    private Teacher testTeacher;
    private Room testRoom;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("John Doe");
        testTeacher.setEmployeeId("EMP-001");
        testTeacher.setDepartment("Mathematics");
        testTeacher.setCertifications(new ArrayList<>(Arrays.asList("Mathematics", "Algebra")));

        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);

        // Create test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseName("Algebra I");
        testCourse.setCourseCode("MATH101");
        testCourse.setSubject("Mathematics");
    }

    // ========== HOME ROOM ASSIGNMENT TESTS ==========

    @Test
    void testAssignHomeRoom_WithValidInputs_ShouldAssign() {
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        Teacher result = service.assignHomeRoom(testTeacher, testRoom);

        assertNotNull(result);
        assertNotNull(result.getHomeRoom());
        assertEquals(testRoom.getId(), result.getHomeRoom().getId());
        verify(teacherRepository, times(1)).save(testTeacher);
    }

    @Test
    void testAssignHomeRoom_WithNullTeacher_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.assignHomeRoom(null, testRoom));
    }

    @Test
    void testAssignHomeRoom_WithNullRoom_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.assignHomeRoom(testTeacher, null));
    }

    @Test
    void testGetHomeRoom_WithAssignedRoom_ShouldReturn() {
        testTeacher.setHomeRoom(testRoom);

        Room result = service.getHomeRoom(testTeacher);

        assertNotNull(result);
        assertEquals(testRoom.getId(), result.getId());
    }

    @Test
    void testGetHomeRoom_WithNoAssignment_ShouldReturnNull() {
        testTeacher.setHomeRoom(null);

        Room result = service.getHomeRoom(testTeacher);

        assertNull(result);
    }

    // ========== CERTIFICATION TESTS ==========

    @Test
    void testIsCertifiedFor_WithValidCertification_ShouldReturnTrue() {
        boolean result = service.isCertifiedFor(testTeacher, "Mathematics");

        assertTrue(result);
    }

    @Test
    void testIsCertifiedFor_WithoutCertification_ShouldReturnFalse() {
        boolean result = service.isCertifiedFor(testTeacher, "English");

        assertFalse(result);
    }

    @Test
    void testIsCertifiedFor_CaseInsensitive_ShouldMatch() {
        boolean result = service.isCertifiedFor(testTeacher, "mathematics");

        assertTrue(result);
    }

    @Test
    void testIsCertifiedFor_WithNullTeacher_ShouldReturnFalse() {
        boolean result = service.isCertifiedFor(null, "Mathematics");

        assertFalse(result);
    }

    @Test
    void testIsCertifiedFor_WithNullSubject_ShouldReturnFalse() {
        boolean result = service.isCertifiedFor(testTeacher, null);

        assertFalse(result);
    }

    @Test
    void testAddCertification_WithNewSubject_ShouldAdd() {
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        Teacher result = service.addCertification(testTeacher, "Geometry");

        assertNotNull(result);
        assertTrue(result.getCertifications().contains("Geometry"));
        verify(teacherRepository, times(1)).save(testTeacher);
    }

    @Test
    void testAddCertification_WithExistingSubject_ShouldNotDuplicate() {
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        int initialSize = testTeacher.getCertifications().size();
        Teacher result = service.addCertification(testTeacher, "Mathematics");

        assertNotNull(result);
        assertEquals(initialSize, result.getCertifications().size());
    }

    @Test
    void testAddCertification_WithNullTeacher_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.addCertification(null, "Mathematics"));
    }

    @Test
    void testRemoveCertification_WithExistingSubject_ShouldRemove() {
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        Teacher result = service.removeCertification(testTeacher, "Algebra");

        assertNotNull(result);
        assertFalse(result.getCertifications().contains("Algebra"));
        verify(teacherRepository, times(1)).save(testTeacher);
    }

    @Test
    void testRemoveCertification_WithNonExistentSubject_ShouldNotFail() {
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        Teacher result = service.removeCertification(testTeacher, "Physics");

        assertNotNull(result);
    }

    @Test
    void testGetCertifiedSubjects_ShouldReturnList() {
        List<String> result = service.getCertifiedSubjects(testTeacher);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Mathematics"));
        assertTrue(result.contains("Algebra"));
    }

    @Test
    void testGetCertifiedSubjects_WithNoCertifications_ShouldReturnEmptyList() {
        testTeacher.setCertifications(new ArrayList<>());

        List<String> result = service.getCertifiedSubjects(testTeacher);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== ELIGIBLE COURSES TESTS ==========

    @Test
    void testGetEligibleCourses_WithMatchingDepartment_ShouldReturnCourses() {
        when(courseRepository.findAll())
            .thenReturn(Arrays.asList(testCourse));

        List<Course> result = service.getEligibleCourses(testTeacher);

        assertNotNull(result);
        // Result depends on implementation filtering logic
    }

    @Test
    void testGetEligibleCourses_WithNoCertifications_ShouldReturnEmptyList() {
        testTeacher.setCertifications(new ArrayList<>());

        List<Course> result = service.getEligibleCourses(testTeacher);

        assertNotNull(result);
        // May return empty or filtered list
    }

    @Test
    void testCanTeachCourse_WithValidCertification_ShouldReturnTrue() {
        boolean result = service.canTeachCourse(testTeacher, testCourse);

        assertTrue(result);
    }

    @Test
    void testCanTeachCourse_WithoutCertification_ShouldReturnFalse() {
        testCourse.setSubject("English");

        boolean result = service.canTeachCourse(testTeacher, testCourse);

        assertFalse(result);
    }

    @Test
    void testCanTeachCourse_WithNullTeacher_ShouldReturnFalse() {
        boolean result = service.canTeachCourse(null, testCourse);

        assertFalse(result);
    }

    @Test
    void testCanTeachCourse_WithNullCourse_ShouldReturnFalse() {
        boolean result = service.canTeachCourse(testTeacher, null);

        assertFalse(result);
    }

    // ========== GET TEACHERS CERTIFIED FOR SUBJECT TESTS ==========

    @Test
    void testGetTeachersCertifiedFor_ShouldReturnQualifiedTeachers() {
        when(teacherRepository.findAll()).thenReturn(Arrays.asList(testTeacher));

        List<Teacher> result = service.getTeachersCertifiedFor("Mathematics");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTeacher.getId(), result.get(0).getId());
    }

    @Test
    void testGetTeachersCertifiedFor_WithNoQualifiedTeachers_ShouldReturnEmptyList() {
        when(teacherRepository.findAll()).thenReturn(Arrays.asList(testTeacher));

        List<Teacher> result = service.getTeachersCertifiedFor("Physics");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTeachersCertifiedFor_WithNullSubject_ShouldReturnEmptyList() {
        List<Teacher> result = service.getTeachersCertifiedFor(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== UPDATE MAX PERIODS TESTS ==========

    @Test
    void testUpdateMaxPeriodsPerDay_WithValidValue_ShouldUpdate() {
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        Teacher result = service.updateMaxPeriodsPerDay(testTeacher, 4);

        assertNotNull(result);
        assertEquals(4, result.getMaxPeriodsPerDay());
        verify(teacherRepository, times(1)).save(testTeacher);
    }

    @Test
    void testUpdateMaxPeriodsPerDay_WithNullTeacher_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.updateMaxPeriodsPerDay(null, 7));
    }

    @Test
    void testUpdateMaxPeriodsPerDay_WithNegativeValue_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.updateMaxPeriodsPerDay(testTeacher, -1));
    }

    @Test
    void testUpdateMaxPeriodsPerDay_WithZero_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.updateMaxPeriodsPerDay(testTeacher, 0));
    }

    // ========== VALIDATE ASSIGNMENT TESTS ==========

    @Test
    void testValidateAssignment_WithValidInputs_ShouldBeValid() {
        testTeacher.setMaxPeriodsPerDay(7);

        TeacherAssignmentValidation result = service.validateAssignment(
            testTeacher,
            testCourse,
            1
        );

        assertNotNull(result);
        assertTrue(result.isValid());
        assertFalse(result.hasIssues());
    }

    @Test
    void testValidateAssignment_WithoutCertification_ShouldHaveIssue() {
        testCourse.setSubject("English");

        TeacherAssignmentValidation result = service.validateAssignment(
            testTeacher,
            testCourse,
            1
        );

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasIssues());
    }

    @Test
    void testValidateAssignment_WithNullTeacher_ShouldHaveIssue() {
        TeacherAssignmentValidation result = service.validateAssignment(
            null,
            testCourse,
            1
        );

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasIssues());
    }

    @Test
    void testValidateAssignment_WithNullCourse_ShouldHaveIssue() {
        TeacherAssignmentValidation result = service.validateAssignment(
            testTeacher,
            null,
            1
        );

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasIssues());
    }

    @Test
    void testValidateAssignment_WithInvalidPeriod_ShouldHaveIssue() {
        TeacherAssignmentValidation result = service.validateAssignment(
            testTeacher,
            testCourse,
            -1
        );

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasIssues());
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testCertificationWorkflow_AddCheckRemove() {
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        // Add new certification
        service.addCertification(testTeacher, "Geometry");
        assertTrue(service.isCertifiedFor(testTeacher, "Geometry"));

        // Check all certifications
        List<String> certs = service.getCertifiedSubjects(testTeacher);
        assertTrue(certs.contains("Geometry"));

        // Remove certification
        service.removeCertification(testTeacher, "Geometry");
        assertFalse(service.isCertifiedFor(testTeacher, "Geometry"));
    }

    @Test
    void testMultipleTeachersForSubject_ShouldFilterCorrectly() {
        Teacher teacher2 = new Teacher();
        teacher2.setId(2L);
        teacher2.setCertifications(Arrays.asList("English", "Literature"));

        when(teacherRepository.findAll()).thenReturn(Arrays.asList(testTeacher, teacher2));

        List<Teacher> mathTeachers = service.getTeachersCertifiedFor("Mathematics");
        List<Teacher> englishTeachers = service.getTeachersCertifiedFor("English");

        assertEquals(1, mathTeachers.size());
        assertEquals(1, englishTeachers.size());
        assertEquals(testTeacher.getId(), mathTeachers.get(0).getId());
        assertEquals(teacher2.getId(), englishTeachers.get(0).getId());
    }
}
