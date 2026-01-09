package com.heronix.service;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Grade Import Service
 * Location: src/main/java/com/eduscheduler/service/GradeImportService.java
 *
 * Handles importing student grades from external SIS platforms
 * (Skyward, PowerSchool, Infinite Campus, etc.) via CSV or JSON.
 *
 * Features:
 * - Flexible field mapping
 * - Auto-detection of SIS formats
 * - Smart data conversion
 * - Missing field handling
 * - Batch import with validation
 * - Detailed error reporting
 * - Rollback capability
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-20
 */
@Slf4j
@Service
@Transactional
public class GradeImportService {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    // Field mapping variations
    private static final Map<String, List<String>> FIELD_ALIASES = new HashMap<>();

    static {
        // Student ID variations
        FIELD_ALIASES.put("studentId", Arrays.asList(
            "student_id", "studentid", "student id", "studentnumber", "student_number",
            "id", "sid", "student num", "studentnum"
        ));

        // Course Code variations
        FIELD_ALIASES.put("courseCode", Arrays.asList(
            "course_code", "coursecode", "course code", "coursenumber", "course_number",
            "course", "course num", "coursenum"
        ));

        // Course Name variations
        FIELD_ALIASES.put("courseName", Arrays.asList(
            "course_name", "coursename", "course name", "coursetitle", "course_title",
            "title", "class", "class name", "classname"
        ));

        // Letter Grade variations
        FIELD_ALIASES.put("letterGrade", Arrays.asList(
            "letter_grade", "lettergrade", "letter grade", "grade", "storedgrade",
            "stored_grade", "finalgrade", "final_grade", "mark"
        ));

        // Numerical Grade variations
        FIELD_ALIASES.put("numericalGrade", Arrays.asList(
            "numerical_grade", "numericalgrade", "numerical grade", "numeric_grade",
            "percent", "percentage", "score", "numericgrade", "numeric grade"
        ));

        // Credits variations
        FIELD_ALIASES.put("credits", Arrays.asList(
            "credit", "credit_hours", "credithours", "credit hours", "units",
            "credit_earned", "earned_credit"
        ));

        // Term variations
        FIELD_ALIASES.put("term", Arrays.asList(
            "semester", "quarter", "period", "termname", "term_name", "term name",
            "marking_period", "grading_period"
        ));

        // Academic Year variations
        FIELD_ALIASES.put("academicYear", Arrays.asList(
            "academic_year", "academicyear", "academic year", "year", "schoolyear",
            "school_year", "school year"
        ));

        // Teacher variations
        FIELD_ALIASES.put("teacherName", Arrays.asList(
            "teacher_name", "teachername", "teacher name", "teacher", "instructor",
            "instructor_name", "staff", "staff_name"
        ));

        // Absences variations
        FIELD_ALIASES.put("absences", Arrays.asList(
            "absence", "abs", "absent", "absences", "days_absent"
        ));

        // Tardies variations
        FIELD_ALIASES.put("tardies", Arrays.asList(
            "tardy", "tardies", "late", "days_late", "lates"
        ));

        // Comments variations
        FIELD_ALIASES.put("comments", Arrays.asList(
            "comment", "notes", "note", "remarks", "teacher_comment", "teacher_comments"
        ));
    }

    // ========================================================================
    // CSV IMPORT
    // ========================================================================

    /**
     * Import grades from CSV file
     */
    public ImportResult importFromCSV(InputStream inputStream, Map<String, String> fieldMapping) {
        log.info("Starting CSV grade import");

        ImportResult result = new ImportResult();
        List<Map<String, String>> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            // Read header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.addError("Empty file");
                return result;
            }

            String[] headers = parseCSVLine(headerLine);
            log.debug("CSV headers: {}", Arrays.toString(headers));

            // Auto-detect field mapping if not provided
            if (fieldMapping == null || fieldMapping.isEmpty()) {
                fieldMapping = autoDetectFieldMapping(headers);
                log.info("Auto-detected field mapping: {}", fieldMapping);
            }

            // Read data rows
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;

