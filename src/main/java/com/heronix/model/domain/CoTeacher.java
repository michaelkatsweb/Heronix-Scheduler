package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Co-Teacher Entity
 * Represents additional teaching staff who work alongside primary teachers
 * Used for inclusion, special education support, and team teaching
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Entity
@Table(name = "co_teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // PERSONAL INFORMATION
    // ========================================================================

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "employee_id", unique = true)
    private String employeeId;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    // ========================================================================
    // CO-TEACHER ATTRIBUTES
    // ========================================================================

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Specialization area (e.g., "Special Education", "ESL", "Math Support")
     */
    @Column(name = "specialization")
    private String specialization;

    /**
     * Certifications held by the co-teacher
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "co_teacher_certifications",
                     joinColumns = @JoinColumn(name = "co_teacher_id"))
    @Column(name = "certification")
    private Set<String> certifications = new HashSet<>();

    /**
     * Support level provided (e.g., "Full Time", "Part Time", "Consultation Only")
     */
    @Column(name = "support_level")
    private String supportLevel;

    /**
     * Maximum number of concurrent classes
     */
    @Column(name = "max_classes")
    private Integer maxClasses = 5;

    /**
     * Preferred grade levels
     */
    @Column(name = "preferred_grades")
    private String preferredGrades;

    /**
     * Preferred subjects
     */
    @Column(name = "preferred_subjects")
    private String preferredSubjects;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "availability_notes")
    private String availabilityNotes;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    /**
     * Schedule slots where this co-teacher is assigned
     */
    @ManyToMany(mappedBy = "coTeachers", fetch = FetchType.LAZY)
    private Set<ScheduleSlot> scheduleSlots = new HashSet<>();

    /**
     * Courses where this co-teacher is assigned
     */
    @ManyToMany(mappedBy = "coTeachers", fetch = FetchType.LAZY)
    private Set<Course> courses = new HashSet<>();

    // ========================================================================
    // AUDIT FIELDS
    // ========================================================================

    @Column(name = "date_added")
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // ========================================================================
    // LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        dateAdded = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get name in "Last, First" format
     */
    public String getNameLastFirst() {
        return lastName + ", " + firstName;
    }

    /**
     * Get current class count
     */
    public int getCurrentClassCount() {
        return scheduleSlots != null ? scheduleSlots.size() : 0;
    }

    /**
     * Check if co-teacher is at capacity
     */
    public boolean isAtCapacity() {
        return maxClasses != null && getCurrentClassCount() >= maxClasses;
    }

    /**
     * Check if co-teacher is available for more assignments
     */
    public boolean isAvailableForAssignment() {
        return Boolean.TRUE.equals(active) && !isAtCapacity();
    }

    /**
     * Get certifications as comma-separated string
     */
    public String getCertificationsDisplay() {
        if (certifications == null || certifications.isEmpty()) {
            return "None";
        }
        return String.join(", ", certifications);
    }

    /**
     * Add a certification
     */
    public void addCertification(String certification) {
        if (certifications == null) {
            certifications = new HashSet<>();
        }
        certifications.add(certification);
    }

    /**
     * Remove a certification
     */
    public void removeCertification(String certification) {
        if (certifications != null) {
            certifications.remove(certification);
        }
    }

    @Override
    public String toString() {
        return "CoTeacher{" +
                "id=" + id +
                ", name='" + getFullName() + '\'' +
                ", specialization='" + specialization + '\'' +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoTeacher)) return false;
        CoTeacher that = (CoTeacher) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
