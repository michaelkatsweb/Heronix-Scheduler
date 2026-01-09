package com.heronix.service.validation;

import com.heronix.model.dto.ValidationIssue;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Comprehensive service for validating import data
 * Validates required fields, data types, and formats
 */
@Service
public class ImportValidationService {
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^[0-9]{10}$");
    private static final Pattern GRADE_LEVEL_PATTERN = 
        Pattern.compile("^(Pre-K|K|[1-9]|1[0-2])$");
    
    /**
     * Validate student import data
     */
    public ValidationIssue validateStudent(Map<String, String> rowData, int rowNumber) {
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        
        // Check required fields
        if (isNullOrEmpty(rowData.get("First Name"))) {
            missingFields.add("First Name");
        }
        if (isNullOrEmpty(rowData.get("Last Name"))) {
            missingFields.add("Last Name");
        }
        if (isNullOrEmpty(rowData.get("Grade Level"))) {
            missingFields.add("Grade Level");
        } else {
            // Validate grade level format
            String gradeLevel = rowData.get("Grade Level").trim();
            if (!GRADE_LEVEL_PATTERN.matcher(gradeLevel).matches()) {
                invalidFields.add("Grade Level (must be Pre-K, K, or 1-12)");
            }
        }
        
        // Validate optional fields if present
        String email = rowData.get("Email");
        if (!isNullOrEmpty(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            invalidFields.add("Email (invalid format)");
        }
        
        String phone = rowData.get("Phone");
        if (!isNullOrEmpty(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            invalidFields.add("Phone (must be 10 digits)");
        }
        
        String dob = rowData.get("Date of Birth");
        if (!isNullOrEmpty(dob) && !isValidDate(dob)) {
            invalidFields.add("Date of Birth (invalid format)");
        }
        
        // If issues found, return ValidationIssue
        if (!missingFields.isEmpty() || !invalidFields.isEmpty()) {
            return new ValidationIssue(
                "Student",
                rowNumber,
                rowData,
                missingFields,
                invalidFields
            );
        }
        
        return null; // No issues
    }
    
    /**
     * Validate teacher import data
     */
    public ValidationIssue validateTeacher(Map<String, String> rowData, int rowNumber) {
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        
        // Required fields
        if (isNullOrEmpty(rowData.get("First Name"))) {
            missingFields.add("First Name");
        }
        if (isNullOrEmpty(rowData.get("Last Name"))) {
            missingFields.add("Last Name");
        }
        if (isNullOrEmpty(rowData.get("Employee ID"))) {
            missingFields.add("Employee ID");
        }
        if (isNullOrEmpty(rowData.get("Email"))) {
            missingFields.add("Email");
        } else {
            // Validate email format
            String email = rowData.get("Email").trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                invalidFields.add("Email (invalid format)");
            }
        }
        
        // Validate optional phone if present
        String phone = rowData.get("Phone");
        if (!isNullOrEmpty(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            invalidFields.add("Phone (must be 10 digits)");
        }
        
        // Validate department if present
        String department = rowData.get("Department");
        if (!isNullOrEmpty(department) && department.length() > 100) {
            invalidFields.add("Department (too long, max 100 characters)");
        }
        
        if (!missingFields.isEmpty() || !invalidFields.isEmpty()) {
            return new ValidationIssue("Teacher", rowNumber, rowData, 
                                      missingFields, invalidFields);
        }
        return null;
    }
    
    /**
     * Validate course import data
     */
    public ValidationIssue validateCourse(Map<String, String> rowData, int rowNumber) {
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        
        // Required fields
        if (isNullOrEmpty(rowData.get("Course Code"))) {
            missingFields.add("Course Code");
        } else {
            String code = rowData.get("Course Code").trim();
            if (code.length() > 20) {
                invalidFields.add("Course Code (max 20 characters)");
            }
        }
        
        if (isNullOrEmpty(rowData.get("Course Name"))) {
            missingFields.add("Course Name");
        } else {
            String name = rowData.get("Course Name").trim();
            if (name.length() > 200) {
                invalidFields.add("Course Name (max 200 characters)");
            }
        }
        
        // Validate subject if present
        if (isNullOrEmpty(rowData.get("Subject"))) {
            missingFields.add("Subject");
        }
        
        // Validate credits if present
        String credits = rowData.get("Credits");
        if (!isNullOrEmpty(credits)) {
            try {
                double creditValue = Double.parseDouble(credits);
                if (creditValue < 0 || creditValue > 10) {
                    invalidFields.add("Credits (must be between 0 and 10)");
                }
            } catch (NumberFormatException e) {
                invalidFields.add("Credits (must be a number)");
            }
        }
        
        // Validate duration if present
        String duration = rowData.get("Duration");
        if (!isNullOrEmpty(duration)) {
            try {
                int durationValue = Integer.parseInt(duration);
                if (durationValue < 1 || durationValue > 240) {
                    invalidFields.add("Duration (must be 1-240 minutes)");
                }
            } catch (NumberFormatException e) {
                invalidFields.add("Duration (must be a number)");
            }
        }
        
        if (!missingFields.isEmpty() || !invalidFields.isEmpty()) {
            return new ValidationIssue("Course", rowNumber, rowData, 
                                      missingFields, invalidFields);
        }
        return null;
    }
    
    /**
     * Validate room import data
     */
    public ValidationIssue validateRoom(Map<String, String> rowData, int rowNumber) {
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        
        // Required fields
        if (isNullOrEmpty(rowData.get("Room Number"))) {
            missingFields.add("Room Number");
        } else {
            String roomNumber = rowData.get("Room Number").trim();
            if (roomNumber.length() > 20) {
                invalidFields.add("Room Number (max 20 characters)");
            }
        }
        
        // Validate capacity if present
        String capacity = rowData.get("Capacity");
        if (!isNullOrEmpty(capacity)) {
            try {
                int capacityValue = Integer.parseInt(capacity);
                if (capacityValue < 1 || capacityValue > 500) {
                    invalidFields.add("Capacity (must be 1-500)");
                }
            } catch (NumberFormatException e) {
                invalidFields.add("Capacity (must be a number)");
            }
        }
        
        // Validate building if present
        String building = rowData.get("Building");
        if (!isNullOrEmpty(building) && building.length() > 50) {
            invalidFields.add("Building (max 50 characters)");
        }
        
        // Validate room type if present
        String roomType = rowData.get("Room Type");
        if (!isNullOrEmpty(roomType)) {
            Set<String> validTypes = Set.of(
                "Classroom", "Lab", "Gym", "Library", 
                "Auditorium", "Cafeteria", "Office"
            );
            if (!validTypes.contains(roomType)) {
                invalidFields.add("Room Type (must be: " + 
                    String.join(", ", validTypes) + ")");
            }
        }
        
        if (!missingFields.isEmpty() || !invalidFields.isEmpty()) {
            return new ValidationIssue("Room", rowNumber, rowData, 
                                      missingFields, invalidFields);
        }
        return null;
    }
    
    /**
     * Validate event import data
     */
    public ValidationIssue validateEvent(Map<String, String> rowData, int rowNumber) {
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        
        // Required fields
        if (isNullOrEmpty(rowData.get("Event Name"))) {
            missingFields.add("Event Name");
        }
        if (isNullOrEmpty(rowData.get("Event Type"))) {
            missingFields.add("Event Type");
        } else {
            String eventType = rowData.get("Event Type").trim();
            Set<String> validTypes = Set.of(
                "Assembly", "Meeting", "Professional Development",
                "Holiday", "Early Dismissal", "Late Start",
                "Parent Conference", "Field Trip", "Sports Event"
            );
            if (!validTypes.contains(eventType)) {
                invalidFields.add("Event Type (invalid type)");
            }
        }
        if (isNullOrEmpty(rowData.get("Date"))) {
            missingFields.add("Date");
        } else {
            if (!isValidDate(rowData.get("Date"))) {
                invalidFields.add("Date (invalid format, use YYYY-MM-DD)");
            }
        }
        
        // Validate times if present
        String startTime = rowData.get("Start Time");
        if (!isNullOrEmpty(startTime) && !isValidTime(startTime)) {
            invalidFields.add("Start Time (invalid format, use HH:MM)");
        }
        
        String endTime = rowData.get("End Time");
        if (!isNullOrEmpty(endTime) && !isValidTime(endTime)) {
            invalidFields.add("End Time (invalid format, use HH:MM)");
        }
        
        if (!missingFields.isEmpty() || !invalidFields.isEmpty()) {
            return new ValidationIssue("Event", rowNumber, rowData, 
                                      missingFields, invalidFields);
        }
        return null;
    }
    
    /**
     * Validate substitute teacher import data
     */
    public ValidationIssue validateSubstitute(Map<String, String> rowData, int rowNumber) {
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        
        // Required fields (similar to teacher but less strict)
        if (isNullOrEmpty(rowData.get("First Name"))) {
            missingFields.add("First Name");
        }
        if (isNullOrEmpty(rowData.get("Last Name"))) {
            missingFields.add("Last Name");
        }
        if (isNullOrEmpty(rowData.get("Phone"))) {
            missingFields.add("Phone");
        } else {
            String phone = rowData.get("Phone").trim();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                invalidFields.add("Phone (must be 10 digits)");
            }
        }
        
        // Validate email if present
        String email = rowData.get("Email");
        if (!isNullOrEmpty(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            invalidFields.add("Email (invalid format)");
        }
        
        if (!missingFields.isEmpty() || !invalidFields.isEmpty()) {
            return new ValidationIssue("Substitute", rowNumber, rowData, 
                                      missingFields, invalidFields);
        }
        return null;
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Check if value is null or empty
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Validate date format (YYYY-MM-DD)
     */
    private boolean isValidDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }
        
        Pattern datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
        if (!datePattern.matcher(date).matches()) {
            return false;
        }
        
        try {
            String[] parts = date.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            
            if (year < 1900 || year > 2100) return false;
            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate time format (HH:MM)
     */
    private boolean isValidTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return false;
        }
        
        Pattern timePattern = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
        return timePattern.matcher(time).matches();
    }
    
    /**
     * Validate grade level
     */
    public boolean isValidGradeLevel(String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            return false;
        }
        return GRADE_LEVEL_PATTERN.matcher(grade.trim()).matches();
    }
    
    /**
     * Validate email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validate phone number
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }
}