package com.heronix.repository;

import com.heronix.model.domain.Vendor;
import com.heronix.model.domain.Vendor.VendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Vendor Repository
 *
 * Data access for vendor management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    /**
     * Find vendor by vendor code
     */
    Optional<Vendor> findByVendorCode(String vendorCode);

    /**
     * Find all active vendors
     */
    List<Vendor> findByActiveTrue();

    /**
     * Find all approved and active vendors
     */
    List<Vendor> findByStatusAndActiveTrue(VendorStatus status);

    /**
     * Find vendors by category
     */
    @Query("SELECT v FROM Vendor v WHERE v.category.id = :categoryId AND v.active = true AND v.status = 'APPROVED' ORDER BY v.name")
    List<Vendor> findByCategoryIdAndApproved(@Param("categoryId") Long categoryId);

    /**
     * Find vendors by status
     */
    List<Vendor> findByStatusOrderByName(VendorStatus status);

    /**
     * Search vendors by name
     */
    @Query("SELECT v FROM Vendor v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY v.name")
    List<Vendor> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find vendors accepting purchase orders
     */
    List<Vendor> findByAcceptsPurchaseOrderTrueAndActiveTrue();

    /**
     * Find vendors needing sync
     */
    @Query("SELECT v FROM Vendor v WHERE v.syncStatus = 'pending' ORDER BY v.updatedAt")
    List<Vendor> findNeedingSync();

    /**
     * Count active vendors by category
     */
    @Query("SELECT COUNT(v) FROM Vendor v WHERE v.category.id = :categoryId AND v.active = true AND v.status = 'APPROVED'")
    long countByCategoryIdAndApproved(@Param("categoryId") Long categoryId);

    /**
     * Find vendors supporting purchase amount
     */
    @Query("SELECT v FROM Vendor v WHERE v.active = true AND v.status = 'APPROVED' " +
           "AND (v.minPurchaseAmount IS NULL OR v.minPurchaseAmount <= :amount) " +
           "AND (v.maxPurchaseAmount IS NULL OR v.maxPurchaseAmount >= :amount) " +
           "ORDER BY v.name")
    List<Vendor> findSupportingAmount(@Param("amount") BigDecimal amount);

    /**
     * Count all approved vendors
     */
    long countByStatus(VendorStatus status);

    /**
     * Find vendors by tag
     */
    @Query("SELECT v FROM Vendor v WHERE v.tags LIKE %:tag% AND v.active = true ORDER BY v.name")
    List<Vendor> findByTag(@Param("tag") String tag);

    /**
     * Find all district-integrated vendors
     */
    List<Vendor> findByIntegrationEnabledTrue();

    /**
     * Find vendors by sync status
     */
    List<Vendor> findBySyncStatus(String syncStatus);

    /**
     * Find vendor by district vendor ID
     */
    Optional<Vendor> findByDistrictVendorId(String districtVendorId);
}
