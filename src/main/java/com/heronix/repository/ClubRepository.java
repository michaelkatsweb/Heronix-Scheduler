package com.heronix.repository;

import com.heronix.model.domain.Club;
import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Club management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-08
 */
@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

    /**
     * Find club by exact name
     */
    Optional<Club> findByName(String name);

    /**
     * Find all active clubs
     */
    List<Club> findByActiveTrueOrderByNameAsc();

    /**
     * Find clubs by category
     */
    List<Club> findByCategoryAndActiveTrueOrderByNameAsc(String category);

    /**
     * Find clubs by advisor
     */
    List<Club> findByAdvisorIdOrderByNameAsc(Long advisorId);

    /**
     * Find clubs by advisor name
     */
    List<Club> findByAdvisorNameOrderByNameAsc(String advisorName);

    /**
     * Find clubs meeting on a specific day
     */
    List<Club> findByMeetingDayAndActiveTrueOrderByMeetingTimeAsc(String meetingDay);

    /**
     * Find clubs with available spots
     */
    @Query("SELECT c FROM Club c WHERE c.active = true " +
           "AND (c.maxCapacity IS NULL OR c.currentEnrollment < c.maxCapacity) " +
           "ORDER BY c.name ASC")
    List<Club> findClubsWithAvailability();

    /**
     * Find clubs at capacity
     */
    @Query("SELECT c FROM Club c WHERE c.active = true " +
           "AND c.maxCapacity IS NOT NULL " +
           "AND c.currentEnrollment >= c.maxCapacity " +
           "ORDER BY c.name ASC")
    List<Club> findClubsAtCapacity();

    /**
     * Find clubs a student is a member of
     */
    @Query("SELECT c FROM Club c JOIN c.members m WHERE m.id = :studentId ORDER BY c.name ASC")
    List<Club> findByMemberId(@Param("studentId") Long studentId);

    /**
     * Count how many clubs a student is in
     */
    @Query("SELECT COUNT(c) FROM Club c JOIN c.members m WHERE m.id = :studentId")
    Long countStudentMemberships(@Param("studentId") Long studentId);

    /**
     * Search clubs by name or description
     */
    @Query("SELECT c FROM Club c WHERE c.active = true " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.name ASC")
    List<Club> searchClubs(@Param("searchTerm") String searchTerm);

    /**
     * Find clubs updated after a specific time (for sync)
     */
    List<Club> findByUpdatedAtAfter(LocalDateTime since);

    /**
     * Get most popular clubs (by enrollment)
     */
    @Query("SELECT c FROM Club c WHERE c.active = true " +
           "ORDER BY c.currentEnrollment DESC")
    List<Club> findMostPopular();

    /**
     * Get total number of active clubs
     */
    Long countByActiveTrue();

    /**
     * Get total membership count across all clubs
     */
    @Query("SELECT COALESCE(SUM(c.currentEnrollment), 0) FROM Club c WHERE c.active = true")
    Long getTotalMemberships();

    /**
     * Get list of all categories
     */
    @Query("SELECT DISTINCT c.category FROM Club c WHERE c.active = true AND c.category IS NOT NULL ORDER BY c.category")
    List<String> findAllCategories();
}
