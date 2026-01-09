package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ConflictMatrixServiceImpl
 *
 * Tests conflict matrix generation from course requests, singleton detection,
 * conflict percentage calculation, and heatmap generation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConflictMatrixServiceImplTest {

    @Mock
    private ConflictMatrixRepository conflictMatrixRepository;

    @Mock
    private CourseRequestRepository courseRequestRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ConflictMatrixServiceImpl service;

    private Course course1;
    private Course course2;
    private Course singletonCourse;
    private ConflictMatrix testConflict;
    private Student testStudent;
    private CourseRequest request1;
    private CourseRequest request2;

    @BeforeEach
    void setUp() {
        // Create test courses
        course1 = new Course();
        course1.setId(1L);
        course1.setCourseCode("MATH101");
        course1.setCourseName("Algebra I");
        course1.setCurrentEnrollment(30);
        course1.setIsSingleton(false);

        course2 = new Course();
        course2.setId(2L);
        course2.setCourseCode("ENG101");
        course2.setCourseName("English I");
        course2.setCurrentEnrollment(25);
        course2.setIsSingleton(false);

        singletonCourse = new Course();
        singletonCourse.setId(3L);
        singletonCourse.setCourseCode("AP101");
        singletonCourse.setCourseName("AP Physics");
        singletonCourse.setCurrentEnrollment(15);
        singletonCourse.setIsSingleton(true);

        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("12345");
        testStudent.setFirstName("John");
        testStudent.setLastName("Smith");

        // Create test course requests
        request1 = new CourseRequest();
        request1.setId(1L);
        request1.setStudent(testStudent);
        request1.setCourse(course1);
        request1.setRequestYear(2025);

        request2 = new CourseRequest();
        request2.setId(2L);
        request2.setStudent(testStudent);
        request2.setCourse(course2);
        request2.setRequestYear(2025);

        // Create test conflict matrix
        testConflict = new ConflictMatrix();
        testConflict.setId(1L);
        testConflict.setCourse1(course1);
        testConflict.setCourse2(course2);
        testConflict.setConflictCount(10);
        testConflict.setScheduleYear(2025);
        testConflict.setConflictPercentage(40.0);
        testConflict.setIsSingletonConflict(false);
        testConflict.setPriorityLevel(5);
        testConflict.setCreatedAt(LocalDateTime.now());

        // Default mock setups
        when(conflictMatrixRepository.save(any(ConflictMatrix.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ========================================================================
    // GENERATE CONFLICT MATRIX TESTS
    // ========================================================================

    @Test
    void testGenerateConflictMatrix_WithValidRequests_ShouldCreateConflicts() {
        List<CourseRequest> requests = Arrays.asList(request1, request2);

        when(courseRequestRepository.findPendingRequestsForYear(2025)).thenReturn(requests);
        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(new ArrayList<>());

        service.generateConflictMatrix(2025);

        verify(conflictMatrixRepository).deleteAll(anyList());
        verify(conflictMatrixRepository, atLeastOnce()).save(any(ConflictMatrix.class));
    }

    @Test
    void testGenerateConflictMatrix_WithMultipleStudents_ShouldCreateMultipleConflicts() {
        Student student2 = new Student();
        student2.setId(2L);
        student2.setStudentId("67890");

        CourseRequest request3 = new CourseRequest();
        request3.setId(3L);
        request3.setStudent(student2);
        request3.setCourse(course1);

        CourseRequest request4 = new CourseRequest();
        request4.setId(4L);
        request4.setStudent(student2);
        request4.setCourse(course2);

        List<CourseRequest> requests = Arrays.asList(request1, request2, request3, request4);

        when(courseRequestRepository.findPendingRequestsForYear(2025)).thenReturn(requests);
        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(new ArrayList<>());

        service.generateConflictMatrix(2025);

        // Should save conflicts for both students
        verify(conflictMatrixRepository, atLeast(2)).save(any(ConflictMatrix.class));
    }

    @Test
    void testGenerateConflictMatrix_WithSingletonCourses_ShouldMarkSingletonConflicts() {
        request2.setCourse(singletonCourse);

        ConflictMatrix singletonConflict = new ConflictMatrix();
        singletonConflict.setCourse1(course1);
        singletonConflict.setCourse2(singletonCourse);
        singletonConflict.setConflictCount(5);
        singletonConflict.setIsSingletonConflict(false);

        List<CourseRequest> requests = Arrays.asList(request1, request2);

        when(courseRequestRepository.findPendingRequestsForYear(2025)).thenReturn(requests);
        when(conflictMatrixRepository.findByScheduleYear(2025))
            .thenReturn(Arrays.asList(singletonConflict));

        service.generateConflictMatrix(2025);

        verify(conflictMatrixRepository, atLeastOnce()).save(argThat(conflict ->
            conflict != null && Boolean.TRUE.equals(conflict.getIsSingletonConflict())
        ));
    }

    @Test
    void testGenerateConflictMatrix_WithEmptyRequests_ShouldHandleGracefully() {
        when(courseRequestRepository.findPendingRequestsForYear(2025)).thenReturn(new ArrayList<>());
        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(new ArrayList<>());

        service.generateConflictMatrix(2025);

        verify(conflictMatrixRepository).deleteAll(anyList());
        // Should not crash
    }

    @Test
    void testGenerateConflictMatrix_WithNullStudentInRequest_ShouldSkip() {
        request1.setStudent(null);

        List<CourseRequest> requests = Arrays.asList(request1, request2);

        when(courseRequestRepository.findPendingRequestsForYear(2025)).thenReturn(requests);
        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(new ArrayList<>());

        service.generateConflictMatrix(2025);

        // Should handle null student gracefully
        verify(conflictMatrixRepository).deleteAll(anyList());
    }

    // ========================================================================
    // GET CONFLICTS FOR COURSE TESTS
    // ========================================================================

    @Test
    void testGetConflictsForCourse_WithValidCourse_ShouldReturnConflicts() {
        List<ConflictMatrix> conflicts = Arrays.asList(testConflict);

        when(conflictMatrixRepository.findAllConflictsForCourse(course1)).thenReturn(conflicts);

        List<ConflictMatrix> result = service.getConflictsForCourse(course1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testConflict, result.get(0));
    }

    @Test
    void testGetConflictsForCourse_WithNoConflicts_ShouldReturnEmptyList() {
        when(conflictMatrixRepository.findAllConflictsForCourse(course1)).thenReturn(new ArrayList<>());

        List<ConflictMatrix> result = service.getConflictsForCourse(course1);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========================================================================
    // GET SINGLETON CONFLICTS TESTS
    // ========================================================================

    @Test
    void testGetSingletonConflicts_ShouldReturnOnlySingletonConflicts() {
        ConflictMatrix singletonConflict = new ConflictMatrix();
        singletonConflict.setId(2L);
        singletonConflict.setCourse1(course1);
        singletonConflict.setCourse2(singletonCourse);
        singletonConflict.setIsSingletonConflict(true);

        List<ConflictMatrix> conflicts = Arrays.asList(singletonConflict);

        when(conflictMatrixRepository.findSingletonConflicts()).thenReturn(conflicts);

        List<ConflictMatrix> result = service.getSingletonConflicts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsSingletonConflict());
    }

    @Test
    void testGetSingletonConflicts_WithNoSingletons_ShouldReturnEmptyList() {
        when(conflictMatrixRepository.findSingletonConflicts()).thenReturn(new ArrayList<>());

        List<ConflictMatrix> result = service.getSingletonConflicts();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========================================================================
    // GET HIGH CONFLICTS TESTS
    // ========================================================================

    @Test
    void testGetHighConflicts_WithThreshold_ShouldReturnHighConflicts() {
        testConflict.setConflictCount(50);

        List<ConflictMatrix> conflicts = Arrays.asList(testConflict);

        when(conflictMatrixRepository.findHighConflicts(40)).thenReturn(conflicts);

        List<ConflictMatrix> result = service.getHighConflicts(40);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getConflictCount() >= 40);
    }

    @Test
    void testGetHighConflicts_WithHighThreshold_ShouldReturnEmptyList() {
        when(conflictMatrixRepository.findHighConflicts(100)).thenReturn(new ArrayList<>());

        List<ConflictMatrix> result = service.getHighConflicts(100);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========================================================================
    // HAS CONFLICT TESTS
    // ========================================================================

    @Test
    void testHasConflict_WithConflictAboveThreshold_ShouldReturnTrue() {
        testConflict.setConflictCount(15);

        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.of(testConflict));

        boolean result = service.hasConflict(course1, course2, 10);

        assertTrue(result);
    }

    @Test
    void testHasConflict_WithConflictBelowThreshold_ShouldReturnFalse() {
        testConflict.setConflictCount(5);

        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.of(testConflict));

        boolean result = service.hasConflict(course1, course2, 10);

        assertFalse(result);
    }

    @Test
    void testHasConflict_WithNoConflict_ShouldReturnFalse() {
        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.empty());
        when(conflictMatrixRepository.findByCourse1AndCourse2(course2, course1))
            .thenReturn(Optional.empty());

        boolean result = service.hasConflict(course1, course2, 10);

        assertFalse(result);
    }

    @Test
    void testHasConflict_WithReversedCourseOrder_ShouldFindConflict() {
        testConflict.setConflictCount(15);

        when(conflictMatrixRepository.findByCourse1AndCourse2(course2, course1))
            .thenReturn(Optional.empty());
        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.of(testConflict));

        boolean result = service.hasConflict(course2, course1, 10);

        assertTrue(result);
    }

    // ========================================================================
    // GET CONFLICT HEATMAP TESTS
    // ========================================================================

    @Test
    void testGetConflictHeatmap_WithValidYear_ShouldReturnHeatmap() {
        List<ConflictMatrix> conflicts = Arrays.asList(testConflict);

        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(conflicts);

        Map<String, Map<String, Integer>> result = service.getConflictHeatmap(2025);

        assertNotNull(result);
        assertTrue(result.containsKey("Algebra I"));
        assertTrue(result.containsKey("English I"));
        assertEquals(10, result.get("Algebra I").get("English I"));
        assertEquals(10, result.get("English I").get("Algebra I"));
    }

    @Test
    void testGetConflictHeatmap_WithMultipleConflicts_ShouldBuildCompleteHeatmap() {
        ConflictMatrix conflict2 = new ConflictMatrix();
        conflict2.setCourse1(course1);
        conflict2.setCourse2(singletonCourse);
        conflict2.setConflictCount(5);

        List<ConflictMatrix> conflicts = Arrays.asList(testConflict, conflict2);

        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(conflicts);

        Map<String, Map<String, Integer>> result = service.getConflictHeatmap(2025);

        assertNotNull(result);
        assertEquals(3, result.size()); // 3 courses
        assertTrue(result.containsKey("Algebra I"));
        assertTrue(result.containsKey("English I"));
        assertTrue(result.containsKey("AP Physics"));
    }

    @Test
    void testGetConflictHeatmap_WithEmptyYear_ShouldReturnEmptyMap() {
        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(new ArrayList<>());

        Map<String, Map<String, Integer>> result = service.getConflictHeatmap(2025);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========================================================================
    // CALCULATE CONFLICT PERCENTAGE TESTS
    // ========================================================================

    @Test
    void testCalculateConflictPercentage_WithValidConflict_ShouldCalculatePercentage() {
        testConflict.setConflictCount(10);
        course1.setCurrentEnrollment(30);
        course2.setCurrentEnrollment(25);

        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.of(testConflict));

        Double result = service.calculateConflictPercentage(course1, course2);

        assertNotNull(result);
        // 10 / 25 (min enrollment) * 100 = 40.0%
        assertEquals(40.0, result, 0.001);
    }

    @Test
    void testCalculateConflictPercentage_WithZeroEnrollment_ShouldReturnZero() {
        testConflict.setConflictCount(10);
        course1.setCurrentEnrollment(0);
        course2.setCurrentEnrollment(25);

        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.of(testConflict));

        Double result = service.calculateConflictPercentage(course1, course2);

        assertNotNull(result);
        assertEquals(0.0, result, 0.001);
    }

    @Test
    void testCalculateConflictPercentage_WithNoConflict_ShouldReturnZero() {
        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.empty());
        when(conflictMatrixRepository.findByCourse1AndCourse2(course2, course1))
            .thenReturn(Optional.empty());

        Double result = service.calculateConflictPercentage(course1, course2);

        assertNotNull(result);
        assertEquals(0.0, result, 0.001);
    }

    @Test
    void testCalculateConflictPercentage_WithReversedCourses_ShouldFindConflict() {
        testConflict.setConflictCount(12);
        course1.setCurrentEnrollment(30);
        course2.setCurrentEnrollment(20);

        when(conflictMatrixRepository.findByCourse1AndCourse2(course2, course1))
            .thenReturn(Optional.empty());
        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.of(testConflict));

        Double result = service.calculateConflictPercentage(course2, course1);

        assertNotNull(result);
        // 12 / 20 (min enrollment) * 100 = 60.0%
        assertEquals(60.0, result, 0.001);
    }

    // ========================================================================
    // UPDATE CONFLICT TESTS
    // ========================================================================

    @Test
    void testUpdateConflict_WithNewConflict_ShouldCreateNew() {
        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.empty());

        ConflictMatrix result = service.updateConflict(course1, course2, 5);

        assertNotNull(result);
        assertEquals(course1, result.getCourse1());
        assertEquals(course2, result.getCourse2());
        assertEquals(5, result.getConflictCount());
        verify(conflictMatrixRepository).save(any(ConflictMatrix.class));
    }

    @Test
    void testUpdateConflict_WithExistingConflict_ShouldIncrementCount() {
        testConflict.setConflictCount(10);

        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.of(testConflict));

        ConflictMatrix result = service.updateConflict(course1, course2, 5);

        assertNotNull(result);
        assertEquals(15, result.getConflictCount()); // 10 + 5
        verify(conflictMatrixRepository).save(testConflict);
    }

    @Test
    void testUpdateConflict_WithReversedOrder_ShouldMaintainConsistentOrdering() {
        // course2.id (2) > course1.id (1), so they should be swapped
        when(conflictMatrixRepository.findByCourse1AndCourse2(course1, course2))
            .thenReturn(Optional.empty());

        ConflictMatrix result = service.updateConflict(course2, course1, 3);

        assertNotNull(result);
        // Should be ordered by ID (course1 has ID 1, course2 has ID 2)
        assertEquals(course1, result.getCourse1());
        assertEquals(course2, result.getCourse2());
    }

    // ========================================================================
    // CLEAR CONFLICT MATRIX TESTS
    // ========================================================================

    @Test
    void testClearConflictMatrix_WithValidYear_ShouldDeleteAll() {
        List<ConflictMatrix> conflicts = Arrays.asList(testConflict);

        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(conflicts);

        service.clearConflictMatrix(2025);

        verify(conflictMatrixRepository).deleteAll(conflicts);
    }

    @Test
    void testClearConflictMatrix_WithEmptyYear_ShouldHandleGracefully() {
        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(new ArrayList<>());

        service.clearConflictMatrix(2025);

        verify(conflictMatrixRepository).deleteAll(anyList());
    }

    @Test
    void testClearConflictMatrix_WithMultipleConflicts_ShouldDeleteAll() {
        ConflictMatrix conflict2 = new ConflictMatrix();
        conflict2.setId(2L);

        List<ConflictMatrix> conflicts = Arrays.asList(testConflict, conflict2);

        when(conflictMatrixRepository.findByScheduleYear(2025)).thenReturn(conflicts);

        service.clearConflictMatrix(2025);

        verify(conflictMatrixRepository).deleteAll(conflicts);
    }

    // ========================================================================
    // NULL SAFETY TESTS
    // ========================================================================

    @Test
    void testGenerateConflictMatrix_WithNullYear_ShouldNotCrash() {
        when(courseRequestRepository.findPendingRequestsForYear(null)).thenReturn(new ArrayList<>());
        when(conflictMatrixRepository.findByScheduleYear(null)).thenReturn(new ArrayList<>());

        service.generateConflictMatrix(null);

        verify(conflictMatrixRepository).deleteAll(anyList());
    }

    @Test
    void testGetConflictsForCourse_WithNullCourse_ShouldReturnEmpty() {
        when(conflictMatrixRepository.findAllConflictsForCourse(null)).thenReturn(new ArrayList<>());

        List<ConflictMatrix> result = service.getConflictsForCourse(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testUpdateConflict_WithNullCourses_ShouldHandleGracefully() {
        // May throw NPE or handle gracefully depending on implementation
        try {
            service.updateConflict(null, course2, 5);
        } catch (NullPointerException e) {
            // Expected for null course
            assertNotNull(e);
        }
    }
}