                String[] values = parseCSVLine(line);
                if (values.length != headers.length) {
                    result.addWarning("Row " + rowNumber + ": Column count mismatch (expected " +
                        headers.length + ", got " + values.length + ")");
                    continue;
                }

                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    record.put(headers[i], values[i]);
                }
                records.add(record);
            }

            log.info("Read {} records from CSV", records.size());

        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            result.addError("Failed to read CSV file: " + e.getMessage());
            return result;
        }

        // Process records
        return processImportRecords(records, fieldMapping, result);
    }

    /**
     * Parse CSV line handling quoted fields
     */
    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    // ========================================================================
    // FIELD MAPPING
    // ========================================================================

    /**
     * Auto-detect field mapping from headers
     */
    public Map<String, String> autoDetectFieldMapping(String[] headers) {
        Map<String, String> mapping = new HashMap<>();

        for (String header : headers) {
            String normalized = header.toLowerCase().trim();

            // Check each field alias
            for (Map.Entry<String, List<String>> entry : FIELD_ALIASES.entrySet()) {
                String fieldName = entry.getKey();
                List<String> aliases = entry.getValue();

                if (aliases.contains(normalized) || normalized.equals(fieldName.toLowerCase())) {
                    mapping.put(header, fieldName);
                    log.debug("Mapped '{}' → '{}'", header, fieldName);
                    break;
                }
            }
        }

        return mapping;
    }

    /**
     * Detect SIS platform from headers
     */
    public String detectSISPlatform(String[] headers) {
        Set<String> headerSet = Arrays.stream(headers)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        // Skyward patterns
        if (headerSet.contains("student id") && headerSet.contains("course code")) {
            return "Skyward";
        }

        // PowerSchool patterns
        if (headerSet.contains("studentid") && headerSet.contains("coursenumber")) {
            return "PowerSchool";
        }

        // Infinite Campus patterns
        if (headerSet.contains("studentnumber") && headerSet.contains("course")) {
            return "Infinite Campus";
        }

        return "Unknown";
    }

    // ========================================================================
    // DATA PROCESSING
    // ========================================================================

    /**
     * Process import records with field mapping
     */
    private ImportResult processImportRecords(List<Map<String, String>> records,
                                               Map<String, String> fieldMapping,
                                               ImportResult result) {
        log.info("Processing {} import records", records.size());

        int successCount = 0;
        int errorCount = 0;

        for (int i = 0; i < records.size(); i++) {
            int rowNumber = i + 2; // +2 for header and 0-index
            Map<String, String> record = records.get(i);

            try {
                StudentGrade grade = mapRecordToGrade(record, fieldMapping);

                if (grade != null) {
                    gradeService.saveGrade(grade);
                    successCount++;
                    result.addSuccess("Row " + rowNumber + ": Grade saved for " +
                        grade.getStudentName());
                } else {
                    errorCount++;
                    result.addError("Row " + rowNumber + ": Failed to create grade from record");
                }

            } catch (Exception e) {
                errorCount++;
                log.error("Error processing row " + rowNumber, e);
                result.addError("Row " + rowNumber + ": " + e.getMessage());
            }
        }

        result.setSuccessCount(successCount);
        result.setErrorCount(errorCount);
        result.setTotalCount(records.size());

        log.info("Import complete: {} success, {} errors", successCount, errorCount);

        return result;
    }

    /**
     * Map CSV/JSON record to StudentGrade entity
     */
    private StudentGrade mapRecordToGrade(Map<String, String> record,
                                           Map<String, String> fieldMapping) {
        // Extract values using field mapping
        String studentId = getMappedValue(record, fieldMapping, "studentId");
        String courseCode = getMappedValue(record, fieldMapping, "courseCode");
        String courseName = getMappedValue(record, fieldMapping, "courseName");
        String letterGrade = getMappedValue(record, fieldMapping, "letterGrade");
        String numericalGradeStr = getMappedValue(record, fieldMapping, "numericalGrade");
        String creditsStr = getMappedValue(record, fieldMapping, "credits");
        String term = getMappedValue(record, fieldMapping, "term");
        String yearStr = getMappedValue(record, fieldMapping, "academicYear");
        String teacherName = getMappedValue(record, fieldMapping, "teacherName");
        String absencesStr = getMappedValue(record, fieldMapping, "absences");
        String tardiesStr = getMappedValue(record, fieldMapping, "tardies");
        String comments = getMappedValue(record, fieldMapping, "comments");

        // Validate required fields
        if (studentId == null || studentId.isEmpty()) {
            throw new IllegalArgumentException("Student ID is required");
        }
        if (courseCode == null || courseCode.isEmpty()) {
            throw new IllegalArgumentException("Course code is required");
        }
        if (letterGrade == null || letterGrade.isEmpty()) {
            if (numericalGradeStr != null && !numericalGradeStr.isEmpty()) {
                double numerical = Double.parseDouble(numericalGradeStr);
                letterGrade = StudentGrade.numericalGradeToLetterGrade(numerical);
            } else {
                throw new IllegalArgumentException("Letter grade or numerical grade is required");
            }
        }

        // Find or create student
        Student student = findOrCreateStudent(studentId);

        // Find or create course
        Course course = findOrCreateCourse(courseCode, courseName);

        // Find or create teacher
        Teacher teacher = null;
        if (teacherName != null && !teacherName.isEmpty()) {
            teacher = findOrCreateTeacher(teacherName);
        }

        // Create grade
        StudentGrade grade = new StudentGrade();
        grade.setStudent(student);
        grade.setCourse(course);
        grade.setLetterGrade(letterGrade.toUpperCase());

        // Set numerical grade
        if (numericalGradeStr != null && !numericalGradeStr.isEmpty()) {
            grade.setNumericalGrade(Double.parseDouble(numericalGradeStr));
        }

        // Set credits
        double credits = 1.0;
        if (creditsStr != null && !creditsStr.isEmpty()) {
            credits = Double.parseDouble(creditsStr);
        }
        grade.setCredits(credits);

        // Set term
        if (term != null && !term.isEmpty()) {
            grade.setTerm(normalizeTerm(term));
        } else {
            grade.setTerm(getCurrentTerm());
        }

        // Set academic year
        int year = LocalDate.now().getYear();
        if (yearStr != null && !yearStr.isEmpty()) {
            year = extractYear(yearStr);
        }
        grade.setAcademicYear(year);

        // Set grade type and final flag
        grade.setGradeType("Final");
        grade.setIsFinal(true);

        // Set grade date
        grade.setGradeDate(LocalDate.now());

        // Set teacher
        if (teacher != null) {
            grade.setTeacher(teacher);
        }

        // Set attendance
        if (absencesStr != null && !absencesStr.isEmpty()) {
            try {
                grade.setAbsences(Integer.parseInt(absencesStr));
            } catch (NumberFormatException e) {
                // Ignore invalid absences
            }
        }

        if (tardiesStr != null && !tardiesStr.isEmpty()) {
            try {
                grade.setTardies(Integer.parseInt(tardiesStr));
            } catch (NumberFormatException e) {
                // Ignore invalid tardies
            }
        }

        // Set comments
        if (comments != null && !comments.isEmpty()) {
            grade.setComments(comments);
        }

        // Calculate GPA points
        Double gpaPoints = StudentGrade.letterGradeToGpaPoints(grade.getLetterGrade());
        if (gpaPoints != null) {
            grade.setGpaPoints(gpaPoints);
        } else {
            grade.setGpaPoints(0.0);
            grade.setIncludeInGPA(false);
        }

        return grade;
    }

    /**
     * Get mapped value from record
     */
    private String getMappedValue(Map<String, String> record,
                                   Map<String, String> fieldMapping,
                                   String fieldName) {
        // Find the original column name that maps to this field
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            if (entry.getValue().equals(fieldName)) {
                return record.get(entry.getKey());
            }
        }
        return null;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Find or create student
     */
    private Student findOrCreateStudent(String studentId) {
        Optional<Student> existing = studentRepository.findByStudentId(studentId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new student
        log.warn("Student {} not found, creating placeholder", studentId);
        Student student = new Student();
        student.setStudentId(studentId);
        student.setFirstName("Import");
        student.setLastName("Student " + studentId);
        student.setGradeLevel("Unknown");
        student.setActive(true);
        return studentRepository.save(student);
    }

    /**
     * Find or create course
     */
    private Course findOrCreateCourse(String courseCode, String courseName) {
        Optional<Course> existing = courseRepository.findByCourseCode(courseCode);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new course
        log.warn("Course {} not found, creating placeholder", courseCode);
        Course course = new Course();
        course.setCourseCode(courseCode);
        course.setCourseName(courseName != null ? courseName : courseCode);
        course.setSubject("General");
        course.setActive(true);
        return courseRepository.save(course);
    }

    /**
     * Find or create teacher
     */
    private Teacher findOrCreateTeacher(String teacherName) {
        // Try to parse name (Last, First or First Last)
        String[] parts;
        String firstName, lastName;

        if (teacherName.contains(",")) {
            parts = teacherName.split(",", 2);
            lastName = parts[0].trim();
            firstName = parts.length > 1 ? parts[1].trim() : "";
        } else {
            parts = teacherName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        }

        // Try to find existing teacher
        List<Teacher> teachers = teacherRepository.findAllActive();
        for (Teacher t : teachers) {
            if (t.getFirstName().equalsIgnoreCase(firstName) &&
                t.getLastName().equalsIgnoreCase(lastName)) {
                return t;
            }
        }

        // Create new teacher
        log.warn("Teacher {} not found, creating placeholder", teacherName);
        Teacher teacher = new Teacher();
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@school.edu");
        teacher.setActive(true);
        return teacherRepository.save(teacher);
    }

    /**
     * Normalize term name
     */
    private String normalizeTerm(String term) {
        String lower = term.toLowerCase();

        // Semester codes
        if (lower.equals("s1") || lower.equals("1") || lower.contains("fall")) {
            return "Fall " + LocalDate.now().getYear();
        }
        if (lower.equals("s2") || lower.equals("2") || lower.contains("spring")) {
            return "Spring " + LocalDate.now().getYear();
        }

        // Quarter codes
        if (lower.equals("q1") || lower.contains("quarter 1")) {
            return "Q1 " + LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1);
        }
        if (lower.equals("q2") || lower.contains("quarter 2")) {
            return "Q2 " + LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1);
        }
        if (lower.equals("q3") || lower.contains("quarter 3")) {
            return "Q3 " + LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1);
        }
        if (lower.equals("q4") || lower.contains("quarter 4")) {
            return "Q4 " + LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1);
        }

        // Return as-is if already in good format
        return term;
    }

    /**
     * Extract year from various formats
     */
    private int extractYear(String yearStr) {
        // Try direct parse
        try {
            return Integer.parseInt(yearStr.trim());
        } catch (NumberFormatException e) {
            // Try to extract from format like "2024-2025"
            if (yearStr.contains("-")) {
                String[] parts = yearStr.split("-");
                try {
                    return Integer.parseInt(parts[0].trim());
                } catch (NumberFormatException e2) {
                    // Fallback to current year
                    return LocalDate.now().getYear();
                }
            }
            return LocalDate.now().getYear();
        }
    }

    /**
     * Get current term
     */
    private String getCurrentTerm() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();

        if (month >= 8 && month <= 12) {
            return "Fall " + now.getYear();
        } else if (month >= 1 && month <= 5) {
            return "Spring " + now.getYear();
        } else {
            return "Summer " + now.getYear();
        }
    }

    // ========================================================================
    // IMPORT RESULT
    // ========================================================================

    /**
     * Import result with detailed feedback
     */
    @Data
    public static class ImportResult {
        private int totalCount = 0;
        private int successCount = 0;
        private int errorCount = 0;
        private int warningCount = 0;

        private List<String> successMessages = new ArrayList<>();
        private List<String> errorMessages = new ArrayList<>();
        private List<String> warningMessages = new ArrayList<>();

        public void addSuccess(String message) {
            successMessages.add(message);
        }

        public void addError(String message) {
            errorMessages.add(message);
            errorCount++;
        }

        public void addWarning(String message) {
            warningMessages.add(message);
            warningCount++;
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean hasWarnings() {
            return warningCount > 0;
        }

        public String getSummary() {
            return String.format("Total: %d, Success: %d, Errors: %d, Warnings: %d",
                totalCount, successCount, errorCount, warningCount);
        }
    }
}
