package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.dto.ImportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Smart CSV Importer - Universal Import System
 * Location: src/main/java/com/eduscheduler/service/impl/SmartCSVImporter.java
 * 
 * Features:
 * - Automatic column detection
 * - Fuzzy matching for column names
 * - Handles any CSV format
 * - Smart data type detection
 * - Preview before import
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class SmartCSVImporter {

    // ========================================================================
    // COLUMN MAPPING DEFINITIONS
    // ========================================================================

    private static class ColumnMappings {
        // Student field mappings
        static final Map<String, List<String>> STUDENT_MAPPINGS = Map.of(
                "studentId", Arrays.asList(
                        "studentid", "student_id", "student id", "id", "sid",
                        "student_number", "studentnumber", "student number", "student #",
                        "student_no", "studentno", "stu_id", "stuid"),
                "firstName", Arrays.asList(
                        "firstname", "first_name", "first name", "fname",
                        "given_name", "givenname", "given name", "first",
                        "student_first_name", "student first name", "name_first"),
                "lastName", Arrays.asList(
                        "lastname", "last_name", "last name", "lname",
                        "surname", "family_name", "familyname", "family name",
                        "last", "student_last_name", "student last name", "name_last"),
                "gradeLevel", Arrays.asList(
                        "gradelevel", "grade_level", "grade level", "grade",
                        "level", "student_grade", "student grade", "current_grade",
                        "current grade", "gr", "grd", "year", "class"),
                "email", Arrays.asList(
                        "email", "e-mail", "email_address", "emailaddress",
                        "email address", "student_email", "student email",
                        "mail", "e_mail", "student_email_address"));

        // Teacher field mappings
        static final Map<String, List<String>> TEACHER_MAPPINGS = Map.of(
                "employeeId", Arrays.asList(
                        "teacherid", "teacher_id", "teacher id", "employeeid",
                        "employee_id", "employee id", "empid", "emp_id", "emp id",
                        "staff_id", "staffid", "staff id", "tid", "id"),
                "firstName", Arrays.asList(
                        "firstname", "first_name", "first name", "fname",
                        "given_name", "givenname", "teacher_first_name"),
                "lastName", Arrays.asList(
                        "lastname", "last_name", "last name", "lname",
                        "surname", "teacher_last_name"),
                "department", Arrays.asList(
                        "department", "dept", "dep", "division", "subject",
                        "subject_area", "teaching_area", "area"),
                "email", Arrays.asList(
                        "email", "e-mail", "email_address", "teacher_email",
                        "work_email", "professional_email"),
                "phone", Arrays.asList(
                        "phone", "phone_number", "phonenumber", "telephone",
                        "tel", "mobile", "cell", "contact", "phone number",
                        "work_phone", "office_phone"));

        // Room field mappings
        static final Map<String, List<String>> ROOM_MAPPINGS = Map.of(
                "roomNumber", Arrays.asList(
                        "roomnumber", "room_number", "room number", "room",
                        "room_no", "roomno", "room #", "classroom",
                        "classroom_number", "class_room", "room_id"),
                "building", Arrays.asList(
                        "building", "bldg", "building_name", "buildingname",
                        "location", "wing", "block", "facility"),
                "capacity", Arrays.asList(
                        "capacity", "max_capacity", "maxcapacity", "seats",
                        "max_students", "maxstudents", "size", "room_size",
                        "student_capacity", "seating"),
                "roomType", Arrays.asList(
                        "roomtype", "room_type", "type", "room type",
                        "classroom_type", "category", "room_category"));

        // Course field mappings
        static final Map<String, List<String>> COURSE_MAPPINGS = Map.of(
                "courseCode", Arrays.asList(
                        "coursecode", "course_code", "course code", "code",
                        "course_id", "courseid", "course id", "course_no",
                        "courseno", "course #", "subject_code"),
                "courseName", Arrays.asList(
                        "coursename", "course_name", "course name", "name",
                        "title", "course_title", "coursetitle", "subject",
                        "subject_name", "class_name", "classname"),
                "maxStudents", Arrays.asList(
                        "maxstudents", "max_students", "max students", "capacity",
                        "enrollment_limit", "enrollmentlimit", "class_size",
                        "classsize", "max_enrollment", "maxenrollment"),
                "duration", Arrays.asList(
                        "duration", "length", "minutes", "duration_minutes",
                        "class_length", "period_length", "time", "hours"));
    }

    // ========================================================================
    // MAIN IMPORT METHOD
    // ========================================================================

    public ImportResult importCSV(InputStream inputStream, String entityType) {
        ImportResult result = new ImportResult(0, 0, 0);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            // Read and analyze headers
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty or has no headers");
            }

            // Parse headers
            List<String> headers = parseCSVLine(headerLine);
            log.info("Detected {} columns: {}", headers.size(), headers);

            // Auto-detect entity type if not specified
            if (entityType == null || entityType.isEmpty()) {
                entityType = detectEntityType(headers);
                log.info("Auto-detected entity type: {}", entityType);
            }

            // Map columns to entity fields
            Map<String, Integer> fieldMapping = mapColumnsToFields(headers, entityType);
            log.info("Field mappings: {}", fieldMapping);

            // Import data based on entity type
            switch (entityType.toLowerCase()) {
                case "student":
                    importStudents(reader, headers, fieldMapping, result);
                    break;
                case "teacher":
                    importTeachers(reader, headers, fieldMapping, result);
                    break;
                case "course":
                    importCourses(reader, headers, fieldMapping, result);
                    break;
                case "room":
                    importRooms(reader, headers, fieldMapping, result);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown entity type: " + entityType);
            }

        } catch (Exception e) {
            log.error("Import failed: ", e);
            result.addError("Import failed: " + e.getMessage());
        }

        return result;
    }

    // ========================================================================
    // ENTITY TYPE DETECTION
    // ========================================================================

    private String detectEntityType(List<String> headers) {
        // Convert headers to lowercase for comparison
        List<String> lowerHeaders = headers.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        // Check for student indicators
        if (containsAny(lowerHeaders, "studentid", "student_id", "gradelevel", "grade_level",
                "homeroom", "guardian", "gpa")) {
            return "student";
        }

        // Check for teacher indicators
        if (containsAny(lowerHeaders, "teacherid", "teacher_id", "employeeid", "employee_id",
                "department", "certifications", "hiredate")) {
            return "teacher";
        }

        // Check for course indicators
        if (containsAny(lowerHeaders, "coursecode", "course_code", "coursename", "course_name",
                "credits", "prerequisite", "subject")) {
            return "course";
        }

        // Check for room indicators
        if (containsAny(lowerHeaders, "roomnumber", "room_number", "building", "capacity",
                "roomtype", "room_type")) {
            return "room";
        }

        // Default to student if unclear
        return "student";
    }

    private boolean containsAny(List<String> list, String... values) {
        for (String value : values) {
            if (list.contains(value)) {
                return true;
            }
        }
        return false;
    }

    // ========================================================================
    // COLUMN MAPPING
    // ========================================================================

    private Map<String, Integer> mapColumnsToFields(List<String> headers, String entityType) {
        Map<String, Integer> fieldMapping = new HashMap<>();
        Map<String, List<String>> mappings;

        // Select appropriate mappings based on entity type
        switch (entityType.toLowerCase()) {
            case "student":
                mappings = ColumnMappings.STUDENT_MAPPINGS;
                break;
            case "teacher":
                mappings = ColumnMappings.TEACHER_MAPPINGS;
                break;
            case "course":
                mappings = ColumnMappings.COURSE_MAPPINGS;
                break;
            case "room":
                mappings = ColumnMappings.ROOM_MAPPINGS;
                break;
            default:
                mappings = new HashMap<>();
        }

        // Map each field to the best matching column
        for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
            String field = entry.getKey();
            List<String> variations = entry.getValue();

            // Find best matching column
            Integer columnIndex = findBestMatch(headers, variations);
            if (columnIndex != null) {
                fieldMapping.put(field, columnIndex);
            }
        }

        return fieldMapping;
    }

    private Integer findBestMatch(List<String> headers, List<String> variations) {
        // First try exact match (case-insensitive)
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toLowerCase().trim();
            for (String variation : variations) {
                if (header.equals(variation.toLowerCase())) {
                    return i;
                }
            }
        }

        // Try fuzzy matching (contains)
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toLowerCase().trim();
            for (String variation : variations) {
                if (header.contains(variation.toLowerCase()) ||
                        variation.toLowerCase().contains(header)) {
                    return i;
                }
            }
        }

        return null;
    }

    // ========================================================================
    // IMPORT METHODS FOR EACH ENTITY
    // ========================================================================

    private void importStudents(BufferedReader reader, List<String> headers,
            Map<String, Integer> fieldMapping, ImportResult result)
            throws IOException {
        String line;
        int lineNumber = 1;
        List<Student> batch = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty())
                continue;

            try {
                List<String> values = parseCSVLine(line);
                Student student = new Student();

                // Map fields
                student.setStudentId(getValue(values, fieldMapping.get("studentId"),
                        "S" + System.currentTimeMillis() + lineNumber));
                student.setFirstName(getValue(values, fieldMapping.get("firstName"), "Unknown"));
                student.setLastName(getValue(values, fieldMapping.get("lastName"), "Unknown"));
                student.setGradeLevel(getValue(values, fieldMapping.get("gradeLevel"), "Unknown"));
                student.setEmail(getValue(values, fieldMapping.get("email"), null));
                student.setActive(true);

                batch.add(student);

                // Save batch
                if (batch.size() >= 100) {
                    // Note: In real implementation, call repository.saveAll(batch)
                    result.addSuccess(batch.size());
                    batch.clear();
                }

            } catch (Exception e) {
                result.addError("Line " + lineNumber + ": " + e.getMessage());
            }
        }

        // Save remaining
        if (!batch.isEmpty()) {
            // Note: In real implementation, call repository.saveAll(batch)
            result.addSuccess(batch.size());
        }
    }

    private void importTeachers(BufferedReader reader, List<String> headers,
            Map<String, Integer> fieldMapping, ImportResult result)
            throws IOException {
        String line;
        int lineNumber = 1;
        List<Teacher> batch = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty())
                continue;

            try {
                List<String> values = parseCSVLine(line);
                Teacher teacher = new Teacher();

                // Get names
                String firstName = getValue(values, fieldMapping.get("firstName"), "");
                String lastName = getValue(values, fieldMapping.get("lastName"), "");

                // Combine for full name or use single name field
                if (!firstName.isEmpty() || !lastName.isEmpty()) {
                    teacher.setName((firstName + " " + lastName).trim());
                } else {
                    teacher.setName("Unknown Teacher");
                }

                teacher.setEmployeeId(getValue(values, fieldMapping.get("employeeId"),
                        "T" + System.currentTimeMillis() + lineNumber));
                teacher.setDepartment(getValue(values, fieldMapping.get("department"), "General"));
                teacher.setEmail(getValue(values, fieldMapping.get("email"), null));
                teacher.setPhoneNumber(getValue(values, fieldMapping.get("phone"), null));
                teacher.setActive(true);
                teacher.setMaxHoursPerWeek(40);
                teacher.setMaxConsecutiveHours(4);

                batch.add(teacher);

                if (batch.size() >= 100) {
                    result.addSuccess(batch.size());
                    batch.clear();
                }

            } catch (Exception e) {
                result.addError("Line " + lineNumber + ": " + e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            result.addSuccess(batch.size());
        }
    }

    private void importCourses(BufferedReader reader, List<String> headers,
            Map<String, Integer> fieldMapping, ImportResult result)
            throws IOException {
        // Similar implementation for courses
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty())
                continue;

            try {
                List<String> values = parseCSVLine(line);
                Course course = new Course();

                course.setCourseCode(getValue(values, fieldMapping.get("courseCode"),
                        "C" + lineNumber));
                course.setCourseName(getValue(values, fieldMapping.get("courseName"),
                        "Course " + lineNumber));

                String maxStudentsStr = getValue(values, fieldMapping.get("maxStudents"), "30");
                course.setMaxStudents(parseInteger(maxStudentsStr, 30));

                String durationStr = getValue(values, fieldMapping.get("duration"), "50");
                course.setDurationMinutes(parseInteger(durationStr, 50));

                course.setActive(true);
                result.addSuccess(1);

            } catch (Exception e) {
                result.addError("Line " + lineNumber + ": " + e.getMessage());
            }
        }
    }

    private void importRooms(BufferedReader reader, List<String> headers,
            Map<String, Integer> fieldMapping, ImportResult result)
            throws IOException {
        // Similar implementation for rooms
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty())
                continue;

            try {
                List<String> values = parseCSVLine(line);
                Room room = new Room();

                room.setRoomNumber(getValue(values, fieldMapping.get("roomNumber"),
                        "Room" + lineNumber));
                room.setBuilding(getValue(values, fieldMapping.get("building"), "Main"));

                String capacityStr = getValue(values, fieldMapping.get("capacity"), "30");
                room.setCapacity(parseInteger(capacityStr, 30));

                result.addSuccess(1);

            } catch (Exception e) {
                result.addError("Line " + lineNumber + ": " + e.getMessage());
            }
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private List<String> parseCSVLine(String line) {
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

        return values;
    }

    private String getValue(List<String> values, Integer index, String defaultValue) {
        if (index != null && index < values.size()) {
            String value = values.get(index);
            if (value != null && !value.isEmpty() && !value.equalsIgnoreCase("null")) {
                return value.trim();
            }
        }
        return defaultValue;
    }

    private int parseInteger(String value, int defaultValue) {
        try {
            // Remove non-numeric characters (keep digits only)
            String cleaned = value.replaceAll("[^0-9]", "");
            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ========================================================================
    // PREVIEW METHOD - Shows user what will be imported
    // ========================================================================

    public Map<String, Object> previewImport(InputStream inputStream, String entityType) {
        Map<String, Object> preview = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String headerLine = reader.readLine();
            List<String> headers = parseCSVLine(headerLine);

            // Auto-detect if needed
            if (entityType == null) {
                entityType = detectEntityType(headers);
            }

            Map<String, Integer> fieldMapping = mapColumnsToFields(headers, entityType);

            // Read first 5 rows as preview
            List<Map<String, String>> sampleData = new ArrayList<>();
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null && count < 5) {
                List<String> values = parseCSVLine(line);
                Map<String, String> row = new HashMap<>();

                for (Map.Entry<String, Integer> entry : fieldMapping.entrySet()) {
                    row.put(entry.getKey(), getValue(values, entry.getValue(), ""));
                }
                sampleData.add(row);
                count++;
            }

            preview.put("entityType", entityType);
            preview.put("headers", headers);
            preview.put("mappings", fieldMapping);
            preview.put("sampleData", sampleData);

        } catch (Exception e) {
            preview.put("error", e.getMessage());
        }

        return preview;
    }
}