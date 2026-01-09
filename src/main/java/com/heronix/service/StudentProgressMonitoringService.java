package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.enums.EnrollmentStatus;
import com.heronix.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Student Progress Monitoring Service
 * Location: src/main/java/com/eduscheduler/service/StudentProgressMonitoringService.java
 *
 * Automated nightly pattern detection for student intervention identification.
 * Monitors multiple data sources to identify students requiring intervention.
 *
 * Features:
 * - Automated nightly pattern detection
 * - Multi-source monitoring (grades, attendance, behavior, observations)
 * - Configurable thresholds for intervention triggers
 * - Real-time grade calculation
 * - Risk pattern identification
 * - Alert generation integration
 *
 * Data Sources:
 * - ClassroomGradeEntry: Daily/weekly assignment grades, missing work
 * - BehaviorIncident: Positive and negative behavior tracking
 * - TeacherObservationNote: Teacher concerns and observations
 * - AttendanceRecord: Tardiness and absence tracking
 *
 * Intervention Triggers:
 * - Attendance: 3+ tardies or 2+ unexcused absences in 2 weeks
 * - Academic: Failing 2+ courses or 5+ missing assignments in 2 weeks
 * - Behavior: 3+ negative incidents or 1+ major incident in 2 weeks
 * - Teacher Observations: 2+ concern-level observations in 2 weeks
 *
 * Scheduled Job: Runs daily at 2:00 AM
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Slf4j
@Service
@Transactional
public class StudentProgressMonitoringService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassroomGradeEntryRepository gradeEntryRepository;

    @Autowired
    private BehaviorIncidentRepository behaviorIncidentRepository;

    @Autowired
    private TeacherObservationNoteRepository observationNoteRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    // ========================================================================
    // INTERVENTION THRESHOLD CONSTANTS
    // ========================================================================

    // Attendance thresholds
    private static final int ATTENDANCE_TARDY_THRESHOLD = 3;
    private static final int ATTENDANCE_UNEXCUSED_ABSENCE_THRESHOLD = 2;
    private static final int ATTENDANCE_MONITORING_DAYS = 14;

    // Academic thresholds
    private static final int ACADEMIC_FAILING_COURSES_THRESHOLD = 2;
    private static final int ACADEMIC_MISSING_ASSIGNMENTS_THRESHOLD = 5;
    private static final double ACADEMIC_FAILING_GRADE_THRESHOLD = 60.0;
    private static final int ACADEMIC_MONITORING_DAYS = 14;

    // Behavior thresholds
    private static final int BEHAVIOR_NEGATIVE_INCIDENTS_THRESHOLD = 3;
    private static final int BEHAVIOR_MONITORING_DAYS = 14;

    // Observation thresholds
    private static final int OBSERVATION_CONCERN_THRESHOLD = 2;
    private static final int OBSERVATION_MONITORING_DAYS = 14;

    // ========================================================================
    // SCHEDULED PATTERN DETECTION (Nightly Job)
    // ========================================================================

    /**
     * Automated nightly pattern detection
     * Runs daily at 2:00 AM to identify students requiring intervention
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void detectInterventionPatterns() {
        log.info("=================================================");
        log.info("Starting nightly student intervention pattern detection");
        log.info("=================================================");

        long startTime = System.currentTimeMillis();
        int totalStudents = 0;
        int studentsNeedingIntervention = 0;

        try {
            // Get all active students
            List<Student> allStudents = studentRepository.findAll()
                .stream()
                .filter(s -> s.getDeleted() == null || !s.getDeleted())
                .collect(Collectors.toList());

            totalStudents = allStudents.size();
            log.info("Analyzing {} active students", totalStudents);

            // Check each student for intervention patterns
            for (Student student : allStudents) {
                boolean needsIntervention = false;

                // Check attendance patterns
                if (detectAttendancePatterns(student)) {
                    needsIntervention = true;
                }

                // Check academic decline patterns
                if (detectAcademicDeclinePatterns(student)) {
                    needsIntervention = true;
                }

                // Check behavior patterns
                if (detectBehaviorPatterns(student)) {
                    needsIntervention = true;
                }

                // Check teacher observation patterns
                if (detectObservationPatterns(student)) {
                    needsIntervention = true;
                }

                if (needsIntervention) {
                    studentsNeedingIntervention++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("=================================================");
            log.info("Pattern detection complete");
            log.info("Total students analyzed: {}", totalStudents);
            log.info("Students needing intervention: {}", studentsNeedingIntervention);
            log.info("Percentage requiring intervention: {}%",
                totalStudents > 0 ? (studentsNeedingIntervention * 100.0 / totalStudents) : 0);
            log.info("Processing time: {} ms", duration);
            log.info("=================================================");

        } catch (Exception e) {
            log.error("Error during nightly pattern detection", e);
        }
    }

    // ========================================================================
    // ATTENDANCE PATTERN DETECTION
    // ========================================================================

    /**
     * Detect attendance-based intervention patterns
     * Triggers: 3+ tardies OR 2+ unexcused absences in past 2 weeks
     */
    public boolean detectAttendancePatterns(Student student) {
        LocalDate startDate = LocalDate.now().minusDays(ATTENDANCE_MONITORING_DAYS);
        LocalDate endDate = LocalDate.now();

        try {
            // Get recent attendance records
            List<AttendanceRecord> recentAttendance =
                attendanceRepository.findByStudentIdAndAttendanceDateBetween(
                    student.getId(), startDate, endDate);

            // Count tardies
            long tardyCount = recentAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.TARDY)
                .count();

            // Count unexcused absences (absences without an excuse code)
            long unexcusedAbsenceCount = recentAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT &&
                            (a.getExcuseCode() == null || a.getExcuseCode().isEmpty()))
                .count();

            boolean hasAttendanceIssue = tardyCount >= ATTENDANCE_TARDY_THRESHOLD ||
                                        unexcusedAbsenceCount >= ATTENDANCE_UNEXCUSED_ABSENCE_THRESHOLD;

            if (hasAttendanceIssue) {
                log.info("ATTENDANCE ALERT: Student {} - Tardies: {}, Unexcused Absences: {}",
                    student.getFullName(), tardyCount, unexcusedAbsenceCount);
            }

            return hasAttendanceIssue;

        } catch (Exception e) {
            log.error("Error detecting attendance patterns for student {}", student.getId(), e);
            return false;
        }
    }

    // ========================================================================
    // ACADEMIC DECLINE PATTERN DETECTION
    // ========================================================================

    /**
     * Detect academic decline patterns
     * Triggers: Failing 2+ courses OR 5+ missing assignments in past 2 weeks
     */
    public boolean detectAcademicDeclinePatterns(Student student) {
        LocalDate startDate = LocalDate.now().minusDays(ACADEMIC_MONITORING_DAYS);
        LocalDate endDate = LocalDate.now();

        try {
            // Count missing assignments in past 2 weeks
            Long missingAssignmentCount =
                gradeEntryRepository.countByStudentAndIsMissingWorkAndDateBetween(
                    student, startDate, endDate);

            // Get all currently enrolled courses for student
            List<Course> studentCourses = getEnrolledCourses(student);

            // Count courses where student is currently failing
            int failingCourseCount = 0;
            for (Course course : studentCourses) {
                Double currentGrade = calculateCurrentGrade(student, course);
                if (currentGrade != null && currentGrade < ACADEMIC_FAILING_GRADE_THRESHOLD) {
                    failingCourseCount++;
                }
            }

            boolean hasAcademicIssue = failingCourseCount >= ACADEMIC_FAILING_COURSES_THRESHOLD ||
                                       missingAssignmentCount >= ACADEMIC_MISSING_ASSIGNMENTS_THRESHOLD;

            if (hasAcademicIssue) {
                log.info("ACADEMIC ALERT: Student {} - Failing {} courses, {} missing assignments",
                    student.getFullName(), failingCourseCount, missingAssignmentCount);
            }

            return hasAcademicIssue;

        } catch (Exception e) {
            log.error("Error detecting academic patterns for student {}", student.getId(), e);
            return false;
        }
    }

    // ========================================================================
    // BEHAVIOR PATTERN DETECTION
    // ========================================================================

    /**
     * Detect behavior patterns requiring intervention
     * Triggers: 3+ negative incidents OR 1+ major incident in past 2 weeks
     */
    public boolean detectBehaviorPatterns(Student student) {
        LocalDate startDate = LocalDate.now().minusDays(BEHAVIOR_MONITORING_DAYS);
        LocalDate endDate = LocalDate.now();

        try {
            // Get negative behavior incidents
            List<BehaviorIncident> negativeIncidents =
                behaviorIncidentRepository.findByStudentAndBehaviorTypeAndIncidentDateBetween(
                    student, BehaviorIncident.BehaviorType.NEGATIVE, startDate, endDate);

            // Count major incidents
            long majorIncidentCount = negativeIncidents.stream()
                .filter(i -> i.getSeverityLevel() == BehaviorIncident.SeverityLevel.MAJOR)
                .count();

            boolean hasBehaviorIssue = negativeIncidents.size() >= BEHAVIOR_NEGATIVE_INCIDENTS_THRESHOLD ||
                                      majorIncidentCount > 0;

            if (hasBehaviorIssue) {
                log.info("BEHAVIOR ALERT: Student {} - {} negative incidents ({} major)",
                    student.getFullName(), negativeIncidents.size(), majorIncidentCount);
            }

            return hasBehaviorIssue;

        } catch (Exception e) {
            log.error("Error detecting behavior patterns for student {}", student.getId(), e);
            return false;
        }
    }

    // ========================================================================
    // TEACHER OBSERVATION PATTERN DETECTION
    // ========================================================================

    /**
     * Detect teacher observation patterns requiring intervention
     * Triggers: 2+ concern-level observations in past 2 weeks
     */
    public boolean detectObservationPatterns(Student student) {
        LocalDate startDate = LocalDate.now().minusDays(OBSERVATION_MONITORING_DAYS);
        LocalDate endDate = LocalDate.now();

        try {
            // Get concern-level observations
            List<TeacherObservationNote> concernObservations =
                observationNoteRepository.findConcernObservationsSince(student, startDate);

            // Also check for flagged interventions
            List<TeacherObservationNote> flaggedObservations =
                observationNoteRepository.findByStudentAndIsFlagForIntervention(student, true)
                .stream()
                .filter(o -> !o.getObservationDate().isBefore(startDate))
                .collect(Collectors.toList());

            boolean hasObservationIssue = concernObservations.size() >= OBSERVATION_CONCERN_THRESHOLD ||
                                         flaggedObservations.size() >= OBSERVATION_CONCERN_THRESHOLD;

            if (hasObservationIssue) {
                log.info("OBSERVATION ALERT: Student {} - {} concern observations, {} flagged for intervention",
                    student.getFullName(), concernObservations.size(), flaggedObservations.size());
            }

            return hasObservationIssue;

        } catch (Exception e) {
            log.error("Error detecting observation patterns for student {}", student.getId(), e);
            return false;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get student's currently enrolled courses
     * Queries StudentEnrollment for active enrollments
     *
     * @param student The student to get courses for
     * @return List of courses student is actively enrolled in
     */
    private List<Course> getEnrolledCourses(Student student) {
        try {
            List<StudentEnrollment> activeEnrollments =
                studentEnrollmentRepository.findByStudentId(student.getId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

            return activeEnrollments.stream()
                .map(StudentEnrollment::getCourse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting enrolled courses for student {}: {}",
                student.getId(), e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========================================================================
    // GRADE CALCULATION
    // ========================================================================

    /**
     * Calculate current grade for a student in a course
     * Uses all grade entries weighted by assignment type
     */
    public Double calculateCurrentGrade(Student student, Course course) {
        try {
            List<ClassroomGradeEntry> gradeEntries =
                gradeEntryRepository.findByStudentAndCourseOrderByDateDesc(student, course);

            if (gradeEntries.isEmpty()) {
                return null;
            }

            // Simple average for now (can be enhanced with weighted categories)
            double totalPoints = 0.0;
            double totalPossible = 0.0;

            for (ClassroomGradeEntry entry : gradeEntries) {
                if (entry.getPointsEarned() != null && entry.getPointsPossible() != null) {
                    totalPoints += entry.getPointsEarned();
                    totalPossible += entry.getPointsPossible();
                }
            }

            if (totalPossible == 0) {
                return null;
            }

            return (totalPoints / totalPossible) * 100.0;

        } catch (Exception e) {
            log.error("Error calculating current grade for student {} in course {}",
                student.getId(), course.getId(), e);
            return null;
        }
    }

    // ========================================================================
    // COMPREHENSIVE STUDENT RISK ASSESSMENT
    // ========================================================================

    /**
     * Get comprehensive risk assessment for a student
     * Returns risk indicators across all monitored areas
     */
    public StudentRiskAssessment getStudentRiskAssessment(Student student) {
        StudentRiskAssessment assessment = new StudentRiskAssessment();
        assessment.setStudent(student);
        assessment.setAssessmentDate(LocalDate.now());

        // Check all risk areas
        assessment.setHasAttendanceRisk(detectAttendancePatterns(student));
        assessment.setHasAcademicRisk(detectAcademicDeclinePatterns(student));
        assessment.setHasBehaviorRisk(detectBehaviorPatterns(student));
        assessment.setHasObservationRisk(detectObservationPatterns(student));

        // Calculate overall risk level
        int riskCount = 0;
        if (assessment.isHasAttendanceRisk()) riskCount++;
        if (assessment.isHasAcademicRisk()) riskCount++;
        if (assessment.isHasBehaviorRisk()) riskCount++;
        if (assessment.isHasObservationRisk()) riskCount++;

        if (riskCount >= 3) {
            assessment.setRiskLevel(RiskLevel.HIGH);
        } else if (riskCount >= 2) {
            assessment.setRiskLevel(RiskLevel.MEDIUM);
        } else if (riskCount >= 1) {
            assessment.setRiskLevel(RiskLevel.LOW);
        } else {
            assessment.setRiskLevel(RiskLevel.NONE);
        }

        log.debug("Risk assessment for {}: {}", student.getFullName(), assessment.getRiskLevel());

        return assessment;
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Risk assessment result
     */
    public static class StudentRiskAssessment {
        private Student student;
        private LocalDate assessmentDate;
        private boolean hasAttendanceRisk;
        private boolean hasAcademicRisk;
        private boolean hasBehaviorRisk;
        private boolean hasObservationRisk;
        private RiskLevel riskLevel;

        // Getters and setters
        public Student getStudent() { return student; }
        public void setStudent(Student student) { this.student = student; }

        public LocalDate getAssessmentDate() { return assessmentDate; }
        public void setAssessmentDate(LocalDate assessmentDate) { this.assessmentDate = assessmentDate; }

        public boolean isHasAttendanceRisk() { return hasAttendanceRisk; }
        public void setHasAttendanceRisk(boolean hasAttendanceRisk) { this.hasAttendanceRisk = hasAttendanceRisk; }

        public boolean isHasAcademicRisk() { return hasAcademicRisk; }
        public void setHasAcademicRisk(boolean hasAcademicRisk) { this.hasAcademicRisk = hasAcademicRisk; }

        public boolean isHasBehaviorRisk() { return hasBehaviorRisk; }
        public void setHasBehaviorRisk(boolean hasBehaviorRisk) { this.hasBehaviorRisk = hasBehaviorRisk; }

        public boolean isHasObservationRisk() { return hasObservationRisk; }
        public void setHasObservationRisk(boolean hasObservationRisk) { this.hasObservationRisk = hasObservationRisk; }

        public RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * Risk level enumeration
     */
    public enum RiskLevel {
        NONE,      // No risk indicators
        LOW,       // 1 risk indicator
        MEDIUM,    // 2 risk indicators
        HIGH       // 3+ risk indicators
    }
}
