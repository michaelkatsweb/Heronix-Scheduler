package com.heronix.repository;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseRecommendation;
import com.heronix.model.domain.CourseSequence;
import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CourseRecommendation entity
 *
 * Provides database access for course recommendation management.
 *
 * Location: src/main/java/com/eduscheduler/repository/CourseRecommendationRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 3 - December 6, 2025 - Intelligent Course Recommendations
 */
@Repository
public interface CourseRecommendationRepository extends JpaRepository<CourseRecommendation, Long> {

    /**
     * Find all active recommendations
     *
     * @return List of active recommendations
     */
    List<CourseRecommendation> findByActiveTrue();

    /**
     * Find all recommendations for a student
     *
     * @param student Student
     * @return List of recommendations
     */
    List<CourseRecommendation> findByStudent(Student student);

    /**
     * Find active recommendations for a student
     *
     * @param student Student
     * @return List of active recommendations
     */
    List<CourseRecommendation> findByStudentAndActiveTrue(Student student);

    /**
     * Find recommendations by status
     *
     * @param status Recommendation status
     * @return List of recommendations
     */
    List<CourseRecommendation> findByStatus(CourseRecommendation.RecommendationStatus status);

    /**
     * Find active recommendations by status
     *
     * @param status Recommendation status
     * @return List of active recommendations
     */
    List<CourseRecommendation> findByStatusAndActiveTrue(CourseRecommendation.RecommendationStatus status);

    /**
     * Find recommendations by type
     *
     * @param type Recommendation type
     * @return List of recommendations
     */
    List<CourseRecommendation> findByRecommendationType(CourseRecommendation.RecommendationType type);

    /**
     * Find recommendations for student by status
     *
     * @param student Student
     * @param status Status
     * @return List of recommendations
     */
    List<CourseRecommendation> findByStudentAndStatus(Student student, CourseRecommendation.RecommendationStatus status);

    /**
     * Find pending recommendations for student
     *
     * @param student Student
     * @return List of pending recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.status = 'PENDING' AND r.active = true")
    List<CourseRecommendation> findPendingForStudent(@Param("student") Student student);

    /**
     * Find accepted recommendations for student
     *
     * @param student Student
     * @return List of accepted recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.status = 'ACCEPTED' AND r.active = true")
    List<CourseRecommendation> findAcceptedForStudent(@Param("student") Student student);

    /**
     * Find recommendations for a course
     *
     * @param course Course
     * @return List of recommendations
     */
    List<CourseRecommendation> findByCourse(Course course);

    /**
     * Find recommendations for student and course
     *
     * @param student Student
     * @param course Course
     * @return Optional containing recommendation if found
     */
    Optional<CourseRecommendation> findByStudentAndCourse(Student student, Course course);

    /**
     * Find recommendations by school year
     *
     * @param schoolYear School year (e.g., "2025-2026")
     * @return List of recommendations
     */
    List<CourseRecommendation> findByRecommendedSchoolYear(String schoolYear);

    /**
     * Find recommendations for student by school year
     *
     * @param student Student
     * @param schoolYear School year
     * @return List of recommendations
     */
    List<CourseRecommendation> findByStudentAndRecommendedSchoolYear(Student student, String schoolYear);

    /**
     * Find recommendations by grade level
     *
     * @param gradeLevel Grade level (9, 10, 11, 12)
     * @return List of recommendations
     */
    List<CourseRecommendation> findByRecommendedGradeLevel(Integer gradeLevel);

    /**
     * Find recommendations for student by grade level
     *
     * @param student Student
     * @param gradeLevel Grade level
     * @return List of recommendations
     */
    List<CourseRecommendation> findByStudentAndRecommendedGradeLevel(Student student, Integer gradeLevel);

    /**
     * Find high priority recommendations (priority <= 3)
     *
     * @return List of high priority recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.priority <= 3 AND r.active = true " +
           "ORDER BY r.priority ASC")
    List<CourseRecommendation> findHighPriorityRecommendations();

    /**
     * Find high priority recommendations for student
     *
     * @param student Student
     * @return List of high priority recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.priority <= 3 AND r.active = true ORDER BY r.priority ASC")
    List<CourseRecommendation> findHighPriorityForStudent(@Param("student") Student student);

    /**
     * Find high confidence recommendations (confidence >= 0.7)
     *
     * @return List of high confidence recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.confidenceScore >= 0.7 AND r.active = true " +
           "ORDER BY r.confidenceScore DESC")
    List<CourseRecommendation> findHighConfidenceRecommendations();

    /**
     * Find high confidence recommendations for student
     *
     * @param student Student
     * @return List of high confidence recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.confidenceScore >= 0.7 AND r.active = true ORDER BY r.confidenceScore DESC")
    List<CourseRecommendation> findHighConfidenceForStudent(@Param("student") Student student);

    /**
     * Find AI-generated recommendations
     *
     * @return List of AI-generated recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.recommendationType = 'AI_GENERATED' " +
           "AND r.active = true")
    List<CourseRecommendation> findAIGeneratedRecommendations();

    /**
     * Find AI-generated recommendations for student
     *
     * @param student Student
     * @return List of AI-generated recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.recommendationType = 'AI_GENERATED' AND r.active = true")
    List<CourseRecommendation> findAIGeneratedForStudent(@Param("student") Student student);

    /**
     * Find recommendations by sequence
     *
     * @param sequence Course sequence
     * @return List of recommendations
     */
    List<CourseRecommendation> findByCourseSequence(CourseSequence sequence);

