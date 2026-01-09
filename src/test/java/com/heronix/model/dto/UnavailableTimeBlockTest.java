package com.heronix.model.dto;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UnavailableTimeBlock DTO
 * Phase 6A: Teacher Availability Constraints
 *
 * @since Phase 6A - December 2, 2025
 */
class UnavailableTimeBlockTest {

    @Test
    void testContains_TimeWithinBlock() {
        // Arrange: Monday 9:00-10:00 block
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        // Act & Assert: Times within block
        assertTrue(block.contains(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
            "Start time should be contained");
        assertTrue(block.contains(DayOfWeek.MONDAY, LocalTime.of(9, 30)),
            "Middle time should be contained");
        assertTrue(block.contains(DayOfWeek.MONDAY, LocalTime.of(9, 59)),
            "Time just before end should be contained");
    }

    @Test
    void testContains_TimeOutsideBlock() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        // Act & Assert: Times outside block
        assertFalse(block.contains(DayOfWeek.MONDAY, LocalTime.of(8, 59)),
            "Time before start should not be contained");
        assertFalse(block.contains(DayOfWeek.MONDAY, LocalTime.of(10, 0)),
            "End time should not be contained (exclusive)");
        assertFalse(block.contains(DayOfWeek.MONDAY, LocalTime.of(10, 1)),
            "Time after end should not be contained");
    }

