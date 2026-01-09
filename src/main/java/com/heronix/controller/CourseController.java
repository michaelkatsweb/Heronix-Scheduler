// Location: src/main/java/com/eduscheduler/controller/CourseController.java
package com.heronix.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.enums.EducationLevel;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Course REST API Controller
 * Location: src/main/java/com/eduscheduler/controller/CourseController.java
 * 
 * Endpoints:
 * - GET /api/courses - Get all courses
 * - GET /api/courses/{id} - Get course by ID
 * - POST /api/courses - Create new course
 * - PUT /api/courses/{id} - Update course
 * - DELETE /api/courses/{id} - Soft delete course
 * - GET /api/courses/active - Get active courses
 * - GET /api/courses/level/{level} - Get by education level
 * - GET /api/courses/subject/{subject} - Get by subject
 * - GET /api/courses/available - Get courses with available seats
 */
@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    /**
     * GET /api/courses
     * Get all courses
     */
    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * GET /api/courses/{id}
     * Get course by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/courses
     * Create new course
     */
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        // Ensure new course is active
        course.setActive(true);

        Course saved = courseRepository.save(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/courses/{id}
     * Update existing course
     */
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(
            @PathVariable Long id,
            @RequestBody Course courseDetails) {

        return courseRepository.findById(id)
                .map(course -> {
                    course.setCourseCode(courseDetails.getCourseCode());
                    course.setCourseName(courseDetails.getCourseName());
                    course.setSubject(courseDetails.getSubject());
                    course.setDescription(courseDetails.getDescription());
                    course.setLevel(courseDetails.getLevel());
                    course.setScheduleType(courseDetails.getScheduleType());
                    course.setTeacher(courseDetails.getTeacher());
                    course.setRoom(courseDetails.getRoom());
                    course.setDurationMinutes(courseDetails.getDurationMinutes());
                    course.setSessionsPerWeek(courseDetails.getSessionsPerWeek());
                    course.setMaxStudents(courseDetails.getMaxStudents());
                    course.setCurrentEnrollment(courseDetails.getCurrentEnrollment());
                    course.setRequiredResources(courseDetails.getRequiredResources());
                    course.setPrerequisites(courseDetails.getPrerequisites());
                    course.setComplexityScore(courseDetails.getComplexityScore());
                    course.setOptimalStartHour(courseDetails.getOptimalStartHour());
                    course.setPriorityLevel(courseDetails.getPriorityLevel());
                    course.setRequiresLab(courseDetails.getRequiresLab());
                    course.setActive(courseDetails.getActive());
                    course.setNotes(courseDetails.getNotes());

                    Course updated = courseRepository.save(course);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/courses/{id}
     * Soft delete course (sets active = false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(course -> {
                    course.setActive(false);
                    courseRepository.save(course);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/courses/active
     * Get all active courses
     */
    @GetMapping("/active")
    public List<Course> getActiveCourses() {
        return courseRepository.findByActiveTrue();
    }

    /**
     * GET /api/courses/code/{courseCode}
     * Get course by course code
     */
    @GetMapping("/code/{courseCode}")
    public ResponseEntity<Course> getCourseByCourseCode(@PathVariable String courseCode) {
        return courseRepository.findByCourseCode(courseCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/courses/level/{level}
     * Get courses by education level
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Course>> getCoursesByLevel(@PathVariable String level) {
        try {
            EducationLevel educationLevel = EducationLevel.valueOf(level.toUpperCase());
            List<Course> courses = courseRepository.findByLevel(educationLevel);
            return ResponseEntity.ok(courses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/courses/subject/{subject}
     * Get courses by subject
     */
    @GetMapping("/subject/{subject}")
    public List<Course> getCoursesBySubject(@PathVariable String subject) {
        return courseRepository.findBySubject(subject);
    }

    /**
     * GET /api/courses/type/{scheduleType}
     * Get courses by schedule type
     */
    @GetMapping("/type/{scheduleType}")
    public ResponseEntity<List<Course>> getCoursesByScheduleType(@PathVariable String scheduleType) {
        try {
            ScheduleType type = ScheduleType.valueOf(scheduleType.toUpperCase());
            List<Course> courses = courseRepository.findByScheduleType(type);
            return ResponseEntity.ok(courses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/courses/teacher/{teacherId}
     * Get courses by teacher
     */
    @GetMapping("/teacher/{teacherId}")
    public List<Course> getCoursesByTeacher(@PathVariable Long teacherId) {
        return courseRepository.findByTeacherId(teacherId);
    }

    /**
     * GET /api/courses/available
     * Get courses with available seats
     */
    @GetMapping("/available")
    public List<Course> getCoursesWithAvailableSeats() {
        return courseRepository.findCoursesWithAvailableSeats();
    }

    /**
     * GET /api/courses/requires-lab
     * Get courses that require a lab
     */
    @GetMapping("/requires-lab")
    public List<Course> getCoursesRequiringLab() {
        return courseRepository.findByRequiresLabTrue();
    }

    /**
     * GET /api/courses/{id}/enrollment
     * Get enrollment info for a course
     */
    @GetMapping("/{id}/enrollment")
    public ResponseEntity<Map<String, Object>> getCourseEnrollment(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(course -> {
                    int available = course.getMaxStudents() - course.getCurrentEnrollment();
                    double fillRate = (course.getCurrentEnrollment() * 100.0) / course.getMaxStudents();

                    Map<String, Object> response = Map.of(
                            "courseId", course.getId(),
                            "courseName", course.getCourseName(),
                            "currentEnrollment", course.getCurrentEnrollment(),
                            "maxStudents", course.getMaxStudents(),
                            "availableSeats", available,
                            "fillRate", fillRate);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/courses/{id}/enroll
     * Increment enrollment count
     */
    @PatchMapping("/{id}/enroll")
    public ResponseEntity<?> enrollStudent(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(course -> {
                    if (course.getCurrentEnrollment() < course.getMaxStudents()) {
                        course.setCurrentEnrollment(course.getCurrentEnrollment() + 1);
                        Course updated = courseRepository.save(course);
                        return ResponseEntity.ok(updated);
                    }
                    // Return error response with message
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of(
                                    "error", "Course is full",
                                    "message", "Cannot enroll - maximum students reached",
                                    "currentEnrollment", course.getCurrentEnrollment(),
                                    "maxStudents", course.getMaxStudents()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

/**
 * PATCH /api/courses/{id}/unenroll
 * Decrement enrollment count
 */
@PatchMapping("/{id}/unenroll")
public ResponseEntity<?> unenrollStudent(@PathVariable Long id) {
    return courseRepository.findById(id)
            .map(course -> {
                if (course.getCurrentEnrollment() > 0) {
                    course.setCurrentEnrollment(course.getCurrentEnrollment() - 1);
                    Course updated = courseRepository.save(course);
                    return ResponseEntity.ok(updated);
                }
                // Return error response with message
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "error", "No students enrolled",
                        "message", "Cannot unenroll - enrollment count is already 0",
                        "currentEnrollment", course.getCurrentEnrollment()
                    ));
            })
            .orElse(ResponseEntity.notFound().build());
}
}