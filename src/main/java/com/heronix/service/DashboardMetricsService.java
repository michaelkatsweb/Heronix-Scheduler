package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Room;
import com.heronix.model.domain.Teacher;
import com.heronix.model.dto.DashboardMetrics;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard Metrics Service - Phase 2 UX Enhancement
 * Location: src/main/java/com/eduscheduler/service/DashboardMetricsService.java
 *
 * Calculates course assignment status metrics and identifies issues
 * requiring attention for the dashboard visual management system.
 *
 * NOTE: Metrics are cached for 5 minutes to improve dashboard performance.
 * Cache is automatically invalidated when courses or teachers are modified.
 *
 * @version 1.1.0
 * @since 2025-11-19
 * @updated 2025-12-10 - Added caching for performance
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class DashboardMetricsService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    /**
     * Calculate comprehensive dashboard metrics including course assignment status
     *
     * Results are cached for 5 minutes to improve dashboard performance.
     *
     * @return DashboardMetrics with all calculated values
     */
    @Cacheable(value = "dashboardMetrics", unless = "#result == null")
    public DashboardMetrics calculateMetrics() {
        log.debug("Calculating dashboard metrics...");

        DashboardMetrics metrics = new DashboardMetrics();

        try {
            // Calculate course assignment metrics
            calculateCourseAssignmentMetrics(metrics);

            // Calculate issue metrics
            calculateIssueMetrics(metrics);

            log.info("Dashboard metrics calculated successfully: {} fully assigned, {} partial, {} unassigned",
                metrics.getFullyAssignedCourses(),
                metrics.getPartiallyAssignedCourses(),
                metrics.getUnassignedCourses());

        } catch (Exception e) {
            log.error("Error calculating dashboard metrics", e);
            // Return partial metrics rather than null
        }

        return metrics;
    }

    /**
     * Calculate course assignment status metrics
     */
    private void calculateCourseAssignmentMetrics(DashboardMetrics metrics) {
        List<Course> allCourses = courseRepository.findAll();
        long totalCourses = allCourses.size();

        // Count courses by assignment status
        long fullyAssigned = allCourses.stream()
            .filter(Course::isFullyAssigned)
            .count();

        long partiallyAssigned = allCourses.stream()
            .filter(Course::isPartiallyAssigned)
            .count();

        long unassigned = allCourses.stream()
            .filter(Course::isUnassigned)
            .count();

        // Set counts
        metrics.setTotalCourses(totalCourses);
        metrics.setFullyAssignedCourses(fullyAssigned);
        metrics.setPartiallyAssignedCourses(partiallyAssigned);
        metrics.setUnassignedCourses(unassigned);

        // Calculate percentages
        if (totalCourses > 0) {
            metrics.setFullyAssignedPercent((fullyAssigned * 100.0) / totalCourses);
            metrics.setPartiallyAssignedPercent((partiallyAssigned * 100.0) / totalCourses);
            metrics.setUnassignedPercent((unassigned * 100.0) / totalCourses);
        } else {
            metrics.setFullyAssignedPercent(0.0);
            metrics.setPartiallyAssignedPercent(0.0);
            metrics.setUnassignedPercent(0.0);
        }

        log.debug("Course assignment metrics: {} total, {} full ({}%), {} partial ({}%), {} unassigned ({}%)",
            totalCourses, fullyAssigned, String.format("%.1f", metrics.getFullyAssignedPercent()),
            partiallyAssigned, String.format("%.1f", metrics.getPartiallyAssignedPercent()),
            unassigned, String.format("%.1f", metrics.getUnassignedPercent()));
    }

    /**
     * Calculate issue metrics for "Attention Required" section
     */
    private void calculateIssueMetrics(DashboardMetrics metrics) {
        // Get all teachers with courses loaded
        List<Teacher> allTeachers = teacherRepository.findAllWithCourses();
        List<Course> allCourses = courseRepository.findAll();

        // 1. Overloaded teachers (6+ courses)
        List<Teacher> overloadedTeachers = allTeachers.stream()
            .filter(Teacher::isOverloaded)
            .collect(Collectors.toList());

        metrics.setOverloadedTeachersCount(overloadedTeachers.size());
        if (overloadedTeachers.isEmpty()) {
            metrics.setOverloadedTeachersMessage("✓ No teachers overloaded");
        } else {
            String teacherNames = overloadedTeachers.stream()
                .limit(2)
                .map(t -> t.getName() + " (" + t.getCourseCount() + " courses)")
                .collect(Collectors.joining(", "));

            if (overloadedTeachers.size() > 2) {
                metrics.setOverloadedTeachersMessage(String.format("• %d teachers overloaded: %s, and %d more",
                    overloadedTeachers.size(), teacherNames, overloadedTeachers.size() - 2));
            } else {
                metrics.setOverloadedTeachersMessage(String.format("• %d teacher(s) overloaded: %s",
                    overloadedTeachers.size(), teacherNames));
            }
        }

        // 2. Underutilized teachers (0 courses)
        List<Teacher> underutilizedTeachers = allTeachers.stream()
            .filter(Teacher::isUnderutilized)
            .collect(Collectors.toList());

        metrics.setUnderutilizedTeachersCount(underutilizedTeachers.size());
        if (underutilizedTeachers.isEmpty()) {
            metrics.setUnderutilizedTeachersMessage("✓ All teachers have course assignments");
        } else {
            String teacherNames = underutilizedTeachers.stream()
                .limit(2)
                .map(Teacher::getName)
                .collect(Collectors.joining(", "));

            if (underutilizedTeachers.size() > 2) {
                metrics.setUnderutilizedTeachersMessage(String.format("• %d teachers unassigned: %s, and %d more",
                    underutilizedTeachers.size(), teacherNames, underutilizedTeachers.size() - 2));
            } else {
                metrics.setUnderutilizedTeachersMessage(String.format("• %d teacher(s) available: %s",
                    underutilizedTeachers.size(), teacherNames));
            }
        }

        // 3. Certification mismatches (courses taught by non-certified teachers)
        long certificationMismatches = allCourses.stream()
            .filter(c -> c.getTeacher() != null)
            .filter(c -> c.getSubject() != null)
            .filter(c -> {
                Teacher teacher = c.getTeacher();
                List<String> certifications = teacher.getCertifiedSubjects();
                return certifications != null && !certifications.contains(c.getSubject());
            })
            .count();

        metrics.setCertificationMismatchCount(certificationMismatches);
        if (certificationMismatches == 0) {
            metrics.setCertificationMismatchMessage("✓ All courses taught by certified teachers");
        } else {
            metrics.setCertificationMismatchMessage(String.format("• %d course(s) have certification mismatch",
                certificationMismatches));
        }

        // 4. Lab room issues (lab courses without lab rooms)
        long labRoomIssues = allCourses.stream()
            .filter(c -> c.getRoom() != null)
            .filter(c -> requiresLab(c))
            .filter(c -> !isLabRoom(c.getRoom()))
            .count();

        metrics.setLabRoomIssuesCount(labRoomIssues);
        if (labRoomIssues == 0) {
            metrics.setLabRoomIssuesMessage("✓ All lab courses have appropriate rooms");
        } else {
            metrics.setLabRoomIssuesMessage(String.format("• %d lab course(s) need lab rooms", labRoomIssues));
        }

        // 5. Capacity issues (rooms too small for enrollment)
        long capacityIssues = allCourses.stream()
            .filter(c -> c.getRoom() != null)
            .filter(c -> c.getMaxStudents() > c.getRoom().getCapacity())
            .count();

        metrics.setCapacityIssuesCount(capacityIssues);
        if (capacityIssues == 0) {
            metrics.setCapacityIssuesMessage("✓ All rooms meet enrollment capacity");
        } else {
            metrics.setCapacityIssuesMessage(String.format("• %d course(s) exceed room capacity", capacityIssues));
        }

        log.debug("Issue metrics: {} overloaded, {} underutilized, {} cert mismatches, {} lab issues, {} capacity issues",
            overloadedTeachers.size(), underutilizedTeachers.size(),
            certificationMismatches, labRoomIssues, capacityIssues);
    }

    /**
     * Check if a course requires a lab
     */
    private boolean requiresLab(Course course) {
        if (course.getSubject() == null) {
            return false;
        }

        String subject = course.getSubject().toLowerCase();
        return subject.contains("science") ||
               subject.contains("chemistry") ||
               subject.contains("biology") ||
               subject.contains("physics") ||
               subject.contains("lab");
    }

    /**
     * Check if a room is a lab room
     */
    private boolean isLabRoom(Room room) {
        if (room.getRoomType() == null) {
            return false;
        }

        return room.getRoomType() == RoomType.LAB ||
               room.getRoomType() == RoomType.SCIENCE_LAB ||
               room.getRoomType() == RoomType.COMPUTER_LAB;
    }

    /**
     * Get the count of fully assigned courses
     *
     * @return count of courses with both teacher and room assigned
     */
    public long countFullyAssignedCourses() {
        return courseRepository.findAll().stream()
            .filter(Course::isFullyAssigned)
            .count();
    }

    /**
     * Get the count of partially assigned courses
     *
     * @return count of courses with only teacher OR room assigned
     */
    public long countPartiallyAssignedCourses() {
        return courseRepository.findAll().stream()
            .filter(Course::isPartiallyAssigned)
            .count();
    }

    /**
     * Get the count of unassigned courses
     *
     * @return count of courses with neither teacher nor room assigned
     */
    public long countUnassignedCourses() {
        return courseRepository.findAll().stream()
            .filter(Course::isUnassigned)
            .count();
    }

    /**
     * Get list of overloaded teachers (6+ courses)
     *
     * @return list of overloaded teachers
     */
    public List<Teacher> getOverloadedTeachers() {
        return teacherRepository.findAllWithCourses().stream()
            .filter(Teacher::isOverloaded)
            .collect(Collectors.toList());
    }

    /**
     * Get list of underutilized teachers (0 courses)
     *
     * @return list of underutilized teachers
     */
    public List<Teacher> getUnderutilizedTeachers() {
        return teacherRepository.findAllWithCourses().stream()
            .filter(Teacher::isUnderutilized)
            .collect(Collectors.toList());
    }

    /**
     * Invalidate dashboard metrics cache
     *
     * Call this method when courses or teachers are modified to ensure
     * fresh metrics on next dashboard load.
     */
    @CacheEvict(value = "dashboardMetrics", allEntries = true)
    public void invalidateMetricsCache() {
        log.debug("Dashboard metrics cache invalidated");
    }

    /**
     * Invalidate cache after course changes
     *
     * This is a convenience method that can be called from course services.
     */
    @CacheEvict(value = "dashboardMetrics", allEntries = true)
    public void onCourseDataChanged() {
        log.info("Course data changed - invalidating dashboard metrics cache");
    }

    /**
     * Invalidate cache after teacher changes
     *
     * This is a convenience method that can be called from teacher services.
     */
    @CacheEvict(value = "dashboardMetrics", allEntries = true)
    public void onTeacherDataChanged() {
        log.info("Teacher data changed - invalidating dashboard metrics cache");
    }
}
