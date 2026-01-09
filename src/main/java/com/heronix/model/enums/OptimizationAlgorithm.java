package com.heronix.model.enums;

/**
 * Optimization Algorithm Enum
 * Defines available optimization algorithms
 *
 * Location: src/main/java/com/eduscheduler/model/enums/OptimizationAlgorithm.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
public enum OptimizationAlgorithm {

    /**
     * Genetic Algorithm - Evolution-based optimization
     * Best for: General scheduling, large search spaces
     */
    GENETIC_ALGORITHM(
        "Genetic Algorithm",
        "Evolution-based optimization using crossover and mutation",
        "🧬",
        AlgorithmType.EVOLUTIONARY
    ),

    /**
     * Simulated Annealing - Probabilistic hill climbing
     * Best for: Finding global optima, avoiding local minima
     */
    SIMULATED_ANNEALING(
        "Simulated Annealing",
        "Probabilistic technique inspired by metallurgy",
        "🔥",
        AlgorithmType.LOCAL_SEARCH
    ),

    /**
     * Tabu Search - Memory-based local search
     * Best for: Quick improvements, avoiding cycles
     */
    TABU_SEARCH(
        "Tabu Search",
        "Local search with memory to avoid revisiting solutions",
        "🔍",
        AlgorithmType.LOCAL_SEARCH
    ),

    /**
     * Hill Climbing - Greedy local search
     * Best for: Quick solutions, simple problems
     */
    HILL_CLIMBING(
        "Hill Climbing",
        "Simple greedy algorithm that always improves",
        "⛰️",
        AlgorithmType.LOCAL_SEARCH
    ),

    /**
     * Constraint Programming - Logic-based approach
     * Best for: Hard constraints, proof of optimality
     */
    CONSTRAINT_PROGRAMMING(
        "Constraint Programming",
        "Logic programming approach to constraint satisfaction",
        "🧩",
        AlgorithmType.EXACT
    ),

    /**
     * Hybrid - Combines multiple algorithms
     * Best for: Complex problems, best overall results
     */
    HYBRID(
        "Hybrid Algorithm",
        "Combines genetic algorithm with local search",
        "🌟",
        AlgorithmType.HYBRID
    );

    // ========================================================================
    // ENUM FIELDS
    // ========================================================================

    private final String displayName;
    private final String description;
    private final String icon;
    private final AlgorithmType type;

    // ========================================================================
    // ALGORITHM TYPES
    // ========================================================================

    public enum AlgorithmType {
        EVOLUTIONARY("Evolutionary", "Population-based algorithms"),
        LOCAL_SEARCH("Local Search", "Neighborhood-based algorithms"),
        EXACT("Exact", "Guarantees optimal solution"),
        HYBRID("Hybrid", "Combines multiple approaches");

        private final String displayName;
        private final String description;

        AlgorithmType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    OptimizationAlgorithm(String displayName, String description, String icon, AlgorithmType type) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.type = type;
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public AlgorithmType getType() {
        return type;
    }

    /**
     * Get recommended population size for evolutionary algorithms
     */
    public int getRecommendedPopulationSize() {
        return switch (this) {
            case GENETIC_ALGORITHM -> 100;
            case HYBRID -> 50;
            default -> 1;
        };
    }

    /**
     * Get recommended number of generations/iterations
     */
    public int getRecommendedIterations() {
        return switch (this) {
            case GENETIC_ALGORITHM -> 1000;
            case SIMULATED_ANNEALING -> 10000;
            case TABU_SEARCH -> 5000;
            case HILL_CLIMBING -> 1000;
            case CONSTRAINT_PROGRAMMING -> 100;
            case HYBRID -> 500;
        };
    }

    /**
     * Check if algorithm uses population
     */
    public boolean usesPopulation() {
        return type == AlgorithmType.EVOLUTIONARY || type == AlgorithmType.HYBRID;
    }

    /**
     * Check if algorithm supports parallel execution
     */
    public boolean supportsParallelExecution() {
        return this == GENETIC_ALGORITHM || this == HYBRID;
    }

    @Override
    public String toString() {
        return icon + " " + displayName;
    }
}
