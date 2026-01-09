package com.heronix.controller.api;

import com.heronix.model.domain.Vendor;
import com.heronix.model.domain.Vendor.VendorStatus;
import com.heronix.model.domain.VendorCategory;
import com.heronix.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vendor REST API Controller
 *
 * Provides REST endpoints for vendor management, approval workflows,
 * and category management
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow EduPro-Teacher portal access
public class VendorController {

    private final VendorService vendorService;

    // ============================================================================
    // VENDOR CRUD ENDPOINTS
    // ============================================================================

    /**
     * Get all active vendors
     * GET /api/vendors
     */
    @GetMapping
    public ResponseEntity<List<Vendor>> getAllVendors() {
        log.info("GET /api/vendors - Fetching all active vendors");
        List<Vendor> vendors = vendorService.getAllActiveVendors();
        return ResponseEntity.ok(vendors);
    }

    /**
     * Get all approved vendors (for teacher dropdown)
     * GET /api/vendors/approved
     */
    @GetMapping("/approved")
    public ResponseEntity<List<Vendor>> getApprovedVendors() {
        log.info("GET /api/vendors/approved - Fetching approved vendors");
        List<Vendor> vendors = vendorService.getApprovedVendors();
        return ResponseEntity.ok(vendors);
    }

    /**
     * Get vendor by ID
     * GET /api/vendors/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(@PathVariable Long id) {
        log.info("GET /api/vendors/{} - Fetching vendor", id);
        return vendorService.getVendorById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get vendor by code
     * GET /api/vendors/code/{code}
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Vendor> getVendorByCode(@PathVariable String code) {
        log.info("GET /api/vendors/code/{} - Fetching vendor by code", code);
        return vendorService.getVendorByCode(code)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search vendors by name
     * GET /api/vendors/search?term=staples
     */
    @GetMapping("/search")
    public ResponseEntity<List<Vendor>> searchVendors(@RequestParam String term) {
        log.info("GET /api/vendors/search?term={}", term);
        List<Vendor> vendors = vendorService.searchVendorsByName(term);
        return ResponseEntity.ok(vendors);
    }

    /**
     * Get vendors by category
     * GET /api/vendors/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Vendor>> getVendorsByCategory(@PathVariable Long categoryId) {
        log.info("GET /api/vendors/category/{} - Fetching vendors", categoryId);
        List<Vendor> vendors = vendorService.getApprovedVendorsByCategory(categoryId);
        return ResponseEntity.ok(vendors);
    }

    /**
     * Get vendors supporting purchase amount
     * GET /api/vendors/supporting-amount?amount=500.00
     */
    @GetMapping("/supporting-amount")
    public ResponseEntity<List<Vendor>> getVendorsSupportingAmount(@RequestParam BigDecimal amount) {
        log.info("GET /api/vendors/supporting-amount?amount={}", amount);
        List<Vendor> vendors = vendorService.getVendorsSupportingAmount(amount);
        return ResponseEntity.ok(vendors);
    }

