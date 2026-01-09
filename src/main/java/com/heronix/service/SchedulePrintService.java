package com.heronix.service;

import com.heronix.model.domain.Schedule;

/**
 * Schedule Print Service
 * Location: src/main/java/com/eduscheduler/service/SchedulePrintService.java
 */
public interface SchedulePrintService {

    void printSchedule(Schedule schedule);

    void printSchedulePreview(Schedule schedule);
}