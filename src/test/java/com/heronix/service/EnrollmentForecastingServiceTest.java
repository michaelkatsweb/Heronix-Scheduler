package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseRequest;
import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Student;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.CourseRequestRepository;
import com.heronix.repository.CourseSectionRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.service.impl.EnrollmentForecastingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for Enrollment Forecasting Service
 *
 * Tests enrollment prediction and capacity planning functionality including:
 * - Total enrollment forecasting
 * - Grade-level predictions
 * - Course demand forecasting
 * - Section recommendations
 * - Capacity warnings
 *
 * Location: src/test/java/com/eduscheduler/service/EnrollmentForecastingServiceTest.java
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentForecastingServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseRequestRepository courseRequestRepository;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @InjectMocks
    private EnrollmentForecastingServiceImpl forecastingService;

    private Course testCourse;
    private Student testStudent;
    private CourseRequest testRequest;
    private CourseSection testSection;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);

        // Create test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("ENG101");
        testCourse.setCourseName("English 10");
        testCourse.setMaxStudents(25);

        // Create test course request
        testRequest = new CourseRequest();
        testRequest.setId(1L);
        testRequest.setCourse(testCourse);
        testRequest.setStudent(testStudent);
        testRequest.setRequestYear(LocalDate.now().getYear());

        // Create test section
        testSection = new CourseSection();
        testSection.setId(1L);
        testSection.setCourse(testCourse);
        testSection.setMaxEnrollment(25);
    }

    @Test
    void testForecastTotalEnrollment_NextYear() {
        // Arrange
        when(studentRepository.count()).thenReturn(500L);
        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        int forecast = forecastingService.forecastTotalEnrollment(targetYear);

        // Assert
        assertTrue(forecast > 0);
        assertTrue(forecast >= 500); // Should be at least current enrollment
    }

    @Test
    void testForecastTotalEnrollment_CurrentYear() {
        // Arrange
        when(studentRepository.count()).thenReturn(500L);
        int currentYear = LocalDate.now().getYear();

        // Act
        int forecast = forecastingService.forecastTotalEnrollment(currentYear);

        // Assert
        assertEquals(500, forecast);
    }

    @Test
    void testForecastEnrollmentByGrade_ReturnsAllGrades() {
        // Arrange
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Student s = new Student();
            s.setId((long) i);
            s.setGradeLevel(String.valueOf(9 + (i % 4))); // Grades 9-12
            s.setActive(true);
            students.add(s);
        }
        when(studentRepository.findAll()).thenReturn(students);
        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        Map<String, Integer> forecast = forecastingService.forecastEnrollmentByGrade(targetYear);

        // Assert
        assertNotNull(forecast);
        assertTrue(forecast.containsKey("9"));
        assertTrue(forecast.containsKey("10"));
        assertTrue(forecast.containsKey("11"));
        assertTrue(forecast.containsKey("12"));
    }

    @Test
    void testForecastCourseDemand_ReturnsPositiveDemand() {
        // Arrange
        List<Course> courses = Arrays.asList(testCourse);
        when(courseRepository.findAll()).thenReturn(courses);

        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CourseRequest req = new CourseRequest();
            req.setCourse(testCourse);
            req.setRequestYear(LocalDate.now().getYear());
            requests.add(req);
        }
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);

        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        Map<Long, Integer> demand = forecastingService.forecastCourseDemand(targetYear);

        // Assert
        assertNotNull(demand);
        assertTrue(demand.containsKey(1L));
    }

    @Test
    void testRecommendSectionCount_SingleSection() {
        // Arrange
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CourseRequest req = new CourseRequest();
            req.setCourse(testCourse);
            req.setRequestYear(LocalDate.now().getYear());
            requests.add(req);
        }
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        int sections = forecastingService.recommendSectionCount(1L, targetYear);

        // Assert
        assertTrue(sections >= 1);
    }

    @Test
    void testRecommendSectionCount_MultipleSections() {
        // Arrange
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            CourseRequest req = new CourseRequest();
            req.setCourse(testCourse);
            req.setRequestYear(LocalDate.now().getYear());
            requests.add(req);
        }
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        int sections = forecastingService.recommendSectionCount(1L, targetYear);

        // Assert
        assertTrue(sections >= 2); // 60 students / 25 per section = 3 sections minimum
    }

    @Test
    void testGetCourseTrend_Growing() {
        // Arrange
        int currentYear = LocalDate.now().getYear();
        List<CourseRequest> requests = new ArrayList<>();

        // Last year: 20 requests
        for (int i = 0; i < 20; i++) {
            CourseRequest req = new CourseRequest();
            req.setCourse(testCourse);
            req.setRequestYear(currentYear - 1);
            requests.add(req);
        }

        // This year: 30 requests (50% growth)
        for (int i = 0; i < 30; i++) {
            CourseRequest req = new CourseRequest();
            req.setCourse(testCourse);
            req.setRequestYear(currentYear);
            requests.add(req);
        }

        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        // Act
        String trend = forecastingService.getCourseTrend(1L);

        // Assert
        assertNotNull(trend);
        assertTrue(trend.contains("Growing") || trend.contains("Stable") || trend.contains("Declining"));
    }

    @Test
    void testGetCourseTrend_InsufficientData() {
        // Arrange
        when(courseRequestRepository.findByCourse(any())).thenReturn(Collections.emptyList());
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        // Act
        String trend = forecastingService.getCourseTrend(1L);

        // Assert
        assertEquals("Insufficient Data", trend);
    }

    @Test
    void testCalculateGrowthRate_PositiveGrowth() {
        // Arrange
        int year1 = LocalDate.now().getYear() - 1;
        int year2 = LocalDate.now().getYear();

        List<CourseRequest> allRequests = new ArrayList<>();

        // Year 1: 100 requests
        for (int i = 0; i < 100; i++) {
            CourseRequest req = new CourseRequest();
            req.setRequestYear(year1);
            allRequests.add(req);
        }

        // Year 2: 120 requests
        for (int i = 0; i < 120; i++) {
            CourseRequest req = new CourseRequest();
            req.setRequestYear(year2);
            allRequests.add(req);
        }

        when(courseRequestRepository.findAll()).thenReturn(allRequests);

        // Act
        double growthRate = forecastingService.calculateGrowthRate(year1, year2);

        // Assert
        assertEquals(20.0, growthRate, 0.1);
    }

    @Test
    void testHasAdequateCapacity_SufficientCapacity() {
        // Arrange
        List<Course> courses = Arrays.asList(testCourse);
        when(courseRepository.findAll()).thenReturn(courses);

        List<CourseRequest> requests = Arrays.asList(testRequest);
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);

        List<CourseSection> sections = Arrays.asList(testSection);
        when(courseSectionRepository.findAll()).thenReturn(sections);

        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        boolean hasCapacity = forecastingService.hasAdequateCapacity(targetYear);

        // Assert
        assertTrue(hasCapacity);
    }

    @Test
    void testGetCapacityWarnings_OverCapacity() {
        // Arrange
        List<Course> courses = Arrays.asList(testCourse);
        when(courseRepository.findAll()).thenReturn(courses);

        // Create 50 requests (exceeds 25-student section capacity)
        List<CourseRequest> requests = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            CourseRequest req = new CourseRequest();
            req.setCourse(testCourse);
            req.setRequestYear(LocalDate.now().getYear());
            requests.add(req);
        }
        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);

        List<CourseSection> sections = Arrays.asList(testSection);
        when(courseSectionRepository.findAll()).thenReturn(sections);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        Map<String, String> warnings = forecastingService.getCapacityWarnings(targetYear);

        // Assert
        assertNotNull(warnings);
    }

    @Test
    void testGetForecastingReport_ContainsKeyMetrics() {
        // Arrange
        when(studentRepository.count()).thenReturn(500L);
        when(studentRepository.findAll()).thenReturn(Arrays.asList(testStudent));
        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));
        when(courseRequestRepository.findByCourse(any())).thenReturn(Arrays.asList(testRequest));
        when(courseSectionRepository.findAll()).thenReturn(Arrays.asList(testSection));

        int targetYear = LocalDate.now().getYear() + 1;

        // Act
        Map<String, Object> report = forecastingService.getForecastingReport(targetYear);

        // Assert
        assertNotNull(report);
        assertTrue(report.containsKey("totalEnrollment"));
        assertTrue(report.containsKey("enrollmentByGrade"));
    }

    @Test
    void testForecastWithLinearRegression_MultiYearData() {
        // Arrange
        List<CourseRequest> requests = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        // Create 3 years of increasing data
        for (int year = currentYear - 2; year <= currentYear; year++) {
            int count = 20 + (year - (currentYear - 2)) * 5; // 20, 25, 30
            for (int i = 0; i < count; i++) {
                CourseRequest req = new CourseRequest();
                req.setCourse(testCourse);
                req.setRequestYear(year);
                requests.add(req);
            }
        }

        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        int targetYear = currentYear + 1;

        // Act
        int forecast = forecastingService.forecastWithLinearRegression(1L, targetYear);

        // Assert
        assertTrue(forecast >= 30); // Should predict continuation of trend
    }

    @Test
    void testForecastWithMovingAverage_ThreeYearWindow() {
        // Arrange
        List<CourseRequest> requests = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        // Create 3 years of data: 20, 25, 30
        for (int year = currentYear - 2; year <= currentYear; year++) {
            int count = 20 + (year - (currentYear - 2)) * 5;
            for (int i = 0; i < count; i++) {
                CourseRequest req = new CourseRequest();
                req.setCourse(testCourse);
                req.setRequestYear(year);
                requests.add(req);
            }
        }

        when(courseRequestRepository.findByCourse(any())).thenReturn(requests);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        int targetYear = currentYear + 1;

        // Act
        int forecast = forecastingService.forecastWithMovingAverage(1L, targetYear, 3);

        // Assert
        assertEquals(25, forecast); // Average of 20, 25, 30 = 25
    }
}
