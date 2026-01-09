package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Academic Plan Entity
 *
 * Represents a four-year academic plan for a student, including:
 * - Course selections by year
 * - Progress tracking
 * - Graduation requirement fulfillment
 * - Alternative scenarios ("what-if" planning)
 *
 * Location: src/main/java/com/eduscheduler/model/domain/AcademicPlan.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - December 6, 2025 - Four-Year Academic Planning
 */
@Entity
@Table(name = "academic_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student this plan belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Plan name (e.g., "STEM Track", "Liberal Arts", "Alternative Plan A")
     */
    @Column(nullable = false, length = 200)
    private String planName;

    /**
     * Plan type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    @Builder.Default
    private PlanType planType = PlanType.STANDARD;

    /**
     * Starting school year (e.g., "2025-2026")
     */
    @Column(name = "start_year", length = 20)
    private String startYear;

    /**
     * Expected graduation year (e.g., "2029")
     */
    @Column(name = "expected_graduation_year", length = 10)
    private String expectedGraduationYear;

    /**
     * Plan status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT;

    /**
     * Is this the active/primary plan?
     */
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Planned courses for each year
     */
    @OneToMany(mappedBy = "academicPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("schoolYear ASC, semester ASC")
    @Builder.Default
    private List<PlannedCourse> plannedCourses = new ArrayList<>();

    /**
     * Total credits planned
     */
    @Column(name = "total_credits_planned")
    private Double totalCreditsPlanned;

    /**
     * Total credits completed
     */
    @Column(name = "total_credits_completed")
    private Double totalCreditsCompleted;

    /**
     * Projected GPA at graduation
     */
    @Column(name = "projected_gpa")
    private Double projectedGPA;

    /**
     * Meets graduation requirements
     */
    @Column(name = "meets_graduation_requirements")
    @Builder.Default
    private Boolean meetsGraduationRequirements = false;

    /**
     * Missing requirements (if any)
     */
    @Column(name = "missing_requirements", columnDefinition = "TEXT")
    private String missingRequirements;

    /**
     * Notes about this plan
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Counselor who approved this plan
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_counselor_id")
    private User approvedByCounselor;

    /**
     * When counselor approved
     */
    @Column(name = "counselor_approved_date")
    private LocalDateTime counselorApprovedDate;

    /**
     * Student accepted this plan
     */
    @Column(name = "student_accepted")
    private Boolean studentAccepted;

    /**
     * When student accepted
     */
    @Column(name = "student_accepted_date")
    private LocalDateTime studentAcceptedDate;

    /**
     * Parent accepted this plan
     */
    @Column(name = "parent_accepted")
    private Boolean parentAccepted;

    /**
     * When parent accepted
     */
    @Column(name = "parent_accepted_date")
    private LocalDateTime parentAcceptedDate;

    /**
     * Plan created timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Plan last updated
     */
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Active plan
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum PlanType {
        STANDARD,        // Standard 4-year plan
        ACCELERATED,     // Advanced/early graduation
        EXTENDED,        // 5-year plan
        ALTERNATIVE,     // Alternative/what-if scenario
        TRANSFER,        // Transfer student plan
        DUAL_ENROLLMENT  // Concurrent high school/college
    }

    public enum PlanStatus {
        DRAFT,           // Being worked on
        PENDING_APPROVAL, // Awaiting counselor/parent approval
        APPROVED,        // Fully approved
        ACTIVE,          // Currently being executed
        COMPLETED,       // Student graduated
        ARCHIVED         // Old/inactive plan
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if plan is draft
     */
    public boolean isDraft() {
        return PlanStatus.DRAFT.equals(status);
    }

    /**
     * Check if plan is approved
     */
    public boolean isApproved() {
        return PlanStatus.APPROVED.equals(status) || PlanStatus.ACTIVE.equals(status);
    }

    /**
     * Check if plan is active
     */
    public boolean isActive() {
        return PlanStatus.ACTIVE.equals(status);
    }

    /**
     * Check if fully approved by all parties
     */
    public boolean isFullyApproved() {
        return Boolean.TRUE.equals(studentAccepted) &&
               Boolean.TRUE.equals(parentAccepted) &&
               approvedByCounselor != null;
    }

    /**
     * Get completion percentage
     */
    public double getCompletionPercentage() {
        if (totalCreditsPlanned == null || totalCreditsPlanned == 0) {
            return 0.0;
        }
        if (totalCreditsCompleted == null) {
            return 0.0;
        }
        return (totalCreditsCompleted / totalCreditsPlanned) * 100.0;
    }

    /**
     * Get remaining credits
     */
    public double getRemainingCredits() {
        if (totalCreditsPlanned == null) {
            return 0.0;
        }
        if (totalCreditsCompleted == null) {
            return totalCreditsPlanned;
        }
        return Math.max(0.0, totalCreditsPlanned - totalCreditsCompleted);
    }

    /**
     * Get courses for specific school year
     */
    public List<PlannedCourse> getCoursesForYear(String schoolYear) {
        return plannedCourses.stream()
            .filter(pc -> schoolYear.equals(pc.getSchoolYear()))
            .toList();
    }

    /**
     * Get courses for specific grade level
     */
    public List<PlannedCourse> getCoursesForGrade(Integer gradeLevel) {
        return plannedCourses.stream()
            .filter(pc -> gradeLevel.equals(pc.getGradeLevel()))
            .toList();
    }

    /**
     * Get courses by semester
     */
    public List<PlannedCourse> getCoursesForSemester(String schoolYear, Integer semester) {
        return plannedCourses.stream()
            .filter(pc -> schoolYear.equals(pc.getSchoolYear()) &&
                         semester.equals(pc.getSemester()))
            .toList();
    }

    /**
     * Count total courses
     */
    public int getTotalCoursesPlanned() {
        return plannedCourses.size();
    }

    /**
     * Count completed courses
     */
    public int getTotalCoursesCompleted() {
        return (int) plannedCourses.stream()
            .filter(PlannedCourse::isCompleted)
            .count();
    }

    /**
     * Approve by counselor
     */
    public void approveByCounselor(User counselor) {
        this.approvedByCounselor = counselor;
        this.counselorApprovedDate = LocalDateTime.now();
        updateStatus();
    }

    /**
     * Accept by student
     */
    public void acceptByStudent() {
        this.studentAccepted = true;
        this.studentAcceptedDate = LocalDateTime.now();
        updateStatus();
    }

    /**
     * Accept by parent
     */
    public void acceptByParent() {
        this.parentAccepted = true;
        this.parentAcceptedDate = LocalDateTime.now();
        updateStatus();
    }

    /**
     * Update status based on approvals
     */
    private void updateStatus() {
        if (isFullyApproved() && !PlanStatus.COMPLETED.equals(status)) {
            if (isPrimary) {
                this.status = PlanStatus.ACTIVE;
            } else {
                this.status = PlanStatus.APPROVED;
            }
        } else if (approvedByCounselor != null && PlanStatus.DRAFT.equals(status)) {
            this.status = PlanStatus.PENDING_APPROVAL;
        }
    }

    /**
     * Add course to plan
     */
    public void addPlannedCourse(PlannedCourse plannedCourse) {
        plannedCourse.setAcademicPlan(this);
        this.plannedCourses.add(plannedCourse);
    }

    /**
     * Remove course from plan
     */
    public void removePlannedCourse(PlannedCourse plannedCourse) {
        this.plannedCourses.remove(plannedCourse);
        plannedCourse.setAcademicPlan(null);
    }

    /**
     * Get summary string
     */
    public String getSummary() {
        return String.format("%s - %s (%s, %.1f%% complete, %d/%d courses)",
            planName,
            student != null ? student.getFullName() : "Unknown",
            status,
            getCompletionPercentage(),
            getTotalCoursesCompleted(),
            getTotalCoursesPlanned());
    }

    @Override
    public String toString() {
        return String.format("AcademicPlan[id=%d, student=%s, plan=%s, type=%s, status=%s]",
            id,
            student != null ? student.getFullName() : "null",
            planName,
            planType,
            status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcademicPlan)) return false;
        AcademicPlan that = (AcademicPlan) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
