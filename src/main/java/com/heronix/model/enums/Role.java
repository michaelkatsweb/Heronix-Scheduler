package com.heronix.model.enums;

/**
 * User Role Enum - Enhanced for UI/UX Reorganization
 * Defines hierarchical roles following organizational workflow.
 *
 * Workflow Order:
 * 1. IT_ADMIN/SUPER_ADMIN - System setup and technical configuration
 * 2. DISTRICT_ADMIN - District policies and settings
 * 3. DATA_ENTRY - Bulk data import and management
 * 4. REGISTRAR - Student registration and enrollment
 * 5. COUNSELOR - Academic planning and course recommendations
 * 6. SCHEDULER - Master schedule generation
 * 7. TEACHER - Gradebook and class management
 * 8. STUDENT - View-only access to personal information
 *
 * Location: src/main/java/com/eduscheduler/model/enums/Role.java
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-12
 */
public enum Role {
    /**
     * IT Administrator / Super Admin - Full system access
     * Permissions: All features, database management, user management, system configuration
     * Dashboard: System health, database status, user activity, error logs
     */
    SUPER_ADMIN("IT Administrator", "Full system access and technical configuration", 1, "#6c757d", "⚙️"),

    /**
     * District Administrator - Policy and district-wide settings
     * Permissions: District settings, school configuration, reporting, user oversight
     * Dashboard: District overview, school statistics, compliance reports
     * Legacy: Maps to old ADMIN role
     */
    ADMIN("District Administrator", "District policies and school configuration", 2, "#28a745", "🏛️"),

    /**
     * Data Entry Specialist - Bulk data operations
     * Permissions: Import/export data, bulk operations, data validation
     * Dashboard: Import status, data quality metrics, pending validations
     */
    DATA_ENTRY("Data Entry Specialist", "Bulk data import and management", 3, "#28a745", "📊"),

    /**
     * Registrar - Student registration and enrollment
     * Permissions: Student management, enrollment, demographic data
     * Dashboard: Enrollment statistics, pending registrations, student counts
     */
    REGISTRAR("Registrar", "Student registration and enrollment management", 4, "#007bff", "👨‍🎓"),

    /**
     * Counselor - Academic planning and guidance
     * Permissions: Course recommendations, academic planning, graduation tracking, at-risk alerts
     * Dashboard: At-risk students, graduation requirements, course recommendations
     */
    COUNSELOR("Counselor", "Academic planning and student guidance", 5, "#6f42c1", "🎯"),

    /**
     * Scheduler - Master schedule creation
     * Permissions: Schedule generation, course assignments, room assignments, conflict resolution
     * Dashboard: Schedule conflicts, teacher/room utilization, unscheduled courses
     */
    SCHEDULER("Scheduler", "Master schedule generation and management", 6, "#17a2b8", "📅"),

    /**
     * Teacher - Classroom and gradebook management
     * Permissions: Gradebook, attendance, view own schedule, student roster
     * Dashboard: Class rosters, grade entry, upcoming assignments, schedule
     */
    TEACHER("Teacher", "Gradebook and classroom management", 7, "#dc3545", "👨‍🏫"),

    /**
     * Student - View-only personal access
     * Permissions: View own schedule, grades, assignments, profile
     * Dashboard: Personal schedule, current grades, upcoming assignments
     */
    STUDENT("Student", "View personal schedule and grades", 8, "#343a40", "📚"),

    /**
     * Principal - View all data with limited editing
     * Legacy role - retained for backward compatibility
     */
    PRINCIPAL("Principal", "View all data, limited editing capabilities", 2, "#28a745", "🏛️"),

    /**
     * Staff - Read-only access
     * Legacy role - retained for backward compatibility
     */
    STAFF("Staff", "Read-only access to schedules and basic data", 8, "#6c757d", "👤"),

    /**
     * Parent - View own student data
     * Future feature - retained for forward compatibility
     */
    PARENT("Parent", "View own student data only (future feature)", 9, "#ffc107", "👨‍👩‍👧");

    private final String displayName;
    private final String description;
    private final int workflowOrder;
    private final String primaryColor;
    private final String icon;

    Role(String displayName, String description, int workflowOrder, String primaryColor, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.workflowOrder = workflowOrder;
        this.primaryColor = primaryColor;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getWorkflowOrder() {
        return workflowOrder;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getIcon() {
        return icon;
    }

    // ========================================================================
    // ADMINISTRATIVE CHECKS
    // ========================================================================

    /**
     * Check if this role has administrative privileges.
     * @return true for SUPER_ADMIN and ADMIN
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if this role can view all data.
     */
    public boolean canViewAllData() {
        return this == SUPER_ADMIN || this == ADMIN || this == PRINCIPAL || this == COUNSELOR || this == SCHEDULER;
    }

    /**
     * Check if this role can edit data.
     */
    public boolean canEditData() {
        return this == SUPER_ADMIN || this == ADMIN || this == COUNSELOR || this == REGISTRAR || this == SCHEDULER || this == DATA_ENTRY;
    }

    // ========================================================================
    // FEATURE-SPECIFIC PERMISSION CHECKS
    // ========================================================================

    /**
     * Check if this role can manage student data.
     */
    public boolean canManageStudents() {
        return this == SUPER_ADMIN || this == ADMIN || this == REGISTRAR || this == COUNSELOR;
    }

    /**
     * Check if this role can manage teacher data.
     */
    public boolean canManageTeachers() {
        return this == SUPER_ADMIN || this == ADMIN || this == DATA_ENTRY;
    }

    /**
     * Check if this role can manage courses.
     */
    public boolean canManageCourses() {
        return this == SUPER_ADMIN || this == ADMIN || this == DATA_ENTRY || this == SCHEDULER;
    }

    /**
     * Check if this role can generate schedules.
     */
    public boolean canGenerateSchedules() {
        return this == SUPER_ADMIN || this == ADMIN || this == SCHEDULER;
    }

    /**
     * Check if this role can enter grades.
     */
    public boolean canEnterGrades() {
        return this == SUPER_ADMIN || this == TEACHER;
    }

    /**
     * Check if this role can view all reports.
     */
    public boolean canViewAllReports() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if this role can import/export data.
     */
    public boolean canImportExportData() {
        return this == SUPER_ADMIN || this == ADMIN || this == DATA_ENTRY;
    }

    /**
     * Check if this role can manage user accounts.
     */
    public boolean canManageUsers() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if this role has read-only access.
     */
    public boolean isReadOnly() {
        return this == STUDENT || this == STAFF || this == PARENT;
    }

    /**
     * Check if this role can access database management features.
     */
    public boolean canManageDatabase() {
        return this == SUPER_ADMIN;
    }

    /**
     * Check if this role can configure district settings.
     */
    public boolean canConfigureDistrict() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if this role can view at-risk student alerts.
     */
    public boolean canViewAtRiskAlerts() {
        return this == SUPER_ADMIN || this == ADMIN || this == COUNSELOR || this == PRINCIPAL;
    }

    /**
     * Check if this role can manage IEP/504 data.
     */
    public boolean canManageIEP() {
        return this == SUPER_ADMIN || this == ADMIN || this == COUNSELOR;
    }

    /**
     * Check if this role can resolve schedule conflicts.
     */
    public boolean canResolveConflicts() {
        return this == SUPER_ADMIN || this == ADMIN || this == SCHEDULER;
    }
}
