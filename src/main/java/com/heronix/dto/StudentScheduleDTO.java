package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Student Schedule Data Transfer Object
 * Contains student's schedule information
 *
 * @author Heronix Scheduling System Team
 * @since December 13, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentScheduleDTO {
    private Long studentId;
    private String studentName;
    private String gradeLevel;
    private List<ScheduleSlotDTO> scheduleSlots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleSlotDTO {
        private Long slotId;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer periodNumber;
        private String courseName;
        private String courseCode;
        private String teacherName;
        private String roomNumber;
        private String roomName;
        private Boolean isLunchPeriod;
        private String dayType; // "DAILY", "A_DAY", "B_DAY", etc.
    }
}
