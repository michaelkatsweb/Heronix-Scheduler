package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Club DTO for API
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubDTO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String advisorName;
    private Long advisorId;
    private String meetingDay;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime meetingTime;

    private Integer durationMinutes;
    private String location;
    private Integer maxCapacity;
    private Integer currentEnrollment;
    private Boolean active;
    private Boolean requiresApproval;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextMeetingDate;

    private String notes;
    private String syncStatus;

    // Calculated fields
    private Integer availableSpots;
    private Boolean atCapacity;

    // Member IDs (for sync)
    private List<Long> memberIds;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
