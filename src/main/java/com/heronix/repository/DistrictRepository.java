package com.heronix.repository;

import com.heronix.model.domain.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for District entity operations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - Multi-Campus Federation
 */
@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    /**
     * Find district by unique code
     */
    Optional<District> findByDistrictCode(String districtCode);

    /**
     * Find all active districts
     */
    List<District> findByActiveTrue();

    /**
     * Search districts by name (case-insensitive)
     */
    List<District> findByNameContainingIgnoreCase(String name);

    /**
     * Check if district code exists
     */
    boolean existsByDistrictCode(String districtCode);

    /**
     * Find districts that allow cross-campus enrollment
     */
    List<District> findByAllowCrossCampusEnrollmentTrueAndActiveTrue();

    /**
     * Find districts with centralized scheduling
     */
    List<District> findByCentralizedSchedulingTrueAndActiveTrue();

    /**
     * Find districts by calendar type
     */
    List<District> findByCalendarType(District.CalendarType calendarType);

    /**
     * Find districts by state
     */
    List<District> findByState(String state);

    /**
     * Count active districts
     */
    long countByActiveTrue();

    /**
     * Get districts with shared teacher support
     */
    @Query("SELECT d FROM District d WHERE d.allowSharedTeachers = true AND d.active = true")
    List<District> findDistrictsWithSharedTeacherSupport();

    /**
     * Get district summary with campus count
     */
    @Query("SELECT d.id, d.name, d.districtCode, SIZE(d.campuses) FROM District d WHERE d.active = true")
    List<Object[]> getDistrictSummaries();

    /**
     * Find districts by fiscal year
     */
    List<District> findByFiscalYear(String fiscalYear);
}
