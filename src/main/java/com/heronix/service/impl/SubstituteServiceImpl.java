// Location: src/main/java/com/eduscheduler/service/impl/SubstituteServiceImpl.java
package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.SlotStatus;
import com.heronix.repository.*;
import com.heronix.service.SubstituteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Substitute Service Implementation
 * Location:
 * src/main/java/com/eduscheduler/service/impl/SubstituteServiceImpl.java
 */
@Slf4j
@Service
@Transactional
public class SubstituteServiceImpl implements SubstituteService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Override
    public List<Teacher> findAvailableSubstitutes(Long slotId) {
        ScheduleSlot slot = scheduleSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (slot.getDayOfWeek() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            return Collections.emptyList();
        }

        // Get all active teachers
        List<Teacher> allTeachers = teacherRepository.findByActiveTrue();

        // Filter out the original teacher
        // ✅ NULL SAFE: Check teacher and teacher ID exist
        if (slot.getTeacher() != null && slot.getTeacher().getId() != null) {
            allTeachers = allTeachers.stream()
                    .filter(t -> t != null && t.getId() != null &&
                                 !t.getId().equals(slot.getTeacher().getId()))
                    .collect(Collectors.toList());
        }

        // Find teachers without conflicts at this time
        List<Teacher> available = new ArrayList<>();
        for (Teacher teacher : allTeachers) {
            // ✅ NULL SAFE: Skip null teachers or teachers with null ID
            if (teacher == null || teacher.getId() == null) continue;

            List<ScheduleSlot> conflicts = scheduleSlotRepository.findTeacherTimeConflicts(
                    teacher.getId(),
                    slot.getDayOfWeek(),
                    slot.getStartTime(),
                    slot.getEndTime());

            if (conflicts.isEmpty()) {
                available.add(teacher);
            }
        }

        // Sort by certification match
        if (slot.getCourse() != null && slot.getCourse().getSubject() != null) {
            String subject = slot.getCourse().getSubject();
            available.sort((t1, t2) -> {
                // ✅ NULL SAFE: Check certifications exist before streaming
                boolean t1Match = (t1 != null && t1.getCertifications() != null) &&
                    t1.getCertifications().stream()
                        .filter(cert -> cert != null)
                        .anyMatch(cert -> cert.toLowerCase().contains(subject.toLowerCase()));
                boolean t2Match = (t2 != null && t2.getCertifications() != null) &&
                    t2.getCertifications().stream()
                        .filter(cert -> cert != null)
                        .anyMatch(cert -> cert.toLowerCase().contains(subject.toLowerCase()));

                if (t1Match && !t2Match)
                    return -1;
                if (!t1Match && t2Match)
                    return 1;
                return 0;
            });
        }

        log.info("Found {} available substitutes for slot {}", available.size(), slotId);
        return available;
    }

    @Override
    public ScheduleSlot assignSubstitute(Long slotId, Long substituteTeacherId) {
        ScheduleSlot slot = scheduleSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        Teacher substitute = teacherRepository.findById(substituteTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + substituteTeacherId));

        // Check for conflicts
        if (slot.getDayOfWeek() != null && slot.getStartTime() != null && slot.getEndTime() != null) {
            List<ScheduleSlot> conflicts = scheduleSlotRepository.findTeacherTimeConflicts(
                    substituteTeacherId,
                    slot.getDayOfWeek(),
                    slot.getStartTime(),
                    slot.getEndTime());

            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("Substitute teacher has a conflict at this time");
            }
        }

        slot.setSubstituteTeacher(substitute);
        slot.setStatus(SlotStatus.SUBSTITUTE_NEEDED);

        ScheduleSlot saved = scheduleSlotRepository.save(slot);
        // ✅ NULL SAFE: Safe extraction of substitute name
        String substituteName = (substitute != null && substitute.getName() != null)
            ? substitute.getName() : "Unknown";
        log.info("Assigned substitute {} to slot {}", substituteName, slotId);

        return saved;
    }

    @Override
    public List<ScheduleSlot> handleTeacherAbsence(Long teacherId, LocalDate date) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        List<ScheduleSlot> affectedSlots = scheduleSlotRepository.findTeacherScheduleByDay(
                teacherId, dayOfWeek);

        List<ScheduleSlot> updated = new ArrayList<>();

        for (ScheduleSlot slot : affectedSlots) {
            // ✅ NULL SAFE: Skip null slots or slots without ID
            if (slot == null || slot.getId() == null) continue;

            // Find best substitute
            Teacher substitute = findBestMatchSubstitute(slot.getId());

            if (substitute != null) {
                slot.setSubstituteTeacher(substitute);
                slot.setStatus(SlotStatus.SUBSTITUTE_ASSIGNED);
                // ✅ NULL SAFE: Safe extraction of substitute name
                String substituteName = (substitute.getName() != null) ? substitute.getName() : "Unknown";
                log.info("Auto-assigned substitute {} for slot {}", substituteName, slot.getId());
            } else {
                slot.setStatus(SlotStatus.ACTIVE);
                log.warn("No substitute found for slot {}", slot.getId());
            }

            updated.add(scheduleSlotRepository.save(slot));
        }

        // ✅ NULL SAFE: Safe extraction of teacher name and stream filtering
        String teacherName = (teacher != null && teacher.getName() != null) ? teacher.getName() : "Unknown";
        log.info("Handled absence for {} on {}: {} slots affected, {} auto-assigned",
                teacherName, date, affectedSlots.size(),
                updated.stream().filter(s -> s != null && s.getSubstituteTeacher() != null).count());

        return updated;
    }

    @Override
    public List<ScheduleSlot> getSubstituteAssignments(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        return scheduleSlotRepository.findByDayOfWeek(dayOfWeek).stream()
                .filter(slot -> slot.getSubstituteTeacher() != null)
                .collect(Collectors.toList());
    }

    @Override
    public ScheduleSlot removeSubstitute(Long slotId) {
        ScheduleSlot slot = scheduleSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        slot.setSubstituteTeacher(null);
        slot.setStatus(SlotStatus.CANCELLED);

        return scheduleSlotRepository.save(slot);
    }

    @Override
    public List<ScheduleSlot> getSubstituteHistory(Long teacherId) {
        // Find all slots where this teacher was a substitute
        // ✅ NULL SAFE: Filter null slots before accessing substitute teacher
        return scheduleSlotRepository.findAll().stream()
                .filter(slot -> slot != null && slot.getSubstituteTeacher() != null &&
                        slot.getSubstituteTeacher().getId() != null &&
                        slot.getSubstituteTeacher().getId().equals(teacherId))
                .collect(Collectors.toList());
    }

    @Override
    public Teacher findBestMatchSubstitute(Long slotId) {
        List<Teacher> available = findAvailableSubstitutes(slotId);

        if (available.isEmpty()) {
            return null;
        }

        ScheduleSlot slot = scheduleSlotRepository.findById(slotId).orElse(null);
        if (slot == null || slot.getCourse() == null || slot.getCourse().getSubject() == null) {
            return available.get(0); // Return first available
        }

        String subject = slot.getCourse().getSubject();

        // Find teacher with matching certification
        // ✅ NULL SAFE: Filter null teachers and certifications before checking match
        Optional<Teacher> bestMatch = available.stream()
                .filter(teacher -> teacher != null && teacher.getCertifications() != null)
                .filter(teacher -> teacher.getCertifications().stream()
                        .filter(cert -> cert != null)
                        .anyMatch(cert -> cert.toLowerCase().contains(subject.toLowerCase())))
                .findFirst();

        return bestMatch.orElse(available.get(0));
    }
}
