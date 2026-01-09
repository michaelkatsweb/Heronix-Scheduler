package com.heronix.util;

import com.heronix.model.domain.*;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility for generating bulk test data
 * Location: src/main/java/com/eduscheduler/util/BulkDataGenerator.java
 */
@Slf4j
@Component
@RequiredArgsConstructor

public class BulkDataGenerator {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    private final Random random = new Random();

    /**
     * Generate small dataset (for quick testing)
     * 5 teachers, 10 rooms, 15 courses, 25 students
     */
    @Transactional
    public void generateSmallDataset() {
        log.info("Generating small test dataset");

        List<Teacher> teachers = generateTeachers(5);
        List<Room> rooms = generateRooms(10);
        List<Course> courses = generateCourses(15, teachers);
        List<Student> students = generateStudents(25);

        log.info("Small dataset complete: {} teachers, {} rooms, {} courses, {} students",
                teachers.size(), rooms.size(), courses.size(), students.size());
    }

    /**
     * Generate medium dataset (for realistic testing)
     * 20 teachers, 30 rooms, 50 courses, 100 students
     */
    @Transactional
    public void generateMediumDataset() {
        log.info("Generating medium test dataset");

        List<Teacher> teachers = generateTeachers(20);
        List<Room> rooms = generateRooms(30);
        List<Course> courses = generateCourses(50, teachers);
        List<Student> students = generateStudents(100);

        log.info("Medium dataset complete: {} teachers, {} rooms, {} courses, {} students",
                teachers.size(), rooms.size(), courses.size(), students.size());
    }

    /**
     * Generate large dataset (for stress testing)
     * 50 teachers, 75 rooms, 150 courses, 500 students
     */
    @Transactional
    public void generateLargeDataset() {
        log.info("Generating large test dataset");

        List<Teacher> teachers = generateTeachers(50);
        List<Room> rooms = generateRooms(75);
        List<Course> courses = generateCourses(150, teachers);
        List<Student> students = generateStudents(500);

        log.info("Large dataset complete: {} teachers, {} rooms, {} courses, {} students",
                teachers.size(), rooms.size(), courses.size(), students.size());
    }

    /**
     * Generate extra-large dataset (for performance testing)
     * 100 teachers, 150 rooms, 300 courses, 1000 students
     */
    @Transactional
    public void generateXLDataset() {
        log.info("Generating XL test dataset");

        List<Teacher> teachers = generateTeachers(100);
        List<Room> rooms = generateRooms(150);
        List<Course> courses = generateCourses(300, teachers);
        List<Student> students = generateStudents(1000);

        log.info("XL dataset complete: {} teachers, {} rooms, {} courses, {} students",
                teachers.size(), rooms.size(), courses.size(), students.size());
    }

    /**
     * Generate sample teachers
     */
    @Transactional
    public List<Teacher> generateTeachers(int count) {
        log.info("Generating {} sample teachers", count);

        String[] firstNames = { "John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa" };
        String[] lastNames = { "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis" };
        String[] departments = { "Math", "Science", "English", "History", "Art", "Music", "PE", "Languages" };

        List<Teacher> teachers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Teacher teacher = new Teacher();
            teacher.setName(firstNames[random.nextInt(firstNames.length)] + " " +
                    lastNames[random.nextInt(lastNames.length)]);
            teacher.setEmployeeId("EMP" + String.format("%04d", i + 1));
            teacher.setEmail("teacher" + (i + 1) + "@school.edu");
            teacher.setPhoneNumber("555-" + String.format("%04d", random.nextInt(10000)));
            teacher.setDepartment(departments[random.nextInt(departments.length)]);
            teacher.setMaxHoursPerWeek(35 + random.nextInt(11)); // 35-45 hours
            teacher.setMaxConsecutiveHours(3 + random.nextInt(3)); // 3-5 hours
            teacher.setPreferredBreakMinutes(15 + random.nextInt(16)); // 15-30 minutes
            teacher.setPriorityLevel(PriorityLevel.Q2_IMPORTANT_NOT_URGENT);
            teacher.setActive(true);

            teachers.add(teacherRepository.save(teacher));
        }

