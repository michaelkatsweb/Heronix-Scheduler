package com.heronix.repository;

import com.heronix.model.domain.ClassroomGradeEntry;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for ClassroomGradeEntry entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Repository
public interface ClassroomGradeEntryRepository extends JpaRepository<ClassroomGradeEntry, Long> {

    // Find all grade entries for a student
    List<ClassroomGradeEntry> findByStudent(Student student);

    // Find grade entries for a student in a specific course
    List<ClassroomGradeEntry> findByStudentAndCourse(Student student, Course course);

    // Find grade entries within a date range
    List<ClassroomGradeEntry> findByStudentAndAssignmentDateBetween(
            Student student,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find missing work for a student
    List<ClassroomGradeEntry> findByStudentAndIsMissingWork(Student student, Boolean isMissingWork);

    // Count missing work within date range
    @Query("SELECT COUNT(g) FROM ClassroomGradeEntry g WHERE g.student = :student " +
           "AND g.isMissingWork = true AND g.assignmentDate BETWEEN :startDate AND :endDate")
    Long countByStudentAndIsMissingWorkAndDateBetween(
            @Param("student") Student student,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find benchmark assessments for a student
    List<ClassroomGradeEntry> findByStudentAndIsBenchmarkAssessment(Student student, Boolean isBenchmark);

    // Find failing grades for a student in a course
    @Query("SELECT g FROM ClassroomGradeEntry g WHERE g.student = :student " +
           "AND g.course = :course AND g.percentageGrade < 60.0")
    List<ClassroomGradeEntry> findFailingGradesByStudentAndCourse(
            @Param("student") Student student,
            @Param("course") Course course
    );

    // Calculate current grade for student in course
    @Query("SELECT g FROM ClassroomGradeEntry g WHERE g.student = :student " +
           "AND g.course = :course ORDER BY g.assignmentDate DESC")
    List<ClassroomGradeEntry> findByStudentAndCourseOrderByDateDesc(
            @Param("student") Student student,
            @Param("course") Course course
    );

    // Find all grade entries for a course
    List<ClassroomGradeEntry> findByCourse(Course course);

    // Find grade entries by assignment type
    List<ClassroomGradeEntry> findByStudentAndAssignmentType(
            Student student,
            ClassroomGradeEntry.AssignmentType assignmentType
    );

    // Find recent grade entries (for quick dashboard display)
    @Query("SELECT g FROM ClassroomGradeEntry g WHERE g.student = :student " +
           "ORDER BY g.assignmentDate DESC")
    List<ClassroomGradeEntry> findRecentByStudent(@Param("student") Student student);
}
