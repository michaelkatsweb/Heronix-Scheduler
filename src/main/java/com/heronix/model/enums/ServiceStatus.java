package com.heronix.model.enums;

/**
 * Status of an IEP service
 * Indicates whether the service has been scheduled and is being delivered
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
public enum ServiceStatus {
    /**
     * Service has been scheduled and sessions are recurring
     */
    SCHEDULED("Scheduled", "Service is scheduled and being delivered"),

    /**
     * Service has not yet been scheduled
     */
    NOT_SCHEDULED("Not Scheduled", "Service needs to be scheduled"),

    /**
     * Service is temporarily on hold
     */
    ON_HOLD("On Hold", "Service is temporarily suspended"),

    /**
     * Service has been discontinued per IEP team decision
     */
    DISCONTINUED("Discontinued", "Service is no longer provided");

    private final String displayName;
    private final String description;

    ServiceStatus(String displayName, String description) {
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
     * Check if this status means service is actively being delivered
     */
    public boolean isActive() {
        return this == SCHEDULED;
    }

    /**
     * Check if this status requires scheduling action
     */
    public boolean requiresScheduling() {
        return this == NOT_SCHEDULED;
    }

    /**
     * Check if this status means no minutes should be tracked
     */
    public boolean shouldTrackMinutes() {
        return this == SCHEDULED || this == ON_HOLD;
    }
}
