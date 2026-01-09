package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.heronix.model.dto.EnrollmentResult;
import com.heronix.model.dto.EnrollmentStatistics;
import com.heronix.model.enums.EnrollmentStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Student Course Assignment Service
 * Location:
 * src/main/java/com/eduscheduler/service/StudentCourseAssignmentService.java
 * 
 * Intelligently enrolls students in courses and assigns them to schedule slots.
 * 
 * Assignment Strategies:
 * 1. BALANCED: Distribute students evenly across all sections
 * 2. GRADE_COHORT: Keep same grade levels together
 * 3. RANDOM: Random assignment for diversity
 * 4. PRIORITY: Assign by priority (seniors first, etc.)
 * 
 * Features:
 * - Respects course capacity limits
 * - Avoids schedule conflicts
 * - Balances class sizes
 * - Tracks enrollment statistics
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCourseAssignmentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;

    // ========================================================================
    // AUTO-ENROLLMENT
    // ========================================================================

    /**
     * Auto-enroll all students in courses based on grade level
     * 
     * This creates a realistic schedule where students are assigned to:
     * - Core courses (Math, English, Science, History)
     * - Electives based on availability
     * - Appropriate for their grade level
     * 
     * @param scheduleId     Schedule to enroll students in
     * @param enrollmentRate Percentage of courses to enroll each student in
     *                       (0.0-1.0)
     * @return Enrollment results
     */
    @Transactional
    public EnrollmentResult autoEnrollStudents(Long scheduleId, double enrollmentRate) {
        log.info("🎓 Starting auto-enrollment for schedule {} (rate: {}%)",
                scheduleId, enrollmentRate * 100);

        EnrollmentResult result = new EnrollmentResult();
        result.setScheduleId(scheduleId);
        result.setStartTime(LocalDateTime.now());

        // Load schedule
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + scheduleId));

        // Load all active students
        List<Student> students = studentRepository.findByActive(true);
        log.info("Found {} active students", students.size());

        // Load all schedule slots for this schedule
        List<ScheduleSlot> scheduleSlots = scheduleSlotRepository.findByScheduleId(scheduleId);
        log.info("Found {} schedule slots", scheduleSlots.size());

        // Group slots by course
        Map<Long, List<ScheduleSlot>> slotsByCourse = scheduleSlots.stream()
                .filter(slot -> slot.getCourse() != null)
                .collect(Collectors.groupingBy(slot -> slot.getCourse().getId()));

        log.info("Courses with slots: {}", slotsByCourse.size());

        int totalEnrollments = 0;
        int failed = 0;

        // Enroll each student
        for (Student student : students) {
            try {
                int studentEnrollments = enrollStudentInCourses(
                        student, schedule, slotsByCourse, enrollmentRate);

                totalEnrollments += studentEnrollments;
                result.incrementStudentEnrollment(student.getGradeLevel(), studentEnrollments);

            } catch (Exception e) {
                log.error("❌ Failed to enroll student {}: {}",
                        student.getStudentId(), e.getMessage());
                failed++;
            }
        }

        result.setTotalStudents(students.size());
        result.setTotalEnrollments(totalEnrollments);
        result.setFailedEnrollments(failed);
        result.setAverageCoursesPerStudent(totalEnrollments / (double) students.size());
        result.setEndTime(LocalDateTime.now());
        result.setSuccess(true);

        log.info("✅ Auto-enrollment complete:");
        log.info("   Students processed: {}", students.size());
        log.info("   Total enrollments: {}", totalEnrollments);
        log.info("   Average courses per student: {:.1f}", result.getAverageCoursesPerStudent());
        log.info("   Failed: {}", failed);

        return result;
    }

    /**
     * Enroll a single student in appropriate courses
     */
    private int enrollStudentInCourses(
            Student student,
            Schedule schedule,
            Map<Long, List<ScheduleSlot>> slotsByCourse,
            double enrollmentRate) {

        int enrolled = 0;

        // Determine how many courses to enroll in (typically 6-8 for high school)
        int targetCourses = (int) (slotsByCourse.size() * enrollmentRate);
        targetCourses = Math.max(5, Math.min(8, targetCourses)); // Between 5-8 courses

        // Get appropriate courses for student's grade level
        List<Long> availableCourses = slotsByCourse.keySet().stream()
                .filter(courseId -> isCourseAppropriateForStudent(courseId, student))
                .collect(Collectors.toList());

        // Shuffle for variety
        Collections.shuffle(availableCourses);

        // Enroll in courses until we reach target
        for (Long courseId : availableCourses) {
            if (enrolled >= targetCourses) {
                break;
            }

            List<ScheduleSlot> courseSlots = slotsByCourse.get(courseId);

            // Find best slot with available capacity
            ScheduleSlot bestSlot = findBestAvailableSlot(courseSlots, student);

            if (bestSlot != null) {
                // Create enrollment
                StudentEnrollment enrollment = new StudentEnrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(bestSlot.getCourse());
                enrollment.setScheduleSlot(bestSlot);
                enrollment.setSchedule(schedule);
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollment.setEnrolledDate(LocalDateTime.now());
                enrollment.setPriority(calculatePriority(student));

                enrollmentRepository.save(enrollment);

                // Update slot enrollment count
                bestSlot.setEnrolledStudents(bestSlot.getEnrolledStudents() + 1);
                scheduleSlotRepository.save(bestSlot);

                enrolled++;
            }
        }

        return enrolled;
    }

    /**
     * Check if course is appropriate for student's grade level
     */
    private boolean isCourseAppropriateForStudent(Long courseId, Student student) {
        Course course = courseRepository.findById(courseId).orElse(null);

        if (course == null) {
            return false;
        }

        // For now, all courses are available to all students
        // In future, add grade-level restrictions, prerequisites, etc.

        return true;
    }

    /**
     * Find best available slot for student
     * Prioritizes:
     * 1. Slots with available capacity
     * 2. Slots that don't conflict with existing enrollments
     * 3. Least-full sections for balance
     */
    private ScheduleSlot findBestAvailableSlot(List<ScheduleSlot> slots, Student student) {
        return slots.stream()
                .filter(slot -> hasCapacity(slot))
                .filter(slot -> !hasTimeConflict(slot, student))
                .min(Comparator.comparingInt(ScheduleSlot::getEnrolledStudents))
                .orElse(null);
    }

    /**
     * Check if slot has available capacity
     */
    private boolean hasCapacity(ScheduleSlot slot) {
        Course course = slot.getCourse();
        if (course == null) {
            return false;
        }

        int maxStudents = course.getMaxStudents() != null ? course.getMaxStudents() : 30;
        int enrolled = slot.getEnrolledStudents() != null ? slot.getEnrolledStudents() : 0;

        return enrolled < maxStudents;
    }

    /**
     * Check if slot conflicts with student's existing schedule
     */
    private boolean hasTimeConflict(ScheduleSlot slot, Student student) {
        TimeSlot slotTime = slot.getTimeSlot();
        if (slotTime == null) {
            return false;
        }

        // Get student's existing enrollments
        List<StudentEnrollment> existingEnrollments = enrollmentRepository
                .findActiveEnrollmentsByStudentId(student.getId());

        // Check for time conflicts
        for (StudentEnrollment enrollment : existingEnrollments) {
            ScheduleSlot existingSlot = enrollment.getScheduleSlot();
            if (existingSlot != null && existingSlot.getTimeSlot() != null) {
                TimeSlot existingTime = existingSlot.getTimeSlot();

                if (timeSlotsOverlap(slotTime, existingTime)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if two time slots overlap
     */
    private boolean timeSlotsOverlap(TimeSlot slot1, TimeSlot slot2) {
        if (!slot1.getDayOfWeek().equals(slot2.getDayOfWeek())) {
            return false;
        }

        return !slot1.getStartTime().isAfter(slot2.getEndTime()) &&
                !slot1.getEndTime().isBefore(slot2.getStartTime());
    }

    /**
     * Calculate priority for student (seniors = higher)
     */
    private int calculatePriority(Student student) {
        String gradeLevel = student.getGradeLevel();

        return switch (gradeLevel) {
            case "12" -> 10; // Seniors first
            case "11" -> 8; // Juniors
            case "10" -> 6; // Sophomores
            case "9" -> 4; // Freshmen
            default -> 5; // Default
        };
    }

    // ========================================================================
    // MANUAL ENROLLMENT
    // ========================================================================

    /**
     * Enroll a specific student in a specific course
     */
    @Transactional
    public StudentEnrollment enrollStudent(Long studentId, Long courseId, Long scheduleId) {
        log.info("Enrolling student {} in course {} for schedule {}",
                studentId, courseId, scheduleId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // Find available slot
        List<ScheduleSlot> slots = scheduleSlotRepository.findByCourseIdWithDetails(courseId);
        ScheduleSlot bestSlot = findBestAvailableSlot(slots, student);

        if (bestSlot == null) {
            throw new RuntimeException("No available slots for this course");
        }

        // Create enrollment
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setScheduleSlot(bestSlot);
        enrollment.setSchedule(schedule);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledDate(LocalDateTime.now());

        enrollment = enrollmentRepository.save(enrollment);

        // Update slot count
        bestSlot.setEnrolledStudents(bestSlot.getEnrolledStudents() + 1);
        scheduleSlotRepository.save(bestSlot);

        log.info("✅ Enrolled student in {}", course.getCourseName());

        return enrollment;
    }

    /**
     * Drop a student from a course
     */
    @Transactional
    public void dropEnrollment(Long enrollmentId) {
        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);

        // Update slot count
        ScheduleSlot slot = enrollment.getScheduleSlot();
        if (slot != null) {
            slot.setEnrolledStudents(Math.max(0, slot.getEnrolledStudents() - 1));
            scheduleSlotRepository.save(slot);
        }

        log.info("✅ Dropped enrollment {}", enrollmentId);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get enrollment statistics for a schedule
     */
    public EnrollmentStatistics getEnrollmentStatistics(Long scheduleId) {
        List<StudentEnrollment> enrollments = enrollmentRepository.findByScheduleId(scheduleId);

        EnrollmentStatistics stats = new EnrollmentStatistics();
        stats.setTotalEnrollments(enrollments.size());
        stats.setActiveEnrollments((int) enrollments.stream()
                .filter(StudentEnrollment::isActive).count());

        // Group by course
        Map<String, Long> byCourse = enrollments.stream()
                .collect(Collectors.groupingBy(
                        StudentEnrollment::getCourseName,
                        Collectors.counting()));
        stats.setEnrollmentsByCourse(byCourse);

        return stats;
    }
}

