package com.heronix.repository;

import com.heronix.model.domain.CourseSequence;
import com.heronix.model.domain.SubjectArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CourseSequence entity
 *
 * Provides database access for course sequence and pathway management.
 *
 * Location: src/main/java/com/eduscheduler/repository/CourseSequenceRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 2 - December 6, 2025 - Course Sequencing
 */
@Repository
public interface CourseSequenceRepository extends JpaRepository<CourseSequence, Long> {

    /**
     * Find all active sequences
     *
     * @return List of active sequences
     */
    List<CourseSequence> findByActiveTrue();

    /**
     * Find sequences by subject area
     *
     * @param subjectArea Subject area
     * @return List of sequences
     */
    List<CourseSequence> findBySubjectArea(SubjectArea subjectArea);

    /**
     * Find active sequences by subject area
     *
     * @param subjectArea Subject area
     * @return List of active sequences
     */
    List<CourseSequence> findBySubjectAreaAndActiveTrue(SubjectArea subjectArea);

    /**
     * Find sequences by type
     *
     * @param type Sequence type
     * @return List of sequences
     */
    List<CourseSequence> findBySequenceType(CourseSequence.SequenceType type);

    /**
     * Find active sequences by type
     *
     * @param type Sequence type
     * @return List of active sequences
     */
    List<CourseSequence> findBySequenceTypeAndActiveTrue(CourseSequence.SequenceType type);

    /**
     * Find sequences by subject area and type
     *
     * @param subjectArea Subject area
     * @param type Sequence type
     * @return List of sequences
     */
    List<CourseSequence> findBySubjectAreaAndSequenceType(SubjectArea subjectArea, CourseSequence.SequenceType type);

    /**
     * Find sequence by name (case-insensitive)
     *
     * @param name Sequence name
     * @return Optional containing sequence if found
     */
    Optional<CourseSequence> findByNameIgnoreCase(String name);

    /**
     * Find sequence by code
     *
     * @param code Sequence code
     * @return Optional containing sequence if found
     */
    Optional<CourseSequence> findByCode(String code);

    /**
     * Check if sequence exists by code
     *
     * @param code Sequence code
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Check if sequence exists by code (case-insensitive)
     *
     * @param code Sequence code
     * @return true if exists
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Find sequences with GPA requirement below threshold
     *
     * @param maxGPA Maximum GPA threshold
     * @return List of sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE cs.minGPARecommended <= :maxGPA AND cs.active = true")
    List<CourseSequence> findSequencesWithGPABelow(@Param("maxGPA") Double maxGPA);

    /**
     * Find sequences suitable for student GPA
     *
     * @param studentGPA Student's GPA
     * @return List of sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "(cs.minGPARecommended IS NULL OR cs.minGPARecommended <= :studentGPA) " +
           "AND cs.active = true")
    List<CourseSequence> findSequencesForStudentGPA(@Param("studentGPA") Double studentGPA);

    /**
     * Find sequences by duration (total years)
     *
     * @param years Number of years
     * @return List of sequences
     */
    List<CourseSequence> findByTotalYears(Integer years);

    /**
     * Find sequences for graduation requirement category
     *
     * Searches for sequences that fulfill a specific graduation requirement category.
     * Search is case-insensitive for better usability.
     *
     * @param category Graduation requirement category (e.g., "Mathematics", "Science", "English")
     * @return List of active sequences matching the graduation requirement category
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "LOWER(cs.graduationRequirementCategory) = LOWER(:category) " +
           "AND cs.active = true")
    List<CourseSequence> findByGraduationRequirementCategory(@Param("category") String category);

    /**
     * Search sequences by name, code, or description
     *
     * @param searchTerm Search term
     * @return List of matching sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "(LOWER(cs.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(cs.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(cs.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND cs.active = true")
    List<CourseSequence> searchSequences(@Param("searchTerm") String searchTerm);

    /**
     * Find sequences containing a specific course
     *
     * @param courseId Course ID
     * @return List of sequences
     */
    @Query("SELECT DISTINCT cs FROM CourseSequence cs " +
           "JOIN cs.steps step " +
           "WHERE step.course.id = :courseId AND cs.active = true")
    List<CourseSequence> findSequencesContainingCourse(@Param("courseId") Long courseId);

    /**
     * Find sequences by total credits
     *
     * @param minCredits Minimum credits
     * @param maxCredits Maximum credits
     * @return List of sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "cs.totalCredits >= :minCredits AND cs.totalCredits <= :maxCredits " +
           "AND cs.active = true")
    List<CourseSequence> findSequencesByCredits(
        @Param("minCredits") Double minCredits,
        @Param("maxCredits") Double maxCredits);

    /**
     * Count sequences by type
     *
     * @param type Sequence type
     * @return Count of sequences
     */
    long countBySequenceTypeAndActiveTrue(CourseSequence.SequenceType type);

    /**
     * Count sequences by subject area
     *
     * @param subjectArea Subject area
     * @return Count of sequences
     */
    long countBySubjectAreaAndActiveTrue(SubjectArea subjectArea);

