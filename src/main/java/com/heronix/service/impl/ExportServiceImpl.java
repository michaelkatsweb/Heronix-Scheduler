package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.ExportFormat;
import com.heronix.repository.RoomRepository;
import com.heronix.repository.ScheduleRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.ExportService;
import com.heronix.util.DateTimeUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {
    
    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    
    @Override
    @Transactional(readOnly = true)
    public byte[] exportSchedule(Long scheduleId, ExportFormat format) throws IOException {
        // Fetch schedule with slots eagerly loaded to avoid LazyInitializationException
        Schedule schedule = scheduleRepository.findByIdWithSlots(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        return switch (format) {
            case PDF -> exportToPDF(schedule);
            case EXCEL -> exportToExcel(schedule);
            case CSV -> exportToCSV(schedule);
            case ICAL -> exportToICal(schedule);
            case HTML -> exportToHTML(schedule);
            case JSON -> exportToJSON(schedule);
        };
    }
    
    @Override
    public byte[] exportToPDF(Schedule schedule) throws IOException {
        try {
            Document document = new Document(PageSize.A4.rotate()); // Landscape for better table fit
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();

            // Title
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph(schedule.getName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Metadata
            com.itextpdf.text.Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph metadata = new Paragraph(
                String.format("Generated: %s | Period: %s | Status: %s",
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    schedule.getPeriod() != null ? schedule.getPeriod() : "N/A",
                    schedule.getStatus() != null ? schedule.getStatus() : "N/A"
                ),
                metaFont
            );
            metadata.setAlignment(Element.ALIGN_CENTER);
            metadata.setSpacingAfter(15);
            document.add(metadata);

            // Group slots by day of week
            // ✅ NULL SAFE: Filter null slots and null dayOfWeek before grouping
            Map<DayOfWeek, java.util.List<ScheduleSlot>> slotsByDay = schedule.getSlots().stream()
                .filter(Objects::nonNull)
                .filter(slot -> slot.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek));

            // Sort days Monday to Friday
            java.util.List<DayOfWeek> sortedDays = Arrays.asList(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            );

            // Create weekly schedule table
            PdfPTable table = new PdfPTable(6); // Time + 5 days
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            // Set column widths
            float[] columnWidths = {1.5f, 2f, 2f, 2f, 2f, 2f};
            table.setWidths(columnWidths);

            // Header row
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
            BaseColor headerColor = new BaseColor(52, 73, 94); // Dark blue

            addHeaderCell(table, "Time", headerFont, headerColor);
            for (DayOfWeek day : sortedDays) {
                addHeaderCell(table, day.toString(), headerFont, headerColor);
            }

            // Get all unique time slots, sorted by start time
            // ✅ NULL SAFE: Filter null slots and null start times
            Set<LocalTime> allTimes = schedule.getSlots().stream()
                .filter(Objects::nonNull)
                .filter(slot -> slot.getStartTime() != null)
                .map(ScheduleSlot::getStartTime)
                .collect(Collectors.toCollection(TreeSet::new));

            // Data rows - one row per time slot
            com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            com.itextpdf.text.Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            for (LocalTime time : allTimes) {
                // Time column
                PdfPCell timeCell = new PdfPCell(new Phrase(
                    DateTimeUtil.formatTimeForDisplay(time), boldCellFont
                ));
                timeCell.setBackgroundColor(new BaseColor(236, 240, 241));
                timeCell.setPadding(8);
                timeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(timeCell);

                // Day columns
                for (DayOfWeek day : sortedDays) {
                    java.util.List<ScheduleSlot> daySlots = slotsByDay.getOrDefault(day, Collections.emptyList());

                    // Find slot for this time
                    // ✅ NULL SAFE: Filter null slots before checking start time
                    Optional<ScheduleSlot> slotOpt = daySlots.stream()
                        .filter(Objects::nonNull)
                        .filter(s -> s.getStartTime() != null && s.getStartTime().equals(time))
                        .findFirst();

                    if (slotOpt.isPresent()) {
                        ScheduleSlot slot = slotOpt.get();
                        PdfPCell cell = createScheduleSlotCell(slot, cellFont, boldCellFont);
                        table.addCell(cell);
                    } else {
                        // Empty cell
                        PdfPCell emptyCell = new PdfPCell(new Phrase("", cellFont));
                        emptyCell.setBackgroundColor(BaseColor.WHITE);
                        emptyCell.setPadding(8);
                        emptyCell.setMinimumHeight(50);
                        table.addCell(emptyCell);
                    }
                }
            }

            document.add(table);

            // Statistics footer
            document.add(new Paragraph("\n"));
            com.itextpdf.text.Font statsFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);

            int totalSlots = schedule.getSlots().size();
            // ✅ NULL SAFE: Filter null slots before counting assignments and conflicts
            long assignedSlots = schedule.getSlots().stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getTeacher() != null && s.getRoom() != null)
                .count();
            long conflictSlots = schedule.getSlots().stream()
                .filter(Objects::nonNull)
                .filter(s -> Boolean.TRUE.equals(s.getHasConflict()))
                .count();

            Paragraph stats = new Paragraph(
                String.format("Schedule Statistics: Total Slots: %d | Assigned: %d (%.1f%%) | Conflicts: %d | Quality Score: %.2f%%",
                    totalSlots, assignedSlots,
                    (totalSlots > 0 ? (assignedSlots * 100.0 / totalSlots) : 0),
                    conflictSlots,
                    schedule.getQualityScore() != null ? schedule.getQualityScore() : 0.0
                ),
                statsFont
            );
            stats.setAlignment(Element.ALIGN_CENTER);
            document.add(stats);

            document.close();

            log.info("PDF exported successfully for schedule: {}", schedule.getName());
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Failed to generate PDF", e);
            throw new IOException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to add header cell to PDF table
     */
    private void addHeaderCell(PdfPTable table, String text, com.itextpdf.text.Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /**
     * Helper method to create a schedule slot cell for PDF
     */
    private PdfPCell createScheduleSlotCell(ScheduleSlot slot, com.itextpdf.text.Font normalFont, com.itextpdf.text.Font boldFont) {
        // Create content with multiple lines
        Paragraph content = new Paragraph();

        // Course name (bold)
        // ✅ NULL SAFE: Check course and course code exist before adding
        if (slot.getCourse() != null && slot.getCourse().getCourseCode() != null) {
            Chunk courseChunk = new Chunk(slot.getCourse().getCourseCode() + "\n", boldFont);
            content.add(courseChunk);
        }

        // Teacher name
        // ✅ NULL SAFE: Check teacher and name exist before adding
        if (slot.getTeacher() != null && slot.getTeacher().getName() != null) {
            Chunk teacherChunk = new Chunk(slot.getTeacher().getName() + "\n", normalFont);
            content.add(teacherChunk);
        }

        // Room number
        // ✅ NULL SAFE: Check room and room number exist before adding
        if (slot.getRoom() != null && slot.getRoom().getRoomNumber() != null) {
            Chunk roomChunk = new Chunk("Room: " + slot.getRoom().getRoomNumber(), normalFont);
            content.add(roomChunk);
        }

        PdfPCell cell = new PdfPCell(content);
        cell.setPadding(8);
        cell.setMinimumHeight(50);
        cell.setVerticalAlignment(Element.ALIGN_TOP);

        // Color coding based on status
        if (Boolean.TRUE.equals(slot.getHasConflict())) {
            cell.setBackgroundColor(new BaseColor(255, 204, 203)); // Light red for conflicts
        } else if (slot.getTeacher() == null || slot.getRoom() == null) {
            cell.setBackgroundColor(new BaseColor(255, 243, 205)); // Light yellow for unassigned
        } else {
            cell.setBackgroundColor(new BaseColor(227, 242, 253)); // Light blue for normal
        }

        return cell;
    }
    
    @Override
    public byte[] exportToExcel(Schedule schedule) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(schedule.getName());
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Day", "Start Time", "End Time", "Course", "Teacher", "Room"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                
                CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }
            
            int rowNum = 1;
            for (ScheduleSlot slot : schedule.getSlots()) {
                // ✅ NULL SAFE: Check slot and timeSlot exist before processing
                if (slot == null || slot.getTimeSlot() == null) continue;

                Row row = sheet.createRow(rowNum++);

                TimeSlot timeSlot = slot.getTimeSlot();
                row.createCell(0).setCellValue(timeSlot.getDayOfWeek() != null ? timeSlot.getDayOfWeek().toString() : "");
                row.createCell(1).setCellValue(timeSlot.getStartTime() != null ? DateTimeUtil.formatTimeForDisplay(timeSlot.getStartTime()) : "");
                row.createCell(2).setCellValue(timeSlot.getEndTime() != null ? DateTimeUtil.formatTimeForDisplay(timeSlot.getEndTime()) : "");
                row.createCell(3).setCellValue(slot.getCourse() != null && slot.getCourse().getCourseName() != null ? slot.getCourse().getCourseName() : "");
                row.createCell(4).setCellValue(slot.getTeacher() != null && slot.getTeacher().getName() != null ? slot.getTeacher().getName() : "");
                row.createCell(5).setCellValue(slot.getRoom() != null && slot.getRoom().getRoomNumber() != null ? slot.getRoom().getRoomNumber() : "");
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
    
    @Override
    public byte[] exportToCSV(Schedule schedule) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Day,Start Time,End Time,Course,Teacher,Room\n");
        
        for (ScheduleSlot slot : schedule.getSlots()) {
            // ✅ NULL SAFE: Check slot and timeSlot exist before processing
            if (slot == null || slot.getTimeSlot() == null) continue;

            TimeSlot timeSlot = slot.getTimeSlot();
            csv.append(timeSlot.getDayOfWeek() != null ? timeSlot.getDayOfWeek() : "").append(",");
            csv.append(timeSlot.getStartTime() != null ? DateTimeUtil.formatTimeForDisplay(timeSlot.getStartTime()) : "").append(",");
            csv.append(timeSlot.getEndTime() != null ? DateTimeUtil.formatTimeForDisplay(timeSlot.getEndTime()) : "").append(",");
            csv.append(slot.getCourse() != null && slot.getCourse().getCourseName() != null ? slot.getCourse().getCourseName() : "").append(",");
            csv.append(slot.getTeacher() != null && slot.getTeacher().getName() != null ? slot.getTeacher().getName() : "").append(",");
            csv.append(slot.getRoom() != null && slot.getRoom().getRoomNumber() != null ? slot.getRoom().getRoomNumber() : "").append("\n");
        }
        
        return csv.toString().getBytes();
    }
    
    @Override
    public byte[] exportToICal(Schedule schedule) throws IOException {
        StringBuilder ical = new StringBuilder();
        
        ical.append("BEGIN:VCALENDAR\n");
        ical.append("VERSION:2.0\n");
        ical.append("PRODID:-//Heronix Scheduling System//Schedule Export//EN\n");
        ical.append("CALSCALE:GREGORIAN\n");
        ical.append("METHOD:PUBLISH\n");
        ical.append("X-WR-CALNAME:").append(schedule.getName()).append("\n");
        ical.append("X-WR-TIMEZONE:America/New_York\n");
        
        for (ScheduleSlot slot : schedule.getSlots()) {
            // ✅ NULL SAFE: Check slot exists before processing
            if (slot == null) continue;

            ical.append("BEGIN:VEVENT\n");
            ical.append("UID:").append(slot.getId() != null ? slot.getId() : 0).append("@eduscheduler.com\n");
            ical.append("SUMMARY:").append(slot.getCourse() != null && slot.getCourse().getCourseName() != null ? slot.getCourse().getCourseName() : "Class").append("\n");

            if (slot.getRoom() != null && slot.getRoom().getRoomNumber() != null) {
                ical.append("LOCATION:").append(slot.getRoom().getRoomNumber()).append("\n");
            }

            if (slot.getTeacher() != null && slot.getTeacher().getName() != null) {
                ical.append("DESCRIPTION:Teacher: ").append(slot.getTeacher().getName()).append("\n");
            }

            ical.append("END:VEVENT\n");
        }
        
        ical.append("END:VCALENDAR\n");
        return ical.toString().getBytes();
    }
    
    @Override
    public byte[] exportToHTML(Schedule schedule) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<title>").append(schedule.getName()).append("</title>\n</head>\n<body>\n");
        html.append("<h1>").append(schedule.getName()).append("</h1>\n");
        html.append("<table border='1'>\n<tr><th>Day</th><th>Time</th><th>Course</th><th>Teacher</th><th>Room</th></tr>\n");
        
        for (ScheduleSlot slot : schedule.getSlots()) {
            // ✅ NULL SAFE: Check slot and timeSlot exist before processing
            if (slot == null || slot.getTimeSlot() == null) continue;

            TimeSlot timeSlot = slot.getTimeSlot();
            html.append("<tr>");
            html.append("<td>").append(timeSlot.getDayOfWeek() != null ? timeSlot.getDayOfWeek() : "").append("</td>");
            html.append("<td>").append(timeSlot.getStartTime() != null ? DateTimeUtil.formatTimeForDisplay(timeSlot.getStartTime()) : "").append("</td>");
            html.append("<td>").append(slot.getCourse() != null && slot.getCourse().getCourseName() != null ? slot.getCourse().getCourseName() : "").append("</td>");
            html.append("<td>").append(slot.getTeacher() != null && slot.getTeacher().getName() != null ? slot.getTeacher().getName() : "").append("</td>");
            html.append("<td>").append(slot.getRoom() != null && slot.getRoom().getRoomNumber() != null ? slot.getRoom().getRoomNumber() : "").append("</td>");
            html.append("</tr>\n");
        }
        
        html.append("</table>\n</body>\n</html>");
        return html.toString().getBytes();
    }
    
    @Override
    public byte[] exportToJSON(Schedule schedule) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\"schedule\":\"").append(schedule.getName()).append("\",\"slots\":[");
        
        boolean first = true;
        for (ScheduleSlot slot : schedule.getSlots()) {
            // ✅ NULL SAFE: Check slot and timeSlot exist before processing
            if (slot == null || slot.getTimeSlot() == null) continue;

            if (!first) json.append(",");
            first = false;

            TimeSlot timeSlot = slot.getTimeSlot();
            json.append("{");
            json.append("\"day\":\"").append(timeSlot.getDayOfWeek() != null ? timeSlot.getDayOfWeek() : "").append("\",");
            json.append("\"startTime\":\"").append(timeSlot.getStartTime() != null ? DateTimeUtil.formatTimeForDisplay(timeSlot.getStartTime()) : "").append("\",");
            json.append("\"endTime\":\"").append(timeSlot.getEndTime() != null ? DateTimeUtil.formatTimeForDisplay(timeSlot.getEndTime()) : "").append("\",");
            json.append("\"course\":\"").append(slot.getCourse() != null && slot.getCourse().getCourseName() != null ? slot.getCourse().getCourseName() : "").append("\",");
            json.append("\"teacher\":\"").append(slot.getTeacher() != null && slot.getTeacher().getName() != null ? slot.getTeacher().getName() : "").append("\",");
            json.append("\"room\":\"").append(slot.getRoom() != null && slot.getRoom().getRoomNumber() != null ? slot.getRoom().getRoomNumber() : "").append("\"");
            json.append("}");
        }
        
        json.append("]}");
        return json.toString().getBytes();
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] exportTeacherSchedule(Long teacherId, ExportFormat format) throws IOException {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("Teacher schedule export for " + teacher.getName()).getBytes());
        return baos.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRoomSchedule(Long roomId, ExportFormat format) throws IOException {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("Room schedule export for " + room.getRoomNumber()).getBytes());
        return baos.toByteArray();
    }

    // ========================================================================
    // GENERIC ENTITY EXPORTS
    // ========================================================================

    @Override
    public byte[] exportTeachersToExcel(java.util.List<Teacher> teachers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Teachers");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Employee ID", "Name", "Email", "Subject", "Phone", "Max Hours/Week", "Active"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            // ✅ NULL SAFE: Filter null teachers before processing
            int rowNum = 1;
            for (Teacher teacher : teachers) {
                if (teacher == null) continue;

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(teacher.getId() != null ? teacher.getId() : 0);
                row.createCell(1).setCellValue(teacher.getEmployeeId() != null ? teacher.getEmployeeId() : "");
                row.createCell(2).setCellValue(teacher.getName() != null ? teacher.getName() : "");
                row.createCell(3).setCellValue(teacher.getEmail() != null ? teacher.getEmail() : "");
                row.createCell(4).setCellValue(teacher.getDepartment() != null ? teacher.getDepartment() : "");
                row.createCell(5).setCellValue(teacher.getPhoneNumber() != null ? teacher.getPhoneNumber() : "");
                row.createCell(6).setCellValue(teacher.getMaxHoursPerWeek() != null ? teacher.getMaxHoursPerWeek() : 0);
                row.createCell(7).setCellValue(Boolean.TRUE.equals(teacher.getActive()));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            log.info("Exported {} teachers to Excel", teachers.size());
            return baos.toByteArray();
        }
    }

    @Override
    public byte[] exportTeachersToCSV(java.util.List<Teacher> teachers) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Employee ID,Name,Email,Subject,Phone,Max Hours/Week,Active\n");

        // ✅ NULL SAFE: Filter null teachers before processing
        for (Teacher teacher : teachers) {
            if (teacher == null) continue;

            csv.append(teacher.getId() != null ? teacher.getId() : "").append(",");
            csv.append(escapeCsv(teacher.getEmployeeId())).append(",");
            csv.append(escapeCsv(teacher.getName())).append(",");
            csv.append(escapeCsv(teacher.getEmail())).append(",");
            csv.append(escapeCsv(teacher.getDepartment())).append(",");
            csv.append(escapeCsv(teacher.getPhoneNumber())).append(",");
            csv.append(teacher.getMaxHoursPerWeek() != null ? teacher.getMaxHoursPerWeek() : "").append(",");
            csv.append(Boolean.TRUE.equals(teacher.getActive())).append("\n");
        }

        log.info("Exported {} teachers to CSV", teachers.size());
        return csv.toString().getBytes();
    }

    @Override
    public byte[] exportCoursesToExcel(java.util.List<Course> courses) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Courses");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Course Code", "Course Name", "Duration (min)", "Subject", "Level", "Max Students", "Requires Lab", "Active"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Course course : courses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(course.getId() != null ? course.getId() : 0);
                row.createCell(1).setCellValue(course.getCourseCode() != null ? course.getCourseCode() : "");
                row.createCell(2).setCellValue(course.getCourseName() != null ? course.getCourseName() : "");
                row.createCell(3).setCellValue(course.getDurationMinutes() != null ? course.getDurationMinutes() : 0);
                row.createCell(4).setCellValue(course.getSubject() != null ? course.getSubject() : "");
                row.createCell(5).setCellValue(course.getLevel() != null ? course.getLevel().toString() : "");
                row.createCell(6).setCellValue(course.getMaxStudents() != null ? course.getMaxStudents() : 0);
                row.createCell(7).setCellValue(course.isRequiresLab());
                row.createCell(8).setCellValue(course.getActive());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            log.info("Exported {} courses to Excel", courses.size());
            return baos.toByteArray();
        }
    }

    @Override
    public byte[] exportCoursesToCSV(java.util.List<Course> courses) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Course Code,Course Name,Duration (min),Subject,Level,Max Students,Requires Lab,Active\n");

        for (Course course : courses) {
            csv.append(course.getId() != null ? course.getId() : "").append(",");
            csv.append(escapeCsv(course.getCourseCode())).append(",");
            csv.append(escapeCsv(course.getCourseName())).append(",");
            csv.append(course.getDurationMinutes() != null ? course.getDurationMinutes() : "").append(",");
            csv.append(escapeCsv(course.getSubject())).append(",");
            csv.append(escapeCsv(course.getLevel() != null ? course.getLevel().toString() : "")).append(",");
            csv.append(course.getMaxStudents() != null ? course.getMaxStudents() : "").append(",");
            csv.append(course.isRequiresLab()).append(",");
            csv.append(course.getActive()).append("\n");
        }

        log.info("Exported {} courses to CSV", courses.size());
        return csv.toString().getBytes();
    }

    @Override
    public byte[] exportRoomsToExcel(java.util.List<Room> rooms) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rooms");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Room Number", "Building", "Capacity", "Room Type", "Equipment", "Active"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Room room : rooms) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(room.getId() != null ? room.getId() : 0);
                row.createCell(1).setCellValue(room.getRoomNumber() != null ? room.getRoomNumber() : "");
                row.createCell(2).setCellValue(room.getBuilding() != null ? room.getBuilding() : "");
                row.createCell(3).setCellValue(room.getCapacity() != null ? room.getCapacity() : 0);
                row.createCell(4).setCellValue(room.getRoomType() != null ? room.getRoomType().toString() : "");
                row.createCell(5).setCellValue(room.getEquipment() != null ? room.getEquipment() : "");
                row.createCell(6).setCellValue(room.getActive() != null ? room.getActive() : false);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            log.info("Exported {} rooms to Excel", rooms.size());
            return baos.toByteArray();
        }
    }

    @Override
    public byte[] exportRoomsToCSV(java.util.List<Room> rooms) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Room Number,Building,Capacity,Room Type,Equipment,Active\n");

        for (Room room : rooms) {
            csv.append(room.getId() != null ? room.getId() : "").append(",");
            csv.append(escapeCsv(room.getRoomNumber())).append(",");
            csv.append(escapeCsv(room.getBuilding())).append(",");
            csv.append(room.getCapacity() != null ? room.getCapacity() : "").append(",");
            csv.append(escapeCsv(room.getRoomType() != null ? room.getRoomType().toString() : "")).append(",");
            csv.append(escapeCsv(room.getEquipment())).append(",");
            csv.append(room.getActive() != null ? room.getActive() : false).append("\n");
        }

        log.info("Exported {} rooms to CSV", rooms.size());
        return csv.toString().getBytes();
    }

    @Override
    public byte[] exportEventsToICal(java.util.List<Event> events) throws IOException {
        StringBuilder ical = new StringBuilder();

        ical.append("BEGIN:VCALENDAR\n");
        ical.append("VERSION:2.0\n");
        ical.append("PRODID:-//Heronix Scheduling System//Events Export//EN\n");
        ical.append("CALSCALE:GREGORIAN\n");
        ical.append("METHOD:PUBLISH\n");
        ical.append("X-WR-CALNAME:Heronix Scheduler Events\n");
        ical.append("X-WR-TIMEZONE:America/New_York\n");

        // ✅ NULL SAFE: Filter null events before processing
        for (Event event : events) {
            if (event == null) continue;

            ical.append("BEGIN:VEVENT\n");
            ical.append("UID:").append(event.getId() != null ? event.getId() : 0).append("@eduscheduler.com\n");
            ical.append("SUMMARY:").append(event.getName() != null ? event.getName() : "Event").append("\n");

            if (event.getStartDateTime() != null) {
                ical.append("DTSTART:").append(formatDateTimeForICal(event.getStartDateTime())).append("\n");
            }

            if (event.getEndDateTime() != null) {
                ical.append("DTEND:").append(formatDateTimeForICal(event.getEndDateTime())).append("\n");
            }

            if (event.getDescription() != null) {
                ical.append("DESCRIPTION:").append(event.getDescription().replace("\n", "\\n")).append("\n");
            }

            if (event.getEventType() != null) {
                ical.append("CATEGORIES:").append(event.getEventType().toString()).append("\n");
            }

            ical.append("END:VEVENT\n");
        }

        ical.append("END:VCALENDAR\n");
        log.info("Exported {} events to iCal", events.size());
        return ical.toString().getBytes();
    }

    @Override
    public byte[] exportEventsToCSV(java.util.List<Event> events) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Event Type,Start Date/Time,End Date/Time,All Day,Blocks Scheduling,Description\n");

        for (Event event : events) {
            csv.append(event.getId() != null ? event.getId() : "").append(",");
            csv.append(escapeCsv(event.getName())).append(",");
            csv.append(escapeCsv(event.getEventType() != null ? event.getEventType().toString() : "")).append(",");
            csv.append(escapeCsv(event.getStartDateTime() != null ? event.getStartDateTime().toString() : "")).append(",");
            csv.append(escapeCsv(event.getEndDateTime() != null ? event.getEndDateTime().toString() : "")).append(",");
            csv.append(event.getAllDay() != null ? event.getAllDay() : false).append(",");
            csv.append(event.getBlocksScheduling() != null ? event.getBlocksScheduling() : false).append(",");
            csv.append(escapeCsv(event.getDescription())).append("\n");
        }

        log.info("Exported {} events to CSV", events.size());
        return csv.toString().getBytes();
    }

    @Override
    public byte[] exportStudentsToCSV(java.util.List<Student> students) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Student ID,First Name,Last Name,Email,Level,Active\n");

        for (Student student : students) {
            csv.append(student.getId() != null ? student.getId() : "").append(",");
            csv.append(escapeCsv(student.getStudentId())).append(",");
            csv.append(escapeCsv(student.getFirstName())).append(",");
            csv.append(escapeCsv(student.getLastName())).append(",");
            csv.append(escapeCsv(student.getEmail())).append(",");
            csv.append(escapeCsv(student.getGradeLevel())).append(",");
            csv.append(student.getActive()).append("\n");
        }

        log.info("Exported {} students to CSV", students.size());
        return csv.toString().getBytes();
    }

    @Override
    public byte[] exportStudentsToExcel(java.util.List<Student> students) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Student ID", "First Name", "Last Name", "Email", "Level", "Active"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Student student : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(student.getId() != null ? student.getId() : 0);
                row.createCell(1).setCellValue(student.getStudentId() != null ? student.getStudentId() : "");
                row.createCell(2).setCellValue(student.getFirstName() != null ? student.getFirstName() : "");
                row.createCell(3).setCellValue(student.getLastName() != null ? student.getLastName() : "");
                row.createCell(4).setCellValue(student.getEmail() != null ? student.getEmail() : "");
                row.createCell(5).setCellValue(student.getGradeLevel() != null ? student.getGradeLevel() : "");
                row.createCell(6).setCellValue(student.getActive());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            log.info("Exported {} students to Excel", students.size());
            return baos.toByteArray();
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatDateTimeForICal(java.time.LocalDateTime dateTime) {
        // Format: YYYYMMDDTHHMMSS
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
    }
}