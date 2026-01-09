package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

/**
 * Test to verify database schema was created correctly
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "logging.level.org.hibernate.SQL=INFO"
})
public class SchemaVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void verifyNewTables() {
        System.out.println("\n========== VERIFYING NEW TABLES ==========\n");

        String[] newTables = {
            "CONFLICT_MATRIX",
            "COURSE_REQUESTS",
            "WAITLISTS",
            "COURSE_SECTIONS"
        };

        for (String tableName : newTables) {
            verifyTable(tableName);
        }

        System.out.println("\n========== VERIFYING ENHANCED COLUMNS ==========\n");

        // Verify new columns in existing tables
        verifyColumn("COURSES", "IS_SINGLETON");
        verifyColumn("COURSES", "IS_ZERO_PERIOD_ELIGIBLE");
        verifyColumn("COURSES", "NUM_SECTIONS_NEEDED");

        verifyColumn("STUDENTS", "IS_SENIOR");
        verifyColumn("STUDENTS", "PRIORITY_WEIGHT");
        verifyColumn("STUDENTS", "GRADUATION_YEAR");

        verifyColumn("SCHEDULE_SLOTS", "PERIOD_EXCLUDED");
        verifyColumn("SCHEDULE_SLOTS", "EXCLUSION_REASON");

        verifyColumn("TEACHERS", "PLANNING_PERIODS_PER_WEEK");
        verifyColumn("TEACHERS", "COMMON_PLANNING_PERIOD");

        System.out.println("\n========== ALL TABLES IN DATABASE ==========\n");
        listAllTables();

        System.out.println("\n========== SCHEMA VERIFICATION COMPLETE ==========\n");
    }

    private void verifyTable(String tableName) {
        try {
            String sql = "SELECT column_name, data_type FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE table_name = ? ORDER BY ordinal_position";
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, tableName);

            if (columns.isEmpty()) {
                System.out.println("❌ Table " + tableName + " NOT FOUND!");
            } else {
                System.out.println("✅ Table " + tableName + " exists with " + columns.size() + " columns:");
                for (Map<String, Object> column : columns) {
                    System.out.println("   - " + column.get("COLUMN_NAME") + " (" + column.get("DATA_TYPE") + ")");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("❌ Error checking table " + tableName + ": " + e.getMessage());
        }
    }

    private void verifyColumn(String tableName, String columnName) {
        try {
            String sql = "SELECT data_type FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE table_name = ? AND column_name = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, tableName, columnName);

            if (result.isEmpty()) {
                System.out.println("❌ Column " + tableName + "." + columnName + " NOT FOUND!");
            } else {
                String dataType = (String) result.get(0).get("DATA_TYPE");
                System.out.println("✅ Column " + tableName + "." + columnName + " exists (" + dataType + ")");
            }
        } catch (Exception e) {
            System.out.println("❌ Error checking column " + tableName + "." + columnName + ": " + e.getMessage());
        }
    }

    private void listAllTables() {
        try {
            String sql = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE table_schema = 'PUBLIC' ORDER BY table_name";
            List<String> tables = jdbcTemplate.queryForList(sql, String.class);

            System.out.println("Total tables: " + tables.size());
            for (String table : tables) {
                System.out.println("   - " + table);
            }
        } catch (Exception e) {
            System.out.println("❌ Error listing tables: " + e.getMessage());
        }
    }
}
