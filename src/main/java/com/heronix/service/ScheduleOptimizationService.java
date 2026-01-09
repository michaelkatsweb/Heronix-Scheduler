
package com.heronix.service;

import com.heronix.model.domain.Schedule;
import com.heronix.model.dto.ScheduleRequest;

/**
 * Interface for schedule optimization using AI/ML
 */
public interface ScheduleOptimizationService {

    /**
     * Optimizes a schedule using AI constraint solving
     */
    Schedule optimizeSchedule(ScheduleRequest request);

    /**
     * Applies Kanban principles to the schedule
     */
    void applyKanbanPrinciples(Schedule schedule);

    /**
     * Applies Eisenhower Matrix prioritization
     */
    void applyEisenhowerMatrix(Schedule schedule);

    /**
     * Applies Lean Six Sigma principles
     */
    void applyLeanSixSigma(Schedule schedule);

    /**
     * Trains ML model from historical data
     */
    void trainMLModel(java.util.List<Schedule> historicalSchedules);

    /**
     * Calculates optimization score for a schedule
     */
    double calculateOptimizationScore(Schedule schedule);
}