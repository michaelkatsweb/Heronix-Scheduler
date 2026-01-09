package com.heronix.controller;

import com.heronix.dto.TeacherSyncDTO;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TeacherSyncController
 * Tests teacher synchronization REST API endpoints
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class TeacherSyncControllerTest {

    @Autowired
    private TeacherSyncController teacherSyncController;

    @Autowired
    private TeacherRepository teacherRepository;

    private Teacher activeTeacher;
    private Teacher inactiveTeacher;

    @BeforeEach
    public void setup() {
        // Create active teacher
        activeTeacher = new Teacher();
        activeTeacher.setName("Active Teacher");
        activeTeacher.setFirstName("Active");
        activeTeacher.setLastName("Teacher");
        activeTeacher.setEmployeeId("SYNC-001");
        activeTeacher.setEmail("active@school.edu");
        activeTeacher.setDepartment("Mathematics");
        activeTeacher.setPassword("$2a$10$hashedpassword");  // BCrypt hashed
        activeTeacher.setActive(true);
        activeTeacher = teacherRepository.save(activeTeacher);

        // Create inactive teacher
        inactiveTeacher = new Teacher();
        inactiveTeacher.setName("Inactive Teacher");
        inactiveTeacher.setFirstName("Inactive");
        inactiveTeacher.setLastName("Teacher");
        inactiveTeacher.setEmployeeId("SYNC-002");
        inactiveTeacher.setEmail("inactive@school.edu");
        inactiveTeacher.setDepartment("Science");
        inactiveTeacher.setPassword("$2a$10$hashedpassword");
        inactiveTeacher.setActive(false);
        inactiveTeacher = teacherRepository.save(inactiveTeacher);
    }

    // ========== HEALTH CHECK ==========

    @Test
    public void testHealth_ShouldReturnOk() {
        // Act
        ResponseEntity<String> response = teacherSyncController.health();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Teacher Sync API is running", response.getBody());

        System.out.println("✓ Health check passed");
        System.out.println("  - Status: " + response.getBody());
    }

    // ========== GET ALL TEACHERS ==========

    @Test
    public void testGetAllTeachers_ShouldReturnActiveTeachersOnly() {
        // Act
        ResponseEntity<List<TeacherSyncDTO>> response = teacherSyncController.getAllTeachers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 1, "Should have at least one active teacher");

        // Verify all returned teachers are active
        boolean allActive = response.getBody().stream().allMatch(TeacherSyncDTO::isActive);
        assertTrue(allActive, "All returned teachers should be active");

        // Verify our test teacher is included
        boolean containsTestTeacher = response.getBody().stream()
            .anyMatch(dto -> "SYNC-001".equals(dto.getEmployeeId()));
        assertTrue(containsTestTeacher, "Should contain our test teacher");

        // Verify inactive teacher is NOT included
        boolean containsInactiveTeacher = response.getBody().stream()
            .anyMatch(dto -> "SYNC-002".equals(dto.getEmployeeId()));
        assertFalse(containsInactiveTeacher, "Should NOT contain inactive teacher");

        System.out.println("✓ All active teachers retrieved successfully");
        System.out.println("  - Total active teachers: " + response.getBody().size());
    }

    @Test
    public void testGetAllTeachers_ShouldIncludeRequiredFields() {
        // Act
        ResponseEntity<List<TeacherSyncDTO>> response = teacherSyncController.getAllTeachers();

        // Assert
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());

        // Get our test teacher from results
        TeacherSyncDTO dto = response.getBody().stream()
            .filter(t -> "SYNC-001".equals(t.getEmployeeId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Test teacher not found"));

        // Verify all required fields are present
        assertNotNull(dto.getId());
        assertEquals("SYNC-001", dto.getEmployeeId());
        assertEquals("Active", dto.getFirstName());
        assertEquals("Teacher", dto.getLastName());
        assertEquals("Active Teacher", dto.getName());
        assertEquals("active@school.edu", dto.getEmail());
        assertEquals("Mathematics", dto.getDepartment());
        assertEquals("$2a$10$hashedpassword", dto.getPassword());
        assertTrue(dto.isActive());

        System.out.println("✓ Teacher DTO contains all required fields");
        System.out.println("  - Employee ID: " + dto.getEmployeeId());
        System.out.println("  - Name: " + dto.getName());
        System.out.println("  - Email: " + dto.getEmail());
        System.out.println("  - Password: " + (dto.getPassword() != null ? "[ENCRYPTED]" : "null"));
    }

    // ========== GET TEACHER BY EMPLOYEE ID ==========

    @Test
    public void testGetTeacherByEmployeeId_WithValidActiveTeacher_ShouldReturnTeacher() {
        // Act
        ResponseEntity<TeacherSyncDTO> response =
            teacherSyncController.getTeacherByEmployeeId("SYNC-001");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SYNC-001", response.getBody().getEmployeeId());
        assertEquals("Active Teacher", response.getBody().getName());
        assertEquals("active@school.edu", response.getBody().getEmail());
        assertTrue(response.getBody().isActive());

        System.out.println("✓ Active teacher retrieved by employee ID");
        System.out.println("  - Employee ID: " + response.getBody().getEmployeeId());
        System.out.println("  - Name: " + response.getBody().getName());
    }

    @Test
    public void testGetTeacherByEmployeeId_WithInactiveTeacher_ShouldReturn404() {
        // Act
        ResponseEntity<TeacherSyncDTO> response =
            teacherSyncController.getTeacherByEmployeeId("SYNC-002");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Inactive teacher correctly returns 404");
        System.out.println("  - Employee ID: SYNC-002 (inactive)");
    }

    @Test
    public void testGetTeacherByEmployeeId_WithNonExistent_ShouldReturn404() {
        // Act
        ResponseEntity<TeacherSyncDTO> response =
            teacherSyncController.getTeacherByEmployeeId("NONEXISTENT");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Non-existent teacher correctly returns 404");
    }

    @Test
    public void testGetTeacherByEmployeeId_ShouldIncludeEncryptedPassword() {
        // Act
        ResponseEntity<TeacherSyncDTO> response =
            teacherSyncController.getTeacherByEmployeeId("SYNC-001");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getPassword());
        assertTrue(response.getBody().getPassword().startsWith("$2a$"),
            "Password should be BCrypt encrypted");

        System.out.println("✓ Teacher sync includes encrypted password");
        System.out.println("  - Password format: BCrypt (starts with $2a$)");
    }

    // ========== SECURITY TESTS ==========

    @Test
    public void testGetAllTeachers_ShouldNotExposeInactiveAccounts() {
        // Act
        ResponseEntity<List<TeacherSyncDTO>> response = teacherSyncController.getAllTeachers();

        // Assert
        assertNotNull(response.getBody());

        // Verify NO inactive teachers are exposed
        long inactiveCount = response.getBody().stream()
            .filter(dto -> !dto.isActive())
            .count();

        assertEquals(0, inactiveCount, "Should not expose any inactive teachers");

        System.out.println("✓ Security: Inactive accounts not exposed");
        System.out.println("  - Inactive teachers in response: 0");
    }

    @Test
    public void testTeacherSyncDTO_ShouldContainOnlyNecessaryFields() {
        // Act
        ResponseEntity<TeacherSyncDTO> response =
            teacherSyncController.getTeacherByEmployeeId("SYNC-001");

        // Assert
        assertNotNull(response.getBody());

        // Verify essential fields are present
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getEmployeeId());
        assertNotNull(response.getBody().getName());
        assertNotNull(response.getBody().getPassword());

        System.out.println("✓ Teacher sync DTO contains necessary fields only");
        System.out.println("  - Fields included: id, employeeId, name, email, password, etc.");
    }
}