    /**
     * Get all sequence types in use
     *
     * @return List of sequence types
     */
    @Query("SELECT DISTINCT cs.sequenceType FROM CourseSequence cs WHERE cs.active = true")
    List<CourseSequence.SequenceType> findAllSequenceTypes();

    /**
     * Find sequences recommended for specific grade level
     *
     * @param gradeLevel Grade level (9, 10, 11, 12)
     * @return List of sequences
     */
    @Query("SELECT DISTINCT cs FROM CourseSequence cs " +
           "JOIN cs.steps step " +
           "WHERE step.recommendedGradeLevel = :gradeLevel " +
           "AND cs.active = true")
    List<CourseSequence> findSequencesForGradeLevel(@Param("gradeLevel") Integer gradeLevel);

    /**
     * Find sequences suitable for IEP students
     *
     * @return List of IEP-friendly sequences
     */
    List<CourseSequence> findByIepFriendlyTrueAndActiveTrue();

    /**
     * Find sequences by prerequisite skill level
     *
     * Searches for sequences where the prerequisite skills contain the specified skill level.
     * Search is case-insensitive and matches partial strings.
     *
     * @param skillLevel Skill level to search for (e.g., "Algebra I", "reading", "computer")
     * @return List of active sequences with matching prerequisite skills
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "LOWER(cs.prerequisiteSkills) LIKE LOWER(CONCAT('%', :skillLevel, '%')) " +
           "AND cs.active = true")
    List<CourseSequence> findSequencesByPrerequisite(@Param("skillLevel") String skillLevel);

    /**
     * Get statistics: count all active sequences
     *
     * @return Count of active sequences
     */
    long countByActiveTrue();

    /**
     * Find sequences created after a certain date
     *
     * @param date Date threshold
     * @return List of sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE cs.createdAt >= :date AND cs.active = true")
    List<CourseSequence> findSequencesCreatedAfter(@Param("date") java.time.LocalDateTime date);

    /**
     * Find sequences updated after a certain date
     *
     * @param date Date threshold
     * @return List of sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE cs.updatedAt >= :date AND cs.active = true")
    List<CourseSequence> findSequencesUpdatedAfter(@Param("date") java.time.LocalDateTime date);

    /**
     * Find sequences with specific number of steps
     *
     * @param stepCount Number of steps
     * @return List of sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE SIZE(cs.steps) = :stepCount AND cs.active = true")
    List<CourseSequence> findSequencesByStepCount(@Param("stepCount") Integer stepCount);

    /**
     * Find sequences with minimum number of steps
     *
     * @param minSteps Minimum number of steps
     * @return List of sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE SIZE(cs.steps) >= :minSteps AND cs.active = true")
    List<CourseSequence> findSequencesWithMinSteps(@Param("minSteps") Integer minSteps);

    // ========================================================================
    // COST-RELATED QUERIES (Audit Item #16)
    // ========================================================================

    /**
     * Find free sequences (no cost)
     *
     * Returns all active sequences that have no associated costs (cost is null or 0).
     * Useful for showing cost-free academic pathways to students.
     *
     * @return List of free sequences
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "(cs.estimatedTotalCost IS NULL OR cs.estimatedTotalCost = 0.0) " +
           "AND cs.active = true")
    List<CourseSequence> findFreeSequences();

    /**
     * Find sequences with costs
     *
     * Returns all active sequences that have associated costs (cost > 0).
     * Useful for financial planning and scholarship allocation.
     *
     * @return List of sequences with costs
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "cs.estimatedTotalCost IS NOT NULL AND cs.estimatedTotalCost > 0.0 " +
           "AND cs.active = true")
    List<CourseSequence> findSequencesWithCosts();

    /**
     * Find sequences within a cost range
     *
     * Returns active sequences where the total cost falls between minCost and maxCost (inclusive).
     * Useful for filtering sequences by affordability.
     *
     * @param minCost Minimum cost (inclusive)
     * @param maxCost Maximum cost (inclusive)
     * @return List of sequences within the cost range
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE " +
           "cs.estimatedTotalCost >= :minCost AND cs.estimatedTotalCost <= :maxCost " +
           "AND cs.active = true " +
           "ORDER BY cs.estimatedTotalCost ASC")
    List<CourseSequence> findSequencesByCostRange(@Param("minCost") Double minCost,
                                                   @Param("maxCost") Double maxCost);

    /**
     * Find sequences sorted by cost (lowest to highest)
     *
     * Returns all active sequences sorted by cost ascending.
     * Free sequences (cost = 0) appear first.
     *
     * @return List of sequences sorted by cost
     */
    @Query("SELECT cs FROM CourseSequence cs WHERE cs.active = true " +
           "ORDER BY cs.estimatedTotalCost ASC NULLS FIRST")
    List<CourseSequence> findAllSequencesSortedByCost();

    /**
     * Calculate total cost for all active sequences
     *
     * Useful for budget planning and financial reporting.
     *
     * @return Sum of all sequence costs
     */
    @Query("SELECT COALESCE(SUM(cs.estimatedTotalCost), 0.0) FROM CourseSequence cs WHERE cs.active = true")
    Double calculateTotalSequenceCosts();
}
