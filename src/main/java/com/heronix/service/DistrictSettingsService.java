package com.heronix.service;

import com.heronix.model.DistrictSettings;

/**
 * Service interface for District Settings management
 *
 * Provides methods for:
 * - Getting/updating district configuration
 * - Generating emails based on district policy
 * - Generating IDs based on district policy
 * - Checking for duplicates
 *
 * Location: src/main/java/com/eduscheduler/service/DistrictSettingsService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - District Configuration System
 */
public interface DistrictSettingsService {

    // ========================================================================
    // DISTRICT SETTINGS CRUD
    // ========================================================================

    /**
     * Get district settings, creating default if none exist
     * @return DistrictSettings instance (never null)
     */
    DistrictSettings getOrCreateDistrictSettings();

    /**
     * Update district settings
     * @param settings Updated settings
     * @return Saved settings
     */
    DistrictSettings updateDistrictSettings(DistrictSettings settings);

    /**
     * Check if district has been configured
     * @return true if configured
     */
    boolean isDistrictConfigured();

    // ========================================================================
    // EMAIL GENERATION
    // ========================================================================

    /**
     * Generate teacher email based on district policy
     * @param firstName Teacher's first name
     * @param lastName Teacher's last name
     * @return Generated email address
     */
    String generateTeacherEmail(String firstName, String lastName);

    /**
     * Generate teacher email with middle name
     * @param firstName Teacher's first name
     * @param middleName Teacher's middle name (optional)
     * @param lastName Teacher's last name
     * @return Generated email address
     */
    String generateTeacherEmail(String firstName, String middleName, String lastName);

    /**
     * Generate student email based on district policy
     * @param firstName Student's first name
     * @param lastName Student's last name
     * @param gradYear Graduation year
     * @return Generated email address
     */
    String generateStudentEmail(String firstName, String lastName, int gradYear);

    /**
     * Generate student email with student ID
     * @param firstName Student's first name
     * @param lastName Student's last name
     * @param gradYear Graduation year
     * @param studentId Existing student ID (if any)
     * @return Generated email address
     */
    String generateStudentEmail(String firstName, String lastName, int gradYear, String studentId);

    /**
     * Check if teacher email is already taken
     * @param email Email to check
     * @return true if taken
     */
    boolean isTeacherEmailTaken(String email);

    /**
     * Check if student email is already taken
     * @param email Email to check
     * @return true if taken
     */
    boolean isStudentEmailTaken(String email);

    /**
     * Generate alternative email if original is taken
     * @param baseEmail Base email that was taken
     * @param isTeacher true for teacher, false for student
     * @return Alternative email address
     */
    String generateAlternativeEmail(String baseEmail, boolean isTeacher);

    // ========================================================================
    // ID GENERATION
    // ========================================================================

    /**
     * Generate next teacher ID based on district policy
     * @return Generated teacher ID (e.g., "T0001")
     */
    String generateNextTeacherId();

    /**
     * Generate next student ID based on district policy
     * @param gradYear Graduation year
     * @return Generated student ID (e.g., "2026-0001")
     */
    String generateNextStudentId(int gradYear);

    /**
     * Check if teacher ID is available (not used)
     * @param teacherId ID to check
     * @return true if available
     */
    boolean isTeacherIdAvailable(String teacherId);

    /**
     * Check if student ID is available (not used)
     * @param studentId ID to check
     * @return true if available
     */
    boolean isStudentIdAvailable(String studentId);

    // ========================================================================
    // ROOM PHONE GENERATION
    // ========================================================================

    /**
     * Generate room phone number based on room number
     * @param roomNumber Room number (e.g., "CLS-101")
     * @return Generated phone number (e.g., "(352) 754-4101")
     */
    String generateRoomPhoneNumber(String roomNumber);

    /**
     * Extract numeric part from room number for phone generation
     * @param roomNumber Room number
     * @return Numeric part
     */
    Integer extractRoomNumberValue(String roomNumber);

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate email format configuration
     * @param format Email format pattern
     * @return true if valid
     */
    boolean isValidEmailFormat(String format);

    /**
     * Validate ID format configuration
     * @param format ID format pattern
     * @return true if valid
     */
    boolean isValidIdFormat(String format);

    /**
     * Get list of supported email format placeholders
     * @return List of placeholders
     */
    String[] getEmailFormatPlaceholders();

    /**
     * Get list of supported ID format placeholders
     * @return List of placeholders
     */
    String[] getIdFormatPlaceholders();
}
