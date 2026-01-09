// Location: src/main/java/com/eduscheduler/util/LeanSixSigmaUtil.java
package com.heronix.util;

import com.heronix.model.domain.Schedule;
import com.heronix.model.domain.ScheduleSlot;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lean Six Sigma Utility Methods
 * Location: src/main/java/com/eduscheduler/util/LeanSixSigmaUtil.java
 * 
 * Implements DMAIC process:
 * - Define: Identify scheduling problems
 * - Measure: Calculate key metrics
 * - Analyze: Root cause analysis
 * - Improve: Optimization suggestions
 * - Control: Monitor performance
 */
public class LeanSixSigmaUtil {

    /**
     * Calculate Cycle Time (average time to schedule a course)
     * Measures the time from schedule creation to last modification per slot
     *
     * @param schedule the schedule to analyze
     * @return average minutes per course slot, or 0.0 if unable to calculate
     */
    public static double calculateCycleTime(Schedule schedule) {
        if (schedule.getCreatedDate() == null || schedule.getLastModifiedDate() == null) {
            return 0.0;
        }

        long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(
            schedule.getCreatedDate().atStartOfDay(),
            schedule.getLastModifiedDate().atStartOfDay()
        );

        int slotCount = schedule.getSlotCount();
        if (slotCount == 0) {
            return 0.0;
        }

        // Average minutes per slot scheduled
        return (double) totalMinutes / slotCount;
    }

    /**
     * Calculate Lead Time (total scheduling process time)
     * Measures total elapsed time from schedule creation to completion/current state
     *
     * @param schedule the schedule to analyze
     * @return total hours from creation to last modification, or 0.0 if unable to calculate
     */
    public static double calculateLeadTime(Schedule schedule) {
        if (schedule.getCreatedDate() == null || schedule.getLastModifiedDate() == null) {
            return 0.0;
        }

        long totalHours = java.time.temporal.ChronoUnit.HOURS.between(
            schedule.getCreatedDate().atStartOfDay(),
            schedule.getLastModifiedDate().atStartOfDay()
        );

        return (double) totalHours;
    }

    /**
     * Calculate Value-Added Ratio
     * (Productive time / Total time)
     */
    public static double calculateValueAddedRatio(Schedule schedule) {
        long totalSlots = schedule.getSlots().size();
        if (totalSlots == 0)
            return 0.0;

        long valueAddedSlots = schedule.getSlots().stream()
                .filter(slot -> slot.getTeacher() != null &&
                        slot.getRoom() != null &&
                        slot.getTimeSlot() != null)
                .count();

        return (valueAddedSlots * 100.0) / totalSlots;
    }

