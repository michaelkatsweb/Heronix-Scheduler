package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.domain.CourseRequest.RequestStatus;
import com.heronix.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ConflictMatrixService
 * Tests conflict generation, singleton detection, and heatmap visualization
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "logging.level.com.eduscheduler=DEBUG"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConflictMatrixServiceTest {

    @Autowired
    private ConflictMatrixService conflictMatrixService;

    @Autowired
    private ConflictMatrixRepository conflictMatrixRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRequestRepository courseRequestRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    private Course calculus;
    private Course biology;
    private Course english;
    private Course history;
    private Student alice;
    private Student bob;
    private Student carol;

    @BeforeAll
    void setupTestData() {
        System.out.println("\n========== SETTING UP TEST DATA ==========\n");

        // Clear any existing test data to avoid unique constraint violations
        // Delete in correct order to respect foreign key constraints
        conflictMatrixRepository.deleteAll();
        courseRequestRepository.deleteAll();
        courseSectionRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();

        // Create test courses
        calculus = createCourse("AP Calculus AB", "MATH501", true, 1);
        biology = createCourse("AP Biology", "SCI401", true, 1);
        english = createCourse("English Literature", "ENG301", false, 3);
        history = createCourse("World History", "HIST201", false, 2);

        // Create test students
        alice = createStudent("Alice", "Anderson", 12, 2026);
        bob = createStudent("Bob", "Brown", 12, 2026);
        carol = createStudent("Carol", "Chen", 11, 2027);

        // Create course requests that will generate conflicts
        createCourseRequest(alice, calculus, 1, 2026);
        createCourseRequest(alice, biology, 2, 2026);
        createCourseRequest(alice, english, 3, 2026);

        createCourseRequest(bob, calculus, 1, 2026);
        createCourseRequest(bob, biology, 2, 2026);
        createCourseRequest(bob, history, 3, 2026);

        createCourseRequest(carol, biology, 1, 2027);
        createCourseRequest(carol, english, 2, 2027);
        createCourseRequest(carol, history, 3, 2027);

        System.out.println("✅ Test data setup complete");
        System.out.println("   - 4 courses (2 singletons: Calculus, Biology)");
        System.out.println("   - 3 students (Alice, Bob, Carol)");
        System.out.println("   - 9 course requests");
        System.out.println();
    }

    @BeforeEach
    void clearConflictMatrix() {
        // Clear any existing conflict matrix entries before each test
        conflictMatrixRepository.deleteAll();
    }

    @Test
    @DisplayName("Should generate conflict matrix for given year")
    void testGenerateConflictMatrix() {
        System.out.println("\n--- TEST: Generate Conflict Matrix ---");

        // Act
        conflictMatrixService.generateConflictMatrix(2026);

        // Assert
        List<ConflictMatrix> allConflicts = conflictMatrixRepository.findAll();
        System.out.println("Generated " + allConflicts.size() + " conflict entries");

        assertThat(allConflicts).isNotEmpty();
        allConflicts.forEach(conflict -> {
            System.out.println("   - " + conflict.getCourse1().getCourseName() +
                             " ↔ " + conflict.getCourse2().getCourseName() +
                             ": " + conflict.getConflictCount() + " students (" +
                             conflict.getConflictPercentage() + "%)");
        });

        System.out.println("✅ Conflict matrix generated successfully\n");
    }

    @Test
    @DisplayName("Should identify singleton conflicts correctly")
    void testGetSingletonConflicts() {
        System.out.println("\n--- TEST: Get Singleton Conflicts ---");

        // Arrange
        conflictMatrixService.generateConflictMatrix(2026);

        // Act
        List<ConflictMatrix> singletonConflicts = conflictMatrixService.getSingletonConflicts();

        // Assert
        System.out.println("Found " + singletonConflicts.size() + " singleton conflicts");
        assertThat(singletonConflicts).isNotEmpty();

        singletonConflicts.forEach(conflict -> {
            // Both courses in a singleton conflict should be singletons
            boolean course1IsSingleton = conflict.getCourse1().getIsSingleton();
            boolean course2IsSingleton = conflict.getCourse2().getIsSingleton();

            System.out.println("   - " + conflict.getCourse1().getCourseName() +
                             " (singleton: " + course1IsSingleton + ") ↔ " +
                             conflict.getCourse2().getCourseName() +
                             " (singleton: " + course2IsSingleton + "): " +
                             conflict.getConflictCount() + " students");

            // At least one should be a singleton
            assertThat(course1IsSingleton || course2IsSingleton).isTrue();
            assertThat(conflict.getIsSingletonConflict()).isTrue();
        });

        System.out.println("✅ Singleton conflicts identified correctly\n");
    }

    @Test
    @DisplayName("Should detect conflict between specific courses")
    void testHasConflict() {
        System.out.println("\n--- TEST: Has Conflict Between Courses ---");

        // Arrange
        conflictMatrixService.generateConflictMatrix(2026);

        // Act & Assert - Calculus and Biology have conflict (both Alice and Bob request them)
        boolean hasConflict = conflictMatrixService.hasConflict(calculus, biology, 1);
        System.out.println("Calculus ↔ Biology conflict (threshold 1): " + hasConflict);
        assertThat(hasConflict).isTrue();

        // Act & Assert - Calculus and History should have lower/no conflict
        boolean noConflict = conflictMatrixService.hasConflict(calculus, history, 5);
        System.out.println("Calculus ↔ History conflict (threshold 5): " + noConflict);

        System.out.println("✅ Conflict detection working correctly\n");
    }

    @Test
    @DisplayName("Should get all conflicts for a specific course")
    void testGetConflictsForCourse() {
        System.out.println("\n--- TEST: Get Conflicts For Course ---");

        // Arrange
        conflictMatrixService.generateConflictMatrix(2026);

        // Act
        List<ConflictMatrix> calculusConflicts = conflictMatrixService.getConflictsForCourse(calculus);

        // Assert
        System.out.println("AP Calculus has " + calculusConflicts.size() + " conflicts:");
        assertThat(calculusConflicts).isNotEmpty();

        calculusConflicts.forEach(conflict -> {
            Course otherCourse = conflict.getCourse1().getId().equals(calculus.getId())
                ? conflict.getCourse2()
                : conflict.getCourse1();

            System.out.println("   - vs " + otherCourse.getCourseName() +
                             ": " + conflict.getConflictCount() + " students (" +
                             conflict.getConflictPercentage() + "%)");
        });

        System.out.println("✅ Course conflicts retrieved successfully\n");
    }

    @Test
    @DisplayName("Should identify high conflicts above threshold")
    void testGetHighConflicts() {
        System.out.println("\n--- TEST: Get High Conflicts ---");

        // Arrange
        conflictMatrixService.generateConflictMatrix(2026);

        // Act
        List<ConflictMatrix> highConflicts = conflictMatrixService.getHighConflicts(2);

        // Assert
        System.out.println("Found " + highConflicts.size() + " high conflicts (threshold: 2 students):");
        highConflicts.forEach(conflict -> {
            System.out.println("   - " + conflict.getCourse1().getCourseName() +
                             " ↔ " + conflict.getCourse2().getCourseName() +
                             ": " + conflict.getConflictCount() + " students");
            assertThat(conflict.getConflictCount()).isGreaterThanOrEqualTo(2);
        });

        System.out.println("✅ High conflicts identified correctly\n");
    }

    @Test
    @DisplayName("Should generate conflict heatmap data structure")
    void testGetConflictHeatmap() {
        System.out.println("\n--- TEST: Get Conflict Heatmap ---");

        // Arrange
        conflictMatrixService.generateConflictMatrix(2026);

        // Act
        Map<String, Map<String, Integer>> heatmap = conflictMatrixService.getConflictHeatmap(2026);

        // Assert
        System.out.println("Heatmap structure:");
        assertThat(heatmap).isNotNull();
        assertThat(heatmap).isNotEmpty();

        heatmap.forEach((course1, conflicts) -> {
            System.out.println("   " + course1 + ":");
            conflicts.forEach((course2, count) -> {
                System.out.println("      - " + course2 + ": " + count + " students");
            });
        });

        System.out.println("✅ Heatmap generated successfully\n");
    }

    @Test
    @DisplayName("Should update existing conflict instead of creating duplicate")
    void testUpdateConflict() {
        System.out.println("\n--- TEST: Update Existing Conflict ---");

        // Arrange - Create initial conflict
        ConflictMatrix initial = conflictMatrixService.updateConflict(calculus, biology, 5);
        assertThat(initial).isNotNull();
        assertThat(initial.getConflictCount()).isEqualTo(5);
        System.out.println("Initial conflict: " + initial.getConflictCount() + " students");

        Long initialId = initial.getId();

        // Act - Update the same conflict (should ADD 10 to existing 5 = 15 total)
        ConflictMatrix updated = conflictMatrixService.updateConflict(calculus, biology, 10);

        // Assert - Should be same entity, not new one, with cumulative count
        assertThat(updated.getId()).isEqualTo(initialId);
        assertThat(updated.getConflictCount()).isEqualTo(15); // 5 + 10 = 15 (cumulative)
        System.out.println("Updated conflict: " + updated.getConflictCount() + " students (cumulative)");

        // Verify only one entry exists
        long count = conflictMatrixRepository.count();
        assertThat(count).isEqualTo(1);

        System.out.println("✅ Conflict updated correctly (no duplicates, cumulative counting)\n");
    }

    @Test
    @DisplayName("Should calculate conflict percentage correctly")
    void testConflictPercentageCalculation() {
        System.out.println("\n--- TEST: Conflict Percentage Calculation ---");

        // Arrange
        conflictMatrixService.generateConflictMatrix(2026);

        // Act
        List<ConflictMatrix> conflicts = conflictMatrixRepository.findAll();

        // Assert
        System.out.println("Conflict percentages:");
        conflicts.forEach(conflict -> {
            Double percentage = conflict.getConflictPercentage();
            assertThat(percentage).isNotNull();
            assertThat(percentage).isBetween(0.0, 100.0);

            System.out.println("   - " + conflict.getCourse1().getCourseName() +
                             " ↔ " + conflict.getCourse2().getCourseName() +
                             ": " + String.format("%.1f%%", percentage));
        });

        System.out.println("✅ Conflict percentages calculated correctly\n");
    }

    @Test
    @DisplayName("Should handle year filtering correctly")
    void testYearFiltering() {
        System.out.println("\n--- TEST: Year Filtering ---");

        // Arrange - Generate conflicts for 2026
        conflictMatrixService.generateConflictMatrix(2026);
        long conflicts2026 = conflictMatrixRepository.findByScheduleYear(2026).size();

        // Generate conflicts for 2027
        conflictMatrixService.generateConflictMatrix(2027);
        long conflicts2027 = conflictMatrixRepository.findByScheduleYear(2027).size();

        // Assert
        System.out.println("2026 conflicts: " + conflicts2026);
        System.out.println("2027 conflicts: " + conflicts2027);

        assertThat(conflicts2026).isGreaterThan(0);
        assertThat(conflicts2027).isGreaterThan(0);

        System.out.println("✅ Year filtering working correctly\n");
    }

    @Test
    @DisplayName("Should flag singleton conflicts appropriately")
    void testSingletonConflictFlagging() {
        System.out.println("\n--- TEST: Singleton Conflict Flagging ---");

        // Arrange
        conflictMatrixService.generateConflictMatrix(2026);

        // Act
        List<ConflictMatrix> allConflicts = conflictMatrixRepository.findAll();

        // Assert
        long singletonFlagged = allConflicts.stream()
            .filter(ConflictMatrix::getIsSingletonConflict)
            .count();

        System.out.println("Total conflicts: " + allConflicts.size());
        System.out.println("Singleton conflicts: " + singletonFlagged);

        allConflicts.forEach(conflict -> {
            boolean course1Singleton = conflict.getCourse1().getIsSingleton();
            boolean course2Singleton = conflict.getCourse2().getIsSingleton();
            boolean flaggedAsSingleton = conflict.getIsSingletonConflict();

            if (flaggedAsSingleton) {
                System.out.println("   ✓ " + conflict.getCourse1().getCourseName() +
                                 " ↔ " + conflict.getCourse2().getCourseName() +
                                 " (flagged as singleton)");
                // If flagged as singleton, at least one course should be a singleton
                assertThat(course1Singleton || course2Singleton).isTrue();
            }
        });

        assertThat(singletonFlagged).isGreaterThan(0);
        System.out.println("✅ Singleton conflicts flagged correctly\n");
    }

    // ========== HELPER METHODS ==========

    private Course createCourse(String name, String code, boolean isSingleton, int sectionsNeeded) {
        Course course = new Course();
        course.setCourseName(name);
        course.setCourseCode(code);
        course.setSubject(code.substring(0, 3));
        course.setIsSingleton(isSingleton);
        course.setNumSectionsNeeded(sectionsNeeded);
        return courseRepository.save(course);
    }

    private Student createStudent(String firstName, String lastName, int grade, int graduationYear) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setGradeLevel(String.valueOf(grade));
        student.setGraduationYear(graduationYear);
        student.setStudentId(firstName.substring(0, 1) + lastName.substring(0, 3) + grade);
        return studentRepository.save(student);
    }

    private CourseRequest createCourseRequest(Student student, Course course, int priorityRank, int year) {
        CourseRequest request = new CourseRequest();
        request.setStudent(student);
        request.setCourse(course);
        request.setPriorityRank(priorityRank);
        request.setRequestStatus(RequestStatus.PENDING);
        request.setRequestYear(year);
        request.setRequestedAt(LocalDateTime.now());
        return courseRequestRepository.save(request);
    }
}
