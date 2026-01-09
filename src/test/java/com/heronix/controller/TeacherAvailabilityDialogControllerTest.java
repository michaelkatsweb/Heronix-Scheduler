package com.heronix.controller;

import com.heronix.model.domain.Teacher;
import com.heronix.model.dto.UnavailableTimeBlock;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TeacherAvailabilityDialogController
 * Tests teacher unavailable time blocks collection management
 *
 * Note: This is a JavaFX dialog controller. We test the business logic
 * (time block collection management, overlap detection, persistence)
 * rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class TeacherAvailabilityDialogControllerTest {

    @Autowired
    private TeacherRepository teacherRepository;

    private Teacher testTeacher;
    private List<UnavailableTimeBlock> testBlocks;

    @BeforeEach
    public void setup() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setName("Dr. Emily Chen");
        testTeacher.setEmployeeId("AVAIL-T001");
        testTeacher.setActive(true);

        // Initialize with empty unavailable blocks list
        testBlocks = new ArrayList<>();
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        testTeacher = teacherRepository.save(testTeacher);
    }

    // ========== TEACHER UNAVAILABLE BLOCKS - BASIC OPERATIONS ==========

    @Test
    public void testCreateTeacher_EmptyBlocksList() {
        // Assert
        assertNotNull(testTeacher.getUnavailableTimeBlocks());
        assertEquals(0, testTeacher.getUnavailableTimeBlocks().size());

        System.out.println("✓ Teacher created with empty blocks list");
    }

    @Test
    public void testAddSingleBlock_ShouldPersist() {
        // Arrange - Monday 9:00-10:00
        UnavailableTimeBlock block = new UnavailableTimeBlock();
        block.setDayOfWeek(DayOfWeek.MONDAY);
        block.setStartTime(LocalTime.of(9, 0));
        block.setEndTime(LocalTime.of(10, 0));
        block.setReason("Department Meeting");
        block.setRecurring(true);

        // Act
        testBlocks.add(block);
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(1, saved.getUnavailableTimeBlocks().size());
        UnavailableTimeBlock savedBlock = saved.getUnavailableTimeBlocks().get(0);
        assertEquals(DayOfWeek.MONDAY, savedBlock.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), savedBlock.getStartTime());
        assertEquals(LocalTime.of(10, 0), savedBlock.getEndTime());

        System.out.println("✓ Single block added and persisted");
    }

    @Test
    public void testAddMultipleBlocks_ShouldPersist() {
        // Arrange - Add 3 different blocks
        UnavailableTimeBlock block1 = createBlock(DayOfWeek.MONDAY, 9, 10, "Department Meeting");
        UnavailableTimeBlock block2 = createBlock(DayOfWeek.WEDNESDAY, 11, 12, "Lunch Duty");
        UnavailableTimeBlock block3 = createBlock(DayOfWeek.FRIDAY, 14, 15, "IEP Meetings");

        // Act
        testBlocks.add(block1);
        testBlocks.add(block2);
        testBlocks.add(block3);
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(3, saved.getUnavailableTimeBlocks().size());

        System.out.println("✓ Multiple blocks added and persisted");
        System.out.println("  - Block count: " + saved.getUnavailableTimeBlocks().size());
    }

    @Test
    public void testRemoveBlock_ShouldPersist() {
        // Arrange - Add 2 blocks
        UnavailableTimeBlock block1 = createBlock(DayOfWeek.MONDAY, 9, 10, "Department Meeting");
        UnavailableTimeBlock block2 = createBlock(DayOfWeek.WEDNESDAY, 11, 12, "Lunch Duty");
        testBlocks.add(block1);
        testBlocks.add(block2);
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        testTeacher = teacherRepository.save(testTeacher);

        // Act - Remove one block
        testBlocks.remove(0);
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(1, saved.getUnavailableTimeBlocks().size());
        assertEquals(DayOfWeek.WEDNESDAY, saved.getUnavailableTimeBlocks().get(0).getDayOfWeek());

        System.out.println("✓ Block removed and changes persisted");
    }

    @Test
    public void testUpdateBlock_ShouldPersist() {
        // Arrange - Add one block
        UnavailableTimeBlock block = createBlock(DayOfWeek.MONDAY, 9, 10, "Department Meeting");
        testBlocks.add(block);
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        testTeacher = teacherRepository.save(testTeacher);

        // Act - Update the block
        block.setEndTime(LocalTime.of(11, 0));  // Extend to 11:00
        block.setReason("Extended Department Meeting");
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        UnavailableTimeBlock updated = saved.getUnavailableTimeBlocks().get(0);
        assertEquals(LocalTime.of(11, 0), updated.getEndTime());
        assertEquals("Extended Department Meeting", updated.getReason());

        System.out.println("✓ Block updated and changes persisted");
    }

    @Test
    public void testClearAllBlocks_ShouldPersist() {
        // Arrange - Add blocks
        testBlocks.add(createBlock(DayOfWeek.MONDAY, 9, 10, "Meeting"));
        testBlocks.add(createBlock(DayOfWeek.WEDNESDAY, 11, 12, "Duty"));
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        testTeacher = teacherRepository.save(testTeacher);

        // Act - Clear all blocks
        testBlocks.clear();
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(0, saved.getUnavailableTimeBlocks().size());

        System.out.println("✓ All blocks cleared and changes persisted");
    }

    // ========== OVERLAP DETECTION ==========

    @Test
    public void testOverlapDetection_NoOverlap() {
        // Arrange - Two non-overlapping blocks
        UnavailableTimeBlock block1 = createBlock(DayOfWeek.MONDAY, 9, 10, "Meeting 1");
        UnavailableTimeBlock block2 = createBlock(DayOfWeek.MONDAY, 10, 11, "Meeting 2");

        // Act
        boolean overlaps = block1.overlaps(block2);

        // Assert
        assertFalse(overlaps);

        System.out.println("✓ Adjacent blocks don't overlap");
    }

    @Test
    public void testOverlapDetection_WithOverlap() {
        // Arrange - Two overlapping blocks
        UnavailableTimeBlock block1 = createBlock(DayOfWeek.MONDAY, 9, 10, "Meeting 1");
        UnavailableTimeBlock block2 = createBlock(DayOfWeek.MONDAY, 9, 30, 10, 30, "Meeting 2");

        // Act
        boolean overlaps = block1.overlaps(block2);

        // Assert
        assertTrue(overlaps);

        System.out.println("✓ Overlapping blocks detected");
    }

    @Test
    public void testOverlapDetection_DifferentDays() {
        // Arrange - Same times, different days
        UnavailableTimeBlock block1 = createBlock(DayOfWeek.MONDAY, 9, 10, "Meeting 1");
        UnavailableTimeBlock block2 = createBlock(DayOfWeek.TUESDAY, 9, 10, "Meeting 2");

        // Act
        boolean overlaps = block1.overlaps(block2);

        // Assert
        assertFalse(overlaps);

        System.out.println("✓ Different days don't overlap");
    }

    // ========== BLOCK COUNT ==========

    @Test
    public void testGetBlockCount() {
        // Arrange
        testBlocks.add(createBlock(DayOfWeek.MONDAY, 9, 10, "Meeting"));
        testBlocks.add(createBlock(DayOfWeek.WEDNESDAY, 11, 12, "Duty"));
        testBlocks.add(createBlock(DayOfWeek.FRIDAY, 14, 15, "IEP"));
        testTeacher.setUnavailableTimeBlocks(testBlocks);

        // Act
        int count = testTeacher.getUnavailableTimeBlocks().size();

        // Assert
        assertEquals(3, count);

        System.out.println("✓ Block count: " + count);
    }

    // ========== REAL-WORLD SCENARIOS ==========

    @Test
    public void testRealWorld_TypicalTeacherSchedule() {
        // Arrange - Typical weekly unavailability
        testBlocks.add(createBlock(DayOfWeek.MONDAY, 14, 0, 15, 0, "Department Meeting"));
        testBlocks.add(createBlock(DayOfWeek.TUESDAY, 12, 0, 12, 30, "Lunch Duty"));
        testBlocks.add(createBlock(DayOfWeek.WEDNESDAY, 8, 0, 8, 30, "Before School Tutoring"));
        testBlocks.add(createBlock(DayOfWeek.THURSDAY, 15, 0, 16, 0, "Committee Meeting"));
        testBlocks.add(createBlock(DayOfWeek.FRIDAY, 13, 0, 14, 30, "IEP Meetings"));

        // Act
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(5, saved.getUnavailableTimeBlocks().size());

        // Calculate total unavailable hours per week
        long totalMinutes = saved.getUnavailableTimeBlocks().stream()
            .mapToLong(UnavailableTimeBlock::getDurationMinutes)
            .sum();

        System.out.println("✓ Real-world: Typical teacher weekly schedule");
        System.out.println("  - Unavailable blocks: " + saved.getUnavailableTimeBlocks().size());
        System.out.println("  - Total unavailable time: " + totalMinutes + " minutes/week");
        System.out.println("  - Hours: " + (totalMinutes / 60.0) + " hours/week");

        assertTrue(totalMinutes > 0);
    }

    @Test
    public void testRealWorld_CoachWithPractices() {
        // Arrange - Teacher who coaches after school
        testBlocks.add(createBlock(DayOfWeek.MONDAY, 15, 30, 17, 30, "Soccer Practice"));
        testBlocks.add(createBlock(DayOfWeek.TUESDAY, 15, 30, 17, 30, "Soccer Practice"));
        testBlocks.add(createBlock(DayOfWeek.WEDNESDAY, 15, 30, 17, 30, "Soccer Practice"));
        testBlocks.add(createBlock(DayOfWeek.THURSDAY, 15, 30, 17, 30, "Soccer Practice"));

        // Act
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(4, saved.getUnavailableTimeBlocks().size());

        // All blocks should be 2 hours (120 minutes)
        boolean allTwoHours = saved.getUnavailableTimeBlocks().stream()
            .allMatch(block -> block.getDurationMinutes() == 120);

        assertTrue(allTwoHours);

        System.out.println("✓ Real-world: Teacher coaching after school");
        System.out.println("  - Practice days: " + saved.getUnavailableTimeBlocks().size());
    }

    @Test
    public void testRealWorld_SpedTeacherWithIEPs() {
        // Arrange - Special Ed teacher with IEP meetings
        testBlocks.add(createBlock(DayOfWeek.MONDAY, 13, 0, 14, 30, "IEP Meeting - Student A"));
        testBlocks.add(createBlock(DayOfWeek.TUESDAY, 10, 0, 11, 0, "IEP Meeting - Student B"));
        testBlocks.add(createBlock(DayOfWeek.WEDNESDAY, 14, 0, 15, 30, "IEP Meeting - Student C"));
        testBlocks.add(createBlock(DayOfWeek.THURSDAY, 9, 0, 10, 30, "IEP Meeting - Student D"));
        testBlocks.add(createBlock(DayOfWeek.FRIDAY, 13, 0, 14, 0, "IEP Team Meeting"));

        // Act
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(5, saved.getUnavailableTimeBlocks().size());

        System.out.println("✓ Real-world: Special Ed teacher with IEP meetings");
        System.out.println("  - IEP meetings per week: " + saved.getUnavailableTimeBlocks().size());
    }

    @Test
    public void testRealWorld_NewTeacher_NoBlocks() {
        // Arrange - New teacher with no unavailability yet

        // Assert
        assertEquals(0, testTeacher.getUnavailableTimeBlocks().size());

        System.out.println("✓ Real-world: New teacher with no unavailable blocks");
    }

    // ========== PERSISTENCE AND RELOAD ==========

    @Test
    public void testPersistence_SaveAndReload() {
        // Arrange - Add blocks
        testBlocks.add(createBlock(DayOfWeek.MONDAY, 9, 10, "Meeting"));
        testBlocks.add(createBlock(DayOfWeek.WEDNESDAY, 11, 12, "Duty"));
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);
        Long teacherId = saved.getId();

        // Act - Reload from database
        Teacher reloaded = teacherRepository.findById(teacherId).orElse(null);

        // Assert
        assertNotNull(reloaded);
        assertEquals(2, reloaded.getUnavailableTimeBlocks().size());
        assertEquals(DayOfWeek.MONDAY, reloaded.getUnavailableTimeBlocks().get(0).getDayOfWeek());

        System.out.println("✓ Blocks persist across save/reload");
    }

    @Test
    public void testPersistence_UpdateExistingBlocks() {
        // Arrange - Save initial blocks
        testBlocks.add(createBlock(DayOfWeek.MONDAY, 9, 10, "Meeting"));
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        testTeacher = teacherRepository.save(testTeacher);

        // Act - Modify and save again
        testBlocks.add(createBlock(DayOfWeek.FRIDAY, 14, 15, "New Meeting"));
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher updated = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(2, updated.getUnavailableTimeBlocks().size());

        System.out.println("✓ Block list updates persist");
    }

    // ========== EDGE CASES ==========

    @Test
    public void testEdgeCase_SetNullBlocksList() {
        // Arrange
        testTeacher.setUnavailableTimeBlocks(null);

        // Act
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert - Should handle null gracefully
        // (Implementation may return empty list or null)
        List<UnavailableTimeBlock> blocks = saved.getUnavailableTimeBlocks();
        assertTrue(blocks == null || blocks.isEmpty());

        System.out.println("✓ Null blocks list handled gracefully");
    }

    @Test
    public void testEdgeCase_MaxBlocks() {
        // Arrange - Add many blocks (stress test)
        for (DayOfWeek day : DayOfWeek.values()) {
            testBlocks.add(createBlock(day, 9, 10, "Morning Meeting"));
            testBlocks.add(createBlock(day, 12, 13, "Lunch"));
            testBlocks.add(createBlock(day, 15, 16, "Afternoon Meeting"));
        }

        // Act
        testTeacher.setUnavailableTimeBlocks(testBlocks);
        Teacher saved = teacherRepository.save(testTeacher);

        // Assert
        assertEquals(21, saved.getUnavailableTimeBlocks().size());  // 7 days * 3 blocks

        System.out.println("✓ Large number of blocks handled correctly");
        System.out.println("  - Total blocks: " + saved.getUnavailableTimeBlocks().size());
    }

    // ========== HELPER METHODS ==========

    private UnavailableTimeBlock createBlock(DayOfWeek day, int startHour, int endHour, String reason) {
        return createBlock(day, startHour, 0, endHour, 0, reason);
    }

    private UnavailableTimeBlock createBlock(DayOfWeek day, int startHour, int startMin,
                                            int endHour, int endMin, String reason) {
        UnavailableTimeBlock block = new UnavailableTimeBlock();
        block.setDayOfWeek(day);
        block.setStartTime(LocalTime.of(startHour, startMin));
        block.setEndTime(LocalTime.of(endHour, endMin));
        block.setReason(reason);
        block.setRecurring(true);
        return block;
    }
}
