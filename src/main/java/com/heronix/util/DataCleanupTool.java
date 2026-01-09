package com.heronix.util;

import com.heronix.model.domain.Course;
import com.heronix.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data Cleanup Tool - Removes pre-assignments to allow OptaPlanner optimization
 *
 * Run with: mvn spring-boot:run -Dspring-boot.run.arguments="--cleanup.data=true"
 *
 * This tool removes pre-assigned teachers and rooms from courses, which allows
 * OptaPlanner to freely assign resources during schedule generation.
 *
 * Location: src/main/java/com/eduscheduler/util/DataCleanupTool.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cleanup.data", havingValue = "true")
public class DataCleanupTool implements CommandLineRunner {

    @Autowired
    private CourseRepository courseRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("\n\n");
        log.info("╔══════════════════════════════════════════════════════════════════════╗");
        log.info("║                    DATA CLEANUP TOOL                                 ║");
        log.info("╚══════════════════════════════════════════════════════════════════════╝\n");

        log.info("This tool will remove pre-assigned teachers and rooms from all courses.");
        log.info("This allows OptaPlanner to freely optimize teacher and room assignments.\n");

        removePreAssignments();

        log.info("\n╔══════════════════════════════════════════════════════════════════════╗");
        log.info("║                    CLEANUP COMPLETE                                  ║");
        log.info("╚══════════════════════════════════════════════════════════════════════╝\n");

        log.info("✅ Data cleanup complete!");
        log.info("   You can now generate schedules with full OptaPlanner optimization.\n");

        log.info("Exiting application.\n");
        System.exit(0);
    }

    private void removePreAssignments() {
        List<Course> allCourses = courseRepository.findAll();

        long coursesWithTeacher = allCourses.stream()
            .filter(c -> c.getTeacher() != null)
            .count();

        long coursesWithRoom = allCourses.stream()
            .filter(c -> c.getRoom() != null)
            .count();

        log.info("═══════════════════════════════════════════════════════════════════════");
        log.info("BEFORE CLEANUP:");
        log.info("  • Total courses: {}", allCourses.size());
        log.info("  • Courses with pre-assigned teacher: {}", coursesWithTeacher);
        log.info("  • Courses with pre-assigned room: {}", coursesWithRoom);
        log.info("═══════════════════════════════════════════════════════════════════════\n");

        if (coursesWithTeacher == 0 && coursesWithRoom == 0) {
            log.info("✅ No pre-assignments found - data is already clean!");
            return;
        }

        log.info("Removing pre-assignments...\n");

        int teachersRemoved = 0;
        int roomsRemoved = 0;

        for (Course course : allCourses) {
            boolean modified = false;

            if (course.getTeacher() != null) {
                log.info("  • Removing teacher from: {} (was: {})",
                    course.getCourseName(),
                    course.getTeacher().getName());
                course.setTeacher(null);
                teachersRemoved++;
                modified = true;
            }

            if (course.getRoom() != null) {
                log.info("  • Removing room from: {} (was: {})",
                    course.getCourseName(),
                    course.getRoom().getRoomNumber());
                course.setRoom(null);
                roomsRemoved++;
                modified = true;
            }

            if (modified) {
                courseRepository.save(course);
            }
        }

        log.info("\n═══════════════════════════════════════════════════════════════════════");
        log.info("AFTER CLEANUP:");
        log.info("  • Teachers removed: {}", teachersRemoved);
        log.info("  • Rooms removed: {}", roomsRemoved);
        log.info("═══════════════════════════════════════════════════════════════════════");
    }
}
