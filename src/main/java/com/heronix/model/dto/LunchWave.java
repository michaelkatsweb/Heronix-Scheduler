package com.heronix.model.dto;

import com.heronix.model.domain.Student;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a lunch wave with assigned cohorts
 *
 * Location: src/main/java/com/eduscheduler/model/dto/LunchWave.java
 *
 * Example:
 * Wave 1 (11:00-11:30): 334 students
 * ├─ Room 101 - Algebra I (28 students)
 * ├─ Room 102 - Geometry (30 students)
 * ├─ Room 103 - Pre-Calculus (24 students)
 * └─ ... (12 cohorts total)
 *
 * @author Heronix Scheduling System Team
 * @version 5.0.0
 * @since 2025-12-06
 */
@Data
public class LunchWave {

    /**
     * Wave number (1, 2, 3, etc.)
     */
    private int number;

    /**
     * Cohorts assigned to this wave
     */
    private List<LunchCohort> cohorts;

    /**
     * Total number of students in this wave
     */
    private int currentSize;

    /**
     * Primary room zones represented in this wave
     * Used for spatial grouping analysis
     */
    private List<String> roomZones;

    public LunchWave(int number) {
        this.number = number;
        this.cohorts = new ArrayList<>();
        this.currentSize = 0;
        this.roomZones = new ArrayList<>();
    }

    /**
     * Add a cohort to this wave
     */
    public void addCohort(LunchCohort cohort) {
        if (cohort == null) {
            return;
        }

        cohorts.add(cohort);
        currentSize += cohort.getSize();

        // Track room zones for spatial analysis
        if (cohort.getRoomZone() != null && !roomZones.contains(cohort.getRoomZone())) {
            roomZones.add(cohort.getRoomZone());
        }
    }

    /**
     * Get all students in this wave (from all cohorts)
     */
    public List<Student> getAllStudents() {
        return cohorts.stream()
                .flatMap(c -> c.getStudents().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get the number of cohorts in this wave
     */
    public int getCohortCount() {
        return cohorts.size();
    }

    /**
     * Get primary room zone (most common zone in this wave)
     * Used for spatial grouping reporting
     */
    public String getPrimaryRoomZone() {
        if (roomZones.isEmpty()) {
            return "Mixed";
        }

        // Count cohorts per zone
        return cohorts.stream()
                .filter(c -> c.getRoomZone() != null)
                .collect(Collectors.groupingBy(
                        LunchCohort::getRoomZone,
                        Collectors.counting()))
                .entrySet().stream()
                .max((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
                .map(e -> e.getKey())
                .orElse("Mixed");
    }

    /**
     * Calculate spatial cohesion score (0-100)
     * Higher score = more cohorts from same room zone
     */
    public double getSpatialCohesionScore() {
        if (cohorts.isEmpty()) {
            return 0.0;
        }

        long roomBasedCohorts = cohorts.stream()
                .filter(LunchCohort::isRoomBased)
                .count();

        if (roomBasedCohorts == 0) {
            return 0.0;
        }

        // Count how many cohorts are from the primary zone
        String primaryZone = getPrimaryRoomZone();
        if ("Mixed".equals(primaryZone)) {
            return 50.0; // Moderate cohesion
        }

        long cohortsInPrimaryZone = cohorts.stream()
                .filter(c -> primaryZone.equals(c.getRoomZone()))
                .count();

        return (double) cohortsInPrimaryZone / roomBasedCohorts * 100.0;
    }

    /**
     * Get detailed breakdown of this wave for logging
     */
    public String getDetailedBreakdown() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Wave %d: %d students from %d cohorts\n",
                number, currentSize, cohorts.size()));

        for (LunchCohort cohort : cohorts) {
            sb.append(String.format("  ├─ %s\n", cohort.toString()));
        }

        sb.append(String.format("  └─ Primary Zone: %s (Cohesion: %.1f%%)",
                getPrimaryRoomZone(), getSpatialCohesionScore()));

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Wave %d (%d students, %d cohorts, Primary Zone: %s)",
                number, currentSize, cohorts.size(), getPrimaryRoomZone());
    }
}
