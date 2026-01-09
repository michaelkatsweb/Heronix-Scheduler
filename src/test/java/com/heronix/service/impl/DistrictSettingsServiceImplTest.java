package com.heronix.service.impl;

import com.heronix.model.DistrictSettings;
import com.heronix.repository.DistrictSettingsRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for DistrictSettingsServiceImpl
 *
 * Service: 27th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/DistrictSettingsServiceImplTest.java
 *
 * Tests cover:
 * - District settings CRUD operations
 * - Teacher email generation with various formats
 * - Student email generation with various formats
 * - Email duplicate detection and alternatives
 * - Teacher ID generation with auto-increment
 * - Student ID generation with graduation year sequences
 * - Room phone number generation
 * - Format validation
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class DistrictSettingsServiceImplTest {

    @Mock(lenient = true)
    private DistrictSettingsRepository districtSettingsRepository;

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @InjectMocks
    private DistrictSettingsServiceImpl service;

    private DistrictSettings testSettings;

    @BeforeEach
    void setUp() {
        // Create test district settings
        testSettings = DistrictSettings.builder()
                .id(1L)
                .districtName("Test District")
                .teacherEmailDomain("@school.edu")
                .studentEmailDomain("@students.school.edu")
                .teacherEmailFormat("{lastname}_{firstname_initial}")
                .studentEmailFormat("{firstname}.{lastname}")
                .teacherIdPrefix("T")
                .teacherIdStartNumber(1)
                .teacherIdPadding(4)
                .teacherIdCurrentNumber(1)
                .studentIdFormat("{grad_year}-{sequence}")
                .studentIdPadding(4)
                .roomPhonePrefix("(555) 123-")
                .roomPhoneExtensionStart(4000)
                .autoGenerateTeacherEmail(true)
                .autoGenerateStudentEmail(true)
                .autoGenerateTeacherId(true)
                .autoGenerateStudentId(true)
                .autoGenerateRoomPhone(true)
                .defaultPeriodDuration(50)
                .defaultPassingPeriod(5)
                .defaultLunchDuration(30)
                .includeDistrictInfoOnPrint(true)
                .build();
    }

    // ========== DISTRICT SETTINGS CRUD TESTS ==========

    @Test
    void testGetOrCreateDistrictSettings_WhenExists_ShouldReturnExisting() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        DistrictSettings result = service.getOrCreateDistrictSettings();

        assertNotNull(result);
        assertEquals("Test District", result.getDistrictName());
        verify(districtSettingsRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateDistrictSettings_WhenNotExists_ShouldCreateDefault() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.empty());
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        DistrictSettings result = service.getOrCreateDistrictSettings();

        assertNotNull(result);
        assertEquals("Heronix Scheduler District", result.getDistrictName());
        verify(districtSettingsRepository).save(any(DistrictSettings.class));
    }

    @Test
    void testUpdateDistrictSettings_WithId_ShouldUpdate() {
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        DistrictSettings result = service.updateDistrictSettings(testSettings);

        assertNotNull(result);
        verify(districtSettingsRepository).save(testSettings);
        verify(districtSettingsRepository, never()).deleteAll();
    }

    @Test
    void testUpdateDistrictSettings_WithoutId_ShouldDeleteAndCreate() {
        testSettings.setId(null);
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        DistrictSettings result = service.updateDistrictSettings(testSettings);

        assertNotNull(result);
        verify(districtSettingsRepository).deleteAll();
        verify(districtSettingsRepository).save(testSettings);
    }

    @Test
    void testIsDistrictConfigured_WithConfiguredSettings_ShouldReturnTrue() {
        testSettings.setDistrictName("Test District");
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        boolean result = service.isDistrictConfigured();

        assertTrue(result);
    }

    @Test
    void testIsDistrictConfigured_WithNonConfiguredSettings_ShouldReturnFalse() {
        testSettings.setDistrictName(null);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        boolean result = service.isDistrictConfigured();

        assertFalse(result);
    }

    // ========== TEACHER EMAIL GENERATION TESTS ==========

    @Test
    void testGenerateTeacherEmail_WithStandardFormat_ShouldGenerateCorrectly() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(teacherRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateTeacherEmail("John", "Smith");

        assertEquals("smith_j@school.edu", result);
    }

    @Test
    void testGenerateTeacherEmail_WithMiddleName_ShouldIncludeMiddleInitial() {
        testSettings.setTeacherEmailFormat("{lastname}_{firstname_initial}{middlename_initial}");
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(teacherRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateTeacherEmail("John", "Michael", "Smith");

        assertEquals("smith_jm@school.edu", result);
    }

    @Test
    void testGenerateTeacherEmail_WithDuplicate_ShouldGenerateAlternative() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(teacherRepository.existsByEmail("smith_j@school.edu")).thenReturn(true);
        when(teacherRepository.existsByEmail("smith_j2@school.edu")).thenReturn(false);

        String result = service.generateTeacherEmail("John", "Smith");

        assertEquals("smith_j2@school.edu", result);
    }

    @Test
    void testGenerateTeacherEmail_WithAutoGenerateDisabled_ShouldReturnEmpty() {
        testSettings.setAutoGenerateTeacherEmail(false);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateTeacherEmail("John", "Smith");

        assertEquals("", result);
    }

    @Test
    void testGenerateTeacherEmail_WithNullSettings_ShouldReturnEmpty() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.empty());
        when(districtSettingsRepository.save(any())).thenAnswer(i -> {
            DistrictSettings ds = i.getArgument(0);
            ds.setAutoGenerateTeacherEmail(null);
            return ds;
        });

        String result = service.generateTeacherEmail("John", "Smith");

        assertEquals("", result);
    }

    @Test
    void testGenerateTeacherEmail_WithNullEmailFormat_ShouldReturnEmpty() {
        testSettings.setTeacherEmailFormat(null);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateTeacherEmail("John", "Smith");

        assertEquals("", result);
    }

    // ========== STUDENT EMAIL GENERATION TESTS ==========

    @Test
    void testGenerateStudentEmail_WithStandardFormat_ShouldGenerateCorrectly() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(studentRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateStudentEmail("Alice", "Johnson", 2025);

        assertEquals("alice.johnson@students.school.edu", result);
    }

    @Test
    void testGenerateStudentEmail_WithGradYear_ShouldIncludeYear() {
        testSettings.setStudentEmailFormat("{firstname}.{lastname}{grad_year_short}");
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(studentRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateStudentEmail("Alice", "Johnson", 2025);

        assertEquals("alice.johnson25@students.school.edu", result);
    }

    @Test
    void testGenerateStudentEmail_WithDuplicate_ShouldGenerateAlternative() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(studentRepository.existsByEmail("alice.johnson@students.school.edu")).thenReturn(true);
        when(studentRepository.existsByEmail("alice.johnson2@students.school.edu")).thenReturn(false);

        String result = service.generateStudentEmail("Alice", "Johnson", 2025);

        assertEquals("alice.johnson2@students.school.edu", result);
    }

    @Test
    void testGenerateStudentEmail_WithAutoGenerateDisabled_ShouldReturnEmpty() {
        testSettings.setAutoGenerateStudentEmail(false);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateStudentEmail("Alice", "Johnson", 2025);

        assertEquals("", result);
    }

    @Test
    void testGenerateStudentEmail_WithStudentId_ShouldIncludeId() {
        testSettings.setStudentEmailFormat("{student_id}");
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(studentRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateStudentEmail("Alice", "Johnson", 2025, "2025-0001");

        assertEquals("2025-0001@students.school.edu", result);
    }

    // ========== EMAIL VALIDATION TESTS ==========

    @Test
    void testIsTeacherEmailTaken_WithExistingEmail_ShouldReturnTrue() {
        when(teacherRepository.existsByEmail("john@school.edu")).thenReturn(true);

        boolean result = service.isTeacherEmailTaken("john@school.edu");

        assertTrue(result);
    }

    @Test
    void testIsTeacherEmailTaken_WithNewEmail_ShouldReturnFalse() {
        when(teacherRepository.existsByEmail("new@school.edu")).thenReturn(false);

        boolean result = service.isTeacherEmailTaken("new@school.edu");

        assertFalse(result);
    }

    @Test
    void testIsTeacherEmailTaken_WithNullEmail_ShouldReturnFalse() {
        boolean result = service.isTeacherEmailTaken(null);

        assertFalse(result);
        verify(teacherRepository, never()).existsByEmail(anyString());
    }

    @Test
    void testIsStudentEmailTaken_WithExistingEmail_ShouldReturnTrue() {
        when(studentRepository.existsByEmail("alice@students.school.edu")).thenReturn(true);

        boolean result = service.isStudentEmailTaken("alice@students.school.edu");

        assertTrue(result);
    }

    @Test
    void testIsStudentEmailTaken_WithNewEmail_ShouldReturnFalse() {
        when(studentRepository.existsByEmail("new@students.school.edu")).thenReturn(false);

        boolean result = service.isStudentEmailTaken("new@students.school.edu");

        assertFalse(result);
    }

    @Test
    void testGenerateAlternativeEmail_WithoutExistingNumber_ShouldAppend2() {
        when(teacherRepository.existsByEmail("smith2@school.edu")).thenReturn(false);

        String result = service.generateAlternativeEmail("smith@school.edu", true);

        assertEquals("smith2@school.edu", result);
    }

    @Test
    void testGenerateAlternativeEmail_WithExistingNumber_ShouldStripAndAppend() {
        when(teacherRepository.existsByEmail("smith2@school.edu")).thenReturn(false);

        String result = service.generateAlternativeEmail("smith5@school.edu", true);

        assertEquals("smith2@school.edu", result);
    }

    @Test
    void testGenerateAlternativeEmail_WithMultipleTaken_ShouldFindNextAvailable() {
        when(teacherRepository.existsByEmail("smith2@school.edu")).thenReturn(true);
        when(teacherRepository.existsByEmail("smith3@school.edu")).thenReturn(true);
        when(teacherRepository.existsByEmail("smith4@school.edu")).thenReturn(false);

        String result = service.generateAlternativeEmail("smith@school.edu", true);

        assertEquals("smith4@school.edu", result);
    }

    // ========== TEACHER ID GENERATION TESTS ==========

    @Test
    void testGenerateNextTeacherId_WithDefaultSettings_ShouldGenerateCorrectly() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        String result = service.generateNextTeacherId();

        assertEquals("T0001", result);
        verify(districtSettingsRepository).save(any(DistrictSettings.class));
    }

    @Test
    void testGenerateNextTeacherId_WithCustomPadding_ShouldPadCorrectly() {
        testSettings.setTeacherIdPadding(6);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        String result = service.generateNextTeacherId();

        assertEquals("T000001", result);
    }

    @Test
    void testGenerateNextTeacherId_ShouldAutoIncrement() {
        testSettings.setTeacherIdCurrentNumber(10);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        String result = service.generateNextTeacherId();

        assertEquals("T0010", result);
        assertEquals(11, testSettings.getTeacherIdCurrentNumber());
    }

    @Test
    void testGenerateNextTeacherId_WithAutoGenerateDisabled_ShouldReturnEmpty() {
        testSettings.setAutoGenerateTeacherId(false);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateNextTeacherId();

        assertEquals("", result);
    }

    @Test
    void testGenerateNextTeacherId_WithNullCurrentNumber_ShouldUseStartNumber() {
        testSettings.setTeacherIdCurrentNumber(null);
        testSettings.setTeacherIdStartNumber(100);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        String result = service.generateNextTeacherId();

        assertEquals("T0100", result);
    }

    @Test
    void testGenerateNextTeacherId_WithNullPadding_ShouldUseDefaultPadding() {
        testSettings.setTeacherIdPadding(null);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(districtSettingsRepository.save(any(DistrictSettings.class))).thenAnswer(i -> i.getArgument(0));

        String result = service.generateNextTeacherId();

        assertEquals("T0001", result);
    }

    // ========== STUDENT ID GENERATION TESTS ==========

    @Test
    void testGenerateNextStudentId_WithDefaultFormat_ShouldGenerateCorrectly() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateNextStudentId(2025);

        assertEquals("2025-0001", result);
    }

    @Test
    void testGenerateNextStudentId_WithCustomFormat_ShouldGenerateCorrectly() {
        testSettings.setStudentIdFormat("{grad_year_short}{sequence}");
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateNextStudentId(2025);

        assertEquals("250001", result);
    }

    @Test
    void testGenerateNextStudentId_ShouldAutoIncrementPerYear() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result1 = service.generateNextStudentId(2025);
        String result2 = service.generateNextStudentId(2025);
        String result3 = service.generateNextStudentId(2026);

        assertEquals("2025-0001", result1);
        assertEquals("2025-0002", result2);
        assertEquals("2026-0001", result3); // Different year, starts at 1
    }

    @Test
    void testGenerateNextStudentId_WithAutoGenerateDisabled_ShouldReturnEmpty() {
        testSettings.setAutoGenerateStudentId(false);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateNextStudentId(2025);

        assertEquals("", result);
    }

    @Test
    void testGenerateNextStudentId_WithNullPadding_ShouldUseDefaultPadding() {
        testSettings.setStudentIdPadding(null);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateNextStudentId(2025);

        assertEquals("2025-0001", result);
    }

    // ========== ID AVAILABILITY TESTS ==========

    @Test
    void testIsTeacherIdAvailable_WithAvailableId_ShouldReturnTrue() {
        when(teacherRepository.existsByEmployeeId("T0001")).thenReturn(false);

        boolean result = service.isTeacherIdAvailable("T0001");

        assertTrue(result);
    }

    @Test
    void testIsTeacherIdAvailable_WithTakenId_ShouldReturnFalse() {
        when(teacherRepository.existsByEmployeeId("T0001")).thenReturn(true);

        boolean result = service.isTeacherIdAvailable("T0001");

        assertFalse(result);
    }

    @Test
    void testIsTeacherIdAvailable_WithNullId_ShouldReturnFalse() {
        boolean result = service.isTeacherIdAvailable(null);

        assertFalse(result);
        verify(teacherRepository, never()).existsByEmployeeId(anyString());
    }

    @Test
    void testIsStudentIdAvailable_WithAvailableId_ShouldReturnTrue() {
        when(studentRepository.existsByStudentId("2025-0001")).thenReturn(false);

        boolean result = service.isStudentIdAvailable("2025-0001");

        assertTrue(result);
    }

    @Test
    void testIsStudentIdAvailable_WithTakenId_ShouldReturnFalse() {
        when(studentRepository.existsByStudentId("2025-0001")).thenReturn(true);

        boolean result = service.isStudentIdAvailable("2025-0001");

        assertFalse(result);
    }

    // ========== ROOM PHONE GENERATION TESTS ==========

    @Test
    void testGenerateRoomPhoneNumber_WithStandardRoom_ShouldGenerateCorrectly() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateRoomPhoneNumber("101");

        assertEquals("(555) 123-4101", result);
    }

    @Test
    void testGenerateRoomPhoneNumber_WithPrefixedRoom_ShouldExtractNumber() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateRoomPhoneNumber("CLS-105");

        assertEquals("(555) 123-4105", result);
    }

    @Test
    void testGenerateRoomPhoneNumber_WithAutoGenerateDisabled_ShouldReturnEmpty() {
        testSettings.setAutoGenerateRoomPhone(false);
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateRoomPhoneNumber("101");

        assertEquals("", result);
    }

    @Test
    void testGenerateRoomPhoneNumber_WithNullRoomNumber_ShouldReturnEmpty() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateRoomPhoneNumber(null);

        assertEquals("", result);
    }

    @Test
    void testGenerateRoomPhoneNumber_WithNonNumericRoom_ShouldReturnEmpty() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));

        String result = service.generateRoomPhoneNumber("GYM");

        assertEquals("", result);
    }

    @Test
    void testExtractRoomNumberValue_WithNumericRoom_ShouldExtract() {
        Integer result = service.extractRoomNumberValue("101");

        assertEquals(101, result);
    }

    @Test
    void testExtractRoomNumberValue_WithPrefixedRoom_ShouldExtractNumber() {
        Integer result = service.extractRoomNumberValue("CLS-105");

        assertEquals(105, result);
    }

    @Test
    void testExtractRoomNumberValue_WithNoNumber_ShouldReturnNull() {
        Integer result = service.extractRoomNumberValue("GYM");

        assertNull(result);
    }

    @Test
    void testExtractRoomNumberValue_WithNullRoom_ShouldReturnNull() {
        Integer result = service.extractRoomNumberValue(null);

        assertNull(result);
    }

    // ========== FORMAT VALIDATION TESTS ==========

    @Test
    void testIsValidEmailFormat_WithValidFormat_ShouldReturnTrue() {
        boolean result = service.isValidEmailFormat("{firstname}.{lastname}");

        assertTrue(result);
    }

    @Test
    void testIsValidEmailFormat_WithMultiplePlaceholders_ShouldReturnTrue() {
        boolean result = service.isValidEmailFormat("{lastname}_{firstname_initial}");

        assertTrue(result);
    }

    @Test
    void testIsValidEmailFormat_WithNoPlaceholders_ShouldReturnFalse() {
        boolean result = service.isValidEmailFormat("statictext");

        assertFalse(result);
    }

    @Test
    void testIsValidEmailFormat_WithNullFormat_ShouldReturnFalse() {
        boolean result = service.isValidEmailFormat(null);

        assertFalse(result);
    }

    @Test
    void testIsValidIdFormat_WithValidFormat_ShouldReturnTrue() {
        boolean result = service.isValidIdFormat("{grad_year}-{sequence}");

        assertTrue(result);
    }

    @Test
    void testIsValidIdFormat_WithNoPlaceholders_ShouldReturnFalse() {
        boolean result = service.isValidIdFormat("staticid");

        assertFalse(result);
    }

    @Test
    void testGetEmailFormatPlaceholders_ShouldReturnAllPlaceholders() {
        String[] placeholders = service.getEmailFormatPlaceholders();

        assertNotNull(placeholders);
        assertEquals(8, placeholders.length);
        assertTrue(containsPlaceholder(placeholders, "{firstname}"));
        assertTrue(containsPlaceholder(placeholders, "{lastname}"));
    }

    @Test
    void testGetIdFormatPlaceholders_ShouldReturnAllPlaceholders() {
        String[] placeholders = service.getIdFormatPlaceholders();

        assertNotNull(placeholders);
        assertEquals(4, placeholders.length);
        assertTrue(containsPlaceholder(placeholders, "{prefix}"));
        assertTrue(containsPlaceholder(placeholders, "{sequence}"));
    }

    // ========== NULL SAFETY TESTS ==========

    @Test
    void testGenerateTeacherEmail_WithNullFirstName_ShouldNotCrash() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(teacherRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateTeacherEmail(null, "Smith");

        assertNotNull(result);
        assertTrue(result.contains("@school.edu"));
    }

    @Test
    void testGenerateTeacherEmail_WithNullLastName_ShouldNotCrash() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(teacherRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateTeacherEmail("John", null);

        assertNotNull(result);
        assertTrue(result.contains("@school.edu"));
    }

    @Test
    void testGenerateStudentEmail_WithNullFirstName_ShouldNotCrash() {
        when(districtSettingsRepository.findFirstBy()).thenReturn(Optional.of(testSettings));
        when(studentRepository.existsByEmail(anyString())).thenReturn(false);

        String result = service.generateStudentEmail(null, "Johnson", 2025);

        assertNotNull(result);
        assertTrue(result.contains("@students.school.edu"));
    }

    @Test
    void testGenerateAlternativeEmail_WithNullEmail_ShouldReturnNull() {
        String result = service.generateAlternativeEmail(null, true);

        assertNull(result);
    }

    @Test
    void testGenerateAlternativeEmail_WithInvalidEmail_ShouldReturnAsIs() {
        String result = service.generateAlternativeEmail("notanemail", true);

        assertEquals("notanemail", result);
    }

    // ========== HELPER METHODS ==========

    private boolean containsPlaceholder(String[] array, String value) {
        for (String item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
