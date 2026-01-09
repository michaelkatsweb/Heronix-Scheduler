package com.heronix.exception;

/**
 * Exception thrown when scheduling conflicts are detected
 * Location: src/main/java/com/eduscheduler/exception/ConflictException.java
 * 
 * Examples:
 * - Teacher double-booked
 * - Room double-booked
 * - Student schedule conflicts
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}