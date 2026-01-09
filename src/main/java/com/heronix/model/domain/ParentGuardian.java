package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Parent/Guardian Entity - K-12 Enrollment Enhancement
 * Location: src/main/java/com/eduscheduler/model/domain/ParentGuardian.java
 *
 * Stores parent/guardian information for students.
 * One student can have multiple parents/guardians (divorced, foster, etc.)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 14, 2025
 */
@Entity
@Table(name = "parent_guardians")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentGuardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // BASIC INFORMATION
    // ========================================================================

    /**
     * First name (REQUIRED)
     */
    @Column(nullable = false, name = "first_name", length = 100)
    private String firstName;

    /**
     * Last name (REQUIRED)
     */
    @Column(nullable = false, name = "last_name", length = 100)
    private String lastName;

    /**
     * Middle name
     */
    @Column(name = "middle_name", length = 100)
    private String middleName;

    /**
     * Relationship to student (REQUIRED)
     * Values: "Mother", "Father", "Stepmother", "Stepfather", "Guardian",
     *         "Foster Mother", "Foster Father", "Grandmother", "Grandfather",
     *         "Aunt", "Uncle", "Other Relative", "Other"
     */
    @Column(nullable = false, name = "relationship", length = 50)
    private String relationship;

    /**
     * Is this the primary custodian? (Legal custody)
     */
    @Column(name = "is_primary_custodian")
    private Boolean isPrimaryCustodian = false;

    /**
     * Does parent/guardian live with student?
     */
    @Column(name = "lives_with_student")
    private Boolean livesWithStudent = true;

    // ========================================================================
    // CONTACT INFORMATION
    // ========================================================================

    /**
     * Home phone (REQUIRED - at least one phone needed)
     */
    @Column(name = "home_phone", length = 20)
    private String homePhone;

    /**
     * Cell/Mobile phone
     */
    @Column(name = "cell_phone", length = 20)
    private String cellPhone;

    /**
     * Work phone
     */
    @Column(name = "work_phone", length = 20)
    private String workPhone;

    /**
     * Email address
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Preferred contact method
     * Values: "Cell Phone", "Home Phone", "Work Phone", "Email", "Text"
     */
    @Column(name = "preferred_contact_method", length = 50)
    private String preferredContactMethod;

    // ========================================================================
    // WORK INFORMATION
    // ========================================================================

    /**
     * Employer name
     */
    @Column(name = "employer", length = 200)
    private String employer;

    /**
     * Work address
     */
    @Column(name = "work_address", length = 300)
    private String workAddress;

    /**
     * Occupation/Job title
     */
    @Column(name = "occupation", length = 100)
    private String occupation;

    // ========================================================================
    // ADDRESS (if different from student)
    // ========================================================================

    /**
     * Street address (if different from student's home address)
     */
    @Column(name = "street_address", length = 255)
    private String streetAddress;

    /**
     * City
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State
     */
    @Column(name = "state", length = 2)
    private String state;

    /**
     * ZIP code
     */
    @Column(name = "zip_code", length = 10)
    private String zipCode;

    // ========================================================================
    // LEGAL & AUTHORIZATION
    // ========================================================================

    /**
     * Has legal custody? (Can make educational decisions)
     */
    @Column(name = "has_legal_custody")
    private Boolean hasLegalCustody = true;

    /**
     * Authorized to pick up student from school?
     */
    @Column(name = "can_pick_up_student")
    private Boolean canPickUpStudent = true;

    /**
     * Authorized for emergency contact?
     */
    @Column(name = "authorized_for_emergency")
    private Boolean authorizedForEmergency = true;

    /**
     * Should receive report cards and school communications?
     */
    @Column(name = "receives_report_cards")
    private Boolean receivesReportCards = true;

    /**
     * Court order restricting contact? (custody issues)
     */
    @Column(name = "has_court_restriction")
    private Boolean hasCourtRestriction = false;

    /**
     * Court restriction notes (confidential)
     */
    @Column(name = "court_restriction_notes", columnDefinition = "TEXT")
    private String courtRestrictionNotes;

    // ========================================================================
    // IDENTIFICATION (for verification during enrollment)
    // ========================================================================

    /**
     * ID type presented during enrollment
     * Values: "Driver's License", "State ID", "Passport", "Military ID",
     *         "I-94 Card", "Green Card", "Community ID"
     */
    @Column(name = "id_type", length = 50)
    private String idType;

    /**
     * ID number
     */
    @Column(name = "id_number", length = 50)
    private String idNumber;

    /**
     * ID issuing state
     */
    @Column(name = "id_state", length = 2)
    private String idState;

    /**
     * ID expiration date
     */
    @Column(name = "id_expiration_date")
    private java.time.LocalDate idExpirationDate;

    // ========================================================================
    // ADDITIONAL INFORMATION
    // ========================================================================

    /**
     * Preferred language
     */
    @Column(name = "preferred_language", length = 50)
    private String preferredLanguage;

    /**
     * Special notes (allergies to be aware of during parent visits, etc.)
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Priority order (for multiple parents/guardians)
     * 1 = Primary contact, 2 = Secondary, etc.
     */
    @Column(name = "priority_order")
    private Integer priorityOrder = 1;

    // ========================================================================
    // AUDIT FIELDS
    // ========================================================================

    /**
     * When this record was created
     */
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    /**
     * When this record was last updated
     */
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    /**
     * Created by (staff username)
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    /**
     * Student this parent/guardian belongs to (REQUIRED)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @ToString.Exclude
    private Student student;

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Get full name (FirstName LastName)
     */
    public String getFullName() {
        if (middleName != null && !middleName.trim().isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    /**
     * Get display name with relationship
     * Example: "Jane Doe (Mother)"
     */
    public String getDisplayName() {
        return getFullName() + " (" + relationship + ")";
    }

    /**
     * Get primary contact phone (first available)
     */
    public String getPrimaryPhone() {
        if (cellPhone != null && !cellPhone.trim().isEmpty()) {
            return cellPhone;
        }
        if (homePhone != null && !homePhone.trim().isEmpty()) {
            return homePhone;
        }
        if (workPhone != null && !workPhone.trim().isEmpty()) {
            return workPhone;
        }
        return null;
    }

    /**
     * Check if parent has any contact phone
     */
    public boolean hasContactPhone() {
        return getPrimaryPhone() != null;
    }

    /**
     * Check if parent has valid email
     */
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty() && email.contains("@");
    }

    /**
     * Get formatted address (if different from student)
     */
    public String getFormattedAddress() {
        if (streetAddress == null || streetAddress.trim().isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(streetAddress);
        if (city != null && !city.trim().isEmpty()) {
            sb.append(", ").append(city);
        }
        if (state != null && !state.trim().isEmpty()) {
            sb.append(", ").append(state);
        }
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            sb.append(" ").append(zipCode);
        }
        return sb.toString();
    }

    /**
     * Check if this is a custodial parent
     */
    public boolean isCustodial() {
        return Boolean.TRUE.equals(isPrimaryCustodian) || Boolean.TRUE.equals(hasLegalCustody);
    }

    /**
     * Validate parent/guardian has minimum required information
     */
    public boolean isValid() {
        // Must have: first name, last name, relationship, and at least one phone
        return firstName != null && !firstName.trim().isEmpty()
            && lastName != null && !lastName.trim().isEmpty()
            && relationship != null && !relationship.trim().isEmpty()
            && hasContactPhone();
    }

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
