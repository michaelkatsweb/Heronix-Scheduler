package com.heronix;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class DataDiagnosticTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    public void diagnoseDataIssues() {
        System.out.println("================================================================================");
        System.out.println("EDUSCHEDULER DATA DIAGNOSTIC REPORT");
        System.out.println("================================================================================");

        // ISSUE #1: Art Course Investigation
        System.out.println("\n### ISSUE #1: ART COURSE INVESTIGATION ###\n");

        List<Course> allCourses = courseRepository.findAll();
        List<Course> artCourses = allCourses.stream()
            .filter(c -> (c.getCourseName() != null && c.getCourseName().toLowerCase().contains("art")) ||
                        (c.getSubject() != null && c.getSubject().toLowerCase().contains("art")) ||
                        (c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains("art")) ||
                        (c.getRequiredRoomType() != null && c.getRequiredRoomType().toString().contains("ART")))
            .collect(Collectors.toList());

        if (artCourses.isEmpty()) {
            System.out.println("  ✓ No Art courses found (as expected)");
        } else {
            System.out.println("  ❌ Found Art-related courses:");
            for (Course c : artCourses) {
                System.out.printf("    - ID=%d, Name='%s', Code='%s', Subject='%s', RoomType='%s', Active=%s%n",
                    c.getId(), c.getCourseName(), c.getCourseCode(), c.getSubject(),
                    c.getRequiredRoomType(), c.getActive());
            }
        }

        // Check room type requirements
        System.out.println("\n  Room types required by active courses:");
        Map<Object, Long> roomTypeCounts = allCourses.stream()
            .filter(Course::getActive)
            .filter(c -> c.getRequiredRoomType() != null)
            .collect(Collectors.groupingBy(Course::getRequiredRoomType, Collectors.counting()));

        for (Map.Entry<Object, Long> entry : roomTypeCounts.entrySet()) {
            System.out.printf("    - %s: %d courses%n", entry.getKey(), entry.getValue());
        }

        // ISSUE #2: Teacher Count Discrepancy
        System.out.println("\n### ISSUE #2: TEACHER COUNT DISCREPANCY ###\n");

        List<Teacher> allTeachers = teacherRepository.findAll();
        long activeTeachers = allTeachers.stream().filter(Teacher::getActive).count();
        long inactiveTeachers = allTeachers.stream().filter(t -> !t.getActive()).count();
        long deletedTeachers = allTeachers.stream().filter(t -> t.getDeleted() != null && t.getDeleted()).count();

        System.out.printf("  Total teachers: %d%n", allTeachers.size());
        System.out.printf("  Active teachers: %d%n", activeTeachers);
        System.out.printf("  Inactive teachers: %d%n", inactiveTeachers);
        System.out.printf("  Soft-deleted teachers: %d%n", deletedTeachers);

        if (allTeachers.size() > 5) {
            System.out.printf("  ❌ ISSUE: Found %d teachers, expected 5%n", allTeachers.size());
        }

        // List all teachers
        System.out.println("\n  All teachers in database:");
        for (Teacher t : allTeachers) {
            String status = "";
            if (!t.getActive()) status += " [INACTIVE]";
            if (t.getDeleted() != null && t.getDeleted()) status += " [DELETED]";

            System.out.printf("    %d. %-25s (ID: %-10s, Dept: %-15s)%s%n",
                t.getId(),
                t.getName(),
                t.getEmployeeId() != null ? t.getEmployeeId() : "N/A",
                t.getDepartment() != null ? t.getDepartment() : "N/A",
                status);
        }

        // ISSUE #3: Teacher Course Assignments
        System.out.println("\n### ISSUE #3: TEACHER COURSE ASSIGNMENTS ###\n");

        List<Teacher> activeTeachersList = allTeachers.stream()
            .filter(Teacher::getActive)
            .collect(Collectors.toList());

        for (Teacher t : activeTeachersList) {
            int courseCount = t.getCourses() != null ? t.getCourses().size() : 0;
            System.out.printf("  %-30s: %d courses%n", t.getName(), courseCount);
        }

        System.out.println("\n================================================================================");
        System.out.println("DIAGNOSTIC COMPLETE");
        System.out.println("================================================================================");
    }
}
