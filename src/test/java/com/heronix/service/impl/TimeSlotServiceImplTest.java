package com.heronix.service.impl;

import com.heronix.model.domain.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for TimeSlotServiceImpl
 *
 * Tests in-memory time slot management for scheduling.
 * TimeSlot is not a JPA entity - managed in-memory by the service.
 */
class TimeSlotServiceImplTest {

    private TimeSlotServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TimeSlotServiceImpl();
    }

    // ========== GET ALL TESTS ==========

    @Test
    void testGetAllTimeSlots_ShouldReturnDefaultSlots() {
        List<TimeSlot> result = service.getAllTimeSlots();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Default should include Mon-Fri, 8 periods each = 40 slots
        assertTrue(result.size() >= 40);
    }

    @Test
    void testGetAllTimeSlots_ShouldIncludeAllWeekdays() {
        List<TimeSlot> result = service.getAllTimeSlots();

        assertNotNull(result);
        // Should have slots for Monday through Friday
        assertTrue(result.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.MONDAY));
        assertTrue(result.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.TUESDAY));
        assertTrue(result.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.WEDNESDAY));
        assertTrue(result.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.THURSDAY));
        assertTrue(result.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.FRIDAY));
    }

    @Test
    void testGetAllTimeSlots_ShouldNotIncludeWeekends() {
        List<TimeSlot> result = service.getAllTimeSlots();

        assertNotNull(result);
        // Should NOT have slots for Saturday or Sunday
        assertFalse(result.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.SATURDAY));
        assertFalse(result.stream().anyMatch(s -> s.getDayOfWeek() == DayOfWeek.SUNDAY));
    }

    // ========== GET BY DAY TESTS ==========

    @Test
    void testGetTimeSlotsByDay_WithMonday_ShouldReturnMondaySlots() {
        List<TimeSlot> result = service.getTimeSlotsByDay(DayOfWeek.MONDAY);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // All returned slots should be for Monday
        assertTrue(result.stream().allMatch(s -> s.getDayOfWeek() == DayOfWeek.MONDAY));
    }

    @Test
    void testGetTimeSlotsByDay_WithTuesday_ShouldReturnTuesdaySlots() {
        List<TimeSlot> result = service.getTimeSlotsByDay(DayOfWeek.TUESDAY);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(s -> s.getDayOfWeek() == DayOfWeek.TUESDAY));
    }

    @Test
    void testGetTimeSlotsByDay_WithSaturday_ShouldReturnEmptyList() {
        List<TimeSlot> result = service.getTimeSlotsByDay(DayOfWeek.SATURDAY);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTimeSlotsByDay_WithSunday_ShouldReturnEmptyList() {
        List<TimeSlot> result = service.getTimeSlotsByDay(DayOfWeek.SUNDAY);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTimeSlotsByDay_WithNull_ShouldReturnEmptyList() {
        List<TimeSlot> result = service.getTimeSlotsByDay(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== CREATE TESTS ==========

    @Test
    void testCreateTimeSlot_WithValidSlot_ShouldAdd() {
        TimeSlot newSlot = new TimeSlot(
            DayOfWeek.MONDAY,
            LocalTime.of(16, 0),
            LocalTime.of(17, 0),
            9
        );

        TimeSlot result = service.createTimeSlot(newSlot);

        assertNotNull(result);
        assertEquals(DayOfWeek.MONDAY, result.getDayOfWeek());
        assertEquals(LocalTime.of(16, 0), result.getStartTime());
    }

    @Test
    void testCreateTimeSlot_WithNullSlot_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.createTimeSlot(null));
    }

    @Test
    void testCreateTimeSlot_ShouldIncreaseCount() {
        int initialCount = service.getAllTimeSlots().size();

        TimeSlot newSlot = new TimeSlot(
            DayOfWeek.FRIDAY,
            LocalTime.of(16, 0),
            LocalTime.of(17, 0),
            9
        );

        service.createTimeSlot(newSlot);

        int newCount = service.getAllTimeSlots().size();
        assertEquals(initialCount + 1, newCount);
    }

    // ========== DELETE TESTS ==========

    @Test
    void testDeleteTimeSlot_WithValidId_ShouldRemove() {
        // Create a specific slot with a period number
        TimeSlot slotToDelete = new TimeSlot(
            DayOfWeek.MONDAY,
            LocalTime.of(16, 0),
            LocalTime.of(17, 0),
            99  // Use unique period number as ID
        );
        service.createTimeSlot(slotToDelete);

        int initialCount = service.getAllTimeSlots().size();

        // Delete by period number (used as ID)
        service.deleteTimeSlot(99L);

        int newCount = service.getAllTimeSlots().size();
        assertEquals(initialCount - 1, newCount);
    }

    @Test
    void testDeleteTimeSlot_WithNullId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.deleteTimeSlot(null));
    }

    @Test
    void testDeleteTimeSlot_WithNonExistentId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.deleteTimeSlot(999999L));
    }

    // ========== FIND BY DAY AND TIME TESTS ==========

    @Test
    void testFindByDayAndTime_WithDefaultSlot_ShouldFind() {
        // Default slots start at 8:00 AM
        Optional<TimeSlot> result = service.findByDayAndTime(
            com.eduscheduler.model.enums.DayOfWeek.MONDAY,
            LocalTime.of(8, 0)
        );

        assertTrue(result.isPresent());
        assertEquals(DayOfWeek.MONDAY, result.get().getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), result.get().getStartTime());
    }

    @Test
    void testFindByDayAndTime_WithNonExistentTime_ShouldReturnEmpty() {
        Optional<TimeSlot> result = service.findByDayAndTime(
            com.eduscheduler.model.enums.DayOfWeek.MONDAY,
            LocalTime.of(23, 0)  // Not a default time
        );

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByDayAndTime_WithWeekend_ShouldReturnEmpty() {
        Optional<TimeSlot> result = service.findByDayAndTime(
            com.eduscheduler.model.enums.DayOfWeek.SATURDAY,
            LocalTime.of(8, 0)
        );

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByDayAndTime_WithNullDay_ShouldReturnEmpty() {
        Optional<TimeSlot> result = service.findByDayAndTime(null, LocalTime.of(8, 0));

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByDayAndTime_WithNullTime_ShouldReturnEmpty() {
        Optional<TimeSlot> result = service.findByDayAndTime(
            com.eduscheduler.model.enums.DayOfWeek.MONDAY,
            null
        );

        assertFalse(result.isPresent());
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testCreateAndRetrieve_ShouldMaintainData() {
        TimeSlot created = service.createTimeSlot(new TimeSlot(
            DayOfWeek.WEDNESDAY,
            LocalTime.of(17, 0),
            LocalTime.of(18, 0),
            10
        ));

        Optional<TimeSlot> found = service.findByDayAndTime(
            com.eduscheduler.model.enums.DayOfWeek.WEDNESDAY,
            LocalTime.of(17, 0)
        );

        assertTrue(found.isPresent());
        assertEquals(created.getDayOfWeek(), found.get().getDayOfWeek());
        assertEquals(created.getStartTime(), found.get().getStartTime());
    }

    @Test
    void testMultipleDays_ShouldFilterCorrectly() {
        List<TimeSlot> mondaySlots = service.getTimeSlotsByDay(DayOfWeek.MONDAY);
        List<TimeSlot> tuesdaySlots = service.getTimeSlotsByDay(DayOfWeek.TUESDAY);

        assertFalse(mondaySlots.isEmpty());
        assertFalse(tuesdaySlots.isEmpty());

        // Verify no overlap
        assertTrue(mondaySlots.stream().allMatch(s -> s.getDayOfWeek() == DayOfWeek.MONDAY));
        assertTrue(tuesdaySlots.stream().allMatch(s -> s.getDayOfWeek() == DayOfWeek.TUESDAY));
    }

    @Test
    void testDefaultTimeSlots_ShouldHaveValidStructure() {
        List<TimeSlot> allSlots = service.getAllTimeSlots();

        for (TimeSlot slot : allSlots) {
            assertNotNull(slot.getDayOfWeek());
            assertNotNull(slot.getStartTime());
            assertNotNull(slot.getEndTime());
            assertTrue(slot.getEndTime().isAfter(slot.getStartTime()));
        }
    }

    @Test
    void testDefaultTimeSlots_ShouldStartAt8AM() {
        List<TimeSlot> mondaySlots = service.getTimeSlotsByDay(DayOfWeek.MONDAY);

        // First slot of the day should start at 8:00 AM
        Optional<TimeSlot> firstSlot = mondaySlots.stream()
            .filter(s -> s.getStartTime().equals(LocalTime.of(8, 0)))
            .findFirst();

        assertTrue(firstSlot.isPresent());
    }

    @Test
    void testDefaultTimeSlots_ShouldHave8Periods() {
        List<TimeSlot> mondaySlots = service.getTimeSlotsByDay(DayOfWeek.MONDAY);

        // Default configuration has 8 periods per day
        assertEquals(8, mondaySlots.size());
    }
}
