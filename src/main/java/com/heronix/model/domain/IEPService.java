package com.heronix.model.domain;

import com.heronix.model.enums.DeliveryModel;
import com.heronix.model.enums.ServiceStatus;
import com.heronix.model.enums.ServiceType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * IEP Service Entity
 *
 * Tracks specific services required by an IEP (speech therapy, OT, PT, counseling, etc.)
 * Each service defines required minutes per week and how it should be delivered.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Entity
@Table(name = "iep_services",
       indexes = {
           @Index(name = "idx_iepservice_iep", columnList = "iep_id"),
           @Index(name = "idx_iepservice_type", columnList = "service_type"),
           @Index(name = "idx_iepservice_status", columnList = "status"),
           @Index(name = "idx_iepservice_staff", columnList = "assigned_staff_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IEPService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The IEP this service belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iep_id", nullable = false)
    private IEP iep;

    /**
     * Type of service
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    private ServiceType serviceType;

    /**
     * Detailed service description
     */
    @Column(name = "service_description", columnDefinition = "TEXT")
    private String serviceDescription;

    /**
     * Required minutes per week (mandated by IEP)
     */
    @Column(name = "minutes_per_week", nullable = false)
    private Integer minutesPerWeek;

    /**
     * Typical session duration in minutes
     * Example: 30 minutes per session, twice per week
     */
    @Column(name = "session_duration_minutes", nullable = false)
    private Integer sessionDurationMinutes;

    /**
     * Frequency description
     * Examples: "2x per week", "3x per week", "Daily", "Monthly"
     */
    @Column(name = "frequency", length = 100)
    private String frequency;

    /**
     * How the service is delivered
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_model", nullable = false, length = 30)
    private DeliveryModel deliveryModel;

    /**
     * Preferred location for service delivery
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * SPED staff member assigned to provide this service
     * (Optional - may be assigned during scheduling)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private Teacher assignedStaff;

    /**
     * Current status of this service
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceStatus status = ServiceStatus.NOT_SCHEDULED;

    /**
     * Scheduled pull-out sessions for this service
     */
    @OneToMany(mappedBy = "iepService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PullOutSchedule> pullOutSchedules = new ArrayList<>();

    /**
     * When this service record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this service record was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this service is fully scheduled
     */
    public boolean isScheduled() {
        return status == ServiceStatus.SCHEDULED;
    }

    /**
     * Get student for this service (convenience method)
     */
    public Student getStudent() {
        return iep != null ? iep.getStudent() : null;
    }

    /**
     * Calculate total scheduled minutes per week across all pull-out schedules
     */
    public int getScheduledMinutesPerWeek() {
        if (pullOutSchedules == null || pullOutSchedules.isEmpty()) {
            return 0;
        }
        return pullOutSchedules.stream()
            .filter(ps -> ps.getStatus().isActive())
            .mapToInt(PullOutSchedule::getDurationMinutes)
            .sum();
    }

    /**
     * Check if scheduled minutes meet IEP requirements
     */
    public boolean meetsMinutesRequirement() {
        return getScheduledMinutesPerWeek() >= minutesPerWeek;
    }

    /**
     * Get compliance percentage (scheduled vs required)
     */
    public double getCompliancePercentage() {
        if (minutesPerWeek == null || minutesPerWeek == 0) {
            return 100.0;
        }
        int scheduled = getScheduledMinutesPerWeek();
        return (scheduled * 100.0) / minutesPerWeek;
    }

    /**
     * Add a pull-out schedule to this service
     */
    public void addPullOutSchedule(PullOutSchedule schedule) {
        pullOutSchedules.add(schedule);
        schedule.setIepService(this);
    }

    /**
     * Remove a pull-out schedule from this service
     */
    public void removePullOutSchedule(PullOutSchedule schedule) {
        pullOutSchedules.remove(schedule);
        schedule.setIepService(null);
    }

    /**
     * Get display string for this service
     */
    public String getDisplayString() {
        return String.format("%s - %d min/week (%s) - %s",
            serviceType.getDisplayName(),
            minutesPerWeek,
            deliveryModel.getDisplayName(),
            status.getDisplayName());
    }

    @Override
    public String toString() {
        return String.format("IEPService{id=%d, type=%s, minutes=%d/week, status=%s, schedules=%d}",
            id,
            serviceType,
            minutesPerWeek,
            status,
            pullOutSchedules.size());
    }
}
