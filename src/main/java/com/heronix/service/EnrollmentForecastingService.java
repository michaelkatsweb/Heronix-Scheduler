package com.heronix.service;

import com.heronix.model.domain.Course;
import java.util.Map;

/**
 * Enrollment Forecasting Service
 *
 * Provides enrollment predictions and capacity planning based on:
 * - Historical enrollment data
 * - Course request trends
 * - Demographic projections
 * - Graduation/promotion rates
 *
 * Helps administrators answer:
 * - How many sections do we need next year?
 * - Which courses are growing/declining?
 * - What's our enrollment trajectory?
 * - Do we have enough capacity?
 *
 * Location: src/main/java/com/eduscheduler/service/EnrollmentForecastingService.java
 */
public interface EnrollmentForecastingService {

    /**
     * Forecast total school enrollment for target year
     *
     * @param targetYear The year to forecast
     * @return Predicted total enrollment
     */
    int forecastTotalEnrollment(Integer targetYear);

    /**
     * Forecast enrollment by grade level
     *
     * @param targetYear The year to forecast
     * @return Map of grade level to predicted enrollment
     */
    Map<String, Integer> forecastEnrollmentByGrade(Integer targetYear);

    /**
     * Forecast course demand (number of students requesting each course)
     *
     * @param targetYear The year to forecast
     * @return Map of course ID to predicted demand
     */
    Map<Long, Integer> forecastCourseDemand(Integer targetYear);

    /**
     * Calculate recommended number of sections for a course
     *
     * @param courseId The course ID
     * @param targetYear The year to plan for
     * @return Recommended number of sections
     */
    int recommendSectionCount(Long courseId, Integer targetYear);

    /**
     * Get enrollment trend for a course (growing, stable, declining)
     *
     * @param courseId The course ID
     * @return Trend description
     */
    String getCourseTrend(Long courseId);

    /**
     * Calculate year-over-year growth rate
     *
     * @param year1 First year
     * @param year2 Second year
     * @return Growth rate as percentage
     */
    double calculateGrowthRate(Integer year1, Integer year2);

    /**
     * Get comprehensive forecasting report
     *
     * @param targetYear The year to forecast
     * @return Map of metrics and predictions
     */
    Map<String, Object> getForecastingReport(Integer targetYear);

    /**
     * Check if we have sufficient capacity for forecasted demand
     *
     * @param targetYear The year to check
     * @return True if capacity is sufficient
     */
    boolean hasAdequateCapacity(Integer targetYear);

    /**
     * Get capacity warnings (courses that may be over/under capacity)
     *
     * @param targetYear The year to check
     * @return Map of course name to warning message
     */
    Map<String, String> getCapacityWarnings(Integer targetYear);

    /**
     * Forecast by using linear regression on historical data
     *
     * @param courseId The course ID
     * @param targetYear The year to forecast
     * @return Predicted enrollment
     */
    int forecastWithLinearRegression(Long courseId, Integer targetYear);

    /**
     * Forecast using moving average
     *
     * @param courseId The course ID
     * @param targetYear The year to forecast
     * @param windowSize Number of years to average
     * @return Predicted enrollment
     */
    int forecastWithMovingAverage(Long courseId, Integer targetYear, int windowSize);
}
