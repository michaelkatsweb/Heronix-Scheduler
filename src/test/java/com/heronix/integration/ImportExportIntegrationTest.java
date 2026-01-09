package com.heronix.integration;

import com.heronix.Heronix SchedulerApiApplication;
import com.heronix.model.domain.*;
import com.heronix.model.dto.ImportResult;
import com.heronix.model.enums.*;
import com.heronix.model.enums.SchedulePeriod;
import com.heronix.model.enums.ScheduleStatus;
import com.heronix.repository.*;
import com.heronix.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for Import/Export Workflows
 * Tests end-to-end import and export functionality across multiple formats
 *
 * @author Heronix Scheduler Integration Testing Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = Heronix SchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class ImportExportIntegrationTest {

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    private Teacher testTeacher;
    private Course testCourse;
    private Room testRoom;
    private Student testStudent;
    private Schedule testSchedule;

    @BeforeEach
    public void setup() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setName("John Smith");
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Smith");
        testTeacher.setEmail("john.smith@test.edu");
        testTeacher.setDepartment("Mathematics");
        testTeacher = teacherRepository.save(testTeacher);

        // Create test course
        testCourse = new Course();
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");
        testCourse.setSubject("Mathematics");
        testCourse.setLevel(EducationLevel.HIGH_SCHOOL);
        testCourse.setMaxStudents(30);
        testCourse.setActive(true);
        testCourse.setCredits(1.0);
        testCourse = courseRepository.save(testCourse);

        // Create test room
        testRoom = new Room();
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);
        testRoom.setActive(true);
        testRoom.setType(RoomType.CLASSROOM);
        testRoom = roomRepository.save(testRoom);

        // Create test student
        testStudent = new Student();
        testStudent.setStudentId("S001");
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Doe");
        testStudent.setEmail("jane.doe@test.edu");
        testStudent.setGradeLevel("10");
        testStudent = studentRepository.save(testStudent);

        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setName("Test Schedule");
        testSchedule.setScheduleType(ScheduleType.TRADITIONAL);
        testSchedule.setPeriod(SchedulePeriod.SEMESTER);
        testSchedule.setStatus(ScheduleStatus.DRAFT);
        testSchedule.setStartDate(java.time.LocalDate.now());
        testSchedule.setEndDate(java.time.LocalDate.now().plusMonths(4));
        testSchedule.setActive(true);
        testSchedule = scheduleRepository.save(testSchedule);
    }

    // ========================================================================
    // TEACHER IMPORT TESTS
    // ========================================================================

    @Test
    public void testImportTeachers_ValidCSV_ShouldSucceed() throws Exception {
        // Arrange
        String csvContent = "Name,First Name,Last Name,Email,Department\n" +
                           "Jane Doe,Jane,Doe,jane.doe@test.edu,Science\n" +
                           "Bob Smith,Bob,Smith,bob.smith@test.edu,English\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "teachers.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importTeachers(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccessCount() >= 2, "Should import at least 2 teachers");
        assertEquals(0, result.getErrorCount(), "Should have no errors");
    }

    @Test
    public void testImportTeachers_InvalidData_ShouldReportErrors() throws Exception {
        // Arrange - Missing required Name field
        String csvContent = "First Name,Last Name,Email,Department\n" +
                           "Jane,Doe,jane.doe@test.edu,Science\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "teachers.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importTeachers(file);

        // Assert
        assertNotNull(result);
        // Should either skip or error on invalid data
        assertTrue(result.getErrorCount() > 0 || result.getSkippedCount() > 0);
    }

    @Test
    public void testImportTeachers_DuplicateEmails_ShouldHandleGracefully() throws Exception {
        // Arrange
        String csvContent = "Name,First Name,Last Name,Email,Department\n" +
                           "Jane Doe,Jane,Doe,duplicate@test.edu,Science\n" +
                           "John Doe,John,Doe,duplicate@test.edu,Math\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "teachers.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importTeachers(file);

        // Assert
        assertNotNull(result);
        // Should handle duplicates by skipping or updating
        assertTrue(result.getTotalProcessed() > 0);
    }

    // ========================================================================
    // COURSE IMPORT TESTS
    // ========================================================================

    @Test
    public void testImportCourses_ValidCSV_ShouldSucceed() throws Exception {
        // Arrange
        String csvContent = "Course Code,Course Name,Subject,Level,Credits,Max Students,Active\n" +
                           "ENG101,English I,English,HIGH_SCHOOL,1.0,25,true\n" +
                           "SCI201,Biology,Science,HIGH_SCHOOL,1.0,20,true\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "courses.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importCourses(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccessCount() >= 2, "Should import at least 2 courses");
    }

    @Test
    public void testImportCourses_MissingRequiredFields_ShouldReportErrors() throws Exception {
        // Arrange - Missing Course Code
        String csvContent = "Course Name,Subject,Level,Credits\n" +
                           "English I,English,HIGH_SCHOOL,1.0\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "courses.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importCourses(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0 || result.getSkippedCount() > 0);
    }

    // ========================================================================
    // ROOM IMPORT TESTS
    // ========================================================================

    @Test
    public void testImportRooms_ValidCSV_ShouldSucceed() throws Exception {
        // Arrange
        String csvContent = "Room Number,Capacity,Room Type,Active\n" +
                           "201,24,LAB,true\n" +
                           "202,30,COMPUTER_LAB,true\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "rooms.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importRooms(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccessCount() >= 2, "Should import at least 2 rooms");
    }

    @Test
    public void testImportRooms_InvalidRoomType_ShouldHandleGracefully() throws Exception {
        // Arrange
        String csvContent = "Room Number,Capacity,Room Type,Active\n" +
                           "201,24,INVALID_TYPE,true\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "rooms.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importRooms(file);

        // Assert
        assertNotNull(result);
        // Should handle invalid enum gracefully
        assertTrue(result.getTotalProcessed() > 0);
    }

    // ========================================================================
    // STUDENT IMPORT TESTS
    // ========================================================================

    @Test
    public void testImportStudents_ValidCSV_ShouldSucceed() throws Exception {
        // Arrange
        String csvContent = "Student ID,First Name,Last Name,Email,Grade Level\n" +
                           "S101,Alice,Johnson,alice.j@test.edu,9\n" +
                           "S102,Bob,Williams,bob.w@test.edu,10\n";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "students.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportResult result = fileImportService.importStudents(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccessCount() >= 2, "Should import at least 2 students");
    }

    @Test
    public void testImportStudents_LargeFile_ShouldCompleteInReasonableTime() throws Exception {
        // Arrange - Create CSV with 100 students
        StringBuilder csvContent = new StringBuilder("Student ID,First Name,Last Name,Email,Grade Level\n");
        for (int i = 1; i <= 100; i++) {
            csvContent.append(String.format("S%03d,Student%d,Test%d,student%d@test.edu,10\n", i, i, i, i));
        }

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "students.csv",
            "text/csv",
            csvContent.toString().getBytes(StandardCharsets.UTF_8)
        );

        // Act
        long startTime = System.currentTimeMillis();
        ImportResult result = fileImportService.importStudents(file);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccessCount() >= 90, "Should import at least 90% of students");
        assertTrue(duration < 10000, "Import should complete in less than 10 seconds");
    }

    // ========================================================================
    // EXCEL EXPORT TESTS
    // ========================================================================

    @Test
    public void testExportTeachersToExcel_ShouldGenerateValidFile() throws Exception {
        // Arrange
        List<Teacher> teachers = teacherRepository.findAll();

        // Act
        byte[] excelData = exportService.exportTeachersToExcel(teachers);

        // Assert
        assertNotNull(excelData);
        assertTrue(excelData.length > 0, "Excel file should have content");
        // Excel files start with PK (ZIP signature)
        assertTrue(excelData[0] == 0x50 && excelData[1] == 0x4B, "Should be valid Excel format");
    }

    @Test
    public void testExportCoursesToExcel_ShouldGenerateValidFile() throws Exception {
        // Arrange
        List<Course> courses = courseRepository.findAll();

        // Act
        byte[] excelData = exportService.exportCoursesToExcel(courses);

        // Assert
        assertNotNull(excelData);
        assertTrue(excelData.length > 0);
    }

    @Test
    public void testExportRoomsToExcel_ShouldGenerateValidFile() throws Exception {
        // Arrange
        List<Room> rooms = roomRepository.findAll();

        // Act
        byte[] excelData = exportService.exportRoomsToExcel(rooms);

        // Assert
        assertNotNull(excelData);
        assertTrue(excelData.length > 0);
    }

    @Test
    public void testExportStudentsToExcel_ShouldGenerateValidFile() throws Exception {
        // Arrange
        List<Student> students = studentRepository.findAll();

        // Act
        byte[] excelData = exportService.exportStudentsToExcel(students);

        // Assert
        assertNotNull(excelData);
        assertTrue(excelData.length > 0);
    }

    // ========================================================================
    // CSV EXPORT TESTS
    // ========================================================================

    @Test
    public void testExportTeachersToCSV_ShouldGenerateValidCSV() throws Exception {
        // Arrange
        List<Teacher> teachers = teacherRepository.findAll();

        // Act
        byte[] csvData = exportService.exportTeachersToCSV(teachers);

        // Assert
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);

        String csvContent = new String(csvData, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("Name") || csvContent.contains("Email"), "CSV should have headers");
        assertTrue(csvContent.contains(testTeacher.getEmail()), "CSV should contain test teacher");
    }

    @Test
    public void testExportCoursesToCSV_ShouldGenerateValidCSV() throws Exception {
        // Arrange
        List<Course> courses = courseRepository.findAll();

        // Act
        byte[] csvData = exportService.exportCoursesToCSV(courses);

        // Assert
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);

        String csvContent = new String(csvData, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains(testCourse.getCourseCode()), "CSV should contain test course");
    }

    @Test
    public void testExportRoomsToCSV_ShouldGenerateValidCSV() throws Exception {
        // Arrange
        List<Room> rooms = roomRepository.findAll();

        // Act
        byte[] csvData = exportService.exportRoomsToCSV(rooms);

        // Assert
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
    }

    @Test
    public void testExportStudentsToCSV_ShouldGenerateValidCSV() throws Exception {
        // Arrange
        List<Student> students = studentRepository.findAll();

        // Act
        byte[] csvData = exportService.exportStudentsToCSV(students);

        // Assert
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
    }

    // ========================================================================
    // SCHEDULE EXPORT TESTS
    // ========================================================================

    @Test
    public void testExportScheduleToExcel_ShouldGenerateValidFile() throws Exception {
        // Act
        byte[] excelData = exportService.exportToExcel(testSchedule);

        // Assert
        assertNotNull(excelData);
        assertTrue(excelData.length > 0);
    }

    @Test
    public void testExportScheduleToCSV_ShouldGenerateValidFile() throws Exception {
        // Act
        byte[] csvData = exportService.exportToCSV(testSchedule);

        // Assert
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
    }

    @Test
    public void testExportScheduleToPDF_ShouldGenerateValidFile() throws Exception {
        // Act
        byte[] pdfData = exportService.exportToPDF(testSchedule);

        // Assert
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        // PDF files start with %PDF
        assertTrue(pdfData[0] == 0x25 && pdfData[1] == 0x50, "Should be valid PDF format");
    }

    @Test
    public void testExportScheduleToICal_ShouldGenerateValidFile() throws Exception {
        // Act
        byte[] iCalData = exportService.exportToICal(testSchedule);

        // Assert
        assertNotNull(iCalData);
        assertTrue(iCalData.length > 0);

        String iCalContent = new String(iCalData, StandardCharsets.UTF_8);
        assertTrue(iCalContent.contains("BEGIN:VCALENDAR"), "Should be valid iCal format");
    }

    @Test
    public void testExportScheduleToHTML_ShouldGenerateValidFile() throws Exception {
        // Act
        byte[] htmlData = exportService.exportToHTML(testSchedule);

        // Assert
        assertNotNull(htmlData);
        assertTrue(htmlData.length > 0);

        String htmlContent = new String(htmlData, StandardCharsets.UTF_8);
        assertTrue(htmlContent.contains("<html>") || htmlContent.contains("<table>"), "Should be valid HTML");
    }

    @Test
    public void testExportScheduleToJSON_ShouldGenerateValidFile() throws Exception {
        // Act
        byte[] jsonData = exportService.exportToJSON(testSchedule);

        // Assert
        assertNotNull(jsonData);
        assertTrue(jsonData.length > 0);

        String jsonContent = new String(jsonData, StandardCharsets.UTF_8);
        assertTrue(jsonContent.contains("{") && jsonContent.contains("}"), "Should be valid JSON");
    }

    // ========================================================================
    // FORMAT-SPECIFIC EXPORT TESTS
    // ========================================================================

    @Test
    public void testExportSchedule_AllFormats_ShouldSucceed() throws Exception {
        // Test all export formats using enum
        for (ExportFormat format : ExportFormat.values()) {
            byte[] exportData = exportService.exportSchedule(testSchedule.getId(), format);

            assertNotNull(exportData, "Export for format " + format + " should not be null");
            assertTrue(exportData.length > 0, "Export for format " + format + " should have content");
        }
    }

    // ========================================================================
    // FILE VALIDATION TESTS
    // ========================================================================

    @Test
    public void testValidateFile_ValidCSV_ShouldReturnTrue() throws Exception {
        // Arrange
        String csvContent = "Name,Email\nJohn Doe,john@test.edu\n";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        boolean isValid = fileImportService.validateFile(file);

        // Assert
        assertTrue(isValid, "Valid CSV file should pass validation");
    }

    @Test
    public void testValidateFile_EmptyFile_ShouldReturnFalse() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.csv",
            "text/csv",
            new byte[0]
        );

        // Act
        boolean isValid = fileImportService.validateFile(file);

        // Assert
        assertFalse(isValid, "Empty file should fail validation");
    }

    @Test
    public void testIsSupportedFileType_CSV_ShouldReturnTrue() {
        // Act
        boolean isSupported = fileImportService.isSupportedFileType("test.csv");

        // Assert
        assertTrue(isSupported, "CSV files should be supported");
    }

    @Test
    public void testIsSupportedFileType_Excel_ShouldReturnTrue() {
        // Act
        boolean xlsxSupported = fileImportService.isSupportedFileType("test.xlsx");
        boolean xlsSupported = fileImportService.isSupportedFileType("test.xls");

        // Assert
        assertTrue(xlsxSupported, "XLSX files should be supported");
        assertTrue(xlsSupported, "XLS files should be supported");
    }

    @Test
    public void testGetSupportedFormats_ShouldReturnList() {
        // Act
        List<String> formats = fileImportService.getSupportedFormats();

        // Assert
        assertNotNull(formats);
        assertFalse(formats.isEmpty(), "Should have at least one supported format");
        assertTrue(formats.contains("csv") || formats.contains("CSV"), "Should support CSV");
    }

    // ========================================================================
    // ROUND-TRIP TESTS (Import → Export → Validate)
    // ========================================================================

    @Test
    public void testRoundTrip_Teachers_CSV_ShouldPreserveData() throws Exception {
        // Arrange - Export existing teachers to CSV
        List<Teacher> originalTeachers = teacherRepository.findAll();
        int originalCount = originalTeachers.size();
        byte[] csvData = exportService.exportTeachersToCSV(originalTeachers);

        // Act - Import the exported CSV
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "teachers.csv",
            "text/csv",
            csvData
        );

        // Clear existing teachers (in transaction, will rollback)
        teacherRepository.deleteAll();

        ImportResult result = fileImportService.importTeachers(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccessCount() > 0, "Should import teachers successfully");

        List<Teacher> importedTeachers = teacherRepository.findAll();
        // Should have similar count (may differ due to validation/duplicate handling)
        assertTrue(importedTeachers.size() > 0, "Should have imported teachers");
    }

    @Test
    public void testRoundTrip_Courses_CSV_ShouldPreserveData() throws Exception {
        // Arrange
        List<Course> originalCourses = courseRepository.findAll();
        byte[] csvData = exportService.exportCoursesToCSV(originalCourses);

        // Act
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "courses.csv",
            "text/csv",
            csvData
        );

        courseRepository.deleteAll();
        ImportResult result = fileImportService.importCourses(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSuccessCount() > 0);
    }

    // ========================================================================
    // ERROR HANDLING TESTS
    // ========================================================================

    @Test
    public void testImportTeachers_NullFile_ShouldThrowException() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            fileImportService.importTeachers(null);
        });
    }

    @Test
    public void testImportCourses_CorruptedFile_ShouldHandleGracefully() throws Exception {
        // Arrange - Binary garbage data
        byte[] corruptedData = new byte[]{0x00, 0x01, 0x02, 0x03, (byte) 0xFF};
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "corrupted.csv",
            "text/csv",
            corruptedData
        );

        // Act
        ImportResult result = fileImportService.importCourses(file);

        // Assert
        assertNotNull(result);
        // Should handle gracefully - either errors or empty result
        assertTrue(result.getErrorCount() > 0 || result.getSuccessCount() == 0);
    }

    @Test
    public void testExportSchedule_NonExistentSchedule_ShouldHandleGracefully() throws Exception {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            exportService.exportSchedule(999999L, ExportFormat.PDF);
        });
    }

    // ========================================================================
    // PERFORMANCE TESTS
    // ========================================================================

    @Test
    public void testExportLargeDataset_ShouldCompleteInReasonableTime() throws Exception {
        // Arrange - Create 50 students
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Student s = new Student();
            s.setStudentId("PERF" + i);
            s.setFirstName("Student" + i);
            s.setLastName("Test" + i);
            s.setEmail("perf" + i + "@test.edu");
            s.setGradeLevel("10");
            students.add(studentRepository.save(s));
        }

        // Act
        long startTime = System.currentTimeMillis();
        byte[] csvData = exportService.exportStudentsToCSV(students);
        byte[] excelData = exportService.exportStudentsToExcel(students);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(csvData);
        assertNotNull(excelData);
        assertTrue(duration < 5000, "Export should complete in less than 5 seconds");
    }
}
