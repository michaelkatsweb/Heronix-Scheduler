// FILE: ScheduleConflictException.java
// LOCATION: src/main/java/com/eduscheduler/exception/ScheduleConflictException.java
package com.heronix.exception;

/**
 * Exception thrown when a scheduling conflict is detected
 */
public class ScheduleConflictException extends Exception {

    public ScheduleConflictException(String message) {
        super(message);
    }

    public ScheduleConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
