package com.heronix.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * List all tables in the database to verify schema
 */
@SpringBootTest
public class ListAllTables {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void listAllTables() {
        System.out.println("\n=================================================");
        System.out.println("ALL TABLES IN DATABASE");
        System.out.println("=================================================\n");

        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = 'PUBLIC' " +
                    "ORDER BY TABLE_NAME";

        List<String> tables = jdbcTemplate.queryForList(sql, String.class);

        System.out.println("Total tables: " + tables.size());
        System.out.println();

        for (String table : tables) {
            System.out.println("  - " + table);
        }

        System.out.println("\n=================================================");
        System.out.println("SEARCHING FOR PHASE 1 TABLES");
        System.out.println("=================================================\n");

        String searchPattern = "%GRADE%";
        sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
             "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME LIKE ? " +
             "ORDER BY TABLE_NAME";

        List<String> gradeTables = jdbcTemplate.queryForList(sql, String.class, searchPattern);
        System.out.println("Tables with 'GRADE' in name: " + gradeTables.size());
        for (String table : gradeTables) {
            System.out.println("  - " + table);
        }

        searchPattern = "%BEHAVIOR%";
        List<String> behaviorTables = jdbcTemplate.queryForList(sql, String.class, searchPattern);
        System.out.println("\nTables with 'BEHAVIOR' in name: " + behaviorTables.size());
        for (String table : behaviorTables) {
            System.out.println("  - " + table);
        }

        searchPattern = "%OBSERVATION%";
        List<String> observationTables = jdbcTemplate.queryForList(sql, String.class, searchPattern);
        System.out.println("\nTables with 'OBSERVATION' in name: " + observationTables.size());
        for (String table : observationTables) {
            System.out.println("  - " + table);
        }

        System.out.println("\n=================================================\n");
    }
}
