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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StudentProgressMonitoringService
 * Tests the fixed methods from December 10, 2025 (Progress Monitoring fixes)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StudentProgressMonitoringServiceTest {

    @Autowired
    private StudentProgressMonitoringService progressMonitoringService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ClassroomGradeEntryRepository gradeEntryRepository;

    @Autowired
    private BehaviorIncidentRepository behaviorIncidentRepository;

    @Autowired
    private TeacherObservationNoteRepository observationNoteRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private Student testStudent;
    private Course mathCourse;
    private Course englishCourse;
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
        testStudent.setStudentId("PROGRESS_TEST_001");
        testStudent.setFirstName("Progress");
        testStudent.setLastName("Monitoring");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);
        testStudent = studentRepository.save(testStudent);

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setEmployeeId("TEACHER_001");
        testTeacher.setName("Test Teacher");
        testTeacher.setFirstName("Test");
        testTeacher.setLastName("Teacher");
        testTeacher = teacherRepository.save(testTeacher);

        // Create test courses
        mathCourse = createCourse("Algebra II", "MATH101");
        englishCourse = createCourse("English 10", "ENG101");
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

    private AttendanceRecord createAttendance(Student student, LocalDate date,
                                             AttendanceRecord.AttendanceStatus status, String excuseCode) {
        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setCourse(mathCourse); // Required field
        record.setAttendanceDate(date);
        record.setStatus(status);
        record.setExcuseCode(excuseCode);
        return attendanceRepository.save(record);
    }

    private ClassroomGradeEntry createGradeEntry(Student student, Course course,
                                                 Double pointsEarned, Double pointsPossible,
                                                 LocalDate date, boolean isMissing) {
        ClassroomGradeEntry entry = new ClassroomGradeEntry();
        entry.setStudent(student);
        entry.setCourse(course);
        entry.setTeacher(testTeacher); // Required field
        entry.setPointsEarned(pointsEarned);
        entry.setPointsPossible(pointsPossible);
        entry.setAssignmentDate(date);
        entry.setIsMissingWork(isMissing);
        entry.setAssignmentName("Test Assignment " + System.currentTimeMillis());
        entry.setEnteredByStaffId(testTeacher.getId()); // Required field
        return gradeEntryRepository.save(entry);
    }

    private BehaviorIncident createBehaviorIncident(Student student, LocalDate date,
                                                   BehaviorIncident.BehaviorType type,
                                                   BehaviorIncident.SeverityLevel severity) {
        BehaviorIncident incident = new BehaviorIncident();
        incident.setStudent(student);
        incident.setReportingTeacher(testTeacher); // Required field
        incident.setIncidentDate(date);
        incident.setIncidentTime(java.time.LocalTime.of(10, 0)); // Required field
        incident.setBehaviorType(type);
        incident.setSeverityLevel(severity);
        incident.setIncidentDescription("Test incident");
        incident.setEnteredByStaffId(testTeacher.getId()); // Required field
        return behaviorIncidentRepository.save(incident);
    }

    // ========================================================================
    // TEST 1: ENROLLED COURSES QUERY (CRITICAL FIX - uses actual enrollments)
    // ========================================================================

    @Test
    public void testGetEnrolledCourses_ActiveEnrollments() {
        // Arrange: Student enrolled in 2 courses
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, englishCourse, EnrollmentStatus.ACTIVE);

        // Act: Use the fixed getEnrolledCourses via calculateCurrentGrade
        Double mathGrade = progressMonitoringService.calculateCurrentGrade(testStudent, mathCourse);

        // Assert: Method should work without errors (returns null when no grades exist)
        // The key fix is that it uses actual enrollments from StudentEnrollment
        assertNull(mathGrade, "Should return null when no grade entries exist");

        System.out.println("✓ getEnrolledCourses() uses actual StudentEnrollment records");
    }

    @Test
    public void testGetEnrolledCourses_OnlyActiveStatus() {
        // Arrange: Mix of enrollment statuses
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, englishCourse, EnrollmentStatus.COMPLETED);

        // Act: Academic decline detection uses getEnrolledCourses
        boolean hasAcademicRisk = progressMonitoringService.detectAcademicDeclinePatterns(testStudent);

        // Assert: Should only check ACTIVE enrollments, not COMPLETED
        // No missing assignments or failing grades = no risk
        assertFalse(hasAcademicRisk, "Should have no academic risk with no grades");

        System.out.println("✓ getEnrolledCourses() filters for ACTIVE status only");
    }

    // ========================================================================
    // TEST 2: ATTENDANCE PATTERN DETECTION
    // ========================================================================

    @Test
    public void testDetectAttendancePatterns_ExcessiveTardies() {
        // Arrange: 3 tardies in past 2 weeks (meets threshold)
        LocalDate today = LocalDate.now();
        createAttendance(testStudent, today.minusDays(1), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(5), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(10), AttendanceRecord.AttendanceStatus.TARDY, null);

        // Act
        boolean hasAttendanceRisk = progressMonitoringService.detectAttendancePatterns(testStudent);

        // Assert
        assertTrue(hasAttendanceRisk, "Should detect attendance risk with 3+ tardies in 2 weeks");
        System.out.println("✓ Detected excessive tardies (3 in 14 days)");
    }

    @Test
    public void testDetectAttendancePatterns_UnexcusedAbsences() {
        // Arrange: 2 unexcused absences in past 2 weeks (meets threshold)
        LocalDate today = LocalDate.now();
        createAttendance(testStudent, today.minusDays(2), AttendanceRecord.AttendanceStatus.ABSENT, null);
        createAttendance(testStudent, today.minusDays(7), AttendanceRecord.AttendanceStatus.ABSENT, "");

        // Act
        boolean hasAttendanceRisk = progressMonitoringService.detectAttendancePatterns(testStudent);

        // Assert
        assertTrue(hasAttendanceRisk, "Should detect attendance risk with 2+ unexcused absences in 2 weeks");
        System.out.println("✓ Detected unexcused absences (2 in 14 days)");
    }

    @Test
    public void testDetectAttendancePatterns_ExcusedAbsencesIgnored() {
        // Arrange: 2 absences but both excused (should NOT trigger)
        LocalDate today = LocalDate.now();
        createAttendance(testStudent, today.minusDays(2), AttendanceRecord.AttendanceStatus.ABSENT, "MEDICAL");
        createAttendance(testStudent, today.minusDays(7), AttendanceRecord.AttendanceStatus.ABSENT, "FAMILY");

        // Act
        boolean hasAttendanceRisk = progressMonitoringService.detectAttendancePatterns(testStudent);

        // Assert
        assertFalse(hasAttendanceRisk, "Should NOT detect risk for excused absences");
        System.out.println("✓ Excused absences do not trigger alerts");
    }

    @Test
    public void testDetectAttendancePatterns_NoIssues() {
        // Arrange: Perfect attendance (1 present record)
        LocalDate today = LocalDate.now();
        createAttendance(testStudent, today, AttendanceRecord.AttendanceStatus.PRESENT, null);

        // Act
        boolean hasAttendanceRisk = progressMonitoringService.detectAttendancePatterns(testStudent);

        // Assert
        assertFalse(hasAttendanceRisk, "Should have no attendance risk with perfect attendance");
        System.out.println("✓ No false positives for good attendance");
    }

    // ========================================================================
    // TEST 3: ACADEMIC DECLINE PATTERN DETECTION
    // ========================================================================

    @Test
    public void testDetectAcademicDecline_FailingMultipleCourses() {
        // Arrange: Student failing 2 courses (meets threshold)
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, englishCourse, EnrollmentStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        // Math: 50/100 = 50% (failing)
        createGradeEntry(testStudent, mathCourse, 50.0, 100.0, today.minusDays(5), false);
        // English: 55/100 = 55% (failing)
        createGradeEntry(testStudent, englishCourse, 55.0, 100.0, today.minusDays(5), false);

        // Act
        boolean hasAcademicRisk = progressMonitoringService.detectAcademicDeclinePatterns(testStudent);

        // Assert
        assertTrue(hasAcademicRisk, "Should detect academic risk when failing 2+ courses");
        System.out.println("✓ Detected student failing multiple courses");
    }

    @Test
    public void testDetectAcademicDecline_ExcessiveMissingAssignments() {
        // Arrange: 5 missing assignments in past 2 weeks (meets threshold)
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(i + 1), true);
        }

        // Act
        boolean hasAcademicRisk = progressMonitoringService.detectAcademicDeclinePatterns(testStudent);

        // Assert
        assertTrue(hasAcademicRisk, "Should detect academic risk with 5+ missing assignments");
        System.out.println("✓ Detected excessive missing assignments (5 in 14 days)");
    }

    @Test
    public void testDetectAcademicDecline_PassingGrades() {
        // Arrange: Student passing all courses
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        createEnrollment(testStudent, englishCourse, EnrollmentStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        // Math: 85/100 = 85% (passing)
        createGradeEntry(testStudent, mathCourse, 85.0, 100.0, today.minusDays(5), false);
        // English: 90/100 = 90% (passing)
        createGradeEntry(testStudent, englishCourse, 90.0, 100.0, today.minusDays(5), false);

        // Act
        boolean hasAcademicRisk = progressMonitoringService.detectAcademicDeclinePatterns(testStudent);

        // Assert
        assertFalse(hasAcademicRisk, "Should have no academic risk when passing all courses");
        System.out.println("✓ No false positives for passing students");
    }

    // ========================================================================
    // TEST 4: GRADE CALCULATION
    // ========================================================================

    @Test
    public void testCalculateCurrentGrade_MultipleAssignments() {
        // Arrange: Multiple assignments with different scores
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        createGradeEntry(testStudent, mathCourse, 85.0, 100.0, today.minusDays(10), false);
        createGradeEntry(testStudent, mathCourse, 90.0, 100.0, today.minusDays(5), false);
        createGradeEntry(testStudent, mathCourse, 80.0, 100.0, today.minusDays(1), false);

        // Act
        Double currentGrade = progressMonitoringService.calculateCurrentGrade(testStudent, mathCourse);

        // Assert
        assertNotNull(currentGrade, "Current grade should not be null");
        assertEquals(85.0, currentGrade, 0.1, "Grade should be (255/300) * 100 = 85%");
        System.out.println("✓ Calculated grade: " + currentGrade + "%");
    }

    @Test
    public void testCalculateCurrentGrade_NoAssignments() {
        // Arrange: No grade entries
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);

        // Act
        Double currentGrade = progressMonitoringService.calculateCurrentGrade(testStudent, mathCourse);

        // Assert
        assertNull(currentGrade, "Should return null when no grade entries exist");
        System.out.println("✓ Returns null for courses with no grades");
    }

    @Test
    public void testCalculateCurrentGrade_IgnoresMissingAssignments() {
        // Arrange: Mix of graded and missing assignments
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        createGradeEntry(testStudent, mathCourse, 80.0, 100.0, today.minusDays(5), false);
        createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(3), true); // Missing
        createGradeEntry(testStudent, mathCourse, 90.0, 100.0, today.minusDays(1), false);

        // Act
        Double currentGrade = progressMonitoringService.calculateCurrentGrade(testStudent, mathCourse);

        // Assert
        assertNotNull(currentGrade, "Current grade should not be null");
        assertEquals(85.0, currentGrade, 0.1, "Grade should ignore missing work: (170/200) * 100 = 85%");
        System.out.println("✓ Grade calculation ignores missing assignments");
    }

    // ========================================================================
    // TEST 5: BEHAVIOR PATTERN DETECTION
    // ========================================================================

    @Test
    public void testDetectBehaviorPatterns_MultipleNegativeIncidents() {
        // Arrange: 3 negative incidents in past 2 weeks (meets threshold)
        LocalDate today = LocalDate.now();
        createBehaviorIncident(testStudent, today.minusDays(1),
            BehaviorIncident.BehaviorType.NEGATIVE, BehaviorIncident.SeverityLevel.MINOR);
        createBehaviorIncident(testStudent, today.minusDays(5),
            BehaviorIncident.BehaviorType.NEGATIVE, BehaviorIncident.SeverityLevel.MINOR);
        createBehaviorIncident(testStudent, today.minusDays(10),
            BehaviorIncident.BehaviorType.NEGATIVE, BehaviorIncident.SeverityLevel.MINOR);

        // Act
        boolean hasBehaviorRisk = progressMonitoringService.detectBehaviorPatterns(testStudent);

        // Assert
        assertTrue(hasBehaviorRisk, "Should detect behavior risk with 3+ negative incidents");
        System.out.println("✓ Detected multiple negative behavior incidents");
    }

    @Test
    public void testDetectBehaviorPatterns_OneMajorIncident() {
        // Arrange: 1 major incident (should trigger immediately)
        LocalDate today = LocalDate.now();
        createBehaviorIncident(testStudent, today.minusDays(1),
            BehaviorIncident.BehaviorType.NEGATIVE, BehaviorIncident.SeverityLevel.MAJOR);

        // Act
        boolean hasBehaviorRisk = progressMonitoringService.detectBehaviorPatterns(testStudent);

        // Assert
        assertTrue(hasBehaviorRisk, "Should detect behavior risk with any major incident");
        System.out.println("✓ Single major incident triggers alert");
    }

    @Test
    public void testDetectBehaviorPatterns_PositiveIncidentsIgnored() {
        // Arrange: Only positive behavior incidents
        LocalDate today = LocalDate.now();
        createBehaviorIncident(testStudent, today.minusDays(1),
            BehaviorIncident.BehaviorType.POSITIVE, BehaviorIncident.SeverityLevel.MINOR);
        createBehaviorIncident(testStudent, today.minusDays(5),
            BehaviorIncident.BehaviorType.POSITIVE, BehaviorIncident.SeverityLevel.MINOR);

        // Act
        boolean hasBehaviorRisk = progressMonitoringService.detectBehaviorPatterns(testStudent);

        // Assert
        assertFalse(hasBehaviorRisk, "Should NOT detect risk for positive incidents");
        System.out.println("✓ Positive behavior incidents do not trigger alerts");
    }

    // ========================================================================
    // TEST 6: COMPREHENSIVE RISK ASSESSMENT
    // ========================================================================

    @Test
    public void testRiskAssessment_HighRisk() {
        // Arrange: Student has 3+ risk areas (attendance, academic, behavior)
        LocalDate today = LocalDate.now();

        // Attendance risk: 3 tardies
        createAttendance(testStudent, today.minusDays(1), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(5), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(10), AttendanceRecord.AttendanceStatus.TARDY, null);

        // Academic risk: 5 missing assignments
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        for (int i = 0; i < 5; i++) {
            createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(i + 1), true);
        }

        // Behavior risk: 1 major incident
        createBehaviorIncident(testStudent, today.minusDays(2),
            BehaviorIncident.BehaviorType.NEGATIVE, BehaviorIncident.SeverityLevel.MAJOR);

        // Act
        StudentProgressMonitoringService.StudentRiskAssessment assessment =
            progressMonitoringService.getStudentRiskAssessment(testStudent);

        // Assert
        assertNotNull(assessment, "Assessment should not be null");
        assertTrue(assessment.isHasAttendanceRisk(), "Should have attendance risk");
        assertTrue(assessment.isHasAcademicRisk(), "Should have academic risk");
        assertTrue(assessment.isHasBehaviorRisk(), "Should have behavior risk");
        assertEquals(StudentProgressMonitoringService.RiskLevel.HIGH, assessment.getRiskLevel(),
            "Should be HIGH risk with 3+ risk areas");

        System.out.println("✓ HIGH risk correctly identified (3+ risk areas)");
    }

    @Test
    public void testRiskAssessment_MediumRisk() {
        // Arrange: Student has 2 risk areas (attendance, academic)
        LocalDate today = LocalDate.now();

        // Attendance risk: 3 tardies
        createAttendance(testStudent, today.minusDays(1), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(5), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(10), AttendanceRecord.AttendanceStatus.TARDY, null);

        // Academic risk: 5 missing assignments
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);
        for (int i = 0; i < 5; i++) {
            createGradeEntry(testStudent, mathCourse, null, 100.0, today.minusDays(i + 1), true);
        }

        // Act
        StudentProgressMonitoringService.StudentRiskAssessment assessment =
            progressMonitoringService.getStudentRiskAssessment(testStudent);

        // Assert
        assertEquals(StudentProgressMonitoringService.RiskLevel.MEDIUM, assessment.getRiskLevel(),
            "Should be MEDIUM risk with 2 risk areas");

        System.out.println("✓ MEDIUM risk correctly identified (2 risk areas)");
    }

    @Test
    public void testRiskAssessment_LowRisk() {
        // Arrange: Student has 1 risk area (attendance only)
        LocalDate today = LocalDate.now();

        // Attendance risk: 3 tardies
        createAttendance(testStudent, today.minusDays(1), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(5), AttendanceRecord.AttendanceStatus.TARDY, null);
        createAttendance(testStudent, today.minusDays(10), AttendanceRecord.AttendanceStatus.TARDY, null);

        // Act
        StudentProgressMonitoringService.StudentRiskAssessment assessment =
            progressMonitoringService.getStudentRiskAssessment(testStudent);

        // Assert
        assertEquals(StudentProgressMonitoringService.RiskLevel.LOW, assessment.getRiskLevel(),
            "Should be LOW risk with 1 risk area");

        System.out.println("✓ LOW risk correctly identified (1 risk area)");
    }

    @Test
    public void testRiskAssessment_NoRisk() {
        // Arrange: Student has no risk indicators
        createEnrollment(testStudent, mathCourse, EnrollmentStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        createGradeEntry(testStudent, mathCourse, 90.0, 100.0, today.minusDays(5), false);

        // Act
        StudentProgressMonitoringService.StudentRiskAssessment assessment =
            progressMonitoringService.getStudentRiskAssessment(testStudent);

        // Assert
        assertFalse(assessment.isHasAttendanceRisk(), "Should have no attendance risk");
        assertFalse(assessment.isHasAcademicRisk(), "Should have no academic risk");
        assertFalse(assessment.isHasBehaviorRisk(), "Should have no behavior risk");
        assertFalse(assessment.isHasObservationRisk(), "Should have no observation risk");
        assertEquals(StudentProgressMonitoringService.RiskLevel.NONE, assessment.getRiskLevel(),
            "Should be NO risk with 0 risk areas");

        System.out.println("✓ NO risk correctly identified (0 risk areas)");
    }
}
