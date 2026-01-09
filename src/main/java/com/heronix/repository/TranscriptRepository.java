package com.heronix.repository;

import com.heronix.model.domain.TranscriptRecord;
import com.heronix.model.domain.TranscriptRecord.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for TranscriptRecord entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Transcript System
 */
@Repository
public interface TranscriptRepository extends JpaRepository<TranscriptRecord, Long> {

    List<TranscriptRecord> findByStudentIdOrderByAcademicYearDescSemesterDesc(Long studentId);

    List<TranscriptRecord> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    List<TranscriptRecord> findByStudentIdAndAcademicYearAndSemester(
        Long studentId, String academicYear, Semester semester);

    List<TranscriptRecord> findByStudentIdAndGradeLevel(Long studentId, Integer gradeLevel);

    @Query("SELECT SUM(t.creditsEarned) FROM TranscriptRecord t " +
           "WHERE t.student.id = :studentId AND t.creditsEarned IS NOT NULL")
    BigDecimal sumCreditsEarnedByStudent(@Param("studentId") Long studentId);

    @Query("SELECT SUM(t.creditsAttempted) FROM TranscriptRecord t " +
           "WHERE t.student.id = :studentId AND t.creditsAttempted IS NOT NULL")
    BigDecimal sumCreditsAttemptedByStudent(@Param("studentId") Long studentId);

    @Query("SELECT AVG(t.gradePoints) FROM TranscriptRecord t " +
           "WHERE t.student.id = :studentId AND t.includeInGpa = true " +
           "AND t.gradePoints IS NOT NULL")
    BigDecimal calculateUnweightedGpa(@Param("studentId") Long studentId);

    @Query("SELECT SUM(t.gradePoints * t.weightFactor * t.creditsAttempted) / " +
           "SUM(t.creditsAttempted) FROM TranscriptRecord t " +
           "WHERE t.student.id = :studentId AND t.includeInGpa = true " +
           "AND t.gradePoints IS NOT NULL AND t.creditsAttempted IS NOT NULL")
    BigDecimal calculateWeightedGpa(@Param("studentId") Long studentId);

    @Query("SELECT t FROM TranscriptRecord t WHERE t.student.id = :studentId " +
           "AND t.courseType IN ('AP', 'IB', 'HONORS', 'DUAL_CREDIT') " +
           "ORDER BY t.academicYear DESC")
    List<TranscriptRecord> findAdvancedCoursesByStudent(@Param("studentId") Long studentId);

    @Query("SELECT DISTINCT t.academicYear FROM TranscriptRecord t " +
           "WHERE t.student.id = :studentId ORDER BY t.academicYear DESC")
    List<String> findAcademicYearsByStudent(@Param("studentId") Long studentId);

    List<TranscriptRecord> findByStudentIdAndRetakeTrue(Long studentId);

    @Query("SELECT COUNT(DISTINCT t.course.id) FROM TranscriptRecord t " +
           "WHERE t.student.id = :studentId AND t.creditsEarned > 0")
    long countCompletedCoursesByStudent(@Param("studentId") Long studentId);
}
