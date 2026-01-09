package com.heronix.model.domain;

import com.heronix.model.enums.ConstraintType;
import com.heronix.model.enums.OptimizationAlgorithm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Optimization Configuration Entity
 * Stores settings for schedule optimization
 *
 * Location: src/main/java/com/eduscheduler/model/domain/OptimizationConfig.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
@Entity
@Table(name = "optimization_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration name
     */
    @Column(name = "config_name", nullable = false)
    private String configName;

    /**
     * Configuration description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ========================================================================
    // ALGORITHM SETTINGS
    // ========================================================================

    /**
     * Optimization algorithm to use
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false)
    @Builder.Default
    private OptimizationAlgorithm algorithm = OptimizationAlgorithm.GENETIC_ALGORITHM;

    /**
     * Population size (for evolutionary algorithms)
     */
    @Column(name = "population_size")
    @Builder.Default
    private Integer populationSize = 100;

    /**
     * Number of generations/iterations
     */
    @Column(name = "max_generations")
    @Builder.Default
    private Integer maxGenerations = 1000;

    /**
     * Mutation rate (0.0 to 1.0)
     */
    @Column(name = "mutation_rate")
    @Builder.Default
    private Double mutationRate = 0.1;

    /**
     * Crossover rate (0.0 to 1.0)
     */
    @Column(name = "crossover_rate")
    @Builder.Default
    private Double crossoverRate = 0.8;

    /**
     * Elite size (number of best solutions to keep)
     */
    @Column(name = "elite_size")
    @Builder.Default
    private Integer eliteSize = 5;

    /**
     * Tournament size for selection
     */
    @Column(name = "tournament_size")
    @Builder.Default
    private Integer tournamentSize = 5;

    // ========================================================================
    // TERMINATION CRITERIA
    // ========================================================================

    /**
     * Maximum runtime in seconds
     */
    @Column(name = "max_runtime_seconds")
    @Builder.Default
    private Integer maxRuntimeSeconds = 300; // 5 minutes

    /**
     * Stop if no improvement for N generations
     */
    @Column(name = "stagnation_limit")
    @Builder.Default
    private Integer stagnationLimit = 100;

    /**
     * Target fitness score (stop if reached)
     */
    @Column(name = "target_fitness")
    private Double targetFitness;

    // ========================================================================
    // CONSTRAINT WEIGHTS
    // ========================================================================

    /**
     * Custom constraint weights (stored as JSON)
     * Maps ConstraintType to weight multiplier
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "optimization_constraint_weights",
                     joinColumns = @JoinColumn(name = "config_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "constraint_type")
    @Column(name = "weight")
    @Builder.Default
    private Map<ConstraintType, Integer> constraintWeights = new HashMap<>();

    // ========================================================================
    // OPTIMIZATION PREFERENCES
    // ========================================================================

    /**
     * Use parallel processing
     */
    @Column(name = "use_parallel_processing")
    @Builder.Default
    private Boolean useParallelProcessing = true;

    /**
     * Number of threads for parallel processing
     */
    @Column(name = "thread_count")
    @Builder.Default
    private Integer threadCount = 4;

    /**
     * Log progress every N generations
     */
    @Column(name = "log_frequency")
    @Builder.Default
    private Integer logFrequency = 10;

    /**
     * Save intermediate results
     */
    @Column(name = "save_intermediate_results")
    @Builder.Default
    private Boolean saveIntermediateResults = false;

    // ========================================================================
    // SCHEDULE PREFERENCES
    // ========================================================================

    /**
     * Start time for classes (e.g., "08:00")
     */
    @Column(name = "start_time")
    @Builder.Default
    private String startTime = "08:00";

    /**
     * End time for classes (e.g., "15:00")
     */
    @Column(name = "end_time")
    @Builder.Default
    private String endTime = "15:00";

    /**
     * Class period duration in minutes
     */
    @Column(name = "period_duration_minutes")
    @Builder.Default
    private Integer periodDurationMinutes = 50;

    /**
     * Break between periods in minutes
     */
    @Column(name = "break_duration_minutes")
    @Builder.Default
    private Integer breakDurationMinutes = 10;

    /**
     * Lunch break duration in minutes
     */
    @Column(name = "lunch_duration_minutes")
    @Builder.Default
    private Integer lunchDurationMinutes = 30;

    /**
     * Lunch break start time (e.g., "12:00")
     */
    @Column(name = "lunch_start_time")
    @Builder.Default
    private String lunchStartTime = "12:00";

    // ========================================================================
    // METADATA
    // ========================================================================

    /**
     * Is this the default configuration?
     */
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Configuration creator
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Last modified timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get weight for a constraint (returns default if not customized)
     */
    public int getConstraintWeight(ConstraintType type) {
        return constraintWeights.getOrDefault(type, type.getDefaultWeight());
    }

    /**
     * Set constraint weight
     */
    public void setConstraintWeight(ConstraintType type, int weight) {
        if (constraintWeights == null) {
            constraintWeights = new HashMap<>();
        }
        constraintWeights.put(type, weight);
    }

    /**
     * Reset all constraint weights to defaults
     */
    public void resetConstraintWeights() {
        constraintWeights.clear();
    }

    /**
     * Get total number of time slots per day
     */
    public int getTotalSlotsPerDay() {
        int totalMinutes = calculateMinutes(endTime) - calculateMinutes(startTime);
        int slotDuration = periodDurationMinutes + breakDurationMinutes;
        return totalMinutes / slotDuration;
    }

    /**
     * Check if configuration is valid
     */
    public boolean isValid() {
        return configName != null && !configName.trim().isEmpty() &&
               algorithm != null &&
               populationSize != null && populationSize > 0 &&
               maxGenerations != null && maxGenerations > 0 &&
               mutationRate != null && mutationRate >= 0 && mutationRate <= 1 &&
               crossoverRate != null && crossoverRate >= 0 && crossoverRate <= 1;
    }

    /**
     * Get configuration summary
     */
    public String getSummary() {
        return String.format("%s: %s with population=%d, generations=%d",
            configName, algorithm.getDisplayName(), populationSize, maxGenerations);
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private int calculateMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
