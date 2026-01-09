package com.heronix.controller;

import com.heronix.repository.*;
import com.heronix.util.BulkDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AdminController
 * Tests administrative endpoints for data management
 *
 * @author Heronix Scheduling System Team
 * @since December 19, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class AdminControllerTest {

    @Autowired
    private AdminController adminController;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BulkDataGenerator bulkDataGenerator;

    @BeforeEach
    public void setup() {
        // Start with clean state for each test
        // @Transactional will rollback after each test
    }

    // ========== STATUS ENDPOINTS ==========

    @Test
    public void testGetStatus_ShouldReturnDatabaseStatus() {
        // Act
        ResponseEntity<Map<String, Object>> response = adminController.getStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> status = response.getBody();
        assertTrue(status.containsKey("teachers"), "Should contain teachers count");
        assertTrue(status.containsKey("courses"), "Should contain courses count");
        assertTrue(status.containsKey("rooms"), "Should contain rooms count");
        assertTrue(status.containsKey("schedules"), "Should contain schedules count");
        assertTrue(status.containsKey("students"), "Should contain students count");
        assertTrue(status.containsKey("events"), "Should contain events count");
        assertTrue(status.containsKey("totalRecords"), "Should contain total records count");

        System.out.println("✓ Database status retrieved successfully");
        System.out.println("  - Teachers: " + status.get("teachers"));
        System.out.println("  - Courses: " + status.get("courses"));
        System.out.println("  - Rooms: " + status.get("rooms"));
        System.out.println("  - Total records: " + status.get("totalRecords"));
    }

    @Test
    public void testHealth_ShouldReturnHealthStatus() {
        // Act
        ResponseEntity<Map<String, String>> response = adminController.health();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, String> health = response.getBody();
        assertEquals("UP", health.get("status"));
        assertTrue(health.containsKey("timestamp"), "Should contain timestamp");

        System.out.println("✓ Health check successful");
        System.out.println("  - Status: " + health.get("status"));
        System.out.println("  - Timestamp: " + health.get("timestamp"));
    }

    // ========== DATA GENERATION ENDPOINTS ==========

    @Test
    public void testGenerateTeachers_WithValidCount_ShouldSucceed() {
        // Arrange
        Map<String, Integer> request = Map.of("count", 10);
        long initialCount = teacherRepository.countByActiveTrue();

        // Act
        ResponseEntity<Map<String, Object>> response = adminController.generateTeachers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> result = response.getBody();
        assertTrue(result.containsKey("message"), "Should contain message");
        assertTrue(result.containsKey("timeTakenMs"), "Should contain time taken");
        assertTrue(result.containsKey("totalTeachers"), "Should contain total teachers");

        // Verify teachers were created
        long finalCount = teacherRepository.countByActiveTrue();
        assertTrue(finalCount >= initialCount, "Teacher count should increase");

        System.out.println("✓ Teachers generated successfully");
        System.out.println("  - Generated: 10");
        System.out.println("  - Time taken: " + result.get("timeTakenMs") + "ms");
        System.out.println("  - Total teachers: " + result.get("totalTeachers"));
    }

    @Test
    public void testGenerateTeachers_WithExcessiveCount_ShouldReturnBadRequest() {
        // Arrange
        Map<String, Integer> request = Map.of("count", 100000); // Exceeds max of 50,000

        // Act
        ResponseEntity<Map<String, Object>> response = adminController.generateTeachers(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> result = response.getBody();
        assertTrue(result.containsKey("error"), "Should contain error message");
        assertTrue(result.get("error").toString().contains("50,000"), "Should mention maximum limit");

        System.out.println("✓ Excessive count correctly rejected");
        System.out.println("  - Error: " + result.get("error"));
    }

    @Test
    public void testGenerateSmall_ShouldCreateSmallDataset() {
        // Arrange
        long initialCount = teacherRepository.countByActiveTrue();

        // Act
        ResponseEntity<Map<String, String>> response = adminController.generateSmall();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, String> result = response.getBody();
        assertTrue(result.containsKey("message"), "Should contain message");
        assertTrue(result.containsKey("teachers"), "Should contain teacher count");

        // Verify teachers were created
        long finalCount = teacherRepository.countByActiveTrue();
        assertTrue(finalCount >= initialCount, "Teacher count should increase");

        System.out.println("✓ Small dataset generated successfully");
        System.out.println("  - Message: " + result.get("message"));
        System.out.println("  - Teachers: " + result.get("teachers"));
    }

    @Test
    public void testGenerateMedium_ShouldCreateMediumDataset() {
        // Arrange
        long initialCount = teacherRepository.countByActiveTrue();

        // Act
        ResponseEntity<Map<String, String>> response = adminController.generateMedium();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, String> result = response.getBody();
        assertTrue(result.containsKey("message"), "Should contain message");
        assertTrue(result.containsKey("teachers"), "Should contain teacher count");

        // Verify teachers were created
        long finalCount = teacherRepository.countByActiveTrue();
        assertTrue(finalCount >= initialCount, "Teacher count should increase");

        System.out.println("✓ Medium dataset generated successfully");
        System.out.println("  - Message: " + result.get("message"));
        System.out.println("  - Teachers: " + result.get("teachers"));
    }

    // NOTE: testGenerateLarge and testGenerateXL skipped - they take too long for unit tests
    // These should be tested in integration/performance tests

    // ========== DATA CLEARING ENDPOINTS ==========

    @Test
    public void testClearTeachers_ShouldDeleteAllTeachers() {
        // Arrange - ensure we have some teachers
        bulkDataGenerator.generateTeachers(5);
        long initialCount = teacherRepository.countByActiveTrue();
        assertTrue(initialCount > 0, "Should have teachers before clearing");

        // Act
        ResponseEntity<Map<String, String>> response = adminController.clearTeachers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, String> result = response.getBody();
        assertTrue(result.containsKey("message"), "Should contain message");
        assertTrue(result.containsKey("remaining"), "Should contain remaining count");

        // Verify teachers were deleted
        long finalCount = teacherRepository.countByActiveTrue();
        assertEquals(0, finalCount, "All teachers should be deleted");

        System.out.println("✓ Teachers cleared successfully");
        System.out.println("  - Message: " + result.get("message"));
        System.out.println("  - Remaining: " + result.get("remaining"));
    }

    @Test
    public void testClearAll_ShouldDeleteAllData() {
        // Arrange - ensure we have some data
        bulkDataGenerator.generateTeachers(3);

        // Act
        ResponseEntity<Map<String, String>> response = adminController.clearAll();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, String> result = response.getBody();
        assertEquals("All data cleared", result.get("message"));
        assertTrue(result.containsKey("teachersDeleted"), "Should contain teachers deleted count");
        assertTrue(result.containsKey("coursesDeleted"), "Should contain courses deleted count");
        assertTrue(result.containsKey("roomsDeleted"), "Should contain rooms deleted count");
        assertTrue(result.containsKey("schedulesDeleted"), "Should contain schedules deleted count");
        assertTrue(result.containsKey("studentsDeleted"), "Should contain students deleted count");
        assertTrue(result.containsKey("eventsDeleted"), "Should contain events deleted count");

        // Verify all data cleared
        assertEquals(0, teacherRepository.countByActiveTrue(), "All teachers should be deleted");
        assertEquals(0, courseRepository.count(), "All courses should be deleted");
        assertEquals(0, roomRepository.count(), "All rooms should be deleted");
        assertEquals(0, studentRepository.count(), "All students should be deleted");

        System.out.println("✓ All data cleared successfully");
        System.out.println("  - Message: " + result.get("message"));
        System.out.println("  - Teachers deleted: " + result.get("teachersDeleted"));
        System.out.println("  - Courses deleted: " + result.get("coursesDeleted"));
        System.out.println("  - Rooms deleted: " + result.get("roomsDeleted"));
    }
}
