package com.heronix.repository;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSequence;
import com.heronix.model.domain.CourseSequenceStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CourseSequenceStep entity
 *
 * Provides database access for course sequence step management.
 *
 * Location: src/main/java/com/eduscheduler/repository/CourseSequenceStepRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 6, 2025 - Course Sequencing
 */
@Repository
public interface CourseSequenceStepRepository extends JpaRepository<CourseSequenceStep, Long> {

    /**
     * Find all steps for a sequence
     *
     * @param courseSequence Course sequence
     * @return List of steps ordered by step_order
     */
    List<CourseSequenceStep> findByCourseSequenceOrderByStepOrderAsc(CourseSequence courseSequence);

    /**
     * Find steps by course
     *
     * @param course Course
     * @return List of steps
     */
    List<CourseSequenceStep> findByCourse(Course course);

    /**
     * Find steps by sequence and course
     *
     * @param courseSequence Course sequence
     * @param course Course
     * @return Optional containing step if found
     */
    Optional<CourseSequenceStep> findByCourseSequenceAndCourse(CourseSequence courseSequence, Course course);

    /**
     * Find step by sequence and step order
     *
     * @param courseSequence Course sequence
     * @param stepOrder Step order number
     * @return Optional containing step if found
     */
    Optional<CourseSequenceStep> findByCourseSequenceAndStepOrder(CourseSequence courseSequence, Integer stepOrder);

    /**
     * Find steps by grade level
     *
     * @param gradeLevel Grade level (9, 10, 11, 12)
     * @return List of steps
     */
    List<CourseSequenceStep> findByRecommendedGradeLevel(Integer gradeLevel);

    /**
     * Find required steps for a sequence
     *
     * @param courseSequence Course sequence
     * @return List of required steps
     */
    List<CourseSequenceStep> findByCourseSequenceAndIsRequiredTrue(CourseSequence courseSequence);

    /**
     * Find optional steps for a sequence
     *
     * @param courseSequence Course sequence
     * @return List of optional steps
     */
    List<CourseSequenceStep> findByCourseSequenceAndIsRequiredFalse(CourseSequence courseSequence);

    /**
     * Find terminal steps for a sequence
     *
     * @param courseSequence Course sequence
     * @return List of terminal steps
     */
    List<CourseSequenceStep> findByCourseSequenceAndIsTerminalTrue(CourseSequence courseSequence);

    /**
     * Find first step in a sequence
     *
     * @param courseSequence Course sequence
     * @return Optional containing first step if found
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.courseSequence = :sequence AND step.stepOrder = 1")
    Optional<CourseSequenceStep> findFirstStep(@Param("sequence") CourseSequence courseSequence);

    /**
     * Find last step in a sequence
     *
     * @param courseSequence Course sequence
     * @return Optional containing last step if found
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.courseSequence = :sequence " +
           "ORDER BY step.stepOrder DESC LIMIT 1")
    Optional<CourseSequenceStep> findLastStep(@Param("sequence") CourseSequence courseSequence);

    /**
     * Find next step after a given step
     *
     * @param courseSequence Course sequence
     * @param currentStepOrder Current step order
     * @return Optional containing next step if found
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.courseSequence = :sequence " +
           "AND step.stepOrder = :currentOrder + 1")
    Optional<CourseSequenceStep> findNextStep(
        @Param("sequence") CourseSequence courseSequence,
        @Param("currentOrder") Integer currentStepOrder);

    /**
     * Find previous step before a given step
     *
     * @param courseSequence Course sequence
     * @param currentStepOrder Current step order
     * @return Optional containing previous step if found
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.courseSequence = :sequence " +
           "AND step.stepOrder = :currentOrder - 1")
    Optional<CourseSequenceStep> findPreviousStep(
        @Param("sequence") CourseSequence courseSequence,
        @Param("currentOrder") Integer currentStepOrder);

    /**
     * Find steps with specific credits
     *
     * @param credits Credit value
     * @return List of steps
     */
    List<CourseSequenceStep> findByCredits(Double credits);

    /**
     * Find steps with alternatives
     *
     * @return List of steps that have alternative courses
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.alternativeCourses IS NOT NULL AND step.alternativeCourses != ''")
    List<CourseSequenceStep> findStepsWithAlternatives();

    /**
     * Find steps by sequence and grade level
     *
     * @param courseSequence Course sequence
     * @param gradeLevel Grade level
     * @return List of steps
     */
    List<CourseSequenceStep> findByCourseSequenceAndRecommendedGradeLevel(
        CourseSequence courseSequence, Integer gradeLevel);

    /**
     * Find steps that require minimum grade from previous step
     *
     * @return List of steps with grade requirements
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.minGradeFromPrevious IS NOT NULL")
    List<CourseSequenceStep> findStepsWithGradeRequirements();

    /**
     * Find steps that have prerequisites defined
     *
     * Returns all steps where prerequisiteStepIds is not null/empty.
     * These steps require completion of other steps before they can be taken.
     *
     * @return List of steps with prerequisites
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.prerequisiteStepIds IS NOT NULL " +
           "AND step.prerequisiteStepIds != '' AND step.prerequisiteStepIds != '[]'")
    List<CourseSequenceStep> findStepsWithPrerequisites();

    /**
     * Find steps in a sequence that have prerequisites
     *
     * @param courseSequence Course sequence
     * @return List of steps with prerequisites in the given sequence
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.courseSequence = :sequence " +
           "AND step.prerequisiteStepIds IS NOT NULL AND step.prerequisiteStepIds != '' " +
           "AND step.prerequisiteStepIds != '[]'")
    List<CourseSequenceStep> findStepsWithPrerequisitesByCourseSequence(@Param("sequence") CourseSequence courseSequence);

    /**
     * Count steps in a sequence
     *
     * @param courseSequence Course sequence
     * @return Count of steps
     */
    long countByCourseSequence(CourseSequence courseSequence);

