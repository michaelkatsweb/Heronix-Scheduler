package com.heronix.service.parser;

import com.heronix.model.dto.FrontlineImportDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for Frontline CSV files
 * Supports flexible column mapping for different Frontline export formats
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Component
public class FrontlineCSVParser {

    private static final Logger logger = LoggerFactory.getLogger(FrontlineCSVParser.class);

    // Common date/time formatters
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("M-d-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy")
    );

    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("h:mm a"),
            DateTimeFormatter.ofPattern("hh:mm a"),
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mma"),
            DateTimeFormatter.ofPattern("hh:mma")
    );

    /**
     * Parse a Frontline CSV file
     */
    public List<FrontlineImportDTO> parseCSV(File file) throws IOException {
        logger.info("Parsing Frontline CSV file: {}", file.getName());

        List<FrontlineImportDTO> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }

            Map<String, Integer> columnMap = parseHeader(headerLine);
            logger.debug("Detected {} columns in CSV", columnMap.size());

            // Read data lines
            String line;
            int rowNumber = 2; // Start at 2 (header is row 1)

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                try {
                    FrontlineImportDTO dto = parseLine(line, columnMap, rowNumber);
                    if (dto != null) {
                        records.add(dto);
                    }
                } catch (Exception e) {
                    logger.warn("Error parsing row {}: {}", rowNumber, e.getMessage());
                    // Continue with next row
                }

                rowNumber++;
            }
        }

        logger.info("Successfully parsed {} records from {}", records.size(), file.getName());
        return records;
    }

    /**
     * Parse CSV header and create column mapping
     */
    private Map<String, Integer> parseHeader(String headerLine) {
        Map<String, Integer> columnMap = new HashMap<>();
        String[] columns = splitCSVLine(headerLine);

        for (int i = 0; i < columns.length; i++) {
            String normalized = normalizeColumnName(columns[i]);
            columnMap.put(normalized, i);
            logger.trace("Column {}: '{}' -> '{}'", i, columns[i], normalized);
        }

        return columnMap;
    }

    /**
     * Parse a single CSV line into a DTO
     */
    private FrontlineImportDTO parseLine(String line, Map<String, Integer> columnMap, int rowNumber) {
        String[] values = splitCSVLine(line);
        FrontlineImportDTO dto = new FrontlineImportDTO();
        dto.setRowNumber(rowNumber);
        dto.setRawData(line);

        // Parse substitute information
        dto.setSubstituteFirstName(getColumnValue(values, columnMap,
                "substitute_first_name", "sub_first_name", "first_name"));
        dto.setSubstituteLastName(getColumnValue(values, columnMap,
                "substitute_last_name", "sub_last_name", "last_name"));
        dto.setSubstituteEmployeeId(getColumnValue(values, columnMap,
                "substitute_id", "sub_id", "employee_id", "emp_id"));
        dto.setSubstituteEmail(getColumnValue(values, columnMap,
                "substitute_email", "sub_email", "email"));
        dto.setSubstitutePhone(getColumnValue(values, columnMap,
                "substitute_phone", "sub_phone", "phone"));
        dto.setSubstituteType(getColumnValue(values, columnMap,
                "substitute_type", "sub_type", "type"));

        // Parse assignment information
        dto.setJobId(getColumnValue(values, columnMap,
                "job_id", "assignment_id", "id"));

        String dateStr = getColumnValue(values, columnMap,
                "date", "assignment_date", "work_date");
        dto.setAssignmentDate(parseDate(dateStr));

        String startTimeStr = getColumnValue(values, columnMap,
                "start_time", "time_in", "begin_time");
        dto.setStartTime(parseTime(startTimeStr));

        String endTimeStr = getColumnValue(values, columnMap,
                "end_time", "time_out", "finish_time");
        dto.setEndTime(parseTime(endTimeStr));

        String endDateStr = getColumnValue(values, columnMap,
                "end_date", "finish_date", "last_date");
        if (endDateStr != null && !endDateStr.isEmpty()) {
            dto.setEndDate(parseDate(endDateStr));
        }

        // Parse replaced staff information
        dto.setReplacedStaffFirstName(getColumnValue(values, columnMap,
                "teacher_first_name", "staff_first_name", "absent_first_name"));
        dto.setReplacedStaffLastName(getColumnValue(values, columnMap,
                "teacher_last_name", "staff_last_name", "absent_last_name"));
        dto.setReplacedStaffEmployeeId(getColumnValue(values, columnMap,
                "teacher_id", "staff_id", "absent_id"));
        dto.setReplacedStaffType(getColumnValue(values, columnMap,
                "staff_type", "position", "role"));

        // Parse assignment details
        dto.setAbsenceReason(getColumnValue(values, columnMap,
                "absence_reason", "reason", "leave_type"));
        dto.setDurationType(getColumnValue(values, columnMap,
                "duration_type", "duration", "assignment_type"));
        dto.setStatus(getColumnValue(values, columnMap,
                "status", "assignment_status", "job_status"));

        String floaterStr = getColumnValue(values, columnMap,
                "is_floater", "floater", "multi_class");
        dto.setIsFloater(parseBoolean(floaterStr));

        // Parse location/course information
        dto.setRoomNumber(getColumnValue(values, columnMap,
                "room", "room_number", "location"));
        dto.setCourseName(getColumnValue(values, columnMap,
                "course_name", "course", "class_name"));
        dto.setCourseCode(getColumnValue(values, columnMap,
                "course_code", "class_code", "section"));
        dto.setSubject(getColumnValue(values, columnMap,
                "subject", "subject_area", "department"));
        dto.setGradeLevel(getColumnValue(values, columnMap,
                "grade_level", "grade", "level"));

        // Parse additional information
        dto.setNotes(getColumnValue(values, columnMap,
                "notes", "comments", "description"));

        String hoursStr = getColumnValue(values, columnMap,
                "hours", "total_hours", "work_hours");
        dto.setHours(parseDouble(hoursStr));

        String payStr = getColumnValue(values, columnMap,
                "pay_amount", "pay", "amount", "total_pay");
        dto.setPayAmount(parseDouble(payStr));

        return dto;
    }

    /**
     * Get value from CSV columns, trying multiple possible column names
     */
    private String getColumnValue(String[] values, Map<String, Integer> columnMap, String... possibleNames) {
        for (String name : possibleNames) {
            Integer index = columnMap.get(normalizeColumnName(name));
            if (index != null && index < values.length) {
                String value = values[index].trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Normalize column name for matching
     */
    private String normalizeColumnName(String columnName) {
        return columnName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Split CSV line handling quoted fields
     */
    private String[] splitCSVLine(String line) {
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

    /**
     * Parse date string with multiple format attempts
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        logger.warn("Could not parse date: '{}'", dateStr);
        return null;
    }

    /**
     * Parse time string with multiple format attempts
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        timeStr = timeStr.trim();

        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(timeStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        logger.warn("Could not parse time: '{}'", timeStr);
        return null;
    }

    /**
     * Parse double value
     */
    private Double parseDouble(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove currency symbols and commas
            String cleaned = str.replaceAll("[$,]", "").trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse double: '{}'", str);
            return null;
        }
    }

    /**
     * Parse boolean value
     */
    private Boolean parseBoolean(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        str = str.trim().toLowerCase();
        return "true".equals(str) || "yes".equals(str) || "1".equals(str) || "y".equals(str);
    }
}
