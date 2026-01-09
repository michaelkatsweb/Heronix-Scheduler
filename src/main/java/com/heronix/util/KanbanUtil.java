package com.heronix.util;

import com.heronix.model.domain.*;
import com.heronix.model.enums.ScheduleStatus;
import java.util.*;

/**
 * Kanban Utility Methods
 * Location: src/main/java/com/eduscheduler/util/KanbanUtil.java
 * 
 * Applies Kanban principles:
 * - Visualize workflow
 * - Limit work in progress (WIP)
 * - Manage flow
 * - Make policies explicit
 * - Improve collaboratively
 */
public class KanbanUtil {

    // WIP limits for different stages
    private static final int WIP_LIMIT_DRAFT = 5;
    private static final int WIP_LIMIT_IN_PROGRESS = 3;
    private static final int WIP_LIMIT_REVIEW = 2;

    /**
     * Calculate Work In Progress (WIP) for schedules
     */
    public static int calculateWIP(List<Schedule> schedules) {
        return (int) schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.DRAFT ||
                             s.getStatus() == ScheduleStatus.IN_PROGRESS ||
                             s.getStatus() == ScheduleStatus.REVIEW)
                .count();
    }

    /**
     * Check if WIP limit is exceeded
     */
    public static boolean isWIPLimitExceeded(List<Schedule> schedules, ScheduleStatus status) {
        long count = schedules.stream()
                .filter(s -> s.getStatus() == status)
                .count();

        return switch (status) {
            case DRAFT -> count > WIP_LIMIT_DRAFT;
            case IN_PROGRESS -> count > WIP_LIMIT_IN_PROGRESS;
            case REVIEW -> count > WIP_LIMIT_REVIEW;
            default -> false;
        };
    }

    /**
     * Calculate Cycle Time (time from start to completion)
     */
    public static double calculateScheduleCycleTime(Schedule schedule) {
        if (schedule.getCreatedDate() == null || schedule.getLastModifiedDate() == null) {
            return 0.0;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(
                schedule.getCreatedDate(),
                schedule.getLastModifiedDate());

        return days;
    }

    /**
     * Calculate Lead Time (time from request to delivery)
     */
    public static double calculateLeadTime(Schedule schedule) {
        return calculateScheduleCycleTime(schedule);
    }

    /**
     * Calculate Throughput (completed schedules per time period)
     */
    public static double calculateThroughput(List<Schedule> schedules, int days) {
        if (days <= 0)
            return 0.0;

        long completed = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.PUBLISHED ||
                             s.getStatus() == ScheduleStatus.ARCHIVED)
                .count();

        return (double) completed / days;
    }

    /**
     * Identify bottlenecks in the scheduling process
     */
    public static List<String> identifyBottlenecks(List<Schedule> schedules) {
        List<String> bottlenecks = new ArrayList<>();

        Map<ScheduleStatus, Long> statusCounts = schedules.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Schedule::getStatus,
                        java.util.stream.Collectors.counting()));

        for (Map.Entry<ScheduleStatus, Long> entry : statusCounts.entrySet()) {
            if (entry.getValue() > getWIPLimit(entry.getKey())) {
                bottlenecks.add(String.format(
                        "Bottleneck in %s stage: %d schedules (limit: %d)",
                        entry.getKey(),
                        entry.getValue(),
                        getWIPLimit(entry.getKey())));
            }
        }

        return bottlenecks;
    }

    /**
     * Get WIP limit for a status
     */
    private static int getWIPLimit(ScheduleStatus status) {
        return switch (status) {
            case DRAFT -> WIP_LIMIT_DRAFT;
            case IN_PROGRESS -> WIP_LIMIT_IN_PROGRESS;
            case REVIEW -> WIP_LIMIT_REVIEW;
            default -> Integer.MAX_VALUE;
        };
    }

    /**
     * Calculate Flow Efficiency
     * Flow Efficiency = (Active Work Time / Total Cycle Time) × 100
     *
     * In scheduling context:
     * - Active time = time spent actually scheduling (estimated from assigned slots)
     * - Total time = cycle time from creation to completion
     *
     * @param schedule the schedule to analyze
     * @return flow efficiency percentage (0-100)
     */
    public static double calculateFlowEfficiency(Schedule schedule) {
        double cycleTime = calculateScheduleCycleTime(schedule);
        if (cycleTime == 0) {
            return 0.0;
        }

        // Estimate active work time based on schedule completion
        // Fully assigned slots indicate active work was done
        long totalSlots = schedule.getSlots().size();
        if (totalSlots == 0) {
            return 0.0;
        }

        long assignedSlots = schedule.getSlots().stream()
            .filter(slot -> slot.getTeacher() != null &&
                           slot.getRoom() != null &&
                           slot.getCourse() != null)
            .count();

        // Calculate completion ratio (0.0 to 1.0)
        double completionRatio = (double) assignedSlots / totalSlots;

        // Estimate active work time as a percentage of cycle time
        // Assume high completion = more active work relative to wait time
        // Base efficiency starts at 20% (minimum productive time)
        // Maximum efficiency is 85% (some overhead is unavoidable)
        double baseEfficiency = 20.0;
        double maxEfficiency = 85.0;

        // Scale efficiency based on completion and quality
        double qualityBonus = schedule.getQualityScore() != null ?
            schedule.getQualityScore() * 10.0 : 0.0;

        double flowEfficiency = baseEfficiency +
            (completionRatio * (maxEfficiency - baseEfficiency)) +
            qualityBonus;

        // Clamp to reasonable range
        return Math.min(100.0, Math.max(0.0, flowEfficiency));
    }

    /**
     * Apply pull-based scheduling
     */
    public static boolean canCreateNewSchedule(List<Schedule> schedules) {
        int currentWIP = calculateWIP(schedules);
        return currentWIP < WIP_LIMIT_DRAFT;
    }

    /**
     * Calculate cumulative flow metrics
     */
    public static Map<String, Object> calculateCumulativeFlow(List<Schedule> schedules) {
        Map<String, Object> metrics = new HashMap<>();

        long draft = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.DRAFT)
                .count();

        long inProgress = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.IN_PROGRESS)
                .count();

        long review = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.REVIEW)
                .count();

        long published = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.PUBLISHED)
                .count();

        metrics.put("draft", draft);
        metrics.put("inProgress", inProgress);
        metrics.put("review", review);
        metrics.put("published", published);
        metrics.put("totalWIP", draft + inProgress + review);

        return metrics;
    }

    /**
     * Generate Kanban board visualization data
     */
    public static Map<String, List<Schedule>> generateKanbanBoard(List<Schedule> schedules) {
        Map<String, List<Schedule>> board = new LinkedHashMap<>();

        board.put("Backlog", schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.DRAFT)
                .toList());

        board.put("In Progress", schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.IN_PROGRESS)
                .toList());

        board.put("Review", schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.REVIEW)
                .toList());

        board.put("Published", schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.PUBLISHED)
                .toList());

        board.put("Archived", schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.ARCHIVED)
                .toList());

        return board;
    }

    /**
     * Calculate blocked time
     */
    public static List<Schedule> getBlockedSchedules(List<Schedule> schedules, int dayThreshold) {
        return schedules.stream()
                .filter(schedule -> {
                    if (schedule.getLastModifiedDate() == null)
                        return false;

                    long daysSinceModified = java.time.temporal.ChronoUnit.DAYS.between(
                            schedule.getLastModifiedDate(),
                            java.time.LocalDate.now());

                    return daysSinceModified > dayThreshold &&
                            schedule.getStatus() != ScheduleStatus.PUBLISHED &&
                            schedule.getStatus() != ScheduleStatus.ARCHIVED;
                })
                .toList();
    }
}
