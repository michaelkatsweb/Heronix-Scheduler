package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Parent/Guardian Data Transfer Object
 * Lightweight DTO for parent/guardian information
 *
 * Created for K-12 Enrollment Enhancement (Phase 2)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 14, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentGuardianDTO {

    // ========================================================================
    // BASIC INFORMATION
    // ========================================================================

    private Long id;

    /**
     * First name (REQUIRED)
     */
    private String firstName;

    /**
     * Last name (REQUIRED)
     */
    private String lastName;

    /**
     * Middle name
     */
    private String middleName;

    /**
     * Full name (computed)
     */
    private String fullName;

    /**
     * Relationship to student (REQUIRED)
     * Values: "Mother", "Father", "Stepmother", "Stepfather", "Guardian",
     *         "Foster Mother", "Foster Father", "Grandmother", "Grandfather",
     *         "Aunt", "Uncle", "Other Relative", "Other"
     */
    private String relationship;

    /**
     * Display name with relationship (e.g., "Jane Doe (Mother)")
     */
    private String displayName;

    /**
     * Is this the primary custodian?
     */
    private Boolean isPrimaryCustodian;

    /**
     * Lives with student?
     */
    private Boolean livesWithStudent;

    // ========================================================================
    // CONTACT INFORMATION
    // ========================================================================

    /**
     * Home phone
     */
    private String homePhone;

    /**
     * Cell/Mobile phone
     */
    private String cellPhone;

    /**
     * Work phone
     */
    private String workPhone;

    /**
     * Primary phone (best phone to reach - computed)
     */
    private String primaryPhone;

    /**
     * Email address
     */
    private String email;

    /**
     * Preferred contact method
     * Values: "Cell Phone", "Home Phone", "Work Phone", "Email", "Text"
     */
    private String preferredContactMethod;

    // ========================================================================
    // WORK INFORMATION
    // ========================================================================

    /**
     * Employer name
     */
    private String employer;

    /**
     * Work address
     */
    private String workAddress;

    /**
     * Occupation/Job title
     */
    private String occupation;

    // ========================================================================
    // ADDRESS (if different from student)
    // ========================================================================

    /**
     * Street address
     */
    private String streetAddress;

    /**
     * City
     */
    private String city;

    /**
     * State
     */
    private String state;

    /**
     * ZIP code
     */
    private String zipCode;

    /**
     * Formatted address (computed)
     */
    private String formattedAddress;

    // ========================================================================
    // LEGAL & AUTHORIZATION
    // ========================================================================

    /**
     * Has legal custody? (Can make educational decisions)
     */
    private Boolean hasLegalCustody;

    /**
     * Authorized to pick up student from school?
     */
    private Boolean canPickUpStudent;

    /**
     * Authorized for emergency contact?
     */
    private Boolean authorizedForEmergency;

    /**
     * Should receive report cards and school communications?
     */
    private Boolean receivesReportCards;

    /**
     * Court order restricting contact? (custody issues)
     */
    private Boolean hasCourtRestriction;

    /**
     * Court restriction notes (confidential)
     */
    private String courtRestrictionNotes;

    // ========================================================================
    // IDENTIFICATION (for verification during enrollment)
    // ========================================================================

    /**
     * ID type presented during enrollment
     * Values: "Driver's License", "State ID", "Passport", "Military ID",
     *         "I-94 Card", "Green Card", "Community ID"
     */
    private String idType;

    /**
     * ID number
     */
    private String idNumber;

    /**
     * ID issuing state
     */
    private String idState;

    /**
     * ID expiration date
     */
    private LocalDate idExpirationDate;

    /**
     * Is ID expired? (computed)
     */
    private Boolean idExpired;

    // ========================================================================
    // ADDITIONAL INFORMATION
    // ========================================================================

    /**
     * Preferred language
     */
    private String preferredLanguage;

    /**
     * Special notes
     */
    private String notes;

    /**
     * Priority order (for multiple parents/guardians)
     * 1 = Primary contact, 2 = Secondary, etc.
     */
    private Integer priorityOrder;

    // ========================================================================
    // AUDIT FIELDS
    // ========================================================================

    /**
     * When this record was created
     */
    private LocalDateTime createdAt;

    /**
     * When this record was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * Created by (staff username)
     */
    private String createdBy;

    // ========================================================================
    // RELATIONSHIP
    // ========================================================================

    /**
     * Student ID this parent/guardian belongs to
     */
    private Long studentId;

    // ========================================================================
    // COMPUTED/HELPER FIELDS
    // ========================================================================

    /**
     * Is this a custodial parent? (computed)
     */
    private Boolean isCustodial;

    /**
     * Has valid contact phone? (computed)
     */
    private Boolean hasContactPhone;

    /**
     * Has valid email? (computed)
     */
    private Boolean hasValidEmail;

    /**
     * Summary text for display (e.g., "Jane Doe (Mother) - (555) 123-4567")
     */
    private String contactSummary;
}
