package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Class Wallet Transaction DTO for API
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassWalletDTO {
    private Long id;
    private Long studentId;
    private String transactionType;  // REWARD, FINE, PURCHASE, REFUND, ADJUSTMENT
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String category;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    private Long teacherId;
    private String teacherName;
    private Boolean approved;
    private String notes;
    private String syncStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
