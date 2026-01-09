package com.heronix.controller;

import com.heronix.util.BulkDataGenerator;
import com.heronix.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AdminController - Administrative endpoints for data management
 * Location: src/main/java/com/eduscheduler/controller/AdminController.java
 * 
 * SECURITY NOTE: In production, these endpoints should be protected with
 * authentication/authorization (Spring Security).
 * 
 * These endpoints allow administrators to:
 * - Load sample/test data
 * - Clear all data
 * - Check database status
 * - Generate bulk data for testing
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // Configure properly in production
public class AdminController {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BulkDataGenerator bulkDataGenerator;

    /**
     * GET /api/admin/status
     * Get current database status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("teachers", teacherRepository.countByActiveTrue());
        status.put("courses", courseRepository.count());
        status.put("rooms", roomRepository.count());
        status.put("schedules", scheduleRepository.count());
        status.put("students", studentRepository.count());
        status.put("events", eventRepository.count());
        status.put("totalRecords",
                teacherRepository.countByActiveTrue() +
                        courseRepository.count() +
                        roomRepository.count() +
                        scheduleRepository.count() +
                        studentRepository.count() +
                        eventRepository.count());

        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/admin/data/generate-teachers
     * Generate bulk teacher data
     * 
     * Request body: { "count": 1000 }
     */
    @PostMapping("/data/generate-teachers")
    public ResponseEntity<Map<String, Object>> generateTeachers(@RequestBody Map<String, Integer> request) {
        int count = request.getOrDefault("count", 100);

        if (count > 50000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximum count is 50,000 teachers"));
        }

        long startTime = System.currentTimeMillis();
        bulkDataGenerator.generateTeachers(count);
        long endTime = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Successfully generated " + count + " teachers");
        response.put("timeTakenMs", endTime - startTime);
        response.put("totalTeachers", teacherRepository.countByActiveTrue());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/data/generate-small
     * Generate small dataset (100 teachers)
     */
    @PostMapping("/data/generate-small")
    public ResponseEntity<Map<String, String>> generateSmall() {
        bulkDataGenerator.generateSmallDataset();
        return ResponseEntity.ok(Map.of(
                "message", "Small dataset generated",
                "teachers", String.valueOf(teacherRepository.countByActiveTrue())));
    }

    /**
     * POST /api/admin/data/generate-medium
     * Generate medium dataset (1,000 teachers)
     */
    @PostMapping("/data/generate-medium")
    public ResponseEntity<Map<String, String>> generateMedium() {
        bulkDataGenerator.generateMediumDataset();
        return ResponseEntity.ok(Map.of(
                "message", "Medium dataset generated",
                "teachers", String.valueOf(teacherRepository.countByActiveTrue())));
    }

    /**
     * POST /api/admin/data/generate-large
     * Generate large dataset (10,000 teachers)
     */
    @PostMapping("/data/generate-large")
    public ResponseEntity<Map<String, String>> generateLarge() {
        bulkDataGenerator.generateLargeDataset();
        return ResponseEntity.ok(Map.of(
                "message", "Large dataset generated",
                "teachers", String.valueOf(teacherRepository.countByActiveTrue())));
    }

    /**
     * POST /api/admin/data/generate-xl
     * Generate extra large dataset (50,000 teachers)
     * WARNING: This takes 1-2 minutes
     */
    @PostMapping("/data/generate-xl")
    public ResponseEntity<Map<String, String>> generateXL() {
        bulkDataGenerator.generateXLDataset();
        return ResponseEntity.ok(Map.of(
                "message", "Extra large dataset generated",
                "teachers", String.valueOf(teacherRepository.countByActiveTrue())));
    }

    /**
     * DELETE /api/admin/data/clear-teachers
     * Clear all teacher data
     */
    @DeleteMapping("/data/clear-teachers")
    public ResponseEntity<Map<String, String>> clearTeachers() {
        long count = teacherRepository.countByActiveTrue();
        teacherRepository.deleteAll();
        return ResponseEntity.ok(Map.of(
                "message", "Deleted " + count + " teachers",
                "remaining", String.valueOf(teacherRepository.countByActiveTrue())));
    }

    /**
     * DELETE /api/admin/data/clear-all
     * Clear ALL data from database
     * WARNING: This is irreversible!
     */
    @DeleteMapping("/data/clear-all")
    public ResponseEntity<Map<String, String>> clearAll() {
        long teacherCount = teacherRepository.countByActiveTrue();
        long courseCount = courseRepository.count();
        long roomCount = roomRepository.count();
        long scheduleCount = scheduleRepository.count();
        long studentCount = studentRepository.count();
        long eventCount = eventRepository.count();

        // Delete in reverse order of dependencies
        scheduleRepository.deleteAll();
        courseRepository.deleteAll();
        eventRepository.deleteAll();
        studentRepository.deleteAll();
        roomRepository.deleteAll();
        teacherRepository.deleteAll();

        Map<String, String> result = new HashMap<>();
        result.put("message", "All data cleared");
        result.put("teachersDeleted", String.valueOf(teacherCount));
        result.put("coursesDeleted", String.valueOf(courseCount));
        result.put("roomsDeleted", String.valueOf(roomCount));
        result.put("schedulesDeleted", String.valueOf(scheduleCount));
        result.put("studentsDeleted", String.valueOf(studentCount));
        result.put("eventsDeleted", String.valueOf(eventCount));

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/health
     * Simple health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", String.valueOf(System.currentTimeMillis())));
    }
}
