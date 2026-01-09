package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vendor Entity
 *
 * Represents approved vendors for Class Wallet purchases.
 * Managed by district administrators with full audit trail.
 *
 * Features:
 * - District-level approval workflow
 * - Category-based organization
 * - Purchase limits and payment terms
 * - Integration with district finance system
 * - Complete contact and address information
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - Class Wallet Vendor Management
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_status", columnList = "status"),
    @Index(name = "idx_vendor_active", columnList = "active"),
    @Index(name = "idx_vendor_category", columnList = "category_id"),
    @Index(name = "idx_vendor_code", columnList = "vendor_code"),
    @Index(name = "idx_vendor_sync", columnList = "sync_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // BASIC INFORMATION
    // ========================================================================

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "vendor_code", unique = true, length = 50)
    private String vendorCode;

    // ========================================================================
    // CATEGORY
    // ========================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private VendorCategory category;

    // ========================================================================
    // CONTACT INFORMATION
    // ========================================================================

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    // ========================================================================
    // ADDRESS
    // ========================================================================

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "USA";

    // ========================================================================
    // STATUS & APPROVAL
    // ========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private VendorStatus status = VendorStatus.PENDING;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ========================================================================
    // DISTRICT INTEGRATION
    // ========================================================================

    @Column(name = "district_vendor_id", length = 100)
    private String districtVendorId;

    @Column(name = "integration_enabled")
    @Builder.Default
    private Boolean integrationEnabled = false;

    @Column(name = "catalog_url", length = 500)
    private String catalogUrl;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    // ========================================================================
    // PURCHASE LIMITS
    // ========================================================================

    @Column(name = "min_purchase_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO;

    @Column(name = "max_purchase_amount", precision = 10, scale = 2)
    private BigDecimal maxPurchaseAmount;

    @Column(name = "requires_quote_above", precision = 10, scale = 2)
    private BigDecimal requiresQuoteAbove;

    // ========================================================================
    // PAYMENT TERMS
    // ========================================================================

    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Column(name = "accepts_purchase_order")
    @Builder.Default
    private Boolean acceptsPurchaseOrder = true;

    @Column(name = "accepts_credit_card")
    @Builder.Default
    private Boolean acceptsCreditCard = false;

    @Column(name = "accepts_check")
    @Builder.Default
    private Boolean acceptsCheck = true;

    // ========================================================================
    // FEATURES & NOTES
    // ========================================================================

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    // ========================================================================
    // SYNC STATUS
    // ========================================================================

    @Column(name = "sync_status", length = 50)
    @Builder.Default
    private String syncStatus = "pending";

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    // ========================================================================
    // METADATA
    // ========================================================================

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum VendorStatus {
        PENDING("Pending Approval"),
        APPROVED("Approved"),
        SUSPENDED("Suspended"),
        REJECTED("Rejected");

        private final String displayName;

        VendorStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ========================================================================
    // BUSINESS METHODS
    // ========================================================================

    /**
     * Check if vendor can be used for purchases
     */
    public boolean isAvailableForPurchase() {
        return active != null && active &&
               status == VendorStatus.APPROVED;
    }

    /**
     * Check if purchase amount requires quote
     */
    public boolean requiresQuote(BigDecimal amount) {
        return requiresQuoteAbove != null &&
               amount.compareTo(requiresQuoteAbove) > 0;
    }

    /**
     * Check if purchase amount is within limits
     */
    public boolean isWithinPurchaseLimits(BigDecimal amount) {
        boolean aboveMin = minPurchaseAmount == null ||
                          amount.compareTo(minPurchaseAmount) >= 0;
        boolean belowMax = maxPurchaseAmount == null ||
                          amount.compareTo(maxPurchaseAmount) <= 0;
        return aboveMin && belowMax;
    }

    /**
     * Get formatted address
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        if (addressLine1 != null && !addressLine1.isEmpty()) {
            address.append(addressLine1);
        }
        if (addressLine2 != null && !addressLine2.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine2);
        }
        if (city != null && !city.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }
        if (state != null && !state.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(state);
        }
        if (zipCode != null && !zipCode.isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(zipCode);
        }
        return address.toString();
    }

    /**
     * Approve vendor
     */
    public void approve(String approvedByUser) {
        this.status = VendorStatus.APPROVED;
        this.approvedBy = approvedByUser;
        this.approvedAt = LocalDateTime.now();
        this.active = true;
    }

    /**
     * Suspend vendor
     */
    public void suspend() {
        this.status = VendorStatus.SUSPENDED;
        this.active = false;
    }

    /**
     * Reject vendor
     */
    public void reject() {
        this.status = VendorStatus.REJECTED;
        this.active = false;
    }

    /**
     * Reactivate vendor
     */
    public void reactivate() {
        if (this.status == VendorStatus.APPROVED) {
            this.active = true;
        }
    }

    @Override
    public String toString() {
        return String.format("Vendor{id=%d, name='%s', code='%s', status=%s, active=%s}",
            id, name, vendorCode, status, active);
    }
}
