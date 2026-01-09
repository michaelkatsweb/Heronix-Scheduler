package com.heronix.model.domain;

import com.heronix.model.dto.RoomPreferences;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Teacher room preferences
 * Phase 6B: Room Preferences
 *
 * Tests the integration between Teacher entity and RoomPreferences DTO,
 * including JSON serialization/deserialization.
 *
 * @since Phase 6B-3 - December 3, 2025
 */
class TeacherRoomPreferencesTest {

    @Test
    void testGetRoomPreferences_NoPreferencesSet() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("John Smith");

        // Act
        RoomPreferences prefs = teacher.getRoomPreferences();

        // Assert
        assertNotNull(prefs);
        assertFalse(prefs.hasRooms());
        assertEquals(0, prefs.getRoomCount());
    }

    @Test
    void testSetRoomPreferences_ValidPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Jane Doe");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));
        prefs.setRestrictedToRooms(false);
        prefs.setStrength(RoomPreferences.PreferenceStrength.HIGH);

        // Act
        teacher.setRoomPreferences(prefs);

        // Assert
        RoomPreferences retrieved = teacher.getRoomPreferences();
        assertNotNull(retrieved);
        assertEquals(3, retrieved.getRoomCount());
        assertTrue(retrieved.prefersRoom(101L));
        assertEquals(RoomPreferences.PreferenceStrength.HIGH, retrieved.getStrength());
        assertFalse(retrieved.isRestrictedToRooms());
    }

    @Test
    void testSetRoomPreferences_NullPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Bob Johnson");

        // Act
        teacher.setRoomPreferences(null);

        // Assert
        RoomPreferences retrieved = teacher.getRoomPreferences();
        assertNotNull(retrieved); // Should return empty preferences, not null
        assertFalse(retrieved.hasRooms());
    }

    @Test
    void testSetRoomPreferences_EmptyPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Alice Brown");

        RoomPreferences prefs = new RoomPreferences();
        // No rooms added

        // Act
        teacher.setRoomPreferences(prefs);

        // Assert
        assertFalse(teacher.hasRoomPreferences());
    }

    @Test
    void testPrefersRoom_WithPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Math Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(205L, 206L, 207L));
        prefs.setRestrictedToRooms(false);
        teacher.setRoomPreferences(prefs);

        Room room205 = new Room();
        room205.setId(205L);
        room205.setRoomNumber("205");

        Room room999 = new Room();
        room999.setId(999L);
        room999.setRoomNumber("999");

        // Act & Assert
        assertTrue(teacher.prefersRoom(room205));
        assertFalse(teacher.prefersRoom(room999));
    }

    @Test
    void testPrefersRoom_NoPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        Room room = new Room();
        room.setId(101L);

        // Act & Assert
        assertFalse(teacher.prefersRoom(room));
    }

    @Test
    void testPrefersRoom_NullRoom() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L));
        teacher.setRoomPreferences(prefs);

        // Act & Assert
        assertFalse(teacher.prefersRoom(null));
    }

    @Test
    void testCanUseRoom_NoRestrictions() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L));
        prefs.setRestrictedToRooms(false);
        teacher.setRoomPreferences(prefs);

        Room room101 = new Room();
        room101.setId(101L);

        Room room999 = new Room();
        room999.setId(999L);

        // Act & Assert
        assertTrue(teacher.canUseRoom(room101)); // Preferred room
        assertTrue(teacher.canUseRoom(room999)); // Non-preferred but allowed
    }

    @Test
    void testCanUseRoom_WithRestrictions_AllowedRoom() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("PE Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L)); // Gymnasium only
        prefs.setRestrictedToRooms(true);
        teacher.setRoomPreferences(prefs);

        Room gymnasium = new Room();
        gymnasium.setId(101L);
        gymnasium.setRoomNumber("Gymnasium");

        // Act & Assert
        assertTrue(teacher.canUseRoom(gymnasium));
    }

    @Test
    void testCanUseRoom_WithRestrictions_DisallowedRoom() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("PE Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L)); // Gymnasium only
        prefs.setRestrictedToRooms(true);
        teacher.setRoomPreferences(prefs);

        Room classroom = new Room();
        classroom.setId(205L);
        classroom.setRoomNumber("205");

        // Act & Assert
        assertFalse(teacher.canUseRoom(classroom));
    }

    @Test
    void testCanUseRoom_NullRoom() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        // Act & Assert
        assertFalse(teacher.canUseRoom(null));
    }

    @Test
    void testHasRoomPreferences_WithPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L));
        teacher.setRoomPreferences(prefs);

        // Act & Assert
        assertTrue(teacher.hasRoomPreferences());
    }

    @Test
    void testHasRoomPreferences_NoPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        // Act & Assert
        assertFalse(teacher.hasRoomPreferences());
    }

    @Test
    void testIsRestrictedToRooms_True() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Science Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(301L)); // Science lab only
        prefs.setRestrictedToRooms(true);
        teacher.setRoomPreferences(prefs);

        // Act & Assert
        assertTrue(teacher.isRestrictedToRooms());
    }

    @Test
    void testIsRestrictedToRooms_False() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Math Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(205L));
        prefs.setRestrictedToRooms(false);
        teacher.setRoomPreferences(prefs);

        // Act & Assert
        assertFalse(teacher.isRestrictedToRooms());
    }

    @Test
    void testIsRestrictedToRooms_NoPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        // Act & Assert
        assertFalse(teacher.isRestrictedToRooms());
    }

    @Test
    void testClearRoomPreferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));
        teacher.setRoomPreferences(prefs);

        // Act
        teacher.clearRoomPreferences();

        // Assert
        assertFalse(teacher.hasRoomPreferences());
        RoomPreferences retrieved = teacher.getRoomPreferences();
        assertFalse(retrieved.hasRooms());
    }

    @Test
    void testJsonSerialization_Preferences() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher with Preferences");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(101L, 102L, 103L));
        prefs.setRestrictedToRooms(false);
        prefs.setStrength(RoomPreferences.PreferenceStrength.HIGH);

        // Act
        teacher.setRoomPreferences(prefs);
        RoomPreferences retrieved = teacher.getRoomPreferences();

        // Assert - Should correctly serialize and deserialize
        assertEquals(3, retrieved.getRoomCount());
        assertTrue(retrieved.prefersRoom(101L));
        assertTrue(retrieved.prefersRoom(102L));
        assertTrue(retrieved.prefersRoom(103L));
        assertFalse(retrieved.isRestrictedToRooms());
        assertEquals(RoomPreferences.PreferenceStrength.HIGH, retrieved.getStrength());
    }

    @Test
    void testJsonSerialization_Restrictions() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher with Restrictions");

        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(Arrays.asList(301L, 302L));
        prefs.setRestrictedToRooms(true);
        prefs.setStrength(RoomPreferences.PreferenceStrength.MEDIUM);

        // Act
        teacher.setRoomPreferences(prefs);
        RoomPreferences retrieved = teacher.getRoomPreferences();

        // Assert
        assertEquals(2, retrieved.getRoomCount());
        assertTrue(retrieved.isRestrictedToRooms());
        assertTrue(retrieved.canUseRoom(301L));
        assertFalse(retrieved.canUseRoom(999L));
    }

    @Test
    void testMultipleTeachersIndependentPreferences() {
        // Arrange
        Teacher teacher1 = new Teacher();
        teacher1.setName("Math Teacher");
        Teacher teacher2 = new Teacher();
        teacher2.setName("PE Teacher");

        RoomPreferences mathPrefs = new RoomPreferences();
        mathPrefs.setPreferredRoomIds(Arrays.asList(205L, 206L));
        mathPrefs.setRestrictedToRooms(false);

        RoomPreferences pePrefs = new RoomPreferences();
        pePrefs.setPreferredRoomIds(Arrays.asList(101L));
        pePrefs.setRestrictedToRooms(true);

        // Act
        teacher1.setRoomPreferences(mathPrefs);
        teacher2.setRoomPreferences(pePrefs);

        // Assert - Preferences should be independent
        assertTrue(teacher1.getRoomPreferences().prefersRoom(205L));
        assertFalse(teacher1.getRoomPreferences().prefersRoom(101L));
        assertFalse(teacher1.isRestrictedToRooms());

        assertTrue(teacher2.getRoomPreferences().prefersRoom(101L));
        assertFalse(teacher2.getRoomPreferences().prefersRoom(205L));
        assertTrue(teacher2.isRestrictedToRooms());
    }

    @Test
    void testPreferenceStrengthPersistence() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");

        // Test each strength level
        for (RoomPreferences.PreferenceStrength strength : RoomPreferences.PreferenceStrength.values()) {
            RoomPreferences prefs = new RoomPreferences();
            prefs.setPreferredRoomIds(Arrays.asList(101L));
            prefs.setStrength(strength);

            // Act
            teacher.setRoomPreferences(prefs);
            RoomPreferences retrieved = teacher.getRoomPreferences();

            // Assert
            assertEquals(strength, retrieved.getStrength());
        }
    }

    @Test
    void testLargeNumberOfPreferredRooms() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setName("Traveling Teacher");

        RoomPreferences prefs = new RoomPreferences();
        java.util.List<Long> roomIds = new java.util.ArrayList<>();
        for (long i = 1; i <= 50; i++) {
            roomIds.add(i);
        }
        prefs.setPreferredRoomIds(roomIds);

        // Act
        teacher.setRoomPreferences(prefs);
        RoomPreferences retrieved = teacher.getRoomPreferences();

        // Assert
        assertEquals(50, retrieved.getRoomCount());
        assertTrue(retrieved.prefersRoom(25L));
        assertFalse(retrieved.prefersRoom(51L));
    }
}
