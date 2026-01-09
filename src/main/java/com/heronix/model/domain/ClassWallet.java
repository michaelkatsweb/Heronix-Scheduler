package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Class Wallet Transaction Entity
 *
 * Represents a transaction in the classroom economy system where teachers
 * can award points, deduct fines, and track student purchases.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-08
 */
@Entity
@Table(name = "class_wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student this transaction belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Type of transaction
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    /**
     * Amount (positive for credits, negative for debits)
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Student's balance after this transaction
     */
    @Column(name = "balance_after", precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    /**
     * Category for organizing transactions
     * Examples: "Homework", "Behavior", "Participation", "Purchase"
     */
    @Column(name = "category")
    private String category;

    /**
     * Description of the transaction
     */
    @Column(name = "description")
    private String description;

    /**
     * Date the transaction occurred
     */
    @Column(name = "transaction_date", nullable = false)
    @Builder.Default
    private LocalDate transactionDate = LocalDate.now();

    /**
     * Teacher who created this transaction
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    /**
     * Name of the teacher (for display when teacher entity not available)
     */
    @Column(name = "teacher_name")
    private String teacherName;

    /**
     * Whether this transaction has been approved
     * (for purchase approvals, etc.)
     */
    @Column(name = "approved")
    @Builder.Default
    private Boolean approved = true;

    /**
     * Additional notes
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========================================================================
    // VENDOR MANAGEMENT FIELDS (Teacher Stipend Wallet)
    // ========================================================================

    /**
     * Vendor used for this purchase (teacher stipend wallet only)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    /**
     * Vendor category for this purchase (teacher stipend wallet only)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_category_id")
    private VendorCategory vendorCategory;

    /**
     * Purchase order number (teacher stipend wallet only)
     */
    @Column(name = "purchase_order_number", length = 50)
    private String purchaseOrderNumber;

    /**
     * Invoice number from vendor (teacher stipend wallet only)
     */
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    /**
     * Path to uploaded receipt file (teacher stipend wallet only)
     */
    @Column(name = "receipt_path", length = 500)
    private String receiptPath;

    /**
     * Approval status for purchases (teacher stipend wallet only)
     */
    @Column(name = "approval_status", length = 50)
    @Builder.Default
    private String approvalStatus = "PENDING";

    /**
     * User who approved this transaction (teacher stipend wallet only)
     */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /**
     * When this transaction was approved (teacher stipend wallet only)
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Reason for rejection if denied (teacher stipend wallet only)
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Whether notification was sent for this transaction (teacher stipend wallet only)
     */
    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    /**
     * Sync status for Teacher Portal sync
     */
    @Column(name = "sync_status")
    @Builder.Default
    private String syncStatus = "synced";

    /**
     * Created timestamp
     */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Updated timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Transaction types for class wallet
     */
    public enum TransactionType {
        REWARD("Reward", "#4CAF50"),
        FINE("Fine", "#F44336"),
        PURCHASE("Purchase", "#2196F3"),
        REFUND("Refund", "#9C27B0"),
        ADJUSTMENT("Adjustment", "#FF9800");

        private final String displayName;
        private final String color;

        TransactionType(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
    }
}
