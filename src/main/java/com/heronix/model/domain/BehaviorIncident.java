package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Behavior Incident Entity
 * Tracks both positive and negative student behavior incidents for monitoring and CRDC reporting.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Entity
@Table(name = "behavior_incidents", indexes = {
    @Index(name = "idx_behavior_student_date", columnList = "student_id, incident_date"),
    @Index(name = "idx_behavior_type_date", columnList = "behavior_type, incident_date"),
    @Index(name = "idx_behavior_category", columnList = "behavior_category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // Nullable for non-class incidents (hallway, cafeteria, etc.)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher reportingTeacher;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "incident_time", nullable = false)
    private LocalTime incidentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_location", nullable = false)
    @Builder.Default
    private IncidentLocation incidentLocation = IncidentLocation.CLASSROOM;

    @Enumerated(EnumType.STRING)
    @Column(name = "behavior_type", nullable = false)
    @Builder.Default
    private BehaviorType behaviorType = BehaviorType.NEGATIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "behavior_category", nullable = false)
    @Builder.Default
    private BehaviorCategory behaviorCategory = BehaviorCategory.DISRUPTION;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level")
    private SeverityLevel severityLevel; // For negative behaviors only

    @Column(name = "incident_description", columnDefinition = "TEXT")
    private String incidentDescription;

    @Column(name = "intervention_applied", columnDefinition = "TEXT")
    private String interventionApplied;

    @Column(name = "parent_contacted", nullable = false)
    @Builder.Default
    private Boolean parentContacted = false;

    @Column(name = "parent_contact_date")
    private LocalDate parentContactDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "parent_contact_method")
    private ContactMethod parentContactMethod;

    @Column(name = "admin_referral_required", nullable = false)
    @Builder.Default
    private Boolean adminReferralRequired = false;

    @Column(name = "referral_outcome", length = 255)
    private String referralOutcome;

    @Column(name = "evidence_attached", nullable = false)
    @Builder.Default
    private Boolean evidenceAttached = false;

    @Column(name = "evidence_file_path")
    private String evidenceFilePath;

    @Column(name = "entry_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime entryTimestamp = LocalDateTime.now();

    @Column(name = "entered_by_staff_id", nullable = false)
    private Long enteredByStaffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum BehaviorType {
        POSITIVE,
        NEGATIVE
    }

    public enum BehaviorCategory {
        // Positive categories
        PARTICIPATION("Positive"),
        COLLABORATION("Positive"),
        LEADERSHIP("Positive"),
        IMPROVEMENT("Positive"),
        HELPING_OTHERS("Positive"),

        // Negative categories
        DISRUPTION("Negative"),
        TARDINESS("Negative"),
        NON_COMPLIANCE("Negative"),
        BULLYING("Negative"),
        FIGHTING("Negative"),
        DEFIANCE("Negative"),
        INAPPROPRIATE_LANGUAGE("Negative"),
        VANDALISM("Negative"),
        THEFT("Negative"),
        HARASSMENT("Negative"),
        TECHNOLOGY_MISUSE("Negative"),
        DRESS_CODE_VIOLATION("Negative"),
        OTHER("Both");

        private final String behaviorType;

        BehaviorCategory(String behaviorType) {
            this.behaviorType = behaviorType;
        }

        public String getBehaviorType() {
            return behaviorType;
        }
    }

    public enum SeverityLevel {
        MINOR,      // Verbal warning, classroom management
        MODERATE,   // Parent contact, detention
        MAJOR       // Admin referral, suspension consideration
    }

    public enum IncidentLocation {
        CLASSROOM,
        HALLWAY,
        CAFETERIA,
        GYMNASIUM,
        LIBRARY,
        AUDITORIUM,
        PARKING_LOT,
        BUS,
        BATHROOM,
        PLAYGROUND,
        OTHER
    }

    public enum ContactMethod {
        PHONE,
        EMAIL,
        IN_PERSON,
        LETTER,
        TEXT_MESSAGE,
        PARENT_PORTAL
    }

    // ========================================================================
    // CALCULATED FIELDS
    // ========================================================================

    @Transient
    public boolean isPositive() {
        return behaviorType == BehaviorType.POSITIVE;
    }

    @Transient
    public boolean isNegative() {
        return behaviorType == BehaviorType.NEGATIVE;
    }

    @Transient
    public boolean requiresImmediateAttention() {
        return severityLevel == SeverityLevel.MAJOR || adminReferralRequired;
    }
}
