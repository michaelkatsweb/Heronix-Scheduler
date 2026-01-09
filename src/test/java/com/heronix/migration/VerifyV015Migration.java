package com.heronix.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for V015 migration
 * Tests that Phase 1 Multi-Level Monitoring tables are created correctly
 */
@SpringBootTest
public class VerifyV015Migration {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testV015TablesCreated() {
        System.out.println("\n=================================================");
        System.out.println("VERIFYING V015 MIGRATION - PHASE 1 TABLES");
        System.out.println("=================================================\n");

        // Test classroom_grade_entries table
        testTableExists("classroom_grade_entries");
        testTableStructure("classroom_grade_entries", List.of(
            "id", "student_id", "course_id", "teacher_id", "campus_id",
            "assignment_name", "assignment_type", "assignment_date", "due_date",
            "points_earned", "points_possible", "percentage_grade", "letter_grade",
            "is_missing_work", "is_late", "is_benchmark_assessment",
            "entry_date", "last_modified", "entered_by", "notes"
        ));
        testIndexes("classroom_grade_entries", List.of(
            "idx_grade_entry_student_date",
            "idx_grade_entry_course_date",
            "idx_grade_entry_missing_work",
            "idx_grade_entry_teacher"
        ));

        // Test behavior_incidents table
        testTableExists("behavior_incidents");
        testTableStructure("behavior_incidents", List.of(
            "id", "student_id", "course_id", "teacher_id", "campus_id",
            "incident_date", "incident_time", "behavior_type", "behavior_category",
            "severity_level", "incident_location", "incident_description",
            "action_taken", "admin_referral_required", "parent_contacted",
            "contact_method", "contact_date", "witness_names", "evidence_attachments",
            "recorded_date", "recorded_by", "last_modified"
        ));
        testIndexes("behavior_incidents", List.of(
            "idx_behavior_student_date",
            "idx_behavior_type_date",
            "idx_behavior_category",
            "idx_behavior_severity",
            "idx_behavior_teacher"
        ));

        // Test teacher_observation_notes table
        testTableExists("teacher_observation_notes");
        testTableStructure("teacher_observation_notes", List.of(
            "id", "student_id", "course_id", "teacher_id", "campus_id",
            "observation_date", "observation_category", "observation_rating",
            "observation_notes", "is_flag_for_intervention",
            "intervention_type_suggested", "entry_timestamp", "last_modified"
        ));
        testIndexes("teacher_observation_notes", List.of(
            "idx_observation_student_date",
            "idx_observation_course_date",
            "idx_observation_intervention_flag",
            "idx_observation_teacher",
            "idx_observation_rating"
        ));

        System.out.println("\n=================================================");
        System.out.println("✓ ALL V015 MIGRATION VERIFICATION TESTS PASSED");
        System.out.println("=================================================\n");
    }

    private void testTableExists(String tableName) {
        System.out.println("Testing table exists: " + tableName);

        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC'";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase());

        assertNotNull(count, "Table " + tableName + " should exist");
        assertEquals(1, count.intValue(), "Table " + tableName + " should exist exactly once");

        System.out.println("  ✓ Table " + tableName + " exists");
    }

    private void testTableStructure(String tableName, List<String> expectedColumns) {
        System.out.println("\nTesting table structure: " + tableName);

        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC' " +
                    "ORDER BY ORDINAL_POSITION";

        List<String> actualColumns = jdbcTemplate.queryForList(sql, String.class, tableName.toUpperCase());

        System.out.println("  Expected columns: " + expectedColumns.size());
        System.out.println("  Actual columns: " + actualColumns.size());

        // Check that all expected columns exist (case-insensitive)
        for (String expectedColumn : expectedColumns) {
            boolean found = actualColumns.stream()
                .anyMatch(col -> col.equalsIgnoreCase(expectedColumn));

            assertTrue(found,
                "Column " + expectedColumn + " should exist in table " + tableName +
                ". Actual columns: " + actualColumns);
        }

        System.out.println("  ✓ All " + expectedColumns.size() + " expected columns found");

        // Display all columns
        System.out.println("  Columns: " + String.join(", ", actualColumns));
    }

    private void testIndexes(String tableName, List<String> expectedIndexes) {
        System.out.println("\nTesting indexes on: " + tableName);

        String sql = "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC' " +
                    "AND INDEX_NAME != 'PRIMARY' " +
                    "GROUP BY INDEX_NAME";

        List<String> actualIndexes = jdbcTemplate.queryForList(sql, String.class, tableName.toUpperCase());

        System.out.println("  Expected indexes: " + expectedIndexes.size());
        System.out.println("  Actual indexes: " + actualIndexes.size());

        // Check that all expected indexes exist (case-insensitive)
        for (String expectedIndex : expectedIndexes) {
            boolean found = actualIndexes.stream()
                .anyMatch(idx -> idx.equalsIgnoreCase(expectedIndex));

            assertTrue(found,
                "Index " + expectedIndex + " should exist on table " + tableName +
                ". Actual indexes: " + actualIndexes);
        }

        System.out.println("  ✓ All " + expectedIndexes.size() + " expected indexes found");
        System.out.println("  Indexes: " + String.join(", ", actualIndexes));
    }

    @Test
    public void testForeignKeyConstraints() {
        System.out.println("\n=================================================");
        System.out.println("VERIFYING FOREIGN KEY CONSTRAINTS");
        System.out.println("=================================================\n");

        // Test foreign keys on classroom_grade_entries
        testForeignKeys("classroom_grade_entries", List.of(
            "fk_grade_entry_student",
            "fk_grade_entry_course",
            "fk_grade_entry_teacher",
            "fk_grade_entry_campus"
        ));

        // Test foreign keys on behavior_incidents
        testForeignKeys("behavior_incidents", List.of(
            "fk_behavior_student",
            "fk_behavior_course",
            "fk_behavior_teacher",
            "fk_behavior_campus"
        ));

        // Test foreign keys on teacher_observation_notes
        testForeignKeys("teacher_observation_notes", List.of(
            "fk_observation_student",
            "fk_observation_course",
            "fk_observation_teacher",
            "fk_observation_campus"
        ));

        System.out.println("\n=================================================");
        System.out.println("✓ ALL FOREIGN KEY CONSTRAINTS VERIFIED");
        System.out.println("=================================================\n");
    }

    private void testForeignKeys(String tableName, List<String> expectedFKs) {
        System.out.println("Testing foreign keys on: " + tableName);

        String sql = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                    "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC' " +
                    "AND CONSTRAINT_TYPE = 'REFERENTIAL'";  // H2 uses 'REFERENTIAL' not 'FOREIGN KEY'

        List<String> actualFKs = jdbcTemplate.queryForList(sql, String.class, tableName.toUpperCase());

        System.out.println("  Expected FKs: " + expectedFKs.size());
        System.out.println("  Actual FKs: " + actualFKs.size());

        for (String expectedFK : expectedFKs) {
            boolean found = actualFKs.stream()
                .anyMatch(fk -> fk.equalsIgnoreCase(expectedFK));

            assertTrue(found,
                "Foreign key " + expectedFK + " should exist on table " + tableName +
                ". Actual FKs: " + actualFKs);
        }

        System.out.println("  ✓ All " + expectedFKs.size() + " foreign keys verified");
        System.out.println("  FKs: " + String.join(", ", actualFKs));
    }
}
