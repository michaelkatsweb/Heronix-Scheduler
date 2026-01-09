package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.repository.TranscriptRepository;
import com.heronix.repository.StudentRepository;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Transcript Service
 * Manages academic transcripts, GPA calculation, and grade reporting.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Transcript System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final TranscriptRepository transcriptRepository;
    private final StudentRepository studentRepository;

    /**
     * Generate full transcript for a student
     */
    public StudentTranscript generateTranscript(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<TranscriptRecord> records = transcriptRepository
            .findByStudentIdOrderByAcademicYearDescSemesterDesc(studentId);

        // ✅ NULL SAFE: Validate records list
        if (records == null) {
            records = new ArrayList<>();
        }

        // Group by academic year
        // ✅ NULL SAFE: Filter null records and check academic year exists
        Map<String, List<TranscriptRecord>> byYear = records.stream()
            .filter(r -> r != null && r.getAcademicYear() != null)
            .collect(Collectors.groupingBy(TranscriptRecord::getAcademicYear));

        List<AcademicYearSummary> yearSummaries = new ArrayList<>();
        for (Map.Entry<String, List<TranscriptRecord>> entry : byYear.entrySet()) {
            yearSummaries.add(buildYearSummary(entry.getKey(), entry.getValue()));
        }

        // Sort by year descending
        yearSummaries.sort((a, b) -> b.getAcademicYear().compareTo(a.getAcademicYear()));

        // Calculate cumulative GPA
        BigDecimal cumulativeGpa = calculateCumulativeGpa(studentId);
        BigDecimal weightedGpa = calculateWeightedGpa(studentId);
        BigDecimal totalCredits = transcriptRepository.sumCreditsEarnedByStudent(studentId);

        // ✅ NULL SAFE: Safe extraction of student names
        String firstName = (student.getFirstName() != null) ? student.getFirstName() : "";
        String lastName = (student.getLastName() != null) ? student.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) fullName = "Unknown Student";

        return StudentTranscript.builder()
            .studentId(studentId)
            .studentName(fullName)
            .studentNumber(student.getStudentId())
            .currentGradeLevel(student.getGradeLevel())
            .academicYears(yearSummaries)
            .cumulativeGpa(cumulativeGpa != null ? cumulativeGpa : BigDecimal.ZERO)
            .weightedGpa(weightedGpa != null ? weightedGpa : BigDecimal.ZERO)
            .totalCreditsEarned(totalCredits != null ? totalCredits : BigDecimal.ZERO)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Calculate cumulative (unweighted) GPA
     */
    public BigDecimal calculateCumulativeGpa(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            return BigDecimal.ZERO;
        }
        return transcriptRepository.calculateUnweightedGpa(studentId);
    }

    /**
     * Calculate weighted GPA
     */
    public BigDecimal calculateWeightedGpa(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            return BigDecimal.ZERO;
        }
        return transcriptRepository.calculateWeightedGpa(studentId);
    }

    /**
     * Add a grade to student transcript
     */
    @Transactional
    public TranscriptRecord addGrade(Long studentId, Long courseId, String academicYear,
            TranscriptRecord.Semester semester, String letterGrade, BigDecimal numericGrade,
            BigDecimal credits, TranscriptRecord.CourseType courseType) {

        // ✅ NULL SAFE: Validate parameters
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        if (academicYear == null || academicYear.isEmpty()) {
            throw new IllegalArgumentException("Academic year cannot be null or empty");
        }
        if (semester == null) {
            throw new IllegalArgumentException("Semester cannot be null");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        BigDecimal gradePoints = TranscriptRecord.letterToGradePoints(letterGrade);
        BigDecimal weightFactor = getWeightFactor(courseType);

        TranscriptRecord record = TranscriptRecord.builder()
            .student(student)
            .academicYear(academicYear)
            .semester(semester)
            .letterGrade(letterGrade)
            .numericGrade(numericGrade)
            .gradePoints(gradePoints)
            .creditsAttempted(credits)
            .creditsEarned(isPassing(letterGrade) ? credits : BigDecimal.ZERO)
            .courseType(courseType)
            .weighted(courseType != TranscriptRecord.CourseType.REGULAR)
            .weightFactor(weightFactor)
            .gradeLevel(Integer.parseInt(student.getGradeLevel()))
            .includeInGpa(true)
            .build();

        return transcriptRepository.save(record);
    }

    /**
     * Get class rank
     */
    public ClassRankInfo getClassRank(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        String gradeLevel = student.getGradeLevel();

        // Get all students in same grade level
        List<Student> classmates = studentRepository.findByGradeLevel(gradeLevel);
        // ✅ NULL SAFE: Validate classmates list
        if (classmates == null) {
            classmates = new ArrayList<>();
        }

        // Calculate GPAs for all students
        List<StudentGpaInfo> gpas = new ArrayList<>();
        for (Student s : classmates) {
            // ✅ NULL SAFE: Skip null students or students with null ID
            if (s == null || s.getId() == null) continue;
            BigDecimal gpa = calculateWeightedGpa(s.getId());
            if (gpa != null) {
                gpas.add(new StudentGpaInfo(s.getId(), gpa));
            }
        }

        // Sort by GPA descending
        gpas.sort((a, b) -> b.getGpa().compareTo(a.getGpa()));

        // Find student's rank
        int rank = 1;
        for (StudentGpaInfo info : gpas) {
            if (info.getStudentId().equals(studentId)) {
                break;
            }
            rank++;
        }

        BigDecimal studentGpa = calculateWeightedGpa(studentId);
        BigDecimal percentile = gpas.isEmpty() ? BigDecimal.ZERO :
            new BigDecimal(100 - (rank * 100.0 / gpas.size()))
                .setScale(1, RoundingMode.HALF_UP);

        return ClassRankInfo.builder()
            .studentId(studentId)
            .rank(rank)
            .totalStudents(gpas.size())
            .gradeLevel(gradeLevel)
            .gpa(studentGpa != null ? studentGpa : BigDecimal.ZERO)
            .percentile(percentile)
            .build();
    }

    /**
     * Check graduation requirements
     */
    public GraduationStatus checkGraduationRequirements(Long studentId) {
        BigDecimal creditsEarned = transcriptRepository.sumCreditsEarnedByStudent(studentId);
        long coursesCompleted = transcriptRepository.countCompletedCoursesByStudent(studentId);

        // Default requirements (can be configured)
        BigDecimal requiredCredits = new BigDecimal("24.0");
        boolean meetsCredits = creditsEarned != null &&
            creditsEarned.compareTo(requiredCredits) >= 0;

        return GraduationStatus.builder()
            .studentId(studentId)
            .creditsEarned(creditsEarned != null ? creditsEarned : BigDecimal.ZERO)
            .creditsRequired(requiredCredits)
            .coursesCompleted((int) coursesCompleted)
            .meetsRequirements(meetsCredits)
            .projectedGraduationYear(calculateProjectedGradYear(studentId))
            .build();
    }

    // Helper methods

    private AcademicYearSummary buildYearSummary(String year, List<TranscriptRecord> records) {
        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        // ✅ NULL SAFE: Validate records list
        if (records == null) {
            records = new ArrayList<>();
        }

        for (TranscriptRecord r : records) {
            // ✅ NULL SAFE: Skip null records
            if (r == null) continue;
            if (r.getIncludeInGpa() && r.getGradePoints() != null && r.getCreditsAttempted() != null) {
                totalPoints = totalPoints.add(r.getQualityPoints());
                totalCredits = totalCredits.add(r.getCreditsAttempted());
            }
        }

        BigDecimal yearGpa = totalCredits.compareTo(BigDecimal.ZERO) > 0 ?
            totalPoints.divide(totalCredits, 3, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return AcademicYearSummary.builder()
            .academicYear(year)
            .courses(records)
            .creditsAttempted(totalCredits)
            .creditsEarned(records.stream()
                .map(TranscriptRecord::getCreditsEarned)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .yearGpa(yearGpa)
            .build();
    }

    private BigDecimal getWeightFactor(TranscriptRecord.CourseType type) {
        // ✅ NULL SAFE: Handle null course type
        if (type == null) {
            return BigDecimal.ONE;
        }
        return switch (type) {
            case AP, IB -> new BigDecimal("1.2");
            case HONORS, DUAL_CREDIT -> new BigDecimal("1.1");
            default -> BigDecimal.ONE;
        };
    }

    private boolean isPassing(String letterGrade) {
        if (letterGrade == null) return false;
        return !letterGrade.equalsIgnoreCase("F") &&
               !letterGrade.equalsIgnoreCase("I") &&
               !letterGrade.equalsIgnoreCase("W");
    }

    private String calculateProjectedGradYear(Long studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null || student.getGradeLevel() == null) return "Unknown";

        try {
            int grade = Integer.parseInt(student.getGradeLevel());
            int yearsToGrad = 12 - grade;
            int gradYear = java.time.Year.now().getValue() + yearsToGrad;
            return String.valueOf(gradYear);
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }

    // DTOs

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentTranscript {
        private Long studentId;
        private String studentName;
        private String studentNumber;
        private String currentGradeLevel;
        private List<AcademicYearSummary> academicYears;
        private BigDecimal cumulativeGpa;
        private BigDecimal weightedGpa;
        private BigDecimal totalCreditsEarned;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademicYearSummary {
        private String academicYear;
        private List<TranscriptRecord> courses;
        private BigDecimal creditsAttempted;
        private BigDecimal creditsEarned;
        private BigDecimal yearGpa;
    }

    @Data
    @AllArgsConstructor
    private static class StudentGpaInfo {
        private Long studentId;
        private BigDecimal gpa;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassRankInfo {
        private Long studentId;
        private int rank;
        private int totalStudents;
        private String gradeLevel;
        private BigDecimal gpa;
        private BigDecimal percentile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraduationStatus {
        private Long studentId;
        private BigDecimal creditsEarned;
        private BigDecimal creditsRequired;
        private int coursesCompleted;
        private boolean meetsRequirements;
        private String projectedGraduationYear;
    }
}
