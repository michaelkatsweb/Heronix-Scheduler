package com.heronix.controller;

import com.heronix.model.dto.UnavailableTimeBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UnavailableTimeBlockDialogController
 * Tests unavailable time block DTO business logic
 *
 * Note: This is a JavaFX dialog controller. We test the DTO business logic
 * (time block validation, overlap detection, contains checks) rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
public class UnavailableTimeBlockDialogControllerTest {

    private UnavailableTimeBlock testBlock;

    @BeforeEach
    public void setup() {
        // Create test unavailable time block
        // Monday 9:00 AM - 10:00 AM (Department Meeting)
        testBlock = new UnavailableTimeBlock();
        testBlock.setDayOfWeek(DayOfWeek.MONDAY);
        testBlock.setStartTime(LocalTime.of(9, 0));
        testBlock.setEndTime(LocalTime.of(10, 0));
        testBlock.setReason("Department Meeting");
        testBlock.setRecurring(true);
    }

    // ========== BASIC CREATION AND PROPERTIES ==========

    @Test
    public void testCreateTimeBlock_ShouldSetProperties() {
        // Assert
        assertNotNull(testBlock);
        assertEquals(DayOfWeek.MONDAY, testBlock.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), testBlock.getStartTime());
        assertEquals(LocalTime.of(10, 0), testBlock.getEndTime());
        assertEquals("Department Meeting", testBlock.getReason());
        assertTrue(testBlock.isRecurring());