    /**
     * Create new vendor
     * POST /api/vendors
     */
    @PostMapping
    public ResponseEntity<Vendor> createVendor(@Valid @RequestBody Vendor vendor,
                                                 @RequestParam(required = false) String createdBy) {
        log.info("POST /api/vendors - Creating vendor: {}", vendor.getName());
        try {
            String user = createdBy != null ? createdBy : "admin";
            Vendor created = vendorService.createVendor(vendor, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating vendor", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update vendor
     * PUT /api/vendors/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Vendor> updateVendor(@PathVariable Long id,
                                                 @Valid @RequestBody Vendor vendor,
                                                 @RequestParam(required = false) String updatedBy) {
        log.info("PUT /api/vendors/{} - Updating vendor", id);
        try {
            String user = updatedBy != null ? updatedBy : "admin";
            Vendor updated = vendorService.updateVendor(id, vendor, user);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating vendor", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete vendor (soft delete)
     * DELETE /api/vendors/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id,
                                               @RequestParam(required = false) String deletedBy) {
        log.info("DELETE /api/vendors/{} - Deleting vendor", id);
        try {
            String user = deletedBy != null ? deletedBy : "admin";
            vendorService.deleteVendor(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting vendor", e);
            return ResponseEntity.notFound().build();
        }
    }

    // ============================================================================
    // APPROVAL WORKFLOW ENDPOINTS
    // ============================================================================

    /**
     * Approve vendor
     * POST /api/vendors/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Vendor> approveVendor(@PathVariable Long id,
                                                  @RequestParam(required = false) String approvedBy) {
        log.info("POST /api/vendors/{}/approve - Approving vendor", id);
        try {
            String user = approvedBy != null ? approvedBy : "admin";
            Vendor approved = vendorService.approveVendor(id, user);
            return ResponseEntity.ok(approved);
        } catch (IllegalArgumentException e) {
            log.error("Error approving vendor", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reject vendor
     * POST /api/vendors/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Vendor> rejectVendor(@PathVariable Long id,
                                                 @RequestParam String reason) {
        log.info("POST /api/vendors/{}/reject - Rejecting vendor: {}", id, reason);
        try {
            Vendor rejected = vendorService.rejectVendor(id, reason);
            return ResponseEntity.ok(rejected);
        } catch (IllegalArgumentException e) {
            log.error("Error rejecting vendor", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Suspend vendor
     * POST /api/vendors/{id}/suspend
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Vendor> suspendVendor(@PathVariable Long id,
                                                  @RequestParam String reason) {
        log.info("POST /api/vendors/{}/suspend - Suspending vendor: {}", id, reason);
        try {
            Vendor suspended = vendorService.suspendVendor(id, reason);
            return ResponseEntity.ok(suspended);
        } catch (IllegalArgumentException e) {
            log.error("Error suspending vendor", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reactivate vendor
     * POST /api/vendors/{id}/reactivate
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Vendor> reactivateVendor(@PathVariable Long id) {
        log.info("POST /api/vendors/{}/reactivate - Reactivating vendor", id);
        try {
            Vendor reactivated = vendorService.reactivateVendor(id);
            return ResponseEntity.ok(reactivated);
        } catch (IllegalArgumentException e) {
            log.error("Error reactivating vendor", e);
            return ResponseEntity.notFound().build();
        }
    }

    // ============================================================================
    // VALIDATION ENDPOINTS
    // ============================================================================

    /**
     * Check if vendor is available for purchase
     * GET /api/vendors/{id}/available
     */
    @GetMapping("/{id}/available")
    public ResponseEntity<Map<String, Boolean>> isVendorAvailable(@PathVariable Long id) {
        log.info("GET /api/vendors/{}/available - Checking availability", id);
        boolean available = vendorService.isVendorAvailableForPurchase(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if purchase requires quote
     * GET /api/vendors/{id}/requires-quote?amount=5000.00
     */
    @GetMapping("/{id}/requires-quote")
    public ResponseEntity<Map<String, Boolean>> doesRequireQuote(@PathVariable Long id,
                                                                   @RequestParam BigDecimal amount) {
        log.info("GET /api/vendors/{}/requires-quote?amount={}", id, amount);
        boolean requiresQuote = vendorService.doesPurchaseRequireQuote(id, amount);
        Map<String, Boolean> response = new HashMap<>();
        response.put("requiresQuote", requiresQuote);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if purchase is within limits
     * GET /api/vendors/{id}/within-limits?amount=500.00
     */
    @GetMapping("/{id}/within-limits")
    public ResponseEntity<Map<String, Boolean>> isWithinLimits(@PathVariable Long id,
                                                                 @RequestParam BigDecimal amount) {
        log.info("GET /api/vendors/{}/within-limits?amount={}", id, amount);
        boolean withinLimits = vendorService.isPurchaseWithinLimits(id, amount);
        Map<String, Boolean> response = new HashMap<>();
        response.put("withinLimits", withinLimits);
        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // STATISTICS ENDPOINTS
    // ============================================================================

    /**
     * Get vendor statistics
     * GET /api/vendors/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("GET /api/vendors/statistics - Fetching statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", vendorService.getAllActiveVendors().size());
        stats.put("approved", vendorService.getVendorCountByStatus(VendorStatus.APPROVED));
        stats.put("pending", vendorService.getVendorCountByStatus(VendorStatus.PENDING));
        stats.put("suspended", vendorService.getVendorCountByStatus(VendorStatus.SUSPENDED));
        stats.put("rejected", vendorService.getVendorCountByStatus(VendorStatus.REJECTED));
        stats.put("categories", vendorService.getActiveCategoryCount());

        return ResponseEntity.ok(stats);
    }

    // ============================================================================
    // CATEGORY ENDPOINTS
    // ============================================================================

    /**
     * Get all active categories
     * GET /api/vendors/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<VendorCategory>> getAllCategories() {
        log.info("GET /api/vendors/categories - Fetching all categories");
        List<VendorCategory> categories = vendorService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get category by ID
     * GET /api/vendors/categories/{id}
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<VendorCategory> getCategoryById(@PathVariable Long id) {
        log.info("GET /api/vendors/categories/{} - Fetching category", id);
        return vendorService.getCategoryById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get category by name
     * GET /api/vendors/categories/name/{name}
     */
    @GetMapping("/categories/name/{name}")
    public ResponseEntity<VendorCategory> getCategoryByName(@PathVariable String name) {
        log.info("GET /api/vendors/categories/name/{} - Fetching category", name);
        return vendorService.getCategoryByName(name)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new category
     * POST /api/vendors/categories
     */
    @PostMapping("/categories")
    public ResponseEntity<VendorCategory> createCategory(@Valid @RequestBody VendorCategory category,
                                                           @RequestParam(required = false) String createdBy) {
        log.info("POST /api/vendors/categories - Creating category: {}", category.getName());
        try {
            String user = createdBy != null ? createdBy : "admin";
            VendorCategory created = vendorService.createCategory(category, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update category
     * PUT /api/vendors/categories/{id}
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<VendorCategory> updateCategory(@PathVariable Long id,
                                                           @Valid @RequestBody VendorCategory category) {
        log.info("PUT /api/vendors/categories/{} - Updating category", id);
        try {
            VendorCategory updated = vendorService.updateCategory(id, category);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating category", e);
            return ResponseEntity.notFound().build();
        }
    }

    // ============================================================================
    // DISTRICT SYNC ENDPOINTS
    // ============================================================================

    /**
     * Get vendors needing sync
     * GET /api/vendors/sync/pending
     */
    @GetMapping("/sync/pending")
    public ResponseEntity<List<Vendor>> getVendorsNeedingSync() {
        log.info("GET /api/vendors/sync/pending - Fetching vendors needing sync");
        List<Vendor> vendors = vendorService.getVendorsNeedingSync();
        return ResponseEntity.ok(vendors);
    }

    /**
     * Mark vendor as synced
     * POST /api/vendors/{id}/sync
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<Void> markVendorAsSynced(@PathVariable Long id) {
        log.info("POST /api/vendors/{}/sync - Marking vendor as synced", id);
        try {
            vendorService.markVendorAsSynced(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error marking vendor as synced", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Sync vendor from district
     * POST /api/vendors/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<Vendor> syncVendorFromDistrict(@RequestBody Vendor districtVendor) {
        log.info("POST /api/vendors/sync - Syncing vendor from district: {}",
            districtVendor.getDistrictVendorId());
        try {
            Vendor synced = vendorService.syncVendorFromDistrict(districtVendor);
            return ResponseEntity.ok(synced);
        } catch (Exception e) {
            log.error("Error syncing vendor from district", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
