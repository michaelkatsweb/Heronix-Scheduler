package com.heronix.config;

import com.heronix.model.domain.Room;
import com.heronix.model.enums.RoomType;
import com.heronix.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phase 5F Migration Runner
 * Automatically populates default activity tags for existing rooms on application startup
 *
 * This runs once on startup and populates activity tags for rooms that don't have them yet.
 * Safe to run multiple times - only updates rooms without activity tags.
 *
 * @since Phase 5F - December 2, 2025
 */
@Component
public class Phase5FMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase5FMigrationRunner.class);

    private final RoomRepository roomRepository;

    public Phase5FMigrationRunner(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🔄 Phase 5F Migration: Checking for rooms without activity tags...");

        try {
            List<Room> allRooms = roomRepository.findAll();
            int updatedCount = 0;
            int skippedCount = 0;

            for (Room room : allRooms) {
                // Skip rooms that already have activity tags
                if (room.hasActivityTags()) {
                    skippedCount++;
                    continue;
                }

                // Populate default activity tags based on room type
                String defaultTags = getDefaultActivityTags(room.getType());
                if (defaultTags != null) {
                    room.setActivityTags(defaultTags);
                    roomRepository.save(room);
                    updatedCount++;
                    log.info("✅ Updated room '{}' ({}) with default tags: {}",
                            room.getRoomNumber(), room.getType(), defaultTags);
                }
            }

            if (updatedCount > 0) {
                log.info("✅ Phase 5F Migration Complete: {} rooms updated, {} rooms skipped (already have tags)",
                        updatedCount, skippedCount);
            } else {
                log.info("✅ Phase 5F Migration: All rooms already configured ({} total rooms)", allRooms.size());
            }

        } catch (Exception e) {
            log.error("❌ Phase 5F Migration failed: {}", e.getMessage(), e);
            // Don't throw - allow application to start even if migration fails
        }
    }

    /**
     * Returns default activity tags for a given room type
     */
    private String getDefaultActivityTags(RoomType roomType) {
        if (roomType == null) {
            return null;
        }

        // Use if-else for Java 17 compatibility
        if (roomType == RoomType.GYMNASIUM) {
            return "Basketball,Volleyball,Indoor Soccer,Badminton,General PE";
        } else if (roomType == RoomType.WEIGHT_ROOM) {
            return "Weights,Strength Training,Conditioning,Fitness,Powerlifting";
        } else if (roomType == RoomType.ART_STUDIO) {
            return "Art,Drawing,Painting,Sculpture,Ceramics";
        } else if (roomType == RoomType.MUSIC_ROOM) {
            return "Music,Instruments,General Music,Music Theory";
        } else if (roomType == RoomType.BAND_ROOM) {
            return "Band,Instruments,Music,Performances";
        } else if (roomType == RoomType.CHORUS_ROOM) {
            return "Chorus,Choir,Vocal Music,Singing";
        } else if (roomType == RoomType.SCIENCE_LAB) {
            return "Science,Lab Work,Experiments,Research";
        } else if (roomType == RoomType.COMPUTER_LAB) {
            return "Computer Science,Programming,Technology,Coding";
        } else if (roomType == RoomType.STEM_LAB) {
            return "STEM,Science,Technology,Engineering,Math,Projects";
        } else if (roomType == RoomType.CULINARY_LAB) {
            return "Culinary Arts,Cooking,Food Preparation,Nutrition";
        } else if (roomType == RoomType.AUDITORIUM) {
            return "Assemblies,Presentations,Theater,Performances";
        } else if (roomType == RoomType.THEATER) {
            return "Drama,Theater,Acting,Performances,Rehearsals";
        } else if (roomType == RoomType.CAFETERIA) {
            return "Lunch,Assemblies,Large Group Activities";
        } else if (roomType == RoomType.LIBRARY) {
            return "Study,Research,Reading,Quiet Work";
        } else if (roomType == RoomType.MEDIA_CENTER) {
            return "Broadcasting,Media Production,Video,Audio,Technology";
        } else if (roomType == RoomType.WORKSHOP) {
            return "Shop,Industrial Arts,Woodworking,Construction,Projects";
        } else if (roomType == RoomType.MULTIPURPOSE) {
            return "General PE,Activities,Events,Flexible Use";
        } else if (roomType == RoomType.RESOURCE_ROOM) {
            return "Special Education,Small Group,Individualized Instruction";
        } else if (roomType == RoomType.TESTING_ROOM) {
            return "Standardized Testing,Assessments,Quiet Environment";
        } else {
            return null; // CLASSROOM and administrative rooms - no specific activity tags
        }
    }
}
