package com.heronix.model.domain;

import com.heronix.model.enums.IEPStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * IEP (Individualized Education Program) Entity
 *
 * Stores IEP information for special education students, including
 * eligibility, goals, accommodations, and related services.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Entity
@Table(name = "ieps",
       indexes = {
           @Index(name = "idx_iep_student", columnList = "student_id"),
           @Index(name = "idx_iep_number", columnList = "iep_number"),
           @Index(name = "idx_iep_status", columnList = "status"),
           @Index(name = "idx_iep_end_date", columnList = "end_date")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IEP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The student this IEP belongs to
     * One student can only have one active IEP at a time
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Unique IEP identification number
     */
    @Column(name = "iep_number", unique = true, length = 50)
    private String iepNumber;

    /**
     * IEP effective start date
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * IEP end date (typically one year from start)
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Date when IEP should be reviewed (typically annual)
     */
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    /**
     * IDEA eligibility category
     * Examples: "Specific Learning Disability", "Speech/Language Impairment",
     *          "Other Health Impairment", "Autism", etc.
     */
    @Column(name = "eligibility_category", length = 100)
    private String eligibilityCategory;

    /**
     * Primary disability (more specific)
     */
    @Column(name = "primary_disability", length = 100)
    private String primaryDisability;

    /**
     * Case manager (typically special education teacher or counselor)
     */
    @Column(name = "case_manager", length = 100)
    private String caseManager;

    /**
     * Current IEP status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IEPStatus status = IEPStatus.DRAFT;

    /**
     * IEP goals (summary or full text)
     */
    @Column(name = "goals", columnDefinition = "TEXT")
    private String goals;

    /**
     * Accommodations and modifications
     */
    @Column(name = "accommodations", columnDefinition = "TEXT")
    private String accommodations;

    /**
     * General notes about the IEP
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Related services required by this IEP
     * (Speech therapy, OT, PT, counseling, etc.)
     */
    @OneToMany(mappedBy = "iep", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IEPService> services = new ArrayList<>();

    /**
     * When this IEP record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this IEP record was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Who created this IEP record (for audit trail)
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this IEP is currently active
     */
    public boolean isActive() {
        // ✅ NULL SAFE: Check endDate exists before comparison
        return status == IEPStatus.ACTIVE &&
               endDate != null &&
               LocalDate.now().isBefore(endDate);
    }

    /**
     * Check if this IEP is expired
     */
    public boolean isExpired() {
        // ✅ NULL SAFE: Check endDate exists before comparison
        return endDate != null && LocalDate.now().isAfter(endDate);
    }

    /**
     * Check if this IEP is due for review soon (within 30 days)
     */
    public boolean isDueForReview() {
        if (nextReviewDate == null) {
            return false;
        }
        return LocalDate.now().plusDays(30).isAfter(nextReviewDate);
    }

    /**
     * Get total required service minutes per week across all services
     */
    public int getTotalMinutesPerWeek() {
        if (services == null || services.isEmpty()) {
            return 0;
        }
        return services.stream()
            .filter(s -> s.getMinutesPerWeek() != null)
            .mapToInt(s -> s.getMinutesPerWeek())
            .sum();
    }

    /**
     * Get count of related services
     */
    public int getServiceCount() {
        return services.size();
    }

    /**
     * Add a service to this IEP
     */
    public void addService(IEPService service) {
        services.add(service);
        service.setIep(this);
    }

    /**
     * Remove a service from this IEP
     */
    public void removeService(IEPService service) {
        services.remove(service);
        service.setIep(null);
    }

    /**
     * Get display string for this IEP
     */
    public String getDisplayString() {
        return String.format("IEP %s - %s (%s to %s) - %s",
            iepNumber != null ? iepNumber : "Draft",
            student != null ? student.getFullName() : "Unknown",
            startDate,
            endDate,
            status.getDisplayName());
    }

    @Override
    public String toString() {
        return String.format("IEP{id=%d, number=%s, student=%s, status=%s, services=%d}",
            id,
            iepNumber,
            student != null ? student.getStudentId() : "null",
            status,
            services.size());
    }
}
