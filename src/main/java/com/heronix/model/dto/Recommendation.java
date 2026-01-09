// Location: src/main/java/com/eduscheduler/model/dto/Recommendation.java
package com.heronix.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * AI-generated recommendation for schedule improvements
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    private String type; // WORKLOAD_BALANCE, ROOM_OPTIMIZATION, etc.
    private String severity; // INFO, WARNING, CRITICAL
    private String title;
    private String description;

    private Long affectedEntityId; // Teacher, Room, or Course ID
    private String affectedEntityType; // TEACHER, ROOM, COURSE
    private String affectedEntityName;

    private double impactScore; // 0-100
    private String actionRequired;
    private boolean canAutoFix;

    private String category; // LEAN, KANBAN, EISENHOWER
}