package com.heronix.repository;

import com.heronix.model.domain.SubjectArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SubjectArea entity
 *
 * Provides database access for subject area management.
 *
 * Location: src/main/java/com/eduscheduler/repository/SubjectAreaRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - December 6, 2025
 */
@Repository
public interface SubjectAreaRepository extends JpaRepository<SubjectArea, Long> {

    /**
     * Find subject area by code
     *
     * @param code Subject code (e.g., "MATH", "ENG")
     * @return Optional containing subject area if found
     */
    Optional<SubjectArea> findByCode(String code);

    /**
     * Find subject area by code (case-insensitive)
     *
     * @param code Subject code
     * @return Optional containing subject area if found
     */
    Optional<SubjectArea> findByCodeIgnoreCase(String code);

    /**
     * Find subject area by name
     *
     * @param name Subject name (e.g., "Mathematics")
     * @return Optional containing subject area if found
     */
    Optional<SubjectArea> findByName(String name);

    /**
     * Find subject area by name (case-insensitive)
     *
     * @param name Subject name
     * @return Optional containing subject area if found
     */
    Optional<SubjectArea> findByNameIgnoreCase(String name);

    /**
     * Find all active subject areas
     *
     * @return List of active subject areas
     */
    List<SubjectArea> findByActiveTrue();

    /**
     * Find all inactive subject areas
     *
     * @return List of inactive subject areas
     */
    List<SubjectArea> findByActiveFalse();

    /**
     * Find all top-level subject areas (no parent)
     *
     * @return List of top-level subject areas
     */
    List<SubjectArea> findByParentSubjectIsNull();

    /**
     * Find all top-level active subject areas
     *
     * @return List of active top-level subject areas
     */
    List<SubjectArea> findByParentSubjectIsNullAndActiveTrue();

    /**
     * Find child subjects of a parent subject
     *
     * @param parentSubject Parent subject area
     * @return List of child subject areas
     */
    List<SubjectArea> findByParentSubject(SubjectArea parentSubject);

    /**
     * Find active child subjects of a parent subject
     *
     * @param parentSubject Parent subject area
     * @return List of active child subject areas
     */
    List<SubjectArea> findByParentSubjectAndActiveTrue(SubjectArea parentSubject);

    /**
     * Find subject areas by department
     *
     * @param department Department name
     * @return List of subject areas in department
     */
    List<SubjectArea> findByDepartment(String department);

    /**
     * Find active subject areas by department
     *
     * @param department Department name
     * @return List of active subject areas in department
     */
    List<SubjectArea> findByDepartmentAndActiveTrue(String department);

    /**
     * Check if subject area code exists
     *
     * @param code Subject code
     * @return true if code exists
     */
    boolean existsByCode(String code);

    /**
     * Check if subject area code exists (case-insensitive)
     *
     * @param code Subject code
     * @return true if code exists
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Count all subject areas
     *
     * @return Total count
     */
    long count();

    /**
     * Count active subject areas
     *
     * @return Count of active subject areas
     */
    long countByActiveTrue();

    /**
     * Count top-level subject areas
     *
     * @return Count of top-level subject areas
     */
    long countByParentSubjectIsNull();

    /**
     * Find subject areas with courses
     *
     * @return List of subject areas that have at least one course
     */
    @Query("SELECT DISTINCT sa FROM SubjectArea sa WHERE SIZE(sa.courses) > 0")
    List<SubjectArea> findSubjectAreasWithCourses();

    /**
     * Find subject areas without courses
     *
     * @return List of subject areas with no courses
     */
    @Query("SELECT sa FROM SubjectArea sa WHERE SIZE(sa.courses) = 0")
    List<SubjectArea> findSubjectAreasWithoutCourses();

    /**
     * Search subject areas by name or code
     *
     * @param searchTerm Search term
     * @return List of matching subject areas
     */
    @Query("SELECT sa FROM SubjectArea sa WHERE " +
           "LOWER(sa.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sa.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sa.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<SubjectArea> searchByNameCodeOrDescription(@Param("searchTerm") String searchTerm);

    /**
     * Find subject area hierarchy (parent and all children)
     *
     * @param subjectAreaId Subject area ID
     * @return List containing subject and all descendants
     */
    @Query("SELECT sa FROM SubjectArea sa WHERE sa.id = :subjectAreaId OR sa.parentSubject.id = :subjectAreaId")
    List<SubjectArea> findHierarchy(@Param("subjectAreaId") Long subjectAreaId);

    /**
     * Get all departments (distinct)
     *
     * @return List of unique department names
     */
    @Query("SELECT DISTINCT sa.department FROM SubjectArea sa WHERE sa.department IS NOT NULL ORDER BY sa.department")
    List<String> findAllDepartments();
}
