package com.heronix.model.enums;

/**
 * Delivery model for IEP services
 * Indicates whether service is individual, group, or consultative
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
public enum DeliveryModel {
    INDIVIDUAL("Individual (1:1)", "One-on-one service delivery", 1, 1),
    SMALL_GROUP("Small Group (2-5)", "Small group service delivery", 2, 5),
    LARGE_GROUP("Large Group (6+)", "Large group service delivery", 6, 15),
    CONSULTATION("Consultation", "Indirect service via consultation with teacher", 0, 0),
    PUSH_IN("Push-In", "Service delivered in general education classroom", 1, 30),
    CO_TEACHING("Co-Teaching", "Co-teaching with general education teacher", 1, 30);

    private final String displayName;
    private final String description;
    private final int minStudents;
    private final int maxStudents;

    DeliveryModel(String displayName, String description, int minStudents, int maxStudents) {
        this.displayName = displayName;
        this.description = description;
        this.minStudents = minStudents;
        this.maxStudents = maxStudents;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getMinStudents() {
        return minStudents;
    }

    public int getMaxStudents() {
        return maxStudents;
    }

    /**
     * Check if this delivery model requires pulling student from general ed class
     */
    public boolean isPullOut() {
        return this == INDIVIDUAL || this == SMALL_GROUP || this == LARGE_GROUP;
    }

    /**
     * Check if this delivery model requires direct service minutes
     */
    public boolean requiresDirectMinutes() {
        return this != CONSULTATION;
    }

    /**
     * Check if this delivery model allows multiple students
     */
    public boolean allowsMultipleStudents() {
        return maxStudents > 1;
    }
}
