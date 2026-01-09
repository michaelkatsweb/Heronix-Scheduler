package com.heronix.model.domain;

import com.heronix.model.dto.UnavailableTimeBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Teacher availability methods
 * Phase 6A: Teacher Availability Constraints
 *
 * Tests the helper methods added to Teacher model for managing unavailable time blocks
 *
 * @since Phase 6A - December 2, 2025
 */
class TeacherAvailabilityTest {

    private Teacher teacher;

    @BeforeEach
    void setUp() {
        teacher = new Teacher();
        teacher.setName("John Smith");
    }

    // ========================================================================
    // GET/SET UNAVAILABLE TIME BLOCKS
    // ========================================================================

    @Test
    void testGetUnavailableTimeBlocks_EmptyWhenNull() {
        // Arrange: Teacher with no unavailable times set

        // Act
        List<UnavailableTimeBlock> blocks = teacher.getUnavailableTimeBlocks();

        // Assert
        assertNotNull(blocks, "Should return non-null list");
        assertTrue(blocks.isEmpty(), "Should return empty list when unavailableTimes is null");
    }

    @Test
    void testSetUnavailableTimeBlocks_SingleBlock() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        // Act
        teacher.setUnavailableTimeBlocks(List.of(block));

        // Assert
        List<UnavailableTimeBlock> retrieved = teacher.getUnavailableTimeBlocks();
        assertEquals(1, retrieved.size(), "Should have 1 block");
        assertEquals(DayOfWeek.MONDAY, retrieved.get(0).getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), retrieved.get(0).getStartTime());
        assertEquals(LocalTime.of(10, 0), retrieved.get(0).getEndTime());
    }

    @Test
    void testSetUnavailableTimeBlocks_MultipleBlocks() {
        // Arrange
        UnavailableTimeBlock block1 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        UnavailableTimeBlock block2 = new UnavailableTimeBlock(
            DayOfWeek.WEDNESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            "IEP Meetings",
            true
        );

        // Act
        teacher.setUnavailableTimeBlocks(List.of(block1, block2));

        // Assert
        List<UnavailableTimeBlock> retrieved = teacher.getUnavailableTimeBlocks();
        assertEquals(2, retrieved.size(), "Should have 2 blocks");
    }

    @Test
    void testSetUnavailableTimeBlocks_NullSetsNull() {
        // Arrange
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Test", true)
        ));

        // Act: Set to null
        teacher.setUnavailableTimeBlocks(null);

        // Assert
        assertTrue(teacher.getUnavailableTimeBlocks().isEmpty(),
            "Setting null should result in empty list");
    }

    @Test
    void testSetUnavailableTimeBlocks_EmptyListSetsNull() {
        // Arrange
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Test", true)
        ));

        // Act: Set to empty list
        teacher.setUnavailableTimeBlocks(List.of());

        // Assert
        assertTrue(teacher.getUnavailableTimeBlocks().isEmpty(),
            "Setting empty list should result in empty list");
    }

    // ========================================================================
    // IS AVAILABLE AT
    // ========================================================================

    @Test
    void testIsAvailableAt_NoBlocksDefined() {
        // Arrange: No unavailable blocks

        // Act & Assert: Teacher should be available at any time
        assertTrue(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
            "Should be available when no blocks defined");
        assertTrue(teacher.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(14, 0)),
            "Should be available when no blocks defined");
    }

    @Test
    void testIsAvailableAt_DuringUnavailableBlock() {
        // Arrange: Monday 9:00-10:00 unavailable
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act & Assert: Should NOT be available during block
        assertFalse(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
            "Should NOT be available at start of block");
        assertFalse(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(9, 30)),
            "Should NOT be available in middle of block");
        assertFalse(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(9, 59)),
            "Should NOT be available just before end of block");
    }

    @Test
    void testIsAvailableAt_OutsideUnavailableBlock() {
        // Arrange: Monday 9:00-10:00 unavailable
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act & Assert: Should be available outside block
        assertTrue(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(8, 59)),
            "Should be available before block");
        assertTrue(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(10, 0)),
            "Should be available at end time (exclusive)");
        assertTrue(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(10, 1)),
            "Should be available after block");
    }

    @Test
    void testIsAvailableAt_DifferentDay() {
        // Arrange: Monday 9:00-10:00 unavailable
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act & Assert: Should be available on different days
        assertTrue(teacher.isAvailableAt(DayOfWeek.TUESDAY, LocalTime.of(9, 30)),
            "Should be available same time on different day");
        assertTrue(teacher.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(9, 30)),
            "Should be available same time on different day");
    }

    @Test
    void testIsAvailableAt_MultipleBlocks() {
        // Arrange: Multiple unavailable blocks
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting 1", true),
            new UnavailableTimeBlock(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), "Meeting 2", true)
        ));

        // Act & Assert
        assertFalse(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(9, 30)),
            "Should NOT be available during first block");
        assertFalse(teacher.isAvailableAt(DayOfWeek.WEDNESDAY, LocalTime.of(14, 30)),
            "Should NOT be available during second block");
        assertTrue(teacher.isAvailableAt(DayOfWeek.TUESDAY, LocalTime.of(9, 30)),
            "Should be available between blocks");
    }

    @Test
    void testIsAvailableAt_NullDay() {
        // Arrange
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act & Assert: Null day should return true (safe default)
        assertTrue(teacher.isAvailableAt(null, LocalTime.of(9, 30)),
            "Null day should return available (safe default)");
    }

    @Test
    void testIsAvailableAt_NullTime() {
        // Arrange
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act & Assert: Null time should return true (safe default)
        assertTrue(teacher.isAvailableAt(DayOfWeek.MONDAY, null),
            "Null time should return available (safe default)");
    }

    // ========================================================================
    // HAS UNAVAILABLE TIMES
    // ========================================================================

    @Test
    void testHasUnavailableTimes_WhenNull() {
        // Arrange: No blocks set

        // Act & Assert
        assertFalse(teacher.hasUnavailableTimes(),
            "Should return false when unavailableTimes is null");
    }

    @Test
    void testHasUnavailableTimes_WhenEmpty() {
        // Arrange: Set to empty list
        teacher.setUnavailableTimeBlocks(List.of());

        // Act & Assert
        assertFalse(teacher.hasUnavailableTimes(),
            "Should return false when list is empty");
    }

    @Test
    void testHasUnavailableTimes_WhenSet() {
        // Arrange: Add a block
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act & Assert
        assertTrue(teacher.hasUnavailableTimes(),
            "Should return true when blocks are set");
    }

    // ========================================================================
    // ADD UNAVAILABLE TIME BLOCK
    // ========================================================================

    @Test
    void testAddUnavailableTimeBlock_ToEmptyList() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        // Act
        teacher.addUnavailableTimeBlock(block);

        // Assert
        List<UnavailableTimeBlock> blocks = teacher.getUnavailableTimeBlocks();
        assertEquals(1, blocks.size(), "Should have 1 block");
        assertEquals(DayOfWeek.MONDAY, blocks.get(0).getDayOfWeek());
    }

    @Test
    void testAddUnavailableTimeBlock_ToExistingList() {
        // Arrange: Add first block
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting 1", true)
        ));

        // Act: Add second block
        teacher.addUnavailableTimeBlock(
            new UnavailableTimeBlock(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), "Meeting 2", true)
        );

        // Assert
        List<UnavailableTimeBlock> blocks = teacher.getUnavailableTimeBlocks();
        assertEquals(2, blocks.size(), "Should have 2 blocks");
    }

    @Test
    void testAddUnavailableTimeBlock_OverlapThrowsException() {
        // Arrange: Add first block
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting 1", true)
        ));

        // Act & Assert: Adding overlapping block should throw
        UnavailableTimeBlock overlappingBlock = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 30),  // Overlaps with 9:00-10:00
            LocalTime.of(10, 30),
            "Meeting 2",
            true
        );

        assertThrows(IllegalArgumentException.class,
            () -> teacher.addUnavailableTimeBlock(overlappingBlock),
            "Adding overlapping block should throw IllegalArgumentException");
    }

    @Test
    void testAddUnavailableTimeBlock_InvalidBlockThrowsException() {
        // Arrange: Invalid block (start after end)
        UnavailableTimeBlock invalidBlock = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(10, 0),
            LocalTime.of(9, 0),  // End before start!
            "Invalid",
            true
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> teacher.addUnavailableTimeBlock(invalidBlock),
            "Adding invalid block should throw IllegalArgumentException");
    }

    // ========================================================================
    // REMOVE UNAVAILABLE TIME BLOCK
    // ========================================================================

    @Test
    void testRemoveUnavailableTimeBlock_ValidIndex() {
        // Arrange: Add two blocks
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting 1", true),
            new UnavailableTimeBlock(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), "Meeting 2", true)
        ));

        // Act: Remove first block
        teacher.removeUnavailableTimeBlock(0);

        // Assert
        List<UnavailableTimeBlock> blocks = teacher.getUnavailableTimeBlocks();
        assertEquals(1, blocks.size(), "Should have 1 block remaining");
        assertEquals(DayOfWeek.WEDNESDAY, blocks.get(0).getDayOfWeek(),
            "Remaining block should be Wednesday block");
    }

    @Test
    void testRemoveUnavailableTimeBlock_InvalidIndex() {
        // Arrange: Add one block
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act: Remove with invalid index (should not throw, just be no-op)
        teacher.removeUnavailableTimeBlock(5);

        // Assert
        assertEquals(1, teacher.getUnavailableTimeBlocks().size(),
            "Should still have 1 block (invalid index ignored)");
    }

    @Test
    void testRemoveUnavailableTimeBlock_NegativeIndex() {
        // Arrange: Add one block
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", true)
        ));

        // Act: Remove with negative index (should not throw, just be no-op)
        teacher.removeUnavailableTimeBlock(-1);

        // Assert
        assertEquals(1, teacher.getUnavailableTimeBlocks().size(),
            "Should still have 1 block (negative index ignored)");
    }

    // ========================================================================
    // CLEAR UNAVAILABLE TIME BLOCKS
    // ========================================================================

    @Test
    void testClearUnavailableTimeBlocks() {
        // Arrange: Add multiple blocks
        teacher.setUnavailableTimeBlocks(List.of(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting 1", true),
            new UnavailableTimeBlock(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), "Meeting 2", true)
        ));

        // Act: Clear all blocks
        teacher.clearUnavailableTimeBlocks();

        // Assert
        assertTrue(teacher.getUnavailableTimeBlocks().isEmpty(),
            "Should have no blocks after clear");
        assertFalse(teacher.hasUnavailableTimes(),
            "hasUnavailableTimes should return false after clear");
    }

    @Test
    void testClearUnavailableTimeBlocks_WhenAlreadyEmpty() {
        // Arrange: No blocks

        // Act: Clear (should not throw)
        teacher.clearUnavailableTimeBlocks();

        // Assert
        assertTrue(teacher.getUnavailableTimeBlocks().isEmpty(),
            "Should remain empty");
    }

    // ========================================================================
    // INTEGRATION TESTS
    // ========================================================================

    @Test
    void testPartTimeTeacherScenario() {
        // Scenario: Teacher only works Monday, Wednesday, Friday
        // Block out Tuesday and Thursday entirely

        // Arrange
        teacher.addUnavailableTimeBlock(
            new UnavailableTimeBlock(DayOfWeek.TUESDAY, LocalTime.of(0, 0), LocalTime.of(23, 59), "Not Working", true)
        );
        teacher.addUnavailableTimeBlock(
            new UnavailableTimeBlock(DayOfWeek.THURSDAY, LocalTime.of(0, 0), LocalTime.of(23, 59), "Not Working", true)
        );

        // Act & Assert: Available on M/W/F
        assertTrue(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(10, 0)),
            "Should be available Monday");
        assertTrue(teacher.isAvailableAt(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0)),
            "Should be available Wednesday");
        assertTrue(teacher.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(9, 0)),
            "Should be available Friday");

        // Not available on T/TH
        assertFalse(teacher.isAvailableAt(DayOfWeek.TUESDAY, LocalTime.of(10, 0)),
            "Should NOT be available Tuesday");
        assertFalse(teacher.isAvailableAt(DayOfWeek.THURSDAY, LocalTime.of(14, 0)),
            "Should NOT be available Thursday");
    }

    @Test
    void testMultipleMeetingsScenario() {
        // Scenario: Teacher has department meeting Monday mornings
        // and IEP meetings Wednesday afternoons

        // Arrange
        teacher.addUnavailableTimeBlock(
            new UnavailableTimeBlock(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Department Meeting", true)
        );
        teacher.addUnavailableTimeBlock(
            new UnavailableTimeBlock(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "IEP Meetings", true)
        );

        // Act & Assert
        assertFalse(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(9, 30)),
            "Should NOT be available Monday morning");
        assertFalse(teacher.isAvailableAt(DayOfWeek.WEDNESDAY, LocalTime.of(15, 0)),
            "Should NOT be available Wednesday afternoon");

        // Available at other times
        assertTrue(teacher.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(14, 0)),
            "Should be available Monday afternoon");
        assertTrue(teacher.isAvailableAt(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0)),
            "Should be available Wednesday morning");
        assertTrue(teacher.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(10, 0)),
            "Should be available Friday");
    }
}
