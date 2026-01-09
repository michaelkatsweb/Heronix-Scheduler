package com.heronix.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DTO representing teacher room preferences
 * Phase 6B: Room Preferences
 *
 * Stored as JSON in Teacher.roomPreferences column
 *
 * @since Phase 6B-1 - December 3, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomPreferences {

    /**
     * IDs of preferred rooms
     */
    private List<Long> preferredRoomIds = new ArrayList<>();

    /**
     * Whether these are restrictions (HARD) or preferences (SOFT)
     * - false (default): Preferences - scheduler will PREFER these rooms but can use others
     * - true: Restrictions - scheduler MUST use only these rooms
     */
    private boolean restrictedToRooms = false;

    /**
     * Strength of preference (only applies when restrictedToRooms = false)
     */
    private PreferenceStrength strength = PreferenceStrength.MEDIUM;

    /**
     * Preference strength enumeration
     */
    public enum PreferenceStrength {
        LOW(1),      // Minor preference (1 point penalty if not met)
        MEDIUM(3),   // Moderate preference (3 point penalty)
        HIGH(5);     // Strong preference (5 point penalty)

        private final int penaltyWeight;

        PreferenceStrength(int penaltyWeight) {
            this.penaltyWeight = penaltyWeight;
        }

        public int getPenaltyWeight() {
            return penaltyWeight;
        }
    }

    /**
     * Cached set for O(1) lookup performance during constraint evaluation
     * Not serialized to JSON
     */
    @JsonIgnore
    private transient Set<Long> preferredRoomIdSet;

    /**
     * Check if a room is in the preferred list
     * Uses cached Set for O(1) performance
     *
     * @param roomId Room ID to check
     * @return true if room is preferred
     */
    public boolean prefersRoom(Long roomId) {
        if (preferredRoomIds == null || preferredRoomIds.isEmpty()) {
            return false;
        }
        return getPreferredRoomIdSet().contains(roomId);
    }

    /**
     * Check if teacher can use a room (based on restrictions)
     *
     * @param roomId Room ID to check
     * @return true if teacher can use this room
     */
    public boolean canUseRoom(Long roomId) {
        if (!restrictedToRooms) {
            return true; // No restrictions = can use any room
        }
        if (preferredRoomIds == null || preferredRoomIds.isEmpty()) {
            return true; // No restricted rooms specified = can use any
        }
        return getPreferredRoomIdSet().contains(roomId);
    }

    /**
     * Get cached set of room IDs for fast lookups
     *
     * @return Set of preferred room IDs
     */
    @JsonIgnore
    public Set<Long> getPreferredRoomIdSet() {
        if (preferredRoomIdSet == null) {
            preferredRoomIdSet = new HashSet<>(preferredRoomIds != null ? preferredRoomIds : new ArrayList<>());
        }
        return preferredRoomIdSet;
    }

    /**
     * Add a room to preferences
     *
     * @param roomId Room ID to add
     */
    public void addRoom(Long roomId) {
        if (preferredRoomIds == null) {
            preferredRoomIds = new ArrayList<>();
        }
        if (!preferredRoomIds.contains(roomId)) {
            preferredRoomIds.add(roomId);
            // Invalidate cache
            preferredRoomIdSet = null;
        }
    }

    /**
     * Remove a room from preferences
     *
     * @param roomId Room ID to remove
     */
    public void removeRoom(Long roomId) {
        if (preferredRoomIds != null) {
            preferredRoomIds.remove(roomId);
            // Invalidate cache
            preferredRoomIdSet = null;
        }
    }

    /**
     * Clear all room preferences
     */
    public void clearRooms() {
        if (preferredRoomIds != null) {
            preferredRoomIds.clear();
        }
        preferredRoomIdSet = null;
    }

    /**
     * Get number of preferred rooms
     *
     * @return Count of preferred rooms
     */
    @JsonIgnore
    public int getRoomCount() {
        return preferredRoomIds != null ? preferredRoomIds.size() : 0;
    }

    /**
     * Check if any rooms are specified
     *
     * @return true if at least one room is specified
     */
    @JsonIgnore
    public boolean hasRooms() {
        return preferredRoomIds != null && !preferredRoomIds.isEmpty();
    }

    /**
     * Validate preferences
     *
     * @return true if valid
     * @throws IllegalArgumentException if invalid
     */
    public boolean validate() {
        // Room IDs can't be null or negative
        if (preferredRoomIds != null) {
            for (Long roomId : preferredRoomIds) {
                if (roomId == null || roomId <= 0) {
                    throw new IllegalArgumentException("Room ID must be positive: " + roomId);
                }
            }
        }

        // Strength must be set when not restricted
        if (!restrictedToRooms && strength == null) {
            throw new IllegalArgumentException("Preference strength must be set");
        }

        return true;
    }

    /**
     * Get display string for UI
     *
     * @return Human-readable description
     */
    @JsonIgnore
    public String getDisplayString() {
        if (!hasRooms()) {
            return "No room preferences";
        }

        if (restrictedToRooms) {
            return String.format("RESTRICTED to %d room(s)", getRoomCount());
        } else {
            return String.format("PREFERS %d room(s) [%s priority]", getRoomCount(), strength);
        }
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}
