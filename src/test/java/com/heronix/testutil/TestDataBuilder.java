package com.heronix.testutil;

import com.heronix.model.domain.*;
import com.heronix.model.enums.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating test data objects with sensible defaults
 * Follows the Test Data Builder pattern
 *
 * Usage:
 * <pre>
 * {@code
 * // Create a teacher with defaults
 * Teacher teacher = TestDataBuilder.aTeacher().build();
 *
 * // Create a teacher with custom properties
 * Teacher customTeacher = TestDataBuilder.aTeacher()
 *     .withFirstName("John")
 *     .withLastName("Doe")
 *     .withDepartment("Math")
 *     .build();
 * }
 * </pre>
 */
public class TestDataBuilder {

    // ==================== Teacher Builder ====================

    public static TeacherBuilder aTeacher() {
        return new TeacherBuilder();
    }

    public static class TeacherBuilder {
        private Long id = 1L;
        private String employeeId = "EMP001";
        private String firstName = "Test";
        private String lastName = "Teacher";
        private String email = "test.teacher@school.edu";
        private String department = "Mathematics";
        private String subject = "Algebra";
        private List<String> certifications = new ArrayList<>(List.of("Math 6-12"));
        private Integer yearsOfExperience = 5;
        private Boolean isFullTime = true;
        private Integer maxCoursesPerDay = 6;
        private Integer maxStudentsPerClass = 30;
        private Integer planningPeriodsPerWeek = 5;

        public TeacherBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public TeacherBuilder withEmployeeId(String employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public TeacherBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public TeacherBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public TeacherBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public TeacherBuilder withDepartment(String department) {
            this.department = department;
            return this;
        }

        public TeacherBuilder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public TeacherBuilder withCertifications(List<String> certifications) {
            this.certifications = certifications;
            return this;
        }

        public TeacherBuilder withYearsOfExperience(Integer years) {
            this.yearsOfExperience = years;
            return this;
        }

        public TeacherBuilder withFullTime(Boolean isFullTime) {
            this.isFullTime = isFullTime;
            return this;
        }

        public TeacherBuilder withMaxCoursesPerDay(Integer max) {
            this.maxCoursesPerDay = max;
            return this;
        }

        public Teacher build() {
            Teacher teacher = new Teacher();
            teacher.setId(id);
            teacher.setEmployeeId(employeeId);
            teacher.setFirstName(firstName);
            teacher.setLastName(lastName);
            // Build full name from first + last if available
            if (firstName != null && lastName != null) {
                teacher.setName(firstName + " " + lastName);
            } else if (firstName != null) {
                teacher.setName(firstName);
            } else if (lastName != null) {
                teacher.setName(lastName);
            }
            teacher.setEmail(email);
            teacher.setDepartment(department);
            teacher.setCertifications(certifications);
            teacher.setActive(true);

            // ✅ FIX: Convert simple certification strings to SubjectCertification objects
            // so that getCertifiedSubjects() works in tests
            // NOTE: We manually set the list to avoid circular reference (Teacher ↔ SubjectCertification)
            if (certifications != null) {
                java.util.List<com.eduscheduler.model.domain.SubjectCertification> subjectCerts =
                    new java.util.ArrayList<>();
                for (String cert : certifications) {
                    com.eduscheduler.model.domain.SubjectCertification subjectCert =
                        new com.eduscheduler.model.domain.SubjectCertification();
                    subjectCert.setSubject(cert);
                    subjectCert.setCertificationNumber("TEST-" + cert.hashCode());
                    subjectCert.setIssueDate(java.time.LocalDate.now().minusYears(1));
                    subjectCert.setExpirationDate(java.time.LocalDate.now().plusYears(5));
                    subjectCert.setIssuingAgency("Florida Department of Education");
                    // Don't set teacher reference to avoid circular dependency
                    subjectCerts.add(subjectCert);
                }
                // Set the list directly instead of using addSubjectCertification()
                teacher.setSubjectCertifications(subjectCerts);
            }

            return teacher;
        }
    }

    // ==================== Course Builder ====================

