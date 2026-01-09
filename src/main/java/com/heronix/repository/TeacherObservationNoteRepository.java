package com.heronix.repository;

import com.heronix.model.domain.TeacherObservationNote;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.TeacherObservationNote.ObservationCategory;
import com.heronix.model.domain.TeacherObservationNote.ObservationRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for TeacherObservationNote entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Repository
public interface TeacherObservationNoteRepository extends JpaRepository<TeacherObservationNote, Long> {

    // Find all observation notes for a student
    List<TeacherObservationNote> findByStudent(Student student);

    // Find observation notes for a student in a specific course
    List<TeacherObservationNote> findByStudentAndCourse(Student student, Course course);

    // Find observation notes within date range
    List<TeacherObservationNote> findByStudentAndObservationDateBetween(
            Student student,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find observations flagged for intervention
    List<TeacherObservationNote> findByStudentAndIsFlagForIntervention(
            Student student,
            Boolean isFlagged
    );

    // Find recent intervention flags
    @Query("SELECT o FROM TeacherObservationNote o WHERE o.student = :student " +
           "AND o.isFlagForIntervention = true AND o.observationDate >= :sinceDate " +
           "ORDER BY o.observationDate DESC")
    List<TeacherObservationNote> findRecentInterventionFlags(
            @Param("student") Student student,
            @Param("sinceDate") LocalDate sinceDate
    );

    // Find observations by category
    List<TeacherObservationNote> findByStudentAndObservationCategory(
            Student student,
            ObservationCategory category
    );

    // Find observations by rating
    List<TeacherObservationNote> findByStudentAndObservationRating(
            Student student,
            ObservationRating rating
    );

    // Find concern-level observations
    @Query("SELECT o FROM TeacherObservationNote o WHERE o.student = :student " +
           "AND o.observationRating = 'CONCERN' AND o.observationDate >= :sinceDate " +
           "ORDER BY o.observationDate DESC")
    List<TeacherObservationNote> findConcernObservationsSince(
            @Param("student") Student student,
            @Param("sinceDate") LocalDate sinceDate
    );

    // Count intervention flags by category
    @Query("SELECT COUNT(o) FROM TeacherObservationNote o WHERE o.student = :student " +
           "AND o.observationCategory = :category AND o.isFlagForIntervention = true " +
           "AND o.observationDate BETWEEN :startDate AND :endDate")
    Long countInterventionFlagsByCategory(
            @Param("student") Student student,
            @Param("category") ObservationCategory category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find recent observations (for quick dashboard display)
    @Query("SELECT o FROM TeacherObservationNote o WHERE o.student = :student " +
           "ORDER BY o.observationDate DESC")
    List<TeacherObservationNote> findRecentByStudent(@Param("student") Student student);
}
