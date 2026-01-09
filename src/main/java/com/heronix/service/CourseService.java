package com.heronix.service;

import com.heronix.model.domain.Course;
import java.util.List;

public interface CourseService {
    List<Course> getAllActiveCourses();
    Course getCourseById(Long id);

    /**
     * Load all courses with teacher and their courses collection for UI display
     * Uses transactional approach to load:
     * 1. Courses with teacher and room
     * 2. Teacher's courses collection (to prevent LazyInitializationException)
     *
     * @return List of courses with all needed collections loaded
     */
    List<Course> findAllWithTeacherCoursesForUI();
}