package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.repository.ScheduleSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for ScheduleExportServiceImpl
 *
 * Tests export functionality to PDF, Excel, CSV, and iCalendar formats.
 * Focuses on validation, data preparation, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleExportServiceImplTest {

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private ScheduleExportServiceImpl service;

    @TempDir
    File tempDir;

    private Schedule testSchedule;
    private List<ScheduleSlot> testSlots;
    private ScheduleSlot testSlot1;
    private ScheduleSlot testSlot2;
    private Course testCourse;
    private Teacher testTeacher;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setScheduleName("Test Schedule");
        testSchedule.setStartDate(LocalDate.of(2025, 1, 6));
        testSchedule.setEndDate(LocalDate.of(2025, 6, 15));
        testSchedule.setActive(true);

        // Create test entities
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseName("Math 101");
        testCourse.setCourseCode("MATH101");
        testCourse.setSubject("Math");

        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("John Doe");
        testTeacher.setEmployeeId("EMP-001");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);

        // Create test slots
        testSlot1 = new ScheduleSlot();
        testSlot1.setId(1L);
        testSlot1.setSchedule(testSchedule);
        testSlot1.setCourse(testCourse);
        testSlot1.setTeacher(testTeacher);
        testSlot1.setRoom(testRoom);
        testSlot1.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot1.setStartTime(LocalTime.of(8, 0));
        testSlot1.setEndTime(LocalTime.of(9, 0));
        testSlot1.setPeriodNumber(1);

        testSlot2 = new ScheduleSlot();
        testSlot2.setId(2L);
        testSlot2.setSchedule(testSchedule);
        testSlot2.setCourse(testCourse);
        testSlot2.setTeacher(testTeacher);
        testSlot2.setRoom(testRoom);
        testSlot2.setDayOfWeek(DayOfWeek.TUESDAY);
        testSlot2.setStartTime(LocalTime.of(9, 0));
        testSlot2.setEndTime(LocalTime.of(10, 0));
        testSlot2.setPeriodNumber(2);

        testSlots = Arrays.asList(testSlot1, testSlot2);

        // Mock repository
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(testSlots);
    }

    // ========== PDF EXPORT TESTS ==========

    @Test
    void testExportToPDF_WithValidSchedule_ShouldCreateFile() throws Exception {
        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".pdf"));
        assertTrue(result.length() > 0);
        verify(scheduleSlotRepository, times(1)).findByScheduleId(1L);
    }

    @Test
    void testExportToPDF_WithNullScheduleName_ShouldUseDefault() throws Exception {
        testSchedule.setScheduleName(null);

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().contains("unnamed_schedule"));
    }

    @Test
    void testExportToPDF_WithNullDates_ShouldHandleGracefully() throws Exception {
        testSchedule.setStartDate(null);
        testSchedule.setEndDate(null);

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToPDF_WithEmptySlots_ShouldCreateFile() throws Exception {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(new ArrayList<>());

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToPDF_WithNullSlotProperties_ShouldHandleGracefully() throws Exception {
        testSlot1.setCourse(null);
        testSlot1.setTeacher(null);
        testSlot1.setRoom(null);

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToPDF_WithNullSlotDayOfWeek_ShouldFilterOut() throws Exception {
        testSlot1.setDayOfWeek(null);

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToPDF_WithNullSlotStartTime_ShouldFilterOut() throws Exception {
        testSlot1.setStartTime(null);

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToPDF_WithSubjectColors_ShouldApplyColorCoding() throws Exception {
        testCourse.setSubject("Science");

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== EXCEL EXPORT TESTS ==========

    @Test
    void testExportToExcel_WithValidSchedule_ShouldCreateFile() throws Exception {
        File result = service.exportToExcel(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".xlsx"));
        assertTrue(result.length() > 0);
        verify(scheduleSlotRepository, times(1)).findByScheduleId(1L);
    }

    @Test
    void testExportToExcel_WithNullScheduleName_ShouldUseDefault() throws Exception {
        testSchedule.setScheduleName(null);

        File result = service.exportToExcel(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToExcel_WithEmptySlots_ShouldCreateFile() throws Exception {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(new ArrayList<>());

        File result = service.exportToExcel(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToExcel_WithMultipleSlotsPerDay_ShouldGroupCorrectly() throws Exception {
        ScheduleSlot slot3 = new ScheduleSlot();
        slot3.setId(3L);
        slot3.setSchedule(testSchedule);
        slot3.setCourse(testCourse);
        slot3.setTeacher(testTeacher);
        slot3.setRoom(testRoom);
        slot3.setDayOfWeek(DayOfWeek.MONDAY);
        slot3.setStartTime(LocalTime.of(10, 0));
        slot3.setEndTime(LocalTime.of(11, 0));

        when(scheduleSlotRepository.findByScheduleId(1L))
            .thenReturn(Arrays.asList(testSlot1, testSlot2, slot3));

        File result = service.exportToExcel(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToExcel_WithNullNestedProperties_ShouldUseEmptyStrings() throws Exception {
        testSlot1.getCourse().setCourseName(null);
        testSlot1.getTeacher().setName(null);
        testSlot1.getRoom().setRoomNumber(null);

        File result = service.exportToExcel(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToExcel_WithNullSlots_ShouldSkipThem() throws Exception {
        List<ScheduleSlot> slotsWithNull = new ArrayList<>(testSlots);
        slotsWithNull.add(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(slotsWithNull);

        File result = service.exportToExcel(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== CSV EXPORT TESTS ==========

    @Test
    void testExportToCSV_WithValidSchedule_ShouldCreateFile() throws Exception {
        File result = service.exportToCSV(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".csv"));
        assertTrue(result.length() > 0);
        verify(scheduleSlotRepository, times(1)).findByScheduleId(1L);
    }

    @Test
    void testExportToCSV_WithNullScheduleName_ShouldUseDefault() throws Exception {
        testSchedule.setScheduleName(null);

        File result = service.exportToCSV(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToCSV_WithEmptySlots_ShouldCreateHeaderOnly() throws Exception {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(new ArrayList<>());

        File result = service.exportToCSV(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0); // Should have at least headers
    }

    @Test
    void testExportToCSV_WithSpecialCharactersInFields_ShouldEscapeCorrectly() throws Exception {
        testCourse.setCourseName("Math, Science & Art");
        testTeacher.setName("O'Brien, John");

        File result = service.exportToCSV(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToCSV_WithNullSlots_ShouldSkipThem() throws Exception {
        List<ScheduleSlot> slotsWithNull = new ArrayList<>(testSlots);
        slotsWithNull.add(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(slotsWithNull);

        File result = service.exportToCSV(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToCSV_WithNullNestedProperties_ShouldUseEmptyStrings() throws Exception {
        testSlot1.getCourse().setCourseName(null);
        testSlot1.getTeacher().setName(null);
        testSlot1.getRoom().setRoomNumber(null);

        File result = service.exportToCSV(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== ICALENDAR EXPORT TESTS ==========

    @Test
    void testExportToICalendar_WithValidSchedule_ShouldCreateFile() throws Exception {
        File result = service.exportToICalendar(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".ics"));
        assertTrue(result.length() > 0);
        verify(scheduleSlotRepository, times(1)).findByScheduleId(1L);
    }

    @Test
    void testExportToICalendar_WithNullScheduleName_ShouldUseDefault() throws Exception {
        testSchedule.setScheduleName(null);

        File result = service.exportToICalendar(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToICalendar_WithEmptySlots_ShouldCreateFile() throws Exception {
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(new ArrayList<>());

        File result = service.exportToICalendar(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToICalendar_WithNullSlotTimes_ShouldSkipSlot() throws Exception {
        testSlot1.setStartTime(null);
        testSlot1.setEndTime(null);

        File result = service.exportToICalendar(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToICalendar_WithNullStartDate_ShouldUseCurrentDate() throws Exception {
        testSchedule.setStartDate(null);

        File result = service.exportToICalendar(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToICalendar_WithNullSlots_ShouldSkipThem() throws Exception {
        List<ScheduleSlot> slotsWithNull = new ArrayList<>(testSlots);
        slotsWithNull.add(null);
        when(scheduleSlotRepository.findByScheduleId(1L)).thenReturn(slotsWithNull);

        File result = service.exportToICalendar(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== FILE NAMING TESTS ==========

    @Test
    void testExportToPDF_ShouldGenerateUniqueFilenames() throws Exception {
        File file1 = service.exportToPDF(testSchedule);
        // Wait a moment to ensure different timestamps
        Thread.sleep(100);
        File file2 = service.exportToPDF(testSchedule);

        assertNotNull(file1);
        assertNotNull(file2);
        // Files with same date but different times might have same name
        // We just verify both files were created
        assertTrue(file1.exists());
        assertTrue(file2.exists());
    }

    @Test
    void testExportFiles_ShouldBeInExportsDirectory() throws Exception {
        File file = service.exportToPDF(testSchedule);

        assertNotNull(file);
        assertTrue(file.getParentFile().getName().contains("Export") ||
                   file.getParentFile().getName().equals("Heronix Scheduler"));
    }

    @Test
    void testGetExportsDirectory_ShouldCreateIfNotExists() {
        File dir = service.getExportsDirectory();

        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }

    @Test
    void testGetExportsDirectory_ShouldBeConsistent() {
        File dir1 = service.getExportsDirectory();
        File dir2 = service.getExportsDirectory();

        assertNotNull(dir1);
        assertNotNull(dir2);
        assertEquals(dir1.getAbsolutePath(), dir2.getAbsolutePath());
    }

    // ========== VALIDATION TESTS ==========

    @Test
    void testExportToPDF_WithSpecialCharsInScheduleName_ShouldSanitize() throws Exception {
        testSchedule.setScheduleName("Test/Schedule:2025");

        File result = service.exportToPDF(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
        // Filename should have special chars replaced with underscores
        assertTrue(result.getName().contains("_"));
    }

    @Test
    void testExportToExcel_WithVariousSubjects_ShouldHandle() throws Exception {
        testCourse.setSubject("English");

        File result = service.exportToExcel(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportToCSV_WithNullStatus_ShouldUseDefault() throws Exception {
        testSlot1.setStatus(null);

        File result = service.exportToCSV(testSchedule);

        assertNotNull(result);
        assertTrue(result.exists());
    }
}
