package com.heronix.testutil;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test Data Generator for Phase 1 Multi-Level Monitoring Entities
 *
 * Generates realistic test data for:
 * - ClassroomGradeEntry
 * - BehaviorIncident
 * - TeacherObservationNote
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 Repository Testing
 */
@Slf4j
@Component
public class Phase1TestDataGenerator {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private ClassroomGradeEntryRepository gradeEntryRepository;

    @Autowired
    private BehaviorIncidentRepository behaviorIncidentRepository;

    @Autowired
    private TeacherObservationNoteRepository observationNoteRepository;

    private final Random random = new Random();

    // ========================================================================
    // PUBLIC API
    // ========================================================================

    /**
     * Generate complete test dataset for Phase 1 testing
     *
     * @return TestDataSet containing all generated entities
     */
    @Transactional
    public TestDataSet generateCompleteDataset() {
        log.info("Generating complete Phase 1 test dataset...");

        TestDataSet dataset = new TestDataSet();

        // Get or create test dependencies
        Student student = getOrCreateTestStudent();
        Teacher teacher = getOrCreateTestTeacher();
        Course course = getOrCreateTestCourse(teacher);
        Campus campus = getOrCreateTestCampus();

        dataset.student = student;
        dataset.teacher = teacher;
        dataset.course = course;
        dataset.campus = campus;

        // Generate grade entries
        dataset.gradeEntries = generateGradeEntries(student, teacher, course, campus, 20);
        log.info("Generated {} grade entries", dataset.gradeEntries.size());

        // Generate behavior incidents
        dataset.behaviorIncidents = generateBehaviorIncidents(student, teacher, course, campus, 10);
        log.info("Generated {} behavior incidents", dataset.behaviorIncidents.size());

        // Generate teacher observations
        dataset.observationNotes = generateObservationNotes(student, teacher, course, campus, 8);
        log.info("Generated {} observation notes", dataset.observationNotes.size());

        log.info("Phase 1 test dataset generation complete!");

        return dataset;
    }

    // ========================================================================
    // GRADE ENTRY GENERATION
    // ========================================================================

    /**
     * Generate classroom grade entries with realistic data
     */
    @Transactional
    public List<ClassroomGradeEntry> generateGradeEntries(
            Student student, Teacher teacher, Course course, Campus campus, int count) {

        List<ClassroomGradeEntry> entries = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        for (int i = 0; i < count; i++) {
            LocalDate assignmentDate = currentDate.minusDays(random.nextInt(60));

            ClassroomGradeEntry entry = ClassroomGradeEntry.builder()
                .student(student)
                .teacher(teacher)
                .course(course)
                .campus(campus)
                .assignmentName(generateAssignmentName(i))
                .assignmentType(randomAssignmentType())
                .assignmentDate(assignmentDate)
                .pointsPossible(100.0)
                .pointsEarned(randomPoints())
                .isBenchmarkAssessment(random.nextInt(10) < 2) // 20% are benchmarks
                .isMissingWork(random.nextInt(10) < 1) // 10% missing
                .teacherNotes(random.nextInt(5) < 1 ? "Needs improvement" : null)
                .enteredByStaffId(teacher.getId())
                .build();

            entries.add(gradeEntryRepository.save(entry));
        }

        return entries;
    }

    private String generateAssignmentName(int index) {
        String[] types = {"Quiz", "Test", "Project", "Homework", "Lab", "Essay"};
        return types[random.nextInt(types.length)] + " #" + (index + 1);
    }

    private ClassroomGradeEntry.AssignmentType randomAssignmentType() {
        ClassroomGradeEntry.AssignmentType[] types = ClassroomGradeEntry.AssignmentType.values();
        return types[random.nextInt(types.length)];
    }

    private Double randomPoints() {
        // Generate realistic grade distribution
        int roll = random.nextInt(100);
        if (roll < 5) return null; // 5% no grade yet
        if (roll < 10) return 40.0 + random.nextDouble() * 20; // 5% failing
        if (roll < 30) return 60.0 + random.nextDouble() * 10; // 20% D
        if (roll < 60) return 70.0 + random.nextDouble() * 10; // 30% C
        if (roll < 85) return 80.0 + random.nextDouble() * 10; // 25% B
        return 90.0 + random.nextDouble() * 10; // 15% A
    }

    // ========================================================================
    // BEHAVIOR INCIDENT GENERATION
    // ========================================================================

