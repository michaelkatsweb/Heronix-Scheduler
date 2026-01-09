package com.heronix.controller;

import com.heronix.dto.StudentDTO;
import com.heronix.dto.StudentDashboardDTO;
import com.heronix.dto.StudentScheduleDTO;
import com.heronix.model.domain.Student;
import com.heronix.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StudentController
 * Tests CRUD operations, query endpoints, and Student Portal functionality
 *
 * @author Heronix Scheduling System Team
 * @since December 19, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class StudentControllerTest {

    @Autowired
    private StudentController studentController;

    @Autowired
    private StudentRepository studentRepository;

    private Student testStudent;
    private Student testStudent2;

    @BeforeEach
    public void setup() {
        // Create first test student
        testStudent = new Student();
        testStudent.setStudentId("STU001");
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setEmail("john.doe@school.edu");
        testStudent.setGradeLevel("9");
        testStudent.setActive(true);
        testStudent.setDateOfBirth(LocalDate.of(2009, 5, 15));
        testStudent.setGraduationYear(2027);
        testStudent.setCurrentGPA(3.5);
        testStudent.setCreditsEarned(10.0);
        testStudent.setCreditsRequired(24.0);
        testStudent.setHasIEP(false);
        testStudent.setHas504Plan(false);
        testStudent = studentRepository.save(testStudent);

        // Create second test student for filtering tests
        testStudent2 = new Student();
        testStudent2.setStudentId("STU002");
        testStudent2.setFirstName("Jane");
        testStudent2.setLastName("Smith");
        testStudent2.setEmail("jane.smith@school.edu");
        testStudent2.setGradeLevel("10");
        testStudent2.setActive(false); // Inactive student for testing
        testStudent2.setDateOfBirth(LocalDate.of(2008, 8, 20));
        testStudent2.setGraduationYear(2026);
        testStudent2.setCurrentGPA(3.8);
        testStudent2.setCreditsEarned(15.0);
        testStudent2.setCreditsRequired(24.0);
        testStudent2.setHasIEP(true);
        testStudent2.setHas504Plan(false);
        testStudent2 = studentRepository.save(testStudent2);
    }

    // ========== BASIC CRUD OPERATIONS ==========

    @Test
    public void testGetAllStudents_ShouldReturnList() {
        // Act
        List<Student> students = studentController.getAllStudents();

        // Assert
        assertNotNull(students);
        assertTrue(students.size() >= 2, "Should return at least two students");

        System.out.println("✓ Students retrieved successfully");
        System.out.println("  - Total students: " + students.size());
    }

    @Test
    public void testGetStudentById_WithValidId_ShouldReturnDTO() {
        // Act
        ResponseEntity<StudentDTO> response = studentController.getStudentById(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("STU001", response.getBody().getStudentId());
        assertEquals("John", response.getBody().getFirstName());
        assertEquals("Doe", response.getBody().getLastName());
        assertEquals("9", response.getBody().getGradeLevel());

        System.out.println("✓ Student retrieved by ID successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Student ID: " + response.getBody().getStudentId());
        System.out.println("  - Name: " + response.getBody().getFullName());
    }

    @Test
    public void testGetStudentById_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<StudentDTO> response = studentController.getStudentById(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Non-existent student correctly returns 404");
    }

    @Test
    public void testCreateStudent_WithValidData_ShouldSucceed() {
        // Arrange
        Student newStudent = new Student();
        newStudent.setStudentId("STU003");
        newStudent.setFirstName("Bob");
        newStudent.setLastName("Johnson");
        newStudent.setEmail("bob.johnson@school.edu");
        newStudent.setGradeLevel("11");
        newStudent.setDateOfBirth(LocalDate.of(2007, 3, 10));

        // Act
        ResponseEntity<Student> response = studentController.createStudent(newStudent);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("STU003", response.getBody().getStudentId());
        assertTrue(response.getBody().isActive(), "New student should be active by default");

        System.out.println("✓ Student created successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Student ID: " + response.getBody().getStudentId());
    }

    @Test
    public void testUpdateStudent_WithValidData_ShouldSucceed() {
        // Arrange
        testStudent.setEmail("john.doe.updated@school.edu");
        testStudent.setGradeLevel("10");

        // Act
        ResponseEntity<Student> response = studentController.updateStudent(
                testStudent.getId(), testStudent);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("john.doe.updated@school.edu", response.getBody().getEmail());
        assertEquals("10", response.getBody().getGradeLevel());

        System.out.println("✓ Student updated successfully");
        System.out.println("  - Updated email: " + response.getBody().getEmail());
        System.out.println("  - Updated grade: " + response.getBody().getGradeLevel());
    }

    @Test
    public void testUpdateStudent_WithNonExistentId_ShouldReturn404() {
        // Arrange
        Student studentUpdate = new Student();
        studentUpdate.setFirstName("Test");

        // Act
        ResponseEntity<Student> response = studentController.updateStudent(99999L, studentUpdate);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Update non-existent student correctly returns 404");
    }

    @Test
    public void testDeleteStudent_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<?> response = studentController.deleteStudent(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify soft deletion (student still exists but inactive)
        Student deletedStudent = studentRepository.findById(testStudent.getId()).orElse(null);
        assertNotNull(deletedStudent, "Student should still exist in database");
        assertFalse(deletedStudent.isActive(), "Student should be inactive after deletion");

        System.out.println("✓ Student soft deleted successfully");
        System.out.println("  - Student still exists: " + (deletedStudent != null));
        System.out.println("  - Student is inactive: " + !deletedStudent.isActive());
    }

    @Test
    public void testDeleteStudent_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<?> response = studentController.deleteStudent(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Delete non-existent student correctly returns 404");
    }

    // ========== QUERY OPERATIONS ==========

    @Test
    public void testGetActiveStudents_ShouldReturnOnlyActiveStudents() {
        // Act
        List<Student> activeStudents = studentController.getActiveStudents();

        // Assert
        assertNotNull(activeStudents);
        assertTrue(activeStudents.size() >= 1, "Should have at least one active student");

        // Verify all returned students are active
        boolean allActive = activeStudents.stream().allMatch(Student::isActive);
        assertTrue(allActive, "All returned students should be active");

        // Verify inactive student is not in the list
        boolean hasInactiveStudent = activeStudents.stream()
                .anyMatch(s -> s.getId().equals(testStudent2.getId()));
        assertFalse(hasInactiveStudent, "Inactive student should not be in active list");

        System.out.println("✓ Active students retrieved successfully");
        System.out.println("  - Active students: " + activeStudents.size());
    }

    @Test
    public void testGetStudentByStudentId_WithValidId_ShouldReturnStudent() {
        // Act
        ResponseEntity<Student> response = studentController.getStudentByStudentId("STU001");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("STU001", response.getBody().getStudentId());
        assertEquals("John", response.getBody().getFirstName());

        System.out.println("✓ Student retrieved by student ID successfully");
        System.out.println("  - Student ID: " + response.getBody().getStudentId());
    }

    @Test
    public void testGetStudentByStudentId_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Student> response = studentController.getStudentByStudentId("NONEXISTENT");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Non-existent student ID correctly returns 404");
    }

    @Test
    public void testGetStudentsByGradeLevel_WithValidGrade_ShouldReturnStudents() {
        // Act
        List<Student> students = studentController.getStudentsByGradeLevel("9");

        // Assert
        assertNotNull(students);
        assertTrue(students.size() >= 1, "Should find at least one 9th grade student");

        // Verify all students are in grade 9
        boolean allGrade9 = students.stream()
                .allMatch(s -> "9".equals(s.getGradeLevel()));
        assertTrue(allGrade9, "All returned students should be in grade 9");

        System.out.println("✓ Students retrieved by grade level successfully");
        System.out.println("  - Grade level: 9");
        System.out.println("  - Students found: " + students.size());
    }

    @Test
    public void testGetStudentsByGradeLevel_WithNonExistentGrade_ShouldReturnEmptyList() {
        // Act
        List<Student> students = studentController.getStudentsByGradeLevel("99");

        // Assert
        assertNotNull(students);
        assertTrue(students.isEmpty(), "Should return empty list for non-existent grade");

        System.out.println("✓ Non-existent grade level returns empty list");
    }

    // ========== ACTIVATION OPERATIONS ==========

    @Test
    public void testActivateStudent_WithInactiveStudent_ShouldSucceed() {
        // Arrange - testStudent2 is inactive
        assertFalse(testStudent2.isActive(), "Precondition: Student should be inactive");

        // Act
        ResponseEntity<Student> response = studentController.activateStudent(testStudent2.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isActive(), "Student should be active after activation");

        System.out.println("✓ Student activated successfully");
        System.out.println("  - Student ID: " + response.getBody().getId());
        System.out.println("  - Is now active: " + response.getBody().isActive());
    }

    @Test
    public void testActivateStudent_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Student> response = studentController.activateStudent(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Activate non-existent student correctly returns 404");
    }

    @Test
    public void testDeactivateStudent_WithActiveStudent_ShouldSucceed() {
        // Arrange - testStudent is active
        assertTrue(testStudent.isActive(), "Precondition: Student should be active");

        // Act
        ResponseEntity<Student> response = studentController.deactivateStudent(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isActive(), "Student should be inactive after deactivation");

        System.out.println("✓ Student deactivated successfully");
        System.out.println("  - Student ID: " + response.getBody().getId());
        System.out.println("  - Is now inactive: " + !response.getBody().isActive());
    }

    @Test
    public void testDeactivateStudent_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Student> response = studentController.deactivateStudent(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Deactivate non-existent student correctly returns 404");
    }

    // ========== STUDENT PORTAL ENDPOINTS ==========

    @Test
    public void testGetStudentDashboard_WithValidId_ShouldReturnDashboard() {
        // Act
        ResponseEntity<StudentDashboardDTO> response = studentController.getStudentDashboard(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        StudentDashboardDTO dashboard = response.getBody();
        assertEquals(testStudent.getId(), dashboard.getStudentId());
        assertEquals("John Doe", dashboard.getStudentName());
        assertEquals("9", dashboard.getGradeLevel());
        assertEquals(3.5, dashboard.getCurrentGPA());
        assertEquals(10.0, dashboard.getCreditsEarned());
        assertEquals(24.0, dashboard.getCreditsRequired());

        System.out.println("✓ Student dashboard retrieved successfully");
        System.out.println("  - Student: " + dashboard.getStudentName());
        System.out.println("  - GPA: " + dashboard.getCurrentGPA());
        System.out.println("  - Credits: " + dashboard.getCreditsEarned() + "/" + dashboard.getCreditsRequired());
    }

    @Test
    public void testGetStudentDashboard_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<StudentDashboardDTO> response = studentController.getStudentDashboard(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Dashboard for non-existent student correctly returns 404");
    }

    @Test
    public void testGetStudentSchedule_WithValidId_ShouldReturnSchedule() {
        // Act
        ResponseEntity<StudentScheduleDTO> response = studentController.getStudentSchedule(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        StudentScheduleDTO schedule = response.getBody();
        assertEquals(testStudent.getId(), schedule.getStudentId());
        assertEquals("John Doe", schedule.getStudentName());
        assertEquals("9", schedule.getGradeLevel());
        assertNotNull(schedule.getScheduleSlots());

        System.out.println("✓ Student schedule retrieved successfully");
        System.out.println("  - Student: " + schedule.getStudentName());
        System.out.println("  - Schedule slots: " + schedule.getScheduleSlots().size());
    }

    @Test
    public void testGetStudentSchedule_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<StudentScheduleDTO> response = studentController.getStudentSchedule(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Schedule for non-existent student correctly returns 404");
    }
}
