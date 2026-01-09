package com.heronix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to mark items as synced
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncCompleteRequestDTO {
    private List<Long> gradeIds;
    private List<Long> attendanceIds;
}
