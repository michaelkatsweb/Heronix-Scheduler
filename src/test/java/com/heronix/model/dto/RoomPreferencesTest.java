package com.heronix.model.dto;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RoomPreferences DTO
 * Phase 6B: Room Preferences
 *
 * @since Phase 6B-3 - December 3, 2025
 */
class RoomPreferencesTest {

    @Test
    void testNoArgsConstructor() {
        // Act
        RoomPreferences prefs = new RoomPreferences();

        // Assert
        assertNotNull(prefs);
        assertNotNull(prefs.getPreferredRoomIds());
        assertTrue(prefs.getPreferredRoomIds().isEmpty());
        assertFalse(prefs.isRestrictedToRooms());
        assertEquals(RoomPreferences.PreferenceStrength.MEDIUM, prefs.getStrength());
    }

    @Test
    void testAllArgsConstructor() {
        // Arrange
        List<Long> roomIds = Arrays.asList(101L, 102L, 103L);

        // Act
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(roomIds);
        prefs.setRestrictedToRooms(true);
        prefs.setStrength(RoomPreferences.PreferenceStrength.HIGH);

        // Assert
        assertEquals(roomIds, prefs.getPreferredRoomIds());
        assertTrue(prefs.isRestrictedToRooms());
        assertEquals(RoomPreferences.PreferenceStrength.HIGH, prefs.getStrength());
    }

