package com.heronix.repository;

import com.heronix.model.domain.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Campus entity operations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - Multi-Campus Federation
 */
@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {

    /**
     * Find campus by unique code
     */
    Optional<Campus> findByCampusCode(String campusCode);

    /**
     * Find all campuses in a district
     */
    List<Campus> findByDistrictId(Long districtId);

    /**
     * Find all active campuses
     */
    List<Campus> findByActiveTrue();

    /**
     * Find all active campuses in a district
     */
    List<Campus> findByDistrictIdAndActiveTrue(Long districtId);

    /**
     * Find campuses by type
     */
    List<Campus> findByCampusType(Campus.CampusType campusType);

    /**
     * Find campuses by type within a district
     */
    List<Campus> findByDistrictIdAndCampusType(Long districtId, Campus.CampusType campusType);

    /**
     * Search campuses by name (case-insensitive)
     */
    List<Campus> findByNameContainingIgnoreCase(String name);

    /**
     * Check if campus code exists
     */
    boolean existsByCampusCode(String campusCode);

    /**
     * Count active campuses in a district
     */
    long countByDistrictIdAndActiveTrue(Long districtId);

    /**
     * Find campuses that allow shared teachers
     */
    @Query("SELECT c FROM Campus c WHERE c.district.allowSharedTeachers = true AND c.active = true")
    List<Campus> findCampusesWithSharedTeacherSupport();

    /**
     * Find campuses by district code
     */
    @Query("SELECT c FROM Campus c WHERE c.district.districtCode = :districtCode AND c.active = true")
    List<Campus> findByDistrictCode(@Param("districtCode") String districtCode);

    /**
     * Get total enrollment across all campuses in a district
     * Counts students across all campuses in the district
     */
    @Query("SELECT CAST(COUNT(s) AS int) FROM Student s WHERE s.campus.district.id = :districtId AND s.campus.active = true")
    Integer getTotalEnrollmentByDistrict(@Param("districtId") Long districtId);
}