    public static CourseBuilder aCourse() {
        return new CourseBuilder();
    }

    public static class CourseBuilder {
        private Long id = 1L;
        private String courseCode = "MATH101";
        private String courseName = "Algebra I";
        private String subject = "Mathematics";
        private String department = "Mathematics";
        private Integer credits = 1;
        private String gradeLevel = "9";
        private Integer minStudents = 15;
        private Integer maxStudents = 30;
        private Integer periodsPerWeek = 5;
        private Boolean requiresLab = false;
        private Boolean isSingleton = false;

        public CourseBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public CourseBuilder withCourseCode(String code) {
            this.courseCode = code;
            return this;
        }

        public CourseBuilder withCourseName(String name) {
            this.courseName = name;
            return this;
        }

        public CourseBuilder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public CourseBuilder withDepartment(String department) {
            this.department = department;
            return this;
        }

        public CourseBuilder withCredits(Integer credits) {
            this.credits = credits;
            return this;
        }

        public CourseBuilder withGradeLevel(String gradeLevel) {
            this.gradeLevel = gradeLevel;
            return this;
        }

        public CourseBuilder withMaxStudents(Integer max) {
            this.maxStudents = max;
            return this;
        }

        public CourseBuilder withPeriodsPerWeek(Integer periods) {
            this.periodsPerWeek = periods;
            return this;
        }

        public CourseBuilder withRequiresLab(Boolean requiresLab) {
            this.requiresLab = requiresLab;
            return this;
        }

        public CourseBuilder withIsSingleton(Boolean isSingleton) {
            this.isSingleton = isSingleton;
            return this;
        }

        public Course build() {
            Course course = new Course();
            course.setId(id);
            course.setCourseCode(courseCode);
            course.setCourseName(courseName);
            course.setSubject(subject);
            course.setCredits(credits.doubleValue());
            course.setMinStudents(minStudents);
            course.setMaxStudents(maxStudents);
            course.setRequiresLab(requiresLab);
            return course;
        }
    }

    // ==================== Student Builder ====================

    public static StudentBuilder aStudent() {
        return new StudentBuilder();
    }

    public static class StudentBuilder {
        private Long id = 1L;
        private String studentId = "STU001";
        private String firstName = "Test";
        private String lastName = "Student";
        private String email = "test.student@school.edu";
        private String gradeLevel = "9";
        private LocalDate dateOfBirth = LocalDate.now().minusYears(14);
        private Double gpa = 3.5;
        private Boolean hasIEP = false;
        private Boolean has504 = false;

        public StudentBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public StudentBuilder withStudentId(String studentId) {
            this.studentId = studentId;
            return this;
        }

        public StudentBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public StudentBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public StudentBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public StudentBuilder withGradeLevel(String gradeLevel) {
            this.gradeLevel = gradeLevel;
            return this;
        }

        public StudentBuilder withGpa(Double gpa) {
            this.gpa = gpa;
            return this;
        }

        public StudentBuilder withIEP(Boolean hasIEP) {
            this.hasIEP = hasIEP;
            return this;
        }

        public StudentBuilder with504(Boolean has504) {
            this.has504 = has504;
            return this;
        }

        public Student build() {
            Student student = new Student();
            student.setId(id);
            student.setStudentId(studentId);
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setGradeLevel(gradeLevel);
            student.setDateOfBirth(dateOfBirth);
            student.setHasIEP(hasIEP);
            return student;
        }
    }

    // ==================== Room Builder ====================

    public static RoomBuilder aRoom() {
        return new RoomBuilder();
    }

    public static class RoomBuilder {
        private Long id = 1L;
        private String roomNumber = "101";
        private String building = "Main";
        private Integer capacity = 30;
        private String roomType = "Classroom";
        private Boolean hasProjector = true;
        private Boolean hasComputers = false;
        private Boolean isLabRoom = false;

        public RoomBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public RoomBuilder withRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
            return this;
        }

        public RoomBuilder withBuilding(String building) {
            this.building = building;
            return this;
        }

        public RoomBuilder withCapacity(Integer capacity) {
            this.capacity = capacity;
            return this;
        }

