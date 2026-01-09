package com.heronix;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 2B Test Data Verification
 * Verifies all required data exists for end-to-end testing
 */
@SpringBootTest
public class VerifyPhase2BTestData {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private ClassroomGradeEntryRepository gradeEntryRepository;

    @Test
    public void verifyAllTestData() {
        System.out.println("=".repeat(70));
        System.out.println("PHASE 2B TEST DATA VERIFICATION");
        System.out.println("=".repeat(70));
        System.out.println();

        // 1. Teachers
        List<Teacher> teachers = teacherRepository.findAll().stream()
            .filter(t -> t.getDeleted() == null || !t.getDeleted())
            .collect(Collectors.toList());
        System.out.println("1. TEACHERS");
        System.out.println("-".repeat(70));
        System.out.println("   Count: " + teachers.size());
        System.out.println("   Status: " + (teachers.size() > 0 ? "✅ READY" : "❌ MISSING"));
        if (!teachers.isEmpty()) {
            System.out.println("   Sample data:");
            teachers.stream().limit(3).forEach(t ->
                System.out.println("     " + t.getId() + " | " + t.getName() + " | " + t.getEmail()));
        }
        System.out.println();

        // 2. Courses
        List<Course> courses = courseRepository.findAll();
        System.out.println("2. COURSES");
        System.out.println("-".repeat(70));
        System.out.println("   Count: " + courses.size());
        System.out.println("   Status: " + (courses.size() > 0 ? "✅ READY" : "❌ MISSING"));
        if (!courses.isEmpty()) {
            System.out.println("   Sample data:");
            courses.stream().limit(3).forEach(c ->
                System.out.println("     " + c.getId() + " | " + c.getCourseName() + " | " + c.getCourseCode()));
        }
        System.out.println();

        // 3. Students
        List<Student> students = studentRepository.findAll().stream()
            .filter(s -> s.getDeleted() == null || !s.getDeleted())
            .collect(Collectors.toList());
        System.out.println("3. STUDENTS");
        System.out.println("-".repeat(70));
        System.out.println("   Count: " + students.size());
        System.out.println("   Status: " + (students.size() > 0 ? "✅ READY" : "❌ MISSING"));
        if (!students.isEmpty()) {
            System.out.println("   Sample data:");
            students.stream().limit(3).forEach(s ->
                System.out.println("     " + s.getId() + " | " + s.getFirstName() + " " + s.getLastName() + " | " + s.getStudentId()));
        }
        System.out.println();

        // 4. Enrollments
        List<StudentEnrollment> enrollments = enrollmentRepository.findAll();
        System.out.println("4. STUDENT ENROLLMENTS");
        System.out.println("-".repeat(70));
        System.out.println("   Count: " + enrollments.size());
        System.out.println("   Status: " + (enrollments.size() > 0 ? "✅ READY" : "❌ MISSING"));
        if (!enrollments.isEmpty()) {
            System.out.println("   Sample enrollments:");
            enrollments.stream().limit(5).forEach(e -> {
                try {
                    if (e.getStudent() != null && e.getCourse() != null) {
                        System.out.println("     " + e.getStudent().getFullName() + " -> " +
                            e.getCourse().getCourseName() + " (" + e.getStatus() + ")");
                    }
                } catch (Exception ex) {
                    // LazyInitializationException - data exists but session closed
                    System.out.println("     [Enrollment ID: " + e.getId() + " - data exists]");
                }
            });
        }
        System.out.println();

        // 5. Campus
        List<Campus> campuses = campusRepository.findAll();
        System.out.println("5. CAMPUS");
        System.out.println("-".repeat(70));
        System.out.println("   Count: " + campuses.size());
        System.out.println("   Status: " + (campuses.size() > 0 ? "✅ READY" : "❌ MISSING"));
        if (!campuses.isEmpty()) {
            System.out.println("   Sample data:");
            campuses.stream().limit(3).forEach(c ->
                System.out.println("     " + c.getId() + " | " + c.getName()));
        }
        System.out.println();

        // 6. Existing Grades
        List<ClassroomGradeEntry> grades = gradeEntryRepository.findAll();
        System.out.println("6. EXISTING CLASSROOM GRADE ENTRIES");
        System.out.println("-".repeat(70));
        System.out.println("   Count: " + grades.size());
        System.out.println("   Status: " + (grades.size() > 0 ? "ℹ️ EXISTS" : "ℹ️ NONE (OK - will create during testing)"));
        System.out.println();

        // 7. Overall Readiness
        System.out.println("=".repeat(70));
        System.out.println("OVERALL READINESS SUMMARY");
        System.out.println("=".repeat(70));

        boolean ready = !teachers.isEmpty() && !courses.isEmpty() && !students.isEmpty()
                     && !enrollments.isEmpty() && !campuses.isEmpty();

        if (ready) {
            System.out.println("✅ ALL REQUIRED DATA PRESENT - READY FOR PHASE 2B TESTING");
            System.out.println();

            // 8. Recommended Test Courses
            System.out.println("=".repeat(70));
            System.out.println("RECOMMENDED TEST COURSES");
            System.out.println("=".repeat(70));
            System.out.println();
            System.out.println("Courses with enrolled students (best for testing):");
            System.out.println();

            courses.stream()
                .map(course -> {
                    // Count enrollments directly to avoid lazy loading issues
                    long enrollmentCount = enrollmentRepository.findByCourseId(course.getId()).size();
                    return new Object[]{course, enrollmentCount};
                })
                .filter(pair -> (Long)pair[1] > 0)
                .sorted((a, b) -> Long.compare((Long)b[1], (Long)a[1]))
                .limit(5)
                .forEach(pair -> {
                    Course c = (Course)pair[0];
                    Long count = (Long)pair[1];
                    System.out.println("  • Course ID " + c.getId() + ": " + c.getCourseName() +
                        " (" + count + " students enrolled)");
                });

        } else {
            System.out.println("❌ MISSING REQUIRED DATA - SEE DETAILS ABOVE");
            System.out.println();
            System.out.println("Missing components:");
            if (teachers.isEmpty()) System.out.println("  - Teachers");
            if (courses.isEmpty()) System.out.println("  - Courses");
            if (students.isEmpty()) System.out.println("  - Students");
            if (enrollments.isEmpty()) System.out.println("  - Student Enrollments");
            if (campuses.isEmpty()) System.out.println("  - Campus");
        }

        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println();

        // Also print testing instructions
        if (ready) {
            System.out.println("NEXT STEPS FOR TESTING:");
            System.out.println("1. Review the PHASE_2B_TESTING_GUIDE_DEC_9_2025.md document");
            System.out.println("2. Start the application: mvn javafx:run");
            System.out.println("3. Navigate to Teacher Quick Entry Dashboard");
            System.out.println("4. Select one of the recommended courses above");
            System.out.println("5. Begin testing grade entry scenarios");
            System.out.println();
        }
    }
}
