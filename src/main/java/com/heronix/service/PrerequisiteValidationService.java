package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CoursePrerequisite;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentCourseHistory;
import com.heronix.repository.CoursePrerequisiteRepository;
import com.heronix.repository.StudentCourseHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating course prerequisites and managing student course history
 *
 * This service is the core of Phase 7E and provides:
 * - Prerequisite validation before enrollment
 * - Course history tracking
 * - Qualification checking
 * - Grade comparison logic
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7E - November 21, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PrerequisiteValidationService {

    private final CoursePrerequisiteRepository prerequisiteRepository;
    private final StudentCourseHistoryRepository historyRepository;

    /**
     * Validate prerequisites for enrollment (public API for StudentEnrollmentService)
     *
     * @param student The student
     * @param course The course they want to enroll in
     * @return ValidationResult with simple canEnroll flag and message
     */
    public ValidationResult validatePrerequisites(Student student, Course course) {
        PrerequisiteCheckResult detailedResult = checkPrerequisites(student, course);

        return new ValidationResult(
            detailedResult.isMeetsPrerequisites(),
            detailedResult.getFailureReason()
        );
    }

    /**
     * Check if student meets prerequisites for a course (internal detailed check)
     *
     * @param student The student
     * @param course The course they want to enroll in
     * @return Result with details of prerequisite check
     */
    public PrerequisiteCheckResult checkPrerequisites(Student student, Course course) {
        log.debug("Checking prerequisites for student {} and course {}",
                  student.getStudentId(), course.getCourseCode());

        PrerequisiteCheckResult result = new PrerequisiteCheckResult();
        result.setCourse(course);
        result.setStudent(student);

        // Get all prerequisites for this course
        List<CoursePrerequisite> prerequisites = prerequisiteRepository.findByCourse(course);

        if (prerequisites.isEmpty()) {
            // No prerequisites = automatically qualified
            result.setMeetsPrerequisites(true);
            result.setFailureReason("No prerequisites required");
            log.debug("Course {} has no prerequisites", course.getCourseCode());
            return result;
        }

        // Get student's completed courses
        List<StudentCourseHistory> completedCourses = historyRepository.findCompletedCourses(student);
        // ✅ NULL SAFE: Filter out histories with null Course references
        Map<Long, StudentCourseHistory> completedCoursesMap = completedCourses.stream()
            .filter(h -> h.getCourse() != null)
            .collect(Collectors.toMap(
                h -> h.getCourse().getId(),
                h -> h,
                (h1, h2) -> h1.getCompletionDate() != null &&
                           (h2.getCompletionDate() == null ||
                            h1.getCompletionDate().isAfter(h2.getCompletionDate())) ? h1 : h2
            ));

        // Group prerequisites by prerequisite group
        Map<Integer, List<CoursePrerequisite>> groupedPrereqs = prerequisites.stream()
            .collect(Collectors.groupingBy(CoursePrerequisite::getPrerequisiteGroup));

        boolean allGroupsSatisfied = true;
        List<String> missingPrereqs = new ArrayList<>();
        List<String> satisfiedPrereqs = new ArrayList<>();

        // Check each group (AND logic between groups, OR logic within groups)
        for (Map.Entry<Integer, List<CoursePrerequisite>> entry : groupedPrereqs.entrySet()) {
            List<CoursePrerequisite> groupPrereqs = entry.getValue();

            boolean groupSatisfied = false;
            List<String> groupMissing = new ArrayList<>();

            // Check if ANY prerequisite in this group is satisfied (OR logic)
            for (CoursePrerequisite prereq : groupPrereqs) {
                Course prereqCourse = prereq.getPrerequisiteCourse();
                // ✅ NULL SAFE: Skip prerequisites with null course references
                if (prereqCourse == null) {
                    continue;
                }
                String minimumGrade = prereq.getMinimumGrade();

                StudentCourseHistory history = completedCoursesMap.get(prereqCourse.getId());

                if (history != null) {
                    // Student has completed this prerequisite course
                    boolean meetsGradeRequirement = true;

                    if (minimumGrade != null && !minimumGrade.isEmpty()) {
                        meetsGradeRequirement = history.meetsMinimumGrade(minimumGrade);
                    }

                    if (meetsGradeRequirement) {
                        groupSatisfied = true;
                        String satisfiedMsg = String.format("%s - %s (Grade: %s)",
                            prereqCourse.getCourseCode(),
                            prereqCourse.getCourseName(),
                            history.getGradeReceived() != null ? history.getGradeReceived() : "N/A");
                        satisfiedPrereqs.add(satisfiedMsg);
                        log.debug("Prerequisite satisfied: {}", satisfiedMsg);
                        break; // One satisfied prereq in group is enough (OR logic)
                    } else {
                        String msg = String.format("%s - %s (Grade: %s, Required: %s or better)",
                            prereqCourse.getCourseCode(),
                            prereqCourse.getCourseName(),
                            history.getGradeReceived(),
                            minimumGrade);
                        groupMissing.add(msg);
                    }
                } else {
                    // Student has not completed this prerequisite
                    String msg = String.format("%s - %s%s",
                        prereqCourse.getCourseCode(),
                        prereqCourse.getCourseName(),
                        minimumGrade != null ? " (Min Grade: " + minimumGrade + ")" : "");
                    groupMissing.add(msg);
                }
            }

            if (!groupSatisfied) {
                allGroupsSatisfied = false;
                if (groupPrereqs.size() > 1) {
                    missingPrereqs.add("One of: " + String.join(" OR ", groupMissing));
                } else {
                    missingPrereqs.addAll(groupMissing);
                }
            }
        }

        result.setMeetsPrerequisites(allGroupsSatisfied);
        result.setMissingPrerequisites(missingPrereqs);
        result.setSatisfiedPrerequisites(satisfiedPrereqs);

        // Check if any prerequisites are recommended (not required)
        // ✅ NULL SAFE: Handle null getIsRequired() values
        boolean hasRecommendedOnly = prerequisites.stream()
            .allMatch(p -> p.getIsRequired() != null && !p.getIsRequired());

        result.setAllowOverride(!allGroupsSatisfied && !hasRecommendedOnly);

        if (allGroupsSatisfied) {
            result.setFailureReason("All prerequisites met");
        } else if (hasRecommendedOnly) {
            result.setFailureReason("Recommended prerequisites not met (enrollment allowed with warning)");
        } else {
            result.setFailureReason("Required prerequisites not met: " +
                                   String.join("; ", missingPrereqs));
        }

        log.info("Prerequisite check for {} enrolling in {}: {}",
                 student.getStudentId(), course.getCourseCode(),
                 allGroupsSatisfied ? "PASSED" : "FAILED");

        return result;
    }

    /**
     * Get all prerequisite requirements for a course
     *
     * @param course The course
     * @return List of prerequisites organized by group
     */
    public List<CoursePrerequisite> getPrerequisites(Course course) {
        return prerequisiteRepository.findByCourse(course);
    }

    /**
     * Get student's course history
     *
     * @param student The student
     * @return List of all courses taken
     */
    public List<StudentCourseHistory> getStudentHistory(Student student) {
        return historyRepository.findByStudent(student);
    }

    /**
     * Get student's completed courses only
     *
     * @param student The student
     * @return List of successfully completed courses
     */
    public List<StudentCourseHistory> getCompletedCourses(Student student) {
        return historyRepository.findCompletedCourses(student);
    }

    /**
     * Check if student has completed a specific course
     *
     * @param student The student
     * @param course The course
     * @param minimumGrade Minimum grade required (null = any passing grade)
     * @return true if student has completed the course with minimum grade
     */
    public boolean hasCompletedCourse(Student student, Course course, String minimumGrade) {
        Optional<StudentCourseHistory> history =
            historyRepository.findCompletedCourse(student, course);

        if (history.isEmpty()) {
            return false;
        }

        if (minimumGrade == null || minimumGrade.isEmpty()) {
            return true; // Course completed, no grade requirement
        }

        return history
            .map(h -> h.meetsMinimumGrade(minimumGrade))
            .orElse(false);
    }

    /**
     * Get courses student is qualified for based on prerequisites
     *
     * @param student The student
     * @param availableCourses List of courses to check
     * @return List of courses student can take
     */
    public List<Course> getQualifiedCourses(Student student, List<Course> availableCourses) {
        return availableCourses.stream()
            .filter(course -> checkPrerequisites(student, course).isMeetsPrerequisites())
            .collect(Collectors.toList());
    }

    /**
     * Get courses student is NOT qualified for with reasons
     *
     * @param student The student
     * @param availableCourses List of courses to check
     * @return Map of unqualified courses to failure reasons
     */
    public Map<Course, String> getUnqualifiedCourses(Student student, List<Course> availableCourses) {
        Map<Course, String> unqualified = new LinkedHashMap<>();

        for (Course course : availableCourses) {
            PrerequisiteCheckResult result = checkPrerequisites(student, course);
            if (!result.isMeetsPrerequisites()) {
                unqualified.put(course, result.getFailureReason());
            }
        }

        return unqualified;
    }

    /**
     * Get prerequisite chain for a course
     * Shows: Course A → Course B → Course C
     *
     * @param course The course
     * @return List of courses in prerequisite chain
     */
    public List<Course> getPrerequisiteChain(Course course) {
        List<Course> chain = new ArrayList<>();
        Set<Long> visited = new HashSet<>(); // Prevent circular references
        buildPrerequisiteChain(course, chain, visited);
        Collections.reverse(chain); // Reverse to show earliest → latest
        return chain;
    }

    /**
     * Recursive helper to build prerequisite chain
     */
    private void buildPrerequisiteChain(Course course, List<Course> chain, Set<Long> visited) {
        if (course == null || visited.contains(course.getId())) {
            return; // Avoid circular references
        }

        visited.add(course.getId());
        chain.add(course);

        // Get prerequisites for this course
        List<CoursePrerequisite> prereqs = prerequisiteRepository.findByCourse(course);

        if (!prereqs.isEmpty()) {
            // Follow the first prerequisite in first group (simplified chain)
            CoursePrerequisite firstPrereq = prereqs.stream()
                .min(Comparator.comparing(CoursePrerequisite::getPrerequisiteGroup))
                .orElse(null);

            // ✅ NULL SAFE: Check both prereq and its course before recursion
            if (firstPrereq != null && firstPrereq.getPrerequisiteCourse() != null) {
                buildPrerequisiteChain(firstPrereq.getPrerequisiteCourse(), chain, visited);
            }
        }
    }

    /**
     * Get formatted prerequisite description for display
     *
     * @param course The course
     * @return Human-readable prerequisite description
     */
    public String getPrerequisiteDescription(Course course) {
        List<CoursePrerequisite> prereqs = prerequisiteRepository.findByCourse(course);

        if (prereqs.isEmpty()) {
            return "No prerequisites";
        }

        Map<Integer, List<CoursePrerequisite>> grouped = prereqs.stream()
            .collect(Collectors.groupingBy(CoursePrerequisite::getPrerequisiteGroup));

        List<String> groupDescriptions = new ArrayList<>();

        for (Map.Entry<Integer, List<CoursePrerequisite>> entry : grouped.entrySet()) {
            List<String> options = entry.getValue().stream()
                .map(CoursePrerequisite::getDisplayString)
                .collect(Collectors.toList());

            if (options.size() > 1) {
                groupDescriptions.add("(" + String.join(" OR ", options) + ")");
            } else {
                groupDescriptions.add(options.get(0));
            }
        }

        return String.join(" AND ", groupDescriptions);
    }

    /**
     * Calculate student's total credits earned
     *
     * @param student The student
     * @return Total credits
     */
    public Double calculateTotalCredits(Student student) {
        return historyRepository.calculateTotalCredits(student);
    }

    /**
     * Calculate student's GPA
     *
     * @param student The student
     * @return GPA on 4.0 scale
     */
    public Double calculateGPA(Student student) {
        Double gpa = historyRepository.calculateGPA(student.getId());
        return gpa != null ? gpa : 0.0;
    }

    /**
     * Simple validation result for StudentEnrollmentService API
     */
    public static class ValidationResult {
        private final boolean canEnroll;
        private final String message;

        public ValidationResult(boolean canEnroll, String message) {
            this.canEnroll = canEnroll;
            this.message = message;
        }

        public boolean isCanEnroll() {
            return canEnroll;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{canEnroll=%s, message='%s'}", canEnroll, message);
        }
    }

    /**
     * Detailed result of prerequisite check
     */
    public static class PrerequisiteCheckResult {
        private Student student;
        private Course course;
        private boolean meetsPrerequisites;
        private List<String> missingPrerequisites = new ArrayList<>();
        private List<String> satisfiedPrerequisites = new ArrayList<>();
        private boolean allowOverride;
        private String failureReason;

        // Getters and setters
        public Student getStudent() { return student; }
        public void setStudent(Student student) { this.student = student; }

        public Course getCourse() { return course; }
        public void setCourse(Course course) { this.course = course; }

        public boolean isMeetsPrerequisites() { return meetsPrerequisites; }
        public void setMeetsPrerequisites(boolean meetsPrerequisites) {
            this.meetsPrerequisites = meetsPrerequisites;
        }

        public List<String> getMissingPrerequisites() { return missingPrerequisites; }
        public void setMissingPrerequisites(List<String> missingPrerequisites) {
            this.missingPrerequisites = missingPrerequisites;
        }

        public List<String> getSatisfiedPrerequisites() { return satisfiedPrerequisites; }
        public void setSatisfiedPrerequisites(List<String> satisfiedPrerequisites) {
            this.satisfiedPrerequisites = satisfiedPrerequisites;
        }

        public boolean isAllowOverride() { return allowOverride; }
        public void setAllowOverride(boolean allowOverride) {
            this.allowOverride = allowOverride;
        }

        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        @Override
        public String toString() {
            return String.format("PrerequisiteCheckResult{student=%s, course=%s, meets=%s, missing=%s}",
                student != null ? student.getStudentId() : "null",
                course != null ? course.getCourseCode() : "null",
                meetsPrerequisites,
                missingPrerequisites);
        }
    }
}
