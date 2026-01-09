package com.heronix.solver.exception;

/**
 * Base exception for scheduling solver errors
 *
 * This is the parent exception for all scheduling-related errors.
 * Provides a consistent error handling hierarchy for the solver module.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
public class SchedulingSolverException extends RuntimeException {

    /**
     * Create exception with message
     *
     * @param message Error message
     */
    public SchedulingSolverException(String message) {
        super(message);
    }

    /**
     * Create exception with message and cause
     *
     * @param message Error message
     * @param cause   Root cause
     */
    public SchedulingSolverException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create exception with cause only
     *
     * @param cause Root cause
     */
    public SchedulingSolverException(Throwable cause) {
        super(cause);
    }
}