    @Test
    void testPrefersRoom_RoomInList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));

        // Act & Assert
        assertTrue(prefs.prefersRoom(101L));
        assertTrue(prefs.prefersRoom(102L));
        assertTrue(prefs.prefersRoom(103L));
    }

    @Test
    void testPrefersRoom_RoomNotInList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));

        // Act & Assert
        assertFalse(prefs.prefersRoom(104L));
        assertFalse(prefs.prefersRoom(999L));
    }

    @Test
    void testPrefersRoom_EmptyList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();

        // Act & Assert
        assertFalse(prefs.prefersRoom(101L));
    }

    @Test
    void testPrefersRoom_NullList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(null);

        // Act & Assert
        assertFalse(prefs.prefersRoom(101L));
    }

    @Test
    void testCanUseRoom_NoRestrictions() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setRestrictedToRooms(false);
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));

        // Act & Assert
        assertTrue(prefs.canUseRoom(101L)); // Preferred room
        assertTrue(prefs.canUseRoom(999L)); // Non-preferred room (but allowed)
    }

    @Test
    void testCanUseRoom_WithRestrictions_AllowedRoom() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setRestrictedToRooms(true);
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));

        // Act & Assert
        assertTrue(prefs.canUseRoom(101L));
        assertTrue(prefs.canUseRoom(102L));
    }

    @Test
    void testCanUseRoom_WithRestrictions_DisallowedRoom() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setRestrictedToRooms(true);
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));

        // Act & Assert
        assertFalse(prefs.canUseRoom(103L));
        assertFalse(prefs.canUseRoom(999L));
    }

    @Test
    void testCanUseRoom_WithRestrictions_EmptyList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setRestrictedToRooms(true);
        prefs.setPreferredRoomIds(Arrays.asList());

        // Act & Assert - Empty restriction list means can use any room
        assertTrue(prefs.canUseRoom(101L));
    }

    @Test
    void testAddRoom() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();

        // Act
        prefs.addRoom(101L);
        prefs.addRoom(102L);

        // Assert
        assertEquals(2, prefs.getRoomCount());
        assertTrue(prefs.prefersRoom(101L));
        assertTrue(prefs.prefersRoom(102L));
    }

    @Test
    void testAddRoom_Duplicate() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.addRoom(101L);

        // Act
        prefs.addRoom(101L); // Add same room again

        // Assert
        assertEquals(1, prefs.getRoomCount()); // Should not duplicate
    }

    @Test
    void testRemoveRoom() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(new ArrayList<>(Arrays.asList(101L, 102L, 103L)));

        // Act
        prefs.removeRoom(102L);

        // Assert
        assertEquals(2, prefs.getRoomCount());
        assertTrue(prefs.prefersRoom(101L));
        assertFalse(prefs.prefersRoom(102L));
        assertTrue(prefs.prefersRoom(103L));
    }

    @Test
    void testRemoveRoom_NotPresent() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));

        // Act
        prefs.removeRoom(999L); // Remove room not in list

        // Assert
        assertEquals(2, prefs.getRoomCount()); // Count unchanged
    }

    @Test
    void testClearRooms() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(new ArrayList<>(Arrays.asList(101L, 102L, 103L)));

        // Act
        prefs.clearRooms();

        // Assert
        assertEquals(0, prefs.getRoomCount());
        assertFalse(prefs.hasRooms());
    }

    @Test
    void testGetRoomCount() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();

        // Act & Assert
        assertEquals(0, prefs.getRoomCount());

        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));
        assertEquals(3, prefs.getRoomCount());
    }

    @Test
    void testGetRoomCount_NullList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(null);

        // Act & Assert
        assertEquals(0, prefs.getRoomCount());
    }

    @Test
    void testHasRooms_WithRooms() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L));

        // Act & Assert
        assertTrue(prefs.hasRooms());
    }

    @Test
    void testHasRooms_NoRooms() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();

        // Act & Assert
        assertFalse(prefs.hasRooms());
    }

    @Test
    void testHasRooms_NullList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(null);

        // Act & Assert
        assertFalse(prefs.hasRooms());
    }

    @Test
    void testValidate_ValidPreferences() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));
        prefs.setRestrictedToRooms(false);
        prefs.setStrength(RoomPreferences.PreferenceStrength.MEDIUM);

        // Act & Assert
        assertDoesNotThrow(() -> prefs.validate());
        assertTrue(prefs.validate());
    }

    @Test
    void testValidate_ValidRestrictions() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L));
        prefs.setRestrictedToRooms(true);

        // Act & Assert
        assertDoesNotThrow(() -> prefs.validate());
    }

    @Test
    void testValidate_NullRoomId() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, null, 102L));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> prefs.validate());
    }

    @Test
    void testValidate_NegativeRoomId() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, -5L));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> prefs.validate());
    }

    @Test
    void testValidate_ZeroRoomId() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(0L));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> prefs.validate());
    }

    @Test
    void testValidate_NullStrength() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L));
        prefs.setRestrictedToRooms(false);
        prefs.setStrength(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> prefs.validate());
    }

    @Test
    void testPreferenceStrength_PenaltyWeights() {
        // Act & Assert
        assertEquals(1, RoomPreferences.PreferenceStrength.LOW.getPenaltyWeight());
        assertEquals(3, RoomPreferences.PreferenceStrength.MEDIUM.getPenaltyWeight());
        assertEquals(5, RoomPreferences.PreferenceStrength.HIGH.getPenaltyWeight());
    }

    @Test
    void testGetDisplayString_Preferences() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));
        prefs.setRestrictedToRooms(false);
        prefs.setStrength(RoomPreferences.PreferenceStrength.HIGH);

        // Act
        String display = prefs.getDisplayString();

        // Assert
        assertTrue(display.contains("PREFERS"));
        assertTrue(display.contains("3"));
        assertTrue(display.contains("HIGH"));
    }

    @Test
    void testGetDisplayString_Restrictions() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));
        prefs.setRestrictedToRooms(true);

        // Act
        String display = prefs.getDisplayString();

        // Assert
        assertTrue(display.contains("RESTRICTED"));
        assertTrue(display.contains("2"));
    }

    @Test
    void testGetDisplayString_NoRooms() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();

        // Act
        String display = prefs.getDisplayString();

        // Assert
        assertEquals("No room preferences", display);
    }

    @Test
    void testToString() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));
        prefs.setRestrictedToRooms(false);
        prefs.setStrength(RoomPreferences.PreferenceStrength.MEDIUM);

        // Act
        String toString = prefs.toString();

        // Assert
        assertEquals(prefs.getDisplayString(), toString);
    }

    @Test
    void testGetPreferredRoomIdSet_CachingBehavior() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));

        // Act
        var set1 = prefs.getPreferredRoomIdSet();
        var set2 = prefs.getPreferredRoomIdSet();

        // Assert
        assertSame(set1, set2); // Should return same cached instance
        assertEquals(3, set1.size());
        assertTrue(set1.contains(101L));
        assertTrue(set1.contains(102L));
        assertTrue(set1.contains(103L));
    }

    @Test
    void testGetPreferredRoomIdSet_CacheInvalidation() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(new ArrayList<>(Arrays.asList(101L, 102L)));

        var set1 = prefs.getPreferredRoomIdSet();

        // Act - Modify the list (should invalidate cache)
        prefs.addRoom(103L);
        var set2 = prefs.getPreferredRoomIdSet();

        // Assert
        assertNotSame(set1, set2); // Should be new instance
        assertEquals(3, set2.size());
        assertTrue(set2.contains(103L));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = Arrays.asList(101L, 102L, 103L);

        // Act
        prefs.setPreferredRoomIds(roomIds);
        prefs.setRestrictedToRooms(true);
        prefs.setStrength(RoomPreferences.PreferenceStrength.HIGH);

        // Assert
        assertEquals(roomIds, prefs.getPreferredRoomIds());
        assertTrue(prefs.isRestrictedToRooms());
        assertEquals(RoomPreferences.PreferenceStrength.HIGH, prefs.getStrength());
    }

    @Test
    void testEdgeCases_VeryLargeRoomList() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new java.util.ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            roomIds.add(i);
        }
        prefs.setPreferredRoomIds(roomIds);

        // Act & Assert - Should handle large lists efficiently
        assertEquals(1000, prefs.getRoomCount());
        assertTrue(prefs.prefersRoom(500L));
        assertFalse(prefs.prefersRoom(1001L));
    }
}
