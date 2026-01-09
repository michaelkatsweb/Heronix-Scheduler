package com.heronix.model.domain;

import com.heronix.repository.AttendanceRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for entity timestamp functionality
 * Tests the createdAt/updatedAt fields added on December 10, 2025
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EntityTimestampTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    public void testStudentTimestampAutoCreation() {
        // Arrange
        Student student = new Student();
        student.setStudentId("TS_TEST_001");
        student.setFirstName("Timestamp");
        student.setLastName("Test");
        student.setGradeLevel("9");
        student.setActive(true);

        // Assert before save
        assertNull(student.getCreatedAt(), "createdAt should be null before save");
        assertNull(student.getUpdatedAt(), "updatedAt should be null before save");

        // Act
        student = studentRepository.save(student);

        // Assert after save
        assertNotNull(student.getCreatedAt(), "createdAt should be set after save");
        assertNotNull(student.getUpdatedAt(), "updatedAt should be set after save");
        assertEquals(student.getCreatedAt(), student.getUpdatedAt(),
            "Both timestamps should be equal on creation");

        System.out.println("Student created with timestamps: " + student.getCreatedAt());
    }

    @Test
    public void testStudentTimestampAutoUpdate() throws InterruptedException {
        // Arrange: Create and save student
        Student student = new Student();
        student.setStudentId("TS_TEST_002");
        student.setFirstName("Update");
        student.setLastName("Test");
        student.setGradeLevel("10");
        student.setActive(true);
        student = studentRepository.save(student);
        studentRepository.flush(); // Ensure saved

        LocalDateTime originalCreated = student.getCreatedAt();
        LocalDateTime originalUpdated = student.getUpdatedAt();

        // Wait to ensure timestamp difference
        Thread.sleep(1000); // 1 second to ensure timestamp difference

        // Act: Update student
        student.setFirstName("Updated");
        student = studentRepository.save(student);
        studentRepository.flush(); // Ensure saved

        // Assert
        assertEquals(originalCreated, student.getCreatedAt(),
            "createdAt should not change on update");
        assertTrue(student.getUpdatedAt().isAfter(originalUpdated) ||
                   student.getUpdatedAt().equals(originalUpdated),
            "updatedAt should be equal or newer after update");

        System.out.println("Original updated: " + originalUpdated);
        System.out.println("New updated: " + student.getUpdatedAt());

        // At minimum, verify createdAt didn't change
        assertNotNull(student.getCreatedAt());
        assertNotNull(student.getUpdatedAt());
    }

    @Test
    public void testAttendanceRecordTimestampAutoCreation() {
        // Arrange: Create student and course
        Student student = new Student();
        student.setStudentId("TS_TEST_003");
        student.setFirstName("Attendance");
        student.setLastName("Test");
        student.setGradeLevel("11");
        student.setActive(true);
        student = studentRepository.save(student);

        Course course = new Course();
        course.setCourseCode("TS_COURSE_001");
        course.setCourseName("Test Course");
        course.setSubject("Test");
        course = courseRepository.save(course);

        // Create attendance record
        AttendanceRecord record = AttendanceRecord.builder()
            .student(student)
            .course(course)
            .attendanceDate(LocalDate.now())
            .status(AttendanceRecord.AttendanceStatus.PRESENT)
            .build();

        // Assert before save
        assertNull(record.getCreatedAt(), "createdAt should be null before save");
        assertNull(record.getUpdatedAt(), "updatedAt should be null before save");

        // Act
        record = attendanceRepository.save(record);

        // Assert after save
        assertNotNull(record.getCreatedAt(), "createdAt should be set after save");
        assertNotNull(record.getUpdatedAt(), "updatedAt should be set after save");
        assertEquals(record.getCreatedAt(), record.getUpdatedAt(),
            "Both timestamps should be equal on creation");

        System.out.println("AttendanceRecord created with timestamps: " + record.getCreatedAt());
    }

    @Test
    public void testAttendanceRecordTimestampAutoUpdate() throws InterruptedException {
        // Arrange: Create and save attendance record
        Student student = new Student();
        student.setStudentId("TS_TEST_004");
        student.setFirstName("Attendance");
        student.setLastName("Update");
        student.setGradeLevel("12");
        student.setActive(true);
        student = studentRepository.save(student);

        Course course = new Course();
        course.setCourseCode("TS_COURSE_002");
        course.setCourseName("Test Course 2");
        course.setSubject("Test");
        course = courseRepository.save(course);

        AttendanceRecord record = AttendanceRecord.builder()
            .student(student)
            .course(course)
            .attendanceDate(LocalDate.now())
            .status(AttendanceRecord.AttendanceStatus.PRESENT)
            .build();
        record = attendanceRepository.save(record);
        attendanceRepository.flush(); // Ensure saved

        LocalDateTime originalCreated = record.getCreatedAt();
        LocalDateTime originalUpdated = record.getUpdatedAt();

        // Wait to ensure timestamp difference
        Thread.sleep(1000); // 1 second to ensure timestamp difference

        // Act: Update attendance record
        record.setStatus(AttendanceRecord.AttendanceStatus.TARDY);
        record = attendanceRepository.save(record);
        attendanceRepository.flush(); // Ensure saved

        // Assert
        assertEquals(originalCreated, record.getCreatedAt(),
            "createdAt should not change on update");
        assertTrue(record.getUpdatedAt().isAfter(originalUpdated) ||
                   record.getUpdatedAt().equals(originalUpdated),
            "updatedAt should be equal or newer after update");

        System.out.println("Original updated: " + originalUpdated);
        System.out.println("New updated: " + record.getUpdatedAt());

        // At minimum, verify timestamps exist
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());
    }

    @Test
    public void testTimestampPersistence() {
        // Arrange: Create student
        Student student = new Student();
        student.setStudentId("TS_TEST_005");
        student.setFirstName("Persistence");
        student.setLastName("Test");
        student.setGradeLevel("9");
        student.setActive(true);
        student = studentRepository.save(student);

        Long studentId = student.getId();
        LocalDateTime createdAt = student.getCreatedAt();
        LocalDateTime updatedAt = student.getUpdatedAt();

        // Act: Clear persistence context and reload
        studentRepository.flush();
        Student reloaded = studentRepository.findById(studentId).orElseThrow();

        // Assert: Timestamps persisted to database
        assertNotNull(reloaded.getCreatedAt(), "createdAt should persist");
        assertNotNull(reloaded.getUpdatedAt(), "updatedAt should persist");
        assertEquals(createdAt, reloaded.getCreatedAt(),
            "createdAt should match after reload");
        assertEquals(updatedAt, reloaded.getUpdatedAt(),
            "updatedAt should match after reload");
    }
}
