package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Check Schedule Generation Status
 * December 6, 2025
 */
@SpringBootTest
public class CheckScheduleGeneration {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkScheduleGeneration() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SCHEDULE GENERATION STATUS CHECK");
        System.out.println("=".repeat(80) + "\n");

        // Check if any schedules exist
        System.out.println("📊 SCHEDULE RECORDS:");
        System.out.println("-".repeat(80));

        String scheduleSql = "SELECT COUNT(*) FROM schedules";
        Integer scheduleCount = jdbcTemplate.queryForObject(scheduleSql, Integer.class);
        System.out.printf("Total schedules in database: %d%n", scheduleCount);

        if (scheduleCount > 0) {
            System.out.println("\n📋 SCHEDULE DETAILS:");
            List<Map<String, Object>> schedules = jdbcTemplate.queryForList(
                "SELECT id, name, created_at, is_published, score " +
                "FROM schedules " +
                "ORDER BY created_at DESC " +
                "LIMIT 5"
            );

            for (Map<String, Object> schedule : schedules) {
                System.out.printf("  Schedule #%s: %s%n", schedule.get("id"), schedule.get("name"));
                System.out.printf("    Created: %s%n", schedule.get("created_at"));
                System.out.printf("    Published: %s%n", schedule.get("is_published"));
                System.out.printf("    Score: %s%n", schedule.get("score"));

                // Check how many slots this schedule has
                String slotCountSql = "SELECT COUNT(*) FROM schedule_slots WHERE schedule_id = ?";
                Integer slotCount = jdbcTemplate.queryForObject(slotCountSql, Integer.class, schedule.get("id"));
                System.out.printf("    Schedule slots: %d%n", slotCount);
                System.out.println();
            }
        } else {
            System.out.println("❌ No schedules found in database!");
            System.out.println("\nThis means:");
            System.out.println("  1. Schedule generation was never run, OR");
            System.out.println("  2. Schedule generation failed to save results, OR");
            System.out.println("  3. Schedules were deleted");
        }

        // Check schedule slots
        System.out.println("\n📊 SCHEDULE SLOTS:");
        System.out.println("-".repeat(80));

        String slotSql = "SELECT COUNT(*) FROM schedule_slots";
        Integer slotCount = jdbcTemplate.queryForObject(slotSql, Integer.class);
        System.out.printf("Total schedule slots: %d%n", slotCount);

        if (slotCount > 0) {
            System.out.println("\n📋 SAMPLE SCHEDULE SLOTS (first 10):");
            List<Map<String, Object>> slots = jdbcTemplate.queryForList(
                "SELECT s.id, s.schedule_id, c.course_name, t.first_name, t.last_name, r.room_number, ts.start_time " +
                "FROM schedule_slots s " +
                "LEFT JOIN courses c ON s.course_id = c.id " +
                "LEFT JOIN teachers t ON s.teacher_id = t.id " +
                "LEFT JOIN rooms r ON s.room_id = r.id " +
                "LEFT JOIN time_slots ts ON s.time_slot_id = ts.id " +
                "LIMIT 10"
            );

            for (Map<String, Object> slot : slots) {
                System.out.printf("  Slot #%s (Schedule #%s): %s - %s %s - Room %s - %s%n",
                    slot.get("id"),
                    slot.get("schedule_id"),
                    slot.get("course_name"),
                    slot.get("first_name"),
                    slot.get("last_name"),
                    slot.get("room_number"),
                    slot.get("start_time")
                );
            }
        } else {
            System.out.println("❌ No schedule slots found!");
            System.out.println("\nExpected slots: 32 courses × 5 days/week = ~160 slots (minimum)");
        }

        // Check time slots
        System.out.println("\n\n📊 TIME SLOTS:");
        System.out.println("-".repeat(80));

        String timeSlotSql = "SELECT COUNT(*) FROM time_slots";
        Integer timeSlotCount = jdbcTemplate.queryForObject(timeSlotSql, Integer.class);
        System.out.printf("Total time slots: %d%n", timeSlotCount);

        if (timeSlotCount == 0) {
            System.out.println("❌ WARNING: No time slots defined!");
            System.out.println("   Schedule generation requires time slots to be configured.");
        } else {
            System.out.println("\n📋 TIME SLOT SUMMARY:");
            List<Map<String, Object>> timeSlots = jdbcTemplate.queryForList(
                "SELECT day_of_week, COUNT(*) as count " +
                "FROM time_slots " +
                "GROUP BY day_of_week " +
                "ORDER BY day_of_week"
            );

            for (Map<String, Object> ts : timeSlots) {
                System.out.printf("  %s: %d periods%n", ts.get("day_of_week"), ts.get("count"));
            }
        }

        // Check courses
        System.out.println("\n\n📊 COURSES:");
        System.out.println("-".repeat(80));

        String courseSql = "SELECT COUNT(*) FROM courses WHERE active = TRUE";
        Integer courseCount = jdbcTemplate.queryForObject(courseSql, Integer.class);
        System.out.printf("Active courses: %d%n", courseCount);

        String assignedSql = "SELECT COUNT(*) FROM courses WHERE active = TRUE AND teacher_id IS NOT NULL";
        Integer assignedCount = jdbcTemplate.queryForObject(assignedSql, Integer.class);
        System.out.printf("Courses with teachers: %d%n", assignedCount);
        System.out.printf("Unassigned courses: %d%n", courseCount - assignedCount);

        // Summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SUMMARY:");
        System.out.println("=".repeat(80));

        System.out.printf("Schedules: %d%n", scheduleCount);
        System.out.printf("Schedule Slots: %d%n", slotCount);
        System.out.printf("Time Slots: %d%n", timeSlotCount);
        System.out.printf("Active Courses: %d%n", courseCount);
        System.out.printf("Courses with Teachers: %d%n", assignedCount);

        System.out.println("\nREADINESS CHECK:");
        boolean ready = true;

        if (timeSlotCount == 0) {
            System.out.println("❌ Missing time slots - Configure schedule periods first");
            ready = false;
        } else {
            System.out.println("✅ Time slots configured");
        }

        if (courseCount == 0) {
            System.out.println("❌ No courses - Add courses to the system");
            ready = false;
        } else {
            System.out.println("✅ Courses exist");
        }

        if (assignedCount < courseCount) {
            System.out.println("⚠️  Some courses lack teachers - May cause issues");
        } else {
            System.out.println("✅ All courses have teachers");
        }

        if (scheduleCount == 0) {
            System.out.println("⚠️  No schedules generated yet - Run schedule generation");
        } else {
            System.out.println("✅ Schedules exist in database");
        }

        if (slotCount == 0 && scheduleCount > 0) {
            System.out.println("❌ PROBLEM: Schedules exist but no slots - Generation may have failed");
            ready = false;
        }

        System.out.println("\n" + "=".repeat(80));
        if (ready && scheduleCount > 0 && slotCount > 0) {
            System.out.println("✅ SCHEDULE GENERATED SUCCESSFULLY");
        } else if (ready && scheduleCount == 0) {
            System.out.println("⚠️  READY FOR SCHEDULE GENERATION");
        } else {
            System.out.println("❌ ISSUES DETECTED - See above");
        }
        System.out.println("=".repeat(80) + "\n");
    }
}
