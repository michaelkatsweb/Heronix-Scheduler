package com.heronix.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DatabaseStats {
    private long studentCount;
    private long teacherCount;
    private long courseCount;
    private long roomCount;
    private long eventCount;
    private long scheduleCount;
    private long scheduleSlotCount;
    private LocalDateTime lastUpdated;

    public long getTotalRecords() {
        return studentCount + teacherCount + courseCount +
                roomCount + eventCount + scheduleCount + scheduleSlotCount;
    }
}