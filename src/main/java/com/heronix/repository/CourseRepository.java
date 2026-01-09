package com.heronix.repository;

import com.heronix.model.domain.Course;
import com.heronix.model.enums.EducationLevel;
import com.heronix.model.enums.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Find course by code
    Optional<Course> findByCourseCode(String courseCode);

    // Find courses by name containing
    List<Course> findByCourseNameContaining(String courseName);

    // Find all active courses
    List<Course> findByActiveTrue();

    // Find courses by subject
    List<Course> findBySubject(String subject);

    // Find courses by education level
    List<Course> findByLevel(EducationLevel level);

    // Find courses by schedule type
    List<Course> findByScheduleType(ScheduleType scheduleType);

    // Find courses by teacher
    List<Course> findByTeacherId(Long teacherId);

    // Find courses that require a lab
    List<Course> findByRequiresLabTrue();

    // Find courses with available seats
    @Query("SELECT c FROM Course c WHERE c.currentEnrollment < c.maxStudents")
    List<Course> findCoursesWithAvailableSeats();

    // Find courses by course codes
    List<Course> findAllByCourseCodeIn(List<String> courseCodes);

    // ========================================================================
    // EAGER LOADING FOR SCHEDULE GENERATION
    // ========================================================================

    /**
     * Find active courses with students eagerly loaded for schedule generation
     * Use this to prevent LazyInitializationException during scheduling
     */
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.students WHERE c.active = true")
    List<Course> findActiveCoursesWithStudents();

    /**
     * Find all courses with teacher and room eagerly loaded
     * Use this to prevent LazyInitializationException in UI tables
     */
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.teacher LEFT JOIN FETCH c.room")
    List<Course> findAllWithTeacherAndRoom();
}