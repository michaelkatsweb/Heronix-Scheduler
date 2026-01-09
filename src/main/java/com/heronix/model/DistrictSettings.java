package com.heronix.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * District Settings Entity
 * Stores district-wide configuration including email formats, ID generation rules,
 * and other district-specific settings.
 *
 * This is a singleton entity - only one record should exist in the database.
 *
 * Location: src/main/java/com/eduscheduler/model/DistrictSettings.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - District Configuration System
 */
@Entity
@Table(name = "district_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // DISTRICT INFORMATION
    // ========================================================================

    @Column(name = "district_name", length = 255)
    private String districtName;

    @Column(name = "district_address", length = 255)
    private String districtAddress;

    @Column(name = "district_city", length = 100)
    private String districtCity;

    @Column(name = "district_state", length = 50)
    private String districtState;

    @Column(name = "district_zip", length = 20)
    private String districtZip;

    @Column(name = "district_phone", length = 20)
    private String districtPhone;

    @Column(name = "district_fax", length = 20)
    private String districtFax;

    @Column(name = "district_website", length = 255)
    private String districtWebsite;

    @Column(name = "district_email", length = 255)
    private String districtEmail;

    // ========================================================================
    // EMAIL CONFIGURATION
    // ========================================================================

    /**
     * Email domain for teachers/staff
     * Example: "@hcsb.k12.fl.us"
     */
    @Column(name = "teacher_email_domain", length = 100)
    private String teacherEmailDomain;

    /**
     * Email domain for students
     * Example: "@hernandocountyschools.org"
     */
    @Column(name = "student_email_domain", length = 100)
    private String studentEmailDomain;

    /**
     * Email format pattern for teachers
     * Supported placeholders:
     * - {firstname} - Full first name
     * - {lastname} - Full last name
     * - {firstname_initial} - First letter of first name
     * - {lastname_initial} - First letter of last name
     * - {middlename_initial} - First letter of middle name
     *
     * Examples:
     * - "{lastname}_{firstname_initial}" → smith_j@domain.com
     * - "{firstname}.{lastname}" → john.smith@domain.com
     * - "{lastname}{firstname_initial}" → smithj@domain.com
     */
    @Column(name = "teacher_email_format", length = 100)
    private String teacherEmailFormat;

    /**
     * Email format pattern for students
     * Additional placeholders:
     * - {student_id} - Student ID number
     * - {grad_year} - Graduation year (4 digits)
     * - {grad_year_short} - Graduation year (2 digits)
     *
     * Examples:
     * - "{firstname}.{lastname}" → jane.doe@domain.com
     * - "{student_id}" → 2026-0001@domain.com
     * - "{lastname}{firstname_initial}{grad_year_short}" → doej26@domain.com
     */
    @Column(name = "student_email_format", length = 100)
    private String studentEmailFormat;

    /**
     * Auto-generate emails when creating new teachers
     */
    @Column(name = "auto_generate_teacher_email")
    @Builder.Default
    private Boolean autoGenerateTeacherEmail = true;

    /**
     * Auto-generate emails when creating new students
     */
    @Column(name = "auto_generate_student_email")
    @Builder.Default
    private Boolean autoGenerateStudentEmail = true;

    // ========================================================================
    // TEACHER ID CONFIGURATION
    // ========================================================================

    /**
     * Prefix for teacher IDs
     * Example: "T", "E" (for Employee), "TCH", etc.
     */
    @Column(name = "teacher_id_prefix", length = 10)
    private String teacherIdPrefix;

    /**
     * Starting number for teacher ID sequence
     * Example: 1, 1000, 100000
     */
    @Column(name = "teacher_id_start_number")
    @Builder.Default
    private Integer teacherIdStartNumber = 1;

    /**
     * Number of digits to pad teacher ID
     * Example: 4 → T0001, T0002
     *          6 → T000001, T000002
     */
    @Column(name = "teacher_id_padding")
    @Builder.Default
    private Integer teacherIdPadding = 4;

    /**
     * Current/next teacher ID number to assign
     * This increments with each new teacher
     */
    @Column(name = "teacher_id_current_number")
    @Builder.Default
    private Integer teacherIdCurrentNumber = 1;

    /**
     * Auto-generate teacher IDs when creating new teachers
     */
    @Column(name = "auto_generate_teacher_id")
    @Builder.Default
    private Boolean autoGenerateTeacherId = true;

    // ========================================================================
    // STUDENT ID CONFIGURATION
    // ========================================================================

    /**
     * Student ID format pattern
     * Supported placeholders:
     * - {grad_year} - 4-digit graduation year
     * - {grad_year_short} - 2-digit graduation year
     * - {sequence} - Auto-incrementing sequence number
     * - {prefix} - Custom prefix
     *
     * Examples:
     * - "{grad_year}-{sequence}" → 2026-0001, 2026-0002
     * - "S{sequence}" → S0001, S0002
     * - "{prefix}{grad_year_short}{sequence}" → STU26001
     */
    @Column(name = "student_id_format", length = 100)
    private String studentIdFormat;

    /**
     * Prefix for student IDs (if using prefix placeholder)
     */
    @Column(name = "student_id_prefix", length = 10)
    private String studentIdPrefix;

    /**
     * Number of digits to pad student ID sequence
     * Example: 4 → 0001, 0002
     */
    @Column(name = "student_id_padding")
    @Builder.Default
    private Integer studentIdPadding = 4;

    /**
     * Auto-generate student IDs when creating new students
     */
    @Column(name = "auto_generate_student_id")
    @Builder.Default
    private Boolean autoGenerateStudentId = true;

    // ========================================================================
    // ROOM PHONE CONFIGURATION
    // ========================================================================

    /**
     * Prefix for room phone numbers
     * Example: "(352) 754-", "(813) 555-"
     */
    @Column(name = "room_phone_prefix", length = 50)
    private String roomPhonePrefix;

    /**
     * Starting extension number for room phones
     * Example: 4000, 1000
     */
    @Column(name = "room_phone_extension_start")
    @Builder.Default
    private Integer roomPhoneExtensionStart = 4000;

    /**
     * Auto-generate room phone numbers
     */
    @Column(name = "auto_generate_room_phone")
    @Builder.Default
    private Boolean autoGenerateRoomPhone = true;

    // ========================================================================
    // PRINT/EXPORT CONFIGURATION
    // ========================================================================

    /**
     * Path to school/district logo for printed materials
     */
    @Column(name = "logo_path", length = 500)
    private String logoPath;

    /**
     * Header text to appear on printed schedules
     */
    @Column(name = "schedule_header_text", length = 500)
    private String scheduleHeaderText;

    /**
     * Include district information on printed materials
     */
    @Column(name = "include_district_info_on_print")
    @Builder.Default
    private Boolean includeDistrictInfoOnPrint = true;

    /**
     * Footer text for printed materials
     */
    @Column(name = "print_footer_text", length = 500)
    private String printFooterText;

    // ========================================================================
    // ACADEMIC YEAR CONFIGURATION
    // ========================================================================

    /**
     * Default class/period duration in minutes
     */
    @Column(name = "default_period_duration")
    @Builder.Default
    private Integer defaultPeriodDuration = 50;

    /**
     * Default passing period duration in minutes
     */
    @Column(name = "default_passing_period")
    @Builder.Default
    private Integer defaultPassingPeriod = 5;

    /**
     * Default lunch duration in minutes
     */
    @Column(name = "default_lunch_duration")
    @Builder.Default
    private Integer defaultLunchDuration = 30;

    // ========================================================================
    // AUDIT FIELDS
    // ========================================================================

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // ========================================================================
    // LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if district has been configured
     */
    public boolean isConfigured() {
        return districtName != null && !districtName.trim().isEmpty();
    }

    /**
     * Check if email generation is fully configured
     */
    public boolean isEmailConfigured() {
        return teacherEmailDomain != null && !teacherEmailDomain.trim().isEmpty() &&
               teacherEmailFormat != null && !teacherEmailFormat.trim().isEmpty() &&
               studentEmailDomain != null && !studentEmailDomain.trim().isEmpty() &&
               studentEmailFormat != null && !studentEmailFormat.trim().isEmpty();
    }

    /**
     * Check if ID generation is fully configured
     */
    public boolean isIdGenerationConfigured() {
        return (teacherIdPrefix != null && !teacherIdPrefix.trim().isEmpty()) &&
               (studentIdFormat != null && !studentIdFormat.trim().isEmpty());
    }
}
