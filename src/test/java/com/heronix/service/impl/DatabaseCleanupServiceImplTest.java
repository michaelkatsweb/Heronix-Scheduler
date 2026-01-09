package com.heronix.service.impl;

import com.heronix.model.domain.Schedule;
import com.heronix.model.domain.ScheduleSlot;
import com.heronix.model.enums.ScheduleStatus;
import com.heronix.repository.ScheduleRepository;
import com.heronix.repository.ScheduleSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for DatabaseCleanupServiceImpl
 *
 * Service: 26th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/DatabaseCleanupServiceImplTest.java
 *
 * Tests cover:
 * - Purge schedules before date
 * - Purge schedules by status
 * - Archive schedules before date
 * - Cleanup orphaned slots
 * - Full cleanup operation
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class DatabaseCleanupServiceImplTest {

    @Mock(lenient = true)
    private ScheduleRepository scheduleRepository;

    @Mock(lenient = true)
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private DatabaseCleanupServiceImpl service;

    private Schedule testSchedule;
    private ScheduleSlot testSlot;

    @BeforeEach
    void setUp() {
        // Create test schedule slot
        testSlot = new ScheduleSlot();
        testSlot.setId(1L);

        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setName("Test Schedule");
        testSchedule.setStatus(ScheduleStatus.PUBLISHED);
        testSchedule.setStartDate(LocalDate.of(2023, 1, 1));
        testSchedule.setEndDate(LocalDate.of(2023, 6, 30));
        testSchedule.setCreatedDate(LocalDate.of(2023, 1, 1));
        testSchedule.setSlots(new ArrayList<>(Arrays.asList(testSlot)));
        testSlot.setSchedule(testSchedule);
    }

    // ========== PURGE SCHEDULES BEFORE DATE TESTS ==========

    @Test
    void testPurgeSchedulesBefore_WithOldSchedules_ShouldDelete() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule oldSchedule = new Schedule();
        oldSchedule.setEndDate(LocalDate.of(2023, 12, 31));
        oldSchedule.setSlots(Arrays.asList(testSlot));

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(oldSchedule));

        int result = service.purgeSchedulesBefore(cutoffDate);

        assertEquals(1, result);
        verify(scheduleSlotRepository).deleteAll(anyList());
        verify(scheduleRepository).deleteAll(anyList());
    }

    @Test
    void testPurgeSchedulesBefore_WithNoOldSchedules_ShouldDeleteNone() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule newSchedule = new Schedule();
        newSchedule.setEndDate(LocalDate.of(2024, 6, 30));

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(newSchedule));

        int result = service.purgeSchedulesBefore(cutoffDate);

        assertEquals(0, result);
        // Note: deleteAll is called even with empty list (implementation behavior)
        verify(scheduleRepository).deleteAll(anyList());
    }

    @Test
    void testPurgeSchedulesBefore_WithNullEndDate_ShouldSkip() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule scheduleWithNullEndDate = new Schedule();
        scheduleWithNullEndDate.setEndDate(null);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(scheduleWithNullEndDate));

        int result = service.purgeSchedulesBefore(cutoffDate);

        assertEquals(0, result);
    }

    @Test
    void testPurgeSchedulesBefore_WithNullSchedule_ShouldNotCrash() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(testSchedule, null));

        int result = service.purgeSchedulesBefore(cutoffDate);

        assertNotNull(result);
        // Should filter out null schedule
        assertTrue(result >= 0);
    }

    @Test
    void testPurgeSchedulesBefore_WithNullSlots_ShouldNotCrash() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule oldSchedule = new Schedule();
        oldSchedule.setEndDate(LocalDate.of(2023, 12, 31));
        oldSchedule.setSlots(null);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(oldSchedule));

        int result = service.purgeSchedulesBefore(cutoffDate);

        assertEquals(1, result);
        verify(scheduleSlotRepository, never()).deleteAll(anyList());
        verify(scheduleRepository).deleteAll(anyList());
    }

    // ========== PURGE SCHEDULES BY STATUS TESTS ==========

    @Test
    void testPurgeSchedulesByStatus_WithMatchingSchedules_ShouldDelete() {
        Schedule draftSchedule = new Schedule();
        draftSchedule.setStatus(ScheduleStatus.DRAFT);
        draftSchedule.setSlots(Arrays.asList(testSlot));

        when(scheduleRepository.findByStatus(ScheduleStatus.DRAFT)).thenReturn(Arrays.asList(draftSchedule));

        int result = service.purgeSchedulesByStatus(ScheduleStatus.DRAFT);

        assertEquals(1, result);
        verify(scheduleSlotRepository).deleteAll(anyList());
        verify(scheduleRepository).deleteAll(anyList());
    }

    @Test
    void testPurgeSchedulesByStatus_WithNoMatches_ShouldDeleteNone() {
        when(scheduleRepository.findByStatus(ScheduleStatus.REVIEW)).thenReturn(Collections.emptyList());

        int result = service.purgeSchedulesByStatus(ScheduleStatus.REVIEW);

        assertEquals(0, result);
        // Note: deleteAll is called even with empty list (implementation behavior)
        verify(scheduleSlotRepository, never()).deleteAll(anyList());  // No slots to delete
        verify(scheduleRepository).deleteAll(anyList());  // Called with empty list
    }

    @Test
    void testPurgeSchedulesByStatus_WithMultipleSchedules_ShouldDeleteAll() {
        Schedule draft1 = new Schedule();
        draft1.setStatus(ScheduleStatus.DRAFT);
        draft1.setSlots(Arrays.asList(testSlot));

        Schedule draft2 = new Schedule();
        draft2.setStatus(ScheduleStatus.DRAFT);
        draft2.setSlots(new ArrayList<>());

        when(scheduleRepository.findByStatus(ScheduleStatus.DRAFT)).thenReturn(Arrays.asList(draft1, draft2));

        int result = service.purgeSchedulesByStatus(ScheduleStatus.DRAFT);

        assertEquals(2, result);
        verify(scheduleSlotRepository, times(2)).deleteAll(anyList());
        verify(scheduleRepository).deleteAll(anyList());
    }

    @Test
    void testPurgeSchedulesByStatus_WithNullSchedule_ShouldNotCrash() {
        when(scheduleRepository.findByStatus(ScheduleStatus.DRAFT))
                .thenReturn(Arrays.asList(testSchedule, null));

        int result = service.purgeSchedulesByStatus(ScheduleStatus.DRAFT);

        assertNotNull(result);
        assertTrue(result >= 0);
    }

    // ========== ARCHIVE SCHEDULES BEFORE DATE TESTS ==========

    @Test
    void testArchiveSchedulesBefore_WithOldSchedules_ShouldArchive() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule oldSchedule = new Schedule();
        oldSchedule.setEndDate(LocalDate.of(2023, 12, 31));
        oldSchedule.setStatus(ScheduleStatus.PUBLISHED);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(oldSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> i.getArgument(0));

        int result = service.archiveSchedulesBefore(cutoffDate);

        assertEquals(1, result);
        verify(scheduleRepository).save(oldSchedule);
        assertEquals(ScheduleStatus.ARCHIVED, oldSchedule.getStatus());
    }

    @Test
    void testArchiveSchedulesBefore_WithAlreadyArchived_ShouldSkip() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule archivedSchedule = new Schedule();
        archivedSchedule.setEndDate(LocalDate.of(2023, 12, 31));
        archivedSchedule.setStatus(ScheduleStatus.ARCHIVED);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(archivedSchedule));

        int result = service.archiveSchedulesBefore(cutoffDate);

        assertEquals(0, result);
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void testArchiveSchedulesBefore_WithNullEndDate_ShouldSkip() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule scheduleWithNullEndDate = new Schedule();
        scheduleWithNullEndDate.setEndDate(null);
        scheduleWithNullEndDate.setStatus(ScheduleStatus.PUBLISHED);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(scheduleWithNullEndDate));

        int result = service.archiveSchedulesBefore(cutoffDate);

        assertEquals(0, result);
    }

    @Test
    void testArchiveSchedulesBefore_WithNullStatus_ShouldSkip() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule scheduleWithNullStatus = new Schedule();
        scheduleWithNullStatus.setEndDate(LocalDate.of(2023, 12, 31));
        scheduleWithNullStatus.setStatus(null);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(scheduleWithNullStatus));

        int result = service.archiveSchedulesBefore(cutoffDate);

        assertEquals(0, result);
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void testArchiveSchedulesBefore_WithNullSchedule_ShouldNotCrash() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(testSchedule, null));

        int result = service.archiveSchedulesBefore(cutoffDate);

        assertNotNull(result);
        assertTrue(result >= 0);
    }

    // ========== CLEANUP ORPHANED SLOTS TESTS ==========

    @Test
    void testCleanupOrphanedSlots_WithOrphanedSlots_ShouldDelete() {
        ScheduleSlot orphaned1 = new ScheduleSlot();
        orphaned1.setSchedule(null);

        ScheduleSlot orphaned2 = new ScheduleSlot();
        orphaned2.setSchedule(null);

        ScheduleSlot notOrphaned = new ScheduleSlot();
        notOrphaned.setSchedule(testSchedule);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(orphaned1, orphaned2, notOrphaned));

        int result = service.cleanupOrphanedSlots();

        assertEquals(2, result);
        verify(scheduleSlotRepository).deleteAll(anyList());
    }

    @Test
    void testCleanupOrphanedSlots_WithNoOrphanedSlots_ShouldDeleteNone() {
        ScheduleSlot slot1 = new ScheduleSlot();
        slot1.setSchedule(testSchedule);

        ScheduleSlot slot2 = new ScheduleSlot();
        slot2.setSchedule(testSchedule);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(slot1, slot2));

        int result = service.cleanupOrphanedSlots();

        assertEquals(0, result);
        // Note: deleteAll is called even with empty list (implementation behavior)
        verify(scheduleSlotRepository).deleteAll(anyList());
    }

    @Test
    void testCleanupOrphanedSlots_WithNullSlot_ShouldNotCrash() {
        ScheduleSlot orphaned = new ScheduleSlot();
        orphaned.setSchedule(null);

        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(orphaned, null));

        int result = service.cleanupOrphanedSlots();

        assertEquals(1, result);
        verify(scheduleSlotRepository).deleteAll(anyList());
    }

    @Test
    void testCleanupOrphanedSlots_WithEmptyList_ShouldReturnZero() {
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        int result = service.cleanupOrphanedSlots();

        assertEquals(0, result);
        // Note: deleteAll is called even with empty list (implementation behavior)
        verify(scheduleSlotRepository).deleteAll(anyList());
    }

    // ========== FULL CLEANUP TESTS ==========

    @Test
    void testPerformFullCleanup_ShouldExecuteAllCleanupOperations() {
        // Setup old schedules
        Schedule oldPublished = new Schedule();
        oldPublished.setEndDate(LocalDate.now().minusYears(2));
        oldPublished.setStatus(ScheduleStatus.PUBLISHED);

        Schedule oldDraft = new Schedule();
        oldDraft.setStatus(ScheduleStatus.DRAFT);
        oldDraft.setCreatedDate(LocalDate.now().minusMonths(7));
        oldDraft.setSlots(new ArrayList<>());

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(oldPublished, oldDraft));
        when(scheduleRepository.findByStatus(ScheduleStatus.DRAFT)).thenReturn(Arrays.asList(oldDraft));
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            service.performFullCleanup();
        });

        // Verify cleanup operations were called
        verify(scheduleRepository, atLeastOnce()).findAll();
        verify(scheduleSlotRepository, atLeastOnce()).findAll();
    }

    @Test
    void testPerformFullCleanup_WithNoDataToClean_ShouldNotCrash() {
        when(scheduleRepository.findAll()).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByStatus(any())).thenReturn(Collections.emptyList());
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            service.performFullCleanup();
        });
    }

    @Test
    void testPerformFullCleanup_WithNullSlots_ShouldNotCrash() {
        Schedule oldDraft = new Schedule();
        oldDraft.setStatus(ScheduleStatus.DRAFT);
        oldDraft.setCreatedDate(LocalDate.now().minusMonths(7));
        oldDraft.setSlots(null);

        when(scheduleRepository.findAll()).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByStatus(ScheduleStatus.DRAFT)).thenReturn(Arrays.asList(oldDraft));
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            service.performFullCleanup();
        });
    }

    // ========== EDGE CASES ==========

    @Test
    void testPurgeSchedulesBefore_WithMultipleOldSchedules_ShouldDeleteAll() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule old1 = new Schedule();
        old1.setEndDate(LocalDate.of(2023, 6, 30));
        old1.setSlots(new ArrayList<>());

        Schedule old2 = new Schedule();
        old2.setEndDate(LocalDate.of(2023, 12, 31));
        old2.setSlots(new ArrayList<>());

        Schedule recent = new Schedule();
        recent.setEndDate(LocalDate.of(2024, 6, 30));

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(old1, old2, recent));

        int result = service.purgeSchedulesBefore(cutoffDate);

        assertEquals(2, result);
        verify(scheduleRepository).deleteAll(anyList());
    }

    @Test
    void testArchiveSchedulesBefore_WithMixedStatuses_ShouldArchiveOnlyNonArchived() {
        LocalDate cutoffDate = LocalDate.of(2024, 1, 1);

        Schedule oldPublished = new Schedule();
        oldPublished.setEndDate(LocalDate.of(2023, 12, 31));
        oldPublished.setStatus(ScheduleStatus.PUBLISHED);

        Schedule oldArchived = new Schedule();
        oldArchived.setEndDate(LocalDate.of(2023, 12, 31));
        oldArchived.setStatus(ScheduleStatus.ARCHIVED);

        when(scheduleRepository.findAll()).thenReturn(Arrays.asList(oldPublished, oldArchived));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> i.getArgument(0));

        int result = service.archiveSchedulesBefore(cutoffDate);

        assertEquals(1, result);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }
}
