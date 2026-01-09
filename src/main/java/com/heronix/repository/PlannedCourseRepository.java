package com.heronix.repository;

import com.heronix.model.domain.AcademicPlan;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.PlannedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PlannedCourse entity
 *
 * Provides database access for planned course management.
 *
 * Location: src/main/java/com/eduscheduler/repository/PlannedCourseRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - December 6, 2025 - Four-Year Academic Planning
 */
@Repository
public interface PlannedCourseRepository extends JpaRepository<PlannedCourse, Long> {

    /**
     * Find all courses for a plan
     *
     * @param academicPlan Academic plan
     * @return List of planned courses
     */
    List<PlannedCourse> findByAcademicPlanOrderBySchoolYearAscSemesterAsc(AcademicPlan academicPlan);

    /**
     * Find courses for plan and school year
     *
     * @param academicPlan Academic plan
     * @param schoolYear School year
     * @return List of planned courses
     */
    List<PlannedCourse> findByAcademicPlanAndSchoolYearOrderBySemesterAsc(
        AcademicPlan academicPlan, String schoolYear);

    /**
     * Find courses for plan and grade level
     *
     * @param academicPlan Academic plan
     * @param gradeLevel Grade level
     * @return List of planned courses
     */
    List<PlannedCourse> findByAcademicPlanAndGradeLevel(
        AcademicPlan academicPlan, Integer gradeLevel);

    /**
     * Find courses for plan, year, and semester
     *
     * @param academicPlan Academic plan
     * @param schoolYear School year
     * @param semester Semester
     * @return List of planned courses
     */
    List<PlannedCourse> findByAcademicPlanAndSchoolYearAndSemester(
        AcademicPlan academicPlan, String schoolYear, Integer semester);

    /**
     * Find courses by status
     *
     * @param status Course status
     * @return List of planned courses
     */
    List<PlannedCourse> findByStatus(PlannedCourse.CourseStatus status);

    /**
     * Find courses by status for plan
     *
     * @param academicPlan Academic plan
     * @param status Course status
     * @return List of planned courses
     */
    List<PlannedCourse> findByAcademicPlanAndStatus(
        AcademicPlan academicPlan, PlannedCourse.CourseStatus status);

    /**
     * Find completed courses for plan
     *
     * @param academicPlan Academic plan
     * @return List of completed courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.status = 'COMPLETED'")
    List<PlannedCourse> findCompletedCoursesForPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find in-progress courses for plan
     *
     * @param academicPlan Academic plan
     * @return List of in-progress courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.status IN ('ENROLLED', 'IN_PROGRESS')")
    List<PlannedCourse> findInProgressCoursesForPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find planned (future) courses for plan
     *
     * @param academicPlan Academic plan
     * @return List of planned courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.status = 'PLANNED'")
    List<PlannedCourse> findPlannedCoursesForPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find required courses for plan
     *
     * @param academicPlan Academic plan
     * @return List of required courses
     */
    List<PlannedCourse> findByAcademicPlanAndIsRequiredTrue(AcademicPlan academicPlan);

    /**
     * Find elective courses for plan
     *
     * @param academicPlan Academic plan
     * @return List of elective courses
     */
    List<PlannedCourse> findByAcademicPlanAndIsRequiredFalse(AcademicPlan academicPlan);

    /**
     * Find courses with unmet prerequisites
     *
     * @param academicPlan Academic plan
     * @return List of courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.prerequisitesMet = false")
    List<PlannedCourse> findCoursesWithUnmetPrerequisites(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find courses with conflicts
     *
     * @param academicPlan Academic plan
     * @return List of courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.hasConflict = true")
    List<PlannedCourse> findCoursesWithConflicts(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find course in plan
     *
     * @param academicPlan Academic plan
     * @param course Course
     * @return Optional containing planned course if found
     */
    Optional<PlannedCourse> findByAcademicPlanAndCourse(AcademicPlan academicPlan, Course course);

    /**
     * Check if course exists in plan
     *
     * @param academicPlan Academic plan
     * @param course Course
     * @return true if exists
     */
    boolean existsByAcademicPlanAndCourse(AcademicPlan academicPlan, Course course);

    /**
     * Count courses in plan
     *
     * @param academicPlan Academic plan
     * @return Count
     */
    long countByAcademicPlan(AcademicPlan academicPlan);

    /**
     * Count completed courses in plan
     *
     * @param academicPlan Academic plan
     * @return Count
     */
    @Query("SELECT COUNT(pc) FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.status = 'COMPLETED'")
    long countCompletedCoursesInPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Count required courses in plan
     *
     * @param academicPlan Academic plan
     * @return Count
     */
    long countByAcademicPlanAndIsRequiredTrue(AcademicPlan academicPlan);

    /**
     * Get total credits for plan
     *
     * @param academicPlan Academic plan
     * @return Total credits
     */
    @Query("SELECT SUM(pc.credits) FROM PlannedCourse pc WHERE pc.academicPlan = :plan")
    Double getTotalCreditsForPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Get total completed credits for plan
     *
     * @param academicPlan Academic plan
     * @return Total completed credits
     */
    @Query("SELECT SUM(pc.credits) FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.status = 'COMPLETED'")
    Double getTotalCompletedCreditsForPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Get total credits for year
     *
     * @param academicPlan Academic plan
     * @param schoolYear School year
     * @return Total credits
     */
    @Query("SELECT SUM(pc.credits) FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.schoolYear = :schoolYear")
    Double getTotalCreditsForYear(@Param("plan") AcademicPlan academicPlan,
                                   @Param("schoolYear") String schoolYear);

    /**
     * Get total credits for semester
     *
     * @param academicPlan Academic plan
     * @param schoolYear School year
     * @param semester Semester
     * @return Total credits
     */
    @Query("SELECT SUM(pc.credits) FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.schoolYear = :schoolYear AND pc.semester = :semester")
    Double getTotalCreditsForSemester(@Param("plan") AcademicPlan academicPlan,
                                       @Param("schoolYear") String schoolYear,
                                       @Param("semester") Integer semester);

    /**
     * Find courses by specific course
     *
     * @param course Course
     * @return List of planned courses
     */
    List<PlannedCourse> findByCourse(Course course);

    /**
     * Find all school years in plan
     *
     * @param academicPlan Academic plan
     * @return List of school years
     */
    @Query("SELECT DISTINCT pc.schoolYear FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "ORDER BY pc.schoolYear")
    List<String> findDistinctSchoolYearsForPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find all grade levels in plan
     *
     * @param academicPlan Academic plan
     * @return List of grade levels
     */
    @Query("SELECT DISTINCT pc.gradeLevel FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "ORDER BY pc.gradeLevel")
    List<Integer> findDistinctGradeLevelsForPlan(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find courses with alternatives
     *
     * @param academicPlan Academic plan
     * @return List of courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.alternatives IS NOT NULL AND pc.alternatives != ''")
    List<PlannedCourse> findCoursesWithAlternatives(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find courses from recommendations
     *
     * @param academicPlan Academic plan
     * @return List of courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.recommendation IS NOT NULL")
    List<PlannedCourse> findCoursesFromRecommendations(@Param("plan") AcademicPlan academicPlan);

    /**
     * Find courses from sequence steps
     *
     * @param academicPlan Academic plan
     * @return List of courses
     */
    @Query("SELECT pc FROM PlannedCourse pc WHERE pc.academicPlan = :plan " +
           "AND pc.sequenceStep IS NOT NULL")
    List<PlannedCourse> findCoursesFromSequence(@Param("plan") AcademicPlan academicPlan);
}
