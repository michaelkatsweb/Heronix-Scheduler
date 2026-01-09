package com.heronix.model.enums;

/**
 * Course assignment status indicating completeness of resource allocation
 *
 * Used for visual management in the UI with color-coded indicators to help
 * data entry staff quickly identify courses that need attention.
 *
 * Visual Indicators:
 * - COMPLETE: 🟢 Green (Teacher AND Room assigned)
 * - PARTIAL: 🟡 Yellow (Only Teacher OR Room assigned)
 * - UNASSIGNED: 🔴 Red (Neither Teacher nor Room assigned)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-19
 */
public enum CourseAssignmentStatus {
    /**
     * Course is fully assigned (both teacher and room)
     * Visual indicator: 🟢 Green
     */
    COMPLETE,

    /**
     * Course is partially assigned (either teacher or room missing)
     * Visual indicator: 🟡 Yellow
     */
    PARTIAL,

    /**
     * Course is not assigned (both teacher and room missing)
     * Visual indicator: 🔴 Red
     */
    UNASSIGNED;

    /**
     * Get the Unicode emoji indicator for this status
     *
     * @return Unicode emoji string
     */
    public String getIndicator() {
        return switch (this) {
            case COMPLETE -> "🟢";
            case PARTIAL -> "🟡";
            case UNASSIGNED -> "🔴";
        };
    }

    /**
     * Get the color hex code for this status (for CSS styling)
     *
     * @return Hex color string (e.g., "#4CAF50")
     */
    public String getColorCode() {
        return switch (this) {
            case COMPLETE -> "#4CAF50";    // Material Green
            case PARTIAL -> "#FFC107";     // Material Amber
            case UNASSIGNED -> "#F44336";  // Material Red
        };
    }

    /**
     * Get a human-readable description
     *
     * @return Status description
     */
    public String getDescription() {
        return switch (this) {
            case COMPLETE -> "Fully Assigned";
            case PARTIAL -> "Partially Assigned";
            case UNASSIGNED -> "Not Assigned";
        };
    }

    /**
     * Determine assignment status from teacher and room availability
     *
     * @param hasTeacher Whether course has a teacher assigned
     * @param hasRoom Whether course has a room assigned
     * @return Appropriate CourseAssignmentStatus
     */
    public static CourseAssignmentStatus from(boolean hasTeacher, boolean hasRoom) {
        if (hasTeacher && hasRoom) {
            return COMPLETE;
        } else if (hasTeacher || hasRoom) {
            return PARTIAL;
        } else {
            return UNASSIGNED;
        }
    }
}
