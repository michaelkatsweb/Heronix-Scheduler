package com.heronix.service;

import com.heronix.model.domain.User;
import com.heronix.model.enums.Role;
import org.springframework.stereotype.Service;

/**
 * Permission Service - Centralized role-based access control
 *
 * This service provides a single point of truth for permission checks
 * across the application. All UI controllers should use this service
 * to determine what features/sections to display based on user role.
 *
 * Location: src/main/java/com/eduscheduler/service/PermissionService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-12
 */
@Service
public class PermissionService {

    // ========================================================================
    // SECTION VISIBILITY - Determines which main sections user can access
    // ========================================================================

    /**
     * Check if user can access Setup section (IT Admin only).
     * Setup includes: System Settings, Database Management, User Management
     */
    public boolean canAccessSetupSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole() == Role.SUPER_ADMIN;
    }

    /**
     * Check if user can access Data Management section.
     * Data includes: Import/Export, Bulk Operations, Data Validation
     */
    public boolean canAccessDataSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        Role role = user.getPrimaryRole();
        return role == Role.SUPER_ADMIN || role == Role.ADMIN || role == Role.DATA_ENTRY;
    }

    /**
     * Check if user can access Students section.
     */
    public boolean canAccessStudentsSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageStudents() ||
               user.getPrimaryRole() == Role.COUNSELOR ||
               user.getPrimaryRole() == Role.TEACHER;
    }

    /**
     * Check if user can access Teachers section.
     */
    public boolean canAccessTeachersSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageTeachers() ||
               user.getPrimaryRole() == Role.SCHEDULER;
    }

    /**
     * Check if user can access Courses section.
     */
    public boolean canAccessCoursesSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageCourses();
    }

    /**
     * Check if user can access Scheduling section.
     */
    public boolean canAccessSchedulingSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canGenerateSchedules() ||
               user.getPrimaryRole() == Role.COUNSELOR;
    }

    /**
     * Check if user can access Gradebook section.
     */
    public boolean canAccessGradebookSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canEnterGrades() ||
               user.getPrimaryRole() == Role.COUNSELOR;
    }

    /**
     * Check if user can access SPED section.
     */
    public boolean canAccessSPEDSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageIEP();
    }

    /**
     * Check if user can access Reports section.
     */
    public boolean canAccessReportsSection(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        // All roles except STUDENT can access reports
        return user.getPrimaryRole() != Role.STUDENT;
    }

    // ========================================================================
    // FEATURE-LEVEL PERMISSIONS
    // ========================================================================

    /**
     * Check if user can add new students.
     */
    public boolean canAddStudents(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageStudents();
    }

    /**
     * Check if user can edit student demographic data.
     */
    public boolean canEditStudentDemographics(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        Role role = user.getPrimaryRole();
        return role == Role.SUPER_ADMIN || role == Role.ADMIN || role == Role.REGISTRAR;
    }

    /**
     * Check if user can enroll students in courses.
     */
    public boolean canEnrollStudents(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        Role role = user.getPrimaryRole();
        return role == Role.SUPER_ADMIN || role == Role.ADMIN ||
               role == Role.REGISTRAR || role == Role.COUNSELOR;
    }

    /**
     * Check if user can delete students.
     */
    public boolean canDeleteStudents(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        Role role = user.getPrimaryRole();
        return role == Role.SUPER_ADMIN || role == Role.ADMIN;
    }

    /**
     * Check if user can view student medical records.
     */
    public boolean canViewMedicalRecords(User user) {
        if (user == null) {
            return false;
        }
        return user.canAccessMedicalData();
    }

    /**
     * Check if user can view/edit IEP data.
     */
    public boolean canManageIEP(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageIEP();
    }

    /**
     * Check if user can generate master schedules.
     */
    public boolean canGenerateMasterSchedule(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canGenerateSchedules();
    }

    /**
     * Check if user can manually edit schedules.
     */
    public boolean canEditSchedules(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        Role role = user.getPrimaryRole();
        return role == Role.SUPER_ADMIN || role == Role.ADMIN || role == Role.SCHEDULER;
    }

    /**
     * Check if user can resolve schedule conflicts.
     */
    public boolean canResolveConflicts(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canResolveConflicts();
    }

    /**
     * Check if user can enter grades for ANY student.
     */
    public boolean canEnterAllGrades(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole() == Role.SUPER_ADMIN;
    }

    /**
     * Check if user can enter grades for their own students.
     */
    public boolean canEnterOwnGrades(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canEnterGrades();
    }

    /**
     * Check if user can manage district settings.
     */
    public boolean canManageDistrictSettings(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canConfigureDistrict();
    }

    /**
     * Check if user can import/export data.
     */
    public boolean canImportExportData(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canImportExportData();
    }

    /**
     * Check if user can manage other user accounts.
     */
    public boolean canManageUsers(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageUsers();
    }

    /**
     * Check if user can access database management.
     */
    public boolean canManageDatabase(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canManageDatabase();
    }

    /**
     * Check if user can view at-risk student alerts.
     */
    public boolean canViewAtRiskAlerts(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canViewAtRiskAlerts();
    }

    /**
     * Check if user can generate all types of reports.
     */
    public boolean canViewAllReports(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole().canViewAllReports();
    }

    /**
     * Check if user can only view their own data (student portal).
     */
    public boolean isStudentPortal(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole() == Role.STUDENT;
    }

    /**
     * Check if user should see the Teacher Portal dashboard.
     */
    public boolean isTeacherPortal(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return false;
        }
        return user.getPrimaryRole() == Role.TEACHER;
    }

    // ========================================================================
    // UI CUSTOMIZATION HELPERS
    // ========================================================================

    /**
     * Get the primary color for user's role (for UI theming).
     */
    public String getRolePrimaryColor(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return "#6c757d"; // Default gray
        }
        return user.getPrimaryRole().getPrimaryColor();
    }

    /**
     * Get the icon for user's role.
     */
    public String getRoleIcon(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return "👤"; // Default user icon
        }
        return user.getPrimaryRole().getIcon();
    }

    /**
     * Get user's role display name.
     */
    public String getRoleDisplayName(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return "No Role";
        }
        return user.getPrimaryRole().getDisplayName();
    }

    /**
     * Check if user has read-only access (cannot edit anything).
     */
    public boolean isReadOnly(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return true; // Default to read-only if no role
        }
        return user.getPrimaryRole().isReadOnly();
    }

    /**
     * Get workflow order for role (used for hierarchical displays).
     */
    public int getRoleWorkflowOrder(User user) {
        if (user == null || user.getPrimaryRole() == null) {
            return 999; // Put at end if no role
        }
        return user.getPrimaryRole().getWorkflowOrder();
    }
}
