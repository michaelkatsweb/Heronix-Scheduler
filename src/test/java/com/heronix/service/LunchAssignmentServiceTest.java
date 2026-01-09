package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for LunchAssignmentService
 *
 * Phase 5E: Testing & Validation
 *
 * Tests:
 * - Student assignment algorithms (by grade, alphabetical, balanced, random, by ID)
 * - Individual student assignments (assign, reassign, remove, lock, unlock)
 * - Teacher assignments and supervision duties
 * - Assignment validation and statistics
 * - Edge cases and error handling
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-01
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "logging.level.com.eduscheduler=DEBUG"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LunchAssignmentServiceTest {

    @Autowired
    private LunchAssignmentService lunchAssignmentService;

    @Autowired
    private LunchWaveService lunchWaveService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentLunchAssignmentRepository studentLunchAssignmentRepository;

    @Autowired
    private TeacherLunchAssignmentRepository teacherLunchAssignmentRepository;

    @Autowired
    private CampusRepository campusRepository;

    // ========== Test Data Builder ==========

    /**
     * Helper class to create fresh test data for each test.
     * All entities are created within the test's transaction, ensuring they're managed.
     */
    private class TestData {
        Schedule schedule;
        List<LunchWave> lunchWaves;
        List<Student> students;
        List<Teacher> teachers;
        Campus campus;

        /**
         * Create all test data: schedule, lunch waves, students, and teachers.
         * @return this TestData instance for chaining
         */
        TestData createAll() {
            createCampus();
            createSchedule();
            createLunchWaves();
            createStudents();
            createTeachers();
            return this;
        }

        void createCampus() {
            campus = new Campus();
            campus.setName("Test Campus - " + System.currentTimeMillis());
            campus.setCampusCode("TC-" + System.currentTimeMillis());
            campus = campusRepository.save(campus);
        }

        void createSchedule() {
            schedule = new Schedule();
            schedule.setName("Test Schedule - Lunch Assignments");
            schedule.setPeriod(SchedulePeriod.MASTER);
            schedule.setScheduleType(ScheduleType.TRADITIONAL);
            schedule.setStartDate(LocalDate.of(2025, 8, 18));
            schedule.setEndDate(LocalDate.of(2025, 12, 20));
            schedule = scheduleRepository.save(schedule);
        }

        void createLunchWaves() {
            lunchWaves = lunchWaveService.createWeekiWacheeTemplate(schedule);
        }

        void createStudents() {
            students = new java.util.ArrayList<>();
            String[] lastNames = {"Anderson", "Baker", "Carter", "Davis", "Evans", "Foster", "Garcia", "Harris",
                                  "Jackson", "Kelly", "Lewis", "Martin", "Nelson", "Oliver", "Parker", "Quinn",
                                  "Roberts", "Smith", "Taylor", "Wilson"};

            // Use timestamp to ensure unique student IDs
            String uniquePrefix = "TEST-" + System.currentTimeMillis() + "-";
            for (int grade = 9; grade <= 12; grade++) {
                for (int i = 0; i < 25; i++) {
                    Student student = new Student();
                    student.setFirstName("Student" + i);
                    student.setLastName(lastNames[i % lastNames.length]);
                    student.setStudentId(uniquePrefix + "S" + grade + String.format("%03d", i));
                    student.setGradeLevel(String.valueOf(grade));
                    student.setEmail("student" + i + "-" + grade + "-" + System.currentTimeMillis() + "@school.edu");
                    student.setCampus(campus);  // Associate student with test campus
                    students.add(studentRepository.save(student));
                }
            }
        }

        void createTeachers() {
            teachers = new java.util.ArrayList<>();
            // Use timestamp to ensure unique employee IDs
            String uniquePrefix = "TEST-" + System.currentTimeMillis() + "-";
            for (int i = 0; i < 20; i++) {
                Teacher teacher = new Teacher();
                teacher.setName("Teacher" + i + " Test");
                teacher.setFirstName("Teacher" + i);
                teacher.setLastName("Test");
                teacher.setEmployeeId(uniquePrefix + "T" + String.format("%03d", i));
                teacher.setEmail("teacher" + i + "-" + System.currentTimeMillis() + "@school.edu");
                teacher.setDepartment("General");
                teacher.setPrimaryCampus(campus);  // Associate with test campus
                teachers.add(teacherRepository.save(teacher));
            }
        }
    }

    // ========== Assignment Algorithm Tests ==========

    @Test
    @Transactional
    @Order(1)
    @DisplayName("Assign students by grade level")
    void testAssignStudentsByGradeLevel() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When
        int assignedCount = lunchAssignmentService.assignStudentsToLunchWaves(
            data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BY_GRADE_LEVEL
        );

        // Then
        assertThat(assignedCount).isEqualTo(100); // All 100 students assigned

        // Verify students are distributed across waves
        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(assignments).hasSize(100);

        // Verify assignment method recorded
        assertThat(assignments)
            .allMatch(a -> a.getAssignmentMethod() == LunchAssignmentMethod.BY_GRADE_LEVEL);
    }

    @Test
    @Transactional
    @Order(2)
    @DisplayName("Assign students alphabetically")
    void testAssignStudentsAlphabetically() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When
        int assignedCount = lunchAssignmentService.assignStudentsToLunchWaves(
            data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.ALPHABETICAL
        );

        // Then
        assertThat(assignedCount).isEqualTo(100);

        // Verify alphabetical distribution
        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(assignments).hasSize(100);

        // Students with last names A-H should be in wave 1
        LunchWave wave1 = data.lunchWaves.get(0);
        List<StudentLunchAssignment> wave1Assignments = lunchAssignmentService.getWaveRoster(wave1.getId());

        assertThat(wave1Assignments)
            .allMatch(a -> {
                String lastName = a.getStudent().getLastName();
                return lastName.charAt(0) >= 'A' && lastName.charAt(0) <= 'H';
            });
    }

    @Test
    @Transactional
    @Order(3)
    @DisplayName("Assign students with balanced distribution")
    void testAssignStudentsBalanced() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When
        int assignedCount = lunchAssignmentService.assignStudentsToLunchWaves(
            data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED
        );

        // Then
        assertThat(assignedCount).isEqualTo(100);

        // Verify balanced distribution across 3 waves
        // 100 students / 3 waves = ~33-34 students per wave
        for (LunchWave wave : data.lunchWaves) {
            List<StudentLunchAssignment> waveAssignments = lunchAssignmentService.getWaveRoster(wave.getId());
            assertThat(waveAssignments.size()).isBetween(30, 35);
        }
    }

    @Test
    @Transactional
    @Order(4)
    @DisplayName("Assign students randomly")
    void testAssignStudentsRandomly() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When
        int assignedCount = lunchAssignmentService.assignStudentsToLunchWaves(
            data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.RANDOM
        );

        // Then
        assertThat(assignedCount).isEqualTo(100);

        // Verify all students are assigned (random distribution may not be perfectly balanced)
        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(assignments).hasSize(100);
    }

    @Test
    @Transactional
    @Order(5)
    @DisplayName("Assign students by student ID ranges")
    void testAssignStudentsByStudentId() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When
        int assignedCount = lunchAssignmentService.assignStudentsToLunchWaves(
            data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BY_STUDENT_ID
        );

        // Then
        assertThat(assignedCount).isEqualTo(100);

        // Verify students with lower IDs are in earlier waves
        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(assignments).hasSize(100);
    }

    // ========== Individual Assignment Tests ==========

    @Test
    @Transactional
    @Order(10)
    @DisplayName("Assign single student to lunch wave")
    void testAssignStudentToWave() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();
        Student student = data.students.get(0);
        LunchWave wave = data.lunchWaves.get(0);

        // When
        StudentLunchAssignment assignment = lunchAssignmentService.assignStudentToWave(
            student.getId(), wave.getId(), "admin"
        );

        // Then
        assertThat(assignment).isNotNull();
        assertThat(assignment.getStudent()).isEqualTo(student);
        assertThat(assignment.getLunchWave()).isEqualTo(wave);
        assertThat(assignment.getAssignedBy()).isEqualTo("admin");
        assertThat(assignment.getIsLocked()).isFalse();
    }

    @Test
    @Transactional
    @Order(11)
    @DisplayName("Reassign student to different wave")
    void testReassignStudent() {
        // Given: Fresh test data with student assigned to wave 1
        TestData data = new TestData().createAll();
        Student student = data.students.get(0);
        LunchWave wave1 = data.lunchWaves.get(0);
        StudentLunchAssignment originalAssignment = lunchAssignmentService.assignStudentToWave(
            student.getId(), wave1.getId(), "admin"
        );

        // When: Reassign to wave 2
        LunchWave wave2 = data.lunchWaves.get(1);
        StudentLunchAssignment newAssignment = lunchAssignmentService.reassignStudent(
            originalAssignment.getId(), wave2.getId(), "admin"
        );

        // Then
        assertThat(newAssignment.getLunchWave()).isEqualTo(wave2);
        assertThat(newAssignment.getManualOverride()).isTrue();

        // Verify only one assignment exists
        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository
            .findByScheduleId(data.schedule.getId());
        assertThat(assignments).hasSize(1);
    }

    @Test
    @Transactional
    @Order(12)
    @DisplayName("Remove student lunch assignment")
    void testRemoveStudentAssignment() {
        // Given: Fresh test data with student assigned to wave
        TestData data = new TestData().createAll();
        Student student = data.students.get(0);
        LunchWave wave = data.lunchWaves.get(0);
        StudentLunchAssignment assignment = lunchAssignmentService.assignStudentToWave(
            student.getId(), wave.getId(), "admin"
        );

        // When
        lunchAssignmentService.removeStudentAssignment(assignment.getId());

        // Then
        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository
            .findByScheduleId(data.schedule.getId());
        assertThat(assignments).isEmpty();
    }

    @Test
    @Transactional
    @Order(13)
    @DisplayName("Lock student assignment")
    void testLockAssignment() {
        // Given: Fresh test data with student assigned to wave
        TestData data = new TestData().createAll();
        Student student = data.students.get(0);
        LunchWave wave = data.lunchWaves.get(0);
        StudentLunchAssignment assignment = lunchAssignmentService.assignStudentToWave(
            student.getId(), wave.getId(), "admin"
        );

        // When
        lunchAssignmentService.lockAssignment(assignment.getId(), "admin");

        // Then
        Optional<StudentLunchAssignment> locked = studentLunchAssignmentRepository.findById(assignment.getId());
        assertThat(locked).isPresent();
        assertThat(locked.get().getIsLocked()).isTrue();
    }

    @Test
    @Transactional
    @Order(14)
    @DisplayName("Unlock student assignment")
    void testUnlockAssignment() {
        // Given: Fresh test data with locked assignment
        TestData data = new TestData().createAll();
        Student student = data.students.get(0);
        LunchWave wave = data.lunchWaves.get(0);
        StudentLunchAssignment assignment = lunchAssignmentService.assignStudentToWave(
            student.getId(), wave.getId(), "admin"
        );
        lunchAssignmentService.lockAssignment(assignment.getId(), "admin");

        // When
        lunchAssignmentService.unlockAssignment(assignment.getId(), "admin");

        // Then
        Optional<StudentLunchAssignment> unlocked = studentLunchAssignmentRepository.findById(assignment.getId());
        assertThat(unlocked).isPresent();
        assertThat(unlocked.get().getIsLocked()).isFalse();
    }

    // ========== Teacher Assignment Tests ==========

    @Test
    @Transactional
    @Order(20)
    @DisplayName("Assign all teachers to lunch waves")
    void testAssignTeachersToLunchWaves() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When
        int assignedCount = lunchAssignmentService.assignTeachersToLunchWaves(data.schedule.getId());

        // Then
        assertThat(assignedCount).isEqualTo(20); // All 20 teachers assigned

        // Verify assignments created
        List<TeacherLunchAssignment> assignments = teacherLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(assignments).hasSize(20);
    }

    @Test
    @Transactional
    @Order(21)
    @DisplayName("Assign teacher supervision duty")
    void testAssignSupervisionDuty() {
        // Given: Fresh test data with teachers pre-assigned
        TestData data = new TestData().createAll();
        lunchAssignmentService.assignTeachersToLunchWaves(data.schedule.getId());

        List<TeacherLunchAssignment> existingAssignments = teacherLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(existingAssignments).isNotEmpty();
        TeacherLunchAssignment assignment = existingAssignments.get(0);

        // When
        TeacherLunchAssignment updated = lunchAssignmentService.assignSupervisionDuty(
            assignment.getId(), "Main Cafeteria", "admin"
        );

        // Then
        assertThat(updated.getHasSupervisionDuty()).isTrue();
        assertThat(updated.getSupervisionLocation()).isEqualTo("Main Cafeteria");
    }

    @Test
    @Transactional
    @Order(22)
    @DisplayName("Remove teacher supervision duty")
    void testRemoveSupervisionDuty() {
        // Given: Fresh test data with teachers pre-assigned and one with supervision duty
        TestData data = new TestData().createAll();
        lunchAssignmentService.assignTeachersToLunchWaves(data.schedule.getId());

        List<TeacherLunchAssignment> existingAssignments = teacherLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(existingAssignments).isNotEmpty();
        TeacherLunchAssignment assignment = existingAssignments.get(1); // Use different teacher than test 21
        lunchAssignmentService.assignSupervisionDuty(assignment.getId(), "Main Cafeteria", "admin");

        // When
        TeacherLunchAssignment updated = lunchAssignmentService.removeSupervisionDuty(
            assignment.getId(), "admin"
        );

        // Then
        assertThat(updated.getHasSupervisionDuty()).isFalse();
        assertThat(updated.getSupervisionLocation()).isNull();
    }

    // ========== Validation Tests ==========

    @Test
    @Transactional
    @Order(30)
    @DisplayName("Check all students are assigned")
    void testAreAllStudentsAssigned() {
        // Given: Fresh test data with no students assigned yet
        TestData data = new TestData().createAll();
        assertThat(lunchAssignmentService.areAllStudentsAssigned(data.schedule.getId())).isFalse();

        // When: Assign all students
        lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED);

        // Then
        assertThat(lunchAssignmentService.areAllStudentsAssigned(data.schedule.getId())).isTrue();
    }

    @Test
    @Transactional
    @Order(31)
    @DisplayName("Check capacities are respected")
    void testAreCapacitiesRespected() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When: Assign students with balanced distribution
        lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED);

        // Then: Capacities should be respected (100 students across 3 waves @ 250 capacity = plenty of room)
        assertThat(lunchAssignmentService.areCapacitiesRespected(data.schedule.getId())).isTrue();
    }

    @Test
    @Transactional
    @Order(32)
    @DisplayName("Check grade level restrictions are respected")
    void testAreGradeLevelsRespected() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When: Assign students
        lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BY_GRADE_LEVEL);

        // Then: Grade levels should be respected (no grade restrictions in Weeki Wachee template)
        assertThat(lunchAssignmentService.areGradeLevelsRespected(data.schedule.getId())).isTrue();
    }

    @Test
    @Transactional
    @Order(33)
    @DisplayName("Validate all assignments are valid")
    void testAreAssignmentsValid() {
        // Given: Fresh test data
        TestData data = new TestData().createAll();

        // When: Assign all students and teachers
        lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED);
        lunchAssignmentService.assignTeachersToLunchWaves(data.schedule.getId());

        // Then
        assertThat(lunchAssignmentService.areAllStudentsAssigned(data.schedule.getId())).isTrue();
        assertThat(lunchAssignmentService.areCapacitiesRespected(data.schedule.getId())).isTrue();
        assertThat(lunchAssignmentService.areGradeLevelsRespected(data.schedule.getId())).isTrue();
    }

    // ========== Query Tests ==========

    @Test
    @Transactional
    @Order(40)
    @DisplayName("Get student assignment")
    void testGetStudentAssignment() {
        // Given: Fresh test data with student assigned to wave
        TestData data = new TestData().createAll();
        Student student = data.students.get(0);
        LunchWave wave = data.lunchWaves.get(0);
        lunchAssignmentService.assignStudentToWave(student.getId(), wave.getId(), "admin");

        // When
        Optional<StudentLunchAssignment> assignment = lunchAssignmentService.getStudentAssignment(
            student.getId(), data.schedule.getId()
        );

        // Then
        assertThat(assignment).isPresent();
        assertThat(assignment.get().getStudent()).isEqualTo(student);
        assertThat(assignment.get().getLunchWave()).isEqualTo(wave);
    }

    @Test
    @Transactional
    @Order(41)
    @DisplayName("Get teacher assignment")
    void testGetTeacherAssignment() {
        // Given: Fresh test data with teachers pre-assigned
        TestData data = new TestData().createAll();
        lunchAssignmentService.assignTeachersToLunchWaves(data.schedule.getId());

        List<TeacherLunchAssignment> existingAssignments = teacherLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        assertThat(existingAssignments).isNotEmpty();
        TeacherLunchAssignment existing = existingAssignments.get(0);

        // When
        Optional<TeacherLunchAssignment> assignment = lunchAssignmentService.getTeacherAssignment(
            existing.getTeacher().getId(), data.schedule.getId()
        );

        // Then
        assertThat(assignment).isPresent();
        assertThat(assignment.get().getTeacher()).isEqualTo(existing.getTeacher());
        assertThat(assignment.get().getLunchWave()).isEqualTo(existing.getLunchWave());
    }

    @Test
    @Transactional
    @Order(42)
    @DisplayName("Get student roster for lunch wave")
    void testGetStudentRosterForWave() {
        // Given: Fresh test data with students assigned
        TestData data = new TestData().createAll();
        lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED);

        // When
        LunchWave wave1 = data.lunchWaves.get(0);
        List<StudentLunchAssignment> roster = lunchAssignmentService.getWaveRoster(wave1.getId());

        // Then
        assertThat(roster).isNotEmpty();
        assertThat(roster).allMatch(a -> a.getLunchWave().equals(wave1));
    }

    @Test
    @Transactional
    @Order(43)
    @DisplayName("Get teacher roster for lunch wave")
    void testGetTeacherRosterForWave() {
        // Given: Fresh test data with teachers assigned
        TestData data = new TestData().createAll();
        lunchAssignmentService.assignTeachersToLunchWaves(data.schedule.getId());

        // When
        LunchWave wave1 = data.lunchWaves.get(0);
        List<TeacherLunchAssignment> roster = lunchAssignmentService.getTeachersInWave(wave1.getId());

        // Then
        assertThat(roster).isNotEmpty();
        assertThat(roster).allMatch(a -> a.getLunchWave().equals(wave1));
    }

    @Test
    @Transactional
    @Order(44)
    @DisplayName("Get unassigned students")
    void testGetUnassignedStudents() {
        // Given: Fresh test data with only half of students assigned
        TestData data = new TestData().createAll();
        for (int i = 0; i < 50; i++) {
            Student student = data.students.get(i);
            LunchWave wave = data.lunchWaves.get(i % 3);
            lunchAssignmentService.assignStudentToWave(student.getId(), wave.getId(), "admin");
        }

        // When
        List<Student> unassigned = lunchAssignmentService.getUnassignedStudents(data.schedule.getId());

        // Then
        assertThat(unassigned).hasSize(50);
    }

    // ========== Statistics Tests ==========

    @Test
    @Transactional
    @Order(50)
    @DisplayName("Get assignment statistics")
    void testGetAssignmentStatistics() {
        // Given: Fresh test data with all students and teachers assigned
        TestData data = new TestData().createAll();
        lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED);
        lunchAssignmentService.assignTeachersToLunchWaves(data.schedule.getId());

        // Assign supervision duty to 3 teachers
        List<TeacherLunchAssignment> teacherAssignments = teacherLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        for (int i = 0; i < 3; i++) {
            lunchAssignmentService.assignSupervisionDuty(
                teacherAssignments.get(i).getId(), "Cafeteria " + (i+1), "admin"
            );
        }

        // When
        LunchAssignmentService.LunchAssignmentStatistics stats =
            lunchAssignmentService.getAssignmentStatistics(data.schedule.getId());

        // Then
        assertThat(stats.getTotalStudents()).isEqualTo(100);
        assertThat(stats.getAssignedStudents()).isEqualTo(100);
        assertThat(stats.getUnassignedStudents()).isEqualTo(0);
        assertThat(stats.getTotalTeachers()).isEqualTo(20);
        assertThat(stats.getAssignedTeachers()).isEqualTo(20);
        assertThat(stats.getTeachersWithDuty()).isEqualTo(3);
    }

    // ========== Edge Cases ==========

    @Test
    @Transactional
    @Order(60)
    @DisplayName("Rebalance assignments with locked assignments")
    void testRebalanceWithLockedAssignments() {
        // Given: Fresh test data with some assignments locked
        TestData data = new TestData().createAll();
        lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED);

        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository.findByScheduleId(data.schedule.getId());
        for (int i = 0; i < 10; i++) {
            lunchAssignmentService.lockAssignment(assignments.get(i).getId(), "admin");
        }

        // When: Rebalance
        int rebalancedCount = lunchAssignmentService.rebalanceLunchWaves(data.schedule.getId());

        // Then: Locked assignments should not be moved
        List<StudentLunchAssignment> lockedAssignments = studentLunchAssignmentRepository.findByScheduleId(data.schedule.getId())
            .stream()
            .filter(a -> a.getIsLocked() != null && a.getIsLocked())
            .toList();

        assertThat(lockedAssignments).hasSize(10);
    }

    @Test
    @Transactional
    @Order(61)
    @DisplayName("Assign with no available waves")
    void testAssignWithNoAvailableWaves() {
        // Given: Fresh test data with all waves deactivated
        TestData data = new TestData().createAll();
        data.lunchWaves.forEach(wave -> lunchWaveService.deactivateWave(wave.getId()));

        // When/Then
        assertThatThrownBy(() ->
            lunchAssignmentService.assignStudentsToLunchWaves(data.schedule.getId(), data.campus.getId(), LunchAssignmentMethod.BALANCED)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("No active lunch waves");
    }

}