    /**
     * Calculate Defect Rate (conflicts per total slots)
     */
    public static double calculateDefectRate(Schedule schedule) {
        long totalSlots = schedule.getSlots().size();
        if (totalSlots == 0)
            return 0.0;

        long defects = schedule.getSlots().stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getHasConflict()))
                .count();

        return (defects * 100.0) / totalSlots;
    }

    /**
     * Calculate Process Capability (Cp)
     * Higher is better (>1.33 is good)
     */
    public static double calculateProcessCapability(Schedule schedule) {
        double defectRate = calculateDefectRate(schedule);
        double processCapability = 100.0 - defectRate;

        // Normalize to capability index (0-2 scale)
        return processCapability / 50.0;
    }

    /**
     * Calculate Overall Equipment Effectiveness (OEE)
     * OEE = Availability × Performance × Quality
     */
    public static double calculateOEE(Schedule schedule) {
        long totalSlots = schedule.getSlots().size();
        if (totalSlots == 0)
            return 0.0;

        // Availability: % of slots without conflicts
        long conflictSlots = schedule.getSlots().stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getHasConflict()))
                .count();
        double availability = ((totalSlots - conflictSlots) * 100.0) / totalSlots;

        // Performance: Efficiency rate
        double performance = schedule.getEfficiencyRate();

        // Quality: % of properly assigned slots
        long qualitySlots = schedule.getSlots().stream()
                .filter(slot -> slot.getTeacher() != null &&
                        slot.getRoom() != null &&
                        !Boolean.TRUE.equals(slot.getHasConflict()))
                .count();
        double quality = (qualitySlots * 100.0) / totalSlots;

        // OEE formula
        return (availability * performance * quality) / 10000.0;
    }

    /**
     * Calculate First Pass Yield (FPY)
     * % of schedules created right the first time
     */
    public static double calculateFirstPassYield(Schedule schedule) {
        long totalSlots = schedule.getSlots().size();
        if (totalSlots == 0)
            return 100.0;

        long correctSlots = schedule.getSlots().stream()
                .filter(slot -> !Boolean.TRUE.equals(slot.getHasConflict()) &&
                        slot.getTeacher() != null &&
                        slot.getRoom() != null)
                .count();

        return (correctSlots * 100.0) / totalSlots;
    }

    /**
     * Identify the 8 Wastes (DOWNTIME)
     * D - Defects
     * O - Overproduction
     * W - Waiting
     * N - Non-utilized talent
     * T - Transportation
     * I - Inventory
     * M - Motion
     * E - Extra processing
     */
    public static Map<String, List<String>> identifyWastes(Schedule schedule) {
        Map<String, List<String>> wastes = new HashMap<>();

        // Defects
        List<String> defects = new ArrayList<>();
        schedule.getSlots().stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getHasConflict()))
                .forEach(slot -> defects.add("Conflict in slot: " + slot.getId()));
        wastes.put("Defects", defects);

        // Overproduction (over-scheduled teachers)
        List<String> overproduction = new ArrayList<>();
        // Would check teacher hours vs capacity
        wastes.put("Overproduction", overproduction);

        // Waiting (idle resources)
        List<String> waiting = new ArrayList<>();
        // Would identify unused time slots
        wastes.put("Waiting", waiting);

        // Non-utilized talent
        List<String> nonUtilized = new ArrayList<>();
        // Would check for underutilized teachers
        wastes.put("Non-utilized Talent", nonUtilized);

        return wastes;
    }

    /**
     * Calculate Sigma Level (quality metric)
     * 6 Sigma = 3.4 defects per million opportunities
     */
    public static double calculateSigmaLevel(Schedule schedule) {
        double defectRate = calculateDefectRate(schedule);

        if (defectRate >= 69.0)
            return 1.0;
        if (defectRate >= 30.0)
            return 2.0;
        if (defectRate >= 6.7)
            return 3.0;
        if (defectRate >= 0.62)
            return 4.0;
        if (defectRate >= 0.023)
            return 5.0;
        if (defectRate < 0.023)
            return 6.0;

        return 0.0;
    }

    /**
     * Generate improvement recommendations based on metrics
     */
    public static List<String> generateImprovementRecommendations(Schedule schedule) {
        List<String> recommendations = new ArrayList<>();

        double defectRate = calculateDefectRate(schedule);
        double oee = calculateOEE(schedule);
        double valueAddedRatio = calculateValueAddedRatio(schedule);

        if (defectRate > 5.0) {
            recommendations.add("High defect rate (" +
                    String.format("%.1f%%", defectRate) +
                    ") - Review constraint configuration");
        }

        if (oee < 70.0) {
            recommendations.add("Low OEE (" +
                    String.format("%.1f%%", oee) +
                    ") - Optimize resource allocation");
        }

        if (valueAddedRatio < 80.0) {
            recommendations.add("Low value-added ratio (" +
                    String.format("%.1f%%", valueAddedRatio) +
                    ") - Reduce non-productive slots");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Schedule meets Lean Six Sigma standards");
        }

        return recommendations;
    }

    /**
     * Calculate Takt Time (available time / customer demand)
     */
    public static double calculateTaktTime(int availableMinutesPerDay, int coursesPerDay) {
        if (coursesPerDay == 0)
            return 0.0;
        return (double) availableMinutesPerDay / coursesPerDay;
    }

    /**
     * Perform Pareto Analysis (80/20 rule)
     * Identify 20% of issues causing 80% of problems
     */
    public static Map<String, Integer> performParetoAnalysis(Schedule schedule) {
        Map<String, Integer> issues = new HashMap<>();

        // Count issues by type
        for (ScheduleSlot slot : schedule.getSlots()) {
            if (Boolean.TRUE.equals(slot.getHasConflict()) && slot.getConflictReason() != null) {
                String reason = slot.getConflictReason();
                issues.put(reason, issues.getOrDefault(reason, 0) + 1);
            }
        }

        // Sort by frequency (descending)
        return issues.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }
}