package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.LunchAssignmentMethod;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for LunchAssignmentServiceImpl
 * Tests student and teacher lunch assignment functionality
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LunchAssignmentServiceImplTest {

    @Mock
    private StudentLunchAssignmentRepository studentLunchAssignmentRepository;

    @Mock
    private TeacherLunchAssignmentRepository teacherLunchAssignmentRepository;

    @Mock
    private LunchWaveRepository lunchWaveRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private LunchAssignmentServiceImpl service;

    private Schedule testSchedule;
    private LunchWave testWave;
    private Student testStudent;
    private Teacher testTeacher;
    private StudentLunchAssignment testAssignment;

    @BeforeEach
    void setUp() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setScheduleName("Fall 2025");

        // Create test lunch wave
        testWave = new LunchWave();
        testWave.setId(1L);
        testWave.setSchedule(testSchedule);
        testWave.setWaveName("Lunch Wave 1");
        testWave.setWaveOrder(1);
        testWave.setMaxCapacity(100);
        testWave.setCurrentAssignments(50);
        testWave.setGradeLevelRestriction(null); // No restriction

        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("12345");
        testStudent.setFirstName("John");
        testStudent.setLastName("Smith");
        testStudent.setGradeLevel("10");

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("Jane");
        testTeacher.setLastName("Doe");
        testTeacher.setActive(true);

        // Create test assignment
        testAssignment = StudentLunchAssignment.builder()
            .id(1L)
            .student(testStudent)
            .schedule(testSchedule)
            .lunchWave(testWave)
            .assignmentMethod(LunchAssignmentMethod.MANUAL)
            .assignedAt(LocalDateTime.now())
            .assignedBy("admin")
            .manualOverride(true)
            .priority(5)
            .isLocked(false)
            .build();
    }

    // ========== ASSIGN STUDENTS TO LUNCH WAVES TESTS ==========

    @Test
    void testAssignStudentsToLunchWaves_WithBalancedMethod_ShouldCallBalancedAssignment() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(lunchWaveRepository.findActiveByScheduleId(1L)).thenReturn(Arrays.asList(testWave));
        when(studentLunchAssignmentRepository.findUnassignedStudents(1L)).thenReturn(new ArrayList<>());

        int result = service.assignStudentsToLunchWaves(1L, LunchAssignmentMethod.BALANCED);

        assertEquals(0, result); // No students to assign
        verify(scheduleRepository, atLeastOnce()).findById(1L);
    }

    @Test
    void testAssignStudentsToLunchWaves_WithManualMethod_ShouldReturnZero() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        int result = service.assignStudentsToLunchWaves(1L, LunchAssignmentMethod.MANUAL);

        assertEquals(0, result);
        verify(scheduleRepository, atLeastOnce()).findById(1L);
        verifyNoInteractions(lunchWaveRepository);
    }

    @Test
    void testAssignStudentsToLunchWaves_WithInvalidSchedule_ShouldThrow() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.assignStudentsToLunchWaves(999L, LunchAssignmentMethod.BALANCED));
    }

    // ========== ASSIGN STUDENT TO WAVE TESTS ==========

    @Test
    void testAssignStudentToWave_WithValidInput_ShouldCreateAssignment() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(lunchWaveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(studentLunchAssignmentRepository.findByStudentAndSchedule(testStudent, testSchedule))
            .thenReturn(Optional.empty());
        when(studentLunchAssignmentRepository.save(any())).thenAnswer(invocation -> {
            StudentLunchAssignment assignment = invocation.getArgument(0);
            assignment.setId(1L);
            return assignment;
        });

        StudentLunchAssignment result = service.assignStudentToWave(1L, 1L, "admin");

        assertNotNull(result);
        assertEquals(testStudent, result.getStudent());
        assertEquals(testWave, result.getLunchWave());
        assertTrue(result.getManualOverride());
        verify(studentLunchAssignmentRepository).save(any());
    }

    @Test
    void testAssignStudentToWave_WithInvalidStudent_ShouldThrow() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.assignStudentToWave(999L, 1L, "admin"));
    }

    @Test
    void testAssignStudentToWave_WithInvalidWave_ShouldThrow() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(lunchWaveRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.assignStudentToWave(1L, 999L, "admin"));
    }

    @Test
    void testAssignStudentToWave_WithNullSchedule_ShouldThrow() {
        testWave.setSchedule(null);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(lunchWaveRepository.findById(1L)).thenReturn(Optional.of(testWave));

        assertThrows(IllegalStateException.class, () ->
            service.assignStudentToWave(1L, 1L, "admin"));
    }

    @Test
    void testAssignStudentToWave_WithFullWave_ShouldThrow() {
        testWave.setCurrentAssignments(100);
        testWave.setMaxCapacity(100);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(lunchWaveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(studentLunchAssignmentRepository.findByStudentAndSchedule(testStudent, testSchedule))
            .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
            service.assignStudentToWave(1L, 1L, "admin"));
    }

    // ========== REASSIGN STUDENT TESTS ==========

    @Test
    void testReassignStudent_WithValidInput_ShouldUpdateAssignment() {
        LunchWave newWave = new LunchWave();
        newWave.setId(2L);
        newWave.setSchedule(testSchedule);
        newWave.setWaveName("Lunch Wave 2");
        newWave.setMaxCapacity(100);
        newWave.setCurrentAssignments(30);

        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(lunchWaveRepository.findById(2L)).thenReturn(Optional.of(newWave));
        when(studentLunchAssignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StudentLunchAssignment result = service.reassignStudent(1L, 2L, "admin");

        assertNotNull(result);
        assertEquals(newWave, result.getLunchWave());
        assertTrue(result.getManualOverride());
        verify(studentLunchAssignmentRepository).save(any());
    }

    @Test
    void testReassignStudent_WithInvalidAssignment_ShouldThrow() {
        when(studentLunchAssignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.reassignStudent(999L, 1L, "admin"));
    }

    @Test
    void testReassignStudent_WithNullStudent_ShouldThrow() {
        testAssignment.setStudent(null);
        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));

        assertThrows(IllegalArgumentException.class, () ->
            service.reassignStudent(1L, 2L, "admin"));
    }

    // ========== REMOVE STUDENT ASSIGNMENT TESTS ==========

    @Test
    void testRemoveStudentAssignment_WithValidAssignment_ShouldDelete() {
        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));

        service.removeStudentAssignment(1L);

        verify(studentLunchAssignmentRepository).delete(testAssignment);
    }

    @Test
    void testRemoveStudentAssignment_WithInvalidId_ShouldThrow() {
        when(studentLunchAssignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.removeStudentAssignment(999L));
    }

    @Test
    void testRemoveStudentAssignment_WithNullLunchWave_ShouldThrow() {
        testAssignment.setLunchWave(null);
        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));

        assertThrows(IllegalStateException.class, () ->
            service.removeStudentAssignment(1L));
    }

    // ========== REMOVE ALL STUDENT ASSIGNMENTS TESTS ==========

    @Test
    void testRemoveAllStudentAssignments_ShouldDeleteAll() {
        List<StudentLunchAssignment> assignments = Arrays.asList(testAssignment);
        when(studentLunchAssignmentRepository.findByScheduleId(1L)).thenReturn(assignments);
        when(lunchWaveRepository.findByScheduleIdOrderByWaveOrderAsc(1L))
            .thenReturn(Arrays.asList(testWave));

        int result = service.removeAllStudentAssignments(1L);

        assertEquals(1, result);
        verify(studentLunchAssignmentRepository).deleteAll(assignments);
    }

    @Test
    void testRemoveAllStudentAssignments_WithNoAssignments_ShouldReturnZero() {
        when(studentLunchAssignmentRepository.findByScheduleId(1L)).thenReturn(new ArrayList<>());
        when(lunchWaveRepository.findByScheduleIdOrderByWaveOrderAsc(1L))
            .thenReturn(Arrays.asList(testWave));

        int result = service.removeAllStudentAssignments(1L);

        assertEquals(0, result);
        verify(studentLunchAssignmentRepository).deleteAll(any());
    }

    // ========== GET UNASSIGNED STUDENTS TESTS ==========

    @Test
    void testGetUnassignedStudents_ShouldReturnStudents() {
        List<Student> students = Arrays.asList(testStudent);
        when(studentLunchAssignmentRepository.findUnassignedStudents(1L)).thenReturn(students);

        List<Student> result = service.getUnassignedStudents(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testStudent, result.get(0));
    }

    @Test
    void testGetUnassignedStudents_WithNoStudents_ShouldReturnEmptyList() {
        when(studentLunchAssignmentRepository.findUnassignedStudents(1L)).thenReturn(new ArrayList<>());

        List<Student> result = service.getUnassignedStudents(1L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========== COUNT UNASSIGNED STUDENTS TESTS ==========

    @Test
    void testCountUnassignedStudents_ShouldReturnCount() {
        when(studentLunchAssignmentRepository.countUnassignedStudents(1L)).thenReturn(5L);

        long result = service.countUnassignedStudents(1L);

        assertEquals(5, result);
    }

    @Test
    void testCountUnassignedStudents_WithNoStudents_ShouldReturnZero() {
        when(studentLunchAssignmentRepository.countUnassignedStudents(1L)).thenReturn(0L);

        long result = service.countUnassignedStudents(1L);

        assertEquals(0, result);
    }

    // ========== GET STUDENT ASSIGNMENT TESTS ==========

    @Test
    void testGetStudentAssignment_WithExisting_ShouldReturnAssignment() {
        when(studentLunchAssignmentRepository.findByStudentIdAndScheduleId(1L, 1L))
            .thenReturn(Optional.of(testAssignment));

        Optional<StudentLunchAssignment> result = service.getStudentAssignment(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(testAssignment, result.get());
    }

    @Test
    void testGetStudentAssignment_WithNonExistent_ShouldReturnEmpty() {
        when(studentLunchAssignmentRepository.findByStudentIdAndScheduleId(999L, 1L))
            .thenReturn(Optional.empty());

        Optional<StudentLunchAssignment> result = service.getStudentAssignment(999L, 1L);

        assertFalse(result.isPresent());
    }

    // ========== GET WAVE ROSTER TESTS ==========

    @Test
    void testGetWaveRoster_ShouldReturnAssignments() {
        List<StudentLunchAssignment> assignments = Arrays.asList(testAssignment);
        when(studentLunchAssignmentRepository.findByLunchWaveIdOrderByStudentName(1L)).thenReturn(assignments);

        List<StudentLunchAssignment> result = service.getWaveRoster(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAssignment, result.get(0));
    }

    // ========== GET ALL STUDENT ASSIGNMENTS TESTS ==========

    @Test
    void testGetAllStudentAssignments_ShouldReturnAllAssignments() {
        List<StudentLunchAssignment> assignments = Arrays.asList(testAssignment);
        when(studentLunchAssignmentRepository.findByScheduleId(1L)).thenReturn(assignments);

        List<StudentLunchAssignment> result = service.getAllStudentAssignments(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAssignment, result.get(0));
    }

    // ========== GET MANUAL ASSIGNMENTS TESTS ==========

    @Test
    void testGetManualAssignments_ShouldReturnManualAssignments() {
        List<StudentLunchAssignment> assignments = Arrays.asList(testAssignment);
        when(studentLunchAssignmentRepository.findManualAssignments(1L)).thenReturn(assignments);

        List<StudentLunchAssignment> result = service.getManualAssignments(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getManualOverride());
    }

    // ========== GET LOCKED ASSIGNMENTS TESTS ==========

    @Test
    void testGetLockedAssignments_ShouldReturnLockedAssignments() {
        testAssignment.setIsLocked(true);
        List<StudentLunchAssignment> assignments = Arrays.asList(testAssignment);
        when(studentLunchAssignmentRepository.findLockedAssignments(1L)).thenReturn(assignments);

        List<StudentLunchAssignment> result = service.getLockedAssignments(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsLocked());
    }

    // ========== LOCK/UNLOCK ASSIGNMENT TESTS ==========

    @Test
    void testLockAssignment_ShouldLockAssignment() {
        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(studentLunchAssignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.lockAssignment(1L, "admin");

        assertTrue(testAssignment.getIsLocked());
        verify(studentLunchAssignmentRepository).save(testAssignment);
    }

    @Test
    void testUnlockAssignment_ShouldUnlockAssignment() {
        testAssignment.setIsLocked(true);
        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(studentLunchAssignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.unlockAssignment(1L, "admin");

        assertFalse(testAssignment.getIsLocked());
        verify(studentLunchAssignmentRepository).save(testAssignment);
    }

    // ========== SET ASSIGNMENT PRIORITY TESTS ==========

    @Test
    void testSetAssignmentPriority_ShouldUpdatePriority() {
        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(studentLunchAssignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.setAssignmentPriority(1L, 10, "admin");

        assertEquals(10, testAssignment.getPriority());
        verify(studentLunchAssignmentRepository).save(testAssignment);
    }

    // ========== MARK AS MANUAL OVERRIDE TESTS ==========

    @Test
    void testMarkAsManualOverride_ShouldSetFlag() {
        testAssignment.setManualOverride(false);
        when(studentLunchAssignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(studentLunchAssignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markAsManualOverride(1L, "admin");

        assertTrue(testAssignment.getManualOverride());
        verify(studentLunchAssignmentRepository).save(testAssignment);
    }

    // ========== NULL SAFETY TESTS ==========

    @Test
    void testAssignStudentToWave_WithNullUsername_ShouldNotCrash() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(lunchWaveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(studentLunchAssignmentRepository.findByStudentAndSchedule(testStudent, testSchedule))
            .thenReturn(Optional.empty());
        when(studentLunchAssignmentRepository.save(any())).thenAnswer(invocation -> {
            StudentLunchAssignment assignment = invocation.getArgument(0);
            assignment.setId(1L);
            return assignment;
        });

        StudentLunchAssignment result = service.assignStudentToWave(1L, 1L, null);

        assertNotNull(result);
    }

    @Test
    void testRemoveAllStudentAssignments_WithNullSchedule_ShouldHandleGracefully() {
        when(studentLunchAssignmentRepository.findByScheduleId(null)).thenReturn(new ArrayList<>());
        when(lunchWaveRepository.findByScheduleIdOrderByWaveOrderAsc(null))
            .thenReturn(new ArrayList<>());

        int result = service.removeAllStudentAssignments(null);

        assertEquals(0, result);
    }
}
