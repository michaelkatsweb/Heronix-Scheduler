package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Alert Generation Service
 * Location: src/main/java/com/eduscheduler/service/AlertGenerationService.java
 *
 * Routes intervention alerts to appropriate school personnel based on alert severity
 * and type. Integrates with StudentProgressMonitoringService for automated alerting.
 *
 * Features:
 * - Alert generation for intervention patterns
 * - Automatic routing to counselors, administrators, teachers
 * - Priority-based alert handling
 * - Alert deduplication (prevent duplicate alerts)
 * - Alert history tracking
 * - Integration with notification system
 *
 * Alert Routing Rules:
 * - LOW risk: Notify teacher and counselor
 * - MEDIUM risk: Notify teacher, counselor, and grade-level administrator
 * - HIGH risk: Notify teacher, counselor, grade-level admin, and principal
 * - CRITICAL (major incidents): Immediate notification to all parties
 *
 * Alert Types:
 * - ATTENDANCE: Tardiness and absence patterns
 * - ACADEMIC: Failing grades and missing work
 * - BEHAVIOR: Negative incidents
 * - OBSERVATION: Teacher concerns
 * - COMBINED: Multiple risk areas
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Slf4j
@Service
@Transactional
public class AlertGenerationService {

    @Autowired
    private StudentProgressMonitoringService progressMonitoringService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    // In-memory alert tracking (could be moved to database for persistence)
    private final Map<String, LocalDateTime> recentAlerts = new HashMap<>();
    private static final int ALERT_COOLDOWN_HOURS = 24; // Don't send duplicate alerts within 24 hours

    // ========================================================================
    // ALERT GENERATION
    // ========================================================================

    /**
     * Generate and route alert for a student based on risk assessment
     */
    public void generateAlert(Student student) {
        // Get comprehensive risk assessment
        StudentProgressMonitoringService.StudentRiskAssessment assessment =
            progressMonitoringService.getStudentRiskAssessment(student);

        // Only generate alerts if there are risk indicators
        if (assessment.getRiskLevel() == StudentProgressMonitoringService.RiskLevel.NONE) {
            return;
        }

        // Check if we've recently sent an alert for this student
        String alertKey = generateAlertKey(student, assessment);
        if (isRecentAlert(alertKey)) {
            log.debug("Skipping duplicate alert for student {}", student.getFullName());
            return;
        }

        // Generate alert based on risk level
        Alert alert = createAlert(student, assessment);

        // Route alert to appropriate personnel
        routeAlert(alert);

        // Track that we sent this alert
        trackAlert(alertKey);

        log.info("Alert generated and routed for student {}: {} - {}",
            student.getFullName(), alert.getAlertType(), alert.getPriority());
    }

    /**
     * Generate alerts for all at-risk students
     * Called by nightly monitoring job
     */
    public void generateAlertsForAllAtRiskStudents() {
        log.info("Generating alerts for all at-risk students");

        List<Student> allStudents = studentRepository.findAll()
            .stream()
            .filter(s -> s.getDeleted() == null || !s.getDeleted())
            .collect(Collectors.toList());

        int alertsGenerated = 0;
        for (Student student : allStudents) {
            try {
                StudentProgressMonitoringService.StudentRiskAssessment assessment =
                    progressMonitoringService.getStudentRiskAssessment(student);

                if (assessment.getRiskLevel() != StudentProgressMonitoringService.RiskLevel.NONE) {
                    generateAlert(student);
                    alertsGenerated++;
                }
            } catch (Exception e) {
                log.error("Error generating alert for student {}", student.getId(), e);
            }
        }

        log.info("Generated {} alerts", alertsGenerated);
    }

    // ========================================================================
    // ALERT CREATION
    // ========================================================================

