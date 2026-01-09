package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * QR Attendance Log Entity
 * Records every QR code scan for attendance tracking with facial recognition verification.
 *
 * Features:
 * - Two-factor authentication (QR code + facial recognition)
 * - Auto-verification when face match score exceeds threshold
 * - Admin verification workflow for low-confidence matches
 * - Complete audit trail of all scans
 * - Parent notification tracking
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - QR Attendance System Phase 1
 */
@Entity
@Table(name = "qr_attendance_log", indexes = {
    @Index(name = "idx_qr_attendance_student", columnList = "student_id"),
    @Index(name = "idx_qr_attendance_timestamp", columnList = "scan_timestamp"),
    @Index(name = "idx_qr_attendance_period", columnList = "period"),
    @Index(name = "idx_qr_attendance_status", columnList = "verification_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrAttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student who scanned the QR code
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @ToString.Exclude
    private Student student;

    /**
     * QR code that was scanned (for audit trail)
     * Should match student.qrCodeId if legitimate
     */
    @Column(name = "qr_code_scanned", nullable = false)
    private String qrCodeScanned;

    /**
     * Timestamp when QR code was scanned
     */
    @Column(name = "scan_timestamp", nullable = false)
    private LocalDateTime scanTimestamp;

    /**
     * Period number (e.g., 1, 2, 3, etc.)
     * Null for arrival/dismissal scans
     */
    @Column(name = "period")
    private Integer period;

    /**
     * Teacher who verified the attendance
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @ToString.Exclude
    private Teacher teacher;

    /**
     * Room number where scan occurred
     */
    @Column(name = "room_number", length = 50)
    private String roomNumber;

    /**
     * Photo captured during QR scan (for facial verification)
     */
    @Lob
    @Column(name = "captured_photo", columnDefinition = "BLOB")
    private byte[] capturedPhoto;

    /**
     * Facial recognition match score (0.0 to 1.0)
     * Higher score = better match
     * Typical threshold: 0.85 for auto-verification
     */
    @Column(name = "face_match_score")
    private Double faceMatchScore;

    /**
     * Did facial recognition match succeed?
     * True if faceMatchScore >= threshold (default 0.85)
     */
    @Column(name = "face_match_success")
    private Boolean faceMatchSuccess;

    /**
     * Verification status of this attendance record
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /**
     * Notes from admin verification (if manually reviewed)
     */
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    /**
     * User who verified this record (if admin verified)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    @ToString.Exclude
    private User verifiedBy;

    /**
     * Timestamp when admin verified this record
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Has parent been notified of this attendance scan?
     */
    @Column(name = "parent_notified")
    private Boolean parentNotified = false;

    /**
     * Timestamp when parent notification was sent
     */
    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    /**
     * Record creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Verification status enum
     */
    public enum VerificationStatus {
        /**
         * Pending verification - initial state
         */
        PENDING,

        /**
         * Automatically verified - face match score >= threshold
         */
        AUTO_VERIFIED,

        /**
         * Manually verified by admin - face match score was low but admin confirmed identity
         */
        ADMIN_VERIFIED,

        /**
         * Rejected - QR code/face mismatch, suspected cheating
         */
        REJECTED,

        /**
         * Flagged for review - suspicious activity detected
         */
        FLAGGED
    }

    /**
     * JPA lifecycle callback - set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.scanTimestamp == null) {
            this.scanTimestamp = LocalDateTime.now();
        }
    }

    /**
     * JPA lifecycle callback - update timestamp on modification
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this record is verified (either auto or admin)
     */
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.AUTO_VERIFIED ||
               verificationStatus == VerificationStatus.ADMIN_VERIFIED;
    }

    /**
     * Check if this record needs admin review
     */
    public boolean needsReview() {
        return verificationStatus == VerificationStatus.PENDING ||
               verificationStatus == VerificationStatus.FLAGGED;
    }
}
