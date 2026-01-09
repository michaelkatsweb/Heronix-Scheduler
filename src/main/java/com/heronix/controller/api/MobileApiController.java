package com.heronix.controller.api;

import com.heronix.service.impl.AttendanceService;
import com.heronix.service.impl.TranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Mobile API Controller
 * REST endpoints optimized for mobile applications (iOS/Android).
 *
 * Features:
 * - Lightweight JSON responses
 * - Offline-sync support
 * - Push notification endpoints
 * - Student schedule view
 * - Attendance tracking
 * - Grade summaries
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 17 - Mobile API Foundation
 */
@Slf4j
@RestController
@RequestMapping("/api/mobile/v1")
@RequiredArgsConstructor
@Tag(name = "Mobile", description = "Mobile application API endpoints")
public class MobileApiController {

    private final AttendanceService attendanceService;
    private final TranscriptService transcriptService;

    // ========================================================================
    // SCHEDULE ENDPOINTS
    // ========================================================================

    @Operation(summary = "Get student's daily schedule",
               description = "Returns the schedule for a specific student on a given date")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/students/{studentId}/schedule")
    public ResponseEntity<MobileScheduleResponse> getStudentSchedule(
            @Parameter(description = "Student ID") @PathVariable Long studentId,
            @Parameter(description = "Date (defaults to today)") @RequestParam(required = false) LocalDate date) {

        if (date == null) date = LocalDate.now();

        log.info("Mobile API: Getting schedule for student {} on {}", studentId, date);

        // TODO: Implement actual schedule retrieval
        MobileScheduleResponse response = MobileScheduleResponse.builder()
            .studentId(studentId)
            .date(date)
            .dayOfWeek(date.getDayOfWeek().name())
            .periods(new ArrayList<>()) // Populate from schedule service
            .lastUpdated(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get student's weekly schedule")
    @GetMapping("/students/{studentId}/schedule/week")
    public ResponseEntity<MobileWeekScheduleResponse> getStudentWeekSchedule(
            @PathVariable Long studentId,
            @RequestParam(required = false) LocalDate weekStart) {

        if (weekStart == null) {
            weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        }

        log.info("Mobile API: Getting weekly schedule for student {} starting {}", studentId, weekStart);

        MobileWeekScheduleResponse response = MobileWeekScheduleResponse.builder()
            .studentId(studentId)
            .weekStart(weekStart)
            .weekEnd(weekStart.plusDays(4))
            .days(new ArrayList<>())
            .lastUpdated(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // ATTENDANCE ENDPOINTS
    // ========================================================================

    @Operation(summary = "Get student attendance summary")
    @GetMapping("/students/{studentId}/attendance")
    public ResponseEntity<AttendanceService.AttendanceSummary> getAttendanceSummary(
            @PathVariable Long studentId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        AttendanceService.AttendanceSummary summary = attendanceService
            .getStudentAttendanceSummary(studentId, startDate, endDate);

        return ResponseEntity.ok(summary);
    }

    // ========================================================================
    // GRADES ENDPOINTS
    // ========================================================================

    @Operation(summary = "Get student GPA and academic summary")
    @GetMapping("/students/{studentId}/grades")
    public ResponseEntity<MobileGradesSummary> getGradesSummary(@PathVariable Long studentId) {
        log.info("Mobile API: Getting grades for student {}", studentId);

        TranscriptService.StudentTranscript transcript = transcriptService.generateTranscript(studentId);
        TranscriptService.ClassRankInfo rankInfo = transcriptService.getClassRank(studentId);

        MobileGradesSummary summary = MobileGradesSummary.builder()
            .studentId(studentId)
            .cumulativeGpa(transcript.getCumulativeGpa())
            .weightedGpa(transcript.getWeightedGpa())
            .totalCredits(transcript.getTotalCreditsEarned())
            .classRank(rankInfo.getRank())
            .classSize(rankInfo.getTotalStudents())
            .lastUpdated(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get current term grades")
    @GetMapping("/students/{studentId}/grades/current")
    public ResponseEntity<List<MobileCurrentGrade>> getCurrentGrades(@PathVariable Long studentId) {
        // TODO: Implement current term grades
        return ResponseEntity.ok(new ArrayList<>());
    }

    // ========================================================================
    // NOTIFICATIONS ENDPOINTS
    // ========================================================================

    @Operation(summary = "Register device for push notifications")
    @PostMapping("/notifications/register")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request) {

        log.info("Mobile API: Registering device {} for user {}", request.getDeviceId(), request.getUserId());

        // TODO: Implement push notification registration
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Device registered successfully");
        response.put("deviceId", request.getDeviceId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get unread notifications")
    @GetMapping("/users/{userId}/notifications")
    public ResponseEntity<List<MobileNotification>> getNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {

        // TODO: Implement notification retrieval
        return ResponseEntity.ok(new ArrayList<>());
    }

    // ========================================================================
    // SYNC ENDPOINTS
    // ========================================================================

    @Operation(summary = "Get changes since last sync",
               description = "Returns all data changes since the given timestamp for offline sync")
    @GetMapping("/sync")
    public ResponseEntity<SyncResponse> getChanges(
            @RequestParam Long userId,
            @RequestParam LocalDateTime lastSync) {

        log.info("Mobile API: Sync request for user {} since {}", userId, lastSync);

        SyncResponse response = SyncResponse.builder()
            .syncTimestamp(LocalDateTime.now())
            .hasChanges(false)
            .scheduleChanges(new ArrayList<>())
            .gradeChanges(new ArrayList<>())
            .attendanceChanges(new ArrayList<>())
            .announcementChanges(new ArrayList<>())
            .build();

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MobileScheduleResponse {
        private Long studentId;
        private LocalDate date;
        private String dayOfWeek;
        private List<MobilePeriod> periods;
        private LocalDateTime lastUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MobileWeekScheduleResponse {
        private Long studentId;
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private List<MobileScheduleResponse> days;
        private LocalDateTime lastUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MobilePeriod {
        private Integer periodNumber;
        private String startTime;
        private String endTime;
        private String courseName;
        private String courseCode;
        private String teacherName;
        private String roomNumber;
        private String location;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MobileGradesSummary {
        private Long studentId;
        private java.math.BigDecimal cumulativeGpa;
        private java.math.BigDecimal weightedGpa;
        private java.math.BigDecimal totalCredits;
        private Integer classRank;
        private Integer classSize;
        private LocalDateTime lastUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MobileCurrentGrade {
        private Long courseId;
        private String courseName;
        private String teacherName;
        private String letterGrade;
        private Double numericGrade;
        private Double percentComplete;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceRegistrationRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Device ID is required")
        @Size(max = 255, message = "Device ID must not exceed 255 characters")
        private String deviceId;

        @NotBlank(message = "Device type is required")
        @Size(max = 50, message = "Device type must not exceed 50 characters")
        private String deviceType; // iOS, Android

        @NotBlank(message = "Push token is required")
        @Size(max = 500, message = "Push token must not exceed 500 characters")
        private String pushToken;

        @Size(max = 50, message = "App version must not exceed 50 characters")
        private String appVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MobileNotification {
        private Long id;
        private String type;
        private String title;
        private String message;
        private LocalDateTime timestamp;
        private boolean read;
        private Map<String, Object> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResponse {
        private LocalDateTime syncTimestamp;
        private boolean hasChanges;
        private List<Object> scheduleChanges;
        private List<Object> gradeChanges;
        private List<Object> attendanceChanges;
        private List<Object> announcementChanges;
    }
}
