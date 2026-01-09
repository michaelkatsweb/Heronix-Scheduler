package com.heronix.repository;

import com.heronix.model.DistrictSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DistrictSettings
 *
 * This is a singleton repository - there should only be one DistrictSettings record.
 *
 * Location: src/main/java/com/eduscheduler/repository/DistrictSettingsRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - District Configuration System
 */
@Repository
public interface DistrictSettingsRepository extends JpaRepository<DistrictSettings, Long> {

    /**
     * Find the first (and should be only) district settings record
     * @return Optional containing the district settings if it exists
     */
    Optional<DistrictSettings> findFirstBy();
}
