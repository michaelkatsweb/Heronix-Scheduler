package com.heronix.model.domain;

import com.heronix.model.enums.Plan504Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 504 Plan Entity
 *
 * Stores 504 Plan information for students with disabilities who need
 * accommodations but do not require special education services.
 *
 * Section 504 of the Rehabilitation Act requires schools to provide
 * accommodations to ensure equal access to education.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Entity
@Table(name = "plan_504",
       indexes = {
           @Index(name = "idx_504_student", columnList = "student_id"),
           @Index(name = "idx_504_number", columnList = "plan_number"),
           @Index(name = "idx_504_status", columnList = "status"),
           @Index(name = "idx_504_end_date", columnList = "end_date")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan504 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The student this 504 Plan belongs to
     * One student can only have one active 504 Plan at a time
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Unique 504 Plan identification number
     */
    @Column(name = "plan_number", unique = true, length = 50)
    private String planNumber;

    /**
     * Plan effective start date
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Plan end date
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Date when plan should be reviewed
     */
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    /**
     * Disability or condition covered by the plan
     * Examples: "ADHD", "Diabetes", "Anxiety Disorder", etc.
     */
    @Column(name = "disability", length = 200)
    private String disability;

    /**
     * 504 Coordinator (person responsible for overseeing implementation)
     */
    @Column(name = "coordinator", length = 100)
    private String coordinator;

    /**
     * Current plan status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Plan504Status status = Plan504Status.DRAFT;

    /**
     * Accommodations provided
     * Examples:
     * - Extended time on tests
     * - Preferential seating
     * - Permission to leave class for medical needs
     * - Modified assignments
     * - Use of assistive technology
     */
    @Column(name = "accommodations", columnDefinition = "TEXT", nullable = false)
    private String accommodations;

    /**
     * Additional notes about the plan
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * When this plan record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this plan record was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Who created this plan record (for audit trail)
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
     * Check if this 504 Plan is currently active
     */
    public boolean isActive() {
        return status == Plan504Status.ACTIVE &&
               LocalDate.now().isBefore(endDate);
    }

    /**
     * Check if this plan is expired
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    /**
     * Check if this plan is due for review soon (within 30 days)
     */
    public boolean isDueForReview() {
        if (nextReviewDate == null) {
            return false;
        }
        return LocalDate.now().plusDays(30).isAfter(nextReviewDate);
    }

    /**
     * Get display string for this 504 Plan
     */
    public String getDisplayString() {
        return String.format("504 Plan %s - %s (%s to %s) - %s",
            planNumber != null ? planNumber : "Draft",
            student != null ? student.getFullName() : "Unknown",
            startDate,
            endDate,
            status.getDisplayName());
    }

    @Override
    public String toString() {
        return String.format("Plan504{id=%d, number=%s, student=%s, status=%s}",
            id,
            planNumber,
            student != null ? student.getStudentId() : "null",
            status);
    }
}
