package com.heronix.model.domain;

import com.heronix.model.enums.CertificationType;
import com.heronix.model.enums.USState;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Subject Certification Entity - UNIVERSAL/MULTI-STATE SUPPORT
 * Represents a specific subject certification from any US state
 * (e.g., FL Mathematics 6-12, CA Single Subject Math, TX 7-12 Mathematics, etc.)
 *
 * Supports all 50 states + DC + territories with flexible naming
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - Multi-State Support
 * @since 2025-11-06
 */
@Entity
@Table(name = "subject_certifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Subject name (e.g., "Mathematics", "Science", "English")
     */
    @Column(nullable = false)
    private String subject;

    /**
     * Grade range covered by certification (e.g., "K-6", "6-12", "K-12")
     */
    @Column(name = "grade_range")
    private String gradeRange;

    /**
     * Certification number/ID from certifying agency
     */
    @Column(name = "certification_number")
    private String certificationNumber;

    /**
     * Type of certification (PROFESSIONAL, TEMPORARY, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "certification_type")
    private CertificationType certificationType;

    /**
     * State that issued the certification (FL, CA, TX, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "issuing_state")
    private USState issuingState = USState.FL;

    /**
     * Issuing agency (auto-populated from state, but can be overridden)
     * Examples: "FL DOE", "CA CTC", "TX TEA", "National Board"
     */
    @Column(name = "issuing_agency")
    private String issuingAgency;

    /**
     * Date certification was issued
     */
    @Column(name = "issue_date")
    private LocalDate issueDate;

    /**
     * Date certification expires (null if no expiration)
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Is this certification currently active?
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Additional specializations (e.g., "AP Calculus", "ESE", "ESOL")
     */
    @Column(name = "specializations", columnDefinition = "TEXT")
    private String specializations;

    /**
     * Notes about this certification
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Teacher who holds this certification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if certification is currently valid
     */
    public boolean isValid() {
        if (!Boolean.TRUE.equals(active)) {
            return false;
        }
        if (expirationDate == null) {
            return true; // No expiration
        }
        return !LocalDate.now().isAfter(expirationDate);
    }

    /**
     * Check if certification is expiring soon (within 90 days)
     */
    public boolean isExpiringSoon() {
        if (expirationDate == null) {
            return false;
        }
        LocalDate threeMonthsFromNow = LocalDate.now().plusMonths(3);
        return expirationDate.isBefore(threeMonthsFromNow) && expirationDate.isAfter(LocalDate.now());
    }

    /**
     * Check if certification has expired
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expirationDate);
    }

    /**
     * Get status string for display
     */
    public String getStatusString() {
        if (!Boolean.TRUE.equals(active)) {
            return "Inactive";
        }
        if (isExpired()) {
            return "Expired";
        }
        if (isExpiringSoon()) {
            return "Expiring Soon";
        }
        return "Active";
    }

    /**
     * Get display string for certification
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();

        // Add state abbreviation prefix
        if (issuingState != null) {
            sb.append("[").append(issuingState.name()).append("] ");
        }

        sb.append(subject);
        if (gradeRange != null && !gradeRange.isEmpty()) {
            sb.append(" (").append(gradeRange).append(")");
        }
        sb.append(" - ").append(certificationType.getDisplayName());
        return sb.toString();
    }

    /**
     * Ensure issuing agency is populated from state if not set
     */
    @PrePersist
    @PreUpdate
    private void ensureIssuingAgency() {
        if (issuingAgency == null && issuingState != null) {
            issuingAgency = issuingState.getCertifyingAgency();
        }
    }

    /**
     * Check if this certification covers a specific grade level
     */
    public boolean coversGradeLevel(String gradeLevel) {
        if (gradeRange == null || gradeRange.isEmpty()) {
            return true; // No range specified, assume all grades
        }

        // Parse grade range (e.g., "K-6", "6-12", "K-12")
        String[] parts = gradeRange.split("-");
        if (parts.length != 2) {
            return true; // Can't parse, assume valid
        }

        try {
            int requestedGrade = parseGrade(gradeLevel);
            int minGrade = parseGrade(parts[0].trim());
            int maxGrade = parseGrade(parts[1].trim());

            return requestedGrade >= minGrade && requestedGrade <= maxGrade;
        } catch (Exception e) {
            return true; // Error parsing, assume valid
        }
    }

    /**
     * Parse grade string to integer (K=0, 1-12=1-12)
     */
    private int parseGrade(String grade) {
        if (grade.equalsIgnoreCase("K") || grade.equalsIgnoreCase("Kindergarten")) {
            return 0;
        }
        return Integer.parseInt(grade);
    }
}
