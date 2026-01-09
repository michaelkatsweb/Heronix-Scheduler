package com.heronix.service.impl;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseRequest;
import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Student;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.CourseRequestRepository;
import com.heronix.repository.CourseSectionRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.service.EnrollmentForecastingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enrollment Forecasting Service Implementation
 *
 * Implements forecasting algorithms:
 * - Linear regression for trend-based prediction
 * - Moving average for smoothed estimates
 * - Historical analysis for pattern detection
 * - Capacity planning recommendations
 *
 * Location: src/main/java/com/eduscheduler/service/impl/EnrollmentForecastingServiceImpl.java
 */
@Service
@Slf4j
public class EnrollmentForecastingServiceImpl implements EnrollmentForecastingService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseRequestRepository courseRequestRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    private static final int DEFAULT_SECTION_CAPACITY = 25;
    private static final double CAPACITY_WARNING_THRESHOLD = 0.9; // 90% capacity

    @Override
    public int forecastTotalEnrollment(Integer targetYear) {
        log.info("Forecasting total enrollment for year: {}", targetYear);

        // Get current enrollment
        long currentEnrollment = studentRepository.count();

        // Calculate historical growth rate
        // For now, use simple projection based on current trends
        // In full implementation, would analyze multi-year data

        int currentYear = LocalDate.now().getYear();
        int yearsDiff = targetYear - currentYear;

        if (yearsDiff <= 0) {
            return (int) currentEnrollment;
        }

        // Assume 2% annual growth (configurable in production)
        double growthRate = 0.02;
        double forecasted = currentEnrollment * Math.pow(1 + growthRate, yearsDiff);

        log.info("Forecasted enrollment: {} (current: {}, growth: {}%)",
            (int) forecasted, currentEnrollment, growthRate * 100);

        return (int) Math.round(forecasted);
    }

    @Override
    public Map<String, Integer> forecastEnrollmentByGrade(Integer targetYear) {
        log.info("Forecasting enrollment by grade for year: {}", targetYear);

        Map<String, Integer> forecast = new HashMap<>();

        // Get current enrollment by grade
        List<Student> allStudents = studentRepository.findAll();
        // ✅ NULL SAFE: Filter null students before accessing properties
        Map<String, Long> currentByGrade = allStudents.stream()
            .filter(student -> student != null && student.isActive())
            .filter(student -> student.getGradeLevel() != null)
            .collect(Collectors.groupingBy(Student::getGradeLevel, Collectors.counting()));

        int currentYear = LocalDate.now().getYear();
        int yearsDiff = targetYear - currentYear;

        // Project each grade level
        String[] grades = {"9", "10", "11", "12"};
        for (String grade : grades) {
            long current = currentByGrade.getOrDefault(grade, 0L);

            // Simple promotion model: grade N becomes grade N+1
            // Adjust for attrition/retention
            double attritionRate = 0.03; // 3% don't continue
            double forecasted = current * Math.pow(1 - attritionRate, yearsDiff);

            forecast.put(grade, (int) Math.round(forecasted));
        }

        log.info("Grade level forecast: {}", forecast);
        return forecast;
    }

    @Override
    public Map<Long, Integer> forecastCourseDemand(Integer targetYear) {
        log.info("Forecasting course demand for year: {}", targetYear);

        Map<Long, Integer> demand = new HashMap<>();

        List<Course> allCourses = courseRepository.findAll();

        for (Course course : allCourses) {
            // ✅ NULL SAFE: Skip null courses or courses with null ID
            if (course == null || course.getId() == null) continue;

            int predicted = forecastWithLinearRegression(course.getId(), targetYear);
            demand.put(course.getId(), predicted);
        }

        log.info("Forecasted demand for {} courses", demand.size());
        return demand;
    }

    @Override
    public int recommendSectionCount(Long courseId, Integer targetYear) {
        log.info("Calculating recommended sections for course {} in year {}", courseId, targetYear);

        // Forecast demand
        int predictedDemand = forecastWithLinearRegression(courseId, targetYear);

        // Get course capacity
        Course course = courseRepository.findById(courseId).orElse(null);
        int sectionCapacity = course != null && course.getMaxStudents() != null ?
            course.getMaxStudents() : DEFAULT_SECTION_CAPACITY;

        // Calculate sections needed (round up)
        int sectionsNeeded = (int) Math.ceil((double) predictedDemand / sectionCapacity);

        // Minimum 1 section if there's any demand
        if (predictedDemand > 0 && sectionsNeeded == 0) {
            sectionsNeeded = 1;
        }

        log.info("Recommended sections: {} (demand: {}, capacity: {})",
            sectionsNeeded, predictedDemand, sectionCapacity);

        return sectionsNeeded;
    }

    @Override
    public String getCourseTrend(Long courseId) {
        log.info("Analyzing trend for course: {}", courseId);

        // Get historical request data
        List<CourseRequest> requests = courseRequestRepository.findByCourse(courseRepository.findById(courseId).orElse(null));

        if (requests.isEmpty()) {
            return "Insufficient Data";
        }

        // Group by year
        // ✅ NULL SAFE: Filter null requests before grouping
        Map<Integer, Long> requestsByYear = requests.stream()
            .filter(req -> req != null && req.getRequestYear() != null)
            .collect(Collectors.groupingBy(CourseRequest::getRequestYear, Collectors.counting()));

        if (requestsByYear.size() < 2) {
            return "Stable";
        }

        // Calculate trend from most recent years
        List<Integer> years = new ArrayList<>(requestsByYear.keySet());
        Collections.sort(years);

        if (years.size() >= 2) {
            int recentYear = years.get(years.size() - 1);
            int previousYear = years.get(years.size() - 2);

            long recentCount = requestsByYear.get(recentYear);
            long previousCount = requestsByYear.get(previousYear);

            double changePercent = ((recentCount - previousCount) * 100.0) / previousCount;

            if (changePercent > 10) {
                return String.format("Growing (+%.1f%%)", changePercent);
            } else if (changePercent < -10) {
                return String.format("Declining (%.1f%%)", changePercent);
            } else {
                return String.format("Stable (%.1f%%)", changePercent);
            }
        }

        return "Stable";
    }

    @Override
    public double calculateGrowthRate(Integer year1, Integer year2) {
        log.info("Calculating growth rate between {} and {}", year1, year2);

        // Get all requests and filter by year
        List<CourseRequest> allRequests = courseRequestRepository.findAll();

        long count1 = allRequests.stream()
            .filter(r -> r.getRequestYear() != null && r.getRequestYear().equals(year1))
            .count();

        long count2 = allRequests.stream()
            .filter(r -> r.getRequestYear() != null && r.getRequestYear().equals(year2))
            .count();

        if (count1 == 0 || count2 == 0) {
            return 0.0;
        }

        double growthRate = ((count2 - count1) * 100.0) / count1;

        log.info("Growth rate: {:.2f}% (from {} to {})", growthRate, count1, count2);

        return growthRate;
    }

    @Override
    public Map<String, Object> getForecastingReport(Integer targetYear) {
        log.info("Generating comprehensive forecasting report for year: {}", targetYear);

        Map<String, Object> report = new HashMap<>();

        // Total enrollment forecast
        int totalEnrollment = forecastTotalEnrollment(targetYear);
        report.put("totalEnrollment", totalEnrollment);

        // Grade level breakdown
        Map<String, Integer> byGrade = forecastEnrollmentByGrade(targetYear);
        report.put("enrollmentByGrade", byGrade);

        // Course demand
        Map<Long, Integer> courseDemand = forecastCourseDemand(targetYear);
        report.put("courseDemand", courseDemand);

        // Top growing courses
        List<Course> allCourses = courseRepository.findAll();
        List<Map<String, Object>> growingCourses = allCourses.stream()
            .map(course -> {
                String trend = getCourseTrend(course.getId());
                if (trend.contains("Growing")) {
                    Map<String, Object> courseInfo = new HashMap<>();
                    courseInfo.put("courseName", course.getCourseName());
                    courseInfo.put("courseCode", course.getCourseCode());
                    courseInfo.put("trend", trend);
                    return courseInfo;
                }
                return null;
            })
            .filter(Objects::nonNull)
            .limit(10)
            .collect(Collectors.toList());
        report.put("growingCourses", growingCourses);

        // Capacity warnings
        Map<String, String> warnings = getCapacityWarnings(targetYear);
        report.put("capacityWarnings", warnings);

        // Overall capacity status
        boolean adequate = hasAdequateCapacity(targetYear);
        report.put("hasAdequateCapacity", adequate);

        log.info("Forecasting report generated with {} metrics", report.size());

        return report;
    }

    @Override
    public boolean hasAdequateCapacity(Integer targetYear) {
        log.info("Checking capacity adequacy for year: {}", targetYear);

        Map<Long, Integer> demand = forecastCourseDemand(targetYear);
        List<CourseSection> sections = courseSectionRepository.findAll();

        // Group sections by course
        Map<Long, List<CourseSection>> sectionsByCourse = sections.stream()
            .collect(Collectors.groupingBy(section -> section.getCourse().getId()));

        // Check each course
        for (Map.Entry<Long, Integer> entry : demand.entrySet()) {
            Long courseId = entry.getKey();
            int predictedDemand = entry.getValue();

            List<CourseSection> courseSections = sectionsByCourse.getOrDefault(courseId, new ArrayList<>());
            int totalCapacity = courseSections.stream()
                .mapToInt(section -> section.getMaxEnrollment() != null ? section.getMaxEnrollment() : DEFAULT_SECTION_CAPACITY)
                .sum();

            // If demand exceeds 90% of capacity, we don't have adequate capacity
            if (predictedDemand > totalCapacity * CAPACITY_WARNING_THRESHOLD) {
                log.warn("Insufficient capacity for course {}: demand={}, capacity={}",
                    courseId, predictedDemand, totalCapacity);
                return false;
            }
        }

        log.info("Capacity is adequate for forecasted demand");
        return true;
    }

    @Override
    public Map<String, String> getCapacityWarnings(Integer targetYear) {
        log.info("Generating capacity warnings for year: {}", targetYear);

        Map<String, String> warnings = new HashMap<>();

        Map<Long, Integer> demand = forecastCourseDemand(targetYear);
        List<CourseSection> sections = courseSectionRepository.findAll();

        // Group sections by course
        Map<Long, List<CourseSection>> sectionsByCourse = sections.stream()
            .collect(Collectors.groupingBy(section -> section.getCourse().getId()));

        for (Map.Entry<Long, Integer> entry : demand.entrySet()) {
            Long courseId = entry.getKey();
            int predictedDemand = entry.getValue();

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) continue;

            List<CourseSection> courseSections = sectionsByCourse.getOrDefault(courseId, new ArrayList<>());
            int totalCapacity = courseSections.stream()
                .mapToInt(section -> section.getMaxEnrollment() != null ? section.getMaxEnrollment() : DEFAULT_SECTION_CAPACITY)
                .sum();

            // Check for over-capacity
            if (predictedDemand > totalCapacity) {
                int shortage = predictedDemand - totalCapacity;
                warnings.put(course.getCourseName(),
                    String.format("OVER CAPACITY: Need %d more seats (demand: %d, capacity: %d)",
                        shortage, predictedDemand, totalCapacity));
            }
            // Check for near-capacity
            else if (predictedDemand > totalCapacity * CAPACITY_WARNING_THRESHOLD) {
                warnings.put(course.getCourseName(),
                    String.format("NEAR CAPACITY: %.0f%% utilized (demand: %d, capacity: %d)",
                        (predictedDemand * 100.0 / totalCapacity), predictedDemand, totalCapacity));
            }
            // Check for under-utilized
            else if (totalCapacity > 0 && predictedDemand < totalCapacity * 0.5) {
                warnings.put(course.getCourseName(),
                    String.format("UNDER-UTILIZED: Only %.0f%% utilized (demand: %d, capacity: %d)",
                        (predictedDemand * 100.0 / totalCapacity), predictedDemand, totalCapacity));
            }
        }

        log.info("Generated {} capacity warnings", warnings.size());
        return warnings;
    }

    @Override
    public int forecastWithLinearRegression(Long courseId, Integer targetYear) {
        log.debug("Forecasting course {} with linear regression for year {}", courseId, targetYear);

        // Get historical course requests
        List<CourseRequest> requests = courseRequestRepository.findByCourse(courseRepository.findById(courseId).orElse(null));

        if (requests.isEmpty()) {
            log.warn("No historical data for course {}", courseId);
            return 0;
        }

        // Group by year
        Map<Integer, Long> requestsByYear = requests.stream()
            .collect(Collectors.groupingBy(CourseRequest::getRequestYear, Collectors.counting()));

        if (requestsByYear.size() < 2) {
            // Not enough data for regression, use last year's count
            return requestsByYear.values().stream().findFirst().orElse(0L).intValue();
        }

        // Perform simple linear regression: y = mx + b
        List<Integer> years = new ArrayList<>(requestsByYear.keySet());
        Collections.sort(years);

        // Calculate means
        double meanX = years.stream().mapToInt(Integer::intValue).average().orElse(0);
        double meanY = requestsByYear.values().stream().mapToLong(Long::longValue).average().orElse(0);

        // Calculate slope (m)
        double numerator = 0;
        double denominator = 0;

        for (Integer year : years) {
            double x = year;
            double y = requestsByYear.get(year);
            numerator += (x - meanX) * (y - meanY);
            denominator += Math.pow(x - meanX, 2);
        }

        double slope = denominator != 0 ? numerator / denominator : 0;
        double intercept = meanY - (slope * meanX);

        // Predict for target year
        double prediction = (slope * targetYear) + intercept;

        // Ensure non-negative
        int forecasted = Math.max(0, (int) Math.round(prediction));

        log.debug("Linear regression result: {} students (slope: {:.2f}, intercept: {:.2f})",
            forecasted, slope, intercept);

        return forecasted;
    }

    @Override
    public int forecastWithMovingAverage(Long courseId, Integer targetYear, int windowSize) {
        log.debug("Forecasting course {} with {}-year moving average for year {}",
            courseId, windowSize, targetYear);

        // Get historical course requests
        List<CourseRequest> requests = courseRequestRepository.findByCourse(courseRepository.findById(courseId).orElse(null));

        if (requests.isEmpty()) {
            return 0;
        }

        // Group by year
        Map<Integer, Long> requestsByYear = requests.stream()
            .collect(Collectors.groupingBy(CourseRequest::getRequestYear, Collectors.counting()));

        List<Integer> years = new ArrayList<>(requestsByYear.keySet());
        Collections.sort(years);

        if (years.size() < windowSize) {
            // Not enough data, use simple average
            double average = requestsByYear.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            return (int) Math.round(average);
        }

        // Calculate moving average from most recent years
        int startIndex = Math.max(0, years.size() - windowSize);
        List<Integer> recentYears = years.subList(startIndex, years.size());

        double average = recentYears.stream()
            .mapToLong(requestsByYear::get)
            .average()
            .orElse(0.0);

        int forecasted = (int) Math.round(average);

        log.debug("Moving average result: {} students (window: {} years)", forecasted, windowSize);

        return forecasted;
    }
}
