package com.heronix;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diagnostic test to identify schedule generation constraint violations
 * December 5, 2025
 */
@SpringBootTest
public class ScheduleGenerationDiagnosticTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Test
    public void diagnoseScheduleGenerationIssues() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SCHEDULE GENERATION DIAGNOSTIC REPORT");
        System.out.println("=".repeat(80) + "\n");

        // ISSUE #1: Courses without teachers
        diagnoseCoursesWithoutTeachers();

        // ISSUE #2: Room type mismatches
        diagnoseRoomTypeMismatches();

        // ISSUE #3: Room capacity issues
        diagnoseRoomCapacityIssues();

        // ISSUE #4: Teacher overload
        diagnoseTeacherOverload();

        // ISSUE #5: Enrollment data
        diagnoseEnrollmentData();

        // SUMMARY
        printSummary();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("DIAGNOSTIC COMPLETE");
        System.out.println("=".repeat(80) + "\n");
    }

    private void diagnoseCoursesWithoutTeachers() {
        System.out.println("### ISSUE #1: COURSES WITHOUT TEACHER ASSIGNMENTS ###\n");

        List<Course> allCourses = courseRepository.findAll();
        List<Long> activeTeacherIds = teacherRepository.findAllActive()
                .stream()
                .map(Teacher::getId)
                .collect(Collectors.toList());

        List<Course> coursesWithoutTeachers = allCourses.stream()
                .filter(c -> c.getTeacher() == null || !activeTeacherIds.contains(c.getTeacher().getId()))
                .collect(Collectors.toList());

        if (coursesWithoutTeachers.isEmpty()) {
            System.out.println("  ✓ All courses have active teacher assignments\n");
        } else {
            System.out.println("  ❌ CRITICAL: " + coursesWithoutTeachers.size() + " courses without active teachers:\n");
            for (Course course : coursesWithoutTeachers) {
                System.out.printf("    - %s (%s) - Subject: %s - Min Grade: %s - Max Grade: %s%n",
                        course.getCourseCode(),
                        course.getCourseName(),
                        course.getSubject(),
                        course.getMinGradeLevel(),
                        course.getMaxGradeLevel());
                if (course.getTeacher() != null) {
                    Teacher t = course.getTeacher();
                    System.out.printf("      Assigned to: %s %s (INACTIVE)%n",
                            t.getFirstName(), t.getLastName());
                }
            }
            System.out.println("\n  FIX: Assign active teachers to these courses\n");
        }
    }

    private void diagnoseRoomTypeMismatches() {
        System.out.println("### ISSUE #2: ROOM TYPE REQUIREMENTS vs AVAILABLE ROOMS ###\n");

        List<Course> allCourses = courseRepository.findAll();
        List<Room> availableRooms = roomRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                .collect(Collectors.toList());

        // Get distinct room type requirements
        Map<String, Long> requiredRoomTypes = allCourses.stream()
                .filter(c -> c.getRequiredRoomType() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getRequiredRoomType().toString(),
                        Collectors.counting()
                ));

        // Get available room types
        Map<String, Long> availableRoomTypes = availableRooms.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRoomType() != null ? r.getRoomType().toString() : "STANDARD_CLASSROOM",
                        Collectors.counting()
                ));

        System.out.println("  Room Types Required by Courses:");
        if (requiredRoomTypes.isEmpty()) {
            System.out.println("    - No specific room type requirements");
        } else {
            requiredRoomTypes.forEach((type, count) ->
                    System.out.printf("    - %s: %d courses%n", type, count));
        }

        System.out.println("\n  Room Types Available:");
        availableRoomTypes.forEach((type, count) ->
                System.out.printf("    - %s: %d rooms%n", type, count));

        // Find mismatches
        System.out.println("\n  Checking for mismatches...");
        boolean foundMismatch = false;
        for (Map.Entry<String, Long> entry : requiredRoomTypes.entrySet()) {
            String requiredType = entry.getKey();
            if (!availableRoomTypes.containsKey(requiredType) || availableRoomTypes.get(requiredType) == 0) {
                foundMismatch = true;
                System.out.printf("  ❌ CRITICAL: %d courses need %s but 0 available%n",
                        entry.getValue(), requiredType);

                // List the courses
                allCourses.stream()
                        .filter(c -> c.getRequiredRoomType() != null &&
                                c.getRequiredRoomType().toString().equals(requiredType))
                        .forEach(c -> System.out.printf("      - %s (%s)%n",
                                c.getCourseCode(), c.getCourseName()));
            }
        }

        if (!foundMismatch) {
            System.out.println("  ✓ All required room types are available\n");
        } else {
            System.out.println("\n  FIX: Add rooms of the required types or change course requirements\n");
        }
    }

    private void diagnoseRoomCapacityIssues() {
        System.out.println("### ISSUE #3: ROOM CAPACITY vs COURSE ENROLLMENT ###\n");

        List<Course> allCourses = courseRepository.findAll();
        List<Room> availableRooms = roomRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                .collect(Collectors.toList());

        boolean foundIssue = false;
        for (Course course : allCourses) {
            // Get max capacity for rooms matching this course's type requirement
            int maxRoomCapacity = availableRooms.stream()
                    .filter(r -> course.getRequiredRoomType() == null ||
                            r.getRoomType() == course.getRequiredRoomType())
                    .mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0)
                    .max()
                    .orElse(0);

            if (course.getMaxStudents() != null && course.getMaxStudents() > maxRoomCapacity) {
                foundIssue = true;
                System.out.printf("  ❌ %s (%s):%n", course.getCourseCode(), course.getCourseName());
                System.out.printf("      Course capacity: %d students%n", course.getMaxStudents());
                System.out.printf("      Max room capacity: %d students%n", maxRoomCapacity);
                System.out.printf("      Room type needed: %s%n",
                        course.getRequiredRoomType() != null ? course.getRequiredRoomType() : "ANY");
            }
        }

        if (!foundIssue) {
            System.out.println("  ✓ All courses fit in available rooms\n");
        } else {
            System.out.println("\n  FIX: Increase room capacity or split courses into sections\n");
        }
    }

    private void diagnoseTeacherOverload() {
        System.out.println("### ISSUE #4: TEACHER COURSE LOAD ###\n");

        List<Teacher> activeTeachers = teacherRepository.findAllActive();
        List<Course> allCourses = courseRepository.findAll();

        System.out.println("  Teacher Course Assignments:");
        boolean foundOverload = false;
        for (Teacher teacher : activeTeachers) {
            long courseCount = allCourses.stream()
                    .filter(c -> c.getTeacher() != null && c.getTeacher().getId().equals(teacher.getId()))
                    .count();

            int totalStudents = allCourses.stream()
                    .filter(c -> c.getTeacher() != null && c.getTeacher().getId().equals(teacher.getId()))
                    .mapToInt(c -> c.getMaxStudents() != null ? c.getMaxStudents() : 0)
                    .sum();

            System.out.printf("    %s %s (%s): %d courses, %d total students%n",
                    teacher.getFirstName(),
                    teacher.getLastName(),
                    teacher.getDepartment() != null ? teacher.getDepartment() : "No Dept",
                    courseCount,
                    totalStudents);

            if (courseCount > 8) {
                foundOverload = true;
                System.out.println("      ⚠️  WARNING: Teaching more than 8 courses");
            }
        }

        if (!foundOverload) {
            System.out.println("  ✓ No teacher overload detected\n");
        } else {
            System.out.println("\n  FIX: Redistribute courses or hire additional teachers\n");
        }
    }

    private void diagnoseEnrollmentData() {
        System.out.println("### ISSUE #5: ENROLLMENT DATA ###\n");

        long totalEnrollments = studentEnrollmentRepository.count();

        if (totalEnrollments == 0) {
            System.out.println("  ⚠️  WARNING: No enrollment data found");
            System.out.println("  NOTE: Enrollments may not be required for schedule generation");
            System.out.println("        but are helpful for room capacity validation\n");
            return;
        }

        long distinctStudents = studentEnrollmentRepository.findAll().stream()
                .map(e -> e.getStudent().getId())
                .distinct()
                .count();
        long distinctCourses = studentEnrollmentRepository.findAll().stream()
                .map(e -> e.getCourse().getId())
                .distinct()
                .count();

        System.out.printf("  Total enrollments: %d%n", totalEnrollments);
        System.out.printf("  Students enrolled: %d%n", distinctStudents);
        System.out.printf("  Courses with enrollments: %d%n", distinctCourses);

        List<Course> allCourses = courseRepository.findAll();
        List<Long> coursesWithEnrollments = studentEnrollmentRepository.findAll().stream()
                .map(e -> e.getCourse().getId())
                .distinct()
                .collect(Collectors.toList());

        List<Course> coursesWithoutEnrollments = allCourses.stream()
                .filter(c -> !coursesWithEnrollments.contains(c.getId()))
                .collect(Collectors.toList());

        if (coursesWithoutEnrollments.isEmpty()) {
            System.out.println("  ✓ All courses have student enrollments\n");
        } else {
            System.out.printf("  ⚠️  INFO: %d courses have no enrollments (may be OK if using max capacity):%n",
                    coursesWithoutEnrollments.size());
            coursesWithoutEnrollments.stream()
                    .limit(10)
                    .forEach(c -> System.out.printf("    - %s (%s)%n", c.getCourseCode(), c.getCourseName()));
            if (coursesWithoutEnrollments.size() > 10) {
                System.out.printf("    ... and %d more%n", coursesWithoutEnrollments.size() - 10);
            }
            System.out.println();
        }
    }

    private void printSummary() {
        System.out.println("### DATA READINESS SUMMARY ###\n");

        long totalTeachers = teacherRepository.count();
        long activeTeachers = teacherRepository.countByActiveTrue();
        long totalCourses = courseRepository.count();
        long coursesWithTeachers = courseRepository.findAll().stream()
                .filter(c -> c.getTeacher() != null)
                .count();
        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                .count();
        long totalStudents = studentRepository.count();

        System.out.printf("  Teachers: %d total, %d active (%.1f%%)%n",
                totalTeachers, activeTeachers,
                totalTeachers > 0 ? (activeTeachers * 100.0 / totalTeachers) : 0);
        System.out.printf("  Courses: %d total, %d with teachers (%.1f%%)%n",
                totalCourses, coursesWithTeachers,
                totalCourses > 0 ? (coursesWithTeachers * 100.0 / totalCourses) : 0);
        System.out.printf("  Rooms: %d total, %d available (%.1f%%)%n",
                totalRooms, availableRooms,
                totalRooms > 0 ? (availableRooms * 100.0 / totalRooms) : 0);
        System.out.printf("  Students: %d total%n", totalStudents);
        System.out.println();
    }
}