    /**
     * Find recommendations for student by sequence
     *
     * @param student Student
     * @param sequence Course sequence
     * @return List of recommendations
     */
    List<CourseRecommendation> findByStudentAndCourseSequence(Student student, CourseSequence sequence);

    /**
     * Find recommendations needing approval (pending with no student/parent response)
     *
     * @return List of recommendations needing approval
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.status = 'PENDING' " +
           "AND (r.studentAccepted IS NULL OR r.parentAccepted IS NULL) AND r.active = true")
    List<CourseRecommendation> findNeedingApproval();

    /**
     * Find recommendations for student needing approval
     *
     * @param student Student
     * @return List of recommendations needing approval
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.status = 'PENDING' " +
           "AND (r.studentAccepted IS NULL OR r.parentAccepted IS NULL) AND r.active = true")
    List<CourseRecommendation> findNeedingApprovalForStudent(@Param("student") Student student);

    /**
     * Find recommendations with unmet prerequisites
     *
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.prerequisitesMet = false AND r.active = true")
    List<CourseRecommendation> findWithUnmetPrerequisites();

    /**
     * Find recommendations with unmet GPA requirements
     *
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.gpaRequirementMet = false AND r.active = true")
    List<CourseRecommendation> findWithUnmetGPARequirements();

    /**
     * Find recommendations with schedule conflicts
     *
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.hasScheduleConflict = true AND r.active = true")
    List<CourseRecommendation> findWithScheduleConflicts();

    /**
     * Find recommendations for student meeting all requirements
     *
     * @param student Student
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.prerequisitesMet = true AND r.gpaRequirementMet = true " +
           "AND r.hasScheduleConflict = false AND r.active = true")
    List<CourseRecommendation> findMeetingAllRequirementsForStudent(@Param("student") Student student);

    /**
     * Find recommendations created after a date
     *
     * @param date Date threshold
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.createdAt >= :date AND r.active = true")
    List<CourseRecommendation> findCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Find recommendations created by counselor
     *
     * @param counselorId Counselor staff ID
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.counselor.id = :counselorId AND r.active = true")
    List<CourseRecommendation> findByCounselor(@Param("counselorId") Long counselorId);

    /**
     * Count recommendations by status
     *
     * @param status Status
     * @return Count
     */
    long countByStatusAndActiveTrue(CourseRecommendation.RecommendationStatus status);

    /**
     * Count recommendations for student
     *
     * @param student Student
     * @return Count
     */
    long countByStudentAndActiveTrue(Student student);

    /**
     * Count pending recommendations for student
     *
     * @param student Student
     * @return Count
     */
    @Query("SELECT COUNT(r) FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.status = 'PENDING' AND r.active = true")
    long countPendingForStudent(@Param("student") Student student);

    /**
     * Count accepted recommendations for student
     *
     * @param student Student
     * @return Count
     */
    @Query("SELECT COUNT(r) FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.status = 'ACCEPTED' AND r.active = true")
    long countAcceptedForStudent(@Param("student") Student student);

    /**
     * Check if recommendation exists for student and course
     *
     * @param student Student
     * @param course Course
     * @return true if exists
     */
    boolean existsByStudentAndCourseAndActiveTrue(Student student, Course course);

    /**
     * Find top N recommendations for student by priority and confidence
     *
     * @param student Student
     * @param pageable Pageable (use PageRequest.of(0, limit))
     * @return List of top recommendations
     */
    @Query(value = "SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.status = 'PENDING' AND r.active = true " +
           "ORDER BY r.priority ASC, r.confidenceScore DESC")
    List<CourseRecommendation> findTopRecommendationsForStudent(
        @Param("student") Student student,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Find recommendations expiring soon (for a school year that's about to start)
     *
     * @param schoolYear School year
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.recommendedSchoolYear = :schoolYear " +
           "AND r.status = 'PENDING' AND r.active = true")
    List<CourseRecommendation> findExpiringSoon(@Param("schoolYear") String schoolYear);

    /**
     * Get statistics: count all active recommendations
     *
     * @return Count
     */
    long countByActiveTrue();

    /**
     * Find recommendations with alternatives
     *
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.alternativeCourses IS NOT NULL " +
           "AND r.alternativeCourses != '' AND r.active = true")
    List<CourseRecommendation> findWithAlternatives();

    /**
     * Find fully approved recommendations (both student and parent accepted)
     *
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.studentAccepted = true " +
           "AND r.parentAccepted = true AND r.active = true")
    List<CourseRecommendation> findFullyApproved();

    /**
     * Find fully approved recommendations for student
     *
     * @param student Student
     * @return List of recommendations
     */
    @Query("SELECT r FROM CourseRecommendation r WHERE r.student = :student " +
           "AND r.studentAccepted = true AND r.parentAccepted = true AND r.active = true")
    List<CourseRecommendation> findFullyApprovedForStudent(@Param("student") Student student);
}
