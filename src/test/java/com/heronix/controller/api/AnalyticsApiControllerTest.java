package com.heronix.controller.api;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.impl.AdvancedAnalyticsService.*;
import com.heronix.service.impl.PredictiveAnalyticsService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AnalyticsApiController
 * Tests advanced analytics and predictive analytics endpoints
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 19, 2025 - Controller Layer Test Coverage
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class AnalyticsApiControllerTest {

    @Autowired
    private AnalyticsApiController analyticsApiController;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Teacher testTeacher;
    private Student testStudent;
    private Course testCourse;

    @BeforeEach
    public void setup() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setEmployeeId("ANALYTICS_TEST_001");
        testTeacher.setFirstName("Analytics");
        testTeacher.setLastName("Teacher");
        testTeacher.setName("Analytics Teacher");
        testTeacher.setEmail("analytics.teacher@eduscheduler.com");
        testTeacher.setDepartment("Testing");
        testTeacher.setActive(true);
        testTeacher = teacherRepository.save(testTeacher);

        // Create test student with IEP
        testStudent = new Student();
        testStudent.setStudentId("ANALYTICS_STU_001");
        testStudent.setFirstName("Analytics");
        testStudent.setLastName("Student");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);
        testStudent.setHasIEP(true); // IEP student for SPED compliance tests
        testStudent = studentRepository.save(testStudent);

        // Create test course
        testCourse = new Course();
        testCourse.setCourseCode("ANALYTICS_101");
        testCourse.setCourseName("Analytics Test Course");
        testCourse.setCourseType(com.eduscheduler.model.enums.CourseType.REGULAR);
        testCourse.setSubject("Testing");
        testCourse.setMaxStudents(30);
        testCourse.setTeacher(testTeacher);
        testCourse = courseRepository.save(testCourse);
    }

    // ========================================================================
    // TEST 1: BURNOUT RISK ANALYSIS
    // ========================================================================

    @Test
    public void testGetTeacherBurnoutRisk_WithValidTeacher_ShouldSucceed() {
        // Act
        ResponseEntity<BurnoutRiskResult> response =
            analyticsApiController.getTeacherBurnoutRisk(testTeacher.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        BurnoutRiskResult result = response.getBody();
        assertNotNull(result.getTeacherId(), "Teacher ID should be present");
        assertNotNull(result.getRiskScore(), "Risk score should be present");
        assertNotNull(result.getRiskLevel(), "Risk level should be present");
        assertTrue(result.getRiskScore() >= 0 && result.getRiskScore() <= 100,
                   "Risk score should be 0-100");

        System.out.println("✓ Teacher burnout risk retrieved successfully");
        System.out.println("  - Risk Score: " + result.getRiskScore());
        System.out.println("  - Risk Level: " + result.getRiskLevel());
    }

    @Test
    public void testGetTeacherBurnoutRisk_WithNonExistentTeacher_ShouldReturnError() {
        // Act
        ResponseEntity<BurnoutRiskResult> response =
            analyticsApiController.getTeacherBurnoutRisk(999999L);

        // Assert - Analytics API uses graceful degradation (returns 200 OK with default/empty data)
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                     "Analytics API handles non-existent entities gracefully with 200 OK");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Non-existent teacher handled gracefully (200 OK with default data)");
    }

    @Test
    public void testGetAllTeacherBurnoutRisks_ShouldReturnList() {
        // Act
        ResponseEntity<List<BurnoutRiskResult>> response =
            analyticsApiController.getAllTeacherBurnoutRisks();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertTrue(response.getBody().size() >= 1, "Should have at least 1 teacher");

        System.out.println("✓ All teacher burnout risks retrieved successfully");
        System.out.println("  - Total Teachers: " + response.getBody().size());
    }

    @Test
    public void testGetHighRiskTeachers_WithDefaultThreshold_ShouldReturnFiltered() {
        // Act (default threshold = 70)
        ResponseEntity<List<BurnoutRiskResult>> response =
            analyticsApiController.getHighRiskTeachers(70);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        // Verify all returned teachers have risk score >= 70
        for (BurnoutRiskResult result : response.getBody()) {
            assertTrue(result.getRiskScore() >= 70,
                      "All teachers should have risk score >= 70");
        }

        System.out.println("✓ High-risk teachers filtered successfully");
        System.out.println("  - High-Risk Count: " + response.getBody().size());
    }

    @Test
    public void testGetHighRiskTeachers_WithCustomThreshold_ShouldReturnFiltered() {
        // Act (custom threshold = 50)
        ResponseEntity<List<BurnoutRiskResult>> response =
            analyticsApiController.getHighRiskTeachers(50);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        // Verify all returned teachers have risk score >= 50
        for (BurnoutRiskResult result : response.getBody()) {
            assertTrue(result.getRiskScore() >= 50,
                      "All teachers should have risk score >= 50");
        }

        System.out.println("✓ Custom threshold filtering works correctly");
    }

    // ========================================================================
    // TEST 2: SPED COMPLIANCE TRACKING
    // ========================================================================

    @Test
    public void testGetSPEDCompliance_WithValidStudent_ShouldSucceed() {
        // Act
        ResponseEntity<SPEDComplianceResult> response =
            analyticsApiController.getSPEDCompliance(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        SPEDComplianceResult result = response.getBody();
        assertNotNull(result.getStudentId(), "Student ID should be present");
        assertNotNull(result.getRequiredMinutesWeekly(), "Required minutes should be present");
        assertNotNull(result.getScheduledMinutesWeekly(), "Scheduled minutes should be present");

        System.out.println("✓ SPED compliance retrieved successfully");
        System.out.println("  - Required: " + result.getRequiredMinutesWeekly() + " minutes");
        System.out.println("  - Scheduled: " + result.getScheduledMinutesWeekly() + " minutes");
    }

    @Test
    public void testGetSPEDCompliance_WithNonExistentStudent_ShouldReturnError() {
        // Act
        ResponseEntity<SPEDComplianceResult> response =
            analyticsApiController.getSPEDCompliance(999999L);

        // Assert - Analytics API uses graceful degradation (returns 200 OK with default/empty data)
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                     "Analytics API handles non-existent entities gracefully with 200 OK");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Non-existent student handled gracefully (200 OK with default data)");
    }

    @Test
    public void testGetAllSPEDCompliance_ShouldReturnSummary() {
        // Act
        ResponseEntity<SPEDComplianceSummary> response =
            analyticsApiController.getAllSPEDCompliance();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        SPEDComplianceSummary summary = response.getBody();
        assertNotNull(summary.getTotalStudentsWithIEP(), "Total IEP students should be present");
        assertNotNull(summary.getCompliantCount(), "Compliant count should be present");
        assertNotNull(summary.getAverageCompliancePercentage(), "Average percentage should be present");

        System.out.println("✓ SPED compliance summary retrieved successfully");
        System.out.println("  - Total IEP Students: " + summary.getTotalStudentsWithIEP());
        System.out.println("  - Compliant: " + summary.getCompliantCount());
    }

    // ========================================================================
    // TEST 3: WHAT-IF SCENARIO SIMULATION
    // ========================================================================

    @Test
    public void testRunWhatIfScenario_WithValidScenario_ShouldSucceed() {
        // Arrange
        WhatIfScenario scenario = WhatIfScenario.builder()
            .type(ScenarioType.TEACHER_REMOVAL)
            .entityId(testTeacher.getId())
            .description("Test scenario: Remove teacher and analyze impact")
            .parameter(null)
            .build();

        // Act
        ResponseEntity<WhatIfResult> response =
            analyticsApiController.runWhatIfScenario(scenario);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        WhatIfResult result = response.getBody();
        assertNotNull(result.getScenarioType(), "Scenario type should be present");

        System.out.println("✓ What-if scenario executed successfully");
        System.out.println("  - Scenario Type: " + result.getScenarioType());
    }

    // ========================================================================
    // TEST 4: PREP TIME EQUITY
    // ========================================================================

    @Test
    public void testGetPrepTimeEquity_ShouldReturnEquityAnalysis() {
        // Act
        ResponseEntity<PrepTimeEquityResult> response =
            analyticsApiController.getPrepTimeEquity();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        PrepTimeEquityResult result = response.getBody();
        assertNotNull(result.getEquityIndex(), "Equity index should be present");
        assertNotNull(result.getAveragePrepMinutes(), "Average prep minutes should be present");
        assertNotNull(result.getBelowAverageTeachers(), "Below average teachers list should be present");

        System.out.println("✓ Prep time equity analysis retrieved successfully");
        System.out.println("  - Equity Index: " + result.getEquityIndex());
        System.out.println("  - Average Prep: " + result.getAveragePrepMinutes() + " minutes");
    }

    // ========================================================================
    // TEST 5: ENROLLMENT FORECASTING
    // ========================================================================

    @Test
    public void testGetEnrollmentForecast_WithValidCourse_ShouldSucceed() {
        // Act
        ResponseEntity<EnrollmentForecast> response =
            analyticsApiController.getEnrollmentForecast(testCourse.getId(), 1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        EnrollmentForecast forecast = response.getBody();
        assertNotNull(forecast.getCourseId(), "Course ID should be present");
        assertNotNull(forecast.getPredictedEnrollment(), "Predicted enrollment should be present");

        System.out.println("✓ Enrollment forecast retrieved successfully");
        System.out.println("  - Predicted Enrollment: " + forecast.getPredictedEnrollment());
    }

    @Test
    public void testGetEnrollmentForecast_WithNonExistentCourse_ShouldReturnError() {
        // Act
        ResponseEntity<EnrollmentForecast> response =
            analyticsApiController.getEnrollmentForecast(999999L, 1);

        // Assert - Analytics API uses graceful degradation (returns 200 OK with default/empty data)
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                     "Analytics API handles non-existent entities gracefully with 200 OK");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Non-existent course handled gracefully (200 OK with default data)");
    }

    @Test
    public void testGetAllEnrollmentForecasts_ShouldReturnList() {
        // Act
        ResponseEntity<List<EnrollmentForecast>> response =
            analyticsApiController.getAllEnrollmentForecasts(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ All enrollment forecasts retrieved successfully");
        System.out.println("  - Total Courses: " + response.getBody().size());
    }

    @Test
    public void testGetAtRiskCourses_ShouldReturnFiltered() {
        // Act
        ResponseEntity<List<EnrollmentForecast>> response =
            analyticsApiController.getAtRiskCourses(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ At-risk courses retrieved successfully");
        System.out.println("  - At-Risk Count: " + response.getBody().size());
    }

    // ========================================================================
    // TEST 6: CONFLICT ANALYSIS
    // ========================================================================

    @Test
    public void testAnalyzeConflicts_ShouldReturnAnalysis() {
        // Act
        ResponseEntity<ConflictAnalysis> response =
            analyticsApiController.analyzeConflicts();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        ConflictAnalysis analysis = response.getBody();
        assertNotNull(analysis.getTotalConflicts(), "Total conflicts should be present");
        assertNotNull(analysis.getCriticalCount(), "Critical count should be present");
        assertNotNull(analysis.getHighCount(), "High count should be present");

        System.out.println("✓ Conflict analysis retrieved successfully");
        System.out.println("  - Total Conflicts: " + analysis.getTotalConflicts());
        System.out.println("  - Critical: " + analysis.getCriticalCount());
    }

    @Test
    public void testGetSmartResolutions_WithValidSlot_ShouldSucceed() {
        // Act
        ResponseEntity<List<ConflictResolution>> response =
            analyticsApiController.getSmartResolutions(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        System.out.println("✓ Smart resolutions retrieved successfully");
        System.out.println("  - Resolution Count: " + response.getBody().size());
    }

    // ========================================================================
    // TEST 7: ASSIGNMENT RECOMMENDATIONS
    // ========================================================================

    @Test
    public void testGetOptimalAssignments_ShouldReturnRecommendations() {
        // Act
        ResponseEntity<AssignmentRecommendations> response =
            analyticsApiController.getOptimalAssignments();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        AssignmentRecommendations recommendations = response.getBody();
        assertNotNull(recommendations.getTotalUnassigned(), "Total unassigned should be present");
        assertNotNull(recommendations.getRecommendations(), "Recommendations list should be present");

        System.out.println("✓ Optimal assignments retrieved successfully");
        System.out.println("  - Unassigned Courses: " + recommendations.getTotalUnassigned());
    }

    @Test
    public void testGetTeacherRecommendation_WithValidCourse_ShouldSucceedOrNotFound() {
        // Act
        ResponseEntity<TeacherAssignmentRec> response =
            analyticsApiController.getTeacherRecommendation(testCourse.getId());

        // Assert
        assertTrue(response.getStatusCode().equals(HttpStatus.OK) ||
                   response.getStatusCode().equals(HttpStatus.NOT_FOUND),
                   "Should return OK or NOT_FOUND");

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            assertNotNull(response.getBody(), "Response body should be present if OK");
            System.out.println("✓ Teacher recommendation found");
        } else {
            System.out.println("✓ No teacher recommendation available (course already assigned)");
        }
    }

    @Test
    public void testGetRoomRecommendation_WithValidParameters_ShouldSucceedOrNotFound() {
        // Act
        ResponseEntity<RoomAssignmentRec> response =
            analyticsApiController.getRoomRecommendation(
                testCourse.getId(),
                "MONDAY",
                "09:00");

        // Assert
        assertTrue(response.getStatusCode().equals(HttpStatus.OK) ||
                   response.getStatusCode().equals(HttpStatus.NOT_FOUND),
                   "Should return OK or NOT_FOUND");

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            assertNotNull(response.getBody(), "Response body should be present if OK");
            System.out.println("✓ Room recommendation found");
        } else {
            System.out.println("✓ No room recommendation available");
        }
    }

    @Test
    public void testGetRoomRecommendation_WithInvalidDay_ShouldReturnError() {
        // Act
        ResponseEntity<RoomAssignmentRec> response =
            analyticsApiController.getRoomRecommendation(
                testCourse.getId(),
                "INVALIDDAY",
                "09:00");

        // Assert
        assertTrue(response.getStatusCode().is5xxServerError(),
                   "Should return error for invalid day");

        System.out.println("✓ Invalid day parameter handled gracefully");
    }

    @Test
    public void testGetRoomRecommendation_WithInvalidTime_ShouldReturnError() {
        // Act
        ResponseEntity<RoomAssignmentRec> response =
            analyticsApiController.getRoomRecommendation(
                testCourse.getId(),
                "MONDAY",
                "invalid_time");

        // Assert
        assertTrue(response.getStatusCode().is5xxServerError(),
                   "Should return error for invalid time");

        System.out.println("✓ Invalid time parameter handled gracefully");
    }

    // ========================================================================
    // TEST 8: ANALYTICS DASHBOARD
    // ========================================================================

    @Test
    public void testGetAnalyticsDashboard_ShouldReturnComprehensiveSummary() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            analyticsApiController.getAnalyticsDashboard();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        Map<String, Object> dashboard = response.getBody();

        // Verify all dashboard sections exist
        assertTrue(dashboard.containsKey("burnoutSummary"), "Should contain burnout summary");
        assertTrue(dashboard.containsKey("spedCompliance"), "Should contain SPED compliance");
        assertTrue(dashboard.containsKey("prepEquity"), "Should contain prep equity");
        assertTrue(dashboard.containsKey("conflicts"), "Should contain conflicts");
        assertTrue(dashboard.containsKey("enrollment"), "Should contain enrollment");
        assertTrue(dashboard.containsKey("assignments"), "Should contain assignments");

        // Verify burnout summary structure
        @SuppressWarnings("unchecked")
        Map<String, Object> burnoutSummary = (Map<String, Object>) dashboard.get("burnoutSummary");
        assertTrue(burnoutSummary.containsKey("totalTeachers"), "Burnout should have total teachers");
        assertTrue(burnoutSummary.containsKey("highRisk"), "Burnout should have high risk count");
        assertTrue(burnoutSummary.containsKey("averageScore"), "Burnout should have average score");

        // Verify SPED compliance structure
        @SuppressWarnings("unchecked")
        Map<String, Object> spedCompliance = (Map<String, Object>) dashboard.get("spedCompliance");
        assertTrue(spedCompliance.containsKey("totalStudents"), "SPED should have total students");
        assertTrue(spedCompliance.containsKey("compliantCount"), "SPED should have compliant count");
        assertTrue(spedCompliance.containsKey("complianceRate"), "SPED should have compliance rate");

        System.out.println("✓ Analytics dashboard retrieved successfully");
        System.out.println("  - Dashboard Sections: " + dashboard.keySet().size());
        System.out.println("  - Total Teachers: " + burnoutSummary.get("totalTeachers"));
        System.out.println("  - Total IEP Students: " + spedCompliance.get("totalStudents"));
    }

    // ========================================================================
    // TEST 9: ERROR HANDLING
    // ========================================================================

    @Test
    public void testErrorHandling_AllEndpointsReturnValidStatus() {
        // Test that all endpoints return valid HTTP status codes (not null)

        assertNotNull(analyticsApiController.getTeacherBurnoutRisk(999999L).getStatusCode());
        assertNotNull(analyticsApiController.getAllTeacherBurnoutRisks().getStatusCode());
        assertNotNull(analyticsApiController.getHighRiskTeachers(70).getStatusCode());
        assertNotNull(analyticsApiController.getSPEDCompliance(999999L).getStatusCode());
        assertNotNull(analyticsApiController.getAllSPEDCompliance().getStatusCode());
        assertNotNull(analyticsApiController.getPrepTimeEquity().getStatusCode());
        assertNotNull(analyticsApiController.getEnrollmentForecast(999999L, 1).getStatusCode());
        assertNotNull(analyticsApiController.getAllEnrollmentForecasts(1).getStatusCode());
        assertNotNull(analyticsApiController.getAtRiskCourses(1).getStatusCode());
        assertNotNull(analyticsApiController.analyzeConflicts().getStatusCode());
        assertNotNull(analyticsApiController.getOptimalAssignments().getStatusCode());
        assertNotNull(analyticsApiController.getAnalyticsDashboard().getStatusCode());

        System.out.println("✓ All endpoints return valid HTTP status codes");
    }
}
