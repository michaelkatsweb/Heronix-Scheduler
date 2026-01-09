package com.heronix.service;

import com.heronix.model.domain.AcademicYear;
import com.heronix.repository.AcademicYearRepository;
import com.heronix.repository.ScheduleRepository;
import com.heronix.repository.CourseSectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing academic years.
 *
 * Handles creation, activation, and querying of academic year records.
 *
 * @author Heronix Scheduler Team
 */
@Service
public class AcademicYearService {

    private static final Logger log = LoggerFactory.getLogger(AcademicYearService.class);

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    /**
     * Creates a new academic year.
     *
     * @param yearName year name (e.g., "2024-2025")
     * @param startDate first day of school year
     * @param endDate last day of school year
     * @return created academic year
     */
    @Transactional
    public AcademicYear createAcademicYear(String yearName, LocalDate startDate, LocalDate endDate) {
        log.info("Creating new academic year: {}", yearName);

        // Check if year name already exists
        Optional<AcademicYear> existing = academicYearRepository.findByYearName(yearName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Academic year '" + yearName + "' already exists");
        }

        // Create new year
        AcademicYear year = new AcademicYear(yearName, startDate, endDate);
        year = academicYearRepository.save(year);

        log.info("Created academic year: {} (ID: {})", yearName, year.getId());
        return year;
    }

    /**
     * Gets the currently active academic year.
     *
     * @return active year, or null if none set
     */
    public AcademicYear getActiveYear() {
        return academicYearRepository.findByActiveTrue().orElse(null);
    }

    /**
     * Sets an academic year as active (deactivates others).
     *
     * @param yearId the year to activate
     * @return the activated year
     */
    @Transactional
    public AcademicYear setActiveYear(Long yearId) {
        log.info("Setting active year: {}", yearId);

        // Deactivate current active year
        AcademicYear currentActive = getActiveYear();
        if (currentActive != null) {
            currentActive.setActive(false);
            academicYearRepository.save(currentActive);
            log.info("Deactivated previous active year: {}", currentActive.getYearName());
        }

        // Activate new year
        AcademicYear newActive = academicYearRepository.findById(yearId)
            .orElseThrow(() -> new IllegalArgumentException("Academic year not found: " + yearId));

        newActive.setActive(true);
        newActive = academicYearRepository.save(newActive);

        log.info("Activated academic year: {}", newActive.getYearName());
        return newActive;
    }

    /**
     * Gets all academic years, ordered by start date (most recent first).
     *
     * @return list of all years
     */
    public List<AcademicYear> getAllYears() {
        return academicYearRepository.findAllOrderByStartDateDesc();
    }

    /**
     * Gets an academic year by ID.
     *
     * @param id the year ID
     * @return the academic year
     */
    public AcademicYear getYearById(Long id) {
        return academicYearRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Academic year not found: " + id));
    }

    /**
     * Gets an academic year by name.
     *
     * @param yearName the year name
     * @return the academic year, or null if not found
     */
    public AcademicYear getYearByName(String yearName) {
        return academicYearRepository.findByYearName(yearName).orElse(null);
    }

    /**
     * Schedules graduation for an academic year.
     *
     * @param yearId the year ID
     * @param graduationDate the graduation date
     * @return updated year
     */
    @Transactional
    public AcademicYear scheduleGraduation(Long yearId, LocalDate graduationDate) {
        log.info("Scheduling graduation for year ID {}: {}", yearId, graduationDate);

        AcademicYear year = getYearById(yearId);
        year.setGraduationDate(graduationDate);
        year = academicYearRepository.save(year);

        log.info("Graduation scheduled: {} on {}", year.getYearName(), graduationDate);
        return year;
    }

    /**
     * Marks an academic year as graduated.
     *
     * @param yearId the year ID
     * @return updated year
     */
    @Transactional
    public AcademicYear markAsGraduated(Long yearId) {
        log.info("Marking academic year {} as graduated", yearId);

        AcademicYear year = getYearById(yearId);
        year.setGraduated(true);
        year = academicYearRepository.save(year);

        log.info("Academic year {} marked as graduated", year.getYearName());
        return year;
    }

    /**
     * Updates an academic year.
     *
     * @param year the year to update
     * @return updated year
     */
    @Transactional
    public AcademicYear updateYear(AcademicYear year) {
        log.info("Updating academic year: {}", year.getYearName());
        return academicYearRepository.save(year);
    }

    /**
     * Deletes an academic year.
     * Only allowed if year has no associated data.
     *
     * @param yearId the year ID
     */
    @Transactional
    public void deleteYear(Long yearId) {
        log.warn("Deleting academic year: {}", yearId);

        AcademicYear year = getYearById(yearId);

        // Don't allow deleting active year
        if (year.isActive()) {
            throw new IllegalStateException("Cannot delete active academic year");
        }

        // Check for associated data before deletion
        long scheduleCount = scheduleRepository.countByDateRange(year.getStartDate(), year.getEndDate());
        if (scheduleCount > 0) {
            throw new IllegalStateException(
                String.format("Cannot delete academic year: %d schedule(s) are associated with it", scheduleCount));
        }

        long sectionCount = courseSectionRepository.countByScheduleYear(year.getStartDate().getYear());
        if (sectionCount > 0) {
            throw new IllegalStateException(
                String.format("Cannot delete academic year: %d course section(s) are associated with it", sectionCount));
        }

        academicYearRepository.delete(year);
        log.info("Deleted academic year: {}", year.getYearName());
    }

    /**
     * Checks if an academic year name is available.
     *
     * @param yearName the year name to check
     * @return true if available
     */
    public boolean isYearNameAvailable(String yearName) {
        return academicYearRepository.findByYearName(yearName).isEmpty();
    }

    /**
     * Gets count of all academic years.
     *
     * @return count
     */
    public long getYearCount() {
        return academicYearRepository.count();
    }
}
