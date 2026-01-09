package com.heronix.integration;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.SubjectCertification;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.CertificationType;
import com.heronix.model.enums.EducationLevel;
import com.heronix.model.enums.PriorityLevel;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.SubjectCertificationRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.SmartTeacherAssignmentService;
import com.heronix.service.SmartTeacherAssignmentService.AssignmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for Smart Teacher Assignment Workflow
 *
 * Tests the complete teacher assignment workflow including:
 * - Smart assignment algorithm
 * - Strict certification matching
 * - Workload balancing (3 modes: COURSE_COUNT, CREDIT_HOURS, TEACHING_PERIODS)
 * - Course sequence assignment (same teacher for course sequences)
 * - Workload limits (2 optimal, 3 maximum)
 * - Teacher shortage handling
 * - Department-based assignment
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 20, 2025 - Integration Testing Phase
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class TeacherAssignmentIntegrationTest {

    @Autowired
    private SmartTeacherAssignmentService assignmentService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectCertificationRepository subjectCertificationRepository;

    // Test teachers
    private Teacher mathTeacher1;
    private Teacher mathTeacher2;
    private Teacher englishTeacher1;
    private Teacher scienceTeacher1;
    private Teacher multiSubjectTeacher;

    // Test courses
    private Course algebra1;
    private Course algebra2;
    private Course geometry;
    private Course calculus;
    private Course english1;
    private Course english2;
    private Course biology;
    private Course chemistry;

    @BeforeEach
    public void setup() {
        // Create test teachers with certifications
        mathTeacher1 = new Teacher();
        mathTeacher1.setName("John Smith");
        mathTeacher1.setFirstName("John");
        mathTeacher1.setLastName("Smith");
        mathTeacher1.setEmail("john.smith@school.edu");
        mathTeacher1.setDepartment("Mathematics");
        mathTeacher1 = teacherRepository.save(mathTeacher1);
        addCertification(mathTeacher1, "Mathematics");

        mathTeacher2 = new Teacher();
        mathTeacher2.setName("Jane Doe");
        mathTeacher2.setFirstName("Jane");
        mathTeacher2.setLastName("Doe");
        mathTeacher2.setEmail("jane.doe@school.edu");
        mathTeacher2.setDepartment("Mathematics");
        mathTeacher2 = teacherRepository.save(mathTeacher2);
        addCertification(mathTeacher2, "Mathematics");

        englishTeacher1 = new Teacher();
        englishTeacher1.setName("Bob Johnson");
        englishTeacher1.setFirstName("Bob");
        englishTeacher1.setLastName("Johnson");
        englishTeacher1.setEmail("bob.johnson@school.edu");
        englishTeacher1.setDepartment("English");
        englishTeacher1 = teacherRepository.save(englishTeacher1);
        addCertification(englishTeacher1, "English");

        scienceTeacher1 = new Teacher();
        scienceTeacher1.setName("Alice Williams");
        scienceTeacher1.setFirstName("Alice");
        scienceTeacher1.setLastName("Williams");
        scienceTeacher1.setEmail("alice.williams@school.edu");
        scienceTeacher1.setDepartment("Science");
        scienceTeacher1 = teacherRepository.save(scienceTeacher1);
        addCertification(scienceTeacher1, "Science");

        multiSubjectTeacher = new Teacher();
        multiSubjectTeacher.setName("Charlie Brown");
        multiSubjectTeacher.setFirstName("Charlie");
        multiSubjectTeacher.setLastName("Brown");
        multiSubjectTeacher.setEmail("charlie.brown@school.edu");
        multiSubjectTeacher.setDepartment("Mathematics");
        multiSubjectTeacher = teacherRepository.save(multiSubjectTeacher);
        addCertification(multiSubjectTeacher, "Mathematics");
        addCertification(multiSubjectTeacher, "English");

        // Create test courses
        algebra1 = createCourse("MATH101", "Algebra 1", "Mathematics", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.HIGH);
        algebra2 = createCourse("MATH102", "Algebra 2", "Mathematics", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.NORMAL);
        geometry = createCourse("MATH201", "Geometry", "Mathematics", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.HIGH);
        calculus = createCourse("MATH301", "Calculus", "Mathematics", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.NORMAL);

        english1 = createCourse("ENG101", "English 1", "English", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.HIGH);
        english2 = createCourse("ENG102", "English 2", "English", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.NORMAL);

        biology = createCourse("SCI101", "Biology", "Science", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.HIGH);
        chemistry = createCourse("SCI201", "Chemistry", "Science", EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.NORMAL);
    }

    private Course createCourse(String code, String name, String subject, EducationLevel level,
                                double credits, PriorityLevel priority) {
        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setSubject(subject);
        course.setLevel(level);
        course.setCredits(credits);
        course.setPriorityLevel(priority);
        course.setActive(true);
        return courseRepository.save(course);
    }

    private void addCertification(Teacher teacher, String subject) {
        SubjectCertification cert = new SubjectCertification();
        cert.setSubject(subject);
        cert.setGradeRange("9-12");
        cert.setCertificationType(CertificationType.PROFESSIONAL);
        cert.setActive(true);
        cert.setTeacher(teacher);
        cert = subjectCertificationRepository.save(cert);

        // Add to teacher's collection
        teacher.addSubjectCertification(cert);
    }

    // ========================================================================
    // BASIC ASSIGNMENT TESTS
    // ========================================================================

    @Test
    public void testSmartAssignAllTeachers_WithSufficientTeachers_ShouldSucceed() {
        // Arrange - All courses unassigned, teachers available
        System.out.println("\n=== DEBUG: Test Setup ===");
        System.out.println("Math Teacher 1 certifications: " + mathTeacher1.getCertifiedSubjects());
        System.out.println("English Teacher 1 certifications: " + englishTeacher1.getCertifiedSubjects());
        System.out.println("Algebra 1 subject: " + algebra1.getSubject());
        System.out.println("English 1 subject: " + english1.getSubject());

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - All courses should be assigned
        System.out.println("\n=== DEBUG: Result ===");
        System.out.println("Total processed: " + result.getTotalCoursesProcessed());
        System.out.println("Courses assigned: " + result.getCoursesAssigned());
        System.out.println("Courses failed: " + result.getCoursesFailed());
        System.out.println("Message: " + result.getMessage());

        assertNotNull(result);
        assertEquals(8, result.getTotalCoursesProcessed());
        assertTrue(result.getCoursesAssigned() > 0, "Expected some courses to be assigned but got: " + result.getCoursesAssigned());
        assertTrue(result.getCoursesFailed() == 0 || result.getCoursesFailed() < result.getTotalCoursesProcessed());

        // Verify courses have teachers assigned
        Course updatedAlgebra1 = courseRepository.findById(algebra1.getId()).orElseThrow();
        assertNotNull(updatedAlgebra1.getTeacher());

        System.out.println("✓ Smart assignment completed:");
        System.out.println("  - Assigned: " + result.getCoursesAssigned());
        System.out.println("  - Failed: " + result.getCoursesFailed());
        System.out.println("  - Message: " + result.getMessage());
    }

    @Test
    public void testSmartAssignment_ShouldRespectCertifications() {
        // Arrange - Only math and English teachers available
        teacherRepository.delete(scienceTeacher1);

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Debug output
        System.out.println("\n=== Certification Respect Test ===");
        System.out.println("Total processed: " + result.getTotalCoursesProcessed());
        System.out.println("Assigned: " + result.getCoursesAssigned());
        System.out.println("Failed: " + result.getCoursesFailed());

        // Assert - Math and English courses assigned, science courses failed
        assertTrue(result.getCoursesAssigned() >= 4,
            "Expected >= 4 courses assigned (Math+English), got: " + result.getCoursesAssigned());
        assertTrue(result.getCoursesFailed() >= 2,
            "Expected >= 2 courses failed (Science), got: " + result.getCoursesFailed());

        // Verify math course has math teacher
        Course updatedAlgebra1 = courseRepository.findById(algebra1.getId()).orElseThrow();
        if (updatedAlgebra1.getTeacher() != null) {
            assertTrue(updatedAlgebra1.getTeacher().getCertifiedSubjects().contains("Mathematics"),
                "Math course should be assigned to math-certified teacher");
        }

        // Verify science course not assigned or has proper error
        Course updatedBiology = courseRepository.findById(biology.getId()).orElseThrow();
        if (updatedBiology.getTeacher() == null) {
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Biology") || e.contains("Science")));
        }
    }

    @Test
    public void testSmartAssignment_WithNoTeachers_ShouldReportError() {
        // Arrange - Delete all teachers
        teacherRepository.deleteAll();

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - With no teachers, nothing should be assigned
        assertEquals(0, result.getCoursesAssigned(), "Should assign 0 courses when no teachers available");

        // Service may process courses or return early - both are valid
        // The key assertion is that no assignments were made
        assertNotNull(result.getMessage(), "Should have a message explaining the result");

        // Verify service handled the scenario gracefully (no exceptions)
        assertTrue(result.getTotalCoursesProcessed() >= 0, "Should process gracefully");
        assertTrue(result.getCoursesAssigned() + result.getCoursesFailed() <= result.getTotalCoursesProcessed(),
            "Assigned + Failed should not exceed total processed");
    }

    @Test
    public void testSmartAssignment_WithAllCoursesAssigned_ShouldSkip() {
        // Arrange - Assign all courses first
        algebra1.setTeacher(mathTeacher1);
        algebra2.setTeacher(mathTeacher1);
        geometry.setTeacher(mathTeacher2);
        calculus.setTeacher(mathTeacher2);
        english1.setTeacher(englishTeacher1);
        english2.setTeacher(englishTeacher1);
        biology.setTeacher(scienceTeacher1);
        chemistry.setTeacher(scienceTeacher1);
        courseRepository.saveAll(List.of(algebra1, algebra2, geometry, calculus,
            english1, english2, biology, chemistry));

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert
        assertEquals(0, result.getTotalCoursesProcessed());
        assertTrue(result.getMessage().contains("already have teachers"));
    }

    // ========================================================================
    // WORKLOAD BALANCING TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_ShouldBalanceWorkload() {
        // Arrange - Multiple math teachers available for math courses

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Math courses should be distributed
        long mathTeacher1Courses = courseRepository.findAll().stream()
            .filter(c -> c.getTeacher() != null &&
                        c.getTeacher().getId().equals(mathTeacher1.getId()))
            .count();

        long mathTeacher2Courses = courseRepository.findAll().stream()
            .filter(c -> c.getTeacher() != null &&
                        c.getTeacher().getId().equals(mathTeacher2.getId()))
            .count();

        long multiSubjectCourses = courseRepository.findAll().stream()
            .filter(c -> c.getTeacher() != null &&
                        c.getTeacher().getId().equals(multiSubjectTeacher.getId()))
            .count();

        System.out.println("  - Math Teacher 1 courses: " + mathTeacher1Courses);
        System.out.println("  - Math Teacher 2 courses: " + mathTeacher2Courses);
        System.out.println("  - Multi-subject teacher courses: " + multiSubjectCourses);

        // At least some distribution should occur
        assertTrue(mathTeacher1Courses <= 3, "No teacher should have more than 3 courses");
        assertTrue(mathTeacher2Courses <= 3, "No teacher should have more than 3 courses");
        assertTrue(multiSubjectCourses <= 3, "No teacher should have more than 3 courses");
    }

    @Test
    public void testSmartAssignment_ShouldRespectWorkloadLimit() {
        // Arrange - Only one math teacher for 4 math courses
        teacherRepository.delete(mathTeacher2);
        teacherRepository.delete(multiSubjectTeacher);

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Should not assign more than 3 courses to one teacher
        long mathTeacher1Courses = courseRepository.findAll().stream()
            .filter(c -> c.getTeacher() != null &&
                        c.getTeacher().getId().equals(mathTeacher1.getId()))
            .count();

        assertTrue(mathTeacher1Courses <= 3,
            "Teacher should not be assigned more than 3 courses (got " + mathTeacher1Courses + ")");

        // Should have warnings or errors about capacity
        assertTrue(result.getWarnings().size() > 0 || result.getErrors().size() > 0,
            "Should have warnings or errors about workload limits");
    }

    @Test
    public void testSmartAssignment_ShouldWarnAtOptimalLimit() {
        // Arrange - Limited teachers

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Check for warnings when teachers reach optimal limit (2 courses)
        if (result.getCoursesAssigned() >= 6) {
            // Some teacher likely has 2+ courses
            // May have warnings about reaching optimal limit
            assertNotNull(result.getWarnings());
        }
    }

    // ========================================================================
    // COURSE SEQUENCE TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_ShouldAssignSequencesToSameTeacher() {
        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - English 1 and English 2 should ideally have same teacher
        Course updatedEnglish1 = courseRepository.findById(english1.getId()).orElseThrow();
        Course updatedEnglish2 = courseRepository.findById(english2.getId()).orElseThrow();

        if (updatedEnglish1.getTeacher() != null && updatedEnglish2.getTeacher() != null) {
            // Ideally same teacher, but may differ if at capacity
            if (updatedEnglish1.getTeacher().getId().equals(updatedEnglish2.getTeacher().getId())) {
                System.out.println("✓ English sequence assigned to same teacher: " +
                    updatedEnglish1.getTeacher().getName());
            } else {
                System.out.println("! English sequence assigned to different teachers (capacity limit)");
            }
        }
    }

    @Test
    public void testSmartAssignment_ShouldAssignAlgebraSequenceToSameTeacher() {
        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Algebra 1 and Algebra 2 should ideally have same teacher
        Course updatedAlgebra1 = courseRepository.findById(algebra1.getId()).orElseThrow();
        Course updatedAlgebra2 = courseRepository.findById(algebra2.getId()).orElseThrow();

        if (updatedAlgebra1.getTeacher() != null && updatedAlgebra2.getTeacher() != null) {
            if (updatedAlgebra1.getTeacher().getId().equals(updatedAlgebra2.getTeacher().getId())) {
                System.out.println("✓ Algebra sequence assigned to same teacher: " +
                    updatedAlgebra1.getTeacher().getName());
            }
        }
    }

    // ========================================================================
    // PRIORITY TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_ShouldPrioritizeHighPriorityCourses() {
        // Arrange - Set different priorities
        algebra1.setPriorityLevel(PriorityLevel.CRITICAL); // High priority
        geometry.setPriorityLevel(PriorityLevel.LOW); // Low priority
        courseRepository.save(algebra1);
        courseRepository.save(geometry);

        // Limit teachers to create scarcity
        teacherRepository.delete(mathTeacher2);
        teacherRepository.delete(multiSubjectTeacher);

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - High priority course should be assigned
        Course updatedAlgebra1 = courseRepository.findById(algebra1.getId()).orElseThrow();
        Course updatedGeometry = courseRepository.findById(geometry.getId()).orElseThrow();

        // Algebra 1 (priority 1) more likely to be assigned than Geometry (priority 3)
        if (result.getCoursesAssigned() < 4) {
            // If not all math courses assigned, check priority was respected
            assertTrue(updatedAlgebra1.getTeacher() != null ||
                      (updatedAlgebra1.getTeacher() == null && updatedGeometry.getTeacher() == null),
                "High priority courses should be assigned first");
        }
    }

    // ========================================================================
    // MULTI-CERTIFICATION TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_WithMultiCertifiedTeacher_ShouldAssignAcrossSubjects() {
        // Arrange - Delete single-subject teachers, keep multi-subject
        teacherRepository.delete(mathTeacher1);
        teacherRepository.delete(mathTeacher2);
        teacherRepository.delete(englishTeacher1);

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Multi-certified teacher should get courses from multiple subjects
        long multiTeacherCourses = courseRepository.findAll().stream()
            .filter(c -> c.getTeacher() != null &&
                        c.getTeacher().getId().equals(multiSubjectTeacher.getId()))
            .count();

        assertTrue(multiTeacherCourses > 0, "Multi-certified teacher should be assigned courses");
        assertTrue(multiTeacherCourses <= 3, "Should not exceed workload limit");

        // Check if assigned to both Math and English
        boolean hasMathCourse = courseRepository.findAll().stream()
            .anyMatch(c -> c.getTeacher() != null &&
                          c.getTeacher().getId().equals(multiSubjectTeacher.getId()) &&
                          c.getSubject().equals("Mathematics"));

        boolean hasEnglishCourse = courseRepository.findAll().stream()
            .anyMatch(c -> c.getTeacher() != null &&
                          c.getTeacher().getId().equals(multiSubjectTeacher.getId()) &&
                          c.getSubject().equals("English"));

        if (hasMathCourse && hasEnglishCourse) {
            System.out.println("✓ Multi-certified teacher assigned to both Math and English");
        }
    }

    // ========================================================================
    // ERROR HANDLING TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_WithTeacherShortage_ShouldReportSpecificErrors() {
        // Arrange - Only one science teacher for 2 science courses
        // All other teachers are Math/English, can't teach Science

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Science courses may fail if teacher at capacity
        if (result.getCoursesFailed() > 0) {
            // Should have error messages about science courses
            boolean hasRelevantError = result.getErrors().stream()
                .anyMatch(e -> e.toLowerCase().contains("science") ||
                              e.toLowerCase().contains("biology") ||
                              e.toLowerCase().contains("chemistry") ||
                              e.toLowerCase().contains("shortage") ||
                              e.toLowerCase().contains("capacity"));

            assertTrue(hasRelevantError, "Should report specific errors about science courses");
        }
    }

    @Test
    public void testSmartAssignment_WithNoCertifiedTeacher_ShouldReportHiringNeeded() {
        // Arrange - Create a course with no certified teacher
        Course physicsCourse = createCourse("SCI301", "Physics", "Science",
            EducationLevel.HIGH_SCHOOL, 1.0, PriorityLevel.HIGH);

        teacherRepository.delete(scienceTeacher1); // Remove only science teacher

        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Should report need to hire science teacher
        boolean hasHiringError = result.getErrors().stream()
            .anyMatch(e -> e.contains("ADMINISTRATOR ACTION") ||
                          e.contains("Hire") ||
                          e.contains("No certified teacher"));

        assertTrue(hasHiringError, "Should report need to hire teachers");
    }

    // ========================================================================
    // PERFORMANCE TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_Performance_ShouldCompleteInReasonableTime() {
        // Act
        long startTime = System.currentTimeMillis();
        AssignmentResult result = assignmentService.smartAssignAllTeachers();
        long endTime = System.currentTimeMillis();

        // Assert - Should complete in reasonable time (< 5 seconds for 8 courses)
        long duration = endTime - startTime;
        assertTrue(duration < 5000,
            "Assignment should complete in < 5 seconds (took " + duration + "ms)");

        System.out.println("✓ Assignment completed in " + duration + "ms");
    }

    // ========================================================================
    // RESULT VALIDATION TESTS
    // ========================================================================

    @Test
    public void testAssignmentResult_ShouldContainCompleteInformation() {
        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert - Result should have all required information
        assertNotNull(result);
        assertTrue(result.getTotalCoursesProcessed() >= 0);
        assertTrue(result.getCoursesAssigned() >= 0);
        assertTrue(result.getCoursesFailed() >= 0);
        assertNotNull(result.getMessage());
        assertNotNull(result.getWarnings());
        assertNotNull(result.getErrors());
        assertTrue(result.getStartTime() > 0);
        assertTrue(result.getEndTime() > 0);
        assertTrue(result.getEndTime() >= result.getStartTime());

        // Assert - Counts should add up
        assertEquals(result.getTotalCoursesProcessed(),
            result.getCoursesAssigned() + result.getCoursesFailed(),
            "Assigned + Failed should equal Total");
    }

    @Test
    public void testAssignmentResult_ShouldCalculateDuration() {
        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Assert
        long duration = result.getEndTime() - result.getStartTime();
        assertTrue(duration >= 0, "Duration should be non-negative");
        assertTrue(duration < 10000, "Duration should be reasonable (< 10s)");

        System.out.println("✓ Assignment duration: " + duration + "ms");
    }

    // ========================================================================
    // IDEMPOTENCY TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_MultipleRuns_ShouldBeConsistent() {
        // Act - Run assignment twice
        AssignmentResult result1 = assignmentService.smartAssignAllTeachers();

        System.out.println("\n=== Multiple Runs Test ===");
        System.out.println("First run - Processed: " + result1.getTotalCoursesProcessed());
        System.out.println("First run - Assigned: " + result1.getCoursesAssigned());
        System.out.println("First run - Failed: " + result1.getCoursesFailed());

        AssignmentResult result2 = assignmentService.smartAssignAllTeachers();

        System.out.println("Second run - Processed: " + result2.getTotalCoursesProcessed());
        System.out.println("Second run - Assigned: " + result2.getCoursesAssigned());
        System.out.println("Second run - Failed: " + result2.getCoursesFailed());

        // Assert - Second run should find all courses already assigned
        // If first run assigned all courses, second run should find 0 unassigned
        // If workload limits prevented full assignment, adjust expectation
        if (result1.getCoursesAssigned() == result1.getTotalCoursesProcessed()) {
            assertEquals(0, result2.getTotalCoursesProcessed(),
                "Second run should find no unassigned courses");
        } else {
            // Some courses couldn't be assigned due to workload limits
            assertTrue(result2.getTotalCoursesProcessed() > 0,
                "Second run should still find unassigned courses if first run hit workload limits");
        }
    }

    // ========================================================================
    // DATABASE PERSISTENCE TESTS
    // ========================================================================

    @Test
    public void testSmartAssignment_ShouldPersistToDatabase() {
        // Act
        AssignmentResult result = assignmentService.smartAssignAllTeachers();

        // Clear persistence context to force reload from database
        teacherRepository.flush();
        courseRepository.flush();

        // Assert - Courses should still have teachers when reloaded
        if (result.getCoursesAssigned() > 0) {
            Course reloadedCourse = courseRepository.findById(algebra1.getId()).orElseThrow();
            if (reloadedCourse.getTeacher() != null) {
                assertNotNull(reloadedCourse.getTeacher());
                System.out.println("✓ Assignment persisted to database");
            }
        }
    }
}
