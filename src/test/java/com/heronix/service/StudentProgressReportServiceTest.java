package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.enums.CourseType;
import com.heronix.model.enums.EnrollmentStatus;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StudentProgressReportService
 * Tests the fixed methods from December 10, 2025 (Progress Report fixes)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StudentProgressReportServiceTest {

    @Autowired
    private StudentProgressReportService progressReportService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ClassroomGradeEntryRepository gradeEntryRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private BehaviorIncidentRepository behaviorIncidentRepository;

    @Autowired
    private TeacherObservationNoteRepository observationNoteRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private Student testStudent;
    private Course mathCourse;
    private Course englishCourse;
    private Course scienceCourse;
    private Schedule testSchedule;
    private Teacher testTeacher;

    @BeforeEach
    public void setup() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setName("2025-2026 School Year");
        testSchedule.setPeriod(com.eduscheduler.model.enums.SchedulePeriod.YEARLY);
        testSchedule.setScheduleType(com.eduscheduler.model.enums.ScheduleType.TRADITIONAL);
        testSchedule.setStatus(com.eduscheduler.model.enums.ScheduleStatus.DRAFT);
        testSchedule.setStartDate(LocalDate.of(2025, 8, 15));
        testSchedule.setEndDate(LocalDate.of(2026, 6, 15));
        testSchedule = scheduleRepository.save(testSchedule);

        // Create test student
        testStudent = new Student();
        testStudent.setStudentId("REPORT_TEST_001");
        testStudent.setFirstName("Report");
        testStudent.setLastName("Testing");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);
        testStudent = studentRepository.save(testStudent);

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setEmployeeId("TEACHER_RPT_001");
        testTeacher.setName("Report Teacher");
        testTeacher.setFirstName("Report");
        testTeacher.setLastName("Teacher");
        testTeacher = teacherRepository.save(testTeacher);

        // Create test courses
        mathCourse = createCourse("Algebra II", "MATH_RPT_101");
        englishCourse = createCourse("English 10", "ENG_RPT_101");
        scienceCourse = createCourse("Biology", "SCI_RPT_101");
    }

    private Course createCourse(String name, String code) {
        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setCourseType(CourseType.REGULAR);
        course.setSubject("Core");
        return courseRepository.save(course);
    }

    private StudentEnrollment createEnrollment(Student student, Course course, EnrollmentStatus status) {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setSchedule(testSchedule);
        enrollment.setStatus(status);
        return studentEnrollmentRepository.save(enrollment);
    }

    private ClassroomGradeEntry createGradeEntry(Student student, Course course,
                                                 Double pointsEarned, Double pointsPossible,
                                                 LocalDate date, boolean isMissing) {
        ClassroomGradeEntry entry = new ClassroomGradeEntry();
        entry.setStudent(student);
        entry.setCourse(course);
        entry.setTeacher(testTeacher);
        entry.setPointsEarned(pointsEarned);
        entry.setPointsPossible(pointsPossible);
        entry.setAssignmentDate(date);
        entry.setIsMissingWork(isMissing);
        entry.setAssignmentName("Assignment " + System.currentTimeMillis());
        entry.setEnteredByStaffId(testTeacher.getId());

        // Calculate percentage grade if both values exist
        if (pointsEarned != null && pointsPossible != null && pointsPossible > 0) {
            entry.setPercentageGrade((pointsEarned / pointsPossible) * 100.0);
        }

        return gradeEntryRepository.save(entry);
    }

    private AttendanceRecord createAttendance(Student student, Course course, LocalDate date,
                                             AttendanceRecord.AttendanceStatus status, String excuseCode) {
        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setCourse(course);
        record.setAttendanceDate(date);
        record.setStatus(status);
        record.setExcuseCode(excuseCode);
        return attendanceRepository.save(record);
    }

    private BehaviorIncident createBehaviorIncident(Student student, LocalDate date,
                                                   BehaviorIncident.BehaviorType type,
                                                   BehaviorIncident.SeverityLevel severity) {
        BehaviorIncident incident = new BehaviorIncident();
        incident.setStudent(student);
        incident.setReportingTeacher(testTeacher);
        incident.setIncidentDate(date);
        incident.setIncidentTime(java.time.LocalTime.of(10, 0));
        incident.setBehaviorType(type);
        incident.setSeverityLevel(severity);
        incident.setIncidentDescription("Test incident");
        incident.setEnteredByStaffId(testTeacher.getId());
        return behaviorIncidentRepository.save(incident);
    }

    private TeacherObservationNote createObservation(Student student, Course course, LocalDate date,
                                                     TeacherObservationNote.ObservationRating rating,
                                                     boolean flagForIntervention) {
        TeacherObservationNote note = new TeacherObservationNote();
        note.setStudent(student);
        note.setCourse(course);
        note.setTeacher(testTeacher);
        note.setObservationDate(date);
        note.setObservationRating(rating);
        note.setObservationNotes("Test observation note");
        note.setIsFlagForIntervention(flagForIntervention);
        return observationNoteRepository.save(note);
    }

    // ========================================================================
    // TEST 1: COURSE GRADES POPULATION (CRITICAL FIX - Dec 10, 2025)
    // ========================================================================

    @Test
    public void testCourseGradesPopulation_UsesActualEnrollments() {
        // Arrange: Student enrolled in 3 courses with grades
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, englishCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, scienceCourse, EnrollmentStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        // Math: 85% (85/100)
        createGradeEntry(testStudent, mathCourse, 85.0, 100.0, today.minusDays(5), false);
        // English: 90% (90/100)
        createGradeEntry(testStudent, englishCourse, 90.0, 100.0, today.minusDays(3), false);
        // Science: 78% (78/100)
        createGradeEntry(testStudent, scienceCourse, 78.0, 100.0, today.minusDays(1), false);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        assertNotNull(report, "Report should not be null");
        assertNotNull(report.getAcademicSummary(), "Academic summary should not be null");

        Map<String, Double> courseGrades = report.getAcademicSummary().getCourseGrades();
        assertNotNull(courseGrades, "Course grades map should not be null");
        assertFalse(courseGrades.isEmpty(), "Course grades should be populated");

        // Verify all 3 courses are in the map
        assertTrue(courseGrades.containsKey("Algebra II"), "Should contain Math course");
        assertTrue(courseGrades.containsKey("English 10"), "Should contain English course");
        assertTrue(courseGrades.containsKey("Biology"), "Should contain Science course");

        // Verify grade values
        assertEquals(85.0, courseGrades.get("Algebra II"), 0.1, "Math grade should be 85%");
        assertEquals(90.0, courseGrades.get("English 10"), 0.1, "English grade should be 90%");
        assertEquals(78.0, courseGrades.get("Biology"), 0.1, "Science grade should be 78%");

        System.out.println("✓ Course grades populated from actual enrollments");
        System.out.println("  - Algebra II: " + courseGrades.get("Algebra II") + "%");
        System.out.println("  - English 10: " + courseGrades.get("English 10") + "%");
        System.out.println("  - Biology: " + courseGrades.get("Biology") + "%");
    }

    @Test
    public void testCourseGradesPopulation_OnlyActiveEnrollments() {
        // Arrange: Mix of enrollment statuses
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, englishCourse, EnrollmentStatus.COMPLETED);
        createEnrollment(testStudent, scienceCourse, EnrollmentStatus.DROPPED);

        LocalDate today = LocalDate.now();
        createGradeEntry(testStudent, mathCourse, 85.0, 100.0, today.minusDays(5), false);
        createGradeEntry(testStudent, englishCourse, 90.0, 100.0, today.minusDays(3), false);
        createGradeEntry(testStudent, scienceCourse, 78.0, 100.0, today.minusDays(1), false);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        Map<String, Double> courseGrades = report.getAcademicSummary().getCourseGrades();

        // Should only include ACTIVE enrollments
        assertEquals(1, courseGrades.size(), "Should only have 1 active enrollment");
        assertTrue(courseGrades.containsKey("Algebra II"), "Should contain Math course (ACTIVE)");
        assertFalse(courseGrades.containsKey("English 10"), "Should NOT contain English (COMPLETED)");
        assertFalse(courseGrades.containsKey("Biology"), "Should NOT contain Science (DROPPED)");

        System.out.println("✓ Only ACTIVE enrollments included in course grades");
    }

    @Test
    public void testCourseGradesPopulation_HandlesNullGrades() {
        // Arrange: Student enrolled but no grades yet
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, englishCourse, EnrollmentStatus.ACTIVE);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        Map<String, Double> courseGrades = report.getAcademicSummary().getCourseGrades();

        // Courses with no grades should not be in the map (calculateCurrentGrade returns null)
        assertEquals(0, courseGrades.size(), "Courses with no grades should not be included");

        System.out.println("✓ Courses with no grades excluded from report");
    }

    // ========================================================================
    // TEST 2: ACADEMIC SUMMARY GENERATION
    // ========================================================================

    @Test
    public void testAcademicSummary_BasicMetrics() {
        // Arrange: Weekly assignments
        LocalDate today = LocalDate.now();
        createGradeEntry(testStudent, mathCourse, 85.0, 100.0, today.minusDays(6), false);
        createGradeEntry(testStudent, mathCourse, 90.0, 100.0, today.minusDays(4), false);
        createGradeEntry(testStudent, mathCourse, 50.0, 100.0, today.minusDays(2), false); // Failing
        createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(1), true); // Missing

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        StudentProgressReportService.AcademicSummary academic = report.getAcademicSummary();

        assertEquals(4, academic.getTotalAssignments(), "Should have 4 total assignments");
        assertEquals(1, academic.getMissingAssignments(), "Should have 1 missing assignment");
        assertEquals(1, academic.getFailingGrades(), "Should have 1 failing grade (< 60%)");

        // Average: (85 + 90 + 50) / 3 = 75.0
        assertEquals(75.0, academic.getAverageGrade(), 0.1, "Average should be 75%");

        System.out.println("✓ Academic summary calculated correctly");
        System.out.println("  - Total assignments: " + academic.getTotalAssignments());
        System.out.println("  - Missing: " + academic.getMissingAssignments());
        System.out.println("  - Failing: " + academic.getFailingGrades());
        System.out.println("  - Average: " + academic.getAverageGrade() + "%");
    }

    // ========================================================================
    // TEST 3: ATTENDANCE SUMMARY GENERATION
    // ========================================================================

    @Test
    public void testAttendanceSummary_AllStatuses() {
        // Arrange: Week of attendance records
        LocalDate today = LocalDate.now();
        createAttendance(testStudent, mathCourse, today.minusDays(6), AttendanceRecord.AttendanceStatus.PRESENT, null);
        createAttendance(testStudent, mathCourse, today.minusDays(5), AttendanceRecord.AttendanceStatus.PRESENT, null);
        createAttendance(testStudent, mathCourse, today.minusDays(4), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, mathCourse, today.minusDays(3), AttendanceRecord.AttendanceStatus.ABSENT, "MEDICAL");
        createAttendance(testStudent, mathCourse, today.minusDays(2), AttendanceRecord.AttendanceStatus.ABSENT, null); // Unexcused
        createAttendance(testStudent, mathCourse, today.minusDays(1), AttendanceRecord.AttendanceStatus.PRESENT, null);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        StudentProgressReportService.AttendanceSummary attendance = report.getAttendanceSummary();

        assertEquals(6, attendance.getTotalDays(), "Should have 6 total days");
        assertEquals(3, attendance.getPresentDays(), "Should have 3 present days");
        assertEquals(2, attendance.getAbsentDays(), "Should have 2 absent days");
        assertEquals(1, attendance.getTardyDays(), "Should have 1 tardy day");
        assertEquals(1, attendance.getUnexcusedAbsences(), "Should have 1 unexcused absence");

        // Attendance rate: 3 / 6 = 50%
        assertEquals(50.0, attendance.getAttendanceRate(), 0.1, "Attendance rate should be 50%");

        System.out.println("✓ Attendance summary calculated correctly");
        System.out.println("  - Present: " + attendance.getPresentDays() + "/" + attendance.getTotalDays());
        System.out.println("  - Absent: " + attendance.getAbsentDays() + " (unexcused: " + attendance.getUnexcusedAbsences() + ")");
        System.out.println("  - Tardy: " + attendance.getTardyDays());
        System.out.println("  - Rate: " + attendance.getAttendanceRate() + "%");
    }

    // ========================================================================
    // TEST 4: BEHAVIOR SUMMARY GENERATION
    // ========================================================================

    @Test
    public void testBehaviorSummary_PositiveAndNegative() {
        // Arrange: Mix of behavior incidents
        LocalDate today = LocalDate.now();
        createBehaviorIncident(testStudent, today.minusDays(6), BehaviorIncident.BehaviorType.POSITIVE, null);
        createBehaviorIncident(testStudent, today.minusDays(5), BehaviorIncident.BehaviorType.POSITIVE, null);
        createBehaviorIncident(testStudent, today.minusDays(4), BehaviorIncident.BehaviorType.NEGATIVE, BehaviorIncident.SeverityLevel.MINOR);
        createBehaviorIncident(testStudent, today.minusDays(3), BehaviorIncident.BehaviorType.NEGATIVE, BehaviorIncident.SeverityLevel.MAJOR);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        StudentProgressReportService.BehaviorSummary behavior = report.getBehaviorSummary();

        assertEquals(2, behavior.getPositiveIncidents(), "Should have 2 positive incidents");
        assertEquals(2, behavior.getNegativeIncidents(), "Should have 2 negative incidents");
        assertEquals(1, behavior.getMajorIncidents(), "Should have 1 major incident");

        System.out.println("✓ Behavior summary calculated correctly");
        System.out.println("  - Positive: " + behavior.getPositiveIncidents());
        System.out.println("  - Negative: " + behavior.getNegativeIncidents() + " (major: " + behavior.getMajorIncidents() + ")");
    }

    // ========================================================================
    // TEST 5: OBSERVATION SUMMARY GENERATION
    // ========================================================================

    @Test
    public void testObservationSummary_RatingsAndFlags() {
        // Arrange: Teacher observations
        LocalDate today = LocalDate.now();
        createObservation(testStudent, mathCourse, today.minusDays(6), TeacherObservationNote.ObservationRating.EXCELLENT, false);
        createObservation(testStudent, mathCourse, today.minusDays(5), TeacherObservationNote.ObservationRating.EXCELLENT, false);
        createObservation(testStudent, mathCourse, today.minusDays(4), TeacherObservationNote.ObservationRating.CONCERN, true);
        createObservation(testStudent, mathCourse, today.minusDays(3), TeacherObservationNote.ObservationRating.CONCERN, true);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        StudentProgressReportService.ObservationSummary observations = report.getObservationSummary();

        assertEquals(4, observations.getTotalObservations(), "Should have 4 total observations");
        assertEquals(2, observations.getExcellentRatings(), "Should have 2 excellent ratings");
        assertEquals(2, observations.getConcernRatings(), "Should have 2 concern ratings");
        assertEquals(2, observations.getInterventionFlags(), "Should have 2 intervention flags");

        System.out.println("✓ Observation summary calculated correctly");
        System.out.println("  - Total: " + observations.getTotalObservations());
        System.out.println("  - Excellent: " + observations.getExcellentRatings());
        System.out.println("  - Concern: " + observations.getConcernRatings());
        System.out.println("  - Flagged for intervention: " + observations.getInterventionFlags());
    }

    // ========================================================================
    // TEST 6: COMPREHENSIVE REPORT GENERATION
    // ========================================================================

    @Test
    public void testComprehensiveReport_AllSections() {
        // Arrange: Complete student data
        LocalDate today = LocalDate.now();

        // Academic
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createGradeEntry(testStudent, mathCourse, 85.0, 100.0, today.minusDays(5), false);

        // Attendance
        createAttendance(testStudent, mathCourse, today.minusDays(3), AttendanceRecord.AttendanceStatus.PRESENT, null);

        // Behavior
        createBehaviorIncident(testStudent, today.minusDays(2), BehaviorIncident.BehaviorType.POSITIVE, null);

        // Observations
        createObservation(testStudent, mathCourse, today.minusDays(1), TeacherObservationNote.ObservationRating.EXCELLENT, false);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        assertNotNull(report, "Report should not be null");
        assertNotNull(report.getStudent(), "Student should be set");
        assertNotNull(report.getStartDate(), "Start date should be set");
        assertNotNull(report.getEndDate(), "End date should be set");
        assertNotNull(report.getGeneratedDate(), "Generated date should be set");
        assertNotNull(report.getAcademicSummary(), "Academic summary should be set");
        assertNotNull(report.getAttendanceSummary(), "Attendance summary should be set");
        assertNotNull(report.getBehaviorSummary(), "Behavior summary should be set");
        assertNotNull(report.getObservationSummary(), "Observation summary should be set");
        assertNotNull(report.getRiskAssessment(), "Risk assessment should be set");
        assertNotNull(report.getRecommendations(), "Recommendations should be set");

        assertEquals(testStudent.getId(), report.getStudent().getId(), "Student ID should match");

        System.out.println("✓ Comprehensive report generated with all sections");
    }

    @Test
    public void testReportFormatting_TextOutput() {
        // Arrange: Simple student data
        LocalDate today = LocalDate.now();
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createGradeEntry(testStudent, mathCourse, 85.0, 100.0, today.minusDays(5), false);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);
        String formattedReport = progressReportService.formatReportAsText(report);

        // Assert
        assertNotNull(formattedReport, "Formatted report should not be null");
        assertFalse(formattedReport.isEmpty(), "Formatted report should not be empty");

        // Verify key sections are present
        assertTrue(formattedReport.contains("WEEKLY STUDENT PROGRESS REPORT"), "Should have title");
        assertTrue(formattedReport.contains("Report Testing"), "Should have student name");
        assertTrue(formattedReport.contains("ACADEMIC PERFORMANCE"), "Should have academic section");
        assertTrue(formattedReport.contains("ATTENDANCE"), "Should have attendance section");
        assertTrue(formattedReport.contains("BEHAVIOR"), "Should have behavior section");
        assertTrue(formattedReport.contains("RECOMMENDATIONS"), "Should have recommendations");

        System.out.println("✓ Report formatted as text successfully");
        System.out.println("\n" + formattedReport);
    }

    // ========================================================================
    // TEST 7: RECOMMENDATION GENERATION
    // ========================================================================

    @Test
    public void testRecommendations_MissingAssignments() {
        // Arrange: 3+ missing assignments
        LocalDate today = LocalDate.now();
        createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(6), true);
        createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(4), true);
        createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(2), true);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        assertTrue(report.getRecommendations().stream()
            .anyMatch(r -> r.contains("missing assignments")),
            "Should recommend addressing missing assignments");

        System.out.println("✓ Recommendation generated for missing assignments");
    }

    @Test
    public void testRecommendations_Tardiness() {
        // Arrange: 3+ tardies
        LocalDate today = LocalDate.now();
        createAttendance(testStudent, mathCourse, today.minusDays(6), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, mathCourse, today.minusDays(4), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, mathCourse, today.minusDays(2), AttendanceRecord.AttendanceStatus.TARDY, null);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        assertTrue(report.getRecommendations().stream()
            .anyMatch(r -> r.contains("tardiness")),
            "Should recommend addressing tardiness");

        System.out.println("✓ Recommendation generated for tardiness pattern");
    }

    @Test
    public void testRecommendations_PositiveReinforcement() {
        // Arrange: 3+ positive behaviors
        LocalDate today = LocalDate.now();
        createBehaviorIncident(testStudent, today.minusDays(6), BehaviorIncident.BehaviorType.POSITIVE, null);
        createBehaviorIncident(testStudent, today.minusDays(4), BehaviorIncident.BehaviorType.POSITIVE, null);
        createBehaviorIncident(testStudent, today.minusDays(2), BehaviorIncident.BehaviorType.POSITIVE, null);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        assertTrue(report.getRecommendations().stream()
            .anyMatch(r -> r.contains("RECOGNITION") || r.contains("positive")),
            "Should recommend positive recognition");

        System.out.println("✓ Recommendation generated for positive behavior recognition");
    }

    @Test
    public void testRecommendations_NoIssues() {
        // Arrange: Perfect student with no issues
        LocalDate today = LocalDate.now();
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createGradeEntry(testStudent, mathCourse, 95.0, 100.0, today.minusDays(5), false);
        createAttendance(testStudent, mathCourse, today.minusDays(3), AttendanceRecord.AttendanceStatus.PRESENT, null);

        // Act
        StudentProgressReportService.StudentWeeklyReport report =
            progressReportService.generateStudentWeeklyReport(testStudent);

        // Assert
        assertFalse(report.getRecommendations().isEmpty(), "Should have at least one recommendation");
        assertTrue(report.getRecommendations().stream()
            .anyMatch(r -> r.contains("Continue monitoring") || r.contains("no immediate concerns")),
            "Should have default recommendation when no issues");

        System.out.println("✓ Default recommendation provided for student with no issues");
    }
}
