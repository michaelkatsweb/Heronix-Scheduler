package com.heronix.model.domain;

import com.heronix.model.enums.PriorityRuleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Priority Rule Entity
 * Administrator-configurable rules for course enrollment priority
 *
 * This entity allows administrators to create custom priority rules that
 * adjust student priority scores based on various criteria. Rules can be
 * enabled/disabled, weighted, and applied to specific student groups.
 *
 * Examples:
 * - "Students with GPA ≥ 3.8 get +100 points"
 * - "Seniors in grade 12 get +200 points"
 * - "Students with IEP get +100 points for inclusive courses"
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 3 - November 20, 2025
 */
@Entity
@Table(name = "priority_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriorityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Rule name (for admin UI display)
     * Example: "Top Academic Performance Bonus"
     */
    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    /**
     * Rule description (explains what this rule does)
     * Example: "Students with GPA 3.8+ receive 100 bonus points for course assignment"
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Rule type/category
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private PriorityRuleType ruleType;

    /**
     * Weight/importance of this rule (1-100)
     * Higher weight = applied first in priority calculations
     * Default: 50 (medium priority)
     */
    @Column(name = "weight")
    private Integer weight = 50;

    /**
     * Is this rule currently active?
     * Inactive rules are not applied to priority calculations
     */
    @Column(name = "active")
    private Boolean active = true;

    // ========================================================================
    // GPA-BASED RULE PARAMETERS
    // ========================================================================

    /**
     * Minimum GPA threshold (for GPA_THRESHOLD rules)
     * Example: 3.8 for "High Academic Performance"
     */
    @Column(name = "min_gpa_threshold")
    private Double minGPAThreshold;

    /**
     * Maximum GPA threshold (for GPA_THRESHOLD rules)
     * Example: 4.5 for weighted GPA upper bound
     * null = no upper limit
     */
    @Column(name = "max_gpa_threshold")
    private Double maxGPAThreshold;

    // ========================================================================
    // BEHAVIOR-BASED RULE PARAMETERS
    // ========================================================================

    /**
     * Minimum behavior score (for BEHAVIOR_BASED rules)
     * Example: 4 for "Good behavior" (4/5 or 5/5)
     */
    @Column(name = "min_behavior_score")
    private Integer minBehaviorScore;

    // ========================================================================
    // GRADE LEVEL / SENIORITY PARAMETERS
    // ========================================================================

    /**
     * Grade levels this rule applies to (comma-separated)
     * Example: "11,12" for juniors and seniors only
     * Example: "9" for freshmen only
     * null = applies to all grade levels
     */
    @Column(name = "grade_levels", length = 50)
    private String gradeLevels;

    // ========================================================================
    // SPECIAL POPULATION PARAMETERS
    // ========================================================================

    /**
     * Apply to students with IEP?
     * true = only students with IEP
     * false = only students without IEP
     * null = all students (ignore IEP status)
     */
    @Column(name = "apply_to_iep")
    private Boolean applyToIEP;

    /**
     * Apply to students with 504 plans?
     */
    @Column(name = "apply_to_504")
    private Boolean applyTo504;

    /**
     * Apply to gifted students?
     */
    @Column(name = "apply_to_gifted")
    private Boolean applyToGifted;

    // ========================================================================
    // BONUS POINTS
    // ========================================================================

    /**
     * Bonus points to add to priority score when rule matches
     * Example: 100 for high academic achievement
     * Example: 50 for good behavior
     */
    @Column(name = "bonus_points")
    private Integer bonusPoints = 0;

    // ========================================================================
    // CUSTOM RULE PARAMETERS
    // ========================================================================

    /**
     * Custom SQL condition (for advanced CUSTOM rules)
     * Example: "credits_earned >= 18 AND honor_roll_status = 'Honor Roll'"
     * WARNING: Use with caution - allows SQL injection if not validated
     */
    @Column(name = "custom_condition", columnDefinition = "TEXT")
    private String customCondition;

    /**
     * Additional metadata in JSON format (for future extensibility)
     * Example: {"min_attendance_percent": 95, "min_credits": 18}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // ========================================================================
    // AUDIT FIELDS
    // ========================================================================

    /**
     * When this rule was created
     */
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    /**
     * Who created this rule
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * When this rule was last modified
     */
    @Column(name = "modified_at")
    private java.time.LocalDateTime modifiedAt;

    /**
     * Who last modified this rule
     */
    @Column(name = "modified_by")
    private String modifiedBy;

    // ========================================================================
    // LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        modifiedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = java.time.LocalDateTime.now();
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Check if this rule applies to a given student
     *
     * @param student Student to check
     * @return true if rule applies to this student
     */
    public boolean appliesTo(Student student) {
        if (!Boolean.TRUE.equals(active)) {
            return false; // Inactive rules don't apply
        }

        // Check grade level filter
        if (gradeLevels != null && !gradeLevels.isEmpty()) {
            String studentGrade = student.getGradeLevel();
            if (studentGrade == null || !gradeLevels.contains(studentGrade)) {
                return false;
            }
        }

        // Check IEP filter
        if (applyToIEP != null) {
            boolean hasIEP = Boolean.TRUE.equals(student.getHasIEP());
            if (applyToIEP != hasIEP) {
                return false;
            }
        }

        // Check 504 filter
        if (applyTo504 != null) {
            boolean has504 = Boolean.TRUE.equals(student.getHas504Plan());
            if (applyTo504 != has504) {
                return false;
            }
        }

        // Check gifted filter
        if (applyToGifted != null) {
            boolean isGifted = Boolean.TRUE.equals(student.getIsGifted());
            if (applyToGifted != isGifted) {
                return false;
            }
        }

        // Check rule-type-specific criteria
        switch (ruleType) {
            case GPA_THRESHOLD:
                return matchesGPAThreshold(student);

            case BEHAVIOR_BASED:
                return matchesBehaviorCriteria(student);

            case SENIORITY:
                return Boolean.TRUE.equals(student.getIsSenior());

            case SPECIAL_NEEDS:
                return Boolean.TRUE.equals(student.getHasIEP()) ||
                       Boolean.TRUE.equals(student.getHas504Plan());

            case GIFTED:
                return Boolean.TRUE.equals(student.getIsGifted());

            case CUSTOM:
                // Custom rules require manual evaluation
                return true;

            default:
                return true; // Unknown rule types apply by default
        }
    }

    /**
     * Check if student meets GPA threshold
     */
    private boolean matchesGPAThreshold(Student student) {
        Double gpa = student.getCurrentGPA();
        if (gpa == null) return false;

        // Check minimum threshold
        if (minGPAThreshold != null && gpa < minGPAThreshold) {
            return false;
        }

        // Check maximum threshold (if set)
        if (maxGPAThreshold != null && gpa > maxGPAThreshold) {
            return false;
        }

        return true;
    }

    /**
     * Check if student meets behavior criteria
     */
    private boolean matchesBehaviorCriteria(Student student) {
        Integer behaviorScore = student.getBehaviorScore();
        if (behaviorScore == null) return false;

        // Check minimum behavior score
        if (minBehaviorScore != null && behaviorScore < minBehaviorScore) {
            return false;
        }

        return true;
    }

    /**
     * Calculate bonus points for this student (if rule applies)
     *
     * @param student Student to calculate bonus for
     * @return Bonus points (0 if rule doesn't apply)
     */
    public int calculateBonus(Student student) {
        if (!appliesTo(student)) {
            return 0;
        }

        return bonusPoints != null ? bonusPoints : 0;
    }

    /**
     * Get human-readable summary of this rule
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(ruleName);

        if (bonusPoints != null && bonusPoints > 0) {
            sb.append(" (+").append(bonusPoints).append(" pts)");
        }

        if (!Boolean.TRUE.equals(active)) {
            sb.append(" [INACTIVE]");
        }

        return sb.toString();
    }

    /**
     * Get detailed explanation of rule criteria
     */
    public String getCriteriaExplanation() {
        StringBuilder sb = new StringBuilder();

        switch (ruleType) {
            case GPA_THRESHOLD:
                sb.append("GPA ");
                if (minGPAThreshold != null && maxGPAThreshold != null) {
                    sb.append(String.format("%.2f-%.2f", minGPAThreshold, maxGPAThreshold));
                } else if (minGPAThreshold != null) {
                    sb.append(String.format("≥ %.2f", minGPAThreshold));
                } else if (maxGPAThreshold != null) {
                    sb.append(String.format("≤ %.2f", maxGPAThreshold));
                }
                break;

            case BEHAVIOR_BASED:
                if (minBehaviorScore != null) {
                    sb.append(String.format("Behavior Score ≥ %d/5", minBehaviorScore));
                }
                break;

            case SENIORITY:
                sb.append("Seniors (Grade 12)");
                break;

            case SPECIAL_NEEDS:
                sb.append("IEP or 504 Plan");
                break;

            case GIFTED:
                sb.append("Gifted/Talented Program");
                break;

            default:
                sb.append(ruleType.getDisplayName());
        }

        if (gradeLevels != null && !gradeLevels.isEmpty()) {
            sb.append(", Grades: ").append(gradeLevels);
        }

        return sb.toString();
    }
}
