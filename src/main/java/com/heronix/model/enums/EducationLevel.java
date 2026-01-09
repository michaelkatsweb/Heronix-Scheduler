
package com.heronix.model.enums;

public enum EducationLevel {
    PRE_K("Pre-Kindergarten"),
    KINDERGARTEN("Kindergarten"),
    ELEMENTARY("Elementary School"),
    MIDDLE_SCHOOL("Middle School"),
    HIGH_SCHOOL("High School"),
    COLLEGE("College"),
    UNIVERSITY("University"),
    GRADUATE("Graduate School");

    private final String displayName;

    EducationLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}