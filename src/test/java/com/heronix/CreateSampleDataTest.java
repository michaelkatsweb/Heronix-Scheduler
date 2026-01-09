package com.heronix;

import com.heronix.model.domain.*;
import com.heronix.model.domain.CourseRequest.RequestStatus;
import com.heronix.model.domain.CourseSection.SectionStatus;
import com.heronix.model.domain.Waitlist.WaitlistStatus;
import com.heronix.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Create sample data to test new advanced scheduling features
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update"
})
public class CreateSampleDataTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ConflictMatrixRepository conflictMatrixRepository;

    @Autowired
    private CourseRequestRepository courseRequestRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Test
    @Transactional
    public void createSampleData() {
        System.out.println("\n========== CREATING SAMPLE DATA ==========\n");

        // Create sample courses
        Course apCalculus = createCourse("AP Calculus", "MATH501", true, false, 1);
        Course apBiology = createCourse("AP Biology", "SCI401", true, false, 1);
        Course englishLit = createCourse("English Literature", "ENG301", false, false, 3);
        Course worldHistory = createCourse("World History", "HIST201", false, false, 2);
        Course peClass = createCourse("Physical Education", "PE101", false, true, 4);

        System.out.println("✅ Created 5 sample courses");

        // Create sample students
        Student seniorAlice = createStudent("Alice", "Anderson", 12, 2026, true, 100);
        Student seniorBob = createStudent("Bob", "Brown", 12, 2026, true, 100);
        Student juniorCarol = createStudent("Carol", "Chen", 11, 2027, false, 75);
        Student juniorDave = createStudent("Dave", "Davis", 11, 2027, false, 75);
        Student sophomoreEve = createStudent("Eve", "Evans", 10, 2028, false, 50);

        System.out.println("✅ Created 5 sample students");

        // Create course sections
        CourseSection calcSection = createSection(apCalculus, "A", true, 0, 25, 15, 20, 2026);
        CourseSection bioSection1 = createSection(apBiology, "A", true, 12, 25, 15, 20, 2026);
        CourseSection bioSection2 = createSection(apBiology, "B", false, 8, 25, 15, 20, 2026);
        CourseSection engSection1 = createSection(englishLit, "A", false, 22, 30, 15, 25, 2026);
        CourseSection engSection2 = createSection(englishLit, "B", false, 18, 30, 15, 25, 2026);

        System.out.println("✅ Created 5 course sections");

        // Create conflict matrix entries
        createConflictMatrix(apCalculus, apBiology, 15, 75.0, true, 1, 2026);
        createConflictMatrix(apCalculus, englishLit, 8, 40.0, true, 2, 2026);
        createConflictMatrix(apBiology, worldHistory, 10, 50.0, true, 2, 2026);
        createConflictMatrix(englishLit, worldHistory, 5, 25.0, false, 3, 2026);

        System.out.println("✅ Created 4 conflict matrix entries");

        // Create course requests with alternates
        createCourseRequest(seniorAlice, apCalculus, 1, apBiology, englishLit, worldHistory,
            RequestStatus.PENDING, true, 100, 2026);
        createCourseRequest(seniorBob, apBiology, 1, apCalculus, englishLit, null,
            RequestStatus.PENDING, true, 100, 2026);
        createCourseRequest(juniorCarol, englishLit, 2, worldHistory, peClass, null,
            RequestStatus.FULFILLED, false, 75, 2026);
        createCourseRequest(juniorDave, apCalculus, 1, englishLit, worldHistory, null,
            RequestStatus.PENDING, false, 75, 2026);
        createCourseRequest(sophomoreEve, peClass, 3, englishLit, null, null,
            RequestStatus.FULFILLED, false, 50, 2026);

        System.out.println("✅ Created 5 course requests");

        // Create waitlist entries
        createWaitlist(seniorAlice, apBiology, null, 1, 150, WaitlistStatus.ACTIVE);
        createWaitlist(seniorBob, apCalculus, null, 1, 150, WaitlistStatus.ACTIVE);
        createWaitlist(juniorDave, apBiology, null, 2, 100, WaitlistStatus.ACTIVE);
        createWaitlist(juniorCarol, apCalculus, null, 2, 75, WaitlistStatus.BYPASSED);

        System.out.println("✅ Created 4 waitlist entries");

        System.out.println("\n========== SAMPLE DATA CREATION COMPLETE ==========");
        System.out.println("\nSummary:");
        System.out.println("- 5 courses (2 singletons, 1 zero-period eligible)");
        System.out.println("- 5 students (2 seniors with priority)");
        System.out.println("- 5 course sections (various enrollment levels)");
        System.out.println("- 4 conflict matrix entries (including singleton conflicts)");
        System.out.println("- 5 course requests (with alternates)");
        System.out.println("- 4 waitlist entries (demonstrating priority ordering)");
        System.out.println("\n✅ All sample data created successfully!");
    }

    private Course createCourse(String name, String code, boolean isSingleton,
                                boolean isZeroPeriodEligible, int sectionsNeeded) {
        Course course = new Course();
        course.setCourseName(name);
        course.setCourseCode(code);
        course.setSubject(code.substring(0, 3));
        course.setIsSingleton(isSingleton);
        course.setIsZeroPeriodEligible(isZeroPeriodEligible);
        course.setNumSectionsNeeded(sectionsNeeded);
        return courseRepository.save(course);
    }

    private Student createStudent(String firstName, String lastName, int grade,
                                  int graduationYear, boolean isSenior, int priorityWeight) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setGradeLevel(String.valueOf(grade));
        student.setGraduationYear(graduationYear);
        student.setIsSenior(isSenior);
        student.setPriorityWeight(priorityWeight);
        student.setStudentId(firstName.substring(0, 1) + lastName.substring(0, 3) + grade);
        return studentRepository.save(student);
    }

    private CourseSection createSection(Course course, String sectionNum, boolean isSingleton,
                                        int currentEnrollment, int maxEnrollment,
                                        int minEnrollment, int targetEnrollment, int year) {
        CourseSection section = new CourseSection();
        section.setCourse(course);
        section.setSectionNumber(sectionNum);
        section.setIsSingleton(isSingleton);
        section.setCurrentEnrollment(currentEnrollment);
        section.setMaxEnrollment(maxEnrollment);
        section.setMinEnrollment(minEnrollment);
        section.setTargetEnrollment(targetEnrollment);
        section.setSectionStatus(SectionStatus.PLANNED);
        section.setScheduleYear(year);
        section.setWaitlistCount(0);
        return courseSectionRepository.save(section);
    }

    private ConflictMatrix createConflictMatrix(Course course1, Course course2, int conflictCount,
                                                double conflictPercentage, boolean isSingletonConflict,
                                                int priorityLevel, int year) {
        ConflictMatrix matrix = new ConflictMatrix();
        matrix.setCourse1(course1);
        matrix.setCourse2(course2);
        matrix.setConflictCount(conflictCount);
        matrix.setConflictPercentage(conflictPercentage);
        matrix.setIsSingletonConflict(isSingletonConflict);
        matrix.setPriorityLevel(priorityLevel);
        matrix.setScheduleYear(year);
        matrix.setCreatedAt(LocalDateTime.now());
        return conflictMatrixRepository.save(matrix);
    }

    private CourseRequest createCourseRequest(Student student, Course course, int priorityRank,
                                             Course alt1, Course alt2, Course alt3,
                                             RequestStatus status, boolean isRequired,
                                             int weight, int year) {
        CourseRequest request = new CourseRequest();
        request.setStudent(student);
        request.setCourse(course);
        request.setPriorityRank(priorityRank);
        request.setAlternateCourse1(alt1);
        request.setAlternateCourse2(alt2);
        request.setAlternateCourse3(alt3);
        request.setRequestStatus(status);
        request.setIsRequiredForGraduation(isRequired);
        request.setStudentWeight(weight);
        request.setRequestYear(year);
        request.setRequestedAt(LocalDateTime.now());
        return courseRequestRepository.save(request);
    }

    private Waitlist createWaitlist(Student student, Course course, ScheduleSlot slot,
                                   int position, int priorityWeight, WaitlistStatus status) {
        Waitlist waitlist = new Waitlist();
        waitlist.setStudent(student);
        waitlist.setCourse(course);
        waitlist.setScheduleSlot(slot);
        waitlist.setPosition(position);
        waitlist.setPriorityWeight(priorityWeight);
        waitlist.setStatus(status);
        waitlist.setAddedAt(LocalDateTime.now());
        return waitlistRepository.save(waitlist);
    }
}
