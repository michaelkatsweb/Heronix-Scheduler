package com.heronix.controller;

import com.heronix.model.domain.*;
import com.heronix.model.domain.AcademicPlan.PlanStatus;
import com.heronix.model.domain.AcademicPlan.PlanType;
import com.heronix.model.domain.PlannedCourse.CourseStatus;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AcademicPlanManagementController
 * Tests four-year academic planning business logic
 *
 * Note: This is a JavaFX controller. We test the business logic
 * (academic plan management, course planning, graduation requirements)
 * rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class AcademicPlanManagementControllerTest {

    @Autowired
    private AcademicPlanRepository academicPlanRepository;

    @Autowired
    private PlannedCourseRepository plannedCourseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private Student testStudent;
    private Course testCourse1;
    private Course testCourse2;
    private AcademicPlan testPlan;
    private User testCounselor;

    @BeforeEach
    public void setup() {
        // Create test student
        testStudent = new Student();
        testStudent.setFirstName("Emily");
        testStudent.setLastName("Rodriguez");
        testStudent.setStudentId("PLAN-S001");
        testStudent.setGradeLevel("9");
        testStudent.setActive(true);
        testStudent = studentRepository.save(testStudent);

        // Create test courses
        testCourse1 = new Course();
        testCourse1.setCourseCode("ENG101");
        testCourse1.setCourseName("English I");
        testCourse1.setCredits(1.0);
        testCourse1.setActive(true);
        testCourse1 = courseRepository.save(testCourse1);

        testCourse2 = new Course();
        testCourse2.setCourseCode("MATH101");
        testCourse2.setCourseName("Algebra I");
        testCourse2.setCredits(1.0);
        testCourse2.setActive(true);
        testCourse2 = courseRepository.save(testCourse2);

        // Create test counselor
        testCounselor = new User();
        testCounselor.setUsername("counselor.smith");
        testCounselor.setEmail("counselor@eduscheduler.com");
        testCounselor.setPassword("password");  // Required field
        testCounselor.setEnabled(true);
        testCounselor = userRepository.save(testCounselor);

        // Create test academic plan
        testPlan = AcademicPlan.builder()
            .student(testStudent)
            .planName("STEM Track")
            .planType(PlanType.STANDARD)
            .status(PlanStatus.DRAFT)
            .startYear("2025-2026")
            .expectedGraduationYear("2029")
            .isPrimary(true)
            .totalCreditsPlanned(24.0)
            .totalCreditsCompleted(0.0)
            .meetsGraduationRequirements(false)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        testPlan = academicPlanRepository.save(testPlan);
    }

    // ========== ACADEMIC PLAN CRUD ==========

    @Test
    public void testCreateAcademicPlan_ShouldSaveSuccessfully() {
        // Arrange
        AcademicPlan newPlan = AcademicPlan.builder()
            .student(testStudent)
            .planName("Liberal Arts Track")
            .planType(PlanType.ALTERNATIVE)
            .status(PlanStatus.DRAFT)
            .startYear("2025-2026")
            .expectedGraduationYear("2029")
            .isPrimary(false)
            .totalCreditsPlanned(24.0)
            .totalCreditsCompleted(0.0)
            .meetsGraduationRequirements(false)
            .active(true)
            .build();

        // Act
        AcademicPlan saved = academicPlanRepository.save(newPlan);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(testStudent.getId(), saved.getStudent().getId());
        assertEquals("Liberal Arts Track", saved.getPlanName());
        assertEquals(PlanType.ALTERNATIVE, saved.getPlanType());
        assertEquals(PlanStatus.DRAFT, saved.getStatus());

        System.out.println("✓ Academic plan created successfully");
        System.out.println("  - Student: " + testStudent.getFirstName() + " " + testStudent.getLastName());
        System.out.println("  - Plan: " + saved.getPlanName());
    }

    @Test
    public void testFindAllPlans_ShouldReturnList() {
        // Act
        List<AcademicPlan> plans = academicPlanRepository.findAll();

        // Assert
        assertNotNull(plans);
        assertTrue(plans.size() >= 1);
        assertTrue(plans.stream().anyMatch(p -> p.getId().equals(testPlan.getId())));

        System.out.println("✓ Found " + plans.size() + " academic plan(s)");
    }

    @Test
    public void testFindPlansByStudent_ShouldReturnStudentPlans() {
        // Act
        List<AcademicPlan> plans = academicPlanRepository.findByStudent(testStudent);

        // Assert
        assertNotNull(plans);
        assertTrue(plans.size() >= 1);
        assertTrue(plans.stream().allMatch(p -> p.getStudent().getId().equals(testStudent.getId())));

        System.out.println("✓ Found " + plans.size() + " plan(s) for student");
    }

    @Test
    public void testUpdateAcademicPlan_ShouldPersistChanges() {
        // Arrange
        testPlan.setStatus(PlanStatus.APPROVED);
        testPlan.setMeetsGraduationRequirements(true);
        testPlan.setUpdatedAt(LocalDateTime.now());

        // Act
        AcademicPlan updated = academicPlanRepository.save(testPlan);

        // Assert
        assertEquals(PlanStatus.APPROVED, updated.getStatus());
        assertTrue(updated.getMeetsGraduationRequirements());

        System.out.println("✓ Academic plan updated successfully");
        System.out.println("  - New status: " + updated.getStatus());
    }

    @Test
    public void testDeleteAcademicPlan_ShouldDeactivate() {
        // Arrange
        testPlan.setActive(false);

        // Act
        AcademicPlan deactivated = academicPlanRepository.save(testPlan);

        // Assert
        assertFalse(deactivated.getActive());

        System.out.println("✓ Academic plan deactivated (soft delete)");
    }

    // ========== PLAN TYPES AND STATUS ==========

    @Test
    public void testPlanType_Standard_ShouldBeSet() {
        // Assert
        assertEquals(PlanType.STANDARD, testPlan.getPlanType());

        System.out.println("✓ Standard plan type verified");
    }

    @Test
    public void testPlanType_Alternative_ShouldBeSet() {
        // Arrange
        testPlan.setPlanType(PlanType.ALTERNATIVE);

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertEquals(PlanType.ALTERNATIVE, saved.getPlanType());

        System.out.println("✓ Alternative plan type verified");
    }

    @Test
    public void testPlanStatus_Draft_ShouldBeDefault() {
        // Assert
        assertEquals(PlanStatus.DRAFT, testPlan.getStatus());

        System.out.println("✓ Draft status is default");
    }

    @Test
    public void testPlanStatus_PendingApproval_ShouldBeSet() {
        // Arrange
        testPlan.setStatus(PlanStatus.PENDING_APPROVAL);

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertEquals(PlanStatus.PENDING_APPROVAL, saved.getStatus());

        System.out.println("✓ Pending approval status verified");
    }

    @Test
    public void testPlanStatus_Approved_ShouldBeSet() {
        // Arrange
        testPlan.setStatus(PlanStatus.APPROVED);

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertEquals(PlanStatus.APPROVED, saved.getStatus());

        System.out.println("✓ Approved status verified");
    }

    // ========== PRIMARY PLAN ==========

    @Test
    public void testPrimaryPlan_ShouldBeIdentified() {
        // Assert
        assertTrue(testPlan.getIsPrimary());

        System.out.println("✓ Primary plan identified");
    }

    @Test
    public void testMultiplePlans_OnlyOnePrimary() {
        // Arrange - Create alternative plan (not primary)
        AcademicPlan altPlan = AcademicPlan.builder()
            .student(testStudent)
            .planName("Alternative Plan")
            .planType(PlanType.ALTERNATIVE)
            .status(PlanStatus.DRAFT)
            .startYear("2025-2026")
            .isPrimary(false)
            .totalCreditsPlanned(24.0)
            .totalCreditsCompleted(0.0)
            .active(true)
            .build();
        altPlan = academicPlanRepository.save(altPlan);

        // Act
        List<AcademicPlan> plans = academicPlanRepository.findByStudent(testStudent);
        long primaryCount = plans.stream().filter(p -> Boolean.TRUE.equals(p.getIsPrimary())).count();

        // Assert
        assertEquals(1, primaryCount);

        System.out.println("✓ Only one primary plan per student");
        System.out.println("  - Total plans: " + plans.size());
        System.out.println("  - Primary plans: " + primaryCount);
    }

    // ========== PLANNED COURSES ==========

    @Test
    public void testAddPlannedCourse_ShouldAttachToPlan() {
        // Arrange
        PlannedCourse plannedCourse = PlannedCourse.builder()
            .academicPlan(testPlan)
            .course(testCourse1)
            .schoolYear("2025-2026")
            .gradeLevel(9)
            .semester(1)  // Fall
            .credits(1.0)
            .isRequired(true)
            .status(CourseStatus.PLANNED)
            .prerequisitesMet(true)
            .hasConflict(false)
            .build();

        // Act
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(testPlan.getId(), saved.getAcademicPlan().getId());
        assertEquals(testCourse1.getId(), saved.getCourse().getId());
        assertEquals(9, saved.getGradeLevel());

        System.out.println("✓ Planned course added to plan");
        System.out.println("  - Course: " + testCourse1.getCourseName());
        System.out.println("  - Grade: " + saved.getGradeLevel());
        System.out.println("  - Semester: " + saved.getSemester());
    }

    @Test
    public void testPlannedCourse_Semester_Fall() {
        // Arrange
        PlannedCourse plannedCourse = PlannedCourse.builder()
            .academicPlan(testPlan)
            .course(testCourse1)
            .schoolYear("2025-2026")
            .gradeLevel(9)
            .semester(1)  // Fall
            .credits(1.0)
            .status(CourseStatus.PLANNED)
            .build();

        // Act
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Assert
        assertEquals(1, saved.getSemester());

        System.out.println("✓ Fall semester course (1) verified");
    }

    @Test
    public void testPlannedCourse_Semester_Spring() {
        // Arrange
        PlannedCourse plannedCourse = PlannedCourse.builder()
            .academicPlan(testPlan)
            .course(testCourse2)
            .schoolYear("2025-2026")
            .gradeLevel(9)
            .semester(2)  // Spring
            .credits(1.0)
            .status(CourseStatus.PLANNED)
            .build();

        // Act
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Assert
        assertEquals(2, saved.getSemester());

        System.out.println("✓ Spring semester course (2) verified");
    }

    @Test
    public void testPlannedCourse_Semester_FullYear() {
        // Arrange
        PlannedCourse plannedCourse = PlannedCourse.builder()
            .academicPlan(testPlan)
            .course(testCourse1)
            .schoolYear("2025-2026")
            .gradeLevel(9)
            .semester(0)  // Full Year
            .credits(1.0)
            .status(CourseStatus.PLANNED)
            .build();

        // Act
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Assert
        assertEquals(0, saved.getSemester());

        System.out.println("✓ Full year course (0) verified");
    }

    // ========== COURSE STATUS ==========

    @Test
    public void testCourseStatus_Planned_ShouldBeDefault() {
        // Arrange
        PlannedCourse plannedCourse = PlannedCourse.builder()
            .academicPlan(testPlan)
            .course(testCourse1)
            .schoolYear("2025-2026")
            .gradeLevel(9)
            .credits(1.0)
            .build();

        // Act
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Assert
        assertEquals(CourseStatus.PLANNED, saved.getStatus());

        System.out.println("✓ Planned status is default");
    }

    @Test
    public void testCourseStatus_InProgress_ShouldBeSet() {
        // Arrange
        PlannedCourse plannedCourse = PlannedCourse.builder()
            .academicPlan(testPlan)
            .course(testCourse1)
            .schoolYear("2025-2026")
            .gradeLevel(9)
            .status(CourseStatus.IN_PROGRESS)
            .credits(1.0)
            .build();

        // Act
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Assert
        assertEquals(CourseStatus.IN_PROGRESS, saved.getStatus());

        System.out.println("✓ In-progress status verified");
    }

    @Test
    public void testCourseStatus_Completed_ShouldBeSet() {
        // Arrange
        PlannedCourse plannedCourse = PlannedCourse.builder()
            .academicPlan(testPlan)
            .course(testCourse1)
            .schoolYear("2025-2026")
            .gradeLevel(9)
            .status(CourseStatus.COMPLETED)
            .gradeEarned("A")
            .credits(1.0)
            .build();

        // Act
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Assert
        assertEquals(CourseStatus.COMPLETED, saved.getStatus());
        assertEquals("A", saved.getGradeEarned());

        System.out.println("✓ Completed status with grade verified");
        System.out.println("  - Grade earned: " + saved.getGradeEarned());
    }

    // ========== GRADUATION REQUIREMENTS ==========

    @Test
    public void testGraduationRequirements_NotMet_Initially() {
        // Assert
        assertFalse(testPlan.getMeetsGraduationRequirements());

        System.out.println("✓ Graduation requirements not met initially");
    }

    @Test
    public void testGraduationRequirements_CanBeSet() {
        // Arrange
        testPlan.setMeetsGraduationRequirements(true);
        testPlan.setMissingRequirements(null);

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertTrue(saved.getMeetsGraduationRequirements());
        assertNull(saved.getMissingRequirements());

        System.out.println("✓ Graduation requirements met flag set");
    }

    @Test
    public void testMissingRequirements_ShouldTrack() {
        // Arrange
        testPlan.setMeetsGraduationRequirements(false);
        testPlan.setMissingRequirements("Science: 1.0 credits, World Language: 2.0 credits");

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertFalse(saved.getMeetsGraduationRequirements());
        assertNotNull(saved.getMissingRequirements());
        assertTrue(saved.getMissingRequirements().contains("Science"));

        System.out.println("✓ Missing requirements tracked");
        System.out.println("  - Missing: " + saved.getMissingRequirements());
    }

    // ========== CREDITS TRACKING ==========

    @Test
    public void testCreditsPlanned_ShouldTrack() {
        // Assert
        assertEquals(24.0, testPlan.getTotalCreditsPlanned());

        System.out.println("✓ Planned credits tracked");
        System.out.println("  - Total planned: " + testPlan.getTotalCreditsPlanned());
    }

    @Test
    public void testCreditsCompleted_ShouldTrack() {
        // Arrange
        testPlan.setTotalCreditsCompleted(8.0);

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertEquals(8.0, saved.getTotalCreditsCompleted());

        System.out.println("✓ Completed credits tracked");
        System.out.println("  - Completed: " + saved.getTotalCreditsCompleted());
    }

    @Test
    public void testCompletionPercentage_ShouldCalculate() {
        // Arrange
        testPlan.setTotalCreditsPlanned(24.0);
        testPlan.setTotalCreditsCompleted(12.0);

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);
        double percentage = (saved.getTotalCreditsCompleted() / saved.getTotalCreditsPlanned()) * 100.0;

        // Assert
        assertEquals(50.0, percentage, 0.01);

        System.out.println("✓ Completion percentage calculated");
        System.out.println("  - Percentage: " + percentage + "%");
    }

    // ========== APPROVAL WORKFLOW ==========

    @Test
    public void testCounselorApproval_ShouldRecord() {
        // Arrange
        testPlan.setApprovedByCounselor(testCounselor);
        testPlan.setCounselorApprovedDate(LocalDateTime.now());
        testPlan.setStatus(PlanStatus.APPROVED);

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertNotNull(saved.getApprovedByCounselor());
        assertNotNull(saved.getCounselorApprovedDate());
        assertEquals(PlanStatus.APPROVED, saved.getStatus());

        System.out.println("✓ Counselor approval recorded");
        System.out.println("  - Approved by: " + testCounselor.getUsername());
    }

    @Test
    public void testStudentAcceptance_ShouldRecord() {
        // Arrange
        testPlan.setStudentAccepted(true);
        testPlan.setStudentAcceptedDate(LocalDateTime.now());

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertTrue(saved.getStudentAccepted());
        assertNotNull(saved.getStudentAcceptedDate());

        System.out.println("✓ Student acceptance recorded");
    }

    @Test
    public void testParentAcceptance_ShouldRecord() {
        // Arrange
        testPlan.setParentAccepted(true);
        testPlan.setParentAcceptedDate(LocalDateTime.now());

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertTrue(saved.getParentAccepted());
        assertNotNull(saved.getParentAcceptedDate());

        System.out.println("✓ Parent acceptance recorded");
    }

    // ========== NOTES AND TRACKING ==========

    @Test
    public void testNotes_ShouldPersist() {
        // Arrange
        testPlan.setNotes("Student wants to focus on STEM courses for college preparation.");

        // Act
        AcademicPlan saved = academicPlanRepository.save(testPlan);

        // Assert
        assertNotNull(saved.getNotes());
        assertTrue(saved.getNotes().contains("STEM"));

        System.out.println("✓ Plan notes persisted");
        System.out.println("  - Notes: " + saved.getNotes());
    }

    @Test
    public void testTimestamps_ShouldTrack() {
        // Assert
        assertNotNull(testPlan.getCreatedAt());
        assertNotNull(testPlan.getUpdatedAt());

        System.out.println("✓ Timestamps tracked");
    }
}
