package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Database Maintenance Service
 * Location: src/main/java/com/eduscheduler/service/DatabaseMaintenanceService.java
 *
 * Provides utility methods for database maintenance and cleanup operations.
 * This service is particularly useful for:
 * - Clearing pre-assigned teachers/rooms from courses
 * - Resetting schedules for fresh generation
 * - Database cleanup and maintenance
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Service
public class DatabaseMaintenanceService {

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Clear all pre-assigned teachers and rooms from courses
     *
     * This is useful when:
     * - OptaPlanner is not distributing teachers/rooms properly
     * - You want a fresh schedule generation without constraints
     * - Testing schedule generation with clean data
     *
     * @return Statistics about the clearing operation
     */
    @Transactional
    public MaintenanceResult clearCourseAssignments() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║   CLEARING COURSE ASSIGNMENTS                                  ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        List<Course> allCourses = courseRepository.findAll();

        // Count courses with pre-assignments
        long coursesWithTeacher = allCourses.stream()
            .filter(c -> c.getTeacher() != null)
            .count();

        long coursesWithRoom = allCourses.stream()
            .filter(c -> c.getRoom() != null)
            .count();

        log.info("\n📊 BEFORE CLEARING:");
        log.info("   Total courses: {}", allCourses.size());
        log.info("   Courses with pre-assigned teacher: {}", coursesWithTeacher);
        log.info("   Courses with pre-assigned room: {}", coursesWithRoom);

