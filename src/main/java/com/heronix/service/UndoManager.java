package com.heronix.service;

import com.heronix.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Manages undo/redo operations for schedule modifications
 * Implements the Command pattern to support reversible operations
 *
 * Location: src/main/java/com/eduscheduler/service/UndoManager.java
 */
@Service
public class UndoManager {

    private static final Logger logger = LoggerFactory.getLogger(UndoManager.class);

    private static final int MAX_UNDO_HISTORY = 50; // Maximum number of commands to keep

    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    private final List<UndoRedoListener> listeners = new ArrayList<>();

    /**
     * Execute a command and add it to the undo stack
     */
    public void executeCommand(Command command) {
        logger.info("Executing command: {}", command.getDescription());

        try {
            command.execute();
            undoStack.push(command);
            redoStack.clear(); // Clear redo stack after new command

            // Limit undo history size
            if (undoStack.size() > MAX_UNDO_HISTORY) {
                undoStack.remove(0);
            }

            notifyListeners();
        } catch (Exception e) {
            logger.error("Command execution failed: {}", command.getDescription(), e);
            throw new RuntimeException("Failed to execute command: " + e.getMessage(), e);
        }
    }

    /**
     * Undo the last command
     */
    public void undo() {
        if (!canUndo()) {
            logger.warn("Cannot undo: no commands in undo stack");
            return;
        }

        Command command = undoStack.pop();
        logger.info("Undoing command: {}", command.getDescription());

        try {
            command.undo();
            redoStack.push(command);
            notifyListeners();
        } catch (Exception e) {
            logger.error("Command undo failed: {}", command.getDescription(), e);
            // Re-add to undo stack if undo fails
            undoStack.push(command);
            throw new RuntimeException("Failed to undo command: " + e.getMessage(), e);
        }
    }

    /**
     * Redo the last undone command
     */
    public void redo() {
        if (!canRedo()) {
            logger.warn("Cannot redo: no commands in redo stack");
            return;
        }

        Command command = redoStack.pop();
        logger.info("Redoing command: {}", command.getDescription());

        try {
            command.execute();
            undoStack.push(command);
            notifyListeners();
        } catch (Exception e) {
            logger.error("Command redo failed: {}", command.getDescription(), e);
            // Re-add to redo stack if redo fails
            redoStack.push(command);
            throw new RuntimeException("Failed to redo command: " + e.getMessage(), e);
        }
    }

    /**
     * Check if undo is available
     */
    public boolean canUndo() {
        return !undoStack.isEmpty() && undoStack.peek().canUndo();
    }

    /**
     * Check if redo is available
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Get description of the next undo command
     */
    public String getUndoDescription() {
        if (canUndo()) {
            return undoStack.peek().getDescription();
        }
        return null;
    }

    /**
     * Get description of the next redo command
     */
    public String getRedoDescription() {
        if (canRedo()) {
            return redoStack.peek().getDescription();
        }
        return null;
    }

    /**
     * Get the size of the undo stack
     */
    public int getUndoStackSize() {
        return undoStack.size();
    }

    /**
     * Get the size of the redo stack
     */
    public int getRedoStackSize() {
        return redoStack.size();
    }

    /**
     * Get list of undo commands (for UI display)
     */
    public List<String> getUndoHistory() {
        List<String> history = new ArrayList<>();
        for (int i = undoStack.size() - 1; i >= 0; i--) {
            history.add(undoStack.get(i).getDescription());
        }
        return history;
    }

    /**
     * Get list of redo commands (for UI display)
     */
    public List<String> getRedoHistory() {
        List<String> history = new ArrayList<>();
        for (int i = redoStack.size() - 1; i >= 0; i--) {
            history.add(redoStack.get(i).getDescription());
        }
        return history;
    }

    /**
     * Clear all undo/redo history
     */
    public void clear() {
        logger.info("Clearing undo/redo history");
        undoStack.clear();
        redoStack.clear();
        notifyListeners();
    }

    /**
     * Clear only redo history
     */
    public void clearRedoHistory() {
        redoStack.clear();
        notifyListeners();
    }

    /**
     * Undo multiple commands at once
     */
    public void undoMultiple(int count) {
        for (int i = 0; i < count && canUndo(); i++) {
            undo();
        }
    }

    /**
     * Redo multiple commands at once
     */
    public void redoMultiple(int count) {
        for (int i = 0; i < count && canRedo(); i++) {
            redo();
        }
    }

    /**
     * Add a listener for undo/redo events
     */
    public void addListener(UndoRedoListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     */
    public void removeListener(UndoRedoListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of state change
     */
    private void notifyListeners() {
        for (UndoRedoListener listener : listeners) {
            listener.onUndoRedoStateChanged(canUndo(), canRedo());
        }
    }

    /**
     * Get statistics about undo/redo usage
     */
    public UndoStatistics getStatistics() {
        int totalCommands = undoStack.size() + redoStack.size();

        // Count commands by type
        java.util.Map<Command.CommandType, Integer> typeCount = new java.util.HashMap<>();
        for (Command cmd : undoStack) {
            typeCount.merge(cmd.getType(), 1, Integer::sum);
        }

        return new UndoStatistics(undoStack.size(), redoStack.size(), totalCommands, typeCount);
    }

    /**
     * Listener interface for undo/redo state changes
     */
    public interface UndoRedoListener {
        void onUndoRedoStateChanged(boolean canUndo, boolean canRedo);
    }

    /**
     * Statistics about undo/redo usage
     */
    public static class UndoStatistics {
        private final int undoCount;
        private final int redoCount;
        private final int totalCommands;
        private final java.util.Map<Command.CommandType, Integer> typeCount;

        public UndoStatistics(int undoCount, int redoCount, int totalCommands,
                            java.util.Map<Command.CommandType, Integer> typeCount) {
            this.undoCount = undoCount;
            this.redoCount = redoCount;
            this.totalCommands = totalCommands;
            this.typeCount = typeCount;
        }

        public int getUndoCount() { return undoCount; }
        public int getRedoCount() { return redoCount; }
        public int getTotalCommands() { return totalCommands; }
        public java.util.Map<Command.CommandType, Integer> getTypeCount() { return typeCount; }

        @Override
        public String toString() {
            return String.format("Undo: %d, Redo: %d, Total: %d", undoCount, redoCount, totalCommands);
        }
    }
}
