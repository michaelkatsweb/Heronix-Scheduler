package com.heronix.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO representing a time block when a teacher is unavailable
 * Used for JSON serialization/deserialization in Teacher.unavailableTimes field
 *
 * Phase 6A: Teacher Availability Constraints
 * Location: src/main/java/com/eduscheduler/model/dto/UnavailableTimeBlock.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0 - Phase 6A
 * @since December 2, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnavailableTimeBlock {

    /**
     * Day of week when teacher is unavailable
     */
    private DayOfWeek dayOfWeek;

    /**
     * Start time of unavailable block
     * Format: HH:mm:ss
     */
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /**
     * End time of unavailable block
     * Format: HH:mm:ss
     */
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    /**
     * Optional reason for unavailability
     * Examples: "Department Meeting", "IEP Meetings", "Professional Development"
     */
    private String reason;

    /**
     * Whether this unavailability recurs weekly
     * Default: true (most common case)
     */
    private boolean recurring = true;

    /**
     * Check if a given time falls within this unavailable block
     *
     * @param day Day of week to check
     * @param time Time to check
     * @return true if the time falls within this unavailable block
     */
    public boolean contains(DayOfWeek day, LocalTime time) {
        if (this.dayOfWeek != day) {
            return false; // Wrong day
        }

        // Check if time is within the unavailable period
        // time >= startTime AND time < endTime
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    /**
     * Check if this block overlaps with another block
     *
     * @param other The other time block to check against
     * @return true if the blocks overlap
     */
    public boolean overlaps(UnavailableTimeBlock other) {
        if (this.dayOfWeek != other.dayOfWeek) {
            return false; // Different days don't overlap
        }

        // Blocks overlap if:
        // - this.start < other.end AND this.end > other.start
        return this.startTime.isBefore(other.endTime) &&
               this.endTime.isAfter(other.startTime);
    }

    /**
     * Get duration of this unavailable block in minutes
     *
     * @return Duration in minutes
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Get a human-readable display string
     *
     * @return Display string like "Monday 09:00-10:00 (Department Meeting)"
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(dayOfWeek.toString().substring(0, 1))
          .append(dayOfWeek.toString().substring(1).toLowerCase())
          .append(" ")
          .append(startTime.toString())
          .append("-")
          .append(endTime.toString());

        if (reason != null && !reason.isEmpty()) {
            sb.append(" (").append(reason).append(")");
        }

        return sb.toString();
    }

    /**
     * Validate this time block
     *
     * @return true if valid
     * @throws IllegalArgumentException if invalid
     */
    public boolean validate() {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Day of week cannot be null");
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                String.format("Start time (%s) must be before end time (%s)",
                    startTime, endTime));
        }

        return true;
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}
