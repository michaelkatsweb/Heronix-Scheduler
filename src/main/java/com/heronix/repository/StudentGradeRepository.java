package com.heronix.repository;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Student Grade Repository
 * Location: src/main/java/com/eduscheduler/repository/StudentGradeRepository.java
 *
 * Data access layer for student grades.
 * Provides queries for GPA calculation, transcript generation, and grade analysis.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-20
 */
@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {

    /**
     * Find all grades for a student
     */
    List<StudentGrade> findByStudentOrderByGradeDateDesc(Student student);

    /**
     * Find all grades for a student by student ID
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.student.id = :studentId ORDER BY g.gradeDate DESC")
    List<StudentGrade> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Find grades for a student in a specific term
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.student.id = :studentId AND g.term = :term")
    List<StudentGrade> findByStudentIdAndTerm(@Param("studentId") Long studentId, @Param("term") String term);

    /**
     * Find grades for a student in a specific academic year
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.student.id = :studentId AND g.academicYear = :year")
    List<StudentGrade> findByStudentIdAndAcademicYear(@Param("studentId") Long studentId, @Param("year") Integer year);

    /**
     * Find final grades only (for GPA calculation)
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.student.id = :studentId AND g.isFinal = true AND g.includeInGPA = true")
    List<StudentGrade> findFinalGradesByStudentId(@Param("studentId") Long studentId);

    /**
     * Find grade for specific student and course
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.student.id = :studentId AND g.course.id = :courseId AND g.term = :term")
    Optional<StudentGrade> findByStudentIdAndCourseIdAndTerm(
        @Param("studentId") Long studentId,
        @Param("courseId") Long courseId,
        @Param("term") String term
    );

    /**
     * Find all grades for a student and course
     */
    List<StudentGrade> findByStudentAndCourse(Student student, Course course);

    /**
     * Find all grades for a course
     */
    List<StudentGrade> findByCourseOrderByStudentLastNameAsc(Course course);

    /**
     * Find all grades for a course by course ID
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.course.id = :courseId ORDER BY g.student.lastName ASC")
    List<StudentGrade> findByCourseId(@Param("courseId") Long courseId);

    /**
     * Find students with GPA above threshold
     */
    @Query("SELECT DISTINCT g.student FROM StudentGrade g WHERE g.isFinal = true " +
           "GROUP BY g.student HAVING AVG(g.gpaPoints) >= :minGpa")
    List<Student> findStudentsWithMinGPA(@Param("minGpa") Double minGpa);

    /**
     * Find students with GPA in range
     */
    @Query("SELECT DISTINCT g.student FROM StudentGrade g WHERE g.isFinal = true " +
           "GROUP BY g.student HAVING AVG(g.gpaPoints) BETWEEN :minGpa AND :maxGpa")
    List<Student> findStudentsWithGPAInRange(
        @Param("minGpa") Double minGpa,
        @Param("maxGpa") Double maxGpa
    );

    /**
     * Calculate unweighted GPA for a student
     */
    @Query("SELECT COALESCE(SUM(g.gpaPoints * g.credits) / NULLIF(SUM(g.credits), 0), 0.0) " +
           "FROM StudentGrade g WHERE g.student.id = :studentId AND g.isFinal = true AND g.includeInGPA = true")
    Double calculateUnweightedGPA(@Param("studentId") Long studentId);

    /**
     * Calculate weighted GPA for a student
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN g.isWeighted = true THEN (g.gpaPoints + 1.0) ELSE g.gpaPoints END * g.credits) " +
           "/ NULLIF(SUM(g.credits), 0), 0.0) " +
           "FROM StudentGrade g WHERE g.student.id = :studentId AND g.isFinal = true AND g.includeInGPA = true")
    Double calculateWeightedGPA(@Param("studentId") Long studentId);

    /**
     * Calculate GPA for specific term
     */
    @Query("SELECT COALESCE(SUM(g.gpaPoints * g.credits) / NULLIF(SUM(g.credits), 0), 0.0) " +
           "FROM StudentGrade g WHERE g.student.id = :studentId AND g.term = :term AND g.isFinal = true AND g.includeInGPA = true")
    Double calculateTermGPA(@Param("studentId") Long studentId, @Param("term") String term);

    /**
     * Get total credits earned by student
     */
    @Query("SELECT COALESCE(SUM(g.credits), 0.0) FROM StudentGrade g " +
           "WHERE g.student.id = :studentId AND g.isFinal = true AND g.letterGrade NOT IN ('F', 'I', 'W')")
    Double getTotalCreditsEarned(@Param("studentId") Long studentId);

    /**
     * Count passing grades for student
     */
    @Query("SELECT COUNT(g) FROM StudentGrade g WHERE g.student.id = :studentId " +
           "AND g.isFinal = true AND g.letterGrade NOT IN ('F', 'I', 'W')")
    Long countPassingGrades(@Param("studentId") Long studentId);

    /**
     * Count failing grades for student
     */
    @Query("SELECT COUNT(g) FROM StudentGrade g WHERE g.student.id = :studentId " +
           "AND g.isFinal = true AND g.letterGrade = 'F'")
    Long countFailingGrades(@Param("studentId") Long studentId);

    /**
     * Find students who got a specific grade in a course
     */
    @Query("SELECT g.student FROM StudentGrade g WHERE g.course.id = :courseId " +
           "AND g.letterGrade = :grade AND g.isFinal = true")
    List<Student> findStudentsByGradeInCourse(
        @Param("courseId") Long courseId,
        @Param("grade") String grade
    );

    /**
     * Get grade distribution for a course
     */
    @Query("SELECT g.letterGrade, COUNT(g) FROM StudentGrade g " +
           "WHERE g.course.id = :courseId AND g.isFinal = true " +
           "GROUP BY g.letterGrade ORDER BY g.letterGrade")
    List<Object[]> getGradeDistributionForCourse(@Param("courseId") Long courseId);

    /**
     * Find students who failed a specific subject
     */
    @Query("SELECT DISTINCT g.student FROM StudentGrade g WHERE g.course.subject = :subject " +
           "AND g.letterGrade = 'F' AND g.isFinal = true")
    List<Student> findStudentsWhoFailedSubject(@Param("subject") String subject);

    /**
     * Find students on honor roll (GPA >= threshold)
     */
    @Query("SELECT DISTINCT g.student FROM StudentGrade g WHERE g.isFinal = true AND g.term = :term " +
           "GROUP BY g.student HAVING AVG(g.gpaPoints) >= :threshold")
    List<Student> findHonorRollStudents(@Param("term") String term, @Param("threshold") Double threshold);

    /**
     * Get recent grades (last N grades for a student)
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.student.id = :studentId " +
           "ORDER BY g.gradeDate DESC")
    List<StudentGrade> findRecentGrades(@Param("studentId") Long studentId);

    /**
     * Find grades entered in date range
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.enteredDate BETWEEN :startDate AND :endDate " +
           "ORDER BY g.enteredDate DESC")
    List<StudentGrade> findGradesEnteredBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find incomplete grades (grade = 'I')
     */
    @Query("SELECT g FROM StudentGrade g WHERE g.letterGrade = 'I' ORDER BY g.gradeDate DESC")
    List<StudentGrade> findIncompleteGrades();

    /**
     * Count total grades in system
     */
    @Query("SELECT COUNT(g) FROM StudentGrade g WHERE g.isFinal = true")
    Long countTotalFinalGrades();

    /**
     * Get average GPA across all students
     */
    @Query("SELECT AVG(s.currentGPA) FROM Student s WHERE s.currentGPA IS NOT NULL AND s.active = true")
    Double getAverageGPAAcrossAllStudents();

    /**
     * Find students with improved GPA (current > previous)
     */
    @Query("SELECT s FROM Student s WHERE s.currentGPA > s.previousTermGPA AND s.active = true")
    List<Student> findStudentsWithImprovedGPA();

    /**
     * Find students with declining GPA (current < previous)
     */
    @Query("SELECT s FROM Student s WHERE s.currentGPA < s.previousTermGPA AND s.active = true")
    List<Student> findStudentsWithDecliningGPA();

    /**
     * Find students eligible for honors based on GPA
     */
    @Query("SELECT s FROM Student s WHERE s.currentGPA >= :threshold AND s.active = true")
    List<Student> findHonorsEligibleStudents(@Param("threshold") Double threshold);

    /**
     * Delete all grades for a student (for data cleanup)
     */
    @Query("DELETE FROM StudentGrade g WHERE g.student.id = :studentId")
    void deleteAllByStudentId(@Param("studentId") Long studentId);

    /**
     * Delete grades for a specific term (rollback functionality)
     */
    @Query("DELETE FROM StudentGrade g WHERE g.term = :term")
    void deleteAllByTerm(@Param("term") String term);
}
