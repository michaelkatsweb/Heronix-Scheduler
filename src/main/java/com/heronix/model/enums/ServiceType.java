package com.heronix.model.enums;

/**
 * Types of special education and related services
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
public enum ServiceType {
    SPEECH_THERAPY("Speech Therapy", "Speech-language therapy services"),
    OCCUPATIONAL_THERAPY("Occupational Therapy", "OT services for fine motor, sensory, daily living skills"),
    PHYSICAL_THERAPY("Physical Therapy", "PT services for gross motor and mobility"),
    COUNSELING("Counseling", "Individual or group counseling services"),
    SOCIAL_WORK("Social Work", "Social work services and case management"),
    VISION_SERVICES("Vision Services", "Services for students with visual impairments"),
    HEARING_SERVICES("Hearing Services", "Services for students with hearing impairments"),
    ADAPTED_PE("Adapted Physical Education", "Modified physical education"),
    BEHAVIORAL_SUPPORT("Behavioral Support", "Behavioral intervention and support"),
    ASSISTIVE_TECHNOLOGY("Assistive Technology", "AT evaluation and training"),
    TRANSITION_SERVICES("Transition Services", "Post-secondary transition planning"),
    READING_INTERVENTION("Reading Intervention", "Specialized reading instruction"),
    MATH_INTERVENTION("Math Intervention", "Specialized math instruction"),
    RESOURCE_ROOM("Resource Room", "General resource room support"),
    OTHER("Other", "Other related services");

    private final String displayName;
    private final String description;

    ServiceType(String displayName, String description) {
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
     * Check if this service type requires a licensed/certified specialist
     */
    public boolean requiresSpecialist() {
        return this == SPEECH_THERAPY ||
               this == OCCUPATIONAL_THERAPY ||
               this == PHYSICAL_THERAPY ||
               this == VISION_SERVICES ||
               this == HEARING_SERVICES;
    }

    /**
     * Check if this service type is typically delivered as pull-out
     */
    public boolean isTypicallyPullOut() {
        return this == SPEECH_THERAPY ||
               this == OCCUPATIONAL_THERAPY ||
               this == PHYSICAL_THERAPY ||
               this == COUNSELING;
    }
}
