package com.heronix.repository;

import com.heronix.model.domain.VendorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Vendor Category Repository
 *
 * Data access for vendor categories
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@Repository
public interface VendorCategoryRepository extends JpaRepository<VendorCategory, Long> {

    /**
     * Find category by name
     */
    Optional<VendorCategory> findByName(String name);

    /**
     * Find all active categories
     */
    List<VendorCategory> findByActiveTrueOrderByDisplayOrder();

    /**
     * Find categories requiring approval
     */
    List<VendorCategory> findByRequiresApprovalTrueAndActiveTrueOrderByDisplayOrder();

    /**
     * Find categories not requiring approval
     */
    List<VendorCategory> findByRequiresApprovalFalseAndActiveTrueOrderByDisplayOrder();

    /**
     * Count active categories
     */
    long countByActiveTrue();

    /**
     * Get all categories ordered by display order
     */
    List<VendorCategory> findAllByOrderByDisplayOrder();
}
