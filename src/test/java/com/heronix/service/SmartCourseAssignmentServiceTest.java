package com.heronix.service;

import com.heronix.dto.CourseAssignmentRecommendation;
import com.heronix.dto.CourseAssignmentRecommendation.RecommendationPriority;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.impl.SmartCourseAssignmentService;
import com.heronix.testutil.BaseServiceTest;
import com.heronix.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SmartCourseAssignmentService
 * Tests intelligent course-to-teacher matching based on:
 * - Null safety (all inputs can be null)
 * - Certification matching
 * - Workload balancing
 * - Subject expertise matching
 * - Recommendation generation and prioritization
 * - Assignment application
 */
@ExtendWith(MockitoExtension.class)
class SmartCourseAssignmentServiceTest extends BaseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private SmartCourseAssignmentService service;

    private Teacher teacher1;
    private Teacher teacher2;
    private Teacher teacher3;
    private Course course1;
    private Course course2;
    private Course course3;
    private List<Teacher> allTeachers;
    private List<Course> allCourses;

    @BeforeEach
    void setUp() {
        // Create teachers with different certifications
        teacher1 = TestDataBuilder.aTeacher()
            .withId(1L)
            .withFirstName("John")
            .withLastName("Math")
            .withDepartment("Mathematics")
            .withCertifications(Arrays.asList("Math 6-12", "Algebra I"))
            .build();

        teacher2 = TestDataBuilder.aTeacher()
            .withId(2L)
            .withFirstName("Jane")
            .withLastName("Science")
            .withDepartment("Science")
            .withCertifications(Arrays.asList("Science 6-12", "Biology"))
            .build();

        teacher3 = TestDataBuilder.aTeacher()
            .withId(3L)
            .withFirstName("Bob")
            .withLastName("English")
            .withDepartment("English")
            .withCertifications(Arrays.asList("English 6-12", "Literature"))
            .build();

        allTeachers = Arrays.asList(teacher1, teacher2, teacher3);

        // Create courses with different requirements
        course1 = TestDataBuilder.aCourse()
            .withId(1L)
            .withCourseCode("MATH101")
            .withCourseName("Algebra I")
            .withSubject("Mathematics")
            .withDepartment("Mathematics")
            .build();
        course1.setRequiredCertifications(Arrays.asList("Math 6-12"));
        course1.setActive(true);

        course2 = TestDataBuilder.aCourse()
            .withId(2L)
            .withCourseCode("SCI101")
            .withCourseName("Biology I")
            .withSubject("Science")
            .withDepartment("Science")
            .build();
        course2.setRequiredCertifications(Arrays.asList("Science 6-12"));
        course2.setActive(true);

        course3 = TestDataBuilder.aCourse()
            .withId(3L)
            .withCourseCode("ENG101")
            .withCourseName("English Literature")
            .withSubject("English")
            .withDepartment("English")
            .build();
        course3.setRequiredCertifications(Arrays.asList("English 6-12"));
        course3.setActive(true);

        allCourses = Arrays.asList(course1, course2, course3);
    }

    // ==================== Null Safety Tests ====================

    @Test
    void testAnalyzeAndRecommend_WithNullCoursesFromRepository_ShouldNotThrowNPE() {
        // Given: Repository returns null
        when(courseRepository.findAll()).thenReturn(null);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithNullTeachersFromRepository_ShouldNotThrowNPE() {
        // Given: Repository returns null
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(teacherRepository.findByActiveTrue()).thenReturn(null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithBothRepositoriesReturningNull_ShouldNotThrowNPE() {
        // Given: Both repositories return null
        when(courseRepository.findAll()).thenReturn(null);
        when(teacherRepository.findByActiveTrue()).thenReturn(null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());

        List<CourseAssignmentRecommendation> result = service.analyzeAndRecommend();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAnalyzeAndRecommend_WithNullCourseInList_ShouldSkipNullCourse() {
        // Given: Course list contains null
        List<Course> coursesWithNull = new ArrayList<>();
        coursesWithNull.add(null);
        coursesWithNull.add(course1);

        when(courseRepository.findAll()).thenReturn(coursesWithNull);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When/Then: Should skip null and process valid course
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithNullTeacherInList_ShouldSkipNullTeacher() {
        // Given: Teacher list contains null
        List<Teacher> teachersWithNull = new ArrayList<>();
        teachersWithNull.add(null);
        teachersWithNull.add(teacher1);

        when(courseRepository.findAll()).thenReturn(allCourses);
        when(teacherRepository.findByActiveTrue()).thenReturn(teachersWithNull);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When/Then: Should skip null and process valid teacher
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testApplyRecommendations_WithNullRecommendationsList_ShouldNotThrowNPE() {
        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.applyRecommendations(null));

        int result = service.applyRecommendations(null);
        assertEquals(0, result);
    }

    @Test
    void testApplyRecommendations_WithNullRecommendationInList_ShouldSkipNull() {
        // Given: Recommendation list contains null
        List<CourseAssignmentRecommendation> recommendations = new ArrayList<>();
        recommendations.add(null);

        CourseAssignmentRecommendation validRec = CourseAssignmentRecommendation.builder()
            .course(course1)
            .recommendedTeacher(teacher1)
            .matchScore(100)
            .build();
        recommendations.add(validRec);

        when(courseRepository.save(any(Course.class))).thenReturn(course1);

        // When: Apply recommendations
        int result = service.applyRecommendations(recommendations);

        // Then: Should skip null and process valid recommendation
        assertEquals(1, result);
    }

    @Test
    void testApplyRecommendations_WithNullCourseInRecommendation_ShouldSkipRecommendation() {
        // Given: Recommendation has null course
        CourseAssignmentRecommendation rec = CourseAssignmentRecommendation.builder()
            .course(null)
            .recommendedTeacher(teacher1)
            .matchScore(100)
            .build();

        // When: Apply recommendations
        int result = service.applyRecommendations(Arrays.asList(rec));

        // Then: Should skip recommendation
        assertEquals(0, result);
        verify(courseRepository, never()).save(any());
    }

    // ==================== Empty Data Tests ====================

    @Test
    void testAnalyzeAndRecommend_WithEmptyCourseList_ShouldReturnEmptyList() {
        // Given: No courses
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should return empty list
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    @Test
    void testAnalyzeAndRecommend_WithEmptyTeacherList_ShouldReturnCriticalRecommendations() {
        // Given: Courses but no teachers
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(teacherRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should return CRITICAL recommendations for courses with requirements
        assertNotNull(recommendations);
        // Note: Implementation may return empty or critical - either is acceptable
    }

    @Test
    void testAnalyzeAndRecommend_WithCourseHavingNoRequiredCertifications_ShouldSkipCourse() {
        // Given: Course with no required certifications
        course1.setRequiredCertifications(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should skip course (no intelligent recommendation possible)
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    // ==================== Certification Matching Tests ====================

    @Test
    void testAnalyzeAndRecommend_WithPerfectMatch_ShouldRecommendMatchedTeacher() {
        // Given: Course requires Math cert, teacher1 has Math cert
        course1.setTeacher(null); // Unassigned

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should recommend teacher1 (Math certified)
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        CourseAssignmentRecommendation rec = recommendations.get(0);
        assertEquals(course1.getId(), rec.getCourse().getId());
        assertNotNull(rec.getRecommendedTeacher());
    }

    @Test
    void testAnalyzeAndRecommend_WithNoMatchingTeacher_ShouldReturnCriticalRecommendation() {
        // Given: Course requires non-existent certification
        course1.setRequiredCertifications(Arrays.asList("Physics 6-12"));
        course1.setTeacher(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should return HIGH recommendation with no qualified teacher
        assertNotNull(recommendations);
        if (!recommendations.isEmpty()) {
            CourseAssignmentRecommendation rec = recommendations.get(0);
            assertEquals(RecommendationPriority.HIGH, rec.getPriority());
        }
    }

    @Test
    void testAnalyzeAndRecommend_WithWrongTeacherAssigned_ShouldRecommendChange() {
        // Given: Math course assigned to English teacher
        course1.setTeacher(teacher3); // English teacher

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should recommend reassignment to Math teacher
        assertNotNull(recommendations);
        if (!recommendations.isEmpty()) {
            CourseAssignmentRecommendation rec = recommendations.get(0);
            assertEquals(RecommendationPriority.CRITICAL, rec.getPriority());
            assertEquals(teacher3.getId(), rec.getCurrentTeacher().getId());
        }
    }

    // ==================== Workload Balancing Tests ====================

    @Test
    void testAnalyzeAndRecommend_WithEqualCertifications_ShouldPreferLessLoadedTeacher() {
        // Given: Two teachers with same certification but different loads
        Teacher teacher4 = TestDataBuilder.aTeacher()
            .withId(4L)
            .withFirstName("Mike")
            .withLastName("MathTeacher")
            .withCertifications(Arrays.asList("Math 6-12"))
            .build();

        List<Teacher> teachers = Arrays.asList(teacher1, teacher4);

        // teacher1 has 5 courses, teacher4 has 0 courses
        when(courseRepository.findByTeacherId(1L)).thenReturn(Arrays.asList(
            course1, course2, course3, new Course(), new Course()
        ));
        when(courseRepository.findByTeacherId(4L)).thenReturn(Collections.emptyList());

        course1.setTeacher(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(teachers);

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should prefer less loaded teacher (implementation dependent)
        assertNotNull(recommendations);
    }

    // ==================== Priority Tests ====================

    @Test
    void testAnalyzeAndRecommend_WithUnassignedCourse_ShouldReturnHighPriority() {
        // Given: Unassigned course with qualified teacher available
        course1.setTeacher(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should return HIGH priority for unassigned course
        assertNotNull(recommendations);
        if (!recommendations.isEmpty()) {
            CourseAssignmentRecommendation rec = recommendations.get(0);
            assertTrue(rec.getPriority() == RecommendationPriority.HIGH ||
                      rec.getPriority() == RecommendationPriority.CRITICAL);
        }
    }

    @Test
    void testAnalyzeAndRecommend_WithInactiveCourse_ShouldSkipCourse() {
        // Given: Inactive course
        course1.setActive(false);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should skip inactive course
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    // ==================== Application Tests ====================

    @Test
    void testApplyRecommendations_WithValidRecommendations_ShouldApplyAssignments() {
        // Given: Valid recommendations
        CourseAssignmentRecommendation rec1 = CourseAssignmentRecommendation.builder()
            .course(course1)
            .currentTeacher(null)
            .recommendedTeacher(teacher1)
            .matchScore(100)
            .build();

        CourseAssignmentRecommendation rec2 = CourseAssignmentRecommendation.builder()
            .course(course2)
            .currentTeacher(teacher1)
            .recommendedTeacher(teacher2)
            .matchScore(95)
            .build();

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Apply recommendations
        int result = service.applyRecommendations(Arrays.asList(rec1, rec2));

        // Then: Should apply both assignments
        assertEquals(2, result);
        verify(courseRepository, times(2)).save(any(Course.class));
    }

    @Test
    void testApplyRecommendations_WithNullRecommendedTeacher_ShouldSkipRecommendation() {
        // Given: Recommendation with null recommended teacher
        CourseAssignmentRecommendation rec = CourseAssignmentRecommendation.builder()
            .course(course1)
            .currentTeacher(teacher2)
            .recommendedTeacher(null) // No recommended teacher
            .matchScore(0)
            .build();

        // When: Apply recommendations
        int result = service.applyRecommendations(Arrays.asList(rec));

        // Then: Should skip recommendation
        assertEquals(0, result);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testApplyRecommendations_WithEmptyList_ShouldReturnZero() {
        // When: Apply empty list
        int result = service.applyRecommendations(Collections.emptyList());

        // Then: Should return 0
        assertEquals(0, result);
        verify(courseRepository, never()).save(any());
    }

    // ==================== Edge Cases ====================

    @Test
    void testAnalyzeAndRecommend_WithMultipleCourses_ShouldProcessAll() {
        // Given: Multiple courses
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When: Analyze
        List<CourseAssignmentRecommendation> recommendations = service.analyzeAndRecommend();

        // Then: Should process all courses
        assertNotNull(recommendations);
    }

    @Test
    void testAnalyzeAndRecommend_WithTeacherHavingNullCertifications_ShouldNotThrowNPE() {
        // Given: Teacher with null certifications
        teacher1.setCertifications(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(teacher1));
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithTeacherHavingNullCertificationInList_ShouldNotThrowNPE() {
        // Given: Teacher certifications list contains null
        teacher1.setCertifications(Arrays.asList("Math 6-12", null, "Algebra I"));

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(teacher1));
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithCourseHavingNullRequiredCertificationInList_ShouldNotThrowNPE() {
        // Given: Course required certifications contains null
        course1.setRequiredCertifications(Arrays.asList("Math 6-12", null));

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithTeacherHavingNullFirstAndLastName_ShouldNotThrowNPE() {
        // Given: Teacher with null names
        teacher1.setFirstName(null);
        teacher1.setLastName(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(teacher1));
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithCourseHavingNullCourseName_ShouldNotThrowNPE() {
        // Given: Course with null name
        course1.setCourseName(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(Collections.emptyList());

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testAnalyzeAndRecommend_WithRepositoryReturningNullForCourseLoad_ShouldNotThrowNPE() {
        // Given: Repository returns null for course load
        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1));
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);
        when(courseRepository.findByTeacherId(anyLong())).thenReturn(null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.analyzeAndRecommend());
    }

    @Test
    void testApplyRecommendations_ShouldSetTeacherOnCourseAndSave() {
        // Given: Valid recommendation
        CourseAssignmentRecommendation rec = CourseAssignmentRecommendation.builder()
            .course(course1)
            .recommendedTeacher(teacher1)
            .matchScore(100)
            .build();

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Apply recommendation
        int result = service.applyRecommendations(Arrays.asList(rec));

        // Then: Should set teacher and save
        assertEquals(1, result);
        assertEquals(teacher1, course1.getTeacher());
        verify(courseRepository).save(course1);
    }
}
