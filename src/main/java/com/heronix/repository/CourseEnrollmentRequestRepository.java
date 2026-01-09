package com.heronix.repository;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseEnrollmentRequest;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.EnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CourseEnrollmentRequest Entity
 *
 * Provides custom queries for intelligent course assignment system:
 * - Finding requests by status
 * - Priority-ordered queries
 * - Waitlist management
 * - Student and course filtering
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - November 20, 2025
 */
@Repository
public interface CourseEnrollmentRequestRepository extends JpaRepository<CourseEnrollmentRequest, Long> {

    // ========================================================================
    // BASIC QUERIES - Find by Student/Course/Status
    // ========================================================================

    /**
     * Find all requests for a specific student
     */
    List<CourseEnrollmentRequest> findByStudent(Student student);

    /**
     * Find all requests for a specific student (by student ID)
     */
    List<CourseEnrollmentRequest> findByStudentId(Long studentId);

    /**
     * Find all requests for a specific course
     */
    List<CourseEnrollmentRequest> findByCourse(Course course);

    /**
     * Find all requests for a specific course (by course ID)
     */
    List<CourseEnrollmentRequest> findByCourseId(Long courseId);

    /**
     * Find specific student-course request
     */
    Optional<CourseEnrollmentRequest> findByStudentAndCourse(Student student, Course course);

    /**
     * Find all requests with a specific status
     */
    List<CourseEnrollmentRequest> findByRequestStatus(EnrollmentRequestStatus status);

    // ========================================================================
    // STATUS-BASED QUERIES
    // ========================================================================

    /**
     * Find all pending requests (awaiting processing)
     */
    List<CourseEnrollmentRequest> findByRequestStatusOrderByPriorityScoreDesc(EnrollmentRequestStatus status);

    /**
     * Find all pending requests for a specific course, sorted by priority
     */
    List<CourseEnrollmentRequest> findByCourseAndRequestStatusOrderByPriorityScoreDesc(
        Course course,
        EnrollmentRequestStatus status
    );

    /**
     * Find all pending requests for a student
     */
    List<CourseEnrollmentRequest> findByStudentAndRequestStatus(
        Student student,
        EnrollmentRequestStatus status
    );

    /**
     * Count requests by status
     */
    long countByRequestStatus(EnrollmentRequestStatus status);

    // ========================================================================
    // WAITLIST QUERIES
    // ========================================================================

    /**
     * Find all waitlisted students for a course, ordered by position
     */
    List<CourseEnrollmentRequest> findByCourseAndIsWaitlistTrueOrderByWaitlistPositionAsc(Course course);

    /**
     * Find all waitlisted students for a course (by course ID)
     */
    @Query("SELECT er FROM CourseEnrollmentRequest er WHERE er.course.id = :courseId " +
           "AND er.isWaitlist = true ORDER BY er.waitlistPosition ASC")
    List<CourseEnrollmentRequest> findWaitlistByCourseId(@Param("courseId") Long courseId);

    /**
     * Count students on waitlist for a course
     */
    long countByCourseAndIsWaitlistTrue(Course course);

    /**
     * Get next available waitlist position for a course
     */
    @Query("SELECT COALESCE(MAX(er.waitlistPosition), 0) + 1 FROM CourseEnrollmentRequest er " +
           "WHERE er.course.id = :courseId AND er.isWaitlist = true")
    Integer getNextWaitlistPosition(@Param("courseId") Long courseId);

    // ========================================================================
    // PRIORITY-BASED QUERIES (For AI Assignment)
    // ========================================================================

    /**
     * Find all pending requests, sorted by total priority (highest first)
     * This is the main query for AI assignment algorithm
     */
    @Query("SELECT er FROM CourseEnrollmentRequest er " +
           "WHERE er.requestStatus = 'PENDING' " +
           "ORDER BY er.priorityScore DESC, er.preferenceRank ASC, er.createdAt ASC")
    List<CourseEnrollmentRequest> findAllPendingOrderedByPriority();

    /**
     * Find pending requests for a specific course, ordered by priority
     */
    @Query("SELECT er FROM CourseEnrollmentRequest er " +
           "WHERE er.course.id = :courseId AND er.requestStatus = 'PENDING' " +
           "ORDER BY er.priorityScore DESC, er.preferenceRank ASC, er.createdAt ASC")
    List<CourseEnrollmentRequest> findPendingByCourseOrderedByPriority(@Param("courseId") Long courseId);

    /**
     * Find pending requests for a specific student, ordered by preference rank
     */
    @Query("SELECT er FROM CourseEnrollmentRequest er " +
           "WHERE er.student.id = :studentId AND er.requestStatus = 'PENDING' " +
           "ORDER BY er.preferenceRank ASC")
    List<CourseEnrollmentRequest> findPendingByStudentOrderedByPreference(@Param("studentId") Long studentId);

    /**
     * Find top N priority requests for a course
     */
    @Query("SELECT er FROM CourseEnrollmentRequest er " +
           "WHERE er.course.id = :courseId AND er.requestStatus = 'PENDING' " +
           "ORDER BY er.priorityScore DESC, er.preferenceRank ASC " +
           "LIMIT :limit")
    List<CourseEnrollmentRequest> findTopPriorityRequestsForCourse(
        @Param("courseId") Long courseId,
        @Param("limit") int limit
    );

