package com.heronix.repository;

import com.heronix.model.domain.CourseSequence;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Student Sequence Repository
 *
 * Manages student course sequence assignments and progress tracking.
 * Enables querying students by sequence, sequences by student, and progress reporting.
 *
 * Location: src/main/java/com/eduscheduler/repository/StudentSequenceRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 10, 2025 - Student Sequence Tracking (Audit Item #17)
 */
@Repository
public interface StudentSequenceRepository extends JpaRepository<StudentSequence, Long> {

    /**
     * Find all sequences for a student
     *
     * @param student Student
     * @return List of student sequences
     */
    List<StudentSequence> findByStudent(Student student);

    /**
     * Find active sequences for a student
     *
     * @param student Student
     * @param isActive Active status
     * @return List of active student sequences
     */
    List<StudentSequence> findByStudentAndIsActive(Student student, Boolean isActive);

    /**
     * Find all students following a specific sequence
     *
     * @param courseSequence Course sequence
     * @return List of student sequences
     */
    List<StudentSequence> findByCourseSequence(CourseSequence courseSequence);

    /**
     * Find active students following a specific sequence
     *
     * @param courseSequence Course sequence
     * @param isActive Active status
     * @return List of active student sequences
     */
    List<StudentSequence> findByCourseSequenceAndIsActive(CourseSequence courseSequence, Boolean isActive);

    /**
     * Find specific student-sequence assignment
     *
     * @param student Student
     * @param courseSequence Course sequence
     * @return Optional student sequence
     */
    Optional<StudentSequence> findByStudentAndCourseSequence(Student student, CourseSequence courseSequence);

    /**
     * Find completed sequences for a student
     *
     * @param student Student
     * @return List of completed sequences
     */
    @Query("SELECT ss FROM StudentSequence ss WHERE ss.student = :student AND ss.completedDate IS NOT NULL")
    List<StudentSequence> findCompletedSequences(@Param("student") Student student);

    /**
     * Find in-progress sequences for a student
     *
     * @param student Student
     * @return List of in-progress sequences
     */
    @Query("SELECT ss FROM StudentSequence ss WHERE ss.student = :student " +
           "AND ss.isActive = true AND ss.completedDate IS NULL")
    List<StudentSequence> findInProgressSequences(@Param("student") Student student);

    /**
     * Find sequences with low completion rate (struggling students)
     *
     * @param maxPercentage Maximum completion percentage
     * @return List of student sequences below threshold
     */
    @Query("SELECT ss FROM StudentSequence ss WHERE ss.completionPercentage <= :maxPercentage " +
           "AND ss.isActive = true AND ss.completedDate IS NULL")
    List<StudentSequence> findSequencesBelowCompletionThreshold(@Param("maxPercentage") Double maxPercentage);

    /**
     * Find sequences recommended by counselors
     *
     * @param student Student
     * @return List of recommended sequences
     */
    List<StudentSequence> findByStudentAndIsRecommendedTrue(Student student);

    /**
     * Count active sequences for a student
     *
     * @param student Student
     * @return Count of active sequences
     */
    long countByStudentAndIsActiveTrue(Student student);

    /**
     * Count students in a sequence
     *
     * @param courseSequence Course sequence
     * @return Count of students
     */
    long countByCourseSequenceAndIsActiveTrue(CourseSequence courseSequence);

    /**
     * Get average completion percentage for a sequence
     *
     * @param courseSequence Course sequence
     * @return Average completion percentage
     */
    @Query("SELECT AVG(ss.completionPercentage) FROM StudentSequence ss " +
           "WHERE ss.courseSequence = :sequence AND ss.isActive = true")
    Double getAverageCompletionForSequence(@Param("sequence") CourseSequence courseSequence);
}
