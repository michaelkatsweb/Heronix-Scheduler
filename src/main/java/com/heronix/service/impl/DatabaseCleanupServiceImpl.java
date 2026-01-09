package com.heronix.service.impl;

import com.heronix.model.domain.Schedule;
import com.heronix.model.domain.ScheduleSlot;
import com.heronix.model.enums.ScheduleStatus;
import com.heronix.repository.ScheduleRepository;
import com.heronix.repository.ScheduleSlotRepository;
import com.heronix.service.DatabaseCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Database Cleanup Service Implementation
 * Location:
 * src/main/java/com/eduscheduler/service/impl/DatabaseCleanupServiceImpl.java
 * 
 * Handles cleanup and purging of old/unused schedule data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseCleanupServiceImpl implements DatabaseCleanupService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    @Override
    @Transactional
    public int purgeSchedulesBefore(LocalDate date) {
        log.info("Purging schedules before: {}", date);

        // ✅ NULL SAFE: Filter null schedules before accessing endDate
        List<Schedule> oldSchedules = scheduleRepository.findAll().stream()
                .filter(s -> s != null && s.getEndDate() != null && s.getEndDate().isBefore(date))
                .toList();

        int count = oldSchedules.size();

        // Delete slots first (foreign key constraint)
        for (Schedule schedule : oldSchedules) {
            // ✅ NULL SAFE: Skip if schedule or slots are null
            if (schedule == null || schedule.getSlots() == null) continue;
            scheduleSlotRepository.deleteAll(schedule.getSlots());
        }

        // Then delete schedules
        scheduleRepository.deleteAll(oldSchedules);

        log.info("Purged {} schedules", count);
        return count;
    }

    @Override
    @Transactional
    public int purgeSchedulesByStatus(ScheduleStatus status) {
        log.info("Purging schedules with status: {}", status);

        List<Schedule> schedules = scheduleRepository.findByStatus(status);
        int count = schedules.size();

        // Delete slots first
        for (Schedule schedule : schedules) {
            // ✅ NULL SAFE: Skip if schedule or slots are null
            if (schedule == null || schedule.getSlots() == null) continue;
            scheduleSlotRepository.deleteAll(schedule.getSlots());
        }

        // Then delete schedules
        scheduleRepository.deleteAll(schedules);

        log.info("Purged {} schedules with status {}", count, status);
        return count;
    }

    @Override
    @Transactional
    public int archiveSchedulesBefore(LocalDate date) {
        log.info("Archiving schedules before: {}", date);

        // ✅ NULL SAFE: Filter null schedules before accessing properties
        List<Schedule> oldSchedules = scheduleRepository.findAll().stream()
                .filter(s -> s != null && s.getEndDate() != null && s.getEndDate().isBefore(date))
                .filter(s -> s.getStatus() != null && s.getStatus() != ScheduleStatus.ARCHIVED)
                .toList();

        int count = 0;
        for (Schedule schedule : oldSchedules) {
            // ✅ NULL SAFE: Skip null schedules
            if (schedule == null) continue;
            schedule.setStatus(ScheduleStatus.ARCHIVED);
            scheduleRepository.save(schedule);
            count++;
        }

        log.info("Archived {} schedules", count);
        return count;
    }

    @Override
    @Transactional
    public int cleanupOrphanedSlots() {
        log.info("Cleaning up orphaned schedule slots");

        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();
        // ✅ NULL SAFE: Filter null slots before checking schedule
        List<ScheduleSlot> orphaned = allSlots.stream()
                .filter(slot -> slot != null && slot.getSchedule() == null)
                .toList();

        int count = orphaned.size();
        scheduleSlotRepository.deleteAll(orphaned);

        log.info("Cleaned up {} orphaned slots", count);
        return count;
    }

    @Override
    @Transactional
    public void performFullCleanup() {
        log.info("=== Starting Full Database Cleanup ===");

        // 1. Archive schedules older than 1 year
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        int archived = archiveSchedulesBefore(oneYearAgo);

        // 2. Delete draft schedules older than 6 months
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        // ✅ NULL SAFE: Filter null schedules before accessing createdDate
        List<Schedule> oldDrafts = scheduleRepository.findByStatus(ScheduleStatus.DRAFT).stream()
                .filter(s -> s != null && s.getCreatedDate() != null &&
                        s.getCreatedDate().isBefore(sixMonthsAgo))
                .toList();

        for (Schedule draft : oldDrafts) {
            // ✅ NULL SAFE: Skip if draft or slots are null
            if (draft == null || draft.getSlots() == null) continue;
            scheduleSlotRepository.deleteAll(draft.getSlots());
        }
        scheduleRepository.deleteAll(oldDrafts);

        // 3. Clean orphaned slots
        int orphaned = cleanupOrphanedSlots();

        log.info("=== Full Cleanup Complete ===");
        log.info("Archived: {}, Deleted old drafts: {}, Orphaned slots: {}",
                archived, oldDrafts.size(), orphaned);
    }
}