    // ========================================================================
    // ACADEMIC YEAR QUERIES
    // ========================================================================

    /**
     * Find all requests for a specific academic year
     */
    List<CourseEnrollmentRequest> findByAcademicYearId(Long academicYearId);

    /**
     * Find pending requests for a specific academic year
     */
    @Query("SELECT er FROM CourseEnrollmentRequest er " +
           "WHERE er.academicYearId = :academicYearId AND er.requestStatus = 'PENDING' " +
           "ORDER BY er.priorityScore DESC")
    List<CourseEnrollmentRequest> findPendingByAcademicYearOrderedByPriority(@Param("academicYearId") Long academicYearId);

    /**
     * Count requests by academic year and status
     */
    @Query("SELECT COUNT(er) FROM CourseEnrollmentRequest er " +
           "WHERE er.academicYearId = :academicYearId AND er.requestStatus = :status")
    long countByAcademicYearAndStatus(
        @Param("academicYearId") Long academicYearId,
        @Param("status") EnrollmentRequestStatus status
    );

    // ========================================================================
    // PREFERENCE RANK QUERIES
    // ========================================================================

    /**
     * Find all first-choice requests for a course
     */
    @Query("SELECT er FROM CourseEnrollmentRequest er " +
           "WHERE er.course.id = :courseId AND er.preferenceRank = 1 " +
           "ORDER BY er.priorityScore DESC")
    List<CourseEnrollmentRequest> findFirstChoiceRequestsForCourse(@Param("courseId") Long courseId);

    /**
     * Count how many students have this course as 1st, 2nd, 3rd choice
     */
    @Query("SELECT er.preferenceRank, COUNT(er) FROM CourseEnrollmentRequest er " +
           "WHERE er.course.id = :courseId " +
           "GROUP BY er.preferenceRank " +
           "ORDER BY er.preferenceRank ASC")
    List<Object[]> countRequestsByPreferenceRank(@Param("courseId") Long courseId);

    // ========================================================================
    // MANUAL ASSIGNMENT QUERIES
    // ========================================================================

    /**
     * Find all manually assigned requests
     */
    List<CourseEnrollmentRequest> findByManuallyAssignedTrue();

    /**
     * Find all auto-generated requests
     */
    List<CourseEnrollmentRequest> findByAutoGeneratedTrue();

    /**
     * Find requests assigned by a specific user
     */
    List<CourseEnrollmentRequest> findByAssignedBy(String assignedBy);

    // ========================================================================
    // STATISTICS QUERIES
    // ========================================================================

    /**
     * Get enrollment request statistics (count by status)
     */
    @Query("SELECT er.requestStatus, COUNT(er) FROM CourseEnrollmentRequest er " +
           "GROUP BY er.requestStatus")
    List<Object[]> getRequestCountsByStatus();

    /**
     * Get average priority score by status
     */
    @Query("SELECT er.requestStatus, AVG(er.priorityScore) FROM CourseEnrollmentRequest er " +
           "GROUP BY er.requestStatus")
    List<Object[]> getAveragePriorityByStatus();

    /**
     * Get course demand summary (requests per course)
     */
    @Query("SELECT c.courseCode, c.courseName, COUNT(er) as requestCount " +
           "FROM CourseEnrollmentRequest er JOIN er.course c " +
           "GROUP BY c.id, c.courseCode, c.courseName " +
           "ORDER BY requestCount DESC")
    List<Object[]> getCourseDemandSummary();

    // ========================================================================
    // CLEANUP QUERIES
    // ========================================================================

    /**
     * Delete all requests for a student
     */
    void deleteByStudent(Student student);

    /**
     * Delete all requests for a course
     */
    void deleteByCourse(Course course);

    /**
     * Delete expired requests
     */
    @Query("DELETE FROM CourseEnrollmentRequest er WHERE er.requestStatus = 'EXPIRED'")
    void deleteExpiredRequests();

    // ========================================================================
    // CONFLICT DETECTION QUERIES
    // ========================================================================

    /**
     * Find students with multiple approved requests for the same time slot
     * (To be used with time slot data if available)
     */
    @Query("SELECT er.student, COUNT(er) FROM CourseEnrollmentRequest er " +
           "WHERE er.requestStatus = 'APPROVED' " +
           "GROUP BY er.student " +
           "HAVING COUNT(er) > 7")
    List<Object[]> findStudentsWithTooManyApprovedCourses();

    /**
     * Check if student already has an approved request for this course
     */
    @Query("SELECT CASE WHEN COUNT(er) > 0 THEN true ELSE false END " +
           "FROM CourseEnrollmentRequest er " +
           "WHERE er.student.id = :studentId AND er.course.id = :courseId " +
           "AND er.requestStatus = 'APPROVED'")
    boolean existsApprovedRequest(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    /**
     * Find duplicate requests (same student + course)
     */
    @Query("SELECT er.student.id, er.course.id, COUNT(er) " +
           "FROM CourseEnrollmentRequest er " +
           "GROUP BY er.student.id, er.course.id " +
           "HAVING COUNT(er) > 1")
    List<Object[]> findDuplicateRequests();
}
