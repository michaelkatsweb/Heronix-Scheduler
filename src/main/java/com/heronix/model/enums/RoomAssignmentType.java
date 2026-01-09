package com.heronix.model.enums;

/**
 * Room Assignment Type Enum
 * Phase 6E: Multi-Room Courses
 *
 * Defines the type of room assignment for multi-room courses.
 *
 * @since Phase 6E - December 3, 2025
 */
public enum RoomAssignmentType {
    /**
     * Primary room - main instructional space
     * Used for majority of instruction time
     */
    PRIMARY("Primary Room"),

    /**
     * Secondary room - supplementary instructional space
     * Used for lab work, breakout sessions, or alternating instruction
     */
    SECONDARY("Secondary Room"),

    /**
     * Overflow room - additional capacity space
     * Used when class size exceeds primary room capacity
     * Typically receives video/audio feed from primary room
     */
    OVERFLOW("Overflow Room"),

    /**
     * Breakout room - small group instruction space
     * Used for differentiated instruction, small groups, or special education support
     */
    BREAKOUT("Breakout Room"),

    /**
     * Rotating room - alternating instructional space
     * Students/teachers rotate between rooms on schedule
     * Common for lab/lecture splits
     */
    ROTATING("Rotating Room");

    private final String displayName;

    RoomAssignmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
