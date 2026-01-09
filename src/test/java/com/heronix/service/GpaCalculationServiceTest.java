package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.enums.CourseType;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentGradeRepository;
import com.heronix.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GpaCalculationService
 * Tests the new GPA calculation service created on December 10, 2025
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GpaCalculationServiceTest {

    @Autowired
    private GpaCalculationService gpaCalculationService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentGradeRepository studentGradeRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Student testStudent;
    private Course mathCourse;
    private Course englishCourse;
    private Course scienceCourse;

    @BeforeEach
    public void setup() {
        // Create test student
        testStudent = new Student();
        testStudent.setStudentId("GPA_TEST_001");
        testStudent.setFirstName("GPA");
        testStudent.setLastName("Test");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);
        testStudent = studentRepository.save(testStudent);

        // Create test courses
        mathCourse = createCourse("Algebra II", CourseType.REGULAR);
        englishCourse = createCourse("English 10 Honors", CourseType.HONORS);
        scienceCourse = createCourse("AP Chemistry", CourseType.AP);
    }

    private Course createCourse(String name, CourseType type) {
        Course course = new Course();
        course.setCourseCode("TEST_" + System.currentTimeMillis());
        course.setCourseName(name);
        course.setCourseType(type);
        course.setSubject("Test Subject");
        return courseRepository.save(course);
    }

    private StudentGrade createGrade(Student student, Course course, String letterGrade) {
        StudentGrade grade = new StudentGrade();
        grade.setStudent(student);
        grade.setCourse(course);
        grade.setLetterGrade(letterGrade);
        grade.setGradeDate(LocalDate.now());
        grade.setTerm("Fall 2025");
        grade.setAcademicYear(2025);
        grade.setGpaPoints(gpaCalculationService.convertLetterGradeToGPA(letterGrade));
        return studentGradeRepository.save(grade);
    }

    @Test
    public void testCalculateCumulativeGPA_WithMultipleGrades() {
        // Arrange: Create grades (A, B+, A-)
        createGrade(testStudent, mathCourse, "A");        // 4.0
        createGrade(testStudent, englishCourse, "B+");    // 3.3
        createGrade(testStudent, scienceCourse, "A-");    // 3.7

        // Act
        Double gpa = gpaCalculationService.calculateCumulativeGPA(testStudent);

        // Assert
        assertNotNull(gpa, "GPA should not be null");
        // Expected: (4.0 + 3.3 + 3.7) / 3 = 3.67
        assertEquals(3.67, gpa, 0.01, "GPA should be approximately 3.67");
    }

    @Test
    public void testCalculateWeightedGPA_WithHonorsAndAP() {
        // Arrange: Create grades
        createGrade(testStudent, mathCourse, "A");        // 4.0 (Regular)
        createGrade(testStudent, englishCourse, "B+");    // 3.3 (Honors)
        createGrade(testStudent, scienceCourse, "A-");    // 3.7 (AP)

        // Act
        Double cumulative = gpaCalculationService.calculateCumulativeGPA(testStudent);
        Double weighted = gpaCalculationService.calculateWeightedGPA(testStudent);

        // Assert
        assertNotNull(cumulative, "Cumulative GPA should not be null");
        assertNotNull(weighted, "Weighted GPA should not be null");
        assertTrue(weighted > cumulative,
            "Weighted GPA should be higher than cumulative for Honors/AP courses");

        // Expected weighted: (4.0 + (3.3+0.5) + (3.7+1.0)) / 3 = 4.17
        assertEquals(4.17, weighted, 0.01, "Weighted GPA should be approximately 4.17");
    }

    @Test
    public void testCalculateCumulativeGPA_NoGrades() {
        // Act: Student with no grades
        Double gpa = gpaCalculationService.calculateCumulativeGPA(testStudent);

        // Assert
        assertNotNull(gpa, "GPA should not be null even with no grades");
        assertEquals(0.0, gpa, 0.01, "GPA should be 0.0 with no grades");
    }

    @Test
    public void testUpdateStudentGPA() {
        // Arrange: Create grades
        createGrade(testStudent, mathCourse, "A");
        createGrade(testStudent, englishCourse, "B");

        // Initial state
        assertNull(testStudent.getCurrentGPA(), "Initial GPA should be null");

        // Act
        gpaCalculationService.updateStudentGPA(testStudent);

        // Reload student from database
        testStudent = studentRepository.findById(testStudent.getId()).orElseThrow();

        // Assert
        assertNotNull(testStudent.getCurrentGPA(), "GPA should be populated after update");
        assertTrue(testStudent.getCurrentGPA() >= 3.0 && testStudent.getCurrentGPA() <= 4.0,
            "GPA should be between 3.0 and 4.0 for A and B grades");
    }

    @Test
    public void testConvertLetterGradeToGPA_AllGrades() {
        // Test all letter grades
        assertEquals(4.0, gpaCalculationService.convertLetterGradeToGPA("A+"), 0.01);
        assertEquals(4.0, gpaCalculationService.convertLetterGradeToGPA("A"), 0.01);
        assertEquals(3.7, gpaCalculationService.convertLetterGradeToGPA("A-"), 0.01);
        assertEquals(3.3, gpaCalculationService.convertLetterGradeToGPA("B+"), 0.01);
        assertEquals(3.0, gpaCalculationService.convertLetterGradeToGPA("B"), 0.01);
        assertEquals(2.7, gpaCalculationService.convertLetterGradeToGPA("B-"), 0.01);
        assertEquals(2.3, gpaCalculationService.convertLetterGradeToGPA("C+"), 0.01);
        assertEquals(2.0, gpaCalculationService.convertLetterGradeToGPA("C"), 0.01);
        assertEquals(1.7, gpaCalculationService.convertLetterGradeToGPA("C-"), 0.01);
        assertEquals(1.3, gpaCalculationService.convertLetterGradeToGPA("D+"), 0.01);
        assertEquals(1.0, gpaCalculationService.convertLetterGradeToGPA("D"), 0.01);
        assertEquals(0.0, gpaCalculationService.convertLetterGradeToGPA("F"), 0.01);
    }

    @Test
    public void testConvertLetterGradeToGPA_InvalidGrade() {
        // Test invalid grade
        Double gpa = gpaCalculationService.convertLetterGradeToGPA("INVALID");
        assertNull(gpa, "Invalid grade should return null");
    }

    @Test
    public void testCalculateWeightedGPA_OnlyRegularCourses() {
        // Arrange: All regular courses
        Course regular1 = createCourse("Math", CourseType.REGULAR);
        Course regular2 = createCourse("English", CourseType.REGULAR);

        createGrade(testStudent, regular1, "A");
        createGrade(testStudent, regular2, "B");

        // Act
        Double cumulative = gpaCalculationService.calculateCumulativeGPA(testStudent);
        Double weighted = gpaCalculationService.calculateWeightedGPA(testStudent);

        // Assert
        assertNotNull(cumulative);
        assertNotNull(weighted);
        // For all regular courses, weighted should equal cumulative
        assertEquals(cumulative, weighted, 0.01,
            "Weighted GPA should equal cumulative for all regular courses");
    }

    @Test
    public void testCalculateCumulativeGPA_WithExistingCurrentGPA() {
        // Arrange: Student already has GPA set
        testStudent.setCurrentGPA(3.5);
        testStudent = studentRepository.save(testStudent);

        // Don't create any grades

        // Act
        Double gpa = gpaCalculationService.calculateCumulativeGPA(testStudent);

        // Assert
        // Note: Service recalculates from grade records, so with no grades it returns 0.0
        // If we want to use existing GPA, we should check it first before calling this method
        assertEquals(0.0, gpa, 0.01,
            "Should return 0.0 when no grade records exist (service always recalculates)");

        // The currentGPA field is still preserved in the entity
        assertEquals(3.5, testStudent.getCurrentGPA(), 0.01,
            "CurrentGPA field should remain unchanged");
    }

    @Test
    public void testUpdateMultipleStudentsGPA() {
        // Arrange: Create multiple students with grades
        Student student2 = new Student();
        student2.setStudentId("GPA_TEST_002");
        student2.setFirstName("Test2");
        student2.setLastName("Student");
        student2.setGradeLevel("10");
        student2.setActive(true);
        student2 = studentRepository.save(student2);

        createGrade(testStudent, mathCourse, "A");
        createGrade(student2, englishCourse, "B");

        // Act - Update each student individually
        gpaCalculationService.updateStudentGPA(testStudent);
        gpaCalculationService.updateStudentGPA(student2);

        // Reload from database
        testStudent = studentRepository.findById(testStudent.getId()).orElseThrow();
        student2 = studentRepository.findById(student2.getId()).orElseThrow();

        // Assert
        assertNotNull(testStudent.getCurrentGPA(), "Student 1 GPA should be updated");
        assertNotNull(student2.getCurrentGPA(), "Student 2 GPA should be updated");
        assertTrue(testStudent.getCurrentGPA() > student2.getCurrentGPA(),
            "Student with A should have higher GPA than student with B");
    }
}
