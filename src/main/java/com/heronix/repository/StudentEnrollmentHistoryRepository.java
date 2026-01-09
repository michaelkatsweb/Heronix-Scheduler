package com.heronix.repository;

import com.heronix.model.domain.StudentEnrollmentHistory;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for StudentEnrollmentHistory entity operations.
 *
 * @author Heronix Scheduler Team
 */
@Repository
public interface StudentEnrollmentHistoryRepository extends JpaRepository<StudentEnrollmentHistory, Long> {

    /**
     * Find all enrollment history for a student.
     *
     * @param student the student
     * @return list of historical enrollments
     */
    List<StudentEnrollmentHistory> findByStudent(Student student);

    /**
     * Find enrollment history for a student in a specific academic year.
     *
     * @param student the student
     * @param academicYear the academic year
     * @return list of enrollments for that year
     */
    List<StudentEnrollmentHistory> findByStudentAndAcademicYear(Student student, AcademicYear academicYear);

    /**
     * Find enrollment history for a student at a specific grade level.
     *
     * @param student the student
     * @param gradeLevel the grade level
     * @return list of enrollments at that grade
     */
    List<StudentEnrollmentHistory> findByStudentAndGradeLevel(Student student, Integer gradeLevel);

    /**
     * Find all enrollments for an academic year.
     *
     * @param academicYear the academic year
     * @return list of all enrollments in that year
     */
    List<StudentEnrollmentHistory> findByAcademicYear(AcademicYear academicYear);

    /**
     * Count enrollments for an academic year.
     *
     * @param academicYear the academic year
     * @return count of enrollments
     */
    long countByAcademicYear(AcademicYear academicYear);

    /**
     * Find students who completed a specific course (useful for prerequisites).
     *
     * @param courseId the course ID
     * @return list of students who completed the course
     */
    @Query("SELECT DISTINCT h.student FROM StudentEnrollmentHistory h " +
           "WHERE h.course.id = :courseId AND h.status = 'COMPLETED'")
    List<Student> findStudentsWhoCompletedCourse(@Param("courseId") Long courseId);
}
