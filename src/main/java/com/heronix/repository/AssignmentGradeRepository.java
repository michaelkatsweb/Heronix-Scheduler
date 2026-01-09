package com.heronix.repository;

import com.heronix.model.domain.AssignmentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AssignmentGrade entity
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Repository
public interface AssignmentGradeRepository extends JpaRepository<AssignmentGrade, Long> {

    /**
     * Find grade for a specific student and assignment
     */
    Optional<AssignmentGrade> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    /**
     * Find all grades for an assignment
     */
    List<AssignmentGrade> findByAssignmentIdOrderByStudentLastName(Long assignmentId);

    /**
     * Find all grades for a student
     */
    List<AssignmentGrade> findByStudentId(Long studentId);

    /**
     * Find all grades for a student in a course
     */
    @Query("SELECT ag FROM AssignmentGrade ag " +
           "JOIN FETCH ag.assignment a " +
           "WHERE ag.student.id = :studentId AND a.course.id = :courseId " +
           "ORDER BY a.dueDate DESC")
    List<AssignmentGrade> findByStudentIdAndCourseId(@Param("studentId") Long studentId,
                                                      @Param("courseId") Long courseId);

    /**
     * Find all grades for a student in a category
     */
    @Query("SELECT ag FROM AssignmentGrade ag " +
           "JOIN ag.assignment a " +
           "WHERE ag.student.id = :studentId AND a.category.id = :categoryId")
    List<AssignmentGrade> findByStudentIdAndCategoryId(@Param("studentId") Long studentId,
                                                        @Param("categoryId") Long categoryId);

    /**
     * Find missing assignments for a student in a course
     */
    @Query("SELECT ag FROM AssignmentGrade ag " +
           "JOIN ag.assignment a " +
           "WHERE ag.student.id = :studentId AND a.course.id = :courseId " +
           "AND ag.status = 'MISSING'")
    List<AssignmentGrade> findMissingByStudentAndCourse(@Param("studentId") Long studentId,
                                                         @Param("courseId") Long courseId);

    /**
     * Count graded submissions for an assignment
     */
    @Query("SELECT COUNT(ag) FROM AssignmentGrade ag WHERE ag.assignment.id = :assignmentId AND ag.score IS NOT NULL")
    long countGradedByAssignment(@Param("assignmentId") Long assignmentId);

    /**
     * Count missing for a student in a course
     */
    @Query("SELECT COUNT(ag) FROM AssignmentGrade ag " +
           "JOIN ag.assignment a " +
           "WHERE ag.student.id = :studentId AND a.course.id = :courseId " +
           "AND ag.status = 'MISSING'")
    long countMissingByStudentAndCourse(@Param("studentId") Long studentId,
                                         @Param("courseId") Long courseId);

    /**
     * Get class average for an assignment
     */
    @Query("SELECT AVG(ag.score) FROM AssignmentGrade ag " +
           "WHERE ag.assignment.id = :assignmentId AND ag.score IS NOT NULL AND ag.excused = false")
    Double getClassAverage(@Param("assignmentId") Long assignmentId);

    /**
     * Delete all grades for an assignment
     */
    void deleteByAssignmentId(Long assignmentId);

    /**
     * Find grades with assignment details for gradebook view
     */
    @Query("SELECT ag FROM AssignmentGrade ag " +
           "JOIN FETCH ag.assignment a " +
           "JOIN FETCH a.category " +
           "WHERE ag.student.id = :studentId AND a.course.id = :courseId " +
           "AND a.countInGrade = true " +
           "ORDER BY a.category.displayOrder, a.dueDate")
    List<AssignmentGrade> findForGradebookCalculation(@Param("studentId") Long studentId,
                                                       @Param("courseId") Long courseId);
}
