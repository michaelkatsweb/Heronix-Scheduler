package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Grading Category Entity
 *
 * Represents a weighted category for assignments within a course.
 * Examples: Tests (40%), Homework (20%), Projects (30%), Participation (10%)
 *
 * Features:
 * - Weighted percentage for grade calculation
 * - Drop lowest option (e.g., drop lowest quiz)
 * - Category types: HOMEWORK, QUIZ, TEST, PROJECT, PARTICIPATION, LAB, FINAL_EXAM
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Entity
@Table(name = "grading_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Course this category belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Category name (e.g., "Tests", "Homework", "Projects")
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Category type for standardization
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category_type")
    private CategoryType categoryType;

    /**
     * Weight percentage (0-100). All categories for a course should sum to 100.
     */
    @Column(name = "weight", nullable = false)
    private Double weight;

    /**
     * Number of lowest scores to drop (0 = don't drop any)
     */
    @Column(name = "drop_lowest")
    @Builder.Default
    private Integer dropLowest = 0;

    /**
     * Display order in gradebook
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Color for UI display (hex code)
     */
    @Column(name = "color", length = 7)
    @Builder.Default
    private String color = "#2196F3";

    /**
     * Whether this category is active
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Category types for standardized grading
     */
    public enum CategoryType {
        HOMEWORK("Homework", "#4CAF50"),
        QUIZ("Quiz", "#FF9800"),
        TEST("Test", "#F44336"),
        PROJECT("Project", "#9C27B0"),
        PARTICIPATION("Participation", "#03A9F4"),
        LAB("Lab", "#00BCD4"),
        FINAL_EXAM("Final Exam", "#E91E63"),
        CLASSWORK("Classwork", "#8BC34A"),
        EXTRA_CREDIT("Extra Credit", "#FFC107"),
        OTHER("Other", "#607D8B");

        private final String displayName;
        private final String defaultColor;

        CategoryType(String displayName, String defaultColor) {
            this.displayName = displayName;
            this.defaultColor = defaultColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDefaultColor() {
            return defaultColor;
        }
    }

    /**
     * Get display name with weight
     */
    public String getDisplayNameWithWeight() {
        return String.format("%s (%.0f%%)", name, weight);
    }

    /**
     * Check if this category is valid (weight > 0)
     */
    public boolean isValid() {
        return weight != null && weight > 0 && weight <= 100;
    }
}
