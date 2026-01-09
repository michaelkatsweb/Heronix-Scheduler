package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * District Entity
 * Represents a school district containing multiple campuses for federation.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - Multi-Campus Federation
 */
@Entity
@Table(name = "districts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String districtCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String email;
    private String website;

    @Column(name = "superintendent_name")
    private String superintendentName;

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Campus> campuses = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "fiscal_year")
    private String fiscalYear;

    @Column(name = "calendar_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CalendarType calendarType = CalendarType.TRADITIONAL;

    public enum CalendarType {
        TRADITIONAL,      // Aug/Sep - May/Jun
        YEAR_ROUND,       // Continuous with breaks
        BALANCED,         // Modified year-round
        EARLY_START,      // Early August start
        TRIMESTER,        // Three terms
        QUARTER           // Four terms
    }

    // Federation settings
    @Column(name = "allow_cross_campus_enrollment")
    @Builder.Default
    private Boolean allowCrossCampusEnrollment = true;

    @Column(name = "allow_shared_teachers")
    @Builder.Default
    private Boolean allowSharedTeachers = true;

    @Column(name = "centralized_scheduling")
    @Builder.Default
    private Boolean centralizedScheduling = false;

    // Calculated fields
    @Transient
    public int getTotalCampuses() {
        return campuses != null ? campuses.size() : 0;
    }

    @Transient
    public int getTotalStudents() {
        if (campuses == null) return 0;
        return campuses.stream()
            .mapToInt(Campus::getCurrentEnrollment)
            .sum();
    }

    @Transient
    public int getTotalTeachers() {
        if (campuses == null) return 0;
        return campuses.stream()
            .mapToInt(c -> c.getTeachers() != null ? c.getTeachers().size() : 0)
            .sum();
    }
}
