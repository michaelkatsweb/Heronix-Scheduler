package com.heronix.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Simple database migration utility for Phase 4B
 * Adds grade level and course category fields to courses table
 */
public class DatabaseMigration {

    public static void main(String[] args) {
        String url = "jdbc:h2:file:./data/eduscheduler;AUTO_SERVER=TRUE;MODE=LEGACY;IFEXISTS=TRUE";
        String user = "sa";
        String password = "";

        System.out.println("=".repeat(70));
        System.out.println("Phase 4B Database Migration");
        System.out.println("Adding grade level and course category fields to courses table");
        System.out.println("=".repeat(70));
        System.out.println();

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("✓ Connected to database");
            System.out.println();

            // Add course_category column
            System.out.println("Adding course_category column...");
            try {
                stmt.execute("ALTER TABLE courses ADD COLUMN course_category VARCHAR(20)");
                System.out.println("  ✓ Column 'course_category' added");
            } catch (Exception e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate")) {
                    System.out.println("  ℹ Column 'course_category' already exists");
                } else {
                    throw e;
                }
            }

            // Set default values for existing rows
            System.out.println("Setting default values...");
            int updated = stmt.executeUpdate("UPDATE courses SET course_category = 'CORE' WHERE course_category IS NULL");
            System.out.println("  ✓ Updated " + updated + " rows with default category 'CORE'");

            // Add min_grade_level column
            System.out.println("Adding min_grade_level column...");
            try {
                stmt.execute("ALTER TABLE courses ADD COLUMN min_grade_level INTEGER");
                System.out.println("  ✓ Column 'min_grade_level' added");
            } catch (Exception e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate")) {
                    System.out.println("  ℹ Column 'min_grade_level' already exists");
                } else {
                    throw e;
                }
            }

            // Add max_grade_level column
            System.out.println("Adding max_grade_level column...");
            try {
                stmt.execute("ALTER TABLE courses ADD COLUMN max_grade_level INTEGER");
                System.out.println("  ✓ Column 'max_grade_level' added");
            } catch (Exception e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate")) {
                    System.out.println("  ℹ Column 'max_grade_level' already exists");
                } else {
                    throw e;
                }
            }

            System.out.println();
            System.out.println("Verifying columns...");

            // Verify columns were added
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

            if (count == 3) {
                System.out.println();
                System.out.println("=".repeat(70));
                System.out.println("✅ Migration completed successfully!");
                System.out.println("   - course_category column added (VARCHAR)");
                System.out.println("   - min_grade_level column added (INTEGER)");
                System.out.println("   - max_grade_level column added (INTEGER)");
                System.out.println("=".repeat(70));
                System.out.println();
                System.out.println("Next steps:");
                System.out.println("1. Load sample data: SAMPLE_COURSES_WITH_GRADE_LEVELS.sql");
                System.out.println("2. Test course eligibility methods in your application");
                System.out.println("3. Update CourseDialog UI to edit these fields");
            } else {
                System.out.println();
                System.out.println("⚠ Warning: Expected 3 columns but found " + count);
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Migration failed:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
