package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Grade Audit Log Entity
 *
 * Tracks all changes made to student grades for accountability and compliance.
 * Used by administrators to monitor grade corrections and ensure data integrity.
 *
 * Features:
 * - Complete audit trail of who changed what and when
 * - Reason tracking for compliance
 * - Before/after values for rollback capability
 * - Admin approval workflow support
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-28
 */
@Entity
@Table(name = "grade_audit_log", indexes = {
    @Index(name = "idx_audit_student_grade", columnList = "student_grade_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_admin", columnList = "edited_by_admin_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The grade record that was modified
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_grade_id", nullable = false)
    private StudentGrade studentGrade;

    /**
     * Administrator who made the edit
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_admin_id", nullable = false)
    private User editedByAdmin;

    /**
     * Field that was changed (e.g., "letterGrade", "numericalGrade", "comments")
     */
    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    /**
     * Previous value (serialized as string)
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * New value (serialized as string)
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Reason for the change (required for compliance)
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * Timestamp of the edit
     */
    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * IP address of the admin who made the edit
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Edit type (MANUAL, TRANSFER, BULK, IMPORT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "edit_type")
    @Builder.Default
    private EditType editType = EditType.MANUAL;

    /**
     * Approval status (for districts requiring admin approval)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    /**
     * Admin who approved the change (if approval required)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_admin_id")
    private User approvedByAdmin;

    /**
     * Approval timestamp
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Is this change reversible?
     */
    @Column(name = "is_reversible")
    @Builder.Default
    private Boolean isReversible = true;

    /**
     * Has this change been reversed?
     */
    @Column(name = "is_reversed")
    @Builder.Default
    private Boolean isReversed = false;

    /**
     * Reference to the reversal audit log entry
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_audit_id")
    private GradeAuditLog reversedByAudit;

    /**
     * Additional notes or comments
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Type of edit operation
     */
    public enum EditType {
        MANUAL("Manual Edit"),
        TRANSFER("Transfer Student Entry"),
        BULK("Bulk Edit Operation"),
        IMPORT("CSV/Data Import"),
        CORRECTION("Teacher Error Correction"),
        SYSTEM("System Automatic");

        private final String displayName;

        EditType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Approval workflow status
     */
    public enum ApprovalStatus {
        PENDING("Pending Approval", "#FF9800"),
        APPROVED("Approved", "#4CAF50"),
        REJECTED("Rejected", "#F44336"),
        AUTO_APPROVED("Auto-Approved", "#2196F3");

        private final String displayName;
        private final String color;

        ApprovalStatus(String displayName, String color) {
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

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get student name for display
     */
    public String getStudentName() {
        return studentGrade != null && studentGrade.getStudent() != null
            ? studentGrade.getStudent().getFullName()
            : "Unknown Student";
    }

    /**
     * Get course name for display
     */
    public String getCourseName() {
        return studentGrade != null && studentGrade.getCourse() != null
            ? studentGrade.getCourse().getCourseName()
            : "Unknown Course";
    }

    /**
     * Get admin name for display
     */
    public String getAdminName() {
        return editedByAdmin != null
            ? editedByAdmin.getUsername()
            : "Unknown Admin";
    }

    /**
     * Get summary description of change
     */
    public String getChangeSummary() {
        return String.format("%s changed from '%s' to '%s'",
            fieldName, oldValue != null ? oldValue : "null", newValue != null ? newValue : "null");
    }

    /**
     * Check if this change is pending approval
     */
    public boolean isPendingApproval() {
        return approvalStatus == ApprovalStatus.PENDING;
    }

    /**
     * Approve this change
     */
    public void approve(User admin) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedByAdmin = admin;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Reject this change
     */
    public void reject(User admin, String rejectionReason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approvedByAdmin = admin;
        this.approvedAt = LocalDateTime.now();
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "Rejection reason: " + rejectionReason;
    }

    /**
     * Mark as reversed
     */
    public void markReversed(GradeAuditLog reversalEntry) {
        this.isReversed = true;
        this.reversedByAudit = reversalEntry;
    }

    /**
     * Check if change can be reversed
     */
    public boolean canBeReversed() {
        return Boolean.TRUE.equals(isReversible) && !Boolean.TRUE.equals(isReversed);
    }

    @Override
    public String toString() {
        return String.format("GradeAuditLog[id=%d, student=%s, course=%s, field=%s, admin=%s, timestamp=%s]",
            id, getStudentName(), getCourseName(), fieldName, getAdminName(), timestamp);
    }
}
