package com.heronix.repository;

import com.heronix.model.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Assignment entity
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Find all assignments for a course
     */
    List<Assignment> findByCourseIdOrderByDueDateDesc(Long courseId);

    /**
     * Find assignments for a course by term
     */
    List<Assignment> findByCourseIdAndTermOrderByDueDateDesc(Long courseId, String term);

    /**
     * Find assignments by category
     */
    List<Assignment> findByCategoryIdOrderByDueDateDesc(Long categoryId);

    /**
     * Find published assignments for a course
     */
    List<Assignment> findByCourseIdAndPublishedTrueOrderByDueDateDesc(Long courseId);

    /**
     * Find assignments due between dates
     */
    List<Assignment> findByCourseIdAndDueDateBetween(Long courseId, LocalDate start, LocalDate end);

    /**
     * Find upcoming assignments (due in next N days)
     */
    @Query("SELECT a FROM Assignment a WHERE a.course.id = :courseId AND a.published = true " +
           "AND a.dueDate BETWEEN :today AND :endDate ORDER BY a.dueDate")
    List<Assignment> findUpcomingAssignments(@Param("courseId") Long courseId,
                                             @Param("today") LocalDate today,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Find past due assignments
     */
    @Query("SELECT a FROM Assignment a WHERE a.course.id = :courseId AND a.published = true " +
           "AND a.dueDate < :today ORDER BY a.dueDate DESC")
    List<Assignment> findPastDueAssignments(@Param("courseId") Long courseId,
                                            @Param("today") LocalDate today);

    /**
     * Find assignments with grades included
     */
    @Query("SELECT DISTINCT a FROM Assignment a LEFT JOIN FETCH a.grades WHERE a.course.id = :courseId")
    List<Assignment> findByCourseIdWithGrades(@Param("courseId") Long courseId);

    /**
     * Count assignments in a category
     */
    long countByCategoryId(Long categoryId);

    /**
     * Count graded assignments for a course
     */
    @Query("SELECT COUNT(DISTINCT a) FROM Assignment a JOIN a.grades g WHERE a.course.id = :courseId AND g.score IS NOT NULL")
    long countGradedAssignmentsForCourse(@Param("courseId") Long courseId);

    /**
     * Get average score for an assignment
     */
    @Query("SELECT AVG(g.score) FROM AssignmentGrade g WHERE g.assignment.id = :assignmentId AND g.score IS NOT NULL")
    Double getAverageScore(@Param("assignmentId") Long assignmentId);
}
