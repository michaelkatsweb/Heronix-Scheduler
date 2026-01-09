package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gradebook Service
 *
 * Handles all gradebook operations including:
 * - Weighted grade calculations
 * - Category management
 * - Assignment management
 * - Grade entry and updates
 * - Report generation
 *
 * Grade Calculation:
 * 1. Calculate category averages (with drop lowest option)
 * 2. Apply category weights
 * 3. Sum weighted averages for final grade
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-22
 */
@Slf4j
@Service
@Transactional
public class GradebookService {

    @Autowired
    private GradingCategoryRepository categoryRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentGradeRepository gradeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    // ========================================================================
    // CATEGORY MANAGEMENT
    // ========================================================================

    /**
     * Create default grading categories for a course
     */
    public List<GradingCategory> createDefaultCategories(Long courseId) {
        log.info("Creating default grading categories for course {}", courseId);

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        List<GradingCategory> categories = new ArrayList<>();

        // Standard K-12 category setup
        categories.add(GradingCategory.builder()
            .course(course)
            .name("Tests")
            .categoryType(GradingCategory.CategoryType.TEST)
            .weight(30.0)
            .displayOrder(1)
            .color("#F44336")
            .build());

        categories.add(GradingCategory.builder()
            .course(course)
            .name("Quizzes")
            .categoryType(GradingCategory.CategoryType.QUIZ)
            .weight(20.0)
            .dropLowest(1) // Drop lowest quiz
            .displayOrder(2)
            .color("#FF9800")
            .build());

        categories.add(GradingCategory.builder()
            .course(course)
            .name("Homework")
            .categoryType(GradingCategory.CategoryType.HOMEWORK)
            .weight(20.0)
            .displayOrder(3)
            .color("#4CAF50")
            .build());

        categories.add(GradingCategory.builder()
            .course(course)
            .name("Projects")
            .categoryType(GradingCategory.CategoryType.PROJECT)
            .weight(20.0)
            .displayOrder(4)
            .color("#9C27B0")
            .build());

        categories.add(GradingCategory.builder()
            .course(course)
            .name("Participation")
            .categoryType(GradingCategory.CategoryType.PARTICIPATION)
            .weight(10.0)
            .displayOrder(5)
            .color("#03A9F4")
            .build());

        return categoryRepository.saveAll(categories);
    }

    /**
     * Get categories for a course
     */
    public List<GradingCategory> getCategoriesForCourse(Long courseId) {
        return categoryRepository.findByCourseIdAndActiveTrueOrderByDisplayOrder(courseId);
    }

