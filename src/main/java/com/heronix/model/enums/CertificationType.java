package com.heronix.model.enums;

/**
 * Teacher Certification Type Enum
 * Defines the different types of teacher certifications in Florida
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-06
 */
public enum CertificationType {

    /**
     * Professional Certification - Highest level, renewable every 5 years
     * Requirements: Master's degree + 3 years teaching experience
     */
    PROFESSIONAL("Professional Certificate",
                 "Highest level certification, renewable every 5 years"),

    /**
     * Temporary Certification - Valid for 3 years, non-renewable
     * Requirements: Bachelor's degree + passing subject area exam
     */
    TEMPORARY("Temporary Certificate",
              "Valid for 3 years, non-renewable"),

    /**
     * Certified - Standard certification, renewable
     * Requirements: Bachelor's degree + passing certification exams
     */
    CERTIFIED("Certified",
              "Standard certification, renewable"),

    /**
     * Provisional - Beginning teacher, valid while completing requirements
     * Requirements: Bachelor's degree, working toward full certification
     */
    PROVISIONAL("Provisional Certificate",
                "Valid while completing full certification requirements"),

    /**
     * Substitute - Allows substitute teaching only
     * Requirements: Bachelor's degree or passing substitute exam
     */
    SUBSTITUTE("Substitute Certificate",
               "Allows substitute teaching only"),

    /**
     * Adjunct - Part-time/adjunct teaching at college level
     * Requirements: Master's degree in subject area
     */
    ADJUNCT("Adjunct Certificate",
            "For part-time college/university teaching"),

    /**
     * Emergency - Temporary emergency certification during teacher shortage
     * Requirements: Bachelor's degree, must work toward full certification
     */
    EMERGENCY("Emergency Certificate",
              "Temporary emergency certification");

    private final String displayName;
    private final String description;

    CertificationType(String displayName, String description) {
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
     * Check if this certification type allows full-time teaching
     */
    public boolean allowsFullTimeTeaching() {
        return this == PROFESSIONAL ||
               this == CERTIFIED ||
               this == PROVISIONAL ||
               this == TEMPORARY;
    }

    /**
     * Check if this certification type requires renewal
     */
    public boolean requiresRenewal() {
        return this == PROFESSIONAL || this == CERTIFIED;
    }

    /**
     * Get renewal period in years (0 if not renewable)
     */
    public int getRenewalYears() {
        switch (this) {
            case PROFESSIONAL:
            case CERTIFIED:
                return 5;
            case TEMPORARY:
                return 3; // But non-renewable
            case PROVISIONAL:
            case EMERGENCY:
                return 1;
            default:
                return 0;
        }
    }
}
