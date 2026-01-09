package com.heronix.model.enums;

public enum MeetingPriority {
    CRITICAL("Critical - Legal Deadline"), // IEP deadline approaching
    HIGH("High Priority"), // 504 meeting needed
    MEDIUM("Medium Priority"), // Regular parent conference
    LOW("Low Priority"), // Optional club meeting
    FLEXIBLE("Flexible"); // Can be rescheduled easily

    private final String displayName;

    MeetingPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}