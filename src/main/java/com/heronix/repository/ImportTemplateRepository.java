package com.heronix.repository;

import com.heronix.model.domain.ImportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing import templates
 */
@Repository
public interface ImportTemplateRepository extends JpaRepository<ImportTemplate, Long> {

    /**
     * Find template by name
     */
    Optional<ImportTemplate> findByName(String name);

    /**
     * Find all templates for specific entity type
     */
    List<ImportTemplate> findByEntityType(String entityType);

    /**
     * Find all templates ordered by last used date
     */
    List<ImportTemplate> findAllByOrderByLastUsedDateDesc();

    /**
     * Find all templates for entity type ordered by name
     */
    List<ImportTemplate> findByEntityTypeOrderByNameAsc(String entityType);

    /**
     * Check if template name exists
     */
    boolean existsByName(String name);
}