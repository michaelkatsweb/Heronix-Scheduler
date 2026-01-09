package com.heronix.model.enums;

/**
 * Enum representing different roles a teacher can have
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.1
 * @since 2025-11-16
 */
public enum TeacherRole {

    /**
     * Lead teacher - Primary instructor for a course
     */
    LEAD_TEACHER("Lead Teacher", "Primary course instructor"),

    /**
     * Co-teacher - Supports students with IEPs across multiple classes
     * Follows assigned students from class to class throughout the day
     */
    CO_TEACHER("Co-Teacher", "IEP support teacher following assigned students"),

    /**
     * Specialist - Counselors, reading specialists, etc.
     */
    SPECIALIST("Specialist", "Specialized support role");

    private final String displayName;
    private final String description;

    TeacherRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
