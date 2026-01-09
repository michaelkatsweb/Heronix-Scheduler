// Location: src/main/java/com/eduscheduler/service/SubstituteService.java
package com.heronix.service;

import com.heronix.model.domain.Teacher;
import com.heronix.model.domain.ScheduleSlot;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing substitute teachers
 */
public interface SubstituteService {

    /**
     * Find available substitute teachers for a slot
     */
    List<Teacher> findAvailableSubstitutes(Long slotId);

    /**
     * Assign substitute to a slot
     */
    ScheduleSlot assignSubstitute(Long slotId, Long substituteTeacherId);

    /**
     * Mark teacher as absent and auto-assign substitutes
     */
    List<ScheduleSlot> handleTeacherAbsence(Long teacherId, LocalDate date);

    /**
     * Get substitute assignments for a date
     */
    List<ScheduleSlot> getSubstituteAssignments(LocalDate date);

    /**
     * Remove substitute assignment
     */
    ScheduleSlot removeSubstitute(Long slotId);

    /**
     * Get substitute history for a teacher
     */
    List<ScheduleSlot> getSubstituteHistory(Long teacherId);

    /**
     * Find best matching substitute based on certifications
     */
    Teacher findBestMatchSubstitute(Long slotId);
}
