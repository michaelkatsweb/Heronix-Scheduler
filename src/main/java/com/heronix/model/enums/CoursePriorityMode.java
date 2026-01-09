package com.heronix.model.enums;

/**
 * Priority modes for course enrollment
 * Determines how students are prioritized for course placement
 */
public enum CoursePriorityMode {
    GPA_BASED("GPA-Based Priority", "Students with higher GPA get priority"),
    FIRST_COME_FIRST_SERVE("First Come First Serve", "Students who register first get priority"),
    LOTTERY("Random Lottery", "Random selection from eligible students"),
    SENIORITY("Seniority Priority", "Seniors get first priority, then juniors, etc."),
    BEHAVIOR_BASED("Behavior-Based", "Students with better behavior scores get priority"),
    HYBRID("Hybrid Priority", "Combination of GPA, seniority, and behavior"),
    MANUAL("Manual Assignment", "Administrator assigns students manually");

    private final String displayName;
    private final String description;

    CoursePriorityMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
