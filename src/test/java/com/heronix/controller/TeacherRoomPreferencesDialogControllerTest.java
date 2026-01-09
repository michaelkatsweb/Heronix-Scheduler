package com.heronix.controller;

import com.heronix.model.domain.Room;
import com.heronix.model.domain.Teacher;
import com.heronix.model.dto.RoomPreferences;
import com.heronix.model.dto.RoomPreferences.PreferenceStrength;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.RoomRepository;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TeacherRoomPreferencesDialogController
 * Tests room preference management business logic
 *
 * Note: This is a JavaFX controller. We test the business logic
 * (room preferences, restrictions) rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class TeacherRoomPreferencesDialogControllerTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Teacher testTeacher;
    private Room testRoom1;
    private Room testRoom2;
    private Room testRoom3;

    @BeforeEach
    public void setup() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setName("Sarah Anderson");
        testTeacher.setEmployeeId("PREF-T001");
        testTeacher.setActive(true);
        testTeacher = teacherRepository.save(testTeacher);

        // Create test rooms
        testRoom1 = new Room();
        testRoom1.setRoomNumber("201");
        testRoom1.setRoomType(RoomType.CLASSROOM);
        testRoom1.setCapacity(25);
        testRoom1.setActive(true);
        testRoom1 = roomRepository.save(testRoom1);

        testRoom2 = new Room();
        testRoom2.setRoomNumber("202");
        testRoom2.setRoomType(RoomType.CLASSROOM);
        testRoom2.setCapacity(30);
        testRoom2.setActive(true);
        testRoom2 = roomRepository.save(testRoom2);

        testRoom3 = new Room();
        testRoom3.setRoomNumber("LAB1");
        testRoom3.setRoomType(RoomType.LAB);
        testRoom3.setCapacity(20);
        testRoom3.setActive(true);
        testRoom3 = roomRepository.save(testRoom3);
    }

    // ========== ROOM PREFERENCES BASIC ==========

    @Test
    public void testCreateRoomPreferences_ShouldInitialize() {
        // Arrange & Act
        RoomPreferences prefs = new RoomPreferences();

        // Assert
        assertNotNull(prefs);
        assertNotNull(prefs.getPreferredRoomIds());
        assertTrue(prefs.getPreferredRoomIds().isEmpty());
        assertFalse(prefs.isRestrictedToRooms()); // Default is preferences, not restrictions
        assertEquals(PreferenceStrength.MEDIUM, prefs.getStrength());

        System.out.println("✓ Room preferences initialized");
        System.out.println("  - Restricted: " + prefs.isRestrictedToRooms());
        System.out.println("  - Strength: " + prefs.getStrength());
    }

    @Test
    public void testAddPreferredRooms_ShouldStoreRoomIds() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        roomIds.add(testRoom2.getId());

        // Act
        prefs.setPreferredRoomIds(roomIds);

        // Assert
        assertEquals(2, prefs.getPreferredRoomIds().size());
        assertTrue(prefs.getPreferredRoomIds().contains(testRoom1.getId()));
        assertTrue(prefs.getPreferredRoomIds().contains(testRoom2.getId()));

        System.out.println("✓ Preferred rooms added");
        System.out.println("  - Count: " + prefs.getPreferredRoomIds().size());
    }

    @Test
    public void testCheckPreferredRoom_ShouldReturnTrue() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);

        // Act
        boolean prefersRoom1 = prefs.prefersRoom(testRoom1.getId());
        boolean prefersRoom2 = prefs.prefersRoom(testRoom2.getId());

        // Assert
        assertTrue(prefersRoom1);
        assertFalse(prefersRoom2);

        System.out.println("✓ Room preference check works");
        System.out.println("  - Room 201 preferred: " + prefersRoom1);
        System.out.println("  - Room 202 preferred: " + prefersRoom2);
    }

    // ========== PREFERENCE STRENGTHS ==========

    @Test
    public void testLowStrengthPreference_ShouldHaveLowPenalty() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setStrength(PreferenceStrength.LOW);

        // Assert
        assertEquals(PreferenceStrength.LOW, prefs.getStrength());
        assertEquals(1, prefs.getStrength().getPenaltyWeight());

        System.out.println("✓ Low strength preference verified");
        System.out.println("  - Penalty weight: " + prefs.getStrength().getPenaltyWeight());
    }

    @Test
    public void testMediumStrengthPreference_ShouldHaveMediumPenalty() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setStrength(PreferenceStrength.MEDIUM);

        // Assert
        assertEquals(PreferenceStrength.MEDIUM, prefs.getStrength());
        assertEquals(3, prefs.getStrength().getPenaltyWeight());

        System.out.println("✓ Medium strength preference verified");
        System.out.println("  - Penalty weight: " + prefs.getStrength().getPenaltyWeight());
    }

    @Test
    public void testHighStrengthPreference_ShouldHaveHighPenalty() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setStrength(PreferenceStrength.HIGH);

        // Assert
        assertEquals(PreferenceStrength.HIGH, prefs.getStrength());
        assertEquals(5, prefs.getStrength().getPenaltyWeight());

        System.out.println("✓ High strength preference verified");
        System.out.println("  - Penalty weight: " + prefs.getStrength().getPenaltyWeight());
    }

    // ========== ROOM RESTRICTIONS ==========

    @Test
    public void testRestrictedToRooms_ShouldEnforceHardConstraint() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);
        prefs.setRestrictedToRooms(true);

        // Act
        boolean canUseRoom1 = prefs.canUseRoom(testRoom1.getId());
        boolean canUseRoom2 = prefs.canUseRoom(testRoom2.getId());

        // Assert
        assertTrue(canUseRoom1);  // Can use restricted room
        assertFalse(canUseRoom2); // Cannot use non-restricted room
        assertTrue(prefs.isRestrictedToRooms());

        System.out.println("✓ Room restrictions enforced");
        System.out.println("  - Can use Room 201: " + canUseRoom1);
        System.out.println("  - Can use Room 202: " + canUseRoom2);
    }

    @Test
    public void testNoRestrictions_ShouldAllowAllRooms() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);
        prefs.setRestrictedToRooms(false);

        // Act
        boolean canUseRoom1 = prefs.canUseRoom(testRoom1.getId());
        boolean canUseRoom2 = prefs.canUseRoom(testRoom2.getId());

        // Assert
        assertTrue(canUseRoom1);
        assertTrue(canUseRoom2); // Can use any room when not restricted
        assertFalse(prefs.isRestrictedToRooms());

        System.out.println("✓ No restrictions - all rooms allowed");
        System.out.println("  - Can use Room 201: " + canUseRoom1);
        System.out.println("  - Can use Room 202: " + canUseRoom2);
    }

    @Test
    public void testEmptyRestrictions_ShouldAllowAllRooms() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        prefs.setRestrictedToRooms(true);
        prefs.setPreferredRoomIds(new ArrayList<>()); // Empty list

        // Act
        boolean canUseRoom1 = prefs.canUseRoom(testRoom1.getId());
        boolean canUseRoom2 = prefs.canUseRoom(testRoom2.getId());

        // Assert
        assertTrue(canUseRoom1); // No restrictions specified = allow all
        assertTrue(canUseRoom2);

        System.out.println("✓ Empty restrictions list allows all rooms");
    }

    // ========== TEACHER ROOM PREFERENCES ==========

    @Test
    public void testSaveTeacherRoomPreferences_ShouldPersist() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        roomIds.add(testRoom2.getId());
        prefs.setPreferredRoomIds(roomIds);
        prefs.setStrength(PreferenceStrength.HIGH);
        prefs.setRestrictedToRooms(false);

        // Act
        testTeacher.setRoomPreferences(prefs);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertNotNull(saved.getRoomPreferences());
        assertEquals(2, saved.getRoomPreferences().getPreferredRoomIds().size());
        assertEquals(PreferenceStrength.HIGH, saved.getRoomPreferences().getStrength());
        assertFalse(saved.getRoomPreferences().isRestrictedToRooms());

        System.out.println("✓ Teacher room preferences saved");
        System.out.println("  - Preferred rooms: " + saved.getRoomPreferences().getPreferredRoomIds().size());
        System.out.println("  - Strength: " + saved.getRoomPreferences().getStrength());
    }

    @Test
    public void testLoadTeacherRoomPreferences_ShouldRetrieve() {
        // Arrange - Save preferences
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);
        prefs.setStrength(PreferenceStrength.MEDIUM);
        testTeacher.setRoomPreferences(prefs);
        testTeacher = teacherRepository.save(testTeacher);

        // Act - Reload teacher
        Teacher loaded = teacherRepository.findById(testTeacher.getId()).orElse(null);

        // Assert
        assertNotNull(loaded);
        assertNotNull(loaded.getRoomPreferences());
        assertEquals(1, loaded.getRoomPreferences().getPreferredRoomIds().size());
        assertEquals(PreferenceStrength.MEDIUM, loaded.getRoomPreferences().getStrength());

        System.out.println("✓ Teacher room preferences loaded");
    }

    @Test
    public void testUpdateTeacherRoomPreferences_ShouldModify() {
        // Arrange - Initial preferences
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);
        prefs.setStrength(PreferenceStrength.LOW);
        testTeacher.setRoomPreferences(prefs);
        testTeacher = teacherRepository.save(testTeacher);

        // Act - Update preferences
        prefs.getPreferredRoomIds().add(testRoom2.getId());
        prefs.setStrength(PreferenceStrength.HIGH);
        testTeacher.setRoomPreferences(prefs);
        Teacher updated = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(2, updated.getRoomPreferences().getPreferredRoomIds().size());
        assertEquals(PreferenceStrength.HIGH, updated.getRoomPreferences().getStrength());

        System.out.println("✓ Teacher room preferences updated");
        System.out.println("  - New count: " + updated.getRoomPreferences().getPreferredRoomIds().size());
    }

    @Test
    public void testClearTeacherRoomPreferences_ShouldRemove() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);
        testTeacher.setRoomPreferences(prefs);
        testTeacher = teacherRepository.save(testTeacher);

        // Act - Clear rooms from preferences
        prefs.clearRooms();
        testTeacher.setRoomPreferences(prefs);
        Teacher cleared = teacherRepository.save(testTeacher);

        // Assert
        assertNotNull(cleared.getRoomPreferences());
        assertFalse(cleared.getRoomPreferences().hasRooms());
        assertEquals(0, cleared.getRoomPreferences().getRoomCount());

        System.out.println("✓ Teacher room preferences cleared");
        System.out.println("  - Has rooms: " + cleared.getRoomPreferences().hasRooms());
    }

    // ========== MULTIPLE ROOMS ==========

    @Test
    public void testMultipleRoomPreferences_ShouldHandleAll() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        roomIds.add(testRoom2.getId());
        roomIds.add(testRoom3.getId());
        prefs.setPreferredRoomIds(roomIds);

        // Act & Assert
        assertEquals(3, prefs.getPreferredRoomIds().size());
        assertTrue(prefs.prefersRoom(testRoom1.getId()));
        assertTrue(prefs.prefersRoom(testRoom2.getId()));
        assertTrue(prefs.prefersRoom(testRoom3.getId()));

        System.out.println("✓ Multiple room preferences handled");
        System.out.println("  - Total preferred rooms: " + prefs.getPreferredRoomIds().size());
    }

    @Test
    public void testHasRooms_ShouldDetectEmptyList() {
        // Arrange
        RoomPreferences emptyPrefs = new RoomPreferences();
        RoomPreferences withRooms = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        withRooms.setPreferredRoomIds(roomIds);

        // Act & Assert
        assertFalse(emptyPrefs.hasRooms());
        assertTrue(withRooms.hasRooms());

        System.out.println("✓ Empty room list detection works");
    }

    // ========== PREFERENCE VS RESTRICTION ==========

    @Test
    public void testPreferenceMode_ShouldBeSoftConstraint() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);
        prefs.setRestrictedToRooms(false);
        prefs.setStrength(PreferenceStrength.MEDIUM);

        // Act & Assert
        assertFalse(prefs.isRestrictedToRooms());
        assertEquals(PreferenceStrength.MEDIUM, prefs.getStrength());
        assertTrue(prefs.canUseRoom(testRoom2.getId())); // Can still use other rooms

        System.out.println("✓ Preference mode (soft constraint) verified");
        System.out.println("  - Strength: " + prefs.getStrength());
        System.out.println("  - Penalty: " + prefs.getStrength().getPenaltyWeight() + " points");
    }

    @Test
    public void testRestrictionMode_ShouldBeHardConstraint() {
        // Arrange
        RoomPreferences prefs = new RoomPreferences();
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(testRoom1.getId());
        prefs.setPreferredRoomIds(roomIds);
        prefs.setRestrictedToRooms(true);

        // Act & Assert
        assertTrue(prefs.isRestrictedToRooms());
        assertTrue(prefs.canUseRoom(testRoom1.getId()));  // Can use restricted room
        assertFalse(prefs.canUseRoom(testRoom2.getId())); // Cannot use other rooms

        System.out.println("✓ Restriction mode (hard constraint) verified");
        System.out.println("  - Must use only: " + prefs.getPreferredRoomIds().size() + " room(s)");
    }
}
