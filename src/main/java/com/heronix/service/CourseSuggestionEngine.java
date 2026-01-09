package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.model.dto.CourseSuggestion;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentEnrollmentHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Intelligent course suggestion engine.
 *
 * Suggests courses for students based on:
 * - Grade level
 * - Completed courses (from history)
 * - Prerequisites
 * - Course progression (Math: Alg I → Geo → Alg II → Pre-Calc → Calc)
 *
 * @author Heronix Scheduler Team
 */
@Service
public class CourseSuggestionEngine {

    private static final Logger log = LoggerFactory.getLogger(CourseSuggestionEngine.class);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentEnrollmentHistoryRepository enrollmentHistoryRepository;

    /**
     * Suggest courses for a student at their current grade level.
     *
     * @param student the student
     * @return list of course suggestions with priorities
     */
    public List<CourseSuggestion> suggestCoursesForStudent(Student student) {
        int gradeLevel = Integer.parseInt(student.getGradeLevel());
        log.info("Generating course suggestions for student {} (Grade {})",
            student.getStudentId(), gradeLevel);

        List<CourseSuggestion> suggestions = new ArrayList<>();

        // Core required courses
        suggestions.addAll(suggestCoreCourses(student, gradeLevel));

        // Electives and continuing courses
        suggestions.addAll(suggestElectives(student, gradeLevel));

        log.info("Generated {} course suggestions for {}", suggestions.size(), student.getStudentId());
        return suggestions;
    }

    /**
     * Suggest core required courses (English, Math, Science, Social Studies).
     *
     * @param student the student
     * @param gradeLevel student's grade level
     * @return list of core course suggestions
     */
    private List<CourseSuggestion> suggestCoreCourses(Student student, int gradeLevel) {
        List<CourseSuggestion> suggestions = new ArrayList<>();

        // English (required for all grades)
        Course englishCourse = suggestEnglish(gradeLevel);
        if (englishCourse != null) {
            suggestions.add(new CourseSuggestion(
                englishCourse,
                CourseSuggestion.Priority.REQUIRED,
                "Required English course for grade " + gradeLevel
            ));
        }

        // Math (progression-based)
        Course mathCourse = suggestMath(student, gradeLevel);
        if (mathCourse != null) {
            suggestions.add(new CourseSuggestion(
                mathCourse,
                CourseSuggestion.Priority.REQUIRED,
                "Next course in math progression"
            ));
        }

        // Science (grade-appropriate)
        Course scienceCourse = suggestScience(gradeLevel);
        if (scienceCourse != null) {
            suggestions.add(new CourseSuggestion(
                scienceCourse,
                CourseSuggestion.Priority.REQUIRED,
                "Required science course"
            ));
        }

        // Social Studies (grade-appropriate)
        Course socialStudiesCourse = suggestSocialStudies(gradeLevel);
        if (socialStudiesCourse != null) {
            suggestions.add(new CourseSuggestion(
                socialStudiesCourse,
                CourseSuggestion.Priority.REQUIRED,
                "Required social studies course"
            ));
        }

        return suggestions;
    }

    /**
     * Suggest elective courses.
     *
     * @param student the student
     * @param gradeLevel student's grade level
     * @return list of elective suggestions
     */
    private List<CourseSuggestion> suggestElectives(Student student, int gradeLevel) {
        List<CourseSuggestion> suggestions = new ArrayList<>();

        // Get student's course history to find continuing electives
        List<Course> previousCourses = getPreviousYearCourses(student);

        // Suggest continuing foreign language
        Course foreignLanguage = suggestContinuingForeignLanguage(previousCourses, gradeLevel);
        if (foreignLanguage != null) {
            suggestions.add(new CourseSuggestion(
                foreignLanguage,
                CourseSuggestion.Priority.RECOMMENDED,
                "Continue foreign language study"
            ));
        }

        // Suggest PE/Health (required)
        Course pe = suggestPE(gradeLevel);
        if (pe != null) {
            suggestions.add(new CourseSuggestion(
                pe,
                CourseSuggestion.Priority.REQUIRED,
                "Physical education requirement"
            ));
        }

        // Suggest arts/music electives
        suggestions.addAll(suggestArtsElectives(previousCourses, gradeLevel));

        return suggestions;
    }

    /**
     * Suggest English course by grade level.
     */
    private Course suggestEnglish(int gradeLevel) {
        String courseCode = switch (gradeLevel) {
            case 9 -> "ENG-091";  // English 9
            case 10 -> "ENG-101"; // English 10
            case 11 -> "ENG-111"; // English 11
            case 12 -> "ENG-121"; // English 12
            default -> null;
        };
        return courseCode != null ? findCourseByCourseCode(courseCode) : null;
    }

