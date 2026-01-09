package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Check room data in the actual database
 */
@SpringBootTest
public class CheckRoomData {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkRoomTypes() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CHECKING ROOM DATA IN DATABASE");
        System.out.println("=".repeat(80) + "\n");

        // Check room type distribution
        System.out.println("Room Type Distribution:");
        System.out.println("-".repeat(80));

        String sql = "SELECT room_type, COUNT(*) as count, SUM(capacity) as total_capacity " +
                     "FROM rooms WHERE available = TRUE GROUP BY room_type ORDER BY count DESC";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            if (results.isEmpty()) {
                System.out.println("❌ NO ROOMS FOUND IN DATABASE!");
            } else {
                System.out.printf("%-25s %10s %15s%n", "Room Type", "Count", "Total Capacity");
                System.out.println("-".repeat(80));

                for (Map<String, Object> row : results) {
                    System.out.printf("%-25s %10d %15d%n",
                            row.get("room_type"),
                            row.get("count"),
                            row.get("total_capacity"));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
        }

        System.out.println("\n" + "=".repeat(80) + "\n");
    }
}