    /**
     * Create an alert object from risk assessment
     */
    private Alert createAlert(Student student, StudentProgressMonitoringService.StudentRiskAssessment assessment) {
        Alert alert = new Alert();
        alert.setStudent(student);
        alert.setGeneratedDate(LocalDateTime.now());
        alert.setRiskLevel(assessment.getRiskLevel());

        // Determine alert type based on risk indicators
        List<AlertType> alertTypes = new ArrayList<>();
        if (assessment.isHasAttendanceRisk()) alertTypes.add(AlertType.ATTENDANCE);
        if (assessment.isHasAcademicRisk()) alertTypes.add(AlertType.ACADEMIC);
        if (assessment.isHasBehaviorRisk()) alertTypes.add(AlertType.BEHAVIOR);
        if (assessment.isHasObservationRisk()) alertTypes.add(AlertType.OBSERVATION);

        if (alertTypes.size() > 1) {
            alert.setAlertType(AlertType.COMBINED);
            alert.setSecondaryAlertTypes(alertTypes);
        } else if (alertTypes.size() == 1) {
            alert.setAlertType(alertTypes.get(0));
        } else {
            alert.setAlertType(AlertType.GENERAL);
        }

        // Determine priority based on risk level
        alert.setPriority(mapRiskLevelToPriority(assessment.getRiskLevel()));

        // Generate alert message
        alert.setMessage(generateAlertMessage(student, assessment));

        return alert;
    }

    /**
     * Generate human-readable alert message
     */
    private String generateAlertMessage(Student student, StudentProgressMonitoringService.StudentRiskAssessment assessment) {
        StringBuilder message = new StringBuilder();
        message.append("Student ").append(student.getFullName()).append(" requires intervention.\n\n");

        message.append("Risk Level: ").append(assessment.getRiskLevel()).append("\n\n");

        message.append("Risk Indicators:\n");
        if (assessment.isHasAttendanceRisk()) {
            message.append("• ATTENDANCE: Excessive tardies or absences detected\n");
        }
        if (assessment.isHasAcademicRisk()) {
            message.append("• ACADEMIC: Failing multiple courses or significant missing work\n");
        }
        if (assessment.isHasBehaviorRisk()) {
            message.append("• BEHAVIOR: Multiple negative incidents or major incident\n");
        }
        if (assessment.isHasObservationRisk()) {
            message.append("• TEACHER OBSERVATIONS: Multiple concern-level observations\n");
        }

        message.append("\nRecommended Action: Review student progress and initiate intervention plan.");

        return message.toString();
    }

    /**
     * Map risk level to alert priority
     */
    private AlertPriority mapRiskLevelToPriority(StudentProgressMonitoringService.RiskLevel riskLevel) {
        switch (riskLevel) {
            case HIGH:
                return AlertPriority.URGENT;
            case MEDIUM:
                return AlertPriority.HIGH;
            case LOW:
                return AlertPriority.NORMAL;
            default:
                return AlertPriority.LOW;
        }
    }

    // ========================================================================
    // ALERT ROUTING
    // ========================================================================

    /**
     * Route alert to appropriate personnel based on priority and type
     */
    private void routeAlert(Alert alert) {
        Set<String> recipients = new HashSet<>();

        Student student = alert.getStudent();

        // Always notify counselors (student counselor assignment not yet implemented,
        // so notify all counselors for now - can be enhanced later with specific counselor assignment)
        List<User> counselors = getCounselors();
        counselors.forEach(c -> {
            if (c.getEmail() != null) {
                recipients.add(c.getEmail());
            }
        });

        // Always notify student's teachers
        List<Teacher> studentTeachers = getStudentTeachers(student);
        studentTeachers.forEach(t -> {
            if (t.getEmail() != null) {
                recipients.add(t.getEmail());
            }
        });

        // Route based on priority
        switch (alert.getPriority()) {
            case URGENT:
                // Notify principal and all administrators
                notifyAdministrators(recipients);
                break;

            case HIGH:
                // Notify grade-level administrator
                notifyGradeLevelAdministrator(student, recipients);
                break;

            case NORMAL:
                // Just teacher and counselor (already added above)
                break;

            case LOW:
                // Just teacher and counselor (already added above)
                break;
        }

        // Create in-app notification
        // Use the existing notification system to create alerts
        String title = String.format("[%s] Student Intervention Alert: %s",
            alert.getPriority(), student.getFullName());

        try {
            // Create a global notification for counselors/administrators
            // In production, this would be role-based or targeted
            // Using CAPACITY_WARNING as a placeholder until STUDENT_INTERVENTION type is added
            notificationService.createGlobalNotification(
                Notification.NotificationType.CAPACITY_WARNING,
                title,
                alert.getMessage()
            );

            log.info("Alert notification created for student {}", student.getFullName());
        } catch (Exception e) {
            log.error("Failed to create alert notification", e);
        }
    }

