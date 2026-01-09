package com.heronix.service.validation;

import com.heronix.model.enums.EducationLevel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Grade Level Validator - FIX #5
 * Location:
 * src/main/java/com/eduscheduler/service/validation/GradeLevelValidator.java
 * 
 * Validates and filters students based on education level.
 * Prevents importing students from wrong grade levels.
 * 
 * ISSUE FIXED:
 * - System showed 7 grade levels when high school should only have 4 (grades
 * 9-12)
 * - Validates students belong to appropriate grade level before import
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0 - FIX #5
 * @since 2025-10-28
 */
@Component
public class GradeLevelValidator {

    /**
     * Valid grade levels for each education level
     */
    public static final List<Integer> PREK_GRADES = Arrays.asList(-1); // Pre-K
    public static final List<Integer> KINDERGARTEN_GRADES = Arrays.asList(0); // K
    public static final List<Integer> ELEMENTARY_GRADES = Arrays.asList(1, 2, 3, 4, 5);
    public static final List<Integer> MIDDLE_SCHOOL_GRADES = Arrays.asList(6, 7, 8);
    public static final List<Integer> HIGH_SCHOOL_GRADES = Arrays.asList(9, 10, 11, 12);
    public static final List<Integer> COLLEGE_GRADES = Arrays.asList(13, 14, 15, 16); // Freshman-Senior
    public static final List<Integer> GRADUATE_GRADES = Arrays.asList(17, 18, 19, 20); // Graduate years

    /**
     * Validate if grade level is appropriate for the education level
     * 
     * @param gradeLevel     Student's grade level
     * @param educationLevel School's education level
     * @return true if valid, false if student should be filtered out
     */
    public boolean isValidGradeForLevel(int gradeLevel, EducationLevel educationLevel) {
        if (educationLevel == null) {
            return true; // No filtering if education level not specified
        }

        switch (educationLevel) {
            case PRE_K:
                return PREK_GRADES.contains(gradeLevel);
            case KINDERGARTEN:
                return KINDERGARTEN_GRADES.contains(gradeLevel);
            case ELEMENTARY:
                return ELEMENTARY_GRADES.contains(gradeLevel);
            case MIDDLE_SCHOOL:
                return MIDDLE_SCHOOL_GRADES.contains(gradeLevel);
            case HIGH_SCHOOL:
                return HIGH_SCHOOL_GRADES.contains(gradeLevel);
            case COLLEGE:
                return COLLEGE_GRADES.contains(gradeLevel);
            case GRADUATE:
                return GRADUATE_GRADES.contains(gradeLevel);
            default:
                return true; // Accept if unknown level
        }
    }

    /**
     * Parse grade level string to integer
     * 
     * Handles various formats:
     * - "9", "10", "11", "12"
     * - "9th", "10th", "11th", "12th"
     * - "Pre-K" → -1
     * - "K" or "Kindergarten" → 0
     * 
     * @param gradeLevelStr Grade level as string
     * @return Grade level as integer, or null if invalid
     */
    public Integer parseGradeLevel(String gradeLevelStr) {
        if (gradeLevelStr == null || gradeLevelStr.trim().isEmpty()) {
            return null;
        }

        String grade = gradeLevelStr.trim().toUpperCase();

        // Handle special cases
        if (grade.equals("PRE-K") || grade.equals("PREK")) {
            return -1;
        }
        if (grade.equals("K") || grade.equals("KINDERGARTEN")) {
            return 0;
        }

        // Remove "TH" suffix (9th → 9, 10th → 10, etc.)
        grade = grade.replaceAll("(ST|ND|RD|TH)$", "");

        // Remove "GRADE" prefix (Grade 9 → 9)
        grade = grade.replaceAll("^GRADE\\s*", "");

        // Parse integer
        try {
            return Integer.parseInt(grade);
        } catch (NumberFormatException e) {
            return null; // Invalid format
        }
    }

    /**
     * Get list of valid grade levels for education level
     * 
     * @param educationLevel Education level
     * @return List of valid integer grade levels
     */
    public List<Integer> getValidGradeLevels(EducationLevel educationLevel) {
        if (educationLevel == null) {
            return Arrays.asList(); // Empty list
        }

        switch (educationLevel) {
            case PRE_K:
                return PREK_GRADES;
            case KINDERGARTEN:
                return KINDERGARTEN_GRADES;
            case ELEMENTARY:
                return ELEMENTARY_GRADES;
            case MIDDLE_SCHOOL:
                return MIDDLE_SCHOOL_GRADES;
            case HIGH_SCHOOL:
                return HIGH_SCHOOL_GRADES;
            case COLLEGE:
                return COLLEGE_GRADES;
            case GRADUATE:
                return GRADUATE_GRADES;
            default:
                return Arrays.asList();
        }
    }

    /**
     * Get human-readable grade level string
     * 
     * @param gradeLevel Integer grade level
     * @return Human-readable string (e.g., "9th Grade", "Kindergarten")
     */
    public String getGradeLevelDisplay(int gradeLevel) {
        if (gradeLevel == -1) {
            return "Pre-K";
        }
        if (gradeLevel == 0) {
            return "Kindergarten";
        }
        if (gradeLevel >= 1 && gradeLevel <= 12) {
            return gradeLevel + getSuffix(gradeLevel) + " Grade";
        }
        if (gradeLevel == 13) {
            return "College Freshman";
        }
        if (gradeLevel == 14) {
            return "College Sophomore";
        }
        if (gradeLevel == 15) {
            return "College Junior";
        }
        if (gradeLevel == 16) {
            return "College Senior";
        }
        if (gradeLevel >= 17) {
            return "Graduate Year " + (gradeLevel - 16);
        }
        return "Unknown";
    }

    /**
     * Get ordinal suffix for number
     */
    private String getSuffix(int number) {
        if (number >= 11 && number <= 13) {
            return "th";
        }
        switch (number % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    /**
     * Validate and provide helpful error message
     * 
     * @param gradeLevel     Grade level to validate
     * @param educationLevel Education level
     * @return Error message if invalid, null if valid
     */
    public String getValidationError(int gradeLevel, EducationLevel educationLevel) {
        if (isValidGradeForLevel(gradeLevel, educationLevel)) {
            return null; // Valid
        }

        List<Integer> validGrades = getValidGradeLevels(educationLevel);
        String validGradesStr = validGrades.stream()
                .map(this::getGradeLevelDisplay)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        return String.format(
                "Grade level %s is not valid for %s. Valid grades: %s",
                getGradeLevelDisplay(gradeLevel),
                educationLevel.name(),
                validGradesStr);
    }
}