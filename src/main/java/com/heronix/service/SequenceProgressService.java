package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.repository.StudentSequenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sequence Progress Service
 *
 * Calculates and reports on student progress through course sequences.
 * Provides visual progress tracking (e.g., "Step 2 of 4 complete").
 *
 * Location: src/main/java/com/eduscheduler/service/SequenceProgressService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 10, 2025 - Sequence Progress Reporting (Audit Item #18)
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SequenceProgressService {

    @Autowired
    private StudentSequenceRepository studentSequenceRepository;

    /**
     * Calculate completion percentage for a student's sequence
     *
     * @param student Student
     * @param sequence Course sequence
     * @param completedCourses List of courses student has completed
     * @return Completion percentage (0.0 to 100.0)
     */
    public double calculateCompletionPercentage(Student student, CourseSequence sequence,
                                                 List<Course> completedCourses) {
        List<CourseSequenceStep> requiredSteps = sequence.getSteps().stream()
            .filter(CourseSequenceStep::isRequired)
            .collect(Collectors.toList());

        if (requiredSteps.isEmpty()) {
            return 0.0;
        }

        long completedSteps = requiredSteps.stream()
            .filter(step -> completedCourses.contains(step.getCourse()))
            .count();

        return (completedSteps * 100.0) / requiredSteps.size();
    }

    /**
     * Get visual progress string (e.g., "Step 2 of 4")
     *
     * @param student Student
     * @param sequence Course sequence
     * @param completedCourses List of completed courses
     * @return Progress string
     */
    public String getProgressString(Student student, CourseSequence sequence,
                                     List<Course> completedCourses) {
        List<CourseSequenceStep> requiredSteps = sequence.getSteps().stream()
            .filter(CourseSequenceStep::isRequired)
            .sorted((a, b) -> a.getStepOrder().compareTo(b.getStepOrder()))
            .collect(Collectors.toList());

        if (requiredSteps.isEmpty()) {
            return "No steps";
        }

        long completedSteps = requiredSteps.stream()
            .filter(step -> completedCourses.contains(step.getCourse()))
            .count();

        return String.format("Step %d of %d", completedSteps, requiredSteps.size());
    }

    /**
     * Get current step for a student
     *
     * @param student Student
     * @param sequence Course sequence
     * @param completedCourses List of completed courses
     * @return Current step (next incomplete required step) or null if complete
     */
    public CourseSequenceStep getCurrentStep(Student student, CourseSequence sequence,
                                              List<Course> completedCourses) {
        List<CourseSequenceStep> requiredSteps = sequence.getSteps().stream()
            .filter(CourseSequenceStep::isRequired)
            .sorted((a, b) -> a.getStepOrder().compareTo(b.getStepOrder()))
            .collect(Collectors.toList());

        for (CourseSequenceStep step : requiredSteps) {
            if (!completedCourses.contains(step.getCourse())) {
                return step; // First incomplete step
            }
        }

        return null; // All steps completed
    }

    /**
     * Get completed steps for a student
     *
     * @param student Student
     * @param sequence Course sequence
     * @param completedCourses List of completed courses
     * @return List of completed steps
     */
    public List<CourseSequenceStep> getCompletedSteps(Student student, CourseSequence sequence,
                                                       List<Course> completedCourses) {
        return sequence.getSteps().stream()
            .filter(step -> completedCourses.contains(step.getCourse()))
            .sorted((a, b) -> a.getStepOrder().compareTo(b.getStepOrder()))
            .collect(Collectors.toList());
    }

    /**
     * Get remaining steps for a student
     *
     * @param student Student
     * @param sequence Course sequence
     * @param completedCourses List of completed courses
     * @return List of remaining steps
     */
    public List<CourseSequenceStep> getRemainingSteps(Student student, CourseSequence sequence,
                                                       List<Course> completedCourses) {
        return sequence.getSteps().stream()
            .filter(step -> !completedCourses.contains(step.getCourse()))
            .sorted((a, b) -> a.getStepOrder().compareTo(b.getStepOrder()))
            .collect(Collectors.toList());
    }

    /**
     * Check if student has completed a sequence
     *
     * @param student Student
     * @param sequence Course sequence
     * @param completedCourses List of completed courses
     * @return true if all required steps completed or terminal step reached
     */
    public boolean isSequenceComplete(Student student, CourseSequence sequence,
                                       List<Course> completedCourses) {
        // Check if student reached a terminal step
        boolean reachedTerminal = sequence.getSteps().stream()
            .filter(CourseSequenceStep::getIsTerminal)
            .anyMatch(step -> completedCourses.contains(step.getCourse()));

        if (reachedTerminal) {
            return true;
        }

        // Check if all required steps completed
        List<CourseSequenceStep> requiredSteps = sequence.getSteps().stream()
            .filter(CourseSequenceStep::isRequired)
            .collect(Collectors.toList());

        return requiredSteps.stream()
            .allMatch(step -> completedCourses.contains(step.getCourse()));
    }

    /**
     * Get progress summary for all students in a sequence
     *
     * @param sequence Course sequence
     * @return Summary string
     */
    public String getSequenceProgressSummary(CourseSequence sequence) {
        List<StudentSequence> studentSequences = studentSequenceRepository
            .findByCourseSequenceAndIsActive(sequence, true);

        if (studentSequences.isEmpty()) {
            return "No students enrolled";
        }

        long completed = studentSequences.stream()
            .filter(StudentSequence::isCompleted)
            .count();

        double avgProgress = studentSequences.stream()
            .mapToDouble(StudentSequence::getCompletionPercentage)
            .average()
            .orElse(0.0);

        return String.format("%d students enrolled, %d completed, %.1f%% average progress",
            studentSequences.size(), completed, avgProgress);
    }

    /**
     * Find students at risk in a sequence (below 25% completion)
     *
     * @param sequence Course sequence
     * @return List of at-risk student sequences
     */
    public List<StudentSequence> getAtRiskStudents(CourseSequence sequence) {
        return studentSequenceRepository.findByCourseSequenceAndIsActive(sequence, true).stream()
            .filter(ss -> ss.getCompletionPercentage() < 25.0 && !ss.isCompleted())
            .collect(Collectors.toList());
    }

    /**
     * Find students excelling in a sequence (above 75% completion)
     *
     * @param sequence Course sequence
     * @return List of excelling student sequences
     */
    public List<StudentSequence> getExcellingStudents(CourseSequence sequence) {
        return studentSequenceRepository.findByCourseSequenceAndIsActive(sequence, true).stream()
            .filter(ss -> ss.getCompletionPercentage() >= 75.0 && !ss.isCompleted())
            .collect(Collectors.toList());
    }

    /**
     * Update progress for a student sequence
     *
     * @param studentSequence Student sequence
     * @param completedCourses List of completed courses
     */
    @Transactional
    public void updateProgress(StudentSequence studentSequence, List<Course> completedCourses) {
        double percentage = calculateCompletionPercentage(
            studentSequence.getStudent(),
            studentSequence.getCourseSequence(),
            completedCourses
        );

        studentSequence.updateCompletionPercentage(percentage);

        CourseSequenceStep currentStep = getCurrentStep(
            studentSequence.getStudent(),
            studentSequence.getCourseSequence(),
            completedCourses
        );

        studentSequence.setCurrentStep(currentStep);

        if (percentage >= 100.0) {
            studentSequence.markCompleted();
        }

        studentSequenceRepository.save(studentSequence);
        log.debug("Updated progress for student {} in sequence {}: {}%",
            studentSequence.getStudent().getStudentId(),
            studentSequence.getCourseSequence().getCode(),
            percentage);
    }
}
