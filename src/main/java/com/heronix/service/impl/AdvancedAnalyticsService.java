package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Analytics Service
 * Provides differentiating analytics features for Heronix Scheduling System.
 *
 * Features:
 * - Teacher Burnout Risk Score
 * - SPED Minutes Compliance Tracker
 * - What-If Scenario Simulator
 * - Prep Time Equity Dashboard
 *
 * Location: src/main/java/com/eduscheduler/service/impl/AdvancedAnalyticsService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 10 - Differentiating Analytics
 */
@Slf4j
@Service
public class AdvancedAnalyticsService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private IEPRepository iepRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    // ========================================================================
    // TEACHER BURNOUT RISK SCORE
    // ========================================================================

    /**
     * Calculate burnout risk score for a teacher (0-100).
     *
     * Factors considered:
     * - Workload (number of classes)
     * - Back-to-back classes without breaks
     * - Number of different course preps
     * - Student load (total students across all classes)
     * - SPED student percentage
     *
     * @param teacherId the teacher's ID
     * @return BurnoutRiskResult with score and contributing factors
     */
    public BurnoutRiskResult calculateBurnoutRiskScore(Long teacherId) {
        log.info("Calculating burnout risk score for teacher ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        if (teacher == null) {
            return BurnoutRiskResult.builder()
                .teacherId(teacherId)
                .riskScore(0)
                .riskLevel("UNKNOWN")
                .message("Teacher not found")
                .build();
        }

        // Get slots using the correct repository method
        List<ScheduleSlot> slots = scheduleSlotRepository.findByTeacherIdWithDetails(teacherId);

        double riskScore = 0.0;
        Map<String, Double> factors = new LinkedHashMap<>();

        // Factor 1: Workload (classes per day)
        double avgClassesPerDay = calculateAverageClassesPerDay(slots);
        double workloadRisk = Math.min(30, avgClassesPerDay * 5); // Max 30 points
        factors.put("Workload (classes/day)", workloadRisk);
        riskScore += workloadRisk;

        // Factor 2: Back-to-back classes without breaks
        int backToBackCount = countBackToBackClasses(slots);
        double backToBackRisk = Math.min(25, backToBackCount * 5); // Max 25 points
        factors.put("Back-to-back classes", backToBackRisk);
        riskScore += backToBackRisk;

        // Factor 3: Number of different course preps
        int uniquePreps = countUniqueCoursePreps(slots);
        double prepRisk = Math.min(15, (uniquePreps - 2) * 5); // Penalty for >2 preps
        prepRisk = Math.max(0, prepRisk);
        factors.put("Course prep variety", prepRisk);
        riskScore += prepRisk;

        // Factor 4: Student load
        int totalStudents = calculateTotalStudentLoad(teacher);
        double studentRisk = Math.min(15, (totalStudents - 100) / 10.0); // Penalty for >100 students
        studentRisk = Math.max(0, studentRisk);
        factors.put("Student load", studentRisk);
        riskScore += studentRisk;

        // Factor 5: SPED student percentage
        double spedPercentage = calculateSpedStudentPercentage(teacher);
        double spedRisk = Math.min(15, spedPercentage / 5); // Higher SPED % = higher risk
        factors.put("SPED student %", spedRisk);
        riskScore += spedRisk;

        // Determine risk level
        String riskLevel;
        String message;
        if (riskScore >= 70) {
            riskLevel = "HIGH";
            message = "Immediate intervention recommended. Consider workload redistribution.";
        } else if (riskScore >= 50) {
            riskLevel = "MODERATE";
            message = "Monitor closely. Review prep time and student load.";
        } else if (riskScore >= 30) {
            riskLevel = "LOW";
            message = "Manageable workload. Continue monitoring.";
        } else {
            riskLevel = "MINIMAL";
            message = "Healthy workload balance.";
        }

        log.info("Burnout risk for teacher {}: {} ({})", teacher.getName(), riskScore, riskLevel);

        return BurnoutRiskResult.builder()
            .teacherId(teacherId)
            .teacherName(teacher.getName())
            .riskScore((int) Math.round(riskScore))
            .riskLevel(riskLevel)
            .message(message)
            .factors(factors)
            .classesPerDay(avgClassesPerDay)
            .backToBackCount(backToBackCount)
            .uniquePreps(uniquePreps)
            .totalStudents(totalStudents)
            .spedPercentage(spedPercentage)
            .build();
    }

    /**
     * Get burnout risk scores for all teachers, sorted by risk
     */
    public List<BurnoutRiskResult> getAllTeacherBurnoutRisks() {
        return teacherRepository.findAllActive().stream()
            .filter(Teacher::getActive)
            .map(t -> calculateBurnoutRiskScore(t.getId()))
            .sorted((a, b) -> Integer.compare(b.getRiskScore(), a.getRiskScore()))
            .collect(Collectors.toList());
    }

    private double calculateAverageClassesPerDay(List<ScheduleSlot> slots) {
        if (slots.isEmpty()) return 0.0;

        Map<DayOfWeek, Long> classesPerDay = slots.stream()
            .filter(s -> s.getDayOfWeek() != null)
            .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek, Collectors.counting()));

        if (classesPerDay.isEmpty()) return 0.0;
        return classesPerDay.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private int countBackToBackClasses(List<ScheduleSlot> slots) {
        int backToBack = 0;

        Map<DayOfWeek, List<ScheduleSlot>> byDay = slots.stream()
            .filter(s -> s.getDayOfWeek() != null && s.getEndTime() != null)
            .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek));

        for (List<ScheduleSlot> daySlots : byDay.values()) {
            daySlots.sort(Comparator.comparing(ScheduleSlot::getStartTime));

            for (int i = 0; i < daySlots.size() - 1; i++) {
                LocalTime end1 = daySlots.get(i).getEndTime();
                LocalTime start2 = daySlots.get(i + 1).getStartTime();

                if (end1 != null && start2 != null) {
                    long gapMinutes = java.time.Duration.between(end1, start2).toMinutes();
                    if (gapMinutes < 10) { // Less than 10 min gap = back-to-back
                        backToBack++;
                    }
                }
            }
        }

        return backToBack;
    }

    private int countUniqueCoursePreps(List<ScheduleSlot> slots) {
        // ✅ NULL SAFE: Filter null courses and course IDs
        return (int) slots.stream()
            .filter(s -> s.getCourse() != null && s.getCourse().getId() != null)
            .map(s -> s.getCourse().getId())
            .distinct()
            .count();
    }

    private int calculateTotalStudentLoad(Teacher teacher) {
        // Count students from teacher's courses using currentEnrollment
        // ✅ NULL SAFE: Check teacher and courses exist
        if (teacher == null || teacher.getCourses() == null) return 0;

        return teacher.getCourses().stream()
            .filter(c -> c.getCurrentEnrollment() != null)
            .mapToInt(Course::getCurrentEnrollment)
            .sum();
    }

    private double calculateSpedStudentPercentage(Teacher teacher) {
        // ✅ NULL SAFE: Check teacher exists
        if (teacher == null) return 0.0;

        int totalStudents = calculateTotalStudentLoad(teacher);
        if (totalStudents == 0) return 0.0;

        // Count SPED students from courses
        long spedCount = 0;
        if (teacher.getCourses() != null) {
            for (Course course : teacher.getCourses()) {
                // ✅ NULL SAFE: Filter null students
                if (course.getStudents() != null) {
                    spedCount += course.getStudents().stream()
                        .filter(s -> s != null && Boolean.TRUE.equals(s.getHasIEP()))
                        .count();
                }
            }
        }

        return (spedCount * 100.0) / totalStudents;
    }

    // ========================================================================
    // SPED MINUTES COMPLIANCE TRACKER
    // ========================================================================

    /**
     * Calculate SPED minutes compliance for a student.
     * Compares scheduled service minutes against IEP required minutes.
     *
     * @param studentId the student's ID
     * @return SPEDComplianceResult with compliance status
     */
    public SPEDComplianceResult calculateSPEDCompliance(Long studentId) {
        log.info("Calculating SPED compliance for student ID: {}", studentId);

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null || !Boolean.TRUE.equals(student.getHasIEP())) {
            return SPEDComplianceResult.builder()
                .studentId(studentId)
                .compliant(true)
                .message("Student not found or does not have IEP")
                .build();
        }

        // Get student's IEP
        // ✅ NULL SAFE: Filter null IEPs from repository result
        IEP iep = iepRepository.findByStudent(student).stream()
            .filter(i -> i != null)
            .findFirst()
            .orElse(null);
        if (iep == null) {
            return SPEDComplianceResult.builder()
                .studentId(studentId)
                .studentName(student.getFullName())
                .compliant(false)
                .message("IEP record not found")
                .build();
        }

        // Calculate required minutes from IEP services
        int requiredMinutesWeekly = iep.getTotalMinutesPerWeek();

        // Calculate scheduled minutes from schedule slots
        int scheduledMinutesWeekly = calculateScheduledMinutes(student);

        // Calculate compliance percentage
        double compliancePercentage = requiredMinutesWeekly > 0
            ? (scheduledMinutesWeekly * 100.0 / requiredMinutesWeekly)
            : 100.0;

        boolean compliant = compliancePercentage >= 95.0; // 95% threshold

        String message;
        if (compliancePercentage >= 100) {
            message = "Full compliance - all IEP minutes scheduled";
        } else if (compliancePercentage >= 95) {
            message = "Within compliance threshold";
        } else if (compliancePercentage >= 80) {
            message = "WARNING: Below compliance - " + (requiredMinutesWeekly - scheduledMinutesWeekly) + " minutes short";
        } else {
            message = "CRITICAL: Significantly below compliance - immediate action required";
        }

        return SPEDComplianceResult.builder()
            .studentId(studentId)
            .studentName(student.getFullName())
            .requiredMinutesWeekly(requiredMinutesWeekly)
            .scheduledMinutesWeekly(scheduledMinutesWeekly)
            .compliancePercentage(compliancePercentage)
            .compliant(compliant)
            .message(message)
            .iepId(iep.getId())
            .build();
    }

    /**
     * Get SPED compliance for all students with IEPs
     */
    public List<SPEDComplianceResult> getAllSPEDCompliance() {
        // ✅ NULL SAFE: Filter null students and student IDs
        return studentRepository.findAll().stream()
            .filter(s -> s != null && s.getId() != null)
            .filter(s -> Boolean.TRUE.equals(s.getHasIEP()))
            .map(s -> calculateSPEDCompliance(s.getId()))
            .sorted((a, b) -> Double.compare(a.getCompliancePercentage(), b.getCompliancePercentage()))
            .collect(Collectors.toList());
    }

    /**
     * Get summary of SPED compliance across all students
     */
    public SPEDComplianceSummary getSPEDComplianceSummary() {
        List<SPEDComplianceResult> allCompliance = getAllSPEDCompliance();

        long totalStudents = allCompliance.size();
        long compliantCount = allCompliance.stream().filter(SPEDComplianceResult::isCompliant).count();
        long criticalCount = allCompliance.stream()
            .filter(r -> r.getCompliancePercentage() < 80)
            .count();

        double avgCompliance = allCompliance.stream()
            .mapToDouble(SPEDComplianceResult::getCompliancePercentage)
            .average()
            .orElse(100.0);

        return SPEDComplianceSummary.builder()
            .totalStudentsWithIEP((int) totalStudents)
            .compliantCount((int) compliantCount)
            .nonCompliantCount((int) (totalStudents - compliantCount))
            .criticalCount((int) criticalCount)
            .averageCompliancePercentage(avgCompliance)
            .overallCompliant(criticalCount == 0)
            .build();
    }

    private int calculateScheduledMinutes(Student student) {
        // Calculate total scheduled minutes from student's courses
        List<Course> studentCourses = courseRepository.findAll().stream()
            .filter(c -> c.getStudents() != null && c.getStudents().contains(student))
            .collect(Collectors.toList());

        int totalMinutes = 0;
        for (Course course : studentCourses) {
            // Get slots for this course
            List<ScheduleSlot> slots = scheduleSlotRepository.findByCourseIdWithDetails(course.getId());
            for (ScheduleSlot slot : slots) {
                if (slot.getStartTime() != null && slot.getEndTime() != null) {
                    long minutes = java.time.Duration.between(
                        slot.getStartTime(), slot.getEndTime()
                    ).toMinutes();
                    totalMinutes += (int) minutes;
                }
            }
        }

        return totalMinutes;
    }

    // ========================================================================
    // WHAT-IF SCENARIO SIMULATOR
    // ========================================================================

    /**
     * Simulate what-if scenario for schedule changes.
     *
     * @param scenario the scenario to simulate
     * @return WhatIfResult with impact analysis
     */
    public WhatIfResult simulateScenario(WhatIfScenario scenario) {
        log.info("Simulating what-if scenario: {}", scenario.getDescription());

        WhatIfResult result = WhatIfResult.builder()
            .scenarioType(scenario.getType())
            .description(scenario.getDescription())
            .impacts(new ArrayList<>())
            .build();

        switch (scenario.getType()) {
            case TEACHER_REMOVAL:
                simulateTeacherRemoval(scenario, result);
                break;
            case ROOM_CLOSURE:
                simulateRoomClosure(scenario, result);
                break;
            case ENROLLMENT_INCREASE:
                simulateEnrollmentIncrease(scenario, result);
                break;
            case TIME_CHANGE:
                simulateTimeChange(scenario, result);
                break;
            default:
                result.setFeasible(false);
                result.setMessage("Unknown scenario type");
        }

        return result;
    }

    private void simulateTeacherRemoval(WhatIfScenario scenario, WhatIfResult result) {
        Teacher teacher = teacherRepository.findById(scenario.getEntityId()).orElse(null);
        if (teacher == null) {
            result.setFeasible(false);
            result.setMessage("Teacher not found");
            return;
        }

        // Find affected slots
        List<ScheduleSlot> affectedSlots = scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId());

        // Calculate impact
        result.getImpacts().add(new ImpactItem("Affected Classes",
            String.valueOf(affectedSlots.size()), "HIGH"));

        // Find potential replacement teachers
        List<Teacher> availableTeachers = teacherRepository.findAllActive().stream()
            .filter(t -> t.getActive() && !t.getId().equals(teacher.getId()))
            .filter(t -> t.getCourseCount() < 6) // Not at capacity
            .collect(Collectors.toList());

        result.getImpacts().add(new ImpactItem("Available Replacements",
            String.valueOf(availableTeachers.size()),
            availableTeachers.isEmpty() ? "CRITICAL" : "MEDIUM"));

        // Affected students from teacher's courses
        int affectedStudents = teacher.getCourses() != null ?
            teacher.getCourses().stream()
                .filter(c -> c.getCurrentEnrollment() != null)
                .mapToInt(Course::getCurrentEnrollment)
                .sum() : 0;

        result.getImpacts().add(new ImpactItem("Affected Students",
            String.valueOf(affectedStudents), affectedStudents > 50 ? "HIGH" : "MEDIUM"));

        result.setFeasible(!availableTeachers.isEmpty());
        result.setMessage(result.isFeasible()
            ? "Scenario feasible with " + availableTeachers.size() + " potential replacements"
            : "CRITICAL: No available replacement teachers");
        result.setRecommendation(result.isFeasible()
            ? "Consider redistributing classes to: " + availableTeachers.stream()
                .limit(3).map(Teacher::getName).collect(Collectors.joining(", "))
            : "Hire substitute or redistribute across multiple teachers");
    }

    private void simulateRoomClosure(WhatIfScenario scenario, WhatIfResult result) {
        result.getImpacts().add(new ImpactItem("Room Closure", "Analysis pending", "MEDIUM"));
        result.setFeasible(true);
        result.setMessage("Room closure simulation - check available alternative rooms");
    }

    private void simulateEnrollmentIncrease(WhatIfScenario scenario, WhatIfResult result) {
        int increasePercent = scenario.getParameter() != null ? scenario.getParameter() : 10;

        // Analyze capacity
        long totalCapacity = courseRepository.findAll().stream()
            .mapToInt(c -> c.getMaxStudents() != null ? c.getMaxStudents() : 30)
            .sum();

        long currentEnrollment = studentRepository.count();
        long projectedEnrollment = (long) (currentEnrollment * (1 + increasePercent / 100.0));

        result.getImpacts().add(new ImpactItem("Current Enrollment",
            String.valueOf(currentEnrollment), "INFO"));
        result.getImpacts().add(new ImpactItem("Projected Enrollment",
            String.valueOf(projectedEnrollment), projectedEnrollment > totalCapacity ? "HIGH" : "MEDIUM"));
        result.getImpacts().add(new ImpactItem("Capacity Headroom",
            String.format("%.1f%%", (totalCapacity - projectedEnrollment) * 100.0 / totalCapacity),
            "INFO"));

        result.setFeasible(projectedEnrollment <= totalCapacity);
        result.setMessage(result.isFeasible()
            ? "Enrollment increase can be accommodated"
            : "Additional sections or rooms needed");
    }

    private void simulateTimeChange(WhatIfScenario scenario, WhatIfResult result) {
        result.getImpacts().add(new ImpactItem("Time Change", "Analysis pending", "MEDIUM"));
        result.setFeasible(true);
        result.setMessage("Time change simulation - check for conflicts");
    }

    // ========================================================================
    // PREP TIME EQUITY DASHBOARD
    // ========================================================================

    /**
     * Calculate prep time equity across all teachers.
     * Identifies disparities in planning/prep periods.
     *
     * @return PrepTimeEquityResult with analysis
     */
    public PrepTimeEquityResult calculatePrepTimeEquity() {
        log.info("Calculating prep time equity across teachers");

        List<TeacherPrepTime> teacherPrepTimes = new ArrayList<>();

        for (Teacher teacher : teacherRepository.findAllActive()) {
            if (!teacher.getActive()) continue;

            List<ScheduleSlot> slots = scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId());
            int prepMinutesWeekly = calculatePrepTime(slots);

            teacherPrepTimes.add(TeacherPrepTime.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getName())
                .department(teacher.getDepartment())
                .prepMinutesWeekly(prepMinutesWeekly)
                .classesPerWeek(slots.size())
                .build());
        }

        // Calculate statistics
        double avgPrepTime = teacherPrepTimes.stream()
            .mapToInt(TeacherPrepTime::getPrepMinutesWeekly)
            .average()
            .orElse(0);

        int minPrepTime = teacherPrepTimes.stream()
            .mapToInt(TeacherPrepTime::getPrepMinutesWeekly)
            .min()
            .orElse(0);

        int maxPrepTime = teacherPrepTimes.stream()
            .mapToInt(TeacherPrepTime::getPrepMinutesWeekly)
            .max()
            .orElse(0);

        // Calculate equity index (0-100, where 100 is perfectly equitable)
        double variance = teacherPrepTimes.stream()
            .mapToDouble(t -> Math.pow(t.getPrepMinutesWeekly() - avgPrepTime, 2))
            .average()
            .orElse(0);
        double stdDev = Math.sqrt(variance);
        double equityIndex = avgPrepTime > 0 ? Math.max(0, 100 - (stdDev / avgPrepTime * 100)) : 100;

        // Identify teachers with below-average prep time
        List<TeacherPrepTime> belowAverage = teacherPrepTimes.stream()
            .filter(t -> t.getPrepMinutesWeekly() < avgPrepTime * 0.8)
            .sorted(Comparator.comparingInt(TeacherPrepTime::getPrepMinutesWeekly))
            .collect(Collectors.toList());

        return PrepTimeEquityResult.builder()
            .teacherPrepTimes(teacherPrepTimes)
            .averagePrepMinutes(avgPrepTime)
            .minPrepMinutes(minPrepTime)
            .maxPrepMinutes(maxPrepTime)
            .equityIndex(equityIndex)
            .belowAverageCount(belowAverage.size())
            .belowAverageTeachers(belowAverage)
            .recommendation(generatePrepTimeRecommendation(equityIndex, belowAverage))
            .build();
    }

    private int calculatePrepTime(List<ScheduleSlot> slots) {
        // Calculate gaps between classes as prep time
        int prepMinutes = 0;

        Map<DayOfWeek, List<ScheduleSlot>> byDay = slots.stream()
            .filter(s -> s.getDayOfWeek() != null && s.getStartTime() != null)
            .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek));

        for (List<ScheduleSlot> daySlots : byDay.values()) {
            daySlots.sort(Comparator.comparing(ScheduleSlot::getStartTime));

            // School day is 8:00 AM to 3:00 PM (420 minutes)
            // Subtract teaching time to get prep time
            int teachingMinutes = daySlots.stream()
                .mapToInt(s -> {
                    if (s.getStartTime() != null && s.getEndTime() != null) {
                        return (int) java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
                    }
                    return 50; // Default class duration
                })
                .sum();

            // Assume 6.5 hours (390 min) available, subtract teaching time
            // Also account for lunch (30 min)
            int dailyPrep = Math.max(0, 390 - teachingMinutes - 30);
            prepMinutes += dailyPrep;
        }

        return prepMinutes;
    }

    private String generatePrepTimeRecommendation(double equityIndex, List<TeacherPrepTime> belowAverage) {
        if (equityIndex >= 90) {
            return "Excellent prep time equity. No immediate action needed.";
        } else if (equityIndex >= 70) {
            return "Good equity with minor disparities. Consider reviewing schedules for: " +
                belowAverage.stream().limit(3).map(TeacherPrepTime::getTeacherName)
                    .collect(Collectors.joining(", "));
        } else if (equityIndex >= 50) {
            return "Moderate disparities detected. " + belowAverage.size() +
                " teachers have significantly less prep time. Schedule review recommended.";
        } else {
            return "CRITICAL: Major prep time inequity. " + belowAverage.size() +
                " teachers are overloaded. Immediate schedule restructuring required.";
        }
    }

    // ========================================================================
    // DATA TRANSFER OBJECTS
    // ========================================================================

    @Data
    @Builder
    public static class BurnoutRiskResult {
        private Long teacherId;
        private String teacherName;
        private int riskScore;
        private String riskLevel;
        private String message;
        private Map<String, Double> factors;
        private double classesPerDay;
        private int backToBackCount;
        private int uniquePreps;
        private int totalStudents;
        private double spedPercentage;
    }

    @Data
    @Builder
    public static class SPEDComplianceResult {
        private Long studentId;
        private String studentName;
        private Long iepId;
        private int requiredMinutesWeekly;
        private int scheduledMinutesWeekly;
        private double compliancePercentage;
        private boolean compliant;
        private String message;
    }

    @Data
    @Builder
    public static class SPEDComplianceSummary {
        private int totalStudentsWithIEP;
        private int compliantCount;
        private int nonCompliantCount;
        private int criticalCount;
        private double averageCompliancePercentage;
        private boolean overallCompliant;
    }

    @Data
    @Builder
    public static class WhatIfScenario {
        @jakarta.validation.constraints.NotNull(message = "Scenario type is required")
        private ScenarioType type;

        @jakarta.validation.constraints.NotNull(message = "Entity ID is required")
        private Long entityId;

        @jakarta.validation.constraints.Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;

        private Integer parameter;
    }

    public enum ScenarioType {
        TEACHER_REMOVAL,
        ROOM_CLOSURE,
        ENROLLMENT_INCREASE,
        TIME_CHANGE
    }

    @Data
    @Builder
    public static class WhatIfResult {
        private ScenarioType scenarioType;
        private String description;
        private boolean feasible;
        private String message;
        private String recommendation;
        private List<ImpactItem> impacts;
    }

    @Data
    public static class ImpactItem {
        private String category;
        private String value;
        private String severity;

        public ImpactItem(String category, String value, String severity) {
            this.category = category;
            this.value = value;
            this.severity = severity;
        }
    }

    @Data
    @Builder
    public static class PrepTimeEquityResult {
        private List<TeacherPrepTime> teacherPrepTimes;
        private double averagePrepMinutes;
        private int minPrepMinutes;
        private int maxPrepMinutes;
        private double equityIndex;
        private int belowAverageCount;
        private List<TeacherPrepTime> belowAverageTeachers;
        private String recommendation;
    }

    @Data
    @Builder
    public static class TeacherPrepTime {
        private Long teacherId;
        private String teacherName;
        private String department;
        private int prepMinutesWeekly;
        private int classesPerWeek;
    }
}
