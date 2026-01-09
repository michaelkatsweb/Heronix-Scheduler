package com.heronix.model.enums;

/**
 * Course Type Enum
 * Represents the difficulty/advancement level of a course
 * (separate from EducationLevel which represents grade level)
 *
 * Location: src/main/java/com/eduscheduler/model/enums/CourseType.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
public enum CourseType {
    REGULAR("Regular"),
    HONORS("Honors"),
    AP("Advanced Placement (AP)"),
    IB("International Baccalaureate (IB)"),
    DUAL_ENROLLMENT("Dual Enrollment"),
    REMEDIAL("Remedial/Support"),
    GIFTED("Gifted/Talented"),
    ELECTIVE("Elective");

    private final String displayName;

    CourseType(String displayName) {
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
