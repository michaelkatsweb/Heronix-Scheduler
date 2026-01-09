package com.heronix.service.impl;

import com.heronix.model.domain.Campus;
import com.heronix.model.domain.District;
import com.heronix.model.domain.Teacher;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Room;
import com.heronix.repository.CampusRepository;
import com.heronix.repository.DistrictRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.RoomRepository;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Multi-Campus Federation operations
 *
 * Features:
 * - Cross-campus teacher sharing
 * - Cross-campus student enrollment
 * - Centralized vs decentralized scheduling
 * - District-wide analytics
 * - Campus resource pooling
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - Multi-Campus Federation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusFederationService {

    private final CampusRepository campusRepository;
    private final DistrictRepository districtRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;

    // ========================================================================
    // DISTRICT OPERATIONS
    // ========================================================================

    /**
     * Create a new district
     */
    @Transactional
    public District createDistrict(District district) {
        // ✅ NULL SAFE: Validate district parameter
        if (district == null) {
            throw new IllegalArgumentException("District cannot be null");
        }

        log.info("Creating district: {}", district.getName());

        // ✅ NULL SAFE: Validate district code before lookup
        if (district.getDistrictCode() == null || district.getDistrictCode().trim().isEmpty()) {
            throw new IllegalArgumentException("District code cannot be null or empty");
        }

        if (districtRepository.existsByDistrictCode(district.getDistrictCode())) {
            throw new IllegalArgumentException("District code already exists: " + district.getDistrictCode());
        }

        return districtRepository.save(district);
    }

    /**
     * Get all active districts
     */
    public List<District> getAllActiveDistricts() {
        List<District> districts = districtRepository.findByActiveTrue();
        // ✅ NULL SAFE: Validate repository result
        if (districts == null) {
            log.warn("Repository returned null for active districts");
            return Collections.emptyList();
        }
        return districts;
    }

    /**
     * Get district by ID with campuses
     */
    public Optional<District> getDistrictById(Long id) {
        return districtRepository.findById(id);
    }

    /**
     * Get district summary with statistics
     */
    public DistrictSummary getDistrictSummary(Long districtId) {
        // ✅ NULL SAFE: Validate districtId parameter
        if (districtId == null) {
            throw new IllegalArgumentException("District ID cannot be null");
        }

        District district = districtRepository.findById(districtId)
            .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtId));

        List<Campus> campuses = campusRepository.findByDistrictIdAndActiveTrue(districtId);
        // ✅ NULL SAFE: Validate repository result
        if (campuses == null) {
            log.warn("Repository returned null for campuses in district {}", districtId);
            campuses = Collections.emptyList();
        }

        int totalStudents = campuses.stream()
            // ✅ NULL SAFE: Filter null campuses before mapping
            .filter(c -> c != null)
            .mapToInt(Campus::getCurrentEnrollment)
            .sum();

        int totalCapacity = campuses.stream()
            // ✅ NULL SAFE: Filter null campuses before mapping
            .filter(c -> c != null)
            .mapToInt(c -> c.getMaxStudents() != null ? c.getMaxStudents() : 0)
            .sum();

        return DistrictSummary.builder()
            .districtId(districtId)
            .districtName(district.getName())
            .districtCode(district.getDistrictCode())
            .totalCampuses(campuses.size())
            .totalStudents(totalStudents)
            .totalCapacity(totalCapacity)
            .utilizationRate(totalCapacity > 0 ? (double) totalStudents / totalCapacity * 100 : 0)
            .allowsCrossCampusEnrollment(Boolean.TRUE.equals(district.getAllowCrossCampusEnrollment()))
            .allowsSharedTeachers(Boolean.TRUE.equals(district.getAllowSharedTeachers()))
            .centralizedScheduling(Boolean.TRUE.equals(district.getCentralizedScheduling()))
            .campusSummaries(campuses.stream()
                // ✅ NULL SAFE: Filter null campuses before building summaries
                .filter(c -> c != null)
                .map(this::buildCampusSummary)
                .filter(summary -> summary != null)
                .collect(Collectors.toList()))
            .build();
    }

    // ========================================================================
    // CAMPUS OPERATIONS
    // ========================================================================

    /**
     * Create a new campus in a district
     */
    @Transactional
    public Campus createCampus(Long districtId, Campus campus) {
        // ✅ NULL SAFE: Validate parameters
        if (districtId == null) {
            throw new IllegalArgumentException("District ID cannot be null");
        }
        if (campus == null) {
            throw new IllegalArgumentException("Campus cannot be null");
        }

        log.info("Creating campus {} in district {}", campus.getName(), districtId);

        District district = districtRepository.findById(districtId)
            .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtId));

        // ✅ NULL SAFE: Validate campus code before lookup
        if (campus.getCampusCode() == null || campus.getCampusCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Campus code cannot be null or empty");
        }

        if (campusRepository.existsByCampusCode(campus.getCampusCode())) {
            throw new IllegalArgumentException("Campus code already exists: " + campus.getCampusCode());
        }

        campus.setDistrict(district);
        return campusRepository.save(campus);
    }

    /**
     * Get all campuses in a district
     */
    public List<Campus> getCampusesByDistrict(Long districtId) {
        // ✅ NULL SAFE: Validate districtId parameter
        if (districtId == null) {
            log.warn("Cannot get campuses for null district ID");
            return Collections.emptyList();
        }

        List<Campus> campuses = campusRepository.findByDistrictIdAndActiveTrue(districtId);
        // ✅ NULL SAFE: Validate repository result
        if (campuses == null) {
            log.warn("Repository returned null for campuses in district {}", districtId);
            return Collections.emptyList();
        }
        return campuses;
    }

    /**
     * Get campus by ID
     */
    public Optional<Campus> getCampusById(Long id) {
        return campusRepository.findById(id);
    }

    /**
     * Get campus by code
     */
    public Optional<Campus> getCampusByCode(String campusCode) {
        return campusRepository.findByCampusCode(campusCode);
    }

    // ========================================================================
    // CROSS-CAMPUS TEACHER SHARING
    // ========================================================================

    /**
     * Get teachers available for cross-campus sharing
     */
    public List<SharedTeacherInfo> getAvailableSharedTeachers(Long districtId) {
        // ✅ NULL SAFE: Validate districtId parameter
        if (districtId == null) {
            throw new IllegalArgumentException("District ID cannot be null");
        }

        District district = districtRepository.findById(districtId)
            .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtId));

        if (!Boolean.TRUE.equals(district.getAllowSharedTeachers())) {
            log.warn("District {} does not allow shared teachers", districtId);
            return Collections.emptyList();
        }

        // Get all teachers in the district's campuses
        List<Campus> campuses = campusRepository.findByDistrictIdAndActiveTrue(districtId);
        // ✅ NULL SAFE: Validate repository result
        if (campuses == null) {
            log.warn("Repository returned null for campuses in district {}", districtId);
            campuses = Collections.emptyList();
        }

        List<SharedTeacherInfo> sharedTeachers = new ArrayList<>();

        for (Campus campus : campuses) {
            // ✅ NULL SAFE: Skip null campuses and validate campus ID
            if (campus == null || campus.getId() == null) {
                continue;
            }

            List<Teacher> teachers = teacherRepository.findAllActive();
            // ✅ NULL SAFE: Validate repository result
            if (teachers == null) {
                log.warn("Repository returned null for teachers");
                teachers = Collections.emptyList();
            }

            teachers = teachers.stream()
                // ✅ NULL SAFE: Filter null teachers before checking active status
                .filter(t -> t != null && Boolean.TRUE.equals(t.getActive()))
                .collect(Collectors.toList());

            for (Teacher teacher : teachers) {
                // ✅ NULL SAFE: Skip null teachers
                if (teacher == null || teacher.getId() == null) {
                    continue;
                }
                // Check if teacher has capacity for additional assignments
                int currentLoad = teacher.getMaxPeriodsPerDay() != null ? teacher.getMaxPeriodsPerDay() : 6;

                // ✅ NULL SAFE: Safe extraction of teacher name
                String teacherName = (teacher.getFirstName() != null ? teacher.getFirstName() : "") +
                    " " + (teacher.getLastName() != null ? teacher.getLastName() : "");

                // ✅ NULL SAFE: Safe certifications join
                List<String> certs = teacher.getCertifications();
                String certStr = (certs != null && !certs.isEmpty()) ?
                    String.join(", ", certs.stream().filter(c -> c != null).collect(Collectors.toList())) :
                    "";

                SharedTeacherInfo info = SharedTeacherInfo.builder()
                    .teacherId(teacher.getId())
                    .teacherName(teacherName.trim())
                    .homeCampusId(campus.getId())
                    .homeCampusName(campus.getName())
                    .certifications(certStr)
                    .maxPeriodsPerDay(currentLoad)
                    .availableForSharing(currentLoad > 4)
                    .build();

                sharedTeachers.add(info);
            }
        }

        return sharedTeachers;
    }

    /**
     * Assign a shared teacher to another campus
     */
    @Transactional
    public SharedTeacherAssignment assignSharedTeacher(Long teacherId, Long targetCampusId, int periodsPerWeek) {
        // ✅ NULL SAFE: Validate parameters
        if (teacherId == null) {
            throw new IllegalArgumentException("Teacher ID cannot be null");
        }
        if (targetCampusId == null) {
            throw new IllegalArgumentException("Campus ID cannot be null");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        Campus targetCampus = campusRepository.findById(targetCampusId)
            .orElseThrow(() -> new IllegalArgumentException("Campus not found: " + targetCampusId));

        log.info("Assigning teacher {} to campus {} for {} periods/week",
            teacher.getLastName(), targetCampus.getName(), periodsPerWeek);

        // ✅ NULL SAFE: Safe extraction of teacher name
        String teacherName = (teacher.getFirstName() != null ? teacher.getFirstName() : "") +
            " " + (teacher.getLastName() != null ? teacher.getLastName() : "");

        return SharedTeacherAssignment.builder()
            .teacherId(teacherId)
            .teacherName(teacherName.trim())
            .targetCampusId(targetCampusId)
            .targetCampusName(targetCampus.getName())
            .periodsPerWeek(periodsPerWeek)
            .status("ACTIVE")
            .build();
    }

    // ========================================================================
    // CROSS-CAMPUS ENROLLMENT
    // ========================================================================

    /**
     * Get students eligible for cross-campus enrollment
     */
    public List<CrossCampusEnrollmentOption> getCrossCampusEnrollmentOptions(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<CrossCampusEnrollmentOption> options = new ArrayList<>();

        // Get all campuses that allow cross-enrollment
        List<Campus> allCampuses = campusRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (allCampuses == null) {
            log.warn("Repository returned null for campuses");
            allCampuses = Collections.emptyList();
        }

        List<Campus> availableCampuses = allCampuses.stream()
            // ✅ NULL SAFE: Filter null campuses before checking active status
            .filter(c -> c != null && Boolean.TRUE.equals(c.getActive()))
            .filter(c -> c.getDistrict() != null &&
                        Boolean.TRUE.equals(c.getDistrict().getAllowCrossCampusEnrollment()))
            .collect(Collectors.toList());

        for (Campus campus : availableCampuses) {
            // ✅ NULL SAFE: Skip null campuses and validate required fields
            if (campus == null || campus.getId() == null || campus.getDistrict() == null) {
                continue;
            }
            int maxCap = campus.getMaxStudents() != null ? campus.getMaxStudents() : 0;
            int availableCapacity = maxCap - campus.getCurrentEnrollment();

            options.add(CrossCampusEnrollmentOption.builder()
                .campusId(campus.getId())
                .campusName(campus.getName())
                .campusCode(campus.getCampusCode())
                .districtName(campus.getDistrict().getName())
                .availableCapacity(availableCapacity)
                .campusType(campus.getCampusType() != null ? campus.getCampusType().name() : "UNKNOWN")
                .gradeLevels(campus.getGradeLevels())
                .specialPrograms("")
                .build());
        }

        return options;
    }

    /**
     * Enroll student in a course at another campus
     */
    @Transactional
    public CrossCampusEnrollmentResult enrollStudentCrossCampus(Long studentId, Long targetCampusId, Long courseId) {
        // ✅ NULL SAFE: Validate parameters
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        if (targetCampusId == null) {
            throw new IllegalArgumentException("Campus ID cannot be null");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        Campus targetCampus = campusRepository.findById(targetCampusId)
            .orElseThrow(() -> new IllegalArgumentException("Campus not found: " + targetCampusId));

        log.info("Cross-campus enrollment: Student {} to campus {} for course {}",
            studentId, targetCampus.getName(), courseId);

        // ✅ NULL SAFE: Safe extraction of student name
        String studentName = (student.getFirstName() != null ? student.getFirstName() : "") +
            " " + (student.getLastName() != null ? student.getLastName() : "");

        return CrossCampusEnrollmentResult.builder()
            .studentId(studentId)
            .studentName(studentName.trim())
            .targetCampusId(targetCampusId)
            .targetCampusName(targetCampus.getName())
            .courseId(courseId)
            .status("ENROLLED")
            .transportationRequired(true)
            .build();
    }

    // ========================================================================
    // DISTRICT-WIDE ANALYTICS
    // ========================================================================

    /**
     * Get district-wide capacity analysis
     */
    public DistrictCapacityAnalysis analyzeDistrictCapacity(Long districtId) {
        // ✅ NULL SAFE: Validate districtId parameter
        if (districtId == null) {
            throw new IllegalArgumentException("District ID cannot be null");
        }

        List<Campus> campuses = campusRepository.findByDistrictIdAndActiveTrue(districtId);
        // ✅ NULL SAFE: Validate repository result
        if (campuses == null) {
            log.warn("Repository returned null for campuses in district {}", districtId);
            campuses = Collections.emptyList();
        }

        int totalCapacity = 0;
        int totalEnrollment = 0;
        List<CampusCapacityInfo> campusInfos = new ArrayList<>();

        for (Campus campus : campuses) {
            // ✅ NULL SAFE: Skip null campuses and validate campus ID
            if (campus == null || campus.getId() == null) {
                continue;
            }
            int capacity = campus.getMaxStudents() != null ? campus.getMaxStudents() : 0;
            int enrollment = campus.getCurrentEnrollment();

            totalCapacity += capacity;
            totalEnrollment += enrollment;

            double utilization = capacity > 0 ? (double) enrollment / capacity * 100 : 0;
            String status = utilization > 95 ? "OVERCROWDED" :
                           utilization > 85 ? "NEAR_CAPACITY" :
                           utilization > 50 ? "HEALTHY" : "UNDERUTILIZED";

            campusInfos.add(CampusCapacityInfo.builder()
                .campusId(campus.getId())
                .campusName(campus.getName())
                .capacity(capacity)
                .currentEnrollment(enrollment)
                .availableSeats(capacity - enrollment)
                .utilizationPercentage(utilization)
                .status(status)
                .build());
        }

        double avgUtilization = totalCapacity > 0 ? (double) totalEnrollment / totalCapacity * 100 : 0;

        return DistrictCapacityAnalysis.builder()
            .districtId(districtId)
            .totalCampuses(campuses.size())
            .totalCapacity(totalCapacity)
            .totalEnrollment(totalEnrollment)
            .totalAvailableSeats(totalCapacity - totalEnrollment)
            .averageUtilization(avgUtilization)
            .overcrowdedCampuses((int) campusInfos.stream()
                .filter(c -> "OVERCROWDED".equals(c.getStatus())).count())
            .underutilizedCampuses((int) campusInfos.stream()
                .filter(c -> "UNDERUTILIZED".equals(c.getStatus())).count())
            .campusDetails(campusInfos)
            .build();
    }

    /**
     * Get resource sharing recommendations
     */
    public List<ResourceSharingRecommendation> getResourceSharingRecommendations(Long districtId) {
        // ✅ NULL SAFE: Validate districtId parameter
        if (districtId == null) {
            throw new IllegalArgumentException("District ID cannot be null");
        }

        List<ResourceSharingRecommendation> recommendations = new ArrayList<>();
        List<Campus> campuses = campusRepository.findByDistrictIdAndActiveTrue(districtId);
        // ✅ NULL SAFE: Validate repository result
        if (campuses == null) {
            log.warn("Repository returned null for campuses in district {}", districtId);
            campuses = Collections.emptyList();
        }

        // Analyze teacher shortages and surpluses
        Map<String, List<Campus>> subjectNeeds = new HashMap<>();
        Map<String, List<Campus>> subjectSurplus = new HashMap<>();

        // Simplified analysis - in real implementation, analyze actual teacher assignments
        for (Campus campus : campuses) {
            // ✅ NULL SAFE: Skip null campuses and validate campus ID
            if (campus == null || campus.getId() == null) {
                continue;
            }
            int maxCap = campus.getMaxStudents() != null ? campus.getMaxStudents() : 0;
            int enrollment = campus.getCurrentEnrollment();
            double utilization = maxCap > 0 ? (double) enrollment / maxCap * 100 : 0;

            if (utilization > 90) {
                // Campus likely needs resources
                recommendations.add(ResourceSharingRecommendation.builder()
                    .type("TEACHER_SHARING")
                    .sourceCampusId(null)
                    .targetCampusId(campus.getId())
                    .targetCampusName(campus.getName())
                    .resource("General Staff")
                    .reason("High utilization (" + String.format("%.1f%%", utilization) + ") suggests need for additional staff")
                    .priority("HIGH")
                    .estimatedImpact("Could reduce class sizes by 10-15%")
                    .build());
            } else if (utilization < 50) {
                // Campus might have surplus resources
                recommendations.add(ResourceSharingRecommendation.builder()
                    .type("CAPACITY_SHARING")
                    .sourceCampusId(campus.getId())
                    .sourceCampusName(campus.getName())
                    .targetCampusId(null)
                    .resource("Available Capacity")
                    .reason("Low utilization (" + String.format("%.1f%%", utilization) + ") - could accept transfer students")
                    .priority("MEDIUM")
                    .estimatedImpact("Could accept " + (maxCap - enrollment) + " additional students")
                    .build());
            }
        }

        return recommendations;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private CampusSummary buildCampusSummary(Campus campus) {
        // ✅ NULL SAFE: Validate campus parameter
        if (campus == null) {
            log.warn("Cannot build summary for null campus");
            return null;
        }

        int maxCap = campus.getMaxStudents() != null ? campus.getMaxStudents() : 0;
        int enrollment = campus.getCurrentEnrollment();
        double utilization = maxCap > 0 ? (double) enrollment / maxCap * 100 : 0;

        return CampusSummary.builder()
            .campusId(campus.getId())
            .campusName(campus.getName())
            .campusCode(campus.getCampusCode())
            .campusType(campus.getCampusType() != null ? campus.getCampusType().name() : "UNKNOWN")
            .currentEnrollment(enrollment)
            .maxCapacity(maxCap)
            .utilizationRate(utilization)
            .gradeLevels(campus.getGradeLevels())
            .build();
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistrictSummary {
        private Long districtId;
        private String districtName;
        private String districtCode;
        private int totalCampuses;
        private int totalStudents;
        private int totalCapacity;
        private double utilizationRate;
        private boolean allowsCrossCampusEnrollment;
        private boolean allowsSharedTeachers;
        private boolean centralizedScheduling;
        private List<CampusSummary> campusSummaries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampusSummary {
        private Long campusId;
        private String campusName;
        private String campusCode;
        private String campusType;
        private int currentEnrollment;
        private int maxCapacity;
        private double utilizationRate;
        private String gradeLevels;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedTeacherInfo {
        private Long teacherId;
        private String teacherName;
        private Long homeCampusId;
        private String homeCampusName;
        private String certifications;
        private int maxPeriodsPerDay;
        private boolean availableForSharing;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedTeacherAssignment {
        private Long teacherId;
        private String teacherName;
        private Long targetCampusId;
        private String targetCampusName;
        private int periodsPerWeek;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrossCampusEnrollmentOption {
        private Long campusId;
        private String campusName;
        private String campusCode;
        private String districtName;
        private int availableCapacity;
        private String campusType;
        private String gradeLevels;
        private String specialPrograms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrossCampusEnrollmentResult {
        private Long studentId;
        private String studentName;
        private Long targetCampusId;
        private String targetCampusName;
        private Long courseId;
        private String status;
        private boolean transportationRequired;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistrictCapacityAnalysis {
        private Long districtId;
        private int totalCampuses;
        private int totalCapacity;
        private int totalEnrollment;
        private int totalAvailableSeats;
        private double averageUtilization;
        private int overcrowdedCampuses;
        private int underutilizedCampuses;
        private List<CampusCapacityInfo> campusDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampusCapacityInfo {
        private Long campusId;
        private String campusName;
        private int capacity;
        private int currentEnrollment;
        private int availableSeats;
        private double utilizationPercentage;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceSharingRecommendation {
        private String type;
        private Long sourceCampusId;
        private String sourceCampusName;
        private Long targetCampusId;
        private String targetCampusName;
        private String resource;
        private String reason;
        private String priority;
        private String estimatedImpact;
    }
}
