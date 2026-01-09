package com.heronix.service.impl;

import com.heronix.model.domain.SpecialEventBlock;
import com.heronix.model.domain.SpecialEventBlock.EventBlockType;
import com.heronix.repository.SpecialEventBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for SpecialEventBlockServiceImpl
 *
 * Tests special event block management: IEP meetings, 504 meetings, planning time, etc.
 * Focuses on CRUD operations and filtering logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpecialEventBlockServiceImplTest {

    @Mock
    private SpecialEventBlockRepository repository;

    @InjectMocks
    private SpecialEventBlockServiceImpl service;

    private SpecialEventBlock testEventBlock;

    @BeforeEach
    void setUp() {
        // Create test event block
        testEventBlock = new SpecialEventBlock();
        testEventBlock.setId(1L);
        testEventBlock.setDescription("IEP Meeting for Student");
        testEventBlock.setBlockType(EventBlockType.IEP_MEETING);
        testEventBlock.setDayOfWeek(DayOfWeek.FRIDAY);
        testEventBlock.setStartTime(LocalTime.of(9, 0));
        testEventBlock.setEndTime(LocalTime.of(10, 30));
        testEventBlock.setActive(true);
        testEventBlock.setBlocksTeaching(true);
    }

    // ========== CREATE TESTS ==========

    @Test
    void testCreateEventBlock_WithValidBlock_ShouldSave() {
        when(repository.save(any(SpecialEventBlock.class))).thenReturn(testEventBlock);

        SpecialEventBlock result = service.createEventBlock(testEventBlock);

        assertNotNull(result);
        assertEquals(testEventBlock.getId(), result.getId());
        assertEquals(testEventBlock.getDescription(), result.getDescription());
        verify(repository, times(1)).save(testEventBlock);
    }

    @Test
    void testCreateEventBlock_WithNullBlock_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.createEventBlock(null));
    }

    @Test
    void testCreateEventBlock_WithNullBlockType_ShouldThrowException() {
        testEventBlock.setBlockType(null);

        assertThrows(IllegalArgumentException.class, () ->
            service.createEventBlock(testEventBlock));
    }

    @Test
    void testCreateEventBlock_WithInvalidTimeRange_ShouldThrowException() {
        testEventBlock.setStartTime(LocalTime.of(10, 0));
        testEventBlock.setEndTime(LocalTime.of(9, 0)); // End before start

        assertThrows(IllegalArgumentException.class, () ->
            service.createEventBlock(testEventBlock));
    }

    // ========== UPDATE TESTS ==========

    @Test
    void testUpdateEventBlock_WithValidBlock_ShouldUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(testEventBlock));
        when(repository.save(any(SpecialEventBlock.class))).thenReturn(testEventBlock);

        testEventBlock.setDescription("Updated IEP Meeting");

        SpecialEventBlock result = service.updateEventBlock(1L, testEventBlock);

        assertNotNull(result);
        assertEquals("Updated IEP Meeting", result.getDescription());
        verify(repository, times(1)).save(testEventBlock);
    }

    @Test
    void testUpdateEventBlock_WithNonExistentId_ShouldThrowException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            service.updateEventBlock(999L, testEventBlock));
    }

    @Test
    void testUpdateEventBlock_WithNullBlock_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.updateEventBlock(1L, null));
    }

    @Test
    void testUpdateEventBlock_WithNullId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.updateEventBlock(null, testEventBlock));
    }

    // ========== DELETE TESTS ==========

    @Test
    void testDeleteEventBlock_WithValidId_ShouldDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(testEventBlock));
        doNothing().when(repository).deleteById(1L);

        service.deleteEventBlock(1L);

        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteEventBlock_WithNonExistentId_ShouldThrowException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            service.deleteEventBlock(999L));
    }

    @Test
    void testDeleteEventBlock_WithNullId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.deleteEventBlock(null));
    }

    // ========== GET ALL ACTIVE TESTS ==========

    @Test
    void testGetAllActiveBlocks_ShouldReturnActiveBlocks() {
        when(repository.findByActiveTrue()).thenReturn(Arrays.asList(testEventBlock));

        List<SpecialEventBlock> result = service.getAllActiveBlocks();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
        verify(repository, times(1)).findByActiveTrue();
    }

    @Test
    void testGetAllActiveBlocks_WithNoActiveBlocks_ShouldReturnEmptyList() {
        when(repository.findByActiveTrue()).thenReturn(Collections.emptyList());

        List<SpecialEventBlock> result = service.getAllActiveBlocks();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== GET BY TYPE TESTS ==========

    @Test
    void testGetBlocksByType_WithIEPMeeting_ShouldReturnIEPBlocks() {
        when(repository.findByBlockType(EventBlockType.IEP_MEETING))
            .thenReturn(Arrays.asList(testEventBlock));

        List<SpecialEventBlock> result = service.getBlocksByType(EventBlockType.IEP_MEETING);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventBlockType.IEP_MEETING, result.get(0).getBlockType());
        verify(repository, times(1)).findByBlockType(EventBlockType.IEP_MEETING);
    }

    @Test
    void testGetBlocksByType_With504Meeting_ShouldReturn504Blocks() {
        SpecialEventBlock block504 = new SpecialEventBlock();
        block504.setBlockType(EventBlockType.SECTION_504_MEETING);

        when(repository.findByBlockType(EventBlockType.SECTION_504_MEETING))
            .thenReturn(Arrays.asList(block504));

        List<SpecialEventBlock> result = service.getBlocksByType(EventBlockType.SECTION_504_MEETING);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventBlockType.SECTION_504_MEETING, result.get(0).getBlockType());
    }

    @Test
    void testGetBlocksByType_WithNoMatchingType_ShouldReturnEmptyList() {
        when(repository.findByBlockType(EventBlockType.TEACHER_PLANNING))
            .thenReturn(Collections.emptyList());

        List<SpecialEventBlock> result = service.getBlocksByType(EventBlockType.TEACHER_PLANNING);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetBlocksByType_WithNullType_ShouldReturnEmptyList() {
        List<SpecialEventBlock> result = service.getBlocksByType(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== GET BY DAY TESTS ==========

    @Test
    void testGetBlocksByDay_WithFriday_ShouldReturnFridayBlocks() {
        when(repository.findByDayOfWeek(DayOfWeek.FRIDAY))
            .thenReturn(Arrays.asList(testEventBlock));

        List<SpecialEventBlock> result = service.getBlocksByDay(DayOfWeek.FRIDAY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DayOfWeek.FRIDAY, result.get(0).getDayOfWeek());
        verify(repository, times(1)).findByDayOfWeek(DayOfWeek.FRIDAY);
    }

    @Test
    void testGetBlocksByDay_WithMonday_ShouldReturnMondayBlocks() {
        SpecialEventBlock mondayBlock = new SpecialEventBlock();
        mondayBlock.setDayOfWeek(DayOfWeek.MONDAY);

        when(repository.findByDayOfWeek(DayOfWeek.MONDAY))
            .thenReturn(Arrays.asList(mondayBlock));

        List<SpecialEventBlock> result = service.getBlocksByDay(DayOfWeek.MONDAY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DayOfWeek.MONDAY, result.get(0).getDayOfWeek());
    }

    @Test
    void testGetBlocksByDay_WithNoMatchingDay_ShouldReturnEmptyList() {
        when(repository.findByDayOfWeek(DayOfWeek.SATURDAY))
            .thenReturn(Collections.emptyList());

        List<SpecialEventBlock> result = service.getBlocksByDay(DayOfWeek.SATURDAY);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetBlocksByDay_WithNullDay_ShouldReturnEmptyList() {
        List<SpecialEventBlock> result = service.getBlocksByDay(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testMultipleBlockTypes_ShouldFilterCorrectly() {
        SpecialEventBlock iepBlock = new SpecialEventBlock();
        iepBlock.setBlockType(EventBlockType.IEP_MEETING);

        SpecialEventBlock planningBlock = new SpecialEventBlock();
        planningBlock.setBlockType(EventBlockType.TEACHER_PLANNING);

        when(repository.findByBlockType(EventBlockType.IEP_MEETING))
            .thenReturn(Arrays.asList(iepBlock));
        when(repository.findByBlockType(EventBlockType.TEACHER_PLANNING))
            .thenReturn(Arrays.asList(planningBlock));

        List<SpecialEventBlock> iepBlocks = service.getBlocksByType(EventBlockType.IEP_MEETING);
        List<SpecialEventBlock> planningBlocks = service.getBlocksByType(EventBlockType.TEACHER_PLANNING);

        assertEquals(1, iepBlocks.size());
        assertEquals(1, planningBlocks.size());
        assertEquals(EventBlockType.IEP_MEETING, iepBlocks.get(0).getBlockType());
        assertEquals(EventBlockType.TEACHER_PLANNING, planningBlocks.get(0).getBlockType());
    }

    @Test
    void testCreateAndRetrieve_ShouldMaintainData() {
        when(repository.save(any(SpecialEventBlock.class))).thenReturn(testEventBlock);
        when(repository.findById(1L)).thenReturn(Optional.of(testEventBlock));

        SpecialEventBlock created = service.createEventBlock(testEventBlock);
        SpecialEventBlock retrieved = repository.findById(1L).orElse(null);

        assertNotNull(created);
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getBlockType(), retrieved.getBlockType());
    }
}
