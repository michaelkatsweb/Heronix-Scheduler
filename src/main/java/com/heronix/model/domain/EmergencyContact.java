package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Emergency Contact Entity - K-12 Enrollment Enhancement
 * Location: src/main/java/com/eduscheduler/model/domain/EmergencyContact.java
 *
 * Stores emergency contact information for students.
 * Schools typically require 2-5 emergency contacts (ordered by priority).
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 14, 2025
 */
@Entity
@Table(name = "emergency_contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

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
     * Relationship to student (REQUIRED)
     * Values: "Parent", "Grandparent", "Aunt", "Uncle", "Sibling",
     *         "Neighbor", "Family Friend", "Other"
     */
    @Column(nullable = false, name = "relationship", length = 50)
    private String relationship;

    // ========================================================================
    // CONTACT INFORMATION (REQUIRED - Multiple phones for redundancy)
    // ========================================================================

    /**
     * Primary phone number (REQUIRED)
     * This is the first number to call
     */
    @Column(nullable = false, name = "primary_phone", length = 20)
    private String primaryPhone;

    /**
     * Secondary phone number (backup)
     */
    @Column(name = "secondary_phone", length = 20)
    private String secondaryPhone;

    /**
     * Email address
     */
    @Column(name = "email", length = 100)
    private String email;

    // ========================================================================
    // PRIORITY & ORDERING
    // ========================================================================

    /**
     * Priority order (REQUIRED)
     * 1 = First person to call in emergency
     * 2 = Second person to call if first unavailable
     * 3 = Third, etc.
     *
     * Multiple contacts can have same priority (call all simultaneously)
     */
    @Column(nullable = false, name = "priority_order")
    private Integer priorityOrder;

    // ========================================================================
    // ADDRESS
    // ========================================================================

    /**
     * Street address
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
    // AUTHORIZATION & AVAILABILITY
    // ========================================================================

    /**
     * Authorized to pick up student from school?
     */
    @Column(name = "authorized_to_pick_up")
    private Boolean authorizedToPickUp = false;

    /**
     * Does this contact live with the student?
     */
    @Column(name = "lives_with_student")
    private Boolean livesWithStudent = false;

    /**
     * Availability notes
     * Examples: "Available 8am-5pm weekdays", "Weekends only", "Call after 6pm"
     */
    @Column(name = "availability_notes", length = 500)
    private String availabilityNotes;

    /**
     * Special notes (e.g., "Speaks Spanish only", "Hard of hearing - text preferred")
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========================================================================
    // WORK INFORMATION (Optional but helpful)
    // ========================================================================

    /**
     * Employer name
     */
    @Column(name = "employer", length = 200)
    private String employer;

    /**
     * Work phone
     */
    @Column(name = "work_phone", length = 20)
    private String workPhone;

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
     * Is this contact active/current?
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    /**
     * Student this emergency contact belongs to (REQUIRED)
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
        return firstName + " " + lastName;
    }

    /**
     * Get display name with relationship
     * Example: "Jane Doe (Grandmother)"
     */
    public String getDisplayName() {
        return getFullName() + " (" + relationship + ")";
    }

    /**
     * Get display name with priority
     * Example: "#1: Jane Doe (Grandmother)"
     */
    public String getDisplayNameWithPriority() {
        return "#" + priorityOrder + ": " + getFullName() + " (" + relationship + ")";
    }

    /**
     * Get best contact phone (primary or secondary)
     */
    public String getBestPhone() {
        if (primaryPhone != null && !primaryPhone.trim().isEmpty()) {
            return primaryPhone;
        }
        if (secondaryPhone != null && !secondaryPhone.trim().isEmpty()) {
            return secondaryPhone;
        }
        if (workPhone != null && !workPhone.trim().isEmpty()) {
            return workPhone;
        }
        return null;
    }

    /**
     * Get formatted address
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
     * Check if contact has valid phone number
     */
    public boolean hasValidPhone() {
        return getBestPhone() != null;
    }

    /**
     * Check if contact has email
     */
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty() && email.contains("@");
    }

    /**
     * Validate contact has minimum required information
     */
    public boolean isValid() {
        // Must have: first name, last name, relationship, priority, and phone
        return firstName != null && !firstName.trim().isEmpty()
            && lastName != null && !lastName.trim().isEmpty()
            && relationship != null && !relationship.trim().isEmpty()
            && priorityOrder != null && priorityOrder > 0
            && hasValidPhone();
    }

    /**
     * Get contact summary for quick display
     * Example: "#1: Jane Doe (Grandmother) - (555) 123-4567"
     */
    public String getContactSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(priorityOrder).append(": ");
        sb.append(getFullName()).append(" (").append(relationship).append(")");
        String phone = getBestPhone();
        if (phone != null) {
            sb.append(" - ").append(phone);
        }
        if (Boolean.TRUE.equals(authorizedToPickUp)) {
            sb.append(" [Can Pick Up]");
        }
        return sb.toString();
    }

    /**
     * Check if this is a high priority contact (priority 1 or 2)
     */
    public boolean isHighPriority() {
        return priorityOrder != null && priorityOrder <= 2;
    }

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();

        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