        public RoomBuilder withRoomType(String roomType) {
            this.roomType = roomType;
            return this;
        }

        public RoomBuilder withLabRoom(Boolean isLabRoom) {
            this.isLabRoom = isLabRoom;
            return this;
        }

        public Room build() {
            Room room = new Room();
            room.setId(id);
            room.setRoomNumber(roomNumber);
            room.setBuilding(building);
            room.setCapacity(capacity);
            room.setHasProjector(hasProjector);
            room.setHasComputers(hasComputers);
            return room;
        }
    }

    // ==================== Schedule Builder ====================

    public static ScheduleBuilder aSchedule() {
        return new ScheduleBuilder();
    }

    public static class ScheduleBuilder {
        private Long id = 1L;
        private String name = "Test Schedule";
        private String semester = "Fall 2025";
        private LocalDate startDate = LocalDate.now();
        private LocalDate endDate = LocalDate.now().plusMonths(4);
        private LocalTime startTime = LocalTime.of(8, 0);
        private LocalTime endTime = LocalTime.of(15, 0);
        private Integer periodDurationMinutes = 50;
        private Integer periodsPerDay = 8;

        public ScheduleBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public ScheduleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ScheduleBuilder withSemester(String semester) {
            this.semester = semester;
            return this;
        }

        public ScheduleBuilder withStartDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public ScheduleBuilder withEndDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public ScheduleBuilder withStartTime(LocalTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public ScheduleBuilder withPeriodsPerDay(Integer periods) {
            this.periodsPerDay = periods;
            return this;
        }

        public Schedule build() {
            Schedule schedule = new Schedule();
            schedule.setId(id);
            schedule.setName(name);
            schedule.setStartDate(startDate);
            schedule.setEndDate(endDate);
            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
            return schedule;
        }
    }

    // ==================== TimeSlot Builder ====================

    public static TimeSlotBuilder aTimeSlot() {
        return new TimeSlotBuilder();
    }

    public static class TimeSlotBuilder {
        private Long id = 1L;
        private Integer periodNumber = 1;
        private LocalTime startTime = LocalTime.of(8, 0);
        private LocalTime endTime = LocalTime.of(8, 50);
        private String dayOfWeek = "MONDAY";

        public TimeSlotBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public TimeSlotBuilder withPeriodNumber(Integer periodNumber) {
            this.periodNumber = periodNumber;
            return this;
        }

        public TimeSlotBuilder withStartTime(LocalTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public TimeSlotBuilder withEndTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public TimeSlotBuilder withDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }

        public TimeSlot build() {
            TimeSlot timeSlot = new TimeSlot();
            timeSlot.setPeriodNumber(periodNumber);
            timeSlot.setStartTime(startTime);
            timeSlot.setEndTime(endTime);
            return timeSlot;
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a list of teachers with sequential IDs
     */
    public static List<Teacher> createTeachers(int count) {
        List<Teacher> teachers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            teachers.add(aTeacher()
                .withId((long) i)
                .withEmployeeId("EMP" + String.format("%03d", i))
                .withFirstName("Teacher" + i)
                .withLastName("Test")
                .build());
        }
        return teachers;
    }

    /**
     * Creates a list of students with sequential IDs
     */
    public static List<Student> createStudents(int count) {
        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            students.add(aStudent()
                .withId((long) i)
                .withStudentId("STU" + String.format("%03d", i))
                .withFirstName("Student" + i)
                .withLastName("Test")
                .build());
        }
        return students;
    }

    /**
     * Creates a list of courses with sequential IDs
     */
    public static List<Course> createCourses(int count) {
        List<Course> courses = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            courses.add(aCourse()
                .withId((long) i)
                .withCourseCode("CRS" + String.format("%03d", i))
                .withCourseName("Course " + i)
                .build());
        }
        return courses;
    }

    /**
     * Creates a list of rooms with sequential IDs
     */
    public static List<Room> createRooms(int count) {
        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rooms.add(aRoom()
                .withId((long) i)
                .withRoomNumber(String.valueOf(100 + i))
                .build());
        }
        return rooms;
    }
}
