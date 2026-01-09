package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vendor Category Entity
 *
 * Organizes vendors by category with approval rules and spending limits.
 *
 * Features:
 * - Category-based organization
 * - Automatic approval thresholds
 * - Visual styling (icon, color)
 * - Display ordering
 * - Active/inactive status
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - Class Wallet Vendor Management
 */
@Entity
@Table(name = "vendor_categories", indexes = {
    @Index(name = "idx_category_active", columnList = "active"),
    @Index(name = "idx_category_order", columnList = "display_order")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // BASIC INFORMATION
    // ========================================================================

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon", length = 50)
    private String icon; // Emoji or icon class

    @Column(name = "color", length = 20)
    private String color; // Hex color code

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // ========================================================================
    // APPROVAL RULES
    // ========================================================================

    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "max_amount_without_approval", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxAmountWithoutApproval = BigDecimal.ZERO;

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
    // BUSINESS METHODS
    // ========================================================================

    /**
     * Check if purchase amount requires approval
     */
    public boolean requiresApprovalForAmount(BigDecimal amount) {
        if (!requiresApproval) {
            return false;
        }
        return maxAmountWithoutApproval == null ||
               amount.compareTo(maxAmountWithoutApproval) > 0;
    }

    /**
     * Get display name with icon
     */
    public String getDisplayNameWithIcon() {
        if (icon != null && !icon.isEmpty()) {
            return icon + " " + name;
        }
        return name;
    }

    @Override
    public String toString() {
        return String.format("VendorCategory{id=%d, name='%s', active=%s, displayOrder=%d}",
            id, name, active, displayOrder);
    }
}