    /**
     * Generate behavior incidents with realistic scenarios
     */
    @Transactional
    public List<BehaviorIncident> generateBehaviorIncidents(
            Student student, Teacher teacher, Course course, Campus campus, int count) {

        List<BehaviorIncident> incidents = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        for (int i = 0; i < count; i++) {
            LocalDate incidentDate = currentDate.minusDays(random.nextInt(60));
            boolean isPositive = random.nextInt(10) < 3; // 30% positive

            BehaviorIncident.BehaviorType behaviorType = isPositive ?
                BehaviorIncident.BehaviorType.POSITIVE :
                BehaviorIncident.BehaviorType.NEGATIVE;

            BehaviorIncident incident = BehaviorIncident.builder()
                .student(student)
                .reportingTeacher(teacher)
                .course(random.nextInt(5) < 4 ? course : null) // 80% in class, 20% outside
                .campus(campus)
                .incidentDate(incidentDate)
                .incidentTime(LocalTime.of(8 + random.nextInt(7), random.nextInt(60)))
                .behaviorType(behaviorType)
                .behaviorCategory(randomBehaviorCategory(isPositive))
                .severityLevel(isPositive ? null : randomSeverityLevel())
                .incidentLocation(randomIncidentLocation())
                .incidentDescription(generateIncidentDescription(isPositive))
                .interventionApplied(isPositive ? "Positive recognition" : randomIntervention())
                .parentContacted(random.nextInt(4) < 1) // 25% parent contacted
                .adminReferralRequired(random.nextInt(10) < 1) // 10% require admin
                .enteredByStaffId(teacher.getId())
                .build();

            incidents.add(behaviorIncidentRepository.save(incident));
        }

        return incidents;
    }

    private BehaviorIncident.BehaviorCategory randomBehaviorCategory(boolean isPositive) {
        if (isPositive) {
            BehaviorIncident.BehaviorCategory[] positive = {
                BehaviorIncident.BehaviorCategory.PARTICIPATION,
                BehaviorIncident.BehaviorCategory.COLLABORATION,
                BehaviorIncident.BehaviorCategory.LEADERSHIP,
                BehaviorIncident.BehaviorCategory.IMPROVEMENT,
                BehaviorIncident.BehaviorCategory.HELPING_OTHERS
            };
            return positive[random.nextInt(positive.length)];
        } else {
            BehaviorIncident.BehaviorCategory[] negative = {
                BehaviorIncident.BehaviorCategory.DISRUPTION,
                BehaviorIncident.BehaviorCategory.TARDINESS,
                BehaviorIncident.BehaviorCategory.NON_COMPLIANCE,
                BehaviorIncident.BehaviorCategory.INAPPROPRIATE_LANGUAGE,
                BehaviorIncident.BehaviorCategory.TECHNOLOGY_MISUSE
            };
            return negative[random.nextInt(negative.length)];
        }
    }

    private BehaviorIncident.SeverityLevel randomSeverityLevel() {
        int roll = random.nextInt(100);
        if (roll < 70) return BehaviorIncident.SeverityLevel.MINOR;
        if (roll < 95) return BehaviorIncident.SeverityLevel.MODERATE;
        return BehaviorIncident.SeverityLevel.MAJOR;
    }

    private BehaviorIncident.IncidentLocation randomIncidentLocation() {
        BehaviorIncident.IncidentLocation[] locations = {
            BehaviorIncident.IncidentLocation.CLASSROOM,
            BehaviorIncident.IncidentLocation.HALLWAY,
            BehaviorIncident.IncidentLocation.CAFETERIA,
            BehaviorIncident.IncidentLocation.GYMNASIUM
        };
        return locations[random.nextInt(locations.length)];
    }

    private String generateIncidentDescription(boolean isPositive) {
        if (isPositive) {
            String[] positive = {
                "Demonstrated exceptional collaboration with peers",
                "Showed significant improvement in participation",
                "Helped struggling classmate understand concept",
                "Displayed outstanding leadership during group activity"
            };
            return positive[random.nextInt(positive.length)];
        } else {
            String[] negative = {
                "Talking during instruction after multiple redirections",
                "Arrived late to class without pass",
                "Refused to follow classroom procedures",
                "Used inappropriate language when frustrated",
                "Using phone during class time"
            };
            return negative[random.nextInt(negative.length)];
        }
    }

    private String randomIntervention() {
        String[] interventions = {
            "Verbal warning and classroom management",
            "Parent contact and behavior contract",
            "Detention assigned",
            "Conference with student",
            "Seating change implemented"
        };
        return interventions[random.nextInt(interventions.length)];
    }

    // ========================================================================
    // TEACHER OBSERVATION GENERATION
    // ========================================================================

