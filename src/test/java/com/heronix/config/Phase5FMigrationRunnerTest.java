package com.heronix.config;

import com.heronix.model.domain.Room;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for Phase 5F Migration Runner
 * Verifies automatic population of default activity tags for existing rooms
 *
 * @since Phase 5F - December 2, 2025
 */
@ExtendWith(MockitoExtension.class)
class Phase5FMigrationRunnerTest {

    @Mock
    private RoomRepository roomRepository;

    private Phase5FMigrationRunner migrationRunner;

    @BeforeEach
    void setUp() {
        migrationRunner = new Phase5FMigrationRunner(roomRepository);
    }

    @Test
    void testMigrationUpdatesRoomsWithoutActivityTags() throws Exception {
        // Arrange
        Room gym = new Room();
        gym.setRoomNumber("GYM-01");
        gym.setType(RoomType.GYMNASIUM);
        gym.setActivityTags(null);

        Room artRoom = new Room();
        artRoom.setRoomNumber("ART-101");
        artRoom.setType(RoomType.ART_STUDIO);
        artRoom.setActivityTags(null);

        Room classroom = new Room();
        classroom.setRoomNumber("ROOM-202");
        classroom.setType(RoomType.CLASSROOM);
        classroom.setActivityTags(null);

        List<Room> rooms = Arrays.asList(gym, artRoom, classroom);
        when(roomRepository.findAll()).thenReturn(rooms);

        // Act
        migrationRunner.run();

        // Assert
        // Should update gym and art room (2 saves)
        // Classroom should not get tags (returns null for CLASSROOM type)
        verify(roomRepository, times(2)).save(any(Room.class));
    }

    @Test
    void testMigrationSkipsRoomsWithExistingTags() throws Exception {
        // Arrange
        Room gym = new Room();
        gym.setRoomNumber("GYM-01");
        gym.setType(RoomType.GYMNASIUM);
        gym.setActivityTags("Custom,Tags");

        List<Room> rooms = Arrays.asList(gym);
        when(roomRepository.findAll()).thenReturn(rooms);

        // Act
        migrationRunner.run();

        // Assert
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void testMigrationHandlesNullRoomType() throws Exception {
        // Arrange
        Room room = new Room();
        room.setRoomNumber("ROOM-001");
        room.setType(null);
        room.setActivityTags(null);

        List<Room> rooms = Arrays.asList(room);
        when(roomRepository.findAll()).thenReturn(rooms);

        // Act
        migrationRunner.run();

        // Assert
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void testMigrationPopulatesCorrectTagsForGymnasium() throws Exception {
        // Arrange
        Room gym = new Room();
        gym.setRoomNumber("GYM-01");
        gym.setType(RoomType.GYMNASIUM);
        gym.setActivityTags(null);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(gym));

        // Act
        migrationRunner.run();

        // Assert
        verify(roomRepository).save(argThat(room ->
                room.getActivityTags() != null &&
                room.getActivityTags().contains("Basketball") &&
                room.getActivityTags().contains("Volleyball")
        ));
    }

    @Test
    void testMigrationPopulatesCorrectTagsForScienceLab() throws Exception {
        // Arrange
        Room lab = new Room();
        lab.setRoomNumber("SCI-101");
        lab.setType(RoomType.SCIENCE_LAB);
        lab.setActivityTags(null);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(lab));

        // Act
        migrationRunner.run();

        // Assert
        verify(roomRepository).save(argThat(room ->
                room.getActivityTags() != null &&
                room.getActivityTags().contains("Science") &&
                room.getActivityTags().contains("Lab Work")
        ));
    }

    @Test
    void testMigrationPopulatesCorrectTagsForComputerLab() throws Exception {
        // Arrange
        Room lab = new Room();
        lab.setRoomNumber("COMP-101");
        lab.setType(RoomType.COMPUTER_LAB);
        lab.setActivityTags(null);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(lab));

        // Act
        migrationRunner.run();

        // Assert
        verify(roomRepository).save(argThat(room ->
                room.getActivityTags() != null &&
                room.getActivityTags().contains("Computer Science") &&
                room.getActivityTags().contains("Programming")
        ));
    }

    @Test
    void testMigrationHandlesExceptionGracefully() throws Exception {
        // Arrange
        when(roomRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act - should not throw exception
        migrationRunner.run();

        // Assert - migration fails but application continues
        verify(roomRepository, never()).save(any(Room.class));
    }
}
