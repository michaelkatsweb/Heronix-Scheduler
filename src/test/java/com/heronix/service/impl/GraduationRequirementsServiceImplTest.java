package com.heronix.service.impl;

import com.heronix.model.domain.Student;
import com.heronix.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for GraduationRequirementsServiceImpl
 *
 * Tests graduation tracking, credit calculations, GPA requirements, and academic standing.
 */
@ExtendWith(MockitoExtension.class)
class GraduationRequirementsServiceImplTest {

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @InjectMocks
    private GraduationRequirementsServiceImpl service;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setStudentId("STU001");
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(3.5);
        testStudent.setAcademicStanding("On Track");

        when(studentRepository.save(any(Student.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ========== GRADUATION READINESS ==========

    @Test
    void testIsOnTrackForGraduation_SeniorWithEnoughCreditsAndGPA_ShouldReturnTrue() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(2.5);

        boolean result = service.isOnTrackForGraduation(testStudent);

        assertTrue(result);
    }

    @Test
    void testIsOnTrackForGraduation_SeniorWithLowGPA_ShouldReturnFalse() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(1.8); // Below 2.0 minimum

        boolean result = service.isOnTrackForGraduation(testStudent);

        assertFalse(result);
    }

    @Test
    void testIsOnTrackForGraduation_SeniorWithInsufficientCredits_ShouldReturnFalse() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(20.0); // Need 24
        testStudent.setCurrentGPA(3.0);

        boolean result = service.isOnTrackForGraduation(testStudent);

        assertFalse(result);
    }

    @Test
    void testIsOnTrackForGraduation_WithNullStudent_ShouldReturnFalse() {
        boolean result = service.isOnTrackForGraduation(null);

        assertFalse(result);
    }

    @Test
    void testIsOnTrackForGraduation_WithNullCreditsEarned_ShouldDefaultToZero() {
        testStudent.setCreditsEarned(null);
        testStudent.setCurrentGPA(2.5);

        boolean result = service.isOnTrackForGraduation(testStudent);

        assertFalse(result); // 0 credits < 24 required
    }

    @Test
    void testIsOnTrackForGraduation_WithNullGPA_ShouldDefaultToZero() {
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(null);

        boolean result = service.isOnTrackForGraduation(testStudent);

        assertFalse(result); // 0.0 GPA < 2.0 required
    }

    // ========== REQUIRED CREDITS BY GRADE ==========

    @Test
    void testGetRequiredCreditsByGrade_Grade9_ShouldReturn6() {
        double result = service.getRequiredCreditsByGrade("9");
        assertEquals(6.0, result);
    }

    @Test
    void testGetRequiredCreditsByGrade_Grade10_ShouldReturn12() {
        double result = service.getRequiredCreditsByGrade("10");
        assertEquals(12.0, result);
    }

    @Test
    void testGetRequiredCreditsByGrade_Grade11_ShouldReturn18() {
        double result = service.getRequiredCreditsByGrade("11");
        assertEquals(18.0, result);
    }

    @Test
    void testGetRequiredCreditsByGrade_Grade12_ShouldReturn24() {
        double result = service.getRequiredCreditsByGrade("12");
        assertEquals(24.0, result);
    }

    @Test
    void testGetRequiredCreditsByGrade_InvalidGrade_ShouldReturn0() {
        double result = service.getRequiredCreditsByGrade("8");
        assertEquals(0.0, result);
    }

    @Test
    void testGetRequiredCreditsByGrade_NullGrade_ShouldReturn0() {
        double result = service.getRequiredCreditsByGrade(null);
        assertEquals(0.0, result);
    }

    @Test
    void testGetRequiredCreditsByGrade_WithWhitespace_ShouldTrimAndReturn() {
        double result = service.getRequiredCreditsByGrade("  12  ");
        assertEquals(24.0, result);
    }

    // ========== ACADEMIC STANDING STATUS ==========

    @Test
    void testGetAcademicStandingStatus_OnTrack_ShouldReturnOnTrack() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(3.0);

        String result = service.getAcademicStandingStatus(testStudent);

