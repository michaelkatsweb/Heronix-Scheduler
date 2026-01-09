package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.domain.AttendanceRecord.AttendanceStatus;
import com.heronix.repository.*;
import com.heronix.service.impl.AttendanceService;
import com.heronix.service.impl.AttendanceService.AttendanceSummary;
import com.heronix.service.impl.AttendanceService.ChronicAbsenceAlert;
import com.heronix.service.impl.AttendanceService.DailyAttendanceReport;
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
 * Comprehensive test suite for AttendanceService
 * Tests null safety, attendance recording, summaries, and reporting
 *
 * Focus areas:
 * - Null safety for all public methods
 * - Single attendance recording
 * - Bulk attendance recording
 * - Attendance summaries and calculations
 * - Chronic absence detection
 * - Daily campus reports
 * - Edge cases (null dates, null statuses, empty lists, etc.)
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest extends BaseServiceTest {

    @Mock(lenient = true)
    private AttendanceRepository attendanceRepository;

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @InjectMocks
    private AttendanceService service;

    private Student testStudent;
    private LocalDate testDate;
    private AttendanceRecord testRecord;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = TestDataBuilder.aStudent()
                .withId(1L)
                .withFirstName("John")
                .withLastName("Doe")
                .withGradeLevel("10")
                .build();

        testDate = LocalDate.of(2025, 12, 17);

        // Create test attendance record
        testRecord = AttendanceRecord.builder()
                .id(1L)
                .student(testStudent)
                .attendanceDate(testDate)
                .status(AttendanceStatus.PRESENT)
                .recordedBy("teacher@school.edu")
                .build();
    }

    // ========================================================================
    // NULL SAFETY TESTS - recordAttendance()
    // ========================================================================

    @Test
    void testRecordAttendance_WithNullStudentId_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.recordAttendance(null, 1L, testDate, AttendanceStatus.PRESENT, "teacher@school.edu")
        );
    }

    @Test
    void testRecordAttendance_WithNullDate_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.recordAttendance(1L, 1L, null, AttendanceStatus.PRESENT, "teacher@school.edu")
        );
    }

    @Test
    void testRecordAttendance_WithNullStatus_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.recordAttendance(1L, 1L, testDate, null, "teacher@school.edu")
        );
    }

    @Test
    void testRecordAttendance_WithNullCourseId_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.recordAttendance(1L, null, testDate, AttendanceStatus.PRESENT, "teacher@school.edu")
        );
    }

    @Test
    void testRecordAttendance_WithNullRecordedBy_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.recordAttendance(1L, 1L, testDate, AttendanceStatus.PRESENT, null)
        );
    }

    @Test
    void testRecordAttendance_WhenStudentNotFound_ShouldThrowException() {
        // Given
        when(studentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            service.recordAttendance(999L, 1L, testDate, AttendanceStatus.PRESENT, "teacher@school.edu")
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - recordClassAttendance()
    // ========================================================================

    @Test
    void testRecordClassAttendance_WithNullDate_ShouldReturnEmptyList() {
        // Given
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        statuses.put(1L, AttendanceStatus.PRESENT);

        // When
        List<AttendanceRecord> result = service.recordClassAttendance(
            1L, null, 1, statuses, "teacher@school.edu");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRecordClassAttendance_WithNullStudentStatuses_ShouldReturnEmptyList() {
        // When
        List<AttendanceRecord> result = service.recordClassAttendance(
            1L, testDate, 1, null, "teacher@school.edu");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRecordClassAttendance_WithNullCourseId_ShouldNotThrowNPE() {
        // Given
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        statuses.put(1L, AttendanceStatus.PRESENT);

        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.recordClassAttendance(null, testDate, 1, statuses, "teacher@school.edu")
        );
    }

    @Test
    void testRecordClassAttendance_WithNullPeriodNumber_ShouldNotThrowNPE() {
        // Given
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        statuses.put(1L, AttendanceStatus.PRESENT);

        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.recordClassAttendance(1L, testDate, null, statuses, "teacher@school.edu")
        );
    }

    @Test
    void testRecordClassAttendance_WithNullRecordedBy_ShouldNotThrowNPE() {
        // Given
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        statuses.put(1L, AttendanceStatus.PRESENT);

        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.recordClassAttendance(1L, testDate, 1, statuses, null)
        );
    }

    @Test
    void testRecordClassAttendance_WithEmptyMap_ShouldReturnEmptyList() {
        // Given
        Map<Long, AttendanceStatus> statuses = new HashMap<>();

        // When
        List<AttendanceRecord> result = service.recordClassAttendance(
            1L, testDate, 1, statuses, "teacher@school.edu");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRecordClassAttendance_WithNullMapEntry_ShouldSkipNullEntry() {
        // Given
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        statuses.put(1L, AttendanceStatus.PRESENT);
        statuses.put(null, AttendanceStatus.ABSENT); // Null key

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When
        List<AttendanceRecord> result = service.recordClassAttendance(
            1L, testDate, 1, statuses, "teacher@school.edu");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Only the valid entry should be processed
    }

    // ========================================================================
    // NULL SAFETY TESTS - getStudentAttendanceSummary()
    // ========================================================================

    @Test
    void testGetStudentAttendanceSummary_WithNullStudentId_ShouldReturnEmptySummary() {
        // When
        AttendanceSummary result = service.getStudentAttendanceSummary(
            null, testDate, testDate.plusDays(7));

        // Then
        assertNotNull(result);
        assertNull(result.getStudentId());
    }

    @Test
    void testGetStudentAttendanceSummary_WithNullStartDate_ShouldReturnEmptySummary() {
        // When
        AttendanceSummary result = service.getStudentAttendanceSummary(
            1L, null, testDate.plusDays(7));

        // Then
        assertNotNull(result);
        assertNull(result.getStartDate());
    }

    @Test
    void testGetStudentAttendanceSummary_WithNullEndDate_ShouldReturnEmptySummary() {
        // When
        AttendanceSummary result = service.getStudentAttendanceSummary(
            1L, testDate, null);

        // Then
        assertNotNull(result);
        assertNull(result.getEndDate());
    }

    @Test
    void testGetStudentAttendanceSummary_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        // Given
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(anyLong(), any(), any()))
            .thenReturn(null);

        // When/Then: Should not throw
        assertDoesNotThrow(() -> {
            AttendanceSummary result = service.getStudentAttendanceSummary(
                1L, testDate, testDate.plusDays(7));
            assertNotNull(result);
            assertEquals(0, result.getTotalDays());
        });
    }

    @Test
    void testGetStudentAttendanceSummary_WithNullRecordsInList_ShouldFilterNulls() {
        // Given
        List<AttendanceRecord> records = new ArrayList<>();
        records.add(testRecord);
        records.add(null); // Null record in list
        records.add(AttendanceRecord.builder()
            .student(testStudent)
            .attendanceDate(testDate.plusDays(1))
            .status(AttendanceStatus.ABSENT)
            .build());

        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(anyLong(), any(), any()))
            .thenReturn(records);

        // When
        AttendanceSummary result = service.getStudentAttendanceSummary(
            1L, testDate, testDate.plusDays(7));

        // Then: Should handle null record gracefully
        assertNotNull(result);
        assertEquals(3, result.getTotalDays()); // Includes null in count but filters in streams
    }

    // ========================================================================
    // NULL SAFETY TESTS - getChronicAbsenceAlerts()
    // ========================================================================

    @Test
    void testGetChronicAbsenceAlerts_WithNullStartDate_ShouldNotThrowNPE() {
        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.getChronicAbsenceAlerts(null, testDate.plusDays(30), 5)
        );
    }

    @Test
    void testGetChronicAbsenceAlerts_WithNullEndDate_ShouldNotThrowNPE() {
        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.getChronicAbsenceAlerts(testDate, null, 5)
        );
    }

    @Test
    void testGetChronicAbsenceAlerts_WhenRepositoryReturnsNull_ShouldReturnEmptyList() {
        // Given
        when(attendanceRepository.findStudentsWithChronicAbsences(any(), any(), anyLong()))
            .thenReturn(null);

        // When
        List<ChronicAbsenceAlert> result = service.getChronicAbsenceAlerts(
            testDate, testDate.plusDays(30), 5);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChronicAbsenceAlerts_WithNullRowInResults_ShouldSkipNullRow() {
        // Given
        List<Object[]> results = new ArrayList<>();
        results.add(new Object[]{1L, 10L});
        results.add(null); // Null row

        when(attendanceRepository.findStudentsWithChronicAbsences(any(), any(), anyLong()))
            .thenReturn(results);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        // When
        List<ChronicAbsenceAlert> alerts = service.getChronicAbsenceAlerts(
            testDate, testDate.plusDays(30), 5);

        // Then: Should skip null row
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
    }

    @Test
    void testGetChronicAbsenceAlerts_WithNullStudentNames_ShouldUseUnknown() {
        // Given
        Student studentWithNullNames = TestDataBuilder.aStudent()
            .withId(2L)
            .withGradeLevel("10")
            .build();
        studentWithNullNames.setFirstName(null);
        studentWithNullNames.setLastName(null);

        List<Object[]> results = new ArrayList<>();
        results.add(new Object[]{2L, 10L});

        when(attendanceRepository.findStudentsWithChronicAbsences(any(), any(), anyLong()))
            .thenReturn(results);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(studentWithNullNames));

        // When
        List<ChronicAbsenceAlert> alerts = service.getChronicAbsenceAlerts(
            testDate, testDate.plusDays(30), 5);

        // Then
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals("Unknown", alerts.get(0).getStudentName());
    }

    // ========================================================================
    // NULL SAFETY TESTS - getDailyReport()
    // ========================================================================

    @Test
    void testGetDailyReport_WithNullDate_ShouldReturnReportWithNullDate() {
        // When
        DailyAttendanceReport result = service.getDailyReport(1L, null);

        // Then
        assertNotNull(result);
        assertNull(result.getDate());
    }

    @Test
    void testGetDailyReport_WithNullCampusId_ShouldNotThrowNPE() {
        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.getDailyReport(null, testDate)
        );
    }

    @Test
    void testGetDailyReport_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        // Given
        when(attendanceRepository.findByAttendanceDateAndCampusId(any(), any()))
            .thenReturn(null);

        // When
        DailyAttendanceReport result = service.getDailyReport(1L, testDate);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalStudents());
    }

    @Test
    void testGetDailyReport_WithNullRecordsInList_ShouldFilterNulls() {
        // Given
        List<AttendanceRecord> records = new ArrayList<>();
        records.add(testRecord);
        records.add(null); // Null record
        records.add(AttendanceRecord.builder()
            .student(testStudent)
            .attendanceDate(testDate)
            .status(AttendanceStatus.ABSENT)
            .build());

        when(attendanceRepository.findByAttendanceDateAndCampusId(any(), any()))
            .thenReturn(records);

        // When
        DailyAttendanceReport result = service.getDailyReport(1L, testDate);

        // Then: Should handle null records gracefully
        assertNotNull(result);
        assertEquals(3, result.getTotalStudents());
    }

    @Test
    void testGetDailyReport_WithNullStatusInRecords_ShouldFilterNulls() {
        // Given
        AttendanceRecord recordWithNullStatus = AttendanceRecord.builder()
            .student(testStudent)
            .attendanceDate(testDate)
            .status(null) // Null status
            .build();

        List<AttendanceRecord> records = new ArrayList<>();
        records.add(testRecord);
        records.add(recordWithNullStatus);

        when(attendanceRepository.findByAttendanceDateAndCampusId(any(), any()))
            .thenReturn(records);

        // When
        DailyAttendanceReport result = service.getDailyReport(1L, testDate);

        // Then: Should filter out records with null status in grouping
        assertNotNull(result);
        assertEquals(2, result.getTotalStudents());
        // Status breakdown should only include the valid status
        assertTrue(result.getStatusBreakdown().size() <= 1);
    }

    // ========================================================================
    // BUSINESS LOGIC TESTS
    // ========================================================================

    @Test
    void testRecordAttendance_WithValidData_ShouldCreateRecord() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When
        AttendanceRecord result = service.recordAttendance(
            1L, 1L, testDate, AttendanceStatus.PRESENT, "teacher@school.edu");

        // Then
        assertNotNull(result);
        verify(attendanceRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void testRecordClassAttendance_WithValidData_ShouldCreateMultipleRecords() {
        // Given
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        statuses.put(1L, AttendanceStatus.PRESENT);
        statuses.put(2L, AttendanceStatus.ABSENT);

        Student student2 = TestDataBuilder.aStudent()
            .withId(2L)
            .withFirstName("Jane")
            .withLastName("Smith")
            .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(testRecord);

        // When
        List<AttendanceRecord> result = service.recordClassAttendance(
            1L, testDate, 1, statuses, "teacher@school.edu");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(attendanceRepository, times(2)).save(any(AttendanceRecord.class));
    }

    @Test
    void testGetStudentAttendanceSummary_WithValidData_ShouldCalculateCorrectly() {
        // Given
        List<AttendanceRecord> records = Arrays.asList(
            createRecord(testStudent, testDate, AttendanceStatus.PRESENT),
            createRecord(testStudent, testDate.plusDays(1), AttendanceStatus.PRESENT),
            createRecord(testStudent, testDate.plusDays(2), AttendanceStatus.ABSENT),
            createRecord(testStudent, testDate.plusDays(3), AttendanceStatus.TARDY)
        );

        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(anyLong(), any(), any()))
            .thenReturn(records);

        // When
        AttendanceSummary result = service.getStudentAttendanceSummary(
            1L, testDate, testDate.plusDays(7));

        // Then
        assertNotNull(result);
        assertEquals(4, result.getTotalDays());
        assertEquals(2, result.getDaysPresent());
        assertEquals(1, result.getDaysAbsent());
        assertEquals(1, result.getDaysTardy());
        assertEquals(50.0, result.getAttendanceRate(), 0.01);
        assertTrue(result.isChronicAbsent()); // <90% attendance
    }

    @Test
    void testGetChronicAbsenceAlerts_WithValidData_ShouldReturnAlerts() {
        // Given
        List<Object[]> results = new ArrayList<>();
        results.add(new Object[]{1L, 15L}); // 15 absences

        when(attendanceRepository.findStudentsWithChronicAbsences(any(), any(), anyLong()))
            .thenReturn(results);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        // When
        List<ChronicAbsenceAlert> alerts = service.getChronicAbsenceAlerts(
            testDate, testDate.plusDays(30), 5);

        // Then
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals(15, alerts.get(0).getTotalAbsences());
        assertEquals("CRITICAL", alerts.get(0).getAlertLevel()); // >10 (threshold * 2)
    }

    @Test
    void testGetDailyReport_WithValidData_ShouldCalculateCorrectly() {
        // Given
        List<AttendanceRecord> records = Arrays.asList(
            createRecord(testStudent, testDate, AttendanceStatus.PRESENT),
            createRecord(testStudent, testDate, AttendanceStatus.PRESENT),
            createRecord(testStudent, testDate, AttendanceStatus.ABSENT),
            createRecord(testStudent, testDate, AttendanceStatus.TARDY)
        );

        when(attendanceRepository.findByAttendanceDateAndCampusId(any(), any()))
            .thenReturn(records);

        // When
        DailyAttendanceReport result = service.getDailyReport(1L, testDate);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getTotalStudents());
        assertEquals(2, result.getStudentsPresent());
        assertEquals(1, result.getStudentsAbsent());
        assertEquals(50.0, result.getAttendanceRate(), 0.01);
    }

    // Helper method
    private AttendanceRecord createRecord(Student student, LocalDate date, AttendanceStatus status) {
        return AttendanceRecord.builder()
            .student(student)
            .attendanceDate(date)
            .status(status)
            .build();
    }
}
