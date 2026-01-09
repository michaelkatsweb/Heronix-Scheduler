package com.heronix.solver.exception;

/**
 * Exception thrown when a requested schedule cannot be found
 *
 * This exception is thrown when:
 * - Schedule ID does not exist in database
 * - Schedule has been deleted
 * - User does not have access to the schedule
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
public class ScheduleNotFoundException extends SchedulingSolverException {

    private final Long scheduleId;

    /**
     * Create exception with schedule ID
     *
     * @param scheduleId The ID of the schedule that was not found
     */
    public ScheduleNotFoundException(Long scheduleId) {
        super("Schedule not found with ID: " + scheduleId);
        this.scheduleId = scheduleId;
    }

    /**
     * Create exception with schedule ID and custom message
     *
     * @param scheduleId The ID of the schedule that was not found
     * @param message    Custom error message
     */
    public ScheduleNotFoundException(Long scheduleId, String message) {
        super(message);
        this.scheduleId = scheduleId;
    }

    /**
     * Get the schedule ID that was not found
     *
     * @return Schedule ID
     */
    public Long getScheduleId() {
        return scheduleId;
    }
}
