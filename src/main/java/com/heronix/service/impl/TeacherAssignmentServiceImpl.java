package com.heronix.service.impl;

import com.heronix.model.domain.Teacher;
import com.heronix.model.domain.Room;
import com.heronix.model.domain.Course;
import com.heronix.repository.TeacherRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.service.TeacherAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Teacher Assignment Service Implementation
 * Manages teacher-room assignments and multi-subject certifications
 *
 * Location: src/main/java/com/eduscheduler/service/impl/TeacherAssignmentServiceImpl.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public Teacher assignHomeRoom(Teacher teacher, Room room) {
        log.info("Assigning home room {} to teacher {}", room.getRoomNumber(), teacher.getName());
        teacher.setHomeRoom(room);
        return teacherRepository.save(teacher);
    }

    @Override
    public Room getHomeRoom(Teacher teacher) {
        return teacher.getHomeRoom();
    }

    @Override
    public boolean isCertifiedFor(Teacher teacher, String subject) {
        if (teacher == null || subject == null) {
            return false;
        }

        String normalizedSubject = subject.toLowerCase().trim();

        // Check SubjectCertification entities (preferred method)
        if (teacher.hasCertificationForSubject(subject)) {
            return true;
        }

        // Check legacy certifications list
        // ✅ NULL SAFE: Filter null certifications before processing
        if (teacher.getCertifications() != null) {
            for (String cert : teacher.getCertifications()) {
                if (cert != null && cert.toLowerCase().trim().contains(normalizedSubject)) {
                    return true;
                }
            }
        }

        // Check department match as fallback
        if (teacher.getDepartment() != null) {
            String normalizedDept = teacher.getDepartment().toLowerCase().trim();
            if (normalizedDept.contains(normalizedSubject) ||
                normalizedSubject.contains(normalizedDept)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public Teacher addCertification(Teacher teacher, String subject) {
        log.info("Adding certification '{}' to teacher {}", subject, teacher.getName());

        if (teacher.getCertifications() == null) {
            teacher.setCertifications(new ArrayList<>());
        }

        if (!teacher.getCertifications().contains(subject)) {
            teacher.getCertifications().add(subject);
            return teacherRepository.save(teacher);
        }

        return teacher;
    }

    @Override
    @Transactional
    public Teacher removeCertification(Teacher teacher, String subject) {
        log.info("Removing certification '{}' from teacher {}", subject, teacher.getName());

        if (teacher.getCertifications() != null) {
            teacher.getCertifications().remove(subject);
            return teacherRepository.save(teacher);
        }

        return teacher;
    }

    @Override
    public List<String> getCertifiedSubjects(Teacher teacher) {
        List<String> subjects = new ArrayList<>();

        // Add from SubjectCertification entities
        // ✅ NULL SAFE: Filter null certifications before checking validity
        if (teacher.getSubjectCertifications() != null) {
            subjects.addAll(
                teacher.getSubjectCertifications().stream()
                    .filter(cert -> cert != null && cert.isValid() && cert.getSubject() != null)
                    .map(cert -> cert.getSubject())
                    .collect(Collectors.toList())
            );
        }

        // Add from legacy certifications list
        // ✅ NULL SAFE: Check cert not null before adding
        if (teacher.getCertifications() != null) {
            for (String cert : teacher.getCertifications()) {
                if (cert != null && !subjects.contains(cert)) {
                    subjects.add(cert);
                }
            }
        }

        return subjects;
    }

    @Override
    public List<Course> getEligibleCourses(Teacher teacher) {
        List<Course> allCourses = courseRepository.findAll();

        // ✅ NULL SAFE: Filter null courses before checking active status
        return allCourses.stream()
            .filter(course -> course != null && Boolean.TRUE.equals(course.getActive()))
            .filter(course -> canTeachCourse(teacher, course))
            .collect(Collectors.toList());
    }

    @Override
    public boolean canTeachCourse(Teacher teacher, Course course) {
        if (teacher == null || course == null) {
            return false;
        }

        // Check if teacher is active
        if (!Boolean.TRUE.equals(teacher.getActive())) {
            return false;
        }

        // Check if course is active
        if (!Boolean.TRUE.equals(course.getActive())) {
            return false;
        }

        // Check if teacher is certified for the subject
        if (course.getSubject() != null) {
            return isCertifiedFor(teacher, course.getSubject());
        }

        // If no subject specified, use subject as department proxy
        // Course doesn't have department field, so use subject instead
        if (course.getSubject() != null && teacher.getDepartment() != null) {
            return course.getSubject().equalsIgnoreCase(teacher.getDepartment());
        }

        return false;
    }

    @Override
    public List<Teacher> getTeachersCertifiedFor(String subject) {
        List<Teacher> allTeachers = teacherRepository.findAllActive();

        // ✅ NULL SAFE: Filter null teachers before checking active status
        return allTeachers.stream()
            .filter(teacher -> teacher != null && Boolean.TRUE.equals(teacher.getActive()))
            .filter(teacher -> isCertifiedFor(teacher, subject))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Teacher updateMaxPeriodsPerDay(Teacher teacher, Integer maxPeriods) {
        log.info("Updating max periods per day for teacher {} to {}", teacher.getName(), maxPeriods);
        teacher.setMaxPeriodsPerDay(maxPeriods);
        return teacherRepository.save(teacher);
    }

    @Override
    public TeacherAssignmentValidation validateAssignment(Teacher teacher, Course course, Integer periodNumber) {
        TeacherAssignmentValidation validation = new TeacherAssignmentValidation(true);

        // Check if teacher is active
        if (!Boolean.TRUE.equals(teacher.getActive())) {
            validation.addIssue("Teacher is not active");
        }

        // Check if course is active
        if (!Boolean.TRUE.equals(course.getActive())) {
            validation.addIssue("Course is not active");
        }

        // Check certification
        if (course.getSubject() != null && !isCertifiedFor(teacher, course.getSubject())) {
            validation.addWarning("Teacher is not certified for subject: " + course.getSubject());
        }

        // Check max periods per day
        if (teacher.getMaxPeriodsPerDay() != null && periodNumber != null) {
            if (periodNumber > teacher.getMaxPeriodsPerDay()) {
                validation.addIssue("Period " + periodNumber + " exceeds teacher's max periods per day (" +
                    teacher.getMaxPeriodsPerDay() + ")");
            }
        }

        // Check workload
        if (teacher.getMaxHoursPerWeek() != null && teacher.getCurrentWeekHours() != null) {
            if (teacher.getCurrentWeekHours() >= teacher.getMaxHoursPerWeek()) {
                validation.addIssue("Teacher has reached maximum weekly hours (" +
                    teacher.getMaxHoursPerWeek() + ")");
            } else if (teacher.getCurrentWeekHours() >= teacher.getMaxHoursPerWeek() * 0.9) {
                validation.addWarning("Teacher is approaching maximum weekly hours");
            }
        }

        // Check if teacher has been assigned to this course before (preferred)
        if (teacher.getCourses() != null && teacher.getCourses().contains(course)) {
            log.debug("Teacher has been assigned to this course before (preferred match)");
        }

        return validation;
    }
}
