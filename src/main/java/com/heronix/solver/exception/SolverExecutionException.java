package com.heronix.solver.exception;

/**
 * Exception thrown when solver execution fails
 *
 * This exception is thrown when:
 * - OptaPlanner solver fails during execution
 * - Solver configuration is invalid
 * - Solver times out
 * - Solver is interrupted or cancelled
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
public class SolverExecutionException extends SchedulingSolverException {

    /**
     * Create exception with message
     *
     * @param message Detailed error message describing the solver failure
     */
    public SolverExecutionException(String message) {
        super(message);
    }

    /**
     * Create exception with message and cause
     *
     * @param message Detailed error message
     * @param cause   Root cause of the solver failure
     */
    public SolverExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
