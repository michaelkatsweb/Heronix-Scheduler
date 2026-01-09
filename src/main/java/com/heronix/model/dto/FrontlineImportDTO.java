package com.heronix.model.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for importing substitute assignment data from Frontline CSV files
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
public class FrontlineImportDTO {

    // Substitute Information
    private String substituteFirstName;
    private String substituteLastName;
    private String substituteEmployeeId;
    private String substituteEmail;
    private String substitutePhone;
    private String substituteType;  // "Certified", "Uncertified", "Para", "Long-Term"

    // Assignment Information
    private String jobId;  // Frontline job ID
    private LocalDate assignmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate endDate;  // For multi-day assignments

    // Staff Being Replaced
    private String replacedStaffFirstName;
    private String replacedStaffLastName;
    private String replacedStaffEmployeeId;
    private String replacedStaffType;  // "Teacher", "Co-Teacher", "Para", etc.

    // Assignment Details
    private String absenceReason;  // "Sick", "Personal", "IEP Meeting", etc.
    private String durationType;  // "Hourly", "Half-Day", "Full-Day", "Multi-Day", "Long-Term"
    private String status;  // "Pending", "Confirmed", "Completed", etc.
    private Boolean isFloater;

    // Location/Course Information
    private String roomNumber;
    private String courseName;
    private String courseCode;
    private String subject;
    private String gradeLevel;

    // Additional Information
    private String notes;
    private Double hours;
    private Double payAmount;

    // Import Metadata
    private Integer rowNumber;  // Original row number in CSV
    private String rawData;  // Original CSV line for debugging

    /**
     * Default constructor
     */
    public FrontlineImportDTO() {
        this.isFloater = false;
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getSubstituteFirstName() {
        return substituteFirstName;
    }

    public void setSubstituteFirstName(String substituteFirstName) {
        this.substituteFirstName = substituteFirstName;
    }

    public String getSubstituteLastName() {
        return substituteLastName;
    }

    public void setSubstituteLastName(String substituteLastName) {
        this.substituteLastName = substituteLastName;
    }

    public String getSubstituteEmployeeId() {
        return substituteEmployeeId;
    }

    public void setSubstituteEmployeeId(String substituteEmployeeId) {
        this.substituteEmployeeId = substituteEmployeeId;
    }

    public String getSubstituteEmail() {
        return substituteEmail;
    }

    public void setSubstituteEmail(String substituteEmail) {
        this.substituteEmail = substituteEmail;
    }

    public String getSubstitutePhone() {
        return substitutePhone;
    }

    public void setSubstitutePhone(String substitutePhone) {
        this.substitutePhone = substitutePhone;
    }

    public String getSubstituteType() {
        return substituteType;
    }

    public void setSubstituteType(String substituteType) {
        this.substituteType = substituteType;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReplacedStaffFirstName() {
        return replacedStaffFirstName;
    }

    public void setReplacedStaffFirstName(String replacedStaffFirstName) {
        this.replacedStaffFirstName = replacedStaffFirstName;
    }

    public String getReplacedStaffLastName() {
        return replacedStaffLastName;
    }

    public void setReplacedStaffLastName(String replacedStaffLastName) {
        this.replacedStaffLastName = replacedStaffLastName;
    }

    public String getReplacedStaffEmployeeId() {
        return replacedStaffEmployeeId;
    }

    public void setReplacedStaffEmployeeId(String replacedStaffEmployeeId) {
        this.replacedStaffEmployeeId = replacedStaffEmployeeId;
    }

    public String getReplacedStaffType() {
        return replacedStaffType;
    }

    public void setReplacedStaffType(String replacedStaffType) {
        this.replacedStaffType = replacedStaffType;
    }

    public String getAbsenceReason() {
        return absenceReason;
    }

    public void setAbsenceReason(String absenceReason) {
        this.absenceReason = absenceReason;
    }

    public String getDurationType() {
        return durationType;
    }

    public void setDurationType(String durationType) {
        this.durationType = durationType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsFloater() {
        return isFloater;
    }

    public void setIsFloater(Boolean isFloater) {
        this.isFloater = isFloater;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Double getHours() {
        return hours;
    }

    public void setHours(Double hours) {
        this.hours = hours;
    }

    public Double getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Double payAmount) {
        this.payAmount = payAmount;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get full name of substitute
     */
    public String getSubstituteFullName() {
        if (substituteFirstName == null && substituteLastName == null) {
            return "Unknown";
        }
        return (substituteFirstName != null ? substituteFirstName : "") + " " +
               (substituteLastName != null ? substituteLastName : "");
    }

    /**
     * Get full name of replaced staff
     */
    public String getReplacedStaffFullName() {
        if (replacedStaffFirstName == null && replacedStaffLastName == null) {
            return "Unknown";
        }
        return (replacedStaffFirstName != null ? replacedStaffFirstName : "") + " " +
               (replacedStaffLastName != null ? replacedStaffLastName : "");
    }

    /**
     * Check if this is a valid import record
     */
    public boolean isValid() {
        return substituteFirstName != null && !substituteFirstName.trim().isEmpty() &&
               substituteLastName != null && !substituteLastName.trim().isEmpty() &&
               assignmentDate != null &&
               startTime != null &&
               endTime != null;
    }

    /**
     * Get validation error message
     */
    public String getValidationError() {
        if (substituteFirstName == null || substituteFirstName.trim().isEmpty()) {
            return "Substitute first name is required";
        }
        if (substituteLastName == null || substituteLastName.trim().isEmpty()) {
            return "Substitute last name is required";
        }
        if (assignmentDate == null) {
            return "Assignment date is required";
        }
        if (startTime == null) {
            return "Start time is required";
        }
        if (endTime == null) {
            return "End time is required";
        }
        return null;
    }

    @Override
    public String toString() {
        return "FrontlineImportDTO{" +
                "jobId='" + jobId + '\'' +
                ", substitute='" + getSubstituteFullName() + '\'' +
                ", date=" + assignmentDate +
                ", replacedStaff='" + getReplacedStaffFullName() + '\'' +
                ", durationType='" + durationType + '\'' +
                '}';
    }
}
