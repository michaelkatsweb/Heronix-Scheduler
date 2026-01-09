package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentCourseHistory;
import com.heronix.model.enums.CompletionStatus;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentCourseHistoryRepository;
import com.heronix.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for importing student course history from CSV files
 *
 * CSV Format:
 * StudentID,CourseCode,AcademicYear,Semester,Grade,Status,Credits,Notes
 * S001,ALG1,2022-2023,Fall,A,COMPLETED,1.0,
 * S001,ENG9,2022-2023,Fall,B+,COMPLETED,1.0,
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7E - November 21, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseHistoryImportService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudentCourseHistoryRepository historyRepository;

    /**
     * Import course history from CSV file
     *
     * @param file The CSV file to import
     * @param createdBy Who is importing (for audit trail)
     * @return Import result with statistics
     */
    @Transactional
    public ImportResult importFromCSV(File file, String createdBy) {
        log.info("Starting course history import from: {}", file.getName());

        ImportResult result = new ImportResult();
        result.setFileName(file.getName());

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            // Skip header row
            line = reader.readLine();
            lineNumber++;

            if (line == null) {
                result.addError("File is empty");
                return result;
            }

            // Validate header
            if (!isValidHeader(line)) {
                result.addError("Invalid header format. Expected: StudentID,CourseCode,AcademicYear,Semester,Grade,Status,Credits,Notes");
                return result;
            }

            // Process data rows
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    processHistoryRow(line, lineNumber, createdBy, result);
                } catch (Exception e) {
                    String error = String.format("Line %d: Error processing row - %s",
                                                lineNumber, e.getMessage());
                    result.addError(error);
                    log.error(error, e);
                }
            }

            result.setSuccess(result.getErrorCount() == 0);

            log.info("Course history import complete: {} imported, {} skipped, {} errors",
                     result.getImportedCount(), result.getSkippedCount(), result.getErrorCount());

        } catch (IOException e) {
            result.addError("Failed to read file: " + e.getMessage());
            log.error("Failed to read CSV file", e);
        }

        return result;
    }

    /**
     * Validate CSV header
     */
    private boolean isValidHeader(String header) {
        String normalized = header.trim().toLowerCase();
        return normalized.contains("studentid") &&
               normalized.contains("coursecode") &&
               normalized.contains("academicyear") &&
               normalized.contains("semester") &&
               normalized.contains("status");
    }

    /**
     * Process a single CSV row
     */
    private void processHistoryRow(String line, int lineNumber, String createdBy, ImportResult result) {
        // Parse CSV (handle commas in quoted fields)
        String[] fields = parseCSVLine(line);

        if (fields.length < 6) {
            result.addError(String.format("Line %d: Insufficient fields (expected at least 6, got %d)",
                                         lineNumber, fields.length));
            return;
        }

        String studentId = fields[0].trim();
        String courseCode = fields[1].trim();
        String academicYear = fields[2].trim();
        String semester = fields[3].trim();
        String grade = fields[4].trim();
        String statusStr = fields[5].trim();
        String creditsStr = fields.length > 6 ? fields[6].trim() : "";
        String notes = fields.length > 7 ? fields[7].trim() : "";

        // Validate required fields
        if (studentId.isEmpty() || courseCode.isEmpty() || academicYear.isEmpty() ||
            semester.isEmpty() || statusStr.isEmpty()) {
            result.addError(String.format("Line %d: Missing required fields", lineNumber));
            return;
        }

        // Find student
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            result.addWarning(String.format("Line %d: Student '%s' not found - skipping", lineNumber, studentId));
            result.incrementSkipped();
            return;
        }
        Student student = studentOpt.get();

        // Find course
        Optional<Course> courseOpt = courseRepository.findByCourseCode(courseCode);
        if (courseOpt.isEmpty()) {
            result.addWarning(String.format("Line %d: Course '%s' not found - skipping", lineNumber, courseCode));
            result.incrementSkipped();
            return;
        }
        Course course = courseOpt.get();

        // Parse completion status
        CompletionStatus status;
        try {
            status = CompletionStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            result.addError(String.format("Line %d: Invalid status '%s'. Valid values: %s",
                                         lineNumber, statusStr, getValidStatusValues()));
            return;
        }

        // Parse credits (optional)
        Double credits = null;
        if (!creditsStr.isEmpty()) {
            try {
                credits = Double.parseDouble(creditsStr);
                if (credits < 0) {
                    result.addWarning(String.format("Line %d: Negative credits %s - setting to 0",
                                                   lineNumber, creditsStr));
                    credits = 0.0;
                }
            } catch (NumberFormatException e) {
                result.addWarning(String.format("Line %d: Invalid credits '%s' - ignoring",
                                               lineNumber, creditsStr));
            }
        }

        // Check for duplicate
        if (historyRepository.existsDuplicate(student.getId(), course.getId(), academicYear, semester)) {
            result.addWarning(String.format("Line %d: Duplicate entry for %s - %s (%s %s) - skipping",
                                           lineNumber, studentId, courseCode, academicYear, semester));
            result.incrementSkipped();
            return;
        }

        // Create history entry
        StudentCourseHistory history = new StudentCourseHistory();
        history.setStudent(student);
        history.setCourse(course);
        history.setAcademicYear(academicYear);
        history.setSemester(semester);
        history.setGradeReceived(grade.isEmpty() ? null : grade);
        history.setCompletionStatus(status);
        history.setCreditsEarned(credits);
        history.setNotes(notes.isEmpty() ? null : notes);
        history.setCreatedBy(createdBy);

        // Set completion date if status is final
        if (status.isFinalStatus()) {
            history.setCompletionDate(LocalDate.now()); // Could parse from data if available
        }

        // Save
        historyRepository.save(history);
        result.incrementImported();

        log.debug("Imported history: {} - {} ({} {}): Grade {}, Status {}",
                 studentId, courseCode, academicYear, semester, grade, status);
    }

    /**
     * Parse CSV line handling quoted fields with commas
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * Get valid completion status values as string
     */
    private String getValidStatusValues() {
        StringBuilder sb = new StringBuilder();
        for (CompletionStatus status : CompletionStatus.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(status.name());
        }
        return sb.toString();
    }

    /**
     * Import result DTO
     */
    public static class ImportResult {
        private String fileName;
        private boolean success;
        private int importedCount = 0;
        private int skippedCount = 0;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public int getImportedCount() { return importedCount; }
        public void incrementImported() { this.importedCount++; }

        public int getSkippedCount() { return skippedCount; }
        public void incrementSkipped() { this.skippedCount++; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        public int getErrorCount() { return errors.size(); }

        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        public int getWarningCount() { return warnings.size(); }

        public String getSummary() {
            return String.format("Import Summary: %d imported, %d skipped, %d errors, %d warnings",
                               importedCount, skippedCount, errors.size(), warnings.size());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getSummary()).append("\n");

            if (!errors.isEmpty()) {
                sb.append("\nErrors:\n");
                errors.forEach(e -> sb.append("  - ").append(e).append("\n"));
            }

            if (!warnings.isEmpty()) {
                sb.append("\nWarnings:\n");
                warnings.forEach(w -> sb.append("  - ").append(w).append("\n"));
            }

            return sb.toString();
        }
    }
}