        assertEquals("On Track", result);
    }

    @Test
    void testGetAcademicStandingStatus_AtRisk1CreditBehind_ShouldReturnAtRisk() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(23.0); // 1 credit behind
        testStudent.setCurrentGPA(2.5);

        String result = service.getAcademicStandingStatus(testStudent);

        assertEquals("At Risk", result);
    }

    @Test
    void testGetAcademicStandingStatus_AtRiskLowGPA_ShouldReturnAtRisk() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(1.8); // Between 1.5 and 2.0

        String result = service.getAcademicStandingStatus(testStudent);

        assertEquals("At Risk", result);
    }

    @Test
    void testGetAcademicStandingStatus_RetentionRisk3CreditsBehind_ShouldReturnRetentionRisk() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(21.0); // 3 credits behind
        testStudent.setCurrentGPA(2.5);

        String result = service.getAcademicStandingStatus(testStudent);

        assertEquals("Retention Risk", result);
    }

    @Test
    void testGetAcademicStandingStatus_RetentionRiskVeryLowGPA_ShouldReturnRetentionRisk() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(1.2); // Below 1.5

        String result = service.getAcademicStandingStatus(testStudent);

        assertEquals("Retention Risk", result);
    }

    @Test
    void testGetAcademicStandingStatus_WithNullStudent_ShouldReturnUnknown() {
        String result = service.getAcademicStandingStatus(null);

        assertEquals("Unknown", result);
    }

    @Test
    void testGetAcademicStandingStatus_WithNullGPA_ShouldDefaultToZero() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(null);

        String result = service.getAcademicStandingStatus(testStudent);

        assertEquals("Retention Risk", result); // 0.0 GPA < 1.5
    }

    // ========== STANDING VISUALIZATION ==========

    @Test
    void testGetStandingColorCode_OnTrack_ShouldReturnGreen() {
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(3.0);

        String color = service.getStandingColorCode(testStudent);

        assertEquals("#4CAF50", color);
    }

    @Test
    void testGetStandingColorCode_AtRisk_ShouldReturnOrange() {
        testStudent.setCreditsEarned(23.0);
        testStudent.setCurrentGPA(2.5);

        String color = service.getStandingColorCode(testStudent);

        assertEquals("#FF9800", color);
    }

    @Test
    void testGetStandingColorCode_RetentionRisk_ShouldReturnRed() {
        testStudent.setCreditsEarned(21.0);
        testStudent.setCurrentGPA(2.5);

        String color = service.getStandingColorCode(testStudent);

        assertEquals("#f44336", color);
    }

    @Test
    void testGetStandingBackgroundColor_OnTrack_ShouldReturnLightGreen() {
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(3.0);

        String color = service.getStandingBackgroundColor(testStudent);

        assertEquals("#E8F5E9", color);
    }

    @Test
    void testGetStandingIcon_OnTrack_ShouldReturnCheckmark() {
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(3.0);

        String icon = service.getStandingIcon(testStudent);

        assertEquals("✅", icon);
    }

    @Test
    void testGetStandingIcon_AtRisk_ShouldReturnWarning() {
        testStudent.setCreditsEarned(23.0);
        testStudent.setCurrentGPA(2.5);

        String icon = service.getStandingIcon(testStudent);

        assertEquals("⚠️", icon);
    }

    @Test
    void testGetStandingIcon_RetentionRisk_ShouldReturnX() {
        testStudent.setCreditsEarned(21.0);
        testStudent.setCurrentGPA(2.5);

        String icon = service.getStandingIcon(testStudent);

        assertEquals("❌", icon);
    }

    // ========== CREDITS BEHIND ==========

    @Test
    void testGetCreditsBehind_OnTrack_ShouldReturn0() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(24.0);

        double result = service.getCreditsBehind(testStudent);

        assertEquals(0.0, result);
    }

    @Test
    void testGetCreditsBehind_Behind3Credits_ShouldReturn3() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(21.0);

        double result = service.getCreditsBehind(testStudent);

        assertEquals(3.0, result);
    }

    @Test
    void testGetCreditsBehind_Ahead_ShouldReturn0() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(26.0); // 2 ahead

        double result = service.getCreditsBehind(testStudent);

        assertEquals(0.0, result); // Never negative
    }

    @Test
    void testGetCreditsBehind_WithNullStudent_ShouldReturn0() {
        double result = service.getCreditsBehind(null);

        assertEquals(0.0, result);
    }

    @Test
    void testGetCreditsBehind_WithNullCreditsEarned_ShouldCalculateFromZero() {
        testStudent.setGradeLevel("12");
        testStudent.setCreditsEarned(null);

        double result = service.getCreditsBehind(testStudent);

        assertEquals(24.0, result); // 0 earned, 24 required
    }

    // ========== TOOLTIP GENERATION ==========

    @Test
    void testGetStandingTooltip_OnTrack_ShouldIncludePositiveMessage() {
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(3.0);

        String tooltip = service.getStandingTooltip(testStudent);

        assertTrue(tooltip.contains("On Track"));
        assertTrue(tooltip.contains("24.0 / 24.0 required"));
        assertTrue(tooltip.contains("on track for graduation"));
    }

    @Test
    void testGetStandingTooltip_AtRisk_ShouldIncludeWarnings() {
        testStudent.setCreditsEarned(23.0);
        testStudent.setCurrentGPA(2.5);

        String tooltip = service.getStandingTooltip(testStudent);

        assertTrue(tooltip.contains("At Risk"));
        assertTrue(tooltip.contains("Credits Behind: 1.0"));
        assertTrue(tooltip.contains("WARNING"));
    }

    @Test
    void testGetStandingTooltip_RetentionRisk_ShouldIncludeCriticalWarning() {
        testStudent.setCreditsEarned(21.0);
        testStudent.setCurrentGPA(1.2);

        String tooltip = service.getStandingTooltip(testStudent);

        assertTrue(tooltip.contains("Retention Risk"));
        assertTrue(tooltip.contains("CRITICAL"));
        assertTrue(tooltip.contains("intervention required"));
    }

    @Test
    void testGetStandingTooltip_WithNullStudent_ShouldReturnNoDataMessage() {
        String tooltip = service.getStandingTooltip(null);

        assertEquals("No data available", tooltip);
    }

    // ========== STUDENT LISTS ==========

    @Test
    void testGetAtRiskStudents_ShouldReturnOnlyAtRiskStudents() {
        Student atRisk1 = createStudent(1L, "12", 23.0, 2.5);
        Student onTrack = createStudent(2L, "12", 24.0, 3.0);
        Student atRisk2 = createStudent(3L, "11", 17.0, 2.5);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(atRisk1, onTrack, atRisk2));

        List<Student> result = service.getAtRiskStudents();

        assertEquals(2, result.size());
        assertTrue(result.contains(atRisk1));
        assertTrue(result.contains(atRisk2));
        assertFalse(result.contains(onTrack));
    }

    @Test
    void testGetAtRiskStudents_WithNullInList_ShouldFilterNulls() {
        Student atRisk = createStudent(1L, "12", 23.0, 2.5);
        when(studentRepository.findAll()).thenReturn(Arrays.asList(atRisk, null));

        List<Student> result = service.getAtRiskStudents();

        assertEquals(1, result.size());
        assertEquals(atRisk.getId(), result.get(0).getId());
    }

    @Test
    void testGetRetentionRiskStudents_ShouldReturnOnlyRetentionRiskStudents() {
        Student retention1 = createStudent(1L, "12", 21.0, 2.5);
        Student onTrack = createStudent(2L, "12", 24.0, 3.0);
        Student retention2 = createStudent(3L, "12", 24.0, 1.2);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(retention1, onTrack, retention2));

        List<Student> result = service.getRetentionRiskStudents();

        assertEquals(2, result.size());
    }

    @Test
    void testGetSeniorsNotMeetingRequirements_ShouldReturnOnlySeniorsNotOnTrack() {
        Student seniorOffTrack = createStudent(1L, "12", 20.0, 2.5);
        Student seniorOnTrack = createStudent(2L, "12", 24.0, 3.0);
        Student juniorOffTrack = createStudent(3L, "11", 15.0, 2.5);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(seniorOffTrack, seniorOnTrack, juniorOffTrack));

        List<Student> result = service.getSeniorsNotMeetingRequirements();

        assertEquals(1, result.size());
        assertEquals(seniorOffTrack.getId(), result.get(0).getId());
    }

    @Test
    void testGetSeniorsNotMeetingRequirements_WithNullGradeLevel_ShouldFilterOut() {
        Student seniorNoGrade = createStudent(1L, null, 20.0, 2.5);
        when(studentRepository.findAll()).thenReturn(Arrays.asList(seniorNoGrade));

        List<Student> result = service.getSeniorsNotMeetingRequirements();

        assertEquals(0, result.size());
    }

    // ========== SUMMARY STATISTICS ==========

    @Test
    void testGetAcademicStandingSummary_ShouldCountAllCategories() {
        Student onTrack = createStudent(1L, "12", 24.0, 3.0);
        Student atRisk = createStudent(2L, "12", 23.0, 2.5);
        Student retention = createStudent(3L, "12", 21.0, 2.5);

        when(studentRepository.findAll()).thenReturn(Arrays.asList(onTrack, atRisk, retention));

        Map<String, Integer> summary = service.getAcademicStandingSummary();

        assertEquals(1, summary.get("onTrack"));
        assertEquals(1, summary.get("atRisk"));
        assertEquals(1, summary.get("retentionRisk"));
        assertEquals(3, summary.get("total"));
    }

    @Test
    void testGetAcademicStandingSummary_WithNullStudent_ShouldSkipNull() {
        Student onTrack = createStudent(1L, "12", 24.0, 3.0);
        when(studentRepository.findAll()).thenReturn(Arrays.asList(onTrack, null));

        Map<String, Integer> summary = service.getAcademicStandingSummary();

        assertEquals(1, summary.get("onTrack"));
        assertEquals(2, summary.get("total")); // Total includes null
    }

    // ========== GPA REQUIREMENT ==========

    @Test
    void testMeetsGPARequirement_Above2Point0_ShouldReturnTrue() {
        testStudent.setCurrentGPA(2.5);

        boolean result = service.meetsGPARequirement(testStudent);

        assertTrue(result);
    }

    @Test
    void testMeetsGPARequirement_Exactly2Point0_ShouldReturnTrue() {
        testStudent.setCurrentGPA(2.0);

        boolean result = service.meetsGPARequirement(testStudent);

        assertTrue(result);
    }

    @Test
    void testMeetsGPARequirement_Below2Point0_ShouldReturnFalse() {
        testStudent.setCurrentGPA(1.9);

        boolean result = service.meetsGPARequirement(testStudent);

        assertFalse(result);
    }

    @Test
    void testMeetsGPARequirement_WithNullStudent_ShouldReturnFalse() {
        boolean result = service.meetsGPARequirement(null);

        assertFalse(result);
    }

    @Test
    void testMeetsGPARequirement_WithNullGPA_ShouldReturnFalse() {
        testStudent.setCurrentGPA(null);

        boolean result = service.meetsGPARequirement(testStudent);

        assertFalse(result); // Defaults to 0.0, which is < 2.0
    }

    // ========== GRADE NUMBER ==========

    @Test
    void testGetGradeNumber_Valid9to12_ShouldReturnNumber() {
        assertEquals(9, service.getGradeNumber("9"));
        assertEquals(10, service.getGradeNumber("10"));
        assertEquals(11, service.getGradeNumber("11"));
        assertEquals(12, service.getGradeNumber("12"));
    }

    @Test
    void testGetGradeNumber_BelowRange_ShouldReturnNegative1() {
        assertEquals(-1, service.getGradeNumber("8"));
    }

    @Test
    void testGetGradeNumber_AboveRange_ShouldReturnNegative1() {
        assertEquals(-1, service.getGradeNumber("13"));
    }

    @Test
    void testGetGradeNumber_WithNullGrade_ShouldReturnNegative1() {
        assertEquals(-1, service.getGradeNumber(null));
    }

    @Test
    void testGetGradeNumber_WithInvalidString_ShouldReturnNegative1() {
        assertEquals(-1, service.getGradeNumber("senior"));
    }

    @Test
    void testGetGradeNumber_WithWhitespace_ShouldTrimAndParse() {
        assertEquals(12, service.getGradeNumber("  12  "));
    }

    // ========== UPDATE STANDING ==========

    @Test
    void testUpdateAcademicStanding_ShouldCalculateAndSave() {
        testStudent.setAcademicStanding("Unknown");
        testStudent.setCreditsEarned(24.0);
        testStudent.setCurrentGPA(3.0);

        Student result = service.updateAcademicStanding(testStudent);

        assertEquals("On Track", result.getAcademicStanding());
        verify(studentRepository).save(testStudent);
    }

    @Test
    void testUpdateAcademicStanding_WithNullStudent_ShouldReturnNull() {
        Student result = service.updateAcademicStanding(null);

        assertNull(result);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void testUpdateAllAcademicStandings_ShouldUpdateChangedStudents() {
        Student student1 = createStudent(1L, "12", 24.0, 3.0);
        student1.setAcademicStanding("Unknown"); // Needs update

        Student student2 = createStudent(2L, "12", 24.0, 3.0);
        student2.setAcademicStanding("On Track"); // Already correct

        when(studentRepository.findAll()).thenReturn(Arrays.asList(student1, student2));

        int updated = service.updateAllAcademicStandings();

        assertEquals(1, updated); // Only student1 updated
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    void testUpdateAllAcademicStandings_WithNullInList_ShouldSkipNull() {
        Student student1 = createStudent(1L, "12", 24.0, 3.0);
        student1.setAcademicStanding("Unknown");

        when(studentRepository.findAll()).thenReturn(Arrays.asList(student1, null));

        int updated = service.updateAllAcademicStandings();

        assertEquals(1, updated);
        verify(studentRepository, times(1)).save(student1);
    }

    // ========== HELPER METHODS ==========

    private Student createStudent(Long id, String gradeLevel, Double credits, Double gpa) {
        Student student = new Student();
        student.setId(id);
        student.setGradeLevel(gradeLevel);
        student.setCreditsEarned(credits);
        student.setCurrentGPA(gpa);
        student.setAcademicStanding("Unknown");
        return student;
    }
}
