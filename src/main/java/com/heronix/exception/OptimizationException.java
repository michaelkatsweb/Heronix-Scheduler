// Location: src/main/java/com/eduscheduler/exception/OptimizationException.java
package com.heronix.exception;

/**
 * Exception thrown when AI schedule optimization fails
 * Location: src/main/java/com/eduscheduler/exception/OptimizationException.java
 */
public class OptimizationException extends RuntimeException {

    public OptimizationException(String message) {
        super(message);
    }

    public OptimizationException(String message, Throwable cause) {
        super(message, cause);
    }
}