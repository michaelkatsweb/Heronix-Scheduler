package com.heronix.solver.exception;

/**
 * Exception thrown when scheduling problem input is invalid
 *
 * This exception is thrown when:
 * - Required data is missing (no teachers, rooms, time slots, etc.)
 * - Data is malformed (null values, empty lists when data is required)
 * - Constraints are impossible to satisfy (e.g., more classes than time slots)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
public class InvalidScheduleProblemException extends SchedulingSolverException {

    /**
     * Create exception with message
     *
     * @param message Detailed error message describing what is invalid
     */
    public InvalidScheduleProblemException(String message) {
        super(message);
    }

    /**
     * Create exception with message and cause
     *
     * @param message Detailed error message
     * @param cause   Root cause of the error
     */
    public InvalidScheduleProblemException(String message, Throwable cause) {
        super(message, cause);
    }
}
