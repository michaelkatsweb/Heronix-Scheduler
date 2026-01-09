package com.heronix.service.impl;

import com.heronix.model.domain.Room;
import com.heronix.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for RoomServiceImpl
 *
 * Service: 19th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/RoomServiceImplTest.java
 *
 * Tests cover:
 * - Get all active rooms
 * - Get room by ID
 * - Get all rooms (including inactive)
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock(lenient = true)
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomServiceImpl service;

    private Room testRoom1;
    private Room testRoom2;

    @BeforeEach
    void setUp() {
        // Create test room 1 (active)
        testRoom1 = new Room();
        testRoom1.setId(1L);
        testRoom1.setRoomNumber("101");
        testRoom1.setCapacity(30);
        testRoom1.setActive(true);

        // Create test room 2 (inactive)
        testRoom2 = new Room();
        testRoom2.setId(2L);
        testRoom2.setRoomNumber("102");
        testRoom2.setCapacity(25);
        testRoom2.setActive(false);
    }

    // ========== GET ALL ACTIVE ROOMS TESTS ==========

    @Test
    void testGetAllActiveRooms_WithActiveRooms_ShouldReturnList() {
        when(roomRepository.findByActiveTrue()).thenReturn(Arrays.asList(testRoom1));

        List<Room> result = service.getAllActiveRooms();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    @Test
    void testGetAllActiveRooms_WithNoActiveRooms_ShouldReturnEmptyList() {
        when(roomRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        List<Room> result = service.getAllActiveRooms();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllActiveRooms_WithMultipleActiveRooms_ShouldReturnAll() {
        Room room3 = new Room();
        room3.setId(3L);
        room3.setRoomNumber("103");
        room3.setActive(true);

        when(roomRepository.findByActiveTrue()).thenReturn(Arrays.asList(testRoom1, room3));

        List<Room> result = service.getAllActiveRooms();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========== GET ROOM BY ID TESTS ==========

    @Test
    void testGetRoomById_WithExistingRoom_ShouldReturnRoom() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom1));

        Room result = service.getRoomById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("101", result.getRoomNumber());
    }

    @Test
    void testGetRoomById_WithNonExistentRoom_ShouldReturnNull() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        Room result = service.getRoomById(999L);

        assertNull(result);
    }

    @Test
    void testGetRoomById_WithNullRoomNumber_ShouldNotCrash() {
        testRoom1.setRoomNumber(null);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom1));

        Room result = service.getRoomById(1L);

        assertNotNull(result);
        assertNull(result.getRoomNumber());
    }

    // ========== FIND ALL TESTS ==========

    @Test
    void testFindAll_WithRooms_ShouldReturnAll() {
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom1, testRoom2));

        List<Room> result = service.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindAll_WithNoRooms_ShouldReturnEmptyList() {
        when(roomRepository.findAll()).thenReturn(Collections.emptyList());

        List<Room> result = service.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAll_ShouldIncludeInactiveRooms() {
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom1, testRoom2));

        List<Room> result = service.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        // Verify both active and inactive are included
        assertTrue(result.stream().anyMatch(r -> r.getActive()));
        assertTrue(result.stream().anyMatch(r -> !r.getActive()));
    }
}
