package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Student Attendance Data Transfer Object
 * Contains attendance record information
 *
 * @author Heronix Scheduling System Team
 * @since December 13, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceDTO {
    private Long recordId;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private String status; // "PRESENT", "ABSENT", "TARDY", "EXCUSED_ABSENT", etc.
    private String courseName;
    private String periodNumber;
    private String notes;
    private Boolean verified;
    private String recordedBy;
}