    /**
     * Update category weights (ensures they sum to 100%)
     */
    public void updateCategoryWeights(Map<Long, Double> categoryWeights) {
        double total = categoryWeights.values().stream().mapToDouble(Double::doubleValue).sum();

        if (Math.abs(total - 100.0) > 0.01) {
            throw new IllegalArgumentException("Category weights must sum to 100%. Current sum: " + total);
        }

        for (Map.Entry<Long, Double> entry : categoryWeights.entrySet()) {
            GradingCategory category = categoryRepository.findById(entry.getKey())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + entry.getKey()));
            category.setWeight(entry.getValue());
            categoryRepository.save(category);
        }

        log.info("Updated {} category weights", categoryWeights.size());
    }

    // ========================================================================
    // ASSIGNMENT MANAGEMENT
    // ========================================================================

    /**
     * Create a new assignment
     */
    public Assignment createAssignment(Long courseId, Long categoryId, String title,
                                        Double maxPoints, LocalDate dueDate) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        GradingCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        Assignment assignment = Assignment.builder()
            .course(course)
            .category(category)
            .title(title)
            .maxPoints(maxPoints)
            .dueDate(dueDate)
            .assignedDate(LocalDate.now())
            .published(false)
            .build();

        assignment = assignmentRepository.save(assignment);
        log.info("Created assignment '{}' for course {}", title, courseId);

        return assignment;
    }

    /**
     * Get assignments for a course
     */
    public List<Assignment> getAssignmentsForCourse(Long courseId) {
        return assignmentRepository.findByCourseIdOrderByDueDateDesc(courseId);
    }

    /**
     * Get assignments by category
     */
    public List<Assignment> getAssignmentsByCategory(Long categoryId) {
        return assignmentRepository.findByCategoryIdOrderByDueDateDesc(categoryId);
    }

    /**
     * Publish an assignment (make visible to students)
     */
    public void publishAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.setPublished(true);
        assignmentRepository.save(assignment);

        log.info("Published assignment '{}'", assignment.getTitle());
    }

    // ========================================================================
    // GRADE ENTRY
    // ========================================================================

    /**
     * Enter or update a grade for a student on an assignment
     */
    public AssignmentGrade enterGrade(Long studentId, Long assignmentId, Double score,
                                       LocalDate submittedDate, String comments) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        // Find existing or create new
        AssignmentGrade grade = gradeRepository.findByStudentIdAndAssignmentId(studentId, assignmentId)
            .orElse(AssignmentGrade.builder()
                .student(student)
                .assignment(assignment)
                .build());

        grade.setScore(score);
        grade.setSubmittedDate(submittedDate != null ? submittedDate : LocalDate.now());
        grade.setComments(comments);
        grade.setGradedDate(LocalDate.now());
        grade.setStatus(AssignmentGrade.GradeStatus.GRADED);

        // Calculate late penalty if applicable
        if (submittedDate != null && assignment.getDueDate() != null) {
            double penalty = assignment.calculateLatePenalty(submittedDate);
            grade.setLatePenalty(penalty);
            if (penalty > 0) {
                grade.setStatus(AssignmentGrade.GradeStatus.LATE);
            }
        }

        grade = gradeRepository.save(grade);
        log.info("Entered grade {} for student {} on assignment {}",
            score, student.getStudentId(), assignment.getTitle());

        return grade;
    }

    /**
     * Bulk enter grades for an assignment
     */
    public int bulkEnterGrades(Long assignmentId, Map<Long, Double> studentScores) {
        int count = 0;
        for (Map.Entry<Long, Double> entry : studentScores.entrySet()) {
            try {
                enterGrade(entry.getKey(), assignmentId, entry.getValue(), LocalDate.now(), null);
                count++;
            } catch (Exception e) {
                log.error("Failed to enter grade for student {}: {}", entry.getKey(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * Mark a grade as excused
     */
    public void excuseGrade(Long studentId, Long assignmentId, String reason) {
        AssignmentGrade grade = gradeRepository.findByStudentIdAndAssignmentId(studentId, assignmentId)
            .orElseGet(() -> {
                Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
                Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

                return AssignmentGrade.builder()
                    .student(student)
                    .assignment(assignment)
                    .build();
            });

        grade.markExcused(reason);
        gradeRepository.save(grade);

        log.info("Excused grade for student {} on assignment {}", studentId, assignmentId);
    }

    // ========================================================================
    // GRADE CALCULATIONS
    // ========================================================================

    /**
     * Calculate weighted course grade for a student
     */
    public StudentCourseGrade calculateCourseGrade(Long studentId, Long courseId) {
        List<GradingCategory> categories = getCategoriesForCourse(courseId);
        List<AssignmentGrade> grades = gradeRepository.findForGradebookCalculation(studentId, courseId);

        if (categories.isEmpty()) {
            return StudentCourseGrade.builder()
                .studentId(studentId)
                .courseId(courseId)
                .finalPercentage(0.0)
                .letterGrade("-")
                .build();
        }

        // Group grades by category
        Map<Long, List<AssignmentGrade>> gradesByCategory = grades.stream()
            .filter(AssignmentGrade::countsInGrade)
            .collect(Collectors.groupingBy(g -> g.getAssignment().getCategory().getId()));

        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        List<CategoryGrade> categoryGrades = new ArrayList<>();

        for (GradingCategory category : categories) {
            List<AssignmentGrade> categoryGradeList = gradesByCategory.getOrDefault(category.getId(), List.of());

            if (categoryGradeList.isEmpty()) {
                continue; // Skip categories with no grades
            }

            // Calculate category average with drop lowest
            double categoryAvg = calculateCategoryAverage(categoryGradeList, category.getDropLowest());

            CategoryGrade cg = CategoryGrade.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .weight(category.getWeight())
                .average(categoryAvg)
                .assignmentCount(categoryGradeList.size())
                .build();
            categoryGrades.add(cg);

            totalWeightedScore += (categoryAvg * category.getWeight() / 100.0);
            totalWeight += category.getWeight();
        }

        // Normalize if not all categories have grades
        double finalPercentage = totalWeight > 0 ? (totalWeightedScore / totalWeight) * 100.0 : 0.0;

        return StudentCourseGrade.builder()
            .studentId(studentId)
            .courseId(courseId)
            .categoryGrades(categoryGrades)
            .finalPercentage(finalPercentage)
            .letterGrade(percentageToLetterGrade(finalPercentage))
            .gpaPoints(percentageToGpaPoints(finalPercentage))
            .totalAssignments(grades.size())
            .gradedAssignments((int) grades.stream().filter(g -> g.getScore() != null).count())
            .missingAssignments((int) grades.stream().filter(g -> g.getStatus() == AssignmentGrade.GradeStatus.MISSING).count())
            .build();
    }

    /**
     * Calculate category average with drop lowest option
     */
    private double calculateCategoryAverage(List<AssignmentGrade> grades, int dropLowest) {
        if (grades.isEmpty()) return 0.0;

        // Get percentages (score / max * 100)
        List<Double> percentages = grades.stream()
            .filter(g -> g.getAdjustedScore() != null)
            .map(g -> {
                double max = g.getAssignment().getMaxPoints();
                return max > 0 ? (g.getAdjustedScore() / max) * 100.0 : 0.0;
            })
            .sorted() // Sort ascending for drop lowest
            .collect(Collectors.toList());

        if (percentages.isEmpty()) return 0.0;

        // Drop lowest N scores
        int toDrop = Math.min(dropLowest, Math.max(0, percentages.size() - 1)); // Keep at least 1
        List<Double> remaining = percentages.subList(toDrop, percentages.size());

        return remaining.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * Get class gradebook summary (all students, all assignments)
     */
    public ClassGradebook getClassGradebook(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        List<Assignment> assignments = assignmentRepository.findByCourseIdWithGrades(courseId);
        List<GradingCategory> categories = getCategoriesForCourse(courseId);

        // Get enrolled students
        List<Student> students = course.getStudents() != null
            ? new ArrayList<>(course.getStudents())
            : List.of();

        // Calculate grade for each student
        List<StudentCourseGrade> studentGrades = students.stream()
            .map(s -> calculateCourseGrade(s.getId(), courseId))
            .collect(Collectors.toList());

        // Calculate class statistics
        DoubleSummaryStatistics stats = studentGrades.stream()
            .mapToDouble(StudentCourseGrade::getFinalPercentage)
            .summaryStatistics();

        return ClassGradebook.builder()
            .courseId(courseId)
            .courseName(course.getCourseName())
            .categories(categories)
            .assignments(assignments)
            .studentGrades(studentGrades)
            .classAverage(stats.getAverage())
            .classHigh(stats.getMax())
            .classLow(stats.getMin())
            .studentCount((int) stats.getCount())
            .build();
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private String percentageToLetterGrade(double percentage) {
        if (percentage >= 97) return "A+";
        if (percentage >= 93) return "A";
        if (percentage >= 90) return "A-";
        if (percentage >= 87) return "B+";
        if (percentage >= 83) return "B";
        if (percentage >= 80) return "B-";
        if (percentage >= 77) return "C+";
        if (percentage >= 73) return "C";
        if (percentage >= 70) return "C-";
        if (percentage >= 67) return "D+";
        if (percentage >= 63) return "D";
        if (percentage >= 60) return "D-";
        return "F";
    }

    private double percentageToGpaPoints(double percentage) {
        if (percentage >= 93) return 4.0;
        if (percentage >= 90) return 3.7;
        if (percentage >= 87) return 3.3;
        if (percentage >= 83) return 3.0;
        if (percentage >= 80) return 2.7;
        if (percentage >= 77) return 2.3;
        if (percentage >= 73) return 2.0;
        if (percentage >= 70) return 1.7;
        if (percentage >= 67) return 1.3;
        if (percentage >= 63) return 1.0;
        if (percentage >= 60) return 0.7;
        return 0.0;
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    @Builder
    public static class StudentCourseGrade {
        private Long studentId;
        private Long courseId;
        private List<CategoryGrade> categoryGrades;
        private Double finalPercentage;
        private String letterGrade;
        private Double gpaPoints;
        private int totalAssignments;
        private int gradedAssignments;
        private int missingAssignments;
    }

    @Data
    @Builder
    public static class CategoryGrade {
        private Long categoryId;
        private String categoryName;
        private Double weight;
        private Double average;
        private int assignmentCount;
    }

    @Data
    @Builder
    public static class ClassGradebook {
        private Long courseId;
        private String courseName;
        private List<GradingCategory> categories;
        private List<Assignment> assignments;
        private List<StudentCourseGrade> studentGrades;
        private Double classAverage;
        private Double classHigh;
        private Double classLow;
        private int studentCount;
    }
}
