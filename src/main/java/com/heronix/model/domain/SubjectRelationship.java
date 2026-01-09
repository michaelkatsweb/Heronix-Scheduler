package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Subject Relationship Entity
 *
 * Tracks relationships between different subject areas for:
 * - Course recommendations
 * - Schedule optimization
 * - Cross-curricular planning
 *
 * Example:
 * Mathematics ↔ Physics (STRONGLY_RELATED)
 * - Physics requires algebra/calculus
 * - Many overlapping concepts
 *
 * Location: src/main/java/com/eduscheduler/model/domain/SubjectRelationship.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - December 6, 2025
 */
@Entity
@Table(name = "subject_relationships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"subject1_id", "subject2_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * First subject in relationship
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject1_id", nullable = false)
    private SubjectArea subject1;

    /**
     * Second subject in relationship
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject2_id", nullable = false)
    private SubjectArea subject2;

    /**
     * Type of relationship
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private RelationshipType relationshipType;

    /**
     * Strength of relationship (1-10 scale)
     * 1-3: Weak relationship
     * 4-6: Moderate relationship
     * 7-10: Strong relationship
     */
    @Column(name = "relationship_strength")
    private Integer relationshipStrength;

    /**
     * Description of why these subjects are related
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Examples of cross-curricular connections
     */
    @Column(name = "connection_examples", columnDefinition = "TEXT")
    private String connectionExamples;

    /**
     * Is this relationship currently active?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * When this relationship was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this relationship was last modified
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Types of relationships between subjects
     */
    public enum RelationshipType {
        /**
         * One subject is prerequisite for the other
         * Example: Algebra → Calculus
         */
        PREREQUISITE("One subject requires the other"),

        /**
         * Subjects enhance each other when taken together
         * Example: Chemistry ↔ Mathematics
         */
        COMPLEMENTARY("Subjects enhance each other"),

        /**
         * Subjects share content or skills
         * Example: English ↔ History (both require analytical writing)
         */
        INTERDISCIPLINARY("Subjects share content/skills"),

        /**
         * Subjects follow a natural order or sequence
         * Example: Biology → Chemistry → Physics
         */
        SEQUENTIAL("Subjects follow a natural order"),

        /**
         * Subjects align to same career pathway
         * Example: Computer Science ↔ Mathematics (STEM careers)
         */
        CAREER_PATHWAY("Subjects align to career path");

        private final String description;

        RelationshipType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if relationship is strong (strength >= 7)
     */
    public boolean isStrongRelationship() {
        return relationshipStrength != null && relationshipStrength >= 7;
    }

    /**
     * Check if relationship is moderate (strength 4-6)
     */
    public boolean isModerateRelationship() {
        return relationshipStrength != null && relationshipStrength >= 4 && relationshipStrength <= 6;
    }

    /**
     * Check if relationship is weak (strength 1-3)
     */
    public boolean isWeakRelationship() {
        return relationshipStrength != null && relationshipStrength >= 1 && relationshipStrength <= 3;
    }

    /**
     * Get the "other" subject in relationship
     *
     * @param subject One of the subjects in the relationship
     * @return The other subject, or null if subject not in relationship
     */
    public SubjectArea getRelatedSubject(SubjectArea subject) {
        if (subject.equals(subject1)) {
            return subject2;
        } else if (subject.equals(subject2)) {
            return subject1;
        }
        return null;
    }

    /**
     * Get strength description (Strong, Moderate, Weak)
     */
    public String getStrengthDescription() {
        if (isStrongRelationship()) {
            return "Strong";
        } else if (isModerateRelationship()) {
            return "Moderate";
        } else if (isWeakRelationship()) {
            return "Weak";
        }
        return "Unknown";
    }

    /**
     * Get display string for UI
     */
    public String getDisplayString() {
        return String.format("%s ↔ %s (%s, %s)",
            subject1 != null ? subject1.getName() : "Unknown",
            subject2 != null ? subject2.getName() : "Unknown",
            relationshipType,
            getStrengthDescription());
    }

    @Override
    public String toString() {
        return String.format("SubjectRelationship{%s ↔ %s (%s, strength=%d)}",
            subject1 != null ? subject1.getCode() : "null",
            subject2 != null ? subject2.getCode() : "null",
            relationshipType,
            relationshipStrength);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectRelationship)) return false;
        SubjectRelationship that = (SubjectRelationship) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
