package com.heronix.service.impl;

import com.heronix.model.domain.CourseSection;
import com.heronix.repository.CourseSectionRepository;
import com.heronix.service.CourseSectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of CourseSectionService
 * Handles business logic for course section management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSectionServiceImpl implements CourseSectionService {

    private final CourseSectionRepository courseSectionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseSection> getSectionsByCourseId(Long courseId) {
        log.debug("Loading sections for course ID: {}", courseId);
        List<CourseSection> sections = courseSectionRepository.findByCourseIdWithTeacherAndRoom(courseId);
        log.info("Found {} sections for course ID: {}", sections.size(), courseId);
        return sections;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseSection getSectionById(Long sectionId) {
        log.debug("Loading section ID: {}", sectionId);
        return courseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with ID: " + sectionId));
    }

    @Override
    @Transactional
    public CourseSection createSection(CourseSection section) {
        log.info("Creating new section for course: {}",
                 section.getCourse() != null ? section.getCourse().getCourseCode() : "Unknown");

        // Validation
        if (section.getCourse() == null) {
            throw new IllegalArgumentException("Course cannot be null");
        }

        // Set defaults
        if (section.getCurrentEnrollment() == null) {
            section.setCurrentEnrollment(0);
        }
        if (section.getMaxEnrollment() == null) {
            section.setMaxEnrollment(30);
        }
        if (section.getMinEnrollment() == null) {
            section.setMinEnrollment(10);
        }
        if (section.getTargetEnrollment() == null) {
            section.setTargetEnrollment(25);
        }
        if (section.getWaitlistCount() == null) {
            section.setWaitlistCount(0);
        }
        if (section.getSectionStatus() == null) {
            section.setSectionStatus(CourseSection.SectionStatus.PLANNED);
        }

        CourseSection saved = courseSectionRepository.save(section);
        log.info("Section created successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public CourseSection updateSection(CourseSection section) {
        log.info("Updating section ID: {}", section.getId());

        // Check if section exists
        if (section.getId() == null || !courseSectionRepository.existsById(section.getId())) {
            throw new IllegalArgumentException("Section not found with ID: " + section.getId());
        }

        // Validate assignment conflicts
        if (section.getAssignedTeacher() != null && section.getAssignedPeriod() != null) {
            List<CourseSection> teacherConflicts = courseSectionRepository
                    .findByTeacherIdAndPeriod(section.getAssignedTeacher().getId(), section.getAssignedPeriod());

            // Remove self from conflicts
            teacherConflicts.removeIf(cs -> cs.getId().equals(section.getId()));

            if (!teacherConflicts.isEmpty()) {
                String conflictCourse = teacherConflicts.get(0).getCourse().getCourseCode();
                throw new IllegalStateException(
                        String.format("Teacher is already assigned to %s during period %d",
                                conflictCourse, section.getAssignedPeriod()));
            }
        }

        if (section.getAssignedRoom() != null && section.getAssignedPeriod() != null) {
            List<CourseSection> roomConflicts = courseSectionRepository
                    .findByRoomIdAndPeriod(section.getAssignedRoom().getId(), section.getAssignedPeriod());

            // Remove self from conflicts
            roomConflicts.removeIf(cs -> cs.getId().equals(section.getId()));

            if (!roomConflicts.isEmpty()) {
                String conflictCourse = roomConflicts.get(0).getCourse().getCourseCode();
                throw new IllegalStateException(
                        String.format("Room is already assigned to %s during period %d",
                                conflictCourse, section.getAssignedPeriod()));
            }
        }

        CourseSection updated = courseSectionRepository.save(section);
        log.info("Section updated successfully: {}", updated.getId());
        return updated;
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        log.info("Deleting section ID: {}", sectionId);

        CourseSection section = courseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with ID: " + sectionId));

        // Check if section has students enrolled
        if (section.getCurrentEnrollment() != null && section.getCurrentEnrollment() > 0) {
            throw new IllegalStateException(
                    String.format("Cannot delete section with %d students enrolled. " +
                            "Remove students first or set section status to CANCELLED.",
                            section.getCurrentEnrollment()));
        }

        courseSectionRepository.delete(section);
        log.info("Section deleted successfully: {}", sectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeacherAvailable(Long teacherId, Integer period) {
        if (teacherId == null || period == null) {
            return true; // No conflict if either is null
        }

        List<CourseSection> conflicts = courseSectionRepository.findByTeacherIdAndPeriod(teacherId, period);
        boolean available = conflicts.isEmpty();

        log.debug("Teacher {} {} for period {}",
                teacherId, available ? "is available" : "is NOT available", period);

        return available;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, Integer period) {
        if (roomId == null || period == null) {
            return true; // No conflict if either is null
        }

        List<CourseSection> conflicts = courseSectionRepository.findByRoomIdAndPeriod(roomId, period);
        boolean available = conflicts.isEmpty();

        log.debug("Room {} {} for period {}",
                roomId, available ? "is available" : "is NOT available", period);

        return available;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseSection> getSectionsByTeacherId(Long teacherId) {
        log.debug("Loading sections for teacher ID: {}", teacherId);
        return courseSectionRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateNextSectionNumber(Long courseId) {
        List<CourseSection> sections = courseSectionRepository.findByCourseIdWithTeacherAndRoom(courseId);

        if (sections.isEmpty()) {
            return "1";
        }

        // Find the highest numeric section number
        int maxNumber = sections.stream()
                .map(CourseSection::getSectionNumber)
                .filter(num -> num != null && num.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return String.valueOf(maxNumber + 1);
    }
}
