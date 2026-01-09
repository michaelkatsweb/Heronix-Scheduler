package com.heronix.model.enums;

/**
 * Status of a 504 Plan
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
public enum Plan504Status {
    /**
     * 504 Plan is currently active and being implemented
     */
    ACTIVE("Active", "504 Plan is currently in effect"),

    /**
     * 504 Plan has passed its end date and needs renewal
     */
    EXPIRED("Expired", "504 Plan has passed end date"),

    /**
     * 504 Plan is pending review or annual update
     */
    PENDING_REVIEW("Pending Review", "504 Plan is due for review"),

    /**
     * 504 Plan is in draft form, not yet finalized
     */
    DRAFT("Draft", "504 Plan is being developed");

    private final String displayName;
    private final String description;

    Plan504Status(String displayName, String description) {
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
     * Check if this status means the plan is active and should be followed
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Check if this status requires attention/action
     */
    public boolean requiresAction() {
        return this == EXPIRED || this == PENDING_REVIEW;
    }
}
