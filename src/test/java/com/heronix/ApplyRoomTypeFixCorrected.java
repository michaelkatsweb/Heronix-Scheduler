package com.heronix;

import com.heronix.model.domain.Room;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ APPLY ROOM TYPE FIX - Convert Rooms to CLASSROOM                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * ROOT CAUSE: 0 CLASSROOM rooms but Math courses need them!
 *
 * This test applies the fix by converting science labs to CLASSROOM type
 * (keeping only 2 science labs).
 *
 * Run with: mvn test -Dtest=ApplyRoomTypeFixCorrected
 *
 * December 6, 2025
 */
@SpringBootTest
@ActiveProfiles("test")
public class ApplyRoomTypeFixCorrected {

    @Autowired
    private RoomRepository roomRepository;

    @Test
    @Transactional
    public void applyRoomTypeFix() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ROOM TYPE FIX - Converting rooms to CLASSROOM");
        System.out.println("=".repeat(80) + "\n");

        // STEP 1: Show current room type distribution
        System.out.println("📊 BEFORE FIX - Room Type Distribution:");
        System.out.println("-".repeat(80));
        showRoomTypeDistribution();

        // STEP 2: Get all available science labs
        List<Room> allScienceLabs = roomRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                .filter(r -> r.getRoomType() == RoomType.SCIENCE_LAB)
                .sorted(Comparator.comparing(Room::getId))
                .collect(Collectors.toList());

        System.out.println("\n🔍 Found " + allScienceLabs.size() + " science labs");

        if (allScienceLabs.size() <= 2) {
            System.out.println("⚠️  Only " + allScienceLabs.size() + " science labs found.");
            System.out.println("    No conversion needed (keeping all science labs).\n");
            return;
        }

        // STEP 3: Keep the last 2 science labs, convert the rest to CLASSROOM
        List<Room> labsToKeep = allScienceLabs.stream()
                .skip(Math.max(0, allScienceLabs.size() - 2))
                .collect(Collectors.toList());

        List<Room> labsToConvert = allScienceLabs.stream()
                .limit(Math.max(0, allScienceLabs.size() - 2))
                .collect(Collectors.toList());

        System.out.println("✅ Keeping " + labsToKeep.size() + " science labs:");
        for (Room lab : labsToKeep) {
            System.out.printf("   - Room %s (ID: %d, Capacity: %d)%n",
                    lab.getRoomNumber(), lab.getId(), lab.getCapacity());
        }

        System.out.println("\n🔄 Converting " + labsToConvert.size() + " science labs to CLASSROOM:");
        int convertedCount = 0;
        for (Room room : labsToConvert) {
            System.out.printf("   - Room %s (ID: %d) %s → %s%n",
                    room.getRoomNumber(),
                    room.getId(),
                    room.getRoomType(),
                    RoomType.CLASSROOM);

            room.setRoomType(RoomType.CLASSROOM);
            roomRepository.save(room);
            convertedCount++;
        }

        System.out.println("\n✅ Converted " + convertedCount + " rooms to CLASSROOM type\n");

        // STEP 4: Verify the fix
        System.out.println("📊 AFTER FIX - Room Type Distribution:");
        System.out.println("-".repeat(80));
        showRoomTypeDistribution();

        // STEP 5: Check if we have enough CLASSROOM rooms
        long classroomCount = roomRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                .filter(r -> r.getRoomType() == RoomType.CLASSROOM)
                .count();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("FINAL VERIFICATION:");
        System.out.println("=".repeat(80));
        System.out.printf("Total CLASSROOM rooms: %d%n", classroomCount);

        if (classroomCount >= 10) {
            System.out.println("Status: ✅ SUFFICIENT");
            System.out.println("Result: Good! You have enough classrooms for Math/English courses\n");
        } else if (classroomCount >= 6) {
            System.out.println("Status: ⚠️  MINIMUM");
            System.out.println("Result: Minimum met, but consider adding more classrooms\n");
        } else {
            System.out.println("Status: ❌ INSUFFICIENT");
            System.out.println("Result: CRITICAL - Need at least 6 classrooms\n");
        }

        System.out.println("=".repeat(80));
        System.out.println("✅ FIX COMPLETE - Try schedule generation again!");
        System.out.println("=".repeat(80) + "\n");
    }

    private void showRoomTypeDistribution() {
        List<Room> allRooms = roomRepository.findAll();

        Map<RoomType, Long> distribution = allRooms.stream()
                .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                .collect(Collectors.groupingBy(
                        r -> r.getRoomType() != null ? r.getRoomType() : RoomType.CLASSROOM,
                        Collectors.counting()
                ));

        Map<RoomType, Integer> capacities = allRooms.stream()
                .filter(r -> Boolean.TRUE.equals(r.getAvailable()))
                .collect(Collectors.groupingBy(
                        r -> r.getRoomType() != null ? r.getRoomType() : RoomType.CLASSROOM,
                        Collectors.summingInt(r -> r.getCapacity() != null ? r.getCapacity() : 0)
                ));

        long totalRooms = distribution.values().stream().mapToLong(Long::longValue).sum();

        System.out.printf("%-25s %10s %15s %12s%n", "Room Type", "Count", "Total Capacity", "Percentage");
        System.out.println("-".repeat(80));

        distribution.entrySet().stream()
                .sorted(Map.Entry.<RoomType, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    RoomType type = entry.getKey();
                    long count = entry.getValue();
                    int capacity = capacities.getOrDefault(type, 0);
                    double percentage = (count * 100.0) / totalRooms;

                    System.out.printf("%-25s %10d %15d %11.1f%%%n",
                            type.getDisplayName(),
                            count,
                            capacity,
                            percentage);
                });

        System.out.println("-".repeat(80));
        System.out.printf("%-25s %10d%n", "TOTAL", totalRooms);
        System.out.println();
    }
}
