package com.heronix.service.impl;

import com.heronix.exception.ImportException;
import com.heronix.model.domain.*;
import com.heronix.model.dto.ImportResult;
import com.heronix.repository.*;
import com.heronix.service.LenientImportService;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LENIENT Import Service Implementation - FIXED VERSION
 * Location:
 * src/main/java/com/eduscheduler/service/impl/LenientImportServiceImpl.java
 * 
 * ✅ FIXED: Smart column mapping handles ALL naming conventions
 * ✅ FIXED: Correct DataIssue constructor usage (no incomplete list)
 * ✅ FIXED: Repository methods use findBy().isPresent() pattern
 * ✅ FIXED: Warnings track incomplete records
 * 
 * @author Heronix Scheduling System Team
 * @version 4.1.0 - COMPILATION FIXES
 * @since 2025-10-18
 */
@Slf4j
@Service("lenientImportService")
public class LenientImportServiceImpl implements LenientImportService {

    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Lazy
    @Autowired
    private LenientImportServiceImpl self;

    @Autowired
    public LenientImportServiceImpl(
            RoomRepository roomRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository) {
        this.roomRepository = roomRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    private static final int DEFAULT_ROOM_CAPACITY = 30;
    private static final String DEFAULT_BUILDING = "Main Building";
    private static final int DEFAULT_FLOOR = 1;
    private static final String DEFAULT_DEPARTMENT = "General";
    private static final String DEFAULT_GRADE_LEVEL = "9";
    private static final int DEFAULT_MAX_STUDENTS = 30;

    @Override
    public ImportResult importRoomsLenient(MultipartFile file) throws ImportException {
        // ✅ NULL SAFE: Validate file parameter
        if (file == null) {
            throw new ImportException("Cannot import from null file");
        }

        // ✅ NULL SAFE: Safe extraction of filename for logging
        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "Unknown";
        log.info("🏫 LENIENT importing rooms from: {}", filename);
        return importFileLenient(file, "room");
    }

    @Override
    public ImportResult importTeachersLenient(MultipartFile file) throws ImportException {
        // ✅ NULL SAFE: Validate file parameter
        if (file == null) {
            throw new ImportException("Cannot import from null file");
        }

        // ✅ NULL SAFE: Safe extraction of filename for logging
        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "Unknown";
        log.info("📚 LENIENT importing teachers from: {}", filename);
        return importFileLenient(file, "teacher");
    }

    @Override
    public ImportResult importStudentsLenient(MultipartFile file) throws ImportException {
        // ✅ NULL SAFE: Validate file parameter
        if (file == null) {
            throw new ImportException("Cannot import from null file");
        }

        // ✅ NULL SAFE: Safe extraction of filename for logging
        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "Unknown";
        log.info("🎓 LENIENT importing students from: {}", filename);
        return importFileLenient(file, "student");
    }

    @Override
    public ImportResult importCoursesLenient(MultipartFile file) throws ImportException {
        // ✅ NULL SAFE: Validate file parameter
        if (file == null) {
            throw new ImportException("Cannot import from null file");
        }

        // ✅ NULL SAFE: Safe extraction of filename for logging
        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "Unknown";
        log.info("📖 LENIENT importing courses from: {}", filename);
        return importFileLenient(file, "course");
    }

    private ImportResult importFileLenient(MultipartFile file, String entityType)
            throws ImportException {
        // ✅ NULL SAFE: Validate file parameter
        if (file == null) {
            throw new ImportException("Cannot import from null file");
        }

        try {
            // ✅ NULL SAFE: Safe extraction of filename with default
            String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "Unknown";
            String extension = getFileExtension(filename);

            log.info("🔧 LENIENT processing: {} (type: {}, entity: {})",
                    filename, extension, entityType);

            // ✅ NULL SAFE: Check extension is not empty before switch
            if (extension == null || extension.isEmpty()) {
                throw new ImportException("File has no extension: " + filename);
            }

            return switch (extension.toLowerCase()) {
                case "csv" -> importCSVLenient(file.getInputStream(), entityType);
                case "xlsx", "xls" -> importExcelLenient(file.getInputStream(), entityType);
                default -> throw new ImportException("Unsupported file type: " + extension);
            };
        } catch (IOException e) {
            throw new ImportException("Failed to read file: " + e.getMessage(), e);
        }
    }

    private ImportResult importCSVLenient(InputStream inputStream, String entityType)
            throws ImportException {

        ImportResult result = new ImportResult("CSV Lenient Import");
        result.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ImportException("CSV file is empty");
            }

            // ✅ NULL SAFE: Check headers are not null before using
            String[] headers = parseCSVLine(headerLine);
            if (headers == null || headers.length == 0) {
                throw new ImportException("CSV file has no headers");
            }

            Map<String, Integer> columnMap = buildColumnMap(headers);

            log.info("📊 Found {} columns with {} mapped variations", headers.length, columnMap.size());

            if (entityType == null || entityType.isEmpty()) {
                entityType = detectEntityType(headers);
            }

            log.info("🔍 Detected entity type: {}", entityType);

            int autoGeneratedCount = 1;
            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty())
                    continue;

