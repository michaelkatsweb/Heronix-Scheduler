package com.heronix.controller.api;

import com.heronix.api.dto.GradeDTO;
import com.heronix.api.dto.AttendanceDTO;
import com.heronix.api.dto.ConflictDTO;
import com.heronix.model.domain.*;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TeacherApiController
 * Tests the December 10, 2025 fix for timestamp-based conflict detection
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TeacherApiControllerTest {

    @Autowired
    private TeacherApiController teacherApiController;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentGradeRepository assignmentGradeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.eduscheduler.repository.GradingCategoryRepository gradingCategoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Teacher testTeacher;
    private Student testStudent;
    private User testUser;
    private Course testCourse;

    @BeforeEach
    public void setup() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setEmployeeId("API_TEST_001");
        testTeacher.setFirstName("API");
        testTeacher.setLastName("Tester");
        testTeacher.setName("API Tester");
        testTeacher.setEmail("api.tester@eduscheduler.com");
        testTeacher.setDepartment("Testing");
        testTeacher.setActive(true);
        testTeacher = teacherRepository.save(testTeacher);

        // Create user account for teacher
        testUser = new User();
        testUser.setUsername(testTeacher.getEmail());
        testUser.setEmail(testTeacher.getEmail()); // Email is required
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles(Set.of("TEACHER")); // Use Set instead of single role
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test student
        testStudent = new Student();
        testStudent.setStudentId("API_STU_001");
        testStudent.setFirstName("API");
        testStudent.setLastName("Student");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);
        testStudent = studentRepository.save(testStudent);

        // Create test course (needed for GradingCategory and AttendanceRecord)
        testCourse = new Course();
        testCourse.setCourseCode("API_TEST_101");
        testCourse.setCourseName("API Test Course");
        testCourse.setCourseType(com.eduscheduler.model.enums.CourseType.REGULAR);
        testCourse.setSubject("Testing");
        testCourse.setTeacher(testTeacher);
        testCourse = courseRepository.save(testCourse);
    }

    // ========================================================================
    // TEST 1: GRADE CONFLICT DETECTION (CRITICAL FIX - Dec 10, 2025)
    // ========================================================================

    @Test
    public void testGradeConflictDetection_ServerVersionNewer() {
        // Arrange: Create test assignment
        Assignment testAssignment = createTestAssignment();

        // Create existing grade with recent updatedAt timestamp
        LocalDateTime now = LocalDateTime.now();
        AssignmentGrade existingGrade = new AssignmentGrade();
        existingGrade.setStudent(testStudent);
        existingGrade.setAssignment(testAssignment);
        existingGrade.setScore(85.0);
        existingGrade.setComments("Original score");
        existingGrade.setUpdatedAt(now); // Manually set updated timestamp
        existingGrade = assignmentGradeRepository.save(existingGrade);

        // Client has older version (created 1 hour ago)
        LocalDateTime clientLastModified = now.minusHours(1);

        // Act: Try to submit grade with outdated timestamp
        GradeDTO gradeDTO = GradeDTO.builder()
            .id(existingGrade.getId())
            .studentId(testStudent.getId())
            .assignmentId(testAssignment.getId())
            .score(90.0) // Client wants to change to 90
            .comments("Updated score from client")
            .modifiedDate(clientLastModified) // Old timestamp
            .build();

        ResponseEntity<Map<String, Object>> response =
            teacherApiController.submitGradesBatch(List.of(gradeDTO));

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode(), "Should return CONFLICT status");

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(false, responseBody.get("success"), "Success should be false");
        assertEquals(1, responseBody.get("conflictCount"), "Should have 1 conflict");
        assertEquals(0, responseBody.get("successCount"), "Should have 0 successes");

        @SuppressWarnings("unchecked")
        List<ConflictDTO> conflicts = (List<ConflictDTO>) responseBody.get("conflicts");
        assertEquals(1, conflicts.size(), "Should have 1 conflict");

        ConflictDTO conflict = conflicts.get(0);
        assertEquals("Grade", conflict.getEntityType(), "Entity type should be Grade");
        assertEquals(existingGrade.getId(), conflict.getEntityId(), "Entity ID should match");
        assertNotNull(conflict.getServerTimestamp(), "Server timestamp should be present");
        assertNotNull(conflict.getClientTimestamp(), "Client timestamp should be present");

        System.out.println("✓ Grade conflict detected when server version is newer");
        System.out.println("  - Server timestamp: " + conflict.getServerTimestamp());
        System.out.println("  - Client timestamp: " + conflict.getClientTimestamp());
    }

    @Test
    public void testGradeConflictDetection_ClientVersionNewer() {
        // Arrange: Create test assignment
        Assignment testAssignment = createTestAssignment();

        // Create existing grade
        LocalDateTime now = LocalDateTime.now();
        AssignmentGrade existingGrade = new AssignmentGrade();
        existingGrade.setStudent(testStudent);
        existingGrade.setAssignment(testAssignment);
        existingGrade.setScore(85.0);
        existingGrade.setComments("Original score");
        existingGrade.setUpdatedAt(now); // Manually set updated timestamp
        existingGrade = assignmentGradeRepository.save(existingGrade);

        // Client has newer version (timestamp from future)
        LocalDateTime clientLastModified = now.plusMinutes(5);

        // Act
        GradeDTO gradeDTO = GradeDTO.builder()
            .id(existingGrade.getId())
            .studentId(testStudent.getId())
            .assignmentId(testAssignment.getId())
            .score(90.0)
            .comments("Updated score from client")
            .modifiedDate(clientLastModified) // Newer timestamp
            .build();

        ResponseEntity<Map<String, Object>> response =
            teacherApiController.submitGradesBatch(List.of(gradeDTO));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody.get("success"), "Success should be true");
        assertEquals(0, responseBody.get("conflictCount"), "Should have 0 conflicts");
        assertEquals(1, responseBody.get("successCount"), "Should have 1 success");

        // Verify grade was updated
        AssignmentGrade updatedGrade = assignmentGradeRepository.findById(existingGrade.getId()).get();
        assertEquals(90.0, updatedGrade.getScore(), "Score should be updated to 90.0");

        System.out.println("✓ No conflict when client version is newer");
    }

    @Test
    public void testGradeConflictDetection_NewGradeNoConflict() {
        // Arrange: Create test assignment
        Assignment testAssignment = createTestAssignment();

        // Create NEW grade (no existing record)
        GradeDTO gradeDTO = GradeDTO.builder()
            .studentId(testStudent.getId())
            .assignmentId(testAssignment.getId())
            .score(95.0)
            .comments("New grade entry")
            .gradedDate(java.time.LocalDate.now())
            .modifiedDate(LocalDateTime.now())
            .build();

        // Act
        ResponseEntity<Map<String, Object>> response =
            teacherApiController.submitGradesBatch(List.of(gradeDTO));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody.get("success"), "Success should be true");
        assertEquals(0, responseBody.get("conflictCount"), "Should have 0 conflicts");
        assertEquals(1, responseBody.get("successCount"), "Should have 1 success");

        System.out.println("✓ New grade created successfully with no conflict");
    }

    // ========================================================================
    // TEST 2: ATTENDANCE CONFLICT DETECTION (CRITICAL FIX - Dec 10, 2025)
    // ========================================================================

    @Test
    public void testAttendanceConflictDetection_ServerVersionNewer() {
        // Arrange: Create existing attendance record
        LocalDateTime now = LocalDateTime.now();
        AttendanceRecord existingRecord = new AttendanceRecord();
        existingRecord.setStudent(testStudent);
        existingRecord.setCourse(testCourse); // ← AttendanceRecord requires Course
        existingRecord.setAttendanceDate(now.toLocalDate());
        existingRecord.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
        existingRecord.setUpdatedAt(now); // Manually set updated timestamp
        existingRecord = attendanceRepository.save(existingRecord);

        // Client has older version
        LocalDateTime clientLastModified = now.minusHours(1);

        // Act: Try to submit attendance with outdated timestamp
        AttendanceDTO attendanceDTO = AttendanceDTO.builder()
            .id(existingRecord.getId())
            .studentId(testStudent.getId())
            .date(LocalDateTime.now().toLocalDate())
            .status("TARDY") // Client wants to change to TARDY
            .lastModified(clientLastModified) // Old timestamp
            .build();

        ResponseEntity<Map<String, Object>> response =
            teacherApiController.submitAttendanceBatch(List.of(attendanceDTO));

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode(), "Should return CONFLICT status");

        Map<String, Object> responseBody = response.getBody();
        assertEquals(false, responseBody.get("success"), "Success should be false");
        assertEquals(1, responseBody.get("conflictCount"), "Should have 1 conflict");
        assertEquals(0, responseBody.get("successCount"), "Should have 0 successes");

        @SuppressWarnings("unchecked")
        List<ConflictDTO> conflicts = (List<ConflictDTO>) responseBody.get("conflicts");
        assertEquals(1, conflicts.size(), "Should have 1 conflict");

        ConflictDTO conflict = conflicts.get(0);
        assertEquals("attendance", conflict.getEntityType(), "Entity type should be attendance");

        System.out.println("✓ Attendance conflict detected when server version is newer");
    }

    @Test
    public void testAttendanceConflictDetection_ClientVersionNewer() {
        // Arrange: Create existing attendance record
        LocalDateTime now = LocalDateTime.now();
        AttendanceRecord existingRecord = new AttendanceRecord();
        existingRecord.setStudent(testStudent);
        existingRecord.setCourse(testCourse); // ← AttendanceRecord requires Course
        existingRecord.setAttendanceDate(now.toLocalDate());
        existingRecord.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
        existingRecord.setUpdatedAt(now); // Manually set updated timestamp
        existingRecord = attendanceRepository.save(existingRecord);

        // Client has newer version
        LocalDateTime clientLastModified = now.plusMinutes(5);

        // Act
        AttendanceDTO attendanceDTO = AttendanceDTO.builder()
            .id(existingRecord.getId())
            .studentId(testStudent.getId())
            .date(LocalDateTime.now().toLocalDate())
            .status("TARDY")
            .lastModified(clientLastModified) // Newer timestamp
            .build();

        ResponseEntity<Map<String, Object>> response =
            teacherApiController.submitAttendanceBatch(List.of(attendanceDTO));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");

        Map<String, Object> responseBody = response.getBody();
        assertEquals(true, responseBody.get("success"), "Success should be true");
        assertEquals(0, responseBody.get("conflictCount"), "Should have 0 conflicts");
        assertEquals(1, responseBody.get("successCount"), "Should have 1 success");

        // Verify attendance was updated
        AttendanceRecord updatedRecord = attendanceRepository.findById(existingRecord.getId()).get();
        assertEquals(AttendanceRecord.AttendanceStatus.TARDY, updatedRecord.getStatus());

        System.out.println("✓ No conflict when client version is newer");
    }

    @Test
    public void testAttendanceConflictDetection_NewRecordNoConflict() {
        // Arrange: Create NEW attendance (no existing record)
        AttendanceDTO attendanceDTO = AttendanceDTO.builder()
            .studentId(testStudent.getId())
            .date(LocalDateTime.now().toLocalDate())
            .status("PRESENT")
            .lastModified(LocalDateTime.now())
            .build();

        // Act
        ResponseEntity<Map<String, Object>> response =
            teacherApiController.submitAttendanceBatch(List.of(attendanceDTO));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");

        Map<String, Object> responseBody = response.getBody();
        assertEquals(true, responseBody.get("success"), "Success should be true");
        assertEquals(0, responseBody.get("conflictCount"), "Should have 0 conflicts");
        assertEquals(1, responseBody.get("successCount"), "Should have 1 success");

        System.out.println("✓ New attendance record created successfully");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Assignment createTestAssignment() {
        // Create grading category first (requires Course)
        com.eduscheduler.model.domain.GradingCategory category =
            new com.eduscheduler.model.domain.GradingCategory();
        category.setName("Test Category " + System.currentTimeMillis());
        category.setWeight(100.0);
        category.setCourse(testCourse); // ← GradingCategory requires Course
        category = gradingCategoryRepository.save(category);

        // Create assignment
        Assignment assignment = new Assignment();
        assignment.setTitle("API Test Assignment " + System.currentTimeMillis());
        assignment.setDescription("Test assignment for API");
        assignment.setMaxPoints(100.0);
        assignment.setDueDate(java.time.LocalDate.now().plusDays(7));
        assignment.setCategory(category);
        assignment.setCourse(testCourse);
        return assignmentRepository.save(assignment);
    }
}
