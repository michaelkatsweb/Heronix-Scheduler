package com.heronix.model.enums;

/**
 * Usage Pattern Enum
 * Phase 6E: Multi-Room Courses
 *
 * Defines when and how a room is used in a multi-room course assignment.
 *
 * @since Phase 6E - December 3, 2025
 */
public enum UsagePattern {
    /**
     * Always use this room
     * Room is used every time the course meets
     */
    ALWAYS("Always"),

    /**
     * Alternating days
     * Room is used every other class meeting
     * Example: Week 1 Mon/Wed/Fri, Week 2 Tue/Thu
     */
    ALTERNATING_DAYS("Alternating Days"),

    /**
     * Odd-numbered days only
     * Room is used on odd-numbered calendar days (1, 3, 5, etc.)
     */
    ODD_DAYS("Odd-Numbered Days"),

    /**
     * Even-numbered days only
     * Room is used on even-numbered calendar days (2, 4, 6, etc.)
     */
    EVEN_DAYS("Even-Numbered Days"),

    /**
     * First half of period
     * Room is used for first half of class period
     * Example: 10:00-10:25 in a 10:00-10:50 period
     */
    FIRST_HALF("First Half of Period"),

    /**
     * Second half of period
     * Room is used for second half of class period
     * Example: 10:25-10:50 in a 10:00-10:50 period
     */
    SECOND_HALF("Second Half of Period"),

    /**
     * Specific days only
     * Room is used only on specific days of the week
     * Configuration stored separately (e.g., "Monday,Wednesday,Friday")
     */
    SPECIFIC_DAYS("Specific Days Only"),

    /**
     * Weekly rotation
     * Room is used on a rotating weekly schedule
     * Example: Week 1 only, Week 2 only, etc.
     */
    WEEKLY_ROTATION("Weekly Rotation");

    private final String displayName;

    UsagePattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Check if this pattern is day-based (affects which day room is used)
     * vs time-based (affects what time during period room is used)
     */
    public boolean isDayBased() {
        return this == ALWAYS
            || this == ALTERNATING_DAYS
            || this == ODD_DAYS
            || this == EVEN_DAYS
            || this == SPECIFIC_DAYS
            || this == WEEKLY_ROTATION;
    }

    /**
     * Check if this pattern is time-based (splits period into segments)
     */
    public boolean isTimeBased() {
        return this == FIRST_HALF || this == SECOND_HALF;
    }
}
