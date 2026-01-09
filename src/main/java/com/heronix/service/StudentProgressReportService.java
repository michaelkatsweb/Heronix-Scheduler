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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Student Progress Report Service
 * Location: src/main/java/com/eduscheduler/service/StudentProgressReportService.java
 *
 * Generates comprehensive weekly progress reports for students, teachers, and administrators.
 * Aggregates data from multiple sources to provide actionable insights.
 *
 * Features:
 * - Weekly automated report generation
 * - Multi-level reporting (student, teacher, counselor, administrator)
 * - Comprehensive data aggregation
 * - Trend analysis
 * - Intervention tracking
 * - Export-ready format
 *
 * Report Types:
 * - Student Individual Progress Report
 * - Teacher Classroom Summary Report
 * - Counselor Caseload Report
 * - Administrator School-Wide Report
 *
 * Data Sources:
 * - ClassroomGradeEntry: Academic performance
 * - BehaviorIncident: Behavior tracking
 * - TeacherObservationNote: Teacher insights
 * - AttendanceRecord: Attendance patterns
 *
 * Scheduled Jobs:
 * - Weekly report generation: Sundays at 8:00 PM
 * - Monthly summary: First Sunday of month at 8:00 PM
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Slf4j
@Service
@Transactional
public class StudentProgressReportService {

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
    private StudentProgressMonitoringService progressMonitoringService;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    // ========================================================================
    // SCHEDULED REPORT GENERATION
    // ========================================================================

