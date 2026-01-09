package com.heronix;

import com.heronix.model.domain.*;
import com.heronix.model.enums.EnrollmentStatus;
import com.heronix.model.enums.SchedulePeriod;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates Phase 2B Test Data
 * - Creates Campus
 * - Creates Schedule
 * - Creates Student Enrollments
 *
 * NOTE: @Transactional removed to persist data
 */
@SpringBootTest
public class CreatePhase2BTestData {

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    public void createTestData() {
        System.out.println("=".repeat(70));
        System.out.println("CREATING PHASE 2B TEST DATA");
        System.out.println("=".repeat(70));
        System.out.println();

        // 1. Create Campus
        System.out.println("1. Creating Campus...");
        Campus campus = campusRepository.findAll().stream().findFirst().orElse(null);
        if (campus == null) {
            campus = Campus.builder()
                .campusCode("MAIN")
                .name("Main Campus")
                .address("123 School Street")
                .city("Springfield")
                .state("IL")
                .zipCode("62701")
                .phone("555-0100")
                .email("main@school.edu")
                .principalName("Dr. Principal")
                .build();
            campus = campusRepository.save(campus);
            System.out.println("   ✅ Created campus: " + campus.getName() + " (ID: " + campus.getId() + ")");
        } else {
            System.out.println("   ℹ️ Campus already exists: " + campus.getName() + " (ID: " + campus.getId() + ")");
        }
        System.out.println();

        // 2. Get or Create Schedule
        System.out.println("2. Getting/Creating Schedule...");
        Schedule schedule = scheduleRepository.findAll().stream().findFirst().orElse(null);
        if (schedule == null) {
            schedule = new Schedule();
            schedule.setName("Test Schedule 2025");
            schedule.setPeriod(SchedulePeriod.YEARLY);
            schedule.setScheduleType(ScheduleType.TRADITIONAL);
            schedule.setStartDate(LocalDate.of(2024, 8, 1));
            schedule.setEndDate(LocalDate.of(2025, 6, 30));
            schedule.setActive(true);
            schedule = scheduleRepository.save(schedule);
            System.out.println("   ✅ Created schedule: " + schedule.getName());
        } else {
            System.out.println("   ℹ️ Using existing schedule: " + schedule.getName() + " (ID: " + schedule.getId() + ")");
        }
        System.out.println();

        // 3. Get Students and Courses
        List<Student> students = studentRepository.findAll().stream()
            .filter(s -> s.getDeleted() == null || !s.getDeleted())
            .collect(Collectors.toList());
        List<Course> courses = courseRepository.findAll();

        System.out.println("3. Available Data:");
        System.out.println("   Students: " + students.size());
        System.out.println("   Courses: " + courses.size());
        System.out.println();

        if (students.isEmpty() || courses.isEmpty()) {
            System.out.println("❌ Cannot create enrollments - missing students or courses!");
            return;
        }

        // 4. Create Student Enrollments
        System.out.println("4. Creating Student Enrollments...");
        int enrollmentCount = 0;

        // Find first few courses for enrollment
        List<Course> targetCourses = courses.stream().limit(5).collect(Collectors.toList());

        for (int i = 0; i < targetCourses.size() && i < 5; i++) {
            Course course = targetCourses.get(i);

            // Enroll 15-20 students per course
            int studentsPerCourse = Math.min(20, students.size() / targetCourses.size());
            int startIdx = i * studentsPerCourse;
            int endIdx = Math.min(startIdx + studentsPerCourse, students.size());

            for (int j = startIdx; j < endIdx; j++) {
                Student student = students.get(j);

                // Check if already enrolled
                boolean alreadyEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                    student.getId(), course.getId());

                if (!alreadyEnrolled) {
                    StudentEnrollment enrollment = new StudentEnrollment();
                    enrollment.setStudent(student);
                    enrollment.setCourse(course);
                    enrollment.setSchedule(schedule);
                    enrollment.setEnrolledDate(LocalDateTime.now());
                    enrollment.setStatus(EnrollmentStatus.ACTIVE);

                    enrollmentRepository.save(enrollment);
                    enrollmentCount++;
                }
            }

            System.out.println("   ✅ Enrolled " + (endIdx - startIdx) + " students in: " +
                course.getCourseName());
        }

        System.out.println();
        System.out.println("   Total enrollments created: " + enrollmentCount);
        System.out.println();

        // 5. Verify Results
        System.out.println("=".repeat(70));
        System.out.println("VERIFICATION");
        System.out.println("=".repeat(70));

        long campusCount = campusRepository.count();
        long totalEnrollments = enrollmentRepository.count();

        System.out.println("Campus Count: " + campusCount);
        System.out.println("Total Enrollments: " + totalEnrollments);
        System.out.println();

        if (campusCount > 0 && totalEnrollments > 0) {
            System.out.println("✅ ALL TEST DATA CREATED SUCCESSFULLY");
            System.out.println();
            System.out.println("Next step: Run VerifyPhase2BTestData to confirm readiness");
        } else {
            System.out.println("❌ Test data creation incomplete");
        }

        System.out.println();
        System.out.println("=".repeat(70));
    }
}
