package com.heronix.repository;

import com.heronix.model.domain.AcademicPlan;
import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AcademicPlan entity
 *
 * Provides database access for four-year academic plan management.
 *
 * Location: src/main/java/com/eduscheduler/repository/AcademicPlanRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - December 6, 2025 - Four-Year Academic Planning
 */
@Repository
public interface AcademicPlanRepository extends JpaRepository<AcademicPlan, Long> {

    /**
     * Find all active plans
     *
     * @return List of active plans
     */
    List<AcademicPlan> findByActiveTrue();

    /**
     * Find plans by student
     *
     * @param student Student
     * @return List of plans
     */
    List<AcademicPlan> findByStudent(Student student);

    /**
     * Find active plans by student
     *
     * @param student Student
     * @return List of active plans
     */
    List<AcademicPlan> findByStudentAndActiveTrue(Student student);

    /**
     * Find primary plan for student
     *
     * @param student Student
     * @return Optional containing primary plan if found
     */
    Optional<AcademicPlan> findByStudentAndIsPrimaryTrueAndActiveTrue(Student student);

    /**
     * Find plans by status
     *
     * @param status Plan status
     * @return List of plans
     */
    List<AcademicPlan> findByStatus(AcademicPlan.PlanStatus status);

    /**
     * Find active plans by status
     *
     * @param status Plan status
     * @return List of active plans
     */
    List<AcademicPlan> findByStatusAndActiveTrue(AcademicPlan.PlanStatus status);

    /**
     * Find plans by type
     *
     * @param type Plan type
     * @return List of plans
     */
    List<AcademicPlan> findByPlanType(AcademicPlan.PlanType type);

    /**
     * Find draft plans
     *
     * @return List of draft plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.status = 'DRAFT' AND p.active = true")
    List<AcademicPlan> findDraftPlans();

    /**
     * Find draft plans for student
     *
     * @param student Student
     * @return List of draft plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.student = :student " +
           "AND p.status = 'DRAFT' AND p.active = true")
    List<AcademicPlan> findDraftPlansForStudent(@Param("student") Student student);

    /**
     * Find approved plans
     *
     * @return List of approved plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.status IN ('APPROVED', 'ACTIVE') AND p.active = true")
    List<AcademicPlan> findApprovedPlans();

    /**
     * Find approved plans for student
     *
     * @param student Student
     * @return List of approved plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.student = :student " +
           "AND p.status IN ('APPROVED', 'ACTIVE') AND p.active = true")
    List<AcademicPlan> findApprovedPlansForStudent(@Param("student") Student student);

    /**
     * Find plans pending approval
     *
     * @return List of plans pending approval
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.status = 'PENDING_APPROVAL' AND p.active = true")
    List<AcademicPlan> findPlansPendingApproval();

    /**
     * Find plans needing full approval (missing student/parent/counselor)
     *
     * @return List of plans needing approval
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.active = true " +
           "AND (p.studentAccepted IS NULL OR p.studentAccepted = false " +
           "OR p.parentAccepted IS NULL OR p.parentAccepted = false " +
           "OR p.approvedByCounselor IS NULL)")
    List<AcademicPlan> findPlansNeedingApproval();

    /**
     * Find plans by graduation year
     *
     * @param graduationYear Graduation year
     * @return List of plans
     */
    List<AcademicPlan> findByExpectedGraduationYearAndActiveTrue(String graduationYear);

    /**
     * Find plans by start year
     *
     * @param startYear Start year
     * @return List of plans
     */
    List<AcademicPlan> findByStartYearAndActiveTrue(String startYear);

    /**
     * Find plans meeting graduation requirements
     *
     * @return List of plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.meetsGraduationRequirements = true AND p.active = true")
    List<AcademicPlan> findPlansMeetingGraduationRequirements();

    /**
     * Find plans NOT meeting graduation requirements
     *
     * @return List of plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.meetsGraduationRequirements = false AND p.active = true")
    List<AcademicPlan> findPlansNotMeetingGraduationRequirements();

    /**
     * Find plans approved by counselor
     *
     * @param counselorId Counselor user ID
     * @return List of plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.approvedByCounselor.id = :counselorId AND p.active = true")
    List<AcademicPlan> findPlansApprovedByCounselor(@Param("counselorId") Long counselorId);

    /**
     * Find plans created after date
     *
     * @param date Date threshold
     * @return List of plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.createdAt >= :date AND p.active = true")
    List<AcademicPlan> findPlansCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Find plans updated after date
     *
     * @param date Date threshold
     * @return List of plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE p.updatedAt >= :date AND p.active = true")
    List<AcademicPlan> findPlansUpdatedAfter(@Param("date") LocalDateTime date);

    /**
     * Count plans by status
     *
     * @param status Status
     * @return Count
     */
    long countByStatusAndActiveTrue(AcademicPlan.PlanStatus status);

    /**
     * Count plans by student
     *
     * @param student Student
     * @return Count
     */
    long countByStudentAndActiveTrue(Student student);

    /**
     * Check if student has primary plan
     *
     * @param student Student
     * @return true if has primary plan
     */
    boolean existsByStudentAndIsPrimaryTrueAndActiveTrue(Student student);

    /**
     * Find plans by completion percentage range
     *
     * @param minPercentage Minimum percentage
     * @param maxPercentage Maximum percentage
     * @return List of plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE " +
           "(p.totalCreditsCompleted / p.totalCreditsPlanned * 100) >= :minPercentage " +
           "AND (p.totalCreditsCompleted / p.totalCreditsPlanned * 100) <= :maxPercentage " +
           "AND p.active = true")
    List<AcademicPlan> findPlansByCompletionPercentage(
        @Param("minPercentage") double minPercentage,
        @Param("maxPercentage") double maxPercentage);

    /**
     * Find plans with high completion (>= 75%)
     *
     * @return List of plans
     */
    @Query("SELECT p FROM AcademicPlan p WHERE " +
           "(p.totalCreditsCompleted / p.totalCreditsPlanned * 100) >= 75 " +
           "AND p.active = true")
    List<AcademicPlan> findPlansNearCompletion();

    /**
     * Get all active plan count
     *
     * @return Count
     */
    long countByActiveTrue();

    /**
     * Count students who don't have any academic plans
     *
     * @return Count of students without plans
     */
    @Query("SELECT COUNT(DISTINCT s) FROM Student s WHERE s.id NOT IN " +
           "(SELECT DISTINCT p.student.id FROM AcademicPlan p WHERE p.active = true)")
    long countStudentsWithoutPlans();
}
