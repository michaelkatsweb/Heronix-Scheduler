package com.heronix.repository;

import com.heronix.model.domain.ScheduleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ScheduleConfigurationRepository extends JpaRepository<ScheduleConfiguration, Long> {
    Optional<ScheduleConfiguration> findByActiveTrue();
    Optional<ScheduleConfiguration> findByName(String name);
}
