package com.heronix.repository;

import com.heronix.model.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AcademicYear entity operations.
 *
 * @author Heronix Scheduler Team
 */
@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    /**
     * Find the currently active academic year.
     *
     * @return the active year, if any
     */
    Optional<AcademicYear> findByActiveTrue();

    /**
     * Find academic year by year name.
     *
     * @param yearName the year name (e.g., "2024-2025")
     * @return the academic year, if found
     */
    Optional<AcademicYear> findByYearName(String yearName);

    /**
     * Find all academic years ordered by start date descending.
     *
     * @return list of years, most recent first
     */
    @Query("SELECT ay FROM AcademicYear ay ORDER BY ay.startDate DESC")
    List<AcademicYear> findAllOrderByStartDateDesc();

    /**
     * Find academic years that have not been marked as graduated.
     *
     * @return list of non-graduated years
     */
    List<AcademicYear> findByGraduatedFalse();

    /**
     * Count active academic years.
     *
     * @return count of active years (should be 0 or 1)
     */
    long countByActiveTrue();
}
