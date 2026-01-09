package com.heronix.exception;

/**
 * General schedule-related exception
 * Location: src/main/java/com/eduscheduler/exception/ScheduleException.java
 */
public class ScheduleException extends RuntimeException {

    public ScheduleException(String message) {
        super(message);
    }

    public ScheduleException(String message, Throwable cause) {
        super(message, cause);
    }
}