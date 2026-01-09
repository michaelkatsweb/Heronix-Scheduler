package com.heronix.service.impl;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseRequest;
import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Student;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Test Suite for EnrollmentForecastingServiceImpl
 * Service #34 in systematic testing plan
 *
 * Tests enrollment forecasting with statistical algorithms
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentForecastingServiceImplTest {

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @Mock(lenient = true)
    private CourseRequestRepository courseRequestRepository;

    @Mock(lenient = true)
    private CourseSectionRepository courseSectionRepository;

    @InjectMocks
    private EnrollmentForecastingServiceImpl service;

    private Student testStudent1;
    private Student testStudent2;
    private Course testCourse;
    private CourseRequest testRequest;
    private CourseSection testSection;

    @BeforeEach
    void setUp() {
        // Setup test students
        testStudent1 = new Student();
        testStudent1.setId(1L);
        testStudent1.setStudentId("S001");
        testStudent1.setGradeLevel("9");
        testStudent1.setActive(true);

        testStudent2 = new Student();
        testStudent2.setId(2L);
        testStudent2.setStudentId("S002");
        testStudent2.setGradeLevel("10");
        testStudent2.setActive(true);

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");
        testCourse.setMaxStudents(25);

        // Setup test course request
        testRequest = new CourseRequest();
        testRequest.setId(1L);
        testRequest.setCourse(testCourse);
        testRequest.setRequestYear(2024);

        // Setup test course section
        testSection = new CourseSection();
        testSection.setId(1L);
        testSection.setCourse(testCourse);
        testSection.setMaxEnrollment(25);

        // Configure repository mocks
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
    }

    // ========================================================================
    // FORECAST TOTAL ENROLLMENT TESTS
    // ========================================================================

    @Test
    void testForecastTotalEnrollment_WithFutureYear_ShouldApplyGrowth() {
        when(studentRepository.count()).thenReturn(1000L);

        int currentYear = LocalDate.now().getYear();
        int result = service.forecastTotalEnrollment(currentYear + 5);

        assertTrue(result > 1000); // Should show growth
        assertTrue(result < 1200); // Reasonable upper bound with 2% growth
    }

    @Test
    void testForecastTotalEnrollment_WithCurrentYear_ShouldReturnCurrent() {
        when(studentRepository.count()).thenReturn(1000L);

        int currentYear = LocalDate.now().getYear();
        int result = service.forecastTotalEnrollment(currentYear);

        assertEquals(1000, result);
    }

    @Test
    void testForecastTotalEnrollment_WithPastYear_ShouldReturnCurrent() {
        when(studentRepository.count()).thenReturn(1000L);

        int currentYear = LocalDate.now().getYear();
        int result = service.forecastTotalEnrollment(currentYear - 1);

        assertEquals(1000, result);
    }

    @Test
    void testForecastTotalEnrollment_WithZeroEnrollment_ShouldReturnZero() {
        when(studentRepository.count()).thenReturn(0L);

        int currentYear = LocalDate.now().getYear();
        int result = service.forecastTotalEnrollment(currentYear + 5);

        assertEquals(0, result);
    }

    // ========================================================================
    // FORECAST ENROLLMENT BY GRADE TESTS
    // ========================================================================

    @Test
    void testForecastEnrollmentByGrade_ShouldReturnGradeMap() {
        List<Student> students = Arrays.asList(testStudent1, testStudent2);
        when(studentRepository.findAll()).thenReturn(students);

        int currentYear = LocalDate.now().getYear();
        Map<String, Integer> result = service.forecastEnrollmentByGrade(currentYear);

        assertNotNull(result);
        assertTrue(result.containsKey("9"));
        assertTrue(result.containsKey("10"));
        assertTrue(result.containsKey("11"));
        assertTrue(result.containsKey("12"));
    }

    @Test
    void testForecastEnrollmentByGrade_WithNullStudents_ShouldFilterThem() {
        List<Student> students = Arrays.asList(testStudent1, null, testStudent2);
        when(studentRepository.findAll()).thenReturn(students);

        int currentYear = LocalDate.now().getYear();
        Map<String, Integer> result = service.forecastEnrollmentByGrade(currentYear);

        assertNotNull(result);
        assertEquals(4, result.size()); // All 4 grades present
    }

    @Test
    void testForecastEnrollmentByGrade_WithNullGradeLevels_ShouldFilterThem() {
        testStudent1.setGradeLevel(null);

        List<Student> students = Arrays.asList(testStudent1, testStudent2);
        when(studentRepository.findAll()).thenReturn(students);

        int currentYear = LocalDate.now().getYear();
        Map<String, Integer> result = service.forecastEnrollmentByGrade(currentYear);

        assertNotNull(result);
        // Grade 10 should have 1 student (with attrition applied)
        assertNotNull(result.get("10"));
    }

    @Test
    void testForecastEnrollmentByGrade_WithInactiveStudents_ShouldFilterThem() {
        testStudent1.setActive(false);

        List<Student> students = Arrays.asList(testStudent1, testStudent2);
        when(studentRepository.findAll()).thenReturn(students);

        int currentYear = LocalDate.now().getYear();
        Map<String, Integer> result = service.forecastEnrollmentByGrade(currentYear);

        assertNotNull(result);
        // Only testStudent2 should be counted
        assertNotNull(result.get("10"));
    }

    // ========================================================================
    // FORECAST COURSE DEMAND TESTS
    // ========================================================================

    @Test
    void testForecastCourseDemand_ShouldReturnDemandMap() {
        Course course2 = new Course();
        course2.setId(2L);
        course2.setCourseCode("ENG101");

        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse, course2));
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());

        int currentYear = LocalDate.now().getYear();
        Map<Long, Integer> result = service.forecastCourseDemand(currentYear);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(1L));
        assertTrue(result.containsKey(2L));
    }

    @Test
    void testForecastCourseDemand_WithNullCourses_ShouldFilterThem() {
        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse, null));
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());

        int currentYear = LocalDate.now().getYear();
        Map<Long, Integer> result = service.forecastCourseDemand(currentYear);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testForecastCourseDemand_WithNullCourseIds_ShouldFilterThem() {
        Course courseNullId = new Course();
        courseNullId.setId(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse, courseNullId));
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());

        int currentYear = LocalDate.now().getYear();
        Map<Long, Integer> result = service.forecastCourseDemand(currentYear);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================================================
    // RECOMMEND SECTION COUNT TESTS
    // ========================================================================

    @Test
    void testRecommendSectionCount_WithLowDemand_ShouldReturnOne() {
        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(testRequest));

        int currentYear = LocalDate.now().getYear();
        int result = service.recommendSectionCount(1L, currentYear);

        assertEquals(1, result);
    }

    @Test
    void testRecommendSectionCount_WithHighDemand_ShouldReturnMultiple() {
        // Create 60 requests (should need 3 sections at 25 capacity each)
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            CourseRequest req = new CourseRequest();
            req.setRequestYear(2024);
            requests.add(req);
        }

        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);

        int currentYear = LocalDate.now().getYear();
        int result = service.recommendSectionCount(1L, currentYear);

        assertTrue(result >= 2); // At least 2 sections needed
    }

    @Test
    void testRecommendSectionCount_WithZeroDemand_ShouldReturnZero() {
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());

        int currentYear = LocalDate.now().getYear();
        int result = service.recommendSectionCount(1L, currentYear);

        assertEquals(0, result);
    }

    @Test
    void testRecommendSectionCount_WithNonExistentCourse_ShouldUseDefault() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());
        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(testRequest));

        int currentYear = LocalDate.now().getYear();
        int result = service.recommendSectionCount(999L, currentYear);

        assertTrue(result >= 0);
    }

    // ========================================================================
    // GET COURSE TREND TESTS
    // ========================================================================

    @Test
    void testGetCourseTrend_WithGrowth_ShouldReturnGrowing() {
        CourseRequest req2023 = new CourseRequest();
        req2023.setRequestYear(2023);

        CourseRequest req2024_1 = new CourseRequest();
        req2024_1.setRequestYear(2024);

        CourseRequest req2024_2 = new CourseRequest();
        req2024_2.setRequestYear(2024);

        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(req2023, req2024_1, req2024_2));

        String result = service.getCourseTrend(1L);

        assertNotNull(result);
        assertTrue(result.contains("Growing") || result.contains("Stable"));
    }

    @Test
    void testGetCourseTrend_WithDecline_ShouldReturnDeclining() {
        List<CourseRequest> requests2023 = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CourseRequest req = new CourseRequest();
            req.setRequestYear(2023);
            requests2023.add(req);
        }

        CourseRequest req2024 = new CourseRequest();
        req2024.setRequestYear(2024);

        List<CourseRequest> allRequests = new ArrayList<>();
        allRequests.addAll(requests2023);
        allRequests.add(req2024);

        when(courseRequestRepository.findByCourse(any())).thenReturn(allRequests);

        String result = service.getCourseTrend(1L);

        assertNotNull(result);
        assertTrue(result.contains("Declining"));
    }

    @Test
    void testGetCourseTrend_WithStableEnrollment_ShouldReturnStable() {
        CourseRequest req2023 = new CourseRequest();
        req2023.setRequestYear(2023);

        CourseRequest req2024 = new CourseRequest();
        req2024.setRequestYear(2024);

        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(req2023, req2024));

        String result = service.getCourseTrend(1L);

        assertNotNull(result);
        assertTrue(result.contains("Stable"));
    }

    @Test
    void testGetCourseTrend_WithNoData_ShouldReturnInsufficientData() {
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());

        String result = service.getCourseTrend(1L);

        assertEquals("Insufficient Data", result);
    }

    @Test
    void testGetCourseTrend_WithSingleYear_ShouldReturnStable() {
        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(testRequest));

        String result = service.getCourseTrend(1L);

        assertEquals("Stable", result);
    }

    @Test
    void testGetCourseTrend_WithNullRequests_ShouldFilterThem() {
        CourseRequest req2023 = new CourseRequest();
        req2023.setRequestYear(2023);

        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(req2023, null));

        String result = service.getCourseTrend(1L);

        assertNotNull(result);
    }

    @Test
    void testGetCourseTrend_WithNullRequestYears_ShouldFilterThem() {
        CourseRequest reqNullYear = new CourseRequest();
        reqNullYear.setRequestYear(null);

        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(testRequest, reqNullYear));

        String result = service.getCourseTrend(1L);

        assertNotNull(result);
    }

    // ========================================================================
    // CALCULATE GROWTH RATE TESTS
    // ========================================================================

    @Test
    void testCalculateGrowthRate_WithGrowth_ShouldReturnPositive() {
        CourseRequest req2023 = new CourseRequest();
        req2023.setRequestYear(2023);

        CourseRequest req2024_1 = new CourseRequest();
        req2024_1.setRequestYear(2024);

        CourseRequest req2024_2 = new CourseRequest();
        req2024_2.setRequestYear(2024);

        when(courseRequestRepository.findAll())
                .thenReturn(Arrays.asList(req2023, req2024_1, req2024_2));

        double result = service.calculateGrowthRate(2023, 2024);

        assertTrue(result > 0); // Should be 100% (from 1 to 2)
    }

    @Test
    void testCalculateGrowthRate_WithDecline_ShouldReturnNegative() {
        CourseRequest req2023_1 = new CourseRequest();
        req2023_1.setRequestYear(2023);

        CourseRequest req2023_2 = new CourseRequest();
        req2023_2.setRequestYear(2023);

        CourseRequest req2024 = new CourseRequest();
        req2024.setRequestYear(2024);

        when(courseRequestRepository.findAll())
                .thenReturn(Arrays.asList(req2023_1, req2023_2, req2024));

        double result = service.calculateGrowthRate(2023, 2024);

        assertTrue(result < 0); // Should be -50% (from 2 to 1)
    }

    @Test
    void testCalculateGrowthRate_WithNoDataForYear1_ShouldReturnZero() {
        when(courseRequestRepository.findAll()).thenReturn(new ArrayList<>());

        double result = service.calculateGrowthRate(2023, 2024);

        assertEquals(0.0, result);
    }

    @Test
    void testCalculateGrowthRate_WithNoDataForYear2_ShouldReturnZero() {
        CourseRequest req2023 = new CourseRequest();
        req2023.setRequestYear(2023);

        when(courseRequestRepository.findAll()).thenReturn(Arrays.asList(req2023));

        double result = service.calculateGrowthRate(2023, 2024);

        assertEquals(0.0, result);
    }

    // ========================================================================
    // FORECASTING REPORT TESTS
    // ========================================================================

    @Test
    void testGetForecastingReport_ShouldIncludeAllMetrics() {
        when(studentRepository.count()).thenReturn(1000L);
        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent1, testStudent2));
        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());
        when(courseSectionRepository.findAll()).thenReturn(new ArrayList<>());

        int currentYear = LocalDate.now().getYear();
        Map<String, Object> result = service.getForecastingReport(currentYear);

        assertNotNull(result);
        assertTrue(result.containsKey("totalEnrollment"));
        assertTrue(result.containsKey("enrollmentByGrade"));
        assertTrue(result.containsKey("courseDemand"));
        assertTrue(result.containsKey("growingCourses"));
        assertTrue(result.containsKey("capacityWarnings"));
        assertTrue(result.containsKey("hasAdequateCapacity"));
    }

    // ========================================================================
    // ADEQUATE CAPACITY TESTS
    // ========================================================================

    @Test
    void testHasAdequateCapacity_WithSufficientCapacity_ShouldReturnTrue() {
        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        int currentYear = LocalDate.now().getYear();
        boolean result = service.hasAdequateCapacity(currentYear);

        assertTrue(result);
    }

    @Test
    void testHasAdequateCapacity_WithInsufficientCapacity_ShouldReturnFalse() {
        // Create many requests to exceed capacity
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            CourseRequest req = new CourseRequest();
            req.setRequestYear(2024);
            requests.add(req);
        }

        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        int currentYear = LocalDate.now().getYear();
        boolean result = service.hasAdequateCapacity(currentYear);

        assertFalse(result); // 50 demand > 25 capacity * 0.9 threshold
    }

    // ========================================================================
    // CAPACITY WARNINGS TESTS
    // ========================================================================

    @Test
    void testGetCapacityWarnings_WithOverCapacity_ShouldReturnWarning() {
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            CourseRequest req = new CourseRequest();
            req.setRequestYear(2024);
            requests.add(req);
        }

        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        int currentYear = LocalDate.now().getYear();
        Map<String, String> result = service.getCapacityWarnings(currentYear);

        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.values().stream().anyMatch(w -> w.contains("OVER CAPACITY")));
    }

    @Test
    void testGetCapacityWarnings_WithNearCapacity_ShouldReturnWarning() {
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 23; i++) { // 23/25 = 92% > 90% threshold
            CourseRequest req = new CourseRequest();
            req.setRequestYear(2024);
            requests.add(req);
        }

        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        int currentYear = LocalDate.now().getYear();
        Map<String, String> result = service.getCapacityWarnings(currentYear);

        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.values().stream().anyMatch(w -> w.contains("NEAR CAPACITY")));
    }

    @Test
    void testGetCapacityWarnings_WithUnderUtilization_ShouldReturnWarning() {
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // 5/25 = 20% < 50% threshold
            CourseRequest req = new CourseRequest();
            req.setRequestYear(2024);
            requests.add(req);
        }

        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        int currentYear = LocalDate.now().getYear();
        Map<String, String> result = service.getCapacityWarnings(currentYear);

        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.values().stream().anyMatch(w -> w.contains("UNDER-UTILIZED")));
    }

    @Test
    void testGetCapacityWarnings_WithNullCourse_ShouldSkipIt() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());
        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(Arrays.asList(testRequest));
        when(courseSectionRepository.findAll()).thenReturn(new ArrayList<>());

        int currentYear = LocalDate.now().getYear();
        Map<String, String> result = service.getCapacityWarnings(currentYear);

        assertNotNull(result);
        // Should handle null course gracefully
    }

    // ========================================================================
    // LINEAR REGRESSION TESTS
    // ========================================================================

    @Test
    void testForecastWithLinearRegression_WithNoData_ShouldReturnZero() {
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());

        int result = service.forecastWithLinearRegression(1L, 2025);

        assertEquals(0, result);
    }

    @Test
    void testForecastWithLinearRegression_WithSingleYear_ShouldReturnThatCount() {
        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(testRequest));

        int result = service.forecastWithLinearRegression(1L, 2025);

        assertEquals(1, result);
    }

    @Test
    void testForecastWithLinearRegression_WithGrowthTrend_ShouldProject() {
        CourseRequest req2023 = new CourseRequest();
        req2023.setRequestYear(2023);

        CourseRequest req2024_1 = new CourseRequest();
        req2024_1.setRequestYear(2024);

        CourseRequest req2024_2 = new CourseRequest();
        req2024_2.setRequestYear(2024);

        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(req2023, req2024_1, req2024_2));

        int result = service.forecastWithLinearRegression(1L, 2025);

        assertTrue(result >= 0);
        assertTrue(result < 100); // Reasonable upper bound
    }

    @Test
    void testForecastWithLinearRegression_ShouldNotReturnNegative() {
        // Create declining trend
        CourseRequest req2023_1 = new CourseRequest();
        req2023_1.setRequestYear(2023);

        CourseRequest req2023_2 = new CourseRequest();
        req2023_2.setRequestYear(2023);

        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(req2023_1, req2023_2));

        int result = service.forecastWithLinearRegression(1L, 2020); // Far past year

        assertTrue(result >= 0); // Should never be negative
    }

    // ========================================================================
    // MOVING AVERAGE TESTS
    // ========================================================================

    @Test
    void testForecastWithMovingAverage_WithNoData_ShouldReturnZero() {
        when(courseRequestRepository.findByCourse(any())).thenReturn(new ArrayList<>());

        int result = service.forecastWithMovingAverage(1L, 2025, 3);

        assertEquals(0, result);
    }

    @Test
    void testForecastWithMovingAverage_WithInsufficientData_ShouldUseAverage() {
        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(testRequest));

        int result = service.forecastWithMovingAverage(1L, 2025, 3);

        assertEquals(1, result);
    }

    @Test
    void testForecastWithMovingAverage_WithSufficientData_ShouldCalculate() {
        CourseRequest req2022 = new CourseRequest();
        req2022.setRequestYear(2022);

        CourseRequest req2023 = new CourseRequest();
        req2023.setRequestYear(2023);

        CourseRequest req2024 = new CourseRequest();
        req2024.setRequestYear(2024);

        when(courseRequestRepository.findByCourse(any()))
                .thenReturn(Arrays.asList(req2022, req2023, req2024));

        int result = service.forecastWithMovingAverage(1L, 2025, 3);

        assertEquals(1, result); // Average of 1, 1, 1
    }

    @Test
    void testForecastWithMovingAverage_ShouldUseRecentYears() {
        List<CourseRequest> requests = new ArrayList<>();

        // Add 5 years of data with varying counts
        for (int year = 2020; year <= 2024; year++) {
            for (int i = 0; i < year - 2019; i++) {
                CourseRequest req = new CourseRequest();
                req.setRequestYear(year);
                requests.add(req);
            }
        }

        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);

        int result = service.forecastWithMovingAverage(1L, 2025, 3);

        assertTrue(result >= 0);
        assertTrue(result < 20); // Reasonable upper bound
    }
}
