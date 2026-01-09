package com.heronix.model.dto;

import com.heronix.model.domain.Student;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of students who should eat lunch together
 * Based on their scheduled class during the lunch period
 *
 * Location: src/main/java/com/eduscheduler/model/dto/LunchCohort.java
 *
 * Examples:
 * - "Room 101 - Algebra I" (28 students)
 * - "Grade 9 - Free Period" (42 students)
 * - "Room 205 - English II" (31 students)
 *
 * @author Heronix Scheduling System Team
 * @version 5.0.0
 * @since 2025-12-06
 */
@Data
public class LunchCohort {

    /**
     * Cohort identifier (e.g., "Room 101 - Algebra I" or "Grade 9")
     */
    private String name;

    /**
     * Students in this cohort
     */
    private List<Student> students;

    /**
     * Room number extracted from cohort name (e.g., "101")
     * Null for non-room-based cohorts (grade-level groups)
     */
    private String roomNumber;

    /**
     * Room zone for spatial grouping (e.g., "1st Floor East")
     * Null if room number cannot be determined
     */
    private String roomZone;

    /**
     * Course name if cohort is based on a class
     */
    private String courseName;

    /**
     * Grade level if cohort is grade-based
     */
    private String gradeLevel;

    public LunchCohort(String name, List<Student> students) {
        this.name = name;
        this.students = new ArrayList<>(students);
        this.roomNumber = extractRoomNumber(name);
        this.roomZone = determineRoomZone(roomNumber);
        this.courseName = extractCourseName(name);
        this.gradeLevel = extractGradeLevel(name);
    }

    /**
     * Get the number of students in this cohort
     */
    public int getSize() {
        return students != null ? students.size() : 0;
    }

    /**
     * Extract room number from cohort name
     * Examples:
     * - "Room 101 - Algebra I" -> "101"
     * - "Grade 9" -> null
     */
    private String extractRoomNumber(String cohortName) {
        if (cohortName == null || !cohortName.startsWith("Room ")) {
            return null;
        }

        try {
            int dashIndex = cohortName.indexOf(" - ");
            if (dashIndex > 5) {
                return cohortName.substring(5, dashIndex).trim();
            }
        } catch (Exception e) {
            // Invalid format, return null
        }

        return null;
    }

    /**
     * Extract course name from cohort name
     * Examples:
     * - "Room 101 - Algebra I" -> "Algebra I"
     * - "Grade 9" -> null
     */
    private String extractCourseName(String cohortName) {
        if (cohortName == null || !cohortName.contains(" - ")) {
            return null;
        }

        try {
            int dashIndex = cohortName.indexOf(" - ");
            return cohortName.substring(dashIndex + 3).trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract grade level from cohort name
     * Examples:
     * - "Grade 9" -> "9"
     * - "Grade 12 - Free Period" -> "12"
     * - "Room 101 - Algebra I" -> null
     */
    private String extractGradeLevel(String cohortName) {
        if (cohortName == null || !cohortName.startsWith("Grade ")) {
            return null;
        }

        try {
            String gradeStr = cohortName.substring(6).trim();
            int spaceIndex = gradeStr.indexOf(" ");
            if (spaceIndex > 0) {
                gradeStr = gradeStr.substring(0, spaceIndex);
            }
            return gradeStr;  // Return as String
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Determine which zone a room belongs to for spatial grouping
     * Examples:
     * - "101" -> "1st Floor East"
     * - "205" -> "2nd Floor East"
     * - "350" -> "3rd Floor West"
     */
    private String determineRoomZone(String roomNum) {
        if (roomNum == null) {
            return null;
        }

        try {
            int num = Integer.parseInt(roomNum);

            // First floor (100-199)
            if (num >= 100 && num < 150) return "1st Floor East";
            if (num >= 150 && num < 200) return "1st Floor West";

            // Second floor (200-299)
            if (num >= 200 && num < 250) return "2nd Floor East";
            if (num >= 250 && num < 300) return "2nd Floor West";

            // Third floor (300-399)
            if (num >= 300 && num < 350) return "3rd Floor East";
            if (num >= 350 && num < 400) return "3rd Floor West";

            // Fourth floor and above (400+)
            if (num >= 400) return "Upper Floors";

            // Below 100 (gym, cafeteria, etc.)
            if (num < 100) return "Ground Floor";

        } catch (NumberFormatException e) {
            // Invalid room number
        }

        return "Other";
    }

    /**
     * Check if this cohort is based on a classroom
     */
    public boolean isRoomBased() {
        return roomNumber != null;
    }

    /**
     * Check if this cohort is based on grade level
     */
    public boolean isGradeBased() {
        return gradeLevel != null;
    }

    @Override
    public String toString() {
        return String.format("%s (%d students%s)",
            name,
            getSize(),
            roomZone != null ? ", " + roomZone : "");
    }
}
