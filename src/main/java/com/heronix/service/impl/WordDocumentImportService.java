package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word Document Import Service
 * Imports schedules and course data from Word documents (.docx)
 *
 * Features:
 * - Table extraction for schedule grids
 * - Text parsing for course/teacher info
 * - IEP document parsing
 * - Syllabus import
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 15 - Document Import
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordDocumentImportService {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    // Common patterns
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(\\d{1,2}:\\d{2})\\s*(?:AM|PM|am|pm)?\\s*[-–]\\s*(\\d{1,2}:\\d{2})\\s*(?:AM|PM|am|pm)?");
    private static final Pattern PERIOD_PATTERN = Pattern.compile(
        "(?i)(?:Period|Per\\.?|P)\\s*(\\d+)");
    private static final Pattern ROOM_PATTERN = Pattern.compile(
        "(?i)(?:Room|Rm\\.?)\\s*([A-Z]?\\d+[A-Z]?)");

    /**
     * Import schedule from Word document
     */
    @Transactional
    public WordImportResult importScheduleFromWord(File wordFile) throws IOException {
        // ✅ NULL SAFE: Validate wordFile parameter
        if (wordFile == null) {
            throw new IllegalArgumentException("Word file cannot be null");
        }

        log.info("Starting Word import from: {}", wordFile.getName());

        WordImportResult result = new WordImportResult();
        result.setFileName(wordFile.getName());

        try (FileInputStream fis = new FileInputStream(wordFile);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Extract text content
            StringBuilder textContent = new StringBuilder();
            // ✅ NULL SAFE: Validate paragraphs list
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            if (paragraphs != null) {
                for (XWPFParagraph para : paragraphs) {
                    // ✅ NULL SAFE: Skip null paragraphs
                    if (para != null && para.getText() != null) {
                        textContent.append(para.getText()).append("\n");
                    }
                }
            }
            result.setExtractedText(textContent.toString());

            // Process tables (schedule grids)
            List<ParsedScheduleEntry> tableEntries = processScheduleTables(document);
            // ✅ NULL SAFE: Validate table entries result
            if (tableEntries != null) {
                result.getScheduleEntries().addAll(tableEntries);
            }

            // Process text content
            List<ParsedScheduleEntry> textEntries = parseTextContent(textContent.toString());
            // ✅ NULL SAFE: Validate text entries result
            if (textEntries != null) {
                result.getScheduleEntries().addAll(textEntries);
            }

            // Match entities
            // ✅ NULL SAFE: Validate schedule entries list
            List<ParsedScheduleEntry> entries = result.getScheduleEntries();
            if (entries != null) {
                for (ParsedScheduleEntry entry : entries) {
                    // ✅ NULL SAFE: Skip null entries
                    if (entry != null) {
                        matchEntities(entry);
                    }
                }
            }

            result.setSuccess(true);
            result.setEntriesFound(result.getScheduleEntries().size());

            log.info("Word import complete: {} entries found", result.getEntriesFound());

        } catch (Exception e) {
            log.error("Error importing Word document: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * Import schedule from InputStream
     */
    @Transactional
    public WordImportResult importScheduleFromWord(InputStream inputStream, String fileName)
            throws IOException {
        // ✅ NULL SAFE: Validate inputStream parameter
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        File tempFile = File.createTempFile("word_import_", ".docx");
        try {
            java.nio.file.Files.copy(inputStream, tempFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            WordImportResult result = importScheduleFromWord(tempFile);
            result.setFileName(fileName);
            return result;
        } finally {
            // ✅ NULL SAFE: Safe file deletion
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Process tables in the document (likely schedule grids)
     */
    private List<ParsedScheduleEntry> processScheduleTables(XWPFDocument document) {
        List<ParsedScheduleEntry> entries = new ArrayList<>();

        for (XWPFTable table : document.getTables()) {
            List<XWPFTableRow> rows = table.getRows();
            if (rows.isEmpty()) continue;

            // Try to identify header row
            XWPFTableRow headerRow = rows.get(0);
            List<String> headers = extractRowText(headerRow);

            // Detect if this is a schedule table
            if (isScheduleTable(headers)) {
                entries.addAll(processScheduleTable(table, headers));
            }
        }

        return entries;
    }

    /**
     * Check if table headers suggest a schedule
     */
    private boolean isScheduleTable(List<String> headers) {
        String combined = String.join(" ", headers).toLowerCase();
        return combined.contains("period") ||
               combined.contains("time") ||
               combined.contains("monday") ||
               combined.contains("course") ||
               combined.contains("teacher");
    }

    /**
     * Process a schedule table
     */
    private List<ParsedScheduleEntry> processScheduleTable(XWPFTable table, List<String> headers) {
        List<ParsedScheduleEntry> entries = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();

        // Find column indices
        int periodCol = findColumnIndex(headers, "period", "per", "p");
        int timeCol = findColumnIndex(headers, "time");
        int courseCol = findColumnIndex(headers, "course", "class", "subject");
        int teacherCol = findColumnIndex(headers, "teacher", "instructor");
        int roomCol = findColumnIndex(headers, "room", "rm", "location");

        // Check for day-based columns (M, T, W, R, F or Monday, Tuesday, etc.)
        Map<DayOfWeek, Integer> dayColumns = findDayColumns(headers);

        // Process data rows
        for (int i = 1; i < rows.size(); i++) {
            List<String> cells = extractRowText(rows.get(i));
            if (cells.isEmpty()) continue;

            if (!dayColumns.isEmpty()) {
                // Day-column format
                for (Map.Entry<DayOfWeek, Integer> dayEntry : dayColumns.entrySet()) {
                    if (dayEntry.getValue() < cells.size()) {
                        String cellContent = cells.get(dayEntry.getValue());
                        if (!cellContent.trim().isEmpty()) {
                            ParsedScheduleEntry entry = new ParsedScheduleEntry();
                            entry.setDayOfWeek(dayEntry.getKey());
                            entry.setRawText(cellContent);

                            if (periodCol >= 0 && periodCol < cells.size()) {
                                entry.setPeriodNumber(extractPeriod(cells.get(periodCol)));
                            }
                            if (timeCol >= 0 && timeCol < cells.size()) {
                                parseTimeRange(cells.get(timeCol), entry);
                            }

                            parseContentCell(cellContent, entry);
                            entries.add(entry);
                        }
                    }
                }
            } else {
                // Regular row format
                ParsedScheduleEntry entry = new ParsedScheduleEntry();
                entry.setRawText(String.join(" | ", cells));

                if (periodCol >= 0 && periodCol < cells.size()) {
                    entry.setPeriodNumber(extractPeriod(cells.get(periodCol)));
                }
                if (timeCol >= 0 && timeCol < cells.size()) {
                    parseTimeRange(cells.get(timeCol), entry);
                }
                if (courseCol >= 0 && courseCol < cells.size()) {
                    entry.setCourseName(cells.get(courseCol).trim());
                }
                if (teacherCol >= 0 && teacherCol < cells.size()) {
                    entry.setTeacherName(cells.get(teacherCol).trim());
                }
                if (roomCol >= 0 && roomCol < cells.size()) {
                    entry.setRoomNumber(extractRoom(cells.get(roomCol)));
                }

                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Extract text from table row
     */
    private List<String> extractRowText(XWPFTableRow row) {
        List<String> cells = new ArrayList<>();
        for (XWPFTableCell cell : row.getTableCells()) {
            cells.add(cell.getText().trim());
        }
        return cells;
    }

    /**
     * Find column index by keyword
     */
    private int findColumnIndex(List<String> headers, String... keywords) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase();
            for (String keyword : keywords) {
                if (h.contains(keyword)) return i;
            }
        }
        return -1;
    }

    /**
     * Find day-of-week columns
     */
    private Map<DayOfWeek, Integer> findDayColumns(List<String> headers) {
        Map<DayOfWeek, Integer> dayColumns = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase().trim();
            DayOfWeek day = parseDayOfWeek(h);
            if (day != null) {
                dayColumns.put(day, i);
            }
        }

        return dayColumns;
    }

    /**
     * Parse day of week from string
     */
    private DayOfWeek parseDayOfWeek(String text) {
        String t = text.toLowerCase().trim();
        if (t.equals("m") || t.startsWith("mon")) return DayOfWeek.MONDAY;
        if (t.equals("t") || t.startsWith("tue")) return DayOfWeek.TUESDAY;
        if (t.equals("w") || t.startsWith("wed")) return DayOfWeek.WEDNESDAY;
        if (t.equals("r") || t.startsWith("thu")) return DayOfWeek.THURSDAY;
        if (t.equals("f") || t.startsWith("fri")) return DayOfWeek.FRIDAY;
        return null;
    }

    /**
     * Parse text content for schedule info
     */
    private List<ParsedScheduleEntry> parseTextContent(String text) {
        List<ParsedScheduleEntry> entries = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Look for time patterns indicating schedule entries
            Matcher timeMatcher = TIME_PATTERN.matcher(line);
            if (timeMatcher.find()) {
                ParsedScheduleEntry entry = new ParsedScheduleEntry();
                entry.setRawText(line);
                parseTimeRange(line, entry);

                // Extract period
                Matcher periodMatcher = PERIOD_PATTERN.matcher(line);
                if (periodMatcher.find()) {
                    entry.setPeriodNumber(Integer.parseInt(periodMatcher.group(1)));
                }

                // Extract room
                entry.setRoomNumber(extractRoom(line));

                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Parse time range from text
     */
    private void parseTimeRange(String text, ParsedScheduleEntry entry) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                entry.setStartTime(parseTime(matcher.group(1)));
                entry.setEndTime(parseTime(matcher.group(2)));
            } catch (Exception e) {
                log.debug("Could not parse time: {}", text);
            }
        }
    }

    /**
     * Parse time string
     */
    private LocalTime parseTime(String timeStr) {
        String normalized = timeStr.trim().toUpperCase()
            .replaceAll("\\s*(AM|PM)\\s*", "");
        String[] parts = normalized.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // Adjust for PM if time string contained PM
        if (timeStr.toUpperCase().contains("PM") && hour < 12) {
            hour += 12;
        }

        return LocalTime.of(hour, minute);
    }

    /**
     * Extract period number
     */
    private Integer extractPeriod(String text) {
        Matcher matcher = PERIOD_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        // Try plain number
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract room number
     */
    private String extractRoom(String text) {
        Matcher matcher = ROOM_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Parse content cell for course/teacher
     */
    private void parseContentCell(String content, ParsedScheduleEntry entry) {
        // Common patterns: "Course - Teacher" or "Course\nTeacher"
        if (content.contains(" - ")) {
            String[] parts = content.split(" - ", 2);
            entry.setCourseName(parts[0].trim());
            if (parts.length > 1) {
                entry.setTeacherName(parts[1].trim());
            }
        } else if (content.contains("\n")) {
            String[] parts = content.split("\n", 2);
            entry.setCourseName(parts[0].trim());
            if (parts.length > 1) {
                entry.setTeacherName(parts[1].trim());
            }
        } else {
            entry.setCourseName(content.trim());
        }
    }

    /**
     * Match parsed entry to existing entities
     */
    private void matchEntities(ParsedScheduleEntry entry) {
        // Match course
        if (entry.getCourseName() != null) {
            List<Course> courses = courseRepository.findAll();
            for (Course c : courses) {
                if (c.getCourseName().equalsIgnoreCase(entry.getCourseName()) ||
                    (c.getCourseCode() != null && c.getCourseCode().equalsIgnoreCase(entry.getCourseName()))) {
                    entry.setMatchedCourseId(c.getId());
                    break;
                }
            }
        }

        // Match teacher
        if (entry.getTeacherName() != null) {
            List<Teacher> teachers = teacherRepository.findAllActive();
            String teacherNorm = entry.getTeacherName().toLowerCase();
            for (Teacher t : teachers) {
                String fullName = (t.getFirstName() + " " + t.getLastName()).toLowerCase();
                if (fullName.contains(teacherNorm) || teacherNorm.contains(t.getLastName().toLowerCase())) {
                    entry.setMatchedTeacherId(t.getId());
                    break;
                }
            }
        }

        // Match room
        if (entry.getRoomNumber() != null) {
            List<Room> rooms = roomRepository.findAll();
            for (Room r : rooms) {
                if (r.getRoomNumber().equalsIgnoreCase(entry.getRoomNumber())) {
                    entry.setMatchedRoomId(r.getId());
                    break;
                }
            }
        }
    }

    // DTOs

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordImportResult {
        private String fileName;
        private boolean success;
        private String error;
        private String extractedText;
        private int entriesFound;
        @Builder.Default
        private List<ParsedScheduleEntry> scheduleEntries = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedScheduleEntry {
        private String rawText;
        private DayOfWeek dayOfWeek;
        private Integer periodNumber;
        private LocalTime startTime;
        private LocalTime endTime;
        private String courseName;
        private String teacherName;
        private String roomNumber;
        private Long matchedCourseId;
        private Long matchedTeacherId;
        private Long matchedRoomId;
    }
}
