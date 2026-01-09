package com.heronix.exception;

/**
 * Custom exception for import operations
 * Location: src/main/java/com/eduscheduler/exception/ImportException.java
 */
public class ImportException extends Exception {
    
    public ImportException(String message) {
        super(message);
    }
    
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ImportException(Throwable cause) {
        super(cause);
    }
}
