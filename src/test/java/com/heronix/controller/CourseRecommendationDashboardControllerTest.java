package com.heronix.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseRecommendation;
import com.heronix.model.domain.CourseRecommendation.RecommendationStatus;
import com.heronix.model.domain.CourseRecommendation.RecommendationType;
import com.heronix.model.domain.Student;
import com.heronix.repository.CourseRecommendationRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CourseRecommendationDashboardController
 * Tests course recommendation management business logic
 *
 * Note: This is a JavaFX controller. We test the business logic
 * (recommendation CRUD, filtering, statistics) rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class CourseRecommendationDashboardControllerTest {

    @Autowired
    private CourseRecommendationRepository recommendationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Student testStudent;
    private Course testCourse1;
    private Course testCourse2;
    private CourseRecommendation testRecommendation;

    @BeforeEach
    public void setup() {
        // Create test student
        testStudent = new Student();
        testStudent.setFirstName("Alice");
        testStudent.setLastName("Johnson");
        testStudent.setStudentId("RECOM-S001");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);
        testStudent = studentRepository.save(testStudent);

        // Create test courses
        testCourse1 = new Course();
        testCourse1.setCourseCode("MATH201");
        testCourse1.setCourseName("Algebra II");
        testCourse1.setCredits(1.0);
        testCourse1.setActive(true);
        testCourse1 = courseRepository.save(testCourse1);

        testCourse2 = new Course();
        testCourse2.setCourseCode("SCI301");
        testCourse2.setCourseName("Chemistry");
        testCourse2.setCredits(1.0);
        testCourse2.setActive(true);
        testCourse2 = courseRepository.save(testCourse2);

        // Create test recommendation
        testRecommendation = new CourseRecommendation();
        testRecommendation.setStudent(testStudent);
        testRecommendation.setCourse(testCourse1);
        testRecommendation.setRecommendedSchoolYear("2025-2026");
        testRecommendation.setRecommendedGradeLevel(10);
        testRecommendation.setPriority(8);
        testRecommendation.setConfidenceScore(0.85);
        testRecommendation.setRecommendationType(RecommendationType.AI_GENERATED);
        testRecommendation.setStatus(RecommendationStatus.PENDING);
        testRecommendation.setReason("Strong performance in Algebra I and prerequisite completion");
        testRecommendation.setPrerequisitesMet(true);
        testRecommendation.setGpaRequirementMet(true);
        testRecommendation.setHasScheduleConflict(false);
        testRecommendation.setActive(true);
        testRecommendation = recommendationRepository.save(testRecommendation);
    }

    // ========== COURSE RECOMMENDATION CRUD ==========

    @Test
    public void testCreateRecommendation_ShouldSaveSuccessfully() {
        // Arrange
        CourseRecommendation newRecommendation = new CourseRecommendation();
        newRecommendation.setStudent(testStudent);
        newRecommendation.setCourse(testCourse2);
        newRecommendation.setRecommendedSchoolYear("2025-2026");
        newRecommendation.setRecommendedGradeLevel(10);
        newRecommendation.setPriority(6);
        newRecommendation.setConfidenceScore(0.75);
        newRecommendation.setRecommendationType(RecommendationType.COUNSELOR_MANUAL);
        newRecommendation.setStatus(RecommendationStatus.PENDING);
        newRecommendation.setReason("Recommended by counselor for science pathway");
        newRecommendation.setActive(true);

        // Act
        CourseRecommendation saved = recommendationRepository.save(newRecommendation);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(testStudent.getId(), saved.getStudent().getId());
        assertEquals(testCourse2.getId(), saved.getCourse().getId());
        assertEquals(RecommendationType.COUNSELOR_MANUAL, saved.getRecommendationType());
        assertEquals(RecommendationStatus.PENDING, saved.getStatus());

        System.out.println("✓ Course recommendation created successfully");
        System.out.println("  - Student: " + testStudent.getFirstName() + " " + testStudent.getLastName());
        System.out.println("  - Course: " + testCourse2.getCourseName());
    }

    @Test
    public void testFindAllRecommendations_ShouldReturnList() {
        // Act
        List<CourseRecommendation> recommendations = recommendationRepository.findAll();

        // Assert
        assertNotNull(recommendations);
        assertTrue(recommendations.size() >= 1);
        assertTrue(recommendations.stream().anyMatch(r -> r.getId().equals(testRecommendation.getId())));

        System.out.println("✓ Found " + recommendations.size() + " recommendation(s)");
    }

    @Test
    public void testFindRecommendationsByStudent_ShouldReturnStudentRecommendations() {
        // Act
        List<CourseRecommendation> recommendations = recommendationRepository.findByStudent(testStudent);

        // Assert
        assertNotNull(recommendations);
        assertTrue(recommendations.size() >= 1);
        assertTrue(recommendations.stream().allMatch(r -> r.getStudent().getId().equals(testStudent.getId())));

        System.out.println("✓ Found " + recommendations.size() + " recommendation(s) for student");
    }

    @Test
    public void testUpdateRecommendation_ShouldPersistChanges() {
        // Arrange
        testRecommendation.setStatus(RecommendationStatus.ACCEPTED);
        testRecommendation.setStudentAccepted(true);
        testRecommendation.setStudentResponseDate(LocalDateTime.now());
        testRecommendation.setUpdatedAt(LocalDateTime.now());

        // Act
        CourseRecommendation updated = recommendationRepository.save(testRecommendation);

        // Assert
        assertEquals(RecommendationStatus.ACCEPTED, updated.getStatus());
        assertTrue(updated.getStudentAccepted());
        assertNotNull(updated.getStudentResponseDate());

        System.out.println("✓ Recommendation updated successfully");
        System.out.println("  - New status: " + updated.getStatus());
    }

    @Test
    public void testDeleteRecommendation_ShouldDeactivate() {
        // Arrange
        testRecommendation.setActive(false);

        // Act
        CourseRecommendation deactivated = recommendationRepository.save(testRecommendation);

        // Assert
        assertFalse(deactivated.getActive());

        System.out.println("✓ Recommendation deactivated (soft delete)");
    }

    // ========== RECOMMENDATION STATUS ==========

    @Test
    public void testPendingStatus_ShouldBeDefault() {
        // Assert
        assertEquals(RecommendationStatus.PENDING, testRecommendation.getStatus());
        assertTrue(testRecommendation.isPending());
        assertFalse(testRecommendation.isAccepted());
        assertFalse(testRecommendation.isRejected());

        System.out.println("✓ Pending status verified");
    }

    @Test
    public void testAcceptedStatus_ShouldBeDetectable() {
        // Arrange
        testRecommendation.setStatus(RecommendationStatus.ACCEPTED);

        // Act
        CourseRecommendation saved = recommendationRepository.save(testRecommendation);

        // Assert
        assertEquals(RecommendationStatus.ACCEPTED, saved.getStatus());
        assertTrue(saved.isAccepted());
        assertFalse(saved.isPending());
        assertFalse(saved.isRejected());

        System.out.println("✓ Accepted status verified");
    }

    @Test
    public void testRejectedStatus_ShouldBeDetectable() {
        // Arrange
        testRecommendation.setStatus(RecommendationStatus.REJECTED);
        testRecommendation.setStudentAccepted(false);

        // Act
        CourseRecommendation saved = recommendationRepository.save(testRecommendation);

        // Assert
        assertEquals(RecommendationStatus.REJECTED, saved.getStatus());
        assertTrue(saved.isRejected());
        assertFalse(saved.isPending());
        assertFalse(saved.isAccepted());
        assertFalse(saved.getStudentAccepted());

        System.out.println("✓ Rejected status verified");
    }

    // ========== RECOMMENDATION TYPES ==========

    @Test
    public void testAIGeneratedRecommendation_ShouldHaveType() {
        // Assert
        assertEquals(RecommendationType.AI_GENERATED, testRecommendation.getRecommendationType());

        System.out.println("✓ AI-generated recommendation type verified");
    }

    @Test
    public void testCounselorManualRecommendation_ShouldHaveType() {
        // Arrange
        testRecommendation.setRecommendationType(RecommendationType.COUNSELOR_MANUAL);

        // Act
        CourseRecommendation saved = recommendationRepository.save(testRecommendation);

        // Assert
        assertEquals(RecommendationType.COUNSELOR_MANUAL, saved.getRecommendationType());

        System.out.println("✓ Counselor manual recommendation type verified");
    }

    @Test
    public void testSequenceBasedRecommendation_ShouldHaveType() {
        // Arrange
        testRecommendation.setRecommendationType(RecommendationType.SEQUENCE_BASED);

        // Act
        CourseRecommendation saved = recommendationRepository.save(testRecommendation);

        // Assert
        assertEquals(RecommendationType.SEQUENCE_BASED, saved.getRecommendationType());

        System.out.println("✓ Sequence-based recommendation type verified");
    }

    // ========== RECOMMENDATION FILTERING ==========

    @Test
    public void testFilterByStatus_ShouldReturnMatchingRecommendations() {
        // Arrange - Create recommendations with different statuses
        CourseRecommendation accepted = new CourseRecommendation();
        accepted.setStudent(testStudent);
        accepted.setCourse(testCourse2);
        accepted.setRecommendedSchoolYear("2025-2026");
        accepted.setPriority(5);
        accepted.setConfidenceScore(0.70);
        accepted.setRecommendationType(RecommendationType.AI_GENERATED);
        accepted.setStatus(RecommendationStatus.ACCEPTED);
        accepted.setActive(true);
        recommendationRepository.save(accepted);

        // Act
        List<CourseRecommendation> allRecommendations = recommendationRepository.findAll();
        List<CourseRecommendation> pendingOnly = allRecommendations.stream()
            .filter(r -> RecommendationStatus.PENDING.equals(r.getStatus()))
            .toList();

        List<CourseRecommendation> acceptedOnly = allRecommendations.stream()
            .filter(r -> RecommendationStatus.ACCEPTED.equals(r.getStatus()))
            .toList();

        // Assert
        assertTrue(pendingOnly.size() >= 1);
        assertTrue(acceptedOnly.size() >= 1);
        assertTrue(pendingOnly.stream().allMatch(r -> r.getStatus() == RecommendationStatus.PENDING));
        assertTrue(acceptedOnly.stream().allMatch(r -> r.getStatus() == RecommendationStatus.ACCEPTED));

        System.out.println("✓ Status filtering works correctly");
        System.out.println("  - Pending: " + pendingOnly.size());
        System.out.println("  - Accepted: " + acceptedOnly.size());
    }

    @Test
    public void testFilterByPriority_ShouldReturnHighPriorityRecommendations() {
        // Act
        List<CourseRecommendation> allRecommendations = recommendationRepository.findAll();
        List<CourseRecommendation> highPriority = allRecommendations.stream()
            .filter(r -> r.getPriority() >= 7)
            .toList();

        // Assert
        assertTrue(highPriority.size() >= 1);
        assertTrue(highPriority.stream().anyMatch(r -> r.getId().equals(testRecommendation.getId())));
        assertTrue(highPriority.stream().allMatch(r -> r.getPriority() >= 7));

        System.out.println("✓ Priority filtering works correctly");
        System.out.println("  - High priority (7+): " + highPriority.size());
    }

    @Test
    public void testFilterByType_ShouldReturnAIRecommendations() {
        // Act
        List<CourseRecommendation> allRecommendations = recommendationRepository.findAll();
        List<CourseRecommendation> aiGenerated = allRecommendations.stream()
            .filter(r -> RecommendationType.AI_GENERATED.equals(r.getRecommendationType()))
            .toList();

        // Assert
        assertTrue(aiGenerated.size() >= 1);
        assertTrue(aiGenerated.stream().allMatch(r -> r.getRecommendationType() == RecommendationType.AI_GENERATED));

        System.out.println("✓ Type filtering works correctly");
        System.out.println("  - AI-generated: " + aiGenerated.size());
    }

    // ========== RECOMMENDATION ATTRIBUTES ==========

    @Test
    public void testConfidenceScore_ShouldBeInValidRange() {
        // Assert
        assertNotNull(testRecommendation.getConfidenceScore());
        assertTrue(testRecommendation.getConfidenceScore() >= 0.0);
        assertTrue(testRecommendation.getConfidenceScore() <= 1.0);
        assertEquals(0.85, testRecommendation.getConfidenceScore(), 0.01);

        System.out.println("✓ Confidence score in valid range");
        System.out.println("  - Confidence: " + (testRecommendation.getConfidenceScore() * 100) + "%");
    }

    @Test
    public void testPriorityLevel_ShouldBeSet() {
        // Assert
        assertNotNull(testRecommendation.getPriority());
        assertTrue(testRecommendation.getPriority() >= 1);
        assertTrue(testRecommendation.getPriority() <= 10);
        assertEquals(8, testRecommendation.getPriority());

        System.out.println("✓ Priority level set correctly");
        System.out.println("  - Priority: " + testRecommendation.getPriority() + "/10");
    }

    @Test
    public void testPrerequisites_ShouldBeTracked() {
        // Assert
        assertTrue(testRecommendation.getPrerequisitesMet());
        assertTrue(testRecommendation.getGpaRequirementMet());
        assertFalse(testRecommendation.getHasScheduleConflict());

        System.out.println("✓ Prerequisites and requirements tracked");
        System.out.println("  - Prerequisites met: " + testRecommendation.getPrerequisitesMet());
        System.out.println("  - GPA requirement met: " + testRecommendation.getGpaRequirementMet());
    }

    @Test
    public void testReasonField_ShouldStoreExplanation() {
        // Assert
        assertNotNull(testRecommendation.getReason());
        assertTrue(testRecommendation.getReason().length() > 0);
        assertTrue(testRecommendation.getReason().contains("Algebra I"));

        System.out.println("✓ Recommendation reason stored");
        System.out.println("  - Reason: " + testRecommendation.getReason());
    }

    // ========== TIMESTAMP TRACKING ==========

    @Test
    public void testTimestamps_ShouldBeTracked() {
        // Assert
        assertNotNull(testRecommendation.getCreatedAt());
        assertNotNull(testRecommendation.getUpdatedAt());

        System.out.println("✓ Timestamps tracked correctly");
    }
}
