package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

/**
 * Student Assignment Data Transfer Object
 * Contains assignment information for student view
 *
 * @author Heronix Scheduling System Team
 * @since December 13, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAssignmentDTO {
    private Long assignmentId;
    private String assignmentName;
    private String description;
    private String courseName;
    private String courseCode;
    private LocalDate dueDate;
    private LocalDate assignedDate;
    private String category; // "Homework", "Quiz", "Test", "Project", etc.
    private Integer maxPoints;
    private Double studentScore;
    private String status; // "NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "GRADED", "MISSING"
    private Boolean isPublished;
    private Boolean isGraded;
    private Boolean isMissing;
    private Boolean isExcused;
    private String teacherComments;

    // Time indicators
    private int daysUntilDue;
    private boolean isOverdue;
    private boolean isDueSoon; // Due within 3 days
}
