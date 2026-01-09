package com.heronix.model.domain;

import com.heronix.model.enums.ParaAssignmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Paraprofessional Entity
 * Represents educational paraprofessionals, teacher aides, and support staff
 * Used for special education support, classroom assistance, and student services
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Entity
@Table(name = "paraprofessionals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paraprofessional {

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
    // PARAPROFESSIONAL ATTRIBUTES
    // ========================================================================

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Role type (e.g., "Special Ed Aide", "Classroom Aide", "Behavioral Support", "1:1 Aide")
     */
    @Column(name = "role_type")
    private String roleType;

    /**
     * Certifications held (e.g., "Para Pro Certified", "CPR Certified", "Crisis Intervention")
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "paraprofessional_certifications",
                     joinColumns = @JoinColumn(name = "paraprofessional_id"))
    @Column(name = "certification")
    private Set<String> certifications = new HashSet<>();

    /**
     * Specialized skills (e.g., "Sign Language", "Behavior Management", "Feeding Assistance")
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "paraprofessional_skills",
                     joinColumns = @JoinColumn(name = "paraprofessional_id"))
    @Column(name = "skill")
    private Set<String> specializedSkills = new HashSet<>();

    /**
     * Assignment type: ONE_ON_ONE, SMALL_GROUP, CLASSROOM_SUPPORT, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type_enum")
    private ParaAssignmentType assignmentTypeEnum = ParaAssignmentType.CLASSROOM_SUPPORT;

    /**
     * Assignment type (e.g., "Student-Specific", "Classroom Support", "Roving", "1:1")
     * @deprecated Use assignmentTypeEnum instead
     */
    @Deprecated
    @Column(name = "assignment_type")
    private String assignmentType;

    /**
     * Maximum number of students can support (for 1:1 or small group)
     */
    @Column(name = "max_students")
    private Integer maxStudents = 1;

    /**
     * Preferred grade levels
     */
    @Column(name = "preferred_grades")
    private String preferredGrades;

    /**
     * Work schedule (e.g., "Full Day", "Morning Only", "Afternoon Only")
     */
    @Column(name = "work_schedule")
    private String workSchedule;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "availability_notes")
    private String availabilityNotes;

    /**
     * Whether this para is trained for medical needs
     */
    @Column(name = "medical_training")
    private Boolean medicalTraining = false;

    /**
     * Whether this para is trained for behavioral intervention
     */
    @Column(name = "behavioral_training")
    private Boolean behavioralTraining = false;

    // ========================================================================
    // STUDENT ASSIGNMENTS
    // ========================================================================

    /**
     * Students assigned to this paraprofessional
     * For ONE_ON_ONE: typically 1 student
     * For SMALL_GROUP: typically 2-5 students
     * For other types: may be empty or contain students needing special attention
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "paraprofessional_student_assignments",
        joinColumns = @JoinColumn(name = "paraprofessional_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @ToString.Exclude
    private List<Student> assignedStudents = new ArrayList<>();

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
     * Get current student count
     */
    public int getCurrentStudentCount() {
        return assignedStudents != null ? assignedStudents.size() : 0;
    }

    /**
     * Check if paraprofessional is at capacity
     */
    public boolean isAtCapacity() {
        return maxStudents != null && getCurrentStudentCount() >= maxStudents;
    }

    /**
     * Check if paraprofessional is available for more assignments
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
     * Get specialized skills as comma-separated string
     */
    public String getSkillsDisplay() {
        if (specializedSkills == null || specializedSkills.isEmpty()) {
            return "None";
        }
        return String.join(", ", specializedSkills);
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

    /**
     * Add a specialized skill
     */
    public void addSkill(String skill) {
        if (specializedSkills == null) {
            specializedSkills = new HashSet<>();
        }
        specializedSkills.add(skill);
    }

    /**
     * Remove a specialized skill
     */
    public void removeSkill(String skill) {
        if (specializedSkills != null) {
            specializedSkills.remove(skill);
        }
    }

    /**
     * Check if has specific skill
     */
    public boolean hasSkill(String skill) {
        return specializedSkills != null && specializedSkills.stream()
                .anyMatch(s -> s.equalsIgnoreCase(skill));
    }

    /**
     * Check if has medical training
     */
    public boolean hasMedicalTraining() {
        return Boolean.TRUE.equals(medicalTraining);
    }

    /**
     * Check if has behavioral training
     */
    public boolean hasBehavioralTraining() {
        return Boolean.TRUE.equals(behavioralTraining);
    }

    /**
     * Assign a student to this paraprofessional
     */
    public void assignStudent(Student student) {
        if (assignedStudents == null) {
            assignedStudents = new ArrayList<>();
        }
        if (!assignedStudents.contains(student)) {
            assignedStudents.add(student);
        }
    }

    /**
     * Remove a student from this paraprofessional's assigned list
     */
    public void removeAssignedStudent(Student student) {
        if (assignedStudents != null) {
            assignedStudents.remove(student);
        }
    }

    /**
     * Check if this is a one-on-one assignment
     */
    public boolean isOneOnOne() {
        return assignmentTypeEnum == ParaAssignmentType.ONE_ON_ONE;
    }

    /**
     * Check if this is a small group assignment
     */
    public boolean isSmallGroup() {
        return assignmentTypeEnum == ParaAssignmentType.SMALL_GROUP;
    }

    /**
     * Check if this is a classroom support assignment
     */
    public boolean isClassroomSupport() {
        return assignmentTypeEnum == ParaAssignmentType.CLASSROOM_SUPPORT;
    }

    /**
     * Check if this is a floater assignment
     */
    public boolean isFloater() {
        return assignmentTypeEnum == ParaAssignmentType.FLOATER;
    }

    @Override
    public String toString() {
        return "Paraprofessional{" +
                "id=" + id +
                ", name='" + getFullName() + '\'' +
                ", roleType='" + roleType + '\'' +
                ", assignmentType=" + assignmentTypeEnum +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paraprofessional)) return false;
        Paraprofessional that = (Paraprofessional) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
