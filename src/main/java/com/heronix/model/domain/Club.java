package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Club Entity
 *
 * Represents an after-school club or extracurricular activity.
 * Tracks memberships, meeting schedules, and capacity.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-08
 */
@Entity
@Table(name = "clubs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Club name (must be unique)
     */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * Club description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Club category
     * Examples: Academic, Sports, Arts, Service, Leadership, STEM, Music
     */
    @Column(name = "category")
    private String category;

    /**
     * Name of club advisor/teacher
     */
    @Column(name = "advisor_name")
    private String advisorName;

    /**
     * Teacher who advises this club
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_id")
    private Teacher advisor;

    /**
     * Day of the week for meetings
     * Examples: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
     */
    @Column(name = "meeting_day")
    private String meetingDay;

    /**
     * Meeting start time
     */
    @Column(name = "meeting_time")
    private LocalTime meetingTime;

    /**
     * Meeting duration in minutes
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * Meeting location
     */
    @Column(name = "location")
    private String location;

    /**
     * Maximum capacity
     */
    @Column(name = "max_capacity")
    private Integer maxCapacity;

    /**
     * Current enrollment count
     */
    @Column(name = "current_enrollment")
    @Builder.Default
    private Integer currentEnrollment = 0;

    /**
     * Whether club is currently active
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Whether club requires approval to join
     */
    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    /**
     * Start date (for seasonal clubs)
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * End date (for seasonal clubs)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Next scheduled meeting date
     */
    @Column(name = "next_meeting_date")
    private LocalDate nextMeetingDate;

    /**
     * Additional notes
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Sync status for Teacher Portal sync
     */
    @Column(name = "sync_status")
    @Builder.Default
    private String syncStatus = "synced";

    /**
     * Students who are members of this club
     */
    @ManyToMany
    @JoinTable(
        name = "club_memberships",
        joinColumns = @JoinColumn(name = "club_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private Set<Student> members = new HashSet<>();

    /**
     * Created timestamp
     */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Updated timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if club is at capacity
     */
    public boolean isAtCapacity() {
        if (maxCapacity == null) return false;
        return currentEnrollment != null && currentEnrollment >= maxCapacity;
    }

    /**
     * Get available spots
     */
    public Integer getAvailableSpots() {
        if (maxCapacity == null) return null;
        if (currentEnrollment == null) return maxCapacity;
        return Math.max(0, maxCapacity - currentEnrollment);
    }

    /**
     * Check if club is active on a given date
     */
    public boolean isActiveOnDate(LocalDate date) {
        if (!Boolean.TRUE.equals(active)) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;
        return true;
    }

    /**
     * Add a student to this club
     */
    public boolean addMember(Student student) {
        if (isAtCapacity()) return false;
        boolean added = members.add(student);
        if (added) {
            currentEnrollment = members.size();
        }
        return added;
    }

    /**
     * Remove a student from this club
     */
    public boolean removeMember(Student student) {
        boolean removed = members.remove(student);
        if (removed) {
            currentEnrollment = members.size();
        }
        return removed;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