    /**
     * Get all teachers for a student
     * Queries student's active course enrollments to find all assigned teachers
     */
    private List<Teacher> getStudentTeachers(Student student) {
        try {
            // Get student's active enrollments
            List<StudentEnrollment> enrollments = studentEnrollmentRepository
                .findActiveEnrollmentsByStudentId(student.getId());

            // Extract unique teachers from enrolled courses
            Set<Teacher> teachers = new HashSet<>();
            for (StudentEnrollment enrollment : enrollments) {
                // Get course with teacher loaded
                Course course = courseRepository.findById(enrollment.getCourse().getId())
                    .orElse(null);

                if (course != null && course.getTeacher() != null) {
                    teachers.add(course.getTeacher());
                }

                // Note: CoTeachers are separate staff (paraprofessionals, co-teachers)
                // and don't have direct Teacher entity references, so we only include
                // the primary teacher for alert routing
            }

            log.debug("Found {} teachers for student {}", teachers.size(), student.getFullName());
            return new ArrayList<>(teachers);

        } catch (Exception e) {
            log.error("Error fetching teachers for student {}", student.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Notify all administrators
     * Adds all administrator emails (SUPER_ADMIN and ADMIN roles) to the recipient list
     */
    private void notifyAdministrators(Set<String> recipients) {
        try {
            // Get all users with administrator roles
            List<User> administrators = userRepository.findAllAdministrators();

            // Add their email addresses to recipients
            for (User admin : administrators) {
                if (admin.getEmail() != null && !admin.getEmail().isEmpty()) {
                    recipients.add(admin.getEmail());
                }
            }

            log.debug("Added {} administrators to alert recipients", administrators.size());

        } catch (Exception e) {
            log.error("Error fetching administrators for alert routing", e);
        }
    }

    /**
     * Notify grade-level administrator
     * Currently adds all administrators since grade-level-specific admin assignment
     * is not yet implemented. In future versions, this could query for administrators
     * assigned to specific grade levels.
     */
    private void notifyGradeLevelAdministrator(Student student, Set<String> recipients) {
        try {
            String gradeLevel = student.getGradeLevel();

            // NOTE: Grade-level-specific administrator assignment not yet implemented
            // For now, notify all administrators for medium/high priority alerts
            // Future enhancement: Add grade level assignment to administrator profiles

            List<User> administrators = userRepository.findAllAdministrators();

            // Add administrator emails to recipients
            for (User admin : administrators) {
                if (admin.getEmail() != null && !admin.getEmail().isEmpty()) {
                    recipients.add(admin.getEmail());
                }
            }

            log.debug("Notified administrators for student in grade {} (grade-level-specific routing not yet implemented)",
                gradeLevel != null ? gradeLevel : "unknown");

        } catch (Exception e) {
            log.error("Error fetching grade-level administrators for alert routing", e);
        }
    }

    /**
     * Get all counselors
     * Queries all users with COUNSELOR role
     */
    private List<User> getCounselors() {
        try {
            List<User> counselors = userRepository.findAllCounselors();
            log.debug("Found {} counselors for alert routing", counselors.size());
            return counselors;
        } catch (Exception e) {
            log.error("Error fetching counselors for alert routing", e);
            return new ArrayList<>();
        }
    }

    // ========================================================================
    // ALERT DEDUPLICATION
    // ========================================================================

    /**
     * Generate unique alert key for deduplication
     */
    private String generateAlertKey(Student student, StudentProgressMonitoringService.StudentRiskAssessment assessment) {
        return String.format("%d_%s_%s",
            student.getId(),
            assessment.getRiskLevel(),
            assessment.getAssessmentDate());
    }

    /**
     * Check if alert was recently sent
     */
    private boolean isRecentAlert(String alertKey) {
        LocalDateTime lastAlert = recentAlerts.get(alertKey);
        if (lastAlert == null) {
            return false;
        }

        LocalDateTime cooldownThreshold = LocalDateTime.now().minusHours(ALERT_COOLDOWN_HOURS);
        return lastAlert.isAfter(cooldownThreshold);
    }

    /**
     * Track that an alert was sent
     */
    private void trackAlert(String alertKey) {
        recentAlerts.put(alertKey, LocalDateTime.now());

        // Clean up old alerts (older than 7 days)
        LocalDateTime cleanupThreshold = LocalDateTime.now().minusDays(7);
        recentAlerts.entrySet().removeIf(entry -> entry.getValue().isBefore(cleanupThreshold));
    }

    // ========================================================================
    // IMMEDIATE ALERTS (For Critical Incidents)
    // ========================================================================

    /**
     * Generate immediate alert for critical incident
     * Bypasses normal cooldown and priority rules
     */
    public void generateImmediateAlert(Student student, String reason, String details) {
        Alert alert = new Alert();
        alert.setStudent(student);
        alert.setGeneratedDate(LocalDateTime.now());
        alert.setAlertType(AlertType.CRITICAL);
        alert.setPriority(AlertPriority.URGENT);
        alert.setMessage(String.format(
            "IMMEDIATE ATTENTION REQUIRED\n\n" +
            "Student: %s\n" +
            "Reason: %s\n\n" +
            "Details:\n%s",
            student.getFullName(), reason, details
        ));

        routeAlert(alert);

        log.warn("IMMEDIATE ALERT generated for student {}: {}",
            student.getFullName(), reason);
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Alert data structure
     */
    public static class Alert {
        private Student student;
        private LocalDateTime generatedDate;
        private AlertType alertType;
        private List<AlertType> secondaryAlertTypes;
        private AlertPriority priority;
        private StudentProgressMonitoringService.RiskLevel riskLevel;
        private String message;

        // Getters and setters
        public Student getStudent() { return student; }
        public void setStudent(Student student) { this.student = student; }

        public LocalDateTime getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }

        public AlertType getAlertType() { return alertType; }
        public void setAlertType(AlertType alertType) { this.alertType = alertType; }

        public List<AlertType> getSecondaryAlertTypes() { return secondaryAlertTypes; }
        public void setSecondaryAlertTypes(List<AlertType> secondaryAlertTypes) {
            this.secondaryAlertTypes = secondaryAlertTypes;
        }

        public AlertPriority getPriority() { return priority; }
        public void setPriority(AlertPriority priority) { this.priority = priority; }

        public StudentProgressMonitoringService.RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(StudentProgressMonitoringService.RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Alert type enumeration
     */
    public enum AlertType {
        ATTENDANCE,    // Attendance issues
        ACADEMIC,      // Academic decline
        BEHAVIOR,      // Behavior incidents
        OBSERVATION,   // Teacher concerns
        COMBINED,      // Multiple risk areas
        CRITICAL,      // Immediate attention required
        GENERAL        // Other
    }

    /**
     * Alert priority enumeration
     */
    public enum AlertPriority {
        LOW,           // Monitor
        NORMAL,        // Review within 3 days
        HIGH,          // Review within 1 day
        URGENT         // Immediate attention
    }
}
