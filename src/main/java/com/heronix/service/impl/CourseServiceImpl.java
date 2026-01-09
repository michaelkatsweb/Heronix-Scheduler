// Location: src/main/java/com/eduscheduler/service/impl/CourseServiceImpl.java
package com.heronix.service.impl;

import com.heronix.model.domain.Course;
import com.heronix.repository.CourseRepository;
import com.heronix.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Course Service Implementation
 * Location: src/main/java/com/eduscheduler/service/impl/CourseServiceImpl.java
 * 
 * Provides business logic for course management operations
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    /**
     * Get all active courses
     * 
     * @return List of active courses
     */
    @Override
    @Transactional(readOnly = true)
    public List<Course> getAllActiveCourses() {
        log.debug("📚 Fetching all active courses");
        List<Course> courses = courseRepository.findByActiveTrue();
        log.info("✅ Found {} active courses", courses.size());
        return courses;
    }

    /**
     * Get course by ID
     * 
     * @param id Course ID
     * @return Course object or null if not found
     */
    @Override
    @Transactional(readOnly = true)
    public Course getCourseById(Long id) {
        log.debug("🔍 Fetching course with ID: {}", id);
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null) {
            // ✅ NULL SAFE: Safe extraction of course properties
            String courseName = course.getCourseName() != null ? course.getCourseName() : "Unknown";
            String courseCode = course.getCourseCode() != null ? course.getCourseCode() : "N/A";
            log.debug("✅ Found course: {} ({})", courseName, courseCode);
        } else {
            log.warn("⚠️ Course not found with ID: {}", id);
        }
        return course;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findAllWithTeacherCoursesForUI() {
        log.debug("Loading all courses with teacher collections for UI");

        // Three-step fetch within single transaction
        // Step 1: Load courses with teacher and room
        List<Course> courses = courseRepository.findAllWithTeacherAndRoom();

        log.debug("Found {} courses, now loading teacher collections...", courses.size());

        // Step 2: Initialize teacher's courses collection
        // Prevents LazyInitializationException in isTeacherQualifiedForCourse
        courses.forEach(course -> {
            if (course.getTeacher() != null) {
                try {
                    if (course.getTeacher().getCourses() != null) {
                        course.getTeacher().getCourses().size();
                    }
                } catch (Exception e) {
                    log.warn("Could not load courses for teacher in course {}: {}",
                            course.getCourseCode(), e.getMessage());
                }
            }
        });

        // Step 3: Initialize teacher's certifications collection
        // Prevents LazyInitializationException in hasExpiringCertifications
        courses.forEach(course -> {
            if (course.getTeacher() != null) {
                try {
                    if (course.getTeacher().getSubjectCertifications() != null) {
                        course.getTeacher().getSubjectCertifications().size();
                    }
                } catch (Exception e) {
                    log.warn("Could not load certifications for teacher in course {}: {}",
                            course.getCourseCode(), e.getMessage());
                }
            }
        });

        log.debug("Loaded {} courses with all teacher collections", courses.size());
        return courses;
    }
}