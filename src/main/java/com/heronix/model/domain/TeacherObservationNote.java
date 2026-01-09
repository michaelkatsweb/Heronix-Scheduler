package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Teacher Observation Note Entity
 * Tracks teacher observations about student comprehension, effort, engagement, and social-emotional well-being.
 * Used for early intervention identification and student progress monitoring.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Entity
@Table(name = "teacher_observation_notes", indexes = {
    @Index(name = "idx_observation_student_date", columnList = "student_id, observation_date"),
    @Index(name = "idx_observation_course_date", columnList = "course_id, observation_date"),
    @Index(name = "idx_observation_intervention_flag", columnList = "is_flag_for_intervention, observation_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherObservationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "observation_date", nullable = false)
    private LocalDate observationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_category", nullable = false)
    @Builder.Default
    private ObservationCategory observationCategory = ObservationCategory.COMPREHENSION;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_rating")
    private ObservationRating observationRating;

    @Column(name = "observation_notes", columnDefinition = "TEXT")
    private String observationNotes;

    @Column(name = "is_flag_for_intervention", nullable = false)
    @Builder.Default
    private Boolean isFlagForIntervention = false;

    @Column(name = "intervention_type_suggested", length = 255)
    private String interventionTypeSuggested; // Academic tutoring, counseling, RTI Tier 2, etc.

    @Column(name = "entry_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime entryTimestamp = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum ObservationCategory {
        COMPREHENSION("Academic - Understanding of content"),
        EFFORT("Academic - Work ethic and persistence"),
        ENGAGEMENT("Academic - Participation and interest"),
        SOCIAL_EMOTIONAL("Well-Being - Social and emotional health"),
        BEHAVIOR("Well-Being - Classroom behavior"),
        ATTENDANCE_PATTERNS("Well-Being - Attendance and punctuality"),
        PEER_INTERACTIONS("Well-Being - Social relationships"),
        MOTIVATION("Academic - Drive and self-direction");

        private final String description;

        ObservationCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ObservationRating {
        EXCELLENT("Significantly exceeds expectations"),
        GOOD("Meets or exceeds expectations"),
        NEEDS_IMPROVEMENT("Requires support to meet expectations"),
        CONCERN("Significant concern - immediate intervention needed");

        private final String description;

        ObservationRating(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========================================================================
    // CALCULATED FIELDS
    // ========================================================================

    @Transient
    public boolean indicatesRisk() {
        return observationRating == ObservationRating.CONCERN ||
               (observationRating == ObservationRating.NEEDS_IMPROVEMENT && isFlagForIntervention);
    }

    @Transient
    public boolean isPositive() {
        return observationRating == ObservationRating.EXCELLENT ||
               observationRating == ObservationRating.GOOD;
    }
}
