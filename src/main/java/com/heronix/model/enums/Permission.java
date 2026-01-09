package com.heronix.model.enums;

/**
 * Permission Enum
 * Granular permissions for fine-grained access control
 *
 * Location: src/main/java/com/eduscheduler/model/enums/Permission.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
public enum Permission {
    // Schedule Management
    SCHEDULE_VIEW_ALL("View All Schedules"),
    SCHEDULE_EDIT_ALL("Edit All Schedules"),
    SCHEDULE_GENERATE("Generate Schedules"),
    SCHEDULE_DELETE("Delete Schedules"),

    // Student Data
    STUDENT_VIEW_ALL("View All Students"),
    STUDENT_EDIT_ALL("Edit All Students"),
    STUDENT_MEDICAL_VIEW("View Medical Information"),
    STUDENT_IEP_VIEW("View IEP/504 Information"),
    STUDENT_CREATE("Create Students"),
    STUDENT_DELETE("Delete Students"),

    // Teacher Data
    TEACHER_VIEW_ALL("View All Teachers"),
    TEACHER_EDIT_ALL("Edit All Teachers"),
    TEACHER_CREATE("Create Teachers"),
    TEACHER_DELETE("Delete Teachers"),

    // Course Management
    COURSE_VIEW("View Courses"),
    COURSE_EDIT("Edit Courses"),
    COURSE_CREATE("Create Courses"),
    COURSE_DELETE("Delete Courses"),

    // Room Management
    ROOM_VIEW("View Rooms"),
    ROOM_EDIT("Edit Rooms"),
    ROOM_CREATE("Create Rooms"),
    ROOM_DELETE("Delete Rooms"),

    // Import/Export
    IMPORT_DATA("Import Data"),
    EXPORT_DATA("Export Data"),
    EXPORT_PDF("Export to PDF"),

    // System Administration
    USER_MANAGEMENT("Manage Users"),
    SYSTEM_SETTINGS("Modify System Settings"),
    VIEW_AUDIT_LOG("View Audit Log");

    private final String displayName;

    Permission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
