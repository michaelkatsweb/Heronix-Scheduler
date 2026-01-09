package com.heronix.model.enums;

/**
 * Status of a scheduled pull-out session
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
public enum PullOutStatus {
    /**
     * Schedule is active and sessions are occurring
     */
    ACTIVE("Active", "Session is scheduled and recurring"),

    /**
     * Schedule has been cancelled (session no longer occurs)
     */
    CANCELLED("Cancelled", "Session has been cancelled"),

    /**
     * Schedule has completed its duration and is no longer active
     */
    COMPLETED("Completed", "Session series has ended"),

    /**
     * Schedule has been moved to a different time/day
     */
    RESCHEDULED("Rescheduled", "Session has been moved to new time");

    private final String displayName;
    private final String description;

    PullOutStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this status means the session is currently active
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Check if this status means the session should count toward minutes
     */
    public boolean countsTowardMinutes() {
        return this == ACTIVE || this == RESCHEDULED;
    }

    /**
     * Check if this is a final status (no further changes expected)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
