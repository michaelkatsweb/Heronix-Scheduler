package com.heronix;

import com.heronix.model.domain.Schedule;
import com.heronix.model.dto.ScheduleGenerationRequest;
import com.heronix.model.enums.ScheduleType;
import com.heronix.service.ScheduleGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Generate a Complete Schedule
 * December 6, 2025
 *
 * This test triggers actual schedule generation and shows the results.
 */
@SpringBootTest
public class GenerateCompleteSchedule {

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void generateSchedule() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("GENERATING COMPLETE SCHEDULE");
        System.out.println("=".repeat(80) + "\n");

        try {
            // Check prerequisites
            System.out.println("📋 PRE-GENERATION CHECKS:");
            System.out.println("-".repeat(80));

            Integer courseCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM courses WHERE active = TRUE", Integer.class);
            Integer teacherCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teachers WHERE active = TRUE", Integer.class);
            Integer roomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms WHERE available = TRUE", Integer.class);
            Integer studentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM students WHERE active = TRUE", Integer.class);

            System.out.printf("Active Courses: %d%n", courseCount);
            System.out.printf("Active Teachers: %d%n", teacherCount);
            System.out.printf("Available Rooms: %d%n", roomCount);
            System.out.printf("Active Students: %d%n", studentCount);

            // Create schedule generation request
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                .scheduleName("Test Schedule - " + LocalDate.now())
                .scheduleType(ScheduleType.TRADITIONAL)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .schoolStartTime(LocalTime.of(7, 0))
                .firstPeriodStartTime(LocalTime.of(7, 20))
                .schoolEndTime(LocalTime.of(14, 10))
                .periodDuration(50)
                .passingPeriodDuration(5)
                .enableLunch(true)
                .lunchStartTime(LocalTime.of(11, 0))
                .lunchDuration(30)
                .build();

            // Generate schedule
            System.out.println("\n🚀 STARTING SCHEDULE GENERATION...");
            System.out.println("-".repeat(80));
            System.out.println("NOTE: You may see many warnings during generation.");
            System.out.println("These are from OptaPlanner's search process (intermediate solutions).");
            System.out.println("Only the FINAL result matters!");
            System.out.println("-".repeat(80) + "\n");

            Schedule schedule = scheduleGenerationService.generateSchedule(
                request,
                (progress, message) -> {
                    System.out.printf("📊 Progress: %d%% - %s%n", progress, message);
                }
            );

            System.out.println("\n✅ SCHEDULE GENERATION COMPLETE!");
            System.out.println("-".repeat(80));
            System.out.printf("Schedule ID: %d%n", schedule.getId());
            System.out.printf("Schedule Name: %s%n", schedule.getName());
            System.out.printf("Status: %s%n", schedule.getStatus());

            // Check results
            System.out.println("\n📊 POST-GENERATION RESULTS:");
            System.out.println("-".repeat(80));

            Integer slotCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schedule_slots WHERE schedule_id = ?",
                Integer.class,
                schedule.getId()
            );
            System.out.printf("Schedule Slots Created: %d%n", slotCount);

            // Show some sample slots
            System.out.println("\n📋 SAMPLE SCHEDULE SLOTS (first 20):");
            System.out.println("-".repeat(80));

            List<Map<String, Object>> slots = jdbcTemplate.queryForList(
                "SELECT c.course_name, t.first_name, t.last_name, r.room_number " +
                "FROM schedule_slots s " +
                "LEFT JOIN courses c ON s.course_id = c.id " +
                "LEFT JOIN teachers t ON s.teacher_id = t.id " +
                "LEFT JOIN rooms r ON s.room_id = r.id " +
                "WHERE s.schedule_id = ? " +
                "LIMIT 20",
                schedule.getId()
            );

            for (Map<String, Object> slot : slots) {
                String firstName = slot.get("first_name") != null ? (String) slot.get("first_name") : "N/A";
                String lastName = slot.get("last_name") != null ? (String) slot.get("last_name") : "";
                System.out.printf("  %s - %s %s - Room %s%n",
                    slot.get("course_name"),
                    firstName,
                    lastName,
                    slot.get("room_number")
                );
            }

            // Summary
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✅ SUCCESS! Schedule Generated Successfully!");
            System.out.println("=".repeat(80));
            System.out.printf("Schedule ID: %d%n", schedule.getId());
            System.out.printf("Schedule Name: %s%n", schedule.getName());
            System.out.printf("Total Slots: %d%n", slotCount);
            System.out.println("\nYou can now view this schedule in the application.");
            System.out.println("=".repeat(80) + "\n");

        } catch (Exception e) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("❌ SCHEDULE GENERATION FAILED");
            System.out.println("=".repeat(80));
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=".repeat(80) + "\n");
            throw new RuntimeException(e);
        }
    }
}
