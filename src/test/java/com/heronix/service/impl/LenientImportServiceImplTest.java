package com.heronix.service.impl;

import com.heronix.exception.ImportException;
import com.heronix.model.domain.*;
import com.heronix.model.dto.ImportResult;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for LenientImportServiceImpl
 *
 * Tests lenient data import with smart column mapping, auto-generation
 * of missing fields, and duplicate detection.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LenientImportServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MultipartFile mockFile;

    @Spy
    private LenientImportServiceImpl service = new LenientImportServiceImpl(
        null, null, null, null
    );

    private Room testRoom;
    private Teacher testTeacher;
    private Student testStudent;
    private Course testCourse;

    @BeforeEach
    void setUp() throws Exception {
        // Manually inject repositories (since we're using @Spy)
        service = new LenientImportServiceImpl(
            roomRepository,
            teacherRepository,
            studentRepository,
            courseRepository
        );

        // Use reflection to set private self field for transactional methods
        var selfField = LenientImportServiceImpl.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(service, service);

        // Create test entities
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);
        testRoom.setType(RoomType.CLASSROOM);

        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("John Doe");
        testTeacher.setEmployeeId("EMP-001");
        testTeacher.setDepartment("Math");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("STU-001");
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Smith");
        testStudent.setGradeLevel("9");

        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");
        testCourse.setMaxStudents(30);

        // Mock repositories
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        // Default: no duplicates
        when(roomRepository.findByRoomNumber(anyString())).thenReturn(Optional.empty());
        when(teacherRepository.findByEmployeeId(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByStudentId(anyString())).thenReturn(Optional.empty());
        when(courseRepository.findByCourseCode(anyString())).thenReturn(Optional.empty());
    }

    // ========== NULL SAFETY TESTS ==========

    @Test
    void testImportRoomsLenient_WithNullFile_ShouldThrowException() {
        assertThrows(ImportException.class, () -> service.importRoomsLenient(null));
    }

    @Test
    void testImportTeachersLenient_WithNullFile_ShouldThrowException() {
        assertThrows(ImportException.class, () -> service.importTeachersLenient(null));
    }

    @Test
    void testImportStudentsLenient_WithNullFile_ShouldThrowException() {
        assertThrows(ImportException.class, () -> service.importStudentsLenient(null));
    }

    @Test
    void testImportCoursesLenient_WithNullFile_ShouldThrowException() {
        assertThrows(ImportException.class, () -> service.importCoursesLenient(null));
    }

    // ========== CSV IMPORT TESTS ==========

    @Test
    void testImportStudentsLenient_WithValidCSV_ShouldImportSuccessfully() throws Exception {
        String csvContent = """
                StudentID,FirstName,LastName,GradeLevel,Email
                STU-001,John,Doe,9,john@example.com
                STU-002,Jane,Smith,10,jane@example.com
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        verify(studentRepository, times(2)).save(any(Student.class));
    }

    @Test
    void testImportStudentsLenient_WithMissingFields_ShouldAutoGenerate() throws Exception {
        String csvContent = """
                FirstName
                John
                Jane
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertTrue(result.getWarningCount() > 0); // Should have warnings for missing fields
    }

    @Test
    void testImportStudentsLenient_WithDuplicateStudentId_ShouldRecordError() throws Exception {
        String csvContent = """
                StudentID,FirstName,LastName,GradeLevel
                STU-001,John,Doe,9
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        when(studentRepository.findByStudentId("STU-001")).thenReturn(Optional.of(testStudent));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    void testImportTeachersLenient_WithValidCSV_ShouldImportSuccessfully() throws Exception {
        String csvContent = """
                Name,EmployeeID,Department
                John Doe,EMP-001,Math
                Jane Smith,EMP-002,Science
                """;

        when(mockFile.getOriginalFilename()).thenReturn("teachers.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importTeachersLenient(mockFile);

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        verify(teacherRepository, times(2)).save(any(Teacher.class));
    }

    @Test
    void testImportTeachersLenient_WithMissingDepartment_ShouldUseDefault() throws Exception {
        String csvContent = """
                Name,EmployeeID
                John Doe,EMP-001
                """;

        when(mockFile.getOriginalFilename()).thenReturn("teachers.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importTeachersLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertTrue(result.getWarningCount() > 0); // Should warn about missing department
    }

    @Test
    void testImportTeachersLenient_WithDuplicateEmployeeId_ShouldRecordError() throws Exception {
        String csvContent = """
                Name,EmployeeID,Department
                John Doe,EMP-001,Math
                """;

        when(mockFile.getOriginalFilename()).thenReturn("teachers.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        when(teacherRepository.findByEmployeeId("EMP-001")).thenReturn(Optional.of(testTeacher));

        ImportResult result = service.importTeachersLenient(mockFile);

        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    void testImportCoursesLenient_WithValidCSV_ShouldImportSuccessfully() throws Exception {
        String csvContent = """
                CourseCode,CourseName
                MATH101,Algebra I
                ENG101,English I
                """;

        when(mockFile.getOriginalFilename()).thenReturn("courses.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importCoursesLenient(mockFile);

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        verify(courseRepository, times(2)).save(any(Course.class));
    }

    @Test
    void testImportCoursesLenient_WithMissingCourseName_ShouldAutoGenerate() throws Exception {
        String csvContent = """
                CourseCode
                MATH101
                """;

        when(mockFile.getOriginalFilename()).thenReturn("courses.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importCoursesLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertTrue(result.getWarningCount() > 0);
    }

    @Test
    void testImportCoursesLenient_WithDuplicateCourseCode_ShouldRecordError() throws Exception {
        String csvContent = """
                CourseCode,CourseName
                MATH101,Algebra I
                """;

        when(mockFile.getOriginalFilename()).thenReturn("courses.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        when(courseRepository.findByCourseCode("MATH101")).thenReturn(Optional.of(testCourse));

        ImportResult result = service.importCoursesLenient(mockFile);

        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    void testImportRoomsLenient_WithValidCSV_ShouldImportSuccessfully() throws Exception {
        String csvContent = """
                RoomNumber,Capacity,Building,Floor
                101,30,Main,1
                102,25,Main,1
                """;

        when(mockFile.getOriginalFilename()).thenReturn("rooms.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importRoomsLenient(mockFile);

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        verify(roomRepository, times(2)).save(any(Room.class));
    }

    @Test
    void testImportRoomsLenient_WithMissingRoomNumber_ShouldAutoGenerate() throws Exception {
        String csvContent = """
                Capacity
                30
                """;

        when(mockFile.getOriginalFilename()).thenReturn("rooms.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importRoomsLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertTrue(result.getWarningCount() > 0);
    }

    @Test
    void testImportRoomsLenient_WithDuplicateRoomNumber_ShouldRecordError() throws Exception {
        String csvContent = """
                RoomNumber,Capacity
                101,30
                """;

        when(mockFile.getOriginalFilename()).thenReturn("rooms.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        when(roomRepository.findByRoomNumber("101")).thenReturn(Optional.of(testRoom));

        ImportResult result = service.importRoomsLenient(mockFile);

        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0);
    }

    // ========== COLUMN MAPPING TESTS ==========

    @Test
    void testImportStudentsLenient_WithAlternativeColumnNames_ShouldMapCorrectly() throws Exception {
        String csvContent = """
                student_id,first_name,last_name,grade_level
                STU-001,John,Doe,9
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
    }

    @Test
    void testImportStudentsLenient_WithCaseInsensitiveHeaders_ShouldMapCorrectly() throws Exception {
        String csvContent = """
                STUDENTID,FIRSTNAME,LASTNAME,GRADELEVEL
                STU-001,John,Doe,9
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
    }

    @Test
    void testImportTeachersLenient_WithVariedColumnNames_ShouldMapCorrectly() throws Exception {
        String csvContent = """
                name,employee_id,dept
                John Doe,EMP-001,Math
                """;

        when(mockFile.getOriginalFilename()).thenReturn("teachers.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importTeachersLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    void testImportStudentsLenient_WithEmptyCSV_ShouldThrowException() throws Exception {
        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

        assertThrows(ImportException.class, () -> service.importStudentsLenient(mockFile));
    }

    @Test
    void testImportStudentsLenient_WithHeaderOnlyCSV_ShouldReturnNoSuccess() throws Exception {
        String csvContent = "StudentID,FirstName,LastName,GradeLevel\n";

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
    }

    @Test
    void testImportStudentsLenient_WithIOException_ShouldThrowImportException() throws Exception {
        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenThrow(new IOException("File read error"));

        assertThrows(ImportException.class, () -> service.importStudentsLenient(mockFile));
    }

    @Test
    void testImportStudentsLenient_WithUnsupportedFileType_ShouldThrowException() throws Exception {
        when(mockFile.getOriginalFilename()).thenReturn("students.txt");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        assertThrows(ImportException.class, () -> service.importStudentsLenient(mockFile));
    }

    @Test
    void testImportStudentsLenient_WithNoFileExtension_ShouldThrowException() throws Exception {
        when(mockFile.getOriginalFilename()).thenReturn("students");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        assertThrows(ImportException.class, () -> service.importStudentsLenient(mockFile));
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void testImportStudentsLenient_WithEmptyRows_ShouldSkipThem() throws Exception {
        String csvContent = """
                StudentID,FirstName,LastName,GradeLevel
                STU-001,John,Doe,9

                STU-002,Jane,Smith,10
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
    }

    @Test
    void testImportStudentsLenient_WithQuotedValues_ShouldHandleCorrectly() throws Exception {
        String csvContent = """
                StudentID,FirstName,LastName,GradeLevel
                "STU-001","John","Doe","9"
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
    }

    @Test
    void testImportStudentsLenient_WithCommasInQuotedValues_ShouldHandleCorrectly() throws Exception {
        String csvContent = """
                StudentID,FirstName,LastName,GradeLevel
                "STU-001","Doe, John","Smith",9
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
    }

    // ========== VALIDATION TESTS ==========

    @Test
    void testValidateSchedule_WithRoomDefaultType_ShouldSetClassroom() throws Exception {
        String csvContent = """
                RoomNumber,Capacity
                101,30
                """;

        when(mockFile.getOriginalFilename()).thenReturn("rooms.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importRoomsLenient(mockFile);

        assertNotNull(result);
        verify(roomRepository, atLeastOnce()).save(argThat(room ->
            room.getType() == RoomType.CLASSROOM
        ));
    }

    @Test
    void testValidateSchedule_WithSaveError_ShouldRecordError() throws Exception {
        String csvContent = """
                StudentID,FirstName,LastName,GradeLevel
                STU-001,John,Doe,9
                """;

        when(mockFile.getOriginalFilename()).thenReturn("students.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        when(studentRepository.save(any(Student.class))).thenThrow(new RuntimeException("Database error"));

        ImportResult result = service.importStudentsLenient(mockFile);

        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    void testValidateSchedule_WithCourseDefaultMaxStudents_ShouldSet30() throws Exception {
        String csvContent = """
                CourseCode,CourseName
                MATH101,Algebra I
                """;

        when(mockFile.getOriginalFilename()).thenReturn("courses.csv");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));

        ImportResult result = service.importCoursesLenient(mockFile);

        assertNotNull(result);
        verify(courseRepository, atLeastOnce()).save(argThat(course ->
            course.getMaxStudents() == 30
        ));
    }
}
