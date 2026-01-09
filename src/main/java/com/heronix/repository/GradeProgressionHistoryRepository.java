package com.heronix.repository;

import com.heronix.model.domain.GradeProgressionHistory;
import com.heronix.model.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GradeProgressionHistory entity operations.
 *
 * @author Heronix Scheduler Team
 */
@Repository
public interface GradeProgressionHistoryRepository extends JpaRepository<GradeProgressionHistory, Long> {

    /**
     * Find progression history for an academic year.
     *
     * @param academicYear the academic year
     * @return the progression record, if exists
     */
    Optional<GradeProgressionHistory> findByAcademicYear(AcademicYear academicYear);

    /**
     * Find all progression history ordered by date descending.
     *
     * @return list of progression events, most recent first
     */
    @Query("SELECT ph FROM GradeProgressionHistory ph ORDER BY ph.progressionDate DESC")
    List<GradeProgressionHistory> findAllOrderByProgressionDateDesc();
}