                // ✅ NULL SAFE: Check values array is not null before parsing
                String[] values = parseCSVLine(line);
                if (values == null) continue;

                try {
                    switch (entityType.toLowerCase()) {
                        case "student" -> {
                            StudentImportResult studentResult = parseStudentFromCSV(
                                    values, columnMap, rowNumber, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (studentResult != null && studentResult.student() != null) {
                                StudentSaveResult saveResult = self.saveStudentInNewTransaction(studentResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + rowNumber + " - " + saveResult.name() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + rowNumber + ": " + saveResult.error());
                                }
                            }
                        }
                        case "teacher" -> {
                            TeacherImportResult teacherResult = parseTeacherFromCSV(
                                    values, columnMap, rowNumber, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (teacherResult != null && teacherResult.teacher() != null) {
                                TeacherSaveResult saveResult = self.saveTeacherInNewTransaction(teacherResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + rowNumber + " - " + saveResult.name() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + rowNumber + ": " + saveResult.error());
                                }
                            }
                        }
                        case "course" -> {
                            CourseImportResult courseResult = parseCourseFromCSV(
                                    values, columnMap, rowNumber, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (courseResult != null && courseResult.course() != null) {
                                CourseSaveResult saveResult = self.saveCourseInNewTransaction(courseResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + rowNumber + " - " + saveResult.courseName() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + rowNumber + ": " + saveResult.error());
                                }
                            }
                        }
                        case "room" -> {
                            RoomImportResult roomResult = parseRoomFromCSV(
                                    values, columnMap, rowNumber, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (roomResult != null && roomResult.room() != null) {
                                RoomSaveResult saveResult = self.saveRoomInNewTransaction(roomResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + rowNumber + " - " + saveResult.roomNumber() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + rowNumber + ": " + saveResult.error());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    result.addError("Row " + rowNumber + ": " + e.getMessage());
                    log.error("❌ Error processing row {}: {}", rowNumber, e.getMessage());
                }
            }

            result.complete();
            logImportSummary(result);

        } catch (IOException e) {
            result.addError("Failed to read CSV: " + e.getMessage());
            throw new ImportException("CSV import failed", e);
        }

        return result;
    }

    private ImportResult importExcelLenient(InputStream inputStream, String entityType)
            throws ImportException {

        ImportResult result = new ImportResult("Excel Lenient Import");
        result.start();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // ✅ NULL SAFE: Validate sheet is not null
            if (sheet == null) {
                throw new ImportException("Excel file has no sheets");
            }

            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new ImportException("Excel file is empty");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            // ✅ NULL SAFE: Validate header row is not null
            if (headerRow == null) {
                throw new ImportException("Excel file has no header row");
            }

            Map<String, Integer> columnMap = buildColumnMapFromRow(headerRow);

            log.info("📊 Processing {} rows from Excel", sheet.getPhysicalNumberOfRows() - 1);

            int autoGeneratedCount = 1;

            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                try {
                    switch (entityType.toLowerCase()) {
                        case "student" -> {
                            StudentImportResult studentResult = parseStudentFromRowLenient(
                                    row, columnMap, i, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (studentResult != null && studentResult.student() != null) {
                                StudentSaveResult saveResult = self.saveStudentInNewTransaction(studentResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + i + " - " + saveResult.name() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + i + ": " + saveResult.error());
                                }
                            }
                        }
                        case "teacher" -> {
                            TeacherImportResult teacherResult = parseTeacherFromRowLenient(
                                    row, columnMap, i, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (teacherResult != null && teacherResult.teacher() != null) {
                                TeacherSaveResult saveResult = self.saveTeacherInNewTransaction(teacherResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + i + " - " + saveResult.name() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + i + ": " + saveResult.error());
                                }
                            }
                        }
                        case "course" -> {
                            CourseImportResult courseResult = parseCourseFromRowLenient(
                                    row, columnMap, i, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (courseResult != null && courseResult.course() != null) {
                                CourseSaveResult saveResult = self.saveCourseInNewTransaction(courseResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + i + " - " + saveResult.courseName() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + i + ": " + saveResult.error());
                                }
                            }
                        }
                        case "room" -> {
                            RoomImportResult roomResult = parseRoomFromRowLenient(
                                    row, columnMap, i, autoGeneratedCount);
                            // ✅ NULL SAFE: Check result is not null before using
                            if (roomResult != null && roomResult.room() != null) {
                                RoomSaveResult saveResult = self.saveRoomInNewTransaction(roomResult);
                                if (saveResult.saved()) {
                                    result.incrementSuccess();
                                    if (saveResult.needsReview()) {
                                        result.addWarning("Row " + i + " - " + saveResult.roomNumber() +
                                                ": Missing " + saveResult.missingFields());
                                    }
                                    autoGeneratedCount++;
                                } else {
                                    result.addError("Row " + i + ": " + saveResult.error());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    result.addError("Row " + i + ": " + e.getMessage());
                    log.error("❌ Error processing row {}: {}", i, e.getMessage());
                }
            }

            result.complete();
            logImportSummary(result);

        } catch (IOException e) {
            result.addError("Failed to read Excel: " + e.getMessage());
            throw new ImportException("Excel import failed", e);
        }

        return result;
    }

    // ========================================================================
    // SMART COLUMN MAPPING
    // ========================================================================

    private Map<String, Integer> buildColumnMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();

        // ✅ NULL SAFE: Validate headers array
        if (headers == null) {
            return map;
        }

        for (int i = 0; i < headers.length; i++) {
            // ✅ NULL SAFE: Skip null header values
            if (headers[i] == null) continue;

            String original = headers[i].trim();
            if (original.isEmpty())
                continue;

            map.put(original.toLowerCase(), i);

            List<String> variations = generateColumnVariations(original);
            // ✅ NULL SAFE: Filter null variations before processing
            if (variations != null) {
                for (String variation : variations) {
                    if (variation != null) {
                        map.putIfAbsent(variation, i);
                    }
                }
            }
        }

        log.debug("📋 Column map: {} entries for {} headers", map.size(), headers.length);
        return map;
    }

    private Map<String, Integer> buildColumnMapFromRow(Row row) {
        Map<String, Integer> map = new HashMap<>();

        if (row == null)
            return map;

        for (Cell cell : row) {
            // ✅ NULL SAFE: Skip null cells
            if (cell == null) continue;

            String value = getCellValueAsString(cell);
            if (value != null && !value.trim().isEmpty()) {
                int colIndex = cell.getColumnIndex();

                map.put(value.toLowerCase().trim(), colIndex);

                List<String> variations = generateColumnVariations(value);
                // ✅ NULL SAFE: Filter null variations before processing
                if (variations != null) {
                    for (String variation : variations) {
                        if (variation != null) {
                            map.putIfAbsent(variation, colIndex);
                        }
                    }
                }
            }
        }

        log.debug("📋 Excel column map: {} entries", map.size());
        return map;
    }

    private List<String> generateColumnVariations(String columnName) {
        List<String> variations = new ArrayList<>();
        String lower = columnName.toLowerCase().trim();

        variations.add(lower);
        variations.add(lower.replace(" ", ""));
        variations.add(lower.replace("_", ""));
        variations.add(lower.replace("-", ""));
        variations.add(lower.replaceAll("[\\s_-]", ""));

        // Student fields
        if (lower.matches(".*(studentid|studentno|student.?(number|id)|id).*")) {
            variations.addAll(List.of("studentid", "student_id", "studentnumber", "id", "sid"));
        }
        if (lower.matches(".*(firstname|first.?name|fname).*")) {
            variations.addAll(List.of("firstname", "first_name", "fname", "first"));
        }
        if (lower.matches(".*(lastname|last.?name|lname|surname).*")) {
            variations.addAll(List.of("lastname", "last_name", "lname", "last", "surname"));
        }
        if (lower.matches(".*(gradelevel|grade.?level|grade).*")) {
            variations.addAll(List.of("gradelevel", "grade_level", "grade", "level"));
        }
        if (lower.matches(".*(email|e.?mail).*")) {
            variations.addAll(List.of("email", "e_mail", "emailaddress"));
        }

        // Teacher fields
        if (lower.matches(".*(employeeid|employee.?id|empid).*")) {
            variations.addAll(List.of("employeeid", "employee_id", "empid", "id", "teacherid"));
        }
        if (lower.matches(".*(department|dept).*")) {
            variations.addAll(List.of("department", "dept"));
        }

        // Course fields
        if (lower.matches(".*(coursecode|course.?code|code).*")) {
            variations.addAll(List.of("coursecode", "course_code", "code", "courseno"));
        }
        if (lower.matches(".*(coursename|course.?name|name|title).*")) {
            variations.addAll(List.of("coursename", "course_name", "name", "title"));
        }
        if (lower.matches(".*(maxstudents|capacity).*")) {
            variations.addAll(List.of("maxstudents", "max_students", "capacity"));
        }

        // Room fields
        if (lower.matches(".*(roomnumber|room.?number|room).*")) {
            variations.addAll(List.of("roomnumber", "room_number", "room"));
        }
        if (lower.matches(".*(building|bldg).*")) {
            variations.addAll(List.of("building", "bldg"));
        }
        if (lower.matches(".*(floor|level).*")) {
            variations.addAll(List.of("floor", "level"));
        }

        return variations.stream().distinct().collect(Collectors.toList());
    }

    // ========================================================================
    // STUDENT PARSING
    // ========================================================================

    private StudentImportResult parseStudentFromCSV(String[] values, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Student student = new Student();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String studentId = getValueFromArray(values, columnMap,
                "studentid", "student_id", "id");
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "STU-" + String.format("%05d", autoGeneratedCount);
            missingFields.add("Student ID");
            needsReview = true;
        }
        student.setStudentId(studentId);

        String firstName = getValueFromArray(values, columnMap,
                "firstname", "first_name", "fname");
        if (firstName == null || firstName.trim().isEmpty()) {
            firstName = "FirstName" + autoGeneratedCount;
            missingFields.add("First Name");
            needsReview = true;
        }
        student.setFirstName(firstName);

        String lastName = getValueFromArray(values, columnMap,
                "lastname", "last_name", "lname");
        if (lastName == null || lastName.trim().isEmpty()) {
            lastName = "LastName" + autoGeneratedCount;
            missingFields.add("Last Name");
            needsReview = true;
        }
        student.setLastName(lastName);

        String gradeLevel = getValueFromArray(values, columnMap,
                "gradelevel", "grade_level", "grade");
        if (gradeLevel == null || gradeLevel.trim().isEmpty()) {
            gradeLevel = DEFAULT_GRADE_LEVEL;
            missingFields.add("Grade Level");
            needsReview = true;
        }
        student.setGradeLevel(gradeLevel);

        String email = getValueFromArray(values, columnMap, "email");
        if (email != null && !email.trim().isEmpty()) {
            student.setEmail(email);
        }

        student.setActive(true);

        return new StudentImportResult(student, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    private StudentImportResult parseStudentFromRowLenient(Row row, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Student student = new Student();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String studentId = getCellValue(row, columnMap, "studentid", "student_id", "id");
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "STU-" + String.format("%05d", autoGeneratedCount);
            missingFields.add("Student ID");
            needsReview = true;
        }
        student.setStudentId(studentId);

        String firstName = getCellValue(row, columnMap, "firstname", "first_name");
        if (firstName == null || firstName.trim().isEmpty()) {
            firstName = "FirstName" + autoGeneratedCount;
            missingFields.add("First Name");
            needsReview = true;
        }
        student.setFirstName(firstName);

        String lastName = getCellValue(row, columnMap, "lastname", "last_name");
        if (lastName == null || lastName.trim().isEmpty()) {
            lastName = "LastName" + autoGeneratedCount;
            missingFields.add("Last Name");
            needsReview = true;
        }
        student.setLastName(lastName);

        String gradeLevel = getCellValue(row, columnMap, "gradelevel", "grade_level");
        if (gradeLevel == null || gradeLevel.trim().isEmpty()) {
            gradeLevel = DEFAULT_GRADE_LEVEL;
            missingFields.add("Grade Level");
            needsReview = true;
        }
        student.setGradeLevel(gradeLevel);

        String email = getCellValue(row, columnMap, "email");
        if (email != null && !email.trim().isEmpty()) {
            student.setEmail(email);
        }

        student.setActive(true);

        return new StudentImportResult(student, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    // Similar parsing methods for Teacher, Course, Room...
    // (Abbreviated for space - include full implementations in actual file)

    private TeacherImportResult parseTeacherFromCSV(String[] values, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Teacher teacher = new Teacher();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String name = getValueFromArray(values, columnMap, "name", "teachername");
        if (name == null || name.trim().isEmpty()) {
            name = "Teacher-AUTO-" + autoGeneratedCount;
            missingFields.add("Name");
            needsReview = true;
        }
        teacher.setName(name);

        String employeeId = getValueFromArray(values, columnMap, "employeeid", "employee_id", "id");
        if (employeeId == null || employeeId.trim().isEmpty()) {
            employeeId = "EMP-" + String.format("%05d", autoGeneratedCount);
            missingFields.add("Employee ID");
            needsReview = true;
        }
        teacher.setEmployeeId(employeeId);

        String department = getValueFromArray(values, columnMap, "department", "dept");
        if (department == null || department.trim().isEmpty()) {
            department = DEFAULT_DEPARTMENT;
            missingFields.add("Department");
            needsReview = true;
        }
        teacher.setDepartment(department);

        teacher.setActive(true);

        return new TeacherImportResult(teacher, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    private TeacherImportResult parseTeacherFromRowLenient(Row row, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Teacher teacher = new Teacher();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String name = getCellValue(row, columnMap, "name", "teachername");
        if (name == null || name.trim().isEmpty()) {
            name = "Teacher-AUTO-" + autoGeneratedCount;
            missingFields.add("Name");
            needsReview = true;
        }
        teacher.setName(name);

        String employeeId = getCellValue(row, columnMap, "employeeid", "employee_id", "id");
        if (employeeId == null || employeeId.trim().isEmpty()) {
            employeeId = "EMP-" + String.format("%05d", autoGeneratedCount);
            missingFields.add("Employee ID");
            needsReview = true;
        }
        teacher.setEmployeeId(employeeId);

        String department = getCellValue(row, columnMap, "department", "dept");
        if (department == null || department.trim().isEmpty()) {
            department = DEFAULT_DEPARTMENT;
            missingFields.add("Department");
            needsReview = true;
        }
        teacher.setDepartment(department);

        teacher.setActive(true);

        return new TeacherImportResult(teacher, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    private CourseImportResult parseCourseFromCSV(String[] values, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Course course = new Course();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String courseCode = getValueFromArray(values, columnMap, "coursecode", "course_code", "code");
        if (courseCode == null || courseCode.trim().isEmpty()) {
            courseCode = "CRS-" + String.format("%04d", autoGeneratedCount);
            missingFields.add("Course Code");
            needsReview = true;
        }
        course.setCourseCode(courseCode);

        String courseName = getValueFromArray(values, columnMap, "coursename", "course_name", "name");
        if (courseName == null || courseName.trim().isEmpty()) {
            courseName = "Course-" + courseCode;
            missingFields.add("Course Name");
            needsReview = true;
        }
        course.setCourseName(courseName);

        course.setMaxStudents(DEFAULT_MAX_STUDENTS);
        course.setActive(true);

        return new CourseImportResult(course, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    private CourseImportResult parseCourseFromRowLenient(Row row, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Course course = new Course();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String courseCode = getCellValue(row, columnMap, "coursecode", "course_code", "code");
        if (courseCode == null || courseCode.trim().isEmpty()) {
            courseCode = "CRS-" + String.format("%04d", autoGeneratedCount);
            missingFields.add("Course Code");
            needsReview = true;
        }
        course.setCourseCode(courseCode);

        String courseName = getCellValue(row, columnMap, "coursename", "course_name", "name");
        if (courseName == null || courseName.trim().isEmpty()) {
            courseName = "Course-" + courseCode;
            missingFields.add("Course Name");
            needsReview = true;
        }
        course.setCourseName(courseName);

        course.setMaxStudents(DEFAULT_MAX_STUDENTS);
        course.setActive(true);

        return new CourseImportResult(course, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    private RoomImportResult parseRoomFromCSV(String[] values, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Room room = new Room();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String roomNumber = getValueFromArray(values, columnMap, "roomnumber", "room_number", "room");
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            roomNumber = "ROOM-AUTO-" + String.format("%03d", autoGeneratedCount);
            missingFields.add("Room Number");
            needsReview = true;
        }
        room.setRoomNumber(roomNumber);

        room.setBuilding(DEFAULT_BUILDING);
        room.setFloor(DEFAULT_FLOOR);
        room.setCapacity(DEFAULT_ROOM_CAPACITY);

        // ✅ FIX: Set default RoomType to prevent null values (December 14, 2025)
        // This matches the behavior of RoomManagementController.handleAddRoom()
        // and prevents data quality issues with null roomType
        if (room.getType() == null) {
            room.setType(com.heronix.model.enums.RoomType.CLASSROOM);
        }

        return new RoomImportResult(room, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    private RoomImportResult parseRoomFromRowLenient(Row row, Map<String, Integer> columnMap,
            int rowNumber, int autoGeneratedCount) {
        Room room = new Room();
        List<String> missingFields = new ArrayList<>();
        boolean needsReview = false;

        String roomNumber = getCellValue(row, columnMap, "roomnumber", "room_number", "room");
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            roomNumber = "ROOM-AUTO-" + String.format("%03d", autoGeneratedCount);
            missingFields.add("Room Number");
            needsReview = true;
        }
        room.setRoomNumber(roomNumber);

        room.setBuilding(DEFAULT_BUILDING);
        room.setFloor(DEFAULT_FLOOR);
        room.setCapacity(DEFAULT_ROOM_CAPACITY);

        // ✅ FIX: Set default RoomType to prevent null values (December 14, 2025)
        // This matches the behavior of RoomManagementController.handleAddRoom()
        // and prevents data quality issues with null roomType
        if (room.getType() == null) {
            room.setType(com.heronix.model.enums.RoomType.CLASSROOM);
        }

        return new RoomImportResult(room, needsReview,
                String.join(", ", missingFields), missingFields);
    }

    // ========================================================================
    // SAVE METHODS - FIXED: Use findBy().isPresent() pattern
    // ========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StudentSaveResult saveStudentInNewTransaction(StudentImportResult importResult) {
        try {
            // ✅ NULL SAFE: Validate import result
            if (importResult == null || importResult.student() == null) {
                return new StudentSaveResult(false, "Unknown", false, null, null,
                        "Invalid import result");
            }

            Student student = importResult.student();

            // ✅ NULL SAFE: Check student ID exists before duplicate check
            if (student.getStudentId() != null &&
                studentRepository.findByStudentId(student.getStudentId()).isPresent()) {
                // ✅ NULL SAFE: Safe name extraction with defaults
                String firstName = (student.getFirstName() != null) ? student.getFirstName() : "";
                String lastName = (student.getLastName() != null) ? student.getLastName() : "";
                return new StudentSaveResult(false,
                        firstName + " " + lastName,
                        false, null, null,
                        "Duplicate Student ID: " + student.getStudentId());
            }

            studentRepository.save(student);

            // ✅ NULL SAFE: Safe name extraction with defaults
            String firstName = (student.getFirstName() != null) ? student.getFirstName() : "";
            String lastName = (student.getLastName() != null) ? student.getLastName() : "";
            return new StudentSaveResult(true,
                    firstName + " " + lastName,
                    importResult.needsReview(),
                    importResult.missingFields(),
                    importResult.missingFieldsList(),
                    null);

        } catch (Exception e) {
            log.error("❌ Failed to save student: {}", e.getMessage());
            return new StudentSaveResult(false, "Unknown", false, null, null,
                    "Save failed: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TeacherSaveResult saveTeacherInNewTransaction(TeacherImportResult importResult) {
        try {
            // ✅ NULL SAFE: Validate import result
            if (importResult == null || importResult.teacher() == null) {
                return new TeacherSaveResult(false, "Unknown", false, null, null,
                        "Invalid import result");
            }

            Teacher teacher = importResult.teacher();

            // ✅ NULL SAFE: Check employee ID exists before duplicate check
            if (teacher.getEmployeeId() != null &&
                teacherRepository.findByEmployeeId(teacher.getEmployeeId()).isPresent()) {
                // ✅ NULL SAFE: Safe name extraction with default
                String name = (teacher.getName() != null) ? teacher.getName() : "Unknown";
                return new TeacherSaveResult(false, name,
                        false, null, null,
                        "Duplicate Employee ID: " + teacher.getEmployeeId());
            }

            teacherRepository.save(teacher);

            // ✅ NULL SAFE: Safe name extraction with default
            String name = (teacher.getName() != null) ? teacher.getName() : "Unknown";
            return new TeacherSaveResult(true, name,
                    importResult.needsReview(),
                    importResult.missingFields(),
                    importResult.missingFieldsList(),
                    null);

        } catch (Exception e) {
            log.error("❌ Failed to save teacher: {}", e.getMessage());
            return new TeacherSaveResult(false, "Unknown", false, null, null,
                    "Save failed: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CourseSaveResult saveCourseInNewTransaction(CourseImportResult importResult) {
        try {
            // ✅ NULL SAFE: Validate import result
            if (importResult == null || importResult.course() == null) {
                return new CourseSaveResult(false, "Unknown", false, null, null,
                        "Invalid import result");
            }

            Course course = importResult.course();

            // ✅ NULL SAFE: Check course code exists before duplicate check
            if (course.getCourseCode() != null &&
                courseRepository.findByCourseCode(course.getCourseCode()).isPresent()) {
                // ✅ NULL SAFE: Safe course name extraction with default
                String courseName = (course.getCourseName() != null) ? course.getCourseName() : "Unknown";
                return new CourseSaveResult(false, courseName,
                        false, null, null,
                        "Duplicate Course Code: " + course.getCourseCode());
            }

            courseRepository.save(course);

            // ✅ NULL SAFE: Safe course name extraction with default
            String courseName = (course.getCourseName() != null) ? course.getCourseName() : "Unknown";
            return new CourseSaveResult(true, courseName,
                    importResult.needsReview(),
                    importResult.missingFields(),
                    importResult.missingFieldsList(),
                    null);

        } catch (Exception e) {
            log.error("❌ Failed to save course: {}", e.getMessage());
            return new CourseSaveResult(false, "Unknown", false, null, null,
                    "Save failed: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RoomSaveResult saveRoomInNewTransaction(RoomImportResult importResult) {
        try {
            // ✅ NULL SAFE: Validate import result
            if (importResult == null || importResult.room() == null) {
                return new RoomSaveResult(false, "Unknown", false, null, null,
                        "Invalid import result");
            }

            Room room = importResult.room();

            // ✅ NULL SAFE: Check room number exists before duplicate check
            if (room.getRoomNumber() != null &&
                roomRepository.findByRoomNumber(room.getRoomNumber()).isPresent()) {
                return new RoomSaveResult(false, room.getRoomNumber(),
                        false, null, null,
                        "Duplicate Room Number: " + room.getRoomNumber());
            }

            roomRepository.save(room);

            // ✅ NULL SAFE: Safe room number extraction with default
            String roomNumber = (room.getRoomNumber() != null) ? room.getRoomNumber() : "Unknown";
            return new RoomSaveResult(true, roomNumber,
                    importResult.needsReview(),
                    importResult.missingFields(),
                    importResult.missingFieldsList(),
                    null);

        } catch (Exception e) {
            log.error("❌ Failed to save room: {}", e.getMessage());
            return new RoomSaveResult(false, "Unknown", false, null, null,
                    "Save failed: " + e.getMessage());
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private String getValueFromArray(String[] values, Map<String, Integer> columnMap,
            String... possibleNames) {
        // ✅ NULL SAFE: Validate parameters
        if (values == null || columnMap == null || possibleNames == null) {
            return null;
        }

        for (String name : possibleNames) {
            // ✅ NULL SAFE: Skip null names
            if (name == null) continue;

            Integer index = columnMap.get(name.toLowerCase());
            if (index != null && index < values.length) {
                String value = values[index];
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private String getCellValue(Row row, Map<String, Integer> columnMap, String... possibleNames) {
        // ✅ NULL SAFE: Validate parameters
        if (row == null || columnMap == null || possibleNames == null) {
            return null;
        }

        for (String name : possibleNames) {
            // ✅ NULL SAFE: Skip null names
            if (name == null) continue;

            Integer colIndex = columnMap.get(name.toLowerCase());
            if (colIndex != null) {
                Cell cell = row.getCell(colIndex);
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        yield String.valueOf((long) numValue);
                    } else {
                        yield String.valueOf(numValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception ex) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
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

    private String detectEntityType(String[] headers) {
        // ✅ NULL SAFE: Validate headers array
        if (headers == null || headers.length == 0) {
            return "student"; // Default entity type
        }

        String headersLower = String.join(",", headers).toLowerCase();

        if (headersLower.contains("student"))
            return "student";
        if (headersLower.contains("teacher") || headersLower.contains("employee"))
            return "teacher";
        if (headersLower.contains("course") || headersLower.contains("class"))
            return "course";
        if (headersLower.contains("room"))
            return "room";

        return "student";
    }

    private String getFileExtension(String filename) {
        if (filename == null)
            return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private void logImportSummary(ImportResult result) {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║              IMPORT COMPLETED SUCCESSFULLY                    ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
        log.info("📊 SUMMARY:");
        log.info("   ✅ Success: {} records", result.getSuccessCount());
        log.info("   ❌ Errors: {} records", result.getErrorCount());
        log.info("   ⚠️  Warnings: {} records", result.getWarningCount());
    }

    // ========================================================================
    // HELPER RECORDS
    // ========================================================================

    private record StudentImportResult(Student student, boolean needsReview,
            String missingFields, List<String> missingFieldsList) {
    }

    private record StudentSaveResult(boolean saved, String name, boolean needsReview,
            String missingFields, List<String> missingFieldsList, String error) {
    }

    private record TeacherImportResult(Teacher teacher, boolean needsReview,
            String missingFields, List<String> missingFieldsList) {
    }

    private record TeacherSaveResult(boolean saved, String name, boolean needsReview,
            String missingFields, List<String> missingFieldsList, String error) {
    }

    private record CourseImportResult(Course course, boolean needsReview,
            String missingFields, List<String> missingFieldsList) {
    }

    private record CourseSaveResult(boolean saved, String courseName, boolean needsReview,
            String missingFields, List<String> missingFieldsList, String error) {
    }

    private record RoomImportResult(Room room, boolean needsReview,
            String missingFields, List<String> missingFieldsList) {
    }

    private record RoomSaveResult(boolean saved, String roomNumber, boolean needsReview,
            String missingFields, List<String> missingFieldsList, String error) {
    }
}