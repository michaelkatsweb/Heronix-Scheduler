// FILE: PriorityLevel.java
// LOCATION: src/main/java/com/eduscheduler/model/enums/PriorityLevel.java
package com.heronix.model.enums;

/**
 * Eisenhower Matrix Priority Levels for course scheduling
 * Maps to Q1 (Urgent & Important), Q2 (Important), Q3 (Urgent), Q4 (Neither)
 */
public enum PriorityLevel {
    // Eisenhower Matrix Quadrants
    Q1_URGENT_IMPORTANT("Q1: Urgent & Important"),
    Q2_IMPORTANT_NOT_URGENT("Q2: Important, Not Urgent"),
    Q3_URGENT_NOT_IMPORTANT("Q3: Urgent, Not Important"),
    Q4_NEITHER("Q4: Neither Urgent nor Important"),

    // Traditional Priority Levels
    CRITICAL("Critical"),
    HIGH("High"),
    NORMAL("Normal"),
    LOW("Low");

    private final String displayName;

    PriorityLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Check if this is an Eisenhower Matrix priority
    public boolean isEisenhowerMatrix() {
        return this == Q1_URGENT_IMPORTANT || this == Q2_IMPORTANT_NOT_URGENT ||
                this == Q3_URGENT_NOT_IMPORTANT || this == Q4_NEITHER;
    }

    // Check if high priority
    public boolean isHighPriority() {
        return this == Q1_URGENT_IMPORTANT || this == CRITICAL || this == HIGH;
    }

    /**
     * Get numeric priority value (higher = more important)
     * Q1 and CRITICAL have highest priority (100)
     * Q4 and LOW have lowest priority (10)
     *
     * @return Priority value from 10 to 100
     */
    public int getPriority() {
        switch (this) {
            case Q1_URGENT_IMPORTANT:
            case CRITICAL:
                return 100;
            case Q2_IMPORTANT_NOT_URGENT:
            case HIGH:
                return 75;
            case Q3_URGENT_NOT_IMPORTANT:
            case NORMAL:
                return 50;
            case Q4_NEITHER:
            case LOW:
                return 10;
            default:
                return 50; // Default to NORMAL priority
        }
    }
}