    /**
     * Count required steps in a sequence
     *
     * @param courseSequence Course sequence
     * @return Count of required steps
     */
    long countByCourseSequenceAndIsRequiredTrue(CourseSequence courseSequence);

    /**
     * Count optional steps in a sequence
     *
     * @param courseSequence Course sequence
     * @return Count of optional steps
     */
    long countByCourseSequenceAndIsRequiredFalse(CourseSequence courseSequence);

    /**
     * Get total credits for a sequence
     *
     * @param courseSequence Course sequence
     * @return Total credits
     */
    @Query("SELECT SUM(step.credits) FROM CourseSequenceStep step WHERE step.courseSequence = :sequence")
    Double getTotalCredits(@Param("sequence") CourseSequence courseSequence);

    /**
     * Get total required credits for a sequence
     *
     * @param courseSequence Course sequence
     * @return Total required credits
     */
    @Query("SELECT SUM(step.credits) FROM CourseSequenceStep step " +
           "WHERE step.courseSequence = :sequence AND step.isRequired = true")
    Double getTotalRequiredCredits(@Param("sequence") CourseSequence courseSequence);

    /**
     * Find steps by sequence type
     *
     * @param sequenceType Sequence type
     * @return List of steps
     */
    @Query("SELECT step FROM CourseSequenceStep step " +
           "WHERE step.courseSequence.sequenceType = :sequenceType")
    List<CourseSequenceStep> findBySequenceType(@Param("sequenceType") CourseSequence.SequenceType sequenceType);

    /**
     * Find courses that appear in multiple sequences
     *
     * @return List of courses
     */
    @Query("SELECT step.course FROM CourseSequenceStep step " +
           "GROUP BY step.course HAVING COUNT(DISTINCT step.courseSequence) > 1")
    List<Course> findCoursesInMultipleSequences();

    /**
     * Find steps for a course in specific sequence type
     *
     * @param course Course
     * @param sequenceType Sequence type
     * @return List of steps
     */
    @Query("SELECT step FROM CourseSequenceStep step " +
           "WHERE step.course = :course AND step.courseSequence.sequenceType = :sequenceType")
    List<CourseSequenceStep> findByCourseAndSequenceType(
        @Param("course") Course course,
        @Param("sequenceType") CourseSequence.SequenceType sequenceType);

    /**
     * Check if course is first step in any sequence
     *
     * @param course Course
     * @return true if course is first step in any sequence
     */
    @Query("SELECT CASE WHEN COUNT(step) > 0 THEN true ELSE false END " +
           "FROM CourseSequenceStep step WHERE step.course = :course AND step.stepOrder = 1")
    boolean isCourseFirstStepInAnySequence(@Param("course") Course course);

    /**
     * Check if course is terminal step in any sequence
     *
     * @param course Course
     * @return true if course is terminal in any sequence
     */
    @Query("SELECT CASE WHEN COUNT(step) > 0 THEN true ELSE false END " +
           "FROM CourseSequenceStep step WHERE step.course = :course AND step.isTerminal = true")
    boolean isCourseTerminalInAnySequence(@Param("course") Course course);

    /**
     * Find all grade levels for a sequence
     *
     * @param courseSequence Course sequence
     * @return List of grade levels
     */
    @Query("SELECT DISTINCT step.recommendedGradeLevel FROM CourseSequenceStep step " +
           "WHERE step.courseSequence = :sequence ORDER BY step.recommendedGradeLevel")
    List<Integer> findGradeLevelsForSequence(@Param("sequence") CourseSequence courseSequence);

    /**
     * Find steps with notes
     *
     * @return List of steps with notes
     */
    @Query("SELECT step FROM CourseSequenceStep step WHERE step.notes IS NOT NULL AND step.notes != ''")
    List<CourseSequenceStep> findStepsWithNotes();

    /**
     * Get maximum step order for a sequence
     *
     * @param courseSequence Course sequence
     * @return Maximum step order
     */
    @Query("SELECT MAX(step.stepOrder) FROM CourseSequenceStep step WHERE step.courseSequence = :sequence")
    Integer getMaxStepOrder(@Param("sequence") CourseSequence courseSequence);

    /**
     * Check if step order exists in sequence
     *
     * @param courseSequence Course sequence
     * @param stepOrder Step order
     * @return true if step order exists
     */
    boolean existsByCourseSequenceAndStepOrder(CourseSequence courseSequence, Integer stepOrder);

    /**
     * Check if course exists in sequence
     *
     * @param courseSequence Course sequence
     * @param course Course
     * @return true if course exists in sequence
     */
    boolean existsByCourseSequenceAndCourse(CourseSequence courseSequence, Course course);
}
