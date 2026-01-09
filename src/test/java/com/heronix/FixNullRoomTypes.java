package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ FIX NULL ROOM TYPES - Set to CLASSROOM (PERMANENT)                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * ROOT CAUSE: 12 rooms have NULL room_type!
 *
 * This fixes the constraint violations by setting NULL room types to CLASSROOM.
 *
 * IMPORTANT: This PERMANENTLY modifies the database (no @Transactional rollback)
 *
 * Run with: mvn test -Dtest=FixNullRoomTypes
 *
 * December 6, 2025
 */
@SpringBootTest
public class FixNullRoomTypes {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void fixNullRoomTypes() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FIX NULL ROOM TYPES - Setting to CLASSROOM");
        System.out.println("=".repeat(80) + "\n");

        // STEP 1: Show current state
        System.out.println("📊 BEFORE FIX:");
        System.out.println("-".repeat(80));
        showRoomTypeDistribution();

        // STEP 2: Find rooms with NULL room_type
        String findNullSql = "SELECT id, room_number, capacity FROM rooms WHERE room_type IS NULL AND available = TRUE";
        List<Map<String, Object>> nullRooms = jdbcTemplate.queryForList(findNullSql);

        System.out.println("\n🔍 Found " + nullRooms.size() + " rooms with NULL room_type:");
        for (Map<String, Object> room : nullRooms) {
            System.out.printf("   - Room %s (ID: %s, Capacity: %s)%n",
                    room.get("room_number"),
                    room.get("id"),
                    room.get("capacity"));
        }

        // STEP 3: Update NULL rooms to CLASSROOM
        String updateSql = "UPDATE rooms SET room_type = 'CLASSROOM' WHERE room_type IS NULL AND available = TRUE";
        int updatedCount = jdbcTemplate.update(updateSql);

        System.out.println("\n✅ Updated " + updatedCount + " rooms to CLASSROOM type\n");

        // STEP 4: Verify the fix
        System.out.println("📊 AFTER FIX:");
        System.out.println("-".repeat(80));
        showRoomTypeDistribution();

        // STEP 5: Final check
        String classroomCountSql = "SELECT COUNT(*) FROM rooms WHERE room_type = 'CLASSROOM' AND available = TRUE";
        Integer classroomCount = jdbcTemplate.queryForObject(classroomCountSql, Integer.class);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("FINAL VERIFICATION:");
        System.out.println("=".repeat(80));
        System.out.printf("Total CLASSROOM rooms: %d%n", classroomCount);

        if (classroomCount >= 10) {
            System.out.println("Status: ✅ SUFFICIENT");
            System.out.println("Result: Good! You have enough classrooms for Math/English courses\n");
        } else if (classroomCount >= 6) {
            System.out.println("Status: ⚠️  MINIMUM");
            System.out.println("Result: Minimum met, but consider adding more classrooms\n");
        } else {
            System.out.println("Status: ❌ INSUFFICIENT");
            System.out.println("Result: CRITICAL - Need at least 6 classrooms\n");
        }

        System.out.println("=".repeat(80));
        System.out.println("✅ FIX COMPLETE - Try schedule generation again!");
        System.out.println("=".repeat(80) + "\n");
    }

    private void showRoomTypeDistribution() {
        String sql = "SELECT " +
                     "CASE WHEN room_type IS NULL THEN 'NULL' ELSE room_type END as room_type, " +
                     "COUNT(*) as count, " +
                     "SUM(capacity) as total_capacity " +
                     "FROM rooms WHERE available = TRUE " +
                     "GROUP BY room_type " +
                     "ORDER BY count DESC";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        System.out.printf("%-25s %10s %15s%n", "Room Type", "Count", "Total Capacity");
        System.out.println("-".repeat(80));

        for (Map<String, Object> row : results) {
            System.out.printf("%-25s %10d %15d%n",
                    row.get("room_type"),
                    row.get("count"),
                    row.get("total_capacity"));
        }

        System.out.println();
    }
}
