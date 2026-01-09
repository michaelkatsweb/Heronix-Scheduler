package com.heronix.model.enums;

/**
 * Enum representing different types of paraprofessional assignments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.1
 * @since 2025-11-16
 */
public enum ParaAssignmentType {

    /**
     * One-on-one assignment to a specific student
     * Para follows assigned student throughout the day
     */
    ONE_ON_ONE("One-on-One", "Dedicated support for a specific student"),

    /**
     * Small group assignment to multiple students
     * Para supports a small group of 2-5 students
     */
    SMALL_GROUP("Small Group", "Support for a small group of students"),

    /**
     * Classroom support assignment
     * Para assists teacher with entire class
     */
    CLASSROOM_SUPPORT("Classroom Support", "General classroom assistance"),

    /**
     * Floater assignment
     * Para rotates between multiple classrooms as needed
     */
    FLOATER("Floater", "Rotates between multiple classrooms"),

    /**
     * Specialized support (e.g., speech, behavioral, medical)
     * Para provides specific type of support
     */
    SPECIALIZED("Specialized Support", "Provides specialized support services"),

    /**
     * Resource room assignment
     * Para works in a dedicated resource room
     */
    RESOURCE_ROOM("Resource Room", "Works in resource room setting");

    private final String displayName;
    private final String description;

    ParaAssignmentType(String displayName, String description) {
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
