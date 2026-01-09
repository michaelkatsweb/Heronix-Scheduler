package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Campus Entity
 * Represents a school campus within a district for multi-campus federation.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 12 - Multi-Campus Federation
 */
@Entity
@Table(name = "campuses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String campusCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String email;

    @Column(name = "principal_name")
    private String principalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @OneToMany(mappedBy = "campus", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "primaryCampus", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Teacher> teachers = new ArrayList<>();

    @OneToMany(mappedBy = "campus", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Student> students = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "grade_levels")
    private String gradeLevels; // e.g., "K-5", "6-8", "9-12"

    @Enumerated(EnumType.STRING)
    @Column(name = "campus_type")
    @Builder.Default
    private CampusType campusType = CampusType.COMPREHENSIVE;

    public enum CampusType {
        ELEMENTARY,
        MIDDLE,
        HIGH,
        COMPREHENSIVE,
        ALTERNATIVE,
        MAGNET,
        VIRTUAL
    }

    // Calculated field for current enrollment
    @Transient
    public int getCurrentEnrollment() {
        return students != null ? students.size() : 0;
    }

    @Transient
    public double getCapacityUtilization() {
        if (maxStudents == null || maxStudents == 0) return 0;
        return (double) getCurrentEnrollment() / maxStudents * 100;
    }
}
