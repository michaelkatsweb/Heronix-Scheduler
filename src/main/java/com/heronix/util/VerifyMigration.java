package com.heronix.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Verify Phase 4B migration completed successfully
 */
public class VerifyMigration {

    public static void main(String[] args) {
        String url = "jdbc:h2:file:./data/eduscheduler;AUTO_SERVER=TRUE;MODE=LEGACY;IFEXISTS=TRUE";
        String user = "sa";
        String password = "";

        System.out.println("=".repeat(70));
        System.out.println("Phase 4B Migration Verification");
        System.out.println("=".repeat(70));
        System.out.println();

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("✓ Connected to database");
            System.out.println();

            // Verify columns exist
            System.out.println("Checking for new columns...");
            ResultSet rs = stmt.executeQuery(
                "SELECT column_name, data_type " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE table_name = 'COURSES' " +
                "AND column_name IN ('COURSE_CATEGORY', 'MIN_GRADE_LEVEL', 'MAX_GRADE_LEVEL')"
            );

            int count = 0;
            while (rs.next()) {
                String colName = rs.getString("column_name");
                String colType = rs.getString("data_type");
                System.out.println("  ✓ " + colName + " (" + colType + ")");
                count++;
            }

            System.out.println();

            if (count == 3) {
                System.out.println("=".repeat(70));
                System.out.println("✅ Migration verified successfully!");
                System.out.println("   All 3 columns exist in the courses table:");
                System.out.println("   - course_category (VARCHAR)");
                System.out.println("   - min_grade_level (INTEGER)");
                System.out.println("   - max_grade_level (INTEGER)");
                System.out.println("=".repeat(70));

                // Check how many courses have been updated
                rs = stmt.executeQuery("SELECT COUNT(*) as total FROM courses");
                if (rs.next()) {
                    int total = rs.getInt("total");
                    System.out.println();
                    System.out.println("Total courses in database: " + total);
                }

                rs = stmt.executeQuery("SELECT COUNT(*) as with_category FROM courses WHERE course_category IS NOT NULL");
                if (rs.next()) {
                    int withCategory = rs.getInt("with_category");
                    System.out.println("Courses with category set: " + withCategory);
                }

                rs = stmt.executeQuery("SELECT COUNT(*) as with_grade FROM courses WHERE min_grade_level IS NOT NULL OR max_grade_level IS NOT NULL");
                if (rs.next()) {
                    int withGrade = rs.getInt("with_grade");
                    System.out.println("Courses with grade levels: " + withGrade);
                }

                System.out.println();
                System.out.println("Next steps:");
                System.out.println("1. Load sample data to see examples of grade-level courses");
                System.out.println("2. Test course eligibility methods in your application");
                System.out.println("3. Update CourseDialog UI to edit these fields");
                System.out.println();

            } else {
                System.out.println("⚠ Warning: Expected 3 columns but found " + count);
                System.out.println("   Migration may not be complete.");
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Verification failed:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
