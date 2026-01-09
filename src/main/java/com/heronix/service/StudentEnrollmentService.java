package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Student;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.CourseSectionRepository;
import com.heronix.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing student course enrollments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Service
@Transactional
public class StudentEnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentEnrollmentService.class);

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final PrerequisiteValidationService prerequisiteValidationService;

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    public StudentEnrollmentService(StudentRepository studentRepository,
                                   CourseRepository courseRepository,
                                   CourseSectionRepository courseSectionRepository,
                                   PrerequisiteValidationService prerequisiteValidationService) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.courseSectionRepository = courseSectionRepository;
        this.prerequisiteValidationService = prerequisiteValidationService;
    }

    // ========================================================================
    // ENROLLMENT OPERATIONS
    // ========================================================================

    /**
     * Enroll a student in a course
     */
    public void enrollStudent(Long studentId, Long courseId) {
        logger.info("Enrolling student {} in course {}", studentId, courseId);

        // Use findByIdWithEnrolledCourses to avoid LazyInitializationException
        Optional<Student> studentOpt = studentRepository.findByIdWithEnrolledCourses(studentId);
        Optional<Course> courseOpt = courseRepository.findById(courseId);

        if (studentOpt.isEmpty()) {
            throw new IllegalArgumentException("Student not found with ID: " + studentId);
        }
        if (courseOpt.isEmpty()) {
            throw new IllegalArgumentException("Course not found with ID: " + courseId);
        }

        Student student = studentOpt.get();
        Course course = courseOpt.get();

        // Check if already enrolled
        if (student.getEnrolledCourses().contains(course)) {
            logger.warn("Student {} is already enrolled in course {}", studentId, courseId);
            return;
        }

        // Check course capacity
        if (course.isFull()) {
            throw new IllegalStateException("Course " + course.getCourseCode() + " is at full capacity");
        }

        // Check prerequisites (basic implementation)
        if (!checkPrerequisites(student, course)) {
            throw new IllegalStateException("Student does not meet prerequisites for course " + course.getCourseCode());
        }

        // Enroll student
        student.getEnrolledCourses().add(course);
        course.setCurrentEnrollment(course.getCurrentEnrollment() + 1);

        studentRepository.save(student);
        courseRepository.save(course);

        logger.info("Successfully enrolled student {} in course {}", student.getStudentId(), course.getCourseCode());
    }

    /**
     * Unenroll a student from a course
     */
    public void unenrollStudent(Long studentId, Long courseId) {
        logger.info("Unenrolling student {} from course {}", studentId, courseId);

        // Use findByIdWithEnrolledCourses to avoid LazyInitializationException
        Optional<Student> studentOpt = studentRepository.findByIdWithEnrolledCourses(studentId);
        Optional<Course> courseOpt = courseRepository.findById(courseId);

        if (studentOpt.isEmpty() || courseOpt.isEmpty()) {
            logger.warn("Student or course not found");
            return;
        }

        Student student = studentOpt.get();
        Course course = courseOpt.get();

        if (student.getEnrolledCourses().remove(course)) {
            course.setCurrentEnrollment(Math.max(0, course.getCurrentEnrollment() - 1));
            studentRepository.save(student);
            courseRepository.save(course);
            logger.info("Successfully unenrolled student {} from course {}", student.getStudentId(), course.getCourseCode());
        }
    }

    /**
     * Bulk enroll students in a course
     */
    public void bulkEnrollStudents(List<Long> studentIds, Long courseId) {
        logger.info("Bulk enrolling {} students in course {}", studentIds.size(), courseId);

        int successCount = 0;
        int failCount = 0;

        for (Long studentId : studentIds) {
            try {
                enrollStudent(studentId, courseId);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to enroll student {}: {}", studentId, e.getMessage());
                failCount++;
            }
        }

        logger.info("Bulk enrollment complete: {} succeeded, {} failed", successCount, failCount);
    }

    /**
     * Bulk enroll a student in multiple courses
     */
    public void bulkEnrollCourses(Long studentId, List<Long> courseIds) {
        logger.info("Bulk enrolling student {} in {} courses", studentId, courseIds.size());

        int successCount = 0;
        int failCount = 0;

        for (Long courseId : courseIds) {
            try {
                enrollStudent(studentId, courseId);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to enroll in course {}: {}", courseId, e.getMessage());
                failCount++;
            }
        }

        logger.info("Bulk enrollment complete: {} succeeded, {} failed", successCount, failCount);
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Get all courses a student is enrolled in
     */
    @Transactional(readOnly = true)
    public List<Course> getStudentEnrollments(Long studentId) {
        return studentRepository.findById(studentId)
                .map(student -> {
                    // Force initialization of lazy-loaded collection within transaction
                    List<Course> courses = student.getEnrolledCourses();
                    courses.size(); // Trigger initialization
                    return new ArrayList<>(courses); // Return detached copy
                })
                .orElse(new ArrayList<>());
    }

    /**
     * Get all students enrolled in a course
     */
    @Transactional(readOnly = true)
    public List<Student> getCourseEnrollments(Long courseId) {
        return courseRepository.findById(courseId)
                .map(Course::getStudents)
                .orElse(new ArrayList<>());
    }

    /**
     * Get available courses for a student (not yet enrolled, has capacity)
     */
    @Transactional(readOnly = true)
    public List<Course> getAvailableCoursesForStudent(Long studentId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Student student = studentOpt.get();
        List<Course> enrolledCourses = student.getEnrolledCourses();

        return courseRepository.findByActiveTrue().stream()
                .filter(course -> !enrolledCourses.contains(course))
                .filter(course -> !course.isFull())
                .filter(course -> checkPrerequisites(student, course))
                .collect(Collectors.toList());
    }

    /**
     * Get courses by education level for a student
     */
    @Transactional(readOnly = true)
    public List<Course> getAvailableCoursesByLevel(Long studentId, String level) {
        return getAvailableCoursesForStudent(studentId).stream()
                .filter(course -> course.getLevel() != null &&
                                  course.getLevel().toString().equalsIgnoreCase(level))
                .collect(Collectors.toList());
    }

    /**
     * Get courses by subject for a student
     */
    @Transactional(readOnly = true)
    public List<Course> getAvailableCoursesBySubject(Long studentId, String subject) {
        return getAvailableCoursesForStudent(studentId).stream()
                .filter(course -> course.getSubject() != null &&
                                  course.getSubject().toLowerCase().contains(subject.toLowerCase()))
                .collect(Collectors.toList());
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Check if student meets prerequisites for a course
     * Uses PrerequisiteValidationService for comprehensive validation
     */
    private boolean checkPrerequisites(Student student, Course course) {
        PrerequisiteValidationService.ValidationResult result =
            prerequisiteValidationService.validatePrerequisites(student, course);

        if (!result.isCanEnroll()) {
            logger.warn("Student {} cannot enroll in course {}: {}",
                       student.getId(), course.getCourseName(), result.getMessage());
        }

        return result.isCanEnroll();
    }

    /**
     * Get detailed prerequisite validation result
     * Public method for UI to show detailed prerequisite information
     */
    public PrerequisiteValidationService.ValidationResult getPrerequisiteValidation(Long studentId, Long courseId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Course> courseOpt = courseRepository.findById(courseId);

        if (studentOpt.isEmpty() || courseOpt.isEmpty()) {
            return new PrerequisiteValidationService.ValidationResult(false, "Student or course not found");
        }

        return prerequisiteValidationService.validatePrerequisites(studentOpt.get(), courseOpt.get());
    }

    /**
     * Validate enrollment is possible
     */
    public boolean canEnroll(Long studentId, Long courseId) {
        try {
            Optional<Student> studentOpt = studentRepository.findById(studentId);
            Optional<Course> courseOpt = courseRepository.findById(courseId);

            if (studentOpt.isEmpty() || courseOpt.isEmpty()) {
                return false;
            }

            Student student = studentOpt.get();
            Course course = courseOpt.get();

            // Check if already enrolled
            if (student.getEnrolledCourses().contains(course)) {
                return false;
            }

            // Check capacity
            if (course.isFull()) {
                return false;
            }

            // Check prerequisites
            if (!checkPrerequisites(student, course)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Error checking enrollment eligibility", e);
            return false;
        }
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get enrollment statistics for a student
     */
    @Transactional(readOnly = true)
    public EnrollmentStats getStudentStats(Long studentId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return new EnrollmentStats(0, 0, 0.0);
        }

        Student student = studentOpt.get();
        int enrolledCount = student.getEnrolledCourseCount();
        int totalCredits = student.getEnrolledCourses().stream()
                .mapToInt(course -> course.getDurationMinutes() != null ? course.getDurationMinutes() / 50 : 1)
                .sum();

        return new EnrollmentStats(enrolledCount, totalCredits, enrolledCount > 0 ? (double) totalCredits / enrolledCount : 0.0);
    }

    /**
     * Get enrollment statistics for a course
     */
    @Transactional(readOnly = true)
    public CourseEnrollmentStats getCourseStats(Long courseId) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return new CourseEnrollmentStats(0, 0, 0.0);
        }

        Course course = courseOpt.get();
        int enrolled = course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0;
        int capacity = course.getMaxStudents() != null ? course.getMaxStudents() : 30;
        double percentage = capacity > 0 ? (enrolled * 100.0 / capacity) : 0.0;

        return new CourseEnrollmentStats(enrolled, capacity, percentage);
    }

    // ========================================================================
    // MID-YEAR AUTO-PLACEMENT (Added for Session 4)
    // ========================================================================

    /**
     * Automatically place a mid-year student into appropriate sections
     * Finds best available sections based on capacity, balance, and schedule conflicts
     *
     * @param studentId The student to place
     * @param requestedCourseIds List of course IDs student needs
     * @return Placement result with successful and failed placements
     */
    @Transactional
    public AutoPlacementResult autoPlaceStudent(Long studentId, List<Long> requestedCourseIds) {
        logger.info("Auto-placing student {} in {} courses", studentId, requestedCourseIds.size());

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return new AutoPlacementResult(new ArrayList<>(), requestedCourseIds,
                "Student not found");
        }

        Student student = studentOpt.get();
        List<Long> successfulPlacements = new ArrayList<>();
        List<Long> failedPlacements = new ArrayList<>();

        for (Long courseId : requestedCourseIds) {
            Optional<CourseSection> bestSection = findBestAvailableSection(student, courseId);

            if (bestSection.isPresent()) {
                try {
                    CourseSection section = bestSection.get();
                    placementStudentInSection(student, section);
                    successfulPlacements.add(courseId);
                    logger.info("Successfully placed student {} in section {} for course {}",
                        student.getId(), section.getId(), courseId);
                } catch (Exception e) {
                    logger.error("Failed to place student in section: {}", e.getMessage());
                    failedPlacements.add(courseId);
                }
            } else {
                failedPlacements.add(courseId);
                logger.warn("No available section found for student {} in course {}", studentId, courseId);
            }
        }

        String message = String.format("Placed in %d/%d courses",
            successfulPlacements.size(), requestedCourseIds.size());

        return new AutoPlacementResult(successfulPlacements, failedPlacements, message);
    }

    /**
     * Find the best available section for a student in a specific course
     * Considers: capacity, schedule conflicts, section balance
     *
     * @param student The student
     * @param courseId The course
     * @return Optional best section
     */
    @Transactional(readOnly = true)
    public Optional<CourseSection> findBestAvailableSection(Student student, Long courseId) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return Optional.empty();
        }

        Course course = courseOpt.get();

        // Get all sections for this course
        List<CourseSection> sections = courseSectionRepository.findAll().stream()
            .filter(s -> s.getCourse() != null && s.getCourse().getId().equals(courseId))
            .filter(s -> s.getSectionStatus() == CourseSection.SectionStatus.OPEN ||
                        s.getSectionStatus() == CourseSection.SectionStatus.SCHEDULED)
            .collect(Collectors.toList());

        if (sections.isEmpty()) {
            return Optional.empty();
        }

        // Filter out full sections
        List<CourseSection> availableSections = sections.stream()
            .filter(s -> !isSectionFull(s))
            .collect(Collectors.toList());

        if (availableSections.isEmpty()) {
            return Optional.empty();
        }

        // Filter out sections with schedule conflicts
        List<CourseSection> conflictFreeSections = availableSections.stream()
            .filter(s -> !hasScheduleConflict(student, s))
            .collect(Collectors.toList());

        if (conflictFreeSections.isEmpty()) {
            logger.warn("All available sections have conflicts for student {}", student.getId());
            // Return the section with most space if no conflict-free sections
            return availableSections.stream()
                .max(Comparator.comparingInt(this::getAvailableSeats));
        }

        // Select best section based on multiple criteria
        return conflictFreeSections.stream()
            .max(Comparator
                .comparingInt(this::getBalanceScore)  // Prefer balanced sections
                .thenComparingInt(this::getAvailableSeats));  // Then most available space
    }

    /**
     * Find alternate section if primary section is full or has conflicts
     *
     * @param student The student
     * @param courseId The course
     * @param excludeSectionId Section to exclude from search
     * @return Optional alternate section
     */
    @Transactional(readOnly = true)
    public Optional<CourseSection> findAlternateSection(Student student, Long courseId, Long excludeSectionId) {
        List<CourseSection> sections = courseSectionRepository.findAll().stream()
            .filter(s -> s.getCourse() != null && s.getCourse().getId().equals(courseId))
            .filter(s -> !s.getId().equals(excludeSectionId))
            .filter(s -> !isSectionFull(s))
            .filter(s -> !hasScheduleConflict(student, s))
            .collect(Collectors.toList());

        return sections.stream()
            .max(Comparator.comparingInt(this::getBalanceScore));
    }

    /**
     * Get list of available sections for a student in a course
     * Useful for presenting options to user
     *
     * @param studentId The student
     * @param courseId The course
     * @return List of available sections with details
     */
    @Transactional(readOnly = true)
    public List<SectionOption> getAvailableSectionOptions(Long studentId, Long courseId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Student student = studentOpt.get();

        List<CourseSection> sections = courseSectionRepository.findAll().stream()
            .filter(s -> s.getCourse() != null && s.getCourse().getId().equals(courseId))
            .filter(s -> !isSectionFull(s))
            .collect(Collectors.toList());

        return sections.stream()
            .map(section -> {
                boolean hasConflict = hasScheduleConflict(student, section);
                int availableSeats = getAvailableSeats(section);
                int balanceScore = getBalanceScore(section);

                return new SectionOption(
                    section.getId(),
                    section.getSectionNumber(),
                    section.getAssignedTeacher() != null ?
                        section.getAssignedTeacher().getFirstName() + " " +
                        section.getAssignedTeacher().getLastName() : "TBA",
                    section.getAssignedPeriod() != null ? section.getAssignedPeriod() : 0,
                    section.getAssignedRoom() != null ? section.getAssignedRoom().getRoomNumber() : "TBA",
                    availableSeats,
                    hasConflict,
                    balanceScore
                );
            })
            .sorted(Comparator
                .comparing(SectionOption::isHasConflict)  // Non-conflicting first
                .thenComparing(Comparator.comparingInt(SectionOption::getBalanceScore).reversed())
                .thenComparing(Comparator.comparingInt(SectionOption::getAvailableSeats).reversed()))
            .collect(Collectors.toList());
    }

    /**
     * Actually enroll student in a specific section
     */
    private void placementStudentInSection(Student student, CourseSection section) {
        // Add to course enrollment
        if (!student.getEnrolledCourses().contains(section.getCourse())) {
            student.getEnrolledCourses().add(section.getCourse());
        }

        // Update section enrollment
        int currentEnrollment = section.getCurrentEnrollment() != null ?
            section.getCurrentEnrollment() : 0;
        section.setCurrentEnrollment(currentEnrollment + 1);

        // Update status if now full
        if (isSectionFull(section)) {
            section.setSectionStatus(CourseSection.SectionStatus.FULL);
        }

        studentRepository.save(student);
        courseSectionRepository.save(section);
    }

    /**
     * Check if section is full
     */
    private boolean isSectionFull(CourseSection section) {
        int current = section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0;
        int max = section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 30;
        return current >= max;
    }

    /**
     * Get number of available seats in section
     */
    private int getAvailableSeats(CourseSection section) {
        int current = section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0;
        int max = section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 30;
        return Math.max(0, max - current);
    }

    /**
     * Calculate balance score for section
     * Higher score = more balanced with other sections of same course
     */
    private int getBalanceScore(CourseSection section) {
        // Get all sections of same course
        List<CourseSection> courseSections = courseSectionRepository.findAll().stream()
            .filter(s -> s.getCourse().getId().equals(section.getCourse().getId()))
            .collect(Collectors.toList());

        if (courseSections.size() <= 1) {
            return 50;  // Neutral score for single section
        }

        // Calculate average enrollment
        double avgEnrollment = courseSections.stream()
            .mapToInt(s -> s.getCurrentEnrollment() != null ? s.getCurrentEnrollment() : 0)
            .average()
            .orElse(0.0);

        // Calculate this section's deviation from average
        int thisEnrollment = section.getCurrentEnrollment() != null ?
            section.getCurrentEnrollment() : 0;
        double deviation = Math.abs(thisEnrollment - avgEnrollment);

        // Lower deviation = higher balance score (inverted)
        int maxScore = 100;
        int deviationPenalty = (int) (deviation * 5);  // Each student off average costs 5 points

        return Math.max(0, maxScore - deviationPenalty);
    }

    /**
     * Check if student has schedule conflict with section
     * Uses ConflictDetectionService if available
     */
    private boolean hasScheduleConflict(Student student, CourseSection section) {
        if (section.getAssignedPeriod() == null) {
            return false;  // No schedule assigned yet, no conflict
        }

        // Get student's current schedule
        List<CourseSection> studentSections = courseSectionRepository.findAll().stream()
            .filter(s -> s.getCourse() != null)
            .filter(s -> student.getEnrolledCourses().contains(s.getCourse()))
            .filter(s -> s.getAssignedPeriod() != null)
            .collect(Collectors.toList());

        // Check for same period conflict
        return studentSections.stream()
            .anyMatch(s -> s.getAssignedPeriod().equals(section.getAssignedPeriod()));
    }

    // ========================================================================
    // HELPER CLASSES
    // ========================================================================

    public static class EnrollmentStats {
        private final int enrolledCourses;
        private final int totalCredits;
        private final double averageCreditsPerCourse;

        public EnrollmentStats(int enrolledCourses, int totalCredits, double averageCreditsPerCourse) {
            this.enrolledCourses = enrolledCourses;
            this.totalCredits = totalCredits;
            this.averageCreditsPerCourse = averageCreditsPerCourse;
        }

        public int getEnrolledCourses() { return enrolledCourses; }
        public int getTotalCredits() { return totalCredits; }
        public double getAverageCreditsPerCourse() { return averageCreditsPerCourse; }
    }

    public static class CourseEnrollmentStats {
        private final int enrolled;
        private final int capacity;
        private final double percentageFull;

        public CourseEnrollmentStats(int enrolled, int capacity, double percentageFull) {
            this.enrolled = enrolled;
            this.capacity = capacity;
            this.percentageFull = percentageFull;
        }

        public int getEnrolled() { return enrolled; }
        public int getCapacity() { return capacity; }
        public double getPercentageFull() { return percentageFull; }
        public int getAvailableSeats() { return capacity - enrolled; }
    }

    /**
     * Result of auto-placement operation
     */
    public static class AutoPlacementResult {
        private final List<Long> successfulPlacements;
        private final List<Long> failedPlacements;
        private final String message;

        public AutoPlacementResult(List<Long> successfulPlacements, List<Long> failedPlacements, String message) {
            this.successfulPlacements = successfulPlacements;
            this.failedPlacements = failedPlacements;
            this.message = message;
        }

        public List<Long> getSuccessfulPlacements() { return successfulPlacements; }
        public List<Long> getFailedPlacements() { return failedPlacements; }
        public String getMessage() { return message; }
        public boolean isFullySuccessful() { return failedPlacements.isEmpty(); }
        public boolean isPartiallySuccessful() { return !successfulPlacements.isEmpty() && !failedPlacements.isEmpty(); }
        public boolean isCompleteFailure() { return successfulPlacements.isEmpty(); }
    }

    /**
     * Available section option for student placement
     */
    public static class SectionOption {
        private final Long sectionId;
        private final String sectionNumber;
        private final String teacherName;
        private final int period;
        private final String roomNumber;
        private final int availableSeats;
        private final boolean hasConflict;
        private final int balanceScore;

        public SectionOption(Long sectionId, String sectionNumber, String teacherName, int period,
                           String roomNumber, int availableSeats, boolean hasConflict, int balanceScore) {
            this.sectionId = sectionId;
            this.sectionNumber = sectionNumber;
            this.teacherName = teacherName;
            this.period = period;
            this.roomNumber = roomNumber;
            this.availableSeats = availableSeats;
            this.hasConflict = hasConflict;
            this.balanceScore = balanceScore;
        }

        public Long getSectionId() { return sectionId; }
        public String getSectionNumber() { return sectionNumber; }
        public String getTeacherName() { return teacherName; }
        public int getPeriod() { return period; }
        public String getRoomNumber() { return roomNumber; }
        public int getAvailableSeats() { return availableSeats; }
        public boolean isHasConflict() { return hasConflict; }
        public int getBalanceScore() { return balanceScore; }

        public String getRecommendation() {
            if (hasConflict) return "⚠️ Schedule Conflict";
            if (balanceScore >= 80) return "✅ Best Choice";
            if (balanceScore >= 60) return "👍 Good Choice";
            return "⚖️ Available";
        }

        @Override
        public String toString() {
            return String.format("Section %s - %s (P%d) - %s - %d seats - %s",
                sectionNumber, teacherName, period, roomNumber, availableSeats, getRecommendation());
        }
    }
}