    /**
     * Weekly automated report generation
     * Runs every Sunday at 8:00 PM
     */
    @Scheduled(cron = "0 0 20 * * SUN")
    @Transactional(readOnly = true)
    public void generateWeeklyReports() {
        log.info("=================================================");
        log.info("Starting weekly progress report generation");
        log.info("=================================================");

        long startTime = System.currentTimeMillis();
        int reportsGenerated = 0;

        try {
            List<Student> allStudents = studentRepository.findAll()
                .stream()
                .filter(s -> s.getDeleted() == null || !s.getDeleted())
                .collect(Collectors.toList());

            log.info("Generating reports for {} active students", allStudents.size());

            for (Student student : allStudents) {
                try {
                    generateStudentWeeklyReport(student);
                    reportsGenerated++;
                } catch (Exception e) {
                    log.error("Failed to generate report for student {}", student.getId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("=================================================");
            log.info("Weekly report generation complete");
            log.info("Reports generated: {}", reportsGenerated);
            log.info("Processing time: {} ms", duration);
            log.info("=================================================");

        } catch (Exception e) {
            log.error("Error during weekly report generation", e);
        }
    }

    // ========================================================================
    // STUDENT INDIVIDUAL REPORT
    // ========================================================================

    /**
     * Generate comprehensive weekly report for a single student
     */
    public StudentWeeklyReport generateStudentWeeklyReport(Student student) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        StudentWeeklyReport report = new StudentWeeklyReport();
        report.setStudent(student);
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedDate(LocalDate.now());

        // Academic Summary
        report.setAcademicSummary(generateAcademicSummary(student, startDate, endDate));

        // Attendance Summary
        report.setAttendanceSummary(generateAttendanceSummary(student, startDate, endDate));

        // Behavior Summary
        report.setBehaviorSummary(generateBehaviorSummary(student, startDate, endDate));

        // Observation Summary
        report.setObservationSummary(generateObservationSummary(student, startDate, endDate));

        // Risk Assessment
        report.setRiskAssessment(progressMonitoringService.getStudentRiskAssessment(student));

        // Recommendations
        report.setRecommendations(generateRecommendations(report));

        log.debug("Generated weekly report for student: {}", student.getFullName());

        return report;
    }

    /**
     * Generate academic performance summary
     */
    private AcademicSummary generateAcademicSummary(Student student, LocalDate startDate, LocalDate endDate) {
        AcademicSummary summary = new AcademicSummary();

        // Get grade entries for the week
        List<ClassroomGradeEntry> weeklyGrades =
            gradeEntryRepository.findByStudentAndAssignmentDateBetween(student, startDate, endDate);

        summary.setTotalAssignments(weeklyGrades.size());

        // Count missing work
        Long missingCount = gradeEntryRepository.countByStudentAndIsMissingWorkAndDateBetween(
            student, startDate, endDate);
        summary.setMissingAssignments(missingCount.intValue());

        // Calculate average grade
        if (!weeklyGrades.isEmpty()) {
            double totalPercentage = weeklyGrades.stream()
                .filter(g -> g.getPercentageGrade() != null)
                .mapToDouble(ClassroomGradeEntry::getPercentageGrade)
                .average()
                .orElse(0.0);
            summary.setAverageGrade(totalPercentage);
        }

        // Count failing grades
        long failingCount = weeklyGrades.stream()
            .filter(g -> g.getPercentageGrade() != null && g.getPercentageGrade() < 60.0)
            .count();
        summary.setFailingGrades((int) failingCount);

        // Get course-level performance for all enrolled courses
        Map<String, Double> courseGrades = new HashMap<>();
        try {
            List<StudentEnrollment> activeEnrollments =
                studentEnrollmentRepository.findByStudentId(student.getId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

            for (StudentEnrollment enrollment : activeEnrollments) {
                Course course = enrollment.getCourse();
                if (course != null) {
                    // Use the progress monitoring service to calculate current grade
                    Double currentGrade = progressMonitoringService.calculateCurrentGrade(student, course);
                    if (currentGrade != null) {
                        courseGrades.put(course.getCourseName(), currentGrade);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calculating course grades for student {}: {}",
                student.getId(), e.getMessage());
        }
        summary.setCourseGrades(courseGrades);

        return summary;
    }

    /**
     * Generate attendance summary
     */
    private AttendanceSummary generateAttendanceSummary(Student student, LocalDate startDate, LocalDate endDate) {
        AttendanceSummary summary = new AttendanceSummary();

        List<AttendanceRecord> weeklyAttendance =
            attendanceRepository.findByStudentIdAndAttendanceDateBetween(student.getId(), startDate, endDate);

        summary.setTotalDays(weeklyAttendance.size());

        // Count by status
        long presentCount = weeklyAttendance.stream()
            .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
            .count();
        summary.setPresentDays((int) presentCount);

        long absentCount = weeklyAttendance.stream()
            .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT)
            .count();
        summary.setAbsentDays((int) absentCount);

        long tardyCount = weeklyAttendance.stream()
            .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.TARDY)
            .count();
        summary.setTardyDays((int) tardyCount);

        // Count unexcused absences (absences without an excuse code)
        long unexcusedCount = weeklyAttendance.stream()
            .filter(a -> a.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT &&
                        (a.getExcuseCode() == null || a.getExcuseCode().isEmpty()))
            .count();
        summary.setUnexcusedAbsences((int) unexcusedCount);

        // Calculate attendance rate
        if (weeklyAttendance.size() > 0) {
            double attendanceRate = (presentCount * 100.0) / weeklyAttendance.size();
            summary.setAttendanceRate(attendanceRate);
        }

        return summary;
    }

    /**
     * Generate behavior summary
     */
    private BehaviorSummary generateBehaviorSummary(Student student, LocalDate startDate, LocalDate endDate) {
        BehaviorSummary summary = new BehaviorSummary();

        // Positive behaviors
        List<BehaviorIncident> positiveIncidents =
            behaviorIncidentRepository.findByStudentAndBehaviorTypeAndIncidentDateBetween(
                student, BehaviorIncident.BehaviorType.POSITIVE, startDate, endDate);
        summary.setPositiveIncidents(positiveIncidents.size());

        // Negative behaviors
        List<BehaviorIncident> negativeIncidents =
            behaviorIncidentRepository.findByStudentAndBehaviorTypeAndIncidentDateBetween(
                student, BehaviorIncident.BehaviorType.NEGATIVE, startDate, endDate);
        summary.setNegativeIncidents(negativeIncidents.size());

        // Major incidents
        long majorCount = negativeIncidents.stream()
            .filter(i -> i.getSeverityLevel() == BehaviorIncident.SeverityLevel.MAJOR)
            .count();
        summary.setMajorIncidents((int) majorCount);

        // Admin referrals
        long referralCount = negativeIncidents.stream()
            .filter(BehaviorIncident::getAdminReferralRequired)
            .count();
        summary.setAdminReferrals((int) referralCount);

        // Parent contact needed
        List<BehaviorIncident> uncontacted =
            behaviorIncidentRepository.findUncontactedParentIncidents(student);
        summary.setParentContactNeeded(
            (int) uncontacted.stream()
                .filter(i -> !i.getIncidentDate().isBefore(startDate))
                .count()
        );

        return summary;
    }

    /**
     * Generate teacher observation summary
     */
    private ObservationSummary generateObservationSummary(Student student, LocalDate startDate, LocalDate endDate) {
        ObservationSummary summary = new ObservationSummary();

        List<TeacherObservationNote> weeklyObservations =
            observationNoteRepository.findByStudentAndObservationDateBetween(student, startDate, endDate);

        summary.setTotalObservations(weeklyObservations.size());

        // Count by rating
        long excellentCount = weeklyObservations.stream()
            .filter(o -> o.getObservationRating() == TeacherObservationNote.ObservationRating.EXCELLENT)
            .count();
        summary.setExcellentRatings((int) excellentCount);

        long concernCount = weeklyObservations.stream()
            .filter(o -> o.getObservationRating() == TeacherObservationNote.ObservationRating.CONCERN)
            .count();
        summary.setConcernRatings((int) concernCount);

        // Intervention flags
        long flaggedCount = weeklyObservations.stream()
            .filter(TeacherObservationNote::getIsFlagForIntervention)
            .count();
        summary.setInterventionFlags((int) flaggedCount);

        // Get recent observations text
        List<String> recentNotes = weeklyObservations.stream()
            .limit(3)
            .map(TeacherObservationNote::getObservationNotes)
            .collect(Collectors.toList());
        summary.setRecentObservations(recentNotes);

        return summary;
    }

    /**
     * Generate recommendations based on report data
     */
    private List<String> generateRecommendations(StudentWeeklyReport report) {
        List<String> recommendations = new ArrayList<>();

        // Academic recommendations
        if (report.getAcademicSummary().getMissingAssignments() >= 3) {
            recommendations.add("ACADEMIC: Schedule time with student to complete missing assignments");
        }
        if (report.getAcademicSummary().getFailingGrades() >= 2) {
            recommendations.add("ACADEMIC: Consider academic intervention or tutoring support");
        }

        // Attendance recommendations
        if (report.getAttendanceSummary().getTardyDays() >= 3) {
            recommendations.add("ATTENDANCE: Discuss tardiness pattern with student and parents");
        }
        if (report.getAttendanceSummary().getUnexcusedAbsences() >= 2) {
            recommendations.add("ATTENDANCE: Contact parents regarding unexcused absences");
        }

        // Behavior recommendations
        if (report.getBehaviorSummary().getMajorIncidents() > 0) {
            recommendations.add("BEHAVIOR: Review behavior intervention plan with student");
        }
        if (report.getBehaviorSummary().getParentContactNeeded() > 0) {
            recommendations.add("BEHAVIOR: Contact parents regarding recent incidents");
        }

        // Observation recommendations
        if (report.getObservationSummary().getInterventionFlags() >= 2) {
            recommendations.add("INTERVENTION: Schedule team meeting to develop support plan");
        }

        // Risk-based recommendations
        StudentProgressMonitoringService.RiskLevel riskLevel =
            report.getRiskAssessment().getRiskLevel();
        if (riskLevel == StudentProgressMonitoringService.RiskLevel.HIGH) {
            recommendations.add("URGENT: Immediate intervention required - contact counselor and administrator");
        } else if (riskLevel == StudentProgressMonitoringService.RiskLevel.MEDIUM) {
            recommendations.add("ACTION NEEDED: Schedule intervention planning meeting within 3 days");
        }

        // Positive reinforcement
        if (report.getBehaviorSummary().getPositiveIncidents() >= 3) {
            recommendations.add("RECOGNITION: Acknowledge positive behavior with student and parents");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Continue monitoring student progress - no immediate concerns identified");
        }

        return recommendations;
    }

    // ========================================================================
    // REPORT FORMATTING
    // ========================================================================

    /**
     * Format report as human-readable text
     */
    public String formatReportAsText(StudentWeeklyReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("=".repeat(80)).append("\n");
        sb.append("WEEKLY STUDENT PROGRESS REPORT\n");
        sb.append("=".repeat(80)).append("\n\n");

        sb.append("Student: ").append(report.getStudent().getFullName()).append("\n");
        sb.append("Grade Level: ").append(report.getStudent().getGradeLevel()).append("\n");
        sb.append("Report Period: ")
            .append(report.getStartDate().format(DATE_FORMATTER))
            .append(" - ")
            .append(report.getEndDate().format(DATE_FORMATTER)).append("\n");
        sb.append("Generated: ").append(report.getGeneratedDate().format(DATE_FORMATTER)).append("\n\n");

        // Academic Summary
        sb.append("-".repeat(80)).append("\n");
        sb.append("ACADEMIC PERFORMANCE\n");
        sb.append("-".repeat(80)).append("\n");
        AcademicSummary academic = report.getAcademicSummary();
        sb.append(String.format("Total Assignments: %d\n", academic.getTotalAssignments()));
        sb.append(String.format("Missing Assignments: %d\n", academic.getMissingAssignments()));
        sb.append(String.format("Average Grade: %.1f%%\n", academic.getAverageGrade()));
        sb.append(String.format("Failing Grades: %d\n\n", academic.getFailingGrades()));

        // Attendance Summary
        sb.append("-".repeat(80)).append("\n");
        sb.append("ATTENDANCE\n");
        sb.append("-".repeat(80)).append("\n");
        AttendanceSummary attendance = report.getAttendanceSummary();
        sb.append(String.format("Present: %d days\n", attendance.getPresentDays()));
        sb.append(String.format("Absent: %d days\n", attendance.getAbsentDays()));
        sb.append(String.format("Tardy: %d days\n", attendance.getTardyDays()));
        sb.append(String.format("Attendance Rate: %.1f%%\n\n", attendance.getAttendanceRate()));

        // Behavior Summary
        sb.append("-".repeat(80)).append("\n");
        sb.append("BEHAVIOR\n");
        sb.append("-".repeat(80)).append("\n");
        BehaviorSummary behavior = report.getBehaviorSummary();
        sb.append(String.format("Positive Incidents: %d\n", behavior.getPositiveIncidents()));
        sb.append(String.format("Negative Incidents: %d\n", behavior.getNegativeIncidents()));
        sb.append(String.format("Major Incidents: %d\n\n", behavior.getMajorIncidents()));

        // Risk Assessment
        sb.append("-".repeat(80)).append("\n");
        sb.append("RISK ASSESSMENT\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append("Risk Level: ").append(report.getRiskAssessment().getRiskLevel()).append("\n\n");

        // Recommendations
        sb.append("-".repeat(80)).append("\n");
        sb.append("RECOMMENDATIONS\n");
        sb.append("-".repeat(80)).append("\n");
        for (int i = 0; i < report.getRecommendations().size(); i++) {
            sb.append(String.format("%d. %s\n", i + 1, report.getRecommendations().get(i)));
        }

        sb.append("\n").append("=".repeat(80)).append("\n");

        return sb.toString();
    }

    // ========================================================================
    // INNER CLASSES - REPORT DATA STRUCTURES
    // ========================================================================

    public static class StudentWeeklyReport {
        private Student student;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate generatedDate;
        private AcademicSummary academicSummary;
        private AttendanceSummary attendanceSummary;
        private BehaviorSummary behaviorSummary;
        private ObservationSummary observationSummary;
        private StudentProgressMonitoringService.StudentRiskAssessment riskAssessment;
        private List<String> recommendations;

        // Getters and setters
        public Student getStudent() { return student; }
        public void setStudent(Student student) { this.student = student; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public LocalDate getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }

        public AcademicSummary getAcademicSummary() { return academicSummary; }
        public void setAcademicSummary(AcademicSummary academicSummary) { this.academicSummary = academicSummary; }

        public AttendanceSummary getAttendanceSummary() { return attendanceSummary; }
        public void setAttendanceSummary(AttendanceSummary attendanceSummary) {
            this.attendanceSummary = attendanceSummary;
        }

        public BehaviorSummary getBehaviorSummary() { return behaviorSummary; }
        public void setBehaviorSummary(BehaviorSummary behaviorSummary) { this.behaviorSummary = behaviorSummary; }

        public ObservationSummary getObservationSummary() { return observationSummary; }
        public void setObservationSummary(ObservationSummary observationSummary) {
            this.observationSummary = observationSummary;
        }

        public StudentProgressMonitoringService.StudentRiskAssessment getRiskAssessment() { return riskAssessment; }
        public void setRiskAssessment(StudentProgressMonitoringService.StudentRiskAssessment riskAssessment) {
            this.riskAssessment = riskAssessment;
        }

        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    public static class AcademicSummary {
        private int totalAssignments;
        private int missingAssignments;
        private double averageGrade;
        private int failingGrades;
        private Map<String, Double> courseGrades;

        // Getters and setters
        public int getTotalAssignments() { return totalAssignments; }
        public void setTotalAssignments(int totalAssignments) { this.totalAssignments = totalAssignments; }

        public int getMissingAssignments() { return missingAssignments; }
        public void setMissingAssignments(int missingAssignments) { this.missingAssignments = missingAssignments; }

        public double getAverageGrade() { return averageGrade; }
        public void setAverageGrade(double averageGrade) { this.averageGrade = averageGrade; }

        public int getFailingGrades() { return failingGrades; }
        public void setFailingGrades(int failingGrades) { this.failingGrades = failingGrades; }

        public Map<String, Double> getCourseGrades() { return courseGrades; }
        public void setCourseGrades(Map<String, Double> courseGrades) { this.courseGrades = courseGrades; }
    }

    public static class AttendanceSummary {
        private int totalDays;
        private int presentDays;
        private int absentDays;
        private int tardyDays;
        private int unexcusedAbsences;
        private double attendanceRate;

        // Getters and setters
        public int getTotalDays() { return totalDays; }
        public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

        public int getPresentDays() { return presentDays; }
        public void setPresentDays(int presentDays) { this.presentDays = presentDays; }

        public int getAbsentDays() { return absentDays; }
        public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }

        public int getTardyDays() { return tardyDays; }
        public void setTardyDays(int tardyDays) { this.tardyDays = tardyDays; }

        public int getUnexcusedAbsences() { return unexcusedAbsences; }
        public void setUnexcusedAbsences(int unexcusedAbsences) { this.unexcusedAbsences = unexcusedAbsences; }

        public double getAttendanceRate() { return attendanceRate; }
        public void setAttendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; }
    }

    public static class BehaviorSummary {
        private int positiveIncidents;
        private int negativeIncidents;
        private int majorIncidents;
        private int adminReferrals;
        private int parentContactNeeded;

        // Getters and setters
        public int getPositiveIncidents() { return positiveIncidents; }
        public void setPositiveIncidents(int positiveIncidents) { this.positiveIncidents = positiveIncidents; }

        public int getNegativeIncidents() { return negativeIncidents; }
        public void setNegativeIncidents(int negativeIncidents) { this.negativeIncidents = negativeIncidents; }

        public int getMajorIncidents() { return majorIncidents; }
        public void setMajorIncidents(int majorIncidents) { this.majorIncidents = majorIncidents; }

        public int getAdminReferrals() { return adminReferrals; }
        public void setAdminReferrals(int adminReferrals) { this.adminReferrals = adminReferrals; }

        public int getParentContactNeeded() { return parentContactNeeded; }
        public void setParentContactNeeded(int parentContactNeeded) { this.parentContactNeeded = parentContactNeeded; }
    }

    public static class ObservationSummary {
        private int totalObservations;
        private int excellentRatings;
        private int concernRatings;
        private int interventionFlags;
        private List<String> recentObservations;

        // Getters and setters
        public int getTotalObservations() { return totalObservations; }
        public void setTotalObservations(int totalObservations) { this.totalObservations = totalObservations; }

        public int getExcellentRatings() { return excellentRatings; }
        public void setExcellentRatings(int excellentRatings) { this.excellentRatings = excellentRatings; }

        public int getConcernRatings() { return concernRatings; }
        public void setConcernRatings(int concernRatings) { this.concernRatings = concernRatings; }

        public int getInterventionFlags() { return interventionFlags; }
        public void setInterventionFlags(int interventionFlags) { this.interventionFlags = interventionFlags; }

        public List<String> getRecentObservations() { return recentObservations; }
        public void setRecentObservations(List<String> recentObservations) {
            this.recentObservations = recentObservations;
        }
    }
}
