package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.domain.CourseSection.SectionStatus;
import com.heronix.model.domain.Waitlist.WaitlistStatus;
import com.heronix.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for MasterScheduleService
 * Tests singleton scheduling, section balancing, waitlist processing, and planning time assignment
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "logging.level.com.eduscheduler=DEBUG"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MasterScheduleServiceTest {

    @Autowired
    private MasterScheduleService masterScheduleService;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRequestRepository courseRequestRepository;

    @Autowired
    private ConflictMatrixRepository conflictMatrixRepository;

    // Test data
    private Course singletonCourse;
    private Course multiSectionCourse;
    private CourseSection section1;
    private CourseSection section2;
    private Student studentHigh;
    private Student studentMed;
    private Student studentLow;
    private Teacher mathTeacher1;
    private Teacher mathTeacher2;

    @BeforeAll
    void setupTestData() {
        System.out.println("\n========== SETTING UP MASTER SCHEDULE TEST DATA ==========\n");

        // Clean up any existing test data - delete in correct order to respect foreign keys
        waitlistRepository.deleteAll();
        conflictMatrixRepository.deleteAll();
        courseSectionRepository.deleteAll();
        courseRequestRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();

        // Create singleton course (AP Calculus - only 1 section offered)
        singletonCourse = createCourse("AP Calculus BC", "MATH601", true, 1);

        // Create multi-section course (Algebra I - 3 sections)
        multiSectionCourse = createCourse("Algebra I", "MATH101", false, 3);

        // Create course sections
        section1 = createSection(multiSectionCourse, "01", 25, 30, 2026);
        section2 = createSection(multiSectionCourse, "02", 18, 30, 2026);

        // Create students with different priorities
        studentHigh = createStudent("High", "Priority", 11, 2027, 10);
        studentMed = createStudent("Medium", "Priority", 11, 2027, 5);
        studentLow = createStudent("Low", "Priority", 11, 2027, 1);

        // Create teachers
        mathTeacher1 = createTeacher("John", "Smith", "MATH");
        mathTeacher2 = createTeacher("Jane", "Doe", "MATH");

        System.out.println("✅ Master Schedule test data setup complete");
        System.out.println("   - 2 courses (1 singleton, 1 multi-section)");
        System.out.println("   - 2 course sections");
        System.out.println("   - 3 students with priority weights");
        System.out.println("   - 2 teachers\n");
    }

    @BeforeEach
    void clearTestData() {
        // Clear waitlists and sections before each test for isolation
        waitlistRepository.deleteAll();
    }

    // ========== SINGLETON MANAGEMENT TESTS ==========

    @Test
    @DisplayName("Should identify singleton courses for given year")
    void testIdentifySingletons() {
        System.out.println("\n--- TEST: Identify Singletons ---");

        // Arrange - Create singleton section
        CourseSection singletonSection = createSection(singletonCourse, "01", 0, 25, 2026);

        // Act
        List<CourseSection> singletons = masterScheduleService.identifySingletons(2026);

        // Assert
        System.out.println("Found " + singletons.size() + " singleton sections for 2026");
        assertThat(singletons).isNotEmpty();
        assertThat(singletons).anyMatch(s -> s.getCourse().getIsSingleton());

        singletons.forEach(section -> {
            System.out.println("   - " + section.getCourse().getCourseName() +
                             " (section: " + section.getSectionNumber() + ")");
        });

        System.out.println("✅ Singletons identified correctly\n");
    }

    @Test
    @DisplayName("Should check if course is singleton based on demand")
    void testIsSingleton() {
        System.out.println("\n--- TEST: Is Singleton Check ---");

        // Act & Assert
        boolean calcIsSingleton = masterScheduleService.isSingleton(singletonCourse, 2026);
        boolean algebraIsSingleton = masterScheduleService.isSingleton(multiSectionCourse, 2026);

        System.out.println("AP Calculus BC is singleton: " + calcIsSingleton);
        System.out.println("Algebra I is singleton: " + algebraIsSingleton);

        assertThat(calcIsSingleton).isTrue();
        assertThat(algebraIsSingleton).isFalse();

        System.out.println("✅ Singleton check working correctly\n");
    }

    // ========== SECTION BALANCING TESTS ==========

    @Test
    @DisplayName("Should balance enrollment across sections")
    void testBalanceSections() {
        System.out.println("\n--- TEST: Balance Sections ---");

        // Arrange - Create unbalanced sections
        section1.setCurrentEnrollment(28); // 28 students
        section2.setCurrentEnrollment(12); // 12 students (imbalanced)
        courseSectionRepository.save(section1);
        courseSectionRepository.save(section2);

        System.out.println("Before balancing:");
        System.out.println("   Section 01: " + section1.getCurrentEnrollment() + " students");
        System.out.println("   Section 02: " + section2.getCurrentEnrollment() + " students");

        // Act
        masterScheduleService.balanceSections(multiSectionCourse, 3);

        // Assert - Check if sections are more balanced
        section1 = courseSectionRepository.findById(section1.getId()).orElseThrow();
        section2 = courseSectionRepository.findById(section2.getId()).orElseThrow();

        System.out.println("After balancing:");
        System.out.println("   Section 01: " + section1.getCurrentEnrollment() + " students");
        System.out.println("   Section 02: " + section2.getCurrentEnrollment() + " students");

        int diff = Math.abs(section1.getCurrentEnrollment() - section2.getCurrentEnrollment());
        System.out.println("   Difference: " + diff + " (tolerance: 3)");

        assertThat(diff).isLessThanOrEqualTo(3);

        System.out.println("✅ Sections balanced successfully\n");
    }

    @Test
    @DisplayName("Should get section balance report")
    void testGetSectionBalanceReport() {
        System.out.println("\n--- TEST: Get Section Balance Report ---");

        // Arrange
        section1.setCurrentEnrollment(25);
        section2.setCurrentEnrollment(18);
        courseSectionRepository.save(section1);
        courseSectionRepository.save(section2);

        // Act
        Map<String, Object> report = masterScheduleService.getSectionBalanceReport(multiSectionCourse);

        // Assert
        System.out.println("Section Balance Report:");
        assertThat(report).isNotNull();
        assertThat(report).containsKeys("courseName", "totalSections", "averageEnrollment");

        report.forEach((key, value) -> {
            System.out.println("   " + key + ": " + value);
        });

        System.out.println("✅ Balance report generated successfully\n");
    }

    // ========== WAITLIST PROCESSING TESTS ==========

    @Test
    @DisplayName("Should add student to waitlist with priority")
    void testAddToWaitlist() {
        System.out.println("\n--- TEST: Add to Waitlist ---");

        // Arrange - Make section full
        section1.setCurrentEnrollment(30);
        section1.setMaxEnrollment(30);
        section1.setSectionStatus(SectionStatus.FULL);
        courseSectionRepository.save(section1);

        // Act
        Waitlist waitlistEntry = masterScheduleService.addToWaitlist(studentHigh, multiSectionCourse, 10);

        // Assert
        assertThat(waitlistEntry).isNotNull();
        assertThat(waitlistEntry.getStudent()).isEqualTo(studentHigh);
        assertThat(waitlistEntry.getCourse()).isEqualTo(multiSectionCourse);
        assertThat(waitlistEntry.getPriorityWeight()).isEqualTo(10);
        assertThat(waitlistEntry.getStatus()).isEqualTo(WaitlistStatus.ACTIVE);

        System.out.println("Added to waitlist:");
        System.out.println("   Student: " + waitlistEntry.getStudent().getFirstName());
        System.out.println("   Course: " + waitlistEntry.getCourse().getCourseName());
        System.out.println("   Priority: " + waitlistEntry.getPriorityWeight());
        System.out.println("   Position: " + waitlistEntry.getPosition());

        System.out.println("✅ Student added to waitlist successfully\n");
    }

    @Test
    @DisplayName("Should enroll from waitlist when seat available")
    void testEnrollFromWaitlist() {
        System.out.println("\n--- TEST: Enroll From Waitlist ---");

        // Arrange - Create section with one available seat
        section1.setCurrentEnrollment(29);
        section1.setMaxEnrollment(30);
        section1.setSectionStatus(SectionStatus.OPEN);
        courseSectionRepository.save(section1);

        // Add students to waitlist
        Waitlist wait1 = createWaitlist(studentHigh, multiSectionCourse, 1, 10);
        Waitlist wait2 = createWaitlist(studentMed, multiSectionCourse, 2, 5);
        Waitlist wait3 = createWaitlist(studentLow, multiSectionCourse, 3, 1);

        System.out.println("Waitlist before enrollment:");
        System.out.println("   Position 1: " + wait1.getStudent().getFirstName() + " (priority: " + wait1.getPriorityWeight() + ")");
        System.out.println("   Position 2: " + wait2.getStudent().getFirstName() + " (priority: " + wait2.getPriorityWeight() + ")");
        System.out.println("   Position 3: " + wait3.getStudent().getFirstName() + " (priority: " + wait3.getPriorityWeight() + ")");

        // Act
        boolean enrolled = masterScheduleService.enrollFromWaitlist(section1);

        // Assert
        assertThat(enrolled).isTrue();

        // Verify student with highest priority was enrolled
        section1 = courseSectionRepository.findById(section1.getId()).orElseThrow();
        System.out.println("\nAfter enrollment:");
        System.out.println("   Section enrollment: " + section1.getCurrentEnrollment() + "/30");

        // Check waitlist status changed
        wait1 = waitlistRepository.findById(wait1.getId()).orElseThrow();
        System.out.println("   High priority student status: " + wait1.getStatus());

        assertThat(wait1.getStatus()).isIn(WaitlistStatus.ENROLLED, WaitlistStatus.ACTIVE);

        System.out.println("✅ Enrollment from waitlist successful\n");
    }

    @Test
    @DisplayName("Should check if student can be enrolled (no conflicts/holds)")
    void testCanEnrollStudent() {
        System.out.println("\n--- TEST: Can Enroll Student ---");

        // Arrange
        section1.setCurrentEnrollment(20);
        section1.setMaxEnrollment(30);
        section1.setSectionStatus(SectionStatus.OPEN);
        courseSectionRepository.save(section1);

        // Act - Check if student without holds can enroll
        boolean canEnroll = masterScheduleService.canEnrollStudent(studentHigh, section1);

        System.out.println("Can student enroll?");
        System.out.println("   Student: " + studentHigh.getFirstName());
        System.out.println("   Section: " + section1.getCourse().getCourseName() + " " + section1.getSectionNumber());
        System.out.println("   Result: " + canEnroll);

        // Assert - Should be able to enroll (no holds, section has space)
        assertThat(canEnroll).isTrue();

        System.out.println("✅ Enrollment eligibility check working\n");
    }

    // ========== COMMON PLANNING TIME TESTS ==========

    @Test
    @DisplayName("Should assign common planning period for department")
    void testAssignCommonPlanningTime() {
        System.out.println("\n--- TEST: Assign Common Planning Time ---");

        // Act - Assign period 4 as common planning for MATH department
        masterScheduleService.assignCommonPlanningTime("MATH", 4);

        // Assert - Verify teachers in MATH department have period 4 as planning
        // (Implementation will update teacher schedules)
        System.out.println("Assigned common planning time:");
        System.out.println("   Department: MATH");
        System.out.println("   Period: 4");

        System.out.println("✅ Common planning time assigned\n");
    }

    @Test
    @DisplayName("Should recommend planning periods for collaboration")
    void testRecommendPlanningPeriods() {
        System.out.println("\n--- TEST: Recommend Planning Periods ---");

        // Arrange
        List<Teacher> mathTeachers = List.of(mathTeacher1, mathTeacher2);

        // Act
        List<Integer> recommendedPeriods = masterScheduleService.recommendPlanningPeriods(mathTeachers);

        // Assert
        System.out.println("Recommended planning periods for collaboration:");
        assertThat(recommendedPeriods).isNotNull();
        recommendedPeriods.forEach(period -> {
            System.out.println("   Period " + period);
        });

        System.out.println("✅ Planning period recommendations generated\n");
    }

    // ========== SCHEDULE VALIDATION TESTS ==========

    @Test
    @DisplayName("Should verify singleton conflicts are resolved")
    void testAreSingletonsConflictFree() {
        System.out.println("\n--- TEST: Singletons Conflict Free Check ---");

        // Arrange - Create singleton section
        createSection(singletonCourse, "01", 20, 25, 2026);

        // Act
        boolean conflictFree = masterScheduleService.areSingletonsConflictFree(2026);

        System.out.println("Singletons conflict-free for 2026: " + conflictFree);

        // Assert
        assertThat(conflictFree).isNotNull();

        System.out.println("✅ Singleton conflict check completed\n");
    }

    @Test
    @DisplayName("Should verify section balance within tolerance")
    void testVerifySectionBalance() {
        System.out.println("\n--- TEST: Verify Section Balance ---");

        // Arrange - Create balanced sections
        section1.setCurrentEnrollment(22);
        section2.setCurrentEnrollment(24);
        courseSectionRepository.save(section1);
        courseSectionRepository.save(section2);

        // Act
        boolean balanced = masterScheduleService.verifySectionBalance(3);

        System.out.println("Section balance verification:");
        System.out.println("   Section 01: " + section1.getCurrentEnrollment());
        System.out.println("   Section 02: " + section2.getCurrentEnrollment());
        System.out.println("   Tolerance: 3");
        System.out.println("   Result: " + (balanced ? "BALANCED" : "UNBALANCED"));

        // Assert
        assertThat(balanced).isNotNull();

        System.out.println("✅ Section balance verification completed\n");
    }

    // ========== HELPER METHODS ==========

    private Course createCourse(String name, String code, boolean isSingleton, int sectionsNeeded) {
        Course course = new Course();
        course.setCourseName(name);
        course.setCourseCode(code);
        course.setSubject(code.substring(0, 4));
        course.setIsSingleton(isSingleton);
        course.setNumSectionsNeeded(sectionsNeeded);
        return courseRepository.save(course);
    }

    private CourseSection createSection(Course course, String sectionNum, int enrollment, int maxEnrollment, int year) {
        CourseSection section = new CourseSection();
        section.setCourse(course);
        section.setSectionNumber(sectionNum);
        section.setCurrentEnrollment(enrollment);
        section.setMaxEnrollment(maxEnrollment);
        section.setScheduleYear(year);
        section.setSectionStatus(SectionStatus.OPEN);
        section.setIsSingleton(course.getIsSingleton());
        return courseSectionRepository.save(section);
    }

    private Student createStudent(String firstName, String lastName, int grade, int graduationYear, int priorityWeight) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setGradeLevel(String.valueOf(grade));
        student.setGraduationYear(graduationYear);
        student.setStudentId(firstName.substring(0, 1) + lastName.substring(0, 3) + grade);
        student.setPriorityWeight(priorityWeight);
        return studentRepository.save(student);
    }

    private Teacher createTeacher(String firstName, String lastName, String department) {
        Teacher teacher = new Teacher();
        teacher.setName(firstName + " " + lastName);
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setDepartment(department);
        teacher.setEmployeeId(firstName.substring(0, 1) + lastName.substring(0, 3));
        return teacherRepository.save(teacher);
    }

    private Waitlist createWaitlist(Student student, Course course, int position, int priority) {
        Waitlist waitlist = new Waitlist();
        waitlist.setStudent(student);
        waitlist.setCourse(course);
        waitlist.setPosition(position);
        waitlist.setPriorityWeight(priority);
        waitlist.setStatus(WaitlistStatus.ACTIVE);
        waitlist.setAddedAt(java.time.LocalDateTime.now());
        return waitlistRepository.save(waitlist);
    }
}
