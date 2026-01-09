package com.heronix.model.enums;

/**
 * Constraint Type Enum
 * Defines types of scheduling constraints for optimization
 *
 * Location: src/main/java/com/eduscheduler/model/enums/ConstraintType.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
public enum ConstraintType {

    // ========================================================================
    // HARD CONSTRAINTS (Must be satisfied)
    // ========================================================================

    /**
     * No teacher can be in two places at once
     */
    NO_TEACHER_OVERLAP("No Teacher Overlap", "Teachers cannot be double-booked", ConstraintCategory.HARD),

    /**
     * No room can host two classes simultaneously
     */
    NO_ROOM_OVERLAP("No Room Overlap", "Rooms cannot be double-booked", ConstraintCategory.HARD),

    /**
     * No student can attend two classes at once
     */
    NO_STUDENT_OVERLAP("No Student Overlap", "Students cannot be in two places at once", ConstraintCategory.HARD),

    /**
     * Room must have sufficient capacity
     */
    ROOM_CAPACITY("Room Capacity", "Room must fit all enrolled students", ConstraintCategory.HARD),

    /**
     * Teacher must be qualified for subject
     */
    TEACHER_QUALIFICATION("Teacher Qualification", "Teacher must be certified for subject", ConstraintCategory.HARD),

    /**
     * Required equipment must be available
     */
    EQUIPMENT_AVAILABLE("Equipment Available", "Required equipment must be in room", ConstraintCategory.HARD),

    /**
     * All courses must be scheduled
     */
    ALL_COURSES_SCHEDULED("All Courses Scheduled", "Every course must have a time slot", ConstraintCategory.HARD),

    // ========================================================================
    // SOFT CONSTRAINTS (Prefer to satisfy, but not required)
    // ========================================================================

    /**
     * Minimize consecutive classes for students
     */
    MINIMIZE_STUDENT_GAPS("Minimize Student Gaps", "Reduce gaps between student classes", ConstraintCategory.SOFT),

    /**
     * Balance teacher workload
     */
    BALANCE_TEACHER_LOAD("Balance Teacher Load", "Distribute hours evenly among teachers", ConstraintCategory.SOFT),

    /**
     * Provide lunch breaks
     */
    LUNCH_BREAK("Lunch Break", "Everyone gets a lunch period", ConstraintCategory.SOFT),

    /**
     * Minimize teacher travel between buildings
     */
    MINIMIZE_TEACHER_TRAVEL("Minimize Teacher Travel", "Keep teacher classes in same building", ConstraintCategory.SOFT),

    /**
     * Respect teacher time preferences
     */
    TEACHER_PREFERENCES("Teacher Preferences", "Honor preferred time slots", ConstraintCategory.SOFT),

    /**
     * Spread difficult courses throughout day
     */
    SPREAD_DIFFICULT_COURSES("Spread Difficult Courses", "Avoid stacking challenging classes", ConstraintCategory.SOFT),

    /**
     * Optimize room utilization
     */
    OPTIMIZE_ROOM_USAGE("Optimize Room Usage", "Maximize room utilization rates", ConstraintCategory.SOFT),

    /**
     * Group related courses
     */
    GROUP_RELATED_COURSES("Group Related Courses", "Schedule related subjects near each other", ConstraintCategory.SOFT),

    /**
     * Minimize student travel between buildings
     */
    MINIMIZE_STUDENT_TRAVEL("Minimize Student Travel", "Keep student classes in same building", ConstraintCategory.SOFT),

    /**
     * Provide teacher preparation periods
     */
    TEACHER_PREP_PERIODS("Teacher Prep Periods", "Ensure teachers have prep time", ConstraintCategory.SOFT),

    /**
     * Respect room preferences for courses
     */
    ROOM_PREFERENCES("Room Preferences", "Use preferred rooms when possible", ConstraintCategory.SOFT),

    /**
     * Balance class sizes
     */
    BALANCE_CLASS_SIZES("Balance Class Sizes", "Distribute students evenly across sections", ConstraintCategory.SOFT),

    /**
     * Avoid early morning advanced courses
     */
    AVOID_EARLY_ADVANCED("Avoid Early Advanced Courses", "Schedule difficult courses mid-morning", ConstraintCategory.SOFT),

    /**
     * Consecutive classes in same subject
     */
    CONSECUTIVE_SAME_SUBJECT("Consecutive Same Subject", "Schedule subject blocks together", ConstraintCategory.SOFT);

    // ========================================================================
    // ENUM FIELDS
    // ========================================================================

    private final String displayName;
    private final String description;
    private final ConstraintCategory category;

    // ========================================================================
    // CONSTRAINT CATEGORIES
    // ========================================================================

    public enum ConstraintCategory {
        HARD("Hard Constraint", "Must be satisfied", 1000),
        SOFT("Soft Constraint", "Prefer to satisfy", 100);

        private final String displayName;
        private final String description;
        private final int baseWeight;

        ConstraintCategory(String displayName, String description, int baseWeight) {
            this.displayName = displayName;
            this.description = description;
            this.baseWeight = baseWeight;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getBaseWeight() { return baseWeight; }
    }

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    ConstraintType(String displayName, String description, ConstraintCategory category) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public ConstraintCategory getCategory() {
        return category;
    }

    public boolean isHard() {
        return category == ConstraintCategory.HARD;
    }

    public boolean isSoft() {
        return category == ConstraintCategory.SOFT;
    }

    /**
     * Get default weight for this constraint
     */
    public int getDefaultWeight() {
        return category.getBaseWeight();
    }

    /**
     * Get icon for constraint category
     */
    public String getIcon() {
        return switch (category) {
            case HARD -> "🔴";
            case SOFT -> "🟡";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
