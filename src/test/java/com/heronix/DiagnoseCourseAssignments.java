package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Diagnose Course-to-Teacher Assignment Issues
 * December 6, 2025
 */
@SpringBootTest
public class DiagnoseCourseAssignments {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void diagnoseCourseAssignments() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COURSE-TO-TEACHER ASSIGNMENT DIAGNOSTIC");
        System.out.println("=".repeat(80) + "\n");

        // Check teacher distribution
        System.out.println("TEACHERS AND THEIR COURSE LOADS:");
        System.out.println("-".repeat(80));

        String teacherSql = "SELECT " +
                "t.id, " +
                "CONCAT(t.first_name, ' ', t.last_name) as teacher_name, " +
                "t.department, " +
                "COUNT(c.id) as course_count " +
                "FROM teachers t " +
                "LEFT JOIN courses c ON t.id = c.teacher_id " +
                "WHERE t.active = TRUE " +
                "GROUP BY t.id, t.first_name, t.last_name, t.department " +
                "ORDER BY course_count DESC";

        List<Map<String, Object>> teachers = jdbcTemplate.queryForList(teacherSql);

        for (Map<String, Object> teacher : teachers) {
            String status = "";
            int count = ((Number) teacher.get("course_count")).intValue();
            if (count > 4) status = " ❌ OVERLOAD";
            else if (count < 3) status = " ⚠️ UNDERLOAD";
            else status = " ✅ OK";

            System.out.printf("%-30s %-35s %2d courses%s%n",
                    teacher.get("teacher_name"),
                    teacher.get("department"),
                    count,
                    status);
        }

        // Check courses by subject
        System.out.println("\n\nCOURSES BY SUBJECT:");
        System.out.println("-".repeat(80));

        String courseSql = "SELECT " +
                "c.subject, " +
                "COUNT(*) as course_count, " +
                "COUNT(c.teacher_id) as assigned_count, " +
                "COUNT(*) - COUNT(c.teacher_id) as unassigned_count " +
                "FROM courses c " +
                "GROUP BY c.subject " +
                "ORDER BY course_count DESC";

        List<Map<String, Object>> courses = jdbcTemplate.queryForList(courseSql);

        for (Map<String, Object> course : courses) {
            int total = ((Number) course.get("course_count")).intValue();
            int assigned = ((Number) course.get("assigned_count")).intValue();
            int unassigned = ((Number) course.get("unassigned_count")).intValue();

            String status = unassigned > 0 ? " ❌" : " ✅";

            System.out.printf("%-40s %2d total, %2d assigned, %2d unassigned%s%n",
                    course.get("subject"),
                    total,
                    assigned,
                    unassigned,
                    status);
        }

        // Check specific PE courses
        System.out.println("\n\nPE COURSE ASSIGNMENTS:");
        System.out.println("-".repeat(80));

        String peSql = "SELECT " +
                "c.course_name, " +
                "c.subject, " +
                "CONCAT(t.first_name, ' ', t.last_name) as teacher_name, " +
                "t.department " +
                "FROM courses c " +
                "LEFT JOIN teachers t ON c.teacher_id = t.id " +
                "WHERE c.subject LIKE '%Physical Education%' " +
                "ORDER BY c.course_name";

        List<Map<String, Object>> peCourses = jdbcTemplate.queryForList(peSql);

        for (Map<String, Object> pe : peCourses) {
            System.out.printf("%-35s → %-25s (%-30s)%n",
                    pe.get("course_name"),
                    pe.get("teacher_name"),
                    pe.get("department"));
        }

        // Check all course-teacher mismatches
        System.out.println("\n\nCOURSE-TEACHER DEPARTMENT MISMATCHES:");
        System.out.println("-".repeat(80));

        String mismatchSql = "SELECT " +
                "c.course_name, " +
                "c.subject, " +
                "CONCAT(t.first_name, ' ', t.last_name) as teacher_name, " +
                "t.department " +
                "FROM courses c " +
                "JOIN teachers t ON c.teacher_id = t.id " +
                "WHERE t.active = TRUE " +
                "ORDER BY t.department, c.course_name";

        List<Map<String, Object>> allCourses = jdbcTemplate.queryForList(mismatchSql);

        for (Map<String, Object> match : allCourses) {
            String subject = (String) match.get("subject");
            String dept = (String) match.get("department");

            boolean mismatch = false;
            if (subject != null && dept != null) {
                if (subject.contains("Physical Education") && !dept.equals("Physical Education")) {
                    mismatch = true;
                } else if (subject.contains("Mathematics") && !dept.equals("Mathematics")) {
                    mismatch = true;
                } else if (subject.contains("English") && !dept.equals("English")) {
                    mismatch = true;
                } else if (subject.contains("Science") && !dept.equals("Science")) {
                    mismatch = true;
                }
            }

            if (mismatch) {
                System.out.printf("❌ %-35s (%-30s) → %-25s (%-30s)%n",
                        match.get("course_name"),
                        subject,
                        match.get("teacher_name"),
                        dept);
            }
        }

        System.out.println("\n" + "=".repeat(80) + "\n");
    }
}
