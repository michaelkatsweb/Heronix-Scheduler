package com.heronix.service.impl;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.TranscriptRecord;
import com.heronix.repository.TranscriptRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.service.impl.TranscriptService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for TranscriptService
 *
 * Testing Strategy:
 * - Null safety for all parameters
 * - Repository null handling
 * - Edge cases (empty lists, missing data)
 * - GPA calculation accuracy
 * - Graduation requirement logic
 *
 * @author Heronix Scheduling System Test Team
 * @version 1.0.0
 * @since December 17, 2025
 */
@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

    @Mock(lenient = true)
    private TranscriptRepository transcriptRepository;

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @InjectMocks
    private TranscriptService service;

    private Student testStudent;
    private TranscriptRecord testRecord;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Smith");
        testStudent.setStudentId("STU001");
        testStudent.setGradeLevel("11");

        // Create test transcript record
        testRecord = TranscriptRecord.builder()
            .id(1L)
            .student(testStudent)
            .academicYear("2024-2025")
            .semester(TranscriptRecord.Semester.FALL)
            .letterGrade("A")
            .numericGrade(new BigDecimal("95.0"))
            .gradePoints(new BigDecimal("4.0"))
            .creditsAttempted(new BigDecimal("1.0"))
            .creditsEarned(new BigDecimal("1.0"))
            .courseType(TranscriptRecord.CourseType.REGULAR)
            .weighted(false)
            .weightFactor(BigDecimal.ONE)
            .gradeLevel(11)
            .includeInGpa(true)
            .build();
    }

    // ==================== generateTranscript() Tests ====================

    @Test
    void testGenerateTranscript_WithNullStudentId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.generateTranscript(null));
    }

    @Test
    void testGenerateTranscript_WhenStudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.generateTranscript(999L));
    }

    @Test
    void testGenerateTranscript_WithNullRecordsList_ShouldHandleGracefully() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.findByStudentIdOrderByAcademicYearDescSemesterDesc(1L)).thenReturn(null);
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(BigDecimal.ZERO);

        StudentTranscript result = service.generateTranscript(1L);

        assertNotNull(result);
        assertEquals(1L, result.getStudentId());
        assertTrue(result.getAcademicYears().isEmpty());
    }

    @Test
    void testGenerateTranscript_WithEmptyRecordsList_ShouldReturnEmptyTranscript() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.findByStudentIdOrderByAcademicYearDescSemesterDesc(1L))
            .thenReturn(new ArrayList<>());
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(BigDecimal.ZERO);

        StudentTranscript result = service.generateTranscript(1L);

        assertNotNull(result);
        assertTrue(result.getAcademicYears().isEmpty());
    }

    @Test
    void testGenerateTranscript_WithNullStudentNames_ShouldUseUnknown() {
        testStudent.setFirstName(null);
        testStudent.setLastName(null);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.findByStudentIdOrderByAcademicYearDescSemesterDesc(1L))
            .thenReturn(new ArrayList<>());
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(BigDecimal.ZERO);

        StudentTranscript result = service.generateTranscript(1L);

        assertEquals("Unknown Student", result.getStudentName());
    }

    @Test
    void testGenerateTranscript_WithNullGpaValues_ShouldDefaultToZero() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.findByStudentIdOrderByAcademicYearDescSemesterDesc(1L))
            .thenReturn(new ArrayList<>());
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(null);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(null);
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(null);

        StudentTranscript result = service.generateTranscript(1L);

        assertEquals(BigDecimal.ZERO, result.getCumulativeGpa());
        assertEquals(BigDecimal.ZERO, result.getWeightedGpa());
        assertEquals(BigDecimal.ZERO, result.getTotalCreditsEarned());
    }

    @Test
    void testGenerateTranscript_WithValidData_ShouldReturnTranscript() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.findByStudentIdOrderByAcademicYearDescSemesterDesc(1L))
            .thenReturn(Arrays.asList(testRecord));
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(new BigDecimal("3.5"));
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.8"));
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(new BigDecimal("15.0"));

        StudentTranscript result = service.generateTranscript(1L);

        assertNotNull(result);
        assertEquals("John Smith", result.getStudentName());
        assertEquals(new BigDecimal("3.5"), result.getCumulativeGpa());
    }

    // ==================== calculateCumulativeGpa() Tests ====================

    @Test
    void testCalculateCumulativeGpa_WithNullStudentId_ShouldReturnZero() {
        BigDecimal result = service.calculateCumulativeGpa(null);

        assertEquals(BigDecimal.ZERO, result);
        verify(transcriptRepository, never()).calculateUnweightedGpa(any());
    }

    @Test
    void testCalculateCumulativeGpa_WithValidStudentId_ShouldReturnGpa() {
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(new BigDecimal("3.5"));

        BigDecimal result = service.calculateCumulativeGpa(1L);

        assertEquals(new BigDecimal("3.5"), result);
    }

    @Test
    void testCalculateCumulativeGpa_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(null);

        assertDoesNotThrow(() -> service.calculateCumulativeGpa(1L));
    }

    // ==================== calculateWeightedGpa() Tests ====================

    @Test
    void testCalculateWeightedGpa_WithNullStudentId_ShouldReturnZero() {
        BigDecimal result = service.calculateWeightedGpa(null);

        assertEquals(BigDecimal.ZERO, result);
        verify(transcriptRepository, never()).calculateWeightedGpa(any());
    }

    @Test
    void testCalculateWeightedGpa_WithValidStudentId_ShouldReturnGpa() {
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.8"));

        BigDecimal result = service.calculateWeightedGpa(1L);

        assertEquals(new BigDecimal("3.8"), result);
    }

    @Test
    void testCalculateWeightedGpa_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(null);

        assertDoesNotThrow(() -> service.calculateWeightedGpa(1L));
    }

    // ==================== addGrade() Tests ====================

    @Test
    void testAddGrade_WithNullStudentId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addGrade(null, 1L, "2024-2025",
                TranscriptRecord.Semester.FALL, "A", new BigDecimal("95.0"),
                new BigDecimal("1.0"), TranscriptRecord.CourseType.REGULAR));
    }

    @Test
    void testAddGrade_WithNullAcademicYear_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addGrade(1L, 1L, null,
                TranscriptRecord.Semester.FALL, "A", new BigDecimal("95.0"),
                new BigDecimal("1.0"), TranscriptRecord.CourseType.REGULAR));
    }

    @Test
    void testAddGrade_WithEmptyAcademicYear_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addGrade(1L, 1L, "",
                TranscriptRecord.Semester.FALL, "A", new BigDecimal("95.0"),
                new BigDecimal("1.0"), TranscriptRecord.CourseType.REGULAR));
    }

    @Test
    void testAddGrade_WithNullSemester_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addGrade(1L, 1L, "2024-2025",
                null, "A", new BigDecimal("95.0"),
                new BigDecimal("1.0"), TranscriptRecord.CourseType.REGULAR));
    }

    @Test
    void testAddGrade_WhenStudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.addGrade(999L, 1L, "2024-2025",
                TranscriptRecord.Semester.FALL, "A", new BigDecimal("95.0"),
                new BigDecimal("1.0"), TranscriptRecord.CourseType.REGULAR));
    }

    @Test
    void testAddGrade_WithValidData_ShouldSaveRecord() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.save(any(TranscriptRecord.class))).thenReturn(testRecord);

        TranscriptRecord result = service.addGrade(1L, 1L, "2024-2025",
            TranscriptRecord.Semester.FALL, "A", new BigDecimal("95.0"),
            new BigDecimal("1.0"), TranscriptRecord.CourseType.REGULAR);

        assertNotNull(result);
        verify(transcriptRepository).save(any(TranscriptRecord.class));
    }

    @Test
    void testAddGrade_WithNullCourseType_ShouldNotThrowNPE() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.save(any(TranscriptRecord.class))).thenReturn(testRecord);

        assertDoesNotThrow(() -> service.addGrade(1L, 1L, "2024-2025",
            TranscriptRecord.Semester.FALL, "A", new BigDecimal("95.0"),
            new BigDecimal("1.0"), null));
    }

    // ==================== getClassRank() Tests ====================

    @Test
    void testGetClassRank_WithNullStudentId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.getClassRank(null));
    }

    @Test
    void testGetClassRank_WhenStudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.getClassRank(999L));
    }

    @Test
    void testGetClassRank_WithNullClassmatesList_ShouldHandleGracefully() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findByGradeLevel("11")).thenReturn(null);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.5"));

        ClassRankInfo result = service.getClassRank(1L);

        assertNotNull(result);
        assertEquals(1, result.getRank());
        assertEquals(0, result.getTotalStudents());
    }

    @Test
    void testGetClassRank_WithEmptyClassmatesList_ShouldReturnRank1() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findByGradeLevel("11")).thenReturn(new ArrayList<>());
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.5"));

        ClassRankInfo result = service.getClassRank(1L);

        assertNotNull(result);
        assertEquals(1, result.getRank());
    }

    @Test
    void testGetClassRank_WithNullStudentsInList_ShouldSkipNulls() {
        List<Student> classmates = new ArrayList<>();
        classmates.add(testStudent);
        classmates.add(null);  // Null student should be skipped
        classmates.add(new Student());  // Student with null ID should be skipped

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findByGradeLevel("11")).thenReturn(classmates);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.5"));

        assertDoesNotThrow(() -> service.getClassRank(1L));
    }

    @Test
    void testGetClassRank_WithValidData_ShouldReturnRankInfo() {
        Student student2 = new Student();
        student2.setId(2L);
        student2.setGradeLevel("11");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findByGradeLevel("11")).thenReturn(Arrays.asList(testStudent, student2));
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.5"));
        when(transcriptRepository.calculateWeightedGpa(2L)).thenReturn(new BigDecimal("3.0"));

        ClassRankInfo result = service.getClassRank(1L);

        assertNotNull(result);
        assertEquals(1, result.getRank());
        assertEquals(2, result.getTotalStudents());
    }

    @Test
    void testGetClassRank_WithNullGpaInList_ShouldSkipNullGpas() {
        Student student2 = new Student();
        student2.setId(2L);
        student2.setGradeLevel("11");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findByGradeLevel("11")).thenReturn(Arrays.asList(testStudent, student2));
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.5"));
        when(transcriptRepository.calculateWeightedGpa(2L)).thenReturn(null);  // Null GPA should be skipped

        ClassRankInfo result = service.getClassRank(1L);

        assertNotNull(result);
        assertEquals(1, result.getTotalStudents());  // Only 1 student with valid GPA
    }

    // ==================== checkGraduationRequirements() Tests ====================

    @Test
    void testCheckGraduationRequirements_WithNullStudentId_ShouldNotThrowNPE() {
        when(transcriptRepository.sumCreditsEarnedByStudent(null)).thenReturn(null);
        when(transcriptRepository.countCompletedCoursesByStudent(null)).thenReturn(0L);

        assertDoesNotThrow(() -> service.checkGraduationRequirements(null));
    }

    @Test
    void testCheckGraduationRequirements_WithNullCreditsEarned_ShouldDefaultToZero() {
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(null);
        when(transcriptRepository.countCompletedCoursesByStudent(1L)).thenReturn(5L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        GraduationStatus result = service.checkGraduationRequirements(1L);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getCreditsEarned());
        assertFalse(result.isMeetsRequirements());
    }

    @Test
    void testCheckGraduationRequirements_WithSufficientCredits_ShouldMeetRequirements() {
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(new BigDecimal("24.0"));
        when(transcriptRepository.countCompletedCoursesByStudent(1L)).thenReturn(20L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        GraduationStatus result = service.checkGraduationRequirements(1L);

        assertNotNull(result);
        assertTrue(result.isMeetsRequirements());
    }

    @Test
    void testCheckGraduationRequirements_WithInsufficientCredits_ShouldNotMeetRequirements() {
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(new BigDecimal("10.0"));
        when(transcriptRepository.countCompletedCoursesByStudent(1L)).thenReturn(10L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        GraduationStatus result = service.checkGraduationRequirements(1L);

        assertNotNull(result);
        assertFalse(result.isMeetsRequirements());
    }

    @Test
    void testCheckGraduationRequirements_WithNullStudent_ShouldHandleProjectedGradYear() {
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(new BigDecimal("15.0"));
        when(transcriptRepository.countCompletedCoursesByStudent(1L)).thenReturn(15L);
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        GraduationStatus result = service.checkGraduationRequirements(1L);

        assertNotNull(result);
        assertEquals("Unknown", result.getProjectedGraduationYear());
    }

    @Test
    void testCheckGraduationRequirements_WithNullGradeLevel_ShouldReturnUnknownGradYear() {
        testStudent.setGradeLevel(null);
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(new BigDecimal("15.0"));
        when(transcriptRepository.countCompletedCoursesByStudent(1L)).thenReturn(15L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        GraduationStatus result = service.checkGraduationRequirements(1L);

        assertEquals("Unknown", result.getProjectedGraduationYear());
    }

    @Test
    void testCheckGraduationRequirements_WithInvalidGradeLevel_ShouldReturnUnknownGradYear() {
        testStudent.setGradeLevel("InvalidGrade");
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(new BigDecimal("15.0"));
        when(transcriptRepository.countCompletedCoursesByStudent(1L)).thenReturn(15L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        GraduationStatus result = service.checkGraduationRequirements(1L);

        assertEquals("Unknown", result.getProjectedGraduationYear());
    }

    // ==================== Business Logic Tests ====================

    @Test
    void testGenerateTranscript_WithRecordsContainingNullAcademicYear_ShouldFilterThem() {
        TranscriptRecord recordWithNullYear = TranscriptRecord.builder()
            .id(2L)
            .academicYear(null)  // Null year should be filtered
            .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.findByStudentIdOrderByAcademicYearDescSemesterDesc(1L))
            .thenReturn(Arrays.asList(testRecord, recordWithNullYear));
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(BigDecimal.ZERO);

        StudentTranscript result = service.generateTranscript(1L);

        assertNotNull(result);
        // Should only have 1 year (filtered the null one)
        assertEquals(1, result.getAcademicYears().size());
    }

    @Test
    void testGenerateTranscript_WithNullRecordInList_ShouldFilterIt() {
        List<TranscriptRecord> recordsWithNull = new ArrayList<>();
        recordsWithNull.add(testRecord);
        recordsWithNull.add(null);  // Null record should be filtered

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.findByStudentIdOrderByAcademicYearDescSemesterDesc(1L))
            .thenReturn(recordsWithNull);
        when(transcriptRepository.calculateUnweightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(BigDecimal.ZERO);
        when(transcriptRepository.sumCreditsEarnedByStudent(1L)).thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() -> service.generateTranscript(1L));
    }

    @Test
    void testAddGrade_WithNullLetterGrade_ShouldNotCrash() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(transcriptRepository.save(any(TranscriptRecord.class))).thenReturn(testRecord);

        assertDoesNotThrow(() -> service.addGrade(1L, 1L, "2024-2025",
            TranscriptRecord.Semester.FALL, null, new BigDecimal("95.0"),
            new BigDecimal("1.0"), TranscriptRecord.CourseType.REGULAR));
    }

    @Test
    void testGetClassRank_WithStudentNotInList_ShouldStillCalculateRank() {
        Student otherStudent = new Student();
        otherStudent.setId(2L);
        otherStudent.setGradeLevel("11");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findByGradeLevel("11")).thenReturn(Arrays.asList(otherStudent));
        when(transcriptRepository.calculateWeightedGpa(1L)).thenReturn(new BigDecimal("3.5"));
        when(transcriptRepository.calculateWeightedGpa(2L)).thenReturn(new BigDecimal("3.0"));

        ClassRankInfo result = service.getClassRank(1L);

        assertNotNull(result);
        // Student not in list but still gets ranked
        assertTrue(result.getRank() > 0);
    }
}
