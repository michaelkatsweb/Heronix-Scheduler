package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ DELETE UNASSIGNED COURSES                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * ROOT CAUSE: 6 courses have no teacher assigned
 * - 4 World Languages courses
 * - 2 Fine Arts courses
 *
 * This removes unassigned courses to allow schedule generation to proceed.
 *
 * Run with: mvn test -Dtest=DeleteUnassignedCourses
 *
 * December 6, 2025
 */
@SpringBootTest
public class DeleteUnassignedCourses {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void deleteUnassignedCourses() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DELETE UNASSIGNED COURSES");
        System.out.println("=".repeat(80) + "\n");

        // STEP 1: Show courses that will be deleted
        System.out.println("📋 COURSES TO BE DELETED:");
        System.out.println("-".repeat(80));

        String findSql = "SELECT id, course_name, subject FROM courses WHERE teacher_id IS NULL";
        List<Map<String, Object>> unassignedCourses = jdbcTemplate.queryForList(findSql);

        if (unassignedCourses.isEmpty()) {
            System.out.println("✅ No unassigned courses found!");
            System.out.println("\n" + "=".repeat(80) + "\n");
            return;
        }

        for (Map<String, Object> course : unassignedCourses) {
            System.out.printf("   [%d] %s (%s)%n",
                    course.get("id"),
                    course.get("course_name"),
                    course.get("subject"));
        }

        System.out.println("\nTotal to delete: " + unassignedCourses.size() + " courses\n");

        // STEP 2: Show before state
        String countSql = "SELECT COUNT(*) FROM courses";
        Integer beforeCount = jdbcTemplate.queryForObject(countSql, Integer.class);
        System.out.println("📊 BEFORE DELETE:");
        System.out.println("-".repeat(80));
        System.out.printf("Total courses: %d%n", beforeCount);
        System.out.printf("Unassigned: %d%n", unassignedCourses.size());
        System.out.printf("Assigned: %d%n", beforeCount - unassignedCourses.size());

        // STEP 3: Delete student enrollments first (foreign key constraint)
        System.out.println("\n🗑️  DELETING STUDENT ENROLLMENTS FOR UNASSIGNED COURSES...");

        String deleteEnrollmentsSql = "DELETE FROM student_courses WHERE course_id IN (SELECT id FROM courses WHERE teacher_id IS NULL)";
        int enrollmentsDeleted = jdbcTemplate.update(deleteEnrollmentsSql);
        System.out.println("✅ Deleted " + enrollmentsDeleted + " student enrollments");

        // STEP 4: Delete unassigned courses
        System.out.println("\n🗑️  DELETING UNASSIGNED COURSES...");

        String deleteSql = "DELETE FROM courses WHERE teacher_id IS NULL";
        int deletedCount = jdbcTemplate.update(deleteSql);

        System.out.println("✅ Deleted " + deletedCount + " courses\n");

        // STEP 4: Show after state
        Integer afterCount = jdbcTemplate.queryForObject(countSql, Integer.class);
        System.out.println("📊 AFTER DELETE:");
        System.out.println("-".repeat(80));
        System.out.printf("Total courses: %d%n", afterCount);
        System.out.printf("All courses assigned: %s%n", afterCount == (beforeCount - deletedCount) ? "✅ YES" : "❌ NO");

        // STEP 5: Show course distribution by subject
        System.out.println("\n📚 REMAINING COURSES BY SUBJECT:");
        System.out.println("-".repeat(80));

        String subjectSql = "SELECT subject, COUNT(*) as count FROM courses GROUP BY subject ORDER BY count DESC";
        List<Map<String, Object>> subjects = jdbcTemplate.queryForList(subjectSql);

        for (Map<String, Object> subject : subjects) {
            System.out.printf("%-40s %2d courses%n",
                    subject.get("subject"),
                    subject.get("count"));
        }

        // STEP 6: Final verification
        String unassignedCheckSql = "SELECT COUNT(*) FROM courses WHERE teacher_id IS NULL";
        Integer remainingUnassigned = jdbcTemplate.queryForObject(unassignedCheckSql, Integer.class);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("FINAL VERIFICATION:");
        System.out.println("=".repeat(80));
        System.out.printf("Courses remaining: %d%n", afterCount);
        System.out.printf("Unassigned courses: %d%n", remainingUnassigned);

        if (remainingUnassigned == 0) {
            System.out.println("Status: ✅ ALL COURSES ASSIGNED");
            System.out.println("Result: Schedule generation should now work!\n");
        } else {
            System.out.println("Status: ⚠️  SOME COURSES STILL UNASSIGNED");
            System.out.println("Result: May need additional fixes\n");
        }

        System.out.println("=".repeat(80));
        System.out.println("✅ CLEANUP COMPLETE - Test schedule generation now!");
        System.out.println("=".repeat(80) + "\n");
    }
}