        log.info("Generated {} teachers", teachers.size());
        return teachers;
    }

    /**
     * Generate sample rooms
     */
    @Transactional
    public List<Room> generateRooms(int count) {
        log.info("Generating {} sample rooms", count);

        RoomType[] types = RoomType.values();
        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Room room = new Room();
            room.setRoomNumber(String.valueOf(100 + i));
            room.setBuilding("Building " + (char) ('A' + (i / 10)));
            room.setFloor((i % 3) + 1);
            room.setCapacity(25 + random.nextInt(16)); // 25-40
            room.setType(types[random.nextInt(types.length)]);
            room.setActive(true);
            room.setWheelchairAccessible(random.nextBoolean());
            room.setHasProjector(random.nextDouble() > 0.3); // 70% have projectors
            room.setHasSmartboard(random.nextDouble() > 0.5); // 50% have smartboards
            room.setHasComputers(random.nextDouble() > 0.6); // 40% have computers

            rooms.add(roomRepository.save(room));
        }

        log.info("Generated {} rooms", rooms.size());
        return rooms;
    }

    /**
     * Generate sample courses
     */
    @Transactional
    public List<Course> generateCourses(int count, List<Teacher> teachers) {
        log.info("Generating {} sample courses", count);

        String[] subjects = { "Mathematics", "Science", "English", "History", "Art", "Music", "PE",
                "Computer Science", "Foreign Language", "Business", "Health" };
        String[] levels = { "Beginner", "Intermediate", "Advanced", "AP", "Honors", "IB" };
        EducationLevel[] eduLevels = EducationLevel.values();

        List<Course> courses = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Course course = new Course();
            course.setCourseCode("CRS" + String.format("%04d", i + 1));

            String subject = subjects[random.nextInt(subjects.length)];
            String level = levels[random.nextInt(levels.length)];
            course.setCourseName(subject + " " + level);
            course.setSubject(subject);

            course.setLevel(eduLevels[random.nextInt(eduLevels.length)]);
            course.setScheduleType(ScheduleType.TRADITIONAL);
            course.setDurationMinutes(45 + random.nextInt(31)); // 45-75 minutes
            course.setSessionsPerWeek(3 + random.nextInt(3)); // 3-5 sessions
            course.setMaxStudents(25 + random.nextInt(11)); // 25-35 students
            course.setCurrentEnrollment(random.nextInt(course.getMaxStudents() + 1));
            course.setActive(true);
            course.setRequiresLab(random.nextDouble() > 0.7); // 30% require lab
            course.setComplexityScore(random.nextInt(10) + 1); // 1-10
            course.setOptimalStartHour(8 + random.nextInt(6)); // 8 AM - 1 PM

            if (!teachers.isEmpty()) {
                course.setTeacher(teachers.get(random.nextInt(teachers.size())));
            }

            courses.add(courseRepository.save(course));
        }

        log.info("Generated {} courses", courses.size());
        return courses;
    }

    /**
     * Generate sample students
     */
    @Transactional
    public List<Student> generateStudents(int count) {
        log.info("Generating {} sample students", count);

        String[] firstNames = { "Alex", "Brian", "Chris", "Diana", "Ethan", "Fiona", "George", "Hannah" };
        String[] lastNames = { "Anderson", "Brown", "Clark", "Davis", "Evans", "Foster", "Green", "Harris" };
        String[] grades = { "9", "10", "11", "12" };

        List<Student> students = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Student student = new Student();
            student.setStudentId("STU" + String.format("%05d", i + 1));
            student.setFirstName(firstNames[random.nextInt(firstNames.length)]);
            student.setLastName(lastNames[random.nextInt(lastNames.length)]);
            student.setGradeLevel(grades[random.nextInt(grades.length)]);
            student.setEmail("student" + (i + 1) + "@school.edu");
            student.setActive(true);

            students.add(studentRepository.save(student));
        }

        log.info("Generated {} students", students.size());
        return students;
    }

    /**
     * Generate complete test dataset
     */
    @Transactional
    public void generateCompleteDataset() {
        generateMediumDataset();
    }

    /**
     * Clear all test data
     */
    @Transactional
    public void clearAllData() {
        log.warn("Clearing all data from database");
        studentRepository.deleteAll();
        courseRepository.deleteAll();
        roomRepository.deleteAll();
        teacherRepository.deleteAll();
        log.info("All data cleared");
    }
}