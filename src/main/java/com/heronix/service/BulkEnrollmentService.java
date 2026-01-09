package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentEnrollmentHistory;
import com.heronix.model.dto.CourseSuggestion;
import com.heronix.model.dto.StudentSearchCriteria;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.StudentEnrollmentHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for bulk student enrollment operations.
 *
 * Provides smart search, filtering, and bulk course assignment for students.
 *
 * @author Heronix Scheduler Team
 */
@Service
public class BulkEnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(BulkEnrollmentService.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseSuggestionEngine suggestionEngine;

    @Autowired
    private StudentEnrollmentHistoryRepository enrollmentHistoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Search for students matching criteria.
     *
     * @param criteria search criteria
     * @return list of matching students
     */
    public List<Student> searchStudents(StudentSearchCriteria criteria) {
        log.info("Searching students with criteria: {}", criteria);

        // Start with all active students
        List<Student> students = studentRepository.findByActiveTrue();

        // Apply filters
        students = applyFilters(students, criteria);

        log.info("Found {} students matching criteria", students.size());
        return students;
    }

    /**
     * Apply search criteria filters to student list.
     */
    private List<Student> applyFilters(List<Student> students, StudentSearchCriteria criteria) {
        return students.stream()
            .filter(s -> matchesGradeLevel(s, criteria))
            .filter(s -> matchesIEP(s, criteria))
            .filter(s -> matches504(s, criteria))
            .filter(s -> matchesName(s, criteria))
            .filter(s -> matchesStudentId(s, criteria))
            .filter(s -> matchesCourseCount(s, criteria))
            .filter(s -> matchesGraduated(s, criteria))
            .filter(s -> matchesGraduationYear(s, criteria))
            .collect(Collectors.toList());
    }

    private boolean matchesGradeLevel(Student s, StudentSearchCriteria c) {
        if (c.getGradeLevel() == null) return true;
        return String.valueOf(c.getGradeLevel()).equals(s.getGradeLevel());
    }

    private boolean matchesIEP(Student s, StudentSearchCriteria c) {
        if (c.getHasIEP() == null) return true;
        return c.getHasIEP().equals(s.getHasIEP());
    }

    private boolean matches504(Student s, StudentSearchCriteria c) {
        if (c.getHas504() == null) return true;
        return c.getHas504().equals(s.getHas504Plan());
    }

    private boolean matchesName(Student s, StudentSearchCriteria c) {
        if (c.getNamePattern() == null || c.getNamePattern().isEmpty()) return true;
        String pattern = c.getNamePattern().toLowerCase();
        String fullName = (s.getFirstName() + " " + s.getLastName()).toLowerCase();
        return fullName.contains(pattern);
    }

    private boolean matchesStudentId(Student s, StudentSearchCriteria c) {
        if (c.getStudentIdPattern() == null || c.getStudentIdPattern().isEmpty()) return true;
        return s.getStudentId().contains(c.getStudentIdPattern());
    }

    private boolean matchesCourseCount(Student s, StudentSearchCriteria c) {
        int courseCount = s.getEnrolledCourseCount();

        if (c.getMinCoursesEnrolled() != null && courseCount < c.getMinCoursesEnrolled()) {
            return false;
        }

        if (c.getMaxCoursesEnrolled() != null && courseCount > c.getMaxCoursesEnrolled()) {
            return false;
        }

        return true;
    }

    private boolean matchesGraduated(Student s, StudentSearchCriteria c) {
        if (c.getGraduated() == null) return true;
        Boolean graduated = s.getGraduated() != null ? s.getGraduated() : false;
        return c.getGraduated().equals(graduated);
    }

    private boolean matchesGraduationYear(Student s, StudentSearchCriteria c) {
        if (c.getGraduationYear() == null) return true;
        return c.getGraduationYear().equals(s.getGraduationYear());
    }

    /**
     * Enroll multiple students in a course.
     *
     * @param students list of students
     * @param course the course
     * @return count of successfully enrolled students
     */
    @Transactional
    public int bulkEnroll(List<Student> students, Course course) {
        log.info("Bulk enrolling {} students in course: {}", students.size(), course.getCourseCode());

        int enrolled = 0;
        int currentEnrollment = getCurrentEnrollmentCount(course);

        for (Student student : students) {
            // Check if course is full
            if (currentEnrollment >= course.getMaxStudents()) {
                log.warn("Course {} is full ({}/ {})", course.getCourseCode(),
                    currentEnrollment, course.getMaxStudents());
                break;
            }

            // Check if student already enrolled
            if (isStudentEnrolled(student, course)) {
                log.debug("Student {} already enrolled in {}", student.getStudentId(), course.getCourseCode());
                continue;
            }

            // Check prerequisites
            if (!hasPrerequisites(student, course)) {
                log.warn("Student {} missing prerequisites for {}", student.getStudentId(), course.getCourseCode());
                continue;
            }

            // Enroll student
            student.getEnrolledCourses().add(course);
            enrolled++;
            currentEnrollment++;
        }

        // Save all students
        studentRepository.saveAll(students);

        log.info("Successfully enrolled {} students in {}", enrolled, course.getCourseCode());
        return enrolled;
    }

    /**
     * Unenroll multiple students from a course.
     *
     * @param students list of students
     * @param course the course
     * @return count of successfully unenrolled students
     */
    @Transactional
    public int bulkUnenroll(List<Student> students, Course course) {
        log.info("Bulk unenrolling {} students from course: {}", students.size(), course.getCourseCode());

        int unenrolled = 0;

        for (Student student : students) {
            if (isStudentEnrolled(student, course)) {
                student.getEnrolledCourses().remove(course);
                unenrolled++;
            }
        }

        studentRepository.saveAll(students);

        log.info("Successfully unenrolled {} students from {}", unenrolled, course.getCourseCode());
        return unenrolled;
    }

    /**
     * Get course suggestions for a grade level.
     *
     * @param gradeLevel the grade level
     * @return list of course suggestions
     */
    public List<CourseSuggestion> getSuggestionsForGrade(int gradeLevel) {
        log.info("Getting course suggestions for grade {}", gradeLevel);

        // Create a sample student at this grade to get suggestions
        Student sampleStudent = new Student();
        sampleStudent.setGradeLevel(String.valueOf(gradeLevel));

        return suggestionEngine.suggestCoursesForStudent(sampleStudent);
    }

    /**
     * Get course suggestions for a specific student.
     *
     * @param student the student
     * @return list of course suggestions
     */
    public List<CourseSuggestion> getSuggestionsForStudent(Student student) {
        return suggestionEngine.suggestCoursesForStudent(student);
    }

    /**
     * Auto-enroll students based on suggestions.
     *
     * @param students list of students
     * @param applyRequired apply required courses only
     * @param applyRecommended apply recommended courses too
     * @return count of enrollments created
     */
    @Transactional
    public int autoEnrollStudents(List<Student> students, boolean applyRequired, boolean applyRecommended) {
        log.info("Auto-enrolling {} students (required={}, recommended={})",
            students.size(), applyRequired, applyRecommended);

        int totalEnrolled = 0;

        for (Student student : students) {
            List<CourseSuggestion> suggestions = getSuggestionsForStudent(student);

            for (CourseSuggestion suggestion : suggestions) {
                // Apply based on priority
                boolean shouldApply = false;

                if (applyRequired && suggestion.getPriority() == CourseSuggestion.Priority.REQUIRED) {
                    shouldApply = true;
                }

                if (applyRecommended && suggestion.getPriority() == CourseSuggestion.Priority.RECOMMENDED) {
                    shouldApply = true;
                }

                if (shouldApply && suggestion.isPrerequisitesMet()) {
                    Course course = suggestion.getCourse();
                    if (!isStudentEnrolled(student, course)) {
                        student.getEnrolledCourses().add(course);
                        totalEnrolled++;
                        log.debug("Enrolled {} in {}", student.getStudentId(), course.getCourseCode());
                    }
                }
            }
        }

        studentRepository.saveAll(students);

        log.info("Auto-enrolled {} courses total", totalEnrolled);
        return totalEnrolled;
    }

    /**
     * Copy enrollments from previous year with course mapping.
     *
     * @param student the student
     * @param courseMapping map of old course to new course (e.g., Eng 9 → Eng 10)
     * @return count of enrollments copied
     */
    @Transactional
    public int copyPreviousYearEnrollments(Student student, Map<Course, Course> courseMapping) {
        log.info("Copying previous year enrollments for {}", student.getStudentId());

        // Get student's previous enrollment history
        List<StudentEnrollmentHistory> previousEnrollments = enrollmentHistoryRepository.findByStudent(student);

        if (previousEnrollments.isEmpty()) {
            log.info("No previous enrollments found for student {}", student.getStudentId());
            return 0;
        }

        // Filter to completed courses only
        List<StudentEnrollmentHistory> completedEnrollments = previousEnrollments.stream()
            .filter(h -> "COMPLETED".equals(h.getStatus()))
            .collect(Collectors.toList());

        int enrolledCount = 0;

        for (StudentEnrollmentHistory history : completedEnrollments) {
            Course oldCourse = history.getCourse();
            Course newCourse = courseMapping.get(oldCourse);

            if (newCourse != null) {
                // Check if student is already enrolled in the new course
                if (!isStudentEnrolled(student, newCourse)) {
                    // Check capacity
                    if (newCourse.getMaxStudents() == null ||
                        getCurrentEnrollmentCount(newCourse) < newCourse.getMaxStudents()) {

                        // Enroll student in the mapped course
                        if (student.getEnrolledCourses() == null) {
                            student.setEnrolledCourses(new ArrayList<>());
                        }
                        student.getEnrolledCourses().add(newCourse);
                        enrolledCount++;

                        log.debug("Enrolled {} in {} (mapped from {})",
                            student.getStudentId(), newCourse.getCourseCode(), oldCourse.getCourseCode());
                    } else {
                        log.warn("Course {} is full, cannot enroll {}",
                            newCourse.getCourseCode(), student.getStudentId());
                    }
                }
            }
        }

        if (enrolledCount > 0) {
            studentRepository.save(student);
            log.info("Enrolled student {} in {} courses from previous year mapping",
                student.getStudentId(), enrolledCount);
        }

        return enrolledCount;
    }

    /**
     * Check if student is enrolled in course.
     */
    private boolean isStudentEnrolled(Student student, Course course) {
        return student.getEnrolledCourses() != null &&
               student.getEnrolledCourses().contains(course);
    }

    /**
     * Check if student has prerequisites for course.
     */
    private boolean hasPrerequisites(Student student, Course course) {
        // For now, simplified - assume they meet prerequisites
        // Real implementation would check enrollment history
        return true;
    }

    /**
     * Get current enrollment count for a course.
     */
    private int getCurrentEnrollmentCount(Course course) {
        // Count students enrolled in this course
        List<Student> allStudents = studentRepository.findByActiveTrue();
        return (int) allStudents.stream()
            .filter(s -> isStudentEnrolled(s, course))
            .count();
    }

    /**
     * Find students missing required courses.
     *
     * @param gradeLevel grade level to check
     * @return list of students missing core courses
     */
    public List<Student> findStudentsMissingRequiredCourses(int gradeLevel) {
        List<Student> students = studentRepository.findByGradeLevelAndActiveTrue(String.valueOf(gradeLevel));
        List<Student> missingCourses = new ArrayList<>();

        for (Student student : students) {
            if (student.getEnrolledCourseCount() < 7) {
                missingCourses.add(student);
            }
        }

        log.info("Found {} students in grade {} missing required courses", missingCourses.size(), gradeLevel);
        return missingCourses;
    }
}
