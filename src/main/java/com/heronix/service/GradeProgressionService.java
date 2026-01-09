package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.dto.ProgressionPreview;
import com.heronix.model.dto.ProgressionResult;
import com.heronix.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Service for automated grade progression.
 *
 * Handles the yearly cycle of graduating seniors and promoting all other students.
 *
 * @author Heronix Scheduler Team
 */
@Service
public class GradeProgressionService {

    private static final Logger log = LoggerFactory.getLogger(GradeProgressionService.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private StudentEnrollmentHistoryRepository enrollmentHistoryRepository;

    @Autowired
    private GradeProgressionHistoryRepository progressionHistoryRepository;

    @Autowired
    private AcademicYearService academicYearService;

    /**
     * Preview what will happen during progression (dry run).
     *
     * @param currentYear the current academic year
     * @return preview with statistics
     */
    public ProgressionPreview previewProgression(AcademicYear currentYear) {
        log.info("Generating progression preview for: {}", currentYear.getYearName());

        ProgressionPreview preview = new ProgressionPreview();

        // Count students by grade
        preview.setGrade9Count(studentRepository.countByGradeLevelAndActiveTrue("9"));
        preview.setGrade10Count(studentRepository.countByGradeLevelAndActiveTrue("10"));
        preview.setGrade11Count(studentRepository.countByGradeLevelAndActiveTrue("11"));
        preview.setGrade12Count(studentRepository.countByGradeLevelAndActiveTrue("12"));

        // Count total enrollments (will be archived)
        int totalEnrollments = 0;
        List<Student> allStudents = studentRepository.findByActiveTrue();
        for (Student student : allStudents) {
            if (student.getEnrolledCourses() != null) {
                totalEnrollments += student.getEnrolledCourses().size();
            }
        }
        preview.setTotalEnrollments(totalEnrollments);

        // Build grade distribution
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("Grade 9", preview.getGrade9Count());
        distribution.put("Grade 10", preview.getGrade10Count());
        distribution.put("Grade 11", preview.getGrade11Count());
        distribution.put("Grade 12", preview.getGrade12Count());
        preview.setGradeDistribution(distribution);

        log.info("Preview generated: {}", preview);
        return preview;
    }

    /**
     * Progress all students to next academic year.
     *
     * This is the main progression method that:
     * 1. Archives current enrollments
     * 2. Graduates seniors (grade 12)
     * 3. Promotes all other students
     * 4. Creates new academic year
     *
     * @param currentYear current academic year
     * @param newYearName name for new year (e.g., "2025-2026")
     * @param newStartDate start date of new year
     * @param newEndDate end date of new year
     * @param progressCallback optional callback for progress updates
     * @return result of progression
     */
    @Transactional
    public ProgressionResult progressToNextYear(
            AcademicYear currentYear,
            String newYearName,
            LocalDate newStartDate,
            LocalDate newEndDate,
            Consumer<String> progressCallback) {

        log.warn("========================================");
        log.warn("GRADE PROGRESSION INITIATED");
        log.warn("Current Year: {}", currentYear.getYearName());
        log.warn("New Year: {}", newYearName);
        log.warn("========================================");

        ProgressionResult result = new ProgressionResult();
        int seniorsGraduated = 0;
        int studentsPromoted = 0;
        int enrollmentsArchived = 0;

        try {
            // Step 1: Archive current enrollments
            updateProgress(progressCallback, "Archiving current enrollments...");
            enrollmentsArchived = archiveCurrentEnrollments(currentYear);
            log.info("Archived {} enrollments", enrollmentsArchived);

            // Step 2: Graduate seniors (grade 12)
            updateProgress(progressCallback, "Graduating seniors...");
            List<Student> seniors = studentRepository.findByGradeLevelAndActiveTrue("12");
            seniorsGraduated = graduateStudents(seniors, currentYear, LocalDate.now());
            log.info("Graduated {} seniors", seniorsGraduated);

            // Step 3: Promote Grade 11 → 12
            updateProgress(progressCallback, "Promoting Grade 11 → 12...");
            List<Student> grade11 = studentRepository.findByGradeLevelAndActiveTrue("11");
            for (Student student : grade11) {
                promoteStudent(student);
                studentsPromoted++;
            }
            studentRepository.saveAll(grade11);
            log.info("Promoted {} students: Grade 11 → 12", grade11.size());

            // Step 4: Promote Grade 10 → 11
            updateProgress(progressCallback, "Promoting Grade 10 → 11...");
            List<Student> grade10 = studentRepository.findByGradeLevelAndActiveTrue("10");
            for (Student student : grade10) {
                promoteStudent(student);
                studentsPromoted++;
            }
            studentRepository.saveAll(grade10);
            log.info("Promoted {} students: Grade 10 → 11", grade10.size());

            // Step 5: Promote Grade 9 → 10
            updateProgress(progressCallback, "Promoting Grade 9 → 10...");
            List<Student> grade9 = studentRepository.findByGradeLevelAndActiveTrue("9");
            for (Student student : grade9) {
                promoteStudent(student);
                studentsPromoted++;
            }
            studentRepository.saveAll(grade9);
            log.info("Promoted {} students: Grade 9 → 10", grade9.size());

            // Step 6: Clear current enrollments
            updateProgress(progressCallback, "Clearing current course assignments...");
            clearCurrentEnrollments();
            log.info("Cleared current course assignments");

            // Step 7: Mark current year as graduated
            updateProgress(progressCallback, "Updating current academic year...");
            currentYear.setGraduated(true);
            currentYear.setProgressionDate(LocalDate.now());
            currentYear.setActive(false);
            academicYearRepository.save(currentYear);

            // Step 8: Create new academic year
            updateProgress(progressCallback, "Creating new academic year...");
            AcademicYear newYear = academicYearService.createAcademicYear(
                newYearName, newStartDate, newEndDate);
            academicYearService.setActiveYear(newYear.getId());
            log.info("Created new academic year: {}", newYearName);

            // Step 9: Create progression history record
            updateProgress(progressCallback, "Recording progression history...");
            GradeProgressionHistory history = new GradeProgressionHistory(currentYear, LocalDate.now());
            history.setSeniorsGraduated(seniorsGraduated);
            history.setStudentsPromoted(studentsPromoted);
            history.setEnrollmentsArchived(enrollmentsArchived);
            progressionHistoryRepository.save(history);

            // Success!
            result.setSuccessful(true);
            result.setSeniorsGraduated(seniorsGraduated);
            result.setStudentsPromoted(studentsPromoted);
            result.setEnrollmentsArchived(enrollmentsArchived);
            result.setNewAcademicYearId(newYear.getId());
            result.setNewYearName(newYearName);
            result.setMessage("Successfully progressed to " + newYearName);

            updateProgress(progressCallback, "Progression complete!");

            log.warn("========================================");
            log.warn("GRADE PROGRESSION COMPLETED SUCCESSFULLY");
            log.warn("Seniors Graduated: {}", seniorsGraduated);
            log.warn("Students Promoted: {}", studentsPromoted);
            log.warn("Enrollments Archived: {}", enrollmentsArchived);
            log.warn("New Academic Year: {}", newYearName);
            log.warn("========================================");

            return result;

        } catch (Exception e) {
            log.error("Grade progression failed", e);
            result.setSuccessful(false);
            result.setMessage("Progression failed: " + e.getMessage());
            throw new RuntimeException("Grade progression failed", e);
        }
    }

    /**
     * Archives current enrollments to history.
     *
     * @param currentYear the current academic year
     * @return count of enrollments archived
     */
    private int archiveCurrentEnrollments(AcademicYear currentYear) {
        List<Student> allStudents = studentRepository.findByActiveTrue();
        List<StudentEnrollmentHistory> historyRecords = new ArrayList<>();

        for (Student student : allStudents) {
            if (student.getEnrolledCourses() != null) {
                for (Course course : student.getEnrolledCourses()) {
                    StudentEnrollmentHistory history = new StudentEnrollmentHistory(
                        student, course, currentYear, Integer.parseInt(student.getGradeLevel()));
                    history.setStatus("COMPLETED");
                    historyRecords.add(history);
                }
            }
        }

        enrollmentHistoryRepository.saveAll(historyRecords);
        return historyRecords.size();
    }

    /**
     * Graduates a list of students.
     *
     * @param seniors list of senior students
     * @param academicYear the academic year they're graduating from
     * @param graduationDate graduation date
     * @return count of students graduated
     */
    private int graduateStudents(List<Student> seniors, AcademicYear academicYear, LocalDate graduationDate) {
        for (Student senior : seniors) {
            senior.setGraduated(true);
            senior.setGraduatedDate(graduationDate);
            senior.setAcademicYearId(academicYear.getId());
            // Optionally set active=false to hide graduated students
            // senior.setActive(false);
        }
        studentRepository.saveAll(seniors);
        return seniors.size();
    }

    /**
     * Promotes a student to next grade level.
     *
     * @param student the student to promote
     */
    private void promoteStudent(Student student) {
        int currentGrade = Integer.parseInt(student.getGradeLevel());
        int nextGrade = currentGrade + 1;
        student.setGradeLevel(String.valueOf(nextGrade));

        // Update graduation year if not set
        if (student.getGraduationYear() == null) {
            int yearsUntilGraduation = 12 - nextGrade;
            int currentYear = LocalDate.now().getYear();
            student.setGraduationYear(currentYear + yearsUntilGraduation);
        }
    }

    /**
     * Clears current course enrollments for all students.
     */
    private void clearCurrentEnrollments() {
        List<Student> allStudents = studentRepository.findByActiveTrue();
        for (Student student : allStudents) {
            if (student.getEnrolledCourses() != null) {
                student.getEnrolledCourses().clear();
            }
        }
        studentRepository.saveAll(allStudents);
    }

    /**
     * Updates progress via callback if provided.
     *
     * @param callback progress callback
     * @param message progress message
     */
    private void updateProgress(Consumer<String> callback, String message) {
        if (callback != null) {
            callback.accept(message);
        }
    }
}
