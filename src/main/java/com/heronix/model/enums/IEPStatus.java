package com.heronix.model.enums;

/**
 * Status of an IEP (Individualized Education Program)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
public enum IEPStatus {
    /**
     * IEP is currently active and being implemented
     */
    ACTIVE("Active", "IEP is currently in effect"),

    /**
     * IEP has passed its end date and needs renewal
     */
    EXPIRED("Expired", "IEP has passed end date"),

    /**
     * IEP is pending review or annual update
     */
    PENDING_REVIEW("Pending Review", "IEP is due for review"),

    /**
     * IEP is in draft form, not yet finalized
     */
    DRAFT("Draft", "IEP is being developed");

    private final String displayName;
    private final String description;

    IEPStatus(String displayName, String description) {
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
     * Check if this status means the IEP is active and should be followed
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
