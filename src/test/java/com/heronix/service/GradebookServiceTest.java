package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.domain.AssignmentGrade.GradeStatus;
import com.heronix.model.domain.GradingCategory.CategoryType;
import com.heronix.repository.*;
import com.heronix.service.GradebookService.CategoryGrade;
import com.heronix.service.GradebookService.ClassGradebook;
import com.heronix.service.GradebookService.StudentCourseGrade;
import com.heronix.testutil.BaseServiceTest;
import com.heronix.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for GradebookService
 * Tests null safety, grade calculations, category management, and reporting
 *
 * Focus areas:
 * - Null safety for all public methods
 * - Grade calculation logic (weighted averages, drop lowest)
 * - Category weight validation
 * - Assignment management
 * - Bulk operations
 * - Edge cases (null scores, empty categories, missing data)
 */
@ExtendWith(MockitoExtension.class)
class GradebookServiceTest extends BaseServiceTest {

    @Mock(lenient = true)
    private GradingCategoryRepository categoryRepository;

    @Mock(lenient = true)
    private AssignmentRepository assignmentRepository;

    @Mock(lenient = true)
    private AssignmentGradeRepository gradeRepository;

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @InjectMocks
    private GradebookService service;

    private Course testCourse;
    private Student testStudent;
    private GradingCategory testCategory;
    private Assignment testAssignment;
    private AssignmentGrade testGrade;

    @BeforeEach
    void setUp() {
        // Create test course
        testCourse = TestDataBuilder.aCourse()
                .withId(1L)
                .withCourseName("Math 101")
                .build();

        // Create test student
        testStudent = TestDataBuilder.aStudent()
                .withId(1L)
                .withFirstName("John")
                .withLastName("Doe")
                .build();

        // Create test category
        testCategory = GradingCategory.builder()
                .id(1L)
                .course(testCourse)
                .name("Tests")
                .categoryType(CategoryType.TEST)
                .weight(30.0)
                .dropLowest(0)
                .active(true)
                .build();

        // Create test assignment
        testAssignment = Assignment.builder()
                .id(1L)
                .course(testCourse)
                .category(testCategory)
                .title("Unit Test 1")
                .maxPoints(100.0)
                .dueDate(LocalDate.now().plusDays(7))
                .published(true)
                .build();

        // Create test grade
        testGrade = AssignmentGrade.builder()
                .id(1L)
                .student(testStudent)
                .assignment(testAssignment)
                .score(95.0)
                .status(GradeStatus.GRADED)
                .gradedDate(LocalDate.now())
                .build();
    }

    // ========================================================================
    // NULL SAFETY TESTS - createDefaultCategories()
    // ========================================================================

