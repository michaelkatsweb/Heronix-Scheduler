package com.heronix.repository;

import com.heronix.model.domain.SchedulerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulerConfigurationRepository extends JpaRepository<SchedulerConfiguration, Long> {

    Optional<SchedulerConfiguration> findByActiveTrue();

    Optional<SchedulerConfiguration> findByName(String name);

    List<SchedulerConfiguration> findByActiveTrueOrderByNameAsc();
}
