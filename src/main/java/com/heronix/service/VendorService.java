package com.heronix.service;

import com.heronix.model.domain.Vendor;
import com.heronix.model.domain.Vendor.VendorStatus;
import com.heronix.model.domain.VendorCategory;
import com.heronix.repository.VendorRepository;
import com.heronix.repository.VendorCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Vendor Service
 *
 * Business logic for vendor management, approval workflows, and district integration
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorCategoryRepository vendorCategoryRepository;

    // ============================================================================
    // VENDOR CRUD OPERATIONS
    // ============================================================================

    /**
     * Create a new vendor (starts as PENDING)
     */
    public Vendor createVendor(Vendor vendor, String createdByUser) {
        log.info("Creating new vendor: {} by user: {}", vendor.getName(), createdByUser);

        // Validate vendor data
        validateVendorData(vendor);

        // Set initial status
        vendor.setStatus(VendorStatus.PENDING);
        vendor.setActive(true);
        vendor.setCreatedBy(createdByUser);
        vendor.setCreatedAt(LocalDateTime.now());
        vendor.setSyncStatus("pending");

        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor created with ID: {}", savedVendor.getId());

        return savedVendor;
    }

    /**
     * Update existing vendor
     */
    public Vendor updateVendor(Long vendorId, Vendor updatedVendor, String updatedByUser) {
        log.info("Updating vendor ID: {} by user: {}", vendorId, updatedByUser);

        Vendor existingVendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));

        // Update fields
        existingVendor.setName(updatedVendor.getName());
        existingVendor.setDescription(updatedVendor.getDescription());
        existingVendor.setCategory(updatedVendor.getCategory());
        existingVendor.setWebsite(updatedVendor.getWebsite());
        existingVendor.setPhone(updatedVendor.getPhone());
        existingVendor.setEmail(updatedVendor.getEmail());
        existingVendor.setContactPerson(updatedVendor.getContactPerson());

        // Address
        existingVendor.setAddressLine1(updatedVendor.getAddressLine1());
        existingVendor.setAddressLine2(updatedVendor.getAddressLine2());
        existingVendor.setCity(updatedVendor.getCity());
        existingVendor.setState(updatedVendor.getState());
        existingVendor.setZipCode(updatedVendor.getZipCode());
        existingVendor.setCountry(updatedVendor.getCountry());

        // Purchase limits
        existingVendor.setMinPurchaseAmount(updatedVendor.getMinPurchaseAmount());
        existingVendor.setMaxPurchaseAmount(updatedVendor.getMaxPurchaseAmount());
        existingVendor.setRequiresQuoteAbove(updatedVendor.getRequiresQuoteAbove());

        // Payment terms
        existingVendor.setPaymentTerms(updatedVendor.getPaymentTerms());
        existingVendor.setAcceptsPurchaseOrder(updatedVendor.getAcceptsPurchaseOrder());
        existingVendor.setAcceptsCreditCard(updatedVendor.getAcceptsCreditCard());
        existingVendor.setAcceptsCheck(updatedVendor.getAcceptsCheck());

        // Notes and tags
        existingVendor.setNotes(updatedVendor.getNotes());
        existingVendor.setTags(updatedVendor.getTags());

        existingVendor.setUpdatedAt(LocalDateTime.now());
        existingVendor.setSyncStatus("pending"); // Mark for sync

        Vendor savedVendor = vendorRepository.save(existingVendor);
        log.info("Vendor updated: {}", savedVendor.getId());

        return savedVendor;
    }

    /**
     * Delete vendor (soft delete by setting active=false)
     */
    public void deleteVendor(Long vendorId, String deletedByUser) {
        log.info("Deleting vendor ID: {} by user: {}", vendorId, deletedByUser);

        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));

        vendor.setActive(false);
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setSyncStatus("pending");

        vendorRepository.save(vendor);
        log.info("Vendor soft-deleted: {}", vendorId);
    }

    /**
     * Get vendor by ID
     */
    @Transactional(readOnly = true)
    public Optional<Vendor> getVendorById(Long vendorId) {
        return vendorRepository.findById(vendorId);
    }

    /**
     * Get vendor by code
     */
    @Transactional(readOnly = true)
    public Optional<Vendor> getVendorByCode(String vendorCode) {
        return vendorRepository.findByVendorCode(vendorCode);
    }

    /**
     * Get all active vendors
     */
    @Transactional(readOnly = true)
    public List<Vendor> getAllActiveVendors() {
        return vendorRepository.findByActiveTrue();
    }

    /**
     * Get all approved vendors (for teacher dropdown)
     */
    @Transactional(readOnly = true)
    public List<Vendor> getApprovedVendors() {
        return vendorRepository.findByStatusAndActiveTrue(VendorStatus.APPROVED);
    }

    /**
     * Get approved vendors by category
     */
    @Transactional(readOnly = true)
    public List<Vendor> getApprovedVendorsByCategory(Long categoryId) {
        return vendorRepository.findByCategoryIdAndApproved(categoryId);
    }

    /**
     * Search vendors by name
     */
    @Transactional(readOnly = true)
    public List<Vendor> searchVendorsByName(String searchTerm) {
        return vendorRepository.searchByName(searchTerm);
    }

    /**
     * Get vendors supporting specific purchase amount
     */
    @Transactional(readOnly = true)
    public List<Vendor> getVendorsSupportingAmount(BigDecimal amount) {
        return vendorRepository.findSupportingAmount(amount);
    }

    // ============================================================================
    // APPROVAL WORKFLOW
    // ============================================================================

    /**
     * Approve vendor
     */
    public Vendor approveVendor(Long vendorId, String approvedByUser) {
        log.info("Approving vendor ID: {} by user: {}", vendorId, approvedByUser);

        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));

        if (vendor.getStatus() == VendorStatus.APPROVED) {
            log.warn("Vendor already approved: {}", vendorId);
            return vendor;
        }

        vendor.approve(approvedByUser);
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setSyncStatus("pending");

        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor approved: {}", savedVendor.getId());

        return savedVendor;
    }

    /**
     * Reject vendor
     */
    public Vendor rejectVendor(Long vendorId, String reason) {
        log.info("Rejecting vendor ID: {} with reason: {}", vendorId, reason);

        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));

        vendor.reject();
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setSyncStatus("pending");

        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor rejected: {}", savedVendor.getId());

        return savedVendor;
    }

    /**
     * Suspend vendor (temporarily disable)
     */
    public Vendor suspendVendor(Long vendorId, String reason) {
        log.info("Suspending vendor ID: {} with reason: {}", vendorId, reason);

        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));

        vendor.suspend();
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setSyncStatus("pending");

        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor suspended: {}", savedVendor.getId());

        return savedVendor;
    }

    /**
     * Reactivate vendor
     */
    public Vendor reactivateVendor(Long vendorId) {
        log.info("Reactivating vendor ID: {}", vendorId);

        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));

        vendor.reactivate();
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setSyncStatus("pending");

        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor reactivated: {}", savedVendor.getId());

        return savedVendor;
    }

    // ============================================================================
    // VENDOR CATEGORY OPERATIONS
    // ============================================================================

    /**
     * Create vendor category
     */
    public VendorCategory createCategory(VendorCategory category, String createdByUser) {
        log.info("Creating vendor category: {} by user: {}", category.getName(), createdByUser);

        category.setCreatedBy(createdByUser);
        category.setCreatedAt(LocalDateTime.now());

        VendorCategory savedCategory = vendorCategoryRepository.save(category);
        log.info("Category created with ID: {}", savedCategory.getId());

        return savedCategory;
    }

    /**
     * Update vendor category
     */
    public VendorCategory updateCategory(Long categoryId, VendorCategory updatedCategory) {
        log.info("Updating vendor category ID: {}", categoryId);

        VendorCategory existingCategory = vendorCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        existingCategory.setName(updatedCategory.getName());
        existingCategory.setDescription(updatedCategory.getDescription());
        existingCategory.setIcon(updatedCategory.getIcon());
        existingCategory.setColor(updatedCategory.getColor());
        existingCategory.setRequiresApproval(updatedCategory.getRequiresApproval());
        existingCategory.setMaxAmountWithoutApproval(updatedCategory.getMaxAmountWithoutApproval());
        existingCategory.setDisplayOrder(updatedCategory.getDisplayOrder());
        existingCategory.setUpdatedAt(LocalDateTime.now());

        VendorCategory savedCategory = vendorCategoryRepository.save(existingCategory);
        log.info("Category updated: {}", savedCategory.getId());

        return savedCategory;
    }

    /**
     * Get all active categories
     */
    @Transactional(readOnly = true)
    public List<VendorCategory> getAllActiveCategories() {
        return vendorCategoryRepository.findByActiveTrueOrderByDisplayOrder();
    }

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public Optional<VendorCategory> getCategoryById(Long categoryId) {
        return vendorCategoryRepository.findById(categoryId);
    }

    /**
     * Get category by name
     */
    @Transactional(readOnly = true)
    public Optional<VendorCategory> getCategoryByName(String name) {
        return vendorCategoryRepository.findByName(name);
    }

    // ============================================================================
    // VALIDATION & BUSINESS LOGIC
    // ============================================================================

    /**
     * Check if vendor can be used for purchase
     */
    @Transactional(readOnly = true)
    public boolean isVendorAvailableForPurchase(Long vendorId) {
        return vendorRepository.findById(vendorId)
            .map(Vendor::isAvailableForPurchase)
            .orElse(false);
    }

    /**
     * Check if purchase amount requires quote
     */
    @Transactional(readOnly = true)
    public boolean doesPurchaseRequireQuote(Long vendorId, BigDecimal amount) {
        return vendorRepository.findById(vendorId)
            .map(vendor -> vendor.requiresQuote(amount))
            .orElse(false);
    }

    /**
     * Check if purchase amount is within limits
     */
    @Transactional(readOnly = true)
    public boolean isPurchaseWithinLimits(Long vendorId, BigDecimal amount) {
        return vendorRepository.findById(vendorId)
            .map(vendor -> vendor.isWithinPurchaseLimits(amount))
            .orElse(false);
    }

    /**
     * Get vendor statistics by status
     */
    @Transactional(readOnly = true)
    public long getVendorCountByStatus(VendorStatus status) {
        return vendorRepository.countByStatus(status);
    }

    /**
     * Get active category count
     */
    @Transactional(readOnly = true)
    public long getActiveCategoryCount() {
        return vendorCategoryRepository.countByActiveTrue();
    }

    // ============================================================================
    // DISTRICT SYNC OPERATIONS
    // ============================================================================

    /**
     * Mark vendor as synced
     */
    public void markVendorAsSynced(Long vendorId) {
        vendorRepository.findById(vendorId).ifPresent(vendor -> {
            vendor.setSyncStatus("synced");
            vendor.setLastSync(LocalDateTime.now());
            vendorRepository.save(vendor);
            log.info("Vendor marked as synced: {}", vendorId);
        });
    }

    /**
     * Get vendors needing sync
     */
    @Transactional(readOnly = true)
    public List<Vendor> getVendorsNeedingSync() {
        return vendorRepository.findBySyncStatus("pending");
    }

    /**
     * Sync vendor from district
     */
    public Vendor syncVendorFromDistrict(Vendor districtVendor) {
        log.info("Syncing vendor from district: {}", districtVendor.getDistrictVendorId());

        Optional<Vendor> existingVendor = vendorRepository.findByDistrictVendorId(
            districtVendor.getDistrictVendorId()
        );

        if (existingVendor.isPresent()) {
            // Update existing vendor
            Vendor vendor = existingVendor.get();
            updateVendorFromDistrict(vendor, districtVendor);
            return vendorRepository.save(vendor);
        } else {
            // Create new vendor from district
            districtVendor.setSyncStatus("synced");
            districtVendor.setLastSync(LocalDateTime.now());
            return vendorRepository.save(districtVendor);
        }
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private void validateVendorData(Vendor vendor) {
        if (vendor.getName() == null || vendor.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor name is required");
        }

        if (vendor.getVendorCode() != null) {
            Optional<Vendor> existing = vendorRepository.findByVendorCode(vendor.getVendorCode());
            boolean isDuplicate = existing
                .map(v -> !v.getId().equals(vendor.getId()))
                .orElse(false);
            if (isDuplicate) {
                throw new IllegalArgumentException("Vendor code already exists: " + vendor.getVendorCode());
            }
        }
    }

    private void updateVendorFromDistrict(Vendor localVendor, Vendor districtVendor) {
        localVendor.setName(districtVendor.getName());
        localVendor.setDescription(districtVendor.getDescription());
        localVendor.setStatus(districtVendor.getStatus());
        localVendor.setActive(districtVendor.getActive());
        localVendor.setWebsite(districtVendor.getWebsite());
        localVendor.setPhone(districtVendor.getPhone());
        localVendor.setEmail(districtVendor.getEmail());
        localVendor.setMinPurchaseAmount(districtVendor.getMinPurchaseAmount());
        localVendor.setMaxPurchaseAmount(districtVendor.getMaxPurchaseAmount());
        localVendor.setRequiresQuoteAbove(districtVendor.getRequiresQuoteAbove());
        localVendor.setPaymentTerms(districtVendor.getPaymentTerms());
        localVendor.setSyncStatus("synced");
        localVendor.setLastSync(LocalDateTime.now());
        localVendor.setUpdatedAt(LocalDateTime.now());
    }
}
