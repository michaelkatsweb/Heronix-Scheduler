package com.heronix.controller.api;

import com.heronix.model.domain.Campus;
import com.heronix.model.domain.District;
import com.heronix.service.impl.CampusFederationService;
import com.heronix.service.impl.CampusFederationService.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Multi-Campus Federation
 *
 * Endpoints:
 * - /api/federation/districts - District management
 * - /api/federation/campuses - Campus management
 * - /api/federation/shared-teachers - Teacher sharing across campuses
 * - /api/federation/cross-enrollment - Cross-campus student enrollment
 * - /api/federation/analytics - District-wide analytics
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - Multi-Campus Federation
 */
@Slf4j
@RestController
@RequestMapping("/api/federation")
@CrossOrigin(origins = "*")
public class FederationApiController {

    @Autowired
    private CampusFederationService federationService;

    // ========================================================================
    // DISTRICT ENDPOINTS
    // ========================================================================

    /**
     * Get all active districts
     */
    @GetMapping("/districts")
    public ResponseEntity<List<District>> getAllDistricts() {
        log.info("API: Getting all active districts");
        try {
            List<District> districts = federationService.getAllActiveDistricts();
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            log.error("Error getting districts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get district by ID
     */
    @GetMapping("/districts/{id}")
    public ResponseEntity<District> getDistrictById(@PathVariable Long id) {
        log.info("API: Getting district {}", id);
        try {
            return federationService.getDistrictById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting district {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get district summary with statistics
     */
    @GetMapping("/districts/{id}/summary")
    public ResponseEntity<DistrictSummary> getDistrictSummary(@PathVariable Long id) {
        log.info("API: Getting district summary for {}", id);
        try {
            DistrictSummary summary = federationService.getDistrictSummary(id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error getting district summary for {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new district
     */
    @PostMapping("/districts")
    public ResponseEntity<District> createDistrict(@Valid @RequestBody District district) {
        log.info("API: Creating district {}", district.getName());
        try {
            District created = federationService.createDistrict(district);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid district creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating district", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // CAMPUS ENDPOINTS
    // ========================================================================

    /**
     * Get all campuses in a district
     */
    @GetMapping("/districts/{districtId}/campuses")
    public ResponseEntity<List<Campus>> getCampusesByDistrict(@PathVariable Long districtId) {
        log.info("API: Getting campuses for district {}", districtId);
        try {
            List<Campus> campuses = federationService.getCampusesByDistrict(districtId);
            return ResponseEntity.ok(campuses);
        } catch (Exception e) {
            log.error("Error getting campuses for district {}", districtId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get campus by ID
     */
    @GetMapping("/campuses/{id}")
    public ResponseEntity<Campus> getCampusById(@PathVariable Long id) {
        log.info("API: Getting campus {}", id);
        try {
            return federationService.getCampusById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting campus {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get campus by code
     */
    @GetMapping("/campuses/code/{campusCode}")
    public ResponseEntity<Campus> getCampusByCode(@PathVariable String campusCode) {
        log.info("API: Getting campus by code {}", campusCode);
        try {
            return federationService.getCampusByCode(campusCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting campus by code {}", campusCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new campus in a district
     */
    @PostMapping("/districts/{districtId}/campuses")
    public ResponseEntity<Campus> createCampus(@PathVariable Long districtId, @Valid @RequestBody Campus campus) {
        log.info("API: Creating campus {} in district {}", campus.getName(), districtId);
        try {
            Campus created = federationService.createCampus(districtId, campus);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid campus creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating campus", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // SHARED TEACHER ENDPOINTS
    // ========================================================================

    /**
     * Get teachers available for cross-campus sharing in a district
     */
    @GetMapping("/districts/{districtId}/shared-teachers")
    public ResponseEntity<List<SharedTeacherInfo>> getAvailableSharedTeachers(@PathVariable Long districtId) {
        log.info("API: Getting available shared teachers for district {}", districtId);
        try {
            List<SharedTeacherInfo> teachers = federationService.getAvailableSharedTeachers(districtId);
            return ResponseEntity.ok(teachers);
        } catch (Exception e) {
            log.error("Error getting shared teachers for district {}", districtId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Assign a teacher to share with another campus
     */
    @PostMapping("/shared-teachers")
    public ResponseEntity<SharedTeacherAssignment> assignSharedTeacher(
            @RequestParam Long teacherId,
            @RequestParam Long targetCampusId,
            @RequestParam int periodsPerWeek) {
        log.info("API: Assigning teacher {} to campus {} for {} periods/week",
            teacherId, targetCampusId, periodsPerWeek);
        try {
            SharedTeacherAssignment assignment = federationService.assignSharedTeacher(
                teacherId, targetCampusId, periodsPerWeek);
            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid shared teacher assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error assigning shared teacher", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // CROSS-CAMPUS ENROLLMENT ENDPOINTS
    // ========================================================================

    /**
     * Get cross-campus enrollment options for a student
     */
    @GetMapping("/students/{studentId}/cross-enrollment-options")
    public ResponseEntity<List<CrossCampusEnrollmentOption>> getCrossCampusEnrollmentOptions(
            @PathVariable Long studentId) {
        log.info("API: Getting cross-campus enrollment options for student {}", studentId);
        try {
            List<CrossCampusEnrollmentOption> options =
                federationService.getCrossCampusEnrollmentOptions(studentId);
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            log.error("Error getting cross-campus options for student {}", studentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Enroll a student in a course at another campus
     */
    @PostMapping("/cross-enrollment")
    public ResponseEntity<CrossCampusEnrollmentResult> enrollStudentCrossCampus(
            @RequestParam Long studentId,
            @RequestParam Long targetCampusId,
            @RequestParam Long courseId) {
        log.info("API: Cross-campus enrollment - student {} to campus {} for course {}",
            studentId, targetCampusId, courseId);
        try {
            CrossCampusEnrollmentResult result = federationService.enrollStudentCrossCampus(
                studentId, targetCampusId, courseId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cross-campus enrollment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error with cross-campus enrollment", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // ANALYTICS ENDPOINTS
    // ========================================================================

    /**
     * Get district-wide capacity analysis
     */
    @GetMapping("/districts/{districtId}/capacity-analysis")
    public ResponseEntity<DistrictCapacityAnalysis> analyzeDistrictCapacity(@PathVariable Long districtId) {
        log.info("API: Analyzing capacity for district {}", districtId);
        try {
            DistrictCapacityAnalysis analysis = federationService.analyzeDistrictCapacity(districtId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error analyzing capacity for district {}", districtId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get resource sharing recommendations for a district
     */
    @GetMapping("/districts/{districtId}/recommendations")
    public ResponseEntity<List<ResourceSharingRecommendation>> getResourceSharingRecommendations(
            @PathVariable Long districtId) {
        log.info("API: Getting resource sharing recommendations for district {}", districtId);
        try {
            List<ResourceSharingRecommendation> recommendations =
                federationService.getResourceSharingRecommendations(districtId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Error getting recommendations for district {}", districtId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
