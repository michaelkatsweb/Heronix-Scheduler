package com.heronix.repository;

import com.heronix.model.domain.SubjectArea;
import com.heronix.model.domain.SubjectRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SubjectRelationship entity
 *
 * Provides database access for subject relationship management.
 *
 * Location: src/main/java/com/eduscheduler/repository/SubjectRelationshipRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - December 6, 2025
 */
@Repository
public interface SubjectRelationshipRepository extends JpaRepository<SubjectRelationship, Long> {

    /**
     * Find all relationships involving a subject (either as subject1 or subject2)
     *
     * @param subject Subject area
     * @return List of relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE sr.subject1 = :subject OR sr.subject2 = :subject")
    List<SubjectRelationship> findBySubject(@Param("subject") SubjectArea subject);

    /**
     * Find all active relationships involving a subject
     *
     * @param subject Subject area
     * @return List of active relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE (sr.subject1 = :subject OR sr.subject2 = :subject) AND sr.active = true")
    List<SubjectRelationship> findActiveBySubject(@Param("subject") SubjectArea subject);

    /**
     * Find relationship between two subjects (direction-independent)
     *
     * @param subject1 First subject
     * @param subject2 Second subject
     * @return Optional containing relationship if found
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE " +
           "(sr.subject1 = :subject1 AND sr.subject2 = :subject2) OR " +
           "(sr.subject1 = :subject2 AND sr.subject2 = :subject1)")
    Optional<SubjectRelationship> findBetweenSubjects(
        @Param("subject1") SubjectArea subject1,
        @Param("subject2") SubjectArea subject2);

    /**
     * Find all relationships where subject is subject1
     *
     * @param subject Subject area
     * @return List of relationships
     */
    List<SubjectRelationship> findBySubject1(SubjectArea subject);

    /**
     * Find all relationships where subject is subject2
     *
     * @param subject Subject area
     * @return List of relationships
     */
    List<SubjectRelationship> findBySubject2(SubjectArea subject);

    /**
     * Find all active relationships
     *
     * @return List of active relationships
     */
    List<SubjectRelationship> findByActiveTrue();

    /**
     * Find relationships by type
     *
     * @param type Relationship type
     * @return List of relationships
     */
    List<SubjectRelationship> findByRelationshipType(SubjectRelationship.RelationshipType type);

    /**
     * Find active relationships by type
     *
     * @param type Relationship type
     * @return List of active relationships
     */
    List<SubjectRelationship> findByRelationshipTypeAndActiveTrue(SubjectRelationship.RelationshipType type);

    /**
     * Find strong relationships (strength >= 7)
     *
     * @return List of strong relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE sr.relationshipStrength >= 7 AND sr.active = true")
    List<SubjectRelationship> findStrongRelationships();

    /**
     * Find strong relationships involving a subject
     *
     * @param subject Subject area
     * @return List of strong relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE " +
           "(sr.subject1 = :subject OR sr.subject2 = :subject) AND " +
           "sr.relationshipStrength >= 7 AND sr.active = true")
    List<SubjectRelationship> findStrongRelationshipsBySubject(@Param("subject") SubjectArea subject);

    /**
     * Find moderate relationships (strength 4-6)
     *
     * @return List of moderate relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE sr.relationshipStrength >= 4 AND sr.relationshipStrength <= 6 AND sr.active = true")
    List<SubjectRelationship> findModerateRelationships();

    /**
     * Find weak relationships (strength 1-3)
     *
     * @return List of weak relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE sr.relationshipStrength >= 1 AND sr.relationshipStrength <= 3 AND sr.active = true")
    List<SubjectRelationship> findWeakRelationships();

    /**
     * Find prerequisite relationships
     *
     * @return List of prerequisite relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE sr.relationshipType = 'PREREQUISITE' AND sr.active = true")
    List<SubjectRelationship> findPrerequisiteRelationships();

    /**
     * Find complementary relationships (subjects that enhance each other)
     *
     * @return List of complementary relationships
     */
    @Query("SELECT sr FROM SubjectRelationship sr WHERE sr.relationshipType = 'COMPLEMENTARY' AND sr.active = true")
    List<SubjectRelationship> findComplementaryRelationships();

    /**
     * Count relationships for a subject
     *
     * @param subject Subject area
     * @return Count of relationships
     */
    @Query("SELECT COUNT(sr) FROM SubjectRelationship sr WHERE sr.subject1 = :subject OR sr.subject2 = :subject")
    long countBySubject(@Param("subject") SubjectArea subject);

    /**
     * Check if relationship exists between two subjects
     *
     * @param subject1 First subject
     * @param subject2 Second subject
     * @return true if relationship exists
     */
    @Query("SELECT CASE WHEN COUNT(sr) > 0 THEN true ELSE false END FROM SubjectRelationship sr WHERE " +
           "(sr.subject1 = :subject1 AND sr.subject2 = :subject2) OR " +
           "(sr.subject1 = :subject2 AND sr.subject2 = :subject1)")
    boolean existsBetweenSubjects(
        @Param("subject1") SubjectArea subject1,
        @Param("subject2") SubjectArea subject2);

    /**
     * Get all related subjects for a subject (returns the "other" subject in each relationship)
     *
     * @param subjectId Subject area ID
     * @return List of related subject areas
     */
    @Query("SELECT CASE WHEN sr.subject1.id = :subjectId THEN sr.subject2 ELSE sr.subject1 END " +
           "FROM SubjectRelationship sr " +
           "WHERE (sr.subject1.id = :subjectId OR sr.subject2.id = :subjectId) AND sr.active = true")
    List<SubjectArea> findRelatedSubjects(@Param("subjectId") Long subjectId);

    /**
     * Get strongly related subjects for a subject (strength >= 7)
     *
     * @param subjectId Subject area ID
     * @return List of strongly related subject areas
     */
    @Query("SELECT CASE WHEN sr.subject1.id = :subjectId THEN sr.subject2 ELSE sr.subject1 END " +
           "FROM SubjectRelationship sr " +
           "WHERE (sr.subject1.id = :subjectId OR sr.subject2.id = :subjectId) " +
           "AND sr.relationshipStrength >= 7 AND sr.active = true")
    List<SubjectArea> findStronglyRelatedSubjects(@Param("subjectId") Long subjectId);
}
