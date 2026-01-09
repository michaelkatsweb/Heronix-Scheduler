package com.heronix.service.impl;

import com.heronix.dto.SISExportResult;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseSectionRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.SISExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of SIS Export Service
 * Handles exports to PowerSchool, Skyward, Infinite Campus, and generic formats
 *
 * Location: src/main/java/com/eduscheduler/service/impl/SISExportServiceImpl.java
 */
@Service
public class SISExportServiceImpl implements SISExportService {

    private static final Logger logger = LoggerFactory.getLogger(SISExportServiceImpl.class);

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private static final String DEFAULT_EXPORT_DIR = "exports";

    // ========== Main Export Operations ==========

    @Override
    public SISExportResult exportMasterSchedule(ExportFormat format, String outputPath) {
        // ✅ NULL SAFE: Validate format parameter
        if (format == null) {
            SISExportResult result = new SISExportResult();
            result.addError("Export format cannot be null");
            result.setSuccess(false);
            return result;
        }

        logger.info("Exporting master schedule to format: {}", format);

        SISExportResult result = new SISExportResult();
        result.setExportType(format.getDisplayName());
        result.setExportTime(LocalDateTime.now());

        try {
            List<CourseSection> sections = courseSectionRepository.findAll();

            switch (format) {
                case POWERSCHOOL:
                    return exportPowerSchoolSections(outputPath);
                case SKYWARD:
                    return exportSkywardSections(outputPath);
                case INFINITE_CAMPUS:
                    return exportInfiniteCampusSections(outputPath);
                case GENERIC_CSV:
                    return exportGenericCSV(sections, outputPath);
                case EXCEL:
                    return exportToExcel(sections, outputPath);
                default:
                    result.addError("Unsupported export format: " + format);
                    result.setSuccess(false);
            }
        } catch (Exception e) {
            logger.error("Export failed", e);
            result.addError("Export failed: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    @Override
    public SISExportResult exportStudentSchedules(ExportFormat format, String outputPath, List<Long> studentIds) {
        // ✅ NULL SAFE: Validate parameters
        if (format == null || studentIds == null) {
            SISExportResult result = new SISExportResult();
            result.addError("Format and student IDs cannot be null");
            result.setSuccess(false);
            return result;
        }

        logger.info("Exporting schedules for {} students", studentIds.size());

        SISExportResult result = new SISExportResult();
        result.setExportType(format.getDisplayName() + " - Student Schedules");
        result.setExportTime(LocalDateTime.now());

        try {
            List<Student> students = studentRepository.findAllById(studentIds);

            switch (format) {
                case POWERSCHOOL:
                    return exportPowerSchoolStudentSchedules(outputPath);
                case SKYWARD:
                    return exportSkywardStudentSchedules(outputPath);
                case INFINITE_CAMPUS:
                    return exportInfiniteCampusRoster(outputPath);
                case GENERIC_CSV:
                case EXCEL:
                    return exportStudentSchedulesToCSV(students, outputPath);
                default:
                    result.addError("Unsupported format");
                    result.setSuccess(false);
            }
        } catch (Exception e) {
            logger.error("Student schedule export failed", e);
            result.addError("Export failed: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    @Override
    public SISExportResult exportAllStudentSchedules(ExportFormat format, String outputPath) {
        // ✅ NULL SAFE: Filter null students and null IDs before mapping
        List<Long> allStudentIds = studentRepository.findAll().stream()
                .filter(student -> student != null && student.getId() != null)
                .map(Student::getId)
                .collect(Collectors.toList());
        return exportStudentSchedules(format, outputPath, allStudentIds);
    }

    @Override
    public SISExportResult exportTeacherSchedules(ExportFormat format, String outputPath, List<Long> teacherIds) {
        // ✅ NULL SAFE: Validate teacherIds parameter
        if (teacherIds == null) {
            SISExportResult result = new SISExportResult();
            result.addError("Teacher IDs cannot be null");
            result.setSuccess(false);
            return result;
        }

        logger.info("Exporting teacher schedules for {} teachers", teacherIds.size());

        SISExportResult result = new SISExportResult();
        result.setExportType("Teacher Schedules");
        result.setExportTime(LocalDateTime.now());

        try {
            List<Teacher> teachers = teacherRepository.findAllById(teacherIds);
            return exportTeacherSchedulesToCSV(teachers, outputPath);
        } catch (Exception e) {
            logger.error("Teacher schedule export failed", e);
            result.addError("Export failed: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    @Override
    public SISExportResult exportCourseSections(ExportFormat format, String outputPath) {
        return exportMasterSchedule(format, outputPath);
    }

    @Override
    public SISExportResult exportEnrollments(ExportFormat format, String outputPath) {
        logger.info("Exporting enrollments");

        SISExportResult result = new SISExportResult();
        result.setExportType("Enrollments");
        result.setExportTime(LocalDateTime.now());

        try {
            return exportEnrollmentsToCSV(outputPath);
        } catch (Exception e) {
            logger.error("Enrollment export failed", e);
            result.addError("Export failed: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    // ========== PowerSchool Exports ==========

    @Override
    public SISExportResult exportToPowerSchool(ExportType exportType, String outputPath) {
        switch (exportType) {
            case STUDENT_SCHEDULES:
                return exportPowerSchoolStudentSchedules(outputPath);
            case COURSE_SECTIONS:
                return exportPowerSchoolSections(outputPath);
            default:
                SISExportResult result = new SISExportResult();
                result.addError("Unsupported PowerSchool export type: " + exportType);
                result.setSuccess(false);
                return result;
        }
    }

    @Override
    public SISExportResult exportPowerSchoolStudentSchedules(String outputPath) {
        logger.info("Exporting PowerSchool student schedules");

        SISExportResult result = new SISExportResult();
        result.setExportType("PowerSchool Student Schedules");
        result.setExportTime(LocalDateTime.now());

        try {
            String filename = outputPath != null ? outputPath :
                    generateExportFilename(ExportFormat.POWERSCHOOL, ExportType.STUDENT_SCHEDULES);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                // PowerSchool format: Student_Number,Course_Number,Section_Number,Expression
                writer.println("Student_Number,Course_Number,Section_Number,Expression");

                List<Student> students = studentRepository.findAll();
                int recordCount = 0;

                for (Student student : students) {
                    // ✅ NULL SAFE: Skip null students
                    if (student == null) continue;

                    String studentNumber = student.getStudentId() != null ? student.getStudentId() : String.valueOf(student.getId());

                    // Get student's enrolled courses
                    // ✅ NULL SAFE: Check for null enrolled courses list
                    List<Course> enrolledCourses = student.getEnrolledCourses();
                    if (enrolledCourses == null) continue;

                    for (Course course : enrolledCourses) {
                        // ✅ NULL SAFE: Skip null courses and null course IDs
                        if (course == null || course.getId() == null) continue;
                        // Find section for this course
                        // ✅ NULL SAFE: Filter null sections and check course/ID before comparison
                        Optional<CourseSection> sectionOpt = courseSectionRepository.findAll().stream()
                                .filter(s -> s != null && s.getCourse() != null && s.getCourse().getId() != null &&
                                           s.getCourse().getId().equals(course.getId()))
                                .filter(s -> s.getCurrentEnrollment() != null && s.getCurrentEnrollment() > 0)
                                .findFirst();

                        if (sectionOpt.isPresent()) {
                            CourseSection section = sectionOpt.get();
                            String courseNumber = course.getCourseCode() != null ? course.getCourseCode() : String.valueOf(course.getId());
                            String sectionNumber = section.getSectionNumber() != null ? section.getSectionNumber() : "1";
                            String expression = section.getAssignedPeriod() != null ? String.valueOf(section.getAssignedPeriod()) : "1";

                            writer.printf("%s,%s,%s,%s%n",
                                    escapeCSV(studentNumber),
                                    escapeCSV(courseNumber),
                                    escapeCSV(sectionNumber),
                                    expression);
                            recordCount++;
                        }
                    }
                }

                result.setFileName(filename);
                result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
                result.setRecordsExported(recordCount);
                result.setSuccess(true);

                logger.info("PowerSchool student schedules exported: {} records", recordCount);
            }
        } catch (IOException e) {
            logger.error("PowerSchool export failed", e);
            result.addError("File write error: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    @Override
    public SISExportResult exportPowerSchoolSections(String outputPath) {
        logger.info("Exporting PowerSchool sections");

        SISExportResult result = new SISExportResult();
        result.setExportType("PowerSchool Sections");
        result.setExportTime(LocalDateTime.now());

        try {
            String filename = outputPath != null ? outputPath :
                    generateExportFilename(ExportFormat.POWERSCHOOL, ExportType.COURSE_SECTIONS);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                // PowerSchool section format
                writer.println("Course_Number,Section_Number,Teacher_Number,Room,Expression,Max_Enrollment");

                List<CourseSection> sections = courseSectionRepository.findAll();
                int recordCount = 0;

                for (CourseSection section : sections) {
                    String courseNumber = section.getCourse().getCourseCode() != null ?
                            section.getCourse().getCourseCode() : String.valueOf(section.getCourse().getId());
                    String sectionNumber = section.getSectionNumber() != null ? section.getSectionNumber() : "1";
                    String teacherNumber = section.getAssignedTeacher() != null ?
                            String.valueOf(section.getAssignedTeacher().getId()) : "";
                    String room = section.getAssignedRoom() != null ?
                            section.getAssignedRoom().getRoomNumber() : "";
                    String expression = section.getAssignedPeriod() != null ?
                            String.valueOf(section.getAssignedPeriod()) : "1";
                    String maxEnrollment = section.getMaxEnrollment() != null ?
                            String.valueOf(section.getMaxEnrollment()) : "30";

                    writer.printf("%s,%s,%s,%s,%s,%s%n",
                            escapeCSV(courseNumber),
                            escapeCSV(sectionNumber),
                            escapeCSV(teacherNumber),
                            escapeCSV(room),
                            expression,
                            maxEnrollment);
                    recordCount++;
                }

                result.setFileName(filename);
                result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
                result.setRecordsExported(recordCount);
                result.setSuccess(true);

                logger.info("PowerSchool sections exported: {} records", recordCount);
            }
        } catch (IOException e) {
            logger.error("PowerSchool section export failed", e);
            result.addError("File write error: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    // ========== Skyward Exports ==========

    @Override
    public SISExportResult exportToSkyward(ExportType exportType, String outputPath) {
        switch (exportType) {
            case STUDENT_SCHEDULES:
                return exportSkywardStudentSchedules(outputPath);
            case COURSE_SECTIONS:
                return exportSkywardSections(outputPath);
            default:
                SISExportResult result = new SISExportResult();
                result.addError("Unsupported Skyward export type: " + exportType);
                result.setSuccess(false);
                return result;
        }
    }

    @Override
    public SISExportResult exportSkywardStudentSchedules(String outputPath) {
        logger.info("Exporting Skyward student schedules");

        SISExportResult result = new SISExportResult();
        result.setExportType("Skyward Student Schedules");
        result.setExportTime(LocalDateTime.now());

        try {
            String filename = outputPath != null ? outputPath :
                    generateExportFilename(ExportFormat.SKYWARD, ExportType.STUDENT_SCHEDULES);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                // Skyward format: Tab-delimited
                writer.println("StudentID\tCourseID\tSectionID\tPeriod\tTeacherID\tRoom");

                List<Student> students = studentRepository.findAll();
                int recordCount = 0;

                for (Student student : students) {
                    // ✅ NULL SAFE: Skip null students
                    if (student == null) continue;

                    String studentId = student.getStudentId() != null ? student.getStudentId() : String.valueOf(student.getId());

                    // ✅ NULL SAFE: Check for null enrolled courses list
                    List<Course> enrolledCourses = student.getEnrolledCourses();
                    if (enrolledCourses == null) continue;

                    for (Course course : enrolledCourses) {
                        // ✅ NULL SAFE: Skip null courses and null course IDs
                        if (course == null || course.getId() == null) continue;

                        // ✅ NULL SAFE: Filter null sections and check course/ID before comparison
                        Optional<CourseSection> sectionOpt = courseSectionRepository.findAll().stream()
                                .filter(s -> s != null && s.getCourse() != null && s.getCourse().getId() != null &&
                                           s.getCourse().getId().equals(course.getId()))
                                .findFirst();

                        if (sectionOpt.isPresent()) {
                            CourseSection section = sectionOpt.get();
                            String courseId = course.getCourseCode() != null ? course.getCourseCode() : String.valueOf(course.getId());
                            String sectionId = section.getSectionNumber() != null ? section.getSectionNumber() : "1";
                            String period = section.getAssignedPeriod() != null ? String.valueOf(section.getAssignedPeriod()) : "1";
                            String teacherId = section.getAssignedTeacher() != null ?
                                    String.valueOf(section.getAssignedTeacher().getId()) : "";
                            String room = section.getAssignedRoom() != null ?
                                    section.getAssignedRoom().getRoomNumber() : "";

                            writer.printf("%s\t%s\t%s\t%s\t%s\t%s%n",
                                    studentId, courseId, sectionId, period, teacherId, room);
                            recordCount++;
                        }
                    }
                }

                result.setFileName(filename);
                result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
                result.setRecordsExported(recordCount);
                result.setSuccess(true);

                logger.info("Skyward student schedules exported: {} records", recordCount);
            }
        } catch (IOException e) {
            logger.error("Skyward export failed", e);
            result.addError("File write error: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    @Override
    public SISExportResult exportSkywardSections(String outputPath) {
        logger.info("Exporting Skyward sections");

        SISExportResult result = new SISExportResult();
        result.setExportType("Skyward Sections");
        result.setExportTime(LocalDateTime.now());

        try {
            String filename = outputPath != null ? outputPath :
                    generateExportFilename(ExportFormat.SKYWARD, ExportType.COURSE_SECTIONS);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("CourseID\tSectionID\tCourseName\tTeacherID\tRoom\tPeriod\tMaxStudents\tCurrentEnrollment");

                List<CourseSection> sections = courseSectionRepository.findAll();
                int recordCount = 0;

                for (CourseSection section : sections) {
                    // ✅ NULL SAFE: Skip null sections and sections with null courses
                    if (section == null || section.getCourse() == null) continue;

                    String courseId = section.getCourse().getCourseCode() != null ?
                            section.getCourse().getCourseCode() : String.valueOf(section.getCourse().getId());
                    String sectionId = section.getSectionNumber() != null ? section.getSectionNumber() : "1";
                    String courseName = section.getCourse().getCourseName();
                    String teacherId = section.getAssignedTeacher() != null ?
                            String.valueOf(section.getAssignedTeacher().getId()) : "";
                    String room = section.getAssignedRoom() != null ?
                            section.getAssignedRoom().getRoomNumber() : "";
                    String period = section.getAssignedPeriod() != null ?
                            String.valueOf(section.getAssignedPeriod()) : "1";
                    String maxStudents = section.getMaxEnrollment() != null ?
                            String.valueOf(section.getMaxEnrollment()) : "30";
                    String currentEnrollment = section.getCurrentEnrollment() != null ?
                            String.valueOf(section.getCurrentEnrollment()) : "0";

                    writer.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                            courseId, sectionId, courseName, teacherId, room, period, maxStudents, currentEnrollment);
                    recordCount++;
                }

                result.setFileName(filename);
                result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
                result.setRecordsExported(recordCount);
                result.setSuccess(true);

                logger.info("Skyward sections exported: {} records", recordCount);
            }
        } catch (IOException e) {
            logger.error("Skyward section export failed", e);
            result.addError("File write error: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    // ========== Infinite Campus Exports ==========

    @Override
    public SISExportResult exportToInfiniteCampus(ExportType exportType, String outputPath) {
        switch (exportType) {
            case STUDENT_SCHEDULES:
                return exportInfiniteCampusRoster(outputPath);
            case COURSE_SECTIONS:
                return exportInfiniteCampusSections(outputPath);
            default:
                SISExportResult result = new SISExportResult();
                result.addError("Unsupported Infinite Campus export type: " + exportType);
                result.setSuccess(false);
                return result;
        }
    }

    @Override
    public SISExportResult exportInfiniteCampusRoster(String outputPath) {
        logger.info("Exporting Infinite Campus roster");

        SISExportResult result = new SISExportResult();
        result.setExportType("Infinite Campus Roster");
        result.setExportTime(LocalDateTime.now());

        try {
            String filename = outputPath != null ? outputPath :
                    generateExportFilename(ExportFormat.INFINITE_CAMPUS, ExportType.STUDENT_SCHEDULES);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("StudentNumber,LastName,FirstName,CourseNumber,SectionNumber,Period,TeacherLastName,TeacherFirstName");

                List<Student> students = studentRepository.findAll();
                int recordCount = 0;

                for (Student student : students) {
                    // ✅ NULL SAFE: Skip null students
                    if (student == null) continue;

                    String studentNumber = student.getStudentId() != null ? student.getStudentId() : String.valueOf(student.getId());
                    String lastName = student.getLastName() != null ? student.getLastName() : "";
                    String firstName = student.getFirstName() != null ? student.getFirstName() : "";

                    // ✅ NULL SAFE: Check for null enrolled courses list
                    List<Course> enrolledCourses = student.getEnrolledCourses();
                    if (enrolledCourses == null) continue;

                    for (Course course : enrolledCourses) {
                        // ✅ NULL SAFE: Skip null courses and null course IDs
                        if (course == null || course.getId() == null) continue;

                        // ✅ NULL SAFE: Filter null sections and check course/ID before comparison
                        Optional<CourseSection> sectionOpt = courseSectionRepository.findAll().stream()
                                .filter(s -> s != null && s.getCourse() != null && s.getCourse().getId() != null &&
                                           s.getCourse().getId().equals(course.getId()))
                                .findFirst();

                        if (sectionOpt.isPresent()) {
                            CourseSection section = sectionOpt.get();
                            String courseNumber = course.getCourseCode() != null ? course.getCourseCode() : String.valueOf(course.getId());
                            String sectionNumber = section.getSectionNumber() != null ? section.getSectionNumber() : "1";
                            String period = section.getAssignedPeriod() != null ? String.valueOf(section.getAssignedPeriod()) : "1";
                            String teacherLast = section.getAssignedTeacher() != null ?
                                    section.getAssignedTeacher().getLastName() : "";
                            String teacherFirst = section.getAssignedTeacher() != null ?
                                    section.getAssignedTeacher().getFirstName() : "";

                            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                                    escapeCSV(studentNumber),
                                    escapeCSV(lastName),
                                    escapeCSV(firstName),
                                    escapeCSV(courseNumber),
                                    escapeCSV(sectionNumber),
                                    period,
                                    escapeCSV(teacherLast),
                                    escapeCSV(teacherFirst));
                            recordCount++;
                        }
                    }
                }

                result.setFileName(filename);
                result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
                result.setRecordsExported(recordCount);
                result.setSuccess(true);

                logger.info("Infinite Campus roster exported: {} records", recordCount);
            }
        } catch (IOException e) {
            logger.error("Infinite Campus export failed", e);
            result.addError("File write error: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    @Override
    public SISExportResult exportInfiniteCampusSections(String outputPath) {
        logger.info("Exporting Infinite Campus sections");

        SISExportResult result = new SISExportResult();
        result.setExportType("Infinite Campus Sections");
        result.setExportTime(LocalDateTime.now());

        try {
            String filename = outputPath != null ? outputPath :
                    generateExportFilename(ExportFormat.INFINITE_CAMPUS, ExportType.COURSE_SECTIONS);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("CourseNumber,SectionNumber,CourseName,Period,RoomNumber,TeacherLastName,TeacherFirstName,MaxSeats,Enrolled");

                List<CourseSection> sections = courseSectionRepository.findAll();
                int recordCount = 0;

                for (CourseSection section : sections) {
                    // ✅ NULL SAFE: Skip null sections and sections with null courses
                    if (section == null || section.getCourse() == null) continue;

                    String courseNumber = section.getCourse().getCourseCode() != null ?
                            section.getCourse().getCourseCode() : String.valueOf(section.getCourse().getId());
                    String sectionNumber = section.getSectionNumber() != null ? section.getSectionNumber() : "1";
                    String courseName = section.getCourse().getCourseName();
                    String period = section.getAssignedPeriod() != null ? String.valueOf(section.getAssignedPeriod()) : "1";
                    String roomNumber = section.getAssignedRoom() != null ? section.getAssignedRoom().getRoomNumber() : "";
                    String teacherLast = section.getAssignedTeacher() != null ? section.getAssignedTeacher().getLastName() : "";
                    String teacherFirst = section.getAssignedTeacher() != null ? section.getAssignedTeacher().getFirstName() : "";
                    String maxSeats = section.getMaxEnrollment() != null ? String.valueOf(section.getMaxEnrollment()) : "30";
                    String enrolled = section.getCurrentEnrollment() != null ? String.valueOf(section.getCurrentEnrollment()) : "0";

                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            escapeCSV(courseNumber),
                            escapeCSV(sectionNumber),
                            escapeCSV(courseName),
                            period,
                            escapeCSV(roomNumber),
                            escapeCSV(teacherLast),
                            escapeCSV(teacherFirst),
                            maxSeats,
                            enrolled);
                    recordCount++;
                }

                result.setFileName(filename);
                result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
                result.setRecordsExported(recordCount);
                result.setSuccess(true);

                logger.info("Infinite Campus sections exported: {} records", recordCount);
            }
        } catch (IOException e) {
            logger.error("Infinite Campus section export failed", e);
            result.addError("File write error: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    // ========== Generic/Helper Export Methods ==========

    private SISExportResult exportGenericCSV(List<CourseSection> sections, String outputPath) throws IOException {
        SISExportResult result = new SISExportResult();
        result.setExportType("Generic CSV");
        result.setExportTime(LocalDateTime.now());

        String filename = outputPath != null ? outputPath :
                generateExportFilename(ExportFormat.GENERIC_CSV, ExportType.COURSE_SECTIONS);

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Course Code,Course Name,Section,Teacher,Room,Period,Max Enrollment,Current Enrollment,Status");

            // ✅ NULL SAFE: Validate sections parameter
            if (sections == null) {
                sections = new ArrayList<>();
            }

            int recordCount = 0;
            for (CourseSection section : sections) {
                // ✅ NULL SAFE: Skip null sections and sections with null courses
                if (section == null || section.getCourse() == null) continue;
                String courseCode = section.getCourse().getCourseCode();
                String courseName = section.getCourse().getCourseName();
                String sectionNum = section.getSectionNumber();
                String teacher = section.getAssignedTeacher() != null ?
                        section.getAssignedTeacher().getFirstName() + " " + section.getAssignedTeacher().getLastName() : "";
                String room = section.getAssignedRoom() != null ? section.getAssignedRoom().getRoomNumber() : "";
                String period = section.getAssignedPeriod() != null ? String.valueOf(section.getAssignedPeriod()) : "";
                String maxEnroll = section.getMaxEnrollment() != null ? String.valueOf(section.getMaxEnrollment()) : "30";
                String currentEnroll = section.getCurrentEnrollment() != null ? String.valueOf(section.getCurrentEnrollment()) : "0";
                String status = section.getSectionStatus() != null ? section.getSectionStatus().toString() : "UNKNOWN";

                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        escapeCSV(courseCode),
                        escapeCSV(courseName),
                        escapeCSV(sectionNum),
                        escapeCSV(teacher),
                        escapeCSV(room),
                        period,
                        maxEnroll,
                        currentEnroll,
                        status);
                recordCount++;
            }

            result.setFileName(filename);
            result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
            result.setRecordsExported(recordCount);
            result.setSuccess(true);
        }

        return result;
    }

    private SISExportResult exportToExcel(List<CourseSection> sections, String outputPath) throws IOException {
        SISExportResult result = new SISExportResult();
        result.setExportType("Excel");
        result.setExportTime(LocalDateTime.now());

        String filename = outputPath != null ? outputPath :
                generateExportFilename(ExportFormat.EXCEL, ExportType.COURSE_SECTIONS);

        // ✅ NULL SAFE: Validate sections parameter
        if (sections == null) {
            sections = new ArrayList<>();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Master Schedule");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Course Code", "Course Name", "Section", "Teacher", "Room", "Period", "Max Enrollment", "Current Enrollment", "Status"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            int rowNum = 1;
            for (CourseSection section : sections) {
                // ✅ NULL SAFE: Skip null sections and sections with null courses
                if (section == null || section.getCourse() == null) continue;
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(section.getCourse().getCourseCode() != null ? section.getCourse().getCourseCode() : "");
                row.createCell(1).setCellValue(section.getCourse().getCourseName() != null ? section.getCourse().getCourseName() : "");
                row.createCell(2).setCellValue(section.getSectionNumber() != null ? section.getSectionNumber() : "");
                row.createCell(3).setCellValue(section.getAssignedTeacher() != null ?
                        section.getAssignedTeacher().getFirstName() + " " + section.getAssignedTeacher().getLastName() : "");
                row.createCell(4).setCellValue(section.getAssignedRoom() != null ? section.getAssignedRoom().getRoomNumber() : "");
                row.createCell(5).setCellValue(section.getAssignedPeriod() != null ? section.getAssignedPeriod() : 0);
                row.createCell(6).setCellValue(section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 30);
                row.createCell(7).setCellValue(section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0);
                row.createCell(8).setCellValue(section.getSectionStatus() != null ? section.getSectionStatus().toString() : "UNKNOWN");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filename)) {
                workbook.write(fileOut);
            }

            result.setFileName(filename);
            result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
            result.setRecordsExported(sections.size());
            result.setSuccess(true);
        }

        return result;
    }

    private SISExportResult exportStudentSchedulesToCSV(List<Student> students, String outputPath) throws IOException {
        SISExportResult result = new SISExportResult();
        result.setExportType("Student Schedules CSV");
        result.setExportTime(LocalDateTime.now());

        String filename = outputPath != null ? outputPath :
                generateExportFilename(ExportFormat.GENERIC_CSV, ExportType.STUDENT_SCHEDULES);

        // ✅ NULL SAFE: Validate students parameter
        if (students == null) {
            students = new ArrayList<>();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Student ID,Last Name,First Name,Grade,Course Code,Course Name,Section,Period,Teacher,Room");

            int recordCount = 0;
            for (Student student : students) {
                // ✅ NULL SAFE: Skip null students
                if (student == null) continue;
                String studentId = student.getStudentId() != null ? student.getStudentId() : String.valueOf(student.getId());
                String lastName = student.getLastName();
                String firstName = student.getFirstName();
                String grade = student.getGradeLevel() != null ? String.valueOf(student.getGradeLevel()) : "";

                // ✅ NULL SAFE: Check for null enrolled courses list
                List<Course> enrolledCourses = student.getEnrolledCourses();
                if (enrolledCourses == null) continue;

                for (Course course : enrolledCourses) {
                    // ✅ NULL SAFE: Skip null courses and null course IDs
                    if (course == null || course.getId() == null) continue;

                    // ✅ NULL SAFE: Filter null sections and check course/ID before comparison
                    Optional<CourseSection> sectionOpt = courseSectionRepository.findAll().stream()
                            .filter(s -> s != null && s.getCourse() != null && s.getCourse().getId() != null &&
                                       s.getCourse().getId().equals(course.getId()))
                            .findFirst();

                    if (sectionOpt.isPresent()) {
                        CourseSection section = sectionOpt.get();
                        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                                escapeCSV(studentId),
                                escapeCSV(lastName),
                                escapeCSV(firstName),
                                grade,
                                escapeCSV(course.getCourseCode()),
                                escapeCSV(course.getCourseName()),
                                escapeCSV(section.getSectionNumber()),
                                section.getAssignedPeriod() != null ? section.getAssignedPeriod() : "",
                                section.getAssignedTeacher() != null ?
                                        escapeCSV(section.getAssignedTeacher().getFirstName() + " " + section.getAssignedTeacher().getLastName()) : "",
                                section.getAssignedRoom() != null ? escapeCSV(section.getAssignedRoom().getRoomNumber()) : "");
                        recordCount++;
                    }
                }
            }

            result.setFileName(filename);
            result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
            result.setRecordsExported(recordCount);
            result.setSuccess(true);
        }

        return result;
    }

    private SISExportResult exportTeacherSchedulesToCSV(List<Teacher> teachers, String outputPath) throws IOException {
        SISExportResult result = new SISExportResult();
        result.setExportType("Teacher Schedules CSV");
        result.setExportTime(LocalDateTime.now());

        String filename = outputPath != null ? outputPath :
                "exports/teacher_schedules_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        // ✅ NULL SAFE: Validate teachers parameter
        if (teachers == null) {
            teachers = new ArrayList<>();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Teacher ID,Last Name,First Name,Period,Course Code,Course Name,Section,Room,Enrolled,Max");

            int recordCount = 0;
            for (Teacher teacher : teachers) {
                // ✅ NULL SAFE: Skip null teachers and teachers with null IDs
                if (teacher == null || teacher.getId() == null) continue;

                // ✅ NULL SAFE: Filter null sections and check assigned teacher/ID before comparison
                List<CourseSection> teacherSections = courseSectionRepository.findAll().stream()
                        .filter(s -> s != null && s.getAssignedTeacher() != null &&
                                   s.getAssignedTeacher().getId() != null &&
                                   s.getAssignedTeacher().getId().equals(teacher.getId()))
                        .collect(Collectors.toList());

                for (CourseSection section : teacherSections) {
                    // ✅ NULL SAFE: Skip null sections and sections with null courses
                    if (section == null || section.getCourse() == null) continue;

                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            teacher.getId(),
                            escapeCSV(teacher.getLastName()),
                            escapeCSV(teacher.getFirstName()),
                            section.getAssignedPeriod() != null ? section.getAssignedPeriod() : "",
                            escapeCSV(section.getCourse().getCourseCode()),
                            escapeCSV(section.getCourse().getCourseName()),
                            escapeCSV(section.getSectionNumber()),
                            section.getAssignedRoom() != null ? escapeCSV(section.getAssignedRoom().getRoomNumber()) : "",
                            section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0,
                            section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 30);
                    recordCount++;
                }
            }

            result.setFileName(filename);
            result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
            result.setRecordsExported(recordCount);
            result.setSuccess(true);
        }

        return result;
    }

    private SISExportResult exportEnrollmentsToCSV(String outputPath) throws IOException {
        SISExportResult result = new SISExportResult();
        result.setExportType("Enrollments CSV");
        result.setExportTime(LocalDateTime.now());

        String filename = outputPath != null ? outputPath :
                "exports/enrollments_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Student ID,Student Name,Course Code,Course Name,Section,Period,Teacher,Enrollment Date");

            List<Student> students = studentRepository.findAll();
            int recordCount = 0;

            for (Student student : students) {
                // ✅ NULL SAFE: Skip null students
                if (student == null) continue;

                // ✅ NULL SAFE: Check for null enrolled courses list
                List<Course> enrolledCourses = student.getEnrolledCourses();
                if (enrolledCourses == null) continue;

                for (Course course : enrolledCourses) {
                    // ✅ NULL SAFE: Skip null courses and null course IDs
                    if (course == null || course.getId() == null) continue;

                    // ✅ NULL SAFE: Filter null sections and check course/ID before comparison
                    Optional<CourseSection> sectionOpt = courseSectionRepository.findAll().stream()
                            .filter(s -> s != null && s.getCourse() != null && s.getCourse().getId() != null &&
                                       s.getCourse().getId().equals(course.getId()))
                            .findFirst();

                    if (sectionOpt.isPresent()) {
                        CourseSection section = sectionOpt.get();
                        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                                escapeCSV(student.getStudentId() != null ? student.getStudentId() : String.valueOf(student.getId())),
                                escapeCSV(student.getFirstName() + " " + student.getLastName()),
                                escapeCSV(course.getCourseCode()),
                                escapeCSV(course.getCourseName()),
                                escapeCSV(section.getSectionNumber()),
                                section.getAssignedPeriod() != null ? section.getAssignedPeriod() : "",
                                section.getAssignedTeacher() != null ?
                                        escapeCSV(section.getAssignedTeacher().getFirstName() + " " + section.getAssignedTeacher().getLastName()) : "",
                                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                        recordCount++;
                    }
                }
            }

            result.setFileName(filename);
            result.setFilePath(Paths.get(filename).toAbsolutePath().toString());
            result.setRecordsExported(recordCount);
            result.setSuccess(true);
        }

        return result;
    }

    // ========== Validation ==========

    @Override
    public List<String> validateExportData(ExportType exportType) {
        List<String> errors = new ArrayList<>();

        switch (exportType) {
            case MASTER_SCHEDULE:
            case COURSE_SECTIONS:
                List<CourseSection> sections = courseSectionRepository.findAll();
                for (CourseSection section : sections) {
                    // ✅ NULL SAFE: Skip null sections
                    if (section == null) continue;

                    if (section.getCourse() == null) {
                        errors.add("Section " + section.getId() + " has no course assigned");
                    }
                    if (section.getAssignedTeacher() == null) {
                        errors.add("Section " + section.getId() + " has no teacher assigned");
                    }
                    if (section.getAssignedRoom() == null) {
                        errors.add("Section " + section.getId() + " has no room assigned");
                    }
                    if (section.getAssignedPeriod() == null) {
                        errors.add("Section " + section.getId() + " has no period assigned");
                    }
                }
                break;

            case STUDENT_SCHEDULES:
                List<Student> students = studentRepository.findAll();
                for (Student student : students) {
                    // ✅ NULL SAFE: Skip null students
                    if (student == null) continue;

                    if (student.getStudentId() == null || student.getStudentId().isEmpty()) {
                        errors.add("Student " + student.getId() + " missing student ID");
                    }
                    if (student.getEnrolledCourses() == null || student.getEnrolledCourses().isEmpty()) {
                        // ✅ NULL SAFE: Safe extraction of student names with defaults
                        String firstName = (student.getFirstName() != null) ? student.getFirstName() : "Unknown";
                        String lastName = (student.getLastName() != null) ? student.getLastName() : "Student";
                        errors.add("Student " + firstName + " " + lastName + " has no enrolled courses");
                    }
                }
                break;
        }

        return errors;
    }

    @Override
    public boolean isFormatSupported(ExportFormat format) {
        return format != null;
    }

    @Override
    public List<ExportFormat> getSupportedFormats() {
        return Arrays.asList(ExportFormat.values());
    }

    // ========== Utility Methods ==========

    @Override
    public String getDefaultExportPath(ExportFormat format) {
        try {
            Files.createDirectories(Paths.get(DEFAULT_EXPORT_DIR));
        } catch (IOException e) {
            logger.warn("Could not create export directory", e);
        }
        return DEFAULT_EXPORT_DIR;
    }

    @Override
    public String generateExportFilename(ExportFormat format, ExportType exportType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String exportTypeName = exportType.name().toLowerCase();
        return String.format("%s/%s_%s_%s.%s",
                DEFAULT_EXPORT_DIR,
                format.name().toLowerCase(),
                exportTypeName,
                timestamp,
                format.getFileExtension());
    }

    @Override
    public String previewExportData(ExportFormat format, ExportType exportType) {
        // Return first 10 rows as preview
        StringBuilder preview = new StringBuilder();
        preview.append("Preview of ").append(format.getDisplayName()).append(" - ").append(exportType).append(":\n\n");

        try {
            // ✅ NULL SAFE: Filter null sections and sections with null courses before limiting
            List<CourseSection> sections = courseSectionRepository.findAll().stream()
                    .filter(section -> section != null && section.getCourse() != null)
                    .limit(10)
                    .collect(Collectors.toList());

            for (CourseSection section : sections) {
                preview.append(String.format("%s - %s (Section %s) - Teacher: %s - Room: %s - Period: %s%n",
                        section.getCourse().getCourseCode(),
                        section.getCourse().getCourseName(),
                        section.getSectionNumber(),
                        section.getAssignedTeacher() != null ? section.getAssignedTeacher().getLastName() : "TBA",
                        section.getAssignedRoom() != null ? section.getAssignedRoom().getRoomNumber() : "TBA",
                        section.getAssignedPeriod() != null ? section.getAssignedPeriod() : "TBA"));
            }
        } catch (Exception e) {
            preview.append("Error generating preview: ").append(e.getMessage());
        }

        return preview.toString();
    }

    /**
     * Escape CSV values
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
