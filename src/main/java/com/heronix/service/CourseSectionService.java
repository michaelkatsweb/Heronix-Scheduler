package com.heronix.service;

import com.heronix.model.domain.CourseSection;
import java.util.List;

/**
 * Service interface for CourseSection operations
 * Handles business logic for managing course sections
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-21
 */
public interface CourseSectionService {

    /**
     * Get all sections for a course
     *
     * @param courseId Course ID
     * @return List of sections
     */
    List<CourseSection> getSectionsByCourseId(Long courseId);

    /**
     * Get section by ID
     *
     * @param sectionId Section ID
     * @return CourseSection
     */
    CourseSection getSectionById(Long sectionId);

    /**
     * Create a new section
     *
     * @param section Section to create
     * @return Created section with ID
     */
    CourseSection createSection(CourseSection section);

    /**
     * Update an existing section
     *
     * @param section Section with updated information
     * @return Updated section
     */
    CourseSection updateSection(CourseSection section);

    /**
     * Delete a section
     *
     * @param sectionId Section ID to delete
     */
    void deleteSection(Long sectionId);

    /**
     * Check if teacher is available at a specific period
     * Returns true if teacher is NOT assigned to any section at that period
     *
     * @param teacherId Teacher ID
     * @param period Period number
     * @return true if teacher is available
     */
    boolean isTeacherAvailable(Long teacherId, Integer period);

    /**
     * Check if room is available at a specific period
     * Returns true if room is NOT assigned to any section at that period
     *
     * @param roomId Room ID
     * @param period Period number
     * @return true if room is available
     */
    boolean isRoomAvailable(Long roomId, Integer period);

    /**
     * Get all sections taught by a teacher
     *
     * @param teacherId Teacher ID
     * @return List of sections
     */
    List<CourseSection> getSectionsByTeacherId(Long teacherId);

    /**
     * Generate next section number for a course
     * E.g., if course has sections "1", "2", "3", returns "4"
     *
     * @param courseId Course ID
     * @return Next section number
     */
    String generateNextSectionNumber(Long courseId);
}
