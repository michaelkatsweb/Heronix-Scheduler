package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.dto.*;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for DataManagementServiceImpl
 *
 * Service: 25th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/DataManagementServiceImplTest.java
 *
 * Tests cover:
 * - Delete all (students, teachers, courses, rooms, events)
 * - Delete by filter (grade level, department, inactive status)
 * - Database statistics retrieval
 * - Error handling and validation
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class DataManagementServiceImplTest {

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @Mock(lenient = true)
    private RoomRepository roomRepository;

    @Mock(lenient = true)
    private EventRepository eventRepository;

    @Mock(lenient = true)
    private ScheduleRepository scheduleRepository;

    @Mock(lenient = true)
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private DataManagementServiceImpl service;

    private Student testStudent;
    private Teacher testTeacher;
    private Course testCourse;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Alice");
        testStudent.setLastName("Johnson");
        testStudent.setGradeLevel("9");
        testStudent.setActive(true);

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("John Smith");
        testTeacher.setDepartment("Mathematics");
        testTeacher.setActive(true);

        // Create test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");

        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);
    }

    // ========== DELETE ALL STUDENTS TESTS ==========

    @Test
    void testDeleteAllStudents_WithStudents_ShouldDelete() {
        when(studentRepository.count()).thenReturn(10L);

        CleanupResult result = service.deleteAllStudents();

        assertTrue(result.isSuccess());
        assertEquals(10, result.getDeletedCount());
        verify(studentRepository).deleteAll();
    }

    @Test
    void testDeleteAllStudents_WithNoStudents_ShouldSucceed() {
        when(studentRepository.count()).thenReturn(0L);

        CleanupResult result = service.deleteAllStudents();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteAllStudents_WithException_ShouldReturnError() {
        when(studentRepository.count()).thenReturn(5L);
        doThrow(new RuntimeException("Database error")).when(studentRepository).deleteAll();

        CleanupResult result = service.deleteAllStudents();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Database error"));
    }

    // ========== DELETE ALL TEACHERS TESTS ==========

    @Test
    void testDeleteAllTeachers_WithTeachers_ShouldDelete() {
        when(teacherRepository.countByActiveTrue()).thenReturn(5L);

        CleanupResult result = service.deleteAllTeachers();

        assertTrue(result.isSuccess());
        assertEquals(5, result.getDeletedCount());
        verify(teacherRepository).deleteAll();
    }

    @Test
    void testDeleteAllTeachers_WithNoTeachers_ShouldSucceed() {
        when(teacherRepository.countByActiveTrue()).thenReturn(0L);

        CleanupResult result = service.deleteAllTeachers();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteAllTeachers_WithException_ShouldReturnError() {
        when(teacherRepository.countByActiveTrue()).thenReturn(3L);
        doThrow(new RuntimeException("Delete failed")).when(teacherRepository).deleteAll();

        CleanupResult result = service.deleteAllTeachers();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== DELETE ALL COURSES TESTS ==========

    @Test
    void testDeleteAllCourses_WithCourses_ShouldDelete() {
        when(courseRepository.count()).thenReturn(20L);

        CleanupResult result = service.deleteAllCourses();

        assertTrue(result.isSuccess());
        assertEquals(20, result.getDeletedCount());
        verify(courseRepository).deleteAll();
    }

    @Test
    void testDeleteAllCourses_WithNoCourses_ShouldSucceed() {
        when(courseRepository.count()).thenReturn(0L);

        CleanupResult result = service.deleteAllCourses();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteAllCourses_WithException_ShouldReturnError() {
        when(courseRepository.count()).thenReturn(10L);
        doThrow(new RuntimeException("Constraint violation")).when(courseRepository).deleteAll();

        CleanupResult result = service.deleteAllCourses();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== DELETE ALL ROOMS TESTS ==========

    @Test
    void testDeleteAllRooms_WithRooms_ShouldDelete() {
        when(roomRepository.count()).thenReturn(15L);

        CleanupResult result = service.deleteAllRooms();

        assertTrue(result.isSuccess());
        assertEquals(15, result.getDeletedCount());
        verify(roomRepository).deleteAll();
    }

    @Test
    void testDeleteAllRooms_WithNoRooms_ShouldSucceed() {
        when(roomRepository.count()).thenReturn(0L);

        CleanupResult result = service.deleteAllRooms();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteAllRooms_WithException_ShouldReturnError() {
        when(roomRepository.count()).thenReturn(8L);
        doThrow(new RuntimeException("FK constraint")).when(roomRepository).deleteAll();

        CleanupResult result = service.deleteAllRooms();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== DELETE ALL EVENTS TESTS ==========

    @Test
    void testDeleteAllEvents_WithEvents_ShouldDelete() {
        when(eventRepository.count()).thenReturn(12L);

        CleanupResult result = service.deleteAllEvents();

        assertTrue(result.isSuccess());
        assertEquals(12, result.getDeletedCount());
        verify(eventRepository).deleteAll();
    }

    @Test
    void testDeleteAllEvents_WithNoEvents_ShouldSucceed() {
        when(eventRepository.count()).thenReturn(0L);

        CleanupResult result = service.deleteAllEvents();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteAllEvents_WithException_ShouldReturnError() {
        when(eventRepository.count()).thenReturn(5L);
        doThrow(new RuntimeException("Delete error")).when(eventRepository).deleteAll();

        CleanupResult result = service.deleteAllEvents();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== DELETE STUDENTS BY GRADE LEVEL TESTS ==========

    @Test
    void testDeleteStudentsByGradeLevel_WithValidGrade_ShouldDelete() {
        Student student1 = new Student();
        student1.setGradeLevel("9");
        Student student2 = new Student();
        student2.setGradeLevel("9");

        when(studentRepository.findByGradeLevel("9")).thenReturn(Arrays.asList(student1, student2));

        CleanupResult result = service.deleteStudentsByGradeLevel("9");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getDeletedCount());
        verify(studentRepository).deleteAll(anyList());
    }

    @Test
    void testDeleteStudentsByGradeLevel_WithNoMatches_ShouldReturnZero() {
        when(studentRepository.findByGradeLevel("12")).thenReturn(Collections.emptyList());

        CleanupResult result = service.deleteStudentsByGradeLevel("12");

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteStudentsByGradeLevel_WithNullGrade_ShouldReturnError() {
        CleanupResult result = service.deleteStudentsByGradeLevel(null);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("cannot be null"));
        verify(studentRepository, never()).deleteAll(anyList());
    }

    @Test
    void testDeleteStudentsByGradeLevel_WithEmptyGrade_ShouldReturnError() {
        CleanupResult result = service.deleteStudentsByGradeLevel("   ");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        verify(studentRepository, never()).deleteAll(anyList());
    }

    @Test
    void testDeleteStudentsByGradeLevel_WithException_ShouldReturnError() {
        when(studentRepository.findByGradeLevel("10")).thenReturn(Arrays.asList(testStudent));
        doThrow(new RuntimeException("Delete failed")).when(studentRepository).deleteAll(anyList());

        CleanupResult result = service.deleteStudentsByGradeLevel("10");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== DELETE TEACHERS BY DEPARTMENT TESTS ==========

    @Test
    void testDeleteTeachersByDepartment_WithValidDepartment_ShouldDelete() {
        Teacher teacher1 = new Teacher();
        teacher1.setDepartment("Mathematics");
        Teacher teacher2 = new Teacher();
        teacher2.setDepartment("Mathematics");

        when(teacherRepository.findByDepartment("Mathematics")).thenReturn(Arrays.asList(teacher1, teacher2));

        CleanupResult result = service.deleteTeachersByDepartment("Mathematics");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getDeletedCount());
        verify(teacherRepository).deleteAll(anyList());
    }

    @Test
    void testDeleteTeachersByDepartment_WithNoMatches_ShouldReturnZero() {
        when(teacherRepository.findByDepartment("Art")).thenReturn(Collections.emptyList());

        CleanupResult result = service.deleteTeachersByDepartment("Art");

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteTeachersByDepartment_WithNullDepartment_ShouldReturnError() {
        CleanupResult result = service.deleteTeachersByDepartment(null);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("cannot be null"));
        verify(teacherRepository, never()).deleteAll(anyList());
    }

    @Test
    void testDeleteTeachersByDepartment_WithEmptyDepartment_ShouldReturnError() {
        CleanupResult result = service.deleteTeachersByDepartment("   ");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        verify(teacherRepository, never()).deleteAll(anyList());
    }

    @Test
    void testDeleteTeachersByDepartment_WithException_ShouldReturnError() {
        when(teacherRepository.findByDepartment("Science")).thenReturn(Arrays.asList(testTeacher));
        doThrow(new RuntimeException("Constraint violation")).when(teacherRepository).deleteAll(anyList());

        CleanupResult result = service.deleteTeachersByDepartment("Science");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== DELETE INACTIVE STUDENTS TESTS ==========

    @Test
    void testDeleteInactiveStudents_WithInactiveStudents_ShouldDelete() {
        Student active = new Student();
        active.setActive(true);
        Student inactive1 = new Student();
        inactive1.setActive(false);
        Student inactive2 = new Student();
        inactive2.setActive(false);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(active, inactive1, inactive2));

        CleanupResult result = service.deleteInactiveStudents();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getDeletedCount());
        verify(studentRepository).deleteAll(anyList());
    }

    @Test
    void testDeleteInactiveStudents_WithNoInactiveStudents_ShouldReturnZero() {
        Student active1 = new Student();
        active1.setActive(true);
        Student active2 = new Student();
        active2.setActive(true);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(active1, active2));

        CleanupResult result = service.deleteInactiveStudents();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteInactiveStudents_WithNullStudent_ShouldNotCrash() {
        Student active = new Student();
        active.setActive(true);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(active, null));

        CleanupResult result = service.deleteInactiveStudents();

        assertTrue(result.isSuccess());
        // Should filter out null students
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteInactiveStudents_WithException_ShouldReturnError() {
        when(studentRepository.findAll()).thenThrow(new RuntimeException("Query failed"));

        CleanupResult result = service.deleteInactiveStudents();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== DELETE INACTIVE TEACHERS TESTS ==========

    @Test
    void testDeleteInactiveTeachers_WithInactiveTeachers_ShouldDelete() {
        Teacher active = new Teacher();
        active.setActive(true);
        Teacher inactive1 = new Teacher();
        inactive1.setActive(false);
        Teacher inactive2 = new Teacher();
        inactive2.setActive(false);

        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(active, inactive1, inactive2));

        CleanupResult result = service.deleteInactiveTeachers();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getDeletedCount());
        verify(teacherRepository).deleteAll(anyList());
    }

    @Test
    void testDeleteInactiveTeachers_WithNoInactiveTeachers_ShouldReturnZero() {
        Teacher active1 = new Teacher();
        active1.setActive(true);
        Teacher active2 = new Teacher();
        active2.setActive(true);

        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(active1, active2));

        CleanupResult result = service.deleteInactiveTeachers();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDeletedCount());
    }

    @Test
    void testDeleteInactiveTeachers_WithMixedActiveStatus_ShouldDeleteOnlyInactive() {
        Teacher activeTrue1 = new Teacher();
        activeTrue1.setActive(true);
        Teacher activeFalse = new Teacher();
        activeFalse.setActive(false);
        Teacher activeTrue2 = new Teacher();
        activeTrue2.setActive(true);

        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(activeTrue1, activeFalse, activeTrue2));

        CleanupResult result = service.deleteInactiveTeachers();

        assertTrue(result.isSuccess());
        // Should delete only those with active=false (1 teacher)
        assertEquals(1, result.getDeletedCount());
    }

    @Test
    void testDeleteInactiveTeachers_WithException_ShouldReturnError() {
        when(teacherRepository.findAllActive()).thenThrow(new RuntimeException("Database error"));

        CleanupResult result = service.deleteInactiveTeachers();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ========== GET DATABASE STATS TESTS ==========

    @Test
    void testGetDatabaseStats_ShouldReturnStats() {
        when(studentRepository.count()).thenReturn(100L);
        when(teacherRepository.countByActiveTrue()).thenReturn(20L);
        when(courseRepository.count()).thenReturn(50L);
        when(roomRepository.count()).thenReturn(30L);
        when(eventRepository.count()).thenReturn(10L);
        when(scheduleRepository.count()).thenReturn(5L);
        when(scheduleSlotRepository.count()).thenReturn(200L);

        DatabaseStats stats = service.getDatabaseStats();

        assertNotNull(stats);
        assertEquals(100L, stats.getStudentCount());
        assertEquals(20L, stats.getTeacherCount());
        assertEquals(50L, stats.getCourseCount());
        assertEquals(30L, stats.getRoomCount());
        assertEquals(10L, stats.getEventCount());
        assertEquals(5L, stats.getScheduleCount());
        assertEquals(200L, stats.getScheduleSlotCount());
        assertNotNull(stats.getLastUpdated());
    }

    @Test
    void testGetDatabaseStats_WithZeroCounts_ShouldReturnZeros() {
        when(studentRepository.count()).thenReturn(0L);
        when(teacherRepository.countByActiveTrue()).thenReturn(0L);
        when(courseRepository.count()).thenReturn(0L);
        when(roomRepository.count()).thenReturn(0L);
        when(eventRepository.count()).thenReturn(0L);
        when(scheduleRepository.count()).thenReturn(0L);
        when(scheduleSlotRepository.count()).thenReturn(0L);

        DatabaseStats stats = service.getDatabaseStats();

        assertNotNull(stats);
        assertEquals(0L, stats.getStudentCount());
        assertEquals(0L, stats.getTeacherCount());
        assertEquals(0L, stats.getCourseCount());
    }
}
