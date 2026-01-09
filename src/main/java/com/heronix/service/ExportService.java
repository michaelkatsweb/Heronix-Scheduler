package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.enums.ExportFormat;

import java.io.IOException;
import java.util.List;

public interface ExportService {

    byte[] exportSchedule(Long scheduleId, ExportFormat format) throws IOException;

    byte[] exportToPDF(Schedule schedule) throws IOException;

    byte[] exportToExcel(Schedule schedule) throws IOException;

    byte[] exportToCSV(Schedule schedule) throws IOException;

    byte[] exportToICal(Schedule schedule) throws IOException;

    byte[] exportToHTML(Schedule schedule) throws IOException;

    byte[] exportToJSON(Schedule schedule) throws IOException;

    byte[] exportTeacherSchedule(Long teacherId, ExportFormat format) throws IOException;

    byte[] exportRoomSchedule(Long roomId, ExportFormat format) throws IOException;

    // Generic entity exports
    byte[] exportTeachersToExcel(List<Teacher> teachers) throws IOException;

    byte[] exportTeachersToCSV(List<Teacher> teachers) throws IOException;

    byte[] exportCoursesToExcel(List<Course> courses) throws IOException;

    byte[] exportCoursesToCSV(List<Course> courses) throws IOException;

    byte[] exportRoomsToExcel(List<Room> rooms) throws IOException;

    byte[] exportRoomsToCSV(List<Room> rooms) throws IOException;

    byte[] exportEventsToICal(List<Event> events) throws IOException;

    byte[] exportEventsToCSV(List<Event> events) throws IOException;

    byte[] exportStudentsToCSV(List<Student> students) throws IOException;

    byte[] exportStudentsToExcel(List<Student> students) throws IOException;
}