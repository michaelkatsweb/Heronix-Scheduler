package com.heronix.model.dto;

import com.heronix.model.domain.Course;

/**
 * Course suggestion with priority and reasoning.
 *
 * Used to suggest courses to students based on their grade level,
 * completed courses, and prerequisites.
 *
 * @author Heronix Scheduler Team
 */
public class CourseSuggestion {

    public enum Priority {
        REQUIRED,      // Must take (core courses)
        RECOMMENDED,   // Strongly suggested (progression)
        OPTIONAL,      // Nice to have (electives)
        AVAILABLE      // Can take if interested
    }

    private Course course;
    private Priority priority;
    private String reason;
    private boolean prerequisitesMet;
    private String prerequisiteNote;

    public CourseSuggestion() {
    }

    public CourseSuggestion(Course course, Priority priority, String reason) {
        this.course = course;
        this.priority = priority;
        this.reason = reason;
        this.prerequisitesMet = true;
    }

    // Getters and Setters

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isPrerequisitesMet() {
        return prerequisitesMet;
    }

    public void setPrerequisitesMet(boolean prerequisitesMet) {
        this.prerequisitesMet = prerequisitesMet;
    }

    public String getPrerequisiteNote() {
        return prerequisiteNote;
    }

    public void setPrerequisiteNote(String prerequisiteNote) {
        this.prerequisiteNote = prerequisiteNote;
    }

    /**
     * Gets display text for UI.
     *
     * @return formatted suggestion text
     */
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        sb.append(course.getCourseCode()).append(" - ").append(course.getCourseName());
        sb.append(" [").append(priority).append("]");
        if (reason != null && !reason.isEmpty()) {
            sb.append(" - ").append(reason);
        }
        if (!prerequisitesMet && prerequisiteNote != null) {
            sb.append(" (⚠️ ").append(prerequisiteNote).append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CourseSuggestion{" +
                "course=" + (course != null ? course.getCourseCode() : "null") +
                ", priority=" + priority +
                ", reason='" + reason + '\'' +
                ", prerequisitesMet=" + prerequisitesMet +
                '}';
    }
}
