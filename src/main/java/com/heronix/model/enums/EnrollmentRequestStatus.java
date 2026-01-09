package com.heronix.model.enums;

/**
 * Status of a course enrollment request
 * Tracks the lifecycle of a student's request to enroll in a course
 */
public enum EnrollmentRequestStatus {
    PENDING("Pending Review", "Request submitted, awaiting processing"),
    APPROVED("Approved - Enrolled", "Request approved, student enrolled in course"),
    WAITLISTED("Waitlisted", "Course full, student added to waitlist"),
    DENIED("Denied", "Request denied (does not meet requirements or course cancelled)"),
    ALTERNATE_ASSIGNED("Alternate Course Assigned", "1st choice unavailable, assigned to alternate"),
    CANCELLED("Cancelled by Student", "Student withdrew their request"),
    EXPIRED("Expired", "Request expired (registration period ended)");

    private final String displayName;
    private final String description;

    EnrollmentRequestStatus(String displayName, String description) {
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
     * Check if this status is a final/terminal state
     */
    public boolean isFinalStatus() {
        return this == APPROVED || this == DENIED || this == CANCELLED || this == EXPIRED;
    }

    /**
     * Check if this status allows the student to be enrolled
     */
    public boolean isEnrolled() {
        return this == APPROVED || this == ALTERNATE_ASSIGNED;
    }

    /**
     * Check if student is still waiting for a decision
     */
    public boolean isWaiting() {
        return this == PENDING || this == WAITLISTED;
    }
}
