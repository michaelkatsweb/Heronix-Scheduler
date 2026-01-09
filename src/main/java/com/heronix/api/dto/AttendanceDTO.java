package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Attendance data transfer object for API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {
    private Long id;
    private Long studentId;
    private Long courseId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String status;
    private String notes;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime timeIn;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime timeOut;

    // Aliases for compatibility
    private java.time.LocalDateTime lastModified;

    public LocalTime getArrivalTime() {
        return timeIn;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.timeIn = arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return timeOut;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.timeOut = departureTime;
    }
}
