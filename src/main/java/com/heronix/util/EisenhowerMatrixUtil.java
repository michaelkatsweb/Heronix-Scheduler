package com.heronix.util;

import com.heronix.model.domain.Course;
import com.heronix.model.enums.PriorityLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Eisenhower Matrix Utility
 * Location: src/main/java/com/eduscheduler/util/EisenhowerMatrixUtil.java
 * 
 * Provides utility methods for managing course priorities using the Eisenhower Matrix
 */
public class EisenhowerMatrixUtil {

    private EisenhowerMatrixUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Categorize courses by priority level
     */
    public static Map<PriorityLevel, List<Course>> categorizeCourses(List<Course> courses) {
        return courses.stream()
                .collect(Collectors.groupingBy(
                        course -> course.getPriorityLevel() != null
                                ? course.getPriorityLevel()
                                : PriorityLevel.Q2_IMPORTANT_NOT_URGENT));
    }

    /**
     * Get courses by specific priority level
     */
    public static List<Course> getCoursesByPriority(List<Course> courses, PriorityLevel priority) {
        switch (priority) {
            case Q1_URGENT_IMPORTANT:
            case Q2_IMPORTANT_NOT_URGENT:
            case Q3_URGENT_NOT_IMPORTANT:
            case Q4_NEITHER:
            case CRITICAL:
            case HIGH:
            case NORMAL:
            case LOW:
                return courses.stream()
                        .filter(c -> c.getPriorityLevel() == priority)
                        .collect(Collectors.toList());
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Get Q1 courses (Urgent & Important)
     */
    public static List<Course> getQ1Courses(List<Course> courses) {
        return courses.stream()
                .filter(c -> c.getPriorityLevel() == PriorityLevel.Q1_URGENT_IMPORTANT ||
                             c.getPriorityLevel() == PriorityLevel.CRITICAL)
                .collect(Collectors.toList());
    }

    /**
     * Get Q2 courses (Important, Not Urgent)
     */
    public static List<Course> getQ2Courses(List<Course> courses) {
        return courses.stream()
                .filter(c -> c.getPriorityLevel() == PriorityLevel.Q2_IMPORTANT_NOT_URGENT ||
                             c.getPriorityLevel() == PriorityLevel.HIGH ||
                             c.getPriorityLevel() == PriorityLevel.NORMAL)
                .collect(Collectors.toList());
    }

    /**
     * Get Q3 courses (Urgent, Not Important)
     */
    public static List<Course> getQ3Courses(List<Course> courses) {
        return courses.stream()
                .filter(c -> c.getPriorityLevel() == PriorityLevel.Q3_URGENT_NOT_IMPORTANT)
                .collect(Collectors.toList());
    }

    /**
     * Get Q4 courses (Neither Urgent nor Important)
     */
    public static List<Course> getQ4Courses(List<Course> courses) {
        return courses.stream()
                .filter(c -> c.getPriorityLevel() == PriorityLevel.Q4_NEITHER ||
                             c.getPriorityLevel() == PriorityLevel.LOW)
                .collect(Collectors.toList());
    }

    /**
     * Set priority for a course
     */
    public static void setPriority(Course course, PriorityLevel priority) {
        if (course != null) {
            course.setPriorityLevel(priority != null
                    ? priority
                    : PriorityLevel.Q2_IMPORTANT_NOT_URGENT);
        }
    }

    /**
     * Get priority recommendations for a course
     */
    public static List<String> getPriorityRecommendations(Course course) {
        List<String> recommendations = new ArrayList<>();

        if (course == null || course.getPriorityLevel() == null) {
            return recommendations;
        }

        switch (course.getPriorityLevel()) {
            case Q1_URGENT_IMPORTANT:
            case CRITICAL:
                recommendations.add("Schedule in optimal morning time slots");
                recommendations.add("Assign most experienced teachers");
                recommendations.add("Ensure adequate resources available");
                break;
            
            case Q2_IMPORTANT_NOT_URGENT:
            case HIGH:
            case NORMAL:
                recommendations.add("Schedule during standard school hours");
                recommendations.add("Allow flexibility for optimal learning times");
                break;
            
            case Q3_URGENT_NOT_IMPORTANT:
                recommendations.add("Can be scheduled in less optimal time slots");
                recommendations.add("Consider combining with related activities");
                break;
            
            case Q4_NEITHER:
            case LOW:
                recommendations.add("Schedule in remaining available slots");
                recommendations.add("Can be optional or elective");
                break;
        }

        return recommendations;
    }

    /**
     * Check if course is high priority
     */
    public static boolean isHighPriority(Course course) {
        return course != null &&
                (course.getPriorityLevel() == PriorityLevel.Q1_URGENT_IMPORTANT ||
                 course.getPriorityLevel() == PriorityLevel.CRITICAL);
    }

    /**
     * Get numeric priority score (higher = more important)
     * NOW COVERS ALL ENUM VALUES
     */
    public static int getPriorityScore(PriorityLevel level) {
        if (level == null) {
            return 2;
        }
        
        return switch (level) {
            case CRITICAL, Q1_URGENT_IMPORTANT -> 4;
            case HIGH, Q2_IMPORTANT_NOT_URGENT -> 3;
            case NORMAL, Q3_URGENT_NOT_IMPORTANT -> 2;
            case LOW, Q4_NEITHER -> 1;
        };
    }

    /**
     * Sort courses by priority (highest first)
     */
    public static List<Course> sortByPriority(List<Course> courses) {
        return courses.stream()
                .sorted((c1, c2) -> Integer.compare(
                        getPriorityScore(c2.getPriorityLevel()),
                        getPriorityScore(c1.getPriorityLevel())))
                .collect(Collectors.toList());
    }
}