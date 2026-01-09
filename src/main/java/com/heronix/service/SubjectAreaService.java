package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.SubjectArea;
import com.heronix.model.domain.SubjectRelationship;
import com.heronix.repository.SubjectAreaRepository;
import com.heronix.repository.SubjectRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing Subject Areas and their relationships
 *
 * Location: src/main/java/com/eduscheduler/service/SubjectAreaService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - December 6, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubjectAreaService {

    private final SubjectAreaRepository subjectAreaRepository;
    private final SubjectRelationshipRepository relationshipRepository;

    // ========================================================================
    // BASIC CRUD OPERATIONS
    // ========================================================================

    /**
     * Get all subject areas
     *
     * @return List of all subject areas
     */
    public List<SubjectArea> getAllSubjectAreas() {
        return subjectAreaRepository.findAll();
    }

    /**
     * Get all active subject areas
     *
     * @return List of active subject areas
     */
    public List<SubjectArea> getActiveSubjectAreas() {
        return subjectAreaRepository.findByActiveTrue();
    }

    /**
     * Get subject area by ID
     *
     * @param id Subject area ID
     * @return Optional containing subject area if found
     */
    public Optional<SubjectArea> getSubjectAreaById(Long id) {
        return subjectAreaRepository.findById(id);
    }

    /**
     * Get subject area by code
     *
     * @param code Subject code (e.g., "MATH")
     * @return Optional containing subject area if found
     */
    public Optional<SubjectArea> getSubjectAreaByCode(String code) {
        return subjectAreaRepository.findByCodeIgnoreCase(code);
    }

    /**
     * Get subject area by name
     *
     * @param name Subject name (e.g., "Mathematics")
     * @return Optional containing subject area if found
     */
    public Optional<SubjectArea> getSubjectAreaByName(String name) {
        return subjectAreaRepository.findByNameIgnoreCase(name);
    }

    /**
     * Create new subject area
     *
     * @param subjectArea Subject area to create
     * @return Created subject area
     */
    @Transactional
    public SubjectArea createSubjectArea(SubjectArea subjectArea) {
        log.info("Creating subject area: {} ({})", subjectArea.getName(), subjectArea.getCode());

        // Check for duplicate code
        if (subjectAreaRepository.existsByCodeIgnoreCase(subjectArea.getCode())) {
            throw new IllegalArgumentException(
                "Subject area with code '" + subjectArea.getCode() + "' already exists");
        }

        SubjectArea saved = subjectAreaRepository.save(subjectArea);
        log.info("Created subject area: {}", saved);
        return saved;
    }

    /**
     * Update subject area
     *
     * @param id Subject area ID
     * @param updates Updated subject area data
     * @return Updated subject area
     */
    @Transactional
    public SubjectArea updateSubjectArea(Long id, SubjectArea updates) {
        SubjectArea existing = subjectAreaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject area not found: " + id));

        log.info("Updating subject area: {}", id);

        // Update fields
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getCode() != null && !updates.getCode().equals(existing.getCode())) {
            // Check for duplicate code
            if (subjectAreaRepository.existsByCodeIgnoreCase(updates.getCode())) {
                throw new IllegalArgumentException(
                    "Subject area with code '" + updates.getCode() + "' already exists");
            }
            existing.setCode(updates.getCode());
        }
        if (updates.getDepartment() != null) {
            existing.setDepartment(updates.getDepartment());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getDisplayColor() != null) {
            existing.setDisplayColor(updates.getDisplayColor());
        }
        if (updates.getActive() != null) {
            existing.setActive(updates.getActive());
        }

        return subjectAreaRepository.save(existing);
    }

    /**
     * Delete subject area (soft delete by setting active=false)
     *
     * @param id Subject area ID
     */
    @Transactional
    public void deleteSubjectArea(Long id) {
        SubjectArea subjectArea = subjectAreaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject area not found: " + id));

        log.info("Deleting (deactivating) subject area: {}", id);
        subjectArea.setActive(false);
        subjectAreaRepository.save(subjectArea);
    }

    // ========================================================================
    // HIERARCHY OPERATIONS
    // ========================================================================

    /**
     * Get all top-level subject areas (no parent)
     *
     * @return List of top-level subject areas
     */
    public List<SubjectArea> getTopLevelSubjectAreas() {
        return subjectAreaRepository.findByParentSubjectIsNullAndActiveTrue();
    }

    /**
     * Get child subjects of a parent
     *
     * @param parentId Parent subject area ID
     * @return List of child subject areas
     */
    public List<SubjectArea> getChildSubjects(Long parentId) {
        SubjectArea parent = subjectAreaRepository.findById(parentId)
            .orElseThrow(() -> new IllegalArgumentException("Parent subject not found: " + parentId));

        return subjectAreaRepository.findByParentSubjectAndActiveTrue(parent);
    }

    /**
     * Set parent subject
     *
     * @param childId Child subject ID
     * @param parentId Parent subject ID (null to make top-level)
     * @return Updated child subject
     */
    @Transactional
    public SubjectArea setParentSubject(Long childId, Long parentId) {
        SubjectArea child = subjectAreaRepository.findById(childId)
            .orElseThrow(() -> new IllegalArgumentException("Child subject not found: " + childId));

        SubjectArea parent = null;
        if (parentId != null) {
            parent = subjectAreaRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent subject not found: " + parentId));

            // Prevent circular reference
            if (isDescendantOf(parent, child)) {
                throw new IllegalArgumentException(
                    "Cannot set parent: would create circular reference");
            }
        }

        log.info("Setting parent of {} to {}", childId, parentId);
        child.setParentSubject(parent);
        return subjectAreaRepository.save(child);
    }

    /**
     * Check if subject1 is descendant of subject2 (prevent circular references)
     *
     * @param subject1 Potential descendant
     * @param subject2 Potential ancestor
     * @return true if subject1 is descendant of subject2
     */
    private boolean isDescendantOf(SubjectArea subject1, SubjectArea subject2) {
        if (subject1 == null) return false;
        if (subject1.equals(subject2)) return true;
        return isDescendantOf(subject1.getParentSubject(), subject2);
    }

    // ========================================================================
    // RELATIONSHIP OPERATIONS
    // ========================================================================

    /**
     * Create relationship between two subjects
     *
     * @param subject1Id First subject ID
     * @param subject2Id Second subject ID
     * @param type Relationship type
     * @param strength Relationship strength (1-10)
     * @param description Description
     * @return Created relationship
     */
    @Transactional
    public SubjectRelationship createRelationship(
            Long subject1Id, Long subject2Id,
            SubjectRelationship.RelationshipType type,
            Integer strength, String description) {

        SubjectArea subject1 = subjectAreaRepository.findById(subject1Id)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subject1Id));

        SubjectArea subject2 = subjectAreaRepository.findById(subject2Id)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subject2Id));

        // Check if relationship already exists
        if (relationshipRepository.existsBetweenSubjects(subject1, subject2)) {
            throw new IllegalArgumentException(
                "Relationship already exists between these subjects");
        }

        log.info("Creating relationship: {} ↔ {} ({})",
            subject1.getCode(), subject2.getCode(), type);

        SubjectRelationship relationship = SubjectRelationship.builder()
            .subject1(subject1)
            .subject2(subject2)
            .relationshipType(type)
            .relationshipStrength(strength)
            .description(description)
            .active(true)
            .build();

        return relationshipRepository.save(relationship);
    }

    /**
     * Get all relationships for a subject
     *
     * @param subjectId Subject area ID
     * @return List of relationships
     */
    public List<SubjectRelationship> getRelationships(Long subjectId) {
        SubjectArea subject = subjectAreaRepository.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

        return relationshipRepository.findActiveBySubject(subject);
    }

    /**
     * Get related subjects for a subject
     *
     * @param subjectId Subject area ID
     * @return List of related subject areas
     */
    public List<SubjectArea> getRelatedSubjects(Long subjectId) {
        return relationshipRepository.findRelatedSubjects(subjectId);
    }

    /**
     * Get strongly related subjects (strength >= 7)
     *
     * @param subjectId Subject area ID
     * @return List of strongly related subject areas
     */
    public List<SubjectArea> getStronglyRelatedSubjects(Long subjectId) {
        return relationshipRepository.findStronglyRelatedSubjects(subjectId);
    }

    /**
     * Delete relationship
     *
     * @param relationshipId Relationship ID
     */
    @Transactional
    public void deleteRelationship(Long relationshipId) {
        log.info("Deleting relationship: {}", relationshipId);
        relationshipRepository.deleteById(relationshipId);
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Search subject areas by name, code, or description
     *
     * @param searchTerm Search term
     * @return List of matching subject areas
     */
    public List<SubjectArea> searchSubjectAreas(String searchTerm) {
        return subjectAreaRepository.searchByNameCodeOrDescription(searchTerm);
    }

    /**
     * Get subject areas by department
     *
     * @param department Department name
     * @return List of subject areas
     */
    public List<SubjectArea> getSubjectAreasByDepartment(String department) {
        return subjectAreaRepository.findByDepartmentAndActiveTrue(department);
    }

    /**
     * Get all departments
     *
     * @return List of unique department names
     */
    public List<String> getAllDepartments() {
        return subjectAreaRepository.findAllDepartments();
    }

    /**
     * Get subject areas with courses
     *
     * @return List of subject areas that have courses
     */
    public List<SubjectArea> getSubjectAreasWithCourses() {
        return subjectAreaRepository.findSubjectAreasWithCourses();
    }

    /**
     * Get subject areas without courses
     *
     * @return List of subject areas with no courses
     */
    public List<SubjectArea> getSubjectAreasWithoutCourses() {
        return subjectAreaRepository.findSubjectAreasWithoutCourses();
    }

    /**
     * Get courses for a subject area (including children)
     *
     * @param subjectId Subject area ID
     * @return List of courses
     */
    public List<Course> getCoursesForSubject(Long subjectId) {
        SubjectArea subject = subjectAreaRepository.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

        return subject.getAllCoursesIncludingChildren();
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get statistics about subject areas
     *
     * @return Statistics map
     */
    public SubjectAreaStatistics getStatistics() {
        long total = subjectAreaRepository.count();
        long active = subjectAreaRepository.countByActiveTrue();
        long topLevel = subjectAreaRepository.countByParentSubjectIsNull();
        long withCourses = subjectAreaRepository.findSubjectAreasWithCourses().size();
        long relationships = relationshipRepository.count();

        return SubjectAreaStatistics.builder()
            .totalSubjectAreas(total)
            .activeSubjectAreas(active)
            .topLevelSubjectAreas(topLevel)
            .subjectAreasWithCourses(withCourses)
            .totalRelationships(relationships)
            .build();
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SubjectAreaStatistics {
        private long totalSubjectAreas;
        private long activeSubjectAreas;
        private long topLevelSubjectAreas;
        private long subjectAreasWithCourses;
        private long totalRelationships;

        @Override
        public String toString() {
            return String.format(
                "Subject Area Statistics: Total=%d, Active=%d, Top-Level=%d, With Courses=%d, Relationships=%d",
                totalSubjectAreas, activeSubjectAreas, topLevelSubjectAreas,
                subjectAreasWithCourses, totalRelationships);
        }
    }
}
