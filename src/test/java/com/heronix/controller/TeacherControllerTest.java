package com.heronix.controller;

import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.PriorityLevel;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TeacherController
 * Tests CRUD operations, query endpoints, and teacher management functionality
 *
 * @author Heronix Scheduling System Team
 * @since December 19, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class TeacherControllerTest {

    @Autowired
    private TeacherController teacherController;

    @Autowired
    private TeacherRepository teacherRepository;

    private Teacher testTeacher;
    private Teacher testTeacher2;

    @BeforeEach
    public void setup() {
        // Create first test teacher
        testTeacher = new Teacher();
        testTeacher.setEmployeeId("TCH001");
        testTeacher.setName("John Smith");
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Smith");
        testTeacher.setDepartment("Mathematics");
        testTeacher.setEmail("john.smith@school.edu");
        testTeacher.setPhoneNumber("555-0001");
        testTeacher.setCertifications(new ArrayList<>(Arrays.asList("Math 7-12", "Advanced Math")));
        testTeacher.setMaxHoursPerWeek(40);
        testTeacher.setMaxConsecutiveHours(4);
        testTeacher.setPreferredBreakMinutes(15);
        testTeacher.setPriorityLevel(PriorityLevel.NORMAL);
        testTeacher.setNotes("Senior Math Teacher");
        testTeacher.setActive(true);
        testTeacher.setCurrentWeekHours(25);
        testTeacher = teacherRepository.save(testTeacher);

        // Create second test teacher for filtering tests
        testTeacher2 = new Teacher();
        testTeacher2.setEmployeeId("TCH002");
        testTeacher2.setName("Jane Doe");
        testTeacher2.setFirstName("Jane");
        testTeacher2.setLastName("Doe");
        testTeacher2.setDepartment("Science");
        testTeacher2.setEmail("jane.doe@school.edu");
        testTeacher2.setPhoneNumber("555-0002");
        testTeacher2.setCertifications(new ArrayList<>(Arrays.asList("Biology 7-12", "Chemistry")));
        testTeacher2.setMaxHoursPerWeek(35);
        testTeacher2.setMaxConsecutiveHours(3);
        testTeacher2.setPreferredBreakMinutes(20);
        testTeacher2.setPriorityLevel(PriorityLevel.HIGH);
        testTeacher2.setNotes("Science Department Head");
        testTeacher2.setActive(false); // Inactive teacher for testing
        testTeacher2.setCurrentWeekHours(0);
        testTeacher2 = teacherRepository.save(testTeacher2);
    }

    // ========== BASIC CRUD OPERATIONS ==========

    @Test
    public void testGetAllTeachers_ShouldReturnActiveTeachers() {
        // Act
        List<Teacher> teachers = teacherController.getAllTeachers();

        // Assert
        assertNotNull(teachers);
        assertTrue(teachers.size() >= 1, "Should return at least one active teacher");

        // Verify only active teachers returned
        boolean allActive = teachers.stream().allMatch(Teacher::getActive);
        assertTrue(allActive, "All returned teachers should be active");

        System.out.println("✓ Active teachers retrieved successfully");
        System.out.println("  - Active teachers: " + teachers.size());
    }

    @Test
    public void testGetTeacherById_WithValidId_ShouldReturnTeacher() {
        // Act
        ResponseEntity<Teacher> response = teacherController.getTeacherById(testTeacher.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TCH001", response.getBody().getEmployeeId());
        assertEquals("John Smith", response.getBody().getName());
        assertEquals("Mathematics", response.getBody().getDepartment());

        System.out.println("✓ Teacher retrieved by ID successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Employee ID: " + response.getBody().getEmployeeId());
        System.out.println("  - Name: " + response.getBody().getName());
    }

    @Test
    public void testGetTeacherById_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Teacher> response = teacherController.getTeacherById(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Non-existent teacher correctly returns 404");
    }

    @Test
    public void testCreateTeacher_WithValidData_ShouldSucceed() {
        // Arrange
        Teacher newTeacher = new Teacher();
        newTeacher.setEmployeeId("TCH003");
        newTeacher.setName("Bob Johnson");
        newTeacher.setFirstName("Bob");
        newTeacher.setLastName("Johnson");
        newTeacher.setDepartment("English");
        newTeacher.setEmail("bob.johnson@school.edu");
        newTeacher.setPhoneNumber("555-0003");
        newTeacher.setMaxHoursPerWeek(38);

        // Act
        ResponseEntity<Teacher> response = teacherController.createTeacher(newTeacher);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("TCH003", response.getBody().getEmployeeId());
        assertTrue(response.getBody().getActive(), "New teacher should be active by default");

        System.out.println("✓ Teacher created successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Employee ID: " + response.getBody().getEmployeeId());
    }

    @Test
    public void testUpdateTeacher_WithValidData_ShouldSucceed() {
        // Arrange
        testTeacher.setName("John R. Smith");
        testTeacher.setDepartment("Mathematics & Computer Science");
        testTeacher.setMaxHoursPerWeek(35);

        // Act
        ResponseEntity<Teacher> response = teacherController.updateTeacher(
                testTeacher.getId(), testTeacher);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John R. Smith", response.getBody().getName());
        assertEquals("Mathematics & Computer Science", response.getBody().getDepartment());
        assertEquals(35, response.getBody().getMaxHoursPerWeek());

        System.out.println("✓ Teacher updated successfully");
        System.out.println("  - Updated name: " + response.getBody().getName());
        System.out.println("  - Updated department: " + response.getBody().getDepartment());
        System.out.println("  - Updated max hours: " + response.getBody().getMaxHoursPerWeek());
    }

    @Test
    public void testUpdateTeacher_WithNonExistentId_ShouldReturn404() {
        // Arrange
        Teacher teacherUpdate = new Teacher();
        teacherUpdate.setName("Test Teacher");

        // Act
        ResponseEntity<Teacher> response = teacherController.updateTeacher(99999L, teacherUpdate);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Update non-existent teacher correctly returns 404");
    }

    @Test
    public void testDeleteTeacher_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<?> response = teacherController.deleteTeacher(testTeacher.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify soft deletion (teacher still exists but inactive)
        Teacher deletedTeacher = teacherRepository.findById(testTeacher.getId()).orElse(null);
        assertNotNull(deletedTeacher, "Teacher should still exist in database");
        assertFalse(deletedTeacher.getActive(), "Teacher should be inactive after deletion");

        System.out.println("✓ Teacher soft deleted successfully");
        System.out.println("  - Teacher still exists: " + (deletedTeacher != null));
        System.out.println("  - Teacher is inactive: " + !deletedTeacher.getActive());
    }

    @Test
    public void testDeleteTeacher_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<?> response = teacherController.deleteTeacher(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Delete non-existent teacher correctly returns 404");
    }

    // ========== QUERY OPERATIONS ==========

    @Test
    public void testGetActiveTeachers_ShouldReturnOnlyActiveTeachers() {
        // Act
        List<Teacher> activeTeachers = teacherController.getActiveTeachers();

        // Assert
        assertNotNull(activeTeachers);
        assertTrue(activeTeachers.size() >= 1, "Should have at least one active teacher");

        // Verify all returned teachers are active
        boolean allActive = activeTeachers.stream().allMatch(Teacher::getActive);
        assertTrue(allActive, "All returned teachers should be active");

        // Verify inactive teacher is not in the list
        boolean hasInactiveTeacher = activeTeachers.stream()
                .anyMatch(t -> t.getId().equals(testTeacher2.getId()));
        assertFalse(hasInactiveTeacher, "Inactive teacher should not be in active list");

        System.out.println("✓ Active teachers retrieved successfully");
        System.out.println("  - Active teachers: " + activeTeachers.size());
    }

    @Test
    public void testGetTeachersByDepartment_WithValidDepartment_ShouldReturnTeachers() {
        // Act
        List<Teacher> mathTeachers = teacherController.getTeachersByDepartment("Mathematics");

        // Assert
        assertNotNull(mathTeachers);
        assertTrue(mathTeachers.size() >= 1, "Should find at least one Math teacher");

        // Verify all teachers are from Mathematics department
        boolean allMath = mathTeachers.stream()
                .allMatch(t -> "Mathematics".equals(t.getDepartment()));
        assertTrue(allMath, "All returned teachers should be from Mathematics department");

        System.out.println("✓ Teachers retrieved by department successfully");
        System.out.println("  - Department: Mathematics");
        System.out.println("  - Teachers found: " + mathTeachers.size());
    }

    @Test
    public void testGetTeachersByDepartment_WithNonExistentDepartment_ShouldReturnEmptyList() {
        // Act
        List<Teacher> teachers = teacherController.getTeachersByDepartment("NonExistentDept");

        // Assert
        assertNotNull(teachers);
        assertTrue(teachers.isEmpty(), "Should return empty list for non-existent department");

        System.out.println("✓ Non-existent department returns empty list");
    }

    @Test
    public void testGetTeacherByEmployeeId_WithValidId_ShouldReturnTeacher() {
        // Act
        ResponseEntity<Teacher> response = teacherController.getTeacherByEmployeeId("TCH001");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TCH001", response.getBody().getEmployeeId());
        assertEquals("John Smith", response.getBody().getName());

        System.out.println("✓ Teacher retrieved by employee ID successfully");
        System.out.println("  - Employee ID: " + response.getBody().getEmployeeId());
    }

    @Test
    public void testGetTeacherByEmployeeId_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Teacher> response = teacherController.getTeacherByEmployeeId("NONEXISTENT");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Non-existent employee ID correctly returns 404");
    }

    @Test
    public void testGetTeacherUtilization_WithValidId_ShouldReturnUtilizationData() {
        // Act
        ResponseEntity<Map<String, Object>> response =
                teacherController.getTeacherUtilization(testTeacher.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> utilization = response.getBody();
        assertEquals(testTeacher.getId(), utilization.get("teacherId"));
        assertEquals("John Smith", utilization.get("teacherName"));
        assertNotNull(utilization.get("utilizationRate"));
        assertEquals(25, utilization.get("currentWeekHours"));
        assertEquals(40, utilization.get("maxWeekHours"));

        System.out.println("✓ Teacher utilization retrieved successfully");
        System.out.println("  - Teacher: " + utilization.get("teacherName"));
        System.out.println("  - Current hours: " + utilization.get("currentWeekHours"));
        System.out.println("  - Max hours: " + utilization.get("maxWeekHours"));
        System.out.println("  - Utilization rate: " + utilization.get("utilizationRate"));
    }

    @Test
    public void testGetTeacherUtilization_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Map<String, Object>> response =
                teacherController.getTeacherUtilization(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Utilization for non-existent teacher correctly returns 404");
    }

    // ========== ACTIVATION OPERATIONS ==========

    @Test
    public void testActivateTeacher_WithInactiveTeacher_ShouldSucceed() {
        // Arrange - testTeacher2 is inactive
        assertFalse(testTeacher2.getActive(), "Precondition: Teacher should be inactive");

        // Act
        ResponseEntity<Teacher> response = teacherController.activateTeacher(testTeacher2.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getActive(), "Teacher should be active after activation");

        System.out.println("✓ Teacher activated successfully");
        System.out.println("  - Teacher ID: " + response.getBody().getId());
        System.out.println("  - Is now active: " + response.getBody().getActive());
    }

    @Test
    public void testActivateTeacher_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Teacher> response = teacherController.activateTeacher(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Activate non-existent teacher correctly returns 404");
    }

    @Test
    public void testDeactivateTeacher_WithActiveTeacher_ShouldSucceed() {
        // Arrange - testTeacher is active
        assertTrue(testTeacher.getActive(), "Precondition: Teacher should be active");

        // Act
        ResponseEntity<Teacher> response = teacherController.deactivateTeacher(testTeacher.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getActive(), "Teacher should be inactive after deactivation");

        System.out.println("✓ Teacher deactivated successfully");
        System.out.println("  - Teacher ID: " + response.getBody().getId());
        System.out.println("  - Is now inactive: " + !response.getBody().getActive());
    }

    @Test
    public void testDeactivateTeacher_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Teacher> response = teacherController.deactivateTeacher(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Deactivate non-existent teacher correctly returns 404");
    }
}
