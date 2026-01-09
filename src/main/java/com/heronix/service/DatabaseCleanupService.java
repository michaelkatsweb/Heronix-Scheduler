package com.heronix.service;

import com.heronix.model.enums.ScheduleStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Database Cleanup Service
 * Location: src/main/java/com/eduscheduler/service/DatabaseCleanupService.java
 */
public interface DatabaseCleanupService {

    // Delete schedules older than specified date
    int purgeSchedulesBefore(LocalDate date);

    // Delete schedules by status
    int purgeSchedulesByStatus(ScheduleStatus status);

    // Archive old schedules instead of deleting
    int archiveSchedulesBefore(LocalDate date);

    // Delete orphaned schedule slots
    int cleanupOrphanedSlots();

    // Full database cleanup
    void performFullCleanup();
}