    /**
     * Generate teacher observation notes
     */
    @Transactional
    public List<TeacherObservationNote> generateObservationNotes(
            Student student, Teacher teacher, Course course, Campus campus, int count) {

        List<TeacherObservationNote> notes = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        for (int i = 0; i < count; i++) {
            LocalDate observationDate = currentDate.minusDays(random.nextInt(60));

            TeacherObservationNote note = TeacherObservationNote.builder()
                .student(student)
                .teacher(teacher)
                .course(course)
                .campus(campus)
                .observationDate(observationDate)
                .observationCategory(randomObservationCategory())
                .observationRating(randomObservationRating())
                .observationNotes(generateObservationNotes())
                .isFlagForIntervention(random.nextInt(10) < 2) // 20% flagged
                .interventionTypeSuggested(random.nextInt(5) < 1 ? "Academic tutoring" : null)
                .build();

            notes.add(observationNoteRepository.save(note));
        }

        return notes;
    }

    private TeacherObservationNote.ObservationCategory randomObservationCategory() {
        TeacherObservationNote.ObservationCategory[] categories =
            TeacherObservationNote.ObservationCategory.values();
        return categories[random.nextInt(categories.length)];
    }

    private TeacherObservationNote.ObservationRating randomObservationRating() {
        int roll = random.nextInt(100);
        if (roll < 5) return TeacherObservationNote.ObservationRating.CONCERN;
        if (roll < 25) return TeacherObservationNote.ObservationRating.NEEDS_IMPROVEMENT;
        if (roll < 70) return TeacherObservationNote.ObservationRating.GOOD;
        return TeacherObservationNote.ObservationRating.EXCELLENT;
    }

    private String generateObservationNotes() {
        String[] notes = {
            "Student actively participates and demonstrates good comprehension",
            "Needs additional support with complex concepts",
            "Shows excellent effort and persistence",
            "Engagement has improved significantly this week",
            "Struggles to stay focused during independent work",
            "Works well with peers and contributes to group activities",
            "May benefit from one-on-one instruction"
        };
        return notes[random.nextInt(notes.length)];
    }

    // ========================================================================
    // TEST DEPENDENCY CREATION
    // ========================================================================

    private Student getOrCreateTestStudent() {
        List<Student> students = studentRepository.findAll();
        if (!students.isEmpty()) {
            return students.stream()
                .filter(s -> s.getDeleted() == null || !s.getDeleted())
                .findFirst()
                .orElse(students.get(0));
        }

        // Create test student if none exists
        Student student = new Student();
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setStudentId("TEST001");
        student.setGradeLevel("9");
        student.setEmail("test.student@school.edu");
        return studentRepository.save(student);
    }

    private Teacher getOrCreateTestTeacher() {
        List<Teacher> teachers = teacherRepository.findAll();
        if (!teachers.isEmpty()) {
            return teachers.get(0);
        }

        // Create test teacher if none exists
        Teacher teacher = new Teacher();
        teacher.setName("Test Teacher");
        teacher.setFirstName("Test");
        teacher.setLastName("Teacher");
        teacher.setEmployeeId("T001");
        teacher.setEmail("test.teacher@school.edu");
        return teacherRepository.save(teacher);
    }

    private Course getOrCreateTestCourse(Teacher teacher) {
        List<Course> courses = courseRepository.findAll();
        if (!courses.isEmpty()) {
            return courses.get(0);
        }

        // Create test course if none exists
        Course course = new Course();
        course.setCourseName("Test Course - Algebra I");
        course.setCourseCode("ALG1-01");
        course.setTeacher(teacher);
        course.setMinGradeLevel(9);
        course.setMaxGradeLevel(12);
        return courseRepository.save(course);
    }

    private Campus getOrCreateTestCampus() {
        List<Campus> campuses = campusRepository.findAll();
        if (!campuses.isEmpty()) {
            return campuses.get(0);
        }

        // Create test campus if none exists
        Campus campus = new Campus();
        campus.setCampusCode("THS");
        campus.setName("Test High School");
        return campusRepository.save(campus);
    }

    // ========================================================================
    // CLEANUP UTILITIES
    // ========================================================================

    /**
     * Clean up all Phase 1 test data
     */
    @Transactional
    public void cleanupTestData() {
        log.info("Cleaning up Phase 1 test data...");
        gradeEntryRepository.deleteAll();
        behaviorIncidentRepository.deleteAll();
        observationNoteRepository.deleteAll();
        log.info("Phase 1 test data cleanup complete");
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Container for generated test data
     */
    public static class TestDataSet {
        public Student student;
        public Teacher teacher;
        public Course course;
        public Campus campus;
        public List<ClassroomGradeEntry> gradeEntries;
        public List<BehaviorIncident> behaviorIncidents;
        public List<TeacherObservationNote> observationNotes;
    }
}