    @Test
    void testContains_DifferentDay() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        // Act & Assert: Different day
        assertFalse(block.contains(DayOfWeek.TUESDAY, LocalTime.of(9, 30)),
            "Same time on different day should not be contained");
    }

    @Test
    void testOverlaps_CompleteOverlap() {
        // Arrange
        UnavailableTimeBlock block1 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Meeting 1",
            true
        );

        UnavailableTimeBlock block2 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Meeting 2",
            true
        );

        // Act & Assert
        assertTrue(block1.overlaps(block2), "Identical blocks should overlap");
        assertTrue(block2.overlaps(block1), "Overlap should be symmetric");
    }

    @Test
    void testOverlaps_PartialOverlap() {
        // Arrange
        UnavailableTimeBlock block1 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Meeting 1",
            true
        );

        UnavailableTimeBlock block2 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 30),
            LocalTime.of(10, 30),
            "Meeting 2",
            true
        );

        // Act & Assert
        assertTrue(block1.overlaps(block2), "Partially overlapping blocks should overlap");
        assertTrue(block2.overlaps(block1), "Overlap should be symmetric");
    }

    @Test
    void testOverlaps_ContainedBlock() {
        // Arrange: block2 completely inside block1
        UnavailableTimeBlock block1 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            "Long Meeting",
            true
        );

        UnavailableTimeBlock block2 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            "Short Meeting",
            true
        );

        // Act & Assert
        assertTrue(block1.overlaps(block2), "Containing block should overlap");
        assertTrue(block2.overlaps(block1), "Contained block should overlap");
    }

    @Test
    void testOverlaps_NoOverlap() {
        // Arrange: Consecutive but non-overlapping blocks
        UnavailableTimeBlock block1 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Meeting 1",
            true
        );

        UnavailableTimeBlock block2 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "Meeting 2",
            true
        );

        // Act & Assert
        assertFalse(block1.overlaps(block2), "Consecutive blocks should not overlap");
        assertFalse(block2.overlaps(block1), "Overlap check should be symmetric");
    }

    @Test
    void testOverlaps_DifferentDays() {
        // Arrange
        UnavailableTimeBlock block1 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Monday Meeting",
            true
        );

        UnavailableTimeBlock block2 = new UnavailableTimeBlock(
            DayOfWeek.TUESDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Tuesday Meeting",
            true
        );

        // Act & Assert
        assertFalse(block1.overlaps(block2), "Different days should not overlap");
    }

    @Test
    void testGetDurationMinutes() {
        // Arrange: 1 hour block
        UnavailableTimeBlock block1 = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "1 Hour Meeting",
            true
        );

        // Arrange: 90 minute block
        UnavailableTimeBlock block2 = new UnavailableTimeBlock(
            DayOfWeek.TUESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(15, 30),
            "90 Minute Meeting",
            true
        );

        // Act & Assert
        assertEquals(60, block1.getDurationMinutes(), "1 hour should be 60 minutes");
        assertEquals(90, block2.getDurationMinutes(), "1.5 hours should be 90 minutes");
    }

    @Test
    void testGetDisplayString() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        // Act
        String display = block.getDisplayString();

        // Assert
        assertTrue(display.contains("Monday"), "Display should include day");
        assertTrue(display.contains("09:00"), "Display should include start time");
        assertTrue(display.contains("10:00"), "Display should include end time");
        assertTrue(display.contains("Department Meeting"), "Display should include reason");
    }

    @Test
    void testGetDisplayString_NoReason() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.WEDNESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            null,  // No reason
            true
        );

        // Act
        String display = block.getDisplayString();

        // Assert
        assertTrue(display.contains("Wednesday"), "Display should include day");
        assertTrue(display.contains("14:00"), "Display should include start time");
        assertFalse(display.contains("("), "Display should not have parentheses without reason");
    }

    @Test
    void testValidate_ValidBlock() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Meeting",
            true
        );

        // Act & Assert
        assertDoesNotThrow(() -> block.validate(), "Valid block should not throw");
        assertTrue(block.validate(), "validate() should return true for valid block");
    }

    @Test
    void testValidate_NullDayOfWeek() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            null,  // Invalid: null day
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Meeting",
            true
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> block.validate(),
            "Null day of week should throw IllegalArgumentException");
    }

    @Test
    void testValidate_NullStartTime() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            null,  // Invalid: null start time
            LocalTime.of(10, 0),
            "Meeting",
            true
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> block.validate(),
            "Null start time should throw IllegalArgumentException");
    }

    @Test
    void testValidate_NullEndTime() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            null,  // Invalid: null end time
            "Meeting",
            true
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> block.validate(),
            "Null end time should throw IllegalArgumentException");
    }

    @Test
    void testValidate_StartTimeAfterEndTime() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(10, 0),  // Start after end
            LocalTime.of(9, 0),
            "Meeting",
            true
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> block.validate(),
            "Start time after end time should throw IllegalArgumentException");
    }

    @Test
    void testValidate_StartTimeEqualsEndTime() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(9, 0),  // Same as start
            "Meeting",
            true
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> block.validate(),
            "Start time equal to end time should throw IllegalArgumentException");
    }

    @Test
    void testToString() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.FRIDAY,
            LocalTime.of(13, 0),
            LocalTime.of(14, 0),
            "Professional Development",
            true
        );

        // Act
        String toString = block.toString();

        // Assert
        assertEquals(block.getDisplayString(), toString,
            "toString() should match getDisplayString()");
    }

    @Test
    void testNoArgsConstructor() {
        // Act
        UnavailableTimeBlock block = new UnavailableTimeBlock();

        // Assert
        assertNotNull(block, "No-args constructor should create instance");
        assertNull(block.getDayOfWeek(), "Day should be null");
        assertNull(block.getStartTime(), "Start time should be null");
        assertNull(block.getEndTime(), "End time should be null");
        assertNull(block.getReason(), "Reason should be null");
        assertTrue(block.isRecurring(), "Recurring should default to true");
    }

    @Test
    void testAllArgsConstructor() {
        // Act
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.THURSDAY,
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            "Test Meeting",
            false
        );

        // Assert
        assertEquals(DayOfWeek.THURSDAY, block.getDayOfWeek());
        assertEquals(LocalTime.of(11, 0), block.getStartTime());
        assertEquals(LocalTime.of(12, 0), block.getEndTime());
        assertEquals("Test Meeting", block.getReason());
        assertFalse(block.isRecurring());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock();

        // Act
        block.setDayOfWeek(DayOfWeek.SATURDAY);
        block.setStartTime(LocalTime.of(8, 0));
        block.setEndTime(LocalTime.of(12, 0));
        block.setReason("Weekend Workshop");
        block.setRecurring(false);

        // Assert
        assertEquals(DayOfWeek.SATURDAY, block.getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), block.getStartTime());
        assertEquals(LocalTime.of(12, 0), block.getEndTime());
        assertEquals("Weekend Workshop", block.getReason());
        assertFalse(block.isRecurring());
    }
}
