package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Emergency Contact Data Transfer Object
 * Lightweight DTO for emergency contact information
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
public class EmergencyContactDTO {

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
     * Full name (computed: FirstName LastName)
     */
    private String fullName;

    /**
     * Relationship to student (REQUIRED)
     * Values: "Parent", "Grandparent", "Aunt", "Uncle", "Sibling",
     *         "Neighbor", "Family Friend", "Other"
     */
    private String relationship;

    /**
     * Display name with relationship (e.g., "Jane Doe (Grandmother)")
     */
    private String displayName;

    /**
     * Display name with priority (e.g., "#1: Jane Doe (Grandmother)")
     */
    private String displayNameWithPriority;

    // ========================================================================
    // CONTACT INFORMATION (REQUIRED - Multiple phones for redundancy)
    // ========================================================================

    /**
     * Primary phone number (REQUIRED)
     * This is the first number to call
     */
    private String primaryPhone;

    /**
     * Secondary phone number (backup)
     */
    private String secondaryPhone;

    /**
     * Work phone number
     */
    private String workPhone;

    /**
     * Best available phone (computed - primary, secondary, or work)
     */
    private String bestPhone;

    /**
     * Email address
     */
    private String email;

    // ========================================================================
    // PRIORITY & ORDERING
    // ========================================================================

    /**
     * Priority order (REQUIRED)
     * 1 = First person to call in emergency
     * 2 = Second person to call if first unavailable
     * 3 = Third, etc.
     */
    private Integer priorityOrder;

    /**
     * Is this a high priority contact? (priority 1 or 2) - computed
     */
    private Boolean isHighPriority;

    // ========================================================================
    // ADDRESS
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
    // AUTHORIZATION & AVAILABILITY
    // ========================================================================

    /**
     * Authorized to pick up student from school?
     */
    private Boolean authorizedToPickUp;

    /**
     * Does this contact live with the student?
     */
    private Boolean livesWithStudent;

    /**
     * Availability notes
     * Examples: "Available 8am-5pm weekdays", "Weekends only", "Call after 6pm"
     */
    private String availabilityNotes;

    /**
     * Special notes (e.g., "Speaks Spanish only", "Hard of hearing - text preferred")
     */
    private String notes;

    // ========================================================================
    // WORK INFORMATION (Optional but helpful)
    // ========================================================================

    /**
     * Employer name
     */
    private String employer;

    // ========================================================================
    // STATUS
    // ========================================================================

    /**
     * Is this contact active/current?
     */
    private Boolean isActive;

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

    // ========================================================================
    // RELATIONSHIP
    // ========================================================================

    /**
     * Student ID this emergency contact belongs to
     */
    private Long studentId;

    // ========================================================================
    // COMPUTED/HELPER FIELDS
    // ========================================================================

    /**
     * Has valid phone number? (computed)
     */
    private Boolean hasValidPhone;

    /**
     * Has valid email? (computed)
     */
    private Boolean hasValidEmail;

    /**
     * Contact summary for quick display (computed)
     * Example: "#1: Jane Doe (Grandmother) - (555) 123-4567 [Can Pick Up]"
     */
    private String contactSummary;

    /**
     * Is this contact valid? (has required fields) - computed
     */
    private Boolean isValid;
}
