package com.heronix.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Time Slot - Represents a specific time period in the schedule
 * Location: src/main/java/com/eduscheduler/model/domain/TimeSlot.java
 * 
 * Used by OptaPlanner as a value range for scheduling
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer periodNumber; // Optional: for period-based schedules

    // Phase 5C: Multiple Rotating Lunch Periods
    private String splitPeriodLabel;  // "A", "B", "C" for split periods (e.g., "4A", "4B", "4C")
    private String lunchWaveLabel;    // "Lunch 1", "Lunch 2", "Lunch 3" for lunch time slots

    /**
     * Constructor without period number
     */
    public TimeSlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodNumber = null;
    }

    /**
     * Constructor with period number (for ScheduleGenerationService)
     */
    public TimeSlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, int periodNumber) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodNumber = periodNumber;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    /**
     * Check if this time slot overlaps with another
     * 
     * @param other The other time slot
     * @return true if they overlap
     */
    public boolean overlapsWith(TimeSlot other) {
        if (other == null || this.dayOfWeek != other.dayOfWeek) {
            return false;
        }

        return this.startTime.isBefore(other.endTime) &&
                other.startTime.isBefore(this.endTime);
    }

    /**
     * Get duration in minutes
     * 
     * @return Duration in minutes
     */
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Format as readable string
     * 
     * @return Formatted string (e.g., "Monday 8:00 AM - 9:00 AM")
     */
    public String toDisplayString() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        String timeStr = String.format("%s %s - %s",
                dayOfWeek,
                startTime.format(timeFormatter),
                endTime.format(timeFormatter));

        if (periodNumber != null) {
            timeStr += " (Period " + periodNumber + ")";
        }

        return timeStr;
    }

    /**
     * Create a TimeSlot from a ScheduleSlot
     * 
     * @param slot The schedule slot
     * @return TimeSlot or null if slot has no time info
     */
    public static TimeSlot fromScheduleSlot(ScheduleSlot slot) {
        if (slot == null || slot.getDayOfWeek() == null ||
                slot.getStartTime() == null || slot.getEndTime() == null) {
            return null;
        }
        return new TimeSlot(
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPeriodNumber());
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}