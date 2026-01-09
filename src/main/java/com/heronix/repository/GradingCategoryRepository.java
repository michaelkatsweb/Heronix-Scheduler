package com.heronix.repository;

import com.heronix.model.domain.GradingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for GradingCategory entity
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Repository
public interface GradingCategoryRepository extends JpaRepository<GradingCategory, Long> {

    /**
     * Find all categories for a course
     */
    List<GradingCategory> findByCourseIdOrderByDisplayOrder(Long courseId);

    /**
     * Find active categories for a course
     */
    List<GradingCategory> findByCourseIdAndActiveTrueOrderByDisplayOrder(Long courseId);

    /**
     * Find by course and category type
     */
    List<GradingCategory> findByCourseIdAndCategoryType(Long courseId, GradingCategory.CategoryType categoryType);

    /**
     * Check if categories sum to 100% for a course
     */
    @Query("SELECT SUM(gc.weight) FROM GradingCategory gc WHERE gc.course.id = :courseId AND gc.active = true")
    Double getTotalWeightForCourse(@Param("courseId") Long courseId);

    /**
     * Count categories for a course
     */
    long countByCourseId(Long courseId);

    /**
     * Delete all categories for a course
     */
    void deleteByCourseId(Long courseId);
}
