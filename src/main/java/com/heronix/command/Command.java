package com.heronix.command;

/**
 * Command interface for implementing the Command pattern
 * Supports undo/redo functionality for schedule operations
 *
 * Location: src/main/java/com/eduscheduler/command/Command.java
 */
public interface Command {

    /**
     * Execute the command
     */
    void execute();

    /**
     * Undo the command (reverse the execute operation)
     */
    void undo();

    /**
     * Get a description of this command
     */
    String getDescription();

    /**
     * Check if this command can be undone
     */
    default boolean canUndo() {
        return true;
    }

    /**
     * Get the command type (for grouping/filtering)
     */
    default CommandType getType() {
        return CommandType.OTHER;
    }

    /**
     * Command types for categorization
     */
    enum CommandType {
        SECTION_ASSIGNMENT("Section Assignment"),
        TEACHER_ASSIGNMENT("Teacher Assignment"),
        ROOM_ASSIGNMENT("Room Assignment"),
        PERIOD_ASSIGNMENT("Period Assignment"),
        STUDENT_ENROLLMENT("Student Enrollment"),
        SCHEDULE_MODIFICATION("Schedule Modification"),
        BULK_OPERATION("Bulk Operation"),
        OTHER("Other");

        private final String displayName;

        CommandType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
