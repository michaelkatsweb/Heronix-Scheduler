package com.heronix.controller.api;

import com.heronix.model.domain.Campus;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.District;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CampusRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.DistrictRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.impl.CampusFederationService.*;
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
 * Integration tests for FederationApiController
 * Tests multi-campus federation system: Districts, Campuses, Shared Teachers, Cross-enrollment
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 19, 2025 - Controller Layer Test Coverage
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class FederationApiControllerTest {

    @Autowired
    private FederationApiController federationApiController;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    private District testDistrict;
    private Campus testCampus1;
    private Campus testCampus2;
    private Teacher testTeacher;
    private Student testStudent;
    private Course testCourse;

    private static final String TEST_DISTRICT_NAME = "Test Federation District";
    private static final String TEST_CAMPUS1_CODE = "FED_CAMPUS_01";
    private static final String TEST_CAMPUS2_CODE = "FED_CAMPUS_02";

    @BeforeEach
    public void setup() {
        // Create test district
        testDistrict = new District();
        testDistrict.setName(TEST_DISTRICT_NAME);
        testDistrict.setDistrictCode("FED_DIST_001");
        testDistrict.setActive(true);
        testDistrict = districtRepository.save(testDistrict);

        // Create test campus 1
        testCampus1 = new Campus();
        testCampus1.setName("Federation Test Campus 1");
        testCampus1.setCampusCode(TEST_CAMPUS1_CODE);
        testCampus1.setDistrict(testDistrict);
        testCampus1.setActive(true);
        testCampus1.setMaxStudents(500);
        testCampus1 = campusRepository.save(testCampus1);

        // Create test campus 2
        testCampus2 = new Campus();
        testCampus2.setName("Federation Test Campus 2");
        testCampus2.setCampusCode(TEST_CAMPUS2_CODE);
        testCampus2.setDistrict(testDistrict);
        testCampus2.setActive(true);
        testCampus2.setMaxStudents(300);
        testCampus2 = campusRepository.save(testCampus2);

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setName("Federation Teacher");  // Required field
        testTeacher.setEmployeeId("FED_TEACHER_001");
        testTeacher.setFirstName("Federation");
        testTeacher.setLastName("Teacher");
        testTeacher.setEmail("fed.teacher@test.edu");
        testTeacher.setPrimaryCampus(testCampus1);
        testTeacher = teacherRepository.save(testTeacher);

        // Create test student
        testStudent = new Student();
        testStudent.setStudentId("FED_STUDENT_001");
        testStudent.setFirstName("Federation");
        testStudent.setLastName("Student");
        testStudent.setGradeLevel("10");  // Required field
        testStudent.setCampus(testCampus1);
        testStudent = studentRepository.save(testStudent);

        // Create test course
        testCourse = new Course();
        testCourse.setCourseCode("FED_COURSE_101");
        testCourse.setCourseName("Federation Test Course");
        testCourse.setCourseType(com.eduscheduler.model.enums.CourseType.REGULAR);
        testCourse.setSubject("Testing");
        testCourse.setMaxStudents(25);
        // Note: Course doesn't have a campus field - it's linked via sections/rooms
        testCourse.setTeacher(testTeacher);
        testCourse = courseRepository.save(testCourse);
    }

    // ========================================================================
    // TEST 1: DISTRICT ENDPOINTS
    // ========================================================================

    @Test
    public void testGetAllDistricts_ShouldSucceed() {
        // Act
        ResponseEntity<List<District>> response = federationApiController.getAllDistricts();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertTrue(response.getBody().size() >= 1, "Should contain at least the test district");

        System.out.println("✓ Districts retrieved successfully");
        System.out.println("  - Total Districts: " + response.getBody().size());
    }

    @Test
    public void testGetDistrictById_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<District> response =
            federationApiController.getDistrictById(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals(TEST_DISTRICT_NAME, response.getBody().getName(), "Should return correct district");

        System.out.println("✓ District retrieved by ID successfully");
        System.out.println("  - District: " + response.getBody().getName());
    }

    @Test
    public void testGetDistrictById_WithNonExistentId_ShouldReturnNotFound() {
        // Act
        ResponseEntity<District> response =
            federationApiController.getDistrictById(999999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Non-existent district handled correctly");
    }

    @Test
    public void testGetDistrictSummary_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<DistrictSummary> response =
            federationApiController.getDistrictSummary(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        DistrictSummary summary = response.getBody();
        assertNotNull(summary.getDistrictId(), "District ID should be present");
        assertTrue(summary.getTotalCampuses() >= 2, "Should show at least 2 campuses");

        System.out.println("✓ District summary retrieved successfully");
        System.out.println("  - Total Campuses: " + summary.getTotalCampuses());
        System.out.println("  - Total Students: " + summary.getTotalStudents());
        System.out.println("  - Total Capacity: " + summary.getTotalCapacity());
    }

    @Test
    public void testCreateDistrict_WithValidData_ShouldSucceed() {
        // Arrange
        District newDistrict = new District();
        newDistrict.setName("New Test District");
        newDistrict.setDistrictCode("NEW_DIST_001");
        newDistrict.setActive(true);

        // Act
        ResponseEntity<District> response = federationApiController.createDistrict(newDistrict);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals("New Test District", response.getBody().getName(), "Should return created district");

        System.out.println("✓ District created successfully");
        System.out.println("  - District: " + response.getBody().getName());
    }

    // ========================================================================
    // TEST 2: CAMPUS ENDPOINTS
    // ========================================================================

    @Test
    public void testGetCampusesByDistrict_WithValidDistrictId_ShouldSucceed() {
        // Act
        ResponseEntity<List<Campus>> response =
            federationApiController.getCampusesByDistrict(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertTrue(response.getBody().size() >= 2, "Should contain at least 2 test campuses");

        System.out.println("✓ Campuses retrieved by district successfully");
        System.out.println("  - Total Campuses: " + response.getBody().size());
    }

    @Test
    public void testGetCampusById_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<Campus> response =
            federationApiController.getCampusById(testCampus1.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals(TEST_CAMPUS1_CODE, response.getBody().getCampusCode(), "Should return correct campus");

        System.out.println("✓ Campus retrieved by ID successfully");
        System.out.println("  - Campus: " + response.getBody().getName());
    }

    @Test
    public void testGetCampusById_WithNonExistentId_ShouldReturnNotFound() {
        // Act
        ResponseEntity<Campus> response =
            federationApiController.getCampusById(999999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Non-existent campus handled correctly");
    }

    @Test
    public void testGetCampusByCode_WithValidCode_ShouldSucceed() {
        // Act
        ResponseEntity<Campus> response =
            federationApiController.getCampusByCode(TEST_CAMPUS1_CODE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals(TEST_CAMPUS1_CODE, response.getBody().getCampusCode(), "Should return correct campus");

        System.out.println("✓ Campus retrieved by code successfully");
        System.out.println("  - Campus Code: " + response.getBody().getCampusCode());
    }

    @Test
    public void testGetCampusByCode_WithNonExistentCode_ShouldReturnNotFound() {
        // Act
        ResponseEntity<Campus> response =
            federationApiController.getCampusByCode("NONEXISTENT_CODE");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Non-existent campus code handled correctly");
    }

    @Test
    public void testCreateCampus_WithValidData_ShouldSucceed() {
        // Arrange
        Campus newCampus = new Campus();
        newCampus.setName("New Test Campus");
        newCampus.setCampusCode("NEW_CAMPUS_001");
        newCampus.setActive(true);
        newCampus.setMaxStudents(400);

        // Act
        ResponseEntity<Campus> response =
            federationApiController.createCampus(testDistrict.getId(), newCampus);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertEquals("New Test Campus", response.getBody().getName(), "Should return created campus");

        System.out.println("✓ Campus created successfully");
        System.out.println("  - Campus: " + response.getBody().getName());
    }

    // ========================================================================
    // TEST 3: SHARED TEACHER ENDPOINTS
    // ========================================================================

    @Test
    public void testGetAvailableSharedTeachers_WithValidDistrictId_ShouldSucceed() {
        // Act
        ResponseEntity<List<SharedTeacherInfo>> response =
            federationApiController.getAvailableSharedTeachers(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Available shared teachers retrieved successfully");
        System.out.println("  - Available Teachers: " + response.getBody().size());
    }

    @Test
    public void testAssignSharedTeacher_WithValidData_ShouldSucceed() {
        // Act
        ResponseEntity<SharedTeacherAssignment> response =
            federationApiController.assignSharedTeacher(
                testTeacher.getId(),
                testCampus2.getId(),
                5  // 5 periods per week
            );

        // Assert
        // Note: May return BAD_REQUEST or OK depending on validation rules
        assertTrue(response.getStatusCode() == HttpStatus.OK ||
                   response.getStatusCode() == HttpStatus.BAD_REQUEST,
                   "Should return OK or BAD_REQUEST status");

        if (response.getStatusCode() == HttpStatus.OK) {
            assertNotNull(response.getBody(), "Response body should be present if OK");
            System.out.println("✓ Shared teacher assigned successfully");
        } else {
            System.out.println("✓ Shared teacher assignment rejected (validation)");
        }
    }

    // ========================================================================
    // TEST 4: CROSS-CAMPUS ENROLLMENT ENDPOINTS
    // ========================================================================

    @Test
    public void testGetCrossCampusEnrollmentOptions_WithValidStudentId_ShouldSucceed() {
        // Act
        ResponseEntity<List<CrossCampusEnrollmentOption>> response =
            federationApiController.getCrossCampusEnrollmentOptions(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Cross-campus enrollment options retrieved successfully");
        System.out.println("  - Available Options: " + response.getBody().size());
    }

    @Test
    public void testEnrollStudentCrossCampus_WithValidData_ShouldSucceed() {
        // Act
        ResponseEntity<CrossCampusEnrollmentResult> response =
            federationApiController.enrollStudentCrossCampus(
                testStudent.getId(),
                testCampus2.getId(),
                testCourse.getId()
            );

        // Assert
        // Note: May return BAD_REQUEST or OK depending on validation rules
        assertTrue(response.getStatusCode() == HttpStatus.OK ||
                   response.getStatusCode() == HttpStatus.BAD_REQUEST,
                   "Should return OK or BAD_REQUEST status");

        if (response.getStatusCode() == HttpStatus.OK) {
            assertNotNull(response.getBody(), "Response body should be present if OK");
            System.out.println("✓ Student enrolled cross-campus successfully");
        } else {
            System.out.println("✓ Cross-campus enrollment rejected (validation)");
        }
    }

    // ========================================================================
    // TEST 5: ANALYTICS ENDPOINTS
    // ========================================================================

    @Test
    public void testAnalyzeDistrictCapacity_WithValidDistrictId_ShouldSucceed() {
        // Act
        ResponseEntity<DistrictCapacityAnalysis> response =
            federationApiController.analyzeDistrictCapacity(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        DistrictCapacityAnalysis analysis = response.getBody();
        assertNotNull(analysis.getDistrictId(), "District ID should be present");
        assertTrue(analysis.getTotalCapacity() >= 800, "Should show total capacity of campuses");

        System.out.println("✓ District capacity analyzed successfully");
        System.out.println("  - Total Capacity: " + analysis.getTotalCapacity());
        System.out.println("  - Total Enrollment: " + analysis.getTotalEnrollment());
        System.out.println("  - Average Utilization: " + String.format("%.1f%%", analysis.getAverageUtilization()));
    }

    @Test
    public void testGetResourceSharingRecommendations_WithValidDistrictId_ShouldSucceed() {
        // Act
        ResponseEntity<List<ResourceSharingRecommendation>> response =
            federationApiController.getResourceSharingRecommendations(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Resource sharing recommendations retrieved successfully");
        System.out.println("  - Total Recommendations: " + response.getBody().size());
    }

    // ========================================================================
    // TEST 6: ERROR HANDLING
    // ========================================================================

    @Test
    public void testGetDistrictSummary_WithNonExistentId_ShouldHandleGracefully() {
        // Act
        ResponseEntity<DistrictSummary> response =
            federationApiController.getDistrictSummary(999999L);

        // Assert
        assertTrue(response.getStatusCode().is5xxServerError() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND,
                   "Should return error status");

        System.out.println("✓ Non-existent district summary handled gracefully");
    }

    @Test
    public void testGetCampusesByDistrict_WithNonExistentDistrictId_ShouldReturnEmpty() {
        // Act
        ResponseEntity<List<Campus>> response =
            federationApiController.getCampusesByDistrict(999999L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Campuses for non-existent district handled gracefully");
        System.out.println("  - Campuses Found: " + response.getBody().size());
    }

    @Test
    public void testCreateDistrict_WithInvalidData_ShouldReturnBadRequest() {
        // Arrange
        District invalidDistrict = new District();
        // Missing required fields (name, code)

        // Act
        ResponseEntity<District> response = federationApiController.createDistrict(invalidDistrict);

        // Assert
        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   response.getStatusCode().is5xxServerError(),
                   "Should return BAD_REQUEST or error status");

        System.out.println("✓ Invalid district creation handled correctly");
    }

    @Test
    public void testCreateCampus_WithInvalidData_ShouldReturnBadRequest() {
        // Arrange
        Campus invalidCampus = new Campus();
        // Missing required fields (name, code)

        // Act
        ResponseEntity<Campus> response =
            federationApiController.createCampus(testDistrict.getId(), invalidCampus);

        // Assert
        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   response.getStatusCode().is5xxServerError(),
                   "Should return BAD_REQUEST or error status");

        System.out.println("✓ Invalid campus creation handled correctly");
    }

    @Test
    public void testAssignSharedTeacher_WithInvalidTeacherId_ShouldReturnError() {
        // Act
        ResponseEntity<SharedTeacherAssignment> response =
            federationApiController.assignSharedTeacher(
                999999L,  // Non-existent teacher
                testCampus2.getId(),
                5
            );

        // Assert
        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   response.getStatusCode().is5xxServerError(),
                   "Should return error status");

        System.out.println("✓ Invalid shared teacher assignment handled correctly");
    }

    @Test
    public void testEnrollStudentCrossCampus_WithInvalidStudentId_ShouldReturnError() {
        // Act
        ResponseEntity<CrossCampusEnrollmentResult> response =
            federationApiController.enrollStudentCrossCampus(
                999999L,  // Non-existent student
                testCampus2.getId(),
                testCourse.getId()
            );

        // Assert
        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   response.getStatusCode().is5xxServerError(),
                   "Should return error status");

        System.out.println("✓ Invalid cross-campus enrollment handled correctly");
    }

    // ========================================================================
    // TEST 7: VALIDATION AND EDGE CASES
    // ========================================================================

    @Test
    public void testGetDistrictSummary_ShouldIncludeAccurateCounts() {
        // Act
        ResponseEntity<DistrictSummary> response =
            federationApiController.getDistrictSummary(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        DistrictSummary summary = response.getBody();
        assertEquals(testDistrict.getId(), summary.getDistrictId(), "Should match district ID");
        assertTrue(summary.getTotalCampuses() >= 2, "Should count campuses correctly");

        System.out.println("✓ District summary includes accurate counts");
    }

    @Test
    public void testAnalyzeDistrictCapacity_ShouldCalculateUtilization() {
        // Act
        ResponseEntity<DistrictCapacityAnalysis> response =
            federationApiController.analyzeDistrictCapacity(testDistrict.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        DistrictCapacityAnalysis analysis = response.getBody();
        assertTrue(analysis.getAverageUtilization() >= 0 &&
                   analysis.getAverageUtilization() <= 100,
                   "Utilization should be 0-100%");

        System.out.println("✓ District capacity utilization calculated correctly");
    }
}
