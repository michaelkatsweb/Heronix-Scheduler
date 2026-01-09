package com.heronix.controller;

import com.heronix.controller.LunchAssignmentController.*;
import com.heronix.model.domain.*;
import com.heronix.model.enums.LunchAssignmentMethod;
import com.heronix.model.enums.SchedulePeriod;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.*;
import com.heronix.service.LunchAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LunchAssignmentController
 * Tests lunch assignment REST API endpoints
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class LunchAssignmentControllerTest {

    @Autowired
    private LunchAssignmentController lunchAssignmentController;

    @Autowired
    private LunchAssignmentService lunchAssignmentService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private LunchWaveRepository lunchWaveRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private Schedule testSchedule;
    private LunchWave testWave1;
    private LunchWave testWave2;
    private Student testStudent;
    private Teacher testTeacher;

    @BeforeEach
    public void setup() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setScheduleName("Test Schedule - Lunch Assignments");
        testSchedule.setStartDate(LocalDate.now());
        testSchedule.setEndDate(LocalDate.now().plusMonths(6));
        testSchedule.setPeriod(SchedulePeriod.SEMESTER);
        testSchedule.setScheduleType(ScheduleType.TRADITIONAL);
        testSchedule.setActive(true);
        testSchedule = scheduleRepository.save(testSchedule);

        // Create first test lunch wave
        testWave1 = new LunchWave();
        testWave1.setSchedule(testSchedule);
        testWave1.setWaveName("Lunch Wave 1");
        testWave1.setWaveOrder(1);
        testWave1.setStartTime(LocalTime.of(10, 4));
        testWave1.setEndTime(LocalTime.of(10, 34));
        testWave1.setMaxCapacity(250);
        testWave1.setCurrentAssignments(0);
        testWave1.setIsActive(true);
        testWave1 = lunchWaveRepository.save(testWave1);

        // Create second test lunch wave
        testWave2 = new LunchWave();
        testWave2.setSchedule(testSchedule);
        testWave2.setWaveName("Lunch Wave 2");
        testWave2.setWaveOrder(2);
        testWave2.setStartTime(LocalTime.of(10, 58));
        testWave2.setEndTime(LocalTime.of(11, 28));
        testWave2.setMaxCapacity(250);
        testWave2.setCurrentAssignments(0);
        testWave2.setIsActive(true);
        testWave2 = lunchWaveRepository.save(testWave2);

        // Create test student
        testStudent = new Student();
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setStudentId("S001");
        testStudent.setGradeLevel("9");
        testStudent = studentRepository.save(testStudent);

        // Create test teacher (use unique ID to avoid collision with TeacherDataInitializer)
        testTeacher = new Teacher();
        testTeacher.setName("Jane Smith Test");
        testTeacher.setFirstName("Jane");
        testTeacher.setLastName("Smith");
        testTeacher.setEmployeeId("LUNCH-TEST-001");
        testTeacher = teacherRepository.save(testTeacher);
    }

    // ========== ASSIGNMENT OPERATIONS ==========

    @Test
    public void testAssignStudents_WithAlphabeticalMethod_ShouldSucceed() {
        // Arrange
        AssignStudentsRequest request = new AssignStudentsRequest();
        request.setScheduleId(testSchedule.getId());
        request.setMethod(LunchAssignmentMethod.ALPHABETICAL);

        // Act
        ResponseEntity<Map<String, Object>> response =
            lunchAssignmentController.assignStudents(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("assignedCount"));
        assertEquals("Alphabetical", response.getBody().get("method").toString());

        System.out.println("✓ Students assigned using ALPHABETICAL method");
        System.out.println("  - Assigned count: " + response.getBody().get("assignedCount"));
    }

    @Test
    public void testAssignStudents_WithBalancedMethod_ShouldSucceed() {
        // Arrange
        AssignStudentsRequest request = new AssignStudentsRequest();
        request.setScheduleId(testSchedule.getId());
        request.setMethod(LunchAssignmentMethod.BALANCED);

        // Act
        ResponseEntity<Map<String, Object>> response =
            lunchAssignmentController.assignStudents(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Balanced", response.getBody().get("method").toString());

        System.out.println("✓ Students assigned using BALANCED method");
    }

    @Test
    public void testAssignTeachers_ShouldSucceed() {
        // Arrange
        Map<String, Long> request = Map.of("scheduleId", testSchedule.getId());

        // Act
        ResponseEntity<Map<String, Object>> response =
            lunchAssignmentController.assignTeachers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("assignedCount"));

        System.out.println("✓ Teachers assigned successfully");
        System.out.println("  - Assigned count: " + response.getBody().get("assignedCount"));
    }

    @Test
    public void testRebalanceAssignments_ShouldSucceed() {
        // Arrange - First assign some students
        AssignStudentsRequest assignRequest = new AssignStudentsRequest();
        assignRequest.setScheduleId(testSchedule.getId());
        assignRequest.setMethod(LunchAssignmentMethod.BALANCED);
        lunchAssignmentController.assignStudents(assignRequest);

        Map<String, Long> request = Map.of("scheduleId", testSchedule.getId());

        // Act
        ResponseEntity<Map<String, Object>> response =
            lunchAssignmentController.rebalanceAssignments(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("reassignedCount"));

        System.out.println("✓ Assignments rebalanced successfully");
        System.out.println("  - Reassigned count: " + response.getBody().get("reassignedCount"));
    }

    // ========== STUDENT ASSIGNMENT OPERATIONS ==========

    @Test
    public void testGetStudentAssignment_WithValidStudent_ShouldReturnAssignment() {
        // Arrange - Create assignment
        lunchAssignmentService.assignStudentToWave(testStudent.getId(), testWave1.getId(), "test");

        // Act
        ResponseEntity<StudentLunchAssignment> response =
            lunchAssignmentController.getStudentAssignment(testStudent.getId(), testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testStudent.getId(), response.getBody().getStudent().getId());

        System.out.println("✓ Student assignment retrieved successfully");
        System.out.println("  - Student: " + response.getBody().getStudent().getFirstName());
        System.out.println("  - Wave: " + response.getBody().getLunchWave().getWaveName());
    }

    @Test
    public void testGetStudentAssignment_WithNonExistent_ShouldReturn404() {
        // Act
        ResponseEntity<StudentLunchAssignment> response =
            lunchAssignmentController.getStudentAssignment(99999L, testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Non-existent student assignment correctly returns 404");
    }

    @Test
    public void testReassignStudent_WithValidData_ShouldSucceed() {
        // Arrange - Create initial assignment
        lunchAssignmentService.assignStudentToWave(testStudent.getId(), testWave1.getId(), "test");

        ReassignStudentRequest request = new ReassignStudentRequest();
        request.setScheduleId(testSchedule.getId());
        request.setLunchWaveId(testWave2.getId());
        request.setUsername("admin");

        // Act
        ResponseEntity<StudentLunchAssignment> response =
            lunchAssignmentController.reassignStudent(testStudent.getId(), request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testWave2.getId(), response.getBody().getLunchWave().getId());

        System.out.println("✓ Student reassigned successfully");
        System.out.println("  - New wave: " + response.getBody().getLunchWave().getWaveName());
    }

    @Test
    public void testRemoveStudentAssignment_WithValidStudent_ShouldSucceed() {
        // Arrange - Create assignment
        lunchAssignmentService.assignStudentToWave(testStudent.getId(), testWave1.getId(), "test");

        // Act
        ResponseEntity<Void> response =
            lunchAssignmentController.removeStudentAssignment(testStudent.getId(), testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        System.out.println("✓ Student assignment removed successfully");
    }

    // ========== TEACHER ASSIGNMENT OPERATIONS ==========

    @Test
    public void testGetTeacherAssignment_WithValidTeacher_ShouldReturnAssignment() {
        // Arrange - Create assignment
        lunchAssignmentService.assignTeacherToWave(testTeacher.getId(), testWave1.getId(), "test");

        // Act
        ResponseEntity<TeacherLunchAssignment> response =
            lunchAssignmentController.getTeacherAssignment(testTeacher.getId(), testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testTeacher.getId(), response.getBody().getTeacher().getId());

        System.out.println("✓ Teacher assignment retrieved successfully");
        System.out.println("  - Teacher: " + response.getBody().getTeacher().getFirstName());
        System.out.println("  - Wave: " + response.getBody().getLunchWave().getWaveName());
    }

    @Test
    public void testGetTeacherAssignment_WithNonExistent_ShouldReturn404() {
        // Act
        ResponseEntity<TeacherLunchAssignment> response =
            lunchAssignmentController.getTeacherAssignment(99999L, testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Non-existent teacher assignment correctly returns 404");
    }

    @Test
    public void testReassignTeacher_WithValidData_ShouldSucceed() {
        // Arrange - Create initial assignment
        lunchAssignmentService.assignTeacherToWave(testTeacher.getId(), testWave1.getId(), "test");

        ReassignTeacherRequest request = new ReassignTeacherRequest();
        request.setScheduleId(testSchedule.getId());
        request.setLunchWaveId(testWave2.getId());
        request.setUsername("admin");

        // Act
        ResponseEntity<TeacherLunchAssignment> response =
            lunchAssignmentController.reassignTeacher(testTeacher.getId(), request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testWave2.getId(), response.getBody().getLunchWave().getId());

        System.out.println("✓ Teacher reassigned successfully");
        System.out.println("  - New wave: " + response.getBody().getLunchWave().getWaveName());
    }

    @Test
    public void testAssignSupervisionDuty_WithValidData_ShouldSucceed() {
        // Arrange - Create initial assignment
        lunchAssignmentService.assignTeacherToWave(testTeacher.getId(), testWave1.getId(), "test");

        SupervisionDutyRequest request = new SupervisionDutyRequest();
        request.setScheduleId(testSchedule.getId());
        request.setLocation("Main Cafeteria");
        request.setUsername("admin");

        // Act
        ResponseEntity<TeacherLunchAssignment> response =
            lunchAssignmentController.assignSupervisionDuty(testTeacher.getId(), request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Main Cafeteria", response.getBody().getSupervisionLocation());

        System.out.println("✓ Supervision duty assigned successfully");
        System.out.println("  - Location: " + response.getBody().getSupervisionLocation());
    }

    @Test
    public void testRemoveSupervisionDuty_WithValidTeacher_ShouldSucceed() {
        // Arrange - Create assignment with supervision
        lunchAssignmentService.assignTeacherToWave(testTeacher.getId(), testWave1.getId(), "test");
        var assignment = lunchAssignmentService.getTeacherAssignment(testTeacher.getId(), testSchedule.getId())
            .orElseThrow();
        lunchAssignmentService.assignSupervisionDuty(assignment.getId(), "Main Cafeteria", "test");

        // Act
        ResponseEntity<TeacherLunchAssignment> response =
            lunchAssignmentController.removeSupervisionDuty(
                testTeacher.getId(),
                testSchedule.getId(),
                "admin"
            );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getSupervisionLocation());

        System.out.println("✓ Supervision duty removed successfully");
    }

    // ========== ROSTER OPERATIONS ==========

    @Test
    public void testGetStudentRoster_WithValidWave_ShouldReturnRoster() {
        // Arrange - Create assignment
        lunchAssignmentService.assignStudentToWave(testStudent.getId(), testWave1.getId(), "test");

        // Act
        ResponseEntity<List<StudentLunchAssignment>> response =
            lunchAssignmentController.getStudentRoster(testWave1.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 1);

        System.out.println("✓ Student roster retrieved successfully");
        System.out.println("  - Roster size: " + response.getBody().size());
    }

    @Test
    public void testGetTeacherRoster_WithValidWave_ShouldReturnRoster() {
        // Arrange - Create assignment
        lunchAssignmentService.assignTeacherToWave(testTeacher.getId(), testWave1.getId(), "test");

        // Act
        ResponseEntity<List<TeacherLunchAssignment>> response =
            lunchAssignmentController.getTeacherRoster(testWave1.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 1);

        System.out.println("✓ Teacher roster retrieved successfully");
        System.out.println("  - Roster size: " + response.getBody().size());
    }

    @Test
    public void testGetUnassignedStudents_ShouldReturnList() {
        // Act
        ResponseEntity<List<Student>> response =
            lunchAssignmentController.getUnassignedStudents(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        System.out.println("✓ Unassigned students retrieved successfully");
        System.out.println("  - Unassigned count: " + response.getBody().size());
    }

    @Test
    public void testGetUnassignedTeachers_ShouldReturnList() {
        // Act
        ResponseEntity<List<Teacher>> response =
            lunchAssignmentController.getUnassignedTeachers(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        System.out.println("✓ Unassigned teachers retrieved successfully");
        System.out.println("  - Unassigned count: " + response.getBody().size());
    }

    // ========== STATISTICS OPERATIONS ==========

    @Test
    public void testGetStatistics_ShouldReturnStats() {
        // Act
        ResponseEntity<LunchAssignmentService.LunchAssignmentStatistics> response =
            lunchAssignmentController.getStatistics(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        System.out.println("✓ Assignment statistics retrieved successfully");
        System.out.println("  - Total students: " + response.getBody().getTotalStudents());
        System.out.println("  - Total teachers: " + response.getBody().getTotalTeachers());
    }

    @Test
    public void testValidateAssignments_ShouldReturnValidation() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            lunchAssignmentController.validateAssignments(testSchedule.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("allStudentsAssigned"));
        assertTrue(response.getBody().containsKey("capacitiesRespected"));
        assertTrue(response.getBody().containsKey("gradeLevelsRespected"));
        assertTrue(response.getBody().containsKey("isValid"));

        System.out.println("✓ Assignment validation completed");
        System.out.println("  - All students assigned: " + response.getBody().get("allStudentsAssigned"));
        System.out.println("  - Capacities respected: " + response.getBody().get("capacitiesRespected"));
        System.out.println("  - Grade levels respected: " + response.getBody().get("gradeLevelsRespected"));
        System.out.println("  - Is valid: " + response.getBody().get("isValid"));
    }
}
