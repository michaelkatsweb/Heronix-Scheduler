package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

/**
 * Student Grade Data Transfer Object
 * Contains grade information for a specific course
 *
 * @author Heronix Scheduling System Team
 * @since December 13, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGradeDTO {
    private Long gradeId;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private String term;
    private Integer academicYear;
    private String letterGrade;
    private Double numericalGrade;
    private Double gpaPoints;
    private Double credits;
    private Boolean isWeighted;
    private String gradeType; // "Final", "Midterm", "Quarter", etc.
    private LocalDate gradeDate;
    private String teacherName;
    private String comments;
    private String effortGrade;
    private String conductGrade;
    private Integer absences;
    private Integer tardies;
    private Boolean isFinal;
}