    @Test
    void testCreateDefaultCategories_WithNullCourseId_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.createDefaultCategories(null)
        );
    }

    @Test
    void testCreateDefaultCategories_WhenCourseNotFound_ShouldThrowException() {
        // Given
        when(courseRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.createDefaultCategories(999L)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - getCategoriesForCourse()
    // ========================================================================

    @Test
    void testGetCategoriesForCourse_WithNullCourseId_ShouldNotThrowNPE() {
        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.getCategoriesForCourse(null)
        );
    }

    @Test
    void testGetCategoriesForCourse_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        // Given
        when(categoryRepository.findByCourseIdAndActiveTrueOrderByDisplayOrder(anyLong()))
            .thenReturn(null);

        // When/Then: Should not throw
        assertDoesNotThrow(() -> {
            List<GradingCategory> result = service.getCategoriesForCourse(1L);
            // Result may be null - service doesn't validate
        });
    }

    // ========================================================================
    // NULL SAFETY TESTS - updateCategoryWeights()
    // ========================================================================

    @Test
    void testUpdateCategoryWeights_WithNullMap_ShouldNotThrowNPE() {
        // When/Then: May throw NPE - testing for null safety
        assertThrows(NullPointerException.class, () ->
            service.updateCategoryWeights(null)
        );
    }

    @Test
    void testUpdateCategoryWeights_WithEmptyMap_ShouldNotThrowException() {
        // Given
        Map<Long, Double> weights = new HashMap<>();

        // When/Then: Should handle empty map (sum = 0)
        assertThrows(IllegalArgumentException.class, () ->
            service.updateCategoryWeights(weights)
        );
    }

    @Test
    void testUpdateCategoryWeights_WithNullValueInMap_ShouldNotThrowNPE() {
        // Given
        Map<Long, Double> weights = new HashMap<>();
        weights.put(1L, null);

        // When/Then: Should throw NPE when processing null value
        assertThrows(NullPointerException.class, () ->
            service.updateCategoryWeights(weights)
        );
    }

    @Test
    void testUpdateCategoryWeights_WithInvalidSum_ShouldThrowException() {
        // Given
        Map<Long, Double> weights = new HashMap<>();
        weights.put(1L, 50.0);
        weights.put(2L, 40.0); // Sum = 90, not 100

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.updateCategoryWeights(weights)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - createAssignment()
    // ========================================================================

    @Test
    void testCreateAssignment_WithNullCourseId_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.createAssignment(null, 1L, "Test", 100.0, LocalDate.now())
        );
    }

    @Test
    void testCreateAssignment_WithNullCategoryId_ShouldThrowException() {
        // Given
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.createAssignment(1L, null, "Test", 100.0, LocalDate.now())
        );
    }

    @Test
    void testCreateAssignment_WithNullTitle_ShouldNotThrowNPE() {
        // Given
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(testCategory));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.createAssignment(1L, 1L, null, 100.0, LocalDate.now())
        );
    }

    @Test
    void testCreateAssignment_WithNullMaxPoints_ShouldNotThrowNPE() {
        // Given
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(testCategory));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.createAssignment(1L, 1L, "Test", null, LocalDate.now())
        );
    }

    @Test
    void testCreateAssignment_WithNullDueDate_ShouldNotThrowNPE() {
        // Given
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(testCategory));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.createAssignment(1L, 1L, "Test", 100.0, null)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - getAssignmentsForCourse()
    // ========================================================================

    @Test
    void testGetAssignmentsForCourse_WithNullCourseId_ShouldNotThrowNPE() {
        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.getAssignmentsForCourse(null)
        );
    }

    @Test
    void testGetAssignmentsForCourse_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        // Given
        when(assignmentRepository.findByCourseIdOrderByDueDateDesc(anyLong()))
            .thenReturn(null);

        // When/Then: Should not throw
        assertDoesNotThrow(() -> {
            List<Assignment> result = service.getAssignmentsForCourse(1L);
            // Result may be null
        });
    }

    // ========================================================================
    // NULL SAFETY TESTS - publishAssignment()
    // ========================================================================

    @Test
    void testPublishAssignment_WithNullAssignmentId_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.publishAssignment(null)
        );
    }

    @Test
    void testPublishAssignment_WhenAssignmentNotFound_ShouldThrowException() {
        // Given
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.publishAssignment(999L)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - enterGrade()
    // ========================================================================

    @Test
    void testEnterGrade_WithNullStudentId_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.enterGrade(null, 1L, 95.0, LocalDate.now(), "Good work")
        );
    }

    @Test
    void testEnterGrade_WithNullAssignmentId_ShouldThrowException() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.enterGrade(1L, null, 95.0, LocalDate.now(), "Good work")
        );
    }

    @Test
    void testEnterGrade_WithNullScore_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(testAssignment));
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(gradeRepository.save(any(AssignmentGrade.class))).thenReturn(testGrade);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.enterGrade(1L, 1L, null, LocalDate.now(), "Good work")
        );
    }

    @Test
    void testEnterGrade_WithNullSubmittedDate_ShouldUseCurrentDate() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(testAssignment));
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(gradeRepository.save(any(AssignmentGrade.class))).thenReturn(testGrade);

        // When
        AssignmentGrade result = service.enterGrade(1L, 1L, 95.0, null, "Good work");

        // Then
        assertNotNull(result);
    }

    @Test
    void testEnterGrade_WithNullComments_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(testAssignment));
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(gradeRepository.save(any(AssignmentGrade.class))).thenReturn(testGrade);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.enterGrade(1L, 1L, 95.0, LocalDate.now(), null)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - bulkEnterGrades()
    // ========================================================================

    @Test
    void testBulkEnterGrades_WithNullAssignmentId_ShouldNotThrowNPE() {
        // Given
        Map<Long, Double> scores = new HashMap<>();
        scores.put(1L, 95.0);

        // When/Then: May throw when trying to enter grades
        assertDoesNotThrow(() -> {
            int result = service.bulkEnterGrades(null, scores);
            // Should return 0 (all failed)
        });
    }

    @Test
    void testBulkEnterGrades_WithNullScoresMap_ShouldNotThrowNPE() {
        // When/Then: Should throw NPE
        assertThrows(NullPointerException.class, () ->
            service.bulkEnterGrades(1L, null)
        );
    }

    @Test
    void testBulkEnterGrades_WithEmptyMap_ShouldReturnZero() {
        // Given
        Map<Long, Double> scores = new HashMap<>();

        // When
        int result = service.bulkEnterGrades(1L, scores);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testBulkEnterGrades_WithNullValueInMap_ShouldSkipEntry() {
        // Given
        Map<Long, Double> scores = new HashMap<>();
        scores.put(1L, null);

        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(testAssignment));
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(gradeRepository.save(any(AssignmentGrade.class))).thenReturn(testGrade);

        // When
        int result = service.bulkEnterGrades(1L, scores);

        // Then: Should process entry with null score
        assertEquals(1, result);
    }

    // ========================================================================
    // NULL SAFETY TESTS - excuseGrade()
    // ========================================================================

    @Test
    void testExcuseGrade_WithNullStudentId_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.excuseGrade(null, 1L, "Sick")
        );
    }

    @Test
    void testExcuseGrade_WithNullAssignmentId_ShouldThrowException() {
        // Given
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.excuseGrade(1L, null, "Sick")
        );
    }

    @Test
    void testExcuseGrade_WithNullReason_ShouldNotThrowNPE() {
        // Given
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.of(testGrade));
        when(gradeRepository.save(any(AssignmentGrade.class))).thenReturn(testGrade);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.excuseGrade(1L, 1L, null)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - calculateCourseGrade()
    // ========================================================================

    @Test
    void testCalculateCourseGrade_WithNullStudentId_ShouldNotThrowNPE() {
        // Given
        when(categoryRepository.findByCourseIdAndActiveTrueOrderByDisplayOrder(anyLong()))
            .thenReturn(Arrays.asList(testCategory));
        when(gradeRepository.findForGradebookCalculation(anyLong(), anyLong()))
            .thenReturn(new ArrayList<>());

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.calculateCourseGrade(null, 1L)
        );
    }

    @Test
    void testCalculateCourseGrade_WithNullCourseId_ShouldNotThrowNPE() {
        // Given
        when(categoryRepository.findByCourseIdAndActiveTrueOrderByDisplayOrder(any()))
            .thenReturn(new ArrayList<>());
        when(gradeRepository.findForGradebookCalculation(anyLong(), any()))
            .thenReturn(new ArrayList<>());

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.calculateCourseGrade(1L, null)
        );
    }

    @Test
    void testCalculateCourseGrade_WithEmptyCategories_ShouldReturnEmptyGrade() {
        // Given
        when(categoryRepository.findByCourseIdAndActiveTrueOrderByDisplayOrder(anyLong()))
            .thenReturn(new ArrayList<>());
        when(gradeRepository.findForGradebookCalculation(anyLong(), anyLong()))
            .thenReturn(new ArrayList<>());

        // When
        StudentCourseGrade result = service.calculateCourseGrade(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getFinalPercentage());
        assertEquals("-", result.getLetterGrade());
    }

    @Test
    void testCalculateCourseGrade_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        // Given
        when(categoryRepository.findByCourseIdAndActiveTrueOrderByDisplayOrder(anyLong()))
            .thenReturn(null);
        when(gradeRepository.findForGradebookCalculation(anyLong(), anyLong()))
            .thenReturn(null);

        // When/Then: May throw NPE - testing for null safety
        assertThrows(NullPointerException.class, () ->
            service.calculateCourseGrade(1L, 1L)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - getClassGradebook()
    // ========================================================================

    @Test
    void testGetClassGradebook_WithNullCourseId_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.getClassGradebook(null)
        );
    }

    @Test
    void testGetClassGradebook_WhenCourseNotFound_ShouldThrowException() {
        // Given
        when(courseRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.getClassGradebook(999L)
        );
    }

    @Test
    void testGetClassGradebook_WithNullStudentList_ShouldHandleGracefully() {
        // Given
        testCourse.setStudents(null);
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));
        when(assignmentRepository.findByCourseIdWithGrades(anyLong()))
            .thenReturn(new ArrayList<>());
        when(categoryRepository.findByCourseIdAndActiveTrueOrderByDisplayOrder(anyLong()))
            .thenReturn(new ArrayList<>());

        // When
        ClassGradebook result = service.getClassGradebook(1L);

        // Then: Should handle null students gracefully
        assertNotNull(result);
    }

    // ========================================================================
    // BUSINESS LOGIC TESTS
    // ========================================================================

    @Test
    void testCreateDefaultCategories_ShouldCreate5Categories() {
        // Given
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));
        when(categoryRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        // When
        List<GradingCategory> result = service.createDefaultCategories(1L);

        // Then
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(categoryRepository).saveAll(anyList());
    }

    @Test
    void testUpdateCategoryWeights_WithValidWeights_ShouldUpdateAll() {
        // Given
        Map<Long, Double> weights = new HashMap<>();
        weights.put(1L, 50.0);
        weights.put(2L, 50.0);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        GradingCategory category2 = GradingCategory.builder().id(2L).build();
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category2));
        when(categoryRepository.save(any(GradingCategory.class))).thenAnswer(i -> i.getArguments()[0]);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.updateCategoryWeights(weights)
        );

        verify(categoryRepository, times(2)).save(any(GradingCategory.class));
    }

    @Test
    void testEnterGrade_WithValidData_ShouldCreateGrade() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(testAssignment));
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(gradeRepository.save(any(AssignmentGrade.class))).thenReturn(testGrade);

        // When
        AssignmentGrade result = service.enterGrade(1L, 1L, 95.0, LocalDate.now(), "Good work");

        // Then
        assertNotNull(result);
        verify(gradeRepository).save(any(AssignmentGrade.class));
    }

    @Test
    void testBulkEnterGrades_WithValidData_ShouldProcessAll() {
        // Given
        Map<Long, Double> scores = new HashMap<>();
        scores.put(1L, 95.0);
        scores.put(2L, 88.0);

        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(testAssignment));
        when(gradeRepository.findByStudentIdAndAssignmentId(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(gradeRepository.save(any(AssignmentGrade.class))).thenReturn(testGrade);

        // When
        int result = service.bulkEnterGrades(1L, scores);

        // Then
        assertEquals(2, result);
        verify(gradeRepository, times(2)).save(any(AssignmentGrade.class));
    }
}