    /**
     * Suggest Math course based on progression.
     */
    private Course suggestMath(Student student, int gradeLevel) {
        // Check what math they completed last year
        Course lastMath = getLastCompletedCourse(student, "Mathematics");

        if (lastMath == null) {
            // Start with Algebra I
            return findCourseByCourseCode("MATH-101");
        }

        // Progression: Algebra I → Geometry → Algebra II → Pre-Calc → AP Calc
        String lastCode = lastMath.getCourseCode();
        String nextCode = switch (lastCode) {
            case "MATH-101" -> "MATH-102"; // Algebra I → Geometry
            case "MATH-102" -> "MATH-103"; // Geometry → Algebra II
            case "MATH-103" -> "MATH-301"; // Algebra II → Pre-Calc
            case "MATH-301" -> "MATH-401"; // Pre-Calc → AP Calc
            default -> null; // Already at highest level
        };

        return nextCode != null ? findCourseByCourseCode(nextCode) : null;
    }

    /**
     * Suggest Science course by grade level.
     */
    private Course suggestScience(int gradeLevel) {
        String courseCode = switch (gradeLevel) {
            case 9 -> "SCI-101";  // Biology
            case 10 -> "SCI-102"; // Chemistry
            case 11 -> "SCI-103"; // Physics
            case 12 -> "SCI-401"; // AP Biology or advanced science
            default -> null;
        };
        return courseCode != null ? findCourseByCourseCode(courseCode) : null;
    }

    /**
     * Suggest Social Studies course by grade level.
     */
    private Course suggestSocialStudies(int gradeLevel) {
        String courseCode = switch (gradeLevel) {
            case 9 -> "SS-101";  // World History
            case 10 -> "SS-102"; // US History
            case 11 -> "SS-103"; // Government
            case 12 -> "SS-104"; // Economics
            default -> null;
        };
        return courseCode != null ? findCourseByCourseCode(courseCode) : null;
    }

    /**
     * Suggest continuing foreign language.
     */
    private Course suggestContinuingForeignLanguage(List<Course> previousCourses, int gradeLevel) {
        // Find if student took foreign language last year
        Course lastFL = previousCourses.stream()
            .filter(c -> "Foreign Languages".equals(c.getSubject()))
            .findFirst()
            .orElse(null);

        if (lastFL == null) {
            // Start with Spanish I or French I
            return findCourseByCourseCode("FL-101");
        }

        // Continue progression
        String lastCode = lastFL.getCourseCode();
        String nextCode = switch (lastCode) {
            case "FL-101" -> "FL-102"; // Spanish I → Spanish II
            case "FL-102" -> "FL-103"; // Spanish II → Spanish III
            case "FL-103" -> "FL-401"; // Spanish III → AP Spanish
            default -> null;
        };

        return nextCode != null ? findCourseByCourseCode(nextCode) : null;
    }

    /**
     * Suggest PE/Health course by grade level.
     */
    private Course suggestPE(int gradeLevel) {
        String courseCode = switch (gradeLevel) {
            case 9 -> "PE-091";  // PE 9th Grade
            case 10 -> "PE-201"; // Health
            case 11 -> "PE-301"; // Sports Medicine
            case 12 -> "PE-201"; // Health (if not taken)
            default -> null;
        };
        return courseCode != null ? findCourseByCourseCode(courseCode) : null;
    }

    /**
     * Suggest arts/music electives based on previous enrollment.
     */
    private List<CourseSuggestion> suggestArtsElectives(List<Course> previousCourses, int gradeLevel) {
        List<CourseSuggestion> suggestions = new ArrayList<>();

        // Find if student took arts courses last year
        List<Course> artsLast = previousCourses.stream()
            .filter(c -> "Arts".equals(c.getSubject()))
            .collect(Collectors.toList());

        if (!artsLast.isEmpty()) {
            // Suggest continuing their arts track
            for (Course artsCourse : artsLast) {
                suggestions.add(new CourseSuggestion(
                    artsCourse,
                    CourseSuggestion.Priority.RECOMMENDED,
                    "Continue from last year"
                ));
            }
        } else {
            // Suggest new arts courses
            Course band = findCourseByCourseCode("ART-301");
            if (band != null) {
                suggestions.add(new CourseSuggestion(
                    band,
                    CourseSuggestion.Priority.OPTIONAL,
                    "Arts elective option"
                ));
            }
        }

        return suggestions;
    }

    /**
     * Get courses student completed in previous year.
     */
    private List<Course> getPreviousYearCourses(Student student) {
        return enrollmentHistoryRepository.findByStudent(student).stream()
            .filter(h -> "COMPLETED".equals(h.getStatus()))
            .map(h -> h.getCourse())
            .collect(Collectors.toList());
    }

    /**
     * Get last completed course in a subject.
     */
    private Course getLastCompletedCourse(Student student, String subject) {
        return getPreviousYearCourses(student).stream()
            .filter(c -> subject.equals(c.getSubject()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find course by course code.
     */
    private Course findCourseByCourseCode(String courseCode) {
        return courseRepository.findByCourseCode(courseCode).orElse(null);
    }
}