        System.out.println("✓ Time block created with properties");
        System.out.println("  - Day: " + testBlock.getDayOfWeek());
        System.out.println("  - Time: " + testBlock.getStartTime() + " - " + testBlock.getEndTime());
    }

    @Test
    public void testRecurring_DefaultIsTrue() {
        // Arrange
        UnavailableTimeBlock block = new UnavailableTimeBlock();
        block.setDayOfWeek(DayOfWeek.TUESDAY);
        block.setStartTime(LocalTime.of(10, 0));
        block.setEndTime(LocalTime.of(11, 0));

        // Assert
        assertTrue(block.isRecurring());

        System.out.println("✓ Recurring defaults to true");
    }

    @Test
    public void testReason_CanBeNull() {
        // Arrange
        testBlock.setReason(null);

        // Assert
        assertNull(testBlock.getReason());

        System.out.println("✓ Reason can be null (optional)");
    }

    // ========== TIME VALIDATION ==========

    @Test
    public void testValidate_ValidBlock_ShouldPass() {
        // Act & Assert
        assertTrue(testBlock.validate());

        System.out.println("✓ Valid time block passes validation");
    }

    @Test
    public void testValidate_NullDay_ShouldThrow() {
        // Arrange
        testBlock.setDayOfWeek(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            testBlock.validate();
        });

        assertTrue(exception.getMessage().contains("Day of week cannot be null"));

        System.out.println("✓ Null day validation throws exception");
    }

    @Test
    public void testValidate_NullStartTime_ShouldThrow() {
        // Arrange
        testBlock.setStartTime(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            testBlock.validate();
        });

        assertTrue(exception.getMessage().contains("Start time and end time cannot be null"));

        System.out.println("✓ Null start time validation throws exception");
    }

    @Test
    public void testValidate_NullEndTime_ShouldThrow() {
        // Arrange
        testBlock.setEndTime(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            testBlock.validate();
        });

        assertTrue(exception.getMessage().contains("Start time and end time cannot be null"));

        System.out.println("✓ Null end time validation throws exception");
    }

    @Test
    public void testValidate_StartAfterEnd_ShouldThrow() {
        // Arrange
        testBlock.setStartTime(LocalTime.of(10, 0));
        testBlock.setEndTime(LocalTime.of(9, 0));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            testBlock.validate();
        });

        assertTrue(exception.getMessage().contains("Start time") &&
                   exception.getMessage().contains("must be before end time"));

        System.out.println("✓ Start after end validation throws exception");
    }

    @Test
    public void testValidate_StartEqualsEnd_ShouldThrow() {
        // Arrange
        testBlock.setStartTime(LocalTime.of(9, 0));
        testBlock.setEndTime(LocalTime.of(9, 0));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            testBlock.validate();
        });

        assertTrue(exception.getMessage().contains("Start time") &&
                   exception.getMessage().contains("must be before end time"));

        System.out.println("✓ Start equals end validation throws exception");
    }

    // ========== CONTAINS CHECKS ==========

    @Test
    public void testContains_TimeWithinBlock_ShouldReturnTrue() {
        // Arrange - Block is Monday 9:00-10:00
        LocalTime timeWithin = LocalTime.of(9, 30);

        // Act
        boolean contains = testBlock.contains(DayOfWeek.MONDAY, timeWithin);

        // Assert
        assertTrue(contains);

        System.out.println("✓ Contains check: time within block returns true");
    }

    @Test
    public void testContains_TimeAtStart_ShouldReturnTrue() {
        // Arrange - Block is Monday 9:00-10:00
        LocalTime timeAtStart = LocalTime.of(9, 0);

        // Act
        boolean contains = testBlock.contains(DayOfWeek.MONDAY, timeAtStart);

        // Assert
        assertTrue(contains);

        System.out.println("✓ Contains check: time at start boundary returns true");
    }

    @Test
    public void testContains_TimeAtEnd_ShouldReturnFalse() {
        // Arrange - Block is Monday 9:00-10:00 (end is exclusive)
        LocalTime timeAtEnd = LocalTime.of(10, 0);

        // Act
        boolean contains = testBlock.contains(DayOfWeek.MONDAY, timeAtEnd);

        // Assert
        assertFalse(contains);

        System.out.println("✓ Contains check: time at end boundary returns false (exclusive)");
    }

    @Test
    public void testContains_TimeBeforeBlock_ShouldReturnFalse() {
        // Arrange - Block is Monday 9:00-10:00
        LocalTime timeBefore = LocalTime.of(8, 30);

        // Act
        boolean contains = testBlock.contains(DayOfWeek.MONDAY, timeBefore);

        // Assert
        assertFalse(contains);

        System.out.println("✓ Contains check: time before block returns false");
    }

    @Test
    public void testContains_TimeAfterBlock_ShouldReturnFalse() {
        // Arrange - Block is Monday 9:00-10:00
        LocalTime timeAfter = LocalTime.of(10, 30);

        // Act
        boolean contains = testBlock.contains(DayOfWeek.MONDAY, timeAfter);

        // Assert
        assertFalse(contains);

        System.out.println("✓ Contains check: time after block returns false");
    }

    @Test
    public void testContains_DifferentDay_ShouldReturnFalse() {
        // Arrange - Block is Monday 9:00-10:00
        LocalTime timeWithin = LocalTime.of(9, 30);

        // Act
        boolean contains = testBlock.contains(DayOfWeek.TUESDAY, timeWithin);

        // Assert
        assertFalse(contains);

        System.out.println("✓ Contains check: different day returns false");
    }

    // ========== OVERLAP DETECTION ==========

    @Test
    public void testOverlaps_IdenticalBlocks_ShouldReturnTrue() {
        // Arrange
        UnavailableTimeBlock other = new UnavailableTimeBlock();
        other.setDayOfWeek(DayOfWeek.MONDAY);
        other.setStartTime(LocalTime.of(9, 0));
        other.setEndTime(LocalTime.of(10, 0));

        // Act
        boolean overlaps = testBlock.overlaps(other);

        // Assert
        assertTrue(overlaps);

        System.out.println("✓ Overlap check: identical blocks overlap");
    }

    @Test
    public void testOverlaps_PartialOverlap_ShouldReturnTrue() {
        // Arrange - Block is Monday 9:00-10:00
        UnavailableTimeBlock other = new UnavailableTimeBlock();
        other.setDayOfWeek(DayOfWeek.MONDAY);
        other.setStartTime(LocalTime.of(9, 30));
        other.setEndTime(LocalTime.of(10, 30));

        // Act
        boolean overlaps = testBlock.overlaps(other);

        // Assert
        assertTrue(overlaps);

        System.out.println("✓ Overlap check: partial overlap returns true");
    }

    @Test
    public void testOverlaps_OneContainsOther_ShouldReturnTrue() {
        // Arrange - Block is Monday 9:00-10:00
        UnavailableTimeBlock other = new UnavailableTimeBlock();
        other.setDayOfWeek(DayOfWeek.MONDAY);
        other.setStartTime(LocalTime.of(9, 15));
        other.setEndTime(LocalTime.of(9, 45));

        // Act
        boolean overlaps = testBlock.overlaps(other);

        // Assert
        assertTrue(overlaps);

        System.out.println("✓ Overlap check: one block contains other returns true");
    }

    @Test
    public void testOverlaps_NoOverlap_ShouldReturnFalse() {
        // Arrange - Block is Monday 9:00-10:00
        UnavailableTimeBlock other = new UnavailableTimeBlock();
        other.setDayOfWeek(DayOfWeek.MONDAY);
        other.setStartTime(LocalTime.of(10, 0));
        other.setEndTime(LocalTime.of(11, 0));

        // Act
        boolean overlaps = testBlock.overlaps(other);

        // Assert
        assertFalse(overlaps);

        System.out.println("✓ Overlap check: adjacent blocks don't overlap");
    }

    @Test
    public void testOverlaps_DifferentDay_ShouldReturnFalse() {
        // Arrange - Block is Monday 9:00-10:00
        UnavailableTimeBlock other = new UnavailableTimeBlock();
        other.setDayOfWeek(DayOfWeek.TUESDAY);
        other.setStartTime(LocalTime.of(9, 0));
        other.setEndTime(LocalTime.of(10, 0));

        // Act
        boolean overlaps = testBlock.overlaps(other);

        // Assert
        assertFalse(overlaps);

        System.out.println("✓ Overlap check: different days don't overlap");
    }

    // ========== DURATION CALCULATION ==========

    @Test
    public void testGetDurationMinutes_ShouldCalculateCorrectly() {
        // Arrange - Block is Monday 9:00-10:00 (60 minutes)

        // Act
        long duration = testBlock.getDurationMinutes();

        // Assert
        assertEquals(60, duration);

        System.out.println("✓ Duration calculated correctly: " + duration + " minutes");
    }

    @Test
    public void testGetDurationMinutes_ThirtyMinutes() {
        // Arrange
        testBlock.setStartTime(LocalTime.of(9, 0));
        testBlock.setEndTime(LocalTime.of(9, 30));

        // Act
        long duration = testBlock.getDurationMinutes();

        // Assert
        assertEquals(30, duration);

        System.out.println("✓ 30-minute duration calculated correctly");
    }

    @Test
    public void testGetDurationMinutes_TwoHours() {
        // Arrange
        testBlock.setStartTime(LocalTime.of(9, 0));
        testBlock.setEndTime(LocalTime.of(11, 0));

        // Act
        long duration = testBlock.getDurationMinutes();

        // Assert
        assertEquals(120, duration);

        System.out.println("✓ 2-hour duration calculated correctly");
    }

    // ========== DISPLAY STRING ==========

    @Test
    public void testGetDisplayString_WithReason() {
        // Act
        String display = testBlock.getDisplayString();

        // Assert
        assertNotNull(display);
        assertTrue(display.contains("Monday"));
        assertTrue(display.contains("09:00"));
        assertTrue(display.contains("10:00"));
        assertTrue(display.contains("Department Meeting"));

        System.out.println("✓ Display string with reason: " + display);
    }

    @Test
    public void testGetDisplayString_WithoutReason() {
        // Arrange
        testBlock.setReason(null);

        // Act
        String display = testBlock.getDisplayString();

        // Assert
        assertNotNull(display);
        assertTrue(display.contains("Monday"));
        assertTrue(display.contains("09:00"));
        assertTrue(display.contains("10:00"));
        assertFalse(display.contains("("));

        System.out.println("✓ Display string without reason: " + display);
    }

    @Test
    public void testToString_ShouldMatchDisplayString() {
        // Act
        String toString = testBlock.toString();
        String displayString = testBlock.getDisplayString();

        // Assert
        assertEquals(displayString, toString);

        System.out.println("✓ toString() matches getDisplayString()");
    }

    // ========== REAL-WORLD SCENARIOS ==========

    @Test
    public void testRealWorld_DepartmentMeeting() {
        // Arrange - Every Monday 2:00-3:00 PM
        UnavailableTimeBlock meeting = new UnavailableTimeBlock();
        meeting.setDayOfWeek(DayOfWeek.MONDAY);
        meeting.setStartTime(LocalTime.of(14, 0));
        meeting.setEndTime(LocalTime.of(15, 0));
        meeting.setReason("Department Meeting");
        meeting.setRecurring(true);

        // Act & Assert
        assertTrue(meeting.validate());
        assertTrue(meeting.isRecurring());
        assertEquals(60, meeting.getDurationMinutes());
        assertTrue(meeting.contains(DayOfWeek.MONDAY, LocalTime.of(14, 30)));

        System.out.println("✓ Real-world: Department meeting scenario");
        System.out.println("  - " + meeting.getDisplayString());
    }

    @Test
    public void testRealWorld_LunchDuty() {
        // Arrange - Every Wednesday 11:30-12:30 PM
        UnavailableTimeBlock lunch = new UnavailableTimeBlock();
        lunch.setDayOfWeek(DayOfWeek.WEDNESDAY);
        lunch.setStartTime(LocalTime.of(11, 30));
        lunch.setEndTime(LocalTime.of(12, 30));
        lunch.setReason("Lunch Duty");
        lunch.setRecurring(true);

        // Act & Assert
        assertTrue(lunch.validate());
        assertEquals(60, lunch.getDurationMinutes());

        System.out.println("✓ Real-world: Lunch duty scenario");
        System.out.println("  - " + lunch.getDisplayString());
    }

    @Test
    public void testRealWorld_IEPMeetings() {
        // Arrange - Every Friday 1:00-2:30 PM
        UnavailableTimeBlock iep = new UnavailableTimeBlock();
        iep.setDayOfWeek(DayOfWeek.FRIDAY);
        iep.setStartTime(LocalTime.of(13, 0));
        iep.setEndTime(LocalTime.of(14, 30));
        iep.setReason("IEP Meetings");
        iep.setRecurring(true);

        // Act & Assert
        assertTrue(iep.validate());
        assertEquals(90, iep.getDurationMinutes());

        System.out.println("✓ Real-world: IEP meetings scenario");
        System.out.println("  - " + iep.getDisplayString());
    }

    @Test
    public void testRealWorld_ProfessionalDevelopment() {
        // Arrange - Every other Thursday 3:00-5:00 PM
        UnavailableTimeBlock pd = new UnavailableTimeBlock();
        pd.setDayOfWeek(DayOfWeek.THURSDAY);
        pd.setStartTime(LocalTime.of(15, 0));
        pd.setEndTime(LocalTime.of(17, 0));
        pd.setReason("Professional Development");
        pd.setRecurring(false);  // Not every week

        // Act & Assert
        assertTrue(pd.validate());
        assertFalse(pd.isRecurring());
        assertEquals(120, pd.getDurationMinutes());

        System.out.println("✓ Real-world: Professional development (non-recurring)");
        System.out.println("  - " + pd.getDisplayString());
    }
}
