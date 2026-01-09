package com.heronix.controller.api;

import com.heronix.service.impl.AdvancedAnalyticsService;
import com.heronix.service.impl.AdvancedAnalyticsService.*;
import com.heronix.service.impl.PredictiveAnalyticsService;
import com.heronix.service.impl.PredictiveAnalyticsService.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Advanced Analytics and Predictive Analytics
 *
 * Endpoints:
 * - /api/analytics/burnout - Teacher burnout risk analysis
 * - /api/analytics/sped-compliance - SPED minutes compliance tracking
 * - /api/analytics/what-if - What-if scenario simulation
 * - /api/analytics/prep-equity - Prep time equity dashboard
 * - /api/analytics/enrollment-forecast - Predictive enrollment forecasting
 * - /api/analytics/conflicts - Smart conflict resolution
 * - /api/analytics/recommendations - Optimal assignment recommendations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - API Integration
 */
@Slf4j
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsApiController {

    @Autowired
    private AdvancedAnalyticsService advancedAnalyticsService;

    @Autowired
    private PredictiveAnalyticsService predictiveAnalyticsService;

    // ========================================================================
    // ADVANCED ANALYTICS ENDPOINTS
    // ========================================================================

    /**
     * Get burnout risk score for a specific teacher
     */
    @GetMapping("/burnout/{teacherId}")
    public ResponseEntity<BurnoutRiskResult> getTeacherBurnoutRisk(@PathVariable Long teacherId) {
        log.info("API: Getting burnout risk for teacher {}", teacherId);
        try {
            BurnoutRiskResult result = advancedAnalyticsService.calculateBurnoutRiskScore(teacherId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error calculating burnout risk for teacher {}", teacherId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get burnout risk scores for all teachers
     */
    @GetMapping("/burnout")
    public ResponseEntity<List<BurnoutRiskResult>> getAllTeacherBurnoutRisks() {
        log.info("API: Getting burnout risks for all teachers");
        try {
            List<BurnoutRiskResult> results = advancedAnalyticsService.getAllTeacherBurnoutRisks();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error calculating burnout risks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get high-risk teachers (burnout score >= threshold)
     */
    @GetMapping("/burnout/high-risk")
    public ResponseEntity<List<BurnoutRiskResult>> getHighRiskTeachers(
            @RequestParam(defaultValue = "70") int threshold) {
        log.info("API: Getting high-risk teachers (threshold: {})", threshold);
        try {
            List<BurnoutRiskResult> allResults = advancedAnalyticsService.getAllTeacherBurnoutRisks();
            List<BurnoutRiskResult> highRisk = allResults.stream()
                .filter(r -> r.getRiskScore() >= threshold)
                .toList();
            return ResponseEntity.ok(highRisk);
        } catch (Exception e) {
            log.error("Error getting high-risk teachers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get SPED compliance for a specific student
     */
    @GetMapping("/sped-compliance/{studentId}")
    public ResponseEntity<SPEDComplianceResult> getSPEDCompliance(@PathVariable Long studentId) {
        log.info("API: Getting SPED compliance for student {}", studentId);
        try {
            SPEDComplianceResult result = advancedAnalyticsService.calculateSPEDCompliance(studentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error calculating SPED compliance for student {}", studentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get SPED compliance summary for all IEP students
     */
    @GetMapping("/sped-compliance")
    public ResponseEntity<SPEDComplianceSummary> getAllSPEDCompliance() {
        log.info("API: Getting SPED compliance summary");
        try {
            SPEDComplianceSummary summary = advancedAnalyticsService.getSPEDComplianceSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error calculating SPED compliance summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Run what-if scenario simulation
     */
    @PostMapping("/what-if")
    public ResponseEntity<WhatIfResult> runWhatIfScenario(@Valid @RequestBody WhatIfScenario scenario) {
        log.info("API: Running what-if scenario: {}", scenario.getType());
        try {
            WhatIfResult result = advancedAnalyticsService.simulateScenario(scenario);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error running what-if scenario", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get prep time equity analysis
     */
    @GetMapping("/prep-equity")
    public ResponseEntity<PrepTimeEquityResult> getPrepTimeEquity() {
        log.info("API: Getting prep time equity analysis");
        try {
            PrepTimeEquityResult result = advancedAnalyticsService.calculatePrepTimeEquity();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error calculating prep time equity", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // PREDICTIVE ANALYTICS ENDPOINTS
    // ========================================================================

    /**
     * Get enrollment forecast for a specific course
     */
    @GetMapping("/enrollment-forecast/{courseId}")
    public ResponseEntity<EnrollmentForecast> getEnrollmentForecast(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") int periodsAhead) {
        log.info("API: Getting enrollment forecast for course {} ({} periods ahead)", courseId, periodsAhead);
        try {
            EnrollmentForecast forecast = predictiveAnalyticsService.predictEnrollment(courseId, periodsAhead);
            return ResponseEntity.ok(forecast);
        } catch (Exception e) {
            log.error("Error predicting enrollment for course {}", courseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get enrollment forecasts for all courses
     */
    @GetMapping("/enrollment-forecast")
    public ResponseEntity<List<EnrollmentForecast>> getAllEnrollmentForecasts(
            @RequestParam(defaultValue = "1") int periodsAhead) {
        log.info("API: Getting enrollment forecasts for all courses ({} periods ahead)", periodsAhead);
        try {
            List<EnrollmentForecast> forecasts = predictiveAnalyticsService.predictAllEnrollments(periodsAhead);
            return ResponseEntity.ok(forecasts);
        } catch (Exception e) {
            log.error("Error predicting enrollments", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get courses at risk of over-enrollment
     */
    @GetMapping("/enrollment-forecast/at-risk")
    public ResponseEntity<List<EnrollmentForecast>> getAtRiskCourses(
            @RequestParam(defaultValue = "1") int periodsAhead) {
        log.info("API: Getting at-risk courses ({} periods ahead)", periodsAhead);
        try {
            List<EnrollmentForecast> atRisk = predictiveAnalyticsService.getAtRiskCourses(periodsAhead);
            return ResponseEntity.ok(atRisk);
        } catch (Exception e) {
            log.error("Error getting at-risk courses", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Analyze and rank all conflicts
     */
    @GetMapping("/conflicts")
    public ResponseEntity<ConflictAnalysis> analyzeConflicts() {
        log.info("API: Analyzing and ranking conflicts");
        try {
            ConflictAnalysis analysis = predictiveAnalyticsService.analyzeAndRankConflicts();
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error analyzing conflicts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get smart resolutions for a specific slot conflict
     */
    @GetMapping("/conflicts/{slotId}/resolutions")
    public ResponseEntity<List<ConflictResolution>> getSmartResolutions(@PathVariable Long slotId) {
        log.info("API: Getting smart resolutions for slot {}", slotId);
        try {
            List<ConflictResolution> resolutions = predictiveAnalyticsService.getSmartResolutions(slotId);
            return ResponseEntity.ok(resolutions);
        } catch (Exception e) {
            log.error("Error getting resolutions for slot {}", slotId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get optimal assignment recommendations for unassigned courses
     */
    @GetMapping("/recommendations")
    public ResponseEntity<AssignmentRecommendations> getOptimalAssignments() {
        log.info("API: Getting optimal assignment recommendations");
        try {
            AssignmentRecommendations recommendations = predictiveAnalyticsService.getOptimalAssignments();
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Error getting assignment recommendations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get teacher recommendation for a specific course
     */
    @GetMapping("/recommendations/teacher/{courseId}")
    public ResponseEntity<TeacherAssignmentRec> getTeacherRecommendation(@PathVariable Long courseId) {
        log.info("API: Getting teacher recommendation for course {}", courseId);
        try {
            // Need to get the course first
            TeacherAssignmentRec recommendation = predictiveAnalyticsService.getOptimalAssignments()
                .getRecommendations().stream()
                .filter(r -> r.getCourseId().equals(courseId))
                .findFirst()
                .orElse(null);

            if (recommendation == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(recommendation);
        } catch (Exception e) {
            log.error("Error getting teacher recommendation for course {}", courseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get room recommendation for a course at specific time
     */
    @GetMapping("/recommendations/room/{courseId}")
    public ResponseEntity<RoomAssignmentRec> getRoomRecommendation(
            @PathVariable Long courseId,
            @RequestParam String day,
            @RequestParam String time) {
        log.info("API: Getting room recommendation for course {} on {} at {}", courseId, day, time);
        try {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
            LocalTime localTime = LocalTime.parse(time);

            RoomAssignmentRec recommendation = predictiveAnalyticsService.recommendRoomForCourse(
                courseId, dayOfWeek, localTime);

            if (recommendation == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(recommendation);
        } catch (Exception e) {
            log.error("Error getting room recommendation for course {}", courseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // DASHBOARD SUMMARY ENDPOINT
    // ========================================================================

    /**
     * Get comprehensive analytics dashboard summary
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAnalyticsDashboard() {
        log.info("API: Getting analytics dashboard summary");
        try {
            // Gather all analytics data
            List<BurnoutRiskResult> burnoutRisks = advancedAnalyticsService.getAllTeacherBurnoutRisks();
            SPEDComplianceSummary spedCompliance = advancedAnalyticsService.getSPEDComplianceSummary();
            PrepTimeEquityResult prepEquity = advancedAnalyticsService.calculatePrepTimeEquity();
            ConflictAnalysis conflicts = predictiveAnalyticsService.analyzeAndRankConflicts();
            List<EnrollmentForecast> atRiskCourses = predictiveAnalyticsService.getAtRiskCourses(1);
            AssignmentRecommendations assignments = predictiveAnalyticsService.getOptimalAssignments();

            // Build summary
            Map<String, Object> dashboard = Map.of(
                "burnoutSummary", Map.of(
                    "totalTeachers", burnoutRisks.size(),
                    "highRisk", burnoutRisks.stream().filter(r -> "HIGH".equals(r.getRiskLevel())).count(),
                    "moderateRisk", burnoutRisks.stream().filter(r -> "MODERATE".equals(r.getRiskLevel())).count(),
                    "averageScore", burnoutRisks.stream().mapToInt(BurnoutRiskResult::getRiskScore).average().orElse(0)
                ),
                "spedCompliance", Map.of(
                    "totalStudents", spedCompliance.getTotalStudentsWithIEP(),
                    "compliantCount", spedCompliance.getCompliantCount(),
                    "complianceRate", spedCompliance.getAverageCompliancePercentage()
                ),
                "prepEquity", Map.of(
                    "equityIndex", prepEquity.getEquityIndex(),
                    "averagePrepMinutes", prepEquity.getAveragePrepMinutes(),
                    "belowAverageCount", prepEquity.getBelowAverageTeachers().size()
                ),
                "conflicts", Map.of(
                    "total", conflicts.getTotalConflicts(),
                    "critical", conflicts.getCriticalCount(),
                    "high", conflicts.getHighCount()
                ),
                "enrollment", Map.of(
                    "atRiskCourses", atRiskCourses.size(),
                    "overCapacityCount", atRiskCourses.stream().filter(EnrollmentForecast::isOverCapacity).count()
                ),
                "assignments", Map.of(
                    "unassignedCourses", assignments.getTotalUnassigned(),
                    "averageMatchScore", assignments.getAverageMatchScore()
                )
            );

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error building analytics dashboard", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
