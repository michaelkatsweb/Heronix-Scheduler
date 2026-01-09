package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Diagnose Hard Constraint Violations
 * December 6, 2025
 *
 * Schedule generation failed with 521 hard violations.
 * This diagnostic will identify the root cause.
 */
@SpringBootTest
public class DiagnoseHardViolations {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void diagnoseHardViolations() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HARD CONSTRAINT VIOLATION DIAGNOSTIC");
        System.out.println("521 VIOLATIONS DETECTED - FINDING ROOT CAUSE");
        System.out.println("=".repeat(80) + "\n");

        // Check 1: Courses without teachers
        checkCoursesWithoutTeachers();

        // Check 2: Room capacity vs course enrollment
        checkRoomCapacity();

        // Check 3: Room type matching
        checkRoomTypeMatching();

        // Check 4: Lab requirements
        checkLabRequirements();

        // Check 5: Lunch period configuration
        checkLunchConfiguration();

        // Check 6: Time slot availability
        checkTimeSlots();

        // Check 7: Teacher availability
        checkTeacherAvailability();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("DIAGNOSTIC COMPLETE");
        System.out.println("=".repeat(80) + "\n");
    }

    private void checkCoursesWithoutTeachers() {
        System.out.println("CHECK 1: COURSES WITHOUT TEACHERS");
        System.out.println("-".repeat(80));

        String sql = "SELECT COUNT(*) FROM courses WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        if (count > 0) {
            System.out.println("❌ CRITICAL: " + count + " courses without teachers!");

            String detailSql = "SELECT course_name, subject FROM courses WHERE active = TRUE AND (teacher_id IS NULL OR teacher_id = 0)";
            List<Map<String, Object>> courses = jdbcTemplate.queryForList(detailSql);

            for (Map<String, Object> course : courses) {
                System.out.printf("   - %s (%s)%n", course.get("course_name"), course.get("subject"));
            }
        } else {
            System.out.println("✅ All courses have teachers assigned");
        }
        System.out.println();
    }

    private void checkRoomCapacity() {
        System.out.println("CHECK 2: ROOM CAPACITY VS ENROLLMENTS");
        System.out.println("-".repeat(80));

        String sql = "SELECT c.course_name, c.id as course_id, " +
                     "COUNT(sc.student_id) as enrolled, " +
                     "MIN(r.capacity) as min_room_capacity " +
                     "FROM courses c " +
                     "LEFT JOIN student_courses sc ON c.id = sc.course_id " +
                     "LEFT JOIN rooms r ON r.available = TRUE " +
                     "WHERE c.active = TRUE " +
                     "GROUP BY c.id, c.course_name " +
                     "HAVING COUNT(sc.student_id) > 0";

        List<Map<String, Object>> courses = jdbcTemplate.queryForList(sql);

        int issues = 0;
        for (Map<String, Object> course : courses) {
            int enrolled = ((Number) course.get("enrolled")).intValue();
            Number minCapObj = (Number) course.get("min_room_capacity");
            int minCap = (minCapObj != null) ? minCapObj.intValue() : 0;

            if (enrolled > minCap) {
                System.out.printf("❌ %s: %d students, min room capacity: %d%n",
                    course.get("course_name"), enrolled, minCap);
                issues++;
            }
        }

        if (issues == 0) {
            System.out.println("✅ All enrolled courses fit in available rooms");
        } else {
            System.out.println("\n⚠️  " + issues + " courses have more students than room capacity!");
        }
        System.out.println();
    }

    private void checkRoomTypeMatching() {
        System.out.println("CHECK 3: ROOM TYPE MATCHING");
        System.out.println("-".repeat(80));

        // Check for courses that need specific room types
        String sql = "SELECT subject, COUNT(*) as course_count FROM courses WHERE active = TRUE GROUP BY subject";
        List<Map<String, Object>> subjects = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> subject : subjects) {
            String subj = (String) subject.get("subject");
            int count = ((Number) subject.get("course_count")).intValue();

            String roomType = null;
            if (subj != null) {
                if (subj.contains("Science")) roomType = "SCIENCE_LAB";
                else if (subj.contains("Physical Education")) roomType = "GYMNASIUM";
                else if (subj.contains("Art")) roomType = "ART_STUDIO";
                else if (subj.contains("Music")) roomType = "MUSIC_ROOM";
                else if (subj.contains("Computer")) roomType = "COMPUTER_LAB";
            }

            if (roomType != null) {
                String roomSql = "SELECT COUNT(*) FROM rooms WHERE room_type = ? AND available = TRUE";
                Integer roomCount = jdbcTemplate.queryForObject(roomSql, Integer.class, roomType);

                if (roomCount < count) {
                    System.out.printf("❌ %s: Need %d %s rooms, have %d%n",
                        subj, count, roomType, roomCount);
                } else {
                    System.out.printf("✅ %s: Need %d %s rooms, have %d%n",
                        subj, count, roomType, roomCount);
                }
            }
        }
        System.out.println();
    }

    private void checkLabRequirements() {
        System.out.println("CHECK 4: LAB REQUIREMENTS");
        System.out.println("-".repeat(80));

        String sql = "SELECT COUNT(*) FROM courses WHERE active = TRUE AND requires_lab = TRUE";
        Integer labCourses = jdbcTemplate.queryForObject(sql, Integer.class);

        String roomSql = "SELECT COUNT(*) FROM rooms WHERE room_type IN ('SCIENCE_LAB', 'COMPUTER_LAB') AND available = TRUE";
        Integer labRooms = jdbcTemplate.queryForObject(roomSql, Integer.class);

        System.out.printf("Lab courses: %d%n", labCourses);
        System.out.printf("Lab rooms: %d%n", labRooms);

        if (labCourses > labRooms) {
            System.out.println("❌ Not enough lab rooms for lab courses!");
        } else {
            System.out.println("✅ Sufficient lab rooms");
        }
        System.out.println();
    }

    private void checkLunchConfiguration() {
        System.out.println("CHECK 5: LUNCH PERIOD CONFIGURATION");
        System.out.println("-".repeat(80));

        // Check if students have lunch wave assignments
        String studentSql = "SELECT COUNT(*) FROM students WHERE lunch_wave IS NULL";
        Integer studentsNoLunch = jdbcTemplate.queryForObject(studentSql, Integer.class);

        // Check if teachers have lunch wave assignments
        String teacherSql = "SELECT COUNT(*) FROM teachers WHERE active = TRUE AND lunch_wave IS NULL";
        Integer teachersNoLunch = jdbcTemplate.queryForObject(teacherSql, Integer.class);

        if (studentsNoLunch > 0) {
            System.out.printf("❌ %d students without lunch wave assignment!%n", studentsNoLunch);
        } else {
            System.out.println("✅ All students have lunch wave");
        }

        if (teachersNoLunch > 0) {
            System.out.printf("❌ %d teachers without lunch wave assignment!%n", teachersNoLunch);
        } else {
            System.out.println("✅ All teachers have lunch wave");
        }
        System.out.println();
    }

    private void checkTimeSlots() {
        System.out.println("CHECK 6: TIME SLOTS");
        System.out.println("-".repeat(80));

        // TimeSlots are in-memory, but we can check if the system initialized them
        System.out.println("ℹ️  Time slots are in-memory (40 slots initialized on startup)");
        System.out.println("   Monday-Friday, 8 periods/day, 8:00 AM - 4:00 PM");
        System.out.println();
    }

    private void checkTeacherAvailability() {
        System.out.println("CHECK 7: TEACHER AVAILABILITY");
        System.out.println("-".repeat(80));

        // Check for teachers with availability restrictions
        String sql = "SELECT COUNT(*) FROM teachers WHERE active = TRUE";
        Integer totalTeachers = jdbcTemplate.queryForObject(sql, Integer.class);

        System.out.printf("Active teachers: %d%n", totalTeachers);
        System.out.println("✅ All teachers assumed available (no restrictions configured)");
        System.out.println();
    }
}