        if (coursesWithTeacher > 0) {
            log.info("\n   Pre-assigned teachers:");
            allCourses.stream()
                .filter(c -> c.getTeacher() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getTeacher().getName(),
                    Collectors.counting()
                ))
                .forEach((teacher, count) ->
                    log.info("      - {}: {} courses", teacher, count));
        }

        if (coursesWithRoom > 0) {
            log.info("\n   Pre-assigned rooms:");
            allCourses.stream()
                .filter(c -> c.getRoom() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getRoom().getRoomNumber(),
                    Collectors.counting()
                ))
                .forEach((room, count) ->
                    log.info("      - Room {}: {} courses", room, count));
        }

        // Clear all assignments
        log.info("\n🔧 CLEARING ASSIGNMENTS...");
        int clearedTeachers = 0;
        int clearedRooms = 0;

        for (Course course : allCourses) {
            if (course.getTeacher() != null) {
                course.setTeacher(null);
                clearedTeachers++;
            }
            if (course.getRoom() != null) {
                course.setRoom(null);
                clearedRooms++;
            }
        }

        // Save all courses
        courseRepository.saveAll(allCourses);

        log.info("\n✅ CLEARING COMPLETE:");
        log.info("   Cleared teacher assignments: {}", clearedTeachers);
        log.info("   Cleared room assignments: {}", clearedRooms);
        log.info("   All courses are now ready for OptaPlanner scheduling");
        log.info("\n════════════════════════════════════════════════════════════════\n");

        return new MaintenanceResult(
            allCourses.size(),
            (int) coursesWithTeacher,
            (int) coursesWithRoom,
            clearedTeachers,
            clearedRooms
        );
    }

    /**
     * Check for pre-assigned courses without clearing
     *
     * @return Statistics about current course assignments
     */
    @Transactional(readOnly = true)
    public MaintenanceResult checkCourseAssignments() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║   CHECKING COURSE ASSIGNMENTS                                  ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        List<Course> allCourses = courseRepository.findAll();

        long coursesWithTeacher = allCourses.stream()
            .filter(c -> c.getTeacher() != null)
            .count();

        long coursesWithRoom = allCourses.stream()
            .filter(c -> c.getRoom() != null)
            .count();

        log.info("\n📊 CURRENT STATUS:");
        log.info("   Total courses: {}", allCourses.size());
        log.info("   Courses with pre-assigned teacher: {}", coursesWithTeacher);
        log.info("   Courses with pre-assigned room: {}", coursesWithRoom);

        if (coursesWithTeacher == 0 && coursesWithRoom == 0) {
            log.info("\n✅ No pre-assignments found - OptaPlanner has full flexibility");
        } else {
            log.warn("\n⚠️ WARNING: Pre-assignments detected!");
            log.warn("   OptaPlanner may respect these assignments.");
            log.warn("   Use clearCourseAssignments() to remove them.");

            if (coursesWithTeacher > 0) {
                log.warn("\n   Pre-assigned teachers:");
                allCourses.stream()
                    .filter(c -> c.getTeacher() != null)
                    .collect(Collectors.groupingBy(
                        c -> c.getTeacher().getName(),
                        Collectors.counting()
                    ))
                    .forEach((teacher, count) ->
                        log.warn("      - {}: {} courses", teacher, count));
            }

            if (coursesWithRoom > 0) {
                log.warn("\n   Pre-assigned rooms:");
                allCourses.stream()
                    .filter(c -> c.getRoom() != null)
                    .collect(Collectors.groupingBy(
                        c -> c.getRoom().getRoomNumber(),
                        Collectors.counting()
                    ))
                    .forEach((room, count) ->
                        log.warn("      - Room {}: {} courses", room, count));
            }
        }

        log.info("\n════════════════════════════════════════════════════════════════\n");

        return new MaintenanceResult(
            allCourses.size(),
            (int) coursesWithTeacher,
            (int) coursesWithRoom,
            0,
            0
        );
    }

    /**
     * Clear assignments for specific courses
     *
     * @param courseCodes List of course codes to clear
     * @return Statistics about the clearing operation
     */
    @Transactional
    public MaintenanceResult clearSpecificCourseAssignments(List<String> courseCodes) {
        log.info("Clearing assignments for {} specific courses", courseCodes.size());

        List<Course> courses = courseRepository.findAllByCourseCodeIn(courseCodes);

        int clearedTeachers = 0;
        int clearedRooms = 0;
        int coursesWithTeacher = 0;
        int coursesWithRoom = 0;

        for (Course course : courses) {
            if (course.getTeacher() != null) {
                coursesWithTeacher++;
                course.setTeacher(null);
                clearedTeachers++;
            }
            if (course.getRoom() != null) {
                coursesWithRoom++;
                course.setRoom(null);
                clearedRooms++;
            }
        }

        courseRepository.saveAll(courses);

        log.info("✅ Cleared {} teacher assignments and {} room assignments from {} courses",
            clearedTeachers, clearedRooms, courses.size());

        return new MaintenanceResult(
            courses.size(),
            coursesWithTeacher,
            coursesWithRoom,
            clearedTeachers,
            clearedRooms
        );
    }

    /**
     * Result object for maintenance operations
     */
    public static class MaintenanceResult {
        private final int totalCourses;
        private final int coursesWithTeacherBefore;
        private final int coursesWithRoomBefore;
        private final int teachersCleared;
        private final int roomsCleared;

        public MaintenanceResult(int totalCourses, int coursesWithTeacherBefore,
                               int coursesWithRoomBefore, int teachersCleared, int roomsCleared) {
            this.totalCourses = totalCourses;
            this.coursesWithTeacherBefore = coursesWithTeacherBefore;
            this.coursesWithRoomBefore = coursesWithRoomBefore;
            this.teachersCleared = teachersCleared;
            this.roomsCleared = roomsCleared;
        }

        public int getTotalCourses() {
            return totalCourses;
        }

        public int getCoursesWithTeacherBefore() {
            return coursesWithTeacherBefore;
        }

        public int getCoursesWithRoomBefore() {
            return coursesWithRoomBefore;
        }

        public int getTeachersCleared() {
            return teachersCleared;
        }

        public int getRoomsCleared() {
            return roomsCleared;
        }

        public boolean hadPreAssignments() {
            return coursesWithTeacherBefore > 0 || coursesWithRoomBefore > 0;
        }

        public String getSummary() {
            return String.format(
                "Total courses: %d | Pre-assigned teachers: %d | Pre-assigned rooms: %d | Cleared: %d teachers, %d rooms",
                totalCourses, coursesWithTeacherBefore, coursesWithRoomBefore, teachersCleared, roomsCleared
            );
        }

        @Override
        public String toString() {
            return getSummary();
        }
    }
}
